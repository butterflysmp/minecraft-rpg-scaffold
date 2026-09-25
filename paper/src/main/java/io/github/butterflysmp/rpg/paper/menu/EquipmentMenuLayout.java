package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.accessory.AccessorySlots;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Where everything sits in the Equipment screen: 54 slots, the house chrome (back 48, close 49), an
 * armour column and an accessory column.
 *
 * <pre>
 *   col:   0  1  2  3  4  5  6  7  8
 *   row 0  .  .  A  .  .  .  C  .  .      A = "Armour" label (2)     C = "Accessories" label (6)
 *   row 1  .  .  H  .  .  .  0  .  .      H/B/L/F = head, chest, legs, feet (11, 20, 29, 38)
 *   row 2  .  .  B  .  .  .  1  .  .      0 = the CLASS accessory slot (15)
 *   row 3  .  .  L  .  .  .  2  .  .      1/2/3 = the universal accessory slots (24, 33, 42)
 *   row 4  .  .  F  .  .  .  3  .  .
 *   row 5  .  .  .  .  .  .  .  .  .      back 48, close 49
 * </pre>
 *
 * <p><b>None of these slots is an input slot</b>, and that is load-bearing: it is what makes the
 * number key, F, drag and double-click refuse by construction ({@code MenuRouting} only performs
 * those gestures on input slots). Every move the screen allows it performs itself.
 */
final class EquipmentMenuLayout {

    private EquipmentMenuLayout() {}

    static final int SIZE = 54;

    static final int ARMOR_LABEL_SLOT = 2;
    static final int ACCESSORY_LABEL_SLOT = 6;

    static final int HEAD_SLOT = 11;
    static final int CHEST_SLOT = 20;
    static final int LEGS_SLOT = 29;
    static final int FEET_SLOT = 38;

    /** Accessory slot index i sits at ACCESSORY_SLOTS.get(i); index 0 is the class slot. */
    static final List<Integer> ACCESSORY_SLOTS = List.of(15, 24, 33, 42);

    static final int BACK_SLOT = 48;
    static final int CLOSE_SLOT = 49;

    static final Set<Integer> ARMOR_SLOTS = Set.of(HEAD_SLOT, CHEST_SLOT, LEGS_SLOT, FEET_SLOT);

    static final Set<Integer> PAINTED_SLOTS;
    static final Set<Integer> FILLER_SLOTS;

    static {
        Set<Integer> painted = new HashSet<>(ARMOR_SLOTS);
        painted.addAll(ACCESSORY_SLOTS);
        painted.add(ARMOR_LABEL_SLOT);
        painted.add(ACCESSORY_LABEL_SLOT);
        painted.add(BACK_SLOT);
        painted.add(CLOSE_SLOT);
        PAINTED_SLOTS = Set.copyOf(painted);
        Set<Integer> filler = new HashSet<>();
        for (int slot = 0; slot < SIZE; slot++) {
            if (!painted.contains(slot)) filler.add(slot);
        }
        FILLER_SLOTS = Set.copyOf(filler);
    }

    /** The armour slot a screen slot shows, if it is one. */
    static Optional<ArmorSlot> armorSlotAt(int slot) {
        return switch (slot) {
            case HEAD_SLOT -> Optional.of(ArmorSlot.HEAD);
            case CHEST_SLOT -> Optional.of(ArmorSlot.CHEST);
            case LEGS_SLOT -> Optional.of(ArmorSlot.LEGS);
            case FEET_SLOT -> Optional.of(ArmorSlot.FEET);
            default -> Optional.empty();
        };
    }

    /** The screen slot that shows an armour slot. */
    static int slotOf(ArmorSlot armor) {
        return switch (armor) {
            case HEAD -> HEAD_SLOT;
            case CHEST -> CHEST_SLOT;
            case LEGS -> LEGS_SLOT;
            case FEET -> FEET_SLOT;
        };
    }

    /** The accessory index (0 = class) a screen slot shows, if it is one. */
    static OptionalInt accessoryIndexAt(int slot) {
        int index = ACCESSORY_SLOTS.indexOf(slot);
        return index < 0 ? OptionalInt.empty() : OptionalInt.of(index);
    }

    static {
        if (ACCESSORY_SLOTS.size() != AccessorySlots.COUNT) {
            throw new IllegalStateException("the screen shows " + ACCESSORY_SLOTS.size()
                    + " accessory slots but a player has " + AccessorySlots.COUNT);
        }
    }
}
