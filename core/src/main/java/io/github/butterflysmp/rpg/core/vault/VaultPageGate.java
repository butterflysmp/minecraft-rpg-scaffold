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
 * <h2>*** PAGE 1 IS FREE. RULED 2026-09-18, AND IT REPLACED A THRESHOLD OF 20. ***</h2>
 *
 * <b>Page 1 has no level requirement at all</b>: a level-1 player reaches real, usable storage
 * through the ender chest block. <b>What level 20 now buys is the HUB SHORTCUT</b> -- the station
 * cell at slot 29 -- and nothing else. See {@link #HUB_SHORTCUT_LEVEL}.
 *
 * <p><b>All seven of Ben's original numbers keep a job.</b> 20 buys the convenience route; the other
 * six buy pages. Nothing was deleted from the ladder, and the count did not change -- which is why
 * the array below still has {@link VaultShape#PAGE_COUNT} entries, with page 1's set to
 * {@link #FREE}. A six-entry array plus a special case would break the equality
 * {@code PAGE_COUNT == thresholds.length} that {@code NexusVaultLayout}'s seven-buttons assertion
 * leans on.
 *
 * <h2>THE THRESHOLDS ARE BEN'S, NOT DERIVED FROM THE CURVE</h2>
 *
 * {@code free / 25 / 30 / 35 / 40 / 45 / 50}. Five levels per page from page 2, which <b>looks</b>
 * like a formula and is not one -- the same trap {@code NexusStationGate}'s {@code 3 / 10 / 13}
 * records from the other direction. <b>Do not re-derive them from the XP totals and do not
 * "regularise" them</b>; they are a content decision and the next one may not be five apart.
 *
 * <p>Page 4's threshold is <b>35</b>. It is called out because the brief that carried these numbers
 * <b>omitted that row entirely</b> -- six pairs printed for seven pages -- and the six it did print
 * were all correct, so nothing about the table looked wrong. <b>It is still 35 under the new
 * ladder</b>: the six page thresholds did not move, page 1's simply left.
 */
public final class VaultPageGate {

    private VaultPageGate() {}

    /**
     * A page with no level requirement.
     *
     * <h2>*** A NAMED ZERO, BECAUSE A BARE 0 IN THAT ARRAY READS AS A MISTAKE ***</h2>
     *
     * {@code unlocked} is {@code level >= threshold} and every real level is at least 1, so zero
     * makes the page free with no special case anywhere -- the predicate, the count and the button
     * renderer all work unchanged. <b>The name is what stops the next reader "fixing" it</b>, and
     * what stops it being read as an unfilled slot in a table of six real numbers.
     *
     * <p>It is also why {@link #refusal} refuses to format a message for a free page: there is no
     * sentence to write, and {@code "unlocks at level 0"} is the one it would invent.
     */
    public static final int FREE = 0;

    /**
     * The level that opens the HUB SHORTCUT -- the station cell at slot 29.
     *
     * <h2>*** 20 GATES A ROUTE, NOT A PAGE. RULED 2026-09-18. ***</h2>
     *
     * It used to be page 1's threshold. Page 1 is now free through the ender chest block, so the 20
     * moved onto the convenience of reaching the same storage from the hub without walking to a
     * block.
     *
     * <p><b>It lives here rather than as a literal in {@code NexusStationGate} because the ladder is
     * one thing</b> -- seven of Ben's numbers, one of which buys a route -- and because
     * {@code NexusStationGate} previously read it from {@code unlockLevel(0)}. That call now returns
     * {@link #FREE}, so a station still reading it would have opened the shortcut at level 1: <b>the
     * exact silent widening this constant exists to prevent.</b>
     */
    public static final int HUB_SHORTCUT_LEVEL = 20;

    /**
     * The level that opens each page, indexed by 0-based page.
     *
     * <p><b>A private array rather than seven constants</b>, because the one invariant worth
     * enforcing is that there are exactly as many thresholds as there are pages -- see the static
     * block below. Seven named constants cannot be counted by anything.
     *
     * <p><b>This javadoc briefly documented {@link #FREE} instead</b>, when the two constants were
     * inserted above it: two javadocs stacked, and Java attaches the first to whatever follows. The
     * array was left undocumented and {@code FREE} gained a description of an array.
     */
    private static final int[] UNLOCK_LEVELS = {FREE, 25, 30, 35, 40, 45, 50};

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

    /**
     * Has this level opened this page? <b>Inclusive</b>: level 25 opens the level-25 page.
     *
     * <p>This example was {@code level 20} until the 2026-09-18 ruling moved that number onto the
     * hub shortcut. <b>20 is no longer any page's threshold</b>, so an inclusivity example using it
     * would have demonstrated the rule on a value the array no longer contains.
     *
     * <p>{@link #FREE} makes page 1 true for every level with no special case: every real level is
     * at least 1, and {@code 1 >= 0}.
     */
    public static boolean unlocked(int page, int level) {
        return level >= unlockLevel(page);
    }

    /**
     * How many pages this level has opened. <b>{@code 1} for a new player, never 0.</b>
     *
     * <p>It said {@code 0} until page 1 became free on 2026-09-18, and the sentence under it read
     * <i>"a level-1 player has zero pages"</i>. <b>That is now the opposite of true</b>, and it was
     * the premise the whole old ladder rested on.
     *
     * <p><b>Counts rather than returning the highest index</b>, so there is no "page -1" for a
     * caller to mishandle. The thresholds are ascending, so this is also the index of the first
     * LOCKED page -- which is why it is still a count and not a boolean.
     */
    public static int unlockedPageCount(int level) {
        int count = 0;
        for (int page = 0; page < VaultShape.PAGE_COUNT; page++) {
            if (unlocked(page, level)) count++;
        }
        return count;
    }

    /**
     * Has this level opened anything at all?
     *
     * <h2>*** ALWAYS TRUE SINCE PAGE 1 BECAME FREE, AND IT IS KEPT ANYWAY ***</h2>
     *
     * It used to distinguish a player with no storage from one with some, and under the old ruling
     * that was the difference between a screen worth opening and a display case. <b>There is no such
     * player now.</b>
     *
     * <p>Kept rather than deleted because it is the one place that FACT is written down: a caller
     * asking "does this player have any vault at all" gets a true answer, and the answer is
     * interesting. Deleting it would move the question to whoever asks next, who would rebuild it
     * out of {@code unlockedPageCount(level) > 0} and not know it cannot be false.
     */
    public static boolean anyUnlocked(int level) {
        return unlockedPageCount(level) > 0;
    }

    /** Is this page free to everybody? True for page 1 alone, by the 2026-09-18 ruling. */
    public static boolean isFree(int page) {
        return unlockLevel(page) == FREE;
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
        // *** A FREE PAGE HAS NO REFUSAL, AND THIS THROWS RATHER THAN INVENTING ONE. ***
        //
        // With page 1 free, the naive formatting is "Vault page 1 unlocks at level 0" -- a sentence
        // that is grammatical, plausible, and about nothing. It cannot be reached through the screen
        // (a free page is never locked, so the click never refuses) which is exactly why it needs
        // the throw: an arm no shipped path reaches is an arm nobody would notice was lying.
        if (isFree(page)) {
            throw new IllegalArgumentException(
                    "vault page " + PageMath.displayPage(page) + " is free; there is no refusal to"
                            + " write for it, and 'unlocks at level 0' is what this prevents");
        }
        // PageMath.displayPage, NOT `page + 1`. That class calls itself "the only zero-to-one
        // conversion in the arc", and an inline `+ 1` here would make it the second -- two sources
        // of truth for one convention, which is the defect this project keeps recording. It lives
        // in core.weapon because the recipe browser needed it first; the home is odd and the
        // function is not.
        return "Vault page " + PageMath.displayPage(page) + " unlocks at level " + unlockLevel(page)
                + ". You are level " + level + ".";
    }
}
