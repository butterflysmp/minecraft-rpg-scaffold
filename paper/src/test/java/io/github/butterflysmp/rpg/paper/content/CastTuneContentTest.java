package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.CastTunes;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.CastTune.Quantity;
import io.github.butterflysmp.rpg.core.enchant.EnchantCandidate;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine;
import io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE FIVE GUARANTEED CAST TUNES AS SHIPPED (the seat's brief of 2026-11-24, PR D), through the real loaders: Ben's names and
 * numbers, each tune applied to its weapon's LOADED right click, the lines, the roll's slot 0 on every mage weapon over many
 * rolls, and the loader's refusals by name.
 */
class CastTuneContentTest {

    private static final Set<String> ELEMENTS = Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither");

    private static Logger capturing(List<String> into) {
        Logger log = Logger.getLogger("CastTuneContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { into.add(r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    private static io.github.butterflysmp.rpg.core.weapon.WeaponRegistry weapons() {
        var weapons = new WeaponLoader(capturing(new ArrayList<>()))
                .loadAll(Path.of("src", "main", "resources", "content", "weapons").toFile());
        assertTrue(weapons.find("knell_beam").isPresent(), "CONTROL: the weapons loaded");
        return weapons;
    }

    private static EnchantRegistry shipped(List<String> warnings) {
        File dir = Path.of("src", "main", "resources", "content", "enchants").toFile();
        return EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, weapons()::find, false).loadAll(dir);
    }

    private static EnchantDefinition one(String id) {
        List<String> warnings = new ArrayList<>();
        return shipped(warnings).find(id).orElseThrow(() -> new AssertionError(id + " did not load: " + warnings));
    }

    /** Ben's name, weapon, quantity and numbers for each; every one guaranteed, weapon-specific, three levels. */
    @Test
    void bensFive() {
        record Want(String id, String name, String weapon, Quantity q, List<Double> values) {}
        for (Want w : List.of(
                new Want("large_sparks", "Large Sparks", "flint_staff", Quantity.DAMAGE, List.of(4.0, 8.0, 12.0)),
                new Want("sharp_crystals", "Sharp Crystals", "crystal_shard_staff", Quantity.DAMAGE, List.of(3.0, 6.0, 9.0)),
                new Want("polished_scepter", "Polished Scepter", "lapis_staff", Quantity.RANGE, List.of(4.0, 8.0, 12.0)),
                new Want("echo_beam", "Echo Beam", "cursed_emerald", Quantity.SHOTS, List.of(4.0, 5.0, 6.0)),
                new Want("early_toll", "Early Toll", "knell_beam", Quantity.WINDUP, List.of(2.0, 4.0, 8.0)))) {
            EnchantDefinition d = one(w.id());
            assertEquals(w.name(), d.displayName(), w.id());
            assertEquals(w.weapon(), d.weapon(), w.id());
            assertTrue(d.guaranteed(), w.id() + " is guaranteed (G1)");
            assertEquals(EnchantEffect.CAST_TUNE, d.effect(), w.id());
            assertEquals(3, d.maxLevel(), w.id());
            assertEquals(new EnchantBehaviour.CastTune(w.weapon(), w.q(), w.values()), d.behaviour(), w.id());
        }
    }

    private static AbilityDefinition tunedAt(String enchant, String weapon, int level) {
        AbilityDefinition cast = weapons().find(weapon).orElseThrow().trigger("right_click").orElseThrow().ability();
        return CastTunes.tune(cast, List.of(new ActiveBehaviour(enchant, level, one(enchant).behaviour())));
    }

    private static double damage(AbilityDefinition a) {
        return a.onHit().stream().filter(e -> e instanceof EffectSpec.Damage).map(e -> ((EffectSpec.Damage) e).amount())
                .findFirst().orElseThrow();
    }

    /** Each tune on its weapon's LOADED right click, at every level: the brief's table, as numbers the cast now holds. */
    @Test
    void eachTuneOnItsLoadedWeapon() {
        for (int level = 1; level <= 3; level++) {
            int i = level - 1;
            assertEquals(new double[]{24, 28, 32}[i], damage(tunedAt("large_sparks", "flint_staff", level)), "20 + 4/8/12");
            assertEquals(new double[]{19, 22, 25}[i], damage(tunedAt("sharp_crystals", "crystal_shard_staff", level)),
                    "16 + 3/6/9");
            assertEquals(new double[]{30, 34, 38}[i],
                    assertInstanceOf(CastSpec.Ray.class, tunedAt("polished_scepter", "lapis_staff", level).cast()).range(),
                    "26 + 4/8/12");
            assertEquals(new int[]{4, 5, 6}[i],
                    assertInstanceOf(CastSpec.Volley.class, tunedAt("echo_beam", "cursed_emerald", level).cast()).shots());
            CastSpec.Volley knell = assertInstanceOf(CastSpec.Volley.class, tunedAt("early_toll", "knell_beam", level).cast());
            assertEquals(new int[]{22, 20, 16}[i], knell.windupTicks(), "24 - 2/4/8");
            assertTrue(knell.lockedAim(), "the locked aim is untouched");
            assertEquals(List.of(0), knell.warning().flashAt(), "the one flash, at the wind-up's tick 0, untouched");
        }
    }

    /** B5: the line at each level, the real number in the quantity's unit (Early Toll in Ben's seconds). */
    @Test
    void theLines() {
        String[][] want = {
                {"large_sparks", "+4 damage", "+8 damage", "+12 damage"},
                {"sharp_crystals", "+3 damage", "+6 damage", "+9 damage"},
                {"polished_scepter", "+4 blocks of range", "+8 blocks of range", "+12 blocks of range"},
                {"echo_beam", "volleys of 4 shots", "volleys of 5 shots", "volleys of 6 shots"},
                {"early_toll", "-0.1s wind-up", "-0.2s wind-up", "-0.4s wind-up"}};
        for (String[] row : want) {
            EnchantDefinition d = one(row[0]);
            for (int level = 1; level <= 3; level++) {
                assertEquals(row[level], EnchantEffectLine.bare(d, level, GearClass.MAGE), row[0] + " level " + level);
            }
        }
    }

    /**
     * THE POOLS, OVER MANY ROLLS (the gate's rollstats row, as a unit row): every fresh roll of each mage weapon with a
     * guaranteed enchant offers it in slot 0, FIRST, locked; the Lapis Staff and the Cursed Emerald offer their behavioural
     * one beside it every time (a pool of one rest, so the cascade's last tier always lands it); Blaze King's Staff's slot
     * 0 is empty; the Breacher's is Shatter Caps alone, as before.
     */
    @Test
    void slotZeroOverManyRolls() {
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = shipped(warnings);
        var roster = EnchantRollItems.roster(r.all());
        var cascade = new io.github.butterflysmp.rpg.core.enchant.CandidateCascade(List.of(
                new io.github.butterflysmp.rpg.core.enchant.CandidateCascade.Tier(4, 0.10),
                new io.github.butterflysmp.rpg.core.enchant.CandidateCascade.Tier(3, 0.40),
                new io.github.butterflysmp.rpg.core.enchant.CandidateCascade.Tier(2, 0.70),
                new io.github.butterflysmp.rpg.core.enchant.CandidateCascade.Tier(1, 1.0)));
        Map<String, List<String>> want = Map.of(
                "flint_staff", List.of("large_sparks"),
                "crystal_shard_staff", List.of("sharp_crystals"),
                "lapis_staff", List.of("polished_scepter", "lapis_crystal"),
                "cursed_emerald", List.of("echo_beam", "focused_beam"),
                "knell_beam", List.of("early_toll"),
                "blaze_kings_staff", List.of(),
                "breacher", List.of("shatter_caps"));
        var weapons = weapons();
        Random random = new Random(20261124L);
        for (var e : want.entrySet()) {
            WeaponDefinition w = weapons.find(e.getKey()).orElseThrow();
            for (int n = 0; n < 500; n++) {
                EnchantState s = EnchantRoll.roll(w.id(), w.gearElement(), EnchantRollItems.has(w),
                        GearClass.of(w.weaponClass()), roster, cascade, random::nextDouble);
                var slot0 = s.slots().get(0);
                assertEquals(e.getValue(), slot0.candidates().stream().map(EnchantCandidate::enchantId).toList(),
                        e.getKey() + " roll " + n);
                assertTrue(slot0.candidates().stream().allMatch(c -> c.level() == 0), "locked");
            }
        }
    }

    /**
     * The loader refuses, BY NAME: a quantity the weapon's right click does not have (range on the Flint Staff's
     * projectile; shots and windup on a cast that is no volley; damage on a cast with no literal damage, which no shipped
     * weapon is, so a weapon_damage-only right click, the Boltor's); an unknown quantity; no weapon; guaranteed on an enchant
     * with no weapon; a wind-up cut that leaves the Knell's warning flash at or after the shot; a shot count of 0; a
     * guaranteed value that is not a boolean. And one good file, the control.
     */
    @Test
    void aMalformedTuneIsRefusedByName(@TempDir Path dir) throws IOException {
        String head = "display_name: \"T\"\nmax_level: 3\neffect: cast_tune\nclass: universal\n";
        java.util.function.BiFunction<String, String, String> block = (q, v) ->
                "cast_tune:\n  quantity: " + q + "\n  value_by_level: " + v + "\n";
        Files.writeString(dir.resolve("good.yml"), head + "weapon: flint_staff\nguaranteed: true\n" + block.apply("damage", "[4, 8, 12]"));
        Files.writeString(dir.resolve("range_on_projectile.yml"), head + "weapon: flint_staff\n" + block.apply("range", "[4, 8, 12]"));
        Files.writeString(dir.resolve("shots_on_ray.yml"), head + "weapon: lapis_staff\n" + block.apply("shots", "[4, 5, 6]"));
        Files.writeString(dir.resolve("windup_on_projectile.yml"), head + "weapon: flint_staff\n" + block.apply("windup", "[2, 4, 8]"));
        Files.writeString(dir.resolve("damage_on_weapon_damage.yml"), head + "weapon: boltor\n" + block.apply("damage", "[4, 8, 12]"));
        Files.writeString(dir.resolve("unknown_quantity.yml"), head + "weapon: flint_staff\n" + block.apply("speed", "[4, 8, 12]"));
        Files.writeString(dir.resolve("unbound.yml"), head + block.apply("damage", "[4, 8, 12]"));
        Files.writeString(dir.resolve("windup_too_long.yml"), head + "weapon: knell_beam\n" + block.apply("windup", "[2, 4, 24]"));
        Files.writeString(dir.resolve("zero_shots.yml"), head + "weapon: cursed_emerald\n" + block.apply("shots", "[0, 5, 6]"));
        Files.writeString(dir.resolve("two_levels.yml"), head + "weapon: flint_staff\n" + block.apply("damage", "[4, 8]"));
        Files.writeString(dir.resolve("guaranteed_word.yml"), head + "weapon: flint_staff\nguaranteed: always\n"
                + block.apply("damage", "[4, 8, 12]"));
        Files.writeString(dir.resolve("guaranteed_loose.yml"), "display_name: \"G\"\nmax_level: 1\neffect: durability\n"
                + "class: universal\nguaranteed: true\n");
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, weapons()::find, false)
                .loadAll(dir.toFile());
        assertTrue(r.find("good").isPresent(), "the control: " + warnings);
        assertTrue(r.find("good").orElseThrow().guaranteed(), "the control's guaranteed: true arrives");
        for (String bad : List.of("range_on_projectile", "shots_on_ray", "windup_on_projectile", "damage_on_weapon_damage",
                "unknown_quantity", "unbound", "windup_too_long", "zero_shots", "two_levels", "guaranteed_word",
                "guaranteed_loose")) {
            assertTrue(r.find(bad).isEmpty(), bad + " loaded: " + warnings);
        }
        java.util.function.Predicate<String> warned = s -> warnings.stream().anyMatch(w -> w.contains(s));
        assertTrue(warned.test("'range_on_projectile' cast_tune: weapon 'flint_staff''s right click has no range to tune"), warnings.toString());
        assertTrue(warned.test("'shots_on_ray' cast_tune: weapon 'lapis_staff''s right click has no shots to tune"), warnings.toString());
        assertTrue(warned.test("'windup_on_projectile' cast_tune: weapon 'flint_staff''s right click has no windup to tune"), warnings.toString());
        assertTrue(warned.test("'damage_on_weapon_damage' cast_tune: weapon 'boltor''s right click has no damage to tune"), warnings.toString());
        assertTrue(warned.test("'unknown_quantity' cast_tune: quantity 'speed' must be one of damage, range, shots, windup"), warnings.toString());
        assertTrue(warned.test("'unbound' cast_tune: tunes one weapon's cast, so the enchant must declare weapon: <id>"), warnings.toString());
        assertTrue(warned.test("'windup_too_long' cast_tune: a volley warning flash_at tick must be in [0, windup_ticks 0)"), warnings.toString());
        assertTrue(warned.test("'zero_shots' cast_tune: cast_tune shots at level 1 was 0.0; a shot count is a whole number of 1 or more"), warnings.toString());
        assertTrue(warned.test("two_levels"), warnings.toString());
        assertTrue(warned.test("'guaranteed_word' declares guaranteed: 'always'; it must be true or false"), warnings.toString());
        assertTrue(warned.test("'guaranteed_loose' declares guaranteed: true but no weapon:"), warnings.toString());
    }

    /** G1's cross-file rule: more guaranteed enchants for one weapon than the slot shows refuses every one of them. */
    @Test
    void moreGuaranteedThanTheSlotShowsIsRefused(@TempDir Path dir) throws IOException {
        String body = "max_level: 3\neffect: cast_tune\nclass: universal\nweapon: flint_staff\nguaranteed: true\n"
                + "cast_tune:\n  quantity: damage\n  value_by_level: [1, 2, 3]\n";
        for (int i = 1; i <= EnchantRoll.MAX_CANDIDATES + 1; i++) {
            Files.writeString(dir.resolve("g" + i + ".yml"), "display_name: \"G" + i + "\"\n" + body);
        }
        Files.writeString(dir.resolve("other.yml"), "display_name: \"O\"\n" + body.replace("flint_staff", "crystal_shard_staff"));
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, weapons()::find, false)
                .loadAll(dir.toFile());
        assertTrue(r.find("other").isPresent(), "the control: another weapon's one guaranteed enchant loads");
        for (int i = 1; i <= EnchantRoll.MAX_CANDIDATES + 1; i++) {
            assertTrue(r.find("g" + i).isEmpty(), "g" + i);
        }
        assertTrue(warnings.stream().anyMatch(w -> w.contains("weapon 'flint_staff' has 5 guaranteed enchants")), warnings.toString());
    }
}
