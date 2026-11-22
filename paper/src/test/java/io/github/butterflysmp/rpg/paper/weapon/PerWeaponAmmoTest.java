package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.weapon.CollectPlan;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PER-WEAPON AMMUNITION, CAUSED (the seat's brief of 2026-11-22, PR 2, rulings 1 and 2). Every row drives the REAL
 * planner {@code QuiverAmmo.supply} hands its reads to ({@code plan}, {@code consumes}, {@code remaining},
 * {@code ammoOf}, {@code QuiverNotice.noAmmoText}); only the two reads of a live {@code Player} (its game mode, its
 * storage slots) and the {@code setItem} writes are left to the boot, because no unit test here can build a Player or
 * an ItemStack ({@code QuiverAmmoTest}'s header).
 *
 * <p>The inventory every row stages holds BOTH items, so a row reading the wrong material reads a different number:
 * arrows in slot 3 (64) and cobblestone in slots 7 (1) and 12 (4).
 */
class PerWeaponAmmoTest {

    private static final List<QuiverAmmo.Cell> BOTH = List.of(
            new QuiverAmmo.Cell(3, Material.ARROW, 64, false),
            new QuiverAmmo.Cell(7, Material.COBBLESTONE, 1, false),
            new QuiverAmmo.Cell(12, Material.COBBLESTONE, 4, false));

    private static WeaponDefinition weapon(String ammo) {
        return new WeaponDefinition("w", "W", "kinetic", Rarity.COMMON, WeaponClass.RANGER, "crossbow", 0.0, 0.0, 0.0,
                2, 60, List.of(new TriggerBinding("right_click", new AbilityDefinition("w/right_click", "Fire",
                        "kinetic", 0, null, null, List.of(), List.of()))), List.of(), Optional.empty(), false, ammo);
    }

    /**
     * The weapon's token through {@code QuiverAmmo.resolve}, with the enum's own lookup standing in for the registry
     * ({@code QuiverAmmo.carriable} on a server).
     */
    private static Material resolved(WeaponDefinition w) {
        return QuiverAmmo.resolve(w.ammo(), t -> Material.valueOf(t.toUpperCase(java.util.Locale.ROOT))).orElseThrow();
    }

    /** A token no item answers to resolves EMPTY, and an empty ammo plans ZERO rounds: the reload fails closed. */
    @Test
    void anAmmoNoItemAnswersToFailsClosed() {
        assertTrue(QuiverAmmo.resolve("not_a_thing", t -> null).isEmpty());
        assertEquals(0, QuiverAmmo.plan(true, BOTH, null, 2).rounds(), "never a fallback to the 64 arrows");
        assertEquals(OptionalInt.empty(), QuiverAmmo.remaining(Material.ARROW, 64, null, 2), "and nothing is debited");
    }

    private static List<Integer> slots(QuiverAmmo.Supply supply) {
        return supply.draws().stream().map(d -> d.source().slot()).toList();
    }

    /** A weapon naming cobblestone reloads from cobblestone, and NOT from the 64 arrows beside it. */
    @Test
    void aWeaponNamingCobblestoneReloadsFromCobblestoneNotArrows() {
        Material ammo = resolved(weapon("cobblestone"));
        assertSame(Material.COBBLESTONE, ammo);
        QuiverAmmo.Supply supply = QuiverAmmo.plan(true, BOTH, ammo, 2);
        assertEquals(2, supply.rounds(), "two rounds, one cobblestone each");
        assertEquals(2, CollectPlan.total(supply.draws()));
        assertFalse(slots(supply).contains(3), "the arrows' slot is never planned: " + slots(supply));
        assertTrue(List.of(7, 12).containsAll(slots(supply)), "only cobblestone slots: " + slots(supply));
    }

    /** A weapon naming nothing reloads from arrows, and NOT from the cobblestone beside them. */
    @Test
    void aWeaponNamingNothingReloadsFromArrowsNotCobblestone() {
        Material ammo = resolved(weapon(null));
        assertSame(Material.ARROW, ammo, "the default is the arrow every weapon had");
        QuiverAmmo.Supply supply = QuiverAmmo.plan(true, BOTH, ammo, 2);
        assertEquals(2, supply.rounds());
        assertEquals(List.of(3), slots(supply), "only the arrows' slot");
    }

    /** One cobblestone for a two-round reload loads ONE round: a partial supply, a partial magazine. */
    @Test
    void aPartialSupplyLoadsAPartialMagazine() {
        List<QuiverAmmo.Cell> one = List.of(new QuiverAmmo.Cell(3, Material.ARROW, 64, false),
                new QuiverAmmo.Cell(7, Material.COBBLESTONE, 1, false));
        QuiverAmmo.Supply supply = QuiverAmmo.plan(true, one, Material.COBBLESTONE, 2);
        assertEquals(1, supply.rounds(), "one cobblestone, one round");
        assertEquals(List.of(7), slots(supply));
        // And none at all is zero rounds, which QuiverState.reloadVerdict turns into NO_AMMO.
        assertEquals(0, QuiverAmmo.plan(true, List.of(BOTH.get(0)), Material.COBBLESTONE, 2).rounds(),
                "arrows alone never load a cobblestone weapon");
        assertEquals(0, QuiverAmmo.plan(true, BOTH.subList(1, 3), Material.ARROW, 2).rounds(),
                "cobblestone alone never loads an arrow weapon");
    }

    /** Creative pays nothing: the full reload and NO draws, whatever the weapon's ammo and whatever is carried. */
    @Test
    void creativeIsFree() {
        assertFalse(QuiverAmmo.consumes(GameMode.CREATIVE));
        assertTrue(QuiverAmmo.consumes(GameMode.SURVIVAL));
        QuiverAmmo.Supply supply = QuiverAmmo.plan(QuiverAmmo.consumes(GameMode.CREATIVE), List.of(),
                Material.COBBLESTONE, 2);
        assertEquals(2, supply.rounds(), "creative supplies what the reload needs, from an empty inventory");
        assertTrue(supply.draws().isEmpty(), "and takes nothing");
    }

    /** Gear of the ammo's material is never counted (the invariant QuiverAmmo carries for arrows, now for any ammo). */
    @Test
    void gearIsNeverAmmunition() {
        List<QuiverAmmo.Cell> gear = List.of(new QuiverAmmo.Cell(5, Material.COBBLESTONE, 9, true));
        assertEquals(0, QuiverAmmo.plan(true, gear, Material.COBBLESTONE, 2).rounds());
    }

    /** The debit re-check is the weapon's ammo: a slot that now holds arrows is never debited for a cobblestone plan. */
    @Test
    void theDebitTakesOnlyTheWeaponsAmmo() {
        assertEquals(OptionalInt.of(2), QuiverAmmo.remaining(Material.COBBLESTONE, 4, Material.COBBLESTONE, 2));
        assertEquals(OptionalInt.empty(), QuiverAmmo.remaining(Material.ARROW, 64, Material.COBBLESTONE, 2));
        assertEquals(OptionalInt.empty(), QuiverAmmo.remaining(Material.COBBLESTONE, 4, Material.ARROW, 2));
    }

    /** THE NOTICE NAMES THE ITEM (ruling 2): cobblestone for a cobblestone weapon; the arrow sentence byte-identical. */
    @Test
    void theNoticeNamesTheItem() {
        assertEquals("You have no cobblestone -- Cobblestone loads this weapon.",
                QuiverNotice.noAmmoText("cobblestone"));
        assertEquals("You have no arrows -- plain Arrows load a quiver.", QuiverNotice.noAmmoText(weapon(null).ammo()),
                "every arrow weapon's notice is what it was");
        assertEquals("You have no cobblestone -- Cobblestone loads this weapon.",
                QuiverNotice.noAmmoText(weapon("cobblestone").ammo()), "the weapon's own token is what it names");
        assertEquals("You have no not a thing -- Not a thing loads this weapon.", QuiverNotice.noAmmoText("not_a_thing"),
                "a token no item answers to is still named, never silently arrows");
    }
}
