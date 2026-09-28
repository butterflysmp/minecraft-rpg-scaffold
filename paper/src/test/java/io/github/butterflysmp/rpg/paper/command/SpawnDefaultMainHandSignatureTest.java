package io.github.butterflysmp.rpg.paper.command;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F16 (PLAN-mob-scaling.md §6): {@code /rpg spawn}'s PRE-SPAWN CONSUMER hands out
 * {@code DefaultMainHand}'s weapons. {@code DefaultMainHandTest} pins the table; only this can see
 * whether the spawn path still reads it. The consumer needs a live world, so this is a source scan.
 */
class SpawnDefaultMainHandSignatureTest {

    private static final Path COMMAND = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg",
            "paper", "command", "RpgCommand.java");

    @Test
    void theSpawnConsumerAppliesTheDefaultMainHandBeforeTheAdd() throws IOException {
        List<String> code = Files.readAllLines(COMMAND, StandardCharsets.UTF_8).stream()
                .filter(l -> !l.trim().startsWith("//") && !l.trim().startsWith("*")).toList();
        assertTrue(code.size() > 500, "must have read RpgCommand, read " + code.size() + " code lines");
        int spawn = indexOf(code, "player.getWorld().spawn(");
        int table = indexOf(code, "DefaultMainHand.of(baseEntity");
        int after = indexOf(code, "The add event has already seeded it");
        assertTrue(spawn >= 0, "POSITIVE CONTROL: the spawn call is found, so the scan is not blind");
        assertTrue(table > spawn, "F16: the pre-spawn consumer must apply DefaultMainHand");
        // The consumer is the lambda passed to spawn(); a call after spawn() returns is after the add event.
        int returned = indexOf(code, "UUID id = spawned.getUniqueId();");
        assertTrue(returned > table, "the table must be applied INSIDE the consumer, before spawn() returns");
        assertTrue(after < 0 || after > table, "and before the post-spawn read");
    }

    private static int indexOf(List<String> code, String needle) {
        for (int i = 0; i < code.size(); i++) if (code.get(i).contains(needle)) return i;
        return -1;
    }
}
