package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantSlot;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The one-button interaction model, in the two-second loop.
 *
 * <p>Two of these arms have NO shipped content that can exercise them, which is the whole argument
 * for testing the decision rather than clicking it on a server. Nothing ships with
 * {@code max_level: 1}, so the per-enchant cap is unreachable from a boot gate; and nothing ships
 * with a missing content file, so the unknown-enchant refusal is too. Both are exactly the arms a
 * reader would assume are fine.
 *
 * <p>The last test pins the ORDER of the two transitions an unlock performs, which is the single
 * place this feature can throw at runtime.
 */
class EnchantClickIntentTest {

    // The shipped files, as records. sharpness.yml / unbreaking.yml.
    private static final EnchantDefinition SHARPNESS = new EnchantDefinition(
            "sharpness", "Sharpness", 3, EnchantEffect.DAMAGE, WeaponClass.MELEE, List.of(5, 10, 15));
    /** Vanilla's Mending shape: a real, legal enchant that tops out at I. Nothing ships one yet. */
    private static final EnchantDefinition SINGLE_LEVEL = new EnchantDefinition(
            "mending", "Mending", 1, EnchantEffect.DURABILITY, null, List.of());

    @Test
    void aLockedCandidateUnlocks() {
        EnchantSlot slot = slotOf(0, EnchantSlot.NONE);

        assertEquals(EnchantClickIntent.UNLOCK, EnchantClickIntent.of(slot, 0, SHARPNESS));
        // Mutation: let a locked candidate fall through to ACTIVATE -> reddens here, and would
        // throw at runtime, because EnchantState.withActive refuses a level-0 candidate.
    }

    @Test
    void anUnlockedInactiveCandidateActivates() {
        // Level 2 and NOT active. Branching on "level == 1" rather than on active-ness would call
        // this a level-up and silently charge the player a level for a swap.
        EnchantSlot slot = slotOf(2, EnchantSlot.NONE);

        assertEquals(EnchantClickIntent.ACTIVATE, EnchantClickIntent.of(slot, 0, SHARPNESS));
        // Mutation: branch on level == 1 instead of activeIndex -> reddens.
    }

    @Test
    void theActiveCandidateBelowItsMaxLevelsUp() {
        assertEquals(EnchantClickIntent.LEVEL_UP, EnchantClickIntent.of(slotOf(1, 0), 0, SHARPNESS));
        assertEquals(EnchantClickIntent.LEVEL_UP, EnchantClickIntent.of(slotOf(2, 0), 0, SHARPNESS));
    }

    @Test
    void theActiveCandidateAtItsMaxIsANoOpNotALevelUp() {
        assertEquals(EnchantClickIntent.AT_MAX, EnchantClickIntent.of(slotOf(3, 0), 0, SHARPNESS));
        // Mutation: ">=" to ">" -> a fourth click asks EnchantState for level 4, which the
        // candidate's own constructor refuses -> reddens.
    }

    @Test
    void aPerEnchantMaxBelowTheModelsIsHonoured() {
        // THE test with no shipped content behind it. EnchantState.MAX_LEVEL is 3 and this enchant
        // stops at 1; the model cannot enforce that, because core has no idea which enchants exist.
        // RpgCommand re-checks it by hand for the same reason -- this is the GUI's copy of that rule.
        EnchantSlot slot = slotOf(1, 0);

        assertEquals(EnchantClickIntent.AT_MAX, EnchantClickIntent.of(slot, 0, SINGLE_LEVEL));
        // Mutation: compare against EnchantState.MAX_LEVEL instead of definition.maxLevel() ->
        // returns LEVEL_UP -> reddens. Nothing on a booted server would have caught this.
    }

    @Test
    void aCandidateAlreadyPastItsMaxDoesNotLevelFurther() {
        // Reachable by editing max_level DOWN in content after an item was already enchanted.
        // Levelling further would widen a gap the content author just narrowed.
        assertEquals(EnchantClickIntent.AT_MAX, EnchantClickIntent.of(slotOf(3, 0), 0, SINGLE_LEVEL));
        // Mutation: "==" instead of ">=" -> level 3 against a cap of 1 reads as below it -> reddens.
    }

    @Test
    void anIndexPastTheSlotsCandidatesIsEmpty() {
        // The table always paints three rows; a slot may have rolled one. The extra cells are
        // filler, and the click that lands on them must be a no-op rather than an exception
        // thrown from inside an inventory handler.
        EnchantSlot twoCandidates = new EnchantSlot(
                List.of(new io.github.butterflysmp.rpg.core.enchant.EnchantCandidate("sharpness", 1),
                        new io.github.butterflysmp.rpg.core.enchant.EnchantCandidate("power", 0)),
                EnchantSlot.NONE);

        assertEquals(EnchantClickIntent.EMPTY, EnchantClickIntent.of(twoCandidates, 2, SHARPNESS));
        assertEquals(EnchantClickIntent.EMPTY, EnchantClickIntent.of(EnchantSlot.empty(), 0, SHARPNESS));
        // Mutation: drop the bounds check -> IndexOutOfBoundsException inside a click handler.
    }

    @Test
    void anUnknownEnchantIsRefusedInEveryState() {
        // The registry fail-softs a malformed file and the item's blob still names the enchant, so
        // null is reachable. Refused whatever the candidate's state: we can neither describe what
        // it does nor bound how far it levels.
        assertEquals(EnchantClickIntent.UNKNOWN_ENCHANT, EnchantClickIntent.of(slotOf(1, 0), 0, null));
        assertEquals(EnchantClickIntent.UNKNOWN_ENCHANT, EnchantClickIntent.of(slotOf(0, EnchantSlot.NONE), 0, null));
        // Mutation: default the cap to EnchantState.MAX_LEVEL when the definition is null -> an
        // enchant nobody can name gets levelled to 3 -> reddens.

        // But an index past the end is EMPTY even with no definition: there is no candidate there
        // to be unknown ABOUT, and reporting "unknown enchant" for a filler pane would be a lie.
        assertEquals(EnchantClickIntent.EMPTY, EnchantClickIntent.of(EnchantSlot.empty(), 0, null));
        // Mutation: move the null check above the bounds check -> reddens.
    }

    @Test
    void unlockAndActivateAreOneClickAndOnlyInThatOrder() {
        // Not a test of the enum but of the transition pair it names, and it is the one that would
        // have caught a runtime throw: withActive REFUSES a locked candidate, so the level must
        // land first and the activation must run on the RESULT, not on the state that was read.
        EnchantState before = EnchantState.empty().addCandidate(0, "sharpness");
        assertEquals(EnchantClickIntent.UNLOCK,
                EnchantClickIntent.of(before.slots().get(0), 0, SHARPNESS));

        EnchantState after = before.withLevel(0, 0, 1).withActive(0, 0);

        assertEquals(1, after.slots().get(0).candidates().get(0).level());
        assertEquals(0, after.slots().get(0).activeIndex(), "unlocking also ACTIVATES");

        // The reversed order does not merely give a different answer -- it throws.
        assertThrows(IllegalArgumentException.class, () -> before.withActive(0, 0),
                "withActive on a locked candidate must refuse, which is why order matters");
    }

    @Test
    void swappingAwayLeavesThePreviousActiveUnlockedAtItsLevel() {
        // The property the whole candidate model exists for, asserted on the state rather than on
        // the enum: a level rides the CANDIDATE, so a swap is free and reversible.
        EnchantState state = EnchantState.empty()
                .addCandidate(0, "sharpness").addCandidate(0, "unbreaking")
                .withLevel(0, 0, 3).withActive(0, 0);

        EnchantSlot slot = state.slots().get(0);
        assertEquals(EnchantClickIntent.UNLOCK, EnchantClickIntent.of(slot, 1, SHARPNESS));

        EnchantState swapped = state.withLevel(0, 1, 1).withActive(0, 1);

        assertEquals(1, swapped.slots().get(0).activeIndex());
        assertEquals(3, swapped.slots().get(0).candidates().get(0).level(),
                "the previous active keeps its level -- swapping back must not cost a re-unlock");
        assertEquals(EnchantClickIntent.ACTIVATE,
                EnchantClickIntent.of(swapped.slots().get(0), 0, SHARPNESS),
                "and clicking it again is a swap back, not another unlock");
    }

    /** One candidate at {@code level}, with {@code activeIndex} as given. */
    private static EnchantSlot slotOf(int level, int activeIndex) {
        return new EnchantSlot(
                List.of(new io.github.butterflysmp.rpg.core.enchant.EnchantCandidate("sharpness", level)),
                activeIndex);
    }
}
