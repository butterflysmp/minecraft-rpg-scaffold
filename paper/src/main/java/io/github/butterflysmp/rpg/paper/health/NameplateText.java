package io.github.butterflysmp.rpg.paper.health;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.OptionalInt;

/**
 * Builds a mob's nameplate text: {@code [GS] <name> <cur>/<max> ❤}. The {@code [GS]} is AQUA (M22) and
 * appears only on a HOSTILE mob (M6, M7); the ❤ (U+2764) is red; the name and the numbers carry no
 * colour of their own. It shows the CUSTOM cur/max. Pure Adventure -- no Bukkit -- so the format is
 * unit-testable, and the numbers are whatever the custom store holds: cap-free, so a GS 500 Warden
 * reading {@code 12500/12500 ❤} is the same mechanism as a zombie's 100.
 *
 * <p><b>The gear score belongs to the FIXED half.</b> A mob's score never changes (M5), so
 * {@link #nameWithScore} is applied once, to the base name the nameplate is registered with, and every
 * later {@link #of} rebuild carries it for free. There is no second field and no second version path.
 *
 * <p>This is the "what the name says" half of the nameplate; {@code MobNameplateManager} rebuilds it
 * on every {@link io.github.butterflysmp.rpg.core.combat.stat.HealthChange}. The "who can see it" half
 * is the per-viewer LOS loop.
 */
public final class NameplateText {

    private NameplateText() {}

    /** The red heart glyph, U+2764. */
    private static final Component HEART = Component.text("❤", NamedTextColor.RED);

    /** M22: the {@code [GS]} is light blue. Nothing else on the plate changes colour. */
    static final NamedTextColor SCORE_COLOR = NamedTextColor.AQUA;

    public static Component of(Component baseName, double current, double max) {
        String numbers = " " + Math.round(current) + "/" + Math.round(max) + " ";
        return Component.textOfChildren(baseName, Component.text(numbers), HEART);
    }

    /**
     * The base name a mob's nameplate is registered with: {@code [GS] Name} for a hostile mob, the
     * bare name for a passive one ({@code gearScore} empty).
     *
     * <p>Built as SIBLINGS under an unstyled parent, never as the name appended to the coloured
     * prefix: a child inherits its parent's style, so {@code prefix.append(name)} would paint the mob's
     * name aqua as well.
     */
    public static Component nameWithScore(OptionalInt gearScore, Component name) {
        if (gearScore.isEmpty()) return name;
        return Component.textOfChildren(
                Component.text("[" + gearScore.getAsInt() + "] ", SCORE_COLOR), name);
    }
}
