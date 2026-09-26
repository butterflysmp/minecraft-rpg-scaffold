package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import io.github.butterflysmp.rpg.core.build.FragmentRegistry;
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
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FragmentLoaderTest {

    @TempDir
    Path dir;

    private final List<String> warnings = new ArrayList<>();
    private Logger log;

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

    /**
     * A FAKE item-material check: resolving a real Material initialises the server's registries, which a
     * unit test does not have. It knows the test's icons and refuses everything else.
     */
    private static final java.util.function.Predicate<String> ITEM =
            name -> java.util.Set.of("RED_DYE").contains(name);

    private FragmentRegistry load() {
        return new FragmentLoader(log, ITEM).loadAll(dir.toFile());
    }

    private String warningText() {
        return String.join(" | ", warnings);
    }

    @Test
    void aWellFormedFragmentLoadsWithItsIdFromTheFilename() throws IOException {
        write("vigor.yml", "display_name: \"<red>Vigor</red>\"\nicon: red_dye\ndescription: [\"More of you.\"]\n"
                + "modifiers:\n  max_health: 4\n  crit_chance: 0.02\n");
        FragmentRegistry registry = load();
        var vigor = registry.find("vigor").orElseThrow();
        assertEquals(4.0, vigor.modifiers().get(AccessoryStat.MAX_HEALTH));
        assertEquals(0.02, vigor.modifiers().get(AccessoryStat.CRIT_CHANCE));
        assertEquals("red_dye", vigor.icon());
        assertEquals(List.of("More of you."), vigor.description());
        assertTrue(warnings.isEmpty(), warningText());
    }

    /** Each refusal skips ITS file by name; the good file beside it still loads. */
    @Test
    void everyRefusalSkipsItsFileByNameAndTheControlStillLoads() throws IOException {
        write("good.yml", "icon: red_dye\nmodifiers:\n  defense: 1\n");
        write("negative.yml", "icon: red_dye\nmodifiers:\n  crit_chance: -0.01\n");
        write("classdmg.yml", "icon: red_dye\nmodifiers:\n  class_damage: 3\n");
        write("unknown_stat.yml", "icon: red_dye\nmodifiers:\n  luck: 1\n");
        write("not_a_number.yml", "icon: red_dye\nmodifiers:\n  defense: lots\n");
        write("bad_icon.yml", "icon: not_a_material\nmodifiers:\n  defense: 1\n");
        write("no_modifiers.yml", "icon: red_dye\n");

        FragmentRegistry registry = load();

        assertEquals(1, registry.size(), "only the control loads: " + warningText());
        assertTrue(registry.find("good").isPresent());
        for (String skipped : List.of("negative", "classdmg", "unknown_stat", "not_a_number", "bad_icon", "no_modifiers")) {
            assertTrue(warningText().contains(skipped + ".yml"), skipped + " is named: " + warningText());
        }
        assertTrue(warningText().contains("positive-only"), "the negative is refused for the ruled reason: " + warningText());
    }

    /** Every shipped fragment loads, and each is marked a placeholder until Ben designs them. */
    @Test
    void everyShippedFragmentLoadsAndIsMarkedAPlaceholder() throws IOException {
        File shipped = new File("src/main/resources/content/fragments");
        File[] files = shipped.listFiles((d, n) -> n.endsWith(".yml"));
        assertTrue(files != null && files.length >= 4, "at least four shipped fragments, found "
                + (files == null ? "no directory" : files.length));
        // The icon check is STUBBED here (no server); the boot log's "N fragments" count is what reads the
        // shipped icons against the real registry, and a refused icon names its file there.
        FragmentRegistry registry = new FragmentLoader(log, name -> true).loadAll(shipped);
        assertEquals(files.length, registry.size(), "every shipped fragment loads: " + warningText());
        for (File f : files) {
            assertTrue(Files.readString(f.toPath()).contains("# PLACEHOLDER -- Ben designs"),
                    f.getName() + " must carry the placeholder marker, so a grep finds it");
        }
    }
}
