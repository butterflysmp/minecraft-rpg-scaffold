package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine;
import io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems;
import io.github.butterflysmp.rpg.paper.weapon.GearItems;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MANA BURN AS SHIPPED (F3, the seat's ruling of 2026-11-24 on PR F): Ben's numbers, its pool on each fire mage weapon and
 * on no other gear, its line, and a malformed block refused by name.
 */
class ManaBurnContentTest {

    private static final Set<String> ELEMENTS = Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither");
    private static final List<String> FIRE_MAGE = List.of("blaze_kings_staff", "flint_staff", "living_lantern");

    private static Logger capturing(List<String> into) {
        Logger log = Logger.getLogger("ManaBurnContentTest-" + System.nanoTime());
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
        assertTrue(weapons.all().size() >= 20, "CONTROL: the weapon walk is not blind");
        return weapons;
    }

    private static EnchantRegistry shipped() {
        File dir = Path.of("src", "main", "resources", "content", "enchants").toFile();
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, weapons()::find, false)
                .loadAll(dir);
        assertTrue(r.find("mana_burn").isPresent(), "mana_burn did not load: " + warnings);
        assertEquals(List.of(), warnings, "the shipped enchants load with no warning");
        return r;
    }

    @Test
    void bensNameAndNumbers() {
        EnchantDefinition m = shipped().find("mana_burn").orElseThrow();
        assertEquals("Mana Burn", m.displayName());
        assertEquals(EnchantEffect.SCORCH_BOOST, m.effect());
        assertEquals(3, m.maxLevel());
        assertEquals(new EnchantBehaviour.ScorchBoost(List.of(10.0, 20.0, 30.0)), m.behaviour(), "Ben: \"by 10/20/30%\"");
        assertEquals("fire", m.element(), "UNIQUE: slot 1");
        assertEquals(GearClass.MAGE, m.gearClass(), "the brief: class mage");
        assertNull(m.weapon(), "no weapon: it is not a weapon-slot enchant");
        assertFalse(m.guaranteed(), "NOT guaranteed");
        assertEquals("fire_charge", m.icon(), "SEAT PLACEHOLDER");
    }

    /**
     * ITS POOL ON EACH, AND ON NO OTHER GEAR. Slot 1 (the unique pool) of the three fire mage weapons is exactly
     * {@code [mana_burn]}; Dragon's Breath (fire, ranger) is the control that the CLASS filter bites; no other weapon's
     * unique pool, and no gear's weapon or normal pool, holds it.
     */
    @Test
    void itsPoolIsTheThreeFireMageWeaponsAndNoOtherGear() {
        var roster = EnchantRollItems.roster(shipped().all());
        List<String> holders = new ArrayList<>();
        int asked = 0;
        for (WeaponDefinition w : weapons().all()) {
            var cls = GearItems.gearClassOf(w);
            if (cls == null) continue;
            asked++;
            var unique = EnchantRoll.uniquePoolFor(w.gearElement(), EnchantRollItems.has(w), cls, roster).stream()
                    .map(EnchantRoll.Rollable::enchantId).toList();
            if (unique.contains("mana_burn")) holders.add(w.id());
            if (FIRE_MAGE.contains(w.id())) assertEquals(List.of("mana_burn"), unique, w.id());
            assertFalse(EnchantRoll.normalPoolFor(EnchantRollItems.has(w), cls, roster).stream()
                    .anyMatch(r -> r.enchantId().equals("mana_burn")), w.id() + " normal pool");
            assertFalse(EnchantRoll.weaponPoolFor(w.id(), EnchantRollItems.has(w), cls, roster).stream()
                    .anyMatch(r -> r.enchantId().equals("mana_burn")), w.id() + " weapon pool");
        }
        assertTrue(asked >= 20, "CONTROL: every weapon was asked, " + asked);
        assertEquals(FIRE_MAGE, holders.stream().sorted().toList());
        var breath = weapons().find("dragons_breath").orElseThrow();
        assertEquals(java.util.Optional.of("fire"), breath.gearElement(), "the control is fire");
        assertEquals(List.of(), EnchantRoll.uniquePoolFor(breath.gearElement(), EnchantRollItems.has(breath),
                GearItems.gearClassOf(breath), roster), "and ranger, so the class filter refuses it");
    }

    /** Its line (WORDING: SEAT PLACEHOLDER 2026-11-24, OPEN FOR BEN). */
    @Test
    void itsLine() {
        EnchantDefinition m = shipped().find("mana_burn").orElseThrow();
        for (int level = 1; level <= 3; level++) {
            assertEquals("+" + (10 * level) + "% Scorch and Ignite damage",
                    EnchantEffectLine.bare(m, level, GearClass.MAGE), "level " + level);
        }
    }

    @Test
    void aMalformedBoostIsRefusedByName(@TempDir Path dir) throws IOException {
        String head = "display_name: \"M\"\nmax_level: 3\neffect: scorch_boost\nclass: mage\nelement: fire\n";
        String good = "scorch_boost:\n  percent_by_level: [10, 20, 30]\n";
        Files.writeString(dir.resolve("good.yml"), head + good);
        Files.writeString(dir.resolve("no_block.yml"), head);
        Files.writeString(dir.resolve("no_levels.yml"), head + "scorch_boost:\n  other: 1\n");
        Files.writeString(dir.resolve("zero.yml"), head + good.replace("[10, 20, 30]", "[10, 0, 30]"));
        Files.writeString(dir.resolve("two_levels.yml"), head + good.replace("[10, 20, 30]", "[10, 20]"));
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, weapons()::find, false)
                .loadAll(dir.toFile());
        assertTrue(r.find("good").isPresent(), "the control: " + warnings);
        for (String bad : List.of("no_block", "no_levels", "zero", "two_levels")) {
            assertTrue(r.find(bad).isEmpty(), bad + " loaded: " + warnings);
            assertTrue(warnings.stream().anyMatch(w -> w.contains(bad)), bad + " not named: " + warnings);
        }
        assertTrue(warnings.stream().anyMatch(w -> w.contains("percent_by_level at level 2 must be above 0")),
                warnings.toString());
    }
}
