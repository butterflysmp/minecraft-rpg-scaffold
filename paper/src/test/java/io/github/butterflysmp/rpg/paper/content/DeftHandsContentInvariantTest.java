package io.github.butterflysmp.rpg.paper.content;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DEFT HANDS' ELIGIBILITY GATE, and it is a CONTENT INVARIANT, not a roll-pool filter (PLAN-deft-hands.md section
 * 3): {@code deft_hands.yml} is {@code class: ranger}, and {@code EnchantRoll.poolFor} filters on class alone -- the
 * roster carries no weapon -- so the enchant enters EVERY ranger weapon's pool. On one with no reload it would render
 * "-30% Reload Time" and grant ZERO. This test makes that state unbuildable for every ranger weapon but the one named
 * exemption. {@link ExpandedQuiverContentInvariantTest}'s sibling, and deliberately NOT folded into it.
 *
 * <p><b>It asserts {@code reload_ticks} DIRECTLY</b>, although {@code WeaponDefinition} already enforces magazine
 * &lt;=&gt; reload in both directions ({@code WeaponQuiverDefinitionTest}). Leaning on that would make this gate
 * silently open the day the authoring rule is relaxed; asserting the quantity the enchant actually reads cannot.
 *
 * <p><b>{@code hunters_bow} is exempt and CAN roll Deft Hands uselessly: a FINDING, not a special case</b> (the
 * seat's ruling, 2026-11-06). It is a dev weapon in the deletion set, as for Expanded Quiver.
 */
class DeftHandsContentInvariantTest {

    /** The ranger weapons at the stack top: boltor, dragons_breath, dragons_plume, hunters_bow, locust, quiver_stone, short_bow, withered_shortbow. */
    private static final int KNOWN_RANGER_WEAPONS = 8;

    // DELETION-SET-CITATION: hunters_bow
    private static final List<String> EXEMPT_DEV_WEAPONS = List.of("hunters_bow");

    /** {@code class: ranger} at column 0 -- the weapon's OWN class, never a nested one. */
    private static final Pattern RANGER_CLASS = Pattern.compile("(?m)^class:\\s*ranger\\s*$");

    /** {@code reload_ticks: <n>} at column 0, with {@code n >= 1} (a 0 authors no reload). */
    private static final Pattern RELOAD_TICKS = Pattern.compile("(?m)^reload_ticks:\\s*[1-9]\\d*\\s*$");

    private static Path contentRoot() {
        try {
            var url = DeftHandsContentInvariantTest.class.getResource("/content");
            assertNotNull(url, "bundled content is missing from the test classpath entirely");
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Path> weaponYaml() {
        try (Stream<Path> files = Files.walk(contentRoot().resolve("weapons"))) {
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

    private static String idOf(Path p) {
        String name = p.getFileName().toString();
        return name.substring(0, name.length() - ".yml".length());
    }

    /** THE POSITIVE CONTROL: the walk finds the weapons, and the reload pattern finds the reloads it should. */
    @Test
    void theWeaponContentIsREACHABLEAndTheReloadPatternMatches() {
        List<Path> weapons = weaponYaml();
        assertFalse(weapons.isEmpty(), "the weapon walk found NO yaml at all -- the scan is blind");
        List<String> ranger = new ArrayList<>();
        List<String> reloading = new ArrayList<>();
        for (Path p : weapons) {
            String body = read(p);
            if (RANGER_CLASS.matcher(body).find()) ranger.add(idOf(p));
            if (RELOAD_TICKS.matcher(body).find()) reloading.add(idOf(p));
        }
        assertEquals(KNOWN_RANGER_WEAPONS, ranger.size(), "ranger weapons found: " + ranger);
        assertEquals(KNOWN_RANGER_WEAPONS - EXEMPT_DEV_WEAPONS.size(), reloading.size(),
                "weapons the reload pattern matched: " + reloading + " -- if it matches none, the gate below "
                        + "is measuring nothing");
        assertTrue(reloading.contains("boltor"), "the control: the Boltor authors reload_ticks 60");
    }

    @Test
    void everyRangerWeaponAuthorsAReloadOrIsAnExemptDevWeapon() {
        List<String> offenders = new ArrayList<>();
        for (Path p : weaponYaml()) {
            String body = read(p);
            if (!RANGER_CLASS.matcher(body).find()) continue;
            String id = idOf(p);
            if (EXEMPT_DEV_WEAPONS.contains(id)) continue;
            if (!RELOAD_TICKS.matcher(body).find()) offenders.add(id);
        }
        assertTrue(offenders.isEmpty(),
                "ranger weapons with no reload_ticks: " + offenders
                        + " -- deft_hands.yml is class: ranger, so it will roll onto these and render "
                        + "\"-N% Reload Time\" while granting ZERO. Author a reload, or decide deliberately that "
                        + "this weapon is an exception and say so here.");
    }

    @Test
    void theExemptionSetHoldsExactlyTheOneDevWeaponItWasWrittenFor() {
        assertEquals(List.of("hunters_bow"), EXEMPT_DEV_WEAPONS,
                "the exemption is for ONE named dev weapon in the deletion set; adding another is a review decision");
    }
}
