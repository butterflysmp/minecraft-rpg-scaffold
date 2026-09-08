package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The damage seams may not carry TWO adjacent booleans, and this fails the build when they do.
 *
 * <h2>Why this exists rather than a comment</h2>
 *
 * {@code CombatantHandle.applyDamage} carried {@code boolean wasCrit, boolean bypassesDefense}, and
 * {@code CombatantStats.damage} carried {@code boolean dealerIsPlayer, boolean wasCrit, boolean
 * bypassesDefense} -- <b>three adjacent booleans, six orderings, five wrong, and all six compiling</b>,
 * with {@code BukkitCombatant} passing all three positionally. Transposed, the scorch call site
 * silently meant "this was a crit, and Defense applies".
 *
 * <p>Two were lifted to {@link CritState} and {@link DefenseRule}. {@code dealerIsPlayer} was allowed
 * to remain a {@code boolean} on one condition: <b>that it is the only one left</b>. A lone boolean
 * has nothing to be transposed with.
 *
 * <p><b>THAT CONDITION IS INVISIBLE AT THE POINT WHERE IT WOULD BE BROKEN.</b> Whoever adds a fourth
 * flag to one of these methods gets a compiler that is perfectly happy, a call site that still reads
 * plausibly, and no reason whatsoever to open this file or read the javadoc that explains why the
 * remaining boolean was safe. That is the scaffold-shape failure this project has now been bitten by
 * repeatedly: <b>a safety that holds on a condition nobody wrote down where it would be violated.</b>
 *
 * <p>So it is written where it WILL be seen -- in a red build. {@code Scorch.UNDECLARED_CAP} has
 * {@code ScorchContentInvariantTest} for exactly this reason, and the precedent is deliberate.
 *
 * <h2>What it does NOT claim</h2>
 *
 * This is not "booleans are bad". A single boolean parameter is fine and several of these methods have
 * one. <b>The hazard is ADJACENCY</b> -- two same-typed flags in one list that the compiler cannot tell
 * apart. One is unambiguous; two is a coin flip that type-checks.
 */
class DamageSignatureTest {

    /** The seams a damage flag would plausibly be added to. */
    private static final List<Class<?>> DAMAGE_SEAMS =
            List.of(CombatantHandle.class, CombatantStats.class);

    @Test
    void noDamageSeamMethodDeclaresTwoOrMoreBooleanParameters() {
        List<String> offenders = new ArrayList<>();
        int scanned = 0;

        for (Class<?> seam : DAMAGE_SEAMS) {
            for (Method m : seam.getDeclaredMethods()) {
                if (m.isSynthetic()) continue;
                scanned++;
                long booleans = Arrays.stream(m.getParameterTypes())
                        .filter(t -> t == boolean.class)
                        .count();
                if (booleans >= 2) {
                    offenders.add(seam.getSimpleName() + "." + m.getName()
                            + " declares " + booleans + " boolean parameters: "
                            + Arrays.toString(m.getParameterTypes()));
                }
            }
        }

        // A SCAN THAT DISCOVERS NOTHING READS EXACTLY LIKE A CLEAN ONE. Assert the walk actually
        // happened before believing its verdict -- the defect this repo has recorded twice for
        // content scans, arriving here as a reflection walk.
        assertTrue(scanned > 0, "reflected over NO methods -- the scan did not run");
        assertTrue(scanned >= 20,
                "expected the damage seams to expose many methods, scanned only " + scanned
                        + " -- a renamed or moved class would silently empty this check");

        assertTrue(offenders.isEmpty(),
                "A damage seam declares two or more boolean parameters, which is the transposition "
                        + "hazard CritState and DefenseRule were introduced to remove. Add a TYPE, or "
                        + "convert the existing boolean too -- do not add a second one.\n  "
                        + String.join("\n  ", offenders));
    }

    @Test
    void thePositiveControlSeesTheMethodsThisIsActuallyAbout() {
        // Names the two signatures by shape, so a rename or a re-ordering that put this check on the
        // wrong methods reddens here rather than passing vacuously above.
        assertTrue(hasMethodTaking(CombatantHandle.class, "applyDamage", CritState.class, DefenseRule.class),
                "CombatantHandle.applyDamage no longer takes CritState and DefenseRule -- either it "
                        + "was renamed, or the flags went back to booleans");
        assertTrue(hasMethodTaking(CombatantStats.class, "damage", CritState.class, DefenseRule.class),
                "CombatantStats.damage no longer takes CritState and DefenseRule");
    }

    @Test
    void exactlyOneBooleanSurvivesOnTheStatsSeamAndThatIsTheDocumentedCondition() {
        // dealerIsPlayer. Its safety is that it is ALONE; this pins the fact the javadoc leans on.
        Method widest = Arrays.stream(CombatantStats.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("damage"))
                .max((a, b) -> Integer.compare(a.getParameterCount(), b.getParameterCount()))
                .orElseThrow(() -> new AssertionError("no CombatantStats.damage method found at all"));

        long booleans = Arrays.stream(widest.getParameterTypes())
                .filter(t -> t == boolean.class)
                .count();

        assertEquals(1, booleans,
                "the widest damage() overload should carry exactly ONE boolean (dealerIsPlayer). "
                        + "Zero means it was typed and this javadoc is stale; two or more is the "
                        + "hazard itself. Signature: " + Arrays.toString(widest.getParameterTypes()));
    }

    private static boolean hasMethodTaking(Class<?> owner, String name, Class<?>... required) {
        return Arrays.stream(owner.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .anyMatch(m -> {
                    List<Class<?>> params = Arrays.asList(m.getParameterTypes());
                    return Arrays.stream(required).allMatch(params::contains);
                });
    }
}
