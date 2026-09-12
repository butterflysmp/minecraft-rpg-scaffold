package io.github.butterflysmp.rpg.core.ability;

import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;

import java.util.Set;

/**
 * Casting an ability, end to end, with zero knowledge of Minecraft.
 * Everything here is exercised by unit tests, not by restarting a server.
 *
 * cast() DECIDES; it does not EXECUTE. It resolves the ability, checks and
 * consumes the cooldown and the resource cost, and hands back a description of
 * what should happen. The caller passes the Success to a CastExecutor.
 *
 * This split exists because resolving where a cast lands reads the world:
 * combatantsNear and castRay are backed by World#getNearbyEntities and
 * World#rayTrace, which are only legal on the region thread owning that chunk.
 * Resolving inline would run them on whatever thread called cast().
 *
 * On which thread, exactly? Today: the one owning the caster's AIM ORIGIN -- their
 * eye. See RpgCommand, which wraps the CastExecutor in onRegion(eye, ...).
 *
 * That is correct for Self and for Melee, whose reach is a few blocks. It is NOT
 * correct for Ray, whose 30-block range can cross into another region, nor for any
 * Burst or Area whose origin is that distant impact. An earlier version of this
 * javadoc claimed the caller hops to "the thread that owns the impact point". That
 * is unimplementable: the impact point is what CastExecutor computes. You cannot hop
 * to the region owning a location you have not yet resolved.
 *
 * The consequence is a Folia-only defect -- on Paper every region scheduler runs on
 * the main thread, so it cannot be reproduced here. Fixing it means stepping a ray
 * across regions the way CastExecutor.step already steps a projectile. See NEXT.md,
 * Commit C.
 */
public final class AbilityService {
    private final AbilityRegistry registry;
    private final CooldownTracker cooldowns;
    private final ResourcePool resources;

    public AbilityService(AbilityRegistry registry, CooldownTracker cooldowns, ResourcePool resources) {
        this.registry = registry;
        this.cooldowns = cooldowns;
        this.resources = resources;
    }

    /**
     * A sealed interface lists exactly which types may implement it, so a switch
     * over a CastResult is checked for exhaustiveness by the compiler.
     */
    public sealed interface CastResult {
        /**
         * Carries a SNAPSHOT of the caster, never the caster. RpgCommand closes over a
         * Success in the lambda it hands to onRegion(...), and RegionScheduler.execute
         * promises only to run the task on the owning region -- not to run it inline, this
         * tick. A live handle crossing that boundary would be an entity outliving its tick.
         * An immutable record crossing it is merely a photograph.
         *
         * The snapshot rather than a bare UUID, because a Self cast detonates at the
         * caster's feet, and a UUID cannot say where those are.
         *
         * Note it carries the AIM, not a resolved target: nothing has looked at
         * the world yet, because nothing here is on the right thread to do so.
         */
        record Success(AbilityDefinition ability, CombatantSnapshot caster, Aim aim) implements CastResult {}

        record OnCooldown(long ticksRemaining) implements CastResult {}

        record InsufficientResource(String resourceId, double required, double available)
                implements CastResult {}

        record UnknownAbility(String id) implements CastResult {}

        /**
         * The ability exists, but this caster is not permitted to cast it -- it is not
         * in their granted set. core is handed that set; it never learns what an
         * archetype is. Returned BEFORE the cooldown and mana checks, so a caster
         * who cannot cast never trips a cooldown or spends mana.
         */
        record Locked(String id) implements CastResult {}

        /**
         * The weapon binds this trigger, but the weapon is BROKEN -- worn to its floor and inert
         * until repaired. An RPG weapon is never destroyed, so "broken" is a state a gate reads
         * rather than an item that is gone; see {@code core.weapon.Durability}.
         *
         * The only arm minted OUTSIDE this class. {@code AbilityService} never returns it: deciding
         * it needs the held item's durability, and core cannot read an ItemStack. Paper's
         * {@code WeaponFire.attempt} mints it, after resolving that the weapon binds the input and
         * BEFORE any cooldown or resource is touched -- same standing as {@link Locked}, so a broken
         * weapon spends nothing and trips no cooldown.
         *
         * It lives in this sealed interface anyway, rather than being a paper-local signal, so the
         * two exhaustive switches over CastResult must handle it or fail to compile. A refusal the
         * caller can forget to render is a weapon that silently does nothing.
         */
        record Broken() implements CastResult {}

        /**
         * The weapon binds this trigger and works, but its quiver is EMPTY -- reload it.
         *
         * <p>A separate arm rather than a reuse of {@link Broken}, and the difference is not
         * cosmetic. {@code Broken} means "worn to its floor and inert until repaired", and
         * {@code BrokenNotice} says <i>"Your weapon is broken -- repair it before using it."</i> A
         * full-durability Boltor with an empty magazine is neither broken nor repairable, and
         * shipping that string for this state would be a falsified line shown to a player -- who,
         * unlike a developer reading a stale comment, has no way to check it. The MECHANISM is
         * reused (minted in paper, throttled notice, same sealed type); the message is not.
         *
         * <p>Minted where {@link Broken} is -- {@code WeaponFire.attempt}, after the binding
         * resolves and BEFORE any cooldown or resource is touched -- so an empty weapon spends
         * nothing and trips no cooldown. Same reason, too: deciding it needs the held item's own
         * data, and core cannot read an ItemStack.
         */
        record Empty() implements CastResult {}

        /**
         * The weapon is mid-reload and cannot fire yet. {@code ticksRemaining} is what the notice
         * counts down.
         *
         * <p>Carries its remaining time for {@link OnCooldown}'s reason: a refusal a player can see
         * the end of is a different experience from one that just says no. Read from the item's own
         * stamped deadline rather than from a timer, so it survives a weapon swap, a relog, and a
         * server restart -- see {@code core.weapon.Quiver.reloadComplete}.
         */
        record Reloading(long ticksRemaining) implements CastResult {}
    }

    /**
     * @param castable the ability ids this caster may cast (resolved from their class,
     *                 in paper). An id absent from this set is refused with Locked,
     *                 before any cooldown or resource is touched.
     */
    public CastResult cast(CombatantSnapshot caster, String abilityId, Aim aim, Set<String> castable) {
        AbilityDefinition def = registry.find(abilityId).orElse(null);
        if (def == null) return new CastResult.UnknownAbility(abilityId);

        // Access before cost. A locked cast must leave the cooldown ready and the
        // mana untouched, so this precedes both checks below -- and a reorder that
        // moved it after them is what AbilityServiceTest's order-pinning test guards.
        if (!castable.contains(abilityId)) return new CastResult.Locked(abilityId);

        return resolve(caster, def, aim);
    }

    /**
     * Fire a pre-resolved cast that is NOT a registered, granted ability -- a weapon
     * trigger. Skips the registry lookup and the castable gate cast() applies, because
     * a weapon is obtained and swung, not unlocked by a class. Everything past the gate
     * is identical: it checks and commits the cooldown and mana atomically here, so
     * there is no check-then-commit window a fast second swing could slip through.
     *
     * The trigger's own id (weaponId/input) is what keys its cooldown, so a weapon's
     * triggers cooldown independently and do not share a timer with any ability.
     */
    public CastResult fireTrigger(CombatantSnapshot caster, AbilityDefinition trigger, Aim aim) {
        return resolve(caster, trigger, aim);
    }

    /**
     * The shared tail of cast() and fireTrigger(): cooldown check -> mana spend ->
     * cooldown trigger -> Success, all before returning. One code path, so the
     * order-pinning mutation test guards both callers.
     */
    private CastResult resolve(CombatantSnapshot caster, AbilityDefinition def, Aim aim) {
        String id = def.id();
        if (!cooldowns.isReady(caster.id(), id)) {
            return new CastResult.OnCooldown(cooldowns.ticksRemaining(caster.id(), id));
        }

        // Mana before cooldown, and both before returning Success. tryConsume is
        // all-or-nothing, so a refusal here leaves the cooldown untouched and the
        // player can immediately try again -- rather than eating the cooldown for
        // an ability that never fired.
        ResourceCost cost = def.cost();
        if (!resources.tryConsume(caster.id(), cost.resourceId(), cost.amount())) {
            return new CastResult.InsufficientResource(cost.resourceId(), cost.amount(),
                    resources.current(caster.id(), cost.resourceId()));
        }

        // A BASIC ATTACK's cadence is scaled by the caster's attack-speed stat; an ability's declared
        // cooldown is its balance, not a swing rate, and is left exactly as authored. Same
        // discriminator the tooltip uses (DamagePayload), so a weapon cannot render as one thing and
        // behave as the other.
        //
        // The speed comes off the SNAPSHOT, frozen on the caster's own thread -- never a live store
        // read here, which would be cross-thread for a caster who has since hopped regions.
        //
        // Note this scales only what the cooldown is SET to. The isReady/ticksRemaining checks above
        // read whatever was already stored, so gaining or losing attack speed mid-cooldown does not
        // retroactively lengthen or shorten a timer already running. Deliberate: the swing you have
        // already committed to keeps the cadence it was committed at.
        int cooldownTicks = DamagePayload.isBasicAttack(def.onHit())
                ? AttackSpeed.effectiveCooldownTicks(def.cooldownTicks(), caster.attackSpeed())
                : def.cooldownTicks();

        // AND THE CAST SHAPE MAY RAISE A FLOOR UNDER THAT, WHICH IS THE IN-FLIGHT GUARD. A volley
        // runs for windup + (shots - 1) * interval ticks; letting it be re-pressed before then
        // overlaps two bursts on one caster. The derivation lives on CastSpec.Volley, which owns all
        // three numbers, so it cannot drift from them -- see CastSpec.minimumCooldownTicks.
        //
        // LAST, after the attack-speed scaling, and that ordering is load-bearing rather than
        // stylistic: a volley whose on_hit carried weapon_damage would be scaled by the caster's
        // attack speed, and a fast enough caster would be scaled BELOW the floor. max() applied
        // afterwards is what makes an under-length cooldown unrepresentable rather than unlikely.
        cooldownTicks = Math.max(cooldownTicks, CastSpec.minimumCooldownTicks(def.cast()));

        // Consumed here, at call time -- not when the effects finally run. If it
        // were consumed at execution time, a player could spam-cast during the
        // hop onto the region thread.
        cooldowns.trigger(caster.id(), id, cooldownTicks);
        return new CastResult.Success(def, caster, aim);
    }
}
