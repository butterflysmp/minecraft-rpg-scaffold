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
        if (!(success.ability().cast() instanceof CastSpec.Dash dash)) return success;
        Vec3 direction = switch (dash.direction()) {
            case MOVEMENT_ELSE_FORWARD -> movementDirection(player);
            case REVERSE_FACING -> reverseFacing(player.getLocation().getYaw());
        };
        Aim dashAim = new Aim(success.aim().origin(), direction);
        return new CastResult.Success(success.ability(), success.caster(), dashAim);
    }

    /**
     * A straight backpedal: the facing flattened to the ground plane (yaw only, pitch
     * DROPPED), then negated. WASD is ignored entirely -- a retreat that went sideways
     * because you happened to be strafing would defeat the purpose. Reuses the same yaw-only
     * flatten as the stationary fallback, so the "no vertical dash" guarantee holds here too:
     * looking up and casting Rekindle dashes back-and-flat, not down-and-back.
     *
     * Player-free and package-visible on purpose, like {@link #directionFromInput}: the
     * horizontal-and-opposite guarantee is unit-tested, not left to a boot.
     */
    static Vec3 reverseFacing(double yawDegrees) {
        return directionFromInput(yawDegrees, false, false, false, false).negate();
    }

    /** Reads the live player, then hands the pure decision to {@link #directionFromInput}. */
    private static Vec3 movementDirection(Player player) {
        Input input = player.getCurrentInput();
        return directionFromInput(player.getLocation().getYaw(),
                input.isForward(), input.isBackward(), input.isLeft(), input.isRight());
    }

    /**
     * WASD as a horizontal, ground-plane direction from the facing yaw. Forward/back run along
     * facing, strafe runs perpendicular, all with a ZERO vertical component -- a dash is a
     * reposition across the ground, never a launch.
     *
     * When no movement key is held the fallback is a W-equivalent forward dash: facing
     * FLATTENED to the horizontal (yaw only, pitch DROPPED). It deliberately does NOT use the
     * look direction -- looking up and casting must not fling the caster straight up. So there
     * is no way to dash vertically at all, standing or moving; that is why this takes only yaw,
     * never pitch. WASD is the enhancement, W-forward is the floor; the cast never whiffs.
     *
     * Player-free and package-visible on purpose: the horizontal-only guarantee is unit-tested,
     * not left to a boot. Aim normalises the result, so a diagonal keypress still dashes the
     * intended distance rather than root-two times it.
     */
    static Vec3 directionFromInput(double yawDegrees, boolean forwardKey, boolean backKey,
                                   boolean leftKey, boolean rightKey) {
        double forward = (forwardKey ? 1 : 0) - (backKey ? 1 : 0);
        double strafe = (leftKey ? 1 : 0) - (rightKey ? 1 : 0);

        // Stationary -> press W for them: a forward dash along facing, on the ground plane.
        if (forward == 0 && strafe == 0) forward = 1;

        double yaw = Math.toRadians(yawDegrees);
        // Bukkit yaw: 0 faces +Z, 90 faces -X. Horizontal facing, then 90 degrees to the left.
        Vec3 forwardHorizontal = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 leftHorizontal = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        return forwardHorizontal.scale(forward).add(leftHorizontal.scale(strafe));
    }
}
