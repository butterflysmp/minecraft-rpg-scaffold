package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE SEAT'S RULING H, 2026-11-22: AN ENCHANT ROLLS ONLY ON A WEAPON THAT HAS ITS QUANTITY. deft_hands needs a reload,
 * expanded_quiver a quiver; the requirement is declared beside the enchant ({@code requires:}), and EnchantRoll's pool
 * applies it after the class filter.
 */
class EnchantQuantityGateTest {

    /**
     * THE WEAPONS WHOSE POOL RULING H CHANGES, and what each loses. Empty on the branch that introduces the gate: every
     * shipped ranger weapon has both quantities. The Crossbow joins it in the content PR (2026-11-22): no reload and no
     * quiver, so it loses both, and its pool is power and unbreaking. The Bow has both and is unchanged.
     */
    private static final Map<String, String> CHANGED = Map.of("crossbow", "deft_hands,expanded_quiver");

    /** Before (class alone) against after (the weapon's quantities), for EVERY shipped weapon. */
    @Test
    void noShippedWeaponsPoolChangesButTheNamedOnes() {
        RollPools roll = RollPools.load();
        Map<String, String> changed = new TreeMap<>();
        int compared = 0;
        for (WeaponDefinition w : roll.weapons().all()) {
            var before = roll.before(w);
            var after = roll.after(w);
            compared++;
            if (!before.equals(after)) {
                var lost = new java.util.ArrayList<>(before);
                lost.removeAll(after);
                changed.put(w.id(), String.join(",", lost));
                assertTrue(before.containsAll(after), w.id() + ": the gate may only REMOVE: " + before + " -> " + after);
            }
        }
        assertEquals(roll.weapons().size(), compared, "every shipped weapon compared");
        assertEquals(new TreeMap<>(CHANGED), changed);
    }

    /** Where the requirement is declared: in the two enchant files, and nowhere else does an enchant declare one. */
    @Test
    void theRequirementIsDeclaredBesideTheEnchant() {
        RollPools roll = RollPools.load();
        Map<String, EnchantRoll.Requires> declared = new TreeMap<>();
        roll.enchants().all().forEach(e -> {
            if (e.requires() != EnchantRoll.Requires.NONE) declared.put(e.id(), e.requires());
        });
        assertEquals(Map.of("deft_hands", EnchantRoll.Requires.RELOAD, "expanded_quiver", EnchantRoll.Requires.QUIVER),
                declared);
    }

    /**
     * THE TWO HALVES THE BEFORE/AFTER ROW CANNOT SEE ON THIS BRANCH (every shipped ranger weapon has both quantities, so a
     * gate that answered EVERY, or a roster that dropped the requirement, leaves every pool unchanged): pinned directly.
     */
    @Test
    void theRosterCarriesTheRequirementAndEachWeaponReportsItsOwnQuantities() {
        RollPools roll = RollPools.load();
        assertTrue(roll.roster().contains(new EnchantRoll.Rollable("deft_hands",
                io.github.butterflysmp.rpg.core.weapon.GearClass.RANGER, EnchantRoll.Requires.RELOAD)), roll.roster().toString());
        assertTrue(roll.roster().contains(new EnchantRoll.Rollable("expanded_quiver",
                io.github.butterflysmp.rpg.core.weapon.GearClass.RANGER, EnchantRoll.Requires.QUIVER)), roll.roster().toString());
        assertEquals(new EnchantRoll.Has(true, true),
                io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems.has(roll.weapons().find("boltor").orElseThrow()));
        assertEquals(EnchantRoll.Has.NOTHING,
                io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems.has(roll.weapons().find("wooden_sword").orElseThrow()));
    }

    /**
     * {@code /rpg enchant pool} (GATE-vanilla-bows' instrument) prints the pool the ROLL uses: EnchantRollItems.poolFor,
     * which builds it with the same has(definition) and roster(adapters) as rollOnAcquire.
     */
    @Test
    void thePoolCommandPrintsTheRollsOwnPool() throws IOException {
        String items = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/weapon/EnchantRollItems.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        // 2026-11-23: poolFor became normalPoolFor (slots 1-3), and uniquePoolFor (slot 0) joined it on the same command.
        int at = items.indexOf("public static List<String> normalPoolFor(GearDefinition definition, AdapterContext adapters) {");
        assertTrue(at > 0, "normalPoolFor is not found");
        String body = items.substring(at, items.indexOf("\n    }\n", at));
        assertTrue(body.contains("EnchantRoll.normalPoolFor(has(definition), gearClass, roster(adapters))"), body);
        int u = items.indexOf("public static List<String> uniquePoolFor(GearDefinition definition, AdapterContext adapters) {");
        assertTrue(u > 0, "uniquePoolFor is not found");
        String ubody = items.substring(u, items.indexOf("\n    }\n", u));
        assertTrue(ubody.contains("EnchantRoll.uniquePoolFor(definition.gearElement(), has(definition), gearClass, roster(adapters))"),
                ubody);
        String cmd = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/command/RpgCommand.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        assertTrue(cmd.contains("io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems.normalPoolFor(gear.gear(), adapters)"));
        assertTrue(cmd.contains("io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems.uniquePoolFor(gear.gear(), adapters)"));
        assertTrue(cmd.contains(".then(Commands.literal(\"pool\")"));
        // And rollOnAcquire rolls with the SAME element accessor and the registry's cascade.
        assertTrue(items.contains("EnchantRoll.roll(definition.gearElement(), has(definition), gearClass, roster(adapters),\n"
                + "                adapters.enchants().candidates(),"), "rollOnAcquire's roll call");
    }

    @Test
    void anUnknownRequirementIsRefusedByName() {
        var thrown = assertThrows(IllegalArgumentException.class, () -> EnchantLoader.requires("reloads", "x"));
        assertTrue(thrown.getMessage().contains("reload or quiver"), thrown.getMessage());
        assertEquals(EnchantRoll.Requires.NONE, EnchantLoader.requires(null, "x"));
        assertEquals(EnchantRoll.Requires.RELOAD, EnchantLoader.requires("Reload", "x"));
        assertEquals(EnchantRoll.Requires.QUIVER, EnchantLoader.requires("quiver", "x"));
    }

    /**
     * AN ITEM ALREADY CARRYING ONE OF THESE ENCHANTS IS NOT TOUCHED. READ: rollOnAcquire returns on an item already rolled
     * BEFORE it builds a pool, so ruling H only ever shapes a NEW roll.
     */
    @Test
    void anItemAlreadyRolledIsNeverRerolled() throws IOException {
        String src = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/weapon/EnchantRollItems.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        int at = src.indexOf("public static void rollOnAcquire(");
        assertTrue(at > 0, "rollOnAcquire is not found");
        String body = src.substring(at, src.indexOf("\n    }\n", at));
        int guard = body.indexOf("if (EnchantItems.isRolled(item, adapters.keys())) return;");
        int roll = body.indexOf("EnchantRoll.roll(definition.gearElement(), has(definition), gearClass, roster(adapters),");   // the element first since 2026-11-23
        assertTrue(guard > 0 && roll > guard, "the already-rolled guard must come first: " + guard + " / " + roll);
        assertFalse(src.contains("Has.EVERY, gearClass"), "production never builds a class-only pool");
    }
}
