package io.github.yourname.rpg.core.ability.effect;

import io.github.yourname.rpg.core.Vec3;
import io.github.yourname.rpg.core.combat.CombatWorld;
import io.github.yourname.rpg.core.combat.Combatant;
import java.util.List;
import java.util.UUID;

/**
 * Interprets EffectSpec against the world. This is the beating heart of the
 * combat system and it is 100% unit-testable -- no server required.
 *
 * Nothing here may retain a Combatant beyond the tick it was handed. A lingering
 * area outlives its caster: the caster can die, log out, or unload with its
 * chunk. Areas therefore carry the caster's UUID, never the Combatant itself.
 */
public final class EffectApplier {
    private final CombatWorld world;

    public EffectApplier(CombatWorld world) {
        this.world = world;
    }

    public void applyAll(List<? extends EffectSpec> specs, Combatant caster, Combatant target, Vec3 origin) {
        applyAll(specs, idOf(caster), target, origin);
    }

    public void apply(EffectSpec spec, Combatant caster, Combatant target, Vec3 origin) {
        apply(spec, idOf(caster), target, origin);
    }

    private static UUID idOf(Combatant c) {
        return c == null ? null : c.id();
    }

    /**
     * For callers that already hold only the caster's id, because the caster may
     * be long gone -- a projectile in flight, or a lingering area. Prefer this
     * over resolving the Combatant back just to pass it in.
     */
    public void applyAllFromCaster(List<? extends EffectSpec> specs, UUID casterId,
                                   Combatant target, Vec3 origin) {
        applyAll(specs, casterId, target, origin);
    }

    private void applyAll(List<? extends EffectSpec> specs, UUID casterId, Combatant target, Vec3 origin) {
        for (EffectSpec spec : specs) {
            apply(spec, casterId, target, origin);
        }
    }

    /**
     * The one place a missing target is handled. Everything below this point
     * may assume a live target, because the type says so.
     */
    private void apply(EffectSpec spec, UUID casterId, Combatant target, Vec3 origin) {
        switch (spec) {
            case EffectSpec.Targeted t -> {
                if (target != null) applyTargeted(t, target, origin);
            }
            case EffectSpec.Untargeted u -> applyUntargeted(u, casterId, origin);
        }
    }

    private void applyTargeted(EffectSpec.Targeted spec, Combatant target, Vec3 origin) {
        switch (spec) {
            case EffectSpec.Damage d -> {
                if (target.isAlive()) {
                    double mult = d.element().multiplierAgainst(target.shieldElement());
                    target.applyDamage(d.amount() * mult, d.element());
                }
            }
            case EffectSpec.Heal h -> target.applyHeal(h.amount());
            case EffectSpec.Knockback k -> {
                Vec3 dir = new Vec3(
                        target.position().x() - origin.x(),
                        target.position().y() - origin.y(),
                        target.position().z() - origin.z());
                target.applyKnockback(dir, k.strength());
            }
            case EffectSpec.Status s ->
                    target.applyStatus(s.statusId(), s.durationTicks(), s.amplifier());
        }
    }

    private void applyUntargeted(EffectSpec.Untargeted spec, UUID casterId, Vec3 origin) {
        switch (spec) {
            case EffectSpec.Visual v -> world.present(origin, v.visualId());

            // The first pulse lands one interval in, not on the landing frame:
            // a target at the impact point already took the direct hit.
            case EffectSpec.Area a -> world.schedule(origin, a.tickInterval(),
                    () -> tickArea(a, casterId, origin, a.tickInterval()));
        }
    }

    private void tickArea(EffectSpec.Area area, UUID casterId, Vec3 origin, int elapsed) {
        for (Combatant c : world.combatantsNear(origin, area.radius())) {
            // Once the caster is gone it is no longer near anything, so this
            // simply stops matching. No need to resolve it to check.
            if (c.id().equals(casterId)) continue;
            for (EffectSpec.Targeted t : area.effects()) {
                applyTargeted(t, c, origin);
            }
        }
        int next = elapsed + area.tickInterval();
        if (next <= area.durationTicks()) {
            world.schedule(origin, area.tickInterval(),
                    () -> tickArea(area, casterId, origin, next));
        }
    }
}
