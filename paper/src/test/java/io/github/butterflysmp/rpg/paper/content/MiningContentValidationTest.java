package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.mining.OreDefinition;
import io.github.butterflysmp.rpg.core.mining.VeinSize;
import io.github.butterflysmp.rpg.core.mining.VeinSizeRegistry;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code ContentValidator.validateMining}: the checks that need another registry or the Bukkit
 * registry, so they arrive as seams and the walk stays testable with no server.
 *
 * <p>Every refusal row starts from {@link #aCleanOreHasNoProblems}'s inputs and breaks ONE thing,
 * and asserts exactly one problem naming it, so a row cannot pass on a different fault.
 */
class MiningContentValidationTest {

    private static final Set<String> SOUNDS = Set.of("minecraft:block.stone.hit", "minecraft:block.stone.break",
            "minecraft:entity.villager.no");
    private static final Predicate<NamespacedKey> SOUND_EXISTS = key -> SOUNDS.contains(key.toString());
    private static final Predicate<String> ITEM_EXISTS = name -> name.equals("raw_iron");
    private static final Predicate<NamespacedKey> PARTICLE_USABLE = key -> key.toString().equals("minecraft:poof");
    private static final Set<String> PACK = Set.of("rpg:mining/iron_patch", "rpg:mining/spot", "rpg:mining/crack");
    private static final Predicate<NamespacedKey> PACK_HAS = key -> PACK.contains(key.toString());

    private static ContentValidator validator() {
        return new ContentValidator(new VisualRegistry(), new StatusRegistry(), new ElementRegistry(),
                key -> true, SOUND_EXISTS);
    }

    private static VeinSizeRegistry small() {
        VeinSizeRegistry sizes = new VeinSizeRegistry();
        sizes.register(new VeinSize("small", 3, 2, 2, 3));
        return sizes;
    }

    private static OreDefinition ore(String item, List<String> sizes, String patch, String miss, String brk) {
        Map<String, Integer> bonus = new java.util.HashMap<>();
        for (String s : sizes) bonus.put(s, 5);
        return ore(item, sizes, patch, miss, brk, "minecraft:poof");
    }

    private static OreDefinition ore(String item, List<String> sizes, String patch, String miss, String brk,
                                     String particle) {
        Map<String, Integer> bonus = new java.util.HashMap<>();
        for (String s : sizes) bonus.put(s, 5);
        return ore(item, sizes, patch, miss, brk, "minecraft:entity.villager.no", particle);
    }

    /** S2: weights and per-size patch models built from the size list; every size gets {@code patch}. */
    private static OreDefinition ore(String item, List<String> sizes, String patch, String miss, String brk,
                                     String denied, String particle) {
        Map<String, Integer> bonus = new java.util.LinkedHashMap<>();
        Map<String, Integer> weights = new java.util.LinkedHashMap<>();
        Map<String, String> models = new java.util.LinkedHashMap<>();
        for (String s : sizes) {
            bonus.put(s, 5);
            weights.put(s, 10);
            models.put(s, patch);
        }
        return new OreDefinition("iron", "Iron", item, 2, 4, bonus, 1200, weights,
                models, "rpg:mining/spot", "rpg:mining/crack", miss, brk, denied, particle);
    }

    private static OreDefinition clean() {
        return ore("raw_iron", List.of("small"), "rpg:mining/iron_patch",
                "minecraft:block.stone.hit", "minecraft:block.stone.break");
    }

    private static List<String> check(OreDefinition ore) {
        return validator().validateMining(List.of(ore), small(), ITEM_EXISTS, PARTICLE_USABLE, PACK_HAS);
    }

    private static void assertOne(List<String> problems, String needle) {
        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains(needle), problems.get(0));
    }

    @Test
    void aCleanOreHasNoProblems() {
        assertEquals(List.of(), check(clean()));
    }

    @Test
    void aSizeThatDidNotLoadIsNamed() {
        assertOne(check(ore("raw_iron", List.of("small", "medium"), "rpg:mining/iron_patch",
                "minecraft:block.stone.hit", "minecraft:block.stone.break")), "'medium'");
    }

    @Test
    void aPayoutThatIsNotAnItemIsNamed() {
        assertOne(check(ore("raw_irn", List.of("small"), "rpg:mining/iron_patch",
                "minecraft:block.stone.hit", "minecraft:block.stone.break")), "'raw_irn'");
    }

    @Test
    void aMissSoundThatDoesNotResolveIsNamed() {
        assertOne(check(ore("raw_iron", List.of("small"), "rpg:mining/iron_patch",
                "minecraft:block.stone.hitt", "minecraft:block.stone.break")), "sounds.miss");
    }

    @Test
    void aBreakSoundThatIsNotEvenAKeyIsNamed() {
        assertOne(check(ore("raw_iron", List.of("small"), "rpg:mining/iron_patch",
                "minecraft:block.stone.hit", "Not A Key")), "sounds.break");
    }

    @Test
    void aModelThePackDoesNotShipIsNamed() {
        // Well-formed, so S1-a's check passed it; the pack has no such item definition.
        assertOne(check(ore("raw_iron", List.of("small"), "rpg:mining/gold_patch",
                "minecraft:block.stone.hit", "minecraft:block.stone.break")), "not in the project pack");
        // Mutation: skip the packHasModel test -> no problem -> reddens.
    }

    @Test
    void aParticleThatNeedsDataOrDoesNotExistIsNamed() {
        assertOne(check(ore("raw_iron", List.of("small"), "rpg:mining/iron_patch",
                "minecraft:block.stone.hit", "minecraft:block.stone.break", "minecraft:block")), "particles.break");
        assertOne(check(ore("raw_iron", List.of("small"), "rpg:mining/iron_patch",
                "minecraft:block.stone.hit", "minecraft:block.stone.break", "Not A Key")), "particles.break");
    }

    @Test
    void aDeniedSoundThatDoesNotResolveIsNamed() {
        // S2, the seat's ruling 8: the new sound key goes through the same check as the other two.
        assertOne(check(ore("raw_iron", List.of("small"), "rpg:mining/iron_patch",
                "minecraft:block.stone.hit", "minecraft:block.stone.break", "minecraft:entity.villager.nope",
                "minecraft:poof")), "sounds.denied");
        // Mutation: drop the sounds.denied check -> no problem -> reddens.
    }

    @Test
    void eachSizesPatchModelIsCheckedAndTheMissingOneIsNamedBySize() {
        // S2, the seat's ruling 6: one model per size. Small's is in the pack; medium's is not.
        VeinSizeRegistry sizes = small();
        sizes.register(new VeinSize("medium", 5, 3, 4, 5));
        Map<String, Integer> bonus = new java.util.LinkedHashMap<>();
        Map<String, Integer> weights = new java.util.LinkedHashMap<>();
        Map<String, String> models = new java.util.LinkedHashMap<>();
        bonus.put("small", 5);
        bonus.put("medium", 6);
        weights.put("small", 50);
        weights.put("medium", 30);
        models.put("small", "rpg:mining/iron_patch");
        models.put("medium", "rpg:mining/gold_patch");
        OreDefinition ore = new OreDefinition("iron", "Iron", "raw_iron", 2, 4, bonus, 1200, weights, models,
                "rpg:mining/spot", "rpg:mining/crack", "minecraft:block.stone.hit", "minecraft:block.stone.break",
                "minecraft:entity.villager.no", "minecraft:poof");
        assertOne(validator().validateMining(List.of(ore), sizes, ITEM_EXISTS, PARTICLE_USABLE, PACK_HAS),
                "models.patch.medium");
        // Mutation: check only the first size's model -> no problem -> reddens.
    }

    @Test
    void aMalformedModelKeyIsNamed() {
        assertOne(check(ore("raw_iron", List.of("small"), "RPG Patch",
                "minecraft:block.stone.hit", "minecraft:block.stone.break")), "models.patch");
    }
}
