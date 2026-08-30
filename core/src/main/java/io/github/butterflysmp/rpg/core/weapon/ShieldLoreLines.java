package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.combat.Shield;

/**
 * The plain-text half of the shield tooltip: pure String/number formatters over the content model.
 * No Adventure, no Bukkit -- lives in core so it runs in the 2-second test loop, and the paper
 * {@code ShieldLore} builder only wraps these in colour and layout. Same split, and same reason, as
 * {@link WeaponLoreLines}.
 *
 * <p>Like its weapon counterpart this describes the ITEM, never the holder: the number here is the
 * shield's authored {@code block_dr}, not any resolved stat, so it is mint-time safe and cannot
 * drift between two players reading the same shield.
 */
public final class ShieldLoreLines {

    private ShieldLoreLines() {}

    /**
     * The stat's name, as its own constant because the tooltip colours the LABEL and the VALUE
     * differently -- gray label, green number, the idiom {@code WeaponLore}'s damage line already
     * uses. Trailing space included so the caller concatenates rather than remembering to.
     *
     * <p><b>"Damage Reduction", not "Block".</b> It is the same quantity either way, but the shield
     * is not the only thing in this game that reduces incoming damage -- armor Defense does too, and
     * the two compose. Naming this what it IS rather than what produces it lets a player read the
     * shield and the armor as the same kind of number. {@code EnchantEffectLine}'s Bulwark arm uses
     * the identical words, so the item and the enchant that modifies it cannot describe the same
     * stat two ways.
     */
    public static final String DAMAGE_REDUCTION_LABEL = "Damage Reduction: ";

    /** The value half: {@code 0.35 -> "35%"}. Coloured, where {@link #DAMAGE_REDUCTION_LABEL} is not. */
    public static String damageReductionValue(double blockDr) {
        return blockPercent(blockDr) + "%";
    }

    /**
     * Label and value in one string, for anything that is not colouring them separately.
     * {@code 0.35 -> "Damage Reduction: 35%"}.
     *
     * <p><b>Rounded to one decimal before trimming, and that is not cosmetic.</b> The content file
     * authors a FRACTION and the tooltip shows a PERCENT, so the formatter has to multiply by 100 --
     * which is exactly where binary floating point stops agreeing with the number the author typed.
     * Measured, before this method was written:
     *
     * <pre>
     *   block_dr: 0.29  ->  0.29 * 100  ==  28.999999999999996
     *   block_dr: 0.55  ->  0.55 * 100  ==  55.00000000000001
     * </pre>
     *
     * A naive trim-trailing-zeros would print those verbatim on the item. The shipped shield's 0.35
     * multiplies cleanly to 35.0, so this would have looked correct in every boot gate and waited
     * for the first person to author an odd fraction. One decimal is kept rather than zero so a
     * genuinely fractional percent survives -- {@code 0.125} is a real "12.5%", not a rounding
     * error -- and the trim then drops the {@code .0} from the whole ones.
     *
     * <p>Worked: {@code 0 -> "Damage Reduction: 0%"}; {@code 0.35 -> "Damage Reduction: 35%"};
     * {@code 0.125 -> "Damage Reduction: 12.5%"}; {@code 1 -> "Damage Reduction: 100%"}.
     */
    public static String damageReductionLabel(double blockDr) {
        return DAMAGE_REDUCTION_LABEL + damageReductionValue(blockDr);
    }

    /**
     * {@code blockDr} as a percent string, clamped and rounded: {@code 0.5 -> "50"}.
     *
     * Goes through {@link Shield#clamp} rather than trusting its argument, so the tooltip can never
     * advertise a negative or over-full block even for an item that somehow carries one. The
     * tooltip and the arithmetic then agree by construction, because they clamp through the same
     * method -- the drift {@code WeaponLoreLines} guards against by reading the definition rather
     * than the holder, in its own idiom.
     */
    public static String blockPercent(double blockDr) {
        double percent = Shield.clamp(blockDr) * 100.0;
        double rounded = Math.round(percent * 10.0) / 10.0;
        return trimNumber(rounded);
    }

    /** {@code 50.0 -> "50"}, {@code 12.5 -> "12.5"}. The idiom WeaponLoreLines uses for its costs. */
    private static String trimNumber(double n) {
        if (n == Math.floor(n) && !Double.isInfinite(n)) return String.valueOf((long) n);
        return String.valueOf(n);
    }
}
