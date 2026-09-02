package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The reverse index, and the collision policy it enforces.
 *
 * <p>This is the part of mint-on-craft that CAN be tested. Everything downstream of it -- the mint,
 * the durability gate, the Crafter guard -- needs a running server and is boot-gate-only, which is
 * exactly why the index was keyed on a String token and put in {@code core}: the policy that decides
 * whether a player receives a wrong item gets a real witness rather than a gate row.
 *
 * <p>Each test names the mutation it forces red.
 */
class CraftResultIndexTest {

    private static ShieldDefinition shield(String id, String craftResult) {
        return new ShieldDefinition(id, id, Rarity.COMMON, "shield", 0.35, List.of(),
                Optional.ofNullable(craftResult));
    }

    private static ArmorDefinition armor(String id, String material, String craftResult) {
        return new ArmorDefinition(id, id, Rarity.COMMON, material, ArmorSlot.CHEST, 6.0, List.of(),
                Optional.ofNullable(craftResult));
    }

    // ------------------------------------------------------------- the happy path

    @Test
    void aClaimedResultResolvesToItsDefinition() {
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("shield", "shield")), problem -> fail("unexpected: " + problem));

        assertEquals("shield", index.forResult("shield").orElseThrow().id());
        assertEquals(1, index.size());
        assertEquals(1, index.claimed());
        assertEquals(0, index.contested());
        // Mutation: skip definitions whose claim is present -> reddens with an empty Optional.
    }

    @Test
    void gearThatClaimsNothingIsNotIndexed() {
        // The norm. Most gear never participates, and an index that swept everything in would mint
        // on crafts nobody opted into.
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("shield", null), armor("iron_chestplate", "iron_chestplate", null)),
                problem -> fail("unexpected: " + problem));

        assertEquals(0, index.size());
        assertEquals(0, index.claimed());
        assertTrue(index.forResult("shield").isEmpty());
        // Mutation: fall back to material() when the claim is absent -> size becomes 2 -> reddens.
        // That mutation is the whole design error this key exists to avoid.
    }

    // ------------------------------------------------------------- the collision

    @Test
    void aContestedResultIsDROPPED_NotFirstWins() {
        // THE policy. Two definitions claiming one item means neither mints, so the player gets a
        // plain item they can craft again -- never a wrong one they own forever.
        List<String> problems = new ArrayList<>();
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("first", "shield"), shield("second", "shield")), problems::add);

        assertTrue(index.forResult("shield").isEmpty(),
                "a contested result must resolve to NOTHING, not to whichever was loaded first");
        assertEquals(0, index.size());
        assertEquals(2, index.claimed(), "both definitions still made a claim");
        assertEquals(1, index.contested());
        // Mutation: keep the first claimant instead of dropping -> the first assertion reddens.
        // That is the alphabetical-order-decides-the-economy bug.
    }

    @Test
    void aCollisionIsReportedOnceAndNamesEveryClaimant() {
        List<String> problems = new ArrayList<>();
        CraftResultIndex.build(
                List.of(shield("alpha", "shield"), shield("beta", "shield"), shield("gamma", "shield")),
                problems::add);

        assertEquals(1, problems.size(), "one message per contested result, not one per claimant");
        String message = problems.get(0);
        assertTrue(message.contains("'alpha'"), message);
        assertTrue(message.contains("'beta'"), message);
        assertTrue(message.contains("'gamma'"), message);
        assertTrue(message.contains("shield"), message);
        // Mutation: never call onProblem -> reddens at 0 != 1. A silently dropped result is the
        // worst outcome available here: no mint, and no way to find out why.
    }

    @Test
    void anUncontestedResultBesideAContestedOneStillResolves() {
        // The collision must not poison the whole index. A content error in one file should cost
        // that one result, not every craft on the server.
        List<String> problems = new ArrayList<>();
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("first", "shield"), shield("second", "shield"),
                        armor("iron_chestplate", "iron_chestplate", "iron_chestplate")),
                problems::add);

        assertTrue(index.forResult("shield").isEmpty());
        assertEquals("iron_chestplate", index.forResult("iron_chestplate").orElseThrow().id());
        assertEquals(1, index.size());
        assertEquals(1, index.contested());
        // Mutation: abandon the whole index on the first collision -> the second assertion reddens.
    }

    // ---------------------------------------------------------- normalisation

    @Test
    void lookupAndBuildNormaliseTheSameWay() {
        // A lookup that normalised differently from the build would miss EVERY entry, and the only
        // symptom would be "crafting mints nothing" with nothing red and nothing logged.
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("shield", "minecraft:SHIELD")), problem -> fail("unexpected: " + problem));

        assertTrue(index.forResult("shield").isPresent(), "authored with a namespace, looked up without");
        assertTrue(index.forResult("SHIELD").isPresent(), "case must not matter");
        assertTrue(index.forResult("minecraft:shield").isPresent(), "namespace must not matter");
        // Mutation: drop the namespace strip in CraftResultToken.token -> all three redden.
        // Mutation: normalise on build but not on lookup -> the last two redden.
    }

    @Test
    void aClaimSpelledTwoWaysIsSTILLAContest() {
        // The subtle one. Without shared normalisation these two look like different results and
        // BOTH index -- so the same craft resolves to whichever the map happened to hold.
        List<String> problems = new ArrayList<>();
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("first", "shield"), shield("second", "MINECRAFT:Shield")), problems::add);

        assertEquals(1, index.contested(), "two spellings of one material are one contested result");
        assertTrue(index.forResult("shield").isEmpty());
        assertEquals(1, problems.size());
        // Mutation: normalise at lookup only, not at build -> contested becomes 0 and size 2.
    }

    // ----------------------------------------------------------- the zero guard

    @Test
    void anEmptyIndexIsAnAnswerTheCallerCanSee() {
        // ZERO IS A DEFECT, and the caller cannot warn about what it cannot observe. This pins that
        // size() reports it rather than the index pretending to be populated.
        CraftResultIndex index = CraftResultIndex.build(List.of(), problem -> fail("unexpected"));

        assertEquals(0, index.size());
        assertEquals(0, index.claimed());
        assertEquals(0, index.contested());
        assertTrue(index.forResult("shield").isEmpty());
        // Mutation: return a hardcoded positive size -> reddens. The boot's zero-warning depends on
        // this number being honest.
    }

    @Test
    void aNullLookupIsNotACrash() {
        // Reachable: the lookup is fed a material name derived from a crafted item, on a path that
        // runs several times a second. Throwing here would break the preview for everyone rather
        // than declining one mint.
        CraftResultIndex index = CraftResultIndex.build(
                List.of(shield("shield", "shield")), problem -> fail("unexpected"));

        assertTrue(index.forResult(null).isEmpty());
        // Mutation: drop the null guard -> NullPointerException -> reddens.
    }

    // ------------------------------------------------- the record's own refusal

    @Test
    void aBlankClaimIsRefusedByTheRecordRatherThanReadAsAbsent() {
        // A file that writes `craft_result:` with nothing after it stated an intention it did not
        // finish. Reading that as "does not participate" would leave the author hunting a mint that
        // was never going to happen.
        IllegalArgumentException blank = assertThrows(IllegalArgumentException.class,
                () -> shield("shield", "   "));
        assertTrue(blank.getMessage().contains("craft_result"), blank.getMessage());
        assertTrue(blank.getMessage().contains("shield"), blank.getMessage());
        // Mutation: treat blank as Optional.empty() -> reddens. The loader turns this throw into a
        // named, skipped file, which is how the author finds out.
    }
}
