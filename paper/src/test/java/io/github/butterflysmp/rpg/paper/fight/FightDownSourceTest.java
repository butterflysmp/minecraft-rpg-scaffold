package io.github.butterflysmp.rpg.paper.fight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F9d, DOWN, ALL DOWN AND THE ENTRY MODE (the seat's go-ahead of 2026-11-19). The wiring sits behind a real server, so
 * it is pinned from the source, comments stripped, each method bounded STRUCTURALLY. The pure halves are core's
 * {@code DownRosterTest} and storage's {@code FightReturnProfileTest}.
 */
class FightDownSourceTest {

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
                "\n    static ", "\n    String ", "\n    boolean "}) {
            int i = code.indexOf(next, from + signature.length());
            if (i >= 0 && i < to) to = i;
        }
        return code.substring(from, to);
    }

    private static String fixtures() throws IOException { return code("fight/FightFixtures.java"); }

    /** a: the death is cancelled with his full health back, he is marked down, and spectating follows on his thread. */
    @Test
    void aDeathInsideIsCancelledAndHeGoesDown() throws IOException {
        String d = body(fixtures(), "public boolean onPlayerDeath(PlayerDeathEvent event)");
        int cancel = d.indexOf("event.setCancelled(true);");
        int revive = d.indexOf("event.setReviveHealth(max == null ? 20.0 : max.getValue());");
        int mark = d.indexOf("if (!a.site().markDown(p.getUniqueId())) return true;");
        int log = d.indexOf("\"FIGHT down player=%s t=%d cause=%s at=%.2f %.2f %.2f\"");
        int view = d.indexOf("adapters.scheduler().onEntity(p, () -> spectate(p, a));");
        assertTrue(cancel > 0 && revive > cancel && mark > revive && log > mark && view > log, d);
        assertTrue(d.contains("if (a == null || p.getWorld() != a.world()) return false;"), "only an arena entrant inside");
        String s = body(fixtures(), "private void spectate(Player p, ArenaFight a)");
        assertTrue(s.contains("p.setGameMode(org.bukkit.GameMode.SPECTATOR);"), s);
        assertTrue(s.contains("p.removePotionEffect(e.getType());") && s.contains("p.setFireTicks(0);"), "cleared: " + s);
        assertTrue(s.contains("if (p.getLocation().getY() < a.site().centre().getY() - 1.0) {"), "lifted out of the void");
    }

    /** a: no respawn follows, so the listener resets what a respawn resets (and the health to full). */
    @Test
    void theListenerResetsHisCombatStateWhenHeGoesDown() throws IOException {
        String l = code("listener/RpgListeners.java");
        assertTrue(body(l, "public void onPlayerDeath(PlayerDeathEvent event)")
                .contains("if (fights.onPlayerDeath(event)) onDowned(event.getPlayer());"));
        String down = body(l, "private void onDowned(Player p)");
        for (String n : new String[] {"adapters.stats().heal(id, adapters.stats().max(id), id, true);",
                "damageWindow.forget(id);", "adapters.scorch().forget(id);", "WitherPotion.forgetAndStrip(adapters.wither(), p);",
                "stoneCaster.onDeath(id);", "adapters.safeLandings().forget(id);"}) {
            assertTrue(down.contains(n), "onDowned resets " + n + ": " + down);
        }
    }

    /** b: a down player takes no damage of any kind, first; and he is no participant. */
    @Test
    void aDownPlayerTakesNoDamageAndIsNoParticipant() throws IOException {
        String l = code("listener/RpgListeners.java");
        int ann = l.indexOf("@EventHandler(priority = EventPriority.LOWEST)\n    public void onDownPlayerDamaged(EntityDamageEvent event)");
        assertTrue(ann > 0, "LOWEST, and not ignoreCancelled");
        assertTrue(body(l, "public void onDownPlayerDamaged(EntityDamageEvent event)")
                .contains("if (event.getEntity() instanceof Player p && fights.isDown(p)) event.setCancelled(true);"));
        String take = body(code("fight/ArenaSite.java"), "public boolean takesPart(Player player)");
        assertTrue(take.contains("|| roster.isDown(player.getUniqueId())) return false;"), take);
        String kill = body(code("fight/FightInstance.java"), "public void onDeath(EntityDeathEvent event)");
        assertTrue(kill.contains("|| !site.takesPart(killer)) return;"), "a down player's kill does not count: " + kill);
    }

    /** b: he still sees the ring, the bar and the chat. */
    @Test
    void aDownPlayerStillSeesTheRingTheBarAndTheChat() throws IOException {
        String port = code("fight/BukkitFightPort.java");
        assertTrue(body(port, "public void draw(List<UUID> viewers, List<double[]> points)").contains("all.addAll(downWatchers());"));
        assertTrue(body(port, "public void tell(String line)").contains("all.addAll(downWatchers());"));
        assertTrue(body(port, "public List<UUID> downWatchers()").contains("if (p != null && p.getWorld() == world) out.add(id);"));
        assertTrue(body(code("fight/FightInstance.java"), "private void trackBossBar()").contains("now.addAll(port.downWatchers());"));
    }

    /** c: ALL DOWN is asked each tick, after "nobody inside", and the return finishes the fight. */
    @Test
    void allDownIsCheckedEachTickAndFinishesTheFight() throws IOException {
        String f = code("fight/FightInstance.java");
        int nobody = f.indexOf("finish(\"nobody-inside-\" + ring.outcome());");
        int check = f.indexOf("site.allDownCheck(ring.fightTick(), port.participants().size());");
        int failed = f.indexOf("\"FIGHT all-down t=%d down=%s outcome=%s returns_in=%d\"");
        int due = f.indexOf("finish(\"all-down-FAILED\");");
        assertTrue(nobody > 0 && check > nobody && failed > check && due > failed, "the order");
        assertTrue(code("fight/FightFixtures.java").contains(
                "new ArenaSite(w, f.arena().shape(), f.arena().allDownSeconds() * 20);"), "the wait from content");
    }

    /** d: the mode he entered in is saved with his return and restored on every route home. */
    @Test
    void theEntryModeIsSavedAndRestoredOnEveryRouteHome() throws IOException {
        String fx = fixtures();
        assertTrue(fx.contains("from.getYaw(), from.getPitch(), p.getGameMode().name());"), "saved on entry");
        String ret = body(fx, "private void returnPlayer(Player p, String why)");
        int restore = ret.indexOf("restoreMode(p, back, why);");
        int tp = ret.indexOf("p.teleportAsync(to);");
        assertTrue(restore > 0 && tp > restore, "the leave, the end, the stale login and a spectator leaving: " + ret);
        String end = body(fx, "private void endArena(ArenaFight a, boolean now)");
        assertTrue(end.contains("restoreMode(p, back, \"end-disable\");"), "a server stop");
        assertTrue(end.contains("restoreMode(p, Optional.empty(), \"end-not-entrant\");"), "anyone else, never spectator");
        assertTrue(body(fx, "public void onPlayerRespawn(PlayerRespawnEvent event)").contains("restoreMode(p, back, \"died\");"));
        String rm = body(fx, "private void restoreMode(Player p, Optional<FightReturn> back, String why)");
        assertTrue(rm.contains("org.bukkit.GameMode.valueOf(back.get().restoreMode())"), rm);
        assertTrue(rm.contains("else if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) want = org.bukkit.GameMode.SURVIVAL;"), rm);
        assertTrue(body(fx, "public void onJoinSettled(Player p)").contains("returnPlayer(p, \"login\");"), "the stale login");
    }

    /** e: a down spectator leaving the fight world is out and returned. */
    @Test
    void aDownSpectatorLeavingTheWorldIsOutAndReturned() throws IOException {
        String w = body(fixtures(), "public void onWorldChange(Player p, World from)");
        int test = w.indexOf("!a.site().isDown(p.getUniqueId())");
        int out = w.indexOf("a.site().removeEntrant(p.getUniqueId());");
        int ret = w.indexOf("returnPlayer(p, \"spectator-left\");");
        assertTrue(test > 0 && out > test && ret > out, w);
        assertTrue(body(code("listener/RpgListeners.java"),
                "public void onFightWorldLeft(org.bukkit.event.player.PlayerChangedWorldEvent event)")
                .contains("fights.onWorldChange(event.getPlayer(), event.getFrom());"));
    }
}
