package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * The two things a Dragon's Plume has to tell its wielder, and the one it must not.
 *
 * <p>{@link QuiverNotice}'s mechanism, reused wholesale: a throttle keyed in the existing
 * {@link CooldownTracker} under {@code __}-prefixed ids no content filename could produce. That
 * class's argument holds unchanged -- the tracker is already concurrent, already keyed per player,
 * and already cleared on quit by {@code RpgListeners.onQuit}, so this adds no state that can leak.
 *
 * <h2>THREE REASONS REACH A RELEASE AND THEY ARE NOT ONE REFUSAL</h2>
 *
 * <pre>
 * BELOW_VANILLA_FLOOR        SILENT. Nothing happened, so nothing is said.
 * NO_ROUNDS                  {@link #noRounds} -- the magazine is spent.
 * the draw never started     {@link #noArrow}  -- R5, and nothing else teaches it.
 * </pre>
 *
 * <p><b>The silence is a decision and is the easiest of the three to get wrong.</b> A release under
 * {@code DrawRelease.MIN_RELEASE_TICKS} is a twitch vanilla would not have fired either; the player
 * has not asked for a shot, and answering a question they did not ask is how a notice becomes noise.
 * {@code DrawRelease.decide} checks the floor FIRST for exactly this reason, so a sub-floor twitch on
 * an empty magazine is silent rather than a lecture about ammunition.
 *
 * <h2>TWO NOTICES, TWO KEYS, AND TWO DIFFERENT TRIGGER PATHS</h2>
 *
 * <p><b>This is the structural point, and it is where someone would otherwise try to unify them:</b>
 *
 * <pre>
 * noRounds   fires at a REAL RELEASE         -- PlumeDraw.fire, off DrawRelease.Reason
 * noArrow    fires when there was NO RELEASE -- PlumeDraw's scheduled tick finding
 *                                               hasActiveItem() false one tick after the interact
 * </pre>
 *
 * <p>They do not share a code path, a moment, or a remedy. <b>One is about the magazine; the other
 * is about the player's inventory.</b>
 *
 * <h2>AND THEY MUST NOT SHARE A THROTTLE KEY, BECAUSE THE PLAYER HITS BOTH IN SEQUENCE</h2>
 *
 * <p>{@code QuiverNotice.noAmmo} got its own key in Slice E with the argument written out, and the
 * same sequence is reachable here -- one weapon along and faster:
 *
 * <pre>
 * release on an empty magazine   -&gt; noRounds: "left-click to reload"
 * the player left-clicks          -&gt; the reload EATS THEIR LOOSE ARROWS (QuiverAmmo.sources
 *                                    walks getStorageContents)
 * they right-click to draw        -&gt; no arrow anywhere, the draw never starts -&gt; noArrow
 * </pre>
 *
 * <p><b>All of it inside a few seconds, and the second notice is the one they need most</b> -- they
 * have just done exactly what they were told and the weapon has stopped responding. A shared bucket
 * would swallow it, and the bug would present as <i>"the reload broke my bow"</i>.
 *
 * <p><b>Nor does {@link #noRounds} reuse {@code QuiverNotice}'s {@code EMPTY_KEY} or its sentence</b>,
 * and the reason is not tidiness. Every other quiver weapon refuses <b>before</b> the shot -- the gate
 * is on the press. <b>The Plume refuses AFTER a three-second hold</b>, so the player has already paid
 * the charge. A message that reads identically to the instant refusal hides the thing that actually
 * happened to them.
 */
public final class PlumeNotice {

    private PlumeNotice() {}

    // *** THE ACTION BAR, NOT CHAT, AND THE WORDS ARE UNCHANGED. *** Ben's action-bar pick,
    // 2026-09-23, reaching these two on 2026-09-24. QuiverNotice moved in this branch's first commit
    // and these were left behind -- a split the pick did not ask for, and the player-facing result was
    // that a Plume put its refusals in two different places depending on which refusal it was.
    //
    // THE ARGUMENT IS QuiverNotice'S AND IT TRANSFERS WITHOUT CHANGE: a magazine state is transient,
    // so it belongs where transient things go, and a permanent chat line about a condition that lasted
    // two seconds is the wrong shape.
    //
    // AND IT IS STRONGER HERE, WHICH IS WHY THESE TWO ARE NOT MERELY CONSISTENT NOW. The class javadoc
    // below traces a sequence a player hits in a few seconds -- release dry, reload, draw, no arrow --
    // that produces noRounds and then noArrow. In chat that is two permanent lines about one fumble.
    //
    // THE THROTTLE IS STILL 40 TICKS AND STILL LOAD-BEARING, for QuiverNotice's reason: an action bar
    // overwrites rather than accumulating, so the SPAM becomes invisible rather than absent. noArrow in
    // particular fires from a SCHEDULED TICK, and a held right-click on a bow with no arrow re-enters
    // onDrawStarted on every interact packet.
    //
    // *** NOT SWEPT, AND RECORDED AS UNRULED RATHER THAN EXCLUDED. *** Measured 2026-09-24: three
    // notices still send to chat -- BrokenNotice, ShieldBrokenNotice and NexusCollisionNotice, one
    // sendMessage each. Ben's pick was about the QUIVER's notices and nobody has put the question for
    // those three, whose subjects are not transient in the same way (a broken item stays broken). The
    // question is open, not answered; NoticeSurfaceTest scopes itself to the quiver family for exactly
    // that reason and says so.

    /** Leading underscores so no content filename can collide -- {@code BrokenNotice}'s convention. */
    private static final String NO_ROUNDS_KEY = "__plume_no_rounds_notice";

    /**
     * ITS OWN KEY, and the class javadoc's sequence is why: a player who reloads on
     * {@link #noRounds}'s advice can strand themselves here in the same breath.
     */
    private static final String NO_ARROW_KEY = "__plume_no_arrow_notice";

    /** {@code QuiverNotice}'s window, deliberately: repeated presses stay quiet, still responsive. */
    private static final int THROTTLE_TICKS = 40;

    /**
     * The project's refusal sound. <b>Declared here rather than shared, which is the house shape</b>
     * -- {@code QuiverNotice} and {@code BrokenNotice} each own their sound constants too. A shared
     * constants class would couple three surfaces that have nothing else in common.
     */
    private static final String REFUSAL_SOUND = "block.dispenser.fail";
    private static final float SOUND_VOLUME = 1.0f;
    private static final float SOUND_PITCH = 1.0f;

    /**
     * The charge was earned and the magazine could not pay for it.
     *
     * <p>Names the remedy, like every notice in this project: a refusal with no remedy reads as a
     * bug. <b>And it names the HOLD</b>, because that is what separates this from every other
     * empty-quiver refusal in the game -- the player waited three seconds for nothing, and a
     * message that did not say so would leave them wondering whether the release even registered.
     */
    public static void noRounds(Player player, CooldownTracker cooldowns) {
        if (!throttled(player, cooldowns, NO_ROUNDS_KEY)) return;
        player.sendActionBar(Component.text(
                "The draw was held for nothing -- your quiver is empty. Left-click to reload.",
                NamedTextColor.GRAY));
        // Past the throttle check so message and sound are one notification and cannot drift apart,
        // exactly as BrokenNotice and QuiverNotice pair them. Player#playSound, not World#playSound:
        // the holder hears it, nobody else does.
        player.playSound(player.getLocation(), REFUSAL_SOUND, SOUND_VOLUME, SOUND_PITCH);
    }

    /**
     * R5: THE BOW WILL NOT DRAW WITHOUT A PLAIN ARROW ON THE PLAYER, AND NOTHING ELSE TEACHES IT.
     *
     * <p>Vanilla's own gate, measured out of the pinned jar: {@code BowItem.use} returns FAIL when
     * {@code getProjectile} finds nothing, so the bow never pulls back and <b>no release event is
     * delivered at all.</b> The failure is completely silent -- <b>a legendary bow that does not
     * move is indistinguishable from a broken item</b>, and the Plume's own ammunition is its
     * 25-round quiver, so a player can be FULLY LOADED and unable to draw.
     *
     * <p>Names the item exactly -- <b>plain arrows only</b> -- on {@code QuiverNotice.noAmmo}'s
     * precedent: a player holding a stack of spectral arrows and being told "you need arrows" would
     * reasonably conclude the feature was broken.
     *
     * <p>It names the OFF-HAND specifically, which is R5's actual content rather than a hint:
     * vanilla's {@code ProjectileWeaponItem.getHeldProjectile} checks the off-hand first, while
     * {@code QuiverAmmo.sources} walks {@code getStorageContents()}, which EXCLUDES it. <b>So an
     * off-hand arrow satisfies the draw and no reload can ever eat it</b> -- the two halves are in
     * different sets by construction, which is what makes the workaround work at all.
     *
     * <h2>BORN WITH A DELETION TRIGGER, BECAUSE A SURFACE THAT EXPLAINS A WORKAROUND OUTLIVES IT</h2>
     *
     * <p>R5 is recorded as a <b>KNOWN WORKAROUND WITH NO TRIGGER</b> -- the operator's words were
     * <i>"in the future we will have a better solution"</i>, and nobody has designed one. This notice
     * exists to teach that workaround and has no other purpose.
     *
     * <p><b>TRIGGER: the day the off-hand arrow requirement is replaced, this method and
     * {@link #NO_ARROW_KEY} are deleted IN THE SAME COMMIT that replaces it</b> -- not in a
     * follow-up, because a follow-up is what turns an instrument into furniture.
     *
     * <p>Written here rather than in a plan for the reason {@code /rpg drawsound}'s trigger was:
     * <b>a surface explaining a workaround stops being true exactly when the workaround dies, and
     * that is the moment nobody is looking at it.</b> A player would be told to do something the
     * weapon no longer needs, and the message would read as authoritative.
     */
    public static void noArrow(Player player, CooldownTracker cooldowns) {
        if (!throttled(player, cooldowns, NO_ARROW_KEY)) return;
        player.sendActionBar(Component.text(
                "This bow needs a plain Arrow in your off-hand to draw -- a reload cannot take it.",
                NamedTextColor.GRAY));
        player.playSound(player.getLocation(), REFUSAL_SOUND, SOUND_VOLUME, SOUND_PITCH);
    }

    /** True if this notice may be sent now; stamps the window as a side effect when it may. */
    private static boolean throttled(Player player, CooldownTracker cooldowns, String key) {
        if (!cooldowns.isReady(player.getUniqueId(), key)) return false;
        cooldowns.trigger(player.getUniqueId(), key, THROTTLE_TICKS);
        return true;
    }
}
