package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.UUID;

/**
 * Ignite: a scorched mob that dies explodes, a short delay later, damaging nearby mobs.
 *
 * <b>THE TRIGGER IS THE OPERATOR'S RULING, 2026-09-09: ANY MOB THAT DIES WHILE SCORCHED IGNITES.</b>
 * Binary -- no stack count, no threshold -- and <b>the killing blow need not be a Scorched attack</b>,
 * so a scorched mob that drowns, falls, or is killed by anything at all still ignites. See
 * {@code DESIGN-status-effects.md} for the reconciled spec and {@code NEXT.md}'s
 * <i>THE RULING: ANY MOB THAT DIES WHILE SCORCHED IGNITES</i> for the arithmetic that refused the
 * threshold shape it replaced.
 *
 * <h2>WHY THE DETONATION IS HERE AND NOT IN THE ADAPTER</h2>
 *
 * The trigger is Bukkit-facing; the detonation is not. This method touches three core calls --
 * {@link CombatWorld#schedule}, {@link CombatWorld#combatantsNear} and {@link CombatWorld#present} --
 * and no server type, so it lives where a CLOCK can see it. That matters more here than anywhere
 * else in the project for two reasons:
 *
 * <ul>
 *   <li><b>{@code FakeWorld} already carries this mechanism's own failure detector.</b> Its
 *       {@code MAX_TASKS_PER_ADVANCE} trip-wire exists for "a task that reschedules itself at a rate
 *       the clock can never outrun" -- which is precisely what a runaway ignite chain is. In core
 *       that detector guards this code for free. In an adapter it would never see it.</li>
 *   <li><b>An adapter no test references is where a silent defect lives.</b> {@code EntityScorchSink}
 *       is the worked example from the previous slice: flipping its accrual rule left the entire
 *       suite green. Putting the fan-out in the adapter layer would give the whole mechanism --
 *       fuse, targeting, attribution, element and defense -- that same shape.</li>
 * </ul>
 *
 * <h2>THE NUMBERS ARE PROVISIONAL, AND THE GATE IS WHAT RULES THEM</h2>
 *
 * Nobody can tune what does not exist, so these are defensible STARTING values chosen from content
 * that already ships, not decisions. {@code GATE-ignite.md} is what turns them into rulings. They
 * are pinned by {@code IgniteTest} so a change to any of them has to be deliberate -- the same
 * discipline {@code EnchantState}'s provisional rule carries, and for the same reason: a provisional
 * value with nothing pinning it drifts without anyone noticing it was ever provisional.
 */
public final class Ignite {

    private Ignite() {
    }

    /**
     * Ticks between the death and the blast.
     *
     * <p><b>This delay is what makes the "fires exactly once" rule nearly free</b>, and it is the
     * mechanism of {@code DESIGN}'s second safety rule rather than a feel dial with a number
     * attached. Serializing the chain THROUGH TIME -- A dies, half a second, A explodes, kills B,
     * half a second, B explodes -- turns "each effect resolves against current state" from a
     * same-tick graph problem into an ordinary sequence. 10 ticks is {@code DESIGN}'s own worked
     * example, half a second.
     *
     * <p>PROVISIONAL. Shorter reads as a screen-clear; longer as a slow, interruptible chain.
     */
    public static final int DELAY_TICKS = 10;

    /**
     * Blast radius, in blocks.
     *
     * <p>PROVISIONAL, and chosen to sit IN FAMILY with the blasts that already ship rather than to
     * invent a feel: {@code solar_grenade}'s burst is 4.0, {@code rekindle}'s and
     * {@code ability_stone}'s thrown embers are 4.0, {@code ember_staff} is 3.5 and
     * {@code emberblade} is 3.0. Reusing the prevailing number means a player who has felt a grenade
     * has already felt this.
     */
    public static final double RADIUS = 4.0;

    /**
     * Blast damage, dealt once to each mob in radius.
     *
     * <p><b>PROVISIONAL, AND THE ONE TO HOLD LOOSEST -- BUT IT IS NOT AN ARBITRARY STARTING POINT.
     * IT WAS CHOSEN TO SIT BELOW A FULL-HEALTH VANILLA MOB'S 20.</b> That is what makes a cascade
     * something you SET UP rather than something that happens to you: the blast alone does not kill
     * a healthy zombie, so a chain only propagates through a pack something has already softened.
     *
     * <p><b>So raising this past 20 changes the mechanism's CHARACTER, not just its number</b> --
     * every blast becomes lethal to a full-health mob, and a single ignition clears a pack unaided.
     * If it is ever raised that far, the propagation argument above has to be re-made rather than
     * inherited. 6.0 is {@code solar_grenade}'s burst damage.
     */
    public static final double DAMAGE = 6.0;

    /**
     * The visual played at the blast.
     *
     * <p><b>A SHARED VISUAL IS A COUPLING, AND IT IS NAMED HERE BECAUSE THIS REPO NAMES THEM.</b>
     * {@code solar_detonation} is authored for {@code solar_grenade} and already reused by
     * {@code emberblade} and {@code ember_staff}; this is its fourth consumer. Re-tuning it for any
     * one of them re-tunes it for Ignite too. Reuse rather than a dedicated visual is the same call
     * the content files already record -- <i>"reuse an existing fire visual; a dedicated one is
     * later polish"</i>.
     */
    public static final String VISUAL_ID = "solar_detonation";

    /**
     * Schedule the blast for a mob that has just died while scorched.
     *
     * <p><b>EVERY VALUE THIS NEEDS IS AN ARGUMENT, AND THAT IS THE WHOLE DESIGN.</b> By the time the
     * scheduled task runs, the mob is gone and its scorch has been forgotten -- so anything read at
     * detonation time would be missing. The caller reads on the death frame and passes the answers
     * in; nothing here reaches back for state.
     *
     * <p><b>THE WORLD, HOWEVER, IS READ INSIDE THE TASK, NEVER AROUND IT.</b>
     * {@link CombatWorld#combatantsNear} is legal only on the thread owning its centre's region, and
     * {@link CombatWorld#schedule} is what puts us on that thread. Resolving the target set before
     * scheduling would read off the wrong thread AND resolve against a pack that has half a second
     * left to move.
     *
     * <h2>THE TARGETING RULE IS STATED HERE RATHER THAN INHERITED</h2>
     *
     * <b>Mob-only, and it is reasoned from the mechanism rather than copied from a helper.</b> This
     * matters because the two existing fan-outs disagree -- {@code EffectApplier}'s burst path skips
     * the caster only, while its thrown-ember path skips players too -- and nothing in the codebase
     * says which is intended. Inheriting either by proximity would be borrowing a rule that was
     * never ruled. The reason Ignite is mob-only is specific to what a cascade IS:
     *
     * <ul>
     *   <li>A burst is <b>aimed and immediate</b>. Someone standing in it made a positioning
     *       decision against a thing they could see coming.</li>
     *   <li>A cascade is <b>neither</b>: it fires half a second after a death, from a corpse,
     *       potentially several links downstream of a kill somebody else made, with no telegraph
     *       beyond the first blast.</li>
     *   <li>And it <b>cannot be play-tested against a second player</b>, so a player-damaging
     *       cascade would ship with no live witness at all.</li>
     * </ul>
     *
     * <b>The cost, stated because it is real:</b> a player can stand safely inside a cascade that a
     * {@code solar_grenade} would have hurt them with. That inconsistency is defensible only while
     * the reason above is written down.
     *
     * @param world     the world to schedule and fan out against
     * @param at        where the mob died, captured on the death frame
     * @param applierId who lit the fire -- scorch's most recent applier for the mob that died, and
     *                  the credit for every kill this blast makes. <b>THE IGNITION IS THE FIRE'S
     *                  DOING, NOT THE KILLING BLOW'S</b>, so it belongs to whoever lit it rather
     *                  than to whoever landed the last hit. Carried by the status rather than
     *                  propagated along the chain: every scorched mob has an applier by definition,
     *                  so every ignition has a source in every case the ruling admits -- including
     *                  drowning, falling and lava, where there is no killer to credit at all.
     * @param victimId  the mob that died, excluded from its own blast
     */
    public static void detonate(CombatWorld world, Vec3 at, UUID applierId, UUID victimId) {
        world.schedule(at, DELAY_TICKS, () -> {
            world.present(at, VISUAL_ID);
            for (Combatant c : world.combatantsNear(at, RADIUS)) {
                // The corpse. A dying mob is still a LivingEntity while its death animation plays,
                // and that animation outlasts this fuse, so it can still be in radius. Excluded
                // defensively: whether it is actually present at +10 ticks has NOT been measured,
                // and the exclusion costs nothing either way.
                if (c.id().equals(victimId)) continue;
                if (c.state().player()) continue;
                // DefenseRule.APPLIES, deliberately, and NOT the burn's BYPASSED. The burn's
                // exemption exists because it is a percent-of-max effect on a clock -- armour
                // blunting it would mean armour reducing a fraction of your own health. A blast is
                // a flat number dealt once, like every other discrete hit, so it goes through
                // Defense like every other discrete hit. See Scorch's armour section, which names
                // this as the BOUNDARY of that rule rather than an exception to it.
                //
                // AccrualRule.INERT: the blast wears fire for the glyph and the effectiveness
                // matrix, but buys survivors NO stacks. A chain therefore spreads only through mobs
                // something already lit -- the ignitable set is the set you lit. ACCRUES would make
                // each blast recruit its own survivors, and the cascade's only terminator would be
                // running out of mobs.
                c.handle().applyDamage(DAMAGE, applierId,
                        CritState.NORMAL, DefenseRule.APPLIES, "fire", AccrualRule.INERT);
            }
        });
    }
}
