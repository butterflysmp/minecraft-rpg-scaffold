# PLAN — THE OWED FIXES FROM THE BOOT OF `1f447197` (R2 TO R6), 2026-11-26

**Status: BUILT, NOT BOOTED.** From the seat's block of 2026-11-26, "GO ON THE FOLLOW-UP PR (R2 TO R6)", via Ben. Branch
`feat/owed-fixes`, from master `6c9b1ff4`. Its rows are re-read by the one short gate at the top of the stack (with the
summon fix's rows, `PLAN-summon-holds-target.md`). The frozen readings (`GATE-blink-tome.md`, READINGS) are not edited.

## DATED NOTE, 2026-11-26 — THE SEAT'S RULINGS ON THE READINGS, R1 TO R7, IN ITS WORDS

> R1. THE PRODUCT READ AS PREDICTED: Bone Club and Drumming Beat (DB8's 18 to 20), the Healing Tome, the Blink Tome's
> nine staged blinks, Blast Fungus, Fast and Fungal, Cluster Bombs, TT20 to TT27, H15 and K9. Ben's files after /stop
> differ by his own later play; nothing to repair.
> R2. BT3 item 7 and the CLEAN lines of BT4 and BT6: MISS, cause in the GATE, not the game (the cooldown held four checks
> until the selftest cleared it). Owed fix: a row that blinks takes the tome off (accessory: clear, then a wait past the
> reconcile) BEFORE any step that clears cooldowns, and a lint rule refuses a clear_state or row end with a Blink accessory
> still equipped and a mob staged as targeting him.
> R3. reset_charge: MISS, and a claim marked READ was FALSE (Player#attack does reset the charge on this server, as
> RpgListeners already said on 2026-08-28). Owed: a dated correction in GATE-bone-club.md and PLAN-bone-club.md saying how
> the READ went wrong; remove the verb or keep it documented as a no-op (CC'S CALL). The charge= readings stand; no row
> is hollow.
> R4. BF3's item samples: MISS by contamination. BF8 is reclassed DID NOT TEST WHAT IT WAS MEANT TO: its instrument counts
> every item and four strays were present, so "no body left on the ground" is not shown by it (the store's fused=0 is the
> only witness). Owed: the item probes print each item's type; a gate that runs after another clears dropped items at its
> setup; BF3 and BF8 re-read; the item near F+(10.59, 0, -0.07) identified or reported as unidentified with its type.
> R5. The HELD name's [white, <colour>] italic against BT1's and HT1's [blue]: MISS on an INFERRED prediction. Owed: READ
> why effectiveName prints that shape and whether a player sees italic; Ben is being asked what he sees.
> R6. BF6's 0.029 offset: MISS at tolerance; owed: the row aligns him before the press or predicts against his own throw
> point.
> R7. None of R2 to R6 blocks the merge: every one is the instrument's. They go into ONE follow-up PR after the merge
> (tests first), with a short gate (the re-read rows by gate.ROW reference, no ask if possible) for a later boot. Record R1
> to R7 as a dated note in that PR, not on the frozen readings.

**And Ben's answer for R5, verbatim, 2026-11-26:** *"1. No item names should not look slanted"*. The seat's ruling on it:
if a player sees italic on any minted name, that is a defect, fixed at the one place names are written; if the shape is
only the instrument's, nothing in the game changes and the read shape is predicted.

## R2 — A ROW THAT BLINKS TAKES THE TOME OFF BEFORE ANY COOLDOWN CLEAR

- **The cause, READ off the boot's log:** BT3's last steps were `probe_target bh`, `wait 20`, `clear_state`; the checks at
  34193, 34198, 34203 and 34208 wrote no BLINK (the cooldown held with `bh` at 0.000 targeting him); `clear_state` then
  cleared his cooldowns with the tome on, and the 5-tick check at 34213 blinked him (`dist=0.000 … safe=394`). BT5 the
  same at 34373 (its `bm`, whose hit's aggro landed after the state ended).
- **The scenario:** every row of `GATE-blink-tome.yml` that equips the tome (BT2 to BT10) now runs `accessory: "clear"`
  then `wait: 6` immediately before its LAST `clear_state` (18 lines added, 0 removed; `OwedRowShapesTest` reads each).
  Six is one more than the reconcile pass's period (READ, `PlayerHealthSystem.RECONCILE_PERIOD_TICKS` = 5), so the pass
  that stops the clock has run (`BLINK STOP … reason=inactive`, or `ended` by the check's own re-check). Unequipping does
  not end a running blink (K4, CC'S CALL as built), so each row's `clear_state` still ends it with `reason=cleared`.
- **The rule, `ScenarioLint.Rule.ACCESSORY_ARMED_AT_CLEAR`:** in a row, once an accessory is equipped and a `mob_target`
  staged, a `clear_state` or the row's end is a finding until an `accessory: clear` is followed by waits adding to at least
  `ACCESSORY_SETTLE_TICKS` (6). One finding per row. **CC'S CALL:** it covers ANY equipped accessory (the lint holds no
  content table to tell a Blink from another); the alternative keys on the Blink's ids. A row with no `mob_target` is not a
  finding (the Healing Tome's rows).
- **Its control:** `AccessoryArmedAtClearLintTest`: the OLD BT3, copied whole as it shipped at `6c9b1ff4`, FAILS; the
  fixed tail passes; `wait 5` fails, `3 + 3` passes; a row that ENDS armed fails; BT7's re-equip after the clear fails; a
  Healing Tome row passes. **Seen firing on the shipped gate before the scenario was fixed:** `BlinkTomeGateCommandTest`
  red on BT2 to BT10 (not BT11, which never equips), then green after.

## R3 — `Player#attack` RESETS THE CHARGE (READ, javap, the pinned server jar)

`run/versions/26.1.2/paper-26.1.2.jar`, MANIFEST `Specification-Version: 26.1.2.build.74-stable` = `pom.xml`'s
`paper.version`:

- `Player.attack(Entity)` has no `resetAttackStrengthTicker` call in its body. At **offset 124** it calls
  `onAttack(Entity)`.
- `Player.onAttack(Entity)` (Paper's): offsets 4 to 27 build and call `PlayerAttackEntityCooldownResetEvent(player,
  target, getAttackStrengthScale(0))`; if it returns true (not cancelled), offset 34 calls `resetOnlyAttackStrengthTicker()`;
  then `Avatar.onAttack`.
- `resetOnlyAttackStrengthTicker()`: `attackStrengthTicker = 0`, and nothing else (`resetAttackStrengthTicker` also zeroes
  `itemSwapTicker`). Its other callers: `Player.tick` (an item change in the main hand) and `detectEquipmentUpdates`.
- **How the first READ missed it:** it searched `attack`'s own body for the name `resetAttackStrengthTicker`; the reset is
  one call down, under a different, Paper-only name. A name search of one method is an absence in the wrong scope.
- **Done:** the dated correction (the account) in `GATE-bone-club.md` under THE CHARGE; a pointer in `PLAN-bone-club.md`
  under M4's "FULL CHARGE"; `Scenario.Verb.RESET_CHARGE`'s javadoc corrected. **`reset_charge` is KEPT as a documented
  no-op** (CC'S CALL, accepted by the seat): its line is the witness that the hit reset the charge.

## R4 — THE ITEM PROBES PRINT THE TYPE; A GATE'S SETUP CLEARS DROPPED ITEMS

- `probe_positions` entries are `x y z <material>` for an item, plus ` fused` (a fused chunk's body) or ` marker` (a marker
  of ours); `probe_entities item` lines gain ` types=[<material> x<n>, …]`, sorted by name.
- **Each gate's SETUP** row-start CLEAN removes every dropped item within 48 of the stand (an Item entity that is neither a
  marker nor a fused body: `SelfTestStaging.droppedItem`) and logs, after its CLEAN line, **`ITEMS cleared=<n> radius=48
  [<material> x<k>, …]`**. A row's own drops are its reading: rows do not clear.
- **The strays of the boot of 1f447197 are not identified**, and cannot be from that log (no line named a type). INFERRED:
  the drops of the zombies the Bone Club rows killed with `/kill @e[tag=t]` near block (2, 0); the one near F+(10.59, 0,
  −0.07) has no candidate. The new setup line names whatever the gate before left.
- Tests: `SelfTestItemTypesTest` (the pure decision and line shapes; the setup's clear and both probes read off the source).

## R5 — THE NAME IS DRAWN UPRIGHT; THE `[white, …] italic` IS THE INSTRUMENT'S (READ, both jars)

- **Server** (the pinned jar): `CraftItemStack.effectiveName()` returns `PaperAdventure.asAdventure(handle
  .getStyledHoverName())`; `ItemStack.getStyledHoverName()` is `Component.empty().append(getHoverName())
  .withStyle(getRarity().color())`, then `.withStyle(ChatFormatting.ITALIC)` when the stack `has(DataComponents
  .CUSTOM_NAME)`. So the HELD line's name is a WRAPPER: white for a common-rarity vanilla item (the vanilla rarity, not
  ours), ITALIC, around our stored name.
- **Client** (`.minecraft/versions/26.1.2/26.1.2.jar`, unobfuscated): the tooltip's first line is `getStyledHoverName`
  (`ItemStack.getTooltipLines`); the hotbar's selected-item name is built the same way in `Gui` (offsets 17 to 61:
  `empty().append(getHoverName()).withStyle(rarity)`, then `ITALIC` when a custom name is present). A component is drawn
  through `Component.visit`, which gives each child `getStyle().applyTo(parent)`; `Style.applyTo` (offsets 77 to 91) keeps
  the CHILD's italic when it is set and takes the parent's only when it is null.
- **Ours:** every minted gear and accessory name is written by `WeaponItems.displayName`, which sets
  `decoration(ITALIC, false)` on the name itself (weapons, shields, armour, tools, accessories: the five mint sites). The
  name decoded from Ben's slot 0 after the boot carries the `italic` key.
- **So a player sees no italic on any minted name: NOT A DEFECT. Nothing in the game changes.** The instrument
  (`SelfTestLines.styled`) reports `italic` when ANY node of the tree says so, without resolving inheritance, so it prints
  the wrapper's italic. The read shape, `"<name>" [white, <colour>] italic`, is what the short gate predicts.
- **Guard, Ben's rule as a test:** `MintedNamesAreNotSlantedTest` walks every shipped gear and accessory definition through
  `WeaponItems.displayName` and requires an explicit `italic: false`; it reads the five mint sites; and it reproduces BT1's
  read shape from the READ wrapper.
- **What only Ben's eyes settle, MARKED FOR HIS ANSWER:** *(Ben's answer on what he sees in the game goes here.)*

## R6 — BF6 ALIGNS HIM RIGHT BEFORE THE PRESS

BF6 aligned him, then waited 40 ticks beside its wolf and zombie before the press; his feet moved 0.029 east (the throw
read `104.529`). The row now runs `execute align xz positioned ~0.5 ~ ~0.5 run tp @s ~ ~ ~ -90 90`, `wait: 2`, then the
press (the TURN_THEN_PRESS lint needs a wait; 2 is the shortest it takes). The short gate predicts every rest point
against the throw's own `at=` (`OwedRowShapesTest` pins the shape).

## CC'S CALLS

1. R2's lint rule covers any equipped accessory, not the Blink by id (no content table in the lint).
2. R2's settle is 6 ticks, one past the reconcile period.
3. R3: `reset_charge` kept as a documented no-op (accepted by the seat).
4. R4: the clear is at a gate's SETUP only, within the linger radius 48, never a marker or a fused body.
5. R4: the ENTITIES line names types sorted by name with counts; POSITIONS appends the type to each entry.
6. R6: the second align with a wait of 2 (alternative: predict against the throw point only).

## OWED, CARRIED

- The strays' identity (R4): unidentifiable from the old log; the next setup line names any.
- Ben's answer on what he sees (R5), marked above.
