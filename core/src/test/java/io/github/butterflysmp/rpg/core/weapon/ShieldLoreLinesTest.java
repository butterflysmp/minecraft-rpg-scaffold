package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The shield tooltip's plain-text half.
 *
 * The headline is {@link #anOddFractionDoesNotLeakBinaryFloatingPointOntoTheItem}, and it is the
 * reason this class rounds at all. The shipped shield declares 0.35, which multiplies to a clean
 * 35.0 -- so a naive formatter passes every boot gate this slice will ever run and then prints
 * "Damage Reduction: 28.999999999999996%" the first time somebody authors 0.29.
 *
 * Every expected string below was produced by EXECUTING damageReductionLabel against the real class and
 * pasting what it printed, never by reasoning about the arithmetic -- the standing rule in
 * DefenseTest. The two values that motivated the rounding were measured BEFORE the method was
 * written: 0.29 * 100 == 28.999999999999996 and 0.55 * 100 == 55.00000000000001.
 *
 * Each test names the mutation it forces red.
 */
class ShieldLoreLinesTest {

    // --- The line -----------------------------------------------------------------------------

    @Test
    void theShippedShieldReadsAsJustOverAThird() {
        // What the boot gate reads off the item, at the SHIPPED value. 0.35 multiplies to a clean
        // 35.0, so like the 0.5 before it this passes without exercising the rounding at all --
        // which is exactly why anOddFraction... below is the headline rather than this.
        assertEquals("Damage Reduction: 35%", ShieldLoreLines.damageReductionLabel(0.35));
        assertEquals("35", ShieldLoreLines.blockPercent(0.35));
        assertEquals("35%", ShieldLoreLines.damageReductionValue(0.35));

        // Bulwark III takes the shipped shield to exactly 0.5 -- the base this slice moved AWAY
        // from -- so the old tooltip string is still reachable, now as an enchanted result.
        assertEquals("Damage Reduction: 50%", ShieldLoreLines.damageReductionLabel(0.5));
        // Mutation: drop the *100 -> "Damage Reduction: 0.35%", a shield advertising a third of a percent
        // -> reddens.
    }

    @Test
    void aWholePercentKeepsNoDecimalPoint() {
        // Trailing ".0" on every shield in the game is the kind of ugly nobody files a bug for and
        // everybody notices. The same trimNumber idiom WeaponLoreLines uses for resource costs.
        assertEquals("Damage Reduction: 0%", ShieldLoreLines.damageReductionLabel(0.0));
        assertEquals("Damage Reduction: 5%", ShieldLoreLines.damageReductionLabel(0.05));
        assertEquals("Damage Reduction: 25%", ShieldLoreLines.damageReductionLabel(0.25));
        assertEquals("Damage Reduction: 75%", ShieldLoreLines.damageReductionLabel(0.75));
        assertEquals("Damage Reduction: 100%", ShieldLoreLines.damageReductionLabel(1.0));
        // Mutation: return String.valueOf(rounded) without the floor check -> "Damage Reduction: 50.0%"
        // -> reddens.
    }

    @Test
    void aGenuinelyFractionalPercentSurvivesTheRounding() {
        // The reason the rounding keeps ONE decimal rather than zero. 12.5% is a real number an
        // author might want, not a floating-point artifact, and rounding to whole percents would
        // silently turn it into 13.
        assertEquals("Damage Reduction: 12.5%", ShieldLoreLines.damageReductionLabel(0.125));
        assertEquals("Damage Reduction: 33.3%", ShieldLoreLines.damageReductionLabel(0.333));
        // Mutation: round to whole percents (Math.round(percent)) -> "Damage Reduction: 13%" and "Damage Reduction: 33%",
        // both lying about the authored value -> reddens.
    }

    // --- The floating-point trap this class exists for ------------------------------------------

    @Test
    void anOddFractionDoesNotLeakBinaryFloatingPointOntoTheItem() {
        // THE headline. Measured before the formatter was written:
        //
        //   0.29 * 100  ==  28.999999999999996
        //   0.55 * 100  ==  55.00000000000001
        //
        // Trimmed without rounding, those print verbatim on the item. Neither is reachable from the
        // shipped content -- 0.35 multiplies cleanly -- so this defect would have passed the boot
        // gate and waited for the first author who typed an odd fraction.
        assertEquals("Damage Reduction: 29%", ShieldLoreLines.damageReductionLabel(0.29));
        assertEquals("Damage Reduction: 55%", ShieldLoreLines.damageReductionLabel(0.55));
        // Mutation: drop the Math.round(percent * 10) / 10.0 and trim the raw product ->
        // "Damage Reduction: 28.999999999999996%" -> reddens.
        //
        // RUN, and the exact blast radius recorded rather than guessed: that mutation reds THIS
        // test and aGenuinelyFractionalPercentSurvivesTheRounding (33.3 -> 33.300000000000004),
        // and leaves the three whole-percent tests GREEN -- including the shipped shield's 0.35.
        // The draft of this comment claimed it left every other test green; it does not. What
        // matters is the half that IS true: nothing reachable from shipped content catches it.
    }

    // --- The clamp, shared with the arithmetic --------------------------------------------------

    @Test
    void anOutOfRangeFractionIsClampedRatherThanAdvertisedAsWritten() {
        // The tooltip and the damage must never disagree. Both go through Shield.clamp, so an item
        // carrying a corrupt block_dr shows the block it will ACTUALLY get, not the one it claims.
        // A tooltip reading "Damage Reduction: -100%" would be the only visible sign of a shield that doubles
        // incoming damage, and it would be pointing at the wrong thing.
        assertEquals("Damage Reduction: 0%", ShieldLoreLines.damageReductionLabel(-1.0), "a negative advertises nothing");
        assertEquals("Damage Reduction: 100%", ShieldLoreLines.damageReductionLabel(2.0), "an over-full advertises full");
        assertEquals("Damage Reduction: 0%", ShieldLoreLines.damageReductionLabel(Double.NaN), "and NaN advertises nothing");
        // Mutation: format blockDr directly instead of Shield.clamp(blockDr) -> "Damage Reduction: -100%" and
        // "Damage Reduction: 200%", a tooltip that disagrees with the arithmetic it describes -> reddens.
    }
}
