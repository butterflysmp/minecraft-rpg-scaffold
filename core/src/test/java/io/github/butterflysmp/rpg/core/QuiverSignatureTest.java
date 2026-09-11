package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.weapon.Quiver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Quiver} may not learn a capacity or a reload duration, and this fails the build when it does.
 *
 * <h2>Why this exists rather than a comment</h2>
 *
 * <p>The quiver slice was SPLIT on the strength of one property. A1 ships the per-item count with
 * capacity and reload ticks supplied by the caller; A2 promotes both to real stats and was budgeted
 * as <b>a change to the two call sites that supply them</b> -- roughly 25 files of known-shape stat
 * plumbing instead of a rewrite. That budget is only real while {@code Quiver} itself never reads
 * those numbers.
 *
 * <p><b>AND THE CONDITION IS INVISIBLE AT THE POINT WHERE IT WOULD BE BROKEN.</b> Whoever adds a
 * {@code DEFAULT_CAPACITY} for a convenience overload, or takes a {@code WeaponDefinition} because
 * every caller has one to hand, gets a compiler that is perfectly happy, a call site that reads
 * better than what it replaced, and no reason whatsoever to open {@code PLAN-quiver.md} and discover
 * that a slice boundary was drawn around the absence of that field. The split would still be paid
 * for and quietly not delivered: it compiles, it passes, and it looks like the plan.
 *
 * <p>So it is written where it WILL be seen -- in a red build. {@code DamageSignatureTest} exists for
 * precisely this argument about adjacent booleans, and {@code Scorch.UNDECLARED_CAP} has
 * {@code ScorchContentInvariantTest}; the precedent is deliberate and the phrasing is borrowed
 * knowingly. <b>A safety that holds on a condition nobody wrote down where it would be violated is
 * not a safety.</b>
 *
 * <h2>What it does NOT claim</h2>
 *
 * <p>This is not "constants are bad", and it is not a general rule about core classes.
 * {@link io.github.butterflysmp.rpg.core.weapon.Durability#MIN_USES} is a constant in the sibling
 * class this one is modelled on, and it belongs there: it is a property of the no-break PROMISE, not
 * a per-weapon number a stat will later supply. The hazard here is specifically <b>a value that slice
 * A2 must be free to vary</b> being frozen into the class that computes with it.
 *
 * <p>It also cannot prove the two call sites are the ONLY ones. It proves the class cannot know the
 * numbers, which is the half that makes "only two call sites change" checkable instead of hopeful.
 */
class QuiverSignatureTest {

    /**
     * {@code Quiver} holds no state of any kind -- so there is nowhere for a capacity or a reload
     * duration to be cached, defaulted, or memoised.
     *
     * <p>Stated as "no fields at all" rather than "no int fields" on purpose: it is a strictly
     * stronger claim, it is trivially true today, and a weaker rule would have to argue about which
     * types could smuggle a number (a {@code long}, a {@code double}, an {@code OptionalInt}, an
     * enum whose constants carry one).
     */
    @Test
    void quiverDeclaresNoFieldsSoNoNumberCanHideInIt() {
        List<String> declared = new ArrayList<>();
        for (Field field : Quiver.class.getDeclaredFields()) {
            if (field.isSynthetic()) continue;   // JaCoCo adds $jacocoData under coverage
            declared.add(field.getType().getSimpleName() + " " + field.getName());
        }
        assertTrue(declared.isEmpty(),
                "Quiver must hold no state -- capacity and reload ticks are A2's to supply. Found: "
                        + declared);
    }

    /**
     * Every public method takes PRIMITIVES ONLY.
     *
     * <p>The failure this blocks is not a constant but an ARGUMENT: {@code spend(WeaponDefinition)},
     * or a {@code CombatantStats} threaded in "so it can look the capacity up itself". Either one
     * moves the supply of the number from the call site into this class, which is the same loss by a
     * different door -- and the more likely one, because every caller does have a definition to hand
     * and passing it reads like tidying up.
     *
     * <p>{@code Durability} already satisfies this ({@code wear(int, int, int)}), so this pins an
     * existing house shape rather than inventing one for the quiver's benefit.
     */
    @Test
    void everyPublicQuiverMethodTakesPrimitivesOnly() {
        List<String> offenders = new ArrayList<>();
        for (Method method : Quiver.class.getDeclaredMethods()) {
            if (method.isSynthetic() || !Modifier.isPublic(method.getModifiers())) continue;
            for (Class<?> parameter : method.getParameterTypes()) {
                if (!parameter.isPrimitive()) {
                    offenders.add(method.getName() + "(" + parameter.getSimpleName() + ")");
                }
            }
        }
        assertTrue(offenders.isEmpty(),
                "a non-primitive parameter moves the supply of capacity/reload into Quiver, which is "
                        + "what A2's budget rests on not happening. Found: " + offenders);
    }

    /**
     * THE POSITIVE CONTROL, and it is not ceremony.
     *
     * <p>Both rows above are satisfied by a class that has no methods and no fields -- so if
     * {@code Quiver} were deleted, renamed, or emptied, they would pass in silence. That is this
     * repository's own recorded defect one level up: <i>a check that did not run looks exactly like a
     * check that passed</i>, and <i>anything that DISCOVERS rather than asserts must fail loudly when
     * it discovers nothing</i>. Both scans above discover rather than assert.
     *
     * <p>So the count is pinned. A new public method is a deliberate edit here, which is the moment
     * to ask whether it obeys the rule above.
     */
    @Test
    void theScansAboveActuallyFoundSomethingToScan() {
        List<String> publicMethods = Arrays.stream(Quiver.class.getDeclaredMethods())
                .filter(m -> !m.isSynthetic() && Modifier.isPublic(m.getModifiers()))
                .map(Method::getName)
                .sorted()
                .toList();
        assertEquals(
                List.of("applyPercent", "clamp", "isEmpty", "reload", "reloadComplete",
                        "reloadCompletesAt", "reloadTicksRemaining", "spend"),
                publicMethods,
                "the scanned set, named rather than counted -- a count against an unnamed set is not "
                        + "an answer, and an empty scan would otherwise pass both rows above");
    }
}
