package io.github.butterflysmp.rpg.core.accessory;

/**
 * Which of a player's accessory slots an accessory is authored for: the one class slot, or any of
 * the three universal ones. The content token is the lowercase name ({@code slot: class}).
 *
 * <p>See {@code PLAN-accessories.md} §3.1 for the schema and ruling A1 for why the class slot is
 * gated on the PROFILE class rather than the held weapon.
 */
public enum AccessorySlotKind {
    UNIVERSAL,
    CLASS;

    /** Case-insensitive lookup for the loader. Null on a miss, so the caller names the file. */
    public static AccessorySlotKind fromName(String name) {
        if (name == null) return null;
        for (AccessorySlotKind kind : values()) {
            if (kind.name().equalsIgnoreCase(name)) return kind;
        }
        return null;
    }

    /** The content token, as authored. */
    public String token() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
