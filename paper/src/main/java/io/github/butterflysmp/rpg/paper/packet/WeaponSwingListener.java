package io.github.butterflysmp.rpg.paper.packet;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponService;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.weapon.WeaponFire;
import org.bukkit.entity.Player;

/**
 * A real left-click fires the held weapon's left_click trigger. Replaces the temporary
 * /rpg swing_TEMP: the arm-swing packet is the reliable left-click signal (Bukkit's
 * PlayerInteractEvent misses left-click-on-air and can double-fire). First packet code
 * in the project, so the threading contract is absolute -- see PacketListenerBase.
 *
 * The callback runs on a NETTY I/O thread. Its body does ZERO Bukkit work: it filters the
 * packet and hops. Everything that touches the world runs inside bukkit(player, ...), on
 * the thread that owns the player.
 *
 * Filter order is load-bearing and is exactly:
 *   1. packet-type reject   -- FIRST, so movement/look packets never reach the hop
 *   2. main-hand + Player   -- still on Netty, pure
 *   3. hop                  -- bukkit(player, ...)
 *   4. weapon_id check      -- INSIDE the hop, because reading the held item is a Bukkit call
 *
 * The weapon_id reject is step 4, not step 1: reading the item off the player is a Bukkit
 * operation and cannot run on the Netty thread. A swing hopping is fine -- a swing is a
 * click, not a tick. A movement packet hopping is not, which is why step 1 comes first.
 */
public final class WeaponSwingListener extends PacketListenerBase {

    private final AdapterContext adapters;
    private final WeaponRegistry weapons;
    private final WeaponService weaponService;

    public WeaponSwingListener(AdapterContext adapters, WeaponRegistry weapons,
                               WeaponService weaponService) {
        super(adapters.scheduler(), PacketListenerPriority.NORMAL);
        this.adapters = adapters;
        this.weapons = weapons;
        this.weaponService = weaponService;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // 1. Packet-type reject, FIRST. Movement and look packets -- the high-frequency
        //    traffic -- die here, having cost one reference comparison and no hop.
        if (event.getPacketType() != PacketType.Play.Client.ANIMATION) return;

        // 2. Main-hand swings only, and only from a Player. Both pure: the hand comes off
        //    the packet wrapper (PacketEvents data, not Bukkit), and getPlayer is a lookup.
        InteractionHand hand = new WrapperPlayClientAnimation(event).getHand();
        if (!isMainHandSwing(event.getPacketType(), hand)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        // 3. Hop. The ONLY statement that reaches the world, and it defers onto the
        //    player's owning thread. Nothing Bukkit has run on the Netty thread.
        bukkit(player, () -> onSwing(player));
    }

    /**
     * The left-click signal: a main-hand arm swing. Pure over its inputs so it is unit
     * testable with no packet and no server -- the one piece of new logic 1b adds that can
     * be. An off-hand swing is not an attack; a non-animation packet is not a swing.
     */
    static boolean isMainHandSwing(PacketTypeCommon type, InteractionHand hand) {
        return type == PacketType.Play.Client.ANIMATION && hand == InteractionHand.MAIN_HAND;
    }

    /**
     * Runs on the thread that owns the player. Bukkit is legal here. This is where the
     * weapon_id reject happens -- reading the held item is a Bukkit call -- and it is
     * delegated to WeaponFire, shared with the right-click handler. Silent: a swing that
     * lands nothing because you are mid-cooldown or out of energy does not deserve chat spam.
     */
    private void onSwing(Player player) {
        WeaponFire.attempt(player, "left_click", weapons, weaponService, adapters);
    }
}
