package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.Orbs;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.PaperCombatWorld;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * EVERY B6 REMOVAL OF AN ORB THAT IS NOT ITS OWN EXPIRY OR A NEWER THROW (Lapis Crystal's D2, 2026-11-23): his quit, his
 * death, a world change, server stop, and the SWEEP of a body no live orb owns. One path, so every cause removes the body
 * and writes its {@code ORB removed … cause=…} line the same way. (Expiry and replacement are {@code CastExecutor}'s.)
 *
 * <p>Never saved with the world: the body is a marker with {@code setPersistent(false)} (READ, {@code configureMarker}),
 * so a save writes it nowhere; the sweep is the backstop for a body that appears anyway.
 */
public final class OrbRemovals {

    private OrbRemovals() {}

    /** Remove {@code owner}'s orb, if any, for {@code cause}. Called on the server thread (a player event, the disable). */
    public static void remove(AdapterContext ctx, UUID owner, String cause) {
        ctx.orbs().remove(owner).ifPresent(orb -> discard(ctx, orb, cause));
    }

    /** Server stop: every orb, cause {@code stop}. */
    public static void removeAll(AdapterContext ctx) {
        for (Orbs.Orb orb : ctx.orbs().removeAll()) discard(ctx, orb, "stop");
    }

    /** The sweep: an entity tagged as an orb's body that no live orb owns is removed where it appears. */
    public static void sweep(AdapterContext ctx, Entity entity) {
        Byte tag = entity.getPersistentDataContainer().get(ctx.keys().orb, PersistentDataType.BYTE);
        if (tag == null) return;
        if (ctx.orbs().ownsMarker(entity.getUniqueId())) return;
        io.github.butterflysmp.rpg.core.Vec3 at = new io.github.butterflysmp.rpg.core.Vec3(entity.getLocation().getX(),
                entity.getLocation().getY(), entity.getLocation().getZ());
        entity.remove();
        ctx.log().info(PaperCombatWorld.orbLine(new UUID(0, 0), "removed", at, "orphan", Bukkit.getCurrentTick()));
    }

    private static void discard(AdapterContext ctx, Orbs.Orb orb, String cause) {
        // Every removal cancels the orb's expiry (the seat's item 6, 2026-11-23): the LINGER work=1 that opened LC2R2-LC4R2
        // on efa38636 was that task, left scheduled here. It could not act (its generation is gone); now it is not left.
        if (orb.expiryTask() >= 0) ctx.work().take(orb.expiryTask());
        // And a block body's backstop (the seat's ruling (b), 2026-11-23): a proper removal leaves it nothing to do.
        if (orb.backstopTask() >= 0) ctx.work().take(orb.backstopTask());
        Entity body = Bukkit.getEntity(orb.markerId());
        if (body != null) body.remove();
        ctx.log().info(PaperCombatWorld.orbLine(orb.owner(), "removed", orb.restPoint(), cause, Bukkit.getCurrentTick()));
    }
}
