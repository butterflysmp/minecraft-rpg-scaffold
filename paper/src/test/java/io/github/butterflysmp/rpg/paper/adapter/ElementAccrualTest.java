package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.AccrualRule;
import io.github.butterflysmp.rpg.core.combat.Scorch;
import io.github.butterflysmp.rpg.core.combat.stat.DamageOutcome;
import io.github.butterflysmp.rpg.paper.content.ElementDefinition;
import io.github.butterflysmp.rpg.paper.content.ElementRegistry;
import io.github.butterflysmp.rpg.paper.content.StatusDefinition;
import io.github.butterflysmp.rpg.paper.content.StatusRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a landed hit accrues. Pure -- no server, no Bukkit entity -- which is the whole reason the
 * decision was split out of {@code BukkitCombatant}: the rows that matter here are about a hit that
 * accrues NOTHING, and "nothing happened" is only a real assertion when the alternative is in the
 * same fixture.
 *
 * <p>The numbers are deliberately all different from one another. {@code dealt} 25 against
 * {@code amount} 30 against {@code newCurrent} 75 means a transposition at any of the three sites
 * reddens, where equal values would let a swap pass as a coincidence -- the trap that made the
 * {@code DamageOutcome} rows use an armoured victim.
 */
class ElementAccrualTest {

    private static final double EPS = 1e-9;

    private static ElementRegistry elements(String id, String appliesStatus) {
        var registry = new ElementRegistry();
        registry.register(new ElementDefinition(id, id, Component.text("*"), appliesStatus));
        return registry;
    }

    private static StatusRegistry statuses() {
        var registry = new StatusRegistry();
        registry.register(new StatusDefinition.Scorch("scorch"));
        registry.register(new StatusDefinition.Immobilize("rooted", false));
        registry.register(new StatusDefinition.Soaked("soaked"));
        registry.register(new StatusDefinition.Potion("surge", NamespacedKey.minecraft("speed")));
        return registry;
    }

    private static Optional<ElementAccrual.ScorchAccrual> accrue(String element, DamageOutcome outcome,
                                                           double amount) {
        return ElementAccrual.forHit(elements("fire", "scorch"), statuses(), element,
                AccrualRule.ACCRUES, outcome, amount);
    }

    // --- THE PAIR. Neither row discriminates alone. ----------------------------------------------

    @Test
    void aNONLETHALFireHitAccruesStacksFromWhatLANDEDAndCapsAtWhatWasASKED() {
        // THE POSITIVE TWIN of the lethal row below, and it exists because "apply was never called"
        // passes just as well when the call is DELETED OUTRIGHT. Same fixture shape, differing only
        // in the victim's remaining health.
        var accrued = accrue("fire", new DamageOutcome(25.0, 75.0), 30.0).orElseThrow();

        assertEquals(12, accrued.stacks(), "stacksFor(25) -- from what LANDED, so armour delays it");
        assertEquals(15.0, accrued.cap(), EPS,
                "HALF of what was ASKED, pre-mitigation. Three distinct numbers -- 25 landed, 30 asked, "
                        + "15 capped -- so no transposition among them can pass as a coincidence. "
                        + "Armour must never lower the ceiling, "
                        + "or it re-enters the DoT through the back door after being ruled out of it");
        assertEquals(Scorch.DEFAULT_DURATION_TICKS, accrued.durationTicks(),
                "the constant whose javadoc has said 'weapon-damage entry point only' since it was "
                        + "written; this is that entry point");
        // Mutation: stacksFor(amount) instead of dealt -> 15 != 12 -> reddens.
        // Mutation: cap from dealt instead of amount -> 25 != 30 -> reddens.
        // Mutation: hardcode 1 stack, as the explicit path does -> reddens.
    }

    @Test
    void aLETHALHitAccruesNOTHINGAtAll() {
        // THE ORDERING INVERSION THIS PREVENTS, measured rather than assumed: CombatantStats.damage
        // publishes its seam event BEFORE returning, and MobDeathSystem.onChange calls setHealth(0)
        // SYNCHRONOUSLY -- which fires removal, which calls scorch().forget(id). Accruing here would
        // register a RepeatingTask AFTER the cleanup meant to cancel it, leaving a map entry and a
        // 160-tick task per lethal fire kill with nothing left to cancel them.
        //
        // It also keeps Ignite's death-gate unambiguous: a killing blow never scorches the target it
        // just killed, so it cannot ignite something that was not already alight.
        //
        // AND SINCE THE 2026-09-09 RULING THIS IS LOAD-BEARING RATHER THAN TIDY. "Any mob that dies
        // while scorched ignites" means that if the killing blow accrued, every fire-weapon kill
        // would grant a first stack to a mob that had none and then detonate it. This skip is the
        // only thing between the ruling and "every emberblade kill explodes".
        assertTrue(accrue("fire", new DamageOutcome(25.0, 0.0), 30.0).isEmpty(),
                "a hit that took the target to zero accrues nothing");
        // Mutation: loosen the gate to newCurrent >= 0 -> ONLY THIS ROW reddens.
        // Mutation: delete the accrual call entirely -> ONLY THE ROW ABOVE reddens.
        // That is why both exist: neither alone can tell a broken gate from a missing feature.
    }

    @Test
    void aCombatantALREADYAtZeroAccruesNothingEither() {
        // The predicate is "still standing", NOT "this hit caused the transition" -- and the
        // difference is reachable, because MobDeathSystem.shouldKill excludes players, so a player
        // at zero custom health stays tracked and alive at the floor. A further hit on them reports
        // reachedZero == false while current is still 0. Keyed on the transition, that hit would
        // look like it landed on a live target.
        assertTrue(accrue("fire", new DamageOutcome(4.0, 0.0), 8.0).isEmpty(),
                "zero health is zero health, whichever hit put them there");
    }

    // --- The loop guard, witnessed BEHAVIOURALLY ------------------------------------------------


    @Test
    void anINERTHitWEARSItsElementAndAccruesNOTHINGFromIt() {
        // THE LOOP GUARD, AND THIS ROW IS WHY THE RULE IS A NAMED VALUE RATHER THAN AN ABSENCE.
        //
        // While scorch's burn carried NO element, the guard's only witness was behavioural -- "a burn
        // tick accrues no stacks" -- and that row COULD NOT TELL THE GUARD FROM A REGISTRY MISS,
        // because a null element misses the registry anyway. It passed either way, which is the same
        // ambiguity a dead guard has.
        //
        // Here the element is REAL and RESOLVES: "fire" is registered, declares scorch, and the hit
        // is non-lethal and large enough to buy stacks. Every other reason to return empty is
        // excluded, so INERT is the ONLY thing that can produce this observation.
        var elements = elements("fire", "scorch");

        assertTrue(ElementAccrual.forHit(elements, statuses(), "fire",
                        AccrualRule.INERT, new DamageOutcome(25.0, 75.0), 30.0).isEmpty(),
                "an INERT hit accrues nothing, even though its element resolves and would accrue");

        // THE CONTROL, in the same fixture and differing only in the rule. Without it, "returns
        // empty" would pass against an implementation that never accrues at all.
        assertTrue(ElementAccrual.forHit(elements, statuses(), "fire",
                        AccrualRule.ACCRUES, new DamageOutcome(25.0, 75.0), 30.0).isPresent(),
                "and the SAME hit accruing is what proves the rule is doing the work");
        // Mutation: ignore the rule (drop the !accrual.accrues() early return) -> the first assertion
        // reddens and the second stays green, which is the pair discriminating.
    }

    @Test
    void theTWOReasonsForNoAccrualAreDIFFERENTFactsWithTheSameOutcome() {
        // Recorded as a row because collapsing them into "the accrual guard" is how one gets deleted.
        //
        //   null element  =  the hit HAS no element      -> fall damage, a thorns reflect, /rpg apply
        //   INERT         =  it HAS one and must not accrue -> scorch's own burn, and nothing else
        //
        // The short applyDamage arities still mean the FIRST, correctly, for every caller they have.
        // Retrofitting INERT onto them would look tidy and would lose the distinction: lava genuinely
        // has no element, and a burn tick genuinely does.
        var elements = elements("fire", "scorch");

        assertTrue(ElementAccrual.forHit(elements, statuses(), null,
                        AccrualRule.ACCRUES, new DamageOutcome(25.0, 75.0), 30.0).isEmpty(),
                "no element at all -- nothing to accrue, whatever the rule says");
        assertTrue(ElementAccrual.forHit(elements, statuses(), "fire",
                        AccrualRule.INERT, new DamageOutcome(25.0, 75.0), 30.0).isEmpty(),
                "an element, but forbidden from accruing -- a different fact, same outcome");
    }

    @Test
    void anUnknownElementAccruesNothingRatherThanThrowing() {
        // Boot validation names a dangling element; a hit must not be where it is discovered.
        assertTrue(accrue("plazma", new DamageOutcome(25.0, 75.0), 30.0).isEmpty());
    }

    // --- Scope: one status, and only one that can accrue -----------------------------------------

    @Test
    void anElementDeclaringNOStatusAccruesNothing() {
        // Six of the seven shipped elements. Absent means off.
        assertTrue(ElementAccrual.forHit(elements("kinetic", null), statuses(), "kinetic",
                AccrualRule.ACCRUES, new DamageOutcome(25.0, 75.0), 30.0).isEmpty());
    }

    @Test
    void aNONSTACKINGStatusAccruesNothingEvenThoughItRESOLVES() {
        // applies_status: rooted resolves perfectly -- the file exists, the id is right -- and has
        // no stack count and no duration of its own, so there is nothing to apply.
        // ContentValidator.validateElements NAMES this at boot, which is why this returns empty
        // silently rather than warning per hit.
        for (String id : new String[] {"rooted", "soaked", "surge"}) {
            assertTrue(ElementAccrual.forHit(elements("nature", id), statuses(), "nature",
                            AccrualRule.ACCRUES, new DamageOutcome(25.0, 75.0), 30.0).isEmpty(),
                    id + " cannot accrue");
        }
        // Mutation: give the non-Scorch switch arms a scorch application -> all three redden.
    }

    // --- The chip hit, and a hit that lands nothing ----------------------------------------------

    @Test
    void aHitThatLANDSNothingAccruesNothing() {
        // dealt <= 0 is a fully absorbed hit. stacksFor returns 0, and returning empty rather than
        // a ScorchAccrual with stacks 0 matters: ScorchStatus.apply early-returns on a non-positive
        // count, so a caller could not tell a refused application from an accepted no-op.
        assertTrue(accrue("fire", new DamageOutcome(0.0, 75.0), 30.0).isEmpty());
    }

    @Test
    void aCHIPHitStillBuysASTACKBecauseAnyLandedFireDamageBurns() {
        // The floor. solar_grenade's field tick declares amount: 2, and against armour the landed
        // figure is 1.67 -- which floored to zero before Scorch.stacksFor gained max(1, ...), so the
        // shipped lingering field would have stopped scorching armoured targets entirely.
        var accrued = accrue("fire", new DamageOutcome(1.67, 18.0), 2.0).orElseThrow();

        assertEquals(1, accrued.stacks(), "1.67 landed still buys one stack");
        assertEquals(1.0, accrued.cap(), EPS,
                "capped at HALF the authored 2, not at the 1.67 that landed");
    }

    // --- Crit: out of the cap, into the stacks -----------------------------------------------------

    @Test
    void aCRITAndANORMALHitOfTheSameBaseCapTheSAMEAndStackDIFFERENTLY() {
        // ONE RULE DECIDES BOTH HALVES, AND THERE IS NO CRIT SPECIAL CASE ANYWHERE:
        //
        //     STACKS MEASURE WHAT LANDED. THE CAP MEASURES WHAT WAS DECLARED.
        //
        // A crit changes what lands, so stacks move with it -- the same reason scorch.yml gives for
        // armour slowing accrual, "stacks come from damage actually landed". A crit does not change
        // what was declared, so the ceiling does not move. Crit-free stacks would mean armour affects
        // the count and crit does not, two treatments of one quantity with no principle separating
        // them, and it would make "1 per 2 damage dealt" false on its face.
        //
        // THE FIXTURE ONLY DISCRIMINATES ON THE CRIT ROW, WHICH IS THE POINT. BukkitCombatant hands
        // this the DECLARED magnitude -- amount / critMultiplier -- so a defect that used the
        // resolved amount instead is invisible at multiplier 1.0, where the two are equal. A fixture
        // without a crit in it would pass against exactly the bug this guards.
        double declared = 20.0;                 // a Flint Staff bolt, as authored
        double critMultiplier = 2.0;

        // NORMAL: nothing was multiplied, so declared == resolved.
        var normal = accrue("fire", new DamageOutcome(20.0, 340.0), declared).orElseThrow();
        // CRIT: 40 landed, but the DECLARED magnitude is still 20 -- what the caller recovers.
        var crit = accrue("fire", new DamageOutcome(40.0, 320.0), declared).orElseThrow();

        assertEquals(normal.cap(), crit.cap(), EPS,
                "SAME ceiling -- the crit did not raise it, which is the ruling");
        assertEquals(declared * Scorch.CAP_FRACTION, crit.cap(), EPS,
                "and it is half the DECLARED 20, not half the 40 that landed");

        assertEquals(10, normal.stacks(), "stacksFor(20)");
        assertEquals(20, crit.stacks(),
                "MORE stacks from the crit, because stacks measure what landed. NO consequence "
                        + "today: the burn rate is flat, and since the 2026-09-09 ruling Ignite "
                        + "reads no count either. Pinned as the arithmetic's own property, not "
                        + "because a consumer is coming");
        assertEquals(critMultiplier * normal.stacks(), (double) crit.stacks(), EPS,
                "exactly the multiplier's worth, since stacksFor is linear above its floor");
        // Mutation: cap from the resolved amount instead of the declared one -> the CRIT row's cap
        // becomes 20 against an expected 10 -> reddens. The normal row does NOT redden, because at
        // multiplier 1.0 declared and resolved are the same number.
    }

    // --- accruesScorch: the predicate Ignite's fire-kill clause shares with forHit ---------------

    @Test
    void accruesScorchIsTRUEForAnAccruingHitOfAScorchDeclaringElement() {
        assertTrue(ElementAccrual.accruesScorch(elements("fire", "scorch"), statuses(),
                "fire", AccrualRule.ACCRUES));
    }

    @Test
    void accruesScorchIsFALSEForAnINERTHitOfTheSameElement() {
        // THE ROW THAT KEEPS RULING 2 ALIVE UNDER THE FIRE-KILL CLAUSE, and it is the whole reason
        // the predicate takes the AccrualRule at all.
        //
        // Ignite's blast wears element "fire" -- it must, for the glyph and the effectiveness
        // matrix. So "a fire hit that kills ignites" would mean A BLAST THAT KILLS AN UNSCORCHED MOB
        // IGNITES IT, and the cascade would recruit everything it killed: the terminator becomes
        // "you run out of mobs", which is exactly what INERT was chosen to prevent.
        //
        // The blast passes INERT and every weapon hit passes ACCRUES, so this ONE clause separates
        // them. Same element, same registries, opposite answer.
        assertFalse(ElementAccrual.accruesScorch(elements("fire", "scorch"), statuses(),
                "fire", AccrualRule.INERT),
                "an INERT hit feeds neither accrual nor ignition -- the same job, stated once");
        // Mutation: drop the accrual.accrues() clause -> this reddens, AND the cascade becomes
        // self-recruiting in game. Nothing else in the suite would notice the second half.
    }

    @Test
    void accruesScorchIsFALSEForAnElementThatDeclaresNoStatusOrADIFFERENTOne() {
        assertFalse(ElementAccrual.accruesScorch(elements("kinetic", null), statuses(),
                "kinetic", AccrualRule.ACCRUES), "no applies_status: nothing accrues, nothing ignites");
        assertFalse(ElementAccrual.accruesScorch(elements("nature", "rooted"), statuses(),
                "nature", AccrualRule.ACCRUES),
                "declares a status that is NOT scorch -- resolves perfectly and still does not ignite");
        assertFalse(ElementAccrual.accruesScorch(elements("fire", "scorch"), statuses(),
                "void", AccrualRule.ACCRUES), "an element the registry has never heard of");
        assertFalse(ElementAccrual.accruesScorch(elements("fire", "scorch"), statuses(),
                null, AccrualRule.ACCRUES), "and a hit wearing no element at all");
        // "rooted" is the discriminating case: it EXISTS in the status registry and resolves, so a
        // predicate that merely checked "the element declares something" would pass it. Only the
        // type match rejects it. Mutation: return true for any non-null status -> this reddens.
    }

    @Test
    void forHitAndTheIgniteClauseDifferByTheLETHALGATEAloneAndNothingElse() {
        // THE ANTI-DRIFT ROW. Two call sites ask this question -- forHit for accrual, and
        // BukkitCombatant for Ignite's fire-kill clause -- and the ONLY thing that may differ
        // between them is lethality. Written twice, the copies would be two authorities on what "a
        // fire hit" means and would part company the first time an element gained a status.
        var elements = elements("fire", "scorch");
        var statuses = statuses();

        // Same hit, twice, differing ONLY in whether it left the target standing.
        DamageOutcome survived = new DamageOutcome(25.0, 75.0);
        DamageOutcome killed = new DamageOutcome(25.0, 0.0);

        assertTrue(ElementAccrual.accruesScorch(elements, statuses, "fire", AccrualRule.ACCRUES),
                "the shared predicate says yes -- it does not know about lethality at all");
        assertTrue(ElementAccrual.forHit(elements, statuses, "fire", AccrualRule.ACCRUES,
                survived, 30.0).isPresent(), "and forHit agrees while the target is standing");
        assertTrue(ElementAccrual.forHit(elements, statuses, "fire", AccrualRule.ACCRUES,
                killed, 30.0).isEmpty(), "but refuses the lethal one -- the gate, and only the gate");
        // Ignite's clause is that third line INVERTED, over the same predicate. Mutation: give the
        // predicate its own lethality check -> the first assertion reddens; give forHit its own
        // element check -> nothing reddens here, which is why the extraction is the guard rather
        // than this row.
    }
}
