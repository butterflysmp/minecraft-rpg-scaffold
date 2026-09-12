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
     * {@code DamageSignatureTest} writes down about its own scope. IT DRIFTS IN TWO DIRECTIONS AND
     * THEY ARE NOT EQUALLY SAFE.</b>
     *
     * <ul>
     *   <li><b>A key ADDED to {@link #parse} and forgotten here</b> produces a spurious warning about
     *       a legitimate key. Loud, visible on the next boot, self-correcting. <b>Safe.</b>
     *   <li><b>A key REMOVED from {@link #parse} and left here</b> is the dangerous one: authoring it
     *       then warns NOTHING and does NOTHING -- <b>which is exactly the defect this guard exists
     *       to prevent, reintroduced by the guard's own staleness.</b> Every behavioural row stays
     *       green: the typo row uses a deliberate misspelling and is unaffected, and the
     *       legitimate-schema and shipped-file rows both assert SILENCE, which a stale entry produces.
     *       Measured, not reasoned -- adding a bogus {@code "sweap"} entry passed all 33 rows.
     * </ul>
     *
     * <p>An earlier version of this javadoc claimed the burden "fails towards noise and not towards
     * silence", which is true of the first case and <b>false of the second</b>, in the direction that
     * reads as reassurance. So the second is guarded rather than described:
     * {@code WeaponLoaderTest.knownKeysAndTheKeysParseActuallyReadsAreTheSameSet} reads this file's
     * own source and requires every entry below to appear as a real {@code s.getX("...")} read. Fields
     * do get renamed here -- {@code remint} exists because a weapon's material can change in content
     * -- so this is not hypothetical.
     *
     * <p><b>SCOPE, ON TWO AXES, stated because an omission is otherwise indistinguishable from an
     * oversight.</b>
     *
     * <p><b>Axis one -- WEAPONS ONLY.</b> Armor, shields, tools, enchants, abilities and mobs have the
     * same hazard and no such check; a general strict-key facility across every loader is a separate
     * pass, and this is the foothold rather than the finished job. Weapons go first because the quiver
     * is the first field in this repository where a typo yields a weapon that is SILENTLY WRONG
     * rather than visibly broken.
     *
     * <p><b>Axis two -- TOP-LEVEL KEYS ONLY, and this is the sharper gap of the two.</b>
     * {@code s.getKeys(false)} is not recursive, so <b>nothing here checks inside a {@code triggers:}
     * block</b>: a misspelled {@code cooldwon_ticks}, {@code casst}, or {@code on_hit} is read by
     * nobody and reported by nobody, exactly as a top-level typo was before this guard existed.
     * <b>That is worse than the axis above, not a lesser case of it</b> -- {@code cooldown_ticks} IS a
     * weapon's fire rate, so a silently-ignored one is precisely the "silently wrong rather than
     * visibly broken" property given above as the reason weapons went first. The trigger-level key
     * set is bounded and knowable, so it is checked too -- see {@link #TRIGGER_KEYS}.
     *
     * <p><b>What remains unchecked is everything BELOW a trigger</b>: the fields inside
     * {@code cast:}, and the entries of {@code on_hit:} / {@code on_cast:} / {@code cost:}. Those are
     * parsed by {@code AbilitySchema} against a grammar SHARED with abilities, so extending the check
     * there covers both content kinds at once -- which is the separate pass named above, not this
     * one. <b>The boundary is two levels deep, and it is drawn here rather than left to be
     * inferred.</b>
     */
    static final java.util.Set<String> KNOWN_KEYS = java.util.Set.of(
            "id", "display_name", "element", "rarity", "class", "material",
            "attack_damage", "attack_speed", "sweep", "quiver_size", "reload_ticks",
            "flavor", "triggers", "craft_result");

    /**
     * Every key a single {@code triggers:} entry may author -- a SEPARATE namespace from
     * {@link #KNOWN_KEYS}, deliberately.
     *
     * <p><b>Merging the two would be the obvious simplification and it would be wrong.</b> One
     * combined set accepts every key at every level: {@code cast:} would become legal at the top of
     * a weapon file and {@code material:} legal inside a trigger, both silently ignored -- the exact
     * failure both checks exist to prevent. A key is only meaningful at its own level.
     *
     * <p><b>This layer matters MORE than the top level, not less, and the worked example is worse
     * than "the wrong cadence".</b> {@code cooldown_ticks} lives here and is the weapon's fire rate
     * under SINGLE presses -- under HELD fire it is a LOWER BOUND that quantises, see below.
     * Misspell it and the trigger takes the {@code getInt} default of <b>0</b>, and
     * {@code AbilityService.resolve} then raises it only to
     * {@code CastSpec.minimumCooldownTicks(cast)} -- which is <b>0 for every cast shape except
     * {@code Volley}</b>. Measured, by invoking it: {@code Ray}, {@code Projectile}, {@code Melee}
     * and {@code Self} all return 0; only {@code Volley} derives one (30, for the Cursed Emerald's
     * windup 20 + (6-1) x 2).
     *
     * <h2>UNDER HELD FIRE, {@code cooldown_ticks} HAS 4-TICK GRANULARITY. MEASURED 2026-09-12.</h2>
     *
     * <p><b>An authored {@code cooldown_ticks} is NOT the weapon's fire interval.</b> A fire needs an
     * input AND an expired cooldown, and a held right-click delivers an input only every <b>4
     * ticks</b> -- Q7's answer, measured on two weapons and two materials, {@code GATE-q7.md}. So the
     * real interval is the cooldown <b>rounded UP to the next input</b>:
     *
     * <pre>
     * ceil(11 / 4) x 4 = 12      quiver_stone measured min 12   EXACT
     * ceil(15 / 4) x 4 = 16      hunters_bow  measured min 16   EXACT
     * </pre>
     *
     * <p><b>So 9, 10, 11 and 12 all produce 12.</b> An author who writes 11 has written 12, and
     * nothing in the schema says so -- which is why it is said here, at the key. Choose a fire rate
     * in multiples of 4 or accept that the number authored is not the number delivered.
     *
     * <p><b>AND THE SAME QUANTISATION SWALLOWS ATTACK SPEED, WHICH REACHES A SHIPPED STAT.</b>
     * {@code AttackSpeed.effectiveCooldownTicks} divides and rounds, and the result is then rounded
     * up to the next input. On {@code hunters_bow} (authored 15): <b>1.1x and 1.2x both still fire
     * at 16</b>; only 1.3x reaches 12. A player equipping a +10% attack-speed piece sees the stat
     * sheet move and the weapon not move. <b>A falsified number shown to a player is the class this
     * project treats as worst</b>, and it is recorded here rather than left for the first person who
     * times it.
     *
     * <p><b>ONE BOUNDARY IS UNMEASURED AND IS NOT MODELLED PAST.</b> Neither weapon read had a
     * cooldown that is an exact multiple of 4, so whether a cooldown of exactly <b>12 fires at 12 or
     * at 16</b> is unknown -- it is the difference between {@code >=} and {@code >} in
     * {@code CooldownTracker.isReady}. {@code /rpg firerate} answers it in one hold on a weapon
     * authored at 12 or 16. Owed, and named as owed.
     *
     * <p>So on a HELD-FIRE QUIVER WEAPON -- a ray or a projectile, which is exactly what the Boltor
     * is -- <b>a misspelled cooldown key means NO COOLDOWN AT ALL.</b> The magazine empties as fast
     * as the input repeats, and every symptom of that reads as <i>"this weapon is fast"</i>, which is
     * the stated design intent. Nothing looks wrong; there is simply no rate limit. That is the
     * worked example this whole check exists for, and it is why the trigger layer was closed before
     * the first quiver weapon was authored rather than after.
     *
     * <p><b>Hand-maintained, so it carries the identical stale-entry hazard</b> {@link #KNOWN_KEYS}
     * does: an entry left here after {@code parse} stops reading it warns nothing and does nothing,
     * reopening the hole one layer down. Guarded the same way and in the same test --
     * {@code WeaponLoaderTest.knownKeysAndTheKeysParseActuallyReadsAreTheSameSet} requires set
     * equality for BOTH levels, scanning {@code t.getX("...")} for this one.
     *
     * <p><b>THE TWO SETS ARE DISJOINT TODAY, AND THAT IS LOAD-BEARING FOR THE TEST RATHER THAN A
     * coincidence.</b> The test separates the levels by RECEIVER ({@code s.} versus {@code t.}), and
     * the near miss is real -- the top level has {@code display_name} where a trigger has
     * {@code name}. While no key lives in both namespaces, a read attributed to the wrong receiver
     * lands in the wrong set and surfaces immediately as inequality. <b>The day a key name is shared
     * between the two levels, that discriminator gets QUIETER rather than louder</b>: a
     * misattribution could then cancel out, with both sets still equal and one level silently
     * unchecked for that key. Anyone adding such a key should strengthen the test first.
     */
    static final java.util.Set<String> TRIGGER_KEYS = java.util.Set.of(
            "name", "description", "cooldown_ticks", "cost", "cast", "on_hit", "on_cast");

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
            // The same check one layer down, and the layer that matters more: cooldown_ticks lives
            // HERE and IS the weapon's fire rate, so a misspelling of it produces a weapon that
            // fires at the wrong cadence while looking entirely correct. Warn, never skip, for the
            // reason the top-level check gives.
            for (String key : t.getKeys(false)) {
                if (!TRIGGER_KEYS.contains(key)) {
                    log.warning("weapon '" + id + "' trigger '" + input + "' has unknown key '" + key
                            + "'; it is read by nothing and will be ignored. Check the spelling"
                            + " against the trigger schema.");
                }
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
