package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The damage cadence this project owns because vanilla can no longer keep it.
 *
 * <p>Pure and clock-driven, so the whole of it is reachable without a server — which is the reason
 * the cadence lives in {@code core} rather than in the rider that calls it.
 *
 * <p>Each test names the mutation it forces red.
 */
class DamageWindowTest {

    private static final double EPS = 1e-9;

    /** Starts at 100, not 0, so nothing can pass on a zero-valued stamp. {@code MeleeHitsTest}'s convention. */
    private final long[] tick = {100L};
    private final DamageWindow window = new DamageWindow(() -> tick[0]);

    private static final UUID VICTIM = UUID.randomUUID();
    private static final UUID OTHER_VICTIM = UUID.randomUUID();

    // --- The ratchet ------------------------------------------------------------------------------

    @Test
    void theFirstHitLandsWholeAndOpensTheWindow() {
        assertEquals(4.0, window.claim(VICTIM, 4.0, false), EPS, "nothing is open, so it all lands");
        // Mutation M6: WINDOW_TICKS = 0 -> the window is never live -> the absorb rows below redden.
    }

    @Test
    void aSecondHitOfTheSameSizeInsideTheWindowIsABSORBED() {
        window.claim(VICTIM, 4.0, false);
        assertEquals(0.0, window.claim(VICTIM, 4.0, false), EPS,
                "vanilla ignores a re-hit that is no bigger than what already landed, and so do we");
        // Mutation M1: return the full amount on the ratchet arm -> reddens with 4.0.
    }

    @Test
    void aLARGERHitInsideTheWindowDealsTheDIFFERENCEAndNotTheWhole() {
        assertEquals(4.0, window.claim(VICTIM, 4.0, false), EPS);
        assertEquals(6.0, window.claim(VICTIM, 10.0, false), EPS,
                "10 arriving over a landed 4 tops up by 6 -- vanilla's amount > lastHurt arm, which "
                        + "applies the excess rather than the whole hit or nothing at all");
        // Mutation M2: return 0.0 instead of the difference -> a bigger second hit deals nothing -> reddens.
    }

    @Test
    void aTopUpRAISESTheBarSoTheNextEqualHitIsAbsorbed() {
        window.claim(VICTIM, 4.0, false);
        window.claim(VICTIM, 10.0, false);
        assertEquals(0.0, window.claim(VICTIM, 10.0, false), EPS,
                "the top-up moved the mark to 10, so a third hit of 10 has nothing left to add");
        assertEquals(2.0, window.claim(VICTIM, 12.0, false), EPS, "and 12 tops up by 2, not by 8");
        // Mutation M4: don't update appliedThisWindow -> the mark stays at 4 -> unbounded top-ups -> reddens.
    }

    @Test
    void aTopUpDoesNOTBuyMoreWINDOW() {
        // THE OBVIOUS IMPLEMENTATION REFRESHES THE EXPIRY ON EVERY CLAIM, AND IT IS WRONG.
        // Vanilla's in-window branch writes lastHurt and jumps to 336 with NO putfield
        // invulnerableTime; only the normal path re-arms, at 319. A window extended by top-ups never
        // closes under a rising stream.
        window.claim(VICTIM, 4.0, false);            // opens at tick 100, closing at 110
        tick[0] += DamageWindow.WINDOW_TICKS - 1;    // 109: still inside
        assertEquals(6.0, window.claim(VICTIM, 10.0, false), EPS, "a top-up at 109, inside the window");

        tick[0] += 1;                                // 110: the ORIGINAL expiry, not 109 + 10
        assertEquals(3.0, window.claim(VICTIM, 3.0, false), EPS,
                "the window closed on schedule, so a small hit opens a fresh one and lands whole");
        // Mutation M8: carry `now + WINDOW_TICKS` on the ratchet arm instead of live.expiresAtTick()
        // -> the window is still live at 110 and 3.0 is absorbed -> reddens with 0.0.
    }

    // --- The clock --------------------------------------------------------------------------------

    @Test
    void theWindowReopensEXACTLYOnTime() {
        window.claim(VICTIM, 4.0, false);

        tick[0] += DamageWindow.WINDOW_TICKS - 1;
        assertEquals(0.0, window.claim(VICTIM, 4.0, false), EPS, "a tick short of the window, still closed");

        tick[0] += 1;
        assertEquals(4.0, window.claim(VICTIM, 4.0, false), EPS, "and it reopens exactly on time");
        // Mutation M7: never expire (drop the `now >= expiresAtTick` test) -> nothing ever lands again -> reddens.
    }

    // --- Per victim, not per cause ----------------------------------------------------------------

    @Test
    void theWindowIsPerVICTIMSoTwoCausesShareOne() {
        // Lava and its fire tick arrive on the SAME victim in the SAME tick -- measured in the
        // 2026-09-06 capture, tick 191129. Vanilla has ONE counter per entity, so the fire tick is
        // inside lava's window and lands nothing. A per-CAUSE window would let both through.
        assertEquals(4.0, window.claim(VICTIM, 4.0, false), EPS, "the lava tick");
        assertEquals(0.0, window.claim(VICTIM, 1.0, false), EPS,
                "the fire tick, same victim, same tick: smaller than what landed, so absorbed");
        // Mutation M3: key the map on cause instead of victim -> both land -> reddens.
    }

    @Test
    void oneVictimsWindowDoesNotCloseANOTHERS() {
        window.claim(VICTIM, 4.0, false);
        assertEquals(4.0, window.claim(OTHER_VICTIM, 4.0, false), EPS,
                "a second victim in the same lava has its own cadence");
        // Mutation M3 again, from the other side: a single shared window -> the second victim is absorbed.
    }

    @Test
    void eightClaimsInONETickYieldOneLandingAndSEVENZeros() {
        // THE GOLEM, FROM THE WIRE. Grouping the capture by (tick, victim uuid) found 15 groups of
        // EIGHT LAVA events for one iron golem in a single tick -- contact is per lava block, and a
        // 1.4x2.7 hitbox occupies several where a player's 0.6x1.8 occupies one. 8 x 4 raw per tick
        // against 100 custom HP is 3.1 ticks, and the golem died in 2-3.
        assertEquals(4.0, window.claim(VICTIM, 4.0, false), EPS, "the first block's contact lands");
        for (int i = 2; i <= 8; i++) {
            assertEquals(0.0, window.claim(VICTIM, 4.0, false), EPS,
                    "contact " + i + " of 8, same tick, same victim: absorbed");
        }
        // Mutation M1 or M6 -> the seven zeros become 4.0 each -> reddens, naming the contact.
    }

    // --- The bypass arm ---------------------------------------------------------------------------

    @Test
    void aBypassingSourceIsNotRatcheted() {
        window.claim(VICTIM, 4.0, false);
        assertEquals(9.0, window.claim(VICTIM, 9.0, true), EPS,
                "vanilla's offset 152 jumps past the ratchet entirely, so a bypassing source deals its "
                        + "whole amount even inside a live window");
        // Mutation M5: ignore the bypassesCooldown parameter -> 9.0 becomes the 5.0 difference -> reddens.
    }

    @Test
    void aBypassingSourceLEAVESTHEWINDOWUNTOUCHED() {
        // THE ARM HAS TWO PROPERTIES AND THE ROW ABOVE ONLY COVERS ONE. A bypass that also wrote into
        // the window would return the right number -- so the row above stays GREEN -- while every
        // ordinary claim after it is ratcheted against a value a bypassing source had no business
        // setting. Vanilla's bypass touches neither lastHurt nor invulnerableTime.
        assertEquals(9.0, window.claim(VICTIM, 9.0, true), EPS, "a bypass on a victim with no window");
        assertEquals(0, window.trackedVictims(), "and it opened nothing");
        assertEquals(4.0, window.claim(VICTIM, 4.0, false), EPS,
                "so an ordinary hit after it opens CLEAN and lands whole, rather than being ratcheted "
                        + "against the 9 that bypassed");
        // Mutation M9: make the BYPASS arm open/update the window like the other arms -> this row
        // reddens (0.0, and trackedVictims 1) while aBypassingSourceIsNotRatcheted stays green.
    }

    // --- Guards -----------------------------------------------------------------------------------

    @Test
    void aZeroDamageEventOpensNOTHING() {
        // Mirrors vanilla's own early return at offsets 283-308: a zero-damage event returns before
        // lastHurt and invulnerableTime are written. Were it to open a window here, a 0.0 event would
        // absorb the real hit behind it.
        assertEquals(0.0, window.claim(VICTIM, 0.0, false), EPS, "nothing to deal");
        assertEquals(0, window.trackedVictims(), "and nothing opened");
        assertEquals(4.0, window.claim(VICTIM, 4.0, false), EPS, "so the real hit behind it lands whole");
        // Mutation: drop the `amount <= 0` guard -> a 0.0 claim opens a window with applied=0.0, and
        // 4.0 then tops up by 4.0 -- which happens to be the same number. The trackedVictims row is
        // what actually reddens, which is why it is asserted rather than the amount alone.
    }

    // --- Bounds and lifecycle ---------------------------------------------------------------------

    @Test
    void forgettingReopensTheWindowImmediately() {
        // THE RESPAWN CASE, AND IT IS A CORRECTNESS FIX RATHER THAN A LEAK FIX. Quit handling does not
        // run on death and entity-removal filters players out, so a player who dies mid-window
        // respawns still holding it -- and respawning into lava or a wall is exactly when
        // environmental damage arrives.
        window.claim(VICTIM, 4.0, false);
        assertEquals(0.0, window.claim(VICTIM, 4.0, false), EPS, "absorbed while the window stands");

        window.forget(VICTIM);
        assertEquals(4.0, window.claim(VICTIM, 4.0, false), EPS,
                "and the full amount once it is dropped -- a respawned player is not still absorbing");
        // Mutation: make forget a no-op -> the respawned victim keeps absorbing -> reddens.
    }

    @Test
    void forgettingSomethingUnknownIsSafe() {
        window.forget(UUID.randomUUID());
        assertEquals(0, window.trackedVictims(), "and leaves nothing behind");
    }

    @Test
    void theMapIsBoundedByForget() {
        window.claim(VICTIM, 4.0, false);
        window.claim(OTHER_VICTIM, 4.0, false);
        assertEquals(2, window.trackedVictims(), "two victims, two windows");

        window.forget(VICTIM);
        window.forget(OTHER_VICTIM);
        assertEquals(0, window.trackedVictims(),
                "a per-victim map with no removal hook grows for the lifetime of the server");
        // Mutation: make forget a no-op -> reddens at 2.
    }

    @Test
    void theWindowConstantMatchesTheMeasuredGatedRegion() {
        // Not testing our arithmetic: holding open the REASON the number is 10. invulnerableTime counts
        // down from 20 and hurtServer takes the in-window branch only while it is above
        // invulnerableDuration/2 -- the 2026-09-06 capture shows the counter ratcheting at 19..11 and
        // the normal path re-arming at exactly 10. If a retune ever makes this a different number,
        // this row is the note explaining what it was matched to.
        assertEquals(10, DamageWindow.WINDOW_TICKS, "the measured gated region, not a taste");
        assertNotEquals(20, DamageWindow.WINDOW_TICKS,
                "and NOT invulnerableDuration itself -- the gate is duration/2, which is the mistake "
                        + "this row exists to stop");
        assertTrue(DamageWindow.WINDOW_TICKS > 0, "zero would mean no cadence at all");
        // No mutation: this asserts a constant against a measurement, not code against itself.
    }
}
