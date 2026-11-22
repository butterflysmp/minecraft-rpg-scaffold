package io.github.butterflysmp.rpg.core.weapon;

import java.util.OptionalInt;

/**
 * THE QUIVER IN THE NEXUS SLOT (Ben, 2026-11-22: "Any time a bow is in the players hand, the menu item (our nexus menu)
 * turns into arrows of a stack size equal to the remaining Quiver shots"; to the seat: "1. Any weapon with a Quiver",
 * "2. Yes, all that changes is the item material"). THE DECISION, pure: what the star's locked slot shows.
 *
 * <p><b>The weapon's own stamped count is the truth; the slot is a MIRROR of it</b> (the seat's ruling 1). Nothing reads
 * the quiver from the slot; this class is handed the count and says what to draw.
 *
 * <ul>
 *   <li>Star OFF: {@link Shown.None} -- there is no Nexus item to change.</li>
 *   <li>The main hand holds no quiver weapon (no stamped count): {@link Shown.Star}.</li>
 *   <li>Zero rounds: {@link Shown.Star}, never a stack of zero (ruling 2).</li>
 *   <li>Otherwise {@link Shown.Arrows} of the rounds, capped at {@link #MAX_SHOWN}: an item's max stack size is 1 to 99
 *       (READ, the pinned jar: {@code CraftMetaItem.setMaxStackSize} -> {@code BoundChecker.requireRange(…, 1, 99)}).
 *       The weapon's count is unaffected by the cap.</li>
 * </ul>
 */
public final class QuiverMirror {

    private QuiverMirror() {}

    /** The largest stack the slot can show: an item's max stack size may not exceed 99 (READ, the pinned jar). */
    public static final int MAX_SHOWN = 99;

    /** What the star's slot shows. */
    public sealed interface Shown {
        /** The star is off: nothing of ours is in the slot, so nothing is drawn. */
        record None() implements Shown {}
        /** The Nexus star, as always. */
        record Star() implements Shown {}
        /** The same Nexus item as ARROWS, this many. */
        record Arrows(int count) implements Shown {}
    }

    /**
     * @param starOn     is his Nexus star on
     * @param heldRounds the rounds loaded in the weapon in his MAIN hand, or empty when it carries no quiver count
     */
    public static Shown decide(boolean starOn, OptionalInt heldRounds) {
        if (!starOn) return new Shown.None();
        if (heldRounds == null || heldRounds.isEmpty() || heldRounds.getAsInt() <= 0) return new Shown.Star();
        return new Shown.Arrows(Math.min(heldRounds.getAsInt(), MAX_SHOWN));
    }

    /**
     * THE WARNING (Ben: "Fall back and add a warning message that shows only to the player. Only show the message when
     * the player have a quiver-based weapon in their inventory and their nexus menu is off"). Sent on an EVENT, never
     * more than once per event: the first observation after his JOIN (nothing observed yet), the observation that finds
     * he has just TURNED THE STAR OFF (it was on last time), and the moment a quiver weapon first REACHES his inventory
     * (none last time, one now). Each is read off the previous observation, so no menu has to tell anyone.
     *
     * @param lastHad    whether the previous observation found a quiver weapon; null when nothing was observed yet
     * @param lastStarOn whether the star was on at the previous observation; null when nothing was observed yet
     * @param hasNow     whether his inventory holds a quiver weapon now
     * @param starOn     is his Nexus star on now
     */
    public static boolean warnNow(Boolean lastHad, Boolean lastStarOn, boolean hasNow, boolean starOn) {
        if (starOn || !hasNow) return false;
        return lastHad == null || lastStarOn == null || !lastHad || lastStarOn;
    }
}
