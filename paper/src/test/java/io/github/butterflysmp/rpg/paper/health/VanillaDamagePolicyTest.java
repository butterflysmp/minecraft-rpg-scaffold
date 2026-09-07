package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.paper.health.VanillaDamagePolicy.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vanilla-damage policy, pinned across the WHOLE axis rather than the causes that happen to
 * matter.
 *
 * <p>The handler itself needs a live entity and a live event and is boot-witnessed by
 * {@code GATE-vanilla-damage.md}. This is the part that is not: {@link DamageCause} is a plain enum
 * and loads without a server -- the same property {@code VanillaHealPolicyTest} relies on for
 * {@code RegainReason}, and the reason this test lives in {@code paper/} rather than {@code core/}
 * despite the usual core-test-first ordering. There is no pure {@code core/} logic in this slice.
 *
 * <p>Each test names the mutation it forces red.
 */
@SuppressWarnings("deprecation")   // DRAGON_BREATH is deprecated in the pinned API; the table must still name it
class VanillaDamagePolicyTest {

    /**
     * The two causes an existing handler already owns, on the tokened melee path. Written here as
     * data rather than inline, because {@link #everyPASSIsOwnedElsewhereOrLeavesNoSURVIVOR} and
     * {@link #theTwoOWNEDCausesDoNotShareAnArmWithTheTwoREMOVALS} both need it and a second copy
     * could drift from the first.
     */
    private static final Set<DamageCause> OWNED_BY_AN_EXISTING_HANDLER =
            EnumSet.of(DamageCause.ENTITY_ATTACK, DamageCause.ENTITY_SWEEP_ATTACK);

    /** The two causes that remove the entity outright, so there is no survivor to have a truth. */
    private static final Set<DamageCause> LEAVES_NO_SURVIVOR =
            EnumSet.of(DamageCause.KILL, DamageCause.VOID);

    /**
     * The expected action for every constant, written out. Not a set of "the interesting ones":
     * NEXT.md's rule is to enumerate the AXIS, and the axis here is thirty-three constants long.
     *
     * <p>An {@link EnumMap} filled by hand rather than {@code Map.of}, which caps at ten pairs.
     */
    private static final Map<DamageCause, Action> EXPECTED = new EnumMap<>(DamageCause.class);

    static {
        // Already owned, on the tokened melee path.
        EXPECTED.put(DamageCause.ENTITY_ATTACK, Action.PASS);
        EXPECTED.put(DamageCause.ENTITY_SWEEP_ATTACK, Action.PASS);

        // Removal, not damage.
        EXPECTED.put(DamageCause.KILL, Action.PASS);
        EXPECTED.put(DamageCause.VOID, Action.PASS);

        // The world hurting you.
        EXPECTED.put(DamageCause.FALL, Action.REROUTE);
        EXPECTED.put(DamageCause.LAVA, Action.REROUTE);
        EXPECTED.put(DamageCause.FIRE, Action.REROUTE);
        EXPECTED.put(DamageCause.FIRE_TICK, Action.REROUTE);
        EXPECTED.put(DamageCause.HOT_FLOOR, Action.REROUTE);
        EXPECTED.put(DamageCause.CAMPFIRE, Action.REROUTE);
        EXPECTED.put(DamageCause.CONTACT, Action.REROUTE);
        EXPECTED.put(DamageCause.DROWNING, Action.REROUTE);
        EXPECTED.put(DamageCause.SUFFOCATION, Action.REROUTE);
        EXPECTED.put(DamageCause.FREEZE, Action.REROUTE);
        EXPECTED.put(DamageCause.MELTING, Action.REROUTE);
        EXPECTED.put(DamageCause.DRYOUT, Action.REROUTE);
        EXPECTED.put(DamageCause.CRAMMING, Action.REROUTE);
        EXPECTED.put(DamageCause.STARVATION, Action.REROUTE);
        EXPECTED.put(DamageCause.FLY_INTO_WALL, Action.REROUTE);
        EXPECTED.put(DamageCause.FALLING_BLOCK, Action.REROUTE);
        EXPECTED.put(DamageCause.LIGHTNING, Action.REROUTE);
        EXPECTED.put(DamageCause.WORLD_BORDER, Action.REROUTE);

        // Damage over time from a status or a potion.
        EXPECTED.put(DamageCause.POISON, Action.REROUTE);
        EXPECTED.put(DamageCause.WITHER, Action.REROUTE);
        EXPECTED.put(DamageCause.MAGIC, Action.REROUTE);

        // Blasts and shockwaves.
        EXPECTED.put(DamageCause.BLOCK_EXPLOSION, Action.REROUTE);
        EXPECTED.put(DamageCause.ENTITY_EXPLOSION, Action.REROUTE);
        EXPECTED.put(DamageCause.SONIC_BOOM, Action.REROUTE);
        EXPECTED.put(DamageCause.THORNS, Action.REROUTE);

        // Sourced by an entity that is not the damager.
        EXPECTED.put(DamageCause.PROJECTILE, Action.REROUTE);

        // Correct either way -- reachability cannot be read on this build.
        EXPECTED.put(DamageCause.SUICIDE, Action.REROUTE);
        EXPECTED.put(DamageCause.DRAGON_BREATH, Action.REROUTE);

        // Someone else's damage.
        EXPECTED.put(DamageCause.CUSTOM, Action.REROUTE);
    }

    @Test
    void everyDamageCauseIsCLASSIFIEDAndTheTableCoversTheWHOLEEnum() {
        // The coverage assertion is the one that survives a Paper upgrade. forCause has no default
        // arm, so a thirty-fourth constant will not COMPILE -- but the day someone silences that by
        // adding a default, this row is what still notices. It also means the count in the class
        // javadoc is CHECKED rather than taken from the grep that first produced it.
        assertEquals(DamageCause.values().length, EXPECTED.size(),
                "the expectation table has drifted from the enum -- a constant was added or removed, "
                        + "and an unclassified cause is one this policy has never had an opinion about");

        for (DamageCause cause : DamageCause.values()) {
            assertEquals(EXPECTED.get(cause), VanillaDamagePolicy.forCause(cause),
                    "the action for " + cause);
        }
        // Mutation M1: move FALL into the KILL/VOID arm -> reddens, "the action for FALL expected
        // <REROUTE> but was <PASS>". RUN RED 2026-09-06.
        // Mutation M5: drop one EXPECTED.put -> reddens on the SIZE row, "expected <33> but was <32>",
        // which is also where the thirty-three in the policy javadoc is measured rather than trusted.
        // RUN RED 2026-09-06.
        // Mutation M6: replace the KILL/VOID arm with `default -> REROUTE` (the compiler goes quiet,
        // exactly as a future maintainer silencing an upgrade error would) -> this row still reddens,
        // "the action for KILL expected <PASS> but was <REROUTE>". RUN RED 2026-09-06.
    }

    @Test
    void everyPASSIsOwnedElsewhereOrLeavesNoSURVIVOR() {
        // THE GROUPING RULE: never pass damage that leaves a survivor whose truth did not move. A
        // passed damage on a tracked survivor is a silent no-op on truth -- the exact defect this
        // whole class exists to fix -- so PASS must be justified per arm, never used for the unclear.
        for (DamageCause cause : DamageCause.values()) {
            if (VanillaDamagePolicy.forCause(cause) != Action.PASS) continue;
            assertTrue(OWNED_BY_AN_EXISTING_HANDLER.contains(cause) || LEAVES_NO_SURVIVOR.contains(cause),
                    cause + " is PASSED with nothing else owning it and a survivor left behind -- the "
                            + "vanilla bar moves and HeartBarRenderer reverts it on the next tick, "
                            + "which is a visible bug. Reroute it, or name why it leaves no survivor.");
        }
        // Mutation M1 (FALL -> PASS), M3 (CUSTOM -> PASS) and M4 (FIRE_TICK -> PASS) each redden this
        // row as well as the table, naming the constant. RUN RED 2026-09-06, all three.
    }

    @Test
    void theTwoMELEECausesAreTheOnlyOnesAnExistingHandlerOWNS() {
        // The other direction of the same rule, and the one that catches the WORSE failure. A cause
        // wrongly moved OUT of PASS lands every melee hit twice: onPlayerMeleeAttack tokens the event
        // and deals the weapon's payload, and a reroute would then deal the token AGAIN as custom HP.
        for (DamageCause owned : OWNED_BY_AN_EXISTING_HANDLER) {
            assertEquals(Action.PASS, VanillaDamagePolicy.forCause(owned),
                    owned + " already has a handler on the tokened melee path (onPlayerMeleeAttack / "
                            + "onPlayerSweepAttack / onMobMeleeAttack). Rerouting it lands every melee "
                            + "hit TWICE.");
        }
        // Mutation M2: move ENTITY_ATTACK into the CUSTOM arm -> reddens here AND on the table.
        // RUN RED 2026-09-06.
    }

    @Test
    void theTwoOWNEDCausesDoNotShareAnArmWithTheTwoREMOVALS() {
        // Both groups are PASS, for two DIFFERENT reasons, and merging them would make one of the two
        // justifications false wherever it was written. This is the EATING control's shape from
        // VanillaHealPolicyTest: the grouping is itself an assertion, so it gets asserted.
        for (DamageCause owned : OWNED_BY_AN_EXISTING_HANDLER) {
            assertTrue(!LEAVES_NO_SURVIVOR.contains(owned),
                    owned + " is owned by a handler, not a removal -- the two PASS groups must stay "
                            + "distinct or one arm's comment stops being true");
        }
        // ENTITY_ATTACK leaves a survivor whose truth DOES move (the melee rider moved it). KILL does
        // not leave one at all. Same action, opposite reasons.
        assertNotEquals(OWNED_BY_AN_EXISTING_HANDLER, LEAVES_NO_SURVIVOR,
                "the two PASS groups must not collapse into one");
        // NO MUTATION, AND THAT IS SAID OUT LOUD rather than left to look like an oversight. This row
        // guards the TEST's own grouping constants, not the policy: no edit to forCause can redden it,
        // because both sets live here. What it stops is a later "simplification" that merges the two
        // sets, after which everyPASSIsOwnedElsewhereOrLeavesNoSURVIVOR would still pass while
        // checking a weaker property than its name claims. Same standing as ManaRegenTest's retired
        // 60-second case: it holds a reason open, it does not test our arithmetic.
    }

    @Test
    void CUSTOMIsREROUTEDBecauseReroutingIsNotEATING() {
        // The heal policy PASSes CUSTOM so it is not the sole writer of a player's health -- "it eats
        // the unforeseen silently". That argument is honoured here rather than copied: rerouting
        // carries another plugin's intent out in full, in the currency that is true. Nothing of ours
        // reaches it (no entity.damage(..) call exists in paper/src/main, and both death systems use
        // setHealth(0), which raises no damage event), so this cannot eat our own damage either.
        assertEquals(Action.REROUTE, VanillaDamagePolicy.forCause(DamageCause.CUSTOM),
                "CUSTOM is how another plugin's damage arrives; passing it would leave a survivor "
                        + "whose truth did not move, which is the defect this class exists to fix");
        // Mutation M3: CUSTOM -> PASS -> reddens here, on the table, and on the PASS-justification
        // row. RUN RED 2026-09-06.
    }

    @Test
    void theMOTIVATINGFireCausesGetNoSpecialTreatment() {
        // Scorch is the NEXT slice, and the temptation is to special-case the cause that motivated
        // this one. FIRE and FIRE_TICK must be classified exactly like CACTUS -- if they ever diverge
        // from the plain environmental group, a Scorch decision has leaked into the boundary.
        assertEquals(VanillaDamagePolicy.forCause(DamageCause.CONTACT),
                VanillaDamagePolicy.forCause(DamageCause.FIRE),
                "FIRE must be treated exactly like any other environmental cause");
        assertEquals(VanillaDamagePolicy.forCause(DamageCause.CONTACT),
                VanillaDamagePolicy.forCause(DamageCause.FIRE_TICK),
                "FIRE_TICK must be treated exactly like any other environmental cause");
        // Mutation M4: move FIRE_TICK into the KILL/VOID arm -- the shape a Scorch special case would
        // take -> reddens here, on the table, and on the PASS-justification row. RUN RED 2026-09-06.
    }
}
