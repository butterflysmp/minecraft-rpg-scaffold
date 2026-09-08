package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.combat.Scorch;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>THE ENFORCER FOR {@link Scorch#UNDECLARED_CAP}'S OWN INVARIANT. A RULE THAT LIVES ONLY IN A
 * COMMENT GETS BUILT PAST.</b>
 *
 * <p>The constant is 2.0 <i>because</i> that is the smallest fire damage any shipped content
 * declares, and the rule that fixes it is: <b>an undeclared cap must never exceed the smallest
 * declared one</b>, so an author who forgets to declare a damage number cannot get a STRONGER scorch
 * than an author who declares one. Author a fire ability at 1.5 tomorrow and the constant silently
 * violates its own documented invariant, in exactly the direction the rule exists to prevent -- and
 * nothing else in the build would notice.
 *
 * <p>It is a BUILD-time check rather than a boot-time one on purpose. {@code ContentValidator}'s own
 * javadoc is explicit -- <i>"Warns; never throws. A typo in the 400th weapon must not take the server
 * down"</i> -- so it names problems, it does not fail. This cannot be built past, which is stronger.
 * (A matching {@code ContentValidator} arm covers content that did not exist at build time: datapack
 * reloads and operator-authored files outside the jar.)
 *
 * <h2>IT MUST FAIL LOUDLY WHEN IT DISCOVERS NOTHING</h2>
 *
 * This DISCOVERS rather than asserts -- it walks a directory looking for files -- and CLAUDE.md
 * records that failure mode twice: {@code getResource("content/")} on a shaded jar returns a non-null
 * URL whose stream is empty, so a scan that finds zero files reads exactly like a scan that ran and
 * found nothing wrong. <b>So the counts below are asserted, not merely logged.</b> If the walk stops
 * finding scorch appliers, this test reddens rather than passing vacuously.
 */
class ScorchContentInvariantTest {

    /** Every application site of `scorch` in shipped content, as of this slice. Asserted, not logged. */
    private static final int KNOWN_SCORCH_APPLICATION_SITES = 9;

    /** `status_id: scorch` in both the block and the inline-map spellings content actually uses. */
    private static final Pattern SCORCH_APPLIED = Pattern.compile("status_id:\\s*scorch\\b");

    /** `amount: <n>` on a damage effect. The cap basis. */
    private static final Pattern DAMAGE_AMOUNT = Pattern.compile("amount:\\s*([0-9]+(?:\\.[0-9]+)?)");

    private static Path contentRoot() {
        try {
            var url = ScorchContentInvariantTest.class.getResource("/content");
            assertNotNull(url, "bundled content is missing from the test classpath entirely");
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Path> yamlUnder(Path root) {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(p -> p.toString().endsWith(".yml")).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void theBundledContentIsREACHABLEAndNonEmpty() {
        // THE POSITIVE CONTROL. Everything below is a search; a search that cannot find the haystack
        // must say so rather than reporting a clean needle count.
        List<Path> yaml = yamlUnder(contentRoot());
        assertFalse(yaml.isEmpty(), "the content walk found NO yaml at all -- the scan is blind, and "
                + "a blind scan reports zero problems exactly like a clean one");
        assertTrue(yaml.size() > 40,
                "expected the full shipped content tree, found only " + yaml.size() + " files");
        try (InputStream in = getClass().getResourceAsStream("/content/statuses/scorch.yml")) {
            assertNotNull(in, "scorch.yml itself is not on the classpath -- the scan is looking in the "
                    + "wrong place");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void everyDeclaredCapIsAtLeastUNDECLARED_CAP() {
        // THE INVARIANT, ENFORCED.
        //
        // For each file that applies scorch, every damage amount in it is a candidate cap (the
        // resolved cap is DamagePayload's FIRST damage-bearing effect, but checking ALL of them is
        // strictly stronger and needs no schema parsing). If any is below the constant, the rule
        // "forgetting must never beat declaring" is false and UNDECLARED_CAP must be lowered.
        List<String> violations = new ArrayList<>();
        int sites = 0;

        for (Path file : yamlUnder(contentRoot())) {
            String text = read(file);
            Matcher applies = SCORCH_APPLIED.matcher(text);
            int inThisFile = 0;
            while (applies.find()) inThisFile++;
            if (inThisFile == 0) continue;
            sites += inThisFile;

            Matcher amounts = DAMAGE_AMOUNT.matcher(text);
            while (amounts.find()) {
                double declared = Double.parseDouble(amounts.group(1));
                if (declared < Scorch.UNDECLARED_CAP) {
                    violations.add(file.getFileName() + " declares a damage of " + declared
                            + ", below UNDECLARED_CAP " + Scorch.UNDECLARED_CAP);
                }
            }
        }

        // The discovery guard, before the verdict: zero sites means the regex stopped matching the
        // schema, not that content is clean.
        assertEquals(KNOWN_SCORCH_APPLICATION_SITES, sites,
                "expected " + KNOWN_SCORCH_APPLICATION_SITES + " scorch application sites in content, "
                        + "found " + sites + ". If content genuinely changed, update the constant; if "
                        + "it did not, this scan has gone blind and its clean verdict is worthless");

        assertTrue(violations.isEmpty(),
                "UNDECLARED_CAP (" + Scorch.UNDECLARED_CAP + ") must never exceed the smallest declared"
                        + " cap, or forgetting to declare one is STRONGER than declaring it. Lower the"
                        + " constant or raise the content:\n  " + String.join("\n  ", violations));
        // Mutation: raise UNDECLARED_CAP to 3.0 -> solar_grenade's field tick of 2 violates -> reddens.
        // Mutation: break SCORCH_APPLIED's regex -> sites drops to 0 -> reddens on the count, NOT
        // silently passing with an empty violations list. That second mutation is the one that
        // matters, and it is why the count is asserted.
    }
}
