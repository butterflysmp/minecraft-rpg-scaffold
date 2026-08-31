package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.DamageEnchants;
import io.github.butterflysmp.rpg.core.enchant.EnchantCurve;
import io.github.butterflysmp.rpg.core.enchant.Unbreaking;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;

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
public final class EnchantEffectLine {

    private EnchantEffectLine() {}

    /**
     * The parenthetical suffix, leading space included, ready to concatenate.
     *
     * @param definition the enchant's content file, or {@code null} for an id the registry no
     *                   longer knows -- reachable, because the loader fail-softs a malformed file
     *                   and the item's blob still names it.
     * @param level      the resolved (effective) level, not the candidate's own.
     * @param heldClass  the {@link GearClass} of the GEAR the enchant is sitting on. Still never
     *                   null, and now honestly so for both kinds: a weapon maps through
     *                   {@code GearClass.of} (its loader refuses a file without a class) and a
     *                   shield presents {@code SHIELD}.
     *                   <p>
     *                   <b>This is the Slice-1 descope reversed.</b> {@code /rpg enchant show}
     *                   refused shields, and {@code HeldGear.effectSuffix} returned "", because
     *                   there was no honest value to pass here -- not because anything crashed. No
     *                   caller ever passed null. Supplying a real value removes the reason those
     *                   refusals existed rather than fixing a bug they were hiding.
     */
    public static String of(EnchantDefinition definition, int level, GearClass heldClass) {
        return " (" + bare(definition, level, heldClass) + ")";
    }

    /**
     * The same sentence with no brackets and no leading space -- what a LORE LINE needs, where a
     * parenthetical aside reads as an afterthought rather than as the description.
     *
     * <p>Extracted rather than stripped at the call site so there is still exactly ONE exhaustive
     * switch on {@code EnchantEffect}. Two describers is what this class was created to end, and a
     * caller trimming the brackets off itself is how the second one comes back.
     *
     * <p><b>Never called with level 0.</b> A LOCKED candidate is described at the level it would
     * BECOME, because the arms below are total over levels rather than guarded: level 0 would read
     * "+0% damage" for a damage enchant and, worse, "consumes durability on 100% of uses" for
     * Unbreaking -- which is backwards, and reads as a curse. The enchant menu passes
     * {@code Math.max(1, level)} and says "Click to unlock at I" beneath, so the two agree.
     */
    public static String bare(EnchantDefinition definition, int level, GearClass heldClass) {
        if (definition == null) return "unknown enchant -- grants nothing";

        // No default arm, deliberately -- the same discipline as WeaponClassLabel.of. A third
        // EnchantEffect constant is a compile error here until someone gives it words.
        return switch (definition.effect()) {
            case DURABILITY -> "consumes durability on "
                    + Math.round(Unbreaking.consumeChance(level) * 100) + "% of uses";
            case DAMAGE -> {
                if (!definition.isUniversal() && definition.gearClass() != heldClass) {
                    // The held side is a NOUN PHRASE, not a bare label with "weapon" glued on.
                    // The old wording hardcoded "... on a X weapon", which reads correctly for the
                    // three fighting classes and absurdly for the fourth -- "on a Shield weapon".
                    // Byte-identical for every weapon case, so the existing assertions are unchanged.
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                double percent = DamageEnchants.percentAt(definition.valueByLevel(), level);
                yield String.format("+%.0f%% damage, x%.2f", percent,
                        DamageEnchants.multiplier(percent));
            }
            case BLOCK_DR -> {
                // The gate is enforced at the content boundary (a BLOCK_DR enchant must be
                // class: shield), so the only way to be holding one on the wrong gear is the dev
                // command or a hand-edited item -- reachable, so it is described rather than assumed
                // away.
                if (definition.gearClass() != heldClass) {
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                // THE SAME WORDS THE ITEM USES. ShieldLoreLines.DAMAGE_REDUCTION_LABEL says "Damage
                // Reduction" on the shield itself, so the enchant that modifies that stat must not
                // call it something else -- a player reading "+15% block" above "Damage Reduction:
                // 50%" has to work out that those are one number.
                //
                // POINTS, not a multiplier: Bulwark is additive on the fraction, so "+15%" means the
                // shield stops fifteen more POINTS of the hit (0.35 -> 0.50), not fifteen percent
                // more of what it already stopped. Saying "x1.15" would describe the rejected
                // reading. The gate reads this line before blocking, so it must be the real number.
                double percent = EnchantCurve.valueAt(definition.valueByLevel(), level);
                yield String.format("+%.0f%% Damage Reduction", percent);
            }
            case REFLECT -> {
                // Same gate story as BLOCK_DR: refused at the content boundary, so the only way to
                // hold one on the wrong gear is the dev command or a hand-edited item. Reachable,
                // so it is described rather than assumed away.
                if (definition.gearClass() != heldClass) {
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                // "to the attacker" is not decoration -- it is the one word that stops this reading
                // as a damage bonus to your own hits. And the percent is of the INCOMING blow, not
                // of what got through, which is why the wording says nothing about blocking.
                double percent = EnchantCurve.valueAt(definition.valueByLevel(), level);
                yield String.format("+%.0f%% reflected to the attacker", percent);
            }
            case DEFENSE -> {
                if (definition.gearClass() != heldClass) {
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                // NO PERCENT SIGN, and that is the whole difference from the three arms above.
                // Defense is a SUMMAND in armor points -- the piece's own Defense line says
                // "Defense: 8", and this adds to that number, not to a fraction of it. Writing
                // "+9%" here would describe an enchant that does not exist and would disagree with
                // the item two lines up.
                //
                // THE SAME WORD THE ITEM USES: ArmorLoreLines.DEFENSE_LABEL prints "Defense" on the
                // piece itself, so the enchant that modifies that stat must not call it something
                // else. Same rule the BLOCK_DR arm follows for "Damage Reduction".
                double points = EnchantCurve.valueAt(definition.valueByLevel(), level);
                yield String.format("+%.0f Defense", points);
            }
            case MAX_HEALTH -> {
                if (definition.gearClass() != heldClass) {
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                // Points again, for the same reason. "Max Health" rather than "Health": the enchant
                // raises the CEILING and grants no current health at all -- equipping is headroom,
                // never a heal -- and a line reading "+30 Health" would promise the heal.
                double points = EnchantCurve.valueAt(definition.valueByLevel(), level);
                yield String.format("+%.0f Max Health", points);
            }
            case MAX_MANA -> {
                if (definition.gearClass() != heldClass) {
                    yield "inert: " + GearClassLabel.describeEnchant(definition.gearClass())
                            + " on " + GearClassLabel.describe(heldClass);
                }
                // Points, and "Max Mana" rather than "Mana", for the identical reasons the arm
                // above gives: the enchant raises the CEILING of a pool and grants no current mana,
                // so "+30 Mana" would promise a top-up the pin deliberately withholds. Equipping is
                // headroom here too -- ResourcePool.setCurrent is what makes that true even for a
                // player who has never cast.
                double points = EnchantCurve.valueAt(definition.valueByLevel(), level);
                yield String.format("+%.0f Max Mana", points);
            }
        };
    }
}