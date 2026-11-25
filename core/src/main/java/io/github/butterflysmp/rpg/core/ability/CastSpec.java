package io.github.butterflysmp.rpg.core.ability;

import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;

import java.util.List;

/** How the ability reaches its target. */
public sealed interface CastSpec {
    record Self() implements CastSpec {}

    /**
     * THE THROWN ORB (YAML {@code type: orb}; Lapis Crystal's D2, 2026-11-23). A body thrown along the aim to
     * {@code range} blocks, stopping short of the first solid block, arriving in {@code flightTicks}, then resting
     * {@code lifeTicks} counted from arrival. ONE per caster: a new throw removes the standing one. {@code radius} is how
     * close a beam must pass to be stopped by it (D3). {@code CastExecutor.throwOrb} flies it; {@code Orbs} holds it.
     *
     * <p>ITS BODY, ONE OF TWO KINDS (YAML {@code body:}, {@code item} when absent; Ben's item 3, 2026-11-23, PR 2):
     * {@code item} names a non-saved item marker, driven by velocity, the body D2 shipped; {@code block}, when not null,
     * is a floating block display ({@link OrbBlock}), placed on the same path one step a tick (it has no physics). With a
     * block body {@code item} may be null.
     */
    record Orb(double range, int flightTicks, int lifeTicks, double radius, String item, OrbShot shot, OrbBlock block)
            implements CastSpec {
        public Orb {
            if (!(range > 0)) throw new IllegalArgumentException("orb range must be above 0, was " + range);
            if (flightTicks < 1) throw new IllegalArgumentException("orb flight_ticks must be 1 or more, was " + flightTicks);
            if (lifeTicks < 1) throw new IllegalArgumentException("orb life_ticks must be 1 or more, was " + lifeTicks);
            if (!(radius > 0)) throw new IllegalArgumentException("orb radius must be above 0, was " + radius);
            if (item != null && item.isBlank()) item = null;
            if (block == null && item == null) throw new IllegalArgumentException("orb item is required for an item body");
        }

        /** An item-bodied orb with a shot (D3's shape, before PR 2). */
        public Orb(double range, int flightTicks, int lifeTicks, double radius, String item, OrbShot shot) {
            this(range, flightTicks, lifeTicks, radius, item, shot, null);
        }

        /** An orb with no shot: it stands, and a beam into it does nothing (D2's shape). */
        public Orb(double range, int flightTicks, int lifeTicks, double radius, String item) {
            this(range, flightTicks, lifeTicks, radius, item, null, null);
        }
    }

    /**
     * A BLOCK-DISPLAY ORB BODY (YAML the orb's {@code body: block} with its {@code block:} section; Ben: "a Floating Lapis
     * block with glowing", 2026-11-23). {@code block} is a block id ({@code lapis_block}; the paper loader refuses one that
     * is not a block); {@code scale} its edge in blocks, centred on the body's position; {@code glow} the glow outline's
     * colour as {@code #RRGGBB}, OPTIONAL (null: no glow; Ben, 2026-11-23: "Remove the glow, it is excessive").
     *
     * <p>ITS LOOK AT REST (Ben, 2026-11-23: "Make it float up and down and spin slightly similar to a dropped item"; "Add a
     * few particles around the lapis crystal"). The MOTION IS THE BODY'S LOOK ONLY (the seat's ruling): the orb's rest
     * point does not move; every {@code beatTicks} while it rests the display's TRANSFORMATION is set to {@link #pose} (a
     * bob of {@code bobBlocks} over {@code bobPeriodTicks}, one turn per {@code turnTicks}), interpolated over the beat, and
     * {@code particles} (a visual id, optional) is presented at the rest point. 0 is "none" for each figure; a body with
     * any of them needs a beat above 0. All of them SEAT PLACEHOLDERS in the content.
     */
    record OrbBlock(String block, double scale, String glow, double bobBlocks, int bobPeriodTicks, int turnTicks,
                    int beatTicks, String particles) {
        public OrbBlock {
            if (block == null || block.isBlank()) throw new IllegalArgumentException("orb block is required for a block body");
            if (!(scale > 0) || Double.isInfinite(scale)) throw new IllegalArgumentException("orb block scale must be above 0, was " + scale);
            if (glow != null && glow.isBlank()) glow = null;
            if (glow != null && !glow.matches("#[0-9a-fA-F]{6}")) {
                throw new IllegalArgumentException("orb block glow must be a colour #RRGGBB, was " + glow);
            }
            if (!(bobBlocks >= 0) || Double.isInfinite(bobBlocks)) throw new IllegalArgumentException("orb block bob_blocks must be 0 or more, was " + bobBlocks);
            if (bobPeriodTicks < 0 || turnTicks < 0 || beatTicks < 0) {
                throw new IllegalArgumentException("orb block bob_period_ticks, turn_ticks and beat_ticks must be 0 or more");
            }
            if (bobBlocks > 0 && bobPeriodTicks == 0) throw new IllegalArgumentException("orb block bob_blocks needs a bob_period_ticks above 0");
            if (particles != null && particles.isBlank()) particles = null;
            if ((bobBlocks > 0 || turnTicks > 0 || particles != null) && beatTicks == 0) {
                throw new IllegalArgumentException("orb block motion or particles need a beat_ticks above 0");
            }
        }

        /** A still block: no motion, no particles (PR 2's shape). */
        public OrbBlock(String block, double scale, String glow) {
            this(block, scale, glow, 0, 0, 0, 0, null);
        }

        /** The glow colour as 0xRRGGBB; -1 for no glow. */
        public int glowRgb() {
            return glow == null ? -1 : Integer.parseInt(glow.substring(1), 16);
        }

        /** Whether the body has a look to keep up at rest: a beat. */
        public boolean animated() {
            return beatTicks > 0;
        }

        /**
         * The display's transformation {@code t} ticks into its rest: a translation and a turn (radians about the
         * vertical). The block model spans 0 to 1 from the display's position (its corner), and the display composes
         * {@code translation . rotate(yaw) . scale} (READ, {@code Transformation.compose}), so the model's centre
         * {@code (1/2, 1/2, 1/2)} lands at {@code translation + rotY(yaw)(scale/2, scale/2, scale/2)}. The translation is
         * chosen so that point is {@code (0, bob, 0)}: the block turns about its own centre and bobs straight up and down
         * over the display's position, the rest point. At {@code t = 0} it is the still centring, {@code -scale/2} each way.
         */
        public Pose pose(long t) {
            double bob = bobPeriodTicks == 0 ? 0 : bobBlocks * Math.sin(2 * Math.PI * t / bobPeriodTicks);
            double yaw = turnTicks == 0 ? 0 : 2 * Math.PI * (t % turnTicks) / turnTicks;
            double h = scale / 2;
            // rotY(yaw) of (h, h, h): x' = h cos + h sin, z' = -h sin + h cos (the JOML rotateY convention).
            double cx = h * Math.cos(yaw) + h * Math.sin(yaw);
            double cz = -h * Math.sin(yaw) + h * Math.cos(yaw);
            return new Pose(-cx, bob - h, -cz, yaw);
        }

        /** A display transformation: its translation, and its turn in radians about the vertical. */
        public record Pose(double tx, double ty, double tz, double yaw) {}
    }

    /**
     * WHAT AN ORB DOES WHEN HIS OWN BEAM MEETS IT (Lapis Crystal's D3, 2026-11-23; YAML the orb cast's {@code shot:}
     * block): it strikes every mob within {@code radius} of itself that it has a clear line to, each for
     * {@code percentByLevel} (by the throwing enchant's level) of the beam's AUTHORED damage, read from the beam's own
     * ability when it meets the orb, in {@code element}; {@code visual} (optional) is presented once at the orb.
     */
    record OrbShot(double radius, java.util.List<Double> percentByLevel, String element, String visual) {
        public OrbShot {
            if (!(radius > 0)) throw new IllegalArgumentException("orb shot radius must be above 0, was " + radius);
            if (percentByLevel == null || percentByLevel.isEmpty()) {
                throw new IllegalArgumentException("orb shot percent_by_level is required: one per level");
            }
            for (int i = 0; i < percentByLevel.size(); i++) {
                Double p = percentByLevel.get(i);
                if (p == null || !(p > 0) || p.isInfinite()) {
                    throw new IllegalArgumentException("orb shot percent_by_level at level " + (i + 1) + " must be above 0, was " + p);
                }
            }
            percentByLevel = java.util.List.copyOf(percentByLevel);
            if (element == null || element.isBlank()) throw new IllegalArgumentException("orb shot element is required");
            if (visual != null && visual.isBlank()) visual = null;
        }

        /** The percentage at {@code level} (1-based), clamped into the authored levels. */
        public double percentAt(int level) {
            return percentByLevel.get(Math.max(1, Math.min(level, percentByLevel.size())) - 1);
        }

        /** The damage one shot deals off a beam whose authored damage is {@code beamAmount}: amount x percent / 100. */
        public double amountFor(double beamAmount, int level) {
            return beamAmount * percentAt(level) / 100.0;
        }
    }
    record Melee(double reach, double arcDegrees) implements CastSpec {}
    /**
     * VANILLA'S OWN SHOT (YAML {@code type: vanilla}, 2026-11-22): the item's bow or crossbow shot, which vanilla draws,
     * loads and shoots. We NEVER cast it -- it is bound only to the {@code shot} trigger, which no input names -- and its
     * on_hit lands where vanilla's arrow lands ({@code CastExecutor.landVanillaShot}). {@code WeaponDefinition} refuses it
     * anywhere else.
     */
    record Vanilla() implements CastSpec {}

    /**
     * THE SUMMON CAST (the Summoner slice, 3a; D2 and D6): {@code type: summon} with {@code summon: <id>}, naming a file in
     * {@code content/summons/}. <b>The engine never executes one.</b> A summon cast is decided by the summon service
     * BEFORE the cast is charged, because a refusal at the cap (D6) costs no mana and starts no cooldown; the engine's
     * arm for it throws, so a summon cast that reaches the engine is a wiring defect, named.
     */
    record Summon(String summonId) implements CastSpec {
        public Summon {
            if (summonId == null || summonId.isBlank()) {
                throw new IllegalArgumentException("a summon cast must name its summon ('summon: <id>')");
            }
        }
    }
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
    record Ray(double range, String beam, boolean pierce, double tolerance) implements CastSpec {

        /**
         * <b>{@code pierce} AND {@code tolerance}, 2026-11-21 (the seat's brief, the Knell Beam, rulings 1 and 2).</b>
         * Both default OFF, and a ray with both off walks exactly as every ray did before them (on
         * {@code CombatWorld.castRay}, stopping at the first body or block it meets).
         *
         * <ul>
         *   <li>{@code pierce}: the walk goes on past a body, to the range or the first block. Every body it strikes
         *       is struck ONCE (a set of struck ids carried across chunk-column hops) and takes the same {@code on_hit},
         *       nearest first.</li>
         *   <li>{@code tolerance}, in BLOCKS: a body is struck when the line meets its box grown by this much on every
         *       side, the Knell mob's geometry ({@code BeamLine.meets}). 0 is the exact box, as the default ray tests.
         *       A margin hit behind a block is refused: the line is clipped at the first block, and a body struck only
         *       in its margin needs a clear sight line from the line to it.</li>
         * </ul>
         */
        public Ray {
            if (!(tolerance >= 0 && Double.isFinite(tolerance))) {
                throw new IllegalArgumentException("a ray's tolerance must be 0 or more blocks, was " + tolerance);
            }
        }

        /** A ray that stops at the first body, tested on its exact box: every ray before the Knell Beam. */
        public Ray(double range, String beam) {
            this(range, beam, false, 0.0);
        }

        /**
         * A ray that draws nothing between muzzle and impact -- what {@code solar_lance} is, and
         * what every ray in this repo was before a beam could be named.
         */
        public Ray(double range) {
            this(range, null);
        }

        /** Whether this ray walks the sweep (pierce or tolerance) rather than the plain first-hit trace. */
        public boolean sweeps() {
            return pierce || tolerance > 0;
        }
    }
    /**
     * A body arcing under gravity until it hits something. {@code trail} is a visual id presented
     * at the projectile's live position once per tick of flight, or null for a bare projectile
     * that leaves nothing.
     *
     * <p>{@code trail} is OPTIONAL and defaults to absent, which is what makes it not a change to
     * the weapons that do not ask for one: {@code hunters_bow} (deleted 2026-11) and {@code ember_staff} (deleted 2026-11) specify no
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
                      Homing homing, String body, Spread spread) implements CastSpec {

        /**
         * <b>{@code item} AND {@code body} ARE MUTUALLY EXCLUSIVE, AND IT IS ENFORCED HERE RATHER
         * THAN TRUSTED.</b> A bolt has one body. {@code item} renders a dropped ITEM driven along
         * the flight; {@code body} renders a real projectile ENTITY of the named kind -- an ARROW,
         * oriented along its travel, or a FIREBALL (LEGACY-B) -- with every interaction it would
         * normally have switched off. Authoring both is a content mistake with
         * no sensible resolution -- whichever the flight picked, the other field would silently do
         * nothing.
         *
         * <p><b>{@code body} IS THE TAIL FIELD EVEN THOUGH {@code homing} IS OLDER</b>, because the
         * ladder's rule is that each convenience constructor drops the TAIL and never a middle
         * field. Putting {@code body} beside {@code item}, where it reads more naturally, would
         * have renumbered every existing call site and broken the property that a reader counting
         * arguments never has to work out which one was omitted.
         *
         * <p><b>The values are judged in {@code core}; PRESENCE is judged in the schema.</b> Same
         * split {@link Homing} uses -- {@code AbilitySchema} asks a question about a file, this asks
         * whether the combination means anything, and a unit test reaches this without a server.
         */
        public Projectile {
            if (item != null && body != null) {
                throw new IllegalArgumentException(
                        "a projectile cast declares ONE body: 'item: " + item + "' and 'body: "
                                + body + "' are mutually exclusive");
            }
        }

        /**
         * A projectile that fires ONE body down the aim vector -- every projectile in this repo
         * before the Dragon's Breath, and the shape any weapon keeps by simply not authoring a
         * {@code spread} block.
         *
         * <p>THE NEW FIELD WENT ON THE END, WHICH IS THE LADDER'S OWN RULE rather than a
         * preference, and it is the third time this record has applied it. Each convenience
         * constructor drops the TAIL, never a middle field, so a reader counting arguments never
         * has to work out which one was omitted. {@code spread} reads more naturally beside
         * {@code homing} -- they are the two blocks that shape a flight -- and putting it there
         * would have renumbered every existing call site.
         */
        public Projectile(double speed, double gravity, int maxLifetimeTicks, String trail, String item,
                          Homing homing, String body) {
            this(speed, gravity, maxLifetimeTicks, trail, item, homing, body, null);
        }

        /**
         * A projectile with a homing block and an ITEM body -- the canonical form until the arrow
         * body arrived, and what every existing call site still constructs.
         */
        public Projectile(double speed, double gravity, int maxLifetimeTicks, String trail, String item,
                          Homing homing) {
            this(speed, gravity, maxLifetimeTicks, trail, item, homing, null);
        }

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
     * A projectile that leaves the muzzle as SEVERAL bodies at once, in a ring around the aim.
     * Null on a {@link Projectile} means one body down the aim vector, which is every projectile
     * this repo shipped before the Dragon's Breath.
     *
     * <p>{@code count} is the TOTAL number of payloads. One flies down the aim vector; the
     * remaining {@code count - 1} are spaced evenly around a ring {@code angleDegrees} off-axis,
     * in the SHOOTER'S VIEW PLANE. <b>The hexagon falls out of {@code 7 - 1 = 6}</b> rather than
     * being a second authored number that could disagree with the first.
     *
     * <h2>WHY A FIELD HERE AND NOT A SEVENTH {@link CastSpec} MEMBER</h2>
     *
     * <p>{@link Volley} is the nearest existing thing and is not this: it repeats a cast over TIME
     * and <i>"re-reads its caster before every shot -- fresh aim, fresh crit roll, fresh price"</i>,
     * which is a burst the player steers. <b>A spread is the opposite: one decision, delivered
     * seven ways, in one frame.</b>
     *
     * <p>That record's own javadoc names the trap this field avoids -- it asks the next person
     * wanting a new shape to reuse rather than propose <i>"a seventh member of a sealed interface
     * that did not need one."</i> <b>A field composes; a member does not.</b> As a field it sits
     * beside {@code speed}, {@code gravity}, {@code trail}, {@code body} and the absent
     * {@code homing}, so <b>a spread of HOMING arrows is expressible with no second mechanism</b>,
     * and {@link CastSpec#minimumCooldownTicks}'s exhaustive switch needs no new arm.
     *
     * <h2>WHAT IS REFUSED, AND WHY EACH ONE IS SILENT RATHER THAN LOUD</h2>
     *
     * <p><b>Every one of these produces a spread block that resolves perfectly and spreads
     * nothing</b> -- {@link Homing}'s own doctrine, and the failure mode {@code CLAUDE.md} names:
     * a file advertising a mechanism the player never receives, with nothing going red.
     *
     * <pre>
     * count            must be &gt;= 2
     *                    &lt;= 1   a spread of one IS a projectile. It is not wrong, it is a
     *                           block that does nothing -- so the weapon should omit it and say
     *                           so, rather than author a ring with no ring in it.
     *
     * angleDegrees     must be in (0, 90)
     *                    &lt;= 0   EVERY body on the aim vector: N exactly-overlapping copies of
     *                           one shot. The tooltip would say `x 7` and the player would see
     *                           one arrow.
     *                    &gt;= 90  the ring is perpendicular to the aim (90) or BEHIND the
     *                           shooter (&gt; 90). At exactly 90 the forward component is zero and
     *                           the ring bodies travel sideways forever.
     * </pre>
     *
     * <p><b>The tests are written as positive assertions, which also refuses NaN for free</b> --
     * every comparison against NaN is false, so {@code angle_degrees: nan} fails
     * {@code angleDegrees > 0} and is named rather than firing a fan in an undefined direction.
     *
     * <p><b>The loader's half is PRESENCE, not range</b>, exactly as {@link Homing} splits it:
     * {@code AbilitySchema} requires both keys because this record <i>holds no defaults</i>, and
     * this constructor judges the values because a value rule belongs where a unit test reaches it
     * without a server.
     *
     * <p><b>NO BUNDLED CONTENT REACHES ANY OF THESE ARMS, AND IT IS SAID HERE SO THE NEXT READER
     * DOES NOT ASSUME PRODUCTION COVERS THEM.</b> {@code dragons_breath} authors two sane numbers and
     * every other weapon authors no spread block at all. <b>The only exercise these arms get is
     * {@code CastSpecSpreadTest}, which CAUSES each condition</b>, and {@code AbilityLoaderTest},
     * which causes them through a real YAML walk and reads the named, skipped file back out of the
     * log.
     */
    record Spread(int count, double angleDegrees, double rotationDegrees) {

        /**
         * The shape every file had before {@code rotation_degrees} (2026-11-22): the ring unrotated, its first body at the
         * shooter's RIGHT. Every caller written before the option keeps this constructor and its pattern.
         */
        public Spread(int count, double angleDegrees) {
            this(count, angleDegrees, 0.0);
        }

        public Spread {
            if (count < 2) {
                throw new IllegalArgumentException("spread count must be >= 2 -- got " + count
                        + ". A spread of one is a plain projectile: omit the spread block rather"
                        + " than authoring a ring with nothing in it.");
            }
            if (!(angleDegrees > 0)) {
                throw new IllegalArgumentException("spread angle_degrees must be > 0 -- got "
                        + angleDegrees + ". At or below zero every body flies down the aim vector,"
                        + " so the cast delivers " + count + " exactly-overlapping copies of one"
                        + " shot while the tooltip advertises a spread.");
            }
            if (!(angleDegrees < 90)) {
                throw new IllegalArgumentException("spread angle_degrees must be < 90 -- got "
                        + angleDegrees + ". At 90 the ring is perpendicular to the aim and its"
                        + " bodies travel sideways forever; past 90 they fly behind the shooter.");
            }
            // THE RING'S ROTATION (the seat's ruling 1, 2026-11-22): degrees counter-clockwise from the shooter's RIGHT
            // toward his UP, as phi already runs in SpreadPattern; 0 is today's ring. Any finite value (90 turns a ring of
            // three to point up, the Breacher's); NaN or infinite would place every ring body at an undefined angle.
            if (!Double.isFinite(rotationDegrees)) {
                throw new IllegalArgumentException("spread rotation_degrees must be finite -- got " + rotationDegrees);
            }
        }

        /** How many bodies sit on the ring: everything but the one down the aim vector. */
        public int ringCount() {
            return count - 1;
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
        /**
         * Recall: the reverse of facing, ALWAYS -- a straight backpedal, ignoring WASD. Also {@code recall_updraft}'s
         * leap (section 7.5): a small {@code speed} and a large {@code lift} make it "up, and a little back".
         */
        REVERSE_FACING,
        /**
         * Sunder (Ben, Q1: <i>"Always straight ahead"</i>): along facing, ALWAYS, ignoring WASD -- {@code
         * REVERSE_FACING} without the negation. Horizontal, so pitch does nothing (section 8.1).
         */
        FACING
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
     *
     * {@code safeLanding} (ruling 31, section 7.6): the caster takes no fall damage from the first landing after
     * this dash. False for every dash before Recall's leap.
     *
     * {@code onLand} (Sunder, PLAN-melee-class.md section 8.2, S1): UNTARGETED effects detonated at the caster's
     * LIVE feet when the leap comes down, however it comes down ({@code Touchdown}). Empty for every dash before
     * Sunder, whose {@code on_hit} still fires at take-off. A non-empty {@code onLand} ALSO makes the landing free
     * of fall damage, whatever {@code safeLanding} says: the seat's ruling 5 (Q3 <i>"None"</i>, F-d) is ONE mark that
     * both detonates the slam and spends the fall immunity, so there is no slam without the immunity to express.
     */
    record Dash(double distance, double speed, double lift, DashDirection direction, boolean safeLanding,
                List<EffectSpec> onLand) implements CastSpec {

        public Dash {
            onLand = List.copyOf(onLand);
        }

        /** A dash with no slam -- every dash before Sunder. */
        public Dash(double distance, double speed, double lift, DashDirection direction, boolean safeLanding) {
            this(distance, speed, lift, direction, safeLanding, List.of());
        }

        /** A dash that leaves fall damage alone -- every dash before section 7.5. */
        public Dash(double distance, double speed, double lift, DashDirection direction) {
            this(distance, speed, lift, direction, false);
        }

        /** Whether this leap holds a landing mark: for its slam, or for {@code safeLanding} alone. */
        public boolean armsLanding() {
            return safeLanding || !onLand.isEmpty();
        }
    }

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
    record Volley(int windupTicks, int shots, int intervalTicks, CastSpec of, boolean lockedAim, Warning warning)
            implements CastSpec {

        /**
         * <b>THE WARNING LINE, 2026-11-21 (the seat's brief, the Knell Beam, ruling 4).</b> A beam visual drawn along the
         * LOCKED line, on each windup tick in {@code flashAt} (0 is the cast frame), the way the Knell mob flashes its
         * line before it fires. Null for a volley that draws nothing before its shot.
         */
        public record Warning(String beam, List<Integer> flashAt) {
            public Warning {
                if (beam == null || beam.isBlank()) {
                    throw new IllegalArgumentException("a volley warning needs a 'beam:' visual id to draw");
                }
                if (flashAt == null || flashAt.isEmpty()) {
                    throw new IllegalArgumentException("a volley warning needs 'flash_at:', the windup ticks it is"
                            + " drawn on; with none it draws nothing");
                }
                flashAt = List.copyOf(flashAt);
            }
        }

        /** A volley that re-aims every shot and draws no warning: every volley before the Knell Beam. */
        public Volley(int windupTicks, int shots, int intervalTicks, CastSpec of) {
            this(windupTicks, shots, intervalTicks, of, false, null);
        }

        /**
         * <p><b>{@code lockedAim} (ruling 3):</b> off, each shot re-reads where the caster looks NOW (the burst a player
         * steers). On, the origin and direction are captured AT THE CAST and every shot fires down that line, wherever
         * the caster has moved or turned: values, not a handle, so nothing about an entity is held across ticks.
         *
         * <p><b>{@code warning} (ruling 4) is REFUSED without {@code lockedAim}</b>: a line drawn along an aim the shot
         * will not use is a lie. It is refused unless {@code of} is a ray (a warning line has the ray's range for its
         * length), and every flash tick must fall inside the windup (a flash at or after the shot warns of nothing).
         */
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
            if (warning != null) {
                if (!lockedAim) {
                    throw new IllegalArgumentException("a volley 'warning:' needs 'locked_aim: true': a line drawn"
                            + " along an aim the shot will not use is a lie");
                }
                if (!(of instanceof Ray)) {
                    throw new IllegalArgumentException("a volley 'warning:' needs its 'of:' to be a ray: the line is"
                            + " drawn to the ray's range");
                }
                for (int t : warning.flashAt()) {
                    if (t < 0 || t >= windupTicks) {
                        throw new IllegalArgumentException("a volley warning flash_at tick must be in [0, windup_ticks "
                                + windupTicks + "), was " + t + ": a flash at or after the shot warns of nothing");
                    }
                }
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
     * A SPELL THAT SWEEPS A CONE IN FRONT OF THE CASTER (the seat's brief of 2026-11-22, the Living Lantern, ruling 1):
     * every body inside a cone of FULL angle {@code arcDegrees} about the aim, out to {@code range} blocks, with a clear
     * line of sight from the eye, takes the {@code on_hit} once.
     *
     * <ul>
     *   <li>{@code range}, in BLOCKS, from the eye to a body's SIGHT POINT ({@code CombatantSnapshot.sightPoint}), the
     *       point the line of sight is traced to (ruling 3).</li>
     *   <li>{@code arcDegrees}, the FULL angle: 60 is 30 either side, up and down as well as left and right. The angle
     *       test is the melee swing's, {@code Aim.withinArc}, reused (one copy). In (0, 180).</li>
     *   <li>{@code travelTicks}: 0 strikes every body on the cast's own tick. N &gt; 0 sends a FRONT out from the eye,
     *       {@code range / N} blocks a tick, from the origin and direction captured AT THE CLICK, as values (ruling 2); a
     *       body is struck on the tick the front first reaches it, once per cast, nearest first within a tick. A body
     *       that walks into the cone BEHIND the front is not struck.</li>
     *   <li>{@code front}, a visual id drawn each tick as a ring at the front's distance, the cone's width there, and its
     *       centre; or null to draw nothing.</li>
     * </ul>
     *
     * <p><b>IT IS NOT THE MELEE CAST, AND THAT IS WHY IT IS A MEMBER RATHER THAN MELEE'S FIELDS.</b> A melee cast picks
     * ONE nearest body, and a weapon whose melee on_hit is a {@code weapon_damage} is a basic attack: vanilla drives it,
     * attack speed scales its cooldown, a charge scales its hit, and the sweep rider splashes it. A cone strikes EVERY
     * body in it and drives none of that: nothing in {@code BasicMelee}, the attack-speed scaling or the sweep reads a
     * cone (each keys on {@code Melee} or on a {@code weapon_damage} on_hit). It reuses the swing's ANGLE TEST, not the
     * swing.
     *
     * <p><b>WHO IT STRIKES (ruling 4, READ 2026-11-22, and KEPT as every spell has it):</b> every live body in it but
     * the caster. ANOTHER PLAYER in the cone IS struck: {@code EffectApplier}'s {@code Damage} arm filters no player and
     * the ability path has no PvP gate, and no {@code PLAYERHIT} line is written for a player target
     * ({@code EffectApplier.trace}). The CASTER never is: a spell's selector leaves him out by id (a ray by
     * {@code castRay}'s ignore id, the swing by {@code casterId}), and the cone does the same in
     * {@code ConeShell.reached}; nothing in the {@code Damage} arm itself would spare him.
     *
     * <p>No bundled content reaches the refusals below; {@code CastSpecConeTest} causes each.
     */
    record Cone(double range, double arcDegrees, int travelTicks, String front) implements CastSpec {
        public Cone {
            if (!(range > 0 && Double.isFinite(range))) {
                throw new IllegalArgumentException("a cone's range must be more than 0 blocks, was " + range);
            }
            if (!(arcDegrees > 0 && arcDegrees < 180)) {
                throw new IllegalArgumentException("a cone's arc_degrees must be in (0, 180), was " + arcDegrees
                        + ": at 0 it strikes nothing, and at 180 or more it reaches beside and behind the caster and"
                        + " has no front ring to draw");
            }
            if (travelTicks < 0) {
                throw new IllegalArgumentException("a cone's travel_ticks must be 0 (instant) or more, was "
                        + travelTicks);
            }
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
            case Vanilla ignored -> 0;   // never cast by us; vanilla's draw and load are the cadence
            case Ray ignored -> 0;
            case Projectile ignored -> 0;
            case Dash ignored -> 0;
            // A cone in flight when the next is cast is two casts, each with its own struck set: nothing doubles up
            // within one, and two overlapping cones are two presses the cooldown already priced. No floor.
            case Cone ignored -> 0;
            // A second throw REPLACES the standing orb (one per caster), so nothing in flight can double up. No floor.
            case Orb ignored -> 0;
            // A recast REPLACES its summon (D6: one per weapon), so nothing in flight can double up. No floor.
            case Summon ignored -> 0;
        };
    }
}
