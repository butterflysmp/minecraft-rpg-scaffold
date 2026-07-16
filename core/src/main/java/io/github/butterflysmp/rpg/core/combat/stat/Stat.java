package io.github.butterflysmp.rpg.core.combat.stat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A named value on a combatant, resolved as {@code base + Σ(modifiers)}.
 *
 * This is the general mechanism the whole stat system stands on. Max health is the first user;
 * attack damage and attack speed reuse it unchanged in later phases, and every future source --
 * an enchantment, a passive, a build aspect -- is just another keyed modifier. Building it general
 * now means those slot in as new sources, not new mechanisms.
 *
 * A modifier is {@code (source, amount)}. The source is a stable string key -- an equipment slot,
 * later a weapon id or enchant id -- so a source can add on equip and REMOVE by the same key on
 * unequip. Re-adding the same source REPLACES: there is always exactly one modifier per source,
 * never N. This is the Soaked "one keyed modifier, base restored exactly" property, generalized
 * from one movement-speed status to any stat with any number of sources.
 *
 * Not thread-safe by design, and it does not need to be. Like a Soaked {@code Active} entry, a
 * combatant's Stat is only ever touched on the thread that owns that combatant; the outer store
 * ({@link CombatantStats}) is what's concurrent. Insertion-ordered so a value that sums many
 * modifiers is deterministic to read in a debugger.
 */
public final class Stat {

    private double base;
    private final Map<String, Double> modifiers = new LinkedHashMap<>();

    public Stat(double base) {
        this.base = base;
    }

    /** The resolved value: base plus every modifier currently applied. */
    public double value() {
        double sum = base;
        for (double amount : modifiers.values()) sum += amount;
        return sum;
    }

    public double base() {
        return base;
    }

    public void setBase(double base) {
        this.base = base;
    }

    /**
     * Add the modifier from {@code source}, or REPLACE it if that source already contributes one.
     * Never appends a second modifier for the same source -- that is the leak the Soaked seam
     * exists to prevent, here for any stat.
     *
     * @return true if this actually changed the resolved value (a new source, or a changed amount),
     *         false if {@code source} already contributed exactly {@code amount} -- so a caller can
     *         fire a change event only on a real transition, not every idempotent re-apply.
     */
    public boolean putModifier(String source, double amount) {
        Double previous = modifiers.put(source, amount);
        return previous == null || previous != amount;
    }

    /**
     * Remove {@code source}'s modifier if present.
     *
     * @return true if a modifier was actually removed (a real transition), false if the source
     *         contributed nothing to begin with.
     */
    public boolean removeModifier(String source) {
        return modifiers.remove(source) != null;
    }

    public boolean hasModifier(String source) {
        return modifiers.containsKey(source);
    }

    /** The amount {@code source} contributes, or 0 if it contributes nothing. */
    public double amountOf(String source) {
        return modifiers.getOrDefault(source, 0.0);
    }

    /** The set of sources currently contributing a modifier. A snapshot copy, safe to iterate. */
    public Set<String> sources() {
        return Set.copyOf(modifiers.keySet());
    }

    /** How many modifiers are applied. The "exactly one per source, never N" probe for tests. */
    public int modifierCount() {
        return modifiers.size();
    }
}
