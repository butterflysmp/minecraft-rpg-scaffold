package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.AccrualRule;
import io.github.butterflysmp.rpg.core.combat.Scorch;
import io.github.butterflysmp.rpg.core.combat.stat.DamageOutcome;
import io.github.butterflysmp.rpg.paper.content.ElementDefinition;
import io.github.butterflysmp.rpg.paper.content.ElementRegistry;
import io.github.butterflysmp.rpg.paper.content.StatusDefinition;
import io.github.butterflysmp.rpg.paper.content.StatusRegistry;

import java.util.Optional;

/**
 * The element -> status bridge: given a hit that just landed, what does its element accrue?
 *
 * <p><b>Pure, and deliberately separated from the application.</b> Everything here is registry
 * lookups and arithmetic, so the DECISION is unit-testable with no server and no Bukkit entity --
 * which is what lets the two rows that matter exist at all (see below). {@code BukkitCombatant}
 * performs the resulting application; it does not decide it.
 *
 * <h2>ONE STATUS, NAMED IN CONTENT, AND NOT A GENERAL EFFECT SYSTEM</h2>
 *
 * An element declares {@code applies_status} and nothing else -- no rate, no duration, no amplifier.
 * The rate is {@link Scorch#DAMAGE_PER_STACK} and the window is
 * {@link Scorch#DEFAULT_DURATION_TICKS}, whose javadoc has scoped it to "the weapon-damage entry
 * point only" since the day it was written; this is that entry point finally reaching it. An element
 * that could declare arbitrary effects is a far bigger feature than either thing this slice was for.
 *
 * <h2>THERE IS NO {@code element == null} GUARD, AND THAT IS DELIBERATE</h2>
 *
 * {@code ElementRegistry.find} is {@code Optional.ofNullable(byId.get(id))} over a
 * {@code LinkedHashMap}, which permits a null key and returns null rather than throwing. A null
 * element therefore falls out through the ordinary registry MISS. An explicit null check would be
 * <b>indistinguishable from the miss path</b> -- no test could separate them -- which is the
 * dead-guard shape this slice already found once, in a {@code catch} around MiniMessage that could
 * never fire.
 *
 * <p>The loop guard's real witness is BEHAVIOURAL: <b>a burn tick accrues no stacks.</b> That row
 * lives in the tests and holds whatever mechanism happens to deliver it -- including the day someone
 * gives {@code find} a null check of its own. The structural half is upstream and unchanged: scorch's
 * burn reaches the port through {@code EntityScorchSink}, which calls the four-argument
 * {@code applyDamage} and so has no element to pass.
 *
 * <h2>THE CAP IS THIS HIT'S PRE-MITIGATION MAGNITUDE, AND IT IS NOT THE OTHER PATH'S NUMBER</h2>
 *
 * See {@code ScorchStatus.apply}'s {@code @param cap}. The two application paths pass
 * <b>differently-shaped numbers on purpose</b>, and a reader who assumes parity will be wrong:
 *
 * <ul>
 *   <li>the EXPLICIT path ({@code type: status} in content) passes {@code caster.payloadDamage()} --
 *       the cast-frozen headline figure, one per ability, the same number the tooltip prints, with
 *       no enchant percent, no class bonus, no charge scale and no crit multiplier in it;</li>
 *   <li>ACCRUAL passes the resolved magnitude of the damage effect that carried it, which contains
 *       all four.</li>
 * </ul>
 *
 * <b>Accrual has no authored figure in scope</b>, and threading one down would be a sixth argument on
 * a port that just took a fifth -- unavailable on the melee and environmental paths at all. This is
 * the same jointly-unsatisfiable shape as post-mitigation stacks versus an upstream site: the one
 * place {@code amount} and {@code dealt} are both in scope is after {@code CombatantStats.damage}
 * returns, so the SITE is forced and the CONTRACT is what moved.
 *
 * <p><b>CONSEQUENCE, RULED ON DELIBERATELY RATHER THAN INHERITED FROM WHICH VARIABLE WAS NEAREST: a
 * crit raises the burn's CEILING as well as the hit.</b> Chosen so crit is a uniform multiplier on
 * everything a swing produces; the alternative makes crit a weak stat specifically on elemental
 * weapons, since most of a fire weapon's output lives in the burn.
 *
 * <p><b>And that contribution is zero on small targets and full on large ones.</b> The tick is
 * {@code min(5% of victim max, cap)}, so on a 100-max mob {@code min(5, 7) == min(5, 14)} and a crit
 * buys no extra burn whatsoever; on a 360-max mob it is 7/tick against 14/tick. <b>Fire crit is a
 * boss-fight stat.</b> That follows from the percent-max cap and is not new, but it is newly VISIBLE,
 * and it will be filed as a bug if the first time anyone states it is after someone notices it.
 */
public final class ElementAccrual {

    private ElementAccrual() {}

    /**
     * What a landed hit accrues onto a burn: how many stacks, capped at what, for how long.
     *
     * <p><b>NAMED FOR SCORCH, AND THAT IS THE POINT.</b> It first carried a {@code statusId} field
     * that nothing in production read -- {@code BukkitCombatant} calls {@code ctx.scorch().apply}
     * unconditionally and takes only the three numbers, so the id's only reader was a test. A field
     * named for a dispatch it does not perform makes the call site look safer than it is.
     *
     * <p>Dropping it alone would not have fixed the hazard underneath, which is worth stating because
     * the sealed switch does NOT protect against it: adding a status kind forces someone to write an
     * ARM, not to write an arm that returns empty. The day some future arm returns a value here, the
     * call site would apply SCORCH for it, silently, with no compile error anywhere.
     *
     * <p>So the TYPE carries the constraint instead of a field advertising a choice that is not made:
     * a value of this type IS a scorch application. A second accruable status cannot reuse it without
     * saying so at the point of writing, and "only scorch accrues" lives in exactly one place -- the
     * switch in {@link #forHit}.
     *
     * @param stacks never zero -- {@link #forHit} returns empty rather than an application worth
     *               nothing, because {@code ScorchStatus.apply} early-returns on a non-positive
     *               count and a caller cannot tell that apart from a refused application
     */
    public record ScorchAccrual(int stacks, double cap, int durationTicks) {}

    /**
     * The accrual this hit earns, or empty for the many reasons a hit earns none.
     *
     * <p><b>The target must still be STANDING, and the predicate is {@code newCurrent > 0} rather
     * than "this hit caused the transition".</b> Two independent reasons, and they do NOT cover the
     * same targets -- the first is mob-only, and reading them as one grants it scope it never had:
     *
     * <ul>
     *   <li><b>For a MOB, an ordering inversion.</b> {@code CombatantStats.damage} publishes its seam
     *       event BEFORE returning, and {@code MobDeathSystem.onChange} calls {@code setHealth(0)}
     *       synchronously -- which fires removal, which calls {@code scorch().forget(id)}. Accruing
     *       on a lethal hit would register a {@code RepeatingTask} AFTER the cleanup meant to cancel
     *       it, leaving a map entry and a 160-tick task per lethal fire kill with nothing left to
     *       cancel them.</li>
     *   <li><b>For a PLAYER, none of that chain runs at all.</b> {@code MobDeathSystem.shouldKill} is
     *       {@code reachedZero() && !targetIsPlayer()}, so there is no {@code setHealth(0)}, no
     *       removal and no {@code forget} to invert. The rule still holds for players, but on the
     *       OTHER reason: Ignite is death-gated, so a killing blow must never scorch the target it
     *       just killed. That reason is path-independent.
     *
     *       <p><b>And since the 2026-09-09 ruling it is load-bearing rather than tidy.</b> "Any mob
     *       that dies while scorched ignites" means a killing blow that accrued would grant a first
     *       stack to a mob that had none and then detonate it -- so this skip is the only thing
     *       between the ruling and EVERY fire-weapon kill exploding. Nothing about the predicate
     *       changed; what rests on it did.</li>
     * </ul>
     *
     * <p><b>CONSEQUENCE, NAMED HERE RATHER THAN DISCOVERED IN A GATE: a player at zero custom health
     * is permanently unscorchable.</b> They are not killed and not removed, so they sit tracked at
     * the floor and every subsequent hit reports {@code newCurrent == 0} -- they never accrue again
     * until healed. That is downstream of the deferred respawn lifecycle rather than a defect of this
     * predicate, which is exactly why it belongs where the predicate lives.
     *
     * @param declaredMagnitude the cap basis: this hit WITHOUT its crit, recovered by the caller. NOT
     *                          {@code outcome.dealt()} -- armour must delay the
     *                            burn through the STACK COUNT and never lower its ceiling, or it
     *                            re-enters the DoT through the back door after being ruled out of it
     */
    public static Optional<ScorchAccrual> forHit(ElementRegistry elements, StatusRegistry statuses,
                                                 String element, AccrualRule accrual,
                                                 DamageOutcome outcome,
                                                 double declaredMagnitude) {
        // INERT: the hit HAS an element -- it draws that element's glyph -- and must not accrue.
        // Scorch's own burn tick is the only caller today, and this is the loop guard. It is a
        // DIFFERENT reason from the null-element early return below: that one means the hit has no
        // element at all. Same outcome, different facts; collapsing them loses the ability to tell
        // a burn tick apart from fall damage.
        if (!accrual.accrues()) return Optional.empty();

        if (outcome.newCurrent() <= 0) return Optional.empty();

        ElementDefinition def = elements.find(element).orElse(null);
        if (def == null || def.appliesStatus() == null) return Optional.empty();

        StatusDefinition status = statuses.find(def.appliesStatus()).orElse(null);
        if (status == null) return Optional.empty();

        // Exhaustive over the sealed type, so a new status kind is a compile error here rather than
        // silently landing in the "accrues nothing" arm. ContentValidator.validateElements already
        // NAMES a non-accruing status at boot, which is why this returns empty without warning.
        return switch (status) {
            case StatusDefinition.Scorch ignored -> scorch(outcome, declaredMagnitude);
            case StatusDefinition.Fire ignored -> Optional.empty();
            case StatusDefinition.Potion ignored -> Optional.empty();
            case StatusDefinition.Immobilize ignored -> Optional.empty();
            case StatusDefinition.Soaked ignored -> Optional.empty();
        };
    }

    private static Optional<ScorchAccrual> scorch(DamageOutcome outcome, double declaredMagnitude) {
        int stacks = Scorch.stacksFor(outcome.dealt());
        if (stacks <= 0) return Optional.empty();

        // THE CAP BASIS IS POSITIVE HERE, BY INVARIANT, SO THERE IS NO FALLBACK BRANCH.
        //
        // stacks > 0 means Scorch.stacksFor saw a positive figure, so dealt > 0. And dealt is either
        // `amount` (DefenseRule.BYPASSED) or Defense.applyDefense(amount, d), which returns `damage`
        // unchanged when defense <= 0 and otherwise scales it by SCALE/(SCALE+defense) -- a factor in
        // (0, 1). Both preserve sign, so:  stacks > 0  =>  dealt > 0  =>  declaredMagnitude > 0.
        //
        // A `declaredMagnitude > 0 ? ... : Scorch.UNDECLARED_CAP` ternary stood here and WAS DEAD.
        // It was mirrored from BukkitCombatant.applyStatus, whose fallback is correct and necessary
        // because THAT path is reachable without a damage effect at all -- a payload declaring only
        // `type: status` has payloadDamage == 0. Accrual is TRIGGERED BY a damage effect and cannot
        // reach that state. The copy also took the value and dropped the emphasis: the original says
        // "fall back to a conservative constant, LOUDLY" and calls warnOnce; the mirror was silent.
        //
        // Its test row had to invent the state to reach it -- 25 damage landed from a hit that asked
        // for 0 -- which is a row whose conditions came from what the signature permits rather than
        // from what the system can produce. And a mutation inside an unreachable branch can only
        // redden the row that keeps the branch alive, so the two justified each other while neither
        // touched production. Both are gone; the invariant is stated once, here.
        // HALF the hit, per Scorch.CAP_FRACTION -- which lives in core with every other scorch rate
        // rather than as a bare 0.5 here, so the next person tuning scorch finds it where they look.
        // The fraction applies to a WEAPON-DERIVED figure only: Scorch.UNDECLARED_CAP is deliberately
        // not halved, and never meets this path anyway.
        return Optional.of(new ScorchAccrual(
                stacks, declaredMagnitude * Scorch.CAP_FRACTION, Scorch.DEFAULT_DURATION_TICKS));
    }
}
