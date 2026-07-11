package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Turns YAML into AbilityDefinition. This assembles the ability record; the shared
 * cast/cost/effect grammar lives in AbilitySchema, which WeaponLoader reuses so a
 * weapon trigger parses identically to an ability.
 *
 * Adding a new ability = adding a .yml file. No recompile. That is the whole
 * point of the content pipeline; do not break it by special-casing abilities
 * in Java.
 *
 * Fails soft. A typo in the 400th weapon must not take the server down: a
 * malformed file is logged, named, and skipped, and every other ability still
 * loads. Errors here are content-authoring mistakes, not programming errors.
 */
public final class AbilityLoader {

    private final Logger log;

    public AbilityLoader(Logger log) {
        this.log = log;
    }

    public AbilityRegistry loadAll(File abilitiesDir) {
        AbilityRegistry registry = new AbilityRegistry();
        File[] files = abilitiesDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed ability '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " ability file(s) were skipped. The server is still running, "
                    + "but that content is not loaded.");
        }
        return registry;
    }

    private AbilityDefinition parse(ConfigurationSection s) {
        return new AbilityDefinition(
                AbilitySchema.req(s, "id"),
                s.getString("display_name", AbilitySchema.req(s, "id")),
                AbilitySchema.req(s, "element"),
                s.getString("archetype", "none"),
                s.getInt("cooldown_ticks", 0),
                AbilitySchema.parseCost(s.getConfigurationSection("cost")),
                AbilitySchema.parseCast(s.getConfigurationSection("cast")),
                AbilitySchema.parseEffects(s.getMapList("on_hit"))
        );
    }
}
