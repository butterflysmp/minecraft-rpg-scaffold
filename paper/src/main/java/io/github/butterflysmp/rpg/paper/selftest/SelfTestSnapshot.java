package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.paper.health.MobNameplateManager;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import java.util.List;
import java.util.UUID;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
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

    /**
     * The tags a SPLIT CHILD takes from its parent (PLAN-dragon-mobs.md step R2): ALL of the parent's, when the
     * parent is one the instrument tagged; NONE otherwise. Pure, so it is a unit row.
     *
     * <p>Why: a row that kills a stage-5 Phage leaves two children, and the row-start clean and the restore remove
     * only what carries {@link #MOB_TAG}. Without this, the children would be untagged and leak into the next row and
     * out of the run. With it, they carry the row's own tags too, so the row can PROBE them by its tag. A mob the
     * instrument never tagged (any mob outside a run) passes nothing on, so production is untouched.
     */
    static java.util.Set<String> tagsForSplitChild(java.util.Set<String> parentTags) {
        return parentTags.contains(MOB_TAG) ? java.util.Set.copyOf(parentTags) : java.util.Set.of();
    }

    /**
     * The tags a PHALANX'S SHOT takes from its Phalanx (dragon-mobs step R7a), only when the instrument tagged the
     * Phalanx: {@code selftest} itself, so the row-start clean removes the shot, and every OTHER tag with
     * {@code _shot} added, so a row names the shot ({@code attack: sh2_shot}) and never confuses it with the Phalanx
     * ({@code sh2}). A Phalanx the instrument never tagged gives none: production, untouched. Pure, so it is a unit row.
     */
    static java.util.Set<String> tagsForShot(java.util.Set<String> ownerTags) {
        if (!ownerTags.contains(MOB_TAG)) return java.util.Set.of();
        java.util.Set<String> out = new java.util.TreeSet<>();
        out.add(MOB_TAG);
        for (String t : ownerTags) if (!t.equals(MOB_TAG)) out.add(t + "_shot");
        return java.util.Set.copyOf(out);
    }

    /**
     * Whether a SPLIT CHILD is born FROZEN (step R3; the seat's brief of 2026-11-16): only when its parent is one the
     * instrument tagged AND froze. Pure, so it is a unit row.
     *
     * <p>Why: from step R3 a live Phage within 3 blocks of the player swells and blows, and the rows that kill a frozen
     * stage 5 never asked for its children to be live. Why AT BIRTH, inside the child's pre-spawn consumer, and not by
     * a later step: a creeper's swell rises by its swell direction EVERY tick whatever its AI ({@code Creeper.tick},
     * read in the jar), and only its swell goal turns that direction up. NoAI stops the goals
     * ({@code Mob.isEffectiveAi}), so a child frozen before its first tick keeps the fresh direction, −1
     * ({@code Creeper.defineSynchedData}), and never swells; a child frozen after it began to swell keeps swelling and
     * still blows. A row that wants live children thaws them itself, in the blast gate (DS1R3). A mob the instrument
     * never tagged is untouched, so production is too.
     */
    static boolean freezeSplitChild(java.util.Set<String> parentTags, boolean parentHasAi) {
        return parentTags.contains(MOB_TAG) && !parentHasAi;
    }

    private final UUID id;
    private final long lifetimeXp;
    private final GameMode gameMode;
    private final ItemStack[] contents;
    private final int heldSlot;
    private final Location location;
    private final Boolean spawnMobs;
    /** In the restore set since SU8's death_rules (2026-11-10): a row sets both, and an aborted row must not leave them set. */
    private final Boolean keepInventory;
    private final Boolean immediateRespawn;
    /** The world's difficulty at RUN START, put back by the one restore (GATE-dragon-fix.md, C5). */
    private final org.bukkit.Difficulty difficulty;
    private final long time;
    private final boolean trace;
    private final double mana;
    /**
     * His CUSTOM health at run start, or NaN when the store did not track him (2026-11-22, the seat's brief, PR 1).
     * Before this, a run that staged his level at 1 left him at his level-1 health out of his own maximum
     * ({@code GATE-knell-beam.md}, THE SEAT'S FOLLOW-UP a): the snapshot held no health at all.
     */
    private final double health;
    /**
     * His VANILLA experience at run start: level and progress (2026-11-23, GATE-unique-enchant-slot). The enchanting
     * table and the anvil charge THIS wallet ({@code XpCurve} over {@code getLevel}/{@code getExp}), and it is not the
     * Lifetime XP above. Before this, a row that staged vanilla XP to buy a candidate left his bar changed after the run.
     */
    private final int vanillaLevel;
    private final float vanillaProgress;
    private final String classId;
    private final String elementId;
    /**
     * First-saved state per block, in save order; restored in reverse so an overlap ends at the oldest. Drained at
     * every unchained row start ({@link #restoreRowBlocks}), not only at the end of the run: run 2 of the
     * {@code 61e722f2} boot drowned its player because each Sunder row staged from the floor the last one left.
     */
    private final SelfTestStaging.RowBlocks<Location, BlockState> blocks = new SelfTestStaging.RowBlocks<>();

    private SelfTestSnapshot(Player p, ProfileService profiles, ResourcePool resources, MobNameplateManager nameplates,
                             CombatantStats stats) {
        this.id = p.getUniqueId();
        this.health = stats.tracks(id) ? stats.current(id) : Double.NaN;
        this.vanillaLevel = p.getLevel();
        this.vanillaProgress = p.getExp();
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
        this.keepInventory = world.getGameRuleValue(GameRules.KEEP_INVENTORY);
        this.immediateRespawn = world.getGameRuleValue(GameRules.IMMEDIATE_RESPAWN);
        this.difficulty = world.getDifficulty();
        this.time = world.getTime();
        this.trace = nameplates.tracing();
        this.mana = resources.current(id, ResourceCost.DEFAULT_RESOURCE);
    }

    static SelfTestSnapshot take(Player p, ProfileService profiles, ResourcePool resources, MobNameplateManager nameplates,
                                 CombatantStats stats) {
        return new SelfTestSnapshot(p, profiles, resources, nameplates, stats);
    }

    /**
     * How long after the restore his health is put back. <b>Two passes of {@code PlayerHealthSystem}'s reconcile loop
     * (5 ticks each)</b>: the XP restore raises his max only on the loop's next pass, and a rise is HEADROOM (current
     * unchanged), so a write before that pass would be clamped to his level-1 max. One pass lands within 5 ticks;
     * the second is the margin. The HEALTH line the write logs carries the max it saw, so a write that came too soon
     * reads as {@code customHP=<m1>/<m1>} rather than passing as a restore.
     */
    static final int HEALTH_RESTORE_DELAY_TICKS = 10;

    /**
     * The health to write back, or NaN for no write. Pure, so it is a unit row. It puts back {@code min(saved, max)}
     * and only ever RAISES him: if regeneration (or anything else) has already put him at or above that, it writes
     * nothing, so the restore never deals him damage. NaN when nothing was saved (he was untracked at run start).
     */
    static double healthToRestore(double saved, double current, double max) {
        if (Double.isNaN(saved)) return Double.NaN;
        double target = Math.min(saved, max);
        return target > current ? target : Double.NaN;
    }

    /**
     * Put his custom health back, {@link #HEALTH_RESTORE_DELAY_TICKS} after the restore, on his own thread, and log
     * {@code HEALTH saved=<s> customHP=<c>/<m> wrote=<w|none>} through {@code log}. At a server stop (the inline
     * scheduler) the delayed task is REFUSED and named by the caller's NOT-RESTORED line: his health is not saved with
     * him (a join registers him at full, {@code PlayerHealthSystem.onJoin}), so nothing is lost.
     */
    void restoreHealthLater(Player p, CombatantStats stats, Scheduler scheduler, java.util.function.Consumer<String> log) {
        scheduler.onEntityLater(p, () -> {
            if (!stats.tracks(id)) {
                log.accept("HEALTH saved=" + fmtHealth(health) + " untracked: nothing written");
                return;
            }
            double target = healthToRestore(health, stats.current(id), stats.max(id));
            if (!Double.isNaN(target)) stats.setCurrent(id, target, id, true);
            log.accept("HEALTH saved=" + fmtHealth(health) + " customHP=" + fmtHealth(stats.current(id)) + "/"
                    + fmtHealth(stats.max(id)) + " wrote=" + (Double.isNaN(target) ? "none" : fmtHealth(target)));
        }, HEALTH_RESTORE_DELAY_TICKS);
    }

    private static String fmtHealth(double v) {
        return Double.isNaN(v) ? "none" : String.format(java.util.Locale.ROOT, "%.3f", v);
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
     *
     * <p>{@code now} is the PLUGIN DISABLE's restore (a server stop mid-run): the scheduler is then an
     * {@code InlineScheduler}, so every task below runs at once, and the teleport is synchronous, because
     * {@code teleportAsync} would complete after the world save it must precede.
     */
    String restore(Player p, ProfileService profiles, ResourcePool resources, MobNameplateManager nameplates,
                   Scheduler scheduler, boolean now) {
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
        if (now) p.teleport(location); else p.teleportAsync(location);

        boolean xp = profileLoaded() && profiles.setLifetimeXp(id, lifetimeXp);
        // The vanilla wallet, written back as the table writes it (setLevel then setExp), never through giveExp.
        p.setLevel(vanillaLevel);
        p.setExp(vanillaProgress);
        boolean cell = classId == null || profiles.setCell(id, classId, elementId);
        resources.setCurrent(id, ResourceCost.DEFAULT_RESOURCE, mana);
        // No setter for the trace: toggle until it reads what it read before (at most once).
        if (nameplates.tracing() != trace) nameplates.toggleTrace();

        World world = location.getWorld();
        scheduler.onGlobal(() -> {
            if (spawnMobs != null) world.setGameRule(GameRules.SPAWN_MOBS, spawnMobs);
            if (keepInventory != null) world.setGameRule(GameRules.KEEP_INVENTORY, keepInventory);
            if (immediateRespawn != null) world.setGameRule(GameRules.IMMEDIATE_RESPAWN, immediateRespawn);
            world.setTime(time);
            if (difficulty != null) world.setDifficulty(difficulty);
        });
        return "RESTORED xp=" + (xp ? lifetimeXp : "NOT-RESTORED") + " cell=" + (cell ? classId + "/" + elementId : "NOT-RESTORED")
                + " gamemode=" + gameMode + " held=" + heldSlot + " mana=" + String.format("%.1f", mana)
                + " trace=" + nameplates.tracing() + " spawn_mobs=" + spawnMobs + " time=" + time
                + " blocks=" + blocks.total() + " mobsRemoved=" + mobs + " orbsRemoved=" + orbs
                + " keep_inventory=" + keepInventory + " immediate_respawn=" + immediateRespawn
                + " difficulty=" + difficulty + " health=" + fmtHealth(health)
                + " vanilla_xp=" + vanillaLevel + "+" + String.format(java.util.Locale.ROOT, "%.3f", vanillaProgress);
    }
}
