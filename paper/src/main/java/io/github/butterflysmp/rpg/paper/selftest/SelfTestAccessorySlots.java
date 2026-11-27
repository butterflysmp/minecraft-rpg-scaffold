package io.github.butterflysmp.rpg.paper.selftest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * H15 (the seat, 2026-11-26): HIS FOUR ACCESSORY SLOTS, captured at run start EXACTLY (the stored text of each slot,
 * an unreadable item's included, byte for byte) and put back at the run's end and on every abort, then READ BACK -- W1's
 * mechanism ({@link SelfTestWornSlots}) for the accessory store. A run that stages the Healing Tome changes his class and
 * his slots; this is what makes that cost him nothing.
 *
 * <p>Generic over the record so the rule is a unit row ({@code SelfTestAccessorySlotsTest}); the paper side gives it the
 * service's {@code PlayerAccessories} and its {@code replace}. Not captured (his store not loaded, or unusable) is its own
 * state: nothing is written back, and the RESTORED line says {@code NOT-RESTORED=accessories(not-captured)}.
 */
final class SelfTestAccessorySlots<R> {

    /** His store, as the snapshot reads and writes it. */
    interface Port<R> {
        /** The current record, empty when the store is not loaded or unusable. */
        Optional<R> read();

        /** Write {@code record} back whole; false when the store refused it. */
        boolean write(R record);

        /** The four slots' stored text, index = slot, null for an empty slot. */
        List<String> slots(R record);
    }

    private final R captured;   // null: not captured

    private SelfTestAccessorySlots(R captured) {
        this.captured = captured;
    }

    static <R> SelfTestAccessorySlots<R> capture(Port<R> port) {
        return new SelfTestAccessorySlots<>(port.read().orElse(null));
    }

    boolean wasCaptured() {
        return captured != null;
    }

    /**
     * {@code [healing_tome,-,unreadable,-]}: each slot's accessory id as {@code nameOf} reads its stored text, {@code -}
     * for empty; or {@code not-captured}.
     */
    String fields(Port<R> port, Function<String, String> nameOf) {
        if (captured == null) return "not-captured";
        return summary(port.slots(captured), nameOf);
    }

    static String summary(List<String> slots, Function<String, String> nameOf) {
        List<String> names = new ArrayList<>();
        for (String s : slots) names.add(s == null ? "-" : nameOf.apply(s));
        return "[" + String.join(",", names) + "]";
    }

    /**
     * Write the captured record back whole, then READ it back: empty when every slot's stored text equals the capture,
     * else the reason, for the RESTORED line's {@code NOT-RESTORED=} field.
     */
    Optional<String> restore(Port<R> port) {
        if (captured == null) return Optional.of("accessories(not-captured)");
        if (!port.write(captured)) return Optional.of("accessories(store-refused)");
        Optional<R> now = port.read();
        if (now.isEmpty()) return Optional.of("accessories(unreadable-after)");
        List<String> want = port.slots(captured);
        List<String> got = port.slots(now.get());
        List<String> differ = new ArrayList<>();
        for (int i = 0; i < want.size(); i++) {
            if (!Objects.equals(want.get(i), i < got.size() ? got.get(i) : null)) differ.add(String.valueOf(i));
        }
        return differ.isEmpty() ? Optional.empty() : Optional.of("accessories(slots " + String.join(",", differ) + ")");
    }

    /** Four nulls: an empty store, for the paper side's slot read. */
    static List<String> emptySlots(int count) {
        return new ArrayList<>(Collections.nCopies(count, null));
    }
}
