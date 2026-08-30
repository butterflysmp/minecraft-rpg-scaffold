package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.GearClass;

/**
 * The {@link GearClass} -> display words mapping: the one place an enchant's GATE becomes tooltip
 * English. Exhaustive switches with no default arm, like {@code RarityColors} and
 * {@link WeaponClassLabel}, so a new constant is a compile error here until it is given words.
 *
 * <p><b>A sibling of {@link WeaponClassLabel}, deliberately not a replacement for it.</b> That one
 * labels the WEAPON axis and is read by the weapon tooltip's damage line, the weapon-type footer and
 * the class-damage ring's name -- three places where "Shield" is meaningless. Merging them would put
 * a SHIELD arm inside the weapon tooltip's switch. Two switches, two axes, and the duplication is
 * three words wide.
 */
public final class GearClassLabel {

    private GearClassLabel() {}

    /**
     * The bare label, for naming the ENCHANT's own gate: "a Melee enchant", "a Shield enchant".
     *
     * <p>Note the token/label asymmetry inherited from the weapon axis: the content token is
     * {@code ranger} and the label is "Ranged".
     */
    public static String of(GearClass gearClass) {
        return switch (gearClass) {
            case MELEE  -> "Melee";
            case RANGER -> "Ranged";
            case MAGE   -> "Magic";
            case SHIELD -> "Shield";
            case ARMOR  -> "Armor";
        };
    }

    /**
     * The whole noun phrase, for naming the gear an enchant is SITTING ON: "a Magic weapon",
     * "a shield".
     *
     * <p>This exists because the inert sentence used to end in a hardcoded "weapon"
     * ({@code "... on a " + label + " weapon"}), which reads correctly for the three fighting
     * classes and absurdly for the fourth -- "a Melee enchant on a Shield weapon". A shield is not a
     * weapon of class shield; it is a shield.
     *
     * <p><b>Built on {@link #of} rather than repeating the words</b>, so there is still exactly ONE
     * place each label is spelled. The switch stays exhaustive with no default arm: the three-class
     * arm lists its constants explicitly rather than defaulting, so a new constant still fails to
     * compile here.
     */
    public static String describe(GearClass gearClass) {
        return switch (gearClass) {
            case MELEE, RANGER, MAGE -> "a " + of(gearClass) + " weapon";
            case SHIELD              -> "a shield";
            // "a piece of armor", not "an Armor armor": the inert sentence reads "... on a piece of
            // armor", and armor is the one gear kind whose label is not also its noun.
            case ARMOR               -> "a piece of armor";
        };
    }
}
