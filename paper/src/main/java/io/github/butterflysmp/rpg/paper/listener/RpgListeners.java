package io.github.butterflysmp.rpg.paper.listener;

import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.combat.HitAccrual;
import io.github.butterflysmp.rpg.core.combat.CritState;
import io.github.butterflysmp.rpg.core.combat.DefenseRule;
import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.FireCadence;
import io.github.butterflysmp.rpg.core.combat.Ignite;
import io.github.butterflysmp.rpg.core.combat.DamageScale;
import io.github.butterflysmp.rpg.core.combat.DamageWindow;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.SweepShare;
import io.github.butterflysmp.rpg.core.combat.stat.HeartScale;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.combat.ShieldExchange;
import io.github.butterflysmp.rpg.core.enchant.Thorns;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import io.github.butterflysmp.rpg.core.weapon.ToolRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponService;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.persistence.PersistentDataType;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.PaperCombatWorld;
import io.github.butterflysmp.rpg.paper.adapter.ImmobilizePhysics;
import io.github.butterflysmp.rpg.paper.health.ArmorBarOverride;
import io.github.butterflysmp.rpg.paper.health.AttackSpeedAttributeOverride;
import io.github.butterflysmp.rpg.paper.health.MobNameplateManager;
import io.github.butterflysmp.rpg.paper.menu.CraftMatrixScreen;
import io.github.butterflysmp.rpg.paper.menu.CraftingMenu;
import io.github.butterflysmp.rpg.paper.menu.EnchantMenu;
import io.github.butterflysmp.rpg.paper.menu.GrindstoneMenu;
import io.github.butterflysmp.rpg.paper.menu.Menu;
import io.github.butterflysmp.rpg.paper.menu.MenuSafety;
import io.github.butterflysmp.rpg.paper.menu.NexusMenu;
import io.github.butterflysmp.rpg.core.recipe.RecipeRegistry;
import io.github.butterflysmp.rpg.paper.content.RecipeRegistrar;
import io.github.butterflysmp.rpg.paper.menu.RecipeCatalogue;
import io.github.butterflysmp.rpg.paper.menu.RecipeProbe;
import io.github.butterflysmp.rpg.paper.nexus.NexusCollisionNotice;
import io.github.butterflysmp.rpg.paper.nexus.NexusLock;
import io.github.butterflysmp.rpg.paper.nexus.NexusItems;
import io.github.butterflysmp.rpg.paper.nexus.NexusOpenGesture;
import io.github.butterflysmp.rpg.paper.nexus.NexusSlots;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import io.github.butterflysmp.rpg.paper.health.PlayerHealthSystem;
import io.github.butterflysmp.rpg.paper.hud.StatsBarSystem;
import io.github.butterflysmp.rpg.paper.health.HealthRegenSystem;
import io.github.butterflysmp.rpg.paper.health.VanillaDamagePolicy;
import io.github.butterflysmp.rpg.paper.health.VanillaHealPolicy;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.core.combat.AttackCharge;
import io.github.butterflysmp.rpg.paper.weapon.MeleeHits;
import io.github.butterflysmp.rpg.paper.weapon.PlumeDraw;
import io.github.butterflysmp.rpg.paper.weapon.WeaponFire;
import io.github.butterflysmp.rpg.paper.weapon.BrokenNotice;
import io.github.butterflysmp.rpg.paper.weapon.QuiverNotice;
import io.github.butterflysmp.rpg.paper.weapon.WeaponDurability;
import io.github.butterflysmp.rpg.paper.weapon.ShieldBlock;
import io.github.butterflysmp.rpg.paper.weapon.ShieldDurability;
import io.github.butterflysmp.rpg.paper.weapon.ShieldItems;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import io.github.butterflysmp.rpg.paper.weapon.GearRefresher;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * The single Bukkit Listener. Registered once, in RpgPlugin.
 *
 * Resist adding a second one. Every handler here should be a thin adapter that
 * hands the event to something that does the actual work; the logic belongs there.
 */
public final class RpgListeners implements Listener {

    /**
     * The vanilla damage a ridden melee swing is capped to: enough for the mob to react (red flash,
     * hurt sound, i-frames), too small to matter mechanically -- the real number is custom HP.
     */
    private static final double TOKEN_DAMAGE = 0.01;

    /**
     * Where a tracked mob's vanilla health is floored so the token can't kill it (death is deferred).
     * The mob analog of the player heart floor; small, since vanilla health is a puppet display only.
     */
    private static final double VANILLA_LIVE_FLOOR = 1.0;

    private final CooldownTracker cooldowns;
    private final FireCadence fireCadence;
    private final ResourcePool resources;
    private final ProfileService profiles;
    private final WeaponRegistry weapons;
    private final ShieldRegistry shields;
    private final ArmorRegistry armor;
    private final ToolRegistry tools;
    private final WeaponService weaponService;
    private final AdapterContext adapters;

    /**
     * The recipe roster the browser pages through. ONE instance, for the whole server.
     *
     * <p>Built lazily on the first browser open and cached for the server lifetime. It lives here
     * rather than as a static field because CLAUDE.md's third architecture invariant rules out
     * static mutable singletons, and rather than on the menu because a per-menu instance would
     * rebuild the whole roster every time anyone opened a crafting table.
     */
    private final RecipeCatalogue recipeCatalogue;

    /**
     * The one {@link RecipeCatalogue} on this server.
     *
     * <h2>*** SHARED, NOT SHAREABLE. A SECOND INSTANCE IS A SECOND CACHE. ***</h2>
     *
     * This class builds it and every hub route reaches the crafting screen through here, so it was
     * private until {@code /menu} needed to open the hub without going through a listener at all.
     *
     * <p><b>The obvious alternative -- {@code new RecipeCatalogue(adapters)} in {@code RpgPlugin}
     * for the command -- is wrong, and quietly.</b> This class caches for the server's lifetime by
     * design (see {@code onRecipeChange}'s javadoc on why invalidation is NOT wanted). Two
     * instances would be two caches warming independently, and they would agree right up until one
     * of them had seen a recipe the other had not -- which presents as a hub route showing a
     * different catalogue from a block route, months later, with no test in a position to see it.
     *
     * <p>Exposed rather than hoisted into {@code RpgPlugin} because <b>this class's constructor is
     * where its lifetime is already decided</b>, and moving ownership would widen a constructor
     * that is already fifteen parameters wide for the sake of one reader.
     */
    public RecipeCatalogue recipeCatalogue() {
        return recipeCatalogue;
    }

    /** Held for {@link #onResourcesReloaded}: re-registering recipes needs the plugin and the content. */
    private final Plugin plugin;
    private final RecipeRegistry recipes;
    private final PlayerHealthSystem healthSystem;
    private final MobNameplateManager nameplates;
    private final StatsBarSystem statsBar;
    private final HealthRegenSystem healthRegen;

    /**
     * Timing state for the vanilla-driven basic melee hit: the pending swing's charge, and the
     * per-victim window that stops a rising charge from landing five full hits inside one set of
     * i-frames. Owned here rather than injected because it is listener-scoped -- the two events it
     * bridges are both on this class, and nothing else in the plugin has a use for it.
     */
    private final MeleeHits meleeHits = new MeleeHits(Bukkit::getCurrentTick);

    /**
     * The Dragon's Plume's draw: the charge tracker, its rising tick, and the release that suppresses
     * vanilla's shot. Slice H1 -- it FIRES NOTHING, deliberately; see {@link PlumeDraw}.
     *
     * <p><b>LISTENER-SCOPED AGAIN, AND IT WAS HOISTED FOR EXACTLY ONE READER THAT NO LONGER
     * EXISTS.</b> It moved to {@code RpgPlugin} the day {@code /rpg drawsound} was written, because
     * the command mutated the tick's sound key and this class read it, so the two had to hold the
     * SAME instance. <b>That command was deleted on its own trigger when the sound was ruled</b>
     * ({@link PlumeDraw#TICK_SOUND}), leaving one holder -- so it is built here, beside
     * {@link #meleeHits} and {@code damageWindow}, for their reason: the events it bridges are all
     * on this class and nothing else in the plugin has a use for it.
     *
     * <p>Assigned in the CONSTRUCTOR rather than as a field initialiser, for {@code hijackedBlocks}'
     * reason: it closes over {@code weapons} and {@code adapters}, which a field initialiser would
     * read before the constructor body assigns them.
     */
    private final PlumeDraw plumeDraw;

    /**
     * The cadence for environmental damage, owned because the token destroys vanilla's ratchet.
     * Listener-scoped for MeleeHits' reason -- the one handler that claims from it is on this class.
     */
    private final DamageWindow damageWindow = new DamageWindow(Bukkit::getCurrentTick);

    /**
     * The blocks whose vanilla screen we replace outright, and what opens INSTEAD.
     *
     * <p><b>A table rather than a second hand-written if-block, because this rule has a documented
     * history of being got wrong.</b> The enchanting table's own comment records that its cancel
     * once sat inside the {@code !isSneaking} guard, so a sneak-right-click skipped the block
     * entirely, nothing cancelled the event, and the vanilla enchanting screen opened -- the one
     * screen the hijack exists to replace. Writing that shape a second time by hand is a bet that
     * any future correction lands in both copies. Here there is one copy, in
     * {@link #openHijackedBlock}, and the third hijack is free: {@code Menu}'s own javadoc already
     * names the anvil and class-select screens as coming.
     *
     * <p>The opener takes the clicked block because the enchanting table needs it (bookshelf power
     * is frozen at open); the crafting table ignores it. One signature beats two.
     *
     * <p>Built in the CONSTRUCTOR rather than as a field initialiser: the openers close over
     * {@code weapons}, {@code adapters} and friends, and a field initialiser runs before the
     * constructor body assigns them, which definite-assignment analysis rejects outright.
     */
    private final Map<Material, BiFunction<Player, Block, Menu>> hijackedBlocks;

    public RpgListeners(CooldownTracker cooldowns, FireCadence fireCadence,
                        ResourcePool resources, ProfileService profiles,
                        WeaponRegistry weapons, ShieldRegistry shields, ArmorRegistry armor,
                        ToolRegistry tools,
                        WeaponService weaponService,
                        AdapterContext adapters,
                        PlayerHealthSystem healthSystem, MobNameplateManager nameplates,
                        StatsBarSystem statsBar, HealthRegenSystem healthRegen,
                        Plugin plugin, RecipeRegistry recipes) {
        this.plugin = plugin;
        this.recipes = recipes;
        this.cooldowns = cooldowns;
        this.fireCadence = fireCadence;
        this.resources = resources;
        this.profiles = profiles;
        this.weapons = weapons;
        this.shields = shields;
        this.armor = armor;
        this.tools = tools;
        this.weaponService = weaponService;
        this.adapters = adapters;
        this.plumeDraw = new PlumeDraw(weapons, adapters, weaponService, cooldowns, fireCadence);
        this.recipeCatalogue = new RecipeCatalogue(adapters);
        this.healthSystem = healthSystem;
        this.nameplates = nameplates;
        this.statsBar = statsBar;
        this.healthRegen = healthRegen;

        this.hijackedBlocks = Map.of(
                Material.ENCHANTING_TABLE,
                (player, block) -> new EnchantMenu(player, weapons, shields, armor, tools, adapters, block),
                Material.CRAFTING_TABLE,
                (player, block) -> new CraftingMenu(player, adapters, recipeCatalogue),
                // THE GRINDSTONE IS ONE MAP ENTRY, AND THE SNEAK-GUARD BUG CANNOT REPRODUCE HERE.
                // openHijackedBlock cancels UNCONDITIONALLY, above the isSneaking check -- the fix
                // PLAN-enchant-table-ui rows 4b/4c paid for. A grindstone route written as its own
                // listener method would have had to re-derive that ordering; written as a table
                // entry it inherits it and cannot get it wrong.
                Material.GRINDSTONE,
                (player, block) -> new GrindstoneMenu(player, weapons, shields, armor, tools,
                        adapters));
    }

    /**
     * A datapack reload wipes every recipe we registered. Put them back.
     *
     * <h2>THE DEFECT THIS CLOSES, OBSERVED 2026-09-04 AS GATE ROW R2</h2>
     *
     * Vanilla {@code /reload} rebuilds the server's recipe manager and does <b>NOT</b> re-enable
     * plugins. {@code onEnable} is the only thing that calls {@code registerAll}, so before this
     * handler existed the Flint Staff's recipe was <b>silently gone until the next restart</b>: the
     * craft simply stopped working, nothing logged, nothing warned.
     *
     * <p><b>Scoped to the REGISTRAR, not to the content.</b> This is not a property of the Flint
     * Staff -- it is a property of anything registered into the recipe manager at enable time on
     * this build, so the next mechanism that registers something meets it too. Fixing it here means
     * that mechanism inherits the fix instead of rediscovering the bug.
     *
     * <h2>Why this ships WITH the mechanism rather than as its own slice</h2>
     *
     * Not "never ship a known defect" -- this slice deliberately ships one, and says so: scorch is
     * {@code kind: fire}, so the Flint Staff's burn is vanilla-rated and does not credit the caster.
     * That gap is <b>STATED</b>: written in the content file, filed as a named debt, explainable to
     * a player who notices.
     *
     * <p>This one was <b>SILENT</b>. And the alternative to fixing it was a caveat carried on every
     * remaining gate row -- "never run this after a /reload" -- which is the decisive argument:
     * <b>a caveat that must be remembered on every row is one that gets forgotten.</b> That is the
     * {@code continue}-inside-the-loop against {@code STATUS_SLOTS}, and the memory version was
     * watched to fail this same week: the stale-jar trap was disarmed by {@code set -e} happening
     * to fire, not by anyone remembering it was armed.
     *
     * <p><b>Re-entry is safe because the registrar was already written for it</b> -- remove-then-add
     * unconditionally, so a key that survived and a key that vanished take the same path. That was
     * written to avoid depending on an undocumented answer, and it is what makes this handler three
     * lines instead of a redesign.
     *
     * <h2>DO NOT ADD A CATALOGUE INVALIDATION HERE</h2>
     *
     * It is the reflexive move and it is wrong. {@code RecipeCatalogue} caches for the server's
     * lifetime and holds key, tier and ingredients -- none of which a drop-and-re-add under the SAME
     * key changes -- and {@code RecipeCatalogue.resolve} re-checks the live roster on every click
     * regardless. Invalidating would throw away a correct cache to fix nothing.
     */
    /**
     * A HOPPER MAY NOT EAT ONE OF OUR MARKERS. Without this, it duplicates items into the economy.
     *
     * <p>A marker -- a thrown ember, a projectile's rendered body -- is a REAL item stack that
     * nobody paid for. Three things collect items and each has to be told separately:
     * {@code setCanPlayerPickup(false)} and {@code setCanMobPickup(false)} handle two of them on the
     * entity, in {@code PaperCombatWorld.configureMarker}. <b>A hopper consults neither.</b>
     * {@code HopperBlockEntity.getItemsAtAndAbove} filters only on {@code ENTITY_STILL_ALIVE}, and
     * {@code addItem(Container, ItemEntity)} copies the stack in and discards the entity -- there is
     * no pickup-delay check anywhere on that path. So the third collector is refused here, at the
     * only place it can be.
     *
     * <p><b>This fixes a PRE-EXISTING defect, surfaced by a gate row rather than introduced by the
     * change that found it.</b> {@code throw_embers} has thrown real blaze powder through the same
     * shared configuration since it shipped, and an ember is an EASIER hopper target than a bolt,
     * not a harder one: it lands and rests for its whole fuse, while a bolt is only ever passing
     * through.
     *
     * <p>Gated on the entity tag, never on the material, so a genuine flint or blaze-powder drop a
     * player earned is collected exactly as vanilla intends. The tag is on the ENTITY and not the
     * stack for the same reason: the stack must stay ordinary, or a marker that somehow was
     * collected would put a tagged item into circulation.
     *
     * <p>Covers hopper minecarts as well as hoppers -- both reach an inventory through this one
     * event, which is why it is cancelled here rather than in a block-specific handler.
     */
    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (event.getItem().getPersistentDataContainer()
                .has(adapters.keys().markerEntity, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    /**
     * THE ARROW BODY MUST NEVER BE PICKED UP, AND THIS IS THE DETECTOR RATHER THAN THE FIX.
     *
     * <p>The fix is {@code PaperCombatWorld.spawnBoltMarker}'s
     * {@code setPickupStatus(DISALLOWED)}. With it, {@code AbstractArrow.playerTouch} refuses
     * before this event is ever raised, so <b>this handler should never fire</b>.
     *
     * <p><b>It is kept, and it is LOUD, because the thing it guards reads REDUNDANT and is not.</b>
     * {@code playerTouch}'s guard is {@code isInGround() OR isNoPhysics()} -- and the body runs with
     * {@code noPhysics} on, which satisfies that disjunction IN MID-AIR. An ordinary flying arrow is
     * unpickable because it is neither; ours is pickable-in-principle for exactly the reason it
     * passes through walls. Delete the {@code DISALLOWED} call as tidy-up and every player who walks
     * through a bolt mints a free arrow.
     *
     * <p><b>A silent cancel would hide exactly the failure it exists for</b> -- same argument, same
     * shape, as {@code PlumeDraw.suppressManagedBowShot}. A guard that logs when it fires cannot be
     * hollow: its firing IS the detector.
     */
    @EventHandler
    public void onPlumeBodyPickup(PlayerPickupArrowEvent event) {
        if (!event.getArrow().getPersistentDataContainer()
                .has(adapters.keys().markerEntity, PersistentDataType.BYTE)) {
            return;
        }
        event.setCancelled(true);
        adapters.log().warning(
                "[plume] PlayerPickupArrowEvent fired for a MARKER BODY, picked up by "
                        + event.getPlayer().getName() + " -- setPickupStatus(DISALLOWED) DID NOT"
                        + " TAKE. playerTouch's guard is `isInGround() OR isNoPhysics()`, and the"
                        + " body runs noPhysics, so nothing else refuses this. The pickup is"
                        + " cancelled here, but a free arrow was one call away from entering the"
                        + " economy -- see PaperCombatWorld.spawnBoltMarker.");
    }

    /**
     * THE ARROW BODY MUST NEVER DEAL DAMAGE, AND ITS FIRING IS THE SIGNAL.
     *
     * <p>{@code castRay} owns every hit in this engine. The body is decoration driven along the
     * segment the ray already traced, so a hit FROM it would be a second, competing resolution --
     * damage the ability never authored, on a target the flight may not have chosen.
     *
     * <p>With {@code setNoPhysics(true)} there is no route to it at all: {@code stepMoveAndHit} is
     * gated on {@code !noPhysics} and is the only caller of {@code findHitEntities} and
     * {@code hitTargetsOrDeflectSelf}. <b>So if this fires, the switch did not take</b> -- which
     * also means the body is colliding with blocks and sticking in them, and the whole visual
     * contract is broken rather than just this one hit.
     *
     * <p>Cancelled AND logged, for {@code suppressManagedBowShot}'s reason: a silent cancel would
     * leave a body that collides with the world and nobody would ever learn why the bolts stopped
     * mid-air.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlumeBodyDamage(EntityDamageByEntityEvent event) {
        if (!event.getDamager().getPersistentDataContainer()
                .has(adapters.keys().markerEntity, PersistentDataType.BYTE)) {
            return;
        }
        event.setCancelled(true);
        adapters.log().warning(
                "[plume] A MARKER BODY dealt damage to " + event.getEntity().getType()
                        + " -- setNoPhysics(true) DID NOT TAKE. stepMoveAndHit is the only route to"
                        + " findHitEntities and it is gated on !noPhysics, so this body is also"
                        + " colliding with blocks and sticking in them. The damage is cancelled"
                        + " here; castRay owns every hit -- see PaperCombatWorld.spawnBoltMarker.");
    }

    @EventHandler
    public void onResourcesReloaded(ServerResourcesReloadedEvent event) {
        RecipeRegistrar.Report report = RecipeRegistrar.registerAll(
                plugin, recipes.all(), adapters.craftResults(), plugin.getLogger());

        // Logged in the SAME shape as the boot line, because that line is the gate's instrument and
        // a second one is now producible by this route. `replaced` is the CROSS-CHECK on R2's
        // finding: 0 means removeRecipe found nothing, so the reload really had dropped the recipe;
        // 1 would mean the key was still there and R2's failure was something else -- a
        // contradiction to resolve, not a pass.
        plugin.getLogger().info("Custom recipes: " + report.registered() + " registered, "
                + report.replaced() + " replaced, " + report.refused() + " refused, of "
                + report.authored() + " authored (re-registered after a " + event.getCause()
                + " resource reload)");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Rebuild every carried weapon's display from current content. FIRST, so nothing downstream
        // reads a stale item -- though nothing does today: the stat reconcile loop sources attack
        // damage from the DEFINITION, not the item, which is why only the display was ever stale.
        // Content reloads only on restart and a dev restart reconnects you, so this one handler
        // covers both the stale emberblade you come back to and the live player logging in after
        // a content update. Already on the joining player's own thread; no scheduler hop needed.
        GearRefresher.refresh(event.getPlayer(), weapons, shields, armor, tools, adapters);
        // Returns immediately; the read happens on the storage I/O thread.
        profiles.onJoin(event.getPlayer().getUniqueId());
        // Register custom health at base 100, render the heart bar, and start the equip reconcile loop.
        healthSystem.onJoin(event.getPlayer());
        // Start this viewer's per-viewer mob-nameplate LOS loop.
        nameplates.onViewerJoin(event.getPlayer());
        // Start this player's action-bar stats line.
        statsBar.onJoin(event.getPlayer());
        healthRegen.onJoin(event.getPlayer());        // start the passive regeneration loop
        // Exactly one Nexus star, in its locked slot. CONVERGES rather than mints-if-absent: the
        // refusals keep a correct state correct and cannot repair a wrong one, and a star stranded
        // in the backpack would be welded there by the same rule that guards the locked slot.
        // Never destroys a player's item -- the displaced occupant goes through MenuSafety.give.
        //
        // *** THIS WAITS FOR THE PROFILE, AND CALLING IT HERE WOULD BE WRONG. ***
        //
        // The locked slot is per-player and lives in the profile, which profiles.onJoin started
        // reading from disk twelve lines above -- ASYNCHRONOUSLY. A converge on this line reads
        // Optional.empty() on essentially every join, falls back to the default, and places the
        // star at 8 for a player whose slot is 3. The profile then lands carrying 3, and from that
        // moment the two halves disagree: the star sits in 8 while the lock protects 3, so an
        // ordinary hotbar cell is inert for the session with nothing said -- and ONLY for players
        // who changed the setting, which is the population least likely to be tested.
        //
        // whenSettled runs once the load succeeds OR fails, so "empty" there means no stored
        // preference PERMANENTLY rather than "not yet", and the default is then the right answer.
        // It runs on the storage I/O thread, hence the hop: writing an inventory off that thread
        // is the same violation as touching Bukkit from a packet callback.
        // adapters.scheduler() rather than a new constructor parameter: AdapterContext already
        // carries it, this class already holds an AdapterContext, and widening a 16-argument
        // constructor to reach something already in scope is the worse trade.
        Player joined = event.getPlayer();
        profiles.whenSettled(joined.getUniqueId(), profile -> adapters.scheduler().onEntity(joined, () -> {
            // The load can settle after they have left -- a fast join/quit, or a slow disk.
            if (!joined.isOnline()) return;
            // *** A SWITCHED-OFF STAR MUST NOT BE RE-MINTED. *** converge MINTS when it finds none,
            // so without this guard the toggle would last exactly until the player's next join and
            // read as the setting not sticking. ABSENT PROFILE READS AS ON -- the shipped default,
            // and the direction that cannot strip a star from someone whose disk read failed.
            if (!profile.map(PlayerProfile::starEnabled).orElse(true)) return;
            NexusSlots.converge(joined, adapters.keys(),
                    profile.map(PlayerProfile::nexusSlot).orElse(NexusLock.DEFAULT_LOCKED_SLOT));
        }));
    }

    /**
     * A mob appeared (spawn OR chunk-load, both funnel here) -- bootstrap its custom HP from vanilla max
     * and cache its nameplate, on the entity's own thread. Dispatch-only; the manager filters armor
     * stands / opt-outs and does the work.
     */
    @EventHandler
    public void onEntityAdd(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof LivingEntity mob && !(mob instanceof Player)) {
            nameplates.onMobAppear(mob);
        }
    }

    /**
     * A mob was removed (death, despawn, chunk-unload) -- drop its nameplate and custom-health state so
     * neither leaks past the entity. Mirrors onQuit for players.
     */
    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof LivingEntity mob && !(mob instanceof Player)) {
            nameplates.onMobRemove(mob.getUniqueId());
            // And its melee window, or the map grows for the lifetime of the server.
            meleeHits.forget(mob.getUniqueId());
            damageWindow.forget(mob.getUniqueId());   // and its environmental window, same reason
            // ...and its burn, or the task outlives the mob.
            //
            // END TWO OF A TWO-ENDED COUPLING -- see onEntityDeath, which ALSO calls forget on this
            // id, earlier, as Ignite's once-ness guard. THIS CALL IS NOT REDUNDANT AND MUST NOT BE
            // DELETED AS SUCH: it covers every removal that is not a death (despawn, chunk-unload)
            // and every death of an UNSCORCHED mob, neither of which onEntityDeath forgets. When a
            // scorched mob dies, the death path has already forgotten it and this call is a no-op --
            // that is expected, and ScorchStatus.forget tolerates it (pinned by
            // ScorchStatusTest.forgettingTwiceIsANoOp).
            adapters.scorch().forget(mob.getUniqueId());
        }
    }

    /**
     * A mob died -- if it was scorched, it IGNITES: an explosion a short fuse later, damaging nearby
     * mobs, credited to whoever lit the fire.
     *
     * <p><b>THE TRIGGER IS THE OPERATOR'S RULING: ANY MOB THAT DIES WHILE SCORCHED IGNITES.</b>
     * Binary -- no stack count -- and the killing blow need not be a Scorched attack, which is why
     * this hangs off {@code EntityDeathEvent} rather than the {@code reachedZero} health seam. A mob
     * killed by {@code VOID} or {@code KILL} is PASSed by {@code VanillaDamagePolicy} and never
     * reaches zero custom HP, so {@code MobDeathSystem} never fires for it; the event covers both
     * paths and the seam covers one. Hooking the seam would silently skip void- and /kill-ed mobs.
     *
     * <h2>EVERYTHING IS CAPTURED HERE, ON THE DEATH FRAME. NOTHING IS READ AT DETONATION.</h2>
     *
     * The fuse outlives the scorch: {@code forget} runs below and again on removal, and the entity
     * itself is gone before the blast lands. A blast that read {@code applier(id)} when it fired
     * would get <b>null</b> -- and would then damage, kill, chain and <b>credit nobody</b>, with a
     * death message the only place it ever showed. So the applier and the position are read now and
     * handed to {@link Ignite#detonate} as values.
     *
     * <h2>THE forget CALL IS THE ONCE-NESS GUARD, AND IT IS DOING TWO JOBS</h2>
     *
     * <b>{@code EntityDeathEvent} does NOT guarantee it fires once per entity.</b> Measured from the
     * pinned API jar rather than assumed: the event is {@code Cancellable} and carries
     * {@code setReviveHealth}, whose own javadoc describes the health an entity revives with <i>after
     * cancelling the event</i> -- so a cancelled death revives the same mob, which can die again and
     * fire again. Any plugin can do that. {@code MobDeathSystem}'s {@code isDead()} guard does not
     * transfer here either: inside a death handler the entity is dying by definition.
     *
     * <p>So once-ness is BUILT, not inherited -- by reading the scorch and immediately forgetting it.
     * A second delivery finds nothing scorched and returns. That reuses state which already exists
     * rather than adding a second map with its own lifetime to leak, which is the shape this repo has
     * refused before ({@code MeleeHits} derives from a window stamp so that "there is nothing to
     * expire, nothing for forget to miss").
     *
     * <p><b>END ONE OF A TWO-ENDED COUPLING.</b> {@code onEntityRemove} also forgets this id. Neither
     * call is redundant -- see the note there -- and deleting this one removes the guard while
     * leaving a suite that still passes.
     *
     * <h2>AND THE ORDERING THIS DEPENDS ON</h2>
     *
     * It works only because scorch is <b>still live when this runs</b>:
     * {@code MobDeathSystem.setHealth(0)} fires {@code EntityDeathEvent}, and {@code onEntityRemove}
     * rides {@code EntityRemoveFromWorldEvent}, which is strictly after. <b>If that order ever
     * inverts, {@code isScorched} is already false on entry and Ignite fires NEVER</b> -- which is
     * indistinguishable from "no mob happened to be scorched", so nothing would report it. That is
     * what {@code GATE-ignite.md} exists to witness.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        if (mob instanceof Player) return;   // nothing in the game can scorch a player

        UUID id = mob.getUniqueId();
        if (!adapters.scorch().isScorched(id)) return;

        UUID applier = adapters.scorch().applier(id);
        // THE THIRD CAPTURE, AND IT MUST BE HERE FOR THE SAME REASON AS THE OTHER TWO. forget()
        // below drops this entry, so a depth read at detonation returns 0 -- and 0 means "a player
        // caused this", so EVERY LINK WOULD DETONATE AT DEPTH 1 AND THE CHAIN LIMIT WOULD NEVER
        // ENGAGE. That failure presents as an unbounded cascade, i.e. as "the limit doesn't work",
        // with nothing pointing back at the order of these three lines.
        int depth = adapters.scorch().depth(id);
        Location at = mob.getLocation();
        adapters.scorch().forget(id);        // the guard -- see the javadoc above

        // depth + 1: this mob was scorched BY a blast at `depth`, so its own ignition is the next
        // link down. A dev application and a player's weapon both store 0, so both detonate at 1.
        Ignite.detonate(new PaperCombatWorld(mob.getWorld(), adapters),
                new Vec3(at.getX(), at.getY(), at.getZ()), applier, id, depth + 1);
    }

    /**
     * A right-click fires the held weapon's right_click trigger -- the costed special.
     * PlayerInteractEvent (unlike the left-click packet path) is reliable for right-click
     * and already runs on the region thread, so this reads the held item and cancels vanilla
     * directly, with no Netty hop and no held-weapon cache.
     *
     * This handler is DISPATCH-ONLY. It decides whether this is a right-click we handle --
     * main hand, air or block -- then hands off to WeaponFire, whose fire() owns the
     * check-spend-commit atomically. It never checks cost or cooldown itself; doing so would
     * reopen the check-then-fire window a fast right-click double-spends through.
     *
     * event.getHand() == HAND is the FIRST branch, and it is load-bearing: PlayerInteractEvent
     * can fire twice for one physical right-click (the main/off-hand pair), and an unfiltered
     * handler would spend mana twice for one press.
     *
     * Vanilla is cancelled when the held weapon actually binds right_click (attempt returns
     * present). ironblade has no right_click, so its right-click passes through and doors and
     * chests still work with it in hand; only a weapon that uses the input consumes it.
     *
     * There are now TWO exceptions to that fall-through, and they are cancelled for different
     * reasons -- one keyed on WHAT IS HELD, the other on WHAT IS CLICKED:
     *
     * <ul>
     *   <li><b>The NEXUS STAR, checked FIRST.</b> Holding it, a right-click opens the hub and is
     *       cancelled unconditionally, whether it landed on air or on a block. It precedes the
     *       hijacked blocks deliberately: a crafting table right-clicked with the star in hand
     *       opens the NEXUS, because the item in the player's hand is what they pressed.
     *   <li><b>The HIJACKED BLOCKS</b> -- today an enchanting table and a crafting table, listed in
     *       {@link #hijackedBlocks}. Each is cancelled unconditionally whatever is held and whether
     *       or not you are sneaking, because our menu replaces that block's vanilla screen outright
     *       and it must never open. See {@link #openHijackedBlock}.
     * </ul>
     *
     * <p><b>The star's branch has no sneak escape hatch and does not need one.</b> The hijacked
     * blocks have one so a Mage can still cast while standing at an enchanting table; the star
     * binds no {@code right_click} and there is no cast to preserve.
     */
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return; // FIRST: main hand only, or one click double-spends
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        // THE NEXUS STAR OPENS THE HUB, AND IT WINS OVER EVERYTHING BELOW.
        //
        // Placed here -- after the two guards above, before the hijacked blocks and before
        // WeaponFire -- because all three of those would otherwise fire on the same press:
        //
        //   ahead of openHijackedBlock  so right-clicking a crafting table while holding the star
        //                               opens the NEXUS, not the crafting menu. Two menus cannot
        //                               both open; the item in the player's hand is what they
        //                               pressed, so it decides.
        //   ahead of WeaponFire.attempt so the hub can never cost mana. The star binds no
        //                               right_click and attempt would return empty, but the
        //                               ordering is the guarantee rather than the coincidence.
        //
        // IT IS CANCELLED UNCONDITIONALLY, and that is not tidiness. Without it, right-clicking a
        // block while holding the star ALSO does whatever that block does -- a chest opens behind
        // our menu, a button presses, a nether star is placed into an item frame. The menu would
        // look right and the world would have changed underneath it.
        //
        // KEYED BY keys.nexus THROUGH NexusItems.isNexus, NEVER BY MATERIAL.
        // HealthModifierItems.mint still mints a plain NETHER_STAR for health_boost_TEMP, and a
        // Material check here would make that dev item open the hub -- row 7 of GATE-nexus.md is
        // the same collision from the other side.
        //
        // OPENING AN INVENTORY DIRECTLY FROM AN EVENT HANDLER IS FINE HERE, AND THE CEREMONY
        // CraftingMenu USES DOES NOT APPLY. That close-then-hop-through-the-scheduler dance exists
        // for MENU-TO-MENU navigation, where a container is already open and openInventory during
        // the close would race. No container is open on a right-click in the world, so there is
        // nothing to close and nothing to hop for. Do not copy the dance in.
        if (NexusItems.isNexus(event.getPlayer().getInventory().getItemInMainHand(),
                adapters.keys())) {
            event.setCancelled(true);

            // THE COLLISION SPEAKS; THE ORDINARY OPEN DOES NOT. Scoped to the shadowed block and
            // NOT to the open, because the usual way to reach the hub is right-clicking AIR and a
            // line of chat every time a player opens their menu is MenuSafety's "no message
            // repeated sixty-four times helps" objection, earned every session.
            //
            // hijackedBlocks.containsKey IS A MAP LOOKUP WITH NO SIDE EFFECT, and that is why it is
            // asked rather than openHijackedBlock being called to find out: that method CANCELS and
            // OPENS, so using it as a predicate would open the very screen being shadowed.
            //
            // Why this speaks at all when the Nexus lock is silent: the lock refuses a GESTURE and
            // the player can SEE the star did not move, so chat would repeat what is on screen.
            // This SHADOWS something else, and the evidence is the thing that did NOT happen --
            // nothing to see, so something to say. Full boundary in NexusCollisionNotice's javadoc.
            Block clicked = event.getClickedBlock();
            if (action == Action.RIGHT_CLICK_BLOCK && clicked != null
                    && hijackedBlocks.containsKey(clicked.getType())) {
                NexusCollisionNotice.shadowedBlock(event.getPlayer(), cooldowns);
            }

            new NexusMenu(event.getPlayer(), adapters, profiles, weapons, resources,
                    recipeCatalogue, shields, armor, tools).open();
            return;
        }

        // THE VANILLA SCREEN NEVER OPENS ON A HIJACKED BLOCK, sneaking or not. Our menus replace
        // those screens outright, so suppressing them is unconditional and sneaking only decides
        // what happens INSTEAD.
        //
        // Ahead of WeaponFire.attempt deliberately, so the weapon never spends mana on a click
        // that opened a menu.
        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && openHijackedBlock(event)) {
            return;
        }

        WeaponFire.attempt(event.getPlayer(), "right_click", weapons, weaponService, adapters,
                        cooldowns, fireCadence)
                .ifPresent(result -> {
                    // Present == this weapon binds right_click. Suppress the vanilla interaction
                    // whether the special fired or was refused -- the player pressed the special.
                    event.setCancelled(true);

                    // BROKEN is handled BEFORE the basic-attack silence below, and that ordering is
                    // the whole reason the bow reports at all: hunters_bow's shot is a
                    // weapon_damage basic attack, so firesABasicAttack returns early and the switch
                    // is never reached. A broken weapon must always say so -- doing nothing without
                    // an explanation reads as a bug -- so it bypasses the silence and relies on
                    // BrokenNotice's throttle to keep held input from spamming chat.
                    if (result instanceof CastResult.Broken) {
                        BrokenNotice.notify(event.getPlayer(), cooldowns);
                        return;
                    }

                    // THE QUIVER REFUSALS RIDE THE SAME BYPASS, AND FOR THE SAME REASON THE COMMENT
                    // ABOVE GIVES. A quiver weapon's shot is bound to right_click and will usually
                    // BE a basic attack, so firesABasicAttack below returns early and the switch is
                    // never reached. An empty magazine that says nothing is indistinguishable from a
                    // bug -- which is the argument BrokenNotice was written on -- so these bypass the
                    // silence too and lean on their own throttles to survive held input.
                    if (result instanceof CastResult.Empty) {
                        QuiverNotice.empty(event.getPlayer(), cooldowns);
                        return;
                    }
                    if (result instanceof CastResult.Reloading reloading) {
                        QuiverNotice.reloading(event.getPlayer(), cooldowns, reloading.ticksRemaining());
                        return;
                    }

                    // A deliberate press deserves feedback, unlike the silent left-click swing --
                    // EXCEPT when the right-click IS the basic attack. The bow's shot is bound to
                    // right_click only so that binding it suppresses the vanilla draw; mechanically
                    // it is a swing, and a player holding down fire is spamming an attack, not
                    // repeatedly deciding to cast something. Chatting at them once per rejected
                    // shot is the spam WeaponSwingListener.onSwing already refuses to produce for
                    // exactly the same reason. A costed special (emberblade's Fireball, the staff's
                    // bolt) is a real decision and keeps its feedback.
                    //
                    // The discriminator is DamagePayload.isBasicAttack -- the same question the
                    // tooltip and the cooldown scaler ask -- so a weapon cannot render as a stat
                    // block, swing at stat-block cadence, and then chat like an ability.
                    if (firesABasicAttack(event.getPlayer())) return;

                    switch (result) {
                        case CastResult.Success ignored -> { } // already executed inside attempt()
                        case CastResult.InsufficientResource lacking ->
                                event.getPlayer().sendMessage(Component.text(
                                        "Not enough %s: %.0f needed, %.0f available".formatted(
                                                lacking.resourceId(), lacking.required(), lacking.available()),
                                        NamedTextColor.GRAY));
                        case CastResult.OnCooldown onCooldown ->
                                event.getPlayer().sendMessage(Component.text(
                                        "On cooldown for %.1fs".formatted(onCooldown.ticksRemaining() / 20.0),
                                        NamedTextColor.GRAY));
                        // A weapon touches neither the ability registry nor the archetype gate,
                        // so these cannot occur -- but the switch stays exhaustive over CastResult.
                        case CastResult.UnknownAbility ignored -> { }
                        case CastResult.Locked ignored -> { }
                        // Handled above, ahead of the basic-attack silence, so the bow reports too.
                        case CastResult.Broken ignored -> { }
                        // Likewise -- both are handled before firesABasicAttack returns.
                        case CastResult.Empty ignored -> { }
                        case CastResult.Reloading ignored -> { }
                    }
                });

        // THE DRAGON'S PLUME'S DRAW STARTS HERE, AND ONLY FOR A WEAPON THAT BINDS NO right_click.
        //
        // Unconditional on purpose: PlumeDraw.onDrawStarted gates on the ITEM, and its gate excludes
        // every weapon whose vanilla interaction the block above just cancelled. A weapon that binds
        // right_click never reaches BowItem.use, so its draw never starts and there is nothing to
        // watch -- which is why hunters_bow, the only bow shipped today, is not a draw weapon.
        //
        // AFTER the attempt rather than before it, so that if a weapon ever binds both, the special
        // is dispatched first and the draw watcher declines on its own terms rather than racing it.
        plumeDraw.onDrawStarted(event.getPlayer());
    }

    /**
     * THE RELEASE OF A DRAWN PLUME -- and the event fires for EVERY item release on the server.
     *
     * <p>Eating, drinking, shields, crossbows, tridents, spyglasses: all of them arrive here. The
     * gate is on the ITEM, inside {@link PlumeDraw}, because an ungated {@code clearActiveItem()}
     * in this handler would break every one of those, silently, on a server where nobody is testing
     * the Plume.
     *
     * <p><b>{@code PlayerStopUsingItemEvent} is not cancellable and does not need to be.</b> It
     * fires BEFORE {@code LivingEntity.releaseUsingItem} re-reads its {@code useItem} field, so
     * clearing the active item from in here makes that re-read yield EMPTY and vanilla's whole
     * release -- {@code BowItem.releaseUsing}, {@code draw()}, {@code useAmmo} -- never runs. The
     * measurement is written up at {@link PlumeDraw}.
     */
    @EventHandler
    public void onStopUsingItem(PlayerStopUsingItemEvent event) {
        plumeDraw.onRelease(event.getPlayer(), event.getItem(), event.getTicksHeldFor());
    }

    /**
     * THE BELT-AND-BRACES GUARD FOR A SHOT THAT SHOULD BE IMPOSSIBLE.
     *
     * <p>If the release clear took, vanilla never reaches the code that fires this event for one of
     * our bows. <b>Its firing is therefore the signal</b> -- see
     * {@link PlumeDraw#suppressManagedBowShot}, which logs loudly rather than cancelling quietly,
     * because a silent cancel would hide exactly the failure the guard exists for.
     */
    @EventHandler
    public void onManagedBowShot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plumeDraw.suppressManagedBowShot(player, event.getBow())) {
            event.setCancelled(true);
        }
    }

    /**
     * Does the held weapon's right_click trigger deal the wielder's ATTACK_DAMAGE stat -- i.e. is
     * this press a basic attack rather than an ability? Absent weapon, absent binding, or a literal
     * payload all read as false, so the feedback stays on by default and only a genuine basic
     * attack is silenced.
     *
     * Re-resolves the held weapon rather than reading it off the CastResult, because only Success
     * carries the definition and the results being silenced are the refusals. Same tick, same
     * thread, immediately after WeaponFire.attempt looked it up, so the two cannot disagree.
     */
    private boolean firesABasicAttack(Player player) {
        return WeaponItems.heldWeaponId(player, adapters.keys())
                .flatMap(weapons::find)
                .flatMap(weapon -> weapon.trigger("right_click"))
                .map(binding -> DamagePayload.isBasicAttack(binding.ability().onHit()))
                .orElse(false);
    }

    /**
     * Suppress a hijacked block's vanilla screen and open ours instead.
     *
     * <p><b>THE CANCEL IS UNCONDITIONAL AND COMES FIRST</b>, before anything looks at sneaking.
     * That ordering is the whole point of this method existing once rather than twice. It used to
     * live inside the {@code !isSneaking} guard, which meant a sneak-right-click skipped the block
     * entirely, nothing cancelled the event, and the vanilla enchanting screen opened -- the one
     * screen the hijack exists to replace. Sneaking suppresses a container GUI only when you are
     * holding a PLACEABLE item; with an empty hand it does nothing at all, so that guard was
     * resting on a rule that does not exist.
     *
     * <p><b>The bill, stated rather than inherited.</b> The enchanting table's version of this note
     * waved the cost off with "a player who wants to build against one can break and re-place it",
     * which was written about a block a base has one of. Crafting tables are everywhere, and this
     * costs two real, permanent, player-facing things:
     *
     * <ul>
     *   <li>No block can be placed against any face of any crafting table or enchanting table.
     *   <li>The vanilla RECIPE BOOK is gone for 3x3 crafting entirely -- its search, its auto-fill
     *       and its "craftable now" filter do not exist in our menu. Until Quick Craft lands,
     *       players craft from memory.
     * </ul>
     *
     * <p>Unconditional is still right. The alternative is classifying which held items suppress a
     * block-entity GUI, and that list goes stale the first time Minecraft adds a placeable -- the
     * denylist defect again, in a place where being wrong opens the very screen we replaced.
     *
     * <p><b>The sneak path can be a silent dead click, and that is accepted rather than unnoticed.</b>
     * Sneaking with a weapon that binds no {@code right_click} leaves the event cancelled and
     * nothing happens, with no feedback. That is already true of the enchanting table and is the
     * price of the escape hatch that keeps a Mage able to cast while standing at one.
     *
     * @return true if a menu was opened and the caller should stop; false to fall through to
     *         {@code WeaponFire.attempt}, which is both the sneak escape hatch and the ordinary
     *         "this block is not ours" path.
     */
    private boolean openHijackedBlock(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        BiFunction<Player, Block, Menu> opener = hijackedBlocks.get(block.getType());
        if (opener == null) return false;

        event.setCancelled(true);

        // Sneaking: fall through to WeaponFire.attempt so the weapon's right_click still fires.
        // Vanilla is already cancelled above, so the block's own screen opens for neither of us.
        if (event.getPlayer().isSneaking()) return false;

        opener.apply(event.getPlayer(), block).open();
        return true;
    }

    /**
     * Route a click to the menu that owns the top inventory.
     *
     * DISPATCH-ONLY, and the routing lives in Menu because the rule it enforces has to be the same
     * for every menu that will ever exist. Menu.handleClick cancels FIRST, unconditionally, before
     * it looks at anything; a consumer never sees the event and so cannot un-cancel it.
     *
     * getHolder() IS the registry -- no map to keep in step, and identity that a renamed item or a
     * duplicated title cannot spoof. getView().getTopInventory() rather than getInventory(): the
     * same object today, but the explicit form stays right when read beside getClickedInventory().
     *
     * <p><b>ignoreCancelled = true IS NOT DECORATION, AND DELETING IT LOOKS LIKE A TIDY-UP.</b> It
     * is the half of the Nexus lock that lives in this annotation. {@link #onNexusClick} runs at
     * LOWEST and cancels; this runs at NORMAL, strictly after, and must then SKIP -- because
     * MenuRouting does not merely un-cancel own-inventory actions, it PERFORMS shiftMove,
     * hotbarMove, offhandMove and collectToCursor by calling setItem / setCurrentItem /
     * setItemOnCursor directly. Those are real writes. A cancel at any LATER priority arrives after
     * the star has already moved and undoes nothing, which is why the guard is below rather than
     * above.
     *
     * <p><b>What its deletion would look like:</b> the star still cannot be dropped with Q, still
     * cannot be picked up in a plain inventory screen, and every menu still opens. ONLY the
     * performed routes -- shift-click into the crafting grid, number key over a grid cell -- quietly
     * start working again, and every test stays green except {@code NexusWiringSignatureTest},
     * which exists for precisely this. Same arrangement as {@link #onMobMeleeAttack} and
     * {@link #onPlayerMeleeAttack}, whose javadoc states the property both rest on: same-priority
     * order is undefined, a strictly later priority is not.
     */
    @EventHandler(ignoreCancelled = true)
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu menu) {
            menu.handleClick(event);
        }
    }

    /**
     * A drag can place items into slots the click handler never sees. Same holder, same rule.
     *
     * <p><b>ignoreCancelled = true, for the reason spelled out on {@link #onMenuClick}</b> --
     * {@link #onNexusDrag} cancels at LOWEST and this must then skip. Pinned by
     * {@code NexusWiringSignatureTest}; deleting it is silent.
     */
    @EventHandler(ignoreCancelled = true)
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu menu) {
            menu.handleDrag(event);
        }
    }

    /**
     * Esc, the close button, death, a disconnect and shutdown ALL arrive here. One return path, so
     * the close button and the escape key cannot drift apart -- they are the same code.
     */
    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu menu) {
            menu.handleClose(event);
        }
    }

    // ------------------------------------------------------------------ the Nexus lock
    //
    // SIX HANDLERS, because the star can leave its slot by six independent families of event and
    // an inventory guard reaches only two of them. Each is a thin adapter: NexusSlots decides,
    // this executes. Refusal is SILENT -- no message, no sound. The player can see the star did
    // not move, and updateInventory() is what makes that true rather than merely intended.
    //
    // DELIBERATELY NOT on onMenuClose: InventoryCloseEvent is not Cancellable, so ignoreCancelled
    // there would be meaningless and a seventh handler would have nothing to cancel. Applying the
    // change uniformly across all three menu dispatchers is the obvious move and the wrong one.

    /**
     * The inventory half of the lock, and the reason {@link #onMenuClick} is ignoreCancelled.
     *
     * <p><b>LOWEST, and it must be strictly below the menu dispatcher rather than above it.</b>
     * MenuRouting PERFORMS its cross-inventory moves -- shiftMove and hotbarMove call
     * {@code setCurrentItem} / {@code setItem} directly -- so a cancel at HIGHEST would arrive
     * after the star had already been written into a crafting grid and would undo nothing. Running
     * first and letting the dispatcher skip is the only arrangement that reaches those two routes.
     * Both are live today: {@code CraftingMenu.acceptsInput} returns true unconditionally over a
     * STACKING grid, and a crafting table is one right-click away.
     *
     * <p>LOWEST -> NORMAL is guaranteed by Bukkit's priority contract, not by registration order.
     * Same-priority order is undefined, which is exactly the trap {@link #onPlayerSweepAttack}'s
     * javadoc records; a strictly lower priority has no coin toss in it.
     *
     * <p><b>If someone later moves the menu dispatcher to LOWEST</b>, the two tie, ordering becomes
     * undefined again and this silently stops working for the performed routes.
     * {@code NexusWiringSignatureTest} is what goes red.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onNexusClick(InventoryClickEvent event) {
        if (!NexusSlots.refuses(event, adapters.keys(), profiles)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        player.updateInventory();

        // THE SIDE EFFECT ON AN ALREADY-REFUSED GESTURE. The refusal above is untouched -- the star
        // does not move, and there is no chat line, sound or title. What is added is that ONE of the
        // gestures this method already refuses now also opens the hub.
        //
        // THE HOOK GOES WHERE THE REFUSAL ALREADY LIVES, and that is the design rather than a
        // convenience: the star's slot is the one cell in a player's inventory where a click is
        // already guaranteed to be intercepted, so nothing new has to reach in.
        //
        // THE FOUR BOOLEANS ARE DERIVED HERE AND THE RULE IS DECIDED IN NexusOpenGesture, which is
        // unit-testable without a server. Two of these predicates did not exist in this project
        // before -- "is the open SCREEN the own inventory" and "is the cursor empty" -- and both are
        // measured rather than approximated; see NexusSlots.isOwnInventoryScreen for the three
        // near-misses that do not answer the first.
        NexusLock.Touched touched = NexusSlots.touchedOf(event.getView(), event.getRawSlot());
        boolean starsOwnSlot = touched.playerInventory()
                && touched.index() == NexusSlots.lockedSlotOf(player, profiles)
                && NexusSlots.starAt(player, adapters.keys()).test(touched.index());

        if (!NexusOpenGesture.opensHub(event.getClick(),
                NexusSlots.isOwnInventoryScreen(event.getView()),
                starsOwnSlot,
                MenuSafety.isEmpty(event.getCursor()))) {
            return;
        }

        // A SCHEDULER HOP, NOT A DIRECT open(). The click event has not finished being processed
        // and the client has not applied the cancellation; opening inline is how a desynced client
        // ends up holding a ghost item. Menu.open's javadoc carries the measured rule --
        // EntityScheduler.run "schedules a task to execute on the next tick", read off the pinned
        // paper-api -- so onEntity is already the hop and onEntityLater(.., 1) would be the same.
        //
        // NO EXPLICIT closeInventory() FIRST. That is a separate question and it is about
        // returnEverything: the own-inventory screen is not one of our menus and holds no input
        // slots of ours, so openInventory's implicit close is sufficient.
        adapters.scheduler().onEntity(player, () -> new NexusMenu(player, adapters, profiles,
                weapons, resources, recipeCatalogue, shields, armor, tools).open());
    }

    /**
     * A drag never reaches {@link #onMenuDrag} when no menu is open, so this exists in its own
     * right rather than as a second copy of the click guard. Same reason {@code Menu.handleDrag}
     * is separate from {@code handleClick}: a drag places items into slots the click handler never
     * sees, and {@code getRawSlots()} is the only thing that enumerates them.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onNexusDrag(InventoryDragEvent event) {
        if (!NexusSlots.refuses(event, adapters.keys(), profiles)) return;
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) player.updateInventory();
    }

    /** Q and Ctrl-Q with no screen open. Not an inventory event; the lock above cannot see it. */
    @EventHandler
    public void onNexusDrop(PlayerDropItemEvent event) {
        if (!NexusItems.isNexus(event.getItemDrop().getItemStack(), adapters.keys())) return;
        event.setCancelled(true);
        event.getPlayer().updateInventory();
    }

    /**
     * F with no screen open. The in-screen F is a different event entirely and is handled by
     * {@link #onNexusClick} through ClickType.SWAP_OFFHAND.
     *
     * <p>Both directions are checked. Swapping the star OUT of the locked slot is the obvious one;
     * swapping something INTO it from the offhand is the other, and it is what would leave the
     * locked slot holding a stranger's item with the star in the offhand.
     */
    @EventHandler
    public void onNexusSwapHand(PlayerSwapHandItemsEvent event) {
        if (!NexusItems.isNexus(event.getMainHandItem(), adapters.keys())
                && !NexusItems.isNexus(event.getOffHandItem(), adapters.keys())) return;
        event.setCancelled(true);
        event.getPlayer().updateInventory();
    }

    /**
     * Right-clicking the held star onto an entity that TAKES it -- an item frame, a glow item
     * frame. The item leaves the hand with no inventory event of any kind, so nothing above sees it.
     *
     * <p><b>The entity type is deliberately NOT filtered.</b> An {@code instanceof ItemFrame} here
     * would read as a tightening and would be a hole: {@code PlayerInteractAtEntityEvent} declares
     * no HandlerList of its own and therefore arrives HERE, covering the whole at-entity family for
     * free. Refusing on "the player is holding the star" needs to know nothing about the target,
     * and slice 1 gives the star no right-click behaviour to preserve.
     */
    @EventHandler
    public void onNexusGiveToEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!NexusItems.isNexus(player.getInventory().getItem(event.getHand()), adapters.keys())) return;
        event.setCancelled(true);
        player.updateInventory();
    }

    /**
     * An armour stand with arms takes a held item, and it needs its OWN handler.
     *
     * <p><b>MEASURED, NOT ASSUMED, because the obvious reading is wrong.</b> Read from the pinned
     * paper-api jar: {@code PlayerArmorStandManipulateEvent} declares its own
     * {@code getHandlerList}/{@code getHandlers}, so it has its own HandlerList and a listener
     * registered for {@code PlayerInteractEntityEvent} NEVER RECEIVES IT -- even though it extends
     * that class. {@code PlayerInteractAtEntityEvent} declares neither and does inherit, which is
     * why {@link #onNexusGiveToEntity} covers that one and not this one. Widening a handler's NAME
     * would not have closed this route.
     */
    @EventHandler
    public void onNexusArmorStand(PlayerArmorStandManipulateEvent event) {
        if (!NexusItems.isNexus(event.getPlayerItem(), adapters.keys())) return;
        event.setCancelled(true);
        event.getPlayer().updateInventory();
    }

    /**
     * NO VANILLA RECIPE EVER CONSUMES A MINTED ITEM, on any surface that resolves through this
     * event.
     *
     * <p>Hijacking the crafting table block protects ONE surface. This protects the others: the 2x2
     * grid in the player's own inventory, the recipe book's auto-fill, and any workbench screen that
     * reaches a player by a route the block hijack does not cover. A minted item eaten at any of
     * them is the same silent, unrecoverable loss.
     *
     * <p>LOWEST so the result is blanked before any other plugin reads it, and so nothing downstream
     * is reasoning about a result that must not exist.
     *
     * <p><b>The event is not {@code Cancellable} and has no {@code setResult}</b> -- verified
     * against the pinned API, not assumed. Suppression is the covariant {@code getInventory()},
     * whose {@code CraftingInventory} does have {@code setResult}. Blanking to null is the refusal.
     *
     * <p><b>This event CANNOT cover the Crafter block</b>, which is why {@link #onCrafterCraft}
     * exists beside it rather than as belt and braces. See that method.
     *
     * <p>Our own crafting menu never relies on this: it screens its matrix before consulting any
     * matcher. The commit path DOES re-enter here, because the player-taking overload of
     * {@code craftItemResult} fires this event by contract -- and it passes, because the matrix was
     * already screened and holds nothing of ours.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        boolean refuse = switch (CraftMatrixScreen.verdict(
                event.getInventory().getMatrix(), adapters.keys())) {
            case CONTAINS_GEAR -> true;
            case VANILLA_ELIGIBLE -> false;
        };
        if (refuse) event.getInventory().setResult(null);
    }

    /**
     * The Crafter block, which {@link #onPrepareCraft} is STRUCTURALLY UNABLE to reach.
     *
     * <p>Not a guess and not defensive duplication: {@code CrafterInventory}'s superinterfaces are
     * {@code Inventory} and {@code Iterable<ItemStack>}. It does NOT extend
     * {@code CraftingInventory}, and {@code PrepareItemCraftEvent}'s only constructor takes a
     * {@code CraftingInventory}. The event therefore cannot be constructed for a Crafter, so a
     * redstone-driven Crafter would happily eat a minted item with the other handler in place and
     * nothing would fire.
     *
     * <p>This event, unlike that one, IS {@code Cancellable}. Cancelling is the refusal: the block
     * keeps its ingredients and simply does not craft.
     *
     * <p>The matrix comes from the block's own container, because the event carries the recipe and
     * the result but not the ingredients, and {@code CrafterInventory} exposes no {@code getMatrix}.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCrafterCraft(CrafterCraftEvent event) {
        // FAILS CLOSED. A CrafterCraftEvent whose block is not a Crafter should be impossible, and
        // if it ever happens we cannot read the ingredients -- which means we cannot tell whether
        // one of them is a player's minted gear. "Unsure means NO CRAFT" is the rule the whole arc
        // rests on, and a bare return here would have been the single line in this slice that said
        // the opposite, in the guard for the surface with the weakest witness.
        //
        // The cost of being wrong in this direction is a Crafter that refuses to craft. The cost of
        // being wrong in the other is a player's weapon, silently and unrecoverably.
        if (!(event.getBlock().getState() instanceof Crafter crafter)) {
            event.setCancelled(true);
            return;
        }

        // GUARD ONE -- THE CORRECTNESS INVARIANT. An INGREDIENT is ours, so the craft would eat a
        // player's minted item. This is the rule the whole arc rests on and it is not negotiable.
        boolean containsGear = switch (CraftMatrixScreen.verdict(
                crafter.getInventory().getContents(), adapters.keys())) {
            case CONTAINS_GEAR -> true;
            case VANILLA_ELIGIBLE -> false;
        };
        if (containsGear) {
            event.setCancelled(true);
            return;
        }

        // GUARD TWO -- THE POLICY. The OUTPUT should be ours. Our table mints on craft; a Crafter
        // does not, so without this a Crafter is the one remaining route to a plain vanilla shield,
        // and the hole widens with every gear kind the roadmap adds. Refusing also stops RPG gear
        // being redstone-farmed, by construction rather than by a list.
        //
        // KEPT SEPARATE FROM GUARD ONE ON PURPOSE, and the two must never be merged into one
        // condition. They refuse for different reasons and have different scopes: guard one protects
        // an item a player already owns and applies to INGREDIENTS; this one is an economy decision
        // about OUTPUTS and applies to items no definition has ever claimed. Someone will eventually
        // want to relax this -- a config flag, a permission, an exception for one material -- and if
        // the two are welded together they will relax the invariant with it, and a Crafter will
        // quietly start eating minted weapons again.
        //
        // ALLOWLIST-SHAPED: everything durable is refused, with no carve-outs for materials that
        // "will never be gear". The moment it becomes "durable except shears, flint and steel,
        // fishing rods" it is ANY_BUT_SHIELD in a new costume, and the next durable item Minecraft
        // adds would be admitted by default rather than refused by default.
        //
        // ACCEPTED COST: cancelling keeps the ingredients, so a redstone clock will pulse a full
        // Crafter forever with nothing coming out, and CrafterCraftEvent has no feedback channel to
        // say why. A REFUSED CRAFTER LOOKS LIKE A JAM, NOT AN ERROR -- the same shape as the
        // sneak-right-click dead click, and accepted for the same reason.
        //
        // GUARD TWO, FIRST ARM -- A RECIPE OF OURS. Required since slice 7, not defensive.
        //
        // The durability test below was a COMPLETE statement of this policy only while every
        // claimed result was durable, which was true for as long as a claim could only be made on a
        // material. A custom recipe breaks it: our recipes register a PLAIN VANILLA result, and the
        // Flint Staff's is a `stick`. A stick is not durable, so the test below waves it straight
        // through, and a redstone Crafter becomes a machine that turns flint into sticks off a
        // recipe whose only purpose is to make weapons.
        //
        // Matched on the KEY rather than the output, because the output is exactly what durability
        // can no longer see.
        NamespacedKey recipeKey = RecipeProbe.keyOf(event.getRecipe());
        if (recipeKey != null
                && recipeKey.getNamespace().equals(adapters.keys().namespace())
                && adapters.craftResults().forRecipe(recipeKey.getKey()).isPresent()) {
            event.setCancelled(true);
            return;
        }

        if (WeaponDurability.maxOf(event.getResult()).isPresent()) {
            event.setCancelled(true);
        }
    }

    /**
     * Earned vanilla XP becomes player XP, one for one.
     *
     * <p>The whole of the progression hook. {@code ProfileService.addLifetimeXp} does the
     * arithmetic and {@code PlayerLevel} owns the curve; this method's only job is to turn an event
     * into a number.
     *
     * <h2>*** WHAT FIRES THIS EVENT IS NARROWER THAN ITS NAME, AND IT WAS MEASURED ***</h2>
     *
     * <b>{@code PlayerExpChangeEvent} is raised from exactly ONE place in the server.</b> Measured
     * 2026-09-17 against the pinned {@code run/versions/26.1.2/paper-26.1.2.jar}: only two classes
     * in the whole jar reference it -- {@code net.minecraft.world.entity.ExperienceOrb}, which
     * raises it, and {@code CraftEventFactory}, which builds it. The factory method's signature is
     * {@code callPlayerExpChangeEvent(Player, ExperienceOrb, int)}: <b>it requires an orb, and
     * there is no orbless overload.</b>
     *
     * <p><b>So this fires on ORB PICKUP and on nothing else</b>, and three consequences follow that
     * would each otherwise need a guard:
     *
     * <ul>
     *   <li><b>{@code setLevel}/{@code setExp} do not reach it.</b> {@code CraftPlayer} is not one
     *       of the two referencing classes. So the enchant table's spend and the grindstone's
     *       refund -- which are wallet-symmetric {@code setLevel}/{@code setExp} writes, by
     *       {@code EnchantMenu}'s ruling -- <b>cannot move a player's level.</b> That is a
     *       CONSEQUENCE OF THE HOOK, NOT A GUARD: there is no check here to delete, and nothing
     *       goes red if someone adds a third wallet writer. Lifetime XP counts what a player
     *       EARNED, and spending is not earning.</li>
     *   <li><b>{@code /xp} does not reach it either.</b> {@code ExperienceCommand$Type}'s bootstrap
     *       table resolves its four arms to {@code Player.giveExperiencePoints},
     *       {@code ServerPlayer.giveExperienceLevels}, {@code setExperiencePoints} and
     *       {@code setExperienceLevels} -- no orb among them -- and
     *       {@code giveExperiencePoints} raises no Bukkit event at all. <b>An operator granting XP
     *       by command moves the vanilla bar and NOT the player level.</b> Recorded because it will
     *       be rediscovered otherwise; see {@code GATE-nexus.md}'s SLICE 9 preamble.</li>
     *   <li><b>A bottle o' enchanting DOES reach it</b>, because
     *       {@code ThrownExperienceBottle} calls {@code ExperienceOrb.awardWithDirection}. That is
     *       the cheap survival-legal staging instrument for the gate rows.</li>
     * </ul>
     *
     * <h2>LOWEST, AND NO {@code ignoreCancelled} -- THE FLAG WOULD BE INERT</h2>
     *
     * <b>LOWEST</b> is Ben's ruling: we read the amount the server produced, before any other
     * plugin's multiplier. Progression is then a function of what the player did, not of what else
     * is installed.
     *
     * <p><b>{@code ignoreCancelled = true} was briefed and is NOT written here, because
     * {@code PlayerExpChangeEvent} IS NOT {@code Cancellable}.</b> Measured against the pinned
     * {@code paper-api-26.1.2.build.74-stable}: the chain is {@code PlayerExpChangeEvent ->
     * PlayerEvent -> Event} and none of the three implements it, against
     * {@code PlayerItemConsumeEvent} on the same instrument, which prints
     * {@code implements org.bukkit.event.Cancellable}. Bukkit only consults the flag for a
     * {@code Cancellable}, so writing it would have been <b>a control credited with protecting
     * something it does not touch</b> -- readable, plausible, and doing nothing forever.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        // ONE FOR ONE. No multiplier, no curve applied here -- PlayerLevel turns the total into a
        // level, and keeping the stored number raw is what makes a retune cost nothing.
        profiles.addLifetimeXp(event.getPlayer().getUniqueId(), event.getAmount());
    }

    /**
     * Without these clears, every player who has ever cast anything keeps a
     * cooldown and resource bucket until the server restarts. Both structures
     * are concurrent, so no scheduler hop is needed to drop them.
     *
     * Dropping the mana pool is also correct game behaviour, not just hygiene:
     * an absent pool reads as full, so a returning player starts charged.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // FIRST, ahead of the clears: a menu holding this player's weapon must give it back while
        // their inventory can still be written. Writes during PlayerQuitEvent persist -- the save
        // runs after this event. Bukkit does fire InventoryCloseEvent on disconnect, but its
        // ordering relative to this event is version-dependent, and returnEverything is idempotent,
        // so CAUSING the close costs nothing and depends on nothing.
        event.getPlayer().closeInventory();

        UUID playerId = event.getPlayer().getUniqueId();
        cooldowns.clear(playerId);
        meleeHits.forgetAttacker(playerId);   // drop any swing that never landed
        damageWindow.forget(playerId);        // and their environmental window, or the map grows
        adapters.scorch().forget(playerId);   // and their burn
        resources.clear(playerId);
        profiles.onQuit(playerId);
        // Drop custom-health state so no modifier or entry leaks across sessions.
        healthSystem.onQuit(playerId);
        // And drop the armor-bar override with it. API-added attribute modifiers persist in player
        // data, so a player who logs out in armor would otherwise carry a large negative armor
        // modifier written by a plugin that might not be installed next time they log in.
        ArmorBarOverride.clear(event.getPlayer(), adapters.keys());
        // Same reasoning for the attack-speed override: a player who logs out holding a boosted
        // weapon would otherwise keep a plugin-written attack-speed modifier in their player data.
        AttackSpeedAttributeOverride.clear(event.getPlayer(), adapters.keys());
        // Stop the action-bar loop and drop its handle.
        statsBar.onQuit(playerId);
        healthRegen.onQuit(playerId);
    }

    /**
     * Death is a setback, not a loot loss: keep inventory + XP, drop nothing. The only path that kills a
     * player is our own setHealth(0) on a custom-HP-zero (PlayerHealthSystem.onChange), so this is global
     * -- there is no vanilla-death path to scope around. Sets flags only; it never touches the store.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // FIRST: return anything a menu is holding while the inventory still exists. The drops list
        // is already populated by the time this event fires, so a returned weapon cannot leak into
        // it -- and setKeepInventory below means it survives the death either way. Caused here
        // rather than relied upon, because "the client closes the container on death" is a
        // behaviour, not a contract.
        event.getPlayer().closeInventory();

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);
    }

    /**
     * Respawn after a custom-HP death: reset custom HP to full and RESTART the two per-entity loops that
     * self-cancelled on the death screen (EntityTaskTarget is inactive while dead). Mirrors onJoin's
     * health + nameplate wiring; the profile is not reloaded (it persists across death). The nameplate
     * loop restart is the easy-to-miss one -- without it a respawned player stops seeing mob nameplates.
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        healthSystem.onRespawn(event.getPlayer());     // reset to base 100, render, restart the reconcile loop
        nameplates.onViewerJoin(event.getPlayer());    // restart the per-viewer nameplate LOS loop
        statsBar.onRespawn(event.getPlayer());         // restart the action-bar loop, dead since the death screen
        healthRegen.onRespawn(event.getPlayer());      // and the regeneration loop, dead for the same reason
        // NOT a leak fix -- a CORRECTNESS one. onQuit does not run on death and onEntityRemove filters
        // players out, so without this a player who died mid-window respawns still holding it and the
        // next environmental hit inside WINDOW_TICKS is absorbed. Respawning into lava or a wall is
        // exactly when environmental damage arrives.
        damageWindow.forget(event.getPlayer().getUniqueId());
        // RESPAWN is a correctness fix, not a leak fix -- DamageWindow.forget's reason exactly: quit
        // handling does not run on death and entity-removal filters players out, so without this a
        // player who died mid-burn respawns still scorched, burning on the old applier's credit.
        adapters.scorch().forget(event.getPlayer().getUniqueId());
        // Same convergence as on join. onQuit does not run on death, and the cursor-at-death path
        // is unverified on 26.1 (GATE-nexus.md row 1), so a star lost to a death would otherwise
        // stay lost until the player's next reconnect rather than until their next respawn.
        //
        // NO WAIT HERE, AND THE ASYMMETRY WITH onJoin IS CORRECT RATHER THAN AN OVERSIGHT. The
        // profile is not reloaded on death (it persists), so by respawn it is already in memory and
        // lockedSlotOf answers synchronously. Its NO_LOCKED_SLOT fallback would only be reached by
        // someone respawning before their join load finished, where converge's own default is the
        // same answer whenSettled would have given.
        // THE SAME SWITCHED-OFF GUARD AS onJoin, AND IT IS NEEDED SEPARATELY. Respawn is the other
        // path that mints, so guarding only the join would leave the star coming back on death --
        // which is a stranger bug report than it not sticking across a session.
        if (!profiles.starEnabled(event.getPlayer().getUniqueId())) return;
        NexusSlots.converge(event.getPlayer(), adapters.keys(),
                NexusSlots.lockedSlotOf(event.getPlayer(), profiles));
    }

    // --- Freeze's attack-suppression. Each handler is a thin gate: if the attacking mob is
    // frozen (its freeze immobilize task is live), cancel the attack. The suppression lifts
    // automatically when the freeze ends -- isFrozen goes false on expiry and on death -- so
    // there is no separate suppression state to clean up.

    /** Melee: a frozen mob deals no damage. The player can still damage IT (damager is the player). */
    @EventHandler
    public void onFrozenMeleeAttack(EntityDamageByEntityEvent event) {
        if (isFrozen(event.getDamager())) event.setCancelled(true);
    }

    /**
     * Capture the swing's CHARGE, before vanilla throws it away.
     *
     * <p>MEASURED, not assumed (2026-08-28 Step 0): vanilla calls {@code resetAttackStrengthTicker()}
     * before {@code hurt()}, so the same swing reads {@code getAttackCooldown() == 1.0000} here and a
     * near-zero value inside the damage event. Reading the charge there would scale every hit to its
     * floor, however well timed.
     *
     * <p>The post-reset value is always {@code 0.5 / period}, so it varies with the weapon and none
     * of these numbers is the constant: the boot measured {@code 0.0400} on a plain iron sword
     * (attack speed 1.6) and {@code 0.1000} bare-handed (4.0), while a weapon minted by this build
     * pins 2.0 and so reads {@code 0.0500}. Cited because the shape is what matters -- an order of
     * magnitude below the real charge, whatever the weapon.
     *
     * <p>Damage is NOT dealt here. This event fires for attacks vanilla will go on to refuse -- an
     * i-framed re-hit among them -- so it says a swing was ATTEMPTED, not that one landed. Landing is
     * the damage event's news, which is why the two are split and why this only stashes.
     *
     * <p>Scoped to mob victims: player victims are a PvP rules decision, deferred with the rider's.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPrePlayerAttack(PrePlayerAttackEntityEvent event) {
        if (!(event.getAttacked() instanceof LivingEntity victim) || victim instanceof Player) return;
        meleeHits.record(event.getPlayer().getUniqueId(), victim.getUniqueId(),
                event.getPlayer().getAttackCooldown());
    }

    /**
     * Vanilla's SWEEP, now OWNED rather than cancelled: each swept mob takes a fraction of what the
     * primary target took on the same swing.
     *
     * <p>Same shape as the basic melee rider above -- vanilla selects, we deal the damage. A sweeping
     * sword raises a separate EntityDamageByEntityEvent per neighbouring mob, with the player as
     * damager and cause ENTITY_SWEEP_ATTACK, and vanilla has already decided the hard parts: that the
     * swing was at full charge, that the weapon is a sword, and which mobs are inside the sweep
     * hitbox. None of that is re-derived here. Riding those events is what keeps this from becoming
     * the 120-degree cone Stage 1 retired -- it is not our arc, our reach or our target selection.
     *
     * <p>THE NUMBER is {@code SWEEP_FRACTION x what the primary was hit for}, and taking a fraction
     * of the primary's FINAL figure is what makes sweep inherit the enchant percentage, the class
     * damage bonus and the charge by construction. There is no second multiplier chain here, so there
     * is nothing for the two to disagree about, and a buffed or well-timed swing sweeps harder for
     * free. It does NOT inherit the vanilla crit, because the crit lands on the tokened number and
     * contributes nothing to the custom one -- it will, unchanged, the day a crit multiplier reaches
     * the custom amount.
     *
     * <p>PRE-mitigation, and forced rather than chosen: {@code applyDamage} is deferred onto the
     * victim's entity scheduler and lands NEXT tick, while vanilla raises every sweep event inside the
     * same synchronous {@code Player#attack} as the primary. The primary's post-mitigation figure
     * therefore does not exist yet when we are asked. Each swept mob mitigates its own Defense once,
     * which is also the reading that avoids double-counting armor.
     *
     * <p>FAILS CLOSED at every gate. No declared sweep on the held weapon, or no stashed primary
     * damage, means the event is cancelled exactly as it was before this pass. That one absence
     * covers a windowed-out primary, an untagged or weaponless hit, and a broken weapon -- a broken
     * weapon's swing is cancelled before it can claim anything, so it stashes nothing and sweeps
     * nothing, with no separate gate needed here.
     *
     * <p><b>THE GATES RUN BEFORE THE TOKEN, deliberately unlike the primary rider.</b> That handler
     * tokens unconditionally, so a refused click still flashes the mob -- an accepted cosmetic, with
     * the fix recorded in NEXT.md as a decision to take later. This takes it: a bystander that will
     * not be swept is neither flashed nor given i-frames. That ordering is the whole reason the old
     * cancel-outright existed (a tokened sweep would set a bystander's i-frames and block the next
     * real hit on it for ten ticks), and it is now bought rather than argued away -- a mob is tokened
     * only when it is actually being damaged.
     *
     * <p>TOKENED, not cancelled, once a sweep does land: the token is what keeps vanilla's flash,
     * hurt sound, i-frames AND its little sweep shove. Cancelling would suppress the push the same
     * way it does for a broken weapon. The can't-kill floor is replicated from the primary rider for
     * the same reason it exists there -- death is the custom-HP path's business.
     *
     * <p>DAMAGE ONLY. A swept mob takes a number and nothing else the payload does: no statuses, no
     * visuals, no durability wear. Going through applyDamage directly rather than re-running the
     * weapon's on_hit is what makes that structural -- {@code CastExecutor}'s melee arm already warns
     * that billing a use per body is the bug to avoid.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerSweepAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) return;
        // NOT a bare return: this handler cancelled EVERY player-damager sweep before sweep was
        // owned, and a victim we will not sweep must keep that cancel rather than have vanilla's
        // sweep damage leak through. A player victim is the live case -- PvP is a deferred rules
        // decision, exactly as the primary rider defers it -- and letting a sweep land on one would
        // be PvP arriving by accident, through the one path nobody would think to look at.
        if (!(event.getEntity() instanceof LivingEntity swept) || swept instanceof Player) {
            event.setCancelled(true);
            return;
        }

        // What this weapon declares, and what the swing actually dealt. Either being absent means
        // this stays exactly the cancel it was before sweep was owned.
        double fraction = WeaponItems.heldWeaponId(attacker, adapters.keys())
                .flatMap(weapons::find)
                .map(WeaponDefinition::sweep)
                .orElse(SweepShare.NONE);
        var primary = meleeHits.primaryHitThisTick(attacker.getUniqueId());
        if (!SweepShare.sweeps(fraction) || primary.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        // The swept mob's OWN window, so a mob already hit this window is not swept on top of it --
        // the same once-per-10-ticks cadence the primary gets.
        //
        // It is NOT what releases this mob's shove, and the boot is what settled that: the sweep
        // knockback arrives with cause SWEEP_ATTACK, which onCombatKnockback returns on before it
        // ever consults the window. The claim still lands first -- all 9 sweep knockback events read
        // landedThisTick=true -- but that is evidence of ORDERING, not the gate doing work. Saying
        // otherwise would credit our code with vanilla's, which this file has had to correct once.
        if (!meleeHits.claimWindow(swept.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        event.setDamage(TOKEN_DAMAGE);                                   // flash + i-frames + shove
        floorSoTokenCannotKill(swept);

        // The FOUR-arg applyDamage, so the swept mob keeps a NORMAL white number even when the
        // primary critted, and now carries the primary's ELEMENT so a fire sweep is fire damage on
        // every mob it caught. Its DAMAGE still inherits the crit in full -- the stashed figure is
        // already multiplied -- so a crit swing sweeps for half of the doubled number.
        //
        // CRIT AND ELEMENT TRANSFER DIFFERENTLY, AND THE PRECEDENT DOES NOT CARRY FROM ONE TO THE
        // OTHER. A crit is a PER-HIT ROLL the bystander did not receive, so colouring every
        // bystander yellow would claim each of them crit independently -- hence the white numbers
        // and no crit particles here. An element is a PER-WEAPON IDENTITY every target of the swing
        // genuinely did receive, so drawing them unmarked would assert something false the other
        // way. Opposite transfer properties; the white-for-sweep decision is untouched.
        //
        // THE ELEMENT COMES FROM THE STASH, NOT FROM THE HELD WEAPON. Reading
        // WeaponDefinition.element() here would be a SECOND derivation of a fact the seam already
        // reports, and the two can disagree in shipped content: ability_stone declares
        // element: kinetic at the weapon level while its nested damage effect declares fire.
        BukkitCombatant.of(swept, adapters).handle().applyDamage(
                SweepShare.of(primary.get().damage(), fraction), attacker.getUniqueId(),
                CritState.NORMAL, DefenseRule.APPLIES, primary.get().element(),
                // ACCRUES, AND IT IS A REAL DECISION RATHER THAN A DEFAULT. INERT is the reflexive
                // choice here -- "a derived hit, half the primary's damage, not a real one" -- and it
                // would be wrong: a0eee2b exists so a fire sweep burns what it caught, and
                // emberblade's flavour was rewritten for it.
                //
                // NO GATE ROW CAN SEE THIS. The share is half the primary, so a bystander takes 3,
                // buys one stack, and on a 20-HP mob its burn reads 1 -- indistinguishable from
                // anything else lighting it. The unit row is the only witness, which is why it
                // exists: SweepShareTest's sibling in RpgListeners has no fixture, so the assertion
                // lives where the decision is readable instead.
                HitAccrual.weapon());
    }

    /**
     * Ride a player's melee hit on a mob: vanilla picks the victim and keeps the cosmetics, we own
     * the mechanics. This is the basic melee attack now -- the arm-swing packet no longer fires it.
     *
     * <p>Vanilla's crosshair attack decides WHO was hit, with its own reach, its own aim and its own
     * occlusion test, so a mob 60 degrees off the crosshair can no longer take a swing meant for
     * something else. We token the vanilla damage -- non-zero so the mob still flashes and takes
     * i-frames, small enough that it cannot double the custom number -- then land the weapon's
     * on_hit payload through the same EffectApplier every ability uses.
     *
     * <p>Runs at HIGH with ignoreCancelled: {@link #onFrozenMeleeAttack} cancels a frozen damager's
     * hit at NORMAL, and same-priority order is undefined. It matters more than it used to -- this
     * handler now DEALS DAMAGE, so a frozen player's suppressed swing must not still land one.
     *
     * <p>ENTITY_ATTACK only, and that gate -- not handler ordering -- is what keeps sweep out.
     * {@link #onPlayerSweepAttack} sits at the SAME priority, so which of the two runs first is
     * undefined; if the canceller lost the coin toss, ignoreCancelled would not save us. The cause
     * check makes the order irrelevant, which is why the cancel there is for the bystander's
     * i-frames rather than for this handler's benefit. Every other cause with a player damager (a
     * thrown potion, a fired arrow's shooter) is not a melee swing and is not ours here either.
     *
     * <p>The charge arrives from the pre-attack stash and FAILS CLOSED: no matching swing means no
     * custom damage, rather than a guessed full-power hit. The window claim is the anti-spam guard,
     * and {@link MeleeHits} documents at length why it keys on our own hit history rather than on
     * vanilla's noDamageTicks -- the short version is that a burning mob's fire ticks drive that
     * counter too, and would have made melee stutter against exactly the mobs our fire content
     * creates.
     *
     * <p>Token-can't-kill is unchanged: death is the custom-HP path's business (MobDeathSystem via
     * setHealth(0)), so the 0.01 token must never drop a tracked mob to zero on its own.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMeleeAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;    // player-initiated
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (victim instanceof Player) return;                            // player->mob only
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        // A BROKEN weapon is inert, and cancelling is what makes it look inert: no damage, and no
        // flash, sound, knockback or i-frames either. A weapon that deals nothing must not stagger
        // anything. Scoped by weapon_id, so an untagged vanilla sword is untouched.
        if (WeaponDurability.isHeldWeaponBroken(attacker, adapters.keys())) {
            event.setCancelled(true);
            return;
        }

        event.setDamage(TOKEN_DAMAGE);                                   // flash + i-frames, no double
        floorSoTokenCannotKill(victim);

        // Fail closed on a missing swing, and claim the window before dealing anything. Both are
        // mutations, so neither may be asked twice for one hit.
        var swing = meleeHits.consume(attacker.getUniqueId(), victim.getUniqueId());
        if (swing.isEmpty()) return;
        if (!meleeHits.claimWindow(victim.getUniqueId())) return;

        // STASH WHAT IT DEALT, for the sweep rider. The number is observed through EffectApplier's
        // damage seam, not recomputed, so sweep cannot drift from the hit it is a fraction of; and it
        // is absent whenever nothing was dealt, which is what makes sweep fail closed.
        WeaponFire.landVanillaMelee(attacker, victim, AttackCharge.scale(swing.get().charge()),
                weapons, adapters, cooldowns)
                .ifPresent(hit -> meleeHits.recordPrimaryHit(
                        attacker.getUniqueId(), hit.amount(), hit.element()));
    }

    /**
     * Pass 2 -- ride a MOB's melee hit on a PLAYER: keep vanilla's cosmetics (red flash, hurt sound,
     * i-frames), own the mechanics. Token the vanilla damage so the player's vanilla hearts barely
     * move and the token can't kill (death is deferred), then drain the player's CUSTOM HP via
     * applyDamage -- the heart bar follows. i-frames are PRESERVED: we ride only what vanilla fires
     * and touch noDamageTicks nowhere, so a player is hit at most once per ~0.5s window regardless of
     * swarm size (the swarm-melt bypass is a deliberate later fork).
     *
     * Runs at HIGH, not NORMAL: {@link #onFrozenMeleeAttack} cancels a frozen mob's hit at NORMAL, and
     * same-priority order is undefined -- HIGH runs strictly after, so ignoreCancelled then skips a
     * frozen attacker's suppressed hit.
     *
     * Amount = the mob's custom ATTACK_DAMAGE stat, NOT event.getDamage(): the vanilla bridge is retired.
     * We seed the mob's stat from its vanilla attack-damage attribute (seedCombatStats, opt-out-agnostic
     * so a nameplate-less mob still hits) and read it back -- the mob analog of bootstrapping mob HP from
     * vanilla MAX_HEALTH, and reading it from the store the way player melee does. Same number initially
     * (the path reads the store, proven; magnitude can now be scaled past vanilla, the attack-side >1024
     * mirror). We still token the vanilla damage for cosmetics only.
     *
     * No new token-can't-kill floor here (unlike the mob victim above): the player heart bar already
     * floors vanilla health at ~half a heart, which is >> the 0.01 token, so it cannot kill.
     * Knockback stays vanilla: onCombatKnockback skips players, and mobs have no declared KB spec.
     * No damage popup: the dealer is a mob (dealerIsPlayer resolves false in applyDamage).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobMeleeAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;          // mob->player only
        if (!(event.getDamager() instanceof LivingEntity attacker)) return; // a living melee attacker
        if (attacker instanceof Player) return;                             // player->player is a later rules decision

        // THE CAUSE GATE, ADDED WITH VanillaDamagePolicy, AND ITS ABSENCE WAS LOAD-BEARING.
        //
        // Both sibling riders have always gated on cause -- onPlayerMeleeAttack on ENTITY_ATTACK
        // (":ENTITY_ATTACK only, and that gate -- not handler ordering -- is what keeps sweep out"),
        // onPlayerSweepAttack on ENTITY_SWEEP_ATTACK. THIS ONE DID NOT, and filtered on entity types
        // alone. So it claimed EVERY EntityDamageByEntityEvent with a player victim and a living
        // non-player damager -- a creeper's ENTITY_EXPLOSION and a warden's SONIC_BOOM among them --
        // and priced each of them as the damager's melee ATTACK_DAMAGE stat.
        //
        // Those causes now belong to VanillaDamagePolicy, which prices them from the event's OWN
        // getDamage() instead. That is the point of the gate rather than a side effect of it: while
        // this handler claimed causes by entity shape, the boundary was not a function of DamageCause,
        // so forCause could not be total, its exhaustive switch could not be meaningful, and it could
        // not be unit-tested without a server. One handler's missing gate was the whole obstacle.
        //
        // What the gate withdraws from those causes, and where each thing went:
        //   - seedCombatStats: NOWHERE NEEDED. onEntityAdd (EntityAddToWorldEvent -- "spawn OR
        //     chunk-load, both funnel here") already seeds every living non-player at world-add, so
        //     the call below was belt-and-braces for the melee path, never the sole route to tracking.
        //   - the shield block AND its durability wear: CARRIED, deliberately, into
        //     onEnvironmentalDamage. Vanilla lets you block a creeper blast; dropping that would be a
        //     gameplay regression, not a repricing, and it is the one that would have shipped unseen.
        //   - the thorns reflect: DELIBERATELY WITHDRAWN. See onEnvironmentalDamage for why.
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        // MUST stay first, and not only for the nameplate. bootstrapIfAbsent is what makes the mob
        // TRACKED, and CombatantStats.damage is a silent no-op on an untracked combatant -- so this
        // line is the precondition for the thorns at the bottom as much as for the stat read below.
        // Move or gate it and the reflect vanishes with no error anywhere.
        nameplates.seedCombatStats(attacker);         // idempotent, opt-out-agnostic: seed HP + attack from vanilla

        // THE PRE-MITIGATION BLOW: the attacker's raw stat, before the block AND before the victim's
        // armor (which lands a thread-hop later inside CombatantStats.damage). Thorns reflects a
        // fraction of THIS, so a heavily armored player reflects more than the hit did to them.
        // final, and never reassigned -- ShieldExchange derives both numbers from it below, so no
        // reduced local exists here that could be passed to the reflect by mistake.
        final double preMitigation = adapters.stats().attackValue(attacker.getUniqueId());

        // THE BLOCK, and it must be resolved BEFORE the token below. EntityDamageEvent.setDamage
        // re-derives every modifier by scaling it against the new base, so reading the BLOCKING
        // modifier after tokening reports the token's share of the block rather than the block.
        // Vanilla decides WHETHER this was a block -- raised, frontal, in-arc; the shield decides
        // what it is worth. See ShieldBlock for why isBlocking() is not the signal.
        ShieldBlock.Outcome block = ShieldBlock.resolve(
                victim, event, adapters.keys(), shields, adapters.enchants());
        // BOTH numbers, from the ONE raw blow. The choice of which value each is derived from is the
        // slice's load-bearing decision and lives in core where a unit test can reach it -- see
        // ShieldExchange, which exists precisely because this method cannot be unit-tested.
        ShieldExchange exchange = ShieldExchange.of(
                preMitigation, block.blocked(), block.effectiveDr(), block.reflectPercent());

        if (block.blocked()) {
            // Wear is charged HERE and vanilla's own is cancelled in onShieldItemDamage, because
            // our Unbreaking is custom and vanilla would never consult it. AFTER the resolve, so
            // the block that breaks the shield still mitigates in full and only the next one does
            // nothing.
            ShieldDurability.applyWearOnBlock(victim, block.slot(), adapters.keys(), cooldowns);
        }

        event.setDamage(TOKEN_DAMAGE);                // ride: keep flash/sound/i-frames, no double, can't kill
        BukkitCombatant.of(victim, adapters).handle()
                .applyDamage(exchange.applied(), attacker.getUniqueId());

        // THE THORNS, and it is LAST for a reason that is NOT tick ordering.
        //
        // Both applyDamage calls defer to their entity's next tick, so "the victim's damage lands
        // first" holds on Paper by FIFO accident and is meaningless on Folia, where the two may be
        // in different regions. Nothing observable depends on the order: the amount was computed
        // synchronously above and there is no shared mutable state.
        //
        // What IS ordering-sensitive is the throw. BukkitCombatant.of runs INLINE and its first act
        // is Regions.requireOwned, which throws for an entity this thread does not own. Placed above
        // the two lines before it, that throw would skip setDamage -- so VANILLA'S FULL DAMAGE would
        // land on the player -- and skip the custom hit as well. Placed here, a throw costs the
        // thorns and nothing else. Fail toward doing less, the same instinct as a dangling
        // shield_id resolving to Outcome.NONE.
        //
        // The two-arg applyDamage, matching the sweep rider: a reflect is computed directly and never
        // passes through the crit multiplier, so it CANNOT crit, and the white number says so
        // honestly. Colour means crit in this game and nothing else.
        //
        // reflects() also skips of()'s wasted snapshot -- a ThreadLocalRandom draw and five stat
        // lookups the reflect discards -- on every ordinary blocked hit.
        if (Thorns.reflects(exchange.reflected())) {
            BukkitCombatant.of(attacker, adapters).handle()
                    .applyDamage(exchange.reflected(), victim.getUniqueId());
        }
    }

    /**
     * EVERY OTHER WAY THE WORLD CAN HURT A TRACKED COMBATANT -- the damage half of the boundary
     * {@link VanillaHealPolicy} states and only half enforces.
     *
     * <p>Fall, drowning, lava, fire, suffocation, cactus, starvation, explosions: before this, all of
     * them moved the VANILLA bar and nothing else. {@code HeartBarRenderer} rewrites that bar from the
     * custom numbers on the next {@code HealthChange} or reconcile tick, so the damage was visible for
     * a fraction of a second and then silently reverted -- which reads to a player as a bug, because
     * it is one. Observed in game as "the health bar changes but the Health number stays at 100".
     *
     * <p><b>Both players AND mobs, and the differing gate is deliberate.</b> {@link #onRegainHealth}
     * gates on "is this a tracked PLAYER" because no vanilla heal was rewriting a mob's health. This
     * gates on "is this combatant TRACKED": mobs read their nameplates from the same store, so mob
     * fall damage was broken the same way. Not a copy-paste slip.
     *
     * <p><b>An untracked combatant is PASSED, and that does not violate the grouping rule.</b> For an
     * untracked combatant vanilla health IS the only truth there is, so leaving the event alone moves
     * the only truth it has. Rerouting would be the bug: {@code CombatantStats.damage} is a silent
     * no-op on an untracked id, so the token would land and the real damage would vanish.
     *
     * <p><b>Registered on the SUPERCLASS.</b> {@code EntityDamageByEntityEvent} declares no
     * {@code HandlerList} of its own, so this receives those too -- which is exactly why the PASS arms
     * for {@code ENTITY_ATTACK} and {@code ENTITY_SWEEP_ATTACK} are load-bearing rather than tidy. The
     * three riders sit at HIGH alongside this one and their relative order is undefined; as with
     * {@link #onPlayerMeleeAttack}, THE CAUSE GATE AND NOT HANDLER ORDERING is what keeps the four
     * disjoint. Reroute a melee cause and every swing lands twice, whichever runs first.
     *
     * <p><b>getDamage(), never getFinalDamage(), and the two are indistinguishable in a green
     * build.</b> {@code getDamage()} is {@code getDamage(BASE)} -- the raw amount before vanilla's
     * armour, resistance and enchantment cuts. We take it raw because {@code CombatantStats.damage}
     * applies our OWN {@code Defense.applyDefense} a thread-hop later. Taking the final amount would
     * mitigate twice, and worse than twice: {@code ArmorBarOverride} has rewritten the player's
     * vanilla ARMOR attribute to MEAN our damage reduction, so vanilla's cut is already this project's
     * own curve wearing vanilla's formula. Same call the mob->player rider makes, for the same reason.
     *
     * <p><b>The shield is resolved here, carried over from the rider that used to see explosions.</b>
     * {@code ShieldBlock.resolve} takes an {@code EntityDamageEvent}, not the ByEntity subclass, and
     * self-gates on {@code isApplicable(BLOCKING)} -- so calling it for every cause is correct with no
     * per-cause branch: a fall has no BLOCKING modifier and resolves to {@code Outcome.NONE}. It must
     * run BEFORE the token, because {@code setDamage} re-derives every modifier against the new base.
     *
     * <p><b>The thorns reflect is NOT carried over, deliberately.</b> Ours is contact reflection off a
     * blocked melee blow; the creeper whose blast is being blocked has already died in the same tick,
     * so the reflect would be a computation with no target. {@code exchange.applied()} is used and
     * {@code exchange.reflected()} is dropped on purpose -- not overlooked. (Vanilla's own THORNS is a
     * separate matter: it is an ARMOUR enchantment raising its own THORNS event, which this handler
     * reroutes like any other. Ours is a SHIELD enchant reflected through {@code applyDamage}, which
     * raises no event. Two sources, not one mechanism applied twice.)
     *
     * <p><b>No window claim.</b> {@link MeleeHits}' window is melee's anti-spam guard, keyed on our own
     * hit history. Environmental cadence is vanilla's, and tokening rather than cancelling is what
     * preserves it -- lava keeps damaging every ten ticks because vanilla's i-frames survive the ride.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        UUID id = target.getUniqueId();
        if (!adapters.stats().tracks(id)) return;
        if (VanillaDamagePolicy.forCause(event.getCause()) == VanillaDamagePolicy.Action.PASS) return;

        // A SCORCHED VICTIM'S FIRE TICK IS OURS, AND LETTING IT THROUGH IS A DOUBLE-DIP.
        //
        // setFireTicks is a VISUAL THAT CARRIES DAMAGE. Scorch keeps the burn's look and takes over
        // its damage -- on its own 20-tick clock, capped by the applying weapon, credited to the
        // applier, bypassing Defense. Vanilla's FIRE_TICK is none of those things: since the vanilla
        // damage boundary landed it reroutes to custom HP uncapped, and attributableId (below) falls
        // back to the VICTIM's own id because a fire tick names no causing entity. So without this
        // gate a scorched target takes both streams and the kills credit the corpse.
        //
        // THE GATE LIVES HERE, NOT IN VanillaDamagePolicy. That table classifies a CAUSE; this is a
        // fact about a VICTIM, and the two must not be confused.
        // VanillaDamagePolicyTest.theMOTIVATINGFireCausesGetNoSpecialTreatment exists specifically to
        // redden if a Scorch decision leaks into the policy, and it should stay able to.
        //
        // BEFORE damageWindow.claim, DELIBERATELY. A suppressed tick dealt nothing, so it must not
        // consume window budget: claiming first would let a zero-damage fire tick open a window that
        // absorbs a real lava hit for the next ten ticks (DamageWindow.claim's ratchet arm), which
        // would make being scorched a DEFENSIVE BUFF against lava. It still tokens and still floors,
        // because it must not reach vanilla either -- the same shape as the absorbed branch below,
        // minus the claim.
        //
        // NAMED DEBT: THIS SUPPRESSES ONE MEMBER OF A FAMILY OF FOUR, AND THE OTHER THREE DOUBLE-DIP.
        //
        // Deferred by the operator 2026-09-09, recorded as a bounded question rather than a symptom.
        // Observed as "the burn doubles only while standing in fire", which IS the diagnosis: block
        // contact raises FIRE, the ignition raises FIRE_TICK, and only the second is caught. Step out
        // and it stops.
        //
        //     FIRE_TICK   suppressed while scorched          <- the stream we replaced
        //     FIRE        NOT. Standing in a fire block.
        //     LAVA        NOT.
        //     HOT_FLOOR   NOT. Magma block.
        //
        // Under any of the other three, a scorched victim takes OUR capped, credited, defense-
        // bypassing burn PLUS vanilla's uncapped, uncredited one -- the precise doubling this
        // suppression exists to prevent, arriving through a sibling cause.
        //
        // AND IT IS NOT A ONE-LINE FIX, WHICH IS THE REAL REASON IT IS A QUESTION RATHER THAN A TODO.
        // Suppressing LAVA would mean a scorched mob takes LESS lava damage than an unscorched one --
        // scorch as a defensive buff. This repo already refused that shape once, which is why the
        // FIRE_TICK gate sits BEFORE damageWindow.claim (see above): a suppressed tick dealt nothing,
        // so it must not consume window budget either.
        //
        // THE BOUNDED QUESTION, for whoever takes it: which of these four should scorch suppress, and
        // does suppressing LAVA make scorch a defensive buff? An answer per cause, like the standing
        // "which causes should Defense touch?" question DefenseRule was built for.
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                && adapters.scorch().isScorched(id)) {
            event.setDamage(TOKEN_DAMAGE);
            floorSoTokenCannotKill(target);
            return;
        }

        // OWN THE CADENCE. The token we are about to write becomes vanilla's lastHurt, which destroys
        // its ratchet for any cause whose cadence IS the invulnerability window -- lava and
        // suffocation measured at 20 Hz, and at 20 Hz x blocks contacted on a large hitbox (eight
        // LAVA events for one golem in one tick). See DamageWindow for why every alternative is dead.
        //
        // RAW event damage, deliberately: the window stores VANILLA units, upstream of the shield and
        // of Defense. Sound because applyDefense is linear in damage, which DefenseTest now guards.
        //
        // bypassesCooldown is false and cannot be otherwise today: Bukkit's Tag exposes no
        // damage_types registry and DamageType carries no tag membership, so vanilla's
        // BYPASSES_COOLDOWN is unreadable from here. The parameter exists for OUR abilities.
        double toDeal = damageWindow.claim(id, event.getDamage(), false);
        if (toDeal <= 0.0) {
            // Absorbed. It still tokens and still floors -- it must not reach vanilla either -- but it
            // resolves no shield and wears no durability, because nothing landed. Logged with
            // applied=0 so an absorbed event is VISIBLE rather than silent: a window that is working
            // and a handler that never fired must not look the same in the log.
            event.setDamage(TOKEN_DAMAGE);
            floorSoTokenCannotKill(target);
            return;
        }

        // BEFORE the token, or the BLOCKING modifier reports the token's share of the block rather
        // than the block. ShieldBlock's own javadoc carries this rule; the ordering here obeys it.
        ShieldBlock.Outcome block = ShieldBlock.resolve(
                target, event, adapters.keys(), shields, adapters.enchants());
        ShieldExchange exchange = ShieldExchange.of(
                toDeal, block.blocked(), block.effectiveDr(), block.reflectPercent());
        // The WEAR needs a Player -- it reads an inventory slot -- while the resolve above is happy
        // with any LivingEntity. The narrowing costs nothing: our shields are player gear, so a mob
        // victim resolves to Outcome.NONE and never reaches here. It is written as a pattern rather
        // than a cast so a future mob-held shield is a silent no-wear rather than a ClassCastException
        // in the middle of a damage event.
        if (block.blocked() && target instanceof Player wearer) {
            ShieldDurability.applyWearOnBlock(wearer, block.slot(), adapters.keys(), cooldowns);
        }


        // THE CONVERSION, AND THIS IS ITS ONLY CALL SITE IN THE PROJECT. An audit of every
        // applyDamage entry found exactly one vanilla-denominated number reaching the custom store --
        // this one. Sweep, mob-melee and thorns price from stats().attackValue; ability and weapon
        // damage from content; the dev commands from an operator-typed integer. A second call to
        // DamageScale.toCustom anywhere would be k squared.
        //
        // LAST STEP, on the value handed to applyDamage. The window upstream stays in VANILLA units
        // (commit 1's invariant), and the shield's DR and reflect are PERCENTAGES, so they COMMUTE
        // with a scalar -- converting before or after them is the same number. The ordering here is
        // documentation, not correctness.
        var maxAttr = target.getAttribute(Attribute.MAX_HEALTH);
        double applied = DamageScale.toCustom(
                exchange.applied(),
                adapters.stats().max(id),
                maxAttr == null ? Double.NaN : maxAttr.getValue(),
                adapters.stats().isBarPuppeted(id));


        event.setDamage(TOKEN_DAMAGE);      // ride: keep i-frames, flash, knockback, cadence
        floorSoTokenCannotKill(target);
        BukkitCombatant.of(target, adapters).handle()
                .applyDamage(applied, attributableId(event, target));
    }

    /**
     * Who to blame for a vanilla damage event: the entity that CAUSED it, or the target itself when
     * nothing did.
     *
     * <p><b>Self-attribution follows {@code applyHeal}'s precedent -- "the honest placeholder rather
     * than a null that would read as unknown" -- but ONLY where it is still honest.</b> That
     * justification holds for a heal because a heal has no causing entity, and it held for damage
     * while the population reaching this handler was sourceless. {@link #onMobMeleeAttack}'s new cause
     * gate CHANGED THAT POPULATION: creeper blasts and warden booms arrive here now, and self-attributing
     * those would record a creeper kill as the victim killing themselves, with no aggro -- a regression
     * against what the melee rider did before the gate, not a rounding error.
     *
     * <p>So the causing entity wins where one exists. Gravity still credits the faller. The faction bit
     * is never touched here: {@code applyDamage} derives {@code dealerIsPlayer} from whatever this
     * resolves to, so the seam cannot claim a player's damage came from a mob or the reverse.
     */
    private static UUID attributableId(EntityDamageEvent event, LivingEntity target) {
        DamageSource source = event.getDamageSource();
        Entity causing = source == null ? null : source.getCausingEntity();
        return causing != null ? causing.getUniqueId() : target.getUniqueId();
    }

    /**
     * Keep the 0.01 token from killing a tracked MOB, which death is not supposed to come from.
     *
     * <p>Extracted at the third call site rather than copied a third time. Mob death is
     * {@code MobDeathSystem}'s, through {@code setHealth(0)} on the {@code reachedZero} transition, so
     * a mob whose puppet health happens to sit below the token must not fall over from the ride.
     *
     * <p><b>Mobs only, and that asymmetry is not an omission.</b> A player's vanilla health is already
     * floored at {@code HeartBarRenderer.MIN_LIVE_HEALTH_POINTS} (1.0) by every render, which is a
     * hundred times the token -- so a player needs no floor here and adding one would be a second
     * mechanism competing with the first.
     */
    private void floorSoTokenCannotKill(LivingEntity victim) {
        if (victim instanceof Player) return;
        if (!adapters.stats().tracks(victim.getUniqueId())) return;
        if (victim.getHealth() - TOKEN_DAMAGE > 0.0) return;
        var attr = victim.getAttribute(Attribute.MAX_HEALTH);
        double vanillaMax = attr == null ? victim.getHealth() : attr.getValue();
        victim.setHealth(Math.min(vanillaMax, VANILLA_LIVE_FLOOR));
    }

    /**
     * Vanilla must NOT wear one of our shields -- {@link ShieldDurability} does it instead.
     *
     * <p>Our {@code Unbreaking} is a custom enchant whose curve lives in core, because the
     * no-vanilla-enchants policy means a player-held item never carries a vanilla enchant to
     * delegate to. Vanilla charging the shield on a block would never consult it, so Unbreaking
     * would sit on the tooltip doing nothing.
     *
     * <p><b>Measured 2026-08-29: vanilla fired this event ZERO times across 20 blocks</b>, so on
     * this build there is no double-wear to prevent and this cancel is a guard against something
     * not currently happening. It stays -- we own this item's durability outright, and any future
     * vanilla path charging it would be an unaccounted second source -- but it should not be
     * described as fixing an observed doubling. The positive evidence for the wear path is the bar
     * itself: 20 blocks with Unbreaking III took it 336 -> 331, against 5.00 expected.
     *
     * <p>Scoped by the {@code shield_id} tag, the same boundary {@code /rpg durability} and the
     * weapon gates draw: an untagged vanilla shield keeps wearing exactly as it always did.
     *
     * <p>Cancels ALL vanilla wear on our shields, not only wear from blocking. That is deliberate
     * and slightly wider than this slice needs: we own the item's durability outright, so any
     * other vanilla source charging it would be a second, unaccounted wear path.
     */
    @EventHandler(ignoreCancelled = true)
    public void onShieldItemDamage(PlayerItemDamageEvent event) {
        if (ShieldItems.shieldId(event.getItem(), adapters.keys()).isPresent()) {
            event.setCancelled(true);
        }
    }

    /**
     * VANILLA HEALS DO NOT MOVE A TRACKED PLAYER'S BAR WITHOUT MOVING THE TRUTH.
     *
     * <p>We own passive regeneration now ({@link HealthRegenSystem}), so vanilla's own must go. But
     * the wider reason applies to every vanilla heal, not only the two being replaced: the vanilla
     * health attribute is a DISPLAY that {@code HeartBarRenderer} rewrites from the custom numbers on
     * the next {@code HealthChange} or reconcile tick. A vanilla heal that lands is therefore visible
     * for a fraction of a second and then silently reverted -- which reads to a player as a bug,
     * because it is one.
     *
     * <p><b>Cancelling is only half the job, and the half that would have made things worse alone.</b>
     * A cancelled healing potion is a SILENT NO-OP: a clean-looking bug that heals zero, worse by this
     * codebase's standards than the visible flicker it replaced. So the potion reasons are cancelled
     * AND translated, in this same handler. Never cancel a heal you are not ready to replace.
     *
     * <p>The classification lives in {@link VanillaHealPolicy} rather than here, because it is the
     * only part of this handler a unit test can reach -- and it is exhaustive over all nine
     * {@code RegainReason} constants with no default arm, so a tenth is a compile error rather than a
     * silent fall-through. See that class for why {@code EATING} is rerouted rather than passed.
     *
     * <p><b>Scope: tracked players only.</b> A mob's health is its own store's business and no vanilla
     * heal is currently rewriting it; an untracked player is one between join and register, whose
     * store read would throw. So both fall through untouched rather than being handled wrongly.
     *
     * <p>This cannot eat our own heals. Nothing here calls {@code Player#setHealth} through a path
     * that fires this event -- the renderer writes the attribute and the health directly, which the
     * API does not report as a regain -- so the cancel can only ever be catching vanilla.
     */
    @EventHandler(ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        UUID id = player.getUniqueId();
        if (!adapters.stats().tracks(id)) return;

        VanillaHealPolicy.Action action = VanillaHealPolicy.forReason(event.getRegainReason());
        if (action == VanillaHealPolicy.Action.PASS) return;

        event.setCancelled(true);
        if (action == VanillaHealPolicy.Action.REROUTE) {
            // The event's amount is in vanilla HEALTH POINTS, on a bar whose scale is a function of
            // this player's custom max. HeartScale.customFromHealthPoints is the inverse of the
            // renderer's own mapping, so a 4-point potion is worth two hearts of whatever bar they
            // have -- 20% of max -- rather than a flat 4 HP that a Growth-raised ceiling would make
            // worthless. Self-attributed: the event names no healer.
            double custom = HeartScale.customFromHealthPoints(event.getAmount(), adapters.stats().max(id));
            if (custom > 0) adapters.stats().heal(id, custom, id, true);
        }
    }


    /**
     * VANILLA owns melee knockback -- gated to the hit that earned it.
     *
     * <p>Basic melee wants vanilla's exact feel: the base push, the small upward pop, and the
     * sprint-hit bonus. Vanilla derives all three itself, taking the sprint bonus from the attacker's
     * state at hit time, so the cheapest and most faithful way to get them is to NOT CANCEL. Nothing
     * custom is re-derived, and no melee weapon needs to declare a knockback effect. This replaces
     * the design's older "always cancel vanilla KB, then apply the declared one", which left melee
     * pushing nothing at all because no shipped weapon declares one.
     *
     * <p>THE GATE releases the push on exactly the hit that claimed the {@link MeleeHits} window, so
     * knockback keeps the same once-per-10-tick cadence as the damage.
     *
     * <p><b>It is a SAFETY NET, not the thing that stops spam knockback -- do not describe it as
     * that.</b> The gate was written expecting a windowed-out click to reach vanilla's knockback and
     * shove a mob for zero damage, the knockback analog of the spam-flash. The 2026-08-28 boot
     * produced that exact shape and disproved it: at tick 12170 a windowed-out re-hit DID reach this
     * rider -- an ATTACK line with no CLAIMED -- and raised no knockback event at all. Vanilla
     * suppresses the re-hit's push itself, upstream, before this handler is ever consulted. So for a
     * SINGLE attacker the cancel branch never fires, and crediting this gate with vanilla's work
     * would be a lie in the comment.
     *
     * <p>It stays because one boot with one attacker did not disprove the cases it covers: co-op,
     * where a second player's refused click is a separate attack vanilla has no reason to suppress;
     * a desync where external damage moves the victim's state out from under our window; and a Paper
     * version where re-hits do knock. Cheap, and correct in all of them. Not to be removed on the
     * strength of a single-attacker boot.
     *
     * <p>{@code landedThisTick} is tick-EXACT, not "the window is open": a mob hit three ticks ago
     * still has an open window, and reading that would leak a push to the very spam-click this
     * exists to refuse.
     *
     * <p>ORDERING is what makes the gate work, and it is vanilla's own: the damage event fires from
     * inside {@code hurt()} and knockback is applied after it returns, within one synchronous
     * {@code Player#attack}. So the rider's claim is already recorded when we are asked. It fails
     * LOUD rather than green -- were the order reversed, the signal would never be present and melee
     * would push nothing, which is the first thing a boot notices.
     *
     * <p>The query does not consume, and the boot proved that is load-bearing rather than cautious.
     * A single hit can raise TWO ENTITY_ATTACK knockback events: measured 2026-08-28, eleven
     * non-sprint hits raised one each and three of four SPRINT hits raised two. A consume-on-read
     * signal would have cancelled the second and eaten the sprint bonus. Every event observed
     * arrived as {@code EntityKnockbackByEntityEvent} with cause ENTITY_ATTACK -- a subclass, which
     * reaches this handler because neither it nor {@code EntityPushedByEntityAttackEvent} declares
     * its own HandlerList.
     *
     * <p>Left alone: knockback on PLAYERS (mob->player stays vanilla, the standing Pass 2 decision)
     * and every non-attack cause -- explosions, sweep -- which were never ours to own. SWEEP is now
     * MEASURED rather than assumed: the 2026-08-28 sweep boot logged every knockback cause above this
     * gate and saw 9 events with cause SWEEP_ATTACK (class EntityKnockbackByEntityEvent, the same
     * subclass ENTITY_ATTACK arrives as). They return on the line below and reach vanilla ungated,
     * which is what gives a swept mob its little shove. So owning sweep DAMAGE needed no second cause
     * here -- verified, not inferred. Mob->mob
     * ENTITY_ATTACK knockback stays cancelled, because this handler keys on the knocked entity and
     * never sees an attacker; that is unchanged by this pass, and recorded in NEXT.md rather than
     * fixed here.
     */
    @EventHandler
    public void onCombatKnockback(EntityKnockbackEvent event) {
        if (event.getCause() != EntityKnockbackEvent.Cause.ENTITY_ATTACK) return;
        if (event.getEntity() instanceof Player) return;                 // player->mob only
        if (meleeHits.landedThisTick(event.getEntity().getUniqueId())) return;  // the hit that earned it
        event.setCancelled(true);
    }

    /** Ranged: a frozen mob looses nothing -- a skeleton frozen mid-draw never fires. */
    @EventHandler
    public void onFrozenProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Entity shooter && isFrozen(shooter)) {
            event.setCancelled(true);
        }
    }

    /** Creeper: detonation is an attack -- a frozen creeper does not explode (the per-tick fuse
        reset pauses the swell; this is the guaranteed no-boom backstop). */
    @EventHandler
    public void onFrozenExplosionPrime(ExplosionPrimeEvent event) {
        if (isFrozen(event.getEntity())) event.setCancelled(true);
    }

    /**
     * Movement suppression for the teleport class: an immobilized mob (Rooted OR Freeze) cannot
     * teleport away -- a frozen enderman stays put even when hit (getting hit is what triggers its
     * teleport). Only REAL (large) teleports are cancelled, so the immobilize's own sub-block anchor
     * corrections -- which also fire this event -- pass through and aren't self-cancelled.
     */
    @EventHandler
    public void onImmobilizedTeleport(EntityTeleportEvent event) {
        Location from = event.getFrom(), to = event.getTo();
        if (to == null) return;
        boolean immobilized = isImmobilized(event.getEntity());
        if (!from.getWorld().equals(to.getWorld())) {          // cross-world is always a real teleport
            if (immobilized) event.setCancelled(true);
            return;
        }
        double minSq = ImmobilizePhysics.MIN_TELEPORT * ImmobilizePhysics.MIN_TELEPORT;
        if (ImmobilizePhysics.suppressTeleport(immobilized, from.distanceSquared(to), minSq)) {
            event.setCancelled(true);
        }
    }

    /**
     * The source-level movement stop for immobilized mobs: veto the translation BEFORE it commits.
     * A strafing skeleton applies its move via deltaMovement during its own tick, after our per-tick
     * velocity-zero has run -- so the zero is stale and the move commits (the creep). Teleporting it
     * back after fights a lost battle (creep-then-snap). EntityMoveEvent fires before the move
     * applies and is source-agnostic (MoveControl, navigation, momentum all funnel through it), so
     * pinning the position here means the mob never moves -- zero creep, nothing to snap back from.
     *
     * This handler is on the hot path -- EntityMoveEvent fires for EVERY moving living entity every
     * tick -- so the bail-out is cheapest-first: a hasChangedPosition() field check, then an O(1)
     * concurrent-map get in isImmobilized() (on the small set of currently-immobilized mobs, not a
     * scan). Rotation-only moves are let through so the mob still turns to face and aim.
     */
    @EventHandler
    public void onImmobilizedMove(EntityMoveEvent event) {
        if (!event.hasChangedPosition()) return;         // cheapest: no translation -> nothing to veto (mob may still aim)
        if (!isImmobilized(event.getEntity())) return;   // O(1) map get on the immobilized set
        Location from = event.getFrom(), to = event.getTo();
        // Zero tolerance: veto ANY translation (keep from x/z, cap y so a hop can't rise, allow
        // falling). Keep the mob's INTENDED facing (to yaw/pitch) so a rooted archer still shoots.
        double[] fix = ImmobilizePhysics.correction(to.getX(), to.getY(), to.getZ(),
                from.getX(), from.getY(), from.getZ(), 0.0);
        if (fix != null) {
            event.setTo(new Location(to.getWorld(), fix[0], fix[1], fix[2], to.getYaw(), to.getPitch()));
        }
    }

    /** Frozen only: attack suppression is a Freeze mechanic. */
    private boolean isFrozen(Entity entity) {
        return adapters.freeze().isImmobilized(entity.getUniqueId());
    }

    /** Rooted OR Freeze: movement suppression (teleport) belongs to both immobilize configs. */
    private boolean isImmobilized(Entity entity) {
        return adapters.immobilize().isImmobilized(entity.getUniqueId())
                || adapters.freeze().isImmobilized(entity.getUniqueId());
    }
}
