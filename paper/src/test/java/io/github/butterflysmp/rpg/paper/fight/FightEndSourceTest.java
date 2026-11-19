package io.github.butterflysmp.rpg.paper.fight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F9c, AN END-TYPE FIGHT WORLD (the seat's F9b brief, section 3; plan 6.9 option a). The wiring sits behind a real
 * world, so it is pinned from the source, comments stripped, each method bounded STRUCTURALLY. The pure half is core's
 * {@code EndFightCheckTest}.
 */
class FightEndSourceTest {

    private static final Path PAPER = Path.of("src/main/java/io/github/butterflysmp/rpg/paper");

    private static String strip(String src) {
        src = src.replace("\r\n", "\n").replaceAll("(?s)/\\*.*?\\*/", "");
        return src.replaceAll("//[^\\n]*", "");
    }

    private static String code(String file) throws IOException {
        return strip(Files.readString(PAPER.resolve(file)));
    }

    private static String body(String code, String signature) {
        int from = code.indexOf(signature);
        assertTrue(from >= 0, "found: " + signature);
        int to = code.length();
        for (String next : new String[] {"@Override", "@EventHandler", "\n    public ", "\n    private ", "\n    void ",
                "\n    static ", "\n    String "}) {
            int i = code.indexOf(next, from + signature.length());
            if (i >= 0 && i < to) to = i;
        }
        return code.substring(from, to);
    }

    /**
     * THE SEAT'S RULING OF 2026-11-19: {@code World.getEnderDragonBattle()} may be read on the fight's own world in the
     * self-check ONLY. Every main source file is scanned, comments stripped: exactly one call, inside endSelfCheck.
     */
    @Test
    void getEnderDragonBattleAppearsInTheSelfCheckAndNowhereElse() throws IOException {
        List<String> where = new ArrayList<>();
        try (Stream<Path> s = Files.walk(PAPER)) {
            for (Path f : s.filter(p -> p.toString().endsWith(".java")).toList()) {
                String c = strip(Files.readString(f));
                int i = -1;
                while ((i = c.indexOf("getEnderDragonBattle(", i + 1)) >= 0) where.add(PAPER.relativize(f).toString());
            }
        }
        assertEquals(List.of(Path.of("fight", "FightWorlds.java").toString()), where, "exactly one call, in FightWorlds");
        assertTrue(body(code("fight/FightWorlds.java"), "public io.github.butterflysmp.rpg.core.fight.EndFightCheck.Reading"
                + " endSelfCheck(").contains("world.getEnderDragonBattle()"), "and it is the self-check's");
    }

    /** The switch and the marker are written BEFORE the world is made; the END environment only then. */
    @Test
    void theEndWorldsConfigAndMarkerAreWrittenBeforeCreateWorld() throws IOException {
        String c = body(code("fight/FightWorlds.java"),
                "public World create(FightContent.Arena arena, FightContent.Environment environment)");
        int config = c.indexOf("Files.writeString(pre.resolve(WORLD_CONFIG), INERT_CONFIG");
        int marker = c.indexOf("Files.writeString(pre.resolve(MARKER), \"rpg fight world \" + key + \"\\n\"");
        int create = c.indexOf("Bukkit.createWorld(creator)");
        assertTrue(marker > 0 && config > marker && create > config, "marker, then the switch, then createWorld: " + c);
        assertTrue(c.contains(".environment(end ? World.Environment.THE_END : World.Environment.NORMAL)"), c);
        assertTrue(c.contains("if (!end) world.setTime(arena.timeOfDayTicks());"), "the hour is a NORMAL world's only");
    }

    /** The file switches exactly the one key off, at the version this build's per-world files carry. */
    @Test
    void theInertConfigSwitchesTheLegacyScanOff() {
        String cfg = FightWorlds.INERT_CONFIG;
        assertTrue(cfg.contains("_version: 31\n"), cfg);
        assertTrue(cfg.contains("entities:\n  spawning:\n    scan-for-legacy-ender-dragon: false\n"), cfg);
        assertEquals("paper-world.yml", FightWorlds.WORLD_CONFIG);
    }

    /** Not inert: the loud line, that world ended, a NORMAL world made instead. Inert: the line, then the arena. */
    @Test
    void aFailedCheckEndsTheWorldAndFallsBackToNormal() throws IOException {
        String f = body(code("fight/FightFixtures.java"), "private void finishEndStart(");
        int check = f.indexOf("worlds.endSelfCheck(w, f.arena().shape());");
        int inert = f.indexOf("if (io.github.butterflysmp.rpg.core.fight.EndFightCheck.inert(r)) {");
        int end = f.indexOf("worlds.unloadAndDeleteNow(w, FightWorlds.folderOf(w));");
        int normal = f.indexOf("use = worlds.create(f.arena(), FightContent.Environment.NORMAL);");
        int start = f.indexOf("startArena(p, f, use, withDragon);");
        assertTrue(check > 0 && inert > check && end > inert && normal > end && start > normal, f);
        String e = body(code("fight/FightFixtures.java"), "public String enter(Player p, boolean withDragon)");
        assertTrue(e.contains("if (w.getEnvironment() == World.Environment.THE_END) {")
                && e.contains("adapters.scheduler().onGlobalLater(() -> finishEndStart(who, f, w, withDragon),"
                + " END_CHECK_DELAY_TICKS);"), "an END world is checked before anything is built: " + e);
    }

    /** A crystal's fire in a fight world is cancelled; outside one it is not ours to touch. */
    @Test
    void aCrystalsFireIsCancelledInAFightWorldOnly() throws IOException {
        String l = body(code("listener/RpgListeners.java"), "public void onFightCrystalFire(");
        int crystal = l.indexOf("event.getIgnitingEntity() instanceof org.bukkit.entity.EnderCrystal crystal");
        int world = l.indexOf("FightWorlds.isFightWorld(event.getBlock().getWorld())) return;");
        int cancel = l.indexOf("event.setCancelled(true);");
        assertTrue(crystal > 0 && world > crystal && cancel > world, l);
    }

    /** F9b fix D (the seat's brief of 2026-11-19): the arena is probed at EVERY arena fight's end, before anyone moves. */
    @Test
    void theArenaIsProbedAtEveryFightsEnd() throws IOException {
        String e = body(code("fight/FightFixtures.java"), "private void endArena(ArenaFight a, boolean now)");
        int probe = e.indexOf("probeArena(a, now ? \"end-disable\" : \"end\");");
        int loop = e.indexOf("for (Player p : new ArrayList<>(a.world().getPlayers()))");
        assertTrue(probe > 0 && loop > probe, "probed first: " + e);
        assertTrue(body(code("fight/FightFixtures.java"), "public String arenaProbe()")
                .contains("return probeArena(a, \"command\");"), "the command probes the same way");
    }
}
