package io.github.butterflysmp.rpg.core.mob;

import java.util.random.RandomGenerator;

/**
 * ONE KNELL'S BEAM CLOCK (PLAN-dragon-mobs.md step R5; the plan's MB3): wait, attempt, telegraph, fire, wait again.
 * Pure: the caller ticks it once per server tick on the Knell's own thread, and does what each {@link Event} says.
 * The randomness is INJECTED ({@link Draw}), so a test and the selftest can fix it.
 *
 * <p>The record's order (cfde822, the plan's section 3.1 and 3.R.1.a): a random wait; then an attempt; an attempt that
 * finds no target waits again (I5); one that locks a line telegraphs for {@code telegraphTicks}, rooted, then fires;
 * a fresh wait starts at the fire (I2).
 *
 * <p><b>THE TICKS, COUNTED:</b> a wait of {@code d} drawn at the start gives the ATTEMPT on the {@code d}-th call to
 * {@link #tick}. {@link #begin} makes that tick the telegraph's tick 0; the next {@code telegraphTicks - 1} calls
 * return ROOT (ticks 1 to 11 of 12) and the {@code telegraphTicks}-th returns FIRE, so the fire is exactly
 * {@code telegraphTicks} ticks after the lock.
 *
 * <p>(A {@code @FunctionalInterface} is an interface with one method, so a lambda {@code (min, max) -> 150} is one.)
 */
public final class BeamCycle {

    /** Where the wait comes from: a whole number of ticks from {@code min} to {@code max}, both included. */
    @FunctionalInterface
    public interface Draw {
        int ticks(int min, int max);
    }

    /** The production draw: uniform over {@code min..max} inclusive, from {@code random}. */
    public static Draw uniform(RandomGenerator random) {
        // nextInt(origin, bound) EXCLUDES the bound, so max + 1 is what makes max reachable (I1: both ends included).
        return (min, max) -> random.nextInt(min, max + 1);
    }

    /** What the caller does this tick. */
    public enum Event {
        /** Nothing: the wait runs on. */
        WAIT,
        /** The wait is over: look for a target, then call {@link #begin} (a line locked) or {@link #noTarget}. */
        ATTEMPT,
        /** A telegraph tick after the first: keep the Knell rooted; draw the warning if {@link #flashing}. */
        ROOT,
        /** The telegraph is over: fire along the locked line. A fresh wait has already been drawn. */
        FIRE
    }

    private final MobBeam beam;
    private final Draw draw;
    private boolean telegraph;
    private int waitLeft;
    private int elapsed;

    public BeamCycle(MobBeam beam, Draw draw) {
        this.beam = beam;
        this.draw = draw;
        this.waitLeft = drawWait();
    }

    public Event tick() {
        if (telegraph) {
            elapsed++;
            if (elapsed >= beam.telegraphTicks()) {
                telegraph = false;
                waitLeft = drawWait();   // I2: the next wait starts AT the fire
                return Event.FIRE;
            }
            return Event.ROOT;
        }
        waitLeft--;
        if (waitLeft > 0) return Event.WAIT;
        waitLeft = 0;                    // an unresolved attempt is asked again next tick, never lost
        return Event.ATTEMPT;
    }

    /** An ATTEMPT locked a line: the telegraph's tick 0 is now. */
    public void begin() {
        if (telegraph || waitLeft > 0) {
            throw new IllegalStateException("begin() outside an attempt (telegraph=" + telegraph + ", waitLeft="
                    + waitLeft + ")");
        }
        telegraph = true;
        elapsed = 0;
    }

    /** An ATTEMPT found no one: wait again (I5). */
    public void noTarget() {
        if (telegraph) throw new IllegalStateException("noTarget() during a telegraph");
        waitLeft = drawWait();
    }

    /**
     * Make the next attempt THIS tick: the instrument's forced beam (the brief's ruling 8). True if the clock was
     * waiting and is now due; false during a telegraph, which a force never interrupts.
     */
    public boolean forceDue() {
        if (telegraph) return false;
        waitLeft = 0;
        return true;
    }

    public boolean inTelegraph() {
        return telegraph;
    }

    /** The telegraph's tick, 0 at the lock. Meaningful only {@link #inTelegraph}. */
    public int elapsed() {
        return elapsed;
    }

    /** Is the warning line drawn this telegraph tick? */
    public boolean flashing() {
        return telegraph && beam.flashes(elapsed);
    }

    /** Ticks left in the current wait. Meaningful only when not {@link #inTelegraph}. */
    public int waitLeft() {
        return waitLeft;
    }

    private int drawWait() {
        int d = draw.ticks(beam.cycleMinTicks(), beam.cycleMaxTicks());
        if (d < beam.cycleMinTicks() || d > beam.cycleMaxTicks()) {
            throw new IllegalStateException("the beam's draw returned " + d + ", outside "
                    + beam.cycleMinTicks() + ".." + beam.cycleMaxTicks());
        }
        return d;
    }
}
