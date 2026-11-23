package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.build.Touchdown;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * The world, as far as core cares. This is the seam between game logic and
 * the server. In production it is backed by Paper. In tests it is a HashMap.
 *
 * schedule() is the ONLY way core defers work. It never touches a scheduler
 * directly, which is what lets the Paper adapter route the callback onto the
 * correct region thread (or, one day, a Folia region thread) without core
 * knowing threads exist.
 */
public interface CombatWorld {

    /**
     * Every combatant within {@code radius} of {@code center}, each paired with a snapshot
     * read here, on this thread. Only legal on the thread owning {@code center}'s region.
     */
    Collection<Combatant> combatantsNear(Vec3 center, double radius);

    /**
     * The combatant with this id, if it is here.
     *
     * Exists for Self casts: CastResult.Success carries only an immutable snapshot of the
     * caster, so acting on the caster needs its handle fetched again on the thread that
     * owns it. The alternative -- carrying a live handle across the region hop -- is the
     * bug this port was split to prevent.
     *
     * Only legal on the thread owning that combatant.
     */
    Optional<Combatant> combatant(UUID id);

    /**
     * The first combatant or block struck by the segment from {@code from} to
     * {@code to}, ignoring {@code ignoreId} (the caster, who is standing at the
     * origin of their own ray).
     *
     * One method serves both shapes that need it: a Ray cast walks the whole
     * range at once, and a Projectile casts the short segment it travelled this
     * tick. Empty means the segment reached {@code to} unobstructed.
     *
     * Like combatantsNear, this reads the world and is only legal on the thread
     * that owns the region containing the segment.
     */
    Optional<RayHit> castRay(Vec3 from, Vec3 to, UUID ignoreId);

    /**
     * Every live body near the segment from {@code from} to {@code to} (any whose box, grown by {@code reach}, could
     * meet it), the {@code ignoreId} body excluded, each with its box; and where the segment first meets a block with a
     * collision box. NOT judged: {@link RaySweep#struck} decides who is struck, in core, on the Knell mob's geometry.
     *
     * <p>Only a PIERCING or TOLERANT ray asks this (the seat's brief of 2026-11-21, the Knell Beam); every other ray
     * stays on {@link #castRay}. The same body filter as {@code castRay}: live living bodies, the caster excluded.
     *
     * <p>Like {@code castRay}, only legal on the thread that owns the region containing the segment. Abstract rather than
     * {@code default}, so an implementor that has not answered it does not compile.
     */
    RaySweep sweepRay(Vec3 from, Vec3 to, UUID ignoreId, double reach);

    /**
     * Whether the segment from {@code from} to {@code to} is clear of BLOCKS. Nothing else
     * stops it: an entity standing in the way does not.
     *
     * That is the whole reason this is not {@link #castRay}. castRay reports the first
     * block OR combatant it meets, so asking it for a sight line reads a mob standing
     * behind another mob as blocked. Melee wants block line of sight, the way vanilla
     * does -- you may hit a mob through a mob, but not through a wall.
     *
     * Like combatantsNear and castRay this reads the world, and is only legal on the
     * thread that owns the region containing the segment. Unlike castRay it can honour
     * that today: its callers trace at most a melee reach -- 3 to 3.5 blocks in shipped
     * content -- so the segment straddles at most one chunk plane and stays inside the
     * region the caster's eye already put us on. A Ray's 30 blocks do not, which is the
     * Folia defect the adapter documents on combatantsNear.
     *
     * A zero-length segment is clear: there is nothing between a point and itself.
     */
    boolean lineOfSightClear(Vec3 from, Vec3 to);

    /**
     * Defer {@code task} by at least {@code delayTicks}, on the thread owning
     * {@code near}'s region.
     *
     * {@code delayTicks} must be >= 1. There is no "schedule this for the current
     * frame": the Paper adapter clamps a delay of 0 up to 1 tick, so asking for 0
     * would quietly get you 1. To act on the current frame, act inline.
     */
    void schedule(Vec3 near, int delayTicks, Runnable task);

    /**
     * Defer {@code task} by at least {@code delayTicks}, on the thread that owns this COMBATANT --
     * the entity twin of {@link #schedule}. A no-op if the combatant is not here, and if it goes
     * before the delay elapses the task simply never runs.
     *
     * <p>Both exist because the two questions are different. {@link #schedule} defers work at a
     * PLACE, which is right for a ray walking chunk columns or a fuse burning where it was lit.
     * This defers work AT A BODY, which is the only correct choice when the task's first act is to
     * READ that body: a location-based hop lands on whichever region owns the point you guessed,
     * and a caster who walked out of it is then read from a thread that does not own them.
     *
     * <p>{@code delayTicks} must be >= 1, exactly as {@link #schedule} requires and for the same
     * reason: the Paper adapter clamps 0 up to 1, so asking for 0 would quietly get you 1.
     */
    void scheduleOn(UUID combatantId, int delayTicks, Runnable task);

    /**
     * Where this combatant's eye is and which way it is LOOKING, right now. Empty if it is not here.
     *
     * <p>A READ, like {@link #combatantsNear} and {@link #castRay}: only legal on the thread that
     * owns this combatant. Pair it with {@link #scheduleOn}, which is what puts you there.
     *
     * <p><b>It is not on {@link CombatantSnapshot}, and that is a decision.</b> A snapshot carries
     * what stays TRUE about a combatant once frozen; a facing is the one thing a caster changes
     * every tick, and it is read precisely because it must NOT be frozen. Putting it on the record
     * would hand every effect that outlives its cast frame -- a projectile, a lingering area -- a
     * stale direction that reads as current, which is the failure the snapshot/handle split exists
     * to prevent.
     */
    Optional<Aim> aimOf(UUID combatantId);

    /**
     * The GEAR SCORE of the weapon this combatant is holding RIGHT NOW, as a whole number where
     * {@link io.github.butterflysmp.rpg.core.weapon.GearScore#BASELINE} scales nothing.
     *
     * <p>A READ, like {@link #aimOf} above: only legal on the thread that owns this combatant.
     *
     * <p><b>It is not on {@link CombatantSnapshot}, and that is a decision -- the same one
     * {@link #aimOf} records, reached by a different route.</b> A held item's score is a property of
     * the ITEM, not of the combatant, and that record is shared with every mob in the game; a
     * component meaningless for half its users acquires a meaning by accident. A score resolved as a
     * {@code Stat} would be worse still: a stat is {@code base + SIGMA(modifiers)}, so the day anything
     * adds a gear-score modifier it would move the TRIGGER path and not the ATTACK path, at the same
     * score, on the same weapon.
     *
     * <h2>*** TOTAL. EVERY ID THIS PORT CAN RESOLVE HAS AN ANSWER, AND THE ANSWER IS WRITTEN DOWN ***</h2>
     *
     * <p><b>{@code GearScore.BASELINE} is the defined answer for a caster with no held gear</b> -- a
     * mob, an empty hand, a vanilla item, an item minted before the score existed. It is stated here
     * in the PORT rather than discovered in an implementation, so the two implementors cannot answer
     * differently and so the decision has somewhere to be revisited the day mob gear arrives. It
     * returns an {@code int} rather than an {@code OptionalInt} for exactly that reason: there is no
     * absent case, only a baseline one.
     *
     * <p><b>Abstract rather than {@code default}.</b> A default returning the baseline would let a
     * test fixture stay silent and still compile -- the blind-fixture shape, where a scan that
     * discovers nothing is indistinguishable from one that found everything in order. Both
     * implementors answer on purpose.
     *
     * <p><b>Re-read per shot, which is a property of the CALLER and not of this method.</b>
     * {@code CastExecutor.volley} calls it once per shot because it rebuilds the whole {@code Caster}
     * each time; a projectile calls it once, at the muzzle. See {@code Caster.withTriggerScore} for
     * why the value is frozen once it is in hand.
     */
    int triggerScoreOf(UUID combatantId);

    /**
     * THE BEHAVIOURAL ENCHANTS ACTIVE ON THE WEAPON {@code combatantId} HOLDS NOW (the seat's B2, 2026-11-23): each
     * enchant that is the active candidate of its slot at level 1 or more and whose file declares a behaviour. Empty for
     * an empty hand, a non-weapon, a mob, and an id this world does not know. Read where {@link #triggerScoreOf} is read,
     * and frozen onto the {@code Caster} the same way, so a slow body lands with the behaviours it was MADE with.
     *
     * <p><b>Abstract</b>, for {@link #triggerScoreOf}'s reason: a default empty list would let a fixture compile while
     * never presenting a behaviour, and no row could see the stamp.
     */
    java.util.List<io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour> behavioursOf(UUID combatantId);

    /**
     * ONE PRIMARY HIT LANDED (B2-B4, 2026-11-23): {@code EffectApplier}'s two damage arms report every damage they
     * actually apply, inside the same gate as {@link #traceHit}. The adapter hands it to the server's one long-lived
     * {@code BehaviourHits}, which ignores a caster that is not a hit ({@code Caster.countsAsHit}) or carries no
     * behaviour, and decides the rest. Called on the thread that owns the TARGET.
     *
     * <p><b>Abstract</b>, so the fixture must say what it does with a hit.
     */
    void primaryHit(Caster caster, Combatant target);

    /** The server's tick now: the clock a behaviour's window and cooldown are counted on. */
    long currentTick();

    /**
     * A dev trace (2026-11-23, Airburst Flames): a projectile SPLIT at {@code at} into {@code bursts}, by
     * {@code casterId}'s cast of {@code source}. The adapter logs it as {@code SPLIT} while {@code /rpg mobtrace} is on,
     * beside PLAYERHIT. It must not throw and must not change anything. Abstract, so the fixture records it.
     */
    void traceSplit(UUID casterId, String source, io.github.butterflysmp.rpg.core.Vec3 at, int bursts);

    /** The server's one {@link Orbs} (Lapis Crystal's D2, 2026-11-23): every standing orb, one per player. */
    Orbs orbs();

    /**
     * An orb's body: a marker exactly as {@link #spawnMarker} spawns one (never saved, never picked up, gravity off, an
     * armed despawn after {@code expectedLifetimeTicks}), TAGGED as an orb, so a body no live orb owns is swept where it
     * appears. Returns its id.
     */
    UUID spawnOrbMarker(io.github.butterflysmp.rpg.core.Vec3 at, String itemId, int expectedLifetimeTicks);

    /**
     * {@code ORB} trace lines (D2): {@code event} is {@code rest} (it came to rest at {@code at}) or {@code removed} (with
     * {@code cause}: expired, replaced, quit, death, world_change, stop, orphan). Logged always, not only under mobtrace:
     * one line per throw and per removal, and the boot's after-stop check reads them.
     */
    void traceOrb(UUID ownerId, String event, io.github.butterflysmp.rpg.core.Vec3 at, String cause);

    /**
     * A dev trace: {@code EffectApplier} reports every direct ability or weapon hit it lands on a
     * non-player target, and the adapter logs it as {@code PLAYERHIT} while {@code /rpg mobtrace} is on.
     * See {@link TracedHit} for what is and is not reported, and why its amount is the pre-Defense one.
     *
     * <p>Called on the thread that entered the applier, which owns the TARGET (the same thread
     * {@code applyDamage} is called from). It must not throw and must not change anything.
     *
     * <p><b>Abstract rather than {@code default}</b>, for {@link #triggerScoreOf}'s reason: a default
     * no-op would let the test fixture compile without recording, and then no row could see whether
     * the applier reports at all.
     */
    void traceHit(TracedHit hit);

    /**
     * Report how a slamming leap's mark ended, under the same switch as {@link #traceHit} ({@code /rpg mobtrace}):
     * one line per ending, slam or not, so a "no slam" reading is a line that says so rather than an absent
     * PLAYERHIT. {@code feet} is null for an ending that does not slam. Abstract for {@link #traceHit}'s reason.
     */
    void traceLanding(UUID casterId, String source, Touchdown how, Vec3 feet);

    /** Fire-and-forget presentation hook. Particles, sounds, damage numbers. */
    void present(Vec3 at, String visualId);

    /**
     * {@link #present} SCALED TO A RADIUS (the void implosion, 2026-11-17): a visual step that is drawn over an area, the
     * implosion's inward spiral, is drawn at {@code radius} blocks rather than at its authored default, so a stage-1
     * Phage's swirl is its own reach wide. Every other step is presented exactly as {@link #present} does. The default
     * ignores the radius, so a port that draws no areas need not answer it.
     */
    default void present(Vec3 at, String visualId, double radius) {
        present(at, visualId);
    }

    /**
     * START AN IMPLOSION'S DRAG at {@code origin} (2026-11-18; {@code shape.drags()}): for the shape's duration the world
     * finds, each tick, every combatant {@code affects} names near the origin (never {@code casterId}; never a creative
     * or spectator player, a shot's carrier, or anything else our hostility rules exempt), reads each one's footing on
     * the thread that owns it, and runs core's {@link io.github.butterflysmp.rpg.core.shape.ImplosionDrag} over them,
     * setting each push's velocity. The drag is point work at the origin: the world's clean ends it. Abstract on
     * purpose, like {@link CombatantHandle#applyPull}: a new port must decide.
     */
    void startDrag(Vec3 origin, io.github.butterflysmp.rpg.core.shape.Implosion shape,
                   io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Affects affects, UUID casterId);

    /**
     * Draw a named visual ALONG the segment from {@code from} to {@code to}, rather than at a
     * point. A beam.
     *
     * <p><b>ONE REGION HOP FOR THE WHOLE SEGMENT, AND THE CHUNK-COLUMN WALK IS WHAT MAKES THAT
     * LEGAL.</b> {@link #present} does its own hop per call, so core walking a segment and calling
     * it N times would be N region hops per tick -- roughly a hundred, for a 26-block beam at four
     * samples per block. This is one. It is sound precisely because the only caller hands it a
     * segment bounded by chunk planes ({@link ChunkTraversal}), so the segment lies inside one
     * chunk column by construction, and a column belongs to exactly one region. <b>The architecture
     * that made the beam awkward to draw is the same one that makes drawing it this way safe.</b>
     *
     * <p>So the contract is narrower than the signature: DO NOT call this with an arbitrary
     * segment. A caller that has not confined its segment to a column must walk columns first.
     *
     * <p>Fire-and-forget like {@link #present}, and an unknown id is a content mistake rather than
     * a programming error.
     */
    void presentAlong(Vec3 from, Vec3 to, String visualId);

    /**
     * Throw a real item of material {@code itemId} from {@code origin} moving at {@code velocity},
     * and return its id. The item flies and lands under ordinary physics -- it IS the marker for
     * a thrown detonator, so no separate display entity is planted. The id, never the entity, is
     * what core keeps -- the same discipline as a caster's UUID: it outlives the frame that threw
     * it, and holding the entity would pin it.
     *
     * Only legal on the thread owning {@code origin}'s region, like every other world write.
     */
    UUID throwMarker(Vec3 origin, Vec3 velocity, String itemId);

    /**
     * Plant a marker of material {@code itemId} AT {@code at} and return its id -- inert, going
     * nowhere on its own. The caller owns its motion from here and supplies it with
     * {@link #driveMarker}.
     *
     * <p>The opposite of {@link #throwMarker} in the one way that matters: that one hands the item
     * to physics and reads back where physics took it, this one is a body rendered at a position
     * something else computed. A projectile that resolves on a traced segment cannot let physics
     * own the position, or the thing you see and the thing you hit are two different objects.
     *
     * <p><b>{@code expectedLifetimeTicks} is not decoration.</b> It is how long the caller expects
     * to need the marker, and the adapter uses it to arm a death that does NOT depend on the caller
     * ever coming back. That matters because the caller is a chain of scheduled callbacks with no
     * {@code finally}: a task scheduled into a region that unloads, or a server that stops, simply
     * does not fire, and what is left behind is a real entity that only our code removes. Every
     * other exit is the caller's job; this parameter covers the exit where there is no caller left.
     *
     * <p>Only legal on the thread owning {@code at}'s region, like every other world write.
     */
    UUID spawnMarker(Vec3 at, String itemId, int expectedLifetimeTicks);

    /**
     * Plant a BOLT BODY of kind {@code body} -- an arrow or a fireball -- at {@code at}, already
     * moving at {@code velocity}, and return its id.
     * The sibling of {@link #spawnMarker}, and everything that method's contract says about
     * ownership, {@code expectedLifetimeTicks} and {@link #driveMarker} applies here unchanged.
     *
     * <p><b>WHY A SIBLING AND NOT A MATERIAL ID ON THE EXISTING METHOD.</b> {@code spawnMarker}
     * takes an ITEM id and renders a dropped item. An arrow body is not an item and has no item id;
     * passing a magic string like {@code "arrow"} through {@code itemId} would put a value in that
     * parameter which is not the thing the parameter is named for, and which the next reader
     * resolves as a {@code Material}. <b>Two shapes, two methods.</b>
     *
     * <p><b>WHY {@code velocity} IS A PARAMETER, AND IT IS THE OPPOSITE OF THE ITEM MARKER'S
     * RULE.</b> {@code spawnMarker}'s shared configuration ZEROES the velocity, and argues at
     * length that <i>a marker must SAY it moves rather than forget to say it does not</i> -- correct,
     * because a fresh item entity has a random pop and a still body is the safe default.
     *
     * <p><b>An arrow derives its ROTATION from its own velocity, so for this body the safe default
     * is inverted.</b> Spawned still, it has no direction to point in on its launch frame, and the
     * platform smooths rotation across ticks, so it would visibly SNAP into line one tick later.
     * The launch velocity is therefore part of creating the body, not something done to it
     * afterwards -- which is why it is a constructor argument here and a forbidden one there.
     *
     * <p><b>{@code body} NAMES THE KIND, AND CORE DOES NOT KNOW WHAT EITHER KIND IS.</b> It is the
     * authored {@code body:} id ({@code arrow}, {@code fireball}), passed through as a string the
     * way {@code itemId} is. It grew here on the day a second body existed (LEGACY-B, the Blaze
     * King's Staff's fireball), as {@code AbilitySchema.parseBody}'s javadoc said it would; the
     * schema refuses any id the adapter cannot build, so an unknown one never reaches this call.
     * <b>The velocity rule above is the arrow's reason, and it does no harm to a fireball</b>: a
     * fireball has no rotation to take from it, and the drive overwrites it on the launch frame.
     *
     * <p>Only legal on the thread owning {@code at}'s region, like every other world write.
     */
    UUID spawnBoltMarker(Vec3 at, Vec3 velocity, int expectedLifetimeTicks, String body);

    /**
     * Drive a marker: give it {@code stepVelocity} as this tick's motion and let the platform carry
     * it. Named for what it does -- it does NOT set a position.
     *
     * <p><b>This is the only marker movement mechanism that has been WITNESSED reaching a player.</b>
     * Its predecessor repositioned the entity outright, which was verified to work server-side (23
     * repositions, zero target/actual mismatches) and verified NOT to reach the client's entity
     * tracker (a straight-up shot, where the body hangs nearly still around 20 blocks, showed
     * nothing there at all). Driving hands the movement to the platform's own mover, which is the
     * path every ordinary thrown item already uses and which is observed to render.
     *
     * <p><b>The caller must apply gravity itself, per tick.</b> The velocity is one tick's motion,
     * not a launch impulse: a value set once at spawn would fly straight while the computed path
     * arcs. {@code ProjectileFlight} already integrates the ability's own gravity each step and
     * hands the resulting step vector here, and the adapter suppresses the platform's own gravity so
     * the two do not both apply.
     *
     * <p><b>Only legal on the thread owning WHERE THE MARKER IS -- not where it is heading.</b> It
     * is an entity write, exactly like {@link #removeMarker}, and carries the identical unhopped
     * contract. A caller stepping a projectile is scheduled onto the region of the point it has
     * flown TO, while the marker still sits at the point it flew FROM, so the call belongs at the
     * END of a step, while still on the region the marker is actually in.
     *
     * <p>Contrast {@link #present}, which the adapter hops onto the right region by itself. This one
     * cannot: an entity write has to happen where the entity is, and only the caller knows that.
     *
     * <p>A no-op if the marker is already gone -- which is a reachable state rather than a
     * defensive one, because the platform may destroy a marker mid-flight (see the adapter).
     */
    void driveMarker(UUID markerId, Vec3 stepVelocity);

    /**
     * Remove a marker from {@link #throwMarker} or {@link #spawnMarker}. A no-op if it is already
     * gone, so the fuse task can call it unconditionally. Only legal on the thread owning the marker.
     */
    void removeMarker(UUID markerId);

    /**
     * Where the marker with this id currently is, or empty if it is gone (removed, or unloaded
     * with its chunk). A read, like {@link #combatantsNear} -- only legal on the thread owning
     * the marker.
     *
     * Lets a fuse detonate where the thrown item actually IS at fuse-end, wherever physics has
     * carried it, rather than where it was thrown.
     */
    Optional<Vec3> markerLocation(UUID markerId);
}
