package io.github.butterflysmp.rpg.core.combat;

import java.util.UUID;

/**
 * How fast one owner's pool of one resource refills, in units PER TICK.
 *
 * <p>The sibling of {@link MaxResolver}, and the second half of the same lift. Slice 2b made the
 * CEILING a question with an owner in it; the SLOPE stayed a single {@code double} for exactly as
 * long as nothing could change one player's rate.
 *
 * <h2>PER TICK, because the pool counts ticks</h2>
 *
 * {@link ResourcePool} regenerates lazily as {@code amount + elapsed * rate}, where {@code elapsed}
 * is in ticks, so this is the pool's native unit and no conversion happens inside it. The STAT behind
 * a paper implementation is per SECOND -- that is the unit a player reads and the unit
 * {@code HealthRegen} already uses -- and {@code ManaRegen} owns the single conversion between them.
 *
 * <p><b>Composition happens in ticks, and that is a floating-point decision rather than a taste
 * one.</b> {@code 100.0/(60*20)} and {@code (100.0/60.0)/20.0} differ by one ULP -- measured,
 * {@code 0x1.5555555555555p-4} against {@code 0x1.5555555555556p-4} -- so an implementation that
 * added a per-second bonus to a per-second base and converted the sum would shift the shipped rate
 * for every player, including players wearing nothing. Adding {@code ManaRegen.perTick(bonus)} to the
 * per-tick base keeps an unenchanted player bit-for-bit unchanged, because {@code perTick(0.0)} is
 * exactly {@code 0.0} and {@code x + 0.0 == x}.
 *
 * <h2>It must be TOTAL</h2>
 *
 * Called from {@code tryConsume} on whichever thread is casting, and from {@code current} on the
 * display thread, for any owner -- including a mob firing a costed trigger and a player between
 * bootstrap and register. An implementation forwarding to the stat store must therefore read a stat
 * that returns a neutral value rather than throwing. A resolver that threw would throw from inside a
 * cast.
 *
 * <p>Asked per RESOURCE as well as per owner, for the reason {@link MaxResolver} gives: the pool
 * promises "mana, and whatever else content asks for", so an implementation ignoring the id would
 * speed up every future resource at once.
 *
 * <h2>Never called from inside {@code compute}</h2>
 *
 * {@code ResourcePool.regenerated} is handed the rate rather than asking for it, exactly as it is
 * handed the ceiling. {@code tryConsume} resolves both ONCE before its {@code compute} lambda --
 * arbitrary caller code must not run inside a mapping function on the map it is computing, and two
 * live reads straddling a gear change could disagree. The arity is what
 * {@code tryConsumeAsksEACHResolverEXACTLYONCE...} counts.
 */
@FunctionalInterface
public interface RegenResolver {

    /**
     * The per-tick regeneration rate for {@code owner}'s {@code resourceId} right now. Never throws;
     * returns the base for an owner or a resource it does not know about.
     */
    double regenFor(UUID owner, String resourceId);

    /** The pre-lift behaviour: one rate for everyone. Used by tests and as a base fallback. */
    static RegenResolver fixed(double regenPerTick) {
        return (owner, resourceId) -> regenPerTick;
    }
}
