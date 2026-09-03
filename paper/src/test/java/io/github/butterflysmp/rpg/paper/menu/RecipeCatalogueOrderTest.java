package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.weapon.SuggestionTier;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The browser's ordering: tier first, recipe key second.
 *
 * <p>Building the catalogue needs a running server; deciding what order it comes out in does not,
 * and that is the half where the defects are. Each test names the mutation it forces red.
 */
class RecipeCatalogueOrderTest {

    private static RecipeCatalogue.Entry entry(String key, SuggestionTier tier) {
        return new RecipeCatalogue.Entry(new NamespacedKey("minecraft", key), tier, false);
    }

    private static List<String> keys(List<RecipeCatalogue.Entry> entries) {
        List<String> out = new ArrayList<>();
        for (RecipeCatalogue.Entry entry : entries) out.add(entry.id().getKey());
        return out;
    }

    @Test
    void ALLGearSortsAheadOfALLVanilla() {
        // THE invariant, and it is deliberately about the SORT rather than about which page things
        // land on. "Page 1 is the gear page" is arithmetic over two numbers that can both move.
        List<RecipeCatalogue.Entry> entries = new ArrayList<>(List.of(
                entry("stick", SuggestionTier.VANILLA),
                entry("emberblade", SuggestionTier.WEAPON),
                entry("torch", SuggestionTier.VANILLA),
                entry("iron_helmet", SuggestionTier.ARMOR),
                entry("bulwark", SuggestionTier.ACCESSORY),
                entry("iron_pickaxe", SuggestionTier.TOOL)));

        entries.sort(RecipeCatalogue.ORDER);

        assertEquals(List.of("emberblade", "bulwark", "iron_pickaxe", "iron_helmet", "stick", "torch"),
                keys(entries));
        // Mutation: sort by key BEFORE tier -> "bulwark, emberblade, iron_helmet, ..." -> reddens.
    }

    @Test
    void theTierOrderIsTheDeclarationOrderOfSuggestionTier() {
        // Asserted against the enum itself, not against a hand-written list, because
        // SuggestionTier's javadoc promises "declaration order IS the ordering" and this is the
        // second consumer of that promise. A hand-written list would silently stop tracking it.
        List<RecipeCatalogue.Entry> entries = new ArrayList<>();
        for (SuggestionTier tier : SuggestionTier.values()) {
            entries.add(entry("x_" + tier.name().toLowerCase(), tier));
        }
        Collections.shuffle(entries, new java.util.Random(20260903L));
        entries.sort(RecipeCatalogue.ORDER);

        List<String> expected = new ArrayList<>();
        for (SuggestionTier tier : SuggestionTier.values()) expected.add("x_" + tier.name().toLowerCase());
        assertEquals(expected, keys(entries));
        // Mutation: reverse the tier comparison -> reddens. Reordering the ENUM also reddens, which
        // is intended: it is a decision someone must take deliberately, not fall into.
    }

    @Test
    void withinATierTheKeyIsTheTIEBREAKAndItIsSTABLE() {
        List<RecipeCatalogue.Entry> entries = new ArrayList<>(List.of(
                entry("zircon_blade", SuggestionTier.WEAPON),
                entry("ashen_pike", SuggestionTier.WEAPON),
                entry("emberblade", SuggestionTier.WEAPON)));

        entries.sort(RecipeCatalogue.ORDER);
        assertEquals(List.of("ashen_pike", "emberblade", "zircon_blade"), keys(entries));
        // Mutation: drop the thenComparing -> the three keep their input order, so the assertion
        // reddens. Without the tiebreak, entries shuffle between rebuilds and a player's memory of
        // where something sits is worthless.
    }

    @Test
    void theSortIsTOTALSoTheSameInputAlwaysGivesTheSameOutput() {
        // The property behind "stable across restarts". Sorting from several different starting
        // orders must converge on one answer; a comparator that returned 0 for distinct entries
        // would leave them in input order and pass any single-order test.
        List<RecipeCatalogue.Entry> source = List.of(
                entry("b", SuggestionTier.WEAPON), entry("a", SuggestionTier.WEAPON),
                entry("c", SuggestionTier.VANILLA), entry("a", SuggestionTier.VANILLA),
                entry("d", SuggestionTier.TOOL));

        List<String> reference = null;
        for (long seed : new long[] {1L, 2L, 3L, 99L, 12345L}) {
            List<RecipeCatalogue.Entry> shuffled = new ArrayList<>(source);
            Collections.shuffle(shuffled, new java.util.Random(seed));
            shuffled.sort(RecipeCatalogue.ORDER);

            if (reference == null) reference = keys(shuffled);
            else assertEquals(reference, keys(shuffled), "seed " + seed + " sorted differently");
        }
        assertEquals(List.of("a", "b", "d", "a", "c"), reference,
                "two weapons, then the tool, then the two vanilla -- each pair keyed a before c/b");
        // Mutation: `thenComparing(e -> 0)` -> the seeds diverge -> reddens on the cross-seed
        // comparison, which no single sort could catch.
    }

    @Test
    void namespaceParticipatesInTheTiebreakNotJustTheKey() {
        // The comparator uses id().toString(), which is "namespace:key". Two plugins registering
        // "sword" must not compare equal -- that is the collision that makes an order unstable in
        // exactly the case a modded server hits and a vanilla one never does.
        RecipeCatalogue.Entry mine = new RecipeCatalogue.Entry(
                new NamespacedKey("rpg", "sword"), SuggestionTier.WEAPON, false);
        RecipeCatalogue.Entry theirs = new RecipeCatalogue.Entry(
                new NamespacedKey("minecraft", "sword"), SuggestionTier.WEAPON, false);

        assertTrue(RecipeCatalogue.ORDER.compare(theirs, mine) < 0, "minecraft: sorts before rpg:");
        assertNotEquals(0, RecipeCatalogue.ORDER.compare(mine, theirs));
        // Mutation: `thenComparing(e -> e.id().getKey())` (key only, dropping the namespace) ->
        // compare returns 0 -> both assertions redden.
    }
}
