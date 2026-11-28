package io.github.butterflysmp.rpg.core.combat;

import java.util.Locale;
import java.util.UUID;

/**
 * ONE DOUSE TRACE LINE (the seat's Y7 and E1, 2026-11-27), written while trace is on. Core builds it; the adapter
 * supplies the TICK and gates it on the trace switch. The three shapes, pinned by {@code DouseTest}:
 *
 * <pre>
 * SCORCH doused target=&lt;type uuid&gt; cause=&lt;water|rain|splash&gt; tick=&lt;t&gt;
 * SCORCH refused target=&lt;type uuid&gt; cause=&lt;water|rain&gt; tick=&lt;t&gt;
 * IGNITE refused target=&lt;type uuid&gt; cause=&lt;water|rain&gt; tick=&lt;t&gt;
 * </pre>
 */
public record DouseLine(Event event, String targetType, UUID target, Douse.Cause cause) {

    /** Which line. */
    public enum Event {
        /** A live Scorch put out: at a burn tick (water, rain) or at a splash. */
        DOUSED("SCORCH doused"),
        /** An application refused on a wet body (Y3). */
        REFUSED("SCORCH refused"),
        /** A detonation refused on a wet body at its death (E1). */
        IGNITE_REFUSED("IGNITE refused");

        private final String head;

        Event(String head) {
            this.head = head;
        }
    }

    public String format(long tick) {
        return String.format(Locale.ROOT, "%s target=%s %s cause=%s tick=%d",
                event.head, targetType, target, cause.key(), tick);
    }
}
