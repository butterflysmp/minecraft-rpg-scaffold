package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The seat's ruling d: <b>the instrument never prints PASS or FAIL.</b> Readings are judged by a person from the real
 * witness lines. Every string the selftest package can print is a constant in one of its class files, so no constant
 * containing either word means no line can.
 */
class SelfTestNeverJudgesTest {

    /** A test-only class that DOES judge: the control. */
    static final class Judge {
        static String verdict(boolean ok) { return ok ? "PASS" : "FAIL"; }
    }

    @Test
    void noSelftestClassCarriesAVerdict() throws IOException, URISyntaxException {
        Path dir = Path.of(SelfTestRun.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve(SelfTestRun.class.getPackageName().replace('.', '/'));
        List<Path> classes;
        try (Stream<Path> s = Files.list(dir)) {
            classes = s.filter(p -> p.toString().endsWith(".class")).toList();
        }
        assertTrue(classes.size() >= 10, "found only " + classes + " -- the scan would be blind");
        List<String> hits = new ArrayList<>();
        for (Path c : classes) {
            for (String k : ClassConstants.parse(Files.readAllBytes(c))) {
                if (k.contains("PASS") || k.contains("FAIL")) hits.add(c.getFileName() + ": " + k);
            }
        }
        assertEquals(List.of(), hits);
    }

    @Test
    void controlTheScanSeesAVerdictWhereThereIsOne() throws IOException {
        List<String> constants = ClassConstants.of(Judge.class);
        assertTrue(constants.contains("PASS") && constants.contains("FAIL"), constants.toString());
    }
}
