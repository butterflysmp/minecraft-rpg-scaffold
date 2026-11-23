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
        assertTrue(shot.contains("boolean broken = WeaponDurability.isBroken(bow);"), shot);
        assertTrue(shot.contains("VanillaShot.of(broken, offHand, weapon.hasQuiver(), before, kind);"), shot);
        assertTrue(shot.contains("if (broken) BrokenNotice.notify(player, cooldowns);"), shot);
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

    /**
     * RULING I (Ben's: "I'd rather have our unbreaking work on the two"): a shot that leaves wears by OUR rule, through the
     * one wear path that consults our Unbreaking, written through the live bow; vanilla's wear on the weapon is cancelled.
     */
    @Test
    void aShotThatLeavesWearsByOurRuleAndVanillasWearIsCancelled() throws IOException {
        String shot = body("weapon/VanillaShots.java", "public void onShot(EntityShootBowEvent event) {");
        int leaves = shot.indexOf("if (!plan.cancel()) {");
        int wear = shot.indexOf("WeaponDurability.applyWearOnShot(player, keys, cooldowns, bow);");
        assertTrue(leaves > 0 && wear > leaves, "the wear is charged only on a shot that leaves: " + leaves + " / " + wear);

        String d = "weapon/WeaponDurability.java";
        assertTrue(body(d, "public static void applyWearOnShot(").contains("wearOnce(player, keys, cooldowns, liveBow, false);"));
        assertTrue(body(d, "public static void applyWearOnUse(")
                .contains("wearOnce(player, keys, cooldowns, player.getInventory().getItemInMainHand(), true);"));
        String once = body(d, "private static void wearOnce(");
        assertTrue(once.contains("if (!Unbreaking.consumes(unbreaking, ThreadLocalRandom.current().nextDouble())) return;"),
                "our Unbreaking is consulted on the one path");
        assertTrue(once.contains("if (writeBackToMainHand) player.getInventory().setItemInMainHand(held);"), once);

        String cancel = body("listener/RpgListeners.java", "public void onVanillaShotWeaponWear(PlayerItemDamageEvent event) {");
        assertTrue(cancel.contains("VanillaShots.cancelsVanillaWear("), cancel);
        assertTrue(cancel.contains("event.setCancelled(true);"), cancel);
    }

    /** Exemptions 2 and 3 are wired through the named gates the exemption test pins. */
    @Test
    void theTwoExemptionsAreWiredThroughTheirGates() throws IOException {
        assertTrue(body("listener/RpgListeners.java", "public void onWeaponCrossbowLoad(")
                .contains("VanillaShots.crossbowLoadCancelled(id.flatMap(weapons::find))"));
        assertTrue(body("weapon/PlumeDraw.java", "private Optional<WeaponDefinition> drawWeapon(ItemStack item) {")
                .contains(".filter(PlumeDraw::drawManaged);"));
    }

    // ------------------------------------------------------------------ the seat's follow-ups 2 and 3, 2026-11-22

    private static final String SHOT = "public void onShot(EntityShootBowEvent event) {";

    /**
     * THE MIRROR'S LAST ARROW: put back by the shot path BEFORE the redraw that draws it, so the redraw finds an item to
     * draw on. The decision is core's (VanillaShotTest); the order is pinned here, since redraw returns at once when it
     * finds no Nexus item.
     */
    @Test
    void theShotRestoresTheTakenNexusItemBeforeItRedraws() throws IOException {
        String shot = body("weapon/VanillaShots.java", SHOT);
        int restore = shot.indexOf("restoreTakenNexus(player, kind, keys, profiles, adapters.log());");
        int redraw = shot.indexOf("if (plan.cancel() || kind == VanillaShot.Consumable.MIRROR) NexusMirror.redraw(player, keys);");
        assertTrue(restore > 0 && redraw > restore, "restore at " + restore + ", redraw at " + redraw);

        String mirror = body("listener/RpgListeners.java", "public void onMirrorShot(EntityShootBowEvent event) {");
        int restoreM = mirror.indexOf("VanillaShots.restoreTakenNexus(player,");
        int redrawM = mirror.indexOf("if (nexus) io.github.butterflysmp.rpg.paper.nexus.NexusMirror.redraw(player, adapters.keys());");
        assertTrue(restoreM > 0 && redrawM > restoreM, "onMirrorShot: restore at " + restoreM + ", redraw at " + redrawM);
    }

    /** The heal's own converge call, into his locked slot, and its OWN line: NEXUSHEAL keeps meaning "something else". */
    @Test
    void theRestoreUsesTheHealsConvergeAndItsOwnLine() throws IOException {
        String restore = body("weapon/VanillaShots.java", "public static void restoreTakenNexus(");
        assertTrue(restore.contains(
                "if (!VanillaShot.restoresNexus(taken, NexusHealing.countNexus(player.getInventory(), keys))) return;"), restore);
        assertTrue(restore.contains(".orElse(NexusLock.DEFAULT_LOCKED_SLOT);"), restore);
        assertTrue(restore.contains("NexusSlots.converge(player, keys, slot);"), restore);
        assertTrue(restore.contains("log.info(\"NEXUSRESTORE player=\" + player.getName() + \" slot=\" + slot + \" cause=shot\");"),
                restore);
        assertFalse(restore.contains("NEXUSHEAL"), "the restore must not write the heal's line");
    }

    /** THE CROSSBOW'S CHARGE AFTER THE SHOT (follow-up 3): its own line, so the committed VANILLASHOT form is unchanged. */
    @Test
    void theShotLogsTheCrossbowsChargeOnItsOwnLine() throws IOException {
        String shot = body("weapon/VanillaShots.java", SHOT);
        assertTrue(shot.contains("if (bow.getItemMeta() instanceof CrossbowMeta crossbow) {"), shot);
        assertTrue(shot.contains("\"CROSSBOWCHARGE \" + player.getName() + \" weapon=\" + weapon.id()"), shot);
        assertTrue(shot.contains("\" charged=\" + crossbow.hasChargedProjectiles()"), shot);
    }

    /** /rpg rounds (follow-up 2(c)): dev server only, as /rpg mining is, and through setLoaded, the write-and-render door. */
    @Test
    void theRoundsCommandIsDevServerOnlyAndWritesThroughSetLoaded() throws IOException {
        String command = source("command/RpgCommand.java");
        int literal = command.indexOf(".then(Commands.literal(\"rounds\")");
        assertTrue(literal > 0, "the rounds literal is not found");
        String next = command.substring(literal, command.indexOf('\n', command.indexOf('\n', literal) + 1));
        assertTrue(next.contains(
                ".requires(source -> MiningDevCommand.permitted(source.getSender().hasPermission(Permissions.DEV), devServer))"),
                "the line after the literal must gate it: " + next);
        String set = body("command/RpgCommand.java", "private static int setRounds(");
        assertTrue(set.contains("QuiverItems.setLoaded(meta, weapon.get(), adapters, player.getUniqueId(), count)"), set);
        assertTrue(set.contains("NexusMirror.redraw(player, adapters.keys());"), set);
    }
}
