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
 * WIRING: that the value is computed from the right vector, written to the right object, at the right
 * two sites, and that a null answer is skipped rather than written as {@code (0, 0)}.
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
     * *** AND THE ROTATION IS RE-WRITTEN EVERY TICK, FROM THE SAME TICK'S VELOCITY. ***
     *
     * <p>A single write at spawn is not enough and the reason is in the platform:
     * {@code AbstractArrow.tick} eases the yaw toward {@code atan2(-x, -z)} -- reversed -- for a
     * {@code noPhysics} arrow, at {@code Mth.lerp(0.2f, ..)} a tick, and every bolt body is
     * {@code noPhysics}. <b>One write is 20% gone after one tick.</b>
     *
     * <p>Pinned on {@code stepVelocity} rather than on any vector, because rotating to the CASTER'S
     * aim, or to the launch velocity, is correct for a straight single bolt and wrong for every
     * steered or spread one -- and indistinguishable from correct in any single-bolt test anyone
     * would write.
     */
    @Test
    void theRotationIsRewrittenEveryTickFromThatTicksVelocity() throws IOException {
        List<String> lines = read(COMBAT_WORLD, 400);

        int drive = indexOf(lines, "public void driveMarker(UUID markerId, Vec3 stepVelocity) {");
        int computed = indexOf(lines, "BodyRotation rotation = BodyRotation.along(stepVelocity);");

        assertTrue(drive >= 0, "driveMarker's signature moved; this scan is no longer anchored");
        assertTrue(computed > drive,
                "driveMarker must re-write the rotation from THIS tick's velocity. Without it the"
                        + " platform's own lerp toward atan2(-x, -z) walks the body round to point"
                        + " backwards, 20% of the way per tick.");
    }

    /**
     * AND THE PER-TICK WRITE IS GATED TO ARROW BODIES.
     *
     * <p>{@code driveMarker} drives ITEM markers too, and an item entity's rotation is not rendered.
     * Writing one would not be harmless belt-and-braces; it would be a write with no meaning, which
     * is the kind of line a later reader deletes for the right reason and the wrong target.
     */
    @Test
    void thePerTickWriteIsGatedToArrowBodies() throws IOException {
        List<String> lines = read(COMBAT_WORLD, 400);

        assertTrue(indexOf(lines, "if (marker instanceof AbstractArrow) {") > 0,
                "the per-tick rotation write must be gated on the marker being an arrow. driveMarker"
                        + " also drives item markers, whose rotation nothing renders.");
    }

    /**
     * A ZERO VELOCITY IS SKIPPED AT BOTH SITES, NOT WRITTEN AS {@code (0, 0)}.
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
    void aZeroVelocityIsSkippedAtBothWriteSites() throws IOException {
        List<String> lines = read(COMBAT_WORLD, 400);

        assertTrue(indexOf(lines, "if (spawnRotation != null) {") > 0,
                "the spawn write must skip a null rotation rather than writing (0, 0)");
        assertTrue(indexOf(lines, "if (rotation != null) marker.setRotation(") > 0,
                "the per-tick write must skip a null rotation rather than writing (0, 0)");
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
