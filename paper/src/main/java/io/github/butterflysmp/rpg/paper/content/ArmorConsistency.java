package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.weapon.ArmorCoverage;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.paper.health.DefenseModifierItems;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Checks, at boot, that the armor content COVERS the server's armor: every armor material the server has (minus the
 * excluded) has exactly one content piece, and every piece names a material the server has as armor for its slot.
 *
 * <h2>REWRITTEN 2026-11-25 (the seat's A2), because its old job ended</h2>
 *
 * Until then this compared each piece's authored {@code defense:} with vanilla's armor points for the material, since
 * the authored number was DISPLAY-ONLY and the stat read vanilla. A1 made the authored number the source, so those two
 * now differ for every piece on purpose (Ben's 5/8/7/4 ladder is no material's), and the comparison would shout about
 * all of them. What can still go wrong is coverage, decided purely in {@link ArmorCoverage}; this class reads the
 * server's side of it from the item registry, which is the only thing core cannot.
 *
 * <h2>Zero is a defect, not a quiet pass</h2>
 *
 * A verifier that verifies nothing reads exactly like one that verified everything. It still warns when handed an
 * empty registry -- the same discipline the loaders' zero-checks follow.
 */
public final class ArmorConsistency {

    private ArmorConsistency() {}

    /**
     * C2 (Ben, 2026-11-25: "No turtle helmet"): the one server armor item with no content piece, by name. Without this
     * the check would report it as uncovered on every boot.
     */
    static final Set<String> EXCLUDED = Set.of("turtle_helmet");

    /** The live check: the server's armor read from its item registry. Returns the number of problems. */
    public static int check(ArmorRegistry armor, Logger log) {
        if (armor.size() == 0) return warnEmpty(log);
        return check(armor, serverArmor(), log);
    }

    /** The decision with the server's side supplied, so a test can stage it. */
    static int check(ArmorRegistry armor, Map<String, ArmorSlot> serverArmor, Logger log) {
        if (armor.size() == 0) return warnEmpty(log);
        List<String> problems = ArmorCoverage.problems(serverArmor, armor.all(), EXCLUDED);
        for (String problem : problems) log.warning("ArmorConsistency: " + problem);
        if (!problems.isEmpty()) {
            log.warning(problems.size() + " armor coverage problem(s). The server is still running.");
        }
        return problems.size();
    }

    private static int warnEmpty(Logger log) {
        log.warning("ArmorConsistency checked ZERO armor pieces. That is not a pass -- it means content/armor loaded "
                + "nothing, so every worn piece falls back to vanilla's armor points instead of its authored Defense, "
                + "and no armor set exists.");
        return 0;
    }

    /**
     * Every armor item the server has, keyed by its content token, with the slot it is worn in: an item whose vanilla
     * defaults grant armor in one of the four armor slots. Read through the SAME method the scan's native half uses.
     * Legacy constants are skipped; the BODY slot (horse and wolf armor) is not one of the four.
     */
    static Map<String, ArmorSlot> serverArmor() {
        Map<String, ArmorSlot> found = new LinkedHashMap<>();
        for (Material material : Material.values()) {
            if (material.isLegacy() || !material.isItem()) continue;
            for (ArmorSlot slot : ArmorSlot.values()) {
                if (DefenseModifierItems.vanillaArmorPoints(material.asItemType(), EquipmentSlot.valueOf(slot.name())) > 0) {
                    found.put(material.name().toLowerCase(Locale.ROOT), slot);
                }
            }
        }
        return found;
    }
}
