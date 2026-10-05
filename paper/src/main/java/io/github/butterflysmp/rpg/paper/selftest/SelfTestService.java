package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.health.MobNameplateManager;
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
    private final Logger log;
    private final String build;
    private final boolean devServer;
    private final ScenarioLoader.Loaded loaded;
    private SelfTestRun active;

    public SelfTestService(AdapterContext adapters, ProfileService profiles, ResourcePool resources,
                           MobNameplateManager nameplates, Logger log, String build, boolean devServer,
                           ScenarioLoader.Loaded loaded) {
        this.adapters = adapters;
        this.profiles = profiles;
        this.resources = resources;
        this.nameplates = nameplates;
        this.log = log;
        this.build = build;
        this.devServer = devServer;
        this.loaded = loaded;
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
                player.getWorld().equals(Bukkit.getWorlds().getFirst()), Bukkit.getOnlinePlayers().size());
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

    /** From {@code RpgListeners.onQuit}, FIRST: the restore must write while the inventory can still be written. */
    public void onQuit(Player player) {
        if (active != null && active.player().getUniqueId().equals(player.getUniqueId())) {
            active.end("ABORTED the player left");
        }
    }

    void ended(SelfTestRun run) {
        if (active == run) active = null;
    }

    // --- what a run reads ------------------------------------------------------------------------------------------

    void log(String line) { log.info(line); }
    String build() { return build; }
    Keys keys() { return adapters.keys(); }
    Scheduler scheduler() { return adapters.scheduler(); }
    CombatantStats stats() { return adapters.stats(); }
    ProfileService profiles() { return profiles; }
    ResourcePool resources() { return resources; }
    MobNameplateManager nameplates() { return nameplates; }
}
