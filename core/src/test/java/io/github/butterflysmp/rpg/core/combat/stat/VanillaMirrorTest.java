package io.github.butterflysmp.rpg.core.combat.stat;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F20 (PLAN-mob-scaling.md §7): a tracked mob's VANILLA health mirrors its custom store as a fraction
 * of the vanilla max, so vanilla's own readers (the crystal and Regeneration gates, the boss bars, the
 * Wither's half-health phase, golem cracks) see the true fraction. M26: all of them come back.
 */
class VanillaMirrorTest {

    private static final UUID MOB = UUID.randomUUID();

    @Test
    void aFullStoreMirrorsToExactlyTheVanillaMax() {
        // F18's fix: at full, vanilla is EXACTLY its max, so both gated heals stop.
        assertEquals(16.0, VanillaMirror.healthFor(240, 240, 16).getAsDouble(), 0.0);
        assertEquals(200.0, VanillaMirror.healthFor(3000, 3000, 200).getAsDouble(), 0.0);
    }

    @Test
    void theMirrorIsTheCustomFractionOfTheVanillaMax() {
        // The gate rows' numbers: the spider (140 / 240 x 16) and the dragon (2500 / 3000 x 200).
        assertEquals(140.0 / 240.0 * 16.0, VanillaMirror.healthFor(140, 240, 16).getAsDouble(), 1e-12);
        assertEquals(2500.0 / 3000.0 * 200.0, VanillaMirror.healthFor(2500, 3000, 200).getAsDouble(), 1e-12);
        assertEquals(8.0, VanillaMirror.healthFor(120, 240, 16).getAsDouble(), 1e-12);
    }

    @Test
    void aTinyFractionIsFlooredAboveZeroSoVanillaCannotKillALiveMob() {
        // 1 / 240 x 16 = 0.0667 would be alive, but a 0.01 token or a vanilla hit would then kill a mob
        // our store says is alive. Mutation: delete the floor -> reddens.
        assertEquals(VanillaMirror.LIVE_FLOOR, VanillaMirror.healthFor(1, 240, 16).getAsDouble(), 0.0);
        assertEquals(VanillaMirror.LIVE_FLOOR, VanillaMirror.healthFor(0.001, 3000, 200).getAsDouble(), 0.0);
    }

    @Test
    void aVanillaMaxBelowTheFloorFloorsAtTheVanillaMax() {
        // setHealth above getMaxHealth() throws on the entity thread: the floor never exceeds the max.
        assertEquals(0.5, VanillaMirror.healthFor(1, 100, 0.5).getAsDouble(), 0.0);
    }

    @Test
    void anEmptyStoreWritesNothingBecauseDeathIsOurs() {
        // MobDeathSystem's setHealth(0) is the only thing that kills. A mirror that wrote the floor here
        // would revive the mob it just watched die. Mutation: write the floor at 0 -> reddens.
        assertTrue(VanillaMirror.healthFor(0, 240, 16).isEmpty());
        assertTrue(VanillaMirror.healthFor(-5, 240, 16).isEmpty());
        assertTrue(VanillaMirror.healthFor(Double.NaN, 240, 16).isEmpty());
    }

    @Test
    void aMissingOrNonPositiveMaxWritesNothingFailingSoft() {
        // Leave vanilla alone rather than write a number derived from nothing.
        assertTrue(VanillaMirror.healthFor(100, 0, 16).isEmpty());
        assertTrue(VanillaMirror.healthFor(100, Double.NaN, 16).isEmpty());
        assertTrue(VanillaMirror.healthFor(100, 240, 0).isEmpty());
        assertTrue(VanillaMirror.healthFor(100, 240, Double.NaN).isEmpty());
        assertTrue(VanillaMirror.healthFor(100, 240, -1).isEmpty());
    }

    @Test
    void theMirrorIsNeverAboveTheVanillaMaxOverAGrid() {
        // Craft's setHealth throws above getMaxHealth(). The whole grid, because a boundary error lands in
        // one cell. Mutation: vanillaMax replaced by customMax in the product -> reddens.
        double[] vanillaMaxes = {0.5, 1, 6, 16, 20, 49.6, 100, 200, 300, 500};
        double[] customMaxes = {1, 30, 240, 360, 1240, 3000, 4500, 12500};
        for (double v : vanillaMaxes) {
            for (double m : customMaxes) {
                for (int step = 1; step <= 20; step++) {
                    double current = m * step / 20.0;
                    OptionalDouble got = VanillaMirror.healthFor(current, m, v);
                    assertTrue(got.isPresent(), "live store " + current + "/" + m + " must mirror");
                    double h = got.getAsDouble();
                    assertTrue(h <= v, h + " above vanilla max " + v + " at " + current + "/" + m);
                    assertTrue(h >= Math.min(v, VanillaMirror.LIVE_FLOOR), h + " below the floor");
                    assertTrue(h > 0, "a live store never mirrors to 0");
                }
            }
        }
    }

    @Test
    void aStoreAboveItsMaxIsClampedToTheVanillaMax() {
        assertEquals(16.0, VanillaMirror.healthFor(300, 240, 16).getAsDouble(), 0.0);
    }

    @Test
    void everyMobKindMirrorsBecauseAHealMustRaiseVanillaToo() {
        // A lowering-only mirror fixes F20 and leaves F18. Mutation: HEAL excluded -> reddens.
        for (HealthChange.Kind kind : HealthChange.Kind.values()) {
            assertTrue(VanillaMirror.shouldMirror(change(kind, false, false)), kind.name());
        }
    }

    @Test
    void aPlayerNeverMirrorsBecauseHeartBarRendererOwnsTheirBar() {
        // Mutation: drop the player exclusion -> reddens.
        for (HealthChange.Kind kind : HealthChange.Kind.values()) {
            assertFalse(VanillaMirror.shouldMirror(change(kind, true, false)), kind.name());
        }
    }

    @Test
    void theKillingChangeNeverMirrors() {
        // reachedZero is MobDeathSystem's to act on, and the mirror must not race it.
        assertFalse(VanillaMirror.shouldMirror(change(HealthChange.Kind.DAMAGE, false, true)));
    }

    private static HealthChange change(HealthChange.Kind kind, boolean player, boolean reachedZero) {
        return new HealthChange(MOB, player, kind, 10.0, MOB, false, reachedZero ? 0.0 : 50.0, 100.0, reachedZero);
    }
}
