package io.github.butterflysmp.rpg.core.weapon;

/**
 * Where page N starts and stops, for a list of entries shown a page at a time.
 *
 * <p><b>Pure, and in {@code core}, for the reason {@link CraftCount} is.</b> The recipe browser's
 * paging is four integer expressions and every one of them has an off-by-one waiting in it. Leaving
 * them inline in a menu class would make them boot-gate-only -- a category this arc has already
 * grown too large -- when they need no server at all.
 *
 * <h2>PAGES ARE ZERO-BASED HERE AND ONE-BASED ON SCREEN</h2>
 *
 * Every method on this class takes and returns a <b>zero-based</b> page index, because that is what
 * indexes arithmetic cleanly. The player is shown {@code page + 1}, and {@link #displayPage} is the
 * only place that conversion happens.
 *
 * <p>Stated this loudly because a mixed convention is the classic source of a browser that skips
 * the first entry or shows an empty final page, and the two are indistinguishable from a typo.
 *
 * <h2>An empty list is ONE page, not zero</h2>
 *
 * {@link #pageCount} never returns 0. A browser with nothing in it still has a page to draw -- with
 * an empty-state message on it -- and every caller would otherwise need its own {@code max(1, ...)}
 * to avoid rendering "Page 1 of 0". Getting that wrong is CLAUDE.md's discovery rule in miniature:
 * a zero page count makes an empty catalogue render as a working browser that happens to be blank.
 */
public final class PageMath {

    private PageMath() {}

    /**
     * How many pages {@code total} entries occupy at {@code pageSize} per page.
     *
     * <p>Always at least 1 -- see the class javadoc. The ceiling division is written as
     * {@code (total + pageSize - 1) / pageSize} rather than with floating point, because
     * {@code Math.ceil((double) total / pageSize)} is exact only until the counts are large, and
     * "exact only until" is not a property worth relying on for a page number.
     *
     * @param total    how many entries there are. Negative is treated as none.
     * @param pageSize entries per page. Must be positive.
     * @throws IllegalArgumentException if {@code pageSize} is not positive
     */
    public static int pageCount(int total, int pageSize) {
        requirePositive(pageSize);
        if (total <= 0) return 1;
        return (total + pageSize - 1) / pageSize;
    }

    /**
     * The requested page, forced into range. <b>Clamps; it does not wrap.</b>
     *
     * <p>Wrapping would be a defect rather than a nicety: the browser's next/previous buttons are
     * hidden at the ends, so a page index out of range means something has gone wrong -- a stale
     * click, or a catalogue that shrank under the player. Snapping to the nearest real page shows
     * them something true. Wrapping teleports them to the far end of the list for no reason they
     * can see.
     *
     * @param page     a zero-based page index, possibly out of range
     * @param total    how many entries there are
     * @param pageSize entries per page. Must be positive.
     */
    public static int clampPage(int page, int total, int pageSize) {
        int last = pageCount(total, pageSize) - 1;
        if (page < 0) return 0;
        if (page > last) return last;
        return page;
    }

    /**
     * The index of the first entry on {@code page}, inclusive.
     *
     * <p><b>NOT clamped against {@code total}.</b> A caller that asks for a page past the end gets
     * an index past the end. Clamping here would quietly turn a bad page index into a valid-looking
     * one, which is the failure {@link #clampPage} exists to make explicit instead.
     *
     * <p>The multiply saturates rather than overflowing: {@code page * pageSize} in {@code int}
     * wraps NEGATIVE for a large enough page, and a negative start index is the one result that
     * would slip past every {@code max(0, ...)} downstream and produce a page showing the wrong
     * entries rather than no entries.
     *
     * @param page     a zero-based page index
     * @param pageSize entries per page. Must be positive.
     */
    public static int startIndex(int page, int pageSize) {
        requirePositive(pageSize);
        long start = (long) Math.max(0, page) * pageSize;
        return (int) Math.min(start, Integer.MAX_VALUE);
    }

    /**
     * The index one past the last entry on {@code page} -- <b>exclusive</b>, so
     * {@code list.subList(startIndex(..), endIndex(..))} is the page.
     *
     * <p>Exclusive, and named {@code endIndex} rather than {@code lastIndex}, because
     * {@link java.util.List#subList} is what every caller does with it and an inclusive bound would
     * need a {@code + 1} at each call site. One place to be wrong is better than three.
     *
     * <p>The {@code min(total)} is what makes a short final page correct rather than an
     * {@link IndexOutOfBoundsException}: a genuinely short last page is the normal case, not a
     * defect.
     *
     * <h3>THE PAIR IS ONLY SLICEABLE FOR A CLAMPED PAGE, and that is the contract</h3>
     *
     * For a page past the end this returns {@code total}, which is LESS than {@link #startIndex} --
     * so {@code subList} would throw. That is deliberate and it is why {@link #clampPage} exists:
     * run the page through it first and the pair is always valid, because a clamped page's start is
     * below {@code total} whenever there is anything to show.
     *
     * <p>Returning something sliceable for an out-of-range page was the alternative, and it is
     * worse -- it renders an empty page for a click that should never have been possible, which
     * looks exactly like a correct empty final page. {@link #sizeOfPage} is the guard for a caller
     * that genuinely does not know whether its page is in range.
     *
     * @param page     a zero-based page index
     * @param pageSize entries per page. Must be positive.
     * @param total    how many entries there are
     */
    public static int endIndex(int page, int pageSize, int total) {
        long end = (long) startIndex(page, pageSize) + pageSize;
        return (int) Math.min(Math.max(0, total), end);
    }

    /**
     * How many entries actually appear on {@code page}. Zero past the end.
     *
     * <p>Exists so a caller can ask "is this page empty" without doing the subtraction itself and
     * getting a negative answer for a page past the end.
     */
    public static int sizeOfPage(int page, int pageSize, int total) {
        return Math.max(0, endIndex(page, pageSize, total) - startIndex(page, pageSize));
    }

    /**
     * The one-based page number to SHOW the player. The only zero-to-one conversion in the arc.
     *
     * @param page a zero-based page index
     */
    public static int displayPage(int page) {
        return Math.max(0, page) + 1;
    }

    private static void requirePositive(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive, was " + pageSize);
        }
    }
}
