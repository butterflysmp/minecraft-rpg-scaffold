package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The stat sheet's text half: the three unit conventions, and the labels.
 *
 * <p><b>Every expected string was produced by EXECUTING the expression, not by reasoning about
 * {@code String.format} or {@code Math.round}.</b> One of them is the reason this class exists at
 * all: {@code GearLoreLines.trimNumber} on the base mana rate returns
 * {@code "1.6666666666666665"}, which is asserted below so nobody re-adopts it for rates.
 *
 * <p>Each test names the mutation it forces red.
 */
class StatsSheetLinesTest {

    /** The shipped base mana rate, per tick, written exactly as RpgPlugin writes it. */
    private static final double MANA_PER_TICK = 100.0 / (60 * 20);

    // --- Rates ------------------------------------------------------------------------------------

    @Test
    void aRateGetsTwoDecimalsAndItsUNITBecauseTrimNumberWouldPrintSixteenDigits() {
        assertEquals("0.20/s", StatsSheetLines.perSecond(HealthRegen.BASE_PER_SECOND),
                "the base health regen, 1 HP every 5 seconds");
        assertEquals("1.67/s", StatsSheetLines.perSecond(ManaRegen.perSecond(MANA_PER_TICK)),
                "the base mana regen -- a full bar in 60 seconds");
        assertEquals("2.67/s",
                StatsSheetLines.perSecond(ManaRegen.perSecond(MANA_PER_TICK + ManaRegen.perTick(1.0))),
                "and with the +1.0/s fixture held");

        // THE reason rates do not use the lore trimmer. Executed, and it is not close.
        assertEquals("1.6666666666666665",
                io.github.butterflysmp.rpg.core.weapon.GearLoreLines.trimNumber(
                        ManaRegen.perSecond(MANA_PER_TICK)),
                "trimNumber falls back to String.valueOf, so a rate would ship sixteen digits");
        // Mutation: have perSecond delegate to trimNumber -> the first three rows redden.
        // Mutation: drop the "/s" suffix -> all three redden.
    }

    @Test
    void perSecondFORMATSAndNeverCONVERTSSoTheTickToSecondHomeStaysSingular() {
        // Handed a per-second value it prints it unchanged. If this method also divided or multiplied
        // by 20, there would be two places that know the tick rate and they would eventually disagree.
        assertEquals("1.00/s", StatsSheetLines.perSecond(1.0));
        assertEquals("20.00/s", StatsSheetLines.perSecond(20.0));
        // Mutation: multiply by 20 inside perSecond -> "20.00/s" and "400.00/s" -> reddens.
    }

    // --- Fractions: the two that are NOT what they look like ---------------------------------------

    @Test
    void critChanceIsAPROBABILITYRenderedAsAPercentAndClampedWhereCombatClampsIt() {
        assertEquals("15%", StatsSheetLines.critChance(Crit.BASE_CHANCE), "0.15 is fifteen percent");
        assertEquals("50%", StatsSheetLines.critChance(0.5));
        assertEquals("0%", StatsSheetLines.critChance(0.0), "a mob, or a player with the stat zeroed");
        assertEquals("100%", StatsSheetLines.critChance(1.5),
                "clamped through Crit.chance -- gear past 100% crits every hit, so the sheet must "
                        + "report the ceiling combat actually applies, not 150%");
        // Mutation: print the raw probability -> "0.15" not "15%" -> reddens.
        // Mutation: clamp with a local Math.min instead of Crit.chance -> the 1.5 row still passes,
        //   which is why the row exists at all; use Crit.chance so one clamp serves both.
    }

    @Test
    void critDamageIsABONUSSoTheSheetShowsOnePlusItJustAsCombatMultipliesByOnePlusIt() {
        assertEquals("2.00x", StatsSheetLines.critDamage(Crit.BASE_DAMAGE),
                "the base bonus of 1.0 means a crit does DOUBLE -- printing the raw 1.0 would tell a "
                        + "player their crits do nothing");
        assertEquals("2.50x", StatsSheetLines.critDamage(1.5), "a +0.5 item");
        assertEquals("1.00x", StatsSheetLines.critDamage(0.0),
                "a zeroed bonus is a crit that multiplies by one -- correct, and worth showing");
        // This is the same 1 + bonus Crit.multiplier applies, so the sheet cannot claim a different
        // crit than the swing delivers.
        // Mutation: drop the 1.0 + -> "1.00x" for the base -> reddens.
    }

    // --- Capacities --------------------------------------------------------------------------------

    @Test
    void aCapacityKeepsWholeNumbersWHOLEAndDoesNotInventDecimals() {
        assertEquals("100", StatsSheetLines.capacity(100), "a round max reads round");
        assertEquals("137.5", StatsSheetLines.capacity(137.5), "and a fractional one is not hidden");
        assertEquals("0", StatsSheetLines.capacity(0), "no defense is 0, not 0.00");
        // Deliberately DIFFERENT from StatsBarText, which Math.rounds 137.5 to "138" because it is a
        // glanceable HUD. The sheet is the precise view; they answer different questions.
        // Mutation: format capacities with two decimals -> "100.00" -> reddens.
    }

    // --- Labels -------------------------------------------------------------------------------------

    @Test
    void theEightLabelsAreAllDISTINCTSoNoLineCanWearAnothersName() {
        List<String> labels = List.of(
                StatsSheetLines.MAX_HEALTH_LABEL, StatsSheetLines.HEALTH_REGEN_LABEL,
                StatsSheetLines.MAX_MANA_LABEL, StatsSheetLines.MANA_REGEN_LABEL,
                StatsSheetLines.DEFENSE_LABEL, StatsSheetLines.DAMAGE_LABEL,
                StatsSheetLines.CRIT_CHANCE_LABEL, StatsSheetLines.CRIT_DAMAGE_LABEL);

        assertEquals(8, labels.size(), "eight lines, as designed");
        assertEquals(8, Set.copyOf(labels).size(),
                "and eight DISTINCT names -- eight near-identical lines is exactly where a "
                        + "copy-pasted constant hides");
        // Mutation: reuse MAX_HEALTH_LABEL for max mana -> the distinctness row reddens.
    }

    @Test
    void aLabelIsPaddedToOneWidthSoTheValueColumnStartsInOnePlace() {
        assertEquals("Max Health   ", StatsSheetLines.label(StatsSheetLines.MAX_HEALTH_LABEL));
        assertEquals("Health Regen ", StatsSheetLines.label(StatsSheetLines.HEALTH_REGEN_LABEL));
        assertEquals("Defense      ", StatsSheetLines.label(StatsSheetLines.DEFENSE_LABEL));

        for (String label : List.of(
                StatsSheetLines.MAX_HEALTH_LABEL, StatsSheetLines.HEALTH_REGEN_LABEL,
                StatsSheetLines.MAX_MANA_LABEL, StatsSheetLines.MANA_REGEN_LABEL,
                StatsSheetLines.DEFENSE_LABEL, StatsSheetLines.DAMAGE_LABEL,
                StatsSheetLines.CRIT_CHANCE_LABEL, StatsSheetLines.CRIT_DAMAGE_LABEL)) {
            assertEquals(StatsSheetLines.LABEL_WIDTH, StatsSheetLines.label(label).length(),
                    label + " pads to the common width");
        }
        // Equal CHARACTER counts, not equal pixel widths -- Minecraft chat is proportional, so the
        // column is predictably slightly ragged rather than arbitrarily so. The boot gate looks at it.
        // Mutation: return the label unpadded -> every row reddens.
    }
}
