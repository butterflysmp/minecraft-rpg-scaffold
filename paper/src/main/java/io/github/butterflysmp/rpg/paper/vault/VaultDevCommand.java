package io.github.butterflysmp.rpg.paper.vault;

import com.mojang.brigadier.context.CommandContext;
import io.github.butterflysmp.rpg.core.vault.VaultCell;
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
 * <h2>THE ASYNC WINDOW: PR 2 PAID THE HOOK, AND THIS COMMAND STILL CANNOT UNDO ITS OWN WRITE</h2>
 *
 * {@code store} clears the player's hand when {@link VaultService#writePage} returns {@code true},
 * which reports only that the write was ISSUED. If the disk write then fails, the item is gone from
 * the hand and never reached the file.
 *
 * <p><b>This section used to say the hook was OWED TO PR 2. It is built</b>: {@code writePage} now
 * takes a failure callback, poisons the vault and logs a report naming the page and every cell, and
 * the screen degrades so its close hands the page back.
 *
 * <p><b>The window itself is NOT closed here, and that is the honest difference rather than an
 * oversight.</b> The screen can hand items back because it still holds them; this command handed
 * them over ticks ago. So {@link #onWriteFailed} tells the operator what happened and does not
 * pretend to repair it -- <b>which is why an operator instrument is allowed this window and the
 * screen is not.</b>
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

        // THE CELLS THE PAGE ALREADY HOLDS ARE CARRIED WITH NO DESCRIPTION. They came off disk as
        // text and this command has not decoded them, so there is no ItemStack to name -- and
        // inventing one would put a guess in a log line an operator is meant to act on. Only the
        // cell being written can be described, which is the one this command knows about.
        Map<Integer, VaultCell> updated = new LinkedHashMap<>();
        found.get().page(page).forEach((cell, item) ->
                updated.put(cell, new VaultCell(item, VaultCell.UNDESCRIBED)));
        updated.put(slot, new VaultCell(encoded, held.getType() + " x" + held.getAmount()));

        if (!vaults.writePage(player.getUniqueId(), page, updated, () -> onWriteFailed(player))) {
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
     * A write this command issued completed exceptionally.
     *
     * <h2>*** IT TELLS THE OPERATOR, AND IT DOES NOT PRETEND TO FIX ANYTHING ***</h2>
     *
     * {@code VaultService} has already poisoned the vault and logged the report naming the page and
     * every cell. There is nothing this command can repair: the item has already left the hand (for
     * {@code store}) or been handed over (for {@code take}), a tick or more ago. <b>So this says so,
     * in the same words the screen uses</b>, rather than being an empty callback that makes the
     * failure look handled.
     *
     * <p><b>ARRIVES ON THE I/O THREAD</b>, so it must not touch the Bukkit API directly. There is no
     * {@code Scheduler} in this class, and {@code Player.sendMessage} is the one Bukkit call that is
     * documented thread-safe on Paper -- so the message goes out directly and <b>nothing else here
     * may follow it.</b> A second line of Bukkit work in this method is a bug, not an extension.
     */
    private static void onWriteFailed(Player player) {
        player.sendMessage(Component.text(
                "That vault write FAILED to reach disk. This vault will accept no further writes"
                        + " this session, and the file keeps its last good contents. See the server"
                        + " log for the page and the items.", NamedTextColor.RED));
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

        Map<Integer, VaultCell> updated = new LinkedHashMap<>();
        found.get().page(page).forEach((cell, item) ->
                updated.put(cell, new VaultCell(item, VaultCell.UNDESCRIBED)));
        updated.remove(slot);

        if (!vaults.writePage(player.getUniqueId(), page, updated, () -> onWriteFailed(player))) {
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
