package io.github.butterflysmp.rpg.core.weapon;

/**
 * The weapon rarity ladder. Deliberately a closed, ordered enum -- the opposite
 * of the element decision. Elements are open and logic-free, so they are content;
 * rarity is a fixed, ordered axis, so it is an enum. The ordering is load-bearing:
 * a later pass compares tiers to size an enchant roll, and enum ordinal gives that
 * ordering for free where a YAML list would hand-roll it.
 *
 * This is still inert reserved data -- it only colors the item name. Its reserved
 * mechanical meaning is the CANDIDATE axis of an enchant roll: how many candidates a
 * slot offers, and how rich a pool it draws from. NOT slot count -- every weapon gets
 * three, fixed, because three is what the table renders and tiering that would need a
 * layout change to go with it. See {@code EnchantRoll.SLOTS}. The
 * tier -> color mapping lives in paper (Adventure's NamedTextColor is not allowed
 * in core), as one exhaustive switch with no default arm, so adding a tier -- if
 * that ever happens -- is a compile error at every mapping site until handled.
 */
public enum Rarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    EXOTIC;

    /**
     * Case-insensitive lookup for the content loader. Returns null on a miss so the
     * caller decides what a bad name means -- the weapon loader throws, turning a bad
     * rarity into a named, skipped file.
     */
    public static Rarity fromName(String name) {
        if (name == null) return null;
        for (Rarity r : values()) {
            if (r.name().equalsIgnoreCase(name)) return r;
        }
        return null;
    }
}
