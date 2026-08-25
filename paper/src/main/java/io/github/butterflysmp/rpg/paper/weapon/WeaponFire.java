package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
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
 * DURABILITY is the one exception, and it is not that kind of race: a broken weapon stays broken
 * for the whole tick and spends nothing, so there is no check-then-fire window to double-spend
 * through. It is gated HERE rather than in each caller precisely so the packet swing and the
 * interact handler cannot drift apart on what "broken" means.
 *
 * DURABILITY WEAR is the other half of the same axis and rides through here too, as the use
 * listener handed to CastExecutor below. It is passed unconditionally: whether a cast charges a
 * use at all (basic attack, not ability) and whether a melee swing connected are both core's to
 * answer, so this stays the dispatch it says it is and gains no second copy of that rule.
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
     *         result (Success already executed, or OnCooldown / InsufficientResource / Broken),
     *         for the caller to react to. Presence means "this weapon binds this input", which is
     *         exactly the per-trigger signal the right-click handler cancels vanilla on -- and it
     *         is why Broken is a present result rather than an empty one: the press must still be
     *         consumed, or a broken weapon would fall through to vanilla behaviour.
     */
    public static Optional<CastResult> attempt(Player player, String input,
                                               WeaponRegistry weapons,
                                               WeaponService weaponService,
                                               AdapterContext adapters,
                                               CooldownTracker cooldowns) {
        Optional<WeaponDefinition> held = WeaponItems.heldWeaponId(player, adapters.keys())
                .flatMap(weapons::find);
        if (held.isEmpty()) return Optional.empty();
        WeaponDefinition weapon = held.get();

        // Only a trigger this weapon actually BINDS may be gated below. Without this an ironblade,
        // which binds no right_click, would return present on right-click once broken and the
        // interact handler would cancel vanilla -- doors and chests would stop working with it in
        // hand. weaponService.fire answers the same question a few lines down; asking it early is
        // what keeps the broken gate from widening the set of inputs this weapon consumes.
        if (weapon.trigger(input).isEmpty()) return Optional.empty();

        // THE BROKEN GATE, for both entry points at once -- the packet swing and the interact
        // handler both arrive here. Before the snapshot and before weaponService.fire, so a broken
        // weapon spends no resource and trips no cooldown; same standing as Locked. The weapon_id
        // is already proven by the lookup above, so this asks the item's durability only.
        if (WeaponDurability.isBroken(player.getInventory().getItemInMainHand())) {
            return Optional.of(new CastResult.Broken());
        }

        Location eye = player.getEyeLocation();
        Aim aim = new Aim(toVec3(eye), toVec3(eye.getDirection()));
        // Snapshot on the player's own thread, before the region hop below. This is also where the
        // caster's attack speed is frozen -- the swing's cadence is decided from it a moment later,
        // and reading the store after the hop would be the cross-thread read this split prevents.
        CombatantSnapshot caster = BukkitCombatant.snapshot(player, adapters.stats());

        Optional<CastResult> result = weaponService.fire(caster, weapon, input, aim);
        result.ifPresent(r -> {
            if (r instanceof CastResult.Success success) {
                // A dash steers by WASD, not by the look-aim built above. Resolve it HERE,
                // still on the player's thread, before the region hop -- getCurrentInput() is
                // player state and illegal past the hop. Every other cast passes through.
                CastResult.Success toRun = DashAim.resolve(player, success);
                // DURABILITY WEAR rides the executor's use listener. Nothing is decided here on
                // purpose: whether this cast charges at all, and whether a melee swing connected,
                // are both CastExecutor's to answer -- see the gate in its execute(). Passing the
                // charge unconditionally is what keeps this wiring from being a second place the
                // basic-attack rule could drift.
                //
                // Safe to touch the player's inventory from inside the hop: the region is the one
                // owning `eye`, which is the player's own, and the listener only ever runs
                // synchronously within execute(). Both halves of that are load-bearing.
                adapters.scheduler().onRegion(eye, () ->
                        new CastExecutor(new PaperCombatWorld(player.getWorld(), adapters),
                                () -> WeaponDurability.applyWearOnUse(player, cooldowns))
                                .execute(toRun));
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
