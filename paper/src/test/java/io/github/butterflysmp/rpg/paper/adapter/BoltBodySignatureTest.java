package io.github.butterflysmp.rpg.paper.adapter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * *** A BOLT BODY MUST SPAWN ALREADY POINTING WHERE IT IS GOING. ***
 *
 * <h2>WHY THIS IS A SOURCE SCAN AND NOT A BEHAVIOURAL TEST</h2>
 *
 * <p>{@code spawnBoltMarker} needs a live {@code World} and returns an entity id; nothing in either
 * module can call it, and there is no MockBukkit. Same shape and same reason as
 * {@code GearScoreWiringSignatureTest} and {@code VaultWiringSignatureTest}: a scan, compared
 * against a named expectation, failing by naming the file.
 *
 * <h2>WHAT WENT WRONG, AND WHY A COMMENT WAS NOT ENOUGH</h2>
 *
 * <p>A {@code Vec3} carries no rotation, so {@code toLocation} produces a {@code Location} at yaw 0
 * and pitch 0 -- <b>due south and level</b>. An arrow derives its rotation from {@code atan2} over
 * {@code deltaMovement} <b>inside its own {@code tick()}</b>, so the velocity set at creation is
 * right one tick BEFORE the rotation derived from it is, and <b>the spawn frame renders whatever
 * the Location said.</b>
 *
 * <p><b>The method already carried a comment about this and the comment read as a fix.</b> It said
 * the velocity is set at creation <i>"so a body spawned still has no direction on its first
 * frame"</i> -- correct about the problem, incomplete as a remedy, and there is no way to tell
 * those apart by reading. <b>Two independent quantities were being set and only one of them was the
 * one that renders.</b>
 *
 * <p>So the guard is mechanical. A comment asserting the rotation is handled is exactly what was
 * there while it was not.
 *
 * <h2>IT IS NOT ABOUT ONE WEAPON</h2>
 *
 * <p>Every {@code body: arrow} cast in the tree spawns through this one method -- {@code
 * dragons_plume}'s charged release and its three tap bands, and anything later. The defect shipped
 * with the arrow body and was observed on the Plume, which is what identified it as belonging to
 * this method rather than to whichever weapon surfaced it.
 */
class BoltBodySignatureTest {

    private static final Path COMBAT_WORLD = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "adapter",
            "PaperCombatWorld.java");

    /**
     * *** THE SPAWN LOCATION CARRIES A DIRECTION. ***
     *
     * <p>The row that would have caught the original: the body must be spawned at a location that
     * has been ROTATED, not at a bare {@code toLocation(at)}.
     *
     * <p>Anchored on the whole call rather than on {@code facing}, because a bare token would match
     * the helper's own declaration and its javadoc. The needle is the argument as it appears at the
     * spawn site.
     */
    @Test
    void theBoltBodySpawnsAtALocationThatHasBeenRotatedToItsVelocity() throws IOException {
        List<String> lines = read(COMBAT_WORLD, 400);

        int spawn = indexOf(lines, "world.spawn(facing(toLocation(at), velocity), Arrow.class");
        assertTrue(spawn > 0,
                "the bolt body must spawn at a location rotated to its velocity. A bare"
                        + " toLocation(at) is yaw 0 / pitch 0 -- due south and level -- and that is"
                        + " what the FIRST RENDERED FRAME wears, whatever velocity the body is"
                        + " given in the same breath. Setting the velocity does not fix it: an"
                        + " arrow derives its rotation in tick(), one tick later.");
    }

    /**
     * AND THE ROTATION IS DERIVED FROM THE VELOCITY, NOT FROM THE CASTER'S LOOK.
     *
     * <p>A plausible wrong fix: rotate the spawn location to the SHOOTER'S aim. That is right for
     * the centre body of a spread and wrong for the other six, right for a volley's first shot and
     * wrong after the caster turns, and <b>indistinguishable from correct in every single-bolt
     * test anyone would write.</b>
     *
     * <p>Pinned by asserting the helper takes the same {@code velocity} the body is then given, so
     * the two cannot drift apart.
     */
    @Test
    void theSpawnRotationComesFromTheVelocityRatherThanFromAnAim() throws IOException {
        List<String> lines = read(COMBAT_WORLD, 400);

        int spawn = indexOf(lines, "world.spawn(facing(toLocation(at), velocity), Arrow.class");
        int setVelocity = indexOf(lines, "arrow.setVelocity(new Vector(velocity.x()");

        assertTrue(spawn > 0 && setVelocity > spawn,
                "the spawn must rotate to the SAME velocity the body is given. Rotating to the"
                        + " caster's aim instead is correct for a single bolt and wrong for every"
                        + " body of a spread but one -- and no single-bolt test could tell.");
    }

    /**
     * A ZERO DIRECTION LEAVES THE ROTATION ALONE.
     *
     * <p>{@code Location.setDirection} on a zero vector writes {@code pitch = 90} -- straight down
     * -- silently. No caller produces one today, so this is a guard against a future caller rather
     * than a live case; it is pinned because the failure would be a body pointing at the floor and
     * the cause would be three files away.
     */
    @Test
    void aZeroDirectionIsLeftAloneRatherThanPointedAtTheFloor() throws IOException {
        List<String> lines = read(COMBAT_WORLD, 400);

        int guard = indexOf(lines, "if (direction.lengthSquared() == 0) return at;");
        assertTrue(guard > 0,
                "facing() must leave a zero direction alone. setDirection on a zero vector writes"
                        + " pitch 90 -- straight down -- which is a worse answer than the default"
                        + " and is arrived at silently.");
    }

    // --- helpers, and their own controls --------------------------------------------------------

    private static List<String> read(Path path, int atLeast) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertTrue(lines.size() > atLeast,
                "the scan must have read " + path + ", not an empty or wrong path -- finding"
                        + " nothing here would make every assertion above pass or fail for the"
                        + " wrong reason. Read " + lines.size() + ", expected more than " + atLeast);
        return lines;
    }

    /** The first line holding {@code needle} as CODE, or -1. */
    private static int indexOf(List<String> lines, String needle) {
        for (int i = 0; i < lines.size(); i++) {
            if (isComment(lines.get(i))) continue;
            if (lines.get(i).contains(needle)) return i;
        }
        return -1;
    }

    /**
     * *** A COMMENTED-OUT CALL IS NOT A CALL, AND THIS FILE'S SUBJECT IS A COMMENT THAT READ AS A
     * FIX. ***
     *
     * <p>{@code PaperCombatWorld} now carries a long note ABOUT the rotation, quoting the wording
     * that used to be there. An unfiltered scan would match that prose and report the control
     * present -- which is the false-presence rule arriving through the instrument, on a file whose
     * defect was a comment being mistaken for a mechanism.
     */
    private static boolean isComment(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*");
    }

    /** The control on the comment filter, caused rather than asserted. */
    @Test
    void theCommentFilterSkipsProseAndKeepsCode() {
        assertTrue(isComment("    // world.spawn(facing(toLocation(at), velocity), Arrow.class"));
        assertTrue(isComment("     * facing(toLocation(at), velocity)"));
        assertEquals(false, isComment("        Arrow body = world.spawn(facing(x), Arrow.class, a -> {"));
    }

    /** And absence reports -1 rather than 0, which every row above relies on. */
    @Test
    void theScannerReportsAbsenceAsMinusOneRatherThanZero() {
        assertEquals(-1, indexOf(List.of("alpha", "beta"), "gamma"));
        assertEquals(0, indexOf(List.of("alpha", "beta"), "alpha"));
    }
}
