package io.github.butterflysmp.rpg.paper.adapter;

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
     * What a landed hit accrues: which status, how many stacks, capped at what, for how long.
     *
     * @param stacks never zero -- {@link #forHit} returns empty rather than an application worth
     *               nothing, because {@code ScorchStatus.apply} early-returns on a non-positive
     *               count and a caller cannot tell that apart from a refused application
     */
    public record Accrued(String statusId, int stacks, double cap, int durationTicks) {}

    /**
     * The accrual this hit earns, or empty for the many reasons a hit earns none.
     *
     * <p><b>The target must still be STANDING, and the predicate is {@code newCurrent > 0} rather
     * than "this hit caused the transition".</b> {@code CombatantStats.damage} publishes its seam
     * event BEFORE returning, and {@code MobDeathSystem.onChange} calls {@code setHealth(0)}
     * synchronously -- which fires removal, which calls {@code scorch().forget(id)}. Accruing on a
     * lethal hit would therefore register a {@code RepeatingTask} AFTER the cleanup meant to cancel
     * it: an ordering inversion, not a race, leaving a map entry and a 160-tick task per lethal fire
     * kill with nothing left to cancel them. It also keeps Ignite's death-gate unambiguous, since a
     * killing blow's own stacks never count toward the threshold it is measured against.
     *
     * @param preMitigationAmount the cap basis. NOT {@code outcome.dealt()} -- armour must delay the
     *                            burn through the STACK COUNT and never lower its ceiling, or it
     *                            re-enters the DoT through the back door after being ruled out of it
     */
    public static Optional<Accrued> forHit(ElementRegistry elements, StatusRegistry statuses,
                                           String element, DamageOutcome outcome,
                                           double preMitigationAmount) {
        if (outcome.newCurrent() <= 0) return Optional.empty();

        ElementDefinition def = elements.find(element).orElse(null);
        if (def == null || def.appliesStatus() == null) return Optional.empty();

        StatusDefinition status = statuses.find(def.appliesStatus()).orElse(null);
        if (status == null) return Optional.empty();

        // Exhaustive over the sealed type, so a new status kind is a compile error here rather than
        // silently landing in the "accrues nothing" arm. ContentValidator.validateElements already
        // NAMES a non-accruing status at boot, which is why this returns empty without warning.
        return switch (status) {
            case StatusDefinition.Scorch ignored -> scorch(def.appliesStatus(), outcome,
                    preMitigationAmount);
            case StatusDefinition.Fire ignored -> Optional.empty();
            case StatusDefinition.Potion ignored -> Optional.empty();
            case StatusDefinition.Immobilize ignored -> Optional.empty();
            case StatusDefinition.Soaked ignored -> Optional.empty();
        };
    }

    private static Optional<Accrued> scorch(String statusId, DamageOutcome outcome,
                                            double preMitigationAmount) {
        int stacks = Scorch.stacksFor(outcome.dealt());
        if (stacks <= 0) return Optional.empty();

        // The cap floor mirrors the explicit path at BukkitCombatant.applyStatus: a non-positive
        // basis is UNDECLARED, never "no cap", because for a percent-of-max effect an absent cap is
        // not a fallback but the absence of one.
        double cap = preMitigationAmount > 0 ? preMitigationAmount : Scorch.UNDECLARED_CAP;
        return Optional.of(new Accrued(statusId, stacks, cap, Scorch.DEFAULT_DURATION_TICKS));
    }
}
