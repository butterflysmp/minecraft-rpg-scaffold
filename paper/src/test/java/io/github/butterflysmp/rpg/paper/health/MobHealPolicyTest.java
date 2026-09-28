package io.github.butterflysmp.rpg.paper.health;

import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * M15 (PLAN-mob-scaling.md slice 3): every vanilla heal on a TRACKED MOB is rerouted into its custom
 * store, proportionally. Nothing replaces a mob's regeneration the way {@code HealthRegenSystem} replaces
 * a player's, so no reason is cancelled without a replacement and none is passed.
 *
 * <p>The pinned paper-api's {@code RegainReason} has nine constants, read with {@code javap}: REGEN,
 * SATIATED, EATING, ENDER_CRYSTAL, MAGIC, MAGIC_REGEN, WITHER_SPAWN, WITHER and CUSTOM. That is exactly
 * the plan's eight plus its decided CUSTOM.
 */
class MobHealPolicyTest {

    @Test
    void everyRegainReasonIsREROUTEAndTheCountIsTheWholeEnum() {
        // The coverage row that survives a Paper upgrade: forReason has no default arm, so a tenth
        // constant does not compile; this row fails if the enum SHRINKS or grows without the policy.
        assertEquals(9, RegainReason.values().length, "the pinned jar's nine reasons");
        for (RegainReason reason : RegainReason.values()) {
            assertEquals(MobHealPolicy.Action.REROUTE, MobHealPolicy.forReason(reason), reason.name()
                    + ". Mutation: REGEN or ENDER_CRYSTAL moved to PASS -> reddens");
        }
    }

    @Test
    void theEndCrystalIsRerouted() {
        // M15's named case: "the crystals stay as strong as in vanilla".
        assertEquals(MobHealPolicy.Action.REROUTE, MobHealPolicy.forReason(RegainReason.ENDER_CRYSTAL));
    }

    @Test
    void CUSTOMIsRerouteForAMobWhereItIsPassForAPlayer() {
        // Decided, following the damage side's "rerouting honours it". For a player CUSTOM passes (a
        // future heal of ours). A mob's custom heals never go through vanilla health at all
        // (CombatantStats.heal writes the store), so a CUSTOM regain on a mob is somebody else's vanilla
        // heal, and dropping it would be the silent no-op.
        assertEquals(MobHealPolicy.Action.REROUTE, MobHealPolicy.forReason(RegainReason.CUSTOM));
        assertEquals(VanillaHealPolicy.Action.PASS, VanillaHealPolicy.forReason(RegainReason.CUSTOM),
                "the player side is unchanged, and the two policies differ here on purpose");
    }
}
