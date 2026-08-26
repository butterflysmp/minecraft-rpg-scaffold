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

    /** The pane that means "nothing here". Deliberately the dullest item in the game. */
    public static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

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
     * <p>Labelled "Close" with a door, NOT "Back" with an arrow: there is no parent menu yet and a
     * back-arrow promises somewhere to go back to. It says what it does -- returning the weapon is
     * the part a player standing there holding something valuable actually wants to know.
     */
    public static ItemStack close() {
        return icon(Material.OAK_DOOR,
                line("Close", NamedTextColor.RED),
                List.of(line("Returns your weapon.", NamedTextColor.GRAY)));
    }

    /**
     * A feature that is visibly not built yet.
     *
     * <p>Says "not implemented yet" rather than rendering a zero. A readout showing {@code 0%} when
     * nothing is counted is indistinguishable from a working readout that measured zero -- which is
     * the exact failure CLAUDE.md's verification section is about, in a place a player can see.
     */
    public static ItemStack placeholder(Material material, String name, String whatIsMissing) {
        return icon(material,
                line(name, NamedTextColor.DARK_GRAY),
                List.of(line("Not implemented yet.", NamedTextColor.DARK_GRAY),
                        line(whatIsMissing, NamedTextColor.DARK_GRAY)));
    }
}
