package io.github.yourname.rpg.paper.adapter;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Who to blame for a hit, if anyone.
 *
 * A CombatantHandle is given the caster's UUID, never the caster: a lingering area outlives
 * its thrower, and holding a LivingEntity would pin it for the area's whole life. So the
 * source entity has to be resolved at the moment damage lands -- on the TARGET's thread,
 * which is not necessarily the caster's.
 *
 * That is why ownership is checked. On Paper the main region owns everything, so a caster
 * always resolves and aggro and kill credit work. On Folia a caster who has walked into
 * another region simply does not resolve, and the damage lands unattributed rather than
 * being read across a region boundary. Losing kill credit is a smaller price than
 * corrupting state, and a much smaller one than pinning the entity.
 *
 * Generic in the entity type on purpose: the decision is pure, so both branches are
 * observable in a unit test with no server. A silent degradation that no test can
 * distinguish from success is not a degradation, it is a bug waiting.
 */
final class Attribution {

    private Attribution() {}

    /**
     * @param resolve looks an entity up by id -- nullable, since the caster may have died,
     *                logged out, or been unloaded with their chunk
     * @param owned   whether the region currently being ticked owns that entity
     * @return the entity to attribute the hit to, or null to deal it unattributed
     */
    static <E> E attributableSource(UUID sourceId, Function<UUID, E> resolve, Predicate<E> owned) {
        if (sourceId == null) return null;
        E source = resolve.apply(sourceId);
        if (source == null) return null;
        return owned.test(source) ? source : null;
    }
}
