package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.Scorch;
import org.junit.jupiter.api.Test;

import java.util.List;
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
    void theFirstBurnLandsONEPERIODInBecauseTheCLOCKIsTheOnlyThingThatBurns() {
        // THIS ROW ASSERTED THE OPPOSITE, AND ITS PREMISE IS GONE RATHER THAN ITS ASSERTION WRONG.
        //
        // It read theFirstBurnLandsONAPPLICATIONNotOnePeriodLater, and guarded an inline burnOnce in
        // the first-application arm. The justification was real: RepeatingTask schedules its first
        // tick rather than running it inline (RepeatingTask.java:42), so without an inline burn a
        // scorch shorter than one period would deal nothing.
        //
        // THAT JUSTIFICATION TURNED OUT TO BE FALSE, AND IT WAS MEASURED RATHER THAN ARGUED. A
        // sub-period duration does NOT deal nothing: it burns once at t=20 and lives 20 ticks, which
        // is the n=1 instance of the round-up damageTicksFor already documents. Printed across 1, 19,
        // 21, 41 and 50 ticks -- burns equalled damageTicksFor at every one.
        //
        // So the inline burn bought nothing and cost the asymmetry that produced slice 1's nine-burn
        // defect, where the refresh arm burned because the first arm did. Both arms are silent now.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);

        scorch.apply(UUID.randomUUID(), clock, sink, 1, 20.0, UUID.randomUUID(), 120, "fire", 0);

        assertEquals(0, sink.count(), "nothing burns on application -- the clock has not moved");
        clock.advance(20);
        assertEquals(1, sink.count(), "the first burn is the clock's first edge");
        assertEquals(20L, sink.burns.get(0).atTick(), "at t=20, not t=0");
        // Mutation: restore the inline burnOnce in apply() -> count 1 before the advance -> reddens.
    }

    @Test
    void aSubPeriodScorchStillBurnsOnceRatherThanVanishing() {
        // THE CASE THE INLINE BURN EXISTED TO PROTECT, held directly now that it does not.
        //
        // It looks like it needs a guard and it does not. A refusal here would put back the special
        // case the inline burn's deletion exists to remove -- one rule, not two.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();

        scorch.apply(id, clock, sink, 1, 20.0, UUID.randomUUID(), 1, "fire", 0);
        clock.advance(400);

        assertEquals(1, sink.count(), "a one-tick scorch burns once, not never");
        assertEquals(20L, sink.burns.get(0).atTick(), "on the clock's first edge, one period in");
        assertEquals(Scorch.damageTicksFor(1), sink.count(), "and core's arithmetic agrees");
        assertFalse(scorch.isScorched(id), "then it is gone");
        // Mutation: refuse durations below one period -> count 0 -> reddens, which is the row that
        // stops that refusal being added back as an obvious-looking safety check.
    }

    @Test
    void burnsLandEveryTwentyTicksAndNotFaster() {
        // SCORCH MUST OWN AN EXPLICIT 20-TICK SCHEDULE (NEXT.md:833). The whole reason this class
        // exists rather than "deal damage while the status is active".
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);

        scorch.apply(UUID.randomUUID(), clock, sink, 1, 20.0, UUID.randomUUID(), 120, "fire", 0);
        clock.advance(60);

        assertEquals(3, sink.count(), "20, 40, 60 -- one per period, not per tick, and none at t=0");
        assertEquals(20L, sink.burns.get(0).atTick());
        assertEquals(40L, sink.burns.get(1).atTick());
        assertEquals(60L, sink.burns.get(2).atTick());
        // Mutation: start the task at period 1 -> 60 burns -> reddens. This is the 20 Hz shape D3a
        // measured, arriving from our own scheduler instead of from a poisoned i-frame window.
    }

    @Test
    void sixDamageTicksForTheSixSecondDefault() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);

        scorch.apply(UUID.randomUUID(), clock, sink, 1, 20.0, UUID.randomUUID(),
                Scorch.DEFAULT_DURATION_TICKS, "fire", 0);
        clock.advance(400);   // well past expiry

        assertEquals(6, sink.count(), "120 ticks at period 20 is SIX burns, at t=20..120");
        assertEquals(Scorch.damageTicksFor(Scorch.DEFAULT_DURATION_TICKS), sink.count(),
                "and core's arithmetic agrees with what the scheduler actually did");
        // The count is a real assertion but not a sufficient one -- an ordering defect can preserve
        // it while moving the lifetime, which is why the lifetime is asserted separately below.
    }

    @Test
    void theSixSecondStatusLivesEXACTLYSixSeconds() {
        // THE ROW THAT CATCHES AN OFF-BY-ONE-PERIOD, RE-DERIVED FROM A NEW PREMISE.
        //
        // It used to catch SoakedStatus's check-then-decrement ordering, which at period 20 would put
        // a full extra second on the status. THE PREMISE HAS FLIPPED: with the inline first burn gone,
        // BURN-THEN-DECREMENT is the correct ordering and decrement-first is the defect -- it drops
        // the LAST period instead of adding one, so a 120-tick scorch would burn five times and fall
        // silent through its sixth second.
        //
        // > A TEST WRITTEN TO GUARD AN ORDERING MUST BE RE-DERIVED WHEN THE ORDERING'S PREMISE
        // > CHANGES. Its red is evidence about the premise, not about the code.
        //
        // This row reddened on a CORRECT change and was re-derived rather than nudged to green. The
        // schedule was MEASURED by printing it: [20, 40, 60, 80, 100, 120].
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();

        scorch.apply(id, clock, sink, 1, 20.0, UUID.randomUUID(), Scorch.DEFAULT_DURATION_TICKS, "fire", 0);

        clock.advance(100);
        assertEquals(5, sink.count(), "five burns have landed by t=100");
        assertTrue(scorch.isScorched(id), "and the status is still running -- its last second is owed");

        clock.advance(20);   // now at t=120
        assertEquals(6, sink.count(), "the SIXTH burn lands at t=120, the final period");
        assertEquals(120L, sink.lastBurnTick(), "the last burn is at t=120, not t=100 and not t=140");
        assertFalse(scorch.isScorched(id), "and the status expires on that same tick -- 6.0s exactly");
        assertEquals(0, clock.pending(), "no re-armed tick left holding the clock");
        assertEquals(0, scorch.trackedVictims(), "and onStop cleared the map entry");
        // MUTATION: keep burn-then-decrement but restore decrement-first -> the t=120 burn never
        // happens -> count 5 and lastBurnTick 100 -> reddens. That is the defect this ordering pairs
        // with the deleted inline burn to prevent.
    }

    // --- Refresh ------------------------------------------------------------------------------

    @Test
    void reApplyingFasterThanThePeriodStillTicks() {
        // THE TOTAL SILENT FAILURE. Restarting the task on refresh re-phases the 20-tick clock, so a
        // weapon hitting every 15 ticks resets it forever: the window keeps refreshing, the burn
        // shows, and NO DAMAGE IS EVER DEALT. SoakedStatus already gets this right for its own reasons
        // (SoakedStatus.java:57); it matters far more here, and nothing else would catch it.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();
        UUID applier = UUID.randomUUID();

        scorch.apply(id, clock, sink, 1, 20.0, applier, 160, "fire", 0);
        for (int i = 0; i < 6; i++) {           // re-apply every 15 ticks, six times
            clock.advance(15);
            scorch.apply(id, clock, sink, 1, 20.0, applier, 160, "fire", 0);
        }

        // NONE of these burns is inline -- there is no inline burn any longer. Every one is the
        // task firing on its OWN 20-tick phase through six refreshes, which is the whole assertion,
        // and the absence of a t=0 entry is now part of it. (This row once counted inline burns per
        // application and asserted there were more burns than applications. That worked only because
        // the refresh arm burned; the direct form is what catches the re-phase.)
        assertEquals(List.of(20L, 40L, 60L, 80L),
                sink.burns.stream().map(FakeScorchSink.Burn::atTick).toList(),
                "the task keeps its own phase across refreshes");
        // MUTATION, RUN RED (measured, not predicted): cancel and restart the task inside apply()'s
        // refresh arm -> the phase resets on every re-application and the 20-tick clock never lands.
        //
        // The EXACT list is an artefact of the mutation, not the point: cancel() fires onStop, which
        // removes the map entry, so the next apply() takes the NEW-Active path instead of the
        // refresh path -- inline burns at odd phases rather than the silence a pure re-phase gives.
        // What this row pins is the invariant that survives either shape: BURNS LAND ON THE ORIGINAL
        // 20-TICK PHASE. Asserting the tick list rather than a count is what makes it insensitive to
        // which of the two the mutation happens to produce.
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

        scorch.apply(id, clock, sink, 1, 20.0, first, 160, "fire", 0);
        clock.advance(20);   // the clock is the only thing that burns -- nothing lands on application
        assertEquals(20.0, sink.burns.get(0).amount(), EPS, "the first applier's cap of 20");
        assertEquals(first, sink.burns.get(0).applierId(), "credited to the first applier");

        scorch.apply(id, clock, sink, 1, 8.0, second, 160, "fire", 0);
        assertEquals(second, scorch.applier(id), "which is what Ignite will credit");

        // The refresh deals nothing of its own (aRefreshDoesNotDealAnUNSCHEDULEDBurn), so the
        // takeover is read from the next SCHEDULED tick. That is a STRONGER claim than the inline
        // read this row used to make: it shows the new cap and credit survive into the clock rather
        // than only into the call that set them.
        clock.advance(20);
        assertEquals(8.0, sink.burns.get(1).amount(), EPS, "the NEWEST applier's cap of 8 takes over");
        assertEquals(second, sink.burns.get(1).applierId(), "and so does the credit");
        assertEquals(40L, sink.burns.get(1).atTick(),
                "and it landed on the clock, at t=40 -- the SECOND scheduled edge, since the first "
                        + "already burned under the original applier");
        // Mutation: keep the original cap/applier on refresh -> min(5% of 5000, 20) = 20 credited to
        // `first`, against an expected 8 credited to `second` -> reddens on both.
    }

    @Test
    void theTimerRefreshesWHOLERatherThanPerStack() {
        // WAS stacksAccumulateAndTheTimerRefreshesWHOLERatherThanPerStack, and it asserted BOTH
        // halves. The accumulate half was deleted with the accumulator on 2026-09-09 (see Scorch's
        // javadoc): with no count to read, "ten stacks from one call, not ten calls" has no
        // observable difference from "one stack from one call" -- there is nothing left that a bulk
        // application changes. THE REFRESH HALF IS THE ONE THAT WAS EVER LOAD-BEARING, and it is
        // untouched: it is the invariant the mutation below attacks.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();

        scorch.apply(id, clock, sink, 10, 20.0, UUID.randomUUID(), 160, "fire", 0);   // a 20-damage staff hit
        assertTrue(scorch.isScorched(id), "one application scorches");

        clock.advance(100);
        scorch.apply(id, clock, sink, 3, 20.0, UUID.randomUUID(), 160, "fire", 0);

        clock.advance(100);   // t=200, past the ORIGINAL 160-tick window
        assertTrue(scorch.isScorched(id), "the refresh extended the WHOLE window, so it is still live");
        // Mutation: refresh only a per-stack clock, or fail to rewrite remaining -> expired -> reddens.
        // Still discriminating without the count: t=200 is 40 ticks past the original window, so an
        // unrefreshed timer has already expired and isScorched returns false.
    }

    @Test
    void aRefreshDoesNotDealAnUNSCHEDULEDBurn() {
        // THE REFRESH ARM MUST NOT BURN. The inline burn in apply() is for the FIRST application --
        // a single stack should burn at least once even if it expires inside one period. On a
        // refresh the task is ALREADY RUNNING and will burn on schedule, so an inline burn there is
        // damage OUTSIDE the clock this class exists to own, and it scales with HIT RATE rather than
        // with time: a weapon hitting every 10 ticks deals 2 inline + 1 scheduled per period, THREE
        // TIMES the stated rate, and "5% of max per second" quietly becomes "5% per second PLUS 5%
        // per application" for every weapon in the game.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();

        scorch.apply(id, clock, sink, 1, 20.0, UUID.randomUUID(), Scorch.DEFAULT_DURATION_TICKS, "fire", 0);
        clock.advance(10);                                   // mid-period: nothing is due here
        scorch.apply(id, clock, sink, 1, 20.0, UUID.randomUUID(), Scorch.DEFAULT_DURATION_TICKS, "fire", 0);

        clock.advance(400);                                  // well past expiry

        assertTrue(sink.burns.stream().noneMatch(b -> b.atTick() == 10L),
                "the re-application at t=10 burned on its own -- burns landed at "
                        + sink.burns.stream().map(b -> String.valueOf(b.atTick())).toList());
        assertEquals(List.of(20L, 40L, 60L, 80L, 100L, 120L),
                sink.burns.stream().map(FakeScorchSink.Burn::atTick).toList(),
                "a re-application inside the window buys STACKS and TIME, not a seventh damage tick");
        assertEquals(30.0, sink.totalDealt(), EPS,
                "so a full window is 6 x 5% = 30% of max, whatever the hit rate");
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
        one.apply(UUID.randomUUID(), clock, sinkOne, 1, 20.0, UUID.randomUUID(), 160, "fire", 0);

        var ten = new ScorchStatus();
        var sinkTen = sinkAt100(clock);
        ten.apply(UUID.randomUUID(), clock, sinkTen, 10, 20.0, UUID.randomUUID(), 160, "fire", 0);

        clock.advance(20);   // nothing burns on application; the clock is the only source

        assertEquals(5.0, sinkOne.burns.get(0).amount(), EPS, "one stack burns 5% of a 100 pool");
        assertEquals(sinkOne.burns.get(0).amount(), sinkTen.burns.get(0).amount(), EPS,
                "and TEN stacks burn exactly the same -- the rate is FLAT. Since the 2026-09-09 "
                        + "ruling nothing reads the count at all, so this pins the argument is a "
                        + "gate and never a multiplier");
        // Mutation: multiply by a.stacks in burnOnce -> 50 against 5 -> reddens.
    }

    @Test
    void theCapBINDSOnAHighMaxPoolAndTheTotalIsSixTimesIt() {
        // The anti-boss brake, through the scheduler rather than the arithmetic -- and asserted at a
        // max where it BINDS, because at 100 a capless implementation is indistinguishable.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = new FakeScorchSink(clock, 5000.0);

        scorch.apply(UUID.randomUUID(), clock, sink, 1, 20.0, UUID.randomUUID(),
                Scorch.DEFAULT_DURATION_TICKS, "fire", 0);
        clock.advance(400);

        assertEquals(6, sink.count(), "six burns");
        assertEquals(20.0, sink.burns.get(0).amount(), EPS, "each held to the cap, not 5% of 5000");
        assertEquals(120.0, sink.totalDealt(), EPS,
                "so a full window against a capped target is 6 x cap -- 120, whatever the pool. "
                        + "Uncapped it would be 1500.");
        // Mutation: drop the cap in Scorch.damagePerTick -> 250 each, 1500 total -> reddens.
    }

    @Test
    void theMaxIsREADFreshEachTickSoAMidBurnMaxChangeIsHonoured() {
        // The pool is read per tick rather than frozen at application. A boss that gains max health
        // mid-fight, or a player whose gear changes, burns against what they have NOW.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);

        scorch.apply(UUID.randomUUID(), clock, sink, 1, 20.0, UUID.randomUUID(), 160, "fire", 0);
        clock.advance(20);   // nothing burns on application; the clock is the only source
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

        scorch.apply(id, clock, sink, 0, 20.0, UUID.randomUUID(), 160, "fire", 0);

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

        scorch.apply(id, clock, sink, 5, 20.0, UUID.randomUUID(), 160, "fire", 0);
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

        scorch.apply(id, clock, sink, 5, 20.0, UUID.randomUUID(), 160, "fire", 0);
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
    void depthIsDEEPESTWinsWhileCapAndApplierAreNEWESTWins() {
        // THE ONE FIELD ON Active THAT IS NOT NEWEST-WINS, AND THE CHAIN LIMIT DEPENDS ON IT.
        //
        // cap, applierId and element are PRESENTATION AND CREDIT facts, where the most recent
        // applier is the right answer. depth is a SAFETY COUNTER, where the deepest reading is --
        // because THE BURN WINDOW DECOUPLES DEPTH FROM ELAPSED TIME, so a later blast is NOT
        // necessarily a deeper blast:
        //
        //   A scorched at depth 1, high HP, survives its burn and dies late -> detonates at 2.
        //   Meanwhile a fast branch has already run 2 -> 3, scorching D at DEPTH 3.
        //   A's late depth-2 blast reaches D. Newest-wins drops D from 3 to 2 and the frontier
        //   advances again -- repeat with staggered survivors and the link count from the original
        //   root is bounded by the MOB POPULATION rather than by MAX_CHAIN_DEPTH.
        //
        // That is the unbounded cascade the recruitment ruling was bounded to avoid, arriving by
        // the back door, and NO GATE ROW COULD REACH IT -- it needs two branches at different
        // speeds converging on one mob.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        scorch.apply(id, clock, sink, 1, 20.0, first, 160, "fire", 3);
        scorch.apply(id, clock, sink, 1, 8.0, second, 160, "fire", 2);   // LATER, but SHALLOWER

        assertEquals(3, scorch.depth(id),
                "the deeper reading survives a later, shallower blast -- max, not overwrite");
        assertEquals(second, scorch.applier(id),
                "while CREDIT is still newest-wins, in the same call: the two rules coexist");
        // THE FIXTURE MUST DISAGREE OR IT CHECKS NOTHING. 3 then 2 is deliberately DECREASING, and
        // the applier changes in the same call -- so a naive `a.depth = depth` reddens the first
        // assertion while the second still passes, which is what proves the two fields follow
        // DIFFERENT rules rather than both having been switched to max.
        //
        // Mutation: a.depth = depth (newest-wins) -> the depth assertion reddens, alone.
        // Mutation: applierId = Math-style "first wins" -> the applier assertion reddens, alone.
    }

    @Test
    void aDeeperBlastStillRaisesTheDepth() {
        // The other direction, so "deepest wins" is not passing merely because the field is never
        // written after the first application.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();

        scorch.apply(id, clock, sink, 1, 20.0, UUID.randomUUID(), 160, "fire", 1);
        scorch.apply(id, clock, sink, 1, 20.0, UUID.randomUUID(), 160, "fire", 3);

        assertEquals(3, scorch.depth(id), "a deeper blast raises it");
        // Mutation: a.depth = Math.min(...) -> reddens here while the row above still passes.
    }

    @Test
    void depthIsZeroForAWeaponOrDevApplicationAndForAnUnknownId() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();

        scorch.apply(id, clock, sink, 1, 20.0, UUID.randomUUID(), 160, "fire", 0);

        assertEquals(0, scorch.depth(id), "no blast caused this, so it ignites at depth 1");
        assertEquals(0, scorch.depth(UUID.randomUUID()), "and an unknown id reads 0, not a throw");
        // 0 is the honest value here rather than a sentinel: it means "no blast", and the death
        // handler adds one to get depth 1 -- the same answer a player kill gets.
    }

    @Test
    void forgettingTwiceIsANoOp() {
        // IGNITE'S ONCE-NESS GUARD RESTS ON THIS, WHICH IS WHY IT IS PINNED RATHER THAN ASSUMED.
        // RpgListeners.onEntityDeath reads the scorch and forgets it immediately, so a second
        // delivery of EntityDeathEvent finds nothing and does nothing. onEntityRemove THEN forgets
        // the same id again on removal. So in the ordinary life of one scorched mob that dies,
        // forget is called TWICE for the same id, and the second call must be harmless.
        //
        // It is idempotent by inspection -- active.remove(id) returns null the second time and the
        // `a != null` guard covers it -- but inspection is exactly what this slice has been wrong
        // about repeatedly, so it is executed here instead.
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var sink = sinkAt100(clock);
        UUID id = UUID.randomUUID();

        scorch.apply(id, clock, sink, 5, 20.0, UUID.randomUUID(), 160, "fire", 0);
        scorch.forget(id);
        int burnsAfterFirstForget = sink.count();

        scorch.forget(id);   // the removal-side call, after the death-side one

        assertFalse(scorch.isScorched(id), "still forgotten");
        assertEquals(0, scorch.trackedVictims(), "and still unmapped");
        clock.advance(200);
        assertEquals(burnsAfterFirstForget, sink.count(), "and nothing was restarted by forgetting again");

        // AND ON AN ID THAT WAS NEVER SCORCHED AT ALL -- the case a mob that dies unscorched hits,
        // where onEntityDeath returns early and onEntityRemove forgets an id with no entry.
        scorch.forget(UUID.randomUUID());
        assertEquals(0, scorch.trackedVictims(), "forgetting an unknown id is harmless too");
        // Mutation: drop the `a != null` guard in forget -> the unknown-id call NPEs -> reddens.
    }

    @Test
    void twoVictimsBurnIndependently() {
        var scorch = new ScorchStatus();
        var clock = new FakeTickTarget();
        var a = sinkAt100(clock);
        var b = new FakeScorchSink(clock, 200.0);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        scorch.apply(first, clock, a, 1, 20.0, UUID.randomUUID(), 160, "fire", 0);
        scorch.apply(second, clock, b, 3, 20.0, UUID.randomUUID(), 40, "fire", 0);

        assertEquals(2, scorch.trackedVictims(), "keyed per victim");
        assertTrue(scorch.isScorched(first), "both are lit");
        assertTrue(scorch.isScorched(second));

        clock.advance(60);
        assertTrue(scorch.isScorched(first), "the 160-tick burn is still going");
        assertFalse(scorch.isScorched(second), "the 40-tick one expired on its own schedule");
        assertEquals(1, scorch.trackedVictims(), "and only its entry was cleared");
        // Mutation: key the store on anything but the victim -> the two collide -> reddens.
    }
}
