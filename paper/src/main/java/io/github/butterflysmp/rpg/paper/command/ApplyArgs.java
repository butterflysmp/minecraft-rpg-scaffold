package io.github.butterflysmp.rpg.paper.command;

import java.util.function.Predicate;

/**
 * The parse/validate step of {@code /rpg apply}, pulled out of Brigadier so it is testable
 * without a server: defaults for the optional args, the loop-safety clamp on stacks, and the
 * unknown-status error. The command's three arity nodes call {@link #resolve} with {@code null}
 * for any trailing argument the player omitted, so the defaults live here, unit-tested, rather
 * than being an untestable property of the command tree's shape.
 */
public record ApplyArgs(String statusId, int durationTicks, int stacks) {

    static final int DEFAULT_DURATION = 100;
    static final int DEFAULT_STACKS = 1;
    /** Past ~9 stacks the 0.6 floor pins the slow, so more is inert -- and this bounds the apply loop. */
    static final int MAX_STACKS = 20;

    /** Either resolved args or a human error message -- never both. */
    public record Resolution(ApplyArgs args, String error) {
        public boolean ok() { return args != null; }
        static Resolution ok(ApplyArgs args) { return new Resolution(args, null); }
        static Resolution error(String message) { return new Resolution(null, message); }
    }

    /**
     * Apply defaults (null duration/stacks become the defaults), clamp stacks into
     * {@code [1, MAX_STACKS]} (defence-in-depth on the apply loop, even if the tree's max
     * changes), and reject an unknown status with a named message. {@code known} tests whether
     * a status id is loaded.
     */
    public static Resolution resolve(String statusId, Integer duration, Integer stacks,
                                     Predicate<String> known) {
        if (!known.test(statusId)) {
            return Resolution.error("Unknown status: " + statusId);
        }
        int dur = duration == null ? DEFAULT_DURATION : Math.max(1, duration);
        int stk = stacks == null ? DEFAULT_STACKS : Math.min(MAX_STACKS, Math.max(1, stacks));
        return Resolution.ok(new ApplyArgs(statusId, dur, stk));
    }
}
