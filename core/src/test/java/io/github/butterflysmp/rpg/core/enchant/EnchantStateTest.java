package io.github.butterflysmp.rpg.core.enchant;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The enchant state model, whose whole job is that A PLAYER'S UNLOCKS NEVER SILENTLY CHANGE.
 *
 * Every case here is a guard whose absence is a real, shippable bug: a swap that forgets what the
 * other candidate cost, a locked enchant that takes effect anyway, an active choice left pointing
 * at a candidate that has just been re-locked, or two slots holding the same enchant resolving to a
 * level no tooltip ever promised.
 *
 * The headline is {@link #swappingTheActiveCandidateRetainsEachCandidatesLevel}. That is the
 * difference between this model and the old repo's one-choice-per-slot shape, and it is the
 * property the richer model was built to have -- so it is asserted directly rather than implied.
 */
class EnchantStateTest {

    private static final String A = "unbreaking";
    private static final String B = "sharpness";

    /** One slot offering both candidates, neither unlocked, nothing active. */
    private static EnchantState twoCandidates() {
        return EnchantState.empty().addCandidate(0, A).addCandidate(0, B);
    }

    @Test
    void anEmptyStateHasNoSlotsAndNoActiveEnchants() {
        // What every freshly minted item carries, and what an absent PDC key decodes to. If this
        // were anything else, every unenchanted weapon in the game would render a tooltip block.
        EnchantState state = EnchantState.empty();
        assertTrue(state.isEmpty());
        assertEquals(List.of(), state.slots());
        assertEquals(List.of(), state.effective());
        assertEquals(0, state.activeLevel(A));
    }

    @Test
    void addingACandidateLeavesItLockedAtLevelZero() {
        // A candidate is an OFFER. Arriving unlocked would hand out for free exactly what the
        // deferred XP economy is meant to charge for, and would do it on every rolled item.
        EnchantState state = EnchantState.empty().addCandidate(0, A);

        assertEquals(1, state.slots().size());
        assertEquals(A, state.slots().get(0).candidates().get(0).enchantId());
        assertEquals(0, state.slots().get(0).candidates().get(0).level());
        assertTrue(state.slots().get(0).candidates().get(0).isLocked());
        assertEquals(EnchantSlot.NONE, state.slots().get(0).activeIndex(), "and it is not active");
        assertEquals(List.of(), state.effective());
        // Mutation: default a new candidate to level 1 -> it is unlocked, and effective() still
        // reports nothing (not active) -> but the level assertion reddens.
    }

    @Test
    void unlockingOneCandidateDoesNotTouchAnother() {
        // Two candidates, two independent levels. The old repo's slot could not represent this at
        // all: it carried ONE chosenLevel, so unlocking B would overwrite what A had cost.
        EnchantState state = twoCandidates()
                .withLevel(0, 0, 2)
                .withLevel(0, 1, 1);

        assertEquals(2, state.slots().get(0).candidates().get(0).level(), "A keeps its level");
        assertEquals(1, state.slots().get(0).candidates().get(1).level(), "B keeps its own");
        // Mutation: rebuild the slot with a single candidate (the old one-choice-per-slot shape)
        // -> A's level is gone -> reddens.
    }

    @Test
    void swappingTheActiveCandidateRetainsEachCandidatesLevel() {
        // THE HEADLINE INVARIANT of this pass. Unlock A to II and B to I, make A active, swap to B,
        // swap back: both levels must be exactly where they started. If a swap costs the outgoing
        // candidate its level, the player pays for the same unlock every time they change their
        // mind -- which is the whole reason the level rides the candidate and not the choice.
        EnchantState state = twoCandidates()
                .withLevel(0, 0, 2)
                .withLevel(0, 1, 1)
                .withActive(0, 0);

        assertEquals(2, state.activeLevel(A));
        assertEquals(0, state.activeLevel(B), "B is unlocked but not active, so it does nothing");

        EnchantState swapped = state.withActive(0, 1);
        assertEquals(1, swapped.activeLevel(B), "B now takes effect at the level it was unlocked to");
        assertEquals(0, swapped.activeLevel(A), "A stops taking effect...");
        assertEquals(2, swapped.slots().get(0).candidates().get(0).level(),
                "...but A KEEPS its unlocked level across the swap");

        EnchantState back = swapped.withActive(0, 0);
        assertEquals(2, back.activeLevel(A), "swapping back restores A at II, not at I and not at 0");
        assertEquals(1, back.slots().get(0).candidates().get(1).level(), "and B still holds its own");
        // Mutation: zero the outgoing candidate's level on a swap -> reddens.
        // Mutation: carry the level with the ACTIVE choice rather than the candidate -> B activates
        // at II -> reddens.
    }

    @Test
    void aLockedCandidateCannotBeMadeActive() {
        // A locked-but-active candidate resolves to level 0: it would render on the tooltip and do
        // nothing, or do nothing and not render, depending which reader asked. Refused at the one
        // place it could be created, so no reader has to remember the check.
        EnchantState state = twoCandidates();

        var ex = assertThrows(IllegalArgumentException.class, () -> state.withActive(0, 0));
        assertTrue(ex.getMessage().contains(A), "the message must name the enchant at fault");
        assertTrue(ex.getMessage().contains("locked"));
        // Mutation: drop the isLocked() check in withActive -> EnchantSlot's constructor still
        // catches it -> so ALSO mutate that one, and effective() then reports a level-0 enchant.
    }

    @Test
    void lockingTheActiveCandidateAlsoClearsActive() {
        // The other route to the same contradiction. Handled as a consequence rather than a refusal
        // so callers cannot forget to clear active first -- and note the OTHER candidate is
        // untouched, which is what stops a re-lock cascading.
        EnchantState state = twoCandidates()
                .withLevel(0, 0, 3)
                .withLevel(0, 1, 1)
                .withActive(0, 0)
                .withLevel(0, 0, 0);

        assertEquals(EnchantSlot.NONE, state.slots().get(0).activeIndex());
        assertEquals(List.of(), state.effective(), "nothing takes effect once it is re-locked");
        assertEquals(1, state.slots().get(0).candidates().get(1).level(), "B is untouched");
        // Mutation: leave activeIndex alone when re-locking -> EnchantSlot's constructor throws on
        // the rebuild -> reddens as an exception rather than a wrong value, which is still red.
    }

    @Test
    void deactivatingASlotKeepsEveryCandidatesLevel() {
        // The dev command's `deactivate`. Turning an enchant off must not be a way to lose it.
        EnchantState state = twoCandidates()
                .withLevel(0, 0, 3)
                .withLevel(0, 1, 2)
                .withActive(0, 0)
                .withoutActive(0);

        assertEquals(List.of(), state.effective());
        assertEquals(3, state.slots().get(0).candidates().get(0).level());
        assertEquals(2, state.slots().get(0).candidates().get(1).level());
    }

    @Test
    void theSameEnchantInTwoSlotsTakesTheHigherLevelNotTheSum() {
        // PROVISIONAL, and pinned so the provisional rule cannot drift unnoticed.
        //
        // TWO active slots at level I each. The levels are deliberately EQUAL and LOW: max gives 1,
        // sum gives 2, and both are inside MAX_LEVEL so effective()'s defensive clamp cannot mask
        // the difference. An earlier version of this test used I and III -- where sum is 4, which
        // the clamp quietly folds back to 3, the same answer max gives. A mutation run proved it:
        // swapping Math::max for Integer::sum reddened NOTHING. The case has to be chosen so the
        // two rules disagree BELOW the cap, or the test is only checking the cap.
        EnchantState twice = EnchantState.empty()
                .addCandidate(0, A).withLevel(0, 0, 1).withActive(0, 0)
                .addCandidate(1, A).withLevel(1, 0, 1).withActive(1, 0);

        assertEquals(List.of(new ActiveEnchant(A, 1)), twice.effective(),
                "two Unbreaking I do not add up to Unbreaking II");
        assertEquals(1, twice.activeLevel(A));
        // Mutation: sum the levels -> 2 -> reddens.

        // And the mixed case, which is what pins "higher", not merely "not the sum".
        EnchantState mixed = EnchantState.empty()
                .addCandidate(0, A).withLevel(0, 0, 1).withActive(0, 0)
                .addCandidate(1, A).withLevel(1, 0, 3).withActive(1, 0);

        assertEquals(List.of(new ActiveEnchant(A, 3)), mixed.effective(),
                "one entry, at the higher of the two levels");
        assertEquals(3, mixed.activeLevel(A));
        // Mutation: take the first rather than the max -> 1 -> reddens.
    }

    @Test
    void theHigherLevelWinsWhicheverSlotOrderItArrivesIn() {
        // Order-independence, asserted rather than assumed -- it is half the argument for choosing
        // max over first-wins, and a LinkedHashMap merge makes it easy to get right and easy to
        // break.
        EnchantState lowFirst = EnchantState.empty()
                .addCandidate(0, A).withLevel(0, 0, 1).withActive(0, 0)
                .addCandidate(1, A).withLevel(1, 0, 3).withActive(1, 0);
        EnchantState highFirst = EnchantState.empty()
                .addCandidate(0, A).withLevel(0, 0, 3).withActive(0, 0)
                .addCandidate(1, A).withLevel(1, 0, 1).withActive(1, 0);

        assertEquals(3, lowFirst.activeLevel(A));
        assertEquals(3, highFirst.activeLevel(A));
        // Mutation: `best.merge(id, level, (a, b) -> b)` (last wins) -> lowFirst gives 1 -> reddens.
    }

    @Test
    void twoDifferentEnchantsBothTakeEffect() {
        // The max rule must collapse DUPLICATES, not distinct enchants. Easy to over-apply.
        EnchantState state = EnchantState.empty()
                .addCandidate(0, A).withLevel(0, 0, 3).withActive(0, 0)
                .addCandidate(1, B).withLevel(1, 0, 1).withActive(1, 0);

        assertEquals(List.of(new ActiveEnchant(A, 3), new ActiveEnchant(B, 1)), state.effective());
    }

    @Test
    void activeLevelIsZeroForAnEnchantThatIsPresentButNotActive() {
        // Unlocked is not active. The seam reads this number, so reporting an unlocked-but-inactive
        // enchant's level would make every candidate on the item take effect at once.
        EnchantState state = twoCandidates().withLevel(0, 0, 3);

        assertEquals(0, state.activeLevel(A));
        // Mutation: report the candidate's level regardless of active -> 3 -> reddens.
    }

    @Test
    void activeLevelIsZeroForAnEnchantTheItemDoesNotHave() {
        assertEquals(0, EnchantState.empty().activeLevel(A));
        assertEquals(0, twoCandidates().withLevel(0, 1, 2).withActive(0, 1).activeLevel(A),
                "B being active says nothing about A");
    }

    @Test
    void activeLevelAndEffectiveCannotDisagree() {
        // They are one computation, deliberately, so the tooltip's number IS the seam's number.
        // Two scans would be two chances to apply the max rule differently.
        EnchantState state = EnchantState.empty()
                .addCandidate(0, A).withLevel(0, 0, 1).withActive(0, 0)
                .addCandidate(1, A).withLevel(1, 0, 3).withActive(1, 0);

        for (ActiveEnchant active : state.effective()) {
            assertEquals(active.level(), state.activeLevel(active.enchantId()),
                    "what the tooltip renders is what the seam applies");
        }
        // Mutation: reimplement activeLevel as its own scan taking the FIRST active match -> 1
        // against effective()'s 3 -> reddens.
    }

    @Test
    void aCandidateIdCarryingADelimiterIsRefusedSoTheCodecCannotBeTricked() {
        // This is what makes encode injective. An id containing a comma would decode back as TWO
        // candidates, so a player's unlocks would silently change shape on the next re-mint --
        // exactly the data loss this pass exists to prevent, arriving through the front door.
        for (String bad : List.of("un;breaking", "un,breaking", "un=breaking", "un:breaking")) {
            var ex = assertThrows(IllegalArgumentException.class,
                    () -> new EnchantCandidate(bad, 1), bad + " must be refused");
            assertTrue(ex.getMessage().contains(bad), "the message must echo the offending id");
        }
        // Mutation: drop the delimiter loop -> construction succeeds -> reddens here, and the
        // round trip in EnchantCodecTest splits it into two candidates.
    }

    @Test
    void aBlankOrMissingCandidateIdIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new EnchantCandidate(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new EnchantCandidate("", 1));
        assertThrows(IllegalArgumentException.class, () -> new EnchantCandidate("   ", 1));
    }

    @Test
    void aLevelOutsideTheAllowedRangeIsRefusedAtConstruction() {
        // Content-authored and hand-constructed values THROW; only the codec repairs, because only
        // the codec is reading something a different build may have written.
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new EnchantCandidate(A, EnchantState.MAX_LEVEL + 1));
        assertTrue(ex.getMessage().contains("4"), "the message must echo the bad value");
        assertThrows(IllegalArgumentException.class, () -> new EnchantCandidate(A, -1));
    }

    @Test
    void anActiveEnchantIsNeverLevelZero() {
        // ActiveEnchant means TAKING EFFECT. A level-0 one is not "active at nothing", it is not
        // active, and it must not be constructible or a renderer would print "Unbreaking".
        assertThrows(IllegalArgumentException.class, () -> new ActiveEnchant(A, 0));
        assertThrows(IllegalArgumentException.class, () -> new ActiveEnchant(A, -1));
    }

    @Test
    void addingACandidateToASlotThatDoesNotExistYetIsRefused() {
        // Exactly one past the end appends; further out would have to invent empty slots, and an
        // item silently growing slots is how a roster bug hides.
        EnchantState state = EnchantState.empty();
        assertThrows(IllegalArgumentException.class, () -> state.addCandidate(1, A));
        assertThrows(IllegalArgumentException.class, () -> state.addCandidate(-1, A));

        assertEquals(1, state.addCandidate(0, A).slots().size(), "the next one appends");
        assertEquals(2, state.addCandidate(0, A).addCandidate(1, B).slots().size());
    }

    @Test
    void aDuplicateIdInOneSlotIsRefused() {
        // Pointless as an offer, and it makes index-versus-id reasoning ambiguous. Across DIFFERENT
        // slots it is allowed -- that is the deferred stacking question, handled by effective().
        EnchantState state = EnchantState.empty().addCandidate(0, A);
        var ex = assertThrows(IllegalArgumentException.class, () -> state.addCandidate(0, A));
        assertTrue(ex.getMessage().contains(A));

        assertEquals(2, state.addCandidate(1, A).slots().size(), "but a second SLOT may offer it");
    }

    @Test
    void anOutOfRangeSlotOrCandidateIsRefusedByEveryTransition() {
        EnchantState state = twoCandidates().withLevel(0, 0, 1);

        assertThrows(IllegalArgumentException.class, () -> state.withLevel(1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> state.withLevel(0, 5, 1));
        assertThrows(IllegalArgumentException.class, () -> state.withActive(0, 5));
        assertThrows(IllegalArgumentException.class, () -> state.withoutActive(3));
        assertThrows(IllegalArgumentException.class, () -> state.withoutActive(-1));
    }

    @Test
    void stateIsImmutableSoATransitionCannotMutateTheOriginal() {
        // Copy-on-write is what makes "a swap did not scribble on the levels" checkable at all. If
        // a transition edited in place, every test above would pass while the caller's own
        // reference silently changed underneath it.
        EnchantState original = twoCandidates().withLevel(0, 0, 2).withActive(0, 0);
        EnchantState changed = original.withLevel(0, 0, 1);

        assertEquals(2, original.slots().get(0).candidates().get(0).level(),
                "the original is untouched by a transition applied to it");
        assertEquals(1, changed.slots().get(0).candidates().get(0).level());
    }

    @Test
    void aStateDoesNotKeepALiveHandleOnTheListItWasBuiltFrom() {
        // Records do NOT copy their constructor arguments. Without List.copyOf the caller's later
        // edit reaches inside an "immutable" value -- and the item's state would change without any
        // transition having been called.
        List<EnchantCandidate> candidates = new ArrayList<>();
        candidates.add(new EnchantCandidate(A, 2));
        EnchantSlot slot = new EnchantSlot(candidates, 0);

        List<EnchantSlot> slots = new ArrayList<>();
        slots.add(slot);
        EnchantState state = new EnchantState(slots);

        candidates.clear();
        slots.clear();

        assertEquals(1, state.slots().size(), "the state kept its own copy of the slot list");
        assertEquals(2, state.slots().get(0).candidates().get(0).level(),
                "and its own copy of the candidate list");
        // Mutation: drop either List.copyOf -> the clear() reaches inside -> reddens.
    }
}
