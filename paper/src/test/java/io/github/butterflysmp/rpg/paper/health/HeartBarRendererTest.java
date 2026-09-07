package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The heart renderer maps custom health onto the vanilla bar (2 points per heart). These pin the
 * design's worked examples and the display != truth property: the write is a pure function of the
 * CUSTOM numbers, and custom 0 shows a floored half-heart -- the bar is a display, never the source
 * of truth. Each names the mutation it forces red.
 */
class HeartBarRendererTest {

    private static final double EPS = 1e-9;

    @Test
    void baseHundredRendersAsTenFullHearts() {
        var bar = new FakeHeartBar();
        new HeartBarRenderer().render(bar, 100, 100);

        assertEquals(20, bar.maxHealthPoints, "100 max -> 10 hearts -> 20 vanilla points");
        assertEquals(20, bar.healthPoints, EPS, "full -> all 20 points filled");
        // Mutation: forget the *2 half-heart conversion -> max 10 -> reddens.
    }

    @Test
    void equippingRaisesHeartCountAndShowsHeadroomAsHurt() {
        var bar = new FakeHeartBar();
        // 100 current out of 400 max: 13 hearts, 25% full -> the "hurt-looking until you heal" state.
        new HeartBarRenderer().render(bar, 100, 400);

        assertEquals(26, bar.maxHealthPoints, "400 max -> 13 hearts -> 26 points");
        assertEquals(6.5, bar.healthPoints, EPS, "100/400 -> 3.25 hearts filled -> 6.5 points");
        // Mutation: fill from raw HP instead of percentage -> 100 current -> 200 points -> reddens.
    }

    @Test
    void fillIsAPercentageAcrossTheTierScale() {
        var bar = new FakeHeartBar();
        new HeartBarRenderer().render(bar, 200, 400);       // 50% of 13 hearts

        assertEquals(26, bar.maxHealthPoints, "13 hearts");
        assertEquals(13, bar.healthPoints, EPS, "200/400 -> 6.5 hearts -> 13 points");
        // Mutation: drop the tier scale (linear hearts) -> wrong count and fill -> reddens.
    }

    /**
     * REPLACES {@code customZeroShowsAFlooredHalfHeartBecauseTheBarIsNotTheTruth}, which asserted the
     * opposite and was <b>pinning a shipped defect</b>.
     *
     * <p>That test was correct when written: the floor's own javadoc said the bar "is purely cosmetic
     * and never lethal" <i>until</i> the next-phase damage system arrived. The damage system arrived,
     * the scaffold's stated expiry condition was met, and neither the floor nor the test that pinned
     * it moved. The test then defended the defect -- deleting the floor reddened it, exactly as its
     * mutation line promised, so the guard read as working right up to the boot that failed.
     *
     * <p>It is rewritten rather than deleted, and said so here, because the next reader's question is
     * "was this checked" and "it asserted the wrong thing" is a different answer from "it passed".
     */
    @Test
    void customZeroRendersZEROSoADisplayWriteCannotUNDOADeath() {
        var bar = new FakeHeartBar();
        new HeartBarRenderer().render(bar, 0, 400);         // custom health is 0

        assertEquals(26, bar.maxHealthPoints, "max still 13 hearts");
        assertEquals(0.0, bar.healthPoints, EPS,
                "custom 0 -> vanilla 0. reachedZero fires only on the TRANSITION, so the NEXT change on "
                        + "an already-dead combatant takes the render branch -- and if that render writes "
                        + "a live value it lands ON TOP of the queued setHealth(0). Measured in game: "
                        + "death screen up, health 1, respawn button dead, relog the only way out");
        // Mutation: restore Math.max(MIN_LIVE_HEALTH_POINTS, ...) at zero -> reddens with 1.0.
    }

    @Test
    void aSliverOfCustomHPStillFloorsBecauseTheFloORGuardsTheLIVING() {
        var bar = new FakeHeartBar();
        // 1 of 400 is 0.065 of a heart -> 0.13 points, which would round-trip to a lethal write.
        new HeartBarRenderer().render(bar, 1, 400);

        assertEquals(HeartBarRenderer.MIN_LIVE_HEALTH_POINTS, bar.healthPoints, EPS,
                "a combatant the truth says is ALIVE must never be killed by a display write -- that is "
                        + "the floor's remaining job, and the whole reason the fix is a boundary at zero "
                        + "rather than deleting the floor");
        // Mutation: delete the floor entirely -> writes 0.13 -> a live player is killed by a render -> reddens.
    }

    @Test
    void theBoundaryIsExactlyZeroInBothDirections() {
        // The pair that makes the rule a boundary rather than two anecdotes. Same max, adjacent inputs,
        // opposite outcomes -- and nothing between them can be reached, since HealthState clamps at 0.
        var dead = new FakeHeartBar();
        var alive = new FakeHeartBar();
        new HeartBarRenderer().render(dead, 0.0, 100);
        new HeartBarRenderer().render(alive, 0.0001, 100);

        assertEquals(0.0, dead.healthPoints, EPS, "exactly zero is the truth, and lethal");
        assertEquals(HeartBarRenderer.MIN_LIVE_HEALTH_POINTS, alive.healthPoints, EPS,
                "anything above zero is alive, and floors");
        // Mutation: move the test to < 0 instead of <= 0 -> the dead row writes the floor -> reddens.
    }

    @Test
    void theWriteDependsOnlyOnTheCustomInputsNotAnyVanillaState() {
        // Same custom numbers, two independent renders: identical writes. The renderer has no vanilla
        // reading to be swayed by -- the seam is write-only -- so the bar can only ever follow custom.
        var a = new FakeHeartBar();
        var b = new FakeHeartBar();
        new HeartBarRenderer().render(a, 150, 250);
        new HeartBarRenderer().render(b, 150, 250);

        assertEquals(a.maxHealthPoints, b.maxHealthPoints, "same custom max -> same displayed max");
        assertEquals(a.healthPoints, b.healthPoints, EPS, "same custom current/max -> same fill");
        assertEquals(24, a.maxHealthPoints, "250 max -> 12 hearts -> 24 points");
        // Mutation: read vanilla health as an input to the fill -> output would vary with server state -> reddens.
    }

    @Test
    void displayedMaxIsNeverBelowOneHeart() {
        var bar = new FakeHeartBar();
        new HeartBarRenderer().render(bar, 3, 5);           // tiny max: 1 heart

        assertEquals(2, bar.maxHealthPoints, "floored at one heart -- vanilla max must be positive");
        // Mutation: drop the min-max floor -> a 0-heart max is an illegal vanilla attribute -> reddens.
    }
}
