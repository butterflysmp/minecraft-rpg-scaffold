package io.github.butterflysmp.rpg.paper.nexus;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE QUIVER MIRROR'S WIRING, READ FROM SOURCE (the seat's ruling 1 and rulings B, E, 2026-11-22). The decision is core's
 * ({@code QuiverMirror}, {@code MirrorShot}); what no unit test can drive is WHEN it is asked and what is done with the
 * answer, because every hook is a Bukkit event. So each hook is pinned in the METHOD that must hold it, bounded by that
 * method's own closing brace (never a line count, which drifts with the javadoc around it).
 *
 * <p>The server halves are GATE-vanilla-bows' AUTO mirror rows; the off hand and an autosave are UNREAD BY DESIGN.
 */
class NexusMirrorWiringTest {

    private static final String P = "src/main/java/io/github/butterflysmp/rpg/paper/";

    private static String source(String file) throws IOException {
        return Files.readString(Path.of(P + file), StandardCharsets.UTF_8).replace("\r", "");
    }

    /** The body of the method whose declaration contains {@code signature}: up to its own 4-space closing brace. */
    private static String body(String file, String signature) throws IOException {
        String src = source(file);
        int at = src.indexOf(signature);
        assertTrue(at > 0, file + ": '" + signature + "' is not found -- the scan is blind");
        int end = src.indexOf("\n    }\n", at);
        assertTrue(end > at, file + ": no closing brace after '" + signature + "'");
        return src.substring(at, end);
    }

    private static void holds(String file, String signature, String needle) throws IOException {
        String body = body(file, signature);
        assertTrue(body.contains(needle), file + " " + signature + " must hold: " + needle);
    }

    /** CONTROL: the bounding helper stops at the method's own end, so a needle from the NEXT method is not found. */
    @Test
    void theBodyHelperIsBoundedByTheMethod() throws IOException {
        String spend = body("weapon/Quivers.java", "public static void spendRounds(");
        assertTrue(!spend.contains("public static boolean beginReload("), "the helper ran past spendRounds' end");
    }

    /** Ruling 1: re-drawn after a shot, at reload begin and at reload end. */
    @Test
    void theQuiverRedrawsAfterAShotAndAtBothEndsOfAReload() throws IOException {
        String redraw = "NexusMirror.redraw(player, keys);";
        holds("weapon/Quivers.java", "public static void spendRounds(", redraw);
        holds("weapon/Quivers.java", "public static boolean beginReload(", redraw);
        holds("weapon/Quivers.java", "private static void finishReload(", redraw);
    }

    /** Ruling 1: re-drawn at a held change, join and respawn; the star put back at quit (ruling E). */
    @Test
    void theListenersRedrawAtHeldChangeJoinAndRespawnAndRestoreAtQuit() throws IOException {
        String l = "listener/RpgListeners.java";
        holds(l, "public void onHeldItemChange(PlayerItemHeldEvent event) {",
                "NexusMirror.redraw(event.getPlayer(), adapters.keys(),");
        holds(l, "public void onJoin(PlayerJoinEvent event) {", "NexusMirror.redraw(joined, adapters.keys());");
        holds(l, "public void onPlayerRespawn(PlayerRespawnEvent event) {",
                "NexusMirror.redraw(event.getPlayer(), adapters.keys());");
        holds(l, "public void onQuit(PlayerQuitEvent event) {",
                "NexusMirror.restoreStar(event.getPlayer(), adapters.keys());");
    }

    /** Ruling E: the beat is the backstop; the star is put back at plugin disable. Ruling F: star on re-draws. */
    @Test
    void theBeatRedrawsAndWarnsDisableRestoresAndTheStarToggleRedraws() throws IOException {
        String enable = body("RpgPlugin.java", "public void onEnable() {");
        int bind = enable.indexOf("this.statsBar.bindBeat(player -> {");
        assertTrue(bind > 0, "the beat hook is bound in onEnable");
        String hook = enable.substring(bind, enable.indexOf("});", bind));
        assertTrue(hook.contains("NexusMirror.redraw(player, adapters.keys());"), hook);
        assertTrue(hook.contains("quiverWarning.observe(player);"), hook);
        holds("hud/StatsBarSystem.java", "private void start(Player player) {", "beatHook.accept(player);");
        holds("RpgPlugin.java", "public void onDisable() {", "NexusMirror.restoreStar(online, adapters.keys());");
        holds("menu/SettingsMenu.java", "private void setStar(boolean enabled) {",
                "NexusMirror.redraw(viewer, adapters.keys());");
    }

    /** Ruling 1: the selftest's restore is followed by a redraw, not preceded by one. */
    @Test
    void theSelftestRestoreIsFollowedByARedraw() throws IOException {
        String end = body("selftest/SelfTestRun.java", "private void end(String why,");
        int restore = end.indexOf("snapshot.restore(");
        int redraw = end.indexOf("NexusMirror.redraw(player, service.adapters().keys());");
        assertTrue(restore > 0 && redraw > restore, "restore at " + restore + ", redraw at " + redraw);
    }

    /** Ruling 3 / A: a reload never plans or debits the mirror. */
    @Test
    void aReloadNeverPlansOrDebitsTheMirror() throws IOException {
        holds("weapon/QuiverAmmo.java", "private static List<Cell> cells(",
                "CraftMatrixScreen.neverAnIngredient(item, keys)));");
        holds("weapon/QuiverAmmo.java", "public static void consume(",
                "if (io.github.butterflysmp.rpg.paper.nexus.NexusItems.isNexus(stack, keys)) continue;");
    }

    /** Rulings B and C: the shot guard asks core, and does each of its three answers. */
    @Test
    void theShotGuardAsksCoreAndPerformsEachAnswer() throws IOException {
        String src = source("listener/RpgListeners.java");
        int sig = src.indexOf("public void onMirrorShot(EntityShootBowEvent event) {");
        assertTrue(sig > 0, "onMirrorShot is not found");
        String annotation = src.substring(src.lastIndexOf("@EventHandler", sig), sig);
        assertTrue(annotation.contains("priority = EventPriority.HIGHEST"), annotation);
        String body = body("listener/RpgListeners.java", "public void onMirrorShot(EntityShootBowEvent event) {");
        assertTrue(body.contains("NexusItems.isNexus(event.getConsumable(), adapters.keys());"), body);
        assertTrue(body.contains("MirrorShot.of(nexus, paid)"), body);
        assertTrue(body.contains("case CANCEL -> event.setCancelled(true);"), body);
        assertTrue(body.contains("arrow.setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);"), body);
        assertTrue(body.contains("if (nexus) io.github.butterflysmp.rpg.paper.nexus.NexusMirror.redraw(player, adapters.keys());"),
                body);
    }
}
