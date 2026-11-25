package io.github.butterflysmp.rpg.core.weapon;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Where a worn piece's Defense comes from (the seat's A1 and A3, 2026-11-25).
 *
 * <p><b>A1: THE AUTHORED NUMBER IS THE SOURCE.</b> A piece contributes the {@code defense:} authored in its content
 * file, scaled by its own gear score ({@code GearScore.scaledDefense}). Until 2026-11-25 that number was display-only
 * and the stat read VANILLA's armor points for the material; Ben's 5/8/7/4 ladder is what retired that, since no
 * vanilla material grants it. The vanilla points are still read, but only as the {@code nativeArmor} the armor bar
 * cancels, which this class has nothing to do with.
 *
 * <p><b>A3: ONE LOOKUP BY MATERIAL.</b> A piece's content id IS its material token ({@code ArmorLoader}), so a tagged
 * piece and an untagged one of the same material resolve to the same definition: the tag first, then the material.
 * There is no second table. An untagged piece has no score of its own, so the caller reads it at the base.
 *
 * <p>Empty when no content piece covers the material in that slot: the turtle helmet (C2: excluded by Ben), anything
 * worn outside its own slot, or a slot holding something that is not armor. What the caller does then is its own
 * decision; see {@code DefenseModifierItems.scan}.
 */
public final class ArmorDefenseSource {

    private ArmorDefenseSource() {}

    /**
     * @param taggedId      the piece's {@code armor_id} tag, if it is one of ours
     * @param materialToken the item's material as a content token ({@code diamond_helmet})
     * @param slot          the slot it is worn in
     */
    public static OptionalDouble authored(ArmorRegistry armor, Optional<String> taggedId, String materialToken,
                                          ArmorSlot slot) {
        Optional<ArmorDefinition> piece = resolve(armor, taggedId, materialToken, slot);
        return piece.isPresent() ? OptionalDouble.of(piece.get().defense()) : OptionalDouble.empty();
    }

    /** The definition a worn item resolves to: the ONE lookup, used for Defense here and for the set decision (B2). */
    public static Optional<ArmorDefinition> resolve(ArmorRegistry armor, Optional<String> taggedId,
                                                    String materialToken, ArmorSlot slot) {
        return taggedId.flatMap(armor::find)
                .or(() -> materialToken == null ? Optional.empty() : armor.find(materialToken))
                .filter(p -> p.slot() == slot);
    }
}
