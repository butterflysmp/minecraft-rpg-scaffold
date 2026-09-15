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

**PREDICTED:**

- **5a** — sword moved to a free slot, star minted into 8. **Sword not destroyed.**
- **5b** — sword dropped at the player's feet **with the message** *"Your inventory was full --
  dropped at your feet."* Star in 8. **Sword not destroyed.**
- **5c** — star moved from 3 to 8; the 13 cobblestone land in a free slot (3 is now free, so most
  likely there). **Count still 13.** Thirteen because it collides with nothing else in this file.
- **5d** — the surplus star at slot 3 is **deleted**; one star remains, in slot 8.

**READING:** _(not run)_

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
