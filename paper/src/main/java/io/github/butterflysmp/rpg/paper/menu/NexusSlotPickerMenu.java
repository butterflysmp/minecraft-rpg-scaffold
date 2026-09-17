package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.nexus.NexusSlots;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Choose which of the 36 main inventory cells the Nexus star lives in.
 *
 * <p>Reached from the Settings hub, which is reached from the Nexus:
 * {@code Nexus -> Settings -> Nexus Slot}. It was the settings screen itself until slice 10 turned
 * that into a screen of buttons.
 *
 * <h2>*** THIS MENU CONTAINS CLONES OF EVERY ITEM THE PLAYER OWNS ***</h2>
 *
 * An occupied cell renders <b>the real item</b>, cloned, so the player sees their own inventory
 * rather than a grid of numbered panes. <b>That is the feature and it is also the hazard: if ANY
 * route lets one of those clones leave this screen, it is free duplication of anything in their
 * inventory.</b> Not a display bug -- an economy hole, and one that scales with how much the
 * player is carrying.
 *
 * <h2>THE DEFENCE, AND WHY "DEFAULT-CANCEL HANDLES IT" IS AN EXPECTATION RATHER THAN EVIDENCE</h2>
 *
 * <ul>
 *   <li><b>{@link #inputSlots()} is EMPTY.</b> With no declared inputs the router performs no
 *       moves, the drag handler permits nothing, and there is no state a close could strand --
 *       the same property {@code NexusMenu} and {@code RecipeBrowserMenu} rely on.</li>
 *   <li><b>{@link #acceptsInput} returns false UNCONDITIONALLY</b>, not "false for items that are
 *       not gear". There is no cursor content this screen should ever accept.</li>
 * </ul>
 *
 * <p><b>Those two make the refusal true BY CONSTRUCTION, and the gate still reads three separate
 * rows for it.</b> {@code MenuRouting} handles shift-click, the number keys and F on <b>three
 * different paths</b>, and <b>two of them act on the HOVERED slot rather than on a declared
 * input</b> -- so a build that refuses one can permit another, and <b>one row passing says nothing
 * about the other two.</b> Rows are per gesture for that reason and must not be collapsed.
 *
 * <h2>WHAT IS NOT PORTED FROM THE PREDECESSOR</h2>
 *
 * Its picker had a <i>"Reserved -- holds your other protected item"</i> arm. <b>It had two
 * protected items; we have one</b>, so that branch is unreachable here and writing it would be a
 * guard whose triggering case cannot exist -- a dead arm with a green suite around it.
 *
 * <p>And <b>choosing a slot here does NOT switch the star back on.</b> The predecessor's picker
 * silently re-enabled it. <b>One setting changed by a different setting's button is a surprise</b>,
 * and there is a dedicated toggle one screen up.
 */
public final class NexusSlotPickerMenu extends Menu {

    private final AdapterContext adapters;
    private final ProfileService profiles;

    /** How to rebuild SETTINGS for the Back button -- the breadcrumb, captured by the opener. */
    private final Supplier<Menu> settings;

    public NexusSlotPickerMenu(Player viewer, AdapterContext adapters, ProfileService profiles,
                               Supplier<Menu> settings) {
        super(viewer, NexusSlotPickerLayout.SIZE,
                MenuIcons.line("Nexus Slot", NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        this.profiles = profiles;
        this.settings = settings;
        render();
    }

    /**
     * <b>NONE. That is the primary defence against the clone hazard</b>, not a detail of it -- see
     * the class javadoc. Do not declare one here to "make the picker feel interactive".
     */
    @Override
    protected Set<Integer> inputSlots() {
        return Set.of();
    }

    /**
     * <b>UNCONDITIONALLY FALSE.</b> Not "false unless it is gear": there is no cursor content this
     * screen should accept, and a conditional here would be a door with a lock on it rather than a
     * wall.
     */
    @Override
    protected boolean acceptsInput(ItemStack cursor) {
        return false;
    }

    @Override
    protected void onClick(MenuClick click) {
        if (click.slot() == NexusSlotPickerLayout.CLOSE_SLOT) {
            viewer.closeInventory();
            return;
        }
        if (click.slot() == NexusSlotPickerLayout.BACK_SLOT) {
            // Back to SETTINGS, not to the hub -- one step up, not two. Hop a tick and do not close
            // first: Menu.open's rule, and this screen holds no input slots.
            adapters.scheduler().onEntity(viewer, () -> settings.get().open());
            return;
        }
        OptionalInt chosen = NexusSlotPickerLayout.chooserFor(click.slot());
        if (chosen.isEmpty()) return;   // filler; inert, and deliberately without a branch of its own
        choose(chosen.getAsInt());
    }

    /**
     * Write the new slot, move the star to it, and repaint.
     *
     * <p><b>ORDER MATTERS AND IT IS WRITE-THEN-PLACE.</b> {@code setNexusSlot} replaces the cached
     * profile, so the {@code converge} below -- and the lock, on the player's very next click --
     * read the new value rather than the old. Placing first would move the star to a slot the lock
     * is not yet protecting, which is slice 4a's defect re-created by hand.
     *
     * <p><b>IT DOES NOT TOUCH {@code starEnabled}.</b> A player whose star is off and who picks a
     * slot has expressed a preference about WHERE, not about WHETHER. The predecessor conflated the
     * two and switched the item back on; the toggle is one screen up and is the only thing that
     * decides that.
     */
    private void choose(int inventorySlot) {
        if (!profiles.setNexusSlot(viewer.getUniqueId(), inventorySlot)) {
            viewer.sendMessage(
                    profiles.availability(viewer.getUniqueId()) == ProfileService.Availability.UNREADABLE
                            ? Component.text(ProfileService.UNREADABLE_PROFILE, NamedTextColor.RED)
                            : Component.text(ProfileService.STILL_LOADING, NamedTextColor.GRAY));
            return;
        }

        // CONVERGE ONLY IF THE STAR IS ON. While it is off, converge would mint one -- which is the
        // silent re-enable this slice exists to refuse. The stored slot still moves, so turning the
        // toggle back on later puts the star where they asked for it.
        boolean enabled = profiles.profile(viewer.getUniqueId())
                .map(profile -> profile.starEnabled())
                .orElse(true);
        if (enabled) {
            NexusSlots.converge(viewer, adapters.keys(), inventorySlot);
        }

        render();
        viewer.sendMessage(Component.text(
                "The Nexus now sits in " + NexusSlotPickerLayout.slotName(inventorySlot)
                        + (enabled ? "." : " -- once you switch it back on."),
                NamedTextColor.AQUA));
    }

    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        // Nothing to return: inputSlots() is empty, so the player never had anything in here.
    }

    private void render() {
        for (int slot : NexusSlotPickerLayout.FILLER_SLOTS) {
            getInventory().setItem(slot, MenuIcons.filler());
        }
        getInventory().setItem(NexusSlotPickerLayout.CLOSE_SLOT, MenuIcons.close());
        // "Settings", NOT "the Nexus" -- the label names the DESTINATION, and this button's
        // destination is one step up rather than two. Same slot as the settings screen's own Back,
        // different word, which is the convention working.
        getInventory().setItem(NexusSlotPickerLayout.BACK_SLOT,
                MenuIcons.back(Material.ARROW, "Settings"));

        // THE CURRENT CHOICE IS READ, NEVER REMEMBERED. lockedSlotOf answers NO_LOCKED_SLOT while
        // the profile is unreadable or still loading, and then nothing is lime -- the honest
        // picture: we do not know which slot is theirs, so we do not claim one.
        int current = NexusSlots.chosenSlotOf(viewer, profiles);

        for (int menuSlot : NexusSlotPickerLayout.SLOT_CHOOSERS) {
            int inventorySlot = NexusSlotPickerLayout.chooserFor(menuSlot).orElseThrow();
            getInventory().setItem(menuSlot, cellIcon(inventorySlot, inventorySlot == current));
        }
    }

    /**
     * One cell of the mirrored inventory.
     *
     * <pre>
     *   the current slot   LIME pane      "Current slot (Hotbar 3)"
     *   an empty cell      LIGHT_GRAY     "Empty (Row 2, slot 4)" + "Click to move the Nexus here."
     *   an occupied cell   THE REAL ITEM, cloned
     * </pre>
     *
     * <p><b>The clone is what makes this an inventory rather than a grid</b>, and it is the hazard
     * the class javadoc is about. <b>{@code clone()} rather than the live stack</b>: handing the
     * menu the player's own {@code ItemStack} instance would let a render write reach their
     * inventory.
     *
     * <p><b>THE CURRENT SLOT WINS OVER ITS CONTENTS.</b> The star itself lives there, so rendering
     * the occupant would paint a Nexus star into the picker -- a second star on screen, which is
     * exactly the shape {@code converge}'s surplus-deletion exists to prevent people creating.
     */
    private ItemStack cellIcon(int inventorySlot, boolean current) {
        String name = NexusSlotPickerLayout.slotName(inventorySlot);
        if (current) {
            return MenuIcons.icon(Material.LIME_STAINED_GLASS_PANE,
                    MenuIcons.line("Current slot (" + name + ")", NamedTextColor.GREEN),
                    List.of(MenuIcons.line("The Nexus sits here.", NamedTextColor.DARK_GRAY)));
        }

        ItemStack occupant = viewer.getInventory().getItem(inventorySlot);
        if (MenuSafety.isEmpty(occupant)) {
            return MenuIcons.icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                    MenuIcons.line("Empty (" + name + ")", NamedTextColor.GRAY),
                    List.of(MenuIcons.line("Click to move the Nexus here.",
                            NamedTextColor.DARK_GRAY)));
        }
        // THE REAL ITEM. No name or lore rewrite: the player is looking for a thing they recognise,
        // and relabelling it would defeat the point of showing it at all.
        return occupant.clone();
    }
}
