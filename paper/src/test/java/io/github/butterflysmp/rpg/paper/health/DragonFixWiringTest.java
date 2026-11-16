package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fixes after the 2026-11-15 boot of the dragon stack (GATE-dragon-fix.md), at the wiring points no core row can
 * see. Comment lines are filtered from every scan, because each site explains itself in prose that names the very
 * call it replaced.
 */
class DragonFixWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");
    private static final Path SELFTEST = Path.of("src", "main", "resources", "selftest");

    /**
     * D1: {@code AttributeInstance#getDefaultValue} is the ATTRIBUTE's registry default (0.7 for movement speed, read
     * from the jar: CraftAttributeInstance#getDefaultValue -> Attribute#getDefaultValue; Attributes' clinit), not the
     * type's. The type's own base is {@code EntityType#getDefaultAttributes()} (DefaultAttributes.getSupplier). The
     * core port only names the quantity; THIS row pins which Bukkit call supplies it, for the applier and the probe.
     */
    @Test
    void theSpeedIsScaledFromTheTypesDefaultAttributesNeverTheAttributesDefault() throws IOException {
        List<String> applier = codeLines(MAIN.resolve(Path.of("health", "MobBodyApplier.java")));
        assertTrue(applier.size() > 30, "must have read MobBodyApplier, read " + applier.size());
        assertTrue(applier.stream().anyMatch(l -> l.contains(".getDefaultAttributes()")),
                "the type's base comes from EntityType#getDefaultAttributes()");
        assertTrue(applier.stream().anyMatch(l -> l.contains("public double typeBase() { return typeBaseSpeed(entity); }")),
                "the core port's typeBase() is answered by typeBaseSpeed");
        assertEquals(0, applier.stream().filter(l -> l.contains("getDefaultValue(")).count(),
                "the attribute's registry default is never read");

        List<String> drivers = codeLines(MAIN.resolve(Path.of("selftest", "SelfTestDrivers.java")));
        assertTrue(drivers.stream().anyMatch(l -> l.contains("MobBodyApplier.typeBaseSpeed(")),
                "PROBE_BODY prints the same quantity the applier scales from");
        assertTrue(drivers.stream().anyMatch(l -> l.contains("\" speedType=\"")), "and names it speedType");
        assertEquals(0, drivers.stream().filter(l -> l.contains("getDefaultValue(")).count(),
                "the probe never prints the attribute's registry default");
    }

    /**
     * C2: a row's mob is tagged by IDENTITY, inside the spawn's pre-spawn consumer, after the stamp: the command's
     * {@code spawn(...)} hands the entity to {@code preAdd} before the add event, so it is never in the world untagged
     * (D2: the clean's removals run a tick later, on each mob's own scheduler, so a nearest-selector tag after the
     * spawn could pick the old one).
     */
    @Test
    void theInstrumentsTagGoesOnInsideThePreSpawnConsumer() throws IOException {
        List<String> command = codeLines(MAIN.resolve(Path.of("command", "RpgCommand.java")));
        int spawnStart = indexOf(command, "LivingEntity spawned = player.getWorld().spawn(");
        int stamp = indexOf(command, "MobSpawner.stampCustom(entity, def, atStage, adapters.keys());");
        int pre = indexOf(command, "preAdd.accept(entity);");
        int spawnEnd = indexOf(command, "UUID id = spawned.getUniqueId();");
        assertTrue(spawnStart > 0 && stamp > spawnStart && pre > stamp && pre < spawnEnd,
                "inside spawn(), after the stamp: " + spawnStart + " < " + stamp + " < " + pre + " < " + spawnEnd);

        List<String> drivers = codeLines(MAIN.resolve(Path.of("selftest", "SelfTestDrivers.java")));
        int tag = indexOf(drivers, "entity.addScoreboardTag(spec.tag());");
        int freeze = indexOf(drivers, "if (spec.frozen()) entity.setAI(false);");
        assertTrue(tag > 0 && freeze > 0, "the SPAWN driver's consumer tags and freezes: " + tag + ", " + freeze);
    }

    /** C2, the scenario half: no dragon scenario tags a mob by a nearest-entity selector. Control: sunder still does. */
    @Test
    void noDragonScenarioTagsByANearestSelector() throws IOException {
        for (String gate : List.of("dragon-bodies", "dragon-split", "dragon-growth")) {
            String yml = Files.readString(SELFTEST.resolve("GATE-" + gate + ".yml"), StandardCharsets.UTF_8);
            assertTrue(yml.contains("- spawn: "), gate + " spawns through the SPAWN step");
            assertEquals(0, yml.lines().filter(l -> !l.trim().startsWith("#") && l.contains("sort=nearest")).count(),
                    gate + " has no nearest-selector line");
        }
        String control = Files.readString(SELFTEST.resolve("GATE-sunder.yml"), StandardCharsets.UTF_8);
        assertTrue(control.lines().anyMatch(l -> !l.trim().startsWith("#") && l.contains("sort=nearest")),
                "the control: the scan can see the pattern where it still is");
    }

    /**
     * C3: the row-start clean is followed by a LEFTOVER count, taken after the clean's removals have run (they are
     * scheduled on each mob's own scheduler, so the count waits {@code LEFTOVER_WAIT_TICKS}).
     */
    @Test
    void theCleanIsFollowedByALeftoverCountOnALaterTick() throws IOException {
        List<String> run = codeLines(MAIN.resolve(Path.of("selftest", "SelfTestRun.java")));
        int removed = indexOf(run, "int removed = snapshot.removeTagged(player, service.scheduler());");
        int later = indexOf(run, "SelfTestStaging.LEFTOVER_WAIT_TICKS);");
        int line = indexOf(run, "log(SelfTestStaging.leftoverLine(");
        assertTrue(removed > 0 && line > removed && later > line,
                "clean, then the count inside a task scheduled LEFTOVER_WAIT_TICKS later: "
                        + removed + " < " + line + " < " + later);
    }

    /** C5: the snapshot records the world's difficulty at RUN START and the one restore every exit path runs puts it back. */
    @Test
    void theDifficultyFoundIsRestoredByTheOneRestore() throws IOException {
        List<String> snapshot = codeLines(MAIN.resolve(Path.of("selftest", "SelfTestSnapshot.java")));
        int took = indexOf(snapshot, "this.difficulty = world.getDifficulty();");
        int set = indexOf(snapshot, "world.setDifficulty(difficulty);");
        int restoreLine = indexOf(snapshot, "\" difficulty=\" + difficulty");
        assertTrue(took > 0 && set > took && restoreLine > set, took + " < " + set + " < " + restoreLine);
        List<String> run = codeLines(MAIN.resolve(Path.of("selftest", "SelfTestRun.java")));
        assertTrue(run.stream().anyMatch(l -> l.contains("String restored = snapshot.restore(")),
                "end(), every exit path's one door, runs the restore");
    }

    /** C6: the player's chat reaches the run through the service, on the player's own scheduler. */
    @Test
    void chatReachesAPendingAskThroughTheService() throws IOException {
        List<String> listeners = codeLines(MAIN.resolve(Path.of("listener", "RpgListeners.java")));
        int handler = indexOf(listeners, "public void onSelfTestChat(io.papermc.paper.event.player.AsyncChatEvent event) {");
        int call = indexOf(listeners, "selfTest.onChat(event.getPlayer(),");
        assertTrue(handler > 0 && call > handler, handler + " < " + call);
        List<String> service = codeLines(MAIN.resolve(Path.of("selftest", "SelfTestService.java")));
        assertTrue(service.stream().anyMatch(l -> l.contains("adapters.scheduler().onEntity(player, () -> {")),
                "the async chat thread only hands off; the run is touched on the player's thread");
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
