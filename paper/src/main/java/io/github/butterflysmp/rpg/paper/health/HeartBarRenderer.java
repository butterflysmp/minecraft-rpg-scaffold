package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.stat.HeartScale;

/**
 * Turns custom health into a vanilla-heart-bar write. Bukkit-free: it takes the two custom numbers
 * and calls {@link HeartBar#render}, so the whole mapping is unit-testable against a fake bar.
 *
 * Display, not truth. Its ONLY inputs are the custom current and max -- it never reads a vanilla
 * value back (the seam gives it no way to), so the vanilla bar can only follow the custom health,
 * never become the source of it. Vanilla renders in half-heart units, so a heart count of N shows a
 * max of 2N points and a fill of F hearts shows 2F points.
 */
public final class HeartBarRenderer {

    /**
     * A live player's display floor: half a heart. Custom health can hit 0, but this phase has no
     * damage/death system -- so a DISPLAY write must not itself kill the player. Death arrives with
     * the next-phase damage system; until then the bar is purely cosmetic and never lethal. This is
     * exactly display != truth: custom current may be 0 while the bar shows half a heart.
     */
    public static final double MIN_LIVE_HEALTH_POINTS = 1.0;

    /** Vanilla max health must be positive; floor the displayed max at one heart. */
    private static final int MIN_MAX_HEALTH_POINTS = 2;

    public void render(HeartBar bar, double customCurrent, double customMax) {
        int hearts = HeartScale.heartCount(customMax);
        int maxPoints = Math.max(MIN_MAX_HEALTH_POINTS, hearts * 2);
        double filledPoints = HeartScale.filledHearts(customCurrent, customMax) * 2.0;
        double healthPoints = Math.max(MIN_LIVE_HEALTH_POINTS, Math.min(maxPoints, filledPoints));
        bar.render(maxPoints, healthPoints);
    }
}
