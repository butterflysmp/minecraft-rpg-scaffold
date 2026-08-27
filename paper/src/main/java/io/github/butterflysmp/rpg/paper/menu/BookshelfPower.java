package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.enchant.BookshelfRing;
import io.github.butterflysmp.rpg.core.enchant.EnchantCost;
import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * How many bookshelves are around an enchanting table.
 *
 * <p>The whole impure half of the bookshelf discount, and deliberately almost nothing: walk
 * {@link BookshelfRing}'s offsets, read a block, compare a material. Every decision that could be
 * subtly wrong -- which positions, how many layers, what the inner skip is -- already happened in
 * {@code core} where a test can see it. What is left needs a live world and is owed to the boot
 * gate, which is exactly as much as should ever be owed there.
 *
 * <p><b>Called once, at open, from the interact event for this very block.</b> Not from a click
 * handler: under Folia the table's region is not an inventory click's to assume, and the interact
 * event is already running on the thread that owns the block being clicked. {@code EnchantMenu}
 * keeps the resulting {@code int} and never the {@code Block}, so there is nothing for a later
 * re-read to be written against.
 */
final class BookshelfPower {

    private BookshelfPower() {}

    /**
     * The power this table has, capped at {@link EnchantCost#MAX_POWER}.
     *
     * <p>Capped HERE as well as inside {@link EnchantCost#clampPower} and the two are not redundant.
     * This one keeps the readout honest -- a full ring is 32 positions and the screen must never say
     * 32/30. That one keeps the price total for any caller at all.
     *
     * <p>Zero is a real answer, not a failure: a bare table has no shelves. That is why this returns
     * a count rather than refusing to find nothing, and why the readout prints the scale beside it --
     * "0/30" is legible as a measurement where a bare "0%" would not be.
     */
    static int at(Block table) {
        int found = 0;
        for (BookshelfRing.Offset offset : BookshelfRing.offsets()) {
            if (table.getRelative(offset.dx(), offset.dy(), offset.dz()).getType()
                    == Material.BOOKSHELF) {
                found++;
            }
        }
        return Math.min(found, EnchantCost.MAX_POWER);
    }
}
