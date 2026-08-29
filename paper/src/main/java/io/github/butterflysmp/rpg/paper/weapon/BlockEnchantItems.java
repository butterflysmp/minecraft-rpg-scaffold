package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.ActiveEnchant;
import io.github.butterflysmp.rpg.core.enchant.DamageEnchants;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import org.bukkit.inventory.ItemStack;

/**
 * Reading a shield's enchant percentages off the BLOCKING STACK. The Bukkit half only.
 *
 * <p>The shield analogue of {@link DamageEnchantItems}, and deliberately the same shape: walk
 * {@code EnchantState.effective()}, resolve each id in the registry, keep the ones binding the
 * mechanism asked for, and sum their curve values. The arithmetic that matters is in
 * {@code core/enchant/Bulwark}; this needs a live {@link ItemStack} and so is boot-witnessed rather
 * than unit-tested, exactly as {@code DamageEnchantItems} is.
 *
 * <h2>Bound by EFFECT, never by id -- and that is the opposite of the Unbreaking seam</h2>
 *
 * {@code ShieldDurability} reads Unbreaking with {@code EnchantItems.activeLevel(stack, keys,
 * Unbreaking.ID)}: a hardcoded id, and no registry lookup at all. That is right THERE, because
 * Unbreaking is one enchant whose curve is Java, so the id IS the binding and an enchant whose
 * content file was deleted keeps working.
 *
 * <p>It would be wrong here. Bulwark's curve lives in {@code percent_by_level}, so the definition
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
 * <p><b>One decode per call.</b> This runs on every blocked hit, and {@code EnchantItems.read}
 * parses the PDC string, so the effect is passed in rather than the caller asking twice for two
 * different mechanisms. Slice 2b's reflect reads its percentage through this same call.
 */
public final class BlockEnchantItems {

    private BlockEnchantItems() {}

    /**
     * The summed percentage this stack's ACTIVE enchants contribute for {@code effect}, or
     * {@code 0.0} when it carries none.
     *
     * <p>Zero is the neutral value every consumer wants -- {@code Bulwark.effectiveDr(dr, 0)} is
     * {@code dr} exactly -- so the overwhelmingly common unenchanted shield needs no branch at the
     * call site.
     *
     * <p>Reads {@code effective()}, so the level here is literally the level the TOOLTIP rendered.
     * A shield promising Bulwark III cannot be blocking at II.
     */
    public static double percentFor(ItemStack stack, Keys keys, EnchantRegistry enchants,
                                    EnchantEffect effect) {
        if (stack == null || enchants == null) return 0.0;

        double total = 0.0;
        for (ActiveEnchant active : EnchantItems.read(stack, keys).effective()) {
            EnchantDefinition definition = enchants.find(active.enchantId()).orElse(null);
            // A dangling id has no curve, so it cannot grant a percent. It still RENDERS on the
            // tooltip -- EnchantLore's deliberate fail-soft -- so the mismatch is visible rather
            // than silent, and it fails toward granting nothing.
            if (definition == null || definition.effect() != effect) continue;
            total += DamageEnchants.percentAt(definition.percentByLevel(), active.level());
        }
        return total;
    }
}
