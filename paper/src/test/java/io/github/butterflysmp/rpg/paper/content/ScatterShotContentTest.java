package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>THE SCATTER SHOT ACTUALLY LOADS, AND THE VALUES IT WAS RULED AT ARRIVE INTACT.</b>
 *
 * <p>Written in {@link VolleyFixtureTest}'s shape and for its reason: the loaders are FAIL-SOFT, so
 * a typo in {@code scatter_shot.yml} surfaces as a named, skipped file at boot and is <b>silent to
 * every other test in the suite.</b> The suite stays green with this weapon entirely absent --
 * <i>a scan that finds nothing reads like a scan that found nothing wrong</i> -- which is why the
 * first row asserts the weapon is there at all.
 *
 * <h2>*** THE ROUND TRIP IS ASSERTED IN BOTH DIRECTIONS, WHICH IS THE POINT OF THE FILE ***</h2>
 *
 * <p>A value authored, parsed, stored and read by nobody is <b>indistinguishable from one the
 * loader silently drops</b>, and both suites stay green either way. So it is not enough to assert
 * that the spread is present: the rows below assert the AUTHORED NUMBERS arrive, and the homing
 * block's ABSENCE arrives too. A loader that hardcoded a spread of seven would pass a
 * presence-only row.
 *
 * <h2>WHAT IT CANNOT SEE</h2>
 *
 * <p>Every geometric and visual question. Whether seven arrows read as seven, whether the hexagon
 * survives being aimed at the sky, whether an arrow curves toward a mob it passes. None of that is
 * reachable from here -- it is {@code GATE-scatter-shot.md}'s job, and a green run of this file is
 * not evidence for any of it.
 *
 * <p><b>This file is the only suite-side witness of the ABSENT HOMING BLOCK</b>, which is a ruling
 * rather than an omission. Mutation {@code MUT14-HOMING} authors one and its entire kill set is
 * {@link #theArrowsDoNotSeekAndTheAbsenceIsTheRuling}.
 */
class ScatterShotContentTest {

    /** Quiet: these loaders are fail-soft and log warnings, and the rows below assert on state. */
    private static Logger quiet() {
        Logger log = Logger.getLogger("ScatterShotContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.setLevel(Level.OFF);
        return log;
    }

    private static Path contentRoot() {
        try {
            var url = ScatterShotContentTest.class.getResource("/content");
            assertNotNull(url, "bundled content is missing from the test classpath entirely");
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static WeaponRegistry shippedWeapons() {
        return new WeaponLoader(quiet()).loadAll(new File(contentRoot().resolve("weapons").toString()));
    }

    /**
     * The weapon, or a failure naming the real cause.
     *
     * <p>A METHOD, not a {@code static final} field. A fixture that threw in {@code <clinit>} once
     * errored all 26 rows of {@code WeaponLoreLinesTest} with only the first report naming the
     * cause; a factory throws for the rows that call it.
     */
    private static WeaponDefinition scatterShot() {
        return shippedWeapons().find("scatter_shot")
                .orElseThrow(() -> new AssertionError(
                        "scatter_shot did not load. The loaders are fail-soft, so this is what a"
                                + " typo in scatter_shot.yml looks like from the suite: silence."));
    }

    private static TriggerBinding rightClick() {
        return scatterShot().trigger("right_click")
                .orElseThrow(() -> new AssertionError("scatter_shot has no right_click trigger"));
    }

    private static CastSpec.Projectile cast() {
        return assertInstanceOf(CastSpec.Projectile.class, rightClick().ability().cast(),
                "the Scatter Shot is a projectile cast; a spread is a field on one");
    }

    // --- the weapon exists and is what it says --------------------------------------------------

    /**
     * IT LOADS, AND IT IS THE FOURTH RANGER WEAPON.
     *
     * <p>The identity row. Everything below is vacuous if this fails, and it is the row that fires
     * when the file is malformed rather than wrong.
     */
    @Test
    void theScatterShotLoadsAsARangerCrossbow() {
        WeaponDefinition weapon = scatterShot();

        assertEquals("Scatter Shot", weapon.displayName());
        assertEquals("kinetic", weapon.element());
        assertEquals("crossbow", weapon.material().toLowerCase(java.util.Locale.ROOT));
    }

    // --- the spread, both directions --------------------------------------------------------

    /**
     * *** SEVEN BODIES AT FIVE DEGREES -- THE RULED PAIR, READ BACK OFF THE FILE. ***
     *
     * <p>Both numbers, because <b>a round trip that asserts only presence passes on a loader that
     * hardcoded the value.</b> The two are staged at different values on purpose -- 7 against 5 --
     * so a transposition between {@code count} and {@code angle_degrees} cannot survive: a loader
     * that read them the wrong way round would report a count of 5 and an angle of 7.
     *
     * <p>Mutation {@code MUT14-COUNT} (7 -> 1) and {@code MUT14-ANGLE} both redden here.
     */
    @Test
    void theSpreadCarriesTheRuledCountAndAngle() {
        CastSpec.Spread spread = cast().spread();

        assertNotNull(spread, "scatter_shot's whole design is the spread block");
        assertEquals(7, spread.count(), "count 7, NOT the angle 5");
        assertEquals(5.0, spread.angleDegrees(), 1e-9, "angle 5, NOT the count 7");
        assertEquals(6, spread.ringCount(), "one down the aim vector and SIX on the ring");
    }

    /**
     * NO OTHER SHIPPED WEAPON AUTHORS A SPREAD, WHICH IS WHAT MAKES THE ROW ABOVE A MEASUREMENT.
     *
     * <p>The negative half of the round trip. If the loader attached a spread to every projectile,
     * the row above would pass and mean nothing. Also the row that would catch a default value
     * creeping into {@code AbilitySchema}'s projectile branch.
     */
    @Test
    void everyOtherProjectileInTheTreeHasNoSpread() {
        List<String> withSpread = shippedWeapons().all().stream()
                .flatMap(w -> w.triggers().stream().map(t -> w.id() + "/" + t.input()))
                .toList();
        assertFalse(withSpread.isEmpty(), "the walk found no triggers at all -- it is not walking");

        for (WeaponDefinition weapon : shippedWeapons().all()) {
            for (TriggerBinding binding : weapon.triggers()) {
                if (weapon.id().equals("scatter_shot")) continue;
                if (binding.ability().cast() instanceof CastSpec.Projectile projectile) {
                    assertNull(projectile.spread(),
                            weapon.id() + "/" + binding.input() + " grew a spread block");
                }
            }
        }
    }

    // --- the absence that is a ruling -----------------------------------------------------------

    /**
     * *** THE ARROWS DO NOT SEEK, AND THE ABSENT BLOCK IS THE RULING RATHER THAN AN OMISSION. ***
     *
     * <p><b>THIS ROW IS THE ONLY SUITE-SIDE WITNESS OF THAT RULING.</b> Nothing else in either
     * module can tell a Scatter Shot that homes from one that does not -- the flight's steer is a
     * no-op on a null {@code Seek}, so a homing block would change no count, no tooltip and no
     * damage figure. It would change where the arrows GO, and only a boot can see that.
     *
     * <p>So this row exists to make {@code MUT14-HOMING} killable at all. Named in the class
     * javadoc as well, because a row whose value is "it is the only guard of X" is the row someone
     * deletes while tidying.
     *
     * <p>The spread and the homing blocks are ORTHOGONAL by design -- a spread of seeking arrows is
     * expressible -- which is exactly why the absence has to be asserted rather than assumed to
     * follow from the spread being present.
     */
    @Test
    void theArrowsDoNotSeekAndTheAbsenceIsTheRuling() {
        assertNull(cast().homing(),
                "scatter_shot was ruled NOT homing; dragons_plume is the only content that may"
                        + " author a homing block");
    }

    /** {@code dragons_plume} still authors one -- the control that proves the row above can fail. */
    @Test
    void theOnlyWeaponThatSeeksStillDoes() {
        WeaponDefinition plume = shippedWeapons().find("dragons_plume")
                .orElseThrow(() -> new AssertionError("dragons_plume did not load"));
        CastSpec.Projectile draw = assertInstanceOf(CastSpec.Projectile.class,
                plume.trigger("draw").orElseThrow().ability().cast());

        assertNotNull(draw.homing(),
                "if THIS is null the assertion above is passing because nothing homes anywhere,"
                        + " which is a broken loader rather than a ruling being honoured");
    }

    // --- the numbers the rest of the design rests on --------------------------------------------

    /**
     * THE MAGAZINE IS FOUR AND THE COOLDOWN IS THIRTY-TWO.
     *
     * <p>Four is not an arbitrary magazine: {@code quiver_size: 1} would make every interval
     * reload-limited and discharge {@code /rpg firerate}'s caution on the confound it exists to
     * catch. The weapon file carries the account; this row is what notices if the value drifts.
     *
     * <p>32 is on the 4-tick input grid, so the delivered interval equals the authored one. A
     * value off the grid would be quantised up silently and the tooltip would not say so.
     */
    @Test
    void theMagazineAndCooldownAreTheRuledValues() {
        assertEquals(4, scatterShot().quiverSize(), "quiver_size 4");
        assertEquals(32, rightClick().ability().cooldownTicks(), "cooldown_ticks 32");
        assertEquals(0, 32 % 4, "32 must stay on the 4-tick input grid or the tooltip lies");
    }

    /**
     * *** IT IS A LITERAL, NOT A BASIC ATTACK -- AND THAT IS WHAT KEEPS 32 EXACT. ***
     *
     * <p>{@code AbilityService} divides a BASIC ATTACK's authored cooldown by the caster's attack
     * speed and uses a literal's verbatim. So this row is not a style check: <b>if the payload ever
     * became {@code weapon_damage}, the ruled 1.6-second rate would silently become a base value
     * true only at attack speed 1.0</b>, and {@code rangedAttackSpeedLabel} would print the
     * authored rate while the weapon fired at another.
     *
     * <p>It is also what makes {@code x 7} renderable at all: the shot count is emitted in the
     * ability block, and the basic-attack stat block has no slot for it.
     */
    @Test
    void theDamageIsAnAuthoredLiteralWhichIsWhatKeepsTheCooldownExact() {
        List<EffectSpec> onHit = rightClick().ability().onHit();

        EffectSpec.Damage damage = onHit.stream()
                .filter(EffectSpec.Damage.class::isInstance)
                .map(EffectSpec.Damage.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "scatter_shot must author a LITERAL damage effect, not weapon_damage"));

        assertEquals(9.0, damage.amount(), 1e-9, "9 per arrow, at gear score 100");
        assertEquals("kinetic", damage.element());
        assertTrue(onHit.stream().noneMatch(EffectSpec.WeaponDamage.class::isInstance),
                "a weapon_damage effect would make this a basic attack and un-fix the cooldown");
    }

    /**
     * IT AUTHORS KNOCKBACK, BECAUSE THE STANDING DECISION REACHES IT.
     *
     * <p>{@code CLAUDE.md}, 2026-09-13: a TRAVELLING ranged weapon authors knockback; an
     * instant-hitting one does not. This is {@code type: projectile} and {@code class: ranger}, so
     * silence would be an exception nobody ruled.
     *
     * <p><b>The STRENGTH is asserted loosely on purpose -- only that it is positive.</b> The
     * magnitude is a proposal pending a boot measurement of whether seven applications in one frame
     * SUM or OVERWRITE, and pinning 0.1 here would turn Ben's ruling into a test failure.
     */
    @Test
    void itAuthorsKnockbackBecauseTheStandingDecisionReachesIt() {
        EffectSpec.Knockback knockback = rightClick().ability().onHit().stream()
                .filter(EffectSpec.Knockback.class::isInstance)
                .map(EffectSpec.Knockback.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "a type: projectile ranger weapon authors knockback -- CLAUDE.md,"
                                + " 2026-09-13. Its absence would be an exception nobody ruled."));

        assertTrue(knockback.strength() > 0,
                "a knockback of zero satisfies the key and not the rule");
    }

    /**
     * NO {@code left_click} TRIGGER, AND THE LOADER WOULD HAVE REFUSED ONE.
     *
     * <p>A quiver binds left-click to the reload, so a weapon declaring both is refused outright.
     * Asserted rather than assumed because the refusal is fail-soft: a file that authored one would
     * be SKIPPED ENTIRELY, and the failure a reader would see is "scatter_shot does not exist".
     */
    @Test
    void theOnlyTriggerIsRightClickBecauseTheQuiverOwnsLeftClick() {
        assertEquals(List.of("right_click"),
                scatterShot().triggers().stream().map(TriggerBinding::input).toList());
    }
}
