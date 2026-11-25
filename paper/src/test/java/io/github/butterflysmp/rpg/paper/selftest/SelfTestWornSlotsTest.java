package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.paper.selftest.SelfTestWornSlots.Slot;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * W1 (the seat, 2026-11-25): HIS OWN WORN ARMOUR AND OFF-HAND ARE CAPTURED AND RESTORED EXPLICITLY, and read back.
 *
 * <p>A pure class over a generic item, so the rule is a unit row (no ItemStack can be built without a server here).
 * The fake item is a record with a mutable-looking name; COPY makes a distinct instance, so a test can tell a restored
 * copy from the original object.
 */
class SelfTestWornSlotsTest {

    /** A stand-in item: equal by name, as ItemStack is equal by type, amount and meta. */
    record Item(String name) {}

    /** His worn slots, as the paper port reads and writes them. Null is an empty slot. */
    static final class Body implements SelfTestWornSlots.Port<Item> {
        final Map<Slot, Item> slots = new EnumMap<>(Slot.class);
        Slot refusing;   // a slot whose write silently does nothing, for the read-back row

        @Override public Item get(Slot s) { return slots.get(s); }
        @Override public void set(Slot s, Item item) { if (s != refusing) slots.put(s, item); }
    }

    private static Item copy(Item i) { return i == null ? null : new Item(i.name()); }
    private static boolean empty(Item i) { return i == null || i.name().isEmpty(); }

    private static SelfTestWornSlots<Item> capture(Body b) {
        return SelfTestWornSlots.capture(b, SelfTestWornSlotsTest::copy, SelfTestWornSlotsTest::empty);
    }

    private static List<Slot> restore(SelfTestWornSlots<Item> w, Body b) {
        return w.restore(b, SelfTestWornSlotsTest::copy, SelfTestWornSlotsTest::empty, Objects::equals);
    }

    private static Body wearing() {
        Body b = new Body();
        b.slots.put(Slot.HEAD, new Item("his_helmet"));
        b.slots.put(Slot.CHEST, new Item("his_chestplate"));
        b.slots.put(Slot.LEGS, new Item("his_leggings"));
        b.slots.put(Slot.FEET, new Item("his_boots"));
        b.slots.put(Slot.OFF_HAND, new Item("his_totem"));
        return b;
    }

    @Test
    void fourDistinctPiecesAndAnOffHandComeBackAfterRowsReplaceAllFive() {
        Body b = wearing();
        SelfTestWornSlots<Item> w = capture(b);
        for (Slot s : Slot.values()) b.slots.put(s, new Item("row_piece_" + s));   // the rows replace all five
        assertEquals(List.of(), restore(w, b), "nothing differs after the read-back");
        assertEquals(wearing().slots, b.slots, "every one of the five is his again");
        assertNotSame(w.captured(Slot.HEAD), b.slots.get(Slot.HEAD), "a COPY was written, so the capture stays clean");
        // Mutation: skip the armour writes -> the four come back as row pieces -> reddens.
        // Mutation: skip the off-hand write -> row_piece_OFF_HAND stays -> reddens.
    }

    @Test
    void aCapturedEmptySlotComesBackEmptyAndTheRowsPieceIsRemoved() {
        Body b = wearing();
        b.slots.remove(Slot.LEGS);
        b.slots.remove(Slot.OFF_HAND);
        SelfTestWornSlots<Item> w = capture(b);
        b.slots.put(Slot.LEGS, new Item("iron_leggings"));      // a test piece a row left
        b.slots.put(Slot.OFF_HAND, new Item("arrow"));
        assertEquals(List.of(), restore(w, b));
        assertNull(b.slots.get(Slot.LEGS), "the row's piece is removed");
        assertNull(b.slots.get(Slot.OFF_HAND));
        // Mutation: write nothing for a captured-empty slot -> iron_leggings stays -> reddens.
    }

    @Test
    void theCountsOnTheLineArePinned() {
        assertEquals("armour=4 offhand=1", capture(wearing()).fields());
        Body bare = new Body();
        assertEquals("armour=0 offhand=0", capture(bare).fields());
        Body partial = wearing();
        partial.slots.remove(Slot.CHEST);
        partial.slots.put(Slot.FEET, new Item(""));               // an empty stack counts as empty
        partial.slots.remove(Slot.OFF_HAND);
        assertEquals("armour=2 offhand=0", capture(partial).fields());
    }

    @Test
    void aSlotThatDoesNotReadBackAsCapturedIsNamed() {
        Body b = wearing();
        SelfTestWornSlots<Item> w = capture(b);
        for (Slot s : Slot.values()) b.slots.put(s, new Item("row_piece_" + s));
        b.refusing = Slot.CHEST;                                  // the write silently fails
        assertEquals(List.of(Slot.CHEST), restore(w, b), "the read-back names the slot");
        assertEquals("NOT-RESTORED=CHEST", SelfTestWornSlots.notRestored(List.of(Slot.CHEST)));
        assertEquals("", SelfTestWornSlots.notRestored(List.of()));
        // Mutation: skip the read-back (return an empty list) -> CHEST unnamed -> reddens.
    }

    @Test
    void theCaptureIsACopyNotTheLiveItem() {
        Body b = wearing();
        SelfTestWornSlots<Item> w = capture(b);
        assertNotSame(b.slots.get(Slot.HEAD), w.captured(Slot.HEAD));
        assertEquals(b.slots.get(Slot.HEAD), w.captured(Slot.HEAD));
    }
}
