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
 * FAST AND FUNGAL AS SHIPPED (the seat's F7, 2026-11-26; Ben: "the guaranteed enchant is Fast and Fungal, reduces
 * cooldown by 1/2/3s"): the Blast Fungus's, guaranteed (G1: first in slot 0, only there, locked at level 0), a cooldown
 * cut of 20 / 40 / 60 ticks; the REAL loader refuses a cut that leaves 0; and the press hands the stamp the behaviours it
 * read at the press (B2).
 */
class FastAndFungalContentTest {

    private static final File CONTENT = Path.of("src", "main", "resources", "content").toFile();

    private static Logger quiet(List<String> warnings) {
        Logger log = Logger.getLogger("FastAndFungalContentTest-" + System.nanoTime());
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

    private static WeaponRegistry weapons(Logger log) {
        return new WeaponLoader(log).loadAll(new File(CONTENT, "weapons"));
    }

    private static EnchantRegistry enchants(Logger log, WeaponRegistry weapons, File dir) {
        var elements = new ElementLoader(log).loadAll(new File(CONTENT, "elements"));
        return EnchantLoader.withWeapons(log, id -> elements.find(id).isPresent(), weapons::find, false).loadAll(dir);
    }

    @Test
    void itShipsAsBenRuledIt() {
        List<String> warnings = new ArrayList<>();
        Logger log = quiet(warnings);
        EnchantDefinition f = enchants(log, weapons(log), new File(CONTENT, "enchants")).find("fast_and_fungal").orElseThrow(
                () -> new AssertionError("fast_and_fungal did not load: " + warnings));
        assertEquals(List.of(), warnings.stream().filter(w -> w.contains("fast_and_fungal")).toList());
        assertEquals("Fast and Fungal", f.displayName());
        assertEquals("blast_fungus", f.weapon());
        assertTrue(f.guaranteed());
        assertEquals(3, f.maxLevel());
        var tune = assertInstanceOf(EnchantBehaviour.CastTune.class, f.behaviour());
        assertEquals(EnchantBehaviour.CastTune.Quantity.COOLDOWN, tune.quantity());
        assertEquals(List.of(20.0, 40.0, 60.0), tune.valueByLevel(), "BEN: 1 / 2 / 3 s");
    }

    /** F7: the loader builds every level and REFUSES, by name, a cut that leaves the cooldown at 0 (here 140 - 140). */
    @Test
    void theRealLoaderRefusesACutToZero(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("too_fungal.yml"), String.join("\n",
                "display_name: \"Too Fungal\"", "max_level: 3", "effect: cast_tune", "class: universal",
                "weapon: blast_fungus", "cast_tune:", "  quantity: cooldown", "  value_by_level: [20, 40, 140]", ""),
                StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("fine_fungal.yml"), String.join("\n",
                "display_name: \"Fine Fungal\"", "max_level: 3", "effect: cast_tune", "class: universal",
                "weapon: blast_fungus", "cast_tune:", "  quantity: cooldown", "  value_by_level: [20, 40, 139]", ""),
                StandardCharsets.UTF_8);
        List<String> warnings = new ArrayList<>();
        Logger log = quiet(warnings);
        EnchantRegistry r = enchants(log, weapons(log), dir.toFile());
        assertTrue(r.find("too_fungal").isEmpty(), "refused");
        assertTrue(r.find("fine_fungal").isPresent(), "the control: 1 tick left loads");
        assertTrue(warnings.stream().anyMatch(w -> w.contains("too_fungal") && w.contains("blast_fungus/right_click")),
                "by name: " + warnings);
    }

    /** G1: over 500 seeded rolls, slot 0 of every fresh Blast Fungus offers it FIRST, locked at level 0; no other gear's. */
    @Test
    void itIsOfferedFirstInSlotZeroOfTheBlastFungusOnly() {
        Logger log = quiet(new ArrayList<>());
        WeaponRegistry w = weapons(log);
        EnchantRegistry e = enchants(log, w, new File(CONTENT, "enchants"));
        var roster = EnchantRollItems.roster(e.all());
        var cascade = new EnchantCandidatesLoader(log).load(new File(CONTENT, "enchant_candidates.yml"));
        WeaponDefinition fungus = w.find("blast_fungus").orElseThrow();
        var cls = GearItems.gearClassOf(fungus);
        Random random = new Random(526L);
        for (int i = 0; i < 500; i++) {
            EnchantState s = EnchantRoll.roll(fungus.id(), fungus.gearElement(), EnchantRollItems.has(fungus), cls, roster,
                    cascade, random::nextDouble);
            EnchantCandidate first = s.slots().get(0).candidates().getFirst();
            assertEquals("fast_and_fungal", first.enchantId(), "roll " + i);
            assertEquals(0, first.level(), "locked, not pre-applied (G2)");
        }
        for (WeaponDefinition other : w.all()) {
            if (other.id().equals("blast_fungus")) continue;
            var pool = EnchantRoll.weaponPoolFor(other.id(), EnchantRollItems.has(other), GearItems.gearClassOf(other), roster);
            assertTrue(pool.stream().noneMatch(p -> p.enchantId().equals("fast_and_fungal")), other.id());
        }
    }

    /** B2: the press reads the held weapon's behaviours ONCE and hands that list to the resolver and to every charge. */
    @Test
    void thePressHandsTheStampTheBehavioursItRead() throws IOException {
        String f = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/weapon/WeaponFire.java"))
                .replace("\r", "");
        assertTrue(f.contains("pressBehaviours =\n                new io.github.butterflysmp.rpg.paper.adapter.PaperCombatWorld("
                + "player.getWorld(), adapters)\n                        .behavioursOf(player.getUniqueId());"), "read at the press");
        assertEquals(3, f.split("weaponService\\.fire\\(caster, binding\\.get\\(\\), aim, pressBehaviours\\)", -1).length - 1,
                "the three charges, each with them");
        assertEquals(0, f.split("weaponService\\.fire\\(caster, binding\\.get\\(\\), aim\\)", -1).length - 1,
                "no charge without them");
    }
}
