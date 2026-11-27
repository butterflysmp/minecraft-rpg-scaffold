package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * GATE-bone-club's ONE typed command, parsed against the SHIPPED scenarios (2026-11-26): what Ben types, and the queue it
 * makes, step for step. Mutation: a gate dropped from the command, or the order typed changed.
 */
class BoneClubGateCommandTest {

    static final String TYPED = "bone-club+blast-fungus+true-tooltips.TT20.TT21.TT22.TT23.TT24.TT25.TT26.TT27";

    @Test
    void theOneCommandQueuesTheWholeStack() {
        ScenarioLoader.Loaded loaded = ScenarioLoader.load(path ->
                BoneClubGateCommandTest.class.getClassLoader().getResourceAsStream(path));
        assertEquals(List.of(), loaded.refusals());
        var parsed = SelfTestGateList.parse(TYPED, loaded.gates());
        assertNull(parsed.refusal());
        assertEquals(List.of("bone-club", "blast-fungus", "true-tooltips"), parsed.gates());
        List<String> queue = SelfTestGateList.queue(parsed, loaded.gates()).stream().map(s -> s.gate() + "#" + s.row()).toList();
        assertEquals(List.of(
                "bone-club#setup", "bone-club#BC1", "bone-club#BC2", "bone-club#BC3", "bone-club#BC4", "bone-club#BC5",
                "bone-club#DB1", "bone-club#DB2", "bone-club#DB3", "bone-club#DB4", "bone-club#DB5", "bone-club#DB6",
                "bone-club#DB7", "bone-club#DB8", "bone-club#BC6",
                "blast-fungus#setup", "blast-fungus#BF1", "blast-fungus#BF2", "blast-fungus#BF3", "blast-fungus#BF4",
                "blast-fungus#BF5", "blast-fungus#BF6", "blast-fungus#BF7", "blast-fungus#FF1", "blast-fungus#FF2",
                "blast-fungus#FF3", "blast-fungus#CB1", "blast-fungus#CB2", "blast-fungus#CB3", "blast-fungus#CB4",
                "blast-fungus#BF8", "blast-fungus#BF9",
                "true-tooltips#setup", "true-tooltips#TT20", "true-tooltips#TT21", "true-tooltips#TT22", "true-tooltips#TT23",
                "true-tooltips#TT24", "true-tooltips#TT25", "true-tooltips#TT26", "true-tooltips#TT27"), queue);
    }
}
