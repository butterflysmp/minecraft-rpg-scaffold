package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Telling a player their SHIELD is broken. The sibling of {@link BrokenNotice}, and a separate class
 * for two reasons, only one of which is the wording.
 *
 * <p>Slice 1 shipped no shield notice at all, and that was correct THEN: a broken shield still
 * blocked at full strength, so there was no consequence to announce, and {@code BrokenNotice}'s text
 * ("your weapon is broken -- repair it before using it") would have been two lies at once. Slice 2
 * gives the break a real consequence -- {@code ShieldBlock.resolve} returns {@code Outcome.NONE} for
 * a broken shield -- so the notice is now the thing that makes that consequence legible instead of
 * feeling like a bug.
 *
 * <h2>ITS OWN THROTTLE KEY, and this is the load-bearing half</h2>
 *
 * {@link BrokenNotice} throttles on {@code "__broken_weapon_notice"} in the shared per-player
 * {@link CooldownTracker}. Reusing that key would mean a broken WEAPON silences the shield notice
 * for two seconds and vice versa -- and a player fighting with a spent sword and a spent shield is
 * exactly the situation where both need to speak. Two keys, two buckets, no interference. Both are
 * cleared by {@code cooldowns.clear(playerId)} on quit, so neither leaks.
 *
 * <h2>WHAT THE PLAYER SEES IS NOT WHAT THE PLAYER GETS, and only a boot can settle it</h2>
 *
 * {@code Durability.wear} floors at one remaining use, so a "broken" shield is still a perfectly
 * functional item as far as VANILLA is concerned. It will keep playing the raise animation, the
 * block sound and the knockback dampen, and will keep reporting {@code BLOCKING < 0} on the event --
 * while our {@code resolve} returns NONE and the player takes the hit in full. This message is the
 * only thing distinguishing that from a broken block mechanic. Whether one crossing notice is enough
 * against vanilla continuing to animate a block that does nothing is a FEEL question the boot gate
 * owes an answer to; it is not settled here.
 */
public final class ShieldBrokenNotice {

    private ShieldBrokenNotice() {}

    /**
     * The tracker key, DISTINCT from {@code BrokenNotice.KEY}. Leading underscores so it can never
     * collide with a real ability id -- those come from content filenames.
     */
    private static final String KEY = "__broken_shield_notice";

    /** Matches {@code BrokenNotice}: long enough that a flurry of blocks is quiet, short enough to remind. */
    private static final int THROTTLE_TICKS = 40;

    /**
     * The same sound key {@link BrokenNotice} proved from the pinned api's bytecode, and inherited
     * rather than re-chosen so the two notices feel like one system.
     *
     * <p>The caveat {@code BrokenNotice} records applies here too and slightly harder: this reads as
     * "the item VANISHED", which is the one thing the durability system promises never happens -- a
     * shield floored at one use is still in your hand. If it ever reads wrong, both classes take the
     * same one-word edit. Do NOT try to verify a replacement with {@code /playsound}; that was tried
     * and discriminates nothing (see {@code BrokenNotice.BROKEN_SOUND}).
     */
    private static final String BROKEN_SOUND = "entity.item.break";

    private static final float BROKEN_SOUND_VOLUME = 1.0f;
    private static final float BROKEN_SOUND_PITCH = 1.0f;

    /**
     * Tell {@code player} their shield is broken, at most once per {@link #THROTTLE_TICKS}.
     *
     * <p>Says what the consequence IS rather than merely that the item is spent, because the
     * consequence is the part that is otherwise invisible: vanilla still animates the block.
     *
     * <p>Must run on the thread that owns the player. The only caller is
     * {@code ShieldDurability.applyWearOnBlock}, reached from the mob-&gt;player damage event, which
     * is already there.
     */
    public static void notify(Player player, CooldownTracker cooldowns) {
        if (!cooldowns.isReady(player.getUniqueId(), KEY)) return;
        cooldowns.trigger(player.getUniqueId(), KEY, THROTTLE_TICKS);
        player.sendMessage(Component.text(
                "Your shield is broken -- it no longer blocks. Repair it.", NamedTextColor.GRAY));
        // Past the throttle check, so message and sound are one notification and cannot drift apart.
        player.playSound(player.getLocation(), BROKEN_SOUND,
                BROKEN_SOUND_VOLUME, BROKEN_SOUND_PITCH);
    }
}
