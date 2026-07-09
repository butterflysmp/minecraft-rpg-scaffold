package io.github.yourname.rpg.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.yourname.rpg.core.ability.AbilityRegistry;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import com.mojang.brigadier.tree.LiteralCommandNode;

/**
 * Brigadier commands, registered through the plugin lifecycle manager.
 * NOT CommandExecutor. NOT onCommand. NOT plugin.yml commands: block.
 */
public final class RpgCommand {

    public static LiteralCommandNode<CommandSourceStack> build(AbilityRegistry registry) {
        return Commands.literal("rpg")
                .then(Commands.literal("abilities")
                        // requires() gates the whole branch: an unpermitted sender
                        // cannot run it, and does not see it in tab completion.
                        .requires(source -> source.getSender().hasPermission(Permissions.ADMIN))
                        .executes(ctx -> {
                            ctx.getSource().getSender().sendMessage(
                                    Component.text("Loaded abilities: " + registry.size(),
                                            NamedTextColor.GOLD));
                            registry.all().forEach(a ->
                                    ctx.getSource().getSender().sendMessage(
                                            Component.text("  " + a.id() + " (" + a.element() + ")",
                                                    NamedTextColor.GRAY)));
                            return 1;
                        }))
                .then(Commands.literal("cast")
                        .requires(source -> source.getSender().hasPermission(Permissions.CAST))
                        .then(Commands.argument("ability", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    registry.all().forEach(a -> builder.suggest(a.id()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "ability");
                                    if (!(ctx.getSource().getExecutor() instanceof Player p)) {
                                        ctx.getSource().getSender().sendMessage(
                                                Component.text("Players only.", NamedTextColor.RED));
                                        return 0;
                                    }
                                    p.sendMessage(Component.text("Casting " + id, NamedTextColor.AQUA));
                                    // TODO: wire to AbilityService
                                    return 1;
                                })))
                .build();
    }
}
