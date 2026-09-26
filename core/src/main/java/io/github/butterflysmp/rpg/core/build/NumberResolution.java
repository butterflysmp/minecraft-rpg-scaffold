package io.github.butterflysmp.rpg.core.build;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * THE ONE FORMULA, for every field and any number of aspects (PLAN-build-system.md section 2.4.1):
 *
 * <pre>
 *   resolved = (base + sum of flat) x (1 + sum of percent / 100)
 * </pre>
 *
 * <p>Flats add with flats and percents with percents (two +10% make +20%, not +21%); the one multiplication
 * comes after both sums, so the order of aspects cannot matter.
 *
 * <p><b>EXACT DECIMAL ARITHMETIC, and that is part of "refuse, never round".</b> Every authored number is a
 * short decimal, so it is taken with {@link BigDecimal#valueOf(double)} and resolved exactly: 200 x 1.20 is
 * 240, not 240.00000000000003. A {@code double} here would make the integer check refuse a legal 240, or pass
 * an illegal value by a tolerance nobody authored.
 *
 * <p><b>REFUSE, NEVER ROUND.</b> A rounded value is a number nobody authored, and the tooltip would advertise
 * it. {@link #refusal} names the resolved value and, where the rule is a grid, the nearest legal values on
 * either side.
 */
public final class NumberResolution {

    private NumberResolution() {}

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** The resolved value of one field on one base, over every change to that field. */
    public static BigDecimal resolve(double base, List<NumberChange> changes) {
        BigDecimal flat = BigDecimal.ZERO;
        BigDecimal percent = BigDecimal.ZERO;
        for (NumberChange change : changes) {
            flat = flat.add(BigDecimal.valueOf(change.flat()));
            percent = percent.add(BigDecimal.valueOf(change.percent()));
        }
        BigDecimal factor = BigDecimal.ONE.add(percent.divide(HUNDRED));
        return BigDecimal.valueOf(base).add(flat).multiply(factor);
    }

    /**
     * What a field's legality depends on beyond its value: the cast's cooldown floor ({@code cooldown_ticks})
     * or the area's tick interval ({@code area.duration_ticks}). {@link #NONE} for every other field.
     */
    public record Context(int cooldownFloor, int tickInterval) {
        public static final Context NONE = new Context(0, 0);
        public static Context cooldown(int floor) { return new Context(floor, 0); }
        public static Context interval(int tickInterval) { return new Context(0, tickInterval); }
    }

    /** Why this resolved value is illegal for its field, or empty if it is legal. */
    public static Optional<String> refusal(AspectField field, BigDecimal resolved, Context context) {
        String value = plain(resolved);
        return switch (field) {
            case COOLDOWN_TICKS -> {
                if (!isWhole(resolved)) yield Optional.of(field.token() + " resolves to " + value
                        + ", not a whole number of ticks" + grid(resolved, 4));
                if (resolved.signum() <= 0) yield Optional.of(field.token() + " resolves to " + value + ", not above 0");
                if (resolved.remainder(BigDecimal.valueOf(4)).signum() != 0) {
                    yield Optional.of(field.token() + " resolves to " + value + ", not a multiple of 4 (held"
                            + " right-click inputs land on a 4-tick grid)" + grid(resolved, 4));
                }
                if (resolved.compareTo(BigDecimal.valueOf(context.cooldownFloor())) < 0) {
                    yield Optional.of(field.token() + " resolves to " + value + ", below this cast's floor of "
                            + context.cooldownFloor() + " ticks");
                }
                yield Optional.empty();
            }
            case COST -> resolved.signum() < 0
                    ? Optional.of(field.token() + " resolves to " + value + ", below 0 (0 is free)")
                    : Optional.empty();
            case DAMAGE_AMOUNT, HEAL_AMOUNT, KNOCKBACK_STRENGTH, BURST_RADIUS, AREA_RADIUS -> resolved.signum() <= 0
                    ? Optional.of(field.token() + " resolves to " + value + ", not above 0 -- a change that does nothing")
                    : Optional.empty();
            case AREA_DURATION_TICKS -> {
                int interval = context.tickInterval();
                if (!isWhole(resolved) || resolved.signum() <= 0
                        || (interval > 0 && resolved.remainder(BigDecimal.valueOf(interval)).signum() != 0)) {
                    yield Optional.of(field.token() + " resolves to " + value + "; an area pulses every " + interval
                            + " ticks, so its duration must be a positive multiple of " + interval
                            + grid(resolved, Math.max(1, interval)));
                }
                yield Optional.empty();
            }
            case STATUS_DURATION_TICKS -> (!isWhole(resolved) || resolved.signum() <= 0)
                    ? Optional.of(field.token() + " resolves to " + value + ", not a whole number of ticks above 0"
                            + grid(resolved, 1))
                    : Optional.empty();
        };
    }

    /** The resolved value as an int, for an integer field that {@link #refusal} has passed. */
    public static int intValue(BigDecimal resolved) {
        return resolved.setScale(0, RoundingMode.UNNECESSARY).intValueExact();
    }

    private static boolean isWhole(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= 0;
    }

    /** ", nearest legal L and U": the multiples of {@code step} either side (only those above 0). */
    private static String grid(BigDecimal value, int step) {
        BigDecimal s = BigDecimal.valueOf(step);
        BigDecimal lower = value.divide(s, 0, RoundingMode.FLOOR).multiply(s);
        BigDecimal upper = lower.add(s);
        if (lower.compareTo(value) == 0) upper = lower;   // on the grid; the other test failed
        return lower.signum() > 0
                ? "; nearest legal " + plain(lower) + " and " + plain(upper)
                : "; nearest legal " + plain(upper.signum() > 0 ? upper : s);
    }

    /** "9", not "9.00" or "9E+0". */
    public static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
