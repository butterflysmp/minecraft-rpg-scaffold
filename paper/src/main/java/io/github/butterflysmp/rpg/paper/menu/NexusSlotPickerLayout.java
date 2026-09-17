package io.github.butterflysmp.rpg.paper.menu;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Where everything sits on the Nexus slot picker: the player's own inventory, mirrored.
 *
 * <p>Six rows. Thirty-six choosers in rows 2-5 laid out exactly as the player's inventory is, and
 * chrome in the bottom row.
 *
 * <pre>
 *   menu 9..35   ->  inventory 9..35    IDENTITY -- 27 of 36 cells map to themselves
 *   menu 36..44  ->  inventory 0..8     the HOTBAR, on the bottom row where it belongs
 * </pre>
 *
 * <b>Twenty-seven of the thirty-six need no translation at all</b>, and the nine that do are the
 * hotbar, which every player already reads as the bottom row. Pointing, not arithmetic.
 *
 * <h2>*** THIS IS A NEW CLASS AND THE MAPPING WAS MOVED, NOT COPIED ***</h2>
 *
 * It lived in {@code SettingsMenuLayout} while settings WAS the picker. Settings is now a screen of
 * buttons, and <b>that file's organising argument has been rewritten twice already</b> -- once when
 * the picker went from nine hotbar cells to thirty-six inventory cells, and again when the screen
 * stopped being a picker at all. Leaving the choosers there would have left a third layer of
 * argument in a file that is now about something else.
 *
 * <h2>*** DO NOT COPY THE PREDECESSOR'S CHROME. CLOSE MOVED BETWEEN TWO SCREENS ONE CLICK APART ***</h2>
 *
 * Its picker used <b>back 49 / close 53</b> while its own settings screen used <b>48/49</b> -- so
 * the one button a player has learned sat in a different cell on two screens they reach in
 * sequence. <b>Ours is 48/49 on both</b>, like every other screen in this plugin.
 *
 * <p><b>The LABELS differ because the destinations do</b>: <i>"Back to Settings"</i> here,
 * <i>"Back to the Nexus"</i> on settings. Same slot, same shape, different word — <b>that is the
 * convention working, not a collision.</b>
 */
final class NexusSlotPickerLayout {

    private NexusSlotPickerLayout() {}

    /** Six rows, like every other screen here. */
    static final int SIZE = 54;

    /** The first slot of the bottom row. */
    static final int BOTTOM_ROW_START = 45;

    /**
     * Back to SETTINGS, not to the hub. <b>48, the plugin's one answer for a back button.</b>
     *
     * <p>Deliberately the same cell {@code SettingsMenuLayout.BACK_SLOT} uses, one screen up. The
     * predecessor put its picker's back at 49 -- <b>which is our CLOSE</b> -- so a player who had
     * learned one screen closed the other.
     */
    static final int BACK_SLOT = 48;

    /** Close. <b>49</b>, the one button that never moves between our screens. */
    static final int CLOSE_SLOT = 49;

    /** The first chooser: menu slot 9, the top-left of the mirrored inventory. */
    static final int FIRST_SLOT_CHOOSER = 9;

    /** Thirty-six: the main inventory, hotbar and storage. No armour, no offhand. */
    static final int MAIN_INVENTORY_SIZE = 36;

    /** Nine, because the hotbar is nine -- here, the size of the bottom ROW of the picker. */
    static final int HOTBAR_SIZE = 9;

    /**
     * The thirty-six chooser slots, in MENU order.
     *
     * <p><b>A {@code List}, not a {@code Set}</b>: the order is the meaning, and {@code Set.copyOf}
     * discards iteration order. <b>The list index is NOT the inventory slot</b> -- the list runs in
     * menu order and {@link #chooserFor} is what converts.
     */
    static final List<Integer> SLOT_CHOOSERS = buildChoosers();

    /**
     * Everything that is plain filler -- all of it except the choosers and the two buttons.
     *
     * <p>Built by SET SUBTRACTION: <b>a filler pane painted over a live button is invisible until
     * someone clicks it.</b> Adding a control later is one more removal line, not a remembered skip.
     */
    static final Set<Integer> FILLER_SLOTS = buildFiller();

    /**
     * Which INVENTORY slot this menu slot chooses, if it is a chooser at all.
     *
     * <pre>
     *   menu  9..35   ->  inventory  9..35     IDENTITY
     *   menu 36..44   ->  inventory  0..8      the hotbar, on the bottom row
     * </pre>
     *
     * <b>The identity half is not a coincidence and it is not free either</b> -- it is what you get
     * when a 54-slot chest's rows 2-5 are laid over a player inventory whose storage starts at 9.
     * <b>A mapping that is ALMOST the identity is the kind that gets a plausible off-by-nine and
     * passes every test staged in the identity half</b>, which is why both seams are pinned.
     *
     * <p>{@code OptionalInt} rather than {@code -1}: a click handler that forgets to test a
     * sentinel silently treats "not a chooser" as slot -1.
     */
    static OptionalInt chooserFor(int menuSlot) {
        if (menuSlot < FIRST_SLOT_CHOOSER || menuSlot >= FIRST_SLOT_CHOOSER + MAIN_INVENTORY_SIZE) {
            return OptionalInt.empty();
        }
        int storageCells = MAIN_INVENTORY_SIZE - HOTBAR_SIZE;          // 27
        int lastStorageMenuSlot = FIRST_SLOT_CHOOSER + storageCells;   // 36, the first hotbar cell
        return menuSlot < lastStorageMenuSlot
                ? OptionalInt.of(menuSlot)                             // 9..35 -> 9..35
                : OptionalInt.of(menuSlot - lastStorageMenuSlot);      // 36..44 -> 0..8
    }

    /**
     * What to call an inventory slot on screen.
     *
     * <p><b>The hotbar is numbered the way the player's keyboard numbers it</b> -- 1 to 9 -- and
     * storage is numbered separately rather than continuing to 36, because <i>"slot 28"</i> means
     * nothing to anyone. <b>A player looking for their star reads a row and a position, not an
     * index</b>, and that is what makes this screen read as an inventory rather than a grid of
     * numbers.
     *
     * <p>Moved here from {@code SettingsMenu} with the choosers, unchanged.
     */
    static String slotName(int inventorySlot) {
        if (inventorySlot < HOTBAR_SIZE) {
            return "Hotbar " + (inventorySlot + 1);
        }
        int storageIndex = inventorySlot - HOTBAR_SIZE;
        return "Row " + (storageIndex / 9 + 1) + ", slot " + (storageIndex % 9 + 1);
    }

    private static List<Integer> buildChoosers() {
        List<Integer> slots = new java.util.ArrayList<>();
        for (int i = 0; i < MAIN_INVENTORY_SIZE; i++) slots.add(FIRST_SLOT_CHOOSER + i);
        return List.copyOf(slots);
    }

    private static Set<Integer> buildFiller() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int slot = 0; slot < SIZE; slot++) slots.add(slot);
        slots.removeAll(SLOT_CHOOSERS);
        slots.remove(BACK_SLOT);
        slots.remove(CLOSE_SLOT);
        return Set.copyOf(slots);
    }
}
