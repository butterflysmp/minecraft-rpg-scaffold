package io.github.butterflysmp.rpg.paper.accessory;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.accessory.AccessoryBehaviour;
import io.github.butterflysmp.rpg.core.accessory.AccessoryContributions.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.accessory.Blinks;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Blink's wiring (K1, K4, K5, K7, K9, K12). Its DECISIONS are core's tables ({@code BlinksTest}); what can only be
 * read here is which function each seam calls, the line formats, and the block classification. A Bukkit-free test cannot
 * run a tick or teleport anybody, so the clock's behaviour is witnessed by the gate; these rows pin its shape.
 */
class BlinkTomesWiringTest {

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

    // --- K12: the trace lines, whole ----------------------------------------------------------------------------------

    @Test
    void theTraceLines() {
        UUID z = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        assertEquals("BLINK wearer=Ben tick=1000 trigger=zombie 00000000-0000-0000-0000-00000000000a dist=3.000"
                        + " from=200.500 201.000 0.500 to=210.500 201.000 -4.500 safe=412 forgot=1",
                BlinkTomes.blinkLine("Ben", 1000, "zombie " + z, 3.0, new Vec3(200.5, 201, 0.5), new Vec3(210.5, 201, -4.5),
                        412, 1));
        assertEquals("BLINK NONE-SAFE wearer=Ben tick=1000 trigger=zombie " + z + " searched=3985",
                BlinkTomes.noneSafeLine("Ben", 1000, "zombie " + z, 3985));
        assertEquals("BLINK REFUSED-TARGET mob=zombie " + z + " tick=1005", BlinkTomes.refusedLine("zombie", z, 1005));
        for (Blinks.EndReason r : Blinks.EndReason.values()) {
            assertEquals("BLINK END wearer=Ben tick=1160 reason=" + r.name().toLowerCase(), BlinkTomes.endLine("Ben", 1160, r));
        }
        assertEquals(List.of("expired", "cast", "melee", "death", "quit", "cleared"),
                Stream.of(Blinks.EndReason.values()).map(Blinks.EndReason::token).toList(), "K12's five and the selftest's clear");
    }

    @Test
    void blinkOfFindsTheBlink_andNothingElse() {
        AccessoryBehaviour pulse = new AccessoryBehaviour.HealingPulse("P", 1, 80, 8, AccessoryBehaviour.Targets.PLAYERS,
                null, 0, null, null);
        AccessoryBehaviour blink = new AccessoryBehaviour.Blink("B", 5, 16, 5, 160, 1200, null, 0, null);
        assertTrue(BlinkTomes.blinkOf(List.of()).isEmpty());
        assertTrue(BlinkTomes.blinkOf(List.of(new ActiveBehaviour(0, "healing_tome", pulse))).isEmpty());
        assertEquals("blink_tome", BlinkTomes.blinkOf(List.of(new ActiveBehaviour(0, "healing_tome", pulse),
                new ActiveBehaviour(0, "blink_tome", blink))).orElseThrow().accessoryId());
    }

    // --- K3: the block classification ---------------------------------------------------------------------------------

    @Test
    void theFloorAndBodyClassification() {
        assertEquals(Blinks.Floor.SAFE, BlinkTomes.floorOf(Material.STONE, true));
        assertEquals(Blinks.Floor.NOT_SOLID, BlinkTomes.floorOf(Material.AIR, false));
        assertEquals(Blinks.Floor.NOT_SOLID, BlinkTomes.floorOf(Material.WATER, false));
        for (Material m : List.of(Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.CACTUS,
                Material.POINTED_DRIPSTONE, Material.LAVA_CAULDRON, Material.SCULK_SHRIEKER, Material.BIG_DRIPLEAF)) {
            assertEquals(Blinks.Floor.HARMFUL, BlinkTomes.floorOf(m, true), m + " is solid and harmful: refused");
        }
        assertEquals(Blinks.Body.CLEAR, BlinkTomes.bodyOf(Material.AIR, false, true));
        assertEquals(Blinks.Body.CLEAR, BlinkTomes.bodyOf(Material.SHORT_GRASS, false, true), "a plant he walks through");
        assertEquals(Blinks.Body.SOLID, BlinkTomes.bodyOf(Material.STONE, false, false));
        assertEquals(Blinks.Body.FLUID, BlinkTomes.bodyOf(Material.WATER, true, true));
        assertEquals(Blinks.Body.FLUID, BlinkTomes.bodyOf(Material.LAVA, true, true));
        assertEquals(Blinks.Body.FLUID, BlinkTomes.bodyOf(Material.SEAGRASS, true, true), "waterlogged");
        for (Material m : List.of(Material.FIRE, Material.SOUL_FIRE, Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE,
                Material.POWDER_SNOW, Material.NETHER_PORTAL, Material.END_PORTAL, Material.END_GATEWAY, Material.COBWEB)) {
            assertEquals(Blinks.Body.HARMFUL, BlinkTomes.bodyOf(m, false, true), m + " is passable and harmful: refused");
        }
    }

    // --- K1: when it runs ---------------------------------------------------------------------------------------------

    @Test
    void theClockIsReconciledFromTheOneReading_andReChecksThroughIt() throws IOException {
        String h = source("health/PlayerHealthSystem.java");
        assertTrue(h.contains("if (blinkTomes != null) blinkTomes.reconcile(player, fromAccessories.behaviours());"),
                "the reconcile pass hands its ONE reading's behaviours");
        String start = body("accessory/BlinkTomes.java", "private void startClock(Player player, ActiveBehaviour active) {");
        assertTrue(start.contains("blinkOf(contributions.apply(id).behaviours())"), "each check re-asks the same function");
        assertTrue(start.contains("Blinks.CHECK_PERIOD_TICKS"));
        assertEquals(5, Blinks.CHECK_PERIOD_TICKS, "CC'S CALL: within the brief's 10");
        String src = source("accessory/BlinkTomes.java");
        for (String store : List.of("accessories()", "AccessoryService", "readSlot", ".slots(")) {
            assertFalse(src.contains(store), "BlinkTomes reads no slot: " + store);
        }
        String rp = source("RpgPlugin.java");
        assertTrue(rp.contains("healthSystem::accessoryContributionsFor, new java.util.SplittableRandom());"));
        assertTrue(rp.contains("adapters.targetGuard().bind(blinkTomes);") && rp.contains("adapters.attacks().bind(blinkTomes);"));
    }

    // --- K4, K5: the state's end ----------------------------------------------------------------------------------------

    /** K5: the ONE place a trigger fires past its gates, and only for a non-summoner weapon. */
    @Test
    void aCastEndsIt_atTheFireSeam_neverForASummonerWeapon() throws IOException {
        String fire = source("weapon/WeaponFire.java");
        int success = fire.indexOf("if (r instanceof CastResult.Success success) {");
        int hook = fire.indexOf("adapters.attacks().attacked(player, io.github.butterflysmp.rpg.core.accessory.Blinks.EndReason.CAST);");
        int summon = fire.indexOf("CastSpec.Summon summonCast) {");
        int direct = fire.indexOf("CastSpec.DirectSummon direct) {");
        assertTrue(success > 0 && hook > success, "inside the Success branch, past the cooldown and mana gates");
        assertTrue(summon > 0 && direct > 0 && summon < success && direct < success,
                "the summon and the direct order return before the Success branch is reached");
        String guard = fire.substring(fire.lastIndexOf("if (", hook), hook);
        assertTrue(guard.contains("weapon.weaponClass() != io.github.butterflysmp.rpg.core.weapon.WeaponClass.SUMMONER"),
                "a summoner weapon's trigger is not an attack: " + guard);
        assertEquals(1, count(fire, "Blinks.EndReason.CAST"), "one place");
        // Mutation: drop the SUMMONER condition -> reddens.
    }

    /** K5: the ONE place his melee hit lands, after the broken weapon's cancel and the summon refusal. */
    @Test
    void aMeleeHitEndsIt_whereItLands() throws IOException {
        String melee = body("listener/RpgListeners.java", "public void onPlayerMeleeAttack(EntityDamageByEntityEvent event) {");
        int hook = melee.indexOf("adapters.attacks().attacked(attacker, io.github.butterflysmp.rpg.core.accessory.Blinks.EndReason.MELEE);");
        assertTrue(hook > melee.indexOf("if (refusedForPlayer(victim, event)) return;"), "after the summon refusal");
        assertTrue(hook > melee.indexOf("WeaponDurability.isHeldWeaponBroken"), "after the broken weapon's cancel");
        assertTrue(hook > melee.indexOf("DamageCause.ENTITY_ATTACK) return;"), "an ENTITY_ATTACK only");
        assertEquals(1, count(source("listener/RpgListeners.java"), "Blinks.EndReason.MELEE"), "one place");
    }

    @Test
    void deathAndQuitEndIt() throws IOException {
        String l = source("listener/RpgListeners.java");
        assertTrue(body("listener/RpgListeners.java", "public void onPlayerDeath(PlayerDeathEvent event) {")
                .contains("healthSystem.blinkTomes().ifPresent(b -> b.onDeath(event.getPlayer()));"));
        String quit = body("listener/RpgListeners.java", "public void onQuit(PlayerQuitEvent event) {");
        int hook = quit.indexOf("healthSystem.blinkTomes().ifPresent(b -> b.onQuit(event.getPlayer()));");
        assertTrue(hook > 0 && hook < quit.indexOf("healthSystem.onQuit(playerId);"), "while he is valid, before the store drops him");
        assertTrue(l.contains("cooldowns.clear(playerId);"), "READ: the cooldown store is cleared on quit (K9's CC'S CALL)");
    }

    // --- K7: the targeting seams ----------------------------------------------------------------------------------------

    /** The engine's one targeting seam: no other non-null setTarget in main code. */
    @Test
    void everySetTargetOfOursGoesThroughTheSeam() throws IOException {
        Pattern call = Pattern.compile("\\.setTarget\\(([^)]*)\\)");
        List<String> outside = new ArrayList<>();
        int seen = 0;
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String file = p.toString().replace('\\', '/');
                for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    String t = line.trim();
                    if (t.startsWith("*") || t.startsWith("//") || t.startsWith("/*")) continue;
                    Matcher m = call.matcher(line);
                    while (m.find()) {
                        seen++;
                        if (m.group(1).trim().equals("null")) continue;          // forgetting is never refused
                        if (file.endsWith("adapter/MobTargets.java")) continue;   // the seam itself
                        outside.add(file + ": " + t);
                    }
                }
            }
        }
        assertTrue(seen >= 2, "control: the scan sees the seam's call and the forget's (" + seen + ")");
        assertEquals(List.of(), outside, "a target set outside MobTargets would bypass the blink's refusal");
        assertTrue(source("adapter/BukkitCombatant.java").contains("MobTargets.set(mob, attacker, ctx.targetGuard());"));
        assertTrue(source("selftest/SelfTestDrivers.java").contains("MobTargets.set((org.bukkit.entity.Mob) best, clear ? null : p,"),
                "the driver's set, and its clear form (G-S2, 2026-11-26), both through the seam");
    }

    @Test
    void vanillasTargetEventIsRefusedAtHighest() throws IOException {
        String l = source("listener/RpgListeners.java");
        String sig = "public void onHiddenTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent event) {";
        int at = l.indexOf(sig);
        assertTrue(at > 0);
        String ann = l.substring(l.lastIndexOf("@EventHandler", at), at);
        assertTrue(ann.contains("EventPriority.HIGHEST") && !ann.contains("ignoreCancelled"), ann);
        assertTrue(body("listener/RpgListeners.java", sig).contains("adapters.targetGuard().refuses(event.getEntity(), event.getTarget())"));
    }

    @Test
    void ourMobAbilitiesSkipAHiddenPlayer_forANewVictimOnly() throws IOException {
        assertTrue(body("health/MobBeamer.java", "private boolean eligible(Mob mob, LivingEntity l, MobBeam beam, Vec3 eye) {")
                .contains("adapters.targetGuard().hides(l)"));
        assertTrue(body("health/MobPouncer.java", "private LivingEntity choose(Pair p, Vec3 feet) {")
                .contains("adapters.targetGuard().hides(l)"));
        String shooter = source("health/MobShooter.java");
        String choose = body("health/MobShooter.java", "private LivingEntity choose(Mob mob, MobShot shot, Vec3 eye, boolean blinked) {");
        assertEquals(2, count(choose, "chooseable("), "the vanilla target and the scan");
        assertTrue(body("health/MobShooter.java", "private LivingEntity reacquire(Owner o, Live s) {").contains("chooseable("));
        assertTrue(shooter.contains("if (s.target != null && !eligible(o.mob, s.target)) {"),
                "a shot already flying at him keeps him: damage on its way still reaches him");
    }

    // --- K9: the cooldown -----------------------------------------------------------------------------------------------

    @Test
    void theCooldownIsTheEnginesStore_spentOnlyOnASuccessfulBlink() throws IOException {
        String blink = body("accessory/BlinkTomes.java", "private void blink(Player player, AccessoryBehaviour.Blink blink, Blinks.Trigger trigger, int now) {");
        int none = blink.indexOf("return;   // K3: nothing happens, the cooldown is NOT spent");
        int spend = blink.indexOf("cooldowns.trigger(id, COOLDOWN_KEY, blink.cooldownTicks());");
        assertTrue(none > 0 && spend > none, "the cooldown is spent only after a safe spot was found");
        assertEquals("accessory:blink", BlinkTomes.COOLDOWN_KEY);
        assertFalse(source("accessory/BlinkTomes.java").contains("ResourcePool"), "K9: no mana is read or spent");
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) n++;
        return n;
    }
}
