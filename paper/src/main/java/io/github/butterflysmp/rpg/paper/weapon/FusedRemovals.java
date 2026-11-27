package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.FusedChunks;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.PaperCombatWorld;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * EVERY REMOVAL OF A FUSED CHUNK THAT IS NOT ITS OWN DETONATION (the seat's F2, 2026-11-26, the Blast Fungus; B6's
 * guarantees, as {@link OrbRemovals} gives the orb): his quit, his death, a world change, server stop, the selftest's
 * clear_state, row-start CLEAN and run end, and the SWEEP of a tagged body no live chunk owns. Each takes the chunk out of
 * the store first ({@link FusedChunks#takeAllOf}), so the flight's next step finds it gone and it never goes off; then the
 * body is removed and a {@code FUSED removed … cause=…} line written.
 *
 * <p>Never saved with the world: the body is a marker with {@code setPersistent(false)} (READ,
 * {@code PaperCombatWorld.configureMarker}); the sweep is the backstop for a body that appears anyway, and the marker's
 * armed vanilla despawn (the fuse plus {@code ORPHAN_GRACE_TICKS}) is the backstop behind that.
 */
public final class FusedRemovals {

    private FusedRemovals() {}

    /** Remove every chunk of {@code owner}'s for {@code cause}; how many. Called on the server thread. */
    public static int removeAllOf(AdapterContext ctx, UUID owner, String cause) {
        int n = 0;
        for (FusedChunks.Chunk chunk : ctx.fused().takeAllOf(owner)) {
            discard(ctx, chunk, cause);
            n++;
        }
        return n;
    }

    /** Server stop: every chunk, cause {@code stop}. */
    public static void removeAll(AdapterContext ctx) {
        for (FusedChunks.Chunk chunk : ctx.fused().takeAll()) discard(ctx, chunk, "stop");
    }

    /** The sweep: an entity tagged as a fused chunk's body that no live chunk owns is removed where it is. */
    public static void sweep(AdapterContext ctx, Entity entity) {
        if (!orphan(ctx, entity)) return;
        io.github.butterflysmp.rpg.core.Vec3 at = new io.github.butterflysmp.rpg.core.Vec3(entity.getLocation().getX(),
                entity.getLocation().getY(), entity.getLocation().getZ());
        entity.remove();
        if (!entity.isValid()) {
            ctx.log().info(PaperCombatWorld.fusedLine(new UUID(0, 0), 0, "removed", at, "orphan", Bukkit.getCurrentTick()));
        }
    }

    /** As {@link OrbRemovals#sweepAppeared}: a removal at the add is refused, so the check is taken a tick later. */
    public static void sweepAppeared(AdapterContext ctx, Entity entity) {
        if (!orphan(ctx, entity)) return;
        ctx.scheduler().onEntityLater(entity, () -> sweep(ctx, entity), 1);
    }

    private static boolean orphan(AdapterContext ctx, Entity entity) {
        Byte tag = entity.getPersistentDataContainer().get(ctx.keys().fused, PersistentDataType.BYTE);
        return tag != null && !ctx.fused().ownsMarker(entity.getUniqueId());
    }

    private static void discard(AdapterContext ctx, FusedChunks.Chunk chunk, String cause) {
        if (chunk.markerId() != null) {
            Entity body = Bukkit.getEntity(chunk.markerId());
            if (body != null) body.remove();
        }
        ctx.log().info(PaperCombatWorld.fusedLine(chunk.owner(), chunk.id(), "removed", chunk.at(), cause,
                Bukkit.getCurrentTick()));
    }
}
