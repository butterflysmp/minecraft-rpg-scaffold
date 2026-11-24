package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.CandidateCascade;
import io.github.butterflysmp.rpg.core.enchant.EnchantCandidate;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.weapon.EnchantRollItems;
import io.github.butterflysmp.rpg.paper.weapon.GearItems;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE UNIQUE SLOT OVER EVERY SHIPPED GEAR DEFINITION (the seat's brief and amendment, 2026-11-23), with the dev-only
 * example LOADED (as on a dev server), so the unique pool is not empty everywhere and the rows can fail.
 *
 * <p>Recounted with a control: 24 weapons, 1 shield, 24 armour pieces, 5 tools, 6 accessories (60); 11 real enchants
 * and the dev one.
 */
class UniqueSlotContentTest {

    /**
     * EVERY SHIPPED GEAR'S POOL AT MASTER {@code 383ded19}, before this PR: captured by running
     * {@code EnchantRoll.poolFor(EnchantRollItems.has(g), GearItems.gearClassOf(g), roster)} over the same five loaders in
     * a worktree of that commit (2026-11-23), sorted by id. The NORMAL pool (slots 1-3) on this branch must equal it for
     * every one of the 60, with the dev unique enchant loaded.
     */
    private static final List<String> MASTER_POOLS = List.of(
            "blaze_kings_staff=attunement,unbreaking",
            "boltor=deft_hands,expanded_quiver,power,unbreaking",
            "bow=deft_hands,expanded_quiver,power,unbreaking",
            "brawlers_gauntlet=",
            "breacher=deft_hands,expanded_quiver,power,unbreaking",
            "chainmail_boots=growth,mana_bank,protection,unbreaking",
            "chainmail_chestplate=growth,mana_bank,protection,unbreaking",
            "chainmail_helmet=growth,mana_bank,protection,unbreaking",
            "chainmail_leggings=growth,mana_bank,protection,unbreaking",
            "copper_sword=sharpness,unbreaking",
            "crossbow=power,unbreaking",
            "crystal_shard_staff=attunement,unbreaking",
            "cursed_emerald=attunement,unbreaking",
            "diamond_boots=growth,mana_bank,protection,unbreaking",
            "diamond_chestplate=growth,mana_bank,protection,unbreaking",
            "diamond_helmet=growth,mana_bank,protection,unbreaking",
            "diamond_leggings=growth,mana_bank,protection,unbreaking",
            "diamond_sword=sharpness,unbreaking",
            "dragons_breath=deft_hands,expanded_quiver,power,unbreaking",
            "dragons_plume=deft_hands,expanded_quiver,power,unbreaking",
            "fletchers_quiver=",
            "flint_staff=attunement,unbreaking",
            "golden_boots=growth,mana_bank,protection,unbreaking",
            "golden_chestplate=growth,mana_bank,protection,unbreaking",
            "golden_helmet=growth,mana_bank,protection,unbreaking",
            "golden_leggings=growth,mana_bank,protection,unbreaking",
            "golden_sword=sharpness,unbreaking",
            "iron_axe=unbreaking",
            "iron_boots=growth,mana_bank,protection,unbreaking",
            "iron_chestplate=growth,mana_bank,protection,unbreaking",
            "iron_helmet=growth,mana_bank,protection,unbreaking",
            "iron_hoe=unbreaking",
            "iron_leggings=growth,mana_bank,protection,unbreaking",
            "iron_pickaxe=unbreaking",
            "iron_shovel=unbreaking",
            "iron_sword=sharpness,unbreaking",
            "keen_charm=",
            "knell_beam=attunement,unbreaking",
            "lapis_staff=attunement,unbreaking",
            "leather_boots=growth,mana_bank,protection,unbreaking",
            "leather_chestplate=growth,mana_bank,protection,unbreaking",
            "leather_helmet=growth,mana_bank,protection,unbreaking",
            "leather_leggings=growth,mana_bank,protection,unbreaking",
            "living_lantern=attunement,unbreaking",
            "locust=deft_hands,expanded_quiver,power,unbreaking",
            "mending_charm=",
            "netherite_boots=growth,mana_bank,protection,unbreaking",
            "netherite_chestplate=growth,mana_bank,protection,unbreaking",
            "netherite_helmet=growth,mana_bank,protection,unbreaking",
            "netherite_leggings=growth,mana_bank,protection,unbreaking",
            "netherite_sword=sharpness,unbreaking",
            "sages_scroll=",
            "shears=unbreaking",
            "shield=bulwark,thorns,unbreaking",
            "short_bow=deft_hands,expanded_quiver,power,unbreaking",
            "stone_sword=sharpness,unbreaking",
            "volley_stone=attunement,unbreaking",
            "ward_charm=",
            "withered_shortbow=deft_hands,expanded_quiver,power,unbreaking",
            "wooden_sword=sharpness,unbreaking"

    );

    private record Loaded(List<GearDefinition> gear, EnchantRegistry real, EnchantRegistry dev, CandidateCascade cascade,
                          int weapons, int shields, int armor, int tools, int accessories) {}

    private static Loaded load() {
        Logger q = Logger.getLogger("UniqueSlotContentTest-" + System.nanoTime());
        q.setUseParentHandlers(false);
        File c = Path.of("src", "main", "resources", "content").toFile();
        var elements = new ElementLoader(q).loadAll(new File(c, "elements"));
        var w = new WeaponLoader(q).loadAll(new File(c, "weapons"));
        var s = new ShieldLoader(q).loadAll(new File(c, "shields"));
        var a = new ArmorLoader(q).loadAll(new File(c, "armor"));
        var t = new ToolLoader(q).loadAll(new File(c, "tools"));
        var x = new AccessoryLoader(q, 100.0).loadAll(new File(c, "accessories"), id -> false);
        List<GearDefinition> gear = new ArrayList<>();
        gear.addAll(w.all());
        gear.addAll(s.all());
        gear.addAll(a.all());
        gear.addAll(t.all());
        gear.addAll(x.all());
        EnchantRegistry real = new EnchantLoader(q, id -> elements.find(id).isPresent(), false).loadAll(new File(c, "enchants"));
        EnchantRegistry dev = new EnchantLoader(q, id -> elements.find(id).isPresent(), true).loadAll(new File(c, "enchants"));
        CandidateCascade cascade = new EnchantCandidatesLoader(q).load(new File(c, "enchant_candidates.yml"));
        return new Loaded(gear, real, dev, cascade, w.size(), s.size(), a.size(), t.size(), x.size());
    }

    private static List<String> ids(List<EnchantRoll.Rollable> pool) {
        return pool.stream().map(EnchantRoll.Rollable::enchantId).toList();
    }

    @Test
    void theShippedSetIsRecountedWithAControl() {
        Loaded l = load();
        assertEquals(24, l.weapons());
        assertEquals(1, l.shields());
        assertEquals(24, l.armor());
        assertEquals(5, l.tools());
        assertEquals(6, l.accessories());
        assertEquals(60, l.gear().size());
        assertEquals(60, MASTER_POOLS.size(), "the snapshot covers the same 60");
        assertEquals(11, l.real().size(), "a real server: the eleven");
        assertEquals(12, l.dev().size(), "the control: a dev server adds the one dev_ example");
        assertNotSame(CandidateCascade.ONE, l.cascade(), "Ben's file loaded, not the fallback");
    }

    /** Slots 1-3's pool, for every shipped gear, is master's pool exactly: the unique mechanism took nothing from it. */
    @Test
    void theNormalPoolOfEveryShippedGearIsUnchangedFromMaster() {
        Loaded l = load();
        var roster = EnchantRollItems.roster(l.dev().all());
        List<String> after = new ArrayList<>();
        for (GearDefinition g : l.gear()) {
            var cls = GearItems.gearClassOf(g);
            List<String> pool = cls == null ? List.of() : ids(EnchantRoll.normalPoolFor(EnchantRollItems.has(g), cls, roster));
            after.add(g.id() + "=" + String.join(",", pool));
        }
        after.sort(null);
        assertEquals(MASTER_POOLS, after);
    }

    /** U5 over every shipped gear: a weapon's accessor returns its element; every other kind, empty. */
    @Test
    void theElementAccessorAnswersForEveryKind() {
        Loaded l = load();
        int withElement = 0;
        for (GearDefinition g : l.gear()) {
            if (g instanceof WeaponDefinition w) {
                assertEquals(Optional.of(w.element()), g.gearElement(), g.id());
                withElement++;
            } else {
                assertEquals(Optional.empty(), g.gearElement(), g.id() + " declares no element");
            }
        }
        assertEquals(24, withElement);
    }

    /** The unique pool: the dev example on every KINETIC weapon (16 of them), nothing anywhere else. */
    @Test
    void theUniquePoolIsTheDevExampleOnKineticWeaponsAndEmptyElsewhere() {
        Loaded l = load();
        var roster = EnchantRollItems.roster(l.dev().all());
        int kinetic = 0;
        for (GearDefinition g : l.gear()) {
            var cls = GearItems.gearClassOf(g);
            List<String> pool = cls == null ? List.of()
                    : ids(EnchantRoll.uniquePoolFor(g.gearElement(), EnchantRollItems.has(g), cls, roster));
            if (g.gearElement().equals(Optional.of("kinetic"))) {
                assertEquals(List.of("dev_kinetic_example"), pool, g.id());
                kinetic++;
            } else {
                assertEquals(List.of(), pool, g.id());
            }
        }
        assertEquals(16, kinetic, "the kinetic weapons: grep -l '^element: kinetic' content/weapons/*.yml | wc -l");
        // And on a REAL server no gear has a unique pool at all (no real enchant declares element: yet).
        var realRoster = EnchantRollItems.roster(l.real().all());
        for (GearDefinition g : l.gear()) {
            var cls = GearItems.gearClassOf(g);
            if (cls == null) continue;
            assertEquals(List.of(), EnchantRoll.uniquePoolFor(g.gearElement(), EnchantRollItems.has(g), cls, realRoster), g.id());
        }
    }

    /**
     * 200 seeded rolls of every shipped gear through Ben's real cascade file: four slots; slot 0 only from the unique pool,
     * slots 1-3 only from the normal pool; at most four candidates a slot.
     */
    @Test
    void overEveryShippedGearSlotZeroIsUniqueOnlyAndTheOthersNormalOnly() {
        Loaded l = load();
        var roster = EnchantRollItems.roster(l.dev().all());
        Random random = new Random(383L);
        int slotZeroFilled = 0, normalFilled = 0;
        for (GearDefinition g : l.gear()) {
            var cls = GearItems.gearClassOf(g);
            if (cls == null) continue;
            List<String> unique = ids(EnchantRoll.uniquePoolFor(g.gearElement(), EnchantRollItems.has(g), cls, roster));
            List<String> normal = ids(EnchantRoll.normalPoolFor(EnchantRollItems.has(g), cls, roster));
            for (int i = 0; i < 200; i++) {
                EnchantState s = EnchantRoll.roll(g.gearElement(), EnchantRollItems.has(g), cls, roster, l.cascade(),
                        random::nextDouble);
                assertEquals(4, s.slots().size(), g.id());
                for (int slot = 0; slot < 4; slot++) {
                    assertTrue(s.slots().get(slot).candidates().size() <= 4);
                    for (EnchantCandidate cand : s.slots().get(slot).candidates()) {
                        List<String> allowed = slot == 0 ? unique : normal;
                        assertTrue(allowed.contains(cand.enchantId()), g.id() + " slot " + slot + ": " + cand.enchantId());
                        if (slot == 0) slotZeroFilled++; else normalFilled++;
                    }
                }
            }
        }
        assertTrue(slotZeroFilled > 0 && normalFilled > 0, "the control: both kinds of slot were filled");
    }
}
