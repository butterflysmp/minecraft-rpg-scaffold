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
  **SUPERSEDED, 2026-11-27 (D1):** that form cleared round wherever he stood before the pad step; the clear now runs
  only on the pad, bounded to its region, in the pad step's own completion: see D1 AND THE F5 RULINGS, P4.
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
- **What only Ben's eyes settle, MARKED FOR HIS ANSWER:** **BEN, VERBATIM, 2026-11-26: "1. No item names should not look slanted".** The READ above says a player sees them upright: no change.

## R6 — BF6 ALIGNS HIM RIGHT BEFORE THE PRESS

BF6 aligned him, then waited 40 ticks beside its wolf and zombie before the press; his feet moved 0.029 east (the throw
read `104.529`). The row now runs `execute align xz positioned ~0.5 ~ ~0.5 run tp @s ~ ~ ~ -90 90`, `wait: 2`, then the
press (the TURN_THEN_PRESS lint needs a wait; 2 is the shortest it takes). The short gate predicts every rest point
against the throw's own `at=` (`OwedRowShapesTest` pins the shape).

## D1 AND THE F5 RULINGS, P1 TO P7 (the seat, 2026-11-27)

**D1, the defect the seat found in this PR, confirmed:** R4's first form cleared dropped items at a gate's SETUP CLEAN
round `stage()`, which is the pad's stand only once a PAD step has run, and "where the run began" before it. The FIRST
gate's setup CLEAN runs before its pad step, so it would have cleared items within 48 of wherever Ben typed the command.
The boot of `1f447197`'s first CLEAN read `return=-102.41 63.00 7.19`, his own ground. **CC's F5 read then found two
more acts of that CLEAN reaching his ground before the pad step, both since the pad existed:** the linger pass
(`endLingering`: every vanilla projectile, area-effect cloud and marker of ours within 48 of him, a thrown trident or an
arrow in a wall included) and `endWork` (every pending core-work chain within 48 of him, anyone's). And the run-end
restore removed every XP orb within 48 of wherever he was. **The seat's miss, in its words:** *"endLingering and endWork
have run on Ben's own ground in the first CLEAN of every run since the pad existed, and I approved every one of those
boots without reading what the first CLEAN reaches before the pad step."*

- **P1, BEFORE A PAD STEP HAS PLACED HIM, A RUN TOUCHES NOTHING IN THE WORLD.** `SelfTestRun`'s row-start CLEAN reads
  `padStand()` (empty while `stagePoint` is null); the linger pass (`endWork`, `endLingering`) runs only with it, and the
  line says which: the CLEAN line ends **` world=<pad|skipped>`**, and with no pad the linger line is **`LINGER skipped`**
  (CC'S CALL: one line saying it was skipped, rather than no line; a frozen gate's CLEAN predictions are read by their
  named fields). The item clear is not the CLEAN's at all (P4). What still runs is HIS OWN: the block restore (empty
  before any row), the inventory restore at a gate boundary, his summons and fused chunks removed, the drum, blink and
  cooldown resets. **A KNOWN COST TO HIM, SAID SO:** a run starts without his summons and his fused chunks (cause CLEAR,
  as today); they are not put back at the run's end.
- **P2, ON THE PAD, EVERY WORLD-TOUCHING CLEANUP IS BOUNDED TO THE PAD'S OWN REGION** (`SelfTestPadShape.inRegion`,
  `bounds`): in blocks from the stand's column, x −3..46 and z −14..14 (the interior x −2..45, z −13..13, and its rim),
  and dy −1..16 from the floor (**CC'S CALL: headroom 16**, above every arc the pad's rows throw; the alternative, the
  rim's own 4, a lobbed arc can clear). The linger pass and the item clear scan that box (`padEntities`: the box, then
  each body tested against the region); `endWork` cancels the chains whose point is in it (`PendingWork.cancelIn`; the
  sphere `cancelNear` keeps its test and has no caller now). **`removeTagged` is left a 48 sphere round him:** it is
  tag-bound (only bodies carrying `selftest`), so it can touch nothing of his, and bounding it to the box would leave a
  tagged mob knocked over the rim alive (a leak).
- **WHAT OF A ROW CAN LEAVE THE BOX:** an ability projectile above the rim's height (dy 5 and up) flies on past x 46 or z
  ±14: no on-pad row aims one there (shots go at eye height, 1.62, and stop at the barrier rim), but if one did, its chain
  point leaves the box, so the next CLEAN does not cancel it; it ends at its own max lifetime, and its marker is
  non-persistent with its despawn armed (`spawnMarker`), so it is never saved. **A bounded leak, not widened to a
  sphere** (the seat's P2). His fused chunks are his own and are removed wherever they are (P1). A tagged mob outside the
  box is still `removeTagged`'s.
- **P3, THE RUN-END RESTORE** removes an XP orb only when a pad step has placed him AND the orb is in the pad's region
  (`SelfTestSnapshot.removedOnRestore`'s fourth argument; `SelfTestRun.end` hands it `padStand().orElse(null)`). **READ,
  everything else `restore()` does to the world:** a tagged mob within 48 is removed (tag-bound, kept); the last chain's
  saved blocks are put back (none before a row ran); he is teleported to his start; spawn_mobs, spawn_phantoms,
  keep_inventory, immediate_respawn, the time and the difficulty are set to the values captured at run start (before a
  pad step none was changed, so each write puts back what is there). Nothing else of his ground is touched.
- **P4, THE FIRST GATE'S ITEMS ARE STILL CLEARED, ON THE PAD.** The item clear runs in `pad()`'s own completion, after
  `stagePoint` is set and the `PAD stood` line, before the setup goes on: **`ITEMS cleared=<n> pad [<type> x<k>, …]`**.
  Every gate with a pad step (each begins with it) starts with a clean pad, the command's first included; a scenario with
  no pad step clears no item (F3).
- **P5, THE GATES WITH NO PAD STEP: NOTHING ABOUT THEM CHANGES HERE.** By P1 their CLEAN now touches nothing in the world.
  The lint rule **`PAD_FIRST`** (a gate's setup begins with `pad:`) reports them as KNOWN by name
  (`PadFirstLintTest.KNOWN`, the count pinned), so no new gate can ship without it. **THE COUNT IS 42, NOT 40:** CC's F5
  report said 40 and the seat's ruling repeated it; READ over the shipped files, 55 gates, 13 with a pad step (each its
  setup's first), 42 with none anywhere.
- **P6, tests and mutations:** `SelfTestPadRegionTest` (the region's every bound, a point within 48 but outside the box,
  the lines, the restore's orb rule), `SelfTestWorldUntouchedBeforePadTest` (no world call before a stand point; the box
  on the pad; the item clear in the pad step's completion; the restore handed the pad or nothing), `PadFirstLintTest`,
  `PendingWorkTest.cancellingInABoxEndsTheTasksInsideItAndNoOthers`. A mutation per rule.
- **P7:** `GATE-held-target.md` re-predicts every setup's CLEAN, LINGER and ITEMS lines.

## DATED NOTE, 2026-11-27 — THE SEAT'S V7, OWED FROM THE BOOT OF `ffacaa76` (GATE-held-target READINGS)

The product read as predicted (the seat's V1); the five MISSes were the gate's, ruled owed and not blocking (V2 to V5,
V7). Recorded here from the seat's brief of 2026-11-27 (via Ben), and built in `feat/owed-v7`, stacked on
`feat/mana-battery`. The ruled form, in the brief's words:

> (a) EVERY PAD GATE'S SETUP CLEARS HIS ACCESSORY SLOTS, in its pad step's completion after he stands on the pad (the
> run's snapshot already puts them back, H15); the CLEAN or pad line says what it cleared. The lint's SELF_STAGED covers
> it. A row that wants an accessory equips it itself, as the tome gates do.
> (b) A BLINKING ROW WITH A SUMMON PRESENT aligns him in the tick before the trigger is staged, AND BlinkPadLandingTest
> proves the landings for every start point within 0.5 of the row's stand point (the box of drift), not one exact point;
> the gate predicts safe= as the set's bounds where it can vary.
> (c) S5 waits until a SUMMON hit is read before its kill (a wait long enough, stated with why) and ends with a clear, so
> RESTORED reads summons=0.
> (d) THE SUMMON target LINE GAINS tick=, pinned by test, so "within N ticks" is readable.
> (e) THE 42 GATES WITH NO PAD STEP REFUSE TO RUN. THE FORM (the seat's ruling): when any gate named in a selftest
> command has no pad step as its setup's first step, NOTHING of the command runs, no snapshot is taken, and one chat
> line says so: SEAT PLACEHOLDER wording "Selftest: '<gate>' predates the pad and would run on your ground. Refused." No
> override word. The gates stay loaded and counted (R0d's numbers do not move). PadFirstLintTest's known list stays the
> record of which they are. READ whether any of the 42 legitimately runs somewhere that is not his ground (a fight world
> of its own): if so, do NOT exempt it; name it in the report and the seat will rule.

Each item's as-built account (file, test, mutation) is added under *V7 AS BUILT*, below, by the branch that builds it.

## CC'S CALLS

1. R2's lint rule covers any equipped accessory, not the Blink by id (no content table in the lint).
2. R2's settle is 6 ticks, one past the reconcile period.
3. R3: `reset_charge` kept as a documented no-op (accepted by the seat).
4. R4: the clear is at a gate's SETUP only, within the linger radius 48, never a marker or a fused body.
5. R4: the ENTITIES line names types sorted by name with counts; POSITIONS appends the type to each entry.
6. R6: the second align with a wait of 2 (alternative: predict against the throw point only).

## OWED, CARRIED

- The strays' identity (R4): unidentifiable from the old log; the next setup line names any.
- Ben's answer on what he sees (R5): given, above.
- **THE 42 GATES WITH NO PAD STEP (P5): predate ruling A; run on his ground; to be refused or moved to the pad in a later
  PR the seat will rule.** boltor-recipe, breacher, breacher-rework, crystal-shard-staff, deft-hands, drag-additive,
  dragon-beam, dragon-blast, dragon-blink, dragon-bodies, dragon-growth, dragon-shot, dragon-skreel, dragon-split,
  dragon-teleport, fight-arena, fight-down, fight-dragon, fight-end, fight-revive, fight-rules, fight-site, fight-template,
  knell-beam, legacy-a, legacy-b, level, living-lantern, loot-roll, melee-cell, mining-auto, nexus-back, nexus-polish,
  remove-dev-weapons, sunder, sword-convert, sword-smithing, sword-tiers, unique-enchant-slot, vanilla-bows, wither-status,
  withered-shortbow. Ben is asked not to type them meanwhile.
- **His summons and fused chunks** are removed at a run's first CLEAN and not put back (P1's known cost).
