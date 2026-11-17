package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

/**
 * THE ONE PATH A CUSTOM MOB IS MADE OURS (PLAN-dragon-mobs.md steps R1 and R2). {@code /rpg spawn} and the Phage's
 * split both call {@link #stampCustom} INSIDE {@code World#spawn}'s pre-spawn consumer, with {@code randomizeData}
 * false and {@code SpawnReason.CUSTOM}, so a split child gets exactly what a spawned mob gets: the {@code mob_id} tag
 * (which the creeper guard keys on), the name, the body and the stage tag (which the health seed reads).
 *
 * <p><b>Why inside the consumer, and not after {@code spawn()} returns:</b> the add event seeds the mob's health
 * register-if-absent, so anything written afterwards arrives to find it already seeded from vanilla
 * ({@code RpgCommand.spawnMob}'s javadoc).
 */
public final class MobSpawner {

    private MobSpawner() {}

    /** Make a freshly constructed entity one of ours, at {@code stage} (ignored for an unstaged mob). */
    public static void stampCustom(LivingEntity entity, MobDefinition def, int stage, Keys keys) {
        entity.getPersistentDataContainer().set(keys.mobId, PersistentDataType.STRING, def.id());
        entity.customName(MiniMessage.miniMessage().deserialize(def.displayName()));
        entity.setCustomNameVisible(false);
        // The BODY and the stage, here for the same reason as the tag: before the add event seeds health from the
        // stage (MobBodyApplier's javadoc).
        MobBodyApplier.apply(entity, def, stage, keys);
        // THE FUSE, from content (dragon-mobs step R3): vanilla's swell is the Phage's fuse (the seat's ruling 20), and
        // its length is the server's maxSwell, which Creeper#setMaxFuseTicks writes (CraftCreeper, javap build 74:
        // putfield maxSwell) and the creeper saves as "Fuse", so it survives an unload. Growth never changes it.
        if (def.blast() != null && entity instanceof org.bukkit.entity.Creeper creeper) {
            creeper.setMaxFuseTicks(def.blast().fuseTicks());
        }
    }
}
