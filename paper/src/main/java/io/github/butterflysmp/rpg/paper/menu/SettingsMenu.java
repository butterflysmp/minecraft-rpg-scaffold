package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
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

/**
 * The Nexus settings screen: choose which hotbar slot the Nexus star lives in.
 *
 * <p>Reached from the redstone torch on the hub, and the torch stopped being a
 * {@link MenuIcons#placeholder} the moment this existed -- the third instance of that distinction,
 * and {@code NexusMenu}'s class javadoc is where the pair is argued.
 *
 * <h2>THE FIRST MENU IN THIS PLUGIN THAT WRITES TO A PROFILE</h2>
 *
 * Every other profile write is a command. That matters because a menu is reachable in states a
 * command is not: <b>a player rejoining with last session's star already in their inventory can
 * open the hub, and this screen, before their profile has finished loading.</b> The star's
 * right-click handler keys on the item's PDC tag and asks nothing about the profile.
 *
 * <p>So this screen must answer the question {@code RpgListeners.onJoin} answers by waiting, and it
 * cannot wait -- the player is looking at it. It ASKS INSTEAD, through
 * {@link ProfileService#availability}, and says which of the two refusals applies. <b>"Try again in
 * a moment" is correct for a load in flight and a lie for one that already failed</b>; the second
 * player would retry forever.
 *
 * <h2>WHAT IT DOES NOT DO: WAIT, GUESS, OR HIDE</h2>
 *
 * The choosers are always painted, even when unwritable. A screen that changed shape depending on a
 * disk read would be a second thing to explain, and a torch that opened nothing would be the defect
 * {@code BrokenNotice} exists for -- <i>doing nothing without an explanation reads as a defect.</i>
 * The click is where the refusal is spoken, because that is when the player has asked a question.
 */
public final class SettingsMenu extends Menu {

    private final AdapterContext adapters;
    private final ProfileService profiles;
    private final WeaponRegistry weapons;
    private final ResourcePool resources;

    /**
     * @param weapons   carried only to rebuild the hub on the way back
     * @param resources the same
     *
     * <p>Both exist because {@code NexusMenu} needs them for its stats head, and the back button
     * constructs a fresh hub. Threading them through is the cost of the back button naming a
     * destination it can actually build.
     */
    public SettingsMenu(Player viewer, AdapterContext adapters, ProfileService profiles,
                        WeaponRegistry weapons, ResourcePool resources) {
        super(viewer, SettingsMenuLayout.SIZE,
                MenuIcons.line("Nexus Settings", NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        this.profiles = profiles;
        this.weapons = weapons;
        this.resources = resources;
        render();
    }

    /**
     * Nothing. Every slot is chrome or a button.
     *
     * <p>Same property the hub and the recipe browser rely on: with no input slots the router
     * performs no moves, the drag handler permits nothing, and there is no state a close could
     * strand -- which is also what makes {@link #onClose} a no-op and lets the back button navigate
     * without an explicit close. {@code Menu.open}'s javadoc is where that rule lives.
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
        if (click.slot() == SettingsMenuLayout.CLOSE_SLOT) {
            viewer.closeInventory();
            return;
        }

        if (click.slot() == SettingsMenuLayout.BACK_SLOT) {
            // HOP A TICK, DO NOT CLOSE FIRST. Menu.open's javadoc carries the measured rule: both
            // Scheduler entity methods land on the NEXT tick -- the pinned paper-api's own javadoc
            // for EntityScheduler.run says so -- and the explicit close exists only to run
            // returnEverything for a menu holding the player's items. This one holds none, so
            // openInventory's implicit close is sufficient. Same shape as the recipe browser's
            // back button, which is now documented rather than merely tolerated.
            adapters.scheduler().onEntity(viewer,
                    () -> new NexusMenu(viewer, adapters, profiles, weapons, resources).open());
            return;
        }

        OptionalInt chosen = SettingsMenuLayout.chooserFor(click.slot());
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
     */
    private void choose(int hotbarSlot) {
        if (!profiles.setNexusSlot(viewer.getUniqueId(), hotbarSlot)) {
            // REFUSED, AND THE REASON IS SAID. Not one message for both arms: see the class javadoc.
            viewer.sendMessage(
                    profiles.availability(viewer.getUniqueId()) == ProfileService.Availability.UNREADABLE
                            ? Component.text(ProfileService.UNREADABLE_PROFILE, NamedTextColor.RED)
                            : Component.text(ProfileService.STILL_LOADING, NamedTextColor.GRAY));
            return;
        }

        // The star follows immediately rather than on next join. converge is the same call the join
        // and respawn paths make, so there is one placement rule and not a second one here.
        NexusSlots.converge(viewer, adapters.keys(), hotbarSlot);
        render();
        viewer.sendMessage(Component.text("The Nexus now sits in slot " + (hotbarSlot + 1) + ".",
                NamedTextColor.AQUA));
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
                MenuIcons.back(Material.NETHER_STAR, "the Nexus"));

        // THE CURRENT CHOICE IS READ, NEVER REMEMBERED. lockedSlotOf answers NO_LOCKED_SLOT while
        // the profile is unreadable or still loading, and then nothing is highlighted -- which is
        // the honest picture: we do not know which slot is theirs, so we do not claim one.
        int current = NexusSlots.lockedSlotOf(viewer, profiles);

        // THREE GREYS ARE ALREADY SPOKEN FOR AND NONE OF THEM MEANS THIS. MenuIcons.FILLER is
        // BLACK, MenuIcons.EMPTY_SUGGESTION is LIGHT_GRAY, and plain GRAY is CraftStatus.EMPTY --
        // whose javadoc says collapsing it with the others is "a REGRESSION, not a simplification".
        // WHITE is unspoken, so an unchosen slot cannot be read as any of them.
        for (int i = 0; i < SettingsMenuLayout.HOTBAR_SIZE; i++) {
            boolean selected = i == current;
            getInventory().setItem(SettingsMenuLayout.SLOT_CHOOSERS.get(i), MenuIcons.icon(
                    selected ? Material.LIME_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE,
                    MenuIcons.line("Slot " + (i + 1),
                            selected ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                    selected
                            ? List.of(MenuIcons.line("The Nexus sits here.", NamedTextColor.DARK_GRAY))
                            : List.of()));
        }
    }
}
