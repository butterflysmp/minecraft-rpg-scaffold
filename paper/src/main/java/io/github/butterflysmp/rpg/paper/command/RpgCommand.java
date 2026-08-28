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
import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.kit.KitDefinition;
import io.github.butterflysmp.rpg.core.kit.KitRegistry;
import io.github.butterflysmp.rpg.core.kit.WeaponGrant;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.weapon.Durability;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.enchant.EnchantLoreLines;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.PaperCombatWorld;
import io.github.butterflysmp.rpg.paper.content.ElementRegistry;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.health.CritModifierItems;
import io.github.butterflysmp.rpg.paper.health.HealthModifierItems;
import io.github.butterflysmp.rpg.paper.health.MobNameplateManager;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.weapon.AttackSpeedModifierItems;
import io.github.butterflysmp.rpg.paper.weapon.ClassDamageModifierItems;
import io.github.butterflysmp.rpg.paper.weapon.DashAim;
import io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine;
import io.github.butterflysmp.rpg.paper.weapon.EnchantItems;
import io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems;
import io.github.butterflysmp.rpg.paper.weapon.WeaponClassLabel;
import io.github.butterflysmp.rpg.paper.weapon.WeaponDurability;
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
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
                                                               MobNameplateManager nameplates,
                                                               ResourcePool resources) {
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
                // Dev tooling, not a game mechanic: refill the caller's mana so testing a costed
                // trigger does not mean waiting out the 60-second regen between casts.
                .then(Commands.literal("mana")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .then(Commands.literal("refill")
                                .executes(ctx -> manaRefill(ctx, resources))))
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
                // Durability dev instruments. /rpg repair stands in for a real repair economy
                // (anvil UI, materials); /rpg durability is the WEAR SOURCE this pass needs, since
                // nothing wears items in play yet -- auto-wear is Pass 2's balance question. It is
                // also what finally makes #12's deferred step-9 clamp boot-witnessable.
                //
                // The <amount> bounds are a correctness guard, not tidiness. Durability.wear is
                // overflow-hardened by its long widening; Durability.repair is NOT, and a negative
                // amount there is current - (-n) == current + n, which overflows int and lands on a
                // full repair. Bounding the arg keeps that guard out of the pure kernel. `set`
                // takes integer(0) because 0 is meaningful there -- it IS fully repaired.
                .then(Commands.literal("repair")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .executes(ctx -> durability(ctx, adapters, DurabilityOp.SET, 0)))
                .then(Commands.literal("durability")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .then(Commands.literal("damage")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> durability(ctx, adapters, DurabilityOp.DAMAGE,
                                                IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(Commands.literal("repair")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> durability(ctx, adapters, DurabilityOp.REPAIR,
                                                IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> durability(ctx, adapters, DurabilityOp.SET,
                                                IntegerArgumentType.getInteger(ctx, "amount"))))))
                // The enchant dev instrument. This pass's /rpg durability: the per-instance ROLL
                // and the enchant TABLE that will eventually put enchants on items do not exist
                // yet, so this stands in for both, exactly as /rpg durability stood in for auto-wear.
                //
                // <slot> and <candidate> are bounded HERE and deliberately NOT in EnchantState:
                // fixed-3-versus-rolled-1-3 is the roster pass's decision, so the kernel stays
                // uncapped and the provisional limit sits at the reachable surface. <level> takes
                // its bound from the core constant, so the command and the model cannot drift.
                //
                // Every branch re-mints the item rather than patching its lore. That is what makes
                // the enchant block impossible to double, and it means every use of this command
                // exercises the carry-forward -- the invariant this whole pass exists to protect
                // gets hammered on every edit instead of being checked once at login.
                .then(Commands.literal("enchant")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .then(Commands.literal("show")
                                .executes(ctx -> enchant(ctx, adapters, weapons, EnchantOp.SHOW, 0, 0, 0, null)))
                        .then(Commands.literal("clear")
                                .executes(ctx -> enchant(ctx, adapters, weapons, EnchantOp.CLEAR, 0, 0, 0, null)))
                        .then(Commands.literal("candidate")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(0, MAX_DEV_SLOT))
                                        .then(Commands.argument("enchant", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    adapters.enchants().all().forEach(e -> builder.suggest(e.id()));
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> enchant(ctx, adapters, weapons, EnchantOp.CANDIDATE,
                                                        IntegerArgumentType.getInteger(ctx, "slot"), 0, 0,
                                                        StringArgumentType.getString(ctx, "enchant"))))))
                        .then(Commands.literal("level")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(0, MAX_DEV_SLOT))
                                        .then(Commands.argument("candidate", IntegerArgumentType.integer(0, MAX_DEV_SLOT))
                                                .then(Commands.argument("level", IntegerArgumentType.integer(0, EnchantState.MAX_LEVEL))
                                                        .executes(ctx -> enchant(ctx, adapters, weapons, EnchantOp.LEVEL,
                                                                IntegerArgumentType.getInteger(ctx, "slot"),
                                                                IntegerArgumentType.getInteger(ctx, "candidate"),
                                                                IntegerArgumentType.getInteger(ctx, "level"), null))))))
                        .then(Commands.literal("active")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(0, MAX_DEV_SLOT))
                                        .then(Commands.argument("candidate", IntegerArgumentType.integer(0, MAX_DEV_SLOT))
                                                .executes(ctx -> enchant(ctx, adapters, weapons, EnchantOp.ACTIVE,
                                                        IntegerArgumentType.getInteger(ctx, "slot"),
                                                        IntegerArgumentType.getInteger(ctx, "candidate"), 0, null)))))
                        // Its own literal rather than `active <slot> none`: closed arity, no string
                        // parsing, and no "none" that could collide with an enchant id.
                        .then(Commands.literal("deactivate")
                                .then(Commands.argument("slot", IntegerArgumentType.integer(0, MAX_DEV_SLOT))
                                        .executes(ctx -> enchant(ctx, adapters, weapons, EnchantOp.DEACTIVATE,
                                                IntegerArgumentType.getInteger(ctx, "slot"), 0, 0, null)))))
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
                // Mint the two crit fixtures. Crit chance bases at 0.15 and crit damage at 1.0, and no
                // content grants either, so without these the boot can only witness the BASE rate and
                // never "gear can modify it". Two commands, not one, because the two stats move
                // independently -- that independence is the thing being demonstrated.
                .then(Commands.literal("critchance")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .executes(ctx -> critBoost(ctx, adapters, null, true))
                        .then(Commands.argument("bonus", DoubleArgumentType.doubleArg(0.0, 1.0))
                                .executes(ctx -> critBoost(ctx, adapters,
                                        DoubleArgumentType.getDouble(ctx, "bonus"), true))))
                .then(Commands.literal("critdamage")
                        .requires(source -> source.getSender().hasPermission(Permissions.DEV))
                        .executes(ctx -> critBoost(ctx, adapters, null, false))
                        .then(Commands.argument("bonus", DoubleArgumentType.doubleArg(0.0, 20.0))
                                .executes(ctx -> critBoost(ctx, adapters,
                                        DoubleArgumentType.getDouble(ctx, "bonus"), false))))
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
     * Refill the caller's mana to full.
     *
     * <p>Implemented as {@code ResourcePool.clear}, which is not a workaround but the pool's own
     * definition of full: an owner with no entry reads as {@code max}, because the pool stores a
     * spent amount and a tick to regenerate from rather than a current value. So dropping the entry
     * IS a refill, and it needs no new core method and no second notion of "full" to drift from the
     * first.
     *
     * <p>It clears EVERY resource this owner holds, not only mana. Today that is the same thing --
     * mana is the only resource -- and this is dev tooling, so the broader sweep is acceptable. If a
     * second resource ever lands, this becomes "refill everything" and should either be renamed or
     * given a per-resource clear.
     */
    private static int manaRefill(CommandContext<CommandSourceStack> ctx, ResourcePool resources) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }
        resources.clear(player.getUniqueId());
        player.sendMessage(Component.text(
                "Mana refilled to " + Math.round(resources.max()) + ".", NamedTextColor.GREEN));
        return 1;
    }

    /**
     * Mint a crit_chance_boost_TEMP or crit_damage_boost_TEMP into the caller's inventory.
     *
     * <p>Both amounts are BONUSES on their stat's base (0.15 for chance, 1.0 for damage), not
     * resolved values -- the message prints the RESOLVED figure so the boot can read what to expect
     * before swinging rather than deciding afterwards what the number it got should have been, the
     * same discipline the enchant effect line follows.
     */
    private static int critBoost(CommandContext<CommandSourceStack> ctx, AdapterContext adapters,
                                 Double bonus, boolean chance) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }
        if (chance) {
            double amount = bonus == null ? CritModifierItems.DEFAULT_CHANCE_BOOST : bonus;
            player.getInventory().addItem(CritModifierItems.mintChance(adapters.keys(), amount));
            player.sendMessage(Component.text(
                    "Gave crit_chance_boost_TEMP (+" + amount + " -> "
                            + Math.round(Crit.chance(Crit.BASE_CHANCE + amount) * 100)
                            + "% crit rate). Hold it and swing.", NamedTextColor.GREEN));
        } else {
            double amount = bonus == null ? CritModifierItems.DEFAULT_DAMAGE_BOOST : bonus;
            player.getInventory().addItem(CritModifierItems.mintDamage(adapters.keys(), amount));
            player.sendMessage(Component.text(
                    "Gave crit_damage_boost_TEMP (+" + amount + " -> "
                            + (1.0 + Crit.BASE_DAMAGE + amount) + "x on a crit). Hold it and swing.",
                    NamedTextColor.GREEN));
        }
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

        // The roll fires at ACQUISITION, never in mint -- remint calls mint, so a roll there
        // would re-roll on every join, refresh and enchant click. See EnchantRollItems.
        ItemStack item = WeaponItems.mint(weapon, adapters);
        EnchantRollItems.rollOnAcquire(item, weapon, adapters);
        player.getInventory().addItem(item);
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

    /** Which direction {@link #durability} moves the held weapon's wear. */
    private enum DurabilityOp { DAMAGE, REPAIR, SET }

    /**
     * Move the held weapon's durability, for testing. The wear source Pass 1 needs: nothing wears
     * items in play yet, so without this the break gate and #12's step-9 clamp cannot be produced
     * in-game at all.
     *
     * Every value goes through the core kernel, so {@code damage} past the floor CLAMPS rather than
     * destroying the item -- this command cannot break the promise it exists to test.
     *
     * Hops to the player's own thread before touching the inventory. A command runs on the command
     * thread, not the player's region thread, which is the contract {@code WeaponRefresher.refresh}
     * states and {@code /rpg refresh} honours.
     *
     * Reports the resulting numbers rather than "done", for the same reason {@code /rpg refresh}
     * reports a count: a no-op and a success must not read alike. The uses-left figure is what the
     * boot gate reads back off the durability bar.
     */
    private static int durability(CommandContext<CommandSourceStack> ctx, AdapterContext adapters,
                                  DurabilityOp op, int amount) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }

        adapters.scheduler().onEntity(player, () -> {
            ItemStack held = player.getInventory().getItemInMainHand();

            // Scope: only our weapons. An untagged vanilla item is not this command's business,
            // the same boundary the gates draw.
            if (WeaponItems.weaponId(held, adapters.keys()).isEmpty()) {
                player.sendMessage(Component.text(
                        "Hold one of our weapons.", NamedTextColor.RED));
                return;
            }

            // A non-Damageable material reports rather than erroring: ember_staff (blaze_rod) and
            // ability_stone (amethyst_shard) have no durability by design, and "no durability" is
            // the correct answer, not a failure.
            OptionalInt max = WeaponDurability.maxOf(held);
            if (max.isEmpty()) {
                player.sendMessage(Component.text(
                        held.getType().name().toLowerCase(Locale.ROOT)
                                + " has no durability -- it can never break.",
                        NamedTextColor.YELLOW));
                return;
            }

            int damage = switch (op) {
                case DAMAGE -> WeaponDurability.wear(held, amount);
                case REPAIR -> WeaponDurability.repair(held, amount);
                case SET -> WeaponDurability.set(held, amount);
            };
            // Write the stack back explicitly rather than relying on the main-hand read being a
            // live mirror, and follow WeaponRefresher's habit of an explicit set. updateInventory
            // makes the bar move now rather than at the client's next sync.
            player.getInventory().setItemInMainHand(held);
            player.updateInventory();

            int maximum = max.getAsInt();
            boolean broken = Durability.isBroken(damage, maximum);
            player.sendMessage(Component.text(
                    "%d/%d uses left (damage %d)%s".formatted(
                            maximum - damage, maximum, damage, broken ? " -- BROKEN" : ""),
                    broken ? NamedTextColor.YELLOW : NamedTextColor.GREEN));
        });
        return 1;
    }

    /**
     * The highest slot and candidate index {@code /rpg enchant} will address.
     *
     * A COMMAND-SIDE bound only. {@code EnchantState} deliberately does not cap slot count, because
     * fixed-3-versus-rolled-1-3 is the roster pass's decision and a cap written now would be a
     * number invented before the question was asked. This exists so a dev cannot mint a 400-slot
     * item by typo, which is guarding at the reachable surface rather than in the kernel.
     */
    private static final int MAX_DEV_SLOT = 2;

    /** Which edit {@link #enchant} makes to the held weapon's enchant state. */
    private enum EnchantOp { CANDIDATE, LEVEL, ACTIVE, DEACTIVATE, CLEAR, SHOW }

    /**
     * Edit the held weapon's enchant state, for testing. The stand-in for both of the things that
     * will eventually put enchants on an item -- the per-instance roll and the enchant table -- in
     * exactly the way {@code /rpg durability} stood in for auto-wear.
     *
     * Every transition goes through the pure, reddening-tested {@code EnchantState}, so this
     * command cannot construct a state the model forbids. What it CAN do is ask for one, which is
     * why each op pre-validates and answers in English: an IllegalArgumentException escaping into
     * Brigadier reads as an internal error rather than as "you have not unlocked that yet".
     *
     * Hops to the player's own thread before touching the inventory, for the reason
     * {@code /rpg durability} and {@code /rpg refresh} both give: a command runs on the command
     * thread, not the player's region thread.
     *
     * <p><b>Writes then RE-MINTS, rather than patching lore.</b> That routes every edit through
     * {@code WeaponItems.remint} -> {@code carryInstanceData} -> {@code applyLore}, so the lore is
     * rebuilt canonically and the enchant block can never be doubled or left stale. It also means
     * the carry-forward invariant is exercised on every single use of this command rather than
     * only at login, which is where a regression in it would otherwise hide.
     */
    private static int enchant(CommandContext<CommandSourceStack> ctx, AdapterContext adapters,
                               WeaponRegistry weapons, EnchantOp op,
                               int slot, int candidate, int level, String enchantId) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage(
                    Component.text("Players only.", NamedTextColor.RED));
            return 0;
        }

        adapters.scheduler().onEntity(player, () -> {
            ItemStack held = player.getInventory().getItemInMainHand();

            // Scope: only our weapons, the same boundary /rpg durability and the gates draw.
            String weaponId = WeaponItems.weaponId(held, adapters.keys()).orElse(null);
            if (weaponId == null) {
                player.sendMessage(Component.text("Hold one of our weapons.", NamedTextColor.RED));
                return;
            }

            // A re-mint needs the definition. Refuse rather than half-edit: writing the state and
            // failing to re-mint would leave an item whose PDC and whose lore disagree, which is
            // the one outcome worse than doing nothing. Same instinct as RefreshVerdict.Dangling.
            WeaponDefinition definition = weapons.find(weaponId).orElse(null);
            if (definition == null) {
                player.sendMessage(Component.text("'" + weaponId + "' has no content file loaded -- "
                        + "cannot re-mint it, so refusing to edit its enchants.", NamedTextColor.RED));
                return;
            }

            EnchantState before = EnchantItems.read(held, adapters.keys());

            if (op == EnchantOp.SHOW) {
                showEnchants(player, definition, before, adapters);
                return;
            }

            if (op == EnchantOp.CLEAR) {
                if (before.isEmpty() && !EnchantItems.isRolled(held, adapters.keys())) {
                    player.sendMessage(Component.text("This weapon carries no enchant data.",
                            NamedTextColor.YELLOW));
                    return;
                }
                held.editMeta(meta -> EnchantItems.clear(meta, adapters.keys()));
                finishEnchant(player, held, definition, adapters);
                player.sendMessage(Component.text("Enchant data cleared -- both keys removed.",
                        NamedTextColor.GREEN));
                return;
            }

            // Pre-validation, so every refusal is a sentence rather than a stack trace.
            EnchantDefinition enchantDef = null;
            if (op == EnchantOp.CANDIDATE) {
                enchantDef = adapters.enchants().find(enchantId).orElse(null);
                if (enchantDef == null) {
                    player.sendMessage(Component.text("Unknown enchant: " + enchantId, NamedTextColor.RED));
                    String available = adapters.enchants().all().stream()
                            .map(EnchantDefinition::id).collect(Collectors.joining(", "));
                    // An EMPTY list here is the loader having found nothing -- the same defect the
                    // boot warning names, caught from in-game. Say so rather than printing
                    // "Available: " and letting it read like a typo.
                    player.sendMessage(available.isEmpty()
                            ? Component.text("No enchants are loaded at all -- check the boot log "
                                    + "for the content/enchants warning.", NamedTextColor.RED)
                            : Component.text("Available: " + available, NamedTextColor.GRAY));
                    return;
                }
                if (slot > before.slots().size()) {
                    player.sendMessage(Component.text("This weapon has " + before.slots().size()
                            + " slot(s); add to slot " + before.slots().size() + " first.",
                            NamedTextColor.RED));
                    return;
                }
            } else {
                if (slot >= before.slots().size()) {
                    player.sendMessage(Component.text("This weapon has " + before.slots().size()
                            + " slot(s); slot " + slot + " does not exist.", NamedTextColor.RED));
                    return;
                }
                int candidates = before.slots().get(slot).candidates().size();
                if (op != EnchantOp.DEACTIVATE && candidate >= candidates) {
                    player.sendMessage(Component.text("Slot " + slot + " has " + candidates
                            + " candidate(s).", NamedTextColor.RED));
                    return;
                }
                if (op == EnchantOp.LEVEL || op == EnchantOp.ACTIVE) {
                    String id = before.slots().get(slot).candidates().get(candidate).enchantId();
                    enchantDef = adapters.enchants().find(id).orElse(null);
                    // A per-enchant max_level may be LOWER than the model's. Checked here because
                    // core has no idea which enchants exist.
                    if (op == EnchantOp.LEVEL && enchantDef != null && level > enchantDef.maxLevel()) {
                        player.sendMessage(Component.text(enchantDef.displayName()
                                + "'s maximum level is " + enchantDef.maxLevel() + ".", NamedTextColor.RED));
                        return;
                    }
                    if (op == EnchantOp.ACTIVE
                            && before.slots().get(slot).candidates().get(candidate).isLocked()) {
                        player.sendMessage(Component.text("That candidate is locked (level 0) -- "
                                + "unlock it first: /rpg enchant level " + slot + " " + candidate + " 1",
                                NamedTextColor.RED));
                        return;
                    }
                }
            }

            EnchantState after;
            try {
                after = switch (op) {
                    case CANDIDATE -> before.addCandidate(slot, enchantId);
                    case LEVEL -> before.withLevel(slot, candidate, level);
                    case ACTIVE -> before.withActive(slot, candidate);
                    case DEACTIVATE -> before.withoutActive(slot);
                    // Handled above and returned; listed so the switch stays exhaustive.
                    case CLEAR, SHOW -> before;
                };
            } catch (IllegalArgumentException ex) {
                // The model's own refusals, surfaced verbatim. Reachable for the cases the
                // pre-validation above deliberately does not duplicate -- a duplicate candidate id
                // being the obvious one.
                player.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED));
                return;
            }

            // A no-op must not wear the same colour as a success. Records give equals() for free,
            // so this costs nothing and catches "I already did that" before it reads as a change.
            if (after.equals(before)) {
                player.sendMessage(Component.text("Nothing changed -- that is already the state.",
                        NamedTextColor.YELLOW));
                return;
            }

            held.editMeta(meta -> EnchantItems.write(meta, after, adapters.keys()));
            finishEnchant(player, held, definition, adapters);

            String name = enchantDef != null ? enchantDef.displayName()
                    : (enchantId != null ? enchantId : "that candidate");
            switch (op) {
                case CANDIDATE -> player.sendMessage(Component.text("Slot " + slot + " candidate "
                        + (after.slots().get(slot).candidates().size() - 1) + ": " + name
                        + " (locked).", NamedTextColor.GREEN));
                case LEVEL -> player.sendMessage(Component.text(level == 0
                        ? "Slot " + slot + ": " + name + " re-locked."
                        : "Slot " + slot + ": " + name + " unlocked to "
                                + EnchantLoreLines.romanNumeral(level) + ".", NamedTextColor.GREEN));
                case ACTIVE -> {
                    int effective = after.activeLevel(
                            after.slots().get(slot).candidates().get(candidate).enchantId());
                    // The suffix dispatches by effect(), so this says what THIS enchant does -- and
                    // says "inert" when it does nothing here, which is the moment that is
                    // correctable. The full stop is the caller's: it keeps the reply a sentence
                    // like its siblings while the shared string stays exactly what show prints.
                    player.sendMessage(Component.text("Slot " + slot + " active: " + name + " "
                            + EnchantLoreLines.romanNumeral(effective)
                            + EnchantEffectLine.of(enchantDef, effective, definition.weaponClass())
                            + ".", NamedTextColor.GREEN));
                    // Belt and braces on top of effective()'s max rule: say so, rather than letting
                    // a dev wonder why activating a second copy changed nothing.
                    long copies = after.slots().stream()
                            .filter(s -> s.active().isPresent())
                            .filter(s -> s.active().orElseThrow().enchantId().equals(
                                    after.slots().get(slot).candidates().get(candidate).enchantId()))
                            .count();
                    if (copies > 1) {
                        player.sendMessage(Component.text("Note: that enchant is active in "
                                + copies + " slots. The highest level wins -- they do not stack "
                                + "(provisional; the roster pass decides the real rule).",
                                NamedTextColor.YELLOW));
                    }
                }
                case DEACTIVATE -> player.sendMessage(Component.text("Slot " + slot
                        + " deactivated -- every candidate keeps its level.", NamedTextColor.GREEN));
                case CLEAR, SHOW -> { }
            }
        });
        return 1;
    }

    /**
     * Re-mint the edited weapon back into the player's hand.
     *
     * The re-mint is what rebuilds the lore, so this is also what makes the enchant block appear,
     * change and disappear. Explicit set plus updateInventory for the same reason
     * {@code /rpg durability} does it: the tooltip changes on this command rather than at the
     * client's next sync.
     */
    private static void finishEnchant(Player player, ItemStack held, WeaponDefinition definition,
                                      AdapterContext adapters) {
        player.getInventory().setItemInMainHand(WeaponItems.remint(held, definition, adapters));
        player.updateInventory();
    }

    /**
     * Print the decoded state AND the raw blob.
     *
     * The raw string is deliberately included: it is the boot gate's evidence that the carry moved
     * BYTES rather than decoding and re-encoding, which is checkable only by comparing the string
     * character for character across a restart. It also makes a decode failure visible -- a blob
     * that is present but reads as empty says so here, where "no enchant data" would not.
     */
    private static void showEnchants(Player player, WeaponDefinition definition,
                                     EnchantState state, AdapterContext adapters) {
        String raw = player.getInventory().getItemInMainHand().getItemMeta()
                .getPersistentDataContainer().get(adapters.keys().enchantData, PersistentDataType.STRING);

        if (raw == null) {
            player.sendMessage(Component.text("This weapon carries no enchant data.", NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text(definition.displayName() + " -- " + state.slots().size()
                + " slot(s)", NamedTextColor.AQUA));
        for (int i = 0; i < state.slots().size(); i++) {
            var slot = state.slots().get(i);
            StringBuilder line = new StringBuilder("  Slot " + i + ": [");
            for (int c = 0; c < slot.candidates().size(); c++) {
                if (c > 0) line.append(", ");
                if (c == slot.activeIndex()) line.append('*');
                var cand = slot.candidates().get(c);
                line.append(cand.enchantId()).append(' ')
                        .append(cand.isLocked() ? "locked" : EnchantLoreLines.romanNumeral(cand.level()));
            }
            player.sendMessage(Component.text(line.append(']').toString(), NamedTextColor.GRAY));
        }
        for (var active : state.effective()) {
            player.sendMessage(Component.text("  active: " + active.enchantId() + " "
                    + EnchantLoreLines.romanNumeral(active.level())
                    + EnchantEffectLine.of(adapters.enchants().find(active.enchantId()).orElse(null),
                            active.level(), definition.weaponClass()), NamedTextColor.GRAY));
        }
        player.sendMessage(Component.text("  raw: " + raw, NamedTextColor.DARK_GRAY));
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
        // and mana here -- rather than inside the region hop below -- is what
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
            case AbilityService.CastResult.Broken ignored -> {
                // Unreachable: /rpg cast goes through AbilityService, which never reads a weapon,
                // and Broken is minted only by WeaponFire.attempt off a held item's durability. The
                // arm exists because CastResult is sealed and this switch has no default -- which
                // is precisely how adding Broken forced every caller to decide what it means.
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
            // Inside the loop: each kit weapon is its own instance and rolls its own candidates.
            EnchantRollItems.rollOnAcquire(item, weapon, adapters);

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
