package io.github.butterflysmp.rpg.paper.health;

import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

/**
 * What to do with a vanilla heal aimed at a TRACKED PLAYER.
 *
 * <h2>The invariant this exists to enforce</h2>
 *
 * No vanilla heal may move a tracked player's bar without moving the truth. The vanilla health
 * attribute is a DISPLAY -- {@code HeartBarRenderer} rewrites it from the custom numbers on the next
 * {@code HealthChange} or reconcile tick -- so a vanilla heal that lands is visible for a fraction of
 * a second and is then silently reverted. That reads to a player as a bug, and it is one.
 *
 * <h2>A separate class, not a switch inside the listener</h2>
 *
 * Because the classification is the whole decision and it is the only part of the handler a unit test
 * can reach. {@code EntityRegainHealthEvent} needs an entity; {@link RegainReason} is a plain enum
 * that loads without a server. So the policy is pinned exactly here and the listener is left as the
 * three lines that act on it.
 *
 * <h2>Exhaustive, with NO default arm, deliberately</h2>
 *
 * {@link #forReason} is a switch EXPRESSION over every one of the nine constants. A tenth added by a
 * future Paper release is then a COMPILE ERROR rather than a silent fall-through into whichever
 * behaviour the default happened to pick -- {@code NEXT.md}'s "enumerate the axis, not the cases you
 * currently have". Do not add a default to make it shorter; the length IS the guard.
 *
 * <h2>Why EATING is REROUTE and not PASS</h2>
 *
 * <b>Because its reachability cannot be read, only measured.</b> The pinned Paper API's own javadoc
 * says "When an animal regains health from eating consumables"; Bukkit's wider documentation
 * describes it as a player reason. Nothing in the constant list settles which is true here.
 *
 * <p>Filing it with the unreachable boss/crystal reasons would therefore be asserting a mechanism
 * instead of measuring one, and it would be wrong in one of two ways with no way to tell which: a
 * latent hole in the invariant above if it does fire, or a false justification with no witness if it
 * does not. Treating it exactly like {@link RegainReason#MAGIC} is correct EITHER WAY and needs no
 * gate row to prove a negative. It also leaves {@link Action#PASS}'s "unreachable behind the player
 * scope" an accurate description of everything actually in it.
 *
 * <h2>Why CUSTOM passes</h2>
 *
 * It is the reason another plugin's heal arrives under, and our own would if anything ever routed
 * through the event. Cancelling everything would make this the sole writer of a tracked player's
 * health, which is a tidier invariant and a worse one: it eats the unforeseen silently.
 */
public final class VanillaHealPolicy {

    private VanillaHealPolicy() {}

    /** What the listener does with the event. */
    public enum Action {
        /** Leave it alone. */
        PASS,
        /** Cancel it. This slice's regeneration system replaces it. */
        CANCEL,
        /** Cancel it AND translate its amount into custom HP, so the heal still happens. */
        REROUTE
    }

    /**
     * The action for one reason, on a tracked player.
     *
     * <p><b>Nothing here is CANCEL-without-replacement except the two regens this slice replaces.</b>
     * That is the rule the arms are grouped by: never cancel a heal you are not ready to replace,
     * because a cancelled potion is a silent no-op -- a clean-looking bug that heals zero, which is
     * worse by this codebase's standards than the visible flicker it would have replaced.
     */
    public static Action forReason(RegainReason reason) {
        return switch (reason) {
            // Replaced. HealthRegenSystem is the passive heal now, and it pays into custom HP.
            case SATIATED, REGEN -> Action.CANCEL;

            // Cancelled AND translated. A potion still heals; it heals the right pool.
            // EATING rides here because its reachability is unknown -- see the class javadoc.
            case MAGIC, MAGIC_REGEN, EATING -> Action.REROUTE;

            // Someone else's heal, or a future one of ours. Not ours to eat.
            case CUSTOM -> Action.PASS;

            // Genuinely unreachable behind the caller's "is this a tracked player" gate: a dragon's
            // crystal, a wither spawning or taking its own effect. Named so the switch stays
            // exhaustive and the next Paper constant does not compile.
            case ENDER_CRYSTAL, WITHER_SPAWN, WITHER -> Action.PASS;
        };
    }
}
