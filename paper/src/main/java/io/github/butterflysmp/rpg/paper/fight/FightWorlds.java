package io.github.butterflysmp.rpg.paper.fight;

import io.github.butterflysmp.rpg.core.fight.ArenaShape;
import io.github.butterflysmp.rpg.core.fight.FightFolders;
import io.github.butterflysmp.rpg.paper.content.FightContent;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import io.papermc.paper.math.Position;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * THE FIGHT'S TEMPORARY WORLDS (the F8 brief, section 4 a, e, f; PLAN-dragon-mobs.md C1): one per fight, created on the
 * main thread, deleted at the fight's end, and swept at startup if a crash left one.
 *
 * <p><b>WHERE A FIGHT WORLD LIVES, READ from the pinned server jar (build 74):</b>
 * <ul>
 *   <li>{@code CraftServer.createWorld} keys the world as a DIMENSION of the main save
 *       ({@code PaperWorldLoader.dimensionKey}).</li>
 *   <li>Its folder is {@code DimensionType.getStorageFolder}: {@code <level>/dimensions/<namespace>/<name>}, here
 *       {@code run/world/dimensions/rpgfight/fight_<8 hex>}.</li>
 *   <li>{@code CraftWorld.getWorldFolder} returns that same folder.</li>
 * </ul>
 *
 * <p><b>WHY DELETING ONE IS SAFE</b> (PLAN-dragon-fight.md section 6.6):
 * <ul>
 *   <li>Its world data is written into its own folder (READ), and the main {@code level.dat} records nothing of it
 *       (INFERRED).</li>
 *   <li>A boot loads only the stem registry's worlds (READ), and a fight world is never among them (INFERRED).</li>
 *   <li>{@code unloadWorld} closes every region file synchronously before it returns (READ).</li>
 * </ul>
 *
 * <p><b>The namespace, the prefix and the marker are constants here, never content</b>: a typo in a YAML file must not
 * be able to point a delete at another namespace. Every delete passes {@link FightFolders#deleteTree}'s four tests and
 * logs its absolute path FIRST.
 */
public final class FightWorlds {

    /** The fight worlds' own namespace: nothing else in the server uses it. */
    public static final String NAMESPACE = "rpgfight";

    /** Every fight world's folder name starts with this. */
    public static final String PREFIX = "fight_";

    /** The file the fight writes in a world's folder at creation; the sweep deletes nothing without it. */
    public static final String MARKER = ".rpg-fight-world";

    /** A blocked unload or delete is tried again this many ticks later... */
    static final int RETRY_TICKS = 20;

    /** ...at most this many times, then left for the next startup's sweep, and said so. */
    static final int MAX_ATTEMPTS = 30;

    private final Scheduler scheduler;
    private final Logger log;

    public FightWorlds(Scheduler scheduler, Logger log) {
        this.scheduler = scheduler;
        this.log = log;
    }

    /** {@code <level>/dimensions/<NAMESPACE>}: the main world's folder is {@code <level>/dimensions/minecraft/overworld}. */
    public Path namespaceDir() {
        Path overworld = Bukkit.getWorlds().getFirst().getWorldFolder().toPath().toAbsolutePath().normalize();
        Path dimensions = overworld.getParent().getParent();
        if (!dimensions.getFileName().toString().equals("dimensions")) {
            throw new IllegalStateException("the main world's folder " + overworld + " is not under dimensions/");
        }
        return dimensions.resolve(NAMESPACE);
    }

    /** An empty world: nothing generates, nothing spawns. The arena's blocks are placed after. */
    static final class EmptyGenerator extends ChunkGenerator {
        @Override public boolean shouldGenerateNoise() { return false; }
        @Override public boolean shouldGenerateSurface() { return false; }
        @Override public boolean shouldGenerateBedrock() { return false; }
        @Override public boolean shouldGenerateCaves() { return false; }
        @Override public boolean shouldGenerateDecorations() { return false; }
        @Override public boolean shouldGenerateMobs() { return false; }
        @Override public boolean shouldGenerateStructures() { return false; }
    }

    /**
     * Creates one fight world (the brief's section 4 a), on the main thread, and writes its marker. The settings:
     * <ul>
     *   <li>NORMAL environment: an END world would build vanilla's own dragon fight (PLAN section 4 c).</li>
     *   <li>An empty generator, and a FORCED spawn position at C. READ: {@code MinecraftServer.initWorld} sets a forced
     *       spawn and skips {@code setInitialSpawn}'s chunk search.</li>
     *   <li>No mob spawning of any kind, mob griefing off.</li>
     *   <li>Time fixed at the content's hour, weather clear and fixed (SEAT PLACEHOLDER: the sky).</li>
     * </ul>
     * Returns null, logged, if the server refused.
     */
    public World create(FightContent.Arena arena) {
        return create(arena, arena.environment());
    }

    /**
     * As above, in this environment (F9c, section 3). For END, BEFORE the world is made, its folder gets our marker (the
     * same file, the same text: the sweep's four tests are unchanged) and a Paper per-world file switching
     * {@code entities.spawning.scan-for-legacy-ender-dragon} off. READ, the pinned server jar:
     * <ul>
     *   <li>the folder is {@code <level>/dimensions/<namespace>/<name>} ({@code DimensionType.getStorageFolder}), so it
     *       is known before {@code createWorld} ({@link #namespaceDir});</li>
     *   <li>Paper reads {@code <that folder>/paper-world.yml} ({@code PaperConfigurations}, {@code getDimensionPath}),
     *       and the world's config is made in the {@code Level} constructor, before {@code ServerLevel} builds vanilla's
     *       fight (offset 155 against 921);</li>
     *   <li>with the switch off, {@code EnderDragonFight.init} on a fresh record sets {@code needsStateScanning} false and
     *       {@code dragonKilled} true: no scan, no exit portal, no dragon made or adopted.</li>
     * </ul>
     * That Paper reads OUR file for OUR world is UNVERIFIED until {@link #endSelfCheck} reads it. The folder existing
     * before {@code createWorld} is the one change to what {@code createWorld} meets; INFERRED harmless (a dimension's
     * folder holds nothing it must find), and the created line and the arena's build are the witness.
     */
    public World create(FightContent.Arena arena, FightContent.Environment environment) {
        String name = PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        NamespacedKey key = new NamespacedKey(NAMESPACE, name);
        ArenaShape s = arena.shape();
        boolean end = environment == FightContent.Environment.END;
        if (end) {
            Path pre = namespaceDir().resolve(name).toAbsolutePath().normalize();
            try {
                Files.createDirectories(pre);
                Files.writeString(pre.resolve(MARKER), "rpg fight world " + key + "\n", StandardCharsets.UTF_8);
                Files.writeString(pre.resolve(WORLD_CONFIG), INERT_CONFIG, StandardCharsets.UTF_8);
                log.info("FIGHTWORLD end-config path=" + pre + " file=" + WORLD_CONFIG
                        + " scan-for-legacy-ender-dragon=false");
            } catch (IOException e) {
                log.warning("FIGHTWORLD end-config NOT written path=" + pre + ": " + e + "; making a NORMAL world instead"
                        + " (the folder, marked, is left for the sweep)");
                return create(arena, FightContent.Environment.NORMAL);
            }
        }
        WorldCreator creator = WorldCreator.ofKey(key)
                .environment(end ? World.Environment.THE_END : World.Environment.NORMAL)
                .generator(new EmptyGenerator())
                .generateStructures(false)
                .forcedSpawnPosition(Position.block(s.centreX(), (int) s.groundY(), s.centreZ()), 0.0f, 0.0f);
        long t0 = System.nanoTime();
        World world = Bukkit.createWorld(creator);
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        if (world == null) {
            log.warning("FIGHTWORLD create refused name=" + name + " ms=" + ms);
            return null;
        }
        world.setGameRule(org.bukkit.GameRules.SPAWN_MOBS, false);
        world.setGameRule(org.bukkit.GameRules.SPAWN_MONSTERS, false);
        world.setGameRule(org.bukkit.GameRules.SPAWN_PHANTOMS, false);
        world.setGameRule(org.bukkit.GameRules.SPAWN_PATROLS, false);
        world.setGameRule(org.bukkit.GameRules.SPAWN_WANDERING_TRADERS, false);
        world.setGameRule(org.bukkit.GameRules.MOB_GRIEFING, false);
        world.setGameRule(org.bukkit.GameRules.ADVANCE_TIME, false);
        world.setGameRule(org.bukkit.GameRules.ADVANCE_WEATHER, false);
        // CC's choice for Ben (not in the brief), flagged in GATE-fight-arena.md. KEEP_INVENTORY: what a death here
        // drops would be deleted with the world, so nothing is dropped. (Step F9d: an entrant's death is cancelled and
        // he goes DOWN instead, so there is no death screen to skip; IMMEDIATE_RESPAWN, which skipped it, is no longer
        // set. The rule stays at vanilla's default, false, in a fight world.)
        world.setGameRule(org.bukkit.GameRules.KEEP_INVENTORY, true);
        // An END world's time is fixed by its type (READ: the_end's has_fixed_time), so the hour is a NORMAL world's only.
        if (!end) world.setTime(arena.timeOfDayTicks());
        world.setStorm(false);
        world.setThundering(false);
        world.setAutoSave(false);
        Path folder = world.getWorldFolder().toPath().toAbsolutePath().normalize();
        try {
            Files.createDirectories(folder);
            Files.writeString(folder.resolve(MARKER), "rpg fight world " + key + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warning("FIGHTWORLD marker NOT written path=" + folder + ": " + e
                    + " (the sweep will refuse this folder; delete it by hand)");
        }
        log.info(String.format(Locale.ROOT, "FIGHTWORLD created name=%s key=%s folder=%s ms=%d environment=%s",
                world.getName(), key, folder, ms, world.getEnvironment()));
        return world;
    }

    /** Paper's per-world file name (READ: {@code PaperConfigurations.WORLD_CONFIG_FILE_NAME}). */
    static final String WORLD_CONFIG = "paper-world.yml";

    /** The per-world file an END fight world gets; the version is the one this build's own per-world files carry. */
    static final String INERT_CONFIG = """
            # Written by the RPG plugin (F9c) for a temporary fight world: vanilla's own dragon fight is made inert.
            _version: 31
            entities:
              spawning:
                scan-for-legacy-ender-dragon: false
            """;

    /**
     * THE SELF-CHECK of an END fight world (F9c, section 3 c), read a couple of ticks after it is made (READ: an empty
     * new world ticks its fight while {@code emptyTime} is under 300, and the fight's tick sets its bar's visibility
     * first). The ONE place that may read {@code World.getEnderDragonBattle()} (the seat's ruling of 2026-11-19, an
     * exception to C1): on the fight's own world handle, to confirm vanilla's fight is inert, never to find, drive or
     * identify our fight. A read that throws is a check that cannot be trusted, and reads as NOT inert.
     */
    public io.github.butterflysmp.rpg.core.fight.EndFightCheck.Reading endSelfCheck(World world, ArenaShape shape) {
        boolean present;
        boolean barVisible;
        boolean ownDragon;
        boolean portal;
        try {
            org.bukkit.boss.DragonBattle battle = world.getEnderDragonBattle();
            present = battle != null;
            barVisible = present && battle.getBossBar().isVisible();
            ownDragon = present && battle.getEnderDragon() != null;
            portal = present && battle.getEndPortalLocation() != null;
        } catch (RuntimeException e) {
            log.warning("FIGHTWORLD end-check could not read vanilla's fight in " + world.getName() + ": " + e);
            present = true;
            barVisible = true;
            ownDragon = true;
            portal = true;
        }
        int bedrock = 0;
        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
            if (world.getBlockAt(shape.centreX(), y, shape.centreZ()).getType() == Material.BEDROCK) bedrock++;
        }
        int dragons = world.getEntitiesByClass(org.bukkit.entity.EnderDragon.class).size();
        return new io.github.butterflysmp.rpg.core.fight.EndFightCheck.Reading(present, barVisible, ownDragon, portal,
                bedrock, dragons);
    }

    /** Places the generated arena's blocks (the brief's section 4 b), on the main thread. Returns the count placed. */
    public long build(World world, ArenaShape shape) {
        long t0 = System.nanoTime();
        long[] counts = new long[2];
        shape.forEachBlock((x, y, z, kind) -> {
            Material m = kind == ArenaShape.Kind.FLOOR ? Material.END_STONE : Material.OBSIDIAN;
            world.getBlockAt(x, y, z).setType(m, false);
            counts[kind == ArenaShape.Kind.FLOOR ? 0 : 1]++;
        });
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        long total = counts[0] + counts[1];
        log.info(String.format(Locale.ROOT, "FIGHTWORLD arena built name=%s blocks=%d end_stone=%d obsidian=%d ms=%d",
                world.getName(), total, counts[0], counts[1], ms));
        return total;
    }

    /**
     * The fight is over: unload the world without saving and delete its folder. It never blocks the server.
     * <ul>
     *   <li>A world that still holds a player (one on his death screen) is tried again {@link #RETRY_TICKS} later.</li>
     *   <li>A delete that fails because a file is still held is tried again the same way.</li>
     *   <li>Each retry is logged, and after {@link #MAX_ATTEMPTS} the folder is left for the next startup's sweep, and
     *       said so.</li>
     * </ul>
     */
    public void unloadAndDelete(World world, Path folder) {
        unloadAttempt(world, folder, 1);
    }

    /**
     * Plugin disable: unload and delete ONCE, now, with no retry (a scheduled retry would never run). Whatever fails is
     * left for the next startup's sweep, and said so.
     */
    public void unloadAndDeleteNow(World world, Path folder) {
        String name = world.getName();
        if (!Bukkit.unloadWorld(world, false)) {
            log.warning("FIGHTWORLD unload at disable REFUSED name=" + name + " players=" + world.getPlayers().size()
                    + "; the next startup's sweep will find the folder");
            return;
        }
        log.info("FIGHTWORLD unloaded name=" + name + " at=disable");
        Path ns = namespaceDir();
        log.info("FIGHTWORLD deleting path=" + folder.toAbsolutePath().normalize());
        try {
            long removed = FightFolders.deleteTree(ns, folder, PREFIX, MARKER);
            log.info("FIGHTWORLD deleted path=" + folder.toAbsolutePath().normalize() + " entries=" + removed);
        } catch (IOException | IllegalStateException e) {
            log.warning("FIGHTWORLD delete at disable FAILED path=" + folder.toAbsolutePath().normalize() + ": " + e
                    + "; the next startup's sweep will try again");
        }
    }

    private void unloadAttempt(World world, Path folder, int attempt) {
        String name = world.getName();
        if (!world.getPlayers().isEmpty()) {
            if (attempt >= MAX_ATTEMPTS) {
                log.warning("FIGHTWORLD unload GAVE UP name=" + name + " players=" + world.getPlayers().size()
                        + " after " + attempt + " attempts; the next startup's sweep will find the folder");
                return;
            }
            log.info("FIGHTWORLD unload-retry name=" + name + " players=" + world.getPlayers().size() + " attempt="
                    + attempt);
            later(() -> unloadAttempt(world, folder, attempt + 1));
            return;
        }
        boolean unloaded = Bukkit.unloadWorld(world, false);
        if (!unloaded) {
            log.info("FIGHTWORLD unload-retry name=" + name + " refused-by-server attempt=" + attempt);
            if (attempt < MAX_ATTEMPTS) later(() -> unloadAttempt(world, folder, attempt + 1));
            return;
        }
        log.info("FIGHTWORLD unloaded name=" + name);
        deleteAttempt(folder, 1);
    }

    private void deleteAttempt(Path folder, int attempt) {
        Path ns = namespaceDir();
        log.info("FIGHTWORLD deleting path=" + folder.toAbsolutePath().normalize());
        try {
            long removed = FightFolders.deleteTree(ns, folder, PREFIX, MARKER);
            log.info("FIGHTWORLD deleted path=" + folder.toAbsolutePath().normalize() + " entries=" + removed);
        } catch (IllegalStateException refused) {
            log.warning("FIGHTWORLD delete REFUSED path=" + folder.toAbsolutePath().normalize() + ": "
                    + refused.getMessage());
        } catch (IOException held) {
            if (attempt >= MAX_ATTEMPTS) {
                log.warning("FIGHTWORLD delete GAVE UP path=" + folder.toAbsolutePath().normalize() + " after " + attempt
                        + " attempts: " + held + "; the next startup's sweep will try again");
                return;
            }
            log.info("FIGHTWORLD delete-retry path=" + folder.toAbsolutePath().normalize() + " attempt=" + attempt
                    + " because=" + held);
            later(() -> deleteAttempt(folder, attempt + 1));
        }
    }

    /** Runs {@code task} {@link #RETRY_TICKS} later, through the project's {@code Scheduler} (global work). */
    private void later(Runnable task) {
        scheduler.onGlobalLater(task, RETRY_TICKS);
    }

    /**
     * THE ORPHAN SWEEP (the brief's section 4 f), at startup: every folder in the namespace folder that no loaded world
     * uses is judged by {@link FightFolders}. Only a DELETE verdict deletes, path logged first; every refusal is logged
     * with its reason and the folder is left alone.
     */
    public void sweepOrphans() {
        Path ns = namespaceDir();
        if (!Files.isDirectory(ns)) {
            log.info("FIGHTWORLD sweep path=" + ns + " folders=0 (no namespace folder)");
            return;
        }
        Set<Path> inUse = new java.util.HashSet<>();
        for (World w : Bukkit.getWorlds()) inUse.add(w.getWorldFolder().toPath().toAbsolutePath().normalize());
        List<Path> children = new ArrayList<>();
        try (Stream<Path> s = Files.list(ns)) {
            s.filter(Files::isDirectory).sorted().forEach(children::add);
        } catch (IOException e) {
            log.warning("FIGHTWORLD sweep could not list " + ns + ": " + e);
            return;
        }
        log.info("FIGHTWORLD sweep path=" + ns + " folders=" + children.size());
        for (Path child : children) {
            Path abs = child.toAbsolutePath().normalize();
            if (inUse.contains(abs)) continue;
            try {
                FightFolders.Verdict v = FightFolders.judge(ns, abs, PREFIX, MARKER);
                if (v != FightFolders.Verdict.DELETE) {
                    log.info("FIGHTWORLD orphan refused path=" + abs + " reason=" + v);
                    continue;
                }
                log.info("FIGHTWORLD deleting path=" + abs);
                long removed = FightFolders.deleteTree(ns, abs, PREFIX, MARKER);
                log.info("FIGHTWORLD orphan swept path=" + abs + " entries=" + removed);
            } catch (IOException | RuntimeException e) {
                log.warning("FIGHTWORLD orphan NOT swept path=" + abs + ": " + e);
            }
        }
    }

    /**
     * {@code /rpg fight worlds}: the instrument's reading:
     * <ul>
     *   <li>how many fight worlds are loaded;</li>
     *   <li>how many folders sit in the namespace folder;</li>
     *   <li>the caller's world.</li>
     * </ul>
     * A row's end reads {@code loaded=0 folders=0}.
     */
    public String worldsLine(Player caller) {
        long loaded = Bukkit.getWorlds().stream().filter(w -> NAMESPACE.equals(w.getKey().getNamespace())).count();
        long folders = 0;
        long marked = 0;
        Path ns = namespaceDir();
        if (Files.isDirectory(ns)) {
            try (Stream<Path> s = Files.list(ns)) {
                for (Path d : s.filter(Files::isDirectory).toList()) {
                    folders++;
                    if (Files.isRegularFile(d.resolve(MARKER))) marked++;
                }
            } catch (IOException e) {
                folders = -1;
            }
        }
        // folders counts every folder in the namespace, marked only the fight's own: R0e's look-alike (no marker) is
        // left there for the whole run, so a row's end reads folders=1 marked=0 while it stands.
        String line = "FIGHTWORLDS loaded=" + loaded + " folders=" + folders + " marked=" + marked + " you="
                + caller.getWorld().getName();
        log.info(line);
        return line;
    }

    /** Whether this world is one of the fight's (its key's namespace is the fight's). */
    public static boolean isFightWorld(World world) {
        return world != null && NAMESPACE.equals(world.getKey().getNamespace());
    }

    /** The folder a fight world lives in, captured before it is unloaded. */
    public static Path folderOf(World world) {
        File f = world.getWorldFolder();
        return f.toPath().toAbsolutePath().normalize();
    }
}
