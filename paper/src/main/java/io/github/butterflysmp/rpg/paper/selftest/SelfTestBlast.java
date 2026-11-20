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

    /**
     * As above, naming the fight dragon's attack (F9b, section 4: "HURT lines must name which attack"). With no attack
     * the line is the one above, word for word.
     */
    static String hurtLine(double before, double after, double defense, String dealer, String dealerType,
                           String attack) {
        String line = hurtLine(before, after, defense, dealer, dealerType);
        return attack == null ? line : line + " attack=" + attack;
    }

    /**
     * As above, naming the CAUSE where the store's hit came from a rule rather than a dealer (Ben's void ruling,
     * 2026-11-19: {@code cause=void}). With no cause the line is the one above, word for word.
     */
    static String hurtLine(double before, double after, double defense, String dealer, String dealerType,
                           String attack, String cause) {
        String line = hurtLine(before, after, defense, dealer, dealerType, attack);
        return cause == null ? line : line + " cause=" + cause;
    }

    static String fuseLine(String tag, UUID id, int fuse, int max, boolean ignited) {
        return "FUSE tag=" + tag + " " + id + " fuse=" + fuse + "/" + max + " ignited=" + ignited;
    }

    /**
     * Since the void implosion's Part C the FUSE is OURS ({@code PhageFuser}, {@code MobFuse}), not the server's
     * {@code swell}, which stays 0: {@code fuse=<swell>/<fuse ticks> dir=<+1|-1>}, or {@code fuse=none} for a creeper
     * with no fuse of ours; then whether the creeper is ignited (the server's flag, which the guard keeps false).
     */
    static String fuseLine(String tag, UUID id, java.util.Optional<int[]> state, boolean ignited) {
        if (state.isEmpty()) return "FUSE tag=" + tag + " " + id + " fuse=none ignited=" + ignited;
        int[] s = state.get();
        return "FUSE tag=" + tag + " " + id + " fuse=" + s[0] + "/" + s[2] + " dir=" + (s[1] > 0 ? "+1" : "-1")
                + " ignited=" + ignited;
    }

    /** THAW's line: the mob's AI READ BACK after the act, so it witnesses the thaw landing (the follow-up, F4). */
    static String thawLine(String tag, UUID id, boolean aiAfter) {
        return "DRIVE thaw tag=" + tag + " " + id + " ai=" + aiAfter;
    }

    /** PROBE_MOTION's line: the mob's velocity, each component to four places. */
    static String motionLine(String tag, UUID id, double vx, double vy, double vz) {
        return String.format(Locale.ROOT, "MOTION tag=%s %s vx=%.4f vy=%.4f vz=%.4f", tag, id, vx, vy, vz);
    }
}
