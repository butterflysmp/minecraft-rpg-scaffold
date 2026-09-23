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
 * *** A BOLT BODY MUST SPAWN ALREADY POINTING WHERE IT IS GOING, AND KEEP POINTING THERE. ***
 *
 * <h2>THIS CLASS HAS BEEN WRONG ONCE, AND THE REWRITE IS WHY IT STILL EXISTS</h2>
 *
 * <p>Its previous three rows pinned {@code world.spawn(facing(toLocation(at), velocity), ...)} -- a
 * helper that wrote the rotation onto a {@code Location}. <b>All three were GREEN and the change they
 * pinned did nothing.</b> {@code CraftEntityTypes} registers {@code EntityType.ARROW} through
 * {@code createAndMoveEmptyRot}, whose positioner is {@code MOVE_EMPTY_ROT} --
 * {@code Entity.snapTo(x, y, z, 0.0F, 0.0F)}, both floats constant -- so a {@code Location}'s
 * rotation never reaches an arrow. The rows proved the source said what it was meant to say. It did.
 *
 * <p><b>THE LESSON IS NOT "DELETE THE SCAN". IT IS THAT A SOURCE SCAN CAN ONLY EVER PROVE THE
 * SOURCE.</b> Those rows were honest about their own subject -- <i>is the spawn location rotated, and
 * to the velocity</i> -- and were never able to answer <i>does the body point where it is going</i>.
 * Only a boot row answers that, and the boot row is what failed. So the rows are re-pointed at the
 * new write sites rather than removed, and this paragraph stays, because the next reader's first
 * question is whether a green scan here means anything.
 *
 * <h2>WHY THIS IS A SOURCE SCAN AND NOT A BEHAVIOURAL TEST</h2>
 *
 * <p>{@code spawnBoltMarker} needs a live {@code World} and returns an entity id; {@code driveMarker}
 * needs an entity to already exist. Nothing in either module can call them, and there is no MockBukkit.
 * Same shape and same reason as {@code GearScoreWiringSignatureTest} and
 * {@code VaultWiringSignatureTest}: a scan, compared against a named expectation, failing by naming
 * the file.
 *
 * <p><b>The arithmetic is not scanned, it is TESTED</b> -- {@code BodyRotationTest} has the seven
 * directions in degrees and executes the rival convention as a mutation. What is left here is the
 * WIRING: that the value is computed from the right vector and written to the right object at the ONE
 * site that has a reader, that a null answer is skipped rather than written as {@code (0, 0)}, and
 * that the per-tick write which was removed for want of a reader has not come back.
 *
 * <h2>IT IS NOT ABOUT ONE WEAPON</h2>
 *
 * <p>Every {@code body: arrow} cast in the tree spawns through {@code spawnBoltMarker} and is driven
 * through {@code driveMarker} -- {@code dragons_plume}'s charged release and its three tap bands, and
 * anything later. The defect shipped with the arrow body and was observed on the Plume, which is what
 * identified it as belonging to these methods rather than to whichever weapon surfaced it.
 *
 * <p>Account: {@code GATE-arrow-body-orientation.md}.
 */
class BoltBodySignatureTest {

    private static final Path COMBAT_WORLD = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "adapter",
            "PaperCombatWorld.java");

    /**
     * *** THE SPAWN FRAME'S ROTATION IS WRITTEN TO THE ENTITY, FROM THE VELOCITY. ***
     *
     * <p>The row that would have caught the original had it been pointed here: the body's rotation is
     * set on the {@code Arrow} inside the spawn consumer -- which runs before
     * {@code addEntityToWorld}, and therefore before the add-entity packet whose yaw and pitch are
     * what the first rendered frame wears.
     *
     * <p>Anchored on the whole call at BOTH ENDS. A prefix needle survives a widening of the thing it
     * names, which is the single most likely edit to a scanned call site.
     */
    @Test
    void theSpawnConsumerWritesTheRotationToTheEntity() throws IOException {
        List<String> lines = read(COMBAT_WORLD, 400);

        int spawn = indexOf(lines, "world.spawn(toLocation(at), Arrow.class, arrow -> {");
        int computed = indexOf(lines, "BodyRotation spawnRotation = BodyRotation.along(velocity);");
        int written = indexOf(lines, "arrow.setRotation(spawnRotation.yaw(), spawnRotation.pitch());");

        assertTrue(spawn >= 0, "the bolt body must still be spawned at a bare toLocation(at)");
        assertTrue(computed > spawn && written > computed,
                "the spawn rotation must be computed from the VELOCITY and written to the ARROW,"
                        + " inside the spawn consumer. A rotation written onto the Location cannot"
                        + " reach an arrow: EntityType.ARROW's positioner is MOVE_EMPTY_ROT, which is"
                        + " snapTo(x, y, z, 0.0F, 0.0F) with both floats constant. That is how the"
                        + " previous version of this fix shipped inert with this file's own guard"
                        + " green.");
    }

    /**
     * AND NO ROTATION IS ROUTED THROUGH A {@code Location}, IN ANY CONVENTION.
     *
     * <p>Not a style rule. {@code Location.setDirection} writes the entity-LOOK convention, which is
     * the yaw and the pitch both negated relative to the projectile one -- so it is wrong on five of
     * seven directions -- AND it is discarded before it reaches an arrow anyway. <b>Both failures at
     * once, which is why the call is banned here rather than corrected.</b>
     *
     * <p>This is also {@code R0}'s ABSENT needle in the gate file: {@code setDirection} must not
     * appear in the deployed {@code PaperCombatWorld.class}.
     */
    @Test
    void noRotationIsRoutedThroughALocation() throws IOException {
        List<String> lines = read(COMBAT_WORLD, 400);

        assertEquals(-1, indexOf(lines, ".setDirection("),
                "Location.setDirection must not be called here. It writes the LOOK convention (yaw"
                        + " and pitch both negated) and the arrow spawn path discards it regardless.");
        assertEquals(-1, indexOf(lines, "facing(toLocation"),
                "the facing() helper is deleted. A call to it surviving means the inert path is back.");
    }


    /**
     * *** AND THERE IS DELIBERATELY NO PER-TICK ROTATION WRITE. ONE WAS WRITTEN AND REMOVED. ***
     *
     * <p>This row guards an ABSENCE, which is unusual and is the point: the obvious next edit to this
     * area is to add a corrective write in {@code driveMarker}, and it would be wrong for a reason
     * nothing else in the tree records.
     *
     * <p><b>It had no reader.</b> The operator watched a bolt on the build where the spawn rotation
     * was never applied at all and reported that it <i>"took a few seconds but did correct itself
     * eventually"</i> -- so the CLIENT converges on the travel direction from its own copy of the
     * arrow tick, and no server-side write changes what a player sees past the first frames. The
     * server's value barely travels anyway ({@code EntityType.ARROW} is built with
     * {@code updateInterval(20)}), and <b>nothing in this codebase reads a marker's rotation</b>:
     * {@code markerOf} is private with three callers, and the only things that leave
     * {@code PaperCombatWorld} are {@code getUniqueId()} and {@code markerLocation}'s {@code Vec3},
     * which has no rotation field.
     *
     * <p><b>So the server-side yaw of a bolt in flight stays about 180 degrees out from its travel,
     * knowingly.</b> Whoever adds the first READER of that value is the person who should delete this
     * row, and they will need {@code driveMarker}'s comment to know what they are undoing.
     */
    @Test
    void thereIsNoPerTickRotationWriteAndThatIsDeliberate() throws IOException {
        List<String> lines = read(COMBAT_WORLD, 400);

        assertEquals(-1, indexOf(lines, "marker.setRotation("),
                "driveMarker must NOT write a rotation. It had no reader -- the client converges on"
                        + " its own, the server sends rotation about once every 20 ticks, and nothing"
                        + " here reads a marker's rotation. If you are adding one back, say what"
                        + " reads it.");
        assertEquals(-1, indexOf(lines, "BodyRotation.along(stepVelocity)"),
                "the per-tick rotation is not computed either. Computing it and not writing it would"
                        + " be worse than both: a reader would take its presence for a write.");
    }

    /**
     * A ZERO VELOCITY IS SKIPPED AT THE ONE WRITE SITE
, NOT WRITTEN AS {@code (0, 0)}.
     *
     * <p>{@code BodyRotation.along} answers {@code null} for a zero vector, and {@code (0, 0)} is due
     * south and level -- <b>the exact defect this whole slice removes</b>, so a caller that wrote the
     * null case as zeroes would reintroduce it silently, for the one input nobody stages.
     *
     * <p>No caller produces a zero velocity today ({@code launch} scales a normalised aim by a
     * positive speed), so this is a guard against a future caller rather than a live case. It is
     * pinned because the failure would be a body pointing due south and the cause would be a null
     * check three files away.
     */
    @Test
    void aZeroVelocityIsSkippedAtTheWriteSite() throws IOException {
        List<String> lines = read(COMBAT_WORLD, 400);

        assertTrue(indexOf(lines, "if (spawnRotation != null) {") > 0,
                "the spawn write must skip a null rotation rather than writing (0, 0)");
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
     * <p>{@code PaperCombatWorld} carries long notes ABOUT the rotation, and they quote the wording
     * and the call names that used to be there -- including {@code facing()} and
     * {@code Location.setDirection}, both of which two rows above assert are ABSENT. An unfiltered
     * scan would match that prose and report the banned call present, which is the false-presence
     * rule arriving through the instrument, on a file whose defect was a comment being mistaken for a
     * mechanism.
     */
    private static boolean isComment(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*");
    }

    /** The control on the comment filter, caused rather than asserted. */
    @Test
    void theCommentFilterSkipsProseAndKeepsCode() {
        assertTrue(isComment("    // world.spawn(toLocation(at), Arrow.class"));
        assertTrue(isComment("     * a facing() helper calling Location.setDirection"));
        assertEquals(false, isComment("        Arrow body = world.spawn(toLocation(at), Arrow.class, a -> {"));
    }

    /**
     * AND THE FILTER IS LOAD-BEARING FOR THE TWO ABSENCE ROWS, MEASURED RATHER THAN ASSERTED.
     *
     * <p>The file really does contain both banned strings in prose. If the filter stopped working,
     * {@link #noRotationIsRoutedThroughALocation} would fail -- so this row proves the prose is there
     * and that the filter is the only thing keeping that row honest.
     */
    @Test
    void theBannedStringsReallyDoAppearInProseInThatFile() throws IOException {
        List<String> lines = read(COMBAT_WORLD, 400);

        long inProse = lines.stream()
                .filter(BoltBodySignatureTest::isComment)
                .filter(l -> l.contains("setDirection") || l.contains("facing("))
                .count();

        assertTrue(inProse > 0,
                "this file is expected to explain, in prose, what used to be here. If that prose is"
                        + " gone the absence rows are no longer being protected by the comment filter"
                        + " and their passing means less than it did.");
    }

    /** And absence reports -1 rather than 0, which every row above relies on. */
    @Test
    void theScannerReportsAbsenceAsMinusOneRatherThanZero() {
        assertEquals(-1, indexOf(List.of("alpha", "beta"), "gamma"));
        assertEquals(0, indexOf(List.of("alpha", "beta"), "alpha"));
    }
}
