package io.github.butterflysmp.rpg.core.vault;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which cells of a page are NOT in the file.
 *
 * <h2>*** ONE COMPARISON, USED BY THREE CALLERS, BECAUSE TWO COPIES OF IT WOULD DRIFT ***</h2>
 *
 * The same question is asked in three places and the answers must agree exactly, or an item is
 * either duplicated or destroyed:
 *
 * <pre>
 *   the SCREEN, closing while degraded   which cells must be handed back
 *   the SERVICE, on a failed save        which cells reached nobody, so the caller can drop them
 *   the DEV COMMAND, on a failed save    the same, for an item it has already taken from a hand
 * </pre>
 *
 * <p>Pure text in, slot indices out. <b>No {@code ItemStack}, no encoding, no Bukkit</b> -- which is
 * what lets the service use it on an I/O thread and the screen use it on the main one.
 *
 * <h2>COMPARED BY CONTENT, NOT BY PRESENCE, AND THE BIAS IS DELIBERATE</h2>
 *
 * A file can hold item X at cell 5 while the screen holds item Y there -- an unpersisted SWAP.
 * Presence alone would call that cell persisted and <b>destroy Y</b>.
 *
 * <p>So anything that does not match exactly counts as unpersisted. <b>Wrong in this direction
 * costs a duplicate; wrong in the other costs the item.</b> That asymmetry is the whole reason the
 * comparison is written down rather than inlined three times.
 */
public final class VaultPageDiff {

    private VaultPageDiff() {}

    /**
     * The slots of {@code live} whose contents are not exactly what {@code onDisk} holds.
     *
     * @param live   slot to encoded item, as the page stands now. Absent means empty.
     * @param onDisk slot to encoded item, as the last CONFIRMED write left it. <b>An empty map means
     *               "nothing is known to be on disk"</b>, which is the safe reading: every occupied
     *               cell is then unpersisted.
     * @return the unpersisted slots. <b>Empty cells are never included</b> -- a cell holding nothing
     *         cannot be handed back, dropped, or lost.
     */
    public static Set<Integer> unpersisted(Map<Integer, String> live, Map<Integer, String> onDisk) {
        Set<Integer> owed = new LinkedHashSet<>();

        for (Map.Entry<Integer, String> cell : live.entrySet()) {
            String item = cell.getValue();
            if (item == null || item.isBlank()) continue;

            // THE CELL IS OWED UNLESS THE FILE HOLDS THE SAME TEXT. `equals` on the encoded string
            // rather than on the item, because this class must not know what an item is -- and
            // because the encoded text is exactly what a write would have stored.
            if (!item.equals(onDisk.get(cell.getKey()))) owed.add(cell.getKey());
        }
        return Set.copyOf(owed);
    }
}
