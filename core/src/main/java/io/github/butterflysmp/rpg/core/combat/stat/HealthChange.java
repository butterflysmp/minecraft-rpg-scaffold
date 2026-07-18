package io.github.butterflysmp.rpg.core.combat.stat;

import java.util.UUID;

/**
 * The observability seam: a health mutation, made visible. Emitted by {@link CombatantStats}
 * whenever a combatant's current or max changes, and consumed by whatever needs to react.
 *
 * This phase, the displays consume it -- the player heart bar refreshes on current (fill) and max
 * (heart count), and next phase the mob nameplate rebuilds its text the same way. NEXT phase, the
 * damage-number popup hooks this exact event to render per-dealer floating numbers, which is why
 * {@code dealer} and {@code dealerIsPlayer} are carried NOW even though nothing renders them yet:
 * building the seam complete means the popup attaches without reopening the engine.
 *
 * {@code dealerIsPlayer} reuses the same faction bit as {@code CombatantSnapshot.player} -- captured
 * by the caller, who holds the snapshot, and frozen here. If health mutated silently, every future
 * consumer (popup, combat log, aggro) would need the engine reopened; it does not.
 *
 * {@code reachedZero} is the death hook, built now and UNCONSUMED this phase: it is true exactly on
 * the DAMAGE hit that brings custom current to 0 (the transition, once -- never on a later hit to an
 * already-0 target). Nothing reacts to it yet; the next-phase death system hooks {@code reachedZero
 * -> die} without reopening the damage path. HEAL and MAX_CHANGE always carry false.
 *
 * @param target         who was affected
 * @param targetIsPlayer whether the target is a player (the heart bar renders only for players)
 * @param kind           what kind of change this was
 * @param amount         the magnitude of a DAMAGE or HEAL; 0 for a pure MAX_CHANGE
 * @param dealer         who caused it, for a per-dealer popup and combat credit; null if unattributed
 * @param dealerIsPlayer whether the dealer was a player -- so the popup can target that player's screen
 * @param newCurrent     the target's custom current health AFTER the change
 * @param max            the target's custom max health AFTER the change
 * @param reachedZero    true only on the DAMAGE hit that brings current to 0 (the death hook; unconsumed)
 */
public record HealthChange(UUID target, boolean targetIsPlayer, Kind kind, double amount,
                           UUID dealer, boolean dealerIsPlayer, double newCurrent, double max,
                           boolean reachedZero) {

    public enum Kind {
        /** Current health fell. */
        DAMAGE,
        /** Current health rose. */
        HEAL,
        /** Max health changed (a modifier was added, removed, or altered); current may have been clamped. */
        MAX_CHANGE
    }
}
