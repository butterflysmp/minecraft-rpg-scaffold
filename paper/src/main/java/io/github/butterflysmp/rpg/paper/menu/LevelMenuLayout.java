package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.progression.PlayerLevel;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * The level screen's slot map (PLAN-level-bonuses.md, the head sub-menu). Opened from the Nexus hub's
 * stats head.
 *
 * <pre>
 *   row 0   . . . . S . . . .      S  the summary: level, XP to next, the totals
 *   row 1   . L L L L L L L .
 *   row 2   . L L L L L L L .      L  one level each, 28 to a page: 1-28, then 29-50
 *   row 3   . L L L L L L L .
 *   row 4   . L L L L L L L .
 *   row 5   P . . B X . . . N      P / N  previous / next page, B back, X close
 * </pre>
 *
 * <p>Back at 48 and Close at 49, the convention the other Nexus sub-screens use
 * ({@code SettingsMenuLayout}). <b>The look is a default and Ben's to change</b>; the plan offered it as
 * one option among several.
 */
final class LevelMenuLayout {

    private LevelMenuLayout() {}

    static final int SIZE = 54;
    static final int SUMMARY_SLOT = 4;
    static final int PREV_SLOT = 45;
    static final int BACK_SLOT = 48;
    static final int CLOSE_SLOT = 49;
    static final int NEXT_SLOT = 53;

    /** The inner 7 x 4 block, row by row. Its order IS the order levels are laid out in. */
    static final List<Integer> LEVEL_SLOTS = IntStream.rangeClosed(1, 4)
            .flatMap(row -> IntStream.rangeClosed(1, 7).map(col -> row * 9 + col))
            .boxed()
            .toList();

    static final int PAGE_SIZE = LEVEL_SLOTS.size();

    /** Enough pages for levels 1..ACTIVE_CAP. Two at a cap of 50; it follows the cap if that moves. */
    static final int PAGE_COUNT = (PlayerLevel.ACTIVE_CAP + PAGE_SIZE - 1) / PAGE_SIZE;

    /** Every slot that is neither a level, the summary nor a button. Paging slots are NOT in it. */
    static final Set<Integer> FILLER_SLOTS = buildFiller();

    private static Set<Integer> buildFiller() {
        Set<Integer> slots = new LinkedHashSet<>();
        for (int slot = 0; slot < SIZE; slot++) slots.add(slot);
        slots.removeAll(LEVEL_SLOTS);
        slots.remove(SUMMARY_SLOT);
        slots.remove(PREV_SLOT);
        slots.remove(BACK_SLOT);
        slots.remove(CLOSE_SLOT);
        slots.remove(NEXT_SLOT);
        return Set.copyOf(slots);
    }

    /** The level shown at {@code index} of {@code page} (both 0-based), or 0 when past the active cap. */
    static int levelAt(int page, int index) {
        int level = page * PAGE_SIZE + index + 1;
        return level <= PlayerLevel.ACTIVE_CAP ? level : 0;
    }

    /** The page {@code level} is on, 0-based -- so the screen opens where the player is. */
    static int pageOf(int level) {
        return (Math.min(Math.max(level, 1), PlayerLevel.ACTIVE_CAP) - 1) / PAGE_SIZE;
    }
}
