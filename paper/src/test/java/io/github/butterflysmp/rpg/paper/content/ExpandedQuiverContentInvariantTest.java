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
 * <b>THE ENFORCER FOR EXPANDED QUIVER'S ROLL GATE: A RANGER WEAPON WITH NO MAGAZINE WOULD RENDER AN
 * HONEST-LOOKING TOOLTIP FOR NOTHING.</b>
 *
 * <p>{@code expanded_quiver.yml} is {@code class: ranger}, so it enters the roll pool of EVERY ranger
 * weapon. Its effect resolves a MAGAZINE. A ranger weapon that authors no {@code quiver_size} would
 * therefore roll the enchant, render <i>"+2 Quiver Arrows"</i>, and grant <b>zero</b> -- which is the
 * exact defect Slice D was organised around and eliminated three times in the arithmetic. This is
 * that defect arriving through the ROLL TABLE instead.
 *
 * <h2>WHY A TEST AND NOT A GATE OR A VALIDATOR ARM</h2>
 *
 * <p>The obvious homes cannot see what they would need to:
 *
 * <ul>
 *   <li><b>The roll</b> keys purely on {@link io.github.butterflysmp.rpg.core.weapon.GearClass} --
 *       {@code EnchantRoll.roll(gearClass, roster, rng)} is handed {@code (id, gearClass)} pairs and
 *       no weapon at all, so it cannot ask whether a magazine exists.
 *   <li><b>The tooltip</b> ({@code EnchantEffectLine}) receives a {@code GearClass}, not a
 *       {@code WeaponDefinition}, so it cannot render an {@code inert:} line for this case the way it
 *       does for a class mismatch.
 *   <li><b>{@code ContentValidator.validateEnchants}</b> receives only the enchants, so it cannot see
 *       the weapons.
 * </ul>
 *
 * <p>Each could be taught to, and each is a signature change rippling through its callers. <b>This
 * check needs neither, is build-time rather than boot-time, and CANNOT BE BUILT PAST</b> -- which is
 * strictly stronger than {@code ContentValidator}, whose own javadoc is explicit that it <i>"warns;
 * never throws"</i>. Same argument, same shape, as {@code ScorchContentInvariantTest}.
 *
 * <p><b>It is a mechanical check rather than a review obligation</b>, which is the property that
 * matters: the difference between <i>inert by design and visible in the content</i> and <i>inert in a
 * way only a player discovers</i>.
 *
 * <h2>IT MUST FAIL LOUDLY WHEN IT DISCOVERS NOTHING</h2>
 *
 * <p>This DISCOVERS rather than asserts -- it walks a directory looking for files -- and
 * {@code CLAUDE.md} records that failure mode twice: a scan finding zero items reads exactly like a
 * scan that ran and found nothing wrong. So the ranger count below is <b>asserted, not logged</b>.
 */
class ExpandedQuiverContentInvariantTest {

    /**
     * Ranger weapons in shipped content: {@code boltor}, {@code dragons_plume}, {@code locust},
     * {@code quiver_stone}, {@code hunters_bow}. <b>Asserted rather than logged</b>, so a regex that
     * stops matching the schema reddens instead of reporting a clean verdict over an empty set.
     *
     * <p>Expected to CHANGE, and in both directions -- the dev-weapon deletion removes two of these.
     * Update it with the content; do not relax it to a lower bound.
     *
     * <p><b>4 -> 5 when {@code dragons_plume.yml} landed, and this row is what noticed.</b> It is a
     * deliberate edit to a named count rather than a relaxation: the Plume is a REAL ranger weapon
     * that authors {@code quiver_size: 25}, so
     * {@link #everyRangerWeaponAuthorsAMagazineOrIsAnExemptDevWeapon} passes on it unchanged and
     * Expanded Quiver rolling onto it grants ACTUAL arrows -- the opposite of {@code hunters_bow},
     * which is exempt by name precisely because it has no magazine for the enchant to enlarge.
     */
    private static final int KNOWN_RANGER_WEAPONS = 5;

    /**
     * THE ONE EXEMPTION, AND IT IS A DEV WEAPON IN THE DELETION SET.
     *
     * <p>{@code hunters_bow} is {@code class: ranger} and authors no {@code quiver_size}. It is
     * exempt rather than fixed because it is already scheduled for deletion (ruled 2026-09-12, parked
     * on <i>enough shipped weapons to replace them</i>), and authoring a magazine onto a weapon that
     * is leaving would be work whose only product is deleted with it.
     *
     * <p><b>THIS IS AN EXEMPTION FOR A NAMED WEAPON, NOT A HOLE IN THE RULE.</b> The set is a
     * literal, so the day a NEW ranger weapon ships without a magazine this test reddens and the
     * author has to decide deliberately -- which is the whole point. Adding a name here is a visible
     * edit in a file whose javadoc says why; forgetting a {@code quiver_size} is not.
     *
     * <p><b>When the dev weapons go, this set goes with them</b> -- grep for
     * {@code DELETION-SET-CITATION} to find every site that must move in that sweep.
     */
    // DELETION-SET-CITATION: hunters_bow
    private static final List<String> EXEMPT_DEV_WEAPONS = List.of("hunters_bow");

    /** {@code class: ranger} at column 0 -- the weapon's OWN class, never a nested one. */
    private static final Pattern RANGER_CLASS = Pattern.compile("(?m)^class:\\s*ranger\\s*$");

    /** {@code quiver_size:} at column 0. ANCHORED, so prose naming the key is not mistaken for it. */
    private static final Pattern QUIVER_SIZE = Pattern.compile("(?m)^quiver_size:\\s*\\d+\\s*$");

    private static Path contentRoot() {
        try {
            var url = ExpandedQuiverContentInvariantTest.class.getResource("/content");
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

    @Test
    void theWeaponContentIsREACHABLEAndHoldsTheRangerWeaponsExpected() {
        // THE POSITIVE CONTROL. Everything below is a search; a search that cannot find its haystack
        // must say so rather than reporting a clean needle count.
        List<Path> weapons = weaponYaml();
        assertFalse(weapons.isEmpty(),
                "the weapon walk found NO yaml at all -- the scan is blind, and a blind scan reports "
                        + "zero problems exactly like a clean one");

        List<String> ranger = new ArrayList<>();
        for (Path p : weapons) {
            if (RANGER_CLASS.matcher(read(p)).find()) ranger.add(idOf(p));
        }
        assertEquals(KNOWN_RANGER_WEAPONS, ranger.size(),
                "expected " + KNOWN_RANGER_WEAPONS + " ranger weapons, found " + ranger
                        + " -- if content moved, update the constant; if the regex stopped matching "
                        + "the schema, the check below is measuring nothing");
    }

    /**
     * THE INVARIANT. Every ranger weapon authors a magazine, or is a named dev weapon on its way out.
     *
     * <p>Reddens the day someone ships a ranger weapon with no {@code quiver_size} while
     * {@code expanded_quiver.yml} is {@code class: ranger}. The fix is then a deliberate choice
     * between authoring a magazine, narrowing the enchant's gate, or teaching the roll to ask -- and
     * the point is that it is a CHOICE rather than a silent tooltip.
     */
    @Test
    void everyRangerWeaponAuthorsAMagazineOrIsAnExemptDevWeapon() {
        List<String> offenders = new ArrayList<>();
        for (Path p : weaponYaml()) {
            String body = read(p);
            if (!RANGER_CLASS.matcher(body).find()) continue;
            String id = idOf(p);
            if (EXEMPT_DEV_WEAPONS.contains(id)) continue;
            if (!QUIVER_SIZE.matcher(body).find()) offenders.add(id);
        }

        assertTrue(offenders.isEmpty(),
                "ranger weapons with no quiver_size: " + offenders
                        + " -- expanded_quiver.yml is class: ranger, so it will roll onto these and "
                        + "render \"+N Quiver Arrows\" while granting ZERO. Author a quiver_size, or "
                        + "decide deliberately that this weapon is an exception and say so here.");
    }

    /**
     * THE EXEMPTION LIST IS NOT ALLOWED TO ROT INTO A BLANKET.
     *
     * <p>An exemption set that grows silently stops being an exemption and becomes the rule. Pinning
     * its size means adding a name is a visible, reviewed edit -- the same discipline
     * {@link #KNOWN_RANGER_WEAPONS} applies to the population it is drawn from.
     */
    @Test
    void theExemptionSetHoldsExactlyTheOneDevWeaponItWasWrittenFor() {
        assertEquals(List.of("hunters_bow"), EXEMPT_DEV_WEAPONS,
                "the exemption is for ONE named dev weapon in the deletion set. Adding another is a "
                        + "decision that belongs in review, not a quiet append.");
    }
}
