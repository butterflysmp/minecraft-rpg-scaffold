package io.github.butterflysmp.rpg.core.weapon;

/**
 * WHAT A VANILLA-DRIVEN BOW OR CROSSBOW SHOT DOES (the seat's rulings 7 to 9, 2026-11-22, with B and C of the same day).
 * The decision is pure; {@code VanillaShots} in paper reads the shot event and performs the plan.
 *
 * <p>Vanilla has ALREADY TAKEN one projectile when the shot event fires (READ, the pinned jar: {@code
 * BowItem.releaseUsing} calls {@code useAmmo} before {@code shoot}, and no flag of the event gives it back; a crossbow
 * took it at its load). Vanilla picks it from the OFF HAND, then the MAIN HAND, then inventory slots 0 upward (READ,
 * {@code Player.getProjectile}), so it may be the Nexus mirror or an arrow of his own. Every answer is therefore about
 * what to GIVE BACK and what to SPEND:
 *
 * <ul>
 *   <li><b>Off hand</b> (ruling C): cancelled; whatever vanilla took is given back (the mirror needs no giving back: the
 *       slot is re-drawn from the weapon's count). Nothing spent.</li>
 *   <li><b>No quiver</b> (the Crossbow): vanilla's shot, untouched, pickup as vanilla set it, unless it carries a mirror
 *       arrow, which is cancelled (no shot nobody paid for).</li>
 *   <li><b>A quiver at 0 rounds</b> (ruling 8): cancelled, given back, the empty notice.</li>
 *   <li><b>A quiver with rounds</b>: one round spent; the arrow can never be picked up (ruling B: a round spent is gone);
 *       given back only when vanilla took a PLAIN arrow of his own. Tipped or spectral: not given back, its effect left
 *       to vanilla (SEAT PLACEHOLDER, OPEN FOR BEN: it stops one tipped arrow giving unlimited tipped shots). The
 *       mirror: nothing to give back.</li>
 * </ul>
 */
public final class VanillaShot {

    private VanillaShot() {}

    /** What vanilla took for this shot. OTHER is anything that is not a plain arrow: tipped, spectral, a firework. */
    public enum Consumable { MIRROR, PLAIN_ARROW, OTHER }

    /**
     * @param cancel      the shot does not leave the bow
     * @param refund      give back one of what vanilla took
     * @param spendRound  spend one round of the held weapon's quiver
     * @param quiverPaid  a round paid for this shot: its arrow can never be picked up (ruling B)
     * @param emptyNotice send the existing empty-quiver notice
     */
    public record Plan(boolean cancel, boolean refund, boolean spendRound, boolean quiverPaid, boolean emptyNotice) {}

    private static final Plan LEAVE = new Plan(false, false, false, false, false);

    public static Plan of(boolean offHand, boolean hasQuiver, int roundsLoaded, Consumable taken) {
        boolean givesBack = taken != Consumable.MIRROR;
        if (offHand) return new Plan(true, givesBack, false, false, false);
        if (!hasQuiver) {
            return taken == Consumable.MIRROR ? new Plan(true, false, false, false, false) : LEAVE;
        }
        if (roundsLoaded <= 0) return new Plan(true, givesBack, false, false, true);
        return new Plan(false, taken == Consumable.PLAIN_ARROW, true, true, false);
    }

    /**
     * The charge term of the hit's sum: the shot's force, never above 1. READ: a bow's force is its draw power, 0.1 to 1
     * ({@code BowItem.getPowerForTime}, passed through {@code shoot} to the event); a crossbow's is the constant 1
     * ({@code CrossbowItem.performShooting}). So "14 x force" (SEAT PLACEHOLDER) and the Crossbow's flat 37 are one rule.
     */
    public static double chargeScale(double force) {
        if (!(force > 0)) return 0.0;   // NaN and negatives
        return Math.min(force, 1.0);
    }
}
