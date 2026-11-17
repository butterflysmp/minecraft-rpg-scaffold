package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobElementResolver;
import io.github.butterflysmp.rpg.core.mob.MobGrowth;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobScaling;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.adapter.LiveTargets;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * THE PHAGE'S GROWTH (PLAN-dragon-mobs.md, the plan's step R4). The rule is core's {@link MobGrowth}; this is the
 * Bukkit half, called from the one death point ({@code RpgListeners.onMobDeath}, BEFORE the split).
 *
 * <h2>WHAT COUNTS, AND THE ONCE-ONLY GUARD</h2>
 *
 * A death that raises {@code EntityDeathEvent} (R2 pinned which: a player's kill, another mob, a fall, fire, the void,
 * {@code /kill}; never a {@code remove()}, a despawn or an unload) of a creature whose element, resolved by
 * {@link MobElementResolver}, is a growth element. The dead creature is marked {@code mob_growth_fed} BEFORE any stage
 * changes, and a marked death is never counted again: the event can be delivered twice.
 *
 * <h2>WHO GROWS</h2>
 *
 * Candidates are found with a CUBE search of the largest growth radius any loaded mob authors
 * ({@code World#getNearbyEntities}); {@link MobGrowth#stageAfter}, not the search, decides who is in the SPHERE. They
 * are collected NOW, on the death's frame, which is BEFORE the split spawns the dying Phage's children, so those
 * children are never candidates (Ben: "No,").
 *
 * <h2>THE STAGE CHANGE</h2>
 *
 * Applied on the GROWING mob's own thread ({@link Scheduler#onEntity}), which re-reads ONLY its stage there
 * ({@link MobGrowth#restage}; who grows was decided at the death), so two deaths in one tick add up instead of both
 * writing the same stage. It writes the stage tag
 * and the body through {@link MobBodyApplier#apply} (R1's rules, written to be re-applied: height from the unscaled
 * height, speed from the type's default), then the health through {@link CombatantStats#rescaleMaxKeepingShare}: the
 * new stage's maximum at the mob's STORED gear score, current at the same share. It touches no tag, no target, and
 * never {@code mob_split}, so a Phage grown to its last stage splits when it dies like any other.
 *
 * <h2>THE SEAM FOR R3</h2>
 *
 * {@link #reportDeath} is "a creature of this element died here", with no death event. The event path calls it;
 * R3's detonation will call it for a Phage's self-destruct (Ben, I). Nothing else calls it yet.
 */
public final class MobGrower {

    private MobGrower() {}

    /**
     * The death-event path: resolve the dead creature's element, mark the death counted, and report it. Returns how
     * many candidates the death will grow (scheduled, not yet applied).
     */
    public static int onDeath(LivingEntity dead, MobRegistry mobs, MobElementResolver elements, Keys keys,
                              CombatantStats stats, Scheduler scheduler, Logger log) {
        var pdc = dead.getPersistentDataContainer();
        String mobId = pdc.get(keys.mobId, PersistentDataType.STRING);
        String element = elements.elementOf(mobId, dead.getType().getKey().getKey()).orElse(null);
        if (element == null) return 0;
        // THE GUARD: a death already counted grows nothing; mark it BEFORE any stage changes.
        boolean alreadyCounted = pdc.has(keys.mobGrowthFed, PersistentDataType.BYTE);
        pdc.set(keys.mobGrowthFed, PersistentDataType.BYTE, (byte) 1);
        String fed = dead.getType().getKey().getKey() + (mobId == null ? "" : "/" + mobId) + " " + dead.getUniqueId();
        return reportDeath(dead.getWorld(), dead.getLocation(), element, dead.getUniqueId(), alreadyCounted, fed,
                mobs, keys, stats, scheduler, log);
    }

    /**
     * THE SEAM: a creature of {@code element} died at {@code at}, with or without a death event. R3's detonation will
     * call this for a Phage's self-destruct. {@code deadId} is excluded from growing; {@code fed} names the death in
     * the log line. Returns how many candidates will grow.
     */
    public static int reportDeath(World world, Location at, String element, UUID deadId, boolean alreadyCounted,
                                  String fed, MobRegistry mobs, Keys keys, CombatantStats stats, Scheduler scheduler,
                                  Logger log) {
        double search = 0;
        for (MobDefinition def : mobs.all()) if (def.growth() != null) search = Math.max(search, def.growth().radius());
        if (search == 0) return 0;

        MobGrowth.Death death = new MobGrowth.Death(deadId, element, world.getName(), vec(at));
        List<MobGrowth.Candidate> candidates = new ArrayList<>();
        List<LivingEntity> bodies = new ArrayList<>();
        for (Entity e : world.getNearbyEntities(at, search, search, search)) {
            if (!(e instanceof LivingEntity mob)) continue;
            String mobId = mob.getPersistentDataContainer().get(keys.mobId, PersistentDataType.STRING);
            if (mobId == null) continue;
            MobDefinition def = mobs.find(mobId).orElse(null);
            if (def == null || def.growth() == null) continue;
            candidates.add(new MobGrowth.Candidate(mob.getUniqueId(), def,
                    MobBodyApplier.stageOf(mob, def, keys, log), LiveTargets.isLive(mob),
                    mob.getWorld().getName(), vec(mob.getLocation())));
            bodies.add(mob);
        }
        List<MobGrowth.Grow> grows = MobGrowth.resolve(death, alreadyCounted, candidates);
        for (MobGrowth.Grow g : grows) {
            LivingEntity mob = bodies.get(indexOf(candidates, g.id()));
            scheduler.onEntity(mob, () -> applyGrowth(mob, death, g, fed, keys, mobs, stats, log));
        }
        return grows.size();
    }

    /**
     * On the growing mob's thread, a tick or more after the death: apply a growth DECIDED AT THE DEATH (the follow-up
     * of 2026-11-17; PLAN-dragon-mobs.md section 8 F5). WHO grows and the distance logged were settled in
     * {@link #reportDeath}, from the positions at the death, and arrive in {@code grow}. Here only the STAGE is re-read
     * ({@link MobGrowth#restage}), so two deaths in one tick add up; a mob that died or was removed meanwhile is
     * refused (the validity guard below, and {@code restage} when it is not alive). Where the Phage stands NOW is never
     * read: until this change it was, and a live Phage that walked past the radius in that tick lost its growth.
     */
    private static void applyGrowth(LivingEntity mob, MobGrowth.Death death, MobGrowth.Grow grow, String fed,
                                    Keys keys, MobRegistry mobs, CombatantStats stats, Logger log) {
        if (!mob.isValid()) return;
        String mobId = mob.getPersistentDataContainer().get(keys.mobId, PersistentDataType.STRING);
        MobDefinition def = mobs.find(mobId).orElse(null);
        if (def == null) return;
        int from = MobBodyApplier.stageOf(mob, def, keys, log);
        int to = MobGrowth.restage(def, from, LiveTargets.isLive(mob));
        if (to == from) return;

        MobBodyApplier.apply(mob, def, to, keys);                 // the stage tag, the height, the speed
        Integer gs = mob.getPersistentDataContainer().get(keys.mobGearScore, PersistentDataType.INTEGER);
        double newMax = MobScaling.maxHealth(def.stages().at(to).maxHealth(), true, MobClassifier.isHostile(mob),
                gs == null ? 0 : gs);
        stats.rescaleMaxKeepingShare(mob.getUniqueId(), newMax);  // the new maximum, the same share
        log.info(growthLine(mobId, mob.getUniqueId(), from, to, fed, death.element(), grow.distance(),
                stats.tracks(mob.getUniqueId()) ? stats.max(mob.getUniqueId()) : newMax));
    }

    /**
     * The GROWTH line. {@code d} is the distance AT THE DEATH, to THREE places, as the BLAST line prints a spared
     * creature's distance, so a Phage grown by a blast reads the same figure in both lines (it printed two places,
     * from the position a tick later, until 2026-11-17).
     */
    static String growthLine(String mobId, UUID id, int from, int to, String fed, String element, double distance,
                             double max) {
        return "GROWTH mob=" + mobId + " id=" + id + " from=" + from + " to=" + to + " fed=" + fed + " element="
                + element + String.format(Locale.ROOT, " d=%.3f max=%.0f", distance, max);
    }

    private static int indexOf(List<MobGrowth.Candidate> candidates, UUID id) {
        for (int i = 0; i < candidates.size(); i++) if (candidates.get(i).id().equals(id)) return i;
        throw new IllegalStateException("a grow names no candidate: " + id);
    }

    private static Vec3 vec(Location l) {
        return new Vec3(l.getX(), l.getY(), l.getZ());
    }
}
