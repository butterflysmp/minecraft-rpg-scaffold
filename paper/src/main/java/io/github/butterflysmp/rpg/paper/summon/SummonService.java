package io.github.butterflysmp.rpg.paper.summon;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.summon.SummonBackstop;
import io.github.butterflysmp.rpg.core.summon.SummonCap;
import io.github.butterflysmp.rpg.core.summon.SummonDefinition;
import io.github.butterflysmp.rpg.core.summon.SummonRegistry;
import io.github.butterflysmp.rpg.core.summon.SummonRemoval;
import io.github.butterflysmp.rpg.core.summon.SummonStore;
import io.github.butterflysmp.rpg.core.summon.SummonTrace;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * THE SUMMON SERVICE (the Summoner slice, 3b; the seat's D1, D3, D6, D7, Q4; 2026-11-24). A 3b summon STANDS WHERE IT
 * WAS SPAWNED AND DOES NOTHING: no guard, no targeting, no left click (PR 4).
 *
 * <p><b>THE CAST (D6).</b> {@code WeaponFire} hands a {@code type: summon} cast here BEFORE the cast is charged.
 * {@link SummonCap} decides; a refusal costs no mana and no cooldown and sends one notice; an allowed cast is charged
 * (the caller's {@code charge}) and, on success, spawns. <b>THE SCALING (D3)</b>: health and hit are the authored values x
 * the held weapon's score / 100, the score read at the cast and fixed ({@code SummonStore.Summon.cast}).
 *
 * <p><b>THE BODY (D1).</b> The entity comes from the summon's YAML; no Java names a wolf. Every vanilla goal removed and
 * only ours added (none in 3b), untamed and ownerless, never saved, attack attribute 0, its health our customHP. The store
 * OWNS the body before it is added to the world, and the tag goes on AFTER (the Lapis Crystal lesson, D7).
 *
 * <p><b>THE LIFECYCLE (D7).</b> Every removal is one {@link SummonRemoval} value, logged {@code SUMMON removed cause=}.
 */
public final class SummonService {

    /** SEAT PLACEHOLDER, OPEN FOR BEN: the one notice of a refused cast, by reason. */
    static String notice(SummonCap.Verdict v, java.util.OptionalInt max) {
        return v == SummonCap.Verdict.REFUSED_UNKNOWN ? "You cannot summon yet: your profile is still loading."
                : "You cannot summon more (max " + (max.isPresent() ? max.getAsInt() : 0) + ").";
    }

    private final AdapterContext ctx;
    private final Supplier<SummonRegistry> registry;
    private final ProfileService profiles;

    public SummonService(AdapterContext ctx, Supplier<SummonRegistry> registry, ProfileService profiles) {
        this.ctx = ctx;
        this.registry = registry;
        this.profiles = profiles;
    }

    public SummonStore store() {
        return ctx.summons();
    }

    // ------------------------------------------------------------------------------------------------ the cast

    /**
     * A {@code type: summon} cast from {@code weapon}'s trigger, decided BEFORE {@code charge} (which spends the mana and
     * starts the cooldown, {@code WeaponService.fire}). On the caster's own thread.
     */
    public CastResult cast(Player player, WeaponDefinition weapon, CastSpec.Summon spec, int score,
                           Supplier<CastResult> charge) {
        Optional<SummonDefinition> def = registry.get().find(spec.summonId());
        if (def.isEmpty()) {
            ctx.warnOnce("Summon '" + spec.summonId() + "' named by weapon '" + weapon.id() + "' is not loaded");
            return new CastResult.SummonRefused("That summon is not loaded.");
        }
        java.util.OptionalInt max = profiles.maxSummons(player.getUniqueId());
        SummonCap.Verdict verdict = SummonCap.decide(weapon.id(), store().liveWeapons(player.getUniqueId()), max);
        if (!verdict.charges()) {
            ctx.log().info("SUMMON refused owner=" + player.getUniqueId() + " weapon=" + weapon.id() + " verdict="
                    + verdict + " max=" + (max.isPresent() ? max.getAsInt() : "unknown"));
            return new CastResult.SummonRefused(notice(verdict, max));
        }
        CastResult charged = charge.get();
        if (charged instanceof CastResult.Success) spawn(player, weapon.id(), def.get(), score);
        return charged;
    }

    /**
     * THE BODY'S SET-UP (D1). Each call, and what stands behind it: <br>
     * {@code World.spawn(Location, Class, Consumer)}: the consumer runs on the new entity BEFORE it is added (paper-api,
     * READ by its signature; "before it is added" is its javadoc's, INFERRED) <br>
     * {@code setPersistent(false)}: never saved, D1(b) READ ({@code saveAsPassenger} refuses an entity whose
     * {@code persist} is false unless forced) <br>
     * {@code MobGoals.removeAllGoals}: every vanilla goal gone, D1(a) READ (only {@code registerGoals}, run once by
     * {@code Mob}'s constructor, adds any); the backstop re-checks <br>
     * {@code Tameable.setTamed(false)}, {@code setOwner(null)}: untamed and ownerless (its interactions are cancelled)
     * <br> {@code ATTACK_DAMAGE} base 0: D1(e), no vanilla hit may deal anything; its health stays vanilla's (8 for a
     * wolf, READ) because its HP is our customHP, {@code bootstrapIfAbsent} with the scaled health before it is added.
     */
    private void spawn(Player owner, String weaponId, SummonDefinition def, int score) {
        EntityType type = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(def.entity()));
        if (type == null || type.getEntityClass() == null || !Mob.class.isAssignableFrom(type.getEntityClass())) {
            ctx.warnOnce("Summon '" + def.id() + "' names '" + def.entity() + "', which is not a mob; nothing spawned");
            return;
        }
        Location at = owner.getLocation();
        Optional<SummonStore.Summon>[] replaced = new Optional[] {Optional.empty()};
        SummonStore.Summon[] placed = new SummonStore.Summon[1];
        Mob body = (Mob) at.getWorld().spawn(at, type.getEntityClass(), e -> {
            Mob mob = (Mob) e;
            mob.setPersistent(false);
            Bukkit.getMobGoals().removeAllGoals(mob);
            if (mob instanceof Tameable t) {
                t.setTamed(false);
                t.setOwner(null);
            }
            AttributeInstance attack = mob.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attack != null) attack.setBaseValue(0.0);
            mob.customName(Component.text(def.name().replace("{owner}", owner.getName())));
            mob.setCustomNameVisible(true);
            placed[0] = SummonStore.Summon.cast(owner.getUniqueId(), weaponId, mob.getUniqueId(), def, score);
            replaced[0] = store().place(placed[0]);   // THE STORE OWNS IT before it is added to the world
            ctx.stats().bootstrapIfAbsent(mob.getUniqueId(), placed[0].health(), 0.0, false);
        });
        // AFTER the add: the tag (D7), so no sweep at the add can meet a tagged body the store does not own.
        body.getPersistentDataContainer().set(ctx.keys().summonOwner, PersistentDataType.STRING,
                owner.getUniqueId().toString());
        ctx.log().info(SummonTrace.spawn(placed[0], vec(body.getLocation())));
        replaced[0].ifPresent(old -> SummonRemovals.discard(ctx, old, SummonRemoval.REPLACE));
    }

    // ------------------------------------------------------------------------------------------------ the backstop

    /** Every {@link SummonBackstop#PERIOD_TICKS}: each owned body judged (owner present? goals still ours?). */
    public void backstopTick() {
        for (SummonStore.Summon s : store().all()) {
            Entity e = Bukkit.getEntity(s.body());
            if (!(e instanceof Mob mob) || !e.isValid()) {
                SummonRemovals.remove(ctx, s.body(), SummonRemoval.BACKSTOP);
                continue;
            }
            ctx.scheduler().onEntity(mob, () -> judge(s, mob));
        }
    }

    private void judge(SummonStore.Summon s, Mob mob) {
        Player owner = Bukkit.getPlayer(s.owner());
        boolean here = owner != null && owner.isOnline() && owner.getWorld().equals(mob.getWorld());
        int foreign = Bukkit.getMobGoals().getAllGoals(mob).size();   // 3b: ours is none, so every goal is foreign
        switch (SummonBackstop.decide(store().owns(s.body()), here, foreign)) {
            case REMOVE -> SummonRemovals.remove(ctx, s.body(), SummonRemoval.BACKSTOP);
            case RESTRIP -> {
                Bukkit.getMobGoals().removeAllGoals(mob);
                ctx.log().info(SummonTrace.goalsRestored(s, vec(mob.getLocation()), foreign));
            }
            case KEEP -> { }
        }
    }

    static Vec3 vec(Location l) {
        return new Vec3(l.getX(), l.getY(), l.getZ());
    }

}
