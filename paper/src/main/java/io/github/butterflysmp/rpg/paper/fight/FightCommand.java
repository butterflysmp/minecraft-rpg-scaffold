package io.github.butterflysmp.rpg.paper.fight;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.butterflysmp.rpg.paper.command.Permissions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.function.Function;

/**
 * {@code /rpg fight start | land <dx> <dz> | mage-die | stop}: the dev fixture of steps F4 + F5
 * (GATE-fight-ring.md). Op-only ({@link Permissions#DEV}), and refused off a dev server ({@code -Drpg.dev=true}), as
 * {@code /rpg selftest} is. Built as its own node and added under {@code /rpg} by {@code RpgPlugin}, so
 * {@code RpgCommand.build}'s signature does not grow.
 *
 * <ul>
 *   <li>{@code start}: C is the block position the command runs at (the caller's, or {@code execute positioned}'s);
 *       the ring is at (C, 48) and the fight clock starts.</li>
 *   <li>{@code land <dx> <dz>}: the Mage lands at C + (dx, dz), blocks, on the standable block there, and is spawned;
 *       the reply names STARTED, HELD or REFUSED_*. {@code land random}: a uniformly random standable spot within the
 *       maximum landing distance.</li>
 *   <li>{@code mage-die}: the Mage is killed through the store.</li>
 *   <li>{@code probe}: one FIGHTPROBE line, the instrument's reading of the Mage and the crystals.</li>
 *   <li>{@code stop}: the fixture ends; nothing it made remains.</li>
 * </ul>
 */
public final class FightCommand {

    private FightCommand() {}

    public static LiteralCommandNode<CommandSourceStack> build(FightFixtures fixtures, boolean devServer) {
        return Commands.literal("fight")
                .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                .then(Commands.literal("start").executes(ctx -> reply(ctx, devServer,
                        p -> fixtures.start(p, ctx.getSource().getLocation()))))
                .then(Commands.literal("probe").executes(ctx -> reply(ctx, devServer, p -> fixtures.probe())))
                .then(Commands.literal("enter").executes(ctx -> reply(ctx, devServer, p -> fixtures.enter(p, true)))
                        .then(Commands.literal("nodragon").executes(ctx -> reply(ctx, devServer,
                                p -> fixtures.enter(p, false)))))
                .then(Commands.literal("dragon")
                        .then(Commands.literal("probe").executes(ctx -> reply(ctx, devServer, p -> fixtures.dragonProbe())))
                        .then(Commands.literal("kill").executes(ctx -> reply(ctx, devServer, p -> fixtures.dragonKill())))
                        .then(Commands.literal("hit")
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0, 1.0e7))
                                        .executes(ctx -> reply(ctx, devServer, p -> fixtures.dragonHit(p,
                                                DoubleArgumentType.getDouble(ctx, "amount"))))))
                        .then(Commands.literal("strike")
                                .then(Commands.argument("part", StringArgumentType.word())
                                        .executes(ctx -> reply(ctx, devServer, p -> fixtures.dragonStrike(p,
                                                StringArgumentType.getString(ctx, "part"))))))
                        .then(Commands.literal("phase")
                                .then(Commands.argument("phase", StringArgumentType.word())
                                        .executes(ctx -> reply(ctx, devServer, p -> fixtures.dragonPhase(
                                                StringArgumentType.getString(ctx, "phase"))))))
                        // F9b: stage the dragon's attacks (bite, wing, breath) on the sender.
                        .then(Commands.literal("touch")
                                .then(Commands.argument("part", StringArgumentType.word())
                                        .executes(ctx -> reply(ctx, devServer, p -> fixtures.dragonTouch(p,
                                                StringArgumentType.getString(ctx, "part"))))))
                        .then(Commands.literal("to-breath").executes(ctx -> reply(ctx, devServer,
                                fixtures::dragonToBreath)))
                        .then(Commands.literal("move")
                                .then(Commands.argument("dx", DoubleArgumentType.doubleArg(-64.0, 64.0))
                                        .then(Commands.argument("dy", DoubleArgumentType.doubleArg(0.0, 64.0))
                                                .then(Commands.argument("dz", DoubleArgumentType.doubleArg(-64.0, 64.0))
                                                        .executes(ctx -> reply(ctx, devServer, p -> fixtures.dragonMove(
                                                                DoubleArgumentType.getDouble(ctx, "dx"),
                                                                DoubleArgumentType.getDouble(ctx, "dy"),
                                                                DoubleArgumentType.getDouble(ctx, "dz")))))))))
                .then(Commands.literal("arena-probe").executes(ctx -> reply(ctx, devServer, p -> fixtures.arenaProbe())))
                .then(Commands.literal("leave").executes(ctx -> reply(ctx, devServer, fixtures::leave)))
                .then(Commands.literal("worlds").executes(ctx -> reply(ctx, devServer, fixtures::worldsLine)))
                .then(Commands.literal("play")
                        .then(Commands.argument("min", DoubleArgumentType.doubleArg(0.0, 36.0))
                                .executes(ctx -> reply(ctx, devServer,
                                        p -> fixtures.play(DoubleArgumentType.getDouble(ctx, "min"), true)))
                                .then(Commands.literal("unsafe").executes(ctx -> reply(ctx, devServer,
                                        p -> fixtures.play(DoubleArgumentType.getDouble(ctx, "min"), false))))))
                .then(Commands.literal("land")
                        .then(Commands.literal("random").executes(ctx -> reply(ctx, devServer,
                                p -> fixtures.landRandom())))
                        .then(Commands.argument("dx", DoubleArgumentType.doubleArg(-1000.0, 1000.0))
                                .then(Commands.argument("dz", DoubleArgumentType.doubleArg(-1000.0, 1000.0))
                                        .executes(ctx -> reply(ctx, devServer, p -> fixtures.land(
                                                DoubleArgumentType.getDouble(ctx, "dx"),
                                                DoubleArgumentType.getDouble(ctx, "dz")))))))
                .then(Commands.literal("mage-die").executes(ctx -> reply(ctx, devServer, p -> fixtures.mageDie())))
                .then(Commands.literal("stop").executes(ctx -> reply(ctx, devServer, p -> fixtures.stop("command"))))
                .build();
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, boolean devServer,
                             Function<Player, String> action) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }
        if (!devServer) {
            player.sendMessage(Component.text("The fight fixture runs only on a dev server (-Drpg.dev=true).",
                    NamedTextColor.RED));
            return 0;
        }
        player.sendMessage(Component.text(action.apply(player), NamedTextColor.GRAY));
        return 1;
    }
}
