package io.github.butterflysmp.rpg.paper.command;

import io.github.butterflysmp.rpg.core.enchant.DamageEnchants;
import io.github.butterflysmp.rpg.core.enchant.Unbreaking;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.weapon.WeaponClassLabel;

/**
 * What an active enchant actually RESOLVES to on the weapon it is sitting on, as a suffix on the
 * "active:" line -- and whether it is inert there. ONE exhaustive switch on
 * {@code EnchantEffect}, shared by {@code /rpg enchant active}'s reply and by {@code show}.
 *
 * <p><b>The sharing is the point.</b> These were two describers, and they had drifted in opposite
 * directions: the activation reply appended Unbreaking's consume rate for EVERY enchant, so
 * activating Sharpness reported "consumes durability on 25% of uses", while show returned "" for a
 * durability enchant and so never printed the rate at all. Each covered exactly what the other
 * dropped. One switch, no default arm, cannot drift.
 *
 * <p>It exists so the boot gate can read the EXPECTED number off the screen BEFORE swinging,
 * rather than deciding afterwards what the number it got should have been. That matters most on
 * the damage arm: the popup ROUNDS, so at the shipped [5, 10, 15] curve a Sharpness I sword
 * renders "8" exactly like an unenchanted one. Without this line the gate could not tell "the
 * enchant is inert" from "the enchant applied and rounding hid it".
 *
 * <p>The class mismatch is called out explicitly for the same reason -- Sharpness on a bow is
 * SUPPOSED to do nothing, and a silent absence of change looks identical to a broken gate. It is
 * said at ACTIVATION and not only in show, because that is the moment the mistake is correctable:
 * the alternative to the inert note is not silence, it is "(+15% damage, x1.15)" on a weapon where
 * that multiplier never applies.
 */
final class EnchantEffectLine {

    private EnchantEffectLine() {}

    /**
     * The parenthetical suffix, leading space included, ready to concatenate.
     *
     * @param definition the enchant's content file, or {@code null} for an id the registry no
     *                   longer knows -- reachable, because the loader fail-softs a malformed file
     *                   and the item's blob still names it.
     * @param level      the resolved (effective) level, not the candidate's own.
     * @param heldClass  the class of the weapon the enchant is sitting on. Never null:
     *                   {@code WeaponDefinition}'s constructor rejects a null class and
     *                   {@code WeaponLoader} refuses a file without one.
     */
    static String of(EnchantDefinition definition, int level, WeaponClass heldClass) {
        if (definition == null) return " (unknown enchant -- grants nothing)";

        // No default arm, deliberately -- the same discipline as WeaponClassLabel.of. A third
        // EnchantEffect constant is a compile error here until someone gives it words.
        return switch (definition.effect()) {
            case DURABILITY -> " (consumes durability on "
                    + Math.round(Unbreaking.consumeChance(level) * 100) + "% of uses)";
            case DAMAGE -> {
                if (!definition.isUniversal() && definition.weaponClass() != heldClass) {
                    yield " (inert: a " + WeaponClassLabel.of(definition.weaponClass())
                            + " enchant on a " + WeaponClassLabel.of(heldClass) + " weapon)";
                }
                double percent = DamageEnchants.percentAt(definition.percentByLevel(), level);
                yield String.format(" (+%.0f%% damage, x%.2f)", percent,
                        DamageEnchants.multiplier(percent));
            }
        };
    }
}