package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** F9: each of the dragon's five part sizes (READ, {@code EnderDragon.<init>}) names its part; any other is "part". */
class DragonPartsTest {

    @Test
    void eachReadSizeNamesItsPart() {
        assertEquals("head", DragonParts.nameOf(1.0, 1.0));
        assertEquals("neck", DragonParts.nameOf(3.0, 3.0));
        assertEquals("body", DragonParts.nameOf(5.0, 3.0));
        assertEquals("tail", DragonParts.nameOf(2.0, 2.0));
        assertEquals("wing", DragonParts.nameOf(4.0, 2.0));
    }

    @Test
    void widthAndHeightAreNotInterchangeable() {
        // The body is 5 wide and 3 high; 3 wide and 5 high is no part. A swap of the two arguments reddens this.
        assertEquals("part", DragonParts.nameOf(3.0, 5.0));
        assertEquals("part", DragonParts.nameOf(2.0, 4.0));
    }

    @Test
    void anyOtherSizeIsJustAPart() {
        assertEquals("part", DragonParts.nameOf(16.0, 8.0));
        assertEquals("part", DragonParts.nameOf(1.01, 1.0));
    }
}
