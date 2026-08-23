package io.github.butterflysmp.rpg.core.ability;

/**
 * How the attack-speed stat turns a weapon's authored swing cadence into the cooldown actually
 * applied.
 *
 * MULTIPLIER semantics, base 1.0: {@code cooldown_ticks} stays the authored base cadence in content,
 * and the stat scales it. A resolved 2.0 halves the gap between swings; 0.5 doubles it. That is why
 * this is a divisor and not, say, an attacks-per-second value replacing the content number -- a
 * weapon's yml keeps meaning what it says, and the stat is a modifier on top.
 *
 * Applies to BASIC ATTACKS only ({@link io.github.butterflysmp.rpg.core.ability.effect.DamagePayload#isBasicAttack}).
 * An ability's declared cooldown is its balance, not a swing rate, and is never scaled here.
 */
public final class AttackSpeed {

    private AttackSpeed() {}

    /** A combatant with no modifiers. Neutral, so the maths below is an identity at rest. */
    public static final double BASE = 1.0;

    /**
     * The slowest a stat may make you. A stacked pile of negative modifiers could otherwise reach 0
     * (an infinite cooldown) or go negative (a negative cooldown, i.e. none at all -- a debuff that
     * loops around into the strongest possible buff). Clamping the divisor is what makes that
     * impossible regardless of what modifiers exist later.
     */
    public static final double MIN_SPEED = 0.1;

    /**
     * The cooldown to actually apply for a basic attack swung at {@code attackSpeed}.
     *
     * Three guards, each load-bearing:
     *
     *  - A declared cooldown of 0 (or less) is returned UNCHANGED. Content saying "no cooldown"
     *    must stay no cooldown; running it through the maths below would floor it to 1 and silently
     *    gate a trigger the content deliberately left ungated.
     *  - {@code attackSpeed} is clamped to {@link #MIN_SPEED} before dividing, so no modifier pile
     *    can produce a divide-by-zero, an infinity, or a negative.
     *  - The result floors at 1 tick. A large buff must not reach 0, which would remove the cooldown
     *    entirely and turn a fast weapon into an unthrottled one.
     *
     * Worked: {@code 10 @ 1.0 -> 10}; {@code 10 @ 2.0 -> 5}; {@code 10 @ 0.5 -> 20};
     * {@code 1 @ 5.0 -> 1} (floor); {@code 0 @ 2.0 -> 0} (stays ungated).
     */
    public static int effectiveCooldownTicks(int cooldownTicks, double attackSpeed) {
        if (cooldownTicks <= 0) return cooldownTicks;
        double speed = Math.max(MIN_SPEED, attackSpeed);
        return (int) Math.max(1, Math.round(cooldownTicks / speed));
    }
}
