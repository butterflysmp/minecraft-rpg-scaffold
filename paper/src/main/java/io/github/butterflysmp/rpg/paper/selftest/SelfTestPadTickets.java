package io.github.butterflysmp.rpg.paper.selftest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * P10, THE PAD HOLDS ITS CHUNKS (the seat's rulings of 2026-11-24: a1-a4, b-f). The DECISION, pure and unit-tested
 * ({@code SelfTestPadTicketsTest}): which chunks (the footprint, from {@link SelfTestPadShape#chunks}, the one source the
 * loads use too), when each ticket is taken, how often a chunk is requested, and the release. The Bukkit calls are
 * behind {@link Chunks} ({@code BukkitPadChunks}, which carries the plugin handle from {@code RpgPlugin}).
 *
 * <p><b>Why a ticket is taken in the load's completion (a1-a3, READ by javap on the pinned 26.1.2 jar).</b> Taking it
 * FIRST is a blocking load: {@code CraftWorld.addPluginChunkTicket} adds the ticket and then calls {@code getChunkAt},
 * which is {@code ServerLevel.getChunk(x, z, FULL, true)} and a {@code syncLoad} on the tick thread. The async load's
 * completion, though, runs on the server's main thread ({@code ChunkTaskScheduler.scheduleChunkTask} queues onto its
 * {@code mainThreadExecutor}, drained by {@code ServerChunkCache$MainThreadExecutor}, whose running thread is the
 * server's {@code mainThread}), and the load's OWN temporary {@code CHUNK_LOAD} ticket is removed only AFTER that
 * completion returns ({@code ChunkTaskScheduler.lambda$scheduleTickingState$1}: the consumer, then
 * {@code removeTicketAtLevel(CHUNK_LOAD, …)}). So a ticket taken there finds the chunk loaded (no load at all) and the
 * chunk is never without a hold. That removal, with nothing of ours holding the chunk, is what "did not stay loaded"
 * was (READ; why it bit on 2026-11-24 and not on the five boots before it is UNKNOWN).
 *
 * <p><b>a2's guard stays anyway:</b> a take is attempted ONLY on a chunk {@link Chunks#isLoaded} reports loaded
 * immediately before it. A chunk found unloaded is requested again, up to {@link #MAX_REQUESTS} requests in all; after
 * that the acquire fails ("did not stay loaded", with held and attempts) and takes nothing more.
 *
 * <p>One thread: every callback runs on the server's main thread (above), and the callers start and end on it.
 */
final class SelfTestPadTickets {

    /** At most this many requests per chunk in total (THE SEAT'S MECHANISM NUMBER, 2026-11-24, not Ben's). */
    static final int MAX_REQUESTS = 3;

    /** The seam: what the pad needs of the chunk system, and nothing more. */
    interface Chunks {
        boolean isLoaded(int x, int z);

        /** Take the plugin's ticket on a chunk the caller has just seen loaded; true when it was taken. */
        boolean take(int x, int z);

        /** Remove the plugin's ticket; true when one was held (READ: false, and harmless, when none is). */
        boolean release(int x, int z);

        /** Request the chunk's async load; {@code loaded} runs when it completes (or fails), on the main thread. */
        void request(int x, int z, Runnable loaded);
    }

    record Chunk(int x, int z) {}

    /**
     * What an acquire ended in: OK with all held, or a failure naming the chunk, held and attempts. {@code line} is the
     * {@code PAD tickets held=<n>} line, set only on the acquire that took the last ticket.
     */
    record Outcome(boolean ok, int held, String why, String line) {}

    /** The footprint's chunks, x outer and z inner, from {@link SelfTestPadShape#chunks}'s inclusive span. */
    static List<Chunk> footprint(int[] span) {
        List<Chunk> out = new ArrayList<>();
        for (int x = span[0]; x <= span[2]; x++) {
            for (int z = span[1]; z <= span[3]; z++) out.add(new Chunk(x, z));
        }
        return List.copyOf(out);
    }

    /** The release's cause, from the run's end reason: end, stop or abort (quit and disable are named by their paths). */
    static String causeOf(String why) {
        if (why.startsWith("FINISHED")) return "end";
        if (why.startsWith("STOPPED")) return "stop";
        return "abort";
    }

    static String releasedLine(int n, String cause) {
        return "PAD tickets released=" + n + " cause=" + cause;
    }

    private final Chunks chunks;
    private final List<Chunk> footprint;
    private final Map<Chunk, Integer> requests = new HashMap<>();
    private final Set<Chunk> held = new LinkedHashSet<>();
    private Consumer<Outcome> onDone;
    private boolean complete;   // all held
    private boolean failed;
    private boolean released;

    SelfTestPadTickets(Chunks chunks, int[] span) {
        this.chunks = chunks;
        this.footprint = footprint(span);
    }

    int held() {
        return held.size();
    }

    /** Request every chunk of the footprint and take each ticket as its load lands; {@code done} is called once. */
    void acquire(Consumer<Outcome> done) {
        if (complete) {   // a second pad step of the same run: all twelve are already held
            done.accept(new Outcome(true, held.size(), null, null));
            return;
        }
        if (failed || released) {
            done.accept(new Outcome(false, held.size(), "the pad's tickets were already given up", null));
            return;
        }
        this.onDone = done;
        for (Chunk c : footprint) request(c);
    }

    private void request(Chunk c) {
        requests.merge(c, 1, Integer::sum);
        chunks.request(c.x(), c.z(), () -> landed(c));
    }

    private void landed(Chunk c) {
        if (complete || failed || released || held.contains(c)) return;   // nothing is taken after the acquire is over
        if (!chunks.isLoaded(c.x(), c.z())) {   // a2: NEVER a take on a chunk reported unloaded
            if (requests.get(c) < MAX_REQUESTS) {
                request(c);
            } else {
                fail("chunk " + c.x() + "," + c.z() + " did not stay loaded held=" + held.size()
                        + " attempts=" + requests.get(c));
            }
            return;
        }
        if (!chunks.take(c.x(), c.z())) {
            fail("chunk " + c.x() + "," + c.z() + " ticket refused held=" + held.size());
            return;
        }
        held.add(c);
        if (held.size() == footprint.size()) {
            complete = true;
            onDone.accept(new Outcome(true, held.size(), null, "PAD tickets held=" + held.size()));
        }
    }

    private void fail(String why) {
        failed = true;
        onDone.accept(new Outcome(false, held.size(), why, null));
    }

    /** Release exactly what was taken, once: the count released; 0 on every later call. */
    int release() {
        if (released) return 0;
        released = true;
        int n = 0;
        for (Chunk c : held) {
            chunks.release(c.x(), c.z());
            n++;
        }
        return n;   // ONCE is the flag above, alone: held is kept, so a second call is refused by it and nothing else
    }

    /** The footprint, for the alarm check after all are held (a4). */
    List<Chunk> footprint() {
        return footprint;
    }
}
