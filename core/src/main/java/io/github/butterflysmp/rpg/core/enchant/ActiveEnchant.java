package io.github.butterflysmp.rpg.core.enchant;

/**
 * An enchant taking effect on an item right now, at its resolved level.
 *
 * The output of {@link EnchantState#effective()}, and the ONE thing both readers consume: the
 * tooltip renders these, and the durability seam reads its level from these. That shared origin is
 * deliberate -- a tooltip that promises a level the effect does not apply is a lie the item tells,
 * and the only reliable way to prevent it is to leave no second place to compute it.
 */
public record ActiveEnchant(String enchantId, int level) {

    public ActiveEnchant {
        if (enchantId == null || enchantId.isBlank()) {
            throw new IllegalArgumentException("enchant id required, was: " + enchantId);
        }
        // 1, not 0: a level-0 enchant is not "active at nothing", it is NOT ACTIVE, and it must
        // never reach a renderer or an effect as though it were.
        if (level < 1 || level > EnchantState.MAX_LEVEL) {
            throw new IllegalArgumentException("active enchant '" + enchantId + "' level must be 1.."
                    + EnchantState.MAX_LEVEL + ", was " + level);
        }
    }
}
