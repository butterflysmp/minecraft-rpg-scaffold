package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE PHALANX'S SHOT, the pure half (PLAN-dragon-mobs.md step R7a). Every expected figure below that is not an exact
 * small number was EXECUTED in jshell from the expression in {@link MobShot}'s javadoc, never predicted: the blend
 * {@code normalize((1 - h) x flight + h x normalize(aim - position))}, then {@code position + speed x that}.
 */
class MobShotTest {

    private static final MobShot.ShotLook LOOK =
            new MobShot.ShotLook("amethyst_block", 0.3125, 0.8, 0xC88CFF, 0xC88CFF, 1.2, 0xFF69B4, 1.5, 4);

    /** Fixed inputs: phalanx.yml's numbers but the homing, here the old game's 0.1 (phalanx.yml ships 0.0513 a tick). */
    private static MobShot fixture() {
        return new MobShot(120, 30, 35, 0.2, 0.1, 240, 35, 1.2, 40, LOOK, List.of(), List.of());
    }

    private static MobShot withHoming(double h) {
        return new MobShot(120, 30, 35, 0.2, h, 240, 35, 1.2, 40, LOOK, List.of(), List.of());
    }

    private static void close(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x(), actual.x(), 1e-12, "x of " + actual);
        assertEquals(expected.y(), actual.y(), 1e-12, "y of " + actual);
        assertEquals(expected.z(), actual.z(), 1e-12, "z of " + actual);
    }

    // --- the flight ----------------------------------------------------------------------------------------------

    @Test
    void theLaunchIsAtTheOriginAimedAtTheTargetAtTheSpeed() {
        MobShot.State s = fixture().launch(new Vec3(0, 1.5, 0), new Vec3(10, 1.5, 0));
        close(new Vec3(0, 1.5, 0), s.position());
        close(new Vec3(0.2, 0, 0), s.velocity());
        assertEquals(0, s.age());
    }

    /**
     * Launched along +x, the target then 5 off to the side: jshell, 2026-11-17, from the javadoc's expression:
     * t=1 p=0.199796021738384 1.5 0.009030487113947; t=2 p=0.399042597146705 1.5 0.026374136904472, v at t=2
     * 0.199246575408321 0 0.017343649790525.
     */
    @Test
    void twoStepsAtFixedInputsMatchTheFiguresExecutedInJshell() {
        MobShot shot = fixture();
        MobShot.State s = shot.launch(new Vec3(0, 1.5, 0), new Vec3(10, 1.5, 0));
        Optional<Vec3> aim = Optional.of(new Vec3(10, 1.5, 5));
        s = shot.step(s, aim);
        close(new Vec3(0.199796021738384, 1.5, 0.009030487113947), s.position());
        assertEquals(1, s.age());
        s = shot.step(s, aim);
        close(new Vec3(0.399042597146705, 1.5, 0.026374136904472), s.position());
        close(new Vec3(0.199246575408321, 0, 0.017343649790525), s.velocity());
        assertEquals(2, s.age());
        assertEquals(0.2, s.velocity().length(), 1e-12, "the speed never changes");
    }

    /**
     * THE SHIPPED 0.0513 IS THE OLD GAME'S STEERING (the seat's ruling on #201): cfde822 blended 10% once per 2-tick
     * step; two of our ticks at 1 - sqrt(0.9) turn the same as that one step for a target nearly ahead (here 1 degree
     * off, far away so the direction to it does not move): jshell, 2026-11-17, 0.099996 degrees for the old step,
     * 0.099964 for two ticks at 0.0513. At 90 degrees off they part (6.340 against 6.177): renormalising between our
     * two ticks, recorded in phalanx.yml.
     */
    @Test
    void twoTicksAtTheShippedHomingTurnAsTheOldGamesOneStep() {
        MobShot shot = new MobShot(120, 30, 35, 0.2, 0.0513, 240, 35, 1.2, 40, LOOK, List.of(), List.of());
        double off = Math.toRadians(1.0);
        Vec3 aim = new Vec3(1e7 * Math.cos(off), 0, 1e7 * Math.sin(off));
        MobShot.State s = shot.launch(Vec3.ZERO, new Vec3(1, 0, 0));
        s = shot.step(shot.step(s, Optional.of(aim)), Optional.of(aim));
        double ours = Math.toDegrees(Math.atan2(s.velocity().z(), s.velocity().x()));
        Vec3 old = new Vec3(1, 0, 0).scale(0.9).add(new Vec3(Math.cos(off), 0, Math.sin(off)).scale(0.1)).normalize();
        double record = Math.toDegrees(Math.atan2(old.z(), old.x()));
        assertEquals(0.099996, record, 1e-6);
        assertEquals(0.099964, ours, 1e-6);
    }

    @Test
    void homingZeroFliesStraightWhateverTheTarget() {
        MobShot shot = withHoming(0.0);
        MobShot.State s = shot.launch(new Vec3(0, 0, 0), new Vec3(1, 0, 0));
        for (int i = 0; i < 10; i++) s = shot.step(s, Optional.of(new Vec3(0, 0, 50)));
        close(new Vec3(2.0, 0, 0), s.position());
    }

    @Test
    void homingOnePointsAtTheTargetEveryTick() {
        MobShot shot = withHoming(1.0);
        MobShot.State s = shot.launch(new Vec3(0, 0, 0), new Vec3(1, 0, 0));
        s = shot.step(s, Optional.of(new Vec3(0, 0, 50)));
        close(new Vec3(0, 0, 0.2), s.position());
    }

    @Test
    void noTargetFliesStraightOn() {
        MobShot shot = fixture();
        MobShot.State s = shot.launch(new Vec3(0, 0, 0), new Vec3(0, 0, -1));
        for (int i = 0; i < 5; i++) s = shot.step(s, Optional.empty());
        close(new Vec3(0, 0, -1.0), s.position());
    }

    @Test
    void aShotExactlyAtItsAimPointKeepsItsDirection() {
        MobShot shot = withHoming(1.0);
        MobShot.State s = new MobShot.State(Vec3.ZERO, new Vec3(3, 0, 0), new Vec3(0.2, 0, 0), 4);
        s = shot.step(s, Optional.of(new Vec3(3, 0, 0)));
        close(new Vec3(3.2, 0, 0), s.position());
    }

    @Test
    void aShotCannotBeAimedAtItsOwnOrigin() {
        assertThrows(IllegalArgumentException.class, () -> fixture().launch(Vec3.ZERO, Vec3.ZERO));
    }

    // --- the endings, each at its edge ---------------------------------------------------------------------------

    private static MobShot.State at(Vec3 position, int age) {
        return new MobShot.State(Vec3.ZERO, position, new Vec3(0.2, 0, 0), age);
    }

    @Test
    void theLifeEndsItAtTick240AndNotAt239() {
        MobShot shot = fixture();
        assertEquals(Optional.empty(), shot.ending(at(new Vec3(1, 0, 0), 239), false, false));
        assertEquals(Optional.of(MobShot.Ending.EXPIRED_LIFE), shot.ending(at(new Vec3(1, 0, 0), 240), false, false));
    }

    @Test
    void theRangeIsTheStraightLineFromTheOriginAndEndsItPast35() {
        MobShot shot = fixture();
        assertEquals(Optional.empty(), shot.ending(at(new Vec3(35, 0, 0), 10), false, false), "exactly 35 is inside");
        assertEquals(Optional.of(MobShot.Ending.EXPIRED_RANGE),
                shot.ending(at(new Vec3(35.000001, 0, 0), 10), false, false));
        // A path of 200 ticks x 0.2 = 40 blocks flown, still 20 from the origin: NOT expired. Path length is not it.
        assertEquals(Optional.empty(), shot.ending(at(new Vec3(20, 0, 0), 200), false, false));
    }

    @Test
    void aBlockEndsItFirstThenAHitThenTheLifeThenTheRange() {
        MobShot shot = fixture();
        MobShot.State old = at(new Vec3(40, 0, 0), 240);
        assertEquals(Optional.of(MobShot.Ending.BLOCKED), shot.ending(old, true, true));
        assertEquals(Optional.of(MobShot.Ending.HIT), shot.ending(old, false, true));
        assertEquals(Optional.of(MobShot.Ending.EXPIRED_LIFE), shot.ending(old, false, false));
    }

    /** A player's box, 0.6 x 1.8, feet at the origin: grown by 1.2 it runs x from -1.5 to 1.5, y from -1.2 to 3.0. */
    private static final BeamBox PLAYER = BeamBox.standing(Vec3.ZERO, 0.6, 1.8);

    @Test
    void theToleranceIsTheBoxGrownBy1Point2FacesIncluded() {
        MobShot shot = fixture();
        assertTrue(shot.meets(PLAYER, new Vec3(1.5, 1.0, 0)), "on the grown face");
        assertFalse(shot.meets(PLAYER, new Vec3(1.5000001, 1.0, 0)));
        assertTrue(shot.meets(PLAYER, new Vec3(0, 3.0, 0)));
        assertFalse(shot.meets(PLAYER, new Vec3(0, 3.0000001, 0)));
        assertTrue(shot.meets(PLAYER, new Vec3(1.5, 3.0, -1.5)), "a corner: the growth is a BOX's, not a sphere's");
    }

    // --- who it hits: players, never mobs ------------------------------------------------------------------------

    private static MobShot.Candidate body(MobHostility.Side side, boolean exempt, String world, Vec3 feet) {
        return new MobShot.Candidate(UUID.randomUUID(), side, exempt, world, BeamBox.standing(feet, 0.6, 1.8));
    }

    @Test
    void aPlayerInTheBoxIsHitAndAMobInTheSameBoxNever() {
        MobShot shot = fixture();
        MobShot.Candidate mob = body(MobHostility.Side.MOB, false, "w", Vec3.ZERO);
        MobShot.Candidate player = body(MobHostility.Side.PLAYER, false, "w", new Vec3(1, 0, 0));
        Vec3 p = new Vec3(0.2, 1.0, 0);
        assertEquals(Optional.of(player), shot.victim(p, "w", List.of(mob, player)));
        assertEquals(Optional.empty(), shot.victim(p, "w", List.of(mob)), "a mob is passed over");
    }

    @Test
    void anExemptPlayerOrOneInAnotherWorldIsNeverHit() {
        MobShot shot = fixture();
        Vec3 p = new Vec3(0, 1.0, 0);
        assertEquals(Optional.empty(), shot.victim(p, "w", List.of(body(MobHostility.Side.PLAYER, true, "w", Vec3.ZERO))));
        assertEquals(Optional.empty(), shot.victim(p, "w", List.of(body(MobHostility.Side.PLAYER, false, "x", Vec3.ZERO))));
    }

    @Test
    void ofTwoPlayersInTheBoxTheNearerIsHit() {
        MobShot shot = fixture();
        MobShot.Candidate far = body(MobHostility.Side.PLAYER, false, "w", new Vec3(1.2, 0, 0));
        MobShot.Candidate near = body(MobHostility.Side.PLAYER, false, "w", new Vec3(-0.5, 0, 0));
        assertEquals(Optional.of(near), shot.victim(new Vec3(0, 0.9, 0), "w", List.of(far, near)));
    }

    // --- the price -----------------------------------------------------------------------------------------------

    @Test
    void aHitDeals40TimesGearScoreOver100BeforeDefence() {
        MobShot shot = fixture();
        assertEquals(40.0, shot.price(true, 100), 1e-12);
        assertEquals(120.0, shot.price(true, 300), 1e-12);
        assertEquals(40.0, shot.price(false, 300), 1e-12, "a passive Phalanx (none ship) takes no GS factor");
    }

    // --- the clock -----------------------------------------------------------------------------------------------

    @Test
    void theClockAttemptsEvery120TicksTheFirst120AfterItAppears() {
        MobShot.Clock c = new MobShot.Clock(120);
        int attempts = 0;
        int firstAt = -1;
        int secondAt = -1;
        for (int t = 1; t <= 240; t++) {
            if (c.tick() == MobShot.Clock.Step.ATTEMPT) {
                attempts++;
                if (firstAt < 0) firstAt = t; else secondAt = t;
            }
        }
        assertEquals(2, attempts);
        assertEquals(120, firstAt);
        assertEquals(240, secondAt);
    }

    @Test
    void aForcedAttemptIsTheNextTickAndRestartsTheCount() {
        MobShot.Clock c = new MobShot.Clock(120);
        for (int t = 1; t <= 50; t++) c.tick();
        c.force();
        assertEquals(MobShot.Clock.Step.FORCED, c.tick());
        for (int t = 1; t < 120; t++) assertEquals(MobShot.Clock.Step.WAIT, c.tick(), "tick " + t + " after the force");
        assertEquals(MobShot.Clock.Step.ATTEMPT, c.tick(), "120 after the force");
    }

    // --- every number is content: the constructor refuses nonsense by name --------------------------------------

    @Test
    void badNumbersAreRefusedByName() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new MobShot(0, 30, 35, 0.2, 0.1, 240, 35, 1.2, 40, LOOK, List.of(), List.of()))
                .getMessage().contains("period_ticks"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new MobShot(120, 30, 35, 0.2, 1.5, 240, 35, 1.2, 40, LOOK, List.of(), List.of()))
                .getMessage().contains("homing"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new MobShot(120, 30, 35, 0, 0.1, 240, 35, 1.2, 40, LOOK, List.of(), List.of()))
                .getMessage().contains("speed"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new MobShot(120, 30, 35, 0.2, 0.1, 0, 35, 1.2, 40, LOOK, List.of(), List.of()))
                .getMessage().contains("life_ticks"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new MobShot.ShotLook("amethyst_block", 0.3125, 0.8, 0x1000000, 0, 1, 0, 1, 4))
                .getMessage().contains("glow"));
    }

    /**
     * WHAT HE SEES IS WHAT HE CAN HIT (the seat's ruling 2 on #201): the hittable box is content, an edge in BLOCKS,
     * and it must hold the visible shape; a box smaller than the shape is refused by name.
     */
    @Test
    void theHittableBoxMustHoldTheShape() {
        assertEquals(0.8, LOOK.hitBox(), 0.0);
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new MobShot.ShotLook("amethyst_block", 0.4, 0.39, 0, 0, 1, 0, 1, 4))
                .getMessage().contains("shot.hit_box"));
        new MobShot.ShotLook("amethyst_block", 0.4, 0.4, 0, 0, 1, 0, 1, 4);   // equal is allowed
    }

    @Test
    void theFlashFallsEveryFourTicksFromTheFire() {
        assertTrue(LOOK.flashes(0));
        assertFalse(LOOK.flashes(3));
        assertTrue(LOOK.flashes(4));
        assertTrue(LOOK.flashes(8));
    }
}
