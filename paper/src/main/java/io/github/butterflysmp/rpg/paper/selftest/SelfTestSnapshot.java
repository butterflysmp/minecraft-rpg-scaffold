package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.paper.health.MobNameplateManager;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import java.util.List;
import java.util.UUID;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
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
 *
 * <p><b>And every experience orb near the player</b> (the seat, 2026-11-06). A staged kill drops orbs, and an orb
 * picked up AFTER the restore is a Lifetime XP gain the restore never sees: GATE-selftest.md ST2's 38cc9903 reading
 * found the next run's snapshot at {@code xp=4}. An orb carries no tag to say who dropped it, so the restore takes
 * every orb in range; on the dev world that is the instrument's own.
 */
final class SelfTestSnapshot {

    static final String MOB_TAG = "selftest";

    /** What the restore removes: a tagged mob, or an experience orb. Never a player. Pure, so it is a unit row. */
    static boolean removedOnRestore(boolean isPlayer, boolean tagged, boolean experienceOrb) {
        return !isPlayer && (tagged || experienceOrb);
    }

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
    /**
     * First-saved state per block, in save order; restored in reverse so an overlap ends at the oldest. Drained at
     * every unchained row start ({@link #restoreRowBlocks}), not only at the end of the run: run 2 of the
     * {@code 61e722f2} boot drowned its player because each Sunder row staged from the floor the last one left.
     */
    private final SelfTestStaging.RowBlocks<Location, BlockState> blocks = new SelfTestStaging.RowBlocks<>();

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
        blocks.save(state.getLocation(), state);
    }

    /** Puts back the blocks the last chain of rows saved, newest first. Returns how many. */
    int restoreRowBlocks() {
        List<BlockState> newestFirst = blocks.drainNewestFirst();
        for (BlockState state : newestFirst) state.update(true, false);
        return newestFirst.size();
    }

    /** Where the player stood when the run began: every unchained row is staged from here again. */
    Location start() {
        return location.clone();
    }

    /** The inventory and held slot the run began with, put back at every gate boundary (DH1's full hotbar). */
    void restoreInventory(Player p) {
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) copy[i] = contents[i] == null ? null : contents[i].clone();
        p.getInventory().setContents(copy);
        p.getInventory().setHeldItemSlot(heldSlot);
    }

    /** Removes, without a death animation, every entity the instrument tagged near the player. Returns how many. */
    int removeTagged(Player p, Scheduler scheduler) {
        int n = 0;
        for (Entity e : p.getNearbyEntities(48, 48, 48)) {
            if (!SelfTestStaging.removedAtRowStart(e instanceof Player, e.getScoreboardTags().contains(MOB_TAG))) continue;
            scheduler.onEntity(e, e::remove);
            n++;
        }
        return n;
    }

    /**
     * Put it all back. Runs on the player's own thread (a continuation, the stop command, or the quit event, whose
     * writes persist: the save runs after it). Returns the line that says what was restored.
     */
    String restore(Player p, ProfileService profiles, ResourcePool resources, MobNameplateManager nameplates,
                   Scheduler scheduler) {
        p.closeInventory();
        int mobs = 0;
        int orbs = 0;
        for (Entity e : p.getNearbyEntities(48, 48, 48)) {
            boolean orb = e instanceof ExperienceOrb;
            if (!removedOnRestore(e instanceof Player, e.getScoreboardTags().contains(MOB_TAG), orb)) continue;
            scheduler.onEntity(e, e::remove);
            if (orb) orbs++; else mobs++;
        }
        for (BlockState state : blocks.drainNewestFirst()) state.update(true, false);   // the last chain's

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
                + " blocks=" + blocks.total() + " mobsRemoved=" + mobs + " orbsRemoved=" + orbs;
    }
}
