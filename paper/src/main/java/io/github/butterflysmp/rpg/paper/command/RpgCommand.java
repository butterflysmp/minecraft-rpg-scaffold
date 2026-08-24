package io.github.butterflysmp.rpg.paper.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AttackSpeed;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.kit.KitDefinition;
import io.github.butterflysmp.rpg.core.kit.KitRegistry;
import io.github.butterflysmp.rpg.core.kit.WeaponGrant;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.PaperCombatWorld;
import io.github.butterflysmp.rpg.paper.content.ElementRegistry;
import io.github.butterflysmp.rpg.paper.health.HealthModifierItems;
import io.github.butterflysmp.rpg.paper.health.MobNameplateManager;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.weapon.AttackSpeedModifierItems;
import io.github.butterflysmp.rpg.paper.weapon.ClassDamageModifierItems;
import io.github.butterflysmp.rpg.paper.weapon.DashAim;
import io.github.butterflysmp.rpg.paper.weapon.WeaponClassLabel;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import io.github.butterflysmp.rpg.paper.weapon.WeaponRefresher;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Brigadier commands, registered through the plugin lifecycle manager.
 * NOT CommandExecutor. NOT onCommand. NOT plugin.yml commands: block.
 */
public final class RpgCommand {

    private RpgCommand() {}

    /** How far /rpg apply's aim-ray reaches for a mob. Named, and within the 20-30 block ask. */
    private static final double TARGET_RANGE = 25.0;
    /** Hitbox inflation for the aim-ray -- a forgiving crosshair, since this is a dev tool. */
    private static final double TARGET_LENIENCE = 0.3;

    public static LiteralCommandNode<CommandSourceStack> build(AbilityRegistry registry,
                                                               AbilityService abilityService,
                                                               AdapterContext adapters,
                                                               KitRegistry kits,
                                                               ElementRegistry elements,
                                                               ProfileService profiles,
                                                               WeaponRegistry weapons,
                                                               MobRegistry mobs,
                                                               MobNameplateManager nameplates) {
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
                        .then(Commands.argument("class", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    kits.classes().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "class");
                                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                        ctx.getSource().getSender().sendMessage(
                                                Component.text("Players only.", NamedTextColor.RED));
                                        return 0;
                                    }
                                    return chooseClass(player, id, kits, profiles, weapons, adapters);
                                })))
                .then(Commands.literal("element")
                        .requires(source -> source.getSender().hasPermission(Permissions.CLASS))
                        .then(Commands.argument("element", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    elements.all().forEach(e -> builder.suggest(e.id()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "element");
                                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                        ctx.getSource().getSender().sendMessage(
                                                Component.text("Players only.", NamedTextColor.RED));
                                        return 0;
                                    }
                                    return chooseElement(player, id, kits, elements, profiles, weapons, adapters);
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
                // Force a lore/display refresh of everything you are carrying, without relogging.
                // Join already does this; this is the same call for the case where reconnecting is
                // the slow way round. Dev-gated: it is a content-iteration instrument, and it
                // rewrites items in your inventory.
                .then(Commands.literal("refresh")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .executes(ctx -> {
                            if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                ctx.getSource().getSender().sendMessage(
                                        Component.text("Players only.", NamedTextColor.RED));
                                return 0;
                            }
                            return refresh(player, weapons, adapters);
                        }))
                // A dev instrument: apply any loaded status, at any stack count and duration,
                // to the mob you are aiming at -- bypassing the class/element/kit gate, which is
                // exactly why it is DEV-gated. It reuses the same applyStatus seam an ability
                // uses, so what it tests is what an ability triggers.
                .then(Commands.literal("apply")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .then(Commands.argument("status", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    adapters.statuses().all().forEach(s -> builder.suggest(s.id()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> apply(ctx, adapters, null, null))
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 12000))
                                        .executes(ctx -> apply(ctx, adapters,
                                                IntegerArgumentType.getInteger(ctx, "duration"), null))
                                        .then(Commands.argument("stacks", IntegerArgumentType.integer(1, ApplyArgs.MAX_STACKS))
                                                .executes(ctx -> apply(ctx, adapters,
                                                        IntegerArgumentType.getInteger(ctx, "duration"),
                                                        IntegerArgumentType.getInteger(ctx, "stacks")))))))
                // Dev instruments for the custom-health phase: drive the player's OWN custom HP so
                // the heart bar can be witnessed before the damage system (next phase) exists. They
                // mutate through CombatantStats -- the same observable path the popup hooks later --
                // never a side door that skips the seam. DEV-gated, like /rpg apply.
                .then(Commands.literal("damage")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                .executes(ctx -> damageSelf(ctx, adapters))))
                .then(Commands.literal("heal")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                .executes(ctx -> healSelf(ctx, adapters))))
                // Mint a health_boost_TEMP into your inventory to prove the equip/unequip modifier
                // lifecycle: hold it -> max rises (headroom), drop/clear/swap -> max falls (clamp).
                .then(Commands.literal("healthboost")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .executes(ctx -> healthBoost(ctx, adapters, null))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                .executes(ctx -> healthBoost(ctx, adapters,
                                        IntegerArgumentType.getInteger(ctx, "amount")))))
                // Spawn a custom mob: the mob analogue of /rpg give <weapon>. Dev-gated, because
                // spawning a 360-HP boss on demand is a test instrument, not a player verb.
                .then(Commands.literal("spawn")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .then(Commands.argument("mob", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    mobs.all().forEach(m -> builder.suggest(m.id()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "mob");
                                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                                        ctx.getSource().getSender().sendMessage(
                                                Component.text("Players only.", NamedTextColor.RED));
                                        return 0;
                                    }
                                    return spawnMob(player, id, mobs, adapters);
                                })))
                // Mint an attack_speed_boost_TEMP. The attack-speed stat bases at 1.0 and no content
                // grants a bonus yet, so without this the feature is invisible at boot: hold it and a
                // basic attack's cooldown scales (10 ticks -> 5 at +1.0), drop it and the cadence
                // returns. An ability's cooldown is deliberately unaffected.
                .then(Commands.literal("attackspeed")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .executes(ctx -> attackSpeedBoost(ctx, adapters, null))
                        .then(Commands.argument("bonus", DoubleArgumentType.doubleArg(0.0, 20.0))
                                .executes(ctx -> attackSpeedBoost(ctx, adapters,
                                        DoubleArgumentType.getDouble(ctx, "bonus")))))
                // Mint a class_damage_boost_TEMP. The class-damage stat bases at 0 and no content
                // grants it yet, so without this the feature is invisible at boot: hold a MATCHING
                // weapon and every direct damage effect gains the bonus (the staff's literal bolt
                // included), swap to another class and the same worn item goes inert.
                .then(Commands.literal("classdamage")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .then(Commands.argument("class", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (WeaponClass c : WeaponClass.values()) {
                                        builder.suggest(c.name().toLowerCase(Locale.ROOT));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> classDamageBoost(ctx, adapters,
                                        StringArgumentType.getString(ctx, "class"), null))
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(-20.0, 100.0))
                                        .executes(ctx -> classDamageBoost(ctx, adapters,
                                                StringArgumentType.getString(ctx, "class"),
                                                DoubleArgumentType.getDouble(ctx, "amount"))))))
                // Damage/heal the LOOKED-AT mob's custom health, through the same observable store path
                // the nameplate hooks -- so the mob nameplate's HP-change update is witnessable this
                // phase (the real damage system, which would drive mob HP for real, is a later phase).
                .then(Commands.literal("mobdamage")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                .executes(ctx -> mobMutate(ctx, adapters, nameplates, false))))
                .then(Commands.literal("mobheal")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                .executes(ctx -> mobMutate(ctx, adapters, nameplates, true))))
                .build();
    }

    /**
     * Damage or heal the mob the player is aiming at, on its custom health. Reuses the /rpg apply
     * aim-ray. DAMAGE routes through the REAL combat path -- {@code BukkitCombatant.applyDamage},
     * the same entry point abilities use -- so the command exercises it (flash, aggro, and the
     * {@code HealthChange} seam) exactly the way /rpg apply exercises {@code applyStatus}. HEAL stays
     * on {@code CombatantStats.heal} directly, because {@code applyHeal} is vanilla-only and would not
     * touch custom HP (a separate gap; see NEXT.md).
     *
     * Ensures the target is nameplated first via {@code onMobAppear} -- which is register-if-absent, so
     * re-calling it each cast leaves an existing plate (and its version) untouched. The seam always
     * fires, but {@code MobNameplateManager.onChange} no-ops for a mob that was never nameplated, so
     * without this the HP change would drain the store yet never reach the plate.
     */
    private static int mobMutate(CommandContext<CommandSourceStack> ctx, AdapterContext adapters,
                                 MobNameplateManager nameplates, boolean heal) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }
        double amount = IntegerArgumentType.getInteger(ctx, "amount");
        Location eye = player.getEyeLocation();
        adapters.scheduler().onRegion(eye, () -> {
            RayTraceResult hit = player.getWorld().rayTraceEntities(
                    eye, eye.getDirection(), TARGET_RANGE, TARGET_LENIENCE,
                    e -> e instanceof LivingEntity living && !(living instanceof Player));
            if (hit == null || !(hit.getHitEntity() instanceof LivingEntity target)) {
                player.sendMessage(Component.text("Look at a mob.", NamedTextColor.RED));
                return;
            }
            UUID id = target.getUniqueId();
            var maxAttr = target.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            double vanillaMax = maxAttr != null ? maxAttr.getValue() : target.getHealth();
            var atkAttr = target.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
            double vanillaAttack = atkAttr != null ? atkAttr.getValue() : 0.0;
            CombatantStats stats = adapters.stats();
            // Track AND nameplate the target (idempotent) so the seam fire actually reaches the plate.
            // Seed attack too for signature consistency with the melee path -- this dev command only
            // damages/heals HP, so it does not read the attack value, but the store shape is uniform.
            stats.bootstrapIfAbsent(id, vanillaMax, vanillaAttack, false);
            nameplates.onMobAppear(target);
            double displayCurrent;
            if (heal) {
                stats.heal(id, amount, player.getUniqueId(), true);   // seam directly; applyHeal is vanilla-only
                displayCurrent = stats.current(id);
            } else {
                // applyDamage defers to the next tick, so compute the expected result now for the
                // message -- an estimate (clamps at 0, as HealthState.damage does). The nameplate,
                // updated when applyDamage lands, is the source of truth.
                displayCurrent = Math.max(0.0, stats.current(id) - amount);
                BukkitCombatant.of(target, adapters).handle().applyDamage(amount, player.getUniqueId());
            }
            player.sendMessage(Component.text(
                    "%s %s: %.0f/%.0f custom HP".formatted(target.getType().name(),
                            heal ? "healed" : "damaged", displayCurrent, stats.max(id)),
                    NamedTextColor.GREEN));
        });
        return 1;
    }

    /** Damage the calling player's OWN custom health, through the observable store path. Heart bar follows. */
    private static int damageSelf(CommandContext<CommandSourceStack> ctx, AdapterContext adapters) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }
        double amount = IntegerArgumentType.getInteger(ctx, "amount");
        CombatantStats stats = adapters.stats();
        UUID id = player.getUniqueId();
        if (!stats.tracks(id)) stats.register(id, CombatantStats.DEFAULT_PLAYER_BASE, true);
        stats.damage(id, amount, id, true);
        player.sendMessage(Component.text("Custom HP: %.0f/%.0f".formatted(stats.current(id), stats.max(id)),
                NamedTextColor.GREEN));
        return 1;
    }

    /** Heal the calling player's OWN custom health (capped at max), through the observable store path. */
    private static int healSelf(CommandContext<CommandSourceStack> ctx, AdapterContext adapters) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }
        double amount = IntegerArgumentType.getInteger(ctx, "amount");
        CombatantStats stats = adapters.stats();
        UUID id = player.getUniqueId();
        if (!stats.tracks(id)) stats.register(id, CombatantStats.DEFAULT_PLAYER_BASE, true);
        stats.heal(id, amount, id, true);
        player.sendMessage(Component.text("Custom HP: %.0f/%.0f".formatted(stats.current(id), stats.max(id)),
                NamedTextColor.GREEN));
        return 1;
    }

    /** Mint a health_boost_TEMP (default +300) into the caller's inventory. */
    private static int healthBoost(CommandContext<CommandSourceStack> ctx, AdapterContext adapters, Integer amount) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }
        double boost = amount == null ? HealthModifierItems.DEFAULT_BOOST : amount;
        player.getInventory().addItem(HealthModifierItems.mint(adapters.keys(), boost));
        player.sendMessage(Component.text("Gave health_boost_TEMP (+" + (int) boost + "). Hold it to raise max HP.",
                NamedTextColor.GREEN));
        return 1;
    }

    /**
     * Spawn a custom mob at the caller's feet.
     *
     * The load-bearing detail is WHEN the tag is applied, not that it is. {@code EntityAddToWorldEvent}
     * fires as the entity enters the world and runs {@code MobNameplateManager.onMobAppear} ->
     * {@code seedCombatStats}, which is register-IF-ABSENT. Tag the entity after spawning and the mob
     * has already been seeded from its vanilla MAX_HEALTH; the tag then changes nothing, and the Knell
     * quietly has 20 HP with no error anywhere. So the PDC tag and the name are set inside the
     * PRE-SPAWN CONSUMER, which runs before the add event.
     *
     * {@code randomizeData: false} keeps a dev spawn deterministic -- no random equipment or variant --
     * so two spawns of the same mob are the same mob.
     *
     * The CustomName is the mob's IDENTITY only, never its HP: the health bar stays a per-viewer packet
     * override (see PacketNameplateSender). CustomNameVisible is false so vanilla does not float the
     * bare name alongside our nameplate -- death messages and /data read "Knell" either way.
     */
    private static int spawnMob(Player player, String mobId, MobRegistry mobs, AdapterContext adapters) {
        MobDefinition def = mobs.find(mobId).orElse(null);
        if (def == null) {
            player.sendMessage(Component.text("Unknown mob: " + mobId, NamedTextColor.RED));
            String available = String.join(", ", mobs.all().stream().map(MobDefinition::id).toList());
            player.sendMessage(Component.text("Available: " + available, NamedTextColor.GRAY));
            return 0;
        }

        EntityType type = Registry.ENTITY_TYPE.get(
                NamespacedKey.minecraft(def.baseEntity().toLowerCase(Locale.ROOT)));
        // Both already warned at boot by ContentValidator; refuse here rather than throw, so a bad
        // content file is a red chat line and not a stack trace in the command dispatcher.
        if (type == null || !type.isAlive()) {
            player.sendMessage(Component.text(
                    "Mob '" + def.id() + "' has base_entity '" + def.baseEntity()
                            + "', which is not a living entity. See the boot log.", NamedTextColor.RED));
            return 0;
        }

        Class<? extends LivingEntity> entityClass = type.getEntityClass().asSubclass(LivingEntity.class);
        Component name = MiniMessage.miniMessage().deserialize(def.displayName());

        LivingEntity spawned = player.getWorld().spawn(
                player.getLocation(), entityClass, CreatureSpawnEvent.SpawnReason.CUSTOM, false,
                entity -> {
                    // BEFORE the add event -- see the javadoc above. Order is the whole trick.
                    entity.getPersistentDataContainer()
                            .set(adapters.keys().mobId, PersistentDataType.STRING, def.id());
                    entity.customName(name);
                    entity.setCustomNameVisible(false);
                });

        player.sendMessage(Component.text("Spawned ", NamedTextColor.AQUA)
                .append(name)
                .append(Component.text(" (" + def.baseEntity() + ", "
                        + Math.round(def.maxHealth()) + " HP)", NamedTextColor.GRAY)));
        return spawned != null ? 1 : 0;
    }


    /**
     * Mint a class_damage_boost_TEMP (default +5) for a named class into the caller's inventory.
     *
     * The amount range allows NEGATIVES deliberately: a "-N <Class> Damage" curse is the case the
     * amount>0 guard on both damage arms exists for, and a dev item is the only way to witness it
     * before any content authors one.
     */
    private static int classDamageBoost(CommandContext<CommandSourceStack> ctx, AdapterContext adapters,
                                        String className, Double amount) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }
        WeaponClass weaponClass = WeaponClass.fromName(className);
        if (weaponClass == null) {
            player.sendMessage(Component.text(
                    "Unknown class '" + className + "'. Try: melee, ranger, mage.", NamedTextColor.RED));
            return 0;
        }
        double bonus = amount == null ? ClassDamageModifierItems.DEFAULT_BOOST : amount;
        player.getInventory().addItem(ClassDamageModifierItems.mint(adapters.keys(), weaponClass, bonus));
        player.sendMessage(Component.text(
                "Gave class_damage_boost_TEMP (+" + bonus + " " + WeaponClassLabel.of(weaponClass)
                        + " Damage). Put it in your OFFHAND -- the main hand holds the weapon -- then"
                        + " hit something with a " + WeaponClassLabel.of(weaponClass) + " weapon.",
                NamedTextColor.GREEN));
        return 1;
    }
    /**
     * Mint an attack_speed_boost_TEMP (default +1.0, i.e. a resolved 2.0) into the caller's inventory.
     * The amount is the BONUS on a base of 1.0, not the multiplier itself.
     */
    private static int attackSpeedBoost(CommandContext<CommandSourceStack> ctx, AdapterContext adapters,
                                        Double bonus) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }
        double amount = bonus == null ? AttackSpeedModifierItems.DEFAULT_BOOST : bonus;
        player.getInventory().addItem(AttackSpeedModifierItems.mint(adapters.keys(), amount));
        player.sendMessage(Component.text(
                "Gave attack_speed_boost_TEMP (+" + amount + " -> " + (AttackSpeed.BASE + amount)
                        + "x). Hold it and swing a basic attack.", NamedTextColor.GREEN));
        return 1;
    }

    /**
     * Apply a status to the mob the player is aiming at. Reuses BukkitCombatant.applyStatus --
     * the exact path an ability's onHit takes -- so /rpg apply is a faithful test instrument.
     * `stacks` is the number of applyStatus calls, because Soaked (and any future stacking
     * status) accumulates by repeated application, not by an amplifier field.
     */
    private static int apply(CommandContext<CommandSourceStack> ctx, AdapterContext adapters,
                             Integer duration, Integer stacks) {
        String statusId = StringArgumentType.getString(ctx, "status");
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }

        ApplyArgs.Resolution resolution = ApplyArgs.resolve(statusId, duration, stacks,
                id -> adapters.statuses().find(id).isPresent());
        if (!resolution.ok()) {
            player.sendMessage(Component.text(resolution.error(), NamedTextColor.RED));
            return 0;
        }
        ApplyArgs args = resolution.args();

        // Resolve the target and apply on the thread owning the aim -- rayTraceEntities is a
        // world read. Entity-only trace (no block occlusion): a dev tool should hit the mob the
        // crosshair is roughly on, even through a fence. Mob-only: skip players, self, non-living.
        Location eye = player.getEyeLocation();
        adapters.scheduler().onRegion(eye, () -> {
            RayTraceResult hit = player.getWorld().rayTraceEntities(
                    eye, eye.getDirection(), TARGET_RANGE, TARGET_LENIENCE,
                    e -> e instanceof LivingEntity living && !(living instanceof Player));
            if (hit == null || !(hit.getHitEntity() instanceof LivingEntity target)) {
                player.sendMessage(Component.text("Look at a mob to apply a status.", NamedTextColor.RED));
                return;
            }
            var handle = BukkitCombatant.of(target, adapters).handle();
            for (int i = 0; i < args.stacks(); i++) {
                handle.applyStatus(args.statusId(), args.durationTicks(), 0);
            }
            player.sendMessage(Component.text(
                    "Applied " + args.statusId() + " x" + args.stacks() + " (" + args.durationTicks()
                            + "t) to " + target.getType().name(), NamedTextColor.GREEN));
        });
        return 1;
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

        player.getInventory().addItem(WeaponItems.mint(weapon, adapters));
        // Same rarity-coloured name the item itself carries -- the chat echo must not disagree
        // with the thing that just landed in the inventory.
        player.sendMessage(Component.text("Given ", NamedTextColor.AQUA)
                .append(WeaponItems.displayName(weapon.displayName(), weapon.rarity())));
        return 1;
    }

    /**
     * Rebuild the display of every custom weapon the player is carrying, from the content loaded
     * now. The same call the join handler makes; this is for iterating on content without a relog.
     *
     * Hops to the player's own thread before touching their inventory. A command runs on the
     * command thread, not the player's region thread, so once Folia is on, scanning an inventory
     * inline here is a cross-region read. (/rpg give has the same gap today -- not this pass's.)
     *
     * Reports the COUNT rather than "done". A scan that finds nothing and a scan that silently did
     * nothing are indistinguishable from a success message, and "finding zero items is a defect,
     * not a quiet no-op" is exactly the trap CLAUDE.md's verification section names. The number is
     * how the boot gate can tell the difference.
     */
    private static int refresh(Player player, WeaponRegistry weapons, AdapterContext adapters) {
        adapters.scheduler().onEntity(player, () -> {
            int refreshed = WeaponRefresher.refresh(player, weapons, adapters);
            // Make a mid-session swap visible immediately rather than at the client's next sync.
            player.updateInventory();
            player.sendMessage(refreshed > 0
                    ? Component.text("Refreshed " + refreshed + " weapon(s) from current content.",
                            NamedTextColor.GREEN)
                    : Component.text("Refreshed 0 weapons -- you are carrying none of ours.",
                            NamedTextColor.YELLOW));
        });
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
        CombatantSnapshot caster = BukkitCombatant.snapshot(player, adapters.stats());

        // Decide INLINE. cast() reads no world state, and consuming the cooldown
        // and energy here -- rather than inside the region hop below -- is what
        // stops a player spamming the command faster than the hop resolves.
        AbilityService.CastResult result = abilityService.cast(caster, abilityId, aim, castable);

        switch (result) {
            case AbilityService.CastResult.Success success -> {
                // A dash steers by WASD, not the look-aim above -- resolve it HERE, on the
                // caster's own thread before the hop, because getCurrentInput() is player
                // state. Every other cast passes through unchanged.
                AbilityService.CastResult.Success toRun = DashAim.resolve(player, success);
                // Resolve and apply on the thread that owns the aim's origin.
                // Everything past this point reads the world: castRay and
                // combatantsNear are illegal anywhere else.
                adapters.scheduler().onRegion(eye, () ->
                        new CastExecutor(new PaperCombatWorld(player.getWorld(), adapters))
                                .execute(toRun));

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
                // A half-chosen player and a wrong-kit player fail the same gate but want
                // different advice: one still owes an axis, the other's kit lacks the ability.
                if (!chosen(profile.archetypeId()) || !chosen(profile.elementId())) {
                    player.sendMessage(Component.text(
                            "Choose a class and an element: /rpg class <class> and /rpg element <element>.",
                            NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text(
                            "Your kit has not unlocked " + locked.id() + ".", NamedTextColor.YELLOW));
                }
                return 0;
            }
        }
    }

    /** An axis is chosen once it is anything but the NONE sentinel. */
    private static boolean chosen(String axis) {
        return !PlayerProfile.NONE.equals(axis);
    }

    /** Set the class axis; the element is carried unchanged, and the kit is re-resolved. */
    private static int chooseClass(Player player, String classId, KitRegistry kits,
                                   ProfileService profiles, WeaponRegistry weapons, AdapterContext adapters) {
        PlayerProfile profile = profiles.profile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            player.sendMessage(Component.text("Your profile is still loading -- try again in a moment.",
                    NamedTextColor.GRAY));
            return 0;
        }
        if (!kits.classes().contains(classId)) {
            player.sendMessage(Component.text("Unknown class: " + classId, NamedTextColor.RED));
            player.sendMessage(Component.text("Available: " + String.join(", ", kits.classes()),
                    NamedTextColor.GRAY));
            return 0;
        }
        return applyKit(player, classId, profile.elementId(), kits, profiles, weapons, adapters);
    }

    /** Set the element axis; the class is carried unchanged, and the kit is re-resolved. */
    private static int chooseElement(Player player, String elementId, KitRegistry kits,
                                     ElementRegistry elements, ProfileService profiles,
                                     WeaponRegistry weapons, AdapterContext adapters) {
        PlayerProfile profile = profiles.profile(player.getUniqueId()).orElse(null);
        if (profile == null) {
            player.sendMessage(Component.text("Your profile is still loading -- try again in a moment.",
                    NamedTextColor.GRAY));
            return 0;
        }
        if (elements.find(elementId).isEmpty()) {
            player.sendMessage(Component.text("Unknown element: " + elementId, NamedTextColor.RED));
            String available = String.join(", ",
                    elements.all().stream().map(io.github.butterflysmp.rpg.paper.content.ElementDefinition::id).toList());
            player.sendMessage(Component.text("Available: " + available, NamedTextColor.GRAY));
            return 0;
        }
        return applyKit(player, profile.archetypeId(), elementId, kits, profiles, weapons, adapters);
    }

    /**
     * Set (class, element) together and grant what the pair resolves to -- FAIL CLOSED, with the
     * precedence: half-selected -> "pick both"; both set but no authored kit -> "not available
     * yet"; a real kit -> unlock its abilities and mint its weapons. Abilities are always
     * re-derived from the new pair (empty when incomplete or unauthored), so a stale class's
     * grants cannot outlive a class change -- and the gate refuses casting either way.
     */
    private static int applyKit(Player player, String classId, String elementId, KitRegistry kits,
                                ProfileService profiles, WeaponRegistry weapons, AdapterContext adapters) {
        boolean complete = chosen(classId) && chosen(elementId);
        KitDefinition kit = complete ? kits.find(classId, elementId).orElse(null) : null;
        List<String> abilities = kit == null ? List.of() : kit.abilityIds();

        boolean set = profiles.setKit(player.getUniqueId(), classId, elementId, abilities);
        if (!set) {
            player.sendMessage(Component.text("Your profile is still loading -- try again in a moment.",
                    NamedTextColor.GRAY));
            return 0;
        }

        if (!complete) {
            player.sendMessage(Component.text(
                    "Choose a class and an element: /rpg class <class> and /rpg element <element>.",
                    NamedTextColor.YELLOW));
            return 1;
        }
        if (kit == null) {
            player.sendMessage(Component.text(
                    "The " + classId + " / " + elementId + " combination isn't available yet.",
                    NamedTextColor.YELLOW));
            return 1;
        }

        grantWeapons(player, kit, weapons, adapters);
        player.sendMessage(Component.text("You are now ", NamedTextColor.AQUA)
                .append(MiniMessage.miniMessage().deserialize(kit.displayName())));
        if (!kit.abilityIds().isEmpty()) {
            player.sendMessage(Component.text("Unlocked: " + String.join(", ", kit.abilityIds()),
                    NamedTextColor.GRAY));
        }
        return 1;
    }

    /**
     * Mint a kit's weapons. The equip weapon goes into a free hotbar slot and is selected, so a
     * fresh player has it in hand and the class is playable at once; the rest go to inventory. A
     * dangling weapon (already warned at boot) is skipped rather than crashing the grant. Never
     * overwrites a held item -- a full hotbar falls back to inventory, and a full inventory says so.
     */
    private static void grantWeapons(Player player, KitDefinition kit, WeaponRegistry weapons,
                                     AdapterContext adapters) {
        List<String> given = new ArrayList<>();
        for (WeaponGrant grant : kit.weapons()) {
            WeaponDefinition weapon = weapons.find(grant.weaponId()).orElse(null);
            if (weapon == null) continue; // validated at boot; skip a dangling grant
            ItemStack item = WeaponItems.mint(weapon, adapters);

            int hotbar = grant.equip() ? firstEmptyHotbarSlot(player) : -1;
            if (hotbar >= 0) {
                player.getInventory().setItem(hotbar, item);
                player.getInventory().setHeldItemSlot(hotbar);
            } else if (player.getInventory().firstEmpty() >= 0) {
                player.getInventory().addItem(item);
            } else {
                player.sendMessage(Component.text(
                        "Inventory full -- couldn't give you " + weapon.id() + ".", NamedTextColor.YELLOW));
                continue;
            }
            given.add(weapon.id());
        }
        if (!given.isEmpty()) {
            player.sendMessage(Component.text("Given: " + String.join(", ", given), NamedTextColor.GRAY));
        }
    }

    /** The first empty hotbar slot (0-8), or -1 if the hotbar is full. */
    private static int firstEmptyHotbarSlot(Player player) {
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack existing = player.getInventory().getItem(slot);
            if (existing == null || existing.getType().isAir()) return slot;
        }
        return -1;
    }

    private static Vec3 toVec3(Location location) {
        return new Vec3(location.getX(), location.getY(), location.getZ());
    }

    private static Vec3 toVec3(Vector vector) {
        return new Vec3(vector.getX(), vector.getY(), vector.getZ());
    }
}
