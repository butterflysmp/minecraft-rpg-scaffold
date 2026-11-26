package io.github.butterflysmp.rpg.core.combat;

/**
 * THE FUSE'S DECISIONS, AS ONE TABLE (the seat's F1, 2026-11-26, the Blast Fungus; Ben: "Keep" the old game's fuse). A
 * FUSED projectile is an ordinary projectile cast that declares {@code fuse_ticks}: it does not end on a block, it comes
 * to REST there; it ends when the fuse runs out, wherever it is; and it ends at once on touching a mob while it flies.
 * Pure: no world, no clock. {@link ProjectileFlight} asks it once a tick, and nothing else decides.
 *
 * <p><b>The clock is the THROW.</b> {@code elapsed} is the flight's own step count: step 0 runs inline on the throw's
 * frame, step {@code n} runs {@code n} ticks later. So a fuse of 40 goes off on the step that runs 40 ticks after the
 * throw. (A press is thrown one tick after it is pressed, READ: {@code WeaponFire} hops the cast through
 * {@code onRegion}, the global scheduler's {@code runDelayed(1)}; so a press at tick T goes off at T + 41.)
 *
 * <p><b>The fuse is asked FIRST.</b> A step whose fuse has run out goes off where the body IS (its position at the start
 * of the step), before any trace: a chunk cannot travel on the tick it goes off. Then a RESTING chunk stays. Then a
 * FLYING chunk's segment is traced, and the first thing on it decides.
 *
 * <p><b>"A mob" is the sides table's</b> ({@code MobHostility.harms} on {@code PvpPath.FUSED_BURST}), asked by the flight
 * before it hands {@link Touch#MOB} here: a body the table spares (another player) is not a touch, and the chunk flies on
 * through it (CC'S CALL, 2026-11-26; the alternative, resting against him, leaves a chunk hanging in the air at his box).
 * A summon is never met at all: the flight's trace passes through one (Q3, {@code CombatWorld.castRay}).
 */
public final class FuseRules {

    private FuseRules() {}

    /** Where the chunk is in its life. */
    public enum Phase { FLYING, RESTING }

    /** What this step's traced segment met first. Only a FLYING chunk's segment is traced. */
    public enum Touch { NOTHING, MOB, BLOCK }

    /** What the step does. */
    public enum Outcome {
        /** Fly on: the next step is scheduled from the segment's end. */
        FLY_ON,
        /** It met a block: it stops at the point it met it and stays there, its fuse still running. */
        REST,
        /** It met a mob in flight: it goes off now, at the point it met it. */
        DETONATE_TOUCH,
        /** Its fuse ran out: it goes off now, where it is (in flight or at rest). */
        DETONATE_FUSE,
        /** At rest with fuse left: nothing happens this tick. */
        STAY
    }

    /** Whether this step traces a segment at all: only a FLYING chunk whose fuse has not run out. */
    public static boolean traces(Phase phase, int elapsed, int fuseTicks) {
        return phase == Phase.FLYING && !fuseOut(elapsed, fuseTicks);
    }

    /** The fuse has run out on the step that runs {@code fuseTicks} after the throw, and on any later one. */
    public static boolean fuseOut(int elapsed, int fuseTicks) {
        return elapsed >= fuseTicks;
    }

    /**
     * THE TABLE. {@code touch} is read only when {@link #traces} is true; pass {@link Touch#NOTHING} otherwise.
     *
     * <pre>
     *   phase    fuse left   touch     outcome
     *   FLYING   yes         NOTHING   FLY_ON
     *   FLYING   yes         MOB       DETONATE_TOUCH
     *   FLYING   yes         BLOCK     REST
     *   RESTING  yes         (none)    STAY
     *   FLYING   no          (none)    DETONATE_FUSE
     *   RESTING  no          (none)    DETONATE_FUSE
     * </pre>
     */
    public static Outcome decide(Phase phase, int elapsed, int fuseTicks, Touch touch) {
        if (fuseTicks < 1) throw new IllegalArgumentException("a fuse is 1 tick or more, was " + fuseTicks);
        if (fuseOut(elapsed, fuseTicks)) return Outcome.DETONATE_FUSE;
        if (phase == Phase.RESTING) return Outcome.STAY;
        return switch (touch) {
            case NOTHING -> Outcome.FLY_ON;
            case MOB -> Outcome.DETONATE_TOUCH;
            case BLOCK -> Outcome.REST;
        };
    }
}
