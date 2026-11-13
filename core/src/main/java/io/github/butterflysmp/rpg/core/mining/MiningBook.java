package io.github.butterflysmp.rpg.core.mining;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;

/**
 * Every player's patch at every slot, and the regrowth clock. The seat's ruling 4, 2026-11-13:
 * <b>in memory, per player UUID, KEPT across logout, LOST on restart.</b> Nothing here removes a
 * player's state on quit.
 *
 * <p>A patch is rolled the first time it is looked at (shown or clicked) while not regrowing. A
 * broken patch is forgotten and its regrowth started; the first look after the deadline rolls a
 * fresh layout. Not thread-safe: main thread only.
 */
public final class MiningBook {

    private final RegrowthClock clock;
    private final RandomGenerator rng;
    private final LongSupplier now;
    private final Map<UUID, Map<String, Patch>> patches = new HashMap<>();

    /**
     * @param clock the regrowth deadlines
     * @param rng   rolls layouts and payouts
     * @param now   the current server tick, for a fresh patch's reveal tick
     */
    public MiningBook(RegrowthClock clock, RandomGenerator rng, LongSupplier now) {
        this.clock = clock;
        this.rng = rng;
        this.now = now;
    }

    /** The player's live patch at this slot, rolled now if there is none; empty while regrowing. */
    public Optional<Patch> patch(UUID player, Slot slot, VeinSize size, PayoutRule rule) {
        if (clock.isRegrowing(player, slot.id())) return Optional.empty();
        Map<String, Patch> mine = patches.computeIfAbsent(player, k -> new HashMap<>());
        Patch patch = mine.get(slot.id());
        if (patch == null) {
            patch = new Patch(SpotLayout.roll(size, rng), rule, now.getAsLong());
            mine.put(slot.id(), patch);
        }
        return Optional.of(patch);
    }

    /** Applies one click. A break forgets the patch and starts its regrowth. */
    public ClickOutcome click(UUID player, Slot slot, VeinSize size, PayoutRule rule, int regrowthTicks, Click click) {
        Optional<Patch> patch = patch(player, slot, size, rule);
        if (patch.isEmpty()) return ClickOutcome.NotLive.INSTANCE;
        ClickOutcome outcome = patch.get().click(click, rng);
        if (outcome instanceof ClickOutcome.Broke) {
            patches.get(player).remove(slot.id());
            clock.start(player, slot.id(), regrowthTicks);
        }
        return outcome;
    }

    /** The tick this player's patch at that slot regrows, if it is regrowing. */
    public OptionalLong regrowAt(UUID player, String slotId) {
        return clock.deadline(player, slotId);
    }

    /** Ends every regrowth of this player's now (the dev command {@code /rpg mining regrow}). */
    public void clearRegrowth(UUID player) {
        clock.clearPlayer(player);
    }

    /** Forgets a removed slot for every player: patches and clocks. */
    public void clearSlot(String slotId) {
        for (Iterator<Map<String, Patch>> it = patches.values().iterator(); it.hasNext(); ) {
            Map<String, Patch> mine = it.next();
            mine.remove(slotId);
            if (mine.isEmpty()) it.remove();
        }
        clock.clearSlot(slotId);
    }
}
