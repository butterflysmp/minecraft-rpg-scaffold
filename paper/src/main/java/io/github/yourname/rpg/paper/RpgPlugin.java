package io.github.yourname.rpg.paper;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.yourname.rpg.core.ability.AbilityRegistry;
import io.github.yourname.rpg.core.ability.AbilityService;
import io.github.yourname.rpg.core.combat.CooldownTracker;
import io.github.yourname.rpg.core.combat.ResourcePool;
import io.github.yourname.rpg.paper.adapter.AdapterContext;
import io.github.yourname.rpg.paper.adapter.Keys;
import io.github.yourname.rpg.paper.command.RpgCommand;
import io.github.yourname.rpg.paper.content.AbilityLoader;
import io.github.yourname.rpg.paper.content.ContentValidator;
import io.github.yourname.rpg.paper.content.StatusLoader;
import io.github.yourname.rpg.paper.content.StatusRegistry;
import io.github.yourname.rpg.paper.content.VisualLoader;
import io.github.yourname.rpg.paper.content.VisualRegistry;
import io.github.yourname.rpg.paper.listener.RpgListeners;
import io.github.yourname.rpg.paper.packet.ExampleTelegraphListener;
import io.github.yourname.rpg.paper.profile.ProfileService;
import io.github.yourname.rpg.paper.scheduler.PaperScheduler;
import io.github.yourname.rpg.paper.scheduler.Scheduler;
import io.github.yourname.rpg.storage.FilePlayerRepository;
import io.github.yourname.rpg.storage.PlayerRepository;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class RpgPlugin extends JavaPlugin {

    /** Long enough for a flush of everyone online; short enough not to hang a restart. */
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 15;

    /** Shipped as defaults on first boot. Never overwritten -- saveResource(.., false). */
    private static final String[] DEFAULT_CONTENT = {
            "content/abilities/solar_grenade.yml",
            "content/visuals/solar_detonation.yml",
            "content/statuses/scorch.yml",
    };

    /** Ability energy. A full bar in 60 seconds. Belongs in archetype content later. */
    private static final double MAX_ENERGY = 100.0;
    private static final double ENERGY_PER_TICK = MAX_ENERGY / (60 * 20);

    private Scheduler scheduler;
    private Keys keys;
    private AdapterContext adapters;
    private AbilityRegistry abilities;
    private VisualRegistry visuals;
    private StatusRegistry statuses;
    private CooldownTracker cooldowns;
    private ResourcePool resources;
    private AbilityService abilityService;
    private ExecutorService storageIo;
    private PlayerRepository repository;
    private ProfileService profiles;

    @Override
    public void onEnable() {
        this.scheduler = new PaperScheduler(this);

        // Every NamespacedKey in the plugin, built once. Never inline at a call site.
        this.keys = new Keys(this);

        // Content: YAML -> definitions. Nothing here is hardcoded in Java.
        for (String path : DEFAULT_CONTENT) {
            saveResource(path, false);
        }
        File contentDir = new File(getDataFolder(), "content");
        this.abilities = new AbilityLoader(getLogger()).loadAll(new File(contentDir, "abilities"));
        this.visuals = new VisualLoader(getLogger()).loadAll(new File(contentDir, "visuals"));
        this.statuses = new StatusLoader(getLogger()).loadAll(new File(contentDir, "statuses"));
        getLogger().info("Loaded " + abilities.size() + " abilities, "
                + visuals.size() + " visuals, " + statuses.size() + " statuses");

        // A visual_id that resolves to nothing should be found now, by name, not by
        // a player casting the ability in six weeks' time. Registry is only reachable
        // here, with the server up, which is why these arrive as predicates.
        validateContent();

        // Built once and shared: the adapters' warn-once set must outlive the
        // short-lived BukkitCombatant and PaperCombatWorld instances.
        this.adapters = new AdapterContext(scheduler, keys, visuals, statuses, getLogger());

        // core takes a tick supplier, not Bukkit, so it stays unit-testable.
        this.cooldowns = new CooldownTracker(Bukkit::getCurrentTick);
        this.resources = new ResourcePool(Bukkit::getCurrentTick, MAX_ENERGY, ENERGY_PER_TICK);
        this.abilityService = new AbilityService(abilities, cooldowns, resources);

        // One thread: file writes for a single player must not race each other,
        // and a serialised queue is plenty for milestone-1 storage. Not a daemon
        // thread -- a pending write must finish even if the JVM is winding down.
        this.storageIo = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "rpg-storage-io");
            thread.setDaemon(false);
            return thread;
        });
        this.repository = new FilePlayerRepository(
                new File(getDataFolder(), "players").toPath(), storageIo);
        this.profiles = new ProfileService(repository, getLogger(), System::currentTimeMillis);

        // The one and only registerEvents call. Keep it that way.
        getServer().getPluginManager().registerEvents(
                new RpgListeners(cooldowns, resources, profiles), this);

        // PacketEvents is a SEPARATE PLUGIN on the server, declared in
        // paper-plugin.yml. We do NOT call PacketEvents.setAPI() or .load()
        // here -- that is only for shaded builds, and shading it would drag
        // GPL-3.0 onto this project.
        PacketEvents.getAPI().getEventManager()
                .registerListener(new ExampleTelegraphListener(scheduler));

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        RpgCommand.build(abilities, abilityService, adapters), "RPG commands"));
    }

    /**
     * Warns, never disables the plugin. Fail-soft: the ability still loads and still
     * deals its damage; it just tells you which reference is dangling.
     */
    private void validateContent() {
        var validator = new ContentValidator(visuals, statuses,
                key -> Registry.MOB_EFFECT.get(key) != null,
                key -> Registry.SOUND_EVENT.get(key) != null);

        List<String> problems = validator.validate(abilities);
        for (String problem : problems) {
            getLogger().warning("Content: " + problem);
        }
        if (!problems.isEmpty()) {
            getLogger().warning(problems.size() + " dangling content reference(s). "
                    + "The server is still running, but those effects will do nothing.");
        }
    }

    @Override
    public void onDisable() {
        // PlayerQuitEvent is not guaranteed to fire for everyone on shutdown, so
        // flush whoever is left. Blocking is correct here: the server is stopping
        // and unwritten profiles are lost progress.
        if (profiles != null) {
            try {
                profiles.saveAllAndClear().get(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                getLogger().warning("Interrupted while saving profiles on shutdown");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to save all profiles on shutdown", e);
            }
        }

        if (storageIo != null) {
            storageIo.shutdown();
            try {
                if (!storageIo.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    getLogger().severe("Storage I/O did not drain; some profiles may be unsaved");
                    storageIo.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                storageIo.shutdownNow();
            }
        }

        // PacketEvents terminates itself; it owns its own lifecycle.
    }

    public Scheduler scheduler() { return scheduler; }
    public Keys keys() { return keys; }
    public AdapterContext adapters() { return adapters; }
    public AbilityRegistry abilities() { return abilities; }
    public VisualRegistry visuals() { return visuals; }
    public StatusRegistry statuses() { return statuses; }
    public CooldownTracker cooldowns() { return cooldowns; }
    public ResourcePool resources() { return resources; }
    public PlayerRepository repository() { return repository; }
    public ProfileService profiles() { return profiles; }

    /**
     * cast() only decides; the caller must run the returned effects on a region
     * thread. Today that is the region owning the caster's eye, which is not always
     * the one owning the impact. See AbilityService for why, and what it costs.
     */
    public AbilityService abilityService() { return abilityService; }
}
