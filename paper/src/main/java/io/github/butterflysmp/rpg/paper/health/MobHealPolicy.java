package io.github.butterflysmp.rpg.paper.health;

import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

/**
 * What {@code RpgListeners.onRegainHealth} does with a vanilla heal on a TRACKED MOB (M15,
 * PLAN-mob-scaling.md slice 3). The mob twin of {@link VanillaHealPolicy}, in the same house style: an
 * exhaustive switch over every {@link RegainReason} with no default arm, so a tenth Paper constant is a
 * compile error rather than a silent fall-through.
 *
 * <p><b>Every reason is REROUTE.</b> Nothing replaces a mob's regeneration the way
 * {@code HealthRegenSystem} replaces a player's, so cancelling any of them without translating it would
 * be a silent no-op: a witch drinking healing that heals nothing. So each is cancelled AND translated
 * into the custom store, by the same proportion as environmental damage (vanilla amount / the vanilla
 * MAX_HEALTH attribute x the custom max, {@code DamageScale.toCustom}), and an End crystal heals the
 * dragon the same fraction of its bar as in vanilla.
 *
 * <p><b>CUSTOM is a DECIDED REROUTE</b>, where the player policy passes it. A mob's own custom heals
 * never touch vanilla health ({@code CombatantStats.heal} writes the store), so a CUSTOM regain on a mob
 * is some other plugin's or command's vanilla heal, and dropping it would be the silent no-op.
 *
 * <p>This lives in {@code paper}, not {@code core}, because {@link RegainReason} is a Bukkit type.
 */
public final class MobHealPolicy {

    private MobHealPolicy() {}

    /** What the listener does with the event. */
    public enum Action {
        /** Leave it alone: vanilla health moves and the custom store does not. NO mob reason uses it today. */
        PASS,
        /** Cancel it AND heal the custom store by the proportional amount. */
        REROUTE
    }

    public static Action forReason(RegainReason reason) {
        return switch (reason) {
            // Passive and food regeneration: a tamed wolf eating, a horse, a mob's own regen.
            case REGEN, SATIATED, EATING -> Action.REROUTE;
            // Potions and effects: a witch's healing, a splashed regeneration.
            case MAGIC, MAGIC_REGEN -> Action.REROUTE;
            // The boss heals, and M15's named case: the End crystal on the dragon.
            case ENDER_CRYSTAL, WITHER_SPAWN, WITHER -> Action.REROUTE;
            // Decided, not discovered: see the class javadoc.
            case CUSTOM -> Action.REROUTE;
        };
    }
}
