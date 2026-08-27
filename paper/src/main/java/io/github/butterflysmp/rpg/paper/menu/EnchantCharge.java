package io.github.butterflysmp.rpg.paper.menu;

/**
 * Which clicks cost something, and which enchant level they are buying.
 *
 * <p>Separate from {@link EnchantClickIntent} on purpose, and the split is the same one that class
 * already names in its own javadoc: what a click MEANS and what it COSTS are different questions.
 * This answers the second, and only after the first has been answered.
 *
 * <p>Bukkit-free, so it reaches the two-second loop. {@code EnchantMenu} cannot be constructed in a
 * unit test -- there is no {@code RegistryAccess} and no MockBukkit -- so a rule left inside the
 * click handler would have no test at all and would be owed entirely to a boot gate. The rule this
 * pass most needs held is precisely the one a boot gate is worst at: that swapping between two
 * unlocked candidates is FREE, no matter how many times you do it, which is only observable as an
 * XP counter that never moves.
 */
final class EnchantCharge {

    private EnchantCharge() {}

    /** What a click that buys nothing costs. Not a level -- a sentinel that means "no charge". */
    static final int FREE = 0;

    /**
     * The enchant level this click BUYS, or {@link #FREE} when it buys nothing.
     *
     * <p><b>No default arm, deliberately</b> -- the discipline {@code EnchantEffectLine.bare} uses.
     * A seventh {@code EnchantClickIntent} is a compile error here until someone prices it, rather
     * than falling through to free and shipping a hole in the economy that nothing would report.
     *
     * <p>Three of the four free arms are free because they change nothing: {@code AT_MAX} is a no-op
     * with feedback, {@code EMPTY} is a filler pane, {@code UNKNOWN_ENCHANT} is a refusal. The
     * fourth, {@code ACTIVATE}, is free because it is MEANT to be: a level rides the candidate
     * rather than the choice, so swapping back and forth costs nothing and a player can experiment.
     * Charge it and the whole point of the candidate model is gone.
     *
     * @param currentLevel the clicked candidate's level now. Only read for {@code LEVEL_UP}, which
     *                     buys the NEXT one -- pricing the level already held would charge II's
     *                     price to reach III.
     */
    static int targetLevel(EnchantClickIntent intent, int currentLevel) {
        return switch (intent) {
            case UNLOCK -> 1;
            case LEVEL_UP -> currentLevel + 1;
            case ACTIVATE, AT_MAX, EMPTY, UNKNOWN_ENCHANT -> FREE;
        };
    }
}
