package io.github.butterflysmp.rpg.core.weapon;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NETHERITE AT THE SMITHING TABLE (PLAN-weapon-tiers.md 1.2, Ben's answer 4: "Yes"; the seat's brief, part g). The pure
 * mapping "base weapon id + template + addition -> the upgrade's weapon id, or refuse", from {@code content/smithing.yml}.
 *
 * <ul>
 *   <li><b>UPGRADE:</b> a minted base with a declared rule. The Paper side mints the upgrade and carries the base's
 *       enchants, wear (clamped to the new maximum) and gear score.</li>
 *   <li><b>REFUSE:</b> a minted base with no rule for these inputs. There is no result, so nothing can be taken and no
 *       ingot is spent. Before this, a minted weapon in a smithing table silently lost its identity (the survey's
 *       2e).</li>
 *   <li><b>NOT_OURS:</b> an unminted base (a plain item). Vanilla smiths it, so vanilla's own netherite upgrades and
 *       trims are untouched.</li>
 * </ul>
 *
 * <p>Template and addition are material tokens, normalised as craft results are ({@link CraftResultToken#token}), so
 * {@code minecraft:NETHERITE_INGOT} and {@code netherite_ingot} agree.
 */
public final class SmithingUpgrades {

    /** One declared upgrade. Every field is required; the tokens are normalised. */
    public record Upgrade(String base, String template, String addition, String result) {
        public Upgrade {
            if (base == null || base.isBlank() || template == null || template.isBlank() || addition == null
                    || addition.isBlank() || result == null || result.isBlank()) {
                throw new IllegalArgumentException("a smithing upgrade needs base, template, addition and result");
            }
            template = CraftResultToken.token(template);
            addition = CraftResultToken.token(addition);
        }
    }

    /** What the smithing table does with these inputs. */
    public enum Kind { UPGRADE, REFUSE, NOT_OURS }

    /** The decision; {@code result} is the upgrade's weapon id for UPGRADE, else null. */
    public record Decision(Kind kind, String result) {}

    private final Map<String, Upgrade> byKey = new LinkedHashMap<>();

    /** @throws IllegalArgumentException if two rules share a base, template and addition (which would be meant?) */
    public SmithingUpgrades(List<Upgrade> upgrades) {
        for (Upgrade u : upgrades == null ? List.<Upgrade>of() : upgrades) {
            Upgrade clash = byKey.putIfAbsent(key(u.base(), u.template(), u.addition()), u);
            if (clash != null) {
                throw new IllegalArgumentException("two smithing upgrades for '" + u.base() + "' + " + u.template()
                        + " + " + u.addition() + ": '" + clash.result() + "' and '" + u.result() + "'");
            }
        }
    }

    private static String key(String base, String template, String addition) {
        return base + "|" + template + "|" + addition;
    }

    /**
     * @param baseWeaponId the base item's weapon id, or null for an unminted item
     * @param template     the template slot's material token, or null if empty
     * @param addition     the addition slot's material token, or null if empty
     */
    public Decision decide(String baseWeaponId, String template, String addition) {
        if (baseWeaponId == null) return new Decision(Kind.NOT_OURS, null);
        if (template == null || addition == null) return new Decision(Kind.REFUSE, null);
        Upgrade u = byKey.get(key(baseWeaponId, CraftResultToken.token(template), CraftResultToken.token(addition)));
        return u == null ? new Decision(Kind.REFUSE, null) : new Decision(Kind.UPGRADE, u.result());
    }

    /** Every declared upgrade, in content order. */
    public List<Upgrade> all() {
        return List.copyOf(byKey.values());
    }

    public int size() {
        return byKey.size();
    }
}
