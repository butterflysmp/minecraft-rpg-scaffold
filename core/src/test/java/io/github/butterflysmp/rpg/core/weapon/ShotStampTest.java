package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE STAMP A VANILLA-DRIVEN SHOT CARRIES TO ITS HIT (the seat's ruling 10, 2026-11-22): the weapon id, the force, and the
 * damage terms and score snapshotted at the shot, as a cast snapshots them. It is written into the arrow's persistent
 * data as one string, so it survives a chunk unload or a restart with the arrow.
 */
class ShotStampTest {

    /** Every field distinct, so a transposition between any two cannot pass. */
    private static ShotStamp stamp() {
        return new ShotStamp("bow", 0.75, 14.0, 3.0, 12.5, 2.0, 49.0, 250,
                java.util.List.of(new io.github.butterflysmp.rpg.core.enchant.ActiveEnchant("tremors", 2),
                        new io.github.butterflysmp.rpg.core.enchant.ActiveEnchant("other", 1)));
    }

    @Test
    void itRoundTripsThroughItsString() {
        ShotStamp stamp = stamp();
        assertEquals(stamp, ShotStamp.decode(stamp.encode()).orElseThrow());
    }

    @Test
    void anAwkwardDoubleRoundTripsExactly() {
        ShotStamp stamp = new ShotStamp("crossbow", 0.1 + 0.2, 37.0, 0.0, 0.0, 1.0, 0.0, 100, java.util.List.of());
        assertEquals(0.1 + 0.2, ShotStamp.decode(stamp.encode()).orElseThrow().force(), 0.0);
    }

    @Test
    void aMalformedStampIsAbsentNeverAGuess() {
        assertTrue(ShotStamp.decode(null).isEmpty());
        assertTrue(ShotStamp.decode("").isEmpty());
        assertTrue(ShotStamp.decode("v0|bow|1.0|14.0|0.0|0.0|1.0|0.0|100").isEmpty(), "another version");
        assertTrue(ShotStamp.decode("v1|bow|1.0|14.0").isEmpty(), "too few fields");
        assertTrue(ShotStamp.decode("v1||1.0|14.0|0.0|0.0|1.0|0.0|100").isEmpty(), "no weapon id");
        assertTrue(ShotStamp.decode("v1|bow|x|14.0|0.0|0.0|1.0|0.0|100").isEmpty(), "not a number");
        assertTrue(ShotStamp.decode("v1|bow|NaN|14.0|0.0|0.0|1.0|0.0|100").isEmpty(), "not finite");
    }

    /**
     * B2 (2026-11-23): the bow's behavioural enchants ride the stamp as v2's tenth field, and a v1 arrow (stamped before
     * this, still in flight or in a saved chunk) still decodes, carrying none.
     */
    @Test
    void theBehaviourEnchantsRideAsVersionTwoAndAVersionOneArrowStillDecodes() {
        assertTrue(stamp().encode().startsWith("v2|bow|"), stamp().encode());
        assertTrue(stamp().encode().endsWith("|250|tremors:2;other:1"), stamp().encode());
        ShotStamp old = ShotStamp.decode("v1|bow|1.0|14.0|0.0|0.0|1.0|0.0|100").orElseThrow();
        assertEquals(java.util.List.of(), old.behaviourEnchants());
        assertEquals(100, old.triggerScore(), "the control: the old fields are read");
        assertTrue(ShotStamp.decode("v2|bow|1.0|14.0|0.0|0.0|1.0|0.0|100").isEmpty(), "v2 needs its tenth field");
        assertTrue(ShotStamp.decode("v2|bow|1.0|14.0|0.0|0.0|1.0|0.0|100|tremors").isEmpty(), "an entry with no level");
        assertTrue(ShotStamp.decode("v2|bow|1.0|14.0|0.0|0.0|1.0|0.0|100|tremors:0").isEmpty(), "a level below 1");
        assertEquals(java.util.List.of(), ShotStamp.decode("v2|bow|1.0|14.0|0.0|0.0|1.0|0.0|100|").orElseThrow()
                .behaviourEnchants(), "an empty tenth field is none");
    }

    /** The hit rebuilds the caster from the LIVE shooter (where he is, who he is) with the STAMPED damage terms. */
    @Test
    void itAppliesItsDamageTermsToTheLiveShooter() {
        UUID id = UUID.randomUUID();
        CombatantSnapshot live = new CombatantSnapshot(id, new Vec3(1, 2, 3), 1.62, true, io.github.butterflysmp.rpg.core.mob.MobHostility.Side.PLAYER,
                1.6, 99.0, 0.5, 0.25, 1.5, 7.0);   // what he holds AT THE HIT: a sword, say
        CombatantSnapshot cast = stamp().applyTo(live);
        assertEquals(id, cast.id());
        assertEquals(new Vec3(1, 2, 3), cast.position());
        assertEquals(1.62, cast.eyeHeight(), 0.0);
        assertEquals(14.0, cast.attackDamage(), 0.0);
        assertEquals(3.0, cast.classDamageBonus(), 0.0);
        assertEquals(12.5, cast.enchantDamagePercent(), 0.0);
        assertEquals(2.0, cast.critMultiplier(), 0.0);
        assertEquals(49.0, cast.levelDamageBonus(), 0.0);
    }

    @Test
    void itTakesItsTermsFromASnapshotAtTheShot() {
        CombatantSnapshot atShot = new CombatantSnapshot(UUID.randomUUID(), Vec3.ZERO, 1.62, true, io.github.butterflysmp.rpg.core.mob.MobHostility.Side.PLAYER,
                0.0, 14.0, 3.0, 12.5, 2.0, 49.0);
        assertEquals(stamp(), ShotStamp.of("bow", 0.75, atShot, 250, stamp().behaviourEnchants()));
        assertEquals(java.util.List.of(), ShotStamp.of("bow", 0.75, atShot, 250).behaviourEnchants(),
                "the four-argument form stamps no behaviour");
    }
}
