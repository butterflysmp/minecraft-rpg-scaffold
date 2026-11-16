package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobSplit;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

/**
 * THE PHAGE'S SPLIT ON DEATH (PLAN-dragon-mobs.md step R2). The rule is core's {@link MobSplit#childrenFor}; this is
 * the Bukkit half, called from the one death point ({@code RpgListeners}' split handler on {@code EntityDeathEvent}).
 *
 * <h2>THE ONCE-ONLY GUARD</h2>
 *
 * The death event can fire twice for one mob (it is cancellable, with a revive health: {@code RpgListeners
 * .onEntityDeath}'s javadoc). So the parent is marked {@code mob_split} BEFORE any child is spawned, and a mob that
 * carries the mark never splits again ({@link MobSplit#childrenFor}'s {@code alreadySplit}). The mark is PDC on the
 * parent, so it needs no map with its own lifetime to leak.
 *
 * <h2>THE CHILDREN</h2>
 *
 * Each child is made ours through {@link MobSpawner#stampCustom} inside {@code World#spawn}'s pre-spawn consumer, the
 * same path {@code /rpg spawn} uses, so it gets the {@code mob_id} tag (the creeper guard keys on it), the name, the
 * stage-1 body and the stage tag, and the add event seeds it at FULL health for its stage. Its GEAR SCORE is the
 * PARENT'S (Ben: "gearscore should be the same as the parent Phage"), copied by M21's own mechanism,
 * {@link MobNameplateManager#inheritGearScore}, which slime children already use: it writes the parent's stored,
 * validated score onto the child before the seed, and the seed prefers a stored score to rolling one from the place.
 *
 * <h2>WHERE A CHILD STANDS</h2>
 *
 * At the parent's position offset along x by {@link MobSplit#offsets()} (the {@code spread} in content). A child whose
 * offset spot would put its body inside a block, or inside a hard entity, is placed at the PARENT'S OWN position
 * instead: the parent's body stood there, and a child is smaller than a stage-5 parent, so it fits. Two children on
 * one spot push apart under vanilla's entity collision. Checked with {@code RegionAccessor#hasCollisionsIn}, the
 * collision API PLAN-dragon-mobs.md section 7 F-T names; it does not load chunks.
 */
public final class MobSplitter {

    private MobSplitter() {}

    /**
     * Split {@code parent} if it is a staged mob dying at its last stage and has not split before. Returns how many
     * children were spawned (0 when it does not split).
     *
     * @param adopt called for each child inside its pre-spawn consumer, with the parent; the selftest's
     *              tag inheritance ({@code SelfTestService.adoptSplitChild}), a no-op for a mob it never tagged
     */
    public static int onDeath(LivingEntity parent, MobRegistry mobs, Keys keys, MobNameplateManager nameplates,
                              Logger log, BiConsumer<LivingEntity, LivingEntity> adopt) {
        var pdc = parent.getPersistentDataContainer();
        String mobId = pdc.get(keys.mobId, PersistentDataType.STRING);
        if (mobId == null) return 0;
        MobDefinition def = mobs.find(mobId).orElse(null);
        int stage = MobBodyApplier.stageOf(parent, def, keys, log);
        boolean alreadySplit = pdc.has(keys.mobSplit, PersistentDataType.BYTE);
        MobSplit.Children children = MobSplit.childrenFor(def, stage, alreadySplit);
        if (children.count() == 0) return 0;

        // THE GUARD, BEFORE ANY CHILD: a second delivery of this death finds the mark and splits nothing.
        pdc.set(keys.mobSplit, PersistentDataType.BYTE, (byte) 1);

        World world = parent.getWorld();
        Location origin = parent.getLocation();
        double childHeight = def.stages().at(children.stage()).height();
        double childWidth = MobSplit.childWidth(parent.getWidth(), parent.getHeight(), childHeight);
        Class<? extends LivingEntity> type = parent.getType().getEntityClass().asSubclass(LivingEntity.class);

        List<String> placed = new ArrayList<>();
        for (double offset : def.split().offsets()) {
            Location spot = origin.clone().add(offset, 0, 0);
            BoundingBox body = new BoundingBox(
                    spot.getX() - childWidth / 2, spot.getY(), spot.getZ() - childWidth / 2,
                    spot.getX() + childWidth / 2, spot.getY() + childHeight, spot.getZ() + childWidth / 2);
            boolean blocked = world.hasCollisionsIn(body);
            Location at = blocked ? origin.clone() : spot;
            LivingEntity child = world.spawn(at, type, CreatureSpawnEvent.SpawnReason.CUSTOM, false, e -> {
                MobSpawner.stampCustom(e, def, children.stage(), keys);
                nameplates.inheritGearScore(parent, List.<org.bukkit.entity.Entity>of(e));
                adopt.accept(parent, e);
            });
            placed.add(child.getUniqueId() + (blocked ? " at-parent" : String.format(Locale.ROOT, " dx=%.2f", offset)));
        }
        Integer gs = pdc.get(keys.mobGearScore, PersistentDataType.INTEGER);
        log.info("SPLIT mob=" + mobId + " parent=" + parent.getUniqueId() + " stage=" + stage
                + " gs=" + (gs == null ? "-" : gs) + " children=" + placed.size() + " childStage=" + children.stage()
                + " " + placed);
        return placed.size();
    }
}
