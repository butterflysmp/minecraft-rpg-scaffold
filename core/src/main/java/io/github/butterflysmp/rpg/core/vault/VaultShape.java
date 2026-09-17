package io.github.butterflysmp.rpg.core.vault;

/**
 * How big the Nexus Vault is: seven pages of thirty-six.
 *
 * <p><b>In {@code core} because three modules need to agree about it and none of them owns it.</b>
 * {@code storage} validates a page index and a slot index against these numbers before anything
 * reaches disk; {@code paper} builds a screen out of them. A constant duplicated across that seam is
 * a constant that can disagree with itself, and the disagreement would be a page the loader accepts
 * and the screen cannot draw -- or worse, the reverse.
 *
 * <h2>SEVEN PAGES IN SEVEN BUTTON SLOTS IS NOT A COINCIDENCE, AND IT IS ASSERTED RATHER THAN NOTICED</h2>
 *
 * The vault's top row is {@code 0} left arrow, {@code 1..7} the page buttons, {@code 8} right arrow.
 * That row holds exactly {@link #PAGE_COUNT} buttons <b>because the page count happens to be seven</b>
 * -- there is no arithmetic forcing it. So the layout test asserts the two are equal rather than
 * leaving a reader to observe it.
 *
 * <p><b>Raise {@code PAGE_COUNT} to eight and the row has nowhere to put the eighth button.</b> The
 * failure without that assertion is not a compile error and not an exception: it is a page that
 * exists in storage, holds the player's items, and has no way to be selected. <b>An unreachable page
 * is indistinguishable from an empty one</b>, which is this project's discovery rule -- finding
 * nothing must be a defect, not a quiet no-op.
 *
 * <h2>{@link #TOTAL_SLOTS} IS DERIVED, NEVER TYPED</h2>
 *
 * It is {@code PAGE_COUNT * SLOTS_PER_PAGE} rather than {@code 252} so that it cannot be a figure
 * maintained by delta -- correct when written and wrong by a growing amount afterwards. The literal
 * {@code 252} appears once, in the test, where its job is to fail if either factor moves.
 */
public final class VaultShape {

    private VaultShape() {}

    /**
     * How many pages a vault has, unlocked or not.
     *
     * <p><b>This is the STORAGE shape, not the player's entitlement.</b> Every vault has seven pages
     * from the moment it exists; which of them a given player may reach is a level question and is
     * answered elsewhere. Conflating the two would put a progression rule in the persistence layer,
     * where a retune of the level curve could not reach it and a rollback would strand items.
     */
    public static final int PAGE_COUNT = 7;

    /** Slots on one page -- the four rows of nine the screen gives up to storage. */
    public static final int SLOTS_PER_PAGE = 36;

    /** Every slot in every page. Derived on purpose; see the class javadoc. */
    public static final int TOTAL_SLOTS = PAGE_COUNT * SLOTS_PER_PAGE;

    /** Is this a page index a vault actually has? Zero-based, as every index here is. */
    public static boolean isPage(int page) {
        return page >= 0 && page < PAGE_COUNT;
    }

    /** Is this a slot index within one page? */
    public static boolean isSlot(int slot) {
        return slot >= 0 && slot < SLOTS_PER_PAGE;
    }

    /**
     * @throws IllegalArgumentException naming the offending value and the legal range
     */
    public static int requirePage(int page) {
        if (!isPage(page)) {
            throw new IllegalArgumentException(
                    "vault page " + page + " is out of range; pages are 0.." + (PAGE_COUNT - 1));
        }
        return page;
    }

    /**
     * @throws IllegalArgumentException naming the offending value and the legal range
     */
    public static int requireSlot(int slot) {
        if (!isSlot(slot)) {
            throw new IllegalArgumentException(
                    "vault slot " + slot + " is out of range; slots are 0.."
                            + (SLOTS_PER_PAGE - 1));
        }
        return slot;
    }
}
