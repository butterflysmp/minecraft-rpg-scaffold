package io.github.butterflysmp.rpg.paper.summon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G1 to G7 (the seat, 2026-11-26) where the server meets the pure guard: the goal carries the HELD target and its tier from
 * tick to tick, scans the give-up range, leaves a body in another world out (so the guard reads it gone), logs the target
 * line only when something changed, and teleports only on the guard's word (which is never while it holds a target). The
 * goal needs a server, so this reads the source. The decisions themselves are {@code SummonGuardTest}'s tables.
 */
class SummonHoldsTargetWiringTest {

    private static String read(String rel) throws IOException {
        return Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/" + rel)).replace("\r", "");
    }

    /** Mutation: the held target carried as a bare id (D4's), or re-read from the tier each tick. */
    @Test
    void theGoalCarriesTheHeldTargetAndItsTier() throws IOException {
        String g = read("summon/SummonGoal.java");
        assertTrue(g.length() > 4_000, "control: SummonGoal was read");
        assertTrue(g.contains("private Optional<SummonGuard.Held> current = Optional.empty();"));
        assertTrue(g.contains("SummonGuard.decide(def, him, at, directed, current, candidates(owner, directed), now);"));
        int held = g.indexOf("current = d.held();");
        int pub = g.indexOf("ctx.summons().setCurrent(body.getUniqueId(), current);", held);
        assertTrue(held > 0 && pub > held, "published, with its tier, in the tick it is decided");
    }

    /** G7. Mutation: the line written every tick, or only on a target change (a kept line would never appear). */
    @Test
    void theTargetLineIsWrittenOnceAChange_withItsReason() throws IOException {
        String g = read("summon/SummonGoal.java");
        assertTrue(g.contains("if (d.change() != SummonGuard.Change.NONE || d.dropped().isPresent()) {"));
        // V7 (d), 2026-11-27: with the tick it was decided on, the same `now` the decision was handed (pinned above).
        assertTrue(g.contains("ctx.log().info(SummonTrace.target(summon, vec(body), d, now));"));
        assertTrue(g.contains("long now = Bukkit.getCurrentTick();"), "now is the server tick");
        assertEquals(1, g.split("SummonTrace\\.target\\(", -1).length - 1, "one target line");
    }

    /** G3, G1. Mutation: the scan left at another radius, or a body in another world kept as a candidate. */
    @Test
    void theScanIsTheGiveUpRange_andAnotherWorldIsGone() throws IOException {
        String g = read("summon/SummonGoal.java");
        int scan = g.indexOf("private List<SummonGuard.Candidate> candidates(Player owner, Optional<UUID> directed) {");
        String body = g.substring(scan, g.indexOf("\n    }\n", scan));
        assertTrue(body.contains("double r = def.giveUpRange();"), "the one named value");
        assertTrue(body.contains("current.map(SummonGuard.Held::id).map(Bukkit::getEntity).ifPresent(near::add);"),
                "a held body beyond the scan is still found by id, so it is dropped for RANGE, not GONE");
        assertTrue(body.contains("if (!l.getWorld().equals(owner.getWorld())) continue;"), "another world reads as gone");
        assertTrue(g.contains("if (d.teleport()) {"), "the teleport is the guard's (G4: never while holding)");
        assertEquals(1, g.split("body\\.teleport\\(", -1).length - 1, "and nowhere else");
    }

    /** G-S2's verb. Mutation: the clear form setting him after all. */
    @Test
    void mobTargetsClearFormForgetsThroughTheSeam() throws IOException {
        String d = read("selftest/SelfTestDrivers.java");
        assertTrue(d.contains("boolean clear = f.length == 2 && f[1].equals(\"clear\");"));
        assertTrue(d.contains("MobTargets.set((org.bukkit.entity.Mob) best, clear ? null : p,"));
    }
}
