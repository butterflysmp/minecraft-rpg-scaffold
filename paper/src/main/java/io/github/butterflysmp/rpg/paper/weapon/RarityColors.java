package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * The one place a rarity tier becomes a colour. Adventure's NamedTextColor cannot live
 * in core, so the mapping lives here -- the same rule as Keys: never inline a rarity
 * colour at a call site, ask this.
 *
 * Deliberately one exhaustive switch with no default arm. Adding a tier to the Rarity
 * enum is then a compile error here until it is coloured, and Phase 4's per-tier slots
 * and strength will be compiler-guided the identical way. A default arm would swallow a
 * new tier as some fallback colour and lose that guarantee.
 */
public final class RarityColors {

    private RarityColors() {}

    public static NamedTextColor of(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> NamedTextColor.WHITE;
            case UNCOMMON -> NamedTextColor.GREEN;
            case RARE -> NamedTextColor.BLUE;
            case EPIC -> NamedTextColor.DARK_PURPLE;
            case LEGENDARY -> NamedTextColor.GOLD;
            case EXOTIC -> NamedTextColor.AQUA;
        };
    }
}
