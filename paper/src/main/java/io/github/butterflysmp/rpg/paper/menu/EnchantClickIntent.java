package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.enchant.EnchantCandidate;
import io.github.butterflysmp.rpg.core.enchant.EnchantSlot;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;

/**
 * What a single left-click on a candidate cell DOES.
 *
 * <p>One button, and the cell's own state decides what it means: a locked candidate unlocks, an
 * unlocked one becomes active, the active one levels up. That is the whole interaction model, and
 * it is a proposal -- flagged for in-game tuning -- which is exactly why it lives here as a pure
 * function rather than as a chain of ifs inside a click handler. Changing the model should be
 * changing this switch and its tests, not re-reading a Bukkit class.
 *
 * <p>Extracted for the reason {@code EnchantEffectLine} and {@code ApplyArgs} were: the decision is
 * Bukkit-free, the class that would otherwise host it cannot be constructed in a unit test, and the
 * cases that matter most have NO shipped content that exercises them on a boot. No shipped enchant
 * has {@code max_level: 1}, and no shipped weapon carries an id whose content file is missing, so
 * the two arms most likely to be got wrong are precisely the two a boot gate cannot reach.
 *
 * <p><b>The economy pass gates the same click.</b> The cost check goes in FRONT of this call, never
 * inside it: this answers "what would this click do", and affording it is a different question.
 */
public enum EnchantClickIntent {

    /**
     * Level 0 -> unlock at I, AND make it active, in that order.
     *
     * <p>One click, not two. The order is load-bearing at the call site:
     * {@code EnchantState.withActive} REFUSES a locked candidate, so the level has to land first
     * and the activation has to run on the RESULT rather than on the state that was read.
     */
    UNLOCK,

    /**
     * Unlocked, and not the active one -> make it active.
     *
     * <p>The previous active keeps its level. That retention is the property the whole candidate
     * model exists for -- a level rides the CANDIDATE, not the choice -- so swapping back and forth
     * costs nothing and a player can experiment.
     */
    ACTIVATE,

    /** Active, and below this enchant's OWN maximum -> one level. */
    LEVEL_UP,

    /**
     * Active and at the cap. A no-op WITH feedback, never a silent nothing.
     *
     * <p>A dead click and a handled cap look identical from the other side of the screen, and the
     * player's next move on a dead click is to report the menu as broken.
     */
    AT_MAX,

    /** The slot rolled fewer candidates than the table shows: a filler pane, not a cell. */
    EMPTY,

    /**
     * The item's blob names an id no content file defines.
     *
     * <p>Reachable, and not a hypothetical: {@code EnchantLoader} fail-softs a malformed file and
     * the item's PDC still names the enchant. Refused rather than acted on, because we can neither
     * describe what it does nor bound how far it levels -- and changing a level nothing defines a
     * maximum for is the one edit that cannot be undone by looking at it.
     */
    UNKNOWN_ENCHANT;

    /**
     * What this click means.
     *
     * @param slot       the enchant slot the clicked column belongs to.
     * @param candidate  the index within it. MAY be past the end -- the table always paints three
     *                   rows and a slot may have rolled fewer -- which is {@link #EMPTY}.
     * @param definition the candidate's content file, or {@code null} for an id the registry no
     *                   longer knows.
     */
    public static EnchantClickIntent of(EnchantSlot slot, int candidate, EnchantDefinition definition) {
        if (candidate < 0 || candidate >= slot.candidates().size()) return EMPTY;

        // BEFORE the state branches, deliberately. An unknown enchant is refused whatever its state:
        // unlocking one would put a level on something the menu cannot even name.
        if (definition == null) return UNKNOWN_ENCHANT;

        EnchantCandidate chosen = slot.candidates().get(candidate);
        if (chosen.isLocked()) return UNLOCK;
        if (slot.activeIndex() != candidate) return ACTIVATE;

        // The PER-ENCHANT cap, not the model's. EnchantState.withLevel will happily take a
        // max_level: 1 enchant to 3 -- core has no idea which enchants exist, which is why
        // RpgCommand.java re-checks this by hand too. The min() is belt and braces against a
        // definition built in a test rather than loaded: the record already refuses a maxLevel
        // above the model's, so it cannot fire on anything the loader produced.
        int cap = Math.min(definition.maxLevel(), EnchantState.MAX_LEVEL);
        // ">=" rather than "==": a candidate already PAST its cap -- content edited down after the
        // item was enchanted -- must not level further. Fails in the same direction
        // DamageEnchants.percentAt clamps.
        return chosen.level() >= cap ? AT_MAX : LEVEL_UP;
    }
}
