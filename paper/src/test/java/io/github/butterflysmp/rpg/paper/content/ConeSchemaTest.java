package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
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
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE CONE, IN A WEAPON FILE (the seat's brief of 2026-11-22, PR 2): {@code type: cone} with {@code range},
 * {@code arc_degrees}, {@code travel_ticks} and an optional {@code front}. Every value arrives as authored, and the
 * staged numbers are all distinct (6.5, 70, 5) so a transposition cannot pass. A missing required key and a volley of a
 * cone are refused by name, the file skipped.
 */
class ConeSchemaTest {

    @TempDir
    Path dir;

    private final List<String> warnings = new ArrayList<>();

    private WeaponRegistry load(String yaml) throws IOException {
        Files.writeString(dir.resolve("probe.yml"), yaml, StandardCharsets.UTF_8);
        Logger log = Logger.getLogger("ConeSchemaTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) {
                if (r.getLevel().intValue() >= Level.WARNING.intValue()) warnings.add(r.getMessage());
            }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return new WeaponLoader(log).loadAll(new File(dir.toString()));
    }

    private static String weapon(String cast) {
        return """
                id: probe
                display_name: "Probe"
                element: fire
                rarity: common
                class: mage
                material: lantern
                triggers:
                  right_click:
                    cooldown_ticks: 16
                    cast:
                %s
                    on_hit:
                      - type: damage
                        amount: 14
                        element: fire
                """.formatted(cast.indent(6));
    }

    private CastSpec.Cone coneOf(WeaponRegistry registry) {
        return assertInstanceOf(CastSpec.Cone.class,
                registry.find("probe").orElseThrow(() -> new AssertionError("probe did not load: " + warnings))
                        .trigger("right_click").orElseThrow().ability().cast());
    }

    @Test
    void everyKeyArrivesAsAuthored() throws IOException {
        CastSpec.Cone cone = coneOf(load(weapon("""
                type: cone
                range: 6.5
                arc_degrees: 70
                travel_ticks: 5
                front: lantern_front""")));
        assertEquals(new CastSpec.Cone(6.5, 70, 5, "lantern_front"), cone);
        assertEquals(List.of(), warnings);
    }

    @Test
    void theFrontIsOptional() throws IOException {
        CastSpec.Cone cone = coneOf(load(weapon("""
                type: cone
                range: 6.5
                arc_degrees: 70
                travel_ticks: 0""")));
        assertNull(cone.front());
        assertEquals(0, cone.travelTicks(), "0 authored, 0 arrived: instant");
    }

    /** Each of the three numbers is REQUIRED: a file missing one is skipped, and the warning names the key. */
    @Test
    void aMissingNumberIsRefusedByName() throws IOException {
        for (String missing : List.of("range", "arc_degrees", "travel_ticks")) {
            warnings.clear();
            StringBuilder cast = new StringBuilder("type: cone\n");
            if (!missing.equals("range")) cast.append("range: 6.5\n");
            if (!missing.equals("arc_degrees")) cast.append("arc_degrees: 70\n");
            if (!missing.equals("travel_ticks")) cast.append("travel_ticks: 5\n");
            WeaponRegistry registry = load(weapon(cast.toString().strip()));
            assertTrue(registry.find("probe").isEmpty(), "missing " + missing + ": the weapon must not load");
            assertTrue(warnings.stream().anyMatch(w -> w.contains("probe.yml") && w.contains(missing)),
                    "missing " + missing + ", named: " + warnings);
        }
    }

    /** A volley of a cone is refused by name, as a volley of melee is. */
    @Test
    void aVolleyOfAConeIsRefusedByName() throws IOException {
        WeaponRegistry registry = load(weapon("""
                type: volley
                windup_ticks: 0
                shots: 2
                interval_ticks: 4
                of:
                  type: cone
                  range: 6.5
                  arc_degrees: 70
                  travel_ticks: 5"""));
        assertTrue(registry.find("probe").isEmpty(), "the weapon must not load");
        assertTrue(warnings.stream().anyMatch(w -> w.contains("probe.yml") && w.contains("'cone'")),
                "named: " + warnings);
    }
}
