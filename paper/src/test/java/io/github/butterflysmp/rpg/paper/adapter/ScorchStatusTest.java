package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.Scorch;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scorch is the first status whose EFFECT IS DAMAGE ON A CLOCK, so the hazards that come due here are
 * timing hazards rather than the attribute-cleanup ones Soaked brought.
 *
 * <p>Two rows exist because nothing else in the project could catch what they catch, and both are
 * TOTAL silent failures rather than wrong numbers:
 *
 * <ul>
 *   <li>{@link #reApplyingFasterThanThePeriodStillTicks} -- restart the task on refresh and a weapon
 *       hitting every 15 ticks re-phases a 20-tick clock forever. Scorch never ticks. Stacks climb,
 *       the burn shows, no damage is ever dealt.
 *   <li>{@link #theEightSecondStatusLivesEXACTLYEightSeconds} -- inherit SoakedStatus's
 *       check-then-decrement ordering and the status lives 9.0 seconds instead of 8.0, while
 *       {@link #eightDamageTicksForTheEightSecondDefault} stays GREEN at eight ticks either way.
 * </ul>
 *
 * Each test names the mutation it forces red.
 */
class ScorchStatusTest {

    private static final double EPS = 1e-9;

    /** 100 max, 20 cap -- the default shape, where 5% is 5 and the cap does NOT bind. */
    private static FakeScorchSink sinkAt100(FakeTickTarget clock) {
        return new FakeScorchSink(clock, 100.0);
    }

    // --- The clock ----------------------------------------------------------------------------

    @Test
    void theFirstBurnLandsONAPPLICATIONNotOnePeriodLater() {
        // RepeatingTask's first tick is SCHEDULED, not inline (RepeatingTask.java:42). For an
        // attribute that is right; for a DoT it means a scorch shorter than one period deals nothing
        // at all. A hit should always burn at least once.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);

        scorch.apply(UUID.randomUUID(), clock, sink, 1, 20.0, UUID.randomUUID(), 160);

        assertEquals(1, sink.count(), "the burn lands immediately, before the clock has moved");
        assertEquals(0L, sink.burns.get(0).atTick(), "at tick 0");
        // Mutation: drop the inline burnOnce in apply() -> count 0 -> reddens.
    }

    @Test
    void burnsLandEveryTwentyTicksAndNotFaster() {
        // SCORCH MUST OWN AN EXPLICIT 20-TICK SCHEDULE (NEXT.md:833). The whole reason this class
        // exists rather than "deal damage while the status is active".
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);

        scorch.apply(UUID.randomUUID(), clock, sink, 1, 20.0, UUID.randomUUID(), 160);
        clock.advance(60);

        assertEquals(4, sink.count(), "tick 0 inline, then 20, 40, 60 -- one per period, not per tick");
        assertEquals(0L, sink.burns.get(0).atTick());
        assertEquals(20L, sink.burns.get(1).atTick());
        assertEquals(40L, sink.burns.get(2).atTick());
        assertEquals(60L, sink.burns.get(3).atTick());
        // Mutation: start the task at period 1 -> 61 burns -> reddens. This is the 20 Hz shape D3a
        // measured, arriving from our own scheduler instead of from a poisoned i-frame window.
    }

    @Test
    void eightDamageTicksForTheEightSecondDefault() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);

        scorch.apply(UUID.randomUUID(), clock, sink, 1, 20.0, UUID.randomUUID(),
                Scorch.DEFAULT_DURATION_TICKS);
        clock.advance(400);   // well past expiry

        assertEquals(8, sink.count(), "160 ticks at period 20 is EIGHT burns, first one included");
        assertEquals(Scorch.damageTicksFor(Scorch.DEFAULT_DURATION_TICKS), sink.count(),
                "and core's arithmetic agrees with what the scheduler actually did");
        // MEASURED, NOT ASSUMED: this row stays GREEN at 8 under the two-part mutation described in
        // theEightSecondStatusLivesEXACTLYEightSeconds, where the status lives 180 ticks. It does
        // redden under the ordering swap alone (9 burns). So the count is a real assertion but not a
        // sufficient one -- which is why the lifetime is asserted separately rather than inferred.
    }

    @Test
    void theEightSecondStatusLivesEXACTLYEightSeconds() {
        // THE ROW THAT CATCHES THE INHERITED OFF-BY-ONE.
        //
        // SoakedStatus checks `remaining <= 0` BEFORE decrementing, so its task acts N/P times and
        // stops one whole period later. At period 1 that is one tick -- invisible, which is why the
        // precedent is fine. AT PERIOD 20 IT IS A FULL SECOND ON AN 8-SECOND STATUS.
        //
        // The tick COUNT is 8 either way. Only the LIFETIME separates them.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();

        scorch.apply(id, clock, sink, 1, 20.0, UUID.randomUUID(), Scorch.DEFAULT_DURATION_TICKS);

        clock.advance(140);
        assertEquals(8, sink.count(), "all eight burns have landed by t=140");
        assertEquals(140L, sink.lastBurnTick(), "the last burn is at t=140, not t=160");

        clock.advance(20);   // now at t=160
        assertFalse(scorch.isScorched(id), "and the status is GONE at t=160 -- 8.0s, not 9.0s");
        assertEquals(8, sink.count(), "t=160 expires the status and deals nothing");
        assertEquals(0, clock.pending(), "no re-armed tick left holding the clock");
        assertEquals(0, scorch.trackedVictims(), "and onStop cleared the map entry");
        // MUTATION, RUN RED: swap to SoakedStatus's `if (remaining <= 0) return false; remaining -=
        // PERIOD;` ordering AND drop the inline first burn in apply(). The status then acts 8 times at
        // t=20..160 -- so eightDamageTicksForTheEightSecondDefault STAYS GREEN AT 8 -- while living
        // 180 ticks. This row reddens on "all eight burns have landed by t=140: expected 8, was 7".
        //
        // The ordering swap ALONE reddens both rows (9 burns), so it does not demonstrate the point.
        // The two-part mutation is the one that shows the lifetime needs its own assertion, and it was
        // executed rather than reasoned about.
    }

    // --- Refresh ------------------------------------------------------------------------------

    @Test
    void reApplyingFasterThanThePeriodStillTicks() {
        // THE TOTAL SILENT FAILURE. Restarting the task on refresh re-phases the 20-tick clock, so a
        // weapon hitting every 15 ticks resets it forever: stacks climb, the burn shows, and NO
        // DAMAGE IS EVER DEALT. SoakedStatus already gets this right for its own reasons
        // (SoakedStatus.java:57); it matters far more here, and nothing else would catch it.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();
        UUID applier = UUID.randomUUID();

        scorch.apply(id, clock, sink, 1, 20.0, applier, 160);
        int burnsFromApplications = 1;
        for (int i = 0; i < 6; i++) {           // re-apply every 15 ticks, six times
            clock.advance(15);
            scorch.apply(id, clock, sink, 1, 20.0, applier, 160);
            burnsFromApplications++;
        }

        assertTrue(sink.count() > burnsFromApplications,
                "SCHEDULED burns must have landed too, not only the inline ones from each apply -- "
                        + "got " + sink.count() + " burns from " + burnsFromApplications
                        + " applications; equal would mean the clock was re-phased and never fired");
        assertEquals(7, scorch.stacks(id), "and the stacks accumulated across the refreshes");
        // Mutation: cancel and restart the task inside apply()'s refresh arm -> the scheduled burns
        // vanish, count == 7 == burnsFromApplications -> reddens.
    }

    @Test
    void aNewApplierTakesOverBOTHTheCapAndTheCredit() {
        // Two operator rules point the same way -- newest applier gets ignite kill credit, and each
        // new stack refreshes the whole timer -- so this is ONE rule, not two.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 5000.0);   // high max so the CAP is what shows
        UUID id = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        scorch.apply(id, clock, sink, 1, 20.0, first, 160);
        assertEquals(20.0, sink.burns.get(0).amount(), EPS, "the first applier's cap of 20");
        assertEquals(first, sink.burns.get(0).applierId(), "credited to the first applier");

        scorch.apply(id, clock, sink, 1, 8.0, second, 160);
        assertEquals(8.0, sink.burns.get(1).amount(), EPS, "the NEWEST applier's cap of 8 takes over");
        assertEquals(second, sink.burns.get(1).applierId(), "and so does the credit");
        assertEquals(second, scorch.applier(id), "which is what slice 2's ignite will credit");
        // Mutation: keep the original cap/applier on refresh -> both reddens.
    }

    @Test
    void stacksAccumulateAndTheTimerRefreshesWHOLERatherThanPerStack() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();

        scorch.apply(id, clock, sink, 10, 20.0, UUID.randomUUID(), 160);   // a 20-damage staff hit
        assertEquals(10, scorch.stacks(id), "ten stacks from one call, not ten calls");

        clock.advance(100);
        scorch.apply(id, clock, sink, 3, 20.0, UUID.randomUUID(), 160);
        assertEquals(13, scorch.stacks(id), "stacks add");

        clock.advance(100);   // t=200, past the ORIGINAL 160-tick window
        assertTrue(scorch.isScorched(id), "the refresh extended the WHOLE window, so it is still live");
        // Mutation: refresh only a per-stack clock, or fail to rewrite remaining -> expired -> reddens.
    }

    // --- The rate is flat -----------------------------------------------------------------------

    @Test
    void stacksDoNOTScaleTheDamage() {
        // THE OPERATOR'S SUPERSEDED STATEMENT, PINNED.
        //
        // He first said stacks determine damage per second, then revised to a flat rate. The Flint
        // Staff applies TEN stacks: at 5% per stack that is 50%/sec, a two-second kill on anything --
        // exactly the execution he ruled out. This is the only place the one-line mutation is
        // reachable, because Scorch.damagePerTick takes no stack argument at all.
        var clock = new FakeTickTarget();

        var one = new ScorchStatus();
        var sinkOne = sinkAt100(clock);
        one.apply(UUID.randomUUID(), clock, sinkOne, 1, 20.0, UUID.randomUUID(), 160);

        var ten = new ScorchStatus();
        var sinkTen = sinkAt100(clock);
        ten.apply(UUID.randomUUID(), clock, sinkTen, 10, 20.0, UUID.randomUUID(), 160);

        assertEquals(5.0, sinkOne.burns.get(0).amount(), EPS, "one stack burns 5% of a 100 pool");
        assertEquals(sinkOne.burns.get(0).amount(), sinkTen.burns.get(0).amount(), EPS,
                "and TEN stacks burn exactly the same -- the rate is flat, stacks feed ignite");
        // Mutation: multiply by a.stacks in burnOnce -> 50 against 5 -> reddens.
    }

    @Test
    void theCapBINDSOnAHighMaxPoolAndTheTotalIsEightTimesIt() {
        // The anti-boss brake, through the scheduler rather than the arithmetic -- and asserted at a
        // max where it BINDS, because at 100 a capless implementation is indistinguishable.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 5000.0);

        scorch.apply(UUID.randomUUID(), clock, sink, 1, 20.0, UUID.randomUUID(),
                Scorch.DEFAULT_DURATION_TICKS);
        clock.advance(400);

        assertEquals(8, sink.count(), "eight burns");
        assertEquals(20.0, sink.burns.get(0).amount(), EPS, "each held to the cap, not 5% of 5000");
        assertEquals(160.0, sink.totalDealt(), EPS,
                "so a full window against a capped target is 8 x cap -- 160, whatever the pool. "
                        + "Uncapped it would be 2000.");
        // Mutation: drop the cap in Scorch.damagePerTick -> 250 each, 2000 total -> reddens.
    }

    @Test
    void theMaxIsREADFreshEachTickSoAMidBurnMaxChangeIsHonoured() {
        // The pool is read per tick rather than frozen at application. A boss that gains max health
        // mid-fight, or a player whose gear changes, burns against what they have NOW.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);

        scorch.apply(UUID.randomUUID(), clock, sink, 1, 20.0, UUID.randomUUID(), 160);
        assertEquals(5.0, sink.burns.get(0).amount(), EPS, "5% of 100");

        sink.maxHealth = 200.0;
        clock.advance(20);
        assertEquals(10.0, sink.burns.get(1).amount(), EPS, "5% of the NEW 200, not the old 100");
        // Mutation: capture maxHealth into Active at apply() -> stays 5.0 -> reddens.
    }

    // --- Lifecycle ----------------------------------------------------------------------------

    @Test
    void aHitTooSmallToBuyAStackScorchesNothing() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();

        scorch.apply(id, clock, sink, 0, 20.0, UUID.randomUUID(), 160);

        assertFalse(scorch.isScorched(id), "zero stacks starts nothing");
        assertEquals(0, sink.count(), "and burns nothing");
        assertEquals(0, clock.pending(), "and leaves no task running");
        // Mutation: drop the stacks<=0 guard -> a 1-damage graze starts a full 8-second burn
        // -> reddens on all three.
    }

    @Test
    void whenTheVictimDiesTheTaskStopsWithoutTouchingIt() {
        // RepeatingTask stops before the body runs when the target reports inactive, so nothing
        // touches a removed entity -- while onStop still clears our per-target state.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();

        scorch.apply(id, clock, sink, 5, 20.0, UUID.randomUUID(), 160);
        int burnsBeforeDeath = sink.count();

        clock.active = false;
        clock.advance(100);

        assertEquals(burnsBeforeDeath, sink.count(), "no burn was dealt to a dead victim");
        assertFalse(scorch.isScorched(id), "and the state is gone");
        assertEquals(0, scorch.trackedVictims(), "with nothing left in the map");
        // Mutation: delete RepeatingTask's isActive guard -> burns land on a removed entity -> reddens.
    }

    @Test
    void forgetDropsAScorchOutrightAndStopsItsTask() {
        // Wired to mob removal, player quit AND player respawn -- the third for DamageWindow.forget's
        // reason: quit handling does not run on death, so without it a player who dies mid-burn
        // respawns still scorched.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();

        scorch.apply(id, clock, sink, 5, 20.0, UUID.randomUUID(), 160);
        int burnsBeforeForget = sink.count();

        scorch.forget(id);
        clock.advance(200);

        assertFalse(scorch.isScorched(id), "forgotten");
        assertEquals(0, scorch.trackedVictims(), "and unmapped");
        assertEquals(burnsBeforeForget, sink.count(), "and it burns no more after being forgotten");
        // Mutation: remove from the map without cancelling the task -> the orphan keeps burning
        // -> the burn-count assertion reddens. Removal alone is NOT enough.
    }

    @Test
    void twoVictimsBurnIndependently() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var a = sinkAt100(clock);
        var b = new FakeScorchSink(clock, 200.0);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        scorch.apply(first, clock, a, 1, 20.0, UUID.randomUUID(), 160);
        scorch.apply(second, clock, b, 3, 20.0, UUID.randomUUID(), 40);

        assertEquals(2, scorch.trackedVictims(), "keyed per victim");
        assertEquals(1, scorch.stacks(first));
        assertEquals(3, scorch.stacks(second));

        clock.advance(60);
        assertTrue(scorch.isScorched(first), "the 160-tick burn is still going");
        assertFalse(scorch.isScorched(second), "the 40-tick one expired on its own schedule");
        assertEquals(1, scorch.trackedVictims(), "and only its entry was cleared");
        // Mutation: key the store on anything but the victim -> the two collide -> reddens.
    }
}
