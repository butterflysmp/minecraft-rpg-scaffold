package io.github.butterflysmp.rpg.core.enchant;

/**
 * WHICH code-owned mechanism an enchant binds to. A fixed, closed axis, so an enum -- exactly like
 * {@code WeaponClass} and {@code Rarity}, and the opposite of the open, logic-free element string.
 *
 * <p>This is the typed schema decision Pass 1 deferred to this pass. Its javadoc, and
 * {@code unbreaking.yml}'s comment block, both said the same thing: content NAMES an effect and
 * bounds it, it never DEFINES one. That rule is unchanged. What this enum adds is the ability for
 * content to name a SECOND mechanism, where before there was only one and the binding could be a
 * bare id ({@link Unbreaking#ID}).
 *
 * <p><b>Why the damage enchants' numbers are data while Unbreaking's curve is Java.</b> The question
 * is not "numbers versus code", it is how many enchants share one mechanism. Unbreaking is one
 * enchant with one curve, so the curve is the mechanism and lives in {@link Unbreaking}. Sharpness,
 * Power and Attunement are THREE enchants sharing ONE mechanism ({@link DamageEnchants}), differing
 * only in a class gate and three percentages. A Java class each would be three copies of the same
 * arithmetic, and adding the fourth would be a recompile -- exactly what invariant 2 forbids. So
 * {@code DAMAGE} names the mechanism and content supplies its parameters, the same way
 * {@code max_level} was already a parameter rather than a mechanism.
 *
 * <p>Adding a constant here means adding a mechanism, which is why they live in the same package:
 * one place to look for what an enchant can DO.
 */
public enum EnchantEffect {

    /** A chance to skip a use's durability cost. The mechanism is {@link Unbreaking}. */
    DURABILITY,

    /**
     * A multiplier on the damage of the weapon it sits on, gated on that weapon's class. The
     * mechanism is {@link DamageEnchants}.
     */
    DAMAGE,

    /**
     * A bonus ADDED to the block fraction of the shield it sits on. The mechanism is
     * {@link Bulwark}, and it is read off the blocking stack in the mob-&gt;player rider, so
     * {@code EnchantDefinition} refuses one that is not {@code class: shield}.
     *
     * <p>A SECOND mechanism rather than a parameter on {@link #DAMAGE}, by this enum's own rule:
     * the question is how many enchants share one mechanism, and these two share none of their
     * arithmetic. Damage multiplies an amount; this composes a fraction and clamps it. What they
     * DO share is the shape of their content -- a {@code value_by_level} curve -- and that is why
     * a second block enchant will be a yml file rather than a recompile.
     */
    BLOCK_DR,

    /**
     * A fraction of an incoming blow sent back at whoever threw it. The mechanism is {@link Thorns},
     * composed with the block in {@code core/combat/ShieldExchange}. Like {@link #BLOCK_DR} it is read
     * off the blocking stack, so {@code EnchantDefinition} refuses one that is not
     * {@code class: shield}.
     *
     * <p>A THIRD mechanism rather than a flavour of {@link #BLOCK_DR}, on the same rule: they share no
     * arithmetic at all. Block composes a fraction and clamps it; this multiplies a blow and hands the
     * product to a SECOND combatant. What they share is the shape of their content -- a
     * {@code value_by_level} curve -- which is what lets both be tuned without a recompile.
     *
     * <p><b>The no-negative rule on its curve matters more here than anywhere else.</b> A negative
     * reflect goes straight through {@code applyDamage} to {@code stats.damage} and HEALS the
     * attacking mob. {@code EnchantDefinition}'s shared {@code requireCurve} covers it by
     * construction, which is precisely what that lift was for.
     */
    REFLECT;

    /**
     * Case-insensitive lookup for the content loader. Returns null on a miss so the CALLER decides
     * what a bad name means -- the enchant loader throws, turning a bad (or absent) effect into a
     * named, skipped file, exactly as the weapon loader does for a bad class or rarity.
     */
    public static EnchantEffect fromName(String name) {
        if (name == null) return null;
        for (EnchantEffect e : values()) {
            if (e.name().equalsIgnoreCase(name)) return e;
        }
        return null;
    }
}