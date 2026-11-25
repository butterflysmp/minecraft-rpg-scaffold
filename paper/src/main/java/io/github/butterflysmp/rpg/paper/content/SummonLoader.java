package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.summon.SummonDefinition;
import io.github.butterflysmp.rpg.core.summon.SummonRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * {@code content/summons/<id>.yml} (D2, the seat's 3a, 2026-11-24): the ONLY class that knows the summon schema.
 *
 * <pre>
 *   entity: wolf                  # a living entity type, by its key
 *   name: "{owner}'s Wolf"        # what floats over it; {owner} is his name
 *   health: 100                   # authored; x gear score / 100 at the cast
 *   damage: 20                    # authored; x gear score / 100 at the cast
 *   guard_radius: 8               # BEN
 *   trigger_radius: 30            # BEN
 *   chase_limit: 32               # SEAT PLACEHOLDER, from the caster
 *   teleport_back: 48             # SEAT PLACEHOLDER
 *   attack_cadence_ticks: 20      # SEAT PLACEHOLDER
 *   damaged_window_ticks: 100     # SEAT PLACEHOLDER
 * </pre>
 *
 * <p>REFUSED, the file named and skipped: an unknown entity, an `element:` key (E1: the hit is the WEAPON's element), a non-positive health or damage, and a missing D4
 * number (every one of the six is required: no default is invented). The entity check is injected so a
 * test can run without a server's registries.
 */
public final class SummonLoader {

    /** The D4 numbers a file must declare. */
    static final List<String> REQUIRED = List.of("health", "damage", "guard_radius", "trigger_radius", "chase_limit",
            "teleport_back", "attack_cadence_ticks", "damaged_window_ticks");

    private final Logger log;
    private final Predicate<String> knownEntity;

    public SummonLoader(Logger log, Predicate<String> knownEntity) {
        this.log = log;
        this.knownEntity = knownEntity;
    }

    /** Every summon in the directory. A directory that does not exist is no summons (none ship in 3a). */
    public SummonRegistry loadAll(File dir) {
        SummonRegistry registry = new SummonRegistry();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;
        Arrays.sort(files);
        int skipped = 0;
        for (File f : files) {
            try {
                registry.register(parse(f.getName().substring(0, f.getName().length() - 4),
                        YamlConfiguration.loadConfiguration(f)));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping summon '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) log.warning(skipped + " summon file(s) were skipped; those summons are not loaded.");
        return registry;
    }

    SummonDefinition parse(String id, ConfigurationSection s) {
        for (String key : REQUIRED) {
            if (!s.isInt(key) && !s.isDouble(key)) {
                throw new IllegalArgumentException("'" + key + "' is missing or not a number; it has no default");
            }
            if (key.endsWith("_ticks") && !s.isInt(key)) {
                throw new IllegalArgumentException("'" + key + "' must be a whole number of ticks, was " + s.get(key));
            }
        }
        String entity = s.getString("entity");
        if (entity == null || !knownEntity.test(entity)) {
            throw new IllegalArgumentException("unknown entity '" + entity + "'");
        }
        if (s.contains("element")) {
            throw new IllegalArgumentException("declares an element; a summon hits in its WEAPON's element (E1)");
        }
        return new SummonDefinition(id, entity, s.getString("name", id), s.getDouble("health"),
                s.getDouble("damage"), s.getDouble("guard_radius"), s.getDouble("trigger_radius"),
                s.getDouble("chase_limit"), s.getDouble("teleport_back"), s.getInt("attack_cadence_ticks"),
                s.getInt("damaged_window_ticks"));
    }
}
