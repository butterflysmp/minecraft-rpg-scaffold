package io.github.butterflysmp.rpg.storage;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerBuildTest {

    private static final UUID PLAYER = UUID.fromString("0b1c2d3e-4f5a-4b6c-8d7e-9f0a1b2c3d4e");

    private static CellLoadout ranger() {
        return new CellLoadout("ranger", "fire", "ultimate_placeholder_ranger",
                List.of("rekindle", "solar_lance"), null, null);
    }

    /** THE PER-CELL KEY, which the "one loadout per player" mutation drops: saving Mage leaves Ranger. */
    @Test
    void eachCellKeepsItsOwnLoadout() {
        CellLoadout mage = new CellLoadout("mage", "fire", "ultimate_placeholder_mage",
                List.of("ember_step", "solar_grenade"), null, null);

        PlayerBuild build = PlayerBuild.empty(PLAYER).with(ranger()).with(mage);

        assertEquals("rekindle", build.loadout("ranger", "fire").orElseThrow().actives().get(0));
        assertEquals("ember_step", build.loadout("mage", "fire").orElseThrow().actives().get(0));
        assertTrue(build.loadout("mage", "void").isEmpty(), "a cell never saved has no loadout");
    }

    @Test
    void savingACellAgainReplacesOnlyThatCell() {
        PlayerBuild build = PlayerBuild.empty(PLAYER).with(ranger())
                .with(ranger().withActive(0, "another_active"));

        assertEquals(1, build.cells().size());
        assertEquals("another_active", build.loadout("ranger", "fire").orElseThrow().actives().get(0));
    }

    /** Two entries for one cell is STRUCTURAL: refused, never silently halved. */
    @Test
    void twoLoadoutsForOneCellAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerBuild(1, PLAYER, List.of(ranger(), ranger())));
    }

    @Test
    void theCellKeyIsTwoFieldsAndExact() {
        PlayerBuild build = PlayerBuild.empty(PLAYER).with(ranger());
        assertTrue(build.loadout("Ranger", "fire").isEmpty(), "nothing lowercases a cell id");
        assertTrue(build.loadout("ranger_f", "ire").isEmpty());
    }

    @Test
    void wrongListLengthsAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new CellLoadout("ranger", "fire", null, List.of("rekindle"), null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CellLoadout("ranger", "fire", null, null, List.of("a", "b", "c"), null));
        assertThrows(IllegalArgumentException.class,
                () -> new CellLoadout("ranger", "fire", null, null, null, List.of("a", "b")));
    }

    @Test
    void absentListsAreFixedLengthsOfEmptySlots() {
        CellLoadout empty = CellLoadout.empty("ranger", "fire");
        assertNull(empty.ultimate());
        assertEquals(Arrays.asList(null, null), empty.actives());
        assertEquals(Arrays.asList(null, null), empty.aspects());
        assertEquals(Arrays.asList(null, null, null, null), empty.fragments());
    }

    /** Ruling 11: one of each fragment. A hand-edited duplicate keeps the FIRST and blanks the rest. */
    @Test
    void aDuplicateFragmentKeepsTheFirstAndBlanksTheRest() {
        CellLoadout loadout = new CellLoadout("ranger", "fire", null, null, null,
                List.of("ember_heart", "keen_ember", "ember_heart", "ember_heart"));
        assertEquals(Arrays.asList("ember_heart", "keen_ember", null, null), loadout.fragments());
    }

    @Test
    void theSameAbilityInBothActivesKeepsTheFirst() {
        CellLoadout loadout = new CellLoadout("ranger", "fire", null, List.of("rekindle", "rekindle"), null, null);
        assertEquals(Arrays.asList("rekindle", null), loadout.actives());
    }

    @Test
    void aBlankIdReadsAsEmpty() {
        CellLoadout loadout = new CellLoadout("ranger", "fire", null, List.of(" ", "rekindle"), null, null);
        assertEquals(Arrays.asList(null, "rekindle"), loadout.actives());
    }
}
