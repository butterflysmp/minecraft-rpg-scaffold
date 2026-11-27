package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2 (the seat's brief, 2026-11-26): a MINTED bone (the Bone Club, the Bone Whistle) is never used on an entity by vanilla,
 * and a plain bone is untouched. The decision is pure and pinned here as a table; the hook's wiring is pinned by
 * {@code SummonWiringTest.aSummonerWeaponTamesNothing}. Mutations named per test.
 */
class MintedBoneUseTest {

    /** Mutation: the minted check dropped (a plain bone is blocked), or the material check dropped (a sword is). */
    @Test
    void onlyOurItemOnABoneIsAMintedBone() {
        assertTrue(MintedBoneUse.isMintedBone(true, "bone"));
        assertFalse(MintedBoneUse.isMintedBone(false, "bone"), "a plain bone is untouched");
        assertFalse(MintedBoneUse.isMintedBone(true, "iron_sword"), "a minted sword is not a bone");
        assertFalse(MintedBoneUse.isMintedBone(true, "bone_meal"), "bone meal is another item");
        assertFalse(MintedBoneUse.isMintedBone(true, null));
    }

    /**
     * THE ONE GUARD's table: (minted, material, summoner class) -> refused. Mutation: the summoner clause dropped (a future
     * summoner weapon on another material would tame), the bone clause dropped (the Bone Club tames a wolf), or the
     * minted check dropped from the summoner clause (cannot happen for real: an unminted item has no class; pinned anyway).
     */
    @Test
    void theEntityGuardRefusesAMintedBoneOfEitherWeaponAndNothingPlain() {
        assertTrue(MintedBoneUse.refuseEntityUse(true, "bone", false), "the Bone Club (melee)");
        assertTrue(MintedBoneUse.refuseEntityUse(true, "bone", true), "the Bone Whistle (summoner)");
        assertTrue(MintedBoneUse.refuseEntityUse(true, "goat_horn", true), "a summoner weapon on any material, as PR 5");
        assertFalse(MintedBoneUse.refuseEntityUse(false, "bone", false), "a plain bone tames, as vanilla");
        assertFalse(MintedBoneUse.refuseEntityUse(true, "iron_sword", false), "a minted sword: not this guard's");
        assertFalse(MintedBoneUse.refuseEntityUse(false, "goat_horn", true), "an unminted item is never refused");
    }
}
