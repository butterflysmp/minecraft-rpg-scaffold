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

    // --- The knockback signal: "a hit landed HERE, NOW" ---
    //
    // Vanilla's melee knockback is no longer cancelled outright; it is released on exactly the hit
    // that claimed the window, so the push keeps the same once-per-window cadence as the damage.
    // These pin the signal the gate reads. The distinction every one of them turns on is
    // LANDED-THIS-TICK vs WINDOW-IS-OPEN: a mob hit three ticks ago still has an open window, and
    // reading that would leak a push to the very spam-click the gate exists to refuse.

    @Test
    void aClaimedHitSignalsOnItsOwnTickAndNotOnTheNext() {
        assertTrue(hits.claimWindow(VICTIM));
        assertTrue(hits.landedThisTick(VICTIM), "the hit that just claimed the window earned its push");

        tick[0] += 1;
        assertFalse(hits.landedThisTick(VICTIM), "and one tick later it has not");
        // Mutation: read ticksRemaining > 0 -- "the window is open" instead of "a hit landed this
        // tick" -> the second assertion fails, and every knockback event for the next nine ticks
        // would be let through -> reddens.
    }

    /**
     * THE SPAM-CLICK. The case the gate exists for, and the case a window-open reading gets wrong.
     *
     * Three ticks after a real hit the window is still shut, so the click deals nothing -- and it
     * must knock nothing either, or a spammer shoves a mob around on clicks worth zero damage.
     */
    @Test
    void aWindowedOutRehitEarnsNeitherDamageNorAPush() {
        assertTrue(hits.claimWindow(VICTIM), "the real hit");

        tick[0] += 3;
        assertFalse(hits.claimWindow(VICTIM), "the spam-click is refused its damage");
        assertFalse(hits.landedThisTick(VICTIM), "and refused its knockback on the same cadence");
        // Mutation: read ticksRemaining > 0 -> the last assertion fails. This is the one that proves
        // the signal is tick-exact rather than window-shaped -> reddens.
    }

    @Test
    void aVictimWeHaveNeverHitNeverSignals() {
        assertFalse(hits.landedThisTick(VICTIM), "no hit, no push");

        tick[0] += 500;
        assertFalse(hits.landedThisTick(VICTIM), "and time passing cannot manufacture one");
        // Mutation: test for ticksRemaining != 0, or drop the WINDOW_TICKS comparison -> an unhit
        // mob reads as freshly hit and vanilla knockback leaks on any ENTITY_ATTACK -> reddens.
    }

    @Test
    void theSignalIsPerVictimAndNotGlobal() {
        assertTrue(hits.claimWindow(VICTIM));

        assertTrue(hits.landedThisTick(VICTIM));
        assertFalse(hits.landedThisTick(OTHER_VICTIM),
                "hitting one mob must not release a push on every other mob being hit this tick");
        // Mutation: key on a constant instead of the victim id -> the second assertion fails ->
        // reddens.
    }

    /**
     * Asking must not consume. One attack can raise more than one knockback event -- Paper's
     * EntityPushedByEntityAttackEvent says so outright ("multiple acceleration calculations") -- and
     * a one-shot signal would cancel the second one, silently eating the sprint bonus that is the
     * whole reason vanilla owns this push.
     */
    @Test
    void askingDoesNotConsumeTheSignalSoOneHitCanReleaseSeveralKnockbacks() {
        assertTrue(hits.claimWindow(VICTIM));

        assertTrue(hits.landedThisTick(VICTIM), "the base push");
        assertTrue(hits.landedThisTick(VICTIM), "the sprint bonus, same tick, same hit");
        assertTrue(hits.landedThisTick(VICTIM), "and any further acceleration vanilla computes");
        // Mutation: clear the stamp on read -> the second assertion fails, and a sprint hit would
        // land its base push with no bonus -> reddens.
    }

    @Test
    void theWindowReopeningIsNotTheSameAsAHitLanding() {
        assertTrue(hits.claimWindow(VICTIM));

        tick[0] += MeleeHits.WINDOW_TICKS;
        assertFalse(hits.landedThisTick(VICTIM),
                "the window is claimable again, but nothing has been hit on this tick");
        assertTrue(hits.claimWindow(VICTIM), "claiming it is what makes the hit real");
        assertTrue(hits.landedThisTick(VICTIM), "and only then is a push earned");
        // Mutation: signal on readiness rather than on the claim -> the first assertion fails, and a
        // swing that never landed would still push -> reddens.
    }

    @Test
    void forgettingAVictimDropsItsSignalAlongWithItsWindow() {
        assertTrue(hits.claimWindow(VICTIM));
        assertTrue(hits.landedThisTick(VICTIM));

        hits.forget(VICTIM);
        assertFalse(hits.landedThisTick(VICTIM), "a forgotten victim carries no stale push");
        assertEquals(0, hits.trackedVictims(), "and the signal leaves nothing of its own behind");
        // The signal is DERIVED from the window rather than stored beside it, so this is what says
        // the derivation inherits the cleanup -- there is no second map for forget to miss.
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

    // --- The primary-damage stash, which the sweep rider reads ---

    @Test
    void thePrimaryDamageIsReadableOnTheTickItWasDealt() {
        hits.recordPrimaryDamage(ATTACKER, 14.2);

        assertEquals(14.2, hits.primaryDamageThisTick(ATTACKER).orElseThrow(), 1e-9);
        // Mutation: stamp with tick+1 -> the sweep events, which fire on this very tick, read empty
        // and no weapon in the game ever sweeps -> reddens.
    }

    /**
     * THE ONE THAT MATTERS: reading does not consume.
     *
     * One sweeping swing raises one damage event per swept mob, and every one of them asks this same
     * question. A consume-on-read would serve the first bystander and silently leave the rest
     * untouched -- a sweep that hits exactly one mob, which is indistinguishable from a sweep that
     * works if you only ever test with two mobs standing together.
     *
     * The direct analog of theKnockbackSignalIsNotConsumedByReadingIt above, and the same lesson the
     * 2026-08-28 boot taught about a sprint hit's two knockback events.
     */
    @Test
    void thePrimaryDamageIsNotConsumedByReadingItSoEverySweptMobSeesIt() {
        hits.recordPrimaryDamage(ATTACKER, 14.2);

        assertEquals(14.2, hits.primaryDamageThisTick(ATTACKER).orElseThrow(), 1e-9, "the first swept mob");
        assertEquals(14.2, hits.primaryDamageThisTick(ATTACKER).orElseThrow(), 1e-9, "the second");
        assertEquals(14.2, hits.primaryDamageThisTick(ATTACKER).orElseThrow(), 1e-9, "and the third");
        // Mutation: remove() on read instead of get() -> only the first swept mob takes damage and
        // the rest are silently skipped -> reddens on the second assertion.
    }

    @Test
    void aStashFromAnEarlierTickBelongsToADifferentSwingAndIsRefused() {
        hits.recordPrimaryDamage(ATTACKER, 14.2);

        tick[0] += 1;
        assertTrue(hits.primaryDamageThisTick(ATTACKER).isEmpty(),
                "one tick later is already a different swing");
        tick[0] += 100;
        assertTrue(hits.primaryDamageThisTick(ATTACKER).isEmpty(), "and it never comes back");
        // Mutation: drop the tick comparison -> a sweep event from a LATER swing reads the previous
        // swing's number, so a sword that swung hard once sweeps for that figure forever -> reddens.
    }

    @Test
    void anAttackerWhoDealtNothingHasNothingStashed() {
        assertTrue(hits.primaryDamageThisTick(ATTACKER).isEmpty(),
                "nothing recorded means nothing to sweep with");
        // Mutation: return OptionalDouble.of(0.0) rather than empty -> the rider stops failing closed
        // and starts tokening bystanders for a swing that never landed -> reddens.
    }

    @Test
    void aFreshSwingOverwritesTheLastOnesNumber() {
        hits.recordPrimaryDamage(ATTACKER, 14.2);
        tick[0] += MeleeHits.WINDOW_TICKS;
        hits.recordPrimaryDamage(ATTACKER, 3.2);          // a poorly timed follow-up

        assertEquals(3.2, hits.primaryDamageThisTick(ATTACKER).orElseThrow(), 1e-9,
                "this swing's number, not the last one's");
        // Mutation: putIfAbsent instead of put -> a weak swing sweeps for the strong swing's figure
        // -> reddens.
    }

    @Test
    void oneAttackersStashIsNotAnothers() {
        UUID other = UUID.randomUUID();
        hits.recordPrimaryDamage(ATTACKER, 14.2);

        assertTrue(hits.primaryDamageThisTick(other).isEmpty(),
                "a second player's sweep must not ride the first player's hit");
        // Mutation: key the map on anything but the attacker -> in co-op one player's swing arms the
        // other's sweep -> reddens.
    }

    @Test
    void forgettingAnAttackerDropsTheirStashedPrimaryHitToo() {
        hits.recordPrimaryDamage(ATTACKER, 14.2);
        assertEquals(1, hits.stashedPrimaries());

        hits.forgetAttacker(ATTACKER);

        assertEquals(0, hits.stashedPrimaries(), "nothing left behind on quit");
        assertTrue(hits.primaryDamageThisTick(ATTACKER).isEmpty());
        // Mutation: drop primaries.remove from forgetAttacker -> an entry per player who ever swung
        // sits there until restart -> reddens on the leak check.
    }
}
