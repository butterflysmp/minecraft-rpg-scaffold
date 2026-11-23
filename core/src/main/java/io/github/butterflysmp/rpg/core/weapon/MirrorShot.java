package io.github.butterflysmp.rpg.core.weapon;

/**
 * WHAT A BOW OR CROSSBOW SHOT MAY DO WITH THE NEXUS MIRROR (the seat's rulings B and C, 2026-11-22). The decision is pure;
 * {@code RpgListeners} reads the shot and performs it.
 *
 * <ul>
 *   <li><b>A shot PAID BY A QUIVER ROUND</b> (the mirror spent by the weapon whose rounds it shows, or a backstop shot
 *       whose own plain arrow was given back): {@link Act#NO_PICKUP}. A round spent is gone; its arrow is never an item
 *       again (ruling B, which withdraws the earlier "eight shots, eight pickups").</li>
 *   <li><b>Any OTHER shot whose consumable is the mirror</b> (a bow that pays no round, say a plain bow in the off hand):
 *       {@link Act#CANCEL}. Vanilla has already split a mirror arrow off by the time the event fires (READ, the pinned
 *       jar), and the slot is re-drawn from the held weapon's count, so cancelling loses nothing and lets no mirror arrow
 *       leave as a shot nobody paid for.</li>
 *   <li>Otherwise, {@link Act#LEAVE}: vanilla's shot, pickup as vanilla set it (the Crossbow's own arrows).</li>
 * </ul>
 */
public final class MirrorShot {

    private MirrorShot() {}

    public enum Act { LEAVE, CANCEL, NO_PICKUP }

    /**
     * @param consumableIsNexus the shot's consumable carries the Nexus tag (it was a mirror arrow)
     * @param paidByQuiverRound a quiver round was spent for this shot
     */
    public static Act of(boolean consumableIsNexus, boolean paidByQuiverRound) {
        if (paidByQuiverRound) return Act.NO_PICKUP;
        if (consumableIsNexus) return Act.CANCEL;
        return Act.LEAVE;
    }
}
