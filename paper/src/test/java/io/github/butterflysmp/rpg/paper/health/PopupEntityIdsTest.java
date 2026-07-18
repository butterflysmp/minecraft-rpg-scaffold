package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fake entity ids must count strictly DOWN from Integer.MAX_VALUE and never repeat, so a client-only
 * damage number can't collide with a real server entity (which count up from 0). Guards exactly that.
 */
class PopupEntityIdsTest {

    @Test
    void countsDownFromMaxValueWithoutRepeating() {
        PopupEntityIds ids = new PopupEntityIds();

        int first = ids.next();
        int second = ids.next();
        int third = ids.next();

        assertEquals(Integer.MAX_VALUE, first, "the first id is MAX_VALUE -- top of the fake range");
        assertEquals(Integer.MAX_VALUE - 1, second, "and it counts strictly down");
        assertTrue(third < second && second < first, "strictly decreasing");
        assertNotEquals(first, second, "never reused");
        // Mutation: return a constant (or count up) -> a decreasing/non-repeat assertion reddens.
    }

    @Test
    void staysWellAboveRealServerEntityIds() {
        // Real ids count up from 0; even after many allocations the fake ids remain enormous.
        PopupEntityIds ids = new PopupEntityIds();
        int id = 0;
        for (int i = 0; i < 1000; i++) id = ids.next();
        assertTrue(id > Integer.MAX_VALUE - 2000, "still near the top of the int range, nowhere near real ids");
    }
}
