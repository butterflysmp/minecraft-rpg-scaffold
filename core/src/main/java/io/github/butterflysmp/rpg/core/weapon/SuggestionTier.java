package io.github.butterflysmp.rpg.core.weapon;

/**
 * What KIND of thing a craft suggestion makes, as an ordering.
 *
 * <p>Quick Craft ranks by this first and by how many the player can make second, so a suggestion
 * that mints RPG gear always outranks one that makes sticks. <b>Declaration order IS the ordering</b>
 * -- {@link Enum#ordinal()} is the sort key -- and that is the intended way to change it.
 *
 * <h2>THERE ARE TWO CONSUMERS, NOT ONE</h2>
 *
 * This javadoc used to say reordering these constants "reorders the column", naming the suggestion
 * column alone. Since slice 6 it also orders the <b>recipe browser's catalogue</b> -- which is to
 * say, <i>which page every recipe in the game lands on</i>. Reordering a constant now moves entries
 * between pages for every player, which is a far larger blast radius than three cells.
 *
 * <p><b>Naming one consumer when there are two is how a rule loses track of who depends on it</b>,
 * and it is the same drift the named-debt entry in {@code NEXT.md} records: a note that enumerates
 * the cases alive when it was written rather than the axis it guards. Both are listed here so the
 * next addition has to consider both:
 *
 * <ul>
 *   <li>the Quick Craft <b>suggestion column</b> -- {@code CraftCount.rank} sorts by this ordinal;
 *   <li>the <b>recipe browser catalogue</b> -- tier first, then recipe key, deciding page placement.
 * </ul>
 *
 * <p>The browser's invariant is <b>"all gear sorts ahead of all vanilla"</b>. It is deliberately NOT
 * "page 1 is the gear page": that is arithmetic over two numbers that can both move, and nothing
 * would warn anyone when it stopped holding.
 *
 * <h2>Core defines the AXIS; paper decides which one a recipe IS</h2>
 *
 * Classifying a recipe needs {@code CraftResultIndex} and the sealed {@code GearDefinition}
 * hierarchy, and the index is built from Bukkit materials -- so the decision lives in
 * {@code SuggestionTiers}, in {@code paper}. The adapter TAGS each candidate with its tier and
 * {@link CraftCount} sorts by it.
 *
 * <p>The same inversion the recipe probe uses: core never asks "what kind of thing is this", it is
 * told. That is what keeps the ranking a pure function with real unit tests instead of another
 * boot-gate-only behaviour.
 */
public enum SuggestionTier {

    /** A minted weapon. The most valuable thing a craft can produce, so it sorts first. */
    WEAPON,

    /**
     * A shield today, and whatever an accessory gear kind turns out to be.
     *
     * <p>Named ACCESSORY rather than SHIELD deliberately: this is a display CATEGORY, not the gear
     * axis. When an accessory kind lands it joins here rather than forcing a seventh position, and
     * {@code SuggestionTiers}' exhaustive switch is what makes that a decision someone has to take
     * rather than a default they fall into.
     */
    ACCESSORY,

    /** A minted tool. */
    TOOL,

    /** A minted piece of armor. */
    ARMOR,

    /**
     * A crafting intermediate -- an ingot, a plank, a component.
     *
     * <p><b>NOTHING MAPS HERE TODAY, and that is stated rather than left to be discovered.</b>
     * {@code SuggestionTiers} cannot return this constant: every recipe either mints gear (the four
     * tiers above) or does not ({@link #VANILLA}), and there is no third source of truth that says
     * "this vanilla item is a material rather than a product".
     *
     * <p>So <b>no test and no gate row can exercise it</b>. It is a named position in an ordering,
     * held open because the operator asked for this axis and because the alternative -- adding it
     * later -- would renumber every tier below it. It is not dead structure, but it is not covered
     * either, and a reader deserves to know which.
     */
    MATERIAL,

    /**
     * An ordinary vanilla craft that mints nothing.
     *
     * <p><b>A PLAYER MAY NEVER SEE ONE IN THE COLUMN, AND THAT IS INTENDED.</b> Twenty-four armor
     * pieces, five tools and one shield all claim a {@code craft_result} today, against <b>THREE</b>
     * suggestion cells. A player carrying common materials can therefore fill the whole column with
     * gear and see no vanilla suggestion at all.
     *
     * <p><i>(This paragraph said NINE until slice 6. The column moved to column 7 and shrank to
     * three cells during slice 5 and this text was not revisited -- the same aging-out-from-under-
     * itself that CLAUDE.md's squash-body convention was written about. Three makes the point
     * harder, not softer: the odds of a vanilla craft reaching the column went DOWN.)</i>
     *
     * <p>That is the direction the ordering was asked for. It is written down here because
     * <i>"sticks and torches vanished from the crafting helper"</i> is exactly what it will look
     * like from the outside, and the next person to hear that report should find this paragraph
     * rather than a bug.
     *
     * <p><b>The recipe browser is the answer to it</b>, and is why that trade-off is now acceptable
     * rather than merely intended: every vanilla recipe is reachable there, on its own pages, sorted
     * last. Gate row Q16 records the column's three-cell limit as passing BY DESIGN; the browser is
     * what it hands off to.
     */
    VANILLA
}
