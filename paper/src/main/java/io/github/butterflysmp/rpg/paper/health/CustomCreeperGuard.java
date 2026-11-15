package io.github.butterflysmp.rpg.paper.health;

/**
 * ONE OF OUR CREEPERS NEVER DETONATES THE VANILLA WAY (PLAN-dragon-mobs.md step R1). Today that is the Phage.
 *
 * <p>The Phage's explosion will be ours, end to end (seat ruling, step R3): our trigger distance, our fuse, our
 * priced damage to players and summons only, and no blocks. Until R3 builds it, the Phage must not explode at all,
 * so all three vanilla routes to a creeper explosion are refused for a creeper that carries a {@code mob_id}. Read
 * from the pinned server jar (build 74; PLAN-dragon-mobs.md section 7 F-C, and {@code Creeper.mobInteract} /
 * {@code thunderHit} for this slice):
 *
 * <ul>
 *   <li><b>THE SWELL</b>: {@code SwellGoal} counts the fuse up near a target and {@code Creeper.tick} calls
 *       {@code explodeCreeper()} when it is full, which fires {@code ExplosionPrimeEvent}. <b>Cancelled, the creeper
 *       lives, its swell is reset to 0 and its ignited flag cleared</b>, so a player sees a Phage swell, snap back,
 *       and swell again while they stand close. That is expected and temporary. <b>This cancel is the backstop for
 *       every route</b>: nothing reaches the blast without passing it.</li>
 *   <li><b>FLINT AND STEEL / A FIRE CHARGE</b> ({@code ItemTags.CREEPER_IGNITERS}): {@code mobInteract} calls
 *       {@code ignite()}, which fires {@code CreeperIgniteEvent}. Cancelled, the Phage is never ignited, so it never
 *       swells unstoppably. The item still takes its durability or is still used up: {@code mobInteract} spends it
 *       after {@code ignite()} returns, whatever the event said.</li>
 *   <li><b>LIGHTNING</b>: {@code thunderHit} fires {@code CreeperPowerEvent} and, uncancelled, CHARGES the creeper.
 *       It does not ignite it; a charged creeper's blast is twice the radius. Refused, so a Phage never carries the
 *       charged look into R3, where our explosion would not honour it.</li>
 * </ul>
 *
 * <p>Keyed on the {@code mob_id} TAG, not on the registry: a creeper that is ours stays ours even if its file is later
 * removed. A vanilla creeper is untouched.
 */
public final class CustomCreeperGuard {

    private CustomCreeperGuard() {}

    /** True when the vanilla detonation route must be refused: the entity is a creeper AND one of ours. */
    public static boolean refusesVanillaDetonation(boolean isCreeper, String mobId) {
        return isCreeper && mobId != null;
    }
}
