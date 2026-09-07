package io.github.butterflysmp.rpg.paper.adapter;

import org.bukkit.entity.LivingEntity;

import java.util.UUID;

/**
 * Binds a real {@link LivingEntity} to the Bukkit-free {@link ScorchSink} seam. 1:1 with the store
 * and the damage port, no logic -- the clock, the cap, the stacks and the lifetime all live in the
 * tested {@link ScorchStatus}, exactly as {@link EntitySpeedAttribute} carries none of Soaked's.
 *
 * <p><b>{@code bypassesDefense = true} is set HERE, once, and is not a parameter of the seam.</b>
 * Scorch always bypasses; making it a per-call argument would invite a caller to pass false and
 * silently reinstate the armour cut the operator ruled out. The one place it is decided is the one
 * place it is written.
 *
 * <p><b>{@code wasCrit = false}, deliberately.</b> A burn tick is not a swing: there is no roll, and
 * {@code CombatantHandle} is explicit that the flag is presentation-only with the multiplier already
 * inside the amount. A DoT that flashed crit particles every second would be lying about a roll that
 * never happened.
 */
public final class EntityScorchSink implements ScorchSink {

    private final LivingEntity entity;
    private final AdapterContext ctx;

    public EntityScorchSink(LivingEntity entity, AdapterContext ctx) {
        this.entity = entity;
        this.ctx = ctx;
    }

    /**
     * The victim's CUSTOM max, straight off the store.
     *
     * Returns 0 for an untracked combatant, which makes the burn 0 rather than throwing --
     * {@code CombatantStats.max} throws on an untracked id, and a scorch whose victim stopped being
     * tracked mid-burn must fizzle rather than raise inside a scheduled tick. The task stops on its
     * own when the entity goes away; this covers the narrower window where the entity lives but the
     * store no longer knows it.
     */
    @Override public double victimMaxHealth() {
        UUID id = entity.getUniqueId();
        return ctx.stats().tracks(id) ? ctx.stats().max(id) : 0.0;
    }

    @Override public void deal(double amount, UUID applierId) {
        if (amount <= 0) return;
        BukkitCombatant.of(entity, ctx).handle().applyDamage(amount, applierId, false, true);
    }
}
