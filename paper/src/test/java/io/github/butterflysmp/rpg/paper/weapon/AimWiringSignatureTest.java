package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * *** WHY THIS FILE EXISTS: {@code Aim}'s TWO-ARGUMENT CONSTRUCTOR IS A LIVE TRAPDOOR. ***
 *
 * <p>{@code new Aim(origin, direction)} derives the shooter's right from the direction's HORIZONTAL
 * component, and <b>that is the exact derivation that yields a ZERO vector at pitch +/-90.</b> Every
 * production aim is wired through {@link ViewAim} today, which reads the YAW and is therefore
 * defined everywhere. <b>Nothing stops a fourth site reaching for the shorter constructor.</b>
 *
 * <p>The resulting bug is invisible at every pitch except two. A world-up-equivalent basis agrees
 * with the shooter's own at every {@code |pitch| < 90}, so a spread fired anywhere but straight up
 * or straight down behaves identically. <b>No behavioural test would see it, and a player would
 * meet it the first time they shot at a bird.</b>
 *
 * <h2>SAME SHAPE AND SAME REASON AS {@code GearScoreWiringSignatureTest}</h2>
 *
 * <p>A source scan, compared against a named list, failing by naming the file. Every method this
 * guards needs a live {@code Player} or a running server, none constructible here, and there is no
 * MockBukkit. The alternative is a javadoc, and a javadoc is what this project's own records call a
 * claim rather than a control.
 *
 * <h2>ANCHORED ON STATEMENTS AND STRUCTURE, NEVER ON A BARE TOKEN OR A LINE COUNT</h2>
 *
 * <p>{@link #isComment} filters commentary out, because all three guarded sites now carry a
 * PARAGRAPH explaining why they use {@code ViewAim} -- <b>an unfiltered scan would match the
 * explanation and report the control present after someone deleted it.</b> That is CLAUDE.md's
 * false-presence rule arriving from the instrument side, and in this repo the commentary outweighs
 * the code by an order of magnitude.
 *
 * <p><b>And no assertion here is bounded by "within N lines".</b> {@code VaultWiringSignatureTest}
 * paid for that twice in one slice: a 30-line bound became a 60-line bound and then failed again,
 * neither time because of a defect -- the code had not moved, the commentary around it had grown.
 * A proximity bound in a file whose methods carry thirty lines of javadoc is an assertion about
 * PROSE LENGTH. Every row below is a whole-file absence or a whole-file presence.
 */
class AimWiringSignatureTest {

    private static final Path WEAPON_FIRE = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "weapon",
            "WeaponFire.java");

    private static final Path COMBAT_WORLD = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "adapter",
            "PaperCombatWorld.java");

    private static final Path COMMAND = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "command",
            "RpgCommand.java");

    private static final Path VIEW_AIM = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "weapon",
            "ViewAim.java");

    /** Both modules' main sources. The walk's roots, not a list of suspects. */
    private static final List<Path> MAIN_ROOTS = List.of(
            Path.of("src", "main", "java"),
            Path.of("..", "core", "src", "main", "java"));

    // --- the three production sites -------------------------------------------------------------

    /**
     * *** THE PRESS FRAME GOES THROUGH {@code ViewAim}. ***
     *
     * <p>{@code WeaponFire} is the aim every weapon fires along, so this is the site that decides
     * whether the Dragon's Breath's hexagon exists at the poles. Without it the ring collapses onto
     * the aim vector when a player looks straight up -- and the tooltip still says {@code x 7}.
     */
    @Test
    void theWeaponPressBuildsItsAimThroughViewAim() throws IOException {
        List<String> lines = read(WEAPON_FIRE, 200);
        assertTrue(indexOf(lines, "ViewAim.of(eye)") > 0,
                "WeaponFire must build its aim through ViewAim. Every shot in the game starts"
                        + " here, so a two-argument Aim on this line loses the shooter's right for"
                        + " the whole plugin.");
    }

    /**
     * *** THE VOLLEY'S LIVE RE-READ GOES THROUGH {@code ViewAim} TOO, AND IT IS THE EASIEST TO
     * FORGET. ***
     *
     * <p>{@code PaperCombatWorld.aimOf} is consulted once per volley shot, not on the press frame,
     * so a weapon firing a volley of a spread would take its basis from here. <b>A build that wired
     * the press correctly and this incorrectly would be right on every shipped weapon today</b> --
     * no shipped content composes the two -- and wrong the moment one does.
     */
    @Test
    void theLiveAimReReadBuildsThroughViewAimAsWell() throws IOException {
        List<String> lines = read(COMBAT_WORLD, 300);
        assertTrue(indexOf(lines, "ViewAim.of(living.getEyeLocation())") > 0,
                "PaperCombatWorld.aimOf must build through ViewAim. It is the per-shot aim a"
                        + " volley re-reads, so a spread inside a volley would lose its basis here"
                        + " while the press frame looked correct.");
    }

    /**
     * THE DEV CAST PATH TOO, SO {@code /rpg cast} REMAINS A FAITHFUL INSTRUMENT.
     *
     * <p>{@code /rpg apply}'s own javadoc argues the same point for statuses: a dev tool that takes
     * a different path is a dev tool that cannot reproduce the bug. A gate row driven through a
     * weaker aim would read PASS on a build that had lost the basis.
     */
    @Test
    void theDevCastPathBuildsThroughViewAim() throws IOException {
        List<String> lines = read(COMMAND, 2000);
        assertTrue(indexOf(lines, "ViewAim.of(eye)") > 0,
                "/rpg cast must build through ViewAim or it cannot reproduce a pole defect");
    }

    // --- the absence, which is the half that actually guards -------------------------------------

    /**
     * *** NO MAIN SOURCE MAY CONSTRUCT A TWO-ARGUMENT {@code Aim}. THIS IS THE ROW THAT GUARDS. ***
     *
     * <p>The three presence rows above would all pass on a build that ALSO constructed a
     * two-argument {@code Aim} somewhere else -- presence and absence are different claims, and
     * only this one closes the trapdoor. <b>{@link ViewAim} removes the opportunity rather than
     * watching it; this row is what guards the funnel itself.</b>
     *
     * <h2>*** IT WALKS EVERY MAIN SOURCE, AND A NAMED LIST IS WHY IT HAD TO ***</h2>
     *
     * <p>This scanned a NAMED LIST of four files until 2026-09-21, justified on the grounds that a
     * walk finding nothing would pass. <b>Widening it to a walk immediately found a FIFTH site the
     * list did not contain</b> -- {@code DashAim.resolve}, which built
     * {@code new Aim(success.aim().origin(), direction)} and discarded the shooter's right that
     * {@code ViewAim} had just read off the yaw.
     *
     * <p><b>That is the exact failure a named list has: it can only ever check the sites somebody
     * already thought of</b>, and the site that matters is the one nobody did. The
     * finding-nothing objection was real and is answered by a CONTROL rather than by a shorter
     * scan -- the row asserts a plausible number of files were read AND that at least one
     * construction was inspected, so an empty walk fails loudly.
     *
     * <p>The needle is {@code new Aim(} and the row then reads the WHOLE CALL, across line wraps,
     * to count its arguments. A prefix needle would keep matching after a widening, which is
     * precisely how a signature scan goes blind: {@code heldScore(player, keys)} stayed a prefix of
     * {@code heldScore(player, keys, weapons)} and kept matching after the change it existed to
     * detect.
     */
    @Test
    void noMainSourceConstructsAnAimWithoutTheShootersRight() throws IOException {
        List<Path> sources = mainSources();

        // THE SANITY CHECK, and it is two claims rather than one. A walk that read three files is
        // as blind as a walk that read none, and a walk that read 300 files but matched no
        // construction has not exercised the assertion at all.
        assertTrue(sources.size() > 200,
                "the walk read only " + sources.size() + " main sources -- it is not walking the"
                        + " tree, and an absence row over an empty walk passes by default");

        int inspected = 0;
        for (Path source : sources) {
            List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (isComment(line)) continue;
                int at = line.indexOf("new Aim(");
                if (at < 0) continue;

                inspected++;
                String call = callFrom(lines, i, at + "new Aim(".length());
                assertEquals(3, argumentsIn(call), source + ":" + (i + 1)
                        + " constructs an Aim with " + argumentsIn(call) + " arguments."
                        + " The two-argument form derives the shooter's right from the DIRECTION,"
                        + " which is ZERO at pitch +/-90 -- so a spread fired straight up collapses"
                        + " onto its aim vector while the tooltip still promises a ring."
                        + " Use ViewAim.of(location) to build one, or aim.pointing(direction) to"
                        + " re-point an existing one. Found: " + call);
            }
        }

        assertTrue(inspected > 0,
                "THE SCAN INSPECTED NO Aim CONSTRUCTION AT ALL, so it proved nothing. The needle"
                        + " 'new Aim(' no longer matches anything in main source -- a scan that"
                        + " discovers nothing must fail loudly, not read as clean.");
    }

    /**
     * Every {@code .java} under both modules' main source roots.
     *
     * <p>{@code storage} is deliberately absent: it has no dependency on {@code core.combat} and
     * cannot name {@code Aim}. Adding it would cost a walk and could never find anything, which is
     * the kind of reach that makes a scan look thorough while changing nothing.
     */
    private static List<Path> mainSources() throws IOException {
        List<Path> out = new ArrayList<>();
        for (Path root : MAIN_ROOTS) {
            if (!Files.isDirectory(root)) continue;
            try (var walk = Files.walk(root)) {
                walk.filter(p -> p.toString().endsWith(".java")).forEach(out::add);
            }
        }
        return out;
    }

    /**
     * {@code ViewAim} IS THE ONE PLACE THE THREE-ARGUMENT FORM IS BUILT, AND IT REALLY BUILDS IT.
     *
     * <p>The control for the row above. If {@code ViewAim} stopped passing a right vector, every
     * other row here would still pass -- they check the CALL SITES, and the call sites would still
     * be calling {@code ViewAim}. This is the row that notices the centre of the wheel coming out.
     */
    @Test
    void viewAimIsWhereTheThirdArgumentComesFrom() throws IOException {
        List<String> lines = read(VIEW_AIM, 40);
        assertTrue(indexOf(lines, "rightOf(eye.getYaw())") > 0,
                "ViewAim must derive the right from the YAW. Deriving it from the direction would"
                        + " reintroduce the pole degeneracy in the one place that exists to avoid"
                        + " it, and every call-site row here would stay green.");
    }

    /**
     * THE YAW IS READ FROM THE {@code Location}, NOT RECOVERED FROM THE DIRECTION.
     *
     * <p>Stated as its own row because the two are indistinguishable in a diff and only one of them
     * works: the yaw is not recoverable from a vertical look vector, where both horizontal
     * components are zero. A signature taking a direction would compile, pass every test likely to
     * be staged, and lose exactly the case this whole slice is about.
     */
    @Test
    void viewAimTakesALocationRatherThanADirection() throws IOException {
        List<String> lines = read(VIEW_AIM, 40);
        assertTrue(indexOf(lines, "public static Aim of(Location eye)") > 0,
                "ViewAim.of must take a Location. A direction has thrown the yaw away, which is"
                        + " the one component that cannot be recovered at the pole.");
    }

    // --- helpers, and their own controls --------------------------------------------------------

    /**
     * The argument list of a call beginning at {@code from} on line {@code i}, joined across lines.
     *
     * <p>Joined rather than read from one line because a constructor call in this repo routinely
     * wraps at 100 columns -- and <b>a multi-line call read one line at a time would report two
     * arguments for a three-argument construction</b>, failing the guard on correct code. The
     * false-absence rule, arriving as a false POSITIVE on a scanner.
     */
    private static String callFrom(List<String> lines, int index, int from) {
        StringBuilder out = new StringBuilder();
        int depth = 1;
        for (int i = index; i < lines.size() && i < index + 12; i++) {
            String line = i == index ? lines.get(i).substring(from) : lines.get(i).trim();
            for (int c = 0; c < line.length(); c++) {
                char ch = line.charAt(c);
                if (ch == '(') depth++;
                if (ch == ')') {
                    depth--;
                    if (depth == 0) return out.toString();
                }
                out.append(ch);
            }
            out.append(' ');
        }
        return out.toString();
    }

    /** Top-level commas plus one. An empty call is zero arguments. */
    private static int argumentsIn(String call) {
        if (call.isBlank()) return 0;
        int depth = 0;
        int count = 1;
        for (int i = 0; i < call.length(); i++) {
            char ch = call.charAt(i);
            if (ch == '(' || ch == '[') depth++;
            if (ch == ')' || ch == ']') depth--;
            if (ch == ',' && depth == 0) count++;
        }
        return count;
    }

    /**
     * *** THE CONTROL ON {@code argumentsIn}, CAUSED RATHER THAN ASSERTED. ***
     *
     * <p>A counter that always returned 3 would pass every row in this file, which is the
     * hardcoded-to-CLEAN failure {@code check-crlf.sh} guards against with three controls. So both
     * directions are exercised, and the NESTED case is exercised too -- a naive comma count would
     * read {@code new Aim(toVec3(a, b), c)} as three arguments and let a two-argument construction
     * through wearing a nested call.
     */
    @Test
    void theArgumentCounterCountsTopLevelCommasOnly() {
        assertEquals(2, argumentsIn("toVec3(eye), toVec3(eye.getDirection())"));
        assertEquals(3, argumentsIn("origin, direction, right"));
        assertEquals(3, argumentsIn("new Vec3(a, b, c), dir, rightOf(yaw)"),
                "a nested call's commas must not be counted, or a two-argument Aim passes");
        assertEquals(1, argumentsIn("onlyOne"));
        assertEquals(0, argumentsIn(""));
    }

    /** And the joiner really crosses a line break, which the row above depends on. */
    @Test
    void theCallReaderJoinsAcrossAWrappedCall() {
        List<String> wrapped = List.of(
                "        return Optional.of(new Aim(origin,",
                "                direction,",
                "                right));");
        String call = callFrom(wrapped, 0, wrapped.get(0).indexOf("new Aim(") + "new Aim(".length());
        assertEquals(3, argumentsIn(call),
                "a wrapped three-argument call must not read as two -- that would fail the guard"
                        + " on correct code, which is worse than not having it");
    }

    private static List<String> read(Path path, int atLeast) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertTrue(lines.size() > atLeast,
                "the scan must have read " + path + ", not an empty or wrong path -- finding"
                        + " nothing here would make every assertion below meaningless, and would"
                        + " make the absence row pass by default. Read " + lines.size()
                        + " lines, expected more than " + atLeast);
        return lines;
    }

    private static int indexOf(List<String> lines, String needle) {
        for (int i = 0; i < lines.size(); i++) {
            if (isComment(lines.get(i))) continue;
            if (lines.get(i).contains(needle)) return i;
        }
        return -1;
    }

    /**
     * *** A COMMENTED-OUT CALL IS NOT A CALL, AND ALL FOUR GUARDED SITES NAME {@code ViewAim} IN
     * PROSE. ***
     *
     * <p>Each of the three call sites carries a paragraph explaining why it uses {@code ViewAim}
     * rather than the two-argument constructor -- and those paragraphs name BOTH. An unfiltered
     * scan would report the control present because the comment lamenting its absence still names
     * it, AND would fail the absence row because the same comment contains {@code new Aim(}.
     */
    private static boolean isComment(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*");
    }

    /** The control on the comment filter, caused rather than asserted. */
    @Test
    void theCommentFilterSkipsProseAndKeepsCode() {
        assertTrue(isComment("    // new Aim(origin, direction);"));
        assertTrue(isComment("     * new Aim(origin, direction)"));
        assertTrue(isComment("    /* new Aim( */"));
        assertTrue(!isComment("    Aim aim = ViewAim.of(eye);"));
    }

    /** And the scanner reports absence as -1 rather than 0, which every presence row relies on. */
    @Test
    void theScannerReportsAbsenceAsMinusOneRatherThanZero() {
        assertEquals(-1, indexOf(List.of("alpha", "beta"), "gamma"));
        assertEquals(0, indexOf(List.of("alpha", "beta"), "alpha"));
    }
}
