package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E1-E3 (the seat, 2026-11-25), THE FOUR COLUMNS ON ARMOR.
 *
 * <p><b>READ, what armor did BEFORE this PR under {@code SLOTS = 4}:</b> slot 0 rolled by the piece's own id, which no
 * {@code weapon:} can name (the loader accepts only loaded weapons), so it was always empty; slot 1 by the piece's element,
 * which armor has none of, so always empty; slots 2 and 3 from {@code growth, mana_bank, protection, unbreaking}
 * ({@code UniqueSlotContentTest.MASTER_POOLS}). Both empty columns already showed the weapon's empty-column markers. So
 * what this PR changes is only slot 0's KEY (the armor slot's pool, so a piece-specific enchant has somewhere to be) and
 * the {@code armor_slot:} declaration; E2's "both start empty" holds because none ships.
 *
 * <p>E3, pinned for every shipped armor piece: slot 0 none, slot 1 none, normal the four.
 */
class ArmorPoolsTest {

    private static final Set<String> ELEMENTS = Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither");

    @TempDir
    Path dir;

    private static Logger quiet(List<String> into) {
        Logger log = Logger.getLogger("ArmorPoolsTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { into.add(r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    private static EnchantRegistry enchants(File enchantDir, List<String> warnings) {
        var weapons = new WeaponLoader(quiet(new ArrayList<>()))
                .loadAll(Path.of("src", "main", "resources", "content", "weapons").toFile());
        return EnchantLoader.withWeapons(quiet(warnings), ELEMENTS::contains, weapons::find, false).loadAll(enchantDir);
    }

    private static List<String> ids(List<EnchantRoll.Rollable> pool) {
        return pool.stream().map(EnchantRoll.Rollable::enchantId).sorted().toList();
    }

    @Test
    void everyShippedArmorPieceHasTwoEmptyColumnsAndTheFourNormals() {
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = enchants(Path.of("src", "main", "resources", "content", "enchants").toFile(), warnings);
        assertEquals(25, r.all().size(), "2026-11-26: + fast_and_fungal, 25 (ls content/enchants/*.yml | wc -l; 24 at fcfbefbb); CONTROL: the 24 shipped enchants loaded (2026-11-25: + sharpened_glass, 23 at 9a6ed020): " + warnings);
        var roster = EnchantRollItems.roster(r.all());
        var armor = new ArmorLoader(quiet(new ArrayList<>())).loadAll(new File("src/main/resources/content/armor"));
        int checked = 0;
        for (ArmorDefinition piece : armor.all()) {
            String key = EnchantRollItems.slotZeroKey(piece);
            assertEquals(EnchantRoll.armorSlotPool(piece.slot()), key, piece.id() + ": slot 0 rolls by its armor slot");
            assertEquals(List.of(), ids(EnchantRoll.weaponPoolFor(key, EnchantRoll.Has.NOTHING, GearClass.ARMOR, roster)),
                    piece.id() + ": slot 0, piece-specific: none ships (E2)");
            assertEquals(List.of(), ids(EnchantRoll.uniquePoolFor(piece.gearElement(), EnchantRoll.Has.NOTHING,
                    GearClass.ARMOR, roster)), piece.id() + ": slot 1, element-specific: armor has no element (E2)");
            assertEquals(List.of("growth", "mana_bank", "protection", "unbreaking"),
                    ids(EnchantRoll.normalPoolFor(EnchantRoll.Has.NOTHING, GearClass.ARMOR, roster)), piece.id());
            checked++;
        }
        assertEquals(28, checked, "the control: every shipped piece was asked");
    }

    private void write(String name, String yaml) throws IOException {
        Files.writeString(dir.resolve(name), yaml, StandardCharsets.UTF_8);
    }

    private static final String BODY = "display_name: \"Visor\"\nmax_level: 3\neffect: defense\nvalue_by_level: [1, 2, 3]\n";

    @Test
    void anArmorSlotEnchantLandsInSlotZeroOfThatSlotsPiecesOnly() throws IOException {
        write("visor.yml", BODY + "class: armor\narmor_slot: head\n");
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = enchants(dir.toFile(), warnings);
        assertEquals(1, r.all().size(), "it loads: " + warnings);
        var roster = EnchantRollItems.roster(r.all());
        for (ArmorSlot slot : ArmorSlot.values()) {
            List<String> pool = ids(EnchantRoll.weaponPoolFor(EnchantRoll.armorSlotPool(slot), EnchantRoll.Has.NOTHING,
                    GearClass.ARMOR, roster));
            assertEquals(slot == ArmorSlot.HEAD ? List.of("visor") : List.of(), pool, slot.name());
        }
        assertEquals(List.of(), ids(EnchantRoll.normalPoolFor(EnchantRoll.Has.NOTHING, GearClass.ARMOR, roster)),
                "and never in a normal slot");
        // Mutation: drop the roster's armor_slot mapping (pass definition.weapon()) -> it rolls as NORMAL -> reddens.
    }

    @Test
    void theLoaderRefusesArmorSlotWithWeaponElementOrANonArmorClassOrAnUnknownSlot() throws IOException {
        String[] bad = {
                BODY + "class: armor\narmor_slot: helmet\n",                       // unknown slot
                BODY + "class: universal\narmor_slot: head\n",                     // not armor
                "display_name: \"V\"\nmax_level: 3\neffect: damage\nvalue_by_level: [1, 2, 3]\nclass: melee\n"
                        + "armor_slot: head\n",                                     // a fighting class
                BODY + "class: armor\narmor_slot: head\nelement: fire\n",          // with element:
                BODY + "class: armor\narmor_slot: head\nweapon: boltor\n",          // with weapon: (see below)
        };
        for (String yaml : bad) {
            Path one = Files.createTempDirectory(dir, "bad");
            Files.writeString(one.resolve("v.yml"), yaml, StandardCharsets.UTF_8);
            List<String> warnings = new ArrayList<>();
            assertEquals(0, enchants(one.toFile(), warnings).all().size(), "refused: " + yaml.replace('\n', '|'));
            assertTrue(warnings.stream().anyMatch(w -> w.contains("Skipping malformed enchant")), warnings.toString());
        }
        // POSITIVE CONTROL: the good one beside them loads.
        Path good = Files.createTempDirectory(dir, "good");
        Files.writeString(good.resolve("v.yml"), BODY + "class: armor\narmor_slot: head\n", StandardCharsets.UTF_8);
        assertEquals(1, enchants(good.toFile(), new ArrayList<>()).all().size());
    }

    /**
     * THE armor_slot + weapon ARM IS UNREACHABLE FROM CONTENT, and this is its only exercise. Through the loader,
     * {@code weapon:}'s own check refuses first: a weapon's class is never armor, so {@code class: armor} + a weapon is
     * already "it could never roll", and any other class trips the armor-class arm. The record arm guards a direct caller.
     */
    @Test
    void theRecordRefusesArmorSlotWithAWeapon() {
        var thrown = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new EnchantDefinition("v", "V", 3, io.github.butterflysmp.rpg.core.enchant.EnchantEffect.DEFENSE,
                        GearClass.ARMOR, List.of(1, 2, 3), "iron_helmet", EnchantRoll.Requires.NONE, null, "boltor", null,
                        false, ArmorSlot.HEAD));
        assertTrue(thrown.getMessage().contains("armor_slot") && thrown.getMessage().contains("weapon"), thrown.getMessage());
        // Mutation: drop the record's armor_slot + weapon arm -> nothing thrown -> reddens.
    }
}
