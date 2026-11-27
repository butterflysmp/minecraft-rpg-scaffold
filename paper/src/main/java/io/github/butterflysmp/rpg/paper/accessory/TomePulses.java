package io.github.butterflysmp.rpg.paper.accessory;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.accessory.AccessoryBehaviour;
import io.github.butterflysmp.rpg.core.accessory.AccessoryContributions;
import io.github.butterflysmp.rpg.core.accessory.AccessoryContributions.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.accessory.HealingPulses;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import io.github.butterflysmp.rpg.core.progression.PlayerLevel;
import io.github.butterflysmp.rpg.paper.adapter.CombatSides;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import io.github.butterflysmp.rpg.paper.scheduler.TaskHandle;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.logging.Logger;

/**
 * THE HEALING PULSE'S CLOCK AND ITS WIRING (the seat's H3, H4, H6, H9, H10; 2026-11-26). The decision is core's
 * ({@link HealingPulses}); this class decides WHEN, gathers who is near on the wearer's thread, and applies each heal on
 * its target's thread.
 *
 * <h2>When it runs (H3): exactly while the accessory CONTRIBUTES</h2>
 *
 * {@code PlayerHealthSystem}'s reconcile pass reads the player's accessories ONCE ({@code accessoryContributions}) and
 * hands this the behaviours that reading found active ({@link #reconcile}). Nothing here reads a slot. At each pulse the
 * pulse asks the SAME function again ({@code contributions}), so a pulse due between an unequip and the next pass does not
 * fire.
 *
 * <h2>The clock (H4): one task per wearer, in server ticks</h2>
 *
 * <ul>
 *   <li><b>The anchor</b> is the reconcile pass that first sees the behaviour active (an equip, a login's profile load, a
 *       class change to mage, a respawn). CC'S CALL: that pass, not the equip's own tick -- the stats an accessory grants
 *       start at that same pass, so "starts contributing" has one meaning; it lands up to 4 ticks after the act
 *       ({@code PlayerHealthSystem}'s 5-tick period). The alternative, hooking each act, misses any act nobody listed.
 *   <li><b>The first pulse is {@code interval_ticks} after the anchor</b> and every {@code interval_ticks} after
 *       ({@link RepeatingTask#start} schedules its first step one period out, READ).
 *   <li><b>It stops at once</b> when the pass or the pulse sees it inactive, on quit ({@link #onQuit}), and on death: an
 *       {@link EntityTaskTarget} is inactive while dead, so the step that finds him dead stops the task without pulsing.
 *       A later start is a new task, so the count restarts from 0: re-equipping can never pulse sooner than the interval.
 *   <li><b>A world change keeps the cadence</b>: the task follows the entity, and nothing here watches the world.
 * </ul>
 *
 * <p><b>Why an entity task and not a global scan.</b> It is {@code HealthRegenSystem}'s shape (a {@link RepeatingTask} on
 * an {@link EntityTaskTarget}), so it runs on the wearer's own thread, on Paper the main thread and on Folia his region,
 * which is where {@code World#getNearbyEntities} around his feet is legal. Only wearers have a task; a player without an
 * active tome costs one map lookup per reconcile pass.
 *
 * <h2>The heal (H6)</h2>
 *
 * Through {@link CombatantStats#heal}, the engine's one heal path ({@code BukkitCombatant.applyHeal} and passive regen use
 * it), with the WEARER as the dealer. The wearer is healed inline (this is his thread); every other target on its own
 * thread through {@code scheduler.onEntity}, as {@code BukkitCombatant.applyHeal} does. Each application re-reads the
 * target: dead or untracked is skipped, and the amount is never more than it misses then. No mana is read or spent (Ben:
 * "No mana cost").
 */
public final class TomePulses {

    private final Scheduler scheduler;
    private final CombatantStats stats;
    private final ProfileService profiles;
    private final Keys keys;
    private final Logger log;
    private final BooleanSupplier trace;
    private final IntSupplier tick;
    /** The pulse-time re-check: {@code PlayerHealthSystem}'s own reading of a player's accessories. */
    private final Function<UUID, AccessoryContributions> contributions;

    private record Running(TaskHandle task, String accessoryId) {}

    private final Map<UUID, Running> running = new ConcurrentHashMap<>();

    public TomePulses(Scheduler scheduler, CombatantStats stats, ProfileService profiles, Keys keys, Logger log,
                      BooleanSupplier trace, IntSupplier tick, Function<UUID, AccessoryContributions> contributions) {
        this.scheduler = scheduler;
        this.stats = stats;
        this.profiles = profiles;
        this.keys = keys;
        this.log = log;
        this.trace = trace;
        this.tick = tick;
        this.contributions = contributions;
    }

    /** The first healing pulse among a reading's active behaviours, if any. */
    static Optional<ActiveBehaviour> pulseOf(List<ActiveBehaviour> active) {
        for (ActiveBehaviour a : active) {
            if (a.behaviour() instanceof AccessoryBehaviour.HealingPulse) return Optional.of(a);
        }
        return Optional.empty();
    }

    /**
     * Called by the reconcile pass, on the player's thread, with what its one reading found active. Starts his clock when
     * a pulse is active and none runs; stops it when none is active.
     */
    public void reconcile(Player player, List<ActiveBehaviour> active) {
        UUID id = player.getUniqueId();
        Optional<ActiveBehaviour> pulse = pulseOf(active);
        Running current = running.get(id);
        if (pulse.isEmpty() || player.isDead()) {
            if (current != null) stop(id, "inactive");
            return;
        }
        if (current != null && current.task().isRunning()) return;
        start(player, pulse.get());
    }

    /** Quit: stop his clock and forget it. */
    public void onQuit(UUID id) {
        stop(id, "quit");
    }

    /** Is a pulse clock running for this player? For the selftest's probes and the wiring test. */
    public boolean runs(UUID id) {
        Running r = running.get(id);
        return r != null && r.task().isRunning();
    }

    private void stop(UUID id, String why) {
        Running r = running.remove(id);
        if (r == null) return;
        r.task().cancel();
        if (trace.getAsBoolean()) {
            org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(id);
            log.info("TOMEPULSE STOP wearer=" + (p == null ? id.toString() : p.getName()) + " tick=" + tick.getAsInt()
                    + " reason=" + why);
        }
    }

    private void start(Player player, ActiveBehaviour active) {
        UUID id = player.getUniqueId();
        AccessoryBehaviour.HealingPulse pulse = (AccessoryBehaviour.HealingPulse) active.behaviour();
        EntityTaskTarget target = new EntityTaskTarget(player, scheduler, log);
        Running[] self = new Running[1];
        TaskHandle task = RepeatingTask.start(target, pulse.intervalTicks(), "tome-pulse", () -> {
            // THE PULSE-TIME RE-CHECK, through the reconcile's own function (H3): inactive now is no pulse, and the end.
            Optional<ActiveBehaviour> still = pulseOf(contributions.apply(id).behaviours());
            if (still.isEmpty()) return false;
            pulse(player, (AccessoryBehaviour.HealingPulse) still.get().behaviour());
            return true;
        }, () -> {
            // The task ended ITSELF (the re-check found it inactive, or he died): logged here. A stop() already removed
            // its entry before cancelling, so this remove fails and that stop is logged once, by stop().
            if (running.remove(id, self[0]) && trace.getAsBoolean()) {
                log.info("TOMEPULSE STOP wearer=" + player.getName() + " tick=" + tick.getAsInt() + " reason=ended");
            }
        });
        self[0] = new Running(task, active.accessoryId());
        running.put(id, self[0]);
        if (trace.getAsBoolean()) {
            log.info("TOMEPULSE START wearer=" + player.getName() + " accessory=" + active.accessoryId() + " slot="
                    + active.slot() + " tick=" + tick.getAsInt() + " interval=" + pulse.intervalTicks());
        }
    }

    /** H10: the pulse's line, written on EVERY pulse while trace is on, a pulse that heals nobody included. */
    static String pulseLine(String wearer, int level, int tick, int candidates, int healed) {
        return "TOMEPULSE wearer=" + wearer + " level=" + level + " tick=" + tick + " candidates=" + candidates
                + " healed=" + healed;
    }

    /** H10: one line per healed target, read at the heal on the target's thread. */
    static String targetLine(String target, double before, double amount, double after) {
        return String.format(Locale.ROOT, "TOMEPULSE target=%s before=%.3f amount=%.3f after=%.3f", target, before, amount,
                after);
    }

    /** One pulse, on the wearer's thread. */
    private void pulse(Player wearer, AccessoryBehaviour.HealingPulse pulse) {
        UUID wearerId = wearer.getUniqueId();
        Optional<PlayerProfile> profile = profiles.profile(wearerId);
        if (profile.isEmpty()) return;   // cannot happen while it contributes (the class gate reads the profile)
        // L: his EFFECTIVE level, as every level bonus reads it (CC'S CALL; the alternative is the raw level past 50).
        int level = PlayerLevel.effectiveLevel(profile.get().lifetimeXp());

        Location feet = wearer.getLocation();
        String world = wearer.getWorld().getName();
        List<HealingPulses.Candidate> nearby = new ArrayList<>();
        Map<UUID, LivingEntity> bodies = new java.util.HashMap<>();
        nearby.add(candidate(wearer));
        bodies.put(wearerId, wearer);
        double r = pulse.radius();
        for (Entity e : wearer.getWorld().getNearbyEntities(feet, r, r, r)) {
            if (!(e instanceof LivingEntity living) || e.getUniqueId().equals(wearerId)) continue;
            nearby.add(candidate(living));
            bodies.put(living.getUniqueId(), living);
        }
        HealingPulses.Decision d = HealingPulses.decide(pulse, level, world, vec(feet), nearby);
        boolean tracing = trace.getAsBoolean();
        if (tracing) {
            log.info(pulseLine(wearer.getName(), level, tick.getAsInt(), d.candidates(), d.healed()));
        }
        for (HealingPulses.Heal h : d.heals()) {
            LivingEntity body = bodies.get(h.id());
            if (!(body instanceof Player target)) continue;
            if (target == wearer) {
                apply(target, wearerId, h, pulse);           // his own thread: now
            } else {
                scheduler.onEntity(target, () -> apply(target, wearerId, h, pulse));   // the target's own thread
            }
        }
    }

    /** Heal one target, on its own thread: re-read, never past max, never the dead; then the feedback. */
    private void apply(Player target, UUID wearerId, HealingPulses.Heal h, AccessoryBehaviour.HealingPulse pulse) {
        UUID id = target.getUniqueId();
        if (!target.isValid() || target.isDead() || !stats.tracks(id)) return;
        double before = stats.current(id);
        if (before <= 0) return;
        double amount = Math.min(h.amount(), stats.max(id) - before);
        if (amount <= 0) return;
        stats.heal(id, amount, wearerId, true);
        double after = stats.current(id);
        if (trace.getAsBoolean()) {
            log.info(targetLine(target.getName(), before, amount, after));
        }
        feedback(target, pulse);
    }

    /**
     * H9 (SEAT PLACEHOLDER 2026-11-26, the old game's): a few happy-villager particles and one heart on the healed player,
     * and a soft sound to that player only. Keys from the YAML; none is shown when nobody is healed (this is only ever
     * called after a heal landed). Logged under trace so a gate can read that it fired.
     */
    private void feedback(Player target, AccessoryBehaviour.HealingPulse pulse) {
        Location at = target.getLocation().add(0, 1.0, 0);
        if (pulse.particle() != null) {
            target.getWorld().spawnParticle(particle(pulse.particle()), at, pulse.particleCount(), 0.4, 0.5, 0.4, 0.0);
        }
        if (pulse.heartParticle() != null) {
            target.getWorld().spawnParticle(particle(pulse.heartParticle()), at.clone().add(0, 1.2, 0), 1, 0, 0, 0, 0.0);
        }
        if (pulse.sound() != null) target.playSound(target.getLocation(), pulse.sound(), 0.6f, 1.2f);
        if (trace.getAsBoolean()) {
            log.info("TOMEPULSE feedback target=" + target.getName() + " particle=" + pulse.particle() + "x"
                    + pulse.particleCount() + " heart=" + pulse.heartParticle() + " sound=" + pulse.sound());
        }
    }

    private static Particle particle(String name) {
        return Particle.valueOf(name.toUpperCase(Locale.ROOT));   // checked at load (AccessoryLoader)
    }

    /** One entity as the decision sees it. Health is the store's, as the stats sheet reads it; untracked is not alive. */
    private HealingPulses.Candidate candidate(LivingEntity e) {
        UUID id = e.getUniqueId();
        boolean tracked = stats.tracks(id);
        HealingPulses.Kind kind = kindOf(e);
        boolean spectator = e instanceof Player p && p.getGameMode() == GameMode.SPECTATOR;
        return new HealingPulses.Candidate(id, e.getName(), kind, e.getWorld().getName(), vec(e.getLocation()),
                tracked && e.isValid() && !e.isDead(), spectator,
                tracked ? stats.current(id) : 0.0, tracked ? stats.max(id) : 0.0);
    }

    private HealingPulses.Kind kindOf(LivingEntity e) {
        if (e instanceof Player) return HealingPulses.Kind.PLAYER;
        if (e instanceof ArmorStand) return HealingPulses.Kind.ARMOR_STAND;
        if (CombatSides.of(e, keys) == MobHostility.Side.SUMMON) return HealingPulses.Kind.SUMMON;
        return HealingPulses.Kind.MOB;
    }

    private static Vec3 vec(Location l) {
        return new Vec3(l.getX(), l.getY(), l.getZ());
    }
}
