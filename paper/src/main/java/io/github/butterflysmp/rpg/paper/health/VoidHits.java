package io.github.butterflysmp.rpg.paper.health;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which store hit was a VOID tick, so the selftest's HURT line can name it ({@code cause=void}). The void handler marks
 * the victim with the server tick at which it priced the hit; the HURT line takes the mark.
 *
 * <p><b>BOUNDED IN TIME, BECAUSE A MARK NOBODY TAKES MUST NOT LABEL A LATER HIT.</b> Outside a selftest row no HURT
 * line runs, so a mark from an ordinary void death would otherwise sit until the next row's first hit and call a
 * lightning strike the void. The damage is applied on the victim's own scheduler, at most a tick or two after the
 * mark, so a mark older than {@link #WITHIN_TICKS} is stale and is dropped, not taken.
 */
public final class VoidHits {

    /** A mark is the void's only within this many ticks of being made. */
    static final int WITHIN_TICKS = 2;

    private final Map<UUID, Integer> markedAt = new ConcurrentHashMap<>();

    /** The void handler priced a hit on {@code victim} at server tick {@code tick}. */
    public void mark(UUID victim, int tick) {
        markedAt.put(victim, tick);
    }

    /** True, once, if {@code victim}'s store hit at {@code tick} is the void's. A stale mark is dropped. */
    public boolean take(UUID victim, int tick) {
        Integer at = markedAt.remove(victim);
        return at != null && tick - at >= 0 && tick - at <= WITHIN_TICKS;
    }

    /** On quit, so the map cannot grow with players who left. */
    public void forget(UUID victim) {
        markedAt.remove(victim);
    }
}
