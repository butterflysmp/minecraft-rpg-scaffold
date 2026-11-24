package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.CandidateCascade;
import io.github.butterflysmp.rpg.core.enchant.CandidateCascade.Tier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code content/enchant_candidates.yml}: Ben's cascade, authored in content (the seat's ruling V3, 2026-11-23).
 * The shipped file IS his numbers; anything else is refused whole, by name, leaving one candidate a slot.
 */
class EnchantCandidatesLoaderTest {

    private static final File SHIPPED = Path.of("src", "main", "resources", "content", "enchant_candidates.yml").toFile();

    private static Logger capturing(List<String> into) {
        Logger log = Logger.getLogger("EnchantCandidatesLoaderTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { into.add(r.getLevel() + " " + r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    /** Ben's "1 enchant = 100%; 2 enchants = 70%; 3 enchants = 40%; 4 enchants = 10%", rarest first. */
    @Test
    void theShippedFileIsBensCascade() {
        List<String> log = new ArrayList<>();
        CandidateCascade c = new EnchantCandidatesLoader(capturing(log)).load(SHIPPED);
        assertEquals(List.of(new Tier(4, 0.10), new Tier(3, 0.40), new Tier(2, 0.70), new Tier(1, 1.0)), c.tiers());
        assertEquals(1, log.size(), log.toString());
    }

    /** The Loaded line, word for word: the gate's R0c predicts exactly this. */
    @Test
    void theLoadedLinePrintsEachWrittenChanceAndItsTrueOdds() {
        List<String> log = new ArrayList<>();
        new EnchantCandidatesLoader(capturing(log)).load(SHIPPED);
        assertEquals(List.of("INFO Loaded enchant candidates: 4 at 10.00% (truly 10.000%), 3 at 40.00% (truly 36.000%),"
                + " 2 at 70.00% (truly 37.800%), 1 at 100.00% (truly 16.200%)"), log);
    }

    @Test
    void aFileNotRarestFirstIsRefusedWholeByNameAndLeavesOne(@TempDir Path dir) throws IOException {
        assertRefused(dir, "tiers:\n  - candidates: 3\n    chance: 0.4\n  - candidates: 4\n    chance: 0.1\n"
                + "  - candidates: 1\n    chance: 1.0\n", "rarest first");
    }

    @Test
    void aChanceOutsideZeroToOneIsRefused(@TempDir Path dir) throws IOException {
        assertRefused(dir, "tiers:\n  - candidates: 2\n    chance: 1.7\n  - candidates: 1\n    chance: 1.0\n", "[0, 1]");
    }

    @Test
    void aLastTierThatIsNotGuaranteedIsRefused(@TempDir Path dir) throws IOException {
        assertRefused(dir, "tiers:\n  - candidates: 2\n    chance: 0.7\n  - candidates: 1\n    chance: 0.5\n", "guaranteed");
    }

    @Test
    void aMissingTiersKeyOrAMissingNumberIsRefused(@TempDir Path dir) throws IOException {
        assertRefused(dir, "nothing: here\n", "needs 'tiers:'");
        assertRefused(dir, "tiers:\n  - candidates: 2\n  - candidates: 1\n    chance: 1.0\n", "needs a number");
    }

    @Test
    void aMissingFileIsRefusedAndLeavesOne(@TempDir Path dir) {
        List<String> log = new ArrayList<>();
        assertSame(CandidateCascade.ONE, new EnchantCandidatesLoader(capturing(log)).load(dir.resolve("absent.yml").toFile()));
        assertTrue(log.size() == 1 && log.get(0).startsWith("WARNING Refusing enchant candidates in absent.yml: the file is missing"),
                log.toString());
    }

    private static void assertRefused(Path dir, String yaml, String reason) throws IOException {
        Path f = dir.resolve("enchant_candidates.yml");
        Files.writeString(f, yaml);
        List<String> log = new ArrayList<>();
        assertSame(CandidateCascade.ONE, new EnchantCandidatesLoader(capturing(log)).load(f.toFile()), yaml);
        assertEquals(1, log.size(), log.toString());
        assertTrue(log.get(0).startsWith("WARNING Refusing enchant candidates in enchant_candidates.yml: "), log.get(0));
        assertTrue(log.get(0).contains(reason), log.get(0));
        assertTrue(log.get(0).endsWith("; every enchant slot offers ONE candidate until it is fixed"), log.get(0));
    }
}
