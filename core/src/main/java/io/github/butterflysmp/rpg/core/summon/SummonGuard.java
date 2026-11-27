package io.github.butterflysmp.rpg.core.summon;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.mob.MobHostility;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * THE GUARD, decided purely, asked every tick the server's goal runs. D4 (the seat's PR 4, 2026-11-24; Ben: guard within
 * 8, attack a mob that attacks him or targets him within 30, a mob he directs takes priority), and G1 to G6 (the seat,
 * 2026-11-26), from Ben's words: <i>"Summons should keep their target until it is defeated or their target switches"</i>
 * and <i>"A wolf (or any summon) should give up if it's target if it's target is outside of a 72 block range"</i>.
 *
 * <p><b>ACQUIRING</b> (nothing held), by tier: the DIRECTED mob; else a mob that DAMAGED him within
 * {@code damagedWindowTicks}; else a mob whose target is him, within {@code triggerRadius} OF HIM. Ties go to the NEAREST
 * TO HIM. Never beyond {@code giveUpRange} FROM HIM, and never unless {@code harms(SUMMON, side)}: never a player, never a
 * summon. "He hit it" is no input at all.
 *
 * <p><b>HOLDING (G1).</b> A held target is KEPT until the first of: it is dead or gone (the caller reports a body in
 * another world as gone); it is beyond {@code giveUpRange} from him (G3); or a STRICTLY HIGHER tier switches it (G2). The
 * reason it was acquired lapsing is NOT a drop: a mob that stops targeting him (the Blink's forget included) or whose
 * damage window runs out stays held, reported once as {@code kept}. When the owner dies, quits or changes world the summon
 * itself is removed (READ, {@code SummonRemoval}: OWNER_DEATH, QUIT, WORLD_CHANGE), so no tick decides without him.
 *
 * <p><b>SWITCHING (G2) IS UPWARD ONLY:</b> the guard remembers the tier a held target was acquired at; a directed order
 * replaces any other target; a mob that damaged him replaces one held at the targeting tier; an equal or lower tier never
 * switches. CC'S CALL: the HELD mob itself reaching a higher tier is promoted in place (no switch, no line).
 *
 * <p><b>TELEPORT (G4):</b> beyond {@code teleportBack} of him, ONLY with nothing held. <b>IDLE:</b> beyond
 * {@code guardRadius} of him it walks back ({@link #needsReturn}; the caller asks it only with no target).
 */
public final class SummonGuard {

    private SummonGuard() {}

    /** One body near him, as the server reads it on its own thread. */
    public record Candidate(UUID id, MobHostility.Side side, Vec3 position, boolean alive, OptionalLong damagedHimAt,
                            boolean targetsHim) {}

    /** The tiers, lowest first; {@link #rank} orders them. */
    public enum Why {
        NONE, TARGETING, DAMAGED, DIRECTED;

        int rank() { return ordinal(); }

        String token() { return name().toLowerCase(Locale.ROOT); }
    }

    public enum Dropped { DEAD, GONE, RANGE }

    /** What changed this tick, for the trace (G7). */
    public enum Change { NONE, ACQUIRED, SWITCHED, KEPT, DROPPED }

    /**
     * The target a summon holds, the tier it was acquired at (G2), and whether its acquiring reason has lapsed (so
     * {@code kept} is said once per lapse). Carried by the caller from tick to tick.
     */
    public record Held(UUID id, Why tier, boolean lapsed) {}

    /**
     * What the guard does this tick. {@code from} is the tier switched FROM (present only on a switch); {@code dropped} why a
     * held target or the directed order was let go THIS tick; {@code clearDirected}: the caller forgets the left-click order.
     */
    public record Decision(Optional<Held> held, Change change, Optional<Why> from, Optional<Dropped> dropped,
                           boolean clearDirected, boolean teleport) {

        public Optional<UUID> target() { return held.map(Held::id); }

        /** The tier the held target is held at; NONE when nothing is. */
        public Why why() { return held.map(Held::tier).orElse(Why.NONE); }

        /** G7: {@code acquired:<tier>}, {@code switched:<from>-><to>}, {@code kept}, {@code dropped:<reason>}, joined. */
        public String reason() {
            List<String> parts = new ArrayList<>();
            dropped.ifPresent(d -> parts.add("dropped:" + d.name().toLowerCase(Locale.ROOT)));
            switch (change) {
                case ACQUIRED -> parts.add("acquired:" + why().token());
                case SWITCHED -> parts.add("switched:" + from.orElse(Why.NONE).token() + "->" + why().token());
                case KEPT -> parts.add("kept");
                case NONE, DROPPED -> { }
            }
            return parts.isEmpty() ? "none" : String.join(",", parts);
        }
    }

    public static Decision decide(SummonDefinition def, Vec3 him, Vec3 summonAt, Optional<UUID> directed,
                                  Optional<Held> held, List<Candidate> candidates, long now) {
        Optional<Dropped> directedDrop = directed.flatMap(id -> dropReason(def, him, id, candidates));
        boolean clearDirected = directedDrop.isPresent();
        Optional<Dropped> heldDrop = held.flatMap(h -> dropReason(def, him, h.id(), candidates));
        Optional<Held> kept = held.filter(h -> heldDrop.isEmpty());
        Optional<Dropped> dropped = directedDrop.or(() -> heldDrop);

        // 1. A directed order names its own target and replaces any other (G2).
        if (directed.isPresent() && !clearDirected) {
            Optional<Candidate> d = candidates.stream()
                    .filter(c -> c.id().equals(directed.get()) && eligible(def, him, c)).findFirst();
            if (d.isPresent()) {
                UUID id = d.get().id();
                Held next = new Held(id, Why.DIRECTED, false);
                if (kept.isPresent() && kept.get().id().equals(id) && kept.get().tier() == Why.DIRECTED) {
                    return finish(def, him, summonAt, Optional.of(next), Change.NONE, Optional.empty(), dropped, clearDirected);
                }
                if (kept.isPresent()) {
                    return finish(def, him, summonAt, Optional.of(next), Change.SWITCHED, Optional.of(kept.get().tier()),
                            dropped, clearDirected);
                }
                return finish(def, him, summonAt, Optional.of(next), Change.ACQUIRED, Optional.empty(), dropped, clearDirected);
            }
        }
        List<Candidate> damaged = candidates.stream().filter(c -> eligible(def, him, c) && damagedInWindow(def, c, now)).toList();
        // 2. A held target is kept, unless a strictly higher tier arrives (G1, G2).
        if (kept.isPresent()) {
            Held h = kept.get();
            Candidate self = find(h.id(), candidates).orElseThrow();   // present: it was not dropped as GONE
            Why tier = h.tier();
            if (tier.rank() < Why.DAMAGED.rank() && damagedInWindow(def, self, now)) tier = Why.DAMAGED;   // CC'S CALL
            if (tier.rank() < Why.DAMAGED.rank()) {
                Optional<Candidate> higher = nearest(him, damaged.stream().filter(c -> !c.id().equals(h.id())).toList());
                if (higher.isPresent()) {
                    return finish(def, him, summonAt, Optional.of(new Held(higher.get().id(), Why.DAMAGED, false)),
                            Change.SWITCHED, Optional.of(h.tier()), dropped, clearDirected);
                }
            }
            boolean qualifies = qualifies(def, him, self, tier, directed, now);
            Change change = !qualifies && !h.lapsed() ? Change.KEPT : Change.NONE;
            return finish(def, him, summonAt, Optional.of(new Held(h.id(), tier, !qualifies)), change, Optional.empty(),
                    dropped, clearDirected);
        }
        // 3. Nothing held: a fresh acquisition under D4's rules (G6: the only way back for a target given up).
        List<Candidate> targeting = candidates.stream().filter(c -> eligible(def, him, c) && c.targetsHim()
                && distance(c.position(), him) <= def.triggerRadius()).toList();
        Optional<Candidate> chosen = nearest(him, damaged);
        Why why = Why.DAMAGED;
        if (chosen.isEmpty()) {
            chosen = nearest(him, targeting);
            why = Why.TARGETING;
        }
        if (chosen.isPresent()) {
            return finish(def, him, summonAt, Optional.of(new Held(chosen.get().id(), why, false)), Change.ACQUIRED,
                    Optional.empty(), dropped, clearDirected);
        }
        return finish(def, him, summonAt, Optional.empty(), dropped.isPresent() ? Change.DROPPED : Change.NONE,
                Optional.empty(), dropped, clearDirected);
    }

    /** G4: teleported to him only with NOTHING held; with a target it is neither teleported nor walked back. */
    private static Decision finish(SummonDefinition def, Vec3 him, Vec3 summonAt, Optional<Held> held, Change change,
                                   Optional<Why> from, Optional<Dropped> dropped, boolean clearDirected) {
        boolean teleport = held.isEmpty() && distance(summonAt, him) > def.teleportBack();
        return new Decision(held, change, from, dropped, clearDirected, teleport);
    }

    /** Whether the held mob still has the reason its tier names (only for reporting {@code kept}; never a drop). */
    private static boolean qualifies(SummonDefinition def, Vec3 him, Candidate c, Why tier, Optional<UUID> directed,
                                     long now) {
        return switch (tier) {
            case DIRECTED -> directed.equals(Optional.of(c.id()));
            case DAMAGED -> damagedInWindow(def, c, now);
            case TARGETING -> c.targetsHim() && distance(c.position(), him) <= def.triggerRadius();
            case NONE -> false;
        };
    }

    private static boolean damagedInWindow(SummonDefinition def, Candidate c, long now) {
        return c.damagedHimAt().isPresent() && now - c.damagedHimAt().getAsLong() <= def.damagedWindowTicks();
    }

    /** Beyond the guard radius of him, with no target: walk back toward him. */
    public static boolean needsReturn(SummonDefinition def, Vec3 him, Vec3 summonAt) {
        return distance(summonAt, him) > def.guardRadius();
    }

    /** G1/G3: a target, held or directed, is let go when dead, gone, or beyond the give-up range FROM HIM (72.0 keeps). */
    private static Optional<Dropped> dropReason(SummonDefinition def, Vec3 him, UUID id, List<Candidate> candidates) {
        Optional<Candidate> c = find(id, candidates);
        if (c.isEmpty()) return Optional.of(Dropped.GONE);
        if (!c.get().alive()) return Optional.of(Dropped.DEAD);
        if (distance(c.get().position(), him) > def.giveUpRange()) return Optional.of(Dropped.RANGE);
        return Optional.empty();
    }

    private static Optional<Candidate> find(UUID id, List<Candidate> candidates) {
        return candidates.stream().filter(x -> x.id().equals(id)).findFirst();
    }

    /** Alive, within the give-up range of him, and a side a summon may harm (MOB only). */
    private static boolean eligible(SummonDefinition def, Vec3 him, Candidate c) {
        return c.alive() && distance(c.position(), him) <= def.giveUpRange()
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
