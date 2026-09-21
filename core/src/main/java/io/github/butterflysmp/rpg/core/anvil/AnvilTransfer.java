package io.github.butterflysmp.rpg.core.anvil;

/**
 * The anvil's whole decision: two slots in, one {@link AnvilVerdict} out.
 *
 * <p>Pure, and in {@code core} for the reason every screen's decidable half is: {@code AnvilMenu}
 * needs a running server to construct, so a rule written there has no test that can reach it at the
 * two-second loop. {@code NexusStationGate} and {@code GrindstoneButton} make the same split, and
 * the slice before last shipped two zero-kill mutations because logic sat on the wrong side of it.
 *
 * <h2>WHAT A TRANSFER IS</h2>
 *
 * A player with a good item on a poor gear score sacrifices a higher-scored item of the SAME TYPE
 * to take its score. <b>The score MOVES; it is not blended and nothing is added.</b> The target ends
 * up carrying the donor's number exactly.
 *
 * <h2>*** 13a DECIDES AND DISPLAYS. IT PERFORMS NOTHING. ***</h2>
 *
 * Nothing in this class mutates anything -- it has no state and no side effects, and
 * {@link AnvilVerdict.Ready} is a description rather than a receipt. The spend, the write and the
 * consumption of the donor are 13b, deliberately separated so the one irreversible operation in the
 * feature gets its own undivided gate.
 *
 * <h2>THERE IS NO LEGACY ARM, AND THAT IS A RULING RATHER THAN AN OVERSIGHT</h2>
 *
 * Ben's: zero deployed versions exist, so there is no population of pre-system items to protect.
 * <b>An unstamped item is just an item reading {@code GearScore.ABSENT}</b> -- which is 100, a
 * correct reading rather than a missing one -- so it may be a target like any other and the donor
 * must simply exceed it. No migration, and no special case for "predates the system".
 */
public final class AnvilTransfer {

    private AnvilTransfer() {}

    /**
     * Decide what the anvil does with these two slots.
     *
     * <h2>THE ORDER OF THESE CHECKS IS THE RULING, NOT AN IMPLEMENTATION DETAIL</h2>
     *
     * <pre>
     *   1  empty          before everything: it is the screen's resting face
     *   2  unscoreable    BEFORE any key is derived -- see below
     *   3  key mismatch   only once both keys certainly exist
     *   4  donor too low  only once the pair is legal
     * </pre>
     *
     * <p><b>Step 2 before step 3 is what makes {@link TransferKey#of}'s throwing arms unreachable in
     * production</b>, and the ordering is enforced by the TYPES rather than by these branches being
     * in the right order: a {@link Side.Unscoreable} carries no key to compare, so there is no way
     * to write step 3 first even by mistake. That is the whole benefit of Ben's sealed-{@code Side}
     * ruling over a nullable key -- the ordering became a compile-time fact instead of a convention
     * a later edit could quietly reverse.
     *
     * <p>It is still asserted rather than assumed: {@code AnvilTransferTest} stages an unscoreable
     * pair and requires {@link AnvilVerdict.NotScoreable} <b>specifically</b>, not merely "some
     * refusal". Both arms refuse; only one of them is right.
     *
     * @param target the item being upgraded -- <b>its rarity prices the transfer</b>
     * @param donor  the item being sacrificed. Its rarity is carried and never read; see
     *               {@link Side.Scored}.
     */
    public static AnvilVerdict evaluate(Side target, Side donor) {
        if (target == null || donor == null) {
            throw new IllegalArgumentException(
                    "an absent Side is not the same as an empty slot: an empty slot is Side.EMPTY,"
                            + " and a null here means the adapter failed to gather one");
        }

        if (target instanceof Side.Empty || donor instanceof Side.Empty) {
            return new AnvilVerdict.SlotEmpty();
        }
        if (!(target instanceof Side.Scored scoredTarget)
                || !(donor instanceof Side.Scored scoredDonor)) {
            return new AnvilVerdict.NotScoreable();
        }
        if (scoredTarget.key() != scoredDonor.key()) {
            return new AnvilVerdict.KeyMismatch(scoredTarget.key(), scoredDonor.key());
        }
        // STRICTLY higher. Ben's ruling: equal is REJECTED, not a no-op -- accepting it would
        // consume a second item in 13b and change nothing.
        if (scoredDonor.score() <= scoredTarget.score()) {
            return new AnvilVerdict.DonorNotHigher(scoredTarget.score(), scoredDonor.score());
        }

        // THE TARGET'S RARITY, and which of the two this reads is its own axis: the two items share
        // a rarity in most pairs, so reading the donor's would be invisible almost always.
        // MUT13A-WHOSERARITY is the splice, and the cross-rarity row is what sees it.
        return new AnvilVerdict.Ready(scoredDonor.score(), AnvilCost.xpPoints(scoredTarget.rarity()));
    }
}
