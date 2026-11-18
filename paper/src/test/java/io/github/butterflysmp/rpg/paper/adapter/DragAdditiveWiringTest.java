package io.github.butterflysmp.rpg.paper.adapter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE ADDITIVE DRAG'S WIRING (feat/drag-additive, 2026-11-18), stated on the SOURCE because a packet and a Netty thread
 * cannot run in a unit test. Each scan has a positive control: a needle that must hit where the thing is, or its
 * silence elsewhere proves nothing.
 */
class DragAdditiveWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    private static String read(String... parts) throws IOException {
        return Files.readString(MAIN.resolve(Path.of("", parts)), StandardCharsets.UTF_8);
    }

    private static int count(String text, String needle) {
        Matcher m = Pattern.compile(Pattern.quote(needle)).matcher(text);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    /**
     * THREADING (CLAUDE.md): the move counter runs on a Netty thread, so neither it nor the counter it feeds may import
     * the Bukkit API. The control: the same needle finds the Bukkit imports of {@code ImplosionDrags}.
     */
    @Test
    void thePacketThreadsCounterTouchesNoBukkit() throws IOException {
        assertEquals(0, count(read("packet", "MoveCountListener.java"), "import org.bukkit"));
        assertEquals(0, count(read("adapter", "DragTrace.java"), "import org.bukkit"));
        assertTrue(count(read("adapter", "ImplosionDrags.java"), "import org.bukkit") > 0, "the control");
    }

    /** A player's push is an ADD, sent by DragImpulse, never a setVelocity; a mob's is still set. */
    @Test
    void aPlayersAddGoesThroughTheImpulseAndAMobsIsSet() throws IOException {
        String code = read("adapter", "ImplosionDrags.java");
        int add = code.indexOf("if (p.mode() == ImplosionDrag.Mode.ADD && l instanceof Player player) {");
        assertTrue(add > 0, "the ADD branch");
        int send = code.indexOf("DragImpulse.send(player, v);", add);
        int set = code.indexOf("l.setVelocity(new Vector(v.x(), v.y(), v.z()));", add);
        assertTrue(send > add && set > send, "the ADD sends the impulse; only the else sets a velocity");
        assertEquals(1, count(code, "setVelocity("), "one set, the mob's");
    }

    /**
     * THE PACKET SHOWS NOTHING: the empty sound, the limited smoke particle, no block particles, and a centre far past
     * the client's 32-block particle cut ({@code ClientLevel#doAddParticle}: {@code ldc2_w 1024.0d}, READ).
     */
    @Test
    void theImpulseIsSilentAndUnseen() throws IOException {
        String code = read("adapter", "DragImpulse.java");
        assertEquals(1, count(code, "Sounds.INTENTIONALLY_EMPTY"));
        assertEquals(1, count(code, "new Particle<>(ParticleTypes.SMOKE)"));
        assertEquals(1, count(code, "new WeightedList<>()"));
        assertEquals(1, count(code, "at.getY() + HIDDEN_ABOVE"));
        assertTrue(DragImpulse.HIDDEN_ABOVE * DragImpulse.HIDDEN_ABOVE > 1024.0 * 4, "well past the 32-block cut");
    }

    /** One registration of the counter, at the plugin's one registration point; the run's end turns the trace off. */
    @Test
    void theCounterIsRegisteredOnceAndTheRunsEndTurnsTheTraceOff() throws IOException {
        assertEquals(1, count(read("RpgPlugin.java"), "new io.github.butterflysmp.rpg.paper.packet.MoveCountListener("));
        String run = read("selftest", "SelfTestRun.java");
        int restore = run.indexOf("String restored = snapshot.restore(");
        assertTrue(restore > 0 && run.indexOf("service.dragTrace().set(false);", restore) > restore);
        assertEquals(1, count(read("selftest", "SelfTestDrivers.java"), "run.dragTrace().set(true);"),
                "the one place it is turned on: the scenario step");
    }

    @Test
    void theTraceStartsOffAndCountsOnlyWhatItIsTold() {
        DragTrace t = new DragTrace();
        assertTrue(!t.on());
        java.util.UUID a = java.util.UUID.randomUUID();
        assertEquals(0, t.moves(a));
        t.moved(a);
        t.moved(a);
        t.moved(null);
        assertEquals(2, t.moves(a));
        t.forget(a);
        assertEquals(0, t.moves(a));
    }
}
