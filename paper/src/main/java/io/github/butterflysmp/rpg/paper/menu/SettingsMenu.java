package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.nexus.NexusSlots;
import io.github.butterflysmp.rpg.paper.nexus.LockedItem;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The Nexus settings screen: <b>a hub of settings, not a picker.</b>
 *
 * <p>Two of them today -- which inventory slot the star lives in, and whether it exists at all:
 *
 * <pre>
 *   Nexus  ->  Settings  ->  Nexus Slot
 *              Settings  ->  (the toggle, in place)
 * </pre>
 *
 * <p>It WAS the picker until slice 10. The thirty-six choosers moved to
 * {@link NexusSlotPickerMenu}; {@code SettingsMenuLayout}'s javadoc records why they were moved
 * rather than left beside the buttons.
 *
 * <h2>THE FIRST MENU IN THIS PLUGIN THAT WRITES TO A PROFILE, AND THAT IS STILL TRUE</h2>
 *
 * Every other profile write is a command. A menu is reachable in states a command is not: <b>a
 * player rejoining with last session's star already in their inventory can open the hub, and this
 * screen, before their profile has finished loading.</b> The star's right-click handler keys on the
 * item's PDC tag and asks nothing about the profile.
 *
 * <p>So this screen answers by ASKING -- {@link ProfileService#availability} -- and says which of
 * the two refusals applies. <b>"Try again in a moment" is correct for a load in flight and a lie
 * for one that already failed.</b>
 *
 * <h2>*** DISABLING REMOVES THE STAR AND TURNS THE LOCK OFF. BOTH HALVES. ***</h2>
 *
 * {@code NexusLock} refuses the locked slot <b>whether or not it holds a star</b> -- its own
 * javadoc says so. So removing the item while leaving the slot locked hands the player a
 * <b>permanently unusable empty cell</b>, and they will report it as an inventory bug, correctly.
 * See {@link #setStar}.
 */
public final class SettingsMenu extends Menu {

    private final AdapterContext adapters;
    private final ProfileService profiles;

    /**
     * How to rebuild the hub for the Back button -- the breadcrumb, captured by whoever opened us.
     *
     * <p><b>A supplier rather than the services a hub needs.</b> It is a breadcrumb and must not
     * become identity; {@code CraftingMenu.Origin} carries that argument and it applies unchanged.
     */
    private final Supplier<Menu> hub;

    public SettingsMenu(Player viewer, AdapterContext adapters, ProfileService profiles,
                        Supplier<Menu> hub) {
        super(viewer, SettingsMenuLayout.SIZE,
                MenuIcons.line("Nexus Settings", NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        this.profiles = profiles;
        this.hub = hub;
        render();
    }

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
        if (click.slot() == SettingsMenuLayout.CLOSE_SLOT) {
            viewer.closeInventory();
            return;
        }
        if (click.slot() == SettingsMenuLayout.BACK_SLOT) {
            // HOP A TICK, DO NOT CLOSE FIRST. Menu.open's measured rule; this screen holds no
            // input slots, so openInventory's implicit close is sufficient.
            adapters.scheduler().onEntity(viewer, () -> hub.get().open());
            return;
        }
        if (click.slot() == SettingsMenuLayout.SLOT_SETTING_SLOT) {
            // THE BREADCRUMB POINTS BACK HERE, not to the hub -- the picker's Back is one step up.
            // It captures THIS screen's own hub supplier, so the chain stays three deep and the
            // picker never has to know what a Nexus hub needs.
            adapters.scheduler().onEntity(viewer, () -> new NexusSlotPickerMenu(
                    viewer, adapters, profiles, NexusSlotPickerMenu.Target.STAR,
                    () -> new SettingsMenu(viewer, adapters, profiles, hub)).open());
            return;
        }
        if (click.slot() == SettingsMenuLayout.TOGGLE_SETTING_SLOT) {
            setStar(!starEnabled());
            return;
        }
        if (click.slot() == SettingsMenuLayout.STONE_SLOT_SETTING_SLOT) {
            adapters.scheduler().onEntity(viewer, () -> new NexusSlotPickerMenu(
                    viewer, adapters, profiles, NexusSlotPickerMenu.Target.STONE,
                    () -> new SettingsMenu(viewer, adapters, profiles, hub)).open());
            return;
        }
        if (click.slot() == SettingsMenuLayout.STONE_TOGGLE_SETTING_SLOT) {
            setStone(!profiles.stoneEnabled(viewer.getUniqueId()));
        }
        // Filler is inert and deliberately has no branch of its own.
    }

    /**
     * The Ability Stone's toggle: {@link #setStar}'s shape and rules exactly (ruling 2: "the same way the
     * Nexus star is"). Enabling refuses an occupied slot and names it; disabling removes every stone and
     * frees the slot -- the profile write is what takes the lock off, and both happen together.
     */
    private void setStone(boolean enabled) {
        int slot = NexusSlots.stoneChosenSlotOf(viewer, profiles);
        if (slot == io.github.butterflysmp.rpg.paper.nexus.NexusLock.NO_LOCKED_SLOT) {
            viewer.sendMessage(
                    profiles.availability(viewer.getUniqueId()) == ProfileService.Availability.UNREADABLE
                            ? Component.text(ProfileService.UNREADABLE_PROFILE, NamedTextColor.RED)
                            : Component.text(ProfileService.STILL_LOADING, NamedTextColor.GRAY));
            return;
        }
        if (enabled) {
            ItemStack occupant = viewer.getInventory().getItem(slot);
            if (!MenuSafety.isEmpty(occupant)) {
                viewer.sendMessage(Component.text(
                        NexusSlotPickerLayout.slotName(slot)
                                + " is occupied. Clear it or use the Ability Stone slot picker first.",
                        NamedTextColor.RED));
                return;
            }
        }
        if (!profiles.setStoneEnabled(viewer.getUniqueId(), enabled)) {
            viewer.sendMessage(Component.text(ProfileService.STILL_LOADING, NamedTextColor.GRAY));
            return;
        }
        LockedItem stone = adapters.stones().lockedItem(viewer.getUniqueId(), profiles.profile(viewer.getUniqueId()));
        if (enabled) {
            NexusSlots.converge(viewer, stone, slot);
            viewer.sendMessage(Component.text(
                    "The Ability Stone is back, in " + NexusSlotPickerLayout.slotName(slot) + ".",
                    NamedTextColor.AQUA));
        } else {
            NexusSlots.removeAll(viewer, stone);
            viewer.sendMessage(Component.text(
                    "The Ability Stone is off. " + NexusSlotPickerLayout.slotName(slot) + " is yours to use.",
                    NamedTextColor.AQUA));
        }
        render();
    }

    /** Is the star on? An unavailable profile reads as ON, which is the shipped default. */
    private boolean starEnabled() {
        return profiles.profile(viewer.getUniqueId())
                .map(PlayerProfile::starEnabled)
                .orElse(true);
    }

    /**
     * Switch the star on or off.
     *
     * <h2>*** OFF IS TWO WRITES AND ON IS A REFUSAL, AND NEITHER IS SYMMETRIC WITH THE OTHER ***</h2>
     *
     * <b>DISABLING removes the star AND clears the lock.</b> Both halves, because
     * {@code NexusLock} refuses the locked slot <b>whether or not it holds a star</b>: leaving the
     * slot locked over an empty cell gives the player a cell they can neither fill nor use, with
     * nothing on screen to explain it. <b>The lock going off is not tidying -- it is the other half
     * of the same setting.</b>
     *
     * <p><b>ENABLING refuses if the target slot is occupied</b>, naming it. That arm is LIVE for
     * us, unlike the predecessor's "reserved" branch: the slot is a stored preference that may
     * have been filled by anything since. <b>The alternative is displacing an item the player did
     * not ask us to move</b>, at a moment they were pressing a button about something else.
     *
     * <p>{@code converge} would happily displace the occupant -- that is right on JOIN, where the
     * player is not watching and the star must exist. It is wrong here.
     */
    private void setStar(boolean enabled) {
        int slot = NexusSlots.chosenSlotOf(viewer, profiles);
        if (slot == io.github.butterflysmp.rpg.paper.nexus.NexusLock.NO_LOCKED_SLOT) {
            viewer.sendMessage(
                    profiles.availability(viewer.getUniqueId()) == ProfileService.Availability.UNREADABLE
                            ? Component.text(ProfileService.UNREADABLE_PROFILE, NamedTextColor.RED)
                            : Component.text(ProfileService.STILL_LOADING, NamedTextColor.GRAY));
            return;
        }

        if (enabled) {
            // READ THE OCCUPANT BEFORE WRITING ANYTHING. While the star is off the lock is off too,
            // so this cell is an ordinary one and may hold anything.
            ItemStack occupant = viewer.getInventory().getItem(slot);
            if (!MenuSafety.isEmpty(occupant)) {
                viewer.sendMessage(Component.text(
                        NexusSlotPickerLayout.slotName(slot)
                                + " is occupied. Clear it or use the slot picker first.",
                        NamedTextColor.RED));
                return;
            }
        }

        if (!profiles.setStarEnabled(viewer.getUniqueId(), enabled)) {
            viewer.sendMessage(Component.text(ProfileService.STILL_LOADING, NamedTextColor.GRAY));
            return;
        }

        if (enabled) {
            // The profile is written first, so converge and the lock read the new value -- the same
            // write-then-place ordering the picker uses.
            NexusSlots.converge(viewer, adapters.keys(), slot);
            viewer.sendMessage(Component.text(
                    "The Nexus is back, in " + NexusSlotPickerLayout.slotName(slot) + ".",
                    NamedTextColor.AQUA));
        } else {
            NexusSlots.removeStars(viewer, adapters.keys());
            viewer.sendMessage(Component.text(
                    "The Nexus is off. " + NexusSlotPickerLayout.slotName(slot)
                            + " is yours to use; open this menu again with /menu.",
                    NamedTextColor.AQUA));
        }
        render();
    }

    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        // Nothing to return: inputSlots() is empty, so the player never had anything in here.
    }

    private void render() {
        for (int slot : SettingsMenuLayout.FILLER_SLOTS) {
            getInventory().setItem(slot, MenuIcons.filler());
        }
        getInventory().setItem(SettingsMenuLayout.CLOSE_SLOT, MenuIcons.close());
        getInventory().setItem(SettingsMenuLayout.BACK_SLOT,
                MenuIcons.back(Material.ARROW, "the Nexus"));

        int current = NexusSlots.chosenSlotOf(viewer, profiles);
        String where = current == io.github.butterflysmp.rpg.paper.nexus.NexusLock.NO_LOCKED_SLOT
                ? "not known yet"
                : NexusSlotPickerLayout.slotName(current);

        getInventory().setItem(SettingsMenuLayout.SLOT_SETTING_SLOT, MenuIcons.icon(
                Material.ITEM_FRAME,
                MenuIcons.line("Nexus Slot", NamedTextColor.GRAY),
                List.of(MenuIcons.line("Currently: " + where, NamedTextColor.DARK_GRAY),
                        MenuIcons.line("Click to choose a different cell.",
                                NamedTextColor.DARK_GRAY))));

        boolean enabled = starEnabled();
        // LIME for on, GRAY for off -- the grindstone's palette rule: GRAY is a state that resolves
        // only if you ACT, which is exactly what a switched-off star is. Not RED: nothing is wrong.
        getInventory().setItem(SettingsMenuLayout.TOGGLE_SETTING_SLOT, MenuIcons.icon(
                enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                MenuIcons.line(enabled ? "Nexus Star: ON" : "Nexus Star: OFF",
                        enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                enabled
                        ? List.of(MenuIcons.line("Click to remove it and free the slot.",
                                NamedTextColor.DARK_GRAY))
                        // BOTH FACTS, the same requirement the hub's locked stations carry: what
                        // clicking does, AND that the menu is still reachable without the item.
                        : List.of(MenuIcons.line("Click to put it back in " + where + ".",
                                        NamedTextColor.DARK_GRAY),
                                MenuIcons.line("/menu opens this hub either way.",
                                        NamedTextColor.DARK_GRAY))));

        // THE ABILITY STONE'S ROW, directly below the star's (PLAN-build-system.md 2.5). Same two cells,
        // same wording shape, so the pair reads as one table.
        int stoneSlot = NexusSlots.stoneChosenSlotOf(viewer, profiles);
        String stoneWhere = stoneSlot == io.github.butterflysmp.rpg.paper.nexus.NexusLock.NO_LOCKED_SLOT
                ? "not known yet"
                : NexusSlotPickerLayout.slotName(stoneSlot);
        getInventory().setItem(SettingsMenuLayout.STONE_SLOT_SETTING_SLOT, MenuIcons.icon(
                Material.ITEM_FRAME,
                MenuIcons.line("Ability Stone Slot", NamedTextColor.GRAY),
                List.of(MenuIcons.line("Currently: " + stoneWhere, NamedTextColor.DARK_GRAY),
                        MenuIcons.line("Click to choose a different hotbar cell.",
                                NamedTextColor.DARK_GRAY))));
        boolean stoneEnabled = profiles.stoneEnabled(viewer.getUniqueId());
        getInventory().setItem(SettingsMenuLayout.STONE_TOGGLE_SETTING_SLOT, MenuIcons.icon(
                stoneEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
                MenuIcons.line(stoneEnabled ? "Ability Stone: ON" : "Ability Stone: OFF",
                        stoneEnabled ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                stoneEnabled
                        ? List.of(MenuIcons.line("Click to remove it and free the slot.",
                                NamedTextColor.DARK_GRAY))
                        : List.of(MenuIcons.line("Click to put it back in " + stoneWhere + ".",
                                NamedTextColor.DARK_GRAY))));
    }
}
