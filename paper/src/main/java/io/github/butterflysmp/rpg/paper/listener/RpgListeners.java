package io.github.butterflysmp.rpg.paper.listener;

import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponService;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.ImmobilizePhysics;
import io.github.butterflysmp.rpg.paper.health.MobNameplateManager;
import io.github.butterflysmp.rpg.paper.menu.EnchantMenu;
import io.github.butterflysmp.rpg.paper.menu.Menu;
import io.github.butterflysmp.rpg.paper.health.PlayerHealthSystem;
import io.github.butterflysmp.rpg.paper.hud.StatsBarSystem;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.weapon.WeaponFire;
import io.github.butterflysmp.rpg.paper.weapon.BrokenNotice;
import io.github.butterflysmp.rpg.paper.weapon.WeaponDurability;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import io.github.butterflysmp.rpg.paper.weapon.WeaponRefresher;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

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
    private final WeaponService weaponService;
    private final AdapterContext adapters;
    private final PlayerHealthSystem healthSystem;
    private final MobNameplateManager nameplates;
    private final StatsBarSystem statsBar;

    public RpgListeners(CooldownTracker cooldowns, ResourcePool resources, ProfileService profiles,
                        WeaponRegistry weapons, WeaponService weaponService, AdapterContext adapters,
                        PlayerHealthSystem healthSystem, MobNameplateManager nameplates,
                        StatsBarSystem statsBar) {
        this.cooldowns = cooldowns;
        this.resources = resources;
        this.profiles = profiles;
        this.weapons = weapons;
        this.weaponService = weaponService;
        this.adapters = adapters;
        this.healthSystem = healthSystem;
        this.nameplates = nameplates;
        this.statsBar = statsBar;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Rebuild every carried weapon's display from current content. FIRST, so nothing downstream
        // reads a stale item -- though nothing does today: the stat reconcile loop sources attack
        // damage from the DEFINITION, not the item, which is why only the display was ever stale.
        // Content reloads only on restart and a dev restart reconnects you, so this one handler
        // covers both the stale emberblade you come back to and the live player logging in after
        // a content update. Already on the joining player's own thread; no scheduler hop needed.
        WeaponRefresher.refresh(event.getPlayer(), weapons, adapters);
        // Returns immediately; the read happens on the storage I/O thread.
        profiles.onJoin(event.getPlayer().getUniqueId());
        // Register custom health at base 100, render the heart bar, and start the equip reconcile loop.
        healthSystem.onJoin(event.getPlayer());
        // Start this viewer's per-viewer mob-nameplate LOS loop.
        nameplates.onViewerJoin(event.getPlayer());
        // Start this player's action-bar stats line.
        statsBar.onJoin(event.getPlayer());
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
     * The ONE exception is an enchanting table, which is cancelled unconditionally whatever is
     * held and whether or not you are sneaking -- the custom table replaces vanilla enchanting, so
     * the vanilla screen must never open. See the block below.
     */
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return; // FIRST: main hand only, or one click double-spends
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        // VANILLA ENCHANTING NEVER OPENS ON THIS BLOCK, sneaking or not. The custom table replaces
        // it outright, so suppressing it is unconditional and sneaking only decides what happens
        // INSTEAD of it.
        //
        // The cancel used to live inside the !isSneaking guard, which meant a sneak-right-click
        // skipped this block entirely, nothing cancelled the event, and the vanilla enchanting
        // screen opened -- the one screen this whole pass exists to replace. Sneaking suppresses a
        // container GUI only when you are holding a PLACEABLE item; with an empty hand it does
        // nothing at all, so the guard was resting on a rule that does not exist.
        //
        // Accepted consequence: you also cannot place a block against an enchanting table any more.
        // That and vanilla enchanting are both things the custom table is here to take over, and a
        // player who wants to build against one can break and re-place it.
        //
        // Ahead of WeaponFire.attempt deliberately, so the weapon never spends mana on a click
        // that opened a menu.
        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null
                && event.getClickedBlock().getType() == Material.ENCHANTING_TABLE) {
            event.setCancelled(true);
            if (!event.getPlayer().isSneaking()) {
                new EnchantMenu(event.getPlayer(), weapons, adapters, event.getClickedBlock()).open();
                return;
            }
            // Sneaking: fall through to WeaponFire.attempt so the weapon's right_click still fires
            // -- the escape hatch that keeps a Mage able to cast while standing at a table. Vanilla
            // is already cancelled above, so the table opens for neither of us.
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
        resources.clear(playerId);
        profiles.onQuit(playerId);
        // Drop custom-health state so no modifier or entry leaks across sessions.
        healthSystem.onQuit(playerId);
        // Stop the action-bar loop and drop its handle.
        statsBar.onQuit(playerId);
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
     * Ride a player's melee swing on a mob for its COSMETICS, own its mechanics. The vanilla event
     * still fires (separate from the packet-driven custom damage), so we cannot ignore it: we TOKEN
     * its damage -- kept just non-zero so the mob still flashes red + gets i-frames, but small enough
     * that it cannot double the custom number the packet path deals via applyDamage -> custom HP.
     * Its knockback is cancelled in {@link #onCombatKnockback}; custom KB is a declared effect.
     *
     * Player-initiated player->mob only. Mob->player is Pass 2 (it drains the player's custom HP and
     * carries an i-frame feel decision) -- deliberately untouched here.
     *
     * Token-can't-kill: death is deferred this phase, so the token must never drop a tracked mob to
     * <=0 while its custom HP is positive. If the token would be lethal, floor the mob's vanilla
     * health first -- the mob analog of the player heart floor. Custom HP stays the source of truth.
     */
    @EventHandler
    public void onPlayerMeleeAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;    // player-initiated
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (victim instanceof Player) return;                            // player->mob only (Pass 2 = mob->player)

        // A BROKEN weapon must be inert, and this handler is the one that would otherwise leak a
        // cosmetic hit past the gate: it never reads a weapon, so the packet path being cancelled
        // still leaves the mob flashing red and playing the hurt sound. Cancelling outright also
        // drops knockback and i-frames, which is correct -- a weapon that deals nothing should not
        // stagger anything. Scoped by weapon_id, so an untagged vanilla sword is untouched.
        if (WeaponDurability.isHeldWeaponBroken(attacker, adapters.keys())) {
            event.setCancelled(true);
            return;
        }

        event.setDamage(TOKEN_DAMAGE);                                   // flash + i-frames, no double-damage
        if (adapters.stats().tracks(victim.getUniqueId())
                && victim.getHealth() - TOKEN_DAMAGE <= 0.0) {
            var attr = victim.getAttribute(Attribute.MAX_HEALTH);
            double vanillaMax = attr == null ? victim.getHealth() : attr.getValue();
            victim.setHealth(Math.min(vanillaMax, VANILLA_LIVE_FLOOR)); // survive the token; death is next pass
        }
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

        nameplates.seedCombatStats(attacker);         // idempotent, opt-out-agnostic: seed HP + attack from vanilla
        double incoming = adapters.stats().attackValue(attacker.getUniqueId());  // the STAT, not event.getDamage()
        event.setDamage(TOKEN_DAMAGE);                // ride: keep flash/sound/i-frames, no double, can't kill
        BukkitCombatant.of(victim, adapters).handle().applyDamage(incoming, attacker.getUniqueId());
    }

    /**
     * We own knockback now. Cancel the vanilla ATTACK knockback on a mob so the declared custom KB
     * (an EffectSpec.Knockback, or none for a Mage weapon) is the only push -- the design's
     * "always cancel vanilla KB, then apply the declared one." Left alone: knockback on players
     * (mob->player is Pass 2) and non-attack causes (explosions, sweep) which aren't ours to own.
     */
    @EventHandler
    public void onCombatKnockback(EntityKnockbackEvent event) {
        if (event.getCause() != EntityKnockbackEvent.Cause.ENTITY_ATTACK) return;
        if (event.getEntity() instanceof Player) return;                 // player->mob only
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
