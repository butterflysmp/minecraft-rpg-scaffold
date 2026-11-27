package io.github.butterflysmp.rpg.core.enchant;

import java.util.Locale;

/**
 * DRUMMING BEAT'S DECISION, PURE (the seat's M4, M5 and M8, 2026-11-26). Asked once per primary melee hit that lands on a
 * mob, BEFORE its crit is rolled, with the player's streak as it stands; it answers the n the hit is rolled at, the bonus
 * to add to the crit chance for THAT hit, what happened, and the streak to keep if the hit lands. Nothing here touches a
 * world, a clock or a random draw: the caller passes the tick and decides whether to keep {@link Decision#after}.
 *
 * <p><b>The rules (the seat's M4, M5; Ben's "Same conditions", "no bonus ceiling", "a crit doesn't reset it"):</b>
 * <ul>
 *   <li>No {@link EnchantBehaviour.DrummingBeat} active on the weapon in hand (level below 1, or another weapon):
 *       {@link Result#PLAIN}, no bonus, and the streak is GONE (the club left the hand, or its Drumming Beat ceased).</li>
 *   <li>Below full charge: {@link Result#RESET_UNCHARGED}, rolled with NO bonus, and NOT counted: the streak is gone.</li>
 *   <li>More than {@code gapTicks} since the last counted hit (from that hit's tick to this one's; exactly
 *       {@code gapTicks} continues): {@link Result#RESET_GAP}; the streak restarts AT this hit, which is its first (no
 *       bonus, counted as 1).</li>
 *   <li>Otherwise {@link Result#COUNTED}: rolled at n with {@code step x n / 100}, and n becomes n + 1.</li>
 * </ul>
 * <p>NO CEILING: the bonus is never capped here; {@code Crit.chance} clamps the sum at 1 at the roll. A CRIT cannot reach
 * this method (it takes no crit), so a crit neither resets nor changes n.
 */
public final class DrumBeats {

    private DrumBeats() {}

    /** The highest pitch Minecraft plays a sound at (M7: "up to the sound's limit"). */
    public static final float MAX_PITCH = 2.0f;

    /**
     * A player's streak: {@code n} consecutive counted hits, the last at {@code lastTick}. {@link #NONE} is no streak.
     * A {@code record} is a small immutable value class whose fields, constructor, equals and toString Java writes itself.
     */
    public record Streak(int n, long lastTick) {
        public static final Streak NONE = new Streak(0, Long.MIN_VALUE);
    }

    /** What one hit did. {@link #token()} is the trace's word (M8). */
    public enum Result {
        COUNTED("counted"), RESET_UNCHARGED("reset:uncharged"), RESET_GAP("reset:gap"), PLAIN("plain");

        private final String token;

        Result(String token) {
            this.token = token;
        }

        public String token() {
            return token;
        }
    }

    /**
     * @param n      the streak this hit is rolled at (0 after a gap reset: it is the first)
     * @param bonus  the fraction added to the hit's crit chance (0 unless counted)
     * @param result what happened
     * @param after  the streak to keep if the hit lands
     */
    public record Decision(int n, double bonus, Result result, Streak after) {}

    public static Decision decide(Streak before, long now, boolean charged, EnchantBehaviour.DrummingBeat beat, int level) {
        Streak s = before == null ? Streak.NONE : before;
        if (beat == null || level < 1) return new Decision(s.n(), 0.0, Result.PLAIN, Streak.NONE);
        if (!charged) return new Decision(s.n(), 0.0, Result.RESET_UNCHARGED, Streak.NONE);
        if (s.n() > 0 && now - s.lastTick() > beat.gapTicks()) {
            return new Decision(0, 0.0, Result.RESET_GAP, new Streak(1, now));
        }
        return new Decision(s.n(), bonus(beat.stepPercentFor(level), s.n()), Result.COUNTED, new Streak(s.n() + 1, now));
    }

    /** {@code stepPercent x n / 100}: the product is a whole number of points for every shipped step, so it is exact. */
    public static double bonus(double stepPercent, int n) {
        return stepPercent * n / 100.0;
    }

    /** M7 (SEAT PLACEHOLDER 2026-11-26, the old game's): the drum's pitch for a streak of {@code nAfter}, to the limit. */
    public static float pitch(int nAfter) {
        return Math.min(MAX_PITCH, 0.7f + 0.2f * nAfter);
    }

    /**
     * M8, THE TRACE LINE: {@code STREAK n=<n> step=<points> chance=<the chance rolled, a fraction> charged=<b>
     * result=<token>}, logged beside the hit's PLAYERHIT while trace is on. {@code step} is the level's step in points (0
     * on a plain hit); {@code chance} the caster's resolved chance plus the bonus, UNCLAMPED (no ceiling: it may read above
     * 1.000), to three decimals.
     */
    public static String line(Decision d, double stepPercent, double chance, boolean charged) {
        return String.format(Locale.ROOT, "STREAK n=%d step=%s chance=%.3f charged=%s result=%s",
                d.n(), points(stepPercent), chance, charged, d.result().token());
    }

    private static String points(double p) {
        return p == Math.rint(p) ? Long.toString((long) p) : Double.toString(p);
    }
}
