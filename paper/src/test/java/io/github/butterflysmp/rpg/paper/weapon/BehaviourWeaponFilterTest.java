package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantCandidate;
import io.github.butterflysmp.rpg.core.enchant.EnchantSlot;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.GrantedTriggers;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.content.EnchantLoader;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import io.github.butterflysmp.rpg.paper.content.WeaponLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE OWED WEAPON FILTER (PLAN-enchant-columns.md, the carried list's item 1; the seat's brief of 2026-11-24, PR 1): a
 * behaviour whose enchant names a weapon ({@code weapon:}, W1) acts ONLY on that weapon, enforced ONCE where behaviours
 * are read from the item ({@link BehaviourItems#activeOf}), which every cast, the tooltip and a vanilla shot's stamp
 * read through. A behaviour with no weapon (a unique, element enchant: Kinetic Tremors) is untouched.
 *
 * <p>Each weapon-specific enchant is staged in slot 0 as the dev command's {@code candidate} op stages it (W2's "can
 * never appear" binds the ROLL, not the dev command), active at level 2, and read through the SHIPPED enchant files and
 * the SHIPPED weapon files. What a cast does with an EMPTY behaviour list is the core's, held by
 * {@code AirburstTest.withoutTheEnchantTheShotIsTheLanterns} and {@code ShatterCapsTest.notActiveOrOnAnotherWeaponFourChunksAsToday}:
 * the weapon fires as authored.
 */
class BehaviourWeaponFilterTest {

    private static final Set<String> ELEMENTS = Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither");

    private static Logger quiet(String name) {
        Logger q = Logger.getLogger("BehaviourWeaponFilterTest-" + name + "-" + System.nanoTime());
        q.setUseParentHandlers(false);
        return q;
    }

    private static WeaponRegistry weapons() {
        WeaponRegistry w = new WeaponLoader(quiet("w")).loadAll(Path.of("src", "main", "resources", "content", "weapons").toFile());
        for (String id : List.of("living_lantern", "crystal_shard_staff", "breacher", "boltor", "lapis_staff", "flint_staff",
                "cursed_emerald")) {
            assertTrue(w.find(id).isPresent(), "CONTROL: " + id + " loaded");
        }
        return w;
    }

    private static EnchantRegistry enchants(WeaponRegistry w) {
        EnchantRegistry r = EnchantLoader.withWeapons(quiet("e"), ELEMENTS::contains, w::find, false)
                .loadAll(Path.of("src", "main", "resources", "content", "enchants").toFile());
        for (String id : List.of("airburst_flames", "shatter_caps", "lapis_crystal", "kinetic_tremors")) {
            assertTrue(r.find(id).isPresent(), "CONTROL: " + id + " loaded");
        }
        return r;
    }

    /** The enchant staged ACTIVE at level 2 in slot {@code slot} (0 the weapon column, 1 the unique). */
    private static EnchantState staged(int slot, String enchantId) {
        EnchantSlot active = new EnchantSlot(List.of(new EnchantCandidate(enchantId, 2)), 0);
        return new EnchantState(List.of(slot == 0 ? active : EnchantSlot.empty(), slot == 1 ? active : EnchantSlot.empty(),
                EnchantSlot.empty(), EnchantSlot.empty()));
    }

    private static List<String> ids(List<ActiveBehaviour> behaviours) {
        return behaviours.stream().map(ActiveBehaviour::enchantId).toList();
    }

    @Test
    void airburstActsOnTheLivingLanternAndOnNoOtherProjectileWeapon() {
        EnchantRegistry r = enchants(weapons());
        EnchantState s = staged(0, "airburst_flames");
        assertEquals(List.of(new ActiveBehaviour("airburst_flames", 2, r.find("airburst_flames").orElseThrow().behaviour())),
                BehaviourItems.activeOf(s, "living_lantern", r::find), "on the lantern: exactly as before");
        assertEquals(List.of(), BehaviourItems.activeOf(s, "crystal_shard_staff", r::find),
                "on the Crystal Shard Staff (a homing projectile): nothing, so it fires its own shot");
        assertEquals(List.of(), BehaviourItems.activeOf(s, "breacher", r::find), "on the Breacher: nothing");
    }

    @Test
    void shatterCapsActsOnTheBreacherAndOnNoOtherWeapon() {
        EnchantRegistry r = enchants(weapons());
        EnchantState s = staged(0, "shatter_caps");
        assertEquals(List.of("shatter_caps"), ids(BehaviourItems.activeOf(s, "breacher", r::find)), "on the Breacher");
        assertEquals(List.of(), BehaviourItems.activeOf(s, "boltor", r::find), "on the Boltor: nothing");
        assertEquals(List.of(), BehaviourItems.activeOf(s, "living_lantern", r::find), "on the lantern: nothing");
    }

    /** The grant reaches the resolver (the firing path and the tooltip) only from the Lapis Staff's own item. */
    @Test
    void lapisCrystalsGrantReachesOnlyTheLapisStaff() {
        WeaponRegistry w = weapons();
        EnchantRegistry r = enchants(w);
        EnchantState s = staged(0, "lapis_crystal");
        WeaponDefinition staff = w.find("lapis_staff").orElseThrow();
        List<ActiveBehaviour> onStaff = BehaviourItems.activeOf(s, "lapis_staff", r::find);
        assertEquals(List.of("lapis_crystal"), ids(onStaff), "on the Lapis Staff");
        assertEquals(List.of("left_click"), GrantedTriggers.granted(staff, onStaff).stream().map(b -> b.input()).toList(),
                "the control: the staff is granted its left click");
        assertEquals(List.of(), BehaviourItems.activeOf(s, "flint_staff", r::find), "on the Flint Staff: nothing reaches it");
    }

    /** A unique behaviour names no weapon, so the filter passes it on every weapon it can be on. */
    @Test
    void kineticTremorsIsUnchangedOnEveryKineticWeapon() {
        WeaponRegistry w = weapons();
        EnchantRegistry r = enchants(w);
        EnchantState s = staged(1, "kinetic_tremors");
        int checked = 0;
        for (WeaponDefinition weapon : w.all()) {
            if (!"kinetic".equals(weapon.element())) continue;
            assertEquals(List.of("kinetic_tremors"), ids(BehaviourItems.activeOf(s, weapon.id(), r::find)), weapon.id());
            checked++;
        }
        // 16: `grep -l '^element: kinetic' paper/src/main/resources/content/weapons/*.yml | wc -l`, 2026-11-24 at 6a1c4713.
        assertEquals(16, checked, "CONTROL: every kinetic weapon was walked");
    }

    /** No minted weapon in hand (a null id): a weapon-specific behaviour reaches nothing; a unique one is not judged here. */
    @Test
    void aNullWeaponIdPassesOnlyBehavioursThatNameNoWeapon() {
        EnchantRegistry r = enchants(weapons());
        assertEquals(List.of(), BehaviourItems.activeOf(staged(0, "airburst_flames"), null, r::find));
        assertEquals(List.of("kinetic_tremors"), ids(BehaviourItems.activeOf(staged(1, "kinetic_tremors"), null, r::find)));
    }
}
