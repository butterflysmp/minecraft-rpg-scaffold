package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BB2R (the seat's follow-up, 2026-11-26): PROBE_ATTRIBUTE reads a player's attribute ON THE SERVER and prints the double.
 * The vanilla `attribute … get` reply cannot carry the decimals to our log. READ, javap: PacketEvents 2.13.0
 * {@code AdventureNBTSerializer.deserializeTranslationArgumentList} decodes a numeric argument with
 * {@code NBTNumber.getAsInt()}, so 1.6 was logged as 1. Mutations named per test.
 */
class SelfTestAttributeProbeTest {

    /** Mutation: the format drops to an integer, or the base is printed as the value. */
    @Test
    void theLineCarriesThreeDecimalsOfTheValueAndTheBase() {
        assertEquals("ATTRIBUTE attack_speed value=1.600 base=4.000", SelfTestDrivers.attributeLine("attack_speed", 1.6, 4.0));
        assertEquals("ATTRIBUTE attack_speed value=2.000 base=4.000", SelfTestDrivers.attributeLine("attack_speed", 2.0, 4.0));
    }

    /** Mutation: the verb's name drifts from its YAML key, or the driver arm is dropped. */
    @Test
    void theVerbLoadsAndIsDriven() throws IOException {
        assertEquals(Scenario.Verb.PROBE_ATTRIBUTE, Scenario.Verb.valueOf("probe_attribute".toUpperCase(java.util.Locale.ROOT)));
        String d = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestDrivers.java"))
                .replace("\r", "");
        assertTrue(d.length() > 20_000, "control: SelfTestDrivers was read");
        assertTrue(d.contains("case PROBE_ATTRIBUTE -> probeAttribute(p, a);"), "the arm");
        int m = d.indexOf("private Optional<String> probeAttribute(Player p, String arg) {");
        assertTrue(m > 0, "the driver");
        String body = d.substring(m, d.indexOf("\n    }\n", m));
        assertTrue(body.contains("instance.getValue()") && body.contains("instance.getBaseValue()"),
                "it reads the server's own value and base: " + body);
    }
}
