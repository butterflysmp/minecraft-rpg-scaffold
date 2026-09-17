package io.github.butterflysmp.rpg.paper.progression;

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
 * The progression wiring the COMPILER CANNOT SEE, and the boot gate mostly will not either.
 *
 * <h2>*** WHY THIS FILE EXISTS: A BUILD WITH NO LISTENER PASSES ALMOST THE WHOLE GATE BLOCK ***</h2>
 *
 * Ben's warning, and it is the reason slice 9 needed a row that kills a mob. {@code /rpg playerxp}
 * makes every level threshold stageable in one line, so the gate block naturally reaches for it --
 * and <b>the command writes the profile directly, bypassing {@code PlayerExpChangeEvent}
 * entirely.</b> Delete the listener and every command-staged row still passes.
 *
 * <p>The gate answers that with ONE row that earns XP from a real orb. <b>This is the two-second
 * half of the same guard</b>, so the 60-second loop is not the only thing standing between a
 * deleted handler and a green build.
 *
 * <h2>WHY A SOURCE SCAN RATHER THAN A CALL</h2>
 *
 * {@code RpgListeners} needs a live {@code Player}, and the Brigadier tree needs a running server to
 * build at all -- neither is constructible here. <b>{@code QuiversSignatureTest} and
 * {@code SourceHygieneSignatureTest} set the precedent</b>: scan the source, compare against a named
 * list, fail by naming the file.
 *
 * <h2>AND EVERY ASSERTION ANCHORS ON CODE, NEVER ON A WORD</h2>
 *
 * The handler's javadoc names {@code PlayerExpChangeEvent} <b>seven times</b> -- it is a long
 * account of what does and does not raise the event. <b>A bare grep for that token would match the
 * prose and report the handler present after somebody deleted it</b>, which is CLAUDE.md's
 * false-presence row exactly: prose that names a key is indistinguishable from the key. So the
 * needle is the method DECLARATION, and the priority is read from the line above it.
 */
class ProgressionWiringSignatureTest {

    private static final Path LISTENERS = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "listener",
            "RpgListeners.java");

    private static final Path COMMAND = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "command",
            "RpgCommand.java");

    /** The declaration, not the type name -- see the class javadoc on false presence. */
    private static final String HANDLER_DECLARATION =
            "public void onPlayerExpChange(PlayerExpChangeEvent event) {";

    @Test
    void theXPHookEXISTSAndIsRegisteredAtLOWEST() throws IOException {
        List<String> lines = Files.readAllLines(LISTENERS, StandardCharsets.UTF_8);
        assertTrue(lines.size() > 1000,
                "the scan must have read RpgListeners, not an empty or wrong path -- "
                        + "finding nothing here would pass every assertion below by default");

        int declaration = indexOfLineContaining(lines, HANDLER_DECLARATION);
        assertTrue(declaration > 0,
                "THE XP HOOK IS GONE. Without it nothing converts a picked-up orb into player XP, "
                        + "and every gate row staged with /rpg playerxp still passes. "
                        + "Expected a line containing: " + HANDLER_DECLARATION);

        String annotation = lines.get(declaration - 1).trim();
        assertEquals("@EventHandler(priority = EventPriority.LOWEST)", annotation,
                "LOWEST is Ben's ruling: we record the amount the SERVER produced, before another "
                        + "plugin's multiplier, so progression is a function of what the player did "
                        + "rather than of what else is installed");

        // *** ignoreCancelled MUST NOT BE HERE, AND THAT IS A MEASUREMENT RATHER THAN A STYLE CALL.
        // It was briefed. PlayerExpChangeEvent is NOT Cancellable -- measured against the pinned
        // paper-api-26.1.2.build.74-stable, the chain is PlayerExpChangeEvent -> PlayerEvent ->
        // Event and none of the three implements it, against PlayerItemConsumeEvent on the same
        // instrument printing `implements org.bukkit.event.Cancellable`. Bukkit only consults the
        // flag for a Cancellable, so writing it would be a control credited with protecting
        // something it does not touch -- readable, plausible, and inert forever.
        assertFalse(annotation.contains("ignoreCancelled"),
                "PlayerExpChangeEvent is not Cancellable, so this flag would guard nothing. "
                        + "If you are adding it back, measure the event's interfaces first");
    }

    @Test
    void theHookIsONEForONE_withNoMultiplierBetweenTheEventAndTheProfile() throws IOException {
        List<String> lines = Files.readAllLines(LISTENERS, StandardCharsets.UTF_8);
        int declaration = indexOfLineContaining(lines, HANDLER_DECLARATION);
        assertTrue(declaration > 0, "the handler must exist for this row to mean anything");

        // *** THE STATEMENT, NOT THE BODY WINDOW -- AND THE FIRST DRAFT GOT THIS WRONG. ***
        // Scanning the eight lines after the declaration for `*` or `/` went RED on the handler's
        // own `// ONE FOR ONE` comment: the slash that opens a comment is a slash. A filter that
        // reads the commentary as well as the content, in a file where commentary outweighs code --
        // CLAUDE.md's false-presence row, arriving from the instrument side.
        int call = indexOfLineContaining(lines, "profiles.addLifetimeXp(");
        assertTrue(call > declaration && call < declaration + 8,
                "the handler must reach the one writer of lifetimeXp, inside its own short body");

        String statement = lines.get(call).trim();
        assertTrue(statement.contains("event.getAmount()"),
                "it must pass the event's OWN amount -- 1 vanilla XP is 1 player XP. Storing the "
                        + "raw number is what makes a curve retune cost nothing. Found: " + statement);
        assertFalse(statement.contains("*") || statement.contains("/"),
                "NO MULTIPLIER AND NO DIVISOR between the event and the profile. A scale factor "
                        + "here would be a second curve, invisible to PlayerLevel and to its tests. "
                        + "Found: " + statement);
    }

    @Test
    void playerxpIsGatedOnAnOPERATORNode_notLeftOpenToEveryone() throws IOException {
        List<String> lines = Files.readAllLines(COMMAND, StandardCharsets.UTF_8);
        int literal = indexOfLineContaining(lines, "Commands.literal(\"playerxp\")");
        assertTrue(literal > 0, "/rpg playerxp must be registered");

        // Brigadier's requires() gates the whole BRANCH, so it sits on the line after the literal.
        // Ungated, any player could set their own level to 99 -- the gate on the three hub stations
        // would then be decoration.
        assertTrue(lines.get(literal + 1).contains("Permissions.DEV"),
                "playerxp is operator tooling and DEV is this project's `default: op` node; "
                        + "found instead: " + lines.get(literal + 1).trim());
    }

    @Test
    void bothUNITSAreRegistered_becauseTheAmountIsMeaninglessWithoutOne() throws IOException {
        List<String> lines = Files.readAllLines(COMMAND, StandardCharsets.UTF_8);
        String source = String.join("\n", lines);

        assertTrue(source.contains("Commands.literal(\"xp\")"), "the xp unit");
        assertTrue(source.contains("Commands.literal(\"levels\")"), "the levels unit");
        assertTrue(source.contains("Commands.literal(\"level\")"),
                "and the singular, which is what people actually type -- the predecessor accepted "
                        + "it and dropping it would be a silent regression in the port");
    }

    /** First line index whose text contains {@code needle}, or -1. */
    private static int indexOfLineContaining(List<String> lines, String needle) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(needle)) return i;
        }
        return -1;
    }
}
