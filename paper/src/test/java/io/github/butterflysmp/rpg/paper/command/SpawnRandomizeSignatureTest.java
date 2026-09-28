package io.github.butterflysmp.rpg.paper.command;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seat's F16c ruling (PLAN-mob-scaling.md §6 F16): {@code /rpg spawn} passes {@code randomizeData}
 * TRUE exactly when the mob is VANILLA ({@code def == null}), so it runs vanilla's own spawn setup, and
 * FALSE for a CUSTOM mob, whose content definition is authoritative (the Knell stays unarmed, M17).
 *
 * <p>The spawn needs a live world, so this is a source scan of the one call.
 */
class SpawnRandomizeSignatureTest {

    private static final Path COMMAND = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg",
            "paper", "command", "RpgCommand.java");

    @Test
    void randomizeDataIsTrueExactlyForAVanillaMob() throws IOException {
        List<String> code = Files.readAllLines(COMMAND, StandardCharsets.UTF_8).stream()
                .filter(l -> !l.trim().startsWith("//") && !l.trim().startsWith("*")).toList();
        int spawn = -1;
        for (int i = 0; i < code.size(); i++) if (code.get(i).contains("player.getWorld().spawn(")) spawn = i;
        assertTrue(spawn >= 0, "POSITIVE CONTROL: the spawn call is found, so the scan is not blind");
        assertEquals(1, code.stream().filter(l -> l.contains("player.getWorld().spawn(")).count(),
                "one spawn call in RpgCommand");
        // The argument list continues on the next line: location, class, reason, randomizeData, consumer.
        String args = code.get(spawn + 1).trim();
        assertTrue(args.startsWith("player.getLocation(), entityClass, CreatureSpawnEvent.SpawnReason.CUSTOM,"),
                "the spawn's arguments moved; this scan reads them from the line after the call: " + args);
        assertTrue(args.contains("CreatureSpawnEvent.SpawnReason.CUSTOM, def == null,"),
                "F16c: randomizeData must be `def == null` -- true for a vanilla mob, false for a custom one. Was: "
                        + args + ". Mutation: invert the condition -> reddens");
    }
}
