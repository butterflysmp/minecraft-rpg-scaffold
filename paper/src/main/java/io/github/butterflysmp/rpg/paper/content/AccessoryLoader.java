package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.accessory.AccessoryNegatives;
import io.github.butterflysmp.rpg.core.accessory.AccessorySlotKind;
import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import io.github.butterflysmp.rpg.core.accessory.AccessoryType;
import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.core.weapon.AccessoryRegistry;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * {@code content/accessories/<id>.yml} -> {@link AccessoryDefinition}. One file per accessory, the id
 * is the filename. The {@code ShieldLoader} shape: sorted, per file, a bad file logged by name and
 * skipped.
 *
 * <pre>
 * display_name: "&lt;gold&gt;Fletcher's Quiver&lt;/gold&gt;"
 * rarity: rare
 * material: shulker_shell        # one of AccessoryDefinition.MATERIALS
 * slot: class                    # universal | class
 * class: ranger                  # class items only: melee | ranger | mage
 * type: quiver                   # class items only: gauntlet | quiver | scroll
 * modifiers:
 *   class_damage: 3
 *   crit_chance: 0.05
 *   health_regen: -0.04          # a drawback -- see AccessoryNegatives
 * flavor:
 *   - "..."
 * </pre>
 *
 * <h2>What this adds to the definition's own refusals</h2>
 *
 * <ul>
 *   <li><b>Every key under {@code modifiers} must be a known stat</b> -- an unknown one refuses the
 *       file rather than being ignored, because an ignored modifier is a tooltip line that is simply
 *       missing, with nothing to say why.
 *   <li><b>Negatives</b>, through {@link AccessoryNegatives} (ruling Q7). They are checked here and
 *       not in the record because one bound needs the max-mana base, which this is handed.
 *   <li><b>An id another gear kind already holds is REFUSED</b>, not warned about. The other four
 *       kinds only warn on a collision, and {@code /rpg give} takes the first match, so a colliding
 *       accessory would be unmintable while looking loaded. A new kind should not inherit that.
 * </ul>
 */
public final class AccessoryLoader {

    private final Logger log;
    private final double maxManaBase;

    /**
     * @param maxManaBase the base mana pool, for the max-mana drawback bound. {@code RpgPlugin} owns
     *                    the number and passes it, rather than core keeping a second copy.
     */
    public AccessoryLoader(Logger log, double maxManaBase) {
        this.log = log;
        this.maxManaBase = maxManaBase;
    }

    /**
     * @param takenByAnotherKind true for an id a weapon, shield, armour piece or tool already holds
     */
    public AccessoryRegistry loadAll(File dir, Predicate<String> takenByAnotherKind) {
        AccessoryRegistry registry = new AccessoryRegistry();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                String id = idOf(f);
                if (takenByAnotherKind.test(id)) {
                    throw new IllegalArgumentException("id '" + id + "' is already a weapon, shield,"
                            + " armour piece or tool; /rpg give would mint that instead. Rename the"
                            + " accessory -- ids are unique across all five gear kinds");
                }
                registry.register(parse(id, YamlConfiguration.loadConfiguration(f)));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed accessory '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " accessory file(s) were skipped. The server is still running, "
                    + "but those accessories are not loaded.");
        }
        return registry;
    }

    private static String idOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    private AccessoryDefinition parse(String id, ConfigurationSection s) {
        String displayName = s.getString("display_name", id);
        Rarity rarity = Rarity.fromName(s.getString("rarity", "common"));
        if (rarity == null) {
            throw new IllegalArgumentException("accessory '" + id + "' has unknown rarity '"
                    + s.getString("rarity") + "'");
        }

        String slotToken = s.getString("slot");
        AccessorySlotKind slot = AccessorySlotKind.fromName(slotToken);
        if (slot == null) {
            throw new IllegalArgumentException("accessory '" + id + "' has slot '" + slotToken
                    + "'; expected universal or class");
        }

        WeaponClass accessoryClass = null;
        String classToken = s.getString("class");
        if (classToken != null) {
            accessoryClass = weaponClass(classToken);
            if (accessoryClass == null) {
                throw new IllegalArgumentException("accessory '" + id + "' has unknown class '"
                        + classToken + "'; expected melee, ranger or mage");
            }
        }

        AccessoryType type = null;
        String typeToken = s.getString("type");
        if (typeToken != null) {
            type = AccessoryType.fromName(typeToken);
            if (type == null) {
                throw new IllegalArgumentException("accessory '" + id + "' has unknown type '"
                        + typeToken + "'; expected gauntlet, quiver or scroll");
            }
        }

        Map<AccessoryStat, Double> modifiers = new EnumMap<>(AccessoryStat.class);
        ConfigurationSection section = s.getConfigurationSection("modifiers");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                AccessoryStat stat = AccessoryStat.fromName(key);
                if (stat == null) {
                    throw new IllegalArgumentException("accessory '" + id + "' modifies unknown stat '"
                            + key + "'; an accessory may modify only " + Arrays.stream(AccessoryStat.values())
                            .map(AccessoryStat::token).toList());
                }
                if (!section.isDouble(key) && !section.isInt(key)) {
                    throw new IllegalArgumentException("accessory '" + id + "' modifier '" + key
                            + "' is not a number: " + section.get(key));
                }
                double amount = section.getDouble(key);
                String refusal = AccessoryNegatives.refusal(stat, amount, maxManaBase);
                if (refusal != null) {
                    throw new IllegalArgumentException("accessory '" + id + "': " + refusal);
                }
                modifiers.put(stat, amount);
            }
        }

        if (s.isString("flavor")) {
            log.warning("Accessory '" + id + "' has a scalar 'flavor:'; it must be a YAML list "
                    + "(one '- ' item per line). Ignoring it.");
        }

        return new AccessoryDefinition(id, displayName, rarity, s.getString("material"), slot,
                accessoryClass, type, modifiers, s.getStringList("flavor"));
    }

    private static WeaponClass weaponClass(String token) {
        for (WeaponClass c : WeaponClass.values()) {
            if (c.name().equalsIgnoreCase(token.trim())) return c;
        }
        return null;
    }
}
