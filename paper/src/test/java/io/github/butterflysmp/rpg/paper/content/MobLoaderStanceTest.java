package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobStance;
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
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The dragon fight's step F6: a mob file's {@code glowing:} and {@code stationary:}, each true or false, absent meaning
 * false, anything else refused by name; and the shipped Dark Mage, read from the real resources tree.
 */
class MobLoaderStanceTest {

    @TempDir Path dir;
    private Logger log;
    private final List<LogRecord> warnings = new ArrayList<>();

    @BeforeEach
    void setUp() {
        log = Logger.getLogger("MobLoaderStanceTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) warnings.add(record);
            }
            @Override public void flush() {}
            @Override public void close() {}
        });
    }

    private MobRegistry load(File from) {
        return new MobLoader(log, Set.of("void")::contains, s -> true, m -> true).loadAll(from);
    }

    private MobRegistry write(String yaml) throws IOException {
        Files.writeString(dir.resolve("m.yml"), yaml, StandardCharsets.UTF_8);
        return load(new File(dir.toString()));
    }

    @Test
    void theShippedDarkMageGlowsIsStationaryAndHas1500() {
        MobDefinition mage = load(new File("src/main/resources/content/mobs")).find("dark_mage").orElseThrow();
        assertEquals("wither_skeleton", mage.baseEntity());
        assertEquals("Withered Dark Mage", mage.displayName());
        assertEquals(1500.0, mage.maxHealth(), 0.0);
        assertEquals("void", mage.element());
        assertEquals(new MobStance(true, true), mage.stance());
        assertTrue(warnings.isEmpty(), "no warning loading the shipped mobs: " + warnings.size());
    }

    @Test
    void aFileWithNeitherKeyStandsAsEveryOtherMob() throws IOException {
        MobDefinition m = write("base_entity: zombie\nmax_health: 20\n").find("m").orElseThrow();
        assertEquals(MobStance.NONE, m.stance());
    }

    @Test
    void eachKeyIsReadOnItsOwn() throws IOException {
        assertEquals(new MobStance(true, false),
                write("base_entity: zombie\nmax_health: 20\nglowing: true\n").find("m").orElseThrow().stance());
        assertEquals(new MobStance(false, true),
                write("base_entity: zombie\nmax_health: 20\nstationary: true\n").find("m").orElseThrow().stance());
    }

    @Test
    void aNonBooleanIsRefusedByName() throws IOException {
        MobRegistry r = write("base_entity: zombie\nmax_health: 20\nstationary: often\n");
        assertTrue(r.find("m").isEmpty(), "refused, never read as false");
        assertTrue(warnings.get(0).getMessage().contains("'stationary' must be true or false, got 'often'"),
                warnings.get(0).getMessage());
    }
}
