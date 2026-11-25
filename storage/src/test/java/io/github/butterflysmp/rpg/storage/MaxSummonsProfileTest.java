package io.github.butterflysmp.rpg.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE STORED BASE OF max_summons (the seat's PR 1, 2026-11-24): persisted as the other profile fields are, behind
 * {@code PlayerRepository}. Absent reads as Ben's default of 1, so a profile written before this change needs no migration.
 */
class MaxSummonsProfileTest {

    @TempDir
    Path dir;

    private static final Executor DIRECT = Runnable::run;

    private static PlayerProfile profile() {
        return PlayerProfile.fresh(UUID.fromString("00000000-0000-0000-0000-0000000000ab"));
    }

    @Test
    void aFreshProfileReadsOneAndAnOverrideIsReadBackAndClears() {
        assertEquals(1, profile().maxSummonsBase());
        assertEquals(0, profile().withMaxSummonsBase(0).maxSummonsBase(), "the cap row's override");
        assertEquals(4, profile().withMaxSummonsBase(4).maxSummonsBase());
        assertEquals(1, profile().withMaxSummonsBase(4).withMaxSummonsBase(null).maxSummonsBase(), "cleared: the default");
        assertThrows(IllegalArgumentException.class, () -> profile().withMaxSummonsBase(-1));
    }

    /** EVERY {@code with...} KEEPS THE BASE: {@code FightReturnProfileTest}'s reflection scan, for this field. */
    @Test
    void everyWithMethodKeepsTheBase() throws Exception {
        PlayerProfile set = profile().withMaxSummonsBase(5);
        List<String> checked = new ArrayList<>();
        for (Method m : PlayerProfile.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers()) || Modifier.isStatic(m.getModifiers())) continue;
            if (!m.getName().startsWith("with") || m.getReturnType() != PlayerProfile.class) continue;
            if (m.getName().equals("withMaxSummonsBase")) continue;
            Object[] args = new Object[m.getParameterCount()];
            Class<?>[] types = m.getParameterTypes();
            for (int i = 0; i < args.length; i++) {
                Class<?> t = types[i];
                args[i] = t == int.class ? 3 : t == long.class ? 7L : t == boolean.class ? Boolean.TRUE
                        : t == String.class ? "x"
                        : t == FightReturn.class ? new FightReturn("world", 0, 64, 0, 0f, 0f) : null;
            }
            PlayerProfile after = (PlayerProfile) m.invoke(set, args);
            assertEquals(5, after.maxSummonsBase(), m.getName() + " dropped the max_summons base");
            checked.add(m.getName());
        }
        assertTrue(checked.size() >= 10, "the scan reached the with-methods: " + checked);
    }

    @Test
    void theBaseRoundTripsThroughTheFileRepository() {
        FilePlayerRepository repo = new FilePlayerRepository(dir, DIRECT);
        PlayerProfile set = profile().withMaxSummonsBase(0);
        repo.save(set).join();
        assertEquals(0, repo.load(set.playerId()).join().orElseThrow().maxSummonsBase(), "0 survives, it is not absence");
        repo.save(set.withMaxSummonsBase(3)).join();
        assertEquals(3, repo.load(set.playerId()).join().orElseThrow().maxSummonsBase());
    }

    /** A profile written before the stat has no key, and reads 1: no migration, no schema bump. */
    @Test
    void aFileWithoutTheKeyReadsOne() throws Exception {
        FilePlayerRepository repo = new FilePlayerRepository(dir, DIRECT);
        PlayerProfile plain = profile();
        repo.save(plain).join();
        String json = Files.readString(dir.resolve(plain.playerId() + ".json"));
        assertFalse(json.contains("maxSummons"), "an untouched base writes no key: " + json);
        assertEquals(1, repo.load(plain.playerId()).join().orElseThrow().maxSummonsBase());
        assertEquals(PlayerProfile.CURRENT_SCHEMA_VERSION, repo.load(plain.playerId()).join().orElseThrow().schemaVersion());
    }
}
