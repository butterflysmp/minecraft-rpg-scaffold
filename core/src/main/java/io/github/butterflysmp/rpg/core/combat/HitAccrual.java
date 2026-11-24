package io.github.butterflysmp.rpg.core.combat;

/**
 * How one hit interacts with the scorch/ignite subsystem: whether its element may ACCRUE, and -- when
 * it may -- how deep in an ignite chain the blast that dealt it was.
 *
 * <h2>WHY THESE TWO FACTS ARE ONE VALUE AND NOT TWO PARAMETERS</h2>
 *
 * <b>REPRESENTABILITY, and that alone is the case.</b> An {@code (AccrualRule, int)} pair lets a call
 * site write {@code (INERT, 2)} and {@code (ACCRUES, 4)}. Both are nonsense -- <b>depth 4 MEANS
 * inert</b>, that is the entire chain-limit ruling -- and neither fails to compile. Here they cannot
 * be written: {@link #terminal()} takes no depth and {@link #chained(int)} is always accruing.
 *
 * <p>And a bare {@code int} would have to use <b>0 as "no depth"</b>, which is a sentinel meaning
 * ABSENCE. This port has rejected that shape twice: the null-element loop guard became
 * {@link AccrualRule} precisely because you cannot assert on a null that means something.
 *
 * <p><b>The transposition hazard is NOT the argument, and is not borrowed.</b> An {@code int} beside
 * an enum cannot be swapped -- the compiler catches it. That reasoning covers {@link CritState} and
 * {@link DefenseRule} and does not reach this.
 *
 * <h2>THE FACTORIES ARE NAMED FOR THE PROPERTY, NOT THE MECHANISM</h2>
 *
 * There is deliberately no {@code blast()} or {@code ignite()} here. {@link DefenseRule}'s javadoc in
 * this same package states the rule: <i>do not rename it after a status.</i> A general damage port
 * must not carry one mechanism's vocabulary, however clearly that mechanism motivated the field.
 *
 * @param rule  whether this hit's element may accrue the status it declares
 * @param depth the depth of the ignite chain link that dealt this hit; {@code 0} for a hit no blast
 *              caused. <b>Meaningful only when {@code rule} is {@link AccrualRule#ACCRUES}</b> -- an
 *              INERT hit creates no scorch entry and fails the ignite predicate, so its depth is read
 *              by nothing, ever. That is why {@link #terminal()} carries none.
 * @param scorchStampPercent MANA BURN'S STAMP (F1, the seat's ruling of 2026-11-24 on PR F), in percent: what the scorch
 *              this hit applies multiplies its tick and its Ignite by, as {@code 1 + percent / 100} (F2). 0 from every
 *              factory below, so a status tick, an Ignite link and every unenchanted hit carry none; only
 *              {@link #withScorchStamp}, called by the damage arms for a primary hit by a weapon carrying the enchant
 *              ({@code BehaviourHits.scorchStamp}), sets one.
 */
public record HitAccrual(AccrualRule rule, int depth, double scorchStampPercent) {

    public HitAccrual {
        if (!(scorchStampPercent >= 0) || Double.isInfinite(scorchStampPercent)) {
            throw new IllegalArgumentException("a scorch stamp is a percent of 0 or more, was " + scorchStampPercent);
        }
    }

    /** The two-fact accrual every factory builds: no stamp. */
    public HitAccrual(AccrualRule rule, int depth) {
        this(rule, depth, 0);
    }

    /** This accrual, stamped with Mana Burn's {@code percent} (F1). */
    public HitAccrual withScorchStamp(double percent) {
        return new HitAccrual(rule, depth, percent);
    }

    /** An ordinary hit: a weapon, an ability, a dev command. Accrues, and no blast caused it. */
    public static HitAccrual weapon() {
        return new HitAccrual(AccrualRule.ACCRUES, 0);
    }

    /**
     * A hit that wears its element for display and accrues nothing -- scorch's own burn tick.
     *
     * <p>Distinct from {@link #terminal()} by INTENT rather than by value: this one is a loop guard on
     * a status feeding itself, that one is a chain limit. They agree today and there is no reason they
     * must, so they are not the same factory.
     */
    public static HitAccrual inert() {
        return new HitAccrual(AccrualRule.INERT, 0);
    }

    /** A hit dealt by an ignite chain link at {@code depth}, which may still recruit. */
    public static HitAccrual chained(int depth) {
        return new HitAccrual(AccrualRule.ACCRUES, depth);
    }

    /**
     * The last link: it damages normally and propagates nothing.
     *
     * <p><b>NO DEPTH, AND THE OMISSION IS THE POINT.</b> INERT creates no {@code Active} entry and
     * fails {@code ElementAccrual.accruesScorch}, so a terminal hit's depth is unreachable by any
     * code path. Storing one would assert a number nothing can read -- the dead-field shape deleted
     * from {@code ScorchStatus}'s stack count. An argless factory states the invariant instead.
     */
    public static HitAccrual terminal() {
        return new HitAccrual(AccrualRule.INERT, 0);
    }

    /** True when this hit's element may accrue. Delegates; see {@link AccrualRule#accrues()}. */
    public boolean accrues() {
        return rule.accrues();
    }
}
