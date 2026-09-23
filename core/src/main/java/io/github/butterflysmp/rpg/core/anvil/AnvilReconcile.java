package io.github.butterflysmp.rpg.core.anvil;

import java.util.Optional;

/**
 * What changed between the decision the player was LOOKING AT and the decision that is true NOW.
 *
 * <h2>*** THE RENDERED PREVIEW IS A READOUT. IT IS NEVER THE DECISION. ***</h2>
 *
 * The confirm click re-derives everything from the live slots and the live wallet and compares. This
 * is the project's standing doctrine, not a new rule: {@code CraftingMenu.suggestions} is marked
 * <i>"ADVISORY, NEVER AUTHORITATIVE … Every commit re-verifies against the pin and the live
 * inventory"</i>, and {@code craftFromSuggestion} is headed <i>"THE COUNT IS ADVISORY; THIS IS
 * AUTHORITATIVE"</i>.
 *
 * <h2>WHAT IT IS ACTUALLY FOR, AND IT IS NOT THE INPUT SLOTS</h2>
 *
 * A three-second arm is a three-second window to swap the inputs — <b>and a swap re-arms, so the
 * face is no longer actionable and the click is already a silent no-op.</b> The re-arm is what makes
 * an input swap safe; this is not.
 *
 * <p><b>The reachable case is the WALLET.</b> XP can move while the menu is open — a kill, a
 * command, another plugin — and none of that touches an input slot or re-arms anything. The wallet
 * is the only input to the decision that is not a slot, and <b>re-evaluation is its only guard.</b>
 *
 * <p>It is kept for the input case too, and not out of caution: on an irreversible action two
 * independent guards is the right number, and the second earns its place <b>precisely by not sharing
 * a premise with the first</b>. This one does not depend on the re-arm being correct.
 *
 * <h2>PRECEDENCE, AND THE ORDER IS THE RULING</h2>
 *
 * <pre>
 *   1  the pair itself changed     a different verdict ARM -- the items are not what they were
 *   2  the result changed          same arm, different score -- what the player RECEIVES
 *   3  the price changed           same score, different cost -- what they PAY
 *   4  affordability changed       same price, different wallet
 * </pre>
 *
 * <b>Most fundamental first.</b> A player whose donor was swapped needs to be told that, not told
 * the price moved as a consequence of it.
 */
public sealed interface AnvilReconcile {

    /**
     * The sentence to show, or empty when nothing changed.
     *
     * <p><b>An {@code Optional} rather than a blank string on {@link Same}.</b> A method that
     * returns {@code ""} for "say nothing" is one caller away from printing an empty chat line, and
     * the emptiness would be carried in a value rather than in the type.
     *
     * <p>Every arm implements it, so a sixth arm cannot be added without answering this — the same
     * policing an exhaustive switch gives, through the interface instead.
     */
    Optional<String> sentence();

    /** Nothing moved: the decision at the click is the decision that was rendered. */
    record Same() implements AnvilReconcile {
        @Override
        public Optional<String> sentence() {
            return Optional.empty();
        }
    }

    /** A different verdict arm entirely -- the items are not the pair that was priced. */
    record PairChanged(AnvilVerdict was, AnvilVerdict now) implements AnvilReconcile {
        @Override
        public Optional<String> sentence() {
            return Optional.of("These items changed. Check the result and try again.");
        }
    }

    /** Still a legal transfer, but the score it would land on has moved. */
    record ScoreChanged(int was, int now) implements AnvilReconcile {
        @Override
        public Optional<String> sentence() {
            return Optional.of("The result changed from " + was + " to " + now
                    + ". Check it and try again.");
        }
    }

    /**
     * Same result, different price.
     *
     * <p><b>This is the arm a text comparison cannot see</b>, and the reason it is separate from
     * {@link ScoreChanged}: swapping the TARGET for a same-score item of a different rarity moves
     * the price and nothing else, and a confirm button that prints only the price renders both
     * decisions identically.
     */
    record CostChanged(int was, int now) implements AnvilReconcile {
        @Override
        public Optional<String> sentence() {
            return Optional.of("The price changed from " + was + " to " + now
                    + " XP. Check it and try again.");
        }
    }

    /**
     * The pair and the price are unchanged; the wallet moved.
     *
     * <p><b>The reachable direction is INTO {@code CannotAfford}.</b> The other way round is a
     * transition into an actionable decision, which re-arms the deadline, so the face is no longer
     * actionable and the click never reaches here. It is still classified rather than collapsed,
     * because this is a pure function and a caller may ask it anything.
     */
    record AffordabilityChanged(Affordability was, Affordability now) implements AnvilReconcile {
        @Override
        public Optional<String> sentence() {
            return Optional.of(now instanceof Affordability.CannotAfford shortfall
                    ? shortfall.sentence()
                    : "You can afford this now. Check the result and try again.");
        }
    }

    /**
     * Compare the decision that was rendered against the decision that is true now.
     *
     * <p><b>Equality first, and it is the whole of the {@link Same} case.</b> {@link AnvilDecision}
     * is a record over decision-bearing fields only, so structural equality is exactly the question
     * being asked and no field can differ for an incidental reason.
     */
    static AnvilReconcile of(AnvilDecision displayed, AnvilDecision current) {
        if (displayed == null || current == null) {
            throw new IllegalArgumentException(
                    "reconciling needs both decisions; a null here means paper never stored the"
                            + " rendered one, and the comparison would silently always agree");
        }
        if (displayed.equals(current)) return new Same();

        // 1. A DIFFERENT ARM. Anything that is not two Ready verdicts is a change of the pair
        //    itself, and there is no score or price to compare across it.
        if (!(displayed.verdict() instanceof AnvilVerdict.Ready wasReady)
                || !(current.verdict() instanceof AnvilVerdict.Ready nowReady)) {
            return new PairChanged(displayed.verdict(), current.verdict());
        }
        // 2. WHAT THE PLAYER RECEIVES, before 3, what they pay.
        if (wasReady.newScore() != nowReady.newScore()) {
            return new ScoreChanged(wasReady.newScore(), nowReady.newScore());
        }
        if (wasReady.xpPoints() != nowReady.xpPoints()) {
            return new CostChanged(wasReady.xpPoints(), nowReady.xpPoints());
        }
        // 4. THE ONLY FIELD LEFT. The decisions are unequal and the verdicts are identical, so the
        //    affordability is what differs -- there is no fifth field for this to fall through to.
        return new AffordabilityChanged(displayed.affordability(), current.affordability());
    }
}
