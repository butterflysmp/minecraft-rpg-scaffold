package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every player's patch at every slot, plus the regrowth clock: in memory, kept across logout, lost
 * on restart (the seat's ruling 4, 2026-11-13). A broken patch is regrowing until its deadline, and
 * the first look after that rolls a fresh patch.
 *
 * <p>S2 (the seat's rulings 1, 2, 4 and 7, 2026-11-14): a slot carries a MAXIMUM size; each patch
 * rolls its own size when it is created; a click on a slot block outside the current patch's
 * footprint is judged as nothing; {@code force} replaces the patch with a chosen size.
 *
 * <p>Every script starts with the SIZE draw ({@code nextInt(0, sum of eligible weights)}), then the
 * layout (count, then u, v pairs), then any payout rolls, in the order the book asks for them.
 */
class MiningBookTest {

    private static final UUID BEN = new UUID(1, 1);
    private static final UUID OTHER = new UUID(2, 2);

    /** A small slot at (10, 64, 20) facing NORTH: its only eligible size is small, drawn from [0, 50). */
    private static Slot smallSlot() { return MiningFixtures.slot(MiningFixtures.small()); }

    /** The small slot's anchor: inside every patch it can hold. */
    private static final BlockPos ANCHOR = new BlockPos(10, 64, 20);

    private final AtomicLong now = new AtomicLong(100);

    private MiningBook book(int... script) {
        return new MiningBook(new RegrowthClock(now::get), ScriptedRandom.of(script), now::get);
    }

    private Optional<Patch> look(MiningBook book, UUID who, Slot slot) {
        return book.patch(who, slot, MiningFixtures.iron(), MiningFixtures.sizes());
    }

    private ClickOutcome clickAt(MiningBook book, UUID who, Slot slot, BlockPos block, Click click) {
        return book.click(who, slot, MiningFixtures.iron(), MiningFixtures.sizes(), block, click).orElseThrow();
    }

    private static Click hitAt(Spot s, long tick) {
        return new Click(1.0f, Optional.of(new PatchPoint(s.cu() + 0.5, s.cv() + 0.5)), true, tick);
    }

    @Test
    void theFirstLookRollsAPatchAndLaterLooksReturnTheSameOne() {
        MiningBook book = book(0, 2, 10, 10, 30, 20);
        Patch first = look(book, BEN, smallSlot()).orElseThrow();
        assertEquals("small", first.size().id());
        assertEquals(List.of(new Spot(10, 10), new Spot(30, 20)), first.layout().spots());
        assertEquals(100L, first.revealTick(0), "the first spot is revealed when the patch is first shown");
        assertSame(first, look(book, BEN, smallSlot()).orElseThrow(), "kept, not re-rolled");
    }

    @Test
    void peekReadsTheExistingPatchAndNeverCreatesOne() {
        // The selftest's read-only accessor. A peek before the first look finds nothing AND draws nothing:
        // the script holds exactly one patch's draws, so a peek that rolled would leave the look below
        // short of numbers ("exhausted"), and a peek that created would return the patch.
        MiningBook book = book(0, 2, 10, 10, 30, 20);
        assertTrue(book.peek(BEN, smallSlot().id()).isEmpty(), "no look yet: nothing to read");
        Patch first = look(book, BEN, smallSlot()).orElseThrow();
        assertSame(first, book.peek(BEN, smallSlot().id()).orElseThrow(), "the same patch the shipped path made");
        assertTrue(book.peek(OTHER, smallSlot().id()).isEmpty(), "another player's patch is not this one");
    }

    @Test
    void peekIsEmptyWhileRegrowingAndDoesNotEndTheRegrowth() {
        MiningBook book = book(0, 2, 10, 10, 30, 20, 2, 2);
        Patch p = look(book, BEN, smallSlot()).orElseThrow();
        clickAt(book, BEN, smallSlot(), ANCHOR, hitAt(p.layout().spots().get(0), 110));
        clickAt(book, BEN, smallSlot(), ANCHOR, hitAt(p.layout().spots().get(1), 111));
        assertTrue(book.peek(BEN, smallSlot().id()).isEmpty(), "broken: regrowing, no patch");
        assertEquals(100 + 1200, book.regrowAt(BEN, smallSlot().id()).orElseThrow(),
                "and the peek left the clock alone (the clock reads the book's now, 100, not the click's tick)");
    }

    @Test
    void eachPlayerHasTheirOwnPatchAtTheSameSlot() {
        MiningBook book = book(0, 2, 10, 10, 30, 20, 0, 2, 40, 6, 3, 25);
        Patch ben = look(book, BEN, smallSlot()).orElseThrow();
        Patch other = look(book, OTHER, smallSlot()).orElseThrow();
        assertNotSame(ben, other);
        assertEquals(List.of(new Spot(40, 6), new Spot(3, 25)), other.layout().spots());
    }

    // --- S2: the size is rolled per patch, from the sizes that fit the slot ----------------------

    @Test
    void aPatchAtAMediumSlotRollsItsSizeAndItsLayoutAtThatSize() {
        Slot slot = MiningFixtures.slot(MiningFixtures.medium());
        // Draw 50 of [0, 80) is medium; then 4 spots inside medium's 80 x 48 pixels.
        MiningBook book = book(50, 4, 10, 10, 30, 10, 50, 10, 70, 40);
        Patch p = look(book, BEN, slot).orElseThrow();
        assertEquals("medium", p.size().id());
        assertEquals(4, p.layout().spots().size());
        assertEquals(new Spot(70, 40), p.layout().spots().get(3), "a centre only a medium patch can hold");
    }

    @Test
    void theBooksDrawIsBoundedByTheSlotsMaximumNotByTheOresLargestSize() {
        // At a medium slot the size draw is from [0, 80). ScriptedRandom refuses 80 with a message naming
        // the bound, so the bound the BOOK passed is read directly. (Any other refusal, e.g. an exhausted
        // script after a wrongly-rolled large, has a different message, so this row cannot pass hollow.)
        Slot slot = MiningFixtures.slot(MiningFixtures.medium());
        var ex = assertThrows(IllegalStateException.class, () -> look(book(80), BEN, slot));
        assertEquals("scripted 80 is outside [0, 80)", ex.getMessage());
        // Mutation: roll against the ore's largest size instead of the slot's maximum -> 80 is legal in
        // [0, 100) and rolls large -> the script runs out -> a different message -> reddens.
    }

    @Test
    void theSameMediumSlotRollsSmallOnALowDraw() {
        Slot slot = MiningFixtures.slot(MiningFixtures.medium());
        MiningBook book = book(49, 2, 10, 10, 30, 20);
        assertEquals("small", look(book, BEN, slot).orElseThrow().size().id(), "draw 49 of [0, 80) is small");
    }

    @Test
    void aBreakAndRegrowthRollsTheSizeAgain() {
        Slot slot = MiningFixtures.slot(MiningFixtures.medium());
        // small (draw 0) with 2 spots; two payout rolls; then medium (draw 79) with 4 spots.
        MiningBook book = book(0, 2, 10, 10, 30, 20, 2, 2, 79, 4, 10, 10, 30, 10, 50, 10, 70, 40);
        Patch p = look(book, BEN, slot).orElseThrow();
        BlockPos in = ANCHOR;
        clickAt(book, BEN, slot, in, hitAt(p.layout().spots().get(0), 110));
        ClickOutcome broke = clickAt(book, BEN, slot, in, hitAt(p.layout().spots().get(1), 111));
        assertEquals(9, assertInstanceOf(ClickOutcome.Broke.class, broke).payout().total(), "2 + 2 + small's 5");
        now.set(111 + 1200);
        Patch again = look(book, BEN, slot).orElseThrow();
        assertEquals("medium", again.size().id(), "re-rolled at regrowth");
    }

    @Test
    void theBreakPaysTheRolledSizesBonusNotTheSlotsMaximums() {
        // An extra_large slot holding a MEDIUM patch: the bonus is medium's 6. Staged so the three
        // candidate bonuses differ: medium 6 (right), extra_large 10 (the slot's maximum), small 5 (the
        // ore's first size). Draw 50 of [0, 100) is medium; 4 spots; then four payout rolls.
        Slot slot = MiningFixtures.slot(MiningFixtures.extraLarge());
        MiningBook book = book(50, 4, 10, 10, 30, 10, 50, 10, 70, 40, 2, 3, 4, 2);
        Patch p = look(book, BEN, slot).orElseThrow();
        for (int i = 0; i < 3; i++) clickAt(book, BEN, slot, ANCHOR, hitAt(p.layout().spots().get(i), 110 + i));
        ClickOutcome broke = clickAt(book, BEN, slot, ANCHOR, hitAt(p.layout().spots().get(3), 113));
        Payout payout = assertInstanceOf(ClickOutcome.Broke.class, broke).payout();
        assertEquals(6, payout.bonus(), "medium's bonus");
        assertEquals(List.of(2, 3, 4, 2), payout.rolls(), "one roll per spot of the MEDIUM patch");
        assertEquals(17, payout.total());
        // Mutations: the rule from the slot's maximum -> 10 -> reddens; from the ore's first size -> 5.
    }

    // --- S2: a click on a slot block outside the current patch is judged as nothing --------------

    @Test
    void aClickOnASlotBlockOutsideTheCurrentPatchHasNoOutcomeAndChangesNothing() {
        // An extra_large slot (7 x 4) holding a small patch (3 x 2) at the same anchor. NORTH's right
        // is -X, so the slot runs x 13..7 and y 64..67; the small patch runs x 11..9 and y 64..65.
        Slot slot = MiningFixtures.slot(MiningFixtures.extraLarge());
        MiningBook book = book(0, 2, 10, 10, 30, 20);
        Patch p = look(book, BEN, slot).orElseThrow();
        Click charged = hitAt(p.layout().spots().get(0), 110);

        BlockPos beside = new BlockPos(12, 64, 20);   // inside the slot, one column beyond the patch
        BlockPos above = new BlockPos(10, 66, 20);    // inside the slot, one row above the patch
        assertTrue(slot.frame().footprint().contains(beside) && slot.frame().footprint().contains(above),
                "both blocks are slot blocks");
        assertEquals(Optional.empty(), book.click(BEN, slot, MiningFixtures.iron(), MiningFixtures.sizes(), beside, charged));
        assertEquals(Optional.empty(), book.click(BEN, slot, MiningFixtures.iron(), MiningFixtures.sizes(), above, charged));
        assertEquals(0, p.liveIndex(), "nothing changed");

        // The patch's own edge blocks DO judge the click.
        assertEquals(new ClickOutcome.Hit(0, 1), clickAt(book, BEN, slot, new BlockPos(11, 65, 20), charged));
        // Mutations: test only the columns -> "above" judges a HIT -> reddens; test only the rows ->
        // "beside" reddens.
    }

    @Test
    void thePatchFrameKeepsTheSlotsAnchorSoItIsCentredAndSharesTheBottomRow() {
        Slot slot = MiningFixtures.slot(MiningFixtures.extraLarge());
        MiningBook book = book(50, 4, 10, 10, 30, 10, 50, 10, 70, 40);
        Patch p = look(book, BEN, slot).orElseThrow();
        FaceFrame frame = p.frame(slot);
        assertEquals(new FaceFrame(10, 64, 20, WallFace.NORTH, 5, 3), frame);
        assertTrue(slot.frame().footprint().containsAll(frame.footprint()), "a patch lies inside its slot");
    }

    @Test
    void aRegrowingSlotJudgesEveryOneOfItsBlocksNotLive() {
        // While regrowing there is no current patch, so the whole slot face is NOT_LIVE (as in slice 1),
        // never "outside".
        Slot slot = MiningFixtures.slot(MiningFixtures.extraLarge());
        MiningBook book = book(0, 2, 10, 10, 30, 20, 2, 2);
        Patch p = look(book, BEN, slot).orElseThrow();
        clickAt(book, BEN, slot, ANCHOR, hitAt(p.layout().spots().get(0), 110));
        clickAt(book, BEN, slot, ANCHOR, hitAt(p.layout().spots().get(1), 111));
        assertEquals(ClickOutcome.NotLive.INSTANCE, clickAt(book, BEN, slot, new BlockPos(13, 67, 20), hitAt(new Spot(5, 5), 112)),
                "the slot's far corner");
    }

    // --- S2: the dev command's forced size --------------------------------------------------------

    @Test
    void forceReplacesThePatchWithAFreshOneOfTheChosenSize() {
        Slot slot = MiningFixtures.slot(MiningFixtures.extraLarge());
        MiningBook book = book(0, 2, 10, 10, 30, 20, 8, 5, 5, 15, 5, 25, 5, 35, 5, 45, 5, 55, 5, 65, 5, 75, 5);
        Patch before = look(book, BEN, slot).orElseThrow();
        now.set(150);
        Patch forced = book.force(BEN, slot, MiningFixtures.iron(), MiningFixtures.sizes(), "extra_large");
        assertNotSame(before, forced);
        assertEquals("extra_large", forced.size().id());
        assertEquals(8, forced.layout().spots().size());
        assertEquals(150L, forced.revealTick(0));
        assertSame(forced, look(book, BEN, slot).orElseThrow(), "it is now the player's patch");
    }

    @Test
    void forceEndsThatSlotsRegrowth() {
        Slot slot = smallSlot();
        MiningBook book = book(0, 2, 10, 10, 30, 20, 2, 2, 2, 40, 6, 3, 25);
        Patch p = look(book, BEN, slot).orElseThrow();
        clickAt(book, BEN, slot, ANCHOR, hitAt(p.layout().spots().get(0), 110));
        clickAt(book, BEN, slot, ANCHOR, hitAt(p.layout().spots().get(1), 111));
        assertTrue(book.regrowAt(BEN, slot.id()).isPresent());
        book.force(BEN, slot, MiningFixtures.iron(), MiningFixtures.sizes(), "small");
        assertTrue(book.regrowAt(BEN, slot.id()).isEmpty(), "forced back now");
        assertTrue(look(book, BEN, slot).isPresent());
    }

    @Test
    void forceRefusesASizeThatDoesNotFitOrThatTheOreDoesNotList() {
        MiningBook book = book();
        var tooBig = assertThrows(IllegalArgumentException.class, () -> book.force(BEN,
                MiningFixtures.slot(MiningFixtures.large()), MiningFixtures.iron(), MiningFixtures.sizes(), "extra_large"));
        assertEquals("Size 'extra_large' (7 x 4) does not fit this slot, whose maximum is 'large' (7 x 3).", tooBig.getMessage());

        java.util.Map<String, Integer> w = new java.util.LinkedHashMap<>();
        w.put("small", 50);
        w.put("large", 15);
        OreDefinition noMedium = MiningFixtures.ironListing(w);
        var unlisted = assertThrows(IllegalArgumentException.class, () -> book.force(BEN,
                MiningFixtures.slot(MiningFixtures.large()), noMedium, MiningFixtures.sizes(), "medium"));
        assertEquals("Ore 'iron' does not list size 'medium'.", unlisted.getMessage());

        var unknown = assertThrows(IllegalArgumentException.class, () -> book.force(BEN,
                MiningFixtures.slot(MiningFixtures.large()), MiningFixtures.iron(), MiningFixtures.sizes(), "huge"));
        assertEquals("No vein size 'huge' is loaded.", unknown.getMessage());
    }

    // --- Regrowth ---------------------------------------------------------------------------------

    @Test
    void breakingStartsRegrowthAndTheNextLookAfterItRollsAgain() {
        // Size, layout 1, the payout rolls (2 and 3), then size and layout 2.
        MiningBook book = book(0, 2, 10, 10, 30, 20, 2, 3, 0, 2, 40, 6, 3, 25);
        Patch p = look(book, BEN, smallSlot()).orElseThrow();
        assertEquals(new ClickOutcome.Hit(0, 1), clickAt(book, BEN, smallSlot(), ANCHOR, hitAt(p.layout().spots().get(0), 110)));
        now.set(120);
        ClickOutcome last = clickAt(book, BEN, smallSlot(), ANCHOR, hitAt(p.layout().spots().get(1), 120));
        assertEquals(10, assertInstanceOf(ClickOutcome.Broke.class, last).payout().total(), "2 + 3 + 5");
        assertEquals(1320L, book.regrowAt(BEN, smallSlot().id()).orElseThrow(), "broke at 120, + 1200");

        now.set(1319);
        assertTrue(look(book, BEN, smallSlot()).isEmpty(), "regrowing: nothing to show");
        assertEquals(ClickOutcome.NotLive.INSTANCE, clickAt(book, BEN, smallSlot(), ANCHOR, hitAt(new Spot(40, 6), 1319)));

        now.set(1320);
        Patch fresh = look(book, BEN, smallSlot()).orElseThrow();
        assertEquals(List.of(new Spot(40, 6), new Spot(3, 25)), fresh.layout().spots(), "re-rolled");
        assertEquals(1320L, fresh.revealTick(0));
    }

    @Test
    void clearingAPlayersRegrowthLetsThePatchBackAtOnce() {
        MiningBook book = book(0, 2, 10, 10, 30, 20, 2, 2, 0, 2, 40, 6, 3, 25);
        Patch p = look(book, BEN, smallSlot()).orElseThrow();
        clickAt(book, BEN, smallSlot(), ANCHOR, hitAt(p.layout().spots().get(0), 110));
        clickAt(book, BEN, smallSlot(), ANCHOR, hitAt(p.layout().spots().get(1), 111));
        assertTrue(look(book, BEN, smallSlot()).isEmpty());
        book.clearRegrowth(BEN);
        assertTrue(look(book, BEN, smallSlot()).isPresent());
    }

    // --- D1 (the seat, 2026-11-13): a regrowing patch always has ONE pending task in this session ---

    /** Breaks BEN's patch at tick 120 so it regrows at 1320. Size, layout, then the two payout rolls. */
    private MiningBook brokenAt120() {
        MiningBook book = book(0, 2, 10, 10, 30, 20, 2, 3, 0, 2, 40, 6, 3, 25);
        Patch p = look(book, BEN, smallSlot()).orElseThrow();
        clickAt(book, BEN, smallSlot(), ANCHOR, hitAt(p.layout().spots().get(0), 110));
        now.set(120);
        clickAt(book, BEN, smallSlot(), ANCHOR, hitAt(p.layout().spots().get(1), 120));
        return book;
    }

    @Test
    void aRegrowingPatchClaimsOneTaskAndALivePatchClaimsNone() {
        MiningBook live = book(0, 2, 10, 10, 30, 20);
        look(live, BEN, smallSlot());
        assertTrue(live.claimRegrowthTask(BEN, smallSlot().id()).isEmpty(), "a live patch needs no regrowth task");

        MiningBook book = brokenAt120();
        now.set(500);
        long token = book.claimRegrowthTask(BEN, smallSlot().id()).orElseThrow();
        assertTrue(book.claimRegrowthTask(BEN, smallSlot().id()).isEmpty(), "deduped: one task per player and slot");
        assertTrue(book.isCurrentRegrowthTask(BEN, smallSlot().id(), token));
        assertTrue(book.claimRegrowthTask(OTHER, smallSlot().id()).isEmpty(), "OTHER is not regrowing there");
    }

    @Test
    void aQuitDropsTheTaskSoTheRejoinedSessionClaimsAFreshOne() {
        MiningBook book = brokenAt120();
        long old = book.claimRegrowthTask(BEN, smallSlot().id()).orElseThrow();
        book.forgetSession(BEN);
        now.set(700);
        long fresh = book.claimRegrowthTask(BEN, smallSlot().id()).orElseThrow();
        assertFalse(book.isCurrentRegrowthTask(BEN, smallSlot().id(), old), "the old session's task is stale");
        assertTrue(book.isCurrentRegrowthTask(BEN, smallSlot().id(), fresh));
        assertEquals(1320L, book.regrowAt(BEN, smallSlot().id()).orElseThrow(), "the clock itself is KEPT across logout");
    }

    @Test
    void releasingAfterTheShowFreesTheMarkerForTheNextBreak() {
        MiningBook book = brokenAt120();
        long token = book.claimRegrowthTask(BEN, smallSlot().id()).orElseThrow();
        now.set(1320);
        assertTrue(book.releaseRegrowthTask(BEN, smallSlot().id(), token));
        assertFalse(book.isCurrentRegrowthTask(BEN, smallSlot().id(), token));
        assertTrue(book.claimRegrowthTask(BEN, smallSlot().id()).isEmpty(), "regrown: nothing to claim");
    }

    @Test
    void removingASlotDropsItsPendingTasks() {
        MiningBook book = brokenAt120();
        long token = book.claimRegrowthTask(BEN, smallSlot().id()).orElseThrow();
        book.clearSlot(smallSlot().id());
        assertFalse(book.isCurrentRegrowthTask(BEN, smallSlot().id(), token));
    }

    @Test
    void removingASlotForgetsEveryPlayersPatchAndClock() {
        MiningBook book = book(0, 2, 10, 10, 30, 20, 0, 2, 40, 6, 3, 25, 0, 2, 5, 5, 20, 20);
        look(book, BEN, smallSlot());
        look(book, OTHER, smallSlot());
        book.clearSlot(smallSlot().id());
        Patch again = look(book, BEN, smallSlot()).orElseThrow();
        assertEquals(List.of(new Spot(5, 5), new Spot(20, 20)), again.layout().spots(), "a fresh roll");
    }
}
