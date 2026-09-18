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

## The band is RULED, and this gate is completable

**Ben ruled the band while the slice was being built: a drop rolls uniformly over `average - 5` to
`average + 15`.** In `GearScoreBand`'s own parameterisation that is **SPREAD 10, SKEW 5** --
`skew - spread = -5`, `skew + spread = +15`, both endpoints exact.

**So nothing in this file is owed any more.** R6b was marked OWED and UNSTAGEABLE in the first
draft of this gate, because a zero-width band has no inside to land in; it is now a full row with
predictions, and the constants are named `SPREAD` and `SKEW` rather than `SPREAD_OWED` and
`SKEW_OWED`. **The zero was discharged by a ruling, not deleted because someone tired of it** --
`GearScoreBand`'s javadoc keeps that history, since the next person to meet an owed number in this
codebase should be able to see what the placeholder bought.

**HOW LONG THE CLIMB TAKES -- SIMULATED BY THE OPERATOR, NOT DERIVED HERE: median 182 scored drops**
from the floor to the 400 soft cap over 400 simulated players, min 150, max 216. **A later tuning
pass RE-RUNS that simulation rather than adjusting the figure**, and its invalidator is an event:
any change to SPREAD, SKEW, or `averageOf`'s denominator or floor. No row below measures it -- the
suite asserts the band's arithmetic and says nothing about the length of the walk.

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
| **Predict** | The unclamped band at a 400 average is `395..415`. The fresh weapon rolls somewhere in **395..400** — never 401, never 415, never 500. The soft cap truncates the top half of the band, which is exactly what `clampDrop` is for. |
| **Predict** | `show` reports `Next drop rolls in: 395..400` — **both ends printed already clamped**, so the cap is visible in the readout before a single drop is taken. |
| **Predict** | With everything stripped instead, the average is 0, the unclamped band is `-5..15`, and a fresh `/rpg give` rolls **exactly 100** — `show` reports `100..100`. The whole band is under the floor. |
| **READ** | |

### R6b — A drop rolls somewhere INSIDE the band, and the band is 21 values wide

**STAGEABLE AS OF BEN'S RULING.** This row was OWED and unstageable in the first draft of this gate;
with SPREAD 10 and SKEW 5 there is now an inside to land in.

**Staged at a 250 average, deliberately: both clamps are far away**, so the endpoints observed are
the BAND's and not the floor's or the cap's. At 400 the cap supplies the top and the row would be
measuring `clampDrop` again — which R6 already does.

| | |
|---|---|
| **Setup** | Six slots all at `set 250` — four armour, two hands. Confirm `Gear Score: 250`. Then `/rpg give boltor` **twenty times**, reading each score. |
| **Predict** | `show` reports `Next drop rolls in: 245..265`. |
| **Predict** | Every one of the twenty lands in **245..265 inclusive**. None below 245, none above 265. |
| **Predict** | **They are NOT all the same number.** A band 21 values wide over twenty draws that all agreed would mean the draw is being ignored — the defect a single-drop reading cannot see, which is why this row takes twenty. |
| **Predict** | Their mean sits **above 250**, because the skew is +5 and not 0. A mean at 250 means the band is centred and the ladder converges instead of climbing. *(Twenty draws is a small sample — a mean landing a little either side of 255 is noise; a mean at or below 250 is not.)* |
| **READ** | |

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

## SEVEN of thirteen weapons do not scale — and Ben has RULED that trigger damage does

**The first draft of this section said THREE, and it was narrower than the truth.** It named the three
staves and stopped, because those were the weapons whose damage I had traced. Re-measured across all
thirteen weapon files:

| | weapons | `attack_damage` |
|---|---|---|
| **SCALE (6)** | boltor 19 · dragons_plume 34 · locust 26 · ironblade 8 · emberblade 7 · hunters_bow 6 | authored, positive |
| **DO NOT (4)** | ember_staff · flint_staff · lapis_staff · ability_stone | **no key at all** |
| **DO NOT (3)** | cursed_emerald · quiver_stone · volley_stone | **authored `0`** |

Instrument: `grep -c '^attack_damage:'` and `grep -m1 '^attack_damage:'` over each of the thirteen
files, anchored at column 0 so a prose mention cannot be counted as the key. PERISHABLE — the next
weapon that ships changes both the counts and the lists.

**The "six scale" figure was exact. The complement was not**, and the two failures are different:
four weapons author no `attack_damage` key, and three author it as `0`. **`cursed_emerald`'s `0` is
not a bug** — it is authored deliberately, and its own file says so: there is no basic attack on that
weapon, the volley is the whole thing, and that is a choice. Its damage is six shots of 27 in
`on_hit`.

### Ben has ruled: TRIGGER DAMAGE SCALES. That is slice 12b, not a patch to this one.

**Four of the seven become scaling weapons under that ruling** — the three staves and
`cursed_emerald`. **It is a SLICE and not an amendment**, because it is a second scaling site in a
different subsystem: `EffectApplier`'s damage path, which already threads `enchantDamagePercent`,
`classDamageBonus`, `chargeScale` and `critMultiplier`. Bolting a fifth factor onto a branch that is
built, measured and verified is how one clean slice becomes two half-slices. **12b comes off 12's
squash.**

> **THE OLD REPO ALREADY DID THIS, AND THE FILE SAYS SO:** `GearScore.scalePower(42, level)` — with
> the note *"no item levels here, which is why 27 had to be re-decided by the operator rather than
> ported"*. **42 was the pre-scaling authored value; 27 is what it became with nothing to scale it.**
> So this ruling reconnects the weapon to the system it was written against.
> 
> **The 27 is to be RE-EXAMINED against 42 when scaling lands — not silently reverted.** The operator
> ruled 27 on its own terms, and a revert would be a derivation replacing a ruling, which is the
> descent defect `GearScoreBand` names.

### STILL UNRULED, AND NOT MINE TO DECIDE: the three stones can be a top-two hotbar slot

**`ability_stone`, `quiver_stone` and `volley_stone` remain unscaling after the trigger ruling** —
and all three are in the WEAPONS registry, so all three **carry a gear score and can occupy one of
the two hand seats.** A high-rolled stone therefore raises the level of every drop the player takes
while contributing nothing to a fight.

**That is precisely the exploit Ben closed for TOOLS, and the tools ruling does not reach items in
the weapons registry** — `GearScore.scoreable` gates on `GearClass`, and all three stones present a
fighting class (`volley_stone`'s own file calls it **a test instrument**; `ability_stone`'s says its
`class: mage` is arbitrary — *"a dev tool; class is required, mage is as good as any here"*).

**PUT IT TO BEN IN 12b. DO NOT EXCLUDE THEM ON MY OWN AUTHORITY.** An exclusion invented here would
be a ruling with no author, which is the failure the *unruled, not excluded* rule exists to stop.
