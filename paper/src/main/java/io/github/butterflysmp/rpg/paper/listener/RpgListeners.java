package io.github.butterflysmp.rpg.paper.listener;

import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.SweepShare;
import io.github.butterflysmp.rpg.core.combat.stat.HeartScale;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.combat.ShieldExchange;
import io.github.butterflysmp.rpg.core.enchant.Thorns;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponService;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.ImmobilizePhysics;
import io.github.butterflysmp.rpg.paper.health.ArmorBarOverride;
import io.github.butterflysmp.rpg.paper.health.AttackSpeedAttributeOverride;
import io.github.butterflysmp.rpg.paper.health.MobNameplateManager;
import io.github.butterflysmp.rpg.paper.menu.CraftMatrixScreen;
import io.github.butterflysmp.rpg.paper.menu.CraftingMenu;
import io.github.butterflysmp.rpg.paper.menu.EnchantMenu;
import io.github.butterflysmp.rpg.paper.menu.Menu;
import io.github.butterflysmp.rpg.paper.health.PlayerHealthSystem;
import io.github.butterflysmp.rpg.paper.hud.StatsBarSystem;
import io.github.butterflysmp.rpg.paper.health.HealthRegenSystem;
import io.github.butterflysmp.rpg.paper.health.VanillaHealPolicy;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.core.combat.AttackCharge;
import io.github.butterflysmp.rpg.paper.weapon.MeleeHits;
import io.github.butterflysmp.rpg.paper.weapon.WeaponFire;
import io.github.butterflysmp.rpg.paper.weapon.BrokenNotice;
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
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
    private final ResourcePool resources;
    private final ProfileService profiles;
    private final WeaponRegistry weapons;
    private final ShieldRegistry shields;
    private final ArmorRegistry armor;
    private final WeaponService weaponService;
    private final AdapterContext adapters;
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

    public RpgListeners(CooldownTracker cooldowns, ResourcePool resources, ProfileService profiles,
                        WeaponRegistry weapons, ShieldRegistry shields, ArmorRegistry armor,
                        WeaponService weaponService,
                        AdapterContext adapters,
                        PlayerHealthSystem healthSystem, MobNameplateManager nameplates,
                        StatsBarSystem statsBar, HealthRegenSystem healthRegen) {
        this.cooldowns = cooldowns;
        this.resources = resources;
        this.profiles = profiles;
        this.weapons = weapons;
        this.shields = shields;
        this.armor = armor;
        this.weaponService = weaponService;
        this.adapters = adapters;
        this.healthSystem = healthSystem;
        this.nameplates = nameplates;
        this.statsBar = statsBar;
        this.healthRegen = healthRegen;

        this.hijackedBlocks = Map.of(
                Material.ENCHANTING_TABLE,
                (player, block) -> new EnchantMenu(player, weapons, shields, armor, adapters, block),
                Material.CRAFTING_TABLE,
                (player, block) -> new CraftingMenu(player, adapters));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Rebuild every carried weapon's display from current content. FIRST, so nothing downstream
        // reads a stale item -- though nothing does today: the stat reconcile loop sources attack
        // damage from the DEFINITION, not the item, which is why only the display was ever stale.
        // Content reloads only on restart and a dev restart reconnects you, so this one handler
        // covers both the stale emberblade you come back to and the live player logging in after
        // a content update. Already on the joining player's own thread; no scheduler hop needed.
        GearRefresher.refresh(event.getPlayer(), weapons, shields, armor, adapters);
        // Returns immediately; the read happens on the storage I/O thread.
        profiles.onJoin(event.getPlayer().getUniqueId());
        // Register custom health at base 100, render the heart bar, and start the equip reconcile loop.
        healthSystem.onJoin(event.getPlayer());
        // Start this viewer's per-viewer mob-nameplate LOS loop.
        nameplates.onViewerJoin(event.getPlayer());
        // Start this player's action-bar stats line.
        statsBar.onJoin(event.getPlayer());
        healthRegen.onJoin(event.getPlayer());        // start the passive regeneration loop
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
        }
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
     * The exceptions are the HIJACKED BLOCKS -- today an enchanting table and a crafting table,
     * listed in {@link #hijackedBlocks}. Each is cancelled unconditionally whatever is held and
     * whether or not you are sneaking, because our menu replaces that block's vanilla screen
     * outright and it must never open. See {@link #openHijackedBlock}.
     */
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return; // FIRST: main hand only, or one click double-spends
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

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
                        cooldowns)
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
                    }
                });
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
     */
    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu menu) {
            menu.handleClick(event);
        }
    }

    /** A drag can place items into slots the click handler never sees. Same holder, same rule. */
    @EventHandler
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
        if (WeaponDurability.maxOf(event.getResult()).isPresent()) {
            event.setCancelled(true);
        }
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
        var primary = meleeHits.primaryDamageThisTick(attacker.getUniqueId());
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
        if (adapters.stats().tracks(swept.getUniqueId())
                && swept.getHealth() - TOKEN_DAMAGE <= 0.0) {
            var attr = swept.getAttribute(Attribute.MAX_HEALTH);
            double vanillaMax = attr == null ? swept.getHealth() : attr.getValue();
            swept.setHealth(Math.min(vanillaMax, VANILLA_LIVE_FLOOR)); // the token can never kill
        }

        // The two-arg applyDamage, so the swept mob's popup is a NORMAL white number even when the
        // primary critted. Its DAMAGE still inherits the crit in full -- the stashed figure is
        // already multiplied -- so a crit swing sweeps for half of the doubled number. Only the
        // presentation differs, and deliberately: the crit was rolled for the hit the player aimed
        // at, and colouring every bystander yellow would claim each of them crit independently. The
        // visible consequence is a yellow "28" on the primary beside white "14"s on its neighbours,
        // and no crit particles on the bystanders either -- the burst is spawned on the crit bit.
        BukkitCombatant.of(swept, adapters).handle()
                .applyDamage(SweepShare.of(primary.getAsDouble(), fraction), attacker.getUniqueId());
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
        if (adapters.stats().tracks(victim.getUniqueId())
                && victim.getHealth() - TOKEN_DAMAGE <= 0.0) {
            var attr = victim.getAttribute(Attribute.MAX_HEALTH);
            double vanillaMax = attr == null ? victim.getHealth() : attr.getValue();
            victim.setHealth(Math.min(vanillaMax, VANILLA_LIVE_FLOOR)); // the token can never kill
        }

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
                .ifPresent(dealt -> meleeHits.recordPrimaryDamage(attacker.getUniqueId(), dealt));
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
