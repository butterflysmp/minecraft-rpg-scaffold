package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;

import java.util.Map;
import java.util.UUID;

/**
 * Converging both mana stats and stamping the pool once if either moved.
 *
 * <h2>Why this is a class rather than four lines in the reconcile loop</h2>
 *
 * Every argument is already a {@code core} type, so this unit-tests against REAL objects with no
 * fakes -- and the four lines it replaces contain a trap that no test could otherwise reach:
 *
 * <pre>{@code
 * if (stats.reconcileMaxManaModifiers(...) || stats.reconcileManaRegenModifiers(...))  // WRONG
 * }</pre>
 *
 * {@code ||} short-circuits. Whenever the ceiling changed, the RATE reconcile would never run, so
 * mana-regen modifiers would silently stop converging -- a piece equipped in the same tick as a Mana
 * Bank piece would never register, and one removed would never be dropped. It looks correct, it
 * compiles, and inside the paper loop nothing can observe it. Here it is a mutation row.
 *
 * <h2>ONE current, so ONE stamp</h2>
 *
 * Mana has a single current value, and BOTH of these stats govern its trajectory -- the ceiling it
 * approaches and the slope it approaches at. So a change to either pins it, once:
 *
 * <ul>
 *   <li><b>The ceiling ROSE</b> -- the pre-change amount is pinned, so the ceiling moves and the
 *       amount does not. Headroom, never a free top-up.
 *   <li><b>The ceiling FELL</b> -- {@code setCurrent}'s own clamp is the unequip clamp.
 *   <li><b>The RATE changed</b> -- the pin re-stamps {@code asOfTick}, so the ticks already elapsed
 *       keep the rate they were earned at and the new rate applies forward only. Without it,
 *       {@code amount + elapsed * rate} re-prices the past: a player empty for twelve seconds has
 *       accrued 20 mana, and equipping a rate-doubler makes the very next read say 40.
 * </ul>
 *
 * <p><b>Only on a real transition.</b> The reconcile loop runs four times a second; a pin every tick
 * would re-stamp {@code asOfTick} forever, so {@code elapsed} would never grow and mana would stop
 * regenerating ENTIRELY -- silently, with a stat block that still reads correctly. {@code NEXT.md}
 * records that failure for the ceiling; the rate joins it here.
 */
public final class ManaTransition {

    private ManaTransition() {}

    /**
     * Converge both mana stats for {@code id} and pin the pool if either actually moved.
     *
     * <p>The reading is taken BEFORE either reconcile, so it reflects the old ceiling and the old
     * rate -- that is what makes the pin the boundary between "earned under the old terms" and
     * "earned under the new ones". A no-op on an untracked combatant: both reconciles report no
     * change, so nothing is written.
     *
     * @return true if the pool was stamped
     */
    public static boolean reconcile(CombatantStats stats, ResourcePool pool, UUID id,
                                    String resourceId,
                                    Map<String, Double> desiredMax, Map<String, Double> desiredRegen) {
        double before = pool.current(id, resourceId);

        // BOTH, into locals, ALWAYS. Never `a || b` -- see the class javadoc. Assigning first is not
        // a style preference; it is the difference between the rate stat converging and not.
        boolean maxChanged = stats.reconcileMaxManaModifiers(id, desiredMax);
        boolean regenChanged = stats.reconcileManaRegenModifiers(id, desiredRegen);

        if (!maxChanged && !regenChanged) return false;
        pool.setCurrent(id, resourceId, before);
        return true;
    }
}
