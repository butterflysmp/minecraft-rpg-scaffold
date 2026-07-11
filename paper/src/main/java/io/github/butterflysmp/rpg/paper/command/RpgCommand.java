package io.github.butterflysmp.rpg.paper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.archetype.Archetype;
import io.github.butterflysmp.rpg.core.archetype.ArchetypeRegistry;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.PaperCombatWorld;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Set;

/**
 * Brigadier commands, registered through the plugin lifecycle manager.
 * NOT CommandExecutor. NOT onCommand. NOT plugin.yml commands: block.
 */
public final class RpgCommand {

    private RpgCommand() {}

    public static LiteralCommandNode<CommandSourceStack> build(AbilityRegistry registry,
                                                               AbilityService abilityService,
                                                               AdapterContext adapters,
                                                               ArchetypeRegistry archetypes,
                                                               ProfileService profiles,
                                                               WeaponRegistry weapons) {
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
                                // Suggest only what this caster can actually cast -- their
                                // class's grants. A list that offers abilities that answer
                                // "you have not unlocked that" is worse than no list.
                                .suggests((ctx, builder) -> {
                                    if (ctx.getSource().getExecutor() instanceof Player player) {
                                        profiles.profile(player.getUniqueId()).ifPresent(profile ->
                                                profile.unlockedAbilities().forEach(builder::suggest));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "ability");
                                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                        ctx.getSource().getSender().sendMessage(
                                                Component.text("Players only.", NamedTextColor.RED));
                                        return 0;
                                    }
                                    return cast(player, id, abilityService, adapters, profiles);
                                })))
                .then(Commands.literal("class")
                        .requires(source -> source.getSender().hasPermission(Permissions.CLASS))
                        .then(Commands.argument("archetype", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    archetypes.all().forEach(a -> builder.suggest(a.id()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "archetype");
                                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                        ctx.getSource().getSender().sendMessage(
                                                Component.text("Players only.", NamedTextColor.RED));
                                        return 0;
                                    }
                                    return chooseClass(player, id, archetypes, profiles);
                                })))
                .then(Commands.literal("give")
                        .requires(source -> source.getSender().hasPermission(Permissions.GIVE))
                        .then(Commands.argument("weapon", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    weapons.all().forEach(w -> builder.suggest(w.id()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "weapon");
                                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                        ctx.getSource().getSender().sendMessage(
                                                Component.text("Players only.", NamedTextColor.RED));
                                        return 0;
                                    }
                                    return give(player, id, weapons, adapters);
                                })))
                .build();
    }

    private static int give(Player player, String weaponId, WeaponRegistry weapons,
                            AdapterContext adapters) {
        WeaponDefinition weapon = weapons.find(weaponId).orElse(null);
        if (weapon == null) {
            player.sendMessage(Component.text("Unknown weapon: " + weaponId, NamedTextColor.RED));
            String available = String.join(", ", weapons.all().stream().map(WeaponDefinition::id).toList());
            player.sendMessage(Component.text("Available: " + available, NamedTextColor.GRAY));
            return 0;
        }

        // First free slot, and if there is none, tell them -- never drop the item on the
        // ground, which is how a "give" silently becomes a "litter the floor".
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Component.text("Your inventory is full -- make room and try again.",
                    NamedTextColor.YELLOW));
            return 0;
        }

        player.getInventory().addItem(WeaponItems.mint(weapon, adapters.keys()));
        player.sendMessage(Component.text("Given ", NamedTextColor.AQUA)
                .append(MiniMessage.miniMessage().deserialize(weapon.displayName())));
        return 1;
    }


    private static int cast(Player player, String abilityId, AbilityService abilityService,
                            AdapterContext adapters, ProfileService profiles) {

        // The gate's input: the abilities this caster's class grants. If the profile is
        // not loaded yet we cannot know it, so we refuse rather than guess -- casting is
        // not urgent enough to risk letting an unloaded player through.
        PlayerProfile profile = profiles.profile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            player.sendMessage(Component.text("Your profile is still loading -- try again in a moment.",
                    NamedTextColor.GRAY));
            return 0;
        }
        Set<String> castable = Set.copyOf(profile.unlockedAbilities());

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
        AbilityService.CastResult result = abilityService.cast(caster, abilityId, aim, castable);

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
            case AbilityService.CastResult.Locked locked -> {
                // A classless player and a wrong-class player fail the same gate but
                // want different advice: one needs to pick a class, the other has one.
                if (profile.archetypeId().equals("none")) {
                    player.sendMessage(Component.text(
                            "You have no class. Pick one: /rpg class <name>", NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text(
                            "Your class has not unlocked " + locked.id() + ".", NamedTextColor.YELLOW));
                }
                return 0;
            }
        }
    }

    private static int chooseClass(Player player, String archetypeId,
                                   ArchetypeRegistry archetypes, ProfileService profiles) {
        Archetype archetype = archetypes.find(archetypeId).orElse(null);
        if (archetype == null) {
            player.sendMessage(Component.text("Unknown class: " + archetypeId, NamedTextColor.RED));
            String available = String.join(", ", archetypes.all().stream().map(Archetype::id).toList());
            player.sendMessage(Component.text("Available: " + available, NamedTextColor.GRAY));
            return 0;
        }

        // setArchetype resolves the class -> granted-abilities set and persists it. It
        // returns false only when the profile has not finished loading, which is the
        // one case we cannot proceed through.
        boolean set = profiles.setArchetype(player.getUniqueId(), archetype.id(), archetype.abilityIds());
        if (!set) {
            player.sendMessage(Component.text("Your profile is still loading -- try again in a moment.",
                    NamedTextColor.GRAY));
            return 0;
        }

        player.sendMessage(Component.text("You are now ", NamedTextColor.AQUA)
                .append(MiniMessage.miniMessage().deserialize(archetype.displayName())));
        player.sendMessage(Component.text("Unlocked: " + String.join(", ", archetype.abilityIds()),
                NamedTextColor.GRAY));
        return 1;
    }

    private static Vec3 toVec3(Location location) {
        return new Vec3(location.getX(), location.getY(), location.getZ());
    }

    private static Vec3 toVec3(Vector vector) {
        return new Vec3(vector.getX(), vector.getY(), vector.getZ());
    }
}
