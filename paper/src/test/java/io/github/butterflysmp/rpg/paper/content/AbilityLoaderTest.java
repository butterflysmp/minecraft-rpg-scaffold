package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A typo in the 400th weapon must not take the server down. These tests pin the
 * fail-soft contract: log, name the file, skip it, keep loading.
 */
class AbilityLoaderTest {

    @TempDir
    Path dir;

    private Logger log;
    private List<LogRecord> warnings;

    @BeforeEach
    void setUp() {
        warnings = new ArrayList<>();
        log = Logger.getLogger("AbilityLoaderTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) warnings.add(record);
            }
            @Override public void flush() {}
            @Override public void close() {}
        });
    }

    private void write(String name, String yaml) throws IOException {
        Files.writeString(dir.resolve(name), yaml, StandardCharsets.UTF_8);
    }

    private AbilityRegistry load() {
        return new AbilityLoader(log).loadAll(new File(dir.toString()));
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    private static final String VALID = """
            id: solar_grenade
            element: fire
            cooldown_ticks: 200
            cast:
              type: projectile
            on_hit:
              - type: damage
                amount: 12
                element: fire
            """;

    @Test
    void loadsAValidAbility() throws IOException {
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size());
        AbilityDefinition def = registry.find("solar_grenade").orElseThrow();
        assertEquals("fire", def.element());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * A projectile's optional trail: authored -> carried, absent -> null.
     *
     * The absent half is what makes this NOT a change to hunters_bow and ember_staff. They name no
     * trail, so they get exactly the null the call site used to hardcode.
     */
    @Test
    void aProjectileTrailIsOptionalAndAbsentMeansNull() throws IOException {
        write("with_trail.yml", """
                id: with_trail
                element: fire
                cast:
                  type: projectile
                  speed: 1.4
                  trail: flint_trail
                on_hit:
                  - type: damage
                    amount: 20
                    element: fire
                """);
        write("without_trail.yml", VALID);

        var registry = load();
        var withTrail = (CastSpec.Projectile) registry.find("with_trail").orElseThrow().cast();
        var bare = (CastSpec.Projectile) registry.find("solar_grenade").orElseThrow().cast();

        assertEquals("flint_trail", withTrail.trail());
        assertNull(bare.trail(), "a projectile that names no trail leaves nothing, as it always did");
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * A projectile's optional BODY: authored -> carried, absent -> null, and independent of the
     * trail. A trail with no body is what the Flint Staff shipped as one slice earlier, so a schema
     * that defaulted one from the other would have made that state unexpressible.
     */
    @Test
    void aProjectileItemIsOptionalAndIndependentOfTheTrail() throws IOException {
        write("bodied.yml", """
                id: bodied
                element: fire
                cast:
                  type: projectile
                  speed: 1.4
                  trail: flint_trail
                  item: flint
                on_hit:
                  - type: damage
                    amount: 20
                    element: fire
                """);
        write("trail_only.yml", """
                id: trail_only
                element: fire
                cast:
                  type: projectile
                  trail: flint_trail
                on_hit:
                  - type: damage
                    amount: 20
                    element: fire
                """);
        write("without_trail.yml", VALID);

        var registry = load();
        var bodied = (CastSpec.Projectile) registry.find("bodied").orElseThrow().cast();
        var trailOnly = (CastSpec.Projectile) registry.find("trail_only").orElseThrow().cast();
        var bare = (CastSpec.Projectile) registry.find("solar_grenade").orElseThrow().cast();

        assertEquals("flint", bodied.item());
        assertEquals("flint_trail", bodied.trail());
        assertNull(trailOnly.item(), "a trail must not imply a body");
        assertEquals("flint_trail", trailOnly.trail());
        assertNull(bare.item(), "and a projectile that names neither gets neither");
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** on_cast carries its visuals, and is empty (never null) when the file names none. */
    @Test
    void onCastIsParsedAndAbsentMeansEmpty() throws IOException {
        write("noisy.yml", """
                id: noisy
                element: fire
                cast:
                  type: projectile
                on_cast:
                  - type: visual
                    visual_id: flint_cast
                on_hit:
                  - type: damage
                    amount: 20
                    element: fire
                """);
        write("quiet.yml", VALID);

        var registry = load();
        assertEquals(List.of(new EffectSpec.Visual("flint_cast")),
                registry.find("noisy").orElseThrow().onCast());
        assertEquals(List.of(), registry.find("solar_grenade").orElseThrow().onCast());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * on_cast admits VISUALS ONLY, and a file that asks for anything else is named and skipped.
     *
     * The bound is deliberately narrower than EffectSpec.Untargeted, which would also let a
     * `burst` deal mob damage at the caster's own eye on every cast and let a `throw_embers` fan
     * around a zero direction vector. Neither was designed; a schema should not carry behaviour
     * nobody chose. See AbilitySchema.parseCastVisuals.
     */
    @Test
    void aNonVisualEffectInOnCastIsNamedAndSkipped() throws IOException {
        write("aaa_bad_cast.yml", """
                id: bad_cast
                element: fire
                cast:
                  type: projectile
                on_cast:
                  - type: damage
                    amount: 20
                    element: fire
                on_hit:
                  - type: damage
                    amount: 20
                    element: fire
                """);
        write("solar_grenade.yml", VALID);

        var registry = load();

        assertTrue(registry.find("bad_cast").isEmpty(), "the malformed file must be skipped");
        assertEquals(1, registry.size(), "and every other ability still loads");
        assertTrue(warningText().contains("on_cast"),
                "the warning must name the offending section: " + warningText());
    }

    /** The dash cast: a new shape, parsed like every other, carrying its four-effect payload. */
    @Test
    void loadsADashCastWithItsPayload() throws IOException {
        write("ember_step.yml", """
                id: ember_step
                element: fire
                cooldown_ticks: 160
                cast:
                  type: dash
                  distance: 12
                  speed: 1.6
                  lift: 0.4
                on_hit:
                  - type: damage
                    amount: 8
                    element: fire
                  - type: knockback
                    strength: 1.0
                  - type: status
                    status_id: scorch
                    duration_ticks: 60
                  - type: visual
                    visual_id: solar_detonation
                """);

        AbilityDefinition def = load().find("ember_step").orElseThrow();

        var dash = assertInstanceOf(CastSpec.Dash.class, def.cast());
        assertEquals(12, dash.distance(), 1e-9);
        assertEquals(1.6, dash.speed(), 1e-9);
        assertEquals(0.4, dash.lift(), 1e-9);
        assertEquals(4, def.onHit().size(), "damage + knockback + status + visual");
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** amplifier is optional -- most statuses have a single tier. */
    @Test
    void statusWithoutAmplifierDefaultsToZero() throws IOException {
        write("scorcher.yml", """
                id: scorcher
                element: solar
                on_hit:
                  - type: status
                    status_id: scorch
                    duration_ticks: 40
                """);

        AbilityRegistry registry = load();

        var status = (EffectSpec.Status) registry.find("scorcher").orElseThrow().onHit().get(0);
        assertEquals(0, status.amplifier());
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void areaWithoutEffectsIsSkippedNotCrashed() throws IOException {
        write("aaa_broken.yml", """
                id: broken
                element: solar
                on_hit:
                  - type: area
                    radius: 4.0
                    duration_ticks: 100
                    tick_interval: 20
                """);
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size(), "the valid ability must still load");
        assertTrue(registry.find("solar_grenade").isPresent());
        assertTrue(warningText().contains("aaa_broken.yml"), warningText());
        assertTrue(warningText().contains("effects"), warningText());
    }

    /**
     * A tick_interval of 0 would make the area reschedule itself forever at zero delay.
     * EffectSpec.Area rejects it; the loader must report it like any content mistake.
     */
    @Test
    void areaWithZeroTickIntervalIsSkippedNotCrashed() throws IOException {
        write("aaa_storm.yml", """
                id: storm
                element: solar
                on_hit:
                  - type: area
                    radius: 4.0
                    duration_ticks: 100
                    tick_interval: 0
                    effects:
                      - type: damage
                        amount: 2
                        element: solar
                """);
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = assertDoesNotThrow(this::load);

        assertEquals(1, registry.size(), "the valid ability must still load");
        assertTrue(warningText().contains("aaa_storm.yml"), warningText());
        assertTrue(warningText().contains("tick_interval"), warningText());
    }

    /**
     * A misspelled element no longer skips the file. Element is a content id now, validated
     * by ContentValidator at boot -- not by the loader, which carries whatever string it is
     * given. A bad value warns later; it does not lose the ability.
     */
    @Test
    void anUnknownElementValueStillLoads() throws IOException {
        write("typo.yml", """
                id: typo
                element: fyre
                on_hit: []
                """);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size());
        assertEquals("fyre", registry.find("typo").orElseThrow().element(), "carried as-is, not resolved");
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void missingRequiredFieldIsSkippedNotCrashed() throws IOException {
        write("aaa_noid.yml", "element: solar\n");
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_noid.yml"), warningText());
        assertTrue(warningText().contains("id"), warningText());
    }

    @Test
    void loadsABurst() throws IOException {
        write("blast.yml", """
                id: blast
                element: solar
                on_hit:
                  - type: burst
                    radius: 4.0
                    effects:
                      - type: damage
                        amount: 6
                        element: solar
                      - type: status
                        status_id: scorch
                        duration_ticks: 40
                """);

        var burst = (EffectSpec.Burst) load().find("blast").orElseThrow().onHit().get(0);

        assertEquals(4.0, burst.radius(), 1e-9);
        assertEquals(2, burst.effects().size());
        assertInstanceOf(EffectSpec.Damage.class, burst.effects().get(0));
        assertInstanceOf(EffectSpec.Status.class, burst.effects().get(1));
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** A burst nesting an untargeted effect cannot be represented in core, same as an area. */
    @Test
    void burstNestingAnUntargetedEffectIsSkippedNotCrashed() throws IOException {
        write("aaa_nested_burst.yml", """
                id: nested
                element: solar
                on_hit:
                  - type: burst
                    radius: 4.0
                    effects:
                      - type: visual
                        visual_id: sparkles
                """);
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_nested_burst.yml"), warningText());
        assertTrue(warningText().contains("cannot be nested"), warningText());
        assertTrue(warningText().contains("burst"), warningText());
    }

    @Test
    void burstWithZeroRadiusIsSkippedNotCrashed() throws IOException {
        write("aaa_flat.yml", """
                id: flat
                element: solar
                on_hit:
                  - type: burst
                    radius: 0
                    effects:
                      - type: damage
                        amount: 6
                        element: solar
                """);
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = assertDoesNotThrow(this::load);

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_flat.yml"), warningText());
        assertTrue(warningText().contains("radius"), warningText());
    }

    /** An area nesting an untargeted effect cannot be represented in core. */
    @Test
    void areaNestingAnUntargetedEffectIsSkippedNotCrashed() throws IOException {
        write("aaa_nested.yml", """
                id: nested
                element: solar
                on_hit:
                  - type: area
                    radius: 4.0
                    duration_ticks: 100
                    tick_interval: 20
                    effects:
                      - type: visual
                        visual_id: sparkles
                """);
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_nested.yml"), warningText());
        assertTrue(warningText().contains("cannot be nested"), warningText());
    }

    @Test
    void unknownEffectTypeIsSkippedNotCrashed() throws IOException {
        write("aaa_bogus.yml", """
                id: bogus
                element: solar
                on_hit:
                  - type: teleport
                    distance: 5
                """);
        write("solar_grenade.yml", VALID);

        AbilityRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("teleport"), warningText());
    }

    /** Every file broken: the server still boots, with zero abilities. */
    @Test
    void allFilesBrokenStillReturnsAnEmptyRegistry() throws IOException {
        write("a.yml", "element: fire\n");                              // no id -> skipped
        write("b.yml", "id: b\nelement: fire\non_hit:\n  - type: teleport\n"); // unknown effect -> skipped

        AbilityRegistry registry = assertDoesNotThrow(this::load);

        assertEquals(0, registry.size());
        assertEquals(3, warnings.size(), "two file warnings plus the summary");
    }

    @Test
    void missingDirectoryYieldsEmptyRegistry() {
        var registry = new AbilityLoader(log).loadAll(new File(dir.toFile(), "does_not_exist"));
        assertEquals(0, registry.size());
    }

    /**
     * The content we actually ship, parsed by the loader we actually run. Both its burst
     * and its area nest a status effect, which is the shape that used to NPE at runtime
     * and the shape the Targeted-only nesting rule must still allow.
     */
    @Test
    void bundledSolarGrenadeContentLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/abilities/solar_grenade.yml")) {
            assertNotNull(in, "bundled content is missing from the classpath");
            Files.write(dir.resolve("solar_grenade.yml"), in.readAllBytes());
        }

        AbilityRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());

        AbilityDefinition def = registry.find("solar_grenade").orElseThrow();
        assertEquals("fire", def.element());
        // Bounded, not pinned: 200 is balance. The bound is still real -- an ability with no
        // cooldown is spammable, and no shipped ability declares one.
        //
        // Note this is deliberately NOT symmetric with the bow's fire rate in WeaponLoaderTest,
        // which asserts nothing at all: the old ability_stone dev weapon (since deleted) shipped `cooldown_ticks: 0` (a dev
        // instrument you spam while tuning), so "> 0" is a real invariant for an ABILITY and not one
        // for a WEAPON TRIGGER. If a cost-gated 0-cooldown ability is ever wanted, delete this line
        // then -- as a deliberate call, not as a tidy-up of an apparent inconsistency.
        assertTrue(def.cooldownTicks() > 0, "a shipped ability declares a cooldown");
        assertEquals("mana", def.cost().resourceId());

        // The blast: splash damage plus the ignition, on the detonation frame.
        var burst = def.onHit().stream()
                .filter(EffectSpec.Burst.class::isInstance)
                .map(EffectSpec.Burst.class::cast)
                .findFirst().orElseThrow(() -> new AssertionError("no burst: mobs would ignite late"));
        // TWO effects now: splash damage, and rooted_TEMP -- a boot-test fixture on the grenade
        // burst. THE EXPLICIT SCORCH IS GONE and is not missing: the splash damage wears
        // element: fire, and fire declares applies_status in content/elements/fire.yml, so the
        // burst accrues scorch from the damage rather than naming it twice. When rooted_TEMP is
        // removed this count returns to 1.
        assertEquals(2, burst.effects().size());
        var splash = assertInstanceOf(EffectSpec.Damage.class, burst.effects().get(0));
        assertEquals("fire", splash.element(),
                "and THIS is what applies the burn now -- the element, not a status effect");
        var rootedTemp = assertInstanceOf(EffectSpec.Status.class, burst.effects().get(1));
        assertEquals("rooted", rootedTemp.statusId()); // rooted_TEMP: remove with the fixture

        // The field: a fire damage pulse, which accrues the burn that keeps you alight while you stand in it.
        var area = def.onHit().stream()
                .filter(EffectSpec.Area.class::isInstance)
                .map(EffectSpec.Area.class::cast)
                .findFirst().orElseThrow();
        assertEquals(1, area.effects().size(),
                "the pulse is DAMAGE ALONE now -- its explicit scorch is gone, and the fire element"
                        + " accrues the burn that keeps you alight while you stand in it");
        var pulse = assertInstanceOf(EffectSpec.Damage.class, area.effects().get(0));
        assertEquals("fire", pulse.element());
    }

    /**
     * The shipped Rekindle, parsed by the loader we actually run. No unit test loaded this
     * file before, so a mistyped key in the throw_embers grammar would have surfaced only at
     * a server boot (where the loader's fail-soft would quietly skip it). This pins the new
     * thrown-item shape -- item + fuse_ticks + burst -- on the real content.
     */
    @Test
    void bundledRekindleContentLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/abilities/rekindle.yml")) {
            assertNotNull(in, "bundled content is missing from the classpath");
            Files.write(dir.resolve("rekindle.yml"), in.readAllBytes());
        }

        AbilityRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());

        AbilityDefinition def = registry.find("rekindle").orElseThrow();
        assertInstanceOf(CastSpec.Dash.class, def.cast());

        var embers = def.onHit().stream()
                .filter(EffectSpec.ThrowEmbers.class::isInstance)
                .map(EffectSpec.ThrowEmbers.class::cast)
                .findFirst().orElseThrow(() -> new AssertionError("no throw_embers: the ember fan is gone"));
        // A three-prong fan. The SPREAD is tuned by feel (the YAML says so), so only the prong
        // count is pinned -- dropping to one ember is a design change, widening to 50 degrees is not.
        assertEquals(3, embers.anglesDegrees().size(), "a three-prong ember fan");
        assertEquals("blaze_powder", embers.itemId());
        assertTrue(embers.fuseTicks() >= 1, "fuse must be a positive number of ticks");
        assertEquals("ember_burst", embers.visual());
        // The detonation carries the mob-only burn: fire damage, which accrues scorch by itself.
        assertTrue(embers.burst().radius() > 0, "the detonation bursts with a real radius");
        assertInstanceOf(EffectSpec.Damage.class, embers.burst().effects().get(0));
        assertEquals(1, embers.burst().effects().size(),
                "damage alone -- the explicit scorch is gone, the element accrues it");
    }

    // --- The volley cast, and its whitelist ---

    /** Body of a volley ability; {@code of} is spliced in so each row varies one thing. */
    private static String volleyYaml(String of) {
        return """
                id: burst
                element: kinetic
                cooldown_ticks: 0
                cast:
                  type: volley
                  windup_ticks: 12
                  shots: 5
                  interval_ticks: 3
                  of:
                %s
                on_hit:
                  - type: damage
                    amount: 10
                    element: kinetic
                """.formatted(of);
    }

    @Test
    void loadsAVolleyAndItsInnerRay() throws IOException {
        write("burst.yml", volleyYaml("""
                    type: ray
                    range: 64
                    beam: some_beam\
                """));

        var volley = assertInstanceOf(CastSpec.Volley.class,
                load().find("burst").orElseThrow().cast());

        assertEquals(12, volley.windupTicks());
        assertEquals(5, volley.shots());
        assertEquals(3, volley.intervalTicks());
        var ray = assertInstanceOf(CastSpec.Ray.class, volley.of());
        assertEquals(64, ray.range(), 1e-9);
        assertEquals("some_beam", ray.beam(),
                "the inner cast is parsed by the SAME parseCast every standalone cast uses, so a "
                        + "ray inside a volley keeps every field a ray has");
        assertTrue(warnings.isEmpty(), warningText());
        // ALL FOUR NUMBERS ARE DISTINCT (12, 5, 3, 64) so a parser reading two fields off one key
        // cannot pass. Mutation: swap getInt("shots") and getInt("interval_ticks") -> reddens.
    }

    @Test
    void loadsAVolleyOfPROJECTILES() throws IOException {
        write("burst.yml", volleyYaml("""
                    type: projectile
                    speed: 1.4\
                """));

        var volley = assertInstanceOf(CastSpec.Volley.class,
                load().find("burst").orElseThrow().cast());
        assertInstanceOf(CastSpec.Projectile.class, volley.of());
        assertTrue(warnings.isEmpty(), warningText());
        // The whitelist's second member. Exercised, not asserted -- see CastExecutorVolleyTest.
    }

    @Test
    void aVolleyWithNoOfSectionIsNamedAndSkipped() throws IOException {
        write("aaa_burst.yml", """
                id: burst
                element: kinetic
                cast:
                  type: volley
                  shots: 5
                on_hit:
                  - type: damage
                    amount: 10
                    element: kinetic
                """);
        write("solar_grenade.yml", VALID);

        var registry = load();

        assertTrue(registry.find("burst").isEmpty(), "the malformed file must be skipped");
        assertEquals(1, registry.size(), "and every other ability still loads");
        assertTrue(warningText().contains("of:"),
                "the warning must name the missing section: " + warningText());
        // AN ABSENT `of:` THROWS rather than defaulting the way an absent `cast:` defaults to Self.
        // A cast section missing entirely has a sensible reading; a volley of nothing does not.
        // Mutation: return a Self for a null section -> the file loads and this reddens.
    }

    @Test
    void aVolleyOfADASHIsRefusedByNameAndTheReasonIsRECORDED() throws IOException {
        write("aaa_burst.yml", volleyYaml("""
                    type: dash
                    distance: 12\
                """));
        write("solar_grenade.yml", VALID);

        var registry = load();

        assertTrue(registry.find("burst").isEmpty(), "the malformed file must be skipped");
        assertEquals(1, registry.size(), "and every other ability still loads");
        assertTrue(warningText().contains("dash"),
                "the warning must name the cast type it refused: " + warningText());
        assertTrue(warningText().contains("wrong way"),
                "and WHY, because this refusal is load-bearing rather than tidy: DashAim resolves a "
                        + "dash's direction before dispatch and matches the OUTER cast only, so a "
                        + "repeated dash would still fire and simply go somewhere else: "
                        + warningText());
    }

    @Test
    void aVolleyOfAVOLLEYIsRefused() throws IOException {
        write("aaa_burst.yml", volleyYaml("""
                    type: volley
                    shots: 3
                    of:
                      type: ray\
                """));
        write("solar_grenade.yml", VALID);

        var registry = load();

        assertTrue(registry.find("burst").isEmpty(), "the malformed file must be skipped");
        assertEquals(1, registry.size());
        assertTrue(warningText().contains("volley"), warningText());
        // Refused TWICE over, deliberately: the loader's whitelist names it, and CastSpec.Volley's
        // compact constructor makes it unconstructible even from Java. The second is what stops a
        // future call site building the fork bomb the loader would have caught.
    }

    @Test
    void theTWOSELFSHAPEDRefusalsAreRefusedTOO() throws IOException {
        write("aaa_a.yml", volleyYaml("    type: self"));
        write("aaa_b.yml", volleyYaml("""
                    type: melee
                    reach: 3\
                """));
        write("solar_grenade.yml", VALID);

        assertEquals(1, load().size(),
                "both files are skipped; only the valid ability loads");
        assertTrue(warningText().contains("self") && warningText().contains("melee"),
                "each refusal names its own type rather than a generic message: " + warningText());
        // THE WHITELIST IS EXHAUSTIVE OVER THE SEALED TYPE, so these rows exist to pin that every
        // non-admitted kind is actually refused rather than falling through. Mutation: replace the
        // switch with `if (inner instanceof Volley) throw` -> self and melee load and this reddens.
    }

    private static String homingYaml(String homingBlock) {
        return """
                id: seeker
                element: void
                cast:
                  type: projectile
                  speed: 2.5
                  gravity: 0.05
                  max_lifetime_ticks: 120
                """ + homingBlock + """
                on_hit:
                  - type: damage
                    amount: 48
                    element: void
                """;
    }

    /**
     * THE HOMING BLOCK REACHES {@code CastSpec.Homing} FROM A FILE, WHICH IT COULD NOT BEFORE.
     *
     * <p>Slice F built the record, {@code CastExecutor}'s mapping and the flight's steer branch, and
     * <b>nothing mapped a yml onto any of it</b> -- the three numbers were reachable only from Java.
     * This row is the one that makes the Dragon's Plume's arrows describable in content.
     *
     * <p>The values are {@code PLAN-dragons-plume.md} §5's, carried INHERITED AND UNJUDGED with gate
     * rows P1-P4. <b>This row asserts they arrive intact, not that they are right</b> -- only a boot
     * can say the second thing, which is what those rows are for.
     */
    @Test
    void aProjectileHOMINGBlockIsCarriedOntoTheCastSpec() throws IOException {
        write("seeker.yml", homingYaml("""
                  homing:
                    lerp: 0.65
                    activation_blocks: 15
                    search_radius: 10
                """));

        var cast = (CastSpec.Projectile) load().find("seeker").orElseThrow().cast();

        assertNotNull(cast.homing(), "an authored homing block must reach the cast spec");
        assertEquals(0.65, cast.homing().lerp());
        assertEquals(15.0, cast.homing().activationBlocks());
        assertEquals(10.0, cast.homing().searchRadius());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * THE ABSENT HALF, AND IT IS WHAT MAKES THIS SLICE NOT A CHANGE TO ANY SHIPPED WEAPON.
     *
     * <p>{@code hunters_bow}, {@code ember_staff}, {@code flint_staff}, {@code emberblade} and
     * {@code volley_stone}'s inner projectile all author no homing block. They get exactly the null
     * the five-argument constructor used to supply, and fly where they were aimed.
     */
    @Test
    void aProjectileWITHOUTAHomingBlockGetsNullAndFliesWhereItWasAimed() throws IOException {
        write("solar_grenade.yml", VALID);

        var cast = (CastSpec.Projectile) load().find("solar_grenade").orElseThrow().cast();

        assertNull(cast.homing(), "no homing block means no chasing, as every projectile was");
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * A PARTIAL BLOCK IS A NAMED, SKIPPED FILE -- never a bolt homing on a default nobody ruled.
     *
     * <p>{@code CastSpec.Homing} holds no defaults on purpose, so an omitted key has no honest
     * reading. Filling one in would be the descent-launders shape {@code CLAUDE.md} records: an
     * invented figure becoming the next weapon's precedent because nobody re-decides an inherited
     * number.
     *
     * <p>Each of the three is dropped in turn rather than one standing for all, because a
     * {@code reqDouble} call accidentally left off one key would pass a row that only ever omits a
     * different one.
     *
     * <h2>MEASURED: THIS ROW UNIQUELY GUARDS ONLY ONE OF ITS THREE SUB-CASES, AND IT IS NOT THE ONE
     * ANYONE WOULD PICK</h2>
     *
     * <p><b>MUT-REQ</b> -- {@code reqDouble}'s presence check replaced by {@code if (false)} --
     * reddens this row, and the failure is <b>{@code expected: <1> but was: <2>}</b>. Only
     * <b>ONE</b> of the three files got through:
     *
     * <pre>
     * no_lerp        getDouble answers 0.0 -> lerp 0    -> refused by CastSpec.Homing anyway
     * no_radius      getDouble answers 0.0 -> radius 0  -> refused by CastSpec.Homing anyway
     * no_activation  getDouble answers 0.0 -> activation 0 IS LEGAL -> LOADS. This is the kill.
     * </pre>
     *
     * <p><b>{@code activation_blocks} is the only one of the three whose absent-default is a VALID
     * value</b>, because steering from the spawn point with no ballistic run-up is a real weapon
     * (see {@code CastSpecHomingTest}'s boundary row). So two thirds of this row are protected twice
     * over, by accident of where the value guards happen to sit, and <b>the presence check is
     * load-bearing for exactly one key.</b>
     *
     * <p><b>Which means: delete the {@code no_activation} case and MUT-REQ goes green.</b> The row
     * would still look like it tested three things. Recorded in the row rather than in the class
     * header, because the person deleting a case is reading the case.</p>
     */
    @Test
    void aPARTIALHomingBlockIsNamedAndSkipped() throws IOException {
        write("no_lerp.yml", homingYaml("""
                  homing:
                    activation_blocks: 15
                    search_radius: 10
                """));
        write("no_activation.yml", homingYaml("""
                  homing:
                    lerp: 0.65
                    search_radius: 10
                """));
        write("no_radius.yml", homingYaml("""
                  homing:
                    lerp: 0.65
                    activation_blocks: 15
                """));
        write("solar_grenade.yml", VALID);

        assertEquals(1, load().size(), "all three partial files are skipped; only the valid one loads");
        assertTrue(warningText().contains("lerp"), warningText());
        assertTrue(warningText().contains("activation_blocks"), warningText());
        assertTrue(warningText().contains("search_radius"), warningText());
    }

    /**
     * A BAD VALUE IS A NAMED, SKIPPED FILE TOO -- and this row CAUSES the condition through the real
     * YAML walk rather than calling the constructor.
     *
     * <p>{@code CastSpecHomingTest} exercises the guard directly; this proves the throw actually
     * reaches the loader's {@code catch(RuntimeException)} and comes back out as a named file, which
     * is the half a core test cannot see. <b>A guard in {@code core} that a loader swallowed
     * silently would look identical to one that works.</b>
     *
     * <p>The value chosen is the NEGATIVE ACTIVATION, because it is the one that is not obvious from
     * the field name: {@code ProjectileFlight.steer} squares it, so {@code -15} behaves exactly like
     * {@code 15} and would otherwise have shipped as a working file.
     */
    @Test
    void aMEANINGLESSHomingValueIsNamedAndSkipped() throws IOException {
        write("backwards.yml", homingYaml("""
                  homing:
                    lerp: 0.65
                    activation_blocks: -15
                    search_radius: 10
                """));
        write("solar_grenade.yml", VALID);

        var registry = load();

        assertTrue(registry.find("seeker").isEmpty(), "the malformed file must be skipped");
        assertEquals(1, registry.size());
        assertTrue(warningText().contains("activation_blocks"), warningText());
        assertTrue(warningText().contains("squared"),
                "the warning must say WHY a negative is not merely odd: " + warningText());
    }

    /**
     * THE ARROW BODY: authored -> carried, absent -> null, and it is NOT the item body.
     *
     * <p>Both fields are asserted in one row because the interesting property is that they are two
     * SLOTS and not one: a schema that mapped {@code body:} onto {@code item()} would satisfy any
     * test that only asked "is a body carried", and would then render a dropped arrow ITEM -- a
     * tumbling stack, which is the exact look slice K exists to remove.
     */
    @Test
    void aProjectileArrowBodyIsCarriedAndIsADifferentSlotFromTheItemBody() throws IOException {
        write("arrow_bodied.yml", """
                id: arrow_bodied
                element: fire
                cast:
                  type: projectile
                  speed: 2.5
                  body: arrow
                on_hit:
                  - type: damage
                    amount: 20
                    element: fire
                """);
        write("plain.yml", VALID);

        var registry = load();
        var arrowBodied = (CastSpec.Projectile) registry.find("arrow_bodied").orElseThrow().cast();
        var bare = (CastSpec.Projectile) registry.find("solar_grenade").orElseThrow().cast();

        assertEquals("arrow", arrowBodied.body());
        assertNull(arrowBodied.item(),
                "`body` must NOT be mapped onto `item` -- an item body TUMBLES, which is the look "
                        + "the arrow body exists to replace");
        assertNull(bare.body(), "a projectile that names no body gets null, exactly as before");
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * ONE BOLT, ONE BODY: authoring both is a NAMED, SKIPPED FILE rather than a silent pick.
     *
     * <p>Whichever the flight chose, the other authored key would do nothing -- a tooltip-class
     * dishonesty in the content file: the author wrote a line, the server read it, nothing happened.
     *
     * <p>Asserted on the WARNING TEXT and not merely on the ability being absent, because a YAML
     * typo, a missing element and this are all "the file did not load". The asserted token
     * {@code "mutually exclusive"} appears exactly once in the message.
     */
    @Test
    void aProjectileAuthoringBothItemAndBodyIsNamedAndSkipped() throws IOException {
        write("two_bodies.yml", """
                id: two_bodies
                element: fire
                cast:
                  type: projectile
                  speed: 2.5
                  item: flint
                  body: arrow
                on_hit:
                  - type: damage
                    amount: 20
                    element: fire
                """);

        var registry = load();

        assertTrue(registry.find("two_bodies").isEmpty(), "the file must be skipped, not resolved");
        assertTrue(warningText().contains("mutually exclusive"),
                "and the operator must be told WHICH two keys fight: " + warningText());
        assertTrue(warningText().contains("two_bodies"),
                "named, so a typo in the 400th weapon is findable: " + warningText());
    }

    /**
     * A SET OF ONE IS STILL A SET. {@code body: arow} is a skipped file, not a silent no-body.
     *
     * <p><b>This is a DIFFERENT standard from {@code item:}, deliberately</b>, and the asymmetry is
     * the thing worth pinning: {@code item:} names a Material out of a huge open set and the adapter
     * warns once and falls back; {@code body:} names one of OURS out of a set of exactly one, so a
     * typo has no plausible reading and must fail loudly.
     *
     * <p>Without this the bolt would fly with no body at all -- visually identical to a weapon that
     * authored nothing, and undetectable from the content file.
     */
    @Test
    void anUnknownProjectileBodyIsNamedAndSkipped() throws IOException {
        write("typo_body.yml", """
                id: typo_body
                element: fire
                cast:
                  type: projectile
                  speed: 2.5
                  body: arow
                on_hit:
                  - type: damage
                    amount: 20
                    element: fire
                """);

        var registry = load();

        assertTrue(registry.find("typo_body").isEmpty(), "the file must be skipped");
        assertTrue(warningText().contains("arow"),
                "the warning must quote what was AUTHORED, not what was expected: " + warningText());
    }

    /**
     * {@code archetype:} is RETIRED (build system slice 2): a file still declaring it LOADS, and ONE warning
     * names every such file. This CAUSES the condition -- no shipped ability declares the key any more, so
     * production never reaches that warning, and this row is its only exercise.
     */
    @Test
    void aRetiredArchetypeKeyStillLoadsAndOneWarningNamesEveryFile() throws IOException {
        write("old_a.yml", "id: old_a\nelement: fire\narchetype: hunter\ncooldown_ticks: 20\n");
        write("old_b.yml", "id: old_b\nelement: fire\narchetype: mage\ncooldown_ticks: 20\n");
        write("clean.yml", "id: clean\nelement: fire\ncooldown_ticks: 20\n");

        AbilityRegistry registry = load();

        assertTrue(registry.find("old_a").isPresent() && registry.find("old_b").isPresent(),
                "a retired key must not cost the file");
        assertTrue(registry.find("clean").isPresent());
        assertEquals(1, warnings.size(), "ONE warning per load, not one per file: " + warningText());
        assertTrue(warningText().contains("old_a.yml") && warningText().contains("old_b.yml"), warningText());
        assertFalse(warningText().contains("clean.yml"), "the control file is not named: " + warningText());
    }

    /** The control: no file declares the key, so nothing is said. */
    @Test
    void noArchetypeKeyNoWarning() throws IOException {
        write("clean.yml", "id: clean\nelement: fire\ncooldown_ticks: 20\n");
        load();
        assertTrue(warnings.isEmpty(), warningText());
    }
}
