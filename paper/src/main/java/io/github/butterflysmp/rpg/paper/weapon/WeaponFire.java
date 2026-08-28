package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.BasicMelee;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.OptionalDouble;

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
        Optional<TriggerBinding> binding = weapon.trigger(input);
        if (binding.isEmpty()) return Optional.empty();

        // THE BROKEN GATE, for both entry points at once -- the packet swing and the interact
        // handler both arrive here. Before the snapshot and before weaponService.fire, so a broken
        // weapon spends no resource and trips no cooldown; same standing as Locked. The weapon_id
        // is already proven by the lookup above, so this asks the item's durability only.
        if (WeaponDurability.isBroken(player.getInventory().getItemInMainHand())) {
            return Optional.of(new CastResult.Broken());
        }


        // THE RETIREMENT. Vanilla's own crosshair attack now delivers the basic melee hit -- it
        // picks the victim, and RpgListeners' EntityDamageByEntityEvent rider lands the payload. So
        // this path must not ALSO fire it from the 120-degree cone, or one click is processed twice.
        //
        // Empty, not Broken: empty means "not ours on this input, leave vanilla alone", which is
        // exactly right here -- vanilla is the thing that will fire it. Placed AFTER the broken gate
        // deliberately, so a broken melee weapon still returns Broken and still explains itself on
        // an air swing, where no damage event will ever fire to explain it instead.
        //
        // Read through BasicMelee, the same predicate landVanillaMelee below uses to SELECT the
        // trigger. One predicate, two opposite senses: if they ever disagree, either the cone comes
        // back or the hit lands twice.
        if (BasicMelee.isVanillaDriven(binding.get().ability())) return Optional.empty();

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
                                () -> WeaponDurability.applyWearOnUse(player, adapters.keys(), cooldowns))
                                .execute(toRun));
            }
        });
        return result;
    }

    /**
     * Land the basic melee hit on the victim VANILLA chose, for the EntityDamageByEntityEvent rider.
     *
     * <p>The counterpart to the retirement in {@link #attempt}: that path refuses a vanilla-driven
     * melee trigger, this one delivers it. Both ask {@link BasicMelee} through
     * {@link WeaponDefinition#vanillaMeleeTrigger()}, so a weapon cannot be skipped by one and
     * unresolved by the other -- which would be a weapon that simply never hits anything.
     *
     * <p>Weapon resolution stays in this class rather than moving into the listener, so there is
     * still exactly one place that turns a held item into a fired trigger.
     *
     * <p>DURABILITY rides the executor's use listener, exactly as {@link #attempt} passes it: whether
     * this cast charges a use at all remains CastExecutor's question, not the wiring's. Wear now
     * lands on a CONNECTING vanilla hit, which is strictly more vanilla than the cone was -- vanilla
     * charges a sword when it hits something, never on a swing through air.
     *
     * <p>No broken-weapon gate here: the rider cancels the vanilla event outright for a broken
     * weapon and returns before reaching this, so a broken weapon deals nothing and wears nothing.
     * The gate lives there because cancelling is what also suppresses the flash and the i-frames.
     *
     * <p>THREADING: runs on the thread owning the attacker, which is where the damage event fires.
     * The victim is within vanilla's ~3-block reach, so it is in the same region -- the same standing
     * onMobMeleeAttack already relies on when it snapshots across the pair.
     *
     * <p>RETURNS what it actually DEALT, for the sweep rider to take a fraction of. Observed through
     * {@code EffectApplier}'s damage seam rather than recomputed here, which is the whole reason the
     * seam exists: a second site would have to re-derive the enchant multiplier, the class bonus, the
     * charge and the arm's liveness gate, and would drift the day any of them moved. Empty means
     * NOTHING WAS DEALT -- no weapon, no melee trigger, an unarmed caster, a target already dead --
     * and the sweep rider fails closed on it, so a swing that dealt nothing sweeps nothing.
     *
     * <p>FIRST-WINS on a payload carrying more than one damage effect, the same rule
     * {@code DamagePayload.of} already applies so that two systems cannot disagree about one weapon.
     * No shipped melee payload has more than one; this is the rule written down for the one that does.
     */
    public static OptionalDouble landVanillaMelee(Player attacker, LivingEntity victim, double chargeScale,
                                        WeaponRegistry weapons, AdapterContext adapters,
                                        CooldownTracker cooldowns) {
        Optional<AbilityDefinition> trigger = WeaponItems.heldWeaponId(attacker, adapters.keys())
                .flatMap(weapons::find)
                .flatMap(WeaponDefinition::vanillaMeleeTrigger);
        if (trigger.isEmpty()) return OptionalDouble.empty();

        CombatantSnapshot caster = BukkitCombatant.snapshot(attacker, adapters.stats());
        Combatant target = BukkitCombatant.of(victim, adapters);

        // A one-slot sink rather than a running total: FIRST-WINS, per the javadoc above. The array
        // is the plain Java idiom for writing to a local from a lambda; it is written and read on
        // this one thread, synchronously, within the landBasicMelee call below.
        double[] dealt = {Double.NaN};
        new CastExecutor(new PaperCombatWorld(victim.getWorld(), adapters),
                () -> WeaponDurability.applyWearOnUse(attacker, adapters.keys(), cooldowns),
                amount -> { if (Double.isNaN(dealt[0])) dealt[0] = amount; })
                .landBasicMelee(trigger.get(), caster, target, chargeScale);
        return Double.isNaN(dealt[0]) ? OptionalDouble.empty() : OptionalDouble.of(dealt[0]);
    }

    private static Vec3 toVec3(Location location) {
        return new Vec3(location.getX(), location.getY(), location.getZ());
    }

    private static Vec3 toVec3(org.bukkit.util.Vector vector) {
        return new Vec3(vector.getX(), vector.getY(), vector.getZ());
    }
}
