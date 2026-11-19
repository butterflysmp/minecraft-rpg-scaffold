package io.github.butterflysmp.rpg.paper.fight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * STEP F8b's WIRING (GATE-fight-arena.md): the fight's own world, its people and its folder. None of it runs without a
 * server, so it is pinned from the source, comments stripped, each method bounded structurally from its signature to
 * the next member (the shape of {@code FightWiringSourceTest}). The judge's four refusals are core's
 * ({@code FightFoldersTest}); this pins that every delete goes THROUGH that judge and logs its path first.
 */
class FightArenaSourceTest {

    private static final Path FIGHT = Path.of("src/main/java/io/github/butterflysmp/rpg/paper/fight");
    private static final Path LISTENERS = Path.of("src/main/java/io/github/butterflysmp/rpg/paper/listener/RpgListeners.java");

    private static String code(Path file) throws IOException {
        String src = Files.readString(file);
        src = src.replaceAll("(?s)/\\*.*?\\*/", "");
        return src.replaceAll("//[^\\n]*", "");
    }

    private static String body(String code, String signature) {
        int from = code.indexOf(signature);
        assertTrue(from >= 0, "found: " + signature);
        int to = code.length();
        for (String next : new String[] {"@Override", "@EventHandler", "\n    public ", "\n    private ", "\n    void ",
                "\n    static "}) {
            int i = code.indexOf(next, from + signature.length());
            if (i >= 0 && i < to) to = i;
        }
        return code.substring(from, to);
    }

    @Test
    void everyDeleteGoesThroughTheJudgeAndLogsItsPathFirst() throws IOException {
        String worlds = code(FIGHT.resolve("FightWorlds.java"));
        for (String sig : new String[] {"private void deleteAttempt(Path folder, int attempt)",
                "public void unloadAndDeleteNow(World world, Path folder)", "public void sweepOrphans()"}) {
            String b = body(worlds, sig);
            int log = b.indexOf("\"FIGHTWORLD deleting path=\"");
            int del = b.indexOf("FightFolders.deleteTree(ns, ");
            assertTrue(log >= 0 && del > log, sig + ": the path is logged BEFORE the delete: " + b);
            assertTrue(b.contains("PREFIX, MARKER)"), sig + ": with the fight's own prefix and marker");
        }
        assertEquals(3, count(worlds, "FightFolders.deleteTree("), "three delete sites, no more");
        assertTrue(worlds.contains("public static final String NAMESPACE = \"rpgfight\";"), "its own namespace");
    }

    @Test
    void nothingInTheFightPackageDeletesAFileAnyOtherWay() throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> s = Files.list(FIGHT)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java")).toList()) {
                String c = code(p);
                for (String banned : new String[] {"Files.delete(", "Files.deleteIfExists(", ".delete()",
                        "walkFileTree(", "Bukkit.getScheduler()"}) {
                    if (c.contains(banned)) found.add(p.getFileName() + ": " + banned);
                }
            }
        }
        assertEquals(List.of(), found, "a delete or a scheduler outside FightFolders and Scheduler");
    }

    @Test
    void theWorldIsNormalEmptyForcedSpawnNoSpawningNoGriefingFixedSkyAndMarked() throws IOException {
        String create = body(code(FIGHT.resolve("FightWorlds.java")), "public World create(FightContent.Arena arena)");
        for (String needle : new String[] {"World.Environment.NORMAL", "new EmptyGenerator()", ".forcedSpawnPosition(",
                "GameRules.SPAWN_MOBS, false", "GameRules.MOB_GRIEFING, false", "GameRules.ADVANCE_TIME, false",
                "GameRules.ADVANCE_WEATHER, false", "world.setTime(arena.timeOfDayTicks())", "world.setStorm(false)",
                "GameRules.KEEP_INVENTORY, true", "GameRules.IMMEDIATE_RESPAWN, true", "folder.resolve(MARKER)"}) {
            assertTrue(create.contains(needle), "create sets " + needle);
        }
    }

    @Test
    void theReturnIsSavedBeforeHeGoesInAndClearedWhenHeComesBack() throws IOException {
        String fixtures = code(FIGHT.resolve("FightFixtures.java"));
        // F9: enter(Player) now delegates to enter(Player, boolean withDragon), which does the work.
        String enter = body(fixtures, "public String enter(Player p, boolean withDragon)");
        int save = enter.indexOf("profiles.setFightReturn(");
        int go = enter.indexOf("p.teleportAsync(entry)");
        assertTrue(save > 0 && go > save, "saved through the profile BEFORE the teleport: " + enter);
        String back = body(fixtures, "private void returnPlayer(Player p, String why)");
        assertTrue(back.contains("profiles.clearFightReturn(p.getUniqueId())"), back);
        String respawn = body(fixtures, "public void onPlayerRespawn(PlayerRespawnEvent event)");
        assertTrue(respawn.contains("event.setRespawnLocation(") && respawn.contains("profiles.clearFightReturn("),
                respawn);
        String quit = body(fixtures, "public void onQuit(Player p)");
        assertFalse(quit.contains("clearFightReturn"), "a quit KEEPS the return for the next login: " + quit);
        assertTrue(quit.contains("removeEntrant("), "and puts him out: " + quit);
    }

    @Test
    void aStaleLoginWithAReturnPendingIsSentBack() throws IOException {
        String settled = body(code(FIGHT.resolve("FightFixtures.java")), "public void onJoinSettled(Player p)");
        assertTrue(settled.contains("profiles.fightReturn(p.getUniqueId())"), settled);
        assertTrue(settled.contains("\"FIGHTWORLD stale login caught") && settled.contains("returnPlayer(p, \"login\")"),
                settled);
        String join = body(code(LISTENERS), "public void onJoin(PlayerJoinEvent event)");
        assertTrue(join.contains("profiles.whenSettled(") && join.contains("fights.onJoinSettled("), join);
    }

    @Test
    void poweredKillsOnlyTheParticipantsOnceAndNobodyInsideEndsIt() throws IOException {
        String instance = code(FIGHT.resolve("FightInstance.java"));
        String kill = body(instance, "private void killParticipants(RingFixture ring)");
        assertTrue(kill.contains("for (RingFixture.Participant p : port.participants())"), "only participants: " + kill);
        assertTrue(instance.contains("if (!poweredKilled && ring.outcome() == MageCycle.Outcome.FAILED_POWERED) {"),
                "once, on POWERED");
        assertTrue(instance.contains("if (site.nobodyInside()) {"), "nobody inside ends the fight");
    }

    @Test
    void everyArenaEndingUnloadsAndDeletes() throws IOException {
        String fixtures = code(FIGHT.resolve("FightFixtures.java"));
        String forget = body(fixtures, "private void forget(FightInstance ended)");
        assertTrue(forget.contains("endArena(a, false)"), forget);
        String end = body(fixtures, "private void endArena(ArenaFight a, boolean now)");
        assertTrue(end.contains("worlds.unloadAndDeleteNow(a.world(), a.folder())")
                && end.contains("worlds.unloadAndDelete(a.world(), a.folder())"), end);
        String disable = body(fixtures, "public void onDisable()");
        assertTrue(disable.contains("endArena(a, true)"), disable);
    }

    @Test
    void theListenersHandOverDeathRespawnAndQuit() throws IOException {
        String l = code(LISTENERS);
        assertTrue(body(l, "public void onPlayerDeath(PlayerDeathEvent event)").contains("fights.onPlayerDeath(event);"));
        assertTrue(body(l, "public void onPlayerRespawn(PlayerRespawnEvent event)").contains("fights.onPlayerRespawn(event);"));
        assertTrue(body(l, "public void onQuit(PlayerQuitEvent event)").contains("fights.onQuit(event.getPlayer());"));
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) n++;
        return n;
    }
}
