package io.github.butterflysmp.rpg.paper.vault;

import com.mojang.brigadier.context.CommandContext;
import io.github.butterflysmp.rpg.core.vault.VaultShape;
import io.github.butterflysmp.rpg.paper.menu.MenuSafety;
import io.github.butterflysmp.rpg.storage.PlayerVault;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@code /rpg vault} -- the dev read-back, and the only way to put anything in a vault in PR 1.
 *
 * <h2>*** WHY A COMMAND EXISTS BEFORE THE SCREEN DOES ***</h2>
 *
 * PR 1 ships the storage layer with <b>no player-visible change</b>, which would leave its two most
 * expensive claims -- that a minted weapon survives a round trip with its PDC intact, and that the
 * last write survives a {@code /stop} -- witnessable only by a screen that does not exist yet.
 *
 * <p>Neither claim can be reached by a unit test in any module: encoding an {@code ItemStack} needs
 * {@code Bukkit.getUnsafe()} and there is no MockBukkit. <b>So without this command, PR 1's gate
 * block would be empty and the storage layer would land unwitnessed.</b>
 *
 * <h2>GATED ON {@code Permissions.DEV}, AND THAT IS NOT A FORMALITY</h2>
 *
 * {@code store} takes an item out of the world into a file and {@code take} puts one back.
 * <b>Ungated, that is an item duplicator</b> -- the same exposure {@code /rpg playerxp} has, which
 * is why it carries the same node. The gate is asserted by {@code VaultWiringSignatureTest}.
 *
 * <h2>THE ASYNC WINDOW IS ACCEPTED HERE AND MUST NOT BE IN PR 2</h2>
 *
 * {@code store} clears the player's hand when {@link VaultService#writePage} returns {@code true},
 * which reports only that the write was ISSUED. If the disk write then fails, the item is gone from
 * the hand and never reached the file.
 *
 * <p><b>That is tolerable for an operator instrument and is not tolerable for the screen</b>, which
 * is exactly the degraded-menu hook PR 2 owes. Written down here rather than left as a difference
 * somebody notices later.
 */
public final class VaultDevCommand {

    private VaultDevCommand() {}

    /** Usage, printed for any incomplete form. */
    public static int usage(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text(
                "Usage: /rpg vault <dump|store|take> [page] [slot]", NamedTextColor.RED));
        sender.sendMessage(Component.text(
                "Pages are 1.." + VaultShape.PAGE_COUNT + " and slots 0.."
                        + (VaultShape.SLOTS_PER_PAGE - 1) + " as you type them; both are stored"
                        + " zero-based.", NamedTextColor.GRAY));
        return 0;
    }

    /**
     * What is in this player's vault, page by page.
     *
     * <p>Prints the OCCUPIED slots and, for each, whether this server can still decode it. That
     * second column is the whole reason the command is worth having: an entry that is present but
     * undecodable is invisible to every other instrument, and it is the state the opaque-preservation
     * rule exists to protect.
     */
    public static int dump(CommandContext<CommandSourceStack> ctx, VaultService vaults) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }

        Optional<PlayerVault> found = vaults.vault(player.getUniqueId());
        if (found.isEmpty()) {
            // Deliberately NOT "your vault is empty". Not loaded and empty are different, and
            // telling an operator the wrong one sends them to look at the wrong thing.
            player.sendMessage(Component.text(
                    "No vault is loaded for you -- it is still reading, or the file could not be"
                            + " read. Check the server log.", NamedTextColor.RED));
            return 0;
        }

        PlayerVault vault = found.get();
        player.sendMessage(Component.text(
                "Vault: " + vault.occupiedSlots() + " occupied slot(s), schema v"
                        + vault.schemaVersion(), NamedTextColor.AQUA));

        for (int page = 0; page < VaultShape.PAGE_COUNT; page++) {
            Map<Integer, String> contents = vault.page(page);
            if (contents.isEmpty()) continue;

            player.sendMessage(Component.text(
                    "  page " + (page + 1) + ": " + contents.size() + " item(s)",
                    NamedTextColor.GRAY));
            for (Map.Entry<Integer, String> cell : contents.entrySet()) {
                Optional<ItemStack> decoded = VaultCodec.decode(cell.getValue());
                String describe = decoded
                        .map(item -> item.getType() + " x" + item.getAmount())
                        .orElse("UNDECODABLE -- kept verbatim, not discarded");
                player.sendMessage(Component.text(
                        "    slot " + cell.getKey() + ": " + describe,
                        decoded.isPresent() ? NamedTextColor.GRAY : NamedTextColor.RED));
            }
        }
        return 1;
    }

    /**
     * Put the item in your main hand into a vault cell.
     *
     * <p>The hand is cleared only if the write was accepted, so a refusal leaves the operator
     * holding the only copy.
     */
    public static int store(CommandContext<CommandSourceStack> ctx, VaultService vaults,
                            int page, int slot) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        String encoded = VaultCodec.encode(held);
        if (encoded == null) {
            player.sendMessage(Component.text("Hold the item you want to store.",
                    NamedTextColor.RED));
            return 0;
        }

        Optional<PlayerVault> found = vaults.vault(player.getUniqueId());
        if (found.isEmpty()) {
            player.sendMessage(Component.text("No vault is loaded for you; nothing was stored.",
                    NamedTextColor.RED));
            return 0;
        }
        if (found.get().page(page).containsKey(slot)) {
            // Refused rather than overwritten. Silently replacing would DESTROY whatever was there,
            // which is the one outcome this whole slice is built to make unreachable.
            player.sendMessage(Component.text(
                    "Page " + (page + 1) + " slot " + slot + " is occupied. Take it first.",
                    NamedTextColor.RED));
            return 0;
        }

        Map<Integer, String> updated = new LinkedHashMap<>(found.get().page(page));
        updated.put(slot, encoded);
        if (!vaults.writePage(player.getUniqueId(), page, updated)) {
            player.sendMessage(Component.text("The vault write was refused; you still hold it.",
                    NamedTextColor.RED));
            return 0;
        }

        player.getInventory().setItemInMainHand(null);
        player.sendMessage(Component.text(
                "Stored in page " + (page + 1) + " slot " + slot + ".", NamedTextColor.AQUA));
        return 1;
    }

    /**
     * Take an item back out of a vault cell.
     *
     * <p>Cleared from the vault BEFORE it is handed over, the same ordering {@code returnEverything}
     * uses: the reverse duplicates the item if the give throws.
     *
     * <p><b>An undecodable entry is refused and left exactly where it is.</b> This server cannot
     * turn those bytes into an item; a later one may, and clearing the cell would be the one step
     * that cannot be undone.
     */
    public static int take(CommandContext<CommandSourceStack> ctx, VaultService vaults,
                           int page, int slot) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }

        Optional<PlayerVault> found = vaults.vault(player.getUniqueId());
        if (found.isEmpty()) {
            player.sendMessage(Component.text("No vault is loaded for you.", NamedTextColor.RED));
            return 0;
        }

        String stored = found.get().page(page).get(slot);
        if (stored == null) {
            player.sendMessage(Component.text(
                    "Page " + (page + 1) + " slot " + slot + " is empty.", NamedTextColor.RED));
            return 0;
        }

        Optional<ItemStack> decoded = VaultCodec.decode(stored);
        if (decoded.isEmpty()) {
            player.sendMessage(Component.text(
                    "That entry cannot be decoded by this server. It has been LEFT IN PLACE rather"
                            + " than discarded -- a later build may read it.", NamedTextColor.RED));
            return 0;
        }

        Map<Integer, String> updated = new LinkedHashMap<>(found.get().page(page));
        updated.remove(slot);
        if (!vaults.writePage(player.getUniqueId(), page, updated)) {
            player.sendMessage(Component.text(
                    "The vault write was refused; the item is still in the vault.",
                    NamedTextColor.RED));
            return 0;
        }

        MenuSafety.give(player, decoded.get());
        player.sendMessage(Component.text(
                "Taken from page " + (page + 1) + " slot " + slot + ".", NamedTextColor.AQUA));
        return 1;
    }
}
