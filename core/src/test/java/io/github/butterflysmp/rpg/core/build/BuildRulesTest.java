package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.build.BuildRules.Picked;
import io.github.butterflysmp.rpg.core.build.LoadoutResolution.Equipped;
import io.github.butterflysmp.rpg.core.build.LoadoutResolution.Saved;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildRulesTest {

    private static final String NONE = "none";

    /** Every id is distinct across the two pools, so an ability leaking between cells has nowhere to hide. */
    private static final PoolDefinition RANGER_FIRE = new PoolDefinition(new CellKey("ranger", "fire"), "Fire Ranger",
            List.of("r_ult"), List.of("r_a", "r_b", "r_c"), new Loadout("r_ult", "r_a", "r_b"));
    private static final PoolDefinition MAGE_FIRE = new PoolDefinition(new CellKey("mage", "fire"), "Fire Mage",
            List.of("m_ult"), List.of("m_a", "m_b", "m_c"), new Loadout("m_ult", "m_a", "m_b"));
    /** A second element for ONE class only, so "elements with a pool for the class" is a real filter. */
    private static final PoolDefinition MAGE_FROST = new PoolDefinition(new CellKey("mage", "frost"), "Frost Mage",
            List.of("f_ult"), List.of("f_a", "f_b"), new Loadout("f_ult", "f_a", "f_b"));

    private static final List<PoolDefinition> SHIPPED = List.of(RANGER_FIRE, MAGE_FIRE);
    private static final List<PoolDefinition> WIDER = List.of(RANGER_FIRE, MAGE_FIRE, MAGE_FROST);

    // ------------------------------------------------------------------ class and element lists

    @Test
    void theClassListIsThePoolsClassesSortedAndDistinct() {
        assertEquals(List.of("mage", "ranger"), BuildRules.classes(SHIPPED));
        assertEquals(List.of("mage", "ranger"), BuildRules.classes(WIDER), "two mage pools, one mage");
    }

    /** Ruling 7: only cells with a pool. At ship that is FIRE, for either class. */
    @Test
    void theShippedElementListIsFireOnly() {
        assertEquals(List.of("fire"), BuildRules.elementsFor(SHIPPED, "ranger"));
        assertEquals(List.of("fire"), BuildRules.elementsFor(SHIPPED, "mage"));
    }

    @Test
    void theElementListIsTheElementsWithAPoolForThatClass() {
        assertEquals(List.of("fire", "frost"), BuildRules.elementsFor(WIDER, "mage"));
        assertEquals(List.of("fire"), BuildRules.elementsFor(WIDER, "ranger"), "no frost ranger pool");
    }

    /** A player with no class yet is offered every element any pool has, so element-first still works. */
    @Test
    void anUnchosenClassIsOfferedEveryPooledElement() {
        assertEquals(List.of("fire", "frost"), BuildRules.elementsFor(WIDER, NONE));
        assertEquals(List.of("fire"), BuildRules.elementsFor(SHIPPED, NONE));
    }

    @Test
    void pickingAClassKeepsTheElementOnlyWhereThatCellHasAPool() {
        assertEquals("frost", BuildRules.elementAfterClassPick(WIDER, "mage", "frost", NONE));
        assertEquals(NONE, BuildRules.elementAfterClassPick(WIDER, "ranger", "frost", NONE),
                "no frost ranger pool: the element is cleared, never left pointing at a cell with no pool");
        assertEquals("fire", BuildRules.elementAfterClassPick(WIDER, "ranger", "fire", NONE));
        assertEquals(NONE, BuildRules.elementAfterClassPick(WIDER, "ranger", NONE, NONE));
        assertEquals(NONE, BuildRules.elementAfterClassPick(WIDER, "ranger", null, NONE),
                "a null element (an old profile) reads as unchosen, and does not throw");
    }

    // ------------------------------------------------------------------ what a slot offers

    /** THE ROW THE "offer every registered ability" MUTATION REDDENS: the other class's actives are absent. */
    @Test
    void aSlotOffersOnlyItsPoolsMembersInThatRole() {
        assertEquals(List.of("r_ult"), BuildRules.choices(RANGER_FIRE, LoadoutSlot.ULTIMATE));
        assertEquals(List.of("r_a", "r_b", "r_c"), BuildRules.choices(RANGER_FIRE, LoadoutSlot.ACTIVE_1));
        assertEquals(List.of("r_a", "r_b", "r_c"), BuildRules.choices(RANGER_FIRE, LoadoutSlot.ACTIVE_2));
        assertFalse(BuildRules.choices(RANGER_FIRE, LoadoutSlot.ACTIVE_1).contains("m_a"), "no Mage ability");
    }

    @Test
    void aPickOutsideThePoolOrTheRoleIsRefused() {
        Equipped current = LoadoutResolution.resolve(RANGER_FIRE, Optional.empty());
        assertEquals(Optional.empty(), BuildRules.pick(RANGER_FIRE, current, LoadoutSlot.ACTIVE_1, "m_a"),
                "another cell's ability");
        assertEquals(Optional.empty(), BuildRules.pick(RANGER_FIRE, current, LoadoutSlot.ULTIMATE, "r_c"),
                "an Active offered as the Ultimate");
        assertEquals(Optional.empty(), BuildRules.pick(RANGER_FIRE, current, LoadoutSlot.ACTIVE_2, "r_ult"),
                "the Ultimate offered as an Active");
    }

    // ------------------------------------------------------------------ the pick itself

    @Test
    void pickingFromTheDefaultChangesOnlyThatSlot() {
        Equipped current = LoadoutResolution.resolve(RANGER_FIRE, Optional.empty());
        assertEquals(Optional.of(new Picked("r_ult", "r_c", "r_b")),
                BuildRules.pick(RANGER_FIRE, current, LoadoutSlot.ACTIVE_1, "r_c"));
        assertEquals(Optional.of(new Picked("r_ult", "r_a", "r_c")),
                BuildRules.pick(RANGER_FIRE, current, LoadoutSlot.ACTIVE_2, "r_c"));
    }

    /** THE ROW THE "allow the same ability in both Actives" MUTATION REDDENS, from both sides. */
    @Test
    void pickingTheOtherActivesAbilitySwapsTheTwo() {
        Equipped current = LoadoutResolution.resolve(RANGER_FIRE, Optional.empty());   // r_a, r_b
        assertEquals(Optional.of(new Picked("r_ult", "r_b", "r_a")),
                BuildRules.pick(RANGER_FIRE, current, LoadoutSlot.ACTIVE_1, "r_b"));
        assertEquals(Optional.of(new Picked("r_ult", "r_b", "r_a")),
                BuildRules.pick(RANGER_FIRE, current, LoadoutSlot.ACTIVE_2, "r_a"));
    }

    /** An empty Active (an id the pool dropped) swaps as an empty: the ability moves, the hole moves with it. */
    @Test
    void swappingWithAnEmptySlotMovesTheHole() {
        Equipped current = LoadoutResolution.resolve(RANGER_FIRE, Optional.of(new Saved("r_ult", "gone", "r_b")));
        assertEquals(Optional.of(new Picked("r_ult", "r_b", null)),
                BuildRules.pick(RANGER_FIRE, current, LoadoutSlot.ACTIVE_1, "r_b"));
    }

    @Test
    void pickingWhatTheSlotAlreadyHoldsChangesNothing() {
        Equipped current = LoadoutResolution.resolve(RANGER_FIRE, Optional.empty());
        assertEquals(Optional.of(new Picked("r_ult", "r_a", "r_b")),
                BuildRules.pick(RANGER_FIRE, current, LoadoutSlot.ACTIVE_1, "r_a"));
    }

    /** No sequence of legal picks can put one ability in both Actives: every pair, from every start. */
    @Test
    void noPickEverLeavesADuplicateActive() {
        List<String> ids = RANGER_FIRE.actives();
        for (String a1 : ids) {
            for (String a2 : ids) {
                if (a1.equals(a2)) continue;
                Equipped current = LoadoutResolution.resolve(RANGER_FIRE, Optional.of(new Saved("r_ult", a1, a2)));
                for (LoadoutSlot slot : List.of(LoadoutSlot.ACTIVE_1, LoadoutSlot.ACTIVE_2)) {
                    for (String id : ids) {
                        Picked p = BuildRules.pick(RANGER_FIRE, current, slot, id).orElseThrow();
                        assertTrue(!p.active1().equals(p.active2()),
                                "from " + a1 + "/" + a2 + ", " + slot + " <- " + id + " gave " + p);
                    }
                }
            }
        }
    }

    @Test
    void theUltimatePickLeavesTheActivesAlone() {
        PoolDefinition twoUlts = new PoolDefinition(new CellKey("ranger", "fire"), "Fire Ranger",
                List.of("u1", "u2"), List.of("r_a", "r_b"), new Loadout("u1", "r_a", "r_b"));
        Equipped current = LoadoutResolution.resolve(twoUlts, Optional.empty());
        assertEquals(Optional.of(new Picked("u2", "r_a", "r_b")),
                BuildRules.pick(twoUlts, current, LoadoutSlot.ULTIMATE, "u2"));
    }
}
