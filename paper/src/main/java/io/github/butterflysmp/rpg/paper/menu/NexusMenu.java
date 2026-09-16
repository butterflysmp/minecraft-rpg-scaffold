package io.github.butterflysmp.rpg.paper.menu;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * The Nexus hub: the screen the Nexus star opens, and the first one a player sees.
 *
 * <p>Slice 2 is deliberately almost nothing -- a close button and a settings button that does not
 * work yet. What it establishes is the ROUTE: the star in the locked slot opens a screen of ours.
 * Everything the hub will hold hangs off that, and none of it can be built until the route is
 * confirmed in play.
 *
 * <h2>THE SETTINGS TORCH IS A {@code placeholder}, AND THE STATS HEAD NEXT SLICE MUST NOT BE</h2>
 *
 * <b>Written down here, one slice early, because the second half lands later and by then the
 * reason will have to be rediscovered.</b>
 *
 * <p>The settings button uses {@link MenuIcons#placeholder}, whose lore reads <i>"Not implemented
 * yet."</i> That is CORRECT here: the settings screen is <b>genuinely not built</b>. There is no
 * behaviour behind the torch, nothing to read off it, and nothing a click could do.
 *
 * <p><b>The stats head coming in slice 3 is the opposite case and needs {@link MenuIcons#icon}.</b>
 * It will carry REAL lore -- a working readout of live figures -- and only its CLICK will be
 * unbuilt. Rendering it with {@code placeholder} would print <i>"Not implemented yet."</i> above
 * correct, live stat numbers.
 *
 * <p><b>That is not hypothetical: it is the defect {@code MenuIcons.placeholder}'s own javadoc
 * records.</b> The recipe browser's empty state -- a feature that worked and had measured zero --
 * was built out of {@code placeholder} and announced itself as missing. <b>Gate row Q33 would have
 * passed on it</b>, because it named the notice and not its lore.
 *
 * <p>So the distinction is <b>WHETHER THE THING IS BUILT, not whether it is clickable</b>:
 *
 * <pre>
 *   settings torch   nothing behind it at all          placeholder()   "Not implemented yet."
 *   stats head       real lore, unbuilt click only     icon()          its own figures
 * </pre>
 *
 * <p>Reaching for {@code placeholder} is a claim that something is NOT BUILT. A surface that
 * renders something true needs {@code icon}, however inert its click.
 */
public final class NexusMenu extends Menu {

    public NexusMenu(Player viewer) {
        super(viewer, NexusMenuLayout.SIZE, MenuIcons.line("Nexus", NamedTextColor.DARK_GRAY));
        render();
    }

    /**
     * Nothing. Every slot is chrome.
     *
     * <p>Ruled by the operator in the first dialog: <b>the hub holds nothing.</b> It is also what
     * makes the screen safe by construction rather than by guarding -- with no input slots the
     * router performs no moves, the drag handler permits nothing, and there is no state a close
     * could strand. Same property {@code RecipeBrowserMenu} relies on.
     */
    @Override
    protected Set<Integer> inputSlots() {
        return Set.of();
    }

    @Override
    protected boolean acceptsInput(ItemStack cursor) {
        return false;
    }

    @Override
    protected void onClick(MenuClick click) {
        if (click.slot() == NexusMenuLayout.CLOSE_SLOT) {
            // Closes, and nothing else -- the same shape CraftingMenu uses, so the button and the
            // escape key cannot drift apart. There is nothing to hand back either way.
            viewer.closeInventory();
            return;
        }
        // The settings torch and every filler pane are inert. Falling through rather than branching
        // on SETTINGS_SLOT deliberately: a no-op branch for it would read as a wired button whose
        // body someone forgot to write, which is the opposite of what the placeholder is saying.
    }

    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        // Nothing to return: inputSlots() is empty, so the player never had anything in here.
    }

    private void render() {
        for (int slot : NexusMenuLayout.FILLER_SLOTS) {
            getInventory().setItem(slot, MenuIcons.filler());
        }

        // THE SHARED BARRIER. Not a second close button minted here -- chrome that drifts between
        // screens reads as two different plugins, which is MenuIcons' whole reason for existing.
        getInventory().setItem(NexusMenuLayout.CLOSE_SLOT, MenuIcons.close());

        // placeholder(), NOT icon(). See the class javadoc: this feature is genuinely not built,
        // and the stats head next slice is the case that must go the other way.
        getInventory().setItem(NexusMenuLayout.SETTINGS_SLOT, MenuIcons.placeholder(
                Material.REDSTONE_TORCH, "Settings", "No settings to change yet."));
    }
}
