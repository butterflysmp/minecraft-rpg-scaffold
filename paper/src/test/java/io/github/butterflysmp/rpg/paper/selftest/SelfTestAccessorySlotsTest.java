package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** H15: his four accessory slots, captured exactly and put back whole, then read back. */
class SelfTestAccessorySlotsTest {

    /** An in-memory store: a record is its four slots' text. */
    private static final class Store implements SelfTestAccessorySlots.Port<List<String>> {
        List<String> record;
        boolean usable = true;
        boolean refuseWrites;
        int writes;

        Store(String... slots) { record = new ArrayList<>(Arrays.asList(slots)); }

        @Override public Optional<List<String>> read() { return usable ? Optional.of(new ArrayList<>(record)) : Optional.empty(); }
        @Override public boolean write(List<String> r) {
            if (!usable || refuseWrites) return false;
            writes++;
            record = new ArrayList<>(r);
            return true;
        }
        @Override public List<String> slots(List<String> r) { return r; }    }

    private static String name(String stored) {
        return stored.startsWith("bad") ? "unreadable" : stored.replace("b64:", "");
    }

    @Test
    void aRunThatChangedHisSlotsPutsThemBackExactly_unreadableIncluded() {
        Store store = new Store("b64:sages_scroll", null, "bad-bytes", "b64:ward_charm");
        SelfTestAccessorySlots<List<String>> snap = SelfTestAccessorySlots.capture(store);
        assertEquals("[sages_scroll,-,unreadable,ward_charm]", snap.fields(store, SelfTestAccessorySlotsTest::name));

        // The run stages the tome and clears the rest.
        store.record = new ArrayList<>(Arrays.asList("b64:healing_tome", null, null, null));
        assertEquals(Optional.empty(), snap.restore(store));
        assertEquals(Arrays.asList("b64:sages_scroll", null, "bad-bytes", "b64:ward_charm"), store.record,
                "byte for byte, the unreadable slot's text included");
        assertEquals(1, store.writes, "one whole-record write");
        // Mutation: skip port.write in restore -> reddens (the slots stay the tome's).
    }

    @Test
    void notCaptured_writesNothing_andSaysSo() {
        Store store = new Store("b64:sages_scroll", null, null, null);
        store.usable = false;
        SelfTestAccessorySlots<List<String>> snap = SelfTestAccessorySlots.capture(store);
        assertFalse(snap.wasCaptured());
        assertEquals("not-captured", snap.fields(store, SelfTestAccessorySlotsTest::name));
        store.usable = true;
        assertEquals(Optional.of("accessories(not-captured)"), snap.restore(store));
        assertEquals(0, store.writes, "a store never captured is never written: it would erase what was there");
    }

    @Test
    void aRefusedWriteOrAMismatchedReadBackIsNamed() {
        Store store = new Store("b64:sages_scroll", null, null, null);
        SelfTestAccessorySlots<List<String>> snap = SelfTestAccessorySlots.capture(store);
        store.refuseWrites = true;
        assertEquals(Optional.of("accessories(store-refused)"), snap.restore(store));

        // A store that takes the write but reads back something else (the read-back is a real read, not the capture).
        SelfTestAccessorySlots.Port<List<String>> liar = new SelfTestAccessorySlots.Port<>() {
            @Override public Optional<List<String>> read() { return Optional.of(Arrays.asList("b64:sages_scroll", "x", null, null)); }
            @Override public boolean write(List<String> r) { return true; }
            @Override public List<String> slots(List<String> r) { return r; }
        };
        SelfTestAccessorySlots<List<String>> snap2 = SelfTestAccessorySlots.capture(liar);
        assertTrue(snap2.wasCaptured());
        SelfTestAccessorySlots.Port<List<String>> after = new SelfTestAccessorySlots.Port<>() {
            @Override public Optional<List<String>> read() { return Optional.of(Arrays.asList("b64:sages_scroll", null, null, null)); }
            @Override public boolean write(List<String> r) { return true; }
            @Override public List<String> slots(List<String> r) { return r; }
        };
        assertEquals(Optional.of("accessories(slots 1)"), snap2.restore(after));
    }

    @Test
    void theSummary() {
        assertEquals("[-,-,-,-]", SelfTestAccessorySlots.summary(SelfTestAccessorySlots.emptySlots(4), s -> s));
    }
}
