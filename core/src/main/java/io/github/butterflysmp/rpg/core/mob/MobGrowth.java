package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A staged mob that GROWS when creatures of an element die near it: the Phage (PLAN-dragon-mobs.md, the plan's step
 * R4). Ben: "If enough Void mobs are killed nearby a small phage, they will grow back into a big Phage"; "Void as in
 * Void element"; "Start with 4, let's try 6 block radius. Any deaths."; G1 (one death, one stage) and G2 (EVERY Phage
 * in range) confirmed. Authored in the mob file's {@code growth:} block.
 *
 * <p><b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code element}: the element whose deaths count, an id in {@code content/elements/}, resolved for the DEAD
 *       creature by {@code MobElementResolver} (our mob's file, else the vanilla table). The Phage's is void.</li>
 *   <li>{@code radius}: BLOCKS, the straight-line distance between the two creatures' FEET positions, INCLUSIVE, in the
 *       same world only. A sphere, never a cube. The Phage's is 6.</li>
 *   <li>{@code stagesPerDeath}: stages gained per counted death, a whole number, capped at the last stage. The
 *       Phage's is 1, so four deaths take a stage 1 to stage 5.</li>
 * </ul>
 * <b>The caller's search may be any shape that contains the sphere</b> (paper uses a cube); {@link #stageAfter}, not
 * the search, decides who is in range.
 */
public record MobGrowth(String element, double radius, int stagesPerDeath) {

    public MobGrowth {
        if (element == null || element.isBlank()) throw new IllegalArgumentException("growth.element required");
        if (!(radius > 0 && Double.isFinite(radius))) {
            throw new IllegalArgumentException("growth.radius must be a positive number of blocks, was " + radius);
        }
        if (stagesPerDeath < 1) {
            throw new IllegalArgumentException("growth.stages_per_death must be at least 1, was " + stagesPerDeath);
        }
    }

    /** A creature that died: its id, its resolved element (null for none), its world, and its feet position. */
    public record Death(UUID id, String element, String world, Vec3 at) {}

    /** A living creature that might grow: its id, its definition (null for a vanilla mob), stage, liveness, place. */
    public record Candidate(UUID id, MobDefinition def, int stage, boolean alive, String world, Vec3 at) {}

    /**
     * One stage change, DECIDED AT THE DEATH: who, from which stage and to which then, and how far it stood from the
     * death (feet to feet) AT THE DEATH. The later tick on the Phage's own thread re-reads only the stage
     * ({@link #restage}); this distance is the one the GROWTH line prints.
     */
    public record Grow(UUID id, int from, int to, double distance) {}

    /**
     * THE RULE for one candidate: its stage after this death. Unchanged unless the candidate is a living staged mob
     * with a growth block, below its last stage, in the same world, within the radius (inclusive), and the death's
     * element is the growth element.
     */
    public static int stageAfter(Candidate c, Death death) {
        MobDefinition def = c.def();
        if (def == null || def.growth() == null || def.stages() == null) return c.stage();
        if (!c.alive() || c.id().equals(death.id())) return c.stage();
        if (death.element() == null || !death.element().equals(def.growth().element())) return c.stage();
        if (!c.world().equals(death.world())) return c.stage();
        double r = def.growth().radius();
        if (c.at().distanceSquared(death.at()) > r * r) return c.stage();
        // THE CAP IS THE ONE RULE FOR THE LAST STAGE: a stage-5 candidate gets min(5, 5 + k) = 5, i.e. no change. A
        // separate "if (stage >= last) return stage" guard was here and was REMOVED: its mutation (>= to >) survived
        // the whole suite because the cap already gives the same answer, so it was a line no test could see.
        return Math.min(def.stages().count(), c.stage() + def.growth().stagesPerDeath());
    }

    /**
     * ONE DEATH, EVERY CANDIDATE: the stage changes this death causes, in candidate order. {@code alreadyCounted} is the
     * once-only guard: the death event can be delivered twice for one creature, and a death already counted grows
     * nothing. The dying creature is never its own candidate's beneficiary ({@link #stageAfter} skips its id).
     */
    public static List<Grow> resolve(Death death, boolean alreadyCounted, List<Candidate> candidates) {
        List<Grow> out = new ArrayList<>();
        if (alreadyCounted) return out;
        for (Candidate c : candidates) {
            int to = stageAfter(c, death);
            if (to != c.stage()) out.add(new Grow(c.id(), c.stage(), to, Math.sqrt(c.at().distanceSquared(death.at()))));
        }
        return out;
    }

    /**
     * THE LATER TICK (the follow-up of 2026-11-17; PLAN-dragon-mobs.md section 8 F5): the stage a Phage that was
     * decided at the death goes to NOW, from the stage it has NOW. It takes NO position, so it cannot re-decide who is
     * in range: Ben's rule is a death "within" the radius WHEN IT HAPPENS, and that was settled by {@link #resolve}.
     * It re-reads the stage so two deaths in one tick add up (the second reads what the first wrote), and refuses a
     * Phage that died or was removed in between, or a mob with no growth.
     */
    public static int restage(MobDefinition def, int stageNow, boolean aliveNow) {
        if (def == null || def.growth() == null || def.stages() == null || !aliveNow) return stageNow;
        return Math.min(def.stages().count(), stageNow + def.growth().stagesPerDeath());
    }
}
