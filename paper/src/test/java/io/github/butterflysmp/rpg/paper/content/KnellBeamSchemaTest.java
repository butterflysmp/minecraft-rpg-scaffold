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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE KNELL BEAM'S FOUR OPTIONS, IN A WEAPON FILE (the seat's brief of 2026-11-21, PR 2): {@code pierce} and
 * {@code tolerance} on a ray, {@code locked_aim} and {@code warning:} on a volley. Each arrives with the value authored
 * (BOTH answers, so a loader that hard-coded either cannot pass), a warning without locked aim is refused BY NAME, and
 * the sweep's port keeps {@code castRay}'s filters (an end crystal is never a body).
 */
class KnellBeamSchemaTest {

    @TempDir
    Path dir;

    private final List<String> warnings = new ArrayList<>();

    private WeaponRegistry load(String yaml) throws IOException {
        Files.writeString(dir.resolve("probe.yml"), yaml, StandardCharsets.UTF_8);
        Logger log = Logger.getLogger("KnellBeamSchemaTest-" + System.nanoTime());
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

    private static String weapon(String volleyExtra, String rayExtra) {
        return """
                id: probe
                display_name: "Probe"
                element: void
                rarity: common
                class: mage
                material: echo_shard
                triggers:
                  right_click:
                    cooldown_ticks: 48
                    cast:
                      type: volley
                      windup_ticks: 24
                      shots: 1
                %s
                      of:
                        type: ray
                        range: 40
                %s
                    on_hit:
                      - type: damage
                        amount: 10
                        element: void
                """.formatted(volleyExtra.indent(6), rayExtra.indent(8));
        // indent(): a text block strips its common indentation, so the extras arrive at column 0. First written
        // without this, the YAML was malformed, and the loader's only report was Bukkit's own logger failing off a
        // server ("Cannot invoke org.bukkit.Server.getLogger()"), which read as a defect in the schema.
    }

    private CastSpec.Volley volleyOf(WeaponRegistry registry) {
        return assertInstanceOf(CastSpec.Volley.class,
                registry.find("probe").orElseThrow(() -> new AssertionError("probe did not load: " + warnings))
                        .trigger("right_click").orElseThrow().ability().cast());
    }

    /** All four authored ON: each arrives with its value. */
    @Test
    void allFourOptionsArrive() throws IOException {
        CastSpec.Volley v = volleyOf(load(weapon("""
                      locked_aim: true
                      warning:
                        beam: knell_warning
                        flash_at: [0]""", """
                        pierce: true
                        tolerance: 0.3""")));
        assertTrue(v.lockedAim());
        assertEquals(new CastSpec.Volley.Warning("knell_warning", List.of(0)), v.warning());
        CastSpec.Ray ray = assertInstanceOf(CastSpec.Ray.class, v.of());
        assertTrue(ray.pierce());
        assertEquals(0.3, ray.tolerance(), 0.0);
        assertEquals(List.of(), warnings);
    }

    /** All four ABSENT: each is off, so a file authored before them parses to what it always did. */
    @Test
    void absentMeansOff() throws IOException {
        CastSpec.Volley v = volleyOf(load(weapon("", "")));
        assertFalse(v.lockedAim());
        assertEquals(null, v.warning());
        CastSpec.Ray ray = assertInstanceOf(CastSpec.Ray.class, v.of());
        assertFalse(ray.pierce());
        assertEquals(0.0, ray.tolerance(), 0.0);
        assertFalse(ray.sweeps(), "a plain ray, on castRay");
    }

    /** A warning without locked aim: the file is skipped, and the warning names it and the rule. */
    @Test
    void aWarningWithoutLockedAimIsRefusedByName() throws IOException {
        WeaponRegistry registry = load(weapon("""
                      warning:
                        beam: knell_warning
                        flash_at: [0]""", ""));
        assertTrue(registry.find("probe").isEmpty(), "the weapon must not load");
        assertTrue(warnings.stream().anyMatch(w -> w.contains("probe.yml") && w.contains("locked_aim")),
                "named, with the rule: " + warnings);
    }

    /**
     * RULINGS 5 AND 6, PINNED: the sweep's port takes castRay's own two filters. Bodies come through
     * {@code LiveTargets.except} (live living bodies, the caster excluded: an END CRYSTAL is never one), and blocks
     * through {@code rayTraceBlocks(…, FluidCollisionMode.NEVER, true)}. Bounded at the next member declaration.
     */
    @Test
    void theSweepKeepsCastRaysFilters() throws IOException {
        String src = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/adapter/PaperCombatWorld.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        int start = src.indexOf("public io.github.butterflysmp.rpg.core.combat.RaySweep sweepRay(");
        assertTrue(start > 0, "sweepRay is not found: the scan is blind");
        int end = src.indexOf("\n    @Override", start);
        assertTrue(end > start, "no member after sweepRay: the bound is blind");
        String body = src.substring(start, end);
        assertTrue(body.contains("return new io.github.butterflysmp.rpg.core.combat.RaySweep(new java.util.ArrayList<>"),
                "CONTROL: the bound holds sweepRay's own return");
        assertTrue(body.contains("LiveTargets.except(ignoreId)"), "bodies through castRay's filter");
        assertTrue(body.contains("LiveTargets.resolve(e)"), "a part resolved to its body");
        assertTrue(body.contains("FluidCollisionMode.NEVER, /* ignorePassableBlocks */ true"), "castRay's block trace");
    }
}
