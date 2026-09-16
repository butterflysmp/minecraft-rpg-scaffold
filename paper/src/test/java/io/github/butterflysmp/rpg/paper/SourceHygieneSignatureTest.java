package io.github.butterflysmp.rpg.paper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repo-wide source hygiene that the COMPILER WILL NOT CATCH, measured across every module.
 *
 * <h2>WHY A TEST AND NOT A COMPILER FLAG -- MEASURED, NOT ASSUMED</h2>
 *
 * <b>A duplicate import is legal Java and {@code javac} is silent about it.</b> Checked against the
 * JDK this project pins: {@code javac 26.0.1 --help-lint} lists 40 categories and <b>none of them
 * covers duplicate or unused imports</b> -- not {@code all}, which enables every listed category and
 * still says nothing. So there is no one-line {@code pom.xml} setting to reach for, and the choice
 * is a third-party analyzer or this.
 *
 * <p><b>This repo already has the mechanism.</b> {@code QuiversSignatureTest} scans both modules'
 * sources and compares what it finds to a named list, and that pattern has caught things nothing
 * else could -- twice. A scanning test costs no new build dependency, runs in the 2-second loop, and
 * fails by NAMING THE FILE. A Checkstyle plugin would be a plugin block plus a config file plus a
 * suppressions file, to catch one defect class this file catches in forty lines.
 *
 * <h2>WHY IT IS WORTH GUARDING AT ALL, GIVEN IT IS HARMLESS</h2>
 *
 * A duplicate import changes no behaviour -- that is exactly the problem. <b>It is invisible to the
 * compiler, invisible to every test, and invisible in review</b>, so it accumulates silently and is
 * only ever found by somebody sweeping for it by hand. It was found by hand twice in one week:
 * {@code StatsSheet} (fixed by #103) and {@code RpgCommand} (fixed by the commit that adds this
 * file). <b>The second was found only because the operator re-swept a file whose FIRST duplicate had
 * already been reported</b> -- one instance was found, the enumeration was not done.
 *
 * <p>This test is the enumeration, run on every build.
 *
 * <h2>SCOPE, STATED SO IT IS NOT MISTAKEN FOR MORE</h2>
 *
 * <b>DUPLICATES ONLY.</b> It does not detect an UNUSED import, which needs the symbol table rather
 * than the text and is the job of an analyzer. It covers {@code import} and {@code import static}
 * alike, because the needle is the whole line.
 */
class SourceHygieneSignatureTest {

    /**
     * Every source root in the project, main and test, relative to this module's working directory.
     *
     * <p><b>TEST SOURCES ARE INCLUDED, unlike {@code QuiversSignatureTest}'s scan.</b> That one
     * excludes them deliberately, because reading an accessor in a test is neither a supply nor a
     * readout. A duplicate import is a defect wherever it is.
     */
    private static final List<Path> SOURCE_ROOTS = List.of(
            Path.of("src", "main", "java"),
            Path.of("src", "test", "java"),
            Path.of("..", "core", "src", "main", "java"),
            Path.of("..", "core", "src", "test", "java"),
            Path.of("..", "storage", "src", "main", "java"),
            Path.of("..", "storage", "src", "test", "java"));

    /**
     * The floor the scan must clear, so FINDING NOTHING cannot be mistaken for PASSING.
     *
     * <p>A discovery that discovers nothing is a defect, not a quiet no-op -- if a source root is
     * renamed, or the working directory moves, every root walks zero files and this test goes green
     * having read nothing at all. Measured at the commit that added this file: <b>471 files</b>. The
     * floor is set well below that so ordinary deletions do not trip it, and well above zero.
     */
    private static final int MINIMUM_FILES_SCANNED = 400;

    @Test
    void noSourceFileImportsTheSameThingTwice() throws IOException {
        List<String> offenders = new ArrayList<>();
        int scanned = 0;

        for (Path root : SOURCE_ROOTS) {
            assertTrue(Files.isDirectory(root),
                    "source root not found: " + root.toAbsolutePath()
                            + " -- a moved or renamed root makes this scan read NOTHING and pass");
            try (var walk = Files.walk(root)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                    scanned++;
                    Set<String> seen = new HashSet<>();
                    for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                        String trimmed = line.strip();
                        // The whole line is the needle, so "import static" is covered by the same
                        // test as "import" -- and a duplicate differing only in whitespace is still
                        // caught, because the line is stripped before comparison.
                        if (!trimmed.startsWith("import ")) continue;
                        if (!seen.add(trimmed)) {
                            offenders.add(file.getFileName() + ": " + trimmed);
                        }
                    }
                }
            }
        }

        assertTrue(scanned >= MINIMUM_FILES_SCANNED,
                "only " + scanned + " files scanned across all three modules -- expected at least "
                        + MINIMUM_FILES_SCANNED + ". A scan that finds nothing looks exactly like a "
                        + "scan that found nothing wrong");

        assertEquals(List.of(), offenders,
                "these files import the same thing twice. javac does not warn about this -- there "
                        + "is no -Xlint category for it -- so nothing else in the build will ever "
                        + "tell you. Delete the second occurrence");
        // Mutation MUTDUPGUARD: restore either duplicate import in RpgCommand.java -> reddens here,
        // naming the file and the line. APPLIED AND MEASURED, in both directions -- this test was
        // written BEFORE the two duplicates were deleted and observed failing on them.
        //
        // Mutation MUTDUPBLIND: point a SOURCE_ROOT at a directory that does not exist -> reddens
        // on the isDirectory assertion, not on a green empty scan. APPLIED AND MEASURED.
    }
}
