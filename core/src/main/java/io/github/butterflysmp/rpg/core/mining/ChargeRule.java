package io.github.butterflysmp.rpg.core.mining;

/**
 * When a swing counts as fully charged. <b>The seat's ruling 2, 2026-11-13: exactly 1.0f.</b>
 *
 * <p>{@code HumanEntity#getAttackCooldown()} clamps at 1.0 (PLAN-mining.md F1), so a full charge
 * reads exactly 1.0f. There is no tolerance on purpose: at attack speed 1.2 the read one tick early
 * is 0.99, and {@code >= 0.99} would accept it.
 *
 * <p>This is the ONE place the rule lives, so a later grace window is a one-line, visible change
 * here, and {@code SpotBoxAndChargeTest} reddens when it happens. Gate row M-CHARGE-17 reads it in
 * game.
 */
public final class ChargeRule {

    /** The only cooldown reading that counts as charged. */
    public static final float FULL_CHARGE = 1.0f;

    private ChargeRule() {}

    public static boolean isCharged(float cooldown) {
        return cooldown == FULL_CHARGE;
    }
}
