package io.github.butterflysmp.rpg.paper.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
 * {@code /rpg firerate} WRITES EVERY LINE IT SHOWS TO THE SERVER LOG TOO (the seat's brief of 2026-11-21, PR 1).
 *
 * <p>Why: {@code GATE-crystal-shard-staff.md}'s HF1 went UNREAD because the command's lines reached Ben's chat and
 * nowhere else, and the gate asked him to type them. Ben, verbatim: "i'm not typing the lines in chat just read them".
 * The seat ruled that the brief was at fault. Now each line is also logged as {@code FIRERATE player=<name> <line>}, so
 * a gate reads it from {@code latest.log}.
 */
class FireRateLogTest {

    /** The one helper sends the line to the player AND logs it, word for word, with the player's name. */
    @Test
    void sayShowsAndLogsTheSameText() {
        List<Component> chat = new ArrayList<>();
        List<String> log = new ArrayList<>();
        RpgCommand.say(chat::add, log::add, "BaronVonYeetus", "  INPUTS  count 12   window 176t   mean 16.00t   min 16t",
                NamedTextColor.GREEN);
        assertEquals(1, chat.size());
        assertEquals("  INPUTS  count 12   window 176t   mean 16.00t   min 16t",
                PlainTextComponentSerializer.plainText().serialize(chat.get(0)));
        assertEquals(NamedTextColor.GREEN, chat.get(0).color());
        assertEquals(List.of("FIRERATE player=BaronVonYeetus   INPUTS  count 12   window 176t   mean 16.00t   min 16t"), log);
    }

    /**
     * Every line {@code fireRate} shows goes through {@code say}: the method holds no bare {@code player.sendMessage},
     * and at least the eight lines it can print (the empty arm, the several-weapons line, the excluded line, the header, INPUTS, FIRES, the cooldown line,
     * the verdict, "Sample cleared") are {@code say} calls. Bounded STRUCTURALLY, at the next member declaration, never
     * by a line count.
     */
    @Test
    void everyLineFireRateShowsGoesThroughSay() throws IOException {
        String src = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/command/RpgCommand.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        int start = src.indexOf("private static int fireRate(");
        assertTrue(start > 0, "fireRate is not found: the scan is blind");
        int end = src.indexOf("\n    private static ", start + 1);
        int endPublic = src.indexOf("\n    static ", start + 1);
        if (endPublic > 0 && (end < 0 || endPublic < end)) end = endPublic;
        assertTrue(end > start, "no member after fireRate: the bound is blind");
        String body = src.substring(start, end);
        assertTrue(body.contains("cadence.clear(id);"), "CONTROL: the bound holds fireRate's own body");
        assertEquals(0, count(body, "player.sendMessage("), "a line shown to the player but not logged");
        assertTrue(count(body, "say(") >= 8, "fewer than eight lines go through say: " + count(body, "say("));
    }

    /**
     * The excluded count in words (2026-11-22, the seat's brief, PR 1): nothing for none, singular for one, plural
     * beyond. Forces red: printing "(0 ... kept out)" on a real sample, which reads as a figure about the sample.
     */
    @Test
    void theExcludedNoteIsWordsAndSilentAtZero() {
        assertEquals("", RpgCommand.excludedNote(0));
        assertEquals(" (1 selftest-driven record kept out of every sample)", RpgCommand.excludedNote(1));
        assertEquals(" (9 selftest-driven records kept out of every sample)", RpgCommand.excludedNote(9));
    }

    /**
     * The selftest's USE step fires its synthetic click INSIDE {@code FireCadence.excluding}, so the instrument's own
     * presses never reach a sample (2026-11-22, PR 1). Bounded at the {@code use(} driver's body.
     */
    @Test
    void theSelftestUseStepIsKeptOutOfTheCadence() throws IOException {
        String src = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/selftest/SelfTestDrivers.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        int start = src.indexOf("private Optional<String> use(Player p)");
        int end = src.indexOf("\n    private ", start + 1);
        assertTrue(start > 0 && end > start, "use( is not found: the scan is blind");
        String body = src.substring(start, end);
        assertTrue(body.contains("run.fireCadence().excluding(p.getUniqueId(), () -> Bukkit.getPluginManager().callEvent(event));"),
                "the synthetic click is wrapped");
        assertEquals(1, count(body, "callEvent("), "and it is the only call of the event, so none escapes the wrap");
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) n++;
        return n;
    }
}
