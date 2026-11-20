package io.github.butterflysmp.rpg.core.weapon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * CONVERT ON ARRIVAL (the seat's ruling, 2026-11-20; PLAN-weapon-tiers.md 1.2, Ben's answer 5, "Yes": a sword obtained by
 * ANY route is minted). Which vanilla materials a WEAPON claims when an untagged item of that material reaches a player,
 * and the pure decision for one item.
 *
 * <ul>
 *   <li><b>A weapon claims by its {@code craft_result}</b>, exactly as the craft index resolved it, so a contested craft
 *       claim (dropped there) claims nothing here either.</li>
 *   <li><b>And by being the RESULT of a smithing upgrade</b>: the netherite sword is not crafted, but a netherite sword
 *       found in loot must still convert. Its material is claimed by the upgrade's result weapon.</li>
 *   <li><b>Fail closed:</b> two weapons claiming one material through the two routes claim NOTHING, and the caller logs
 *       {@code Refusing arrival claim '<m>'}.</li>
 * </ul>
 *
 * <p>Only weapons claim on arrival. A shield's, armour piece's or tool's {@code craft_result} still mints on craft only;
 * part h is the swords'.
 */
public final class ArrivalClaims {

    /** What happens to one item reaching a player's inventory. */
    public enum Kind { MINT, LEAVE_TAGGED, LEAVE_UNCLAIMED }

    /** The decision; {@code weaponId} is the weapon to mint for MINT, else null. */
    public record Decision(Kind kind, String weaponId) {}

    private final Map<String, String> byMaterial;

    private ArrivalClaims(Map<String, String> byMaterial) {
        this.byMaterial = byMaterial;
    }

    /**
     * @param weapons   every weapon definition
     * @param crafts    the craft index (its contests and refusals already applied)
     * @param upgrades  the smithing upgrades; each result weapon claims its own material
     * @param onProblem called once per contested material, naming it
     */
    public static ArrivalClaims build(Collection<WeaponDefinition> weapons, CraftResultIndex crafts,
                                      SmithingUpgrades upgrades, Consumer<String> onProblem) {
        Map<String, List<String>> claimants = new LinkedHashMap<>();
        for (WeaponDefinition w : weapons) {
            if (w.craftResult().isEmpty()) continue;
            String material = w.craftResult().get();
            crafts.forResult(material).filter(g -> g.id().equals(w.id()))
                    .ifPresent(g -> add(claimants, material, w.id()));
        }
        for (SmithingUpgrades.Upgrade u : upgrades.all()) {
            for (WeaponDefinition w : weapons) {
                if (w.id().equals(u.result())) add(claimants, CraftResultToken.token(w.material()), w.id());
            }
        }
        Map<String, String> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : claimants.entrySet()) {
            if (e.getValue().size() == 1) {
                resolved.put(e.getKey(), e.getValue().get(0));
            } else {
                onProblem.accept("Refusing arrival claim '" + e.getKey() + "': weapons " + e.getValue()
                        + " all claim it, so neither is minted on arrival.");
            }
        }
        return new ArrivalClaims(resolved);
    }

    private static void add(Map<String, List<String>> claimants, String material, String id) {
        List<String> ids = claimants.computeIfAbsent(material, k -> new ArrayList<>());
        if (!ids.contains(id)) ids.add(id);
    }

    /**
     * @param tagged   whether the item already carries any gear id (minted)
     * @param material the item's material token, or null for an empty slot
     */
    public Decision decide(boolean tagged, String material) {
        if (tagged) return new Decision(Kind.LEAVE_TAGGED, null);
        String id = material == null ? null : byMaterial.get(CraftResultToken.token(material));
        return id == null ? new Decision(Kind.LEAVE_UNCLAIMED, null) : new Decision(Kind.MINT, id);
    }

    /** How many materials convert on arrival. */
    public int size() {
        return byMaterial.size();
    }

    /** The claimed materials and their weapons, in build order (for the boot line). */
    public Map<String, String> all() {
        return Map.copyOf(byMaterial);
    }
}
