package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;

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
 * <p><b>Why {@code percent_by_level} is data when Unbreaking's curve is Java.</b> The question is
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
 * @param weaponClass    the class this enchant is gated on, or {@code null} for {@code universal}
 *                       (no gate). Null rather than a fourth enum constant so the gate reuses
 *                       {@link WeaponClass} itself: a parallel enum would need SUMMONER adding in
 *                       two places the day that class lands, and the exhaustive-switch discipline
 *                       that makes such an addition a compile error only works with one enum.
 * @param percentByLevel the damage percent at levels 1..n, or an EMPTY list for a durability
 *                       enchant. Its size is held equal to {@code maxLevel}, which is what makes
 *                       level -> percent total.
 */
public record EnchantDefinition(String id, String displayName, int maxLevel,
                                EnchantEffect effect, WeaponClass weaponClass,
                                List<Integer> percentByLevel) {

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
        percentByLevel = percentByLevel == null ? List.of() : List.copyOf(percentByLevel);

        switch (effect) {
            case DAMAGE -> {
                if (percentByLevel.isEmpty()) {
                    throw new IllegalArgumentException("enchant '" + id + "' has effect: damage and"
                            + " so requires percent_by_level (one percent per level, e.g. [5, 10, 15])");
                }
                // Size EQUALS max_level, not "at least". This is what makes level -> percent total:
                // a short list leaves a legal level with no percent, and a long one hides levels the
                // enchant can never reach, which reads on the tooltip as a promise it cannot keep.
                if (percentByLevel.size() != maxLevel) {
                    throw new IllegalArgumentException("enchant '" + id + "' declares max_level "
                            + maxLevel + " but percent_by_level has " + percentByLevel.size()
                            + " entr" + (percentByLevel.size() == 1 ? "y" : "ies")
                            + "; they must match, one percent per level");
                }
                for (int i = 0; i < percentByLevel.size(); i++) {
                    Integer percent = percentByLevel.get(i);
                    if (percent == null) {
                        throw new IllegalArgumentException("enchant '" + id + "' has a non-numeric"
                                + " percent_by_level entry at level " + (i + 1));
                    }
                    // Negative is refused rather than supported. Stat permits negative modifiers and
                    // a "curse" enchant is a legitimate future idea, but a negative PERCENT is far
                    // more likely a typo, and one below -100 would flip a hit into a heal-shaped
                    // negative. A curse wants its own naming and its own decision, not a sign slip.
                    if (percent < 0) {
                        throw new IllegalArgumentException("enchant '" + id + "' has a negative"
                                + " percent (" + percent + ") at level " + (i + 1)
                                + "; a damage enchant scales damage up, and a curse is its own"
                                + " content decision rather than a negative here");
                    }
                }
            }
            case DURABILITY -> {
                // A file may not claim a control it does not have. Nothing gates a durability
                // enchant by class and nothing reads a percent off one, so declaring either would be
                // a lie the file tells about itself -- the same defect the weapon-lore pass fixed by
                // stripping authored colours from display_name once rarity owned the colour.
                if (!percentByLevel.isEmpty()) {
                    throw new IllegalArgumentException("enchant '" + id + "' has effect: durability"
                            + " and must not declare percent_by_level -- nothing reads it, so the"
                            + " file would be claiming a control it does not have");
                }
                if (weaponClass != null) {
                    throw new IllegalArgumentException("enchant '" + id + "' has effect: durability"
                            + " and must be class: universal -- durability is not class-gated, so"
                            + " naming a class would be a promise nothing keeps");
                }
            }
        }
    }

    /** True when this enchant is gated on no class at all and applies to whatever it sits on. */
    public boolean isUniversal() {
        return weaponClass == null;
    }
}
