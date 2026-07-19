package io.github.butterflysmp.rpg.core.combat.stat;

import java.util.Set;

/**
 * One stat's modifier surface, as {@link ModifierReconciler} sees it: the sources currently applied,
 * and the two transitions the diff drives -- set/replace one source, or clear one -- each reporting
 * whether it actually changed the resolved value.
 *
 * This is the seam that lets the leak-proof reconcile diff be written ONCE and reused per stat.
 * {@link HealthState} exposes one target per stat: max HP (whose set clamps current on a decrease)
 * and attack damage (a plain {@link Stat}, no current to clamp). The reconciler never learns which
 * stat it is converging -- it only removes sources absent from the desired set and sets the rest,
 * exactly as it did for max HP alone.
 *
 * Package-private: only the stat package reconciles, and only {@link HealthState} implements it.
 */
interface ModifierTarget {

    /** The sources currently contributing a modifier. A snapshot copy, safe to iterate while mutating. */
    Set<String> sources();

    /** Add or replace {@code source}'s modifier; true if the resolved value actually changed. */
    boolean setModifier(String source, double amount);

    /** Remove {@code source}'s modifier if present; true if one was actually removed. */
    boolean clearModifier(String source);
}
