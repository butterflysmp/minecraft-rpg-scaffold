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

    @Test
    void customZeroShowsAFlooredHalfHeartBecauseTheBarIsNotTheTruth() {
        var bar = new FakeHeartBar();
        new HeartBarRenderer().render(bar, 0, 400);         // custom health is 0

        assertEquals(26, bar.maxHealthPoints, "max still 13 hearts");
        assertEquals(HeartBarRenderer.MIN_LIVE_HEALTH_POINTS, bar.healthPoints, EPS,
                "custom 0 -> display floored to half a heart: the DISPLAY must not itself kill (death is next phase)");
        // This IS display != truth: truth is 0, the bar shows a floor. Nothing downstream may read
        // this half-heart back as the health.
        // Mutation: pass filled 0 straight through -> a display write drops the live player to 0 HP -> reddens.
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
