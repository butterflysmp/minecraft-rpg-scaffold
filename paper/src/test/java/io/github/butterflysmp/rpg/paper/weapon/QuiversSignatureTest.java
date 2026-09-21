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
     * <p><b>This is boot row V1's display failure turned into a red build.</b> {@code spendRounds}
     * (then called {@code spendRound}, before a release could cost more than one)
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
     *
     * <h2>*** IT FIRED ON A NAME AND NOT ON A KEY, 2026-09-24. RECORDED, NOT FIXED ***</h2>
     *
     * <p>The needles are the BARE identifiers {@code quiverLoaded} and {@code quiverCapacity}, which
     * are the names of the {@code Keys} fields. <b>{@code StatsBarText} landed on this list having
     * touched no key at all</b>: its new quiver parameters were called {@code quiverLoaded} and
     * {@code quiverCapacity}, and a bare identifier cannot tell {@code keys.quiverLoaded} from
     * {@code int quiverLoaded}. That is a FALSE PRESENCE -- the reading was real, the conclusion was
     * about something else -- and it is the harder direction, because a hit reads as confirmation.
     *
     * <p><b>It was resolved by RENAMING the parameters</b> to {@code loadedRounds} and
     * {@code magazineCapacity}, not by adding {@code StatsBarText} to the list and not by loosening the
     * needle. Adding it would have been false: the file does not touch the key, and the list is the
     * thing a reader trusts. The rename also happens to be the better name for a pure formatter, which
     * describes a quantity rather than a storage key.
     *
     * <p><b>The needle's breadth is a standing cost and is named for whoever owns this row.</b>
     * {@code quiverLoaded} is a natural local-variable name, so this will fire again. Anchoring to
     * {@code .quiverLoaded} would fix it -- every real touch is a field access, including
     * {@code Keys}' own {@code this.quiverLoaded = ...} -- but tightening a needle is how a guard goes
     * blind, and that judgement belongs to whoever can re-derive this list rather than to a slice that
     * merely tripped over it.
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
     * <p><b>STATUS OF ALL SEVEN SITES, enumerated rather than counted -- they live in the SIX files
     * the assertion names, because {@code QuiverItems} holds two of them ({@code resolveCapacity} and
     * {@code capacityIn}). Sites and files are different quantities and both are named here so the
     * two numbers cannot read as a contradiction:</b>
     *
     * <table><tr><th>site</th><th>role</th><th>witnessed?</th></tr>
     * <tr><td>{@code QuiverState.capacityOf} / {@code from}</td><td>the resolution</td>
     *     <td><b>YES</b> — {@code QuiverStateTest}, MUTFACTORYDROP red</td></tr>
     * <tr><td>{@code QuiverSize.resolve}</td><td>stat + authored, and the {@code MIN_CAPACITY} floor</td>
     *     <td><b>YES</b> — {@code QuiverSizeTest}, MUTQSFLOOR and MUTQSMINCAP red</td></tr>
     * <tr><td>{@code WeaponLore.build}</td><td>resolves for the tooltip</td>
     *     <td><b>YES</b> — {@code WeaponLoreTest}, MUTSTAMPDROP red</td></tr>
     * <tr><td>{@code QuiverItems.resolveCapacity}</td><td>the READ: plumbs the wielder's stat into
     *     {@code QuiverSize.resolve}</td>
     *     <td><b>NO</b> — needs an {@code ItemStack} at every caller. Boot-only; the arithmetic it
     *     used to hold now lives in core.</td></tr>
     * <tr><td>{@code Quivers.stateOf}</td><td>reads five values, decides nothing</td>
     *     <td><b>NO</b> — needs an {@code ItemStack}. Boot-only; the decision it used to make has moved.</td></tr>
     * <tr><td>{@code WeaponItems.applyLore}</td><td>plumbs the stamp to the tooltip</td>
     *     <td><b>NO</b> — needs an {@code ItemStack}. Boot-only.</td></tr>
     * <tr><td>{@code QuiverItems.capacityIn}</td><td>the accessor; resolves nothing</td>
     *     <td>n/a — there is no stamp-versus-authored decision in it</td></tr>
     * </table>
     *
     * <h3>{@code RpgCommand} JOINED THIS LIST IN COMMIT 3, DELIBERATELY, AND IT IS THE ONLY ENTRY
     * THAT NEVER WRITES</h3>
     *
     * <p>{@code /rpg quiversize} prints the capacity the wielder will pack at, which is the
     * discipline every dev instrument here follows -- <i>a gate should read what to expect before it
     * starts watching.</i> To print it, it obtains one. <b>That is a new door, opened by the same
     * commit that declares it</b>, which is the shape commits 1b through 1f were spent learning: the
     * guard that does not scan its own new front door.
     *
     * <p>It is not a SECOND resolver, and the distinction is the reason it is allowed rather than
     * refactored away: it calls {@code QuiverSize.resolve} -- the same expression the write path
     * calls -- rather than re-deriving {@code authored + bonus} inline. A cap or a curve added to
     * {@code resolve} reaches the command's message for free. Writing
     * {@code weapon.quiverSize() + amount} there instead would have been the defect, and would have
     * been invisible to this scan.
     *
     * <p><b>The needle is {@code "QuiverSize.resolve("} and NOT {@code "QuiverSize."}</b>, measured:
     * the wide form also matches {@code QuiverSizeModifierItems}, whose {@code boosts} /
     * {@code contribution} / {@code arrows} calls obtain a BONUS, not a capacity. Including it would
     * put a file on this list that never resolves one, and a list whose membership stops meaning
     * "decides which capacity governs" stops answering that question -- the same dilution argument
     * that keeps {@code weapon.quiverSize()} off the needle set below.
     *
     * <p><b>THE import-static BAN HAS A POSITIVE CONTROL, BECAUSE A NEW FILTER THAT HAS ONLY EVER
     * SEEN PASSING INPUT HAS NEVER BEEN TESTED.</b> {@code MUTSTATICIMPORT}, run: add
     * {@code import static ...QuiverSize.resolve;} to {@code WeaponFire} and call
     * {@code resolve(weapon.quiverSize(), 0.0)} unqualified. Measured in the mutated file:
     * {@code QuiverSize.resolve(} occurs <b>ZERO</b> times, so the qualified needle was blind to it
     * exactly as predicted -- and the row still went red, {@code WeaponFire.java} appearing in the
     * actual list. <b>The catch came entirely from the new needle.</b> Restored byte-identical.
     *
     * <p>A SEVENTH FILE is a deliberate edit to this list, which is the moment to ask whether it should
     * instead be reading the state the others already built.
     *
     * <p><b>IT SCANS FOR THE ACCESSOR AS WELL AS THE RESOLVER, AND THE FIRST VERSION DID NOT —
     * WHICH IS WHY IT MISSED THE DEFECT IT WAS WRITTEN FOR.</b> Measured: a mutation adding
     * {@code QuiverItems.capacityIn(held, keys).orElse(weapon.quiverSize())} to {@code WeaponFire}
     * — the inline third resolver, exactly the case in the paragraphs above — came back **green**.
     * It calls neither {@code capacityOf} nor the key by name, so a scan for either was blind to it.
     * <b>A guard aimed at the name of the right thing rather than at the shape of the wrong thing.</b>
     * Obtaining a capacity at all now requires appearing on this list -- IN EITHER MODULE.
     *
     * <h2>{@code weapon.quiverSize()} IS DELIBERATELY NOT SCANNED FOR, AND HERE IS THE COST</h2>
     *
     * <p>It is the third source -- the authored value -- and it is a real way to obtain a capacity,
     * so the obvious hardening is to add {@code "quiverSize()"} to the {@code contains} above.
     * <b>Measured before deciding, not argued:</b> {@code grep -rn "quiverSize()" core/src/main/java
     * paper/src/main/java} finds five live call sites -- {@code QuiverItems:90}, {@code :98},
     * {@code :183}, {@code Quivers:168}, {@code WeaponLore:91} -- and <b>every one of them is already
     * inside a file on this list.</b> Scanning for it today would change no verdict.
     *
     * <p><b>And the two candidate needles do not match the same files -- measured with the scan's own
     * comment-stripping, not reasoned:</b>
     *
     * <table><tr><th>needle</th><th>files matched</th></tr>
     * <tr><td>{@code "quiverSize()"}</td><td>{@code QuiverItems}, {@code Quivers}, {@code WeaponLore}
     *     -- a SUBSET of this list; adding it changes no verdict today</td></tr>
     * <tr><td>{@code "quiverSize"}</td><td>those three <b>plus {@code WeaponDefinition} and
     *     {@code WeaponLoader}</b></td></tr>
     * </table>
     *
     * <p>The gap between the two rows is itself the finding. {@code WeaponDefinition} names
     * {@code quiverSize} on eight non-javadoc lines -- the record component, four validation arms, a
     * convenience factory's parameter and argument, and {@code hasQuiver()} -- and <b>not one of them
     * has parentheses</b>, because a record reads its own component as a field inside its body.
     * {@code WeaponLoader} holds it as a local {@code int} for the same reason. So the paren form is
     * narrow in precisely the way {@code capacityOf} was: <b>aimed at a name-form rather than a
     * shape</b>, and blind to the authoring code by accident of syntax rather than by decision.
     *
     * <p><b>Taking the wide needle is what is declined, and this is its price.</b> Both new files
     * AUTHOR the number; neither chooses between sources. A list whose membership no longer means
     * "decides which capacity governs" stops being readable as the answer to that question, which is
     * the property the five entries are for. Taking the narrow needle instead would buy a guard that
     * is green today for a reason that has nothing to do with the rule it states.
     *
     * <p><b>So the cost is stated rather than the risk denied:</b> a sixth file could call
     * {@code weapon.quiverSize()} directly, ignore the stamp, and this row would stay green. What
     * stops that is not this scan -- it is that the stamp has <b>exactly two readers outside
     * {@code QuiverItems} itself</b>, measured on {@code keys.quiverCapacity} across both modules:
     * {@code Quivers:155} and {@code WeaponItems:239}, and <b>both go through
     * {@code capacityIn}/{@code capacityInMeta}</b>, which IS the needle scanned above. So the
     * inline-resolver shape ({@code stamped.orElse(weapon.quiverSize())}) cannot be written anywhere
     * without landing on this list. A caller reaching for the authored value ALONE is not a second
     * resolver; it is a caller that never asked about the item at all, and the defect that produces
     * is {@code MUTSTATEDROP}'s -- a stamp that is not read -- which {@code QuiverStateTest} and
     * {@code WeaponLoreTest} witness directly.
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
                    // stripComments, shared with everyExternalReaderOfTheMagazineDeclaresWhetherItSettles
                    // rather than inlined here as it was until 2026-09-24. Two copies of one stripper is
                    // two things to drift, and that row's positive control is a claim about THE stripper
                    // -- which is only true if there is one.
                    String code = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                    // BOTH the accessor and the resolver, because scanning for capacityOf alone
                    // MISSED the defect this guard exists for -- measured, see the javadoc.
                    //
                    // AND THE LAST TWO NEEDLES ARE QUALIFIED, SO THEY ARE PAIRED WITH AN
                    // import-static BAN. "capacityIn" and "capacityOf(" are bare names and match
                    // however the call is spelled; "QuiverState.from(" and "QuiverSize.resolve("
                    // require the class, and `import static ...QuiverSize.resolve;` then
                    // `resolve(weapon.quiverSize(), bonus)` would obtain a capacity matching
                    // NEITHER. Measured: paper/src/main uses import static today (CraftingMenu 5,
                    // EnchantMenu 4), so this is a live idiom in the module this walks.
                    //
                    // The ban is one needle per class, not a broader scan: a file that static-imports
                    // either class LANDS ON THIS LIST and fails the membership assertion, which puts
                    // its author here, beside the reason.
                    if (code.contains("capacityIn") || code.contains("capacityOf(")
                            || code.contains("QuiverState.from(")
                            || code.contains("QuiverSize.resolve(")
                            || code.contains("import static "
                                    + "io.github.butterflysmp.rpg.core.weapon.QuiverState.")
                            || code.contains("import static "
                                    + "io.github.butterflysmp.rpg.core.combat.QuiverSize.")) {
                        resolvers.add(file.getFileName().toString());
                    }
                }
            }
        }
        assertTrue(scanned > 100, "only " + scanned + " files scanned across both modules");

        java.util.Collections.sort(resolvers);
        assertEquals(
                List.of("QuiverItems.java", "QuiverState.java", "Quivers.java", "RpgCommand.java",
                        "StatsSheetProjection.java", "WeaponItems.java", "WeaponLore.java"),
                resolvers,
                "a capacity may only be OBTAINED in these SEVEN files, across core AND paper. Reading "
                        + "QuiverItems.capacityIn anywhere else and resolving it inline -- "
                        + "stamped.orElse(weapon.quiverSize()) -- is a second resolver, and two "
                        + "resolvers is how the tooltip and the refusal logic come to disagree.");
    }

    /**
     * NO PUBLIC WAY TO BUILD A {@code QuiverState} CAN APPEAR UNNOTICED.
     *
     * <p><b>This row was called {@code noSixthWayToObtainACapacityCanAppearUnnoticed}, and that name
     * was false in the commit that wrote it.</b> It claimed EVERY way; it checked only the public
     * STATIC surface. The assertion message never overstated -- it said "QuiverState's public static
     * surface changed" -- so the name was the half that lied, and it lied about exactly the gap that
     * existed: {@code QuiverState} was a {@code public record}, <b>whose canonical constructor is
     * public</b>, so {@code new QuiverState(loaded, weapon.quiverSize(), from, to)} fabricated a
     * capacity past this pin and past the source scan above. That was the route {@code Quivers.stateOf}
     * itself used two commits ago.
     *
     * <p>Two ways to fix a name that overstates: shrink the name, or make it true. <b>Made true.</b>
     * {@code QuiverState} is now a final class with a PRIVATE canonical constructor, and this row
     * asserts that too -- so between the two assertions the name holds: a static factory and a
     * constructor are the only ways to obtain an instance, and both are pinned. Shrinking the name
     * instead would have left the record's door open with an honest label on the wrong door.
     *
     * <p><b>WITNESSED, not asserted. {@code MUTCTOR}:</b> {@code private QuiverState(} ->
     * {@code public QuiverState( // MUTCTOR}, spliced by line number; marker present 1, original
     * gone 0, +10 bytes (16291 -> 16301). <b>Result: 1 failure out of 6 rows -- this one, on its
     * second assertion.</b> The first assertion stayed green, which is the point worth recording:
     * a constructor is not a static method, so <b>the surface pin alone could not see the defect it
     * was named after</b>. Restored from a scratchpad copy, byte-identical, markers left 0.
     *
     * <p><b>SCOPE, because this row is named for what it watches and a reader will assume it watches
     * the rest.</b> It covers CONSTRUCTION only -- public static methods and public constructors.
     * The public INSTANCE surface is pinned separately by
     * {@link #theInstanceSurfaceOfQuiverStateIsNamedToo}; between them they cover every public
     * member declared on {@code QuiverState}, and nothing else. What neither covers is listed at
     * that row.
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
    void noPublicWayToBuildAQuiverStateCanAppearUnnoticed() {
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

        // AND THE CONSTRUCTOR IS NOT ON THIS SURFACE, WHICH IS WHY QuiverState IS NO LONGER A RECORD.
        // A public record's canonical constructor is public and would be a fifth entry point this
        // reflection cannot report. Made private instead -- see that class's own javadoc.
        assertTrue(Arrays.stream(QuiverState.class.getDeclaredConstructors())
                        .noneMatch(c -> Modifier.isPublic(c.getModifiers())),
                "QuiverState has a PUBLIC constructor. That is a way to fabricate a capacity that "
                        + "neither this pin nor the source scan can see -- it was the defect that "
                        + "made this class stop being a record.");
    }


    /**
     * THE INSTANCE SURFACE, PINNED TOO -- AND THE SCOPE STATEMENT THE PAIR OWES.
     *
     * <p>{@link #noPublicWayToBuildAQuiverStateCanAppearUnnoticed} covers CONSTRUCTION: public
     * static methods, and public constructors. <b>It does not cover the instance surface</b>, so
     * until this row existed, {@code loaded()} / {@code reloadStartedAt()} / {@code reloadCompletesAt()}
     * could come back, or {@link QuiverState#capacity} could quietly go, and neither guard would say
     * anything. That is the same gap as the module-scan one, one level in: <b>a pin's name says what
     * it watches, and a reader assumes it watches the rest.</b>
     *
     * <p><b>So, together, the two rows cover exactly:</b> public static methods, public constructors,
     * public instance methods -- all declared on {@code QuiverState} itself. <b>They do NOT cover:</b>
     * parameter and return TYPES (a member may change shape without either row noticing), anything
     * non-public, the {@code Fire} and {@code Reload} enum constants (which {@code QuiverStateTest}
     * exercises by value), and {@code Quiver} -- which has its own {@code QuiverSignatureTest}.
     *
     * <p>Why the instance surface is a door and not just tidiness: {@code capacity()} returns the
     * RESOLVED value, and that is the whole point. An accessor handing back the raw stamp and the
     * authored value separately -- {@code stampedCapacity()}, {@code authoredCapacity()} -- would let
     * a caller redo {@link QuiverState#capacityOf}'s job outside it, which is {@code MUTINLINE}
     * arriving through a fourth shape.
     *
     * <p><b>AND DECLARING {@code equals}/{@code hashCode}/{@code toString} WILL FAIL THIS ROW, ON
     * PURPOSE.</b> {@code getDeclaredMethods} does not report inherited {@code Object} members, so
     * they are absent from the list below -- which is exactly today's state, and the class javadoc's
     * cost note says why (equality is identity now, and this repo has zero hand-written
     * {@code equals}). Restoring value semantics is allowed; doing it silently is not. The author
     * lands here, beside the note that says which fields such an {@code equals} must cover.
     */
    @Test
    void theInstanceSurfaceOfQuiverStateIsNamedToo() {
        List<String> instanceMembers = Arrays.stream(QuiverState.class.getDeclaredMethods())
                .filter(m -> !m.isSynthetic() && Modifier.isPublic(m.getModifiers())
                        && !Modifier.isStatic(m.getModifiers()))
                .map(Method::getName)
                .distinct()
                .sorted()
                .toList();

        assertEquals(
                // roundsNeeded() joined in Slice E, and it is ALLOWED under this row's own rule --
                // which is worth stating here, because the failure message asks the question and the
                // answer belongs beside the set rather than in a commit nobody will find.
                //
                // The rule is that no accessor may hand back the RAW STAMP and the AUTHORED VALUE
                // separately, because a caller holding both can redo capacityOf's resolution outside
                // it. roundsNeeded() returns a single computed int -- the GAP between the resolved
                // capacity and the count -- and exposes neither input. It exists precisely so paper
                // does NOT need loaded() back to do the subtraction itself, which would have walked
                // straight through the door this row guards.
                // roundsRemaining() joined in Slice G, and THE OBJECTION COMES FIRST BECAUSE IT IS
                // WHAT A REVIEWER SEES: it IS loaded(). Algebraically capacity - (capacity - loaded)
                // = loaded, so it returns the accessor the record conversion removed, under a name
                // that describes the Dragon's Plume's use of it.
                //
                // IT IS ADMITTED UNDER THIS ROW'S OWN RULE. The guarded pair is the RAW STAMP beside
                // the AUTHORED VALUE, because a caller holding both can redo capacityOf's resolution
                // outside it. `loaded` is NEITHER of those two -- it is a third quantity -- and
                // capacity() already hands out the RESOLVED value, so what the pair recovers is
                // capacity() itself, which is public already. Nothing new becomes derivable.
                //
                // *** AND THE ADMISSION DEPENDS ON THE CLAMP, WHICH IS WHY THAT IS NOT A DETAIL. ***
                // roundsNeeded() + roundsRemaining() == capacity() holds for every STAMPED state
                // INCLUDING the over-full one -- 0 + 8 -- only because roundsRemaining() clamps.
                // Unclamped it would be 0 + 11, which recovers a number that is not the capacity and
                // is not anything else either, and the sentence above stops being true. Anyone
                // tempted to unclamp meets this comment before the red.
                //
                // Unstamped is the one exception and it fails SAFE: 0 + 0 recovers 0 rather than the
                // capacity -- less than the public surface already gives, never more.
                //
                // AND loaded()'s REMOVAL WAS COLLATERAL, NOT A PROHIBITION. The class javadoc
                // records it as one of three accessors lost to the record-to-class conversion with
                // ZERO CALLERS anywhere in either module, and calls the narrower surface an
                // improvement. "Nobody was using it" and "this must never come back" are different
                // sentences, and only the first one was written.
                List.of("capacity", "fireVerdict", "isReloading", "reloadTicksRemaining",
                        "reloadVerdict", "roundsNeeded", "roundsRemaining"),
                instanceMembers,
                "QuiverState's public INSTANCE surface changed. capacity() returns the RESOLVED "
                        + "value and is the only accessor anybody should need; an accessor handing "
                        + "back the raw stamp and the authored value separately lets a caller redo "
                        + "capacityOf's job outside it. If this failed because equals/hashCode/"
                        + "toString were declared, read the class javadoc's cost note first -- that "
                        + "is allowed, but it must name the fields it covers.");
    }
    /**
     * TODAY'S PUBLIC SURFACE, named rather than counted.
     *
     * <p><b>EVERY MEMBER BUT {@code stateOf} WRITES</b> -- {@code beginReload},
     * {@code resolveForShot}, {@code settleMaturedReload}, {@code spendRounds} and
     * {@code tryReloadHeldWeapon} -- and every one is verb-named so it cannot be mistaken for a query.
     * A new member added here is a deliberate edit to this list, which is the moment to ask whether its
     * name says what it does.
     *
     * <blockquote><b>THAT SENTENCE READ "Three of these four WRITE" UNTIL 2026-09-24, AND IT DISAGREED
     * WITH THE FOUR NAMES IN ITS OWN PARENTHESIS OVER A SET OF FIVE.</b> Two errors cancelling into one
     * plausible clause -- and the corrected form <b>names the set instead of counting it</b>, which is
     * this row's own stated rule applied to its own javadoc: <i>a count over an unnamed set cannot be
     * checked by the reader</i>. The count was checkable and wrong; nobody added the parts up.</blockquote>
     *
     * <blockquote><b>5 -&gt; 6 on 2026-09-24, for {@code settleMaturedReload}, and THE ROW FIRED BEFORE
     * THE LIST WAS EDITED.</b> The method was written first and this list left alone on purpose, to see
     * what the guard said: <i>"expected: &lt;[beginReload, resolveForShot, spendRounds, stateOf,
     * tryReloadHeldWeapon]&gt; but was: &lt;[beginReload, resolveForShot, settleMaturedReload,
     * spendRounds, stateOf, tryReloadHeldWeapon]&gt;"</i>. <b>That red IS this row's positive control</b>
     * -- editing both in one go produces the same green suite whether the row can see a new member or
     * not, which is the mechanism {@code NoticeThrottleKeysTest}'s own {@code 7 -> 8} bump records.</blockquote>
     *
     * <p><b>AND A RENAME IS ONE TOO, WHICH IS THIS ROW EARNING ITS KEEP RATHER THAN OBSTRUCTING.</b>
     * {@code spendRound} became {@code spendRounds} in slice H2b, when the Dragon's Plume's release
     * made a press cost up to five rounds instead of one. The set went red, the rename was looked at
     * rather than absorbed, and the plural turns out to be the honest name -- <b>a method called
     * {@code spendRound} taking a count would read as spending one round of a particular kind.</b>
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
                List.of("beginReload", "resolveForShot", "settleMaturedReload", "spendRounds",
                        "stateOf", "tryReloadHeldWeapon"),
                publicMethods,
                "Quivers' public surface changed. Every member but stateOf commits, so a new one "
                        + "needs a verb name that says so -- and a new PURE one belongs beside "
                        + "stateOf in the javadoc that points callers at it.");
    }

    /**
     * THE AUTHORED RELOAD DURATION IS READ ONLY WHERE THIS LIST SAYS -- and it is a list because a COUNT
     * of it was false within one commit of being written.
     *
     * <p>{@code Quivers.beginReload} carried the comment <i>"grep -rn "reloadTicks()" ... finds
     * exactly this one"</i>. True when drafted; <b>the same commit wrote its refutation sixty lines
     * away</b> in the {@code /rpg reloadtime} block. Measured at that tip: four hits in main source,
     * six unscoped. {@code PLAN-quiver-a2.md} repeated the count and was wrong the same way.
     *
     * <p>That is A1's <i>"two call sites"</i> error from the other side, and the rule drawn from it
     * then applies now: <b>name the set, do not count an unnamed one.</b> A grep quoted with its
     * command is the most trustworthy-looking form a count can take, which is exactly why a stale one
     * costs more -- the next reader runs it, gets a different number, and stops believing the
     * comments that are right.
     *
     * <h2>SUPPLY versus READOUT, which is the distinction that survives the next line being added</h2>
     *
     * <table><tr><th>file</th><th>role</th></tr>
     * <tr><td>{@code Quivers}</td><td><b>SUPPLY</b> -- reads the authored duration to DRIVE
     *     BEHAVIOUR. Exactly one site: {@code beginReload} resolves it and stamps the deadline.</td></tr>
     * <tr><td>{@code RpgCommand}</td><td><b>READOUT</b> -- reads it to show somebody a number, and
     *     prints the RESOLVED value beside the authored one every time, through
     *     {@code ReloadTime.resolve} rather than re-deriving {@code authored + bonus}.</td></tr>
     * <tr><td>{@code StatsSheetProjection}</td><td><b>READOUT</b>, and it is the {@code /rpg stats}
     *     readout that MOVED here rather than a new one. It joined this list the day the Nexus
     *     stats head needed the same ten numbers: the alternative was the hub re-deriving them,
     *     which is this row's defect on all ten at once. <b>Two surfaces render it now -- chat and
     *     an item tooltip -- and both read it from here</b>, so the set grew by a file and not by a
     *     source of truth.</td></tr>
     * </table>
     *
     * <p><b>A readout cannot quietly become a second source of truth</b> as long as it composes
     * through the same resolver, which is why {@code RpgCommand} is a listed member rather than an
     * exemption -- the same call the capacity scan makes about the same file.
     *
     * <p><b>This row is what makes commit 6 safe to write at all:</b> the stats sheet added a THIRD
     * readout in the same commit that corrected the count, so any fresh number would have been stale
     * on arrival for the third time. A named set absorbs it.
     *
     * <p>Scope, on {@link #theCapacityIsResolvedOnlyWhereThisListSays}'s pattern: MAIN source of both
     * modules. Test sources are excluded deliberately -- {@code WeaponQuiverDefinitionTest} and
     * {@code WeaponLoaderTest} both read {@code reloadTicks()} to assert a loaded value, which is
     * neither supply nor readout. The needle is the bare accessor name, so a static import cannot
     * walk past it the way it did the qualified capacity needles.
     */
    @Test
    void theAuthoredReloadDurationIsReadOnlyWhereThisListSays() throws IOException {
        List<String> readers = new java.util.ArrayList<>();
        int scanned = 0;
        for (Path root : List.of(Path.of("src", "main", "java"),
                                 Path.of("..", "core", "src", "main", "java"))) {
            assertTrue(Files.isDirectory(root), "source root not found: " + root.toAbsolutePath());
            try (var walk = Files.walk(root)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                    scanned++;
                    String raw = Files.readString(file, StandardCharsets.UTF_8);
                    String code = raw.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
                    if (code.contains("reloadTicks()")) {
                        readers.add(file.getFileName().toString());
                    }
                }
            }
        }
        assertTrue(scanned > 100, "only " + scanned + " files scanned across both modules");

        java.util.Collections.sort(readers);
        assertEquals(List.of("Quivers.java", "RpgCommand.java", "StatsSheetProjection.java"), readers,
                "the authored reload duration may only be read in these THREE files: Quivers SUPPLIES "
                        + "it (beginReload resolves and stamps), RpgCommand and StatsSheetProjection "
                        + "READ IT OUT beside the resolved value. A fourth file is a deliberate edit "
                        + "-- and the question to ask is which of the two it is, because a supply "
                        + "site that does not go through ReloadTime.resolve is a second source of "
                        + "truth and a readout that does not is a number that will drift from the "
                        + "weapon.");
    }

    /**
     * THE AUTHORED QUIVER SIZE IS READ ONLY WHERE THIS LIST SAYS -- the twin of the row above, and
     * <b>it exists because commit 3 measured this exact needle and DECLINED it on reasoning commit 6
     * refuted.</b>
     *
     * <h2>The reasoning, and why it was wrong</h2>
     *
     * <p>Commit 3 wrote: <i>"{@code "quiverSize()"} matches QuiverItems, Quivers, WeaponLore -- a
     * SUBSET of this list; adding it changes no verdict today."</i> The measurement was correct and
     * is correct still. <b>The conclusion drawn from it was the error.</b>
     *
     * <p><b>"It changes no verdict today" is the wrong test for a guard.</b> A guard's whole value is
     * the file nobody has written yet. This repository already carries the inverted form of that
     * principle, in {@code QuiverSize}'s javadoc: <i>"a filter with no call sites is not a filter that
     * is being applied."</i> Pointed the other way it reads: <b>a guard with nothing to catch today is
     * not a guard that will have nothing to catch.</b>
     *
     * <h2>And there is a worked example, one commit later, in the same file</h2>
     *
     * <p>The reload twin -- built on the same needle shape -- <b>caught {@code StatsSheetValues} on
     * its first run</b>, a file that did not exist when the needle was designed. It had accessors
     * named {@code quiverSize()} and {@code reloadTicks()}, both returning RESOLVED values under the
     * names {@code WeaponDefinition} uses for AUTHORED ones.
     *
     * <p><b>The same file had the same collision on BOTH sides, and only one of them was caught by a
     * guard.</b> The capacity half was fixed because its sibling tripped -- which is luck, and this
     * row is the removal of that luck.
     *
     * <h2>SUPPLY versus READOUT, measured at this tip</h2>
     *
     * <table><tr><th>file</th><th>role</th><th>sites</th></tr>
     * <tr><td>{@code QuiverItems}</td><td><b>SUPPLY</b></td>
     *     <td>{@code stampFull} stamps the AUTHORED capacity at MINT (deliberately -- the headroom
     *     rule), and {@code resolveCapacity} is the one resolution for a write.</td></tr>
     * <tr><td>{@code Quivers}</td><td><b>SUPPLY</b></td>
     *     <td>{@code stateOf} passes it as {@code QuiverState.from}'s unstamped fallback, which
     *     drives a verdict.</td></tr>
     * <tr><td>{@code WeaponLore}</td><td><b>READOUT</b></td>
     *     <td>the tooltip's unstamped fallback -- what a definitions-only harness renders.</td></tr>
     * <tr><td>{@code RpgCommand}</td><td><b>READOUT</b></td>
     *     <td>the stats sheet's gather and {@code /rpg quiversize}'s message, both composing through
     *     {@code QuiverSize.resolve} rather than re-deriving.</td></tr>
     * </table>
     *
     * <p><b>A SECOND ROW RATHER THAN A WIDENING OF {@link #theCapacityIsResolvedOnlyWhereThisListSays},
     * and the dilution argument from commit 3 is what decides it.</b> That row answers <i>who decides
     * which capacity governs</i>; this one answers <i>who touches the authored number at all</i>.
     * Folding the needle in would have mixed the two and left the first list unable to answer its own
     * question -- which was the true half of commit 3's reasoning, and it survives.
     *
     * <p>Its four files are a proper SUBSET of that row's six, today. That is expected and is not a
     * reason to merge them: subsets diverge exactly when somebody adds the file this row exists for.
     */
    @Test
    void theAuthoredQuiverSizeIsReadOnlyWhereThisListSays() throws IOException {
        List<String> readers = new java.util.ArrayList<>();
        int scanned = 0;
        for (Path root : List.of(Path.of("src", "main", "java"),
                                 Path.of("..", "core", "src", "main", "java"))) {
            assertTrue(Files.isDirectory(root), "source root not found: " + root.toAbsolutePath());
            try (var walk = Files.walk(root)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                    scanned++;
                    String raw = Files.readString(file, StandardCharsets.UTF_8);
                    String code = raw.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
                    if (code.contains("quiverSize()")) {
                        readers.add(file.getFileName().toString());
                    }
                }
            }
        }
        assertTrue(scanned > 100, "only " + scanned + " files scanned across both modules");

        java.util.Collections.sort(readers);
        assertEquals(
                List.of("QuiverItems.java", "Quivers.java", "RpgCommand.java",
                        "StatsSheetProjection.java", "WeaponLore.java"),
                readers,
                "the AUTHORED quiver size may only be read in these FIVE files. QuiverItems and "
                        + "Quivers SUPPLY it (a mint stamp, a write's resolution, an unstamped "
                        + "fallback that drives a verdict); WeaponLore, RpgCommand and "
                        + "StatsSheetProjection READ IT OUT. A "
                        + "sixth file is a deliberate edit, and the question to ask is which half it "
                        + "is in -- a supply site that does not go through QuiverSize.resolve is a "
                        + "second source of truth, and a readout that does not is a number that will "
                        + "drift from the weapon.");
    }

    /**
     * *** HOW MANY ROUNDS ONE PRESS COSTS -- AND THIS ROW EXISTS BECAUSE A MUTATION CAME BACK
     * GREEN. ***
     *
     * <p>2026-09-21, slice 14: {@code MUT14QUIVER7} changed {@code WeaponFire}'s spend from
     * {@code 1} to {@code 7} and <b>the entire suite stayed green at 2147 tests.</b> Nothing
     * anywhere could see a weapon billing seven rounds for one press. That is not a gap in the
     * quiver tests; it is the shape of the whole path -- {@code Quivers.spendRounds} needs a live
     * {@code Player} and a live {@code ItemStack}, so no module can call it.
     *
     * <p><b>The green run is the measurement and this row is the receipt.</b> The identical
     * mutation was re-applied after this was written and it reddened, which is what makes
     * "nothing else could see this" a measured claim rather than an assertion.
     *
     * <h2>WHAT THE RULE ACTUALLY IS, AND WHY THE SPREAD DOES NOT CHANGE IT</h2>
     *
     * <pre>
     * a plain press          1 round        every weapon
     * a YAW FAN              N rounds       the Plume's release -- N arrows, N rounds, ruled
     * a SPREAD               1 round        the Scatter Shot -- SEVEN bodies, ONE round, ruled
     * </pre>
     *
     * <p><b>The spread costs one BY CONSTRUCTION rather than by a second rule</b>, and that is the
     * property worth guarding: a spread is expanded inside {@code CastExecutor.launch}, below the
     * commit, so it never produces a {@code yawOffsets} array and this expression never sees it.
     * <b>Had the spread been wired as a fan, it would have billed seven rounds and nothing would
     * have said so.</b>
     *
     * <p>Anchored on the whole conditional rather than on {@code 1}, because a bare {@code 1} occurs
     * everywhere and a prefix would keep matching after a widening -- the failure mode
     * {@code heldScore(player, keys)} demonstrated by staying a prefix of its own replacement.
     */
    @Test
    void onePressSpendsOneRoundUnlessItIsAYawFan() throws IOException {
        Path weaponFire = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg",
                "paper", "weapon", "WeaponFire.java");
        String raw = Files.readString(weaponFire, StandardCharsets.UTF_8);
        assertTrue(raw.lines().count() > 200,
                "the scan did not read WeaponFire.java -- finding nothing here would make the"
                        + " assertion below pass by default");

        // Comments stripped: the spend site carries a paragraph naming both branches, so an
        // unfiltered match would report the expression present after somebody changed it.
        String code = raw.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");

        assertTrue(code.contains("yawOffsets == null ? 1 : yawOffsets.length"),
                "WeaponFire must spend ONE round for a press with no yaw fan, and yawOffsets.length"
                        + " for one that has. A spread is NOT a fan -- it is expanded below"
                        + " CastExecutor.commit and produces no offsets -- so the Scatter Shot's"
                        + " seven bodies cost one round through this very expression. Change it and"
                        + " a seven-body press bills seven rounds, which no other test in either"
                        + " module can see: measured 2026-09-21, MUT14QUIVER7, suite green at 2147.");
    }

    /**
     * EVERY EXTERNAL READER OF THE MAGAZINE IS NAMED HERE WITH ITS ROLE -- <b>settles</b>, or
     * <b>display only</b>.
     *
     * <h2>THE DEFECT, WHICH SHIPPED AND SURVIVED SEVEN WEEKS BECAUSE NOTHING NAMED THE RULE</h2>
     *
     * <p>{@link Quivers#stateOf} is PURE, and the class javadoc sells that as the safe property. For a
     * RENDER it is. <b>For a DECISION it is the hazard</b>: what it returns is the LOADED count, and a
     * matured-but-unsettled reload's rounds are in {@code quiver_reload_pending}, not in it.
     *
     * <p>{@code PlumeDraw.cap} read it and handed the number to {@code DrawRelease.decide}. Reload a
     * Dragon's Plume, let it mature without firing, draw, release: cap 0, {@code Nothing(NO_ROUNDS)},
     * <i>"your quiver is empty"</i> -- and <b>because the release refused, nothing reached
     * {@code WeaponFire} and nothing settled the reload.</b> The weapon stayed dead until a left-click.
     * On master since {@code e9e657a} (#78); reported by Ben on #147's boot.
     *
     * <p><b>A REFUSAL IS NOT A READ THAT SETTLES -- IT IS A READ THAT REMOVES THE REASON ANYTHING
     * WOULD LOOK AGAIN.</b> That is why the lazy design's <i>"the next read settles it"</i> guarantee
     * does not cover a deciding reader, and why the rule needs stating rather than deriving.
     *
     * <h2>WHAT THIS ROW CAN SEE, AND WHAT IT CANNOT -- BOTH MEASURED</h2>
     *
     * <p><b>It catches the two edits that would reproduce the defect</b>, and they are different edits:
     *
     * <pre>
     * a SIXTH reader appears        the membership list goes red, and its author has to write down
     *                              whether their site settles -- which is the question nobody put
     *                              when cap() was written.
     * the settle call is DELETED    the second assertion goes red. MEASURED: MUT-DRAWSETTLE (delete
     *                              PlumeDraw's Quivers.settleMaturedReload call -- delta 1 removed,
     *                              0 added against a pristine copy) left ALL 2170 ROWS GREEN before
     *                              this row existed, and reddens exactly this one now.
     * </pre>
     *
     * <p><b>IT CANNOT SEE MISPLACEMENT, AND THAT IS SAID RATHER THAN LEFT.</b> A settle call moved
     * BELOW the {@code stateOf} read, or guarded behind a condition that is never true, satisfies this
     * row exactly as the correct code does. <b>This is a presence check over source text, not a
     * behavioural witness</b> -- the behavioural witnesses are {@code GATE-quiver-feedback.md}'s
     * {@code P1} and {@code P2}, and neither substitutes for the other. Do not read a green suite here
     * as proof the call is in the right place.
     *
     * <p><b>AND IT IS BLIND INSIDE {@code Quivers} ITSELF</b>, because the needle is QUALIFIED and that
     * class calls its own method unqualified. Correct rather than a gap: {@code Quivers} owns both the
     * pure read and the settle, and is where the rule is authored. A reader that is not {@code Quivers}
     * is what this row is about.
     *
     * <h2>THE FULL WALK, INCLUDING THE SITES THIS NEEDLE DOES NOT REACH</h2>
     *
     * <p>The list this row asserts is <b>files that obtain a {@link QuiverState}</b>, which is narrower
     * than <i>everything that touches the magazine</i>. Measured 2026-09-24 across both modules'
     * main source, comments stripped, over
     * {@code Quivers.stateOf|QuiverItems.loadedIn|capacityIn|roundsRemaining()|resolveForShot|settleMaturedReload}:
     *
     * <pre>
     * ON THIS LIST
     *   PlumeDraw.cap           SETTLES       decides the cap, and via DrawRelease.decide whether
     *                                         the release fires at all. The #147 site.
     *   StatsBarSystem          SETTLES       the action-bar quiver field. Added 2026-09-24. It
     *     .heldMagazine                       DRAWS, and it settles anyway -- see below.
     *   QuiverSweep.reassert    DISPLAY ONLY  reloadTicksRemaining -> the hotbar overlay. Its own
     *                                         javadoc already forbids resolveForShot here.
     * NOT ON IT, AND WHY
     *   WeaponFire.attempt      SETTLES       via resolveForShot. Obtains no QuiverState itself.
     *   QuiverReloadCue.fire    SETTLES       calls settleMaturedReload and reads nothing. Added
     *                                         2026-09-24 so the magazine is full as the cue sounds.
     *   Quivers.*               owner         stateOf, spendRounds, finishReload -- unqualified.
     *   WeaponItems.applyLore   DISPLAY ONLY  loadedInMeta/capacityInMeta -> the tooltip.
     *   QuiverItems.carry*      TRANSPORT     copies the keys meta-to-meta at a re-mint.
     * </pre>
     *
     * <p><b>A site that SETTLES without obtaining a state is correctly absent</b>, because it cannot
     * commit the defect: there is no stale number for it to decide on. The list watches the readers.
     *
     * <h2>*** TWO DRAWING SURFACES, OPPOSITE ROLES, AND "DISPLAY ONLY" IS NOT THE CRITERION ***</h2>
     *
     * <p>{@code QuiverSweep} and {@code StatsBarSystem} both DRAW, and one settles while the other
     * must not. That looks like an inconsistency and is not; the criterion is narrower than
     * decide-versus-display:
     *
     * <pre>
     * SETTLE where settling CHANGES what you are about to show or decide.
     * DO NOT where it cannot.
     * </pre>
     *
     * <p><b>{@code QuiverSweep} draws from {@code reloadTicksRemaining}, which is 0 for a matured
     * reload whether or not it has been settled</b> -- so a settle there writes an item and changes
     * nothing drawn, which is precisely the <i>"commit a matured reload as a side effect of drawing a
     * progress bar"</i> its javadoc refuses. <b>The stats bar draws the COUNT, which is 0 before the
     * settle and full after.</b> There the settle is not a side effect of rendering; it is what makes
     * the render true.
     *
     * <p>So the rule stated at {@code Quivers} widens from <i>deciders</i> to <i>deciders and honest
     * readouts</i>, and {@code QuiverSweep} is untouched by the widening rather than grandfathered out
     * of it. The trigger for the HUD is concrete: reload, switch slots, let it mature unheld, switch
     * back -- nothing has settled it, and the bar would read {@code 0/25} until the player acted.
     *
     * <blockquote><b>THIS ROW CAUGHT {@code StatsBarSystem} BEFORE THE LIST WAS EDITED, WHICH IS ITS
     * POSITIVE CONTROL.</b> The HUD reader was written first and this list left alone, and it reported
     * <i>"expected: &lt;[PlumeDraw.java, QuiverSweep.java]&gt; but was: &lt;[PlumeDraw.java,
     * QuiverSweep.java, StatsBarSystem.java]&gt;"</i> -- a new reader forced to declare its role,
     * which is the whole job. Two days old and it has caught one.</blockquote>
     */
    @Test
    void everyExternalReaderOfTheMagazineDeclaresWhetherItSettles() throws IOException {
        // *** THE COMMENT STRIPPER'S POSITIVE CONTROL, FIRST, BECAUSE BOTH ASSERTIONS BELOW DEPEND ON
        // IT. *** This repo's javadocs quote their own call sites constantly -- PlumeDraw mentions
        // settleMaturedReload in a comment as well as calling it, measured 2 raw against 1 in code --
        // so an unstripped needle would find the prose describing the call and pass with the call gone.
        // That is the false-presence family, and the control is one line: strip a string that is ONLY a
        // comment and require the needle to vanish.
        String control = "/* settleMaturedReload */ // settleMaturedReload\nint x = 1;";
        assertFalse(stripComments(control).contains("settleMaturedReload"),
                "the comment stripper is not stripping. Every needle below then matches javadoc as "
                        + "well as code, so this row would pass over a file whose CALL had been "
                        + "deleted and whose comment about it survived.");
        assertTrue(stripComments(control).contains("int x = 1;"),
                "the comment stripper ate the CODE too, so every needle below is being run against "
                        + "nothing and would report a clean absence forever.");

        List<String> readers = new java.util.ArrayList<>();
        int scanned = 0;
        for (Path root : List.of(Path.of("src", "main", "java"),
                                 Path.of("..", "core", "src", "main", "java"))) {
            assertTrue(Files.isDirectory(root), "source root not found: " + root.toAbsolutePath());
            try (var walk = Files.walk(root)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                    scanned++;
                    String code = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                    // QUALIFIED, SO IT IS PAIRED WITH AN import-static BAN, exactly as
                    // theCapacityIsResolvedOnlyWhereThisListSays pairs its last two needles. A file
                    // doing `import static ...Quivers.stateOf;` then `stateOf(held, keys, weapon)`
                    // would be an external reader this needle cannot see -- so a static import of
                    // Quivers LANDS ON THIS LIST and fails the membership assertion, which puts its
                    // author here beside the reason. Measured: paper/src/main uses import static today
                    // (CraftingMenu 5, EnchantMenu 4), so the idiom is live in the module this walks.
                    //
                    // AnvilFace.stateOf and AnvilButton exist and are NOT quiver readers, which is why
                    // the needle cannot be the bare method name.
                    if (code.contains("Quivers.stateOf(")
                            || code.contains("import static "
                                    + "io.github.butterflysmp.rpg.paper.weapon.Quivers.")) {
                        readers.add(file.getFileName().toString());
                    }
                }
            }
        }
        assertTrue(scanned > 100, "only " + scanned + " files scanned across both modules");

        java.util.Collections.sort(readers);
        assertEquals(
                List.of("PlumeDraw.java", "QuiverSweep.java", "StatsBarSystem.java"),
                readers,
                "the set of files that obtain a QuiverState from outside Quivers has changed. Each one "
                        + "must declare, in its own comment, whether it SETTLES a matured reload or is "
                        + "DISPLAY ONLY. The test is NOT decide-versus-display -- it is whether "
                        + "settling CHANGES what you are about to show or decide:\n"
                        + "  PlumeDraw.cap    SETTLES -- it DECIDES (DrawRelease.decide refuses on it)\n"
                        + "  StatsBarSystem   SETTLES -- it DRAWS, but it draws the COUNT, which is 0 "
                        + "before the settle and full after\n"
                        + "  QuiverSweep      DISPLAY ONLY -- it draws from reloadTicksRemaining, "
                        + "which is 0 either way, so a settle there would change nothing drawn\n"
                        + "A reader that DECIDES and does not settle is the #147 defect again: a "
                        + "refusal taken on a stale magazine, which then stops anything looking "
                        + "again. Add your file here and write down which it is.");

        // THE DELETION WITNESS, AND IT IS THE HALF THE MEMBERSHIP LIST CANNOT PROVIDE. The list above
        // goes red when a SIXTH reader appears; it stays green when the settle call is removed from a
        // reader already on it -- which is the exact mutation that reproduces the shipped bug.
        String plume = stripComments(Files.readString(
                Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper",
                        "weapon", "PlumeDraw.java"),
                StandardCharsets.UTF_8));
        assertTrue(plume.contains("Quivers.settleMaturedReload("),
                "PlumeDraw no longer CALLS Quivers.settleMaturedReload in code. Its cap() decides "
                        + "whether a release fires, so reading the magazine without settling a matured "
                        + "reload first restores the #147 defect exactly: the Plume refuses a release "
                        + "as empty, the refusal is what stops anything settling the reload, and the "
                        + "weapon stays dead until a left-click. If cap() was legitimately restructured, "
                        + "point this assertion at whatever now settles before the read -- do not "
                        + "delete it.");
    }

    /**
     * Comments out, code in.
     *
     * <p><b>Used by {@link #everyExternalReaderOfTheMagazineDeclaresWhetherItSettles} and by
     * {@link #theCapacityIsResolvedOnlyWhereThisListSays}</b> -- the second was inlined until
     * 2026-09-24 and was pointed here so that the first row's positive control is a claim about the
     * stripper those two rows actually run.
     *
     * <p><b>FOUR OTHER ROWS IN THIS FILE STILL STRIP INLINE, AND THEY ARE MEASURED RATHER THAN
     * SWEPT.</b> Counted 2026-09-24 at four: {@code quiversNeverWritesTheCountWithoutRenderingIt},
     * {@code theCountKeyIsTouchedInExactlyTwoFilesAcrossAllOfPaper},
     * {@code theAuthoredReloadDurationIsReadOnlyWhereThisListSays} and
     * {@code theAuthoredQuiverSizeIsReadOnlyWhereThisListSays}.
     * <b>Not converted here, deliberately</b> -- this slice is a defect fix, and rewriting four
     * unrelated scan rows is how a revision regresses what it was not revising.
     *
     * <p><b>So the positive control above covers the two rows named, and NOT those four.</b> Said
     * because the alternative sentence -- <i>"this file strips comments in one place"</i> -- would be
     * a false claim about the other four, and a reader trusting it would take their control for
     * granted. Converting them is owed work with a checkable trigger: the count in this javadoc is
     * wrong the moment anybody does it, which is the reason it is written as a list.
     */
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
    }
}
