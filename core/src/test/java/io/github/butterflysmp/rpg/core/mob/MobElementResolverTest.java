package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * "The element of this creature" (PLAN-dragon-mobs.md, ruling 2 and step R1): our mob's file first, else the
 * vanilla table by entity type, else none. Paper supplies only the mob id (null for a vanilla mob) and the bare
 * type key.
 */
class MobElementResolverTest {

    private static MobRegistry mobs() {
        MobRegistry mobs = new MobRegistry();
        mobs.register(new MobDefinition("knell", "wither_skeleton", "Knell", 360,
                MobBody.VANILLA, "void", null));
        mobs.register(new MobDefinition("withered_mount", "spider", "Withered Mount", 120,
                MobBody.VANILLA, null, null));
        // A custom mob on an enderman body, authored FIRE: the file must win over the table's void.
        mobs.register(new MobDefinition("ember_walker", "enderman", "Ember Walker", 50,
                MobBody.VANILLA, "fire", null));
        // A custom mob on an enderman body that authors no element: it falls through to the table.
        mobs.register(new MobDefinition("plain_walker", "enderman", "Plain Walker", 50,
                MobBody.VANILLA, null, null));
        return mobs;
    }

    private static final MobElementResolver RESOLVER =
            new MobElementResolver(mobs(), Map.of("enderman", "void"));

    @Test
    void ourMobsFileGivesTheElement() {
        assertEquals(Optional.of("void"), RESOLVER.elementOf("knell", "wither_skeleton"));
    }

    @Test
    void aVanillaMobTakesTheTableByType() {
        assertEquals(Optional.of("void"), RESOLVER.elementOf(null, "enderman"));
    }

    @Test
    void aCreatureInNeitherHasNone() {
        assertEquals(Optional.empty(), RESOLVER.elementOf(null, "zombie"));
        assertEquals(Optional.empty(), RESOLVER.elementOf("withered_mount", "spider"), "the mount: no element");
    }

    @Test
    void theFileWinsOverTheTable() {
        assertEquals(Optional.of("fire"), RESOLVER.elementOf("ember_walker", "enderman"));
    }

    /** The choice this slice makes, stated as a row: a file with no element does not erase the table's. */
    @Test
    void aFileThatAuthorsNoElementFallsThroughToTheTable() {
        assertEquals(Optional.of("void"), RESOLVER.elementOf("plain_walker", "enderman"));
    }

    /** A stale mob_id (a file since removed) is treated as no file, not as no element. */
    @Test
    void anUnknownMobIdFallsThroughToTheTable() {
        assertEquals(Optional.of("void"), RESOLVER.elementOf("deleted_mob", "enderman"));
        assertEquals(Optional.empty(), RESOLVER.elementOf("deleted_mob", "zombie"));
    }
}
