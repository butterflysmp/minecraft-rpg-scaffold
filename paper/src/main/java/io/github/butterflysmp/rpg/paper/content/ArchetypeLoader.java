package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.archetype.Archetype;
import io.github.butterflysmp.rpg.core.archetype.ArchetypeRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Turns YAML into Archetype. The only class that knows the archetype schema.
 *
 * A class's id is its filename minus .yml, as with visuals and statuses. Fails
 * soft, like the other loaders: a malformed file is logged, named, and skipped.
 *
 * A class that grants nothing is a defect, not an empty class -- an empty or
 * missing abilities list is treated as malformed and skipped, so it never quietly
 * ships a class nobody can play.
 */
public final class ArchetypeLoader {

    private final Logger log;

    public ArchetypeLoader(Logger log) {
        this.log = log;
    }

    public ArchetypeRegistry loadAll(File archetypesDir) {
        ArchetypeRegistry registry = new ArchetypeRegistry();
        File[] files = archetypesDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(idOf(f), yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed archetype '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " archetype file(s) were skipped. The server is still running, "
                    + "but that class is not loaded.");
        }
        return registry;
    }

    /** The id is the filename: hunter.yml -> hunter. */
    private static String idOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    private Archetype parse(String id, ConfigurationSection s) {
        String displayName = s.getString("display_name", id);
        List<String> abilities = s.getStringList("abilities");
        if (abilities.isEmpty()) {
            throw new IllegalArgumentException("archetype '" + id + "' grants no abilities");
        }
        return new Archetype(id, displayName, abilities);
    }
}
