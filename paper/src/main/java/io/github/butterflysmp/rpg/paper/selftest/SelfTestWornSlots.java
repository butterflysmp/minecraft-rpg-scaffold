package io.github.butterflysmp.rpg.paper.selftest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * W1 (the seat, 2026-11-25): HIS OWN FOUR ARMOUR SLOTS AND HIS OFF-HAND, captured at run start and restored
 * EXPLICITLY, then READ BACK, wherever the snapshot restores his inventory.
 *
 * <p>READ (javap, the pinned server jar): {@code CraftInventoryPlayer} inherits {@code getContents} and
 * {@code setContents} from {@code CraftInventory}. NMS {@code Inventory.getContents} is the 36 storage slots then the
 * equipment, in index order: feet 36, legs 37, chest 38, head 39, off-hand 40, body 41, saddle 42 (43 in all); and
 * {@code setContents} writes every one of {@code 0..getSize()}. So the whole-array snapshot already carried them. This
 * class does not depend on that: it captures and writes the five slots by name, and checks they arrived.
 *
 * <p>Generic over the item, so the rule is a unit row ({@code SelfTestWornSlotsTest}); the paper side gives it
 * {@code ItemStack}, {@code ItemStack::clone} and {@code Objects.equals} (ItemStack's own equality: type, amount and
 * meta). CC'S CALL: the snapshot had no item equality check to reuse, so it is ItemStack's own.
 */
final class SelfTestWornSlots<T> {

    enum Slot { HEAD, CHEST, LEGS, FEET, OFF_HAND }

    /** His five slots, as the paper side reads and writes them. A null read is an empty slot. */
    interface Port<T> {
        T get(Slot slot);

        void set(Slot slot, T item);
    }

    /** Captured COPIES; an empty slot is held as null. */
    private final Map<Slot, T> captured;

    private SelfTestWornSlots(Map<Slot, T> captured) {
        this.captured = captured;
    }

    static <T> SelfTestWornSlots<T> capture(Port<T> port, UnaryOperator<T> copy, Predicate<T> empty) {
        Map<Slot, T> out = new EnumMap<>(Slot.class);
        for (Slot s : Slot.values()) {
            T item = port.get(s);
            out.put(s, empty.test(item) ? null : copy.apply(item));
        }
        return new SelfTestWornSlots<>(out);
    }

    /** The captured item in a slot (a copy; null when the slot was empty). */
    T captured(Slot slot) {
        return captured.get(slot);
    }

    /** {@code armour=<n of 4 non-empty> offhand=<0|1>}, the CAPTURED and RESTORED lines' fields. */
    String fields() {
        int armour = 0;
        for (Slot s : new Slot[] {Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET}) if (captured.get(s) != null) armour++;
        return "armour=" + armour + " offhand=" + (captured.get(Slot.OFF_HAND) != null ? 1 : 0);
    }

    /**
     * Write every captured slot back (a fresh copy, or EMPTY where the capture was empty, so a row's piece is removed),
     * then READ each back and return the slots that differ from the capture, in slot order. Empty reads as empty
     * whatever the port returns for it.
     */
    List<Slot> restore(Port<T> port, UnaryOperator<T> copy, Predicate<T> empty, BiPredicate<T, T> same) {
        for (Slot s : Slot.values()) {
            T want = captured.get(s);
            port.set(s, want == null ? null : copy.apply(want));
        }
        List<Slot> differ = new ArrayList<>();
        for (Slot s : Slot.values()) {
            T want = captured.get(s);
            T got = port.get(s);
            boolean ok = want == null ? empty.test(got) : (!empty.test(got) && same.test(want, got));
            if (!ok) differ.add(s);
        }
        return differ;
    }

    /** {@code NOT-RESTORED=<slots>} for the line, or empty when every slot read back as captured. */
    static String notRestored(List<Slot> differ) {
        if (differ.isEmpty()) return "";
        List<String> names = new ArrayList<>();
        for (Slot s : differ) names.add(s.name());
        return "NOT-RESTORED=" + String.join(",", names);
    }
}
