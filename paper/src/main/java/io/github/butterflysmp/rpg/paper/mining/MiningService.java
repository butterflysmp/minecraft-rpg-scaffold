package io.github.butterflysmp.rpg.paper.mining;

import io.github.butterflysmp.rpg.core.mining.BlockPos;
import io.github.butterflysmp.rpg.core.mining.Click;
import io.github.butterflysmp.rpg.core.mining.ClickOutcome;
import io.github.butterflysmp.rpg.core.mining.FaceFrame;
import io.github.butterflysmp.rpg.core.hud.ActionBarHolds;
import io.github.butterflysmp.rpg.core.mining.MiningBook;
import io.github.butterflysmp.rpg.core.mining.MiningNotices;
import io.github.butterflysmp.rpg.core.mining.MiningTraceLine;
import io.github.butterflysmp.rpg.core.mining.OreDefinition;
import io.github.butterflysmp.rpg.core.mining.OreRegistry;
import io.github.butterflysmp.rpg.core.mining.Patch;
import io.github.butterflysmp.rpg.core.mining.PatchPoint;
import io.github.butterflysmp.rpg.core.mining.PayoutRule;
import io.github.butterflysmp.rpg.core.mining.Slot;
import io.github.butterflysmp.rpg.core.mining.SlotRegistry;
import io.github.butterflysmp.rpg.core.mining.Spot;
import io.github.butterflysmp.rpg.core.mining.VeinSize;
import io.github.butterflysmp.rpg.core.mining.VeinSizeRegistry;
import io.github.butterflysmp.rpg.core.mining.WallFace;
import io.github.butterflysmp.rpg.paper.health.PopupEntityIds;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Mining slice 1's paper side (PLAN-mining.md 5.4): the BlockDamageEvent input, the per-player
 * visuals, the payout, the sounds and particle, regrowth, and the dev commands' work.
 *
 * <p><b>THREADING.</b> Every method runs on the main thread: Bukkit events, Brigadier command
 * execution, and the regrowth task on the player's entity scheduler. There is NO packet listener and
 * nothing here runs on a Netty thread. The rules themselves live in core ({@link MiningBook},
 * {@link io.github.butterflysmp.rpg.core.mining.Patch}); this class only adapts them.
 *
 * <p><b>The read site is {@link BlockDamageEvent}</b> (the seat's ruling 1). It fires once per click,
 * from the dig START, before the swing's reset, so no swing is ever a read site and the second swing
 * of a click is never looked at. It is not {@code ignoreCancelled}: master's stone handler cancels
 * the same event when a stone is held, and the patch block must stay protected regardless.
 */
public final class MiningService {

    /** Off the wall along the normal, in blocks: the seat's ruling 6. M-LOOK reads how they look. */
    static final double PATCH_OUTSET = 0.0005;
    static final double SPOT_OUTSET = 0.001;
    /** A spot's 3 x 3 pixel mark: 3/16 of a block. */
    static final float SPOT_SCALE = 3f / 16f;
    /** Click reach for the dev placement command, in blocks. */
    static final double PLACE_REACH = 5.0;

    private final Logger log;
    private final Scheduler scheduler;
    private final OreRegistry ores;
    private final VeinSizeRegistry sizes;
    private final SlotRegistry slots;
    private final MiningBook book;
    private final MiningVisuals visuals;
    private final MiningPack pack;
    private final PopupEntityIds ids;
    private final boolean packOnJoin;
    /** Who owns each player's action bar; DENIED takes it for MiningNotices.DENIED_HOLD_TICKS (S1-c). */
    private final ActionBarHolds<Component> holds;
    private final Set<UUID> tracing = new HashSet<>();
    /** What each player is currently shown, per slot id. */
    private final Map<UUID, Map<String, Shown>> shown = new HashMap<>();

    /** The display ids one player sees at one slot: the patch, and each revealed spot by index. */
    private record Shown(int patchId, List<Integer> spotIds) {
        int[] all() {
            int[] out = new int[spotIds.size() + 1];
            out[0] = patchId;
            for (int i = 0; i < spotIds.size(); i++) out[i + 1] = spotIds.get(i);
            return out;
        }
    }

    public MiningService(Logger log, Scheduler scheduler, OreRegistry ores, VeinSizeRegistry sizes,
                         SlotRegistry slots, MiningBook book, MiningPack pack, PopupEntityIds ids,
                         boolean packOnJoin, ActionBarHolds<Component> holds) {
        this.log = log;
        this.scheduler = scheduler;
        this.ores = ores;
        this.sizes = sizes;
        this.slots = slots;
        this.book = book;
        this.visuals = new MiningVisuals();
        this.pack = pack;
        this.ids = ids;
        this.packOnJoin = packOnJoin;
        this.holds = holds;
    }

    // --- Events -------------------------------------------------------------------------------

    /** The input. Cancels the dig on every slot block for every player, then judges the click. */
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        String world = block.getWorld().getUID().toString();
        Optional<Slot> found = slots.at(world, new BlockPos(block.getX(), block.getY(), block.getZ()));
        if (found.isEmpty()) return;
        event.setCancelled(true);
        Slot slot = found.get();
        if (event.getBlockFace() != toBlockFace(slot.frame().face())) return;   // the side of the wall

        Player player = event.getPlayer();
        Optional<OreDefinition> ore = ores.find(slot.oreId());
        Optional<VeinSize> size = sizes.find(slot.sizeId());
        if (ore.isEmpty() || size.isEmpty()) return;   // its content failed to load; boot named it
        PayoutRule rule = ore.get().payoutRule(slot.sizeId());

        // Read everything the click needs NOW, before anything else can move the ticker.
        float cooldown = player.getAttackCooldown();
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();
        Optional<PatchPoint> aim = slot.frame().worldToPatch(eye.getX(), eye.getY(), eye.getZ(),
                dir.getX(), dir.getY(), dir.getZ());
        boolean pickaxe = Tag.ITEMS_PICKAXES.isTagged(event.getItemInHand().getType());
        long tick = Bukkit.getCurrentTick();

        show(player, slot);   // a click on a patch the player was never shown still has one to judge
        Optional<Patch> before = book.patch(player.getUniqueId(), slot, size.get(), rule);
        int liveBefore = before.map(Patch::liveIndex).orElse(-1);
        int spotCount = before.map(p -> p.layout().spots().size()).orElse(0);

        ClickOutcome outcome = book.click(player.getUniqueId(), slot, size.get(), rule,
                ore.get().regrowthTicks(), new Click(cooldown, aim, pickaxe, tick));
        trace(player, MiningTraceLine.click(player.getName(), slot.id(), name(outcome),
                liveBefore, spotCount, cooldown, aim, tick));

        switch (outcome) {
            case ClickOutcome.Hit hit -> onHit(player, slot, ore.get(), before.orElseThrow(), hit);
            case ClickOutcome.Broke broke -> onBroke(player, slot, ore.get(), broke);
            case ClickOutcome.Miss miss -> playAt(player, slot, ore.get().missSound());
            case ClickOutcome.Denied denied -> deny(player, tick);
            case ClickOutcome.Uncharged uncharged -> { }   // plays nothing (the seat's ruling 4)
            case ClickOutcome.NotLive notLive -> { }       // plays nothing
        }
    }

    /**
     * DENIED: the red notice takes the player's action bar for {@link MiningNotices#DENIED_HOLD_TICKS}
     * (Ben, 2026-11-14: 1.5 s). Sent at once here; the stats bar re-sends it on each of its beats
     * inside the hold and draws the stats line again on the first beat at or after the end. A second
     * DENIED inside the hold REPLACES it, so the hold restarts from that click. Main thread (a Bukkit
     * event), the same thread as the stats bar's loop on Paper.
     */
    private void deny(Player player, long tick) {
        Component text = Component.text(MiningNotices.DENIED_TEXT, NamedTextColor.RED);
        ActionBarHolds.Hold<Component> hold = holds.hold(player.getUniqueId(), text, "denied", tick,
                MiningNotices.DENIED_HOLD_TICKS, tracing.contains(player.getUniqueId()));
        player.sendActionBar(text);
        trace(player, MiningTraceLine.hold(player.getName(), hold.reason(), hold.start(), hold.until()));
    }

    /** The other break route (F-M5): a creative break, or a STOP on a block left in destroyPos. */
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (slots.at(block.getWorld().getUID().toString(),
                new BlockPos(block.getX(), block.getY(), block.getZ())).isPresent()) {
            event.setCancelled(true);
        }
    }

    public void onJoin(Player player) {
        if (packOnJoin) offerPack(player);
    }

    /**
     * Logs the client's answer to OUR pack's offer, always (the gate's M-PACK witness, as the spike's
     * MS7 was). Another pack's status is not ours to log.
     */
    public void onPackStatus(Player player, UUID packId, String status) {
        packStatusLine(player.getName(), packId, status).ifPresent(log::info);
    }

    /** The status line for our pack, or empty for anyone else's. */
    static Optional<String> packStatusLine(String player, UUID packId, String status) {
        if (!MiningPack.PACK_ID.equals(packId)) return Optional.empty();
        return Optional.of("MINING pack status player=" + player + " status=" + status);
    }

    /** The client drops its fake entities on disconnect; forget what it was shown. State is kept. */
    public void onQuit(Player player) {
        shown.remove(player.getUniqueId());
        book.forgetSession(player.getUniqueId());   // its entity tasks are dropped with it (F8c)
    }

    /** Shows each slot whose patch display stands in this chunk. Main thread (a synchronous event). */
    public void onChunkLoad(Player player, Chunk chunk) {
        for (Slot slot : slots.inChunk(chunk.getWorld().getUID().toString(), chunk.getX(), chunk.getZ())) {
            if (isHomeChunk(slot, chunk)) show(player, slot);
        }
    }

    /**
     * Forgets what the player was shown in this chunk. This is also what clears {@code shown} when the
     * client drops its entities on a respawn or a dimension change: both remove the player from the old
     * level's chunk loader, which fires this event for every sent chunk, naming the OLD world, and the
     * re-add sends each chunk again through {@link #onChunkLoad} (PLAN F8a/F8b, the seat's D2).
     */
    public void onChunkUnload(Player player, Chunk chunk) {
        for (Slot slot : slots.inChunk(chunk.getWorld().getUID().toString(), chunk.getX(), chunk.getZ())) {
            if (isHomeChunk(slot, chunk)) hide(player, slot);
        }
    }

    /** Tears every display down, and stops the pack server. Called from onDisable. */
    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Map<String, Shown> mine = shown.remove(player.getUniqueId());
            if (mine != null) for (Shown s : mine.values()) visuals.destroy(player, s.all());
        }
        pack.stop();
    }

    // --- Dev commands (Permissions.DEV, wired in RpgCommand) ---------------------------------------

    /** {@code /rpg mining place <ore> <size>} on the looked-at wall face. @return the message to show */
    public Component place(Player player, String oreId, String sizeId) {
        Optional<OreDefinition> ore = ores.find(oreId);
        if (ore.isEmpty()) return refuse(player, "No ore '" + oreId + "' is loaded.");
        Optional<VeinSize> size = sizes.find(sizeId);
        if (size.isEmpty()) return refuse(player, "No vein size '" + sizeId + "' is loaded.");
        if (!ore.get().sizes().contains(sizeId)) return refuse(player, "Ore '" + oreId + "' does not list size '" + sizeId + "'.");

        RayTraceResult hit = player.rayTraceBlocks(PLACE_REACH);
        if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() == null) {
            return refuse(player, "Look at a wall within " + (int) PLACE_REACH + " blocks.");
        }
        WallFace face = toWallFace(hit.getHitBlockFace());
        if (face == null) return refuse(player, "A patch goes on a wall (north, south, east or west face), not "
                + hit.getHitBlockFace().name().toLowerCase() + ".");
        Block anchor = hit.getHitBlock();
        World world = anchor.getWorld();
        FaceFrame frame = new FaceFrame(anchor.getX(), anchor.getY(), anchor.getZ(), face,
                size.get().width(), size.get().height());
        for (BlockPos b : frame.footprint()) {
            Block wall = world.getBlockAt(b.x(), b.y(), b.z());
            if (!wall.getType().isSolid()) return refuse(player, "Block " + coords(b) + " is not solid.");
            Block front = world.getBlockAt(b.x() + face.nx, b.y(), b.z() + face.nz);
            if (!front.isPassable()) return refuse(player, "The face of block " + coords(b) + " is covered.");
        }
        Slot slot = Slot.of(world.getUID().toString(), frame, oreId, sizeId);
        try {
            slots.place(slot);
        } catch (IllegalArgumentException overlap) {
            return refuse(player, overlap.getMessage());
        }
        log.info("MINING place slot=" + slot.id() + " ore=" + oreId + " size=" + sizeId
                + " footprint=" + footprintText(frame) + " by=" + player.getName());
        for (Player viewer : world.getPlayers()) {
            if (viewer.isChunkSent(homeChunkKey(slot))) show(viewer, slot);
        }
        return Component.text("Placed " + oreId + " " + sizeId + " at " + slot.id() + ".", NamedTextColor.GREEN);
    }

    /** {@code /rpg mining remove}: the slot under the crosshair. */
    public Component remove(Player player) {
        RayTraceResult hit = player.rayTraceBlocks(PLACE_REACH);
        if (hit == null || hit.getHitBlock() == null) return error("Look at a placed patch's wall.");
        Block b = hit.getHitBlock();
        Optional<Slot> slot = slots.at(b.getWorld().getUID().toString(), new BlockPos(b.getX(), b.getY(), b.getZ()));
        if (slot.isEmpty()) return error("That block is not under a placed patch.");
        for (Player viewer : Bukkit.getOnlinePlayers()) hide(viewer, slot.get());
        slots.remove(slot.get().id());
        book.clearSlot(slot.get().id());
        log.info("MINING remove slot=" + slot.get().id() + " by=" + player.getName());
        return Component.text("Removed " + slot.get().id() + ".", NamedTextColor.GREEN);
    }

    /** {@code /rpg mining regrow}: ends the caller's regrowth now, for the gate. */
    public Component regrow(Player player) {
        book.clearRegrowth(player.getUniqueId());
        int shownNow = 0;
        for (Slot slot : slots.all()) {
            if (player.getWorld().getUID().toString().equals(slot.world()) && player.isChunkSent(homeChunkKey(slot))) {
                if (show(player, slot)) shownNow++;
            }
        }
        log.info("MINING regrow-all player=" + player.getName() + " shown=" + shownNow);
        return Component.text("Your patches regrew (" + shownNow + " shown).", NamedTextColor.GREEN);
    }

    /** {@code /rpg mining trace}: toggles the caller's MINING log lines. */
    public Component trace(Player player) {
        boolean on = tracing.add(player.getUniqueId());
        if (!on) tracing.remove(player.getUniqueId());
        log.info("MINING trace player=" + player.getName() + " on=" + on);
        return Component.text("Mining trace " + (on ? "ON" : "OFF") + ".", NamedTextColor.GREEN);
    }

    /** {@code /rpg mining pack}, and the join toggle. */
    public void offerPack(Player player) {
        try {
            pack.offer(player);
        } catch (IOException e) {
            log.warning("MINING pack offer failed for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(error("The dev pack server could not start: " + e.getMessage()));
        }
    }

    // --- Rendering ------------------------------------------------------------------------------

    /** Spawns the player's patch at this slot if they are not already shown it. @return true if shown now */
    private boolean show(Player player, Slot slot) {
        Map<String, Shown> mine = shown.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        if (mine.containsKey(slot.id())) return false;
        Optional<OreDefinition> ore = ores.find(slot.oreId());
        Optional<VeinSize> size = sizes.find(slot.sizeId());
        if (ore.isEmpty() || size.isEmpty()) return false;
        Optional<Patch> patch = book.patch(player.getUniqueId(), slot, size.get(), ore.get().payoutRule(slot.sizeId()));
        if (patch.isEmpty()) {
            // REGROWING. Make sure ONE task on THIS session's Player will show it at regrow_at (the
            // seat's D1: a quit drops the old task, so a rejoin's chunk load must re-arm it).
            ensureRegrowthTask(player, slot);
            return false;
        }

        FaceFrame frame = slot.frame();
        float[] rot = frame.face().displayRotation();
        int patchId = ids.next();
        visuals.spawn(player, patchId, frame.centre(PATCH_OUTSET), modelItem(ore.get().patchModel()), rot,
                frame.width(), frame.height(), 1f);
        List<Integer> spotIds = new ArrayList<>();
        Patch p = patch.get();
        for (int i = 0; i <= p.liveIndex() && i < p.layout().spots().size(); i++) {
            int id = ids.next();
            String model = i < p.liveIndex() ? ore.get().crackModel() : ore.get().spotModel();
            visuals.spawn(player, id, spotAt(frame, p.layout().spots().get(i)), modelItem(model), rot,
                    SPOT_SCALE, SPOT_SCALE, 1f);
            spotIds.add(id);
        }
        mine.put(slot.id(), new Shown(patchId, spotIds));
        trace(player, MiningTraceLine.show(player.getName(), slot.id(), p.layout().spots(), p.liveIndex(),
                Bukkit.getCurrentTick()));
        return true;
    }

    private void hide(Player player, Slot slot) {
        Map<String, Shown> mine = shown.get(player.getUniqueId());
        if (mine == null) return;
        Shown s = mine.remove(slot.id());
        if (s != null) visuals.destroy(player, s.all());
    }

    private void onHit(Player player, Slot slot, OreDefinition ore, Patch patch, ClickOutcome.Hit hit) {
        Shown s = shown.getOrDefault(player.getUniqueId(), Map.of()).get(slot.id());
        if (s == null) return;
        visuals.setItem(player, s.spotIds().get(hit.crackedIndex()), modelItem(ore.crackModel()));
        int id = ids.next();
        visuals.spawn(player, id, spotAt(slot.frame(), patch.layout().spots().get(hit.revealedIndex())),
                modelItem(ore.spotModel()), slot.frame().face().displayRotation(), SPOT_SCALE, SPOT_SCALE, 1f);
        s.spotIds().add(id);
    }

    private void onBroke(Player player, Slot slot, OreDefinition ore, ClickOutcome.Broke broke) {
        hide(player, slot);
        Location centre = centreLocation(player.getWorld(), slot);
        playAt(player, slot, ore.breakSound());
        Particle particle = Registry.PARTICLE_TYPE.get(NamespacedKey.fromString(ore.breakParticle()));
        if (particle != null) player.spawnParticle(particle, centre, 20);

        // THE PAYOUT. A plain vanilla item; what does not fit drops at the player's feet, as vanilla
        // does (the seat's ruling, 2026-11-13). Raw iron stacks to 64 and a break pays at most 17.
        int total = broke.payout().total();
        int dropped = 0;
        Material item = Material.matchMaterial(ore.payoutItem());
        if (item != null && total > 0) {
            Map<Integer, ItemStack> left = player.getInventory().addItem(new ItemStack(item, total));
            for (ItemStack rest : left.values()) {
                dropped += rest.getAmount();
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
            }
        }
        long regrowAt = book.regrowAt(player.getUniqueId(), slot.id()).orElse(Bukkit.getCurrentTick());
        trace(player, MiningTraceLine.breakLine(player.getName(), slot.id(), ore.payoutItem(), broke.payout(),
                total - dropped, dropped, regrowAt));
        ensureRegrowthTask(player, slot);
    }

    /** Schedules the regrowth task unless one is already pending for this player and slot. */
    private void ensureRegrowthTask(Player player, Slot slot) {
        OptionalLong token = book.claimRegrowthTask(player.getUniqueId(), slot.id());
        if (token.isPresent()) scheduleRegrowth(player, slot, token.getAsLong());
    }

    /**
     * Respawns the patch when its regrowth ends, if the player is still here to see it. The task
     * acts only while its token is current: a quit or a slot removal makes it stale.
     */
    private void scheduleRegrowth(Player player, Slot slot, long token) {
        long regrowAt = book.regrowAt(player.getUniqueId(), slot.id()).orElse(Bukkit.getCurrentTick());
        long delay = Math.max(1L, regrowAt - Bukkit.getCurrentTick());
        scheduler.onEntityLater(player, () -> {
            if (!book.isCurrentRegrowthTask(player.getUniqueId(), slot.id(), token)) return;   // stale
            if (book.regrowAt(player.getUniqueId(), slot.id()).isPresent()) {
                scheduleRegrowth(player, slot, token);   // ran a tick early: wait out the rest, same token
                return;
            }
            book.releaseRegrowthTask(player.getUniqueId(), slot.id(), token);
            if (!player.isOnline() || slots.find(slot.id()).isEmpty()) return;
            if (player.getWorld().getUID().toString().equals(slot.world())
                    && player.isChunkSent(homeChunkKey(slot)) && show(player, slot)) {
                Patch p = currentPatch(player, slot);
                trace(player, MiningTraceLine.regrow(player.getName(), slot.id(),
                        p == null ? 0 : p.layout().spots().size(), Bukkit.getCurrentTick()));
            }
        }, delay);
    }

    private Patch currentPatch(Player player, Slot slot) {
        Optional<OreDefinition> ore = ores.find(slot.oreId());
        Optional<VeinSize> size = sizes.find(slot.sizeId());
        if (ore.isEmpty() || size.isEmpty()) return null;
        return book.patch(player.getUniqueId(), slot, size.get(), ore.get().payoutRule(slot.sizeId())).orElse(null);
    }

    // --- Helpers -------------------------------------------------------------------------------

    private void trace(Player player, String line) {
        if (tracing.contains(player.getUniqueId())) log.info(line);
    }

    private void playAt(Player player, Slot slot, String sound) {
        // To this one player only: Player#playSound, not World#playSound.
        player.playSound(centreLocation(player.getWorld(), slot), sound, 1f, 1f);
    }

    private static double[] spotAt(FaceFrame frame, Spot spot) {
        return frame.patchToWorld(spot.cu() + 0.5, spot.cv() + 0.5, SPOT_OUTSET);
    }

    private static Location centreLocation(World world, Slot slot) {
        double[] c = slot.frame().centre(0.5);
        return new Location(world, c[0], c[1], c[2]);
    }

    /** The chunk the patch display stands in: the chunk of the footprint face's centre. */
    private static boolean isHomeChunk(Slot slot, Chunk chunk) {
        double[] c = slot.frame().centre(PATCH_OUTSET);
        return ((int) Math.floor(c[0]) >> 4) == chunk.getX() && ((int) Math.floor(c[2]) >> 4) == chunk.getZ();
    }

    private static long homeChunkKey(Slot slot) {
        double[] c = slot.frame().centre(PATCH_OUTSET);
        return Chunk.getChunkKey((int) Math.floor(c[0]) >> 4, (int) Math.floor(c[2]) >> 4);
    }

    private static ItemStack modelItem(String model) {
        ItemStack item = new ItemStack(Material.PAPER);
        item.editMeta(meta -> meta.setItemModel(NamespacedKey.fromString(model)));
        return item;
    }

    static String name(ClickOutcome outcome) {
        return switch (outcome) {
            case ClickOutcome.Hit h -> "HIT";
            case ClickOutcome.Broke b -> "BROKE";
            case ClickOutcome.Miss m -> "MISS";
            case ClickOutcome.Uncharged u -> "UNCHARGED";
            case ClickOutcome.Denied d -> "DENIED";
            case ClickOutcome.NotLive n -> "NOT_LIVE";
        };
    }

    static WallFace toWallFace(BlockFace face) {
        return switch (face) {
            case NORTH -> WallFace.NORTH;
            case SOUTH -> WallFace.SOUTH;
            case EAST -> WallFace.EAST;
            case WEST -> WallFace.WEST;
            default -> null;
        };
    }

    static BlockFace toBlockFace(WallFace face) {
        return switch (face) {
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case EAST -> BlockFace.EAST;
            case WEST -> BlockFace.WEST;
        };
    }

    private static String coords(BlockPos b) {
        return b.x() + "," + b.y() + "," + b.z();
    }

    private static String footprintText(FaceFrame frame) {
        List<String> parts = new ArrayList<>();
        for (BlockPos b : frame.footprint()) parts.add(coords(b));
        return String.join(";", parts);
    }

    /**
     * A {@code /rpg mining place} refusal: logged ALWAYS as {@code MINING place-refused} with the chat
     * text verbatim (S1-c, so M-PLACE's refusals are a log reading), then shown in red.
     */
    private Component refuse(Player player, String text) {
        log.info(MiningTraceLine.placeRefused(player.getName(), text));
        return error(text);
    }

    private static Component error(String text) {
        return Component.text(text, NamedTextColor.RED);
    }
}
