package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F9e c to e: THE REVIVE (Ben: "Another player has to hold shift/crouch on top of that players copy for 3 seconds to
 * respawn them"; "2. 50% health"; "3. No" (damage is not a break); "4. No but we will add modifiers to change that
 * later"). Two and three players.
 */
class ReviveTrackerTest {

    private static final UUID COPY = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID COPY2 = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private static ReviveTracker.Attempt at(UUID who, double distance, boolean sneaking) {
        return new ReviveTracker.Attempt(who, distance, sneaking);
    }

    /** 60 ticks (3 seconds) in the radius, sneaking, without a break: done on the 60th, not the 59th. */
    @Test
    void sixtyTicksSneakingInTheRadiusRevive() {
        ReviveTracker t = new ReviveTracker(60, 1.5);
        for (int i = 1; i < 60; i++) {
            ReviveTracker.Result r = t.tick(COPY, List.of(at(A, 1.0, true)));
            assertEquals(ReviveTracker.Outcome.PROGRESS, r.outcome(), "tick " + i);
            assertEquals(i, r.progressTicks(A));
        }
        ReviveTracker.Result done = t.tick(COPY, List.of(at(A, 1.0, true)));
        assertEquals(ReviveTracker.Outcome.DONE, done.outcome());
        assertEquals(A, done.reviver());
    }

    @Test
    void theRadiusIsInclusiveAndAHairOutsideIsABreak() {
        ReviveTracker t = new ReviveTracker(60, 1.5);
        assertEquals(1, t.tick(COPY, List.of(at(A, 1.5, true))).progressTicks(A), "on the radius counts");
        ReviveTracker.Result r = t.tick(COPY, List.of(at(A, 1.5001, true)));
        assertEquals(0, r.progressTicks(A), "outside the radius: reset");
        assertEquals(List.of(A), r.broken());
    }

    /** Every break resets to zero: he stops sneaking, he leaves the radius, he goes down (he is no longer offered). */
    @Test
    void eachBreakResetsTheProgress() {
        ReviveTracker t = new ReviveTracker(60, 1.5);
        for (int i = 0; i < 30; i++) t.tick(COPY, List.of(at(A, 1.0, true)));
        ReviveTracker.Result stop = t.tick(COPY, List.of(at(A, 1.0, false)));
        assertEquals(List.of(A), stop.broken(), "stopped sneaking");
        assertEquals(0, stop.progressTicks(A));
        for (int i = 0; i < 30; i++) t.tick(COPY, List.of(at(A, 1.0, true)));
        ReviveTracker.Result away = t.tick(COPY, List.of(at(A, 3.0, true)));
        assertEquals(List.of(A), away.broken(), "left the radius (or the copy moved out of it)");
        for (int i = 0; i < 30; i++) t.tick(COPY, List.of(at(A, 1.0, true)));
        ReviveTracker.Result gone = t.tick(COPY, List.of());
        assertEquals(List.of(A), gone.broken(), "went down himself (no longer a participant, so not offered)");
        assertEquals(0, gone.progressTicks(A));
        assertEquals(ReviveTracker.Outcome.NONE, gone.outcome());
    }

    /** Ben: "No". A hit is not an input at all: progress continues across any number of hits. */
    @Test
    void damageIsNotABreak() {
        ReviveTracker t = new ReviveTracker(60, 1.5);
        for (int i = 0; i < 59; i++) t.tick(COPY, List.of(at(A, 1.0, true))); // he is being hit throughout
        assertEquals(ReviveTracker.Outcome.DONE, t.tick(COPY, List.of(at(A, 1.0, true))).outcome());
    }

    /** Two revivers do not stack; the first to finish revives. Three players: two revivers, one copy. */
    @Test
    void twoReviversDoNotStackAndTheFirstToFinishRevives() {
        ReviveTracker t = new ReviveTracker(60, 1.5);
        for (int i = 0; i < 20; i++) t.tick(COPY, List.of(at(A, 1.0, true)));
        ReviveTracker.Result both = null;
        for (int i = 0; i < 39; i++) both = t.tick(COPY, List.of(at(A, 1.0, true), at(B, 1.0, true)));
        assertEquals(59, both.progressTicks(A));
        assertEquals(39, both.progressTicks(B), "B's own count, not A's plus B's");
        ReviveTracker.Result done = t.tick(COPY, List.of(at(A, 1.0, true), at(B, 1.0, true)));
        assertEquals(A, done.reviver(), "A started first and finishes first");
        assertEquals(ReviveTracker.Outcome.DONE, done.outcome());
    }

    @Test
    void twoReviversFinishingOnOneTickGoToTheFirstOffered() {
        ReviveTracker t = new ReviveTracker(3, 1.5);
        t.tick(COPY, List.of(at(B, 1.0, true), at(A, 1.0, true)));
        t.tick(COPY, List.of(at(B, 1.0, true), at(A, 1.0, true)));
        assertEquals(B, t.tick(COPY, List.of(at(B, 1.0, true), at(A, 1.0, true))).reviver());
    }

    @Test
    void eachCopyHasItsOwnProgressAndAForgottenCopyStartsOver() {
        ReviveTracker t = new ReviveTracker(60, 1.5);
        for (int i = 0; i < 10; i++) t.tick(COPY, List.of(at(A, 1.0, true)));
        assertEquals(1, t.tick(COPY2, List.of(at(A, 1.0, true))).progressTicks(A), "a second copy counts its own");
        t.forget(COPY);
        assertEquals(1, t.tick(COPY, List.of(at(A, 1.0, true))).progressTicks(A));
    }

    @Test
    void theRulesRefuseNonsense() {
        assertThrows(IllegalArgumentException.class, () -> new ReviveTracker(0, 1.5));
        assertThrows(IllegalArgumentException.class, () -> new ReviveTracker(60, 0.0));
    }

    /** e: ONE rule decides whether he may be revived again; unset is unlimited (Ben: "No"). */
    @Test
    void theLimitIsOneRuleAndUnsetIsUnlimited() {
        assertTrue(ReviveRules.mayRevive(0, OptionalInt.empty()));
        assertTrue(ReviveRules.mayRevive(1000, OptionalInt.empty()), "no limit");
        assertTrue(ReviveRules.mayRevive(1, OptionalInt.of(2)));
        assertFalse(ReviveRules.mayRevive(2, OptionalInt.of(2)), "the count has reached the limit");
        assertFalse(ReviveRules.mayRevive(0, OptionalInt.of(0)), "a limit of 0 forbids every revive");
    }

    /** d: Ben's "50% health", of his maximum. */
    @Test
    void theRevivedHealthIsTheFractionOfHisMaximum() {
        assertEquals(175.0, ReviveRules.revivedHealth(350.0, 0.5), 1e-9);
        assertEquals(100.0, ReviveRules.revivedHealth(100.0, 1.0), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> ReviveRules.revivedHealth(100.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> ReviveRules.revivedHealth(100.0, 1.5));
    }
}
