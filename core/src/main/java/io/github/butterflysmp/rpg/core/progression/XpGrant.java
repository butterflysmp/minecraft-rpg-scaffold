package io.github.butterflysmp.rpg.core.progression;

import java.util.OptionalLong;

/**
 * What {@code /rpg playerxp <add|set> <player> <amount> <levels|xp>} resolves to.
 *
 * <p>Four combinations, one lifetime-XP total out. Pure, so the whole of the command's arithmetic
 * is unit-testable and the adapter in {@code paper} is left with parsing and printing.
 *
 * <h2>*** "levels" IS A LOOKUP HERE AND WAS A DIRECT WRITE IN THE PREDECESSOR ***</h2>
 *
 * The predecessor stored {@code (level, progress-into-level)} as two fields, so
 * {@code set 13 levels} wrote {@code 13} into a field and left progress alone.
 * <b>We store lifetime XP and derive the level</b> ({@link PlayerLevel}), so there is no level
 * field to write and the unit has to be converted:
 *
 * <pre>
 *   set N levels  ->  lifetime = totalForLevel(N)                     EXACTLY the threshold
 *   add N levels  ->  lifetime += totalForLevel(L+N) - totalForLevel(L)
 * </pre>
 *
 * <b>Both go through {@link PlayerLevel}'s prefix-sum table. There is no second path</b>, and there
 * must not be: a level threshold computed anywhere else is a second copy of the curve.
 *
 * <h2>AND THE TWO ARMS DIVERGE FROM THE PREDECESSOR IN OPPOSITE DIRECTIONS, WHICH IS WORTH SAYING</h2>
 *
 * <ul>
 *   <li><b>{@code add} reproduces it exactly.</b> There, {@code add 1 levels} raised the level field
 *       and left progress untouched -- which in our units is {@code lifetime += the rung}, because
 *       {@code T(L+1) + p} minus {@code T(L) + p} is the rung. <b>Partial progress survives</b>,
 *       and that falls out rather than being arranged.</li>
 *   <li><b>{@code set} deliberately does not.</b> There, {@code set 13 levels} kept whatever partial
 *       XP you had. Here it lands on {@code 19,980} with <b>zero progress into 13</b>. Ben's
 *       ruling, and the reason is staging: an operator setting a gate threshold wants to be ON it,
 *       not past it by an unknown amount.</li>
 * </ul>
 *
 * <p>The divergence is recorded because the predecessor's behaviour is the thing a reader will
 * compare against, and one of these two would otherwise look like a porting slip.
 */
public final class XpGrant {

    private XpGrant() {}

    /**
     * The predecessor's refusal, ported verbatim because it is right: it names the operation that
     * DOES take an absolute value, so the operator's next keystroke is obvious.
     */
    public static final String ADD_REFUSES_NEGATIVE =
            "'add' requires a non-negative amount; use 'set' for absolute values.";

    /** Add to what is there, or replace it. */
    public enum Op { ADD, SET }

    /** The unit the amount is quoted in. */
    public enum Unit { XP, LEVELS }

    /**
     * The lifetime-XP total this command should leave the player on.
     *
     * <h2>THERE IS NO INT CHUNKING HERE, AND THE REASON IT EXISTED IS WORTH RECORDING</h2>
     *
     * The predecessor looped: {@code while (remaining > 0) { addPlayerXp(min(remaining, MAX_INT)); }}
     * <b>because its {@code addPlayerXp} took an {@code int} and the command took a {@code long}.</b>
     * It was a type bridge, not a policy.
     *
     * <p><b>Ours is {@code long} end to end, so the loop has nothing left to do.</b> Written down
     * because <b>a loop with no reason left in it is exactly what someone re-adds for symmetry</b>
     * -- and re-adding it here would reintroduce a partial-application window for no gain.
     *
     * @return the new total, or <b>EMPTY when the command is REFUSED</b> -- which happens only for
     *         {@code add} with a negative amount, in either unit. The caller prints
     *         {@link #ADD_REFUSES_NEGATIVE}.
     */
    public static OptionalLong targetLifetimeXp(Op op, Unit unit, long amount, long currentLifetimeXp) {
        // THE REFUSAL COVERS BOTH UNITS, AND THE PREDECESSOR'S COVERED ONLY ONE. Its check sat in
        // the xp branch, so `add -3 levels` there quietly DEMOTED a player through the same command
        // that refuses `add -3 xp`. Tightened rather than ported: the message is about `add`, not
        // about xp, and `set` reaches every value this now refuses.
        if (op == Op.ADD && amount < 0) return OptionalLong.empty();

        return OptionalLong.of(switch (unit) {
            case XP -> op == Op.ADD
                    ? PlayerLevel.plus(currentLifetimeXp, amount)
                    : Math.max(0L, amount);
            case LEVELS -> op == Op.ADD
                    ? addLevels(currentLifetimeXp, amount)
                    : PlayerLevel.totalForLevel(clampLevel(amount));
        });
    }

    /**
     * Raise the level by {@code levels}, keeping progress into the current one.
     *
     * <p>The delta is taken between two thresholds rather than by writing the new threshold, which
     * is what preserves the partial. At the cap the delta is zero and nothing moves.
     */
    private static long addLevels(long currentLifetimeXp, long levels) {
        int from = PlayerLevel.levelFor(currentLifetimeXp);
        // SATURATING, NOT `from + levels`. `/rpg playerxp add <player> 9223372036854775807 levels`
        // is reachable from a keyboard, and a plain sum WRAPS TO Long.MIN_VALUE -- which clamps to
        // level 1, makes the delta zero, and silently does nothing.
        //
        // *** THIS WAS WRITTEN AS `(long) from + levels` FIRST, WITH A JAVADOC ON clampLevel
        // CLAIMING THE long WIDENING PREVENTED THE OVERFLOW. IT DOES NOT. *** Widening `from` to
        // long stops an INT overflow; the sum is still a long sum and still wraps. The claim was
        // about the cast, the defect was in the addition, and the row below is what found it.
        int to = clampLevel(PlayerLevel.plus(from, levels));
        return PlayerLevel.plus(currentLifetimeXp,
                PlayerLevel.totalForLevel(to) - PlayerLevel.totalForLevel(from));
    }

    /**
     * Fold any number onto a real level, so {@link PlayerLevel#totalForLevel} -- which throws
     * outside {@code 1..99} by design -- is only ever handed a legal one.
     *
     * <p><b>This clamps and does not protect the arithmetic that produced its argument.</b> The
     * caller saturates before calling; see {@link #addLevels}.
     */
    private static int clampLevel(long level) {
        if (level < 1) return 1;
        if (level > PlayerLevel.MAX_LEVEL) return PlayerLevel.MAX_LEVEL;
        return (int) level;
    }
}
