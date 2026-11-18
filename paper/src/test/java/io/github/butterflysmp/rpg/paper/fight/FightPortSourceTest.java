package io.github.butterflysmp.rpg.paper.fight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The three Bukkit calls the fixture's port makes sit behind a real server, so no unit test can run them. This pins
 * WHICH calls they are, from the source (the shape of {@code LiveTargetsTest}): the bolt is the EFFECT-ONLY one, the
 * damage skips defence and wears no element, and the particle is sent with force. Comments are stripped first, so a
 * javadoc naming the wrong call cannot satisfy or fail a needle.
 */
class FightPortSourceTest {

    private static final Path PORT = Path.of("src/main/java/io/github/butterflysmp/rpg/paper/fight/BukkitFightPort.java");

    private static String code() throws IOException {
        String src = Files.readString(PORT);
        src = src.replaceAll("(?s)/\\*.*?\\*/", "");   // block and javadoc comments
        return src.replaceAll("//[^\\n]*", "");          // line comments
    }

    private static int count(String code, String regex) {
        Matcher m = Pattern.compile(regex).matcher(code);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    @Test
    void theBoltIsEffectOnly() throws IOException {
        String code = code();
        assertEquals(1, count(code, "\\.strikeLightningEffect\\("), "one effect-only bolt");
        assertEquals(0, count(code, "\\.strikeLightning\\("), "never the damaging bolt");
        // CONTROL: the needle for the damaging call can see one when it is there.
        assertEquals(1, count("world.strikeLightning(loc);", "\\.strikeLightning\\("));
    }

    @Test
    void theDamageIsTheStoresEntryWithDefenceBypassedAndNoElement() throws IOException {
        String code = code();
        assertEquals(1, count(code,
                "\\.applyDamage\\(amount, player, CritState\\.NORMAL, DefenseRule\\.BYPASSED, null\\);"));
    }

    @Test
    void theParticleIsSentWithForce() throws IOException {
        String code = code();
        assertEquals(1, count(code, "\\.spawnParticle\\(Particle\\.DUST, [^;]*, dust, true\\);"));
        assertTrue(!code.contains(", dust, false)"), "never unforced");
    }
}
