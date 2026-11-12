package io.github.butterflysmp.rpg.paper.mining.spike;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.github.butterflysmp.rpg.core.mining.spike.FacePoint;
import io.github.butterflysmp.rpg.core.mining.spike.FacePoint.Face;
import io.github.butterflysmp.rpg.core.mining.spike.FacePoint.Spot;
import io.github.butterflysmp.rpg.core.mining.spike.MineSpikeLine;
import io.github.butterflysmp.rpg.core.mining.spike.MineSpikeLine.Src;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * The mining INPUT SPIKE (GATE-mining-spike.md). SPIKE CODE: on {@code spike/mining-input}, never
 * merged. It answers one question, how a hit on a weak spot is detected (PLAN-mining.md §4), by
 * logging every relevant packet and event as a MINESPIKE line.
 *
 * <p><b>Threading.</b> {@link #isActive} and {@link #nextSeq} are the ONLY members the Netty thread
 * calls: a concurrent-map read and an atomic increment, no Bukkit. Everything else runs on the
 * player's own thread, reached by the command, a Bukkit event, or {@code PacketListenerBase.bukkit}.
 *
 * <p><b>Not a static singleton</b> (invariant 3): one instance, built in {@code RpgPlugin} and
 * passed to the three places that need it. Sessions live in memory, keyed by UUID, and die on quit.
 */
public final class MineSpike {

    /** Below the damage popups' range (PopupEntityIds counts down from Integer.MAX_VALUE). PLAN F-M7. */
    static final int FIRST_ENTITY_ID = 1_500_000_000;
    static final float SPOT_SIZE = 3f / FacePoint.PX;   // Ben: visible 3x3 sixteenths
    static final float BOX_SIZE = 5f / FacePoint.PX;    // Ben: hit box 5x5 sixteenths
    /** The patch sits behind the spot: half the spot's outset. */
    static final double PATCH_OUTSET = FacePoint.OUTSET / 2;
    static final double REACH = 6.0;
    static final NamespacedKey SPOT_MODEL = new NamespacedKey("rpg", "mining_spot");
    static final NamespacedKey PATCH_MODEL = new NamespacedKey("rpg", "mining_patch");
    /** Vanilla's key, so the tooltip keeps its green "N Attack Speed" line (PLAN F4). */
    static final NamespacedKey BASE_ATTACK_SPEED = NamespacedKey.minecraft("base_attack_speed");
    static final double VANILLA_BASE_ATTACK_SPEED = 4.0;

    /** The three spawn modes. {@code BLOCK} and {@code BOX} use a vanilla item so MS1-MS6 need no pack. */
    public enum Mode { BLOCK, BOX, MODEL }

    private record Session(Mode mode, UUID world, int bx, int by, int bz, Face face, Spot spot,
                           int spotId, Integer boxId, Integer patchId) {
        String role(int id) {
            if (id == spotId) return "spot";
            if (boxId != null && id == boxId) return "box";
            if (patchId != null && id == patchId) return "patch";
            return "other";
        }

        int[] ids() {
            if (boxId != null) return new int[]{spotId, boxId};
            if (patchId != null) return new int[]{spotId, patchId};
            return new int[]{spotId};
        }
    }

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong();
    private final AtomicInteger ids = new AtomicInteger(FIRST_ENTITY_ID);
    private final Logger log;
    private final MineSpikeVisuals visuals = new MineSpikeVisuals();
    private final MineSpikePack pack;

    public MineSpike(Logger log) {
        this.log = log;
        this.pack = new MineSpikePack(log);
    }

    // --- Netty-safe ----------------------------------------------------------------------------

    /** Netty-safe: a concurrent-map read. */
    boolean isActive(UUID playerId) {
        return playerId != null && sessions.containsKey(playerId);
    }

    /** Netty-safe: the shared order for every MINESPIKE line. */
    long nextSeq() {
        return seq.incrementAndGet();
    }

    // --- the command ---------------------------------------------------------------------------

    /** A second run of any mode stops the running session; otherwise start {@code mode}. */
    void toggle(Player player, Mode mode) {
        Session running = sessions.get(player.getUniqueId());
        if (running != null) {
            stop(player, running, "second_run");
            player.sendMessage(Component.text("Mine spike OFF (" + running.mode().name().toLowerCase() + ").",
                    NamedTextColor.GRAY));
            return;
        }
        RayTraceResult hit = player.rayTraceBlocks(REACH, FluidCollisionMode.NEVER);
        if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() == null) {
            player.sendMessage(Component.text("Look at a block face within " + (int) REACH + " blocks.",
                    NamedTextColor.RED));
            return;
        }
        Block block = hit.getHitBlock();
        Face face = Face.valueOf(hit.getHitBlockFace().name());
        Spot spot = FacePoint.snap(block.getX(), block.getY(), block.getZ(), face,
                hit.getHitPosition().getX(), hit.getHitPosition().getY(), hit.getHitPosition().getZ());
        float[] rot = FacePoint.rotation(face);

        int spotId = ids.getAndDecrement();
        Integer boxId = null;
        Integer patchId = null;
        if (mode == Mode.MODEL) {
            patchId = ids.getAndDecrement();
            double[] c = FacePoint.centre(block.getX(), block.getY(), block.getZ(), face, PATCH_OUTSET);
            visuals.spawnItemDisplay(player, patchId, c[0], c[1], c[2], modelItem(PATCH_MODEL), rot, 2f, 3f, 1f);
            visuals.spawnItemDisplay(player, spotId, spot.x(), spot.y(), spot.z(), modelItem(SPOT_MODEL), rot,
                    SPOT_SIZE, SPOT_SIZE, 1f);
        } else {
            visuals.spawnItemDisplay(player, spotId, spot.x(), spot.y(), spot.z(), new ItemStack(Material.EMERALD),
                    rot, SPOT_SIZE, SPOT_SIZE, SPOT_SIZE);
            if (mode == Mode.BOX) {
                boxId = ids.getAndDecrement();
                visuals.spawnInteraction(player, boxId, spot.x(), spot.y(), spot.z(), BOX_SIZE, BOX_SIZE);
            }
        }
        sessions.put(player.getUniqueId(), new Session(mode, block.getWorld().getUID(),
                block.getX(), block.getY(), block.getZ(), face, spot, spotId, boxId, patchId));
        log.info(MineSpikeLine.place(mode.name().toLowerCase(), block.getX(), block.getY(), block.getZ(),
                face.name(), spot.x(), spot.y(), spot.z(), spotId, boxId));
        if (patchId != null) {
            log.info("MINESPIKE PATCH patchId=" + patchId + " model=" + PATCH_MODEL + " spotModel=" + SPOT_MODEL);
        }
        player.sendMessage(Component.text("Mine spike ON (" + mode.name().toLowerCase() + ") on "
                + block.getType().getKey().getKey() + ", face " + face + ".", NamedTextColor.GREEN));
    }

    /**
     * The test pickaxe. An iron pickaxe whose ATTACK_SPEED is {@code speed}, keeping its default
     * attack damage.
     *
     * <p>PLAN F4: adding ANY modifier through ItemMeta REPLACES the type's defaults, so the defaults
     * are copied first and only the speed entry is replaced. The override reading (PLAN §3) shows
     * {@code AttackSpeedAttributeOverride} leaves this alone, because a pickaxe is not a weapon of ours.
     */
    void givePick(Player player, double speed) {
        ItemStack pick = new ItemStack(Material.IRON_PICKAXE);
        double modifier = speed - VANILLA_BASE_ATTACK_SPEED;
        pick.editMeta(meta -> {
            Multimap<Attribute, AttributeModifier> mods = ArrayListMultimap.create();
            Material.IRON_PICKAXE.asItemType().getDefaultAttributeModifiers()
                    .forEach((attribute, existing) -> {
                        if (!Attribute.ATTACK_SPEED.equals(attribute)) mods.put(attribute, existing);
                    });
            mods.put(Attribute.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED, modifier,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            meta.setAttributeModifiers(mods);
            meta.displayName(Component.text("Spike Pickaxe (speed " + speed + ")"));
            meta.setMaxStackSize(1);
        });
        player.getInventory().addItem(pick);
        float delay = (float) (1.0 / speed * 20.0);
        log.info(MineSpikeLine.pick(speed, modifier, delay));
        player.sendMessage(Component.text("Gave the spike pickaxe: attack speed " + speed
                + ", a full charge every " + delay + " ticks.", NamedTextColor.GREEN));
    }

    void offerPack(Player player) {
        try {
            pack.offer(player);
            player.sendMessage(Component.text("Mining spike pack offered.", NamedTextColor.GREEN));
        } catch (IOException e) {
            log.warning("MINESPIKE PACK failed: " + e);
            player.sendMessage(Component.text("The pack server failed to start: " + e.getMessage(),
                    NamedTextColor.RED));
        }
    }

    // --- the lines -----------------------------------------------------------------------------

    /** A PACKET line, logged after the hop. Main thread. */
    void logPacket(Player player, long seq, String type, Integer entityId) {
        Session s = sessions.get(player.getUniqueId());
        if (s == null) return;
        String entity = entityId == null ? null : entityId + ":" + s.role(entityId);
        log.info(new MineSpikeLine(seq, Bukkit.getCurrentTick(), Src.PACKET, type,
                player.getAttackCooldown(), entity, null).line());
    }

    /** An EVENT line, logged inside the vanilla handler. Main thread. */
    public void logEvent(Player player, String type, Integer entityId, boolean withAim) {
        Session s = sessions.get(player.getUniqueId());
        if (s == null) return;
        String entity = entityId == null ? null : entityId + ":" + s.role(entityId);
        String aim = withAim ? aim(player, s) : null;
        log.info(new MineSpikeLine(nextSeq(), Bukkit.getCurrentTick(), Src.EVENT, type,
                player.getAttackCooldown(), entity, aim).line());
    }

    /** A pack status line. It needs no session: the pack is offered on its own. */
    public void logPackStatus(Player player, String status) {
        log.info(new MineSpikeLine(nextSeq(), Bukkit.getCurrentTick(), Src.EVENT, "PACK_" + status,
                null, null, null).line());
    }

    /** True if {@code block} is the block this player's session sits on. */
    public boolean isSpikedBlock(Player player, Block block) {
        Session s = sessions.get(player.getUniqueId());
        return s != null && block.getWorld().getUID().equals(s.world())
                && block.getX() == s.bx() && block.getY() == s.by() && block.getZ() == s.bz();
    }

    /**
     * Candidate A's input: where the server's own ray, from the server's view of the player's eye
     * and rotation, meets the spiked face. {@code off} if it meets anything else.
     */
    private String aim(Player player, Session s) {
        RayTraceResult hit = player.rayTraceBlocks(REACH, FluidCollisionMode.NEVER);
        if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() == null) return "off";
        Block b = hit.getHitBlock();
        if (b.getX() != s.bx() || b.getY() != s.by() || b.getZ() != s.bz()
                || !hit.getHitBlockFace().name().equals(s.face().name())) return "off";
        return FacePoint.aim(s.spot(), hit.getHitPosition().getX(), hit.getHitPosition().getY(),
                hit.getHitPosition().getZ()).text();
    }

    // --- teardown ------------------------------------------------------------------------------

    /** On quit: the client is leaving, so there is nothing to destroy on it. */
    public void forget(Player player) {
        Session s = sessions.remove(player.getUniqueId());
        if (s != null) log.info(MineSpikeLine.stop(s.mode().name().toLowerCase(), "quit"));
    }

    /** On disable: tear every session down and stop the pack server. */
    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Session s = sessions.get(player.getUniqueId());
            if (s != null) stop(player, s, "disable");
        }
        sessions.clear();
        pack.stop();
    }

    private void stop(Player player, Session s, String reason) {
        sessions.remove(player.getUniqueId());
        visuals.destroy(player, s.ids());
        log.info(MineSpikeLine.stop(s.mode().name().toLowerCase(), reason));
    }

    private static ItemStack modelItem(NamespacedKey model) {
        ItemStack item = new ItemStack(Material.PAPER);
        item.editMeta(meta -> meta.setItemModel(model));
        return item;
    }
}
