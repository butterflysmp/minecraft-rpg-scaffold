package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.vault.VaultCloseDisposal;
import io.github.butterflysmp.rpg.core.vault.VaultReturnPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The vault's Back leaves the saved page exactly as an explicit close did: the seat's ruling 3.
 *
 * <h2>WHAT THIS IS: A MODEL OF {@code NexusVaultMenu.onClose}, DRIVEN BY THE REAL CORE DECISIONS</h2>
 *
 * {@code NexusVaultMenu} cannot be constructed without a server. Its close is three decisions, and the two that
 * decide anything are pure and in {@code core}: {@link VaultReturnPolicy#returnedSlots} (what a give-back takes) and
 * {@link VaultCloseDisposal#of} (who claims the cells). This class replays {@code onClose}'s order around them, as
 * {@code NexusVaultMenu} writes it and {@code VaultWiringSignatureTest.theCloseFlushesBEFOREItHandsAnythingBack} pins
 * it: <b>write if healthy (degrade on a refused write), claim, give back</b>. It then compares the OLD Back (one
 * explicit close) with the NEW one ({@code returnEverything} in the click, the implicit close a tick later).
 *
 * <p><b>What it proves:</b> that with the shipped policies the two orders end in the same file, the same items handed
 * back, the same items on the ground, on every path. <b>What it does not:</b> that {@code onClose} is still written
 * in that order (the vault signature test), or that the implicit close reaches {@code onClose} at all (a jar fact,
 * GATE-nexus-back.md F1, and AUTO row NB5).
 */
class VaultBackOrderTest {

    private static final Set<Integer> STORAGE = Set.of(9, 10, 11);

    /** The vault screen as the close sees it. */
    private static final class Vault {
        final Map<Integer, String> cells = new TreeMap<>();
        Map<Integer, String> file = new TreeMap<>();
        final List<String> handedBack = new ArrayList<>();
        final List<String> ground = new ArrayList<>();
        String cursor;
        boolean degraded;
        boolean writeRefused;
        boolean claimed;

        Vault copy() {
            Vault v = new Vault();
            v.cells.putAll(cells);
            v.file = new TreeMap<>(file);
            v.cursor = cursor;
            v.degraded = degraded;
            v.writeRefused = writeRefused;
            return v;
        }

        /** {@code NexusVaultMenu.alreadyOnDisk}: a cell whose current contents match the file, compared by content. */
        Set<Integer> alreadyOnDisk() {
            Set<Integer> same = new HashSet<>();
            for (int slot : STORAGE) {
                if (cells.get(slot) != null && cells.get(slot).equals(file.get(slot))) same.add(slot);
            }
            return same;
        }

        /** {@code Menu.returnEverything}: the returned slots (clear, then give), then the cursor, always. */
        void returnEverything() {
            for (int slot : VaultReturnPolicy.returnedSlots(degraded, STORAGE, degraded ? alreadyOnDisk() : Set.of())) {
                String held = cells.remove(slot);
                if (held != null) handedBack.add(held);
            }
            if (cursor != null) {
                handedBack.add(cursor);
                cursor = null;
            }
        }

        /** {@code NexusVaultMenu.onClose}, in its order: write first, claim, give back. */
        void onClose(boolean disconnect) {
            if (!degraded) {
                if (writeRefused) degraded = true;
                else file = new TreeMap<>(cells);
            }
            VaultCloseDisposal disposal = VaultCloseDisposal.of(degraded, disconnect);
            if (disposal.claimsTheCells() && !claimed) {
                claimed = true;
                if (disposal == VaultCloseDisposal.DROP) {
                    for (int slot : VaultReturnPolicy.returnedSlots(degraded, STORAGE, alreadyOnDisk())) {
                        String item = cells.remove(slot);
                        if (item != null) ground.add(item);
                    }
                }
            }
            returnEverything();
        }

        Map<String, Object> outcome() {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("file", file);
            o.put("handedBack", new TreeMap<>(count(handedBack)));
            o.put("ground", new TreeMap<>(count(ground)));
            o.put("cursor", cursor);
            return o;
        }

        private static Map<String, Integer> count(List<String> items) {
            Map<String, Integer> m = new TreeMap<>();
            for (String i : items) m.merge(i, 1, Integer::sum);
            return m;
        }
    }

    /** The old Back: one explicit close, reason PLUGIN. */
    private static Vault oldBack(Vault start) {
        Vault v = start.copy();
        v.onClose(false);
        return v;
    }

    /** The new Back: returnEverything in the click, then the implicit close (OPEN_NEW, or a DISCONNECT in the hop). */
    private static Vault newBack(Vault start, boolean disconnectInTheHop) {
        Vault v = start.copy();
        v.returnEverything();
        v.onClose(disconnectInTheHop);
        return v;
    }

    private static Vault page() {
        Vault v = new Vault();
        v.file.put(9, "sword");
        v.cells.put(9, "sword");      // on disk
        v.cells.put(10, "bow");       // placed this session, written by the last gesture's hop... or not
        v.cursor = "arrow";
        return v;
    }

    @Test
    void healthy_theFileGetsThePageAndOnlyTheCursorComesBack() {
        Vault start = page();
        Vault before = oldBack(start);
        Vault after = newBack(start, false);

        assertEquals(before.outcome(), after.outcome());
        assertEquals(Map.of(9, "sword", 10, "bow"), after.file, "the close's flush writes the whole page");
        assertEquals(List.of("arrow"), after.handedBack, "a healthy vault hands back the cursor and nothing else");
    }

    @Test
    void healthy_butTheClosingWriteIsRefused_thePageComesBackAndTheFileKeepsItsLastGoodCopy() {
        Vault start = page();
        start.writeRefused = true;
        Vault before = oldBack(start);
        Vault after = newBack(start, false);

        assertEquals(before.outcome(), after.outcome());
        assertEquals(Map.of(9, "sword"), after.file, "a refused write changes nothing on disk");
        assertEquals(Map.of("arrow", 1, "bow", 1), after.outcome().get("handedBack"),
                "the unpersisted bow comes back; the sword is already on disk, so it is not handed back twice");
    }

    @Test
    void degradedBeforeTheBack_noWriteAndTheUnpersistedCellsComeBack() {
        Vault start = page();
        start.degraded = true;
        Vault before = oldBack(start);
        Vault after = newBack(start, false);

        assertEquals(before.outcome(), after.outcome());
        assertEquals(Map.of(9, "sword"), after.file, "a degraded vault writes nothing");
        assertEquals(Map.of("arrow", 1, "bow", 1), after.outcome().get("handedBack"));
    }

    /**
     * Degraded, and the player quits inside the one-tick hop. The old Back had no hop to quit in: its explicit close
     * handed the cells back in the click. The new one does the same in the click, so the DISCONNECT close that follows
     * finds nothing to drop.
     */
    @Test
    void degradedAndADisconnectInTheHop_theCellsWereAlreadyHandedBackInTheClick_nothingReachesTheGround() {
        Vault start = page();
        start.degraded = true;
        Vault before = oldBack(start);
        Vault after = newBack(start, true);

        assertEquals(before.outcome(), after.outcome());
        assertEquals(Map.of(), after.outcome().get("ground"));
    }

    /** The control: the model can tell two orders apart, so the equalities above are not a model that cannot differ. */
    @Test
    void control_theModelSeesADifferenceWhereOneExists_aDisconnectCloseWithNothingHandedBackFirstDrops() {
        Vault start = page();
        start.degraded = true;

        Vault dropFirst = start.copy();
        dropFirst.onClose(true);      // a DISCONNECT close with nothing handed back first: the cells go to the ground
        Vault handFirst = newBack(start, true);

        assertEquals(Map.of("bow", 1), dropFirst.outcome().get("ground"));
        assertEquals(Map.of(), handFirst.outcome().get("ground"));
    }
}
