package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The ONE ordering rule, shared by the column, the browser and the catalogue.
 *
 * <p>Each test names the mutation it forces red. The armor tests iterate {@link ArmorSlot#values()}
 * rather than listing four constants, so a fifth slot joins the assertion automatically instead of
 * slipping in below FEET unnoticed -- {@code CraftCountTest}'s tier loop is the pattern.
 */
class CraftOrderTest {

    /** A minimal {@link CraftOrder}, so these tests do not depend on {@code Craftable}'s shape. */
    private record Item(String key, SuggestionTier tier, ArmorSlot armorSlot) implements CraftOrder {}

    private static Item armor(String key, ArmorSlot slot) {
        return new Item(key, SuggestionTier.ARMOR, slot);
    }

    private static List<String> keys(List<? extends CraftOrder> items) {
        List<String> out = new ArrayList<>();
        for (CraftOrder item : items) out.add(item.key());
        return out;
    }

    // --- armor ordering --------------------------------------------------------------------------

    @Test
    void armorSortsHEADCHESTLEGSFEETWhateverTheirKeys() {
        // THE row. Keys chosen so ALPHABETICAL and BODY-SLOT order disagree completely: sorted by
        // key these are boots, chestplate, helmet, leggings -- which looks deliberate and is wrong.
        List<Item> items = new ArrayList<>(List.of(
                armor("minecraft:leather_boots", ArmorSlot.FEET),
                armor("minecraft:diamond_helmet", ArmorSlot.HEAD),
                armor("minecraft:golden_leggings", ArmorSlot.LEGS),
                armor("minecraft:iron_chestplate", ArmorSlot.CHEST)));

        items.sort(CraftOrder.TIER_FIRST);

        assertEquals(List.of("minecraft:diamond_helmet", "minecraft:iron_chestplate",
                "minecraft:golden_leggings", "minecraft:leather_boots"), keys(items));
        // Mutation: drop the armorSlot term so the tiebreak falls back to key -> boots, chestplate,
        // helmet, leggings -> reddens. That is the mutation the gate row is written against too.
    }

    @Test
    void theArmorOrderIsArmorSlotsDECLARATIONOrder() {
        // Asserted against the enum itself, never a hand-written list of four. ArmorSlot's javadoc
        // now promises "declaration order IS the ordering"; a literal list here would silently stop
        // tracking that promise, and a FIFTH slot would join the enum without joining this test.
        List<Item> items = new ArrayList<>();
        for (ArmorSlot slot : ArmorSlot.values()) {
            // keys deliberately REVERSE-alphabetical to the slot order, so a key-only tiebreak
            // cannot accidentally produce the right answer.
            items.add(armor("z" + (ArmorSlot.values().length - slot.ordinal()), slot));
        }
        Collections.shuffle(items, new Random(20260903L));
        items.sort(CraftOrder.TIER_FIRST);

        List<String> expected = new ArrayList<>();
        for (ArmorSlot slot : ArmorSlot.values()) {
            expected.add("z" + (ArmorSlot.values().length - slot.ordinal()));
        }
        assertEquals(expected, keys(items));
        assertEquals(4, ArmorSlot.values().length,
                "if a fifth slot lands, this is a deliberate product decision -- see ArmorSlot");
        // Mutation: reverse the armorSlot comparison -> reddens. Reordering the ENUM also reddens,
        // which is intended: it is a decision someone must take, not fall into.
    }

    @Test
    void armorOfTheSameSlotFallsBackToTheKey() {
        List<Item> items = new ArrayList<>(List.of(
                armor("minecraft:zinc_helmet", ArmorSlot.HEAD),
                armor("minecraft:alloy_helmet", ArmorSlot.HEAD)));

        items.sort(CraftOrder.TIER_FIRST);
        assertEquals(List.of("minecraft:alloy_helmet", "minecraft:zinc_helmet"), keys(items));
        // Mutation: drop the key term -> the two keep input order -> reddens. Without it, two
        // helmets shuffle between rebuilds and a player's memory of where one sits is worthless.
    }

    // --- tier ordering ---------------------------------------------------------------------------

    @Test
    void ALLGearSortsAheadOfALLVanilla() {
        List<Item> items = new ArrayList<>(List.of(
                new Item("stick", SuggestionTier.VANILLA, null),
                new Item("emberblade", SuggestionTier.WEAPON, null),
                armor("iron_helmet", ArmorSlot.HEAD),
                new Item("bulwark", SuggestionTier.ACCESSORY, null),
                new Item("iron_pickaxe", SuggestionTier.TOOL, null)));

        items.sort(CraftOrder.TIER_FIRST);
        assertEquals(List.of("emberblade", "bulwark", "iron_pickaxe", "iron_helmet", "stick"),
                keys(items));
        // Mutation: sort by key before tier -> reddens.
    }

    @Test
    void theTierOrderIsSuggestionTiersDECLARATIONOrder() {
        List<Item> items = new ArrayList<>();
        for (SuggestionTier tier : SuggestionTier.values()) {
            items.add(new Item("x_" + tier.name().toLowerCase(), tier, null));
        }
        Collections.shuffle(items, new Random(7L));
        items.sort(CraftOrder.TIER_FIRST);

        List<String> expected = new ArrayList<>();
        for (SuggestionTier tier : SuggestionTier.values()) expected.add("x_" + tier.name().toLowerCase());
        assertEquals(expected, keys(items));
    }

    // --- the arms that cannot happen, defined anyway ----------------------------------------------

    @Test
    void aNonArmorEntrySortsBelowEveryBodySlotAndDoesNotThrow() {
        // Unreachable in practice: within one tier either every member has a slot (ARMOR) or none
        // does. Defined rather than left to throw because a comparator that throws does so from
        // inside a sort deep in a click handler, and "unreachable today" is the assumption this
        // repo has been wrong about most often.
        List<Item> items = new ArrayList<>(List.of(
                armor("a_helmet", ArmorSlot.HEAD),
                new Item("b_nothing", SuggestionTier.ARMOR, null)));

        assertDoesNotThrow(() -> items.sort(CraftOrder.TIER_FIRST));
        assertEquals(List.of("b_nothing", "a_helmet"), keys(items),
                "NOT_ARMOR is -1, so it sorts below HEAD");
    }

    @Test
    void aNullKeyAndANullTierDoNotThrow() {
        List<Item> items = new ArrayList<>(List.of(
                new Item(null, null, null),
                new Item("real", SuggestionTier.WEAPON, null)));

        assertDoesNotThrow(() -> items.sort(CraftOrder.TIER_FIRST));
        assertEquals("real", items.get(0).key(), "an untiered entry sorts last, not first");
        // Mutation: drop either null guard -> NullPointerException from inside the sort -> reddens
        // as an error rather than a failure, which is still red and still correct.
    }

    // --- the property ------------------------------------------------------------------------------

    @Test
    void theOrderIsTOTALSoTheSameInputAlwaysGivesTheSameOutput() {
        // A comparator returning 0 for distinct entries leaves them in input order and passes any
        // single-shuffle test. This is what catches that.
        List<Item> source = List.of(
                armor("h", ArmorSlot.HEAD), armor("c", ArmorSlot.CHEST),
                armor("l", ArmorSlot.LEGS), armor("f", ArmorSlot.FEET),
                new Item("w", SuggestionTier.WEAPON, null),
                new Item("v", SuggestionTier.VANILLA, null));

        List<String> reference = null;
        for (long seed : new long[] {1L, 2L, 3L, 99L, 12345L}) {
            List<Item> shuffled = new ArrayList<>(source);
            Collections.shuffle(shuffled, new Random(seed));
            shuffled.sort(CraftOrder.TIER_FIRST);
            if (reference == null) reference = keys(shuffled);
            else assertEquals(reference, keys(shuffled), "seed " + seed + " sorted differently");
        }
        assertEquals(List.of("w", "h", "c", "l", "f", "v"), reference);
    }

    // --- the column keeps its own composition -------------------------------------------------------

    @Test
    void theCOLUMNStillLeadsWithCOUNTAndOnlyThenSharesTheTiebreak() {
        // The column and the browser share the WITHIN-TIER tiebreak, not a total order. If someone
        // "unified" them by giving the column TIER_FIRST, three cells would stop showing what the
        // player can make most of -- so the difference is asserted rather than left as a comment.
        List<CraftCount.Stock> stock = List.of(new CraftCount.Stock(1, 64));
        List<CraftCount.Candidate> candidates = List.of(
                new CraftCount.Candidate("a_few", SuggestionTier.VANILLA, List.of(List.of(1), List.of(1))),
                new CraftCount.Candidate("z_many", SuggestionTier.VANILLA, List.of(List.of(1))));

        List<CraftCount.Craftable> ranked = CraftCount.rank(candidates, stock);

        assertEquals(List.of("z_many", "a_few"), keys(ranked),
                "64 of z_many beats 32 of a_few, despite z sorting after a");
        // Mutation: replace RANKING with CraftOrder.TIER_FIRST -> "a_few" leads -> reddens.
    }

    @Test
    void theCOLUMNUsesTheSHAREDTiebreakForArmorRatherThanItsOwnKeyComparison() {
        // The whole point of the refactor. Armor is squeezed out of the three-cell column today
        // (gate row Q16), so if the column kept a private key-only tiebreak nothing in play would
        // reveal the disagreement -- which is exactly how two orderings that agree today get
        // written. Equal counts, so the tiebreak decides.
        //
        // THE KEYS ARE CHOSEN SO ALPHABETICAL AND BODY-SLOT ORDER DISAGREE, and that is not
        // decoration. The first version of this test used diamond_helmet (HEAD) and leather_boots
        // (FEET) -- where "d" < "l" AND head < feet, so both orderings give the same answer. The
        // mutation that swaps the shared tiebreak for a key-only one RAN GREEN against it. A test
        // that cannot fail is worth nothing however green; the mutation is what found it.
        List<CraftCount.Stock> stock = List.of(new CraftCount.Stock(1, 64));
        List<CraftCount.Candidate> candidates = List.of(
                new CraftCount.Candidate("minecraft:a_boots", SuggestionTier.ARMOR,
                        ArmorSlot.FEET, List.of(List.of(1))),
                new CraftCount.Candidate("minecraft:z_helmet", SuggestionTier.ARMOR,
                        ArmorSlot.HEAD, List.of(List.of(1))));

        List<CraftCount.Craftable> ranked = CraftCount.rank(candidates, stock);

        assertEquals(List.of("minecraft:z_helmet", "minecraft:a_boots"), keys(ranked),
                "the column must order armor head-down, NOT by key -- z_helmet sorts last "
                        + "alphabetically and first by body slot");
        assertEquals(ArmorSlot.HEAD, ranked.get(0).armorSlot(),
                "rank() must carry the slot through, not drop or default it");
        // Mutation: RANKING's final term back to `Craftable::key` -> a_boots first -> reddens.
        // Mutation: rank() passing null for armorSlot -> both assertions redden.
    }
}
