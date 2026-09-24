package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.QuiverCue;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.OptionalLong;

/**
 * The sound a reload makes when it finishes.
 *
 * <p>Operator's ruling, 2026-09-24, choosing option (1) of the three proposed in
 * {@code GATE-quiver-feedback.md}: <b>one scheduled task per reload</b>, firing
 * {@code item.crossbow.loading_end} -- the natural partner to the {@code loading_start} that
 * {@link QuiverNotice#reloadStarted} already plays.
 *
 * <h2>THIS IS THE FIRST SCHEDULED TASK IN THE RELOAD DESIGN, AND THAT WAS A DELIBERATE TRADE</h2>
 *
 * <p>{@code Quivers}' own javadoc argued for evaluating the reload on READ precisely because <i>"a
 * scheduled task needs an expiry event, and an expiry event can be missed."</i> That argument is
 * untouched and the item is <b>still the authority</b>: this task decides nothing and stores nothing
 * beyond the deadline it was made for. It asks the item, at fire time, whether the thing it was made
 * for is still true.
 *
 * <p><b>The reason a cue needs a task at all:</b> nothing runs at maturity. The reload is settled
 * lazily by the next read, so there is no moment at which a sound could otherwise be played, and the
 * cue's whole value is its TIMING. Option (2) -- sounding inside {@code finishReload} -- is free and
 * lands the noise on a later shot, which is a different thing entirely.
 *
 * <h2>WHAT MAKES IT SAFE</h2>
 *
 * <p><b>The deadline comparison.</b> {@link QuiverCue#shouldSound} is the whole guard, it is pure, it
 * lives in {@code core}, and it covers swapping away, two quivers, a dropped weapon, a reload settled
 * early by the shot path, and a second reload started in between. Its javadoc enumerates them.
 *
 * <p><b>A departed player needs no check here, and that is the port's doing rather than luck.</b>
 * {@code PaperScheduler.onEntityLater} passes a {@code null} retired-runnable, and its own comment
 * records what that means: <i>"if the entity is gone before the delay, do nothing."</i> So a logout,
 * a death or an unload drops the task rather than running it against a stale player. <b>An
 * {@code isOnline()} check here would be a guard whose failure path cannot be observed</b>, which this
 * repo treats as worse than no guard.
 *
 * <p><b>It is an ENTITY task, not a region or global one</b>, because it reads the player's main hand
 * and plays a sound to them. On Folia the player's own scheduler is the only thread allowed to do
 * that, and it follows them across regions -- which a region task pinned at the reload's location
 * would not.
 */
public final class QuiverReloadCue {

    private QuiverReloadCue() {}

    /** Vanilla's crossbow finish, matching the loading_start that opens the reload. */
    private static final String SOUND = "item.crossbow.loading_end";
    private static final float VOLUME = 1.0f;
    private static final float PITCH = 1.0f;

    /**
     * Book the cue for a reload that has just started.
     *
     * @param completesAt the tick the reload matures -- <b>the same value stamped on the item</b>,
     *                    passed in rather than recomputed so the task and the stamp cannot disagree
     * @param reloadTicks the reload's REAL length, after reload-time modifiers, which is what
     *                    {@code Quivers.beginReload} resolved through {@code ReloadTime.resolve}. The
     *                    delay must be that and not the authored number, or a player with a
     *                    reload-time bonus hears the cue late.
     */
    public static void schedule(Player player, WeaponDefinition weapon, AdapterContext adapters,
                                long completesAt, int reloadTicks) {
        String weaponId = weapon.id();
        adapters.scheduler().onEntityLater(player,
                () -> fire(player, weaponId, adapters, completesAt), reloadTicks);
    }

    /**
     * Ask the item whether the reload this task was made for is the one that just finished; if it is,
     * SETTLE it and then sound.
     *
     * <p>Every value is read fresh. Nothing is remembered across the delay except
     * {@code scheduledDeadline}, which is the question rather than an answer.
     *
     * <h2>*** IT SETTLES, AS OF 2026-09-24, AND THE CLASS JAVADOC'S "DECIDES NOTHING" IS WITHDRAWN ***</h2>
     *
     * <p>Ben's report on #147's boot was that the magazine <b>still read 0 when the sweep finished</b>.
     * That is the lazy reload working as designed and feeling broken: the rounds sit in
     * {@code quiver_reload_pending} until a read reaches {@code finishReload}, so the number moved on
     * the player's next ACTION rather than at maturity.
     *
     * <p><b>Now that a task exists at exactly the right moment, the settle belongs in it.</b> The
     * magazine reads full as the click plays, which is what a completion cue is for -- a sound saying
     * "ready" over a counter saying 0 is worse than no sound.
     *
     * <p><b>THIS ADDS NO HAZARD, BECAUSE IT RIDES A GUARD THAT WAS ALREADY LOAD-BEARING.</b>
     * {@link QuiverCue#shouldSound} already had to cover the swap, the drop, two quivers, a logout and
     * a reload settled early by the shot path -- because every one of them would otherwise produce a
     * WRONG SOUND. The settle sits behind the same comparison, so each case lands the same way:
     *
     * <pre>
     * swapped away / dropped   no match -&gt; no sound, NO SETTLE. Correct: the reload is still on the
     *                          item and the next read of it settles it, exactly as before this change.
     * two quivers              each task matches only the item carrying its own deadline.
     * settled early by a shot  the stamps are gone, so nothing matches -- no double settle.
     * logged out               PaperScheduler drops the task; nothing runs at all.
     * </pre>
     *
     * <p><b>THE ITEM STAMP IS STILL THE AUTHORITY AND THIS TASK IS STILL SUBORDINATE TO IT.</b> What a
     * missed or stale task costs is now <b>the TIMING of the refill</b> rather than the refill: every
     * lazy path still settles, so a reload this task never saw is settled by the next shot, reload or
     * draw. That is the property {@code Quivers}' javadoc rests on, and it is the reason the settle can
     * be added here without giving up leak-proofness.
     *
     * <p><b>ORDER: SETTLE, THEN SOUND.</b> The sound is the announcement and the write is the thing
     * announced.
     */
    private static void fire(Player player, String scheduledWeaponId, AdapterContext adapters,
                             long scheduledDeadline) {
        Keys keys = adapters.keys();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!QuiverCue.shouldSound(scheduledWeaponId, WeaponItems.weaponId(held, keys),
                scheduledDeadline, deadlineOn(held, keys))) {
            return;
        }
        // THE WEAPON IS RE-RESOLVED RATHER THAN CAPTURED. Only the ID crosses the delay -- the same
        // reason the deadline does and the ItemStack does not -- so a definition reloaded by /rpg reload
        // mid-flight cannot be a stale object held by a task.
        //
        // A weapon that no longer resolves is a no-op: shouldSound has already proven the item claims
        // this id, so an empty lookup here means the content went away underneath it, and refusing to
        // settle leaves the reload for the lazy path exactly as a missed task would.
        adapters.weapons().find(scheduledWeaponId).ifPresent(weapon -> {
            Quivers.settleMaturedReload(player, weapon, adapters);
            player.playSound(player.getLocation(), SOUND, VOLUME, PITCH);
        });
    }

    /** The completion tick stamped on a stack, or empty -- including for a stack with no meta. */
    private static OptionalLong deadlineOn(ItemStack stack, Keys keys) {
        if (stack == null || !stack.hasItemMeta()) return OptionalLong.empty();
        Long stamped = stack.getItemMeta().getPersistentDataContainer()
                .get(keys.quiverReloadCompletesAt, PersistentDataType.LONG);
        return stamped == null ? OptionalLong.empty() : OptionalLong.of(stamped);
    }

}
