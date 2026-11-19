package io.github.butterflysmp.rpg.paper.fight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ONE RUNTIME, TWO SITES (the F8 brief, section 3; GATE-fight-site.md). The fight's logic must exist ONCE, take a
 * {@link FightSite}, keep all its state in the instance, and two fights at once must be possible in the code. None of
 * that runs without a server, so it is pinned from the source, comments stripped first (the shape of
 * {@code FightWiringSourceTest}):
 * <ul>
 *   <li>exactly one place in {@code paper/} builds the core fight ({@code new RingFixture(}) and its port, and it is
 *       {@code FightInstance}, whose constructor takes a {@code FightSite};</li>
 *   <li>the port reads who takes part from the SITE, never from its own radius rule;</li>
 *   <li>{@code FightFixtures} holds its fights by identity, and a death reaches EVERY running fight, not only the dev
 *       command's;</li>
 *   <li>no static mutable state anywhere in the fight package (CLAUDE.md invariant 3).</li>
 * </ul>
 */
class FightSiteSourceTest {

    private static final Path PAPER = Path.of("src/main/java/io/github/butterflysmp/rpg/paper");
    private static final Path FIGHT = PAPER.resolve("fight");

    private static String code(Path file) throws IOException {
        String src = Files.readString(file);
        src = src.replaceAll("(?s)/\\*.*?\\*/", "");
        return src.replaceAll("//[^\\n]*", "");
    }

    private static List<Path> javaFiles(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    /** From {@code signature} to the next member declaration at four spaces' indent. */
    private static String body(String code, String signature) {
        int from = code.indexOf(signature);
        assertTrue(from >= 0, "found: " + signature);
        int to = code.length();
        for (String next : new String[] {"@Override", "\n    public ", "\n    private ", "\n    void ", "\n    String "}) {
            int i = code.indexOf(next, from + signature.length());
            if (i >= 0 && i < to) to = i;
        }
        return code.substring(from, to);
    }

    @Test
    void theFightIsBuiltInExactlyOnePlaceAndThatPlaceTakesASite() throws IOException {
        List<String> builders = new ArrayList<>();
        List<String> ports = new ArrayList<>();
        for (Path p : javaFiles(PAPER)) {
            String c = code(p);
            if (c.contains("new RingFixture(")) builders.add(PAPER.relativize(p).toString().replace('\\', '/'));
            if (c.contains("new BukkitFightPort(")) ports.add(PAPER.relativize(p).toString().replace('\\', '/'));
        }
        assertEquals(List.of("fight/FightInstance.java"), builders, "the core fight is built once");
        assertEquals(List.of("fight/FightInstance.java"), ports, "and its port once, beside it");
        String instance = code(FIGHT.resolve("FightInstance.java"));
        assertTrue(Pattern.compile("FightInstance\\([^)]*FightSite site").matcher(instance).find(),
                "the instance's constructor takes the site");
    }

    @Test
    void whoTakesPartIsTheSitesRule() throws IOException {
        String port = body(code(FIGHT.resolve("BukkitFightPort.java")), "public List<RingFixture.Participant> participants()");
        assertTrue(port.contains("site.takesPart("), port);
        assertTrue(!port.contains("FightParticipants.takesPart("), "the port has no radius rule of its own: " + port);
    }

    @Test
    void theFixturesHoldFightsByIdentityAndADeathReachesEveryOne() throws IOException {
        String fixtures = code(FIGHT.resolve("FightFixtures.java"));
        assertTrue(fixtures.contains("Map<UUID, FightInstance>"), "fights are held by identity");
        String onDeath = body(fixtures, "public void onDeath(EntityDeathEvent event)");
        // The loop's body is the call and nothing else: a guard such as "if (f == current)" inside it would keep the
        // loop and offer the death to one fight only.
        assertTrue(onDeath.matches("(?s).*for \\(FightInstance (\\w+) : [^\\n]*fights\\.values\\(\\)\\)\\) \\{\\s*"
                        + "\\1\\.onDeath\\(event\\);\\s*}.*"),
                "a death is offered to every fight held, unconditionally: " + onDeath);
        String onDisable = body(fixtures, "public void onDisable()");
        assertTrue(onDisable.matches("(?s).*for \\(FightInstance \\w+ : .*\\).*"), "and disable ends every one: " + onDisable);
    }

    @Test
    void noStaticMutableStateInTheFightPackage() throws IOException {
        Pattern staticField = Pattern.compile("(?m)^\\s+(?:private |public |protected )?static (?!final )[\\w<>, ?]+ \\w+\\s*[=;]");
        List<String> found = new ArrayList<>();
        for (Path p : javaFiles(FIGHT)) {
            Matcher m = staticField.matcher(code(p));
            while (m.find()) found.add(p.getFileName() + ": " + m.group().trim());
        }
        assertEquals(List.of(), found, "a static mutable field holds fight state outside any instance");
        // The control: the pattern finds a static mutable field when one is there.
        assertTrue(staticField.matcher("    private static int fights = 0;").find(), "the pattern is not blind");
        assertTrue(!staticField.matcher("    static final int STAND_BAND = 8;").find(), "and passes a constant");
    }
}
