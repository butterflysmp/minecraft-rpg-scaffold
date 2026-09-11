package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

    /**
     * Every key a weapon file may author, which is every key {@link #parse} reads, plus {@code id}.
     *
     * <p><b>{@code id} is in the set and is NOT read.</b> The id comes from the filename
     * ({@code ironblade.yml -> ironblade}); every shipped file also writes {@code id:} redundantly,
     * and without it here the loader would warn about all of them.
     *
     * <h2>Why an unknown key is worth warning about at all</h2>
     *
     * <p>Because it is read by NOBODY, a misspelled key cannot be caught by any of the value guards
     * in {@code WeaponDefinition}: {@code s.getInt("quivver_size", NO_QUIVER)} returns the default,
     * the weapon loads perfectly cleanly, and the only symptom is a crossbow that turns out to have
     * no magazine. Every other guard in this pipeline validates a value that WAS read, so this class
     * of typo had nothing looking for it.
     *
     * <p><b>THIS IS TODAY'S SET, AND IT IS HAND-MAINTAINED -- the same property
     * {@code DamageSignatureTest} writes down about its own scope.</b> A new field added to
     * {@link #parse} and forgotten here produces a spurious warning about a legitimate key. That is
     * the SAFE direction of drift -- loud, visible on the next boot, and self-correcting -- rather
     * than a real typo going unreported, so the maintenance burden fails towards noise and not
     * towards silence.
     *
     * <p><b>Scope, stated because the omission is otherwise indistinguishable from an oversight:</b>
     * weapons only. Armor, shields, tools, enchants, abilities and mobs have the same hazard and no
     * such check; a general strict-key facility across every loader is a separate pass, and this is
     * the foothold rather than the finished job. Weapons go first because the quiver is the first
     * field in this repository where a typo yields a weapon that is SILENTLY WRONG rather than
     * visibly broken.
     */
    private static final java.util.Set<String> KNOWN_KEYS = java.util.Set.of(
            "id", "display_name", "element", "rarity", "class", "material",
            "attack_damage", "attack_speed", "sweep", "quiver_size", "reload_ticks",
            "flavor", "triggers", "craft_result");

    private WeaponDefinition parse(String id, ConfigurationSection s) {
        // WARN, never skip. An unknown key is a typo in a file whose other fields are fine, and
        // refusing the whole weapon over one misspelling would be a worse trade than loading it and
        // saying so -- the same call the scalar-`flavor:` warning below makes.
        for (String key : s.getKeys(false)) {
            if (!KNOWN_KEYS.contains(key)) {
                log.warning("weapon '" + id + "' has unknown key '" + key + "'; it is read by nothing"
                        + " and will be ignored. Check the spelling against the weapon schema.");
            }
        }

        String displayName = s.getString("display_name", id);
        String element = s.getString("element", "kinetic");
        Rarity rarity = rarity(s.getString("rarity", "common"));
        // class is a REQUIRED mechanical axis, not defaulted like rarity: a missing or bad value is a
        // named, skipped file, so a forgotten class can never silently ship as some default (future
        // class-typed modifiers key on it -- a wrong default is a silent-correctness bug).
        WeaponClass weaponClass = weaponClass(s.getString("class"), id);
        // The item the weapon renders as; paper resolves the string to a Material. Defaults
        // to a sword, so every weapon before the bow needs no material field.
        String material = s.getString("material", WeaponDefinition.DEFAULT_MATERIAL);
        // The weapon's melee attack damage: the number a basic swing (weapon_damage on_hit) deals,
        // read back off the caster's ATTACK_DAMAGE stat. 0 for ranged/costed weapons with no melee.
        // A negative is rejected by WeaponDefinition -> the file is skipped, named, like any malformed one.
        double attackDamage = s.getDouble("attack_damage", 0.0);
        // The weapon's melee cadence in attacks per second, driving vanilla's attack-strength period
        // directly (every vanilla sword is 1.6). 0 for a ranged/costed weapon with no melee basic.
        // WeaponDefinition rejects a negative, and rejects a MISSING one on a vanilla-driven melee
        // weapon -- the file is skipped and named, like any malformed one.
        double attackSpeed = s.getDouble("attack_speed", 0.0);
        // The sweep fraction: what each bystander caught by vanilla's sweeping swing takes, as a
        // share of the number the primary target took. ABSENT MEANS NO SWEEP, which is deliberate and
        // is what keeps this field from being a migration: an operator's already-edited weapon file
        // simply does not sweep on the next restart, rather than being rejected the way a newly
        // required field would reject it. WeaponDefinition rejects a negative, and rejects a declared
        // sweep on a weapon with no vanilla-driven melee trigger -- the file is skipped and named,
        // like any malformed one.
        double sweep = s.getDouble("sweep", 0.0);
        // THE QUIVER: how many rounds the magazine holds, and how long refilling it takes. Both
        // ABSENT MEANS NO QUIVER, on the same 0-is-absent convention as the three fields above, which
        // is what keeps this from being a migration -- every weapon already shipped simply carries no
        // magazine on the next restart rather than being rejected by a newly required field.
        //
        // WeaponDefinition rejects a negative, rejects each of the pair declared without the other
        // (a reload with nothing to reload, a magazine that could never be refilled), and rejects a
        // quiver weapon that also declares a left_click trigger -- left-click is the reload. The file
        // is skipped and named, like any malformed one.
        //
        // NOTE THE ASYMMETRY WITH EVERY FIELD ABOVE, because it is the one thing a reader will get
        // wrong here: quiver_size is an INT, not a double. The magazine is a count of rounds, and
        // "3.5 arrows" is not a quantity -- so the rounding rule for a PERCENTAGE modifier reaching
        // it lives in core.weapon.Quiver.applyPercent and is not re-expressed at this boundary.
        int quiverSize = s.getInt("quiver_size", WeaponDefinition.NO_QUIVER);
        int reloadTicks = s.getInt("reload_ticks", 0);
        // Authored tooltip prose. Optional; absent -> empty list. MUST be a YAML list: getStringList
        // returns [] for a scalar (flavor: "one line" would vanish silently -- the "finds nothing"
        // trap). So warn, loudly and named, when someone writes it as a scalar, and don't skip the
        // weapon over cosmetic prose -- it just renders stats-only.
        if (s.isString("flavor")) {
            log.warning("weapon '" + id + "' has a scalar 'flavor:'; it must be a YAML list "
                    + "(one '- ' item per line). Ignoring it.");
        }
        List<String> flavor = s.getStringList("flavor");

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
            // The authored ability NAME is the trigger's `name:` (falls back to the weapon name),
            // rendered as the gold ability-name line. The authored DESCRIPTION is `description:` --
            // a YAML list, same loud-scalar-warning as the weapon's flavor.
            String abilityName = t.getString("name", displayName);
            if (t.isString("description")) {
                log.warning("weapon '" + id + "' trigger '" + input + "' has a scalar 'description:'; "
                        + "it must be a YAML list (one '- ' item per line). Ignoring it.");
            }
            List<String> description = t.getStringList("description");
            AbilityDefinition ability = new AbilityDefinition(
                    id + "/" + input,
                    abilityName,
                    element,
                    "none",
                    t.getInt("cooldown_ticks", 0),
                    AbilitySchema.parseCost(t.getConfigurationSection("cost")),
                    AbilitySchema.parseCast(t.getConfigurationSection("cast")),
                    AbilitySchema.parseEffects(t.getMapList("on_hit")),
                    description,
                    // What is heard/seen the instant the trigger is pressed, as against on_hit's
                    // "where it resolves". For a projectile weapon those are ticks apart.
                    AbilitySchema.parseCastVisuals(t.getMapList("on_cast")));
            bindings.add(new TriggerBinding(input, ability));
        }

        // WeaponDefinition rejects an empty trigger list (and a negative attack_damage) -- caught above,
        // named, skipped.
        // The mint-on-craft claim. OPTIONAL and with NO default, unlike material above -- a default
        // would opt every weapon in and, since materials are contested by design (every sword-shaped
        // weapon leaves material at DEFAULT_MATERIAL), would index nothing at all. Absent is the
        // norm; a blank value is refused by the record and named as a skipped file.
        Optional<String> craftResult = Optional.ofNullable(s.getString("craft_result"));

        return new WeaponDefinition(id, displayName, element, rarity, weaponClass, material,
                attackDamage, attackSpeed, sweep, quiverSize, reloadTicks, bindings, flavor,
                craftResult);
    }

    private static Rarity rarity(String raw) {
        Rarity parsed = Rarity.fromName(raw);
        if (parsed == null) {
            throw new IllegalArgumentException(
                    "Unknown rarity '" + raw + "'; expected one of " + Arrays.toString(Rarity.values()));
        }
        return parsed;
    }

    /** Required: a missing (null) or unknown class throws, so the file is skipped and named, like a bad rarity. */
    private static WeaponClass weaponClass(String raw, String id) {
        if (raw == null) {
            throw new IllegalArgumentException("weapon '" + id + "' is missing required 'class' (one of "
                    + Arrays.toString(WeaponClass.values()) + ")");
        }
        WeaponClass parsed = WeaponClass.fromName(raw);
        if (parsed == null) {
            throw new IllegalArgumentException(
                    "Unknown class '" + raw + "' in weapon '" + id + "'; expected one of "
                            + Arrays.toString(WeaponClass.values()));
        }
        return parsed;
    }
}
