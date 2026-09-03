package io.github.butterflysmp.rpg.paper.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
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

    /**
     * Menu chrome on top of an item's own lore, separated by one blank line.
     *
     * <p>Extracted so it has a REAL WITNESS. The thing that uses it --
     * {@code CraftingMenu.suggestionIcon} -- needs a live {@code ItemMeta} and a minted stack, so it
     * is boot-gate-only; the ORDERING is not, and it is the half that is easy to get wrong. Same
     * trade {@code CollectPlan} and {@code GridClickIntent} make.
     *
     * <p><b>The rarity footer must stay LAST</b>, exactly as it is on the real item in the player's
     * hand. That is why chrome goes on top rather than appended: a suggestion icon that ended in
     * "Uses items from your inventory" would put the tier badge in the middle of the tooltip, which
     * is the same defect {@code GearLore.appendRarityFooter} warns about from the other direction.
     *
     * <p><b>NO TRAILING BLANK when there is nothing underneath</b>, which is the one case worth
     * naming: an unclaimed vanilla result has no lore of its own, and a separator with nothing after
     * it renders as a stray empty row.
     *
     * <p><b>Deliberately NOT {@code EnchantLore.applied}, which is the same shape and would be wrong
     * here.</b> That method guards on the PREPENDED block being empty; this one guards on the
     * UNDERNEATH being empty. {@code applied(existing, chrome)} with no existing lore yields
     * {@code chrome + blank} -- a trailing separator. The two are close enough that someone will
     * eventually try to merge them, so the difference is written down rather than left to be
     * rediscovered.
     *
     * @param chrome   the menu's own lines. Never null; an empty list yields {@code existing}.
     * @param existing the item's own lore, or null for an item that has none.
     */
    public static List<Component> chromeOver(List<Component> chrome, List<Component> existing) {
        if (chrome == null || chrome.isEmpty()) {
            return existing == null ? List.of() : List.copyOf(existing);
        }
        if (existing == null || existing.isEmpty()) return List.copyOf(chrome);

        List<Component> out = new ArrayList<>(chrome.size() + 1 + existing.size());
        out.addAll(chrome);
        out.add(blank());
        out.addAll(existing);
        return out;
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
     * The close button: a labelled BARRIER, and <b>no lore at all</b>.
     *
     * <p>Labelled "Close" on a BARRIER, not "Back" with an arrow: a back-arrow promises somewhere to
     * go back to, and this closes rather than navigates. (The recipe browser's "Back to crafting" IS
     * a navigation and has its own icon; this is not it.)
     *
     * <h2>THE LORE LINE WENT, AND THE ARGUMENT FOR IT IS KEPT RATHER THAN DELETED</h2>
     *
     * It used to read <i>"Returns your weapon."</i>, and this javadoc used to argue for it:
     *
     * <blockquote>
     * <i>"It says what it does -- returning the weapon is the part a player standing there holding
     * something valuable actually wants to know."</i>
     * </blockquote>
     *
     * <p><b>That argument was sound and is preserved because it is the reasoning, not a mistake.</b>
     * Deleting the paragraph with the line would lose why the line existed, and the next person to
     * think "the close button should explain itself" would have to rediscover it. This is rule 3 in
     * its other direction: an argument can outlive the thing it argued for, and the fix is to say
     * what replaced it.
     *
     * <p><b>What replaced it:</b> the button is now name-only, on operator instruction, for a
     * quieter screen. Two things make the loss small rather than free:
     *
     * <ul>
     *   <li><b>The behaviour was never the BUTTON's.</b> {@code Menu.returnEverything} runs on every
     *       close -- Esc, death, disconnect, shutdown -- so lore on the button implied the return was
     *       a property of clicking it. Gate row 16 closes with Esc precisely because it is not.
     *   <li><b>It is ONE button on both screens</b>, and the enchant table is where the line was most
     *       accurate. Keeping it there was considered and REJECTED: it would mean two close buttons,
     *       which is exactly the drift this class exists to stop -- see the class javadoc. One
     *       slightly plainer button beats two that are subtly different.
     * </ul>
     *
     * <p>Changed 2026-09-03. Gate rows 16, 22 and Q22 are re-run because this appearance changed on
     * BOTH screens, and slice 5's precedent applies: <i>the enchant menu was recoloured; its
     * behaviour must not have moved with its appearance.</i>
     */
    public static ItemStack close() {
        return icon(Material.BARRIER, line("Close", NamedTextColor.RED), List.of());
    }

    /**
     * A feature that is visibly not built yet.
     *
     * <p>Says "not implemented yet" rather than rendering a zero. A readout showing {@code 0%} when
     * nothing is counted is indistinguishable from a working readout that measured zero -- which is
     * the exact failure CLAUDE.md's verification section is about, in a place a player can see.
     *
     * <h2>UNUSED AGAIN, 2026-09-03 — AND KEPT, AS A DECISION WITH A DATE ON IT</h2>
     *
     * <b>KEPT.</b> Not by default -- zero consumers twice is a fair argument for deletion and it was
     * considered. It stays because {@code MenuIcons} is the reusable base ({@code Menu},
     * {@code MenuRouting} and {@code MenuSafety} all landed with no consumer at all), and because
     * the anvil, class-select and stat screens are still ahead and each will want the rule this
     * encodes before it is finished. <b>Delete it if a third graduation arrives with none of those
     * screens built</b> -- at that point "kept for future use" has been wrong twice.
     *
     * <h2>TWO GRADUATIONS, AND THEY ARE DIFFERENT SHAPES</h2>
     *
     * <ol>
     *   <li><b>A placeholder became a READOUT by gaining a SCALE.</b> The enchant table's bookshelf
     *       slot: <i>"0/30"</i> reads as a measurement where a bare <i>"0%"</i> could not. That is
     *       the pattern worth copying.
     *   <li><b>A placeholder became a real FEATURE, and separately, a readout that had been WEARING
     *       THE PLACEHOLDER'S CLOTHES got its own icon.</b> The recipe browser button stopped being
     *       unbuilt; and the browser's empty state -- <i>"Nothing you can make right now"</i> -- had
     *       been built out of {@code placeholder}, so it rendered <b>"Not implemented yet."</b>
     *       underneath a feature that was working correctly and had measured zero.
     * </ol>
     *
     * <p><b>The second half of (2) is the one that shipped a wrong message, and it is this method's
     * own warning inverted.</b> The paragraph above says a bare {@code 0%} cannot be told apart from
     * a working readout that measured zero. The empty state was the working readout that measured
     * zero -- and it was built out of the placeholder, so it announced that the feature was missing.
     * <b>Reaching for this method is a claim that something is NOT BUILT.</b> A surface that
     * correctly measured nothing needs {@link #icon}, and needs to say so in its name.
     *
     * <p>Gate row Q33 <b>would have passed on that defect</b>: it named the notice and not its lore,
     * so an operator would have ticked a SOLE WITNESS while the screen read "not implemented yet".
     * The row is tightened to require the name AND the absence of lore.
     */
    public static ItemStack placeholder(Material material, String name, String whatIsMissing) {
        return icon(material,
                line(name, NamedTextColor.DARK_GRAY),
                List.of(line("Not implemented yet.", NamedTextColor.DARK_GRAY),
                        line(whatIsMissing, NamedTextColor.DARK_GRAY)));
    }
}
