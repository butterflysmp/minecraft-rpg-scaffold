package io.github.butterflysmp.rpg.paper.health;

import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * What to do with a vanilla damage event aimed at a TRACKED COMBATANT.
 *
 * <h2>The invariant this exists to enforce, and the half-boundary it completes</h2>
 *
 * Nothing vanilla may move a tracked combatant's bar without moving the truth. The vanilla health
 * attribute is a DISPLAY -- {@code HeartBarRenderer} rewrites it from the custom numbers on the next
 * {@code HealthChange} or reconcile tick -- so vanilla damage that lands is visible for a fraction of
 * a second and is then silently reverted. That reads to a player as a bug, and it is one.
 *
 * <p><b>{@link VanillaHealPolicy} states exactly that invariant for HEALS, and was enforced only for
 * heals.</b> It enumerates all nine {@code RegainReason} constants, refuses a default arm on purpose,
 * and reroutes -- careful work, and half a boundary. Fall damage and drowning were live defects on
 * master for the whole time it stood, because an invariant stated in terms of one direction gets
 * enforced in one direction. This class is the other half.
 *
 * <h2>Scope: BOTH players and mobs, and the differing gate is deliberate</h2>
 *
 * {@code VanillaHealPolicy}'s caller gates on "is this a tracked PLAYER", because no vanilla heal was
 * rewriting a mob's health. This one's caller gates on "is this combatant TRACKED" -- mobs read their
 * nameplates from the same store, so mob fall damage was broken the same way. That difference is not
 * a copy-paste slip.
 *
 * <h2>The grouping rule</h2>
 *
 * <b>NEVER PASS DAMAGE THAT LEAVES A SURVIVOR WHOSE TRUTH DID NOT MOVE.</b> A passed damage on a
 * tracked survivor is a silent no-op on truth, which is the defect above. The rule is stated that way
 * rather than as "never pass a damage you are not already handling" because the second form needed an
 * exception on its second use -- {@link DamageCause#VOID} and {@link DamageCause#KILL} -- and a rule
 * that needs a carve-out on its second use is stated wrong. Those two leave no survivor, so they are
 * outside the domain rather than exceptions to it, and the arm below reasons FROM the rule instead of
 * apologising for it. The caller's untracked gate is outside the domain for the mirror reason: for an
 * untracked combatant vanilla health IS the only truth there is, so passing moves it.
 *
 * <h2>Exhaustive, with NO default arm, deliberately</h2>
 *
 * {@link #forCause} is a switch EXPRESSION over every one of the thirty-three constants. A
 * thirty-fourth added by a future Paper release is then a COMPILE ERROR rather than a silent
 * fall-through into whichever behaviour the default happened to pick -- {@code NEXT.md}'s "enumerate
 * the axis, not the cases you currently have". Do not add a default to make it shorter; the length IS
 * the guard.
 *
 * <h2>Why REROUTE tokens rather than cancels</h2>
 *
 * The three shipped melee riders in {@code RpgListeners} all set the vanilla damage to
 * {@code TOKEN_DAMAGE} instead of cancelling, and this reuses that mechanism rather than inventing a
 * second one. Cancelling would remove far more than the health change: vanilla's i-frames, hurt
 * animation, knockback and death all ride the event. Tokening keeps every one of them. The sharpest
 * case is LAVA, which damages roughly every ten ticks BECAUSE of vanilla i-frames -- a cancelled lava
 * event would set none, and the reroute would fire on every tick instead of every tenth. Tokening
 * also leaves {@code BukkitCombatant.applyDamage}'s {@code getNoDamageTicks() == 0} flash gate meaning
 * what it already means, rather than inverting it.
 *
 * <h2>Why ENTITY_ATTACK and ENTITY_SWEEP_ATTACK are PASS</h2>
 *
 * <b>Because they already have handlers, and rerouting them would land every melee hit TWICE.</b>
 * {@code RpgListeners.onPlayerMeleeAttack}, {@code onPlayerSweepAttack} and {@code onMobMeleeAttack}
 * own them on the tokened melee path. PASS on the two most obviously damage-shaped constants in the
 * enum reads as an oversight without this paragraph, which is why it is here rather than inferred.
 *
 * <p>That claim is only true because {@code onMobMeleeAttack} was given an {@code ENTITY_ATTACK} gate
 * in the same change. It had none, so it claimed every {@code EntityDamageByEntityEvent} with a player
 * victim and a living non-player damager -- a creeper's {@code ENTITY_EXPLOSION} and a warden's
 * {@code SONIC_BOOM} among them. Without that gate the PASS set would not be a function of
 * {@link DamageCause} at all, and this class could not exist.
 *
 * <h2>Why VOID and KILL are PASS</h2>
 *
 * They are REMOVAL, not damage: they exist to take an entity out of the world unconditionally, so
 * there is no survivor whose truth could fail to move. A token would make {@code /kill} a no-op, and
 * would turn the void into a fall whose duration SCALES WITH MAX HP -- "kills eventually" is not the
 * same as "kills", and the stat it scales with is designed to grow.
 *
 * <p><b>Consequence to know, because a later slice depends on it:</b> a combatant removed this way
 * never reaches zero CUSTOM HP, so it does not run the custom death path ({@code MobDeathSystem} and
 * {@code PlayerHealthSystem} both consume {@code reachedZero}). Nothing leaks -- cleanup rides
 * {@code EntityRemoveFromWorldEvent} to {@code onMobRemove}, which clears plate and store on death,
 * despawn and chunk-unload alike -- but anything that must happen ON DEATH has to hook
 * {@code EntityDeathEvent}, which covers both paths, rather than {@code reachedZero}, which covers
 * one.
 *
 * <h2>Why CUSTOM is REROUTE, where the heal side PASSes</h2>
 *
 * The heal side passes CUSTOM so this project is not the sole writer of a tracked player's health --
 * cancelling it "eats the unforeseen silently". That argument is honoured here rather than copied:
 * <b>rerouting is not eating.</b> Another plugin's intent, hurt this entity, is carried out in full,
 * in the currency this project actually uses. Nothing of ours reaches it either -- no
 * {@code entity.damage(...)} call exists anywhere in {@code paper/src/main}, and both death systems
 * kill through {@code setHealth(0)}, which raises no damage event.
 *
 * <h2>Why SUICIDE and DRAGON_BREATH ride with the rest</h2>
 *
 * The {@code EATING} argument, from the heal policy: their reachability on this build cannot be read,
 * only measured. {@code DRAGON_BREATH} is deprecated in the pinned API with a javadoc saying
 * {@code ENTITY_ATTACK} is used instead; {@code SUICIDE} names a mechanism nothing in this project
 * invokes. Grouping them with the live causes is correct EITHER WAY -- translated if they fire, inert
 * if they do not -- whereas filing them under PASS would assert a mechanism instead of measuring one,
 * and be wrong in one of two ways with no way to tell which.
 *
 * <h2>Why there is no CANCEL</h2>
 *
 * <b>Because no cause needs one, and that is a result rather than a symmetry choice.</b>
 * {@code VanillaHealPolicy} has CANCEL because two reasons genuinely needed cancelling and
 * {@code HealthRegenSystem} replaced them; the constant is there because it has cases. Copying it
 * here without cases would copy the shape and drop the argument -- and an arm nothing exercises is an
 * authoritative-sounding declaration of a capability that has never run, which is the defect
 * {@code CLAUDE.md} records under the {@code flint_staff.yml} stacking note. The mirror of the heal
 * policy is in {@link #forCause}, not in this enum's cardinality.
 */
public final class VanillaDamagePolicy {

    private VanillaDamagePolicy() {}

    /** What the listener does with the event. */
    public enum Action {
        /** Leave it alone. Either something else already owns it, or it leaves no survivor. */
        PASS,
        /**
         * Token the vanilla damage AND translate its amount into custom HP, so the damage still
         * happens -- in the currency that is true. Tokening, not cancelling: see the class javadoc.
         */
        REROUTE
    }

    /**
     * The action for one cause, on a tracked combatant.
     *
     * <p><b>Every PASS arm is either owned by an existing handler or leaves no survivor.</b> That is
     * the rule the arms are grouped by, and {@code VanillaDamagePolicyTest} asserts it across the
     * whole enum rather than trusting this comment.
     */
    @SuppressWarnings("deprecation")   // DRAGON_BREATH is deprecated in the pinned API; the arm is still required for exhaustiveness
    public static Action forCause(DamageCause cause) {
        return switch (cause) {
            // ALREADY OWNED, on the tokened melee path. Rerouting these lands every melee hit TWICE.
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> Action.PASS;

            // REMOVAL, not damage: no survivor, so outside the grouping rule's domain entirely.
            case KILL, VOID -> Action.PASS;

            // The world hurting you. The population this slice exists for.
            case FALL, LAVA, FIRE, FIRE_TICK, HOT_FLOOR, CAMPFIRE, CONTACT, DROWNING, SUFFOCATION,
                 FREEZE, MELTING, DRYOUT, CRAMMING, STARVATION, FLY_INTO_WALL, FALLING_BLOCK,
                 LIGHTNING, WORLD_BORDER -> Action.REROUTE;

            // Damage over time from a status or a potion. The damage-side mirror of MAGIC/MAGIC_REGEN.
            case POISON, WITHER, MAGIC -> Action.REROUTE;

            // Blasts and shockwaves. ENTITY_EXPLOSION and SONIC_BOOM arrive here only because
            // onMobMeleeAttack was gated to ENTITY_ATTACK in the same change; before that they were
            // priced as melee. THORNS is a VANILLA ARMOUR enchantment -- our own Thorns is a SHIELD
            // enchant resolved in ShieldBlock and reflected through applyDamage, which raises no
            // event, so these are two sources rather than one mechanism applied twice.
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION, SONIC_BOOM, THORNS -> Action.REROUTE;

            // Sourced by an entity that is not the damager: an arrow's shooter, resolved off the
            // DamageSource by the caller. Unowned in BOTH directions before this -- a mob's arrow on a
            // player was deferred by the mob->player pass, and a player's bow on a mob fell through
            // the melee rider's damager check for the same reason.
            case PROJECTILE -> Action.REROUTE;

            // Reachability cannot be read on this build, only measured -- so grouped where the arm is
            // correct either way. See the class javadoc.
            case SUICIDE, DRAGON_BREATH -> Action.REROUTE;

            // Someone else's damage. Rerouting honours it rather than eating it.
            case CUSTOM -> Action.REROUTE;
        };
    }
}
