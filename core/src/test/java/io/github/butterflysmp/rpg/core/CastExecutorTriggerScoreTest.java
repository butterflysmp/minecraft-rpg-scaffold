package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.combat.AttackCharge;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.weapon.GearScore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * *** THE GEAR SCORE REACHES A LITERAL DAMAGE PAYLOAD ON EVERY CAST PATH, NOT JUST THE VOLLEY. ***
 *
 * <h2>WHY THIS FILE EXISTS: FOUR GREEN ROWS GUARDED NOTHING</h2>
 *
 * <p>{@code CastExecutor} writes the trigger score at THREE sites -- {@code commit} (every
 * projectile, ray and burst), {@code landBasicMelee}, and {@code volley}. Measured 2026-09-20 by
 * deleting the write at each site in turn and running the whole of {@code core}:
 *
 * <pre>
 *   volley           RED    CastExecutorVolleyTest.eachShotREREADSTheGearScore...
 *   commit           GREEN  1138 pass          &lt;- nothing noticed
 *   landBasicMelee   GREEN  1138 pass          &lt;- nothing noticed
 * </pre>
 *
 * <p><b>Two of three sites could be deleted and the suite did not object</b>, on the slice's own
 * headline claim. These rows close that.
 *
 * <h2>*** THE MISTAKE THAT MADE IT POSSIBLE, BECAUSE IT IS EASY TO REPEAT HERE ***</h2>
 *
 * <p>{@code EffectApplierTest}'s rows set {@code Dummy.triggerScore} and then call
 * {@code dummy.asCaster()} -- and {@code asCaster()} applies {@code withTriggerScore} ITSELF. <b>The
 * fixture builds the object the production factory failed to build</b>, so those rows pin the
 * ARITHMETIC and say nothing about the WIRING. They are correct and they were never the guard
 * anybody thought they were.
 *
 * <p><b>So nothing below calls {@code asCaster()}.</b> Every row here drives a real
 * {@code AbilityService} into a real {@code CastExecutor}, which builds its own {@code Caster} via
 * {@code Caster.of(snapshot)} and asks {@code world.triggerScoreOf(...)} for the score. The dummy's
 * {@code triggerScore} field is read by {@link FakeWorld#triggerScoreOf}, which is the port -- the
 * same shape production uses, where {@code PaperCombatWorld} reads the held item.
 *
 * <p><b>The acceptance criterion for these rows was the mutation, not the assertion.</b> A row that
 * passes proves the code works today; only deleting the write and watching this redden proves the
 * row would notice if it stopped. Both were re-run after these landed.
 *
 * <h2>STAGING</h2>
 *
 * <p>Score <b>250</b> against an authored <b>16</b> gives <b>40</b>. No two staged quantities are
 * equal, so a transposition has nowhere to hide, and 250 is far from both clamps -- neither the
 * floor nor the cap can supply the answer.
 */
class CastExecutorTriggerScoreTest {

    private static final double EPS = 1e-9;

    /** {@code FakeWorld.Dummy}'s default eye height, and therefore where a ray travels. */
    private static final double EYE = 1.62;

    /** The authored literal. Distinct from every other number in this file. */
    private static final double AUTHORED = 16.0;

    /** Not a clean multiple of the baseline, and far from MIN and HARD_CAP. */
    private static final int SCORE = 250;

    /** {@code 16 * 250 / 100}. Stated as the product so a reader can check it without running. */
    private static final double SCALED = 40.0;

    /**
     * A cast that is NOT a volley, so it commits once and never re-projects. This is what every
     * staff, every projectile and every ray in shipped content actually is.
     */
    private static AbilityDefinition rayWithLiteralDamage() {
        return new AbilityDefinition("test", "Test", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Ray(30),
                List.of(new EffectSpec.Damage(AUTHORED, "kinetic")));
    }

    /**
     * A melee-delivered ability that authors a LITERAL damage effect.
     *
     * <p><b>NO SHIPPED WEAPON DECLARES THIS, AND THE ROW STAGES IT DELIBERATELY.</b> Measured across
     * all thirteen content weapons on 2026-09-20: two declare a {@code type: melee} cast
     * ({@code ironblade}, {@code emberblade}) and BOTH carry {@code weapon_damage} alone, which
     * ignores the trigger score by design because {@code WeaponAttackItems} already scaled it into
     * the ATTACK_DAMAGE stat. {@code emberblade}'s fireball looks like a counterexample and is not --
     * it is a {@code type: projectile} cast, so it goes through {@code commit}.
     *
     * <p><b>So {@code landBasicMelee}'s write is correct-for-the-future on content that does not
     * exist</b>, and this fixture CAUSES the condition rather than asserting the arm is present --
     * which is the only way to test a guard whose triggering case no shipped content reaches.
     */
    private static AbilityDefinition meleeWithLiteralDamage() {
        return new AbilityDefinition("melee", "Melee", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Melee(3.0, 90.0),
                List.of(new EffectSpec.Damage(AUTHORED, "kinetic")));
    }

    private static FakeWorld.Dummy casterIn(FakeWorld world) {
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);
        return caster;
    }

    private static FakeWorld.Dummy victimAt(FakeWorld world, double x) {
        var d = new FakeWorld.Dummy(new Vec3(x, EYE, 0));
        world.entities.add(d);
        return d;
    }

    /**
     * Drive the ability through the REAL service and executor. Only the world is fake.
     *
     * <p><b>This is the whole point of the file: the {@code Caster} is built by {@code commit}</b>,
     * from {@code caster.snapshot()} plus {@code world.triggerScoreOf(...)}. Nothing here hands the
     * executor a pre-scored caster.
     */
    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def) {
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L),
                new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), def.id(), new Aim(new Vec3(0, EYE, 0), new Vec3(1, 0, 0)),
                        Set.of(def.id())));
        new CastExecutor(world).execute(success);
    }

    // --- site: commit ---------------------------------------------------------------------------

    /**
     * *** THE {@code commit} SITE. Deleting its write leaves the suite GREEN without this row. ***
     *
     * <p>This is the path EVERY shipped trigger weapon except a volley takes -- the three staves,
     * {@code quiver_stone}, and {@code emberblade}'s fireball.
     */
    @Test
    void aNonVolleyCastScalesItsLiteralDamageByTheScoreTheWORLDReports() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        var victim = victimAt(world, 5);
        caster.triggerScore = SCORE;   // read back by FakeWorld.triggerScoreOf, not by asCaster

        cast(world, caster, rayWithLiteralDamage());

        assertEquals(100 - SCALED, victim.health, EPS,
                "THE SCORE MUST REACH A NON-VOLLEY CAST. commit() builds this Caster and is the path"
                        + " every staff, projectile and ray in shipped content takes. If this reads"
                        + " 84 the authored 16 arrived unscaled and the slice's main claim is false"
                        + " for every weapon except the volley. AND 100 MEANS THE RAY NEVER LANDED --"
                        + " a hollow fixture, not a wiring failure.");
    }

    /** The identity, on the same path: a baseline score changes nothing. */
    @Test
    void aNonVolleyCastAtBASELINEDealsTheAuthoredAmount() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        var victim = victimAt(world, 5);

        assertEquals(GearScore.BASELINE, caster.triggerScore,
                "the dummy default must BE the baseline, not merely behave like it");

        cast(world, caster, rayWithLiteralDamage());

        assertEquals(100 - AUTHORED, victim.health, EPS,
                "BASELINE scales nothing on this path either -- an unreconciled caster deals its"
                        + " authored 16. Scaled would be 60; 100 means the ray never landed.");
    }

    // --- site: landBasicMelee -------------------------------------------------------------------

    /**
     * *** THE {@code landBasicMelee} SITE. Deleting its write leaves the suite GREEN without this
     * row, and NOTHING ELSE OBSERVES IT -- no other test, and no gate row, because no shipped
     * content reaches it. ***
     *
     * <p>See {@link #meleeWithLiteralDamage()} for why the fixture stages content that does not
     * ship, and why that is the correct construction rather than a hollow one.
     */
    @Test
    void aMeleeDeliveredLiteralDamageScalesByTheScoreTheWORLDReports() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        var victim = victimAt(world, 2);
        caster.triggerScore = SCORE;

        new CastExecutor(world).landBasicMelee(meleeWithLiteralDamage(), caster.snapshot(),
                new Combatant(victim.snapshot(), victim), AttackCharge.FULL_CHARGE);

        assertEquals(100 - SCALED, victim.health, EPS,
                "THE SCORE MUST REACH A MELEE-DELIVERED LITERAL. No shipped weapon authors one, so"
                        + " this site has no gate row and no other test -- deleting its write is"
                        + " invisible to everything else in the project. Unscaled reads 84;"
                        + " 100 means the swing never connected.");
    }

    /**
     * *** THE NEGATIVE, AND IT IS THE ROW THAT GUARDS THE TWO MELEE WEAPONS THAT ACTUALLY SHIP. ***
     *
     * <p>{@code ironblade} and {@code emberblade} are the only weapons declaring a {@code type: melee}
     * cast, and BOTH carry {@code weapon_damage} alone. That arm reads {@code caster.attackDamage()}
     * -- the ATTACK_DAMAGE stat, which {@code WeaponAttackItems} has ALREADY scaled by the held
     * item's score at mint. <b>Applying the trigger score here as well would make a score-400 weapon
     * deal SIXTEEN times rather than four.</b>
     *
     * <p><b>NOTHING WOULD LOOK WRONG.</b> Both factors are individually correct and each has its own
     * passing test; the defect exists only in their composition. So this row holds the attack damage
     * FIXED while the trigger score moves, and asserts the number does not budge.
     *
     * <h2>ITS MUTATION IS AN ADDITION, NOT A DELETION, AND THAT IS WHY IT IS A SEPARATE ROW</h2>
     *
     * <p>Every other row in this file is killed by REMOVING a {@code withTriggerScore} write. This
     * one is killed by ADDING {@code GearScore.scaledDamage} to {@code EffectApplier}'s
     * {@code WeaponDamage} arm -- the opposite edit, on a different file. <b>A row whose mutation
     * deletes cannot detect an over-application</b>, so the three positive rows above are all blind
     * to this defect and it is blind to theirs.
     *
     * <p><b>This differs from {@code EffectApplierTest}'s row of the same name in the one way that
     * matters:</b> that one builds its {@code Caster} through {@code dummy.asCaster()}, which applies
     * {@code withTriggerScore} in the FIXTURE. This drives {@code landBasicMelee}, so the score
     * arrives the way production delivers it -- and this is the path a real melee weapon takes.
     *
     * <p>Staged at {@code attackDamage 19} with score {@code 250}: the correct answer is 19 and the
     * defect's answer is 47.5, which share no digits and cannot be confused at a glance.
     */
    @Test
    void aMeleeWEAPONDamageIsNOTScaledBecauseTheStatAlreadyCarriesTheScore() {
        var world = new FakeWorld();
        var caster = casterIn(world);
        var victim = victimAt(world, 2);
        caster.attackDamage = 19;      // the Boltor's figure, already score-scaled at mint
        caster.triggerScore = SCORE;   // a score the world reports, which this arm must IGNORE

        new CastExecutor(world).landBasicMelee(meleeWithWeaponDamage(), caster.snapshot(),
                new Combatant(victim.snapshot(), victim), AttackCharge.FULL_CHARGE);

        assertEquals(100 - 19, victim.health, EPS,
                "THE SCORE MUST NOT REACH THE weapon_damage ARM. The ATTACK_DAMAGE stat already"
                        + " carries it, so a second application is 16x at score 400. If this reads"
                        + " 52.5 the scale was applied twice -- and ironblade and emberblade are the"
                        + " two shipped weapons that would take it. 100 means the swing never"
                        + " connected and this row exercised nothing.");
    }

    /**
     * A melee-delivered BASIC ATTACK -- {@code weapon_damage}, which is what both shipped melee
     * weapons actually declare. Contrast {@link #meleeWithLiteralDamage()}, which no weapon has.
     */
    private static AbilityDefinition meleeWithWeaponDamage() {
        return new AbilityDefinition("swing", "Swing", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Melee(3.0, 90.0),
                List.of(new EffectSpec.WeaponDamage("kinetic")));
    }
}
