package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.build.LoadoutSlot;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Where everything sits on the Build screen and on its picker (PLAN-build-system.md section 3.3, ruling
 * 16). Rows are 1-based, as every layout instruction on this project is.
 *
 * <pre>
 *   col:   0  1  2  3  4  5  6  7  8
 *   row 1  .  .  .  C  .  E  .  .  .      C = class (3)       E = element (5)
 *   row 2  .  .  Q  .  L  .  R  .  .      Q = the Ultimate (11), L = Active 1 (13), R = Active 2 (15)
 *   row 3  .  .  .  a  .  a  .  .  .      a = the two aspect cells (21, 23), "Coming in a later update"
 *   row 4  .  f  .  f  .  f  .  f  .      f = the four fragment cells (28, 30, 32, 34), the same
 *   row 5  .  .  .  .  .  .  .  .  .
 *   row 6  .  .  .  .  .  .  .  .  .      back 48, close 49
 * </pre>
 *
 * <p><b>The loadout row reads in the order the stone's inputs are named: Q, Left, Right</b> -- the
 * Ultimate, then Active 1 (left click), then Active 2 (right click).
 *
 * <p><b>NONE of these slots is an input slot</b>, on either screen. Every icon is a rendered item built
 * here, never a real item, and a click is a button press. That is what makes shift-click, the number
 * key, F, drag and double-click refuse by construction ({@code MenuRouting} only moves items for input
 * slots) -- the Nexus slot picker's anti-dupe rule.
 */
final class BuildMenuLayout {

    private BuildMenuLayout() {}

    static final int SIZE = 54;

    static final int CLASS_SLOT = 3;
    static final int ELEMENT_SLOT = 5;

    static final int ULTIMATE_SLOT = 11;
    static final int ACTIVE_1_SLOT = 13;
    static final int ACTIVE_2_SLOT = 15;

    /** Aspect cell i sits at ASPECT_SLOTS.get(i). Not built until slice 5. */
    static final List<Integer> ASPECT_SLOTS = List.of(21, 23);

    /** Fragment cell i sits at FRAGMENT_SLOTS.get(i). Not built until slice 4. */
    static final List<Integer> FRAGMENT_SLOTS = List.of(28, 30, 32, 34);

    static final int BACK_SLOT = 48;
    static final int CLOSE_SLOT = 49;

    static final Set<Integer> PAINTED_SLOTS;
    static final Set<Integer> FILLER_SLOTS;

    static {
        Set<Integer> painted = new HashSet<>(List.of(CLASS_SLOT, ELEMENT_SLOT,
                ULTIMATE_SLOT, ACTIVE_1_SLOT, ACTIVE_2_SLOT, BACK_SLOT, CLOSE_SLOT));
        painted.addAll(ASPECT_SLOTS);
        painted.addAll(FRAGMENT_SLOTS);
        PAINTED_SLOTS = Set.copyOf(painted);
        Set<Integer> filler = new HashSet<>();
        for (int slot = 0; slot < SIZE; slot++) {
            if (!painted.contains(slot)) filler.add(slot);
        }
        FILLER_SLOTS = Set.copyOf(filler);
    }

    /** The loadout slot a screen slot shows, if it is one. */
    static Optional<LoadoutSlot> loadoutSlotAt(int slot) {
        return switch (slot) {
            case ULTIMATE_SLOT -> Optional.of(LoadoutSlot.ULTIMATE);
            case ACTIVE_1_SLOT -> Optional.of(LoadoutSlot.ACTIVE_1);
            case ACTIVE_2_SLOT -> Optional.of(LoadoutSlot.ACTIVE_2);
            default -> Optional.empty();
        };
    }

    /** The screen slot that shows a loadout slot. */
    static int slotOf(LoadoutSlot slot) {
        return switch (slot) {
            case ULTIMATE -> ULTIMATE_SLOT;
            case ACTIVE_1 -> ACTIVE_1_SLOT;
            case ACTIVE_2 -> ACTIVE_2_SLOT;
        };
    }

    /** The input a loadout slot is cast with, as the screen labels it. */
    static String inputLabel(LoadoutSlot slot) {
        return switch (slot) {
            case ULTIMATE -> "Q";
            case ACTIVE_1 -> "Left";
            case ACTIVE_2 -> "Right";
        };
    }

    // ------------------------------------------------------------------ the picker

    /**
     * The picker's option cells: rows 2-5, columns 1-7 -- 28 options, left to right then down. The house
     * back (48) and close (49) as on every screen.
     */
    static final List<Integer> OPTION_SLOTS = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43);

    /** Row 1, centre: what is being chosen. */
    static final int PICKER_TITLE_SLOT = 4;

    static final Set<Integer> PICKER_FILLER_SLOTS;

    static {
        Set<Integer> filler = new HashSet<>();
        for (int slot = 0; slot < SIZE; slot++) filler.add(slot);
        filler.removeAll(OPTION_SLOTS);
        filler.remove(PICKER_TITLE_SLOT);
        filler.remove(BACK_SLOT);
        filler.remove(CLOSE_SLOT);
        PICKER_FILLER_SLOTS = Set.copyOf(filler);
    }

    /** Which option (0-based) a picker slot holds, if it is an option cell at all. */
    static OptionalInt optionAt(int slot) {
        int index = OPTION_SLOTS.indexOf(slot);
        return index < 0 ? OptionalInt.empty() : OptionalInt.of(index);
    }
}
