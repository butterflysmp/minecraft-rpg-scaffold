package io.github.butterflysmp.rpg.paper.content;

import com.google.common.collect.Multimap;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemType;

import java.util.logging.Logger;

/**
 * Checks, at boot, that every armor piece's AUTHORED defense equals the vanilla armor points its
 * material actually grants -- and says so, loudly, when it does not.
 *
 * <h2>Why this has to exist</h2>
 *
 * A minted piece has its Defense described in two independent places:
 *
 * <ul>
 *   <li>the {@code defense:} key in {@code content/armor/&lt;tier&gt;.yml}, which is what the
 *       TOOLTIP prints; and
 *   <li>{@code ItemType.getDefaultAttributeModifiers(slot)}, vanilla's own armor points, which is
 *       what {@code DefenseModifierItems} feeds into the STAT, the mitigation and the armor bar.
 * </ul>
 *
 * Minting never writes the first onto the item, so nothing makes them agree. They agree today only
 * because someone typed matching numbers.
 *
 * <p><b>A mismatch is invisible from every single vantage point.</b> The tooltip reads
 * "Defense: 9" and looks right. The action bar reads 8 and looks right. The armor bar fills to the
 * DR of 8 and looks right. The damage taken matches 8 and looks right. Nothing throws, nothing
 * logs, no test can see it -- core cannot reach an {@code ItemType} registry and paper has no live
 * server in the unit loop. The ONLY moment the two numbers are in the same JVM is boot, which is
 * why the check lives here rather than in a test.
 *
 * <p>This is also the seam Slice 2 breaks on purpose. Protection (+3/6/9 Defense) makes a piece's
 * real Defense diverge from its material's vanilla points by design, at which point
 * {@code DefenseModifierItems.desiredModifiers} can no longer serve as both the stat source and the
 * {@code nativeArmor} the bar cancels -- they are one map today. When that lands, this check needs
 * to compare against the piece's BASE rather than its effective value, or it will start shouting
 * about every enchanted piece.
 *
 * <h2>Zero is a defect, not a quiet pass</h2>
 *
 * A verifier that verifies nothing reads exactly like one that verified everything. This warns when
 * it is handed an empty registry, the same discipline the loaders' zero-checks follow and the
 * failure mode CLAUDE.md records twice.
 *
 * <h2>The duplicated read</h2>
 *
 * {@link #vanillaArmorPoints} repeats the {@code ADD_NUMBER}-over-{@code Attribute.ARMOR} sum that
 * {@code DefenseModifierItems.armorOf} performs, rather than calling it. That is deliberate for this
 * slice: {@code DefenseModifierItems} is one of the files this slice promises to leave byte-identical,
 * so the additive claim can be verified by diff rather than by argument. The two reads must stay in
 * step, and unifying them belongs to the same follow-up PR that factors the gear abstraction.
 */
public final class ArmorConsistency {

    private ArmorConsistency() {}

    /**
     * Compare every loaded piece against vanilla and warn per mismatch. Returns the number of
     * mismatches found, so the caller can decide whether to say anything about a clean run.
     */
    public static int check(ArmorRegistry armor, Logger log) {
        if (armor.size() == 0) {
            log.warning("ArmorConsistency checked ZERO armor pieces. That is not a pass -- it means "
                    + "content/armor loaded nothing, and every armor tooltip a player sees will be "
                    + "unverified against vanilla's own numbers.");
            return 0;
        }

        int mismatches = 0;
        for (ArmorDefinition piece : armor.all()) {
            Material material = Material.matchMaterial(piece.material());
            if (material == null) {
                // Not a consistency failure -- ArmorItems.materialOf will fall back and mint
                // something wearable -- but the authored defense then describes an item that does
                // not exist, so it cannot be checked and must not be reported as checked.
                log.warning("Armor '" + piece.id() + "' names material '" + piece.material()
                        + "', which does not resolve. Its defense cannot be checked against vanilla, "
                        + "and it will mint as a leather fallback.");
                mismatches++;
                continue;
            }

            double vanilla = vanillaArmorPoints(material, piece.slot());
            if (vanilla != piece.defense()) {
                mismatches++;
                log.warning("Armor '" + piece.id() + "' authors defense " + piece.defense()
                        + " but vanilla grants " + vanilla + " armor points in the "
                        + piece.slot() + " slot. THE TOOLTIP WILL LIE: the stat, the mitigation and "
                        + "the armor bar all use vanilla's " + vanilla + ", and only the lore line "
                        + "uses " + piece.defense() + ". Fix the content file to match vanilla.");
            }
        }

        if (mismatches > 0) {
            log.warning(mismatches + " armor piece(s) disagree with vanilla's armor points. The "
                    + "server is still running; those pieces display a number they do not deliver.");
        }
        return mismatches;
    }

    /**
     * The armor points {@code material} inherently grants in {@code slot}.
     *
     * <p>Mirrors {@code DefenseModifierItems.armorOf} exactly, including its {@code ADD_NUMBER}
     * filter: every vanilla armor modifier is flat, and a scaling one summed as though it were flat
     * would be silently wrong in the same direction in both places, which would make this check
     * agree with a wrong stat.
     *
     * <p>{@code EquipmentSlot.valueOf(slot.name())} is what makes {@code ArmorSlot}'s constant names
     * a wire format rather than a naming preference -- see {@code ArmorSlotTest}.
     */
    private static double vanillaArmorPoints(Material material, io.github.butterflysmp.rpg.core.weapon.ArmorSlot slot) {
        ItemType type = material.asItemType();
        if (type == null) return 0.0;
        EquipmentSlot equipmentSlot = EquipmentSlot.valueOf(slot.name());
        Multimap<Attribute, AttributeModifier> defaults = type.getDefaultAttributeModifiers(equipmentSlot);
        double armor = 0.0;
        for (AttributeModifier modifier : defaults.get(Attribute.ARMOR)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADD_NUMBER) {
                armor += modifier.getAmount();
            }
        }
        return armor;
    }
}
