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
 * Nothing here may retain a Combatant beyond the tick it was handed: its handle wraps a
 * live entity. A lingering area outlives its caster, who can die, log out, or unload with
 * their chunk. Areas therefore carry the caster's UUID, never the Combatant itself -- and
 * that same UUID is what attributes the damage.
 *
 * Reads come off the snapshot, writes go to the handle. Neither is interchangeable, and
 * the types enforce it: you cannot ask a handle a question, and you cannot hit a snapshot.
 */
public final class EffectApplier {
    private final CombatWorld world;

    public EffectApplier(CombatWorld world) {
        this.world = world;
    }

    /**
     * The caster is identified, never held. Callers already have only an id by the time an
     * effect lands: a projectile in flight, a lingering area, a ray mid-walk.
     */
    public void applyAll(List<? extends EffectSpec> specs, UUID casterId,
                         Combatant target, Vec3 origin) {
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
                if (target != null) applyTargeted(t, casterId, target, origin);
            }
            case EffectSpec.Untargeted u -> applyUntargeted(u, casterId, origin);
        }
    }

    private void applyTargeted(EffectSpec.Targeted spec, UUID casterId,
                               Combatant target, Vec3 origin) {
        switch (spec) {
            case EffectSpec.Damage d -> {
                if (target.state().alive()) {
                    // The elemental multiplier is resolved HERE, against the snapshot's
                    // shield. The port downstream carries only a number and a culprit.
                    double mult = d.element().multiplierAgainst(target.state().shieldElement());
                    target.handle().applyDamage(d.amount() * mult, casterId);
                }
            }
            case EffectSpec.Heal h -> target.handle().applyHeal(h.amount());
            case EffectSpec.Knockback k -> {
                Vec3 position = target.state().position();
                Vec3 dir = new Vec3(
                        position.x() - origin.x(),
                        position.y() - origin.y(),
                        position.z() - origin.z());
                target.handle().applyKnockback(dir, k.strength());
            }
            case EffectSpec.Status s ->
                    target.handle().applyStatus(s.statusId(), s.durationTicks(), s.amplifier());
        }
    }

    private void applyUntargeted(EffectSpec.Untargeted spec, UUID casterId, Vec3 origin) {
        switch (spec) {
            case EffectSpec.Visual v -> world.present(origin, v.visualId());

            // Inline, on this very frame. Scheduling it -- even at delay 0 -- would put
            // the splash a tick behind the visual, because Paper clamps 0 up to 1.
            case EffectSpec.Burst b -> applyToNearby(b.effects(), casterId, origin, b.radius());

            // A field, not a blast. Its first pulse lands one interval in; anything that
            // should happen at the moment of impact belongs in a Burst.
            case EffectSpec.Area a -> world.schedule(origin, a.tickInterval(),
                    () -> tickArea(a, casterId, origin, a.tickInterval()));
        }
    }

    /**
     * Everything in radius except the caster. You do not scorch yourself with your own
     * grenade, and once the caster is gone it is no longer near anything, so the check
     * simply stops matching -- no need to resolve the UUID back to a Combatant.
     */
    private void applyToNearby(List<EffectSpec.Targeted> effects, UUID casterId,
                               Vec3 origin, double radius) {
        for (Combatant c : world.combatantsNear(origin, radius)) {
            if (c.id().equals(casterId)) continue;
            for (EffectSpec.Targeted t : effects) {
                applyTargeted(t, casterId, c, origin);
            }
        }
    }

    private void tickArea(EffectSpec.Area area, UUID casterId, Vec3 origin, int elapsed) {
        applyToNearby(area.effects(), casterId, origin, area.radius());

        int next = elapsed + area.tickInterval();
        if (next <= area.durationTicks()) {
            world.schedule(origin, area.tickInterval(),
                    () -> tickArea(area, casterId, origin, next));
        }
    }
}
