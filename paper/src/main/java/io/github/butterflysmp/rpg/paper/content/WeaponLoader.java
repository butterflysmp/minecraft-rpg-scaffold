package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Turns YAML into WeaponDefinition. The only class that knows the weapon schema.
 *
 * A weapon is a container of triggers, and each trigger is an ability body (cast /
 * cost / cooldown / on_hit) bound to an input. So each trigger reuses AbilitySchema
 * verbatim -- the same parser abilities use -- and the trigger's ability id is
 * synthesized as weaponId + "/" + input, which is what keys its cooldown so a
 * weapon's triggers cooldown independently.
 *
 * element and rarity are inert reserved data in Phase 1; they load and later color
 * the item name. element defaults to kinetic (the neutral element, never absent);
 * rarity defaults to common.
 *
 * A weapon's id is its filename minus .yml, as with the other content types. Fails
 * soft: a malformed file is logged, named, and skipped, and a weapon with no valid
 * triggers is malformed (WeaponDefinition rejects it), so it never quietly ships a
 * weapon that does nothing.
 */
public final class WeaponLoader {

    private final Logger log;

    public WeaponLoader(Logger log) {
        this.log = log;
    }

    public WeaponRegistry loadAll(File weaponsDir) {
        WeaponRegistry registry = new WeaponRegistry();
        File[] files = weaponsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(idOf(f), yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed weapon '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " weapon file(s) were skipped. The server is still running, "
                    + "but that weapon is not loaded.");
        }
        return registry;
    }

    /** The id is the filename: ironblade.yml -> ironblade. */
    private static String idOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    private WeaponDefinition parse(String id, ConfigurationSection s) {
        String displayName = s.getString("display_name", id);
        String element = s.getString("element", "kinetic");
        Rarity rarity = rarity(s.getString("rarity", "common"));
        // The item the weapon renders as; paper resolves the string to a Material. Defaults
        // to a sword, so every weapon before the bow needs no material field.
        String material = s.getString("material", WeaponDefinition.DEFAULT_MATERIAL);

        ConfigurationSection triggers = s.getConfigurationSection("triggers");
        if (triggers == null) {
            throw new IllegalArgumentException("weapon '" + id + "' has no 'triggers' section");
        }

        List<TriggerBinding> bindings = new ArrayList<>();
        for (String input : triggers.getKeys(false)) {
            ConfigurationSection t = triggers.getConfigurationSection(input);
            if (t == null) {
                throw new IllegalArgumentException(
                        "trigger '" + input + "' in weapon '" + id + "' must be a section");
            }
            // A trigger IS an ability body plus an input. Identity fields come from the
            // weapon; cast/cost/cooldown/effects parse through the shared AbilitySchema.
            AbilityDefinition ability = new AbilityDefinition(
                    id + "/" + input,
                    displayName,
                    element,
                    "none",
                    t.getInt("cooldown_ticks", 0),
                    AbilitySchema.parseCost(t.getConfigurationSection("cost")),
                    AbilitySchema.parseCast(t.getConfigurationSection("cast")),
                    AbilitySchema.parseEffects(t.getMapList("on_hit")));
            bindings.add(new TriggerBinding(input, ability));
        }

        // WeaponDefinition rejects an empty trigger list -- caught above, named, skipped.
        return new WeaponDefinition(id, displayName, element, rarity, material, bindings);
    }

    private static Rarity rarity(String raw) {
        Rarity parsed = Rarity.fromName(raw);
        if (parsed == null) {
            throw new IllegalArgumentException(
                    "Unknown rarity '" + raw + "'; expected one of " + Arrays.toString(Rarity.values()));
        }
        return parsed;
    }
}
