package io.github.butterflysmp.rpg.paper;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.kit.KitRegistry;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponService;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.ImmobilizePhysics;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.command.RpgCommand;
import io.github.butterflysmp.rpg.paper.content.AbilityLoader;
import io.github.butterflysmp.rpg.paper.content.KitLoader;
import io.github.butterflysmp.rpg.paper.content.ContentValidator;
import io.github.butterflysmp.rpg.paper.content.ElementLoader;
import io.github.butterflysmp.rpg.paper.content.ElementRegistry;
import io.github.butterflysmp.rpg.paper.content.StatusLoader;
import io.github.butterflysmp.rpg.paper.content.StatusRegistry;
import io.github.butterflysmp.rpg.paper.content.VisualLoader;
import io.github.butterflysmp.rpg.paper.content.VisualRegistry;
import io.github.butterflysmp.rpg.paper.content.WeaponLoader;
import io.github.butterflysmp.rpg.paper.listener.RpgListeners;
import io.github.butterflysmp.rpg.paper.packet.ExampleTelegraphListener;
import io.github.butterflysmp.rpg.paper.packet.WeaponSwingListener;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.scheduler.PaperScheduler;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import io.github.butterflysmp.rpg.storage.FilePlayerRepository;
import io.github.butterflysmp.rpg.storage.PlayerRepository;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

public final class RpgPlugin extends JavaPlugin {

    /** Long enough for a flush of everyone online; short enough not to hang a restart. */
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 15;

    /**
     * Everything under this prefix in the plugin jar is shipped as a default on first
     * boot. There is no list. Adding an ability means adding a .yml, and nothing else.
     *
     * This used to be a hardcoded String[] of three paths, which meant the 500th weapon
     * needed a line of Java -- the exact thing CLAUDE.md invariant 2 forbids. The
     * pipeline was intact in the direction the invariant is usually read (an operator
     * drops a .yml into plugins/Rpg/content/ and it loads) and broken in the direction
     * the project needed (shipping that .yml in the jar).
     */
    private static final String CONTENT_PREFIX = "content/";

    /** Ability energy. A full bar in 60 seconds. Belongs in archetype content later. */
    private static final double MAX_ENERGY = 100.0;
    private static final double ENERGY_PER_TICK = MAX_ENERGY / (60 * 20);

    private Scheduler scheduler;
    private Keys keys;
    private AdapterContext adapters;
    private AbilityRegistry abilities;
    private VisualRegistry visuals;
    private StatusRegistry statuses;
    private ElementRegistry elements;
    private KitRegistry kits;
    private WeaponRegistry weapons;
    private CooldownTracker cooldowns;
    private ResourcePool resources;
    private AbilityService abilityService;
    private WeaponService weaponService;
    private ExecutorService storageIo;
    private PlayerRepository repository;
    private ProfileService profiles;

    @Override
    public void onEnable() {
        this.scheduler = new PaperScheduler(this);

        // Every NamespacedKey in the plugin, built once. Never inline at a call site.
        this.keys = new Keys(this);

        // Content: YAML -> definitions. Nothing here is hardcoded in Java.
        saveDefaultContent();

        File contentDir = new File(getDataFolder(), "content");
        this.abilities = new AbilityLoader(getLogger()).loadAll(new File(contentDir, "abilities"));
        this.visuals = new VisualLoader(getLogger()).loadAll(new File(contentDir, "visuals"));
        this.statuses = new StatusLoader(getLogger()).loadAll(new File(contentDir, "statuses"));
        this.elements = new ElementLoader(getLogger()).loadAll(new File(contentDir, "elements"));
        this.kits = new KitLoader(getLogger()).loadAll(new File(contentDir, "kits"));
        this.weapons = new WeaponLoader(getLogger()).loadAll(new File(contentDir, "weapons"));
        getLogger().info("Loaded " + abilities.size() + " abilities, "
                + visuals.size() + " visuals, " + statuses.size() + " statuses, "
                + elements.size() + " elements, "
                + kits.size() + " kits, " + weapons.size() + " weapons");

        // A visual_id that resolves to nothing should be found now, by name, not by
        // a player casting the ability in six weeks' time. Registry is only reachable
        // here, with the server up, which is why these arrive as predicates.
        validateContent();

        // The immobilize anchor's drift tolerance -- the one tuning knob, in config.yml so it
        // can be dialled without a rebuild (edit + restart). Clamped so a typo can't break it.
        saveDefaultConfig();
        double anchorDrift = Math.max(0.0, Math.min(2.0,
                getConfig().getDouble("immobilize.anchor-drift-blocks", ImmobilizePhysics.ANCHOR_DRIFT)));
        getLogger().info("Immobilize anchor drift tolerance: " + anchorDrift + " blocks");

        // Built once and shared: the adapters' warn-once set must outlive the
        // short-lived BukkitCombatant and PaperCombatWorld instances.
        this.adapters = new AdapterContext(scheduler, keys, visuals, statuses, getLogger(), anchorDrift);

        // core takes a tick supplier, not Bukkit, so it stays unit-testable.
        this.cooldowns = new CooldownTracker(Bukkit::getCurrentTick);
        this.resources = new ResourcePool(Bukkit::getCurrentTick, MAX_ENERGY, ENERGY_PER_TICK);
        this.abilityService = new AbilityService(abilities, cooldowns, resources);
        // A weapon trigger fires through the same cooldown/energy machinery, gate-free.
        this.weaponService = new WeaponService(abilityService);

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
                new RpgListeners(cooldowns, resources, profiles, weapons, weaponService, adapters), this);

        // PacketEvents is a SEPARATE PLUGIN on the server, declared in
        // paper-plugin.yml. We do NOT call PacketEvents.setAPI() or .load()
        // here -- that is only for shaded builds, and shading it would drag
        // GPL-3.0 onto this project.
        //
        // The swing listener reads the arm-swing packet to fire a weapon's left_click
        // trigger. It runs on Netty I/O threads and hops via PacketListenerBase before
        // touching anything Bukkit -- the one piece of Phase 1 that must not race.
        PacketEvents.getAPI().getEventManager()
                .registerListener(new ExampleTelegraphListener(scheduler));
        PacketEvents.getAPI().getEventManager()
                .registerListener(new WeaponSwingListener(adapters, weapons, weaponService));

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        RpgCommand.build(abilities, abilityService, adapters, kits, elements, profiles, weapons),
                        "RPG commands"));
    }

    /**
     * Copy every content/**.yml out of the plugin jar into the data folder, once.
     *
     * Enumerated with JarFile rather than through the resource API, and that is not a
     * stylistic choice. JavaPlugin.getResource returns an InputStream, so it cannot list
     * a directory at all; and a URLClassLoader reaching for the same thing gets a
     * jar:file:...!/content/ URL that opens to a ZERO-BYTE stream and whose getFile()
     * is not a path -- new File(url.getFile()).list() returns null. Measured against the
     * real shaded jar. A scan built on that route does not crash. It silently finds
     * nothing, which on a server whose data folder is already populated looks exactly
     * like a scan that works.
     *
     * Hence the warning below: finding zero shipped files is a defect, not a quiet no-op.
     *
     * saveResource(.., false) never overwrites. So this ships defaults; it does not
     * update them. Editing a .yml in the repo does NOT propagate to a data folder that
     * already holds it -- see NEXT.md's deferred list, "the tuning loop".
     */
    private void saveDefaultContent() {
        List<String> shipped;
        try (JarFile jar = new JarFile(getFile())) {
            shipped = jar.stream()
                    .map(JarEntry::getName)
                    .filter(name -> name.startsWith(CONTENT_PREFIX) && name.endsWith(".yml"))
                    .sorted() // deterministic, like the loaders' Arrays.sort
                    .toList();
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE,
                    "Could not read the plugin jar to find default content under " + CONTENT_PREFIX
                            + "; no defaults will be written", ex);
            return;
        }

        if (shipped.isEmpty()) {
            getLogger().warning("No default content found in the plugin jar under '" + CONTENT_PREFIX
                    + "'. If the data folder is already populated the server will still run, and this"
                    + " will look like it worked. It did not.");
            return;
        }

        for (String path : shipped) {
            saveResource(path, false);
        }
    }

    /**
     * Warns, never disables the plugin. Fail-soft: the ability still loads and still
     * deals its damage; it just tells you which reference is dangling.
     */
    private void validateContent() {
        var validator = new ContentValidator(visuals, statuses, elements,
                key -> Registry.MOB_EFFECT.get(key) != null,
                key -> Registry.SOUND_EVENT.get(key) != null);

        List<String> problems = validator.validate(abilities);
        // A kit naming an ability or weapon nothing defines is the most invisible dangling
        // reference of all: it reads as a deliberate gap, not a typo. Both registries are
        // available here, so they arrive as the predicate seams.
        problems.addAll(validator.validateKits(kits.all(),
                id -> abilities.find(id).isPresent(),
                id -> weapons.find(id).isPresent()));
        // A weapon trigger's on_hit can dangle a visual_id or status_id the same way an
        // ability's can, and is checked the same walk. Naming the file at boot beats a
        // silent no-visual the first time someone swings it.
        problems.addAll(validator.validateWeapons(weapons.all()));
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
    public KitRegistry kits() { return kits; }
    public WeaponRegistry weapons() { return weapons; }
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

    /** Fires a weapon trigger through the shared cooldown/energy path, gate-free. */
    public WeaponService weaponService() { return weaponService; }
}
