package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P10, THE PAD HOLDS ITS CHUNKS (the seat's rulings of 2026-11-24, a1-a4 and b-f). The pure half: which chunks, when each
 * ticket is taken (only on a chunk reported loaded, a2), the 3-request cap (a3's number), and a release of exactly what
 * was taken, once. The Bukkit half is {@code BukkitPadChunks}, behind {@link SelfTestPadTickets.Chunks}.
 */
class SelfTestPadTicketsTest {

    /** The stand of today's boot: 104,201,-4 (GATE-mage-guaranteed's readings), so world spawn -96, -4. */
    private static final SelfTestPadShape.Stand STAND = SelfTestPadShape.stand(-96, -4);

    /** A scripted chunk system: each request is queued; the test decides when a load lands and whether it stays. */
    private static final class FakeChunks implements SelfTestPadTickets.Chunks {
        final Set<String> loaded = new HashSet<>();
        final Set<String> tickets = new LinkedHashSet<>();
        final List<String> requests = new ArrayList<>();
        final List<String> takes = new ArrayList<>();
        final List<String> takesWhileUnloaded = new ArrayList<>();
        final List<String> releases = new ArrayList<>();
        final Deque<Runnable> pending = new ArrayDeque<>();
        final Map<String, Integer> slips = new HashMap<>();   // a chunk unloads before its callback this many times
        boolean refuseTake;

        static String key(int x, int z) { return x + "," + z; }

        @Override public boolean isLoaded(int x, int z) { return loaded.contains(key(x, z)); }

        @Override public boolean take(int x, int z) {
            takes.add(key(x, z));
            if (!loaded.contains(key(x, z))) takesWhileUnloaded.add(key(x, z));
            return !refuseTake && tickets.add(key(x, z));
        }

        @Override public boolean release(int x, int z) {
            releases.add(key(x, z));
            return tickets.remove(key(x, z));
        }

        @Override public void request(int x, int z, Runnable loadedCallback) {
            requests.add(key(x, z));
            pending.add(() -> {
                String k = key(x, z);
                int left = slips.getOrDefault(k, 0);
                if (left > 0) {                       // it loaded, and the load's own hold dropped before we ran
                    slips.put(k, left - 1);
                    loaded.remove(k);
                } else {
                    loaded.add(k);
                }
                loadedCallback.run();
            });
        }

        void landAll() {
            while (!pending.isEmpty()) pending.poll().run();
        }
    }

    private static final class Done implements java.util.function.Consumer<SelfTestPadTickets.Outcome> {
        final List<SelfTestPadTickets.Outcome> calls = new ArrayList<>();
        @Override public void accept(SelfTestPadTickets.Outcome o) { calls.add(o); }
    }

    /** The footprint, by arithmetic: x 101..150 -> chunks 6..9, z -18..10 -> chunks -2..0; 12 chunks, x then z. */
    @Test
    void theFootprintIsTwelveChunksFromTheShapesOneSource() {
        int[] span = SelfTestPadShape.chunks(STAND);
        assertEquals(104, STAND.x());
        assertEquals(-4, STAND.z());
        assertEquals(101, STAND.x() + SelfTestPadShape.X1 - 1, "the rim's west column");
        assertEquals(150, STAND.x() + SelfTestPadShape.X2 + 1, "the rim's east column");
        assertEquals(-18, STAND.z() + SelfTestPadShape.Z1 - 1);
        assertEquals(10, STAND.z() + SelfTestPadShape.Z2 + 1);
        assertEquals(List.of(6, -2, 9, 0), List.of(span[0], span[1], span[2], span[3]));
        List<SelfTestPadTickets.Chunk> f = SelfTestPadTickets.footprint(span);
        assertEquals(12, f.size());
        assertEquals(new SelfTestPadTickets.Chunk(6, -2), f.get(0), "x outer, z inner: the old loop's order");
        assertEquals(new SelfTestPadTickets.Chunk(9, 0), f.get(11));
    }

    /** The loads use the footprint, the same set: one source, not two lists. */
    @Test
    void theLoadsRequestExactlyTheFootprint() {
        var chunks = new FakeChunks();
        var t = new SelfTestPadTickets(chunks, SelfTestPadShape.chunks(STAND));
        t.acquire(new Done());
        List<String> want = SelfTestPadTickets.footprint(SelfTestPadShape.chunks(STAND)).stream()
                .map(c -> c.x() + "," + c.z()).toList();
        assertEquals(want, chunks.requests);
    }

    /** All twelve held before the outcome says OK, and the held line is reported once, at the twelfth. */
    @Test
    void allTwelveAreHeldBeforeTheOutcomeIsOk() {
        var chunks = new FakeChunks();
        var t = new SelfTestPadTickets(chunks, SelfTestPadShape.chunks(STAND));
        var done = new Done();
        t.acquire(done);
        assertEquals(0, chunks.takes.size(), "nothing is taken before a load lands");
        assertTrue(done.calls.isEmpty());
        chunks.landAll();
        assertEquals(1, done.calls.size(), "one outcome");
        assertTrue(done.calls.get(0).ok());
        assertEquals(12, done.calls.get(0).held());
        assertEquals(12, chunks.tickets.size());
        assertEquals(12, t.held());
        assertEquals("PAD tickets held=12", done.calls.get(0).line());
    }

    /** a2: a take is NEVER attempted on a chunk reported unloaded (that is the blocking load). */
    @Test
    void noTakeIsAttemptedOnAChunkReportedUnloaded() {
        var chunks = new FakeChunks();
        chunks.slips.put("6,-2", 2);
        chunks.slips.put("8,0", 1);
        var t = new SelfTestPadTickets(chunks, SelfTestPadShape.chunks(STAND));
        t.acquire(new Done());
        chunks.landAll();
        assertEquals(List.of(), chunks.takesWhileUnloaded);
        assertEquals(12, chunks.takes.size(), "one take per chunk, each on a loaded chunk");
    }

    /** a3's retry: a chunk that slips once is requested again and held; the run goes on. */
    @Test
    void aChunkThatSlipsOnceIsRequestedAgainAndHeld() {
        var chunks = new FakeChunks();
        chunks.slips.put("7,-2", 1);
        var t = new SelfTestPadTickets(chunks, SelfTestPadShape.chunks(STAND));
        var done = new Done();
        t.acquire(done);
        chunks.landAll();
        assertEquals(2, chunks.requests.stream().filter("7,-2"::equals).count());
        assertTrue(done.calls.get(0).ok());
        assertEquals(12, t.held());
    }

    /** A chunk that slips on all three requests fails, naming held and attempts; the release returns exactly those held. */
    @Test
    void aChunkThatSlipsThreeTimesFailsAndReleasesExactlyWhatWasTaken() {
        var chunks = new FakeChunks();
        chunks.slips.put("7,-2", 3);
        var t = new SelfTestPadTickets(chunks, SelfTestPadShape.chunks(STAND));
        var done = new Done();
        t.acquire(done);
        chunks.landAll();
        assertEquals(1, done.calls.size(), "one outcome, not one per late callback");
        SelfTestPadTickets.Outcome o = done.calls.get(0);
        assertFalse(o.ok());
        assertEquals(3, chunks.requests.stream().filter("7,-2"::equals).count(), "SEAT'S MECHANISM NUMBER: 3 in total");
        int heldAtFail = o.held();
        assertEquals("chunk 7,-2 did not stay loaded held=" + heldAtFail + " attempts=3", o.why());
        assertEquals(heldAtFail, t.held(), "no take after the failure");
        assertEquals(11, heldAtFail, "every other chunk landed and was held");
        assertEquals(heldAtFail, t.release());
        assertEquals(heldAtFail, chunks.releases.size());
        assertTrue(chunks.tickets.isEmpty(), "nothing left force-held");
    }

    /** Every exit releases exactly what was taken, ONCE: a second release does nothing. */
    @Test
    void theReleaseIsExactlyWhatWasTakenAndOnlyOnce() {
        var chunks = new FakeChunks();
        var t = new SelfTestPadTickets(chunks, SelfTestPadShape.chunks(STAND));
        t.acquire(new Done());
        chunks.landAll();
        assertEquals(12, t.release());
        assertEquals(0, t.release(), "the second exit finds nothing to release");
        assertEquals(12, chunks.releases.size(), "each chunk released once");
        assertEquals(chunks.takes.stream().sorted().toList(), chunks.releases.stream().sorted().toList());
        assertTrue(chunks.tickets.isEmpty());
    }

    /** A release before any load landed releases nothing, and a load landing after it takes nothing (no leak). */
    @Test
    void aLoadLandingAfterTheReleaseTakesNothing() {
        var chunks = new FakeChunks();
        var t = new SelfTestPadTickets(chunks, SelfTestPadShape.chunks(STAND));
        var done = new Done();
        t.acquire(done);
        assertEquals(0, t.release(), "the run ended before any load landed");
        chunks.landAll();
        assertEquals(0, chunks.takes.size(), "no ticket outlives the run");
        assertTrue(chunks.tickets.isEmpty());
    }

    /** A ticket the server refuses (already held) fails the acquire rather than being counted or released later. */
    @Test
    void aRefusedTakeFails() {
        var chunks = new FakeChunks();
        chunks.refuseTake = true;
        var t = new SelfTestPadTickets(chunks, SelfTestPadShape.chunks(STAND));
        var done = new Done();
        t.acquire(done);
        chunks.landAll();
        assertFalse(done.calls.get(0).ok());
        assertTrue(done.calls.get(0).why().startsWith("chunk 6,-2 ticket refused held=0"), done.calls.get(0).why());
        assertEquals(0, t.release());
    }

    /** A run of several gates reaches the pad more than once: once all twelve are held, acquiring again takes nothing. */
    @Test
    void acquiringAgainOnceHeldTakesNothingAndIsOk() {
        var chunks = new FakeChunks();
        var t = new SelfTestPadTickets(chunks, SelfTestPadShape.chunks(STAND));
        t.acquire(new Done());
        chunks.landAll();
        var again = new Done();
        t.acquire(again);
        chunks.landAll();
        assertEquals(12, chunks.takes.size(), "no second take: a plugin ticket is not counted (READ)");
        assertEquals(12, chunks.requests.size());
        assertTrue(again.calls.get(0).ok());
        assertNull(again.calls.get(0).line(), "the held line is logged once, at the twelfth take");
    }

    /** The release causes, by the run's end reason. */
    @Test
    void theCauseOfARelease() {
        assertEquals("end", SelfTestPadTickets.causeOf("FINISHED"));
        assertEquals("stop", SelfTestPadTickets.causeOf("STOPPED by /rpg selftest stop"));
        assertEquals("abort", SelfTestPadTickets.causeOf("ABORTED pad: chunk 6,-2 did not stay loaded held=0 attempts=3"));
        assertEquals("abort", SelfTestPadTickets.causeOf("ABORTED in GATE-x.yml#R1: boom"));
        assertEquals("PAD tickets released=12 cause=end", SelfTestPadTickets.releasedLine(12, "end"));
    }
}
