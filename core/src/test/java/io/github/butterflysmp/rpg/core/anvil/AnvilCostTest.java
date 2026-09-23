package io.github.butterflysmp.rpg.core.anvil;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.xp.XpCurve;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The anvil's price list: authored in LEVELS, charged in POINTS.
 *
 * <h2>BOTH HALVES ARE ASSERTED, WHICH IS {@code EnchantCostTest}'S SHAPE AND ITS REASON</h2>
 *
 * The levels are asserted as literals AND the points are asserted as literals AND the derivation is
 * asserted to connect them. <b>Any one alone lets the other move unseen</b>: literals alone would
 * agree with a mutated curve, and the derivation alone would agree with a mutated table.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class AnvilCostTest {

    @Test
    void theSIXLEVELSAreBENSAuthoredTable_notDerivedFromAnything() {
        // LITERALS. There is no formula behind these and nothing here should suggest one -- they
        // are a designer's numbers, in the units a designer thinks in.
        assertEquals(10, AnvilCost.levelFor(Rarity.COMMON), "common at 10 levels");
        assertEquals(16, AnvilCost.levelFor(Rarity.UNCOMMON), "uncommon at 16");
        assertEquals(25, AnvilCost.levelFor(Rarity.RARE), "rare at 25");
        assertEquals(40, AnvilCost.levelFor(Rarity.EPIC), "epic at 40");
        assertEquals(60, AnvilCost.levelFor(Rarity.LEGENDARY), "legendary at 60");
        assertEquals(90, AnvilCost.levelFor(Rarity.EXOTIC), "exotic at 90");
        // Mutation MUT13A-COST: EPIC 40 -> 25 -> reddens here AND on the points row below, because
        // the two halves are pinned independently.
    }

    @Test
    void theSIXPRICESAreTheLEVELSPutThroughTheCURVE_derivedAndNotTyped() {
        // THE POINTS AS LITERALS. What the player is actually charged, and what the screen prints.
        assertEquals(160, AnvilCost.xpPoints(Rarity.COMMON));
        assertEquals(352, AnvilCost.xpPoints(Rarity.UNCOMMON));
        assertEquals(910, AnvilCost.xpPoints(Rarity.RARE));
        assertEquals(2920, AnvilCost.xpPoints(Rarity.EPIC));
        assertEquals(8670, AnvilCost.xpPoints(Rarity.LEGENDARY));
        assertEquals(24045, AnvilCost.xpPoints(Rarity.EXOTIC));

        // AND THE DERIVATION CONNECTS THEM -- without this the literals above are a second table
        // that could drift from the curve, which is the exact thing deriving them was meant to
        // prevent.
        int checked = 0;
        for (Rarity rarity : Rarity.values()) {
            assertEquals(XpCurve.totalForLevel(AnvilCost.levelFor(rarity)),
                    AnvilCost.xpPoints(rarity),
                    rarity + "'s price must BE its level put through the curve");
            checked++;
        }
        assertEquals(Rarity.values().length, checked, "the sweep has to have actually run");
        assertEquals(6, checked, "six tiers today");
    }

    /**
     * *** THE PRICES MUST ALL DIFFER, OR A TRANSPOSITION BETWEEN TWO TIERS IS INVISIBLE. ***
     *
     * <p>{@code CLAUDE.md}'s collision rule, applied to a table rather than a fixture: <i>"when two
     * independent quantities are equal, at least one of them is not being tested."</i> Two tiers
     * priced the same would make {@code MUT13A-COST} -- and any future swap of two rows -- a
     * mutation nothing can see.
     */
    @Test
    void everyTIERCostsADIFFERENTAmount_soNoTwoCanBeSwappedUnseen() {
        Set<Integer> prices = new HashSet<>();
        Set<Integer> levels = new HashSet<>();
        int checked = 0;
        for (Rarity rarity : Rarity.values()) {
            prices.add(AnvilCost.xpPoints(rarity));
            levels.add(AnvilCost.levelFor(rarity));
            checked++;
        }
        assertEquals(Rarity.values().length, prices.size(),
                "two tiers at one price makes a swap between them undetectable: " + prices);
        assertEquals(Rarity.values().length, levels.size(),
                "and the same at the level the table is authored in: " + levels);
        assertEquals(6, checked, "the sweep has to have actually run");
    }

    /**
     * *** THE LADDER ONLY GOES UP, AND IT IS THE ONE PROPERTY A PLAYER WILL NOTICE. ***
     *
     * <p>A rarer item costing LESS to upgrade would read as a bug the first time anyone compared
     * two screens, and it is exactly what a transposed pair of rows produces. Checked pairwise
     * across the declaration order, which {@link Rarity}'s own javadoc says is load-bearing.
     */
    @Test
    void theLADDERIsSTRICTLYIncreasing_aRarerItemNeverCostsLess() {
        Rarity[] tiers = Rarity.values();
        int checked = 0;
        for (int i = 1; i < tiers.length; i++) {
            assertTrue(AnvilCost.xpPoints(tiers[i]) > AnvilCost.xpPoints(tiers[i - 1]),
                    tiers[i] + " must cost more than " + tiers[i - 1] + ", but "
                            + AnvilCost.xpPoints(tiers[i]) + " <= "
                            + AnvilCost.xpPoints(tiers[i - 1]));
            checked++;
        }
        assertEquals(5, checked, "five steps between six tiers -- the sweep has to have run");
    }

    @Test
    void aMISSINGRarityIsAProgrammingErrorAndIsREFUSEDLOUDLY() {
        // Every GearDefinition carries a rarity, so a null here is a programming error rather than
        // bad data -- EnchantCost.xpPoints draws the same asymmetry against its clamping argument.
        assertThrows(IllegalArgumentException.class, () -> AnvilCost.levelFor(null));
        assertThrows(IllegalArgumentException.class, () -> AnvilCost.xpPoints(null));
    }

    /**
     * *** THE THREE FIGURES SHARED WITH {@code EnchantCost} ARE A COINCIDENCE OF THE CURVE. ***
     *
     * <p>352, 910 and 2920 appear in both tables because 16, 25 and 40 are levels both authors
     * chose. <b>Neither table reads the other</b>, and this row exists so that nobody "removes the
     * duplication" by pointing one at the other -- they are independent design decisions that
     * happen to have landed on three of the same rungs.
     */
    @Test
    void theOVERLAPWithTheENCHANTTableIsACoincidence_andNeitherReadsTheOther() {
        assertEquals(XpCurve.totalForLevel(16), AnvilCost.xpPoints(Rarity.UNCOMMON));
        assertEquals(XpCurve.totalForLevel(25), AnvilCost.xpPoints(Rarity.RARE));
        assertEquals(XpCurve.totalForLevel(40), AnvilCost.xpPoints(Rarity.EPIC));

        // AND THE OTHER THREE ARE RUNGS THE ENCHANT TABLE DOES NOT USE, which is what makes the
        // independence visible rather than asserted: an anvil table copied from the enchant one
        // could not have produced 10, 60 or 90.
        assertEquals(XpCurve.totalForLevel(10), AnvilCost.xpPoints(Rarity.COMMON));
        assertEquals(XpCurve.totalForLevel(60), AnvilCost.xpPoints(Rarity.LEGENDARY));
        assertEquals(XpCurve.totalForLevel(90), AnvilCost.xpPoints(Rarity.EXOTIC));
    }
}
