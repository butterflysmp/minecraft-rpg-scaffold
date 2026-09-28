package io.github.butterflysmp.rpg.core.mob;

import java.util.List;

/**
 * A mob's seeded melee attack: its {@code ATTACK_DAMAGE} base plus its held main-hand weapon's ADD
 * modifiers (the seat's ruling of 2026-09-28, PLAN-mob-scaling.md §6 F16).
 *
 * <p><b>Why not the live attribute.</b> The seed runs inside {@code EntityAddToWorldEvent}, and by then
 * vanilla has folded the held weapon into the attribute only if a player was in tracking range to pair
 * with the mob ({@code ServerEntity.sendPairingData} calls {@code detectEquipmentUpdates}; read with
 * {@code javap}). So the live value was a fact about who stood nearby. Computing base + weapon from the
 * item makes the seed the same at any distance, and equal to the hit vanilla deals once the mob has
 * ticked.
 *
 * <p><b>ADD only.</b> Every vanilla weapon's attack modifier is {@code ADD_VALUE} (swords, tools and axes,
 * spears, the trident: each builder read from the pinned jar). A modifier with any other operation is
 * left OUT and COUNTED, because where it would sit in vanilla's order (add, then multiply base, then
 * multiply total) against this sum is not guessed. The caller WARNs on a non-zero count.
 */
public final class MeleeSeed {

    private MeleeSeed() {}

    /** An attribute modifier's operation, as far as the seed cares: ADD, or anything else. */
    public enum Operation { ADD_VALUE, OTHER }

    /** One attack-damage modifier on the held weapon. */
    public record Modifier(Operation operation, double amount) {}

    /** The seeded attack, and how many modifiers were left out because they were not ADD. */
    public record Seed(double attack, int unpriced) {}

    /**
     * @param base     the mob's {@code ATTACK_DAMAGE} BASE value (never the live value)
     * @param mainHand the held main-hand item's attack-damage modifiers that apply to the main hand
     */
    public static Seed attack(double base, List<Modifier> mainHand) {
        double sum = base;
        int unpriced = 0;
        for (Modifier m : mainHand) {
            if (m.operation() == Operation.ADD_VALUE) sum += m.amount();
            else unpriced++;
        }
        return new Seed(sum, unpriced);
    }
}
