package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NO TWO NOTICES MAY SHARE A THROTTLE BUCKET BY ACCIDENT.
 *
 * <h2>The defect this exists for, which nothing else in the suite could see</h2>
 *
 * <p>Every notice in this project throttles through the shared {@code CooldownTracker} under a
 * {@code __}-prefixed key. <b>Two refusals sharing a key silence each other</b>, and the silence
 * only appears when a player hits both inside one window -- so it is invisible to every test that
 * exercises one notice at a time, and invisible to a reader who is looking at one file.
 *
 * <p>{@code QuiverNotice.noAmmo} was given its own key in Slice E with the sequence written out:
 * <i>fire dry, get "empty", press reload, get "no arrows"</i>. {@link PlumeNotice} reaches the same
 * shape one weapon along and faster -- release on an empty magazine, left-click as instructed, the
 * reload eats the loose arrows, and the next draw does not start. <b>A shared bucket would swallow
 * the second message, which is the one the player needs most</b>, and the bug would present as
 * "the reload broke my bow".
 *
 * <p><b>None of that was enforced by anything before this row.</b> The keys were three separate
 * private constants in three files, and merging two of them -- or copy-pasting a file and forgetting
 * to rename its key -- compiled, passed, and shipped.
 *
 * <h2>THE SCAN HAS A POSITIVE CONTROL, BECAUSE A SCAN THAT FINDS NOTHING PASSES FOREVER</h2>
 *
 * <p>A reflective walk that matched no fields would assert distinctness over an empty set and be
 * green for the rest of the project's life. So the count is pinned: <b>finding fewer keys than exist
 * is a defect, not a quiet no-op.</b> Adding a notice is a deliberate edit to that number, which is
 * the moment to ask whether its key is its own.
 *
 * <h2>WHAT THIS DOES NOT CHECK</h2>
 *
 * <p>It cannot see keys that are equal by CONSTRUCTION rather than by literal -- a key built from a
 * weapon id at runtime, say. No such notice exists; if one is written, this row is where to widen.
 * And it says nothing about whether two notices SHOULD share: {@code QuiverNotice.reloadStarted} and
 * {@code reloading} deliberately share {@code RELOADING_KEY}, which is one CONSTANT used twice and
 * therefore invisible here. <b>That is the correct blind spot</b> -- the hazard is two constants
 * holding one value, not one constant honestly reused.
 */
class NoticeThrottleKeysTest {

    /**
     * Every class that throttles a player-facing notice. Named rather than discovered by package
     * scan: a list that must be edited is a list whose staleness is visible in a diff.
     */
    private static final List<Class<?>> NOTICE_CLASSES =
            List.of(PlumeNotice.class, QuiverNotice.class, BrokenNotice.class,
                    ShieldBrokenNotice.class,
                    // NOT IN THIS PACKAGE, AND THAT IS THE POINT OF A NAMED LIST RATHER THAN A
                    // PACKAGE SCAN. NexusCollisionNotice lives in ...paper.nexus beside the rest of
                    // the Nexus; a scan of ...paper.weapon would have missed it entirely and the
                    // count below would still have read 7 -- passing, over a set that had silently
                    // stopped being complete. The class javadoc's "a list that must be edited is a
                    // list whose staleness is visible in a diff" is what caught this.
                    io.github.butterflysmp.rpg.paper.nexus.NexusCollisionNotice.class,
                    io.github.butterflysmp.rpg.paper.build.StoneNotice.class);

    /**
     * The keys that exist today, counted. <b>This is the positive control</b>, and it is also the
     * line a new notice trips.
     *
     * <blockquote><b>IT CAUGHT ITS OWN AUTHOR ON ITS FIRST RUN, WHICH IS THE BEST EVIDENCE IT
     * WORKS.</b> This was written as <b>6</b>, counted off a {@code git grep} for {@code "__} whose
     * output had SEVEN lines -- one of which was {@code ShieldBrokenNotice}'s javadoc <i>mentioning</i>
     * {@code "__broken_weapon_notice"} in prose, and one of the seven constants therefore never
     * counted. <b>Grepping a string finds the commentary as well as the code</b>, which this repo
     * records as a false PRESENCE, and the reflective count is immune to it because it reads fields
     * rather than text. Corrected to 7 by running it.</blockquote>
     *
     * <blockquote><b>7 -&gt; 8 on 2026-09-15, for {@code NexusCollisionNotice}, and the bump was
     * WATCHED RATHER THAN ASSUMED.</b> The class was added to {@link #NOTICE_CLASSES} first and this
     * number left at 7 on purpose, to see the row go red: it reported <i>"the scan found 8 throttle
     * keys, not 7"</i> and listed all eight, which is what confirms the new key is both SEEN and
     * DISTINCT. Editing both in one go would have produced the same green suite whether the scan
     * had found the key or not.</blockquote>
     */
    private static final int KEYS_TODAY = 12;   // 8, plus StoneNotice's three (the Ability Stone slice), plus its empty-slot line (slice 2)

    @Test
    void everyThrottleKeyInTheProjectIsItsOwn() {
        List<String> keys = allThrottleKeys();

        assertEquals(KEYS_TODAY, keys.size(),
                "the scan found " + keys.size() + " throttle keys, not " + KEYS_TODAY + ". If you "
                        + "ADDED a notice, edit this number and check its key is its own -- that is "
                        + "the whole point of the edit. If the scan found FEWER, it has stopped "
                        + "seeing them and would pass over an empty set forever: " + keys);

        Set<String> distinct = new HashSet<>(keys);
        assertEquals(keys.size(), distinct.size(),
                "two notices share a throttle key, so one of them will be SILENT whenever a player "
                        + "hits both inside the window -- and only then, which is why nothing else "
                        + "catches it: " + keys);
    }

    /**
     * THE PLUME'S TWO KEYS SPECIFICALLY, ASSERTED BY NAME.
     *
     * <p>The row above would catch them being merged, but it would report it as "two notices share a
     * key" over a list of six. <b>This one names the pair and the sequence</b>, so the failure
     * message is the argument rather than a puzzle -- and it survives {@link #KEYS_TODAY} being
     * edited for an unrelated notice.
     */
    @Test
    void thePlumesTwoNoticesDoNotShareABucketWithEachOtherOrWithTheQuiversEmptyLine() throws Exception {
        String noRounds = keyValue(PlumeNotice.class, "NO_ROUNDS_KEY");
        String noArrow = keyValue(PlumeNotice.class, "NO_ARROW_KEY");
        String quiverEmpty = keyValue(QuiverNotice.class, "EMPTY_KEY");

        assertNotEquals(noRounds, noArrow,
                "THE SEQUENCE THIS BREAKS: release on an empty magazine -> noRounds says "
                        + "'left-click to reload' -> the reload EATS the player's loose arrows -> "
                        + "the next draw does not start -> noArrow is the message that explains it, "
                        + "and a shared bucket swallows it.");

        assertNotEquals(quiverEmpty, noRounds,
                "the Plume refuses AFTER a three-second hold; every other quiver weapon refuses "
                        + "before the shot. Sharing EMPTY_KEY couples two surfaces whose trigger "
                        + "paths have nothing in common.");
        assertNotEquals(quiverEmpty, noArrow);
    }

    /** Both messages must name a REMEDY -- "a refusal with no remedy reads as a bug". */
    @Test
    void bothPlumeNoticesNameWhatToDoAboutIt() throws Exception {
        // Asserted through the source rather than by sending a message, because a Player cannot be
        // constructed here. The strings are what a player reads, so the check is on the strings.
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "weapon",
                "PlumeNotice.java"), java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(source.contains("Left-click to reload."),
                "noRounds must name the remedy, like every notice in this project");
        assertTrue(source.contains("plain Arrow in your off-hand"),
                "noArrow must name the item EXACTLY and the SLOT exactly -- 'you need arrows' would "
                        + "read as broken to a player holding spectral arrows, and the off-hand is "
                        + "the whole content of R5 rather than a hint");
    }

    private static List<String> allThrottleKeys() {
        List<String> keys = new ArrayList<>();
        for (Class<?> type : NOTICE_CLASSES) {
            for (Field f : type.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) || f.getType() != String.class) continue;
                f.setAccessible(true);
                try {
                    Object value = f.get(null);
                    if (value instanceof String s && s.startsWith("__")) keys.add(s);
                } catch (IllegalAccessException e) {
                    throw new AssertionError("could not read " + type.getSimpleName() + "." + f.getName(), e);
                }
            }
        }
        return keys;
    }

    private static String keyValue(Class<?> type, String field) throws Exception {
        Field f = type.getDeclaredField(field);
        f.setAccessible(true);
        return (String) f.get(null);
    }
}
