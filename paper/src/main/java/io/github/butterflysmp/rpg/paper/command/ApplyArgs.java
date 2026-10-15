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

    /**
     * The reply when the looked-at mob is IMMUNE to the status, or empty when it is not (the seat's
     * ruling 5, 2026-10-01: the reply must say it was refused, and why).
     *
     * <p>The command used to reply "Applied ..." before the entity thread ran the status arm, where the
     * immunity refusal lives, so a refused application read exactly like a landed one. The command now
     * asks this FIRST, on the same region task that found the target, and replies with this instead of
     * applying. The arm in {@code BukkitCombatant} keeps its own check for every other caller.
     *
     * @param typeKey the target's entity type key, as {@code MOBSEED} prints it ({@code wither_skeleton})
     */
    public static java.util.Optional<String> immunityRefusal(
            io.github.butterflysmp.rpg.paper.content.StatusDefinition status, String typeKey) {
        if (status instanceof io.github.butterflysmp.rpg.paper.content.StatusDefinition.Wither wither
                && wither.isImmune(typeKey)) {
            return java.util.Optional.of("Refused: " + typeKey + " is immune to " + status.id()
                    + " (statuses/" + status.id() + ".yml); nothing was applied");
        }
        return java.util.Optional.empty();
    }
}
