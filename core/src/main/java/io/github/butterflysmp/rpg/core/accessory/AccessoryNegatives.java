package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.HealthRegen;

/**
 * Whether a NEGATIVE modifier -- a drawback -- may load. Ruling Q7, the plan's §3.3.
 *
 * <p><b>The rule: a drawback must be a real reduction, always.</b> Two ways it can fail to be one:
 *
 * <ul>
 *   <li><b>The stat has no floor</b> -- max HP. {@code HealthState.max()} is {@code base + sum} with
 *       no clamp, so a large enough drawback reaches zero or below. Refused outright in v1.
 *   <li><b>The stat clamps where it is read</b> -- crit chance clamps at 0, defense ignores a value
 *       at or below 0. A drawback that meets the clamp reads "-5" on the tooltip and does nothing.
 * </ul>
 *
 * <h2>The bound, for {@link AccessoryStat.Negatives#BOUNDED} stats</h2>
 *
 * {@code COUNT x |amount| < base}: four slots each carrying the same drawback still cannot reach
 * the player's base. Conservative -- it ignores every other source -- and mechanical, which is the
 * point: a drawback that loads is a real reduction by construction, so the tooltip is honest without
 * anyone checking it.
 *
 * <p>The max-mana base is not a core constant -- it lives in {@code RpgPlugin} beside the pool it
 * sizes, and {@code NEXT.md} records it becoming archetype content -- so the caller supplies it
 * rather than this class keeping a second copy of the number.
 */
public final class AccessoryNegatives {

    private AccessoryNegatives() {}

    /**
     * The player base a BOUNDED stat's drawback is measured against.
     *
     * @param maxManaBase the base mana pool, supplied by the caller (see the class note)
     * @throws IllegalArgumentException for a stat whose negatives are refused -- it has no bound
     */
    public static double base(AccessoryStat stat, double maxManaBase) {
        return switch (stat) {
            case MAX_MANA -> maxManaBase;
            case CRIT_CHANCE -> Crit.BASE_CHANCE;
            case CRIT_DAMAGE -> Crit.BASE_DAMAGE;
            case HEALTH_REGEN -> HealthRegen.BASE_PER_SECOND;
            case MAX_HEALTH, MANA_REGEN, DEFENSE, CLASS_DAMAGE -> throw new IllegalArgumentException(
                    stat.token() + " has no bound: its negatives are refused outright");
        };
    }

    /**
     * Null when {@code amount} may load on {@code stat}; otherwise the reason it may not, as a
     * sentence the loader appends to the file's name.
     */
    public static String refusal(AccessoryStat stat, double amount, double maxManaBase) {
        if (!(amount < 0)) return null;   // non-negative, or NaN -- NaN is the definition's to refuse
        return switch (stat.negatives()) {
            case REFUSED -> stat == AccessoryStat.MAX_HEALTH
                    ? "a negative max_health is refused in v1: max HP has no floor, so a drawback"
                            + " there can reach zero"
                    : "a negative " + stat.token() + " is refused: it cannot be guaranteed to be a"
                            + " real reduction";
            case BOUNDED -> {
                double base = base(stat, maxManaBase);
                yield AccessorySlots.COUNT * -amount < base ? null
                        : "a negative " + stat.token() + " of " + amount + " is too large: "
                                + AccessorySlots.COUNT + " slots of it must stay below the player"
                                + " base " + base + ", or the drawback meets the stat's clamp";
            }
        };
    }
}
