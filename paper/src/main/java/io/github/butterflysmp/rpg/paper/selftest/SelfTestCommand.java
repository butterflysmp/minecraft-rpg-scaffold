package io.github.butterflysmp.rpg.paper.selftest;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.butterflysmp.rpg.paper.command.Permissions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * {@code /rpg selftest all | <gate> [row] | stop}. Op-only ({@link Permissions#DEV}); the guard
 * ({@link SelfTestGuard}) refuses everywhere but a dev server, in the dev world, with one player online.
 *
 * <p>Built as its own node and added under {@code /rpg} by {@code RpgPlugin}, so {@code RpgCommand.build}'s
 * signature does not grow.
 */
public final class SelfTestCommand {

    private SelfTestCommand() {}

    public static LiteralCommandNode<CommandSourceStack> build(SelfTestService service) {
        return Commands.literal("selftest")
                .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                .then(Commands.literal("stop").executes(ctx -> reply(ctx, p -> service.stop(p))))
                .then(Commands.argument("gate", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("all");
                            service.gates().keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> reply(ctx, p -> service.start(p, StringArgumentType.getString(ctx, "gate"), null)))
                        .then(Commands.argument("row", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    var rows = service.gates().get(StringArgumentType.getString(ctx, "gate"));
                                    if (rows != null) rows.stream().map(Scenario::row)
                                            .filter(r -> !r.equals(Scenario.SETUP)).forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> reply(ctx, p -> service.start(p,
                                        StringArgumentType.getString(ctx, "gate"),
                                        StringArgumentType.getString(ctx, "row"))))))
                .build();
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, java.util.function.Function<Player, String> action) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }
        player.sendMessage(Component.text(action.apply(player), NamedTextColor.GRAY));
        return 1;
    }
}
