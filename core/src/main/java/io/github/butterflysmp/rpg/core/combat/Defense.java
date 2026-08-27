package io.github.butterflysmp.rpg.core.combat;

/**
 * The defense stat: how much of an incoming hit armor turns away, and where the vanilla armor bar
 * should sit to say so.
 *
 * DIMINISHING RETURNS, not flat subtraction. Damage is scaled by {@code 100/(100+defense)}, so each
 * point of defense is worth slightly less than the one before and the reduction is asymptotic to
 * 100% without ever arriving. That is the whole reason this is a curve and not a subtraction: flat
 * subtraction has two failure modes this cannot have -- enough defense makes you immune, and a small
 * hit against big defense goes negative and HEALS. Neither is reachable here.
 *
 * The two public curves are ONE curve read two ways, and the identity
 * {@code applyDefense(d, x) == d * (1 - damageReduction(x))} holds exactly. Mitigation needs the
 * survivor's fraction; the bar needs the reduced fraction. Deriving both from the same expression is
 * what stops the number the player reads from drifting away from the damage they take.
 *
 * Vanilla armor is the source, and the numbers are DELIBERATELY LOW: a full diamond set is 20 armor,
 * which is ~17% here, not half. Diamond is starter gear in this project, and a starter set that
 * halved incoming damage would leave nothing for real gear to grant. The armor bar reading one sixth
 * full in full diamond is the intended sight, not a bug.
 *
 * Pure, dependency-free, and in {@code core} beside {@link io.github.butterflysmp.rpg.core.combat.stat.HeartScale}
 * -- which encodes the vanilla heart facts the same way {@link #FULL_ARMOR_BAR_POINTS} encodes the
 * vanilla armor-bar fact. Neither needs Bukkit to state a number Minecraft fixed.
 */
public final class Defense {

    private Defense() {}

    /**
     * The half-life constant of the curve. At {@code defense == SCALE} the reduction is exactly 50%,
     * which is what makes the curve readable: "100 defense halves damage" is a sentence a player can
     * hold, and every other value is that sentence bent by diminishing returns.
     */
    public static final double SCALE = 100.0;

    /** A full vanilla armor bar. 20 points, drawn as 10 icons. */
    public static final double FULL_ARMOR_BAR_POINTS = 20.0;

    /**
     * {@code damage} after {@code defense} has taken its cut: {@code damage * 100/(100+defense)}.
     *
     * Non-positive defense returns the damage UNCHANGED rather than running the maths. That guard is
     * load-bearing twice over: an untracked combatant resolves to 0 defense (the summand convention
     * in {@code CombatantStats}), so undefended must mean untouched; and a negative defense from some
     * future debuff would otherwise divide by a number below 1 and AMPLIFY the hit, turning armor
     * shred into a damage buff. A defense of exactly {@code -100} would divide by zero.
     *
     * Worked: {@code 10 @ 0 -> 10} (untouched); {@code 10 @ 100 -> 5} (the half-life);
     * {@code 100 @ 20 -> 83.3} (full vanilla diamond, the starter-gear number).
     */
    public static double applyDefense(double damage, double defense) {
        if (defense <= 0) return damage;
        return damage * SCALE / (SCALE + defense);
    }

    /**
     * The fraction of a hit {@code defense} turns away, in {@code [0, 1)}: {@code defense/(100+defense)}.
     * The same curve as {@link #applyDefense} read as the part removed rather than the part surviving.
     *
     * Never reaches 1.0 for any finite defense, so the bar can never read "fully immune" -- which
     * would be a lie the moment a big enough hit landed anyway.
     *
     * Worked: {@code 0 -> 0.0}; {@code 20 -> 0.1667} (full vanilla diamond); {@code 100 -> 0.5}.
     */
    public static double damageReduction(double defense) {
        if (defense <= 0) return 0.0;
        return defense / (SCALE + defense);
    }

    /**
     * Where the vanilla armor bar should sit for this defense, in armor POINTS: the damage-reduction
     * fraction across a full bar. This is what makes the bar mean DR instead of material.
     *
     * Bounded by {@code [0, 20)} because {@link #damageReduction} is bounded by {@code [0, 1)}, so the
     * result can never approach vanilla's 30-point clamp from either side.
     *
     * Worked: {@code 0 -> 0.0} (empty); {@code 20 -> 3.33} (~1.7 of 10 icons); {@code 100 -> 10.0} (half).
     */
    public static double armorBarPoints(double defense) {
        return damageReduction(defense) * FULL_ARMOR_BAR_POINTS;
    }

    /**
     * The attribute modifier that drags a combatant's NATIVE armor sum to {@link #armorBarPoints},
     * so that {@code nativeArmor + barModifier(...) == armorBarPoints(defense)}.
     *
     * Normally NEGATIVE, and that is the point: worn armor already fills the bar by material, and
     * this cancels that sum before re-filling it by DR. Full diamond natively sums 20 points and
     * wants 3.33, so the modifier is about -16.67.
     *
     * {@code nativeArmor} must be the sum read from the equipped PIECES, never the live armor
     * attribute -- the attribute is what this modifier is applied to, so feeding it back in would
     * have the value chase itself down every scan.
     */
    public static double barModifier(double defense, double nativeArmor) {
        return armorBarPoints(defense) - nativeArmor;
    }
}
