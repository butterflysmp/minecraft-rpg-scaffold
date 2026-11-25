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
 * <p><b>2026-11-24 (PR B): the Volley Stone was DELETED; its file is now the TEST-ONLY fixture
 * {@code fixtures/weapons/volley_fixture.yml}, which ships nowhere.</b> Everything below about "the stone" and
 * "boot" is the record of the shipped stone; the rows now read the fixture.
 *
 * <p>{@code volley_stone.yml} was the only thing in the tree that exercises {@code CastSpec.Volley}
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
                () -> new AssertionError("volley_fixture has no '" + input + "' trigger"));
    }

    /** The TEST-ONLY fixture directory (2026-11-24, PR B): never on the main classpath, never in the jar. */
    private static Path fixtureRoot() {
        try {
            var url = VolleyFixtureTest.class.getResource("/fixtures");
            assertNotNull(url, "the test fixtures are missing from the test classpath entirely");
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static VisualRegistry fixtureVisuals() {
        return new VisualLoader(quiet()).loadAll(new File(fixtureRoot().resolve("visuals").toString()));
    }

    /**
     * <b>THE STONE WAS DELETED 2026-11-24 (PR B; Ben: "Delete the volley stone in this pass").</b> Its file moved, word
     * for word below a header, to {@code fixtures/weapons/volley_fixture.yml}, with its id, display name and two visual
     * ids renamed, so every row below reads the SAME triggers it read before. The mechanism is unchanged; only where
     * the file lives is.
     */
    private static WeaponDefinition volleyStone() {
        var weapons = new WeaponLoader(quiet()).loadAll(new File(fixtureRoot().resolve("weapons").toString()));
        // THE POSITIVE CONTROL, and it comes first: a blind walk reports a missing fixture exactly like a broken one.
        assertEquals(1, weapons.all().size(),
                "the fixture walk found " + weapons.all().size() + " weapon files, not the one fixture");
        assertTrue(shippedWeapons().all().size() >= 7, "and the shipped walk is not blind either");
        assertTrue(shippedWeapons().find("volley_fixture").isEmpty() && shippedWeapons().find("volley_stone").isEmpty(),
                "NEITHER the fixture nor the deleted stone ships");
        return weapons.find("volley_fixture").orElseThrow(
                () -> new AssertionError("volley_fixture.yml did not load. The loaders are fail-soft, "
                        + "so a typo in it is a named, skipped file and silent everywhere "
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
        assertEquals("volley_fixture_beam", ray.beam());
    }

    /**
     * <b>NO SHIPPED WEAPON FIRES A VOLLEY OF PROJECTILES NOW (2026-11-24, PR B).</b> The stone's Scatter was the only one;
     * the engine path stays, exercised by the fixture above. The control: the same sweep over the fixture finds one.
     */
    @Test
    void noShippedWeaponFiresAVolleyOfProjectilesAndTheFixtureDoes() {
        List<String> shipped = new java.util.ArrayList<>();
        int triggers = 0;
        for (WeaponDefinition w : shippedWeapons().all()) {
            for (String input : List.of("right_click", "left_click")) {
                var t = w.trigger(input);
                if (t.isEmpty()) continue;
                triggers++;
                if (t.get().ability().cast() instanceof CastSpec.Volley v && v.of() instanceof CastSpec.Projectile) {
                    shipped.add(w.id() + " " + input);
                }
            }
        }
        assertTrue(triggers >= 10, "the sweep saw only " + triggers + " triggers -- blind");
        assertEquals(List.of(), shipped, "a shipped weapon fires a volley of projectiles");
        assertInstanceOf(CastSpec.Projectile.class,
                ((CastSpec.Volley) trigger(volleyStone(), "right_click").ability().cast()).of(),
                "the control: the fixture's Scatter does, so the sweep's arm can match");
    }

    /**
     * <b>THE SHIPPED WEAPONS PRODUCE NO CONTENT WARNING AT ALL NOW (2026-11-24, PR B).</b> Until the stone went, every boot
     * printed its Rake warning ("the known pair"); R0c now predicts no {@code [Rpg]} WARN at all, and this is the build
     * half of that prediction, for the weapon arm. The control: the same validator over the shipped weapons PLUS the
     * fixture reports exactly one, Rake's.
     */
    @Test
    void theShippedWeaponsValidateWithNoWarningAndTheFixtureAddsExactlyOne() {
        var visuals = shippedVisuals();
        var shipped = List.copyOf(shippedWeapons().all());
        assertTrue(shipped.size() >= 7, "blind walk");
        var none = new ContentValidator(visuals, shippedStatuses(), shippedElements(), key -> true, key -> true)
                .validateWeapons(shipped);
        assertEquals(List.of(), none, "a shipped weapon warns at boot");
        var withFixture = new java.util.ArrayList<>(shipped);
        withFixture.add(volleyStone());
        fixtureVisuals().all().forEach(visuals::register);   // the fixture's own two, so it warns for its floor only
        var one = new ContentValidator(visuals, shippedStatuses(), shippedElements(),
                key -> true, key -> true).validateWeapons(withFixture);
        assertEquals(1, one.size(), "the control: the fixture's Rake floor, exactly: " + one);
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
        var visuals = fixtureVisuals();
        assertEquals(2, visuals.all().size(),
                "the fixture visual walk found " + visuals.all().size() + " files, not the fixture's two");

        assertTrue(visuals.find("volley_fixture_beam").isPresent(), "volley_fixture_beam.yml did not load");
        assertTrue(visuals.find("volley_fixture_cast").isPresent(), "volley_fixture_cast.yml did not load");
        // 2026-11-24 (PR B): both moved out of content/visuals with the stone; neither ships under either name.
        for (String id : List.of("volley_beam", "volley_cast", "volley_fixture_beam", "volley_fixture_cast")) {
            assertTrue(shippedVisuals().find(id).isEmpty(), id + " ships");
        }
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
        var problems = new ContentValidator(fixtureVisuals(), shippedStatuses(), shippedElements(),
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
