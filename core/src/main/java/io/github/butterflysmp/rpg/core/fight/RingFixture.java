package io.github.butterflysmp.rpg.core.fight;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * F4 + F5, THE DEV FIXTURE'S LOGIC (PLAN-dragon-fight.md section 6): one {@link MageCycle} and one {@link StrikeRamp}
 * driven by a fight clock, drawing the ring and striking whoever stands outside it. No Mage mob, no crystals, no arena,
 * no dragon. The paper side owns the loop, the commands and the world; this owns every decision, so each is tested
 * here against a fake {@link Port}.
 *
 * <p><b>The clock.</b> {@link #tick} is called once a game tick. The fight's tick is 0 at the start and the first call
 * makes it 1, so the first strike tick is 40 and the first redraw 4. Commands ({@link #land}, {@link #mageDie},
 * {@link #kill}) act at the fight's current tick.
 *
 * <p><b>What it never does:</b> read the drawing to decide anything. "Inside" is {@link Ring#contains} on the ring
 * {@link MageCycle} reports, and nothing else. It kills no one on a failure: it says which failure, once, and in CLOSED
 * the strikes go on (every point is outside); in POWERED they stop.
 *
 * <p><b>The lines</b>, each one logged through the port (the paper side prefixes {@code [Rpg]}):
 * <ul>
 *   <li>{@code RINGTICK t=<t> phase=<P> centre=<dx> <dz> r=<r> crystals=<n> points=<p> viewers=<v>}, once per
 *       redraw; the centre is the OFFSET from C, so a reading does not depend on where the fixture was started;
 *       {@code points} is how many particles each viewer is sent this redraw.</li>
 *   <li>{@code FIGHTPHASE t=<t> <from>-><to>}, on the exact tick of every phase change, a landing's and a death's
 *       included.</li>
 *   <li>{@code STRIKE t=<t> player=<name> k=<k> damage=<d> inside|outside}, per participant per strike tick.</li>
 *   <li>{@code FIGHTKILL t=<t> killer=<uuid> counted=<b> r_before=<r> r_after=<r>}, per kill fed in.</li>
 *   <li>{@code FIGHT land|die|stop|FAILED_CLOSED|FAILED_POWERED ...}.</li>
 * </ul>
 */
public final class RingFixture {

    /** The world, as the fixture needs it. The paper side implements it; the tests fake it. */
    public interface Port {
        /** Who is in the fight now (the paper side's filter: same world, near C, survival or adventure). */
        List<Participant> participants();

        /** Draw one redraw's points to these players. */
        void draw(List<UUID> viewers, List<double[]> points);

        /** A lightning bolt that is an EFFECT ONLY (no vanilla damage, no fire) at this player's feet. */
        void strikeEffect(UUID player, double xBlocks, double yBlocks, double zBlocks);

        /** Damage through the game's own damage pipeline. */
        void damage(UUID player, double amount);

        void log(String line);

        /** A line in the participants' chat. */
        void tell(String line);

        /** Crystal {@code index} (1-based) lights: it appears at its spot, its beam on the Mage (step F7). */
        void lightCrystal(int index);

        /** Every lit crystal goes out (the Mage died, or the cycle no longer shows any). */
        void clearCrystals();

        /** The cue for a refused hit on the Mage (step F6). */
        void refusedCue();
    }

    /** One participant, read on the tick: who and where his FEET are, blocks. */
    public record Participant(UUID id, String name, double feetXBlocks, double feetYBlocks, double feetZBlocks) {}

    private final FightSettings settings;
    private final RingLook look;
    private final double centreXBlocks;
    private final double groundYBlocks;
    private final double centreZBlocks;
    private final Port port;
    private final MageCycle cycle;
    private final StrikeRamp<UUID> ramp;
    private final Set<UUID> seen = new HashSet<>();

    private long fightTick;
    private boolean stopped;
    private boolean failureReported;
    private MageCycle.Phase lastPhase = MageCycle.Phase.IDLE;
    private int lastCrystals;
    private MageCycle.Snapshot lastSnapshot;
    private long slowStartTick = -1;
    private int strikeLimit;

    /**
     * THE PLAY ROW'S SAFETY (GATE-fight-mage.md, MQ8): when any participant's consecutive strikes reach {@code limit},
     * the fixture stops after that strike, so a player healed between strikes is never handed one larger than the
     * {@code limit}-th. 0 means no limit (every other use).
     */
    public void strikeLimit(int limit) {
        this.strikeLimit = Math.max(0, limit);
    }

    public RingFixture(FightSettings settings, RingLook look, double centreXBlocks, double groundYBlocks,
                       double centreZBlocks, Port port) {
        this.settings = settings;
        this.look = look;
        this.centreXBlocks = centreXBlocks;
        this.groundYBlocks = groundYBlocks;
        this.centreZBlocks = centreZBlocks;
        this.port = port;
        this.cycle = new MageCycle(settings, centreXBlocks, centreZBlocks);
        this.ramp = new StrikeRamp<>(settings);
    }

    public long fightTick() { return fightTick; }

    public boolean stopped() { return stopped; }

    /** One game tick of the fight. */
    public void tick() {
        if (stopped) return;
        fightTick++;
        MageCycle.Snapshot snap = cycle.tick(fightTick);
        notePhase(snap);
        if (snap.outcome() != MageCycle.Outcome.RUNNING && !failureReported) {
            failureReported = true;
            port.log("FIGHT " + snap.outcome() + " t=" + fightTick);
            port.tell("The fight has failed: " + snap.outcome() + " (fixture: nobody is killed).");
        }
        List<Participant> present = port.participants();
        dropAbsent(present);
        if (fightTick % look.redrawPeriodTicks() == 0) {
            List<double[]> points = RingPoints.points(snap.ring(), groundYBlocks, look, fightTick);
            port.draw(present.stream().map(Participant::id).toList(), points);
            port.log(String.format(Locale.ROOT,
                    "RINGTICK t=%d phase=%s centre=%.3f %.3f r=%.3f crystals=%d points=%d viewers=%d",
                    fightTick, snap.phase(), snap.ring().centreXBlocks() - centreXBlocks,
                    snap.ring().centreZBlocks() - centreZBlocks, snap.ring().radiusBlocks(), snap.crystalsLit(),
                    points.size(), present.size()));
        }
        if (ramp.isStrikeTick(fightTick) && snap.outcome() != MageCycle.Outcome.FAILED_POWERED) {
            for (Participant p : present) strike(p, snap.ring());
            if (strikeLimit > 0) {
                for (Participant p : present) {
                    if (ramp.consecutiveStrikes(p.id()) >= strikeLimit) {
                        port.log("FIGHT strike-limit t=" + fightTick + " player=" + p.name() + " k="
                                + ramp.consecutiveStrikes(p.id()));
                        stop();
                        return;
                    }
                }
            }
        }
    }

    /**
     * One FIGHTPHASE line per change, on the tick it happened: from {@link #tick} for a change the clock brings, and
     * from the input itself for one an input causes (a landing, a death), so the line names the input's tick.
     */
    private void notePhase(MageCycle.Snapshot snap) {
        lastSnapshot = snap;
        if (snap.phase() != lastPhase) {
            port.log("FIGHTPHASE t=" + fightTick + " " + lastPhase + "->" + snap.phase());
            if (snap.phase() == MageCycle.Phase.SLOW_CLOSE) slowStartTick = fightTick;
            if (snap.phase() == MageCycle.Phase.FAST_CLOSE) slowStartTick = -1;
            lastPhase = snap.phase();
        }
        syncCrystals(snap);
    }

    /**
     * The crystals follow the cycle's count on the tick it changes (step F7): each newly lit one lights, in order; a
     * fall (the Mage's death, or a closed ring, whose snapshot shows none) puts every lit one out. POWERED keeps all.
     */
    private void syncCrystals(MageCycle.Snapshot snap) {
        int now = snap.crystalsLit();
        if (now > lastCrystals) {
            for (int i = lastCrystals + 1; i <= now; i++) {
                port.log("CRYSTAL t=" + fightTick + " lit index=" + i);
                port.lightCrystal(i);
            }
        } else if (now < lastCrystals) {
            for (int i = 1; i <= lastCrystals; i++) port.log("CRYSTAL t=" + fightTick + " removed index=" + i);
            port.clearCrystals();
        }
        lastCrystals = now;
    }

    /** The cycle's outcome as of the last tick: RUNNING, or how the fight failed. */
    public MageCycle.Outcome outcome() {
        return lastSnapshot == null ? MageCycle.Outcome.RUNNING : lastSnapshot.outcome();
    }

    /** Whether a hit on the Mage lands now: the cycle's own flag, true exactly in the slow close (step F6). */
    public boolean mageDamageable() {
        return !stopped && lastSnapshot != null && lastSnapshot.mageDamageable();
    }

    /** A hit on the Mage was refused by the gate: the MAGEGATE line and the cue. */
    public void refused(double amount, String source) {
        port.log(String.format(Locale.ROOT, "MAGEGATE t=%d phase=%s amount=%.3f source=%s refused",
                fightTick, lastSnapshot == null ? MageCycle.Phase.IDLE : lastSnapshot.phase(), amount, source));
        port.refusedCue();
    }

    private void strike(Participant p, Ring ring) {
        boolean inside = ring.contains(p.feetXBlocks(), p.feetZBlocks());
        double damage = ramp.strike(p.id(), inside);
        port.log(String.format(Locale.ROOT, "STRIKE t=%d player=%s k=%d damage=%.3f %s",
                fightTick, p.name(), ramp.consecutiveStrikes(p.id()), damage, inside ? "inside" : "outside"));
        if (damage > 0.0) {
            port.strikeEffect(p.id(), p.feetXBlocks(), p.feetYBlocks(), p.feetZBlocks());
            port.damage(p.id(), damage);
        }
    }

    /** A player who has left the fight keeps no strike count (StrikeRamp: "A player who leaves is dropped"). */
    private void dropAbsent(List<Participant> present) {
        Set<UUID> now = new HashSet<>();
        for (Participant p : present) now.add(p.id());
        seen.removeIf(id -> {
            if (now.contains(id)) return false;
            ramp.drop(id);
            return true;
        });
        seen.addAll(now);
    }

    /** The Mage "lands" at C + (dx, dz), blocks. */
    public MageCycle.LandingResult land(double dxBlocks, double dzBlocks) {
        if (stopped) return MageCycle.LandingResult.IGNORED_ENDED;
        MageCycle.LandingResult result = cycle.mageLanded(fightTick, centreXBlocks + dxBlocks, centreZBlocks + dzBlocks);
        port.log(String.format(Locale.ROOT, "FIGHT land t=%d dx=%.3f dz=%.3f result=%s",
                fightTick, dxBlocks, dzBlocks, result));
        notePhase(cycle.tick(fightTick));
        return result;
    }

    /** The Mage dies now. */
    public void mageDie() {
        if (stopped) return;
        cycle.mageDied(fightTick);
        MageCycle.Snapshot snap = cycle.tick(fightTick);
        port.log("FIGHT die t=" + fightTick + " phase=" + snap.phase() + " slow_close_started="
                + (slowStartTick < 0 ? "never" : "t=" + slowStartTick)
                + (slowStartTick < 0 ? "" : " ticks_in_slow_close=" + (fightTick - slowStartTick)));
        notePhase(snap);
    }

    /**
     * A mob died to a player standing at these feet coordinates (the seat's placeholder: any mob counts).
     *
     * @return whether the ring counted it
     */
    public boolean kill(UUID killer, double feetXBlocks, double feetZBlocks) {
        if (stopped) return false;
        double before = cycle.tick(fightTick).ring().radiusBlocks();
        boolean counted = cycle.kill(fightTick, feetXBlocks, feetZBlocks);
        double after = cycle.tick(fightTick).ring().radiusBlocks();
        port.log(String.format(Locale.ROOT, "FIGHTKILL t=%d killer=%s counted=%s r_before=%.3f r_after=%.3f",
                fightTick, killer, counted, before, after));
        return counted;
    }

    /** Ends the fixture. Nothing it made outlives it: it draws nothing, strikes nothing and takes no input again. */
    public void stop() {
        if (stopped) return;
        stopped = true;
        port.log("FIGHT stop t=" + fightTick);
    }

    public FightSettings settings() { return settings; }
}
