package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.accessory.AccessorySlots;

/**
 * THE ONE negative bound for a stat, counted across BOTH systems that can move it (PLAN-build-system.md
 * section 2.3, amendment 3). {@code AccessoryNegatives.refusal} reads it; nothing else restates it.
 *
 * <h2>The arithmetic</h2>
 *
 * <pre>
 *   sources that can move one stat      4 accessory slots + 4 fragment slots        = 8
 *   sources that can move it DOWN       4 accessory slots + 4 fragment slots x 0    = 4
 *   a bounded drawback is accepted iff  NEGATIVE_SOURCES x |amount| &lt; the player's base
 * </pre>
 *
 * <p>Fragments contribute ZERO negative sources because they are positive-only (ruling 21, 2026-09-26). The
 * plan had the bound at 8x; measured against the shipped accessories, 8x would have refused
 * {@code fletchers_quiver} (health_regen -0.04: 0.32 against 0.2) and {@code sages_scroll} (crit_chance
 * -0.03: 0.24 against 0.15), both ruled numbers. <b>If fragments ever gain drawbacks,
 * {@link #FRAGMENT_NEGATIVE_SOURCES} becomes {@code FragmentSlots.COUNT} and every shipped drawback is
 * re-measured</b> -- {@code StatSourceBoundTest} and {@code AccessoryNegativesTest} redden if not.
 *
 * <p>ONE bound, not one per system: two 4x bounds would each pass while the pair reached the clamp.
 */
public final class StatSourceBound {

    private StatSourceBound() {}

    /** Every accessory slot can carry a (bounded) negative. */
    public static final int ACCESSORY_NEGATIVE_SOURCES = AccessorySlots.COUNT;

    /** No fragment slot can: fragments are positive-only (ruling 21). */
    public static final int FRAGMENT_NEGATIVE_SOURCES = 0;

    /** How many copies of one drawback can land on one stat at once. */
    public static final int NEGATIVE_SOURCES = ACCESSORY_NEGATIVE_SOURCES + FRAGMENT_NEGATIVE_SOURCES;

    /** Whether {@link #NEGATIVE_SOURCES} copies of {@code amount} stay strictly above the floor of {@code base}. */
    public static boolean withinBound(double amount, double base) {
        return !(amount < 0) || NEGATIVE_SOURCES * -amount < base;
    }
}
