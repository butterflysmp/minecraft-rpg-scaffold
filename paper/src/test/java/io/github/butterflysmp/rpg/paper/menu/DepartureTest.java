package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Departure}: the order a menu-to-menu navigation runs in, and that it runs ONCE.
 *
 * <p><b>What this proves:</b> the give-back runs synchronously, in the click, BEFORE the hop is even requested; the
 * next menu opens only inside the hop; a second departure from the same menu does nothing. <b>What it does not
 * prove:</b> that {@code Menu.navigateTo} hands {@code Departure} the real {@code returnEverything} and the real
 * scheduler. That is a wiring question, and {@code MenuNavigationSignatureTest} carries it.
 *
 * <p>{@code Menu} cannot be constructed without a server ({@code Bukkit.createInventory}), so the decision lives in a
 * Bukkit-free class and is tested here, the same split as {@code GridClickIntent} and {@code MenuRouting}'s pure
 * halves.
 */
class DepartureTest {

    /** A hop that queues its task rather than running it: the scheduler's "next tick", made inspectable. */
    private static final class QueuedHop implements Consumer<Runnable> {
        final List<Runnable> queued = new ArrayList<>();

        @Override
        public void accept(Runnable task) {
            queued.add(task);
        }

        void runNextTick() {
            List<Runnable> now = new ArrayList<>(queued);
            queued.clear();
            now.forEach(Runnable::run);
        }
    }

    @Test
    void theGiveBackRunsInTheClickAndTheOpenOnlyInTheHop() {
        List<String> order = new ArrayList<>();
        QueuedHop hop = new QueuedHop();
        Departure departure = new Departure();

        boolean started = departure.begin(
                () -> order.add("give-back"),
                task -> { order.add("hop requested"); hop.accept(task); },
                () -> order.add("open next"));

        assertTrue(started);
        assertEquals(List.of("give-back", "hop requested"), order,
                "the player's items come back IN THE CLICK, before the hop is even requested -- the same moment the "
                        + "old explicit close returned them -- and nothing opens yet");
        assertEquals(1, hop.queued.size(), "exactly one hop");

        hop.runNextTick();
        assertEquals(List.of("give-back", "hop requested", "open next"), order,
                "the next menu opens inside the hop, never inline in the click handler (Menu.open's first rule)");
    }

    @Test
    void underwayIsFalseUntilBeginAndTrueFromThen() {
        Departure departure = new Departure();
        assertFalse(departure.underway(), "a fresh menu is not departing");

        QueuedHop hop = new QueuedHop();
        departure.begin(() -> { }, hop, () -> { });
        assertTrue(departure.underway(), "underway from the click, so the menu is inert during the hop");

        hop.runNextTick();
        assertTrue(departure.underway(), "and it stays so: a departed menu never takes a click again");
    }

    /**
     * Two Back clicks inside the one-tick hop. Before this change the first click CLOSED the screen, so a second could
     * not land. Now the screen stays up for a tick, and a second departure would open the next menu twice.
     */
    @Test
    void aSecondDepartureFromTheSameMenuDoesNothing() {
        int[] giveBacks = {0};
        int[] opens = {0};
        QueuedHop hop = new QueuedHop();
        Departure departure = new Departure();

        assertTrue(departure.begin(() -> giveBacks[0]++, hop, () -> opens[0]++));
        assertFalse(departure.begin(() -> giveBacks[0]++, hop, () -> opens[0]++),
                "the second begin is refused and says so");
        hop.runNextTick();

        assertEquals(1, giveBacks[0], "one give-back");
        assertEquals(1, opens[0], "one open");
    }

    /** Underway is set BEFORE the give-back runs, so a give-back that re-enters (a listener, a nested click) is refused. */
    @Test
    void aReentrantDepartureFromInsideTheGiveBackIsRefused() {
        QueuedHop hop = new QueuedHop();
        Departure departure = new Departure();
        boolean[] inner = {true};

        departure.begin(() -> inner[0] = departure.begin(() -> { }, hop, () -> { }), hop, () -> { });

        assertFalse(inner[0], "the re-entrant begin is refused");
        assertEquals(1, hop.queued.size(), "and requested no hop of its own");
    }
}
