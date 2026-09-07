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
     * A LIVE combatant's display floor: half a heart. It stops a rounding-down from killing someone
     * the truth says is alive -- 1 custom HP out of 400 is a sliver of a heart, and writing it as 0
     * would be a display write committing a death the death system never claimed.
     *
     * <p><b>It applies STRICTLY ABOVE ZERO. At exactly zero the renderer writes zero</b> -- see
     * {@link #render}. That boundary is the whole of the fix for the defect below, and it is what the
     * word LIVE in this constant's name has always meant.
     *
     * <h2>THIS FLOOR ONCE APPLIED AT ZERO TOO, AND THAT WAS A SHIPPED DEFECT</h2>
     *
     * Its original javadoc read: <i>"this phase has no damage/death system -- so a DISPLAY write must
     * not itself kill the player. Death arrives with the next-phase damage system; UNTIL THEN the bar
     * is purely cosmetic and never lethal."</i>
     *
     * <p><b>That scaffold wrote its own expiry condition down, the condition was met, and the floor
     * stayed.</b> The next-phase damage system is {@code PlayerHealthSystem.onChange}, in this
     * package. Once it existed, {@code reachedZero} scheduled a real {@code setHealth(0)} -- and
     * because {@code reachedZero} fires only on the TRANSITION to zero, the very next
     * {@code HealthChange} on an already-zero combatant took the render branch instead and wrote this
     * floor **over the top of the kill**.
     *
     * <p>Measured in game 2026-09-06: the player got a death screen, health read <b>1</b>, the bar was
     * empty, and <b>the respawn button did nothing</b> -- the server saw a living player and refused.
     * Relog was the only recovery, and it returned the player standing where they died, because no
     * death had ever completed. <b>Any</b> further change supplied the fatal render: another damage
     * tick, or a passive regeneration heal. To this code those are the same non-transitioning event,
     * which is why there was one defect here and not two.
     */
    public static final double MIN_LIVE_HEALTH_POINTS = 1.0;

    /** Vanilla max health must be positive; floor the displayed max at one heart. */
    private static final int MIN_MAX_HEALTH_POINTS = 2;

    /** What a combatant at zero custom HP displays: nothing. The truth, not a floor. */
    private static final double DEAD_HEALTH_POINTS = 0.0;

    /**
     * Write the custom numbers onto the vanilla bar.
     *
     * <p><b>Zero renders as ZERO.</b> The display now agrees with a death instead of overwriting it,
     * so a render that lands behind a {@code setHealth(0)} is idempotent rather than a revival.
     *
     * <p><b>And it still cannot kill anyone the truth says is alive:</b> the zero write is reached
     * only when custom HP is actually zero, and every value above zero -- however small -- still
     * floors at {@link #MIN_LIVE_HEALTH_POINTS}. The death system owns the transition to zero; this
     * method only stops contradicting it.
     */
    public void render(HeartBar bar, double customCurrent, double customMax) {
        int hearts = HeartScale.heartCount(customMax);
        int maxPoints = Math.max(MIN_MAX_HEALTH_POINTS, hearts * 2);
        if (customCurrent <= 0) {
            bar.render(maxPoints, DEAD_HEALTH_POINTS);
            return;
        }
        double filledPoints = HeartScale.filledHearts(customCurrent, customMax) * 2.0;
        double healthPoints = Math.max(MIN_LIVE_HEALTH_POINTS, Math.min(maxPoints, filledPoints));
        bar.render(maxPoints, healthPoints);
    }
}
