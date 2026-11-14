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
 * {@link #permitted}: {@code Permissions.DEV} AND the dev-server marker (the seat's ruling, 2026-11-14,
 * PLAN §2). Each runs on the main thread, as Brigadier executes commands there.
 */
public final class MiningDevCommand {

    private MiningDevCommand() {}

    /**
     * Whether a sender sees {@code /rpg mining} at all: the DEV permission AND a dev server. The seat's
     * ruling, 2026-11-14: BOTH, and nothing else. The selftest's other conditions (the dev world, one
     * player, SURVIVAL) belong to a RUN, not to a dev command, so they are not here.
     *
     * <p>{@code devServer} is {@code Boolean.getBoolean(SelfTestGuard.DEV_PROPERTY)}, read ONCE in
     * {@code RpgPlugin} and handed to both the selftest and this rule: {@code -Drpg.dev=true}, which only
     * {@code scripts/dev-server.sh} sets.
     *
     * <p><b>Off a dev server the subtree is ABSENT, not a refusal.</b> This is a Brigadier
     * {@code requires}, so a sender it rejects is never sent the node and the dispatcher answers as it
     * does for any command that sender cannot use. Absent, because: the marker is fixed for the JVM's
     * life, so the answer never changes mid-boot; a refusal would need a node every sender can see and
     * a check on each of its leaves; and a production server should not advertise dev tools. The cost:
     * an op on a non-dev server learns nothing from the reply. That is the trade, stated.
     */
    public static boolean permitted(boolean hasDevPermission, boolean devServer) {
        return hasDevPermission && devServer;
    }

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
