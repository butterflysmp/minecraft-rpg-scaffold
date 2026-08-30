package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Turns YAML into EnchantDefinition. The only class that knows the enchant schema: an id, a display
 * name, a maximum level, the MECHANISM it binds to, its class gate, and -- for a damage enchant --
 * its authored curve.
 *
 * <p>The {@code effect} and {@code class} keys are the behaviour fields Pass 1 deferred to Pass 2.
 * The rule they were deferred to protect is intact: content names a mechanism and parameterises it,
 * and still cannot define one. See {@link EnchantDefinition} for why a damage enchant's percentages
 * are authored while Unbreaking's curve is Java.
 *
 * An enchant's id is its filename minus .yml, as with the other content types. Fails soft: a
 * malformed file is logged, named, and skipped, and every other enchant still loads.
 *
 * <p>The ROSTER pass extends this additively -- a pool or weight field, and more files -- rather
 * than replacing it. That is the whole reason identity is content in Pass 1 despite the roster
 * being deferred: an in-Java identity would have cost that pass a migration through this one's code.
 */
public final class EnchantLoader {

    private final Logger log;

    public EnchantLoader(Logger log) {
        this.log = log;
    }

    public EnchantRegistry loadAll(File enchantsDir) {
        EnchantRegistry registry = new EnchantRegistry();
        File[] files = enchantsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(idOf(f), yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed enchant '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " enchant file(s) were skipped. The server is still running, "
                    + "but that enchant will render as its raw id and cannot be granted.");
        }
        return registry;
    }

    /** The id is the filename: unbreaking.yml -> unbreaking. */
    private static String idOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    private EnchantDefinition parse(String id, ConfigurationSection s) {
        return new EnchantDefinition(id, s.getString("display_name", id), s.getInt("max_level", 1),
                effect(s.getString("effect"), id),
                gearClass(s.getString("class"), id),
                // getIntegerList returns an EMPTY list for an absent key, never null. A durability
                // enchant therefore lands on the empty list the record requires of it, and a damage
                // enchant with the key forgotten is refused by the record rather than here -- one
                // place owns the schema rules, and it is the record.
                s.getIntegerList("value_by_level"),
                // Cosmetic, so it DEFAULTS rather than throwing -- a display typo must not drop a
                // working enchant, which is what skipping the file would do. A name that does not
                // resolve to a Material is caught at boot by ContentValidator, which can reach the
                // Bukkit registry that this loader deliberately cannot.
                s.getString("icon", EnchantDefinition.DEFAULT_ICON));
    }

    /**
     * The mechanism this enchant binds to. REQUIRED, and never defaulted.
     *
     * A default would have to be {@code durability}, which would silently turn a damage enchant
     * whose {@code effect:} line was misspelled into an Unbreaking that grants no damage and skips
     * wear instead -- working, wrong, and invisible. The same reasoning makes {@code class} required
     * on a weapon.
     */
    private static EnchantEffect effect(String raw, String id) {
        if (raw == null) {
            throw new IllegalArgumentException("enchant '" + id + "' is missing required 'effect'"
                    + " (one of " + Arrays.toString(EnchantEffect.values()) + ")");
        }
        EnchantEffect parsed = EnchantEffect.fromName(raw);
        if (parsed == null) {
            throw new IllegalArgumentException("Unknown effect '" + raw + "' in enchant '" + id
                    + "'; expected one of " + Arrays.toString(EnchantEffect.values()));
        }
        return parsed;
    }

    /**
     * The class gate. REQUIRED, and {@code universal} is spelled out rather than being what you get
     * by leaving the line off -- an absent gate on a damage enchant is exactly the mistake that
     * would make Sharpness boost every weapon in the game, and it must not be reachable by
     * forgetting something.
     *
     * <p>Returns null for {@code universal}, which is the no-gate value {@code DamageEnchants}
     * expects. Every other name goes through {@link GearClass#fromName}, so the enchant and the
     * weapon it sits on are parsed by ONE function and cannot disagree about what "ranger" means.
     * Note the token is {@code ranger}, matching the enum and every weapon yml; "Ranged" is only the
     * tooltip's label for it.
     */
    private static GearClass gearClass(String raw, String id) {
        if (raw == null) {
            throw new IllegalArgumentException("enchant '" + id + "' is missing required 'class'"
                    + " (universal, or one of " + Arrays.toString(GearClass.values()) + ")");
        }
        if (UNIVERSAL.equalsIgnoreCase(raw)) return null;
        GearClass parsed = GearClass.fromName(raw);
        if (parsed == null) {
            throw new IllegalArgumentException("Unknown class '" + raw + "' in enchant '" + id
                    + "'; expected " + UNIVERSAL + " or one of "
                    + Arrays.toString(GearClass.values()));
        }
        return parsed;
    }

    /** The content token for "no class gate". Not a {@link GearClass} constant, by design. */
    private static final String UNIVERSAL = "universal";
}
