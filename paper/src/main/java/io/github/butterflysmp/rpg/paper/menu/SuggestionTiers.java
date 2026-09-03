package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import io.github.butterflysmp.rpg.core.weapon.SuggestionTier;
import io.github.butterflysmp.rpg.core.weapon.ToolDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;

/**
 * Which {@link SuggestionTier} a craft belongs in.
 *
 * <p>The paper half of the ordering: {@code core} owns the AXIS and the sort, this owns the
 * decision. It lives here because classifying needs the sealed {@code GearDefinition} hierarchy and,
 * upstream of it, {@code CraftResultIndex} -- which is keyed on Bukkit material tokens.
 *
 * <p>Its own class rather than a method on {@code RecipeProbe} for the reason
 * {@code GearClassLabel} is its own class: it is a small total function over a sealed type, and
 * keeping it separate is what lets the exhaustive switch be read in one screen.
 */
public final class SuggestionTiers {

    private SuggestionTiers() {}

    /**
     * The tier a crafted item sorts in.
     *
     * <p><b>An exhaustive switch over the sealed {@link GearDefinition} with NO default arm</b>, the
     * same discipline {@code GearItems.gearClassOf} uses. A FIFTH gear kind is a compile error here
     * until someone decides where it ranks -- which is the point: a new kind silently sorting last,
     * below vanilla planks, is exactly the quiet wrong answer a default arm would give.
     *
     * @param claimed the gear definition this craft mints, or {@code null} for an ordinary vanilla
     *                craft that mints nothing. Null is the common case by a wide margin: most of the
     *                roster claims no {@code craft_result} at all.
     */
    public static SuggestionTier of(GearDefinition claimed) {
        if (claimed == null) return SuggestionTier.VANILLA;

        return switch (claimed) {
            case WeaponDefinition weapon -> SuggestionTier.WEAPON;
            // A shield is an ACCESSORY, not a category of its own. The tier is a display grouping
            // and is deliberately wider than the gear kind, so an accessory kind joins it later
            // rather than forcing a new position and renumbering everything below.
            case ShieldDefinition shield -> SuggestionTier.ACCESSORY;
            case ToolDefinition tool -> SuggestionTier.TOOL;
            case ArmorDefinition armor -> SuggestionTier.ARMOR;
        };
    }

    // NOTE: SuggestionTier.MATERIAL is UNREACHABLE from this method, and deliberately so. Every
    // craft either mints gear -- the four arms above -- or does not, and nothing anywhere says
    // "this vanilla item is an intermediate rather than a product". There is no source of truth to
    // consult, so there is no arm to write.
    //
    // That means MATERIAL has no test and no gate row, which is recorded on the constant itself.
    // Leaving the position in the ordering is cheaper than inserting it later and renumbering, but
    // it is uncovered, and saying so here is the difference between held-open and forgotten.
}
