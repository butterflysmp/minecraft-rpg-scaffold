package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Lookup by id, and the guards on a mob definition's required fields. */
class MobRegistryTest {

    private static MobDefinition knell() {
        return new MobDefinition("knell", "wither_skeleton", "Knell", 360);
    }

    @Test
    void findsARegisteredMobById() {
        MobRegistry mobs = new MobRegistry();
        mobs.register(knell());

        MobDefinition found = mobs.find("knell").orElseThrow();

        assertEquals("wither_skeleton", found.baseEntity());
        assertEquals("Knell", found.displayName());
        assertEquals(360, found.maxHealth(), 1e-9);
        assertEquals(1, mobs.size());
    }

    @Test
    void anUnknownIdIsEmptyNotAThrow() {
        assertTrue(new MobRegistry().find("nobody").isEmpty());
    }

    @Test
    void aDuplicateIdIsRejected() {
        MobRegistry mobs = new MobRegistry();
        mobs.register(knell());

        assertThrows(IllegalStateException.class, () -> mobs.register(knell()));
    }

    /** base_entity is REQUIRED: a silent default would spawn the wrong creature and look deliberate. */
    @Test
    void aMobWithNoBaseEntityIsRejected() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> new MobDefinition("knell", null, "Knell", 360));
        assertTrue(ex.getMessage().contains("knell"), "the message must name the file at fault");
        assertThrows(IllegalArgumentException.class,
                () -> new MobDefinition("knell", "  ", "Knell", 360));
    }

    /** 0 max HP is born dead or unkillable depending on the reachedZero transition. Not a legal value. */
    @Test
    void aNonPositiveMaxHealthIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new MobDefinition("knell", "wither_skeleton", "Knell", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new MobDefinition("knell", "wither_skeleton", "Knell", -5));
    }
}
