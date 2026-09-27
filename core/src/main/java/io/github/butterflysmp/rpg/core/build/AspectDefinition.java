package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An aspect: it changes ONE ability, its {@code target}, by ADDING behaviour (ruling 3) and, under the
 * whitelist, by changing numbers the target already has (ruling 9). PLAN-build-system.md sections 2.4 and
 * 2.4.1. Content, from {@code content/aspects/<id>.yml}.
 *
 * <ul>
 *   <li>{@code addOnHit}: effects appended to the target's {@code on_hit} -- any effect an ability may carry;</li>
 *   <li>{@code addOnCast}: visuals appended to the target's {@code on_cast};</li>
 *   <li>{@code modify}: whitelisted number changes to the target's OWN effects ({@link AspectField}).</li>
 *   <li>{@code recast}: a follow-up ability the target's input casts shortly after it (section 7.4).</li>
 * </ul>
 *
 * <p>A record: an immutable value whose fields are fixed at construction.
 */
public record AspectDefinition(String id, String displayName, List<String> description, String target,
                               List<EffectSpec> addOnHit, List<EffectSpec.Visual> addOnCast,
                               List<NumberChange> modify, Recast recast) {

    /**
     * RECAST (section 7.4, rulings 28-30): pressing the target's input again, from {@code fromTicks} to
     * {@code windowTicks} after casting it, casts {@code ability} instead -- once, for free, with no cooldown of
     * its own. The rule is {@link RecastRule}; the state, {@link RecastTracker}.
     */
    public record Recast(String ability, int fromTicks, int windowTicks) {
        public Recast {
            if (ability == null || ability.isBlank()) throw new IllegalArgumentException("recast needs an ability");
            if (!(0 < fromTicks && fromTicks < windowTicks)) {
                throw new IllegalArgumentException("recast needs 0 < from_ticks < window_ticks, got " + fromTicks
                        + " and " + windowTicks);
            }
        }
    }

    /** An aspect with no recast -- every aspect before section 7.4. */
    public AspectDefinition(String id, String displayName, List<String> description, String target,
                            List<EffectSpec> addOnHit, List<EffectSpec.Visual> addOnCast, List<NumberChange> modify) {
        this(id, displayName, description, target, addOnHit, addOnCast, modify, null);
    }

    public AspectDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("aspect id required");
        if (target == null || target.isBlank()) throw new IllegalArgumentException("aspect " + id + " needs a target");
        if (displayName == null || displayName.isBlank()) displayName = id;
        description = description == null ? List.of() : List.copyOf(description);
        addOnHit = addOnHit == null ? List.of() : List.copyOf(addOnHit);
        addOnCast = addOnCast == null ? List.of() : List.copyOf(addOnCast);
        modify = modify == null ? List.of() : List.copyOf(modify);
        if (addOnHit.isEmpty() && addOnCast.isEmpty() && modify.isEmpty() && recast == null) {
            throw new IllegalArgumentException("aspect " + id + " adds and changes nothing");
        }
        Set<AspectField> seen = new HashSet<>();
        for (NumberChange change : modify) {
            if (!seen.add(change.field())) {
                throw new IllegalArgumentException("aspect " + id + " modifies " + change.field().token()
                        + " twice; write one entry with both flat and percent");
            }
        }
    }
}
