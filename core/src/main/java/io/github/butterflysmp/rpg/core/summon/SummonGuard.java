package io.github.butterflysmp.rpg.core.summon;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.MobHostility;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * D4, THE GUARD, decided purely (the seat's PR 4, 2026-11-24; Ben: guard within 8, attack a mob that attacks him or
 * targets him within 30, a mob he directs takes priority). Asked every tick the server's goal runs.
 *
 * <p><b>THE TARGET, by priority:</b> the DIRECTED mob; else a mob that DAMAGED him within {@code damagedWindowTicks}; else
 * a mob whose target is him, within {@code triggerRadius} OF HIM. Ties go to the NEAREST TO HIM. A candidate is never taken
 * beyond {@code chaseLimit} FROM HIM, and never unless {@code harms(SUMMON, side)}: never a player, never a summon. "He hit
 * it" is no input at all, so a mob he merely hit is left alone (the seat's default).
 *
 * <p><b>DROPPED</b> (logged {@code dropped=<reason>}): the directed order, or the current target, is dead, gone, or beyond the chase limit from him (F1). A
 * dropped mob may be taken again when it qualifies again; nothing remembers the drop (Ben's A: a fleeing skeleton).
 * <b>TELEPORT:</b> beyond {@code teleportBack} of him. <b>IDLE:</b> beyond {@code guardRadius} of him it walks back.
 */
public final class SummonGuard {

    private SummonGuard() {}

    /** One body near him, as the server reads it on its own thread. */
    public record Candidate(UUID id, MobHostility.Side side, Vec3 position, boolean alive, OptionalLong damagedHimAt,
                            boolean targetsHim) {}

    public enum Why { NONE, DIRECTED, DAMAGED, TARGETING }

    public enum Dropped { DEAD, GONE, LIMIT }

    /**
     * What the guard does this tick. {@code why} is the TIER THAT CHOSE THIS TICK'S TARGET, never the history (F3).
     * {@code dropped} names why a target it held was let go THIS tick: the directed order (then {@code clearDirected})
     * or, failing that, the current target. {@code clearDirected}: the caller forgets the left-click order.
     */
    public record Decision(Optional<UUID> target, Why why, Optional<Dropped> dropped, boolean clearDirected,
                           boolean teleport) {}

    /**
     * F1-F4 (the seat, 2026-11-24). {@code directed} is set ONLY by the left click and kept until it is dead, gone or
     * beyond the chase limit from him; {@code current} is what it attacked last tick, used only to keep a target within
     * its tier and to report its drop. The priority is RE-DECIDED EVERY TICK: directed, then damaged, then targeting; a
     * higher tier pre-empts the current target; within the same tier the current target is KEPT while it still qualifies
     * (no flip-flop between two equal mobs as distances wobble); only then does "nearest to him" choose.
     */
    public static Decision decide(SummonDefinition def, Vec3 him, Vec3 summonAt, Optional<UUID> directed,
                                  Optional<UUID> current, List<Candidate> candidates, long now) {
        boolean teleport = distance(summonAt, him) > def.teleportBack();
        Optional<Dropped> directedDrop = directed.flatMap(id -> dropReason(def, him, id, candidates));
        boolean clearDirected = directedDrop.isPresent();
        if (directed.isPresent() && !clearDirected) {
            Optional<Candidate> d = candidates.stream()
                    .filter(c -> c.id().equals(directed.get()) && eligible(def, him, c)).findFirst();
            if (d.isPresent()) return new Decision(Optional.of(d.get().id()), Why.DIRECTED, Optional.empty(), false, teleport);
        }
        List<Candidate> damaged = candidates.stream().filter(c -> eligible(def, him, c) && c.damagedHimAt().isPresent()
                && now - c.damagedHimAt().getAsLong() <= def.damagedWindowTicks()).toList();
        List<Candidate> targeting = candidates.stream().filter(c -> eligible(def, him, c) && c.targetsHim()
                && distance(c.position(), him) <= def.triggerRadius()).toList();
        Optional<Candidate> chosen = keepOrNearest(him, current, damaged);
        Why why = Why.DAMAGED;
        if (chosen.isEmpty()) {
            chosen = keepOrNearest(him, current, targeting);
            why = Why.TARGETING;
        }
        if (chosen.isEmpty()) why = Why.NONE;
        Optional<UUID> target = chosen.map(Candidate::id);
        // The drop of the CURRENT target, reported by the tick that loses it (the caller then carries the new target as
        // current, so the next tick has nothing to report: "once, not every tick").
        Optional<Dropped> currentDrop = current.filter(id -> !target.equals(Optional.of(id)) && !directed.equals(Optional.of(id)))
                .flatMap(id -> dropReason(def, him, id, candidates));
        return new Decision(target, why, directedDrop.or(() -> currentDrop), clearDirected, teleport);
    }

    /** F2: within one tier, the current target if it still qualifies for THAT tier; else the one nearest to him. */
    private static Optional<Candidate> keepOrNearest(Vec3 him, Optional<UUID> current, List<Candidate> tier) {
        if (current.isPresent()) {
            Optional<Candidate> kept = tier.stream().filter(c -> c.id().equals(current.get())).findFirst();
            if (kept.isPresent()) return kept;
        }
        return nearest(him, tier);
    }

    /** Beyond the guard radius of him, with no target: walk back toward him. */
    public static boolean needsReturn(SummonDefinition def, Vec3 him, Vec3 summonAt) {
        return distance(summonAt, him) > def.guardRadius();
    }

    private static Optional<Dropped> dropReason(SummonDefinition def, Vec3 him, UUID id, List<Candidate> candidates) {
        Optional<Candidate> c = candidates.stream().filter(x -> x.id().equals(id)).findFirst();
        if (c.isEmpty()) return Optional.of(Dropped.GONE);
        if (!c.get().alive()) return Optional.of(Dropped.DEAD);
        if (distance(c.get().position(), him) > def.chaseLimit()) return Optional.of(Dropped.LIMIT);
        return Optional.empty();
    }

    /** Alive, within the chase limit of him, and a side a summon may harm (MOB only). */
    private static boolean eligible(SummonDefinition def, Vec3 him, Candidate c) {
        return c.alive() && distance(c.position(), him) <= def.chaseLimit()
                && MobHostility.harms(MobHostility.Side.SUMMON, c.side(), false, null);
    }

    private static Optional<Candidate> nearest(Vec3 him, List<Candidate> cs) {
        Candidate best = null;
        for (Candidate c : cs) {
            if (best == null || distance(c.position(), him) < distance(best.position(), him)) best = c;
        }
        return Optional.ofNullable(best);
    }

    private static double distance(Vec3 a, Vec3 b) {
        return Math.sqrt(a.distanceSquared(b));
    }
}
