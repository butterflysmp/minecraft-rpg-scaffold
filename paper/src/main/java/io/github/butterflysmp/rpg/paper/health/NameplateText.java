package io.github.butterflysmp.rpg.paper.health;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Builds a mob's nameplate text: {@code <name> <cur>/<max> ❤} with the ❤ (U+2764) in red, showing the
 * CUSTOM cur/max. Pure Adventure -- no Bukkit -- so the format is unit-testable, and the numbers are
 * whatever the custom store holds (cap-free: a boss reading {@code 5000/5000 ❤} is representable, even
 * though nothing seeds a mob above vanilla's 1024 this phase).
 *
 * This is the "what the name says" half of the nameplate; {@code MobNameplateManager} rebuilds it on
 * every {@link io.github.butterflysmp.rpg.core.combat.stat.HealthChange}. The "who can see it" half is
 * the per-viewer LOS loop.
 */
public final class NameplateText {

    private NameplateText() {}

    /** The red heart glyph, U+2764. */
    private static final Component HEART = Component.text("❤", NamedTextColor.RED);

    public static Component of(Component baseName, double current, double max) {
        String numbers = " " + Math.round(current) + "/" + Math.round(max) + " ";
        return Component.textOfChildren(baseName, Component.text(numbers), HEART);
    }
}
