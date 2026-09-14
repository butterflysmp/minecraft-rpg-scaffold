package io.github.butterflysmp.rpg.core.combat;

/**
 * WHERE THE ARROWS OF A RELEASE POINT: a symmetric fan, ten degrees between neighbours.
 *
 * <p>Ruled by the operator. A five-arrow release fans at <b>{@code 20 10 0 -10 -20}</b>, and the
 * general rule is <i>ten degrees, centred</i> -- the ruled five being that rule evaluated at
 * {@code n = 5}.
 *
 * <pre>
 * n=5   20   10    0  -10  -20        spacing 10, width 40
 * n=4        15    5   -5  -15        spacing 10, width 30
 * n=3        10    0  -10             spacing 10, width 20
 * n=2              5   -5             spacing 10, width 10
 * n=1              0
 * </pre>
 *
 * <h2>THE SPACING IS HELD CONSTANT AND THE WIDTH IS NOT, WHICH IS THE CHOICE</h2>
 *
 * <p>The alternative was to hold the <b>40-degree width</b> and respace -- {@code n=3} at
 * {@code 20 0 -20}, {@code n=2} at {@code 20 -20}. <b>Both reproduce the ruled five exactly</b> and
 * they disagree at every count below it, so the ruled row alone could not have chosen between them.
 *
 * <p>Ten degrees, centred, is what was ruled: <b>a half-charged release is a TIGHTER GROUP, not a
 * sparser one.</b> A two-arrow release under the width rule would have been a 40-degree split --
 * two arrows going noticeably different places, which reads as a malfunction rather than as a
 * smaller shot.
 *
 * <h2>THE EVEN COUNTS ARE REACHABLE ONLY THROUGH THE MAGAZINE, NEVER THROUGH THE CHARGE</h2>
 *
 * <p><b>This is the thing a reader will otherwise wonder about, because R1' yields 1, 3 or 5.</b>
 * The charge never produces two arrows or four. The cap does:
 *
 * <pre>
 * charge          step 1, 2, 3   ->  1, 3, 5 arrows      the only counts a full magazine gives
 * magazine cap    R3a            ->  the step the rounds can pay for IN FULL
 * </pre>
 *
 * <p>Under R3a a release fires exactly the tracked step, so <b>two rounds gives ONE arrow and four
 * rounds gives THREE</b> -- and the even rows of the table above are <i>still</i> not produced.
 * They become reachable the moment anything yields an even count: a future ruling on the increment,
 * a capacity modifier that subtracts, or an overturn of R3a in favour of {@code min(step, rounds)},
 * which would fire two arrows on two rounds.
 *
 * <p><b>So the even rows are defined and unreached rather than dead.</b> They are computed by the
 * same expression as the odd ones -- there is no separate branch to rot -- and
 * {@code DrawFanTest} exercises them directly, which is the only exercise they get. Said here
 * because a reader counting shipped call sites would find none and reasonably conclude the rule had
 * grown cases nobody wanted.
 *
 * <h2>DEGREES, AND THE VECTOR IS RESOLVED OUTSIDE core</h2>
 *
 * <p>These are YAW offsets from the caster's aim, in degrees, and nothing here knows what a
 * direction is. Turning an offset into a vector needs the aim, which is {@code paper}'s -- the same
 * split {@code CastSpec.DashDirection} uses, where core names the RULE and the resolver lives
 * outside it.
 */
public final class DrawFan {

    private DrawFan() {}

    /** RULED: ten degrees between neighbouring arrows, whatever the count. */
    public static final double SPACING_DEGREES = 10.0;

    /**
     * The yaw offsets for a release of {@code arrows} arrows, <b>widest-first</b>, centred on zero.
     *
     * <p><b>Widest-first so the output reads in the ruling's own order</b> -- {@code 20 10 0 -10 -20}
     * rather than its reverse. The fan is symmetric, so the ORDER carries no meaning and no caller
     * may depend on it; it is chosen only so a reader comparing this against the ruling sees the
     * same sequence instead of having to check it is the same set.
     *
     * <p>An empty array for {@code arrows <= 0}: a release of nothing points nowhere, and a caller
     * that somehow asks should get an empty loop rather than an exception.
     */
    public static double[] offsetsFor(int arrows) {
        if (arrows <= 0) return new double[0];
        double[] offsets = new double[arrows];
        double middle = (arrows - 1) / 2.0;
        for (int i = 0; i < arrows; i++) {
            offsets[i] = (middle - i) * SPACING_DEGREES;
        }
        return offsets;
    }
}
