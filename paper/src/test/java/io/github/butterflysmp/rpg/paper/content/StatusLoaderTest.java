package io.github.butterflysmp.rpg.paper.content;

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
 * Same fail-soft contract as AbilityLoaderTest. No server: the loader resolves no
 * PotionEffectType, which is the whole reason potion_type stays a NamespacedKey.
 */
class StatusLoaderTest {

    @TempDir
    Path dir;

    private Logger log;
    private List<LogRecord> warnings;

    @BeforeEach
    void setUp() {
        warnings = new ArrayList<>();
        log = Logger.getLogger("StatusLoaderTest-" + System.nanoTime());
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

    private StatusRegistry load() {
        return new StatusLoader(log).loadAll(new File(dir.toString()));
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    @Test
    void loadsAFireStatusAndTakesTheIdFromTheFilename() throws IOException {
        write("scorch.yml", "kind: fire\n");

        StatusRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());
        var fire = assertInstanceOf(StatusDefinition.Fire.class, registry.find("scorch").orElseThrow());
        assertEquals("scorch", fire.id());
    }

    @Test
    void loadsARootedStatusAsImmobilizeThatDoesNotSuppressAttacks() throws IOException {
        write("rooted.yml", "kind: rooted\n");

        var immobilize = assertInstanceOf(StatusDefinition.Immobilize.class,
                load().find("rooted").orElseThrow());

        assertEquals("rooted", immobilize.id());
        assertFalse(immobilize.suppressAttacks(), "Rooted immobilizes but does not suppress attacks");
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void loadsAFreezeStatusAsImmobilizeThatSuppressesAttacks() throws IOException {
        write("freeze.yml", "kind: freeze\n");

        var immobilize = assertInstanceOf(StatusDefinition.Immobilize.class,
                load().find("freeze").orElseThrow());

        assertEquals("freeze", immobilize.id());
        assertTrue(immobilize.suppressAttacks(), "Freeze is Immobilize + attack suppression");
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void loadsASoakedStatusFromTheKind() throws IOException {
        write("soaked.yml", "kind: soaked\n");

        var soaked = assertInstanceOf(StatusDefinition.Soaked.class,
                load().find("soaked").orElseThrow());

        assertEquals("soaked", soaked.id());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * The key is parsed, not resolved. Resolving it would need Registry.MOB_EFFECT,
     * which would need a server, which would make this test impossible.
     */
    @Test
    void loadsAPotionStatusWithoutResolvingTheEffectType() throws IOException {
        write("sluggish.yml", """
                kind: potion
                potion_type: slowness
                """);

        var potion = assertInstanceOf(StatusDefinition.Potion.class,
                load().find("sluggish").orElseThrow());

        assertEquals("minecraft:slowness", potion.potionType().toString());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * NamespacedKey.minecraft("Slowness") would throw, and it would throw later,
     * from inside a scheduler task. fromString returns null, so it lands here.
     */
    @Test
    void capitalisedPotionTypeIsSkippedNotThrown() throws IOException {
        write("aaa_shouty.yml", """
                kind: potion
                potion_type: Slowness
                """);
        write("scorch.yml", "kind: fire\n");

        StatusRegistry registry = assertDoesNotThrow(this::load);

        assertEquals(1, registry.size(), "the valid status must still load");
        assertTrue(warningText().contains("aaa_shouty.yml"), warningText());
        assertTrue(warningText().contains("Invalid potion_type"), warningText());
    }

    @Test
    void potionWithoutTypeIsSkippedNotCrashed() throws IOException {
        write("aaa_bare.yml", "kind: potion\n");
        write("scorch.yml", "kind: fire\n");

        StatusRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("aaa_bare.yml"), warningText());
        assertTrue(warningText().contains("potion_type"), warningText());
    }

    @Test
    void unknownKindIsSkippedNotCrashed() throws IOException {
        write("aaa_bogus.yml", "kind: teleport\n");
        write("scorch.yml", "kind: fire\n");

        StatusRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("teleport"), warningText());
    }

    @Test
    void missingKindIsSkippedNotCrashed() throws IOException {
        write("aaa_empty.yml", "duration_ticks: 40\n");
        write("scorch.yml", "kind: fire\n");

        StatusRegistry registry = load();

        assertEquals(1, registry.size());
        assertTrue(warningText().contains("kind"), warningText());
    }

    @Test
    void missingDirectoryYieldsEmptyRegistry() {
        var registry = new StatusLoader(log).loadAll(new File(dir.toFile(), "does_not_exist"));
        assertEquals(0, registry.size());
    }

    /** The content we actually ship, parsed by the loader we actually run. */
    @Test
    void bundledScorchContentLoads() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/statuses/scorch.yml")) {
            assertNotNull(in, "bundled content is missing from the classpath");
            Files.write(dir.resolve("scorch.yml"), in.readAllBytes());
        }

        StatusRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(1, registry.size());
        // Scorch, NOT Fire. It was `kind: fire` until the scorch slice, when the burn stopped being
        // vanilla-rated: this row is what noticed the content change, which is the job. Fire still
        // exists and is still right for anything wanting only the look -- see StatusDefinition.
        assertInstanceOf(StatusDefinition.Scorch.class, registry.find("scorch").orElseThrow());
    }

    @Test
    void loadsAFireStatusStillBecauseSCORCHDidNotREPLACEIt() throws IOException {
        // The two kinds are distinct and both are reachable. If scorch had been implemented by
        // changing what `kind: fire` MEANS, every existing fire status would have silently become a
        // capped, credited, defense-bypassing DoT -- and nothing in content would have said so.
        write("emberglow.yml", "kind: fire");

        StatusRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertInstanceOf(StatusDefinition.Fire.class, registry.find("emberglow").orElseThrow());
        // Mutation: map "fire" to StatusDefinition.Scorch -> reddens.
    }

    /**
     * {@code kind: wither} (WITHER-STATUS) loads with its IMMUNE list, lowercased; absent means none.
     * Mutation: drop the {@code "wither"} arm in {@code StatusLoader.parse} -> the file is skipped as an
     * unknown kind -> reddens.
     */
    @Test
    void loadsAWitherStatusWithItsImmuneTypesAndAnAbsentListIsEmpty() throws IOException {
        write("withering.yml", """
                kind: wither
                immune:
                  - wither_skeleton
                  - Wither
                """);
        write("plain_wither.yml", "kind: wither\n");

        StatusRegistry registry = load();
        var wither = assertInstanceOf(StatusDefinition.Wither.class, registry.find("withering").orElseThrow());
        assertEquals(java.util.Set.of("wither_skeleton", "wither"), wither.immune(), "lowercased");
        assertTrue(wither.isImmune("wither_skeleton"));
        assertFalse(wither.isImmune("zombie"), "the control: an unlisted type is not immune");
        var plain = assertInstanceOf(StatusDefinition.Wither.class, registry.find("plain_wither").orElseThrow());
        assertTrue(plain.immune().isEmpty(), "absent means nobody is immune, not a skipped file");
        assertTrue(warnings.isEmpty(), warningText());
    }

    /**
     * A NAMESPACED IMMUNE KEY IS A NAMED, SKIPPED FILE. {@code minecraft:wither_skeleton} could never
     * match the bare type key the check compares, so loading it would protect nobody while reading as a
     * ruling. Mutation: drop the key check -> the file loads -> reddens.
     */
    @Test
    void aNamespacedImmuneKeyIsSkippedAndNamed() throws IOException {
        write("withering.yml", """
                kind: wither
                immune:
                  - minecraft:wither_skeleton
                """);

        assertTrue(load().find("withering").isEmpty(), "the file must be skipped");
        assertTrue(warningText().contains("minecraft:wither_skeleton"), warningText());
    }

    /**
     * AN UNKNOWN IMMUNE KEY IS REFUSED BY NAME, AND THE REST OF THE LIST STILL LOADS (the seat,
     * 2026-10-01). {@code wither_skelton} is well-formed and names nothing; the WARN begins
     * {@code Refusing} so R0c's pattern finds it, and names the file and the key.
     *
     * <p>Mutation: drop the {@code KNOWN_ENTITY_TYPES} check -> the typo loads into the set -> reddens.
     */
    @Test
    void anUnknownImmuneKeyIsRefusedByNameAndTheRestLoads() throws IOException {
        write("withering.yml", """
                kind: wither
                immune:
                  - wither_skelton
                  - wither
                """);

        var wither = assertInstanceOf(StatusDefinition.Wither.class, load().find("withering").orElseThrow(),
                "the file still loads");
        assertEquals(java.util.Set.of("wither"), wither.immune(), "the correct key survives, the typo does not");
        assertEquals(1, warnings.size(), "one WARN: " + warningText());
        assertTrue(warningText().startsWith("Refusing"), warningText());
        assertTrue(warningText().contains("withering.yml"), "names the file: " + warningText());
        assertTrue(warningText().contains("wither_skelton"), "names the key: " + warningText());
    }

    /** The registry the check reads is the real one, not empty: the shipped keys are in it, a typo is not. */
    @Test
    void theEntityTypeListIsTheJarsAndKnowsTheShippedKeys() {
        assertTrue(StatusLoader.KNOWN_ENTITY_TYPES.contains("wither_skeleton"));
        assertTrue(StatusLoader.KNOWN_ENTITY_TYPES.contains("wither"));
        assertTrue(StatusLoader.KNOWN_ENTITY_TYPES.contains("zombie"));
        assertFalse(StatusLoader.KNOWN_ENTITY_TYPES.contains("wither_skelton"));
    }

    /**
     * THE SHIPPED BYTES: {@code withering.yml} is {@code kind: wither} immune to wither skeletons and
     * the Wither (Q-W6), read from the classpath and not from a fixture the test wrote itself.
     * Mutation: delete a key from the shipped list -> reddens.
     */
    @Test
    void theShippedWitheringIsAWitherImmuneToWitherSkeletonsAndTheWither() throws IOException {
        try (var in = getClass().getResourceAsStream("/content/statuses/withering.yml")) {
            assertNotNull(in, "withering.yml missing from the classpath");
            write("withering.yml", new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        }
        var wither = assertInstanceOf(StatusDefinition.Wither.class, load().find("withering").orElseThrow());
        assertEquals(java.util.Set.of("wither_skeleton", "wither"), wither.immune());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** Ruling 22: a stale surge.yml in an old data folder is SKIPPED and named once, never deleted. */
    @Test
    void aRetiredStatusFileIsSkippedAndNamedOnce() throws IOException {
        write("surge.yml", "kind: fire\n");
        write("clean.yml", "kind: fire\n");
        StatusRegistry registry = load();
        assertTrue(registry.find("surge").isEmpty(), "a retired status must not load from a stale copy");
        assertTrue(registry.find("clean").isPresent(), "the control file still loads: " + warningText());
        assertEquals(1, warnings.size(), "ONE warning: " + warningText());
        assertTrue(warningText().contains("surge.yml"), warningText());
        assertFalse(warningText().contains("clean.yml"), warningText());
    }
}
