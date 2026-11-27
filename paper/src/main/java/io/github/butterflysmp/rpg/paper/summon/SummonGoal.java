package io.github.butterflysmp.rpg.paper.summon;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.combat.CritState;
import io.github.butterflysmp.rpg.core.combat.DefenseRule;
import io.github.butterflysmp.rpg.core.combat.HitAccrual;
import io.github.butterflysmp.rpg.core.mob.MobHostility;
import io.github.butterflysmp.rpg.core.summon.SummonDefinition;
import io.github.butterflysmp.rpg.core.summon.SummonFeeds;
import io.github.butterflysmp.rpg.core.summon.SummonGuard;
import io.github.butterflysmp.rpg.core.summon.SummonStore;
import io.github.butterflysmp.rpg.core.summon.SummonTrace;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.CombatSides;
import io.github.butterflysmp.rpg.paper.adapter.LiveTargets;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * THE SUMMON'S ONE GOAL (the seat's 4b, 2026-11-24): the guard on the server. Every tick, on the body's own thread (a
 * Paper goal ticks with its mob), it builds the candidates near HIM, asks {@link SummonGuard#decide}, and executes:
 * paths to the target and, in reach, applies OUR hit every {@code attack_cadence_ticks}; idle, walks back within the guard
 * radius; beyond the teleport distance, teleports to him. No vanilla attack exists to fire (every vanilla goal is gone and
 * its attack attribute is 0); the swing is a visual only.
 *
 * <p><b>THE HIT (D3):</b> {@code applyDamageAs(SUMMON, damage, owner, NORMAL, APPLIES, weapon element, weapon accrual)}:
 * the authored damage x the score it was cast at / 100, fixed; the weapon's element; the victim's defence applies; the
 * credit is the owner's; the attacker side is SUMMON, given explicitly (Q1); no crit (NORMAL), no class or level bonus
 * (none is added), and never a "hit" for an enchant (the engine's {@code primaryHit} is never called here, B4).
 *
 * <p>Server calls, each READ in paper-api by javap: {@code Goal} ({@code shouldActivate}, {@code tick}, {@code getKey},
 * {@code getTypes}); {@code GoalKey.of(Class, NamespacedKey)}; {@code Mob.getPathfinder().moveTo(Entity, double)} and
 * {@code stopPathfinding()}; {@code LivingEntity.swingMainHand()}; {@code Mob.getTarget()}.
 */
public final class SummonGoal implements Goal<Mob> {

    /** CC'S CALL: the pathing speed multiplier (1.0 = the mob's own). Alternative: a YAML number. */
    static final double SPEED = 1.0;
    /** CC'S CALL: in reach at 2.0 blocks, centre to centre. Alternative: vanilla's attack bounding box. */
    static final double REACH = 2.0;

    private final AdapterContext ctx;
    private final SummonFeeds feeds;
    private final SummonStore.Summon summon;
    private final SummonDefinition def;
    private final Mob body;
    private final GoalKey<Mob> key;
    private Optional<SummonGuard.Held> current = Optional.empty();   // G1, G2: the held target and its tier
    private long lastAttackTick = Long.MIN_VALUE / 2;

    SummonGoal(AdapterContext ctx, SummonFeeds feeds, SummonStore.Summon summon, SummonDefinition def, Mob body) {
        this.ctx = ctx;
        this.feeds = feeds;
        this.summon = summon;
        this.def = def;
        this.body = body;
        this.key = GoalKey.of(Mob.class, goalKey(ctx));
    }

    /** O3: OUR goals are keyed in our namespace, so the backstop never counts them as foreign. */
    static NamespacedKey goalKey(AdapterContext ctx) {
        return NamespacedKey.fromString(ctx.keys().summonOwner.getNamespace() + ":summon_guard");
    }

    @Override public boolean shouldActivate() {
        return ctx.summons().owns(body.getUniqueId());
    }

    @Override public boolean shouldStayActive() {
        return shouldActivate();
    }

    @Override public GoalKey<Mob> getKey() {
        return key;
    }

    @Override public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }

    @Override public void tick() {
        Player owner = Bukkit.getPlayer(summon.owner());
        if (owner == null || !owner.isOnline() || !owner.getWorld().equals(body.getWorld())) return;   // the backstop's
        long now = Bukkit.getCurrentTick();
        Vec3 him = vec(owner);
        Vec3 at = vec(body);
        Optional<UUID> directed = ctx.summons().directed(body.getUniqueId());
        SummonGuard.Decision d = SummonGuard.decide(def, him, at, directed, current, candidates(owner, directed), now);
        if (d.clearDirected()) ctx.summons().clearDirected(body.getUniqueId());
        if (d.teleport()) {
            body.teleport(owner.getLocation());
            ctx.log().info(SummonTrace.teleport(summon, vec(body), at));
        }
        if (d.change() != SummonGuard.Change.NONE || d.dropped().isPresent()) {   // G7: once, with its reason
            ctx.log().info(SummonTrace.target(summon, vec(body), d));
        }
        current = d.held();
        ctx.summons().setCurrent(body.getUniqueId(), current);   // R3: the probe reads OUR target
        Entity target = current.map(SummonGuard.Held::id).map(Bukkit::getEntity).orElse(null);
        if (target instanceof LivingEntity victim) {
            body.getPathfinder().moveTo(victim, SPEED);
            if (body.getLocation().distance(victim.getLocation()) <= REACH && now - lastAttackTick >= def.attackCadenceTicks()) {
                lastAttackTick = now;
                body.swingMainHand();   // the visual; the damage is ours, below
                BukkitCombatant.of(victim, ctx).handle().applyDamageAs(MobHostility.Side.SUMMON, summon.damage(),
                        summon.owner(), CritState.NORMAL, DefenseRule.APPLIES, summon.element(), HitAccrual.weapon());
                ctx.log().info(SummonTrace.hit(summon, vec(body), victim.getUniqueId(), summon.authoredDamage(),
                        summon.element(), now));   // X2: the tick the hit landed on
            }
        } else if (SummonGuard.needsReturn(def, him, at)) {
            body.getPathfinder().moveTo(owner, SPEED);
        } else {
            body.getPathfinder().stopPathfinding();
        }
    }

    /**
     * The bodies near HIM within the give-up range, read on this thread; the directed and held ones by id too (a held one
     * beyond the range is found by id and dropped for range). G1: a body in ANOTHER WORLD is left out, so the guard reads
     * it as gone (CC'S CALL: reported as {@code dropped:gone}).
     */
    private List<SummonGuard.Candidate> candidates(Player owner, Optional<UUID> directed) {
        List<SummonGuard.Candidate> out = new ArrayList<>();
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        double r = def.giveUpRange();
        List<Entity> near = new ArrayList<>(owner.getNearbyEntities(r, r, r));
        directed.map(Bukkit::getEntity).ifPresent(near::add);
        current.map(SummonGuard.Held::id).map(Bukkit::getEntity).ifPresent(near::add);
        for (Entity e : near) {
            if (!(e instanceof LivingEntity l) || l.equals(body) || l.equals(owner) || !seen.add(l.getUniqueId())) continue;
            if (!l.getWorld().equals(owner.getWorld())) continue;   // G1: another world is gone
            out.add(new SummonGuard.Candidate(l.getUniqueId(), CombatSides.of(l, ctx.keys()), vec(l), LiveTargets.isLive(l),
                    feeds.damagedAt(owner.getUniqueId(), l.getUniqueId()),
                    l instanceof Mob m && owner.equals(m.getTarget())));
        }
        return out;
    }

    private static Vec3 vec(Entity e) {
        return new Vec3(e.getLocation().getX(), e.getLocation().getY(), e.getLocation().getZ());
    }
}
