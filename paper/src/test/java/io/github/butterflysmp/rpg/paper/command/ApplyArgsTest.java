package io.github.butterflysmp.rpg.paper.command;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The one server-free slice of /rpg apply: defaults, the loop-safety clamp, and the
 * unknown-status error. The command tree is otherwise Bukkit-facing and boot-verified, but
 * "no numbers means soaked, 100, 1" is logic worth pinning so a wrong default can't ship green.
 */
class ApplyArgsTest {

    private static final Predicate<String> KNOWN = Set.of("soaked", "rooted", "scorch")::contains;

    @Test
    void noNumbersMeansTheDefaults() {
        var r = ApplyArgs.resolve("soaked", null, null, KNOWN);
        assertTrue(r.ok());
        assertEquals(new ApplyArgs("soaked", 100, 1), r.args());
    }

    @Test
    void durationOnlyLeavesStacksAtOne() {
        var r = ApplyArgs.resolve("soaked", 40, null, KNOWN);
        assertEquals(new ApplyArgs("soaked", 40, 1), r.args());
    }

    @Test
    void bothNumbersPassThrough() {
        var r = ApplyArgs.resolve("soaked", 100, 5, KNOWN);
        assertEquals(new ApplyArgs("soaked", 100, 5), r.args());
    }

    @Test
    void anUnknownStatusIsANamedErrorNotAThrow() {
        var r = ApplyArgs.resolve("nope", null, null, KNOWN);
        assertFalse(r.ok());
        assertNull(r.args());
        assertTrue(r.error().contains("nope"), r.error());
    }

    @Test
    void stacksAreClampedToTheLoopSafetyCap() {
        var r = ApplyArgs.resolve("soaked", 100, 50, KNOWN);
        assertEquals(ApplyArgs.MAX_STACKS, r.args().stacks(),
                "stacks clamped so the apply loop can't run pathologically");
    }

    /**
     * THE REFUSAL REPLY (the seat's ruling 5): an immune target gets "Refused", naming the type and the
     * status, never "Applied". A zombie is the control (not immune: no refusal), and so is a non-wither
     * status on a wither skeleton.
     *
     * <p>Mutation: return empty unconditionally -> the refusal assertion reddens.
     */
    @Test
    void anImmuneTargetIsRefusedByNameAndTheControlsAreNot() {
        var withering = new io.github.butterflysmp.rpg.paper.content.StatusDefinition.Wither(
                "withering", Set.of("wither_skeleton", "wither"));
        assertEquals(java.util.Optional.of("Refused: wither_skeleton is immune to withering"
                        + " (statuses/withering.yml); nothing was applied"),
                ApplyArgs.immunityRefusal(withering, "wither_skeleton"));
        assertTrue(ApplyArgs.immunityRefusal(withering, "zombie").isEmpty(), "a zombie is not immune");
        assertTrue(ApplyArgs.immunityRefusal(
                new io.github.butterflysmp.rpg.paper.content.StatusDefinition.Scorch("scorch"),
                "wither_skeleton").isEmpty(), "scorch has no immune list");
        assertTrue(ApplyArgs.immunityRefusal(null, "zombie").isEmpty(), "an unknown status is resolve's error");
    }
}
