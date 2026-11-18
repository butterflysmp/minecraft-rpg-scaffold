package io.github.butterflysmp.rpg.paper.adapter;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.protocol.sound.Sounds;
import com.github.retrooper.packetevents.protocol.util.WeightedList;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import io.github.butterflysmp.rpg.core.Vec3;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * THE DRAG'S ADD, DELIVERED TO A PLAYER'S OWN CLIENT, WHICH ADDS IT TO WHATEVER MOMENTUM HE CARRIES (feat/drag-additive,
 * 2026-11-18; the seat's ruling: no design may need the server to know his momentum). One packet per drag per tick.
 *
 * <h2>WHY A PACKET (CLAUDE.md invariant 4): THE API CANNOT SAY "ADD"</h2>
 * Every line READ with {@code javap -c -p}, the client {@code .minecraft/versions/26.1.2/26.1.2.jar}, the server
 * {@code run/versions/26.1.2/paper-26.1.2.jar}:
 * <ul>
 *   <li>{@code Entity#setVelocity} REPLACES his velocity: {@code ClientPacketListener#handleSetEntityMotion} calls
 *       {@code Entity#lerpMotion}, which is {@code setDeltaMovement}. To add with it the server must know his momentum,
 *       which it does not (the drag of #205 estimated it and varied run to run).</li>
 *   <li>{@code Player#teleport} with {@code TeleportFlag.Relative.VELOCITY_*} keeps his velocity but cannot add to it:
 *       {@code CraftEntity#teleport0} builds its {@code TeleportTransition} with {@code Vec3.ZERO} as the delta. And
 *       any position packet makes the server wait for his confirm: {@code ServerGamePacketListenerImpl#teleport} sets
 *       {@code awaitingPositionFromClient}, and while it is set {@code handleMovePlayer} keeps only his rotation
 *       ({@code updateAwaitingTeleport} true, then {@code absSnapRotationTo}); on the confirm the server snaps him to
 *       the position it sent ({@code handleAcceptTeleportPacket}: {@code absSnapTo}). Twenty a second would drop his
 *       moves for most of the second. An id we invented ourselves would not wait, but a confirm whose id equals the
 *       server's last, with nothing awaited, KICKS him ({@code disconnect(invalid_player_movement)}).</li>
 *   <li>{@code World#createExplosion} is a real explosion: damage, blocks, sound, and a push we cannot set.</li>
 * </ul>
 * <b>THE ROUTE: {@code ClientboundExplodePacket}'s {@code playerKnockback}</b>, which the client ADDS:
 * {@code ClientPacketListener#handleExplosion} ends {@code playerKnockback().ifPresent(player::addDeltaMovement)}, and
 * {@code Entity#addDeltaMovement} is {@code setDeltaMovement(getDeltaMovement().add(v))} (no override in
 * {@code LocalPlayer}, {@code AbstractClientPlayer}, {@code Player} or {@code LivingEntity}). It carries no teleport id
 * and no confirm; the server learns of it only through his next moves, like his own walking. It is vanilla's own route
 * for pushing a player with an explosion. The packet's other effects, each READ, and what is sent for each:
 * <ul>
 *   <li><b>the sound</b> ({@code playLocalSound} at the centre): {@code intentionally_empty}, which the client's
 *       {@code SoundEngine#play} special-cases ({@code SoundManager.INTENTIONALLY_EMPTY_SOUND}; that it then plays
 *       nothing is INFERRED from that branch and the name), at a centre {@link #HIDDEN_ABOVE} blocks above him;</li>
 *   <li><b>the one particle</b> ({@code ClientLevel#addParticle} at the centre): {@code smoke}, registered with its
 *       override-limiter FALSE ({@code ParticleTypes}: {@code register("smoke", false)}), so {@code doAddParticle}
 *       drops it beyond 32 blocks of the camera ({@code ldc2_w 1024.0d}): the centre is {@link #HIDDEN_ABOVE} above;</li>
 *   <li><b>the block particles</b> ({@code ClientExplosionTracker#track}): an EMPTY list, which it returns on
 *       before tracking anything; radius 0, block count 0.</li>
 * </ul>
 * PacketEvents 2.13.0 writes the packet, at V_1_21_9 and later, as the server's own codec reads it (centre,
 * radius, block count, the optional knockback, particle, sound, the block-particle list: the order of
 * {@code ClientboundExplodePacket.STREAM_CODEC}'s seven fields, READ). Sent on the caller's thread, which owns the
 * player (the drag's origin region).
 */
public final class DragImpulse {

    /** BLOCKS above his feet the packet's centre sits: far past the particle's 32-block cut. CHOSEN 2026-11-18. */
    static final double HIDDEN_ABOVE = 256.0;

    private DragImpulse() {}

    /** Have {@code player}'s client add {@code add} (blocks per tick) to his own velocity. */
    public static void send(Player player, Vec3 add) {
        Location at = player.getLocation();
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerExplosion(
                new Vector3d(at.getX(), at.getY() + HIDDEN_ABOVE, at.getZ()), 0.0f, 0,
                new Vector3d(add.x(), add.y(), add.z()),
                new Particle<>(ParticleTypes.SMOKE), Sounds.INTENTIONALLY_EMPTY, new WeightedList<>()));
    }
}
