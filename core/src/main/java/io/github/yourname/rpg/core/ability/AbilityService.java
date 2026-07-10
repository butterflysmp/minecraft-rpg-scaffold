package io.github.yourname.rpg.core.ability;

import io.github.yourname.rpg.core.combat.Aim;
import io.github.yourname.rpg.core.combat.CombatantSnapshot;
import io.github.yourname.rpg.core.combat.CooldownTracker;
import io.github.yourname.rpg.core.combat.ResourcePool;

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
    }

    public CastResult cast(CombatantSnapshot caster, String abilityId, Aim aim) {
        AbilityDefinition def = registry.find(abilityId).orElse(null);
        if (def == null) return new CastResult.UnknownAbility(abilityId);

        if (!cooldowns.isReady(caster.id(), abilityId)) {
            return new CastResult.OnCooldown(cooldowns.ticksRemaining(caster.id(), abilityId));
        }

        // Energy before cooldown, and both before returning Success. tryConsume is
        // all-or-nothing, so a refusal here leaves the cooldown untouched and the
        // player can immediately try again -- rather than eating the cooldown for
        // an ability that never fired.
        ResourceCost cost = def.cost();
        if (!resources.tryConsume(caster.id(), cost.resourceId(), cost.amount())) {
            return new CastResult.InsufficientResource(cost.resourceId(), cost.amount(),
                    resources.current(caster.id(), cost.resourceId()));
        }

        // Consumed here, at call time -- not when the effects finally run. If it
        // were consumed at execution time, a player could spam-cast during the
        // hop onto the region thread.
        cooldowns.trigger(caster.id(), abilityId, def.cooldownTicks());
        return new CastResult.Success(def, caster, aim);
    }
}
