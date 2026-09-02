package io.github.butterflysmp.rpg.core.weapon;

import java.util.List;
import java.util.Optional;

/**
 * A tool, as authored in {@code content/tools/&lt;anything&gt;.yml}.
 *
 * THE FOURTH ARM of the sealed {@link GearDefinition}, and the one the interface's javadoc was
 * written for: *"A future gear kind -- a trinket, a mount -- has to be admitted here explicitly, and
 * every exhaustive switch over gear then stops compiling until it is handled."* Adding it breaks
 * {@code GearItems.mint}, {@code remint} and {@code gearClassOf} until each says what a tool does.
 *
 * <h2>It carries NO stat, deliberately</h2>
 *
 * A weapon has attack damage, a shield has {@code block_dr}, a piece of armor has {@code defense}.
 * A tool has nothing of ours. Mining speed, harvest level and durability are vanilla's, and
 * {@code ToolItems.mint} pins no attribute modifier and sets no item flag, so a minted iron pickaxe
 * digs exactly as a plain one does. Inventing a mining-speed number here would mean either
 * overriding vanilla's (a mechanic this slice does not want) or displaying a figure nothing reads --
 * which is the "authored value that vanilla actually owns" trap {@code ArmorConsistency} exists to
 * catch one kind over.
 *
 * <p>So the record is {@link ShieldDefinition}'s flat shape with the stat swapped for a
 * {@link ToolKind}: not a number, an identity.
 *
 * <p>What it deliberately does NOT carry, beyond the stat:
 *
 * <ul>
 *   <li><b>No tier.</b> The file it came from is a container, not a tier -- see {@code ToolLoader}.
 *       Rarity is per entry, so shipping one file does not silently decide the rarity curve for
 *       five more.
 *   <li><b>No class.</b> {@code GearItems.gearClassOf} answers {@code GearClass.TOOL} for every
 *       tool, one constant for the whole kind, exactly as armor gets one for all four slots.
 *   <li><b>No triggers.</b> Mining is not an ability; it is vanilla's item behaviour, unridden.
 * </ul>
 */
public record ToolDefinition(
        String id,
        String displayName,
        Rarity rarity,
        String material,
        ToolKind kind,
        List<String> flavor,
        Optional<String> craftResult
) implements GearDefinition {

    /**
     * The material a tool gets when its content file does not name one.
     *
     * <p>There is deliberately NO usable default and this constant is a message, not a fallback:
     * unlike a shield, "a tool" names no single vanilla item, and the material is also the id. The
     * refusal below is what content sees. Kept as a named constant so the message says the same
     * thing everywhere it is quoted.
     */
    public static final String MATERIAL_REQUIRED =
            "it names both the vanilla item and the tool's own id, so it cannot be defaulted";

    public ToolDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("tool id must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("tool '" + id + "' has a blank display_name");
        }
        if (rarity == null) {
            throw new IllegalArgumentException("tool '" + id + "' has no rarity");
        }
        if (material == null || material.isBlank()) {
            throw new IllegalArgumentException("tool '" + id + "' has a blank material; "
                    + MATERIAL_REQUIRED);
        }
        // REFUSED, never defaulted. A tool with no kind has no footer noun, and the only available
        // fallback would be the generic word "Tool" on every one of them -- which is invisible in
        // exactly the way this project keeps finding: the item mints, renders, and reads slightly
        // wrong forever. The loader turns this into a named, skipped entry.
        if (kind == null) {
            throw new IllegalArgumentException("tool '" + id + "' has no kind; it must name one of "
                    + java.util.Arrays.toString(ToolKind.values()).toLowerCase(java.util.Locale.ROOT));
        }
        // THE KIND AND THE MATERIAL MAY NOT DISAGREE.
        //
        // Same defect shape as slice 2's craft_result-versus-material, and it gets the same answer:
        // FORBIDDEN at boot rather than gated. `material: iron_pickaxe` with `kind: shovel` would
        // mint a pickaxe whose footer reads "Common Shovel" -- nothing throws, nothing logs, and the
        // only vantage point from which it is visible is a player reading the last line of a
        // tooltip.
        //
        // Refused HERE rather than in ContentValidator because it needs no Bukkit registry: it is a
        // string comparison, so it can be a refusal and a unit test instead of a boot warning. The
        // validator seam exists for the checks that genuinely cannot run headless.
        //
        // Compared on the NORMALISED token so `minecraft:IRON_PICKAXE` is agreement rather than a
        // false alarm -- the same normalisation ContentValidator applies to a craft_result claim.
        if (!kind.matchesMaterial(CraftResultToken.token(material))) {
            throw new IllegalArgumentException("tool '" + id + "' is kind "
                    + kind.name().toLowerCase(java.util.Locale.ROOT) + " but its material is '"
                    + material + "'. The footer would call it a "
                    + kind.name().toLowerCase(java.util.Locale.ROOT)
                    + " while the item in hand is something else. Make them agree.");
        }
        flavor = flavor == null ? List.of() : List.copyOf(flavor);
        craftResult = CraftResultToken.normalise(craftResult, "tool", id);
    }

    /**
     * The shape without a craft-result claim, matching the sibling records' convenience constructor
     * so a test reads the same whichever kind it is building.
     */
    public ToolDefinition(String id, String displayName, Rarity rarity, String material,
                          ToolKind kind, List<String> flavor) {
        this(id, displayName, rarity, material, kind, flavor, Optional.empty());
    }
}
