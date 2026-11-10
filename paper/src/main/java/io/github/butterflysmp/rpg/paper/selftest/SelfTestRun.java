package io.github.butterflysmp.rpg.paper.selftest;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.paper.health.MobNameplateManager;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * One run: a queue of scenarios, walked step by step on THE PLAYER'S OWN SCHEDULER.
 *
 * <p><b>Every wait is a real wait</b>: a {@code wait: n} step ends this pass, and {@code onEntityLater(player, …, n)}
 * resumes it n ticks later. Nothing sleeps, nothing runs async, and every step runs on the thread that owns the player,
 * so the run is Folia-shaped as written. The reconcile (5 t), a full emberblade charge (~12.5 t) and a DOT period
 * (40 t) are waited for, not assumed.
 *
 * <p><b>Lines.</b> Each row is bracketed {@code START … DONE} (or {@code SKIPPED <why>}); between them sit the
 * instrument's own lines (CMD, DRIVE, SLOT, PROBE, REPLY) and the real witness lines the row caused. A DOTTICK that
 * arrives after DONE belongs to no row, so a DOT row's YAML waits out its window before it ends.
 *
 * <p><b>It ends exactly once</b>, by finishing, by {@code /rpg selftest stop}, by the player quitting, dying or leaving
 * the dev world, or by an exception, and every one of those paths runs the same restore.
 */
final class SelfTestRun {

    private final SelfTestService service;
    private final Player player;
    private final List<Scenario> queue;
    private final SelfTestDrivers drivers;
    private final Queue<ReplyReader.Reply> replies = new ConcurrentLinkedQueue<>();
    private final World world;
    private SelfTestSnapshot snapshot;
    private PacketListenerCommon reader;

    private int scenarioIndex = -1;
    private int stepIndex;
    private long rowStartTick;
    private String previousRow = "";
    private String previousGate;
    private final SelfTestStaging.RowRules<GameRule<Boolean>> rowRules = new SelfTestStaging.RowRules<>();
    private boolean deathExpected;
    private boolean ended;

    SelfTestRun(SelfTestService service, Player player, List<Scenario> queue) {
        this.service = service;
        this.player = player;
        this.queue = List.copyOf(queue);
        this.world = player.getWorld();
        this.drivers = new SelfTestDrivers(this, service.keys());
    }

    void start(String what) {
        snapshot = SelfTestSnapshot.take(player, service.profiles(), service.resources(), service.nameplates());
        long rows = queue.stream().filter(s -> !s.row().equals(Scenario.SETUP)).count();
        service.log("SELFTEST RUN START " + what + " rows=" + rows + " selftest=" + service.build()
                + " player=" + player.getUniqueId() + (snapshot.profileLoaded() ? "" : " PROFILE-NOT-LOADED"));
        // One tick later, so the command's own "Selftest started" reply goes out before the reader exists.
        service.scheduler().onEntityLater(player, () -> {
            if (ended) return;
            reader = PacketEvents.getAPI().getEventManager().registerListener(new ReplyReader(player.getUniqueId(), replies));
            nextScenario();
        }, 1);
    }

    // --- the walk ------------------------------------------------------------------------------------------------

    private void nextScenario() {
        scenarioIndex++;
        if (scenarioIndex >= queue.size()) {
            end("FINISHED");
            return;
        }
        Scenario s = current();
        stepIndex = 0;
        rowStartTick = Bukkit.getCurrentTick();
        log("START scenario=" + s.source() + " selftest=" + service.build() + " player=" + player.getUniqueId());
        boolean gateBoundary = SelfTestStaging.gateChanged(previousGate, s.gate());
        previousGate = s.gate();
        // A chained row reads what the row before it left (its blocks, its mobs, its held item): leave it all.
        if (SelfTestStaging.chained(s)) {
            advance();
            return;
        }
        // THE ROW-START CLEAN (SelfTestStaging; the 61e722f2 boot's staging leaks). The last chain's blocks go back,
        // the inventory goes back at a gate boundary, every tagged entity goes, and the player returns to where the
        // run began, so no row stages from the floor or the hotbar or the mobs the last one left. THE TELEPORT GOES
        // FIRST: a player still standing in a pit the last row dug would otherwise be inside the restored blocks for
        // a tick, and suffocation is damage the run's restore does not undo. The start was air when the run began, so
        // it is air again once its blocks are back.
        Location start = snapshot.start();
        player.teleportAsync(start).thenRun(() -> service.scheduler().onEntity(player, () -> {
            if (ended) return;
            int blocks = snapshot.restoreRowBlocks();
            if (gateBoundary) snapshot.restoreInventory(player);
            int removed = snapshot.removeTagged(player, service.scheduler());
            log("CLEAN blocks=" + blocks + " removed=" + removed + " inventory=" + (gateBoundary ? "restored" : "kept")
                    + " return=" + String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", start.getX(), start.getY(), start.getZ()));
            advance();
        }));
    }

    private void advance() {
        if (ended) return;
        // A death_rules row may lose its player (SU8): wait for the respawn instead of aborting.
        if (SelfTestStaging.waitForRespawn(deathExpected, player.isDead()) && player.isOnline()) {
            awaitRespawn(player.getLocation(), 0);
            return;
        }
        Optional<String> invalid = invalidated();
        if (invalid.isPresent()) {
            end("ABORTED " + invalid.get());
            return;
        }
        drainReplies();
        Scenario s = current();
        try {
            while (stepIndex < s.steps().size()) {
                Scenario.Step step = s.steps().get(stepIndex++);
                if (step.verb() == Scenario.Verb.WAIT) {
                    service.scheduler().onEntityLater(player, this::advance, Long.parseLong(step.arg().trim()));
                    return;
                }
                Optional<String> skip = drivers.execute(player, step);
                drainReplies();
                if (skip.isPresent()) {
                    log("SKIPPED " + skip.get());
                    restoreRowRules();
                    previousRow = "";
                    nextScenario();
                    return;
                }
                // A kill leaves a body in its death animation for about a second, and the next aim or look-ray can
                // land on it (WS2c's control hit the dying wither skeleton). Wait until none is left.
                if (SelfTestStaging.isKill(step)) {
                    settle(0);
                    return;
                }
            }
        } catch (RuntimeException e) {
            log("ABORTED " + e);
            end("ABORTED in " + s.source() + ": " + e);
            return;
        }
        // Two ticks for the last step's replies to come back through the reader, then DONE.
        service.scheduler().onEntityLater(player, this::rowDone, 2);
    }

    /** Polls once a tick until no dying body is near the player, or {@link SelfTestStaging#SETTLE_CAP_TICKS} pass. */
    private void settle(int waited) {
        if (ended) return;
        int dying = 0;
        for (Entity e : player.getNearbyEntities(48, 48, 48)) {
            if (SelfTestStaging.dying(e instanceof LivingEntity, e instanceof LivingEntity l && l.isDead(), e.isValid())) dying++;
        }
        if (dying == 0 || waited >= SelfTestStaging.SETTLE_CAP_TICKS) {
            log("SETTLED dying=" + dying + " ticks=" + waited);
            advance();
            return;
        }
        service.scheduler().onEntityLater(player, () -> settle(waited + 1), 1);
    }

    /**
     * Polls for the respawn on the REGION scheduler, never the player's own. Paper retires an entity's tasks across a
     * death (`PaperScheduler.onEntityLater` passes no retired callback, so a retired task simply never runs), and run
     * 2 of the {@code 61e722f2} boot shows it: after Ben drowned in SU11's 240-tick wait the row logged nothing for 78
     * seconds, until he stopped it. Back on the player's scheduler only once he is alive again.
     */
    private void awaitRespawn(Location where, int waited) {
        if (ended) return;
        if (!player.isOnline()) {
            end("ABORTED the player left");
            return;
        }
        if (!player.isDead()) {
            log("RESPAWNED ticks=" + waited);
            service.scheduler().onEntity(player, this::advance);
            return;
        }
        if (waited >= SelfTestStaging.RESPAWN_CAP_TICKS) {
            end("ABORTED the player did not respawn in " + waited + " ticks");
            return;
        }
        service.scheduler().onRegionLater(where, () -> awaitRespawn(where, waited + 1), 1);
    }

    /** {@code death_rules}: this row only, keep_inventory and immediate_respawn on, and the row may lose its player. */
    void deathRules() {
        for (GameRule<Boolean> rule : List.of(GameRules.KEEP_INVENTORY, GameRules.IMMEDIATE_RESPAWN)) {
            rowRules.record(rule, world.getGameRuleValue(rule));
        }
        deathExpected = true;
        log("DEATH_RULES keep_inventory=" + world.getGameRuleValue(GameRules.KEEP_INVENTORY)
                + "->true immediate_respawn=" + world.getGameRuleValue(GameRules.IMMEDIATE_RESPAWN) + "->true");
        service.scheduler().onGlobal(() -> {
            world.setGameRule(GameRules.KEEP_INVENTORY, true);
            world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
        });
    }

    /** Puts back what a death_rules row changed, at its end, however it ends. */
    private void restoreRowRules() {
        deathExpected = false;
        if (!rowRules.active()) return;
        Map<GameRule<Boolean>, Boolean> back = rowRules.drain();
        StringBuilder line = new StringBuilder("RULES restored");
        for (var e : back.entrySet()) line.append(' ').append(e.getKey().getKey().getKey()).append('=').append(e.getValue());
        log(line.toString());
        service.scheduler().onGlobal(() -> back.forEach((rule, value) -> {
            if (value != null) world.setGameRule(rule, value);
        }));
    }

    private void rowDone() {
        if (ended) return;
        drainReplies();
        restoreRowRules();
        log("DONE ticks=" + (Bukkit.getCurrentTick() - rowStartTick));
        previousRow = current().row();
        nextScenario();
    }

    /** Why the run may not continue, re-checked before every pass: the guard's conditions can change mid-run. */
    private Optional<String> invalidated() {
        if (!player.isOnline()) return Optional.of("the player left");
        if (player.isDead()) return Optional.of("the player died");
        if (!player.getWorld().equals(world)) return Optional.of("the player left the dev world");
        int online = Bukkit.getOnlinePlayers().size();
        if (online != 1) return Optional.of(online + " players are online");
        return Optional.empty();
    }

    /** Stop, quit and finish all come here. Idempotent: the second caller finds it ended. */
    void end(String why) {
        if (ended) return;
        ended = true;
        drainReplies();
        if (reader != null) PacketEvents.getAPI().getEventManager().unregisterListener(reader);
        restoreRowRules();   // a row that ended mid-way; the snapshot below puts the RUN START values back too
        String restored = snapshot.restore(player, service.profiles(), service.resources(), service.nameplates(),
                service.scheduler());
        service.log("SELFTEST RUN " + why);
        service.log("SELFTEST RUN " + restored);
        service.ended(this);
    }

    private void drainReplies() {
        ReplyReader.Reply reply;
        while ((reply = replies.poll()) != null) {
            log("REPLY" + (reply.overlay() ? " actionbar " : " ") + SelfTestLines.styled(reply.message()));
        }
    }

    // --- what the drivers read -------------------------------------------------------------------------------------

    private Scenario current() {
        return queue.get(scenarioIndex);
    }

    void log(String text) {
        Scenario s = scenarioIndex >= 0 && scenarioIndex < queue.size() ? current() : null;
        service.log(s == null ? "SELFTEST " + text : SelfTestLines.row(s.gate(), s.row(), text));
    }

    String previousRow() { return previousRow; }
    Player player() { return player; }
    SelfTestSnapshot snapshot() { return snapshot; }
    MobNameplateManager nameplates() { return service.nameplates(); }
    ProfileService profiles() { return service.profiles(); }
    CombatantStats stats() { return service.stats(); }
}
