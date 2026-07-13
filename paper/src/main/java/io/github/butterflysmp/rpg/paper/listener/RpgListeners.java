package io.github.butterflysmp.rpg.paper.listener;

import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponService;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.ImmobilizePhysics;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.weapon.WeaponFire;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

/**
 * The single Bukkit Listener. Registered once, in RpgPlugin.
 *
 * Resist adding a second one. Every handler here should be a thin adapter that
 * hands the event to something that does the actual work; the logic belongs there.
 */
public final class RpgListeners implements Listener {

    private final CooldownTracker cooldowns;
    private final ResourcePool resources;
    private final ProfileService profiles;
    private final WeaponRegistry weapons;
    private final WeaponService weaponService;
    private final AdapterContext adapters;

    public RpgListeners(CooldownTracker cooldowns, ResourcePool resources, ProfileService profiles,
                        WeaponRegistry weapons, WeaponService weaponService, AdapterContext adapters) {
        this.cooldowns = cooldowns;
        this.resources = resources;
        this.profiles = profiles;
        this.weapons = weapons;
        this.weaponService = weaponService;
        this.adapters = adapters;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Returns immediately; the read happens on the storage I/O thread.
        profiles.onJoin(event.getPlayer().getUniqueId());
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
