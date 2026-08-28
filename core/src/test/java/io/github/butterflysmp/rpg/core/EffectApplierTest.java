package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.effect.EffectApplier;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.AttackCharge;
import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.SweepShare;
import io.github.butterflysmp.rpg.core.combat.Combatant;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EffectApplierTest {

    /** What the world hands out: a snapshot to read, a handle to act on. */
    private static Combatant pair(FakeWorld.Dummy dummy) {
        return new Combatant(dummy.snapshot(), dummy);
    }

    /**
     * Every effect variant, applied with no target. A projectile that lands in
     * an empty field resolves no target, and the bundled solar_grenade.yml
     * carries a status effect. None of these may throw.
     */
    @Test
    void noEffectThrowsWhenThereIsNoTarget() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var applier = new EffectApplier(world);

        List<EffectSpec> everyVariant = List.of(
                new EffectSpec.Damage(12, "fire"),
                new EffectSpec.Heal(5),
                new EffectSpec.Knockback(1.5),
                new EffectSpec.Status("scorch", 40, 0),
                new EffectSpec.Visual("solar_detonation"),
                new EffectSpec.Area(4.0, 100, 20,
                        List.of(new EffectSpec.Damage(2, "fire"))));

        assertDoesNotThrow(() -> applier.applyAll(everyVariant, caster.asCaster(), null, Vec3.ZERO));
    }

    /** The specific regression: a status effect with nobody to apply it to. */
    @Test
    void statusWithNullTargetIsSkippedNotThrown() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var applier = new EffectApplier(world);

        assertDoesNotThrow(() -> applier.applyAll(
                List.of(new EffectSpec.Status("scorch", 40, 0)), caster.asCaster(), null, Vec3.ZERO));
    }

    /** Untargeted effects still run when there is no target. */
    @Test
    void visualStillPresentsWithNoTarget() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var applier = new EffectApplier(world);

        applier.applyAll(List.of(new EffectSpec.Visual("solar_detonation")), caster.asCaster(), null, Vec3.ZERO);

        assertEquals(List.of("solar_detonation"), world.presented);
    }

    /**
     * A burst is a blast, not a field: it lands on the frame it is applied, with no
     * scheduling at all. Anything scheduled would arrive at least one tick late, because
     * the Paper adapter clamps a zero delay up to one tick.
     *
     * It needs no target -- a grenade that detonates on bare ground still splashes.
     */
    @Test
    void burstAppliesNestedEffectsImmediatelyExcludingCaster() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var victim = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var farAway = new FakeWorld.Dummy(new Vec3(50, 0, 0));
        world.entities.add(caster);
        world.entities.add(victim);
        world.entities.add(farAway);

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Burst(4.0, List.of(
                        new EffectSpec.Damage(6, "fire"),
                        new EffectSpec.Status("scorch", 40, 0)))),
                caster.asCaster(), null, Vec3.ZERO);

        assertEquals(0, world.pendingTasks(), "a burst is inline; it must schedule nothing");
        assertEquals(94, victim.health, 0.001);
        assertEquals(List.of("scorch"), victim.statuses);

        assertEquals(100, caster.health, 0.001, "you do not splash yourself");
        assertTrue(caster.statuses.isEmpty(), "you do not scorch yourself");

        assertEquals(100, farAway.health, 0.001, "outside the radius");
    }

    /**
     * A thrown ember lands as a real item at once, waits out its single launch fuse, then
     * detonates mob-only and removes the item in the SAME task -- so item and detonation cannot
     * diverge and no item leaks. Three mutations redden here: drop the player skip and the
     * player burns; drop removeMarker and the item leaks; slip the fuse and the timing shifts.
     */
    @Test
    void throwEmbersFiresAfterItsFuseMobOnlyAndRemovesItsItem() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var mob = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var player = new FakeWorld.Dummy(new Vec3(2, 0, 0));
        player.player = true;
        world.entities.add(caster);
        world.entities.add(mob);
        world.entities.add(player);

        var embers = new EffectSpec.ThrowEmbers(List.of(0.0), 0.6, 0.25, "blaze_powder", 20,
                new EffectSpec.Burst(4.0, List.of(
                        new EffectSpec.Damage(8, "fire"),
                        new EffectSpec.Status("scorch", 60, 0))),
                "ember_burst", null);   // visual = boom at detonation; trail unused here

        new EffectApplier(world).applyAll(List.of(embers), caster.asCaster(), null, Vec3.ZERO);

        assertEquals(1, world.markers.size(), "the item is thrown at once");
        assertEquals(100, mob.health, 1e-9, "nothing detonates before the fuse");
        assertFalse(world.presented.contains("ember_burst"), "the boom waits for the fuse");

        world.advanceTicks(19);
        assertEquals(100, mob.health, 1e-9, "the fuse is 20 ticks -- not at 19");
        assertEquals(1, world.markers.size(), "the item lives exactly as long as the fuse");

        world.advanceTicks(1);
        assertEquals(92, mob.health, 1e-9, "detonates at 20 ticks: 8 fire damage");
        assertEquals(List.of("scorch"), mob.statuses);
        assertEquals(100, player.health, 1e-9, "mob-only: the burst spares players");
        assertTrue(player.statuses.isEmpty(), "mob-only: no scorch on players");
        assertEquals(100, caster.health, 1e-9, "you do not burn yourself");
        assertTrue(world.markers.isEmpty(), "item removed on detonation -- no leak");
        assertTrue(world.presented.contains("ember_burst"), "the boom fires with the blast");
    }

    /**
     * The blast-fungus guarantee: the fuse detonates where the thrown item actually IS at
     * fuse-end, not where it was thrown. Throw at the origin, drift the item 10 blocks away
     * (standing in for its flight and landing), fire the fuse -- the mob under the LIVE item
     * burns, the mob still sitting at the throw origin does not. Revert the burst point to
     * {@code origin} and the two mobs swap fates: this test reddens.
     */
    @Test
    void throwEmbersDetonatesAtTheItemsLivePositionNotWhereItWasThrown() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var mobAtThrow = new FakeWorld.Dummy(new Vec3(1, 0, 0));     // within 4 of the throw origin
        var mobAtLanding = new FakeWorld.Dummy(new Vec3(10, 0, 0));  // within 4 of where it lands
        world.entities.add(caster);
        world.entities.add(mobAtThrow);
        world.entities.add(mobAtLanding);

        var embers = new EffectSpec.ThrowEmbers(List.of(0.0), 0.6, 0.25, "blaze_powder", 20,
                new EffectSpec.Burst(4.0, List.of(new EffectSpec.Damage(8, "fire"))), null, null);

        new EffectApplier(world).applyAll(List.of(embers), caster.asCaster(), null, Vec3.ZERO);

        // The item flew and landed 10 blocks away before the fuse fired.
        UUID itemId = world.markers.keySet().iterator().next();
        world.moveMarker(itemId, new Vec3(10, 0, 0));

        world.advanceTicks(20);

        assertEquals(92, mobAtLanding.health, 1e-9,
                "detonates at the item's LIVE position: the mob there burns");
        assertEquals(100, mobAtThrow.health, 1e-9,
                "not where it was thrown: the mob at the origin is 9 blocks from the blast");
        assertTrue(world.markers.isEmpty(), "item still removed on detonation -- no leak");
    }

    /**
     * The per-tick tracking loop draws the trail every tick the ember is alive, at its live
     * position -- so a clean particle LINE follows the arc, not a single puff. A trail declared
     * but never wired would only show (by its absence) at boot; this catches it in core. Drop
     * the per-tick present(trail) and the count falls to zero.
     */
    @Test
    void throwEmbersLeavesATrailEveryTickOfFlight() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        world.entities.add(caster);

        var embers = new EffectSpec.ThrowEmbers(List.of(0.0), 0.6, 0.25, "blaze_powder", 5,
                new EffectSpec.Burst(4.0, List.of(new EffectSpec.Damage(8, "fire"))), null, "ember_trail");

        new EffectApplier(world).applyAll(List.of(embers), caster.asCaster(), null, Vec3.ZERO);
        world.advanceTicks(5);

        long trailCount = world.presented.stream().filter("ember_trail"::equals).count();
        assertTrue(trailCount > 1,
                "the trail is emitted every tick of flight, not just once; got " + trailCount);
    }

    @Test
    void burstRejectsANonPositiveRadius() {
        List<EffectSpec.Targeted> effects = List.of(new EffectSpec.Damage(6, "fire"));

        var zero = assertThrows(IllegalArgumentException.class,
                () -> new EffectSpec.Burst(0, effects));
        assertTrue(zero.getMessage().contains("radius"), zero.getMessage());
    }

    /**
     * tickArea computes next = elapsed + tickInterval and reschedules while
     * next <= durationTicks. With tickInterval 0 that condition never fails, so the
     * area reschedules itself forever at zero delay. Reject it at construction.
     */
    @Test
    void areaRejectsANonPositiveTickInterval() {
        List<EffectSpec.Targeted> effects = List.of(new EffectSpec.Damage(2, "fire"));

        var zero = assertThrows(IllegalArgumentException.class,
                () -> new EffectSpec.Area(4.0, 100, 0, effects));
        assertTrue(zero.getMessage().contains("tick_interval"), zero.getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> new EffectSpec.Area(4.0, 100, -20, effects));
    }

    @Test
    void areaRejectsANonPositiveRadius() {
        List<EffectSpec.Targeted> effects = List.of(new EffectSpec.Damage(2, "fire"));

        var zero = assertThrows(IllegalArgumentException.class,
                () -> new EffectSpec.Area(0, 100, 20, effects));
        assertTrue(zero.getMessage().contains("radius"), zero.getMessage());

        assertThrows(IllegalArgumentException.class,
                () -> new EffectSpec.Area(-1.0, 100, 20, effects));
    }

    /**
     * Knockback is DECLARED, not default. An on-hit with no {@code Knockback} effect pushes the
     * target NOWHERE -- the common case (a quarter of weapons, Mage staves especially, deal none),
     * so absence must mean zero, never a sneaked-in fallback. Declared, it applies exactly that
     * strength, away from the origin.
     *
     * Mutation: a default knockback when none is declared -> knockbackCalls on the no-KB target
     * goes to 1 -> reddens.
     */
    @Test
    void knockbackIsAppliedOnlyWhenDeclared() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var noKb = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var withKb = new FakeWorld.Dummy(new Vec3(2, 0, 0));
        var applier = new EffectApplier(world);

        // Damage only -- the Mage case: hurt, but not moved.
        applier.applyAll(List.of(new EffectSpec.Damage(6, "fire")), caster.asCaster(), pair(noKb), Vec3.ZERO);
        assertEquals(0, noKb.knockbackCalls, "no Knockback effect declared -> no knockback (default is none)");

        // Damage + declared Knockback -- the Melee case: hurt AND shoved.
        applier.applyAll(List.of(new EffectSpec.Damage(6, "fire"), new EffectSpec.Knockback(1.5)),
                caster.asCaster(), pair(withKb), Vec3.ZERO);
        assertEquals(1, withKb.knockbackCalls, "a declared Knockback applies exactly one push");
        assertEquals(1.5, withKb.lastKnockbackStrength, 1e-9, "at the declared strength");
    }

    /** And a targeted effect still lands when there is one. */
    @Test
    void targetedEffectsStillApplyWhenTargetPresent() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var applier = new EffectApplier(world);

        applier.applyAll(List.of(
                new EffectSpec.Damage(12, "fire"),
                new EffectSpec.Status("scorch", 40, 0)), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(88, target.health, 0.001);
        assertEquals(List.of("scorch"), target.statuses);
    }

    /**
     * Damage names its culprit. The port carries the caster's id, never the caster, so a
     * grenade that outlives its thrower still knows whom to credit.
     */
    @Test
    void damageIsAttributedToTheCaster() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Damage(12, "fire")),
                caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(caster.id(), target.lastDamageSource, "the caster must be blamed");
        assertEquals(88, target.health, 0.001, "12 damage, no element multiplier");
    }

    /**
     * Element is identity, not math. The same amount deals the same damage whatever the
     * element -- there is no multiplier, no shield, no triangle. This is the subtraction's
     * proof: re-introduce a multiplier and these two numbers diverge.
     */
    @Test
    void damageIsTheAmountRegardlessOfElement() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var solarTarget = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var voidTarget = new FakeWorld.Dummy(new Vec3(2, 0, 0));

        var applier = new EffectApplier(world);
        applier.applyAll(List.of(new EffectSpec.Damage(10, "fire")),
                caster.asCaster(), pair(solarTarget), Vec3.ZERO);
        applier.applyAll(List.of(new EffectSpec.Damage(10, "void")),
                caster.asCaster(), pair(voidTarget), Vec3.ZERO);

        assertEquals(90, solarTarget.health, 1e-9);
        assertEquals(voidTarget.health, solarTarget.health, 1e-9, "element must not touch the number");
    }

    /**
     * WeaponDamage deals the CASTER'S attack-damage stat -- not a literal -- so the swing and the
     * tooltip share one number. The amount rides the Caster, frozen from the caster's snapshot at
     * cast time; there is no longer any world method that could resolve it at hit time.
     */
    @Test
    void weaponDamageDealsTheCastersAttackStat() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 8.0;                         // the caster's resolved ATTACK_DAMAGE

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(92, target.health, 1e-9, "the hit dealt the caster's attack stat (8), not a literal");
        assertEquals(caster.id(), target.lastDamageSource, "attributed to the caster");
        // Mutation: read the TARGET's frozen stat instead of the caster's -> the number is wrong -> reddens.
    }

    /**
     * The caster's stat, never the target's. Both carry one, and they differ here on purpose: a
     * hit must deal what the SWINGER has, not what the thing being swung at happens to have. With
     * both values non-zero the amt>0 guard cannot mask a mix-up, so only the number can tell them
     * apart -- which is exactly what makes this reddening.
     */
    @Test
    void weaponDamageReadsTheCasterNotTheTarget() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 8.0;
        target.attackDamage = 30.0;                        // a well-armed victim changes nothing

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(92, target.health, 1e-9, "the caster's 8, not the target's 30");
        // Mutation: read target.state().attackDamage() -> "expected: <92> but was: <70>" -> reddens.
    }

    /**
     * Weapon-only melee: an unarmed caster (attack 0, or untracked) deals NOTHING and fires no damage
     * seam at all -- the amt>0 guard. This is why base attack is 0 for players.
     */
    @Test
    void weaponDamageWithZeroAttackDealsNothingAndDoesNotFireTheSeam() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);       // attackDamage left at 0 -> untracked/unarmed
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(100, target.health, 1e-9, "unarmed (0 attack) deals nothing");
        assertEquals(0, target.damageCalls, "and does not fire a spurious 0-damage seam");
        // Mutation: drop the amt>0 guard -> applyDamage(0,..) fires, damageCalls == 1 -> reddens.
    }

    /** Splash damage carries the same culprit, and still never splashes the caster. */
    @Test
    void burstDamageCarriesTheCasterIdToEveryVictim() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var victim = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(caster);
        world.entities.add(victim);

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Burst(4.0, List.of(new EffectSpec.Damage(6, "fire")))),
                caster.asCaster(), null, Vec3.ZERO);

        assertEquals(caster.id(), victim.lastDamageSource);
        assertNull(caster.lastDamageSource, "a burst never splashes its own caster");
    }

    // --- Class-typed damage modifiers --------------------------------------------------------------

    /**
     * The WeaponDamage arm adds the caster's class-damage bonus ON TOP of the attack stat. The two
     * numbers are deliberately different (8 and 5) so neither can stand in for the other, and the
     * total (13) is reachable by no single one of them.
     */
    @Test
    void weaponDamageAddsTheClassDamageBonusOnTopOfTheAttackStat() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 8.0;          // the sword's inherent damage
        caster.classDamageBonus = 5.0;      // +5 Melee gear, active because a melee weapon is held

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(87, target.health, 1e-9, "8 inherent + 5 gear = 13, not 8 and not 5");
        // Mutation: drop the + caster.classDamageBonus() addend -> 92 -> reddens.
    }

    /**
     * THE ARM THAT RETIRES THE "+MAGIC DAMAGE HAS NOTHING TO GRIP" BLOCKER.
     *
     * A LITERAL Damage effect -- the shape ember_staff's Ember Bolt carries, on a weapon that
     * declares attack_damage 0 and reads no stat at all -- receives the class bonus too. Before this
     * pass a class-typed modifier keyed on the attack-damage stat would have been silently inert
     * here, which is exactly what NEXT.md forbade shipping.
     *
     * Note the caster's attackDamage stays 0: the literal's number comes from content, and the bonus
     * is the ONLY stat contribution. So a green here cannot be explained by the WeaponDamage path.
     */
    @Test
    void aLiteralDamageAlsoReceivesTheClassDamageBonus() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.classDamageBonus = 5.0;      // +5 Magic gear, active because a mage weapon is held
        // caster.attackDamage deliberately left 0 -- a staff declares attack_damage: 0

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Damage(16, "fire")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(79, target.health, 1e-9, "the authored 16 is a BASE; gear adds 5 on top");
        // Mutation: drop the addend from the Damage arm -> 84 -> reddens. This is the test that
        // fails if the pass is quietly narrowed back to basic attacks only.
    }

    /**
     * The bonus reaches a literal nested inside a BURST -- the emberblade's fireball shape, and the
     * staff's. This needs no recursion code of its own: Burst reaches damage only through
     * applyTargeted, carrying the same frozen Caster, so changing the leaf covered every nesting.
     * The test exists to prove that claim rather than assert it in a comment.
     */
    @Test
    void aLiteralNestedInABurstReceivesTheClassDamageBonus() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var victim = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(caster);
        world.entities.add(victim);
        caster.classDamageBonus = 5.0;

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Burst(4.0, List.of(new EffectSpec.Damage(12, "fire")))),
                caster.asCaster(), null, Vec3.ZERO);

        assertEquals(83, victim.health, 1e-9, "12 + 5 through the burst's nested effect");
        assertEquals(100, caster.health, 1e-9, "and a burst still never splashes its own caster");
    }

    /**
     * The regression pin: with no class gear, every number is exactly what it was before this pass.
     * Both arms, one test. If the bonus ever stops defaulting to 0 -- a base other than 0.0 on the
     * stat, a neutral-value mix-up with attack speed's 1.0 -- this is what catches it.
     */
    @Test
    void withNoClassGearBothArmsDealExactlyWhatTheyDealtBefore() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var weaponTarget = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var literalTarget = new FakeWorld.Dummy(new Vec3(2, 0, 0));
        caster.attackDamage = 8.0;          // classDamageBonus left at 0

        var applier = new EffectApplier(world);
        applier.applyAll(List.of(new EffectSpec.WeaponDamage("kinetic")),
                caster.asCaster(), pair(weaponTarget), Vec3.ZERO);
        applier.applyAll(List.of(new EffectSpec.Damage(12, "fire")),
                caster.asCaster(), pair(literalTarget), Vec3.ZERO);

        assertEquals(92, weaponTarget.health, 1e-9, "the attack stat alone");
        assertEquals(88, literalTarget.health, 1e-9, "the authored literal alone");
        // Mutation: default classDamageBonus to AttackSpeed.BASE (1.0), copying the divisor's
        // neutral -> 91 and 87 -> reddens. A summand's absent value is 0, not 1.
    }

    /**
     * A net-zero total fires NO seam, on the LITERAL arm. This is the guard the class-modifier pass
     * added to the Damage arm, which previously checked only alive(): Stat permits negative
     * modifiers, so a future "-N <Class> Damage" curse could drive a literal to zero or below, and
     * applyDamage would otherwise push a negative amount into the HealthChange seam and the damage
     * popup while HealthState.damage silently no-ops it.
     */
    @Test
    void aLiteralCancelledOutByANegativeBonusFiresNoDamageSeam() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.classDamageBonus = -12.0;    // a curse exactly cancelling the literal below

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Damage(12, "fire")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(100, target.health, 1e-9, "nothing dealt");
        assertEquals(0, target.damageCalls, "and no 0-or-negative seam fired into the popup");
        // Mutation: revert the Damage arm to the pre-pass `if (target.state().alive())` -> the seam
        // fires with amount 0 -> damageCalls == 1 -> reddens.
    }

    /**
     * The bonus cannot resurrect an unarmed swing. Belt-and-braces on top of the structural gate in
     * ClassDamageModifiers (a null held class yields no grants at all): even if a bonus somehow
     * reached a caster with no weapon, the resolved TOTAL is what the amt>0 guard reads.
     */
    @Test
    void aNegativeTotalOnTheWeaponArmDealsNothingAndFiresNoSeam() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 0.0;
        caster.classDamageBonus = -3.0;     // a total of -3: no hit, no seam

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(100, target.health, 1e-9, "a negative total deals nothing");
        assertEquals(0, target.damageCalls, "and fires no seam");
    }

    // --- Damage-modifier enchants (Sharpness / Power / Attunement) ----------------------------------

    /**
     * THE ORDERING TEST, and the one whose NUMBER distinguishes two designs rather than merely
     * confirming a change happened.
     *
     * The rule is percent on the WEAPON'S BASE, flat gear bonus on top:
     * {@code 8 * 1.15 + 5 = 14.2}. The rival design -- multiply the sum -- gives
     * {@code (8 + 5) * 1.15 = 14.95}. Both are "the enchant and the bonus both applied", so a test
     * asserting only "more than 8" would pass on either. This asserts the number that tells them
     * apart, the same way the class-damage pass's boot record turned on 17 rather than 24.
     *
     * If this ever reads 14.95, the multiply has been moved outside the addition.
     */
    @Test
    void theEnchantPercentMultipliesTheWeaponBaseAndTheClassBonusIsAddedAfter() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 8.0;              // the ironblade's inherent damage
        caster.classDamageBonus = 5.0;          // +5 Melee gear
        caster.enchantDamagePercent = 15.0;     // Sharpness III on that sword

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(85.8, target.health, 1e-9, "8*1.15 + 5 = 14.2, NOT (8+5)*1.15 = 14.95");
        // Mutation: (attackDamage + classDamageBonus) * multiplier -> 85.05 -> reddens.
    }

    /**
     * A LITERAL Damage is multiplied too -- the ember_staff's Ember Bolt, on a weapon that reads no
     * stat at all. This is what Attunement exists to do, and it is the reason the multiplier is
     * applied AT THE ARM rather than pre-baked into the caster's attackDamage at projection: the
     * literal's amount is not known until the effect fires, so a pre-baked multiplier could never
     * have reached it.
     *
     * attackDamage stays 0, so a green here cannot be explained by the WeaponDamage path.
     */
    @Test
    void aLiteralDamageIsMultipliedByTheEnchantPercentToo() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.enchantDamagePercent = 15.0;     // Attunement III on the staff
        // caster.attackDamage deliberately left 0 -- a staff declares attack_damage: 0

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Damage(16, "fire")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(81.6, target.health, 1e-9, "16 * 1.15 = 18.4");
        // Mutation: drop the multiplier from the literal arm -> 84 -> reddens.
    }

    /**
     * A literal nested inside a Burst is multiplied, so every nesting shape is covered by changing
     * the two leaves. Burst and Area reach damage only through applyTargeted carrying the same
     * frozen Caster, and this proves that rather than asserting it in a comment -- the same argument
     * {@code aLiteralNestedInABurstReceivesTheClassDamageBonus} makes for the class bonus.
     */
    @Test
    void aLiteralNestedInABurstIsMultipliedByTheEnchantPercent() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var victim = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(caster);
        world.entities.add(victim);
        caster.enchantDamagePercent = 15.0;

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Burst(4.0, List.of(new EffectSpec.Damage(12, "fire")))),
                caster.asCaster(), null, Vec3.ZERO);

        assertEquals(86.2, victim.health, 1e-9, "12 * 1.15 = 13.8 through the burst's nested effect");
        assertEquals(100, caster.health, 1e-9, "and a burst still never splashes its own caster");
    }

    /**
     * THE REGRESSION PIN. With no enchant, both arms deal EXACTLY what they dealt before this pass.
     *
     * This is the assertion that makes 0.0 the safe default for the new stat, and it is why the stat
     * carries a percent rather than a multiplier: a multiplier-valued field defaulting to 0.0 would
     * have zeroed both of these instead of leaving them alone, and every one of the 311 tests that
     * predate this pass relies on that neutral being right.
     */
    @Test
    void withNoEnchantBothArmsDealExactlyWhatTheyDealtBefore() {
        var world = new FakeWorld();
        var weaponCaster = new FakeWorld.Dummy(Vec3.ZERO);
        var weaponTarget = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        weaponCaster.attackDamage = 8.0;
        // enchantDamagePercent deliberately left 0.0

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), weaponCaster.asCaster(),
                pair(weaponTarget), Vec3.ZERO);
        assertEquals(92, weaponTarget.health, 1e-9, "an unenchanted sword still deals exactly 8");

        var literalCaster = new FakeWorld.Dummy(Vec3.ZERO);
        var literalTarget = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Damage(16, "fire")), literalCaster.asCaster(),
                pair(literalTarget), Vec3.ZERO);
        assertEquals(84, literalTarget.health, 1e-9, "an unenchanted staff still deals exactly 16");
        // Mutation: multiplier() returns percent/100 (drop the 1 +) -> both deal 0 -> reddens.
    }

    /**
     * The {@code amount > 0} rule survives the multiply. A -100% percent cancels the weapon's whole
     * base, and the arm must deal nothing and fire NO seam rather than push a 0 into the HealthChange
     * seam and the damage popup.
     *
     * Not reachable from shipped content -- the loader refuses a negative percent -- and pinned
     * because the guard it protects is the one {@code aLiteralCancelledOutByANegativeBonusFiresNoSeam}
     * already protects from the additive side, and a multiply is a new way to reach zero.
     */
    @Test
    void aPercentThatCancelsTheBaseDealsNothingAndFiresNoSeam() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 8.0;
        caster.enchantDamagePercent = -100.0;   // x0.0

        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(100, target.health, 1e-9, "a cancelled base deals nothing");
        assertEquals(0, target.damageCalls, "and fires no seam");
    }

    /**
     * WHERE the charge multiplies, which is a balance decision disguised as an arithmetic one.
     *
     * The fixture is the only shape that can tell the two candidates apart: a weapon base AND an
     * enchant percent AND a flat class bonus, all non-zero. With any of them zero both placements
     * produce the same number and the test proves nothing.
     *
     *   scale the WHOLE amount:  (8 * 1.15 + 5) * 0.25 = 3.55   -> health 96.45   <- ours
     *   scale the BASE only:     (8 * 0.25) * 1.15 + 5 = 7.30   -> health 92.70
     *
     * Both values were produced by EXECUTING the expressions, not by doing the algebra here. They
     * differ by 3.75 -- more than twice the damage -- because scaling only the base leaves the flat
     * class bonus as a spam-proof floor. With enough +N Melee gear that floor makes fast weak
     * swings out-damage timed ones, which inverts the entire charge model.
     */
    @Test
    void chargeScalesTheWholeAmountAndNotJustTheWeaponBase() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 8.0;
        caster.enchantDamagePercent = 15.0;   // Sharpness III
        caster.classDamageBonus = 5.0;        // +5 Melee gear

        new EffectApplier(world).applyAll(List.of(new EffectSpec.WeaponDamage("kinetic")),
                caster.asCaster(0.25), pair(target), Vec3.ZERO);

        assertEquals(96.45, target.health, 1e-9,
                "a quarter-charged swing deals a quarter of the WHOLE amount");
        // Mutation: move * chargeScale() inside the parens (scaling only the base) -> 92.70 -> reddens.
        // Mutation: delete * chargeScale() entirely -> 85.80 -> reddens.
    }

    /**
     * The regression that protects every OTHER caller. Abilities, projectiles and areas all build
     * their Caster through the no-charge factory, which passes FULL_CHARGE -- so the day the charge
     * factor landed, none of their numbers were allowed to move.
     *
     * This is safe to assert tightly because AttackCharge.scale(1.0) == 1.0 exactly in binary
     * floating point, which AttackChargeTest pins by execution rather than by algebra. If that
     * identity were merely approximate, every ability in the game would have drifted here.
     */
    @Test
    void aFullChargeSourceDealsExactlyWhatItDidBeforeChargeExisted() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 8.0;
        caster.enchantDamagePercent = 15.0;
        caster.classDamageBonus = 5.0;

        new EffectApplier(world).applyAll(List.of(new EffectSpec.WeaponDamage("kinetic")),
                caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(85.80, target.health, 1e-9, "8 * 1.15 + 5 = 14.2, untouched by the charge factor");
        // Mutation: make the no-arg Caster.of pass anything but FULL_CHARGE -> reddens, and with it
        // every ability, projectile and lingering area in the game.
    }

    // --- The direct-damage seam, which the sweep rider reads ---

    /**
     * The seam reports the number that was DEALT, not a number recomputed from the caster.
     *
     * The whole point of reporting from the arm is that the sweep rider cannot drift from the
     * primary hit: whatever the enchant, the class bonus and the charge produced here IS what a
     * swept mob takes a fraction of. So the assertion ties the reported value to the target's lost
     * health rather than to a literal -- if the two ever disagree, the seam has stopped observing
     * and started predicting.
     */
    @Test
    void theSeamReportsExactlyWhatTheDamageArmDealt() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 8.0;              // the ironblade
        caster.classDamageBonus = 5.0;          // +5 Melee gear
        caster.enchantDamagePercent = 15.0;     // Sharpness III
        List<Double> reported = new ArrayList<>();

        new EffectApplier(world, reported::add).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(1, reported.size(), "one damage effect, one report");
        assertEquals(14.2, reported.get(0), 1e-9, "8*1.15 + 5 -- the ordering test's number");
        assertEquals(100.0 - target.health, reported.get(0), 1e-9,
                "the reported figure IS the health the target lost");
        // Mutation: report caster.attackDamage instead of amount -> 8.0, and the swept mob silently
        // loses the enchant and the class bonus while the primary keeps them -> reddens.
    }

    /** The charge reaches the seam too, so a weak swing reports a weak number for sweep to halve. */
    @Test
    void theSeamCarriesTheChargeSoAnEarlySwingSweepsWeakly() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 8.0;
        List<Double> reported = new ArrayList<>();

        new EffectApplier(world, reported::add).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")),
                caster.asCaster(AttackCharge.scale(0.0)), pair(target), Vec3.ZERO);

        assertEquals(1, reported.size());
        assertEquals(1.6, reported.get(0), 1e-9, "8 at the uncharged floor of 0.2");
        // Mutation: report the amount BEFORE the chargeScale multiply -> 8.0, and a spammed swing
        // would sweep as hard as a timed one -> reddens.
    }

    /**
     * A REFUSED hit reports nothing, and this is the test that makes "a swing that dealt nothing
     * sweeps nothing" true rather than hoped for.
     *
     * The seam sits INSIDE the {@code amount > 0 && alive()} gate. Were it moved one line out --
     * beside the gate rather than within it -- a swing at a corpse would still report a number, the
     * sweep rider would stash it, and bystanders would take damage from a hit that never landed.
     */
    @Test
    void aRefusedHitReportsNothingAtAll() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        List<Double> reported = new ArrayList<>();

        var dead = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        dead.health = 0.0;                      // already gone: the alive() half of the gate
        caster.attackDamage = 8.0;
        new EffectApplier(world, reported::add).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), caster.asCaster(), pair(dead), Vec3.ZERO);
        assertTrue(reported.isEmpty(), "a hit on a corpse reports nothing");

        var live = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 0.0;              // unarmed: the amount > 0 half of the gate
        new EffectApplier(world, reported::add).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), caster.asCaster(), pair(live), Vec3.ZERO);
        assertTrue(reported.isEmpty(), "an unarmed swing reports nothing");
        assertEquals(100.0, live.health, 1e-9, "and dealt nothing, which is why it must report nothing");
        // Mutation: move onDirectDamage.accept OUTSIDE the if -> both report, the sweep rider stashes
        // a phantom number, and a swing that dealt nothing sweeps bystanders -> reddens.
    }

    /** Every caller that does not ask for the seam is untouched: the default is a no-op, not a null. */
    @Test
    void theDefaultConstructorNeedsNoListenerAndStillDeals() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 8.0;

        assertDoesNotThrow(() -> new EffectApplier(world).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), caster.asCaster(), pair(target), Vec3.ZERO));
        assertEquals(92.0, target.health, 1e-9, "and still dealt its 8");
        // Mutation: default the consumer to null instead of a no-op -> NullPointerException on every
        // ability cast in the game -> reddens.
    }

    // --- Crit: one frozen multiplier, applied to every damage arm alike ---

    @Test
    void aCritDoublesTheWeaponArmAndTheSeamReportsTheDoubledNumber() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 8.0;
        caster.critMultiplier = Crit.multiplier(Crit.BASE_CHANCE, Crit.BASE_DAMAGE, 0.0);  // rolled a crit
        List<Double> reported = new ArrayList<>();

        new EffectApplier(world, reported::add).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(16.0, reported.get(0), 1e-9, "8 x 2.0");
        assertEquals(84.0, target.health, 1e-9);
        assertTrue(target.lastDamageWasCrit, "the port carries the crit bit for the popup and particle");
        // Mutation: drop * caster.critMultiplier() from the WeaponDamage arm -> 8.0 dealt, and crit
        // silently does nothing to a basic swing while still flashing yellow -> reddens.
    }

    @Test
    void aCritMultipliesAnAbilityLiteralToo() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.critMultiplier = 2.0;
        // attackDamage stays 0, so a green here cannot be explained by the WeaponDamage path.
        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.Damage(12, "fire")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(76.0, target.health, 1e-9, "12 x 2.0 = 24 off 100");
        // Mutation: apply the multiplier in the WeaponDamage arm only -> the staff's bolt never crits
        // while the sword does, with nothing saying so -> reddens.
    }

    @Test
    void aNonCritIsAnExactIdentityOnTheEightyFivePercentOfSwingsThatDoNotCrit() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 8.0;
        // Default critMultiplier is Crit.NO_CRIT. Exact equality, not EPS: an approximate identity
        // here would drift every non-crit hit in the game by a rounding error.
        new EffectApplier(world).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(92.0, target.health, "8 dealt, untouched by a multiplier of exactly 1.0");
        assertFalse(target.lastDamageWasCrit, "and the seam says it was not a crit");
        // Mutation: make NO_CRIT 1.0000001 -> the assertion is exact, so it reddens; with an EPS it
        // would not, which is why this one row is written without a tolerance.
    }

    /**
     * The crit rides the SAME number sweep takes a fraction of, which is how sweep inherits crit
     * without a roll of its own.
     *
     * The sweep rider stashes what the seam reports and deals SweepShare.of(that, fraction). So
     * asserting the seam reports the crit-multiplied figure IS the proof that a swept mob takes half
     * of a CRIT primary rather than half of an unmultiplied one -- the paper half is one multiply
     * away and has no second roll to get wrong.
     */
    @Test
    void theSeamReportsTheCritMultipliedNumberSoSweepInheritsTheCrit() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 8.0;
        caster.classDamageBonus = 5.0;
        caster.enchantDamagePercent = 15.0;
        caster.critMultiplier = 2.0;
        List<Double> reported = new ArrayList<>();

        new EffectApplier(world, reported::add).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")), caster.asCaster(), pair(target), Vec3.ZERO);

        assertEquals(28.4, reported.get(0), 1e-9, "(8*1.15 + 5) * 2.0 -- the crit lands LAST");
        assertEquals(14.2, SweepShare.of(reported.get(0), 0.5), 1e-9,
                "and a swept mob takes half of THAT, not half of the uncritted 14.2");
        // Mutation: apply the crit before the class bonus -- (8*1.15)*2 + 5 = 23.4 -> reddens, and
        // pins the ordering the way the enchant/class-bonus test pins theirs.
    }

    @Test
    void theChargeAndTheCritBothApplyAndNeitherEatsTheOther() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var target = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        caster.attackDamage = 8.0;
        caster.critMultiplier = 2.0;
        List<Double> reported = new ArrayList<>();

        new EffectApplier(world, reported::add).applyAll(
                List.of(new EffectSpec.WeaponDamage("kinetic")),
                caster.asCaster(AttackCharge.scale(0.5)), pair(target), Vec3.ZERO);

        assertEquals(6.4, reported.get(0), 1e-9, "8 at half charge is 3.2, critting is 6.4");
        // Mutation: replace * chargeScale * critMultiplier with * critMultiplier -> 16.0, a badly
        // timed crit hitting as hard as a perfect one -> reddens.
    }
}
