package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.CastTunes;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.TracedHit;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour.CastTune.Quantity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * THE ONE SEAM FOR NUMERIC CHANGES TO A WEAPON'S OWN CAST (the seat's G4 and G5, 2026-11-24): {@code CastTune} and
 * {@code CastTunes}, through the real {@code CastExecutor} on the fake world. Each row names the quantity it moves. The
 * fixtures copy the shipped right clicks' numbers (Flint Staff 20 fire; Lapis Staff a ray of 26 for 21; Cursed Emerald a
 * volley of 20 / 3 / 2 of a 32 ray for 27; Knell Beam a locked volley of 24 / 1). The fake's ray meets a body whose
 * POSITION lies within {@code hitRadius} of the segment (FocusedBeamTest's note), so every mob stands at his eye height.
 */
class CastTuneTest {

    private static final double X0 = 4, Z = 8, EYE = 1.62;

    private static EnchantBehaviour.CastTune tune(String weapon, Quantity q, double... values) {
        return new EnchantBehaviour.CastTune(weapon, q, java.util.Arrays.stream(values).boxed().toList());
    }

    private static AbilityDefinition ray(String id, double range, double amount, String element) {
        return new AbilityDefinition(id, "Bolt", element, 20, new ResourceCost("mana", 5),
                new CastSpec.Ray(range, "beam"), List.of(new EffectSpec.Damage(amount, element)), List.of());
    }

    private static AbilityDefinition emerald() {
        return new AbilityDefinition("cursed_emerald/right_click", "Cursed Volley", "kinetic", 30,
                new ResourceCost("mana", 40), new CastSpec.Volley(20, 3, 2, new CastSpec.Ray(32, "emerald_beam")),
                List.of(new EffectSpec.Damage(27, "kinetic")), List.of());
    }

    private static AbilityDefinition knell() {
        return new AbilityDefinition("knell_beam/right_click", "Knell's Toll", "void", 48, new ResourceCost("mana", 13),
                new CastSpec.Volley(24, 1, 1, new CastSpec.Ray(40, "knell_toll_line"), true,
                        new CastSpec.Volley.Warning("knell_warning", List.of(0))),
                List.of(new EffectSpec.Damage(63, "void")), List.of());
    }

    private record Stage(FakeWorld world, FakeWorld.Dummy him) {}

    private static FakeWorld.Dummy mob(double out) {
        var m = new FakeWorld.Dummy(new Vec3(X0 + out, EYE, Z));
        m.health = 1000;
        return m;
    }

    private static Stage stage(List<ActiveBehaviour> behaviours, FakeWorld.Dummy... mobs) {
        var world = new FakeWorld();
        world.hitRadius = 0.3;
        var him = new FakeWorld.Dummy(new Vec3(X0, 0, Z));
        him.player = true;
        him.behaviours = behaviours;
        world.entities.add(him);
        world.entities.addAll(List.of(mobs));
        return new Stage(world, him);
    }

    private static void press(Stage s, AbilityDefinition ability) {
        Aim aim = s.world().aimOf(s.him().id()).orElseThrow();
        new CastExecutor(s.world()).execute(new AbilityService.CastResult.Success(ability, s.him().snapshot(), aim, true));
    }

    private static List<Double> sent(Stage s, FakeWorld.Dummy target) {
        return s.world().traced.stream().filter(h -> h.targetId().equals(target.id())).map(TracedHit::sent).toList();
    }

    private static List<ActiveBehaviour> active(String id, int level, EnchantBehaviour.CastTune t) {
        return List.of(new ActiveBehaviour(id, level, t));
    }

    // ---- DAMAGE: a flat addition to the AUTHORED amount, before the gear score (G5) ---------------------------------

    /**
     * Large Sparks III at score 150: (20 + 12) x 1.5 = 48. Added AFTER the score it would read 20 x 1.5 + 12 = 42; with no
     * enchant, 20 x 1.5 = 30. Three different numbers, so the term's position is what the row reads.
     */
    @Test
    void aDamageTuneAddsToTheAuthoredAmountBeforeTheScore() {
        var m = mob(10);
        Stage s = stage(active("large_sparks", 3, tune("flint_staff", Quantity.DAMAGE, 4, 8, 12)), m);
        s.him().triggerScore = 150;
        press(s, ray("flint_staff/right_click", 32, 20, "fire"));
        assertEquals(List.of(48.0), sent(s, m));

        var c = mob(10);
        Stage plain = stage(List.of(), c);
        plain.him().triggerScore = 150;
        press(plain, ray("flint_staff/right_click", 32, 20, "fire"));
        assertEquals(List.of(30.0), sent(plain, c), "the control: no enchant, 20 x 1.5");
    }

    /**
     * SCORCH'S CAP BASIS IS THE TUNED HIT: the status a Large Sparks III bolt applies carries the headline damage 32 (20 +
     * 12), not the authored 20, so the burn is capped by what the cast now deals (half of it, in paper's applier).
     */
    @Test
    void aStatusTheTunedCastAppliesCarriesTheTunedHeadline() {
        var m = mob(10);
        Stage s = stage(active("large_sparks", 3, tune("flint_staff", Quantity.DAMAGE, 4, 8, 12)), m);
        press(s, new AbilityDefinition("flint_staff/right_click", "Bolt", "fire", 20, new ResourceCost("mana", 5),
                new CastSpec.Ray(32, "beam"),
                List.of(new EffectSpec.Damage(20, "fire"), new EffectSpec.Status("scorch", 0, 0)), List.of()));
        assertEquals(32.0, m.lastStatusSourceDamage, "the tuned headline: 20 + 12");
    }

    /** Each level its own addition: Sharp Crystals' +3 / +6 / +9 on 16 at score 100. */
    @Test
    void eachLevelsAddition() {
        for (int level = 1; level <= 3; level++) {
            var m = mob(10);
            Stage s = stage(active("sharp_crystals", level, tune("crystal_shard_staff", Quantity.DAMAGE, 3, 6, 9)), m);
            press(s, ray("crystal_shard_staff/right_click", 32, 16, "kinetic"));
            assertEquals(List.of(16.0 + 3 * level), sent(s, m), "level " + level);
        }
    }

    /** A tune acts only on its own weapon's right click: the same state on another weapon's cast changes nothing. */
    @Test
    void anotherWeaponsCastIsUntouched() {
        var m = mob(10);
        Stage s = stage(active("large_sparks", 3, tune("flint_staff", Quantity.DAMAGE, 4, 8, 12)), m);
        press(s, ray("crystal_shard_staff/right_click", 32, 16, "kinetic"));
        assertEquals(List.of(16.0), sent(s, m));
        AbilityDefinition other = ray("flint_staff/left_click", 32, 20, "fire");
        assertSame(other, CastTunes.tune(other, active("large_sparks", 3, tune("flint_staff", Quantity.DAMAGE, 4, 8, 12))),
                "another gesture of the same weapon is untouched too");
    }

    // ---- RANGE: an addition to a ray's range ------------------------------------------------------------------------

    /** Polished Scepter III: the Lapis Beam's 26 becomes 38, so a mob 36 out takes the 21; with none, nothing. */
    @Test
    void aRangeTuneLengthensTheRay() {
        var m = mob(36);
        Stage s = stage(active("polished_scepter", 3, tune("lapis_staff", Quantity.RANGE, 4, 8, 12)), m);
        press(s, ray("lapis_staff/right_click", 26, 21, "kinetic"));
        s.world().advanceTicks(5);
        assertEquals(List.of(21.0), sent(s, m));

        var c = mob(36);
        Stage plain = stage(List.of(), c);
        press(plain, ray("lapis_staff/right_click", 26, 21, "kinetic"));
        plain.world().advanceTicks(5);
        assertEquals(List.of(), sent(plain, c), "the control: 26 does not reach 36");
    }

    // ---- SHOTS: a volley's shot count --------------------------------------------------------------------------------

    /** Echo Beam I / II / III: four, five, six bolts of 27; none, the plain three. */
    @Test
    void aShotsTuneSetsTheVolleyCount() {
        var c = mob(10);
        Stage plain = stage(List.of(), c);
        press(plain, emerald());
        plain.world().advanceTicks(60);
        assertEquals(3, sent(plain, c).size(), "the plain volley: Ben's 3");
        for (int level = 1; level <= 3; level++) {
            var m = mob(10);
            Stage s = stage(active("echo_beam", level, tune("cursed_emerald", Quantity.SHOTS, 4, 5, 6)), m);
            press(s, emerald());
            s.world().advanceTicks(60);
            assertEquals(java.util.Collections.nCopies(3 + level, 27.0), sent(s, m), "level " + level);
        }
    }

    /**
     * THE COUNT IS READ AT EACH SHOT (ruling 1: the weapon in hand at each shot). Echo Beam III active at the press, gone
     * before the first shot: the plain three fire. The wind-up was already running and is the press's.
     */
    @Test
    void theShotCountIsReadAtEachShot() {
        var m = mob(10);
        Stage s = stage(active("echo_beam", 3, tune("cursed_emerald", Quantity.SHOTS, 4, 5, 6)), m);
        press(s, emerald());
        s.world().advanceTicks(10);
        s.him().behaviours = List.of();
        s.world().advanceTicks(50);
        assertEquals(3, sent(s, m).size());
    }

    // ---- WINDUP: a reduction of a volley's wind-up, in ticks ---------------------------------------------------------

    /**
     * Early Toll III against none: the Knell Beam's 24-tick wind-up minus 8 is 16. Read off the VOLLEYSHOT trace (shot 0):
     * pressed at tick 5 (stamped, as {@code WeaponFire} does), cast at 5 in the fake, so the first shot's tick minus the
     * press reads 16 against 24.
     */
    @Test
    void aWindupTuneShortensTheWindup() {
        for (int[] row : new int[][] {{3, 16}, {0, 24}}) {
            var m = mob(10);
            Stage s = stage(row[0] == 0 ? List.of()
                    : active("early_toll", row[0], tune("knell_beam", Quantity.WINDUP, 2, 4, 8)), m);
            s.world().advanceTicks(5);
            press(s, knell());
            s.world().advanceTicks(40);
            assertEquals(1, s.world().volleyShots.size());
            var shot = s.world().volleyShots.getFirst();
            assertEquals(0, shot.shotIndex());
            assertEquals(5, shot.pressedTick());
            assertEquals(row[1], shot.tick() - shot.pressedTick(), "Early Toll level " + row[0]);
            assertEquals(List.of(63.0), sent(s, m));
        }
    }

    // ---- THE RECORD'S OWN REFUSALS (the loader adds the weapon's) ---------------------------------------------------

    @Test
    void aMalformedTuneIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> tune("x", Quantity.SHOTS, 4, 0.5),
                "a shot count must be a whole number of 1 or more");
        assertThrows(IllegalArgumentException.class, () -> tune("x", Quantity.WINDUP, 2.5),
                "a wind-up reduction is whole ticks");
        assertThrows(IllegalArgumentException.class, () -> tune("x", Quantity.DAMAGE, 4, -1),
                "a damage or range addition must be above 0");
        assertThrows(IllegalArgumentException.class, () -> tune("x", Quantity.RANGE));
        assertThrows(IllegalArgumentException.class, () -> new EnchantBehaviour.CastTune(" ", Quantity.RANGE, List.of(1.0)));
    }

    /** G4's "a value that would make the cast invalid": a wind-up cut below 0 throws where it is built. */
    @Test
    void aWindupCutBelowZeroIsRefusedWhereTheCastIsBuilt() {
        assertThrows(IllegalArgumentException.class, () -> CastTunes.tune(knell(),
                active("early_toll", 1, tune("knell_beam", Quantity.WINDUP, 25))));
    }
}
