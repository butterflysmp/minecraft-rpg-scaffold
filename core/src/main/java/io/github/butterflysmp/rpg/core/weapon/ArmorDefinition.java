package io.github.butterflysmp.rpg.core.weapon;

import java.util.List;
import java.util.Optional;

/**
 * One piece of armor, as authored in {@code content/armor/&lt;tier&gt;.yml}.
 *
 * THE THIRD GEAR SHAPE, after {@link WeaponDefinition} and {@link ShieldDefinition}, and the one
 * whose arrival is the trigger those two record for factoring a shared {@code GearDefinition}. That
 * extraction is the IMMEDIATE follow-up PR, not this slice -- deliberately, so a feature slice and a
 * refactor of shipped, stable code do not share a diff. Until then this is a third duplicate BY
 * DESIGN, and the design says it is transient.
 *
 * <p>Its own record for the same hard reason the shield needed one: {@code WeaponDefinition}'s
 * constructor REJECTS an empty trigger list and REQUIRES a {@link WeaponClass}. Armor has neither.
 *
 * <p><b>CORRECTED 2026-11-25 (the seat's A1): {@code defense} IS NOW THE SOURCE OF THE PIECE'S DEFENSE.</b> The scan
 * reads it through {@link ArmorDefenseSource} (scaled by the piece's score), and vanilla's points are read only as the
 * native armor the bar cancels. The paragraph below is kept as the record of slice 1, when it was not:
 *
 * <p><b>{@code defense} IS DISPLAY-ONLY IN THIS SLICE, AND THAT IS THE MOST IMPORTANT THING ON THIS
 * RECORD.</b> The Defense a worn piece actually contributes is read off VANILLA, by
 * {@code DefenseModifierItems.armorOf}, out of {@code ItemType.getDefaultAttributeModifiers} -- not
 * from here. This field feeds the tooltip line and nothing else. Editing it changes the LABEL, not
 * the mitigation, and the two silently disagreeing is exactly the failure {@code ArmorConsistency}
 * exists to shout about at boot. It is authored anyway, rather than read from vanilla at mint time,
 * so the numbers stay in content where the project keeps its numbers -- but it is a MIRROR of
 * vanilla's value, never a lever on it.
 *
 * <p>What it deliberately does NOT carry:
 *
 * <ul>
 *   <li><b>No element.</b> A weapon's element types the damage it deals; armor deals none.
 *       Element-typed MITIGATION -- a helmet that resists fire -- is a real design someone might
 *       want, and it is a later decision, not a field to reserve blank now.
 *   <li><b>No class.</b> Nothing gates armor by melee/ranger/mage. The enchant roll and the enchant
 *       inert-check are keyed on {@link GearClass}, which has no armor constant yet: armor ships
 *       enchant-COMPATIBLE (it carries the container) but not enchant-ROLLED, precisely the line
 *       {@link ShieldDefinition} drew in its own first slice.
 *   <li><b>No triggers.</b> Wearing armor is not an ability.
 *   <li><b>No durability figure.</b> Armor wear stays vanilla's in this slice; weapons and shields
 *       own theirs, armor does not.
 * </ul>
 */
public record ArmorDefinition(
        String id,
        String displayName,
        Rarity rarity,
        String material,
        ArmorSlot slot,
        double defense,
        List<String> flavor,
        Optional<String> craftResult,
        Optional<String> set,
        List<ArmorSetBonus> setBonus
) implements GearDefinition {

    public ArmorDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("armor id must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("armor '" + id + "' has a blank display_name");
        }
        if (rarity == null) {
            throw new IllegalArgumentException("armor '" + id + "' has no rarity");
        }
        if (material == null || material.isBlank()) {
            throw new IllegalArgumentException("armor '" + id + "' has a blank material");
        }
        if (slot == null) {
            throw new IllegalArgumentException("armor '" + id + "' has no slot");
        }
        // REFUSED, not clamped, and written as a NEGATED RANGE so NaN is caught with it: every
        // comparison against NaN is false, so `defense < 0` would wave NaN straight through and mint
        // a piece whose tooltip reads "Defense: NaN".
        //
        // The upper bound is deliberately absent: the number is Ben's (A1, 2026-11-25: it IS the piece's
        // Defense now, not a mirror of vanilla's), and a ceiling here would be a guess about a balance
        // decision. Until 2026-11-25 this comment said vanilla owned the number and ArmorConsistency
        // caught a wrong one; neither is true any more.
        if (!(defense >= 0)) {
            throw new IllegalArgumentException("armor '" + id + "' has defense " + defense
                    + "; it must be zero or more");
        }
        flavor = flavor == null ? List.of() : List.copyOf(flavor);
        craftResult = CraftResultToken.normalise(craftResult, "armor", id);
        // B1 (2026-11-25): the SET is the tier this piece was authored in, and its bonus is that tier's set_bonus:
        // list, carried on all four pieces so the tooltip (B8) reads it off the definition. No set means no bonus.
        set = set == null ? Optional.empty() : set.filter(s -> !s.isBlank());
        setBonus = setBonus == null ? List.of() : List.copyOf(setBonus);
        if (set.isEmpty() && !setBonus.isEmpty()) {
            throw new IllegalArgumentException("armor '" + id + "' declares a set bonus but belongs to no set");
        }
    }

    /** The shape without a set (every caller before B1), so existing callers and tests keep compiling. */
    public ArmorDefinition(String id, String displayName, Rarity rarity, String material, ArmorSlot slot,
                           double defense, List<String> flavor, Optional<String> craftResult) {
        this(id, displayName, rarity, material, slot, defense, flavor, craftResult, Optional.empty(), List.of());
    }

    /**
     * The shape without a craft-result claim, so existing callers and tests keep compiling.
     *
     * <p>Note a claim on armor is per PIECE: one tier file yields four definitions and each names
     * its own slot's item, so this is never "the tier's" craft result.
     */
    public ArmorDefinition(String id, String displayName, Rarity rarity, String material,
                           ArmorSlot slot, double defense, List<String> flavor) {
        this(id, displayName, rarity, material, slot, defense, flavor, Optional.empty());
    }

    /** U5: this kind declares no element (2026-11-23), so its unique slot rolls nothing. */
    @Override
    public Optional<String> gearElement() {
        return Optional.empty();
    }
}
