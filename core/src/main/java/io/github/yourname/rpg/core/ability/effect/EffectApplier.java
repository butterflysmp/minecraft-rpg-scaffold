package io.github.yourname.rpg.core.ability.effect;

import io.github.yourname.rpg.core.Vec3;
import io.github.yourname.rpg.core.combat.CombatWorld;
import io.github.yourname.rpg.core.combat.Combatant;
import java.util.List;

/**
 * Interprets EffectSpec against the world. This is the beating heart of the
 * combat system and it is 100% unit-testable -- no server required.
 */
public final class EffectApplier {
    private final CombatWorld world;

    public EffectApplier(CombatWorld world) {
        this.world = world;
    }

    public void applyAll(List<EffectSpec> specs, Combatant caster, Combatant target, Vec3 origin) {
        for (EffectSpec spec : specs) {
            apply(spec, caster, target, origin);
        }
    }

    public void apply(EffectSpec spec, Combatant caster, Combatant target, Vec3 origin) {
        switch (spec) {
            case EffectSpec.Damage d -> {
                if (target != null && target.isAlive()) {
                    double mult = d.element().multiplierAgainst(target.shieldElement());
                    target.applyDamage(d.amount() * mult, d.element());
                }
            }
            case EffectSpec.Heal h -> {
                if (target != null) target.applyHeal(h.amount());
            }
            case EffectSpec.Knockback k -> {
                if (target != null) {
                    Vec3 dir = new Vec3(
                            target.position().x() - origin.x(),
                            target.position().y() - origin.y(),
                            target.position().z() - origin.z());
                    target.applyKnockback(dir, k.strength());
                }
            }
            case EffectSpec.Status s ->
                    target.applyStatus(s.statusId(), s.durationTicks(), s.amplifier());

            case EffectSpec.Visual v -> world.present(origin, v.visualId());

            // The first pulse lands one interval in, not on the landing frame:
            // a target at the impact point already took the direct hit.
            case EffectSpec.Area a -> world.schedule(origin, a.tickInterval(),
                    () -> tickArea(a, caster, origin, a.tickInterval()));
        }
    }

    private void tickArea(EffectSpec.Area area, Combatant caster, Vec3 origin, int elapsed) {
        for (Combatant c : world.combatantsNear(origin, area.radius())) {
            if (c.id().equals(caster.id())) continue;
            applyAll(area.effects(), caster, c, origin);
        }
        int next = elapsed + area.tickInterval();
        if (next <= area.durationTicks()) {
            world.schedule(origin, area.tickInterval(),
                    () -> tickArea(area, caster, origin, next));
        }
    }
}
