package io.github.butterflysmp.rpg.core.build;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The section 2.4.1 formula and the per-field legality, over the whole grid the plan names. */
class NumberResolutionTest {

    private static NumberChange flat(AspectField field, double flat) { return new NumberChange(field, flat, 0); }
    private static NumberChange pct(AspectField field, double percent) { return new NumberChange(field, 0, percent); }

    private static BigDecimal resolve(double base, NumberChange... changes) {
        return NumberResolution.resolve(base, List.of(changes));
    }

    @Test
    void flatOnlyPercentOnlyAndBoth() {
        assertEquals(0, new BigDecimal("200").compareTo(resolve(160, flat(AspectField.COOLDOWN_TICKS, 40))));
        assertEquals(0, new BigDecimal("9").compareTo(resolve(12, pct(AspectField.DAMAGE_AMOUNT, -25))));
        // (10 + 2) x (1 + 50/100) = 18
        assertEquals(0, new BigDecimal("18").compareTo(
                resolve(10, new NumberChange(AspectField.DAMAGE_AMOUNT, 2, 50))));
    }

    /** The sums commute: A then B equals B then A. */
    @Test
    void twoChangesCommute() {
        NumberChange a = new NumberChange(AspectField.COST, 10, 20);
        NumberChange b = new NumberChange(AspectField.COST, -5, -10);
        assertEquals(0, resolve(35, a, b).compareTo(resolve(35, b, a)));
    }

    /**
     * THE PAIR EXAMPLE (section 2.4.1): -25% and +10% on a 12 are SUMMED to -15%, not multiplied. The expected
     * value is the expression EXECUTED here, not a decimal written from reasoning -- and it is asserted to
     * differ from the multiplicative 12 x 0.75 x 1.10.
     */
    @Test
    void percentsAreSummedNotMultiplied() {
        BigDecimal resolved = resolve(12, pct(AspectField.DAMAGE_AMOUNT, -25), pct(AspectField.DAMAGE_AMOUNT, 10));
        BigDecimal summed = new BigDecimal("12").multiply(BigDecimal.ONE.add(new BigDecimal("-15").movePointLeft(2)));
        assertEquals(0, summed.compareTo(resolved), "(12 + 0) x (1 + (-25 + 10)/100)");
        double multiplied = 12 * 0.75 * 1.10;
        assertNotEquals(multiplied, resolved.doubleValue(), 1e-9);
    }

    // ------------------------------------------------------------------ legality: refuse, never round

    /** cooldown_ticks: an integer, a multiple of 4 (the held right-click grid), above the floor, above 0. */
    @Test
    void cooldownTicksOnTheGrid() {
        NumberResolution.Context noFloor = NumberResolution.Context.cooldown(0);
        assertEquals(Optional.empty(), NumberResolution.refusal(AspectField.COOLDOWN_TICKS, new BigDecimal("240"), noFloor));
        Optional<String> twoThirty = NumberResolution.refusal(AspectField.COOLDOWN_TICKS, new BigDecimal("230"), noFloor);
        assertTrue(twoThirty.isPresent(), "230 is not a multiple of 4");
        assertTrue(twoThirty.get().contains("228") && twoThirty.get().contains("232"), twoThirty.get());
        assertTrue(NumberResolution.refusal(AspectField.COOLDOWN_TICKS, new BigDecimal("0"), noFloor).isPresent(), "0");
        assertTrue(NumberResolution.refusal(AspectField.COOLDOWN_TICKS, new BigDecimal("241.2"), noFloor).isPresent(),
                "not a whole tick");
    }

    @Test
    void cooldownTicksUnderAVolleyFloorIsRefused() {
        NumberResolution.Context floor40 = NumberResolution.Context.cooldown(40);
        Optional<String> under = NumberResolution.refusal(AspectField.COOLDOWN_TICKS, new BigDecimal("36"), floor40);
        assertTrue(under.isPresent() && under.get().contains("40"), under.toString());
        assertEquals(Optional.empty(), NumberResolution.refusal(AspectField.COOLDOWN_TICKS, new BigDecimal("40"), floor40));
    }

    /** area.duration_ticks: QUANTISED to its interval -- a change that does not cross one changes nothing. */
    @Test
    void areaDurationOnItsInterval() {
        NumberResolution.Context every20 = NumberResolution.Context.interval(20);
        Optional<String> oneTen = NumberResolution.refusal(AspectField.AREA_DURATION_TICKS, new BigDecimal("110"), every20);
        assertTrue(oneTen.isPresent() && oneTen.get().contains("100") && oneTen.get().contains("120"), oneTen.toString());
        assertEquals(Optional.empty(), NumberResolution.refusal(AspectField.AREA_DURATION_TICKS, new BigDecimal("120"), every20));
    }

    @Test
    void costMayBeZeroNotNegative() {
        NumberResolution.Context none = NumberResolution.Context.NONE;
        assertTrue(NumberResolution.refusal(AspectField.COST, new BigDecimal("-1"), none).isPresent());
        assertEquals(Optional.empty(), NumberResolution.refusal(AspectField.COST, BigDecimal.ZERO, none), "0 is free");
    }

    @Test
    void theMagnitudesMustStayAboveZero() {
        for (AspectField field : List.of(AspectField.DAMAGE_AMOUNT, AspectField.HEAL_AMOUNT,
                AspectField.KNOCKBACK_STRENGTH, AspectField.BURST_RADIUS, AspectField.AREA_RADIUS)) {
            assertTrue(NumberResolution.refusal(field, BigDecimal.ZERO, NumberResolution.Context.NONE).isPresent(),
                    field + " at 0 is the drawback that does nothing");
            assertEquals(Optional.empty(), NumberResolution.refusal(field, new BigDecimal("0.5"), NumberResolution.Context.NONE),
                    field.token());
        }
    }

    @Test
    void statusDurationIsWholeTicksAboveZero() {
        NumberResolution.Context none = NumberResolution.Context.NONE;
        assertEquals(Optional.empty(), NumberResolution.refusal(AspectField.STATUS_DURATION_TICKS, new BigDecimal("80"), none));
        assertTrue(NumberResolution.refusal(AspectField.STATUS_DURATION_TICKS, new BigDecimal("80.5"), none).isPresent());
        assertTrue(NumberResolution.refusal(AspectField.STATUS_DURATION_TICKS, BigDecimal.ZERO, none).isPresent());
    }

    // ------------------------------------------------------------------ the whitelist and a change's shape

    @Test
    void theWhitelistIsExactlyTheNineFields() {
        assertEquals(List.of("cooldown_ticks", "cost", "damage.amount", "heal.amount", "knockback.strength",
                        "burst.radius", "area.radius", "area.duration_ticks", "status.duration_ticks"),
                java.util.Arrays.stream(AspectField.values()).map(AspectField::token).toList());
        assertEquals(Optional.of(AspectField.DAMAGE_AMOUNT), AspectField.fromToken("damage.amount"));
        assertEquals(Optional.empty(), AspectField.fromToken("weapon_damage"), "off the list, deliberately");
        assertEquals(Optional.empty(), AspectField.fromToken("area.tick_interval"), "off the list, deliberately");
    }

    @Test
    void aChangeMustChangeSomethingFinite() {
        assertThrows(IllegalArgumentException.class, () -> new NumberChange(AspectField.COST, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new NumberChange(AspectField.COST, Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new NumberChange(AspectField.COST, 0, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new NumberChange(null, 1, 0));
    }
}
