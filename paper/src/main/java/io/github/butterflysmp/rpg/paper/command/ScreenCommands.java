package io.github.butterflysmp.rpg.paper.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import io.github.butterflysmp.rpg.core.weapon.ToolRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.menu.NexusScreens;
import io.github.butterflysmp.rpg.paper.menu.RecipeCatalogue;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.vault.VaultService;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One top-level command per Nexus screen -- {@code /level}, {@code /build}, {@code /gear}, {@code /vault},
 * {@code /anvil}, {@code /craft}, {@code /recipes}, {@code /enchanting}, {@code /grindstone},
 * {@code /settings} -- beside {@code /menu} (Ben, 2026-09-29: <i>"Names are good"</i>).
 *
 * <p>Top-level for the reason {@code /menu} is: reach. Registered as further nodes in the ONE
 * {@code LifecycleEvents.COMMANDS} handler in {@code RpgPlugin}, not a handler of their own. Each has its own
 * permission node, {@code default: true}.
 *
 * <p><b>THIS CLASS DECIDES NOTHING ABOUT A SCREEN.</b> The player and profile checks are {@code /menu}'s;
 * death and the level gate are {@link NexusScreens#refusal}'s, which asks the same gate the hub asks. A check
 * added here instead would be a second copy of a rule the hub owns.
 */
public final class ScreenCommands {

    private ScreenCommands() {}

    /** A node for every {@link NexusScreens.Screen}, in the enum's order. */
    public static List<LiteralCommandNode<CommandSourceStack>> build(
            AdapterContext adapters, ProfileService profiles, WeaponRegistry weapons,
            ResourcePool resources, RecipeCatalogue recipes, ShieldRegistry shields,
            ArmorRegistry armor, ToolRegistry tools, VaultService vaults) {
        List<LiteralCommandNode<CommandSourceStack>> nodes = new ArrayList<>();
        for (NexusScreens.Screen screen : NexusScreens.Screen.values()) {
            nodes.add(Commands.literal(screen.command())
                    .requires(source -> source.getSender().hasPermission(screen.permission()))
                    .executes(ctx -> {
                        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                            ctx.getSource().getSender().sendMessage(
                                    Component.text("Players only.", NamedTextColor.RED));
                            return 0;
                        }
                        Optional<PlayerProfile> profile = profiles.profile(player.getUniqueId());
                        if (profile.isEmpty()) {
                            player.sendMessage(RpgCommand.profileUnavailable(profiles, player));
                            return 0;
                        }
                        Optional<String> refused = NexusScreens.refusal(player, screen, profile.get());
                        if (refused.isPresent()) {
                            // GRAY, as the hub's refusal of a locked station is (NexusMenu.onClick).
                            player.sendMessage(Component.text(refused.get(), NamedTextColor.GRAY));
                            return 0;
                        }
                        NexusScreens.open(player, screen, adapters, profiles, weapons, resources, recipes,
                                shields, armor, tools, vaults);
                        return 1;
                    })
                    .build());
        }
        return List.copyOf(nodes);
    }
}
