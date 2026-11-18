package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.paper.health.DamageNumberText;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OWED FROM F1-F3 (PLAN-dragon-fight.md section 6.1), the text half: a {@code Double.MAX_VALUE} strike's HURT line and
 * its damage number carry no {@code Infinity} and no {@code NaN}. The store half is core's {@code MaxValueStrikeTest}.
 */
class MaxValueLinesTest {

    @Test
    void theHurtLineForAMaxValueStrikeIsFinite() {
        String line = SelfTestBlast.hurtLine(Double.MAX_VALUE, Double.MAX_VALUE, 40.0, "self", "self");
        assertFalse(line.contains("Infinity"), line);
        assertFalse(line.contains("NaN"), line);
        assertTrue(line.startsWith("HURT before=1797693134862315"), line);
    }

    @Test
    void theDamageNumberForAMaxValueStrikeIsFinite() {
        String text = PlainTextComponentSerializer.plainText().serialize(DamageNumberText.of(Double.MAX_VALUE));
        assertEquals(Long.toString(Long.MAX_VALUE), text, "Math.round saturates at Long.MAX_VALUE");
    }

    @Test
    void controlTheNeedleSeesAnInfinity() {
        String line = SelfTestBlast.hurtLine(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0, "self", "self");
        assertTrue(line.contains("Infinity"), "the check above would see an infinity: " + line);
    }
}
