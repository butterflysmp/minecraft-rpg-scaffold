package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.mob.MobElementResolver;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.health.MobNameplateManager;
import io.github.butterflysmp.rpg.paper.mining.MiningService;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * {@code /rpg selftest}'s state: the loaded scenarios, and at most ONE run.
 *
 * <p>Built in {@code RpgPlugin.onEnable}, which logs {@link #enableLine()} -- the boot's witness that the dev marker
 * reached the JVM and how many scenarios the jar carries.
 */
public final class SelfTestService {

    private final AdapterContext adapters;
    private final ProfileService profiles;
    private final ResourcePool resources;
    private final MobNameplateManager nameplates;
    private final MiningService mining;
    private final Logger log;
    private final String build;
    private final boolean devServer;
    private final ScenarioLoader.Loaded loaded;
    private final MobElementResolver mobElements;
    /** The Knell's beam, for the BEAM step (dragon-mobs step R5). */
    private final io.github.butterflysmp.rpg.paper.health.MobBeamer beams;
    private final io.github.butterflysmp.rpg.paper.health.MobShooter shots;
    /** The Knell's teleport, for the TELEPORT_DRAW step (dragon-mobs step R6). */
    private final io.github.butterflysmp.rpg.paper.health.MobTeleporter teleports;
    private final Spawner spawner;
    private SelfTestRun active;

    /**
     * {@code /rpg spawn}'s own path with a pre-spawn hook, for the SPAWN step (GATE-dragon-fix.md, C2). RpgPlugin binds
     * it to {@code RpgCommand.spawnFor}, so the step spawns exactly what the command spawns. Empty when refused.
     */
    @FunctionalInterface
    public interface Spawner {
        Optional<org.bukkit.entity.LivingEntity> spawn(Player player, String mob, Integer stage, Integer gearScore,
                                                     java.util.function.Consumer<? super org.bukkit.entity.LivingEntity> preAdd);
    }

    public SelfTestService(AdapterContext adapters, ProfileService profiles, ResourcePool resources,
                           MobNameplateManager nameplates, MiningService mining, Logger log, String build, boolean devServer,
                           ScenarioLoader.Loaded loaded, MobElementResolver mobElements, Spawner spawner,
                           io.github.butterflysmp.rpg.paper.health.MobBeamer beams,
                           io.github.butterflysmp.rpg.paper.health.MobTeleporter teleports,
                           io.github.butterflysmp.rpg.paper.health.MobShooter shots) {
        this.shots = shots;
        // A tagged Phalanx's shot is tagged by identity at its fire (step R7a), as a split child is at its birth.
        shots.addListener(this::adoptShot);
        this.teleports = teleports;
        this.beams = beams;
        this.spawner = spawner;
        this.adapters = adapters;
        this.profiles = profiles;
        this.resources = resources;
        this.nameplates = nameplates;
        this.mining = mining;
        this.log = log;
        this.build = build;
        this.devServer = devServer;
        this.loaded = loaded;
        this.mobElements = mobElements;
    }

    /** One line at boot. GATE-selftest.md's R0d reads it. Refusals follow it, one WARN each. */
    public String enableLine() {
        return "Selftest: " + (devServer ? "dev server" : "NOT a dev server (-D" + SelfTestGuard.DEV_PROPERTY
                + "=true absent); /rpg selftest refuses") + ", " + loaded.rows() + " scenarios across "
                + loaded.gates().size() + " gates " + loaded.gates().keySet();
    }

    public List<String> refusals() {
        return loaded.refusals();
    }

    public Map<String, List<Scenario>> gates() {
        return loaded.gates();
    }

    /** Start {@code all}, one gate, or one row of one gate. Returns the reply. */
    public String start(Player player, String gate, String row) {
        Optional<String> refusal = SelfTestGuard.refusal(devServer,
                player.getWorld().equals(Bukkit.getWorlds().getFirst()), Bukkit.getOnlinePlayers().size(),
                player.getGameMode().name());
        if (refusal.isPresent()) return refusal.get();
        if (active != null) return "Refusing: a selftest is already running. /rpg selftest stop ends it.";

        List<Scenario> queue = new ArrayList<>();
        if (gate.equals("all")) {
            loaded.gates().values().forEach(queue::addAll);
        } else {
            List<Scenario> scenarios = loaded.gates().get(gate);
            if (scenarios == null) return "No selftest gate '" + gate + "'. Gates: " + loaded.gates().keySet();
            for (Scenario s : scenarios) {
                // A single row still runs its gate's setup first: the row was written against that setup.
                if (row == null || s.row().equals(row) || s.row().equals(Scenario.SETUP)) queue.add(s);
            }
            if (row != null && queue.stream().noneMatch(s -> s.row().equals(row))) {
                return "No row '" + row + "' in selftest gate '" + gate + "'.";
            }
        }
        active = new SelfTestRun(this, player, queue);
        active.start(gate + (row == null ? "" : " " + row));
        return "Selftest started: " + gate + (row == null ? "" : " " + row)
                + ". The log carries every reading; this prints none. /rpg selftest stop ends it and restores.";
    }

    public String stop(Player player) {
        if (active == null) return "No selftest is running.";
        active.end("STOPPED by /rpg selftest stop");
        return "Selftest stopped; everything it changed is restored (the log says what).";
    }

    /**
     * From {@code RpgPlugin.onDisable}, FIRST (GATE-dragon-knell-4.md): a server stop during a run. The quit event comes
     * after the plugin is disabled, so it cannot do this; the run restores here, synchronously, before the server
     * saves players and chunks.
     */
    public void onDisable() {
        if (active != null) active.endAtDisable();
    }

    /** From {@code RpgListeners.onQuit}, FIRST: the restore must write while the inventory can still be written. */
    public void onQuit(Player player) {
        if (active != null && active.player().getUniqueId().equals(player.getUniqueId())) {
            active.end("ABORTED the player left");
        }
    }

    /**
     * The player's chat line, from {@code RpgListeners.onSelfTestChat} on the ASYNC chat thread (GATE-dragon-fix.md,
     * C6). Nothing is read or touched here but the hand-off: the run is reached on the player's own scheduler, where it
     * lives, and ignores the line unless a question is waiting.
     */
    public void onChat(Player player, String line) {
        adapters.scheduler().onEntity(player, () -> {
            SelfTestRun run = active;
            if (run != null && run.player().getUniqueId().equals(player.getUniqueId())) run.answer(line);
        });
    }

    /**
     * A DAMAGE change on a player (dragon-mobs step R3), from {@link SelfTestDamageTap}: the running row's HURT line,
     * when the player is the run's. Called on the player's own thread, inside the hit.
     */
    void onPlayerHurt(io.github.butterflysmp.rpg.core.combat.stat.HealthChange change) {
        SelfTestRun run = active;
        if (run != null) run.hurt(change);
    }

    void ended(SelfTestRun run) {
        if (active == run) active = null;
    }

    // --- what a run reads ------------------------------------------------------------------------------------------

    void log(String line) { log.info(line); }
    String build() { return build; }
    Keys keys() { return adapters.keys(); }

    /**
     * A split child of a mob the instrument tagged takes its parent's tags, inside the child's pre-spawn consumer, so
     * it is never in the world untagged (PLAN-dragon-mobs.md step R2; {@link SelfTestSnapshot#tagsForSplitChild}).
     * Called by the split for every child; a no-op for a parent the instrument never tagged. From step R3 a FROZEN
     * parent's child is also born frozen ({@link SelfTestSnapshot#freezeSplitChild}), and each adopted child logs
     * {@code CHILD parent=<uuid> child=<uuid> frozen=<true|false>}, so a gate can read which it was.
     */
    public void adoptSplitChild(org.bukkit.entity.LivingEntity parent, org.bukkit.entity.LivingEntity child) {
        java.util.Set<String> tags = SelfTestSnapshot.tagsForSplitChild(parent.getScoreboardTags());
        if (tags.isEmpty()) return;   // a mob the instrument never tagged: production, untouched
        for (String tag : tags) child.addScoreboardTag(tag);
        // Step R3: a frozen parent's child is born frozen, here, before its first tick (SelfTestSnapshot's javadoc).
        if (SelfTestSnapshot.freezeSplitChild(parent.getScoreboardTags(), parent.hasAI())) child.setAI(false);
        // READ BACK from the child, not the rule's answer, so the line witnesses the freeze landing.
        String line = "CHILD parent=" + parent.getUniqueId() + " child=" + child.getUniqueId()
                + " frozen=" + !child.hasAI();
        SelfTestRun run = active;
        if (run != null) run.log(line);
        else log(line);
    }

    /**
     * A Phalanx's shot was fired (step R7a): if the instrument tagged the Phalanx, the shot takes
     * {@link SelfTestSnapshot#tagsForShot}, and one line names it, so a row reads which carrier is its shot.
     */
    /** One of the player's own swings, attacks or interacts (step R7a): to the active run, which logs it only while a row watches. */
    public void watchSwing(org.bukkit.entity.Player p, String kind) {
        SelfTestRun run = active;
        if (run != null && run.player().equals(p)) run.watchSwing(kind);
    }

    public void adoptShot(org.bukkit.entity.Mob owner, org.bukkit.entity.LivingEntity carrier) {
        java.util.Set<String> tags = SelfTestSnapshot.tagsForShot(owner.getScoreboardTags());
        if (tags.isEmpty()) return;
        for (String tag : tags) carrier.addScoreboardTag(tag);
        String line = "SHOTTAG owner=" + owner.getUniqueId() + " shot=" + carrier.getUniqueId() + " tags="
                + new java.util.TreeSet<>(carrier.getScoreboardTags());
        SelfTestRun run = active;
        if (run != null) run.log(line);
        else log(line);
    }

    /** "The element of this creature", for PROBE_BODY (PLAN-dragon-mobs.md step R1). */
    MobElementResolver mobElements() { return mobElements; }
    io.github.butterflysmp.rpg.paper.health.MobBeamer beams() { return beams; }
    io.github.butterflysmp.rpg.paper.health.MobTeleporter teleports() { return teleports; }
    io.github.butterflysmp.rpg.paper.health.MobShooter shots() { return shots; }
    Spawner spawner() { return spawner; }
    Scheduler scheduler() { return adapters.scheduler(); }
    CombatantStats stats() { return adapters.stats(); }
    ProfileService profiles() { return profiles; }
    ResourcePool resources() { return resources; }
    MobNameplateManager nameplates() { return nameplates; }
    MiningService mining() { return mining; }
}
