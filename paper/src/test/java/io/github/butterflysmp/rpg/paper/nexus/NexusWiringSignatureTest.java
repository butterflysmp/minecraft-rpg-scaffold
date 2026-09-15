package io.github.butterflysmp.rpg.paper.nexus;

import io.github.butterflysmp.rpg.paper.listener.RpgListeners;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Nexus lock's correctness lives in TWO annotation attributes on TWO methods in ANOTHER class,
 * with nothing in the language connecting them to the guard they protect. This is the connection.
 *
 * <h2>WHAT THE DELETION LOOKS LIKE, WHICH IS WHY A COMMENT IS NOT ENOUGH</h2>
 *
 * <p>{@code ignoreCancelled = true} on {@code onMenuClick} reads as a harmless tidy-up -- an
 * attribute that appears to do nothing, on a handler that has nothing to do with the Nexus. Remove
 * it and:
 *
 * <ul>
 *   <li>the star still cannot be dropped with Q,
 *   <li>it still cannot be picked up in a plain inventory screen,
 *   <li>every menu still opens and every button still works,
 *   <li>and <b>only</b> the PERFORMED routes -- shift-click into the crafting grid, a number key
 *       over a grid cell -- quietly start working again.
 * </ul>
 *
 * <p>Every other test in the suite stays green. That is this repo's signature failure: a check that
 * did not run looks exactly like a check that passed. So the wiring is pinned here, and these tests
 * are named for the DEFECT each prevents rather than for the attribute each reads.
 *
 * <h2>WHY THE ORDERING IS A CONTRACT AND NOT A HOPE</h2>
 *
 * <p>Bukkit dispatches strictly by {@code EventPriority}; within ONE priority the order is
 * undefined. {@code onPlayerSweepAttack}'s javadoc already records what that costs -- <i>"sits at
 * the SAME priority, so which of the two runs first is undefined; if the canceller lost the coin
 * toss, ignoreCancelled would not save us"</i>. LOWEST strictly precedes NORMAL, so there is no
 * coin toss. Both halves of that arrangement are asserted below, because either alone is inert.
 *
 * <h2>WHAT IT DOES NOT CLAIM</h2>
 *
 * <p>It cannot check that the guard's VERDICT is right -- that is {@code NexusLockTest} -- nor that
 * MenuRouting still performs rather than permits its cross-inventory moves. It checks the two facts
 * that are reflectable and silent: the priorities, and the attribute.
 */
class NexusWiringSignatureTest {

    @Test
    void theMenuDispatcherSkipsACancelledClick_orThePerformedRoutesSilentlyReopen() {
        // MenuRouting does not merely un-cancel: shiftMove and hotbarMove call setCurrentItem and
        // setItem DIRECTLY. Those writes cannot be undone by any later cancel, so the only defence
        // is that route() never runs at all on a refused click.
        assertTrue(handler(InventoryClickEvent.class, "onMenuClick").ignoreCancelled(),
                "onMenuClick must be ignoreCancelled=true, or a refused click still reaches "
                        + "MenuRouting, which PERFORMS the move the guard just cancelled");
        // Mutation: delete ignoreCancelled from onMenuClick -> reddens here and NOWHERE else.
    }

    @Test
    void theMenuDragDispatcherSkipsACancelledDrag_sameDefect() {
        assertTrue(handler(InventoryDragEvent.class, "onMenuDrag").ignoreCancelled(),
                "onMenuDrag must be ignoreCancelled=true, for onMenuClick's reason");
        // Mutation: delete ignoreCancelled from onMenuDrag -> reddens here and nowhere else.
    }

    @Test
    void theClickGuardRunsSTRICTLYBeforeTheMenuDispatcher_notMerelyAtSomeOtherPriority() {
        // Asserted as a strict ORDERING rather than as two literals, so that moving either handler
        // reddens. Equal priorities would leave the order undefined and the guard a coin toss.
        EventPriority guard = handler(InventoryClickEvent.class, "onNexusClick").priority();
        EventPriority dispatcher = handler(InventoryClickEvent.class, "onMenuClick").priority();

        assertEquals(EventPriority.LOWEST, guard,
                "the Nexus click guard must run at LOWEST");
        assertTrue(guard.ordinal() < dispatcher.ordinal(),
                "the guard (" + guard + ") must run STRICTLY before the menu dispatcher ("
                        + dispatcher + "); same-priority order is undefined");
        // Mutation: onNexusClick -> HIGHEST, or onMenuClick -> LOWEST -> reddens.
    }

    @Test
    void theDragGuardRunsSTRICTLYBeforeTheMenuDragDispatcher_sameDefect() {
        EventPriority guard = handler(InventoryDragEvent.class, "onNexusDrag").priority();
        EventPriority dispatcher = handler(InventoryDragEvent.class, "onMenuDrag").priority();

        assertEquals(EventPriority.LOWEST, guard, "the Nexus drag guard must run at LOWEST");
        assertTrue(guard.ordinal() < dispatcher.ordinal(),
                "the drag guard (" + guard + ") must run STRICTLY before the menu drag dispatcher ("
                        + dispatcher + ")");
        // Mutation: onNexusDrag -> NORMAL -> the ordering assertion reddens.
    }

    @Test
    void everyNexusHandlerIsActuallyRegisterable_anUnannotatedGuardIsInert() {
        // A handler that loses its @EventHandler is never called at all, and looks completely
        // normal. All six, because the six event families are independent and five of them have no
        // other test in this file.
        for (String name : new String[]{
                "onNexusClick", "onNexusDrag", "onNexusDrop",
                "onNexusSwapHand", "onNexusGiveToEntity", "onNexusArmorStand"}) {
            assertTrue(annotated(name), name + " has no @EventHandler and is therefore never called");
        }
        // Mutation: delete @EventHandler from any one of the six -> reddens naming it.
    }

    // ---------------------------------------------------------------- helpers

    private static EventHandler handler(Class<?> eventType, String method) {
        try {
            EventHandler annotation = RpgListeners.class
                    .getDeclaredMethod(method, eventType)
                    .getAnnotation(EventHandler.class);
            assertTrue(annotation != null, method + " has no @EventHandler");
            return annotation;
        } catch (NoSuchMethodException e) {
            throw new AssertionError(
                    "RpgListeners." + method + "(" + eventType.getSimpleName() + ") is gone. The "
                            + "Nexus lock depends on it by name; renaming it silently unpins the "
                            + "wiring this class exists to hold.", e);
        }
    }

    /** Is there a method of this name on RpgListeners carrying @EventHandler? */
    private static boolean annotated(String name) {
        for (Method method : RpgListeners.class.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.isAnnotationPresent(EventHandler.class)) {
                return true;
            }
        }
        return false;
    }
}
