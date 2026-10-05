package io.github.butterflysmp.rpg.paper.adapter;

/**
 * Where the shared DoT store reports each tick ({@link DotTick}; the seat's S3 ruling). The store calls
 * it on every burn; production's implementation ({@code AdapterContext}) logs the {@code DOTTICK} line
 * only while {@code /rpg mobtrace} is on, so the switch stays on {@code MobNameplateManager}, beside
 * {@code MOBSEED} and {@code MOBHIT}, and this is a read of it.
 *
 * <p>Called on the victim's own thread, from inside the tick body, and must not throw.
 */
@FunctionalInterface
public interface DotTrace {

    void tick(DotTick tick);

    /** Reports nothing: the store's default, for a test that is not about the trace. */
    DotTrace NONE = tick -> { };
}
