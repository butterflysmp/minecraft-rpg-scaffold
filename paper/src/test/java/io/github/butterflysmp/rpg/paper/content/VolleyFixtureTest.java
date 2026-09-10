package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>THE VOLLEY FIXTURE ACTUALLY LOADS, AND ITS BOOT WARNINGS ARE THE ONES IT INTENDS.</b>
 *
 * <p>{@code volley_stone.yml} is the only thing in the tree that exercises {@code CastSpec.Volley}
 * end to end through the REAL loaders, so a typo in it would otherwise surface as a named, skipped
 * file at boot -- fail-soft by design, and therefore silent to every other test. The suite would
 * stay green with the fixture entirely absent, which is exactly the shape this project has been
 * bitten by: <b>a scan that finds nothing reads like a scan that found nothing wrong.</b>
 *
 * <p>This is a BUILD-time check rather than a boot-time one, for the reason
 * {@link ScorchContentInvariantTest} gives: {@code ContentValidator} warns and never throws, so it
 * names problems rather than failing. This cannot be built past.
 *
 * <h2>WHAT IT CANNOT SEE</h2>
 *
 * Every visual question. Whether eight beams one tick apart read as eight, whether the wind-up
 * chime is audible before the first shot, whether a missing volley gives any feedback at all --
 * none of that is reachable from here. That is {@code GATE-volley.md}'s job, and a green run of this
 * file is not evidence for any of it.
 *
 * <p>Each row names the mutation it forces red.
 */
class VolleyFixtureTest {

    /** Quiet: these loaders are fail-soft and log warnings, and the rows below assert on state. */
    private static Logger quiet() {
        Logger log = Logger.getLogger("VolleyFixtureTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.setLevel(Level.OFF);
        return log;
    }

    private static Path contentRoot() {
        try {
            var url = VolleyFixtureTest.class.getResource("/content");
            assertNotNull(url, "bundled content is missing from the test classpath entirely");
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static WeaponRegistry shippedWeapons() {
        return new WeaponLoader(quiet()).loadAll(new File(contentRoot().resolve("weapons").toString()));
    }

    private static VisualRegistry shippedVisuals() {
        return new VisualLoader(quiet()).loadAll(new File(contentRoot().resolve("visuals").toString()));
    }

    private static ElementRegistry shippedElements() {
        return new ElementLoader(quiet()).loadAll(new File(contentRoot().resolve("elements").toString()));
    }

    private static StatusRegistry shippedStatuses() {
        return new StatusLoader(quiet()).loadAll(new File(contentRoot().resolve("statuses").toString()));
    }

    private static TriggerBinding trigger(WeaponDefinition weapon, String input) {
        return weapon.trigger(input).orElseThrow(
                () -> new AssertionError("volley_stone has no '" + input + "' trigger"));
    }

    private static WeaponDefinition volleyStone() {
        var weapons = shippedWeapons();
        // THE POSITIVE CONTROL, and it comes first. Everything below is a lookup in a registry
        // built by walking a directory; a walk that found nothing returns an empty registry, and
        // "volley_stone is absent" would then be reported identically whether the fixture is
        // broken or the walk is blind.
        assertTrue(weapons.all().size() >= 7,
                "the weapon walk found only " + weapons.all().size() + " files -- it is blind, and a "
                        + "blind walk reports a missing fixture exactly like a broken one");
        return weapons.find("volley_stone").orElseThrow(
                () -> new AssertionError("volley_stone.yml did not load. The loaders are fail-soft, "
                        + "so a typo in it is a named, skipped file at boot and silent everywhere "
                        + "else -- this row is the only thing that can see it"));
    }

    @Test
    void theFixtureLoadsAndItsTwoTriggersAreVOLLEYS() {
        var stone = volleyStone();

        var scatter = assertInstanceOf(CastSpec.Volley.class, trigger(stone, "right_click").ability().cast());
        assertEquals(10, scatter.windupTicks());
        assertEquals(3, scatter.shots());
        assertEquals(5, scatter.intervalTicks());
        assertInstanceOf(CastSpec.Projectile.class, scatter.of(),
                "Scatter exists to EXERCISE the whitelist's second member rather than assert it");

        var rake = assertInstanceOf(CastSpec.Volley.class, trigger(stone, "left_click").ability().cast());
        assertEquals(20, rake.windupTicks());
        assertEquals(8, rake.shots());
        assertEquals(1, rake.intervalTicks(),
                "interval 1 is the SCHEDULER'S FLOOR, and Rake sits on it deliberately: the gate has "
                        + "to measure the grammar's limit, not a weapon's comfortable tuning");
        var ray = assertInstanceOf(CastSpec.Ray.class, rake.of());
        assertEquals(64, ray.range(), 1e-9);
        assertEquals("volley_beam", ray.beam());
    }

    @Test
    void theTwoTriggersShareNONumberWithEachOther() {
        var stone = volleyStone();
        var a = (CastSpec.Volley) trigger(stone, "right_click").ability().cast();
        var b = (CastSpec.Volley) trigger(stone, "left_click").ability().cast();

        assertEquals(3, List.of(
                a.windupTicks() != b.windupTicks(),
                a.shots() != b.shots(),
                a.intervalTicks() != b.intervalTicks()).stream().filter(x -> x).count(),
                "all three fields must differ between the two triggers");
        assertFalse(a.of().getClass().equals(b.of().getClass()),
                "and the inner cast kinds must differ too");

        // THIS IS THE FIXTURE'S WHOLE REASON FOR EXISTING, SO IT IS PINNED RATHER THAN TRUSTED TO A
        // COMMENT. If the wrapper only ever ran at one configuration, nothing would have shown it
        // generalises, and the first weapon wanting a different one would find out at boot. A future
        // edit that "tidies" the two triggers into matching numbers deletes the property without
        // deleting a line of prose about it.
    }

    @Test
    void theFixtureNamesNoNUMBERTheCursedEmeraldClaims() {
        var rake = (CastSpec.Volley) trigger(volleyStone(), "left_click").ability().cast();

        assertFalse(rake.windupTicks() == 20 && rake.shots() == 6 && rake.intervalTicks() == 2,
                "20/6/2 is the Cursed Emerald's triple. A fixture staged at a weapon's settings "
                        + "quietly absorbs that weapon's decisions as though they were the "
                        + "mechanism's, which is what this whole slice is arranged to prevent");
        // DELIBERATELY A CONJUNCTION, not three separate assertions: Rake DOES author windup 20, and
        // that is fine -- a wind-up is not a decision anyone owns. What must never happen is the
        // whole triple coinciding. Asserting the fields individually would forbid a number that is
        // not the problem and would have to be relaxed the first time it collided by chance.
        //
        // WHERE 20/6/2 COMES FROM, AND WHAT CHANGED UNDER IT: it is
        // content/weapons/cursed_emerald.yml's triple. It was a number from a PLAN when this row was
        // written and became a number from a SHIPPED FILE at 603936c. The row itself did not change
        // meaning -- it has always asserted "Rake is not this particular triple", true before and
        // after -- but IF THE EMERALD IS EVER RETUNED, THIS ROW KEEPS GUARDING A TRIPLE NOBODY USES
        // and nothing here would say so. Retune the emerald, retune this literal.
        //
        // DO NOT "FIX" THIS BY READING THE EMERALD'S FILE. That would make the row assert that two
        // files DIFFER -- true by coincidence rather than by decision -- and it would redden the day
        // the emerald ships a triple that collides by chance, which is not the defect this row is
        // for. The defect is a FIXTURE STAGED AT A WEAPON'S SETTINGS, and that is a claim about
        // this file's intent, not about the other file's contents.
    }

    @Test
    void everyVISUALTheFixtureNamesActuallyExists() {
        var visuals = shippedVisuals();
        assertTrue(visuals.all().size() >= 14,
                "the visual walk found only " + visuals.all().size() + " files -- blind again");

        assertTrue(visuals.find("volley_beam").isPresent(), "volley_beam.yml did not load");
        assertTrue(visuals.find("volley_cast").isPresent(), "volley_cast.yml did not load");
        // ContentValidator names a dangling beam at boot, but an UNREFERENCED visual is SILENT: the
        // validator walks references outward and has no orphan check. So a fixture whose visuals
        // failed to parse would warn for the beam and say nothing at all about the cast sound.
    }

    /**
     * <b>THE EXPECTED WARNING IS ASSERTED, BECAUSE AN EXPECTED WARNING NOBODY CHECKS IS JUST NOISE
     * THAT TRAINS PEOPLE TO IGNORE THE CHANNEL.</b>
     *
     * <p>Rake authors {@code cooldown_ticks: 0} on purpose -- it is the only value that makes gate
     * row V5 mean anything, since authoring the floor itself would stage two independent quantities
     * as equal and the row would pass whether the floor existed or not. The cost is one permanent
     * {@code Content:} warning at every boot. This row pins that there is EXACTLY ONE, that it is
     * Rake's, and that it names both numbers -- so the day a second appears, somebody knows.
     */
    @Test
    void theFixtureProducesEXACTLYONEBootWarningAndItIsRakesFloor() {
        var problems = new ContentValidator(shippedVisuals(), shippedStatuses(), shippedElements(),
                key -> true, key -> true)
                .validateWeapons(List.of(volleyStone()));

        assertEquals(1, problems.size(),
                "one expected warning, no more and no fewer: " + problems);
        assertTrue(problems.get(0).contains("left_click"),
                "it is RAKE's, not Scatter's -- Scatter authors 40 against a floor of 20, and a "
                        + "warning there would mean the floor is overwriting deliberate values: "
                        + problems);
        assertTrue(problems.get(0).contains("27"),
                "and it names the derived floor the author will actually get: " + problems);
        // THE REGISTRIES ARE THE SHIPPED ONES, NOT EMPTY STUBS, AND THE FIRST DRAFT GOT THAT
        // BACKWARDS. It passed an empty ElementRegistry with a comment claiming that "isolated the
        // cooldown arm" -- it did the opposite: three extra warnings appeared, all of them saying
        // kinetic is undefined, which is true of the stub and false of the server. A fixture that
        // MANUFACTURES problems cannot assert an exact problem count. Loading the real elements and
        // statuses is what makes "exactly one" a statement about the boot rather than about the
        // test.
        // Mutation: author 27 on Rake -> the count drops to 0 and this reddens, which is the point:
        // the fixture's whole gate value depends on that field staying 0.
    }
}
