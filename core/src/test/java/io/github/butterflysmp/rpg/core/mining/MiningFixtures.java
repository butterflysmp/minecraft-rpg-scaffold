package io.github.butterflysmp.rpg.core.mining;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * S2's shared test content: the four vein sizes and the iron ore, as the shipped YAML authors them
 * (PLAN-mining.md 5.12). FACTORY METHODS, not static fields: a fixture that throws then fails only the
 * rows that call it, never a whole class's initialiser.
 *
 * <p>Sizes, from Ben's design doc as relayed by the seat (2026-11-14): small 3 x 2 with 2-3 spots,
 * medium 5 x 3 with 4-5, large 7 x 3 with 6-7, extra_large 7 x 4 with 8-9. Weights 50 / 30 / 15 / 5
 * (PROVISIONAL). Bonuses 5 / 6 / 8 / 10 (Ben: "This is fine for now").
 */
final class MiningFixtures {

    private MiningFixtures() {}

    static VeinSize small() { return new VeinSize("small", 3, 2, 2, 3); }

    static VeinSize medium() { return new VeinSize("medium", 5, 3, 4, 5); }

    static VeinSize large() { return new VeinSize("large", 7, 3, 6, 7); }

    static VeinSize extraLarge() { return new VeinSize("extra_large", 7, 4, 8, 9); }

    static VeinSizeRegistry sizes() {
        VeinSizeRegistry r = new VeinSizeRegistry();
        r.register(small());
        r.register(medium());
        r.register(large());
        r.register(extraLarge());
        return r;
    }

    /** The four ids in authored order. */
    static Map<String, Integer> weights() {
        Map<String, Integer> w = new LinkedHashMap<>();
        w.put("small", 50);
        w.put("medium", 30);
        w.put("large", 15);
        w.put("extra_large", 5);
        return w;
    }

    static Map<String, Integer> bonuses() {
        Map<String, Integer> b = new LinkedHashMap<>();
        b.put("small", 5);
        b.put("medium", 6);
        b.put("large", 8);
        b.put("extra_large", 10);
        return b;
    }

    static Map<String, String> patchModels() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("small", "rpg:mining/iron_patch_small");
        m.put("medium", "rpg:mining/iron_patch_medium");
        m.put("large", "rpg:mining/iron_patch_large");
        m.put("extra_large", "rpg:mining/iron_patch_extra_large");
        return m;
    }

    /** The shipped iron ore. */
    static OreDefinition iron() {
        return ore(weights(), bonuses(), patchModels());
    }

    /** An iron ore listing only {@code weights}' sizes, with matching bonuses and models. */
    static OreDefinition ironListing(Map<String, Integer> weights) {
        Map<String, Integer> bonus = new LinkedHashMap<>();
        Map<String, String> models = new LinkedHashMap<>();
        for (String id : weights.keySet()) {
            bonus.put(id, bonuses().get(id));
            models.put(id, patchModels().get(id));
        }
        return ore(weights, bonus, models);
    }

    static OreDefinition ore(Map<String, Integer> weights, Map<String, Integer> bonus, Map<String, String> models) {
        return new OreDefinition("iron", "Iron", "raw_iron", 2, 4, bonus, 1200, weights, models,
                "rpg:mining/spot", "rpg:mining/crack",
                "minecraft:block.stone.hit", "minecraft:block.stone.break", "minecraft:entity.villager.no",
                "minecraft:poof");
    }

    /** A NORTH slot anchored at (10, 64, 20) whose maximum is {@code max}. */
    static Slot slot(VeinSize max) {
        return Slot.of("w", new FaceFrame(10, 64, 20, WallFace.NORTH, max.width(), max.height()), "iron", max.id());
    }
}
