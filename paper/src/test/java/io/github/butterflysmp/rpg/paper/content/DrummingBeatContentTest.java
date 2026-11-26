package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.CandidateCascade;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantCandidate;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.weapon.DrummingBeatHits;
import io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine;
import io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M3, M9 and M10 (the seat's brief, 2026-11-26): Drumming Beat as shipped, and the Bone Club's three pools. Ben: "2.
 * Drumming beat, every consecutive hit adds 3/4/5 critical chance. Same conditions" / "5. Yes guaranteed enchant".
 * Mutations named per test.
 */
class DrummingBeatContentTest {

    private static final Set<String> ELEMENTS = Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither");

    private static Logger capturing(List<String> into) {
        Logger log = Logger.getLogger("DrummingBeatContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { into.add(r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    private static WeaponRegistry weapons() {
        var weapons = new WeaponLoader(capturing(new ArrayList<>()))
                .loadAll(Path.of("src", "main", "resources", "content", "weapons").toFile());
        assertTrue(weapons.find("bone_club").isPresent(), "CONTROL: the club loaded");
        return weapons;
    }

    private static EnchantRegistry shipped(List<String> warnings) {
        File dir = Path.of("src", "main", "resources", "content", "enchants").toFile();
        return EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, weapons()::find, false).loadAll(dir);
    }

    /** Ben's name and numbers; weapon-specific to the club; guaranteed; the new behaviour; no warning. */
    @Test
    void itLoadsAsBenGaveIt() {
        List<String> warnings = new ArrayList<>();
        EnchantDefinition d = shipped(warnings).find("drumming_beat")
                .orElseThrow(() -> new AssertionError("drumming_beat did not load: " + warnings));
        assertEquals(List.of(), warnings.stream().filter(w -> w.contains("drumming_beat")).toList());
        assertEquals("Drumming Beat", d.displayName());
        assertEquals(EnchantEffect.DRUMMING_BEAT, d.effect());
        assertEquals("bone_club", d.weapon());
        assertTrue(d.guaranteed());
        assertEquals(3, d.maxLevel());
        assertEquals(List.of(), d.valueByLevel(), "a behaviour: no top-level curve");
        EnchantBehaviour.DrummingBeat b = assertInstanceOf(EnchantBehaviour.DrummingBeat.class, d.behaviour());
        assertEquals(new EnchantBehaviour.DrummingBeat("bone_club", 100, List.of(3.0, 4.0, 5.0),
                "block.note_block.basedrum", "block.bone_block.break"), b);
    }

    /** The loader refuses a drumming_beat that names no weapon, an unknown one, or another than the enchant's. */
    @Test
    void theLoaderRefusesAStreakWithoutItsWeapon() throws IOException {
        Path dir = Files.createTempDirectory("drum");
        String base = "display_name: \"X\"\nmax_level: 3\neffect: drumming_beat\nclass: universal\n";
        String block = "drumming_beat:\n  gap_ticks: 100\n  step_percent_by_level: [3, 4, 5]\n";
        Files.writeString(dir.resolve("a.yml"), base + block);                                  // no weapon:
        Files.writeString(dir.resolve("b.yml"), base + "weapon: no_such_weapon\n" + block);    // unknown weapon
        Files.writeString(dir.resolve("c.yml"), base + "weapon: bone_club\n" + block);         // CONTROL: loads
        Files.writeString(dir.resolve("d.yml"), base + "weapon: bone_club\ndrumming_beat:\n  gap_ticks: 0\n"
                + "  step_percent_by_level: [3, 4, 5]\n");                                      // gap below 1
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, weapons()::find, false)
                .loadAll(dir.toFile());
        assertTrue(r.find("c").isPresent(), "CONTROL: the well-formed file loads: " + warnings);
        assertTrue(r.find("a").isEmpty() && r.find("b").isEmpty() && r.find("d").isEmpty(), "three refused: " + warnings);
        assertTrue(warnings.stream().anyMatch(w -> w.contains("drumming_beat: counts one weapon's hits")), warnings.toString());
        // READ (the first run of this test): an unknown weapon: is refused by the loader's general weapon check, before the
        // block is read, so this file never reaches drumming_beat's own "is not a weapon this loader can see".
        assertTrue(warnings.stream().anyMatch(w -> w.contains("declares weapon: 'no_such_weapon', which is not a loaded weapon")),
                warnings.toString());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("gap_ticks must be 1 or more")), warnings.toString());
    }

    /** M9 (B5): the line carries the REAL step. Mutation: the arm prints the level, or a fraction. */
    @Test
    void itsLineIsTheRealNumber() {
        EnchantDefinition d = shipped(new ArrayList<>()).find("drumming_beat").orElseThrow();
        assertEquals("+3% Crit Chance per consecutive hit", EnchantEffectLine.bare(d, 1, GearClass.MELEE));
        assertEquals("+4% Crit Chance per consecutive hit", EnchantEffectLine.bare(d, 2, GearClass.MELEE));
        assertEquals("+5% Crit Chance per consecutive hit", EnchantEffectLine.bare(d, 3, GearClass.MELEE));
    }

    /** The bare club's hits carry the trace (M8): the registry names it; an iron sword is not named (the control). */
    @Test
    void theRegistryNamesTheClubAndNoOtherWeapon() {
        EnchantRegistry r = shipped(new ArrayList<>());
        assertTrue(DrummingBeatHits.drums("bone_club", r));
        for (WeaponDefinition w : weapons().all()) {
            assertEquals(w.id().equals("bone_club"), DrummingBeatHits.drums(w.id(), r), w.id());
        }
    }

    private static List<String> ids(List<EnchantRoll.Rollable> pool) {
        return pool.stream().map(EnchantRoll.Rollable::enchantId).sorted().toList();
    }

    /**
     * M10, THE POOLS: slot 0 is Drumming Beat, slot 1 the kinetic unique (Kinetic Tremors), the normal pool a melee
     * weapon's (Sharpness, Unbreaking), as the bottle's. An iron sword's slot 0 is empty and holds it in no pool.
     */
    @Test
    void theClubsPoolsAndTheSwordsControl() {
        EnchantRegistry r = shipped(new ArrayList<>());
        var roster = EnchantRollItems.roster(r.all());
        assertTrue(roster.size() >= 20, "CONTROL: the roster loaded, " + roster.size());
        WeaponRegistry w = weapons();
        WeaponDefinition club = w.find("bone_club").orElseThrow();
        WeaponDefinition sword = w.find("iron_sword").orElseThrow();
        GearClass melee = GearClass.of(club.weaponClass());
        assertEquals(List.of("drumming_beat"),
                ids(EnchantRoll.weaponPoolFor(club.id(), EnchantRollItems.has(club), melee, roster)));
        assertEquals(List.of("kinetic_tremors"),
                ids(EnchantRoll.uniquePoolFor(club.gearElement(), EnchantRollItems.has(club), melee, roster)));
        assertEquals(List.of("sharpness", "unbreaking"),
                ids(EnchantRoll.normalPoolFor(EnchantRollItems.has(club), melee, roster)));

        assertEquals(List.of(), ids(EnchantRoll.weaponPoolFor(sword.id(), EnchantRollItems.has(sword), melee, roster)));
        assertFalse(ids(EnchantRoll.normalPoolFor(EnchantRollItems.has(sword), melee, roster)).contains("drumming_beat"));
    }

    /** G1 over many rolls: every fresh club offers it in slot 0, FIRST, locked; no sword ever does. */
    @Test
    void slotZeroOverManyRolls() {
        var roster = EnchantRollItems.roster(shipped(new ArrayList<>()).all());
        var cascade = new CandidateCascade(List.of(new CandidateCascade.Tier(4, 0.10), new CandidateCascade.Tier(3, 0.40),
                new CandidateCascade.Tier(2, 0.70), new CandidateCascade.Tier(1, 1.0)));
        WeaponRegistry w = weapons();
        Random random = new Random(20261126L);
        for (String id : List.of("bone_club", "iron_sword")) {
            WeaponDefinition d = w.find(id).orElseThrow();
            for (int n = 0; n < 500; n++) {
                EnchantState s = EnchantRoll.roll(d.id(), d.gearElement(), EnchantRollItems.has(d),
                        GearClass.of(d.weaponClass()), roster, cascade, random::nextDouble);
                var slot0 = s.slots().get(0).candidates().stream().map(EnchantCandidate::enchantId).toList();
                assertEquals(id.equals("bone_club") ? List.of("drumming_beat") : List.of(), slot0, id + " roll " + n);
                assertTrue(s.slots().get(0).candidates().stream().allMatch(c -> c.level() == 0), "locked, not pre-applied");
                if (id.equals("iron_sword")) {
                    for (var slot : s.slots()) {
                        assertTrue(slot.candidates().stream().noneMatch(c -> c.enchantId().equals("drumming_beat")));
                    }
                }
            }
        }
    }
}
