package io.github.butterflysmp.rpg.paper.build;

import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.build.LoadoutResolution.Equipped;
import io.github.butterflysmp.rpg.core.build.LoadoutSlot;
import io.github.butterflysmp.rpg.core.build.RecastTracker;
import io.github.butterflysmp.rpg.core.build.StoneInput;
import io.github.butterflysmp.rpg.core.build.SwingGuard;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.PaperCombatWorld;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.weapon.DashAim;
import io.github.butterflysmp.rpg.paper.weapon.ViewAim;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turns an Ability Stone input into a cast (PLAN-build-system.md section 2.5).
 *
 * <p>Every input goes through here, and here only decides nothing: {@link StoneInput} picks the slot,
 * {@link SwingGuard} vets a swing, the loadout names the ability, and the EXISTING
 * {@link AbilityService#cast} checks the cooldown, spends the mana and starts the cooldown. So:
 * <ul>
 *   <li>the cooldown key is the bare ability id, shared with {@code /rpg cast};</li>
 *   <li><b>a held input re-casts whenever the cooldown allows (ruling 19)</b> -- each input simply
 *       tries, and {@code resolve} refuses until the cooldown ends, so holding cannot cast faster
 *       than the cooldown;</li>
 *   <li>a refusal is an action-bar line ({@link StoneNotice}), never chat.</li>
 * </ul>
 *
 * <p><b>THREADING.</b> Every entry point is called from a Bukkit event handler on the player's own
 * thread, and the deferred swing decision is {@code Scheduler.onEntityLater}. Nothing here runs on a
 * Netty thread. The drop-attempt ticks are per player, in memory, and dropped on quit (CLAUDE.md
 * invariant 3: no persistent static player state).
 */
public final class StoneCaster {

    private final AbilityService abilityService;
    private final AdapterContext adapters;
    private final ProfileService profiles;
    private final CooldownTracker cooldowns;
    private final Stones stones;

    /** Each player's most recent stone DROP ATTEMPT tick, for the Q-swing guard. */
    private final Map<UUID, Long> lastDropAttempt = new ConcurrentHashMap<>();

    /**
     * Each player's RECAST state (section 7.4): a hold per button and the open window. Fed EVERY input that
     * names an ability, refused ones included -- a held click's refused inputs are what carry its hold.
     */
    private final Map<UUID, RecastTracker> recasts = new ConcurrentHashMap<>();

    public StoneCaster(AbilityService abilityService, AdapterContext adapters, ProfileService profiles,
                       CooldownTracker cooldowns, Stones stones) {
        this.abilityService = abilityService;
        this.adapters = adapters;
        this.profiles = profiles;
        this.cooldowns = cooldowns;
        this.stones = stones;
    }

    /**
     * A right-click or a Q with the stone. Decided and cast now: neither has a same-tick twin to wait for.
     *
     * @param mainHand   the event came from the main hand
     * @param screenOpen the input came through an open inventory screen
     */
    public void onInput(Player player, StoneInput.Input input, boolean mainHand, boolean screenOpen) {
        onInput(player, input, mainHand, screenOpen, Bukkit.getCurrentTick());
    }

    /** {@link #onInput}, for an input that ARRIVED at {@code inputTick} -- a swing decided a tick later. */
    private void onInput(Player player, StoneInput.Input input, boolean mainHand, boolean screenOpen, long inputTick) {
        StoneInput.slotFor(input, mainHand, screenOpen).ifPresent(slot -> cast(player, slot, inputTick));
    }

    /**
     * A stone drop attempt -- a Q from the hotbar, or a refused click on the stone in a screen. Recorded
     * so a swing in the SAME tick does not also cast Active 1 (measured: every Q comes with one).
     */
    public void recordDropAttempt(Player player) {
        lastDropAttempt.put(player.getUniqueId(), (long) Bukkit.getCurrentTick());
    }

    /**
     * A main-hand swing with the stone: a left click. NOT cast now -- decided ONE TICK LATER, because an
     * in-screen Q's swing arrives BEFORE its drop click in the same tick (measured, section 3.1.0.1), and
     * only a late decision sees both. The cost is 1 tick (50 ms) of latency on Active 1.
     */
    public void onSwing(Player player) {
        long swingTick = Bukkit.getCurrentTick();
        adapters.scheduler().onEntityLater(player, () -> {
            if (!player.isOnline()) return;
            long lastDrop = lastDropAttempt.getOrDefault(player.getUniqueId(), SwingGuard.NEVER);
            if (!SwingGuard.castsActive1(swingTick, lastDrop)) return;
            // Still holding the stone? A swing that was the last act before switching slots must not
            // cast from whatever is in the hand now.
            if (!StoneItems.isStone(player.getInventory().getItemInMainHand(), adapters.keys())) return;
            // The SWING's tick, not this deferred one: the recast window and the hold are measured in input ticks.
            onInput(player, StoneInput.Input.LEFT, true, false, swingTick);
        }, 1);
    }

    public void forget(UUID playerId) {
        lastDropAttempt.remove(playerId);
        recasts.remove(playerId);
    }

    /** Death closes any recast window: the respawned player did not cast what it was opened for. */
    public void onDeath(UUID playerId) {
        RecastTracker tracker = recasts.get(playerId);
        if (tracker != null) tracker.clear();
    }

    private void cast(Player player, LoadoutSlot slot, long inputTick) {
        UUID playerId = player.getUniqueId();
        Optional<io.github.butterflysmp.rpg.storage.PlayerProfile> profile = profiles.profile(playerId);
        Optional<Equipped> equipped = stones.equippedFor(playerId, profile);
        if (equipped.isEmpty()) {
            StoneNotice.noLoadout(player, cooldowns);
            return;
        }
        // A SAVED loadout may leave a slot empty (an id the pool no longer offers there -- LoadoutResolution).
        // Nothing to cast is said on the action bar, like every other refusal here.
        Optional<String> abilityId = equipped.get().idFor(slot);
        if (abilityId.isEmpty()) {
            StoneNotice.emptySlot(player, cooldowns);
            return;
        }
        Set<String> castable = Set.copyOf(equipped.get().ids());

        // THE RECAST (section 7.4), decided BEFORE the ordinary cast. Every input is recorded first, refused or not.
        // An accepted recast casts the follow-up through castUnchecked -- no cost, no cooldown check, no cooldown
        // of its own (ruling 30) -- and the ordinary cast is not attempted. A refused one (too early, too late,
        // already used, or the hold that cast the target) falls through to it, and the player sees the target's
        // own cooldown line.
        RecastTracker tracker = recasts.computeIfAbsent(playerId, id -> new RecastTracker());
        Optional<String> followUp = tracker.input(slot, inputTick, abilityId.get(),
                stones.recastFor(playerId, profile, abilityId.get()).isPresent());

        // The /rpg cast shape exactly: aim and snapshot on the caster's own thread, decide inline so the
        // cooldown and mana are spent before any hop, then run the effects on the region that owns the aim.
        Location eye = player.getEyeLocation();
        Aim aim = ViewAim.of(eye);
        CombatantSnapshot caster = BukkitCombatant.snapshot(player, adapters.stats());
        if (followUp.isPresent()) {
            switch (abilityService.castUnchecked(caster, followUp.get(), aim)) {
                case CastResult.Success success -> run(player, eye, success);
                // The loader refuses a recast naming no loaded ability, so this is a registry that changed after.
                case CastResult.UnknownAbility unknown ->
                        adapters.warnOnce("Ability Stone: recast names unknown ability '" + unknown.id() + "'");
                default -> { }
            }
            return;
        }
        // THE PLAYER'S DERIVE (slice 5): their active aspects' changes, applied before the cost and cooldown, and
        // their behaviour fragments' appends (section 7.3).
        CastResult result = abilityService.cast(caster, abilityId.get(), aim, castable,
                stones.deriveFor(playerId, profile));

        switch (result) {
            case CastResult.Success success -> {
                // A recast aspect active on this ability opens its window; a cast of it without one closes any.
                tracker.castSucceeded(abilityId.get(), inputTick, stones.recastFor(playerId, profile, abilityId.get()));
                run(player, eye, success);
            }
            case CastResult.OnCooldown onCooldown ->
                    StoneNotice.onCooldown(player, cooldowns, displayName(abilityId.get()), onCooldown.ticksRemaining());
            case CastResult.InsufficientResource lacking ->
                    StoneNotice.notEnoughMana(player, cooldowns, displayName(abilityId.get()),
                            lacking.required(), lacking.available());
            // A pool naming an ability nothing defines is refused at load (PoolLoader), so this is a
            // registry that changed after the pools loaded -- nothing in this build does that.
            case CastResult.UnknownAbility unknown ->
                    adapters.warnOnce("Ability Stone: loadout names unknown ability '" + unknown.id() + "'");
            // castable IS the loadout the id came from, so Locked cannot arise here.
            case CastResult.Locked ignored -> { }
            // Weapon-only results: minted by WeaponFire off a held weapon, which the stone is not.
            case CastResult.Broken ignored -> { }
            case CastResult.Empty ignored -> { }
            case CastResult.Reloading ignored -> { }
        }
    }

    /** Aim-correct a dash, then run the effects on the region that owns the aim. */
    private void run(Player player, Location eye, CastResult.Success success) {
        CastResult.Success toRun = DashAim.resolve(player, success);
        adapters.scheduler().onRegion(eye, () ->
                new CastExecutor(new PaperCombatWorld(player.getWorld(), adapters)).execute(toRun));
    }

    private String displayName(String abilityId) {
        return stones.abilities().find(abilityId).map(def -> def.displayName()).orElse(abilityId);
    }
}
