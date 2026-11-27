package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorLoreLines;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.paper.content.ArmorLoader;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.content.EnchantLoader;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import io.github.butterflysmp.rpg.paper.content.WeaponLoader;
import io.github.butterflysmp.rpg.paper.hud.StatsBarText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MANA BATTERY (the seat's B1 to B6, 2026-11-27; Ben: "1/2/3 per 5 seconds", then "B"). The shipped file
 * as loaded, the gate that holds MANA_REGEN to armor, what a piece grants, its tooltip line, and armor's five normals.
 */
class ManaBatteryTest {

    private static final Set<String> ELEMENTS = Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither");

    @TempDir
    Path dir;

    private static Logger quiet(List<String> into) {
        Logger log = Logger.getLogger("ManaBatteryTest-" + System.nanoTime());
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

    private static EnchantRegistry shipped() {
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = enchants(Path.of("src", "main", "resources", "content", "enchants").toFile(), warnings);
        assertTrue(r.find("mana_battery").isPresent(), "the shipped file loads: " + warnings);
        return r;
    }

    private static ArmorDefinition piece(String id) {
        return new ArmorLoader(quiet(new ArrayList<>())).loadAll(new File("src/main/resources/content/armor"))
                .find(id).orElseThrow();
    }

    /** B1: every field of the shipped file, as the loader reads it. */
    @Test
    void theShippedFile_isAnArmorNormalEnchantOfOneTwoThree() {
        EnchantDefinition d = shipped().find("mana_battery").orElseThrow();
        assertEquals("Mana Battery", d.displayName());
        assertEquals(3, d.maxLevel());
        assertEquals(EnchantEffect.MANA_REGEN, d.effect());
        assertEquals(GearClass.ARMOR, d.gearClass());
        assertEquals(List.of(1, 2, 3), d.valueByLevel(), "Ben's numbers, in mana per FIVE SECONDS");
        assertEquals("redstone_block", d.icon(), "SEAT PLACEHOLDER 2026-11-27");
        assertEquals(null, d.armorSlot(), "no armor_slot: a NORMAL slot of every piece");
        assertFalse(d.isUnique());
        assertFalse(d.isWeaponSpecific());
        assertFalse(d.guaranteed());
        assertFalse(d.isBehavioural());
    }

    /**
     * B2: MANA_REGEN is ARMOR_ONLY, for the reason DEFENSE, MAX_HEALTH and MAX_MANA are. Universal and a fighting class are
     * refused by name; armor loads (the control). A file with no value_by_level is refused (it is curved).
     */
    @Test
    void manaRegen_isHeldToArmor() throws IOException {
        String body = "display_name: \"B\"\nmax_level: 3\neffect: mana_regen\nvalue_by_level: [1, 2, 3]\n";
        String[] bad = {body + "class: universal\n", body + "class: mage\n", body + "class: shield\n",
                "display_name: \"B\"\nmax_level: 3\neffect: mana_regen\nclass: armor\n"};
        for (String yaml : bad) {
            Path one = Files.createTempDirectory(dir, "bad");
            Files.writeString(one.resolve("b.yml"), yaml, StandardCharsets.UTF_8);
            List<String> warnings = new ArrayList<>();
            assertEquals(0, enchants(one.toFile(), warnings).all().size(), "refused: " + yaml.replace('\n', '|'));
            assertTrue(warnings.stream().anyMatch(w -> w.contains("Skipping malformed enchant")), warnings.toString());
        }
        Path good = Files.createTempDirectory(dir, "good");
        Files.writeString(good.resolve("b.yml"), body + "class: armor\n", StandardCharsets.UTF_8);
        assertEquals(1, enchants(good.toFile(), new ArrayList<>()).all().size(), "the control: armor loads");
        // Mutation: MANA_REGEN -> Gate.MAIN_HAND_ONLY -> universal and mage load -> reddens.
    }

    /** B2: a piece grants its level's value, by EFFECT; two distinct effects on one state do not cross. */
    @Test
    void aPieceGrantsItsLevelsValue_inManaPerSecond() {
        EnchantRegistry r = shipped();
        for (int level = 1; level <= 3; level++) {
            EnchantState state = TrueTooltipsConsistencyTest.activeAt("mana_battery", level);
            assertEquals(level, EnchantValues.totalFor(state, r, EnchantEffect.MANA_REGEN), 0.0, "level " + level);
            assertEquals(0.0, EnchantValues.totalFor(state, r, EnchantEffect.MAX_MANA), 0.0, "never Mana Bank's");
        }
        EnchantState bank = TrueTooltipsConsistencyTest.activeAt("mana_bank", 3);
        assertEquals(0.0, EnchantValues.totalFor(bank, r, EnchantEffect.MANA_REGEN), 0.0, "and Mana Bank grants no regen");
    }

    private static List<String> lore(ArmorDefinition a, EnchantState s, EnchantRegistry r, int score) {
        return TrueTooltipsConsistencyTest.plain(ArmorItems.statLore(a, s, r, OptionalInt.of(score)));
    }

    /**
     * B5: the stat block gains ONE line under Defense, "Mana Regen: +N/5s" (U1, 2026-11-27), as Mana Bank's "Mana: +N"; bare, none. Wording a
     * SEAT PLACEHOLDER 2026-11-27. And the colour is the mana colour, as Mana Bank's.
     */
    @Test
    void theTooltip_addsOneManaRegenLine_asManaBankAddsItsManaLine() {
        EnchantRegistry r = shipped();
        ArmorDefinition helmet = piece("iron_helmet");
        List<String> bare = lore(helmet, EnchantState.empty(), r, 100);
        assertTrue(bare.stream().noneMatch(l -> l.startsWith("Mana Regen")), bare.toString());
        for (int level = 1; level <= 3; level++) {
            List<String> with = lore(helmet, TrueTooltipsConsistencyTest.activeAt("mana_battery", level), r, 100);
            List<String> expected = new ArrayList<>(bare);
            int defense = bare.indexOf(bare.stream().filter(l -> l.startsWith("Defense: ")).findFirst().orElseThrow());
            expected.add(defense + 1, "Mana Regen: +" + level + "/5s");
            assertEquals(expected, with, "level " + level + ": bare plus one line under Defense");
        }
        List<Component> rich = ArmorItems.statLore(helmet, TrueTooltipsConsistencyTest.activeAt("mana_battery", 3), r,
                OptionalInt.of(100));
        Component line = rich.stream().filter(c -> TrueTooltipsConsistencyTest.plain(List.of(c)).getFirst()
                .startsWith("Mana Regen")).findFirst().orElseThrow();
        assertEquals(StatsBarText.MANA_COLOR, ((TextComponent) line.children().getLast()).color(), "the value, mana blue");
        assertEquals("Mana Regen", ArmorLoreLines.MANA_REGEN_LABEL);
    }

    /** B4: "curved" is a value_by_level; the piece's score never moves an enchant's number, Mana Bank's or this one's. */
    @Test
    void theValueDoesNotMoveWithTheScore() {
        EnchantRegistry r = shipped();
        ArmorDefinition helmet = piece("iron_helmet");
        for (int score : new int[] {100, 200}) {
            List<String> battery = lore(helmet, TrueTooltipsConsistencyTest.activeAt("mana_battery", 3), r, score);
            List<String> bank = lore(helmet, TrueTooltipsConsistencyTest.activeAt("mana_bank", 3), r, score);
            assertTrue(battery.contains("Mana Regen: +3/5s"), score + ": " + battery);
            assertTrue(bank.contains("Mana: +30"), score + ": " + bank);
        }
        // And the scan's sum has no score input at all: EnchantValues.totalFor(state, registry, effect).
    }

    /** B5: the effect suffix (the enchant table, /rpg enchant show), "(+N Mana Regen/5s)" since U1, 2026-11-27; inert off armor. */
    @Test
    void theEffectSuffix_readsPlusNManaRegen() {
        EnchantDefinition d = shipped().find("mana_battery").orElseThrow();
        assertEquals(" (+1 Mana Regen/5s)", EnchantEffectLine.of(d, 1, GearClass.ARMOR));
        assertEquals(" (+2 Mana Regen/5s)", EnchantEffectLine.of(d, 2, GearClass.ARMOR));
        assertEquals(" (+3 Mana Regen/5s)", EnchantEffectLine.of(d, 3, GearClass.ARMOR));
        assertTrue(EnchantEffectLine.of(d, 3, GearClass.MAGE).contains("inert"), "the control: off armor it says inert");
    }

    /** B6: armor's normal pool is FIVE, mana_battery among them; slots 0 and 1 stay empty, for every shipped piece. */
    @Test
    void armorsNormalPool_isFive_onEveryPiece() {
        EnchantRegistry r = shipped();
        var roster = EnchantRollItems.roster(r.all());
        var armor = new ArmorLoader(quiet(new ArrayList<>())).loadAll(new File("src/main/resources/content/armor"));
        int checked = 0;
        for (ArmorDefinition a : armor.all()) {
            assertEquals(List.of("growth", "mana_bank", "mana_battery", "protection", "unbreaking"),
                    EnchantRoll.normalPoolFor(EnchantRoll.Has.NOTHING, GearClass.ARMOR, roster).stream()
                            .map(EnchantRoll.Rollable::enchantId).toList(), a.id() + ", in roster (file-name) order");
            assertEquals(List.of(), EnchantRoll.weaponPoolFor(EnchantRollItems.slotZeroKey(a), EnchantRoll.Has.NOTHING,
                    GearClass.ARMOR, roster), a.id() + ": slot 0");
            assertEquals(List.of(), EnchantRoll.uniquePoolFor(a.gearElement(), EnchantRoll.Has.NOTHING, GearClass.ARMOR,
                    roster), a.id() + ": slot 1");
            checked++;
        }
        assertEquals(28, checked, "the control: every shipped piece was asked");
        assertFalse(EnchantRoll.normalPoolFor(EnchantRoll.Has.NOTHING, GearClass.MELEE, roster).stream()
                .anyMatch(x -> x.enchantId().equals("mana_battery")), "and never in a weapon's pool");
    }
}
