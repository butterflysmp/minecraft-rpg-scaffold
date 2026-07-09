package io.github.yourname.rpg.core.ability;

import io.github.yourname.rpg.core.Vec3;
import io.github.yourname.rpg.core.ability.effect.EffectApplier;
import io.github.yourname.rpg.core.combat.CombatWorld;
import io.github.yourname.rpg.core.combat.Combatant;
import io.github.yourname.rpg.core.combat.CooldownTracker;

/**
 * Casting an ability, end to end, with zero knowledge of Minecraft.
 * Everything here is exercised by unit tests, not by restarting a server.
 */
public final class AbilityService {
    private final AbilityRegistry registry;
    private final CooldownTracker cooldowns;
    private final EffectApplier effects;

    public AbilityService(AbilityRegistry registry, CooldownTracker cooldowns, CombatWorld world) {
        this.registry = registry;
        this.cooldowns = cooldowns;
        this.effects = new EffectApplier(world);
    }

    public sealed interface CastResult {
        record Success(AbilityDefinition ability) implements CastResult {}
        record OnCooldown(long ticksRemaining) implements CastResult {}
        record UnknownAbility(String id) implements CastResult {}
    }

    public CastResult cast(Combatant caster, String abilityId, Combatant target, Vec3 impactPoint) {
        AbilityDefinition def = registry.find(abilityId).orElse(null);
        if (def == null) return new CastResult.UnknownAbility(abilityId);

        if (!cooldowns.isReady(caster.id(), abilityId)) {
            return new CastResult.OnCooldown(cooldowns.ticksRemaining(caster.id(), abilityId));
        }

        cooldowns.trigger(caster.id(), abilityId, def.cooldownTicks());
        effects.applyAll(def.onHit(), caster, target, impactPoint);
        return new CastResult.Success(def);
    }
}
