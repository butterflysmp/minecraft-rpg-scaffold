package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.build.StoneItems;
import io.github.butterflysmp.rpg.core.mining.Spot;
import io.github.butterflysmp.rpg.paper.menu.Menu;
import io.github.butterflysmp.rpg.paper.mining.MiningService;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.RayTraceResult;

/**
 * The verbs. <b>Each one is the ONLY code that reaches its shipped path, and each says in its line what it drove</b>,
 * so a reader of the log knows which instrument stands between the click Ben would have made and the witness line.
 *
 * <p>The drivers, each read from the pinned jar ({@code PLAN-selftest.md} §1 and §8):
 * <ul>
 *   <li><b>ATTACK</b>: {@code Player#attack} → {@code CraftLivingEntity.attack} → NMS {@code Player.attack}, the
 *       method the attack packet calls. It fires {@code PrePlayerAttackEntityEvent} and
 *       {@code EntityDamageByEntityEvent} into our listeners. It SKIPS the packet's reach and minimum-charge gates,
 *       and the arm swing.</li>
 *   <li><b>USE, SWING, CLICK, HOLD's event</b>: synthetic Bukkit events into every registered listener, ours
 *       included. They SKIP the client's packet and its decode.</li>
 *   <li><b>DROP</b>: {@code HumanEntity#dropItem(false)} → {@code ServerPlayer.drop(Z)}, the Q packet's own call,
 *       which fires {@code PlayerDropItemEvent} in {@code LivingEntity.drop}. It skips the packet's drop-rate limit.</li>
 *   <li><b>CMD</b>: {@code Player#performCommand} → the server's one Brigadier dispatcher. It skips
 *       {@code PlayerCommandPreprocessEvent} and the "issued server command" log line, so it logs its own.</li>
 * </ul>
 *
 * <p>Every method runs on the player's own thread and returns a SKIPPED reason, or empty when the step ran.
 */
final class SelfTestDrivers {

    private static final double REACH = 16.0;

    private final SelfTestRun run;
    private final Keys keys;

    SelfTestDrivers(SelfTestRun run, Keys keys) {
        this.run = run;
        this.keys = keys;
    }

    Optional<String> execute(Player p, Scenario.Step step) {
        String a = step.arg().trim();
        return switch (step.verb()) {
            case CMD -> command(p, a);
            case WAIT -> Optional.empty();   // the run schedules it; it never reaches a driver
            case TRACE -> trace();
            case ATTACK -> attack(p, a);
            case AIM -> aim(p, a);
            case USE -> use(p);
            case SWING -> swing(p);
            case DROP -> drop(p);
            case HOLD -> hold(p, a);
            case CLICK -> click(p, a, item -> plain(item.effectiveName()).startsWith(a), "name^=\"" + a + "\"");
            case CLICK_MATERIAL -> click(p, a, item -> item.getType().name().equalsIgnoreCase(a), "material=" + a);
            case CLOSE -> { p.closeInventory(); run.log("DRIVE close"); yield Optional.empty(); }
            case DUMP -> dump(p, a);
            case PROBE -> probe(p, a);
            case PROBE_INVENTORY -> probeInventory(p, a);
            case PROBE_BLOCKS -> probeBlocks(p, a);
            case PROBE_ENTITIES -> probeEntities(p, a);
            case PROBE_POSITIONS -> probePositions(p, a);
            case PROBE_SELF -> probeSelf(p);
            case DEATH_RULES -> { run.deathRules(); yield Optional.empty(); }
            case SAVE_BLOCKS -> saveBlocks(p, a);
            case EXPECT_CELL -> expectCell(p, a);
            case AFTER -> run.previousRow().equals(a)
                    ? Optional.empty()
                    : Optional.of("it reads the state " + a + " leaves, and " + a + " did not run just before it");
            case AIM_BLOCK -> aimBlock(p, a);
            case AIM_PATCH -> aimPatch(p, a);
            case DIG -> dig(p);
            case MINING_TRACE -> miningTrace(p);
            case NOTE -> { run.log("NOTE " + a); yield Optional.empty(); }
        };
    }

    private Optional<String> command(Player p, String text) {
        run.log("CMD /" + text);
        boolean handled = p.performCommand(text);
        if (!handled) run.log("CMD-UNHANDLED /" + text);   // a fact the dispatcher returned, not a judgement
        return Optional.empty();
    }

    private Optional<String> trace() {
        if (!run.nameplates().tracing()) run.nameplates().toggleTrace();
        run.log("DRIVE trace on=" + run.nameplates().tracing());
        return Optional.empty();
    }

    private Optional<String> attack(Player p, String tag) {
        Optional<Entity> target = nearestTagged(p, tag);
        if (target.isEmpty()) return Optional.of("no entity tagged '" + tag + "' within " + (int) REACH + " blocks");
        Entity e = target.get();
        run.log("DRIVE attack Player#attack target=" + e.getUniqueId() + " " + e.getType().getKey().getKey()
                + " d=" + fmt(p.getLocation().distance(e.getLocation())) + " charge=" + fmt(p.getAttackCooldown()));
        p.attack(e);
        return Optional.empty();
    }

    private Optional<String> aim(Player p, String tag) {
        Optional<Entity> target = nearestTagged(p, tag);
        if (target.isEmpty()) return Optional.of("no entity tagged '" + tag + "' within " + (int) REACH + " blocks");
        Entity e = target.get();
        Location feet = p.getLocation();
        Location eye = e instanceof LivingEntity l ? l.getEyeLocation() : e.getLocation();
        SelfTestAim.Rotation r = SelfTestAim.at(feet.getX(), feet.getY(), feet.getZ(), p.getEyeHeight(),
                eye.getX(), eye.getY(), eye.getZ());
        Location turned = feet.clone();
        turned.setYaw(r.yaw());
        turned.setPitch(r.pitch());
        run.log("DRIVE aim teleportAsync eye->eye target=" + e.getUniqueId() + " " + e.getType().getKey().getKey()
                + " d=" + fmt(horizontal(p, e)) + " yaw=" + fmt(r.yaw()) + " pitch=" + fmt(r.pitch()));
        p.teleportAsync(turned);
        return Optional.empty();
    }

    private Optional<String> use(Player p) {
        ItemStack held = p.getInventory().getItemInMainHand();
        PlayerInteractEvent event = new PlayerInteractEvent(p, Action.RIGHT_CLICK_AIR,
                held.isEmpty() ? null : held, null, BlockFace.SELF, EquipmentSlot.HAND);
        run.log("DRIVE use PlayerInteractEvent RIGHT_CLICK_AIR HAND item=" + itemId(held)
                + " yaw=" + fmt(p.getLocation().getYaw()) + " pitch=" + fmt(p.getLocation().getPitch()));
        Bukkit.getPluginManager().callEvent(event);
        return Optional.empty();
    }

    private Optional<String> swing(Player p) {
        run.log("DRIVE swing PlayerArmSwingEvent HAND item=" + itemId(p.getInventory().getItemInMainHand()));
        Bukkit.getPluginManager().callEvent(new PlayerArmSwingEvent(p, EquipmentSlot.HAND));
        return Optional.empty();
    }

    private Optional<String> drop(Player p) {
        String before = itemId(p.getInventory().getItemInMainHand());
        boolean dropped = p.dropItem(false);
        run.log("DRIVE drop HumanEntity#dropItem(false) item=" + before + " returned=" + dropped
                + " heldAfter=" + itemId(p.getInventory().getItemInMainHand()));
        return Optional.empty();
    }

    /**
     * Select a hotbar slot holding {@code what}. A weapon outside the hotbar is MOVED into an empty hotbar slot first
     * (staging, logged as STAGE); the stone is never moved, because its slot is the player's choice. The slot change
     * is the client's order: {@code PlayerItemHeldEvent} first, then the slot only if no listener cancelled it
     * ({@code ServerGamePacketListenerImpl.handleSetCarriedItem}, read from the jar). The charge reset that follows
     * is vanilla's own, in {@code Player.tick}, whatever changed the slot.
     */
    private Optional<String> hold(Player p, String what) {
        PlayerInventory inv = p.getInventory();
        Predicate<ItemStack> match = switch (what) {
            case "empty" -> item -> item == null || item.isEmpty();
            case "stone" -> item -> item != null && StoneItems.isStone(item, keys);
            default -> item -> item != null && WeaponItems.weaponId(item, keys).filter(what::equals).isPresent();
        };
        int slot = -1;
        for (int i = 0; i < 9 && slot < 0; i++) if (match.test(inv.getItem(i))) slot = i;
        if (slot < 0 && !what.equals("empty") && !what.equals("stone")) {
            int from = -1;
            for (int i = 9; i < 36 && from < 0; i++) if (match.test(inv.getItem(i))) from = i;
            int to = -1;
            for (int i = 0; i < 9 && to < 0; i++) { ItemStack it = inv.getItem(i); if (it == null || it.isEmpty()) to = i; }
            if (from >= 0 && to >= 0) {
                inv.setItem(to, inv.getItem(from));
                inv.setItem(from, null);
                run.log("STAGE moved " + what + " from slot " + from + " to hotbar " + to);
                slot = to;
            }
        }
        if (slot < 0) return Optional.of("no hotbar slot holds " + what);
        int previous = inv.getHeldItemSlot();
        if (previous != slot) {
            PlayerItemHeldEvent event = new PlayerItemHeldEvent(p, previous, slot);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return Optional.of("PlayerItemHeldEvent " + previous + "->" + slot + " was cancelled");
            inv.setHeldItemSlot(slot);
        }
        run.log("DRIVE hold PlayerItemHeldEvent+setHeldItemSlot " + previous + "->" + slot + " item=" + itemId(inv.getItem(slot)));
        return Optional.empty();
    }

    /** A plain left click, routed exactly as the client's is: {@code RpgListeners.onMenuClick} → {@code Menu.handleClick}. */
    private Optional<String> click(Player p, String arg, Predicate<ItemStack> match, String label) {
        InventoryView view = p.getOpenInventory();
        Inventory top = view.getTopInventory();
        if (!(top.getHolder() instanceof Menu)) return Optional.of("no menu is open to click " + label);
        for (int raw = 0; raw < top.getSize(); raw++) {
            ItemStack item = top.getItem(raw);
            if (item == null || item.isEmpty() || !match.test(item)) continue;
            run.log("DRIVE click InventoryClickEvent LEFT PICKUP_ALL slot=" + raw + " " + SelfTestLines.styled(item.effectiveName()));
            Bukkit.getPluginManager().callEvent(new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, raw,
                    ClickType.LEFT, InventoryAction.PICKUP_ALL));
            return Optional.empty();
        }
        return Optional.of("no slot of " + top.getHolder().getClass().getSimpleName() + " matches " + label);
    }

    private Optional<String> dump(Player p, String what) {
        if (what.equals("held")) {
            ItemStack held = p.getInventory().getItemInMainHand();
            run.log("HELD " + itemId(held) + " " + held.getType() + " x" + held.getAmount() + " name="
                    + SelfTestLines.styled(held.effectiveName()) + " lore=" + lore(held));
            return Optional.empty();
        }
        InventoryView view = p.getOpenInventory();
        Inventory top = view.getTopInventory();
        if (!(top.getHolder() instanceof Menu menu)) {
            run.log("SCREEN none (open view type " + view.getType() + ")");
            return Optional.empty();
        }
        run.log("SCREEN " + menu.getClass().getSimpleName() + " title=" + SelfTestLines.styled(view.title()));
        for (int raw = 0; raw < top.getSize(); raw++) {
            ItemStack item = top.getItem(raw);
            if (item == null || item.isEmpty()) continue;
            String name = plain(item.effectiveName());
            // Filler panes carry a blank name; they are chrome, not content, and would bury the row in noise.
            if (name.isBlank() && item.getType().name().endsWith("GLASS_PANE")) continue;
            run.log("SLOT " + raw + " " + item.getType() + " name=" + SelfTestLines.styled(item.effectiveName())
                    + " lore=" + lore(item));
        }
        return Optional.empty();
    }

    private Optional<String> probe(Player p, String tag) {
        List<Entity> tagged = new ArrayList<>();
        for (Entity e : p.getNearbyEntities(REACH, REACH, REACH)) if (e.getScoreboardTags().contains(tag)) tagged.add(e);
        if (tagged.isEmpty()) {
            run.log("PROBE tag=" + tag + " none within " + (int) REACH + " blocks");
            return Optional.empty();
        }
        tagged.sort(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(p.getLocation())));
        for (Entity e : tagged) {
            String hp = run.stats().tracks(e.getUniqueId())
                    ? fmt(run.stats().current(e.getUniqueId())) + "/" + fmt(run.stats().max(e.getUniqueId()))
                    : "untracked";
            run.log("PROBE tag=" + tag + " " + e.getUniqueId() + " " + e.getType().getKey().getKey()
                    + " customHP=" + hp + " fireTicks=" + e.getFireTicks()
                    + " dead=" + (e instanceof LivingEntity l && l.isDead())
                    + " d=" + fmt(horizontal(p, e)) + " dy=" + fmt(e.getLocation().getY() - p.getLocation().getY()));
        }
        return Optional.empty();
    }

    private Optional<String> probeInventory(Player p, String weaponId) {
        List<String> slots = new ArrayList<>();
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && WeaponItems.weaponId(item, keys).filter(weaponId::equals).isPresent()) {
                slots.add(i + "x" + item.getAmount());
            }
        }
        run.log("INVENTORY " + weaponId + " slots=" + slots);
        return Optional.empty();
    }

    private Optional<String> probeBlocks(Player p, String arg) {
        String[] f = arg.split("\\s+");
        if (f.length != 7) return Optional.of("PROBE_BLOCKS wants '<MATERIAL> dx1 dy1 dz1 dx2 dy2 dz2', got '" + arg + "'");
        Material material = Material.matchMaterial(f[0]);
        if (material == null) return Optional.of("unknown material " + f[0]);
        int count = 0;
        for (Block b : box(p, f, 1)) if (b.getType() == material) count++;
        run.log("BLOCKS " + material + " count=" + count + " box=" + String.join(" ", List.of(f).subList(1, 7)));
        return Optional.empty();
    }

    private Optional<String> probeEntities(Player p, String arg) {
        String[] f = arg.split("\\s+");
        if (f.length != 2) return Optional.of("PROBE_ENTITIES wants '<type> <radius>', got '" + arg + "'");
        EntityType type;
        try { type = EntityType.valueOf(f[0].toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return Optional.of("unknown entity type " + f[0]); }
        double r = Double.parseDouble(f[1]);
        long count = p.getNearbyEntities(r, r, r).stream().filter(e -> e.getType() == type).count();
        run.log("ENTITIES " + type + " count=" + count + " radius=" + f[1]);
        return Optional.empty();
    }

    private Optional<String> probeSelf(Player p) {
        String hp = run.stats().tracks(p.getUniqueId())
                ? fmt(run.stats().current(p.getUniqueId())) + "/" + fmt(run.stats().max(p.getUniqueId()))
                : "untracked";
        Location l = p.getLocation();
        run.log("SELF customHP=" + hp + " at=" + String.format(Locale.ROOT, "%.2f %.2f %.2f", l.getX(), l.getY(), l.getZ())
                + " onGround=" + p.isOnGround() + " fall=" + fmt(p.getFallDistance()));
        return Optional.empty();
    }

    private Optional<String> probePositions(Player p, String arg) {
        String[] f = arg.split("\\s+");
        if (f.length != 2) return Optional.of("PROBE_POSITIONS wants '<type> <radius>', got '" + arg + "'");
        EntityType type;
        try { type = EntityType.valueOf(f[0].toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return Optional.of("unknown entity type " + f[0]); }
        double r = Double.parseDouble(f[1]);
        Location feet = p.getLocation();
        List<String> at = new ArrayList<>();
        for (Entity e : p.getNearbyEntities(r, r, r)) {
            if (e.getType() != type) continue;
            Location l = e.getLocation();
            at.add(String.format(Locale.ROOT, "%.2f %.2f %.2f",
                    l.getX() - feet.getX(), l.getY() - feet.getY(), l.getZ() - feet.getZ()));
        }
        run.log("POSITIONS " + type + " n=" + at.size() + " at=[" + String.join("; ", at) + "] radius=" + f[1]);
        return Optional.empty();
    }

    private Optional<String> saveBlocks(Player p, String arg) {
        String[] f = ("x " + arg).split("\\s+");
        if (f.length != 7) return Optional.of("SAVE_BLOCKS wants 'dx1 dy1 dz1 dx2 dy2 dz2', got '" + arg + "'");
        int n = 0;
        for (Block b : box(p, f, 1)) { run.snapshot().saveBlock(b.getState()); n++; }
        run.log("STAGE saved " + n + " blocks for the restore, box=" + arg);
        return Optional.empty();
    }

    private Optional<String> expectCell(Player p, String arg) {
        String[] want = arg.split("\\s+");
        Optional<PlayerProfile> profile = run.profiles().profile(p.getUniqueId());
        String cls = profile.map(PlayerProfile::archetypeId).orElse("?");
        String el = profile.map(PlayerProfile::elementId).orElse("?");
        run.log("CELL class=" + cls + " element=" + el);
        if (want.length == 2 && want[0].equalsIgnoreCase(cls) && want[1].equalsIgnoreCase(el)) return Optional.empty();
        return Optional.of("the Build cell is " + cls + "/" + el + ", and this row is staged for " + arg);
    }

    // --- mining (GATE-mining-auto.md) ---------------------------------------------------------------------------

    /** How far AIM_PATCH looks along the eye for the slot it aims within. The placements are 1.5 blocks away. */
    private static final double SLOT_LOOKUP_REACH = 6.0;

    private Optional<String> aimBlock(Player p, String arg) {
        String[] f = arg.split("\\s+");
        if (f.length != 4) return Optional.of("AIM_BLOCK wants 'dx dy dz FACE', got '" + arg + "'");
        BlockFace face;
        try { face = BlockFace.valueOf(f[3].toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return Optional.of("unknown face " + f[3]); }
        Block b = p.getLocation().getBlock().getRelative(Integer.parseInt(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2]));
        double[] t = SelfTestMiningAim.faceCentre(b.getX(), b.getY(), b.getZ(), face.getModX(), face.getModY(), face.getModZ());
        turnEyeTo(p, t, "DRIVE aim_block teleportAsync eye->face block=" + b.getX() + "," + b.getY() + "," + b.getZ()
                + " " + b.getType().getKey().getKey() + " face=" + face);
        return Optional.empty();
    }

    private Optional<String> aimPatch(Player p, String arg) {
        RayTraceResult hit = p.rayTraceBlocks(SLOT_LOOKUP_REACH);
        if (hit == null || hit.getHitBlock() == null) {
            return Optional.of("the eye meets no block within " + (int) SLOT_LOOKUP_REACH + ", so no slot to aim at");
        }
        Optional<MiningService.PatchView> found = run.mining().peek(p, hit.getHitBlock());
        if (found.isEmpty()) return Optional.of("the looked-at block is under no mining slot");
        MiningService.PatchView view = found.get();
        if (!view.live()) {
            // Regrowing (or never shown): there is no patch to aim within. The aim stays where it is, so a DIG that
            // follows is judged on the slot as a real click would be: NOT_LIVE.
            run.log("DRIVE aim_patch slot=" + view.slotId() + " patch=none aim unchanged");
            return Optional.empty();
        }
        double[] uv;
        try { uv = SelfTestMiningAim.target(arg, view.frame(), view.spots(), view.liveIndex()); }
        catch (IllegalArgumentException e) { return Optional.of(e.getMessage()); }
        double[] w = view.frame().patchToWorld(uv[0], uv[1], 0.0);
        StringBuilder spots = new StringBuilder();
        for (Spot s : view.spots()) spots.append(spots.isEmpty() ? "" : ";").append(s.cu()).append(',').append(s.cv());
        turnEyeTo(p, w, "DRIVE aim_patch teleportAsync eye->patch slot=" + view.slotId() + " size=" + view.sizeId()
                + " spots=" + spots + " live=" + (view.liveIndex() + 1) + "/" + view.spots().size()
                + " target=" + arg.trim() + " pixel=" + String.format(Locale.ROOT, "%.2f,%.2f", uv[0], uv[1]));
        return Optional.empty();
    }

    /** Turns the eye to a world point, as AIM does ({@link SelfTestAim}, from the EYE), and logs where. */
    private void turnEyeTo(Player p, double[] t, String what) {
        Location feet = p.getLocation();
        SelfTestAim.Rotation r = SelfTestAim.at(feet.getX(), feet.getY(), feet.getZ(), p.getEyeHeight(), t[0], t[1], t[2]);
        Location turned = feet.clone();
        turned.setYaw(r.yaw());
        turned.setPitch(r.pitch());
        run.log(what + " at=" + String.format(Locale.ROOT, "%.3f %.3f %.3f", t[0], t[1], t[2])
                + " yaw=" + fmt(r.yaw()) + " pitch=" + fmt(r.pitch()));
        p.teleportAsync(turned);
    }

    /**
     * One click on the block the eye meets, as the dig START reaches the server: {@code BlockDamageEvent} with the
     * face and the main-hand item, into every listener; then the arm swing's server-side reset. The event is the
     * shipped read site (PLAN-mining.md 5.4), so {@code MiningService} reads the charge, the aim and the item from the
     * player exactly as for Ben's click. The DRIVE line's {@code cooldown} is read before the event, the value the
     * MINING click line must repeat.
     */
    private Optional<String> dig(Player p) {
        var rangeAttribute = p.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
        double range = rangeAttribute == null ? 4.5 : rangeAttribute.getValue();
        RayTraceResult hit = p.rayTraceBlocks(range);
        if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() == null) {
            return Optional.of("the eye meets no block within the block-interaction range " + fmt(range));
        }
        Block b = hit.getHitBlock();
        ItemStack item = p.getInventory().getItemInMainHand();
        var speedAttribute = p.getAttribute(Attribute.ATTACK_SPEED);
        float before = p.getAttackCooldown();
        BlockDamageEvent event = new BlockDamageEvent(p, b, hit.getHitBlockFace(), item, false);
        Bukkit.getPluginManager().callEvent(event);
        p.resetCooldown();
        run.log("DRIVE dig BlockDamageEvent block=" + b.getX() + "," + b.getY() + "," + b.getZ() + " face="
                + hit.getHitBlockFace() + " item=" + itemId(item) + " cooldown=" + String.format(Locale.ROOT, "%.4f", before)
                + " attack_speed=" + (speedAttribute == null ? "?" : fmt(speedAttribute.getValue()))
                + " cancelled=" + event.isCancelled() + " then resetCooldown cooldown=" + String.format(Locale.ROOT, "%.4f", p.getAttackCooldown()));
        return Optional.empty();
    }

    private Optional<String> miningTrace(Player p) {
        MiningService mining = run.mining();
        if (!mining.isTracing(p)) mining.trace(p);   // the shipped toggle, which logs its own MINING trace line
        run.log("DRIVE mining_trace on=" + mining.isTracing(p));
        return Optional.empty();
    }

    // --- helpers -------------------------------------------------------------------------------------------------

    private static Optional<Entity> nearestTagged(Player p, String tag) {
        return p.getNearbyEntities(REACH, REACH, REACH).stream()
                .filter(e -> e.getScoreboardTags().contains(tag) && !(e instanceof LivingEntity l && l.isDead()))
                .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(p.getLocation())));
    }

    /** The blocks of a box given as offsets from the player's block, fields {@code f[from..from+5]}. */
    private static List<Block> box(Player p, String[] f, int from) {
        Block origin = p.getLocation().getBlock();
        int x1 = Integer.parseInt(f[from]), y1 = Integer.parseInt(f[from + 1]), z1 = Integer.parseInt(f[from + 2]);
        int x2 = Integer.parseInt(f[from + 3]), y2 = Integer.parseInt(f[from + 4]), z2 = Integer.parseInt(f[from + 5]);
        List<Block> blocks = new ArrayList<>();
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    blocks.add(origin.getRelative(x, y, z));
        return blocks;
    }

    private String itemId(ItemStack item) {
        if (item == null || item.isEmpty()) return "empty";
        if (StoneItems.isStone(item, keys)) return "stone";
        return WeaponItems.weaponId(item, keys).orElse(item.getType().getKey().getKey());
    }

    private static String lore(ItemStack item) {
        List<Component> lines = item.lore();
        if (lines == null || lines.isEmpty()) return "[]";
        List<String> out = new ArrayList<>();
        for (Component line : lines) out.add(SelfTestLines.styled(line));
        return "[" + String.join("; ", out) + "]";
    }

    private static String plain(Component c) {
        return c == null ? "" : PlainTextComponentSerializer.plainText().serialize(c);
    }

    private static double horizontal(Player p, Entity e) {
        double dx = p.getLocation().getX() - e.getLocation().getX();
        double dz = p.getLocation().getZ() - e.getLocation().getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }
}
