package io.github.butterflysmp.rpg.paper.content;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Turns YAML into EnchantDefinition. The only class that knows the enchant schema, which is an id,
 * a display name and a maximum level -- and no behaviour, deliberately (see EnchantDefinition).
 *
 * An enchant's id is its filename minus .yml, as with the other content types. Fails soft: a
 * malformed file is logged, named, and skipped, and every other enchant still loads.
 *
 * <p>The ROSTER pass extends this additively -- a pool or weight field, and more files -- rather
 * than replacing it. That is the whole reason identity is content in Pass 1 despite the roster
 * being deferred: an in-Java identity would have cost that pass a migration through this one's code.
 */
public final class EnchantLoader {

    private final Logger log;

    public EnchantLoader(Logger log) {
        this.log = log;
    }

    public EnchantRegistry loadAll(File enchantsDir) {
        EnchantRegistry registry = new EnchantRegistry();
        File[] files = enchantsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(idOf(f), yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed enchant '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " enchant file(s) were skipped. The server is still running, "
                    + "but that enchant will render as its raw id and cannot be granted.");
        }
        return registry;
    }

    /** The id is the filename: unbreaking.yml -> unbreaking. */
    private static String idOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    private EnchantDefinition parse(String id, ConfigurationSection s) {
        return new EnchantDefinition(id, s.getString("display_name", id), s.getInt("max_level", 1));
    }
}
