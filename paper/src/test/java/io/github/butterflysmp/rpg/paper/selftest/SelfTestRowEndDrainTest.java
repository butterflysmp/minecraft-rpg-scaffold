package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P-R2 (the seat's ruling on #308, 2026-11-25): a row ends with a drain, ONE tick, a second drain, then DONE, so a reply to
 * the row's last step that reaches the reader after the first drain is still logged under THIS row. The run needs a
 * server, so the order is pinned by reading the source, every needle with a control. Mutations named per test.
 */
class SelfTestRowEndDrainTest {

    private static String run() throws IOException {
        return Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestRun.java"))
                .replace("\r", "");
    }

    /** Mutation: the redrain tick set to 0 (the second drain runs in the same tick, and catches nothing new). */
    @Test
    void theTicksAreTwoThenOne() {
        assertEquals(2, SelfTestStaging.ROW_END_TICKS, "unchanged from before P-R2");
        assertEquals(1, SelfTestStaging.ROW_END_REDRAIN_TICKS, "P-R2's one tick");
    }

    /** Mutation: the first drain, the scheduled hop, or the second drain dropped, or DONE logged before the second. */
    @Test
    void drainThenOneTickThenDrainThenDone() throws IOException {
        String r = run();
        assertTrue(r.length() > 20_000, "control: SelfTestRun was read, " + r.length() + " chars");
        int end = r.indexOf("service.scheduler().onEntityLater(player, this::rowDone, SelfTestStaging.ROW_END_TICKS);");
        int done = r.indexOf("private void rowDone() {");
        int first = r.indexOf("drainReplies();", done);
        int hop = r.indexOf("service.scheduler().onEntityLater(player, this::rowDoneDrained, SelfTestStaging.ROW_END_REDRAIN_TICKS);", done);
        int drained = r.indexOf("private void rowDoneDrained() {");
        int second = r.indexOf("drainReplies();", drained);
        int logged = r.indexOf("log(\"DONE ticks=\"", drained);
        assertTrue(end > 0, "the steps' end waits ROW_END_TICKS, then rowDone");
        assertTrue(done > 0 && first > done && hop > first && drained > hop, "rowDone drains, then hops ONE tick");
        assertTrue(second > drained && logged > second, "the hop drains AGAIN, then logs DONE");
        assertEquals(1, r.split("log\\(\"DONE ticks=\"", -1).length - 1, "DONE is logged in one place");
        assertEquals(-1, r.substring(done, drained).indexOf("log(\"DONE"), "and not before the second drain");
    }

    /**
     * GATE-broken-bottle BB7: the USE driver logs what every listener LEFT of its synthetic press, after the dispatch.
     * Mutation: the line dropped, or logged before callEvent (it would read the defaults, DEFAULT and DENY, always).
     */
    @Test
    void theUseDriverLogsTheVerdictAfterTheDispatch() throws IOException {
        assertEquals("USE result useItemInHand=DENY useInteractedBlock=DENY", SelfTestDrivers.useResultLine("DENY", "DENY"));
        String d = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestDrivers.java"))
                .replace("\r", "");
        assertTrue(d.length() > 20_000, "control: SelfTestDrivers was read");
        int use = d.indexOf("private Optional<String> use(Player p) {");
        int dispatch = d.indexOf("Bukkit.getPluginManager().callEvent(event));", use);
        int logged = d.indexOf("run.log(useResultLine(event.useItemInHand().name(), event.useInteractedBlock().name()));", use);
        int next = d.indexOf("private Optional<String> swing(Player p) {", use);
        assertTrue(use > 0 && dispatch > use && logged > dispatch && next > logged, "logged after the dispatch, in use()");
    }
}
