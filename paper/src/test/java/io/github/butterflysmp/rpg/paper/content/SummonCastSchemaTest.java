package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** THE "summon" CAST TYPE (the seat's 3a, 2026-11-24): parsed, its id required, and never repeatable. */
class SummonCastSchemaTest {

    private static YamlConfiguration yaml(String text) throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(text);
        return y;
    }

    @Test
    void aSummonCastNamesItsSummon() throws Exception {
        assertEquals(new CastSpec.Summon("test_wolf"), AbilitySchema.parseCast(yaml("type: summon\nsummon: test_wolf\n")));
    }

    @Test
    void aSummonCastWithNoSummonIsRefusedNamed() throws Exception {
        var ex = assertThrows(IllegalArgumentException.class, () -> AbilitySchema.parseCast(yaml("type: summon\n")));
        assertTrue(ex.getMessage().contains("summon: <id>"), ex.getMessage());
    }

    @Test
    void aVolleyOfSummonsIsRefused() throws Exception {
        var ex = assertThrows(IllegalArgumentException.class, () -> AbilitySchema.parseCast(yaml(
                "type: volley\nshots: 2\ninterval_ticks: 4\nof:\n  type: summon\n  summon: test_wolf\n")));
        assertTrue(ex.getMessage().contains("summon"), ex.getMessage());
    }
}
