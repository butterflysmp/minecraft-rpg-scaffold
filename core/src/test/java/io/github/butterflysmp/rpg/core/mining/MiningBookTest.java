package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every player's patch at every slot, plus the regrowth clock: in memory, kept across logout, lost
 * on restart (the seat's ruling 4, 2026-11-13). A broken patch is regrowing until its deadline, and
 * the first look after that rolls a fresh layout.
 */
class MiningBookTest {

    private static final UUID BEN = new UUID(1, 1);
    private static final UUID OTHER = new UUID(2, 2);
    private static final VeinSize SMALL = new VeinSize("small", 3, 2, 2, 3);
    private static final PayoutRule RULE = new PayoutRule(2, 4, 5);
    private static final Slot SLOT = Slot.of("w", new FaceFrame(10, 64, 20, WallFace.NORTH, 3, 2), "iron", "small");

    private final AtomicLong now = new AtomicLong(100);

    // Layouts: count 2, then (u, v) pairs. One fixed layout per roll, so each roll is known.
    private MiningBook book(int... script) {
        return new MiningBook(new RegrowthClock(now::get), ScriptedRandom.of(script), now::get);
    }

    private static Click hitAt(Spot s, long tick) {
        return new Click(1.0f, Optional.of(new PatchPoint(s.cu() + 0.5, s.cv() + 0.5)), true, tick);
    }

    @Test
    void theFirstLookRollsAPatchAndLaterLooksReturnTheSameOne() {
        MiningBook book = book(2, 10, 10, 30, 20);
        Patch first = book.patch(BEN, SLOT, SMALL, RULE).orElseThrow();
        assertEquals(List.of(new Spot(10, 10), new Spot(30, 20)), first.layout().spots());
        assertEquals(100L, first.revealTick(0), "the first spot is revealed when the patch is first shown");
        assertSame(first, book.patch(BEN, SLOT, SMALL, RULE).orElseThrow(), "kept, not re-rolled");
    }

    @Test
    void eachPlayerHasTheirOwnPatchAtTheSameSlot() {
        MiningBook book = book(2, 10, 10, 30, 20, 2, 40, 6, 3, 25);
        Patch ben = book.patch(BEN, SLOT, SMALL, RULE).orElseThrow();
        Patch other = book.patch(OTHER, SLOT, SMALL, RULE).orElseThrow();
        assertNotSame(ben, other);
        assertEquals(List.of(new Spot(40, 6), new Spot(3, 25)), other.layout().spots());
    }

    @Test
    void breakingStartsRegrowthAndTheNextLookAfterItRollsAgain() {
        // Layout 1, then the payout rolls (2 and 3), then layout 2.
        MiningBook book = book(2, 10, 10, 30, 20, 2, 3, 2, 40, 6, 3, 25);
        Patch p = book.patch(BEN, SLOT, SMALL, RULE).orElseThrow();
        assertEquals(new ClickOutcome.Hit(0, 1),
                book.click(BEN, SLOT, SMALL, RULE, 1200, hitAt(p.layout().spots().get(0), 110)));
        now.set(120);
        ClickOutcome last = book.click(BEN, SLOT, SMALL, RULE, 1200, hitAt(p.layout().spots().get(1), 120));
        assertEquals(10, assertInstanceOf(ClickOutcome.Broke.class, last).payout().total(), "2 + 3 + 5");
        assertEquals(1320L, book.regrowAt(BEN, SLOT.id()).orElseThrow(), "broke at 120, + 1200");

        now.set(1319);
        assertTrue(book.patch(BEN, SLOT, SMALL, RULE).isEmpty(), "regrowing: nothing to show");
        assertEquals(ClickOutcome.NotLive.INSTANCE,
                book.click(BEN, SLOT, SMALL, RULE, 1200, hitAt(new Spot(40, 6), 1319)));

        now.set(1320);
        Patch fresh = book.patch(BEN, SLOT, SMALL, RULE).orElseThrow();
        assertEquals(List.of(new Spot(40, 6), new Spot(3, 25)), fresh.layout().spots(), "re-rolled");
        assertEquals(1320L, fresh.revealTick(0));
    }

    @Test
    void clearingAPlayersRegrowthLetsThePatchBackAtOnce() {
        MiningBook book = book(2, 10, 10, 30, 20, 2, 2, 2, 40, 6, 3, 25);
        Patch p = book.patch(BEN, SLOT, SMALL, RULE).orElseThrow();
        book.click(BEN, SLOT, SMALL, RULE, 1200, hitAt(p.layout().spots().get(0), 110));
        book.click(BEN, SLOT, SMALL, RULE, 1200, hitAt(p.layout().spots().get(1), 111));
        assertTrue(book.patch(BEN, SLOT, SMALL, RULE).isEmpty());
        book.clearRegrowth(BEN);
        assertTrue(book.patch(BEN, SLOT, SMALL, RULE).isPresent());
    }

    @Test
    void removingASlotForgetsEveryPlayersPatchAndClock() {
        MiningBook book = book(2, 10, 10, 30, 20, 2, 40, 6, 3, 25, 2, 5, 5, 20, 20);
        book.patch(BEN, SLOT, SMALL, RULE);
        book.patch(OTHER, SLOT, SMALL, RULE);
        book.clearSlot(SLOT.id());
        Patch again = book.patch(BEN, SLOT, SMALL, RULE).orElseThrow();
        assertEquals(List.of(new Spot(5, 5), new Spot(20, 20)), again.layout().spots(), "a fresh roll");
    }
}
