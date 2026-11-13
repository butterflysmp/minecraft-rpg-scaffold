package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Trail-style patch: spots revealed one at a time, earlier spots stay as cracks, and the last
 * charged hit breaks the patch.
 *
 * <p><b>The order of the checks is NotLive, Denied, Miss, Uncharged, then Hit or Broke</b>
 * (PLAN-mining.md 5.2). Each pair of neighbours in that order has a row whose input satisfies BOTH,
 * so swapping any two adjacent checks changes that row's outcome:
 * <ul>
 *   <li>broken AND no pickaxe: NotLive, not Denied;</li>
 *   <li>no pickaxe AND aim outside: Denied, not Miss;</li>
 *   <li>aim outside AND uncharged: Miss, not Uncharged.</li>
 * </ul>
 */
class PatchTest {

    // Three spots far apart, so an aim at one is never inside another's box.
    private static final SpotLayout LAYOUT =
            new SpotLayout(List.of(new Spot(10, 10), new Spot(30, 20), new Spot(40, 6)));
    private static final PayoutRule RULE = new PayoutRule(2, 4, 5);

    private static Patch fresh() {
        return new Patch(LAYOUT, RULE, 100L);
    }

    /** The centre of a spot's centre pixel. */
    private static Optional<PatchPoint> at(Spot s) {
        return Optional.of(new PatchPoint(s.cu() + 0.5, s.cv() + 0.5));
    }

    private static final Optional<PatchPoint> BESIDE = Optional.of(new PatchPoint(20.5, 28.5));

    private static Click charged(Optional<PatchPoint> aim, long tick) {
        return new Click(1.0f, aim, true, tick);
    }

    private static ClickOutcome click(Patch p, Click c) {
        return p.click(c, ScriptedRandom.of(2, 3, 4));
    }

    // --- Each outcome ---------------------------------------------------------------------------

    @Test
    void aChargedHitOnTheLiveSpotCracksItAndRevealsTheNext() {
        Patch p = fresh();
        ClickOutcome o = click(p, charged(at(LAYOUT.spots().get(0)), 117));
        assertEquals(new ClickOutcome.Hit(0, 1), o);
        assertEquals(1, p.liveIndex());
        assertEquals(117L, p.hitTick(0));
        assertEquals(117L, p.revealTick(1), "the next spot is revealed in the tick of the hit");
        assertEquals(100L, p.revealTick(0), "the first spot was revealed when the patch appeared");
    }

    @Test
    void aMissChangesNothing() {
        Patch p = fresh();
        assertEquals(ClickOutcome.Miss.INSTANCE, click(p, charged(BESIDE, 120)));
        assertEquals(ClickOutcome.Miss.INSTANCE, click(p, charged(Optional.empty(), 121)),
                "a ray that never reaches the plane is a miss");
        assertEquals(0, p.liveIndex());
        assertEquals(-1L, p.hitTick(0));
    }

    @Test
    void anUnchargedHitOnTheLiveSpotChangesNothing() {
        Patch p = fresh();
        assertEquals(ClickOutcome.Uncharged.INSTANCE,
                click(p, new Click(0.99f, at(LAYOUT.spots().get(0)), true, 120)));
        assertEquals(0, p.liveIndex());
    }

    @Test
    void aClickOnACrackIsAMiss() {
        Patch p = fresh();
        click(p, charged(at(LAYOUT.spots().get(0)), 117));
        assertEquals(ClickOutcome.Miss.INSTANCE, click(p, charged(at(LAYOUT.spots().get(0)), 140)),
                "only the LIVE spot's box counts");
        assertEquals(1, p.liveIndex());
    }

    @Test
    void walkingAThreeSpotPatchFromFreshToBroken() {
        Patch p = fresh();
        assertEquals(new ClickOutcome.Hit(0, 1), click(p, charged(at(LAYOUT.spots().get(0)), 117)));
        assertEquals(new ClickOutcome.Hit(1, 2), click(p, charged(at(LAYOUT.spots().get(1)), 140)));
        ClickOutcome last = click(p, charged(at(LAYOUT.spots().get(2)), 163));
        ClickOutcome.Broke broke = assertInstanceOf(ClickOutcome.Broke.class, last);
        assertEquals(2, broke.crackedIndex());
        assertEquals(List.of(2, 3, 4), broke.payout().rolls(), "one roll per spot, at the break");
        assertEquals(14, broke.payout().total(), "2 + 3 + 4 + the bonus 5");
        assertTrue(p.isBroken());
        assertEquals(163L, p.hitTick(2));
        assertEquals(ClickOutcome.NotLive.INSTANCE, click(p, charged(at(LAYOUT.spots().get(2)), 200)));
    }

    // --- The order of the checks ----------------------------------------------------------------

    @Test
    void brokenAndNoPickaxeIsNotLive() {
        Patch p = fresh();
        for (int i = 0; i < 3; i++) click(p, charged(at(LAYOUT.spots().get(i)), 117 + i));
        assertTrue(p.isBroken());
        assertEquals(ClickOutcome.NotLive.INSTANCE,
                click(p, new Click(1.0f, at(LAYOUT.spots().get(2)), false, 300)));
        // Mutation: check Denied before NotLive -> Denied -> reddens.
    }

    @Test
    void noPickaxeAndAimOutsideIsDenied() {
        Patch p = fresh();
        assertEquals(ClickOutcome.Denied.INSTANCE, click(p, new Click(1.0f, BESIDE, false, 120)));
        assertEquals(ClickOutcome.Denied.INSTANCE,
                click(p, new Click(1.0f, at(LAYOUT.spots().get(0)), false, 121)),
                "a charged hit on the live spot without a pickaxe is still denied");
        assertEquals(0, p.liveIndex());
        // Mutation: check Miss before Denied -> the first assertion reads Miss -> reddens.
    }

    @Test
    void aimOutsideAndUnchargedIsAMiss() {
        // Consequence, stated: an uncharged click BESIDE the spot is a Miss, so it plays the miss
        // sound; only an uncharged click IN the box is Uncharged, which plays nothing.
        Patch p = fresh();
        assertEquals(ClickOutcome.Miss.INSTANCE, click(p, new Click(0.5f, BESIDE, true, 120)));
        // Mutation: check Uncharged before Miss -> Uncharged -> reddens.
    }

    @Test
    void aFreshPatchHasItsFirstSpotLiveAndNothingHit() {
        Patch p = fresh();
        assertEquals(0, p.liveIndex());
        assertFalse(p.isBroken());
        assertEquals(-1L, p.revealTick(1), "not yet revealed");
        assertEquals(3, p.layout().spots().size());
    }
}
