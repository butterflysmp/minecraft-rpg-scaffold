package io.github.butterflysmp.rpg.core.vault;

import io.github.butterflysmp.rpg.core.weapon.PageMath;

/**
 * Which vault pages a player's level has opened.
 *
 * <p>Pure -- no Bukkit, no profile, no inventory -- so every threshold is unit-testable at the
 * 2-second loop. {@code NexusStationGate} makes the same split for the same reason, and this class
 * is deliberately its neighbour in shape: thresholds, an {@code unlocked} predicate, and the strings
 * a locked cell renders.
 *
 * <h2>*** THE ENTITLEMENT, NOT THE SHAPE. {@link VaultShape} IS THE OTHER HALF. ***</h2>
 *
 * {@code VaultShape} says every vault has seven pages <b>from the moment it exists</b>; this says
 * which of them a given player may reach. That separation is {@code VaultShape}'s own ruling, and
 * it is why a level retune can move these numbers without a storage migration: no page index on
 * disk depends on anything here.
 *
 * <h2>THE THRESHOLDS ARE BEN'S, NOT DERIVED FROM THE CURVE</h2>
 *
 * {@code 20 / 25 / 30 / 35 / 40 / 45 / 50}. A flat five levels per page, which <b>looks</b> like a
 * formula and is not one -- the same trap {@code NexusStationGate}'s {@code 3 / 10 / 13} records
 * from the other direction. <b>Do not re-derive them from the XP totals and do not "regularise"
 * them</b>; they are a content decision and the next one may not be five apart.
 *
 * <p>Page 4's threshold is <b>35</b>. It is called out because the brief that carried these numbers
 * <b>omitted that row entirely</b> -- six pairs printed for seven pages -- and the six it did print
 * were all correct, so nothing about the table looked wrong.
 */
public final class VaultPageGate {

    private VaultPageGate() {}

    /**
     * The level that opens each page, indexed by 0-based page.
     *
     * <p><b>A private array rather than seven constants</b>, because the one invariant worth
     * enforcing is that there are exactly as many thresholds as there are pages -- see the static
     * block below. Seven named constants cannot be counted by anything.
     */
    private static final int[] UNLOCK_LEVELS = {20, 25, 30, 35, 40, 45, 50};

    /**
     * *** A THRESHOLD PER PAGE, ASSERTED AT CLASS LOAD RATHER THAN NOTICED. ***
     *
     * <p>{@link VaultShape#PAGE_COUNT} and this array are two numbers that must agree, and nothing
     * makes them agree. Raise {@code PAGE_COUNT} to eight and the eighth page has <b>no threshold
     * at all</b>: {@link #unlockLevel} would throw deep inside a render, on a page that exists in
     * storage and holds the player's items.
     *
     * <p><b>Class-load rather than a test, because a test can be deleted and this cannot.</b> It is
     * the same instinct {@code VaultShape} states for its own button-count assertion, one layer up:
     * an unreachable page is indistinguishable from an empty one, so finding nothing must be a
     * defect rather than a quiet no-op.
     */
    static {
        if (UNLOCK_LEVELS.length != VaultShape.PAGE_COUNT) {
            throw new IllegalStateException(
                    "VaultPageGate has " + UNLOCK_LEVELS.length + " thresholds for "
                            + VaultShape.PAGE_COUNT + " pages; every page needs exactly one");
        }
    }

    /**
     * The level that opens this page.
     *
     * @param page 0-based, as every page index inside the plugin is. The <b>player</b> sees
     *             {@code page + 1}, converted at the edge -- the command does it in
     *             {@code RpgCommand}'s {@code vaultCell}, and the screen does it on the button.
     */
    public static int unlockLevel(int page) {
        VaultShape.requirePage(page);
        return UNLOCK_LEVELS[page];
    }

    /** Has this level opened this page? <b>Inclusive</b>: level 20 opens the level-20 page. */
    public static boolean unlocked(int page, int level) {
        return level >= unlockLevel(page);
    }

    /**
     * How many pages this level has opened. {@code 0} for a new player.
     *
     * <p><b>Counts rather than returning the highest index</b>, so the empty case needs no sentinel:
     * a level-1 player has zero pages, and there is no "page -1" for a caller to mishandle. The
     * thresholds are ascending, so this is also the index of the first LOCKED page.
     */
    public static int unlockedPageCount(int level) {
        int count = 0;
        for (int page = 0; page < VaultShape.PAGE_COUNT; page++) {
            if (unlocked(page, level)) count++;
        }
        return count;
    }

    /** Has this level opened anything at all? The vault is a display case until it has. */
    public static boolean anyUnlocked(int level) {
        return unlockedPageCount(level) > 0;
    }

    /**
     * *** THERE IS NO {@code openingPage(level)} METHOD, AND THE ABSENCE IS THE DECISION. ***
     *
     * <p>A first draft had one, documented as <i>"the first unlocked page, or page 0 when nothing is
     * unlocked"</i>. <b>The thresholds ascend, so the first unlocked page is ALWAYS page 0</b> -- the
     * method ignored its own argument and returned a constant. A parameter nothing reads is a claim
     * that the answer depends on it, and the first person to add a non-ascending threshold would
     * have trusted that claim.
     *
     * <p>The screen opens on page 0 for everyone, including a player with nothing unlocked: the
     * screen still opens -- that is the hijack ruling -- and page 1 locked, naming the level it
     * wants, is the only place that sentence can be read.
     */

    /**
     * What a locked page button says when it is clicked. <b>SENT, not optional</b>, and for
     * {@code NexusStationGate.refusal}'s reason: the button dims its NAME, and a dimmed name lives
     * in the hover tooltip, so an un-hovered locked page is pixel-identical to an open one.
     *
     * <p>Both facts, in the order {@code NexusStationGate} established: the rule, then where the
     * player stands.
     */
    public static String refusal(int page, int level) {
        // PageMath.displayPage, NOT `page + 1`. That class calls itself "the only zero-to-one
        // conversion in the arc", and an inline `+ 1` here would make it the second -- two sources
        // of truth for one convention, which is the defect this project keeps recording. It lives
        // in core.weapon because the recipe browser needed it first; the home is odd and the
        // function is not.
        return "Vault page " + PageMath.displayPage(page) + " unlocks at level " + unlockLevel(page)
                + ". You are level " + level + ".";
    }
}
