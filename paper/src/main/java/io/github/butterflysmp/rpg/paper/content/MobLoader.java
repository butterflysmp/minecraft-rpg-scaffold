package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Turns YAML into {@link MobDefinition}. The only class that knows the mob schema.
 *
 * A mob's id is its filename minus .yml, as with every other content type. Fails soft: a malformed
 * file is logged, NAMED, and skipped, and every other mob still loads -- so one bad file cannot take
 * the server's mob content down with it.
 *
 * {@code base_entity} has no default on purpose. Rarity defaults to common and material defaults to a
 * sword because a wrong guess there is cosmetic; a wrong guess at which creature to spawn is not. A
 * missing value throws, which turns it into a named, skipped file at boot -- the same treatment a
 * weapon's missing {@code class} gets, and for the same reason.
 */
public final class MobLoader {

    private final Logger log;

    public MobLoader(Logger log) {
        this.log = log;
    }

    public MobRegistry loadAll(File mobsDir) {
        MobRegistry registry = new MobRegistry();
        File[] files = mobsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(idOf(f), yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed mob '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " mob file(s) were skipped. The server is still running, "
                    + "but that mob cannot be spawned.");
        }
        return registry;
    }

    /** The id is the filename: knell.yml -> knell. */
    private static String idOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    /**
     * MobDefinition rejects a blank base_entity, a blank display name and a non-positive max_health --
     * caught by loadAll, named, skipped. Whether the base_entity NAMES A REAL living entity is not
     * knowable here (it needs the Bukkit registry, which needs a running server), so ContentValidator
     * checks that at boot instead, the same way a dangling visual_id is checked.
     */
    private MobDefinition parse(String id, ConfigurationSection s) {
        return new MobDefinition(
                id,
                s.getString("base_entity"),
                s.getString("display_name", id),
                s.getDouble("max_health", 0.0));
    }
}
