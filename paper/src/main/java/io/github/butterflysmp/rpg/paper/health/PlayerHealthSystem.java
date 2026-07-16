package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import io.github.butterflysmp.rpg.core.combat.stat.HealthListener;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * The Paper side of player custom health: the display listener, the per-player equip/unequip
 * reconcile loop, and the join/quit lifecycle. Mobs are not handled here -- their nameplate is the
 * next phase; this listener ignores non-player changes.
 *
 * It is the {@link HealthListener} the store emits to, so it renders the heart bar whenever custom
 * health moves. Construction is two-step to break the cycle (the store needs a listener, this needs
 * the store): build this, build the store with it, then {@link #bind}.
 */
public final class PlayerHealthSystem implements HealthListener {

    /**
     * How often the reconcile loop rescans a player's equipment. Equipment changes are rare, so a
     * few times a second is ample and cheap; the cost is a handful of slot reads per player per tick
     * of the period, and at most one tick of latency on a stat change (imperceptible).
     */
    private static final int RECONCILE_PERIOD_TICKS = 5;

    private final Scheduler scheduler;
    private final Keys keys;
    private final HeartBarRenderer renderer = new HeartBarRenderer();
    private CombatantStats stats;

    public PlayerHealthSystem(Scheduler scheduler, Keys keys) {
        this.scheduler = scheduler;
        this.keys = keys;
    }

    /** Wire the store this renders. Called once in onEnable, right after the store is built. */
    public void bind(CombatantStats stats) {
        this.stats = stats;
    }

    /** The store this owns, for the dev commands that damage/heal through the observable path. */
    public CombatantStats stats() {
        return stats;
    }

    /**
     * A health change: refresh the player's heart bar. Only players have a heart bar; a mob change
     * (its nameplate is the next phase) is ignored. Resolves the player and hops onto its own thread
     * before touching the Bukkit attribute -- the change may have been emitted from any thread.
     */
    @Override
    public void onChange(HealthChange change) {
        if (!change.targetIsPlayer()) return;
        Player player = Bukkit.getPlayer(change.target());
        if (player == null) return;
        scheduler.onEntity(player, () ->
                renderer.render(new EntityHeartBar(player), change.newCurrent(), change.max()));
    }

    /**
     * Register the player at base 100 full, render once (resetting a vanilla bar left stale from a
     * previous session), and start the reconcile loop that tracks their equipped +HP items.
     */
    public void onJoin(Player player) {
        UUID id = player.getUniqueId();
        stats.register(id, CombatantStats.DEFAULT_PLAYER_BASE, true);
        scheduler.onEntity(player, () ->
                renderer.render(new EntityHeartBar(player), stats.current(id), stats.max(id)));
        startReconcileLoop(player);
    }

    /** Drop the player's health state on logout, so no modifier or entry leaks across sessions. */
    public void onQuit(UUID id) {
        stats.clear(id);
    }

    /**
     * A per-player loop that rescans equipment and converges the store's max modifiers to it. Same
     * shape as the Soaked countdown -- {@link RepeatingTask} on an {@link EntityTaskTarget} -- so it
     * self-cancels when the player leaves and never touches a removed entity. The body never returns
     * false: it runs until the player is gone, at which point the target reports inactive and the
     * task stops. Cleanup of the store is the quit handler's job, not the loop's.
     */
    private void startReconcileLoop(Player player) {
        EntityTaskTarget target = new EntityTaskTarget(player, scheduler);
        UUID id = player.getUniqueId();
        RepeatingTask.start(target, RECONCILE_PERIOD_TICKS, () -> {
            Map<String, Double> desired = HealthModifierItems.desiredModifiers(player, keys);
            stats.reconcileMaxModifiers(id, desired);
            return true;
        }, () -> { });
    }
}
