package io.github.butterflysmp.rpg.core.enchant;

/**
 * The custom Unbreaking enchant: a chance to skip a use's durability cost.
 *
 * OURS, not vanilla's. Per the no-vanilla-enchants policy a player-held item can never carry a
 * vanilla enchant here, so there is nothing on the item to delegate to -- the curve is written out.
 * (The old repo took the shortcut, mapping its custom Unbreaking straight onto
 * {@code Enchantment.UNBREAKING} and letting the server apply it. That is exactly what this
 * project's policy forbids, and it is recorded here so nobody re-derives it as a simplification.)
 *
 * <p>The curve mirrors vanilla's own: a use consumes durability with probability
 * {@code 1/(level+1)}, so III consumes one swing in four and skips three. Level 0 is threshold 1.0
 * -- always consume -- which is what makes an unenchanted weapon wear EXACTLY as it did before this
 * class existed, rather than nearly so.
 *
 * <p><b>The draw is not here.</b> The RNG call stays at the impure call site
 * ({@code WeaponDurability.applyWearOnUse}) and this takes the already-drawn double, the same split
 * {@code DamagePopupManager.jitter} uses and for the same payoff: the decision is reddening-testable
 * against exact boundary values with no random source and no seeded fake.
 */
public final class Unbreaking {

    private Unbreaking() {}

    /**
     * The id, which is also the name of its content file ({@code content/enchants/unbreaking.yml}).
     *
     * Hardcoding THIS is not the banned content-in-Java pattern: it is the code side of a code-owned
     * effect binding, the standing {@code EffectSpec}'s subtypes already have. Content names an
     * effect and bounds it; it never defines one. The display name and the maximum level -- the
     * parts that are genuinely data -- live in the yml.
     */
    public static final String ID = "unbreaking";

    /**
     * The probability that one use CONSUMES durability: {@code 1/(level+1)}.
     *
     * Worked: 0 -&gt; 1.0 (always); I -&gt; 0.5; II -&gt; 0.333...; III -&gt; 0.25.
     *
     * <p>Feeds the DURABILITY arm of the dev command's effect line ({@code EnchantEffectLine}) --
     * "consumes durability on 25% of uses" -- so the boot gate can read the expected rate off the
     * screen BEFORE swinging rather than deciding afterwards what the number it got should have
     * been. A damage enchant's line comes from {@code DamageEnchants} instead.
     *
     * <p>That "and only that arm" is written narrowly on purpose. The broad version of this
     * sentence -- "what the dev command prints back" -- was true of every effect, and it outlived a
     * bug in which {@code /rpg enchant active} appended this rate to EVERY enchant, Sharpness
     * included.
     */
    public static double consumeChance(int level) {
        if (level <= 0) return 1.0;
        // Clamped, not trusted. At level 99 the threshold would be 0.01 and a hand-edited or
        // future-build item would be effectively indestructible.
        return 1.0 / (Math.min(level, EnchantState.MAX_LEVEL) + 1);
    }

    /**
     * Does this use consume durability? {@code roll} is a draw from {@code [0,1)}.
     *
     * <p>Two guards, each preventing a weapon that never wears:
     *
     * <ul>
     *   <li>{@code level <= 0} returns true directly. At level 0 this agrees with the formula, so
     *       the branch looks redundant -- it is not. At a NEGATIVE level (a corrupt blob, a
     *       hand-edited item) {@code 1.0/(-2+1)} is {@code -1.0}, and {@code roll < -1.0} is false
     *       for every roll: the weapon would become INDESTRUCTIBLE. The guard is the only thing
     *       standing between a malformed level and free durability forever.</li>
     *   <li>the clamp inside {@link #consumeChance}, for the same failure from the other end.</li>
     * </ul>
     *
     * <p><b>Strict {@code <}, not {@code <=}.</b> The source is {@code ThreadLocalRandom.nextDouble()},
     * which is half-open {@code [0,1)}, so with {@code <} the measure of the consuming set is
     * exactly the threshold. {@code <=} would consume on one extra point.
     *
     * <p>A roll outside the unit interval is not reachable from that source and is total anyway: a
     * negative roll consumes, which fails toward WEARING rather than toward free durability.
     */
    public static boolean consumes(int level, double roll) {
        if (level <= 0) return true;
        return roll < consumeChance(level);
    }
}
