package io.github.butterflysmp.rpg.paper.adapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * THE ARROW BODY'S ARMED SELF-DESTRUCT IS ONE INTEGER AND IT HAS AN OFF-BY-ONE THAT FAILS SILENT.
 *
 * <p>Read out of the pinned server jar, {@code AbstractArrow.tickDespawn()} is:
 *
 * <pre>
 *   life++;  if (life &gt;= rate) discard(DESPAWN);
 * </pre>
 *
 * <p>{@code rate} is read from CONFIG at runtime -- {@code non-player-arrow-despawn-rate}, falling
 * back to spigot's {@code arrow-despawn-rate} -- so the item marker's trick of subtracting from a
 * compiled-in constant cannot be used. {@code PaperCombatWorld} arms {@code life} instead, so that
 * the FIRST despawn tick the body is ever given is fatal <b>whatever the rate is set to</b>.
 *
 * <h2>*** THE TRAP: {@code Integer.MAX_VALUE} IS THE OBVIOUS VALUE AND IT DISARMS THE WHOLE THING ***</h2>
 *
 * <pre>
 *   life = MAX_VALUE - 1   ->  life++ lands on MAX_VALUE     ->  MAX_VALUE &gt;= rate   DISCARDS
 *   life = MAX_VALUE       ->  life++ OVERFLOWS to MIN_VALUE ->  MIN_VALUE &gt;= rate   NEVER discards
 * </pre>
 *
 * <p><b>The wrong value is the one that reads safer</b>, and it fails by leaving the body alive for
 * the full configured rate -- 1200 ticks on this server, sixty seconds -- with nothing red anywhere.
 * There is no runtime symptom short of a boot row that watches an orphan.
 *
 * <p><b>This row is the only guard of that literal.</b> No other test in the suite reads
 * {@code ARMED_ARROW_LIFETIME}, and the constant is not reachable from content, so tidying it to
 * {@code Integer.MAX_VALUE} would be green everywhere else. It asserts the ARITHMETIC rather than
 * the number, so it survives the constant being re-derived and reddens only if the property breaks.
 *
 * <p>Package-private for this: the constant is an implementation detail of one adapter method and
 * is deliberately not public. That is why this class sits in the adapter's own package.
 */
class PlumeBodyLifetimeTest {

    /**
     * THE INCREMENT MUST NOT OVERFLOW, AND IT MUST LAND AT THE CEILING.
     *
     * <p>Both halves, because either alone passes under a wrong value: a bare "does not overflow"
     * passes at {@code 0}, and a bare "is large" passes at {@code MAX_VALUE}. Together they name
     * exactly one integer.
     */
    @Test
    void theArmedLifetimeSurvivesItsOwnIncrementAndLandsAtTheCeiling() {
        int afterTheIncrement = PaperCombatWorld.ARMED_ARROW_LIFETIME + 1;

        assertTrue(afterTheIncrement > PaperCombatWorld.ARMED_ARROW_LIFETIME,
                "life++ must NOT overflow: at Integer.MAX_VALUE it wraps to MIN_VALUE and the "
                        + "body then outlives the whole configured despawn rate, silently");
        assertEquals(Integer.MAX_VALUE, afterTheIncrement,
                "and it must land exactly on the ceiling, so `life >= rate` is true for every int "
                        + "rate an operator can configure -- which is the entire point of a "
                        + "sentinel rather than an arithmetic pre-age");
    }

    /**
     * AND THE COMPARISON THE SENTINEL EXISTS TO WIN, RUN AGAINST THE RATES THAT CAN ACTUALLY REACH IT.
     *
     * <p>Staged at this repo's configured 1200, at the vanilla default 6000, and at the largest
     * value the config type can hold. <b>No two of these are equal</b>, and none equals the
     * constant, so a transposition between any of them has nowhere to hide.
     *
     * <p>{@code 0} is included deliberately: an operator CAN configure a zero rate, and a sentinel
     * that only worked for large rates would be a guard with a hole in exactly the configuration
     * that needs it least and reveals it soonest.
     */
    @Test
    void theFirstDespawnTickIsFatalAtEveryRateAnOperatorCanConfigure() {
        int afterTheIncrement = PaperCombatWorld.ARMED_ARROW_LIFETIME + 1;

        for (int rate : new int[] {0, 1200, 6000, Integer.MAX_VALUE}) {
            assertTrue(afterTheIncrement >= rate,
                    "tickDespawn discards when life >= rate; it must do so on the FIRST call at "
                            + "rate " + rate);
        }
    }
}
