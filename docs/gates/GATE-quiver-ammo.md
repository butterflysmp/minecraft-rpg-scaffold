# GATE — Slice E: arrows load quivers

## **Status: RUN 2026-09-13. All six rows PASS.**

**Every prediction below was written BEFORE the boot**, and before the plugin was built or deployed.
Verdicts read from the gate page's own store at `2026-09-13T22:09:43Z` — `b1`–`b6`, all PASS.

---

## ⚠ READ THIS BEFORE THE TABLE: WHAT THIS RECORD DOES **NOT** CONTAIN

**Two things about this record are weaker than six PASS rows look.** They are here, above the table,
because a reader must meet them before the verdicts rather than find them in a footnote.

### (i) NO OBSERVED FIGURES WERE CAPTURED. NOT ONE READING.

Every row below carries a bare verdict. **No magazine count, no arrow count, no message text was
written down.** So every observed cell reads **"PASS — figure not captured"**, never a number.

> **NO NUMBER IN AN OBSERVED CELL HAS BEEN RECONSTRUCTED, BY ANYONE.** A figure inferred after the
> fact from what the code *should* have produced is not an observation — it is the prediction wearing
> the observation's clothes, and it would make this table self-confirming.

**A PASS WITH NO READING IS A VERDICT, NOT A MEASUREMENT**, and this file holds verdicts.

**The cause, in one line so it is fixable rather than repeated:** the tick-through made the verdict
one click and the reading a typing job — **on a gate whose whole doctrine is that the observed column
is filled in beside the prediction.** That is a defect in the instrument, not in the operator.

### (ii) THE JAR-FRESHNESS CONTROL HAS NO ENTRY AT ALL

The setup section below requires confirming the running jar contains `QuiverAmmo` before believing any
row. **No such entry was recorded.**

**The honest reconciliation, both halves:**

- **The freshness conclusion still stands — but it stands on THE ROWS, not on the control.** A stale
  jar has no `QuiverAmmo`, so reloads would not consume at all: **row 1 would have read `8/8` with
  seven arrows still in the bag**, and **row 2's notice would not exist**. Those rows are
  self-discriminating.
- **That is LUCK IN HOW THE ROWS WERE WRITTEN, NOT EVIDENCE THAT THE CONTROL WAS TAKEN.** The control
  was specified, it was not performed, and nothing here should be read as saying it passed.

> **A control that was not run is not a control that passed, however sound the inference that replaces
> it.** The inference is recorded because it is true; the control is recorded as missing because it
> is.

---

> **TWO SHIPPED WEAPONS CHANGE BEHAVIOUR AND NEITHER WEAPON FILE IS TOUCHED.** `boltor`
> (`quiver_size: 8`) and `locust` (`12`) become arrow-dependent through the mechanic alone. A reader
> looking for the content change will not find one.

**Build and deploy first.** The deployed jar predates this slice — verified by listing its contents
rather than by its mtime, which a `touch` probe destroyed:

```
unzip -l run/plugins/rpg-0.1.0-SNAPSHOT.jar | grep -c expanded_quiver   ->  0
unzip -l run/plugins/rpg-0.1.0-SNAPSHOT.jar | grep -c power.yml         ->  1   (positive control)
```

So `./scripts/dev-server.sh` (build + deploy + boot), and **confirm the running jar contains
`QuiverAmmo`** before believing any row below. A stale jar reproduces the OLD behaviour perfectly,
and every row here would then read as a pass for the wrong reason.

---

## WHY THIS SLICE NEEDS A BOOT MORE THAN USUAL

**`MUT-PENDING` was RUN and reddened NOTHING across all 1612 rows.** Replacing the stored pending
count with the resolved capacity in `Quivers.finishReload` — **the exact defect this slice exists to
prevent** — is invisible to the entire suite, because that method needs a `Player` and an `ItemMeta`
and no unit test in this project has either.

> **ROW 1 IS THE ONLY THING IN THE PROJECT THAT CAN CATCH IT.** Not a nice-to-have: the arithmetic is
> guarded in core, the wiring is guarded by nothing, and this is the wiring.

| mutation | run? | result |
|---|---|---|
| `MUT-RELOAD` — `Quiver.reload` ignores the rounds paid for | **RUN** | **2 / 9 red** — the partial-load rows |
| `MUT-ORDER` — ammo rung above the full/not-full test | **RUN** | **1 / 9 red** — `aFULLMagazineWithNoArrowsIsSTILLAlreadyFull` |
| `MUT-AMMO` — ammo material widened to `SPECTRAL_ARROW` | **RUN** | **2 / 6 red** — including the instrument-collision row |
| **`MUT-PENDING`** — maturity refills to capacity, not to what was paid for | **RUN** | **0 of 1612. UNGUARDED.** → **ROW 1** |
| `MUT-DEBIT` — debit by re-finding stacks rather than by recorded slot | not run | needs a `Player` → **ROW 1 + ROW 3** |
| `MUT-TRANSITION` — `beginReload` returns true on a non-transition | not run | needs a `Player` → **ROW 5** |

---

## SETUP

```
/rpg give boltor            a Boltor mints FULL (8/8) -- the settled ruling
                            (mint also serves menu icons; minting empty would
                             make every preview read "Quiver: 0/8")
/gamemode survival          rows 1-3 and 5. Row 4 is the creative one.
```

**Clear the inventory of arrows between rows.** Several rows below turn on the player holding
*exactly* a stated number, and a forgotten stack from the previous row is the most likely way this
gate reports a pass it did not earn.

---

## ROW 1 — PARTIAL LOAD *(the row that stands in for `MUT-PENDING`)*

**Ruling 5.** Fire the Boltor dry (8 shots). Hold **exactly 7 arrows**. Reload.

| | prediction | observed |
|---|---|---|
| tooltip after the reload matures | **`Quiver: 7/8`** | PASS -- figure not captured |
| arrows left in inventory | **0** | PASS -- figure not captured |
| under `MUT-PENDING` this would read | `8/8` | *(counterfactual -- not an observation)* |

> **READ THE TOOLTIP *AND* THE INVENTORY. Either alone is a weaker row than it looks.**
> The tooltip alone cannot distinguish a correct partial load from a full load that failed to debit;
> the inventory alone cannot distinguish a correct debit from one that took the arrows and loaded
> nothing.

**7 and 8 are deliberately unequal.** Staging 8 arrows into an 8-round magazine passes whether the
`min(needed, available)` exists or not — the fixture would be measuring itself.

---

## ROW 2 — NO-AMMO REFUSAL

**Ruling 4.** Fire **one** shot (7/8). Hold **zero arrows**. Press reload.

| | prediction | observed |
|---|---|---|
| message | the **new** notice — *"You have no arrows -- plain Arrows load a quiver."* | PASS -- figure not captured |
| message is **NOT** | *"already full"*, nor *"Your quiver is empty -- left-click to reload."* | PASS -- figure not captured |
| tooltip | unchanged at **`7/8`** | PASS -- figure not captured |
| **`quiver_reload_started_at` on the item** | **ABSENT** | PASS -- figure not captured |
| **`quiver_reload_completes_at`** | **ABSENT** | PASS -- figure not captured |
| **`quiver_reload_pending`** | **ABSENT** | PASS -- figure not captured |

> **THE THREE PDC ROWS ARE THE HALF MOST LIKELY TO BE DROPPED, AND THEY ARE THE HALF THAT MATTERS.**
> The message distinguishes the refusal from the wrong refusal. **Only the absent stamps distinguish
> *refused* from *started, and instantly finished with nothing to load.*** Both look identical on
> screen.

**Then press reload again immediately.** The notice is throttled at 40 ticks on its **own** key, so
the second press inside two seconds should be silent — and critically, **firing dry first and then
reloading must produce BOTH messages**, not one, because a shared throttle key would silence the
second.

---

## ROW 3 — THE INTERRUPT COST *(write this one hardest)*

**Ruling 3: arrows are consumed AT THE START.** Fire dry. Hold **exactly 5 arrows**. Press reload,
then **swap to another weapon before it matures** (`reload_ticks: 60` = 3 seconds — ample).

| | prediction | observed |
|---|---|---|
| arrows in inventory, immediately after the swap | **0 — GONE** | PASS -- figure not captured |
| Boltor's tooltip | **unchanged at `0/8`** | PASS -- figure not captured |
| after swapping back and waiting | still `0/8`; a new reload needs new arrows | PASS -- figure not captured |

> ### THIS IS DELIBERATE, IT IS UNUSUAL, AND IT IS INDISTINGUISHABLE FROM A BUG UNLESS THE RECORD
> ### SAYS IT WAS CHOSEN.
>
> **Ruling 3 is exactly this.** A player who interrupts a reload has spent the arrows and has nothing
> to show for it. That is the cost of the ruling, it is the row a player will complain about, and the
> prediction is written **before the boot** so that nobody is tempted to soften it after seeing it.
>
> **If this row is ever changed, it is a RULING that changed — not a bug that was fixed.**

**5 is not 8 and not 7**, so this row cannot be confused with row 1's staging, and "the arrows are
gone" cannot be read as "the magazine took them".

---

## ROW 4 — CREATIVE *(the only thing that will ever exercise that arm)*

**`/gamemode creative`.** Fire the Boltor dry. **Empty the inventory of arrows completely.** Reload.

| | prediction | observed |
|---|---|---|
| tooltip after maturity | **`8/8` — a FULL magazine** | PASS -- figure not captured |
| inventory | **UNCHANGED — no arrows added, none removed** | PASS -- figure not captured |
| no-ammo notice | **NOT shown** | PASS -- figure not captured |

> **THE EMPTY INVENTORY IS LOAD-BEARING.** With arrows present, a creative reload to full is
> **indistinguishable** from a survival one that debited correctly — the row would pass whether the
> creative arm existed or not. It must be run with **zero** arrows.
>
> **The second half — NO INVENTORY CHANGE — is the half that can fail on its own**, and it is what
> separates *skipped the check* from *debited something*. Read the inventory, not just the tooltip.

**And this is the ONLY route to that arm.** `GameMode` is a `Player` property, `core` cannot see it,
and no unit test holds a `Player`. The paper unit test pins only that four modes exist.

> **`ADVENTURE` and `SPECTATOR` are DERIVED, not ruled** — both consume. Not gated here; recorded so
> a future row knows they were never observed.

---

## ROW 5 — THE HELD-INPUT GUARD *(a standing property this slice could break)*

**Hold left-click** on a Boltor that wants a reload, with **exactly 3 arrows**, for ~3 seconds.

| | prediction | observed |
|---|---|---|
| arrows consumed | **3, ONCE** | PASS -- figure not captured |
| arrows consumed if `beginReload` stopped being a real-transition gate | ~60, twenty times a second | *(counterfactual -- not an observation)* |
| the reload completes | **yes** — the deadline is not pushed back by held input | PASS -- figure not captured |

> `beginReload` *"returns true only on a REAL TRANSITION"*, and Slice E put an **inventory debit**
> behind that property. It was safe before because the only cost was a PDC write; it now costs the
> player real items, roughly twenty times a second if the property is lost.
>
> **Nothing in the suite protects this.** The debit sits past every refusal arm for exactly this
> reason, and this row is what says so.

---

## ROW 6 — THE SPECTRAL INSTRUMENT IS NOT AMMO

**`/rpg give quiver_size_boost`** (a **SPECTRAL** arrow) and hold it with **zero plain arrows**.
Fire dry, reload.

| | prediction | observed |
|---|---|---|
| reload | **REFUSED** — the no-ammo notice | PASS -- figure not captured |
| the instrument | **still in the inventory, untouched** | PASS -- figure not captured |

> A family match would consume the item that ENLARGES the magazine, to fill the magazine it
> enlarged. `QuiverAmmoTest` guards the constants; **this row guards the walk**, which no unit test
> reaches.

> **THIS ROW DOES NOT REACH THE GEAR GUARD, AND THAT IS WORTH SAYING RATHER THAN LEAVING.** A spectral
> arrow is excluded by the **material** check before `isGear` is ever consulted, so this row would pass
> identically with the gear guard deleted.
>
> **The gear guard is implemented and is UNREACHABLE TODAY**: `sources()` calls
> `CraftMatrixScreen.isGear`, the same call `RecipeProbe.groups` makes, and **no minted item is
> `Material.ARROW`**, so nothing in shipped content can trip it. It exists because it must already be
> there on the day one is — the alternative is a player's minted item being eaten as ammunition with
> nothing reporting it.
>
> **Nothing stages it, and nothing can until such an item exists.** Listed below rather than left for
> someone to assume this row covered it.

---

## WHAT THIS GATE DOES NOT COVER

- **`locust`** is not exercised. It shares every code path with the Boltor and differs only in
  authored numbers (12 rounds). Named so the omission is deliberate rather than assumed.
- **Over-full quivers** (`roundsNeeded` returning 0 for a count above capacity) — reachable only by
  removing a quiver-size modifier mid-reload. Core row covers the arithmetic; no boot row stages it.
- **A pre-Slice-E item** carrying the two old stamps and no pending key. `finishReload` adds zero
  rounds and clears the reload. Not staged: producing one requires a build from before this slice.
- **THE GEAR GUARD** — `sources()` skipping a minted item via `CraftMatrixScreen.isGear`. **Implemented
  and unreachable**: no minted item is `Material.ARROW`, so no content can trip it and row 6 does not
  reach it (a spectral arrow is refused on material first). **Its trigger is the day something is
  minted as a plain arrow**, and that is the day this row becomes stageable.
- **`consume()` re-reads each slot's TYPE but not its AMOUNT**, so a slot that SHRANK between the plan
  and the take is not skipped — `left` goes negative and the stack is cleared. **This is
  byte-for-byte `InventoryCraft.debit`**, the pattern this slice was told to follow, and the window is
  theoretical inside one synchronous tick with no scheduler hop. **Inherited knowingly and not changed
  here**: altering a copied pattern in the slice that copies it is how a relocation stops being
  provable. If it is ever fixed, it should be fixed in both places at once.
