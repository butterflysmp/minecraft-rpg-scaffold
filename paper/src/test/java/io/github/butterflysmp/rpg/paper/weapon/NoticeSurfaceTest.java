package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE QUIVER FAMILY'S NOTICES GO TO THE ACTION BAR. A {@code sendMessage} AMONG THEM IS THE DEFECT.
 *
 * <h2>Why a row and not a convention</h2>
 *
 * <p>Ben's pick, 2026-09-23: a magazine state is transient, so it belongs where transient things go.
 * {@link QuiverNotice}'s four notices moved in this branch's first commit -- and
 * {@link PlumeNotice}'s two <b>were left in chat</b>, which nobody noticed until the boot, because
 * the two classes are exercised by different weapons and the split is invisible in either file alone.
 *
 * <p><b>That is the whole shape this row guards: a decision applied to one of two files.</b> Nothing
 * compiled differently, nothing failed, and the player-facing result was a Dragon's Plume that put
 * its refusals in two different places depending on which refusal it was. A copy-pasted notice class
 * reaching for the wrong method is the same edit one step later.
 *
 * <h2>*** SCOPED TO THE QUIVER FAMILY, AND THE OTHER THREE ARE UNRULED RATHER THAN EXCLUDED ***</h2>
 *
 * <p>Measured 2026-09-24: {@code BrokenNotice}, {@code ShieldBrokenNotice} and
 * {@code NexusCollisionNotice} each send <b>one</b> chat message, and <b>this row does not reach
 * them.</b>
 *
 * <p><b>Nobody has decided that those three should move.</b> Ben's pick was about the quiver's
 * notices, and the argument behind it does not obviously transfer -- a broken item stays broken, so
 * the state is not transient in the way an empty magazine is, and a permanent line may be right. <b>The
 * question was never put.</b>
 *
 * <p>Written as <i>unruled</i> and not as <i>excluded</i>, because the two are one keystroke apart and
 * only one is true: "excluded" says a decision was taken and closes the question, where this is still
 * open and worth putting. A settled-looking answer with no author is what this project keeps finding.
 *
 * <h2>What it cannot see</h2>
 *
 * <p>It reads SOURCE TEXT for the send method, so it cannot see a notice routed through a helper, or
 * one whose surface is chosen at runtime. No such notice exists; if one is written, this row is where
 * to widen. And it says nothing about the WORDS -- {@code NoticeThrottleKeysTest} owns the keys, the
 * gate rows own what a player actually sees.
 */
class NoticeSurfaceTest {

    /**
     * The quiver family, by file. <b>Named rather than found by package scan</b>, for
     * {@code NoticeThrottleKeysTest}'s reason: a list that must be edited is a list whose staleness is
     * visible in a diff, and a package scan would silently absorb the three unruled classes above.
     */
    private static final List<String> QUIVER_FAMILY = List.of("PlumeNotice.java", "QuiverNotice.java");

    /** Every action-bar send in the family. <b>The positive control</b>: 2 in PlumeNotice, 4 in QuiverNotice. */
    private static final int ACTION_BAR_SENDS_TODAY = 6;

    @Test
    void theQuiverFamilySpeaksOnTheActionBarAndNeverInChat() throws IOException {
        List<String> chatSenders = new ArrayList<>();
        int actionBarSends = 0;

        for (String name : QUIVER_FAMILY) {
            Path file = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper",
                    "weapon", name);
            assertTrue(Files.isRegularFile(file),
                    name + " is not where this row looks: " + file.toAbsolutePath() + ". A renamed or "
                            + "moved notice class leaves this row scanning nothing and passing forever.");

            // COMMENTS STRIPPED, and it is not optional here. Both files now carry prose ARGUING for
            // the action bar, and PlumeNotice's block names sendMessage three times while explaining
            // which classes still use it. An unstripped scan would report the file that documents the
            // rule as the file that breaks it -- the false-presence family, exactly as a grep for
            // "applies_status" once found the comment denying the key.
            String code = Files.readString(file, StandardCharsets.UTF_8)
                    .replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");

            if (code.contains("sendMessage(")) chatSenders.add(name);
            actionBarSends += countOf(code, "sendActionBar(");
        }

        // *** THE CHAT ASSERTION RUNS FIRST AND THE CONTROL SECOND, WHICH IS THE OPPOSITE OF THE USUAL
        // ORDER AND IS ARGUED FOR RATHER THAN DEFAULTED TO. ***
        //
        // The standing rule is that a positive control precedes an ABSENCE assertion, because a scan
        // that read nothing finds no sendMessage either and the absence becomes a fact about the scan.
        // That rule is satisfied here by POSITION-INDEPENDENCE, not by ordering: both assertions are in
        // one method, so a blind scan fails the method whichever runs first -- only the MESSAGE changes.
        //
        // And the two cases cannot overlap. A blind scan reports ZERO chat senders, so the chat
        // assertion PASSES and control reaches the count, which then catches it. A chat send that is
        // really there proves the scan is reading the file, so blindness is not the diagnosis. There is
        // no state in which the chat assertion fails FOR a blind scan.
        //
        // MEASURED, which is why this order was chosen: under MUT-PLUMECHAT (revert PlumeNotice's two
        // sendActionBar calls to sendMessage) the count-first order reported "the scan found 4, not 6"
        // -- true, and it reads as the instrument breaking rather than as the notice moving. The most
        // specific diagnosis available goes first.
        assertFalse(chatSenders.contains("PlumeNotice.java"),
                "PlumeNotice is back in chat. Its noRounds and noArrow fire in sequence within a few "
                        + "seconds -- release dry, reload on that advice, draw with no arrow left -- so "
                        + "chat collects two permanent lines about one fumble. Ben's action-bar pick, "
                        + "2026-09-23, covers these two.");

        assertEquals(List.of(), chatSenders,
                "a quiver-family notice sends to CHAT. A magazine state is transient and belongs on "
                        + "the action bar; QuiverNotice moved on 2026-09-23 and PlumeNotice was left "
                        + "behind, which is the split this row exists for. Offenders: " + chatSenders
                        + "\nNOTE the scope: BrokenNotice, ShieldBrokenNotice and NexusCollisionNotice "
                        + "still use sendMessage and are UNRULED, not excluded -- do not move them to "
                        + "satisfy this row, and do not add them to it without a ruling.");

        // THE POSITIVE CONTROL. Pinning the action-bar count is what makes the absence above mean
        // something: it proves the scan can SEE the sends it is meant to be discriminating between. A
        // wrong path, a stripper that ate the file, or a rename all land here.
        assertEquals(ACTION_BAR_SENDS_TODAY, actionBarSends,
                "the scan found " + actionBarSends + " action-bar sends in the quiver family, not "
                        + ACTION_BAR_SENDS_TODAY + " (PlumeNotice 2, QuiverNotice 4). If you ADDED a "
                        + "notice, edit this number. If the scan found FEWER and the chat assertions "
                        + "above passed, it has stopped seeing the sends -- those assertions were then "
                        + "passing over an empty read and prove nothing.");
    }

    /**
     * THE THREE OUTSIDE THE FAMILY ARE STILL IN CHAT, ASSERTED SO THE SCOPE ABOVE IS A MEASUREMENT.
     *
     * <p><b>This row does not say they are right.</b> It says the claim in the class javadoc -- <i>three
     * notices still send to chat, and nobody has ruled on them</i> -- is true of the tree, so a reader
     * meeting that scope can trust it rather than re-grepping.
     *
     * <p><b>AND IT IS WHAT MAKES THE SCOPE FALSIFIABLE.</b> If somebody sweeps those three to the
     * action bar, this row goes red and its message asks them to record the ruling that authorised it.
     * <b>A scope note with nothing checking it decays into a description of a tree that has moved</b> --
     * which is how "unruled" quietly becomes "excluded" with no author.
     */
    @Test
    void theThreeUnruledNoticesAreStillInChatAndThatIsRecordedNotEndorsed() throws IOException {
        List<String> stillChat = new ArrayList<>();
        for (String relative : List.of("weapon/BrokenNotice.java", "weapon/ShieldBrokenNotice.java",
                                       "nexus/NexusCollisionNotice.java")) {
            Path file = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper")
                    .resolve(relative);
            assertTrue(Files.isRegularFile(file), "not found: " + file.toAbsolutePath());
            String code = Files.readString(file, StandardCharsets.UTF_8)
                    .replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
            if (code.contains("sendMessage(")) stillChat.add(file.getFileName().toString());
        }

        assertEquals(
                List.of("BrokenNotice.java", "ShieldBrokenNotice.java", "NexusCollisionNotice.java"),
                stillChat,
                "the three notices outside the quiver family no longer all send to chat. That may be "
                        + "right -- but it is a RULING nobody has recorded, and the scope note on this "
                        + "class calls them UNRULED on the strength of this reading. If Ben has now "
                        + "ruled on them, move them into QUIVER_FAMILY's list (renaming it) and write "
                        + "the ruling down with its date. Found in chat: " + stillChat);
    }

    private static int countOf(String haystack, String needle) {
        int count = 0;
        for (int at = haystack.indexOf(needle); at >= 0; at = haystack.indexOf(needle, at + 1)) count++;
        return count;
    }
}
