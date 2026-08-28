package io.github.butterflysmp.rpg.core.combat;

/**
 * How much of a basic melee hit a swing has EARNED by the time it lands.
 *
 * <p>Vanilla does not let you spam a sword at full power: the attack-strength meter refills over
 * the weapon's attack-speed period, and a swing thrown before it fills is scaled down. This is that
 * rule, as a pure function, so the basic melee hit rewards timing instead of click rate.
 *
 * <p><b>A curve, not a gate.</b> The alternative -- refuse the swing until the meter is full -- is
 * what the retired {@code CooldownTracker} path did, and the two are mutually exclusive: under a
 * gate, every swing that is ALLOWED is a full-charge swing, so a charge multiplier would be dead
 * code. Vanilla's model is "you may always swing, but an early one is weak," and that is the one
 * that makes a timed swing feel earned.
 *
 * <p><b>Provenance -- this curve was MEASURED, not recalled.</b> On the 2026-08-28 Step 0 boot, a
 * vanilla iron sword (attack damage 6.0) swung at a read charge of 0.76 dealt a raw 3.9725, and the
 * same sword at charge 1.0 dealt 6.0. {@code 6.0 * (0.2 + 0.76^2 * 0.8) = 3.97248}. The constants
 * below are that observation, not a half-remembered decompile.
 *
 * <p><b>Where it applies.</b> The scale multiplies the WHOLE combined amount in
 * {@code EffectApplier} -- {@code (attackDamage * enchantMultiplier + classDamageBonus) * scale} --
 * and it is the LAST transform. Scaling only the weapon base would leave the flat class bonus as a
 * spam-proof damage floor, and with enough {@code +N Melee} gear more swings would beat timed
 * swings: the model inverted. Deliberate divergence from vanilla, which scales its base by this
 * curve and its enchantment bonus linearly by the raw charge -- our enchant is a percentage
 * multiplier rather than additive Sharpness, so one curve over the whole amount is the coherent
 * reading. {@link #FULL_CHARGE} is exactly 1.0 in binary floating point (verified by execution,
 * not by algebra), so an ability or projectile passing 1.0 is an EXACT identity, not a near one.
 *
 * <p>Worked: {@code scale}: {@code 0 -> 0.2}; {@code 0.25 -> 0.25}; {@code 0.5 -> 0.4};
 * {@code 0.76 -> 0.66208}; {@code 1 -> 1.0}.
 * {@code apply}: {@code 8 @ 0 -> 1.6}; {@code 8 @ 0.5 -> 3.2}; {@code 8 @ 1 -> 8.0}.
 */
public final class AttackCharge {

    private AttackCharge() {}

    /**
     * What a completely uncharged swing still lands. Load-bearing at BOTH ends: drop it and an
     * instant re-click deals a flat zero, which reads as a broken weapon rather than a punished
     * one -- and it is the reason spam is weak instead of free.
     */
    public static final double MIN_SCALE = 0.2;

    /** The share of the hit the charge itself is worth. MIN_SCALE + CHARGED_SHARE == 1.0. */
    public static final double CHARGED_SHARE = 0.8;

    /** A fully wound-up swing. The neutral value every non-basic-melee caller passes. */
    public static final double FULL_CHARGE = 1.0;

    /**
     * The multiplier a swing at {@code charge} has earned, on the vanilla curve.
     *
     * <p>The clamp is load-bearing in both directions and guards an OPEN input: the charge arrives
     * from {@code Player#getAttackCooldown()} across a thread hop and a stash, so a stale or
     * absent read must not become damage amplification. Above 1.0 the quadratic would multiply the
     * hit UP without it (charge 2.0 would deal 3.4x); below 0.0 the square silently flips a
     * negative back to a positive, so a -1.0 would read as a full-power swing rather than the
     * weakest possible one.
     *
     * <p>Quadratic, not linear: it is what keeps the last third of the wind-up worth waiting for.
     * Linear would make a half-charged swing worth 0.6 rather than 0.4, and the timing would stop
     * mattering.
     */
    public static double scale(double charge) {
        double clamped = Math.max(0.0, Math.min(FULL_CHARGE, charge));
        return MIN_SCALE + clamped * clamped * CHARGED_SHARE;
    }

    /** {@code damage} reduced by how little of the swing was earned. */
    public static double apply(double damage, double charge) {
        return damage * scale(charge);
    }
}
