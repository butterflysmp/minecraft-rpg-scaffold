package io.github.butterflysmp.rpg.core.fight;

/**
 * F3, ONE MAGE CYCLE as a pure state machine (PLAN-dragon-fight.md section 6, step F3). One instance belongs to one
 * fight. It holds no Bukkit type and schedules nothing: the fight feeds it ticks and events, and reads back the ring,
 * whether the Mage may be hurt, how many crystals are lit and whether the fight has failed.
 *
 * <pre>
 *   IDLE (ring at C, border)
 *     -- mage landed --> FAST_CLOSE (Mage NOT damageable)
 *     -- ring reaches (M, mageRing) --> SLOW_CLOSE (Mage damageable; a crystal lights every interval)
 *     -- mage died --> RETURNING (crystals out) -- ring home --> IDLE, or straight into a held landing's FAST_CLOSE
 *   terminal: CLOSED (the radius reached 0)   POWERED (every crystal lit before he died)
 * </pre>
 *
 * <p><b>Time.</b> Every input carries the fight's tick, and ticks may not run backwards. Before acting on an input
 * the machine first catches up to its tick, placing every transition on the exact tick it happened, however many
 * ticks were skipped. So an event and a transition on the same tick: the transition comes first. That decides two
 * ties, both against the players: a kill on the tick the ring closes is too late, and a Mage death on the tick the
 * last crystal lights is too late (seat choices, recorded in the PLAN).
 *
 * <p><b>The seat's rulings this follows.</b> A landing while RETURNING is held until the ring is home (seat mechanism
 * ruling, 2026-11-18). In CLOSED or POWERED every input is ignored. A Mage death in FAST_CLOSE (only an admin can cause
 * one) returns the ring from where it stands.
 */
public final class MageCycle {

    /** Where the cycle is. CLOSED and POWERED are terminal, and both mean the fight has failed. */
    public enum Phase { IDLE, FAST_CLOSE, SLOW_CLOSE, RETURNING, CLOSED, POWERED }

    /** Whether the fight goes on, and if it failed, how. */
    public enum Outcome { RUNNING, FAILED_CLOSED, FAILED_POWERED }

    /** What became of a landing. Every value but STARTED and HELD means nothing changed. */
    public enum LandingResult {
        /** The fast close began on this tick. */
        STARTED,
        /** The ring is still returning from the last Mage; this one's fast close starts the tick it is home. */
        HELD,
        /** Further from C than the rings allow; see {@link FightSettings#maxLandingDistanceBlocks()}. */
        REFUSED_TOO_FAR,
        /** A Mage is already channelling, or one is already held. One Mage at a time (Ben: "One mage per phase"). */
        REFUSED_BUSY,
        /** The fight has already failed. */
        IGNORED_ENDED
    }

    /**
     * What the fight reads each tick.
     *
     * @param phase          where the cycle is
     * @param ring           the storm ring
     * @param mageDamageable whether a hit on the Mage lands: true exactly in SLOW_CLOSE (Ben, 2026-11-18)
     * @param crystalsLit    how many crystals are lit; 0 outside SLOW_CLOSE and POWERED
     * @param mageAlive      whether a Mage is alive (channelling, or landed and held): the dragon's immunity flag
     * @param outcome        RUNNING, or how the fight failed
     */
    public record Snapshot(Phase phase, Ring ring, boolean mageDamageable, int crystalsLit, boolean mageAlive,
                           Outcome outcome) {}

    private final FightSettings settings;
    private final double centreXBlocks;
    private final double centreZBlocks;

    private Phase phase = Phase.IDLE;
    private long lastTick = Long.MIN_VALUE;

    private FastClosePath path;       // FAST_CLOSE, SLOW_CLOSE, RETURNING: the current Mage's path
    private long fastStartTick;       // FAST_CLOSE
    private SlowClose slow;           // SLOW_CLOSE
    private long slowStartTick;       // SLOW_CLOSE
    private ReturnPath returning;     // RETURNING
    private FastClosePath held;       // RETURNING: a landing waiting for the ring to come home
    private Ring terminalRing;        // CLOSED, POWERED: the ring as it stood when the fight failed

    public MageCycle(FightSettings settings, double centreXBlocks, double centreZBlocks) {
        this.settings = settings;
        this.centreXBlocks = centreXBlocks;
        this.centreZBlocks = centreZBlocks;
    }

    /** Catches up to {@code tick} and reports the state on it. */
    public Snapshot tick(long tick) {
        advance(tick);
        return snapshot(tick);
    }

    /** The Mage landed at M on {@code tick}. */
    public LandingResult mageLanded(long tick, double mageXBlocks, double mageZBlocks) {
        advance(tick);
        if (isTerminal()) return LandingResult.IGNORED_ENDED;
        if (phase == Phase.FAST_CLOSE || phase == Phase.SLOW_CLOSE) return LandingResult.REFUSED_BUSY;
        if (phase == Phase.RETURNING && held != null) return LandingResult.REFUSED_BUSY;
        Landing landing = FastClosePath.plan(settings, centreXBlocks, centreZBlocks, mageXBlocks, mageZBlocks);
        // A switch over a sealed interface: the compiler checks both shapes are handled.
        FastClosePath planned = switch (landing) {
            case Landing.TooFar tooFar -> null;
            case Landing.Accepted accepted -> accepted.path();
        };
        if (planned == null) return LandingResult.REFUSED_TOO_FAR;
        if (phase == Phase.RETURNING) {
            held = planned;
            return LandingResult.HELD;
        }
        startFastClose(planned, tick);
        return LandingResult.STARTED;
    }

    /**
     * A mob died on {@code tick}, killed by a player standing at these feet coordinates.
     *
     * @return whether it counted: only in SLOW_CLOSE, and only when the killer is inside the ring
     */
    public boolean kill(long tick, double killerXBlocks, double killerZBlocks) {
        advance(tick);
        if (phase != Phase.SLOW_CLOSE) return false;
        if (!slow.ringAt(tick).contains(killerXBlocks, killerZBlocks)) return false;
        slow = slow.afterKill(tick);
        return true;
    }

    /**
     * The Mage died on {@code tick}. In SLOW_CLOSE or FAST_CLOSE the crystals go out and the ring returns from where it
     * stands. While RETURNING it is the HELD Mage who died, so the held landing is cancelled. Otherwise ignored.
     */
    public void mageDied(long tick) {
        advance(tick);
        switch (phase) {
            case FAST_CLOSE -> {
                returning = ReturnPath.fromFastClose(path, tick, tick - fastStartTick);
                phase = Phase.RETURNING;
            }
            case SLOW_CLOSE -> {
                returning = ReturnPath.fromSlowClose(path, tick, slow.radiusAt(tick));
                slow = null;
                phase = Phase.RETURNING;
            }
            case RETURNING -> held = null;
            case IDLE, CLOSED, POWERED -> { /* no Mage to die, or the fight is over */ }
        }
        advance(tick); // a return from progress 0 is home on the same tick
    }

    private boolean isTerminal() {
        return phase == Phase.CLOSED || phase == Phase.POWERED;
    }

    private void startFastClose(FastClosePath planned, long tick) {
        path = planned;
        fastStartTick = tick;
        phase = Phase.FAST_CLOSE;
    }

    /** Runs every transition due up to and including {@code tick}, each on the tick it happens. */
    private void advance(long tick) {
        if (tick < lastTick) {
            throw new IllegalArgumentException("ticks may not run backwards: " + tick + " after " + lastTick);
        }
        lastTick = tick;
        boolean moved = true;
        while (moved) {
            moved = false;
            switch (phase) {
                case FAST_CLOSE -> {
                    long slowStart = fastStartTick + path.durationWholeTicks();
                    if (slowStart <= tick) {
                        slowStartTick = slowStart;
                        slow = SlowClose.start(settings, path.mageXBlocks(), path.mageZBlocks(), slowStart);
                        phase = Phase.SLOW_CLOSE;
                        moved = true;
                    }
                }
                case SLOW_CLOSE -> {
                    long closes = slow.closeTick();
                    long powered = slowStartTick + settings.crystalCount() * settings.crystalIntervalTicks();
                    // A tie goes to CLOSED: the ring shutting is the earlier-checked failure.
                    if (closes <= tick && closes <= powered) {
                        terminalRing = slow.ringAt(closes);
                        phase = Phase.CLOSED;
                    } else if (powered <= tick) {
                        terminalRing = slow.ringAt(powered);
                        phase = Phase.POWERED;
                    }
                }
                case RETURNING -> {
                    long home = returning.homeTick();
                    if (home <= tick) {
                        returning = null;
                        path = null;
                        if (held != null) {
                            FastClosePath next = held;
                            held = null;
                            startFastClose(next, home);
                        } else {
                            phase = Phase.IDLE;
                        }
                        moved = true;
                    }
                }
                case IDLE, CLOSED, POWERED -> { /* nothing happens on its own */ }
            }
        }
    }

    private Snapshot snapshot(long tick) {
        return switch (phase) {
            case IDLE -> new Snapshot(phase, borderRing(), false, 0, false, Outcome.RUNNING);
            case FAST_CLOSE -> new Snapshot(phase, path.ringAfter(tick - fastStartTick), false, 0, true,
                    Outcome.RUNNING);
            case SLOW_CLOSE -> new Snapshot(phase, slow.ringAt(tick), true, crystalsLitAt(tick), true,
                    Outcome.RUNNING);
            case RETURNING -> new Snapshot(phase, returning.ringAt(tick), false, 0, held != null, Outcome.RUNNING);
            case CLOSED -> new Snapshot(phase, terminalRing, false, 0, true, Outcome.FAILED_CLOSED);
            case POWERED -> new Snapshot(phase, terminalRing, false, settings.crystalCount(), true,
                    Outcome.FAILED_POWERED);
        };
    }

    /** Crystals lit on {@code tick} of the slow close: one per full interval since it began, at most all of them. */
    private int crystalsLitAt(long tick) {
        long lit = (tick - slowStartTick) / settings.crystalIntervalTicks();
        return (int) Math.min(settings.crystalCount(), lit);
    }

    private Ring borderRing() {
        return new Ring(centreXBlocks, centreZBlocks, settings.borderRadiusBlocks());
    }
}
