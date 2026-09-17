package io.github.butterflysmp.rpg.core.weapon;

/**
 * *** THE ROLL BAND'S TWO NUMBERS. BOTH ARE OWED, AND NEITHER IS INVENTED HERE. ***
 *
 * <p>Ben ruled that a drop's score is <b>rolled at mint, banded on the player's CURRENT AVERAGE</b>,
 * and that <b>rarity does not affect the band at all</b>. He has not given the SPREAD or the SKEW.
 * They are the same kind of number as the enchant rungs and the Boltor's {@code 19}: <b>his to rule,
 * and not derivable from the material.</b>
 *
 * <h2>WHY THE SHIPPED VALUES ARE ZERO, AND WHY THAT IS NOT A DEFAULT</h2>
 *
 * A plausible-looking spread -- {@code 25}, {@code 50}, a tenth of the average -- <b>would be
 * indistinguishable from a ruling.</b> The next reader finds a number in a constant, in a file with
 * this much commentary around it, and derives the next one from it. That is CLAUDE.md's descent rule:
 * <i>"a number derived from a placeholder becomes a precedent"</i>, and the Boltor's
 * {@code attack_damage} is this project's own instance -- {@code 19} was ruled OUTRIGHT precisely
 * because deriving it from a dev weapon would have outlived the dev weapon.
 *
 * <p><b>Zero cannot be mistaken for tuning.</b> A band of zero width is visibly degenerate: every
 * roll lands exactly on the centre, which no designer would choose and no reader will quote as a
 * precedent. It is the one value that reads as ABSENT rather than as CHOSEN.
 *
 * <p><b>And it ships a WORKING mechanism, not a stub.</b> With both at zero the whole ladder is live
 * and observable -- a drop still bands on the average, still clamps at {@link GearScore#SOFT_CAP},
 * and still floors at {@link GearScore#MIN}, which is what makes the 16-average new player's first
 * drop land at 100. {@code GearScore.roll} takes the two as PARAMETERS, so the arithmetic is reddened
 * across real spreads in {@code GearScoreTest} today and Ben's numbers are a one-line change here
 * with no code to write.
 *
 * <h2>*** THE SHAPE IS A DESIGN QUESTION, NOT A TUNING ONE, AND IT NEEDS SAYING ***</h2>
 *
 * <b>A band centred exactly on the average CONVERGES rather than climbs.</b> Progression then comes
 * entirely from the player KEEPING THE BETTER ROLL AND DISCARDING THE WORSE -- selection, not the
 * band. That works, it is how Destiny works, and <b>it is slow</b>.
 *
 * <p>If Ben wants a felt climb, the band skews upward: {@link #SKEW_OWED} is the number that does it,
 * and it is a separate decision from how WIDE the band is. Recorded here so the question is put
 * rather than answered by whoever happens to pick the spread.
 *
 * <p><b>A gate row cannot witness the spread while it is zero.</b> Stated in {@code GATE-gearscore.md}
 * rather than left for someone to discover: the row that watches a drop land somewhere inside a band
 * is unstageable until these two numbers exist, and it is marked OWED there for the same reason.
 */
public final class GearScoreBand {

    private GearScoreBand() {}

    /**
     * Half-width of the band, in score points. A roll is uniform over
     * {@code [average + skew - spread, average + skew + spread]}.
     *
     * <p><b>OWED -- BEN'S NUMBER.</b> Zero is not a choice about width; see the class javadoc. Do not
     * replace it with a guess, and do not derive anything from it.
     */
    public static final int SPREAD_OWED = 0;

    /**
     * How far the band's centre sits ABOVE the player's average. Zero centres it.
     *
     * <p><b>OWED -- BEN'S NUMBER, AND IT IS THE DESIGN HALF RATHER THAN THE TUNING HALF.</b> Zero
     * converges; positive climbs. See the class javadoc: this is the question, not the answer.
     */
    public static final int SKEW_OWED = 0;
}
