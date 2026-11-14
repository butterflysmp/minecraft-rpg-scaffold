package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mining verbs' wiring, which needs a server to run and so is pinned by reading the source (the shape
 * {@code MiningWiringSignatureTest} uses). Every needle is a whole statement, anchored at both ends, and the bodies are
 * cut between two method signatures, so a moved or widened call reads red rather than matching a prefix.
 *
 * <p>The live witnesses are GATE-mining-auto.md's rows: each DIG's {@code cooldown=} against its MINING click line.
 */
class SelfTestMiningDriversTest {

    private static final Path DRIVERS = Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestDrivers.java");

    private static String drivers() throws IOException {
        return Files.readString(DRIVERS, StandardCharsets.UTF_8);
    }

    private static String between(String source, String from, String to) {
        int a = source.indexOf(from);
        assertTrue(a >= 0, "'" + from + "' moved; this scan needs its new name");
        int b = source.indexOf(to, a + from.length());
        assertTrue(b > a, "'" + to + "' no longer follows '" + from + "'");
        return source.substring(a, b);
    }

    private static int at(String body, String statement) {
        int i = body.indexOf(statement);
        assertTrue(i >= 0, "'" + statement + "' not found in:\n" + body);
        assertEquals(-1, body.indexOf(statement, i + 1), "'" + statement + "' must appear once");
        return i;
    }

    @Test
    void aDigReadsTheChargeThenFiresTheEventThenResetsAsTheSwingDoes() throws IOException {
        String dig = between(drivers(), "private Optional<String> dig(Player p) {", "private Optional<String> miningTrace(");
        int read = at(dig, "float before = p.getAttackCooldown();");
        int built = at(dig, "BlockDamageEvent event = new BlockDamageEvent(p, b, hit.getHitBlockFace(), item, false);");
        int fired = at(dig, "Bukkit.getPluginManager().callEvent(event);");
        int reset = at(dig, "p.resetCooldown();");
        assertTrue(read < built && built < fired && fired < reset,
                "read, build, fire, reset: the click's read comes BEFORE the swing's reset (PLAN-mining.md F1)");
        // The event carries what the player holds, as a real dig START does: no item argument to the verb.
        at(dig, "ItemStack item = p.getInventory().getItemInMainHand();");
        // The reach is the player's own block-interaction range, not a constant of ours.
        at(dig, "var rangeAttribute = p.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);");
        at(dig, "RayTraceResult hit = p.rayTraceBlocks(range);");
        // Mutation: resetCooldown above callEvent -> every click reads 0.0300 -> reddens here.
    }

    @Test
    void theMiningAimsTurnTheEyeNotTheFeet() throws IOException {
        String turn = between(drivers(), "private void turnEyeTo(", "private Optional<String> dig(");
        at(turn, "SelfTestAim.Rotation r = SelfTestAim.at(feet.getX(), feet.getY(), feet.getZ(), p.getEyeHeight(), t[0], t[1], t[2]);");
        at(turn, "p.teleportAsync(turned);");
        String patch = between(drivers(), "private Optional<String> aimPatch(", "private void turnEyeTo(");
        at(patch, "double[] w = view.frame().patchToWorld(uv[0], uv[1], 0.0);");   // the PATCH's frame, on the wall plane
    }

    @Test
    void theVerbsReadTheMiningServiceAndChangeNothingButTheTrace() throws IOException {
        // Every MiningService call the drivers make, by method name. peek and isTracing are read-only; trace is the
        // shipped toggle, and it is only ever called to turn the trace ON.
        String src = drivers();
        List<String> calls = new ArrayList<>();
        Matcher m = Pattern.compile("(?:run\\.mining\\(\\)|mining)\\.([a-zA-Z]+)\\(").matcher(src);
        while (m.find()) calls.add(m.group(1));
        assertEquals(List.of("peek", "isTracing", "trace", "isTracing"), calls, "the mining calls, in source order");
        String trace = between(src, "private Optional<String> miningTrace(Player p) {", "// --- helpers");
        at(trace, "if (!mining.isTracing(p)) mining.trace(p);");
        // Positive control: the pattern finds a call written the way the drivers write them.
        Matcher control = Pattern.compile("(?:run\\.mining\\(\\)|mining)\\.([a-zA-Z]+)\\(").matcher("run.mining().peek(p, b)");
        assertTrue(control.find() && control.group(1).equals("peek"));
    }
}
