package io.github.butterflysmp.rpg.core.enchant;

/**
 * ONE BEHAVIOURAL ENCHANT, ACTIVE ON THE WEAPON THAT MADE A CAST (the seat's B2, 2026-11-23): the enchant's id (its file
 * name, used ONLY as the secondary damage's PLAYERHIT source and as a key for its own state, never compared to a literal
 * in Java), the level it is active at (1 or more), and the behaviour its file declares.
 */
public record ActiveBehaviour(String enchantId, int level, EnchantBehaviour behaviour) {
    public ActiveBehaviour {
        if (enchantId == null || enchantId.isBlank()) throw new IllegalArgumentException("an active behaviour needs an id");
        if (level < 1) {
            throw new IllegalArgumentException("enchant '" + enchantId + "' is active at level " + level
                    + "; a behaviour acts only at level 1 or more (B2)");
        }
        if (behaviour == null) throw new IllegalArgumentException("enchant '" + enchantId + "' has no behaviour");
    }
}
