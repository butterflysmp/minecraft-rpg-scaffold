package io.github.butterflysmp.rpg.core.build;

/**
 * One cell of the (class, element) grid: the key a pool is filed under.
 *
 * <p>A record, never a concatenated string. Splitting {@code "ranger_fire"} back into its halves
 * invites the {@code ("ranger_f","ire")} collision; a two-field key cannot collide. The kit registry
 * this replaces used the same key for the same reason ({@code KitRegistry.KitKey}).
 *
 * <p>Ids are compared EXACTLY. Nothing lowercases a class or element id anywhere in the codebase, so
 * {@code "Ranger"} is not {@code "ranger"} -- the same spelling rule the profile's class field and
 * {@code AccessorySlots} already follow.
 */
public record CellKey(String classId, String elementId) {
    public CellKey {
        if (classId == null || classId.isBlank()) throw new IllegalArgumentException("cell class required");
        if (elementId == null || elementId.isBlank()) throw new IllegalArgumentException("cell element required");
    }
}
