package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.accessory.AccessoryLoreLines;
import io.github.butterflysmp.rpg.core.accessory.AccessorySlots;
import io.github.butterflysmp.rpg.core.equipment.ArmorPlacement;
import io.github.butterflysmp.rpg.core.equipment.ArmorPlacement.Gesture;
import io.github.butterflysmp.rpg.core.equipment.ArmorPlacement.Input;
import io.github.butterflysmp.rpg.core.equipment.ArmorPlacement.Outcome;
import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.paper.accessory.Accessories;
import io.github.butterflysmp.rpg.paper.accessory.AccessoryService;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.nexus.NexusItems;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.vault.VaultCodec;
import io.github.butterflysmp.rpg.paper.weapon.AccessoryItems;
import io.github.butterflysmp.rpg.storage.PlayerAccessories;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The Equipment screen (accessories Slice B): the player's four armour slots and four accessory slots.
 *
 * <h2>LIVE STATE, NEVER THE RENDER</h2>
 *
 * Every slot here is a VIEW. The armour column shows {@code player.getEquipment()}; the accessory
 * column shows the accessory store. The items painted into this inventory are display copies that no
 * gesture can take (the framework cancels every click, and none of these slots is an input slot).
 * So <b>every decision reads the live equipment and the live store at click time, never the painted
 * item</b>, and the screen re-renders one tick after any click. A render that went stale -- armour
 * changed by a command while the screen was open -- therefore cannot be acted on (gate row B19).
 *
 * <h2>The armour column does vanilla's job</h2>
 *
 * The rules are {@link ArmorPlacement}'s, which cites the vanilla code each reproduces. Writes go
 * through {@code EntityEquipment.setItem(slot, item, false)}: that call is non-silent and plays the
 * item's own equip sound, so this class plays none (F1 -- a second sound would be a double).
 *
 * <h2>The accessory column goes through Slice A</h2>
 *
 * The same decision ({@link AccessorySlots#canEquip}) and the same store and ordering the dev command
 * used (PLAN-accessories.md §3.9): an equip takes the item off the cursor first and gives it back if
 * the write fails; an unequip writes first and hands the item over only when the write succeeds. A
 * slot holding an UNREADABLE entry refuses every gesture, so its kept bytes are never overwritten; a
 * store still loading, or unavailable, refuses every accessory gesture while the armour column works.
 */
public final class EquipmentMenu extends Menu {

    private final AdapterContext adapters;
    private final ProfileService profiles;
    private final Supplier<Menu> hub;

    public EquipmentMenu(Player viewer, AdapterContext adapters, ProfileService profiles,
                         Supplier<Menu> hub) {
        super(viewer, EquipmentMenuLayout.SIZE, MenuIcons.line("Equipment", NamedTextColor.DARK_GRAY));
        this.adapters = adapters;
        this.profiles = profiles;
        this.hub = hub;
        render();
    }

    // --- the framework's questions ----------------------------------------------------------------

    /**
     * NONE, and load-bearing: an input slot is one the router performs the number key, F, drag and
     * double-click on. None of those may touch a view of the player's equipment, so none is one.
     */
    @Override
    protected Set<Integer> inputSlots() {
        return Set.of();
    }

    /**
     * NOTHING is handed back on close except the cursor (which {@code returnEverything} always
     * returns). Explicit rather than inherited (F5): the default is {@code inputSlots()}, and a future
     * input slot must not start returning display copies of live armour.
     */
    @Override
    protected Set<Integer> returnedSlots() {
        return Set.of();
    }

    @Override
    protected boolean acceptsInput(ItemStack cursor) {
        return false;
    }

    /** A shift-click ON an armour or accessory slot reaches {@link #onClick}; it moves nothing itself. */
    @Override
    protected boolean shiftClickDispatches(int slot) {
        return EquipmentMenuLayout.ARMOR_SLOTS.contains(slot)
                || EquipmentMenuLayout.ACCESSORY_SLOTS.contains(slot);
    }

    // --- clicks -----------------------------------------------------------------------------------

    @Override
    protected void onClick(MenuClick click) {
        int slot = click.slot();
        if (slot == EquipmentMenuLayout.CLOSE_SLOT) {
            viewer.closeInventory();
            return;
        }
        if (slot == EquipmentMenuLayout.BACK_SLOT) {
            adapters.scheduler().onEntity(viewer, () -> hub.get().open());
            return;
        }
        ClickType type = click.click();
        boolean shift = type == ClickType.SHIFT_LEFT || type == ClickType.SHIFT_RIGHT;
        boolean plain = type == ClickType.LEFT || type == ClickType.RIGHT;
        if (!shift && !plain) return;   // a drop key, a middle click: nothing here moves for those

        Optional<ArmorSlot> armor = EquipmentMenuLayout.armorSlotAt(slot);
        OptionalInt accessory = EquipmentMenuLayout.accessoryIndexAt(slot);
        if (armor.isPresent()) {
            armorClick(armor.get(), shift);
        } else if (accessory.isPresent()) {
            accessoryClick(accessory.getAsInt(), shift);
        }
        scheduleRender();
    }

    private void armorClick(ArmorSlot armor, boolean shift) {
        EntityEquipment equipment = viewer.getEquipment();
        EquipmentSlot bukkit = bukkitSlot(armor);
        ItemStack cursor = viewer.getItemOnCursor();
        ItemStack resting = equipment.getItem(bukkit);
        boolean cursorEmpty = MenuSafety.isEmpty(cursor);
        boolean occupied = !MenuSafety.isEmpty(resting);

        Gesture gesture;
        if (shift) gesture = Gesture.SHIFT_OUT;
        else if (cursorEmpty) gesture = occupied ? Gesture.TAKE : null;
        else gesture = occupied ? Gesture.SWAP : Gesture.PLACE;
        if (gesture == null) return;

        Outcome outcome = ArmorPlacement.decide(new Input(armor, gesture,
                cursorEmpty ? Optional.empty() : EquipmentReads.armorSlotOf(cursor),
                cursorEmpty || EquipmentReads.allowedForPlayer(cursor),
                occupied && EquipmentReads.bound(resting),
                viewer.getGameMode() == GameMode.CREATIVE,
                cursorEmpty ? 0 : cursor.getAmount(),
                occupied,
                !cursorEmpty && NexusItems.isNexus(cursor, adapters.keys())));

        switch (outcome) {
            case PLACE_ONE -> {
                ItemStack one = cursor.clone();
                one.setAmount(1);
                ItemStack rest = cursor.clone();
                rest.setAmount(cursor.getAmount() - 1);
                viewer.setItemOnCursor(rest.getAmount() <= 0 ? null : rest);   // clear first
                equipment.setItem(bukkit, one, false);
            }
            case TAKE -> {
                ItemStack out = resting.clone();
                equipment.setItem(bukkit, null, false);                         // clear first
                viewer.setItemOnCursor(out);
            }
            case SWAP -> {
                ItemStack in = cursor.clone();
                ItemStack out = resting.clone();                                // both cloned first
                equipment.setItem(bukkit, in, false);
                viewer.setItemOnCursor(out);
            }
            case SHIFT_OUT -> {
                if (viewer.getInventory().firstEmpty() == -1) {
                    say("There is no room in your inventory.");
                    return;
                }
                ItemStack out = resting.clone();
                equipment.setItem(bukkit, null, false);                         // clear first
                viewer.getInventory().addItem(out);
            }
            case REFUSE_BOUND -> say("That is bound to you: Curse of Binding.");
            // Every other refusal is silent, as vanilla's are: the item simply stays where it is.
            case SHIFT_IN_ONE, NOTHING, REFUSE_WRONG_SLOT, REFUSE_NOT_FOR_PLAYER, REFUSE_OCCUPIED,
                 REFUSE_STACK, REFUSE_STAR -> { }
        }
        viewer.updateInventory();
    }

    /**
     * A shift-click from the player's half that no input slot took -- every shift-in, here, since
     * there are none. An accessory routes to the accessory column; anything else to its own armour
     * slot. The hook's contract: clear the source first, return true only if something moved.
     */
    @Override
    protected boolean shiftInElsewhere(ItemStack moving, Consumer<ItemStack> setSource) {
        if (AccessoryItems.isAccessory(moving, adapters.keys())) {
            return accessoryShiftIn(moving, setSource);
        }
        Optional<ArmorSlot> target = EquipmentReads.armorSlotOf(moving);
        if (target.isEmpty()) return false;
        EntityEquipment equipment = viewer.getEquipment();
        EquipmentSlot bukkit = bukkitSlot(target.get());
        Outcome outcome = ArmorPlacement.decide(new Input(target.get(), Gesture.SHIFT_IN, target,
                EquipmentReads.allowedForPlayer(moving), false,
                viewer.getGameMode() == GameMode.CREATIVE, moving.getAmount(),
                !MenuSafety.isEmpty(equipment.getItem(bukkit)),
                NexusItems.isNexus(moving, adapters.keys())));
        if (outcome != Outcome.SHIFT_IN_ONE) return false;

        ItemStack one = moving.clone();
        one.setAmount(1);
        ItemStack rest = moving.clone();
        rest.setAmount(moving.getAmount() - 1);
        setSource.accept(rest.getAmount() <= 0 ? null : rest);                 // clear first
        equipment.setItem(bukkit, one, false);
        scheduleRender();
        return true;
    }

    // --- the accessory column ---------------------------------------------------------------------

    private void accessoryClick(int index, boolean shift) {
        UUID id = viewer.getUniqueId();
        Accessories accessories = adapters.accessories();
        Optional<PlayerAccessories> stored = accessories.service().accessories(id);
        if (stored.isEmpty()) {
            say(accessories.service().unusable(id)
                    ? "Your accessories are unavailable this session."
                    : "Your accessories are still loading.");
            return;
        }
        Optional<String> text = stored.get().item(index);
        if (text.isPresent() && readable(index).isEmpty()) {
            say("That slot holds an item this server cannot read. It is kept, and cannot be moved.");
            return;
        }
        ItemStack cursor = viewer.getItemOnCursor();
        boolean cursorEmpty = MenuSafety.isEmpty(cursor);
        if (shift) {
            if (text.isPresent()) unequip(index, text.get(), false);
            return;
        }
        if (cursorEmpty && text.isPresent()) {
            unequip(index, text.get(), true);
        } else if (!cursorEmpty && text.isEmpty()) {
            equipFromCursor(index, cursor);
        } else if (!cursorEmpty) {
            say("Take the accessory out first.");
        }
    }

    /** The decoded definition in a slot, when the slot is occupied AND readable. */
    private Optional<AccessoryDefinition> readable(int index) {
        return adapters.accessories().worn(viewer.getUniqueId())
                .map(worn -> worn.get(index));
    }

    private void equipFromCursor(int index, ItemStack cursor) {
        Optional<AccessoryDefinition> def = definitionOf(cursor);
        if (def.isEmpty()) return;
        if (!equipAllowed(index, def.get())) return;

        ItemStack taken = cursor.clone();
        viewer.setItemOnCursor(null);                                           // take first (§3.9)
        if (!write(index, VaultCodec.encode(taken), ok -> {
            if (!ok) {
                MenuSafety.give(viewer, taken);
                say("That equip FAILED to reach disk; the accessory is back in your inventory.");
            }
        })) {
            viewer.setItemOnCursor(taken);
        }
        viewer.updateInventory();
    }

    private boolean accessoryShiftIn(ItemStack moving, Consumer<ItemStack> setSource) {
        UUID id = viewer.getUniqueId();
        Optional<PlayerAccessories> stored = adapters.accessories().service().accessories(id);
        if (stored.isEmpty()) return false;
        Optional<AccessoryDefinition> def = definitionOf(moving);
        if (def.isEmpty()) return false;

        // A class item aims at slot 0; a universal at the first EMPTY of 1..3, in order.
        int target = -1;
        if (def.get().slot() == io.github.butterflysmp.rpg.core.accessory.AccessorySlotKind.CLASS) {
            if (stored.get().item(AccessorySlots.CLASS_SLOT).isEmpty()) target = AccessorySlots.CLASS_SLOT;
        } else {
            for (int i = 1; i < AccessorySlots.COUNT; i++) {
                if (stored.get().item(i).isEmpty()) { target = i; break; }
            }
        }
        if (target < 0) return false;
        if (!equipAllowed(target, def.get())) return false;

        ItemStack taken = moving.clone();
        setSource.accept(null);                                                 // clear first
        if (!write(target, VaultCodec.encode(taken), ok -> {
            if (!ok) {
                MenuSafety.give(viewer, taken);
                say("That equip FAILED to reach disk; the accessory is back in your inventory.");
            }
        })) {
            setSource.accept(taken);                                            // nothing moved
            return false;
        }
        scheduleRender();
        return true;
    }

    private void unequip(int index, String text, boolean toCursor) {
        Optional<ItemStack> item = VaultCodec.decode(text);
        if (item.isEmpty()) return;   // unreadable -- refused above, and never overwritten
        if (!toCursor && viewer.getInventory().firstEmpty() == -1) {
            say("There is no room in your inventory.");
            return;
        }
        // WRITE FIRST, GIVE ONLY ON SUCCESS (§3.9). Residual, recorded in the gate file: a player who
        // disconnects before the success hop runs is not handed the item (the Scheduler drops tasks
        // for a gone entity) -- the vault's accepted "completion lost" residual.
        write(index, null, ok -> {
            if (!ok) {
                say("That unequip FAILED to reach disk; the accessory stays equipped.");
                return;
            }
            if (toCursor && isOpenHere() && MenuSafety.isEmpty(viewer.getItemOnCursor())) {
                viewer.setItemOnCursor(item.get());
            } else {
                MenuSafety.give(viewer, item.get());
            }
            viewer.updateInventory();
        });
    }

    private boolean write(int index, String encoded, Consumer<Boolean> onSettledOnOwnThread) {
        AccessoryService service = adapters.accessories().service();
        boolean accepted = service.write(viewer.getUniqueId(), index, encoded, ok ->
                adapters.scheduler().onEntity(viewer, () -> {
                    onSettledOnOwnThread.accept(ok);
                    renderIfOpen();
                }));
        if (!accepted) say("Your accessories accept no changes right now; nothing moved.");
        return accepted;
    }

    private Optional<AccessoryDefinition> definitionOf(ItemStack item) {
        Optional<String> id = AccessoryItems.accessoryId(item, adapters.keys());
        if (id.isEmpty()) {
            say("Only accessories go in these slots.");
            return Optional.empty();
        }
        Optional<AccessoryDefinition> def = adapters.accessories().registry().find(id.get());
        if (def.isEmpty()) say("'" + id.get() + "' has no content file loaded.");
        return def;
    }

    private boolean equipAllowed(int index, AccessoryDefinition def) {
        return switch (AccessorySlots.canEquip(index, def, profileClass())) {
            case OK -> true;
            case CLASS_SLOT_LOCKED -> { say("The class slot is locked: choose a class first."); yield false; }
            case WRONG_SLOT_KIND -> {
                say(index == AccessorySlots.CLASS_SLOT
                        ? "This slot takes a class accessory."
                        : "These slots take universal accessories; a class accessory goes in the top one.");
                yield false;
            }
            case WRONG_CLASS -> {
                say("That accessory is for " + AccessorySlots.classToken(def.accessoryClass()) + ".");
                yield false;
            }
        };
    }

    // --- render -----------------------------------------------------------------------------------

    private void scheduleRender() {
        adapters.scheduler().onEntityLater(viewer, this::renderIfOpen, 1);
    }

    private void renderIfOpen() {
        if (isOpenHere()) render();
    }

    private boolean isOpenHere() {
        return viewer.getOpenInventory().getTopInventory().getHolder() == this;
    }

    private void render() {
        for (int slot : EquipmentMenuLayout.FILLER_SLOTS) {
            getInventory().setItem(slot, MenuIcons.filler());
        }
        getInventory().setItem(EquipmentMenuLayout.CLOSE_SLOT, MenuIcons.close());
        getInventory().setItem(EquipmentMenuLayout.BACK_SLOT, MenuIcons.back(Material.ARROW, "the Nexus"));
        getInventory().setItem(EquipmentMenuLayout.ARMOR_LABEL_SLOT, MenuIcons.icon(Material.IRON_CHESTPLATE,
                MenuIcons.line("Armour", NamedTextColor.GRAY), List.of()));
        getInventory().setItem(EquipmentMenuLayout.ACCESSORY_LABEL_SLOT, MenuIcons.icon(Material.ECHO_SHARD,
                MenuIcons.line("Accessories", NamedTextColor.GRAY), List.of()));

        EntityEquipment equipment = viewer.getEquipment();
        for (ArmorSlot armor : ArmorSlot.values()) {
            ItemStack worn = equipment.getItem(bukkitSlot(armor));
            getInventory().setItem(EquipmentMenuLayout.slotOf(armor), MenuSafety.isEmpty(worn)
                    ? MenuIcons.icon(Material.GRAY_STAINED_GLASS_PANE,
                            MenuIcons.line(armorName(armor), NamedTextColor.GRAY),
                            List.of(MenuIcons.line("Empty", NamedTextColor.DARK_GRAY)))
                    : worn.clone());
        }
        renderAccessories();
    }

    private void renderAccessories() {
        UUID id = viewer.getUniqueId();
        Accessories accessories = adapters.accessories();
        Optional<PlayerAccessories> stored = accessories.service().accessories(id);
        String profileClass = profileClass();
        for (int index = 0; index < AccessorySlots.COUNT; index++) {
            int slot = EquipmentMenuLayout.ACCESSORY_SLOTS.get(index);
            if (stored.isEmpty()) {
                getInventory().setItem(slot, accessories.service().unusable(id)
                        ? MenuIcons.icon(Material.BARRIER, MenuIcons.line("Accessories unavailable", NamedTextColor.RED),
                                List.of(MenuIcons.line("This session; see the server log.", NamedTextColor.DARK_GRAY)))
                        : MenuIcons.icon(Material.GRAY_STAINED_GLASS_PANE,
                                MenuIcons.line("Loading...", NamedTextColor.GRAY), List.of()));
                continue;
            }
            Optional<String> text = stored.get().item(index);
            if (text.isEmpty()) {
                getInventory().setItem(slot, emptyAccessorySlot(index, profileClass));
                continue;
            }
            Optional<AccessoryDefinition> def = readable(index);
            Optional<ItemStack> item = VaultCodec.decode(text.get());
            if (def.isEmpty() || item.isEmpty()) {
                getInventory().setItem(slot, MenuIcons.icon(Material.BARRIER,
                        MenuIcons.line("Unreadable item", NamedTextColor.RED),
                        List.of(MenuIcons.line("Kept, not deleted. Contributes nothing.", NamedTextColor.DARK_GRAY))));
                continue;
            }
            ItemStack shown = item.get().clone();
            if (!AccessorySlots.contributes(def.get(), profileClass)) {
                shown.editMeta(meta -> {
                    List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
                    lore.add(MenuIcons.line("Inactive — requires class: "
                            + AccessoryLoreLines.className(def.get()), NamedTextColor.RED));
                    meta.lore(lore);
                });
            }
            getInventory().setItem(slot, shown);
        }
    }

    private ItemStack emptyAccessorySlot(int index, String profileClass) {
        if (index == AccessorySlots.CLASS_SLOT) {
            return AccessorySlots.hasClass(profileClass)
                    ? MenuIcons.icon(Material.ORANGE_STAINED_GLASS_PANE,
                            MenuIcons.line("Class accessory slot", NamedTextColor.GOLD), List.of())
                    : MenuIcons.icon(Material.BARRIER,
                            MenuIcons.line("Choose a class to unlock", NamedTextColor.RED), List.of());
        }
        return MenuIcons.icon(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                MenuIcons.line("Universal accessory slot", NamedTextColor.GRAY), List.of());
    }

    // --- helpers ----------------------------------------------------------------------------------

    private String profileClass() {
        return profiles.profile(viewer.getUniqueId()).map(PlayerProfile::archetypeId).orElse(null);
    }

    private static EquipmentSlot bukkitSlot(ArmorSlot armor) {
        return switch (armor) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
        };
    }

    private static String armorName(ArmorSlot armor) {
        return switch (armor) {
            case HEAD -> "Head";
            case CHEST -> "Chest";
            case LEGS -> "Legs";
            case FEET -> "Feet";
        };
    }

    private void say(String message) {
        viewer.sendMessage(MenuIcons.line(message, NamedTextColor.GRAY));
    }

    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        // Only the cursor can be holding anything of the player's; returnEverything hands it back
        // (and drops at the feet when the inventory is full). Idempotent, as onClose must be.
        returnEverything();
    }
}
