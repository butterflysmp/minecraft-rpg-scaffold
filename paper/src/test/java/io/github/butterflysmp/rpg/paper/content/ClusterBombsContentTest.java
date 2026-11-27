package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantCandidate;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems;
import io.github.butterflysmp.rpg.paper.weapon.GearItems;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CLUSTER BOMBS AS SHIPPED (the seat's F9, F10, 2026-11-26): Ben's 55 / 80 / 105 and radius 5, three splits, the
 * Blast Fungus's and NOT guaranteed; the loader refuses it, by name, on a weapon whose right click is not fused; and slot
 * 0 of a fresh Blast Fungus offers Fast and Fungal FIRST and Cluster Bombs beside it (only one is ever ACTIVE).
 */
class ClusterBombsContentTest {

    private static final File CONTENT = Path.of("src", "main", "resources", "content").toFile();

    private static Logger quiet(List<String> warnings) {
        Logger log = Logger.getLogger("ClusterBombsContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) {
                if (r.getLevel().intValue() >= Level.WARNING.intValue()) warnings.add(r.getMessage());
            }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    private static EnchantRegistry enchants(Logger log, WeaponRegistry weapons, File dir) {
        var elements = new ElementLoader(log).loadAll(new File(CONTENT, "elements"));
        return EnchantLoader.withWeapons(log, id -> elements.find(id).isPresent(), weapons::find, false).loadAll(dir);
    }

    @Test
    void itShipsAsBenRuledIt() {
        List<String> warnings = new ArrayList<>();
        Logger log = quiet(warnings);
        WeaponRegistry w = new WeaponLoader(log).loadAll(new File(CONTENT, "weapons"));
        EnchantDefinition c = enchants(log, w, new File(CONTENT, "enchants")).find("cluster_bombs")
                .orElseThrow(() -> new AssertionError("cluster_bombs did not load: " + warnings));
        assertEquals(List.of(), warnings.stream().filter(m -> m.contains("cluster_bombs")).toList());
        assertEquals("Cluster Bombs", c.displayName());
        assertEquals("blast_fungus", c.weapon());
        assertFalse(c.guaranteed(), "BEN: \"No its not guaranteed\"");
        var b = assertInstanceOf(EnchantBehaviour.ClusterBombs.class, c.behaviour());
        assertEquals(List.of(55.0, 80.0, 105.0), b.damageByLevel(), "BEN");
        assertEquals(5.0, b.radius(), "BEN: \"make the blast size of the big bomb 5 blocks\"");
        assertEquals(3, b.splitCount(), "BEN: splits into 3");
        assertEquals(2.5, b.splitDistance());
        assertEquals(20, b.splitFuseTicks());
        assertEquals("nature", b.element());
    }

    /** The loader refuses it, by name, on a weapon whose right click is not a fused projectile (the flint staff's). */
    @Test
    void itIsRefusedOnAWeaponWithNoFuse(@TempDir Path dir) throws IOException {
        String body = String.join("\n", "display_name: \"X\"", "max_level: 1", "effect: cluster_bombs",
                "class: universal", "weapon: %s", "cluster_bombs:", "  damage_by_level: [55]", "  element: nature",
                "  radius: 5", "  split_count: 3", "  split_distance: 2.5", "  split_fuse_ticks: 20", "  split_lift: 0.25", "");
        Files.writeString(dir.resolve("on_flint.yml"), body.formatted("flint_staff"), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("on_fungus.yml"), body.formatted("blast_fungus"), StandardCharsets.UTF_8);
        List<String> warnings = new ArrayList<>();
        Logger log = quiet(warnings);
        EnchantRegistry r = enchants(log, new WeaponLoader(log).loadAll(new File(CONTENT, "weapons")), dir.toFile());
        assertTrue(r.find("on_flint").isEmpty(), "refused");
        assertTrue(r.find("on_fungus").isPresent(), "the control: on the Blast Fungus it loads");
        assertTrue(warnings.stream().anyMatch(m -> m.contains("on_flint") && m.contains("not a fused projectile")), warnings.toString());
    }

    /** G1 with two candidates: 500 seeded rolls, Fast and Fungal always first and locked; Cluster Bombs offered too. */
    @Test
    void slotZeroOffersFastAndFungalFirstAndClusterBombsBesideIt() {
        Logger log = quiet(new ArrayList<>());
        WeaponRegistry w = new WeaponLoader(log).loadAll(new File(CONTENT, "weapons"));
        EnchantRegistry e = enchants(log, w, new File(CONTENT, "enchants"));
        var roster = EnchantRollItems.roster(e.all());
        var cascade = new EnchantCandidatesLoader(log).load(new File(CONTENT, "enchant_candidates.yml"));
        WeaponDefinition fungus = w.find("blast_fungus").orElseThrow();
        var cls = GearItems.gearClassOf(fungus);
        assertEquals(List.of("cluster_bombs", "fast_and_fungal"), EnchantRoll.weaponPoolFor(fungus.id(),
                EnchantRollItems.has(fungus), cls, roster).stream().map(EnchantRoll.Rollable::enchantId).sorted().toList());
        Random random = new Random(527L);
        int cluster = 0;
        for (int i = 0; i < 500; i++) {
            EnchantState s = EnchantRoll.roll(fungus.id(), fungus.gearElement(), EnchantRollItems.has(fungus), cls, roster,
                    cascade, random::nextDouble);
            List<EnchantCandidate> slot0 = s.slots().get(0).candidates();
            assertEquals("fast_and_fungal", slot0.getFirst().enchantId(), "roll " + i);
            assertEquals(0, slot0.getFirst().level());
            assertEquals(List.of("fast_and_fungal", "cluster_bombs"), slot0.stream().map(EnchantCandidate::enchantId).toList(),
                    "roll " + i + ": READ, CandidateCascade.count caps every tier at the one candidate left, so it is always offered");
            cluster++;
        }
        assertEquals(500, cluster);
    }
}
