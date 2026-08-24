package io.github.butterflysmp.rpg.core.ability.effect;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.combat.Caster;
import io.github.butterflysmp.rpg.core.combat.CombatWorld;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import java.util.List;
import java.util.UUID;

/**
 * Interprets EffectSpec against the world. This is the beating heart of the
 * combat system and it is 100% unit-testable -- no server required.
 *
 * Nothing here may retain a Combatant beyond the tick it was handed: its handle wraps a
 * live entity. A lingering area outlives its caster, who can die, log out, or unload with
 * their chunk. Areas therefore carry a {@link Caster} -- a frozen value, an id plus the stats
 * captured on the caster's own thread at cast time -- never the Combatant itself, and never the
 * full CombatantSnapshot, whose position and liveness would be stale by the time a fuse burns
 * down. That same id is what attributes the damage.
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
     * The caster is carried as a frozen value, never held. Callers already have no live handle by
     * the time an effect lands: a projectile in flight, a lingering area, a ray mid-walk.
     */
    public void applyAll(List<? extends EffectSpec> specs, Caster caster,
                         Combatant target, Vec3 origin) {
        applyAll(specs, caster, target, origin, Vec3.ZERO);
    }

    /**
     * With a facing {@code direction} for the untargeted effects that need one (an ember fan
     * points where the caster faces). Most effects ignore it; the plain four-arg entry point
     * passes {@link Vec3#ZERO}, since a self/melee/ray/projectile impact has no fan to aim.
     */
    private void applyAll(List<? extends EffectSpec> specs, Caster caster,
                          Combatant target, Vec3 origin, Vec3 direction) {
        for (EffectSpec spec : specs) {
            apply(spec, caster, target, origin, direction);
        }
    }

    /**
     * The one place a missing target is handled. Everything below this point
     * may assume a live target, because the type says so.
     */
    private void apply(EffectSpec spec, Caster caster, Combatant target, Vec3 origin, Vec3 direction) {
        switch (spec) {
            case EffectSpec.Targeted t -> {
                if (target != null) applyTargeted(t, caster, target, origin);
            }
            case EffectSpec.Untargeted u -> applyUntargeted(u, caster, origin, direction);
        }
    }

    private void applyTargeted(EffectSpec.Targeted spec, Caster caster,
                               Combatant target, Vec3 origin) {
        switch (spec) {
            // Both direct-damage arms add the caster's CLASS DAMAGE BONUS -- the sum of their equipped
            // "+N <Class> Damage" gear whose class matches the class of the weapon they hold, frozen
            // at cast time on the Caster. The bonus is SEPARATE from the weapon's inherent damage: the
            // sword's 8 and the bow's 6 stay where they are, and this adds on top. That is why the
            // emberblade's fireball can take +Melee without inheriting the swing's 8.
            //
            // Note the gate is the HELD WEAPON'S CLASS, resolved when the desired modifier set was
            // built -- NOT DamagePayload.isBasicAttack. Keying on the payload would have reached only
            // stat-reading effects, which is precisely why a "+Magic Damage" modifier used to have
            // nothing to grip: ember_staff carries a LITERAL amount and declares attack_damage 0.
            case EffectSpec.Damage d -> {
                // Element is identity, not math -- it flavors the hit and gates kits, but
                // never multiplies the number. The port downstream carries the amount and a culprit.
                double amount = d.amount() + caster.classDamageBonus();
                if (amount > 0 && target.state().alive()) {
                    target.handle().applyDamage(amount, caster.id());
                }
            }
            case EffectSpec.WeaponDamage wd -> {
                // The basic attack: the caster's attack damage, FROZEN AT CAST TIME. Melee and
                // projectile read the identical value here. For melee, cast is effectively hit --
                // the caster is within reach -- so freezing costs nothing. For a projectile it is
                // the only legal reading: impact resolves on the TARGET'S region, which on Folia is
                // not the caster's, so asking the store for the caster's stat here would be an
                // off-thread read. There is no longer a world method that could perform one.
                //
                // A resolved 0 means unarmed (or untracked) -- deal nothing rather than fire a
                // spurious 0-damage seam. Unarmed STAYS 0 structurally, not by convention: no held
                // weapon means no weapon class means no matching grant means a bonus of 0, so
                // weapon-only melee cannot be resurrected by gear. Element is identity here too.
                double amount = caster.attackDamage() + caster.classDamageBonus();
                if (amount > 0 && target.state().alive()) {
                    target.handle().applyDamage(amount, caster.id());
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

    private void applyUntargeted(EffectSpec.Untargeted spec, Caster caster, Vec3 origin, Vec3 direction) {
        switch (spec) {
            case EffectSpec.Visual v -> world.present(origin, v.visualId());

            // Inline, on this very frame. Scheduling it -- even at delay 0 -- would put
            // the splash a tick behind the visual, because Paper clamps 0 up to 1.
            case EffectSpec.Burst b -> applyToNearby(b.effects(), caster, origin, b.radius());

            // A field, not a blast. Its first pulse lands one interval in; anything that
            // should happen at the moment of impact belongs in a Burst.
            case EffectSpec.Area a -> world.schedule(origin, a.tickInterval(),
                    () -> tickArea(a, caster, origin, a.tickInterval()));

            // A fan of REAL thrown items, each tracked by its own per-tick loop: draw the trail
            // at the live position, count the fuse, detonate mob-only where it lies. No landing
            // detection, no separate marker -- the thrown item IS the marker.
            case EffectSpec.ThrowEmbers te -> throwEmbers(te, caster, origin, direction);
        }
    }

    private void throwEmbers(EffectSpec.ThrowEmbers te, Caster caster, Vec3 origin, Vec3 facing) {
        List<Vec3> directions = EffectSpec.ThrowEmbers.fan(facing, te.anglesDegrees());
        for (Vec3 direction : directions) {
            Vec3 velocity = direction.scale(te.speed()).add(new Vec3(0, te.launchLift(), 0));
            UUID itemId = world.throwMarker(origin, velocity, te.itemId());
            // First tick runs inline on the launch frame -- the same shape as ProjectileFlight,
            // which draws its trail and steps inline before scheduling the next tick.
            trackEmber(te, caster, itemId, origin, te.fuseTicks());
        }
    }

    /**
     * One tick of a thrown ember. Reads where the item IS now, draws the trail there, and then
     * either detonates (fuse spent) or schedules the next tick AT that live position.
     *
     * Scheduling the next tick at the item's current position is what re-enters the region that
     * owns the item, so every read, particle, and the eventual burst run on the correct region
     * thread -- the same per-tick region re-entry ProjectileFlight uses for the grenade, which is
     * why the detonation is region-correct on Folia and needs no caveat. {@code lastKnown} is
     * only the fallback if the item has already vanished (removed / unloaded).
     */
    private void trackEmber(EffectSpec.ThrowEmbers te, Caster caster, UUID itemId,
                            Vec3 lastKnown, int fuseLeft) {
        Vec3 at = world.markerLocation(itemId).orElse(lastKnown);
        // One clean flame at the live position; the item's motion between ticks draws the line.
        if (te.trail() != null) world.present(at, te.trail());

        if (fuseLeft <= 0) {
            // The boom lands with the blast: same tick, same place, so they cannot diverge.
            if (te.visual() != null) world.present(at, te.visual());
            // Mob-only, like a dash's payload: a denial zone burns mobs, not players.
            applyToNearbyMobs(te.burst().effects(), caster, at, te.burst().radius());
            world.removeMarker(itemId);
            return;
        }
        world.schedule(at, 1, () -> trackEmber(te, caster, itemId, at, fuseLeft - 1));
    }

    /**
     * Everything in radius except the caster. You do not scorch yourself with your own
     * grenade, and once the caster is gone it is no longer near anything, so the check
     * simply stops matching -- no need to resolve the UUID back to a Combatant.
     */
    private void applyToNearby(List<EffectSpec.Targeted> effects, Caster caster,
                               Vec3 origin, double radius) {
        applyToEach(effects, caster, world.combatantsNear(origin, radius), origin);
    }

    /**
     * A payload against a PRE-RESOLVED set of targets -- the seam a Burst (radius set) and a
     * Dash (swept-line set) share. They differ only in who is in the set; the per-target
     * application, and the caster-exclusion that goes with it, is one loop, here.
     *
     * Targeted effects land on each target; Untargeted effects (a visual, a lingering field)
     * fire ONCE at {@code origin}, hit or miss -- a dash still flashes its flame when it
     * catches no one. Callers pass their whole {@code onHit} list; the split is made here so a
     * cast arm never re-implements it.
     */
    public void applyToSet(List<? extends EffectSpec> specs, Caster caster,
                           Iterable<Combatant> targets, Vec3 origin, Vec3 direction) {
        for (EffectSpec spec : specs) {
            if (spec instanceof EffectSpec.Untargeted u) applyUntargeted(u, caster, origin, direction);
        }
        for (Combatant c : targets) {
            if (c.id().equals(caster.id())) continue;
            for (EffectSpec spec : specs) {
                if (spec instanceof EffectSpec.Targeted t) applyTargeted(t, caster, c, origin);
            }
        }
    }

    private void applyToEach(List<EffectSpec.Targeted> effects, Caster caster,
                             Iterable<Combatant> targets, Vec3 origin) {
        for (Combatant c : targets) {
            if (c.id().equals(caster.id())) continue;
            for (EffectSpec.Targeted t : effects) {
                applyTargeted(t, caster, c, origin);
            }
        }
    }

    /**
     * Everything in radius except the caster AND players -- a mob-only blast. The player skip
     * is the same rule SweptLine applies to a dash's payload, read off the frozen snapshot so
     * a core test can guard it: delete the skip and an in-radius player is wrongly burned.
     */
    private void applyToNearbyMobs(List<EffectSpec.Targeted> effects, Caster caster,
                                   Vec3 origin, double radius) {
        for (Combatant c : world.combatantsNear(origin, radius)) {
            if (c.id().equals(caster.id())) continue;
            if (c.state().player()) continue;
            for (EffectSpec.Targeted t : effects) {
                applyTargeted(t, caster, c, origin);
            }
        }
    }

    private void tickArea(EffectSpec.Area area, Caster caster, Vec3 origin, int elapsed) {
        applyToNearby(area.effects(), caster, origin, area.radius());

        int next = elapsed + area.tickInterval();
        if (next <= area.durationTicks()) {
            world.schedule(origin, area.tickInterval(),
                    () -> tickArea(area, caster, origin, next));
        }
    }
}
