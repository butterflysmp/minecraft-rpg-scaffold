package io.github.butterflysmp.rpg.core.enchant;

import io.github.butterflysmp.rpg.core.weapon.GearClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;

/**
 * The per-instance enchant roll: which candidates a piece of GEAR is born offering.
 *
 * <h2>THE SHAPE, 2026-11-23: ONE UNIQUE SLOT AND THREE NORMAL ONES</h2>
 *
 * Ben, verbatim: <i>"the first enchantment is a "Unique" enchantment in that the enchantments in that slot are tied to
 * the element more. The second and third enchantments are still from the normal pool"</i>; then, the same day, <i>"Instead
 * of 2 normal enchantment slots, I want 3 in addition to the unique enchantment."</i> and <i>"I want to allow a chance for 4
 * enchantments in each instead of 3"</i>. The seat's rulings U1-U6 and V1-V3.
 *
 * <ul>
 *   <li><b>{@link #SLOTS} = 4</b>, always. {@link #UNIQUE_SLOT} (index 0 then; 1 since the re-cut below) draws ONLY from {@link #uniquePoolFor}: the enchants
 *       whose {@code element} is the gear's, then the class filter, then {@code requires}. Slots 1, 2 and 3 draw ONLY from
 *       {@link #normalPoolFor}: the enchants with NO element, by class then {@code requires}. A unique enchant never
 *       reaches slots 1-3, and a normal one never reaches slot 0.</li>
 *   <li><b>No element, or an empty unique pool, gives slot 0 NO candidates</b> (Ben's "A": "make it possible that an item
 *       has none if there are none in the pool"). No gear loses a normal slot.</li>
 *   <li><b>Each slot offers up to {@link #MAX_CANDIDATES} (4) distinct candidates</b>, unique and normal alike, counted by
 *       {@link CandidateCascade} (Ben's cascade, authored in content) and capped at the pool's size.</li>
 * </ul>
 *
 * <h2>THE COLUMNS RE-CUT, 2026-11-23 (later the same day): A WEAPON SLOT, A UNIQUE SLOT, TWO NORMAL ONES</h2>
 *
 * Ben, verbatim: <i>"The first column will be weapon specific enchantments that fundamentally change how the weapons
 * work. The second column will be our unique enchantments we just made and the 3rd and 4th columns will be the normal
 * enchantments"</i>, and asked whether normal drops to two or the table grows to five: <i>"Three back to 2 (A)"</i>. The
 * seat's rulings W1-W4. <b>Still {@link #SLOTS} = 4; what each index holds moved:</b>
 *
 * <ul>
 *   <li><b>{@link #WEAPON_SLOT} (0)</b> draws ONLY from {@link #weaponPoolFor}: the enchants whose {@code weapon} names
 *       this gear's id, then the class filter, then {@code requires}. Gear that is not a weapon, and a weapon no enchant
 *       names, rolls it EMPTY.</li>
 *   <li><b>{@link #UNIQUE_SLOT} (1)</b> draws ONLY from {@link #uniquePoolFor}, unchanged (element, class, requires). It
 *       was index 0 until this change.</li>
 *   <li><b>Slots 2 and 3</b> draw ONLY from {@link #normalPoolFor}, unchanged. Slot 1 was normal until this change.</li>
 * </ul>
 *
 * <p>The three pools are disjoint by construction: a weapon-specific enchant has no element (the loader refuses both), so
 * it is never unique, and {@link #normalPoolFor} skips both other kinds. {@link #kindOf} names each index's kind, and it
 * is the one place that does.
 *
 * <p>Every candidate arrives LOCKED at level 0 with nothing active: the roll decides what is on offer, the player decides
 * what to buy.
 *
 * <p><b>Decomposed by decision KIND, not by draw.</b> {@link CandidateCascade#lands} and {@link #pick} each take one
 * already-drawn double, so each real boundary is pinned at an exact value with no random source. {@link #roll} is a thin
 * orchestrator over them.
 *
 * <p>The {@code ThreadLocalRandom} draw and the PDC write stay in paper ({@code EnchantRollItems}), with the
 * once-per-item guard. Nothing here knows which enchants exist: the roster arrives as {@link Rollable}s that paper builds
 * from {@code EnchantDefinition}.
 */
public final class EnchantRoll {

    private EnchantRoll() {}

    /**
     * Every rollable piece of gear gets exactly this many slots: the unique slot and three normal ones (Ben, 2026-11-23,
     * V1). WAS 3 until then. Held equal to {@code EnchantMenuLayout.SLOTS} by a test in paper, so a roll the table cannot
     * show is caught at build time and not refused at the door in front of a player.
     */
    public static final int SLOTS = 4;

    /** The weapon-specific slot's index (W2, 2026-11-23). The table's column 2. */
    public static final int WEAPON_SLOT = 0;

    /** The unique slot's index. WAS 0 until the columns were re-cut (W2, 2026-11-23); the table's column 4. */
    public static final int UNIQUE_SLOT = 1;

    /** What each slot index draws from. */
    public enum Kind { WEAPON, UNIQUE, NORMAL }

    /** The kind of slot {@code slot} is: 0 weapon-specific, 1 unique, 2 and 3 normal (W2). The one place that says so. */
    public static Kind kindOf(int slot) {
        if (slot == WEAPON_SLOT) return Kind.WEAPON;
        if (slot == UNIQUE_SLOT) return Kind.UNIQUE;
        return Kind.NORMAL;
    }

    /**
     * The most candidates one slot may offer (Ben's "a chance for 4 enchantments in each instead of 3", V2). WAS 3. Held
     * equal to {@code EnchantMenuLayout.CANDIDATES}; {@link CandidateCascade} refuses a tier above it.
     */
    public static final int MAX_CANDIDATES = 4;

    /**
     * One enchant, as the roll needs to see it: its id, the class it is valid on, the quantity it needs, and the element
     * that makes it UNIQUE.
     *
     * <p>{@code gearClass} is null for a {@code universal} enchant, valid on everything. {@code element} is null for a
     * NORMAL enchant, which every enchant was until 2026-11-23; non-null makes it unique to gear of that element (U1).
     * {@code weapon} is null except on a WEAPON-SPECIFIC enchant, where it is the one weapon id it belongs to (W1); an
     * enchant may not be both, here as in the loader.
     */
    public record Rollable(String enchantId, GearClass gearClass, Requires requires, String element, String weapon,
                           boolean guaranteed) {
        /**
         * {@code guaranteed} (the seat's G1, 2026-11-24; Ben: "add one that comes with every weapon guaranteed"): slot 0 of
         * its weapon ALWAYS offers it, listed first, and the cascade rolls only the rest ({@link #roll}). Only a
         * WEAPON-SPECIFIC enchant may be guaranteed: one that names no weapon has no slot to be guaranteed in.
         */
        public Rollable {
            if (enchantId == null || enchantId.isBlank()) {
                throw new IllegalArgumentException("a rollable enchant needs an id");
            }
            if (requires == null) requires = Requires.NONE;
            if (element != null && element.isBlank()) {
                throw new IllegalArgumentException("enchant '" + enchantId + "': a blank element; absent means normal");
            }
            if (weapon != null && weapon.isBlank()) {
                throw new IllegalArgumentException("enchant '" + enchantId + "': a blank weapon; absent means not weapon-specific");
            }
            if (element != null && weapon != null) {
                throw new IllegalArgumentException("enchant '" + enchantId + "' is both unique (element " + element
                        + ") and weapon-specific (weapon " + weapon + "); it may be one or neither");
            }
            if (guaranteed && weapon == null) {
                throw new IllegalArgumentException("enchant '" + enchantId + "' is guaranteed but names no weapon; only a"
                        + " weapon-specific enchant may be guaranteed (G1)");
            }
        }

        /** Not guaranteed: every enchant before 2026-11-24 (G1). */
        public Rollable(String enchantId, GearClass gearClass, Requires requires, String element, String weapon) {
            this(enchantId, gearClass, requires, element, weapon, false);
        }

        /** A UNIQUE enchant (an element) or a NORMAL one (null), never weapon-specific. */
        public Rollable(String enchantId, GearClass gearClass, Requires requires, String element) {
            this(enchantId, gearClass, requires, element, null);
        }

        /** A NORMAL enchant (no element), with a quantity requirement. */
        public Rollable(String enchantId, GearClass gearClass, Requires requires) {
            this(enchantId, gearClass, requires, null, null);
        }

        /** A NORMAL enchant with no quantity requirement. */
        public Rollable(String enchantId, GearClass gearClass) {
            this(enchantId, gearClass, Requires.NONE, null, null);
        }

        /** A WEAPON-SPECIFIC enchant belonging to {@code weaponId} (W1). */
        public static Rollable forWeapon(String enchantId, GearClass gearClass, Requires requires, String weaponId) {
            if (weaponId == null) throw new IllegalArgumentException("enchant '" + enchantId + "': a weapon id is required");
            return new Rollable(enchantId, gearClass, requires, null, weaponId);
        }

        /** A WEAPON-SPECIFIC enchant belonging to {@code weaponId}, {@code guaranteed} or not (G1). */
        public static Rollable forWeapon(String enchantId, GearClass gearClass, Requires requires, String weaponId,
                                         boolean guaranteed) {
            if (weaponId == null) throw new IllegalArgumentException("enchant '" + enchantId + "': a weapon id is required");
            return new Rollable(enchantId, gearClass, requires, null, weaponId, guaranteed);
        }

        public boolean isUnique() {
            return element != null;
        }

        public boolean isWeaponSpecific() {
            return weapon != null;
        }

        /** The kind of slot this enchant may be offered in. */
        public Kind kind() {
            if (weapon != null) return Kind.WEAPON;
            if (element != null) return Kind.UNIQUE;
            return Kind.NORMAL;
        }
    }

    /**
     * THE QUANTITY AN ENCHANT NEEDS THE HELD WEAPON TO HAVE (the seat's ruling H, 2026-11-22): AN ENCHANT ROLLS ONLY ON A
     * WEAPON THAT HAS ITS QUANTITY. Declared beside the enchant (its file's {@code requires:}), never as a list of weapon
     * ids, so the next weapon needs no edit. NAME THE QUANTITY's eligibility half, made a filter.
     */
    public enum Requires {
        /** No requirement beyond the class. */
        NONE,
        /** A reload: {@code reload_ticks} above 0 ({@code deft_hands}). */
        RELOAD,
        /** A quiver: {@code quiver_size} above 0 ({@code expanded_quiver}). */
        QUIVER;

        public boolean metBy(Has held) {
            return switch (this) {
                case NONE -> true;
                case RELOAD -> held.reload();
                case QUIVER -> held.quiver();
            };
        }
    }

    /** The quantities the held gear has, as {@link Requires} reads them. */
    public record Has(boolean reload, boolean quiver) {
        /** Every quantity: the pool is the class filter alone (how every pool was built before ruling H). */
        public static final Has EVERY = new Has(true, true);
        /** No quantity: armour, shields, tools, and a weapon with no magazine and no reload. */
        public static final Has NOTHING = new Has(false, false);
    }

    /**
     * THE NORMAL POOL: the enchants slots 1-3 may offer on gear of class {@code heldClass}, in roster order. Named
     * {@code poolFor} until 2026-11-23, when it was the only pool; renamed so no caller reads it as "every enchant".
     *
     * <p>A unique enchant (one with an element) is never in it. Order is preserved, so the pool is a deterministic
     * function of the registry and a fixed set of draws reproduces a fixed roll.
     *
     * <p>A null {@code heldClass} yields the universal normal enchants and nothing else (unreachable: {@code class} is
     * required on a weapon and a shield). An ACCESSORY's pool is EMPTY (ruling A4), not "every universal enchant".
     */
    public static List<Rollable> normalPoolFor(Has held, GearClass heldClass, List<Rollable> roster) {
        List<Rollable> pool = new ArrayList<>();
        if (roster == null) return pool;
        // Ruling A4, EXPLICIT: accessories take no part in enchanting. EnchantRollItems.rollOnAcquire also refuses an
        // accessory before it writes anything; this is the half a unit test can see.
        if (heldClass == GearClass.ACCESSORY) return pool;
        for (Rollable rollable : roster) {
            if (rollable == null) continue;
            // U3 and W2: a unique enchant is the unique slot's alone, a weapon-specific one the weapon slot's alone.
            if (rollable.kind() != Kind.NORMAL) continue;
            // null class == universal: no gate, valid on whatever it is offered for.
            if (rollable.gearClass() != null && rollable.gearClass() != heldClass) continue;
            // RULING H: AFTER the class filter, the quantity the enchant reads must exist on the held weapon.
            if (!rollable.requires().metBy(held)) continue;
            pool.add(rollable);
        }
        return pool;
    }

    /**
     * THE UNIQUE POOL: the enchants slot 0 may offer, in roster order. Three filters, in this order (U2): the element must
     * be the gear's, then the class (or universal), then {@code requires}.
     *
     * @param element the gear's element, {@code Optional.empty()} for gear with none; NEVER null (U6: a caller cannot skip
     *                it by passing nothing). Empty gives an empty pool.
     */
    public static List<Rollable> uniquePoolFor(Optional<String> element, Has held, GearClass heldClass,
                                               List<Rollable> roster) {
        if (element == null) {
            throw new IllegalArgumentException("the gear's element is required: Optional.empty() for gear with none");
        }
        List<Rollable> pool = new ArrayList<>();
        if (roster == null || element.isEmpty()) return pool;
        if (heldClass == GearClass.ACCESSORY) return pool;   // ruling A4, as above
        for (Rollable rollable : roster) {
            if (rollable == null) continue;
            if (!element.get().equals(rollable.element())) continue;                                    // 1. element
            if (rollable.gearClass() != null && rollable.gearClass() != heldClass) continue;            // 2. class
            if (!rollable.requires().metBy(held)) continue;                                             // 3. requires
            pool.add(rollable);
        }
        return pool;
    }

    /**
     * THE WEAPON POOL (W2, 2026-11-23): the enchants the weapon slot may offer, in roster order. Three filters, in this
     * order: the enchant's {@code weapon} must be {@code gearId}, then the class (or universal), then {@code requires}.
     *
     * @param gearId the gear's own id; NEVER null or blank (W4: as unskippable as the element). Gear that is not a weapon
     *               passes its own id and gets an empty pool, because no enchant can name it (the loader refuses a
     *               {@code weapon:} that is not a loaded weapon).
     */
    public static List<Rollable> weaponPoolFor(String gearId, Has held, GearClass heldClass, List<Rollable> roster) {
        requireGearId(gearId);
        List<Rollable> pool = new ArrayList<>();
        if (roster == null) return pool;
        if (heldClass == GearClass.ACCESSORY) return pool;   // ruling A4, as above
        for (Rollable rollable : roster) {
            if (rollable == null) continue;
            if (!gearId.equals(rollable.weapon())) continue;                                            // 1. this weapon
            if (rollable.gearClass() != null && rollable.gearClass() != heldClass) continue;            // 2. class
            if (!rollable.requires().metBy(held)) continue;                                             // 3. requires
            pool.add(rollable);
        }
        return pool;
    }

    /**
     * E1 (the seat, 2026-11-25): the SLOT-0 POOL KEY every armor piece of {@code slot} rolls by, and that an
     * {@code armor_slot:} enchant belongs to. Ben: "Helmet boots chestplate and leggings all different", so every helmet
     * shares one slot-0 pool and every chestplate another. It rides the weapon pool's own key ({@link Rollable#weapon},
     * matched against the gear id {@link #roll} is given), so slot 0 has ONE mechanism: a weapon rolls it by its id, an
     * armor piece by this. The colon keeps it disjoint from every weapon id, which is a content file name.
     */
    public static String armorSlotPool(io.github.butterflysmp.rpg.core.weapon.ArmorSlot slot) {
        if (slot == null) throw new IllegalArgumentException("an armor slot is required");
        return "armor_slot:" + slot.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static void requireGearId(String gearId) {
        if (gearId == null || gearId.isBlank()) {
            throw new IllegalArgumentException("the gear's id is required: the weapon slot is drawn by it (W4)");
        }
    }

    /** The pool slot {@code slot} draws from: {@link #kindOf} decides which of the three. */
    public static List<Rollable> poolForSlot(int slot, String gearId, Optional<String> element, Has held,
                                             GearClass heldClass, List<Rollable> roster) {
        return switch (kindOf(slot)) {
            case WEAPON -> weaponPoolFor(gearId, held, heldClass, roster);
            case UNIQUE -> uniquePoolFor(element, held, heldClass, roster);
            case NORMAL -> normalPoolFor(held, heldClass, roster);
        };
    }

    /**
     * The candidate a single draw selects from {@code remaining}: uniform over its indices.
     *
     * <p>Takes the pool it draws from rather than a fixed one, because the caller REMOVES each pick before drawing again,
     * which makes a slot's candidates distinct by construction rather than by a retry loop. {@code 0.0} is always the
     * first entry and a draw just under 1 always the last, whatever the pool has shrunk to. Null on an empty pool.
     */
    public static Rollable pick(List<Rollable> remaining, double roll) {
        if (remaining == null || remaining.isEmpty()) return null;
        int index = (int) (roll * remaining.size());
        return remaining.get(Math.max(0, Math.min(index, remaining.size() - 1)));
    }

    /**
     * A whole piece of gear's opening state: {@link #SLOTS} slots of locked candidates, slot 0 from the weapon pool, slot
     * 1 from the unique pool and slots 2-3 from the normal pool ({@link #kindOf}).
     *
     * <p>Each slot draws from a FRESH copy of its pool: distinctness is within a slot (and {@code EnchantSlot}'s own
     * constructor enforces it), so two normal slots may both offer Sharpness, and {@code EnchantState.effective()}
     * resolves that to the highest level, never the sum.
     *
     * <p>Draws, slot by slot: the cascade's (one per tier rolled, none for an empty pool or the guaranteed tier), then one
     * per candidate picked. Stated for the end-to-end test only.
     *
     * @param gearId  the gear's own id ({@code GearDefinition.id()}); never null or blank (W4)
     * @param element the gear's element (U5: {@code GearDefinition.gearElement()}); {@code Optional.empty()} for none;
     *                never null (U6)
     */
    public static EnchantState roll(String gearId, Optional<String> element, Has held, GearClass heldClass,
                                    List<Rollable> roster, CandidateCascade cascade, DoubleSupplier draws) {
        requireGearId(gearId);
        if (element == null) {
            throw new IllegalArgumentException("the gear's element is required: Optional.empty() for gear with none");
        }
        if (cascade == null) throw new IllegalArgumentException("the candidate cascade is required");
        List<EnchantSlot> slots = new ArrayList<>(SLOTS);
        for (int slot = 0; slot < SLOTS; slot++) {
            // THE GUARANTEED CANDIDATES (G1, 2026-11-24) come first, in roster order, and take no draw; the cascade then
            // counts the REST of the pool, capped at the room they leave. Only a weapon-specific enchant can be
            // guaranteed (Rollable refuses the rest), so only slot 0's pool ever holds one.
            List<EnchantCandidate> candidates = new ArrayList<>(MAX_CANDIDATES);
            List<Rollable> remaining = new ArrayList<>();
            for (Rollable r : poolForSlot(slot, gearId, element, held, heldClass, roster)) {
                if (r.guaranteed()) {
                    if (candidates.size() < MAX_CANDIDATES) candidates.add(new EnchantCandidate(r.enchantId(), 0));
                } else {
                    remaining.add(r);
                }
            }
            int count = cascade.count(Math.min(remaining.size(), MAX_CANDIDATES - candidates.size()), draws);
            for (int i = 0; i < count; i++) {
                Rollable picked = pick(remaining, draws.getAsDouble());
                if (picked == null) break;
                remaining.remove(picked);
                candidates.add(new EnchantCandidate(picked.enchantId(), 0));
            }
            slots.add(new EnchantSlot(candidates, EnchantSlot.NONE));
        }
        return new EnchantState(slots);
    }

    /**
     * THE DEV TALLY ({@code /rpg enchant rollstats}): for each slot index 0..{@link #SLOTS}-1, how many of {@code states}
     * offered 0..{@link #MAX_CANDIDATES} candidates there. A slot a state lacks counts as 0; a count past the bound is
     * clamped into the last column (no shipped roll makes one).
     */
    public static int[][] tally(List<EnchantState> states) {
        int[][] out = new int[SLOTS][MAX_CANDIDATES + 1];
        for (EnchantState state : states) {
            for (int slot = 0; slot < SLOTS; slot++) {
                int n = slot < state.slots().size() ? state.slots().get(slot).candidates().size() : 0;
                out[slot][Math.min(n, MAX_CANDIDATES)]++;
            }
        }
        return out;
    }
}
