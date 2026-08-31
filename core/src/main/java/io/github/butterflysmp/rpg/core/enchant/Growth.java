package io.github.butterflysmp.rpg.core.enchant;

/**
 * Growth: flat points added to the MAX HEALTH of whoever wears the piece it sits on.
 *
 * <p>The shape of {@link Protection}, and the arithmetic is the same one line. What differs is
 * everything downstream, and it is worth knowing before authoring a number here.
 *
 * <h2>This is the only enchant that can TAKE HEALTH AWAY</h2>
 *
 * Every other enchant feeds a stat that is read at the moment of use -- damage, block, reflect,
 * defense -- so removing one changes what the NEXT hit does and nothing else. Max health is not like
 * that. {@code HealthState} fixed the transition rules long before this enchant existed, and Growth
 * inherits both:
 *
 * <ul>
 *   <li><b>Equipping is HEADROOM, never a heal.</b> 100/100 with a +30 piece becomes 100/130 -- the
 *       player now looks hurt and has gained nothing they can spend.
 *   <li><b>Unequipping CLAMPS current down.</b> 130/130 losing that piece becomes 100/100. If
 *       current was already below the new max it is left alone.
 * </ul>
 *
 * That asymmetry is deliberate and correct -- it is what stops equip/unequip cycling being a free
 * heal -- but it means a source that vanished and came back would clamp on the way down and NOT
 * restore on the way up. Not reachable today: the registry is fixed at boot and the scan reads the
 * slot the piece is actually in, so a Growth source only disappears when the piece does.
 *
 * <h2>Points, and stacking</h2>
 *
 * Flat points for the same reason Protection uses them: max health is a summand, and each piece
 * contributes its own bonus under its own key, so the reconciler sums four pieces without this
 * method knowing a set exists. Full Growth III is +120 on a base of 100.
 *
 * <p>Unlike Protection there is no curve to hide behind: +120 max health is a straight doubling of
 * the player's pool, where +36 Defense is bent by an asymptotic curve into about twice the
 * mitigation. The two enchants read as siblings on a tooltip and are NOT equally scaled underneath.
 * If Growth is ever retuned, that is the reason, and it is a content edit rather than a code change.
 *
 * <p>No {@code Growth.ID}: bound by {@code EnchantEffect.MAX_HEALTH}, so the second max-health
 * enchant is a yml file rather than a recompile.
 */
public final class Growth {

    private Growth() {}

    /** No bonus. The same 0.0-is-absent convention {@link Protection#NONE} and its siblings use. */
    public static final double NONE = 0.0;

    /** Does this bonus grant anything at all? Strictly {@code >}, so 0 declares nothing. */
    public static boolean boosts(double bonusPoints) {
        return bonusPoints > NONE;
    }

    /**
     * A piece's contribution to max health. Trivially the bonus itself -- unlike
     * {@link Protection#effectiveDefense}, there is no per-piece base to add it to, because a piece
     * of armor has no max health of its own.
     *
     * <p>It exists anyway rather than the caller using the raw number, so that the ONE place a
     * Growth bonus becomes a stat modifier is named, and so a future rule -- diminishing returns
     * past a threshold, a per-set cap -- has somewhere to live that is not a scan loop.
     */
    public static double contribution(double bonusPoints) {
        return bonusPoints;
    }
}
