# GATE — Slice E: arrows load quivers

## **Status: NOT RUN.**

**Every prediction below was written BEFORE any boot**, and before the plugin was built or deployed.
Nothing in this file has been observed. When it is run, the observed column is filled in beside the
prediction — never over it.

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
| tooltip after the reload matures | **`Quiver: 7/8`** | |
| arrows left in inventory | **0** | |
| under `MUT-PENDING` this would read | `8/8` | |

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
| message | the **new** notice — *"You have no arrows -- plain Arrows load a quiver."* | |
| message is **NOT** | *"already full"*, nor *"Your quiver is empty -- left-click to reload."* | |
| tooltip | unchanged at **`7/8`** | |
| **`quiver_reload_started_at` on the item** | **ABSENT** | |
| **`quiver_reload_completes_at`** | **ABSENT** | |
| **`quiver_reload_pending`** | **ABSENT** | |

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
| arrows in inventory, immediately after the swap | **0 — GONE** | |
| Boltor's tooltip | **unchanged at `0/8`** | |
| after swapping back and waiting | still `0/8`; a new reload needs new arrows | |

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
| tooltip after maturity | **`8/8` — a FULL magazine** | |
| inventory | **UNCHANGED — no arrows added, none removed** | |
| no-ammo notice | **NOT shown** | |

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
| arrows consumed | **3, ONCE** | |
| arrows consumed if `beginReload` stopped being a real-transition gate | ~60, twenty times a second | |
| the reload completes | **yes** — the deadline is not pushed back by held input | |

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
| reload | **REFUSED** — the no-ammo notice | |
| the instrument | **still in the inventory, untouched** | |

> A family match would consume the item that ENLARGES the magazine, to fill the magazine it
> enlarged. `QuiverAmmoTest` guards the constants; **this row guards the walk**, which no unit test
> reaches.

---

## WHAT THIS GATE DOES NOT COVER

- **`locust`** is not exercised. It shares every code path with the Boltor and differs only in
  authored numbers (12 rounds). Named so the omission is deliberate rather than assumed.
- **Over-full quivers** (`roundsNeeded` returning 0 for a count above capacity) — reachable only by
  removing a quiver-size modifier mid-reload. Core row covers the arithmetic; no boot row stages it.
- **A pre-Slice-E item** carrying the two old stamps and no pending key. `finishReload` adds zero
  rounds and clears the reload. Not staged: producing one requires a build from before this slice.
