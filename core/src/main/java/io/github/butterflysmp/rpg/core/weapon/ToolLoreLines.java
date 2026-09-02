package io.github.butterflysmp.rpg.core.weapon;

/**
 * The strings a tool's tooltip is built from.
 *
 * <p>Its own class beside {@code ArmorLoreLines}, {@code ShieldLoreLines} and
 * {@code WeaponLoreLines}, and by far the thinnest of the four: a tool has no stat, so it has no
 * stat LINE. What is left is the footer noun, which is the one piece of a tool's tooltip that is
 * neither vanilla's nor authored.
 *
 * <p>In {@code core} with no Bukkit imports, so the noun is a two-second unit test rather than a
 * boot gate row -- the same trade every other {@code *LoreLines} class makes.
 */
public final class ToolLoreLines {

    private ToolLoreLines() {}

    /**
     * The noun a tool is called in its rarity footer: {@code PICKAXE -> "Pickaxe"}.
     *
     * <p>An exhaustive switch with NO DEFAULT ARM, the same compiler-guided shape
     * {@code ArmorLoreLines.slotNoun} uses. A new {@link ToolKind} is then a compile error here
     * until someone names it, rather than silently footering as some fallback -- and "Common Tool"
     * on every one of them is exactly the fallback that would be reached for.
     *
     * <p>These are the generic NOUNS, not the item names. An Iron Pickaxe's footer reads "Common
     * Pickaxe" because the footer says what KIND of gear the item is, exactly as a weapon's reads
     * "Rare Melee Weapon" and a leather cap's reads "Common Helmet", rather than repeating the name
     * three lines above it.
     *
     * <p>Kept separate from {@link ToolKind#materialToken()} rather than derived from it by
     * case-folding. The two agree for every constant that exists today and diverge at the first
     * irregular -- {@code flint_and_steel} against "Flint and Steel" -- and a derivation that
     * happens to work for five values is the "template that assumed today's values" shape this
     * project has already shipped once, as "a Armor enchant".
     */
    public static String kindNoun(ToolKind kind) {
        return switch (kind) {
            case PICKAXE -> "Pickaxe";
            case AXE     -> "Axe";
            case SHOVEL  -> "Shovel";
            case HOE     -> "Hoe";
            case SHEARS  -> "Shears";
        };
    }
}
