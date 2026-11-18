package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE PHAGE'S OWN FUSE, THE WIRING THAT NEEDS A SERVER (the seat's ruling 1 on #204, route (b)). The rules are core's
 * ({@code MobFuseTest}, {@code StagedDistanceTest}); these rows read the paper half's lines, and each names the
 * condition of the ruling it pins. Comment lines are filtered from the scans, so a javadoc that quotes a call cannot
 * satisfy a needle.
 */
class PhageFuseWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    private static List<String> fuser() throws IOException {
        return codeLines(MAIN.resolve(Path.of("health", "PhageFuser.java")));
    }

    /**
     * CONDITION 2: VANILLA'S SWELL GOAL IS REMOVED AND OURS CLAIMS MOVE, AT EVERY APPEARANCE. A creeper read back from
     * disk gets a fresh SwellGoal from {@code Creeper#registerGoals} (READ), so the removal is in {@code onAppear}, which
     * the add-to-world handler and the enable sweep both call (the next row).
     */
    @Test
    void vanillasSwellGoalIsRemovedAndOursClaimsMoveAtEveryAppearance() throws IOException {
        List<String> code = fuser();
        int appear = indexOf(code, "public void onAppear(LivingEntity entity) {");
        int remove = indexAfter(code, appear, "goals.removeGoal(creeper, VanillaGoal.CREEPER_SWELL);");
        int add = indexAfter(code, appear, "goals.addGoal(creeper, HOLD_PRIORITY, new Hold(creeper));");
        assertTrue(appear > 0 && appear < remove && remove < add, "appear < remove < add: " + appear + " " + remove
                + " " + add);
        assertTrue(code.stream().anyMatch(l -> l.contains("return EnumSet.of(GoalType.MOVE);")), "the hold claims MOVE");
        assertTrue(code.stream().anyMatch(l -> l.contains("static final int HOLD_PRIORITY = 2;")), "vanilla's priority");
    }

    /** CONDITION 3, the lifecycle: the add and remove handlers, the tracking handler, and the enable sweep all reach it. */
    @Test
    void theAddRemoveAndTrackHandlersAndTheEnableSweepReachTheFuser() throws IOException {
        List<String> listeners = codeLines(MAIN.resolve(Path.of("listener", "RpgListeners.java")));
        int add = indexOf(listeners, "public void onEntityAdd(EntityAddToWorldEvent event) {");
        assertTrue(indexAfter(listeners, add, "fuser.onAppear(mob);") > add, "appear");
        int rem = indexOf(listeners, "public void onEntityRemove(EntityRemoveFromWorldEvent event) {");
        assertTrue(indexAfter(listeners, rem, "fuser.onRemove(mob.getUniqueId());") > rem, "remove");
        int track = indexOf(listeners, "public void onTrackEntity(io.papermc.paper.event.player.PlayerTrackEntityEvent event) {");
        assertEquals(track + 1, indexAfter(listeners, track, "fuser.onTrack(event.getPlayer(), event.getEntity());"),
                "the tracking handler hands over at once");
        List<String> plugin = codeLines(MAIN.resolve("RpgPlugin.java"));
        assertTrue(plugin.stream().anyMatch(l -> l.contains("fuser.onAppear(mob);")), "the enable sweep");
        assertTrue(plugin.stream().anyMatch(l -> l.contains("String swellCheck = fuser.selfCheck();")),
                "CONDITION 5: the startup self-check is logged");
    }

    /**
     * CONDITION 2, THE SERVER'S OWN DIRECTION STAYS -1: nothing in the fuser touches the server's swell. The ONE
     * {@code explode()} is inside {@code detonate}, between the mark and its removal; no {@code setIgnited},
     * {@code ignite()}, {@code setFuseTicks} or {@code setMaxFuseTicks}.
     */
    @Test
    void theFuserNeverTouchesTheServersSwellAndExplodesOnlyInsideTheMark() throws IOException {
        List<String> code = fuser();
        for (String never : List.of("setIgnited(", ".ignite(", "setFuseTicks(", "setMaxFuseTicks(")) {
            assertFalse(code.stream().anyMatch(l -> l.contains(never)), "the fuser must not contain " + never);
        }
        assertEquals(1, code.stream().filter(l -> l.contains(".explode()")).count(), "one explode");
        int detonate = indexOf(code, "public void detonate(Creeper creeper) {");
        int mark = indexAfter(code, detonate, "raising.add(id);");
        int explode = indexAfter(code, detonate, "creeper.explode();");
        int unmark = indexAfter(code, detonate, "raising.remove(id);");
        assertTrue(detonate < mark && mark < explode && explode < unmark, detonate + " " + mark + " " + explode + " "
                + unmark);
    }

    /** CONDITION 1: the packet carries the READ index and type, and the index is 16. */
    @Test
    void thePacketCarriesTheReadIndexAsAnInt() throws IOException {
        List<String> code = fuser();
        assertTrue(code.stream().anyMatch(l -> l.contains("public static final int SWELL_DIR_INDEX = 16;")));
        assertTrue(code.stream().anyMatch(l -> l.contains(
                "List.of(new EntityData<>(SWELL_DIR_INDEX, EntityDataTypes.INT, direction))));")));
        assertEquals(16, PhageFuser.SWELL_DIR_INDEX);
    }

    /**
     * CONDITION 3: EVERY VIEWER. A START or RESET goes to {@code getTrackedBy()}; a new viewer of a lit Phage is sent
     * the state ONE TICK LATER (the event fires before {@code addPairing}, READ).
     */
    @Test
    void everyViewerIsShownAndANewOneATickLater() throws IOException {
        List<String> code = fuser();
        int show = indexOf(code, "private void show(Active a, int direction, String reason, OptionalDouble d, double trigger, double cancel) {");
        assertTrue(indexAfter(code, show, "Set<Player> viewers = a.creeper.getTrackedBy();") == show + 1);
        assertTrue(indexAfter(code, show, "for (Player p : viewers) send(p, a.creeper.getEntityId(), direction);")
                == show + 2);
        int track = indexOf(code, "public void onTrack(Player viewer, Entity entity) {");
        int later = indexAfter(code, track, "adapters.scheduler().onEntityLater(entity, () -> {");
        int tail = indexAfter(code, later, "}, 1);");
        assertTrue(track > 0 && later > track && tail > later, track + " " + later + " " + tail);
    }

    /** CONDITION 4: the prime handler refuses every prime our fuse did not raise, with its line, BEFORE any blast. */
    @Test
    void thePrimeHandlerRefusesAPrimeOurFuseDidNotRaise() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("listener", "RpgListeners.java")));
        int handler = indexOf(code, "public void onCustomCreeperPrime(ExplosionPrimeEvent event) {");
        int cancel = indexAfter(code, handler, "event.setCancelled(true);");
        int raised = indexAfter(code, handler, "if (!fuser.isRaising(creeper.getUniqueId())) {");
        int line = indexAfter(code, handler, "adapters.log().info(\"CREEPERGUARD prime refused mob=\" + mobId");
        int detonate = indexAfter(code, handler, "MobBlaster.detonate(creeper,");
        assertTrue(handler < cancel && cancel < raised && raised < line && line < detonate,
                handler + " " + cancel + " " + raised + " " + line + " " + detonate);
        List<String> drivers = codeLines(MAIN.resolve(Path.of("selftest", "SelfTestDrivers.java")));
        assertTrue(drivers.stream().anyMatch(l -> l.contains("case DETONATE -> creepers(p, a, \"detonate\", c -> run.fuser().detonate(c));")),
                "the instrument's detonate goes through the fuser's mark");
        assertFalse(drivers.stream().anyMatch(l -> l.contains("c.explode()")), "and never around it");
    }

    /** The SWELL line, exactly, as a gate predicts it. */
    @Test
    void theSwellLineNamesTheDirectionTheReasonTheTickTheViewersAndTheDistances() {
        UUID id = UUID.fromString("4c30b659-a0dd-4c32-a998-e4e45e8c49c7");
        assertEquals("SWELL mob=phage id=" + id + " dir=+1 reason=start tick=1200 viewers=1 d=2.500 trigger=3.000"
                        + " cancel=7.000 swell=0",
                PhageFuser.line("phage", id, 1, "start", 1200, 1, OptionalDouble.of(2.5), 3.0, 7.0, 0));
        assertEquals("SWELL mob=phage id=" + id + " dir=-1 reason=reset tick=1215 viewers=2 d=7.010 trigger=3.000"
                        + " cancel=7.000 swell=15",
                PhageFuser.line("phage", id, -1, "reset", 1215, 2, OptionalDouble.of(7.01), 3.0, 7.0, 15));
        assertEquals("SWELL mob=phage id=" + id + " dir=+1 reason=track tick=1220 viewers=1 d=- trigger=- cancel=-"
                        + " swell=5",
                PhageFuser.line("phage", id, 1, "track", 1220, 1, OptionalDouble.empty(), Double.NaN, Double.NaN, 5));
    }

    /** The FUSED line, at the fuse's end, so a gate reads the tick it blew against the START line's tick. */
    @Test
    void theFusedLineNamesTheTickAndTheFullSwell() {
        UUID id = UUID.fromString("4c30b659-a0dd-4c32-a998-e4e45e8c49c7");
        assertEquals("FUSED mob=phage id=" + id + " tick=1230 swell=30/30",
                PhageFuser.fusedLine("phage", id, 1230, 30, 30));
    }

    private static int indexOf(List<String> code, String needle) {
        return indexAfter(code, -1, needle);
    }

    private static int indexAfter(List<String> code, int from, String needle) {
        for (int i = from + 1; i < code.size(); i++) if (code.get(i).contains(needle)) return i;
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
