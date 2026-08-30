package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.weapon.GearClass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The damage-modifier enchant mechanism: Sharpness, Power and Attunement, which are one mechanism
 * with three sets of parameters rather than three mechanisms. See {@link EnchantEffect} for why the
 * parameters are content and the arithmetic is here.
 *
 * <p>An enchant of this type multiplies the damage of the weapon it SITS ON, and only while that
 * weapon's class matches the enchant's own. Sharpness is {@code melee}, so it does nothing on a bow;
 * Power is {@code ranger}, so it does nothing on a staff. A {@code universal} enchant (null class)
 * matches whatever it is on.
 *
 * <p><b>It is NO LONGER the same axis as {@code ClassDamageModifiers}, and that changed in Slice 2.</b>
 * It used to be: both keyed on {@code WeaponClass}. Now this one keys on {@link GearClass} -- because
 * an enchant's gate is a GEAR gate and must be able to say "shield" -- while
 * {@code ClassDamageModifiers} stays on {@code WeaponClass}, because a ring's {@code +N <Class>
 * Damage} gates on the WEAPON you are fighting with and a shield in the other hand must not change it.
 *
 * <p>The two still answer neighbouring questions -- that one asks whether an OTHER slot's grant
 * matches the weapon in your hand, this one whether an enchant sitting on the held weapon matches
 * THAT weapon -- and both return an id-keyed map for the same leak-proof reconcile diff. They were
 * always deliberately separate functions; they now also have deliberately separate types, which is
 * what stops a shield-gated enchant from ever being mistaken for a ring's class grant.
 *
 * <p><b>A SHIELD-gated enchant can never appear here at all</b>, and that is enforced twice: this
 * map is built from the held WEAPON, whose {@code GearClass} comes through {@code GearClass.of} and
 * therefore can never be {@code SHIELD}; and {@code EnchantDefinition} refuses a {@code DAMAGE}
 * enchant gated on {@code shield} at the content boundary, because it could never fire.
 *
 * <p><b>This returns PERCENT, not a multiplier, and that is load-bearing.</b> The value rides a
 * {@code Stat}, whose {@code value()} is {@code base + Sum(modifiers)}. Percentages are genuinely
 * additive, so summing is the correct composition and the neutral value is 0.0 -- the same neutral
 * as the class-damage bonus, which is what lets an untracked combatant resolve to x1.0 for free. A
 * multiplier-valued stat based at 1.0 would resolve to 2.0 with two sources, and any 0.0 default on
 * it would silently zero all damage. {@link #multiplier(double)} converts, once, at the arm.
 *
 * <p>Pure, and in {@code core}, because it is the part worth testing -- its paper counterpart
 * ({@code DamageEnchantItems}) needs a live {@code Player} to read the held item, and a Bukkit
 * {@code Player} cannot be constructed in a unit test. Same split as {@code ClassDamageModifiers}.
 */
public final class DamageEnchants {

    private DamageEnchants() {}

    /**
     * One active damage enchant, as the gate needs to see it: the class it gates on, its authored
     * curve, and the level it resolved to.
     *
     * <p>{@code gearClass} is null for a {@code universal} enchant, which matches every held
     * class. That is the same null-means-no-gate convention {@code ClassDamageModifiers} uses for an
     * empty hand, read from the other side.
     */
    public record Grant(GearClass gearClass, List<Integer> percentByLevel, int level) {}

    /**
     * The percent this curve grants at {@code level}: {@code percentByLevel[level - 1]}.
     *
     * <p>Levels at or below 0 grant nothing -- a locked or absent enchant must not scale damage, and
     * this is the branch every unenchanted weapon in the game takes.
     *
     * <p><b>CLAMPED to the list, and that is a real guard rather than defensive habit.</b>
     * {@link EnchantState#effective()} clamps to {@link EnchantState#MAX_LEVEL} (the model's global
     * 3), NOT to the individual enchant's authored {@code max_level}. The loader holds those equal
     * for a file it accepted, but a hand-edited item or a blob written by a build whose content
     * differed can carry level 3 for a two-entry curve. Without the clamp that is an
     * {@code IndexOutOfBoundsException} thrown from inside a reconcile tick, on a path that must be
     * total. It fails toward the enchant's own top percent, never past it -- the same direction
     * {@code Unbreaking.consumeChance}'s clamp fails in.
     *
     * <p><b>MOVED to {@link EnchantCurve} in Slice 2b; this delegates.</b> The curve lookup is not
     * damage-specific and had grown block and reflect callers, for whom asking {@code DamageEnchants}
     * for a percentage implied a coupling that does not exist. The signature is kept exactly so this
     * class's own tests -- and any caller that legitimately IS about damage -- are untouched.
     *
     * <p>New callers that are not about damage should import {@link EnchantCurve} directly. This
     * method stays because deleting it would move a test suite for no behavioural gain.
     */
    public static double percentAt(List<Integer> percentByLevel, int level) {
        return EnchantCurve.percentAt(percentByLevel, level);
    }

    /**
     * The damage percents active for a weapon of class {@code heldClass}, keyed by enchant id.
     *
     * <p>The returned map is the "desired" set a {@code CombatantStats.reconcileEnchantDamageModifiers}
     * converges to: same source keys in, so the leak-proof diff drops an enchant the moment it stops
     * being active. Keying by ENCHANT ID (rather than by one lumped "MAIN_HAND" source) is what lets
     * {@code Stat} do the summing, so two different damage enchants on one weapon compose without
     * this method knowing they did.
     *
     * <p>{@code heldClass} is null when the hand holds nothing, or nothing of ours. That returns an
     * EMPTY map, and it is belt and braces rather than the load-bearing guard it is in
     * {@code ClassDamageModifiers}: these grants are read OFF the held weapon, so no weapon already
     * means no grants. It is written anyway so the function is total and cannot be made to grant a
     * universal enchant's percent to an empty hand.
     *
     * <p>A non-matching enchant is simply ABSENT from the result, not present at 0.0, so the
     * reconciler removes its source rather than leaving a dead modifier behind.
     */
    public static Map<String, Double> matching(GearClass heldClass, Map<String, Grant> active) {
        Map<String, Double> desired = new HashMap<>();
        if (heldClass == null || active == null) return desired;
        for (Map.Entry<String, Grant> entry : active.entrySet()) {
            Grant grant = entry.getValue();
            if (grant == null) continue;
            // null class == universal: no gate, matches whatever it is on.
            if (grant.gearClass() != null && grant.gearClass() != heldClass) continue;
            double percent = percentAt(grant.percentByLevel(), grant.level());
            if (percent != 0.0) desired.put(entry.getKey(), percent);
        }
        return desired;
    }

    /**
     * The damage multiplier for a resolved percent: {@code 1 + percent/100}. THE one place the
     * formula lives, so the two damage arms cannot disagree about it.
     *
     * <p>Worked: 0 -&gt; 1.0 (an unenchanted or untracked caster deals exactly what it dealt before
     * this class existed); 5 -&gt; 1.05; 15 -&gt; 1.15.
     */
    public static double multiplier(double percent) {
        return 1.0 + percent / 100.0;
    }
}
