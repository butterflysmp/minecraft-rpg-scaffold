# PLAN — Slice E: Arrows load quivers

**Status: NOT IMPLEMENTED. No Java written, no test written, no mutation owed, nothing booted.**
This document is the investigation and the design.

> **THREE FORWARD REFERENCES, NAMED HERE SO THEY DO NOT READ AS ERRORS.** This branch is cut from
> `master`, and **Slice D (Expanded Quiver) has not merged yet.** So three citations below point at
> things that are **not in this branch's base**:
>
> - **§6.3** cites `ExpandedQuiverContentInvariantTest` — ships with Slice D.
> - **§8** cites Slice D's `MUT-MERGE` / `MUT-PREFIX` result — the measurement that two assertions
>   modelling their defects against `Stat` reddened **nothing**.
> - **§8**'s rule is that measurement's lesson.
>
> **ORDERING, and it is not negotiable: SLICE D MERGES FIRST, THEN THIS REBASES ONTO THE NEW
> `master`.** A rebase against a tree identical to the one these patches were written against cannot
> conflict; **the reverse order is untested and the tempting order was MEASURED FAILING** on the
> Locust split, with `merge-tree` run both ways. Expect `CLAUDE.md` and `NEXT.md` to be the only
> overlap — both slices touch them, neither touches the other's code.
>
> **A citation is a claim that a thing is there, and it is as checkable as any other.** These three
> become true on merge; until then they are marked rather than silently wrong.
>
> ### BUT THE MARKING MAKES THE ORDER LEGIBLE, NOT REQUIRED — AND THAT IS THE PART TO KNOW
>
> > **A FORWARD REFERENCE IS A DEPENDENCY WITH NO ENFORCEMENT.**
>
> **Nothing in git prevents this branch merging first.** There is no hook, no check, no failing
> build. If it does merge first, `master` carries citations to artifacts that **do not exist**, and
> **NOTHING FAILS** — no test reddens, no validator warns, no grep lists them. The reader who follows
> one finds a name and no file.
>
> **That is the same class as the `File.java:NNN` citation defect this repository already tracks as
> an open finding, arriving through MERGE ORDER instead of through insertion.** The existing one is
> caused by a line number going stale when text is added above it; this one is caused by a commit
> landing in an order nobody enforced. Same symptom, same silence, different door.
>
> **THE PRACTICAL RISK TODAY IS NEAR ZERO, and that is a fact about the schedule rather than about
> the safeguard:** Slice D is ready for its PR and Slice E is blocked on the creative-mode ruling, so
> the natural order already holds. **Recorded anyway, because "the risk is currently zero" and "the
> order is enforced" are different sentences, and only the first one is true.**
>
> **So: do not read the marking above as a guarantee.** It tells a reader what the order should be.
> It does not make it happen, and if the order is ever inverted the marking is what explains the
> dangling names rather than what prevented them.

> ## THIS IS NOT A CONTENT SLICE, AND IT IS NOT ABOUT THE DRAGON'S PLUME
>
> It is **`core` + `paper`**, and it **CHANGES TWO SHIPPED WEAPONS RETROACTIVELY**: `boltor`
> (`quiver_size: 8`) and `locust` (`quiver_size: 12`) both become **arrow-dependent**. Neither
> weapon's file is edited — **they are touched only by the mechanic.**
>
> **A reader who meets this diff expecting a new weapon will misread every file in it.** There is no
> new `content/` entry, no new `WeaponDefinition` field, and the only content-shaped thing in the
> slice is a player-facing message.

---

## 0. THE FIVE RULINGS, VERBATIM

1. **"Arrows load all quivers. I know this isn't a rule we've set up yet."**
2. **PLAIN ARROWS ONLY** — vanilla `ARROW`. Not spectral, not tipped.
3. **CONSUMED AT THE START** of the reload. An interrupted reload costs them.
4. **NO ARROWS = THE WEAPON IS DEAD.** No free magazine, no fallback. Arrows are cheap; running dry
   is a planning failure, not a design gap.
5. **PARTIAL MAGAZINES.** 7 arrows into an empty 8-round Boltor loads 7.

---

## 1. THE STRUCTURAL FINDING — **CONFIRMED, AND IT NEEDS A FIFTH KEY**

**Verified against the tree rather than accepted.**

Today a reload persists exactly **two** values, both timestamps:

```java
keys.quiverReloadStartedAt      // Quivers.beginReload
keys.quiverReloadCompletesAt
```

and maturity refills via `Quivers.finishReload` → `QuiverItems.setFull(...)` →
`setLoaded(..., resolveCapacity(weapon, adapters, wielder))`. **The amount is re-derived from
capacity at maturity**, which is sound *only* because capacity cannot move mid-reload.

**Rulings 3 and 5 together break that.** The amount is decided **at the start**, from an inventory
read at the start; the refill happens at **maturity**, when the inventory has moved on.

> ### THE PENDING ROUND COUNT MUST BE PERSISTED ON THE ITEM, alongside the two timestamps.
>
> There is no other honest place for it:
>
> - **Re-reading the inventory at maturity** refills by an amount the player never paid for — and
>   directly contradicts ruling 3, because the arrows were already taken.
> - **Re-deriving from capacity** silently restores the all-or-nothing behaviour ruling 5 refused.

> **A VALUE THAT WAS DERIVABLE BECOMES STORAGE THE MOMENT ITS INPUTS CAN CHANGE BETWEEN THE DECISION
> AND THE USE.** *"Refill to capacity"* needed no memory because **capacity cannot move mid-reload**.
> An arrow count can. That is the whole of the finding, and it is why this is a `Keys` change and not
> a one-line arithmetic edit.

**So: a fifth quiver key**, provisionally `quiver_reload_pending` (INTEGER), written in the same
`editMeta` as the two stamps and removed in the same `editMeta` that clears them.

### 1.1 AND IT REACHES `Fire.RELOAD_MATURED` — **TWO SITES, ONE VALUE**

`RELOAD_MATURED` exists on **both** verdict ladders, and `Quivers` handles it twice:

| site | path | today |
|---|---|---|
| `Quivers:90` | `beginReload`, `Reload.RELOAD_MATURED` | `finishReload(...)` → refill to **capacity** |
| `Quivers:214` | the fire path, `Fire.RELOAD_MATURED` | same `finishReload(...)` |

The fire path exists so *"the press that matures a reload is not wasted"* — the read refills the item
and the shot goes through on the same tick. **Both refills become "by the stored pending amount".**

> **They already share `finishReload`, which is what makes this safe** — one function, one read of the
> new key, and the two sites cannot drift. **If the implementation ever splits them, that is the
> defect.** Say so at `finishReload`.

---

## 2. `Quiver`'s API CHANGES — **and the blast radius is smaller than the argument suggests**

`Quiver.reload(int capacity)` is `clamp(capacity, capacity)`. It **does not take the loaded count at
all**, because a reload was always total. A partial reload depends on `loaded`, which the function
cannot currently see.

**DO NOT ADD AN OVERLOAD.** Change the signature so every call site is a compile error and gets
visited. A defaulted parameter lets a missed site read the wrong thing **silently**, and `Quiver` is
**public core API** — `QuiverSize`'s javadoc records that `resolve(9, -5.0)` returned `4` through
exactly such a public path while the content pipeline was believed to be the only way in.

> **MEASURED, AND THE PLAN SHOULD NOT OVERSELL IT: there is exactly ONE production call site.**
> `QuiverItems.java:104`, inside `stampFull`. Everything else is tests and one javadoc reference.
>
> **The signature argument still holds and the reason is not blast radius — it is the PUBLIC SURFACE.**
> An overload on a public core class is reachable by a caller that does not exist yet, and that is the
> door `QuiverSize.resolve` records being walked through. **But a report claiming "every call site
> gets visited" implies a sweep, and the sweep is one line.** Stating the real number is what stops
> the next reader believing the change was larger than it was.

**The shape must keep `QuiverSignatureTest`'s primitives-only pin.** That test *discovers* (it
reflects over public methods) and carries its own positive control, so it fails loudly rather than
vacuously. **The DECISION is core's, taking an `int` arrow count; the READ stays in `paper`** — the
same argument `QuiverSize.resolve`'s javadoc makes, and the reason **no `ItemStack` goes anywhere
near `core`.**

---

## 3. THE VERDICT LADDER — **confirmed exactly, and the insertion point is forced**

`QuiverState.reloadVerdict` today, read from the file:

```java
if (loaded.isEmpty()) return Reload.UNSTAMPED;
if (reloadStartedAt.isPresent()) {
    return isReloading(now) ? Reload.ALREADY_RELOADING : Reload.RELOAD_MATURED;
}
return loaded.getAsInt() >= Quiver.clamp(capacity, capacity)
        ? Reload.ALREADY_FULL
        : Reload.BEGIN;
```

A no-ammo state enters that ladder, and **where** decides whether the player is told the truth:

| state | must NOT be | because |
|---|---|---|
| loaded 7 / cap 8, **zero** arrows | `ALREADY_FULL` | **it is not full** |
| loaded 7 / cap 8, **zero** arrows | `BEGIN` | **nothing would happen** |
| loaded 7 / cap 8, **one** arrow | — | `BEGIN`, loading exactly **1** |

> **So the ammo check sits BETWEEN the full/not-full test and `BEGIN`** — after the ladder has
> established there is room, before it commits to starting.

**The new verdict must be distinguishable from `ALREADY_FULL` in the message the player sees.**

> **A REFUSAL THAT REUSES ANOTHER REFUSAL'S MESSAGE IS A BUG REPORT WAITING TO BE FILED.** *"Already
> full"* on a weapon showing **7/8** is exactly the kind of thing a player screenshots.

`QuiverNotice` is where that lands. It already has `empty(...)`, `reloadStarted(...)` and
`reloading(...)`, all throttled through `BrokenNotice`'s mechanism keyed in the existing
`CooldownTracker`. **The new state needs its own text and its own throttle key — not a reuse of
`EMPTY_KEY`**, or two different refusals share one throttle and silence each other.

---

## 4. PAPER'S INVENTORY READ — **the brief's measurement is WRONG, and the correction makes the slice smaller**

> **The brief states: *"this project does not read inventory CONTENTS anywhere today"* and *"the only
> `getInventory()` calls are `firstEmpty()` in `RpgCommand`"*. BOTH ARE FALSE**, and I am reporting it
> because the conclusion drawn from them — *"counting arrows and removing them is wholly new code"* —
> is the part that would have shaped the implementation wrongly.

**Measured: SIX sites walk inventory contents today.**

```
RpgListeners:729     crafter.getInventory().getContents()
CraftingMenu:646     viewer.getInventory().getStorageContents()
MenuSafety:69        player.getInventory().getStorageContents()
RecipeProbe:435      inventory.getStorageContents()
GearRefresher:58     inventory.getContents()
MenuRouting:382      backpack.getStorageContents()
```

and `getInventory()` appears in **nineteen** files, not one.

### 4.1 AND THERE IS AN ESTABLISHED THREE-PART PATTERN THAT THIS SLICE SHOULD FOLLOW

**This is the useful half of the correction.** The crafting path already does *count a material across
an inventory, decide how much to take from which slot, and debit it* — split exactly the way this
project splits things:

| step | where | shape |
|---|---|---|
| **WALK & GROUP** | `RecipeProbe.groups` | walks `getStorageContents()`, skips gear, groups by `isSimilar`, totals amounts, emits `CollectPlan.Source(tier, slot, amount)` |
| **DECIDE** | **`core/weapon/CollectPlan.plan`** | **PURE, in core** — which sources, in what order, how much from each |
| **DEBIT** | `InventoryCraft.debit(draws)` | **"the debit BY RECORDED SLOT, never by re-finding similar stacks"** |

> **THE DEBIT INVARIANT IS ALREADY WRITTEN DOWN AND APPLIES HERE UNCHANGED.** `InventoryCraft`'s
> javadoc: *"the debit BY RECORDED SLOT, never by re-finding similar stacks"*, and *"craft first,
> debit second, in one synchronous pass with no scheduler hop between."* An arrow debit that
> re-finds stacks can take the wrong ones if anything moved; this project already refuses that.
>
> **`CollectPlan` is the precedent for the core/paper split this slice needs** — a pure decision about
> slots and amounts, in `core`, with the performing left to `paper`. **It is NOT bound by
> `QuiverSignatureTest`'s primitives rule**, which pins `Quiver` specifically; `CollectPlan` already
> takes records and is correct.

**OPEN, and it is an implementation choice rather than a ruling:** whether to **reuse** `CollectPlan`
for the arrow draw or write a narrower counterpart. Reuse gets the ordering rule and the stack
arithmetic for free and is already unit-tested; the cost is that `CollectPlan`'s two-tier
inventory/menu model has no meaning here. **Recommend: reuse, with `TIER_INVENTORY` only** — but this
is worth one look at `CollectPlan.plan`'s ordering before committing, because *smallest-stack-first*
is a crafting nicety that may read oddly when it fragments a player's arrow stacks.

`Quivers.beginReload` remains the single site, as the brief says — it already switches on
`reloadVerdict` and already stamps both timestamps, so **the count, the removal and the stamp belong
in one place and one transition.** `beginReload` *"returns true only on a REAL TRANSITION"*, which is
what makes it safe to call from the swing path; **preserve that property or the arrows get taken
twice**, twenty times a second.

---

## 5. THE SPECTRAL-ARROW TRAP — **ruling 2 is LOAD-BEARING, not a preference**

> **The brief says the only `Material.ARROW` in the codebase is a menu icon in `RecipeBrowserMenu`.
> That is true of `ARROW`. It is NOT true of the arrow FAMILY**, and the exception is the one that
> matters.

```
QuiverSizeModifierItems.java:87    new ItemStack(Material.SPECTRAL_ARROW)
```

**The `quiver_size_boost` dev instrument IS a spectral arrow**, PDC-tagged, and it is the item that
grants `+19` quiver size. It is minted by `/rpg give` and a player testing this slice will be holding
one **in the same inventory as their ammo.**

> **So "plain arrows only" is not a taste ruling — it is what stops the reload EATING THE INSTRUMENT
> THAT MAKES THE MAGAZINE BIGGER.** A naive *"count anything arrow-shaped"* consumes the player's
> quiver-size gear to load the quiver it was enlarging, which is a bug report with a screenshot
> attached.
>
> **The guard is `Material.ARROW` exactly, and `TIPPED_ARROW`/`SPECTRAL_ARROW` are separate materials
> in Bukkit**, so exact-material matching already excludes both — **ruling 2 is satisfied by the
> obvious implementation, and this section exists so nobody "improves" it into a family match.**

**Recommend additionally excluding gear** via the same `CraftMatrixScreen.isGear(item, keys)`
invariant the crafting walk uses — *"a minted item is never a material"*. Belt and braces today (no
minted item is `Material.ARROW`), and it is the rule that keeps being true when one is.

---

## 6. UNRULED — **three, each investigated, none decided here**

### 6.1 CREATIVE MODE — **RULED: CREATIVE DOES NOT CONSUME ARROWS**

> **Operator, 2026-09-13: *"creative mode shouldn't consume arrows."***

> **THIS IS THE FIRST `GameMode` BRANCH IN THE PROJECT.** Measured at **zero** across both modules,
> main and test, against a positive control. **So it sets the precedent every later consumable will
> cite — mana potions, repair kits, ammo of any other kind — and it must be written TO BE CITED
> rather than written for this slice.**

#### THE RULING REMOVES THE DEBIT. IT MUST ALSO REMOVE THE REFUSAL, AND THOSE ARE DIFFERENT EDITS

**Taken literally, *"shouldn't consume"* only deletes the debit** — which would leave a creative
player with an empty inventory **refused by the no-ammo verdict for having no arrows, while
consuming none.** Refused for lacking a thing they were never going to spend.

> **RULED READING: creative SKIPS THE AMMO CHECK ENTIRELY AND RELOADS TO FULL, regardless of
> inventory.** Not *passes it trivially with a count of zero* — **skips it.**

The two differ exactly where it matters, and the plan states which so the gap is not filled by
whoever meets it first:

| creative, 0 arrows, 7/8 Boltor | verdict | result |
|---|---|---|
| **skip the check** ← **RULED** | `BEGIN` | reloads to **8/8**, inventory untouched |
| pass it trivially | no-ammo refusal | **refused**, and nothing consumed — the incoherent one |
| debit-only removal | `BEGIN`, loads **1** | partial magazine from arrows that do not exist |

#### A GAME-MODE BRANCH IS A BRANCH OVER A SET, NOT A BOOLEAN

**There are FOUR modes and the ruling names one.** `!= CREATIVE` would decide the other three **by
omission**, which is how three behaviours get chosen by whichever comparison someone reached for.

> **ENUMERATE ALL FOUR IN THE ARM AND SAY WHAT EACH DOES.** A switch over `GameMode` with no default,
> so a fifth mode in a future Paper release is a **compile error** — the same discipline
> `VanillaDamagePolicy.forCause` applies over its thirty-three `DamageCause` constants, and for the
> identical reason: *the length IS the guard.*

| mode | arrows consumed? | reasoning |
|---|---|---|
| `SURVIVAL` | **yes** | the ruled default; rulings 1–5 apply in full |
| `CREATIVE` | **no** | ruled — skips the check, reloads to full |
| `ADVENTURE` | **yes** | a play mode with a restricted inventory, not a build mode. A player in adventure is *playing*, and ruling 4's *"running dry is a planning failure"* is a statement about play |
| `SPECTATOR` | **yes — behaves as survival** | **believed unreachable, and it consumes anyway.** See below: the unreachability is a derivation, not a measurement, so the arm states a safe behaviour rather than relying on never firing |

> **`ADVENTURE` AND `SPECTATOR` ARE NOT RULED — THEY ARE DERIVED ABOVE, AND THAT IS A DIFFERENT
> THING.** The operator named `CREATIVE`. The reasoning for the other two is mine and is
> overturnable; it is written here so the derivation is visible rather than buried in a comparison
> operator.

#### AN ARM DOCUMENTED AS UNREACHABLE MUST STILL BE SAFE IF REACHED

**`SPECTATOR`'s unreachability is a DERIVATION about what a spectator can do with an item. It is not
a measurement**, and this project's own rule is that *a guard with no instances is not a guard that
cannot fire.*

> **SO THE ARM SAYS WHAT HAPPENS IF IT FIRES, AND THE ANSWER IS THE CONSERVATIVE ONE: IT CONSUMES,
> EXACTLY AS SURVIVAL DOES.** Not a throw, and not a silent share of survival's branch — a named arm
> whose behaviour is stated.

**The asymmetry is what decides it, and it runs entirely one way:**

| if the derivation is WRONG and a spectator reaches it | arm throws | arm consumes |
|---|---|---|
| outcome | **a crash**, from an assumption nobody verified | a reload that costs arrows |
| who is harmed | the player, and the server log | **nobody — a spectator has no arrows to lose and no weapon to fire** |

**An unreachable arm that throws converts a wrong assumption into a crash.** One that consumes costs
a spectator nothing **precisely because they cannot reach it anyway** — so the conservative answer is
free, and it is free *because* of the same unreachability that tempted the throw.

> **This is the identical reasoning the `ADVENTURE` derivation uses** — *a player in adventure is
> playing, so ruling 4 applies* — and stating them the same way is what makes the pair legible as one
> decision with two applications rather than two independent guesses.

**Both remain marked as MINE rather than the operator's**, which is what keeps them cheap to
overturn, and is the right default for a branch where the operator ruled exactly one arm of four.

#### AND IT ADDS A GATE ROW, BECAUSE NO TEST CAN REACH IT

**`GameMode` is a `Player` property and `core` cannot see it.** So creative is now a behaviour with
**no unit test able to reach it at all** — the split this project makes everywhere puts the decision
in `core` on primitives, and *which mode the player is in* cannot cross that line without an
`ItemStack`-shaped violation.

**Row 4 below is therefore the ONLY thing that will ever exercise this arm.** Say so in the arm's own
javadoc, per the standing rule for a guard whose only exercise is one test.

### 6.2 `/rpg give` — **SETTLED: MINT STAYS FULL** (recommendation taken 2026-09-13; no longer blocking)

**MEASURED: every minted quiver weapon arrives FULL today.** `WeaponItems.java:183` calls
`QuiverItems.stampFull(meta, weapon, keys)` at mint, which writes `Quiver.reload(weapon.quiverSize())`.

> **AND MINT IS NOT ONLY `/rpg give`.** `stampFull`'s own javadoc: *"Mint has no player -- several
> mint paths are previews and icons that describe a WEAPON rather than a held item."* **So a weapon
> that minted empty would make every menu icon and recipe preview read `Quiver: 0/8`**, which is a
> tooltip change in a slice with no business touching tooltips.

**This is the answer the brief wanted, and it also decides testability:** if minted weapons arrive
full, the slice is testable the moment it ships — `/rpg give boltor`, fire eight, then need arrows.
If they arrive empty, **nothing can be tested until a player has arrows**, which is fine, and the
icons regress, which is not.

**Recommend: mint stays FULL.** The first magazine is the one the weapon was built with; every
subsequent one costs arrows.

> **TAKEN AS RECOMMENDED, 2026-09-13**, on the operator's instruction to adopt it unless overruled.
> **This is an adopted recommendation, not a ruling**, and the distinction is worth keeping: the
> reasoning above is mine, so overturning it costs nothing and needs no re-derivation of why the
> icons matter.

### 6.3 `quiver_stone` — **REPORTED, NOT EXEMPTED**

**MEASURED: referenced in 10 test files.** Reading what each needs:

| file | what it uses `quiver_stone` for |
|---|---|
| `QuiverSizeTest`, `ReloadTimeTest`, `FireCadenceTest` (core) | its **numbers** — `9`, `34` — as arithmetic fixtures. **No item, no reload performed.** |
| `WeaponLoaderTest`, `WeaponLoreTest`, `StatsSheetTest`, `HeldFireQuantisationPinTest` | **loading and rendering** its definition |
| `QuiverSizeModifierItemsTest`, `ReloadTimeModifierItemsTest` | resolved-value arithmetic |
| `ExpandedQuiverContentInvariantTest` **(Slice D — not on this branch's base; see the header)** | counts it as one of four ranger weapons |

> **NOTHING PERFORMS A RELOAD ON IT.** Every reference is a number, a load, or a render — because a
> reload needs a `Player` and an `ItemStack`, which no unit test in this project has. **So no test
> relies on `quiver_stone` reloading, and the slice does not need to exempt it.**
>
> **Reported rather than pre-emptively exempted, as asked** — and the answer came out the other way
> from the Expanded Quiver slice, where `hunters_bow` *did* need naming.

**Whether a FIXTURE needs arrows is then a live question only for boot testing**, and it answers
itself: `quiver_stone` is a weapon like any other, it will demand arrows like any other, and the gate
rows below use the **Boltor** anyway.

---

## 7. GATE ROWS — **this slice needs a boot**

`GATE-quiver-ammo.md`, **Status: NOT RUN**, predictions written **before** any boot. At minimum:

| # | row | prediction |
|---|---|---|
| **1** | **PARTIAL LOAD.** 7 arrows, empty Boltor | `Quiver: 7/8` **and 0 arrows left**. Read the **tooltip AND the inventory** — one without the other cannot tell a partial load from a full one that failed to debit |
| **2** | **NO-AMMO REFUSAL.** 0 arrows, 7/8 loaded | the **new** notice, **NOT** "already full", and **no timestamps stamped** |
| **3** | **THE INTERRUPT COST.** Begin a reload, swap weapons before it matures | **arrows GONE, magazine unchanged** |
| **4** | **CREATIVE.** In creative with an **empty inventory**, reload a spent Boltor | **full magazine, and NO inventory change** |

> ### ROW 3 IS THE ONE TO WRITE MOST CAREFULLY
>
> It is **deliberate**, it is **unusual**, and **it is indistinguishable from a bug unless the record
> says it was chosen.** Ruling 3 is what it means, and the prediction goes in writing **before anyone
> is tempted to soften it** — because the moment it is observed, softening it will look like fixing
> it.
>
> **Stage it so no two quantities collide** — not 8 arrows into an 8-round magazine, or the row passes
> whether the debit happened or not.

**Row 2's "no timestamps stamped" is the half most likely to be dropped**, and it is the half that
distinguishes *refused* from *started and instantly finished*. Read the PDC, not just the message.

> **ROW 4 IS THE ONLY THING THAT WILL EVER EXERCISE THE CREATIVE ARM.** `GameMode` is a `Player`
> property, `core` cannot see it, and no unit test in this project holds a `Player` — so there is no
> other route, now or later. **An empty inventory is load-bearing in the staging**: with arrows
> present, a creative reload to full is indistinguishable from a survival one that debited correctly,
> and the row would pass whether the arm existed or not.
>
> **Its second half — NO INVENTORY CHANGE — is the half that can fail on its own**, and it is what
> separates *skipped the check* from *debited something*. Read the inventory, not just the tooltip.

---

## 8. WHAT IS OWED, AND WHAT IS NOT

**NO MUTATION WORK IS OWED. There is no Java with an assertion in it yet.** Saying so rather than
reporting an empty section.

**When it lands, the mutation set is knowable now — and the Slice D lesson applies directly:**

> **A TEST THAT REPRODUCES A DEFECT IS NOT A TEST WIRED TO THE CODE THAT COULD CAUSE IT.** Slice D
> shipped two assertions that modelled their defects against `Stat` with the keys as literals; both
> mutations reddened **nothing**. **Every row below must read the real constant or call the real
> function.**

- `MUT-PENDING` — `finishReload` refills to **capacity** instead of the stored pending amount.
  **Must redden.** This is the slice's headline defect and it is invisible to any test that does not
  stage a partial reload.
- `MUT-MATURED` — fix only `Quivers:90` and leave `Quivers:214` on the old path (or vice versa).
  **Must redden**, and only a row that matures a reload **through the FIRE path** can see it.
- `MUT-ORDER` — move the ammo check above the full/not-full test. **Must redden** on a full magazine
  with zero arrows, which must still say *already full*.
- `MUT-DEBIT` — debit by re-finding similar stacks instead of by recorded slot.
- `MUT-TRANSITION` — make `beginReload` return true on a non-transition. **Must redden**, and this is
  the one that takes arrows twenty times a second if it ships.

**Count the axes before counting the mutations:** `MUT-PENDING` and `MUT-MATURED` are two degrees of
freedom in one change — *what* is refilled and *which sites* refill it — and one splice cannot
certify both.

---

## 9. ORDER

1. ~~Answer the three unruled items in §6.~~ **DONE, 2026-09-13 — ALL THREE CLOSED. Nothing blocks
   implementation.** 6.1 **ruled** (creative skips the check entirely); 6.2 **adopted as
   recommended** (mint stays full); 6.3 **answered by measurement** (no test reloads `quiver_stone`,
   so no exemption needed).
2. **`core` first**: `Quiver.reload`'s new signature, the new `Reload` verdict, and the ladder
   insertion — with `QuiverStateTest` rows written **before** the paper wiring.
3. **The fifth key**, and `finishReload` reading it at both sites.
4. **`paper`**: the arrow walk, the `CollectPlan` draw, the debit, the notice.
5. **`GATE-quiver-ammo.md`**, predictions first, then the boot.

**Nothing in `content/` changes.** `boltor.yml` and `locust.yml` are not edited.
