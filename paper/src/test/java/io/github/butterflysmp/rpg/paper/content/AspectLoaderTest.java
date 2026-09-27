package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.build.AspectRegistry;
import io.github.butterflysmp.rpg.core.build.CellKey;
import io.github.butterflysmp.rpg.core.build.Loadout;
import io.github.butterflysmp.rpg.core.build.PoolDefinition;
import io.github.butterflysmp.rpg.core.build.PoolRegistry;
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
import java.util.function.Predicate;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PLAN-build-system.md section 3.5: the aspect loader's parse refusals and its pool-level checks. */
class AspectLoaderTest {

    @TempDir
    Path dir;

    private final List<String> warnings = new ArrayList<>();
    private Logger log;

    /** The shipped abilities, loaded for real: every check here resolves against a real target. */
    private static final AbilityRegistry ABILITIES =
            new AbilityLoader(Logger.getAnonymousLogger()).loadAll(new File("src/main/resources/content/abilities"));

    /** The traced status kinds: rooted and soaked continuous; scorch not. */
    private static final Predicate<String> TRACED = id -> id.equals("rooted") || id.equals("soaked") || id.equals("freeze");

    @BeforeEach
    void captureWarnings() {
        log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord record) { warnings.add(record.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
    }

    private void write(String name, String yaml) throws IOException {
        Files.writeString(dir.resolve(name), yaml, StandardCharsets.UTF_8);
    }

    private String warningText() {
        return String.join(" | ", warnings);
    }

    /** A Fire Ranger pool listing exactly these aspects. */
    private static PoolRegistry rangerListing(String... aspects) {
        PoolRegistry pools = new PoolRegistry();
        pools.register(new PoolDefinition(new CellKey("ranger", "fire"), "Fire Ranger",
                List.of("ultimate_placeholder_ranger"), List.of("recall", "solar_lance"),
                new Loadout("ultimate_placeholder_ranger", "recall", "solar_lance"), List.of(), List.of(aspects)));
        return pools;
    }

    private AspectRegistry checked(AspectRegistry parsed, PoolRegistry pools) {
        return new AspectLoader(log).checkAgainstPools(parsed, pools, ABILITIES::find, TRACED);
    }

    @Test
    void theControlAbilitiesLoaded() {
        assertTrue(ABILITIES.find("solar_lance").isPresent() && ABILITIES.find("recall").isPresent(),
                "the real targets must load, or every check below would refuse for the wrong reason");
    }

    // ------------------------------------------------------------------ parse refusals, by name

    @Test
    void aFieldOffTheWhitelistIsRefusedNamingIt() throws IOException {
        write("bad_field.yml", "target: solar_lance\nmodify:\n  - { field: weapon_damage, percent: 10 }\n");
        write("good.yml", "target: solar_lance\nmodify:\n  - { field: damage.amount, percent: 10 }\n");
        AspectRegistry parsed = new AspectLoader(log).loadAll(dir.toFile());
        assertEquals(1, parsed.size(), warningText());
        assertTrue(warningText().contains("bad_field.yml") && warningText().contains("weapon_damage"), warningText());
    }

    @Test
    void aNonVisualInAddOnCastIsRefused() throws IOException {
        write("cast_damage.yml", "target: solar_lance\nadd_on_cast:\n  - { type: damage, amount: 3, element: fire }\n");
        assertEquals(0, new AspectLoader(log).loadAll(dir.toFile()).size());
        assertTrue(warningText().contains("cast_damage.yml"), warningText());
    }

    // ------------------------------------------------------------------ the pool-level checks

    /** THE ROW THE "drop the zero-match refusal" MUTATION REDDENS: a ray authors no knockback to change. */
    @Test
    void aModifyMatchingNothingInTheTargetIsRefused() throws IOException {
        write("push_lance.yml", "target: solar_lance\nmodify:\n  - { field: knockback.strength, percent: 20 }\n");
        AspectRegistry kept = checked(new AspectLoader(log).loadAll(dir.toFile()), rangerListing("push_lance"));
        assertEquals(0, kept.size());
        assertTrue(warningText().contains("push_lance") && warningText().contains("knockback.strength")
                && warningText().contains("matches nothing"), warningText());
    }

    @Test
    void aStatusDurationOnScorchIsRefused() throws IOException {
        // recall carries no status, so the fixture target is a scorch-carrying ability:
        // add one inline through a pool that lists an aspect on it. No shipped ability authors scorch as a
        // Status effect, so this row builds the case by hand on a registry of its own.
        AbilityRegistry abilities = new AbilityRegistry();
        abilities.register(new io.github.butterflysmp.rpg.core.ability.AbilityDefinition("burner", "Burner", "fire", 100,
                new io.github.butterflysmp.rpg.core.ability.ResourceCost("mana", 10),
                new io.github.butterflysmp.rpg.core.ability.CastSpec.Self(),
                List.of(new io.github.butterflysmp.rpg.core.ability.effect.EffectSpec.Status("scorch", 60, 0))));
        write("longer_burn.yml", "target: burner\nmodify:\n  - { field: status.duration_ticks, flat: 20 }\n");
        PoolRegistry pools = new PoolRegistry();
        pools.register(new PoolDefinition(new CellKey("mage", "fire"), "x", List.of("burner"), List.of("a", "b"),
                new Loadout("burner", "a", "b"), List.of(), List.of("longer_burn")));
        AspectRegistry kept = new AspectLoader(log).checkAgainstPools(new AspectLoader(log).loadAll(dir.toFile()),
                pools, abilities::find, TRACED);
        assertEquals(0, kept.size());
        assertTrue(warningText().contains("longer_burn") && warningText().contains("scorch"), warningText());
    }

    /** THE ROW THE "drop the pair check" MUTATION REDDENS: each alone is legal; together -120% damage. */
    @Test
    void anIllegalPairIsRefusedThoughEachAloneIsLegal() throws IOException {
        write("dim_a.yml", "target: solar_lance\nmodify:\n  - { field: damage.amount, percent: -60 }\n");
        write("dim_b.yml", "target: solar_lance\nmodify:\n  - { field: damage.amount, percent: -60 }\n");
        AspectRegistry parsed = new AspectLoader(log).loadAll(dir.toFile());
        assertEquals(2, checked(parsed, rangerListing("dim_a")).size(), "dim_a alone refuses nothing: " + warningText());
        assertEquals(2, checked(parsed, rangerListing("dim_b")).size(), "dim_b alone refuses nothing: " + warningText());
        warnings.clear();
        AspectRegistry together = checked(parsed, rangerListing("dim_a", "dim_b"));
        assertEquals(0, together.size(), "BOTH of the pair are refused");
        assertTrue(warningText().contains("dim_a") && warningText().contains("dim_b")
                && warningText().contains("together"), warningText());
    }

    // ------------------------------------------------------------------ the shipped aspects

    // ------------------------------------------------------------------ section 7.4: the recast

    private static final String UPDRAFT = """
            target: recall
            recast:
              ability: recall_updraft
              from_ticks: 10
              window_ticks: 50
            """;

    @Test
    void aRecastParsesAndIsAnAspectOnItsOwn() throws IOException {
        write("updraft.yml", UPDRAFT);
        AspectRegistry parsed = new AspectLoader(log).loadAll(dir.toFile());
        var recast = parsed.find("updraft").orElseThrow().recast();
        assertEquals(new io.github.butterflysmp.rpg.core.build.AspectDefinition.Recast("recall_updraft", 10, 50), recast);
        assertEquals(1, checked(parsed, rangerListing("updraft")).size(), warningText());
    }

    @Test
    void aRecastMissingAKeyOrWithItsTicksOutOfOrderIsRefused() throws IOException {
        write("no_window.yml", "target: recall\nrecast:\n  ability: recall_updraft\n  from_ticks: 10\n");
        write("backwards.yml", "target: recall\nrecast:\n  ability: recall_updraft\n  from_ticks: 50\n  window_ticks: 10\n");
        assertEquals(0, new AspectLoader(log).loadAll(dir.toFile()).size());
        assertTrue(warningText().contains("no_window.yml") && warningText().contains("window_ticks"), warningText());
        assertTrue(warningText().contains("backwards.yml"), warningText());
    }

    @Test
    void aRecastOfAnUnknownAbilityIsRefused() throws IOException {
        write("updraft.yml", UPDRAFT.replace("recall_updraft", "leap_nowhere"));
        assertEquals(0, checked(new AspectLoader(log).loadAll(dir.toFile()), rangerListing("updraft")).size());
        assertTrue(warningText().contains("leap_nowhere"), warningText());
    }

    /** The follow-up's only door is the recast: a pool offering it as an Active is refused. */
    @Test
    void aRecastWhoseFollowUpAPoolOffersIsRefused() throws IOException {
        write("updraft.yml", UPDRAFT);
        PoolRegistry pools = new PoolRegistry();
        pools.register(new PoolDefinition(new CellKey("ranger", "fire"), "Fire Ranger",
                List.of("ultimate_placeholder_ranger"), List.of("recall", "solar_lance", "recall_updraft"),
                new Loadout("ultimate_placeholder_ranger", "recall", "solar_lance"), List.of(), List.of("updraft")));
        assertEquals(0, checked(new AspectLoader(log).loadAll(dir.toFile()), pools).size());
        assertTrue(warningText().contains("recall_updraft") && warningText().contains("NO pool"), warningText());
    }

    /** One follow-up per cast: two recast aspects on one target in one pool are refused as a pair, by name. */
    @Test
    void twoRecastsOnOneTargetAreRefusedAsAPair() throws IOException {
        write("updraft.yml", UPDRAFT);
        write("second_wind.yml", UPDRAFT);
        AspectRegistry kept = checked(new AspectLoader(log).loadAll(dir.toFile()), rangerListing("updraft", "second_wind"));
        assertEquals(0, kept.size());
        assertTrue(warningText().contains("second_wind") && warningText().contains("ONE follow-up"), warningText());
    }

    /** Ruling 26's stale copy, the arc_surge way: named once, skipped, never deleted. */
    @Test
    void aStaleBankedEmbersCopyIsSkippedNotRefused() throws IOException {
        write("banked_embers.yml", "target: rekindle\nadd_on_cast:\n  - { type: visual, visual_id: ember_burst }\n");
        AspectRegistry parsed = new AspectLoader(log).loadAll(dir.toFile());
        assertEquals(0, parsed.size());
        assertTrue(warningText().contains("banked_embers.yml") && warningText().contains("DELETED"), warningText());
        assertTrue(Files.exists(dir.resolve("banked_embers.yml")), "never deleted");
    }

    /** The four shipped aspects parse, pass their own pools' checks, and are marked as placeholders. */
    @Test
    void theShippedAspectsLoadPassAndAreMarkedPlaceholders() throws IOException {
        File shipped = new File("src/main/resources/content/aspects");
        File[] files = shipped.listFiles((d, n) -> n.endsWith(".yml"));
        assertTrue(files != null && files.length == 4, "four shipped aspects");
        AspectRegistry parsed = new AspectLoader(log).loadAll(shipped);
        PoolRegistry pools = new PoolRegistry();
        pools.register(new PoolDefinition(new CellKey("ranger", "fire"), "Fire Ranger",
                List.of("ultimate_placeholder_ranger"), List.of("recall", "solar_lance"),
                new Loadout("ultimate_placeholder_ranger", "recall", "solar_lance"), List.of(),
                List.of("searing_lance", "updraft")));
        pools.register(new PoolDefinition(new CellKey("mage", "fire"), "Fire Mage",
                List.of("ultimate_placeholder_mage"), List.of("ember_step", "solar_grenade", "solar_lance"),
                new Loadout("ultimate_placeholder_mage", "ember_step", "solar_grenade"), List.of(),
                List.of("cinder_wake", "lingering_sun")));
        AspectRegistry kept = checked(parsed, pools);
        assertEquals(4, kept.size(), "every shipped aspect passes: " + warningText());
        assertFalse(warningText().contains("Refusing"), warningText());
        // Every shipped aspect is a placeholder EXCEPT updraft, which is RULED (rulings 28-30) and says so.
        for (File f : files) {
            String text = Files.readString(f.toPath());
            if (f.getName().equals("updraft.yml")) {
                assertTrue(text.contains("ruling 29") && !text.contains("PLACEHOLDER"), f.getName());
            } else {
                assertTrue(text.contains("# PLACEHOLDER -- Ben designs"), f.getName());
            }
        }
    }
}
