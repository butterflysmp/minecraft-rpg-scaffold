package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.QuiverState;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The safe way to ASK about a quiver must stay reachable, or callers go back to the one that WRITES.
 *
 * <h2>Why this exists rather than a comment</h2>
 *
 * <p>{@link Quivers#resolveForShot} commits: a matured reload refills the magazine, an unstamped item
 * is stamped and written back. That is correct -- the lazy design has no expiry event to miss
 * precisely because the read IS the commit point. But it was first called {@code refusalFor}, which
 * reads as a question, and <b>the next two pieces of work both want to ask that question</b>: the
 * quiver lore line, and the action-bar HUD. Either one calling the obvious-looking method would
 * silently finish reloads as a side effect of RENDERING, and the bug would present as reloads
 * completing early -- which looks like the timer working, not like a read doing a write.
 *
 * <p>{@link Quivers#stateOf} is the answer: pure, no {@code Player}, returns a {@link QuiverState} a
 * caller can interrogate freely. <b>The split only protects anything while that method is public and
 * obvious.</b> Made private in a tidy-up -- it was private until this commit -- and every caller is
 * pushed straight back onto the committing path, with nothing red.
 *
 * <p>So the condition is written where it will be seen, in a red build, on
 * {@code DamageSignatureTest}'s idiom and with its argument: <b>a safety that holds on a condition
 * nobody wrote down where it would be violated is not a safety.</b>
 *
 * <h2>What it does NOT claim</h2>
 *
 * <p>It cannot check that {@code stateOf} is pure, or that {@code resolveForShot}'s name is a good
 * one. Purity and naming are not reflectable. What is checkable is that the pure entry point still
 * EXISTS and is still PUBLIC, and that the committing surface has not quietly grown a new member
 * whose name invites the same mistake -- both of which are the mechanical half of the decision.
 */
class QuiversSignatureTest {

    @Test
    void thePureWayToAskAboutAQuiverIsPublicAndTakesNoPlayer() {
        Method stateOf = Arrays.stream(Quivers.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("stateOf"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Quivers.stateOf is GONE. It is the only side-effect-free way to ask about a "
                                + "quiver; without it a HUD or tooltip reaches for resolveForShot and "
                                + "commits reloads while rendering."));

        assertTrue(Modifier.isPublic(stateOf.getModifiers()),
                "stateOf must stay PUBLIC -- made private, every caller is pushed back onto the "
                        + "committing path and nothing goes red");
        assertTrue(Modifier.isStatic(stateOf.getModifiers()));
        assertEquals(QuiverState.class, stateOf.getReturnType(),
                "it must hand back the core value type, so the asking happens in core and not here");

        // "TAKES NO Player" IS A PROXY FOR PURITY, AND IT IS DOCUMENTED AS ONE RATHER THAN AS THE
        // RULE -- because this sentence is what a future reader reasons from when deciding whether
        // some NEW method qualifies, and the obvious reading of it is wrong.
        //
        // THE ACTUAL MECHANISM: writes in this class are `held.editMeta(...)` on an ItemStack --
        // the UNSTAMPED arm does exactly that -- followed by setItemInMainHand to persist the copy
        // back. Bukkit's getItemInMainHand hands out a COPY, so editing the meta alone changes
        // nothing the player can see; THE PLAYER IS THE HALF THAT PERSISTS. So a Player is not what
        // ENABLES a write, it is what makes a write STICK.
        //
        // The proxy holds because of that copy semantics, not because Player is the write capability.
        // A method taking only an ItemStack could still call editMeta and mutate a caller's stack --
        // so a future addition must be judged on whether it edits meta at all, and this check is the
        // cheap mechanical half, not the definition.
        for (Class<?> parameter : stateOf.getParameterTypes()) {
            assertTrue(!parameter.getName().endsWith(".Player"),
                    "stateOf must not take a Player. That is a PROXY for purity (see the comment "
                            + "above): writes here are editMeta + setItemInMainHand, and the Player "
                            + "is the half that persists them, so needing one marks the committing "
                            + "path -- it does not define it.");
        }
    }

    /**
     * A COUNT WRITTEN IN PLAY MUST CARRY ITS OWN RENDER, AND {@code Quivers} MAY NOT WRITE ONE RAW.
     *
     * <p><b>This is boot row V1's display failure turned into a red build.</b> {@code spendRound}
     * wrote the count key and called {@code updateInventory}, and nothing re-ran {@code applyLore} --
     * which executes only from {@code mint} and {@code remint}. The stored count moved, the tooltip
     * did not, and <b>three green rows were entirely consistent with the defect</b>: Q4, Q5 and Q6
     * (relog, {@code /rpg refresh}, the enchant table) all route through {@code remint}, so the
     * display snapped to the right number at exactly those three moments. The bug presented as
     * <i>"the counter only updates when you relog."</i>
     *
     * <p>The fix is structural rather than remembered: {@link QuiverItems#setLoaded} writes AND
     * renders in one call, so <b>a fourth write site cannot express the write without the render.</b>
     * This guards the other half — that nobody reopens the raw path beside it.
     *
     * <p>{@link QuiverItems#stampFull} is the single legitimate raw writer and is <b>MINT-ONLY</b>,
     * where {@code applyLore} follows by construction. A call to it from {@code Quivers} would be an
     * in-play write with no render, which is precisely the shipped defect, so it is refused here too.
     *
     * <p>Reads the source rather than reflecting, because the hazard is a CALL inside a method body
     * and no signature shows it. Comments are stripped first — this file's own javadoc names both
     * forbidden strings, and a scan that does not strip prose reads documentation as if it were code.
     */
    @Test
    void quiversNeverWritesTheCountWithoutRenderingIt() throws IOException {
        Path source = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg",
                "paper", "weapon", "Quivers.java");
        assertTrue(Files.isRegularFile(source), "source not found at " + source.toAbsolutePath());
        String raw = Files.readString(source, StandardCharsets.UTF_8);
        String code = raw.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");

        // The stripper needs its own control: one that silently did nothing returns the whole file,
        // and every assertion below would then run against prose.
        assertTrue(code.length() < raw.length(), "comment stripping removed nothing");
        assertFalse(code.contains("{@link"), "javadoc survived the strip; the scan would read prose");

        assertFalse(code.contains("stampFull"),
                "stampFull is MINT-ONLY (applyLore follows it by construction). Calling it from "
                        + "Quivers is an in-play write with no render -- the same defect by a "
                        + "different door. Use setLoaded.");

        // POSITIVE CONTROL: the assertion above is satisfied by an empty file, so prove the
        // sanctioned call is actually present and this class really does write counts.
        assertTrue(code.contains("QuiverItems.setLoaded"),
                "Quivers writes no counts at all -- either the scan is broken or the funnel is gone");
    }

    /**
     * THE COUNT KEY IS TOUCHED IN EXACTLY TWO FILES, AND THE SCAN IS WIDER THAN ONE FILE ON PURPOSE.
     *
     * <p><b>The row above reads {@code Quivers.java} only, which covers today's three in-play writers
     * and would stay green for a writer added anywhere else.</b> That boundary is not hypothetical:
     * <b>slice A2 walks straight into it.</b> {@code DESIGN-stat-engine.md}'s rule — adopted verbatim
     * by this slice — is that a capacity DECREASE clamps the current value. When quiver size becomes
     * a stat, something must clamp the stored count when capacity drops, and that something will
     * almost certainly live in the stat-reconcile path rather than in {@code Quivers}. <b>It is a
     * write. It must re-render.</b> A one-file scan would not notice.
     *
     * <p>So the boundary is a GUARD rather than a note: {@code quiverLoaded} may appear under
     * {@code paper/src/main} in {@link io.github.butterflysmp.rpg.paper.adapter.Keys} (which DECLARES
     * it) and {@link QuiverItems} (which owns every read and write), <b>and nowhere else.</b> A2's
     * clamp written in the wrong place reddens here, which is exactly when it is cheap to move.
     *
     * <p><b>The cost is a named exemption list that must grow deliberately</b>, and it is the same
     * cost already priced for {@code WeaponLoader.KNOWN_KEYS} — with the same conclusion: <b>it fails
     * towards NOISE.</b> A legitimate new reader reddens loudly and someone adds it on purpose;
     * nothing goes quietly unguarded. The set is named rather than counted, so a reader can check it.
     */
    @Test
    void theCountKeyIsTouchedInExactlyTwoFilesAcrossAllOfPaper() throws IOException {
        Path main = Path.of("src", "main", "java");
        assertTrue(Files.isDirectory(main), "paper sources not found at " + main.toAbsolutePath());

        List<String> touching = new java.util.ArrayList<>();
        int scanned = 0;
        try (var walk = Files.walk(main)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                scanned++;
                String code = Files.readString(file, StandardCharsets.UTF_8)
                        .replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
                if (code.contains("quiverLoaded") || code.contains("quiverCapacity")) {
                    touching.add(file.getFileName().toString());
                }
            }
        }

        // A walk that discovered nothing reads exactly like a walk that found everything in order.
        assertTrue(scanned > 50, "only " + scanned + " files scanned -- the walk is not reaching paper");

        java.util.Collections.sort(touching);
        assertEquals(List.of("Keys.java", "QuiverItems.java"), touching,
                "the quiver count key must be touched ONLY where it is declared (Keys) and where it "
                        + "is owned (QuiverItems, whose setLoaded writes AND re-renders). A write "
                        + "anywhere else is boot row V1's defect in a new location -- and A2's "
                        + "capacity clamp is the known candidate. Add a file here only deliberately.");
    }

    /**
     * THE CAPACITY IS RESOLVED IN A NAMED SET OF PLACES, BECAUSE "EXACTLY ONE PLACE" HAD NO
     * INSTRUMENT.
     *
     * <p>{@code QuiverState.capacityOf}'s javadoc claims it is <i>"the WHOLE of the ordering"</i> and
     * that the fallback therefore <i>"happens in exactly one place rather than once per reader."</i>
     * <b>In commit 1a that was true only because there were no readers at all.</b> There are now two,
     * and nothing stopped a third from writing {@code stamped.orElse(weapon.quiverSize())} inline —
     * at which point the sentence is false and no test changes colour.
     *
     * <p><b>And the defect that inline reader WOULD BE is the exact one the stamp exists to
     * prevent:</b> a tooltip rendering the stamp while the refusal logic resolves the holder live.
     * Two resolvers is how they come to disagree.
     *
     * <p>So the readers are pinned as a NAMED SET rather than a count — a count over an unnamed set
     * cannot be checked by the reader, which is the rule these reports are held to:
     *
     * <ul>
     *   <li>{@code QuiverState} (core) — where {@code capacityOf} is DEFINED.
     *   <li>{@code QuiverItems} — where the accessor is DEFINED; it matches its own scan, the way
     *       {@code KNOWN_KEYS} must carry {@code id}.
     *   <li>{@code Quivers.stateOf} — the verdict path, which both refusal and enforcement read.
     *   <li>{@code WeaponItems.applyLore} — reads the stamp off the meta it is building.
     *   <li>{@code WeaponLore.build} — resolves it for the tooltip.
     * </ul>
     *
     * <h2>WHAT THIS GUARD DOES NOT DO, AND IT IS THE LARGER HALF</h2>
     *
     * <p><b>A GUARD OVER A SET OF CALL SITES IS NOT A TEST OF WHAT THOSE CALL SITES DO.</b> It proves
     * no sixth resolver exists. It cannot prove the five resolve correctly, and <b>it goes green for
     * every one of them that silently stops reading the stamp.</b> Measured, three times:
     *
     * <ul>
     *   <li>{@code MUTSTAMPDROP} — {@code WeaponLore} ignores the stamp. <b>Green</b> until
     *       {@code WeaponLoreTest} gained a row staging a stamp that DIFFERS from the authored value.
     *   <li>{@code MUTSTATEDROP} — {@code Quivers.stateOf} ignores it, stranding a boosted Ranger's
     *       last rounds behind {@code ALREADY_FULL}. <b>Green</b> until the resolution moved into
     *       {@link QuiverState#from}, where a core row can reach it.
     *   <li>{@code MUTAPPLYLORE2} — {@code WeaponItems.applyLore} passes {@code empty} while KEEPING
     *       the {@code capacityInMeta} call, so the set is unchanged. <b>Green, and still is.</b>
     * </ul>
     *
     * <p><b>The second of those was found by review, not by this guard, and the first version of the
     * third was caught only by luck</b> — dropping the call shrank the set, so the membership check
     * fired for a reason unrelated to the behaviour. Keeping the call makes it green again.
     *
     * <p><b>STATUS OF ALL FIVE, enumerated rather than counted:</b>
     *
     * <table><tr><th>site</th><th>role</th><th>witnessed?</th></tr>
     * <tr><td>{@code QuiverState.capacityOf} / {@code from}</td><td>the resolution</td>
     *     <td><b>YES</b> — {@code QuiverStateTest}, MUTFACTORYDROP red</td></tr>
     * <tr><td>{@code WeaponLore.build}</td><td>resolves for the tooltip</td>
     *     <td><b>YES</b> — {@code WeaponLoreTest}, MUTSTAMPDROP red</td></tr>
     * <tr><td>{@code Quivers.stateOf}</td><td>reads five values, decides nothing</td>
     *     <td><b>NO</b> — needs an {@code ItemStack}. Boot-only; the decision it used to make has moved.</td></tr>
     * <tr><td>{@code WeaponItems.applyLore}</td><td>plumbs the stamp to the tooltip</td>
     *     <td><b>NO</b> — needs an {@code ItemStack}. Boot-only.</td></tr>
     * <tr><td>{@code QuiverItems.capacityIn}</td><td>the accessor; resolves nothing</td>
     *     <td>n/a — there is no stamp-versus-authored decision in it</td></tr>
     * </table>
     *
     * <p>The two NOs are argument-passing with no decision in them. Both are recorded in
     * {@code PLAN-quiver-a2.md}'s COMMIT TABLE -- a document that exists -- rather than promised to
     * {@code GATE-quiver-a2.md}, which is commit 7 and is not written. A promise to a file that does
     * not exist is how the ContentValidator arm was named in four places and then was not there.
     *
     * <p>A sixth is a deliberate edit to this list, which is the moment to ask whether it should
     * instead be reading the state the others already built.
     *
     * <p><b>IT SCANS FOR THE ACCESSOR AS WELL AS THE RESOLVER, AND THE FIRST VERSION DID NOT —
     * WHICH IS WHY IT MISSED THE DEFECT IT WAS WRITTEN FOR.</b> Measured: a mutation adding
     * {@code QuiverItems.capacityIn(held, keys).orElse(weapon.quiverSize())} to {@code WeaponFire}
     * — the inline third resolver, exactly the case in the paragraphs above — came back **green**.
     * It calls neither {@code capacityOf} nor the key by name, so a scan for either was blind to it.
     * <b>A guard aimed at the name of the right thing rather than at the shape of the wrong thing.</b>
     * Obtaining a capacity at all now requires appearing on this list -- IN EITHER MODULE.
     */
    @Test
    void theCapacityIsResolvedOnlyWhereThisListSays() throws IOException {
        // BOTH MODULES. Scanning only paper/ would have been a claim true by accident of where the
        // code currently sits: capacityOf is PUBLIC ON A CORE CLASS, and WeaponLoreLines.quiverLine
        // is in core and is the natural place for a later commit to move the resolution into -- at
        // which point the tooltip's resolver would leave a paper-only guard's sight entirely and the
        // list would still read as four files. That is A1's one-file-scope finding recurring one
        // module over.
        List<String> resolvers = new java.util.ArrayList<>();
        int scanned = 0;
        for (Path root : List.of(Path.of("src", "main", "java"),
                                 Path.of("..", "core", "src", "main", "java"))) {
            assertTrue(Files.isDirectory(root), "source root not found: " + root.toAbsolutePath());
            try (var walk = Files.walk(root)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                    scanned++;
                    String code = Files.readString(file, StandardCharsets.UTF_8)
                            .replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
                    // BOTH the accessor and the resolver, because scanning for capacityOf alone
                    // MISSED the defect this guard exists for -- measured, see the javadoc.
                    if (code.contains("capacityIn") || code.contains("capacityOf(")
                            || code.contains("QuiverState.from(")) {
                        resolvers.add(file.getFileName().toString());
                    }
                }
            }
        }
        assertTrue(scanned > 100, "only " + scanned + " files scanned across both modules");

        java.util.Collections.sort(resolvers);
        assertEquals(
                List.of("QuiverItems.java", "QuiverState.java", "Quivers.java", "WeaponItems.java",
                        "WeaponLore.java"),
                resolvers,
                "a capacity may only be OBTAINED in these FIVE files, across core AND paper. Reading "
                        + "QuiverItems.capacityIn anywhere else and resolving it inline -- "
                        + "stamped.orElse(weapon.quiverSize()) -- is a second resolver, and two "
                        + "resolvers is how the tooltip and the refusal logic come to disagree.");
    }

    /**
     * EVERY WAY TO OBTAIN A CAPACITY IS ON THIS LIST, AND A SIXTH CANNOT BE ADDED SILENTLY.
     *
     * <h2>Why a reflective pin and not a wider string list</h2>
     *
     * <p>The scan above is <b>a list of names</b>, and it has now been too narrow three times, each
     * time for the same reason and each time widened afterwards:
     *
     * <ol>
     *   <li>it named {@code capacityOf}; {@code MUTINLINE} arrived through {@code capacityIn}.
     *   <li>it scanned {@code paper}; {@code capacityOf} is public in <b>core</b>.
     *   <li>{@link QuiverState#from} was added as a new way to obtain a capacity — <b>in the same
     *       commit whose javadoc said "a guard aimed at the NAME of the right thing rather than the
     *       SHAPE of the wrong one"</b> — and the list was not widened. A sixth file could write
     *       {@code QuiverState.from(loaded, OptionalInt.empty(), weapon.quiverSize(), a, b)} and
     *       obtain a state whose capacity ignores the stamp, containing neither scanned string.
     *       That is {@code MUTSTATEDROP} relocated one file over.
     * </ol>
     *
     * <p><b>Widening the list a fourth time fixes today and leaves the mechanism that failed three
     * times in place</b> — the mechanism being that somebody has to REMEMBER. Instance 3 is the proof
     * that remembering fails even with the rule freshly written three paragraphs above.
     *
     * <p>So this row does not try to be a better list. <b>It makes the list impossible to leave
     * stale in silence</b>: {@code QuiverState}'s public static surface is pinned, so adding a sixth
     * entry point <b>fails the build</b>, and the author lands here — beside the scan, with this
     * javadoc explaining what else must move. The guard cannot enumerate every future name; it can
     * refuse to let one appear unnoticed.
     *
     * <p>Same idiom as {@link #theCommittingSurfaceHasNotQuietlyGrown} and
     * {@code DamageSignatureTest}, and the same argument: <b>a safety that holds on a condition
     * nobody wrote down where it would be violated is not a safety.</b> Here the condition is
     * written where it will be violated — at the surface that grows.
     */
    @Test
    void noSixthWayToObtainACapacityCanAppearUnnoticed() {
        List<String> entryPoints = Arrays.stream(QuiverState.class.getDeclaredMethods())
                .filter(m -> !m.isSynthetic() && Modifier.isPublic(m.getModifiers())
                        && Modifier.isStatic(m.getModifiers()))
                .map(Method::getName)
                .distinct()
                .sorted()
                .toList();

        assertEquals(List.of("capacityOf", "from", "loaded", "reloading", "unstamped"), entryPoints,
                "QuiverState's public static surface changed. Every one of these yields a capacity -- "
                        + "capacityOf resolves one, the rest build a state carrying one -- so a new "
                        + "member must ALSO be added to the source scan in "
                        + "theCapacityIsResolvedOnlyWhereThisListSays, or a caller can obtain a "
                        + "capacity that ignores the stamp and no test will notice.");
    }

    /**
     * TODAY'S PUBLIC SURFACE, named rather than counted.
     *
     * <p>Three of these four WRITE ({@code resolveForShot}, {@code spendRound}, {@code beginReload},
     * {@code tryReloadHeldWeapon} -- all but {@code stateOf}), and every one is verb-named so it
     * cannot be mistaken for a query. A new member added here is a deliberate edit to this list,
     * which is the moment to ask whether its name says what it does.
     *
     * <p>Pinned as a SET rather than a count, because a count over an unnamed set cannot be checked
     * by the reader -- which is the same rule this slice's reports are held to.
     */
    @Test
    void theCommittingSurfaceHasNotQuietlyGrown() {
        List<String> publicMethods = Arrays.stream(Quivers.class.getDeclaredMethods())
                .filter(m -> !m.isSynthetic() && Modifier.isPublic(m.getModifiers()))
                .map(Method::getName)
                .distinct()
                .sorted()
                .toList();

        assertEquals(
                List.of("beginReload", "resolveForShot", "spendRound", "stateOf", "tryReloadHeldWeapon"),
                publicMethods,
                "Quivers' public surface changed. Every member but stateOf commits, so a new one "
                        + "needs a verb name that says so -- and a new PURE one belongs beside "
                        + "stateOf in the javadoc that points callers at it.");
    }
}
