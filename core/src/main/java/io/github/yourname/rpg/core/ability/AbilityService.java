package io.github.yourname.rpg.core.ability;

import io.github.yourname.rpg.core.combat.Aim;
import io.github.yourname.rpg.core.combat.Combatant;
import io.github.yourname.rpg.core.combat.CooldownTracker;
import io.github.yourname.rpg.core.combat.ResourcePool;

/**
 * Casting an ability, end to end, with zero knowledge of Minecraft.
 * Everything here is exercised by unit tests, not by restarting a server.
 *
 * cast() DECIDES; it does not EXECUTE. It resolves the ability, checks and
 * consumes the cooldown and the resource cost, and hands back a description of
 * what should happen. The caller passes the Success to a CastExecutor on the
 * thread that owns the impact point -- on Paper, Scheduler.onRegion(...).
 *
 * This split exists because resolving where a cast lands reads the world:
 * combatantsNear and castRay are backed by World#getNearbyEntities and
 * World#rayTrace, which are only legal on the region thread owning that chunk.
 * Resolving inline would run them on whatever thread called cast().
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
         * Hand this to a CastExecutor immediately, then drop it. It holds a live
         * Combatant reference and must never be stored across ticks -- see
         * EffectApplier.
         *
         * Note it carries the AIM, not a resolved target: nothing has looked at
         * the world yet, because nothing here is on the right thread to do so.
         */
        record Success(AbilityDefinition ability, Combatant caster, Aim aim) implements CastResult {}

        record OnCooldown(long ticksRemaining) implements CastResult {}

        record InsufficientResource(String resourceId, double required, double available)
                implements CastResult {}

        record UnknownAbility(String id) implements CastResult {}
    }

    public CastResult cast(Combatant caster, String abilityId, Aim aim) {
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
