package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.combat.stat.HealthState;
import io.github.butterflysmp.rpg.core.combat.stat.ModifierReconciler;
import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.core.weapon.ClassDamageModifiers;
import io.github.butterflysmp.rpg.core.weapon.ClassDamageModifiers.ClassGrant;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What worn accessories contribute, keyed for the reconcilers -- the pure half of the scanner.
 */
class AccessoryContributionsTest {

    private final AccessoryDefinition quiver = AccessoryFixtures.fletchersQuiver();
    private final AccessoryDefinition ward = AccessoryFixtures.wardCharm();

    private static List<AccessoryDefinition> slots(AccessoryDefinition... four) {
        return Arrays.asList(four);
    }

    @Test
    void aMatchingRangerGetsEverythingTheQuiverGrants_underItsSlotKey() {
        AccessoryContributions c = AccessoryContributions.of(slots(quiver, ward, null, null), "ranger");
        assertEquals(Map.of("accessory:0", 0.05), c.sources(AccessoryStat.CRIT_CHANCE));
        assertEquals(Map.of("accessory:0", -0.04), c.sources(AccessoryStat.HEALTH_REGEN));
        assertEquals(Map.of("accessory:1", 3.0), c.sources(AccessoryStat.DEFENSE));
        assertEquals(Map.of("accessory:0", new ClassGrant(WeaponClass.RANGER, 3.0)), c.classGrants(),
                "class damage becomes a GRANT, so matching() can still gate it on the held weapon");
        assertTrue(c.sources(AccessoryStat.CLASS_DAMAGE).isEmpty(), "never also a flat modifier");
    }

    /** *** THE CLASS GATE, through the scanner. A class change makes the class slot INERT. *** */
    @Test
    void aClassChangeMakesTheClassSlotInert_andTheUniversalsStay() {
        for (String profile : Arrays.asList("mage", "none", null, "Ranger")) {
            AccessoryContributions c = AccessoryContributions.of(slots(quiver, ward, null, null), profile);
            assertTrue(c.sources(AccessoryStat.CRIT_CHANCE).isEmpty(), "quiver inert for " + profile);
            assertTrue(c.sources(AccessoryStat.HEALTH_REGEN).isEmpty(), "and so is its drawback");
            assertTrue(c.classGrants().isEmpty(), "and its class damage");
            assertEquals(Map.of("accessory:1", 3.0), c.sources(AccessoryStat.DEFENSE), "the universal stays");
        }
        // Mutation: drop the contributes() check in AccessoryContributions.of -> reddens.
    }

    @Test
    void anItemInTheWrongKindOfSlotIsNotHonoured() {
        AccessoryContributions c = AccessoryContributions.of(slots(ward, quiver, null, null), "ranger");
        assertTrue(c.sources(AccessoryStat.DEFENSE).isEmpty(), "a universal item in the class slot");
        assertTrue(c.sources(AccessoryStat.CRIT_CHANCE).isEmpty(), "a class item in a universal slot");
    }

    @Test
    void twoCopiesOfOneUniversalBothCount_underTheirOwnKeys() {
        AccessoryContributions c = AccessoryContributions.of(slots(null, ward, ward, null), "none");
        assertEquals(Map.of("accessory:1", 3.0, "accessory:2", 3.0), c.sources(AccessoryStat.DEFENSE),
                "ruling Q2: two copies of a universal are allowed, and Stat sums distinct sources");
    }

    @Test
    void emptySlotsContributeNothing_andTheShapeIsEnforced() {
        AccessoryContributions c = AccessoryContributions.of(slots(null, null, null, null), "ranger");
        for (AccessoryStat stat : AccessoryStat.values()) {
            assertTrue(c.sources(stat).isEmpty(), stat.token());
        }
        assertThrows(IllegalArgumentException.class,
                () -> AccessoryContributions.of(slots(null, null, null), "ranger"));
        assertTrue(AccessoryContributions.NONE.sources(AccessoryStat.MAX_HEALTH).isEmpty());
    }

    @Test
    void theSourceKeysCarryThePrefixAndNoQuiver() {
        for (int slot = 0; slot < AccessorySlots.COUNT; slot++) {
            String key = AccessoryContributions.sourceKey(slot);
            assertTrue(key.startsWith(AccessoryContributions.SOURCE_PREFIX), key);
            assertTrue(!key.toLowerCase().contains("quiver"), "ruling A2: " + key);
        }
        assertEquals("accessory:", AccessoryContributions.SOURCE_PREFIX);
    }

    /**
     * *** THE MERGE, witnessed against Stat. A reconcile of the merged map keeps BOTH sources; a
     * reconcile of either alone would wipe the other. ***
     */
    @Test
    void theMergedMapKeepsBothSources_whenReconciledOnce() {
        Map<String, Double> gear = Map.of("CHEST", 10.0, "growth:CHEST", 20.0);
        Map<String, Double> acc = Map.of("accessory:1", 5.0);
        Map<String, Double> merged = AccessoryContributions.merged(gear, acc);
        assertEquals(Map.of("CHEST", 10.0, "growth:CHEST", 20.0, "accessory:1", 5.0), merged);

        HealthState state = new HealthState(100.0, 0.0, true);
        ModifierReconciler.reconcile(state, merged);
        assertEquals(135.0, state.max(), 1e-9, "100 base + 10 + 20 + 5");

        // The failure the merge exists to prevent: two reconciles, each wiping the other's sources.
        ModifierReconciler.reconcile(state, gear);
        ModifierReconciler.reconcile(state, acc);
        assertEquals(105.0, state.max(), 1e-9, "separate reconciles keep only whichever ran last");
        // Mutation: return `gear` or `accessories` alone from merged() -> reddens.
    }

    @Test
    void theGrantMergeFeedsMatching_soTheHeldWeaponStillGates() {
        Map<String, ClassGrant> worn = Map.of("HEAD", new ClassGrant(WeaponClass.MAGE, 2.0));
        Map<String, ClassGrant> acc = Map.of("accessory:0", new ClassGrant(WeaponClass.RANGER, 3.0));
        Map<String, ClassGrant> merged = AccessoryContributions.mergedGrants(worn, acc);
        assertEquals(Map.of("accessory:0", 3.0), ClassDamageModifiers.matching(WeaponClass.RANGER, merged));
        assertEquals(Map.of("HEAD", 2.0), ClassDamageModifiers.matching(WeaponClass.MAGE, merged));
        assertEquals(Map.of(), ClassDamageModifiers.matching(null, merged), "an empty hand gets nothing");
    }
}
