package io.github.butterflysmp.rpg.core.weapon;

/**
 * The weapon's mechanical class -- a fixed, closed axis, so an enum, exactly like {@link Rarity}
 * (and the opposite of the open, logic-free element string). A weapon declares one.
 *
 * It is no longer inert. Beyond labelling the tooltip, it is the GATE on class-typed damage
 * modifiers: a {@code +N <Class> Damage} source contributes to its wielder's class-damage stat only
 * while the class of the weapon they HOLD matches, so a mage ring boosts staves and not swords. See
 * {@link ClassDamageModifiers}, which owns that rule, and note the gate is the held weapon's class
 * rather than the payload's shape -- which is what lets the bonus reach a weapon whose damage is an
 * authored literal rather than a stat read.
 *
 * SUMMONER is deliberately absent until it has mechanics: the class -> label mapping in paper is an
 * exhaustive switch with no default arm, so adding a tier is a compile error at every mapping site
 * until handled -- the standing discipline. The tier -> label mapping lives in paper for the same
 * reason {@link Rarity}'s colour does: it is a presentation choice, not core's business.
 */
public enum WeaponClass {
    MELEE,
    RANGER,
    MAGE;

    /**
     * Case-insensitive lookup for the content loader. Returns null on a miss so the caller decides
     * what a bad name means -- the weapon loader throws, turning a bad (or absent) class into a
     * named, skipped file, exactly as it does for a bad rarity.
     */
    public static WeaponClass fromName(String name) {
        if (name == null) return null;
        for (WeaponClass c : values()) {
            if (c.name().equalsIgnoreCase(name)) return c;
        }
        return null;
    }
}
