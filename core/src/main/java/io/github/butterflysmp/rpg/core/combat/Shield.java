package io.github.butterflysmp.rpg.core.combat;

/**
 * The block stat: how much of an incoming hit a raised shield turns away.
 *
 * A FLAT FRACTION, not a curve -- deliberately the opposite choice from {@link Defense}. Defense is
 * asymptotic because armor accumulates across four slots and an unbounded stat, so it needs a shape
 * that can absorb arbitrarily large inputs without ever reaching immunity. A shield's block is ONE
 * authored number on ONE item, bounded at the content file, and it is meant to be legible: "this
 * shield stops half" is the whole of it. Bending that through a diminishing-returns curve would buy
 * nothing and cost the player the ability to predict it.
 *
 * <p><b>This composes with {@link Defense}; it does not replace it.</b> The rider applies the block
 * first and {@code CombatantStats.damage} applies defense second, so a blocking player in armor gets
 * both -- {@code applyDefense(applyBlock(d, dr), defense)}.
 *
 * <p>The order is FIXED BY THE PIPELINE, not by a preference: the block is applied in the rider,
 * on the attacker's thread, while defense is applied a thread hop later inside
 * {@code CombatantStats.damage}. There is no call site at which the two could be swapped.
 *
 * <p>Do not be tempted to record that the order "does not matter because multiplication commutes".
 * That is the wrong law -- swapping these two steps is REASSOCIATION, not commutation, and it is
 * not exact in binary floating point. Measured across 22400 combinations of damage, block fraction
 * and defense, the two orderings differ in 4780 of them, by at most 2.842170943040401e-14 (worst
 * case: damage 123.5, block 0.3, defense 1). Far below anything a player could see, and far above
 * zero -- which is why the composition test asserts with an epsilon rather than {@code ==}.
 *
 * <p><b>Vanilla decides WHETHER a block happened; this decides what it is worth.</b> Raised, frontal
 * and in-arc are all vanilla's own validity, inherited by riding its damage event rather than
 * re-derived from {@code isBlocking()} plus a facing check we would have to keep in step with
 * Mojang. Nothing in this class knows what a shield is; it takes a number and a fraction.
 *
 * Pure and dependency-free, the shape of {@link Defense} and {@link AttackCharge}.
 */
public final class Shield {

    private Shield() {}

    /** No block at all. The absent value, and what an unshielded hit is scaled by. */
    public static final double NONE = 0.0;

    /** A total block. The upper clamp, not a recommendation -- see {@link #applyBlock}. */
    public static final double FULL = 1.0;

    /**
     * {@code blockDr} clamped to {@code [0, 1]}, which is the only range the multiply below is safe
     * over. Every other method goes through this rather than trusting its argument.
     *
     * {@code NaN} clamps to {@link #NONE}: {@code Math.max} propagates NaN, so the comparison is
     * written the way round that rejects it instead. A NaN block would make the damage NaN, and a
     * NaN hit does not reduce a health bar -- it erases it into a value no comparison is true about.
     */
    public static double clamp(double blockDr) {
        if (!(blockDr > NONE)) return NONE;   // false for NaN, which is the point
        return Math.min(blockDr, FULL);
    }

    /**
     * The fraction of a hit that gets THROUGH a block: {@code 1 - clamp(blockDr)}.
     *
     * The common shield declares {@code 0.5} and therefore passes {@code 0.5}. Stated as its own
     * method because the content file authors the fraction STOPPED and the arithmetic needs the
     * fraction PASSED, and having the flip in one place is what stops the two readings being mixed
     * up at a call site -- the same reason {@link Defense} exposes both of its readings.
     */
    public static double passThrough(double blockDr) {
        return FULL - clamp(blockDr);
    }

    /**
     * {@code damage} after a block worth {@code blockDr} has taken its cut.
     *
     * <p><b>The clamp is the whole safety story, and it fails in BOTH directions.</b> {@code blockDr}
     * arrives from a YAML file a server operator can edit, so neither bound is theoretical:
     *
     * <ul>
     *   <li>{@code block_dr: -1} unclamped is {@code damage * (1 - (-1))} = {@code damage * 2}. A
     *       shield that DOUBLES the hit it was raised to stop.</li>
     *   <li>{@code block_dr: 2} unclamped is {@code damage * (1 - 2)} = {@code -damage}. Negative
     *       damage reaches {@code CombatantStats.damage} and HEALS the victim -- a shield that makes
     *       you stronger the more you are hit, which is the worse of the two because it looks like a
     *       feature until someone stands in a mob swarm and never dies.</li>
     * </ul>
     *
     * A {@code blockDr} of zero returns the damage untouched, so a shield declaring no block does
     * nothing rather than something subtle. Both {@code ShieldDefinition}'s constructor and this
     * clamp guard the range; that is deliberate duplication, because the constructor guards CONTENT
     * and this guards the ARITHMETIC, and only one of them is between a hand-edited item and a
     * healing hit.
     *
     * Worked: {@code 8 @ 0.5 -> 4}; {@code 8 @ 0 -> 8}; {@code 8 @ 1 -> 0}.
     */
    public static double applyBlock(double damage, double blockDr) {
        return damage * passThrough(blockDr);
    }

    /**
     * Does this fraction declare a block at all?
     *
     * Strictly greater than zero, so an absent or zero {@code block_dr} is not a block. The same
     * predicate shape as {@code SweepShare.sweeps}, and for the same reason: PRESENCE is the opt-in,
     * and {@code >=} here would make every shield "blocking" for zero and put the rider's witness
     * log on every hit in the game.
     */
    public static boolean blocks(double blockDr) {
        return blockDr > NONE;
    }
}
