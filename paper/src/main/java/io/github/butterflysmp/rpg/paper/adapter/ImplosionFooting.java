package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.shape.Implosion;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * WHAT THE WORLD SAYS ABOUT ONE VICTIM OF A VOID IMPLOSION (2026-11-17): the {@link Implosion.Footing} core's shape
 * needs, read through the API on the victim's OWN thread. The one reader, shared by the Phage's blast
 * ({@code MobBlaster}) and an ability's pull ({@code BukkitCombatant}'s {@code applyPull}), so the two uses cannot read
 * a footing two ways.
 *
 * <ul>
 *   <li><b>feet</b>: {@code getLocation()}, the entity's position.</li>
 *   <li><b>on the ground</b>: {@code isOnGround()}. For a player it is what the client last reported (INFERRED).</li>
 *   <li><b>block friction</b>: the slipperiness of the block {@link #MOVEMENT_BLOCK_BELOW} below the feet, READ:
 *       {@code Entity#getBlockPosBelowThatAffectsMyMovement} is {@code getOnPos(0.500001f)}; through the API,
 *       {@code BlockType#getSlipperiness} ({@code Material#getSlipperiness} delegates to it). Read only on the ground;
 *       in the air the model uses {@code 1.0f} and never asks.</li>
 *   <li><b>resistance</b>: the VALUE of the attribute the use names: {@code EXPLOSION_KNOCKBACK_RESISTANCE} or
 *       {@code KNOCKBACK_RESISTANCE}, 0 when the entity has none or the use names {@code NONE}.</li>
 * </ul>
 */
public final class ImplosionFooting {

    private ImplosionFooting() {}

    /** BLOCKS below the feet of the block whose friction moves a body: READ, {@code ldc_w 0.500001f} in the jar. */
    public static final double MOVEMENT_BLOCK_BELOW = (double) 0.500001f;

    public static Implosion.Footing of(LivingEntity e, Implosion.Resistance resistance) {
        Location l = e.getLocation();
        boolean ground = e.isOnGround();
        float friction = ground
                ? l.clone().subtract(0, MOVEMENT_BLOCK_BELOW, 0).getBlock().getType().getSlipperiness()
                : Implosion.DEFAULT_BLOCK_FRICTION;   // not read in the air: the model uses 1.0f there
        // The eye height: the old push pointed at the victim's EYE (F-B, READ), so its lift came from there.
        return new Implosion.Footing(new Vec3(l.getX(), l.getY(), l.getZ()), e.getEyeHeight(), ground, friction,
                resistance(e, resistance));
    }

    static double resistance(LivingEntity e, Implosion.Resistance resistance) {
        Attribute a = switch (resistance) {
            case EXPLOSION_KNOCKBACK -> Attribute.EXPLOSION_KNOCKBACK_RESISTANCE;
            case KNOCKBACK -> Attribute.KNOCKBACK_RESISTANCE;
            case NONE -> null;
        };
        if (a == null) return 0.0;
        AttributeInstance i = e.getAttribute(a);
        return i == null ? 0.0 : i.getValue();
    }

    /** A player in creative or spectator is never pulled, as our hostility rules exempt them ({@code MobHostility}). */
    public static boolean exempt(Entity e) {
        return e instanceof Player p && (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR);
    }
}
