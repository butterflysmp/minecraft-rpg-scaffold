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
 * @param wasCrit        whether this DAMAGE was a critical hit -- the bit the displays cannot derive,
 *                       because the roll happened on the dealer's thread a tick earlier and the
 *                       amount alone cannot say whether it was doubled
 * @param element        the element this DAMAGE wore ("fire", "kinetic", ...), or null for none. The
 *                       displays cannot derive it: the number's glyph is a content fact only
 *                       {@code ElementRegistry} holds, and by the time a change reaches a listener the
 *                       payload that named the element is three hops upstream. Null is the honest
 *                       value for every non-payload path -- the thorns reflect, the vanilla-damage
 *                       boundary, and scorch's own burn tick.
 * @param beforeDefense  the DAMAGE offered BEFORE the target's defence; {@code amount} is what landed. Equal to
 *                       {@code amount} for every change nothing mitigated (HEAL, MAX_CHANGE, a bypassed hit). Added
 *                       in dragon-mobs step R3 for the selftest's damage line (GATE-dragon-fix.md D5): the store is
 *                       the one place both numbers exist, and recomputing the curve elsewhere would be a second site.
 * @param throughDefense true for a DAMAGE that went THROUGH the target's Defense ({@code DefenseRule.APPLIES}), false for
 *                       a bypassed hit and for every HEAL and MAX_CHANGE. Added 2026-11-25 (PLAN-armor-sets.md, D2'):
 *                       a worn armor piece wears on exactly the hits Defense prices, and the store is the one place
 *                       that knows which rule a hit used.
 */
public record HealthChange(UUID target, boolean targetIsPlayer, Kind kind, double amount,
                           UUID dealer, boolean dealerIsPlayer, double newCurrent, double max,
                           boolean reachedZero, boolean wasCrit, String element, double beforeDefense,
                           boolean throughDefense) {

    /** Not through Defense: every construction before 2026-11-25 (HEAL, MAX_CHANGE, a bypassed hit, the tests). */
    public HealthChange(UUID target, boolean targetIsPlayer, Kind kind, double amount,
                        UUID dealer, boolean dealerIsPlayer, double newCurrent, double max,
                        boolean reachedZero, boolean wasCrit, String element, double beforeDefense) {
        this(target, targetIsPlayer, kind, amount, dealer, dealerIsPlayer, newCurrent, max, reachedZero, wasCrit,
                element, beforeDefense, false);
    }

    /** The form before step R3: nothing mitigated, so before equals the amount. Every older site reads unchanged. */
    public HealthChange(UUID target, boolean targetIsPlayer, Kind kind, double amount,
                        UUID dealer, boolean dealerIsPlayer, double newCurrent, double max,
                        boolean reachedZero, boolean wasCrit, String element) {
        this(target, targetIsPlayer, kind, amount, dealer, dealerIsPlayer, newCurrent, max,
                reachedZero, wasCrit, element, amount);
    }

    /**
     * A DAMAGE change wearing no element. Keeps every construction site that predates elements
     * reading unchanged, and -- more than convenience -- it is half of the structural loop guard:
     * an elementless change cannot accrue a status, and this is the form the burn tick's path builds.
     */
    public HealthChange(UUID target, boolean targetIsPlayer, Kind kind, double amount,
                        UUID dealer, boolean dealerIsPlayer, double newCurrent, double max,
                        boolean reachedZero, boolean wasCrit) {
        this(target, targetIsPlayer, kind, amount, dealer, dealerIsPlayer, newCurrent, max,
                reachedZero, wasCrit, null);
    }

    /**
     * A DAMAGE change that was not a crit -- HEAL and MAX_CHANGE, and any damage path with no crit to
     * report. Keeps the four non-damage construction sites from having to state a flag that means
     * nothing to them.
     */
    public HealthChange(UUID target, boolean targetIsPlayer, Kind kind, double amount,
                        UUID dealer, boolean dealerIsPlayer, double newCurrent, double max,
                        boolean reachedZero) {
        this(target, targetIsPlayer, kind, amount, dealer, dealerIsPlayer, newCurrent, max,
                reachedZero, false);
    }

    public enum Kind {
        /** Current health fell. */
        DAMAGE,
        /** Current health rose. */
        HEAL,
        /** Max health changed (a modifier was added, removed, or altered); current may have been clamped. */
        MAX_CHANGE
    }
}
