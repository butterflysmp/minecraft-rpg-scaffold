package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The browser's paging arithmetic. Each test names the mutation it forces red.
 *
 * <p>The last two are the ones worth having: everything above them checks a single expression,
 * while {@link #everyEntryAppearsExactlyOnceAcrossAllPages} and
 * {@link #theSlicesAreAlwaysValidSubListBoundsAfterClamping} check the PROPERTY the browser actually
 * needs -- no entry lost, none shown twice -- across every list size against a fixed page size. An
 * off-by-one that survives the individual assertions cannot survive those.
 */
class PageMathTest {

    private static final int PAGE = 45;   // the browser's real page size, so the sweeps are honest

    // --- pageCount -----------------------------------------------------------------------------

    @Test
    void anEmptyCatalogueIsONEPageNotZero() {
        assertEquals(1, PageMath.pageCount(0, PAGE), "an empty browser still has a page to draw");
        assertEquals(1, PageMath.pageCount(-7, PAGE), "a negative total is treated as none");
        // Mutation: `if (total <= 0) return 1` -> `return 0` -> both redden, and the browser would
        // render "Page 1 of 0".
    }

    @Test
    void aPartialPageStillCountsAsAWholeOne() {
        assertEquals(1, PageMath.pageCount(1, PAGE));
        assertEquals(1, PageMath.pageCount(PAGE, PAGE), "exactly full is still one page");
        assertEquals(2, PageMath.pageCount(PAGE + 1, PAGE), "one over rolls to a second page");
        assertEquals(2, PageMath.pageCount(2 * PAGE, PAGE));
        assertEquals(3, PageMath.pageCount(2 * PAGE + 1, PAGE));
        // Mutation: drop the `+ pageSize - 1` (floor instead of ceiling) -> the PAGE+1 and 2*PAGE+1
        // cases redden, and the final entries would be unreachable.
    }

    @Test
    void pageSizeMustBePositive() {
        // Not defensive noise: pageSize comes from a layout constant, and a layout edit that leaves
        // it 0 would otherwise divide by zero deep inside a click handler rather than at boot.
        assertThrows(IllegalArgumentException.class, () -> PageMath.pageCount(10, 0));
        assertThrows(IllegalArgumentException.class, () -> PageMath.pageCount(10, -1));
        assertThrows(IllegalArgumentException.class, () -> PageMath.startIndex(0, 0));
    }

    // --- clampPage -----------------------------------------------------------------------------

    @Test
    void clampPageCLAMPSAndDoesNotWRAP() {
        // THE distinction. Wrapping teleports a player to the far end of the list for no reason
        // they can see; clamping shows them the nearest real page.
        assertEquals(0, PageMath.clampPage(-1, 200, PAGE),
                "below the start clamps to 0, not to the last page");
        assertEquals(0, PageMath.clampPage(Integer.MIN_VALUE, 200, PAGE));

        int last = PageMath.pageCount(200, PAGE) - 1;      // 200 entries at 45 => 5 pages, last = 4
        assertEquals(4, last);
        assertEquals(last, PageMath.clampPage(last + 1, 200, PAGE),
                "past the end clamps to the last page, not to 0");
        assertEquals(last, PageMath.clampPage(Integer.MAX_VALUE, 200, PAGE));
        // Mutation: `if (page < 0) return 0` -> `return last`, or `if (page > last) return 0`
        // (a wrap) -> reddens on the "not to the last page" / "not to 0" assertions specifically.
    }

    @Test
    void clampPageLeavesAnInRangePageAlone() {
        for (int p = 0; p <= 4; p++) {
            assertEquals(p, PageMath.clampPage(p, 200, PAGE), "page " + p + " is already in range");
        }
    }

    @Test
    void clampingAnEmptyCatalogueYieldsPageZero() {
        // pageCount is 1, so last is 0. This is the case where a wrap and a clamp agree, which is
        // exactly why the wrap test above uses a non-empty list.
        assertEquals(0, PageMath.clampPage(0, 0, PAGE));
        assertEquals(0, PageMath.clampPage(9, 0, PAGE));
        assertEquals(0, PageMath.clampPage(-9, 0, PAGE));
    }

    // --- startIndex / endIndex -----------------------------------------------------------------

    @Test
    void startIndexAdvancesByAFULLPagePerPage() {
        assertEquals(0, PageMath.startIndex(0, PAGE));
        assertEquals(45, PageMath.startIndex(1, PAGE));
        assertEquals(90, PageMath.startIndex(2, PAGE));
        // Mutation: `page * (pageSize - 1)` -> 44 and 88 -> reddens. That mutation is the one that
        // makes the last entry of each page repeat as the first of the next, which is the defect
        // gate row Q11 was reworded to name.
    }

    @Test
    void endIndexIsEXCLUSIVEAndStopsAtTheTotal() {
        assertEquals(45, PageMath.endIndex(0, PAGE, 200),
                "a full first page ends one past its last entry");
        assertEquals(200, PageMath.endIndex(4, PAGE, 200), "the short final page stops at the total");
        assertEquals(20, PageMath.endIndex(0, PAGE, 20), "a single short page stops at the total");
        assertEquals(0, PageMath.endIndex(0, PAGE, 0), "an empty catalogue slices to nothing");
        // Mutation: drop the `min(total)` -> the final-page cases return 225 -> redden, and a real
        // subList would throw IndexOutOfBoundsException in a click handler.
    }

    @Test
    void aShortFinalPageIsCORRECTNotADefect() {
        // Stated as its own test because "the last page is short" was the ambiguous wording in the
        // gate row: a short last page is the normal case. The defects are entries REPEATED from the
        // previous page, or a final page that DROPS entries -- both covered by the sweep below.
        assertEquals(20, PageMath.sizeOfPage(4, PAGE, 200), "200 entries: four full pages then 20");
        assertEquals(45, PageMath.sizeOfPage(3, PAGE, 200));
    }

    @Test
    void aPagePastTheEndIsEmptyRatherThanNegative() {
        assertEquals(0, PageMath.sizeOfPage(9, PAGE, 200));
        assertEquals(0, PageMath.sizeOfPage(1, PAGE, 0));
        // Mutation: drop the `max(0, ...)` in sizeOfPage -> returns a negative size -> reddens.
    }

    @Test
    void aHugePageIndexSaturatesRatherThanWrappingAtAll() {
        // int overflow here slips past every downstream max(0, ..), so the multiply is done in long
        // and saturated.
        //
        // THE SIGN ASSERTION BELOW DOES NOT DISCRIMINATE, AND THAT IS MEASURED, NOT ASSUMED. This
        // test was first written believing the int multiply "wraps negative"; the mutation was run
        // and printed
        //
        //     expected: <2147483647> but was: <2147483603>
        //
        // Integer.MAX_VALUE * 45 in int wraps to a POSITIVE 2147483603, so `> 0` stays green through
        // the very mutation it was written to catch. It is kept only to document that -- some other
        // page index does wrap negative, so the sign is worth stating -- but the assertEquals is the
        // assertion doing the work, and pretending otherwise would credit coverage that is not there.
        assertTrue(PageMath.startIndex(Integer.MAX_VALUE, PAGE) > 0,
                "not discriminating for the int-multiply mutation -- see the comment");
        assertEquals(Integer.MAX_VALUE, PageMath.startIndex(Integer.MAX_VALUE, PAGE),
                "saturates rather than wrapping");
        assertEquals(0, PageMath.sizeOfPage(Integer.MAX_VALUE, PAGE, 200));
        // Mutation: `(long) Math.max(0, page) * pageSize` -> `Math.max(0, page) * pageSize`
        // (int multiply) -> reddens on the assertEquals with 2147483603. Watched red.
    }

    // --- the properties the browser actually needs ----------------------------------------------

    @Test
    void everyEntryAppearsExactlyOnceAcrossAllPages() {
        // THE property. Walk every page of every catalogue size from 0 to 3 pages' worth and
        // reassemble the list from its slices: it must come back identical. Nothing lost, nothing
        // duplicated, nothing reordered.
        for (int total = 0; total <= 3 * PAGE + 7; total++) {
            List<Integer> source = new ArrayList<>();
            for (int i = 0; i < total; i++) source.add(i);

            List<Integer> rebuilt = new ArrayList<>();
            int pages = PageMath.pageCount(total, PAGE);
            for (int p = 0; p < pages; p++) {
                rebuilt.addAll(source.subList(
                        PageMath.startIndex(p, PAGE), PageMath.endIndex(p, PAGE, total)));
            }
            assertEquals(source, rebuilt, "total=" + total + " did not round-trip through its pages");
        }
        // Mutation: ANY off-by-one in startIndex, endIndex or pageCount -> reddens, naming the size.
        // This is the test that makes the individual ones above redundant-but-diagnostic.
    }

    @Test
    void theSlicesAreAlwaysValidSubListBoundsAfterClamping() {
        // The contract endIndex's javadoc states: clamp first, and the pair is always sliceable.
        // Asserted rather than trusted, because the class deliberately does NOT make an unclamped
        // page safe -- so if clampPage ever stopped being sufficient, nothing else would notice.
        for (int total = 0; total <= 2 * PAGE + 3; total++) {
            for (int requested : new int[] {Integer.MIN_VALUE, -3, 0, 1, 2, 5, Integer.MAX_VALUE}) {
                int page = PageMath.clampPage(requested, total, PAGE);
                int from = PageMath.startIndex(page, PAGE);
                int to = PageMath.endIndex(page, PAGE, total);
                assertTrue(0 <= from && from <= to && to <= total,
                        "total=" + total + " requested=" + requested
                                + " gave bounds [" + from + ", " + to + "]");
            }
        }
    }

    @Test
    void displayPageIsTheONLYPlaceZeroBasedBecomesOneBased() {
        assertEquals(1, PageMath.displayPage(0), "the first page is shown as 1");
        assertEquals(5, PageMath.displayPage(4));
        assertEquals(1, PageMath.displayPage(-2), "a negative index still shows as page 1");
        // Mutation: return `page` -> the player sees "Page 0 of 5" -> reddens.
    }
}
