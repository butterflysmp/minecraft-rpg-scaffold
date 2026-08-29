package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Turns YAML into {@link ShieldDefinition}. The only class that knows the shield schema.
 *
 * The shape of {@link WeaponLoader}, minus everything a shield has none of: no triggers, so no
 * {@code AbilitySchema} reuse; no class, so nothing is required beyond the file existing; no
 * element. A shield's whole mechanical content is one number.
 *
 * <p>A shield's id is its filename minus {@code .yml}, as with every other content type. Fails
 * soft: a malformed file is logged, NAMED and skipped, and the server still boots. The refusals
 * live in {@code ShieldDefinition}'s constructor rather than here, so the schema knows how to READ
 * the file and the model knows what is legal -- which is what lets an out-of-range
 * {@code block_dr} arrive as "Skipping malformed shield 'x.yml': ... block_dr 2.0; it must be
 * between 0 and 1" instead of as a shield that heals whoever holds it.
 */
public final class ShieldLoader {

    private final Logger log;

    public ShieldLoader(Logger log) {
        this.log = log;
    }

    public ShieldRegistry loadAll(File shieldsDir) {
        ShieldRegistry registry = new ShieldRegistry();
        File[] files = shieldsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(idOf(f), yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed shield '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " shield file(s) were skipped. The server is still running, "
                    + "but that shield is not loaded.");
        }
        return registry;
    }

    /** The id is the filename: shield.yml -> shield. */
    private static String idOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    private ShieldDefinition parse(String id, ConfigurationSection s) {
        String displayName = s.getString("display_name", id);
        Rarity rarity = rarity(s.getString("rarity", "common"), id);
        String material = s.getString("material", ShieldDefinition.DEFAULT_MATERIAL);

        // No default worth having. A shield with no block_dr: is a shield that blocks nothing, and
        // defaulting it to some fraction would invent a mechanic the file never asked for -- so the
        // default is 0 and the resulting "Block: 0%" tooltip says so out loud. Contrast the weapon
        // loader's attack_damage, which defaults the same way and for the same reason.
        double blockDr = s.getDouble("block_dr", 0.0);

        // A scalar where a list belongs is silently ignored by getStringList, which is how a
        // one-line flavor: disappears with no explanation. Warn, exactly as WeaponLoader does.
        if (s.isString("flavor")) {
            log.warning("Shield '" + id + "' has a scalar 'flavor:'; it must be a YAML list "
                    + "(one '- ' item per line). Ignoring it.");
        }

        return new ShieldDefinition(id, displayName, rarity, material, blockDr,
                s.getStringList("flavor"));
    }

    /** Unknown rarity throws, which the caller turns into a named, skipped file. */
    private static Rarity rarity(String name, String id) {
        Rarity rarity = Rarity.fromName(name);
        if (rarity == null) {
            throw new IllegalArgumentException("shield '" + id + "' has unknown rarity '" + name + "'");
        }
        return rarity;
    }
}
