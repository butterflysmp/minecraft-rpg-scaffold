package io.github.butterflysmp.rpg.core.mining;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The two registries: find by id, keep load order, and refuse a duplicate id. */
class MiningRegistryTest {

    @Test
    void aDuplicateVeinSizeIdIsRefused() {
        VeinSizeRegistry sizes = new VeinSizeRegistry();
        sizes.register(new VeinSize("small", 3, 2, 2, 3));
        assertThrows(IllegalStateException.class, () -> sizes.register(new VeinSize("small", 1, 1, 1, 1)));
        assertEquals(1, sizes.size());
        assertEquals(3, sizes.find("small").orElseThrow().width(), "the first registration stands");
        assertTrue(sizes.find("medium").isEmpty());
    }

    @Test
    void aDuplicateOreIdIsRefused() {
        OreRegistry ores = new OreRegistry();
        ores.register(OreDefinitionTest.iron(Map.of("small", 5), List.of("small")));
        assertThrows(IllegalStateException.class,
                () -> ores.register(OreDefinitionTest.iron(Map.of("small", 5), List.of("small"))));
        assertEquals(1, ores.all().size());
    }
}
