package io.github.butterflysmp.rpg.paper.health;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One viewer's per-mob nameplate bookkeeping, driving the text-on-change / visibility-per-cycle
 * optimization. Lives inside a single viewer's LOS loop, so it is single-threaded (the viewer's
 * thread) and needs no synchronization.
 *
 * The optimization: the name TEXT only changes on a health change (tracked by a per-mob version), but
 * VISIBILITY changes as the viewer moves. So the loop re-asserts visibility every cycle but only
 * resends the text when it actually changed -- which is cheap, since the Component encoding is the
 * expensive part.
 *
 * Two DISTINCT reasons to (re)send the text, and both must fire (this is the load-bearing subtlety):
 *  - FIRST SIGHT: this viewer has never been sent this mob's text (no version recorded). An UNDAMAGED
 *    mob whose text never changed still needs its text on the viewer's first sight, or a newly-arrived
 *    player sees a mob with no nameplate. First sight is NOT the same event as a version change.
 *  - VERSION CHANGED: the mob's health changed, so the cached text was rebuilt with a new version.
 */
public final class ViewerNameplateState {

    /** The mob's nameplate version this viewer was last sent, per mob. Absent = never sent (first sight). */
    private final Map<UUID, Long> sentVersion = new HashMap<>();

    /** A single mob's send decision for this cycle. */
    public record SendDecision(boolean includeName, boolean visible) {}

    /**
     * Decide what to send this viewer for {@code mobId} this cycle, and record the send. The name is
     * included on first sight (never sent) OR when the version changed; visibility (LOS) is always
     * asserted.
     */
    public SendDecision decide(UUID mobId, long version, boolean lineOfSight) {
        Long lastSent = sentVersion.get(mobId);
        boolean firstSight = lastSent == null;
        boolean versionChanged = !firstSight && lastSent != version;
        boolean includeName = firstSight || versionChanged;
        if (includeName) sentVersion.put(mobId, version);
        return new SendDecision(includeName, lineOfSight);
    }

    /** Drop bookkeeping for mobs no longer in range this cycle, so the map stays bounded. */
    public void retainInRange(Set<UUID> inRange) {
        sentVersion.keySet().retainAll(inRange);
    }

    /** How many mobs this viewer is tracking. For tests/bounds. */
    public int trackedCount() {
        return sentVersion.size();
    }
}
