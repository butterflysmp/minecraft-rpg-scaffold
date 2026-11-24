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
     * D1 (2026-11-23): a loaded weapon's whole definition by id, so a granted trigger can be checked against the weapon's
     * own gestures and quiver. Empty from the class-only constructor, which therefore refuses every grant_trigger by name.
     */
    private java.util.function.Function<String, java.util.Optional<io.github.butterflysmp.rpg.core.weapon.WeaponDefinition>>
            weaponDefinition = id -> java.util.Optional.empty();

    /**
     * THE LOADER THE SERVER USES (since D1): elements, and the WEAPONS themselves, from which each weapon's class is
     * read ({@code GearClass.of}) for W1 and its gestures and quiver for a granted trigger.
     */
    public static EnchantLoader withWeapons(Logger log, java.util.function.Predicate<String> elementExists,
            java.util.function.Function<String, java.util.Optional<io.github.butterflysmp.rpg.core.weapon.WeaponDefinition>> weapons,
            boolean dev) {
        EnchantLoader loader = new EnchantLoader(log, elementExists,
                id -> weapons.apply(id).map(w -> GearClass.of(w.weaponClass())), dev);
        loader.weaponDefinition = weapons;
        return loader;
    }

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
        java.util.List<EnchantDefinition> parsed = new java.util.ArrayList<>();
        for (File f : files) {
            if (idOf(f).startsWith(DEV_PREFIX) && !dev) {
                devOnly.add(idOf(f));
                continue;
            }
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                parsed.add(parse(idOf(f), yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed enchant '" + f.getName() + "': " + ex.getMessage());
            }
        }
        // G1 (2026-11-24): more guaranteed enchants for ONE weapon than the slot can show (MAX_CANDIDATES) is refused, by
        // name, for every one of them: a slot that guaranteed five could show only four, and which four is no rule anyone
        // wrote. A cross-file rule, so it is checked here, after every file is parsed and before any is registered.
        java.util.Map<String, java.util.List<String>> guaranteedByWeapon = new java.util.TreeMap<>();
        for (EnchantDefinition def : parsed) {
            if (def.guaranteed()) guaranteedByWeapon.computeIfAbsent(def.weapon(), w -> new java.util.ArrayList<>()).add(def.id());
        }
        for (EnchantDefinition def : parsed) {
            java.util.List<String> same = def.guaranteed() ? guaranteedByWeapon.get(def.weapon()) : null;
            if (same != null && same.size() > io.github.butterflysmp.rpg.core.enchant.EnchantRoll.MAX_CANDIDATES) {
                skipped++;
                log.warning("Skipping malformed enchant '" + def.id() + ".yml': weapon '" + def.weapon() + "' has "
                        + same.size() + " guaranteed enchants " + same + "; its slot shows at most "
                        + io.github.butterflysmp.rpg.core.enchant.EnchantRoll.MAX_CANDIDATES);
                continue;
            }
            registry.register(def);
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
                element, weapon, behaviour(s, id, weapon), guaranteed(s, id));
    }

    /**
     * G1 (2026-11-24): {@code guaranteed: true} or absent (false). A MECHANICAL key, so anything but a YAML boolean is
     * refused by name: {@code guaranteed: yes-please} must not quietly read as false. The record refuses it on an enchant
     * that names no weapon.
     */
    private static boolean guaranteed(ConfigurationSection s, String id) {
        if (!s.contains("guaranteed")) return false;
        if (!s.isBoolean("guaranteed")) {
            throw new IllegalArgumentException("enchant '" + id + "' declares guaranteed: '" + s.get("guaranteed")
                    + "'; it must be true or false");
        }
        return s.getBoolean("guaranteed");
    }

    /**
     * THE BEHAVIOUR BLOCK (the seat's B1, 2026-11-23), read for the effect that names it: today {@code shockwave:}. The
     * record ({@code EnchantDefinition.requireBehaviour}) refuses a behavioural effect without its block, a stat effect
     * with one, and levels that disagree with {@code max_level}; this method refuses a block whose own keys are missing
     * or malformed, BY THE KEY'S NAME (a misspelt key must never fall back to a default and act at a number nobody
     * wrote). The block's shape:
     *
     * <pre>
     * shockwave:
     *   hits: 3                       # hits on ONE target within the window
     *   window_ticks: 60
     *   cooldown_ticks: 5             # per player
     *   damage_by_level: [10, 15, 20] # one per level
     *   radius_by_level: [3, 4, 5]    # blocks, one per level
     *   element: kinetic              # the damage's element
     *   visual: tremor_wave           # optional: presented once at the origin
     * </pre>
     */
    private io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour behaviour(ConfigurationSection s, String id, String weapon) {
        java.util.List<String> present = new java.util.ArrayList<>();
        for (String key : BEHAVIOUR_BLOCKS) if (s.contains(key)) present.add(key);
        if (present.isEmpty()) return null;
        if (present.size() > 1) {
            throw new IllegalArgumentException("enchant '" + id + "' declares " + present + "; one behaviour block at most");
        }
        String name = present.get(0);
        ConfigurationSection b = s.getConfigurationSection(name);
        if (b == null) throw new IllegalArgumentException("enchant '" + id + "': " + name + ": must be a block of keys");
        if (name.equals("grant_trigger")) return grantTrigger(b, id, weapon);
        if (name.equals("cast_tune")) return castTune(b, id, weapon);
        String element = requiredString(b, "element", id);
        if (!elementExists.test(element)) {
            throw new IllegalArgumentException("enchant '" + id + "' " + name + ": element '" + element
                    + "' is not a loaded element");
        }
        try {
            return switch (name) {
                case "shockwave" -> new io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.Shockwave(
                        requiredInt(b, "hits", id), requiredInt(b, "window_ticks", id), requiredInt(b, "cooldown_ticks", id),
                        requiredDoubles(b, "damage_by_level", id), requiredDoubles(b, "radius_by_level", id),
                        element, b.getString("visual"));
                // airburst: travel_blocks, count, angle_degrees, speed, burst_travel_blocks, damage_by_level, element, and
                // an OPTIONAL homing block for the bursts (Ben, 2026-11-23: "seek as well"): lerp, activation_blocks,
                // search_radius, all three required when it is present (as a projectile's, AbilitySchema.parseHoming),
                // judged by CastSpec.Homing. Absent: straight bursts, as before.
                case "airburst" -> new io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.Airburst(
                        requiredDouble(b, "travel_blocks", id), requiredInt(b, "count", id),
                        requiredDouble(b, "angle_degrees", id), requiredDouble(b, "speed", id),
                        requiredDouble(b, "burst_travel_blocks", id), requiredDoubles(b, "damage_by_level", id), element,
                        burstHoming(b.getConfigurationSection("homing"), id));
                // shatter (Shatter Caps, 2026-11-24): travel_blocks, speed, item (optional: the stone's look),
                // damage_by_level, element. Its weapon is the enchant's own weapon:, whose right click must be a
                // projectile WITH a spread (the stone replaces the spread, and the split relaunches it).
                case "shatter" -> new io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.Shatter(
                        shatterWeapon(id, weapon), requiredDouble(b, "travel_blocks", id), requiredDouble(b, "speed", id),
                        b.getString("item"), requiredDoubles(b, "damage_by_level", id), element);
                // focused_beam (Focused Beam, 2026-11-24): range, damage_by_level, element. Its weapon is the enchant's
                // own weapon:, whose right click must be a VOLLEY OF RAYS (the beam replaces the volley's shots with one of
                // its own ray, read from the weapon at run time).
                case "focused_beam" -> new io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.FocusedBeam(
                        focusedBeamWeapon(id, weapon), requiredDouble(b, "range", id),
                        requiredDoubles(b, "damage_by_level", id), element);
                default -> throw new IllegalStateException("unreachable: " + name);
            };
        } catch (IllegalArgumentException ex) {
            throw ex.getMessage().startsWith("enchant '") ? ex
                    : new IllegalArgumentException("enchant '" + id + "' " + name + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * THE GRANTED TRIGGER (Lapis Crystal's D1, 2026-11-23). The block is an {@code input} and an {@code ability:} in the
     * SAME grammar a weapon's trigger uses ({@code AbilitySchema}): name, description, cooldown_ticks, cost, on_cast, cast,
     * on_hit. Its id is {@code <enchant id>/<input>} (its own cooldown); its element is the weapon's. Refused by name:
     * an enchant that is not weapon-specific; a weapon this loader cannot see; an input that is not left_click or
     * right_click; a weapon that already binds that gesture; a weapon with a QUIVER (left click is its reload, and its
     * right click its shot).
     *
     * <pre>
     * grant_trigger:
     *   input: left_click
     *   ability:
     *     name: "Lapis Crystal"
     *     cooldown_ticks: 600
     *     cost: { resource: mana, amount: 30 }
     *     cast: { type: ... }
     * </pre>
     */
    private io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour grantTrigger(ConfigurationSection b, String id,
                                                                                   String weaponId) {
        if (weaponId == null) {
            throw new IllegalArgumentException("enchant '" + id + "' grant_trigger: is granted to one weapon, so the"
                    + " enchant must declare weapon: <id>");
        }
        io.github.butterflysmp.rpg.core.weapon.WeaponDefinition weapon = weaponDefinition.apply(weaponId).orElseThrow(() ->
                new IllegalArgumentException("enchant '" + id + "' grant_trigger: weapon '" + weaponId
                        + "' is not a weapon this loader can see, so its gestures cannot be checked"));
        String input = requiredString(b, "input", id);
        if (!GRANTABLE_INPUTS.contains(input)) {
            throw new IllegalArgumentException("enchant '" + id + "' grant_trigger: input '" + input + "' must be one of "
                    + GRANTABLE_INPUTS);
        }
        if (weapon.trigger(input).isPresent()) {
            throw new IllegalArgumentException("enchant '" + id + "' grant_trigger: weapon '" + weaponId
                    + "' already binds " + input);
        }
        if (weapon.hasQuiver()) {
            throw new IllegalArgumentException("enchant '" + id + "' grant_trigger: weapon '" + weaponId
                    + "' has a quiver (left click is its reload), so no trigger may be granted to it");
        }
        ConfigurationSection a = b.getConfigurationSection("ability");
        if (a == null) {
            throw new IllegalArgumentException("enchant '" + id + "' grant_trigger: is missing its ability: block");
        }
        if (!a.isInt("cooldown_ticks")) {
            throw new IllegalArgumentException("enchant '" + id + "' grant_trigger: ability is missing a whole-number"
                    + " 'cooldown_ticks'");
        }
        io.github.butterflysmp.rpg.core.ability.AbilityDefinition ability = new io.github.butterflysmp.rpg.core.ability.AbilityDefinition(
                id + "/" + input,
                a.getString("name", id),
                weapon.element(),
                a.getInt("cooldown_ticks"),
                AbilitySchema.parseCost(a.getConfigurationSection("cost")),
                AbilitySchema.parseCast(a.getConfigurationSection("cast")),
                AbilitySchema.parseEffects(a.getMapList("on_hit")),
                a.getStringList("description"),
                AbilitySchema.parseCastVisuals(a.getMapList("on_cast")));
        // D3: an orb's shot must name a LOADED element, by name, as every behaviour's element must.
        if (ability.cast() instanceof io.github.butterflysmp.rpg.core.ability.CastSpec.Orb orb && orb.shot() != null
                && !elementExists.test(orb.shot().element())) {
            throw new IllegalArgumentException("enchant '" + id + "' grant_trigger: the orb's shot element '"
                    + orb.shot().element() + "' is not a loaded element");
        }
        return new io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.GrantTrigger(weaponId, input, ability);
    }

    /**
     * THE CAST TUNE (the seat's G4, 2026-11-24): the one seam for a numeric change to the weapon's OWN right click. The
     * block:
     *
     * <pre>
     * cast_tune:
     *   quantity: damage              # damage | range | shots | windup
     *   value_by_level: [4, 8, 12]    # one per level; damage and range ADD, shots SETS, windup SUBTRACTS (ticks)
     * </pre>
     *
     * Refused BY NAME (ruling H's spirit: a tune must never advertise a number the cast cannot receive): an enchant that
     * is not weapon-specific; a weapon this loader cannot see; a weapon with no right click; a quantity that is not one of
     * the four; a quantity the right click does not HAVE (damage: no literal damage in its on_hit; range: neither a ray nor
     * a volley of rays; shots and windup: not a volley); a value the record refuses (a shot count below 1, a fraction of a
     * tick); and any level that would make the cast itself invalid, found by BUILDING the tuned cast for every level here
     * (a wind-up below 0, a warning flash at or after the shortened wind-up: {@code CastSpec.Volley}'s own refusals). The
     * level count against {@code max_level} is the record's.
     */
    private io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour castTune(ConfigurationSection b, String id,
                                                                               String weaponId) {
        if (weaponId == null) {
            throw new IllegalArgumentException("enchant '" + id + "' cast_tune: tunes one weapon's cast, so the enchant"
                    + " must declare weapon: <id>");
        }
        io.github.butterflysmp.rpg.core.weapon.WeaponDefinition weapon = weaponDefinition.apply(weaponId).orElseThrow(() ->
                new IllegalArgumentException("enchant '" + id + "' cast_tune: weapon '" + weaponId
                        + "' is not a weapon this loader can see, so its cast cannot be checked"));
        io.github.butterflysmp.rpg.core.ability.AbilityDefinition cast = weapon.trigger("right_click")
                .map(t -> t.ability())
                .orElseThrow(() -> new IllegalArgumentException("enchant '" + id + "' cast_tune: weapon '" + weaponId
                        + "' has no right click to tune"));
        String raw = requiredString(b, "quantity", id);
        io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.CastTune.Quantity quantity;
        try {
            quantity = io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.CastTune.Quantity.valueOf(
                    raw.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("enchant '" + id + "' cast_tune: quantity '" + raw
                    + "' must be one of damage, range, shots, windup");
        }
        io.github.butterflysmp.rpg.core.ability.CastSpec spec = cast.cast();
        boolean has = switch (quantity) {
            case DAMAGE -> cast.onHit().stream()
                    .anyMatch(e -> e instanceof io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Damage);
            case RANGE -> spec instanceof io.github.butterflysmp.rpg.core.ability.CastSpec.Ray
                    || (spec instanceof io.github.butterflysmp.rpg.core.ability.CastSpec.Volley v
                        && v.of() instanceof io.github.butterflysmp.rpg.core.ability.CastSpec.Ray);
            case SHOTS, WINDUP -> spec instanceof io.github.butterflysmp.rpg.core.ability.CastSpec.Volley;
        };
        if (!has) {
            throw new IllegalArgumentException("enchant '" + id + "' cast_tune: weapon '" + weaponId + "''s right click has"
                    + " no " + raw + " to tune");
        }
        io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.CastTune tune;
        try {
            tune = new io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.CastTune(weaponId, quantity,
                    requiredDoubles(b, "value_by_level", id));
            for (int level = 1; level <= tune.levels(); level++) {
                io.github.butterflysmp.rpg.core.ability.CastTunes.tune(cast, java.util.List.of(
                        new io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour(id, level, tune)));
            }
        } catch (IllegalArgumentException ex) {
            throw ex.getMessage().startsWith("enchant '") ? ex
                    : new IllegalArgumentException("enchant '" + id + "' cast_tune: " + ex.getMessage(), ex);
        }
        return tune;
    }

    /** The gestures an enchant may grant (D1). */
    static final java.util.List<String> GRANTABLE_INPUTS = java.util.List.of("left_click", "right_click");

    /** Every behaviour block a file may declare, one at most (B1). */
    private static final java.util.List<String> BEHAVIOUR_BLOCKS = java.util.List.of("shockwave", "airburst", "shatter",
            "focused_beam", "grant_trigger", "cast_tune");

    /**
     * FOCUSED BEAM'S WEAPON, refused by name (the seat's brief of 2026-11-24): the enchant must be weapon-specific, its
     * weapon one this loader can see, and that weapon's right click a VOLLEY whose inner cast is a RAY: the beam replaces
     * the volley's shots with one of that ray, so a weapon without one has nothing to focus.
     */
    private String focusedBeamWeapon(String id, String weaponId) {
        if (weaponId == null) {
            throw new IllegalArgumentException("enchant '" + id + "' focused_beam: replaces one weapon's volley, so the"
                    + " enchant must declare weapon: <id>");
        }
        io.github.butterflysmp.rpg.core.weapon.WeaponDefinition weapon = weaponDefinition.apply(weaponId).orElseThrow(() ->
                new IllegalArgumentException("enchant '" + id + "' focused_beam: weapon '" + weaponId
                        + "' is not a weapon this loader can see, so its volley cannot be checked"));
        boolean rayVolley = weapon.trigger("right_click")
                .map(t -> t.ability().cast() instanceof io.github.butterflysmp.rpg.core.ability.CastSpec.Volley v
                        && v.of() instanceof io.github.butterflysmp.rpg.core.ability.CastSpec.Ray)
                .orElse(false);
        if (!rayVolley) {
            throw new IllegalArgumentException("enchant '" + id + "' focused_beam: weapon '" + weaponId + "''s right click is"
                    + " not a volley of rays, so there is nothing to focus");
        }
        return weaponId;
    }

    /**
     * SHATTER'S WEAPON, refused by name (the seat's brief of 2026-11-24): the enchant must be weapon-specific, its weapon
     * one this loader can see, and that weapon's right click a projectile WITH a spread: the stone replaces the spread,
     * and the split relaunches the weapon's own cast, so a weapon without one has nothing to shatter into.
     */
    private String shatterWeapon(String id, String weaponId) {
        if (weaponId == null) {
            throw new IllegalArgumentException("enchant '" + id + "' shatter: replaces one weapon's spread, so the enchant"
                    + " must declare weapon: <id>");
        }
        io.github.butterflysmp.rpg.core.weapon.WeaponDefinition weapon = weaponDefinition.apply(weaponId).orElseThrow(() ->
                new IllegalArgumentException("enchant '" + id + "' shatter: weapon '" + weaponId
                        + "' is not a weapon this loader can see, so its shot cannot be checked"));
        boolean spreadShot = weapon.trigger("right_click")
                .map(t -> t.ability().cast() instanceof io.github.butterflysmp.rpg.core.ability.CastSpec.Projectile p
                        && p.spread() != null)
                .orElse(false);
        if (!spreadShot) {
            throw new IllegalArgumentException("enchant '" + id + "' shatter: weapon '" + weaponId + "''s right click is"
                    + " not a projectile with a spread, so there is nothing to shatter into");
        }
        return weaponId;
    }

    /** The airburst's bursts' homing block, or null when the file declares none. */
    private static io.github.butterflysmp.rpg.core.ability.CastSpec.Homing burstHoming(ConfigurationSection h, String id) {
        if (h == null) return null;
        return new io.github.butterflysmp.rpg.core.ability.CastSpec.Homing(requiredDouble(h, "lerp", id),
                requiredDouble(h, "activation_blocks", id), requiredDouble(h, "search_radius", id));
    }

    private static int requiredInt(ConfigurationSection b, String key, String id) {
        if (!b.isInt(key)) {
            throw new IllegalArgumentException("enchant '" + id + "' " + b.getName() + ": is missing a whole-number '"
                    + key + "'");
        }
        return b.getInt(key);
    }

    private static double requiredDouble(ConfigurationSection b, String key, String id) {
        if (!(b.get(key) instanceof Number n)) {
            throw new IllegalArgumentException("enchant '" + id + "' " + b.getName() + ": is missing the number '"
                    + key + "'");
        }
        return n.doubleValue();
    }

    private static java.util.List<Double> requiredDoubles(ConfigurationSection b, String key, String id) {
        if (!b.isList(key)) {
            throw new IllegalArgumentException("enchant '" + id + "' " + b.getName() + ": is missing the list '" + key
                    + "' (one value per level)");
        }
        java.util.List<Double> out = new java.util.ArrayList<>();
        for (Object o : b.getList(key)) {
            if (!(o instanceof Number n)) {
                throw new IllegalArgumentException("enchant '" + id + "' " + b.getName() + ": '" + key + "' holds '" + o
                        + "', which is not a number");
            }
            out.add(n.doubleValue());
        }
        return out;
    }

    private static String requiredString(ConfigurationSection b, String key, String id) {
        String v = b.getString(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("enchant '" + id + "' " + b.getName() + ": is missing '" + key + "'");
        }
        return v.trim().toLowerCase(java.util.Locale.ROOT);
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
