package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.weapon.SmithingUpgrades;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * {@code content/smithing.yml}: the upgrades the smithing table makes of minted weapons (the seat's brief, part g; core's
 * {@link SmithingUpgrades}). One {@code Loaded smithing} line names every upgrade. A bad row gets a {@code Refusing smithing
 * upgrade <n>} line naming why, and the rest load. A minted weapon with no upgrade is refused at the table, so a refused
 * row here costs a smith and never an item.
 */
public final class SmithingLoader {

    private final Logger log;
    private final Predicate<String> weaponExists;
    private final Predicate<String> materialExists;

    public SmithingLoader(Logger log, Predicate<String> weaponExists, Predicate<String> materialExists) {
        this.log = log;
        this.weaponExists = weaponExists;
        this.materialExists = materialExists;
    }

    public SmithingUpgrades load(File file) {
        if (!file.isFile()) {
            log.warning("Refusing smithing: content/smithing.yml is absent, so every minted weapon is refused at the"
                    + " smithing table and no upgrade is made.");
            return new SmithingUpgrades(List.of());
        }
        List<Map<?, ?>> rows = YamlConfiguration.loadConfiguration(file).getMapList("upgrades");
        List<SmithingUpgrades.Upgrade> kept = new ArrayList<>();
        int n = 0;
        for (Map<?, ?> row : rows) {
            n++;
            String base = str(row.get("base"));
            String template = str(row.get("template"));
            String addition = str(row.get("addition"));
            String result = str(row.get("result"));
            String why;
            if (base == null || template == null || addition == null || result == null) {
                why = "it needs base, template, addition and result";
            } else if (!weaponExists.test(base)) {
                why = "base '" + base + "' is not a weapon";
            } else if (!weaponExists.test(result)) {
                why = "result '" + result + "' is not a weapon";
            } else if (!materialExists.test(template)) {
                why = "template '" + template + "' is not a material";
            } else if (!materialExists.test(addition)) {
                why = "addition '" + addition + "' is not a material";
            } else {
                SmithingUpgrades.Upgrade u = new SmithingUpgrades.Upgrade(base, template, addition, result);
                boolean clash = kept.stream().anyMatch(k -> k.base().equals(u.base()) && k.template().equals(u.template())
                        && k.addition().equals(u.addition()));
                if (!clash) {
                    kept.add(u);
                    continue;
                }
                why = "it clashes with an earlier upgrade for the same base, template and addition";
            }
            log.warning("Refusing smithing upgrade " + n + ": " + why + ". The rest load.");
        }
        SmithingUpgrades upgrades = new SmithingUpgrades(kept);
        StringBuilder names = new StringBuilder();
        for (SmithingUpgrades.Upgrade u : upgrades.all()) {
            if (!names.isEmpty()) names.append(", ");
            names.append(u.base()).append(" + ").append(u.template()).append(" + ").append(u.addition())
                    .append(" -> ").append(u.result());
        }
        log.info("Loaded smithing: " + upgrades.size() + " upgrades [" + names + "]");
        return upgrades;
    }

    private static String str(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString().trim();
    }
}
