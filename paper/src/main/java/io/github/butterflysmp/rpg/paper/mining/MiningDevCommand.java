package io.github.butterflysmp.rpg.paper.mining;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.function.BiFunction;

/**
 * The {@code /rpg mining} dev commands (PLAN-mining.md 5.4). Wired in {@code RpgCommand} behind
 * {@code Permissions.DEV}; they move to the {@code rpg.dev} guard once #175 merges (PLAN §2).
 * Each runs on the main thread, as Brigadier executes commands there.
 */
public final class MiningDevCommand {

    private MiningDevCommand() {}

    public static int usage(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendMessage(Component.text(
                "/rpg mining place <ore> <max size> | remove | regrow | roll <size> | trace | pack", NamedTextColor.YELLOW));
        return Command.SINGLE_SUCCESS;
    }

    /** S2: {@code /rpg mining roll <size>}, a fresh patch of that size at the looked-at slot. */
    public static int roll(CommandContext<CommandSourceStack> ctx, MiningService mining, String size) {
        return asPlayer(ctx, (player, m) -> m.roll(player, size), mining);
    }

    public static int place(CommandContext<CommandSourceStack> ctx, MiningService mining, String ore, String size) {
        return asPlayer(ctx, (player, m) -> m.place(player, ore, size), mining);
    }

    public static int remove(CommandContext<CommandSourceStack> ctx, MiningService mining) {
        return asPlayer(ctx, (player, m) -> m.remove(player), mining);
    }

    public static int regrow(CommandContext<CommandSourceStack> ctx, MiningService mining) {
        return asPlayer(ctx, (player, m) -> m.regrow(player), mining);
    }

    public static int trace(CommandContext<CommandSourceStack> ctx, MiningService mining) {
        return asPlayer(ctx, (player, m) -> m.trace(player), mining);
    }

    public static int pack(CommandContext<CommandSourceStack> ctx, MiningService mining) {
        return asPlayer(ctx, (player, m) -> {
            m.offerPack(player);
            return Component.text("Offered the dev pack.", NamedTextColor.GREEN);
        }, mining);
    }

    private static int asPlayer(CommandContext<CommandSourceStack> ctx,
                                BiFunction<Player, MiningService, Component> action, MiningService mining) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }
        player.sendMessage(action.apply(player, mining));
        return Command.SINGLE_SUCCESS;
    }
}
