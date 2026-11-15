package io.github.butterflysmp.rpg.paper.content;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Turns {@code content/mob_elements.yml} into the vanilla table: a bare entity-type key to an element id
 * (PLAN-dragon-mobs.md, N2). It exists because a vanilla Enderman is Void (Ben, 2026-11-15) and has no mob file. A
 * mob file's own {@code element} wins over this table ({@code core.mob.MobElementResolver}).
 *
 * <p><b>It lives at the content root, NOT in {@code content/mobs/}</b>: {@code MobLoader.loadAll} parses every
 * {@code *.yml} there as a mob, so a table file in that directory would be a malformed mob.
 *
 * <p>Fail-soft per ROW, as {@code withering.yml}'s immune list is: a row whose type names no entity, or whose
 * element names no element, is REFUSED with a {@code Refusing} WARN naming it, so R0c's pattern catches it at boot,
 * and the rest of the table loads.
 */
public final class MobElementLoader {

    private final Logger log;
    private final Predicate<String> elementExists;

    public MobElementLoader(Logger log, Predicate<String> elementExists) {
        this.log = log;
        this.elementExists = elementExists;
    }

    /** The table, or an empty one if the file is absent. Never null. */
    public Map<String, String> load(File file) {
        Map<String, String> out = new LinkedHashMap<>();
        if (!file.isFile()) return out;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String raw : yaml.getKeys(false)) {
            String type = raw.toLowerCase(Locale.ROOT).trim();
            String element = yaml.getString(raw);
            if (!StatusLoader.KNOWN_ENTITY_TYPES.contains(type)) {
                log.warning("Refusing row '" + raw + "' in " + file.getName() + ": no entity type has that key"
                        + " (a bare key like enderman). The rest of the table loads.");
                continue;
            }
            if (element == null || !elementExists.test(element)) {
                log.warning("Refusing row '" + raw + "' in " + file.getName() + ": element '" + element
                        + "' names no element in content/elements. The rest of the table loads.");
                continue;
            }
            out.put(type, element);
        }
        return out;
    }
}
