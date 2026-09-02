package io.github.butterflysmp.rpg.core.weapon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Which gear definition a crafted vanilla item should become.
 *
 * <p>The reverse of everything else in this package: every other gear lookup is by id, and this one
 * answers "the player just crafted an {@code iron_chestplate} -- is that ours?". Built ONCE at boot
 * from all four registries, because the crafting preview runs several times a second and a scan per
 * craft would be a walk of every definition on every grid change.
 *
 * <h2>Keyed on {@code craft_result}, never on {@code material}</h2>
 *
 * A material is PRESENTATION. {@code WeaponDefinition.DEFAULT_MATERIAL} is {@code iron_sword} and
 * every sword-shaped weapon leaves it there, so materials are contested BY DESIGN -- shipped content
 * already has two weapons on {@code iron_sword} and always will. Keying on them would mean
 * {@code iron_sword} permanently contested, a boot warning firing forever about correct content, and
 * no sword ever minting. {@link GearDefinition#craftResult()} is an opt-in claim instead, so one
 * definition per result is a property of the data rather than a hope.
 *
 * <h2>A contested result is DROPPED, and that is the fail-safe direction</h2>
 *
 * When two definitions claim the same item, neither is indexed and crafting it yields plain vanilla.
 * Not first-wins: the loaders sort their files for determinism, so first-wins would be alphabetical
 * order making an economy decision -- and a player would receive whichever weapon sorted earlier,
 * silently, with a rename able to change it. A plain item costs a player nothing; they can craft
 * another. A wrong minted item is theirs forever.
 *
 * <p>Pure, and in {@code core}, deliberately. {@link GearDefinition#craftResult()} is a String token,
 * so nothing here needs Bukkit -- which means the collision policy, the normalisation and the
 * zero-guard all have real unit tests instead of joining the boot-gate-only list this project keeps
 * for everything on the crafting surface.
 */
public final class CraftResultIndex {

    private final Map<String, GearDefinition> byResult;
    private final int claimed;
    private final int contested;

    private CraftResultIndex(Map<String, GearDefinition> byResult, int claimed, int contested) {
        this.byResult = byResult;
        this.claimed = claimed;
        this.contested = contested;
    }

    /**
     * Walk every definition and index the ones that claim a craft result.
     *
     * @param gear      all gear, from all four registries. A definition with no claim is skipped.
     * @param onProblem called ONCE per contested result, naming every claimant, so the operator can
     *                  open the right files. The seam that keeps this class free of a Logger and
     *                  therefore unit-testable -- the same trade {@code ContentValidator} makes with
     *                  its predicates.
     */
    public static CraftResultIndex build(Collection<? extends GearDefinition> gear,
                                         Consumer<String> onProblem) {
        // LinkedHashMap so two boots agree on order, exactly as GearRegistry does and for the same
        // reason: the messages below are read in a log, and an order that shuffles per boot makes
        // two runs impossible to compare.
        Map<String, List<GearDefinition>> claimants = new LinkedHashMap<>();
        int claimed = 0;

        for (GearDefinition definition : gear) {
            Optional<String> claim = definition.craftResult();
            if (claim.isEmpty()) continue;
            claimed++;
            claimants.computeIfAbsent(claim.get(), key -> new ArrayList<>()).add(definition);
        }

        Map<String, GearDefinition> resolved = new LinkedHashMap<>();
        int contested = 0;

        for (Map.Entry<String, List<GearDefinition>> entry : claimants.entrySet()) {
            List<GearDefinition> claiming = entry.getValue();
            if (claiming.size() == 1) {
                resolved.put(entry.getKey(), claiming.get(0));
                continue;
            }

            contested++;
            StringBuilder ids = new StringBuilder();
            for (GearDefinition definition : claiming) {
                if (!ids.isEmpty()) ids.append(", ");
                ids.append('\'').append(definition.id()).append('\'');
            }
            onProblem.accept("Gear " + ids + " all claim craft_result '" + entry.getKey()
                    + "'. None of them will be minted on craft, because there is no way to tell "
                    + "which was meant. Give the key to exactly one of them.");
        }

        return new CraftResultIndex(resolved, claimed, contested);
    }

    /**
     * The definition a crafted item should become, or empty for an item nothing claims.
     *
     * <p>Normalises through {@link CraftResultToken#token} exactly as the build did, so a lookup
     * spelled {@code minecraft:IRON_SWORD} finds an entry authored as {@code iron_sword}. A lookup
     * that normalised differently from the build would miss every entry and the only symptom would
     * be that crafting mints nothing.
     */
    public Optional<GearDefinition> forResult(String materialToken) {
        if (materialToken == null) return Optional.empty();
        return Optional.ofNullable(byResult.get(CraftResultToken.token(materialToken)));
    }

    /** How many results are actually indexed. ZERO IS A DEFECT -- the boot says so out loud. */
    public int size() {
        return byResult.size();
    }

    /** How many definitions made a claim, contested ones included. The denominator in the boot log. */
    public int claimed() {
        return claimed;
    }

    /** How many results were dropped for being claimed twice. */
    public int contested() {
        return contested;
    }
}
