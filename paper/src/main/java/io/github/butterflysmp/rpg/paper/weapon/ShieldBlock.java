package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.Shield;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

/**
 * Did vanilla consider this hit blocked, and if so by which of our shields?
 *
 * The whole adapter between vanilla's block validity and our arithmetic. It holds the ONE
 * deprecation suppression this slice needs, so {@code DamageModifier} appears in exactly one file.
 * No arithmetic lives here -- the fraction goes to {@link Shield} in core.
 *
 * <h2>Why the event's BLOCKING modifier, and not {@code isBlocking()}</h2>
 *
 * {@code HumanEntity.isBlocking()} is DIRECTION-BLIND: it is true for a player holding right-click
 * whatever they are facing, so a shield read that way would block a hit landing in the player's
 * back. Boot-witnessed: {@code isBlocking=true} on hits at 107 and 160 degrees off the victim's
 * facing, both of which vanilla let straight through.
 *
 * <p>Vanilla's own validity is raised AND frontal AND within the shield's horizontal arc, and the
 * SIGN of {@code DamageModifier.BLOCKING} is that verdict already computed. Inheriting it means the
 * arc rule stays Mojang's to change rather than ours to keep in step -- measured on this build as
 * blocking out to 89.2 degrees off-facing and passing at 107.4, which brackets vanilla's documented
 * 90-degree {@code horizontalBlockingAngle} without pinning it (our probe was a 3D dot; vanilla's
 * check is horizontal).
 *
 * <p>The test is a strict {@code < 0}. The modifier is a REDUCTION, so a block is a negative
 * number; a full block is {@code -raw}, which is still negative, so detection holds even when the
 * hit is reduced to nothing. {@code getDamage(DamageModifier)} is safe to call for an inapplicable
 * modifier -- it returns 0 -- and unlike {@code setDamage(DamageModifier, double)} it never throws.
 *
 * <h2>The strict {@code <} is load-bearing, and NOT for the reason it looks like</h2>
 *
 * Boot-witnessed 2026-08-29. The obvious worry is the full-block case, and that one is safe by a
 * mile: a full block reports {@code -raw}, robustly negative. The real trap is the hit that is NOT
 * blocked while the shield is RAISED -- a swing landing outside the frontal arc. Vanilla reports
 * that as:
 *
 * <pre>
 *   blockingApplicable=true   blocking=-0.0000   isBlocking=true   facingDot=-0.2987
 * </pre>
 *
 * NEGATIVE ZERO, not plain zero. Executed against the real values rather than reasoned about:
 *
 * <pre>
 *   -0.0 &lt;  0                  -&gt; false    &lt;- what this class ships. correct.
 *   -0.0 &lt;= 0                  -&gt; true     &lt;- would report EVERY hit from behind as blocked
 *   -0.0 != 0                  -&gt; false    -- would also have been correct
 *   Double.compare(-0.0, 0.0)  -&gt; -1       &lt;- a compare()-based spelling MIS-FIRES
 * </pre>
 *
 * So {@code <=}, or the idiomatic-looking {@code Double.compare(getDamage(BLOCKING), 0) < 0}, would
 * silently invert the frontal-arc rule this class exists to inherit: a player holding block would
 * take half damage from behind, with vanilla playing no block cue to contradict it. Do not
 * "simplify" this comparison.
 *
 * <p>Also witnessed, and the reason {@link #vanillaBlocked} tests the VALUE rather than
 * applicability: {@code isApplicable(BLOCKING)} is {@code true} on every player damage event,
 * including one taken bare-handed with no shield in the inventory at all. The modifier's javadoc
 * "only present for Players" means exactly that and nothing more -- it is not a block signal.
 *
 * <p><b>The enum is deprecated (since 1.12) but not marked for removal</b>, and on the pinned Paper
 * (26.1.2) it is still the only block signal on the event: {@code DamageSource} carries none, and
 * the {@code blocks_attacks} data component describes the ITEM, not the hit. If a future build
 * removes it, this class is the one place that has to change.
 *
 * <h2>What it deliberately does not do</h2>
 *
 * An UNTAGGED vanilla shield resolves to {@link Outcome#NONE}: vanilla blocks it visually and
 * mechanically in vanilla terms, but the mob's stat reaches the player's custom HP undiminished,
 * because the vanilla number is tokened away by the rider. A plain shield is, mechanically, not
 * blocking at all. That is a known and is recorded in NEXT.md rather than fixed here.
 */
public final class ShieldBlock {

    private ShieldBlock() {}

    /**
     * The verdict: whether one of our shields blocked, what fraction it stops, and which hand held
     * it so the wear can be charged to the right slot.
     *
     * {@code slot} and {@code shieldId} are meaningful only when {@code blocked} is true; on
     * {@link #NONE} they are null, and the rider never reads them because it branches on
     * {@code blocked} first.
     */
    public record Outcome(boolean blocked, double blockDr, EquipmentSlot slot, String shieldId) {

        /** No block: either vanilla did not block, or what blocked was not one of ours. */
        public static final Outcome NONE = new Outcome(false, Shield.NONE, null, null);
    }

    /**
     * Resolve a damage event against the victim's shields.
     *
     * <p><b>Call this BEFORE {@code event.setDamage(...)}.</b> {@code EntityDamageEvent.setDamage}
     * re-derives every modifier by scaling them against the new base, so reading BLOCKING after the
     * rider has tokened the damage reports the token's share of the block rather than the block.
     * The rider's ordering is what makes this correct, and it is commented there too.
     */
    public static Outcome resolve(LivingEntity victim, EntityDamageEvent event, Keys keys,
                                  ShieldRegistry shields) {
        if (!vanillaBlocked(event)) return Outcome.NONE;

        Optional<EquipmentSlot> hand = ShieldItems.shieldHand(victim, keys);
        if (hand.isEmpty()) return Outcome.NONE;   // vanilla shield, or none: no custom mitigation

        EquipmentSlot slot = hand.get();
        String id = ShieldItems.shieldId(victim.getEquipment().getItem(slot), keys).orElse(null);
        if (id == null) return Outcome.NONE;

        // A dangling shield_id -- an item whose content file is gone -- blocks NOTHING rather than
        // guessing a fraction. Same instinct as RefreshVerdict.Dangling and the enchant command's
        // refuse-rather-than-half-edit: an unknown definition is a reason to do less, not to invent
        // a default. It is loud in the witness log and silent in play.
        ShieldDefinition definition = shields.find(id).orElse(null);
        if (definition == null) return Outcome.NONE;

        return new Outcome(true, definition.blockDr(), slot, id);
    }

    /**
     * Did VANILLA block this hit? The raised/frontal/in-arc verdict, read off the event.
     *
     * Public rather than package-private so the temporary {@code [BLOCK]} witness, which lives in
     * the listener package, can print the same answer the rider decides on rather than drawing its
     * own -- the "never draw a second value to print" rule the crit pass learned the hard way, when
     * a witness rolled its own random and logged {@code crit=false} on the tick a yellow number
     * appeared.
     */
    @SuppressWarnings("deprecation")   // DamageModifier: deprecated since 1.12, not for removal,
                                       // and still the only block signal on the event. See above.
    public static boolean vanillaBlocked(EntityDamageEvent event) {
        // STRICT <, and never <= or Double.compare: an unblocked hit taken with the shield raised
        // reports NEGATIVE ZERO here. See the class javadoc for the measured comparison table.
        return event.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)
                && event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0;
    }

    /**
     * PUBLIC ONLY FOR THE WITNESS -- narrow this back to private when the [BLOCK] log is stripped.
     *
     * The raw BLOCKING modifier, for the witness log only. Negative when vanilla blocked, 0 when
     * the modifier is absent.
     */
    @SuppressWarnings("deprecation")
    public static double blockingModifier(EntityDamageEvent event) {
        return event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING);
    }

    /**
     * PUBLIC ONLY FOR THE WITNESS -- narrow back to private when the [BLOCK] log is stripped.
     *
     * Whether the event carries a BLOCKING modifier at all.
     */
    @SuppressWarnings("deprecation")
    public static boolean blockingApplicable(EntityDamageEvent event) {
        return event.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING);
    }
}
