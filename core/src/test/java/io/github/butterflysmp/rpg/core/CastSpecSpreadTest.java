package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * THE GUARD {@code CastSpec.Spread} OWES, AND EVERY ROW HERE CAUSES ITS CONDITION.
 *
 * <p>Written in {@code CastSpecHomingTest}'s shape deliberately -- the two records are siblings,
 * both optional sub-blocks of a projectile cast, both judging values in {@code core} while the
 * schema judges presence. <b>NOT ONE ROW HERE ASSERTS AN ARM EXISTS.</b> Each constructs the bad
 * value and reads the throw, which is {@code CLAUDE.md}'s rule for a guard whose triggering case
 * does not occur in shipped content: <i>write the test that makes the guard fire, and watch it
 * fire.</i>
 *
 * <p><b>Bundled content reaches none of these arms.</b> {@code scatter_shot} authors {@code (7, 5)}
 * and every other weapon authors no spread block at all, so <b>this file is their only
 * exercise</b>, and the record's javadoc says so.
 *
 * <h2>EVERY REFUSAL IS OF A VALUE THAT WOULD HAVE RESOLVED AND SPREAD NOTHING</h2>
 *
 * <p>None of these throws downstream, mis-renders, or produces a visible defect. They produce a
 * spread block that parses, loads, <b>renders {@code x N} on the tooltip</b> and puts every body
 * down one line -- the failure {@code CLAUDE.md} names as a mechanism advertised and never
 * received, with nothing going red. The tooltip half is what makes this worse than the homing
 * case: a dead homing block is invisible, where a dead spread block actively lies.
 *
 * <h2>THE ACCEPTED ROWS ARE CONTROLS, NOT DECORATION</h2>
 *
 * <p>A validator that refuses everything passes every refusal row on this page. The shipped pair
 * and BOTH BOUNDARIES of the angle band are constructed and kept, so a guard widened by one
 * comparison goes red here rather than silently taking the Scatter Shot out of the language.
 */
class CastSpecSpreadTest {

    /**
     * THE SHIPPED PAIR, and the control for every refusal below.
     *
     * <p>{@code scatter_shot.yml}'s own numbers, read off the file rather than invented, so this
     * row fails if the guard ever stops admitting the one weapon that uses it.
     */
    @Test
    void theShippedPairIsAccepted() {
        CastSpec.Spread spread = new CastSpec.Spread(7, 5);
        assertEquals(7, spread.count());
        assertEquals(5, spread.angleDegrees(), 1e-9);
        assertEquals(6, spread.ringCount(), "seven bodies is one down the aim and SIX on the ring");
    }

    /**
     * {@code ringCount} IS {@code count - 1} AND THE HEXAGON FALLS OUT OF IT.
     *
     * <p>Staged at three counts, none of them equal to any other quantity in the row, because the
     * record is an int beside a double and a row that read the wrong field could still pass if the
     * two happened to agree.
     */
    @Test
    void theRingIsEverythingButTheBodyOnTheAimVector() {
        assertEquals(6, new CastSpec.Spread(7, 5).ringCount(), "the shipped hexagon");
        assertEquals(1, new CastSpec.Spread(2, 30).ringCount(), "the smallest legal spread");
        assertEquals(11, new CastSpec.Spread(12, 2.5).ringCount());
    }

    // --- count: a spread of one is a projectile ------------------------------------------------

    /**
     * A SPREAD OF ONE IS REFUSED BECAUSE IT IS NOT WRONG -- IT IS A BLOCK THAT DOES NOTHING.
     *
     * <p>{@code count: 1} resolves perfectly: one body, down the aim vector, which is exactly what
     * a projectile with no spread block does. The weapon should say so by omitting the block.
     * Refused at the door so the file is named and skipped rather than loading a mechanism that is
     * indistinguishable from its own absence.
     */
    @Test
    void aSpreadOfOneIsRefusedRatherThanSilentlyBeingAPlainProjectile() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Spread(1, 5));
        assertTrue(thrown.getMessage().contains("omit the spread block"),
                "the message must say what to do instead; got: " + thrown.getMessage());
    }

    /** Zero and negative counts, which are the same defect further along. */
    @Test
    void aZeroOrNegativeCountIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Spread(0, 5));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Spread(-3, 5));
    }

    /** TWO is the boundary and it is LEGAL -- the control for the three rows above. */
    @Test
    void twoIsTheSmallestLegalSpread() {
        assertEquals(2, new CastSpec.Spread(2, 5).count());
    }

    // --- angleDegrees: the band is (0, 90) -----------------------------------------------------

    /**
     * AT OR BELOW ZERO EVERY BODY FLIES DOWN THE AIM VECTOR, AND THE TOOLTIP STILL SAYS {@code x 7}.
     *
     * <p>This is the arm that matters most, because it is the one a plausible edit produces: an
     * author who wants a tighter group types a smaller number and does not stop at zero. The
     * result is seven exactly-overlapping arrows advertised as a spread.
     */
    @Test
    void aZeroAngleIsRefusedBecauseEveryBodyWouldFlyDownOneLine() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Spread(7, 0));
        assertTrue(thrown.getMessage().contains("overlapping"),
                "the message must name what actually happens; got: " + thrown.getMessage());
    }

    /** A negative angle is the same collapse, arrived at from the other side. */
    @Test
    void aNegativeAngleIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Spread(7, -5));
    }

    /**
     * AT 90 THE RING IS PERPENDICULAR TO THE AIM; PAST IT THE BODIES FLY BEHIND THE SHOOTER.
     *
     * <p>90 itself is refused rather than admitted as a degenerate edge: the forward component is
     * exactly zero there, so the ring bodies travel sideways forever and the centre body is the
     * only one that can ever hit anything.
     */
    @Test
    void ninetyDegreesAndBeyondIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Spread(7, 90));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Spread(7, 120));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Spread(7, 180));
    }

    /**
     * THE BAND'S INTERIOR IS LEGAL RIGHT UP TO THE BOUNDARY -- the control for the row above.
     *
     * <p>{@code 89.9} is admitted. A guard that had been written {@code <= 89} or {@code < 45}
     * would pass every refusal row on this page and fail here.
     */
    @Test
    void theWholeOpenBandIsAccepted() {
        assertEquals(0.1, new CastSpec.Spread(3, 0.1).angleDegrees(), 1e-9);
        assertEquals(89.9, new CastSpec.Spread(3, 89.9).angleDegrees(), 1e-9);
    }

    /**
     * NaN IS REFUSED FOR FREE, WHICH IS WHY THE GUARDS ARE POSITIVE ASSERTIONS.
     *
     * <p>Every comparison against NaN is false, so {@code !(angle > 0)} rejects it without a
     * {@code Double.isNaN} check. Written as a row because the mechanism is invisible in the source
     * -- a reader sees two range checks and no NaN check, and would reasonably add one.
     */
    @Test
    void aNaNAngleIsRefusedByThePositiveFormOfTheGuard() {
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Spread(7, Double.NaN));
    }

    /** Infinity likewise: it fails {@code < 90} rather than needing an arm of its own. */
    @Test
    void anInfiniteAngleIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Spread(7, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Spread(7, Double.NEGATIVE_INFINITY));
    }

    // --- composition ---------------------------------------------------------------------------

    /**
     * A SPREAD AND A HOMING BLOCK COMPOSE, AND THAT IS THE WHOLE ARGUMENT FOR A FIELD.
     *
     * <p>{@code CastSpec.Volley}'s javadoc asks the next person wanting a new shape to reuse rather
     * than propose <i>"a seventh member of a sealed interface that did not need one"</i>. A field
     * on {@code Projectile} composes with everything already there; a member would have had to
     * re-express speed, gravity, trail, body AND homing to say the same thing.
     *
     * <p>No shipped content authors both -- {@code scatter_shot} is ruled NOT homing -- so this row
     * is the only thing establishing that the combination is representable at all.
     */
    @Test
    void aSpreadOfHomingBodiesIsExpressible() {
        CastSpec.Projectile projectile = new CastSpec.Projectile(2.5, 0.05, 120, null, null,
                new CastSpec.Homing(0.65, 15, 10), "arrow", new CastSpec.Spread(7, 5));

        assertNotNull(projectile.homing(), "the two blocks are orthogonal");
        assertNotNull(projectile.spread());
        assertEquals(7, projectile.spread().count());
    }

    /**
     * THE LADDER STILL DROPS THE TAIL, WHICH IS THE PROPERTY THAT LET THIS FIELD BE ADDED AT ALL.
     *
     * <p>{@code Projectile}'s javadoc states the rule: each convenience constructor drops the TAIL,
     * never a middle field, so a reader counting arguments never has to work out which one was
     * omitted. {@code spread} went on the end for that reason even though it reads more naturally
     * beside {@code homing}.
     *
     * <p><b>This row is what makes the claim checkable.</b> Every rung is constructed and its
     * spread asserted absent -- so an edit that inserted the field in the middle, renumbering the
     * rungs, goes red here instead of silently rebinding forty call sites.
     */
    @Test
    void everyConvenienceRungLeavesTheSpreadAbsent() {
        assertNull(new CastSpec.Projectile(2.5, 0.05, 120).spread(), "3-arg");
        assertNull(new CastSpec.Projectile(2.5, 0.05, 120, "trail").spread(), "4-arg");
        assertNull(new CastSpec.Projectile(2.5, 0.05, 120, "trail", "ARROW").spread(), "5-arg");
        assertNull(new CastSpec.Projectile(2.5, 0.05, 120, "trail", "ARROW",
                new CastSpec.Homing(0.65, 15, 10)).spread(), "6-arg");
        assertNull(new CastSpec.Projectile(2.5, 0.05, 120, "trail", null,
                new CastSpec.Homing(0.65, 15, 10), "arrow").spread(), "7-arg");
    }

    /**
     * THE {@code item}/{@code body} EXCLUSION STILL BITES WITH A SPREAD PRESENT.
     *
     * <p>The compact constructor runs before any of this field's logic, and a new tail argument is
     * exactly the kind of edit that reorders a constructor body by accident. Cheap to state,
     * impossible to notice otherwise.
     */
    @Test
    void theOneBodyRuleSurvivesTheNewField() {
        assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Projectile(2.5, 0.05, 120, null, "ARROW", null, "arrow",
                        new CastSpec.Spread(7, 5)));
    }
}
