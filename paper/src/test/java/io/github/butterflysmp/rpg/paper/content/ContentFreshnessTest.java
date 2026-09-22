package io.github.butterflysmp.rpg.paper.content;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * *** THE CONTENT A TEST READS CAME FROM THIS TREE, AND NOT FROM WHICHEVER BRANCH BUILT LAST. ***
 *
 * <h2>WHY THIS EXISTS: A TRUE READING ABOUT THE WRONG SUBJECT</h2>
 *
 * <p>Every content test in this package reads the TEST CLASSPATH -- {@code getResource("/content")},
 * which resolves to {@code target/classes/content} -- and not the source tree. <b>Maven's resource
 * copy adds and overwrites; it never DELETES.</b> So a branch switch leaves the previous branch's
 * content sitting in {@code target/}, and an incremental build does not remove it.
 *
 * <p><b>MEASURED 2026-09-22.</b> Switching from {@code feat/14-scatter-shot} to a branch off
 * {@code master} and running {@code ./mvnw test} failed with:
 *
 * <pre>
 * expected 5 ranger weapons, found [boltor, dragons_plume, hunters_bow, locust, quiver_stone,
 *                                   scatter_shot]
 * </pre>
 *
 * <p>{@code scatter_shot.yml} was <b>not in that branch's source tree at all.</b> It was in
 * {@code target/classes/content/weapons/}, left by the previous build. The test was correct, the
 * scan was not blind, and the reading was TRUE -- <b>of the wrong subject.</b>
 *
 * <h2>THIS IS R0'S LESSON ONE LEVEL DOWN, IN THE BUILD DIRECTORY INSTEAD OF THE DEPLOYED JAR</h2>
 *
 * <p>{@code GATE-*.md}'s R0 exists because <i>a wrong jar does not announce itself -- it produces
 * readings, in the right shape, at plausible values.</i> <b>A warm {@code target/} does exactly the
 * same thing to the build.</b> Same rule, same failure, one layer in: <b>a reading is bound to a
 * subject, and the binding is a separate fact from the reading.</b>
 *
 * <p>And the binding is invisible from the result. A green content test after a branch switch is
 * indistinguishable from a green content test on a clean tree, <b>and every content test taken that
 * way is suspect while nothing says so.</b>
 *
 * <h2>A ROW RATHER THAN A RULE, AND THAT IS THE CHOICE</h2>
 *
 * <p>The alternative remedy was a standing rule -- <i>run {@code clean} after any branch switch.</i>
 * <b>A rule people must remember is the kind that fires again</b>, and it fires exactly when
 * somebody is busy doing something else, which is when a branch switch happens. A row that checks it
 * cannot be forgotten, costs one walk, and names the remedy in its own failure message.
 *
 * <p><b>It also catches the OTHER direction, which no rule would have.</b> Source edited and not
 * recompiled is the same class of defect arriving from the opposite side: the test then reads
 * content that is merely OLD rather than foreign, and that is quieter still.
 */
class ContentFreshnessTest {

    /** Where the tests actually read from: {@code target/classes/content}. */
    private static Path onClasspath() {
        try {
            var url = ContentFreshnessTest.class.getResource("/content");
            assertNotNull(url, "bundled content is missing from the test classpath entirely");
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Where the content actually LIVES on this branch.
     *
     * <p>Surefire runs with the module directory as its working directory, which is what makes this
     * relative path the source of truth rather than a guess. The same assumption every
     * {@code *SignatureTest} in this module already rests on.
     */
    private static Path inThisTree() {
        return Path.of("src", "main", "resources", "content");
    }

    /** Every file under {@code root}, as paths RELATIVE to it, sorted so the two sets compare. */
    private static TreeSet<String> filesUnder(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            TreeSet<String> out = new TreeSet<>();
            walk.filter(Files::isRegularFile)
                    .forEach(p -> out.add(root.relativize(p).toString().replace('\\', '/')));
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * *** THE CLASSPATH CONTENT AND THE SOURCE CONTENT ARE THE SAME SET OF FILES. ***
     *
     * <p>Both directions, because they are different defects with different remedies:
     *
     * <ul>
     *   <li><b>On the classpath and NOT in the source</b> -- a stale file from another branch.
     *       {@code ./mvnw clean}.</li>
     *   <li><b>In the source and NOT on the classpath</b> -- a file added since the last build.
     *       Any build fixes it, and until one runs, every content test is reading a tree that is
     *       missing it.</li>
     * </ul>
     *
     * <p>Asserted as SETS rather than as counts: two equal counts over different files is exactly
     * the reading a count cannot distinguish, and a rename produces precisely that.
     */
    @Test
    void theContentOnTheClasspathIsTheContentInThisTree() {
        TreeSet<String> classpath = filesUnder(onClasspath());
        TreeSet<String> source = filesUnder(inThisTree());

        // THE CONTROL. Both walks must find a plausible tree; an empty one would make the set
        // comparison pass trivially in one direction and fail uselessly in the other.
        assertTrue(source.size() > 30,
                "the SOURCE walk found only " + source.size() + " files under " + inThisTree()
                        + " -- the path is wrong or the working directory is not the module root,"
                        + " and an equality assertion over an empty set proves nothing");
        assertTrue(classpath.size() > 30,
                "the CLASSPATH walk found only " + classpath.size() + " files -- the build did not"
                        + " copy the resources, and every content test in this package is reading"
                        + " almost nothing");

        TreeSet<String> staleOnClasspath = new TreeSet<>(classpath);
        staleOnClasspath.removeAll(source);

        TreeSet<String> missingFromBuild = new TreeSet<>(source);
        missingFromBuild.removeAll(classpath);

        assertEquals(new TreeSet<String>(), staleOnClasspath,
                "*** STALE CONTENT ON THE TEST CLASSPATH. *** These files are in target/classes and"
                        + " NOT in this branch's source: " + staleOnClasspath + ". Maven's resource"
                        + " copy never deletes, so a branch switch leaves the previous branch's"
                        + " content behind and every content test in this package reads it as though"
                        + " it were yours. RUN ./mvnw clean. Measured 2026-09-22: this is how"
                        + " ExpandedQuiverContentInvariantTest came to count a weapon that did not"
                        + " exist on the branch it was running on.");

        assertEquals(new TreeSet<String>(), missingFromBuild,
                "*** CONTENT IN THE SOURCE THAT THE BUILD HAS NOT COPIED. *** These files exist on"
                        + " this branch and are NOT on the test classpath: " + missingFromBuild
                        + ". Every content test is running against a tree that is missing them, so"
                        + " a new weapon or element reads as absent rather than as broken. Re-run"
                        + " the build.");
    }

    /**
     * AND THE FILES ARE BYTE-IDENTICAL, NOT MERELY PRESENT.
     *
     * <p>The set check above catches a file ADDED, REMOVED or RENAMED. It cannot see a file that was
     * EDITED and not recopied -- same name, old bytes -- which is the quietest member of the family
     * and the one an incremental build produces most often.
     *
     * <p><b>A byte comparison is sound here because nothing filters this content, and that was read
     * rather than assumed.</b> {@code paper/pom.xml} declares two resource blocks: {@code filtering}
     * is TRUE for {@code paper-plugin.yml} alone and FALSE for everything else, which is the block
     * {@code content/} falls in. <b>If a future pom ever filters {@code content/}, this row goes red
     * on a correct tree</b> -- and the fix is then to narrow it to the set check, not to delete it.
     */
    @Test
    void everyContentFileOnTheClasspathIsByteIdenticalToItsSource() {
        Path classpathRoot = onClasspath();
        Path sourceRoot = inThisTree();

        List<String> differing = new ArrayList<>();
        int compared = 0;

        for (String relative : filesUnder(sourceRoot)) {
            Path source = sourceRoot.resolve(relative);
            Path built = classpathRoot.resolve(relative);
            if (!Files.exists(built)) continue;   // the set row above owns that failure

            compared++;
            try {
                if (Files.mismatch(source, built) != -1) differing.add(relative);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        assertTrue(compared > 30,
                "only " + compared + " files were compared -- the walk is not reaching the content,"
                        + " and a clean verdict over nothing is worthless");

        assertEquals(List.of(), differing,
                "*** THE BUILD'S COPY OF THESE FILES IS NOT WHAT THE SOURCE SAYS: " + differing
                        + " *** An EDIT that was not recopied is the quietest form of a stale"
                        + " target/: the file is present, the name is right, and the bytes are"
                        + " somebody else's. Re-run the build; if it persists, check whether"
                        + " paper/pom.xml has started FILTERING content/, in which case this row"
                        + " should be narrowed rather than deleted.");
    }
}
