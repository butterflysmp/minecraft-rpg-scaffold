package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.accessory.AccessoryBehaviour;
import io.github.butterflysmp.rpg.core.accessory.AccessoryLoreLines;
import io.github.butterflysmp.rpg.core.accessory.HealingPulses;
import io.github.butterflysmp.rpg.core.combat.HealthRegen;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import io.github.butterflysmp.rpg.core.progression.LevelBonus;
import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.paper.content.AccessoryLoader;
import io.github.butterflysmp.rpg.paper.weapon.AccessoryLore;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-healing-tome.md's every predicted number, EXECUTED through the real functions (S-1: nothing derived). The wound is
 * {@code /rpg damage <W>}: {@code RpgCommand.damageSelf} calls {@code CombatantStats.damage(id, W, id, true)} (READ), so the
 * dealer is HE, there is no score, attack attribute or difficulty term, and the only other term is his defence.
 */
class HealingTomeGateArithmeticTest {

    private static final UUID BEN = UUID.nameUUIDFromBytes("ben".getBytes());
    private static final double REGEN = 0.2;   // staged: base 0.2/s, level bonus 0 below 10, armour off, slots cleared

    private static AccessoryDefinition tome() {
        return new AccessoryLoader(Logger.getLogger("t"), 100.0)
                .loadAll(new File("src/main/resources/content/accessories"), id -> false)
                .find("healing_tome").orElseThrow();
    }

    /** His store at level L, full, as the row stages it: base 100 + the level's max (no other source). */
    private static CombatantStats at(int level, List<HealthChange> changes) {
        CombatantStats stats = new CombatantStats(changes::add);
        stats.register(BEN, CombatantStats.DEFAULT_PLAYER_BASE, true);
        LevelBonus bonus = LevelBonus.at(level);
        stats.reconcileMaxModifiers(BEN, bonus.sources(io.github.butterflysmp.rpg.core.accessory.AccessoryStat.MAX_HEALTH));
        stats.heal(BEN, 1_000_000, BEN, true);
        return stats;
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    @Test
    void theLevelsTerms() {
        assertEquals(5.0, LevelBonus.at(2).maxHealth());
        assertEquals(10.0, LevelBonus.at(3).maxHealth());
        for (int l : new int[] {2, 3}) {
            assertEquals(0.0, LevelBonus.at(l).defense(), "no level defence at " + l);
            assertEquals(0.0, LevelBonus.at(l).healthRegenPerSecond(), "no level regen at " + l);
        }
        assertEquals(REGEN, HealthRegen.BASE_PER_SECOND);
        // One unsaturated payment (saturation 0), a 20-tick window: exactly the rate.
        assertEquals(0.2, HealthRegen.healAmount(REGEN, false, 20, 98.0, 105.0));
    }

    /** S-1: each row's wound, through the store, at the staged level and defence 0. The SAFETY sum. */
    @Test
    void theWounds_andTheSafetySum() {
        record Wound(String row, int level, int w, String hurt, String hp) {}
        List<Wound> wounds = List.of(
                new Wound("TP1", 2, 7, "before=7.000 after=7.000", "98.000/105.000"),
                new Wound("TP2", 3, 8, "before=8.000 after=8.000", "102.000/110.000"),
                new Wound("TP3", 3, 2, "before=2.000 after=2.000", "108.000/110.000"),
                new Wound("TP4", 2, 2, "before=2.000 after=2.000", "103.000/105.000"));
        double sum = 0;
        for (Wound w : wounds) {
            List<HealthChange> changes = new ArrayList<>();
            CombatantStats stats = at(w.level(), changes);
            stats.damage(BEN, w.w(), BEN, true);
            HealthChange hurt = changes.getLast();
            assertEquals(HealthChange.Kind.DAMAGE, hurt.kind());
            assertEquals(w.hurt(), "before=" + f(hurt.beforeDefense()) + " after=" + f(hurt.amount()), w.row());
            assertEquals(w.hp(), f(stats.current(BEN)) + "/" + f(stats.max(BEN)), w.row());
            assertTrue(stats.current(BEN) >= stats.max(BEN) / 2, w.row() + ": never below half his staged max");
            sum += hurt.amount();
        }
        assertEquals("19.000", f(sum), "THE SUM FOR THE WHOLE COMMAND (the lower gates' 0.000 each), at or under 20.000");
        assertTrue(sum <= 20.0);
    }

    /**
     * The pulses of TP1, TP2 and TP3 at EVERY count of regeneration payments the timeline allows (n1 in {3, 4} before the
     * first pulse; n2 in {7, 8} before the second), through the real decision and the real store.
     */
    @Test
    void thePulses_atEveryAllowedRegenCount() {
        AccessoryBehaviour.HealingPulse pulse = (AccessoryBehaviour.HealingPulse) tome().behaviour();
        record Row(String row, int level, int w, double expect1, Double expect2) {}
        for (Row r : List.of(new Row("TP1", 2, 7, 2.0, 2.0), new Row("TP2", 3, 8, 3.0, 3.0),
                new Row("TP3", 3, 2, Double.NaN, null))) {
            for (int n1 : new int[] {3, 4}) {
                for (int n2 : new int[] {7, 8}) {
                    List<HealthChange> changes = new ArrayList<>();
                    CombatantStats stats = at(r.level(), changes);
                    stats.damage(BEN, r.w(), BEN, true);
                    for (int i = 0; i < n1; i++) regen(stats);
                    double before1 = stats.current(BEN);
                    double a1 = pulseOnce(pulse, r.level(), stats);
                    if (r.row().equals("TP3")) {
                        assertEquals(f(stats.max(BEN) - before1), f(a1), "TP3: amount = missing");
                        assertEquals("110.000", f(stats.current(BEN)), "TP3: exactly max");
                        for (int i = n1; i < n2; i++) regen(stats);
                        assertEquals(-1.0, pulseOnce(pulse, r.level(), stats), "TP3's second pulse: nobody hurt");
                        continue;
                    }
                    assertEquals(r.expect1(), a1, r.row() + " pulse 1, n1=" + n1);
                    for (int i = n1; i < n2; i++) regen(stats);
                    assertEquals(r.expect2(), pulseOnce(pulse, r.level(), stats), r.row() + " pulse 2, n2=" + n2);
                }
            }
        }
        // The general forms the gate writes, at n1 = 3 (TP1): before = 98.000 + 0.200 x n1 = 98.600; after 100.600.
        List<HealthChange> changes = new ArrayList<>();
        CombatantStats stats = at(2, changes);
        stats.damage(BEN, 7, BEN, true);
        for (int i = 0; i < 3; i++) regen(stats);
        assertEquals("98.600", f(stats.current(BEN)));
        pulseOnce(pulse, 2, stats);
        assertEquals("100.600", f(stats.current(BEN)));
    }

    /**
     * HT6's control pulse: a wound of 2 at level 2, and at least 12 regeneration payments before the pulse (250 ticks or
     * more). Is he EXACTLY full, so the pulse reads healed=0 and never a 0.000 heal? Executed, not predicted: at every
     * count from 10 (the fewest that could fill 2.0) to 20.
     */
    @Test
    void ht6sControlPulseFindsHimExactlyFull() {
        AccessoryBehaviour.HealingPulse pulse = (AccessoryBehaviour.HealingPulse) tome().behaviour();
        for (int n = 10; n <= 20; n++) {
            List<HealthChange> changes = new ArrayList<>();
            CombatantStats stats = at(2, changes);
            stats.damage(BEN, 2, BEN, true);
            for (int i = 0; i < n; i++) regen(stats);
            if (n >= 12) {
                assertEquals(stats.max(BEN), stats.current(BEN), "exactly full after " + n);
                assertEquals(-1.0, pulseOnce(pulse, 2, stats), "healed=0 after " + n + " payments");
            }
        }
    }

    private static void regen(CombatantStats stats) {
        double paid = HealthRegen.healAmount(REGEN, false, 20, stats.current(BEN), stats.max(BEN));
        if (paid > 0) stats.heal(BEN, paid, BEN, true);
    }

    /** One pulse on him alone: the decision, then the heal TomePulses.apply makes. Returns the amount, or -1 for none. */
    private static double pulseOnce(AccessoryBehaviour.HealingPulse pulse, int level, CombatantStats stats) {
        HealingPulses.Decision d = HealingPulses.decide(pulse, level, "world", new Vec3(0, 64, 0), List.of(
                new HealingPulses.Candidate(BEN, "Ben", HealingPulses.Kind.PLAYER, "world", new Vec3(0, 64, 0), true, false,
                        stats.current(BEN), stats.max(BEN))));
        assertEquals(1, d.candidates());
        if (d.healed() == 0) return -1.0;
        double amount = Math.min(d.heals().get(0).amount(), stats.max(BEN) - stats.current(BEN));
        stats.heal(BEN, amount, BEN, true);
        return amount;
    }

    /** TP8: the HELD line's name and lore, through the instrument's own formatter over the mint's own builders. */
    @Test
    void theHeldLine() {
        AccessoryDefinition tome = tome();
        String name = SelfTestLines.styled(WeaponItems.displayName(tome.displayName(), tome.rarity()));
        List<String> lore = new ArrayList<>();
        for (Component c : AccessoryLore.build(tome)) lore.add(SelfTestLines.styled(c));
        assertEquals("\"Healing Tome\" [blue]", name);
        assertEquals("[\"Radiant Pulse\" [gold]; \"Every 4s, heals hurt players within 8 blocks\" [gray]; "
                + "\"for 1 health per your level\" [gray]; \"Mage class accessory\" [gray]; \"\" []; "
                + "\"Rare Accessory\" [blue]]", "[" + String.join("; ", lore) + "]");
    }

    /** TP4 and TP9: the stats sheet's accessory lines, as the REPLY lines print them. */
    @Test
    void theSheetLines() {
        AccessoryDefinition tome = tome();
        assertEquals("Healing Tome: Radiant Pulse", AccessoryLoreLines.sheetLine("Healing Tome", tome, true, "Magic"));
        assertEquals("Healing Tome: inactive (requires Mage)", AccessoryLoreLines.sheetLine("Healing Tome", tome, false, "Magic"));
        assertEquals(Map.of(), tome.modifiers());
    }
}
