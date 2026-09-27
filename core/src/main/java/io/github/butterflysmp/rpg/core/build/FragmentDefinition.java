package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A fragment: one of a cell's four fragment slots (PLAN-build-system.md sections 2.3 and 7.3). Content, from
 * {@code content/fragments/<id>.yml}. It is never an item: {@code icon} names a material for the Build screen's
 * icon and nothing else.
 *
 * <p><b>A fragment is EITHER a STAT fragment OR a BEHAVIOUR fragment, never both.</b>
 * <ul>
 *   <li>A stat fragment carries {@code modifiers}, and rides the stat pipeline ({@link FragmentContributions}).</li>
 *   <li>A behaviour fragment carries a {@code target} ability and effects to append to its {@code on_hit}
 *       ({@code addOnHit}), the aspects' append path (section 7.3). It is INACTIVE while its target is not
 *       equipped, by the aspects' own predicate ({@link AspectApplication#activeFragmentsFor}).</li>
 * </ul>
 * The mix is refused: the inactive rule applies to the behaviour, and a half-inactive fragment would be a second
 * rule nobody ruled.
 *
 * <p><b>POSITIVE-ONLY (ruling 21).</b> Every modifier must be a finite amount above zero. Only a negative
 * can carry a stat to its floor, so a fragment adds no negative source to the combined bound
 * ({@link StatSourceBound}), and the shipped accessories' drawbacks keep the bound they were ruled under. A
 * behaviour fragment has no modifiers at all, so it adds no source of either sign.
 *
 * <p><b>{@code class_damage} is refused</b> (section 2.3): a fragment is already class-scoped by its pool,
 * so it would be the same number arriving by a second door.
 *
 * <p>A record: an immutable value type, whose fields are fixed at construction.
 */
public record FragmentDefinition(String id, String displayName, String icon, List<String> description,
                                 Map<AccessoryStat, Double> modifiers, String target, List<EffectSpec> addOnHit) {

    /** A stat fragment -- every fragment before section 7.3. */
    public FragmentDefinition(String id, String displayName, String icon, List<String> description,
                              Map<AccessoryStat, Double> modifiers) {
        this(id, displayName, icon, description, modifiers, null, List.of());
    }

    public FragmentDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("fragment id required");
        if (displayName == null || displayName.isBlank()) displayName = id;
        description = description == null ? List.of() : List.copyOf(description);
        addOnHit = addOnHit == null ? List.of() : List.copyOf(addOnHit);
        boolean hasStats = modifiers != null && !modifiers.isEmpty();
        boolean hasTarget = target != null;
        if (hasTarget && target.isBlank()) throw new IllegalArgumentException("fragment " + id + ": a blank target");
        if (hasTarget != !addOnHit.isEmpty()) {
            throw new IllegalArgumentException("fragment " + id + ": a behaviour fragment needs BOTH a target and"
                    + " add_on_hit effects");
        }
        if (hasStats && hasTarget) {
            throw new IllegalArgumentException("fragment " + id + ": a fragment is stats OR behaviour, not both"
                    + " (section 7.3)");
        }
        if (!hasStats && !hasTarget) {
            throw new IllegalArgumentException("fragment " + id + " modifies nothing and adds nothing");
        }
        Map<AccessoryStat, Double> copy = new EnumMap<>(AccessoryStat.class);
        if (hasStats) {
            for (Map.Entry<AccessoryStat, Double> e : modifiers.entrySet()) {
                AccessoryStat stat = e.getKey();
                double amount = e.getValue();
                if (stat == AccessoryStat.CLASS_DAMAGE) {
                    throw new IllegalArgumentException("fragment " + id + ": class_damage is refused on a fragment"
                            + " (a fragment is already class-scoped by its pool)");
                }
                if (!(amount > 0) || Double.isInfinite(amount)) {
                    throw new IllegalArgumentException("fragment " + id + ": " + stat.token() + " is " + amount
                            + " -- fragments are positive-only (ruling 21): a finite amount above zero");
                }
                copy.put(stat, amount);
            }
        }
        modifiers = Collections.unmodifiableMap(copy);
    }

    /** A behaviour fragment: it appends to its target's cast, and carries no stats. */
    public boolean behavioural() {
        return target != null;
    }
}
