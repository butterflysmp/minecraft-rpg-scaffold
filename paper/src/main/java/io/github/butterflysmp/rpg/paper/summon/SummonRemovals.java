package io.github.butterflysmp.rpg.paper.summon;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.summon.SummonRemoval;
import io.github.butterflysmp.rpg.core.summon.SummonStore;
import io.github.butterflysmp.rpg.core.summon.SummonTrace;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * D7, EVERY WAY A SUMMON LEAVES (the seat, 2026-11-24), in the manner of {@code OrbRemovals} (B6): each removal is one
 * {@link SummonRemoval} value, logged {@code SUMMON removed ... cause=<token>}; the store stops owning the body, its
 * customHP record is cleared, and the body is removed. Static over the {@link AdapterContext}, so the selftest's
 * clear_state and the event hooks reach it without the service.
 */
public final class SummonRemovals {

    private SummonRemovals() {}

    /** One owned body. False when the store did not own it. */
    public static boolean remove(AdapterContext ctx, UUID body, SummonRemoval cause) {
        var s = ctx.summons().remove(body);
        s.ifPresent(summon -> discard(ctx, summon, cause));
        return s.isPresent();
    }

    /** Every body of his (his death, quit, world change; the selftest's clear_state). */
    public static int removeAllOf(AdapterContext ctx, UUID owner, SummonRemoval cause) {
        List<SummonStore.Summon> gone = ctx.summons().removeAllOf(owner);
        for (SummonStore.Summon s : gone) discard(ctx, s, cause);
        return gone.size();
    }

    /** Server stop. */
    public static void removeAll(AdapterContext ctx) {
        for (SummonStore.Summon s : ctx.summons().removeAll()) discard(ctx, s, SummonRemoval.STOP);
    }

    /** A record the store has ALREADY let go of (a replace hands the old one back). */
    static void discard(AdapterContext ctx, SummonStore.Summon s, SummonRemoval cause) {
        Entity body = Bukkit.getEntity(s.body());
        Vec3 at = body == null ? null : vec(body.getLocation());   // O2: gone means at=unknown
        ctx.stats().clear(s.body());
        if (body != null && body.isValid()) body.remove();
        ctx.log().info(SummonTrace.removed(s, at, cause));
    }

    /** A tagged body the store does not own: an orphan (saved by some path, or left by a crash). */
    static boolean orphan(AdapterContext ctx, Entity e) {
        return e.getPersistentDataContainer().has(ctx.keys().summonOwner) && !ctx.summons().owns(e.getUniqueId());
    }

    /** At enable, and one tick after an add: an orphan is removed and logged. On the entity's thread. */
    public static void sweep(AdapterContext ctx, Entity e) {
        if (!orphan(ctx, e)) return;
        Vec3 at = vec(e.getLocation());
        e.remove();
        ctx.log().info(String.format(Locale.ROOT, "SUMMON removed owner=- weapon=- body=%s at=%.2f %.2f %.2f cause=%s",
                e.getUniqueId(), at.x(), at.y(), at.z(), SummonRemoval.ORPHAN.token()));
    }

    /** As an entity is added: a tagged orphan is removed ONE TICK LATER (a removal at the add is refused, as for orbs). */
    public static void sweepAppeared(AdapterContext ctx, Entity e) {
        if (!orphan(ctx, e)) return;
        ctx.scheduler().onEntityLater(e, () -> sweep(ctx, e), 1);
    }

    static Vec3 vec(Location l) {
        return new Vec3(l.getX(), l.getY(), l.getZ());
    }
}
