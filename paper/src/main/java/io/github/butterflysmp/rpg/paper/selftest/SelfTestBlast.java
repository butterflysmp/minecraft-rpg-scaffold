package io.github.butterflysmp.rpg.paper.selftest;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The selftest's lines and grammar for the Phage's blast (PLAN-dragon-mobs.md step R3; GATE-dragon-blast.md). Pure,
 * so {@code SelfTestBlastStepsTest} pins each exact text. It reads; it never judges.
 *
 * <ul>
 *   <li><b>HURT</b>, the player's damage line: every DAMAGE change the store makes to the run's player while a row
 *       runs, with the amount OFFERED (before defence), the amount that LANDED, the defence between them, and the
 *       dealer. GATE-dragon-fix.md D5 stayed UNVERIFIED and DB10R2 mis-predicted because no such line existed;
 *       predictions about damage are now written against this line, never against a health difference.</li>
 *   <li><b>FUSE</b>: a tagged creeper's swell, {@code Creeper#getFuseTicks} (the server's {@code swell} field) over
 *       {@code getMaxFuseTicks} ({@code maxSwell}), and whether it is ignited.</li>
 *   <li><b>AWAIT_FUSE</b> {@code "<tag> <min fuse> <timeout ticks>"}: wait, a tick at a time, until the tagged
 *       creeper's swell reaches {@code min}, then log its FUSE line; or log that it did not within the timeout.</li>
 *   <li><b>DETONATE</b> {@code <tag>}: {@code Creeper#explode()}, which calls the server's {@code explodeCreeper()}
 *       directly and so raises the same {@code ExplosionPrimeEvent} a completed swell does. The instrument's way to
 *       blow a FROZEN Phage at a known spot.</li>
 *   <li><b>IGNITE</b> {@code <tag>}: {@code Creeper#ignite()}, the route flint and steel takes
 *       ({@code Creeper.mobInteract} calls it; {@code CustomCreeperGuard}'s javadoc).</li>
 * </ul>
 */
final class SelfTestBlast {

    private SelfTestBlast() {}

    /** AWAIT_FUSE's argument. */
    record Await(String tag, int minFuse, int timeoutTicks) {}

    static Optional<Await> parseAwait(String arg) {
        String[] w = arg.trim().split("\\s+");
        if (w.length != 3 || w[0].isEmpty()) return Optional.empty();
        try {
            int min = Integer.parseInt(w[1]);
            int timeout = Integer.parseInt(w[2]);
            if (min < 1 || timeout < 1) return Optional.empty();
            return Optional.of(new Await(w[0], min, timeout));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** A step whose argument is exactly one tag. */
    static boolean oneTag(String arg) {
        String t = arg.trim();
        return !t.isEmpty() && !t.contains(" ");
    }

    static String hurtLine(double before, double after, double defense, String dealer, String dealerType) {
        return String.format(Locale.ROOT, "HURT before=%.3f after=%.3f defense=%.3f dealer=%s dealerType=%s",
                before, after, defense, dealer, dealerType);
    }

    static String fuseLine(String tag, UUID id, int fuse, int max, boolean ignited) {
        return "FUSE tag=" + tag + " " + id + " fuse=" + fuse + "/" + max + " ignited=" + ignited;
    }
}
