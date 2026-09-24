package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EVERY RELOAD KEY THAT IS STAMPED IS ALSO CARRIED ACROSS A RE-MINT.
 *
 * <h2>THE DEFECT THIS WAS WRITTEN FOR, AND IT WAS LIVE ON master</h2>
 *
 * <p>{@code quiverReloadPending} -- the rounds a running reload will deliver -- shipped in Slice E.
 * It was added to the WRITE site in {@code Quivers.beginReload} and <b>not</b> to
 * {@code QuiverItems.carry}, while its two siblings (the start and the deadline) were carried. So a
 * re-mint produced a reload with a deadline and no payload, and:
 *
 * <ol>
 *   <li>the arrows are taken at the START of a reload, not at maturity (RULING 3);</li>
 *   <li>a re-mint happens on <b>every join</b> -- {@code RpgListeners.onJoin} calls
 *       {@code GearRefresher.refresh}, and {@code WeaponItems.remint} builds a FRESH item, copying
 *       only what {@code carryInstanceData} copies;</li>
 *   <li>{@code Quivers.pendingRounds} answers {@code 0} for an absent key, and
 *       {@code Quiver.reload(loaded, 0, capacity)} is the magazine unchanged.</li>
 * </ol>
 *
 * <p><b>So: reload, relog, and the arrows were gone with nothing to show for them.</b> Silent, and
 * the mirror image of the relog-to-refill exploit {@code QuiverItems.carry}'s javadoc exists to
 * prevent.
 *
 * <h2>WHY A SIGNATURE SCAN RATHER THAN A BEHAVIOURAL ROW</h2>
 *
 * <p>{@code carry} takes two {@code ItemMeta}, which needs a running server, so the behaviour is
 * boot-only. <b>What is checkable here is the RULE</b>: the set of reload keys and the set of carried
 * keys must be equal. That generalises past the one bug -- a fourth reload key added to the stamp and
 * forgotten here fails this row -- which a single behavioural assertion about {@code pending} would
 * not.
 */
class QuiverReloadCarrySignatureTest {

    private static final Path KEYS = Path.of("src", "main", "java", "io", "github", "butterflysmp",
            "rpg", "paper", "adapter", "Keys.java");
    private static final Path QUIVER_ITEMS = Path.of("src", "main", "java", "io", "github",
            "butterflysmp", "rpg", "paper", "weapon", "QuiverItems.java");

    /**
     * The reload keys, declared in {@code Keys}, must every one of them be written by
     * {@code QuiverItems.carry}.
     *
     * <p><b>Two positive controls, because a scan that finds nothing reads exactly like a scan that
     * found nothing wrong.</b> The key list must be non-empty, and the extracted method body must
     * contain {@code quiverLoaded} -- a key known to be carried, which proves the extraction found the
     * real method and not an empty string.
     *
     * <p>Mutation: delete the {@code quiverReloadPending} write from {@code carry} -> this reddens
     * naming that key. Restore it -> green. Measured both ways.
     */
    @Test
    void everyReloadKeyDeclaredInKeysIsCarriedAcrossAReMint() throws IOException {
        List<String> reloadKeys = declaredReloadKeys();
        assertFalse(reloadKeys.isEmpty(),
                "found NO quiverReload* keys in Keys.java -- the scan is broken, not the code");
        assertEquals(3, reloadKeys.size(),
                "expected the three reload keys (started, completes, pending), found " + reloadKeys);

        String carry = carryBody();
        assertTrue(carry.contains("target.set(keys.quiverLoaded"),
                "control: the extracted carry() body does not WRITE quiverLoaded, a key that is "
                        + "certainly carried -- so the extraction, not the code, is what failed");

        // *** ANCHORED TO THE WRITE, NOT TO THE KEY'S NAME. THE FIRST DRAFT WAS NOT, AND IT WAS
        // *** HOLLOW -- measured, by deleting the pending write and watching this row stay GREEN.
        //
        // carry()'s body names quiverReloadPending three times: twice in the comment recording the
        // defect, and once in the source read. A `contains(key)` test therefore matched THE PROSE
        // DESCRIBING THE BUG and reported the bug as fixed. That is the false presence this repo
        // records at `grep -l "applies_status"` finding a file whose comment says it declares none --
        // and it is worse here, because the comment was added by the same commit as the fix.
        //
        // `target.set(keys.<key>` is the write and only the write: `source.get` cannot match it, and
        // no sentence about a key contains it.
        List<String> missing = new ArrayList<>();
        for (String key : reloadKeys) {
            if (!carry.contains("target.set(keys." + key)) missing.add(key);
        }
        assertTrue(missing.isEmpty(),
                "QuiverItems.carry does not carry " + missing + ". A reload key that is stamped and "
                        + "not carried survives a re-mint as half a reload: the arrows are spent at "
                        + "the start, so the player pays and receives nothing. Carry it beside the "
                        + "deadline, or stop stamping it.");
    }

    /** Every {@code public final NamespacedKey quiverReload...} field name declared in Keys. */
    private static List<String> declaredReloadKeys() throws IOException {
        assertTrue(Files.isRegularFile(KEYS), "source not found at " + KEYS.toAbsolutePath());
        String raw = Files.readString(KEYS, StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("public final NamespacedKey (quiverReload\\w+);").matcher(raw);
        List<String> found = new ArrayList<>();
        while (m.find()) found.add(m.group(1));
        return found;
    }

    /**
     * The body of {@code QuiverItems.carry}, bounded STRUCTURALLY.
     *
     * <p>From the declaration to the first line that is exactly a method-level closing brace. <b>Not
     * a line count</b>: a bound of "the next 30 lines" is an assertion about comment length, and this
     * repo has watched that drift twice as javadoc grew around unmoved code.
     */
    private static String carryBody() throws IOException {
        assertTrue(Files.isRegularFile(QUIVER_ITEMS),
                "source not found at " + QUIVER_ITEMS.toAbsolutePath());
        String raw = Files.readString(QUIVER_ITEMS, StandardCharsets.UTF_8);
        int start = raw.indexOf("public static void carry(ItemMeta from");
        assertTrue(start >= 0, "QuiverItems.carry(ItemMeta, ItemMeta, Keys) not found -- renamed?");
        int end = raw.indexOf("\n    }", start);
        assertTrue(end > start, "no method-level closing brace after carry's declaration");
        return raw.substring(start, end);
    }
}
