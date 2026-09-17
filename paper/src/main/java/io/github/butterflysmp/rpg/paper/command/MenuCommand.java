package io.github.butterflysmp.rpg.paper.command;

import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import io.github.butterflysmp.rpg.core.weapon.ToolRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.menu.NexusMenu;
import io.github.butterflysmp.rpg.paper.menu.RecipeCatalogue;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import com.mojang.brigadier.tree.LiteralCommandNode;

/**
 * {@code /menu} — opens the Nexus hub, exactly as the star does.
 *
 * <h2>*** NO "rpg" PREFIX, AND NOT OP-GATED. BOTH ARE BEN'S RULINGS. ***</h2>
 *
 * <b>It is {@code /menu}, for REACH.</b> Everything else this plugin offers a player lives under
 * {@code /rpg}; this one does not, because it is the door to all of it and a player who has to know
 * the prefix to find the door has not been given a door.
 *
 * <p><b>{@code Permissions.MENU} is declared {@code default: true}</b>, so it is granted to
 * everyone -- the same treatment {@code cast}, {@code class} and {@code stats} get. It is declared
 * rather than left off {@code requires()} entirely so that <b>an admin can revoke it</b>: an
 * undeclared node cannot be taken away, only worked around.
 *
 * <h2>*** WHY THIS EXISTS IN THE SAME SLICE AS THE TOGGLE: THE TOGGLE DELETES THE STAR ***</h2>
 *
 * The Nexus settings screen is about to grow a switch that <b>removes the star from the player's
 * inventory and turns the lock off</b>. Without a second route to the hub, <b>that switch is a
 * one-way door</b> -- a player turns the star off, and the screen that would let them turn it back
 * on is the screen they can no longer reach.
 *
 * <p><b>So this command is not a convenience and the ordering is not cosmetic.</b> Ben ruled the
 * two ship together, and {@code GATE-nexus.md}'s SLICE 10 block reads {@code /menu} WITH THE STAR
 * DISABLED as the row the whole ordering exists for.
 *
 * <h2>A PROFILE THAT HAS NOT LOADED GETS ROW 28's MESSAGE, NOT A DEGRADED HUB</h2>
 *
 * {@link NexusMenu} tolerates an absent profile -- it reads level 1 and renders no progression
 * lines -- so this command COULD open regardless. <b>It does not.</b> A hub opened mid-load shows
 * a level-40 player three locked stations and no XP figures, which is a screen that looks broken
 * rather than a screen that is waiting.
 *
 * <p>{@link RpgCommand#profileUnavailable} turns the distinction into a sentence, and it is shared
 * rather than copied <b>precisely so this command and the settings screen cannot come to answer the
 * same question differently</b> -- which is the argument that method already carries, now with a
 * second caller.
 */
public final class MenuCommand {

    private MenuCommand() {}

    /**
     * Build the {@code /menu} node.
     *
     * <p><b>Nine services, threaded rather than reached for</b>, because {@link NexusMenu} is the
     * one screen that carries the full set -- see its constructor's javadoc on why the bundle is
     * not owed. This command is a second door onto that same screen and takes the same key.
     *
     * <p><b>Registered in {@code RpgPlugin}'s ONE {@code LifecycleEvents.COMMANDS} handler</b>,
     * beside {@code RpgCommand}. A second handler would be the sprawl the banned-patterns table
     * names; a second node inside the one handler is not.
     */
    public static LiteralCommandNode<CommandSourceStack> build(
            AdapterContext adapters, ProfileService profiles, WeaponRegistry weapons,
            ResourcePool resources, RecipeCatalogue recipes, ShieldRegistry shields,
            ArmorRegistry armor, ToolRegistry tools) {

        return Commands.literal("menu")
                .requires(source -> source.getSender().hasPermission(Permissions.MENU))
                .executes(ctx -> {
                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                        ctx.getSource().getSender().sendMessage(
                                Component.text("Players only.", NamedTextColor.RED));
                        return 0;
                    }
                    // THE PROFILE GATE. Empty means still loading OR unreadable, and those need
                    // different sentences -- "try again in a moment" is a lie for the second.
                    if (profiles.profile(player.getUniqueId()).isEmpty()) {
                        player.sendMessage(RpgCommand.profileUnavailable(profiles, player));
                        return 0;
                    }
                    // NO SCHEDULER HOP. Unlike a menu opened FROM another menu, nothing is closing
                    // first -- Menu.open's tick-hop rule is about not racing an inventory close,
                    // and a command is not inside one.
                    new NexusMenu(player, adapters, profiles, weapons, resources, recipes,
                            shields, armor, tools).open();
                    return 1;
                })
                .build();
    }
}
