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
 * and no server type, so it lives where a CLOCK can see it.
 *
 * <p><b>The five properties this mechanism has are all timing- or seam-shaped</b> -- the fuse, the
 * targeting, the attribution, the element and the defense rule -- and a clock is the only thing that
 * can assert any of them. <b>An adapter no test references is where a silent defect lives:</b>
 * {@code EntityScorchSink} is the worked example from the previous slice, where flipping its accrual
 * rule left the entire suite green. Putting the fan-out in the adapter layer would give this whole
 * mechanism that same shape. <b>That reason alone carries the placement.</b>
 *
 * <h2>AND WHAT CORE CANNOT COVER, IN THE SAME BREATH, SO THE PLACEMENT IS NOT OVER-READ</h2>
 *
 * <b>THE CHAIN ITSELF IS EMERGENT FROM THE PAPER EVENT LOOP AND HAS NO CORE WITNESS.</b> Each link
 * re-enters through {@code RpgListeners.onEntityDeath}; core never learns that anything died, which
 * is also why one death stays one scheduled task. <b>A green core suite proves each blast is
 * delayed, targeted, attributed and mitigated. It proves NOTHING about a cascade.</b>
 * {@code GATE-ignite.md} is the only witness for that.
 *
 * <p><b>In particular, {@code FakeWorld}'s runaway trip-wire does NOT guard this.</b> That check
 * fires on <i>a task that reschedules itself</i>; {@link #detonate} schedules once and never
 * re-arms, so the detector cannot see an ignite chain -- not rarely, structurally. An earlier draft
 * of this javadoc claimed the opposite as a reason for the core placement. It was false, and it
 * contradicted {@code IgniteTest}'s own javadoc in the same commit. Recorded rather than quietly
 * deleted, because a placement defended by a reason that does not hold is one refactor away from
 * being moved back.
 *
 * <h2>THE BLAST RECRUITS, AND THAT REVERSED AN EARLIER RULING</h2>
 *
 * Until 2026-09-09 the blast was INERT: it wore fire for the glyph and bought survivors no stacks, so
 * <i>the ignitable set was the set you lit</i>. <b>That was ruled the other way</b> -- links 1..3 now
 * ACCRUE, so each blast scorches its survivors and the cascade grows its own fuel.
 *
 * <p><b>THE OLD REASONING WAS NOT MISTAKEN, AND IT IS RECORDED RATHER THAN QUIETLY DROPPED.</b> It
 * argued that recruitment's only terminator would be "you run out of mobs", which in a spawner or a
 * farm is a room-clearing chain nobody asked for. That was correct, and it is exactly why
 * {@link #MAX_CHAIN_DEPTH} exists: <b>the operator took the risk deliberately and then bounded it.</b>
 * Overturned BY RULING, with a bound supplied -- which is a different thing from the argument having
 * been wrong, and the distinction matters to anyone tempted to remove the cap on the grounds that
 * recruitment "was fine".
 *
 * <h2>THE NUMBERS WERE PROVISIONAL. THE GATE RAN 2026-09-10, AND THEY ARE NOW ADOPTED.</h2>
 *
 * They began as defensible STARTING values chosen from content that already ships, on the argument
 * that nobody can tune what does not exist. <b>{@code GATE-ignite.md} was named as the authority that
 * would rule them, it ran green, and no tuning was requested -- so all four are ADOPTED:</b>
 * {@link #RADIUS} 4.0, {@link #DAMAGE} 6.0, {@link #DELAY_TICKS} 20, {@link #MAX_CHAIN_DEPTH} 4.
 *
 * <p><b>THE LABEL MATTERS MORE THAN IT LOOKS, WHICH IS WHY IT IS MOVED RATHER THAN LEFT.</b> A value
 * still marked provisional is <b>a value nobody owns</b>: the next person to read
 * <i>"PROVISIONAL, shorter reads as a screen-clear"</i> treats {@code DELAY_TICKS} as unclaimed --
 * when it is also the reason a four-link cascade is four seconds of FUSE, and the number the
 * 28-second worst case is written against. <b>An adopted value changed is a DECISION; a provisional
 * one changed is HOUSEKEEPING.</b> Moving them across is what makes the next change argue for itself.
 *
 * <p>This is <i>a rule outliving its premise</i> in its other form: not an arithmetic that keeps
 * evaluating after its reason is gone, but <b>a LABEL that keeps disclaiming after the thing it
 * disclaimed was settled.</b> The disclaimer is the part that goes stale silently -- nothing fails,
 * and it reads as caution rather than as a stale note.
 *
 * <p>They remain pinned by {@code IgniteTest}, which is now pinning adopted values rather than
 * guarding provisional ones -- the same rows, a stronger claim.
 */
public final class Ignite {

    private Ignite() {
    }

    /**
     * Ticks between the death and the blast.
     *
     * <p><b>This delay is what makes the "fires exactly once" rule nearly free</b>, and it is the
     * mechanism of {@code DESIGN}'s second safety rule rather than a feel dial with a number
     * attached. Serializing the chain THROUGH TIME -- A dies, a second, A explodes, kills B, a
     * second, B explodes -- turns "each effect resolves against current state" from a same-tick
     * graph problem into an ordinary sequence.
     *
     * <p><b>20 TICKS DELIBERATELY EXCEEDS {@code DESIGN}'s WORKED EXAMPLE, WHICH SAYS HALF A
     * SECOND.</b> Ruled 2026-09-09. Stated rather than left, because a constant whose javadoc cites
     * a spec it no longer matches is the falsified-prose shape: {@code DESIGN}'s "half a second" is
     * an illustration of the SHAPE (serialise through time), not a number this has to hit, and at
     * one second a cascade reads as a wave a player can watch roll rather than as a single event.
     *
     * <h2>THIS BOUNDS THE FUSE. IT DOES NOT BOUND THE WAVE.</h2>
     *
     * <b>{@link #MAX_CHAIN_DEPTH} is FOUR LINKS, NOT FOUR SECONDS</b>, and the difference arrived
     * with recruitment. While the only way into a chain was to be KILLED by a blast, every link was
     * one fuse from the last and four links really were four seconds. <b>A recruited SURVIVOR takes
     * a second path with a much longer clock:</b> it burns {@code Scorch.DEFAULT_DURATION_TICKS}
     * (120 ticks, six seconds), dies on its final burn, and only then starts a fuse.
     *
     * <pre>
     *   four links x (6s burn window + 1s fuse) = UP TO 28 SECONDS
     * </pre>
     *
     * So tuning this toward zero for snappiness does NOT make a cascade short -- it makes each
     * detonation arrive sooner after its own death, and leaves the burn window untouched. The bound
     * on the wave is the depth cap; this is the bound on one link's delay.
     *
     * <p><b>ADOPTED 2026-09-10</b>, on the gate. Shorter reads as a screen-clear; longer as a slow,
     * interruptible chain. <b>And it is no longer only a feel dial:</b> it is the per-link fuse the
     * cascade-duration arithmetic is written against, so lowering it for snappiness changes a figure
     * two other places quote.
     */
    public static final int DELAY_TICKS = 20;

    /**
     * How many links a cascade may run before it stops propagating.
     *
     * <b>DEPTH, NOT A COUNT OF EXPLOSIONS, AND THE DIFFERENCE IS NOT PEDANTRY.</b> A cascade is a
     * TREE -- one blast can kill two mobs and both ignite. Counting explosions would make "which is
     * the fourth" depend on iteration order among siblings, which nothing chose and no test could
     * pin. Depth is a property of the link, so every branch is bounded independently and the answer
     * does not move when the fan-out loop is reordered.
     *
     * <pre>
     *   depth 1   the ignition caused by a PLAYER kill
     *   depth 2   a mob killed by (or recruited by) a depth-1 blast
     *   depth 3   ...
     *   depth 4   fires, damages normally, and PROPAGATES NOTHING
     * </pre>
     *
     * <p><b>"Propagates nothing" needs no new mechanism.</b> The fourth link passes
     * {@link HitAccrual#terminal()}, which is INERT -- so it scorches nobody, AND it fails
     * {@code ElementAccrual.accruesScorch}, so its kills do not ignite either. <b>Both halves of the
     * rule fall out of a value that already existed</b>, which is the third distinct job the accrual
     * rule has done without a special case.
     *
     * <p><b>The safety property this exists for is DURATION.</b> Width is uncomfortable; unbounded
     * duration is what takes a server down. Without a cap, a dense room is a chain whose link count
     * is bounded only by the mob population -- and see {@link #DELAY_TICKS} for why that is far
     * longer in seconds than the link count suggests.
     */
    public static final int MAX_CHAIN_DEPTH = 4;

    /**
     * Blast radius, in blocks.
     *
     * <p><b>ADOPTED 2026-09-10</b>, on the gate. Chosen to sit IN FAMILY with the blasts that already ship rather than to
     * invent a feel: {@code solar_grenade}'s burst is 4.0, {@code rekindle}'s and
     * {@code ability_stone}'s thrown embers are 4.0, {@code ember_staff} is 3.5 and
     * {@code emberblade} is 3.0. Reusing the prevailing number means a player who has felt a grenade
     * has already felt this.
     */
    public static final double RADIUS = 4.0;

    /**
     * Blast damage, dealt once to each mob in radius.
     *
     * <p><b>ADOPTED 2026-09-10 on the gate, AND STILL THE ONE TO HOLD LOOSEST -- BUT IT WAS NEVER AN ARBITRARY STARTING POINT.
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
     * <p><b>ITS OWN VISUAL, AND THE POINT IS TO REMOVE A COUPLING RATHER THAN TO ADD POLISH.</b> This
     * pointed at {@code solar_detonation}, which is authored for {@code solar_grenade} and already
     * shared with {@code emberblade} and {@code ember_staff} -- so tuning the cascade's bang would
     * have re-tuned three weapons' fireballs, and tuning theirs would have re-tuned the cascade. A
     * shared visual is a coupling this repo names; this one is now cut.
     *
     * <p>{@code ignite_blast} is an explosion particle and an explosion crack, at a lower pitch than
     * {@code ember_burst}'s so a cascade is audibly distinct from a thrown ember even though both use
     * {@code entity.generic.explode}.
     */
    public static final String VISUAL_ID = "ignite_blast";

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
     * scheduling would read off the wrong thread AND resolve against a pack that has a second
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
     *   <li>A cascade is <b>neither</b>: it fires a second after a death, from a corpse,
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
     *
     *                  <p><b>AND THE FIRE-KILL CLAUSE PASSES THE KILLER HERE, WHICH IS NOT AN
     *                  EXCEPTION TO THAT RULE.</b> When a fire blow kills a mob that was never
     *                  scorched ({@code BukkitCombatant}'s second clause), there IS no lighter --
     *                  nothing ever lit it -- so there is no competing candidate, and <b>the killing
     *                  blow IS the fire</b>. Crediting the dealer there is the same rule reaching its
     *                  only answer, not a second rule sitting beside it. Stated because the two
     *                  sentences look contradictory side by side, and a reader who finds them
     *                  without this note will take one of them for a bug.
     * @param victimId  the mob that died, excluded from its own blast
     * @param depth     which link of the cascade this is. {@code 1} for an ignition a PLAYER caused;
     *                  otherwise the depth of the blast that scorched or killed this mob, plus one.
     *                  At {@link #MAX_CHAIN_DEPTH} the blast still fires and still damages -- it
     *                  simply carries {@link HitAccrual#terminal()} and propagates nothing.
     */
    public static void detonate(CombatWorld world, Vec3 at, UUID applierId, UUID victimId, int depth) {
        // The whole limit, in one expression. >= rather than > because depth is 1-based: the FOURTH
        // explosion is the terminal one, so depth 4 must already be inert.
        HitAccrual accrual = depth >= MAX_CHAIN_DEPTH
                ? HitAccrual.terminal()
                : HitAccrual.chained(depth);
        world.schedule(at, DELAY_TICKS, () -> {
            world.present(at, VISUAL_ID);
            for (Combatant c : world.combatantsNear(at, RADIUS)) {
                // The corpse, and the exclusion is here for a PLAYER-VISIBLE reason rather than as a
                // shrug. A dying mob stays a LivingEntity while its death animation plays, so it can
                // still be found here and can still be tracked. A tracked corpse taking the blast
                // emits a HealthChange, and a HealthChange RENDERS A FLOATING DAMAGE NUMBER OVER A
                // CORPSE. That is the cost, and it is the reason this line exists.
                //
                // AND THE TIMING IS NOW MARGINAL, WHICH STRENGTHENS THIS RATHER THAN WEAKENING IT.
                // At the old 10-tick fuse the animation comfortably outlasted the blast, so the
                // corpse was reliably present. At 20 the two are the SAME ORDER -- roughly a full
                // death animation -- so whether the corpse is still there when the blast lands is a
                // race that server timing decides, not something this code can know. A guard whose
                // triggering case is a coin-flip is MORE load-bearing than one whose case is
                // certain: it fires sometimes, so its absence would be an intermittent artifact, and
                // an intermittent artifact is the kind nobody reproduces on demand.
                //
                // The exact animation length is server behaviour, not ours, so it stays a gate
                // observation and never a claim here. GATE-ignite.md's I1 records whether a number
                // ever appears over the corpse.
                if (c.id().equals(victimId)) continue;
                if (c.state().player()) continue;
                // DefenseRule.APPLIES, deliberately, and NOT the burn's BYPASSED. The burn's
                // exemption exists because it is a percent-of-max effect on a clock -- armour
                // blunting it would mean armour reducing a fraction of your own health. A blast is
                // a flat number dealt once, like every other discrete hit, so it goes through
                // Defense like every other discrete hit. See Scorch's armour section, which names
                // this as the BOUNDARY of that rule rather than an exception to it.
                //
                // THE BLAST RECRUITS -- it scorches its survivors -- UNTIL THE LAST LINK, which
                // passes terminal() and recruits nobody. See the class javadoc: this REVERSED an
                // earlier ruling, and the bound is what made the reversal safe.
                c.handle().applyDamage(DAMAGE, applierId,
                        CritState.NORMAL, DefenseRule.APPLIES, "fire", accrual);
            }
        });
    }
}
