package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE PHALANX'S BLINK, the pure half (PLAN-dragon-mobs.md step R7b). cfde822 (section 3.4): "a non-creative,
 * non-spectator player in the D4 box while it is not already invisible. Tries 8 random points 6-16 blocks out ... the
 * first passable one wins; none passes, no blink." The landing goes through the Knell's ONE check
 * ({@link BeamTeleport#foot}).
 */
class MobBlinkTest {

    /** The shipped numbers (phalanx.yml): 5, 6, 16, 8, 3. */
    private static MobBlink blink() {
        return new MobBlink(5, 6, 16, 8, 3, List.of(), "squid_ink", 12);
    }

    private static final BeamBox AT_3 = BeamBox.standing(new Vec3(3, 0, 0), 0.6, 1.8);

    // --- the trigger -----------------------------------------------------------------------------------------------

    @Test
    void aPlayerWhoseBoxIsInsideTheFiveBlockBoxTriggersItAndAMobNever() {
        MobBlink b = blink();
        assertTrue(b.triggered(Vec3.ZERO, AT_3, MobHostility.Side.PLAYER, false));
        assertFalse(b.triggered(Vec3.ZERO, AT_3, MobHostility.Side.MOB, false), "a mob never");
        assertFalse(b.triggered(Vec3.ZERO, AT_3, MobHostility.Side.PLAYER, true), "a creative or spectator player never");
    }

    /** The box, as cfde822 queried it: a body whose box meets the cube of half-extent 5 round the Phalanx's feet. */
    @Test
    void theEdgeIsTheBoxNotADistance() {
        MobBlink b = blink();
        // His box's near face (x - 0.3) exactly at 5: inside, faces included.
        assertTrue(b.triggered(Vec3.ZERO, BeamBox.standing(new Vec3(5.3, 0, 0), 0.6, 1.8), MobHostility.Side.PLAYER, false));
        assertFalse(b.triggered(Vec3.ZERO, BeamBox.standing(new Vec3(5.3001, 0, 0), 0.6, 1.8), MobHostility.Side.PLAYER, false));
        // A corner 4.9 off on both axes is 6.93 away and still inside the box: the box, not a distance.
        assertTrue(b.triggered(Vec3.ZERO, BeamBox.standing(new Vec3(5.2, 0, 5.2), 0.6, 1.8), MobHostility.Side.PLAYER, false));
    }

    // --- the landing -----------------------------------------------------------------------------------------------

    /** A world where every point is loaded, free and grounded, except where the test says. */
    private static final class World implements BeamTeleport.Footing {
        final List<Vec3> asked = new ArrayList<>();
        boolean loaded = true;
        double wallAboveX = Double.POSITIVE_INFINITY;   // fits is false for x beyond this
        double voidBeyondZ = Double.POSITIVE_INFINITY;  // grounded is false for z beyond this
        @Override public boolean loaded(Vec3 f) { asked.add(f); return loaded; }
        @Override public boolean fits(Vec3 f) { return f.x() <= wallAboveX; }
        @Override public boolean grounded(Vec3 f) { return f.z() <= voidBeyondZ; }
        @Override public boolean sees(Vec3 f) { throw new AssertionError("the blink never asks for sight"); }
    }

    @Test
    void aDrawLandsAtItsAngleAndDistanceOnTheFeetLevel() {
        World w = new World();
        MobBlink.Decision d = blink().decide(new Vec3(10, 64, 10), List.of(new MobBlink.Draw(90, 8)), w);
        assertEquals(1, d.tried().size());
        Vec3 l = d.landing().orElseThrow();
        assertEquals(10.0, l.x(), 1e-9);
        assertEquals(64.0, l.y(), 1e-9);
        assertEquals(18.0, l.z(), 1e-9, "90 degrees is +z (the angle turns x toward z)");
    }

    @Test
    void attemptsAreTriedInOrderAndTheFirstThatStandsWins() {
        World w = new World();
        w.wallAboveX = 5;   // anything east of x 5 does not fit
        MobBlink.Decision d = blink().decide(Vec3.ZERO,
                List.of(new MobBlink.Draw(0, 10), new MobBlink.Draw(180, 7)), w);
        assertEquals(2, d.tried().size());
        assertEquals(BeamTeleport.Outcome.NO_FOOTING, d.tried().get(0).outcome());
        assertEquals(7, d.tried().get(0).blocked(), "every height of the tolerance's 7 refused by fit");
        assertEquals(BeamTeleport.Outcome.OK, d.tried().get(1).outcome());
        assertEquals(-7.0, d.landing().orElseThrow().x(), 1e-9);
    }

    /**
     * THE FIRST THAT STANDS WINS, AND NOTHING AFTER IT IS ASKED. Added after MUTFIRST (the decision returns only on the
     * LAST draw) left the class green: every other row has at most one standing draw, so first-wins and last-wins were the
     * same picture. Two standing draws, distinct landings.
     */
    @Test
    void ofTwoStandingDrawsTheFirstWinsAndTheSecondIsNeverTried() {
        World w = new World();
        MobBlink.Decision d = blink().decide(Vec3.ZERO,
                List.of(new MobBlink.Draw(0, 7), new MobBlink.Draw(180, 9)), w);
        assertEquals(1, d.tried().size(), "nothing after the first OK is asked");
        assertEquals(7.0, d.landing().orElseThrow().x(), 1e-9);
    }

    @Test
    void noAttemptStandsAndItStays() {
        World w = new World();
        w.voidBeyondZ = -100;   // nothing is grounded
        List<MobBlink.Draw> draws = new ArrayList<>();
        for (int i = 0; i < 8; i++) draws.add(new MobBlink.Draw(45 * i, 6 + i));
        MobBlink.Decision d = blink().decide(Vec3.ZERO, draws, w);
        assertEquals(8, d.tried().size());
        assertEquals(Optional.empty(), d.landing());
        assertTrue(d.tried().stream().allMatch(v -> v.outcome() == BeamTeleport.Outcome.NO_FOOTING && v.ungrounded() == 7));
    }

    @Test
    void anUnloadedColumnIsRefusedAsUnloaded() {
        World w = new World();
        w.loaded = false;
        MobBlink.Decision d = blink().decide(Vec3.ZERO, List.of(new MobBlink.Draw(0, 6)), w);
        assertEquals(BeamTeleport.Outcome.UNLOADED, d.tried().getFirst().outcome());
    }

    @Test
    void theDrawsComeFromTheInjectedSourceAndAreAsManyAsTheAttempts() {
        MobBlink b = blink();
        List<MobBlink.Draw> draws = b.draws(MobBlink.uniform(new Random(7)));
        assertEquals(8, draws.size());
        for (MobBlink.Draw d : draws) {
            assertTrue(d.angleDegrees() >= 0 && d.angleDegrees() < 360, d.toString());
            assertTrue(d.distance() >= 6 && d.distance() <= 16, d.toString());
        }
        assertEquals(draws, b.draws(MobBlink.uniform(new Random(7))), "the same seed, the same draws");
    }

    @Test
    void badNumbersAreRefusedByName() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new MobBlink(5, 16, 6, 8, 3, List.of(), "x", 1))
                .getMessage().contains("blink.far"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new MobBlink(5, 6, 16, 0, 3, List.of(), "x", 1))
                .getMessage().contains("blink.attempts"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new MobBlink(0, 6, 16, 8, 3, List.of(), "x", 1))
                .getMessage().contains("blink.trigger_range"));
    }

    // --- the shield ------------------------------------------------------------------------------------------------

    @Test
    void theShieldIsSpentByTheFirstHitOfAnyPositiveAmountAndNeverByZero() {
        assertTrue(MobShield.spends(0.01));
        assertTrue(MobShield.spends(240));
        assertFalse(MobShield.spends(0.0), "a hit of zero damage does not spend it");
        assertFalse(MobShield.spends(-1));
        assertFalse(MobShield.spends(Double.NaN));
    }
}
