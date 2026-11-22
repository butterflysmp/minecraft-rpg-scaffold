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
        return new ShotStamp("bow", 0.75, 14.0, 3.0, 12.5, 2.0, 49.0, 250);
    }

    @Test
    void itRoundTripsThroughItsString() {
        ShotStamp stamp = stamp();
        assertEquals(stamp, ShotStamp.decode(stamp.encode()).orElseThrow());
    }

    @Test
    void anAwkwardDoubleRoundTripsExactly() {
        ShotStamp stamp = new ShotStamp("crossbow", 0.1 + 0.2, 37.0, 0.0, 0.0, 1.0, 0.0, 100);
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

    /** The hit rebuilds the caster from the LIVE shooter (where he is, who he is) with the STAMPED damage terms. */
    @Test
    void itAppliesItsDamageTermsToTheLiveShooter() {
        UUID id = UUID.randomUUID();
        CombatantSnapshot live = new CombatantSnapshot(id, new Vec3(1, 2, 3), 1.62, true, true,
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
        CombatantSnapshot atShot = new CombatantSnapshot(UUID.randomUUID(), Vec3.ZERO, 1.62, true, true,
                0.0, 14.0, 3.0, 12.5, 2.0, 49.0);
        assertEquals(stamp(), ShotStamp.of("bow", 0.75, atShot, 250));
    }
}
