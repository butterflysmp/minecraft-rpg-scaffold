package io.github.butterflysmp.rpg.paper.vault;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    // --- PR 2: the screen, the hijack and the migration ---------------------------------------

    private static final Path VAULT_MENU = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "menu",
            "NexusVaultMenu.java");

    /**
     * *** THE ENDER CHEST IS HIJACKED, AND NOTHING ELSE IN THE SUITE CAN SEE IT. ***
     *
     * <p>Delete this map entry and the vault still works from the hub, every unit row stays green,
     * and the only symptom is that a right-clicked ender chest opens the VANILLA screen -- into
     * which a player then puts items that the migration has already copied. <b>Two containers, both
     * writable, holding the same stacks.</b>
     *
     * <p>Anchored on the {@code Material.ENDER_CHEST} line and the opener directly beneath it,
     * because a bare {@code ENDER_CHEST} matches this slice's own commentary about the block.
     */
    @Test
    void theEnderChestIsHIJACKEDIntoTheVaultScreen() throws IOException {
        List<String> lines = read(LISTENERS, 2000);

        int material = indexOfLineContaining(lines, "Material.ENDER_CHEST,");
        assertTrue(material > 0,
                "the ender chest must be in hijackedBlocks, or the vanilla chest stays reachable"
                        + " beside a vault that has already copied its contents");

        int opener = indexOfLineContaining(lines, "new NexusVaultMenu(", material);
        assertTrue(opener > material && opener <= material + 2,
                "the opener must sit directly under the material it is keyed to; a NexusVaultMenu"
                        + " built somewhere else in the file is a different route");
    }

    /**
     * *** THE WRITE HOPS. NOTHING IN onClick MAY WRITE SYNCHRONOUSLY. ***
     *
     * <p>This is the slice's headline defect and it is invisible to every unit row: a synchronous
     * write reads the cells one tick early, which DESTROYS on a put-in and DUPLICATES on a
     * take-out. The screen cannot be constructed in a test, so the scan pins the shape instead --
     * {@code scheduleWrite} is reached from {@code onClick}, and the only call to
     * {@code writeCurrentPage} in that method is inside a scheduled task.
     */
    @Test
    void theClickPathSchedulesTheWriteRatherThanWritingInline() throws IOException {
        List<String> lines = read(VAULT_MENU, 400);

        int onClick = indexOfLineContaining(lines, "protected void onClick(MenuClick click) {");
        assertTrue(onClick > 0, "onClick must exist");

        int scheduled = indexOfLineContaining(lines, "scheduleWrite();", onClick);
        assertTrue(scheduled > onClick && scheduled < onClick + 20,
                "onClick must hand off to scheduleWrite before any button branch");

        int inside = indexOfLineContaining(lines, "scheduleWrite()", 0);
        int declaration = indexOfLineContaining(lines, "private void scheduleWrite() {");
        int hop = indexOfLineContaining(lines, "adapters.scheduler().onEntity(viewer", declaration);
        int write = indexOfLineContaining(lines, "writeCurrentPage()", declaration);
        assertTrue(inside > 0 && declaration > 0, "scheduleWrite must be declared and called");
        assertTrue(hop > declaration && hop < write,
                "the scheduler hop must come BEFORE the write inside scheduleWrite -- a write above"
                        + " it reads the slots one tick early, which is the whole defect");
    }

    /** The drag path is the same defect, and the draft that was withdrawn did not mention it. */
    @Test
    void theDragPathSchedulesTheWriteToo() throws IOException {
        List<String> lines = read(VAULT_MENU, 400);

        int hook = indexOfLineContaining(lines, "protected void onDragPermitted() {");
        assertTrue(hook > 0, "onDragPermitted must be overridden: a permitted drag changes the"
                + " contents and dispatches nothing else");

        int scheduled = indexOfLineContaining(lines, "scheduleWrite();", hook);
        assertTrue(scheduled > hook && scheduled < hook + 4,
                "and it must schedule, not write inline -- that hook's own javadoc says the"
                        + " contents have NOT changed yet when it is called");
    }

    /**
     * *** THE MIGRATION STAMPS ONLY AFTER THE ITEMS ARE WRITTEN. ***
     *
     * <p>Reverse these two and a crash in between leaves a player stamped as migrated with an empty
     * page 1 -- their chest still full, and <b>nothing will ever copy it again.</b> The order is the
     * whole guarantee, and it is one line's distance from being wrong.
     */
    @Test
    void theMigrationWritesTheItemsBeforeItStampsTheProfile() throws IOException {
        List<String> lines = read(VAULT_MENU, 400);

        int method = indexOfLineContaining(lines, "private void migrateIfDue() {");
        assertTrue(method > 0, "migrateIfDue must exist");

        int write = indexOfLineContaining(lines, "writeCurrentPage()", method);
        int stamp = indexOfLineContaining(lines, "profiles.setVaultMigrated(", method);

        assertTrue(write > method, "the migration must write the page");
        assertTrue(stamp > method, "and must stamp the profile");
        assertTrue(write < stamp,
                "THE WRITE MUST COME FIRST. Stamping first and crashing leaves a player marked"
                        + " migrated with an empty page 1 and a full ender chest they cannot open");
    }

    /**
     * *** THE MIGRATION IS NOT LEVEL-GATED, AND THE ABSENCE IS THE RULING. ***
     *
     * <p>Page 1 became free on 2026-09-18, so there is nothing left to wait for -- and <b>deferring
     * would now be actively harmful rather than merely pointless.</b> A level-5 player fills page 1,
     * plays for weeks, and a migration deferred to level 20 then copies 27 stacks into a page that
     * is no longer empty: {@code VaultMigrationPlan} skips occupied cells, so most of the chest
     * would be silently left behind in a container the hijack has made unreachable.
     *
     * <p><b>An absent guard cannot be asserted by behaviour here</b> -- the screen needs a server --
     * so the scan pins that {@code migrateIfDue} consults no threshold. Re-adding one is the kind of
     * edit that reads like tightening a check.
     */
    @Test
    void theMigrationConsultsNoLevelThreshold() throws IOException {
        List<String> lines = read(VAULT_MENU, 400);

        int method = indexOfLineContaining(lines, "private void migrateIfDue() {");
        assertTrue(method > 0, "migrateIfDue must exist");
        int end = indexOfMemberDeclarationAfter(lines, method);

        for (int i = method; i < end; i++) {
            assertFalse(lines.get(i).contains("VaultPageGate.unlocked")
                            || lines.get(i).contains("VaultPageGate.isFree")
                            || lines.get(i).contains("HUB_SHORTCUT_LEVEL"),
                    "migrateIfDue must consult NO threshold -- page 1 is free and a deferred"
                            + " migration copies into a page the player has already filled. Found: "
                            + lines.get(i).trim());
        }

        // AND THE STAMP IS STILL THERE, because it is still the whole mechanism: an empty page 1 is
        // indistinguishable from a migrated-empty ender chest.
        int stamp = indexOfLineContaining(lines, "profiles.setVaultMigrated(", method);
        assertTrue(stamp > method && stamp < end,
                "the stamp stays -- without it the copy re-runs on every open");
    }

    /**
     * The opt-out is CONDITIONAL, and the condition is delegated to something testable.
     *
     * <p>{@code returnedSlots} is what stops the vault handing its contents back, and the degraded
     * arm is what stops it being a shredder when a write fails. Neither can be executed here, so
     * this pins that the decision is not inlined -- {@code VaultReturnPolicy} is where both answers
     * are actually exercised.
     */
    @Test
    void theReturnOptOutDelegatesToThePolicyRatherThanInliningIt() throws IOException {
        List<String> lines = read(VAULT_MENU, 400);

        int override = indexOfLineContaining(lines, "protected Set<Integer> returnedSlots() {");
        assertTrue(override > 0, "the vault must override returnedSlots, or it hands storage back");

        int delegate = indexOfLineContaining(lines, "VaultReturnPolicy.returnedSlots(", override);
        assertTrue(delegate > override && delegate < override + 8,
                "the answer must come from VaultReturnPolicy, which is the only place both the"
                        + " healthy and the degraded answer can be run in a unit test");
    }

    /**
     * *** onClose FLUSHES SYNCHRONOUSLY, AND IT IS THE ONLY PATH THAT SURVIVES A QUIT. ***
     *
     * <p>A gesture schedules its write for the next tick. **{@code PaperScheduler.onEntity} passes
     * `retired` as null**, so Paper drops an entity task whose entity is gone -- a player who
     * disconnects inside that tick never has the task run at all. Without this flush the item is
     * written nowhere, and {@code returnedSlots()} is empty while healthy, so it is handed back to
     * nobody either.
     *
     * <p><b>The ORDER is the other half.</b> {@code returnEverything} CLEARS the cells it returns,
     * so a flush after it would write an empty page over a full one.
     */
    @Test
    void theCloseFlushesBEFOREItHandsAnythingBack() throws IOException {
        List<String> lines = read(VAULT_MENU, 400);

        int onClose = indexOfLineContaining(lines, "protected void onClose(InventoryCloseEvent");
        assertTrue(onClose > 0, "onClose must exist");

        int write = indexOfLineContaining(lines, "writeCurrentPage()", onClose);
        int giveBack = indexOfLineContaining(lines, "returnEverything();", onClose);

        // *** BOUNDED BY THE NEXT MEMBER DECLARATION, NOT BY A LINE COUNT. ***
        //
        // This assertion was `onClose + 30`, then `+ 60`, and failed BOTH times on a comment rather
        // than on a defect -- once when the UNWITNESSED note was added, once when the disconnect
        // drop was. A proximity bound in this codebase silently becomes a COMMENT-LENGTH assertion:
        // the instrument is correct and its SCOPE drifts under it as prose accumulates.
        //
        // What the row claims is structural -- these two statements are in THIS method, in THIS
        // order -- so it is measured structurally.
        int endOfMethod = indexOfMemberDeclarationAfter(lines, onClose);
        assertTrue(endOfMethod > onClose, "onClose must be followed by another member to bound it");

        assertTrue(write > onClose && write < endOfMethod,
                "onClose must flush the page synchronously -- the scheduled write is DROPPED on a"
                        + " quit, so this is the only path that covers it");
        assertTrue(giveBack > onClose && giveBack < endOfMethod,
                "and it must still hand back what it owes, in the same method");
        assertTrue(write < giveBack,
                "THE WRITE MUST COME FIRST. returnEverything clears the cells it returns, so"
                        + " flushing afterwards writes an empty page over a full one.");
    }

    /**
     * The degraded close hands back the DIFFERENCE, not the page.
     *
     * <p>Poisoning leaves the file at its last good copy, so returning every cell duplicates
     * whatever the file still has -- **a full-page duplicator**, not the single-cursor residual the
     * design argues is irreducible. The subtraction runs in {@code VaultReturnPolicy}, which is
     * where both answers are executed; this pins that the screen actually supplies the second set
     * rather than passing an empty one.
     */
    @Test
    void theDegradedReturnSubtractsWhatIsAlreadyOnDisk() throws IOException {
        List<String> lines = read(VAULT_MENU, 400);

        int override = indexOfLineContaining(lines, "protected Set<Integer> returnedSlots() {");
        assertTrue(override > 0, "the vault must override returnedSlots");

        int delegate = indexOfLineContaining(lines, "VaultReturnPolicy.returnedSlots(", override);
        assertTrue(delegate > override && delegate < override + 8, "and must delegate the decision");
        assertTrue(lines.get(delegate).contains("alreadyOnDisk()"),
                "the THIRD argument must be the live disk comparison, not an empty set: "
                        + lines.get(delegate));

        int compare = indexOfLineContaining(lines, "private Set<Integer> alreadyOnDisk() {");
        assertTrue(compare > 0, "and that comparison must exist");
        int persisted = indexOfLineContaining(lines, "vaults.persistedPage(", compare);
        assertTrue(persisted > compare && persisted < compare + 12,
                "it must read what is ON DISK, not the cache -- the cache is what the failed write"
                        + " intended and is exactly the thing that is wrong");
    }

    /**
     * The next member declaration after {@code from} -- a method or field at class-body indentation.
     *
     * <h2>*** THE STRUCTURAL REPLACEMENT FOR A PROXIMITY BOUND, AND IT EXISTS BECAUSE ONE DRIFTED ***</h2>
     *
     * A bound of "within N lines" answers <i>are these close together</i>, which in a file whose
     * methods carry thirty lines of javadoc each is a question about PROSE. It failed twice on
     * comments that were added, both times reporting a defect that was not there -- and once during
     * a mutation run, where it was nearly attributed to the mutation.
     *
     * <p>Four spaces then a modifier is the class-body member indentation this project uses
     * throughout. Comments are skipped by {@link #isComment}, so a javadoc line beginning with
     * {@code *} cannot end a method early.
     */
    private static int indexOfMemberDeclarationAfter(List<String> lines, int from) {
        for (int i = from + 1; i < lines.size(); i++) {
            if (isComment(lines.get(i))) continue;
            String line = lines.get(i);
            if (line.startsWith("    private ") || line.startsWith("    protected ")
                    || line.startsWith("    public ") || line.startsWith("    static ")) {
                return i;
            }
        }
        return lines.size();
    }

    /**
     * The bound helper finds the next member and is not fooled by a javadoc line.
     *
     * <p>A control on the instrument, for the reason the whole file exists: a helper that returned
     * {@code from + 1} for everything would make every ordering assertion above vacuous.
     */
    @Test
    void theMemberBoundSkipsCommentsAndFindsTheNextDeclaration() {
        List<String> source = List.of(
                "    protected void first() {",
                "        doThing();",
                "    }",
                "",
                "    /**",
                "     * private void notReallyADeclaration()",
                "     */",
                "    private void second() {");

        assertEquals(7, indexOfMemberDeclarationAfter(source, 0),
                "the javadoc line quoting a declaration must not end the method early");
        assertEquals(source.size(), indexOfMemberDeclarationAfter(source, 7),
                "and running off the end reports the end, rather than -1 into an assertion");
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
