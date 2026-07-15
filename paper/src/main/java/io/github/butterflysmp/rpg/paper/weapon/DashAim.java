package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import org.bukkit.Input;
import org.bukkit.entity.Player;

/**
 * The one place a dash's direction is decided -- and the only reason getCurrentInput() is
 * read at all. A dash goes the way the player is MOVING (WASD), not the way they are LOOKING,
 * so facing an enemy while pressing strafe-left steps you left, still aimed at them. That is
 * what makes Ember Step an evasive step rather than a charge.
 *
 * It lives paper-side because both inputs are Bukkit: the WASD key states and the facing yaw.
 * Core's Dash arm is handed the resolved direction as the aim and never knows which it was.
 *
 * MUST run on the thread that owns the player -- getCurrentInput() and the location reads
 * below are player state. Both callers invoke it inline, before their region hop, where that
 * holds.
 */
public final class DashAim {

    private DashAim() {}

    /**
     * For a Dash cast, replace the look-aim the caller built with one pointing the way the
     * player moves. Every other cast passes through untouched -- a ray still fires where you
     * look. The origin is carried over unchanged; the Dash arm sweeps from the caster's feet
     * regardless, so only the direction matters here.
     */
    public static CastResult.Success resolve(Player player, CastResult.Success success) {
        if (!(success.ability().cast() instanceof CastSpec.Dash)) return success;
        Aim dashAim = new Aim(success.aim().origin(), movementDirection(player));
        return new CastResult.Success(success.ability(), success.caster(), dashAim);
    }

    /**
     * WASD as a world-space direction. Forward/back run along the player's facing, strafe
     * runs perpendicular -- both flattened to the ground, because movement keys are horizontal:
     * you cannot press W to climb. When no movement key is held there is no movement to follow,
     * so a standing Ember Step falls back to the look direction and lunges where the caster
     * faces. The cast never whiffs; WASD is the enhancement, look is the floor.
     *
     * Aim normalises the returned vector, so a diagonal keypress still dashes the intended
     * distance rather than root-two times it.
     */
    private static Vec3 movementDirection(Player player) {
        Input input = player.getCurrentInput();
        double forward = (input.isForward() ? 1 : 0) - (input.isBackward() ? 1 : 0);
        double strafe = (input.isLeft() ? 1 : 0) - (input.isRight() ? 1 : 0);

        if (forward == 0 && strafe == 0) {
            var look = player.getEyeLocation().getDirection();
            return new Vec3(look.getX(), look.getY(), look.getZ());
        }

        double yaw = Math.toRadians(player.getLocation().getYaw());
        // Bukkit yaw: 0 faces +Z, 90 faces -X. Horizontal facing, then 90 degrees to the left.
        Vec3 forwardHorizontal = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 leftHorizontal = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        return forwardHorizontal.scale(forward).add(leftHorizontal.scale(strafe));
    }
}
