package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FragmentDefinitionTest {

    private static FragmentDefinition fragment(Map<AccessoryStat, Double> modifiers) {
        return new FragmentDefinition("vigor", "Vigor", "red_dye", List.of("More of you."), modifiers);
    }

    @Test
    void aPositiveModifierOnAUniversalStatIsAccepted() {
        FragmentDefinition f = fragment(Map.of(AccessoryStat.MAX_HEALTH, 4.0, AccessoryStat.CRIT_CHANCE, 0.02));
        assertEquals(4.0, f.modifiers().get(AccessoryStat.MAX_HEALTH));
        assertEquals(0.02, f.modifiers().get(AccessoryStat.CRIT_CHANCE));
    }

    /** Ruling 21: positive-only. Every stat, including the ones an ACCESSORY may carry a bounded negative on. */
    @Test
    void aNegativeIsRefusedOnEveryStat_ruling21() {
        for (AccessoryStat stat : AccessoryStat.values()) {
            if (stat == AccessoryStat.CLASS_DAMAGE) continue;
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> fragment(Map.of(stat, -0.01)), stat.token());
            assertTrue(e.getMessage().contains("positive-only"), e.getMessage());
        }
    }

    @Test
    void zeroNanAndInfinityAreRefused() {
        for (double bad : new double[] {0.0, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> fragment(Map.of(AccessoryStat.DEFENSE, bad)),
                    String.valueOf(bad));
        }
    }

    /** class_damage is refused in v1 (section 2.3): a fragment is already class-scoped by its pool. */
    @Test
    void classDamageIsRefused() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> fragment(Map.of(AccessoryStat.CLASS_DAMAGE, 3.0)));
        assertTrue(e.getMessage().contains("class_damage"), e.getMessage());
    }

    @Test
    void aFragmentMustModifySomethingAndHaveAnId() {
        assertThrows(IllegalArgumentException.class, () -> fragment(Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new FragmentDefinition(" ", "x", "red_dye", List.of(), Map.of(AccessoryStat.DEFENSE, 1.0)));
    }

    @Test
    void theRegistryRefusesADuplicateId() {
        FragmentRegistry registry = new FragmentRegistry();
        registry.register(fragment(Map.of(AccessoryStat.DEFENSE, 1.0)));
        assertTrue(registry.find("vigor").isPresent());
        assertThrows(IllegalStateException.class, () -> registry.register(fragment(Map.of(AccessoryStat.DEFENSE, 2.0))));
        assertEquals(1, registry.size());
    }

    @Test
    void fourSlotsAndAPrefixDisjointFromTheAccessories() {
        assertEquals(4, FragmentSlots.COUNT, "ruling 4: a fixed 4");
        assertEquals("fragment:0", FragmentSlots.sourceKey(0));
        assertEquals("fragment:3", FragmentSlots.sourceKey(3));
        assertThrows(IllegalArgumentException.class, () -> FragmentSlots.sourceKey(4));
        assertTrue(!FragmentSlots.SOURCE_PREFIX.equals(
                io.github.butterflysmp.rpg.core.accessory.AccessoryContributions.SOURCE_PREFIX));
    }
}
