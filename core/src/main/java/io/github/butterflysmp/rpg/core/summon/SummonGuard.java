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
 * <p><b>DROPPED</b> (logged {@code dropped=<reason>}): the held target is dead, gone, or beyond the chase limit from him. A
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

    /** What the guard does this tick. {@code dropped} names why the HELD target (directed or current) was let go. */
    public record Decision(Optional<UUID> target, Why why, Optional<Dropped> dropped, boolean teleport) {}

    public static Decision decide(SummonDefinition def, Vec3 him, Vec3 summonAt, Optional<UUID> held,
                                  List<Candidate> candidates, long now) {
        boolean teleport = distance(summonAt, him) > def.teleportBack();
        Optional<Dropped> dropped = held.flatMap(id -> dropReason(def, him, id, candidates));
        Optional<Candidate> directed = held.isPresent() && dropped.isEmpty()
                ? candidates.stream().filter(c -> c.id().equals(held.get()) && eligible(def, him, c)).findFirst()
                : Optional.empty();
        if (directed.isPresent()) return new Decision(Optional.of(directed.get().id()), Why.DIRECTED, dropped, teleport);
        Optional<Candidate> damaged = nearest(him, candidates.stream().filter(c -> eligible(def, him, c)
                && c.damagedHimAt().isPresent() && now - c.damagedHimAt().getAsLong() <= def.damagedWindowTicks()).toList());
        if (damaged.isPresent()) return new Decision(Optional.of(damaged.get().id()), Why.DAMAGED, dropped, teleport);
        Optional<Candidate> targeting = nearest(him, candidates.stream().filter(c -> eligible(def, him, c)
                && c.targetsHim() && distance(c.position(), him) <= def.triggerRadius()).toList());
        if (targeting.isPresent()) return new Decision(Optional.of(targeting.get().id()), Why.TARGETING, dropped, teleport);
        return new Decision(Optional.empty(), Why.NONE, dropped, teleport);
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
