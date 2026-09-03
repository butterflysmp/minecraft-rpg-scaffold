package io.github.butterflysmp.rpg.paper;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.kit.KitRegistry;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ManaRegen;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.combat.stat.CompositeHealthListener;
import io.github.butterflysmp.rpg.core.weapon.CraftResultIndex;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import io.github.butterflysmp.rpg.core.weapon.ToolDefinition;
import io.github.butterflysmp.rpg.core.weapon.ToolRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponService;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.ImmobilizePhysics;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.command.RpgCommand;
import io.github.butterflysmp.rpg.paper.content.AbilityLoader;
import io.github.butterflysmp.rpg.paper.content.KitLoader;
import io.github.butterflysmp.rpg.paper.content.MobLoader;
import io.github.butterflysmp.rpg.paper.content.ContentValidator;
import io.github.butterflysmp.rpg.paper.content.ElementLoader;
import io.github.butterflysmp.rpg.paper.content.ElementRegistry;
import io.github.butterflysmp.rpg.paper.content.EnchantLoader;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import io.github.butterflysmp.rpg.paper.content.StatusLoader;
import io.github.butterflysmp.rpg.paper.content.StatusRegistry;
import io.github.butterflysmp.rpg.paper.content.VisualLoader;
import io.github.butterflysmp.rpg.paper.content.VisualRegistry;
import io.github.butterflysmp.rpg.paper.content.ArmorConsistency;
import io.github.butterflysmp.rpg.paper.content.ArmorLoader;
import io.github.butterflysmp.rpg.paper.content.ShieldLoader;
import io.github.butterflysmp.rpg.paper.content.ToolLoader;
import io.github.butterflysmp.rpg.paper.content.WeaponLoader;
import io.github.butterflysmp.rpg.paper.health.DamagePopupManager;
import io.github.butterflysmp.rpg.paper.health.MobDeathSystem;
import io.github.butterflysmp.rpg.paper.health.MobNameplateManager;
import io.github.butterflysmp.rpg.paper.health.PacketDamagePopupSender;
import io.github.butterflysmp.rpg.paper.health.PacketNameplateSender;
import io.github.butterflysmp.rpg.paper.health.PlayerHealthSystem;
import io.github.butterflysmp.rpg.paper.hud.StatsBarSystem;
import io.github.butterflysmp.rpg.paper.health.HealthRegenSystem;
import io.github.butterflysmp.rpg.paper.listener.RpgListeners;
import io.github.butterflysmp.rpg.paper.menu.Menu;
import io.github.butterflysmp.rpg.paper.packet.ExampleTelegraphListener;
import io.github.butterflysmp.rpg.paper.packet.VanillaCritParticleListener;
import io.github.butterflysmp.rpg.paper.packet.WeaponSwingListener;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.scheduler.PaperScheduler;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import io.github.butterflysmp.rpg.storage.FilePlayerRepository;
import io.github.butterflysmp.rpg.storage.PlayerRepository;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    /** Ability mana. A full bar in {@link #MANA_REFILL_SECONDS}. Belongs in archetype content later. */
    private static final double MAX_MANA = 100.0;

    /**
     * How long a full bare bar takes to refill: 100 seconds, so the base rate is a round 1 mana/s
     * and reads {@code 5.00/5s} on the stat sheet.
     *
     * <p><b>Rebalanced from 60 in Stats Slice 3.</b> Named rather than inlined so the intent is in
     * one place and the constant below cannot be retuned without the reason moving with it.
     */
    private static final int MANA_REFILL_SECONDS = 100;

    /**
     * The BASE refill rate, per tick.
     *
     * <p><b>Per-tick is canonical; per-second is DERIVED from it</b>
     * ({@code ManaRegen.perSecond(MANA_PER_TICK)}), never the reverse, and the resolver below
     * composes IN TICKS. That is not a style rule -- it is why this is written as one division rather
     * than two.
     *
     * <p><b>The hazard that rule exists for does not show at THIS base, and that is exactly when
     * someone deletes the rule.</b> Slice 2 measured it on the old 60-second base:
     * {@code 100/(60*20)} is {@code 0x1.5555555555555p-4} while {@code (100/60.0)/20.0} is
     * {@code 0x1.5555555555556p-4} -- one ULP apart, {@code ==} false, so reaching the value the
     * other way would have re-rated every player on the server silently. At the 100-second base the
     * two orderings agree exactly ({@code 0x1.999999999999ap-5} either way), because the numbers
     * happen to be kind.
     *
     * <p>They will not always be. The next retune picks a divisor at random as far as this is
     * concerned, so the single-division form and the derive-from-ticks direction stay -- they cost
     * nothing and they are the difference between a rebalance that ships what it says and one that
     * ships a rate nobody chose. {@code ManaRegenTest} keeps the 60-second case as a standing
     * witness for the same reason.
     */
    private static final double MANA_PER_TICK = MAX_MANA / (MANA_REFILL_SECONDS * 20);

    private Scheduler scheduler;
    private Keys keys;
    private AdapterContext adapters;
    private AbilityRegistry abilities;
    private VisualRegistry visuals;
    private StatusRegistry statuses;
    private ElementRegistry elements;
    private EnchantRegistry enchants;
    private KitRegistry kits;
    private WeaponRegistry weapons;
    private ShieldRegistry shields;
    private ArmorRegistry armor;
    private ToolRegistry tools;
    private MobRegistry mobs;
    private CraftResultIndex craftResults;
    private CooldownTracker cooldowns;
    private ResourcePool resources;
    private CombatantStats stats;
    private PlayerHealthSystem healthSystem;
    private MobNameplateManager nameplates;
    private StatsBarSystem statsBar;
    private HealthRegenSystem healthRegen;
    private DamagePopupManager popups;
    private MobDeathSystem mobDeath;
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
        this.enchants = new EnchantLoader(getLogger()).loadAll(new File(contentDir, "enchants"));
        this.kits = new KitLoader(getLogger()).loadAll(new File(contentDir, "kits"));
        this.weapons = new WeaponLoader(getLogger()).loadAll(new File(contentDir, "weapons"));
        this.shields = new ShieldLoader(getLogger()).loadAll(new File(contentDir, "shields"));
        this.armor = new ArmorLoader(getLogger()).loadAll(new File(contentDir, "armor"));
        this.tools = new ToolLoader(getLogger()).loadAll(new File(contentDir, "tools"));
        this.mobs = new MobLoader(getLogger()).loadAll(new File(contentDir, "mobs"));
        getLogger().info("Loaded " + abilities.size() + " abilities, "
                + visuals.size() + " visuals, " + statuses.size() + " statuses, "
                + elements.size() + " elements, " + enchants.size() + " enchants, "
                + kits.size() + " kits, " + weapons.size() + " weapons, "
                + shields.size() + " shields, " + armor.size() + " armor, "
                + tools.size() + " tools, " + mobs.size() + " mobs");

        // ZERO IS A DEFECT, NOT A QUIET NO-OP. A loader that discovers nothing reads exactly like
        // one that worked, and this is the failure mode CLAUDE.md records twice: getResource on a
        // shaded jar returns a non-null URL whose stream is zero bytes, and on a data folder that
        // is already populated the difference is invisible. Said out loud, at WARNING, because the
        // boot gate's first step is to read this line.
        if (enchants.size() == 0) {
            getLogger().warning("No enchants loaded from content/enchants -- every enchant will "
                    + "render as its raw id, /rpg enchant can grant nothing, and Unbreaking can "
                    + "never appear on a tooltip. Expected at least unbreaking.yml.");
        }

        // The same guard, for the same reason, on the directory this slice adds. content/shields is
        // BRAND NEW, which makes it the most likely of all of them to arrive empty: an existing
        // run/ data folder predates it entirely, and saveResource never overwrites, so the only
        // thing that puts shield.yml on disk is the jar enumeration finding it.
        if (shields.size() == 0) {
            getLogger().warning("No shields loaded from content/shields -- /rpg give can mint none, "
                    + "and blocking will reduce nothing however many shields are held. Expected at "
                    + "least shield.yml.");
        }

        // ONE ID, TWO REGISTRIES. /rpg give resolves weapons before shields, so a shared id would
        // silently make the shield unmintable -- and it would look exactly like the shield having
        // failed to load, which the warning above would then NOT fire for. Neither registry can see
        // the other, so this is the only place the collision is visible.
        for (ShieldDefinition shield : shields.all()) {
            if (weapons.find(shield.id()).isPresent()) {
                getLogger().warning("Shield '" + shield.id() + "' shares its id with a weapon. "
                        + "/rpg give resolves weapons first, so the shield cannot be minted by id. "
                        + "Rename one of the two content files.");
            }
        }


        // And again on content/armor, which this slice adds. Newest directory, same reasoning as
        // shields: an existing run/ data folder predates it entirely, and saveResource never
        // overwrites, so the only thing that puts the six tier files on disk is the jar
        // enumeration finding them. Twenty-four is the expected count -- six tiers, four slots --
        // and a number below that means a tier file was skipped, which its own warning will have
        // said out loud.
        if (armor.size() == 0) {
            getLogger().warning("No armor loaded from content/armor -- /rpg give can mint no armor. "
                    + "Note this does NOT disable the Defense stat: a plain vanilla chestplate still "
                    + "sources its full Defense, because that is read from vanilla and not from a "
                    + "tag. Expected 24 pieces from six tier files.");
        }

        // ONE ID, THREE REGISTRIES. /rpg give resolves weapons, then shields, then armor, so a
        // shared id silently shadows whichever comes later -- and that looks exactly like the
        // shadowed piece having failed to load, which the zero-checks above would NOT fire for.
        // No registry can see the others, so this is the only place a collision is visible.
        for (ArmorDefinition piece : armor.all()) {
            if (weapons.find(piece.id()).isPresent()) {
                getLogger().warning("Armor '" + piece.id() + "' shares its id with a weapon. "
                        + "/rpg give resolves weapons first, so the armor piece cannot be minted by "
                        + "id. Rename one of the two content files.");
            }
            if (shields.find(piece.id()).isPresent()) {
                getLogger().warning("Armor '" + piece.id() + "' shares its id with a shield. "
                        + "/rpg give resolves shields first, so the armor piece cannot be minted by "
                        + "id. Rename one of the two content files.");
            }
        }

        // And again on content/tools, which this slice adds. NEWEST directory, so it is the most
        // likely of the four to arrive empty for the reason the shield guard already records: an
        // existing run/ data folder predates it entirely and saveResource never overwrites, so the
        // only thing that puts iron.yml on disk is the jar enumeration finding it. On a populated
        // data folder that failure is invisible; only a FRESH one exposes it.
        //
        // FIVE is the expected count, and the arithmetic is worth stating because it is not a grid:
        // four tiered kinds plus shears, which has no tier at all. A number below five means an
        // ENTRY was skipped -- not the file -- and its own warning will have named it.
        if (tools.size() == 0) {
            getLogger().warning("No tools loaded from content/tools -- /rpg give can mint none, and "
                    + "crafting an iron pickaxe will hand out a plain vanilla one. Note this does "
                    + "NOT break mining: an untagged pickaxe digs exactly as a minted one does, "
                    + "because we pin no attributes. Expected 5 tools from iron.yml.");
        }

        // ONE ID, FOUR REGISTRIES. /rpg give resolves weapons, then shields, then armor, then
        // tools, so a shared id silently shadows whichever comes later -- and that looks exactly
        // like the shadowed tool having failed to load, which the zero-check above would NOT fire
        // for. No registry can see the others, so this is the only place a collision is visible.
        //
        // Tools resolve LAST, so this loop has three arms where armor's has two. Not symmetry for
        // its own sake: a tool is the one kind that can lose every contest, so it is the one whose
        // collisions most need naming.
        for (ToolDefinition tool : tools.all()) {
            if (weapons.find(tool.id()).isPresent()) {
                getLogger().warning("Tool '" + tool.id() + "' shares its id with a weapon. "
                        + "/rpg give resolves weapons first, so the tool cannot be minted by id. "
                        + "Rename one of the two content files.");
            }
            if (shields.find(tool.id()).isPresent()) {
                getLogger().warning("Tool '" + tool.id() + "' shares its id with a shield. "
                        + "/rpg give resolves shields first, so the tool cannot be minted by id. "
                        + "Rename one of the two content files.");
            }
            if (armor.find(tool.id()).isPresent()) {
                getLogger().warning("Tool '" + tool.id() + "' shares its id with a piece of armor. "
                        + "/rpg give resolves armor first, so the tool cannot be minted by id. "
                        + "Rename one of the two content files.");
            }
        }

        // The tooltip number against vanilla's. This is the ONLY moment the two live in the same
        // JVM: content/armor authors the defense a piece DISPLAYS, vanilla owns the defense it
        // DELIVERS, and nothing makes them agree. A mismatch is invisible from every vantage point
        // in-game -- see ArmorConsistency's own javadoc -- so it has to be shouted about here.
        ArmorConsistency.check(armor, getLogger());

        // A visual_id that resolves to nothing should be found now, by name, not by
        // a player casting the ability in six weeks' time. Registry is only reachable
        // here, with the server up, which is why these arrive as predicates.
        validateContent();

        // WHICH CRAFTED ITEM BECOMES WHICH GEAR. Built once, here, because the crafting preview runs
        // several times a second and a scan per craft would walk every definition on every grid
        // change. Built AFTER the loaders and BEFORE the AdapterContext that carries it.
        //
        // Keyed on craft_result and never on material: materials are contested by design (every
        // sword-shaped weapon leaves material at DEFAULT_MATERIAL), so an index keyed on them would
        // warn forever about correct content and never mint a sword. See CraftResultIndex.
        List<GearDefinition> allGear = new ArrayList<>();
        allGear.addAll(weapons.all());
        allGear.addAll(shields.all());
        allGear.addAll(armor.all());
        allGear.addAll(tools.all());
        this.craftResults = CraftResultIndex.build(allGear,
                problem -> getLogger().warning("Content: " + problem));

        // THE POSITIVE CONTROL, and it matters as much as the collision warning above. An index that
        // built EMPTY -- a renamed key, a load-order slip, registries not yet populated -- produces
        // exactly the same log as an index with no collisions: silence. Every craft would then stay
        // vanilla and nothing anywhere would say why. So the count that DID register is printed, and
        // a gate row reads this line before anyone starts crafting.
        //
        // THREE NUMBERS, AND THE THIRD IS THE ONLY REAL CONTROL. `size` and `claimed` both derive
        // from the SAME parse: a bug that dropped every armor claim would zero both together and
        // print "1 indexed, 1 claiming" -- internally consistent, and indistinguishable from a
        // server where only the shield opted in. `allGear.size()` comes from the registries instead,
        // so the same bug reads "1 indexed, 1 claiming, of 30" and is wrong at a glance.
        //
        // Same shape as a grep with no positive control: a self-consistent pair proves nothing, and
        // only a number from outside the thing being checked tells a working parse from a dead one.
        getLogger().info("Mint-on-craft: " + craftResults.size() + " indexed, "
                + craftResults.claimed() + " claiming, of " + allGear.size() + " gear definitions"
                + (craftResults.contested() > 0
                        ? ", " + craftResults.contested() + " dropped as contested" : ""));

        // ZERO IS A DEFECT, NOT A QUIET NO-OP -- the same rule the loader counts above follow. Zero
        // here means no crafted item will ever become RPG gear, which in play is indistinguishable
        // from the feature not existing.
        if (craftResults.size() == 0) {
            getLogger().warning("No craft_result claims were indexed -- crafting will never mint RPG "
                    + "gear, and a crafted shield will give ZERO custom protection. Expected at "
                    + "least shield.yml, the armor pieces and the iron tools to claim one.");
        }

        // The immobilize anchor's drift tolerance -- the one tuning knob, in config.yml so it
        // can be dialled without a rebuild (edit + restart). Clamped so a typo can't break it.
        saveDefaultConfig();
        double anchorDrift = Math.max(0.0, Math.min(2.0,
                getConfig().getDouble("immobilize.anchor-drift-blocks", ImmobilizePhysics.ANCHOR_DRIFT)));
        getLogger().info("Immobilize anchor drift tolerance: " + anchorDrift + " blocks");

        // Custom health: the store is the source of truth; TWO displays ride its HealthChange seam,
        // fanned out by a composite listener -- the player heart bar and the per-viewer mob nameplate.
        // Two-step bind breaks the cycle (the store needs a listener, each display needs the store).
        this.healthSystem = new PlayerHealthSystem(scheduler, keys, weapons, enchants);
        this.nameplates = new MobNameplateManager(scheduler, new PacketNameplateSender(), keys, mobs);
        // Third display: the per-dealer damage-number popup. Pure seam consumer -- reads amount/dealer
        // off the event, so no bind(stats) and no mob-lifecycle hooks (unlike the nameplate).
        this.popups = new DamagePopupManager(scheduler, new PacketDamagePopupSender());
        // Fourth consumer: mob death. Also a pure seam consumer (no bind). Wired LAST so the displays
        // above render the final state before it kills the mob on the reachedZero transition.
        this.mobDeath = new MobDeathSystem(scheduler);
        this.stats = new CombatantStats(new CompositeHealthListener(healthSystem, nameplates, popups, mobDeath));
        this.healthSystem.bind(stats);
        this.nameplates.bind(stats);

        // Built once and shared: the adapters' warn-once set must outlive the
        // short-lived BukkitCombatant and PaperCombatWorld instances.
        this.adapters = new AdapterContext(scheduler, keys, visuals, statuses, elements, enchants, getLogger(), stats, anchorDrift, craftResults);

        // core takes a tick supplier, not Bukkit, so it stays unit-testable.
        this.cooldowns = new CooldownTracker(Bukkit::getCurrentTick);
        // THE PER-PLAYER CEILING: the base pool plus whatever Mana Bank the player is wearing.
        //
        // Scoped to DEFAULT_RESOURCE deliberately. The pool is keyed by (owner, resourceId) and
        // promises "mana, and whatever else content asks for", so a resolver that ignored the id
        // would raise the ceiling on every future resource at once.
        //
        // TOTAL by construction: maxManaBonusValue returns 0.0 for an untracked combatant rather
        // than throwing the way max(id) does, so this can be called from inside a cast -- on
        // whatever thread is casting, for a mob firing a costed trigger -- without a tracks() guard.
        //
        // MAX_MANA stays the BASE and stays here, because NEXT.md records it becoming archetype
        // content later; the stat holds only the part gear contributes.
        //
        // THE PER-PLAYER RATE joins it in Stats Slice 2, same shape and same scoping. The stat is in
        // mana per SECOND (matching Health Regen, and matching what a player reads), so the bonus is
        // converted and ADDED TO THE PER-TICK BASE rather than the sum being converted -- see
        // MANA_PER_TICK above for the ULP that makes those two different numbers. With no bonus,
        // ManaRegen.perTick(0.0) is exactly 0.0 and x + 0.0 == x, so an unenchanted player's rate is
        // bit-for-bit what shipped before this slice.
        this.resources = new ResourcePool(Bukkit::getCurrentTick,
                (owner, resourceId) -> ResourceCost.DEFAULT_RESOURCE.equals(resourceId)
                        ? MAX_MANA + stats.maxManaBonusValue(owner)
                        : MAX_MANA,
                (owner, resourceId) -> ResourceCost.DEFAULT_RESOURCE.equals(resourceId)
                        ? MANA_PER_TICK + ManaRegen.perTick(stats.manaRegenBonusValue(owner))
                        : MANA_PER_TICK);
        this.abilityService = new AbilityService(abilities, cooldowns, resources);
        // The reconcile loop pins the pool's current value when a max-mana modifier changes, so it
        // needs the pool. A second bind rather than a constructor param because the pool is built
        // AFTER the health system -- the same cycle-breaking two-step bind(stats) already uses.
        this.healthSystem.bindResources(resources);
        // A weapon trigger fires through the same cooldown/mana machinery, gate-free.
        this.weaponService = new WeaponService(abilityService);

        // The action-bar HUD reads both stores; it owns no state beyond its per-player loops.
        this.statsBar = new StatsBarSystem(scheduler, stats, resources);

        // Passive health regeneration: its own per-player loop, on its own clock. See the class
        // javadoc for why it is not folded into the reconcile loop that already visits everyone.
        this.healthRegen = new HealthRegenSystem(scheduler, stats);

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
                new RpgListeners(cooldowns, resources, profiles, weapons, shields, armor, tools, weaponService, adapters,
                        healthSystem, nameplates, statsBar, healthRegen), this);

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
                .registerListener(new WeaponSwingListener(adapters, weapons, weaponService, cooldowns));
        // Suppress vanilla's own crit particles, so a burst means OUR roll. Pure cancel on the
        // Netty thread -- no hop, no Bukkit. See the class for why a packet is justified here and
        // why it cannot eat our own spawnParticle burst.
        PacketEvents.getAPI().getEventManager()
                .registerListener(new VanillaCritParticleListener());

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(
                        RpgCommand.build(abilities, abilityService, adapters, kits, elements, profiles, weapons, shields, armor, tools, mobs, nameplates, resources),
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
     * A content {@code base_entity} name -> Bukkit EntityType, or null if it names nothing. Uses the
     * Registry rather than {@code EntityType.valueOf}, matching how the plugin resolves attributes,
     * potion effects and sounds -- and unlike valueOf it returns null instead of throwing, which is
     * what lets the validator report a typo by name instead of blowing up the boot.
     */
    private static EntityType entityType(String name) {
        if (name == null || name.isBlank()) return null;
        return Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
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
        // An enchant's icon is the one content field that fails INVISIBLY: a typo neither throws
        // nor skips the file, and the enchant works perfectly while rendering as the fallback book.
        // Material.matchMaterial is the same resolver WeaponItems uses, so the check and the render
        // cannot disagree about what counts as a material.
        problems.addAll(validator.validateEnchants(enchants.all(),
                name -> Material.matchMaterial(name) != null));
        // A mob's base_entity must name a real LIVING entity. Resolving that needs the Bukkit registry,
        // which is only reachable here -- so it arrives as predicates, like the potion/sound checks.
        // "arrow" is a real EntityType and would pass a mere existence check, then ClassCastException
        // at the first /rpg spawn; isAlive() is what turns that into a named boot warning instead.
        problems.addAll(validator.validateMobs(mobs.all(),
                name -> entityType(name) != null,
                name -> {
                    EntityType type = entityType(name);
                    return type != null && type.isAlive();
                }));
        // A craft_result must name a real DURABLE material and must equal its own material. Both
        // mistakes are completely silent in play -- one hands the player a different item than the
        // one they crafted, the other indexes cleanly and then never fires -- and boot is the only
        // moment the claim and the Bukkit registry are in the same JVM. Same argument, and the same
        // shape, as ArmorConsistency directly above.
        //
        // getMaxDurability() is why this is a predicate rather than a direct call: it throws
        // ExceptionInInitializerError with no server, which is what keeps the walk unit-testable.
        List<GearDefinition> claimants = new ArrayList<>();
        claimants.addAll(weapons.all());
        claimants.addAll(shields.all());
        claimants.addAll(armor.all());
        claimants.addAll(tools.all());
        problems.addAll(validator.validateCraftResults(claimants,
                name -> Material.matchMaterial(name) != null,
                name -> {
                    Material material = Material.matchMaterial(name);
                    return material != null && material.getMaxDurability() > 0;
                }));

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
        // FIRST, ahead of the profile flush: a menu can be holding a player's weapon, and nothing
        // else in the plugin gives it back. A weapon returned to an inventory has to be written
        // before the shutdown save runs, or it is saved out of existence.
        //
        // returnEverything() is called DIRECTLY rather than relying on closeInventory() still being
        // routed through our handlers while the plugin is disabling. It is idempotent, so the close
        // that follows -- and any InventoryCloseEvent it fires -- is free.
        //
        // Bukkit.getOnlinePlayers() is the right question here despite invariant 3: that invariant
        // is about PLAYER STATE, and "who on this node has a menu open right now" is not state a
        // repository could answer.
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu menu) {
                menu.returnEverything();
                player.closeInventory();
            }
        }

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

    public ShieldRegistry shields() { return shields; }
    public ToolRegistry tools() { return tools; }
    public ArmorRegistry armor() { return armor; }
    public CooldownTracker cooldowns() { return cooldowns; }
    public ResourcePool resources() { return resources; }
    public CombatantStats stats() { return stats; }
    public PlayerRepository repository() { return repository; }
    public ProfileService profiles() { return profiles; }

    /**
     * cast() only decides; the caller must run the returned effects on a region
     * thread. Today that is the region owning the caster's eye, which is not always
     * the one owning the impact. See AbilityService for why, and what it costs.
     */
    public AbilityService abilityService() { return abilityService; }

    /** Fires a weapon trigger through the shared cooldown/mana path, gate-free. */
    public WeaponService weaponService() { return weaponService; }
}
