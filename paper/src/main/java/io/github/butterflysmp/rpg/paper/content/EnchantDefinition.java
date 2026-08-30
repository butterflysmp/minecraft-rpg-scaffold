package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.GearClass;

import java.util.List;

/**
 * One custom enchant: identity, bounds, and WHICH mechanism it binds to.
 *
 * <b>The behaviour field Pass 1 deferred to Pass 2 is {@link #effect}</b>, and the rule it was
 * deferred to protect is unchanged: content NAMES an effect and bounds it; it never DEFINES one.
 * {@code effect: durability} names {@code core/enchant/Unbreaking.java}; {@code effect: damage}
 * names {@code core/enchant/DamageEnchants.java}. Neither file contains a mechanism, and the
 * {@code durability_skip: 0.25} key the old javadoc warned about is still refused -- an enchant
 * cannot describe HOW it works, only which of the known ways it does.
 *
 * <p><b>Why {@code value_by_level} is data when Unbreaking's curve is Java.</b> The question is
 * not numbers-versus-code, it is how many enchants share one mechanism. Unbreaking is one enchant
 * with one curve, so its curve IS its mechanism. Sharpness, Power and Attunement are three enchants
 * sharing ONE mechanism, differing only in a class gate and three percentages -- a Java class each
 * would be three copies of the same arithmetic, and the fourth would be a recompile, which
 * invariant 2 forbids. So the mechanism is named and its parameters are authored, exactly as
 * {@code max_level} was already an authored bound rather than a mechanism. See {@link EnchantEffect}.
 *
 * <p>The list is explicit ({@code [5, 10, 15]}) rather than a linear scalar so that a future
 * non-linear curve is not blocked by this pass's schema.
 *
 * <p><b>Every rule here THROWS rather than defaulting or clamping.</b> These are content-authored
 * mechanical axes: the loader names the file and skips it, which is a mistake someone can fix. Only
 * {@code EnchantCodec} repairs, because only it reads something a different build may have written.
 * The precedent is {@code WeaponLoader}'s treatment of {@code class} -- required, never defaulted,
 * because a wrong default on a gate is a silent-correctness bug rather than a visible one.
 *
 * <p>Lives in {@code paper/content} beside {@link ElementDefinition} and {@code StatusDefinition},
 * not in core: core reasons about enchant STATE and about the mechanisms, and never needs to know
 * which enchants exist. The durability seam proves it -- it compares an id and never consults a
 * registry.
 *
 * @param gearClass      the GEAR this enchant may sit on, or {@code null} for {@code universal} (no
 *                       gate). Null rather than a fifth enum constant, because "valid everywhere" is
 *                       the absence of a gate rather than a kind of gear.
 *                       <p>
 *                       <b>This was {@code WeaponClass} until Slice 2, and the javadoc here argued
 *                       for keeping it that way:</b> "a parallel enum would need SUMMONER adding in
 *                       two places the day that class lands, and the exhaustive-switch discipline
 *                       that makes such an addition a compile error only works with one enum." The
 *                       first half is true and was accepted. The second half was wrong, and the
 *                       compiler settled it: {@code GearClass.of(WeaponClass)} is itself an
 *                       exhaustive switch with no default arm, so deleting an arm gives <i>"the
 *                       switch expression does not cover all possible input values"</i>. SUMMONER
 *                       lands in two places, one of which the compiler names.
 *                       <p>
 *                       What forced the split is that a shield has no fighting class and never will.
 *                       Adding SHIELD to {@code WeaponClass} would drive it through every exhaustive
 *                       weapon switch -- starting with {@code WeaponClassLabel.of}, which has no
 *                       default arm and no words for it -- to make one enchant gate expressible.
 * @param icon           the Material name this enchant renders as in the enchant table, e.g.
 *                       {@code iron_sword}. Presentational identity, so it is CONTENT rather than an
 *                       id->Material map in Java: the 500th enchant must not need a recompile to have
 *                       a picture. Kept as a String, not a Material, so this record stays free of
 *                       Bukkit and therefore unit-testable; it is resolved at render time exactly as
 *                       {@code WeaponItems.materialOf} resolves a weapon material.
 * @param valueByLevel the VALUE this enchant grants at levels 1..n, or an EMPTY list for a durability
 *                       enchant. Its size is held equal to {@code maxLevel}, which is what makes
 *                       level -> value total. What the value MEANS is the mechanism's business: a
     *                       percent for damage, block and reflect; flat POINTS for defense and max
     *                       health. The curve itself never divides, which is why it is not "percent".
 */
public record EnchantDefinition(String id, String displayName, int maxLevel,
                                EnchantEffect effect, GearClass gearClass,
                                List<Integer> valueByLevel, String icon) {

    /**
     * The item an enchant renders as in the enchant table when its file names none.
     *
     * <p>COSMETIC, so it defaults -- unlike {@code effect} and {@code class}, which are mechanical
     * gates and are refused when absent. A wrong default on a gate is a silent-correctness bug; a
     * wrong default on an icon is a picture someone can see is wrong. The same split WeaponLoader
     * makes between a weapon material and its class.
     */
    public static final String DEFAULT_ICON = "enchanted_book";

    public EnchantDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("enchant id required");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("enchant '" + id + "' requires display_name");
        }
        // Content-authored, so it THROWS rather than clamping -- the loader names the file and
        // skips it, which is a mistake someone can fix. Only EnchantCodec repairs, because only
        // EnchantCodec is reading something a different build may have written.
        if (maxLevel < 1 || maxLevel > EnchantState.MAX_LEVEL) {
            throw new IllegalArgumentException("enchant '" + id + "' max_level must be 1.."
                    + EnchantState.MAX_LEVEL + ", was " + maxLevel);
        }
        if (effect == null) {
            throw new IllegalArgumentException("enchant '" + id + "' requires an effect");
        }
        valueByLevel = valueByLevel == null ? List.of() : List.copyOf(valueByLevel);
        // Blank as well as null: an empty material string resolves to no Material at all, and the
        // table would paint air where a candidate should be. Falling back beats rendering nothing.
        icon = (icon == null || icon.isBlank()) ? DEFAULT_ICON : icon;

        // SWITCH EXPRESSIONS, NOT A SWITCH STATEMENT, and that distinction is the whole guarantee.
        //
        // This was a switch STATEMENT until Slice 2b, carrying a comment claiming "no default, so a
        // new EnchantEffect constant is a compile error here". THAT CLAIM WAS FALSE. Java requires
        // exhaustiveness of switch EXPRESSIONS; a switch statement over an enum may silently cover
        // nothing. Adding REFLECT compiled clean and fell through to NO VALIDATION AT ALL -- no curve
        // rule, no gate rule -- which for a reflect enchant means a negative percent reaching
        // stats.damage and HEALING the attacking mob. The compiler proved it: only EnchantEffectLine,
        // whose switch really is an expression, failed to compile.
        //
        // Choosing the rules as VALUES restores the guarantee for real. A new constant now fails here
        // twice, by name, until someone states what a file carrying it may claim.
        Gate gate = switch (effect) {
            case DAMAGE     -> Gate.ANY_BUT_SHIELD;
            // Both read off the BLOCKING STACK, which only a shield can be.
            case BLOCK_DR,
                 REFLECT    -> Gate.SHIELD_ONLY;
            case DURABILITY -> Gate.UNIVERSAL_ONLY;
        };
        boolean curved = switch (effect) {
            case DAMAGE, BLOCK_DR, REFLECT -> true;
            case DURABILITY                -> false;
        };

        // The two rules are shared rather than copied per arm: three effects now hold the same curve
        // rules, and three copies of "size must equal max_level" is how they drift.
        if (curved) {
            requireCurve(id, effect, maxLevel, valueByLevel);
        } else {
            requireNoCurve(id, effect, valueByLevel);
        }
        requireGate(id, effect, gearClass, gate);
    }

    /** Which gates an effect's mechanism can actually be read through. See {@link #requireGate}. */
    private enum Gate { UNIVERSAL_ONLY, SHIELD_ONLY, ANY_BUT_SHIELD }

    /**
     * A file may not claim a control it does not have -- the rule the durability arm has enforced
     * since Pass 2, generalised now that there are three mechanisms with three different readers.
     *
     * <p>Each rule says the same thing: THIS effect's mechanism only ever runs against gear of a
     * certain kind, so gating it anywhere else is a promise nothing keeps. They are not taxonomy
     * claims about what gear could theoretically exist -- if an off-hand parry dagger ever blocks,
     * {@code SHIELD_ONLY} becomes a two-value check and nothing else moves.
     */
    private static void requireGate(String id, EnchantEffect effect, GearClass gearClass, Gate gate) {
        switch (gate) {
            case UNIVERSAL_ONLY -> {
                // Nothing gates wear by class: WeaponDurability and ShieldDurability both read the
                // level off the stack in hand without consulting a class at all.
                if (gearClass != null) {
                    throw new IllegalArgumentException("enchant '" + id + "' has effect: "
                            + token(effect) + " and must be class: universal -- durability is not"
                            + " class-gated, so naming a class would be a promise nothing keeps");
                }
            }
            case SHIELD_ONLY -> {
                // Read off the blocking stack in the mob->player rider. A universal one would enter
                // EVERY weapon's roll pool and sell a player an XP unlock that does nothing, which
                // is exactly the mistake `class:` is required and spelled out to prevent.
                if (gearClass != GearClass.SHIELD) {
                    throw new IllegalArgumentException("enchant '" + id + "' has effect: "
                            + token(effect) + " and must be class: shield -- it is read off the"
                            + " blocking stack, so on anything else it would never fire"
                            + (gearClass == null
                                    ? " (universal would put it in every weapon's pool)"
                                    : ", was class: " + gearClass.name().toLowerCase()));
                }
            }
            case ANY_BUT_SHIELD -> {
                // DamageEnchantItems reads the MAIN HAND's weapon and maps it through GearClass.of,
                // which can never yield SHIELD. So a shield-gated damage enchant is structurally
                // unreachable rather than merely useless.
                if (gearClass == GearClass.SHIELD) {
                    throw new IllegalArgumentException("enchant '" + id + "' has effect: "
                            + token(effect) + " and must not be class: shield -- the damage gate"
                            + " reads the weapon in your main hand, so it could never fire");
                }
            }
        }
    }

    /**
     * The curve rules, shared by every effect that HAS a curve.
     *
     * <p>A verbatim lift of what the DAMAGE arm did alone, so
     * {@code aCurveWhoseLengthDisagreesWithMaxLevelIsRefusedBothWays} and
     * {@code aNegativePercentIsRefusedRatherThanShippedAsACurse} stay green unedited -- which is the
     * proof the lift was faithful rather than a rewrite wearing its name.
     *
     * <p><b>The negative rule matters MORE for the newer effects than for damage.</b> A negative
     * block percent silently weakens the shield the file claims to strengthen; a negative reflect
     * goes straight through {@code applyDamage} to {@code stats.damage} and HEALS the
     * attacking mob. Sharing one validator is what stops that being three separate decisions.
     */
    private static void requireCurve(String id, EnchantEffect effect, int maxLevel,
                                     List<Integer> valueByLevel) {
        if (valueByLevel.isEmpty()) {
            throw new IllegalArgumentException("enchant '" + id + "' has effect: " + token(effect)
                    + " and so requires value_by_level (one value per level, e.g. [5, 10, 15])");
        }
        // Size EQUALS max_level, not "at least". This is what makes level -> value total: a short
        // list leaves a legal level with no value, and a long one hides levels the enchant can
        // never reach, which reads on the tooltip as a promise it cannot keep.
        if (valueByLevel.size() != maxLevel) {
            throw new IllegalArgumentException("enchant '" + id + "' declares max_level "
                    + maxLevel + " but value_by_level has " + valueByLevel.size()
                    + " entr" + (valueByLevel.size() == 1 ? "y" : "ies")
                    + "; they must match, one value per level");
        }
        for (int i = 0; i < valueByLevel.size(); i++) {
            Integer value = valueByLevel.get(i);
            if (value == null) {
                throw new IllegalArgumentException("enchant '" + id + "' has a non-numeric"
                        + " value_by_level entry at level " + (i + 1));
            }
            // Negative is refused rather than supported. Stat permits negative modifiers and a
            // "curse" enchant is a legitimate future idea, but a negative PERCENT is far more likely
            // a typo, and one below -100 would flip a hit into a heal-shaped negative. A curse wants
            // its own naming and its own decision, not a sign slip.
            if (value < 0) {
                throw new IllegalArgumentException("enchant '" + id + "' has a negative"
                        + " value (" + value + ") at level " + (i + 1)
                        + "; these enchants scale their effect UP, and a curse is its own"
                        + " content decision rather than a negative here");
            }
        }
    }

    /** The other half of the same rule: an effect with no curve may not author one. */
    private static void requireNoCurve(String id, EnchantEffect effect, List<Integer> valueByLevel) {
        if (!valueByLevel.isEmpty()) {
            throw new IllegalArgumentException("enchant '" + id + "' has effect: " + token(effect)
                    + " and must not declare value_by_level -- nothing reads it, so the"
                    + " file would be claiming a control it does not have");
        }
    }

    /** The effect as an author spells it in a file, so a refusal quotes their own token back. */
    private static String token(EnchantEffect effect) {
        return effect.name().toLowerCase();
    }

    /** Without an icon: the shape every caller predating the enchant table uses. */
    public EnchantDefinition(String id, String displayName, int maxLevel, EnchantEffect effect,
                             GearClass gearClass, List<Integer> valueByLevel) {
        this(id, displayName, maxLevel, effect, gearClass, valueByLevel, DEFAULT_ICON);
    }

    /** True when this enchant is gated on no class at all and applies to whatever it sits on. */
    public boolean isUniversal() {
        return gearClass == null;
    }
}
