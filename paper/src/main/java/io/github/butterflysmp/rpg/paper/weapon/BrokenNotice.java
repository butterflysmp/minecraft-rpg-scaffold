package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Telling a player their weapon is broken, without spamming them.
 *
 * Private to the holder, like the dry-pool denial it sits beside -- a plain {@code sendMessage},
 * never a broadcast.
 *
 * <p><b>Why this is throttled at all.</b> Both paths it serves are deliberately silent by default,
 * and for good reasons that a per-action message would trample:
 *
 * <ul>
 *   <li>{@code WeaponSwingListener.onSwing} discards its result -- "a swing that lands nothing
 *       because you are mid-cooldown or out of energy does not deserve chat spam". Holding
 *       left-click is one swing per tick.</li>
 *   <li>{@code RpgListeners.onRightClick} silences refusals when the press IS a basic attack, which
 *       the hunters_bow's shot is. Holding fire is attacking, not repeatedly deciding to cast.</li>
 * </ul>
 *
 * Broken has to speak on both -- a weapon that does nothing with no explanation is
 * indistinguishable from a bug -- so it bypasses those silences and pays for it with a throttle
 * instead. First blocked action reports; holding the button stays quiet; a later attempt reminds.
 *
 * <p><b>A sound rides with the message</b>, immediately after it and therefore INSIDE the same
 * throttle -- so the two can never separate, and holding an input cannot turn the feedback into a
 * rattle. It fires wherever the message does: the moment a use breaks the weapon
 * ({@code WeaponDurability.applyWearOnUse}) and every gated action afterwards. Per-player through
 * {@code Player#playSound}, not the world's, so only the holder hears it -- the same privacy the
 * plain {@code sendMessage} above is chosen for. A broadcast would tell a whole server about one
 * player's inventory.
 *
 * <p>The throttle reuses {@link CooldownTracker} rather than adding a second per-player map: it is
 * already concurrent (Folia has two players on two region threads), already keyed per player, and
 * already cleared on quit by {@code RpgListeners.onQuit}, so this adds no state that can leak.
 */
public final class BrokenNotice {

    private BrokenNotice() {}

    /**
     * The tracker key. Leading underscores so it can never collide with a real ability id -- those
     * come from content filenames, and the loader would have to accept a file called
     * {@code __broken_weapon_notice.yml} for this bucket to be shared with a genuine cooldown.
     */
    private static final String KEY = "__broken_weapon_notice";

    /** Roughly two seconds. Long enough that held input is quiet, short enough to feel responsive. */
    private static final int THROTTLE_TICKS = 40;

    /**
     * The break sound, as a KEY rather than {@code org.bukkit.Sound.ENTITY_ITEM_BREAK}.
     *
     * Not a style preference. {@code org.bukkit.Sound} is an interface whose constants are
     * registry-backed, and touching one without a running server throws -- MEASURED, with a
     * throwaway probe that printed
     * {@code PROBE_RESULT: THREW -> java.lang.ExceptionInInitializerError}, not assumed from the
     * javadoc on {@code VisualSpec.Sound} that says the same thing. Referencing the constant here
     * would make this class unloadable in any unit test forever, to buy a compile-time check on one
     * string. {@code VisualSpec.Sound} keeps a key for exactly this reason and says so.
     *
     * The string is the cost of that: a typo plays SILENCE rather than failing, and nothing at
     * startup catches it -- {@code ContentValidator} checks {@code Registry.SOUND_EVENT} for
     * content-declared sounds, and this one is hardcoded, so it is outside that net.
     *
     * <p>So the key was proved from the PINNED api's BYTECODE ({@code
     * paper-api-26.1.2.build.49-beta}), where the static initialiser reads
     * {@code ldc_w "entity.item.break"} immediately followed by
     * {@code putstatic ENTITY_ITEM_BREAK} -- the field-to-key association itself, not merely the
     * string's presence somewhere in the class.
     *
     * <p><b>Do not try to confirm this with {@code /playsound}.</b> It was tried. The command
     * answers {@code No player was found} for a bogus id (
     * {@code minecraft:definitely.not.a.real.sound}) exactly as it does for a real one, because it
     * resolves targets before it cares about the sound and a resource pack may define any id it
     * likes. Four commands, four identical replies, zero information -- a check that runs, looks
     * green, and discriminates nothing. Only the negative control revealed that, which is why it
     * is written down here instead of being rediscovered.
     *
     * <p>If the vanilla break sound reads as "the item VANISHED" -- the one thing this system
     * promises never happens -- swap it for {@code "block.anvil.land"} or
     * {@code "block.note_block.bass"}. Both were proved the same way, adjacent to their own
     * {@code putstatic}, so either is a one-word edit here and nowhere else.
     */
    private static final String BROKEN_SOUND = "entity.item.break";

    /** Full volume, unshifted pitch: this is a notification, not an ambience. */
    private static final float BROKEN_SOUND_VOLUME = 1.0f;
    private static final float BROKEN_SOUND_PITCH = 1.0f;

    /**
     * Tell {@code player} their weapon is broken, at most once per {@link #THROTTLE_TICKS}.
     *
     * Must run on the thread that owns the player -- every caller is already there (the swing
     * listener via its Netty hop, the interact and damage handlers by virtue of being events).
     */
    public static void notify(Player player, CooldownTracker cooldowns) {
        if (!cooldowns.isReady(player.getUniqueId(), KEY)) return;
        cooldowns.trigger(player.getUniqueId(), KEY, THROTTLE_TICKS);
        player.sendMessage(Component.text(
                "Your weapon is broken -- repair it before using it.", NamedTextColor.GRAY));
        // Past the throttle check, so message and sound are one notification and cannot drift
        // apart. Player#playSound, not World#playSound: the holder hears it, nobody else does.
        // Same thread as the sendMessage above -- no new threading concern, and every caller is
        // already on the thread that owns the player.
        player.playSound(player.getLocation(), BROKEN_SOUND,
                BROKEN_SOUND_VOLUME, BROKEN_SOUND_PITCH);
    }
}
