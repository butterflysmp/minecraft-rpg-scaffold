package io.github.butterflysmp.rpg.paper.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;

/**
 * Suppress VANILLA's crit particles, so a crit burst means OUR roll and nothing else.
 *
 * <p>Vanilla decides a crit on its own criteria -- falling, not sprinting, not on a ladder, no
 * blindness -- and plays the particle burst for it. None of that reaches our damage: the vanilla
 * multiplier lands on the TOKENED number, so a vanilla crit has always been a visual claiming
 * something the engine did not do. Before this pass that was a small lie about a hit that was merely
 * unremarkable. Now that we HAVE crit particles of our own, it is worse than a lie -- it is the same
 * symbol meaning two different things, and a player counting bursts to read their crit rate would
 * count vanilla's jump attacks among them.
 *
 * <h2>Why a packet, when the project says prefer the API</h2>
 * There is no Bukkit event for this. The crit visual is not an EntityDamageEvent side effect that can
 * be cancelled, not a particle spawn that can be intercepted, and not exposed on any attack event --
 * vanilla sends it directly from {@code Player#attack} with no hook in between. This is the case the
 * standing rule reserves packets for: the API genuinely cannot express the effect.
 *
 * <h2>Why cancelling this CANNOT eat our own particle</h2>
 * Checked against the pinned PacketEvents 2.13.0 API before a line of this was written, and it is the
 * prerequisite the whole change rests on:
 *
 * <ul>
 *   <li>vanilla's crit burst is {@link PacketType.Play.Server#ENTITY_ANIMATION} carrying
 *       {@code CRITICAL_HIT} (id 4) -- the packet this cancels;</li>
 *   <li>our burst is {@code World.spawnParticle(Particle.CRIT, ...)} from {@code DamagePopupManager},
 *       which is {@link PacketType.Play.Server#PARTICLE}.</li>
 * </ul>
 *
 * Two different packet types, so the cancel below cannot reach ours. Had they shared a type this
 * class would have had to distinguish them by payload, or our particle would have had to move to a
 * path the cancel does not cover -- which is why that was settled first rather than discovered by a
 * boot in which crits stopped sparkling altogether.
 *
 * <h2>Scope: CRITICAL_HIT only</h2>
 * {@code MAGIC_CRITICAL_HIT} (id 5, the enchanted-hit sparkle) is deliberately left alone. It is a
 * different visual from a different vanilla condition, and this pass is about crit particles.
 *
 * <p>It was also expected to be unreachable -- the no-vanilla-enchants policy means a player-held
 * item carries no enchantment for vanilla to award a bonus for -- and the 2026-08-28 boot CONFIRMED
 * that: the witness logged every animation type sent during the session and saw no
 * {@code MAGIC_CRITICAL_HIT} at all. That is an absence over one session, not a proof, which is
 * exactly why it was observed rather than cancelled on the strength of the argument. If it ever does
 * appear, cancelling it is one enum away.
 *
 * <h2>Every CRITICAL_HIT is a player's</h2>
 * The packet names only the VICTIM's entity id, never the attacker, so this cannot filter on "was the
 * attacker a player" from the packet alone. It does not need to: vanilla sends this from
 * {@code Player#crit}, reached only from {@code Player#attack}. A mob has no path to it. So
 * cancelling every one of them IS cancelling it for player attacks.
 *
 * <h2>Threading</h2>
 * Runs on a Netty I/O thread and stays there. It reads the packet, compares an enum, and cancels --
 * the one shape the threading contract permits without a hop. It touches NO Bukkit API, which is why
 * it extends {@link PacketListenerAbstract} directly rather than {@link PacketListenerBase}: the base
 * exists to carry a {@code Scheduler} for the hop, and taking one here would be a dead field implying
 * a hop that must never happen.
 */
public final class VanillaCritParticleListener extends PacketListenerAbstract {

    public VanillaCritParticleListener() {
        super(PacketListenerPriority.NORMAL);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_ANIMATION) return;
        WrapperPlayServerEntityAnimation animation = new WrapperPlayServerEntityAnimation(event);
        if (animation.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.CRITICAL_HIT) {
            event.setCancelled(true);
        }
    }
}
