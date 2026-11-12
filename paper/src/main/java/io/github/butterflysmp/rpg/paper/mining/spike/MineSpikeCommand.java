package io.github.butterflysmp.rpg.paper.mining.spike;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * The bodies of {@code /rpg minespike}. SPIKE CODE. The node, and its {@code Permissions.DEV} gate,
 * live in {@code RpgCommand} (the seat's ruling, 2026-11-12: DEV only, no -D property).
 */
public final class MineSpikeCommand {

    private MineSpikeCommand() {}

    public static int usage(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendMessage(Component.text(
                "/rpg minespike <block|box|model> (again to stop) | pick <speed> | pack", NamedTextColor.GRAY));
        return 1;
    }

    public static int toggle(CommandContext<CommandSourceStack> ctx, MineSpike spike, MineSpike.Mode mode) {
        Player player = player(ctx);
        if (player == null) return 0;
        spike.toggle(player, mode);
        return 1;
    }

    public static int pick(CommandContext<CommandSourceStack> ctx, MineSpike spike, double speed) {
        Player player = player(ctx);
        if (player == null) return 0;
        spike.givePick(player, speed);
        return 1;
    }

    public static int pack(CommandContext<CommandSourceStack> ctx, MineSpike spike) {
        Player player = player(ctx);
        if (player == null) return 0;
        spike.offerPack(player);
        return 1;
    }

    private static Player player(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getExecutor() instanceof Player p) return p;
        ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
        return null;
    }
}
