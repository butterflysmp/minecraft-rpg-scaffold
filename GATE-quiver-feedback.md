# GATE — quiver reload feedback: action bar + hotbar sweep

**Status: NOT RUN AGAINST THE LIVE TIP.** Every prediction was written **before** any boot. Readings
go **beside** a prediction, never over it, and a prediction is not edited once its row has been read.

**Branch:** `feat/quiver-feedback`, off `3ae97ff`. **LIVE TIP: `4284936`.**
**Declared game mode: SURVIVAL — and here it is load-bearing, not a formality.**

> ### *** EVERY READING NAMES THE BUILD IT WAS TAKEN ON, AND READINGS DO NOT CARRY ACROSS A TIP ***
>
> Ben booted **`91b74c7`** and read rows from the `A`, `S`, `C` and `D` blocks. **`4284936` then
> changed what three of those blocks are about** — `PlumeNotice` moved to the action bar (the `A`
> block's surface), `QuiverReloadCue` began settling (the `C` block's whole subject), and
> `PlumeDraw.cap` began settling before it reads.
>
> **SO THOSE READINGS ARE HISTORY AGAINST `91b74c7`, NOT THE STATE OF THIS GATE.** A reading carried
> forward unremarked is a reading bound to the wrong subject — the same failure as a row read off the
> wrong jar, arriving through time instead of through a worktree. **The gate is re-read on `4284936`.**
>
> **Practically: a reading written into this file carries the build it was taken on, in the cell.**
> `PASS (91b74c7)` and `PASS (4284936)` are different facts, and only the second is this gate passing.
> A cell reading bare `PASS` is a cell nobody can bind, and it is not accepted here.
>
> **THE ONE ROW WHOSE `91b74c7` READING IS ITS REAL AND ONLY READING IS `P0`**, because P0's subject
> IS the old build. It is recorded below, bound, and it cannot be re-run after this branch merges.

> ### *** CREATIVE CANNOT READ THIS FILE AT ALL ***
>
> The creative-divergence register's first row: `hasInfiniteMaterials()` short-circuits
> `BowItem.use`'s ammunition check, so **the Plume's arrow is never consumed and
> `QuiverAmmo.consume` finds no draws to take**. A reload in creative therefore costs nothing and
> `noAmmo` cannot be produced.
>
> **Every row below is about a reload's edges**, so a creative boot would stage the condition
> without the cost and pass whether the code worked or not. **That is the hollow-fixture shape with
> creative as the mechanism.** Survival, with a real magazine and a real arrow stack.

---

## R0 — THE DEPLOYED BUILD CARRIES THIS SLICE. If R0 fails, STOP.

**PowerShell**, because that is the shell these get run in — no `unzip`, no `javap`.

| # | prediction | instrument | READING |
|---|---|---|---|
| **R0a** | the build line names this branch's tip | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | _(not run)_ |
| **R0b** | **6** action-bar sends and **0** chat sends across the **two** quiver notice classes — `QuiverNotice` **4**, `PlumeNotice` **2** | `$j = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'` then a class scan — see below | _(not run)_ |
| **R0d** | `PlumeDraw` and `QuiverReloadCue` are both in the jar and both reference `settleMaturedReload` | the constant-pool scan below | _(not run)_ |

> **R0b READ "4 … in the deployed notice class" UNTIL ITEM 4 LANDED, AND THE SINGULAR WAS THE DEFECT
> IT NOW MEASURES.** There were always **two** notice classes; the row named one, so a `PlumeNotice`
> still sending to chat satisfied it. The prediction is corrected rather than annotated because **this
> row has not been read** — no reading has been written beside it, so nothing is being overwritten.
| **R0c** | no boot error mentioning `use_cooldown`, `quiver_reload_` or `QuiverSweep` | `Select-String -Path run\logs\latest.log -Pattern 'use_cooldown\|quiver_reload_\|QuiverSweep'` | _(not run)_ |

**R0b's class scan, since a jar is a ZIP and `Select-String` on it returns nothing for everything:**

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
$e = $zip.GetEntry('io/github/butterflysmp/rpg/paper/weapon/QuiverSweep.class')
if ($e) { 'QuiverSweep: PRESENT' } else { 'QuiverSweep: ABSENT -- STOP' }
$zip.Dispose()
```

**A present `QuiverSweep.class` plus the expected build hash is the binding.** Neither alone is: the
hash says which commit built the jar, the class says the symbol is in it, and a checkout of the wrong
branch followed by a correct build satisfies neither.

**R0d's constant-pool scan, and it needs one, because a CLASS being present says nothing about which
VERSION of it is present.** `PlumeDraw.class` has been in every jar since #78; what R0d has to see is
the *call added by this slice*. A deflated entry has to be inflated before any needle can match it —
`Select-String` on the jar returns nothing for everything:

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($n in 'PlumeDraw','QuiverReloadCue') {
  $e = $zip.GetEntry("io/github/butterflysmp/rpg/paper/weapon/$n.class")
  if (-not $e) { "$n : ABSENT -- STOP"; continue }
  $r = New-Object IO.StreamReader($e.Open())
  $t = $r.ReadToEnd(); $r.Close()
  if ($t -match 'settleMaturedReload') { "$n : settleMaturedReload PRESENT" }
  else { "$n : settleMaturedReload ABSENT -- the fix is NOT in this jar. STOP." }
}
$zip.Dispose()
```

> **THE SCAN'S OWN POSITIVE CONTROL IS THE PAIR.** Both names must print PRESENT. **One PRESENT and
> one ABSENT is the informative failure** — it means the jar predates one of the two halves, which is
> exactly the state a partial rebuild produces. A scan that could only ever report on one class would
> read as a pass with half the slice missing.

---

## THE ROWS — ITEM 1, THE ACTION BAR

| # | what to do | PREDICTION | READING |
|---|---|---|---|
| **A1** | Empty the magazine, then press fire. | The refusal appears **on the action bar**, above the hotbar, reading *"Your quiver is empty -- left-click to reload."* **Nothing in chat.** The dispenser-fail click still plays. | _(not run)_ |
| **A2** | Hold fire down for five seconds on an empty magazine. | The line **holds still**. It is rewritten at most once per 40 ticks, so it does not flicker, and chat stays empty throughout. | _(not run)_ |
| **A3** | Left-click to reload with a full magazine of arrows in the inventory. | *"Reloading..."* on the action bar, with the crossbow loading-start sound. | _(not run)_ |
| **A4** | Press fire during the reload. | *"Reloading -- 1.2s"* (or whatever remains) on the action bar. **No sound** on this one. | _(not run)_ |
| **A5** | Drop every arrow, empty the magazine, left-click. | *"You have no arrows -- plain Arrows load a quiver."* **Survival only** — creative cannot produce this row at all. | _(not run)_ |

---

## THE ROWS — ITEM 2, THE HOTBAR SWEEP

| # | what to do | PREDICTION | READING |
|---|---|---|---|
| **S1** | Left-click to reload. Watch the Plume's hotbar slot. | The vanilla cooldown **sweep** wipes across the icon and empties over exactly the reload's length (60 ticks authored, modified by any reload-time bonus). | _(not run)_ |
| **S2** | During that sweep, hold **right-click**. | **The bow does not start drawing at all** — no charge, no animation, no arrow. This is vanilla: `useItem` returns PASS before `ItemStack.use`. | _(not run)_ |
| **S3** | During the sweep, press fire once and watch the action bar. | *"Reloading -- Xs"* **still appears.** The interact event fires before the cooldown is consulted, which is what makes items 1 and 2 compose. | _(not run)_ |
| **S4** | Put a **second bow** (`hunters_bow`, or a vanilla bow) in the hotbar and reload the Plume. | **Only the Plume sweeps.** The other bow is untouched and still usable. This is the whole point of the private cooldown group. | _(not run)_ |
| **S5** | Reload, then switch to another hotbar slot mid-reload, then switch back. | The sweep is **gone while away and correct when you return** — re-derived from the item, not remembered. | _(not run)_ |
| **S6** | Reload, then **log out** mid-reload and back in. | The sweep is **back and showing the right remainder**, and the reload still matures. A vanilla cooldown does not survive a logout; the deadline is on the item. | _(not run)_ |
| **S7** | Reload, then **die** mid-reload, then respawn holding the weapon. | Same as S6. | _(not run)_ |
| **S8** | Let a reload mature while you watch the slot. | The sweep ends **as the magazine refills**, not before and not after. | _(not run)_ |

**S4 is the row the whole mechanism exists for**, and it is the one that fails loudly if the
`use_cooldown` group did not land: without a group the cooldown keys on `minecraft:bow` and **every
bow sweeps and every bow is blocked.**

**S2 IS RULED INTENDED -- Ben, 2026-09-24, recorded as his.** A reloading weapon that cannot be drawn
is the design, not a side effect to be tolerated. The prediction stands as written and is now the
**expected** reading rather than an open question: a boot that shows the bow refusing to draw mid-reload
is S2 **passing**.

> **It is still worth a row rather than a sentence.** The behaviour comes from vanilla's own
> `useItem` short-circuit, so it would arrive whether or not anyone wanted it -- and a ruled behaviour
> with no reading is indistinguishable from an unnoticed one. **The row is what makes the ruling
> checkable.**

---

## THE ROW THAT PROVES THE DEFECT FIX — AND IT IS NOT ABOUT FEEDBACK AT ALL

| # | what to do | PREDICTION | READING |
|---|---|---|---|
| **D1** | Note your arrow count. Left-click to reload the Plume. **Log out before it matures.** Log back in and wait for maturity. | The magazine **fills with the rounds you paid for.** **Before this slice it filled with NOTHING** and the arrows were gone: the deadline was carried across the join's re-mint and the pending count was not. | _(not run)_ |

> **This is the found defect, and D1 is its only end-to-end witness.** The unit row
> (`QuiverReloadCarrySignatureTest`) checks the RULE — every stamped reload key is carried — because
> `carry` takes two `ItemMeta` and cannot be exercised without a server. **The rule row is what stops
> a fourth key repeating it; D1 is what proves this one is actually fixed.**

---

## ITEM 3 — THE RELOAD-COMPLETE CUE. **RULED AND BUILT.**

> **Ben's ruling, 2026-09-24, recorded as his: option (1), one scheduled task per reload, sound
> `item.crossbow.loading_end`.** The three options and their costs are kept below as the record of
> what was chosen against, not as an open question.

**Why a task was needed at all, and it is the read that forced the question to be put:**

**THE RELOAD IS LAZY. NOTHING RUNS AT MATURITY.** `Quivers`' own javadoc: *"No task is queued when a
reload starts. `resolveForShot` asks the item whether its deadline has passed and completes it in
place if so. That is what makes the reload leak-proof."* The two PDC stamps are a deadline, not a
timer — **so there is no moment at which a sound could be played today.** A cue needs something new.

**THE THREE OPTIONS, CHEAPEST FIRST:**

| | mechanism | what it costs | what can go wrong |
|---|---|---|---|
| **(1)** | **One scheduled task per reload**, at `reload_ticks`, that plays the sound if the held item is still the same reloading weapon | one task per reload; **gives up the "no task" property the design is proud of** | the item moved, was swapped, dropped, or the player logged out — every one of them must be checked at fire time, and a task cannot be cancelled by an item moving |
| **(2)** | **Play it at the next read that finds the reload matured** — inside `finishReload`, which already exists and is already called lazily | **nothing.** No new scheduling, no new state | **it is not a cue at the right TIME.** `finishReload` runs when the player next fires, so the sound lands on the shot, possibly seconds late. It is a *"your reload was ready"* noise, not a *"ready now"* one |
| **(3)** | **One shared repeating task** that walks players with a live reload and fires cues as deadlines pass | one task total, not one per reload; needs a registry of who is reloading | that registry is exactly the per-player state the item-stamp design removed on purpose |

**MY RECOMMENDATION IS (1), and the reason is that the cue's whole value is its TIMING.** Option (2)
is free and answers a different question; option (3) rebuilds the state the design deleted.

**THE HAZARDS OF (1), NAMED, BECAUSE THEY ARE WHY THIS IS A RULING AND NOT A CHORE:**

- **The item moved or was swapped.** The task must re-resolve the held item and check it is the same
  weapon *with the same deadline*, not merely that a reload exists. Cheap: compare the deadline it was
  scheduled for against the deadline the item now carries.
- **Two quivers.** A player with two Plumes can have two live reloads and two tasks. The deadline
  comparison handles it: each task only fires for the item carrying its own deadline.
- **Logout.** A Folia-safe task on a departed player must no-op rather than throw; the scheduler's
  `onEntity` already gives that shape.
- **The reload matured EARLY.** `finishReload` is also reached by the shot path, so the task can fire
  after the reload has already been settled. The deadline comparison catches that too — the stamps are
  gone, so there is nothing to match.
- **A dropped weapon.** The reload is on the item and travels with it; the *player* gets no cue,
  which is correct — they are not holding it.

**The sound is vanilla's `item.crossbow.loading_end`**, the natural partner to the
`item.crossbow.loading_start` that `reloadStarted` already plays.

**Item 3 is now in this branch**, built on the same tip so that one boot reads all three items. Its
rows are the `C` block below.

---

## THE ROWS — ITEM 3, THE RELOAD-COMPLETE CUE

**Predictions written before any boot, as with every row above.** The cue is
`item.crossbow.loading_end`, booked for the reload's post-modifier length, and it sounds only if the
held item still carries the very deadline the task was made for.

| # | what to do | PREDICTION | READING |
|---|---|---|---|
| **C1** | Reload and wait, holding the weapon, watching the hotbar. | **One** `loading_end` click, **as the sweep empties** -- not before it, not a beat after. The magazine reads full at the same moment. | _(not run)_ |
| **C2** | Reload, then switch to another hotbar slot and stay there past maturity. | **No sound at all.** The held item is not the weapon the task was made for. | _(not run)_ |
| **C3** | Reload one Plume, switch to a **second** Plume and reload it too, then hold the second and wait. | **Two sounds, each at its own time** -- and each one is only heard if that weapon is the one in hand when its own deadline passes. So expect the second Plume's cue for certain and the first one's only if you are holding it then. | _(not run)_ |
| **C4** | Reload, then **fire at the exact moment of maturity**. | **One sound at most, never two.** If the shot settles the reload first the cue is silent -- the stamps are gone, so there is nothing for it to match. | _(not run)_ |
| **C5** | Reload, then **log out** before maturity. Read the log. | **No error, no stack trace**, nothing mentioning `QuiverReloadCue`. The entity task is dropped rather than run against a departed player. | _(not run)_ |

> ### *** C3 IS THE ROW WHOSE PREDICTION IS NOT "TWO SOUNDS", AND THE DIFFERENCE IS THE DESIGN ***
>
> A cue sounds **for the weapon in your hand**, because that is the only weapon whose finish the
> player can see. Two staggered reloads therefore produce two sounds **only if the player is holding
> the right one at each deadline** -- which is what makes the row worth running rather than assuming.
>
> **The alternative would be worse and was rejected by the ruling:** a cue that sounded for any
> reloading Plume would tell the player a magazine is ready while they hold an empty one.

> ### *** C4's ONE-TICK CORNER IS DECLARED, NOT DISCOVERED ***
>
> If the shot runs before the task in the maturity tick, **the cue is silent and that is correct** --
> the player got the magazine and the shot. The window is exactly one tick wide. It is written here
> because a silent C4 would otherwise read as a failure of C1.

> ### *** WHAT NO BOOT ROW CAN SEE, AND WHERE IT IS GUARDED INSTEAD ***
>
> The deadline comparison is the whole safety of the task, and a boot cannot show it working -- a
> passing C1 looks identical with the comparison in place and with it replaced by `true`. **Measured:
> that exact mutation left all 2164 tests green** before `QuiverCueTest` existed, and reddens exactly
> one row now. **C2, C3 and C4 are the behavioural witnesses; `QuiverCueTest` is the guard.** Neither
> substitutes for the other.

> ### *** C1's SECOND SENTENCE WAS A PREDICTION THE CODE COULD NOT HAVE MET, AND IT IS LEFT EXACTLY AS WRITTEN ***
>
> *"The magazine reads full at the same moment."* Written with the cue, before any boot — and **on
> `91b74c7` it was false.** The cue played a sound and settled nothing, so the counter moved on the
> player's next action, which is precisely what Ben reported.
>
> **The prediction is not edited, because it was RIGHT and the code was WRONG.** Item 3's second half
> (`QuiverReloadCue` now calls `Quivers.settleMaturedReload` behind the same deadline check) is what
> makes it true. **A prediction that a boot would have falsified is the most valuable thing a gate
> sheet can contain, and rewriting it to match the shipped behaviour would have destroyed the one
> record that the sheet caught something.**

---

## THE ROWS — THE #147 DEFECT: A DECIDING READER ON A STALE MAGAZINE

**Reported by Ben on #147's boot:** *"Arrows are taken, the sweep appears. When it finishes the quiver
still says 0, the bow won't fire, until left-click, then it fires without a second reload."*

**THE CAUSE, CONFIRMED BY READING EVERY LINK BEFORE ANY FIX WAS WRITTEN.** `PlumeDraw.cap` read
`Quivers.stateOf(...).roundsRemaining()` — the LOADED count — and handed it to `DrawRelease.decide`.
A matured-but-unsettled reload's rounds live in `quiver_reload_pending`, so the cap was `0`, the
release was `Nothing(NO_ROUNDS)`, and `WeaponFire.attemptFan` was never reached — **so nothing settled
the reload either.** The refusal is what kept it broken.

**On master since `e9e657a` (#78, slice H1) — `git log -S "roundsRemaining()" -- PlumeDraw.java`
returns that one commit, and `git merge-base --is-ancestor e9e657a master` confirms it. #147 did not
introduce it.**

| # | what to do | PREDICTION | READING |
|---|---|---|---|
| **P0** | **ON `91b74c7`, THE BUILD BEN BOOTED — the repro.** Reload the Plume with arrows in the inventory. Wait until the sweep has fully emptied. **Do NOT left-click.** Draw and release. | **The "held for nothing" refusal, EVERY TIME** — *"The draw was held for nothing -- your quiver is empty."* The quiver still reads `0/25`. Then left-click once: it reloads **instantly, with no second wait**, and now fires. | **REPRODUCED, per Ben, on `91b74c7`.** Bound below. No figures — the reading is a described behaviour, not a measurement. |
| **P1** | **ON THIS BRANCH'S TIP — the same gesture, exactly.** | **It fires.** The magazine reads its full rounds before the draw starts, the charge ticks climb past step 1, and the release fans its arrows. **No refusal, and no left-click needed.** | _(not run)_ |
| **P2** | Reload, hold the weapon, and **watch the counter as the `loading_end` click plays**. | The counter reads **full ON the click**, not on the next action. This is C1's second sentence, now met. | _(not run)_ |
| **P3** | Reload. **Start drawing BEFORE the reload matures** — S2 says the sweep blocks the draw, so begin the moment it clears — and hold through maturity if you can arrange the overlap. | **The draw is not interrupted.** No animation reset, no re-pull, and the charge ticks keep climbing. If the cap rises mid-draw, **the ticks resume up the ladder** rather than stopping. | _(not run)_ |
| **P4** | Reload to full, then draw and **count the charge clicks**. | **Three clicks**, rising in pitch — the full ladder, because the cap is the settled magazine. Before the fix a matured-unsettled reload capped it at **zero clicks**. | _(not run)_ |

> ### *** P0 IS ON THE OLD BUILD AND IT IS THE ONLY ROW HERE THAT CANNOT BE RUN AFTER MERGING ***
>
> It is written as a row rather than as a paragraph because **the cause was established by READING and
> not by measurement**, and this sheet's standing rule is that a verdict must not stand in for a
> reading.
>
> **If P0 did NOT reproduce**, the diagnosis is wrong and P1 proves nothing: a green P1 on a defect
> that never presented is the hollow-fixture shape with the *defect* as the missing condition.
>
> ### *** P0'S READING IS BOUND TO A SESSION BEFORE IT IS RECORDED, BECAUSE A REPORT IS NOT A BINDING ***
>
> **The reading is Ben's, and Ben's report names a behaviour, not a build.** *"Arrows are taken, the
> sweep appears. When it finishes the quiver still says 0, the bow won't fire, until left-click, then
> it fires without a second reload."* That sentence is true of some boot; **which boot is a separate
> fact, and it is the one R0 exists to establish.** Recording the report without it would be a true
> reading bound to no subject — the failure R0's own account calls *"a true reading bound to the wrong
> subject"*, which no amount of distrusting the answer catches.
>
> **THE BINDING, and the instrument is the build line `#145` added for exactly this:**
>
> ```
> $ grep -c "\[Rpg\] Build:" run/logs/latest.log
> 1
> $ grep -m1 "\[Rpg\] Build:" run/logs/latest.log
> [00:34:39] [Server thread/INFO]: [Rpg] Build: 91b74c7
> [00:34:55] [User Authenticator #0/INFO]: UUID of player BaronVonYeetus is b6ae27e9-...
> [00:34:56] [Server thread/INFO]: BaronVonYeetus joined the game
> [00:42:00] ... left ... [00:42:01] ... rejoined ...
> [00:48:31] [Server thread/INFO]: BaronVonYeetus left the game
> ```
>
> **Four things make it a binding rather than a coincidence:**
>
> - **`91b74c7` is the tip P0 is written against**, and it is the branch tip the fix was built on.
> - **ONE boot in that file**, so the build line and the play session cannot belong to different runs.
>   A log with two `Build:` lines would bind nothing.
> - **A player was on for ~14 minutes of it** — joined `00:34:56`, left `00:48:31`, with a relog in the
>   middle. An empty session would carry the build and witness no behaviour.
> - **`91b74c7` appears in NO OTHER SESSION.** Swept every `run/logs/*.log.gz`: only three archives
>   carry a build line at all (`b487971`, `unknown -- NOT built by dev-server.sh`, `cf1f68e`), because
>   the line itself only landed at `3ae97ff`. **So there is exactly one candidate session and no
>   ambiguity to resolve.**
>
> **WHAT THE LOG DOES NOT DO, SAID PLAINLY: it does not witness the behaviour.** Drawing a bow and
> reading an action bar are not logged, and nothing in that session's `[Rpg]` lines mentions the Plume
> — the `[plume]` warnings are the two that fire only when something is wrong, and neither did. **The
> log binds the SESSION to the BUILD. Ben binds the BEHAVIOUR to the SESSION.** Both halves are needed
> and only one of them is mechanical; recording it as *"REPRODUCED, per Ben"* rather than
> *"REPRODUCED"* is what keeps that visible.

> ### *** P3 IS THE ROW FOR A HAZARD THE JAR ALREADY HALF-ANSWERED, AND ONLY THE CLIENT HALF IS OPEN ***
>
> Both new settle sites can fire **while the player is drawing a bow**, and settling REWRITES the main
> hand. Read off `run/versions/26.1.2/paper-26.1.2.jar` with `javap -c`:
>
> ```
> LivingEntity.updatingUsingItem()
>    19:  ItemStack.isSameItem(getItemInHand(usedHand), useItem)
>    22:  ifeq 48            <- NOT the same item -> 48
>    34:  putfield useItem   <- same item: RE-POINT at the new stack
>    49:  stopUsingItem()    <- 48: THE DRAW IS CANCELLED
>
> ItemStack.isSameItem(a, b)
>     2:  b.getItem()
>     5:  a.is(Object)       <- ITEM TYPE ONLY. No components, no NBT, no count.
> ```
>
> **A bow replaced by a bow passes, so the SERVER does not cancel the draw.** The charge is untouched
> too: `getTicksUsingItem` is `useItem.getUseDuration(this) - useItemRemaining`, and the re-point moves
> neither term.
>
> **WHAT THE JAR CANNOT ANSWER IS WHAT THE CLIENT DOES WITH A RE-SENT HOTBAR SLOT MID-DRAW**, because
> the pull animation is the client's own. **That is the whole content of P3**, and it is a COSMETIC
> question — a flicker would be ugly and would not be a correctness failure. Said here so a flicker is
> not read as the settle having broken something.
>
> **Had `isSameItem` compared components, every quiver write would have been a draw-cancel and this
> whole shape would have been wrong.** The reading is load-bearing, which is why it is bytecode rather
> than recollection.

---

## THE ROWS — ITEM 4, THE PLUME'S TWO NOTICES ON THE ACTION BAR

**Ben's action-bar pick, 2026-09-23, reaching `PlumeNotice` on 2026-09-24.** `QuiverNotice`'s four
moved in this branch's first commit; these two were left in chat, which is the split R0b's singular
wording could not see.

| # | what to do | PREDICTION | READING |
|---|---|---|---|
| **A6** | Empty the magazine, hold the draw past the floor, and release. | *"The draw was held for nothing -- your quiver is empty. Left-click to reload."* **on the action bar. Nothing in chat.** The dispenser-fail click still plays. | _(not run)_ |
| **A7** | Remove every plain arrow from your inventory **and your off-hand**, then right-click the Plume. | *"This bow needs a plain Arrow in your off-hand to draw -- a reload cannot take it."* **on the action bar. Nothing in chat.** | _(not run)_ |
| **A8** | Run the class javadoc's sequence in one breath: release on an empty magazine, left-click as told, then right-click to draw. | **Two different action-bar lines in sequence** — A6's, then A7's — with **neither swallowing the other**. They hold separate throttle keys, and this is the sequence those keys exist for. **Chat stays empty throughout.** | _(not run)_ |

> **A8 IS THE ROW THE SEPARATE KEYS WERE WRITTEN FOR, AND MOVING TO THE ACTION BAR MAKES IT HARDER TO
> READ RATHER THAN EASIER.** An action bar **overwrites**, so the first line is gone by the time the
> second arrives — you are checking that the second line APPEARS, not that both are visible at once.
> In chat both would have stacked and the row would have been trivial. **Read it as two events, not as
> two lines on screen.**

> ### *** WHAT NO ROW ABOVE CAN SEE, AND WHERE IT IS GUARDED INSTEAD ***
>
> **`MUT-DRAWSETTLE`** — delete `PlumeDraw.cap`'s `Quivers.settleMaturedReload` call; verified applied
> by a line delta of **1 removed / 0 added** against a pristine copy, and a comment-stripped needle
> going **1 → 0**. **It left ALL 2170 ROWS GREEN**, which is the measurement that says P0/P1 are the
> only behavioural witnesses this defect ever had. It now reddens exactly
> `QuiversSignatureTest.everyExternalReaderOfTheMagazineDeclaresWhetherItSettles`, and nothing else.
>
> **`MUT-PLUMECHAT`** — revert `PlumeNotice`'s two `sendActionBar` calls to `sendMessage`; **2 removed
> / 2 added**, marker present **2**, original gone **0**. Reddens exactly
> `NoticeSurfaceTest.theQuiverFamilySpeaksOnTheActionBarAndNeverInChat`.
>
> **THE SOURCE SCAN IS A PRESENCE CHECK AND NOT A BEHAVIOURAL WITNESS.** A settle call moved BELOW the
> read, or guarded behind a condition that is never true, satisfies it exactly as correct code does.
> **P1 is the row that can tell those apart, and nothing in the suite can.** Do not read a green suite
> as proof the call is in the right place.
