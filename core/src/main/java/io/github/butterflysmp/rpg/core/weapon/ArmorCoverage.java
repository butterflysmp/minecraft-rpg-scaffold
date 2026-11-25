package io.github.butterflysmp.rpg.core.weapon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A2 (the seat, 2026-11-25): the boot check that replaced "authored Defense equals vanilla's".
 *
 * <p>Once the authored number IS the Defense (A1), comparing it with vanilla's would shout about every piece by
 * design. What can still go wrong is COVERAGE: a server armor material with no content piece silently keeps vanilla's
 * points (the scan's fallback) and can never be part of a set; a content piece naming a material the server lacks, or
 * authored under a slot that material does not go in, mints a leather fallback or reconciles into no slot at all.
 *
 * <p>Pure: the server's armor materials arrive as a map, read in paper from the item registry, so the decision is
 * testable here with no server.
 */
public final class ArmorCoverage {

    private ArmorCoverage() {}

    /**
     * @param serverArmor every armor item the server has: material token to the slot it is worn in
     * @param pieces      every loaded content piece
     * @param excluded    materials ruled out of content by name (C2: the turtle helmet, Ben 2026-11-25)
     * @return one line per problem, empty when the roster is complete
     */
    public static List<String> problems(Map<String, ArmorSlot> serverArmor, Collection<ArmorDefinition> pieces,
                                        Set<String> excluded) {
        List<String> problems = new ArrayList<>();
        Map<String, ArmorDefinition> byMaterial = new HashMap<>();
        for (ArmorDefinition p : pieces) byMaterial.put(p.material(), p);

        for (Map.Entry<String, ArmorSlot> armor : serverArmor.entrySet()) {
            if (excluded.contains(armor.getKey())) continue;
            if (!byMaterial.containsKey(armor.getKey())) {
                problems.add("the server's armor '" + armor.getKey() + "' (" + armor.getValue()
                        + ") has no content piece: it keeps vanilla's points and belongs to no set");
            }
        }
        for (ArmorDefinition p : pieces) {
            if (excluded.contains(p.material())) {
                problems.add("armor '" + p.id() + "' names '" + p.material() + "', which is excluded from content");
                continue;
            }
            ArmorSlot actual = serverArmor.get(p.material());
            if (actual == null) {
                problems.add("armor '" + p.id() + "' names '" + p.material()
                        + "', which is not armor the server has; it will mint as a leather fallback");
            } else if (actual != p.slot()) {
                problems.add("armor '" + p.id() + "' is authored under " + p.slot() + " but '" + p.material()
                        + "' is worn in " + actual + "; it would contribute nothing");
            }
        }
        return problems;
    }
}
