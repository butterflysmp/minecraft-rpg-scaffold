package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dragon-mobs step R1 (PLAN-dragon-mobs.md): the guard's rule, and the three wiring points that need a live server
 * and so have no other unit row. Comment lines are filtered from the scans, because every one of these sites explains
 * itself in prose beside the call.
 */
class DragonBodiesWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    @Test
    void onlyOurCreepersRefuseTheVanillaDetonation() {
        assertTrue(CustomCreeperGuard.refusesVanillaDetonation(true, "phage"), "our creeper: refused");
        assertFalse(CustomCreeperGuard.refusesVanillaDetonation(true, null), "a vanilla creeper still explodes");
        assertFalse(CustomCreeperGuard.refusesVanillaDetonation(false, "knell"), "not a creeper: nothing to refuse");
    }

    /** All three vanilla routes to a creeper blast are cancelled through the guard: the swell, flint, lightning. */
    @Test
    void theListenersCancelAllThreeRoutesThroughTheGuard() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("listener", "RpgListeners.java")));
        assertTrue(code.size() > 1000, "must have read RpgListeners, read " + code.size());
        assertEquals(3, code.stream().filter(l -> l.contains("CustomCreeperGuard.refusesVanillaDetonation(")).count(),
                "the prime, the ignite and the power handler each ask the guard");
        for (String event : List.of("(ExplosionPrimeEvent event)", "CreeperIgniteEvent event)",
                "CreeperPowerEvent event)")) {
            assertTrue(code.stream().anyMatch(l -> l.contains(event)), "a handler takes " + event);
        }
    }

    /** The body and the stage go on in the PRE-SPAWN consumer, before the add event seeds health. */
    @Test
    void theSpawnAppliesTheBodyInsideThePreSpawnConsumer() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("command", "RpgCommand.java")));
        int consumer = indexOf(code, ".set(adapters.keys().mobId, PersistentDataType.STRING, def.id());");
        int apply = indexOf(code, "MobBodyApplier.apply(entity, def, atStage, adapters.keys());");
        int spawnEnd = indexOf(code, "UUID id = spawned.getUniqueId();");
        assertTrue(consumer > 0 && apply > consumer && apply < spawnEnd,
                "the body is applied after mob_id is written and before spawn() returns: "
                        + consumer + " < " + apply + " < " + spawnEnd);
    }

    /** The seed asks the STAGED form, so a stage-3 Phage seeds 120 and not its file's stage-1 80. */
    @Test
    void theSeedReadsTheStage() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("health", "MobNameplateManager.java")));
        assertTrue(code.stream().anyMatch(l -> l.contains("MobSeeding.maxHealth(mobs, mobId, stage, maxHealthOf(mob));")),
                "the seed passes the live mob's stage");
    }

    private static int indexOf(List<String> code, String needle) {
        for (int i = 0; i < code.size(); i++) if (code.get(i).contains(needle)) return i;
        return -1;
    }

    private static List<String> codeLines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                .filter(l -> {
                    String t = l.trim();
                    return !(t.startsWith("//") || t.startsWith("*") || t.startsWith("/*"));
                })
                .toList();
    }
}
