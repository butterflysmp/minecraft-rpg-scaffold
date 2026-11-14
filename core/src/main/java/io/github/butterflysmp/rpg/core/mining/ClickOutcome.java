package io.github.butterflysmp.rpg.core.mining;

/**
 * What a click did. A sealed interface: these are the only kinds, so a {@code switch} over a
 * {@code ClickOutcome} is checked by the compiler for a missing case.
 *
 * <p>Sounds are the paper side's (S1-b), from the ore's content: a {@link Miss} plays the miss
 * sound, a {@link Broke} plays the break sound, {@link Uncharged} and {@link NotLive} play nothing,
 * and {@link Denied} sends an action-bar line (the seat's ruling 4, 2026-11-13).
 */
public sealed interface ClickOutcome {

    /** The patch is broken and regrowing. Nothing changes. */
    enum NotLive implements ClickOutcome { INSTANCE }

    /** Not holding a pickaxe. Nothing changes. Ben: "Yes, denied if not holding a pickaxe". */
    enum Denied implements ClickOutcome { INSTANCE }

    /** The aim is outside the live spot's box. Nothing changes. */
    enum Miss implements ClickOutcome { INSTANCE }

    /** The aim is in the live spot's box, but the swing was not fully charged. Nothing changes. */
    enum Uncharged implements ClickOutcome { INSTANCE }

    /** The live spot cracked, and the next was revealed. Indices are 0-based, in reveal order. */
    record Hit(int crackedIndex, int revealedIndex) implements ClickOutcome {}

    /** The last spot cracked and the patch broke, paying {@code payout}. */
    record Broke(int crackedIndex, Payout payout) implements ClickOutcome {}
}
