package io.github.butterflysmp.rpg.paper.menu;

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
 * *** WHAT THIS FILE PROVES, AND — MORE IMPORTANTLY — WHAT IT DOES NOT. ***
 *
 * <h2>IT PROVES THE CALL IS WRITTEN. IT NEVER PROVES THE DOOR WORKS.</h2>
 *
 * This is a SOURCE SCAN. {@code MUT13A-DOOR} edits source and this scanner reddens, which is
 * <b>close to tautological</b> — the mutation and the detector are looking at the same characters.
 * A green row here means the text is present, and nothing more.
 *
 * <p><b>THE BEHAVIOURAL GUARD IS ELSEWHERE, AND IT IS NAMED HERE SO NOBODY CITES THIS FILE FOR
 * IT:</b> {@code AnvilTransferTest}'s {@code NotScoreable} rows are what prove an item carrying no
 * score is refused, and refused for the RIGHT reason rather than as a key mismatch. If you are
 * asking "does the scoreable door work", read those; if you are asking "did somebody delete the
 * call", read this.
 *
 * <p>A row standing in as a mutation's substitute owes the same discrimination check as any other
 * row, and this one's is: <b>it discriminates a deleted call from a present one, and nothing
 * else.</b>
 *
 * <h2>WHY A SCAN AT ALL</h2>
 *
 * {@code AnvilMenu} cannot be constructed without a running server — {@code Bukkit.createInventory}
 * and a live {@code Player} — and {@code new ItemStack(...)} throws without a {@code RegistryAccess}.
 * There is no MockBukkit. So the wiring inside that class is reachable by nothing but a boot, and a
 * scan is the only mechanical check available. Same shape and same reason as
 * {@code GearScoreWiringSignatureTest} and {@code VaultWiringSignatureTest}.
 *
 * <h2>ANCHORED ON STATEMENTS, NEVER ON A BARE TOKEN</h2>
 *
 * {@code AnvilMenu}'s javadoc names {@code GearScore.carriesScore}, {@code inputSlots} and
 * {@code OUTPUT_SLOT} in prose repeatedly. <b>An unfiltered grep would match the paragraph
 * EXPLAINING a control and report it present after somebody deleted it</b> — and that is not
 * hypothetical here: while this slice's mutations were being run, an "original gone" check on the
 * bare needle {@code GearScore.carriesScore} reported <b>1 surviving occurrence</b> for a mutation
 * that had applied perfectly. The survivor was the javadoc. {@link #isComment} is why the rows
 * below cannot repeat that.
 */
class AnvilWiringSignatureTest {

    private static final Path ANVIL_MENU = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "menu",
            "AnvilMenu.java");

    private static final Path LISTENERS = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "listener",
            "RpgListeners.java");

    /**
     * *** THE COMPOSED DOOR, ANCHORED AT BOTH ENDS INCLUDING THE CLOSING PAREN. ***
     *
     * <p>{@code GearScore.carriesScore} composes TWO independent refusals — the kind-level rule and
     * the instance-level exception — and that class's javadoc names the failure exactly: <b>a silent
     * collapse from two refusals to one.</b> Asking only {@code scoreable(kind)} would let a
     * declared-unscored item into a transfer and hand its score away.
     *
     * <p><b>The needle carries its closing paren</b>, for the reason {@code GearScoreWiringSignatureTest}
     * learned the hard way: a needle stopping short is a PREFIX of the correct call and of every
     * future widening of it, so it keeps matching after the edit it exists to notice.
     */
    @Test
    void theSideGathererAsksTheCOMPOSEDDoorAndNotALiteral() throws IOException {
        List<String> lines = read(ANVIL_MENU, 200);

        int gatherer = indexOf(lines, "private Side sideOf(");
        assertTrue(gatherer > 0, "the one gathering method must exist");

        int composed = indexOf(lines,
                "GearScore.carriesScore(GearItems.gearClassOf(definition), declaredUnscored(definition))");
        assertTrue(composed > gatherer,
                "THE GATHERER MUST ASK THE COMPOSED DOOR, below its own declaration. Asking the "
                        + "kind-level rule alone lets a declared-unscored item become a Scored side "
                        + "and trade its roll away.");

        // AND IT MUST NOT PASS A LITERAL, which would satisfy the signature above while restoring
        // exactly the defect it was introduced to prevent. Both literals banned: `true` admits
        // everything, `false` refuses everything and would make the screen inert.
        assertEquals(-1, indexOf(lines,
                        "GearScore.carriesScore(GearItems.gearClassOf(definition), true)"),
                "a hardcoded true satisfies the composed signature and admits every tool");
        assertEquals(-1, indexOf(lines,
                        "GearScore.carriesScore(GearItems.gearClassOf(definition), false)"),
                "and a hardcoded false makes every item unscoreable, which looks like a broken "
                        + "screen rather than a missing guard");
        // Mutation MUT13A-DOOR: replace the composed call with `true` -> reddens here. SEE THE
        // CLASS JAVADOC: this proves the call is WRITTEN, not that the door WORKS.
    }

    /**
     * *** THE PREVIEW CELL IS NOT AN INPUT, AND {@code render()} DOES NOT TOUCH THE THREE ITEM
     * CELLS. ***
     *
     * <p>Both are unreachable by any unit test — one needs {@code returnEverything} on a live
     * inventory, the other needs a painted screen — and both are silent when wrong: the first hands
     * out a free copy of the player's item on every close, the second destroys their gear.
     */
    @Test
    void renderNEVERWritesAnItemCell_andTheOutputIsNotAnInputSlot() throws IOException {
        List<String> lines = read(ANVIL_MENU, 200);

        int inputSlots = indexOf(lines, "protected Set<Integer> inputSlots()");
        assertTrue(inputSlots > 0, "the input declaration must exist");
        int inputsReturn = indexOf(lines, "return AnvilMenuLayout.INPUT_SLOTS;", inputSlots);
        assertTrue(inputsReturn > inputSlots,
                "inputSlots() must return the layout's set, not a hand-built one that could grow "
                        + "the preview cell");

        int render = indexOf(lines, "private void render()");
        assertTrue(render > 0, "render must exist");
        int endOfRender = indexOf(lines, "private void refreshPreview()", render);
        assertTrue(endOfRender > render, "refreshPreview follows render -- the scan needs a bound");

        // STRUCTURALLY BOUNDED, never "within N lines": a line-count bound on a file whose methods
        // carry thirty lines of javadoc is an assertion about PROSE LENGTH, and it drifts without
        // anybody touching the code. CLAUDE.md records three false reds from exactly that.
        for (int i = render; i < endOfRender; i++) {
            if (isComment(lines.get(i))) continue;
            String line = lines.get(i);
            assertTrue(!line.contains("setItem(TARGET_SLOT") && !line.contains("setItem(DONOR_SLOT")
                            && !line.contains("setItem(OUTPUT_SLOT"),
                    "render() must never write an item cell -- painting over an input destroys the "
                            + "player's gear, and writing the preview here would race the refresh. "
                            + "Line " + (i + 1) + ": " + line.trim());
        }
    }

    /**
     * *** ALL THREE ANVIL MATERIALS ARE HIJACKED, AND TWO OF THEM ARE THE TRAP. ***
     *
     * <p>An anvil degrades as it is used, so {@code CHIPPED_ANVIL} and {@code DAMAGED_ANVIL} are
     * what a lived-in world mostly contains. Hijacking only the pristine one leaves the other two
     * <b>opening the vanilla screen</b> — the one screen the entry exists to replace — and it would
     * present as a bug that "sometimes" happens.
     */
    @Test
    void everyANVILMaterialIsHijacked_notJustThePristineOne() throws IOException {
        List<String> lines = read(LISTENERS, 1000);

        int table = indexOf(lines, "this.hijackedBlocks = Map.of(");
        assertTrue(table > 0, "the hijack table must exist");

        for (String material : List.of("Material.ANVIL,", "Material.CHIPPED_ANVIL,",
                "Material.DAMAGED_ANVIL,")) {
            int at = indexOf(lines, material, table);
            assertTrue(at > table,
                    material + " must be in the hijack table. A missing one opens VANILLA, and a "
                            + "damaged anvil is the common case in a world anyone has played in.");
            int opener = indexOf(lines, "new AnvilMenu(player", at);
            assertTrue(opener > at && opener <= at + 2,
                    material + " must be followed by an AnvilMenu opener on the next line or two");
        }

        // AND THE BLOCK-OPENED FORM CARRIES NO BREADCRUMB: opened from the world there is no hub in
        // the story, so 48 is bar rather than a Back button. The six-argument constructor is what
        // guarantees that -- there is no supplier to pass.
        assertEquals(-1, indexOf(lines, "new AnvilMenu(player, weapons, shields, armor, tools, adapters, () ->"),
                "a world anvil must not be given a hub supplier -- the constructor signature IS the "
                        + "origin discriminator");
    }

    // --- 13b: the confirm path -------------------------------------------------------------------
    //
    // *** EVERYTHING BELOW IS A SCAN, AND THE CLASS JAVADOC'S WARNING APPLIES TO ALL OF IT. ***
    // These rows prove the CALLS ARE WRITTEN. The behavioural guards are AnvilDecisionTest,
    // AnvilReconcileTest and AnvilButtonTest. They are here because AnvilMenu cannot be constructed
    // without a server, so the ORDER and PLACEMENT of statements inside it is reachable by nothing
    // else at all.

    /**
     * *** THE CONFIRM RECOMPUTES. IT DOES NOT SPEND WHAT WAS RENDERED. ***
     *
     * <p>The wallet is the only input to the decision that is not an input slot: XP can move while
     * the menu is open and nothing about that re-arms anything. <b>Re-evaluation is its only
     * guard.</b>
     */
    @Test
    void theConfirmRECOMPUTESTheDecisionAndReconcilesItAgainstWhatWasDisplayed() throws IOException {
        List<String> lines = read(ANVIL_MENU, 400);

        int confirm = indexOf(lines, "private void attemptTransfer()");
        assertTrue(confirm > 0, "the confirm handler must exist");

        int recompute = indexOf(lines, "AnvilDecision current = decide(target, donor);", confirm);
        assertTrue(recompute > confirm,
                "IT MUST RECOMPUTE. Spending the displayed decision makes the rendered preview the "
                        + "authority, and the wallet can have moved since it was drawn.");

        int reconcile = indexOf(lines, "AnvilReconcile.of(displayedDecision, current)", recompute);
        assertTrue(reconcile > recompute,
                "and it must RECONCILE the two, in that order -- displayed against current");

        // AND IT MUST NOT COMPARE THE RENDERED TEXT. lastConfirmText is a repaint-suppression cache
        // sitting in the same class; two different decisions can format identically.
        assertEquals(-1, indexOf(lines, "lastConfirmText.equals", confirm),
                "THE COMPARISON IS OVER THE DECISION, NEVER THE DISPLAY. Same target rarity means "
                        + "the same price, so a donor swapped for one of a different score renders "
                        + "the same string -- a false negative on the only irreversible action.");
    }

    /**
     * *** THE DEADLINE HAS TWO TRIGGERS AND EACH IS SCANNED SEPARATELY. ***
     *
     * <p>Dropping one leaves the other intact, so a single "is rearm called" row would vouch for
     * whichever half it happened to find.
     */
    @Test
    void theDeadlineReArmsOnAnINPUTChange_bothGestures() throws IOException {
        List<String> lines = read(ANVIL_MENU, 400);

        int moved = indexOf(lines, "if (click.itemMoved())");
        assertTrue(moved > 0, "the item-moved arm must exist");
        int armedOnClick = indexOf(lines, "rearm();", moved);
        assertTrue(armedOnClick > moved && armedOnClick < moved + 12,
                "a placement or a removal must restart the countdown, inside that arm");

        int drag = indexOf(lines, "protected void onDragPermitted()");
        assertTrue(drag > 0, "the drag hook must exist");
        int armedOnDrag = indexOf(lines, "rearm();", drag);
        assertTrue(armedOnDrag > drag && armedOnDrag < drag + 5,
                "and a permitted drag is an input change like any other");

        // AND rearm() IS SYNCHRONOUS, NOT INSIDE THE DEFERRED REPAINT. It touches an int and needs
        // no knowledge of the new contents; deferring it would let a click in that tick act on a
        // stale deadline.
        int deferred = indexOf(lines, "onEntityLater(viewer, this::refreshPreview, 1)", moved);
        assertTrue(armedOnClick < deferred,
                "rearm must come BEFORE the deferred repaint, not inside it");
    }

    /**
     * *** THE SECOND TRIGGER: A FACE THAT BECOMES ACTIONABLE STARTS THE CLOCK FROM ZERO. ***
     *
     * <p>Without it, a player looking at a RED cannot-afford button who kills a mob sees the face
     * become <b>LIME with no countdown</b>, and a click already in flight lands on an irreversible
     * action the arm never watched.
     *
     * <p><b>And it must be one-directional.</b> If a decision getting WORSE also re-armed, the
     * re-arm would catch the wallet-drop case as well as re-evaluation, and the two guards would
     * stop being separable.
     */
    @Test
    void theDeadlineReArmsWhenTheFaceBecomesACTIONABLE_andOnlyInThatDirection() throws IOException {
        List<String> lines = read(ANVIL_MENU, 400);

        int repaint = indexOf(lines, "private void refreshPreview()");
        assertTrue(repaint > 0, "the repaint must exist");

        int transition = indexOf(lines,
                "if (current.actionable() && (displayedDecision == null || !displayedDecision.actionable()))",
                repaint);
        assertTrue(transition > repaint,
                "ONE-DIRECTIONAL, AND THE CONDITION IS THE RULING. It must test that the NEW "
                        + "decision is actionable and the OLD one was not -- a symmetric test would "
                        + "re-arm on a decision getting worse and entangle the two guards.");

        int armed = indexOf(lines, "rearm();", transition);
        assertTrue(armed > transition && armed < transition + 4,
                "and the branch must actually restart the clock");
    }

    /**
     * *** THE WALLET IS POINTS-SYMMETRIC: READ ONCE, COMPUTE ONCE, WRITE ONCE. ***
     *
     * <p>Both existing spenders say why {@code giveExp(-n)} is banned -- it walks the levels down
     * through float accumulation inside NMS and bumps the EXPERIENCE scoreboard criterion -- and
     * {@code setLevel(getLevel() - levels)} silently discards the part-full bar.
     */
    @Test
    void theWalletWriteIsTheSYMMETRICPair_andTheBannedFormsAreAbsent() throws IOException {
        List<String> lines = read(ANVIL_MENU, 400);

        int confirm = indexOf(lines, "private void attemptTransfer()");
        int read = indexOf(lines, "XpCurve.totalPoints(viewer.getLevel(), viewer.getExp())", confirm);
        assertTrue(read > confirm, "the wallet is read from the two numbers on the player's screen");

        int level = indexOf(lines, "viewer.setLevel(XpCurve.levelFor(remaining));", read);
        int exp = indexOf(lines, "viewer.setExp(XpCurve.progressFor(remaining));", read);
        assertTrue(level > read, "and written back through XpCurve's exact inverse");
        assertTrue(exp == level + 1,
                "BOTH HALVES, ADJACENT. Writing the level without the bar loses the fraction, "
                        + "which is a point-per-purchase leak that nothing reports.");

        assertEquals(-1, indexOf(lines, "giveExp"), "giveExp is banned -- see both spenders");
        assertEquals(-1, indexOf(lines, "getTotalExperience"),
                "and getTotalExperience is never the input: it does not track spends");
        assertEquals(-1, indexOf(lines, "viewer.setLevel(viewer.getLevel()"),
                "nor setLevel(getLevel() - n), which discards the part-full bar");
    }

    /**
     * *** THE THREE WRITES ARE ADJACENT, AND NOTHING HOPS A SCHEDULER BETWEEN THEM. ***
     *
     * <p>{@code CraftingMenu} states the rule this rests on: an ordering argument is correct
     * <i>"only because nothing hops a scheduler in between … so nothing can move underneath
     * them."</i> A deferred call inserted between the stamp and the deduct would make the residual
     * table a work of fiction.
     */
    @Test
    void theTHREEWritesAreAdjacentWithNoSchedulerHopBetweenThem() throws IOException {
        List<String> lines = read(ANVIL_MENU, 400);

        int confirm = indexOf(lines, "private void attemptTransfer()");
        int stamp = indexOf(lines, "GearScoreItems.write(meta, ready.newScore()", confirm);
        int consume = indexOf(lines, "getInventory().setItem(DONOR_SLOT, null);", stamp);
        int deduct = indexOf(lines, "viewer.setLevel(XpCurve.levelFor(remaining));", consume);

        assertTrue(stamp > confirm, "1. the target is stamped");
        assertTrue(consume > stamp, "2. THEN the donor is consumed -- consume-first destroys an "
                + "item and upgrades nothing, which is the one residual with no visible trace");
        assertTrue(deduct > consume, "3. AND THE WALLET IS LAST -- the anvil CHARGES, so it takes "
                + "EnchantMenu's ordering, not the grindstone's refund ordering");

        // NOTHING FALLIBLE OR DEFERRED BETWEEN THEM.
        for (int i = stamp; i <= deduct; i++) {
            if (isComment(lines.get(i))) continue;
            assertFalse(lines.get(i).contains("onEntityLater") || lines.get(i).contains("onEntity("),
                    "NO SCHEDULER HOP between the three writes -- line " + (i + 1) + ": "
                            + lines.get(i).trim());
            assertFalse(lines.get(i).contains("say("),
                    "and nothing that can throw on a player's connection -- line " + (i + 1));
        }

        // AND THE RE-RENDER RIDES THE SAME editMeta AS THE WRITE, which is slice 12c's door: a
        // per-item value that is RENDERED has to be re-rendered wherever it is WRITTEN.
        int refresh = indexOf(lines, "GearItems.refreshLore(meta, definition, adapters);", stamp);
        assertTrue(refresh == stamp + 1,
                "the lore refresh must be the very next line, inside one editMeta");
    }

    /**
     * *** THE PREVIEW CELL IS WRITTEN AND NEVER READ. ***
     *
     * <p>13a's javadoc promised <i>"13b will not transfer by moving it"</i>. This is the slice that
     * could have falsified it, so the absence is asserted rather than assumed.
     */
    @Test
    void theOUTPUTCellIsNeverREADByTheConfirm_theClaim13aMade() throws IOException {
        List<String> lines = read(ANVIL_MENU, 400);

        assertEquals(-1, indexOf(lines, "getItem(OUTPUT_SLOT)"),
                "NOTHING READS THE PREVIEW. It is a readout that never becomes cargo, and a "
                        + "transfer that moved it would be handing the player a minted copy.");

        int confirm = indexOf(lines, "private void attemptTransfer()");
        int target = indexOf(lines, "getInventory().getItem(TARGET_SLOT);", confirm);
        assertTrue(target > confirm,
                "the confirm reads the TARGET cell -- the item it mutates in place");
    }

    // --- helpers, and their own controls ---------------------------------------------------------

    private static List<String> read(Path path, int atLeast) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertTrue(lines.size() > atLeast,
                "the scan must have read " + path + ", not an empty or wrong path -- finding "
                        + "nothing here would make every assertion below meaningless, and would "
                        + "make the absence rows pass by default. Read " + lines.size()
                        + " lines, expected more than " + atLeast);
        return lines;
    }

    private static int indexOf(List<String> lines, String needle) {
        return indexOf(lines, needle, 0);
    }

    /** The first line at or after {@code from} holding {@code needle} as CODE, or -1. */
    private static int indexOf(List<String> lines, String needle, int from) {
        for (int i = Math.max(0, from); i < lines.size(); i++) {
            if (isComment(lines.get(i))) continue;
            if (lines.get(i).contains(needle)) return i;
        }
        return -1;
    }

    private static boolean isComment(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*");
    }

    @Test
    void theScannerReportsAbsenceAsMinusOneRatherThanZero() {
        assertEquals(-1, indexOf(List.of("alpha", "beta"), "gamma"));
        assertEquals(0, indexOf(List.of("alpha", "beta"), "alpha"));
    }

    /**
     * *** THE CONTROL ON THE COMMENT FILTER, CAUSED RATHER THAN ASSERTED. ***
     *
     * <p>Every absence row above is satisfied by a filter that matches nothing at all. This stages
     * a needle that exists ONLY in commentary and requires the scanner to miss it — the positive
     * control that turns "it is not there" into "it is not there, and my instrument can find it
     * when it is".
     */
    @Test
    void theScannerSKIPSCommentary_whichIsTheOnlyReasonTheAbsenceRowsMeanAnything() {
        List<String> staged = List.of(
                "     * GearScore.carriesScore(GearItems.gearClassOf(definition), true)",
                "        // GearScore.carriesScore(GearItems.gearClassOf(definition), true)",
                "        int realCode = 1;");
        assertEquals(-1, indexOf(staged, "GearScore.carriesScore(GearItems.gearClassOf(definition), true)"),
                "a banned call named in javadoc or commented out is NOT a call -- without this the "
                        + "literal-ban rows would redden on their own explanation");

        // AND THE FILTER CAN STILL SEE CODE, or it would satisfy every absence row by being blind.
        assertEquals(2, indexOf(staged, "int realCode"),
                "the filter must skip commentary WITHOUT skipping everything");
    }
}
