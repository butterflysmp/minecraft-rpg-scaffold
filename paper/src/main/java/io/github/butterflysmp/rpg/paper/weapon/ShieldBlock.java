package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.Shield;
import io.github.butterflysmp.rpg.core.enchant.Bulwark;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.enchant.Thorns;
import io.github.butterflysmp.rpg.core.weapon.Durability;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.OptionalInt;

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
    public record Outcome(boolean blocked, double effectiveDr, double reflectPercent,
                          EquipmentSlot slot, String shieldId) {

        /**
         * No block, for any of THREE reasons that mean the same thing to the rider: vanilla did not
         * block, what blocked was not one of ours (a plain vanilla shield, or a dangling id), or the
         * shield is BROKEN. One outcome, one meaning -- "no custom mitigation from this stack".
         *
         * <p><b>{@code reflectPercent} is {@link Thorns#NONE} here, and that is what makes the
         * reflect inherit every one of those reasons for free.</b> A hit from behind, an untagged
         * shield, a dangling id and a broken shield all send nothing back without the rider needing
         * a single extra branch -- one predicate, all three shield effects.
         */
        public static final Outcome NONE =
                new Outcome(false, Shield.NONE, Thorns.NONE, null, null);
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
                                  ShieldRegistry shields, EnchantRegistry enchants) {
        if (!vanillaBlocked(event)) return Outcome.NONE;

        Optional<EquipmentSlot> hand = ShieldItems.shieldHand(victim, keys);
        if (hand.isEmpty()) return Outcome.NONE;   // vanilla shield, or none: no custom mitigation

        EquipmentSlot slot = hand.get();
        ItemStack stack = victim.getEquipment().getItem(slot);
        String id = ShieldItems.shieldId(stack, keys).orElse(null);
        if (id == null) return Outcome.NONE;

        // A dangling shield_id -- an item whose content file is gone -- blocks NOTHING rather than
        // guessing a fraction. Same instinct as RefreshVerdict.Dangling and the enchant command's
        // refuse-rather-than-half-edit: an unknown definition is a reason to do less, not to invent
        // a default. It is loud in the witness log and silent in play.
        ShieldDefinition definition = shields.find(id).orElse(null);
        if (definition == null) return Outcome.NONE;

        // A BROKEN SHIELD STOPS BLOCKING, and this is the single gate every shield mechanic falls
        // off: base DR here, Bulwark below it, and the reflect Slice 2b hangs off reflectPercent.
        // All three, one predicate. Slice 1 shipped without one
        // deliberately -- Durability.wear floors at one remaining use, so a spent shield simply
        // stopped wearing and kept blocking at full strength, which made all of that slice's
        // durability work cosmetic and broke the symmetry with weapons, which DO gate on broken.
        //
        // An item with no durability at all is NOT broken. maxOf is empty for a non-Damageable
        // stack, and that must return normally rather than throw -- the same early-out shape
        // ShieldDurability.applyWearOnBlock uses.
        OptionalInt max = WeaponDurability.maxOf(stack);
        if (max.isPresent()
                && Durability.isBroken(WeaponDurability.damageOf(stack), max.getAsInt())) {
            return Outcome.NONE;
        }

        // BOTH shield enchants are read HERE, and from ONE decode. Composing them in resolve rather
        // than in the rider means the ordering constraint above (resolve before setDamage) covers
        // the enchant read for free, and the rider gets finished numbers rather than ingredients.
        //
        // ONE EnchantItems.read, not two. This runs on every blocked hit and the read parses the
        // PDC string, so the state is hoisted and the effect-scan runs over it twice -- which is
        // exactly what BlockEnchantItems' state overload was extracted for in Slice 2a.
        EnchantState state = EnchantItems.read(stack, keys);
        double bulwark = BlockEnchantItems.percentFor(state, enchants, EnchantEffect.BLOCK_DR);
        double thorns = BlockEnchantItems.percentFor(state, enchants, EnchantEffect.REFLECT);

        // The DR is the EFFECTIVE fraction -- Bulwark composed and clamped -- which is why that
        // component is not called blockDr: a name still saying "the shield's own DR" while carrying
        // an enchant's contribution is how a witness log starts lying. reflectPercent is raw points,
        // because the reflect has nothing to compose with until it meets the incoming blow.
        //
        // The two are adjacent doubles in DIFFERENT units, and no compiler can catch transposing
        // them. A swap would feed a percent to Shield.applyBlock -- 15.0 as a fraction clamps to a
        // total block -- and reflect a fraction, so it presents in play as an unkillable player
        // dealing rounding-error damage back. The locals are named for the enchants rather than for
        // their types so the construction below reads as a sentence.
        return new Outcome(true, Bulwark.effectiveDr(definition.blockDr(), bulwark), thorns,
                slot, id);
    }

    /**
     * Did VANILLA block this hit? The raised/frontal/in-arc verdict, read off the event.
     *
     * Private: {@link #resolve} is the only caller, and the only way this verdict should ever be
     * reached. It was briefly public to feed the [BLOCK] witness, which is now stripped -- the
     * witness printed THIS method's answer rather than recomputing one, the "never draw a second
     * value to print" rule the crit pass learned when its witness rolled its own random and logged
     * {@code crit=false} on the tick a yellow number appeared.
     */
    @SuppressWarnings("deprecation")   // DamageModifier: deprecated since 1.12, not for removal,
                                       // and still the only block signal on the event. See above.
    private static boolean vanillaBlocked(EntityDamageEvent event) {
        // STRICT <, and never <= or Double.compare: an unblocked hit taken with the shield raised
        // reports NEGATIVE ZERO here. See the class javadoc for the measured comparison table.
        return event.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)
                && event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0;
    }

}
