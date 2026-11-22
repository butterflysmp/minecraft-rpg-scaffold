package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE VANILLA-DRIVEN SHOT'S WIRING, READ FROM SOURCE (the seat's rulings 7 to 10, 2026-11-22). The decisions are core's and
 * unit-tested there; what no unit test can drive is the Bukkit events, so each hook is pinned in the METHOD that must hold
 * it, bounded by that method's own closing brace. The server halves are GATE-vanilla-bows' prompted rows.
 */
class VanillaShotWiringTest {

    private static final String P = "src/main/java/io/github/butterflysmp/rpg/paper/";

    private static String source(String file) throws IOException {
        return Files.readString(Path.of(P + file), StandardCharsets.UTF_8).replace("\r", "");
    }

    private static String body(String file, String signature) throws IOException {
        String src = source(file);
        int at = src.indexOf(signature);
        assertTrue(at > 0, file + ": '" + signature + "' is not found -- the scan is blind");
        int end = src.indexOf("\n    }\n", at);
        assertTrue(end > at, file + ": no closing brace after '" + signature + "'");
        return src.substring(at, end);
    }

    private static String annotationOf(String file, String signature) throws IOException {
        String src = source(file);
        int at = src.indexOf(signature);
        assertTrue(at > 0, signature + " is not found");
        return src.substring(src.lastIndexOf("@EventHandler", at), at);
    }

    /** CONTROL: the bound stops at the method's end. */
    @Test
    void theBodyHelperIsBoundedByTheMethod() throws IOException {
        String shot = body("listener/RpgListeners.java", "public void onVanillaShot(EntityShootBowEvent event) {");
        assertFalse(shot.contains("onVanillaShotHit"), "the helper ran past onVanillaShot's end");
    }

    /** The shot runs at HIGH, before onMirrorShot at HIGHEST, which reads the quiver-paid mark it writes. */
    @Test
    void theShotRunsBeforeTheMirrorGuard() throws IOException {
        String l = "listener/RpgListeners.java";
        assertTrue(annotationOf(l, "public void onVanillaShot(EntityShootBowEvent event) {")
                .contains("priority = EventPriority.HIGH, ignoreCancelled = true"));
        assertTrue(annotationOf(l, "public void onMirrorShot(EntityShootBowEvent event) {")
                .contains("priority = EventPriority.HIGHEST"));
        assertTrue(body(l, "public void onVanillaShot(EntityShootBowEvent event) {").contains("vanillaShots.onShot(event);"));
    }

    /** The hit: the token, the floor, then ours; and the REROUTE path returns early on exactly the same predicate. */
    @Test
    void theHitReplacesVanillasAndTheRerouteSkipsTheSameArrows() throws IOException {
        String l = "listener/RpgListeners.java";
        String hit = body(l, "public void onVanillaShotHit(EntityDamageByEntityEvent event) {");
        int token = hit.indexOf("event.setDamage(TOKEN_DAMAGE);");
        int floor = hit.indexOf("floorSoTokenCannotKill(hit.victim());");
        int land = hit.indexOf("vanillaShots.land(hit);");
        assertTrue(hit.contains("vanillaShots.stamped(event).ifPresent(hit -> {"), hit);
        assertTrue(token > 0 && floor > token && land > floor, "token " + token + ", floor " + floor + ", land " + land);

        String reroute = body(l, "public void onEnvironmentalDamage(EntityDamageEvent event) {");
        int skip = reroute.indexOf("if (vanillaShots.stamped(event).isPresent()) return;");
        int policy = reroute.indexOf("VanillaDamagePolicy.forCause(");
        assertTrue(skip > 0 && skip < policy, "the stamped skip must come before the REROUTE policy: " + skip + " / " + policy);
    }

    /** The plan performed: give back, cancel, spend through the LIVE bow, notify, mark, stamp, redraw. */
    @Test
    void theShotPerformsEveryPartOfThePlan() throws IOException {
        String shot = body("weapon/VanillaShots.java", "public void onShot(EntityShootBowEvent event) {");
        assertTrue(shot.contains("if (!(event.getEntity() instanceof Player player)) return;"), "ruling 13: a mob's bow");
        assertTrue(shot.contains("VanillaShot.of(offHand, weapon.hasQuiver(), before, kind);"), shot);
        assertTrue(shot.contains("if (plan.refund()) giveBack(player, taken);"), shot);
        assertTrue(shot.contains("if (plan.cancel()) event.setCancelled(true);"), shot);
        assertTrue(shot.contains("if (plan.spendRound()) Quivers.spendRoundOnShot(player, weapon, adapters, bow);"), shot);
        assertTrue(shot.contains("if (plan.emptyNotice()) QuiverNotice.empty(player, cooldowns);"), shot);
        assertTrue(shot.contains("keys.quiverPaidShot, PersistentDataType.BYTE"), shot);
        assertTrue(shot.contains("stamp(player, weapon, event.getForce(), projectile);"), shot);
        assertTrue(shot.contains("NexusMirror.redraw(player, keys);"), shot);
    }

    /**
     * READ, ProjectileWeaponItem.shoot: hurtAndBreak runs on vanilla's own bow reference AFTER the event. So the spend at
     * the shot writes through the event's live bow and never replaces the main-hand stack.
     */
    @Test
    void theShotSpendWritesThroughTheLiveBowAndNeverReplacesTheStack() throws IOException {
        String spend = body("weapon/Quivers.java", "public static void spendRoundOnShot(");
        assertTrue(spend.contains("liveBow.editMeta("), spend);
        assertFalse(spend.contains("setItemInMainHand"), "a replaced stack loses the bow's wear to an orphan");
    }

    /** Exemptions 2 and 3 are wired through the named gates the exemption test pins. */
    @Test
    void theTwoExemptionsAreWiredThroughTheirGates() throws IOException {
        assertTrue(body("listener/RpgListeners.java", "public void onWeaponCrossbowLoad(")
                .contains("VanillaShots.crossbowLoadCancelled(id.flatMap(weapons::find))"));
        assertTrue(body("weapon/PlumeDraw.java", "private Optional<WeaponDefinition> drawWeapon(ItemStack item) {")
                .contains(".filter(PlumeDraw::drawManaged);"));
    }
}
