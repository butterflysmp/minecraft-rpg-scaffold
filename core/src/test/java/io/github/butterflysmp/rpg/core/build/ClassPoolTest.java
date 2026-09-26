package io.github.butterflysmp.rpg.core.build;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAN-build-system.md section 7.1: a class file's ids are merged into each of its cells at load. */
class ClassPoolTest {

    private static final ClassPool RANGER = new ClassPool("ranger", List.of(), List.of("recall"));

    /** The cell's own ids first, in their order, then the class's: the picker's display order. */
    @Test
    void theClassIdsFollowTheCellsOwn() {
        assertEquals(List.of("solar_lance", "recall"), RANGER.mergeActives(List.of("solar_lance")));
        assertEquals(List.of("ultimate_placeholder_ranger"),
                RANGER.mergeUltimates(List.of("ultimate_placeholder_ranger")));
    }

    /**
     * THE CLASS MERGE's row: without it a cell offers nothing class-wide, and ruling 24 ("any Ranger can pick
     * it") is false. The shipped ranger_fire pool then names a default nothing offers and is refused.
     */
    @Test
    void aCellWithNoActivesOfItsOwnStillGetsTheClassOnes() {
        assertEquals(List.of("recall"), RANGER.mergeActives(List.of()));
    }

    /** Two homes for one fact are refused, not de-duplicated: removing it from the class file must not leave it. */
    @Test
    void anIdBothClassWideAndCellListedIsRefusedNamingIt() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> RANGER.mergeActives(List.of("recall", "solar_lance")));
        assertTrue(ex.getMessage().contains("recall"), ex.getMessage());
        assertTrue(ex.getMessage().contains("ranger.yml"), ex.getMessage());
    }

    /** Across roles, the refusal is PoolDefinition's own "both an ultimate and an active", on the merged lists. */
    @Test
    void anIdClassWideAsActiveAndCellListedAsUltimateIsRefusedByThePool() {
        List<String> ultimates = RANGER.mergeUltimates(List.of("recall"));
        List<String> actives = RANGER.mergeActives(List.of("solar_lance"));
        assertThrows(IllegalArgumentException.class, () -> new PoolDefinition(new CellKey("ranger", "fire"), "x",
                ultimates, actives, new Loadout("recall", "solar_lance", "recall")));
    }

    @Test
    void aClassFileItselfMayNotListAnIdTwiceOrInBothRoles() {
        assertThrows(IllegalArgumentException.class,
                () -> new ClassPool("ranger", List.of(), List.of("recall", "recall")));
        assertThrows(IllegalArgumentException.class,
                () -> new ClassPool("ranger", List.of("recall"), List.of("recall")));
    }

    @Test
    void aClassFileNeedsAClassAndOffersSomething() {
        assertThrows(IllegalArgumentException.class, () -> new ClassPool(" ", List.of(), List.of("recall")));
        assertThrows(IllegalArgumentException.class, () -> new ClassPool("ranger", List.of(), List.of()));
    }
}
