package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-bone-club's two new drivers (2026-11-26): RESET_CHARGE (the server half of a real swing, which Player#attack never
 * does, READ) and INTERACT_ENTITY (the server's verdict on a right click on an entity). Their lines, pinned whole, and their
 * wiring read off the source. Mutations: either case dropped from the dispatch; the reset call dropped; the event built as
 * a plain PlayerInteractEntityEvent (shares the handler list, but is not the event the real click raises).
 */
class SelfTestBoneClubDriversTest {

    @Test
    void theLines() {
        assertEquals("DRIVE reset_charge HumanEntity#resetCooldown cooldown=1.0000->0.0200",
                SelfTestDrivers.resetChargeLine(1.0f, 0.02f));
        assertEquals("INTERACT result cancelled=true tamed=false held=bone_club amount=1",
                SelfTestDrivers.interactResultLine(true, "false", "bone_club", 1));
    }

    @Test
    void theVerbsParseFromTheirYamlNames() {
        assertEquals(Scenario.Verb.RESET_CHARGE, Scenario.Verb.valueOf("reset_charge".toUpperCase(java.util.Locale.ROOT)));
        assertEquals(Scenario.Verb.INTERACT_ENTITY, Scenario.Verb.valueOf("interact_entity".toUpperCase(java.util.Locale.ROOT)));
    }

    @Test
    void theWiring() throws IOException {
        List<String> d = Files.readAllLines(Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper",
                "selftest", "SelfTestDrivers.java"), StandardCharsets.UTF_8);
        int reset = -1, interact = -1, event = -1, call = -1;
        for (int i = 0; i < d.size(); i++) {
            String l = d.get(i);
            if (l.contains("case RESET_CHARGE -> {")) reset = i;
            if (l.contains("case INTERACT_ENTITY -> interactEntity(p, a);")) interact = i;
            if (l.contains("var event = new org.bukkit.event.player.PlayerInteractAtEntityEvent(p, e,")) event = i;
            if (event > 0 && call < 0 && l.contains("Bukkit.getPluginManager().callEvent(event);") && i > event) call = i;
        }
        assertTrue(reset > 0 && d.get(reset + 3).contains("p.resetCooldown();"), "the reset is the swing's server half");
        assertTrue(interact > 0 && event > 0 && call == event + 2, "the real click's event, into every listener");
    }
}
