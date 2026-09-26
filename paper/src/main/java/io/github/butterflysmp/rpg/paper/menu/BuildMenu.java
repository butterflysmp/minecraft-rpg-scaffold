package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.build.LoadoutResolution.Equipped;
import io.github.butterflysmp.rpg.core.build.LoadoutSlot;
import io.github.butterflysmp.rpg.core.build.PoolDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The Build screen (PLAN-build-system.md section 3.3): the player's class, element and loadout, reached
 * from the Nexus at slot 21. <b>It is the only writer of the cell and the loadout</b>: {@code /rpg class},
 * {@code /rpg element} and the dev {@code /rpg build set} are deleted in this slice.
 *
 * <p>Row 1 opens the class and element pickers; row 2 opens the Ultimate / Active 1 / Active 2 pickers,
 * each offering only what the cell's pool lists in that role ({@code BuildRules}). Rows 3 and 4 are the
 * aspect and fragment cells. The fragment row is LIVE since slice 4 (four slots, one of each, ruling
 * 11); the aspect row still renders "Coming in a later update" until slice 5.
 *
 * <h2>EVERY ICON IS RENDERED HERE; NONE IS A REAL ITEM</h2>
 *
 * {@link #inputSlots()} is empty and {@link #acceptsInput} is unconditionally false -- the Nexus slot
 * picker's anti-dupe rule -- so no gesture can move anything in or out. And no icon is ever a copy of
 * a real item: an ability cell is a plain {@link MenuIcons#icon}, never a minted Ability Stone, so even
 * an icon that escaped would be an inert pane with a name.
 */
public final class BuildMenu extends Menu {

    private final AdapterContext adapters;
    private final ProfileService profiles;

    /** How to rebuild the Nexus for the Back button -- the breadcrumb, captured by the opener. */
    private final Supplier<Menu> hub;

    public BuildMenu(Player viewer, AdapterContext adapters, ProfileService profiles, Supplier<Menu> hub) {
        super(viewer, BuildMenuLayout.SIZE, MenuIcons.line("Build", NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        this.profiles = profiles;
        this.hub = hub;
        render();
    }

    /** NONE: every cell is a button. See the class javadoc. */
    @Override
    protected Set<Integer> inputSlots() {
        return Set.of();
    }

    /** UNCONDITIONALLY FALSE: there is no cursor content this screen should accept. */
    @Override
    protected boolean acceptsInput(ItemStack cursor) {
        return false;
    }

    @Override
    protected void onClick(MenuClick click) {
        int slot = click.slot();
        if (slot == BuildMenuLayout.CLOSE_SLOT) {
            viewer.closeInventory();
            return;
        }
        if (slot == BuildMenuLayout.BACK_SLOT) {
            // Hop a tick, no explicit close: this screen holds no input slots (Menu.open's rule).
            adapters.scheduler().onEntity(viewer, () -> hub.get().open());
            return;
        }
        if (slot == BuildMenuLayout.CLASS_SLOT) {
            openPicker(BuildPickerMenu.Kind.CLASS);
            return;
        }
        if (slot == BuildMenuLayout.ELEMENT_SLOT) {
            openPicker(BuildPickerMenu.Kind.ELEMENT);
            return;
        }
        java.util.OptionalInt fragmentSlot = BuildMenuLayout.fragmentIndexAt(slot);
        if (fragmentSlot.isPresent()) {
            if (pool().isEmpty()) {
                viewer.sendMessage(Component.text("Choose a class and an element first.", NamedTextColor.YELLOW));
                return;
            }
            openPicker(BuildPickerMenu.Kind.FRAGMENT, fragmentSlot.getAsInt());
            return;
        }
        Optional<LoadoutSlot> loadoutSlot = BuildMenuLayout.loadoutSlotAt(slot);
        if (loadoutSlot.isEmpty()) return;   // filler, and the not-yet-built aspect cells
        if (pool().isEmpty()) {
            viewer.sendMessage(Component.text("Choose a class and an element first.", NamedTextColor.YELLOW));
            return;
        }
        openPicker(BuildPickerMenu.Kind.of(loadoutSlot.get()));
    }

    private void openPicker(BuildPickerMenu.Kind kind) {
        openPicker(kind, -1);
    }

    /** {@code fragmentSlot} is the 0-based fragment slot for {@code Kind.FRAGMENT}, and -1 otherwise. */
    private void openPicker(BuildPickerMenu.Kind kind, int fragmentSlot) {
        Supplier<Menu> back = () -> new BuildMenu(viewer, adapters, profiles, hub);
        adapters.scheduler().onEntity(viewer, () ->
                new BuildPickerMenu(viewer, adapters, profiles, kind, fragmentSlot, back).open());
    }

    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        // Nothing to return: inputSlots() is empty, so the player never had anything in here.
    }

    private Optional<PlayerProfile> profile() {
        return profiles.profile(viewer.getUniqueId());
    }

    private Optional<PoolDefinition> pool() {
        return profile().flatMap(p -> adapters.stones().pools().find(p.archetypeId(), p.elementId()));
    }

    private void render() {
        for (int slot : BuildMenuLayout.FILLER_SLOTS) getInventory().setItem(slot, MenuIcons.filler());
        getInventory().setItem(BuildMenuLayout.CLOSE_SLOT, MenuIcons.close());
        getInventory().setItem(BuildMenuLayout.BACK_SLOT, MenuIcons.back(Material.ARROW, "the Nexus"));

        Optional<PlayerProfile> profile = profile();
        getInventory().setItem(BuildMenuLayout.CLASS_SLOT, axisIcon(Material.NAME_TAG, "Class",
                profile, PlayerProfile::archetypeId, BuildMenu::classLabel));
        getInventory().setItem(BuildMenuLayout.ELEMENT_SLOT, axisIcon(Material.GLOWSTONE_DUST, "Element",
                profile, PlayerProfile::elementId, this::elementLabel));

        Optional<Equipped> equipped = adapters.stones().equippedFor(viewer.getUniqueId(), profile);
        boolean unusable = adapters.stones().storeUnavailable(viewer.getUniqueId());
        for (LoadoutSlot slot : LoadoutSlot.values()) {
            getInventory().setItem(BuildMenuLayout.slotOf(slot), loadoutIcon(slot, equipped, unusable));
        }

        for (int i = 0; i < BuildMenuLayout.ASPECT_SLOTS.size(); i++) {
            getInventory().setItem(BuildMenuLayout.ASPECT_SLOTS.get(i), comingLater("Aspect " + (i + 1)));
        }
        // THE FRAGMENT ROW, LIVE (slice 4): the current cell's four slots, as equipped.
        List<String> fragments = adapters.stones().equippedFragments(viewer.getUniqueId(), profile);
        boolean pooled = pool().isPresent();
        for (int i = 0; i < BuildMenuLayout.FRAGMENT_SLOTS.size(); i++) {
            getInventory().setItem(BuildMenuLayout.FRAGMENT_SLOTS.get(i),
                    fragmentIcon(i, pooled ? fragments.get(i) : null, pooled, unusable));
        }
    }

    /**
     * A class or element cell. An unreadable or still-loading profile says so rather than showing a
     * choice it does not know. The axis is read from the PROFILE, not mapped through an Optional: a null
     * class survives load (PLAN-accessories 8.4) and must read as "none", not as "profile unavailable".
     */
    private ItemStack axisIcon(Material material, String axis, Optional<PlayerProfile> profile,
                               java.util.function.Function<PlayerProfile, String> read,
                               java.util.function.Function<String, Component> label) {
        if (profile.isEmpty()) {
            return MenuIcons.icon(material, MenuIcons.line(axis, NamedTextColor.GRAY),
                    List.of(MenuIcons.line(profileUnavailableText(), NamedTextColor.RED)));
        }
        String value = read.apply(profile.get());
        boolean chosen = value != null && !PlayerProfile.NONE.equals(value);
        Component name = MenuIcons.line(axis + ": ", NamedTextColor.GRAY)
                .append(chosen ? label.apply(value) : Component.text("none", NamedTextColor.DARK_GRAY));
        return MenuIcons.icon(material, name,
                List.of(MenuIcons.line("Click to choose.", NamedTextColor.DARK_GRAY)));
    }

    private static Component classLabel(String classId) {
        return Component.text(capitalised(classId), NamedTextColor.WHITE);
    }

    private Component elementLabel(String elementId) {
        return adapters.elements().find(elementId)
                .map(def -> MiniMessage.miniMessage().deserialize(def.displayName()))
                .orElse(Component.text(elementId, NamedTextColor.WHITE));
    }

    /** One loadout cell: the equipped ability, "(empty)", or "choose a class and an element first". */
    private ItemStack loadoutIcon(LoadoutSlot slot, Optional<Equipped> equipped, boolean unusable) {
        String input = BuildMenuLayout.inputLabel(slot);
        String role = slot == LoadoutSlot.ULTIMATE ? "Ultimate" : "Active";
        if (equipped.isEmpty()) {
            return MenuIcons.icon(Material.GRAY_STAINED_GLASS_PANE,
                    MenuIcons.line(input + ": " + role, NamedTextColor.DARK_GRAY),
                    List.of(MenuIcons.line("Choose a class and an element first.", NamedTextColor.DARK_GRAY)));
        }
        Optional<String> id = equipped.get().idFor(slot);
        Optional<AbilityDefinition> ability = id.flatMap(i -> adapters.stones().abilities().find(i));
        Component name = MenuIcons.line(input + ": ", NamedTextColor.GRAY).append(ability
                .map(def -> MiniMessage.miniMessage().deserialize(def.displayName()))
                .orElse(Component.text("(empty)", NamedTextColor.DARK_GRAY))
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(MenuIcons.line(role + (equipped.get().fromDefault() ? " -- the default" : ""),
                NamedTextColor.DARK_GRAY));
        ability.ifPresent(def -> lore.addAll(abilityLore(def)));
        lore.add(MenuIcons.blank());
        lore.add(unusable
                ? MenuIcons.line("Build unavailable -- changes cannot be saved.", NamedTextColor.RED)
                : MenuIcons.line("Click to change.", NamedTextColor.DARK_GRAY));
        return MenuIcons.icon(slot == LoadoutSlot.ULTIMATE ? Material.AMETHYST_CLUSTER : Material.PRISMARINE_SHARD,
                name, lore);
    }

    /**
     * An ability's authored description and its cooldown, shared with the picker. The description is
     * plain gray text, as {@code WeaponLore} renders it -- the display NAME is MiniMessage, the lines are not.
     */
    static List<Component> abilityLore(AbilityDefinition def) {
        List<Component> lore = new ArrayList<>();
        for (String line : def.description()) lore.add(MenuIcons.line(line, NamedTextColor.GRAY));
        lore.add(MenuIcons.line("Cooldown: " + formatSeconds(def.cooldownTicks()), NamedTextColor.DARK_GRAY));
        return lore;
    }

    private static String formatSeconds(int ticks) {
        double seconds = ticks / 20.0;
        return (seconds == Math.floor(seconds) ? String.valueOf((int) seconds) : String.valueOf(seconds)) + "s";
    }

    /**
     * One fragment cell: the slotted fragment (its own icon, name and modifiers), an empty slot, or "choose a
     * class and an element first". Rendered here as an icon -- never a minted item.
     */
    private ItemStack fragmentIcon(int index, String id, boolean pooled, boolean unusable) {
        String label = "Fragment " + (index + 1);
        if (!pooled) {
            return MenuIcons.icon(Material.GRAY_STAINED_GLASS_PANE, MenuIcons.line(label, NamedTextColor.DARK_GRAY),
                    List.of(MenuIcons.line("Choose a class and an element first.", NamedTextColor.DARK_GRAY)));
        }
        Component footer = unusable
                ? MenuIcons.line("Build unavailable -- changes cannot be saved.", NamedTextColor.RED)
                : MenuIcons.line("Click to " + (id == null ? "choose." : "change."), NamedTextColor.DARK_GRAY);
        Optional<io.github.butterflysmp.rpg.core.build.FragmentDefinition> fragment =
                id == null ? Optional.empty() : adapters.stones().fragments().find(id);
        if (fragment.isEmpty()) {
            return MenuIcons.icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                    MenuIcons.line(label + ": (empty)", NamedTextColor.GRAY), List.of(footer));
        }
        List<Component> lore = new ArrayList<>(fragmentLore(fragment.get()));
        lore.add(MenuIcons.blank());
        lore.add(footer);
        return MenuIcons.icon(fragmentMaterial(fragment.get()),
                MenuIcons.line(label + ": ", NamedTextColor.GRAY).append(
                        MiniMessage.miniMessage().deserialize(fragment.get().displayName())
                                .decoration(TextDecoration.ITALIC, false)), lore);
    }

    /** A fragment's modifiers and description, shared with the picker. */
    static List<Component> fragmentLore(io.github.butterflysmp.rpg.core.build.FragmentDefinition fragment) {
        List<Component> lore = new ArrayList<>();
        for (var m : fragment.modifiers().entrySet()) {
            lore.add(MenuIcons.line(io.github.butterflysmp.rpg.core.accessory.AccessoryLoreLines.modifier(
                    m.getKey(), m.getValue(), null), NamedTextColor.BLUE));
        }
        for (String line : fragment.description()) lore.add(MenuIcons.line(line, NamedTextColor.GRAY));
        return lore;
    }

    /** The fragment's authored icon; the loader refused any icon that names no item, so this cannot miss. */
    static Material fragmentMaterial(io.github.butterflysmp.rpg.core.build.FragmentDefinition fragment) {
        Material material = Material.matchMaterial(fragment.icon().toUpperCase(java.util.Locale.ROOT));
        return material == null ? Material.BARRIER : material;
    }

    /** An aspect cell: not built until slice 5, and says so. */
    private static ItemStack comingLater(String name) {
        return MenuIcons.icon(Material.BARRIER, MenuIcons.line(name, NamedTextColor.DARK_GRAY),
                List.of(MenuIcons.line("Coming in a later update", NamedTextColor.DARK_GRAY)));
    }

    private String profileUnavailableText() {
        return profiles.availability(viewer.getUniqueId()) == ProfileService.Availability.UNREADABLE
                ? ProfileService.UNREADABLE_PROFILE : ProfileService.STILL_LOADING;
    }

    static String capitalised(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
