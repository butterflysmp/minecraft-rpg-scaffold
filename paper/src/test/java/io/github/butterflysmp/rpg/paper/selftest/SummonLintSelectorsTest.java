package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.summon.SummonDefinition;
import io.github.butterflysmp.rpg.paper.content.SummonLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * X1: the lint's summon selectors are exactly one per entity a shipped summon uses, so a new summon entity cannot ship
 * with rows the lint is blind to. The CONTROL is the shipped count: one summon, so the set is not read off an empty load.
 */
class SummonLintSelectorsTest {

    @Test
    void oneSelectorPerShippedSummonEntity() {
        Logger log = Logger.getLogger("SummonLintSelectorsTest");
        log.setUseParentHandlers(false);
        var shipped = new SummonLoader(log, e -> true)
                .loadAll(Path.of("src", "main", "resources", "content", "summons").toFile());
        assertEquals(1, shipped.size(), "CONTROL: ls content/summons/*.yml | wc -l = 1");
        Set<String> want = new TreeSet<>();
        for (SummonDefinition d : shipped.all()) want.add("type=minecraft:" + d.entity());
        assertEquals(want, new TreeSet<>(ScenarioLint.SUMMON_SELECTORS));
    }
}
