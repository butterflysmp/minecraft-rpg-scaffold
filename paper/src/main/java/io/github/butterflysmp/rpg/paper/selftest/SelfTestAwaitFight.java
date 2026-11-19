package io.github.butterflysmp.rpg.paper.selftest;

import java.util.OptionalInt;

/**
 * AWAIT_FIGHT {@code "<ceiling ticks>"} (the F8 brief, section 2; GATE-fight-site.md): wait, a tick at a time, until
 * the dev command's fight is no longer running, or until the ceiling. It logs which happened, with the ticks waited:
 * <ul>
 *   <li>{@code AWAIT fight ended ticks=<n>};</li>
 *   <li>{@code AWAIT fight ceiling ticks=<c> still running}.</li>
 * </ul>
 * GATE-fight-mage.md's MQ7 waited its full 1800 ticks after Ben had killed the Mage at 460; this ends the wait when the
 * fight does. Pure, so the grammar and the ending are tested without a server.
 */
final class SelfTestAwaitFight {

    private SelfTestAwaitFight() {}

    /** One whole number of ticks, at least 1; anything else is refused at load. */
    static OptionalInt parse(String arg) {
        String a = arg == null ? "" : arg.trim();
        if (!a.matches("\\d+")) return OptionalInt.empty();
        try {
            int ceiling = Integer.parseInt(a);
            return ceiling >= 1 ? OptionalInt.of(ceiling) : OptionalInt.empty();
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    /** Whether the wait is over: the fight has ended, or the ceiling is reached. */
    static boolean done(boolean running, int waited, int ceiling) {
        return !running || waited >= ceiling;
    }

    /** The line the run logs when the wait ends. */
    static String line(boolean running, int waited, int ceiling) {
        return running ? "AWAIT fight ceiling ticks=" + ceiling + " still running" : "AWAIT fight ended ticks=" + waited;
    }
}
