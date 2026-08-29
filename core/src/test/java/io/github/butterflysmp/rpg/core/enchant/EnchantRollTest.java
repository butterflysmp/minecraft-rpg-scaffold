package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Rollable;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shippable bugs this guards, in the order they would hurt:
 *
 * <ul>
 *   <li>A roll offering an enchant the weapon can never use -- Power on a sword. The class gate is
 *       the only thing between a player and a candidate that is inert the moment they buy it.
 *   <li>A roll producing a slot count the table cannot render, which {@code EnchantMenuLayout}
 *       refuses at the door: the weapon would become un-enchantable rather than mis-rendered.
 *   <li>A candidate arriving UNLOCKED, which hands out for free the thing the whole economy pass
 *       exists to charge for.
 *   <li>A slot offering the same enchant twice, which {@code EnchantSlot} throws on -- from inside
 *       a mint, where an exception loses the item rather than the roll.
 * </ul>
 *
 * <p>The draws are fed as literal doubles, the way {@code UnbreakingTest} feeds
 * {@code Unbreaking.consumes}: the reason the draw was left at the call site is so these numbers can
 * be exact. The per-decision tests own the boundaries; the one end-to-end test owns the shape, so no
 * boundary assertion depends on the draw ORDER.
 */
class EnchantRollTest {

    // The shipped roster, id for id and class for class. `unbreaking` is universal == null class.
    private static final Rollable SHARPNESS = new Rollable("sharpness", GearClass.MELEE);
    private static final Rollable POWER = new Rollable("power", GearClass.RANGER);
    private static final Rollable ATTUNEMENT = new Rollable("attunement", GearClass.MAGE);
    private static final Rollable BULWARK = new Rollable("bulwark", GearClass.SHIELD);
    private static final Rollable RIPOSTE = new Rollable("riposte", GearClass.SHIELD);
    private static final Rollable UNBREAKING = new Rollable("unbreaking", null);
    private static final List<Rollable> ROSTER =
            List.of(SHARPNESS, POWER, ATTUNEMENT, BULWARK, RIPOSTE, UNBREAKING);

    /**
     * Literal draws, consumed in order. Running off the end throws
     * {@code ArrayIndexOutOfBoundsException} on purpose: a roll that asked for more draws than the
     * test accounted for has changed its arity, and that should be loud rather than absorbed.
     */
    private static DoubleSupplier draws(double... values) {
        int[] cursor = {0};
        return () -> values[cursor[0]++];
    }

    /** Every draw the same value. For the tests that assert a property over any draw at all. */
    private static DoubleSupplier always(double value) {
        return () -> value;
    }

    private static int distinctCount(List<String> ids) {
        return (int) ids.stream().distinct().count();
    }

    private static List<String> idsIn(EnchantSlot slot) {
        List<String> ids = new ArrayList<>();
        for (EnchantCandidate candidate : slot.candidates()) ids.add(candidate.enchantId());
        return ids;
    }

    // ---- the shape ------------------------------------------------------------------------

    @Test
    void everyRolledWeaponGetsExactlyThreeSlots() {
        // Fixed, not tier-varied, and equal to what the table renders. A weapon with four would be
        // refused by EnchantMenuLayout.overflow and could never be enchanted at all.
        assertEquals(3, EnchantRoll.roll(GearClass.MELEE, ROSTER, always(0.0)).slots().size());
        assertEquals(3, EnchantRoll.roll(GearClass.MAGE, ROSTER, always(0.99)).slots().size());
        assertEquals(3, EnchantRoll.SLOTS);
        // Mutation: SLOTS 3 -> 2, or the loop bound -> reddens.
    }

    @Test
    void everyRolledCandidateIsLockedAndNothingIsActive() {
        EnchantState state = EnchantRoll.roll(GearClass.MELEE, ROSTER, always(0.99));
        for (EnchantSlot slot : state.slots()) {
            assertEquals(EnchantSlot.NONE, slot.activeIndex(), "a fresh roll activates nothing");
            for (EnchantCandidate candidate : slot.candidates()) {
                assertEquals(0, candidate.level());
                assertTrue(candidate.isLocked());
            }
        }
        // The consequence that matters: a freshly rolled weapon is mechanically unenchanted.
        assertTrue(state.effective().isEmpty(), "nothing takes effect until the player unlocks");
        // Mutation: `new EnchantCandidate(id, 0)` -> `1` -> reddens on all three.
    }

    @Test
    void withinASlotTheCandidatesAreDistinct() {
        // Draws chosen to ask for two candidates in every slot from a pool of exactly two, which is
        // the case a non-shrinking pool would fill with the same id twice.
        EnchantState state = EnchantRoll.roll(GearClass.MELEE, ROSTER, always(0.99));
        for (EnchantSlot slot : state.slots()) {
            List<String> ids = idsIn(slot);
            assertEquals(ids.size(), distinctCount(ids), "a slot offers an enchant twice: " + ids);
        }
        // Mutation: drop `remaining.remove(picked)` -> RUN 2026-08-26, reddened 7 tests with
        // "slot offers 'unbreaking' twice; a slot's candidates must be distinct". EnchantSlot's own
        // constructor throws BEFORE the assertion below runs, so this case guards by erroring, not
        // by asserting. The assertion stays as defence in depth against that rule moving.
    }

    @Test
    void theSameEnchantMayBeOfferedInMoreThanOneSlot() {
        // No mutual exclusion across slots: EnchantState.effective() resolves a duplicate to the
        // HIGHEST level either slot holds it at, never the sum, so offering it twice is legal.
        EnchantState state = EnchantRoll.roll(GearClass.MELEE, ROSTER, always(0.0));
        for (EnchantSlot slot : state.slots()) {
            assertEquals(List.of("sharpness"), idsIn(slot));
        }
        // Mutation: hoist `remaining` out of the slot loop (one shrinking pool for the whole
        // weapon) -> slots 1 and 2 can no longer repeat sharpness -> reddens.
    }

    // ---- the class gate -------------------------------------------------------------------

    @Test
    void aMeleeWeaponIsNeverOfferedPowerOrAttunement() {
        EnchantState state = EnchantRoll.roll(GearClass.MELEE, ROSTER, always(0.99));
        for (EnchantSlot slot : state.slots()) {
            for (String id : idsIn(slot)) {
                assertTrue(id.equals("sharpness") || id.equals("unbreaking"),
                        "a melee weapon was offered '" + id + "'");
            }
        }
        // Mutation: drop the class comparison in poolFor -> reddens.
    }

    @Test
    void aShieldIsNeverOfferedAWeaponEnchantAndNoWeaponIsEverOfferedBulwark() {
        // The gate in BOTH directions, and the second half is the one nothing guarded before Slice 2.
        // A shield offered Sharpness would sell a player a damage multiplier that DamageEnchantItems
        // never reads (it looks at the main hand's weapon); a sword offered Bulwark would sell a
        // block bonus read off a stack that cannot block.
        for (String id : idsIn(EnchantRoll.roll(GearClass.SHIELD, ROSTER, always(0.99)).slots().get(0))) {
            assertTrue(id.equals("bulwark") || id.equals("riposte") || id.equals("unbreaking"),
                    "a shield was offered '" + id + "'");
        }
        // BOTH shield enchants must stay off every weapon. Asserted per enchant rather than by
        // counting, because this roster is hand-written and can drift from the yml files
        // independently -- a Rollable declared with the wrong GearClass here would otherwise only
        // show up on a server.
        for (GearClass weapon : List.of(GearClass.MELEE, GearClass.RANGER, GearClass.MAGE)) {
            assertFalse(EnchantRoll.poolFor(weapon, ROSTER).contains(BULWARK),
                    weapon + " was offered Bulwark -- a block enchant on a weapon");
            assertFalse(EnchantRoll.poolFor(weapon, ROSTER).contains(RIPOSTE),
                    weapon + " was offered Riposte -- a reflect read off a stack that cannot block");
        }
    }

    @Test
    void aShieldsPoolIsTHREEAndIsTheFirstShippedGearThatCanFillASlot() {
        // Bulwark + Riposte + Unbreaking. Every WEAPON class is still two (its own damage enchant
        // plus Unbreaking), so the shield is the only gear whose slot can hold three candidates.
        //
        // This is the moment EnchantMenuLayout.CANDIDATES == 3 stops being a constant pinned against
        // another constant and starts being exercised by a real roll of real content. Slice 2a
        // predicted it here by name, in the test this one replaces.
        assertEquals(3, EnchantRoll.poolFor(GearClass.SHIELD, ROSTER).size());
        assertEquals(2, EnchantRoll.poolFor(GearClass.MELEE, ROSTER).size());

        // == 3, not <= 3. At always(0.99) candidateCount(3, 0.99) IS 3, so a <= assertion would be
        // vacuous -- it would pass just as happily on a pool that had silently stayed at two.
        for (EnchantSlot slot : EnchantRoll.roll(GearClass.SHIELD, ROSTER, always(0.99)).slots()) {
            assertEquals(3, slot.candidates().size(),
                    "a full draw on a pool of three must fill all three cells of the slot");
        }

        // And the distinctness rule finally runs at pool size 3: the third pick comes from a pool
        // that has already been shrunk TWICE, which no shipped roster could reach before.
        for (EnchantSlot slot : EnchantRoll.roll(GearClass.SHIELD, ROSTER, always(0.99)).slots()) {
            assertEquals(3, distinctCount(idsIn(slot)),
                    "a slot offered the same enchant twice: " + idsIn(slot));
        }
    }

    @Test
    void thePoolIsTheClassEnchantPlusTheUniversalOne() {
        assertEquals(List.of(SHARPNESS, UNBREAKING), EnchantRoll.poolFor(GearClass.MELEE, ROSTER));
        assertEquals(List.of(POWER, UNBREAKING), EnchantRoll.poolFor(GearClass.RANGER, ROSTER));
        assertEquals(List.of(ATTUNEMENT, UNBREAKING), EnchantRoll.poolFor(GearClass.MAGE, ROSTER));
        // Roster ORDER is preserved here too: bulwark precedes unbreaking in the roster, so it
        // precedes it in the pool. That is what makes a fixed set of draws reproduce a fixed roll.
        assertEquals(List.of(BULWARK, RIPOSTE, UNBREAKING), EnchantRoll.poolFor(GearClass.SHIELD, ROSTER));
        // Roster order is preserved, so the pool is a deterministic function of the registry.
        // Mutation: drop the `weaponClass() != null` arm (universal stops matching) -> UNBREAKING
        // disappears from all three -> reddens.
    }

    @Test
    void aNullHeldClassLeavesOnlyTheUniversalEnchants() {
        // Unreachable today -- `class` is required on a weapon -- but total either way, and it must
        // not be the arm that grants a class enchant to a classless item.
        assertEquals(List.of(UNBREAKING), EnchantRoll.poolFor(null, ROSTER));
    }

    // ---- candidateCount: the boundaries ----------------------------------------------------

    @Test
    void aPoolOfTwoCutsBetweenOneAndTwoCandidatesAtAHalf() {
        // THE boundary of the count decision, at the pool size every class actually has today.
        assertEquals(1, EnchantRoll.candidateCount(2, 0.0));
        assertEquals(1, EnchantRoll.candidateCount(2, 0.4999999));
        assertEquals(2, EnchantRoll.candidateCount(2, 0.5), "0.5 is the first draw that offers two");
        assertEquals(2, EnchantRoll.candidateCount(2, 0.9999999));
        // Mutation: drop the `1 +` -> RUN 2026-08-26, reddened 3 tests. Note what it does NOT do:
        // Math.max(1, ..) absorbs the zero, so no slot goes empty -- every count collapses to 1
        // instead. Observed here: "0.5 is the first draw that offers two ==> expected: <2> but
        // was: <1>". The two guards are not redundant, they bound different ends.
    }

    @Test
    void aPoolOfTwoNeverProducesAThreeCandidateSlot() {
        // The accepted sparseness: with two valid enchants per class, a third candidate is
        // unreachable however the draw lands. It becomes reachable when the roster grows.
        for (double roll = 0.0; roll < 1.0; roll += 0.01) {
            int count = EnchantRoll.candidateCount(2, roll);
            assertTrue(count == 1 || count == 2, "count " + count + " from a pool of two");
        }
        // And the same, read off a whole roll rather than the decision in isolation.
        for (EnchantSlot slot : EnchantRoll.roll(GearClass.MELEE, ROSTER, always(0.99)).slots()) {
            assertTrue(slot.candidates().size() <= 2);
        }
        // Mutation: cap `MAX_CANDIDATES` instead of `min(poolSize, MAX_CANDIDATES)` -> a count of 3
        // is asked for, pick runs the pool dry and returns null -> the roll would silently short --
        // which is why the roll assertion above is here beside the arithmetic one.
    }

    @Test
    void aThreeCandidateSlotIsReachableOnceThePoolIsBigEnough() {
        // Proves the 1..3 range is real in isolation, from literal pool sizes rather than from any
        // roster. It was written when NO shipped gear could reach a pool of three; the shield does
        // now (Bulwark + Riposte + Unbreaking), and aShieldsPoolIsTHREE... is where that is asserted
        // end to end. This one stays because it pins the decision independently of what ships --
        // the day a fourth melee enchant lands, nothing here has to change either.
        assertEquals(3, EnchantRoll.candidateCount(3, 0.9999999));
        assertEquals(3, EnchantRoll.candidateCount(9, 0.9999999), "capped by the layout, not the pool");
        assertEquals(1, EnchantRoll.candidateCount(3, 0.0));
        assertEquals(2, EnchantRoll.candidateCount(3, 0.34));
        assertEquals(3, EnchantRoll.MAX_CANDIDATES);
    }

    @Test
    void anEmptyPoolOffersNothingRatherThanOne() {
        assertEquals(0, EnchantRoll.candidateCount(0, 0.0));
        assertEquals(0, EnchantRoll.candidateCount(0, 0.9999999));
        // Mutation: drop the `cap <= 0` arm -> returns 1 -> the roll asks pick for a candidate the
        // pool cannot supply -> reddens.
    }

    @Test
    void aDrawAtOrPastTheEndsCannotAskForMoreThanThePoolHolds() {
        // ThreadLocalRandom is half-open [0,1) so 1.0 never arrives from the shipped call site.
        // A hand-fed one must still not return a count pick cannot fill.
        assertEquals(2, EnchantRoll.candidateCount(2, 1.0));
        assertEquals(2, EnchantRoll.candidateCount(2, 7.5));
        assertEquals(1, EnchantRoll.candidateCount(2, -0.5));
    }

    // ---- pick: the boundaries --------------------------------------------------------------

    @Test
    void pickTakesTheFirstEntryAtZeroAndTheLastJustUnderOne() {
        List<Rollable> pool = List.of(SHARPNESS, UNBREAKING);
        assertEquals(SHARPNESS, EnchantRoll.pick(pool, 0.0));
        assertEquals(SHARPNESS, EnchantRoll.pick(pool, 0.4999999));
        assertEquals(UNBREAKING, EnchantRoll.pick(pool, 0.5), "0.5 is the first draw that reaches the second");
        assertEquals(UNBREAKING, EnchantRoll.pick(pool, 0.9999999));
        // Mutation: `roll * size` -> `roll * (size - 1)` -> 0.5 no longer reaches UNBREAKING -> reddens.
    }

    @Test
    void pickAddressesTheShrunkenPoolNotTheOriginal() {
        // The case distinctness creates: after one pick the pool is SHORTER, and the same draw must
        // now mean a different index. A pick written against the original size would run off the end
        // or skew every draw after the first.
        List<Rollable> shrunk = List.of(UNBREAKING);
        assertEquals(UNBREAKING, EnchantRoll.pick(shrunk, 0.0));
        assertEquals(UNBREAKING, EnchantRoll.pick(shrunk, 0.9999999), "a one-entry pool has one answer");
    }

    @Test
    void pickReturnsNullOnAnEmptyPoolRatherThanThrowing() {
        assertNull(EnchantRoll.pick(List.of(), 0.0));
        assertNull(EnchantRoll.pick(null, 0.0));
    }

    @Test
    void aDrawAtOrPastTheEndsStaysInsideThePool() {
        List<Rollable> pool = List.of(SHARPNESS, UNBREAKING);
        assertEquals(UNBREAKING, EnchantRoll.pick(pool, 1.0));
        assertEquals(SHARPNESS, EnchantRoll.pick(pool, -0.5));
    }

    // ---- the whole roll --------------------------------------------------------------------

    @Test
    void aFullRollReadsSlotBySlotFromItsDraws() {
        // The ONE test that depends on draw order, and it is here so nothing else has to be.
        // Pool for MELEE is [sharpness, unbreaking], so cap is 2.
        //   slot 0: count 0.0 -> 1;  pick 0.0 -> sharpness
        //   slot 1: count 0.9 -> 2;  pick 0.0 -> sharpness, then 0.0 of [unbreaking] -> unbreaking
        //   slot 2: count 0.9 -> 2;  pick 0.9 -> unbreaking, then 0.0 of [sharpness] -> sharpness
        EnchantState state = EnchantRoll.roll(GearClass.MELEE, ROSTER,
                draws(0.0, 0.0,   0.9, 0.0, 0.0,   0.9, 0.9, 0.0));

        assertEquals(3, state.slots().size());
        assertEquals(List.of("sharpness"), idsIn(state.slots().get(0)));
        assertEquals(List.of("sharpness", "unbreaking"), idsIn(state.slots().get(1)));
        assertEquals(List.of("unbreaking", "sharpness"), idsIn(state.slots().get(2)));

        // Counts genuinely vary within one weapon, which is the thing a fixed count would hide.
        assertNotEquals(state.slots().get(0).candidates().size(),
                state.slots().get(1).candidates().size(),
                "every slot rolled the same count -- the count draw is being ignored");
    }

    @Test
    void anEmptyPoolStillProducesThreeSlotsOfferingNothing() {
        // Unreachable while Unbreaking is universal. It must not throw from inside a mint, and it
        // must still produce a rolled item -- "decided, and it came to nothing" is a real outcome
        // the enchant_rolled flag exists to record separately from the state.
        EnchantState state = EnchantRoll.roll(GearClass.MELEE, List.of(), always(0.5));
        assertEquals(3, state.slots().size());
        for (EnchantSlot slot : state.slots()) {
            assertTrue(slot.candidates().isEmpty());
            assertEquals(EnchantSlot.NONE, slot.activeIndex());
        }
        assertFalse(state.isEmpty(), "three empty slots is not the same value as no slots");
    }

    @Test
    void aRolledStateSurvivesTheWireGrammar() {
        // The roll's output has to be storable: paper encodes it straight into the PDC, and a state
        // that did not round-trip would lose candidates the moment the item was re-minted.
        EnchantState rolled = EnchantRoll.roll(GearClass.MAGE, ROSTER,
                draws(0.0, 0.0,   0.9, 0.0, 0.0,   0.0, 0.9));
        EnchantState decoded = EnchantCodec.decode(EnchantCodec.encode(rolled));
        assertEquals(rolled, decoded);
        // Records give exact equality, so this is the whole state and not a spot check.
    }

    @Test
    void aRollableNeedsAnId() {
        assertThrows(IllegalArgumentException.class, () -> new Rollable(null, GearClass.MELEE));
        assertThrows(IllegalArgumentException.class, () -> new Rollable("  ", GearClass.MELEE));
    }
}
