package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.recipe.RecipeDefinition;
import io.github.butterflysmp.rpg.core.recipe.RecipeRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Turns YAML into {@link RecipeDefinition}. The only class that knows the custom-recipe schema.
 *
 * <p>A recipe's id is its filename minus {@code .yml}, as with every other content type but tools
 * -- and here that is load-bearing rather than conventional, because <b>the id becomes the key half
 * of the {@code NamespacedKey} we register</b>. Two things follow:
 *
 * <ul>
 *   <li><b>The key is stable across restarts for free.</b> Nothing has to remember to keep it
 *       stable; the filesystem does.
 *   <li><b>Two recipes cannot claim one key</b>, because a directory holds one file of a given
 *       name. That is why the recipe axis of {@code CraftResultIndex} needs no contested-claim
 *       policy while the material axis does.
 * </ul>
 *
 * <p>Fails soft, exactly as {@code WeaponLoader} does: a malformed file is logged, named and
 * skipped, and the server keeps running. That contract matters more here than elsewhere, because
 * every shape rule {@link RecipeDefinition} enforces corresponds to a Bukkit method that THROWS --
 * an unvalidated definition reaching the registrar would abort {@code onEnable} over one bad file.
 */
public final class RecipeLoader {

    private final Logger log;

    public RecipeLoader(Logger log) {
        this.log = log;
    }

    public RecipeRegistry loadAll(File recipesDir) {
        RecipeRegistry registry = new RecipeRegistry();
        File[] files = recipesDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(idOf(f), yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed recipe '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " recipe file(s) were skipped. The server is still running, "
                    + "but that recipe cannot be crafted and whatever it mints is unobtainable.");
        }
        return registry;
    }

    /** The id is the filename: flint_staff.yml -> flint_staff. */
    private static String idOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    private RecipeDefinition parse(String id, ConfigurationSection s) {
        // A SCALAR `shape:` READS AS AN EMPTY LIST, not as an error -- getStringList returns an
        // empty list for a string value, so `shape: "FSS"` would arrive here as no shape at all.
        // RecipeDefinition refuses an empty shape, but the message would say "no shape:" for a file
        // that plainly has one, so name the real mistake. Same trap WeaponLoader guards for flavor.
        if (s.isString("shape")) {
            throw new IllegalArgumentException("recipe '" + id + "' has a scalar 'shape:'; it must "
                    + "be a YAML list, one '- \"ROW\"' per row");
        }
        List<String> shape = s.getStringList("shape");

        Map<Character, String> ingredients = new LinkedHashMap<>();
        ConfigurationSection section = s.getConfigurationSection("ingredients");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                // ONE CHARACTER, because a shape cell is one character. A two-character key could
                // never match any cell, so the recipe would register with an unmapped symbol and
                // match nothing -- which RecipeDefinition would report as a missing ingredient for
                // the first letter, naming the wrong problem.
                if (key.length() != 1) {
                    throw new IllegalArgumentException("recipe '" + id + "' has ingredient key '"
                            + key + "'; each key must be the single character it stands for in the "
                            + "shape");
                }
                ingredients.put(key.charAt(0), section.getString(key));
            }
        }

        // Everything else -- the shape bounds, the symbol/ingredient agreement, the id's legality
        // and a blank `mints:` -- is refused by the record, in core, where it is unit-tested.
        return new RecipeDefinition(id, shape, ingredients, s.getString("mints"));
    }
}
