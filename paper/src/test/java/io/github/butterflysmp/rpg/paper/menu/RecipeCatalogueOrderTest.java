package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.CraftOrder;
import io.github.butterflysmp.rpg.core.weapon.SuggestionTier;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The catalogue's end of the shared ordering.
 *
 * <p><b>The ORDERING RULE itself is tested in {@code CraftOrderTest}, in core, and is deliberately
 * NOT re-tested here.</b> Two copies of those assertions would be two things to keep in step -- the
 * exact duplication the shared comparator was introduced to remove. What is local to the catalogue,
 * and so belongs here, is that {@code Entry} projects itself onto {@link CraftOrder} correctly and
 * that {@code ORDER} really is the shared rule rather than a lookalike.
 *
 * <p>Each test names the mutation it forces red.
 */
class RecipeCatalogueOrderTest {

    private static RecipeCatalogue.Entry entry(String key, SuggestionTier tier) {
        return new RecipeCatalogue.Entry(new NamespacedKey("minecraft", key), tier, null);
    }

    private static RecipeCatalogue.Entry armor(String key, ArmorSlot slot) {
        return new RecipeCatalogue.Entry(new NamespacedKey("minecraft", key),
                SuggestionTier.ARMOR, slot);
    }

    private static List<String> keys(List<RecipeCatalogue.Entry> entries) {
        List<String> out = new ArrayList<>();
        for (RecipeCatalogue.Entry entry : entries) out.add(entry.id().getKey());
        return out;
    }

    @Test
    void ORDERIsTheSHAREDComparatorAndNotACopyOfIt() {
        // THE assertion this file exists for. If someone re-inlines a local `tier -> key`
        // comparator here, the catalogue starts sorting armor alphabetically while the browser
        // sorts it head-down -- and armor is squeezed out of the three-cell column (Q16), so
        // nothing in play would show the disagreement.
        assertSame(CraftOrder.TIER_FIRST, RecipeCatalogue.ORDER,
                "the catalogue must USE the shared ordering, not reimplement it");
        // Mutation: ORDER = Comparator.comparing(...) -> reddens immediately.
    }

    @Test
    void entryProjectsItsNamespacedKeyOntoCraftOrdersStringKey() {
        // Entry's identity is a NamespacedKey; CraftOrder tiebreaks on a String. The projection is
        // the one piece of CraftOrder that Entry implements by hand, so it is the one piece that
        // can be wrong here.
        RecipeCatalogue.Entry mine = new RecipeCatalogue.Entry(
                new NamespacedKey("rpg", "sword"), SuggestionTier.WEAPON, null);

        assertEquals("rpg:sword", mine.key(), "the FULL key, namespace included");
        assertNull(new RecipeCatalogue.Entry(null, SuggestionTier.WEAPON, null).key(),
                "a null id must not throw from inside a sort");
        // Mutation: return id.getKey() (drops the namespace) -> reddens, and two plugins both
        // registering "sword" would compare equal and shuffle between rebuilds.
    }

    @Test
    void theNamespaceParticipatesInTheTiebreak() {
        // The consequence of the projection above, asserted through the comparator rather than
        // through the accessor, because that is where it matters.
        RecipeCatalogue.Entry mine = new RecipeCatalogue.Entry(
                new NamespacedKey("rpg", "sword"), SuggestionTier.WEAPON, null);
        RecipeCatalogue.Entry theirs = new RecipeCatalogue.Entry(
                new NamespacedKey("minecraft", "sword"), SuggestionTier.WEAPON, null);

        assertTrue(RecipeCatalogue.ORDER.compare(theirs, mine) < 0, "minecraft: sorts before rpg:");
        assertNotEquals(0, RecipeCatalogue.ORDER.compare(mine, theirs));
    }

    @Test
    void aRealisticCatalogueSortsGearFirstThenArmorHeadDownThenVanilla() {
        // One end-to-end shape over the catalogue's own record, so the wiring is exercised even
        // though the rule is proved in core. Keys chosen so alphabetical order disagrees with the
        // body-slot order.
        List<RecipeCatalogue.Entry> entries = new ArrayList<>(List.of(
                entry("stick", SuggestionTier.VANILLA),
                armor("leather_boots", ArmorSlot.FEET),
                entry("emberblade", SuggestionTier.WEAPON),
                armor("diamond_helmet", ArmorSlot.HEAD),
                entry("torch", SuggestionTier.VANILLA),
                armor("golden_leggings", ArmorSlot.LEGS),
                entry("bulwark", SuggestionTier.ACCESSORY),
                armor("iron_chestplate", ArmorSlot.CHEST),
                entry("iron_pickaxe", SuggestionTier.TOOL)));

        Collections.shuffle(entries, new Random(20260903L));
        entries.sort(RecipeCatalogue.ORDER);

        assertEquals(List.of(
                "emberblade",            // WEAPON
                "bulwark",               // ACCESSORY
                "iron_pickaxe",          // TOOL
                "diamond_helmet",        // ARMOR, head down...
                "iron_chestplate",
                "golden_leggings",
                "leather_boots",
                "stick",                 // VANILLA, by key
                "torch"), keys(entries));
        // Mutation: drop the armorSlot term -> the four armor pieces come out
        // boots, chestplate, helmet, leggings -- alphabetical, and it LOOKS deliberate -> reddens.
    }
}
