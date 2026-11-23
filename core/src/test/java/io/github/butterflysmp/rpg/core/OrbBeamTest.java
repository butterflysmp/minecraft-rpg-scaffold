package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.Orbs;
import io.github.butterflysmp.rpg.core.combat.TracedHit;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE BEAM AND THE CRYSTAL'S SHOTS (Lapis Crystal's D3, the seat's message of 2026-11-23), through the real
 * {@link CastExecutor}'s ray walk on the fake world. Ben's 8 blocks and 20 / 30 / 40 %, the Lapis Beam's 21; the seat's
 * 0.6 radius. Everything stands in one horizontal plane (the fake's ray tests distance to a body's feet).
 *
 * <p>Him at the origin facing east; his orb resting 6 east. N 5 from the orb (inside 8), F 9 from it (outside), P a player
 * 3 from it, L 4 from it behind a wall, T the beam's own target 15 east: 9 BEHIND the orb, beyond 8, so it takes nothing
 * when the orb stops the beam, and 21 when there is no orb (the control).
 */
class OrbBeamTest {

    private static final Aim EAST = new Aim(Vec3.ZERO, new Vec3(1, 0, 0));
    private static final AbilityDefinition BEAM = beam("lapis_staff/right_click");
    private static final Orbs.Shot SHOT = new Orbs.Shot(8, List.of(20.0, 30.0, 40.0), "kinetic", null);

    private static AbilityDefinition beam(String id) {
        return new AbilityDefinition(id, "Lapis Beam", "kinetic", 0, ResourceCost.FREE,
                new CastSpec.Ray(26, null, false, 0), List.of(new EffectSpec.Damage(21, "kinetic")));
    }

    private record Stage(FakeWorld world, FakeWorld.Dummy him, FakeWorld.Dummy n, FakeWorld.Dummy f, FakeWorld.Dummy p,
                         FakeWorld.Dummy l, FakeWorld.Dummy t) {}

    private static Stage stage() {
        var world = new FakeWorld();
        world.hitRadius = 0.3;
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        var n = new FakeWorld.Dummy(new Vec3(6, 0, 5));
        var f = new FakeWorld.Dummy(new Vec3(6, 0, 9));
        var p = new FakeWorld.Dummy(new Vec3(6, 0, 3));
        p.player = true;
        var l = new FakeWorld.Dummy(new Vec3(6, 0, -4));
        var t = new FakeWorld.Dummy(new Vec3(15, 0, 0));
        world.entities.addAll(List.of(him, n, f, p, l, t));
        world.sightBlocked = (from, to) -> to.z() < -1;   // a wall between the orb and L
        return new Stage(world, him, n, f, p, l, t);
    }

    private static void orb(Stage s, UUID owner, String weaponId, int level, boolean resting) {
        var placed = s.world().orbs.place(owner, UUID.randomUUID(), new Vec3(6, 0, 0), 0.6, 300, weaponId,
                "lapis_crystal", level, SHOT);
        if (resting) s.world().orbs.arrive(owner, placed.placed().generation(), 0);
    }

    private static void fire(Stage s, AbilityDefinition ability) {
        new CastExecutor(s.world()).execute(new AbilityService.CastResult.Success(ability, s.him().snapshot(), EAST, true));
        s.world().advanceTicks(5);
    }

    private static List<TracedHit> from(Stage s, String source) {
        return s.world().traced.stream().filter(h -> source.equals(h.source())).toList();
    }

    @Test
    void aBeamIntoTheOrbStrikesEachMobWithinEightWithAClearLineAndNothingElse() {
        Stage s = stage();
        orb(s, s.him().id(), "lapis_staff", 2, true);
        fire(s, BEAM);
        assertEquals(List.of(), from(s, "lapis_staff/right_click"), "the beam stopped at the orb: it struck nothing");
        List<TracedHit> shots = from(s, "lapis_crystal/crystal");
        assertEquals(1, shots.size(), "N alone: " + shots);
        assertEquals(s.n().id(), shots.get(0).targetId());
        assertEquals(6.3, shots.get(0).sent(), 1e-9, "21 x 30 / 100, at score 100 with no other term");
        assertEquals(100.0, s.f().health, 1e-9, "9 from the orb: outside 8");
        assertEquals(100.0, s.p().health, 1e-9, "never a player");
        assertEquals(100.0, s.l().health, 1e-9, "no clear line from the orb");
        assertEquals(100.0, s.t().health, 1e-9, "the beam's own target, behind the orb and beyond 8: untouched");
        assertEquals(100.0, s.him().health, 1e-9, "never himself");
    }

    /** The control: no orb, and the beam reaches its target for 21. */
    @Test
    void withNoOrbTheBeamReachesItsTarget() {
        Stage s = stage();
        fire(s, BEAM);
        assertEquals(List.of(21.0), from(s, "lapis_staff/right_click").stream().map(TracedHit::sent).toList());
        assertEquals(79.0, s.t().health, 1e-9);
    }

    /** "The orb stays for the entire duration": a second beam fires it again. */
    @Test
    void aSecondBeamFiresItAgain() {
        Stage s = stage();
        orb(s, s.him().id(), "lapis_staff", 2, true);
        fire(s, BEAM);
        fire(s, BEAM);
        assertEquals(2, from(s, "lapis_crystal/crystal").size());
        assertEquals(100 - 2 * 6.3, s.n().health, 1e-9);
        assertTrue(s.world().orbs.of(s.him().id()).isPresent(), "not used up");
    }

    @Test
    void levelOneAndLevelThree() {
        for (int[] row : new int[][] {{1, 42}, {3, 84}}) {
            Stage s = stage();
            orb(s, s.him().id(), "lapis_staff", row[0], true);
            fire(s, BEAM);
            assertEquals(row[1] / 10.0, from(s, "lapis_crystal/crystal").get(0).sent(), 1e-9, "level " + row[0]);
        }
    }

    /** It carries the beam's terms (ruling 2): the beam's score 150 makes 6.3 into 9.45. */
    @Test
    void itCarriesTheBeamsTerms() {
        Stage s = stage();
        s.him().triggerScore = 150;
        orb(s, s.him().id(), "lapis_staff", 2, true);
        fire(s, BEAM);
        assertEquals(9.45, from(s, "lapis_crystal/crystal").get(0).sent(), 1e-9, "6.3 x 150 / 100 (EXECUTED: 9.45)");
    }

    /** What the beam must NOT meet, one row each so each is its own red: an orb still in flight. */
    @Test
    void theBeamPassesHisOrbInFlight() {
        Stage flying = stage();
        orb(flying, flying.him().id(), "lapis_staff", 2, false);
        fire(flying, BEAM);
        assertEquals(1, from(flying, "lapis_staff/right_click").size(), "in flight: the beam passes to its target");
    }

    /** Another player's orb ("His own Lapis Beam, and only his"). */
    @Test
    void theBeamPassesAnotherPlayersOrb() {
        Stage theirs = stage();
        orb(theirs, new UUID(9, 9), "lapis_staff", 2, true);
        fire(theirs, BEAM);
        assertEquals(1, from(theirs, "lapis_staff/right_click").size(), "another player's orb: his beam passes");
    }

    /** A beam that is not a trigger of the weapon that granted the orb. */
    @Test
    void anotherWeaponsBeamPassesHisOrb() {
        Stage other = stage();
        orb(other, other.him().id(), "lapis_staff", 2, true);
        fire(other, beam("cursed_emerald/right_click"));
        assertEquals(1, from(other, "cursed_emerald/right_click").size(), "not the granting weapon's beam: it passes");
        assertEquals(0, from(other, "lapis_crystal/crystal").size());
    }

    /**
     * THE 8 IS A SPHERE ON THE SERVER TOO (the shockwave's lesson): with the fixture answering as the server does (a box),
     * a mob 6 east and 6 south of the orb (8.49 away: inside the box of 8, outside the sphere) takes nothing.
     */
    @Test
    void theEightIsCutToASphereWhenTheWorldAnswersWithABox() {
        Stage s = stage();
        s.world().boxSearch = true;
        var corner = new FakeWorld.Dummy(new Vec3(12, 0, 6));
        s.world().entities.add(corner);
        orb(s, s.him().id(), "lapis_staff", 2, true);
        fire(s, BEAM);
        assertEquals(100.0, corner.health, 1e-9, "8.49 from the orb: inside the box, outside the sphere");
        assertEquals(100 - 6.3, s.n().health, 1e-9, "the control: N (5 away) still takes its shot");
    }

    /** With Kinetic Tremors active, three beams into the orb make three crystal shots on N and never a shockwave (B4). */
    @Test
    void crystalShotsAloneNeverFireAShockwave() {
        Stage s = stage();
        s.him().behaviours = List.of(new ActiveBehaviour("kinetic_tremors", 1, new EnchantBehaviour.Shockwave(3, 60, 5,
                List.of(10.0), List.of(30.0), "kinetic", null)));
        orb(s, s.him().id(), "lapis_staff", 2, true);
        for (int i = 0; i < 3; i++) fire(s, BEAM);
        assertEquals(3, from(s, "lapis_crystal/crystal").size(), "the control: three shots landed on N");
        assertEquals(0, from(s, "kinetic_tremors/shockwave").size());
    }
}
