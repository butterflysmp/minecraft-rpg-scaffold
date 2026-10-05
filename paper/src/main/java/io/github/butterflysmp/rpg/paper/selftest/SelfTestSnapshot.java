package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.paper.health.MobNameplateManager;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Everything a run changes, captured before its first step and put back after its last (the seat's safety list:
 * XP, gamemode, inventory, position, level, gamerules, time -- plus the cell, mana, the trace and any blocks a
 * scenario saved).
 *
 * <p><b>The level is the XP</b>: a level is derived from lifetime XP, so restoring the XP restores it. That write
 * goes through {@code ProfileService.setLifetimeXp}, the one sanctioned lowering, and it WRITES TO DISK, as
 * {@code /rpg playerxp} does.
 *
 * <p><b>Every mob a scenario makes carries the scoreboard tag {@value #MOB_TAG}</b> (the YAML's summon and
 * {@code data merge} lines add it), and the restore removes every such mob near the player.
 */
final class SelfTestSnapshot {

    static final String MOB_TAG = "selftest";

    private final UUID id;
    private final long lifetimeXp;
    private final GameMode gameMode;
    private final ItemStack[] contents;
    private final int heldSlot;
    private final Location location;
    private final Boolean spawnMobs;
    private final long time;
    private final boolean trace;
    private final double mana;
    private final String classId;
    private final String elementId;
    /** First-saved state per block, in save order; restored in reverse so an overlap ends at the oldest. */
    private final Map<Location, BlockState> blocks = new LinkedHashMap<>();

    private SelfTestSnapshot(Player p, ProfileService profiles, ResourcePool resources, MobNameplateManager nameplates) {
        this.id = p.getUniqueId();
        PlayerProfile profile = profiles.profile(id).orElse(null);
        this.lifetimeXp = profile == null ? -1 : profile.lifetimeXp();
        this.classId = profile == null ? null : profile.archetypeId();
        this.elementId = profile == null ? null : profile.elementId();
        this.gameMode = p.getGameMode();
        ItemStack[] raw = p.getInventory().getContents();
        this.contents = new ItemStack[raw.length];
        for (int i = 0; i < raw.length; i++) contents[i] = raw[i] == null ? null : raw[i].clone();
        this.heldSlot = p.getInventory().getHeldItemSlot();
        this.location = p.getLocation().clone();
        World world = p.getWorld();
        this.spawnMobs = world.getGameRuleValue(GameRules.SPAWN_MOBS);
        this.time = world.getTime();
        this.trace = nameplates.tracing();
        this.mana = resources.current(id, ResourceCost.DEFAULT_RESOURCE);
    }

    static SelfTestSnapshot take(Player p, ProfileService profiles, ResourcePool resources, MobNameplateManager nameplates) {
        return new SelfTestSnapshot(p, profiles, resources, nameplates);
    }

    boolean profileLoaded() {
        return lifetimeXp >= 0;
    }

    void saveBlock(BlockState state) {
        blocks.putIfAbsent(state.getLocation(), state);
    }

    /**
     * Put it all back. Runs on the player's own thread (a continuation, the stop command, or the quit event, whose
     * writes persist: the save runs after it). Returns the line that says what was restored.
     */
    String restore(Player p, ProfileService profiles, ResourcePool resources, MobNameplateManager nameplates,
                   Scheduler scheduler) {
        p.closeInventory();
        int mobs = 0;
        for (Entity e : p.getNearbyEntities(48, 48, 48)) {
            if (e instanceof Player || !e.getScoreboardTags().contains(MOB_TAG)) continue;
            scheduler.onEntity(e, e::remove);
            mobs++;
        }
        List<BlockState> reverse = new ArrayList<>(blocks.values());
        java.util.Collections.reverse(reverse);
        for (BlockState state : reverse) state.update(true, false);

        p.getInventory().setContents(contents);
        p.getInventory().setHeldItemSlot(heldSlot);
        p.setGameMode(gameMode);
        p.teleportAsync(location);

        boolean xp = profileLoaded() && profiles.setLifetimeXp(id, lifetimeXp);
        boolean cell = classId == null || profiles.setCell(id, classId, elementId);
        resources.setCurrent(id, ResourceCost.DEFAULT_RESOURCE, mana);
        // No setter for the trace: toggle until it reads what it read before (at most once).
        if (nameplates.tracing() != trace) nameplates.toggleTrace();

        World world = location.getWorld();
        scheduler.onGlobal(() -> {
            if (spawnMobs != null) world.setGameRule(GameRules.SPAWN_MOBS, spawnMobs);
            world.setTime(time);
        });
        return "RESTORED xp=" + (xp ? lifetimeXp : "NOT-RESTORED") + " cell=" + (cell ? classId + "/" + elementId : "NOT-RESTORED")
                + " gamemode=" + gameMode + " held=" + heldSlot + " mana=" + String.format("%.1f", mana)
                + " trace=" + nameplates.tracing() + " spawn_mobs=" + spawnMobs + " time=" + time
                + " blocks=" + blocks.size() + " mobsRemoved=" + mobs;
    }
}
