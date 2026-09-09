package io.github.butterflysmp.rpg.paper.content;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
 * An element is the thinnest content type: an id (the filename) and a display name. These
 * pin that, plus the fail-soft contract shared by every loader, plus that the seven shipped
 * elements load.
 */
class ElementLoaderTest {

    @TempDir
    Path dir;

    private Logger log;
    private List<LogRecord> warnings;

    @BeforeEach
    void setUp() {
        warnings = new ArrayList<>();
        log = Logger.getLogger("ElementLoaderTest-" + System.nanoTime());
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

    private ElementRegistry load() {
        return new ElementLoader(log).loadAll(new File(dir.toString()));
    }

    private String warningText() {
        return String.join("\n", warnings.stream().map(LogRecord::getMessage).toList());
    }

    @Test
    void loadsAnElementWithItsIdAndDisplayName() throws IOException {
        write("fire.yml", "display_name: \"<red>Fire</red>\"\n");

        ElementRegistry registry = load();

        assertEquals(1, registry.size());
        ElementDefinition fire = registry.find("fire").orElseThrow();
        assertEquals("fire", fire.id(), "the id is the filename");
        assertEquals("<red>Fire</red>", fire.displayName());
        assertTrue(warnings.isEmpty(), warningText());
    }

    @Test
    void displayNameDefaultsToTheId() throws IOException {
        write("kinetic.yml", "");

        assertEquals("kinetic", load().find("kinetic").orElseThrow().displayName());
    }

    @Test
    void missingDirectoryYieldsEmptyRegistry() {
        var registry = new ElementLoader(log).loadAll(new File(dir.toFile(), "does_not_exist"));
        assertEquals(0, registry.size());
    }

    /** The seven shipped elements, parsed by the loader we actually run. */
    @Test
    void theSevenBundledElementsLoad() throws IOException {
        String[] ids = {"fire", "water", "nature", "undead", "void", "wither", "kinetic"};
        for (String id : ids) {
            try (var in = getClass().getResourceAsStream("/content/elements/" + id + ".yml")) {
                assertNotNull(in, "bundled element is missing from the classpath: " + id);
                Files.write(dir.resolve(id + ".yml"), in.readAllBytes());
            }
        }

        ElementRegistry registry = load();

        assertTrue(warnings.isEmpty(), warningText());
        assertEquals(7, registry.size());
        for (String id : ids) {
            assertTrue(registry.find(id).isPresent(), id + " must load");
        }
    }

    // --- The two properties an element gained ----------------------------------------------------

    @Test
    void bothNewFieldsAreParsedAndABAREElementStillLoadsWithNEITHER() throws IOException {
        // ABSENT MEANS OFF, and this row is the reason the loader is lenient rather than strict.
        // RpgPlugin.saveDefaultContent uses saveResource(path, false), which NEVER overwrites, so a
        // server whose data folder predates this slice holds seven one-field files. Making either
        // field required would take seven working elements off that server to fix a cosmetic gap.
        // ContentValidator names the omission at boot instead.
        write("fire.yml", "display_name: \"<red>Fire</red>\"\n"
                + "damage_symbol: \"<gold>F</gold>\"\n"
                + "applies_status: scorch\n");
        write("nature.yml", "display_name: \"<green>Nature</green>\"\n");

        ElementRegistry registry = load();
        assertTrue(warnings.isEmpty(), warningText());

        ElementDefinition fire = registry.find("fire").orElseThrow();
        assertEquals("scorch", fire.appliesStatus(), "fire accrues the status it names");
        assertNotNull(fire.damageSymbol(), "and carries a parsed glyph");
        assertEquals("F", PlainTextComponentSerializer.plainText().serialize(fire.damageSymbol()),
                "the glyph is the MiniMessage content, deserialized");
        assertEquals(NamedTextColor.GOLD, fire.damageSymbol().color(),
                "and it wears the colour the SAME field declared -- one value, nothing to keep in sync");

        ElementDefinition nature = registry.find("nature").orElseThrow();
        assertNull(nature.appliesStatus(), "an element that declares no status accrues nothing");
        assertNull(nature.damageSymbol(), "and one that declares no glyph draws none");

        // Mutation: read the wrong key for either field -> the fire assertions redden.
        // Mutation: make either field required (throw when absent) -> nature is SKIPPED, so
        // registry.find("nature") is empty and the warning list is non-empty -> reddens.
    }

    @Test
    void theGLYPHIsParsedATLOADSoAMalformedOneIsANamedSkippedFile() throws IOException {
        // MEASURED, AND IT REDDENED THIS ROW FIRST. The guard was written as a try/catch around
        // MiniMessage.deserialize, with a javadoc claiming a malformed glyph became a named, skipped
        // file. A throwaway probe over ten malformed inputs -- unknown tag, unknown colour, bad hex,
        // unclosed tag, mismatched close, a bare "<", bogus gradient -- showed MiniMessage THROWS ON
        // NONE OF THEM. It renders an unparsed tag as its own source text. So the catch was dead
        // code, and the javadoc would have been a third comment in this repo claiming an enforcement
        // that did not exist.
        //
        // THE REAL DEFECT IS SILENT AND COSMETIC: damage_symbol: "<god>F</god>" draws the literal
        // characters <god>F</god> over a mob on every hit. So the guard moved onto the RESULT --
        // text still holding < or > means a tag did not parse, the only signal MiniMessage gives.
        // This row asserts the BEHAVIOUR rather than the mechanism, which is why it survived the
        // guard being rewritten underneath it.
        //
        // It also arms a fail-soft path that had never executed: the loader catch was unreachable
        // (YamlConfiguration does not throw on bad YAML; filename ids cannot collide), so the skip,
        // the log line and the summary count all run here for the first time.
        write("fire.yml", "display_name: \"<red>Fire</red>\"\n"
                + "damage_symbol: \"<not_a_tag>F</not_a_tag>\"\n");
        write("water.yml", "display_name: \"<aqua>Water</aqua>\"\n");

        ElementRegistry registry = load();

        assertTrue(registry.find("fire").isEmpty(), "the malformed element is SKIPPED, not half-loaded");
        assertTrue(registry.find("water").isPresent(), "and every other element still loads");
        assertTrue(warningText().contains("Skipping malformed element 'fire.yml'"),
                "named, so an operator knows which file: " + warningText());
        // Mutation: store the raw string instead of parsing here -> fire LOADS -> reddens on the
        // first assertion, and the failure moves to the popup's hot path where nothing names it.
    }

    @Test
    void aNONSCALARValueIsAHalfFinishedEditRatherThanAnAbsence() throws IOException {
        // THE ONE THAT WOULD OTHERWISE BE SILENT, AND THE REASON THIS GUARD EARNS ITS LINES.
        // `applies_status: [scorch, soaked]` is a plausible thing to try. getString returns null for
        // a list, so without the guard the element would load, look correct, and accrue nothing
        // FOREVER -- a shipped ability silently doing nothing, which is the exact failure this repo
        // keeps recording.
        //
        // A BLANK GLYPH USED TO BE ASSERTED HERE AS THE SAME CLASS, AND IT IS NOT ANY MORE. That row
        // read "and so is a blank glyph" and its premise was that every element is meant to be
        // marked. Kinetic is now deliberately unmarked, so `damage_symbol: ""` is a DECLARATION and
        // gets its own row below. Re-derived rather than deleted: the non-scalar half is untouched
        // and still guards the silent case.
        write("fire.yml", "display_name: \"<red>Fire</red>\"\napplies_status:\n  - scorch\n  - soaked\n");
        write("nature.yml", "display_name: \"<green>Nature</green>\"\n");

        ElementRegistry registry = load();

        assertTrue(registry.find("fire").isEmpty(), "a list where one status belongs is a skip");
        assertTrue(registry.find("nature").isPresent(), "the well-formed element is unaffected");
        assertTrue(warningText().contains("1 element file(s) were skipped"),
                "the summary counts it: " + warningText());
        // Mutation: drop the isString guard -> fire loads with appliesStatus null -> reddens.
    }

    @Test
    void anEMPTYGlyphIsADECLARATIONAndANABSENTOneIsAGAP() throws IOException {
        // THE DISTINCTION #6 EXISTS TO PRESERVE, and it is the whole reason kinetic writes "" rather
        // than dropping the field.
        //
        //   ""       a decision -- this element is deliberately unmarked. Loads, no warning.
        //   absent   a GAP. saveResource(path, false) never overwrites, so a data folder predating
        //            the field looks exactly like the decision unless the two are kept apart.
        //
        // Same rule Scorch.UNDECLARED_CAP states for a different field: ABSENCE IS NOT A NEUTRAL
        // VALUE. Collapse them and an operator can never be told which of their elements are
        // unmarked ON PURPOSE.
        write("kinetic.yml", "display_name: \"<white>Kinetic</white>\"\ndamage_symbol: \"\"\n");
        write("nature.yml", "display_name: \"<green>Nature</green>\"\n");

        ElementRegistry registry = load();

        var kinetic = registry.find("kinetic").orElseThrow();
        assertNotNull(kinetic.damageSymbol(),
                "an EMPTY glyph is a value, not a null -- that is what tells it from an absent one");
        assertEquals("", PlainTextComponentSerializer.plainText().serialize(kinetic.damageSymbol()),
                "and it renders as nothing, so the number is drawn bare");

        assertNull(registry.find("nature").orElseThrow().damageSymbol(),
                "an ABSENT glyph is null -- the gap ContentValidator names at boot");

        assertTrue(warnings.isEmpty(), "neither is malformed, so neither is skipped: " + warningText());
        // Mutation: throw on a blank value again -> kinetic is SKIPPED -> reddens.
        // Mutation: return null for a blank value -> the two become indistinguishable, kinetic gets
        // named as a legacy gap at every boot, and the first assertion reddens.
    }

    @Test
    void ANONBLANKGlyphThatRendersAsNOTHINGIsStillAHalfFinishedEdit() throws IOException {
        // The third case, which the empty declaration must not swallow. `"<gold></gold>"` is tags
        // with no content between them -- authored, non-blank, and renders to nothing. It is a typo,
        // not a decision, and it stays a named skip. The message points at the decision spelling so
        // an author who meant "unmarked" is told how to say it.
        write("fire.yml", "display_name: \"<red>Fire</red>\"\ndamage_symbol: \"<gold></gold>\"\n");

        ElementRegistry registry = load();

        assertTrue(registry.find("fire").isEmpty(), "tags that consumed the whole value are a skip");
        assertTrue(warningText().contains("Write \"\" if the element is meant to be unmarked"),
                "and the message names the spelling for the deliberate case: " + warningText());
        // Mutation: treat any empty RENDER as the deliberate case -> fire loads unmarked, a typo
        // ships as a decision -> reddens.
    }

    @Test
    void theBundledFIREDeclaresBothAndTHESHIPPEDBYTESAreWhatIsAsserted() throws IOException {
        // Against the real resources, not a fixture. This is the row that catches "the comment was
        // rewritten and the field was never added" -- a fixture-only test would pass with fire.yml
        // untouched, because the fixture is written by the test itself.
        for (String id : new String[] {"fire", "kinetic"}) {
            try (var in = getClass().getResourceAsStream("/content/elements/" + id + ".yml")) {
                assertNotNull(in, "bundled element missing from the classpath: " + id);
                Files.write(dir.resolve(id + ".yml"), in.readAllBytes());
            }
        }

        ElementRegistry registry = load();
        assertTrue(warnings.isEmpty(), warningText());

        assertEquals("scorch", registry.find("fire").orElseThrow().appliesStatus(),
                "shipped fire accrues scorch -- the whole point of the slice");
        assertNotNull(registry.find("fire").orElseThrow().damageSymbol(), "and is marked");

        // KINETIC IS THE CONTROL, IN SHIPPED CONTENT. It carries a glyph and NO accrual, so an
        // ironblade swing draws its mark and does not burn on the same code path an emberblade
        // swing burns through. Without a shipped element that declares one field and not the other,
        // "content-driven" is an assertion about code nobody has separated.
        ElementDefinition kinetic = registry.find("kinetic").orElseThrow();
        assertNotNull(kinetic.damageSymbol(), "kinetic is marked too -- every element is");
        assertNull(kinetic.appliesStatus(), "but it accrues NOTHING, and that is the control");
        // Mutation: add applies_status to kinetic.yml -> reddens, and the in-game control is gone.
    }

    @Test
    void aCHEVRONGlyphIsRejectedTooAndTheMessageSaysSoRatherThanBlamingATag() throws IOException {
        // THE LIMITATION, MEASURED RATHER THAN CLAIMED IN A JAVADOC.
        //
        // The unparsed-tag check cannot tell a typo from an intentional chevron: MiniMessage passes a
        // bare "<" through untouched, so "<white><</white>" arrives here as the single character "<"
        // -- byte-identical to what a mistyped tag leaves behind. Rejecting both is the right call.
        // NAMING A CAUSE WE CANNOT DISTINGUISH IS NOT: an author who wrote a chevron on purpose would
        // be told to check the spelling of a tag they never wrote, and would go hunting a bug that is
        // not there. An error that misnames its cause is worse than one that admits what it cannot
        // separate.
        //
        // This row also re-measures a claim about a LIBRARY -- that MiniMessage prints a bare "<"
        // rather than rejecting it. That is exactly the kind of claim which is true when written and
        // silently false two versions later, so it is pinned rather than asserted in prose.
        write("fire.yml", "display_name: \"<red>Fire</red>\"\n"
                + "damage_symbol: \"<white><</white>\"\n");

        ElementRegistry registry = load();

        assertTrue(registry.find("fire").isEmpty(),
                "a chevron is rejected like a broken tag, because they are the same string here");
        assertTrue(warningText().contains("cannot contain < or >"),
                "the message states the LIMITATION rather than a cause: " + warningText());
        assertTrue(warningText().contains("chevron"),
                "and it names the chevron case explicitly, so an author who meant one is not sent "
                        + "hunting a tag they never wrote: " + warningText());
        // Mutation: reword the message to "a tag failed to parse, check the spelling" -> the last two
        // assertions redden. Mutation: drop the < / > check -> the first reddens.
    }
}
