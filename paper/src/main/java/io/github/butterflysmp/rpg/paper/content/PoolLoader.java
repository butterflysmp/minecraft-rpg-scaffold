package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.build.CellKey;
import io.github.butterflysmp.rpg.core.build.ClassPool;
import io.github.butterflysmp.rpg.core.build.Loadout;
import io.github.butterflysmp.rpg.core.build.PoolDefinition;
import io.github.butterflysmp.rpg.core.build.PoolRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Loads {@code content/builds/<class>_<element>.yml}, one pool per cell (PLAN-build-system.md section 2.2), and
 * {@code content/builds/<class>.yml}, what EVERY cell of a class offers (section 7.1, ruling 24). The template of
 * {@code KitLoader}, which this replaced: sorted, per-file, and a bad file is logged by name and skipped.
 *
 * <p><b>TWO KINDS OF FILE, TOLD APART BY {@code element:}.</b> A file with no {@code element:} is a CLASS file,
 * and its filename stem must equal its {@code class:} -- so a cell file that merely forgot its element (stem
 * {@code ranger_fire}, class {@code ranger}) is refused as loudly as before, never read as a class file. A class
 * file may carry {@code class}, {@code actives} and {@code ultimates}; anything else is refused by name, because
 * a silently ignored key reads as a feature that works.
 *
 * <p><b>TWO PASSES.</b> Every class file is parsed first ({@link ClassPool}); then each cell is parsed with its
 * class's ids MERGED IN before its {@link PoolDefinition} is built, so every rule below runs on the merged
 * lists. A refused class file leaves its cells without its entries, and the existing checks then refuse exactly
 * the cells that depended on it.
 *
 * <p><b>A pool naming an ability nothing defines is REFUSED, not warned about.</b> That differs from
 * {@code ContentValidator}, which only warns, and the difference is the point: a pool that loads with a
 * hole makes the Ability Stone cast {@code UnknownAbility} for every player of that cell, silently. So the
 * id check is made here, against the loaded ability registry, which is why this loads AFTER the abilities.
 */
public final class PoolLoader {

    /** The keys a class file may carry (section 7.1). */
    static final Set<String> CLASS_FILE_KEYS = Set.of("class", "actives", "ultimates");

    private final Logger log;

    public PoolLoader(Logger log) {
        this.log = log;
    }

    /** {@link #loadAll(File, Predicate, Predicate, Function, Function)} with no behaviour fragments. */
    public PoolRegistry loadAll(File buildsDir, Predicate<String> abilityExists, Predicate<String> fragmentExists,
                                Function<String, Optional<String>> aspectTarget) {
        return loadAll(buildsDir, abilityExists, fragmentExists, aspectTarget, id -> Optional.empty());
    }

    /**
     * @param aspectTarget   the target ability of an aspect id, or empty if no aspect has that id (slice 5)
     * @param fragmentTarget the target ability of a BEHAVIOUR fragment id, or empty for a stat fragment or none
     *                       (section 7.3)
     */
    public PoolRegistry loadAll(File buildsDir, Predicate<String> abilityExists, Predicate<String> fragmentExists,
                                Function<String, Optional<String>> aspectTarget,
                                Function<String, Optional<String>> fragmentTarget) {
        PoolRegistry registry = new PoolRegistry();
        File[] files = buildsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;
        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;

        // Pass 1: the class files.
        Map<String, ClassPool> classPools = new LinkedHashMap<>();
        List<File> cellFiles = new ArrayList<>();
        Map<File, YamlConfiguration> yaml = new LinkedHashMap<>();
        for (File f : files) {
            YamlConfiguration s = YamlConfiguration.loadConfiguration(f);
            yaml.put(f, s);
            if (s.contains("element")) {
                cellFiles.add(f);
                continue;
            }
            try {
                ClassPool pool = parseClass(stem(f), s, abilityExists);
                classPools.put(pool.classId(), pool);
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping class pool '" + f.getName() + "': " + ex.getMessage()
                        + ". Every cell of that class loads WITHOUT its entries.");
            }
        }

        // Pass 2: the cells, each with its class's ids merged in.
        for (File f : cellFiles) {
            try {
                YamlConfiguration s = yaml.get(f);
                registry.register(parse(s, Optional.ofNullable(classPools.get(s.getString("class"))), abilityExists,
                        fragmentExists, aspectTarget, fragmentTarget));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping pool '" + f.getName() + "': " + ex.getMessage());
            }
        }
        for (String classId : classPools.keySet()) {
            if (registry.all().stream().noneMatch(p -> p.cell().classId().equals(classId))) {
                log.warning("Class pool '" + classId + ".yml' has no cell to offer its abilities to: no loaded "
                        + classId + "_<element>.yml. It offers nothing.");
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " pool file(s) were skipped. The server is still running, but a "
                    + "player of that (class, element) cell has no Ability Stone loadout.");
        }
        return registry;
    }

    /** A class file (section 7.1). Package-private so its rules are testable against a hand-built YAML section. */
    static ClassPool parseClass(String stem, ConfigurationSection s, Predicate<String> abilityExists) {
        String classId = req(s, "class");
        if (!classId.equals(stem)) {
            throw new IllegalArgumentException("has no `element:`, so it is read as a CLASS file, but its name '"
                    + stem + ".yml' is not its class '" + classId + "'. A cell file needs `element:`; a class file"
                    + " is named <class>.yml");
        }
        List<String> refusedKeys = s.getKeys(false).stream().filter(k -> !CLASS_FILE_KEYS.contains(k)).sorted().toList();
        if (!refusedKeys.isEmpty()) {
            throw new IllegalArgumentException("a class file may carry only " + CLASS_FILE_KEYS.stream().sorted().toList()
                    + ", and this one carries " + refusedKeys + " (fragments, aspects and a default belong to a cell)");
        }
        ClassPool pool = new ClassPool(classId, s.getStringList("ultimates"), s.getStringList("actives"));
        List<String> unknown = new ArrayList<>();
        for (String id : pool.ultimates()) if (!abilityExists.test(id)) unknown.add(id);
        for (String id : pool.actives()) if (!abilityExists.test(id)) unknown.add(id);
        if (!unknown.isEmpty()) throw new IllegalArgumentException("names abilities nothing defines: " + unknown);
        return pool;
    }

    /** A cell with no class file and no behaviour fragments -- the pre-section-7 form, kept for its tests. */
    static PoolDefinition parse(ConfigurationSection s, Predicate<String> abilityExists,
                                Predicate<String> fragmentExists, Function<String, Optional<String>> aspectTarget) {
        return parse(s, Optional.empty(), abilityExists, fragmentExists, aspectTarget, id -> Optional.empty());
    }

    /** Package-private so the loader's rules are testable against a hand-built YAML section. */
    static PoolDefinition parse(ConfigurationSection s, Optional<ClassPool> classPool, Predicate<String> abilityExists,
                                Predicate<String> fragmentExists, Function<String, Optional<String>> aspectTarget,
                                Function<String, Optional<String>> fragmentTarget) {
        CellKey cell = new CellKey(req(s, "class"), req(s, "element"));
        String displayName = s.getString("display_name", cell.classId() + " " + cell.elementId());
        List<String> ownUltimates = s.getStringList("ultimates");
        List<String> ownActives = s.getStringList("actives");
        // Section 7.1: the class file's ids, merged in AFTER the cell's own and BEFORE any rule below runs.
        // An id listed in both, in the same role, is refused here by ClassPool; in different roles, by the
        // PoolDefinition constructor's "both an ultimate and an active".
        List<String> ultimates = classPool.map(c -> c.mergeUltimates(ownUltimates)).orElse(ownUltimates);
        List<String> actives = classPool.map(c -> c.mergeActives(ownActives)).orElse(ownActives);
        // Slice 4: the fragments this cell offers. Optional -- a pool with none has four empty slots.
        List<String> fragments = s.getStringList("fragments");
        // Slice 5: the aspects this cell offers. Optional, like the fragments.
        List<String> aspects = s.getStringList("aspects");

        ConfigurationSection def = s.getConfigurationSection("default");
        if (def == null) throw new IllegalArgumentException("Missing required section: default");
        List<String> defaultActives = def.getStringList("actives");
        if (defaultActives.size() != 2) {
            throw new IllegalArgumentException("default.actives must name exactly 2 abilities (left, right), got "
                    + defaultActives.size());
        }
        Loadout defaultLoadout = new Loadout(req(def, "ultimate"), defaultActives.get(0), defaultActives.get(1));

        List<String> unknown = new ArrayList<>();
        for (String id : ownUltimates) if (!abilityExists.test(id)) unknown.add(id);
        for (String id : ownActives) if (!abilityExists.test(id)) unknown.add(id);
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("names abilities nothing defines: " + unknown);
        }
        // The list rules (at least 1 ultimate and 2 actives, no id in both, default drawn from the
        // lists) are the record's, so a pool built any other way obeys them too.
        // A fragment id nothing defines is REFUSED, the same rule as an ability: a pool offering a fragment
        // the Build screen cannot render or apply is a pool that lies. Checked AFTER the abilities, so one
        // refusal names one kind.
        List<String> unknownFragments = new ArrayList<>();
        for (String id : fragments) if (!fragmentExists.test(id)) unknownFragments.add(id);
        if (!unknownFragments.isEmpty()) {
            throw new IllegalArgumentException("names fragments nothing defines: " + unknownFragments);
        }
        // Section 7.3: a BEHAVIOUR fragment whose target this pool does not offer could never be active here --
        // the aspects' rule below, for the same reason.
        List<String> badFragments = new ArrayList<>();
        for (String id : fragments) {
            fragmentTarget.apply(id).ifPresent(target -> {
                if (!ultimates.contains(target) && !actives.contains(target)) {
                    badFragments.add(id + " (targets " + target + ", which this pool does not offer)");
                }
            });
        }
        if (!badFragments.isEmpty()) {
            throw new IllegalArgumentException("names behaviour fragments it cannot offer: " + badFragments);
        }
        // An aspect nothing defines is REFUSED, and so is one whose TARGET this pool does not offer (section 2.4:
        // "must be in the same pool") -- an aspect on an ability the player cannot equip here could never be
        // active, and the Build screen would offer a choice that does nothing.
        List<String> badAspects = new ArrayList<>();
        for (String id : aspects) {
            Optional<String> target = aspectTarget.apply(id);
            if (target.isEmpty()) {
                badAspects.add(id + " (nothing defines it)");
            } else if (!ultimates.contains(target.get()) && !actives.contains(target.get())) {
                badAspects.add(id + " (targets " + target.get() + ", which this pool does not offer)");
            }
        }
        if (!badAspects.isEmpty()) {
            throw new IllegalArgumentException("names aspects it cannot offer: " + badAspects);
        }
        return new PoolDefinition(cell, displayName, ultimates, actives, defaultLoadout, fragments, aspects);
    }

    private static String stem(File f) {
        return f.getName().substring(0, f.getName().length() - ".yml".length());
    }

    private static String req(ConfigurationSection s, String path) {
        String v = s.getString(path);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("Missing required field: " + path);
        return v;
    }
}
