# GATE-crafting.md — the operator gate for the crafting arc

**This file is the source of truth for the crafting boot gate.** It is versioned with the
code because, for the behaviours listed in `NEXT.md`'s unwitnessed table, **these rows are
the only check that exists anywhere in the project.** The suite passes with any of them
deleted.

Until this file existed the row text lived only in a chat transcript and two published web
pages — outside git, invisible to anyone reading the repository, and unrecoverable once the
conversation scrolled. A witness that is not in the repository is not a witness. That is the
defect this file closes, and it is why the file is committed alongside the code rather than
kept in a plan document: plans are per-slice and get superseded, and the gate outlives them.

## How to use it

- A rendered tick-through version may exist as a published page for convenience. **If the
  two ever disagree, this file wins.**
- Report at **row granularity** — "28 of 28, rows 7, 8, 12 and 12c by name" — never "the
  gate passed".
- Where a row says **count**, write the number down before and after. A duplication and a
  theft look identical in a screenshot of the after state.
- A row marked **sole witness** names the behaviour it is the only check for. Skipping it is
  not reduced confidence; it is zero.
- A row marked **discriminating** fails if the specific defect the slice exists to prevent
  is present.
- Rows are re-run when the code they witness changes — see each slice's re-gate list.

## Rule 4 applies to every row here

*A gate row can be impossible, or real but non-discriminating, and both credit coverage that
does not exist.* Before adding a row: state which line it reaches, verify it is physically
possible, and prefer a row whose **pass is impossible without the path**. A row that turns
out to be impossible is **replaced and said so**, never quietly swapped — the original stays
marked never-a-test, because the next reader's question is "was this checked", and "it was
replaced because it could not exist" is a different answer from "it passed".

---

# Slice 1 — the grid surface

Run and passed 2026-09-01, 28 of 28 (27 runnable + row 12b replaced as impossible).
Rows 7, 8, 12 and 12c carry the slice.

## Setup

| # | action | expect |
|---|---|---|
| 1 | Boot the dev server. | Plugin enables. No `SEVERE`, no exception, no `ERROR` in the log. |
| 0a | Stock inventory before starting. | Two full stacks of cobblestone · oak planks · 6 milk buckets · 3 wheat · 2 sugar · 1 egg · 8 honey bottles · an iron ingot · a stick · **a minted weapon** · **a minted item whose base Material is a vanilla ingredient** (Blaze Powder). |
| 0b | Place a crafting table, a Crafter, an enchanting table. | The Crafter needs a redstone pulse — have a lever on it before row 8. |

## The hijack

| # | action | expect |
|---|---|---|
| 2 | Right-click the crafting table. | Our menu opens. **The vanilla 3×3 screen never appears, not for a frame.** |
| 3 | Sneak-right-click the table holding a placeable block. | The weapon's `right_click` fires, or nothing at all (a silent dead click is the documented accepted case). **No vanilla screen, and no block is placed** — that last part is the permanent cost this slice pays knowingly. |

## The guards — run these early

| # | action | expect | marks |
|---|---|---|---|
| 7 | Minted weapon in a grid slot alongside vanilla ingredients that would otherwise form a valid recipe. | Result slot stays **empty**. | discriminating · only in-game check that the gear-tag screen fires |
| 8 | Minted Blaze Powder in the player's own **2×2**, then loaded into a **Crafter** and pulsed. | Both refuse. The Crafter keeps its ingredients and does not craft. | discriminating · **sole witness** that `CrafterCraftEvent` is wired — `PrepareItemCraftEvent` structurally cannot fire for a Crafter, so row 7 does not reach this surface and neither does any test |

## Ordinary crafting still works

| # | action | expect |
|---|---|---|
| 4 | Two oak planks into grid slots **0** and **3** (left column). Take the result. | 4 sticks on the cursor. Grid empty afterwards. |
| 1e | On that craft, compare the result-slot preview against what landed on the cursor. | Identical item, identical count. **This row cannot catch a substituted result** — nothing on the dev server mutates a craft result, so it passes on a build with that bug. The re-check in `craftOnceToCursor` is the only protection there. |

## Container remainders

| # | action | expect | marks |
|---|---|---|---|
| 12 | Craft a **cake**: 3 milk buckets top row, sugar–egg–sugar middle, 3 wheat bottom. Count buckets first. | Cake out, **three empty buckets left in the grid**, bucket count unchanged. | discriminating · **sole witness** for `getResultingMatrix()` · count |
| 12b | ~~Two milk buckets in each of the three top slots.~~ | **IMPOSSIBLE — NEVER A TEST.** `MILK_BUCKET.getMaxStackSize()` is 1, so the state it described cannot exist. Written from reasoning; the number was never checked. Replaced by 12c. Not a partial pass — the gate was 27/27. | |
| 12c | Two **honey bottles** in each of four cells of a 2×2. Craft one honey block. Count bottles first. | Each cell still holds a honey bottle, and **four glass bottles reach you** — inventory, or your feet with a message. | **sole witness anywhere** for `getOverflowItems()`; row 12's buckets fit back into the matrix and never reach it · count |

> Reaching the overflow path needs a remainder-producing ingredient that **stacks**. A sweep of
> the whole `Material` enum found exactly two: `HONEY_BOTTLE` (16) and `DRAGON_BREATH` (64),
> both remaindering to `GLASS_BOTTLE`. All three buckets are max stack 1.

## The grid gestures

| # | action | expect | marks |
|---|---|---|---|
| 5 | Right-click-place-one across three grid slots, then take it all back. | Nothing gained, nothing lost. | count |
| 6 | Drag-distribute a stack across grid slots. Then try a drag that starts in the grid and ends in your own inventory. | The first distributes. **The second does nothing** — a drag spanning both halves is refused. | **sole witness** for the drag widening's raw-slot bound · count |
| 1c | With the crafting menu open, drag-distribute a stack **entirely within your own inventory**. | Works exactly as it always has. | **sole witness** that `handleDrag`'s cancel-first shape did not swallow backpack drags |
| 1d | The same drag with the **enchanting** menu open. | Unchanged from before slice 1. The enchant tests are structurally blind to this. | **sole witness** |
| 9 | Grid slot holds a **stick**. Hold an **iron ingot** on the cursor, click that slot. | Ingot in the slot, stick on the cursor. One swap, nothing gained or lost. | **sole witness** for the dissimilar cursor swap · count |
| 10 | Grid slot holds **40 cobblestone**. Hold **64** on the cursor, click that slot. | Slot reads **64**, cursor keeps **40**. Total 104 either way. | **sole witness** for the merge overflow arithmetic · count |
| 11 | Grid holds cobblestone in one slot with **empty slots to its left**. Shift-click 64 more in from your inventory. | **It tops up the matching slot**, not the first empty one. | **sole witness** for shift-click's top-up ordering |
| 13 | Load the grid for **exactly one** craft. Shift-click the result slot. | Exactly one craft's output, nothing extra. **Count precisely** — the bug this catches is **+1**, invisible among a dozen crafts. | **sole witness** that `shiftClickDispatches` performs no move · count |
| 14 | Hover an **occupied** grid slot, press a number key whose hotbar slot is also full. | Refused. Both items intact. The deliberate deviation from vanilla — both-full has no direction to infer. | |
| 15 | Hover an occupied grid slot, press **F** with a full offhand. Then repeat 14 and 15 with the slot **empty**. | Both-full refused; empty-slot cases move in. | |
| 20 | **Double-click a filler pane while holding a matching item.** | **Nothing sweeps out of the menu.** `COLLECT_TO_CURSOR` stays refused. | The row that proves the collect-to-cursor exploit is closed: double-clicking a glass pane in your OWN inventory would otherwise sweep every matching stack out of the top inventory, and this menu paints some forty identical filler panes |

## Everything comes back

Load the grid before each. Together these four are the only witness that
`returnEverything()` clears before it gives — the ordering that stops a re-entrant close
leaving two.

| # | action | expect | marks |
|---|---|---|---|
| 16 | Close with **Esc**. | Everything comes back. Nothing doubled. | **sole witness** · count |
| 17 | **Die** with the menu open. | Everything comes back. | count |
| 18 | **Disconnect** with it open, reconnect. | Everything is there. | count |
| 19 | **Stop the server** with it open. Restart and log in. | Nothing lost. This is the path `onDisable`'s direct `returnEverything()` call exists for. | count |

## Nothing else moved

| # | action | expect |
|---|---|---|
| 21 | Repeat rows 9 and 10 on the **enchant menu's weapon slot**. | Both refused, exactly as before slice 1. |
| 22 | Open the enchanting table and enchant something end to end. | Unchanged. |

## 1b — not run, and recorded as not run

The thread-identity witness was never built. It needs a player clicking inside a menu, so no
boot can produce it, and the instrumentation would have to be added for one boot and removed
before the commit. Low value now: the dev server is Paper, not Folia, so the answer is
main-thread by construction. **It becomes load-bearing the day this project moves to Folia** —
`getCraftingRecipe` and both `craftItemResult` overloads are the calls to re-check then.

---

# Slice 2 — mint on craft

Run and passed 2026-09-02, operator-confirmed. Rows N2, N3, N5b, N9, N10 carry the slice.
Re-ran 7, 8, 12, 12c because `commitCraft` changed its return type.

**No weapon opted in**: the boot log reads `25 indexed, 25 claiming, of 30` — 24 armor
pieces plus one shield. `ironblade` and `emberblade` both sit on `iron_sword`, and a
contested token is dropped by design, so crafting any sword yields plain vanilla. **That is
row N4 passing, not a defect.** There is no weapon-mint row.

| # | action | expect | marks |
|---|---|---|---|
| N1 | Read the boot log for the mint-on-craft line. | `Mint-on-craft: N indexed, M claiming, of T gear definitions`, **N > 0**. The third number comes from outside the parse, so a parse that dropped claims reads `1 indexed, 1 claiming, of 30` and is obviously wrong. Read it **before** crafting. | the positive control |
| N2 | Craft a **shield** at the table. Open it. | Tooltip reads **Damage Reduction: 35%**, two flavour lines, a Common Shield footer, and decided enchant candidate slots. **A plain shield has no lore at all.** | discriminating · **sole witness** for the mint-on-result path |
| N3 | Craft an **iron chestplate**. Open it. | Tooltip reads **Defense: 6**, the flavour line, a Common Chestplate footer. **The name does not distinguish it** — vanilla and minted both read "Iron Chestplate" in white, so "looks right" is not a pass. | discriminating · proves the per-slot parse worked |
| N4 | Craft a durable item with no `craft_result` — a wooden sword or a bow. | Plain vanilla. No lore, no error, no half-minted state. | |
| N5 | Craft a **torch**, and planks. | Completely untouched. Non-durable, so the rule never fires. | discriminating negative — without it, a rule that fires on everything and a rule that works are indistinguishable |
| N5b | Shift-click bulk-craft several shields, then **open every single one**. | All minted, all rolled. **Do not count them — inspect them.** | discriminating · **sole witness** that the bulk path shares the mint |
| N6 | Craft several shields separately, compare candidate slots. | The rolls **vary**. One roll proves the call happens; varying rolls prove it draws. | |
| N7 | Craft one, then `/rpg give` the same definition. | Both rolled, indistinguishable in kind. | |
| N8 | Compare the result-slot preview against the received item — **on a craft whose roll produced candidates**. | Received item carries named candidate slots; preview carries none. Everything else identical. | **An empty roll makes this row non-discriminating** — an empty state is legal and sets the flag, so with no candidates the two are identical and the row passes either way. Discard and re-craft until one rolls candidates. |
| N9 | Load a Crafter with a valid **iron chestplate** recipe, pulse it. | Refuses. Ingredients stay in the block. Reaches the **durable-result** guard — the policy — not row 8's invariant. | **sole witness** for the durable-result guard; `getMaxDurability()` throws headless so no test can reach it |
| N10 | Load a Crafter with a **torch** recipe, pulse it. | Still crafts. | discriminating negative — **a guard that refuses every Crafter craft passes N9 perfectly** |

**The bill this slice buys:** 84 durable materials can no longer be crafted in a Crafter —
shears, flint and steel, fishing rods, carrot on a stick, brush, wolf armor, mace and seven
spear variants among them. Automated shear production for a wool farm stops working.

---

# Slice 3 — the grid's vanilla feel

Run and passed 2026-09-02, operator-confirmed: every row below, and the re-opened rows listed next.
Rows S1, S6, S11 and S12 carry the slice.

**M6 IS NOT RUN AND IS STILL OWED.** It was briefly recorded as run — inferred from "the gate was
run" rather than reported — and corrected the same day. It is a separate BUILD, not a row, which is
how it fell through: a report covering the rows does not cover it. A pass is written here only when
someone says it was observed.

Three defects found by RUNNING the slice 2 gate; every row there passed, so these are findings
rather than failures.

**Re-opened by this slice**, because it changes `MenuRouting`, `Menu` and `CraftingMenu`:
**6, 9, 10, 11, 13, 1c, 1d, 16-19** and **12, 12c** (the last two because `commitCraft` changed
shape again), plus **N5b** and **N8** from slice 2. **Row 20 is REWRITTEN below, not re-run.**

## Row 20, superseded 2026-09-02

| # | action | expected | notes |
|---|---|---|---|
| ~~20~~ | ~~Double-click a filler pane while holding a matching item.~~ | ~~Nothing sweeps out of the menu. COLLECT_TO_CURSOR stays refused.~~ | **SUPERSEDED — the gesture changed.** It is now performed rather than refused, so "nothing happens" is no longer the correct observable. Replaced by S12, which asserts the same exploit is still closed while the gesture works. Kept because "replaced because the gesture changed" is a different answer from "it passed" |

## The recipe pin

| # | action | expected | notes |
|---|---|---|---|
| S1 | **THE REPORTED CASE.** Shield layout: 6 oak planks in each plank slot, **50 iron ingots** in the iron slot. **Count the ingots first.** Shift-click the result. | **Six shields, and FORTY-FOUR IRON INGOTS still in the grid.** | discriminating · **sole witness** for the pin · **count** — the defect's signature is a number, not an appearance. Unpinned, the loop re-matches to iron nuggets and converts the remaining 44 |
| S2 | 64 planks in each plank slot, shift-click. | Stops at 64 crafts with material still loaded. A **second** shift-click continues from there. | proves the `MAX_BULK_CRAFTS` bound and the pin-mismatch exit are DIFFERENT exits, which otherwise look identical |
| S3 | **Firework rockets** — paper and gunpowder, plenty of both — shift-clicked. | Crafts normally, repeatedly. | **sole witness** that the pin works for `ComplexRecipe`. `Recipe` declares no key; only `CraftingRecipe` and `ComplexRecipe` do. Nothing else in the gate reaches a complex recipe, and slice 1 delegated to the server's matcher precisely to handle them |

## The drag refresh

| # | action | expected | notes |
|---|---|---|---|
| S4 | Drag-distribute a stack across the grid to complete a recipe, then **touch nothing else**. | The result slot fills **on its own**, within a tick. | **sole witness** for `onDragPermitted`. Before this slice the preview stayed stale until the next click |

## The double-click collect

| # | action | expected | notes |
|---|---|---|---|
| S5 | Cursor holds 1 plank. **63+ planks in your inventory**, planks ALSO loaded in the grid. Double-click. **Count grid contents first.** | Cursor fills to 64. **The grid is UNTOUCHED** and the loaded recipe survives. | **count** — the inventory alone could fill it, so the grid must not be reached |
| S6 | Cursor holds 1 plank. Only **10** planks in your inventory, plenty in the grid. Double-click. **Count both first.** | Takes the 10 **AND** reaches into the grid. | **THE ONE THAT CATCHES A DEAD SECOND TIER.** Without it, a collect that never touches the grid passes S5 perfectly, and "inventory first" is indistinguishable from "inventory only" · **count** |
| S7 | Several partial plank stacks plus one full stack in your inventory. Double-click. | The **partials** drain first; the full stack is broken into last. | the half of vanilla's behaviour that is kept, and nothing else in the gate exercises it |
| S12 | **Hold glass panes matching the filler. Double-click.** | **NOTHING leaves the menu chrome.** | **discriminating · replaces row 20** · the row worth failing the slice over. Vanilla's collect would sweep every matching stack out of the top inventory, and this menu paints forty identical panes. Performing the gesture is what answers that — the objection was never dropped |
| S8 | Load a recipe. **Double-click the RESULT slot.** **Count ingredients first.** | **Exactly ONE craft's worth consumed.** | **count** · NOT "nothing happens" — a double-click fires LEFT then DOUBLE_CLICK, and the LEFT half already crafted. Two crafts is the defect, and it is invisible except by counting |
| S9 | Load a full recipe, then double-click a matching stack held in your inventory. | The preview reflects the grid **within the same click** — no second action needed. | the collect refreshes synchronously, because we performed it and know the grid. Finding 2 in a new costume |
| S10 | **In creative mode**, middle-click a filler pane. | Nothing is cloned. | **sole witness** for the `CREATIVE` refusal, which was split into its own statement when `DOUBLE_CLICK` left it. Without this row that guard has no check at all |
| S11 | **REGRESSION — the ENCHANT menu.** Open it with a weapon in its slot. Double-click a matching stack held in your inventory. | **The weapon slot is untouched.** The gesture collects from your inventory only. | **sole witness** that collect sources are STACKING slots, not `inputSlots()`. This is a BASE-CLASS change exactly as `handleDrag` was, and the enchant tests are structurally blind to it — the same blindness that made 1c and 1d necessary |

## Mutation 6 — OWED. Run against a MUTATED build

| # | action | expected | notes |
|---|---|---|---|
| M6 | Build with the pin **re-read inside** `craftRepeatedly`'s loop rather than captured before it. Run **S1**. | The ingot count goes to **zero** — the mutant converts them. | The pin's capture-once property has NO unit witness: the field moves during the loop, which needs a live menu and a live grid. Listing the mutation without executing it would be a prediction nothing tests. Run once, then restore and re-run S1 clean |

---

# Maintenance

- When a slice changes `MenuRouting`, `Menu` or `CraftingMenu`, list by number which rows
  re-open, from `NEXT.md`'s unwitnessed table. The suite passes either way for all of them.
- When a row is added, give it the same three columns: action, expected observable, and what
  visibly distinguishes a pass — plus its sole-witness claim if it has one.
- `NEXT.md`'s unwitnessed table should name the row id here that witnesses each entry, so the
  two documents are joined rather than parallel.
