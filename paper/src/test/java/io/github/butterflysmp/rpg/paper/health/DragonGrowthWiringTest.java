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
 * The Phage's growth (PLAN-dragon-mobs.md, the plan's step R4): the wiring that needs a live server and so has no
 * other unit row. The rule is core's ({@code MobGrowthTest}); the health share is core's ({@code HealthRescaleTest}).
 * Comment lines are filtered from the scans.
 */
class DragonGrowthWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    /**
     * THE PARENT AND ITS CHILDREN: in the one death handler, growth collects its candidates BEFORE the split spawns
     * the children, so a stage 5's own death grows the other Phages and never its two children. Pinned here, not
     * left to the order the server happens to call two handlers in.
     */
    @Test
    void growthRunsBeforeTheSplitInTheOneDeathHandler() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("listener", "RpgListeners.java")));
        int handler = indexOf(code, "public void onMobDeath(EntityDeathEvent event) {");
        int grow = indexOf(code, "MobGrower.onDeath(event.getEntity(),");
        int split = indexOf(code, "MobSplitter.onDeath(event.getEntity(),");
        assertTrue(handler > 0 && handler < grow && grow < split && split <= handler + 6,
                "handler < growth < split, in one method: " + handler + " " + grow + " " + split);
        assertTrue(code.stream().noneMatch(l -> l.contains("public void onMobDeathSplit(")),
                "no second death handler for the split: one handler, one order");
    }

    /** THE ONCE-ONLY GUARD, as wired: read the mark, write it, then report; before any stage can change. */
    @Test
    void theDeathIsMarkedCountedBeforeItIsReported() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("health", "MobGrower.java")));
        int read = indexOf(code, "boolean alreadyCounted = pdc.has(keys.mobGrowthFed, PersistentDataType.BYTE);");
        int write = indexOf(code, "pdc.set(keys.mobGrowthFed, PersistentDataType.BYTE, (byte) 1);");
        int report = indexOf(code, "return reportDeath(dead.getWorld(), dead.getLocation(), element, dead.getUniqueId(), alreadyCounted, fed,");
        assertTrue(read > 0 && read < write && write < report, "read < write < report: " + read + " " + write + " " + report);
    }

    /** The stage change: the body through R1's applier, then the health at the same share. */
    @Test
    void aStageChangeAppliesTheBodyThenTheShare() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("health", "MobGrower.java")));
        int apply = indexOf(code, "MobBodyApplier.apply(mob, def, to, keys);");
        int share = indexOf(code, "stats.rescaleMaxKeepingShare(mob.getUniqueId(), newMax);");
        assertTrue(apply > 0 && share > apply, "body, then health: " + apply + " " + share);
    }

    /** A Phage grown to its last stage splits when it dies: growth never writes the split's mark. */
    @Test
    void growthNeverWritesTheSplitMark() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("health", "MobGrower.java")));
        assertTrue(code.size() > 50, "must have read MobGrower, read " + code.size());
        assertFalse(code.stream().anyMatch(l -> l.contains("mobSplit")), "MobGrower touches no mob_split mark");
    }

    /**
     * THE DECISION IS FIXED AT THE DEATH (the follow-up of 2026-11-17; PLAN-dragon-mobs.md section 8 F5). WHO grows
     * and the distance logged are decided in {@code reportDeath}, from the positions at the death, and handed to
     * {@code applyGrowth} with the grow. {@code applyGrowth}, a tick later on the Phage's own scheduler, re-reads only
     * the STAGE ({@code MobGrowth.restage}): it never reads where the Phage stands now and never asks the range rule
     * again. Bounded STRUCTURALLY, from {@code applyGrowth}'s declaration to the next member, never by a line count.
     */
    @Test
    void theLaterTickReReadsOnlyTheStage() throws IOException {
        List<String> code = codeLines(MAIN.resolve(Path.of("health", "MobGrower.java")));
        int start = indexOf(code, "private static void applyGrowth(");
        int end = start + 1;
        while (end < code.size() && !code.get(end).startsWith("    private ") && !code.get(end).startsWith("    public ")
                && !code.get(end).startsWith("    static ")) end++;
        assertTrue(start > 0 && end > start + 5 && end < code.size(), "bounded applyGrowth: " + start + ".." + end);
        List<String> body = code.subList(start, end);
        assertTrue(body.stream().noneMatch(l -> l.contains("getLocation()")), "never where the Phage stands now");
        assertTrue(body.stream().noneMatch(l -> l.contains("stageAfter(")), "never the range rule again");
        assertTrue(body.stream().anyMatch(l -> l.contains("MobGrowth.restage(def, from, ")), "only the stage, re-read");
        assertTrue(body.stream().anyMatch(l -> l.contains("grow.distance()")), "the distance decided at the death");
        assertTrue(code.stream().anyMatch(l -> l.contains("applyGrowth(mob, death, g, fed,")),
                "reportDeath hands each grow, decided at the death, to the later tick");
    }

    /** The GROWTH line, exact: the distance to THREE places, as the BLAST line prints its spared distance. */
    @Test
    void theGrowthLinePrintsTheDistanceAsTheBlastLineDoes() {
        java.util.UUID id = java.util.UUID.fromString("50302ad8-568e-4e27-9496-ff7666f001fc");
        assertEquals("GROWTH mob=phage id=" + id + " from=1 to=2 fed=creeper/phage x element=void d=0.737 max=120",
                MobGrower.growthLine("phage", id, 1, 2, "creeper/phage x", "void", 0.7374, 120.0));
        assertEquals("GROWTH mob=phage id=" + id + " from=4 to=5 fed=enderman y element=void d=2.000 max=480",
                MobGrower.growthLine("phage", id, 4, 5, "enderman y", "void", 2.0, 480.0));
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
