package io.github.butterflysmp.rpg.core.weapon;

/**
 * What a refresh should do with one inventory slot, decided from its weapon id alone.
 *
 * The Lore Refresher rebuilds an old item's DISPLAY -- name, lore, material, the melee
 * suppressor -- from the weapon's current definition. Its behaviour was never stale: swinging
 * an emberblade reads weapon_id off the item and looks up the CURRENT WeaponDefinition, so
 * triggers and attack damage are already current. Only what mint() baked can drift. An item is
 * therefore weapon_id (persistent) plus a display derived from content (a cache), and a refresh
 * rebuilds the cache.
 *
 * This is the pure half of that pass, and it is in core for the same reason
 * {@link ClassDamageModifiers} is: its paper counterpart CANNOT be unit-tested. Scanning an
 * inventory needs live ItemStacks, and {@code new ItemStack(...)} throws without a running
 * server (see WeaponItemsTest) -- so the ItemStack read, the re-mint and the instance carry-forward
 * are all boot-witnessed. The decision they hang off is not, and the decision is where the two
 * failures that matter live:
 *
 *  - re-minting something that is not ours, which would rewrite a player's vanilla item;
 *  - "cleaning up" an id whose content file is gone, which would destroy a real item over a typo.
 *
 * Both are silent in review and loud in the world, so they are pinned here instead.
 *
 * A sealed interface means only the three records below may implement it, so a switch over a
 * verdict can be proven exhaustive by the compiler. The scan site switches with NO default arm --
 * a fourth outcome then becomes a compile error there until it is handled, rather than being
 * silently swallowed by a catch-all.
 */
public sealed interface RefreshVerdict {

    /** No weapon id: not one of ours. Leave the slot completely alone, and say nothing. */
    record Untagged() implements RefreshVerdict {}

    /**
     * Tagged with an id no loaded weapon claims -- the content file was renamed or deleted.
     * Leave the item exactly as it is and report it once. Never strip the tag and never replace
     * the item: a content typo must not cost a player the thing they earned, and the item starts
     * working again the moment the definition comes back.
     *
     * Carries the id so the warning can name it; a warning that cannot say WHICH id is missing
     * sends you looking through every weapon file.
     */
    record Dangling(String id) implements RefreshVerdict {}

    /** Ours, and the content is loaded: rebuild the display from this definition. */
    record Remint(GearDefinition definition) implements RefreshVerdict {}

    /** Allocation-free {@link Untagged}: the overwhelmingly common verdict, once per empty slot. */
    RefreshVerdict UNTAGGED = new Untagged();

    /**
     * Decide what one slot needs, given the weapon id read off it (null when the item carries
     * none, which includes an empty slot) and the CURRENTLY loaded weapons.
     *
     * The registry is a parameter rather than anything cached because "current" is the entire
     * point of the pass -- the definition a refresh mints from must be the one loaded now, not
     * one captured when the item was first minted.
     */
    static RefreshVerdict decide(String id, GearRegistry<? extends GearDefinition> registry) {
        if (id == null) return UNTAGGED;
        return registry.find(id)
                .<RefreshVerdict>map(Remint::new)
                .orElseGet(() -> new Dangling(id));
    }
}
