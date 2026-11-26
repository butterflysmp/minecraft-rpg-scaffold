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
 * so the run is Folia-shaped as written. The reconcile (5 t), a full emberblade (deleted 2026-11) charge (~12.5 t) and a DOT period
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
    /** The dying count at the current settle's first poll (ST9's {@code first=}). */
    private int settleFirst;
    private boolean ended;

    SelfTestRun(SelfTestService service, Player player, List<Scenario> queue) {
        this.service = service;
        this.player = player;
        this.queue = List.copyOf(queue);
        this.world = player.getWorld();
        this.drivers = new SelfTestDrivers(this, service.keys(), service.mobElements());
        this.padTickets = SelfTestPad.tickets(world, service.padChunks(world));
    }

    /**
     * P10 (the seat's rulings of 2026-11-24): the pad's chunk tickets, taken at the PAD step and held for the whole run;
     * released in {@link #end(String, io.github.butterflysmp.rpg.paper.scheduler.Scheduler, boolean, String)}, the one
     * funnel every exit passes (FINISHED, every abort including the pad's own failure, stop, quit, disable), once.
     */
    private final SelfTestPadTickets padTickets;

    void start(String what) {
        snapshot = SelfTestSnapshot.take(player, service.profiles(), service.resources(), service.nameplates(),
                service.stats());
        long rows = queue.stream().filter(s -> !s.row().equals(Scenario.SETUP)).count();
        service.log("SELFTEST RUN START " + what + " rows=" + rows + " selftest=" + service.build()
                + " player=" + player.getUniqueId() + (snapshot.profileLoaded() ? "" : " PROFILE-NOT-LOADED"));
        // W1 (the seat, 2026-11-25): what he WEARS at capture, so the RESTORED line can be read against it.
        service.log("SELFTEST RUN CAPTURED " + snapshot.capturedFields());
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
        watchingSwings = false;   // a row watches swings only when it says so (step R7a)
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
        Location start = stage();
        player.teleportAsync(start).thenRun(() -> service.scheduler().onEntity(player, () -> {
            if (ended) return;
            SelfTestSnapshot.RowRestore rowBlocks = snapshot.restoreRowBlocks();
            int blocks = rowBlocks.restored();
            String worn = gateBoundary ? snapshot.restoreInventory(player) : null;   // W1: his five slots, read back
            int removed = snapshot.removeTagged(player, service.scheduler());
            // HIS SUMMONS GO TOO (2026-11-26, after SM16R2's wolf lived into the next gate's first row): a summon carries no
            // selftest tag, so removeTagged never takes one. Cause CLEAR, as clear_state's; each logs its SUMMON removed line.
            int summons = io.github.butterflysmp.rpg.paper.summon.SummonRemovals.removeAllOf(service.adapters(),
                    player.getUniqueId(), io.github.butterflysmp.rpg.core.summon.SummonRemoval.CLEAR);
            // AND HIS FUSED CHUNKS (the seat's F2, 2026-11-26): a chunk left from the last row never goes off in this one.
            int fused = io.github.butterflysmp.rpg.paper.weapon.FusedRemovals.removeAllOf(service.adapters(), player.getUniqueId(), "selftest");
            // AND HIS DRUMMING BEAT STREAK (the Bone Club's M5, 2026-11-26): no row inherits the last row's n. Logged as the
            // n that was forgotten.
            var drumStreaks = service.adapters().behaviourHits().drums();
            int drum = drumStreaks.of(player.getUniqueId()).n();
            drumStreaks.forget(player.getUniqueId());
            log("CLEAN blocks=" + blocks + " " + SelfTestStaging.readBackField(rowBlocks.readBack(), blocks) + " removed=" + removed + " summons=" + summons + " fused=" + fused + " inventory=" + (gateBoundary ? "restored" : "kept")
                    + (worn == null ? "" : " " + worn)
                    + " return=" + String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", start.getX(), start.getY(), start.getZ())
                    + " drum=" + drum);
            log(SelfTestStaging.lingerLine(SelfTestStaging.LINGER_RADIUS, endWork(start), endLingering(start)));
            // A ROW STARTS CLEAN OR SAYS SO (GATE-dragon-fix.md, C3). The removals above run on each mob's own
            // scheduler, a tick later, so the count waits for them; then every live body still in the row's radius
            // is named. The 2026-11-15 boot's rows staged beside mobs earlier rows had leaked, and no line said so.
            service.scheduler().onEntityLater(player, () -> {
                if (ended) return;
                log(SelfTestStaging.leftoverLine(SelfTestStaging.LEFTOVER_RADIUS, leftovers()));
                advance();
            }, SelfTestStaging.LEFTOVER_WAIT_TICKS);
        }));
    }

    /** C2 (step R7b), FIRST: end every point-scheduled chain round the start, so none re-spawns what the next pass removes. */
    private int endWork(Location start) {
        return service.work().cancelNear(start.getWorld().getUID(), start.getX(), start.getY(), start.getZ(),
                SelfTestStaging.LINGER_RADIUS);
    }

    /** C2 (step R7b), THEN: remove each projectile, cloud and marker of ours round the start; "type uuid", nearest first. */
    private List<String> endLingering(Location start) {
        int r = SelfTestStaging.LINGER_RADIUS;
        List<String> ended = new java.util.ArrayList<>();
        start.getWorld().getNearbyEntities(start, r, r, r).stream()
                .filter(e -> e.getLocation().distanceSquared(start) <= (double) r * r)
                .filter(e -> SelfTestStaging.lingering(e instanceof org.bukkit.entity.Projectile,
                        e instanceof org.bukkit.entity.AreaEffectCloud,
                        e.getPersistentDataContainer().has(service.keys().markerEntity), e instanceof Player))
                .sorted(java.util.Comparator.comparingDouble(e -> e.getLocation().distanceSquared(start)))
                .forEach(e -> {
                    ended.add(e.getType().getKey().getKey() + " " + e.getUniqueId());
                    service.scheduler().onEntity(e, e::remove);
                });
        return ended;
    }

    /** Every live body in the row's radius that is not the player or scenery: "type uuid", nearest first. */
    private List<String> leftovers() {
        int r = SelfTestStaging.LEFTOVER_RADIUS;
        return player.getNearbyEntities(r, r, r).stream()
                .filter(e -> e.getLocation().distanceSquared(player.getLocation()) <= (double) r * r)
                // F9e a: a fight's copy is not a leftover mob (the fight removes it at its end).
                .filter(e -> !io.github.butterflysmp.rpg.paper.fight.FightCopies.isCopy(e))
                .filter(e -> SelfTestStaging.leftover(e instanceof LivingEntity, e instanceof Player,
                        e instanceof org.bukkit.entity.ArmorStand, e instanceof LivingEntity l && l.isDead()))
                .sorted(java.util.Comparator.comparingDouble(e -> e.getLocation().distanceSquared(player.getLocation())))
                .map(e -> e.getType().getKey().getKey() + " " + e.getUniqueId())
                .toList();
    }

    // --- THE PAD (Ben's ruling A, 2026-11-23; the seat's P5) ---------------------------------------------------------

    /** The pad's stand point once a PAD step has moved him there; null before. Touched only on the player's thread. */
    private Location stagePoint;

    /** Where every unchained row stages from: the pad's stand point once he is on it, else where the run began. */
    Location stage() {
        return stagePoint != null ? stagePoint.clone() : snapshot.start();
    }

    /**
     * PAD: build or check the pad (its chunks loaded, then its region), and only on success teleport him to its stand
     * point, facing east. The snapshot was taken at RUN START, before this, so every end path returns him to where he
     * typed the command. Any failure: no teleport, one plain chat line, the run ends.
     */
    private void pad() {
        SelfTestPad.ensure(world, service.scheduler(), padTickets).whenComplete((result, err) -> service.scheduler().onEntity(player, () -> {
            if (ended) return;
            SelfTestPad.Result r = err != null ? new SelfTestPad.Result(false, null, err.toString(), null) : result;
            if (r.ticketsLine() != null) log(r.ticketsLine());   // P10(d): "PAD tickets held=12", once, at the twelfth
            if (r.line() != null) log(r.line());
            if (!r.ok()) {
                padFailed(r.why());
                return;
            }
            Location stand = r.stand();
            player.teleportAsync(stand).whenComplete((moved, terr) -> service.scheduler().onEntity(player, () -> {
                if (ended) return;
                if (terr != null || !Boolean.TRUE.equals(moved)) {
                    padFailed("the teleport onto it was refused");
                    return;
                }
                stagePoint = stand.clone();
                log("PAD stood at=" + String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f yaw=%.3f pitch=%.3f",
                        stand.getX(), stand.getY(), stand.getZ(), stand.getYaw(), stand.getPitch()));
                advance();
            }));
        }));
    }

    private void padFailed(String why) {
        log("PAD fail " + why);
        player.sendMessage(net.kyori.adventure.text.Component.text("The selftest could not set up its platform in the sky ("
                + why + "), so nothing ran and you were not moved.", net.kyori.adventure.text.format.NamedTextColor.RED));
        end("ABORTED pad: " + why);
    }

    // --- ASK: a question in chat, the next chat line logged verbatim (GATE-dragon-fix.md, C6) ----------------------

    /** The question waiting for an answer, or null. Touched only on the player's thread. */
    private SelfTestAsk.Ask pendingAsk;
    /** Which question a timeout belongs to, so a late timeout never resolves a later question. */
    private int askSerial;
    private long askStartTick;

    private void ask(Scenario.Step step) {
        SelfTestAsk.Ask a = SelfTestAsk.parse(step.arg()).orElseThrow(
                () -> new IllegalStateException("ask: the loader admitted '" + step.arg() + "'"));
        pendingAsk = a;
        int serial = ++askSerial;
        askStartTick = Bukkit.getCurrentTick();
        player.sendMessage(net.kyori.adventure.text.Component.text(SelfTestAsk.prompt(a),
                net.kyori.adventure.text.format.NamedTextColor.GOLD));
        service.scheduler().onEntityLater(player, () -> {
            if (ended || pendingAsk == null || serial != askSerial) return;
            log(SelfTestAsk.notStated(pendingAsk));
            pendingAsk = null;
            advance();
        }, a.timeoutTicks());
    }

    /** The player's chat line, on the player's thread (SelfTestService#onChat hops it here). Ignored unless asked. */
    void answer(String line) {
        if (ended || pendingAsk == null) return;
        log(SelfTestAsk.answered(pendingAsk, line, Bukkit.getCurrentTick() - askStartTick));
        pendingAsk = null;
        askSerial++;   // the pending timeout now belongs to no question
        advance();
    }

    private void advance() {
        if (ended) return;
        // A death_rules row may lose its player (SU8): wait for the respawn instead of aborting.
        if (SelfTestStaging.waitForRespawn(deathExpected, player.isDead()) && player.isOnline()) {
            // Polled at the run's start, a world that stays loaded: a death in a fight world is in a world that may be
            // unloading by the next tick (the seat's ruling 2b; awaitFight does the same).
            awaitRespawn(snapshot.start(), 0);
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
                if (step.verb() == Scenario.Verb.ASK) {
                    ask(step);   // resumes from answer() or the timeout
                    return;
                }
                if (step.verb() == Scenario.Verb.PAD) {
                    pad();   // resumes from the teleport's completion, or ends the run
                    return;
                }
                if (step.verb() == Scenario.Verb.PREFLIGHT) {
                    // THE PREFLIGHT (the seat's brief B): a bad spot runs NO row. He is told what is wrong and where,
                    // the run ends here and the snapshot restores at once; a pass logs its counts and goes on.
                    SelfTestPreflight.Tally t = drivers.preflight(player, SelfTestPreflight.parse(step.arg()).orElseThrow(
                            () -> new IllegalStateException("preflight: the loader admitted '" + step.arg() + "'")));
                    log(t.logLine());
                    if (!t.ok()) {
                        List<String> tell = t.tell();
                        player.sendMessage(net.kyori.adventure.text.Component.text(tell.get(0),
                                net.kyori.adventure.text.format.NamedTextColor.RED));
                        player.sendMessage(net.kyori.adventure.text.Component.text(tell.get(1),
                                net.kyori.adventure.text.format.NamedTextColor.GOLD));
                        end("ABORTED preflight: the ground failed, no row ran");
                        return;
                    }
                    continue;
                }
                if (step.verb() == Scenario.Verb.STRIKE_AT_AGE) {
                    strikeAtAge(SelfTestStrike.parseAtAge(step.arg()).orElseThrow(
                            () -> new IllegalStateException("strike_at_age: the loader admitted '" + step.arg() + "'")), 0);
                    return;   // resumes when the shot reaches the age, or at the timeout
                }
                if (step.verb() == Scenario.Verb.AWAIT_FUSE) {
                    awaitFuse(SelfTestBlast.parseAwait(step.arg()).orElseThrow(
                            () -> new IllegalStateException("await_fuse: the loader admitted '" + step.arg() + "'")), 0);
                    return;   // resumes when the swell reaches the threshold, or at the timeout
                }
                if (step.verb() == Scenario.Verb.AWAIT_FIGHT) {
                    awaitFight(SelfTestAwaitFight.parse(step.arg()).orElseThrow(
                            () -> new IllegalStateException("await_fight: the loader admitted '" + step.arg() + "'")), 0);
                    return;   // resumes when the fight ends, or at the ceiling
                }
                Optional<String> skip = drivers.execute(player, step);
                drainReplies();
                if (skip.isPresent()) {
                    log("SKIPPED " + skip.get());
                    // G4 (the seat's ruling, 2026-11-24): a SETUP that fails ends the WHOLE run, the state after it is
                    // unknown; a row's skip moves on, as before.
                    if (SelfTestGateList.skipEndsRun(current())) {
                        end(SelfTestGateList.setupAbort(current().gate(), skip.get()));
                        return;
                    }
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
        // Two ticks for the last step's replies to come back through the reader, then a drain, one more tick, a drain, DONE.
        service.scheduler().onEntityLater(player, this::rowDone, SelfTestStaging.ROW_END_TICKS);
    }

    /**
     * AWAIT_FIGHT ({@link SelfTestAwaitFight}): polls once a tick until the dev command's fight is no longer running,
     * or the ceiling; logs which, with the ticks waited, and goes on.
     *
     * <p><b>On the GLOBAL scheduler, never the player's own:</b> in the arena he may die while this waits (a death is
     * the fight's failure), and a death retires his own tasks, so a poll there would simply stop ({@link #awaitRespawn}'s
     * note). A player found dead when the wait ends goes to the respawn wait; a live one goes on, on his own thread.
     */
    private void awaitFight(int ceiling, int waited) {
        if (ended) return;
        boolean running = service.fightRunning();
        if (SelfTestAwaitFight.done(running, waited, ceiling)) {
            log(SelfTestAwaitFight.line(running, waited, ceiling));
            if (player.isDead() && player.isOnline()) {
                // Polled at the run's start, a world that stays loaded: he died in a fight world that may be unloading.
                awaitRespawn(snapshot.start(), 0);
            } else {
                service.scheduler().onEntity(player, this::advance);
            }
            return;
        }
        service.scheduler().onGlobalLater(() -> awaitFight(ceiling, waited + 1), 1);
    }

    /**
     * Polls once a tick until no dying body is near the player, or {@link SelfTestStaging#SETTLE_CAP_TICKS} pass. The
     * SETTLED line's {@code first=} is the count at the first poll, so a reader sees the wait had something to wait for.
     */
    private void settle(int waited) {
        if (ended) return;
        int dying = 0;
        for (Entity e : player.getNearbyEntities(48, 48, 48)) {
            if (SelfTestStaging.dying(e instanceof LivingEntity, e instanceof LivingEntity l && l.isDead())) dying++;
        }
        if (waited == 0) settleFirst = dying;
        if (dying == 0 || waited >= SelfTestStaging.SETTLE_CAP_TICKS) {
            log("SETTLED dying=" + dying + " ticks=" + waited + " first=" + settleFirst);
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
    /**
     * AWAIT_FUSE (dragon-mobs step R3; {@link SelfTestBlast}): poll the nearest creeper carrying the tag once a tick
     * until its swell reaches the threshold, then log its FUSE line with the ticks waited. The swell rises by one a
     * tick ({@code Creeper.tick}), so the first reading at or past the threshold is the threshold itself unless a
     * tick was skipped. No such creeper, or the timeout, is logged by name and the row goes on.
     */
    // --- the Phalanx's shot: a real swing, watched (the seat's ruling 4 on #201) -------------------------------------

    /** Set by the WATCH_SWINGS step; reset at every row's start. Touched only on the player's thread. */
    private boolean watchingSwings;

    void watchSwings(boolean on) {
        watchingSwings = on;
    }

    /**
     * One of the player's own swings, attacks or interacts (RpgListeners), while a row watches: the nearest live shot,
     * the distance along HIS LOOK into its server box, or a miss and the gap, out to his interaction range; and one tick
     * later whether that shot is still live.
     */
    void watchSwing(String kind) {
        if (ended || !watchingSwings) return;
        Location eye = player.getEyeLocation();
        var shots = service.shots().liveShots(eye);
        if (shots.isEmpty()) {
            log(SelfTestStrike.swingLine(kind, null, java.util.OptionalDouble.empty(), 0, null, null));
            return;
        }
        var s = shots.getFirst();
        var range = player.getAttribute(org.bukkit.attribute.Attribute.ENTITY_INTERACTION_RANGE);
        double reach = range == null ? 3.0 : range.getValue();
        io.github.butterflysmp.rpg.core.Vec3 e = new io.github.butterflysmp.rpg.core.Vec3(eye.getX(), eye.getY(), eye.getZ());
        org.bukkit.util.Vector d = eye.getDirection();
        io.github.butterflysmp.rpg.core.Vec3 look = new io.github.butterflysmp.rpg.core.Vec3(d.getX(), d.getY(), d.getZ());
        log(SelfTestStrike.swingLine(kind, s.id(), SelfTestStrike.along(e, look, reach, s.box()),
                SelfTestStrike.gap(e, look, reach, s.box()), e, look));
        service.scheduler().onEntityLater(player, () -> {
            if (!ended) log(SelfTestStrike.afterLine(s.id(), service.shots().isLive(s.id())));
        }, 1);
    }

    /**
     * STRIKE_AT_AGE (the seat's ruling 3 on #201): polled each tick on the player's thread until the tagged shot's age
     * reaches the given age, then the strike in that same tick. Whichever of this task and the Phalanx's loop runs first
     * in a tick, the strike lands before the shot's NEXT step, so an age N strike reads the shot at age N.
     */
    private void strikeAtAge(SelfTestStrike.AtAge a, int waited) {
        if (ended) return;
        Entity carrier = player.getNearbyEntities(48, 48, 48).stream()
                .filter(x -> x.getScoreboardTags().contains(a.tag()))
                .min(java.util.Comparator.comparingDouble(x -> x.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);
        var live = carrier == null ? null : service.shots().liveShots(player.getEyeLocation()).stream()
                .filter(s -> s.id().equals(carrier.getUniqueId())).findFirst().orElse(null);
        SelfTestStrike.Next next = SelfTestStrike.next(live != null, live == null ? 0 : live.age(), a.age(), waited,
                a.timeoutTicks());
        if (next == SelfTestStrike.Next.NO_SHOT_AT_TIMEOUT) {
            log("DRIVE strike_at_age tag=" + a.tag() + " no live shot after " + waited + " ticks (the timeout)");
            advance();
            return;
        }
        if (next == SelfTestStrike.Next.STRIKE) {
            var c = io.github.butterflysmp.rpg.paper.health.MobShooter.shapeCentre(live.carrier()).orElse(live.centre());
            Location eye = player.getEyeLocation();
            log("DRIVE strike_at_age tag=" + a.tag() + " age=" + live.age() + " waited=" + waited);
            drivers.strikeAlong(player, a.tag(), "strike_at_age",
                    new org.bukkit.util.Vector(c.x() - eye.getX(), c.y() - eye.getY(), c.z() - eye.getZ()));
            advance();
            return;
        }
        if (next == SelfTestStrike.Next.YOUNG_AT_TIMEOUT) {
            log("DRIVE strike_at_age tag=" + a.tag() + " age=" + live.age() + " under " + a.age() + " at the timeout");
            advance();
            return;
        }
        service.scheduler().onEntityLater(player, () -> strikeAtAge(a, waited + 1), 1);
    }

    private void awaitFuse(SelfTestBlast.Await a, int waited) {
        if (ended) return;
        org.bukkit.entity.Creeper c = player.getNearbyEntities(48, 48, 48).stream()
                .filter(e -> e instanceof org.bukkit.entity.Creeper && e.getScoreboardTags().contains(a.tag()))
                .map(e -> (org.bukkit.entity.Creeper) e)
                .min(java.util.Comparator.comparingDouble(e -> e.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);
        if (c == null) {
            log("FUSE tag=" + a.tag() + " none within 48 blocks after " + waited + " ticks");
            advance();
            return;
        }
        java.util.Optional<int[]> fuse = fuser().state(c.getUniqueId());
        int swell = fuse.map(s -> s[0]).orElse(0);
        if (swell >= a.minFuse()) {
            log(SelfTestBlast.fuseLine(a.tag(), c.getUniqueId(), fuse, c.isIgnited())
                    + " waited=" + waited);
            advance();
            return;
        }
        if (waited >= a.timeoutTicks()) {
            log(SelfTestBlast.fuseLine(a.tag(), c.getUniqueId(), fuse, c.isIgnited())
                    + " waited=" + waited + " under " + a.minFuse() + " at the timeout");
            advance();
            return;
        }
        service.scheduler().onEntityLater(player, () -> awaitFuse(a, waited + 1), 1);
    }

    /**
     * THE PLAYER'S DAMAGE LINE (dragon-mobs step R3; {@link SelfTestBlast#hurtLine}), from the store's own change,
     * so the amount before defence and the amount that landed are both the store's. Called on the player's thread,
     * inside the hit. The dealer's type is looked up NOW: a dealer already removed (a Phage that blew) reads
     * {@code gone}, and the hit's own uuid is still printed.
     */
    void hurt(io.github.butterflysmp.rpg.core.combat.stat.HealthChange change) {
        if (ended || scenarioIndex < 0 || !change.target().equals(player.getUniqueId())) return;
        String dealer;
        String type;
        if (change.dealer() == null) {
            dealer = "none";
            type = "none";
        } else if (change.dealer().equals(player.getUniqueId())) {
            dealer = "self";
            type = "self";
        } else {
            dealer = change.dealer().toString();
            Entity e = player.getWorld().getEntity(change.dealer());
            String mobId = e == null ? null
                    : e.getPersistentDataContainer().get(service.keys().mobId, org.bukkit.persistence.PersistentDataType.STRING);
            type = e == null ? "gone" : e.getType().getKey().getKey() + (mobId == null ? "" : "/" + mobId);
        }
        log(SelfTestBlast.hurtLine(change.beforeDefense(), change.amount(), service.stats().defenseValue(player.getUniqueId()),
                dealer, type, service.attackNameFor(player.getUniqueId(), change.dealer()),
                service.hurtCauseFor(player.getUniqueId())));
    }

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
        restoreRowRules(service.scheduler());
    }

    private void restoreRowRules(io.github.butterflysmp.rpg.paper.scheduler.Scheduler scheduler) {
        deathExpected = false;
        if (!rowRules.active()) return;
        Map<GameRule<Boolean>, Boolean> back = rowRules.drain();
        StringBuilder line = new StringBuilder("RULES restored");
        for (var e : back.entrySet()) line.append(' ').append(e.getKey().getKey().getKey()).append('=').append(e.getValue());
        log(line.toString());
        scheduler.onGlobal(() -> back.forEach((rule, value) -> {
            if (value != null) world.setGameRule(rule, value);
        }));
    }

    /**
     * P-R2 (the seat's ruling on #308, 2026-11-25): the row ends with a DRAIN, ONE TICK, and a second DRAIN before DONE.
     * Paper sends a reply from the channel's event loop after the command returns, so a reply to the row's last step can
     * reach the reader after the first drain; the tick and the second drain log it under THIS row, never the next one's.
     * It narrows the window; it cannot close it (the send is on another thread). Its one caller waits {@link SelfTestStaging#ROW_END_TICKS} first.
     */
    private void rowDone() {
        if (ended) return;
        drainReplies();
        service.scheduler().onEntityLater(player, this::rowDoneDrained, SelfTestStaging.ROW_END_REDRAIN_TICKS);
    }

    private void rowDoneDrained() {
        if (ended) return;
        drainReplies();
        restoreRowRules();
        log("DONE ticks=" + (Bukkit.getCurrentTick() - rowStartTick));
        previousRow = current().row();
        nextScenario();
    }

    /** Why the run may not continue, re-checked before every pass: the guard's conditions can change mid-run. */
    private Optional<String> invalidated() {
        // The rule is SelfTestStaging.invalid's, pure and walked by SelfTestArenaGuardTest: a fight's own world counts
        // as the run's (GATE-fight-arena.md, the seat's ruling 2a); any other world still aborts.
        return SelfTestStaging.invalid(player.isOnline(), player.isDead(), player.getWorld().equals(world),
                io.github.butterflysmp.rpg.paper.fight.FightWorlds.isFightWorld(player.getWorld()),
                Bukkit.getOnlinePlayers().size());
    }

    /** Stop, quit and finish all come here. Idempotent: the second caller finds it ended. */
    void end(String why) {
        end(why, service.scheduler(), false, SelfTestPadTickets.causeOf(why));
    }

    /**
     * HIS QUIT MID-RUN (2026-11-23, the pad's P6). The restore's return is a SYNCHRONOUS teleport here, as at a server
     * stop. READ from the server jar (build 74): {@code CraftEntity#teleportAsync} loads the destination's chunks and
     * teleports in a callback, and {@code CraftPlayer#teleport0} returns false once his {@code connection} is null. On a
     * quit his save follows this event, so an async return could land after the save, or never, and leave him saved on
     * the pad. The scheduler stays the live one: the plugin is not disabling.
     */
    void endAtQuit() {
        end("ABORTED the player left", service.scheduler(), true, "quit");
    }

    /**
     * A SERVER STOP MID-RUN (GATE-dragon-knell-4.md). The plugin is disabling: its scheduler refuses tasks, and the
     * server saves players and chunks right after (MinecraftServer#stopServer disables plugins first, read from the
     * jar). So the same restore runs SYNCHRONOUSLY, every task inline, and anything it asked for later or off-thread is
     * logged by name, never dropped silently. Its lines are the normal end's: ABORTED (reason=server-stop), RESTORED.
     */
    void endAtDisable() {
        java.util.List<String> refused = new java.util.ArrayList<>();
        io.github.butterflysmp.rpg.paper.scheduler.InlineScheduler inline =
                new io.github.butterflysmp.rpg.paper.scheduler.InlineScheduler(refused::add);
        end("ABORTED reason=server-stop", inline, true, "disable");
        for (String what : refused) service.log("SELFTEST RUN NOT-RESTORED at server-stop: a task asked for " + what);
    }

    private void end(String why, io.github.butterflysmp.rpg.paper.scheduler.Scheduler scheduler, boolean now,
                     String cause) {
        if (ended) return;
        ended = true;
        drainReplies();
        if (reader != null) PacketEvents.getAPI().getEventManager().unregisterListener(reader);
        restoreRowRules(scheduler);   // a row that ended mid-way; the snapshot below puts the RUN START values back too
        String restored = snapshot.restore(player, service.profiles(), service.resources(), service.nameplates(),
                scheduler, now);
        // X8 (the seat, 2026-11-25): NO SUMMON OUTLIVES A RUN. The restore above removes selftest-TAGGED mobs only, and a
        // summon is not tagged; so on every exit (END, ABORTED, quit, server-stop) his summons go here, cause CLEAR
        // (CC'S CALL: the clear_state cause; the alternative is a new END cause in the core's eleven). discard runs inline.
        restored += " summons=" + io.github.butterflysmp.rpg.paper.summon.SummonRemovals.removeAllOf(service.adapters(),
                player.getUniqueId(), io.github.butterflysmp.rpg.core.summon.SummonRemoval.CLEAR);
        restored += " fused=" + io.github.butterflysmp.rpg.paper.weapon.FusedRemovals.removeAllOf(service.adapters(), player.getUniqueId(), "selftest");   // F2: no chunk outlives a run
        // THE MIRROR (2026-11-22): the restore may have put back a mirror from RUN START; drawn again from what he holds
        // now, so no restore leaves arrows where his star should be. The quit and disable paths then put the star back.
        io.github.butterflysmp.rpg.paper.nexus.NexusMirror.redraw(player, service.adapters().keys());
        // HIS HEALTH, last and later (2026-11-22, the seat's brief, PR 1): after the XP restore above has had two
        // reconcile passes to raise his max. Logged as "SELFTEST RUN HEALTH ...", after the RESTORED line.
        snapshot.restoreHealthLater(player, service.stats(), scheduler, line -> service.log("SELFTEST RUN " + line));
        // The drag's per-tick instrument is a scenario step's, never the server's: off at every run's end (it starts off).
        service.dragTrace().set(false);
        restored += " drag_tick=" + service.dragTrace().on();
        // A fight fixture a row left running ends with the run (GATE-fight-ring.md): one line only when there was one.
        String fight = service.stopFight();
        service.log("SELFTEST RUN " + why);
        service.log("SELFTEST RUN " + restored);
        if (fight != null) service.log("SELFTEST RUN fight-fixture " + fight);
        // P10(b, d): the pad's tickets go LAST, after the restore, on every exit; exactly what was taken, once. On the
        // disable path the plugin is still enabled here (RpgPlugin.onDisable calls this FIRST), and removing a ticket
        // does not need the plugin enabled (READ: removePluginChunkTicket checks only for a null plugin).
        service.log("SELFTEST RUN " + SelfTestPadTickets.releasedLine(padTickets.release(), cause));
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
    io.github.butterflysmp.rpg.paper.mining.MiningService mining() { return service.mining(); }
    io.github.butterflysmp.rpg.paper.adapter.DragTrace dragTrace() { return service.dragTrace(); }
    ProfileService profiles() { return service.profiles(); }
    CombatantStats stats() { return service.stats(); }
    io.github.butterflysmp.rpg.core.combat.ResourcePool resources() { return service.resources(); }
    SelfTestService.Spawner spawner() { return service.spawner(); }
    io.github.butterflysmp.rpg.paper.scheduler.Scheduler scheduler() { return service.scheduler(); }
    io.github.butterflysmp.rpg.paper.health.MobBeamer beams() { return service.beams(); }
    io.github.butterflysmp.rpg.paper.health.MobTeleporter teleports() { return service.teleports(); }
    io.github.butterflysmp.rpg.paper.health.MobShooter shots() { return service.shots(); }
    io.github.butterflysmp.rpg.paper.health.MobPouncer pounces() { return service.pounces(); }
    io.github.butterflysmp.rpg.paper.health.PhageFuser fuser() { return service.fuser(); }
    io.github.butterflysmp.rpg.core.combat.FireCadence fireCadence() { return service.fireCadence(); }
    /** RELOAD's route: the shipped left-click handler, bound by RpgPlugin ({@link SelfTestService#bindLeftClick}). */
    java.util.function.Consumer<org.bukkit.entity.Player> leftClick() { return service.leftClick(); }
    /** CLEAR_STATE's: the one cooldown tracker, bound by RpgPlugin ({@link SelfTestService#bindCooldowns}); null if unbound. */
    io.github.butterflysmp.rpg.core.combat.CooldownTracker cooldowns() { return service.cooldowns(); }
    io.github.butterflysmp.rpg.paper.adapter.AdapterContext adapters() { return service.adapters(); }
}
