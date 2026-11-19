package io.github.butterflysmp.rpg.core.fight;

import java.util.OptionalInt;

/**
 * THE REVIVE'S TWO NUMBERS (step F9e d and e), each ONE rule so a later modifier changes one place.
 * <ul>
 *   <li><b>The limit</b> (Ben: "No but we will add modifiers to change that later"): revives are counted per player per
 *       fight; {@code max_revives} unset means unlimited. No modifier system exists yet.</li>
 *   <li><b>The health</b> (Ben: "2. 50% health"): a fraction of his maximum.</li>
 * </ul>
 */
public final class ReviveRules {

    private ReviveRules() {}

    /** Whether a player revived {@code revivesSoFar} times in this fight may be revived again. */
    public static boolean mayRevive(int revivesSoFar, OptionalInt maxRevives) {
        return maxRevives.isEmpty() || revivesSoFar < maxRevives.getAsInt();
    }

    /** His health on revive: {@code fraction} of his maximum, the fraction in (0, 1]. */
    public static double revivedHealth(double maxHealth, double fraction) {
        if (!(fraction > 0.0) || fraction > 1.0) {
            throw new IllegalArgumentException("the revived fraction must be above 0 and at most 1: " + fraction);
        }
        return maxHealth * fraction;
    }
}
