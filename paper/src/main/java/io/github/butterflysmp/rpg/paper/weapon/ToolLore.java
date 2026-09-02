package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.ToolDefinition;
import io.github.butterflysmp.rpg.core.weapon.ToolLoreLines;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The tool tooltip: optional flavour, and the "&lt;Rarity&gt; &lt;Kind&gt;" footer.
 *
 * <h2>The thinnest of the four, because a tool has NO STAT BLOCK</h2>
 *
 * Every sibling opens with a stat line -- a weapon's damage, a shield's Damage Reduction, a piece of
 * armor's Defense -- rendered from a number its content file authored. A tool authors none. Mining
 * speed, harvest level and durability all belong to vanilla, and {@link ToolItems#mint} pins nothing
 * and hides nothing, so vanilla's own tooltip lines are still there underneath ours.
 *
 * <p><b>That absence is the design, not a gap to fill later.</b> A "Mining Speed: 6" line here would
 * be a number this project displays and does not own -- which is exactly the split
 * {@code ArmorConsistency} exists to police for Defense, and it can only police it because armor's
 * authored value and vanilla's live one are checked against each other at boot. A tool with no
 * authored number has nothing to drift.
 *
 * <p>Takes the definition and nothing else -- the same structural guarantee {@link WeaponLore} and
 * {@link ShieldLore} make. Lore that cannot see item state cannot drift from it. The enchant block
 * is NOT built here; {@link ToolItems#applyLore} prepends it through {@code EnchantLore.applied},
 * which is what keeps the rarity footer literally last.
 */
public final class ToolLore {

    private ToolLore() {}

    /**
     * The tool's lore, top to bottom: any flavour, then the rarity footer.
     *
     * <p>No leading blank line, for {@link ShieldLore}'s reason: there is nothing above the flavour
     * to be separated from, and opening a tooltip with an empty line reads as a rendering bug.
     * {@code GearLore.appendFlavor} contributes its own separator when there is flavour to separate.
     *
     * <p>The footer noun is the KIND, so an Iron Pickaxe reads "Common Pickaxe" -- not "Common
     * Tool", which is what a fallback would produce and is therefore the specific string a gate row
     * looks for. See {@code ToolLoreLines.kindNoun}, an exhaustive switch with no default arm.
     */
    public static List<Component> build(ToolDefinition tool) {
        List<Component> lore = new ArrayList<>();

        GearLore.appendFlavor(lore, tool);

        GearLore.appendRarityFooter(lore, tool.rarity(), ToolLoreLines.kindNoun(tool.kind()));

        return lore;
    }
}
