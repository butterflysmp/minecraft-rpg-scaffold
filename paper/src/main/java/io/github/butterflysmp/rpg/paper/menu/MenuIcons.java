package io.github.butterflysmp.rpg.paper.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * The furniture every menu is built out of: filler panes, a close button, a labelled placeholder.
 *
 * <p>Shared here rather than per-menu so a second menu looks like the first one. Chrome that
 * drifts between screens reads as two different plugins.
 */
public final class MenuIcons {

    private MenuIcons() {}

    /**
     * The pane that means "nothing here". Deliberately the dullest item in the game.
     *
     * <p><b>BLACK since slice 5, and changed HERE so both menus move together.</b> That is the whole
     * reason this constant is shared rather than per-menu -- see the class javadoc: chrome that
     * drifts between screens reads as two different plugins. The enchant table is fully black too,
     * with no exception.
     *
     * <p><b>GRAY is no longer chrome, and that matters to more than the eye.</b> It is now the
     * status bar's EMPTY colour, so a gray pane in this menu is a READOUT rather than furniture.
     * Gate row S12 had to be rewritten for exactly this: it said "hold panes matching the filler",
     * and an operator holding gray panes after this change would have tested nothing and passed.
     */
    public static final Material FILLER = Material.BLACK_STAINED_GLASS_PANE;

    /**
     * An EMPTY QUICK-CRAFT CELL. Not a second filler -- a cell that is waiting to hold something.
     *
     * <p>The suggestion column is often short: with three cells and an ordinary inventory, one or
     * two may have nothing to show. Painted in {@link #FILLER} they were invisible, and a column
     * that vanishes when it is short reads as a broken feature rather than an empty one.
     *
     * <p><b>LIGHT gray, and the distinction from plain gray is LOAD-BEARING rather than a shade
     * preference.</b> Plain {@code GRAY_STAINED_GLASS_PANE} is {@code CraftStatus.EMPTY}'s colour --
     * the status bar's "grid is empty" state -- and gate rows S12b and S12c exist to pin the two
     * apart. Using it here would put two meanings on one material in one screen, which is exactly
     * how row S12 rotted when the chrome went black: an operator holding panes "matching the
     * filler" tested nothing and the row passed.
     *
     * <p>It reads as gray to a player and is a different material to the code, which is the whole
     * point. <b>Anything that collapses this and the status bar's gray into one constant is a
     * REGRESSION, not a simplification</b> -- and every other gate row would still pass after it.
     */
    public static final Material EMPTY_SUGGESTION = Material.LIGHT_GRAY_STAINED_GLASS_PANE;

    /**
     * A blank pane in a given colour, for a readout rather than for chrome.
     *
     * <p>Same shape as {@link #filler()} -- an empty display name, because a blank name still hovers
     * -- so a status cell and a chrome cell are visually identical apart from the colour, which is
     * the only thing carrying meaning.
     */
    public static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> meta.displayName(Component.empty()
                .decoration(TextDecoration.ITALIC, false)));
        return item;
    }

    /**
     * A named, non-italic lore line.
     *
     * <p>The explicit {@code decoration(ITALIC, false)} is load-bearing and must not be
     * "simplified" away: lore and display names render italic by DEFAULT, so dropping the call
     * leaves the line italic via NOT_SET rather than making it plain. Same note EnchantLore carries.
     */
    public static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    /** A blank lore line, non-italic for the same reason. */
    public static Component blank() {
        return Component.empty().decoration(TextDecoration.ITALIC, false);
    }

    /** A display item: a material, a name, and lore. No behaviour, no PDC, nothing to carry. */
    public static ItemStack icon(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(name);
            meta.lore(lore);
        });
        return item;
    }

    /** A filler pane, with no name at all rather than a blank one -- a blank name still hovers. */
    public static ItemStack filler() {
        ItemStack item = new ItemStack(FILLER);
        item.editMeta(meta -> meta.displayName(Component.empty()
                .decoration(TextDecoration.ITALIC, false)));
        return item;
    }

    /**
     * The close button.
     *
     * <p>Labelled "Close" on a BARRIER, not "Back" with an arrow: there is no parent menu yet and a
     * back-arrow promises somewhere to go back to. It says what it does -- returning the weapon is
     * the part a player standing there holding something valuable actually wants to know.
     */
    public static ItemStack close() {
        return icon(Material.BARRIER,
                line("Close", NamedTextColor.RED),
                List.of(line("Returns your weapon.", NamedTextColor.GRAY)));
    }

    /**
     * A feature that is visibly not built yet.
     *
     * <p>Says "not implemented yet" rather than rendering a zero. A readout showing {@code 0%} when
     * nothing is counted is indistinguishable from a working readout that measured zero -- which is
     * the exact failure CLAUDE.md's verification section is about, in a place a player can see.
     *
     * <p><b>Currently unused, and kept on purpose.</b> Its only consumer was the enchant table's
     * bookshelf slot, which now prints a real count. It stays because {@code MenuIcons} is the
     * reusable base -- {@code Menu}, {@code MenuRouting} and {@code MenuSafety} all landed with no
     * consumer at all -- and because the rule it encodes is one the anvil, class-select and stat
     * screens will each need before they are finished. The graduation is also the pattern worth
     * copying: a placeholder becomes a readout by gaining a SCALE, so "0/30" reads as a measurement
     * where a bare "0%" could not.
     */
    public static ItemStack placeholder(Material material, String name, String whatIsMissing) {
        return icon(material,
                line(name, NamedTextColor.DARK_GRAY),
                List.of(line("Not implemented yet.", NamedTextColor.DARK_GRAY),
                        line(whatIsMissing, NamedTextColor.DARK_GRAY)));
    }
}
