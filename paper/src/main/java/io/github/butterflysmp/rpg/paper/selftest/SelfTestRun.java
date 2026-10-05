package io.github.butterflysmp.rpg.paper.selftest;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.paper.health.MobNameplateManager;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.bukkit.Bukkit;
import org.bukkit.World;
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
        advance();
    }

    private void advance() {
        if (ended) return;
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
                    previousRow = "";
                    nextScenario();
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

    private void rowDone() {
        if (ended) return;
        drainReplies();
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
