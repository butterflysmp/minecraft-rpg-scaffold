package io.github.butterflysmp.rpg.paper.health;

/**
 * The vanilla heart bar as a Bukkit-free seam, so the tier-to-vanilla rendering is unit-testable
 * without a server -- the same shape as {@code SpeedAttribute}.
 *
 * Deliberately WRITE-ONLY. There is no getter, and that is the load-bearing part: the renderer
 * physically cannot read the player's vanilla health back, so it cannot mistake the display for the
 * source of truth. Custom health is truth; this bar only follows. A {@code render} carries the
 * displayed vanilla max (in health points, 2 per heart) and the displayed current, both computed
 * upstream from the CUSTOM numbers alone.
 */
public interface HeartBar {

    /**
     * Show {@code maxHealthPoints} as the vanilla max (2 points = 1 heart) and {@code healthPoints}
     * filled. Both are already derived from custom health; the implementation only writes them.
     */
    void render(int maxHealthPoints, double healthPoints);
}
