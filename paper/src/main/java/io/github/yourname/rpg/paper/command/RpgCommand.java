package io.github.yourname.rpg.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.yourname.rpg.core.Vec3;
import io.github.yourname.rpg.core.ability.AbilityRegistry;
import io.github.yourname.rpg.core.ability.AbilityService;
import io.github.yourname.rpg.core.ability.CastExecutor;
import io.github.yourname.rpg.core.combat.Aim;
import io.github.yourname.rpg.core.combat.CombatantSnapshot;
import io.github.yourname.rpg.paper.adapter.AdapterContext;
import io.github.yourname.rpg.paper.adapter.BukkitCombatant;
import io.github.yourname.rpg.paper.adapter.PaperCombatWorld;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Brigadier commands, registered through the plugin lifecycle manager.
 * NOT CommandExecutor. NOT onCommand. NOT plugin.yml commands: block.
 */
public final class RpgCommand {

    private RpgCommand() {}

    public static LiteralCommandNode<CommandSourceStack> build(AbilityRegistry registry,
                                                               AbilityService abilityService,
                                                               AdapterContext adapters) {
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
                                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                        ctx.getSource().getSender().sendMessage(
                                                Component.text("Players only.", NamedTextColor.RED));
                                        return 0;
                                    }
                                    return cast(player, id, abilityService, adapters);
                                })))
                .build();
    }

    private static int cast(Player player, String abilityId, AbilityService abilityService,
                            AdapterContext adapters) {

        Location eye = player.getEyeLocation();
        Aim aim = new Aim(toVec3(eye), toVec3(eye.getDirection()));

        // Photograph the caster HERE, on the caster's own thread, before the hop below.
        // Taken after the hop it would be the same cross-region read wearing a new type --
        // and on Paper, where both sides of the hop are the main thread, no test could
        // tell. BukkitCombatant.snapshot enforces the thread; this ordering does not.
        CombatantSnapshot caster = BukkitCombatant.snapshot(player, adapters);

        // Decide INLINE. cast() reads no world state, and consuming the cooldown
        // and energy here -- rather than inside the region hop below -- is what
        // stops a player spamming the command faster than the hop resolves.
        AbilityService.CastResult result = abilityService.cast(caster, abilityId, aim);

        switch (result) {
            case AbilityService.CastResult.Success success -> {
                // Resolve and apply on the thread that owns the aim's origin.
                // Everything past this point reads the world: castRay and
                // combatantsNear are illegal anywhere else.
                adapters.scheduler().onRegion(eye, () ->
                        new CastExecutor(new PaperCombatWorld(player.getWorld(), adapters))
                                .execute(success));

                player.sendMessage(Component.text("Cast ", NamedTextColor.AQUA)
                        .append(MiniMessage.miniMessage().deserialize(success.ability().displayName())));
                return 1;
            }
            case AbilityService.CastResult.OnCooldown onCooldown -> {
                player.sendMessage(Component.text(
                        "On cooldown for %.1fs".formatted(onCooldown.ticksRemaining() / 20.0),
                        NamedTextColor.GRAY));
                return 0;
            }
            case AbilityService.CastResult.InsufficientResource lacking -> {
                player.sendMessage(Component.text(
                        "Not enough %s: %.0f needed, %.0f available".formatted(
                                lacking.resourceId(), lacking.required(), lacking.available()),
                        NamedTextColor.GRAY));
                return 0;
            }
            case AbilityService.CastResult.UnknownAbility unknown -> {
                player.sendMessage(Component.text("Unknown ability: " + unknown.id(),
                        NamedTextColor.RED));
                return 0;
            }
        }
    }

    private static Vec3 toVec3(Location location) {
        return new Vec3(location.getX(), location.getY(), location.getZ());
    }

    private static Vec3 toVec3(Vector vector) {
        return new Vec3(vector.getX(), vector.getY(), vector.getZ());
    }
}
