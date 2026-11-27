package io.github.butterflysmp.rpg.paper.accessory;

/**
 * H12 (the seat, 2026-11-26): WHAT A BOOK DOES IN VANILLA, AND WHAT STOPS IT FOR A MINTED ACCESSORY. The Healing Tome is
 * the first accessory minted on a material vanilla uses (a {@code book}); the other five materials were chosen inert, so
 * the gear guards were never asked whether they cover accessories. Each row below was READ, and each is closed for OUR
 * item only: a plain book is untouched, because every guard keys on {@code accessory_id}, never on the material.
 *
 * <table>
 *   <caption>A book's vanilla uses on 26.1.2 (READ: the server jar's data and classes)</caption>
 *   <tr><th>use</th><th>READ</th><th>what stops it for a minted accessory</th></tr>
 *   <tr><td>a villager buys it</td><td>{@code villager_trade/librarian/2/book_emerald.json} wants {@code minecraft:book};
 *       four more librarian trades take one as a second cost</td><td>{@link #refusesTrade}, at
 *       {@code PlayerPurchaseEvent} (fired in {@code MerchantResultSlot.onTake} BEFORE {@code MerchantOffer.take}; a
 *       cancel zeroes the result and returns, so the cost slots are never taken)</td></tr>
 *   <tr><td>a chiseled bookshelf takes it</td><td>{@code tags/item/bookshelf_books.json} lists {@code minecraft:book}</td>
 *       <td>{@link #refusesShelf}: a player's click, and a hopper's move into one</td></tr>
 *   <tr><td>a lectern</td><td>{@code tags/item/lectern_books.json}: written and writable books only</td><td>nothing to
 *       stop: a plain book is not accepted</td></tr>
 *   <tr><td>crafting (book and quill, bookshelf, enchanting table)</td><td>{@code recipe/writable_book.json},
 *       {@code bookshelf.json}, {@code enchanting_table.json}</td><td>already: {@code CraftMatrixScreen.isGear} knows
 *       {@code accessory_id}, and every crafting surface refuses CONTAINS_GEAR (the 2x2, the table, the crafter)</td></tr>
 *   <tr><td>the enchanting table, anvil, grindstone</td><td>all three blocks are hijacked into our menus
 *       ({@code RpgListeners.hijackedBlocks})</td><td>already: each refuses an accessory with the A4 words
 *       ({@code AccessoryRefusals})</td></tr>
 * </table>
 *
 * <p>Pure, so each rule is a unit row ({@code AccessoryVanillaUsesTest}); the listeners in {@code RpgListeners}
 * ({@code onAccessoryTrade}, {@code onAccessoryShelved}, {@code onAccessoryHoppered}) read the items and ask here.
 */
public final class AccessoryVanillaUses {

    private AccessoryVanillaUses() {}

    /** A trade is refused when either cost slot holds one of our accessories. */
    public static boolean refusesTrade(boolean firstCostIsAccessory, boolean secondCostIsAccessory) {
        return firstCostIsAccessory || secondCostIsAccessory;
    }

    /** Putting an item into a chiseled bookshelf is refused when the item is one of our accessories. */
    public static boolean refusesShelf(boolean intoChiseledBookshelf, boolean itemIsAccessory) {
        return intoChiseledBookshelf && itemIsAccessory;
    }
}
