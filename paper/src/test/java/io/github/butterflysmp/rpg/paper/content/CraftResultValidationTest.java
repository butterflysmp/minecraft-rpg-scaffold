package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two boot refusals that stand between a content typo and a player receiving the wrong item.
 *
 * <p>Both mistakes are SILENT at runtime, which is the whole reason they are checked at boot: a
 * mismatched claim mints a different item than the one crafted, and a non-durable claim indexes
 * cleanly and then never fires. Neither throws, neither logs, and no in-game observation
 * distinguishes them from correct behaviour.
 *
 * <p>The two Bukkit questions -- does this name resolve to a Material, and does that Material have
 * durability -- arrive as predicates, so the walk is testable with no server. That is the same trade
 * every check in {@code ContentValidator} makes.
 *
 * <p>Each test names the mutation it forces red.
 */
class CraftResultValidationTest {

    /** Pretend registry: these three resolve, and only the two gear items have durability. */
    private static final Set<String> REAL = Set.of("shield", "iron_chestplate", "amethyst_shard");
    private static final Set<String> DURABLE = Set.of("shield", "iron_chestplate");

    private static final Predicate<String> EXISTS = REAL::contains;
    private static final Predicate<String> IS_DURABLE = DURABLE::contains;

    private static final ContentValidator VALIDATOR =
            new ContentValidator(null, null, null, key -> true, key -> true);

    private static ShieldDefinition shield(String id, String material, String craftResult) {
        return new ShieldDefinition(id, id, Rarity.COMMON, material, 0.35, List.of(),
                Optional.ofNullable(craftResult));
    }

    private static ArmorDefinition armor(String id, String material, String craftResult) {
        return new ArmorDefinition(id, id, Rarity.COMMON, material, ArmorSlot.CHEST, 6.0, List.of(),
                Optional.ofNullable(craftResult));
    }

    private static List<String> check(GearDefinition... gear) {
        return VALIDATOR.validateCraftResults(List.of(gear), EXISTS, IS_DURABLE);
    }

    // ------------------------------------------------------------------ clean

    @Test
    void aClaimThatMatchesItsOwnDurableMaterialIsFine() {
        assertEquals(List.of(), check(shield("shield", "shield", "shield")));
        assertEquals(List.of(),
                check(armor("iron_chestplate", "iron_chestplate", "iron_chestplate")));
        // Mutation: always report a problem -> reddens, and every correct file would be named at boot.
    }

    @Test
    void gearWithNoClaimIsNotChecked() {
        // The norm. Most gear never opts in, and a validator that complained about the absence
        // would bury the real problems under twenty-odd lines of noise every boot.
        assertEquals(List.of(), check(shield("shield", "shield", null),
                armor("gold_chestplate", "gold_chestplate", null)));
        // Mutation: drop the `claim.isEmpty()` skip -> every unclaimed piece is reported -> reddens.
    }

    // --------------------------------------------- REFUSAL 1: craft != material

    @Test
    void aClaimThatDiffersFromItsMaterialIsREFUSED() {
        // THE substitution: craft iron, receive diamond. The mint builds from material(), not from
        // what was crafted, so nothing downstream of here would notice.
        List<String> problems = check(armor("mismatched", "shield", "iron_chestplate"));

        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("mismatched"), problems.get(0));
        assertTrue(problems.get(0).contains("iron_chestplate"), problems.get(0));
        assertTrue(problems.get(0).contains("shield"), problems.get(0));
        // Mutation: drop the equality check -> reddens at 0 != 1. This is the one that lets a
        // player craft one item and receive another.
    }

    @Test
    void aClaimSpelledDifferentlyFromItsMaterialStillMATCHES() {
        // Normalisation, applied to both sides. Without it, a file that writes the namespace on one
        // key and not the other is reported as a substitution -- a false alarm on correct content,
        // which is how a warning becomes something people scroll past.
        assertEquals(List.of(), check(shield("shield", "minecraft:SHIELD", "shield")));
        assertEquals(List.of(), check(shield("shield", "shield", "MINECRAFT:Shield")));
        // Mutation: compare the raw strings instead of the tokens -> both redden.
    }

    // ------------------------------------------------ REFUSAL 2: not durable

    @Test
    void aNonDurableClaimIsREFUSED() {
        // The silent no-op: it would index cleanly and then be dropped by the mint's durability
        // gate on every single craft, with nothing anywhere saying why.
        List<String> problems = check(shield("stone", "amethyst_shard", "amethyst_shard"));

        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("amethyst_shard"), problems.get(0));
        assertTrue(problems.get(0).contains("durability"), problems.get(0));
        // Mutation: drop the durability check -> reddens at 0 != 1.
    }

    // --------------------------------------------------- an unresolvable name

    @Test
    void aClaimNamingNoMaterialIsReportedONCE() {
        // It must not also be reported as a mismatch and as non-durable -- three lines about one
        // typo, two of them meaningless, is how a boot log stops being read.
        List<String> problems = check(shield("typo", "shield", "iron_chestplat"));

        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("not a material"), problems.get(0));
        // Mutation: drop the `continue` after the existence failure -> reddens at 3 != 1.
    }

    // ------------------------------------------------------------- the whole walk

    @Test
    void everyDefinitionIsWalkedNotJustTheFirst() {
        // A walk that stopped early would validate whichever file sorted first and wave the rest
        // through -- and the boot would look clean.
        // Each bad definition trips exactly ONE refusal, so the count is unambiguous: bad_one's
        // claim agrees with its material and is merely not durable; bad_two's is durable and
        // disagrees.
        List<String> problems = check(
                shield("ok", "shield", "shield"),
                shield("bad_one", "amethyst_shard", "amethyst_shard"),
                armor("bad_two", "shield", "iron_chestplate"));

        assertEquals(2, problems.size(), problems.toString());
        assertTrue(problems.toString().contains("bad_one"), problems.toString());
        assertTrue(problems.toString().contains("bad_two"), problems.toString());
        // Mutation: `break` after the first problem -> reddens at 1 != 2.
    }
}
