package io.github.butterflysmp.rpg.paper.hud;

import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import io.github.butterflysmp.rpg.paper.scheduler.TaskHandle;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends each player their own action-bar stats line, on their own thread, often enough that it never
 * fades.
 *
 * Display only. Nothing here regenerates, spends, or clamps anything: mana regenerates lazily inside
 * {@link ResourcePool} on read, and health is owned by the health system. This class reads two
 * stores and draws.
 *
 * <h2>Why per-player and not one global loop</h2>
 * The loop runs on a {@link EntityTaskTarget}, so each player's bar is built and sent on the thread
 * that owns THAT player -- which is what makes it correct under Folia. Iterating
 * {@code getOnlinePlayers()} from one global task and calling {@code sendActionBar} across regions is
 * exactly the pattern this avoids.
 *
 * <h2>Lifecycle</h2>
 * {@link EntityTaskTarget#isActive()} is {@code entity.isValid() && !entity.isDead()}, so the loop
 * SELF-CANCELS on the death screen and must be restarted on respawn -- see {@link #onRespawn}. That
 * is the trap this class is most likely to regress on, and it is why {@code RpgListeners} calls into
 * here from three places rather than one.
 */
public final class StatsBarSystem {

    /**
     * How often the bar is redrawn. The action bar fades after roughly three seconds, so half a
     * second keeps it solid with a wide margin while costing two store reads and one packet per
     * player per half second. Tunable: raise it and the bar starts to flicker as it fades between
     * sends; lower it and you pay for frames nobody can perceive.
     */
    private static final int BAR_PERIOD_TICKS = 10;

    private final Scheduler scheduler;
    private final CombatantStats stats;
    private final ResourcePool resources;

    /**
     * The live loop per player, kept so a second start cannot stack a second bar on one player.
     * The health and nameplate loops discard their handles and re-start unconditionally, which is
     * safe only because their targets have always already self-cancelled; two live loops here would
     * mean two sends racing on one action bar with no way to stop either.
     */
    private final Map<UUID, TaskHandle> tasks = new ConcurrentHashMap<>();

    public StatsBarSystem(Scheduler scheduler, CombatantStats stats, ResourcePool resources) {
        this.scheduler = scheduler;
        this.stats = stats;
        this.resources = resources;
    }

    /** Start this player's bar on join. */
    public void onJoin(Player player) {
        start(player);
    }

    /**
     * Restart the bar after a death. Load-bearing: the loop self-cancelled while the player was dead
     * (the target reports inactive), so without this a respawned player's bar never returns for the
     * rest of the session. Mirrors {@link #onJoin}.
     */
    public void onRespawn(Player player) {
        start(player);
    }

    /** Drop the handle on logout so nothing leaks across sessions. */
    public void onQuit(UUID playerId) {
        TaskHandle task = tasks.remove(playerId);
        if (task != null) task.cancel();
    }

    private void start(Player player) {
        UUID id = player.getUniqueId();
        TaskHandle existing = tasks.get(id);
        if (existing != null && existing.isRunning()) return;

        EntityTaskTarget target = new EntityTaskTarget(player, scheduler);
        TaskHandle task = RepeatingTask.start(target, BAR_PERIOD_TICKS, () -> {
            // Not yet bootstrapped: skip this frame rather than inventing numbers. CombatantStats
            // .current/.max THROW for an untracked id -- they do not return 0 -- so an unguarded read
            // would throw every period rather than merely display something wrong. Health registers
            // synchronously in PlayerHealthSystem.onJoin, so this is the edge, not the norm.
            if (!stats.tracks(id)) return true;

            player.sendActionBar(StatsBarText.of(
                    stats.current(id), stats.max(id),
                    resources.current(id, ResourceCost.DEFAULT_RESOURCE), resources.max()));
            return true; // never done: runs until the player quits or dies
        }, () -> tasks.remove(id));

        // start() only SCHEDULES the first tick, so the task cannot have stopped -- and its onStop
        // cannot have run this remove -- before we store the handle. Same ordering as ImmobilizeStatus.
        tasks.put(id, task);
    }
}
