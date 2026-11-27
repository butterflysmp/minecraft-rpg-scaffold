package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DRUMMING BEAT'S WIRING, READ OFF THE SOURCE (the Bone Club's M4 to M8, 2026-11-26): the paper half needs a live server,
 * so its order is pinned here, every needle on code lines only (comments filtered), each with a position. The decision's
 * own table is core's DrummingBeatTest. Mutations named per test.
 */
class DrummingBeatWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    private static List<String> code(String rel) throws IOException {
        return Files.readAllLines(MAIN.resolve(rel), StandardCharsets.UTF_8).stream()
                .filter(l -> !(l.trim().startsWith("//") || l.trim().startsWith("*") || l.trim().startsWith("/*"))).toList();
    }

    private static int at(List<String> c, String needle, int from) {
        for (int i = Math.max(0, from); i < c.size(); i++) if (c.get(i).contains(needle)) return i;
        return -1;
    }

    /**
     * M6: the decision is made BEFORE the snapshot (the one roll), its bonus is the snapshot's critBonus, and the streak is
     * kept only for a hit that LANDED, after it. Mutations: the snapshot without the bonus (MUT-NOBONUS); the put before the
     * landing or without the landed check (MUT-PUTEARLY: a whiffed swing would count).
     */
    @Test
    void theDecisionFeedsTheOneRollAndOnlyALandedHitMovesTheStreak() throws IOException {
        List<String> l = code("weapon/WeaponFire.java");
        int m = at(l, "public static Optional<Landed> landVanillaMelee(", 0);
        int decide = at(l, "? io.github.butterflysmp.rpg.core.enchant.DrumBeats.decide(drums.of(id), org.bukkit.Bukkit.getCurrentTick(),", m);
        int bonus = at(l, "double critBonus = drum == null ? 0.0 : drum.bonus();", decide);
        int snap = at(l, "CombatantSnapshot caster = BukkitCombatant.snapshot(attacker, adapters.stats(), adapters.keys(), critBonus);", bonus);
        int land = at(l, ".landBasicMelee(trigger.get(), caster, target, chargeScale);", snap);
        int gate = at(l, "if (drum != null && landed[0] != null) {", land);
        int put = at(l, "drums.put(id, drum.after());", gate);
        assertTrue(m > 0 && decide > m && bonus > decide && snap > bonus && land > snap && gate > land && put == gate + 1,
                "decide, bonus, snapshot with it, land, then keep the streak only if it landed");
        assertEquals(1, l.stream().filter(x -> x.contains("drums.put(")).count(), "one put, the gated one");
    }

    /** "Full charge" is the charge term's own reading, and only a mob hit is asked. Mutation: > for >=; the mob gate dropped. */
    @Test
    void fullChargeAndTheMobGate() throws IOException {
        List<String> l = code("weapon/WeaponFire.java");
        assertTrue(at(l, "boolean charged = charge >= io.github.butterflysmp.rpg.core.combat.AttackCharge.FULL_CHARGE;", 0) > 0);
        assertTrue(at(l, "boolean onMob = io.github.butterflysmp.rpg.core.mob.MobHostility.countsAsAMobHit(", 0) > 0);
        assertTrue(at(l, "io.github.butterflysmp.rpg.core.enchant.DrumBeats.Decision drum = club && onMob", 0) > 0);
        assertTrue(at(l, "if (!club) drums.forget(id);", 0) > 0, "a hit with a weapon no Drumming Beat names forgets the streak");
        assertTrue(at(l, "double chargeScale = io.github.butterflysmp.rpg.core.combat.AttackCharge.scale(charge);", 0) > 0,
                "the scale is still the curve of the same reading");
    }

    /** The snapshot adds the bonus to the resolved chance right before the one roll. Mutation: the term dropped. */
    @Test
    void theSnapshotAddsTheBonusAtTheRoll() throws IOException {
        List<String> l = code("adapter/BukkitCombatant.java");
        int four = at(l, "public static CombatantSnapshot snapshot(LivingEntity entity, CombatantStats stats, Keys keys, double critBonus) {", 0);
        int read = at(l, "double critChance = stats.critChanceValue(entity.getUniqueId()) + critBonus;", four);
        int roll = at(l, "Crit.multiplier(critChance, critDamage, critRoll),", read);
        assertTrue(four > 0 && read > four && roll > read, "the bonus enters the roll");
        assertTrue(at(l, "return snapshot(entity, stats, keys, 0.0);", 0) > 0, "every other caller adds nothing");
    }

    /** The caller passes the RAW charge (the scale moved inside). Mutation: the scaled charge passed (never full). */
    @Test
    void theRiderPassesTheRawCharge() throws IOException {
        List<String> l = code("listener/RpgListeners.java");
        assertTrue(at(l, "WeaponFire.landVanillaMelee(attacker, victim, swing.get().charge(),", 0) > 0);
    }

    /** M5: every reset that is not a hit forgets the streak. Mutation: any one handler removed. */
    @Test
    void everyResetForgets() throws IOException {
        List<String> l = code("listener/RpgListeners.java");
        for (String handler : List.of("onDrumSlotChange(PlayerItemHeldEvent", "onDrumSwapHands(", "onDrumDrop(",
                "onDrumInventoryClick(", "onDrumInventoryDrag(", "onDrumDeath(", "onDrumWorldChange(")) {
            int m = at(l, "public void " + handler, 0);
            assertTrue(m > 0, handler);
            assertTrue(l.get(m + 1).contains("adapters.behaviourHits().drums().forget("), handler + " forgets");
        }
        int quit = at(l, "public void onQuit(PlayerQuitEvent event) {", 0);
        assertTrue(at(l, "adapters.behaviourHits().drums().forget(event.getPlayer().getUniqueId());", quit) > quit, "quit");
        List<String> d = code("selftest/SelfTestDrivers.java");
        int clear = at(d, "private Optional<String> clearState(Player p) {", 0);
        assertTrue(at(d, "adapters.behaviourHits().drums().forget(id);", clear) > clear, "clear_state");
        assertTrue(at(d, "+ \" summons=\" + summoned + \" fused=\" + fused + \" drum=\" + drum + \" blink=\" + (blink ? \"ended\" : \"none\"));", clear) > clear, "and says the n");
        List<String> r = code("selftest/SelfTestRun.java");
        int forget = at(r, "drumStreaks.forget(player.getUniqueId());", 0);
        int clean = at(r, "log(\"CLEAN blocks=", forget);
        assertTrue(forget > 0 && clean > forget && at(r, "+ \" drum=\" + drum + \" blink=\" + (blink ? \"ended\" : \"none\"));", clean) > clean, "the row-start CLEAN");
    }

    /** M8: the trace line is logged only while trace is on, with the chance the snapshot rolled. */
    @Test
    void theTraceLine() throws IOException {
        List<String> l = code("weapon/WeaponFire.java");
        int gate = at(l, "if (drum != null && landed[0] != null) {", 0);
        int on = at(l, "if (adapters.mobTrace().getAsBoolean()) {", gate);
        int line = at(l, "adapters.log().info(io.github.butterflysmp.rpg.core.enchant.DrumBeats.line(drum, step,", on);
        int chance = at(l, "adapters.stats().critChanceValue(id) + critBonus, charged));", line);
        assertTrue(gate > 0 && on > gate && line > on && chance == line + 1);
    }
}
