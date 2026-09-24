package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.ability.CastSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * WHERE THE BODIES OF A SPREAD POINT: one down the aim vector, the rest in a ring around it,
 * BUILT IN THE SHOOTER'S VIEW PLANE.
 *
 * <p>Ruled by the operator: the pattern <b>looks identical from the shooter's side at every angle,
 * including straight up.</b> That is a statement about the shooter's own frame, not about the
 * world's, and the difference between those two frames is this class's entire subject.
 *
 * <h2>*** THE BASIS ARRIVES AS A PARAMETER. DERIVING IT HERE IS THE BUG. ***</h2>
 *
 * <p>The obvious way to build a view-plane basis from a look vector is to cross it with world up.
 * <b>It is wrong, and it is wrong in the one place nobody tests.</b>
 *
 * <pre>
 * right = forward x worldUp        equals the shooter's own right at EVERY pitch but two
 *                                  == ZERO at pitch +90 and -90, where forward IS worldUp
 * </pre>
 *
 * <p><b>So a world-up basis and the shooter's own basis AGREE EXACTLY except at the poles.</b>
 * {@code forward x worldUp} is horizontal and perpendicular to forward's horizontal projection --
 * which is precisely the shooter's right -- for all {@code |pitch| < 90}. It degenerates only AT
 * the pole, and there the hexagon flattens into a line or vanishes.
 *
 * <p><b>THE CONSEQUENCE FOR TESTING, WHICH IS WHY THIS PARAGRAPH IS HERE RATHER THAN IN A PLAN:</b>
 * a fixture staged at the horizon <b>cannot tell the two derivations apart</b>. A mutation
 * replacing this basis with a world-up one is invisible at pitch 0, so a row staged there measures
 * the fixture instead of the code. <b>The pole rows are the only rows that can see this axis at
 * all.</b>
 *
 * <p>The shooter's right depends only on YAW, so it never degenerates -- and <b>yaw is not
 * recoverable from the direction at the pole</b>, where both horizontal components are zero. That
 * is why {@link Aim} carries a right vector rather than this class deriving one: the information
 * genuinely is not in the direction.
 *
 * <h2>THE SIGN OF UP IS UNGUARDABLE, AND IT IS NAMED RATHER THAN GUARDED</h2>
 *
 * <p>{@code up = right x forward}, which at pitch 0 with the project's yaw convention is world up.
 * <b>Flipping it produces a byte-identical pattern.</b> N points spaced evenly around a ring map
 * onto themselves under reflection -- for the shipped seven, {@code {0, 60, 120, 180, 240, 300}}
 * reversed is the same set -- so <b>no assertion on the handedness of this basis can fail.</b>
 *
 * <p>Said here in the {@code HitDamage} manner, where three of four orderings are UNGUARDABLE
 * rather than unguarded, because the alternative is a test row that cannot redden. The sign is
 * still chosen correctly: an asymmetric pattern would make it observable, and a reader who finds a
 * sign nobody chose will assume it was arbitrary.
 *
 * <h2>DEGREES IN, UNIT VECTORS OUT</h2>
 *
 * <p>Unlike {@link DrawFan}, which yields YAW OFFSETS and leaves the vector to {@code paper}, this
 * yields the vectors themselves. The reason is the split it is built on: a yaw offset is one
 * number and needs no basis, where a view-plane offset needs two axes -- so handing out angles
 * would hand out the basis problem with them, to a caller with no test harness for it. <b>The
 * basis is the thing that goes wrong; it stays where a unit test reaches it.</b>
 */
public final class SpreadPattern {

    private SpreadPattern() {}

    /**
     * The directions one press fires along: {@code spread.count()} unit vectors, <b>the aim vector
     * FIRST</b> and the ring after it in increasing angle from the shooter's right.
     *
     * <p><b>The aim vector is first so the list is never merely a set.</b> A caller rendering one
     * body differently -- a tracer, a heavier centre shot -- needs to know which one it is, and a
     * caller that does not care is unaffected. {@link DrawFan} states the opposite for its own
     * output ("the ORDER carries no meaning and no caller may depend on it") because its fan is
     * symmetric about a centre that may not exist; this one always has a centre.
     *
     * <p>The ring is built at {@code tan(angleDegrees)} off-axis and RENORMALISED, so every
     * returned vector is a unit vector and the angle each makes with the aim is exactly
     * {@code angleDegrees}. Adding the offset to a unit forward and normalising is what makes that
     * true -- scaling the offset by the speed instead would give a ring whose angle varied with
     * speed.
     *
     * @param forward the aim direction. Normalised here; a caller need not.
     * @param right   the SHOOTER'S right, perpendicular to {@code forward}. See the class javadoc
     *                for why this is a parameter.
     * @param spread  the authored ring, already value-checked by its own constructor
     * @throws IllegalArgumentException if the basis is degenerate -- see below
     */
    public static List<Vec3> directionsFor(Vec3 forward, Vec3 right, CastSpec.Spread spread) {
        Vec3 f = forward.normalize();
        Vec3 r = right.normalize();
        Vec3 up = r.cross(f).normalize();

        // *** A DEGENERATE BASIS THROWS RATHER THAN COLLAPSING QUIETLY, AND THE ARM IS REACHABLE.
        //
        // With a zero or parallel right, `up` is zero and every ring body would come back equal to
        // `f` -- the cast would fire count bodies down one line while the tooltip advertised a
        // spread. That is this project's named worst case: a mechanism that resolves perfectly and
        // does nothing.
        //
        // It is REACHABLE, which is what makes it a guard rather than a dead catch: Aim's two-
        // argument convenience derives `right` horizontally and yields ZERO at pitch +/-90, so a
        // caller who reaches for the shorter constructor and fires a spread straight up arrives
        // here. AimWiringSignatureTest is what keeps production off that path; this is what happens
        // if it ever fails to.
        //
        // A throw mid-cast is the lesser evil HERE and not everywhere -- CastExecutor.executeFan
        // declines to guard its own analogous case on the grounds that five dashes are "visible on
        // the first press". A collapsed ring is not visible: it looks like a weapon firing one
        // arrow, at a pitch nobody is watching.
        if (up.lengthSquared() == 0) {
            throw new IllegalArgumentException(
                    "spread basis is degenerate: right " + right + " is zero or parallel to forward "
                            + forward + ", so there is no view plane to build a ring in. This is what"
                            + " Aim's two-argument constructor yields at pitch +/-90 -- pass the"
                            + " shooter's own right vector.");
        }

        double offset = Math.tan(Math.toRadians(spread.angleDegrees()));
        List<Vec3> directions = new ArrayList<>(spread.count());
        directions.add(f);

        int ring = spread.ringCount();
        for (int i = 0; i < ring; i++) {
            double phi = 2 * Math.PI * i / ring;
            Vec3 lateral = r.scale(Math.cos(phi) * offset).add(up.scale(Math.sin(phi) * offset));
            directions.add(f.add(lateral).normalize());
        }
        return directions;
    }
}
