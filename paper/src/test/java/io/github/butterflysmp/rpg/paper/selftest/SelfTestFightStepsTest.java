package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE INSTRUMENT FIXES CARRIED FROM GATE-fight-mage.md's READINGS (the F8 brief, section 2; GATE-fight-site.md):
 * <ul>
 *   <li>AWAIT_FIGHT: wait until the fight ends, with a ceiling;</li>
 *   <li>PROBE_ENTITIES no longer counts a body in its death ticks (FR7R2 and MG4 counted one), and says how many it
 *       left out;</li>
 *   <li>ATTACK names the attacker's attack damage, and says so when it is 0: then {@code Player.attack} raises no
 *       damage event (READ; MG2's driven attack logged nothing).</li>
 * </ul>
 */
class SelfTestFightStepsTest {

    private static final Path DRIVERS =
            Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestDrivers.java");

    // --- AWAIT_FIGHT: "<ceiling ticks>" ---

    @Test
    void anAwaitFightTakesOneWholePositiveCeiling() {
        assertEquals(1800, SelfTestAwaitFight.parse("1800").orElseThrow());
        assertEquals(1, SelfTestAwaitFight.parse(" 1 ").orElseThrow());
        for (String bad : List.of("", "0", "-5", "ten", "1800 600", "1.5")) {
            assertTrue(SelfTestAwaitFight.parse(bad).isEmpty(), "refused: '" + bad + "'");
        }
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.AWAIT_FIGHT, "soon").isPresent(), "refused at LOAD");
        assertTrue(SelfTestStaging.refusal(Scenario.Verb.AWAIT_FIGHT, "1800").isEmpty());
    }

    @Test
    void theWaitEndsWhenTheFightEndsOrAtTheCeilingAndSaysWhich() {
        assertFalse(SelfTestAwaitFight.done(true, 10, 1800), "still running, under the ceiling: wait on");
        assertTrue(SelfTestAwaitFight.done(false, 10, 1800), "the fight ended");
        assertTrue(SelfTestAwaitFight.done(true, 1800, 1800), "the ceiling");
        assertEquals("AWAIT fight ended ticks=412", SelfTestAwaitFight.line(false, 412, 1800));
        assertEquals("AWAIT fight ceiling ticks=1800 still running", SelfTestAwaitFight.line(true, 1800, 1800));
    }

    /**
     * A DEATH RETIRES THE PLAYER'S OWN TASKS (SelfTestRun's awaitRespawn note: a retired task never runs). In the arena he
     * may die while the run waits for the fight to end, so the wait polls on the GLOBAL scheduler, and a player found
     * dead when it ends goes to the respawn wait, never to his own scheduler.
     */
    @Test
    void theFightWaitSurvivesADeathDuringIt() throws IOException {
        String run = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestRun.java"))
                .replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
        int from = run.indexOf("private void awaitFight(int ceiling, int waited)");
        assertTrue(from >= 0);
        String body = run.substring(from, run.indexOf("\n    private ", from + 10));
        assertTrue(body.contains("onGlobalLater("), "polls on the global scheduler: " + body);
        assertTrue(!body.contains("onEntityLater("), "never on his own: " + body);
        assertTrue(body.contains("player.isDead()") && body.contains("awaitRespawn("), "a dead player waits to respawn: "
                + body);
    }

    private static String driver(String signature) throws IOException {
        String src = Files.readString(DRIVERS).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
        int from = src.indexOf(signature);
        assertTrue(from >= 0, "found: " + signature);
        int to = src.indexOf("\n    private ", from + signature.length());
        return src.substring(from, to < 0 ? src.length() : to);
    }

    @Test
    void theEntityProbeLeavesOutADyingBodyAndCountsWhatItLeftOut() throws IOException {
        String body = driver("private Optional<String> probeEntities(Player p, String arg)");
        assertTrue(body.contains("SelfTestStaging.dying("), "the dying test is the settle step's own: " + body);
        assertTrue(body.contains("\" dying=\""), "and the line says how many it left out: " + body);
    }

    @Test
    void theAttackLineNamesTheAttackDamageAndAZeroIsSaidOutLoud() throws IOException {
        String body = driver("private Optional<String> attack(Player p, String tag)");
        assertTrue(body.contains("Attribute.ATTACK_DAMAGE"), body);
        assertTrue(body.contains("\" attack_damage=\""), body);
        assertTrue(body.contains("NO_DAMAGE_EVENT"), "a zero is named, not left to silence: " + body);
        assertTrue(body.contains("if (!(attackDamage > 0.0)) {"), "and named exactly when it is not above 0: " + body);
        assertTrue(body.indexOf("NO_DAMAGE_EVENT") < body.indexOf("p.attack(e);"), "before the attack: " + body);
    }
}
