package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * THE ONE DERIVE (PLAN-build-system.md sections 2.4 and 2.4.1): an ability as a player's active aspects change
 * it. The cast AND the Build screen's lore both read this, so the tooltip and the cast cannot disagree.
 *
 * <p><b>MODIFY FIRST, APPEND SECOND.</b> A {@code modify} reaches the target's OWN effects and never an
 * effect an aspect appends: appended effects are authored at their final numbers, and otherwise aspect A's
 * {@code damage.amount -25%} would scale aspect B's appended burst.
 *
 * <p><b>PER PLAYER, NEVER GLOBAL.</b> The registry's definition is never replaced; a derived definition is a
 * new record with the SAME id (so the cooldown key is the ability's), handed to the one cast that asked.
 *
 * <p>Pure. The instance memoises: the same (base, aspects) returns the same record.
 */
public final class AspectApplication {

    private final Map<List<Object>, AbilityDefinition> memo = new ConcurrentHashMap<>();

    /** {@link #deriveUncached}, memoised on (base, aspects in slot order). */
    public AbilityDefinition derive(AbilityDefinition base, List<AspectDefinition> active) {
        return derive(base, active, List.of());
    }

    /** {@link #deriveUncached}, memoised on (base, aspects in slot order, behaviour fragments in slot order). */
    public AbilityDefinition derive(AbilityDefinition base, List<AspectDefinition> active,
                                    List<FragmentDefinition> fragments) {
        return memo.computeIfAbsent(List.of(base, List.copyOf(active), List.copyOf(fragments)),
                key -> deriveUncached(base, active, fragments));
    }

    /**
     * The aspects, in slot order, that are ACTIVE on {@code abilityId}: they target it, and it is equipped.
     * An aspect whose target is not equipped is INACTIVE (amendment 2): it stays slotted and contributes
     * nothing -- to neither the appends nor the sums.
     */
    public static List<AspectDefinition> activeFor(String abilityId, List<AspectDefinition> slotted, Set<String> equipped) {
        return active(abilityId, slotted, AspectDefinition::target, equipped);
    }

    /**
     * The BEHAVIOUR fragments, in slot order, that are active on {@code abilityId} (section 7.3): the SAME
     * predicate as {@link #activeFor}, not a copy of it. A stat fragment has no target and is never here.
     */
    public static List<FragmentDefinition> activeFragmentsFor(String abilityId, List<FragmentDefinition> slotted,
                                                              Set<String> equipped) {
        return active(abilityId, slotted, FragmentDefinition::target, equipped);
    }

    /** The one inactive rule (amendment 2): targets {@code abilityId}, and {@code abilityId} is equipped. */
    private static <T> List<T> active(String abilityId, List<T> slotted, java.util.function.Function<T, String> target,
                                      Set<String> equipped) {
        if (!equipped.contains(abilityId)) return List.of();
        return slotted.stream().filter(x -> x != null && abilityId.equals(target.apply(x))).toList();
    }

    /**
     * The derived ability. Aspects for another target are ignored. With none, the base itself is returned.
     * Assumes the combination passed {@link #refusals} at load: an integer field that did not resolve to a
     * whole number throws rather than being rounded.
     */
    public static AbilityDefinition deriveUncached(AbilityDefinition base, List<AspectDefinition> aspects) {
        return deriveUncached(base, aspects, List.of());
    }

    /**
     * The derived ability with behaviour fragments too (section 7.3). ORDER: the aspects' modify, then the
     * aspects' appends in aspect-slot order, then the fragments' appends in fragment-slot order. No modify
     * reaches a fragment's appended effect, for the reason no modify reaches an aspect's.
     */
    public static AbilityDefinition deriveUncached(AbilityDefinition base, List<AspectDefinition> aspects,
                                                   List<FragmentDefinition> fragments) {
        List<AspectDefinition> mine = aspects.stream().filter(a -> a.target().equals(base.id())).toList();
        List<FragmentDefinition> myFragments = fragments.stream()
                .filter(f -> f != null && base.id().equals(f.target())).toList();
        if (mine.isEmpty() && myFragments.isEmpty()) return base;
        Rewriter rewriter = new Rewriter(changesByField(mine), f -> true, base);

        // 1. MODIFY the target's own numbers.
        int cooldown = base.cooldownTicks();
        List<NumberChange> cooldownChanges = rewriter.changes.get(AspectField.COOLDOWN_TICKS);
        if (cooldownChanges != null) {
            cooldown = NumberResolution.intValue(NumberResolution.resolve(base.cooldownTicks(), cooldownChanges));
        }
        ResourceCost cost = base.cost();
        List<NumberChange> costChanges = rewriter.changes.get(AspectField.COST);
        if (costChanges != null && cost != null) {
            cost = new ResourceCost(cost.resourceId(), NumberResolution.resolve(cost.amount(), costChanges).doubleValue());
        }
        List<EffectSpec> onHit = new ArrayList<>();
        for (EffectSpec effect : base.onHit()) onHit.add(rewriter.rewrite(effect));

        // 2. APPEND, in slot order -- after the modify, so no modify reaches an appended effect.
        List<EffectSpec.Visual> onCast = new ArrayList<>(base.onCast());
        for (AspectDefinition aspect : mine) {
            onHit.addAll(aspect.addOnHit());
            onCast.addAll(aspect.addOnCast());
        }
        // 3. The behaviour fragments' appends, LAST, in fragment-slot order (section 7.3).
        for (FragmentDefinition fragment : myFragments) onHit.addAll(fragment.addOnHit());
        return new AbilityDefinition(base.id(), base.displayName(), base.element(), cooldown, cost, base.cast(),
                onHit, base.description(), onCast);
    }

    /**
     * Why these aspects, together on {@code base}, are illegal: empty when they are legal. The loader runs it
     * for every aspect alone and for every PAIR in a pool on the same target (section 2.4.1).
     *
     * <ul>
     *   <li>a {@code modify} field that matches ZERO effects in the target (the eligibility half, mechanical);</li>
     *   <li>any resolved value {@link NumberResolution#refusal} refuses, per effect instance;</li>
     *   <li>{@code status.duration_ticks} on a status whose kind is not traced continuous.</li>
     * </ul>
     *
     * @param statusDurationTraced is this status id's kind traced continuous in its duration
     */
    public static List<String> refusals(AbilityDefinition base, List<AspectDefinition> aspects,
                                        Predicate<String> statusDurationTraced) {
        List<AspectDefinition> mine = aspects.stream().filter(a -> a.target().equals(base.id())).toList();
        String who = mine.stream().map(AspectDefinition::id).toList() + " on " + base.id() + ": ";
        Rewriter rewriter = new Rewriter(changesByField(mine), statusDurationTraced, base);
        for (EffectSpec effect : base.onHit()) rewriter.rewrite(effect);   // walks every effect; collects refusals

        List<String> refusals = new ArrayList<>();
        for (Map.Entry<AspectField, List<NumberChange>> entry : rewriter.changes.entrySet()) {
            AspectField field = entry.getKey();
            switch (field) {
                case COOLDOWN_TICKS -> NumberResolution.refusal(field,
                                NumberResolution.resolve(base.cooldownTicks(), entry.getValue()),
                                NumberResolution.Context.cooldown(CastSpec.minimumCooldownTicks(base.cast())))
                        .ifPresent(r -> refusals.add(who + r));
                case COST -> {
                    if (base.cost() == null) {
                        refusals.add(who + "cost matches nothing -- the target has no cost");
                    } else {
                        NumberResolution.refusal(field, NumberResolution.resolve(base.cost().amount(), entry.getValue()),
                                NumberResolution.Context.NONE).ifPresent(r -> refusals.add(who + r));
                    }
                }
                default -> {
                    if (rewriter.matches.getOrDefault(field, 0) == 0) {
                        refusals.add(who + field.token() + " matches nothing in the target's on_hit");
                    }
                }
            }
        }
        for (String r : rewriter.refusals) refusals.add(who + r);
        return refusals;
    }

    private static Map<AspectField, List<NumberChange>> changesByField(List<AspectDefinition> aspects) {
        Map<AspectField, List<NumberChange>> byField = new EnumMap<>(AspectField.class);
        for (AspectDefinition aspect : aspects) {
            for (NumberChange change : aspect.modify()) {
                byField.computeIfAbsent(change.field(), f -> new ArrayList<>()).add(change);
            }
        }
        return byField;
    }

    /**
     * Walks an effect tree, rewriting every whitelisted number it meets, and counting and checking as it
     * goes. One walker for derive and for the checks, so the two cannot address different effects.
     */
    private static final class Rewriter {
        final Map<AspectField, List<NumberChange>> changes;
        final Predicate<String> statusDurationTraced;
        final Map<AspectField, Integer> matches = new EnumMap<>(AspectField.class);
        final List<String> refusals = new ArrayList<>();

        Rewriter(Map<AspectField, List<NumberChange>> changes, Predicate<String> statusDurationTraced,
                 AbilityDefinition base) {
            this.changes = changes;
            this.statusDurationTraced = statusDurationTraced;
        }

        /** The resolved value of {@code field} on {@code value}, or the value itself if nothing changes it. */
        private BigDecimal resolve(AspectField field, double value, NumberResolution.Context context) {
            List<NumberChange> list = changes.get(field);
            if (list == null) return null;
            matches.merge(field, 1, Integer::sum);
            BigDecimal resolved = NumberResolution.resolve(value, list);
            NumberResolution.refusal(field, resolved, context).ifPresent(refusals::add);
            return resolved;
        }

        private double dbl(AspectField field, double value) {
            BigDecimal r = resolve(field, value, NumberResolution.Context.NONE);
            return r == null ? value : r.doubleValue();
        }

        /** An int field: rewritten only if legal; an illegal one keeps its value here and is refused. */
        private int ticks(AspectField field, int value, NumberResolution.Context context) {
            BigDecimal r = resolve(field, value, context);
            if (r == null || NumberResolution.refusal(field, r, context).isPresent()) return value;
            return NumberResolution.intValue(r);
        }

        EffectSpec rewrite(EffectSpec effect) {
            return switch (effect) {
                case EffectSpec.Targeted t -> rewriteTargeted(t);
                case EffectSpec.Burst b -> rewriteBurst(b);
                case EffectSpec.Area a -> new EffectSpec.Area(dbl(AspectField.AREA_RADIUS, a.radius()),
                        ticks(AspectField.AREA_DURATION_TICKS, a.durationTicks(),
                                NumberResolution.Context.interval(a.tickInterval())),
                        a.tickInterval(), rewriteAll(a.effects()));
                case EffectSpec.ThrowEmbers e -> new EffectSpec.ThrowEmbers(e.anglesDegrees(), e.speed(),
                        e.launchLift(), e.itemId(), e.fuseTicks(), e.burst() == null ? null : rewriteBurst(e.burst()),
                        e.visual(), e.trail());
                case EffectSpec.Visual v -> v;
            };
        }

        private EffectSpec.Burst rewriteBurst(EffectSpec.Burst b) {
            return new EffectSpec.Burst(dbl(AspectField.BURST_RADIUS, b.radius()), rewriteAll(b.effects()));
        }

        private List<EffectSpec.Targeted> rewriteAll(List<EffectSpec.Targeted> effects) {
            List<EffectSpec.Targeted> out = new ArrayList<>();
            for (EffectSpec.Targeted t : effects) out.add(rewriteTargeted(t));
            return out;
        }

        private EffectSpec.Targeted rewriteTargeted(EffectSpec.Targeted t) {
            return switch (t) {
                case EffectSpec.Damage d -> new EffectSpec.Damage(dbl(AspectField.DAMAGE_AMOUNT, d.amount()), d.element());
                case EffectSpec.Heal h -> new EffectSpec.Heal(dbl(AspectField.HEAL_AMOUNT, h.amount()));
                case EffectSpec.Knockback k -> new EffectSpec.Knockback(dbl(AspectField.KNOCKBACK_STRENGTH, k.strength()));
                case EffectSpec.Status s -> {
                    if (changes.containsKey(AspectField.STATUS_DURATION_TICKS) && !statusDurationTraced.test(s.statusId())) {
                        matches.merge(AspectField.STATUS_DURATION_TICKS, 1, Integer::sum);
                        refusals.add("status.duration_ticks on '" + s.statusId() + "': that status kind is not traced"
                                + " continuous in its duration, so a change there is refused (section 2.4.1)");
                        yield s;
                    }
                    yield new EffectSpec.Status(s.statusId(),
                            ticks(AspectField.STATUS_DURATION_TICKS, s.durationTicks(), NumberResolution.Context.NONE),
                            s.amplifier());
                }
                case EffectSpec.WeaponDamage w -> w;
            };
        }
    }
}
