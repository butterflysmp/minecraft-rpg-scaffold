package io.github.butterflysmp.rpg.core.mob;

import java.util.OptionalDouble;

/**
 * THE SKREEL PAIR'S CYCLE, a pure state machine (PLAN-dragon-mobs.md step R8): recover, hold the band, wind up, leap,
 * land. One instance per live pair, held by its loop and stepped once a tick. Paper supplies three facts each tick and
 * does what the returned {@link Step} says; nothing here knows a block, an entity or a clock but its own counts.
 *
 * <h2>THE STAGES</h2>
 * <ul>
 *   <li><b>RECOVER</b>: where it starts and where every leap ends. It holds the band (closes in when outside it, stands
 *       inside it) but may not begin a wind-up. It becomes HOLD the first tick the period has run since the last wind-up
 *       began (or since the loop began).</li>
 *   <li><b>HOLD</b>: as RECOVER, and a wind-up begins the first tick the target is in the band.</li>
 *   <li><b>WINDUP</b>: rooted. Begun on tick 0; ROOT on ticks 1 to {@code windupTicks - 1}; the LAUNCH on tick
 *       {@code windupTicks}. <b>Once begun it always leaps</b>: the target leaving the band, dying or vanishing changes
 *       nothing, because the aim was fixed when it began (the Knell's beam fires along its locked line the same way).</li>
 *   <li><b>LEAP</b>: FLY each tick, until the first tick it is on the ground AFTER having left it (LANDED), or until
 *       {@code timeoutTicks} after the launch (TIMED_OUT), whichever is first. Either ends in RECOVER, and paper tests the
 *       hit where the mount then stands.</li>
 * </ul>
 *
 * <h2>THE INPUTS, EACH TICK</h2>
 * {@code canAct}: the pair may act of its own (the mount has AI; a frozen pair never begins a wind-up, as a frozen Knell
 * never beams, I23). {@code distance}: feet to feet to its target, or empty for none. {@code onGround}: the mount's.
 */
public final class PounceCycle {

    public enum Stage { RECOVER, HOLD, WINDUP, LEAP }

    /** What paper does this tick. */
    public enum Step {
        /** Stop its path: no target, no AI, or in the band. */
        STAND,
        /** Path to the stand-off point ({@link MobPounce#standOff}). */
        CLOSE_IN,
        /** A wind-up's first tick: fix the aim, root, warn. */
        BEGIN,
        /** A wind-up tick after the first: stay rooted. */
        ROOT,
        /** Unroot and set the leap's velocity ({@link MobPounce#leap}). */
        LAUNCH,
        /** Hold the leap's horizontal velocity. */
        FLY,
        /** It came down: test the hit where it stands. */
        LANDED,
        /** The timeout: test the hit where it is, and stop holding the velocity. */
        TIMED_OUT
    }

    private final MobPounce pounce;
    private Stage stage = Stage.RECOVER;
    private int inStage;
    private int sinceWindup;
    private boolean left;
    private boolean forced;

    public PounceCycle(MobPounce pounce) {
        this.pounce = pounce;
    }

    /**
     * THE INSTRUMENT: begin a wind-up on the next tick, whatever the clock, the band, the target or the AI. Refused
     * (false) during a wind-up or a leap.
     */
    public boolean force() {
        if (stage == Stage.WINDUP || stage == Stage.LEAP) return false;
        forced = true;
        return true;
    }

    public Step tick(boolean canAct, OptionalDouble distance, boolean onGround) {
        if (sinceWindup < Integer.MAX_VALUE) sinceWindup++;
        switch (stage) {
            case WINDUP -> {
                inStage++;
                if (inStage >= pounce.windupTicks()) {
                    stage = Stage.LEAP;
                    inStage = 0;
                    left = false;
                    return Step.LAUNCH;
                }
                return Step.ROOT;
            }
            case LEAP -> {
                inStage++;
                if (!onGround) {
                    left = true;
                } else if (left) {
                    stage = Stage.RECOVER;
                    return Step.LANDED;
                }
                if (inStage >= pounce.timeoutTicks()) {
                    stage = Stage.RECOVER;
                    return Step.TIMED_OUT;
                }
                return Step.FLY;
            }
            default -> {
                if (stage == Stage.RECOVER && sinceWindup >= pounce.periodTicks()) stage = Stage.HOLD;
                boolean inBand = distance.isPresent() && pounce.inBand(distance.getAsDouble());
                if (forced || (stage == Stage.HOLD && canAct && inBand)) {
                    forced = false;
                    stage = Stage.WINDUP;
                    inStage = 0;
                    sinceWindup = 0;
                    return Step.BEGIN;
                }
                if (!canAct || distance.isEmpty() || inBand) return Step.STAND;
                return Step.CLOSE_IN;
            }
        }
    }

    public Stage stage() {
        return stage;
    }

    /** Ticks since this stage's first tick (0 on it). */
    public int inStage() {
        return inStage;
    }

    /** Ticks since the last wind-up began, or since the loop began. */
    public int sinceWindup() {
        return sinceWindup;
    }
}
