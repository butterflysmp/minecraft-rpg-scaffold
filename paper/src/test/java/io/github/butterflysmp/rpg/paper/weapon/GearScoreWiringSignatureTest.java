package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * *** WHY THIS FILE EXISTS: THE WHOLE SUITE PASSES ON A BUILD THAT SCORES NOTHING. ***
 *
 * <p>{@code GearScoreTest} drives the arithmetic directly, so it is green whether or not anything in
 * the plugin ever stamps an item, carries a stamp across a re-mint, or scales a single point of
 * damage. Delete {@code GearScoreItems.carry} from {@code GearItems.carryInstanceData} and every one
 * of those rows still passes; the only symptom is that <b>every scored item in the game silently
 * reverts to 100 on the player's next login</b>.
 *
 * <p>Same shape, and the same reason, as {@code VaultWiringSignatureTest} and
 * {@code ProgressionWiringSignatureTest}: a source scan, compared against a named list, failing by
 * naming the file. Every method this guards needs a live {@code Player}, an {@code ItemStack} or a
 * running server to build the Brigadier tree -- none constructible here, and there is no MockBukkit.
 *
 * <h2>ANCHORED ON STATEMENTS AND ADJACENCY, NEVER ON A BARE TOKEN</h2>
 *
 * The files this reads name {@code GearScore}, {@code gearScore} and {@code Permissions.DEV} in prose
 * constantly -- this slice's javadocs are the densest commentary in it. <b>A bare grep would match the
 * paragraph EXPLAINING a control and report it present after somebody deleted it</b>, which is
 * CLAUDE.md's false-presence rule arriving from the instrument side. So {@link #isComment} filters
 * commentary out and every assertion pins a STATEMENT or the relationship between two adjacent lines.
 *
 * <p><b>That filter is not theoretical here. It was earned twice in one slice.</b> While this slice was
 * being written, a marker grep for {@code OptionalInt averageScore) &#123;} reported a signature
 * widening as applied; it had matched a DIFFERENT method's parameter list, and the widening had
 * silently no-opped. The same class of error, from the same cause.
 */
class GearScoreWiringSignatureTest {

    private static final Path COMMAND = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "command",
            "RpgCommand.java");

    private static final Path GEAR_ITEMS = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "weapon",
            "GearItems.java");

    private static final Path SCORE_ITEMS = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "weapon",
            "GearScoreItems.java");

    private static final Path DEFENSE = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "health",
            "DefenseModifierItems.java");

    private static final Path ATTACK = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "weapon",
            "WeaponAttackItems.java");

    private static final Path WEAPON_ITEMS = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "weapon",
            "WeaponItems.java");

    private static final Path CRAFT = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "menu",
            "InventoryCraft.java");

    // --- the carry, which is the quietest failure in the slice -------------------------------

    /**
     * *** WITHOUT THIS LINE, EVERY SCORED ITEM IN THE GAME REVERTS TO 100 ON THE NEXT LOGIN. ***
     *
     * <p>And it reverts to a LEGAL score, so nothing throws, nothing is red, and the tooltip simply
     * loses a line. That is quieter than a lost magazine (which renders as an unstamped defect) or
     * lost enchants (which a player notices at once), and a re-mint happens on every join.
     */
    @Test
    void theGearScoreIsCARRIEDAcrossAReMint() throws IOException {
        List<String> lines = read(GEAR_ITEMS, 150);

        int declaration = indexOf(lines, "public static void carryInstanceData(");
        assertTrue(declaration > 0, "the one carry method must exist");

        int call = indexOf(lines, "GearScoreItems.carry(from, to, keys);");
        assertTrue(call > declaration && call < declaration + 15,
                "NOTHING CARRIES THE SCORE. Every scored item reverts to GearScore.ABSENT on the"
                        + " player's next login -- silently, to a legal value, with the whole suite"
                        + " still green. It must be inside carryInstanceData's own body.");
    }

    // --- the two scaling seams ----------------------------------------------------------------

    @Test
    void weaponDamageIsSCALEDByTheHeldItemsScore() throws IOException {
        List<String> lines = read(ATTACK, 60);
        assertTrue(indexOf(lines, "GearScore.scaledDamage(weapon.attackDamage(), score)") > 0,
                "the MAIN_HAND modifier must be the SCALED damage. Without this the stat converges to"
                        + " the authored figure and no weapon in the game scales, while the tooltip"
                        + " says it does.");
        assertTrue(indexOf(lines, "GearScoreItems.heldScore(player, keys)") > 0,
                "and the score must come from the item in hand, not from the definition");
    }

    @Test
    void armourDefenseIsSCALEDPerPiece() throws IOException {
        List<String> lines = read(DEFENSE, 150);
        assertTrue(indexOf(lines, "GearScore.scaledDefense(vanilla,") > 0,
                "each piece's material points must be scaled by that piece's own score");
    }

    /**
     * *** THE NATIVE SUM ACCUMULATES BEFORE THE SCALE, AND THE ORDER IS THE WHOLE ASSERTION. ***
     *
     * <p>{@code nativeArmor} is what the vanilla {@code armor} attribute LITERALLY HOLDS, and
     * {@code ArmorBarOverride} cancels exactly that before refilling the bar from damage reduction.
     * Accumulate the SCALED number instead and the cancellation over-subtracts, the attribute lands
     * negative, and Minecraft clamps it to zero: <b>the armour bar reads EMPTY on the most-armoured
     * player in the game</b>, while the stat, the mitigation and the tooltip all stay correct.
     *
     * <p>Nothing throws, and no unit test in any module can see it -- this scan is the only mechanical
     * guard, and the gate's armour-bar row is the only witness.
     *
     * <h2>*** THE SECOND HALF OF THIS ROW HAS NEVER BEEN WITNESSED FIRING, AND THAT IS RECORDED
     * RATHER THAN GLOSSED ***</h2>
     *
     * <p>Two axes here, so two mutations were run. The ORDER axis is measured: moving
     * {@code nativeArmor += vanilla} below the scaled declaration reddens this row on the
     * {@code native_ < scaled} assertion.
     *
     * <p><b>The VALUE axis -- the loop below banning {@code nativeArmor += scaled} -- could not be
     * mutated in isolation.</b> {@code scaled} is declared BELOW the accumulate in the correct
     * ordering, so {@code nativeArmor += scaled} <b>does not compile there</b>; the only mutation that
     * compiles moves the accumulate down first, and the row then dies on the FIRST assertion instead
     * -- measured, it failed on <i>"the native sum must still accumulate the raw vanilla points"</i>
     * and the loop was never reached.
     *
     * <p>So the ban is a guard whose failure path has NOT been observed. It is kept deliberately: it
     * becomes reachable the moment someone hoists the {@code scaled} declaration above the accumulate,
     * which is exactly the tidy-up this row exists to catch, and on that tree it is the only
     * assertion that would still be looking. <b>Named here so the next reader does not mistake it for
     * a verified guard</b> -- CLAUDE.md: a guard that cannot fire is indistinguishable from one that
     * protects you.
     */
    @Test
    void theNativeArmourSumIsTheUNSCALEDOneAndAccumulatesFirst() throws IOException {
        List<String> lines = read(DEFENSE, 150);

        int native_ = indexOf(lines, "nativeArmor += vanilla;");
        int scaled = indexOf(lines, "double scaled = GearScore.scaledDefense(vanilla,");
        assertTrue(native_ > 0, "the native sum must still accumulate the raw vanilla points");
        assertTrue(scaled > 0, "and the scaled figure must be computed separately");
        assertTrue(native_ < scaled,
                "THE UNSCALED SUM MUST COME FIRST. If nativeArmor is ever fed the scaled number the"
                        + " armour bar empties on the most-armoured player in the game, with the stat"
                        + " and the tooltip both still correct and nothing red anywhere.");

        for (String line : lines) {
            if (isComment(line)) continue;
            assertTrue(!line.contains("nativeArmor += scaled"),
                    "nativeArmor must never accumulate the scaled figure. Found: " + line.trim());
        }
    }

    // --- the pickaxe ruling -------------------------------------------------------------------

    /**
     * *** A TOOL IS NOT A CANDIDATE FOR THE AVERAGE. ADDING {@code toolId} ADMITS EVERY PICKAXE. ***
     *
     * <p>Ben's ruling: a pickaxe must never become one of the top two and raise a drop level without
     * contributing anything to a fight. {@code GearScore.scoreable} enforces it at the STAMP, where a
     * core row reddens it; this scan is the only mechanical guard on the READ side, because
     * {@code candidateScore} needs a live {@code ItemStack}.
     */
    @Test
    void theAverageReadsTheThreeScoreableTagsAndNotToolId() throws IOException {
        List<String> lines = read(SCORE_ITEMS, 150);

        int declaration = indexOf(lines, "static OptionalInt candidateScore(");
        assertTrue(declaration > 0, "the candidate read must exist");

        int end = declaration;
        while (end < lines.size() && !lines.get(end).trim().equals("}")) end++;

        String body = String.join("\n", lines.subList(declaration, Math.min(end + 1, lines.size())));
        assertTrue(body.contains("keys.weaponId"), "a weapon is a candidate");
        assertTrue(body.contains("keys.shieldId"), "a shield is a candidate");
        assertTrue(body.contains("keys.armorId"), "a piece of armour is a candidate");
        assertTrue(!body.contains("keys.toolId"),
                "A TOOL IS NOT. Reading toolId here puts every pickaxe in the hand pool, where it can"
                        + " displace a real weapon from the top two and raise the level of every drop"
                        + " the player takes. Found in the body of candidateScore.");
    }

    // --- the once-per-item rule ---------------------------------------------------------------

    /**
     * *** THE STAMP MUST NOT BE REACHABLE FROM {@code mint}. ***
     *
     * <p>{@code remint} calls {@code mint}, so a stamp placed there fires on every join, every
     * {@code /rpg refresh}, every {@code /rpg enchant} sub-op and every enchant-table click --
     * re-banding a player's whole wardrobe against their current average several times a session. The
     * identical trap {@code EnchantRollItems} documents for the roll, and the identical remedy: the
     * rule is about the CALL SITE, because the obvious flag guard reads as absent inside {@code mint}
     * and would fail open every time.
     */
    @Test
    void nothingStampsAScoreFromInsideWeaponItems() throws IOException {
        List<String> lines = read(WEAPON_ITEMS, 300);
        for (String line : lines) {
            if (isComment(line)) continue;
            assertTrue(!line.contains("stampOnAcquire"),
                    "A STAMP IN THE MINT PATH RE-ROLLS ON EVERY JOIN. WeaponItems.remint calls mint,"
                            + " so this re-bands every item a player owns against their current"
                            + " average on every login and every enchant click. Found: "
                            + line.trim());
        }
    }

    /** All three acquisition paths stamp: {@code /rpg give}, the kit grant, and a craft. */
    @Test
    void everyAcquisitionPathStampsAScore() throws IOException {
        List<String> command = read(COMMAND, 2000);
        List<String> craft = read(CRAFT, 300);

        int give = indexOf(command, "GearScoreItems.stampOnAcquire(item, GearItems.gearClassOf(definition)");
        assertTrue(give > 0, "/rpg give must stamp -- it is the path every gate row uses");

        int kit = indexOf(command, "GearScoreItems.stampOnAcquire(item, GearItems.gearClassOf(weapon)");
        assertTrue(kit > 0, "the kit grant must stamp, or a kit weapon is permanently unscoreable"
                + " -- gear is never scored retroactively");

        assertTrue(indexOf(craft, "GearScoreItems.stampOnAcquire(minted,") > 0,
                "a crafted item must stamp, for the same reason");
    }

    /**
     * *** THE STAMP COMES BEFORE THE ITEM REACHES THE INVENTORY, AND THAT IS LOAD-BEARING. ***
     *
     * <p>The average reads the hotbar. An item stamped AFTER it landed would be in its own pool and
     * would help set its own band -- so a drop would be banded partly on itself. Stamping first means
     * a drop is banded on the gear the player HAD.
     */
    @Test
    void theStampHappensBeforeTheItemEntersTheInventory() throws IOException {
        List<String> lines = read(COMMAND, 2000);

        int stamp = indexOf(lines, "GearScoreItems.stampOnAcquire(item, GearItems.gearClassOf(definition)");
        assertTrue(stamp > 0, "the give path's stamp must exist for this ordering to be checkable");

        int add = indexOf(lines, "player.getInventory().addItem(item);", stamp);
        assertTrue(add > stamp,
                "THE STAMP MUST COME FIRST. Stamped after the add, the item is in its own hand pool"
                        + " and helps set its own band.");
        assertTrue(add < stamp + 12, "and the two must be the same give, not two unrelated sites");
    }

    // --- the command gate ---------------------------------------------------------------------

    /**
     * *** UNGATED, {@code /rpg gearscore set} IS A DAMAGE CHEAT. ***
     *
     * <p>It multiplies a weapon's damage and an armour piece's Defense by up to 5x on whatever the
     * player is holding. Asserted as ADJACENCY -- the line immediately after the literal -- because
     * the comment block above it names {@code Permissions.DEV} in prose, and a bare search would find
     * that and report the gate present after it had been deleted.
     */
    @Test
    void theGearScoreDevCommandIsGatedOnDEV() throws IOException {
        List<String> lines = read(COMMAND, 2000);

        int literal = indexOf(lines, "Commands.literal(\"gearscore\")");
        assertTrue(literal > 0, "the /rpg gearscore subcommand must exist -- without it not one gate"
                + " row in this slice is stageable");

        String next = lines.get(literal + 1).trim();
        assertTrue(next.startsWith(".requires("),
                "the gate must be the VERY NEXT thing on the branch, so nothing can be inserted above"
                        + " it that runs ungated. Found: " + next);
        assertTrue(next.contains("Permissions.DEV"),
                "and it must be the DEV node. Found: " + next);
    }

    /**
     * The score argument's bounds come from {@code GearScore}, not from literals, so Brigadier's
     * refusal cannot drift from the arithmetic.
     *
     * <p>Searched FROM the gearscore literal rather than from the top: {@code Commands.argument} occurs
     * many times in this file, and a scan from line 0 would find a different subcommand's argument and
     * report a perfectly true fact about the wrong code.
     */
    @Test
    void theScoreArgumentIsBoundedByTheSharedConstantsRatherThanLiterals() throws IOException {
        List<String> lines = read(COMMAND, 2000);

        int literal = indexOf(lines, "Commands.literal(\"gearscore\")");
        assertTrue(literal > 0, "the subcommand must exist for this row to mean anything");

        int argument = indexOf(lines, "Commands.argument(\"score\"", literal);
        assertTrue(argument > literal, "the score argument must follow the gearscore literal");
        assertTrue(argument < literal + 20,
                "and must be on the same branch, or this row is reading a different subcommand");

        String region = String.join("\n", lines.subList(argument,
                Math.min(lines.size(), argument + 3)));
        assertTrue(region.contains("GearScore.MIN") && region.contains("GearScore.HARD_CAP"),
                "bound the score on the shared constants, not on a 100 and a 500 that can drift"
                        + " apart from the clamp. Found: " + region);
    }

    /** {@code clear} must exist: it is the ONLY way to stage the absent-reads-100 gate row. */
    @Test
    void theCommandCanCLEARAStampToManufactureALegacyItem() throws IOException {
        List<String> lines = read(COMMAND, 2000);
        int literal = indexOf(lines, "Commands.literal(\"gearscore\")");
        assertTrue(indexOf(lines, "Commands.literal(\"clear\")", literal) > literal,
                "WITHOUT clear THERE IS NO WAY TO MANUFACTURE AN UNSTAMPED ITEM. Nothing mints one"
                        + " any more, so the 'an item minted before this slice deals yesterday's"
                        + " damage' row would need an inventory saved from before the build.");
    }

    // --- helpers ------------------------------------------------------------------------------

    /**
     * Read a source file, and assert the scan actually found it.
     *
     * <p><b>The positive control, and it is not optional.</b> A wrong path returns an empty list,
     * every {@link #indexOf} then returns -1, and every assertion above would fail with a message
     * blaming the production code instead of the test. Worse, an assertion phrased the other way round
     * would PASS by default -- which is exactly what the {@code toolId} and {@code stampOnAcquire}
     * rows below are, so this control is what makes those two mean anything at all.
     */
    private static List<String> read(Path path, int atLeast) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertTrue(lines.size() > atLeast,
                "the scan must have read " + path + ", not an empty or wrong path -- finding nothing"
                        + " here would make every assertion below meaningless, and would make the"
                        + " absence rows pass by default. Read " + lines.size()
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

    /**
     * *** A COMMENTED-OUT CALL IS NOT A CALL, AND IN THIS REPO THE COMMENTARY OUTWEIGHS THE CODE. ***
     *
     * <p>Every control this file guards is also EXPLAINED in prose within a few lines of itself --
     * {@code GearScoreItems.carry}, {@code stampOnAcquire} and {@code keys.toolId} all appear in
     * javadoc as well as in code. An unfiltered scan searches the commentary too, and would report a
     * deleted control present because the paragraph lamenting its deletion still names it.
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
        assertEquals(-1, indexOf(List.of("alpha", "beta"), "gamma"));
        assertEquals(0, indexOf(List.of("alpha", "beta"), "alpha"));
    }

    /** And the offset form really skips, which several rows above depend on. */
    @Test
    void theScannerCanSkipAnEarlierMatchOfTheSameNeedle() {
        List<String> twice = List.of("argument here", "middle", "argument here");
        assertEquals(0, indexOf(twice, "argument", 0));
        assertEquals(2, indexOf(twice, "argument", 1),
                "without this, a needle that occurs in several subcommands reads the wrong one");
        assertEquals(-1, indexOf(twice, "argument", 3));
    }

    /**
     * *** THE CONTROL ON THE COMMENT FILTER, CAUSED RATHER THAN ASSERTED. ***
     *
     * <p>The filter is the difference between "this call exists" and "this text appears somewhere",
     * and a commented-out call is exactly how a deleted control keeps looking present. The fixture
     * stages all three comment shapes because the production files use all three.
     */
    @Test
    void theScannerIgnoresCommentedOutCode() {
        assertEquals(-1, indexOf(
                List.of("        // GearScoreItems.carry(from, to, keys);"),
                "GearScoreItems.carry(from, to, keys);"),
                "a commented-out call must not read as a call");
        assertEquals(-1, indexOf(
                List.of("     * GearScoreItems.carry(from, to, keys); moves the stamp"),
                "GearScoreItems.carry(from, to, keys);"),
                "nor must a javadoc line describing it");
        assertEquals(-1, indexOf(
                List.of("        /* GearScoreItems.carry(from, to, keys); */"),
                "GearScoreItems.carry(from, to, keys);"),
                "nor a block comment");
        assertEquals(0, indexOf(
                List.of("        GearScoreItems.carry(from, to, keys);"),
                "GearScoreItems.carry(from, to, keys);"),
                "while the real statement is still found");
    }
}
