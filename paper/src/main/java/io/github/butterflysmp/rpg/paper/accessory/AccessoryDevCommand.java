package io.github.butterflysmp.rpg.paper.accessory;

import com.mojang.brigadier.context.CommandContext;
import io.github.butterflysmp.rpg.core.accessory.AccessoryContributions;
import io.github.butterflysmp.rpg.core.accessory.AccessorySlots;
import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.menu.MenuSafety;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.vault.VaultCodec;
import io.github.butterflysmp.rpg.paper.weapon.AccessoryItems;
import io.github.butterflysmp.rpg.storage.PlayerAccessories;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * {@code /rpg accessory <show|equip|unequip> [slot]} -- a DEV INSTRUMENT, not a feature.
 *
 * <h2>Why it exists, and when it is deleted</h2>
 *
 * Slice A ships accessories with no screen to wear them from; the Equipment submenu is Slice B. So
 * without this, nothing in Slice A's gate could put an accessory on a player, and every stat row
 * would be unstageable. It is the vault's {@code VaultDevCommand} for the same reason.
 *
 * <p><b>DELETION TRIGGER: delete this class, and its branch in {@code RpgCommand}, when
 * {@code GATE-accessories-b.md} reads PASS on every Equipment-screen row.</b> The screen then does
 * this command's job with the vanilla rules reproduced, and a second, rule-free way to equip would be
 * a door the screen's rules do not watch. Marker for the sweep: {@code DEV: delete when
 * GATE-accessories-b.md passes}.
 *
 * <h2>The rules it applies are the real ones</h2>
 *
 * Equipping goes through {@link AccessorySlots#canEquip} -- the class slot is locked with no class and
 * takes only the profile's own class (ruling A1) -- so a gate row read through this command reads the
 * same decision the screen will make.
 *
 * <h2>The item moves only after the write settles</h2>
 *
 * Equip takes the item from the hand, then writes; on a FAILED write it gives the item back. Unequip
 * writes the empty slot, and gives the item only on SUCCESS. So the item is never both in the file
 * and in the inventory. The write's callback runs on the storage thread, so both hand-backs hop to
 * the player's thread first.
 *
 * <p>Permissions.DEV, like {@code /rpg vault}: an equip takes an item out of the world into a file,
 * and ungated that is a duplication surface.
 */
public final class AccessoryDevCommand {

    private AccessoryDevCommand() {}

    public static int usage(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("Usage: /rpg accessory <show|equip|unequip> [slot]",
                NamedTextColor.RED));
        sender.sendMessage(Component.text("Slots are 0.." + (AccessorySlots.COUNT - 1) + ": "
                + AccessorySlots.CLASS_SLOT + " is the class slot, the rest are universal. equip takes"
                + " the accessory in your MAIN HAND.", NamedTextColor.GRAY));
        return 0;
    }

    public static int show(CommandContext<CommandSourceStack> ctx, AdapterContext adapters,
                           ProfileService profiles) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) return playersOnly(ctx);
        Accessories accessories = adapters.accessories();
        Optional<PlayerAccessories> stored = accessories.service().accessories(player.getUniqueId());
        if (stored.isEmpty()) {
            // Not "you wear nothing": not loaded and empty are different, and saying the wrong one
            // sends an operator to look at the wrong thing.
            player.sendMessage(Component.text(accessories.service().unusable(player.getUniqueId())
                    ? "Your accessories are UNAVAILABLE this session -- the file could not be read, or a"
                            + " write failed. See the server log."
                    : "Your accessories are still loading.", NamedTextColor.RED));
            return 0;
        }
        String profileClass = profileClass(profiles, player);
        List<AccessoryDefinition> worn = accessories.worn(player.getUniqueId()).orElse(List.of());
        player.sendMessage(Component.text("Accessories (profile class: " + profileClass + "):",
                NamedTextColor.AQUA));
        for (int slot = 0; slot < AccessorySlots.COUNT; slot++) {
            String kind = slot == AccessorySlots.CLASS_SLOT ? "class" : "universal";
            String line;
            NamedTextColor color = NamedTextColor.GRAY;
            if (stored.get().item(slot).isEmpty()) {
                line = "empty";
            } else if (worn.size() <= slot || worn.get(slot) == null) {
                line = "UNREADABLE -- kept verbatim, contributes nothing";
                color = NamedTextColor.RED;
            } else {
                AccessoryDefinition def = worn.get(slot);
                boolean active = AccessorySlots.contributes(def, profileClass);
                line = plainName(def) + " (" + def.id() + ")" + (active ? "" : " -- INACTIVE");
                color = active ? NamedTextColor.WHITE : NamedTextColor.YELLOW;
            }
            player.sendMessage(Component.text("  " + slot + " [" + kind + "] " + line, color));
        }
        player.sendMessage(Component.text("  source keys: " + AccessoryContributions.SOURCE_PREFIX
                + "<slot>", NamedTextColor.DARK_GRAY));
        return 1;
    }

    public static int equip(CommandContext<CommandSourceStack> ctx, AdapterContext adapters,
                            ProfileService profiles, int slot) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) return playersOnly(ctx);
        adapters.scheduler().onEntity(player, () -> equipOnOwnThread(player, adapters, profiles, slot));
        return 1;
    }

    private static void equipOnOwnThread(Player player, AdapterContext adapters, ProfileService profiles,
                                         int slot) {
        Accessories accessories = adapters.accessories();
        ItemStack held = player.getInventory().getItemInMainHand();
        String id = AccessoryItems.accessoryId(held, adapters.keys()).orElse(null);
        if (id == null) {
            player.sendMessage(Component.text("Hold an accessory in your main hand.", NamedTextColor.RED));
            return;
        }
        AccessoryDefinition def = accessories.registry().find(id).orElse(null);
        if (def == null) {
            player.sendMessage(Component.text("'" + id + "' has no content file loaded -- refusing to"
                    + " equip something whose stats cannot be read.", NamedTextColor.RED));
            return;
        }
        String profileClass = profileClass(profiles, player);
        switch (AccessorySlots.canEquip(slot, def, profileClass)) {
            case OK -> { }
            case CLASS_SLOT_LOCKED -> {
                player.sendMessage(Component.text("The class slot is locked: choose a class first"
                        + " (/rpg class).", NamedTextColor.RED));
                return;
            }
            case WRONG_SLOT_KIND -> {
                player.sendMessage(Component.text(slot == AccessorySlots.CLASS_SLOT
                        ? "Slot 0 takes a CLASS accessory; that one is universal -- use slots 1-3."
                        : "Slots 1-3 take UNIVERSAL accessories; a class accessory goes in slot 0.",
                        NamedTextColor.RED));
                return;
            }
            case WRONG_CLASS -> {
                player.sendMessage(Component.text("That accessory is for "
                        + AccessorySlots.classToken(def.accessoryClass()) + "; your class is "
                        + profileClass + ".", NamedTextColor.RED));
                return;
            }
        }
        Optional<PlayerAccessories> stored = accessories.service().accessories(player.getUniqueId());
        if (stored.isEmpty()) {
            player.sendMessage(Component.text("Your accessories are not loaded (see /rpg accessory"
                    + " show); nothing was equipped.", NamedTextColor.RED));
            return;
        }
        if (stored.get().item(slot).isPresent()) {
            // Refused rather than swapped: a swap is two writes, and the screen will own that.
            player.sendMessage(Component.text("Slot " + slot + " is occupied -- unequip it first.",
                    NamedTextColor.RED));
            return;
        }

        ItemStack taken = held.clone();
        String encoded = VaultCodec.encode(taken);
        // TAKE FIRST, THEN WRITE -- and give back only if the write fails. See the class note.
        player.getInventory().setItemInMainHand(null);
        boolean accepted = accessories.service().write(player.getUniqueId(), slot, encoded, ok ->
                adapters.scheduler().onEntity(player, () -> {
                    if (ok) {
                        player.sendMessage(Component.text("Equipped " + plainName(def) + " in slot "
                                + slot + ".", NamedTextColor.AQUA));
                    } else {
                        MenuSafety.give(player, taken);
                        player.sendMessage(Component.text("That equip FAILED to reach disk; the"
                                + " accessory is back in your inventory and your accessories accept"
                                + " no more changes this session. See the server log.", NamedTextColor.RED));
                    }
                }));
        if (!accepted) {
            player.getInventory().setItemInMainHand(taken);
            player.sendMessage(Component.text("The write was refused; you still hold it.", NamedTextColor.RED));
        }
    }

    public static int unequip(CommandContext<CommandSourceStack> ctx, AdapterContext adapters, int slot) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) return playersOnly(ctx);
        adapters.scheduler().onEntity(player, () -> unequipOnOwnThread(player, adapters, slot));
        return 1;
    }

    private static void unequipOnOwnThread(Player player, AdapterContext adapters, int slot) {
        AccessoryService service = adapters.accessories().service();
        Optional<PlayerAccessories> stored = service.accessories(player.getUniqueId());
        if (stored.isEmpty()) {
            player.sendMessage(Component.text("Your accessories are not loaded (see /rpg accessory"
                    + " show).", NamedTextColor.RED));
            return;
        }
        Optional<String> text = stored.get().item(slot);
        if (text.isEmpty()) {
            player.sendMessage(Component.text("Slot " + slot + " is empty.", NamedTextColor.RED));
            return;
        }
        Optional<ItemStack> item = VaultCodec.decode(text.get());
        if (item.isEmpty()) {
            player.sendMessage(Component.text("That entry cannot be decoded by this server. It has been"
                    + " LEFT IN PLACE rather than discarded -- a later build may read it.", NamedTextColor.RED));
            return;
        }
        // WRITE FIRST, THEN GIVE -- and only on success. See the class note.
        boolean accepted = service.write(player.getUniqueId(), slot, null, ok ->
                adapters.scheduler().onEntity(player, () -> {
                    if (ok) {
                        MenuSafety.give(player, item.get());
                        player.sendMessage(Component.text("Unequipped slot " + slot + ".", NamedTextColor.AQUA));
                    } else {
                        player.sendMessage(Component.text("That unequip FAILED to reach disk; the"
                                + " accessory stays in your accessories file and your accessories"
                                + " accept no more changes this session. See the server log.",
                                NamedTextColor.RED));
                    }
                }));
        if (!accepted) {
            player.sendMessage(Component.text("The write was refused; nothing moved.", NamedTextColor.RED));
        }
    }

    private static String profileClass(ProfileService profiles, Player player) {
        return profiles.profile(player.getUniqueId()).map(PlayerProfile::archetypeId).orElse(null);
    }

    static String plainName(AccessoryDefinition def) {
        return PlainTextComponentSerializer.plainText()
                .serialize(MiniMessage.miniMessage().deserialize(def.displayName()));
    }

    private static int playersOnly(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
        return 0;
    }
}
