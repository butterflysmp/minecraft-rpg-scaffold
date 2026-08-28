package io.github.butterflysmp.rpg.core.combat;

/**
 * Turning a weapon's authored cadence and its wielder's attack-speed stat into the single vanilla
 * attack-speed modifier that paces a basic melee swing.
 *
 * <p>The sibling of {@link Defense#barModifier}, and here for the same reason: the paper side is
 * attribute plumbing that needs a live {@code Player} and can only be boot-witnessed, so the
 * arithmetic it depends on lives in core where it can be reddened. If a swing ever feels wrong, this
 * is the half that can be proven; the other half is one add/remove call.
 *
 * <p><b>Why a modifier and not a value.</b> Vanilla has no "set the attribute" call -- an attribute
 * is a base plus modifiers -- so expressing a desired total means subtracting the base off it. The
 * player's base is {@link #VANILLA_BASE} (4.0, a bare fist), and the weapon contributes nothing of
 * its own: a minted weapon carries an explicit attack-damage modifier, which replaces the item's
 * whole default block including whatever speed its material would have given. That was measured on
 * the 2026-08-28 boot -- a minted ironblade read 4.0 where a plain iron sword read 1.6 -- and it is
 * what makes this a single modifier owning the entire value rather than a delta on the item's.
 *
 * <p><b>Why multiply and not add.</b> The stat is a MULTIPLIER basing at 1.0 (see
 * {@code AttackSpeed.BASE}), so a source granting "+100% attack speed" resolves to 2.0 and doubles
 * the weapon's own cadence. Adding it instead would make a boost worth the same on a fast weapon as
 * on a slow one, which is not what a percentage means.
 *
 * <p>Worked, for an ironblade at 1.6 with the dev boost's resolved 2.0:
 * {@code desiredSpeed(1.6, 1.0) -> 1.6}; {@code modifier(1.6, 1.0) -> -2.4};
 * {@code desiredSpeed(1.6, 2.0) -> 3.2}; {@code modifier(1.6, 2.0) -> -0.8}.
 */
public final class AttackSpeedAttribute {

    private AttackSpeedAttribute() {}

    /**
     * A player's base attack speed with nothing held: 4.0 attacks per second.
     *
     * Every modifier below is expressed relative to this, so it is not a tuning number and must not
     * be "balanced" -- change it and the modifier stops producing the total it names.
     */
    public static final double VANILLA_BASE = 4.0;

    /**
     * The attack speed a wielder should actually swing at: the weapon's authored cadence scaled by
     * the wielder's resolved multiplier.
     *
     * <p>A non-positive {@code weaponBaseSpeed} means the weapon declares no melee cadence (a bow, a
     * staff, or nothing held at all) and returns 0 -- the caller's signal to write NO modifier and
     * leave the player's vanilla base untouched, rather than to write a zero. Absent, not zeroed:
     * the same discipline the equipment scan follows, and the difference between a player who has
     * never held one of our weapons carrying no trace of this plugin and one who carries a 0.
     */
    public static double desiredSpeed(double weaponBaseSpeed, double statMultiplier) {
        if (weaponBaseSpeed <= 0) return 0.0;
        return weaponBaseSpeed * statMultiplier;
    }

    /**
     * The {@code ADD_NUMBER} modifier amount that drives the attribute to {@link #desiredSpeed}.
     *
     * Normally NEGATIVE, and that is not a bug to be "fixed": every real weapon is slower than a
     * bare fist, so an ironblade at 1.6 pins -2.4. A positive result means the wielder is swinging
     * faster than an empty hand, which a large enough boost legitimately produces.
     */
    public static double modifier(double weaponBaseSpeed, double statMultiplier) {
        return desiredSpeed(weaponBaseSpeed, statMultiplier) - VANILLA_BASE;
    }
}
