package io.github.butterflysmp.rpg.core.xp;

/**
 * Vanilla Minecraft's experience curve, both directions, in integers.
 *
 * <p>Minecraft shows a player two numbers -- a LEVEL and a part-full bar -- but the thing that is
 * actually spendable is neither of them. It is the POINT total those two encode, and the encoding is
 * non-linear: level 40 is 2920 points, level 20 is 550, so half the levels is not half the money.
 * Anything that charges a player has to work in points or it charges a different price at every
 * level while looking like it charges one.
 *
 * <p>This is vanilla's curve re-derived, not Bukkit's. {@code Player.getTotalExperience()} is
 * deliberately NOT the input anywhere: it is a running total that does not track spends correctly,
 * and it drifts from what the client is displaying. The wallet is always computed from
 * {@code (getLevel(), getExp())}, which is exactly what the player is looking at.
 *
 * <p><b>Integer arithmetic throughout, and the reason is narrower than it looks.</b> The wiki forms
 * are {@code 2.5L^2 - 40.5L + 360} and {@code 4.5L^2 - 162.5L + 2220}; both are rewritten here over a
 * common denominator of 2, which is exact because for odd {@code L} the two halves cancel.
 *
 * <p><b>A double implementation of that same algebra is ALSO exact, and this was measured rather
 * than assumed.</b> Substituting {@code (long)(2.5 * l * l - 40.5 * l + 360)} was run as a mutation
 * and every test stayed green: {@code 2.5} and {@code 40.5} are exact binary fractions, and the
 * magnitudes here stay far inside a double's mantissa, so nothing rounds. The prediction written
 * here first -- "right at even levels and one off at odd ones" -- was WRONG, and is recorded as
 * wrong rather than quietly deleted. The integer form is kept because a class with no doubles in it
 * needs no such analysis from the next reader, not because the double form computes a wrong answer.
 * Where a double genuinely does bite is one layer up, in {@code EnchantCost}'s discount.
 *
 * <p>Lives in its own package rather than in {@code core.enchant} because it is a fact about
 * Minecraft, not a rule about enchanting. The enchant table is merely its first caller.
 */
public final class XpCurve {

    private XpCurve() {}

    /**
     * The highest level this curve will evaluate, chosen so the point total still fits in an int:
     * {@code totalForLevel(21863)} is 2,147,407,943 and 21864 overflows.
     *
     * <p>A guard at the reachable surface, not a rule about the game. {@code Player.getLevel()} is an
     * int and {@code /xp set @s 2000000000 levels} is a command a server operator can type, so a
     * level far past anything playable can reach this code. Clamping keeps every function here TOTAL
     * -- an absurd level reads as an enormous-but-finite wallet rather than as a negative one, and a
     * negative wallet is the shape that makes everything look free.
     */
    public static final int MAX_LEVEL = 21863;

    /**
     * The total points banked by a player who has reached {@code level} from zero, spending nothing.
     *
     * <p>Level 16 is 352, level 25 is 910, level 40 is 2920 -- the three anchors the enchant prices
     * are derived from, and the three the test pins against vanilla.
     */
    public static int totalForLevel(int level) {
        long l = clampLevel(level);
        long total = l <= 16
                ? l * l + 6 * l
                : l <= 31
                        ? (5 * l * l - 81 * l + 720) / 2      // == 2.5L^2 - 40.5L + 360
                        : (9 * l * l - 325 * l + 4440) / 2;   // == 4.5L^2 - 162.5L + 2220
        return (int) total;
    }

    /**
     * The size of {@code level}'s own bar: points from {@code level} to {@code level + 1}.
     *
     * <p><b>The bands are NOT the bands above, and that is the easiest thing here to get wrong.</b>
     * The cumulative total switches formula at 16/17 and 31/32; the bar size switches at 15/16 and
     * 30/31. Line the two up "consistently" and every level in between is off. What pins it is not
     * this comment but the consistency property in the test: for every level,
     * {@code totalForLevel(l + 1) - totalForLevel(l)} must equal this.
     *
     * <p><b>THESE boundaries are load-bearing and the cumulative ones are not</b> -- measured, by
     * moving all four. Shift a bar band by one and five tests redden. Shift a CUMULATIVE band by one
     * and NOTHING reddens, in either direction: vanilla's three parabolas intersect at consecutive
     * integer pairs (15 and 16, 30 and 31), so a branch anywhere inside {14,15,16} or {29,30,31}
     * computes the identical function. Only a shift of two or more is observable (13, 17, 28 and 32
     * each redden). So an off-by-one on lines 50 and 52 is not a bug that hides -- it is not a bug.
     */
    public static int pointsToNextLevel(int level) {
        int l = (int) clampLevel(level);
        if (l <= 15) return 2 * l + 7;
        if (l <= 30) return 5 * l - 38;
        return 9 * l - 158;
    }

    /**
     * A player's whole wallet, from the two numbers on their screen.
     *
     * <p><b>The bar fraction is ROUNDED, not truncated, and that is what makes a purchase exact.</b>
     * Minecraft stores the bar as a {@code float}: writing a progress back and reading it again
     * returns a value a hair below the one intended, so {@code (int)} truncation loses a point every
     * time -- {@code (741f / 742f) * 742f} is 740.99997. Rounding makes this the exact inverse of
     * {@link #levelFor} and {@link #progressFor}, so points cannot leak out of a wallet that is only
     * being read and written. The cost is that this may read one point above vanilla's own
     * truncating computation, which fails towards the player.
     *
     * @param progress the bar fraction, {@code Player.getExp()}. Clamped: a corrupt value must not
     *                 inflate a wallet.
     */
    public static int totalPoints(int level, double progress) {
        long l = clampLevel(level);
        double p = Math.max(0.0, Math.min(1.0, progress));
        long total = totalForLevel((int) l) + Math.round(p * pointsToNextLevel((int) l));
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    /**
     * The level a point total lands in: the largest {@code l} with {@code totalForLevel(l) <= total}.
     *
     * <p>Linear from zero rather than a binary search. It is bounded by {@link #MAX_LEVEL}, it runs
     * once per purchase, and its termination is readable without an argument about which side of the
     * midpoint an equal total falls on -- which is the boundary this whole class exists to get right.
     */
    public static int levelFor(int totalPoints) {
        if (totalPoints <= 0) return 0;
        int level = 0;
        while (level < MAX_LEVEL && totalForLevel(level + 1) <= totalPoints) level++;
        return level;
    }

    /**
     * How far into {@link #levelFor}'s bar that same total sits, as {@code Player.setExp} wants it.
     *
     * <p>Clamped to {@code [0, 1]} because {@code setExp} THROWS outside it, and a throw here would
     * be a crash on a purchase. It cannot fire for any reachable total -- it would need a bar of over
     * ten million points -- and it is a guard rather than an argument for that reason.
     */
    public static float progressFor(int totalPoints) {
        if (totalPoints <= 0) return 0.0f;
        int level = levelFor(totalPoints);
        int into = totalPoints - totalForLevel(level);
        float progress = (float) (into / (double) pointsToNextLevel(level));
        return Math.max(0.0f, Math.min(1.0f, progress));
    }

    private static long clampLevel(int level) {
        return Math.max(0, Math.min(MAX_LEVEL, level));
    }
}
