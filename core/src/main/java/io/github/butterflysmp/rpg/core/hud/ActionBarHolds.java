package io.github.butterflysmp.rpg.core.hud;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who owns a player's action bar at tick t: the stats line, or a message HOLDING it until tick T.
 * Mining S1-c (the seat's mechanism rulings 1-3, 2026-11-14), for Ben's "Is it possible to have the
 * pickaxe message over ride the action bar for 1.5s".
 *
 * <p><b>The rule.</b> A hold started at tick {@code s} for {@code n} ticks owns the bar for
 * {@code s <= t < s + n}. The stats bar asks on each of its own beats ({@link #beat}): while a hold is
 * live the beat RE-SENDS the held message (so its time on screen never depends on how long a client
 * keeps an action bar up), and the first beat at or after {@code s + n} draws the stats line again and
 * reports the end, once. A newer hold REPLACES an older one. {@link #clear} ends a hold (quit). Nothing
 * is persisted: this is a map, lost on restart.
 *
 * <p><b>NAME THE QUANTITY:</b> ticks are SERVER ticks, integers. A hold of 30 is 1.5 s only at 20 TPS.
 *
 * <p><b>THREADING.</b> A player's entry is read and written by that player's own thread only: the
 * stats bar's per-player loop (the entity scheduler) and the input that starts a hold (a Bukkit event
 * for that player). On Paper both are the main thread. The map is concurrent and each write is a
 * single atomic operation ({@code put}, {@code remove(key, value)}), so entries of different players
 * may also be touched from different region threads (Folia) safely. The generic {@code M} is the
 * message, so core never sees an Adventure {@code Component}.
 */
public final class ActionBarHolds<M> {

    /**
     * One hold. {@code traced} says whether its end is logged (the dev trace was on for the player when
     * it started).
     */
    public record Hold<M>(M message, String reason, long start, long until, boolean traced) { }

    /** What one beat of the stats bar does. */
    public sealed interface Beat<M> permits Beat.Stats, Beat.Held, Beat.Released {
        /** No hold: draw the stats line. */
        record Stats<M>() implements Beat<M> { }
        /** A live hold: RE-SEND its message, and do not draw the stats line. */
        record Held<M>(Hold<M> hold) implements Beat<M> { }
        /** The first beat at or after the hold's end: draw the stats line, and report the end once. */
        record Released<M>(Hold<M> hold) implements Beat<M> { }
    }

    private final ConcurrentHashMap<UUID, Hold<M>> holds = new ConcurrentHashMap<>();

    /**
     * Starts this player's hold, REPLACING any older one. @param now the current server tick
     * @param ticks how long the message owns the bar; must be positive
     * @return the hold, whose {@code until} is {@code now + ticks}
     */
    public Hold<M> hold(UUID player, M message, String reason, long now, int ticks, boolean traced) {
        if (ticks <= 0) throw new IllegalArgumentException("a hold needs at least one tick, got " + ticks);
        Hold<M> hold = new Hold<>(message, reason, now, now + ticks, traced);
        holds.put(player, hold);
        return hold;
    }

    /** One beat of this player's stats bar at tick {@code now}. Releases an ended hold. */
    public Beat<M> beat(UUID player, long now) {
        Hold<M> hold = holds.get(player);
        if (hold == null) return new Beat.Stats<>();
        if (now < hold.until()) return new Beat.Held<>(hold);
        // Remove THIS hold only: a newer one that replaced it in between must survive.
        holds.remove(player, hold);
        return new Beat.Released<>(hold);
    }

    /**
     * Is a hold LIVE for this player at tick {@code now}: started, and {@code now < until}? Mining S2
     * (the seat's ruling 8): the DENIED sound plays only when a hold STARTS with none already live. A
     * hold whose end has passed is not live even before a beat has released it.
     */
    public boolean isLive(UUID player, long now) {
        Hold<M> hold = holds.get(player);
        return hold != null && now < hold.until();
    }

    /** Ends this player's hold (on quit). */
    public void clear(UUID player) {
        holds.remove(player);
    }

    /** The end line, logged by the stats bar when a traced hold is released. */
    public static String endLine(String player, Hold<?> hold, long tick) {
        return String.format(Locale.ROOT, "HUD hold-end player=%s reason=%s start=%d until=%d tick=%d",
                player, hold.reason(), hold.start(), hold.until(), tick);
    }
}
