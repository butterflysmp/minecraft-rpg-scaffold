package io.github.butterflysmp.rpg.paper.listener;

import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponService;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.ImmobilizePhysics;
import io.github.butterflysmp.rpg.paper.health.MobNameplateManager;
import io.github.butterflysmp.rpg.paper.health.PlayerHealthSystem;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.weapon.WeaponFire;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
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

    public RpgListeners(CooldownTracker cooldowns, ResourcePool resources, ProfileService profiles,
                        WeaponRegistry weapons, WeaponService weaponService, AdapterContext adapters,
                        PlayerHealthSystem healthSystem, MobNameplateManager nameplates) {
        this.cooldowns = cooldowns;
        this.resources = resources;
        this.profiles = profiles;
        this.weapons = weapons;
        this.weaponService = weaponService;
        this.adapters = adapters;
        this.healthSystem = healthSystem;
        this.nameplates = nameplates;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Returns immediately; the read happens on the storage I/O thread.
        profiles.onJoin(event.getPlayer().getUniqueId());
        // Register custom health at base 100, render the heart bar, and start the equip reconcile loop.
        healthSystem.onJoin(event.getPlayer());
        // Start this viewer's per-viewer mob-nameplate LOS loop.
        nameplates.onViewerJoin(event.getPlayer());
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
     * handler would spend energy twice for one press.
     *
     * Vanilla is cancelled ONLY when the held weapon actually binds right_click (attempt
     * returns present). ironblade has no right_click, so its right-click passes through and
     * doors and chests still work with it in hand; only a weapon that uses the input consumes it.
     */
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return; // FIRST: main hand only, or one click double-spends
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        WeaponFire.attempt(event.getPlayer(), "right_click", weapons, weaponService, adapters)
                .ifPresent(result -> {
                    // Present == this weapon binds right_click. Suppress the vanilla interaction
                    // whether the special fired or was refused -- the player pressed the special.
                    event.setCancelled(true);
                    switch (result) {
                        case CastResult.Success ignored -> { } // already executed inside attempt()
                        // A deliberate press deserves feedback, unlike the silent left-click swing.
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
                    }
                });
    }

    /**
     * Without these clears, every player who has ever cast anything keeps a
     * cooldown and resource bucket until the server restarts. Both structures
     * are concurrent, so no scheduler hop is needed to drop them.
     *
     * Dropping the energy pool is also correct game behaviour, not just hygiene:
     * an absent pool reads as full, so a returning player starts charged.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cooldowns.clear(playerId);
        resources.clear(playerId);
        profiles.onQuit(playerId);
        // Drop custom-health state so no modifier or entry leaks across sessions.
        healthSystem.onQuit(playerId);
    }

    /**
     * Death is a setback, not a loot loss: keep inventory + XP, drop nothing. The only path that kills a
     * player is our own setHealth(0) on a custom-HP-zero (PlayerHealthSystem.onChange), so this is global
     * -- there is no vanilla-death path to scope around. Sets flags only; it never touches the store.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
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
        if (!(event.getDamager() instanceof Player)) return;             // player-initiated
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (victim instanceof Player) return;                            // player->mob only (Pass 2 = mob->player)

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
