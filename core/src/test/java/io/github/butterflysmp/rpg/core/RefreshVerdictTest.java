package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.RefreshVerdict;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Lore Refresher's per-slot policy: given the weapon id read off an inventory slot, does the
 * refresh leave it alone, report it, or rebuild it?
 *
 * This is the pure half of the pass and the only half that can be tested at all -- the ItemStack
 * scan, the re-mint and the durability carry-forward all need a running server (see
 * WeaponItemsTest) and are witnessed on the boot. So these tests carry the two properties that
 * would otherwise rest entirely on someone remembering to check them in-game: an item that is not
 * ours is never touched, and an item whose content file has gone is never destroyed.
 *
 * Each test names the mutation it forces red.
 */
class RefreshVerdictTest {

    private static final double EPS = 1e-9;

    /** A trigger's ability, synthesized the way WeaponLoader does: id = weaponId/input. */
    private static AbilityDefinition trigger(String weaponId, String input) {
        return new AbilityDefinition(weaponId + "/" + input, "Test", "kinetic", "none",
                0, ResourceCost.FREE, new CastSpec.Melee(3.0, 90),
                List.of(new EffectSpec.Damage(8, "kinetic")));
    }

    /** An emberblade at a given attack damage -- the field the "current definition" tests vary. */
    private static WeaponDefinition emberblade(double attackDamage) {
        return new WeaponDefinition("emberblade", "Emberblade", "fire", Rarity.RARE,
                WeaponClass.MELEE, "iron_sword", attackDamage, 0.0,
                List.of(new TriggerBinding("left_click", trigger("emberblade", "left_click"))),
                List.of());
    }

    private static WeaponRegistry registryOf(WeaponDefinition... weapons) {
        var registry = new WeaponRegistry();
        for (WeaponDefinition weapon : weapons) registry.register(weapon);
        return registry;
    }

    /**
     * THE SAFETY INVARIANT. A slot with no weapon id -- an empty slot, a dirt block, a vanilla
     * sword, a naturally-spawned enchanted bow -- is not ours, and the refresh must not form an
     * opinion about it. This is what keeps a scan of all 41 slots from being a rewrite of the
     * player's whole inventory.
     */
    @Test
    void anItemWithNoWeaponIdIsNotOursAndIsLeftAlone() {
        var verdict = RefreshVerdict.decide(null, registryOf(emberblade(7)));

        assertInstanceOf(RefreshVerdict.Untagged.class, verdict,
                "an untagged item must not be re-minted, however much content is loaded");
        // Mutation: treat null as a lookup ("" or a miss) -> Dangling, and every empty slot warns;
        // treat it as a match -> Remint, and the refresh overwrites vanilla items -> reddens.
    }

    /**
     * The destructive failure this pass must not have. An id whose content file was renamed or
     * deleted is DANGLING, not a re-mint and not a strip: the item stays exactly as it is. A
     * content typo must not cost a player the thing they earned, and the weapon starts working
     * again the moment its definition comes back.
     *
     * Asserted as "not Remint" as well as "is Dangling", because the failure mode is specifically
     * a lookup that falls through to some default definition rather than reporting the miss.
     */
    @Test
    void anIdWithNoLoadedDefinitionIsDanglingRatherThanRemintedOrStripped() {
        var verdict = RefreshVerdict.decide("deleted_sword", registryOf(emberblade(7)));

        assertInstanceOf(RefreshVerdict.Dangling.class, verdict);
        assertFalse(verdict instanceof RefreshVerdict.Remint,
                "a missing definition must never produce something to mint from");
        assertFalse(verdict instanceof RefreshVerdict.Untagged,
                "it IS ours -- silently skipping it would hide a real content break");
        // Mutation: drop the Dangling arm so a miss falls back to Untagged (or to the registry's
        // first weapon) -> the item is silently left unreported, or re-minted as a DIFFERENT
        // weapon -> reddens.
    }

    /**
     * The warning has to be able to name the id. A dangling report that cannot say WHICH id is
     * missing sends you reading every file in content/weapons to find it.
     */
    @Test
    void theDanglingVerdictCarriesTheMissingIdSoTheWarningCanNameIt() {
        var verdict = RefreshVerdict.decide("deleted_sword", registryOf(emberblade(7)));

        assertEquals("deleted_sword", ((RefreshVerdict.Dangling) verdict).weaponId());
        // Mutation: report a constant ("unknown") instead of the id -> reddens.
    }

    /** The ordinary case: a loaded id yields the definition to rebuild the display from. */
    @Test
    void aLoadedIdYieldsARemintCarryingThatWeaponsDefinition() {
        var verdict = RefreshVerdict.decide("emberblade", registryOf(emberblade(7)));

        var remint = assertInstanceOf(RefreshVerdict.Remint.class, verdict);
        assertEquals("emberblade", remint.definition().id());
    }

    /**
     * THE POINT OF THE WHOLE PASS. The definition handed back must come from the registry passed
     * in -- the content loaded NOW -- not from anything captured when the item was first minted.
     *
     * Two registries hold the same id at different attack damage, which is what a content edit
     * plus a restart looks like. Deciding against the second must yield 12. A stale-source bug
     * cannot survive this: there is no "correct" answer available except by reading the argument,
     * and the two registries are otherwise identical, so it cannot pass by returning either one
     * unconditionally.
     */
    @Test
    void theRemintCarriesTheCurrentDefinitionNotTheOneTheItemWasMintedFrom() {
        var beforeTheContentEdit = registryOf(emberblade(8));
        var afterTheContentEdit = registryOf(emberblade(12));

        var stale = (RefreshVerdict.Remint) RefreshVerdict.decide("emberblade", beforeTheContentEdit);
        var current = (RefreshVerdict.Remint) RefreshVerdict.decide("emberblade", afterTheContentEdit);

        assertEquals(8.0, stale.definition().attackDamage(), EPS);
        assertEquals(12.0, current.definition().attackDamage(), EPS,
                "the refresh must mint from the content loaded now, or it refreshes nothing");
        // Mutation: resolve against anything other than the passed registry -- a cached or static
        // registry, or the definition the caller already held -> both reads return the same
        // number -> reddens.
    }
}
