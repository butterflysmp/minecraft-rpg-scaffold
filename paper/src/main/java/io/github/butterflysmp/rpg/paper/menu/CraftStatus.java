package io.github.butterflysmp.rpg.paper.menu;

import org.bukkit.Material;

/**
 * What the crafting grid is currently saying, as three states.
 *
 * <h2>NO NEW STATE IS COMPUTED HERE</h2>
 *
 * Both inputs already exist and are already used to paint the result slot:
 * {@code previewedRecipe} is present or empty, and the grid is empty or not. This enum is a NAME
 * for combinations that were already being decided, so the bar and the result slot cannot disagree
 * about whether a recipe matched -- they are painted from one call, on one trigger.
 *
 * <p>A second source would drift the first time someone changed one and not the other, which is the
 * defect this arc has now met on preview-versus-commit, count-versus-assembly, and tier-versus-mint.
 *
 * <h2>RED collapses two causes, deliberately</h2>
 *
 * {@link #INVALID} means "the grid holds something and nothing will come of it". That is true both
 * for an arrangement no recipe matches AND for a matrix holding minted gear, which
 * {@code CraftMatrixScreen} hides from the server's matcher entirely.
 *
 * <p>Both are honest reds -- in each case the player has items in the grid and no craft is coming --
 * and a fourth state was not asked for. Recorded so the collapse is a decision rather than an
 * oversight: someone who later wants "this contains your gear" as its own colour is ADDING a state,
 * not fixing a bug.
 */
public enum CraftStatus {

    /** The grid is empty. Nothing is wrong; nothing is happening. */
    EMPTY,

    /** A recipe matched. The result slot is showing what it makes. */
    VALID,

    /** The grid holds something and no craft will come of it. See the class javadoc on RED. */
    INVALID;

    /**
     * Which state a grid is in.
     *
     * @param gridEmpty is every grid cell empty?
     * @param matched   did the preview resolve a recipe? This is exactly the {@code previewedRecipe}
     *                  the commit pins against, so the bar cannot claim a match the commit would
     *                  refuse.
     */
    public static CraftStatus of(boolean gridEmpty, boolean matched) {
        if (gridEmpty) return EMPTY;
        return matched ? VALID : INVALID;
    }

    /**
     * The pane this state paints.
     *
     * <p><b>An exhaustive switch EXPRESSION with no default arm</b>, so a fourth state is a compile
     * error here until someone gives it a colour -- rather than silently painting as whichever arm
     * happened to be last. The discipline {@code GridClickIntent} and {@code GearItems} use, and the
     * reason {@code NEXT.md}'s first rule exists.
     */
    public Material material() {
        return switch (this) {
            case EMPTY -> Material.GRAY_STAINED_GLASS_PANE;
            case VALID -> Material.LIME_STAINED_GLASS_PANE;
            case INVALID -> Material.RED_STAINED_GLASS_PANE;
        };
    }
}
