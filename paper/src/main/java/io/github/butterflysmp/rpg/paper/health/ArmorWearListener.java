package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import io.github.butterflysmp.rpg.core.combat.stat.HealthListener;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ArmorWear;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import io.github.butterflysmp.rpg.paper.weapon.ArmorDurability;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

/**
 * D2' (the seat, 2026-11-25): the ONE place armor wear is triggered. It is a listener on the store, because every hit
 * on a player converges on {@code CombatantStats.damage}, and the store is the only place that knows whether Defense
 * priced the hit ({@code HealthChange.throughDefense}). The decision is core's {@link ArmorWear#wears}; the write hops
 * to the player's own thread, as {@code PlayerHealthSystem.onChange}'s render does.
 *
 * <p>The cooldown tracker arrives through a supplier because the store's listener is composed before the tracker
 * exists (RpgPlugin's order). It is read only at a hit, long after enable.
 */
public final class ArmorWearListener implements HealthListener {

    private final Scheduler scheduler;
    private final Keys keys;
    private final ArmorRegistry armor;
    private final Supplier<CooldownTracker> cooldowns;

    public ArmorWearListener(Scheduler scheduler, Keys keys, ArmorRegistry armor, Supplier<CooldownTracker> cooldowns) {
        this.scheduler = scheduler;
        this.keys = keys;
        this.armor = armor;
        this.cooldowns = cooldowns;
    }

    @Override
    public void onChange(HealthChange change) {
        if (!ArmorWear.wears(change)) return;
        Player player = Bukkit.getPlayer(change.target());
        if (player == null) return;
        scheduler.onEntity(player, () -> ArmorDurability.applyWearOnHit(player, keys, armor, cooldowns.get()));
    }
}
