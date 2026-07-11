package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponService;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.PaperCombatWorld;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Firing a held weapon's trigger for a player, shared by the left-click swing listener and
 * the right-click interact handler. Both do the identical dance -- held item to weapon,
 * build aim and snapshot, fire, execute a Success on the owning region -- differing only in
 * the input string and what they do with the result. Extracted here so neither duplicates
 * it, and so each caller stays a thin adapter.
 *
 * DISPATCH, not a gate. attempt() does NOT check cost or cooldown -- WeaponService.fire's
 * shared check-spend-commit tail owns that atomically. A caller must not gate either, or a
 * fast input double-spends through the check-then-fire window.
 *
 * MUST be called on the thread that owns the player: BukkitCombatant.snapshot enforces it,
 * and CastExecutor.execute is scheduled onto the aim's owning region from there. The swing
 * listener reaches that thread via its Netty hop; the interact handler is already on it.
 */
public final class WeaponFire {

    private WeaponFire() {}

    /**
     * Fire the held weapon's {@code input} trigger.
     *
     * @return empty if the player holds no weapon of ours, or the weapon has no binding for
     *         {@code input} -- in which case nothing was checked, spent, or cancelled, and a
     *         caller should leave vanilla behaviour untouched. Otherwise the fired trigger's
     *         result (Success already executed, or OnCooldown / InsufficientResource), for
     *         the caller to react to. Presence means "this weapon binds this input", which is
     *         exactly the per-trigger signal the right-click handler cancels vanilla on.
     */
    public static Optional<CastResult> attempt(Player player, String input,
                                               WeaponRegistry weapons,
                                               WeaponService weaponService,
                                               AdapterContext adapters) {
        Optional<WeaponDefinition> held = WeaponItems.heldWeaponId(player, adapters.keys())
                .flatMap(weapons::find);
        if (held.isEmpty()) return Optional.empty();
        WeaponDefinition weapon = held.get();

        Location eye = player.getEyeLocation();
        Aim aim = new Aim(toVec3(eye), toVec3(eye.getDirection()));
        // Snapshot on the player's own thread, before the region hop below.
        CombatantSnapshot caster = BukkitCombatant.snapshot(player, adapters);

        Optional<CastResult> result = weaponService.fire(caster, weapon, input, aim);
        result.ifPresent(r -> {
            if (r instanceof CastResult.Success success) {
                adapters.scheduler().onRegion(eye, () ->
                        new CastExecutor(new PaperCombatWorld(player.getWorld(), adapters))
                                .execute(success));
            }
        });
        return result;
    }

    private static Vec3 toVec3(Location location) {
        return new Vec3(location.getX(), location.getY(), location.getZ());
    }

    private static Vec3 toVec3(org.bukkit.util.Vector vector) {
        return new Vec3(vector.getX(), vector.getY(), vector.getZ());
    }
}
