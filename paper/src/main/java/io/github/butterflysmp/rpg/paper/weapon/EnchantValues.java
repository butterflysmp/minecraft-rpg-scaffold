package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.ActiveEnchant;
import io.github.butterflysmp.rpg.core.enchant.EnchantCurve;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;

/**
 * Summing what an item's active enchants grant for ONE effect, off an already-decoded state.
 *
 * <p>Walk {@code EnchantState.effective()}, resolve each id in the registry, keep the ones binding
 * the mechanism asked for, and sum their curve values. The arithmetic that matters lives in the
 * mechanism class -- {@code Bulwark}, {@code Thorns}, {@code Protection}, {@code Growth}.
 *
 * <p><b>WAS {@code BlockEnchantItems}, and the name was wrong before it was misleading.</b> It has
 * been parameterized by {@code EnchantEffect} since Slice 2b -- it never knew anything about
 * blocking -- and Armor Slice 2a made it read DEFENSE off worn pieces, at which point "Block" named
 * one caller rather than the class. The alternative was a second copy of this loop for armor, which
 * is the one thing this project will not duplicate: structure yes, logic never.
 *
 * <p>{@code totalFor} rather than {@code percentFor} for the same reason {@code value_by_level}
 * replaced {@code percent_by_level}: Protection and Growth grant flat POINTS, and nothing here
 * divides. What the number means stays the mechanism's business.
 *
 * <p><b>Unlike {@code DamageEnchantItems}, this one IS unit-tested.</b> That sibling is
 * boot-witnessed entirely because every entry point it has needs a live {@code Player}. Keeping the
 * PDC decode OUT of this class leaves it over plain values -- {@code EnchantState},
 * {@code EnchantDefinition} and {@code EnchantRegistry} are none of them Bukkit -- so the rule worth
 * guarding runs with no server at all.
 *
 * <h2>Bound by EFFECT, never by id -- and that is the opposite of the Unbreaking seam</h2>
 *
 * {@code ShieldDurability} reads Unbreaking with {@code EnchantItems.activeLevel(stack, keys,
 * Unbreaking.ID)}: a hardcoded id, and no registry lookup at all. That is right THERE, because
 * Unbreaking is one enchant whose curve is Java, so the id IS the binding and an enchant whose
 * content file was deleted keeps working.
 *
 * <p>It would be wrong here. Bulwark's curve lives in {@code value_by_level}, so the definition
 * must be resolved anyway -- and once it is, filtering on {@code effect()} rather than on an id is
 * free and means the SECOND block enchant is a yml file rather than a recompile, which invariant 2
 * requires. There is no {@code Bulwark.ID} constant on purpose.
 *
 * <p>The cost of that choice is the asymmetry {@code DamageEnchantItems} already records: deleting
 * {@code bulwark.yml} silently switches Bulwark off while the tooltip still renders it, because
 * {@code EnchantLore} fail-softs a dangling id. Same direction as every other unknown here -- it
 * fails toward granting nothing.
 *
 * <h2>Summing is safe, and only because effective() already de-duplicated</h2>
 *
 * {@code EnchantState.effective()} resolves an id held in two slots to the HIGHEST level either
 * holds it at, never the sum. So a shield carrying Bulwark in two columns contributes once, and
 * summing across the DISTINCT ids that remain is the correct composition -- the same additive rule
 * {@code DamageEnchants} documents for percentages. Summing over a state that had itself summed
 * duplicates would let two columns of one enchant walk a shield to {@code dr = 1.0}.
 *
 * <p><b>The DECODE lives in the caller, and that is what makes one blocked hit cost one read.</b>
 * {@code ShieldBlock.resolve} decodes the blocking stack once and scans the resulting state twice --
 * BLOCK_DR for Bulwark, REFLECT for Thorns. The effect is a parameter rather than this class
 * answering for every mechanism at once, so a caller that wants only one pays for only one pass.
 */
public final class EnchantValues {

    private EnchantValues() {}

    /**
     * The summed percentage a piece of gear's ACTIVE enchants contribute for {@code effect}, or
     * {@code 0.0} when it carries none.
     *
     * <p>Zero is the neutral value every consumer wants -- {@code Bulwark.effectiveDr(dr, 0)} is
     * {@code dr} exactly, and {@code Thorns.reflects(0)} is false -- so the overwhelmingly common
     * unenchanted shield needs no branch at any call site.
     *
     * <p>Reads {@code effective()}, so the level here is literally the level the TOOLTIP rendered.
     * A shield promising Bulwark III cannot be blocking at II.
     *
     * <p><b>Takes an already-decoded state, and there is deliberately no {@code ItemStack} overload
     * any more.</b> One existed through Slice 2a, when the block rider still decoded per call. Slice
     * 2b hoisted that read into {@code ShieldBlock.resolve} so BOTH shield enchants come off ONE
     * decode, which left the stack-shaped overload with zero production callers -- and its mere
     * presence forced a {@code (EnchantState) null} cast in the test just to disambiguate. Deleting
     * it removed dead public API and that cast together.
     *
     * <p>It is also why this class has any unit coverage at all. {@code EnchantState},
     * {@code EnchantDefinition} and {@code EnchantRegistry} are plain values, so this runs with no
     * server -- unlike its sibling {@code DamageEnchantItems}, whose every entry point needs a live
     * {@code Player} and which is therefore boot-witnessed entirely.
     */
    public static double totalFor(EnchantState state, EnchantRegistry enchants,
                                    EnchantEffect effect) {
        if (state == null || enchants == null || effect == null) return 0.0;

        double total = 0.0;
        for (ActiveEnchant active : state.effective()) {
            EnchantDefinition definition = enchants.find(active.enchantId()).orElse(null);
            // A dangling id has no curve, so it cannot grant a percent. It still RENDERS on the
            // tooltip -- EnchantLore's deliberate fail-soft -- so the mismatch is visible rather
            // than silent, and it fails toward granting nothing.
            if (definition == null || definition.effect() != effect) continue;
            total += EnchantCurve.valueAt(definition.valueByLevel(), active.level());
        }
        return total;
    }
}
