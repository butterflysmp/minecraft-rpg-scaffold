package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import io.github.butterflysmp.rpg.core.combat.stat.HealthListener;
import io.github.butterflysmp.rpg.core.combat.stat.VanillaMirror;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * F20, the vanilla mirror (PLAN-mob-scaling.md §7): after every write to a tracked MOB's custom store,
 * its vanilla health is set to the same FRACTION of its vanilla max. The arithmetic, and why, is
 * {@link VanillaMirror}'s. This class only reaches the entity.
 *
 * <p><b>ONE PLACE, NOT EACH PATH.</b> This is a listener on the {@link HealthChange} seam, beside the
 * displays and {@code MobDeathSystem}. {@code CombatantStats} is the only writer of current health, and
 * each of its three writers ({@code damage}, {@code heal}, {@code reconcileMaxModifiers}) emits exactly
 * one change. So every damage path (the tokened riders, {@code applyDamage} with every ability, weapon,
 * scorch, Ignite and reflect) and every heal is mirrored without being named here. A damage path added
 * later is mirrored without anyone remembering to. The seed is the one store write that emits nothing,
 * so {@code MobNameplateManager}'s seed calls {@link #apply} once as well.
 *
 * <p><b>Threading:</b> {@code onChange} runs on the TARGET entity's owning thread. That is the contract
 * {@code MobDeathSystem} already relies on for {@code setHealth(0)}, so no scheduler hop is needed.
 *
 * <p><b>{@code setHealth} is safe to call from a listener</b>, read from the pinned jar (26.1.2):
 * {@code CraftLivingEntity.setHealth} writes the synced health data and nothing else. It raises no damage
 * or regain event, so it cannot recurse into our listeners. It plays no hurt animation or sound and leaves
 * the invulnerability ticks alone. It kills only at exactly 0, which {@link VanillaMirror} never returns.
 * No entity class overrides it, and the dragon's phase logic reads health only inside {@code hurt()}.
 */
public final class MobVanillaMirror implements HealthListener {

    @Override
    public void onChange(HealthChange change) {
        if (!VanillaMirror.shouldMirror(change)) return;
        if (!(Bukkit.getEntity(change.target()) instanceof LivingEntity mob)) return;
        apply(mob, change.newCurrent(), change.max());
    }

    /**
     * Set {@code mob}'s vanilla health to mirror a store at {@code current} of {@code customMax}. It
     * writes nothing for a player, a dead mob, a mob with no MAX_HEALTH attribute, or a store at 0
     * (death is ours). Only {@code setHealth} is called; the attribute is never written (M9).
     */
    public static void apply(LivingEntity mob, double current, double customMax) {
        if (mob instanceof Player || mob.isDead()) return;
        var attr = mob.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        VanillaMirror.healthFor(current, customMax, attr.getValue()).ifPresent(mob::setHealth);
    }
}
