package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * X6 (b) (the seat, 2026-11-28): THE LINES PRINT THE VIEW AN ACT READ. The stone's {@code drop} line ends with the yaw
 * and pitch (as the use line's always has), and a look-reading command is preceded by a {@code DRIVE look} line.
 */
class ViewLinesTest {

    @Test
    void theDropLineEndsWithTheYawAndPitchItFiredOn() {
        assertEquals("DRIVE drop HumanEntity#dropItem(false) item=stone returned=false heldAfter=stone yaw=-90.000 pitch=0.000",
                SelfTestDrivers.dropLine("stone", false, "stone", -90f, 0f));
    }

    @Test
    void aLookReadingCommandIsPrecededByItsView() {
        assertEquals("DRIVE look yaw=-90.000 pitch=12.500", SelfTestDrivers.lookCommandLine(-90f, 12.5f));
    }

    /** The wiring, read off the source: the command driver asks the lint's own predicate, and the drop driver its line. */
    @Test
    void theDriversWriteThem() throws IOException {
        String src = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestDrivers.java"),
                StandardCharsets.UTF_8);
        assertTrue(src.contains("if (ScenarioLint.readsLook(new Scenario.Step(Scenario.Verb.CMD, text))) {"));
        assertTrue(src.contains("run.log(lookCommandLine(p.getLocation().getYaw(), p.getLocation().getPitch()));"));
        assertTrue(src.contains("run.log(dropLine(before, dropped, itemId(p.getInventory().getItemInMainHand()), p.getLocation().getYaw(),"));
    }
}
