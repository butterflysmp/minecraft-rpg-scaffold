package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code probe_mana}: THE PLAYER'S MANA, READ RATHER THAN INFERRED (the seat's brief of 2026-11-21, PR 1).
 *
 * <p>Why: {@code GATE-crystal-shard-staff.md}'s CS2 could only say "mana falls by 8, READ from source", because no
 * selftest step read a player's mana. A row now brackets a cast with two {@code probe_mana} steps and reads the fall
 * from the two {@code MANA} lines.
 */
class SelfTestManaProbeTest {

    /**
     * The line, word for word, every field distinct so a transposition cannot pass. W4 (the seat, 2026-11-27): it ends
     * {@code tick=<t>}, the server tick the probe read the pool in, last so every older field reads as before.
     */
    @Test
    void theLineIsWordForWord() {
        assertEquals("MANA current=87.000 max=100.000 tick=4021", SelfTestDrivers.manaLine(87.0, 100.0, 4021));
        assertEquals("MANA current=12.500 max=130.000 tick=77", SelfTestDrivers.manaLine(12.5, 130.0, 77));
        // Mutation: drop " tick=" -> reddens. Pass max for the tick -> "tick=100" never matches 4021 -> reddens.
    }

    /** W4: the probe passes the SERVER tick, read where it reads the pool. Mutation: a literal 0, or no tick at all. */
    @Test
    void theProbePassesTheServerTick() throws java.io.IOException {
        String d = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestDrivers.java")).replace("\r", "");
        int probe = d.indexOf("case PROBE_MANA -> {");
        assertTrue(probe > 0, "control: the PROBE_MANA arm was found");
        String arm = d.substring(probe, d.indexOf("yield Optional.empty();", probe));
        assertTrue(arm.contains("pool.max(p.getUniqueId(), ResourceCost.DEFAULT_RESOURCE), Bukkit.getCurrentTick()));"),
                arm);
    }

    /** The scenario grammar admits {@code probe_mana} with no argument, as {@code probe_self} is admitted. */
    @Test
    void theVerbLoads() {
        ScenarioLoader.Loaded loaded = ScenarioLoader.load(path -> switch (path) {
            case "selftest/index.yml" -> stream("gates:\n  - probe\n");
            case "selftest/GATE-probe.yml" -> stream("setup:\n  - close: \"\"\nX1:\n  - probe_mana: \"\"\n  - probe_self: \"\"\n");
            default -> null;
        });
        assertEquals(List.of(), loaded.refusals(), "probe_mana is refused");
        List<Scenario.Step> steps = loaded.gates().get("probe").stream()
                .filter(s -> s.row().equals("X1")).findFirst().orElseThrow().steps();
        assertEquals(Scenario.Verb.PROBE_MANA, steps.get(0).verb());
        assertTrue(steps.get(1).verb() == Scenario.Verb.PROBE_SELF, "CONTROL: the neighbouring verb loads too");
    }

    private static java.io.InputStream stream(String s) {
        return new java.io.ByteArrayInputStream(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
