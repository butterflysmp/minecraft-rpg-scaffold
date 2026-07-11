package io.github.butterflysmp.rpg.paper.content;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Turns YAML into ElementDefinition. The only class that knows the element schema, which
 * is nothing but an id and a display name -- an element carries no logic.
 *
 * An element's id is its filename minus .yml, as with the other content types. Fails soft:
 * a malformed file is logged, named, and skipped, and every other element still loads.
 */
public final class ElementLoader {

    private final Logger log;

    public ElementLoader(Logger log) {
        this.log = log;
    }

    public ElementRegistry loadAll(File elementsDir) {
        ElementRegistry registry = new ElementRegistry();
        File[] files = elementsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(idOf(f), yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed element '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " element file(s) were skipped. The server is still running, "
                    + "but that element is not loaded.");
        }
        return registry;
    }

    /** The id is the filename: fire.yml -> fire. */
    private static String idOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    private ElementDefinition parse(String id, ConfigurationSection s) {
        return new ElementDefinition(id, s.getString("display_name", id));
    }
}
