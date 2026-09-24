package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.weapon.QuiverCue;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE RELOAD-COMPLETE CUE SOUNDS FOR ITS OWN RELOAD AND FOR NO OTHER.
 *
 * <h2>THIS FILE EXISTS BECAUSE THE MUTATION SURVIVED THE WHOLE SUITE FIRST</h2>
 *
 * <p>Measured at {@code 599c51f} with the cue wired and no rows written: {@code MUTCUEDEADLINE} --
 * replacing {@code heldDeadline.getAsLong() == scheduledDeadline} with {@code true} -- left
 * <b>1189 core / 62 storage / 913 paper, all green</b>, marker confirmed in the tree. <b>A green first
 * run is the finding.</b> The comparison is the entire safety of a scheduled task, and nothing could
 * see it break.
 *
 * <p>It could not have been otherwise while the decision lived in the paper task: that needs a
 * {@code Player}, an {@code ItemStack} and a sound, so it is boot-only. <b>Moving the decision into
 * {@code core} is what made the guard possible</b>, and that is the reason the class is shaped as a
 * pure function rather than as an {@code if} inside the {@code Runnable}.
 *
 * <h2>THE STAGING, AND WHY NO TWO NUMBERS IN IT ARE EQUAL</h2>
 *
 * <p>Deadlines here are {@code 500} and {@code 620} and the weapon ids differ by more than a letter.
 * A row staged with one deadline, or with the scheduled and held values equal by accident, cannot
 * detect a transposition between them -- which is exactly the fault a broken comparison is.
 */
class QuiverCueTest {

    private static final String PLUME = "dragons_plume";
    private static final String BOLTOR = "boltor";
    private static final long DEADLINE = 500L;
    private static final long OTHER_DEADLINE = 620L;

    /** The ordinary case: same weapon, same deadline, so the reload that finished is this one. */
    @Test
    void soundsWhenTheHeldItemCarriesTheVeryDeadlineTheTaskWasMadeFor() {
        assertTrue(QuiverCue.shouldSound(PLUME, Optional.of(PLUME), DEADLINE, OptionalLong.of(DEADLINE)),
                "same weapon, same deadline: this is the reload that just matured");
    }

    /**
     * A DIFFERENT DEADLINE ON THE RIGHT WEAPON IS THE CASE THE COMPARISON EXISTS FOR, and it is the
     * one a count of arguments cannot see.
     *
     * <p>It is reached two ways in play and both matter: a SECOND reload started after this task was
     * booked (a later deadline), and a second identical weapon whose own reload is running. In both,
     * the weapon id matches and the sound would be a lie about which reload finished.
     *
     * <p><b>Mutation: replace the comparison with {@code true} -> this row reddens alone.</b> Measured;
     * the same mutation left the entire suite green before this file existed.
     */
    @Test
    void staysSilentWhenTheDeadlineOnTheItemIsADifferentOne() {
        assertFalse(QuiverCue.shouldSound(PLUME, Optional.of(PLUME), DEADLINE,
                        OptionalLong.of(OTHER_DEADLINE)),
                "a later deadline means a DIFFERENT reload -- a second one started, or a second "
                        + "quiver's. Sounding here would name the wrong reload");
    }

    /**
     * NO DEADLINE AT ALL: the reload was already settled, and the cue must defer rather than double.
     *
     * <p>{@code Quivers.finishReload} removes the stamps, so a task firing after the shot path has
     * settled the reload finds nothing. <b>This is what "at most once per reload" means in
     * practice</b> -- not a counter, but the absence of the thing the task was asking about.
     */
    @Test
    void staysSilentWhenTheReloadHasAlreadyBeenSettledAndTheStampsAreGone() {
        assertFalse(QuiverCue.shouldSound(PLUME, Optional.of(PLUME), DEADLINE, OptionalLong.empty()),
                "no deadline on the item means the reload was already completed -- the cue defers to "
                        + "the settle instead of doubling it");
    }

    /** Swapped to another weapon of ours: the id no longer matches, so this is not our reload. */
    @Test
    void staysSilentWhenAnotherOfOurWeaponsIsHeld() {
        assertFalse(QuiverCue.shouldSound(PLUME, Optional.of(BOLTOR), DEADLINE,
                        OptionalLong.of(DEADLINE)),
                "a different weapon in hand: even an identical deadline is not this weapon's reload");
    }

    /**
     * Holding nothing of ours -- an empty hand, a dirt block, a vanilla bow -- or having dropped the
     * weapon entirely. Both arrive here as an absent id.
     *
     * <p>Silence is correct rather than merely safe: the player is not holding the thing that
     * finished, so a sound would have no referent on screen.
     */
    @Test
    void staysSilentWhenNothingOfOursIsHeld() {
        assertFalse(QuiverCue.shouldSound(PLUME, Optional.empty(), DEADLINE, OptionalLong.of(DEADLINE)),
                "empty hand, vanilla item, or dropped: there is nothing for the cue to be about");
    }

    /**
     * A null scheduled id cannot match anything, and says so rather than throwing.
     *
     * <p><b>Unreachable from the one caller</b> -- {@code QuiverReloadCue.schedule} takes it from
     * {@code weapon.id()}, which a definition always has. It is asserted anyway because the method is
     * public on a core class and a {@code NullPointerException} inside a scheduled task is a stack
     * trace in the log with no player-visible cause. <b>Its only exercise is this row</b>, which is
     * stated here because the next reader will otherwise assume production covers it.
     */
    @Test
    void aNullScheduledWeaponIdIsFalseRatherThanAThrow() {
        assertFalse(QuiverCue.shouldSound(null, Optional.of(PLUME), DEADLINE, OptionalLong.of(DEADLINE)),
                "no scheduled weapon means no match, and never an exception inside a task");
    }
}
