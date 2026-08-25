package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantState;

/**
 * One custom enchant: identity and bounds. An id, a display name, and how high it goes.
 *
 * <b>There is deliberately no behaviour field.</b> An enchant's EFFECT is a mechanism, not a
 * number -- Unbreaking's durability-skip curve is {@code core/enchant/Unbreaking.java}, bound to
 * this definition by id and nothing else. Content NAMES an effect and bounds it; it never defines
 * one, the same relationship ability yml has with {@code EffectSpec}'s subtypes. Adding a
 * {@code durability_skip: 0.25} key here is the obvious next temptation and it is the deferred
 * damage-modifier pass's schema decision, not a one-liner.
 *
 * <p>Lives in {@code paper/content} beside {@link ElementDefinition} and {@code StatusDefinition},
 * not in core: core reasons about enchant STATE and about one curve, and never needs to know which
 * enchants exist. The durability seam proves it -- it compares an id and never consults a registry.
 */
public record EnchantDefinition(String id, String displayName, int maxLevel) {

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
    }
}
