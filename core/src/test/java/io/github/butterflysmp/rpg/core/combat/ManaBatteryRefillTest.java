package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GATE-mana-battery's MB2 to MB5, EXECUTED through the pool's own functions rather than worked by hand (the rule:
 * never predict floating point). The resolver is RpgPlugin's, term for term:
 *
 * <pre>
 *   per tick  = MANA_PER_TICK + ManaRegen.perTick(bonus per second)
 *   MANA_PER_TICK = MAX_MANA / (MANA_REFILL_SECONDS * 20) = 100.0 / (100 * 20)      (READ, RpgPlugin)
 *   the sheet = StatsSheetLines.perFiveSeconds(ManaRegen.perSecond(per tick))         (READ, StatsSheetProjection)
 * </pre>
 *
 * The bonus is the stat's value: base 0.0 plus every source (the TEMP fixture, the armor set, accessories, fragments
 * and now Mana Battery's pieces), summed by {@code Stat.value()}.
 */
class ManaBatteryRefillTest {

    /** RpgPlugin's expression, copied with its own types: a double over an int product. */
    static final double MANA_PER_TICK = 100.0 / (100 * 20);

    private static final UUID HIM = new UUID(0, 1);

    private static ResourcePool pool(AtomicLong tick, double bonusPerSecond) {
        return new ResourcePool(tick::get, (o, r) -> 100.0,
                (o, r) -> MANA_PER_TICK + ManaRegen.perTick(bonusPerSecond));
    }

    /** MB3: staged at 10, read 100 ticks later; with Mana Battery III (+3/s) and with nothing (the control). */
    @Test
    void mb3_theRefillOverAHundredTicks() {
        AtomicLong tick = new AtomicLong(5000);
        ResourcePool with = pool(tick, 3.0);
        ResourcePool without = pool(tick, ManaRegen.NONE);
        with.setCurrent(HIM, "mana", 10.0);
        without.setCurrent(HIM, "mana", 10.0);
        assertEquals(10.0, with.current(HIM, "mana"), 0.0, "the first read, the tick it was staged");
        tick.addAndGet(100);
        // EXECUTED values, pinned exactly: 10 + 100 x (0.05 + 0.15) and 10 + 100 x 0.05.
        assertEquals(30.0, with.current(HIM, "mana"), 0.0);
        assertEquals(15.0, without.current(HIM, "mana"), 0.0);
        assertEquals(0.05, MANA_PER_TICK, 0.0);
        assertEquals(0.15, ManaRegen.perTick(3.0), 0.0);
    }

    /** The sheet's line for each bonus a row reads: none, I, II, III, III+II, and III+II with the golden set's +2. */
    @Test
    void theSheetsManaRegenLine_forEachRowsBonus() {
        double[] bonus = {0, 1, 2, 3, 5, 7};
        String[] sheet = {"5.00/5s", "10.00/5s", "15.00/5s", "20.00/5s", "30.00/5s", "40.00/5s"};
        for (int i = 0; i < bonus.length; i++) {
            double perTick = MANA_PER_TICK + ManaRegen.perTick(bonus[i]);
            assertEquals(sheet[i], StatsSheetLines.perFiveSeconds(ManaRegen.perSecond(perTick)), "bonus " + bonus[i]);
        }
    }
}
