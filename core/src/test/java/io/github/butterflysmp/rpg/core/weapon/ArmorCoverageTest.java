package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A2 (the seat, 2026-11-25): what can still go wrong once the authored Defense is the source. Every armor material
 * the server has (minus the excluded) has exactly one content piece, and every content piece names a material the
 * server has as armor FOR THE SLOT it is authored under.
 *
 * <p>The POSITIVE CONTROL is {@link #aCompleteRosterHasNoProblems} beside each fault row: a checker that always
 * answered "no problems" would pass it and fail every other row; one that always complained fails it.
 */
class ArmorCoverageTest {

    private static ArmorDefinition piece(String material, ArmorSlot slot) {
        return new ArmorDefinition(material, material, Rarity.COMMON, material, slot, 5, List.of());
    }

    private static Map<String, ArmorSlot> server() {
        Map<String, ArmorSlot> m = new LinkedHashMap<>();
        m.put("copper_helmet", ArmorSlot.HEAD);
        m.put("copper_boots", ArmorSlot.FEET);
        m.put("turtle_helmet", ArmorSlot.HEAD);
        return m;
    }

    private static final Set<String> NO_TURTLE = Set.of("turtle_helmet");

    @Test
    void aCompleteRosterHasNoProblems() {
        assertEquals(List.of(), ArmorCoverage.problems(server(),
                List.of(piece("copper_helmet", ArmorSlot.HEAD), piece("copper_boots", ArmorSlot.FEET)), NO_TURTLE));
    }

    @Test
    void aServerArmorMaterialWithNoContentPieceIsNamed() {
        List<String> p = ArmorCoverage.problems(server(), List.of(piece("copper_helmet", ArmorSlot.HEAD)), NO_TURTLE);
        assertEquals(1, p.size(), p.toString());
        assertTrue(p.get(0).contains("copper_boots"), p.get(0));
        // Mutation: skip the server walk -> [] -> reddens.
    }

    @Test
    void theExcludedMaterialNeedsNoPieceButMayNotHaveOne() {
        List<String> p = ArmorCoverage.problems(server(), List.of(piece("copper_helmet", ArmorSlot.HEAD),
                piece("copper_boots", ArmorSlot.FEET), piece("turtle_helmet", ArmorSlot.HEAD)), NO_TURTLE);
        assertEquals(1, p.size(), p.toString());
        assertTrue(p.get(0).contains("turtle_helmet") && p.get(0).contains("excluded"), p.get(0));
        // And without the exclusion the same server would name the turtle helmet as uncovered:
        List<String> q = ArmorCoverage.problems(server(), List.of(piece("copper_helmet", ArmorSlot.HEAD),
                piece("copper_boots", ArmorSlot.FEET)), Set.of());
        assertEquals(1, q.size(), q.toString());
        assertTrue(q.get(0).contains("turtle_helmet"), q.get(0));
    }

    @Test
    void aPieceNamingAMaterialTheServerDoesNotHaveIsNamed() {
        List<String> p = ArmorCoverage.problems(server(), List.of(piece("copper_helmet", ArmorSlot.HEAD),
                piece("copper_boots", ArmorSlot.FEET), piece("gold_helmet", ArmorSlot.HEAD)), NO_TURTLE);
        assertEquals(1, p.size(), p.toString());
        assertTrue(p.get(0).contains("gold_helmet"), p.get(0));
    }

    @Test
    void aPieceAuthoredUnderTheWrongSlotIsNamed() {
        List<String> p = ArmorCoverage.problems(server(), List.of(piece("copper_helmet", ArmorSlot.HEAD),
                piece("copper_boots", ArmorSlot.LEGS)), NO_TURTLE);
        assertEquals(1, p.size(), p.toString());
        assertTrue(p.get(0).contains("copper_boots") && p.get(0).contains("FEET") && p.get(0).contains("LEGS"),
                p.get(0));
        // Mutation: drop the slot comparison -> [] -> reddens.
    }
}
