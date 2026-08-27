# PLAN — the enchant economy pass: XP cost and bookshelf power

Closes four `NEXT.md` entries at once: *"The XP economy and bookshelf power are deferred; unlocking
is free in Pass 1."*, its restatement in the still-deferred list, *"The bookshelf readout is a
labelled placeholder, not a `0%` count."*, and *"Unlocking is FREE, and the XP economy gates the same
click."*

Branched off `origin/master` at `10f12e3` (PR #20, the enchant rolls pass), verified from origin
rather than assumed — `git rev-parse origin/master` and `git rev-parse master` printed the same sha,
tree clean.

## What this pass is

The table has shipped twice and charged nothing both times. PR #19 built the menu and left
`applyCandidateClick` deliberately free, with `EnchantClickIntent`'s javadoc reserving the seam in as
many words: *"The cost check goes in FRONT of this call, never inside it."* PR #20 made a weapon
arrive pre-rolled, so there are finally real candidates to buy. Slot 8 has been sitting there since
#19 saying **"Not implemented yet"**.

This pass takes that reserved seam, and the forecast held exactly: `EnchantClickIntent` needed no
change at all, and the whole economy is a guard and two lines in front of the transition plus a
deduction behind it.

**The currency is vanilla XP, counted in points.** Verified rather than assumed:
`RpgListeners.onPlayerDeath` already sets `setKeepLevel(true)` and `setDroppedExp(0)`, so XP survives
death here and is a durable wallet rather than something you lose on the way to the table.

## The commits

| # | commit | what it is |
|---|---|---|
| 1 | `905d9e5` feat(core): vanilla's XP curve, in integers | `XpCurve` — level↔points, both directions |
| 2 | `419712d` feat(core): an unlock has a price in XP | `EnchantCost` — the price and the discount |
| 3 | `1a91324` feat(core): the ring an enchanting table counts | `BookshelfRing` — the 32 offsets |
| 4 | `d36ea61` feat: the table counts its bookshelves | `BookshelfPower`, the `Block` thread-through, slot 8 |
| 5 | `296060a` feat: an unlock is paid for in XP | `EnchantCharge` + the seam + the cell prices |
| 6 | this file | the record and the gate |

Commits 1–3 land with nothing importing them. Commit 4 lands the readout as **"Bookshelf Power
N/30" with no discount claim** — nothing is discounted yet, and a commit that says otherwise is a lie
for the length of a commit. Commit 5 adds the discount line at the moment the discount becomes real.

## THE MISTAKE THIS PASS WAS REWRITTEN FOR

The first design priced in **levels**: `round(BASE_levels × (1 − power/100))` with
`BASE = {16, 25, 40}`. It was wrong, and quietly.

**XP levels are not a linear currency.** Level *n* costs more points than level *n−1*, so discounting
the level COUNT discounts the actual price by far more than the label says:

| | levels | points | real discount |
|---|---|---|---|
| reach III, no shelves | 40 | `totalForLevel(40)` = **2920** | — |
| reach III, 30 power, **level model** | 28 | `totalForLevel(28)` = **1186** | **59.4% off** |
| reach III, 30 power, **points model** | — | `2920 × 70/100` = **2044** | **30.0% off** |

A "30% discount" was really 59% at III, and a different number at every rung. Boot gate row 13 is
where that is witnessed, and `EnchantCostTest` asserts `1186` by name so the discarded model stays
visible rather than being quietly forgotten.

Everything structural survived the rewrite: the seam ordering, the ring geometry, the frozen-at-open
decision and the pure/impure split were all unaffected. Only the units and the XP mechanics moved.

## The load-bearing decisions

**Points are the unit of account; levels are only ever an input or a display.** `BASE_LEVELS =
{16, 25, 40}` stays as the tuning knob because it is the unit a designer thinks in, but it is
*derived* through the curve into `{352, 910, 2920}` before anything is charged. "16 levels" is an
unambiguous amount of money at exactly one point on the curve — a player starting from zero — and
this pass is careful never to use it anywhere else. That is also what makes gate rows 4–6 readable:
set your level to 16 and the unlock takes you to exactly 0.

**The deduction is `wallet − cost` in points. `setLevel`/`setExp` are the rendering, not the
arithmetic.** This is explicitly not `setLevel(getLevel() − costInLevels)`, which was the first
draft's error wearing a different hat. Chosen over `giveExp(-n)` because that walks the levels down
through float accumulation inside NMS and bumps the scoreboard XP score as a side effect; the pure
computation is exact and reddening-tested. `Player.getTotalExperience()` is **never read anywhere** —
it does not track spends and drifts from what the client displays.

**The bar fraction is rounded, not truncated, and that is what makes a wallet exact.** Minecraft
stores the bar as a `float`. `(741f / 742f) * 742f` is `740.99997`, so a truncating read cannot
recover the point count a write just put there and a player would bleed a point on every purchase.
`Math.round` makes `totalPoints` the exact inverse of `levelFor`/`progressFor`, asserted as a
round-trip property over every total from 0 to 10000. The cost is that the wallet may read one point
above vanilla's own truncating computation, which fails *towards* the player.

**Integer arithmetic in the discount, and here it genuinely bites** — see the mutation table.

**The check goes in front of the transition; the deduction goes behind it; there is no rollback.**
Every path that can refuse — unaffordable, the model's `IllegalArgumentException`, the
`equals(before)` no-op, the two arms that only say something — sits between them, so a refused click,
a no-op click and an unaffordable click are indistinguishable from the wallet's point of view. The
deduction is then the **last mutation in the method**, which is what makes a compensating write
unnecessary rather than merely omitted. A `catch` that restored the wallet would be a second write on
an error path no test can reach, and would double-refund if the throw landed after the restore. The
residual case — an exception between the re-mint and the deduction, which no shipped path produces —
grants an enchant free. That fails towards the player and is visible on the item; charging first
would fail towards a player charged for nothing, which is visible nowhere.

**Bookshelf power is frozen at open, and the `Block` is deliberately not kept.** "Place shelves, then
reopen" is a fine interaction, and freezing buys the Folia-correct thing for free: the scan runs
inside `PlayerInteractEvent` for the very block that was clicked, on the thread that owns it, where a
re-read from `InventoryClickEvent` would not be. Keeping only the `int` means there is nothing for a
later re-read to be written against. The field is assigned **before** `render()`, which paints from
it — assigned after, every table on the server reads `0/30` for ever and only gate row 13 would
notice.

**The cell price and the charged price come from one expression.** `candidateIcon` calls
`EnchantClickIntent.of(...)` and `EnchantCharge.targetLevel(...)` — the same two calls the click
makes — rather than re-deriving from `locked`/`active`. Two expressions drift, and then the boot gate
is checking one against itself. It falls out well: `AT_MAX`, `EMPTY` and `UNKNOWN_ENCHANT` all price
at zero, so a cell that cannot be bought shows no price with no special-casing at all.

**No air-gap rule.** Vanilla wants the block between table and shelf to be transparent; this does
not, so a shelf walled in behind stone counts. It halves the reads, drops a rule players already find
opaque, and makes a full ring something a gate can actually build. The escape hatch is one occlusion
check per offset, which is itself an argument for offsets being a first-class thing.

**No creative exemption.** Vanilla enchanting is free in creative; this is not. A creative player can
`/xp` freely anyway, so the exemption buys nothing and costs an untested branch — and the gate runner
is almost certainly in creative, so an exemption would guarantee the gate never witnesses a charge.

**`/rpg enchant` stays free.** The economy gates the *table*, not the dev instrument. A dev workflow
has to be able to build a state without grinding XP, and a priced command would put a wallet in the
setup line of every future boot gate.

**`MenuIcons.placeholder` is kept, unused.** Its only consumer graduated to a real readout. It stays
because `MenuIcons` is the reusable base — `Menu`, `MenuRouting` and `MenuSafety` all landed with no
consumer at all — and because the rule it encodes is one the anvil, class-select and stat screens
will each need. The argument the other way is real: it is dead code and git remembers it.

**Each rung is priced independently.** 4182 to take one enchant to III with no shelves, 2927 with a
full ring. No bundle rate — a player who stops at I has not overpaid for it.

## What was verified, and how

`./mvnw clean package` → **BUILD SUCCESS, 643 tests** (core 371, storage 17, paper 255), against 606
at `origin/master` (core 340, paper 249). `scripts/check-tests.sh` reports per-module presence and
`scripts/check-jar.sh` passes.

Every gate figure below was **computed by running the shipped code**, not by hand: a scratch program
against `core/target/classes` printed the wallet anchors, the price grid and the end state of each
purchase row. The numbers in the gate are that output.

**Sixteen mutations were run**, each after confirming the marker was in the source, and each restored
from a scratchpad copy and re-verified green. Three of them stayed green, and those are the
interesting ones.

| mutation | result |
|---|---|
| `EnchantCost`: rewrite as `(int)(base * (1 - p/100.0))` | **1 red, and only at III@30**: *"the cell that catches a double discount ==> expected: `<2044>` but was: `<2043>`"*. I and II stay green at that power and that is **correct** |
| `EnchantCost`: rewrite as `Math.round(base * (1 - p/100.0))` | **2 red, and neither at power 30**: *"900.9 floors to 900, it does not round to 901 ==> expected: `<900>` but was: `<901>`"*, plus I@10 *"expected: `<316>` but was: `<317>`"* |
| `EnchantCost`: drop the upper clamp | 2 red — *"past the cap is the cap ==> expected: `<2044>` but was: `<-2920>`"*. A **negative** price, which the affordability check passes trivially and the deduction pays out on |
| `EnchantCost`: drop the lower clamp | 1 red — *"below zero is no discount ==> expected: `<2920>` but was: `<3066>`"* |
| `EnchantCost`: `BASE_POINTS[targetLevel]`, no `- 1` | 5 failures **and 3 errors** — every rung one too high, and III walks off the array |
| `XpCurve`: `Math.round` → truncation on the bar | 2 red — *"total 23 must survive being written out and read back ==> expected: `<23>` but was: `<22>`"* |
| `XpCurve`: `levelFor` `<=` → `<` | 3 red — *"exactly level 1's bank IS level 1 ==> expected: `<1>` but was: `<0>`"* |
| `XpCurve`: drop the progress clamp | 1 red — *"a negative bar does not subtract ==> expected: `<352>` but was: `<310>`"* |
| `XpCurve`: drop the lower half of the level clamp | 1 red — *"a negative level is no wallet at all ==> expected: `<0>` but was: `<-5>"* |
| `XpCurve`: drop the `MAX_LEVEL` cap | 1 red — *"expected: `<2147407943>` but was: `<1073744211>"*, an overflow into a wallet that is nearly free |
| `XpCurve`: compute `totalForLevel` in `int` not `long` | 2 red — the round trip at level 20000 collapses, *"expected: `<20000>` but was: `<0>"* |
| `XpCurve`: drop the bar term from `totalPoints` | 4 red |
| `XpCurve`: bar band `l <= 15` → `l <= 16` | 5 red — *"first level of the middle band ==> expected: `<42>` but was: `<39>"* |
| `XpCurve`: bar band `l <= 30` → `l <= 31` | 3 red |
| `BookshelfRing`: skip `<=` → `<`; skip `&&` → `||`; `TOP_LAYER` 1 → 2; `FOOTPRINT_RADIUS` 2 → 3 | all red **by error, not assertion** — the class-load self-check throws first: *"the bookshelf ring built 48 / 8 / 48 / 80 positions; it must be 32"* |
| `BookshelfRing`: shift the `dx` window by one | 2 red — *"Offset[dx=3, dy=0, dz=-2] is outside the 5x5 footprint"*. **The count stays at exactly 32**, so the self-check and every size assertion stay green |
| `EnchantCharge`: `UNLOCK` → free / `LEVEL_UP` → `currentLevel` / `ACTIVATE` priced | 3, 2 and 3 red |

### Three predictions that were wrong, kept rather than replaced

**1. A one-off in `XpCurve`'s CUMULATIVE band boundaries reddens nothing.** Predicted to fail at
L=16; it does not, in either direction. Vanilla's three parabolas intersect at *consecutive integer
pairs* — 15 and 16, 30 and 31 — so a branch anywhere inside `{14,15,16}` or `{29,30,31}` computes the
identical function. Measured by moving all four boundaries: 13, 17, 28 and 32 each redden, 14, 15, 29
and 30 do not. An off-by-one there is not a bug that hides; it is not a bug. The BAR bands intersect
at a single point each and a one-off there reddens five tests, which is the asymmetry worth knowing.

**2. Rewriting a curve band with `2.5`/`40.5` doubles reddens nothing either.** Predicted to lose the
cancelling half at odd levels. All 11 tests stayed green: both constants are exact binary fractions
and these magnitudes sit far inside a double's mantissa, so nothing rounds. The integer form is a
no-doubles-in-this-class choice, **not** a correctness fix, and the javadoc now says so.

**3. The double discount was predicted to break at II (636).** It does not — `1 - 30/100.0` is the
same double as `0.7`, and `910 × 0.7` lands within half a ULP of 637 so the multiply rounds back up,
while `2920 × 0.7` does not. **The reviewer caught this before it was run.** Recorded because it is
the same failure twice over: a plausible story about floating point, asserted without executing it.
All three wrong predictions were about floating point and all three were written before running
anything.

The consequence is a real design constraint rather than an embarrassment: a double reimplementation
is wrong in **exactly one of nine** shipped price cells, and *which* one depends on the double form.
`(int)` breaks only at power 30; `Math.round` breaks only away from it. So `EnchantCostTest` asserts
the whole grid, and III@30 = 2044 is named on its own assertion.

Two mutation-tooling failures are also worth recording, both caught by the marker `grep` rather than
by a green run: a `sed` addressed at a line number that was no longer the target line, and a `perl`
pattern whose unescaped parentheses were read as capture groups. A third bounced off `s|...|` when the
replacement contained `||`. All three "succeeded" silently and would have read as mutations that
reddened nothing.

## Boot gate — OWED IN FULL BY A HUMAN

Every row needs a `Player`, an XP bar and a placed block. A console log can only prove the plugin
loaded.

Stop any previous server first — a live one holds the jar lock, which is the incident `CLAUDE.md`'s
verification section opens with. Then `./scripts/dev-server.sh --refresh-content` (a **shell flag**
on that script, not a command; the in-game re-mint is `/rpg refresh`).

**Setup.** Flat ground, one enchanting table with clear space around it and two blocks above. The
wallet instrument is `/xp set @s N levels`.

**SCREENSHOT THE LEVEL NUMBER AND THE BAR BEFORE AND AFTER EVERY ROW THAT CLICKS.** A charge of 0 and
a charge of 352 look identical in a picture of the *after* state. Note that `/xp query` reports the
bar portion, **not** the banked total — which is why rows 4–6 are built to land on exactly **level 0
with an empty bar**, a state that needs no arithmetic to read.

| # | action | expect | proves |
|---|---|---|---|
| 1 | boot | `Loaded ... 4 enchants`, no `Skipping malformed enchant` | the roster the cells describe |
| 2 | bare table, no shelves anywhere: right-click | slot 8 reads **Bookshelf Power 0/30** and **"0% off unlocks and level-ups."** — **never "Not implemented yet"** | the placeholder is gone; the readout is a measurement |
| 3 | `/xp set @s 16 levels`, `/rpg give ironblade`, insert it, hover a **locked** candidate | **"Locked. Click to unlock at I -- 352 XP."** | the full price on the cell where the decision is |
| 4 | click it | unlocks at I and glints; chat names **352 XP**; **level 0, bar completely empty** | 352 is exactly a level-16 bank — the derivation, visible in game |
| 5 | `/xp set @s 25 levels`, hover (**"-- 910 XP"**), click | **II**; **level 0, bar empty** | the price is of the level being *bought*, not the one held |
| 6 | `/xp set @s 40 levels`, hover (**"-- 2920 XP"**), click | **III**; **level 0, bar empty** | the third rung |
| 7 | `/xp set @s 15 levels`, fresh `ironblade`, click a locked candidate | refused: **"Sharpness costs 352 XP; you have 315."** Level **still 15**. Weapon **unchanged** — still locked, still one item | the clean refusal, and **315** proves the wallet is points, not a level count |
| 8 | with the weapon at III from row 6 and level 0, click it again | *"Sharpness is already at its maximum."*; still level 0; the cell shows **no price line** | `AT_MAX` is free and priced at nothing |
| 9 | `/xp set @s 40`; unlock the **other** candidate in that column, then click back and forth between the two **five times** | the glint swaps each time, Sharpness still reads **III**, and **the level and bar do not move once** after that second unlock | **ACTIVATE STAYS FREE** — the level-you-paid-for property |
| 10 | `/xp set @s 16 levels` then `/xp add @s -1 points` (wallet 351), click a locked candidate | refused: **"costs 352 XP; you have 351."** | the check reads the whole wallet **including the bar**; one point short is short |
| 11 | `/xp add @s 1 points` (wallet 352), click again | it unlocks; **level 0, bar empty** | exactly affordable is affordable |
| 12 | `/xp set @s 46 levels` (4267), take one candidate from locked to **III** in three clicks, no shelves | total spent **4182**, ending at **level 6 with the bar about two-thirds full (13/19)** | the whole points model end to end — a level model cannot land here |
| 13 | build a **full 5×5 outer ring** at table-Y and Y+1 (32 shelf positions), reopen | **30/30**, **"30% off"**; cells read **246 / 637 / 2044** | **2044 is exactly 70% of 2920.** The level model would have charged `totalForLevel(28)` = **1186**, i.e. 59% off. **THE ROW THIS PASS WAS REWRITTEN FOR** |
| 14 | full ring, `/xp set @s 35 levels` (2045), weapon at II, click to III | it succeeds, costing **2044**, leaving **level 0 with 1 point** in the bar | the discount applied in points, to the point |
| 15 | same, but `/xp set @s 34 levels` (1897) | refused: **"costs 2044 XP; you have 1897."** | the boundary from below |
| 16 | reduce the ring to **exactly 1** shelf, reopen | **1/30**, **"1% off"**, and II reads **900** — *not* 901 | the discount **floors**; rounding to nearest would take a point from the player |
| 17 | reduce to **exactly 10** shelves, reopen | **10/30**; cells read **316 / 819 / 2628** | the per-shelf percentage across all three rungs |
| 18 | fill the **inner 3×3** only (the 8 cells touching the table, both layers), nothing in the ring, reopen | **0/30**; cells back to **352 / 910 / 2920** | the inner-ring skip |
| 19 | shelves at **Y−1** only, reopen; then at **Y+2** only, reopen | **0/30** both times | the layer set is exactly {0, +1} |
| 20 | shelves at **dx = ±3** only, reopen | **0/30** | the 5×5 bound |
| 21 | open at **0** shelves, build the full ring **without closing the menu**, then click a locked candidate | slot 8 **still reads 0/30**, the cell still says **352 XP**, and the full **352** is charged | the power is **frozen at open**, as designed |
| 22 | close, reopen | **30/30**, and the next click charges the discounted price | reopening is the re-count |
| 23 | build a **second** table elsewhere with a different shelf count; open each | each reads **its own** number | the count comes from the clicked block, not a global or the last-opened one |
| 24 | with `/xp set @s 0`, click a locked candidate; then take the weapon out and `/rpg enchant show` | refused, and the blob is **identical** to before the click | the refusal never reached the write |
| 25 | on a held weapon with `/xp set @s 0`, run the dev path (`/rpg enchant candidate ...`, then a level change) | it works; **no charge**; still level 0 | the economy gates the TABLE, not the dev command |
| 26 | in **Creative**, unlock one | **still charged** | no creative exemption |
| 27 | click a **filler** pane in a column that rolled fewer than 3 candidates | nothing happens, no charge, no price line on it | `EMPTY` is priced at nothing |
| 28 | spend down to a low level, then `/kill`, respawn | the spend is **not refunded**; level and bar are what they were | `setKeepLevel` makes the wallet durable in both directions |

Rows **13** and **12** are the pass's whole point. Row 13 is the discount being 30% in the units
actually charged; row 12 is the points model surviving three sequential purchases and landing
somewhere a level model could not.

Rows 9, 7 and 21 **carry information** rather than confirming a change: 9 is the property the whole
candidate model exists for, 7 is the only evidence that the untestable check-before / deduct-after
ordering is the right way round, and 21 is a design choice a player could otherwise report as a bug.

## GATE RESULT — RUN AND PASSED, 2026-08-27

**26 rows witnessed live and passed** (1–20, 23–28). The runner's witnessed result, not a pasted
transcript. The headline is **row 13**: a full ring charged **2044** for III, exactly 70% of 2920,
where the discarded level model would have charged `totalForLevel(28)` = **1186** — 59.4% off wearing
a 30% label. That row is the whole reason this pass was rewritten, and it is now an observation
rather than an argument.

**Rows 21 and 22 were NOT witnessed and are struck from the witnessed count.** They are not
reachable single-player: you cannot place a block while a GUI is open, so one player cannot build a
ring without first closing the menu the row is about. They are discharged as **structurally
guaranteed**, which is deliberately not the same claim as "passed":

| | what makes it hold | checked by |
|---|---|---|
| 21, frozen at open | `bookshelfPower` is a `private final int` assigned **exactly once**, from the only call to `BookshelfPower.at` in the codebase; every other mention is a read | the compiler enforces it — row 21 cannot fail without a compile error |
| 21, no re-scan possible | **no `Block` field exists**; `Block` appears in `EnchantMenu` only as a constructor parameter | `grep` for `Block ` in the class returns the parameter and nothing else |
| 22, re-count on reopen | `RpgListeners:185` constructs a **new** `EnchantMenu` per right-click, so reopening runs the constructor again | one call site, grepped |

That argument does **not** cover whether the scan reads the world correctly — but rows 13 and 18–20
witnessed exactly that and passed, so the only unobserved surface left is the freeze itself, which is
what the `final` field settles. **Both rows stay witnessable with a second player** (one places
shelves while the other holds the menu open) and are worth running if one is ever to hand. They are
not written off as unreachable for ever, and they are not counted as passed.

Rows 16 and 10 are the two rows that would still pass if the wallet were counted in levels — they are
what make "points" observable rather than merely asserted. Rows 18–20 are the only proof
`BookshelfPower.at` reads the right 32 places.

**Two things no unit test can reach**, so the gate is the only evidence that will ever exist for
them: the **seam ordering** (moving the deduction above the transition leaves all 643 tests green —
rows 7, 10 and 24), and the **world read** itself (rows 13, 18–20).

## Scope guard

If these surface mid-build, decline — each is its own later refinement: a confirmation dialog before
a spend; a sound cue; the vanilla air-gap rule; per-enchant pricing in YAML; affordability colouring
on the cells; a second currency; refunds or disenchanting; using `XpCurve` for anything outside this
table.
