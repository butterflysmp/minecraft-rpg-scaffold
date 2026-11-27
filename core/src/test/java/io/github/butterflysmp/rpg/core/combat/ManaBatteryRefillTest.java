package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.combat.stat.Stat;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Mana Battery's numbers through the WHOLE CHAIN, EXECUTED (the seat's C2 and C5, 2026-11-27; Ben chose "B": the values
 * are mana per FIVE SECONDS, the sheet's unit). Never worked by hand: the rule is never predict floating point.
 *
 * <pre>
 *   authored (per 5 s) --ManaRegen.perSecondFromPerFiveSeconds--> a piece's modifier (per second)
 *   the stat            = Stat.value(): base 0.0 plus every piece's modifier (and any other source's)
 *   the pool, per tick  = MANA_PER_TICK + ManaRegen.perTick(stat)            (READ, RpgPlugin's resolver)
 *   MANA_PER_TICK       = MAX_MANA / (MANA_REFILL_SECONDS * 20) = 100.0 / (100 * 20)
 *   the sheet           = StatsSheetLines.perFiveSeconds(ManaRegen.perSecond(per tick))   (READ, StatsSheetProjection)
 * </pre>
 */
class ManaBatteryRefillTest {

    /** RpgPlugin's expression, copied with its own types: a double over an int product. */
    static final double MANA_PER_TICK = 100.0 / (100 * 20);

    private static final UUID HIM = new UUID(0, 1);

    /** The stat per second for worn pieces authored at these levels (per 5 s), plus any per-second source (the set). */
    static double stat(double perSecondOther, int... authored) {
        Stat s = new Stat(0.0);
        for (int i = 0; i < authored.length; i++) {
            s.putModifier("manabattery:" + i, ManaRegen.contribution(ManaRegen.perSecondFromPerFiveSeconds(authored[i])));
        }
        if (perSecondOther != 0) s.putModifier("armorset:golden", perSecondOther);
        return s.value();
    }

    static double perTick(double stat) {
        return MANA_PER_TICK + ManaRegen.perTick(stat);
    }

    static String sheet(double stat) {
        return StatsSheetLines.perFiveSeconds(ManaRegen.perSecond(perTick(stat)));
    }

    /** C2: each level, and the sums, printed exactly as the sheet prints them; the raw doubles shown beside. */
    @Test
    void theChain_authoredToTheSheet() {
        String[] labels = {"bare", "I", "II", "III", "III+II", "III x4", "III+II+golden set 2/s"};
        double[] stats = {stat(0), stat(0, 1), stat(0, 2), stat(0, 3), stat(0, 3, 2), stat(0, 3, 3, 3, 3), stat(2.0, 3, 2)};
        String[] printed = new String[stats.length];
        for (int i = 0; i < stats.length; i++) {
            printed[i] = sheet(stats[i]);
            System.out.println("CHAIN " + labels[i] + ": stat/s=" + stats[i] + " perTick=" + perTick(stats[i])
                    + " sheet raw=" + ManaRegen.perSecond(perTick(stats[i])) * StatsSheetLines.RATE_WINDOW_SECONDS
                    + " printed=" + printed[i]);
        }
        assertEquals(java.util.List.of(EXPECTED_SHEET), java.util.List.of(printed));
    }

    /** EXECUTED by this test (2026-11-27), then pinned. Raw: 5.0, 6.000000000000001, 7.000000000000001, 8.0, 10.0, 16.999999999999996, 20.0. */
    static final String[] EXPECTED_SHEET = {"5.00/5s", "6.00/5s", "7.00/5s", "8.00/5s", "10.00/5s", "17.00/5s", "20.00/5s"};

    /** C5, MB3: staged at 10, read 100 ticks later; one piece at III (0.6/s) and the control with none. */
    @Test
    void mb3_theRefillOverAHundredTicks() {
        AtomicLong tick = new AtomicLong(5000);
        double withStat = stat(0, 3);
        ResourcePool with = new ResourcePool(tick::get, (o, r) -> 100.0, (o, r) -> perTick(withStat));
        ResourcePool without = new ResourcePool(tick::get, (o, r) -> 100.0, (o, r) -> perTick(stat(0)));
        with.setCurrent(HIM, "mana", 10.0);
        without.setCurrent(HIM, "mana", 10.0);
        assertEquals(10.0, with.current(HIM, "mana"), 0.0, "the first read, the tick it was staged");
        tick.addAndGet(100);
        System.out.println("MB3 with=" + with.current(HIM, "mana") + " without=" + without.current(HIM, "mana")
                + " perTick(0.6)=" + ManaRegen.perTick(withStat));
        assertEquals(EXPECTED_MB3_WITH, with.current(HIM, "mana"), 0.0);
        assertEquals(15.0, without.current(HIM, "mana"), 0.0);
    }

    /** EXECUTED: 10 + 100 x (0.05 + ManaRegen.perTick(0.6)), perTick(0.6) = 0.03. */
    static final double EXPECTED_MB3_WITH = 18.0;
}
