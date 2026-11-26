package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.weapon.DescriptionTokens;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * True tooltips' content half (the seat's D1, D2, D4 and T7, 2026-11-26): the loaders refuse a token they cannot fill, by
 * name, from the REAL shipped files with one token planted; and every shipped token and every behaviour enchant's
 * {@code ability_description:} is fillable by the cast it describes.
 */
class TrueTooltipsContentTest {

    private static final Path CONTENT = Path.of("src", "main", "resources", "content");

    private static Logger capturing(List<String> into) {
        Logger log = Logger.getLogger("TrueTooltipsContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { into.add(r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    private static WeaponRegistry weapons() {
        return new WeaponLoader(capturing(new ArrayList<>())).loadAll(CONTENT.resolve("weapons").toFile());
    }

    @Test
    void theCursedEmeraldsSentenceIsTokenisedAndTheLoaderAcceptsIt() throws IOException {
        String yml = Files.readString(CONTENT.resolve("weapons/cursed_emerald.yml"), StandardCharsets.UTF_8);
        assertTrue(yml.contains("\"Channel, then loose {shots_word}\""), "D2: the shipped sentence carries the token");
        WeaponDefinition emerald = weapons().find("cursed_emerald").orElseThrow();
        assertEquals(List.of("Channel, then loose three", "bolts down your line of sight."),
                DescriptionTokens.fill(emerald.trigger("right_click").orElseThrow().ability().description(),
                        emerald.trigger("right_click").orElseThrow().ability().cast()), "bare, Ben's sentence exactly");
    }

    @Test
    void aWeaponWithAnUnknownTokenIsRefusedByNameAndTheTokenOnACastWithNoCountToo(@TempDir Path dir) throws IOException {
        String yml = Files.readString(CONTENT.resolve("weapons/cursed_emerald.yml"), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("cursed_emerald.yml"), yml.replace("{shots_word}", "{damage}"), StandardCharsets.UTF_8);
        String lapis = Files.readString(CONTENT.resolve("weapons/lapis_staff.yml"), StandardCharsets.UTF_8);
        String planted = lapis.replace("\"Right-click to fire a blue beam.\"", "\"Fires {shots} blue beams.\"");
        assertTrue(!planted.equals(lapis), "CONTROL: the lapis sentence was planted");
        Files.writeString(dir.resolve("lapis_staff.yml"), planted, StandardCharsets.UTF_8);

        List<String> log = new ArrayList<>();
        WeaponRegistry r = new WeaponLoader(capturing(log)).loadAll(dir.toFile());
        assertTrue(r.find("cursed_emerald").isEmpty(), "an unknown token skips the file");
        assertTrue(r.find("lapis_staff").isEmpty(), "{shots} on a ray, which has no count, skips the file");
        assertTrue(log.stream().anyMatch(l -> l.contains("cursed_emerald") && l.contains("{damage}")), String.join("\n", log));
        assertTrue(log.stream().anyMatch(l -> l.contains("lapis_staff") && l.contains("{shots}")), String.join("\n", log));
    }

    @Test
    void anEnchantsAbilityDescriptionIsAListAndOnlyABehaviourMayCarryOne(@TempDir Path dir) throws IOException {
        String shatter = Files.readString(CONTENT.resolve("enchants/shatter_caps.yml"), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("shatter_caps.yml"), shatter.replace("{shots_word}", "{range}"), StandardCharsets.UTF_8);
        String sharp = Files.readString(CONTENT.resolve("enchants/sharpness.yml"), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("sharpness.yml"), sharp + "\nability_description:\n  - \"Cuts\"\n", StandardCharsets.UTF_8);
        String power = Files.readString(CONTENT.resolve("enchants/power.yml"), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("power.yml"), power + "\nability_description: \"one line\"\n", StandardCharsets.UTF_8);

        List<String> log = new ArrayList<>();
        WeaponRegistry w = weapons();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(log), id -> true, w::find, false).loadAll(dir.toFile());
        assertTrue(r.find("shatter_caps").isEmpty(), "{range} is not a token in this slice (D3)");
        assertTrue(r.find("sharpness").isEmpty(), "a stat enchant carries no ability prose");
        assertTrue(r.find("power").isEmpty(), "a scalar is refused, not read as nothing");
        String all = String.join("\n", log);
        assertTrue(all.contains("{range}"), all);
        assertTrue(all.contains("sharpness") && all.contains("no behaviour"), all);
        assertTrue(all.contains("power") && all.contains("YAML list"), all);
    }

    @Test
    void everyShippedBehaviourProseIsFillableByTheCastItDescribes() {
        WeaponRegistry w = weapons();
        EnchantRegistry enchants = EnchantLoader.withWeapons(capturing(new ArrayList<>()), id -> true, w::find, false)
                .loadAll(CONTENT.resolve("enchants").toFile());
        List<String> withProse = new ArrayList<>();
        for (EnchantDefinition e : enchants.all()) {
            if (e.abilityDescription().isEmpty()) continue;
            withProse.add(e.id());
            CastSpec cast = w.find(e.weapon()).orElseThrow().trigger("right_click").orElseThrow().ability().cast();
            DescriptionTokens.validate(e.abilityDescription(), cast);   // throws by name if it cannot be filled
        }
        assertEquals(List.of("airburst_flames", "cluster_bombs", "focused_beam", "shatter_caps"), withProse.stream().sorted().toList());
    }
}
