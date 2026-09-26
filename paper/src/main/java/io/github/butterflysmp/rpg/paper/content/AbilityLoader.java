package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        List<String> declaringArchetype = new ArrayList<>();
        List<String> retired = new ArrayList<>();
        for (File f : files) {
            if (RETIRED_FILES.contains(f.getName())) {
                retired.add(f.getName());
                continue;
            }
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(yaml));
                if (yaml.contains(RETIRED_ARCHETYPE_KEY)) declaringArchetype.add(f.getName());
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed ability '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " ability file(s) were skipped. The server is still running, "
                    + "but that content is not loaded.");
        }
        // ONE warning per load, naming every file, rather than one per file. The file still LOADS: an
        // old copy in run/plugins/Rpg/content/ (saveResource never overwrites) must not lose an ability
        // over a key that now means nothing.
        if (!declaringArchetype.isEmpty()) {
            log.warning(declaringArchetype.size() + " ability file(s) still declare `"
                    + RETIRED_ARCHETYPE_KEY + ":`, a key the build system retired and this loader IGNORES: "
                    + declaringArchetype + ". Delete the line; pools decide who may cast an ability "
                    + "(content/builds/). A deployed copy is refreshed by ./scripts/dev-server.sh --refresh-content.");
        }
        if (!retired.isEmpty()) {
            log.warning(retired.size() + " ability file(s) in " + abilitiesDir.getPath() + " " + retired
                    + " name DELETED abilities and are NOT loaded. They are stale copies (saveResource never"
                    + " deletes); they can be deleted, and --refresh-content clears them on a dev server.");
        }
        return registry;
    }

    /**
     * Ability files DELETED by a ruling, skipped if a stale copy survives in a data folder. The stale-kit-file
     * handling (named once, never deleted), plus a skip: this loader reads every file in the directory, so
     * without it a stale copy would still register the deleted ability.
     *
     * <p>{@code arc_surge.yml}: ruling 20 (2026-09-26), PLAN-build-system.md.
     */
    static final java.util.Set<String> RETIRED_FILES = java.util.Set.of("arc_surge.yml");

    /**
     * {@code archetype:} -- RETIRED in the build system's slice 2 (PLAN-build-system.md section 2.2). It was
     * parsed into {@code AbilityDefinition.archetypeId} and read by NOTHING, and its values contradicted the
     * kits for two of the four fire abilities. Who may cast an ability is now its pool's listing. The key is
     * ignored with the one warning above, never refused.
     */
    static final String RETIRED_ARCHETYPE_KEY = "archetype";

    private AbilityDefinition parse(ConfigurationSection s) {
        return new AbilityDefinition(
                AbilitySchema.req(s, "id"),
                s.getString("display_name", AbilitySchema.req(s, "id")),
                AbilitySchema.req(s, "element"),
                s.getInt("cooldown_ticks", 0),
                AbilitySchema.parseCost(s.getConfigurationSection("cost")),
                AbilitySchema.parseCast(s.getConfigurationSection("cast")),
                AbilitySchema.parseEffects(s.getMapList("on_hit")),
                List.of(),   // a standalone ability carries no authored tooltip prose
                AbilitySchema.parseCastVisuals(s.getMapList("on_cast"))
        );
    }
}
