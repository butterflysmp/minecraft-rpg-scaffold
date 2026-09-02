package io.github.butterflysmp.rpg.core.weapon;

import java.util.Locale;

/**
 * What kind of tool a {@link ToolDefinition} is: the word its rarity footer uses, and the vanilla
 * item family its material must belong to.
 *
 * <h2>An open LIST, not a closed grid -- and that is the whole reason this is an enum</h2>
 *
 * {@link ArmorSlot} is a closed axis Minecraft decided: four slots, and a fifth would be a Minecraft
 * event. This is not that. Of the 84 durable materials on a booted server, the tier-by-kind grid
 * describes 24; outside it sit shears, a brush, a fishing rod, flint and steel, a mace, a carrot on
 * a stick and seven spears. So this enum is expected to GROW, repeatedly, and it is shaped for that:
 * every constant must answer {@link #materialToken} here and {@code ToolLoreLines.kindNoun} there,
 * both exhaustive switches with no default arm, before the build passes.
 *
 * <p><b>That obligation is the point.</b> A new tool kind cannot be added and left to render as some
 * fallback noun -- which is the "a Armor enchant" defect one axis over, and the reason
 * {@code NEXT.md}'s first rule says to derive per-value text from the value rather than from a
 * template that assumed today's values.
 *
 * <p><b>The constant names are not display text.</b> The footer noun lives in
 * {@code ToolLoreLines.kindNoun}, beside {@code ArmorLoreLines.slotNoun} and for the same reason
 * {@link ArmorSlot} keeps its own names out of the tooltip: display and identity answer different
 * questions and would fight if merged. The first irregular proves they diverge -- a future
 * {@code FLINT_AND_STEEL} is the token {@code flint_and_steel} and the noun "Flint and Steel".
 */
public enum ToolKind {
    PICKAXE,
    AXE,
    SHOVEL,
    HOE,
    SHEARS;

    /**
     * The vanilla item token this kind names: {@code PICKAXE -> "pickaxe"}.
     *
     * <p>Exhaustive, with NO default arm, deliberately -- the same discipline
     * {@code GearClass.of} and {@code RarityColors} use. A new constant is a compile error here
     * until someone says which vanilla item family it belongs to.
     *
     * <p>Lower case because it is compared against a normalised material token, never displayed.
     */
    public String materialToken() {
        return switch (this) {
            case PICKAXE -> "pickaxe";
            case AXE     -> "axe";
            case SHOVEL  -> "shovel";
            case HOE     -> "hoe";
            case SHEARS  -> "shears";
        };
    }

    /**
     * Does this material token name an item of THIS kind?
     *
     * <p><b>THE UNDERSCORE IS THE ENTIRE CHECK, and a bare {@code endsWith} is the bug.</b>
     * {@code "iron_pickaxe".endsWith("axe")} is {@code true}, so a suffix test without the separator
     * accepts {@code material: iron_pickaxe} paired with {@code kind: AXE} -- which is precisely the
     * disagreement {@link ToolDefinition} refuses this for. With the separator,
     * {@code "iron_pickaxe".endsWith("_axe")} is false, because the four characters before the end
     * are {@code kaxe}.
     *
     * <p>The bare equality arm is what lets an UNTIERED tool be an ordinary entry rather than a
     * special case: {@code shears} is the whole token, with no tier prefix at all, and a future
     * {@code flint_and_steel} will be too. A loader shaped around {@code tier + "_" + kind} would
     * have to special-case both.
     *
     * @param materialToken already normalised through {@code CraftResultToken.token}. Null is no
     *                      match rather than a throw, so the caller's own blank-material refusal
     *                      reports first and names the real problem.
     */
    public boolean matchesMaterial(String materialToken) {
        if (materialToken == null) return false;
        String token = materialToken();
        return materialToken.equals(token) || materialToken.endsWith("_" + token);
    }

    /**
     * Case-insensitive lookup for the content loader. Returns null on a miss so the CALLER decides
     * what a bad name means -- the tool loader throws, turning a bad kind into a named, skipped
     * entry, the same contract {@link Rarity#fromName} and {@link ArmorSlot#fromName} have.
     */
    public static ToolKind fromName(String name) {
        if (name == null) return null;
        String trimmed = name.trim().toUpperCase(Locale.ROOT);
        for (ToolKind kind : values()) {
            if (kind.name().equals(trimmed)) return kind;
        }
        return null;
    }
}
