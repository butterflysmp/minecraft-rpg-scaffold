package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import io.github.butterflysmp.rpg.core.build.FragmentDefinition;
import io.github.butterflysmp.rpg.core.build.FragmentRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Loads {@code content/fragments/<id>.yml} (PLAN-build-system.md section 2.3). The id is the filename.
 *
 * <pre>
 *   display_name: "&lt;gold&gt;Ember Heart&lt;/gold&gt;"   MiniMessage; defaults to the id
 *   icon: blaze_powder                              the Build screen's icon -- never minted, never an item
 *   description: ["A little more of you burns."]
 *   modifiers:
 *     max_health: 4                                 AccessoryStat's tokens, REUSED; positive-only (ruling 21)
 *
 *   -- OR, a BEHAVIOUR fragment (section 7.3), never both:
 *   target: recall                                  one ability; the pools that list this fragment must offer it
 *   add_on_hit: [ ... ]                             appended to the target's on_hit (AbilitySchema's parser)
 * </pre>
 *
 * <p>A bad file is SKIPPED BY NAME and the rest load, as every loader here does. What is refused, and
 * where: an unknown stat, a non-number and an icon that names no item HERE; a negative (ruling 21),
 * {@code class_damage} and an empty modifier map in {@link FragmentDefinition}, the one home of those rules.
 */
public final class FragmentLoader {

    private final Logger log;
    private final Predicate<String> itemMaterial;

    /**
     * @param itemMaterial does this icon name resolve to an ITEM material. Passed in, as ContentValidator's
     *                     {@code materialExists} is, because resolving a Material initialises the server's
     *                     registries -- {@code RpgPlugin} hands in {@code Material.matchMaterial}, a test a fake.
     */
    public FragmentLoader(Logger log, Predicate<String> itemMaterial) {
        this.log = log;
        this.itemMaterial = itemMaterial;
    }

    public FragmentRegistry loadAll(File fragmentsDir) {
        FragmentRegistry registry = new FragmentRegistry();
        File[] files = fragmentsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;
        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                registry.register(parse(f.getName().substring(0, f.getName().length() - ".yml".length()),
                        YamlConfiguration.loadConfiguration(f), itemMaterial));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping fragment '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " fragment file(s) were skipped. The server is still running, but a pool"
                    + " naming one of them will be refused.");
        }
        return registry;
    }

    static FragmentDefinition parse(String id, ConfigurationSection s, Predicate<String> itemMaterial) {
        String icon = s.getString("icon");
        if (icon == null || !itemMaterial.test(icon.toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("icon '" + icon + "' names no item material");
        }
        Map<AccessoryStat, Double> modifiers = new EnumMap<>(AccessoryStat.class);
        ConfigurationSection section = s.getConfigurationSection("modifiers");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                AccessoryStat stat = AccessoryStat.fromName(key);
                if (stat == null) {
                    throw new IllegalArgumentException("modifies unknown stat '" + key + "'; a fragment may modify only "
                            + Arrays.stream(AccessoryStat.values()).filter(st -> st != AccessoryStat.CLASS_DAMAGE)
                            .map(AccessoryStat::token).toList());
                }
                if (!section.isDouble(key) && !section.isInt(key)) {
                    throw new IllegalArgumentException("modifier '" + key + "' is not a number: " + section.get(key));
                }
                modifiers.put(stat, section.getDouble(key));
            }
        }
        // Section 7.3: a BEHAVIOUR fragment appends to one ability, through the aspects' parser. It never moves a
        // number and carries no on_cast, so both keys are refused by name rather than silently ignored.
        for (String refused : List.of("modify", "add_on_cast")) {
            if (s.contains(refused)) {
                throw new IllegalArgumentException("`" + refused + ":` is refused on a fragment -- a behaviour fragment"
                        + " may carry only `target:` and `add_on_hit:` (PLAN-build-system.md section 7.3)");
            }
        }
        String target = s.getString("target");
        List<io.github.butterflysmp.rpg.core.ability.effect.EffectSpec> addOnHit =
                AbilitySchema.parseEffects(s.getMapList("add_on_hit"));
        return new FragmentDefinition(id, s.getString("display_name"), icon, s.getStringList("description"), modifiers,
                target, addOnHit);
    }
}
