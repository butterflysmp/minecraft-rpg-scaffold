package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The armor slot axis, and the one property of it that is load-bearing far beyond its own file.
 *
 * The headline is {@link #everyConstantIsNamedForTheEquipmentSlotTokenTheDefenseMapIsKeyedBy}.
 * {@code DefenseModifierItems.desiredModifiers} keys its map by {@code EquipmentSlot.name()}, and
 * {@code ModifierReconciler} matches applied modifiers to desired ones BY THAT STRING. So these
 * four constant names are a wire format shared with Bukkit's enum, not a local naming choice.
 *
 * core cannot import EquipmentSlot to assert the equality directly -- that is the whole reason this
 * enum exists -- so the tokens are restated here as literals. That is not a weaker test, it is the
 * only shape available, and it fails the instant someone renames a constant to something that reads
 * better.
 *
 * Each test names the mutation it forces red.
 */
class ArmorSlotTest {

    // --- The wire format ------------------------------------------------------------------------

    @Test
    void everyConstantIsNamedForTheEquipmentSlotTokenTheDefenseMapIsKeyedBy() {
        // These four literals are org.bukkit.inventory.EquipmentSlot's own names for the armor
        // slots. Renaming ArmorSlot.HEAD to HELMET would read better, compile everywhere, and
        // silently stop matching the reconciler's keys.
        assertEquals("HEAD", ArmorSlot.HEAD.name());
        assertEquals("CHEST", ArmorSlot.CHEST.name());
        assertEquals("LEGS", ArmorSlot.LEGS.name());
        assertEquals("FEET", ArmorSlot.FEET.name());
        // Mutation: rename any constant (HEAD -> HELMET) -> reddens.
    }

    @Test
    void theAxisIsExactlyTheFourWearableSlots() {
        // An explicit enumeration, deliberately, the way GearClassTest pins its own axis: adding a
        // fifth slot must be a conscious act that reddens a test, not a quiet widening. BODY and
        // SADDLE are EquipmentSlot values this axis must never grow to include -- they are a
        // horse's barding, and DefenseModifierItems names its four slots explicitly to avoid them.
        assertArrayEquals(
                new ArmorSlot[]{ArmorSlot.HEAD, ArmorSlot.CHEST, ArmorSlot.LEGS, ArmorSlot.FEET},
                ArmorSlot.values());
        // Mutation: add a fifth constant -> reddens.
    }

    // --- fromName -------------------------------------------------------------------------------

    @Test
    void fromNameIsCaseInsensitiveBecauseContentAuthorsWriteLowercase() {
        // The content files author `head:`, `chest:` -- lowercase, as YAML keys read.
        assertSame(ArmorSlot.HEAD, ArmorSlot.fromName("head"));
        assertSame(ArmorSlot.CHEST, ArmorSlot.fromName("CHEST"));
        assertSame(ArmorSlot.LEGS, ArmorSlot.fromName("Legs"));
        assertSame(ArmorSlot.FEET, ArmorSlot.fromName("fEeT"));
        // Mutation: use equals instead of equalsIgnoreCase -> every shipped tier file's four
        // lowercase keys stop resolving -> reddens.
    }

    @Test
    void fromNameReturnsNullOnAMissSoTheLoaderDecidesWhatABadSlotMeans() {
        // Null, not a throw and not a default: the loader turns it into a NAMED, SKIPPED file. A
        // default of HEAD would load a typo'd chestplate as a helmet.
        assertNull(ArmorSlot.fromName("helmet"));
        assertNull(ArmorSlot.fromName("torso"));
        assertNull(ArmorSlot.fromName(""));
        assertNull(ArmorSlot.fromName(null));
        // Mutation: return HEAD instead of null on a miss -> a misspelt slot silently mints in the
        // wrong slot -> reddens.
    }

    @Test
    void everyConstantRoundTripsThroughItsOwnName() {
        // A discovery-shaped check rather than four literals, so a new constant is covered the day
        // it is added. It asserts a non-empty walk for the reason CLAUDE.md records twice: a loop
        // over an empty set reads exactly like a loop that passed.
        List<ArmorSlot> all = List.of(ArmorSlot.values());
        assertEquals(4, all.size(), "the walk must not be empty or short");
        for (ArmorSlot slot : all) {
            assertSame(slot, ArmorSlot.fromName(slot.name()), "round trip for " + slot);
        }
        // Mutation: have fromName compare against ordinal() rather than name() -> reddens.
    }
}
