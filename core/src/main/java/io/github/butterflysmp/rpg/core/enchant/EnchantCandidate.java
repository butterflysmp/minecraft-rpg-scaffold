package io.github.butterflysmp.rpg.core.enchant;

/**
 * One candidate enchant on a slot: an id, and the level the player has UNLOCKED it to.
 *
 * A candidate is an OFFER, not an effect. Level 0 is locked -- present on the item, visible to a
 * future table UI, doing nothing. Levels 1..{@link EnchantState#MAX_LEVEL} are unlocked, and an
 * unlocked candidate still only takes effect while its slot has it ACTIVE. That three-way split
 * (offered / unlocked / active) is the whole point of the model: unlocking A to II and B to I must
 * both persist, and swapping which one is active must not cost either of them their level.
 *
 * <p>The old repo could not express this -- its slot carried one {@code chosenId} and one
 * {@code chosenLevel}, so a swap forgot what the other candidate had cost. Putting the level on the
 * CANDIDATE rather than on the choice is the difference, and it is visible in the codec's grammar.
 *
 * <p><b>The id may not contain a codec delimiter.</b> That is not tidiness: it is what makes
 * {@link EnchantCodec#encode} injective. An id carrying a {@code ,} would decode back as two
 * candidates, and a player's unlocks would silently change shape on the next re-mint.
 */
public record EnchantCandidate(String enchantId, int level) {

    /**
     * The four characters {@link EnchantCodec} reserves: slot, candidate, level and active-index
     * separators. Declared here rather than in the codec because this is where they are ENFORCED,
     * and a rule enforced far from where it is declared is one that gets deleted by someone who
     * cannot see what it was for.
     */
    public static final String DELIMITERS = ";,=:";

    public EnchantCandidate {
        if (enchantId == null || enchantId.isBlank()) {
            throw new IllegalArgumentException("enchant id required, was: " + enchantId);
        }
        for (int i = 0; i < enchantId.length(); i++) {
            if (DELIMITERS.indexOf(enchantId.charAt(i)) >= 0) {
                throw new IllegalArgumentException("enchant id '" + enchantId
                        + "' may not contain any of " + DELIMITERS
                        + " -- they are the enchant codec's delimiters");
            }
        }
        if (level < 0 || level > EnchantState.MAX_LEVEL) {
            throw new IllegalArgumentException("enchant '" + enchantId + "' level must be 0.."
                    + EnchantState.MAX_LEVEL + ", was " + level);
        }
    }

    /** Offered but not yet unlocked: on the item, and doing nothing. */
    public boolean isLocked() {
        return level == 0;
    }
}
