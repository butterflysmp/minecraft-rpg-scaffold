package io.github.butterflysmp.rpg.core.ability.effect;

/**
 * Reports a direct damage hit as it lands: how much, and wearing what element.
 *
 * <p>Replaces a {@code DoubleConsumer}. The sweep rider is the one production reader, and it needs
 * both halves for the same reason: the swept bystanders take a fraction of the primary's number AND
 * should wear the primary's element, since a fire sweep is fire damage landing on three mobs.
 *
 * <p><b>THE TWO ARRIVE TOGETHER BECAUSE THEY MUST NOT BE SOURCED SEPARATELY.</b> The obvious
 * alternative -- keep the amount here and read the weapon's declared element at the sweep site --
 * is the second-derivation this seam exists to prevent, and it can already disagree with the truth:
 * {@code ability_stone.yml} declares {@code element: kinetic} at the weapon level while its nested
 * damage effect declares {@code element: fire}. A weapon's element and its damage effect's element
 * are different facts, and only one of them is what actually hit.
 *
 * <p><b>A transposed implementation is a COMPILE ERROR, not a test.</b> {@code double} and
 * {@code String} cannot be swapped, so no row is written for it -- a mutation that cannot be
 * expressed is a property the type enforces, not a guard that is missing. What IS expressible, and
 * is therefore what the tests pin, is that the amount and the element come from the SAME call: a
 * stash that took one from one invocation and the other from another would type-check perfectly.
 *
 * <p>Threading contract is unchanged from the {@code DoubleConsumer} it replaces: this runs
 * SYNCHRONOUSLY inside the damage arm, on the thread that entered the applier, and inside the
 * liveness and positive-amount gate -- so a refused hit reports nothing at all.
 */
@FunctionalInterface
public interface DirectDamage {

    /**
     * @param amount  what the arm actually dealt, after every multiplier the applier applies
     * @param element the content id the damage effect declared. Never null from content --
     *                {@code AbilitySchema.str} THROWS on a missing {@code element}, so a damage
     *                effect without one is a named, skipped file rather than a null reaching here.
     *                No guard is written against it for that reason.
     */
    void accept(double amount, String element);
}
