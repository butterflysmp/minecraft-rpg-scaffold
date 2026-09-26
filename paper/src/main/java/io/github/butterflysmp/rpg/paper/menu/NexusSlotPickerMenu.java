package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.nexus.NexusSlots;
import io.github.butterflysmp.rpg.paper.nexus.NexusLock;
import io.github.butterflysmp.rpg.core.build.LockedSlots;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import java.util.Optional;
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

    /**
     * Which locked item this picker moves (PLAN-build-system.md section 1.3). ONE picker for both,
     * generalised rather than copied: the star may go in any of the 36 main slots, the Ability Stone on
     * the HOTBAR only (ruling 2). Each target's other item is shown as RESERVED and refused -- the branch
     * this picker's predecessor had and this one dropped because "we have one" protected item. Now there
     * are two, and two locked items in one slot would fight (section 1.3).
     */
    public enum Target { STAR, STONE }

    private final AdapterContext adapters;
    private final ProfileService profiles;
    private final Target target;

    /** How to rebuild SETTINGS for the Back button -- the breadcrumb, captured by the opener. */
    private final Supplier<Menu> settings;

    public NexusSlotPickerMenu(Player viewer, AdapterContext adapters, ProfileService profiles,
                               Target target, Supplier<Menu> settings) {
        super(viewer, NexusSlotPickerLayout.SIZE,
                MenuIcons.line(target == Target.STAR ? "Nexus Slot" : "Ability Stone Slot",
                        NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        this.profiles = profiles;
        this.target = target;
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
        // The stone's storage cells are painted as filler and are just as inert: choose() asks
        // LockedSlots, the one home of the range, and an out-of-range pick does nothing.
        choose(chosen.getAsInt());
    }

    /**
     * Write the new slot, move the item to it, and repaint.
     *
     * <p><b>ORDER MATTERS AND IT IS WRITE-THEN-PLACE.</b> The setter replaces the cached profile, so
     * the {@code converge} below -- and the lock, on the player's very next click -- read the new
     * value rather than the old. Placing first would move the item to a slot the lock is not yet
     * protecting, which is slice 4a's defect re-created by hand.
     *
     * <p><b>IT DOES NOT TOUCH THE TOGGLE.</b> A player whose item is off and who picks a slot has
     * expressed a preference about WHERE, not about WHETHER. The toggle is one screen up and is the
     * only thing that decides that.
     *
     * <p><b>THE RULE IS {@code LockedSlots}, IN CORE.</b> Out of range (a stone off the hotbar) is
     * inert; the other item's slot is RESERVED and says so. That is what keeps the two items from ever
     * sharing a slot, and {@code LockedSlotsTest} proves it over sequences of picks.
     */
    private void choose(int inventorySlot) {
        Optional<PlayerProfile> profile = profiles.profile(viewer.getUniqueId());
        if (profile.isEmpty()) {
            unavailable();
            return;
        }
        int starSlot = NexusSlots.validSlotOr(profile.get().nexusSlot(), NexusLock.DEFAULT_LOCKED_SLOT);
        int stoneSlot = NexusSlots.stoneSlotOf(profile.get());
        LockedSlots.Refusal refusal = target == Target.STAR
                ? LockedSlots.refuseStarPick(inventorySlot, stoneSlot)
                : LockedSlots.refuseStonePick(inventorySlot, starSlot);
        if (refusal == LockedSlots.Refusal.OUT_OF_RANGE) return;   // a filler cell; inert
        if (refusal == LockedSlots.Refusal.RESERVED) {
            viewer.sendMessage(Component.text(NexusSlotPickerLayout.slotName(inventorySlot)
                    + " holds your " + otherNoun() + ". Choose another cell.", NamedTextColor.RED));
            return;
        }

        boolean written = target == Target.STAR
                ? profiles.setNexusSlot(viewer.getUniqueId(), inventorySlot)
                : profiles.setStoneSlot(viewer.getUniqueId(), inventorySlot);
        if (!written) {
            unavailable();
            return;
        }

        // CONVERGE ONLY IF THE ITEM IS ON. While it is off, converge would mint one -- which is the
        // silent re-enable the toggle exists to refuse. The stored slot still moves, so turning the
        // toggle back on later puts the item where they asked for it.
        boolean enabled = target == Target.STAR
                ? profiles.starEnabled(viewer.getUniqueId())
                : profiles.stoneEnabled(viewer.getUniqueId());
        if (enabled) {
            if (target == Target.STAR) {
                NexusSlots.converge(viewer, adapters.keys(), inventorySlot);
            } else {
                NexusSlots.converge(viewer, adapters.stones().lockedItem(viewer.getUniqueId(), profiles.profile(viewer.getUniqueId())),
                        inventorySlot);
            }
        }

        render();
        viewer.sendMessage(Component.text(
                capitalised(noun()) + " now sits in " + NexusSlotPickerLayout.slotName(inventorySlot)
                        + (enabled ? "." : " -- once you switch it back on."),
                NamedTextColor.AQUA));
    }

    private void unavailable() {
        viewer.sendMessage(
                profiles.availability(viewer.getUniqueId()) == ProfileService.Availability.UNREADABLE
                        ? Component.text(ProfileService.UNREADABLE_PROFILE, NamedTextColor.RED)
                        : Component.text(ProfileService.STILL_LOADING, NamedTextColor.GRAY));
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

        // THE CURRENT CHOICE IS READ, NEVER REMEMBERED. NO_LOCKED_SLOT while the profile is unreadable
        // or still loading, and then nothing is lime -- the honest picture: we do not know which slot
        // is theirs, so we do not claim one. Likewise the other item's cell, for RESERVED.
        int current = target == Target.STAR
                ? NexusSlots.chosenSlotOf(viewer, profiles)
                : NexusSlots.stoneChosenSlotOf(viewer, profiles);
        int reserved = target == Target.STAR
                ? NexusSlots.stoneChosenSlotOf(viewer, profiles)
                : NexusSlots.chosenSlotOf(viewer, profiles);

        for (int menuSlot : NexusSlotPickerLayout.SLOT_CHOOSERS) {
            int inventorySlot = NexusSlotPickerLayout.chooserFor(menuSlot).orElseThrow();
            // The stone's picker paints the storage rows as filler: hotbar only (ruling 2).
            if (target == Target.STONE && inventorySlot > LockedSlots.STONE_MAX_SLOT) {
                getInventory().setItem(menuSlot, MenuIcons.filler());
                continue;
            }
            getInventory().setItem(menuSlot,
                    cellIcon(inventorySlot, inventorySlot == current, inventorySlot == reserved));
        }
    }

    /**
     * One cell of the mirrored inventory.
     *
     * <pre>
     *   the current slot   LIME pane      "Current slot (Hotbar 3)"
     *   the other's slot   BARRIER        "Reserved (Hotbar 9)" + "Holds your Nexus."
     *   an empty cell      LIGHT_GRAY     "Empty (Row 2, slot 4)" + "Click to move the Nexus here."
     *   an occupied cell   THE REAL ITEM, cloned
     * </pre>
     *
     * <p><b>The clone is what makes this an inventory rather than a grid</b>, and it is the hazard
     * the class javadoc is about. <b>{@code clone()} rather than the live stack</b>: handing the
     * menu the player's own {@code ItemStack} instance would let a render write reach their
     * inventory.
     *
     * <p><b>THE CURRENT SLOT WINS OVER ITS CONTENTS</b>, and so does the RESERVED one. Each holds a
     * locked item, and rendering the occupant would paint a star or a stone into the picker -- a
     * second one on screen, which is exactly the shape {@code converge}'s surplus-deletion exists to
     * prevent people creating.
     */
    private ItemStack cellIcon(int inventorySlot, boolean current, boolean reserved) {
        String name = NexusSlotPickerLayout.slotName(inventorySlot);
        if (current) {
            return MenuIcons.icon(Material.LIME_STAINED_GLASS_PANE,
                    MenuIcons.line("Current slot (" + name + ")", NamedTextColor.GREEN),
                    List.of(MenuIcons.line(capitalised(noun()) + " sits here.", NamedTextColor.DARK_GRAY)));
        }
        if (reserved) {
            return MenuIcons.icon(Material.BARRIER,
                    MenuIcons.line("Reserved (" + name + ")", NamedTextColor.RED),
                    List.of(MenuIcons.line("Holds your " + otherNoun() + ".", NamedTextColor.DARK_GRAY)));
        }

        ItemStack occupant = viewer.getInventory().getItem(inventorySlot);
        if (MenuSafety.isEmpty(occupant)) {
            return MenuIcons.icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                    MenuIcons.line("Empty (" + name + ")", NamedTextColor.GRAY),
                    List.of(MenuIcons.line("Click to move " + noun() + " here.",
                            NamedTextColor.DARK_GRAY)));
        }
        // THE REAL ITEM. No name or lore rewrite: the player is looking for a thing they recognise,
        // and relabelling it would defeat the point of showing it at all.
        return occupant.clone();
    }

    private String noun() {
        return target == Target.STAR ? "the Nexus" : "the Ability Stone";
    }

    private String otherNoun() {
        return target == Target.STAR ? "Ability Stone" : "Nexus";
    }

    private static String capitalised(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
