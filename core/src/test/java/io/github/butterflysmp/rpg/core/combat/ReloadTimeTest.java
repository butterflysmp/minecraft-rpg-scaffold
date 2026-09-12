package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.weapon.Quiver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reload time: whole ticks added to a weapon's authored reload duration.
 *
 * <p>The numbers are {@code quiver_stone}'s shipped {@code reload_ticks: 34} and the A2 instrument's
 * {@code +14}, resolving to <b>48</b>. Swept free of every authored value in {@code content/}, and
 * <b>the swept quantity is the RESOLVED one</b> -- a gate row reads what is on screen.
 *
 * <p><b>THE DOWNWARD ROW IS THE ONE THAT MATTERS AND THE ONE NOTHING ELSE COVERS.</b> In play,
 * reload gear REDUCES the number; A2 ships no instrument that can, because at base 34 every
 * collision-free reducing value lands on an authored number or on the bonus itself. So this class is
 * the whole of the slice's coverage for the direction the feature will actually be used in.
 *
 * <p>Each row names what it forces red. <b>Three were RUN</b>, spliced by line number, marker grepped
 * both directions, byte delta recomputed from the edit:
 *
 * <table><tr><th>mutation</th><th>edit</th><th>bytes</th><th>result</th></tr>
 * <tr><td>{@code MUTRTCLAMP}</td><td>a floor added to {@code resolve}: {@code Math.max(1, ...)}</td>
 *     <td>+27 = 12 + 1 + 14</td>
 *     <td><b>1 of 7 red</b>: the downward row, {@code expected 0 but was 1}. <b>The ruling that
 *     reload has NO floor is therefore enforced by a test rather than asserted in prose</b> -- the
 *     only kind of "we decided not to" that survives a later reader.</td></tr>
 * <tr><td>{@code MUTRTROUND}</td><td>{@code ticks}: {@code Math.floor} -> {@code Math.round}</td>
 *     <td>+14 = 0 + 14</td>
 *     <td><b>1 of 7 red</b> on {@code -2.1}, which is this stat's LIVE direction -- a row staged
 *     only on positives would have stayed green</td></tr>
 * <tr><td>{@code MUTGATE}</td><td>{@code declares}: {@code != NONE} -> {@code > NONE}, i.e. the
 *     sibling's gate, which is what this file shipped with for one commit</td>
 *     <td>+10 = -1 + 11</td>
 *     <td><b>2 red, in BOTH modules</b>: this class's gate row and
 *     {@code ReloadTimeModifierItemsTest.aReducingInstrumentDECLARESAModifierRatherThanBeingSilentlyDropped}.
 *     The scanner and the helper are guarded independently, so reverting either one is caught.</td></tr>
 * </table>
 *
 * <p>All three restored from scratchpad copies, byte-identical by {@code cmp}, {@code markers left: 0}.
 */
class ReloadTimeTest {

    /** {@code quiver_stone.yml}'s authored reload duration, in ticks. */
    private static final int AUTHORED = 34;

    /** The A2 instrument's bonus. Resolves to 48; 14, 34 and 48 are pairwise different. */
    private static final int INSTRUMENT_BONUS = 14;

    /** An arbitrary tick to start a reload on -- the arithmetic is relative, the value is not. */
    private static final long NOW = 1_000L;

    /**
     * ZERO DECLARES NOTHING; A NEGATIVE DECLARES A REDUCTION. <b>The opposite of what this row
     * asserted when it was written, and the reason is the whole of the {@code declares} rename.</b>
     *
     * <p>It read {@code assertFalse(boosts(-1), "a reducing amount must not become a modifier")} --
     * copied verbatim from {@code QuiverSizeTest}, where it is correct. This stat's sign is
     * inverted, so that gate forbade <b>reload-speed gear entirely</b>: an item carrying −5 would
     * declare nothing and do nothing, silently, every scan, while this class's own downward row
     * proved the arithmetic beneath it worked. <b>Ruled by the operator: both directions.</b>
     *
     * <p>Forces red: {@code declares} narrowed back to {@code > NONE} -- which is precisely the
     * change a later reader makes to bring it into line with its five siblings, so the row asserts
     * the negative case first and by name.
     */
    @Test
    void zeroDeclaresNothingButANEGATIVEDeclaresAReduction() {
        assertTrue(ReloadTime.declares(-5),
                "A RELOAD-SPEED ITEM CARRIES A NEGATIVE. Gating on > NONE would drop it silently, "
                        + "which is what > NONE did here until review caught it.");
        assertTrue(ReloadTime.declares(-1), "one tick faster is a real modifier");
        assertTrue(ReloadTime.declares(1), "and one tick slower is too");
        assertTrue(ReloadTime.declares(INSTRUMENT_BONUS));
        assertFalse(ReloadTime.declares(ReloadTime.NONE),
                "only 0 declares nothing -- a no-op source written every scan is churn");

        assertFalse(QuiverSize.boosts(-5),
                "AND THE SIBLING IS DELIBERATELY THE OTHER WAY: for quiver size, increase-only is a "
                        + "real content ruling -- content cannot make a quiver smaller. The two "
                        + "helpers differ because their SIGNS differ, not by oversight.");
    }

    @Test
    void theContributionIsTheBonusItself() {
        assertEquals(INSTRUMENT_BONUS, ReloadTime.contribution(INSTRUMENT_BONUS));
        assertEquals(ReloadTime.NONE, ReloadTime.contribution(ReloadTime.NONE));
    }

    /**
     * WITH NO GEAR THE RESOLVED DURATION IS EXACTLY THE AUTHORED ONE -- the neutral that keeps every
     * existing weapon reloading at the speed it did before this stat existed.
     */
    @Test
    void withNoGearTheResolvedDurationIsExactlyTheAuthoredOne() {
        assertEquals(AUTHORED, ReloadTime.resolve(AUTHORED, 0.0));
        assertEquals(1, ReloadTime.resolve(1, 0.0), "and the authored minimum is untouched too");
        // Forces red: a base other than 0, or an off-by-one in resolve.
    }

    /**
     * The instrument's own arithmetic: 34 + 14 = 48, and no two of the three are equal.
     *
     * <p>Forces red: resolve returning the base, the bonus, or a product. 14, 34 and 48 share no
     * value, so a row that read one for another cannot pass here.
     */
    @Test
    void theInstrumentResolvesToFortyEight() {
        int resolved = ReloadTime.resolve(AUTHORED, INSTRUMENT_BONUS);
        assertEquals(48, resolved, "quiver_stone's 34 plus the instrument's +14");
        assertNotEquals(AUTHORED, INSTRUMENT_BONUS);
        assertNotEquals(AUTHORED, resolved);
        assertNotEquals(INSTRUMENT_BONUS, resolved);
    }

    /**
     * THE DOWNWARD DIRECTION, AND ZERO IS AN INSTANT RELOAD RATHER THAN A BROKEN ONE.
     *
     * <p><b>This row is the slice's only witness for the direction gear will actually move this
     * stat</b>, because no A2 instrument can reduce it. It is also the measurement behind the ruling
     * that reload gets NO FLOOR: the standard is <i>is there a resolved value at which the mechanic
     * has no reachable state</i>, and the answer is measured here rather than argued.
     *
     * <p>At a resolved 0 the deadline equals the start and {@code reloadComplete} is true on that
     * same tick -- the reload stamps, matures and clears. Compare capacity 0, where
     * {@code QuiverStateTest.aCapacityOfZeroWouldBeARefusalOnBothInputs} measures both inputs refused
     * with no recovery. <b>Different answers from the same standard, which is what makes it a
     * standard rather than a preference.</b>
     *
     * <p>An instant reload deletes the brake this feature is about. That is a BALANCE objection and
     * it stays where it sits -- priced on Q7, which is unrun -- rather than being smuggled in as a
     * mechanism floor. Three earlier attempts to do exactly that are recorded on
     * {@code ReloadTime}'s class javadoc.
     *
     * <p>Forces red: any floor or clamp added to {@code resolve}; {@code reloadCompletesAt} losing
     * its {@code Math.max}, which would stamp a deadline BEFORE its own start.
     */
    @Test
    void theResolvedDurationGoesDownFreelyAndZeroIsAnInstantReload() {
        assertEquals(20, ReloadTime.resolve(AUTHORED, -14.0), "34 - 14, passed straight through");
        assertEquals(0, ReloadTime.resolve(AUTHORED, -34.0), "exactly 0 is reachable");
        assertEquals(-6, ReloadTime.resolve(AUTHORED, -40.0),
                "and BELOW zero is not refused here -- there is no floor in resolve, deliberately");

        long completesAt = Quiver.reloadCompletesAt(NOW, ReloadTime.resolve(AUTHORED, -34.0));
        assertEquals(NOW, completesAt, "a 0-tick reload's deadline IS its start");
        assertTrue(Quiver.reloadComplete(NOW, NOW, completesAt),
                "and it is complete on that same tick -- stamped, matured, cleared. A reachable "
                        + "state, which is why there is no mechanism argument for a reload floor.");
        assertEquals(0L, Quiver.reloadTicksRemaining(NOW, NOW, completesAt));

        long fromNegative = Quiver.reloadCompletesAt(NOW, ReloadTime.resolve(AUTHORED, -40.0));
        assertEquals(NOW, fromNegative,
                "a NEGATIVE resolved duration is clamped by reloadCompletesAt's Math.max -- one "
                        + "enforcement site, and a representability rule (a deadline is never before "
                        + "its own start) rather than a balance floor");
    }

    /**
     * FLOORING FAVOURS THE PLAYER HERE, AND THE HOUSE RULE IS THE MODE, NOT THE PHRASE.
     *
     * <p>{@code QuiverSize} and {@code Quiver.applyPercent} both justify {@code Math.floor} with
     * <i>"it rounds against the player"</i>. That phrase is FALSE of this stat and copying it across
     * would have been a word carried for being the house rule rather than for being true -- the
     * failure {@code NEXT.md} now names as its sixth shape.
     *
     * <p>A bigger capacity helps; a bigger reload hurts. Same {@code Math.floor}, opposite sides:
     * {@code arrows(2.9)} grants 2 where round would grant 3 (less, against them);
     * {@code ticks(-2.1)} takes 3 ticks off where round would take 2 (shorter, for them).
     *
     * <p>What carries over is ONE ROUNDING MODE PER WEAPON. Forces red: {@code Math.round} or a bare
     * {@code (int)} cast -- the cast truncates toward zero, so it agrees with floor on {@code +2.9}
     * and disagrees on every negative, which is precisely this stat's live direction.
     */
    @Test
    void flooringFavoursThePlayerHereAndTheHouseRuleIsTheMODENotThePhrase() {
        assertEquals(-3, ReloadTime.ticks(-2.1), "floor -2.1 = -3; round says -2; a cast says -2");
        assertEquals(-3, ReloadTime.ticks(-2.9));
        assertEquals(2, ReloadTime.ticks(2.9), "floor 2.9 = 2; round would say 3");
        assertEquals(2, ReloadTime.ticks(2.0), "an exact value is unchanged");

        assertEquals(2, QuiverSize.arrows(2.9), "the sibling, same mode");
        assertTrue(ReloadTime.resolve(AUTHORED, -2.1) < ReloadTime.resolve(AUTHORED, -2.0),
                "flooring a REDUCTION makes the reload shorter, which helps the player -- the "
                        + "opposite side from the capacity stat, under the identical rounding mode");
    }

    /**
     * A SUM OF WHOLE-TICK MODIFIERS ROUND-TRIPS EXACTLY, which is why the {@code double} storage is
     * not a hazard: {@code contribution} takes an {@code int}, and integral doubles are exact far
     * below {@code 2^53}.
     */
    @Test
    void aSumOfWholeTickModifiersRoundTripsExactly() {
        double summed = ReloadTime.contribution(INSTRUMENT_BONUS)
                + ReloadTime.contribution(-5)
                + ReloadTime.contribution(2);
        assertEquals(11.0, summed, 0.0, "14 - 5 + 2, summed as doubles, is exact");
        assertEquals(11, ReloadTime.ticks(summed));
        assertEquals(AUTHORED + 11, ReloadTime.resolve(AUTHORED, summed));
    }
}
