package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Burst;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Damage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAN-build-system.md section 3.5: modify first, append second, per player, memoised. */
class AspectApplicationTest {

    /** solar_lance as shipped: a ray, 25 mana, 100 ticks, a visual then a Damage 12. */
    private static final AbilityDefinition LANCE = new AbilityDefinition("solar_lance", "Solar Lance", "fire", 100,
            new ResourceCost("mana", 25), new CastSpec.Ray(30, null),
            List.of(new EffectSpec.Visual("solar_lance"), new Damage(12, "fire")));

    private static AspectDefinition aspect(String id, String target, List<EffectSpec> addOnHit, List<NumberChange> modify) {
        return new AspectDefinition(id, id, List.of(), target, addOnHit, List.of(), modify);
    }

    /** searing_lance (section 2.4.1): -25% damage, and an appended burst of 4. */
    private static final AspectDefinition SEARING = aspect("searing_lance", "solar_lance",
            List.of(new Burst(2.5, List.of(new Damage(4, "fire")))),
            List.of(new NumberChange(AspectField.DAMAGE_AMOUNT, 0, -25)));

    @Test
    void noAspectIsTheBaseItself() {
        assertSame(LANCE, AspectApplication.deriveUncached(LANCE, List.of()));
    }

    /** Appended AFTER the base, never before; the id is kept, so the cooldown key is the ability's. */
    @Test
    void appendsNeverPrependsAndKeepsTheId() {
        AspectDefinition plain = aspect("plain", "solar_lance", List.of(new Burst(2.5, List.of(new Damage(4, "fire")))), List.of());
        AbilityDefinition derived = AspectApplication.deriveUncached(LANCE, List.of(plain));
        assertEquals("solar_lance", derived.id());
        assertEquals(LANCE.onHit(), derived.onHit().subList(0, 2), "the base effects first, unchanged");
        assertEquals(3, derived.onHit().size());
        assertTrue(derived.onHit().get(2) instanceof Burst);
    }

    /** THE ROW THE "prepend instead of append" MUTATION REDDENS: the headline is the FIRST damage. */
    @Test
    void anAppendLeavesTheHeadlineDamage() {
        AspectDefinition bigBurst = aspect("big", "solar_lance", List.of(new Damage(99, "fire")), List.of());
        AbilityDefinition derived = AspectApplication.deriveUncached(LANCE, List.of(bigBurst));
        assertEquals(12.0, DamagePayload.headlineDamage(derived.onHit(), 0), 1e-12);
    }

    @Test
    void twoAspectsAppendInSlotOrder() {
        AspectDefinition a = aspect("a", "solar_lance", List.of(new EffectSpec.Visual("first")), List.of());
        AspectDefinition b = aspect("b", "solar_lance", List.of(new EffectSpec.Visual("second")), List.of());
        List<EffectSpec> onHit = AspectApplication.deriveUncached(LANCE, List.of(a, b)).onHit();
        assertEquals(new EffectSpec.Visual("first"), onHit.get(2));
        assertEquals(new EffectSpec.Visual("second"), onHit.get(3));
    }

    /**
     * THE ROW THE "append first, modify second" MUTATION REDDENS: A's -25% damage reaches the lance's own 12
     * (-> 9) and NEVER the burst an aspect appended, which stays at its authored 4.
     */
    @Test
    void modifyNeverTouchesAnAppendedEffect() {
        AbilityDefinition derived = AspectApplication.deriveUncached(LANCE, List.of(SEARING));
        assertEquals(new Damage(9.0, "fire"), derived.onHit().get(1), "the lance's own hit: 12 x 0.75");
        Burst appended = (Burst) derived.onHit().get(2);
        assertEquals(new Damage(4, "fire"), appended.effects().get(0), "the appended burst keeps its authored 4");
    }

    /** damage.amount reaches EVERY Damage, nested in burst, area and throw_embers.burst included. */
    @Test
    void damageAmountReachesNestedDamage() {
        AbilityDefinition nested = new AbilityDefinition("n", "N", "fire", 100, new ResourceCost("mana", 10),
                new CastSpec.Self(),
                List.of(new Burst(2, List.of(new Damage(10, "fire"))),
                        new EffectSpec.Area(3, 60, 20, List.of(new Damage(20, "fire"))),
                        new EffectSpec.ThrowEmbers(List.of(0.0), 0.6, 0.25, "blaze_powder", 30,
                                new Burst(4, List.of(new Damage(8, "fire"))), null, null)));
        AspectDefinition half = aspect("half", "n", List.of(), List.of(new NumberChange(AspectField.DAMAGE_AMOUNT, 0, -50)));
        List<EffectSpec> onHit = AspectApplication.deriveUncached(nested, List.of(half)).onHit();
        assertEquals(new Damage(5.0, "fire"), ((Burst) onHit.get(0)).effects().get(0));
        assertEquals(new Damage(10.0, "fire"), ((EffectSpec.Area) onHit.get(1)).effects().get(0));
        assertEquals(new Damage(4.0, "fire"), ((EffectSpec.ThrowEmbers) onHit.get(2)).burst().effects().get(0));
    }

    /** banked_embers is HAND-BUILT here: its content file was deleted by ruling 26. It pins section 2.4.1's example. */
    @Test
    void cooldownAndCostAreModified() {
        AbilityDefinition recall = new AbilityDefinition("recall", "Recall", "fire", 200,
                new ResourceCost("mana", 35), new CastSpec.Self(), List.of(new Damage(8, "fire")));
        AspectDefinition banked = aspect("banked_embers", "recall", List.of(),
                List.of(new NumberChange(AspectField.COST, 10, 0), new NumberChange(AspectField.COOLDOWN_TICKS, 0, 20)));
        AbilityDefinition derived = AspectApplication.deriveUncached(recall, List.of(banked));
        assertEquals(45.0, derived.cost().amount(), 1e-12);
        assertEquals(240, derived.cooldownTicks());
    }

    /** An aspect whose target is not equipped contributes to NEITHER the appends nor the sums. */
    @Test
    void anInactiveAspectContributesNothing() {
        List<AspectDefinition> slotted = List.of(SEARING);
        assertEquals(List.of(), AspectApplication.activeFor("solar_lance", slotted, Set.of("recall", "ember_step")),
                "solar_lance is not equipped: the aspect is inactive");
        assertEquals(List.of(SEARING), AspectApplication.activeFor("solar_lance", slotted, Set.of("solar_lance")));
        AbilityDefinition derived = AspectApplication.deriveUncached(LANCE,
                AspectApplication.activeFor("solar_lance", slotted, Set.of("recall")));
        assertSame(LANCE, derived, "inactive: the base, untouched");
    }

    /** An aspect for another ability never reaches this one, even if handed in. */
    @Test
    void anAspectForAnotherTargetIsIgnored() {
        AspectDefinition other = aspect("o", "ember_step", List.of(new EffectSpec.Visual("x")), List.of());
        assertSame(LANCE, AspectApplication.deriveUncached(LANCE, List.of(other)));
    }

    @Test
    void theSameInputsReturnTheSameMemoisedRecord() {
        AspectApplication application = new AspectApplication();
        AbilityDefinition first = application.derive(LANCE, List.of(SEARING));
        assertSame(first, application.derive(LANCE, List.of(SEARING)));
        assertEquals(AspectApplication.deriveUncached(LANCE, List.of(SEARING)), first);
    }

    // ------------------------------------------------------------------ behaviour fragments (section 7.3)

    private static FragmentDefinition behaviour(String id, String target, EffectSpec... addOnHit) {
        return new FragmentDefinition(id, id, "blaze_powder", List.of(), java.util.Map.of(), target, List.of(addOnHit));
    }

    /** Aspects' appends first, in aspect-slot order; then the fragments', in fragment-slot order. */
    @Test
    void fragmentAppendsFollowTheAspectsInSlotOrder() {
        AspectDefinition a = aspect("a", "solar_lance", List.of(new EffectSpec.Visual("aspect")), List.of());
        FragmentDefinition f1 = behaviour("f1", "solar_lance", new EffectSpec.Visual("fragment1"));
        FragmentDefinition f2 = behaviour("f2", "solar_lance", new EffectSpec.Visual("fragment2"));
        List<EffectSpec> onHit = AspectApplication.deriveUncached(LANCE, List.of(a), List.of(f1, f2)).onHit();
        assertEquals(List.of(new EffectSpec.Visual("aspect"), new EffectSpec.Visual("fragment1"),
                new EffectSpec.Visual("fragment2")), onHit.subList(2, 5));
    }

    /** No aspect's modify reaches a fragment's appended effect: its 4 stays 4 under searing_lance's -25%. */
    @Test
    void modifyNeverTouchesAFragmentsAppend() {
        FragmentDefinition f = behaviour("f", "solar_lance", new Damage(4, "fire"));
        AbilityDefinition derived = AspectApplication.deriveUncached(LANCE, List.of(SEARING), List.of(f));
        assertEquals(new Damage(9.0, "fire"), derived.onHit().get(1));
        assertEquals(new Damage(4, "fire"), derived.onHit().get(3));
    }

    /** A fragment alone derives too (no aspect slotted), and keeps the id. */
    @Test
    void aFragmentAloneAppends() {
        FragmentDefinition f = behaviour("f", "solar_lance", new EffectSpec.Visual("v"));
        AbilityDefinition derived = AspectApplication.deriveUncached(LANCE, List.of(), List.of(f));
        assertEquals("solar_lance", derived.id());
        assertEquals(new EffectSpec.Visual("v"), derived.onHit().get(2));
    }

    /**
     * THE INACTIVE-FRAGMENT ROW: Ember Cache while Recall is not equipped is inactive -- the aspects' own rule.
     * A stat fragment has no target and is never active on anything.
     */
    @Test
    void aFragmentWhoseTargetIsNotEquippedIsInactive() {
        FragmentDefinition cache = behaviour("fragment_ember_cache", "recall", new EffectSpec.Visual("v"));
        FragmentDefinition vigor = new FragmentDefinition("vigor", "Vigor", "red_dye", List.of(),
                java.util.Map.of(io.github.butterflysmp.rpg.core.accessory.AccessoryStat.MAX_HEALTH, 4.0));
        java.util.List<FragmentDefinition> slotted = java.util.Arrays.asList(vigor, null, cache, null);
        assertEquals(List.of(), AspectApplication.activeFragmentsFor("recall", slotted, Set.of("solar_lance")),
                "recall is not equipped: Ember Cache is inactive");
        assertEquals(List.of(cache), AspectApplication.activeFragmentsFor("recall", slotted, Set.of("recall", "solar_lance")));
        assertEquals(List.of(), AspectApplication.activeFragmentsFor("solar_lance", slotted, Set.of("recall", "solar_lance")));
    }

    @Test
    void theFragmentsAreInTheMemoKey() {
        AspectApplication application = new AspectApplication();
        FragmentDefinition f = behaviour("f", "solar_lance", new EffectSpec.Visual("v"));
        AbilityDefinition with = application.derive(LANCE, List.of(), List.of(f));
        assertEquals(3, with.onHit().size());
        assertSame(LANCE, application.derive(LANCE, List.of(), List.of()), "no fragment: the base, not the memo's");
        assertSame(with, application.derive(LANCE, List.of(), List.of(f)));
    }

    // ------------------------------------------------------------------ the loader's checks

    /** A modify matching ZERO effects in its target is refused: knockback.strength on a ray that pushes nothing. */
    @Test
    void aModifyMatchingNothingIsRefused() {
        AspectDefinition push = aspect("push", "solar_lance", List.of(),
                List.of(new NumberChange(AspectField.KNOCKBACK_STRENGTH, 0, 20)));
        List<String> refusals = AspectApplication.refusals(LANCE, List.of(push), id -> true);
        assertTrue(refusals.stream().anyMatch(r -> r.contains("knockback.strength") && r.contains("matches nothing")),
                refusals.toString());
        assertEquals(List.of(), AspectApplication.refusals(LANCE, List.of(SEARING), id -> true));
    }

    /** banked_embers' control (section 2.4.1; the aspect's content file was deleted by ruling 26, the arithmetic stands): +15% on 200 is 230, refused naming 228 and 232. */
    @Test
    void anIllegalResolutionIsRefusedNamingTheNeighbours() {
        AbilityDefinition recall = new AbilityDefinition("recall", "Recall", "fire", 200,
                new ResourceCost("mana", 35), new CastSpec.Self(), List.of(new Damage(8, "fire")));
        AspectDefinition fifteen = aspect("fifteen", "recall", List.of(),
                List.of(new NumberChange(AspectField.COOLDOWN_TICKS, 0, 15)));
        List<String> refusals = AspectApplication.refusals(recall, List.of(fifteen), id -> true);
        assertTrue(refusals.size() == 1 && refusals.get(0).contains("228") && refusals.get(0).contains("232"),
                refusals.toString());
    }

    /** status.duration_ticks: refused on a status kind not traced continuous (scorch among them). */
    @Test
    void aStatusDurationOnAnUntracedKindIsRefused() {
        AbilityDefinition scorcher = new AbilityDefinition("s", "S", "fire", 100, new ResourceCost("mana", 10),
                new CastSpec.Self(), List.of(new EffectSpec.Status("scorch", 60, 0)));
        AspectDefinition longer = aspect("longer", "s", List.of(),
                List.of(new NumberChange(AspectField.STATUS_DURATION_TICKS, 20, 0)));
        List<String> refusals = AspectApplication.refusals(scorcher, List.of(longer), id -> !id.equals("scorch"));
        assertTrue(refusals.stream().anyMatch(r -> r.contains("scorch")), refusals.toString());
        assertEquals(List.of(), AspectApplication.refusals(scorcher, List.of(longer), id -> true), "traced: legal");
    }

    /** A PAIR can be illegal though each aspect alone is legal: -60% and -60% on one damage. */
    @Test
    void anIllegalPairIsRefusedThoughEachAloneIsLegal() {
        AspectDefinition a = aspect("a", "solar_lance", List.of(), List.of(new NumberChange(AspectField.DAMAGE_AMOUNT, 0, -60)));
        AspectDefinition b = aspect("b", "solar_lance", List.of(), List.of(new NumberChange(AspectField.DAMAGE_AMOUNT, 0, -60)));
        assertEquals(List.of(), AspectApplication.refusals(LANCE, List.of(a), id -> true));
        assertEquals(List.of(), AspectApplication.refusals(LANCE, List.of(b), id -> true));
        assertTrue(!AspectApplication.refusals(LANCE, List.of(a, b), id -> true).isEmpty(), "-120% together");
    }
}
