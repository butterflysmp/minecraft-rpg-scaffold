package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.paper.health.VanillaHealPolicy.Action;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vanilla-heal policy, pinned across the WHOLE axis rather than the cases that happen to matter.
 *
 * <p>The handler itself needs a live entity and a live event and is boot-witnessed. This is the part
 * that is not: {@link RegainReason} is a plain enum and loads without a server, so the decision -- the
 * only thing in that handler that can be wrong in an interesting way -- is asserted exactly.
 *
 * <p>Each test names the mutation it forces red.
 */
class VanillaHealPolicyTest {

    /**
     * The expected action for every constant, written out. Not a set of "the interesting ones":
     * NEXT.md's rule is to enumerate the AXIS, and the axis here is nine constants long.
     */
    private static final Map<RegainReason, Action> EXPECTED = new EnumMap<>(Map.of(
            RegainReason.SATIATED, Action.CANCEL,
            RegainReason.REGEN, Action.CANCEL,
            RegainReason.MAGIC, Action.REROUTE,
            RegainReason.MAGIC_REGEN, Action.REROUTE,
            RegainReason.EATING, Action.REROUTE,
            RegainReason.CUSTOM, Action.PASS,
            RegainReason.ENDER_CRYSTAL, Action.PASS,
            RegainReason.WITHER_SPAWN, Action.PASS,
            RegainReason.WITHER, Action.PASS));

    @Test
    void everyRegainReasonIsCLASSIFIEDAndTheTableCoversTheWHOLEEnum() {
        // The coverage assertion is the one that survives a Paper upgrade. forReason has no default
        // arm, so a tenth constant will not COMPILE -- but the day someone silences that by adding a
        // default, this row is what still notices.
        assertEquals(RegainReason.values().length, EXPECTED.size(),
                "the expectation table has drifted from the enum -- a constant was added or removed, "
                        + "and an unclassified reason is one this policy has never had an opinion about");

        for (RegainReason reason : RegainReason.values()) {
            assertEquals(EXPECTED.get(reason), VanillaHealPolicy.forReason(reason),
                    "the action for " + reason);
        }
        // Mutation: move any single constant to a different arm -> reddens, naming the constant.
    }

    @Test
    void theTwoREGENSThisSliceReplacesAreTheOnlyCancelWITHOUTAReplacement() {
        // The rule: never cancel a heal you are not ready to replace. A bare CANCEL is only correct
        // where something else now does the healing -- which is true of exactly these two, because
        // HealthRegenSystem IS the passive heal now.
        for (RegainReason reason : RegainReason.values()) {
            if (VanillaHealPolicy.forReason(reason) != Action.CANCEL) continue;
            assertTrue(reason == RegainReason.SATIATED || reason == RegainReason.REGEN,
                    reason + " is cancelled with nothing replacing it -- a cancelled heal that is "
                            + "not rerouted is a SILENT NO-OP, which is worse than the flicker it "
                            + "replaced. Reroute it or pass it.");
        }
        // Mutation: change MAGIC from REROUTE to CANCEL -> a healing potion becomes a clean-looking
        // bug that heals zero -> reddens.
    }

    @Test
    void EATINGIsTreatedAsAPlayerHealBecauseItsReachabilityCannotBeREAD() {
        // The pinned API's javadoc calls EATING an ANIMAL reason; Bukkit's wider documentation calls
        // it a player one. Nothing in the constant list settles it for 26.1.2, and only runtime could.
        //
        // So it is grouped with MAGIC rather than with the boss/crystal reasons, which makes the arm
        // correct EITHER WAY: if it fires it is translated rather than leaking a heal that lies, and
        // if it never fires the arm is inert. Filing it under PASS would have been asserting a
        // mechanism instead of measuring one, and wrong in one of two ways with no way to tell which.
        assertEquals(Action.REROUTE, VanillaHealPolicy.forReason(RegainReason.EATING),
                "EATING is player-reachable for all we can prove, so it must not be passed through");
        assertNotEquals(VanillaHealPolicy.forReason(RegainReason.ENDER_CRYSTAL),
                VanillaHealPolicy.forReason(RegainReason.EATING),
                "and it must not share an arm with the genuinely-unreachable boss reasons -- that "
                        + "grouping is what would make PASS's justification false");
        // Mutation: move EATING into the ENDER_CRYSTAL/WITHER arm -> both rows redden.
    }

    @Test
    void CUSTOMPassesThroughSoThisIsNotTheSOLEWriterOfAPlayersHealth() {
        assertEquals(Action.PASS, VanillaHealPolicy.forReason(RegainReason.CUSTOM),
                "CUSTOM is how another plugin's heal arrives, and how our own would if anything ever "
                        + "routed through the event -- cancelling it would eat the unforeseen silently");
        // Mutation: CUSTOM -> CANCEL -> reddens.
    }
}
