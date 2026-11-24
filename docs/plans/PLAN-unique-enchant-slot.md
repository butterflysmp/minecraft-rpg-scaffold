# PLAN — THE UNIQUE ENCHANT SLOT: THE MECHANISM, NO CONTENT YET

Written 2026-11-23 on `feat/unique-enchant-slot` (stacked on `feat/selftest-row-case`, #248, on master `383ded19`), from
the seat's brief of 2026-11-23 and its amendment the same day. Every server or code fact is marked READ (our source, at
the commit named) or INFERRED. **The gate is `docs/gates/GATE-unique-enchant-slot.md`, one R0 for PRs 1, 2 and 3; no boot
until Ben's first unique enchants are stacked on top, so one boot reads both** (the seat's brief).

## 1. BEN'S WORDS, VERBATIM, 2026-11-23

**The first set (the seat's brief):**

> "I want to change enchantments a little bit. 1. Instead of 3 generic enchantment slots, the first enchantment is a
> "Unique" enchantment in that the enchantments in that slot are tied to the element more. The second and third
> enchantments are still from the normal pool"

Asked which gear: *"1. Everything that can be enchanted that has an element will be able to receive unique enchantments
but make it possible that an item has none if there are none in the pool."* Asked what the unique enchants are: *"2. I
will get to this"*. *"3. Kinetic will also have enchantments unique to them"*. Asked how many choices the unique slot
offers: *"4. The same 1-3"*. Asked about items already rolled: *"5. We don't have any live servers, only our dev world and
we don't care about existing items in that it's solely for testing"*. Asked what slot 0 does with no unique enchant
available, A (stays empty) or B (falls back to the normal pool): **"A"**.

**The amendment (the seat's message of the same day; where it and the first set disagree, it wins):**

> "1. Instead of 2 normal enchantment slots, I want 3 in addition to the unique enchantment."
> "2. Move the Enchantment Table info item to the top right corner and move the bookshelf to the left of that. Make the
> rest of the nexus menu sub menus and our custom UIs consistent with that too"
> "3. The Slot for the item that is being enchanted should go where the Enchantment table info item was."
> "4. The Unique enchantments should go in collum 2 and the normal ones where they are today"
> "5. I want to allow a chance for 4 enchantments in each instead of 3"
> "6. I want to change the chances of multiple enchants: 1 enchant = 100%; 2 enchants = 70%; 3 enchants = 40%; 4
> enchants = 10%. I want this roll to be cascading like we did with the loot tables. (First it rolls a 10% chance, if
> that misses it rolls the 40%, if that misses it rolls the 70% and so on)"
> "I will give you unique enchants soon"

## 2. THE RULINGS (the seat's), AS BUILT

- **U1.** An enchant is UNIQUE when its file declares `element: <element id>`; the loader refuses an id that is not a
  loaded element, by name (`EnchantLoader`'s `element` section). No `element:`: NORMAL, as all 11 real enchants are.
- **U2.** A unique enchant still declares its class (or universal) and may declare `requires`; the unique pool applies
  element, then class, then requires (`EnchantRoll.uniquePoolFor`). Declared beside the enchant, never as gear ids.
- **V1 (replaces U3's count).** FOUR slots: index 0 is THE UNIQUE SLOT on every piece of rollable gear; 1, 2 and 3 are
  normal. Slot 0's pool is the unique enchants of the gear's element; **no element, or an empty pool, gives slot 0 NO
  candidates (Ben's A)**. Slots 1-3 roll from the normal pool only. No gear loses a normal slot.
- **V2 (replaces U4).** Up to FOUR candidates in every slot, unique and normal alike.
- **V3.** The count is Ben's CASCADE: a draw against 0.10 for four; on a miss against 0.40 for three; on a miss against
  0.70 for two; otherwise one; then capped at the pool's size; a pool of 0 gives 0. A draw is consumed only for a tier
  rolled. Core's `CandidateCascade` (`lands`, one decision per drawn double; `count`, the orchestration). **His numbers
  live in `content/enchant_candidates.yml`**, read by `EnchantCandidatesLoader`, refused whole by name for any other
  shape (not rarest first, a chance outside [0, 1], a count outside 1..4, a last tier not 1.0, a 1.0 above the last),
  which leaves `CandidateCascade.ONE` (one candidate a slot, a WARN beginning `Refusing enchant candidates`). Its Loaded
  line, measured by `EnchantCandidatesLoaderTest`: **`Loaded enchant candidates: 4 at 10.00% (truly 10.000%), 3 at
  40.00% (truly 36.000%), 2 at 70.00% (truly 37.800%), 1 at 100.00% (truly 16.200%)`**.
- **U5.** The element comes from `GearDefinition.gearElement()`, ONE abstract accessor on the sealed gear interface
  (READ below).
- **U6.** `EnchantRoll.roll(Optional<String> element, Has, GearClass, roster, CandidateCascade, draws)`: the element is a
  required parameter and a null is refused (`IllegalArgumentException`); a caller with no element passes
  `Optional.empty()`.
- **V4 (replaces U7's numbers).** Old items: no migration, no re-roll (READ below).
- **U8.** An empty slot 0 stored, shown and refused cleanly (READ below).
- **The dev-only example:** `content/enchants/dev_kinetic_example.yml` (kinetic, universal, effect damage, 1/2/3%). A
  file whose id starts `dev_` loads ONLY on a dev server (`-Drpg.dev=true`, `SelfTestGuard.DEV_PROPERTY`, set only by
  `scripts/dev-server.sh`; `EnchantLoader.DEV_PREFIX`); elsewhere it is not registered and one INFO line names it.
  `EnchantLoaderTest.theDevEnchantIsAbsentWithoutTheDevMarker` pins both halves.

## 3. THE CONSEQUENCE, KNOWN TO BEN (his "A")

**Until Ben designs unique enchants, every newly rolled item has THREE usable slots** (slots 1-3; the amendment's count,
where the first brief's was two). On a real server no gear has a unique pool at all; on the dev server only the 16
kinetic weapons do, and only the dev example (`UniqueSlotContentTest`).

## 4. THE READ ANSWERS (our source, on this branch)

**U5: where the accessor lives and what it returns.** `core/.../weapon/GearDefinition.java`, `Optional<String>
gearElement()`, abstract, so each of the five kinds answers in its own record: `WeaponDefinition` returns
`Optional.of(element)` (a weapon's `element` is required, "use kinetic, never absent"); `ShieldDefinition`,
`ArmorDefinition`, `ToolDefinition` and `AccessoryDefinition` return `Optional.empty()`. No gear file gained an element.
`EnchantRollItems.rollOnAcquire` / `normalPoolFor` / `uniquePoolFor` / `rollStats` read it; a kind that declares an
element later changes only its own record.

**V4: an old item (three normal slots, up to three candidates).** Simplest safe behaviour: what the code already does;
no code was written for it. HELD: its tooltip shows its active enchants as before (`EnchantLore` reads `effective()`, which
ignores slot count and which slot is "unique"; `OldThreeSlotItemTest`). DUMPED: `/rpg enchant show` prints its three slots.
IN THE TABLE: it fits (three of three inside four of four, `EnchantMenuLayout.overflow` is empty), every candidate has a
cell, its slot 0's NORMAL candidates show in the unique column under the "Unique" label, column 8 stays filler; a click is
priced by the same `EnchantCharge`/`EnchantCost` as any candidate, and the grindstone refunds every unlocked rung
(`OldThreeSlotItemTest`, `EnchantMenuLayoutTest.anOldThreeSlotItemFitsAndEveryCandidateHasACell`). It never throws,
dupes or charges differently. **The one oddity, cosmetic: an old item's normal enchants sit under "Unique".**

**U8: does each surface tolerate a slot with zero candidates? Every one already did; none needed a fix.**
- `EnchantCodec`: an empty slot encodes `:-1` and decodes back to `EnchantSlot.empty()` (`decodeSlot`: an empty
  candidate part adds nothing). `EnchantRollTest.aRolledStateSurvivesTheWireGrammar` pins `v1;:-1;…` round-tripping.
- `EnchantState` / `EnchantSlot`: `EnchantSlot.empty()` is legal; `effective()` skips a slot with no active candidate.
- `EnchantMenu`: `renderCandidates` loops `index < candidates().size()`, so an empty slot drew filler; **the only change
  is presentation**: the unique column's first cell now shows the empty item (below). A click there resolves to (0, 0)
  and `applyCandidateClick` returns at `if (candidateIndex >= slot.candidates().size()) return;` BEFORE the price, the
  wallet read, the write or the re-mint: it changes nothing and charges nothing.
- `EnchantLore` / `EnchantLoreLines`: read `effective()`; an empty slot adds no line.
- `GrindstoneRefund`: `listSpend` and `stripped` loop over candidates; an empty slot adds 0 and stays empty.
- The anvil: no `Anvil*.java` reads `EnchantState` or `EnchantItems`; a repair re-mints and carries the raw blob.
- `/rpg enchant show` prints `Slot 0: []`; `level`/`active` on it refuse `Slot 0 has 0 candidate(s).`
- `EnchantRoll` itself already returned an empty slot for an empty pool before this change.

## 5. WHERE THE SLOT COUNT AND THE CANDIDATE BOUND CHANGED

| place | was | now |
|---|---|---|
| `EnchantRoll.SLOTS` | 3 | **4** (and `UNIQUE_SLOT = 0`) |
| `EnchantRoll.MAX_CANDIDATES` | 3 | **4** |
| `EnchantMenuLayout.SLOTS` / `CANDIDATES` | 3 / 3 | **4 / 4** (`EnchantMenuLayoutTest` holds them equal to the roll's) |
| `RpgCommand.MAX_DEV_SLOT` (`/rpg enchant` slot arg) | literal 2 | **`EnchantRoll.SLOTS - 1`** (3) |
| `/rpg enchant` candidate arg | `MAX_DEV_SLOT` (2) | **`MAX_DEV_CANDIDATE = EnchantRoll.MAX_CANDIDATES - 1`** (3) |
| `CandidateCascade` tier bound | (new) | 1..`MAX_CANDIDATES` |
| `EnchantCodec` | uncapped, `v1` | **unchanged** (no bound, no version bump) |
| `EnchantState` | uncapped | unchanged (javadoc only) |
| tests pinning 3 | `EnchantRollTest`, `EnchantMenuLayoutTest` | rewritten for 4 |

## 6. THE TABLE'S LAYOUT (`EnchantMenuLayout`, 1-based prose, index = (row-1)*9+(col-1))

BEN'S: the info item at row 1 col 9 (**8**); the bookshelf at row 1 col 8 (**7**); the input at row 1 col 5 (**4**, where
the info item was); the unique slot in column 2, the normal slots in columns 4, 6, 8. **SEAT PLACEHOLDER 2026-11-23, told
to Ben:** each column runs down rows 2-5: unique **10, 19, 28, 37**; normal **12, 21, 30, 39 / 14, 23, 32, 41 / 16, 25,
34, 43**. Back 48 and Close 49 unchanged. The input slot's javadoc trail gained its fourth entry; nothing deleted.

## 7. OPEN FOR BEN (built as SEAT PLACEHOLDER 2026-11-23)

1. **The "Unique" label:** a NAME_TAG at index 1 (row 1, col 2, over the unique column), named **`Unique`** (light
   purple), lore **`Enchants tied to this item's element.`** (gray).
2. **The empty unique slot in the table:** a GRAY_DYE in the unique column's first cell (10), named **`No unique
   enchant`** (dark gray), lore **`Nothing unique is tied to this item's element yet.`** (dark gray).
3. **The tooltip says NOTHING for an empty unique slot** (the item's lore lists active enchants only, as before).
4. **The table's hints, reworded because the input moved to the top:** with nothing placed, **`Place a weapon, shield,
   armor piece or tool in the`** / **`empty slot at the top to see the enchants it can carry.`** (was "…tool below" /
   "to see the enchants it can carry."); a click with nothing placed, **`Put a weapon, shield, armor piece or tool in the
   slot at the top.`** (was "…in the slot on the left.").
5. **Rows 2-5 for the four candidates** (the seat's, above).
6. **The dev example's name, icon and numbers** are a dev fixture's (`Dev Kinetic Example`, amethyst shard, 1/2/3%) and
   go with the file when his first unique enchants land.

## 8. PR 3 (`feat/info-top-right`): THE INFO ITEM AT THE TOP RIGHT ON EVERY SCREEN (Ben's 2, second sentence)

READ first, every screen this plugin opens (all are 54-slot chests, nine wide); the full table is
`GATE-unique-enchant-slot.md`, *PR 3: EVERY SCREEN, READ*. **Moved to index 8 (row 1, column 9)**, each from 4 with 8
filler before: the **anvil** (`AnvilMenuLayout.INFO_SLOT`), the **grindstone** (`GrindstoneMenuLayout.INFO_SLOT`) and
**crafting** (`CraftingMenuLayout.INDICATOR_SLOT`, the screen's icon); the enchanting table moved in PR 2. Back and Close
unchanged everywhere (`InfoTopRightTest`).

**OPEN FOR BEN, LEFT AS THEY ARE (the seat's "DO NOT GUESS"):**
1. **Level**: its item at 4 is a READOUT ("Level N", his XP and bonuses), not a hint. Is it the screen's info item?
2. **Build picker**: its item at 4 TITLES what is being picked. Info item or not?
3. **Vault**: no info item, and 8 is its NEXT page arrow.
4. **Recipe book**: no info item, and 8 is a recipe entry (entries fill indices 0-44).
5. **Nexus hub, Build, Equipment, slot picker, Settings**: no info item at all. Should they get one?

**AN INSTRUMENT CHANGE ON THE SAME PR, STATED SO IT IS NOT MISSED:** `SelfTestSnapshot` now captures and restores his
VANILLA level and progress (the wallet the table and the anvil charge), and its RESTORED line ends
`vanilla_xp=<level>+<progress>`. Needed by the gate's purchase row (US7); without it a run that buys a candidate leaves
his vanilla XP changed. `SelfTestWeaponIdsTest` was widened to armor and tool ids, as its own javadoc required the first
time a scenario gave one.
