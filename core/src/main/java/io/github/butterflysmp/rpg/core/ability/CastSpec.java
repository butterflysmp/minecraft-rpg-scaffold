package io.github.butterflysmp.rpg.core.ability;

/** How the ability reaches its target. */
public sealed interface CastSpec {
    record Self() implements CastSpec {}
    record Melee(double reach, double arcDegrees) implements CastSpec {}
    /**
     * A straight line to the first thing in the way. {@code beam} is a visual id drawn ALONG each
     * chunk-column segment as the ray walks it, or null for a bare ray that draws nothing between
     * the muzzle and the impact.
     *
     * <p>{@code beam} is OPTIONAL and defaults to absent, which is what makes it not a change to
     * {@code solar_lance}: it specifies no beam, gets null, and behaves byte-identically to before
     * this field existed. Exactly the ladder {@link Projectile} uses for {@code trail} and
     * {@code item}, and for the same reason -- the convenience constructor drops the TAIL, so a
     * reader counting arguments never has to work out which field was omitted.
     *
     * <p><b>A BEAM IS A SEGMENT, AND THAT IS WHY IT IS NOT AN on_hit VISUAL.</b> The nearest
     * existing thing, {@code EffectSpec.Visual}, presents at a POINT -- it would draw a puff at one
     * end of the shot instead of a line down it. Same shape as the {@code throw_embers} trap the
     * Flint Staff hit: the closest available mechanism has the wrong GEOMETRY, and reaching for it
     * produces something that looks like a bug rather than failing loudly.
     */
    record Ray(double range, String beam) implements CastSpec {

        /**
         * A ray that draws nothing between muzzle and impact -- what {@code solar_lance} is, and
         * what every ray in this repo was before a beam could be named.
         */
        public Ray(double range) {
            this(range, null);
        }
    }
    /**
     * A body arcing under gravity until it hits something. {@code trail} is a visual id presented
     * at the projectile's live position once per tick of flight, or null for a bare projectile
     * that leaves nothing.
     *
     * <p>{@code trail} is OPTIONAL and defaults to absent, which is what makes it not a change to
     * the weapons that do not ask for one: {@code hunters_bow} and {@code ember_staff} specify no
     * trail, get null, and behave byte-identically to before this field existed. The field is here
     * because {@link io.github.butterflysmp.rpg.core.combat.ProjectileFlight} has ALWAYS presented
     * a trail every step -- there was simply no way for the schema to produce anything but null,
     * and the call site's {@code // a bare projectile leaves no trail} read as a decision when it
     * was the only value available.
     *
     * <p>{@code item} is a material id RENDERED AS THE PROJECTILE'S BODY -- a real item entity
     * driven to the positions this flight computes, un-pickup-able, removed when the bolt resolves.
     * Also optional, and absent for the same two dev weapons.
     *
     * <p>The two are independent on purpose. A trail without a body is what the Flint Staff shipped
     * as one slice earlier; a body without a trail is a silent thrown rock. Neither implies the
     * other, so neither defaults from the other.
     */
    record Projectile(double speed, double gravity, int maxLifetimeTicks, String trail, String item,
                      Homing homing) implements CastSpec {

        /**
         * A projectile that flies where it was aimed -- every projectile in this repo before the
         * Dragon's Plume, and the shape any weapon keeps by simply not authoring a homing block.
         *
         * <p>THE NEW FIELD WENT ON THE END, WHICH IS THE LADDER'S OWN RULE rather than a
         * preference: each convenience constructor drops the TAIL, never a middle field, so a
         * reader counting arguments never has to work out which one was omitted. That is also why
         * this rung exists at all -- the five-argument form was the canonical constructor until
         * {@code homing} arrived, and it keeps working unchanged.
         */
        public Projectile(double speed, double gravity, int maxLifetimeTicks, String trail, String item) {
            this(speed, gravity, maxLifetimeTicks, trail, item, null);
        }

        /**
         * A projectile with a trail but NO RENDERED BODY -- what the Flint Staff was between the
         * trail landing and the body landing, and what any projectile that wants particles without
         * an entity still is.
         */
        public Projectile(double speed, double gravity, int maxLifetimeTicks, String trail) {
            this(speed, gravity, maxLifetimeTicks, trail, null, null);
        }

        /**
         * A projectile with neither -- every call site that predates both fields, and both dev
         * weapons. The same optional-argument ladder {@code AbilityDefinition} uses for its
         * authored description: each convenience constructor drops the TAIL, never a middle field,
         * so a reader counting arguments never has to work out which one was omitted.
         */
        public Projectile(double speed, double gravity, int maxLifetimeTicks) {
            this(speed, gravity, maxLifetimeTicks, null, null, null);
        }
    }

    /**
     * A projectile that STEERS toward a mob in flight. Null on a {@link Projectile} means the body
     * flies where it was aimed, which is every projectile this repo shipped before the Dragon's
     * Plume.
     *
     * <p>{@code lerp} is how far the flight turns toward its target each tick -- the direction is
     * blended {@code lerp} of the way from where the bolt is going to where the target is, then
     * renormalised. {@code activationBlocks} is how far from the SPAWN POINT the bolt flies
     * ballistically before it starts steering at all. {@code searchRadius} is how far it looks for
     * something to chase, re-chosen every tick.
     *
     * <h2>THE THREE NUMBERS ARE ANOTHER GAME'S, AND THIS TYPE DOES NOT JUDGE THEM</h2>
     *
     * <p>{@code PLAN-dragons-plume.md} §5 carries them as <b>INHERITED AND UNJUDGED</b> with a gate
     * row each -- 0.65, 15 blocks and 10 blocks, tuned against a weapon that fired an instant fan of
     * three rather than a charged release of five. <b>Nothing here is a ruling about their values</b>,
     * and the record deliberately holds no defaults: a caller states all three or authors no homing.
     *
     * <p><b>AND THE SEARCH RADIUS CROSSED A SHAPE CHANGE, WHICH THE NUMBER DOES NOT SHOW.</b> The
     * old repo's search was {@code getNearbyEntities(10, 10, 10)} -- a <b>20x20x20 BOX</b>, whose
     * corner is {@code sqrt(3) x 10 = 17.32} blocks out. {@link
     * io.github.butterflysmp.rpg.core.combat.CombatWorld#combatantsNear} is a <b>SPHERE</b>. So the
     * same {@code 10} is a strictly smaller search everywhere except along the axes, and
     * <b>a number carried across a shape change is not the same number.</b>
     *
     * <h2>THE VALIDATION THIS RECORD OWED IS NOW HERE, BECAUSE THE CASE BECAME REACHABLE</h2>
     *
     * <p>This section used to say the guard was <b>unreachable</b> -- <i>"no content authors a
     * homing block, because the YAML key does not exist yet"</i> -- and that it was OWED at the
     * schema slice. <b>That slice is this one.</b> {@code AbilitySchema} now reads a
     * {@code homing:} block, so every value below is authorable by anyone editing a yml, and the
     * compact constructor is no longer a dead catch.
     *
     * <p><b>NO BUNDLED CONTENT REACHES ANY OF THESE ARMS, AND IT IS SAID HERE SO THE NEXT READER
     * DOES NOT ASSUME PRODUCTION COVERS THEM.</b> Shipped weapons either author no homing block at
     * all or author three sane numbers. <b>The only exercise these arms get is
     * {@code CastSpecHomingTest}, which CAUSES each condition</b> rather than asserting the arm
     * exists -- and {@code AbilityLoaderTest} which causes them through a real YAML walk and reads
     * the named, skipped file back out of the log.
     *
     * <h2>WHAT IS REFUSED, AND WHY EACH ONE IS SILENT RATHER THAN LOUD</h2>
     *
     * <p><b>Every one of these produces a homing block that resolves perfectly and homes at
     * nothing</b>, which is the failure mode {@code CLAUDE.md} names: a tooltip -- or here, a
     * content file -- advertising a mechanism the player never receives, with nothing going red.
     *
     * <pre>
     * lerp             must be in (0, 1]
     *                    &lt;= 0   no turn at all, and NEGATIVE steers AWAY from the target
     *                    &gt;  1   blends PAST the target direction and mirrors; it oscillates
     *
     * activationBlocks must be &gt;= 0
     *                    &lt;  0   MEASURED IN {@code ProjectileFlight.steer}: the gate is
     *                           {@code distanceSquared(spawn) < activation * activation}, so the
     *                           SIGN IS SQUARED AWAY and -15 behaves exactly like +15. A number
     *                           that silently means its own opposite is worth refusing at the door.
     *
     * searchRadius     must be &gt; 0
     *                    &lt;= 0   {@code combatantsNear} finds nothing, forever
     * </pre>
     *
     * <p><b>The tests are written as positive assertions, which also refuses NaN for free</b> --
     * every comparison against NaN is false, so {@code lerp: nan} fails {@code lerp > 0} and is
     * named rather than flying a bolt whose direction is undefined.
     *
     * <p><b>The loader's half is PRESENCE, not range</b>, and the two halves are deliberately in
     * different places: {@code AbilitySchema} requires all three keys because this record
     * <i>holds no defaults</i> (above), and this constructor judges the values because a value rule
     * belongs where a unit test reaches it without a server.
     */
    record Homing(double lerp, double activationBlocks, double searchRadius) {

        public Homing {
            if (!(lerp > 0 && lerp <= 1)) {
                throw new IllegalArgumentException("homing lerp must be in (0, 1] -- got " + lerp
                        + ". At or below zero the bolt never turns (and a negative one turns AWAY"
                        + " from its target); above one it blends past the target direction and"
                        + " oscillates. Either way the homing block resolves and does nothing.");
            }
            if (!(activationBlocks >= 0)) {
                throw new IllegalArgumentException("homing activation_blocks must be >= 0 -- got "
                        + activationBlocks + ". ProjectileFlight.steer compares"
                        + " distanceSquared(spawn) against activation * activation, so a NEGATIVE"
                        + " activation is squared into its own positive twin and silently behaves"
                        + " like " + Math.abs(activationBlocks) + ".");
            }
            if (!(searchRadius > 0)) {
                throw new IllegalArgumentException("homing search_radius must be > 0 -- got "
                        + searchRadius + ". combatantsNear on a radius at or below zero finds"
                        + " nothing, forever, so the bolt never acquires a target.");
            }
        }
    }

    /**
     * Which way a dash sends the caster. The concrete direction VECTOR is still resolved
     * outside core (it reads a Bukkit player); this only names the RULE, so the same
     * resolver can produce either without core learning what a Player is.
     */
    enum DashDirection {
        /** Ember Step: the way the player is moving (WASD), or facing when stationary. */
        MOVEMENT_ELSE_FORWARD,
        /** Rekindle: the reverse of facing, ALWAYS -- a straight backpedal, ignoring WASD. */
        REVERSE_FACING
    }

    /**
     * Moves the caster. A one-shot velocity impulse of magnitude {@code speed} along a
     * direction resolved outside core (per {@code direction}), plus a small upward
     * {@code lift}, carried by physics. {@code distance} is the INTENDED length -- the
     * swept line used to find who the dash passes through, independent of the ballistic path
     * the caster actually takes. The concrete vector is deliberately NOT a field: core stays
     * Bukkit-free; only the {@code direction} MODE is declared here.
     *
     * {@code lift} exists because a purely horizontal ground impulse is eaten by Minecraft's
     * first-tick ground friction and barely travels; a touch of up arcs the caster off the
     * floor so the horizontal velocity carries. It is a tuning number -- dialed in the yml
     * against {@code speed} until a flat-ground dash reads the intended distance.
     */
    record Dash(double distance, double speed, double lift, DashDirection direction) implements CastSpec {}

    /**
     * A cast that runs ANOTHER cast {@code shots} times, {@code intervalTicks} apart, after a
     * {@code windupTicks} telegraph. The repeating primitive the schema did not have.
     *
     * <p>{@code EffectSpec.Area} was the nearest existing thing and is not this: it lingers at a
     * FIXED POINT applying effects to whoever is near it, so it can neither re-cast nor re-aim.
     * A volley re-reads its caster before every shot -- fresh aim, fresh crit roll, fresh price --
     * which is what makes it a burst the player steers rather than six copies of one decision.
     *
     * <p><b>{@code shots: 1} WITH A WIND-UP IS A TELEGRAPHED SINGLE CAST, AND IT COSTS NOTHING.</b>
     * A delayed strike with an audible commitment is something nothing else in this schema can
     * express, and it is simply one shot of this record. It is named here so that the first person
     * who wants a charged single shot reuses this rather than proposing a {@code Delayed} kind --
     * a seventh member of a sealed interface that did not need one.
     *
     * <p><b>{@code of} IS A WHITELIST AT THE LOADER, NOT AN OPEN SLOT.</b> The type permits any
     * {@code CastSpec} because a record cannot say otherwise; {@code AbilitySchema.innerCast}
     * admits only {@code ray} and {@code projectile} and refuses the rest by name. Nesting is
     * refused HERE as well, in the compact constructor, because a volley of volleys is a fork bomb
     * and that is a representability question rather than a content one.
     */
    record Volley(int windupTicks, int shots, int intervalTicks, CastSpec of) implements CastSpec {

        public Volley {
            if (windupTicks < 0) {
                throw new IllegalArgumentException("volley windup_ticks must be >= 0, got " + windupTicks
                        + "; 0 means the first shot fires on the cast frame");
            }
            if (shots < 1) {
                throw new IllegalArgumentException("volley shots must be >= 1, got " + shots
                        + "; a volley of nothing is a cast that does nothing");
            }
            // CombatWorld.scheduleOn refuses a delay below 1 because Paper clamps 0 up to 1, so an
            // interval of 0 would silently become 1 rather than firing every shot on one frame.
            // Refused here instead, where the number is authored and the message can say so.
            if (intervalTicks < 1) {
                throw new IllegalArgumentException("volley interval_ticks must be >= 1, got "
                        + intervalTicks + "; the scheduler cannot defer by less than a tick, so 0"
                        + " would quietly become 1");
            }
            if (of == null) {
                throw new IllegalArgumentException("volley has no 'of' cast to repeat");
            }
            if (of instanceof Volley) {
                throw new IllegalArgumentException("a volley cannot repeat a volley: each shot would"
                        + " start its own burst, and the count multiplies every interval");
            }
        }

        /**
         * The tick the LAST shot is fired on, counted from the cast frame.
         *
         * <p>Shot 1 lands at {@code windupTicks} and each subsequent shot one interval later, so
         * shot N lands at {@code windupTicks + (N - 1) * intervalTicks}. It is the tick of the last
         * FIRING, not of the last resolution: a ray's chunk-column walk outlives this by however
         * many planes it crosses, and that is deliberate -- see {@link CastSpec#minimumCooldownTicks}
         * for why the guard is drawn at the firing and not at the landing.
         */
        public int minimumCooldownTicks() {
            return windupTicks + (shots - 1) * intervalTicks;
        }
    }

    /**
     * The shortest cooldown this cast shape may be given, whatever its file authored.
     *
     * <p><b>THIS EXISTS BECAUSE A DERIVATION THAT LIVES IN A PLAN DOCUMENT IS NOT A MECHANISM.</b>
     * A volley of 6 shots, 2 ticks apart, after a 20-tick wind-up runs for 30 ticks. Authoring
     * {@code cooldown_ticks: 30} beside it reproduces an in-flight guard only while two
     * independently-computed durations stay exactly equal -- and nothing in the file states that
     * they must. Retune the wind-up to 22 and the volley ends at t=32 while the cooldown expires at
     * t=30: two volleys overlap, twelve shots interleave, and no test reddens, because no test knows
     * the two numbers were meant to agree. Deriving it from the record that owns all three inputs
     * makes an under-length cooldown UNREPRESENTABLE rather than merely unlikely.
     *
     * <p>{@code AbilityService} applies this as a FLOOR -- {@code max(authored, derived)} -- so a
     * longer authored cooldown is still honoured and only an impossible one is raised.
     * {@code ContentValidator} warns at load when the floor binds, because a value that was
     * overridden and a value that was never read look identical from the file.
     *
     * <p><b>THE GUARD IS DRAWN AT THE LAST FIRING, NOT THE LAST LANDING</b>, and the difference is
     * real: a 64-block ray walks up to four chunk columns after it is fired, so shot 6's beam is
     * still travelling when this expires. That is not an overlap -- rays within one volley already
     * overlap by design. The hazard is two VOLLEYS running at once, and the last firing is exactly
     * where that stops.
     *
     * <p>An EXHAUSTIVE pattern switch rather than a {@code default} method on this interface, and
     * deliberately: a default of 0 would let a seventh kind inherit "no floor" silently, which is
     * the admitted-by-default shape this project refuses. A new kind has to state its answer.
     *
     * <p><b>TRIGGER -- the residual hole, recorded rather than guarded.</b> {@code CooldownTracker}
     * keys on CASTER AND ABILITY, so this stops a caster re-pressing the SAME volley and nothing
     * else. The day a SECOND volley ability ships, a caster can hold one volley in flight and start
     * another. That is when an active-cast set keyed on the caster alone becomes necessary, and not
     * before: today the fixture is the only volley in the tree, so the case is content-unreachable
     * rather than mechanism-unreachable.
     */
    static int minimumCooldownTicks(CastSpec cast) {
        return switch (cast) {
            case Volley volley -> volley.minimumCooldownTicks();
            case Self ignored -> 0;
            case Melee ignored -> 0;
            case Ray ignored -> 0;
            case Projectile ignored -> 0;
            case Dash ignored -> 0;
        };
    }
}
