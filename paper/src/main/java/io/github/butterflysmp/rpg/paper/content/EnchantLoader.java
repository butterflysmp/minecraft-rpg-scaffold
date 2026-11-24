package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.accessory.AccessoryRefusals;
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
    /** U1: whether an element id names a LOADED element. An enchant's {@code element:} that does not is refused. */
    private final java.util.function.Predicate<String> elementExists;
    /** Whether this is a dev server: only then does a {@code dev_} enchant load (the dev-only example, 2026-11-23). */
    private final boolean dev;

    /**
     * @param elementExists whether an element id is loaded (elements load first, in {@code RpgPlugin})
     * @param dev           {@code SelfTestGuard.DEV_PROPERTY}, the one dev-server marker the loot's dev sources also read
     */
    public EnchantLoader(Logger log, java.util.function.Predicate<String> elementExists, boolean dev) {
        this(log, elementExists, id -> java.util.Optional.empty(), dev);
    }

    /**
     * W1 (2026-11-23): the class of a LOADED WEAPON by id, empty for an id that is not one. An enchant's {@code weapon:}
     * that does not name a loaded weapon is refused, and so is one whose {@code class:} could never match that weapon's.
     */
    private final java.util.function.Function<String, java.util.Optional<GearClass>> weaponClass;

    /**
     * @param weaponClass a loaded weapon's class by id, empty when the id is not a loaded weapon (weapons load first, in
     *                    {@code RpgPlugin}, since the columns were re-cut)
     */
    public EnchantLoader(Logger log, java.util.function.Predicate<String> elementExists,
                         java.util.function.Function<String, java.util.Optional<GearClass>> weaponClass, boolean dev) {
        this.log = log;
        this.elementExists = elementExists;
        this.weaponClass = weaponClass;
        this.dev = dev;
    }

    /**
     * Not a dev server, and NO element is known, so a file declaring {@code element:} is refused. The shape every test
     * that loads the NORMAL roster uses; {@code RpgPlugin} uses the three-argument form.
     */
    public EnchantLoader(Logger log) {
        this(log, id -> false, false);
    }

    /**
     * WHAT EVERY DEV-ONLY ENCHANT'S ID STARTS WITH (the seat's brief, 2026-11-23, "in the manner of loot/cascade.yml's
     * dev_sources"). A file named {@code dev_*.yml} loads ONLY on a dev server; on any other server it is not registered
     * at all, so it cannot roll, be granted, or be named. The prefix IS the marker: no real enchant may start with it,
     * because any file that does is dev-only by construction.
     */
    static final String DEV_PREFIX = "dev_";

    public EnchantRegistry loadAll(File enchantsDir) {
        EnchantRegistry registry = new EnchantRegistry();
        File[] files = enchantsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        java.util.List<String> devOnly = new java.util.ArrayList<>();
        for (File f : files) {
            if (idOf(f).startsWith(DEV_PREFIX) && !dev) {
                devOnly.add(idOf(f));
                continue;
            }
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
        // INFO, not a warning, and not the word the R0c scan searches for: on a real server this is the expected case.
        if (!devOnly.isEmpty()) {
            log.info("Dev-only enchants not loaded (not a dev server): " + devOnly);
        }
        return registry;
    }

    /** The id is the filename: unbreaking.yml -> unbreaking. */
    private static String idOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    private EnchantDefinition parse(String id, ConfigurationSection s) {
        GearClass gearClass = gearClass(s.getString("class"), id);
        String element = element(s.getString("element"), id);
        String weapon = weapon(s.getString("weapon"), id, gearClass);
        // Both element: and weapon: is refused by the RECORD (EnchantDefinition), by name: one place owns that rule.
        return new EnchantDefinition(id, s.getString("display_name", id), s.getInt("max_level", 1),
                effect(s.getString("effect"), id),
                gearClass,
                // getIntegerList returns an EMPTY list for an absent key, never null. A durability
                // enchant therefore lands on the empty list the record requires of it, and a damage
                // enchant with the key forgotten is refused by the record rather than here -- one
                // place owns the schema rules, and it is the record.
                s.getIntegerList("value_by_level"),
                // Cosmetic, so it DEFAULTS rather than throwing -- a display typo must not drop a
                // working enchant, which is what skipping the file would do. A name that does not
                // resolve to a Material is caught at boot by ContentValidator, which can reach the
                // Bukkit registry that this loader deliberately cannot.
                s.getString("icon", EnchantDefinition.DEFAULT_ICON),
                requires(s.getString("requires"), id),
                element, weapon);
    }

    /**
     * W1: absent is not weapon-specific; present makes the enchant WEAPON-SPECIFIC to that one weapon, and must name a
     * LOADED WEAPON (not a shield, armour piece, tool or accessory: only a weapon has a weapon slot worth filling). Refused
     * by name otherwise, and refused when the enchant's {@code class:} names a class other than that weapon's, because
     * such an enchant could never roll (the weapon pool applies the class filter, as the unique pool does).
     */
    private String weapon(String raw, String id, GearClass gearClass) {
        if (raw == null) return null;
        String weapon = raw.trim().toLowerCase(java.util.Locale.ROOT);
        java.util.Optional<GearClass> cls = weapon.isEmpty() ? java.util.Optional.empty() : weaponClass.apply(weapon);
        if (cls.isEmpty()) {
            throw new IllegalArgumentException("enchant '" + id + "' declares weapon: '" + raw
                    + "', which is not a loaded weapon");
        }
        if (gearClass != null && gearClass != cls.get()) {
            throw new IllegalArgumentException("enchant '" + id + "' declares weapon: " + weapon + " (class "
                    + cls.get().name().toLowerCase(java.util.Locale.ROOT) + ") and class: "
                    + gearClass.name().toLowerCase(java.util.Locale.ROOT) + "; it could never roll");
        }
        return weapon;
    }

    /**
     * U1: absent is a NORMAL enchant; present makes it UNIQUE to gear of that element, and must name a LOADED element.
     * Refused by name otherwise: a misspelt element would make a unique enchant that no gear can ever roll, silently.
     */
    private String element(String raw, String id) {
        if (raw == null) return null;
        String element = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (element.isEmpty() || !elementExists.test(element)) {
            throw new IllegalArgumentException("enchant '" + id + "' declares element: '" + raw
                    + "', which is not a loaded element");
        }
        return element;
    }

    /**
     * THE QUANTITY THE ENCHANT NEEDS THE HELD WEAPON TO HAVE (the seat's ruling H, 2026-11-22): absent is none;
     * {@code reload} (reload_ticks above 0) or {@code quiver} (quiver_size above 0). A MECHANICAL GATE, so an unknown value
     * is REFUSED by name, never defaulted: a misspelt requirement that fell back to none would roll the enchant onto the
     * weapons it was written to keep it off.
     */
    static io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Requires requires(String raw, String id) {
        if (raw == null) return io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Requires.NONE;
        return switch (raw.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "reload" -> io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Requires.RELOAD;
            case "quiver" -> io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Requires.QUIVER;
            default -> throw new IllegalArgumentException("enchant '" + id + "' declares requires: '" + raw
                    + "'; it must be reload or quiver, or absent");
        };
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
        // Ruling A4, EXPLICIT. GearClass.ACCESSORY exists so exhaustive switches must answer for
        // accessories, and fromName therefore parses `class: accessory`. Every effect's gate in
        // EnchantDefinition is an allowlist and would refuse it anyway -- in a gate's words, for a
        // gate's reason. This says the true one: accessories take no part in enchanting.
        // EnchantLoaderTest asserts THIS text, which is the only reading deleting this line reddens.
        if (parsed == GearClass.ACCESSORY) {
            throw new IllegalArgumentException("enchant '" + id + "': " + AccessoryRefusals.ENCHANT_CLASS);
        }
        return parsed;
    }

    /** The content token for "no class gate". Not a {@link GearClass} constant, by design. */
    private static final String UNIVERSAL = "universal";
}
