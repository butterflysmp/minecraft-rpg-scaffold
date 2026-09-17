package io.github.butterflysmp.rpg.paper.vault;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * *** WHY THIS FILE EXISTS: PR 1's WHOLE SUITE PASSES ON A BUILD THAT NEVER LOADS A VAULT. ***
 *
 * <p>{@code VaultServiceTest} drives {@code onJoin} and {@code writePage} directly, so it is green
 * whether or not anything in the plugin ever calls them. Delete {@code vaults.onJoin(...)} from
 * {@code RpgListeners} and every unit row in this slice still passes; the only symptom is that no
 * player ever has a vault, which no test can see and which PR 1 has no screen to reveal.
 *
 * <p>Same shape, and the same reason, as {@code ProgressionWiringSignatureTest}: a source scan,
 * compared against a named list, failing by naming the file. {@code RpgListeners} needs a live
 * {@code Player} and the Brigadier tree needs a running server to build at all -- neither is
 * constructible here, and there is no MockBukkit.
 *
 * <h2>ANCHORED ON DECLARATIONS AND ADJACENCY, NEVER ON A BARE TOKEN</h2>
 *
 * The comments in these files name {@code Permissions.DEV}, {@code vaults} and {@code VaultService}
 * repeatedly -- this slice's own javadocs are the densest commentary in it. <b>A bare grep would
 * match the prose explaining a control and report it present after somebody deleted it</b>, which is
 * CLAUDE.md's false-presence rule arriving from the instrument side. So every assertion below pins a
 * STATEMENT, or the relationship between two adjacent lines.
 */
class VaultWiringSignatureTest {

    private static final Path LISTENERS = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "listener",
            "RpgListeners.java");

    private static final Path PLUGIN = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper",
            "RpgPlugin.java");

    private static final Path COMMAND = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "command",
            "RpgCommand.java");

    private static final String JOIN_DECLARATION = "public void onJoin(PlayerJoinEvent event) {";
    private static final String QUIT_DECLARATION = "public void onQuit(PlayerQuitEvent event) {";

    // --- the join and quit hooks --------------------------------------------------------------

    @Test
    void theVaultIsLOADEDOnJoin() throws IOException {
        List<String> lines = read(LISTENERS, 2000);

        int declaration = indexOfLineContaining(lines, JOIN_DECLARATION);
        assertTrue(declaration > 0, "the join handler must exist: " + JOIN_DECLARATION);

        int call = indexOfLineContaining(lines, "vaults.onJoin(");
        assertTrue(call > declaration,
                "NOTHING LOADS A VAULT. Without this call no player ever has one, every unit row in"
                        + " this slice still passes, and PR 1 has no screen that would show it.");
        assertTrue(call < declaration + 40,
                "the load must happen inside the join handler's own body, not somewhere later in"
                        + " the file that merely mentions it");
    }

    @Test
    void theVaultIsDROPPEDOnQuit() throws IOException {
        List<String> lines = read(LISTENERS, 2000);

        int declaration = indexOfLineContaining(lines, QUIT_DECLARATION);
        assertTrue(declaration > 0, "the quit handler must exist: " + QUIT_DECLARATION);

        int call = indexOfLineContaining(lines, "vaults.onQuit(");
        assertTrue(call > declaration && call < declaration + 40,
                "the cached vault must be dropped on quit, inside the quit handler, or every player"
                        + " who has ever logged in stays in the map until a restart");
    }

    /**
     * *** THE ABSENT SAVE ON QUIT IS A DECISION, AND THIS IS WHERE IT IS DEFENDED IN THE SOURCE. ***
     *
     * <p>Write-through means the file is already current, so a save here would be redundant -- and
     * worse, it would MASK a broken write-through by making an ordinary quit look correct on a build
     * whose per-mutation write did nothing. The obvious "tidy-up" is to add one, and it is wrong.
     */
    @Test
    void quitDoesNotSAVETheVault() throws IOException {
        List<String> lines = read(LISTENERS, 2000);
        int call = indexOfLineContaining(lines, "vaults.onQuit(");
        assertTrue(call > 0, "the quit call must exist for this row to mean anything");

        int declaration = indexOfLineContaining(lines, QUIT_DECLARATION);
        for (int i = declaration; i < Math.min(lines.size(), declaration + 40); i++) {
            String statement = lines.get(i).trim();
            if (statement.startsWith("//") || statement.startsWith("*")) continue;
            assertTrue(!statement.contains("vaults.saveAllAndClear")
                            && !statement.contains("vaults.writePage"),
                    "NO VAULT WRITE ON QUIT. Write-through already persisted every page, and a save"
                            + " here would make a build with a broken write-through look correct on"
                            + " every ordinary quit -- losing a page only on a crash, the one path"
                            + " nobody tests. Found: " + statement);
        }
    }

    // --- the shutdown flush -------------------------------------------------------------------

    /**
     * The flush must be ISSUED AND AWAITED before {@code storageIo} is shut down.
     *
     * <p>Ordering is the whole assertion. A vault whose final write is still queued when the
     * executor stops loses a page, and nothing about the shutdown says so.
     */
    @Test
    void theShutdownFlushRunsBEFORETheExecutorIsDrained() throws IOException {
        List<String> lines = read(PLUGIN, 600);

        int flush = indexOfLineContaining(lines, "vaults.saveAllAndClear()");
        assertTrue(flush > 0,
                "onDisable must flush the vaults. Without it a queued final write is dropped when"
                        + " the executor stops, and the page is gone with no error anywhere.");

        int drain = indexOfLineContaining(lines, "storageIo.shutdown();");
        assertTrue(drain > 0, "the executor drain must exist for this ordering to be checkable");
        assertTrue(flush < drain,
                "THE FLUSH MUST COME FIRST. Flushing after the executor has been shut down would"
                        + " queue work onto a stopped executor -- the write never runs, the future"
                        + " never completes, and the shutdown looks clean.");
    }

    @Test
    void theVaultRepositoryRidesTheSAMEExecutorAsTheProfiles() throws IOException {
        List<String> lines = read(PLUGIN, 600);

        int construction = indexOfLineContaining(lines, "new FileVaultRepository(");
        assertTrue(construction > 0, "the vault repository must be constructed in the plugin");

        String region = String.join("\n", lines.subList(construction,
                Math.min(lines.size(), construction + 3)));
        assertTrue(region.contains("storageIo"),
                "ONE serialised queue, shared with the profiles: a player's two files must not be"
                        + " written concurrently, and the existing drain then covers both. Found: "
                        + region);
    }

    // --- the dev command gate -----------------------------------------------------------------

    /**
     * *** UNGATED, {@code /rpg vault} IS AN ITEM DUPLICATOR. ***
     *
     * <p>{@code store} takes an item out of the world into a file and {@code take} puts one back.
     * The same exposure {@code /rpg playerxp} has, and the same node.
     *
     * <p>Asserted as ADJACENCY -- the line immediately after the literal -- because the comment
     * block above it names {@code Permissions.DEV} in prose, and a bare search would find that and
     * report the gate present after it had been deleted.
     */
    @Test
    void theVaultDevCommandIsGatedOnDEV() throws IOException {
        List<String> lines = read(COMMAND, 2000);

        int literal = indexOfLineContaining(lines, "Commands.literal(\"vault\")");
        assertTrue(literal > 0, "the /rpg vault subcommand must exist");

        String next = lines.get(literal + 1).trim();
        assertTrue(next.startsWith(".requires("),
                "the gate must be the VERY NEXT thing on the branch, so nothing can be inserted"
                        + " above it that runs ungated. Found: " + next);
        assertTrue(next.contains("Permissions.DEV"),
                "and it must be the DEV node. Found: " + next);
    }

    /**
     * The page argument's bounds come from {@code VaultShape}, not from literals, so Brigadier's
     * refusal cannot drift from the storage layer's own validation.
     */
    @Test
    void theCellArgumentsAreBoundedByTheSharedShapeRatherThanByLiterals() throws IOException {
        List<String> lines = read(COMMAND, 2000);

        int page = indexOfLineContaining(lines, "Commands.argument(\"page\"");
        assertTrue(page > 0, "the page argument must exist");
        assertTrue(lines.get(page).contains("VaultShape.PAGE_COUNT"),
                "bound the page on the shared constant, not on a 7 that can drift. Found: "
                        + lines.get(page).trim());

        // *** SEARCHED FROM THE PAGE LINE, NOT FROM THE TOP, AND THE FIRST DRAFT DID NOT. ***
        // `Commands.argument("slot"` occurs FIVE times in this file -- /rpg enchant has four of
        // them, all bounded by MAX_DEV_SLOT. Scanning from the top found one of THOSE and reported
        // the vault's slot argument unbounded. The text was real, the pattern was right, and it
        // belonged to somebody else: CLAUDE.md's false presence, from the instrument side.
        //
        // The page argument is unique (measured: one occurrence), so it is the anchor and the slot
        // is found relative to it.
        int slot = indexOfLineContaining(lines, "Commands.argument(\"slot\"", page);
        assertTrue(slot > page, "the vault's own slot argument must follow its page argument");
        assertTrue(slot < page + 3,
                "and must be the very next argument on the same branch, or this row is reading a"
                        + " different subcommand's slot");
        String slotRegion = String.join("\n", lines.subList(slot, Math.min(lines.size(), slot + 2)));
        assertTrue(slotRegion.contains("VaultShape.SLOTS_PER_PAGE"),
                "same for the slot. Found: " + slotRegion);
    }

    // --- helpers ------------------------------------------------------------------------------

    /**
     * Read a source file, and assert the scan actually found it.
     *
     * <p><b>The positive control, and it is not optional.</b> A wrong path returns an empty list,
     * every {@code indexOfLineContaining} then returns -1, and every assertion above would fail with
     * a message blaming the production code instead of the test. Worse, an assertion phrased the
     * other way round would PASS by default.
     */
    private static List<String> read(Path path, int atLeast) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertTrue(lines.size() > atLeast,
                "the scan must have read " + path + ", not an empty or wrong path -- finding"
                        + " nothing here would make every assertion below meaningless. Read "
                        + lines.size() + " lines, expected more than " + atLeast);
        return lines;
    }

    /** The first line containing {@code needle}, or -1. */
    private static int indexOfLineContaining(List<String> lines, String needle) {
        return indexOfLineContaining(lines, needle, 0);
    }

    /**
     * The first line at or after {@code from} containing {@code needle}, or -1.
     *
     * <p>The offset exists because several of these needles are NOT unique in their file -- four
     * other subcommands declare a {@code "slot"} argument. A scan from the top finds whichever comes
     * first, which is a different subcommand's line, and reports a perfectly true fact about the
     * wrong code.
     */
    private static int indexOfLineContaining(List<String> lines, String needle, int from) {
        for (int i = Math.max(0, from); i < lines.size(); i++) {
            if (isComment(lines.get(i))) continue;
            if (lines.get(i).contains(needle)) return i;
        }
        return -1;
    }

    /**
     * *** A COMMENTED-OUT CALL IS NOT A CALL, AND THE FIRST DRAFT COULD NOT TELL. ***
     *
     * <p>Found by mutation: commenting out {@code vaults.saveAllAndClear()} in {@code onDisable}
     * left the line still CONTAINING the needle, so the scan reported the flush present and the
     * shutdown row stayed green against a build that no longer flushed anything.
     *
     * <p>That is this project's false-presence rule arriving from the instrument side, and it is the
     * same shape as {@code grep "applies_status"} matching the prose that denies the key. In a
     * codebase where the commentary outweighs the code by an order of magnitude, an unfiltered scan
     * searches the commentary too.
     *
     * <p>Indices are NOT renumbered -- the real line number is still returned, so the adjacency
     * assertions above keep meaning what they say.
     */
    private static boolean isComment(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*");
    }

    /** Guards the helper itself: a needle that is certainly absent must report -1, not 0. */
    @Test
    void theScannerReportsAbsenceAsMinusOneRatherThanZero() {
        assertEquals(-1, indexOfLineContaining(List.of("alpha", "beta"), "gamma"));
        assertEquals(0, indexOfLineContaining(List.of("alpha", "beta"), "alpha"));
    }

    /** And the offset form really skips: the same needle, found twice, at two indices. */
    @Test
    void theScannerCanSkipAnEarlierMatchOfTheSameNeedle() {
        List<String> twice = List.of("slot here", "middle", "slot here");
        assertEquals(0, indexOfLineContaining(twice, "slot", 0));
        assertEquals(2, indexOfLineContaining(twice, "slot", 1),
                "without this, a needle that occurs in four other subcommands reads the wrong one");
        assertEquals(-1, indexOfLineContaining(twice, "slot", 3));
    }

    /**
     * *** THE CONTROL ON THE COMMENT FILTER, AND IT IS CAUSED RATHER THAN ASSERTED. ***
     *
     * <p>The filter is the difference between "this call exists" and "this text appears somewhere",
     * and a commented-out call is exactly how a deleted control keeps looking present. The fixture
     * stages all three comment shapes because the production files use all three.
     */
    @Test
    void theScannerIgnoresCommentedOutCode() {
        assertEquals(-1, indexOfLineContaining(
                List.of("        // vaults.saveAllAndClear();"), "vaults.saveAllAndClear()"),
                "a commented-out call must not read as a call");
        assertEquals(-1, indexOfLineContaining(
                List.of("     * vaults.saveAllAndClear() is the flush"), "vaults.saveAllAndClear()"),
                "nor must a javadoc line describing it");
        assertEquals(-1, indexOfLineContaining(
                List.of("        /* vaults.saveAllAndClear(); */"), "vaults.saveAllAndClear()"),
                "nor a block comment");
        assertEquals(0, indexOfLineContaining(
                List.of("        vaults.saveAllAndClear().get(15, SECONDS);"),
                "vaults.saveAllAndClear()"),
                "while the real statement is still found");
    }
}
