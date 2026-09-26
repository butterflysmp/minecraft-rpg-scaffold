package io.github.butterflysmp.rpg.core.build;

/**
 * NO FALL DAMAGE FROM THE LANDING RIGHT AFTER A LEAP (ruling 31; PLAN-build-system.md section 7.6). A mark armed
 * by a dash with {@code safe_landing}, stepped once per tick with what the server reports about the player, and
 * IMMUNE until it clears.
 *
 * <p>It clears:
 * <ul>
 *   <li>on the tick AFTER the first ground contact following take-off. The landing tick itself stays immune,
 *       because the landing move's FALL event and the per-tick reading are in an order nobody has traced;</li>
 *   <li>when a fall that had started is absorbed mid-air (fall distance back to 0 while not on the ground: water,
 *       a ladder, a cobweb) -- there is no landing left to protect;</li>
 *   <li>if the leap never leaves the ground within {@link #TAKE_OFF_TICKS} (a ceiling);</li>
 *   <li>by the {@link #BACKSTOP_TICKS} backstop, whatever else happened.</li>
 * </ul>
 * The paper side also clears it on the FALL event it cancels, on death and on quit.
 *
 * <p>The ruling's words cover the whole first landing however far it is: a leap off a cliff lands free. A height
 * cap would be a rule nobody made.
 *
 * <p>A record: an immutable value; {@link #step} returns the next state.
 */
public record SafeLanding(Phase phase, int ticks, boolean fell) {

    public enum Phase { ARMED, AIRBORNE, LANDED, CLEARED }

    /** Ticks a mark may wait on the ground for the impulse to lift the player before it gives up. */
    public static final int TAKE_OFF_TICKS = 4;

    /** 10 s, about 11 times the ~22-tick flat leap (an estimate, section 7.5). */
    public static final int BACKSTOP_TICKS = 200;

    public static SafeLanding armed() {
        return new SafeLanding(Phase.ARMED, 0, false);
    }

    public boolean immune() {
        return phase != Phase.CLEARED;
    }

    public boolean cleared() {
        return phase == Phase.CLEARED;
    }

    /** One tick's reading. */
    public SafeLanding step(boolean onGround, double fallDistance) {
        if (phase == Phase.CLEARED) return this;
        int t = ticks + 1;
        if (t > BACKSTOP_TICKS) return clearedAt(t);
        return switch (phase) {
            case ARMED -> onGround
                    ? (t > TAKE_OFF_TICKS ? clearedAt(t) : new SafeLanding(Phase.ARMED, t, false))
                    : new SafeLanding(Phase.AIRBORNE, t, fallDistance > 0);
            case AIRBORNE -> {
                if (onGround) yield new SafeLanding(Phase.LANDED, t, fell);
                if (fell && fallDistance <= 0) yield clearedAt(t);
                yield new SafeLanding(Phase.AIRBORNE, t, fell || fallDistance > 0);
            }
            case LANDED, CLEARED -> clearedAt(t);
        };
    }

    private static SafeLanding clearedAt(int t) {
        return new SafeLanding(Phase.CLEARED, t, false);
    }
}
