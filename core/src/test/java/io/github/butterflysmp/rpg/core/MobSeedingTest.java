package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobSeeding;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The separation guarantee: a tagged mob scales, an untagged one is untouched.
 *
 * Both directions are asserted because only one of them is the feature. A resolver that returned the
 * definition's HP for everything would make the Knell look perfect while silently turning every wither
 * skeleton on the server into a 360-HP monster -- a bug that presents as success.
 */
class MobSeedingTest {

    private static final double VANILLA_WITHER_SKELETON = 20.0;
    private static final double EPS = 1e-9;

    private static MobRegistry registryWithKnell() {
        MobRegistry mobs = new MobRegistry();
        mobs.register(new MobDefinition("knell", "wither_skeleton", "Knell", 360));
        return mobs;
    }

    @Test
    void aTaggedMobSeedsFromItsDefinition() {
        assertEquals(360, MobSeeding.maxHealth(registryWithKnell(), "knell", VANILLA_WITHER_SKELETON), EPS,
                "the Knell's content max_health, not the entity's vanilla 20");
    }

    /**
     * The half that protects every ordinary mob on the server. A null tag must take the vanilla value
     * through untouched -- this is the path an unmodified wither skeleton, zombie or cow follows, and
     * it must behave exactly as it did before custom mobs existed.
     */
    @Test
    void anUntaggedMobKeepsItsVanillaMaxExactly() {
        assertEquals(VANILLA_WITHER_SKELETON,
                MobSeeding.maxHealth(registryWithKnell(), null, VANILLA_WITHER_SKELETON), EPS,
                "no mob_id tag means vanilla, unchanged -- the separation the whole pass exists for");
    }

    /**
     * Keying is by ID, not by entity type. The registry holds a wither-skeleton-based mob, but an
     * untagged wither skeleton must not inherit it -- which is exactly what a type-keyed
     * implementation would get wrong while passing the tagged test above.
     */
    @Test
    void knowingAMobBasedOnAnEntityTypeDoesNotScaleThatType() {
        MobRegistry mobs = registryWithKnell();   // knell IS a wither_skeleton

        double untagged = MobSeeding.maxHealth(mobs, null, VANILLA_WITHER_SKELETON);

        assertEquals(VANILLA_WITHER_SKELETON, untagged, EPS,
                "a wither skeleton with no tag stays 20 even though a wither-skeleton mob is loaded");
    }

    /**
     * A tag naming a mob the registry does not know -- a content file renamed or deleted while a
     * tagged mob is still alive in a loaded chunk. Fail soft to vanilla: 0 would make it unkillable or
     * born dead, and throwing would blow up an entity-add event.
     */
    @Test
    void aDanglingTagFallsBackToVanillaRatherThanZeroOrThrowing() {
        assertEquals(VANILLA_WITHER_SKELETON,
                MobSeeding.maxHealth(registryWithKnell(), "deleted_boss", VANILLA_WITHER_SKELETON), EPS);
    }

    @Test
    void theDefinitionsValueIsUsedEvenWhenItDwarfsVanillasCap() {
        // The custom store is uncapped, so a boss past vanilla's 1024 is the same mechanism as 360.
        MobRegistry mobs = new MobRegistry();
        mobs.register(new MobDefinition("wraith", "wither_skeleton", "Wraith", 5000));

        assertEquals(5000, MobSeeding.maxHealth(mobs, "wraith", VANILLA_WITHER_SKELETON), EPS);
    }
}
