package io.github.butterflysmp.rpg.core.combat.stat;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Converges a combatant's applied max modifiers to a desired set by DIFFING, not by listening.
 *
 * This is why the equip/unequip lifecycle is structurally leak-proof. An event-driven scheme has to
 * catch every way an item can leave the hand -- drop, break, hotbar-swap, death, /clear, despawn --
 * and a single missed event LEAKS a modifier the item no longer justifies. Reconcile does not care
 * HOW an item left: the caller supplies the modifiers its currently-equipped items justify RIGHT
 * NOW, and this removes anything applied that is no longer desired. Every removal path reduces to
 * the same thing -- "that source is not in the desired set this cycle" -- so none can be missed.
 *
 * The diff also makes the transition fire ONCE, at the change, not every cycle: a source already
 * applied at the same amount is left untouched (no re-headroom, no re-clamp on a steady state), so a
 * combatant sitting at 50/400 with the item still held is not yanked back to full or re-clamped each
 * tick. {@link Stat#putModifier} and {@link HealthState#setMaxModifier} report whether they actually
 * changed anything, and that is what this returns.
 *
 * Pure: no Bukkit, no scheduling. The Paper reconcile loop only supplies the equipped-items scan;
 * the leak-proofness lives here, where a unit test exercises every removal path with no server.
 */
public final class ModifierReconciler {

    private ModifierReconciler() {}

    /**
     * Make {@code state}'s max modifiers exactly {@code desired} (source -> amount).
     *
     * @return true if anything changed (a source added, removed, or its amount altered) -- so the
     *         caller emits a change event once, only on a real transition.
     */
    public static boolean reconcile(HealthState state, Map<String, Double> desired) {
        boolean changed = false;

        // Remove every applied source no longer desired. This is the whole leak story: an item that
        // left the hand -- by ANY path -- is simply absent from `desired`, so its source is dropped.
        for (String source : state.maxModifierSources()) {
            if (!desired.containsKey(source)) {
                changed |= state.clearMaxModifier(source);
            }
        }

        // Add sources newly present, and update any whose amount changed. A source already applied
        // at the same amount reports no change and fires nothing -- the steady-state silence.
        for (Map.Entry<String, Double> entry : desired.entrySet()) {
            changed |= state.setMaxModifier(entry.getKey(), entry.getValue());
        }

        return changed;
    }

    /** The applied sources that {@code desired} would drop -- exposed for tests that assert the diff. */
    public static Set<String> wouldRemove(HealthState state, Map<String, Double> desired) {
        Set<String> applied = new HashSet<>(state.maxModifierSources());
        applied.removeIf(desired::containsKey);
        return applied;
    }
}
