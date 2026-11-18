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
    /** The dying count at the current settle's first poll (ST9's {@code first=}). */
    private int settleFirst;
    private boolean ended;

    SelfTestRun(SelfTestService service, Player player, List<Scenario> queue) {
        this.service = service;
        this.player = player;
        this.queue = List.copyOf(queue);
        this.world = player.getWorld();
        this.drivers = new SelfTestDrivers(this, service.keys(), service.mobElements());
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
        Location start = snapshot.start();
        player.teleportAsync(start).thenRun(() -> service.scheduler().onEntity(player, () -> {
            if (ended) return;
            int blocks = snapshot.restoreRowBlocks();
            if (gateBoundary) snapshot.restoreInventory(player);
            int removed = snapshot.removeTagged(player, service.scheduler());
            log("CLEAN blocks=" + blocks + " removed=" + removed + " inventory=" + (gateBoundary ? "restored" : "kept")
                    + " return=" + String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", start.getX(), start.getY(), start.getZ()));
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
                .filter(e -> SelfTestStaging.leftover(e instanceof LivingEntity, e instanceof Player,
                        e instanceof org.bukkit.entity.ArmorStand, e instanceof LivingEntity l && l.isDead()))
                .sorted(java.util.Comparator.comparingDouble(e -> e.getLocation().distanceSquared(player.getLocation())))
                .map(e -> e.getType().getKey().getKey() + " " + e.getUniqueId())
                .toList();
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
                if (step.verb() == Scenario.Verb.ASK) {
                    ask(step);   // resumes from answer() or the timeout
                    return;
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
                dealer, type));
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
        end(why, service.scheduler(), false);
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
        end("ABORTED reason=server-stop", inline, true);
        for (String what : refused) service.log("SELFTEST RUN NOT-RESTORED at server-stop: a task asked for " + what);
    }

    private void end(String why, io.github.butterflysmp.rpg.paper.scheduler.Scheduler scheduler, boolean now) {
        if (ended) return;
        ended = true;
        drainReplies();
        if (reader != null) PacketEvents.getAPI().getEventManager().unregisterListener(reader);
        restoreRowRules(scheduler);   // a row that ended mid-way; the snapshot below puts the RUN START values back too
        String restored = snapshot.restore(player, service.profiles(), service.resources(), service.nameplates(),
                scheduler, now);
        // The drag's per-tick instrument is a scenario step's, never the server's: off at every run's end (it starts off).
        service.dragTrace().set(false);
        restored += " drag_tick=" + service.dragTrace().on();
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
    io.github.butterflysmp.rpg.paper.mining.MiningService mining() { return service.mining(); }
    io.github.butterflysmp.rpg.paper.adapter.DragTrace dragTrace() { return service.dragTrace(); }
    ProfileService profiles() { return service.profiles(); }
    CombatantStats stats() { return service.stats(); }
    SelfTestService.Spawner spawner() { return service.spawner(); }
    io.github.butterflysmp.rpg.paper.scheduler.Scheduler scheduler() { return service.scheduler(); }
    io.github.butterflysmp.rpg.paper.health.MobBeamer beams() { return service.beams(); }
    io.github.butterflysmp.rpg.paper.health.MobTeleporter teleports() { return service.teleports(); }
    io.github.butterflysmp.rpg.paper.health.MobShooter shots() { return service.shots(); }
    io.github.butterflysmp.rpg.paper.health.MobPouncer pounces() { return service.pounces(); }
    io.github.butterflysmp.rpg.paper.health.PhageFuser fuser() { return service.fuser(); }
}
