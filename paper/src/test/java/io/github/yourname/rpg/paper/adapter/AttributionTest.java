package io.github.yourname.rpg.paper.adapter;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * No server. The decision is pure -- resolve an id, check who owns it -- so it is tested
 * with String stand-ins for entities. Only the entity.damage(...) call that follows needs
 * Bukkit, and that stays unverified until someone boots a server.
 *
 * These two branches look identical from outside: damage lands either way. A silent
 * degradation nothing can distinguish from success is not a degradation, it is a bug
 * waiting for a bug report. So both are pinned.
 */
class AttributionTest {

    private static final UUID CASTER = UUID.randomUUID();

    private static final Predicate<String> OWNED_BY_THIS_REGION = e -> true;
    private static final Predicate<String> OWNED_BY_ANOTHER_REGION = e -> false;

    private static Function<UUID, String> world(Map<UUID, String> entities) {
        return entities::get;
    }

    @Test
    void attributesWhenTheSourceResolvesAndThisRegionOwnsIt() {
        var resolve = world(Map.of(CASTER, "the caster"));

        assertEquals("the caster",
                Attribution.attributableSource(CASTER, resolve, OWNED_BY_THIS_REGION));
    }

    /** The caster died, logged out, or unloaded with their chunk. Their grenade lives on. */
    @Test
    void doesNotAttributeWhenTheSourceHasGone() {
        var resolve = world(Map.of());

        assertNull(Attribution.attributableSource(CASTER, resolve, OWNED_BY_THIS_REGION));
    }

    /**
     * The Folia case, and the whole reason this class exists. The caster is alive and
     * resolvable, but belongs to a region this thread is not ticking. Reading them here
     * would be the cross-region access the port split exists to remove, so the damage is
     * dealt unattributed instead: no aggro, no kill credit, no corruption.
     */
    @Test
    void doesNotAttributeAcrossARegionBoundary() {
        var resolve = world(Map.of(CASTER, "the caster, elsewhere"));

        assertNull(Attribution.attributableSource(CASTER, resolve, OWNED_BY_ANOTHER_REGION),
                "a source owned by another region must not be attributed");
    }

    /** A lingering area whose caster was never recorded. Nothing to look up, nothing to blame. */
    @Test
    void doesNotAttributeANullSource() {
        assertNull(Attribution.attributableSource(null, world(Map.of()), OWNED_BY_THIS_REGION));
    }

    /** Ownership is never asked about an entity that does not exist. */
    @Test
    void doesNotTestOwnershipOfAMissingSource() {
        Predicate<String> exploding = e -> { throw new AssertionError("must not test a null source"); };

        assertNull(Attribution.attributableSource(CASTER, world(Map.of()), exploding));
    }
}
