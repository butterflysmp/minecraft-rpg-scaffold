package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.kit.KitDefinition;
import io.github.butterflysmp.rpg.core.kit.KitRegistry;
import io.github.butterflysmp.rpg.core.kit.WeaponGrant;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Turns YAML into KitDefinition. The only class that knows the kit schema. A kit is one cell
 * of the (class, element) grid; its key is the (class, element) pair it DECLARES, not its
 * filename -- the filename is a convention (ranger_fire.yml), never parsed.
 *
 * Fails soft, like the other loaders: a malformed file is logged, named, and skipped, and a
 * kit granting nothing is malformed (KitDefinition rejects it), so it never quietly ships a
 * (class, element) cell nobody can play.
 */
public final class KitLoader {

    private final Logger log;

    public KitLoader(Logger log) {
        this.log = log;
    }

    public KitRegistry loadAll(File kitsDir) {
        KitRegistry registry = new KitRegistry();
        File[] files = kitsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed kit '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " kit file(s) were skipped. The server is still running, "
                    + "but that (class, element) cell is not available.");
        }
        return registry;
    }

    private KitDefinition parse(ConfigurationSection s) {
        String classId = req(s, "class");
        String elementId = req(s, "element");
        String displayName = s.getString("display_name", classId + " " + elementId);
        List<WeaponGrant> weapons = parseWeapons(s.getMapList("weapons"));
        List<String> abilities = s.getStringList("abilities");
        // KitDefinition rejects a cell that grants nothing -- caught above, named, skipped.
        return new KitDefinition(classId, elementId, displayName, weapons, abilities);
    }

    private static String req(ConfigurationSection s, String path) {
        String v = s.getString(path);
        if (v == null) throw new IllegalArgumentException("Missing required field: " + path);
        return v;
    }

    /** Each weapon grant is a map { id: <weapon>, equip: <bool> }; equip defaults false. */
    private List<WeaponGrant> parseWeapons(List<Map<?, ?>> raw) {
        List<WeaponGrant> out = new ArrayList<>();
        for (Map<?, ?> m : raw) {
            Object id = m.get("id");
            if (id == null) throw new IllegalArgumentException("weapon grant is missing its 'id'");
            out.add(new WeaponGrant(String.valueOf(id), Boolean.TRUE.equals(m.get("equip"))));
        }
        return out;
    }
}
