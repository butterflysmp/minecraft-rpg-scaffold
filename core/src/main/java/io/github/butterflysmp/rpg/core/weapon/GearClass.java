package io.github.butterflysmp.rpg.core.weapon;

/**
 * The axis an ENCHANT is gated on: what kind of gear it may sit on. A fixed, closed set, so an enum
 * -- exactly like {@link WeaponClass} and {@link Rarity}.
 *
 * <p><b>This is not a rename of {@link WeaponClass} with a fourth constant, and the difference is
 * the whole reason it exists.</b> {@code WeaponClass} answers "what does this weapon fight as", and
 * three things read it: the tooltip label, the weapon-type footer, and {@link ClassDamageModifiers}
 * (a ring's {@code +N <Class> Damage} gates on the weapon in your hand). None of those has any
 * answer for a shield. This enum answers a different question -- "what may this enchant sit on" --
 * and {@link #SHIELD} is the only value with a live enchant reader that is not on the weapon axis.
 * Merging the two would put a SHIELD arm inside the weapon tooltip's switch, where it is meaningless.
 *
 * <p><b>Why a second enum rather than SHIELD on {@code WeaponClass}.</b> {@code EnchantDefinition}'s
 * javadoc used to argue the opposite -- that reusing one enum was worth it because "a parallel enum
 * would need SUMMONER adding in two places the day that class lands, and the exhaustive-switch
 * discipline only works with one enum". The first half is true and accepted. The second half is not:
 * {@link #of} is itself an exhaustive switch with no default arm, so adding SUMMONER to
 * {@code WeaponClass} is a COMPILE ERROR here until it is given a gear class. Two places, one of
 * which the compiler names. The alternative -- SHIELD on {@code WeaponClass} -- would force a
 * shield-shaped hole through every exhaustive weapon switch, starting with
 * {@code WeaponClassLabel.of}, which has no default arm and no words for it.
 *
 * <p>{@code null} is not a constant here: it is the {@code universal} gate, meaning no gate at all.
 * Same null-means-no-gate convention {@code EnchantDefinition.isUniversal},
 * {@code EnchantRoll.poolFor} and {@code DamageEnchants.matching} already share.
 */
public enum GearClass {
    MELEE,
    RANGER,
    MAGE,

    /**
     * A shield. Has no {@link WeaponClass} and never will -- it does not fight, it blocks -- which
     * is why {@link #of} can never produce this value and only a shield presents it directly.
     */
    SHIELD;

    /**
     * Case-insensitive lookup for the content loader. Returns null on a miss so the CALLER decides
     * what a bad name means -- the enchant loader throws, turning a bad (or absent) class into a
     * named, skipped file, exactly as it does for a bad effect.
     *
     * <p>Note this cannot distinguish "unknown token" from "universal": {@code universal} is not a
     * constant here and is handled by the loader before it reaches this method. Both arrive as null,
     * which is why the loader tests the {@code universal} token FIRST and treats anything else that
     * misses as an error.
     */
    public static GearClass fromName(String name) {
        if (name == null) return null;
        for (GearClass c : values()) {
            if (c.name().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    /**
     * The gating class a weapon of this fighting class presents.
     *
     * <p><b>Exhaustive, with NO default arm, deliberately</b> -- the same discipline as
     * {@code WeaponClassLabel.of}. A new {@code WeaponClass} constant is a compile error here until
     * someone decides what it gates as, which is the mechanism that keeps the two enums from
     * drifting apart silently.
     *
     * <p>Null in, null out. An empty hand -- or a hand holding nothing of ours -- has no gating
     * class, and that null is the same one {@code universal} means, so it flows into
     * {@code DamageEnchants.matching}'s existing null arm unchanged rather than needing a new one.
     */
    public static GearClass of(WeaponClass weaponClass) {
        if (weaponClass == null) return null;
        return switch (weaponClass) {
            case MELEE  -> MELEE;
            case RANGER -> RANGER;
            case MAGE   -> MAGE;
        };
    }
}
