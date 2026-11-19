package io.github.butterflysmp.rpg.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE FIGHT'S RETURN LOCATION LIVES BEHIND {@code PlayerRepository} (the F8 brief, section 4 c; PLAN-dragon-mobs.md C5:
 * "PLAYER SAVING STAYS BEHIND PlayerRepository"). Saved on entering a fight world, cleared on the way out, so a crash or a
 * quit inside leaves a record the next login reads.
 */
class FightReturnProfileTest {

    @TempDir
    Path dir;

    private static final Executor DIRECT = Runnable::run;
    private static final FightReturn HOME = new FightReturn("world", 12.5, 64.0, -30.25, 90.0f, 10.0f);

    private static PlayerProfile profile() {
        return PlayerProfile.fresh(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
    }

    @Test
    void aFreshProfileHasNoReturnAndSettingOneIsReadBackAndClearing() {
        assertEquals(Optional.empty(), profile().fightReturn());
        PlayerProfile set = profile().withFightReturn(HOME);
        assertEquals(Optional.of(HOME), set.fightReturn());
        assertEquals(Optional.empty(), set.withoutFightReturn().fightReturn());
    }

    /**
     * EVERY {@code with...} KEEPS THE RETURN. Each one builds a new record from every field, so a method that forgets the
     * new field drops a player's way home silently. Driven by reflection over every public {@code with...} returning a
     * profile, so a method added later is covered without anyone remembering this test.
     */
    @Test
    void everyWithMethodKeepsTheReturn() throws Exception {
        PlayerProfile set = profile().withFightReturn(HOME);
        List<String> checked = new ArrayList<>();
        for (Method m : PlayerProfile.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers()) || Modifier.isStatic(m.getModifiers())) continue;
            if (!m.getName().startsWith("with") || m.getReturnType() != PlayerProfile.class) continue;
            if (m.getName().equals("withoutFightReturn") || m.getName().equals("withFightReturn")) continue;
            Object[] args = new Object[m.getParameterCount()];
            Class<?>[] types = m.getParameterTypes();
            for (int i = 0; i < args.length; i++) {
                Class<?> t = types[i];
                args[i] = t == int.class ? 3 : t == long.class ? 7L : t == boolean.class ? Boolean.TRUE
                        : t == String.class ? "x" : null;
            }
            PlayerProfile after = (PlayerProfile) m.invoke(set, args);
            assertEquals(Optional.of(HOME), after.fightReturn(), m.getName() + " dropped the fight return");
            checked.add(m.getName());
        }
        assertTrue(checked.size() >= 9, "the scan reached the with-methods: " + checked);
    }

    @Test
    void theReturnRoundTripsThroughTheFileRepository() {
        FilePlayerRepository repo = new FilePlayerRepository(dir, DIRECT);
        PlayerProfile set = profile().withFightReturn(HOME);
        repo.save(set).join();
        assertEquals(Optional.of(HOME), repo.load(set.playerId()).join().orElseThrow().fightReturn());
        repo.save(set.withoutFightReturn()).join();
        assertEquals(Optional.empty(), repo.load(set.playerId()).join().orElseThrow().fightReturn());
    }

    /** A file written before the field existed has no key, and reads as no return pending: no migration needed. */
    @Test
    void aFileWithoutTheKeyReadsAsNoReturn() throws Exception {
        FilePlayerRepository repo = new FilePlayerRepository(dir, DIRECT);
        PlayerProfile plain = profile();
        repo.save(plain).join();
        String json = Files.readString(dir.resolve(plain.playerId() + ".json"));
        assertFalse(json.contains("fightReturn"), "an empty return writes no key: " + json);
        assertEquals(Optional.empty(), repo.load(plain.playerId()).join().orElseThrow().fightReturn());
    }

    @Test
    void aReturnRefusesNonsense() {
        assertThrows(IllegalArgumentException.class, () -> new FightReturn(" ", 0, 64, 0, 0, 0), "no world");
        assertThrows(IllegalArgumentException.class, () -> new FightReturn(null, 0, 64, 0, 0, 0), "no world");
        assertThrows(IllegalArgumentException.class, () -> new FightReturn("world", Double.NaN, 64, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new FightReturn("world", 0, Double.POSITIVE_INFINITY, 0, 0, 0));
    }
}
