package io.github.butterflysmp.rpg.core.ability.effect;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.combat.Caster;
import io.github.butterflysmp.rpg.core.combat.CombatWorld;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.enchant.ShockwaveStreaks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * WHAT A PRIMARY HIT DOES TO A BEHAVIOURAL ENCHANT (the seat's B2-B4, 2026-11-23). ONE instance per server, long-lived
 * (paper holds it on its {@code AdapterContext}; a test world holds its own), because the state it keeps outlives any
 * one cast. Every {@code CombatWorld.primaryHit} lands here.
 *
 * <p><b>What counts as a hit (B4, and the seat's reading of Kinetic Tremors):</b> a damage that {@code EffectApplier}
 * actually applied, by a caster that {@code countsAsHit} (not a damage-over-time tick, not secondary damage), carrying at
 * least one behaviour, on a target that is not a player. Each pellet of a spread is its own damage, so its own hit.
 *
 * <p><b>The shockwave (B4):</b> SECONDARY damage. It is landed by {@link EffectApplier#applyToSet} through the same Damage
 * arm as an ability hit, under {@code Caster.asSecondary("<enchant id>/shockwave")}: so it carries its own source on the
 * PLAYERHIT line, every damage term of the hit that fired it (below), no behaviour (it can trigger nothing), and is not a
 * hit. It strikes only mobs: never the caster, never a player.
 *
 * <p><b>Every term of a shockwave's damage</b> (the Damage arm, READ): {@code GearScore.scaledDamage(level's amount,
 * triggerScore)} (amount x score / 100; Ben's "Damage will scale with the gear score"), times
 * {@code DamageEnchants.multiplier(enchantDamagePercent)} (a damage enchant active on the same weapon), plus
 * {@code classDamageBonus + weaponLevelDamage} (his "+N Class Damage" gear and, on a weapon hit, his level's damage
 * bonus), times {@code chargeScale} (a basic swing's charge; 1 for a trigger), times {@code critMultiplier} (the hit's own
 * crit, drawn once at the cast). All of them the hit's, frozen when the weapon made it.
 */
public final class BehaviourHits {

    private final ShockwaveStreaks streaks = new ShockwaveStreaks();

    public ShockwaveStreaks streaks() {
        return streaks;
    }

    /** One primary hit by {@code caster} on {@code target}, in {@code world}. */
    public void onHit(CombatWorld world, Caster caster, Combatant target) {
        if (!caster.countsAsHit() || caster.behaviours().isEmpty()) return;
        if (target.state().player()) return;   // only what his abilities may hurt counts, and that is mobs
        long now = world.currentTick();
        for (ActiveBehaviour active : caster.behaviours()) {
            switch (active.behaviour()) {
                case EnchantBehaviour.Shockwave shockwave -> {
                    if (streaks.hit(caster.id(), active.enchantId(), target.id(), now, shockwave)) {
                        shockwave(world, caster, active, shockwave, target.state().position());
                    }
                }
                // An airburst acts on the FLIGHT (CastExecutor.launchToSplit), never on a hit.
                case EnchantBehaviour.Airburst ignored -> { }
                // A shatter acts on the PRESS (CastExecutor.launchStone), never on a hit; its stone and chunks are hits.
                case EnchantBehaviour.Shatter ignored -> { }
                // A focused beam acts on the VOLLEY (CastExecutor.focusedBeam), never on a hit; its one ray is a hit.
                case EnchantBehaviour.FocusedBeam ignored -> { }
                // A granted trigger acts when its gesture is pressed (GrantedTriggers), never on a hit.
                case EnchantBehaviour.GrantTrigger ignored -> { }
            }
        }
    }

    /** The source a shockwave's damage carries on the PLAYERHIT line. */
    public static String shockwaveSource(String enchantId) {
        return enchantId + "/shockwave";
    }

    private static void shockwave(CombatWorld world, Caster caster, ActiveBehaviour active,
                                  EnchantBehaviour.Shockwave shockwave, Vec3 origin) {
        Caster secondary = caster.asSecondary(shockwaveSource(active.enchantId()));
        UUID self = caster.id();
        double radius = shockwave.radiusAt(active.level());
        List<Combatant> struck = new ArrayList<>();
        for (Combatant c : world.combatantsNear(origin, radius)) {
            if (c.id().equals(self)) continue;          // never himself
            if (c.state().player()) continue;           // never a player
            if (!c.state().alive()) continue;
            // THE RADIUS IS A SPHERE, CENTRE TO CENTRE (feet to the struck mob's feet). READ: the server's combatantsNear
            // is World#getNearbyEntities(center, r, r, r), a BOX whose corners reach r x sqrt(3); without this a level-1
            // wave would strike a mob 5.2 blocks off a corner. With it, the server and the fake world agree.
            if (c.state().position().distanceSquared(origin) > radius * radius) continue;
            struck.add(c);
        }
        List<EffectSpec> specs = new ArrayList<>();
        if (shockwave.visual() != null) specs.add(new EffectSpec.Visual(shockwave.visual()));
        specs.add(new EffectSpec.Damage(shockwave.damageAt(active.level()), shockwave.element()));
        new EffectApplier(world).applyToSet(specs, secondary, struck, origin, Vec3.ZERO);
    }
}
