package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * X10 (the seat, 2026-11-25): a weapon whose summon cast names no loaded summon is REFUSED AT LOAD, the file named, and
 * not left to refuse at the first press. The shipped Bone Whistle is the subject, copied from main content.
 */
class WeaponSummonRefusalTest {

    @TempDir
    Path dir;

    private final List<String> warnings = new ArrayList<>();

    private WeaponLoader loader(Set<String> loadedSummons) {
        Logger log = Logger.getLogger("WeaponSummonRefusalTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { warnings.add(r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return new WeaponLoader(log, loadedSummons::contains);
    }

    private static final Path SHIPPED = Path.of("src", "main", "resources", "content", "weapons", "bone_whistle.yml");

    /** CONTROL: with its summon loaded, the shipped whistle loads clean, so the refusal below is not a parse failure. */
    @Test
    void theWhistleLoadsWhenItsWolfIsLoaded() throws IOException {
        Files.copy(SHIPPED, dir.resolve("bone_whistle.yml"));
        WeaponRegistry r = loader(Set.of("bone_whistle_wolf")).loadAll(dir.toFile());
        assertEquals(List.of(), warnings);
        assertTrue(r.find("bone_whistle").isPresent());
    }

    @Test
    void theWhistleIsRefusedWhenItsWolfIsNotLoaded() throws IOException {
        Files.copy(SHIPPED, dir.resolve("bone_whistle.yml"));
        WeaponRegistry r = loader(Set.of("some_other_summon")).loadAll(dir.toFile());
        assertTrue(r.find("bone_whistle").isEmpty(), "refused at load");
        assertEquals("Skipping malformed weapon 'bone_whistle.yml': weapon 'bone_whistle' names summon 'bone_whistle_wolf',"
                + " which no file in content/summons defines", warnings.get(0), "the file and the summon named");
    }
}
