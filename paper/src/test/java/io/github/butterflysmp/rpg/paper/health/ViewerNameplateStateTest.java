package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The text-on-change / visibility-per-cycle decision. The load-bearing subtlety is that FIRST SIGHT
 * and VERSION CHANGE are distinct reasons to send the text -- both must fire, or a newly-arrived
 * viewer sees an undamaged mob with no nameplate. Each test names the mutation it forces red.
 */
class ViewerNameplateStateTest {

    private final UUID mob = UUID.randomUUID();

    @Test
    void firstSightSendsTheTextEvenForAnUndamagedMob() {
        var state = new ViewerNameplateState();
        // version 1 = never damaged; the viewer is seeing this mob for the first time.
        var decision = state.decide(mob, 1, true);

        assertTrue(decision.includeName(), "first sight sends the text, even though the version never changed");
        assertTrue(decision.visible(), "and asserts visibility from LOS");
        // Mutation: gate the name on version-change only -> a new viewer of an undamaged mob gets no
        //           nameplate (includeName false on first sight) -> reddens. This is confirmation #3.
    }

    @Test
    void aSteadyVersionSendsVisibilityOnly() {
        var state = new ViewerNameplateState();
        state.decide(mob, 1, true);                         // first sight sent the text
        var again = state.decide(mob, 1, true);            // same version next cycle

        assertFalse(again.includeName(), "unchanged version -> no re-encoded text, visibility only");
        assertTrue(again.visible(), "visibility still asserted each cycle");
        // Mutation: always includeName -> the Component is re-encoded every cycle -> the per-cycle
        //           optimization is gone (still 'correct' but the guard on the optimization reddens).
    }

    @Test
    void aVersionBumpResendsTheText() {
        var state = new ViewerNameplateState();
        state.decide(mob, 1, true);
        var bumped = state.decide(mob, 2, true);           // HP changed -> new version

        assertTrue(bumped.includeName(), "a version change resends the text");
        var settled = state.decide(mob, 2, true);
        assertFalse(settled.includeName(), "then settles back to visibility-only at the new version");
        // Mutation: gate the name on first-sight only -> a damaged mob's new HP never reaches the
        //           viewer who already saw it -> reddens.
    }

    @Test
    void visibilityFollowsLineOfSightEachCycle() {
        var state = new ViewerNameplateState();
        state.decide(mob, 1, true);
        var hidden = state.decide(mob, 1, false);          // viewer lost LOS

        assertFalse(hidden.visible(), "no LOS -> visible=false, re-asserted this cycle");
        assertFalse(hidden.includeName(), "and no text resend for a mere LOS change");
        // Mutation: hardcode visible=true -> a mob behind a wall stays shown -> reddens.
    }

    @Test
    void outOfRangeMobsArePrunedToBoundTheMap() {
        var state = new ViewerNameplateState();
        UUID other = UUID.randomUUID();
        state.decide(mob, 1, true);
        state.decide(other, 1, true);
        assertEquals(2, state.trackedCount(), "two mobs tracked");

        state.retainInRange(Set.of(mob));                  // `other` left range

        assertEquals(1, state.trackedCount(), "the departed mob was pruned");
        // And a mob that re-enters range is treated as first sight again (text resent):
        assertTrue(state.decide(other, 1, true).includeName(), "re-entering range is a fresh first sight");
        // Mutation: retainInRange no-ops -> the map grows unbounded with every mob ever seen -> reddens.
    }
}
