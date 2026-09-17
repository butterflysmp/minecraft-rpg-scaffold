# GATE — Gear Score (Slice 12)

**Status: NOT RUN.**

**Every prediction below was written BEFORE any boot, and no prediction is edited once a row has been
read.** Readings go in the `READ` column beside the prediction they answer, never over it.

**GAME MODE: SURVIVAL, for every row unless the row says otherwise.** Declared per the standing debt
in `CLAUDE.md` — sixteen `GATE-*.md` files still owe this line and this one is not going to be the
seventeenth.

> **WHY SURVIVAL MATTERS LESS HERE THAN IT DID FOR THE PLUME, AND IT IS STILL DECLARED.** The
> creative-divergence register's shape is *creative removes a COST* — ammunition, durability,
> consumption. **Gear score scales a number rather than spending anything**, so no row below is
> satisfied for free by a removed cost. The two rows that could diverge are **R7** (the armour bar,
> which creative hides entirely) and **R9** (the enchant-table control). Both say so in the row.

---

## What is owed before this gate can be completed

**R6 IS UNSTAGEABLE TODAY AND THAT IS NOT A DEFECT.** `GearScoreBand.SPREAD_OWED` and `SKEW_OWED`
both ship **0**, because they are Ben's numbers and a plausible-looking spread would become a
precedent (see that class). With a zero-width band every roll lands exactly on the centre, so
*"a drop rolls somewhere inside the band"* has no band to land inside.

**What R6 CAN witness today** is the half that does not need a width: a drop bands on the average and
**clamps at 400**. That is written as R6 and is stageable. The spread half is **R6b, marked OWED**,
and is the one row in this file that cannot be run until Ben rules two numbers.

---

## The two rows that cost something, and are sole witnesses

### R1 — Two scores of one definition deal damage in the ratio of their scores

**Sole witness. No test in any module can construct an `ItemStack`, so this claim exists nowhere
else.** The unit suite asserts the *arithmetic* (`GearScoreTest.twoScoresOfOneDefinitionDealDamageInTheRatioOfTheirScores`);
only a boot can show the arithmetic actually reaching a hit.

**Staged at 175 and 340. NOT at 100 and 400, and the departure is deliberate.**

> Ben's ruling states the claim as *"a GS 100 weapon and a GS 400 weapon … in exactly the ratio
> 100:400"*, and **100:400 is a clean 4x** — which is satisfied by a defect that doubles twice, by one
> that squares the ratio of the *hundreds digits*, and by several other wrong arithmetics. `175:340`
> is satisfied by the rule and by nothing convenient: neither is a multiple of the other, neither is a
> multiple of the baseline, and `340/175` is not a ratio any plausible bug produces by accident. This
> is CLAUDE.md's collision rule applied to a gate row's staging rather than to a unit fixture.

| | |
|---|---|
| **Setup** | `/rpg give boltor` twice. `/rpg gearscore set 175` on the first, `set 340` on the second. Same mob, same element, no enchants on either (a fresh give may roll candidates — confirm with `/rpg enchant show` that neither has an ACTIVE damage enchant, or the ratio is measuring Sharpness). |
| **Predict** | Boltor authors `attack_damage: 19`. The 175 bolt deals **33.25**; the 340 bolt deals **64.6**. Cross-multiplied: `33.25 x 340 = 11305 = 64.6 x 175`. |
| **Predict** | The tooltips read `Gear Score: 175` and `Gear Score: 340` BEFORE either is fired — the number is readable off the screen ahead of the hit, which is what makes this falsifiable rather than a post-hoc reading. |
| **READ** | |

### R2 — An item minted before this slice deals exactly the damage it dealt yesterday

**Sole witness, and NOTHING in the suite can see it.** `GearScoreTest.anUnstampedItemScalesNothing`
asserts that `ABSENT` is the identity *in the arithmetic*; it cannot assert that a real unstamped
`ItemStack` reaches that arithmetic at all. This is the absent-equals-100 claim, and it is the whole
justification for shipping with **no stamp, no migration and no schema bump**.

| | |
|---|---|
| **Setup** | `/rpg give emberblade`, then **`/rpg gearscore clear`** on it. That is the only way to manufacture a legacy item: nothing mints unstamped gear any more, so without `clear` this row would need an inventory saved from before the build. |
| **Predict** | `/rpg gearscore show` reports `Held: no stamp (reads 100)` — **not** `Held: 100`. The two are reported differently on purpose; if they read alike this row is indistinguishable from one staged on a genuine 100. |
| **Predict** | The tooltip carries **NO** `Gear Score:` line at all. A legacy item is quiet, not labelled 100. |
| **Predict** | It deals emberblade's authored **7**, exactly — the same number it dealt before this slice existed. |
| **READ** | |

---

## The average

### R3 — Six slots, with slots deliberately empty

**The row that proves the denominator is 6 and not "however many are filled".**

| | |
|---|---|
| **Setup** | Strip to nothing. Equip ONE piece of our armour, `/rpg gearscore set 400` on it. Both hands empty. |
| **Predict** | `/rpg gearscore show` reports `Slots: [400, 0, 0, 0, 0, 0]` and `Gear Score: 66` (`400 / 6 = 66.67`, floored). |
| **Predict** | **If it reports 400, empty slots are being skipped** and the denominator has become the filled count. That is the `MUT12-SKIPEMPTY` defect, in the field. |
| **READ** | |

### R4 — The top two across hotbar AND offhand together

**Staged so that a wrong rule gives a DIFFERENT number, which took one attempt to get right.**

> **THE FIRST STAGING OF THIS ROW WAS HOLLOW AND IS RECORDED RATHER THAN QUIETLY REPLACED.** It put
> the offhand at **400** and the hotbar at 250 and 175. Under the correct rule the top two are
> `[400, 250]`; under the wrong rule — *the offhand gets a guaranteed seat and the hotbar contributes
> its best* — they are **also** `[400, 250]`. **Identical answer, so the row measured the fixture.**
> That is CLAUDE.md's *control that succeeds for the wrong reason*, caught by asking what the row
> reports if the rule it checks is deleted.
>
> **The fix is to make the OFFHAND the LOWEST of the three**, so a guaranteed seat displaces a
> higher hotbar item and the average moves.

| | |
|---|---|
| **Setup** | No armour. Hotbar: two weapons at `set 400` and `set 250`. Offhand: a shield at `set 175`. |
| **Predict** | One pool, top two: `[400, 250]` → `Slots: [0, 0, 0, 0, 400, 250]`, `Gear Score: 108` (`650 / 6`). |
| **Predict** | **If it reports 95**, the offhand is being given a slot of its own (`400 + 175 = 575 / 6`) instead of competing in one pool. |
| **READ** | |

### R5 — A tool in the hotbar does not enter the top two

**Ben's ruling: a pickaxe must never become one of the top two and raise a drop level without
contributing anything to a fight.**

> **Staged so the two answers differ, for R4's reason.** A pickaxe reads `ABSENT` (100) *if it is
> admitted at all*, so pairing it with a 340 and a 250 weapon would be hollow — 100 loses the top-two
> contest anyway and the row would pass either way. **One scored weapon only**, so an admitted
> pickaxe actually takes the second seat.

| | |
|---|---|
| **Setup** | No armour, offhand empty. Hotbar: ONE weapon at `set 175`, plus a minted tool (`/rpg give` any pickaxe). |
| **Predict** | `Hand pool: [175]` — the tool is **absent from the pool entirely**, not present as a 0. `Gear Score: 29` (`175 / 6 = 29.17`). |
| **Predict** | **If it reports 45**, the tool was admitted and read as a baseline item (`275 / 6 = 45.83`). That is `MUT12-TOOLID` in the field. |
| **Predict** | `/rpg gearscore set 300` **on the held pickaxe** still leaves it out of the pool on the next `show`: the stamp path refuses tools through `GearScore.scoreable`, so the write lands on the PDC and the read never counts it. **Two independent refusals, and this predicts both.** |
| **READ** | |

---

## The roll

### R6 — A drop bands on the average and never exceeds 400

| | |
|---|---|
| **Setup** | Equip four armour pieces and two weapons, all at `set 400`. Confirm `Gear Score: 400`. Then `/rpg give` a fresh weapon and read its score. |
| **Predict** | The fresh weapon rolls **400**, not 401 and not 500. With the band at zero width the roll lands on the centre, and `clampDrop` holds it at the soft cap regardless. |
| **Predict** | `show` reports `Next drop would roll: 400` and names the band as `spread 0, skew 0 -- BOTH OWED`. |
| **Predict** | With everything stripped instead, a fresh `/rpg give` rolls **100** — the 0-average player's whole band is under the floor. This is the *new player holding one sword* consequence Ben accepted, observed directly. |
| **READ** | |

### R6b — A drop rolls somewhere INSIDE the band — ***OWED, UNSTAGEABLE***

**Cannot be run until `GearScoreBand.SPREAD_OWED` and `SKEW_OWED` are ruled.** With both at 0 the
band is a point, so there is no inside. Left in the file rather than omitted, so the gate records
that the coverage is *owed* rather than *absent* — an unruled case must read as unruled.

| | |
|---|---|
| **Predict** | (unwritable until the two numbers exist) |
| **READ** | ***OWED*** |

---

## The trap rows

### R7 — The armour bar is not empty on a high-scored set

**`nativeArmor` must accumulate the UNSCALED points.** Feed it the scaled figure and
`ArmorBarOverride` over-subtracts, the vanilla attribute lands negative, Minecraft clamps it to zero,
and **the bar reads EMPTY on the most-armoured player in the game** — while the stat, the mitigation
and the tooltip all stay correct. Nothing throws, and no unit test in any module can see it.

**SURVIVAL ONLY, AND THIS ROW IS VOID IN CREATIVE** — creative hides the armour bar outright, so the
reading cannot be taken there at all.

| | |
|---|---|
| **Setup** | Full diamond set, all four ours, each at `set 400`. |
| **Predict** | The armour bar is **visibly populated**, not empty. |
| **Predict** | Each piece's tooltip shows a Defense figure 4x its material's points — a diamond chestplate reads **32** (`8 x 4`), a helmet **12** (`3 x 4`). |
| **Predict** | `/rpg stats` Defense is the sum of the four scaled figures, and the tooltips add up to it. |
| **READ** | |

### R8 — A relog does not downgrade anything

**A re-mint happens on every join, and losing the stamp there reverts every scored item to 100 —
silently, to a legal value, with the whole suite green.**

| | |
|---|---|
| **Setup** | Hold and wear scored gear from R7. `/stop`, reboot, rejoin. |
| **Predict** | Every score is **unchanged**. No tooltip loses its `Gear Score:` line and the average is the same number as before the restart. |
| **Predict** | `/rpg refresh` also leaves every score unchanged — it re-mints deliberately, which is the same path. |
| **READ** | |

### R9 — CONTROL: the vault, the hub and the enchant table still behave

**The control, and it is not a formality: this slice widened three lore builders, the one carry
method, the reconcile loop and the Brigadier tree.**

| | |
|---|---|
| **Setup** | Open `/menu`. Open the vault screen. Enchant something at a real table. `/rpg vault dump`. |
| **Predict** | The hub paints every slot as before, with **one added line** on the stats head: `Gear Score` between the progression block and the eight combat stats. |
| **Predict** | The enchant table still rolls, still charges, and an enchanted item **keeps its gear score** across the table's re-mint. Protection still composes into the Defense line — **and the Defense line now shows the SCALED base with the UNSCALED bonus added on top**. |
| **Predict** | The vault still round-trips an item with its PDC intact, **score included**. |
| **Predict** | *(Creative note: `MenuRouting` refuses `CLONE_STACK`, unchanged by this slice.)* |
| **READ** | |

---

## Mutations — applied and measured, no kill-set predictions

Run before this file was written, on the tree at the slice's own HEAD. **Every one was applied to a
scratchpad-backed copy (`cp`, never `git checkout --`), verified with both halves of the marker grep
where the marker was distinct from the original, verified with a LINE DELTA AGAINST THE PRISTINE COPY
in every case, and restored with a byte-identity check.** Full-tree marker sweep afterwards: **0**.

| mutation | what it changed | reddened |
|---|---|---|
| `MUT12-SOFTCAP` | `clampDrop` ceiling `SOFT_CAP` → `HARD_CAP` | **2** — `aRollNeverExceedsTheSoftCap`, `theDropClampStopsAtTheSOFTCapWhereTheReadClampWouldNot` |
| `MUT12-ABSENT` | `orAbsent` returns `EMPTY` instead of `ABSENT` | **1** — `anUnstampedItemReadsAsTheBaselineRatherThanZero` |
| `MUT12-TOPONE` | `topTwo` loop bound `HAND_SLOTS` → `1` | **2** — `theTwoHighestHandsCountAndTheRestDoNot`, `theSixSlotsAreFourArmourThenTheTwoHighestHands` |
| `MUT12-TOOLS` | `scoreable`: `case TOOL -> false` → `true` | **2** — `weaponsArmourAndShieldsScoreAndToolsDoNot`, `everyGearClassIsAnsweredWithoutADefaultArm` |
| `MUT12-SKIPEMPTY` | `averageOf` skips empties and divides by the filled count | **2** — `aNewPlayerHoldingOneBaselineSwordAveragesSixteen`, `theAverageIsOverAllSixSlotsWithEmptiesCounted` |
| `MUT12-NOCARRY` | `GearScoreItems.carry` **deleted** from `carryInstanceData` | **1** — `theGearScoreIsCARRIEDAcrossAReMint` |
| `MUT12-TOOLID` | `candidateScore` also reads `keys.toolId` | **1** — `theAverageReadsTheThreeScoreableTagsAndNotToolId` |
| `MUT12-NATIVEORDER` | `nativeArmor += vanilla` moved BELOW the scaled declaration | **1** — `theNativeArmourSumIsTheUNSCALEDOneAndAccumulatesFirst` |
| `MUT12-NATIVEVALUE` | the same accumulate changed to `+= scaled` | **1** — same row, **but on a DIFFERENT assertion** (see below) |
| `MUT12-NOSCALE` | the MAIN_HAND modifier reverted to the authored `attackDamage()` | **1** — `weaponDamageIsSCALEDByTheHeldItemsScore` |

### Three things the mutation pass found, which are the reason it was run

**`MUT12-NOCARRY` IS A DELETION, SO THE MARKER GREP DOES NOT APPLY TO IT.** The replacement text is
empty, which makes both halves of the grep uninterpretable rather than merely wrong — CLAUDE.md's
eighth mutation-lie. **The line delta was the whole verification**: `-1`, with the deleted line read
back rather than counted.

**`MUT12-NATIVEVALUE` COULD NOT BE ISOLATED TO ITS OWN AXIS, AND THE ROW'S SECOND ASSERTION IS
THEREFORE UNWITNESSED.** `scaled` is declared *below* the accumulate in the correct ordering, so
`nativeArmor += scaled` **does not compile** there; the only mutation that compiles moves the
accumulate down first, and the row then dies on the FIRST assertion instead. Measured: it failed on
*"the native sum must still accumulate the raw vanilla points"*, and the loop banning
`nativeArmor += scaled` **was never reached**. That ban is kept — it becomes reachable exactly when
someone hoists the declaration, which is the tidy-up it exists to catch — and it is **named as
unwitnessed in the test's own javadoc** so nobody mistakes it for a verified guard.

**A MARKER GREP LIED DURING THIS SLICE, AND IT IS WHY THE SIGNATURE TEST FILTERS COMMENTS.** A grep
for `OptionalInt averageScore) {` reported a signature widening as applied. It had matched a
**different method's parameter list**; the widening had silently no-opped, because a `{\n`-adjacent
pattern **cannot match on a CRLF working tree** — `\r` sits between the brace and the newline. Caught
by *printing the region* instead of trusting the grep. Both failures are recorded in
`GearScoreWiringSignatureTest`'s class javadoc.

### What the mutations do NOT cover, stated so the gap is not rediscovered

`GearScore.BASELINE`, `MIN` and `ABSENT` are **all 100**, and they are three unrelated quantities —
the divisor, the floor, and what an unstamped item reads as. **No mutation can distinguish them and
no row in the suite can either**; swapping any two leaves everything green. Naming all three is the
only protection available, and `GearScore`'s class javadoc says so at the top.

---

## A measured finding this slice did not fix: three mage weapons do not scale

**Measured at the slice's own HEAD, over `paper/src/main/resources/content/weapons/` — DURABLE as a
mechanism, PERISHABLE as a file list (the next weapon that ships changes it).**

Gear score scales the **`ATTACK_DAMAGE` stat**, which is fed from a weapon's authored
`attack_damage`. Six shipped weapons declare a positive one and therefore scale: `boltor` (19),
`dragons_plume` (34), `locust` (26), `ironblade` (8), `emberblade` (7), `hunters_bow` (6).

**`ember_staff`, `flint_staff` and `lapis_staff` declare none.** Their damage is authored as a literal
`type: damage / amount:` inside the weapon's own trigger — `ember_staff` is `amount: 16` — which never
reads the stat. So **a scored staff shows a number on its tooltip and its damage does not move.**

> **This is the ELIGIBILITY half of CLAUDE.md's own rule, arriving a fifth time**, and it is reported
> rather than patched because the fix is a **ruling, not a mechanism**: a staff's trigger damage is
> the weapon's damage in every sense a player cares about, but the effect type carrying it
> (`type: damage`) is the same one **class abilities** use, and Ben ruled that *weapon damage and
> armour defense* scale and **nothing else**. Scaling at that seam would scale ability damage too,
> which is outside what was ruled.
>
> **The tooltip is NOT dishonest today**, and that is deliberate: `GearLoreLines.SCORE_LABEL` promises
> nothing about damage, because a shield is scoreable and scales nothing either. A staff's score still
> does real work — it feeds the wielder's average, which bands their next drop.
>
> **What is owed is Ben's answer to one question:** should a `type: projectile` MAGE weapon's authored
> trigger damage scale with gear score? **Unruled, not excluded** — the question has never been put.
