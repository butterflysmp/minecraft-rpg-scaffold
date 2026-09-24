package io.github.butterflysmp.rpg.core.weapon;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * Should a scheduled reload-complete cue actually make a sound?
 *
 * <h2>WHY THIS IS A PURE FUNCTION IN core/ AND NOT AN {@code if} IN THE PAPER TASK</h2>
 *
 * <p>The cue is the first scheduled task in the reload design, and a task's hazard is that it fires
 * into a world that has moved on: the weapon was swapped, dropped, replaced by a second quiver, or
 * the reload was already settled by a shot. <b>The decision that handles all of that is arithmetic on
 * four values</b>, and arithmetic belongs where it can be executed in two seconds rather than in a
 * sixty-second boot.
 *
 * <p>It is also what makes the guard <b>mutable</b>. The paper task needs a {@code Player}, an
 * {@code ItemStack} and a sound, so a mutation there is boot-only and a broken comparison would ship
 * green. Here, breaking the comparison reddens {@code QuiverCueTest}.
 *
 * <h2>THE RULE, AND EVERY HAZARD IT COVERS</h2>
 *
 * <p><b>Sound the cue only if the held item is the same weapon AND carries the very deadline the task
 * was scheduled for.</b> Operator's ruling, 2026-09-24. One comparison, five hazards:
 *
 * <ul>
 *   <li><b>Swapped away.</b> A different weapon in hand carries a different id, or none.</li>
 *   <li><b>Two quivers.</b> Both may be reloading; their deadlines were stamped at different ticks,
 *       so each task matches only its own item.</li>
 *   <li><b>Dropped.</b> Nothing is in hand, so nothing sounds -- correct: the player is not holding
 *       the thing that finished.</li>
 *   <li><b>Settled early by the shot path.</b> {@code Quivers.finishReload} removes the stamps, so a
 *       task firing afterwards finds no deadline and stays quiet. <b>That is what "at most once"
 *       means</b> -- the cue defers to the settle rather than doubling it.</li>
 *   <li><b>A second reload started in between.</b> It stamped a later deadline, so the older task
 *       cannot match it.</li>
 * </ul>
 *
 * <p><b>ONE CONSEQUENCE, STATED BECAUSE IT IS A REAL CORNER AND NOT A BUG.</b> If the player fires in
 * the very tick the reload matures and the shot runs first, the stamps are already gone and the cue is
 * silent. The window is exactly one tick, the player got their magazine and their shot, and the
 * alternative -- sounding without checking -- is the double cue the ruling forbids.
 *
 * <p><b>The item remains the authority.</b> The task carries no state but the deadline it was made
 * for; every fact it decides on is read back off the item at fire time.
 */
public final class QuiverCue {

    private QuiverCue() {}

    /**
     * @param scheduledWeaponId the weapon whose reload this task was created for
     * @param heldWeaponId      the weapon in the player's hand now, or empty for anything else
     * @param scheduledDeadline the completion tick this task was scheduled against
     * @param heldDeadline      the completion tick stamped on the held item, or empty if none is
     * @return true if the cue should sound
     */
    public static boolean shouldSound(String scheduledWeaponId, Optional<String> heldWeaponId,
                                      long scheduledDeadline, OptionalLong heldDeadline) {
        if (scheduledWeaponId == null || heldWeaponId.isEmpty()) return false;
        if (!scheduledWeaponId.equals(heldWeaponId.get())) return false;
        if (heldDeadline.isEmpty()) return false;
        return heldDeadline.getAsLong() == scheduledDeadline;
    }
}
