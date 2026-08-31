package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.HealthRegen;
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
 * Pays each player their passive health regeneration, on their own thread, once a second.
 *
 * <p>The mechanism Growth was missing. Max health could rise; nothing filled it, because the only
 * routes into custom HP were {@code /rpg heal} and {@code /rpg mobheal}. This is the passive one.
 *
 * <h2>Its own loop, not the reconcile loop, and that is a decision</h2>
 *
 * {@code PlayerHealthSystem}'s loop already visits every player four times a second, so folding this
 * into it would have cost nothing to write. It stays separate because that period's javadoc is
 * explicitly about how often EQUIPMENT is rescanned -- sharing it would couple regeneration speed to
 * gear-swap responsiveness, and a later tuning of either would silently retune the other.
 * {@link io.github.butterflysmp.rpg.paper.hud.StatsBarSystem} is its own poller for the same reason.
 *
 * <p>The period is 20 ticks and that number is load-bearing beyond cost: at one fire per second the
 * stat's value, the "HP/s" a stat sheet will print, and the amount paid per fire are all the SAME
 * NUMBER. There is no conversion factor for a display to drift against. {@code HealthRegen.healAmount}
 * takes the period and does the arithmetic anyway, so changing it stays correct -- it just stops
 * being free to explain.
 *
 * <h2>What is here and what is in core</h2>
 *
 * Everything except one Bukkit read. Whether to heal and how much are {@link HealthRegen}, where they
 * are pinned exactly by unit test. This class samples {@code getSaturation()} and hands the store the
 * number core computed.
 *
 * <p><b>It charges nothing, and food still gates the rate.</b> This loop was designed to add
 * exhaustion inside the saturation window, on the premise that cancelling vanilla's {@code SATIATED}
 * regen would also stop vanilla charging for it. Boot gate row 4 measured that premise on Paper
 * 26.1.2 and it is false -- saturation drained in roughly four to five seconds with our heal
 * cancelled and no charge of our own. Vanilla drains saturation regardless, so the fed/hungry two-tier
 * comes for free and a charge here would have doubled the drain. See {@code NEXT.md}.
 *
 * <h2>Lifecycle</h2>
 *
 * {@link EntityTaskTarget#isActive()} is {@code isValid() && !isDead()}, so the loop SELF-CANCELS on
 * the death screen and must be restarted on respawn. Three entry points, not one -- the trap
 * {@code StatsBarSystem} names and the reason {@code RpgListeners} calls in from three places.
 */
public final class HealthRegenSystem {

    /**
     * How often regeneration is paid. One second: see the class javadoc for why this number and the
     * rate's unit are deliberately the same.
     *
     * <p>Also the cheapest cadence that stays smooth. Every paid window emits a {@code HealthChange},
     * which drives a {@code setHealth} write and a heart-bar render for that player -- so the cost of
     * halving this is doubling those, and the visible gain is nil, because the bar only moves a
     * perceptible amount every few HP.
     */
    static final int REGEN_PERIOD_TICKS = 20;

    private final Scheduler scheduler;
    private final CombatantStats stats;

    /**
     * The live loop per player, kept so a second start cannot stack a second regeneration on one
     * player. Not merely tidiness here, as it is for a display: two live loops would pay DOUBLE the
     * rate, silently and for the rest of the session, and the only symptom would be a number that
     * felt slightly wrong.
     */
    private final Map<UUID, TaskHandle> tasks = new ConcurrentHashMap<>();

    public HealthRegenSystem(Scheduler scheduler, CombatantStats stats) {
        this.scheduler = scheduler;
        this.stats = stats;
    }

    /** Start this player's regeneration on join. */
    public void onJoin(Player player) {
        start(player);
    }

    /**
     * Restart after a death. Load-bearing for the same reason {@code StatsBarSystem.onRespawn} is:
     * the loop self-cancelled while the player was dead, so without this a respawned player never
     * regenerates again for the rest of the session -- and unlike a missing action bar, that failure
     * is invisible until someone notices they are not healing.
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
        TaskHandle task = RepeatingTask.start(target, REGEN_PERIOD_TICKS, () -> {
            // Not yet bootstrapped: skip rather than throw. current/max THROW for an untracked id.
            // Health registers synchronously in PlayerHealthSystem.onJoin, so this is the edge.
            if (!stats.tracks(id)) return true;

            // The tier gate, and the loop's only read of live player state. Vanilla owns the drain
            // that eventually flips this false; nothing here writes it.
            boolean saturated = player.getSaturation() > 0;

            double healed = HealthRegen.healAmount(stats.healthRegenValue(id), saturated,
                    REGEN_PERIOD_TICKS, stats.current(id), stats.max(id));
            if (healed <= 0) return true;   // full, dead, or a mob's rate -- pay nothing, emit nothing

            // Through the ordinary heal seam, so the heart bar, the popup filter and every other
            // HealthChange consumer see regeneration exactly as they see any other heal. Self-dealt:
            // the player is both target and dealer, which is what a passive heal is.
            stats.heal(id, healed, id, true);
            return true; // never done: runs until the player quits or dies
        }, () -> tasks.remove(id));

        // start() only SCHEDULES the first tick, so the task cannot have stopped -- and its onStop
        // cannot have run this remove -- before we store the handle. Same ordering as StatsBarSystem.
        tasks.put(id, task);
    }
}
