package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The anti-spam window and the charge stash, on a fake clock.
 *
 * This is the one piece of Stage 1 that is NOT in the reviewed plan: the plan's guard read
 * vanilla's noDamageTicks, and the 2026-08-28 boot disproved that -- FIRE_TICK and LAVA drive the
 * same counter, so a burning mob would have gone un-hittable for half of every second. These tests
 * pin the replacement, and the environmental case has a test of its own precisely because it is the
 * failure the original design would have shipped.
 *
 * No Bukkit here: MeleeHits takes a tick supplier, so the whole window is exercised on a fake clock
 * rather than being deferred to a boot the way the listener wiring must be.
 */
class MeleeHitsTest {

    private final long[] tick = {100L};
    private final MeleeHits hits = new MeleeHits(() -> tick[0]);

    private static final UUID ATTACKER = UUID.randomUUID();
    private static final UUID VICTIM = UUID.randomUUID();
    private static final UUID OTHER_VICTIM = UUID.randomUUID();

    // --- The window ---

    @Test
    void aVictimTakesOneCustomHitPerWindowHoweverFastTheClicksArrive() {
        assertTrue(hits.claimWindow(VICTIM), "the first hit lands");
        assertFalse(hits.claimWindow(VICTIM), "a same-tick re-hit does not");

        tick[0] += MeleeHits.WINDOW_TICKS - 1;
        assertFalse(hits.claimWindow(VICTIM), "nor does one a tick short of the window");

        tick[0] += 1;
        assertTrue(hits.claimWindow(VICTIM), "the window reopens exactly on time");
        // Mutation: make claimWindow always return true -> the escalation hole reopens and a rising
        // charge lands ~5 full hits inside one set of i-frames -> reddens on the second assertion.
    }

    /**
     * THE LAVA-BAT TEST. The window closes ONLY when we ourselves land a hit.
     *
     * A victim we have never hit is claimable no matter how much time has passed and no matter what
     * else has been damaging it. Environmental damage -- fire ticks on a burning zombie, lava, fall
     * -- drives vanilla's noDamageTicks, which is why the guard cannot read it: a mob set alight by
     * our own Scorch DoT would otherwise be un-hittable for half of every second.
     *
     * MeleeHits never sees noDamageTicks at all, and that absence is the fix. This test states it as
     * a property rather than a comment: nothing but our own claim can close the window.
     */
    @Test
    void onlyOurOwnHitsCloseTheWindowSoEnvironmentalDamageCannotSuppressASwing() {
        // Time passes -- during which, in the world, this mob is burning and taking a fire tick a
        // second, each one setting its noDamageTicks to 20. None of that reaches here, because there
        // is no API by which it could: MeleeHits is told about OUR hits and nothing else.
        tick[0] += 500;
        assertEquals(0, hits.trackedVictims(),
                "nothing but our own hit may ever put a victim in the map");

        // So the very first swing at a long-burning mob lands, where a noDamageTicks-based guard
        // would have refused it for half of every second.
        assertTrue(hits.claimWindow(VICTIM), "a mob we have not hit is hittable, however it is burning");

        // And our own hit is what closes it -- the anti-spam half still holds.
        assertFalse(hits.claimWindow(VICTIM), "our hit, and only our hit, closes the window");
        tick[0] += MeleeHits.WINDOW_TICKS;
        assertTrue(hits.claimWindow(VICTIM), "which reopens on our clock, not the world's");
        // Mutation: seed the map from anything the world writes, or make claimWindow consult a
        // victim's incoming damage -> the trackedVictims assertion fails -> reddens.
    }

    @Test
    void theWindowIsPerVictimAndNotGlobal() {
        assertTrue(hits.claimWindow(VICTIM));
        assertTrue(hits.claimWindow(OTHER_VICTIM),
                "hitting one mob must not lock out every other mob in the world");
        assertFalse(hits.claimWindow(VICTIM), "and each keeps its own window");
        // Mutation: key the tracker on a constant instead of the victim id -> the second assertion
        // fails and one swing locks the whole server out for 10 ticks -> reddens.
    }

    // --- The pending swing ---

    @Test
    void aSwingIsConsumedByTheAttemptSoAStaleOneCannotBeMisreadLater() {
        hits.record(ATTACKER, VICTIM, 0.75);

        assertTrue(hits.consume(ATTACKER, VICTIM).isPresent(), "the swing that landed");
        assertTrue(hits.consume(ATTACKER, VICTIM).isEmpty(), "and it is gone, not reusable");
        assertEquals(0, hits.pendingSwings());
        // Mutation: peek instead of remove -> the second assertion fails, and one stashed charge
        // would be reused by every later hit that found no swing of its own -> reddens.
    }

    @Test
    void aSwingAtADifferentVictimFailsClosedRatherThanGuessingACharge() {
        hits.record(ATTACKER, VICTIM, 1.0);

        assertTrue(hits.consume(ATTACKER, OTHER_VICTIM).isEmpty(),
                "a mismatched victim yields nothing -- the caller deals no damage rather than guess");
        assertTrue(hits.consume(ATTACKER, VICTIM).isEmpty(),
                "and the mismatch still consumed it, so it cannot go stale");
        // Mutation: drop the victim check -> the first assertion fails, and a swing aimed at one mob
        // would supply the charge for a hit on another -> reddens.
    }

    @Test
    void consumingWithNoSwingRecordedYieldsNothing() {
        assertTrue(hits.consume(ATTACKER, VICTIM).isEmpty());
    }

    @Test
    void theChargeSurvivesTheHandoffIntact() {
        hits.record(ATTACKER, VICTIM, 0.7600);
        assertEquals(0.7600, hits.consume(ATTACKER, VICTIM).orElseThrow().charge(), 1e-9,
                "the pre-attack read is what reaches the damage arm, unmodified");
    }

    @Test
    void aSecondSwingReplacesOneThatNeverLanded() {
        hits.record(ATTACKER, VICTIM, 0.2);
        hits.record(ATTACKER, VICTIM, 0.9);

        assertEquals(1, hits.pendingSwings(), "one attacker holds at most one pending swing");
        assertEquals(0.9, hits.consume(ATTACKER, VICTIM).orElseThrow().charge(), 1e-9,
                "the newer swing wins; the older one never landed");
    }

    // --- Bounds ---

    @Test
    void forgettingAVictimDropsItsWindowSoTheMapCannotGrowForever() {
        hits.claimWindow(VICTIM);
        hits.claimWindow(OTHER_VICTIM);
        assertEquals(2, hits.trackedVictims());

        hits.forget(VICTIM);
        assertEquals(1, hits.trackedVictims(), "death, despawn and chunk-unload all route here");
        assertTrue(hits.claimWindow(VICTIM), "and a forgotten victim starts clean");

        hits.forget(OTHER_VICTIM);
        hits.forget(VICTIM);
        assertEquals(0, hits.trackedVictims(), "nothing is left behind");
        // Mutation: make forget a no-op -> the map grows with every mob the server ever spawns ->
        // reddens. This is the leak check the plan asked for.
    }

    @Test
    void forgettingAnAttackerDropsAnUnlandedSwing() {
        hits.record(ATTACKER, VICTIM, 0.5);
        assertEquals(1, hits.pendingSwings());

        hits.forgetAttacker(ATTACKER);
        assertEquals(0, hits.pendingSwings(), "a player who logs out mid-swing leaves nothing behind");
    }

    @Test
    void forgettingSomethingUnknownIsSafe() {
        hits.forget(UUID.randomUUID());
        hits.forgetAttacker(UUID.randomUUID());
        assertEquals(0, hits.trackedVictims());
        assertEquals(0, hits.pendingSwings());
    }
}
