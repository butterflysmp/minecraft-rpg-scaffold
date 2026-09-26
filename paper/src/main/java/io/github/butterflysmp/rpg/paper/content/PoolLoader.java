package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.build.CellKey;
import io.github.butterflysmp.rpg.core.build.Loadout;
import io.github.butterflysmp.rpg.core.build.PoolDefinition;
import io.github.butterflysmp.rpg.core.build.PoolRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Loads {@code content/builds/<class>_<element>.yml}, one pool per cell (PLAN-build-system.md section 2.2).
 * The template of {@code KitLoader}, which this replaced: sorted, per-file, and a bad file is logged by name and skipped.
 *
 * <p><b>A pool naming an ability nothing defines is REFUSED, not warned about.</b> That differs from
 * {@code ContentValidator}, which only warns, and the difference is the point: a pool that loads with a
 * hole makes the Ability Stone cast {@code UnknownAbility} for every player of that cell, silently. So the
 * id check is made here, against the loaded ability registry, which is why this loads AFTER the abilities.
 */
public final class PoolLoader {

    private final Logger log;

    public PoolLoader(Logger log) {
        this.log = log;
    }

    public PoolRegistry loadAll(File buildsDir, Predicate<String> abilityExists) {
        PoolRegistry registry = new PoolRegistry();
        File[] files = buildsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;
        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                registry.register(parse(YamlConfiguration.loadConfiguration(f), abilityExists));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping pool '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " pool file(s) were skipped. The server is still running, but a "
                    + "player of that (class, element) cell has no Ability Stone loadout.");
        }
        return registry;
    }

    /** Package-private so the loader's rules are testable against a hand-built YAML section. */
    static PoolDefinition parse(ConfigurationSection s, Predicate<String> abilityExists) {
        CellKey cell = new CellKey(req(s, "class"), req(s, "element"));
        String displayName = s.getString("display_name", cell.classId() + " " + cell.elementId());
        List<String> ultimates = s.getStringList("ultimates");
        List<String> actives = s.getStringList("actives");

        ConfigurationSection def = s.getConfigurationSection("default");
        if (def == null) throw new IllegalArgumentException("Missing required section: default");
        List<String> defaultActives = def.getStringList("actives");
        if (defaultActives.size() != 2) {
            throw new IllegalArgumentException("default.actives must name exactly 2 abilities (left, right), got "
                    + defaultActives.size());
        }
        Loadout defaultLoadout = new Loadout(req(def, "ultimate"), defaultActives.get(0), defaultActives.get(1));

        List<String> unknown = new ArrayList<>();
        for (String id : ultimates) if (!abilityExists.test(id)) unknown.add(id);
        for (String id : actives) if (!abilityExists.test(id)) unknown.add(id);
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("names abilities nothing defines: " + unknown);
        }
        // The list rules (at least 1 ultimate and 2 actives, no id in both, default drawn from the
        // lists) are the record's, so a pool built any other way obeys them too.
        return new PoolDefinition(cell, displayName, ultimates, actives, defaultLoadout);
    }

    private static String req(ConfigurationSection s, String path) {
        String v = s.getString(path);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing required field: " + path);
        return v;
    }
}
