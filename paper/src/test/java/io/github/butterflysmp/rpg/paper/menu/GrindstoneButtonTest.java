package io.github.butterflysmp.rpg.paper.menu;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The strip button's state machine: the precedence, the palette, and the two grays.
 *
 * <p>This is the unit twin of {@code GATE-nexus.md} slice 6 rows 41 and 46 -- the boot rows read it
 * on a real screen, and this reads it in two seconds.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class GrindstoneButtonTest {

    @Test
    void anEmptyTrayIsGRAYWhateverTheClockSays_thePRECEDENCERuling() {
        // ROW 41's unit twin. Open the menu and click confirm IMMEDIATELY: the deadline initialises
        // at open time, so the clock is running -- and the button must STILL be gray, because a
        // lock on an action with nothing to lose is not information the player needs.
        GrindstoneButton.Face face =
                GrindstoneButton.faceFor(true, 0, 0, GrindstoneButton.ARM_TICKS);

        assertEquals(GrindstoneButton.State.NOTHING_EMPTY, face.state(),
                "an empty tray outranks the arming clock");
        assertEquals(Material.GRAY_DYE, face.material(), "gray, not yellow");
        assertNotEquals(GrindstoneButton.State.ARMING, face.state(),
                "the clock must not win here -- that is the whole ruling");
        // Mutation: test remainingTicks before strippable -> ARMING -> reddens.
    }

    @Test
    void theTWOGRAYStatesSayDIFFERENTThings_andTHATISRow28sRule() {
        // *** ROW 46's UNIT TWIN, AND THE ROW THAT CATCHES THE COLLAPSE. ***
        //
        // Both are gray because both are player-resolvable. The REMEDIES differ -- "put something
        // in" against "these are the wrong items" -- so the TEXT must differ. If someone later
        // folds these into one string, every other assertion in this file still passes: they have
        // the same colour, the same acts() answer, and neither is READY.
        GrindstoneButton.Face empty = GrindstoneButton.faceFor(true, 0, 0, 0);
        GrindstoneButton.Face stripped = GrindstoneButton.faceFor(false, 0, 0, 0);

        assertEquals(Material.GRAY_DYE, empty.material(), "both are gray");
        assertEquals(Material.GRAY_DYE, stripped.material(), "both are gray");

        assertNotEquals(empty.state(), stripped.state(), "but they are DIFFERENT STATES");
        assertNotEquals(empty.text(), stripped.text(),
                "and they must say different things -- collapsing them is GATE-nexus Row 28's "
                        + "defect, where one colour served a pending load and a failed one");

        assertEquals("Add items to strip", empty.text(), "the empty tray names the remedy");
        assertEquals("These have nothing to strip", stripped.text(),
                "and the stripped tray names a different one");
        // Mutation: return the same text from both arms -> the two assertNotEquals redden.
    }

    @Test
    void thePALETTEIsARule_yellowResolvesByWaitingAndGrayOnlyByActing() {
        assertEquals(Material.LIME_DYE,
                GrindstoneButton.faceFor(false, 3, 999, 0).material(), "ready is LIME");
        assertEquals(Material.YELLOW_DYE,
                GrindstoneButton.faceFor(false, 3, 999, 40).material(),
                "arming is YELLOW -- it resolves itself if you wait");
        assertEquals(Material.GRAY_DYE,
                GrindstoneButton.faceFor(false, 0, 0, 0).material(),
                "nothing-to-strip is GRAY -- it resolves only if you act");

        // AND THE THREE ARE DISTINCT, which is the half that would otherwise be vacuous: three
        // arms all returning one colour satisfies every assertion that names a colour once.
        assertNotEquals(GrindstoneButton.faceFor(false, 3, 999, 0).material(),
                GrindstoneButton.faceFor(false, 3, 999, 40).material(), "lime is not yellow");
        assertNotEquals(GrindstoneButton.faceFor(false, 3, 999, 40).material(),
                GrindstoneButton.faceFor(false, 0, 0, 0).material(), "yellow is not gray");
        // Mutation: YELLOW_DYE -> GRAY_DYE -> the palette row and a distinctness row redden.
    }

    @Test
    void theCountOnTheButtonIsTheItemsThatCHANGE_notTheTraySize() {
        GrindstoneButton.Face face = GrindstoneButton.faceFor(false, 9, 1463, 0);
        assertEquals("Strip 9 items -- +1463 XP", face.text(),
                "nine of a fourteen-item tray -- printing 14 invites dividing 1463 by 14");

        // AND ONE ITEM IS NOT "1 items".
        assertEquals("Strip 1 item -- +123 XP", GrindstoneButton.faceFor(false, 1, 123, 0).text(),
                "singular when there is one");
        // Mutation: drop the plural arm -> the second reddens.
    }

    @Test
    void theCountdownRoundsUP_soALockedButtonNeverReadsZero() {
        // A zero on a button that is not yet armed is the one number that would be a lie.
        assertEquals(3, GrindstoneButton.secondsRemaining(60), "three seconds exactly");
        assertEquals(3, GrindstoneButton.secondsRemaining(50), "2.5s still reads 3");
        assertEquals(2, GrindstoneButton.secondsRemaining(40));
        assertEquals(2, GrindstoneButton.secondsRemaining(30), "1.5s still reads 2");
        assertEquals(1, GrindstoneButton.secondsRemaining(20));
        assertEquals(1, GrindstoneButton.secondsRemaining(10), "half a second still reads 1");
        assertEquals(0, GrindstoneButton.secondsRemaining(0), "and armed reads 0");
        // Mutation: integer-divide without the +19 -> 50 reads 2 and 10 reads 0 -> reddens.
    }

    @Test
    void theRenderedTextChangesEXACTLYThreeTimesOverTheCountdown() {
        // THE REASON THE TICK CAN SKIP MOST OF ITS WRITES. At a 10-tick period the countdown fires
        // six times, and the text takes only three distinct values -- so half the writes are
        // redundant and the menu is not repainting a static icon under the player's clicks.
        String last = null;
        int writes = 0;
        for (int ticks = GrindstoneButton.ARM_TICKS; ticks > 0;
                ticks -= GrindstoneButton.PERIOD_TICKS) {
            String text = GrindstoneButton.faceFor(false, 2, 500, ticks).text();
            if (!text.equals(last)) { writes++; last = text; }
        }
        assertEquals(3, writes, "3, 2, 1 -- six fires, three writes");
        assertEquals(6, GrindstoneButton.ARM_TICKS / GrindstoneButton.PERIOD_TICKS,
                "and six is the number of fires, so the saving is real rather than assumed");
    }

    @Test
    void ONLYLimeActs_andEveryOtherClickIsASilentNoOp() {
        assertTrue(GrindstoneButton.acts(GrindstoneButton.faceFor(false, 3, 999, 0)), "READY acts");
        assertFalse(GrindstoneButton.acts(GrindstoneButton.faceFor(false, 3, 999, 40)),
                "a click during the lockout does NOTHING -- the countdown is the feedback");
        assertFalse(GrindstoneButton.acts(GrindstoneButton.faceFor(true, 0, 0, 0)),
                "and an empty tray does nothing");
        assertFalse(GrindstoneButton.acts(GrindstoneButton.faceFor(false, 0, 0, 0)),
                "and neither does a stripped one");
        // Mutation: acts() returns true for ARMING -> the lockout row reddens. THAT MUTATION IS
        // THE BUG THE ARMING DELAY EXISTS TO PREVENT.
    }

    @Test
    void aRefundOfZeroCanNEVERReachTheREADYFace() {
        // THE SEAM BETWEEN THE TWO MODELS, ASSERTED. GrindstoneRefund.strippableCount is non-zero
        // exactly when the refund is non-zero -- the cheapest rung costs 352 -- so a LIME button
        // advertising "+0 XP" is unreachable. If that ever changes, this is where it shows.
        for (int ticks = 0; ticks <= GrindstoneButton.ARM_TICKS; ticks += 10) {
            assertNotEquals(GrindstoneButton.State.READY,
                    GrindstoneButton.faceFor(false, 0, 0, ticks).state(),
                    "nothing strippable must never render as ready, at any point on the clock");
        }
    }
}
