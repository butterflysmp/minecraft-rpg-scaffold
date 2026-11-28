package io.github.butterflysmp.rpg.core.combat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * EVERY CASTER'S PULSES STILL TO FIRE (Smolder, the seat's Z5, Z6 and A5, 2026-11-27): one run per caster, on the Lapis
 * orb's pattern ({@link Orbs}). The server keeps ONE ({@code CombatWorld.pulseRuns}); a run is started at the cast, told
 * each pulse as it fires, and holds the handle of the next pulse's pending link ({@code CombatWorld.scheduleOnCancellable},
 * a {@code PendingWork} link). Ending a run (his death, quit, world change, server stop, the selftest's clear) takes it
 * out and hands back the handle and how many pulses were still to fire, so the caller cancels the link and writes the
 * {@code cancelled} line.
 *
 * <p><b>A PULSE ALREADY EXPANDING IS NOT HERE</b> (CC'S CALL, allowed either way by Z5): its ring is point work at its
 * own fixed origin and finishes, as a thrown grenade's blast does when its thrower dies. Only the PENDING pulses end.
 *
 * <p>Thread-safe by {@code synchronized}: a run is started and advanced on the caster's thread and ended from a player
 * event or the selftest; on Paper all of those are the main thread.
 */
public final class PulseRuns {

    /** One run: whose, its trace label, how many pulses in all, and a generation no other run shares. */
    public record Run(UUID caster, String label, int count, long generation) {}

    /** A run that was ended before its last pulse: the run, the pulses never fired, and the pending link's handle (-1 for none). */
    public record Ended(Run run, int remaining, long pendingHandle) {}

    /** A new run, and the run it replaced if the caster had one still pending. */
    public record Started(Run run, Optional<Ended> replaced) {}

    private static final class Live {
        final Run run;
        int fired;
        long pending = -1;

        Live(Run run) {
            this.run = run;
        }

        Ended ended() {
            return new Ended(run, run.count() - fired, pending);
        }
    }

    private final Map<UUID, Live> live = new HashMap<>();
    private long nextGeneration = 1;

    /** Start a run for {@code caster}. One per caster: a run still pending is replaced, and handed back to be cancelled. */
    public synchronized Started start(UUID caster, String label, int count) {
        Run run = new Run(caster, label, count, nextGeneration++);
        Live old = live.put(caster, new Live(run));
        return new Started(run, Optional.ofNullable(old).map(Live::ended));
    }

    /** Whether {@code run} is still its caster's live run: false once it ended, finished or was replaced. */
    public synchronized boolean isCurrent(Run run) {
        Live l = live.get(run.caster());
        return l != null && l.run.generation() == run.generation();
    }

    /** Pulse {@code n} of {@code run} fired. The last one finishes the run. A run no longer current is ignored. */
    public synchronized void fired(Run run, int n) {
        Live l = live.get(run.caster());
        if (l == null || l.run.generation() != run.generation()) return;
        l.fired = n;
        l.pending = -1;
        if (n >= run.count()) live.remove(run.caster());
    }

    /** The handle of {@code run}'s next pulse's pending link. */
    public synchronized void pending(Run run, long handle) {
        Live l = live.get(run.caster());
        if (l != null && l.run.generation() == run.generation()) l.pending = handle;
    }

    /** End {@code caster}'s run, if it has one. */
    public synchronized Optional<Ended> end(UUID caster) {
        return Optional.ofNullable(live.remove(caster)).map(Live::ended);
    }

    /** End every run (server stop). */
    public synchronized List<Ended> endAll() {
        List<Ended> out = new ArrayList<>();
        for (Live l : live.values()) out.add(l.ended());
        live.clear();
        return out;
    }

    /** {@code caster}'s live run, if any. */
    public synchronized Optional<Run> of(UUID caster) {
        return Optional.ofNullable(live.get(caster)).map(l -> l.run);
    }

    /** How many runs are live. */
    public synchronized int size() {
        return live.size();
    }
}
