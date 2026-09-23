package io.github.butterflysmp.rpg.core.anvil;

import io.github.butterflysmp.rpg.core.weapon.Rarity;

/**
 * What one of the anvil's two input slots holds, as far as a transfer is concerned.
 *
 * <h2>*** THE THREE STATES ARE NAMED AND THE ILLEGAL ONES ARE UNREPRESENTABLE ***</h2>
 *
 * Ben's ruling, and it replaced a seam of five plain values
 * ({@code targetKey, targetScore, donorKey, donorScore, targetRarity}) that <b>could not tell an
 * EMPTY slot from an item that carries no score</b> -- both arrive as an absent key. The fix is not
 * a sixth value or a nullability convention; it is three named states:
 *
 * <pre>
 *   Empty         the slot holds nothing
 *   Unscoreable   it holds something that carries no gear score -- a tool, a vanilla item, or
 *                 an instance whose own definition declares it unscored
 *   Scored        it holds our gear, with a key and a score
 * </pre>
 *
 * <ul>
 *   <li><b>{@link Scored} cannot exist without a key.</b> The record demands one, and
 *       {@link TransferKey#of} throws rather than producing an absent one, so <b>a null key exists
 *       nowhere in this system</b> and nothing can be minted by hand to mean "unscoreable".
 *   <li><b>{@link Unscoreable} carries NO score</b>, because an item with no key has no meaningful
 *       one. A seam that carried a number here would be carrying a figure nobody may read.
 *   <li><b>The whole refusal set stays in core.</b> Letting {@code paper} short-circuit the empty
 *       case would put one refusal in one module and three in another, which is the two-homes
 *       failure {@code CLAUDE.md}'s opening section records happening twice already.
 * </ul>
 *
 * <h2>PAPER'S JOB IS UNCHANGED AND IS STILL GATHERING</h2>
 *
 * The adapter maps a slot to one of these three and hands it over. <b>No {@code ItemStack}
 * crosses.</b> "The slot has nothing in it" is an inventory fact -- paper's to observe and core's
 * to name -- and this type is what lets both do their own half.
 *
 * @see AnvilTransfer#evaluate
 */
public sealed interface Side {

    /**
     * Nothing in the slot.
     *
     * <p>Not an error state: it is the face the screen OPENS in, and its refusal is worded as an
     * instruction rather than a complaint. See {@code AnvilVerdict.SlotEmpty}.
     */
    record Empty() implements Side {}

    /**
     * An item that carries no gear score.
     *
     * <p>Three unrelated causes arrive as this one state, and that is correct rather than lossy --
     * <b>the player's next action is identical for all three</b>: a tool (the kind-level rule), an
     * item that is none of ours (no id tag), and an instance whose definition declares it unscored
     * ({@code volley_stone}). {@code GearScore.carriesScore} is the one door that composes the first
     * and third; the second is the lookup returning nothing.
     */
    record Unscoreable() implements Side {}

    /**
     * Our gear: a key, the score it carries now, and the rarity that prices a transfer INTO it.
     *
     * <h2>THE RARITY IS A FIELD HERE RATHER THAN A PARAMETER OF {@link AnvilTransfer#evaluate}</h2>
     *
     * <p>The brief named {@code targetRarity} as a top-level parameter. It sits here instead, for
     * the same reason this interface exists at all: <b>an {@link Empty} target has no rarity to
     * pass</b>, so a top-level parameter would be null on exactly the path that has no item --
     * reintroducing one level down the null this type removes.
     *
     * <p><b>The DONOR's rarity is carried and deliberately never read.</b> Ben's ruling is that the
     * cost keys on the rarity of the item BEING UPGRADED. Which of the two is read is its own axis,
     * it is invisible whenever both items share a rarity -- most pairs -- and it is therefore a
     * TESTABLE property rather than a comment: {@code AnvilTransferTest} stages a {@code COMMON}
     * target against an {@code EXOTIC} donor, and {@code MUT13A-WHOSERARITY} is the splice that
     * proves the row can see the difference.
     *
     * @param key    what this item may trade with
     * @param score  the score it carries now. An unstamped item reads {@code GearScore.ABSENT},
     *               which is a correct reading rather than a missing one -- there is no legacy arm
     *               and no migration anywhere in this feature.
     * @param rarity the tier that prices a transfer INTO this item
     */
    record Scored(TransferKey key, int score, Rarity rarity) implements Side {

        public Scored {
            if (key == null) {
                throw new IllegalArgumentException(
                        "a Scored side without a key is the state this type exists to make"
                                + " unrepresentable; an item with no key is Unscoreable");
            }
            if (rarity == null) {
                throw new IllegalArgumentException(
                        "a Scored side without a rarity cannot be priced; every GearDefinition"
                                + " carries one");
            }
        }
    }

    /** Allocation-free {@link Empty}, the way {@code RefreshVerdict.UNTAGGED} is. */
    Side EMPTY = new Empty();

    /** Allocation-free {@link Unscoreable}, for the same reason. */
    Side UNSCOREABLE = new Unscoreable();
}
