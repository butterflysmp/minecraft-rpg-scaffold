package io.github.butterflysmp.rpg.paper.menu;

import java.util.function.Consumer;

/**
 * One menu's departure to another: give the player's items back NOW, open the next menu a tick LATER, and only once.
 *
 * <p>Bukkit-free on purpose, so the order is a unit row ({@code DepartureTest}) rather than a boot.
 * {@link Menu#navigateTo} is its only caller and hands it the real pieces.
 *
 * <p><b>Underway is set FIRST</b>, before the give-back runs, so a give-back that re-enters (a listener reacting to
 * the items arriving, a nested click) cannot start a second departure. And it never clears: a menu that has departed
 * is finished, whether the next one opened or not. If the next screen fails to open, the old one stays up and inert,
 * and Esc still closes it through {@code onClose}.
 */
final class Departure {

    private boolean underway;

    /** True from the moment a departure begins. The menu drops every click and drag while this holds. */
    boolean underway() {
        return underway;
    }

    /**
     * @param giveBack runs NOW, in the click: the player's items back in their inventory in the same tick
     * @param hop      the scheduler's next-tick hop; handed {@code arrive} and nothing else
     * @param arrive   opens the next menu, inside the hop
     * @return false, having run nothing, if this menu is already departing
     */
    boolean begin(Runnable giveBack, Consumer<Runnable> hop, Runnable arrive) {
        if (underway) return false;
        underway = true;
        giveBack.run();
        hop.accept(arrive);
        return true;
    }
}
