# GATE — quiver reload feedback: action bar + hotbar sweep

**Status: NOT RUN.** Every prediction was written **before** any boot. Readings go **beside** a
prediction, never over it, and a prediction is not edited once its row has been read.

**Branch:** `feat/quiver-feedback`, off `3ae97ff`.
**Declared game mode: SURVIVAL — and here it is load-bearing, not a formality.**

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
| **R0b** | **4** action-bar sends and **0** chat sends in the deployed notice class | `$j = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'` then a class scan — see below | _(not run)_ |
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
