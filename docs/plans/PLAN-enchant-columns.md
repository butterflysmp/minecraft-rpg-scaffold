# PLAN — THE ENCHANT COLUMNS RE-CUT, THE LIVING LANTERN REWORKED, AND BEN'S FIRST BEHAVIOURAL ENCHANTS

Written 2026-11-23 from the seat's brief of that day ("THREE ENCHANT COLUMNS' WORTH OF WORK"), stacked on
`feat/boltor-sound-flint-text` (#251), on #250, #249, #248, on master `383ded19`. Every server or code fact is marked READ
(our source, at the commit named), EXECUTED, or INFERRED. **One gate and ONE R0 for the whole stack from #248 up; no boot
until Ben says "ready" and the seat approves code and gate from origin.** Each PR appends its own section below.

## 1. PR A — THE COLUMNS RE-CUT (`feat/enchant-columns`)

### Ben's words, verbatim, 2026-11-23

> "I want to change enchantments a little more. The first column will be weapon specific enchantments that
> fundamentally change how the weapons work. The second column will be our unique enchantments we just made and the 3rd
> and 4th columns will be the normal enchantments"

Asked whether normal drops from three slots to two (A) or the table grows to five columns (B): **"1. Three back to 2
(A)"**. Shown the seat's assumptions (a weapon with none designed has an empty first column; armor, shields and tools have
an empty first column; the first column offers 1 to 4 by the same cascade; a weapon-specific enchant belongs to one named
weapon, not a family): **"2. Yes"**. **"We can remove the placeholder names/labels above the columns."**

### The seat's rulings, as built

- **W1.** WEAPON-SPECIFIC when the file declares `weapon: <weapon id>` (`EnchantLoader`'s `weapon` section). Refused by
  name: an id that is not a loaded WEAPON (a shield id included), and both `weapon:` and `element:` (the record,
  `EnchantDefinition`, owns that refusal; the loader does not repeat it). **One refusal the brief did not name, stated so
  it is not missed:** a `class:` other than `universal` that differs from the named weapon's class is refused too,
  because such an enchant could never roll (the weapon pool applies the class filter, as the unique pool does).
- **W2.** Four slots. `EnchantRoll.kindOf` is the one place that says what each index holds: **0 WEAPON, 1 UNIQUE, 2 and
  3 NORMAL**. `weaponPoolFor(gearId, …)` filters by the enchant's `weapon` equal to the gear's id, then class, then
  `requires`. `normalPoolFor` now skips every non-NORMAL kind (it skipped only unique ones). The three pools are disjoint
  by construction (`EnchantRollWeaponTest.noPoolHoldsAnotherKind`).
- **W3.** The cells did not move (column 2 slot 0, 4 slot 1, 6 and 8 slots 2 and 3; `EnchantMenuLayout.rawSlotFor`
  unchanged). The "Unique" NAME_TAG at index 1 and its constant `UNIQUE_LABEL_SLOT` are REMOVED; index 1 is filler. The
  empty-slot marker stays, one per column that can be empty (`EnchantMenuLayout.emptyMarkerFor`), both SEAT PLACEHOLDERS.
- **W4.** `EnchantRoll.roll(String gearId, Optional<String> element, …)`: a null or blank id is refused
  (`IllegalArgumentException`), as a null element is. Over every shipped gear (`UniqueSlotContentTest`): each slot's
  candidates are only its own kind over 200 seeded rolls of each of the 60; the normal pool (slots 2 and 3) equals
  master's `383ded19` snapshot for all 60; the weapon pool is EMPTY for all 60 at this PR.
- **W5.** `dev_kinetic_example.yml` stays until the PR that lands Kinetic Tremors deletes it.

### What else moved, stated so the diff does not have to be read to learn it

- **The load order (READ, `RpgPlugin.onEnable`):** the enchants loaded right after the elements and BEFORE the weapons;
  they now load right after the weapons, because the loader asks the weapon registry. READ: nothing between the old and
  new position reads `enchants` (its next use is the `Loaded …` summary line). `EnchantLoadOrderTest` pins the order and
  the lookup from the source.
- **`/rpg enchant pool`** prints a THIRD line, `Weapon pool: <ids>` (gray), after `Pool:` and `Unique pool:`, whose
  wording is unchanged: `Pool:` is now slots 2-3's and `Unique pool:` slot 1's.
- **`/rpg enchant rollstats`**: the first line gains `weaponPool=<n>` before `uniquePool=`; the slot lines read
  `slot 0 (weapon):`, `slot 1 (unique):`, `slot 2:`, `slot 3:` (`EnchantRollItems.slotLabel`).
- **The dev command's `candidate` op still writes any enchant into any slot.** W2's "can never appear" binds the ROLL; the
  dev staging instrument is left able to stage a mixed column (the gate's full-table row depends on it). Not a shipped
  surface: `/rpg enchant` is dev-server only.
- **An item rolled under #249's shape (slot 0 unique, 1-3 normal)** exists only on the dev world, never merged: by Ben's
  V4 ("we don't care about existing items"), no migration. In the table its unique candidates sit in the weapon column
  and its first normal slot's in the unique column; nothing throws, dupes or charges differently (the same code path V4
  read for the three-slot shape).

### OPEN FOR BEN (built as SEAT PLACEHOLDER 2026-11-23)

1. **The empty weapon slot's marker:** a GRAY_DYE in column 2's first cell (index 10), named **`No weapon enchant`** (dark
   gray), lore **`Nothing is tied to this weapon yet.`** (dark gray).
2. **The empty unique slot's marker:** #249's, unchanged, now in column 4's first cell (index 12): **`No unique
   enchant`** / **`Nothing unique is tied to this item's element yet.`**

### Tests and mutations

New: `EnchantRollWeaponTest` (12 rows, `grep -c @Test`), `EnchantLoadOrderTest` (1); new rows in `EnchantLoaderTest` (2),
`EnchantMenuLayoutTest` (1), `UniqueSlotContentTest` (1); rewritten for the moved index: `EnchantRollTest`,
`EnchantRollUniqueTest`, `EnchantMenuLayoutTest`, `EnchantRollStatsTest`, `EnchantQuantityGateTest`,
`UniqueSlotContentTest`. Ten mutations, each a single-site literal replace with a marker, each printing `marker
present: 1  original gone: 0  line delta: 0`, each restored by `cp` and checked `cmp` byte-identical:

| mutation | the edit | reddened |
|---|---|---|
| MUTWPOOLID | weapon filter accepts any weapon enchant | 7 rows of `EnchantRollWeaponTest` |
| MUTNORMALKIND | normal pool skips only unique (the old line) | 4 rows, among them `noPoolHoldsAnotherKind` |
| MUTKINDOF | `kindOf(1)` answers NORMAL | 12 rows across the three roll tests |
| MUTGEARID | the gear-id refusal disabled | `aNullOrBlankGearIdIsRefused` |
| MUTBOTH | the Rollable's both-kinds refusal disabled | `aRollableCannotBeBothUniqueAndWeaponSpecific` |
| MUTLOADERWEAPON | the not-a-loaded-weapon refusal disabled | both new `EnchantLoaderTest` rows |
| MUTLOADERCLASS | the class-mismatch refusal disabled | the W1 row (`wrong_class`) |
| MUTRECORDBOTH | the record's both-keys refusal disabled | the W1 row (`both`) |
| MUTMARKER | the weapon marker removed | `theWeaponAndUniqueColumnsHaveEmptyMarkersAndTheNormalOnesDoNot` |
| MUTLOOKUP | the loader handed no weapon lookup | `EnchantLoadOrderTest` |

**UNMUTATED, said so:** the ORDER half of `EnchantLoadOrderTest` (moving a block is not a single-site replace); the
`Weapon pool:` line's presence is pinned by `EnchantQuantityGateTest`'s source read, not mutated.
