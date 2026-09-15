# GATE — the Nexus: the six routes out, and the two things a unit test cannot see

**Status: NOT RUN.** Every row below was written BEFORE any boot, and every expected value was
recorded so that a later reading could disagree with it. **When a row is read, its reading is
written BESIDE its prediction and the prediction is NOT edited.** A prediction revised after the
fact proves nothing.

**The unit suite covers the DECISION, not the DELIVERY.** `NexusLockTest` has 21 rows over
`NexusLock`, and `NexusWiringSignatureTest` has 5 over the annotation wiring. Neither can see a
live `InventoryView`, a real `PlayerInventory` layout, or what the vanilla client does with a
cancelled event — the project has no MockBukkit, and `new ItemStack(...)` throws without a running
server. **That gap is this file.**

**Seven mutations were run against the unit suite before this file was written**, and their kill
sets are recorded at the bottom, because a row here that duplicates a mutation's coverage is a boot
slot spent on something already measured.

---

## WHAT THE UNIT SUITE ALREADY SETTLED, SO NO ROW RE-ASKS IT

- The verdict for every `ClickType`, both cursor states, every `InventoryAction` — 21 rows.
- That the locked slot is refused **whether or not it currently holds a star** (mutation M5).
- That a stray star is refused **wherever it sits** (mutation M6).
- That a menu's own slot 8 is **not** the locked slot.
- That the two annotation attributes the lock rests on are present and strictly ordered (M2).

**None of that is re-asked below.** These rows are the parts that are not decidable off-server.

---

## ROW 1 — THE CURSOR AT DEATH

**THE ONE OPEN QUESTION INHERITED FROM THE BRIEF, AND IT WAS RIGHT TO DOUBT IT.**
`RpgListeners.onPlayerDeath` sets `keepInventory(true)` and clears `getDrops()`. A cursor item is
**not a `getDrops()` entry** — it becomes a separate item entity — so `getDrops().clear()` cannot
reach it. The handler calls `closeInventory()` first, which returns the cursor only when one of OUR
menus is open, and then only through `MenuSafety.give`, which **drops at the player's feet when the
inventory is full**.

**Staging.** Three sub-rows, deliberately different so no two quantities in the row are equal:

| | screen open at death | inventory state |
|---|---|---|
| 1a | one of our menus (crafting) | 3 free slots |
| 1b | vanilla chest | 3 free slots |
| 1c | vanilla chest | **0 free slots** |

Put a **non-Nexus** item on the cursor for each (a stack of 7 cobblestone — 7 so it cannot be
confused with a count of 1, 3 or 8 anywhere else in this file), then die.

**PREDICTED:** 1a returns the cobblestone to the inventory. 1b and 1c are **UNKNOWN** — that is the
point of the row. 1c may drop at the feet even in the 1a shape.

**READING:** _(not run)_

**Why a non-Nexus item:** the star should be unable to reach the cursor at all, so staging with the
star would be testing two things at once and would fail to distinguish "the cursor path is safe"
from "the star never got there". Row 6 tests the second claim separately.

---

## ROW 2 — THE ITEM FRAME, AND THE ARMOUR STAND THAT NEEDED ITS OWN HANDLER

**MEASURED OFF-SERVER, STILL UNCONFIRMED IN PLAY.** Read from the pinned `paper-api` jar's constant
pools:

```
PlayerInteractEntityEvent        declares getHandlerList/getHandlers
PlayerInteractAtEntityEvent      declares NEITHER  -> inherits, so onNexusGiveToEntity receives it
PlayerArmorStandManipulateEvent  declares BOTH     -> its OWN HandlerList, so it does NOT
```

That is why there are two handlers and not one. **A wider NAME on a single handler would have left
the armour stand open**, which is the shape the route was nearly closed with.

**Staging.** Hold the star. In order: right-click an **item frame**, a **glow item frame**, and an
**armour stand with arms**.

**PREDICTED:** all three refuse. The star stays in slot 8, nothing enters the frame or the stand,
**no message and no sound**. Then repeat each with an ordinary cobblestone to confirm the handlers
are not refusing everything — **that control is the row**, because a handler that refuses every
interaction looks identical to one that refuses correctly.

**READING:** _(not run)_

---

## ROW 3 — THE PERFORMED ROUTES, WHICH ARE THE WHOLE REASON FOR THE PRIORITY ARRANGEMENT

**These two are live today and a cancel at a LATER priority cannot stop either**, because
`MenuRouting` performs them by calling `setCurrentItem` / `setItem` directly rather than by
un-cancelling.

**Staging.** Open a crafting table (hijacked to `CraftingMenu`, whose `acceptsInput` returns `true`
unconditionally over a `STACKING` grid).

- **3a** — shift-click the star from the hotbar toward the grid.
- **3b** — hover an empty grid cell and press **9**.
- **3c** — hover an empty grid cell and press **5** (an ordinary hotbar slot holding cobblestone).

**PREDICTED:** 3a and 3b refuse, star unmoved. **3c SUCCEEDS** — the cobblestone moves into the
grid. 3c is not padding: without it, 3a and 3b passing is equally consistent with the guard having
broken the crafting menu outright, which is the failure mode the whole design is shaped around.

**READING:** _(not run)_

---

## ROW 4 — THE COORDINATE CONVERSION, IN BOTH VIEWS

**THE ROW `NexusLockTest` EXISTS TO BE CHECKED AGAINST.** The lock works in `PlayerInventory` index
space; `NexusSlots` converts with `view.getInventory(raw)` + `view.convertSlot(raw)`. The raw
numbers differ per view and **raw 8 is the BOOTS slot in the own-inventory screen**:

| physical slot | own-inventory screen | 54-slot chest open |
|---|---|---|
| locked hotbar 8 | raw **44** | raw **89** |
| boots | raw **8** | raw **85** |

**Staging.** Wearing boots, with the star in slot 8:

- **4a** — press E (own inventory). Left-click the **boots**. Then left-click the **star**.
- **4b** — open a **double chest**. Left-click the **boots**. Then left-click the **star**.

**PREDICTED:** in both views the **boots move freely** and the **star does not**. A conversion bug
that compared raw numbers would invert exactly this in 4a — refusing the boots, permitting the star
— and would look like a working lock to anyone who only tested one of the two.

**READING:** _(not run)_

---

## ROW 5 — JOIN CONVERGENCE, INCLUDING THE CASE THAT DESTROYS AN ITEM IF IT IS WRONG

**The highest-stakes row in the file.** Every existing player joins once with slot 8 already
occupied.

**Staging.** Four sub-rows, each a separate join:

| | slot 8 before join | star elsewhere | free slots |
|---|---|---|---|
| 5a | diamond sword | none | 5 |
| 5b | diamond sword | none | **0** |
| 5c | cobblestone x13 | star in slot 3 | 5 |
| 5d | **the star** | a second star in slot 3 | 5 |

### 5c AND 5d HAVE NO IN-GAME ROUTE TO THEIR STARTING STATE, SO THE STAGING IS PART OF THE ROW

**Caught BEFORE the boot rather than after, which is the good version of finding it.** Both sub-rows
need a Nexus star resting at **slot 3** before the join, and 5d needs a **second** one. Neither
condition can arrive by playing — every route to it is refused by the guard the row exists to test:

| the route to slot 3 | what refuses it |
|---|---|
| left / right / shift-click the star at 8 | `touchesTheStar`, `LOCKED_SLOT` arm — the clicked index IS 8 |
| number key while hovering the star | the same arm, via the clicked slot |
| hover slot 3, press **9** | the same arm, via `getHotbarButton()` — the `NUMBER_KEY` arm contributes `(true, 8)` |
| **F** | `onNexusSwapHand`, and `SWAP_OFFHAND` contributes `(true, 40)` besides |
| **Q**, in a screen or in hand | the `DROP` / `CONTROL_DROP` arm, and `onNexusDrop` for the in-hand case |
| drag off the star | you cannot start one — picking it up is refused, and `refusesDrag` refuses a star on the cursor anyway |
| creative middle-click | `InventoryCreativeEvent` inherits `InventoryClickEvent`'s `HandlerList`, so it reaches `onNexusClick` — **measured below** |
| minting a second star | `NexusItems.mint` has **exactly one call site**, inside `converge`, and it mints only when none is present |

**And `converge()` runs only ON JOIN**, which is the thing being measured — so the row cannot stage
itself by converging first.

**The creative row is MEASURED, not assumed**, because own-`HandlerList` inheritance is the exact
shape that produced defect 5 of `#92`. `javap -p` against the pinned
`paper-api 26.1.2.build.74-stable`:

```
InventoryCreativeEvent           declares NEITHER  -> inherits, so onNexusClick DOES receive it
PlayerInteractAtEntityEvent      declares NEITHER  -> inherits            (CONTROL, agrees with #92)
PlayerArmorStandManipulateEvent  declares BOTH     -> its own HandlerList (CONTROL, agrees with #92)
```

The two control lines reproduce `#92`'s recorded readings from the same jar on the same run. **An
instrument that finds nothing looks identical to one that is not looking**, so it is made to print a
known-present positive alongside the answer being sought.

**THE READING IS VOID WITHOUT THE ROUTE.** A star placed by a staging route and a star stranded by a
real defect are not the same starting state, so **the reading names which route it used** or it
records an ending state with no known starting one.

**Route A — `/item replace … from …`, which COPIES a stack with its components.** No restart, no NBT
editor, no second account. It raises no inventory event, so the lock never sees it: exactly the class
of route `NexusSlots.converge`'s own javadoc names as the reason convergence exists rather than
mint-if-absent — *"a direct server-side `setItem`, which raises no event at all and so cannot be
refused"*. Hotbar index *n* is `hotbar.n`, so the locked slot is `hotbar.8` and slot 3 is `hotbar.3`.

- **5d** — one command, and the copy is byte-identical to the original **by construction**:

  ```
  /item replace entity @s hotbar.3 from entity @s hotbar.8
  ```

  Leave slot 8 alone. That is 5d's starting state: the star at 8, a second at 3.

- **5c** — the same copy FIRST, then overwrite the original. **In that order** — the reverse destroys
  the source before it has been copied:

  ```
  /item replace entity @s hotbar.3 from entity @s hotbar.8
  /item replace entity @s hotbar.8 with minecraft:cobblestone 13
  ```

  That leaves exactly one star, at 3, with 13 cobblestone at 8.

> **The `from entity` form is UNCONFIRMED on 26.1 and is written here unverified.** It is offered
> first because its failure is **LOUD** — an unparseable command errors at the console — and because
> tab-completion after `/item replace entity @s hotbar.3 ` settles it in one keystroke. If it is not
> there, fall to Route B or C rather than improvising.

**NEVER STAGE THE SECOND STAR WITH `with minecraft:nether_star`.** That mints an UNTAGGED lookalike
— row 7's whole subject — which `isNexus` rejects, so `converge` correctly leaves it where it sits
and the reading presents as *"the surplus star was not deleted"*. **A hollow fixture reported as a
defect in the code it was meant to test**, and it is the one wrong turn this row makes easy.

**THE PRE-JOIN CONTROL, WHICH IS WHAT MAKES THE STAGING CHECKABLE WITHOUT CONVERGE.** Before
quitting, in the own-inventory screen:

- Left-click the item at **slot 3** — it must be **REFUSED**. `touchesTheStar`'s second arm refuses a
  star *wherever it sits*, so a refusal here proves the copy carried the tag. It is independent of
  `converge`, and it is non-destructive: a refusal changes nothing, so the staging survives the
  check.
- Left-click an ordinary item elsewhere — it must **MOVE**. Without it, a refusal at slot 3 is
  equally consistent with the guard refusing everything, which is the control every other row in this
  file carries.

**And for 5c only: no other cobblestone anywhere in the inventory.** `MenuSafety.give` calls
`addItem`, which fills a **PARTIAL stack before an empty slot** — so a second cobblestone stack
silently absorbs the displaced 13 and the count reading is destroyed rather than failed.

**Route B — the offline copy.** Stop the server and, in `world/playerdata/<uuid>.dat`, **COPY the
existing star's stack** into the slot-3 entry rather than authoring one. A copy cannot get the tag
wrong; a hand-written tag can, and its failure is the silent one above. The tag is `rpg:nexus` — a
`BYTE` under `new NamespacedKey(plugin, "nexus")`, the namespace being `paper-plugin.yml`'s
`name: Rpg` lowercased.

**Route C — the plugin-absent boot**, which is *"disable the plugin, move the star, re-enable"* made
executable: **Paper has no `/plugin disable`**, so the window is opened by removing the jar. Stop the
server, `rm -f run/plugins/rpg-*.jar`, boot the server **directly** — `cd run && java -jar paper.jar
--nogui`, because `dev-server.sh` re-deploys the jar even under `--no-build` — move the star by hand
with no guard registered, quit, restore the jar, boot normally, join.

> **AND THE TRAP THAT MAKES ROUTE C THE RISKIEST OF THE THREE: KILLING THE SCRIPT DOES NOT KILL THE
> SERVER.** Ctrl-C on `dev-server.sh` leaves `java` running. It holds `rpg-*.jar` open, so `rm -f`
> fails with *"Device or resource busy"* — and the boot you then take for plugin-absent is the old
> plugin-PRESENT server still answering, which refuses every move and reads as Route C being
> impossible. **Confirm no surviving `java` process before removing the jar, and confirm the jar is
> gone afterwards.** Same file lock as `CLAUDE.md`'s `*** MUTATION STILL IN DEPLOYED JAR ***` entry.

**PREDICTED:**

- **5a** — sword moved to a free slot, star minted into 8. **Sword not destroyed.**
- **5b** — sword dropped at the player's feet **with the message** *"Your inventory was full --
  dropped at your feet."* Star in 8. **Sword not destroyed.**
- **5c** — star moved from 3 to 8; the 13 cobblestone land in a free slot (3 is now free, so most
  likely there). **Count still 13.** Thirteen because it collides with nothing else in this file.
- **5d** — one star remains, **in slot 8**, and no Nexus star sits anywhere else.

> **5d's PREDICTION WAS CHANGED WHILE THE STAGING WAS WRITTEN, AND THE OLD WORDING IS RECORDED
> BECAUSE IT WAS THE OPPOSITE OF WHAT THE CODE DOES.** It read *"the surplus star at slot 3 is
> **deleted**"*. Traced: `converge` collects `stars = [3, 8]` in index order, deletes every entry
> **after the first** — `setItem(stars.get(1), null)`, which is the star at **8** — then promotes the
> survivor at 3 into 8. The star that dies is slot **8**'s.
>
> **The row cannot read which one died, and that is why the prediction is an END STATE.** Both stars
> are byte-identical under every route above, so no observation distinguishes them. Staging with
> *distinguishable* stars to recover the mechanism would test convergence against a state convergence
> cannot produce — `mint` makes them identical — so a future reader must not turn this bullet back
> into a claim about WHICH star was deleted.
>
> This is a change to a prediction in a file whose own rule is that predictions are not edited. The
> rule binds **after a row is read**; this file is `Status: NOT RUN`, nothing has been read, and the
> old sentence would have sent the first reading to the wrong conclusion.

**READING:** _(not run)_ — **5c and 5d are not readable without naming their STAGING ROUTE (A, B or
C) and the outcome of the PRE-JOIN CONTROL.** A reading that omits either records an ending state
with no known starting one, which cannot be interpreted later and cannot be re-run.

**5d is the only deletion this feature performs and it is deliberate.** A surplus Nexus star is
plugin-minted, worth nothing and re-minted free; the guarantee is *never destroys a PLAYER's item*,
and the sword in 5a/5b is what that guarantee is about.

---

## ROW 6 — THE SILENT REFUSAL, AND THE CLIENT RESYNC THAT MAKES IT TRUE

**Ben's ruling: refusal is SILENT.** No message, no sound, nothing in chat. The player can see the
star did not move.

**THE RESYNC IS WHAT MAKES THAT LAST CLAUSE TRUE RATHER THAN MERELY INTENDED.** A cancelled
`InventoryClickEvent` leaves the client rendering the move until a resync arrives, so without
`player.updateInventory()` the player sees the star *did* move and then snap back — the ruling
violated in the visible direction. Every refusal path calls it.

**Staging.** With no menu open, press E and try, in order: pick up the star; drag from it; press Q
on it; press F on it; number-key it to slot 1. Watch the slot, not the chat.

**PREDICTED:** the star never appears to leave slot 8 even momentarily. No chat line, no sound, no
title. **If a flicker is visible on any of the five, name which** — the resync is per-path and a
flicker on one is not a flicker on all.

**READING:** _(not run)_

---

## ROW 7 — THE DEV STAR STILL WORKS

**The collision this design exists to survive.** `HealthModifierItems.mint` mints a `NETHER_STAR`
for `health_boost_TEMP`. The Nexus star is keyed by `keys.nexus`, never by Material.

**Staging.** `/rpg health 300` to mint a dev star. Confirm it is a nether star. Then drop it,
shift-click it into a chest, and stack-test it beside the Nexus star.

**PREDICTED:** the dev star **moves freely** — dropped, chested, picked up — while the Nexus star
does not. They **do not stack**, both because their PDC differs and because the Nexus star pins
`setMaxStackSize(1)`.

**READING:** _(not run)_

**A Material-keyed lock would fail this row in the most confusing possible way**: the dev star
would become undroppable and the bug would present as "the health item is stuck", nowhere near the
Nexus.

---

## MUTATION EVIDENCE — WHICH ROW GUARDS WHAT, MEASURED RATHER THAN ASSUMED

Run 2026-09-15 against the unit suite. Both halves of the marker grep confirmed on each (marker
present, original gone), line deltas measured against scratchpad copies, and every file restored
byte-identical with `cmp` — never `git checkout --`, which **cannot restore `NexusLock.java` at all
because it is untracked**.

| mutation | rows killed | note |
|---|---|---|
| `MUTLOCKED` `LOCKED_SLOT 8 -> 7` | **0, then 1** | **APPLIED AND DID NOT BITE on the first run.** See below. |
| `MUTOFFHAND` `OFFHAND_SLOT 40 -> 39` | **0, then 1** | **THE SAME DEFECT, MISSED BY THE SWEEP THAT SHOULD HAVE FOLLOWED THE FIRST.** See below. |
| `MUTIGNORE` strip `ignoreCancelled` from `onMenuClick` | 1 | `NexusWiringSignatureTest` only; `NexusLockTest` stayed green, correctly |
| `MUTALWAYSFALSE` | 10 | every refusal row |
| `MUTALWAYSTRUE` | 8 | **every PERMIT row — the dead-menu guard** |
| `MUTAXISLOCKED` drop the `LOCKED_SLOT` arm | 2 | both about the locked slot |
| `MUTAXISSTAR` drop the `starAt` arm | 2 | both about a star elsewhere |
| `MUTDRAGCURSOR` drop `cursorIsStar` from `refusesDrag` | 1 | the drag path only |

**`MUTAXISLOCKED` and `MUTAXISSTAR` have DISJOINT kill sets**, which is the point of running both:
`touchesTheStar` has two independent arms and one mutation could not have certified the other.

> ### THE FIRST MUTATION FAILED, AND IT IS THE MOST USEFUL THING IN THIS FILE
>
> `LOCKED_SLOT 8 -> 7` was applied — marker present, original gone, 13-byte delta, 2 lines changed
> — and **all twenty rows stayed GREEN**.
>
> Every row named `NexusLock.LOCKED_SLOT` **symbolically**, so the mutation moved the code and the
> expectation together. That is CLAUDE.md's *applied, no bite*, and nothing mechanical catches it:
> both halves of the marker grep pass, the delta is right, the scope is right.
>
> **Worse, the constant's own javadoc asserted the opposite** — that the tests referring to it
> symbolically was *why* it was guarded. A symbolic reference cannot pin a value. Only a literal
> can. `theLockedSlotIsTheRIGHTMOSTHOTBARSLOT_theONLYRowThatPinsTheVALUE` was added, the mutation
> re-run, and it then reddened **exactly that one row**. The javadoc now says which row is the sole
> guard.
>
> **Every row in this gate is staged against 8.** If the locked slot is ever re-ruled, that test
> row goes red first and this file needs restaging.

> ### AND THE SAME DEFECT WAS SHIPPED A SECOND TIME, IN THE SAME FILE, BY NOT SWEEPING FOR IT
>
> `OFFHAND_SLOT = 40` was in **exactly** the state `LOCKED_SLOT` had just been found in. Its only
> appearance in the whole test file named it symbolically **on both sides** — the touched set built
> `Touched(true, OFFHAND_SLOT)` and the predicate tested `index == NexusLock.OFFHAND_SLOT` — so the
> two moved together. Measured: `40 -> 39` applied, all 21 rows **green**.
>
> **And 39 is the HELMET slot** in the index space this class declares. The mutant lock watches a
> player's helmet instead of their offhand; every F-swap of the star out of the locked slot goes
> unrefused, and nothing reddens.
>
> **THE LESSON IS NOT "PIN YOUR CONSTANTS". IT IS THAT FINDING A SHAPE IS NOT SWEEPING FOR IT.**
> The first instance was found by mutation, written up, and fixed — and the write-up said *"the
> javadoc now says which row is the sole guard"*, a sentence scoped to one constant while a second
> instance of the identical defect sat two lines below it. It was caught in **review**, not by the
> work that had just identified the shape.
>
> **The rule, in the form that would have caught it: when you find a defect shape, enumerate where
> else it can live BEFORE writing the fix.** Here that enumeration is two lines long — `NexusLock`
> has exactly two constants.
>
> Swept afterwards, and recorded so the next reader need not redo it:
>
> | value | pinned by |
> |---|---|
> | `NexusLock.LOCKED_SLOT = 8` | `theLockedSlotIsTheRIGHTMOSTHOTBARSLOT_theONLYRowThatPinsTheVALUE` |
> | `NexusLock.OFFHAND_SLOT = 40` | `theOffhandIsSLOT40AndNOT39_theHELMET_theONLYRowThatPinsThatVALUE` |
> | `NexusItems` — `NETHER_STAR`, the `BYTE` tag, `setMaxStackSize(1)` | **nothing in the suite.** Row 7 of this file, and only that. A different gap: visibly untested rather than falsely guarded. |
> | `NexusSlots` — the conversion | **nothing in the suite.** Rows 4–5, and only those. |

> ### THE SUITE PRODUCED TWO HOLLOW CHECKS, NOT ONE — AND THE SECOND WAS FOUND IN REVIEW
>
> One is an accident; two in one file is a pattern worth naming.
>
> The first was `MUTLOCKED` passing against symbolic rows. The second was a row named
> *`theSameLockedSlotIsRefusedWhicheverViewItArrivedFrom`*, which asserted
> `refusesClick(X) == refusesClick(X)`: two `Touched` **records** built from identical arguments,
> compared through the same function. `Touched` is a record, so they were equal. **It could not fail
> for any implementation whatsoever.** Two comments — *"raw 44 converted"*, *"raw 89 converted"* —
> described a conversion the test never performed, because `NexusSlots` was never on the stack. Its
> mutation note named a mutation that **cannot be written**: `refusesClick` has no view-shaped
> parameter to corrupt.
>
> It read as coverage of the coordinate-space property — the property this entire slice exists
> around — and provided none.
>
> **The conversion cannot be unit-tested here, and that is measured rather than assumed.** Probed
> against the pinned `paper-api`: both `InventoryView.convertSlot(int)` and
> `InventoryView.getInventory(int)` are **ABSTRACT, not default** — *"abstract method convertSlot(int)
> in InventoryView cannot be accessed directly"*. The arithmetic lives in the server's
> `CraftInventoryView`, off the test classpath. A stub would have to implement `convertSlot` itself,
> and the test would then assert its own fake arithmetic: the same hollowness one layer down.
>
> The row was replaced with one that claims only what it can show — the single branch of
> `touchedOf` that returns before the view is consulted, which a plausible reordering would turn
> into an NPE inside an event handler. **Rows 4–5 below carry the conversion, alone.**
