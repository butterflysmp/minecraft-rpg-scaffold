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
        // THE STANCE (the dragon fight's step F6): glowing, and stationary. A stationary mob loses every goal, so it never
        // moves, attacks or chooses a target of its own (Paper's MobGoals API, read: removeAllGoals(Mob)); knockback
        // resistance 1 stops vanilla pushes; the byte lets RpgListeners refuse any target set on it. Its AI stays ON, so
        // gravity still acts and it stands on the floor. Pushes by our own setVelocity ignore the attribute: the fight
        // that owns a stationary mob pins it (FightFixtures).
        if (def.stance().glowing()) entity.setGlowing(true);
        if (def.stance().stationary()) {
            entity.getPersistentDataContainer().set(keys.mobStationary, PersistentDataType.BYTE, (byte) 1);
            var kb = entity.getAttribute(org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE);
            if (kb != null) kb.setBaseValue(1.0);
            if (entity instanceof org.bukkit.entity.Mob mob) org.bukkit.Bukkit.getMobGoals().removeAllGoals(mob);
        }
        // THE FUSE, from content (dragon-mobs step R3): vanilla's swell is the Phage's fuse (the seat's ruling 20), and
        // its length is the server's maxSwell, which Creeper#setMaxFuseTicks writes (CraftCreeper, javap build 74:
        // putfield maxSwell) and the creeper saves as "Fuse", so it survives an unload. Growth never changes it.
        if (def.blast() != null && entity instanceof org.bukkit.entity.Creeper creeper) {
            creeper.setMaxFuseTicks(def.blast().fuseTicks());
        }
        // THE MAIN HAND, from content (dragon-mobs step R7a: the Phalanx's bow, X3), NEVER DROPPED. Here, before the add
        // event, for the same reason as the rest. Equipping a skeleton re-chooses its weapon goal (READ:
        // AbstractSkeleton#onEquipItem -> reassessWeaponGoal, a bow -> RangedBowAttackGoal at priority 4). The loader
        // refused an unknown key; a null here could only be a registry the loader never saw.
        if (def.mainHand() != null && entity.getEquipment() != null) {
            org.bukkit.Material item = org.bukkit.Material.matchMaterial(def.mainHand());
            if (item != null) {
                entity.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(item));
                entity.getEquipment().setItemInMainHandDropChance(0f);
            }
        }
        // THE ONE-HIT SHIELD (step R7b): up from birth, its item in the off hand, never dropped. The byte is the state;
        // the item is its look, cleared when it absorbs (cfde822: "off hand cleared; byte removed").
        if (def.shield() != null) {
            entity.getPersistentDataContainer().set(keys.mobShield, PersistentDataType.BYTE, (byte) 1);
            org.bukkit.Material off = org.bukkit.Material.matchMaterial(def.shield().item());
            if (off != null && entity.getEquipment() != null) {
                entity.getEquipment().setItemInOffHand(new org.bukkit.inventory.ItemStack(off));
                entity.getEquipment().setItemInOffHandDropChance(0f);
            }
        }
    }
}
