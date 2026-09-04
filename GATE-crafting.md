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
- **NAME THE ROWS YOU ARE ABOUT TO RUN, BEFORE YOU RUN THEM.** Then a count against that
  list is recoverable, and "eleven of eleven" is a complete answer because the eleven are
  written down. **A count against an unnamed set is not an answer, however precise the
  number looks.**

  > This replaces a weaker rule: *"Report at row granularity — '28 of 28, rows 7, 8, 12 and
  > 12c by name' — never 'the gate passed'."* It asked for naming at REPORT time, which is
  > the moment the information is hardest to reconstruct and easiest to skip.
  >
  > **Worked example, still unrecovered:** slice 5's first run was reported as **34 of 34**.
  > The old rule was satisfied in spirit — a precise count, not "it passed" — and the
  > breakdown is gone anyway, because no list existed to count against. The 34 also exceeds
  > the 20 runnable Q rows, so it evidently folded in re-gate rows, and which ones is part of
  > the same lost answer. The eleven-row re-gate below is the counter-example: the set was
  > named in the commit that requested it, so "eleven of eleven" reconstructs completely.
  >
  > Same shape as Q2's missing craftable / probed / distinct-stack counts: **observed by
  > someone, never written down.** Both are still owed.
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

**M6 WAS NOT RUN HERE, AND ON 2026-09-02 IT WAS CLOSED AS WILL NOT BE RUN** — see the slice 4
section. It was briefly recorded as run — inferred from "the gate was run" rather than reported —
and corrected the same day. It is a separate BUILD, not a row, which is how it fell through twice:
a report covering the rows does not cover it. A pass is written here only when someone says it was
observed, and no one ever will for this one.

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
| S12 | **Hold BLACK stained glass panes. Double-click.** | **NOTHING leaves the menu chrome.** | **discriminating · replaces row 20** · **REWRITTEN in slice 5 — it silently stopped being a test.** It said "panes matching the filler", and the filler went from GRAY to BLACK. An operator holding gray panes after that change tests nothing and the row PASSES. The material is now named explicitly so the row cannot rot the same way again |
| S12b | **With the grid EMPTY, hold GRAY panes. Double-click.** | **The status bar does not move either.** | **discriminating · new in slice 5, and it exists because of S12's rot.** Gray is no longer chrome: it is the status bar's EMPTY colour. So a gray double-click now targets the one bottom-row region that is NOT filler, and it must be just as immovable. The bar is menu furniture, not an input slot |
| S12c | **Hold LIGHT GRAY panes. Double-click.** | **Nothing leaves the suggestion column.** | **discriminating · third chrome colour, third immovable surface.** Empty quick-craft cells are `LIGHT_GRAY_STAINED_GLASS_PANE` so a short column is visible instead of vanishing into black. Every colour that appears in this menu needs a row: the collect gesture chooses its own sources, and a region nobody tested is a region nobody knows is safe |
| S8 | Load a recipe. **Double-click the RESULT slot.** **Count ingredients first.** | **Exactly ONE craft's worth consumed.** | **count** · NOT "nothing happens" — a double-click fires LEFT then DOUBLE_CLICK, and the LEFT half already crafted. Two crafts is the defect, and it is invisible except by counting |
| S9 | Load a full recipe, then double-click a matching stack held in your inventory. | The preview reflects the grid **within the same click** — no second action needed. | the collect refreshes synchronously, because we performed it and know the grid. Finding 2 in a new costume |
| S10 | **In creative mode**, middle-click a filler pane. | Nothing is cloned. | **sole witness** for the `CREATIVE` refusal, which was split into its own statement when `DOUBLE_CLICK` left it. Without this row that guard has no check at all |
| S11 | **REGRESSION — the ENCHANT menu.** Open it with a weapon in its slot. Double-click a matching stack held in your inventory. | **The weapon slot is untouched.** The gesture collects from your inventory only. | **sole witness** that collect sources are STACKING slots, not `inputSlots()`. This is a BASE-CLASS change exactly as `handleDrag` was, and the enchant tests are structurally blind to it — the same blindness that made 1c and 1d necessary |

## Mutation 6 — proposed here, NOT RUN here, closed in slice 4

| # | action | expected | notes |
|---|---|---|---|
| ~~M6~~ | ~~Build with the pin **re-read inside** `craftRepeatedly`'s loop rather than captured before it. Run **S1**.~~ | ~~The ingot count goes to **zero** — the mutant converts them.~~ | **WILL NOT BE RUN** — reclassified 2026-09-02 after a second consecutive skip. The reasoning that put it here still stands: the pin's capture-once property has NO unit witness, because the field moves during the loop and that needs a live menu and a live grid. What changed is the honest expectation of anyone running it. See the slice 4 section for the consequence and for what was done instead |

---

# Slice 4 — tools, the fourth gear kind

**Run 2026-09-02, operator-confirmed: 17 of 18.** The eighteen are T1–T12 (twelve), the five
re-opened rows N1, N4, 21, 22 and S1, and M6. **Every row passed. The one that is not a pass is
M6, and it was not run — see below; it is now classified WILL NOT BE RUN rather than owed.**

Rows **T5**, **T7**, **T9**, **T10** and **T12** carry the slice: the untiered tool, the property
no tooltip can show, the silent accessor, the `CONTAINS_GEAR` hole, and the flat list's
entry-not-file refusal.

`ToolDefinition` joins the sealed `GearDefinition`. Five iron tools ship in one file:
pickaxe, axe, shovel, hoe and **shears** — the untiered one, which is the point.

**Setup:** boot on a **FRESH data folder**, or with `--refresh-content`. `saveResource(path,
false)` never overwrites, so on a populated `run/plugins/Rpg/` the new `content/tools/`
directory is the most likely thing in this slice to silently not arrive. Only a fresh folder
exposes it.

## The boot log — read these three lines before touching anything

| # | action | expected | notes |
|---|---|---|---|
| T1 | Read the aggregate content line. | `... 5 tools, ...`, and the **tools zero-guard does NOT fire**. | **sole witness** for the SHIPPING half of the pipeline. The loader working and the file reaching disk are different things, and on a populated data folder they are indistinguishable |
| T2 | Read the mint-on-craft line. | **`30 indexed, 30 claiming, of 35 gear definitions`.** | **count** · arithmetic written in, not `> 0`: it was 25/25/30, and five tools all claiming makes it 30/30/35. A count that moves by the WRONG amount is the parse defect the third number exists to catch |
| T3 | Read for any `Tool 'x' shares its id` collision warnings. | **None.** | the four-registry loop. Tools resolve LAST, so a tool is the one kind that can lose every contest |

## The mint, and the footer noun

| # | action | expected | notes |
|---|---|---|---|
| T4 | Craft an **iron pickaxe**. Read the tooltip's last line. | Minted gear whose footer reads **exactly `Common Pickaxe`**. | **discriminating** · NOT `Common Tool` — that is what a default arm, a null kind or a fallback would each produce, and all three are indistinguishable in play. NOT `Common Iron Pickaxe` either: the footer says what KIND of gear it is, not its name |
| T5 | Craft **shears**. Read the footer. | **`Common Shears`**. | **discriminating** · the UNTIERED tool as an ordinary entry. `shears` is the whole material token with no tier prefix, so this is the row a `tiers x kinds` loader could not pass without a special case. Nothing else distinguishes a flat list from a grid |
| T6 | Craft a **DIAMOND pickaxe**. | **Plain vanilla.** No rarity name, no lore, no footer. | **discriminating** · no diamond tier ships, so nothing claims it. This is what proves `craft_result` is a PER-DEFINITION opt-in rather than "tools mint now" · also the specific item row N4 now names |

## The thing a tooltip cannot tell you

| # | action | expected | notes |
|---|---|---|---|
| T7 | **MINE with the minted iron pickaxe.** Break stone; compare the break time against a plain iron pickaxe. Check that durability ticks down. Check vanilla's own tooltip lines are still shown. | **Identical to plain in every respect.** | **discriminating · sole witness** for `ToolItems.mint` pinning nothing and flagging nothing. `ShieldDefinition` records this failure one kind over: a shield on the wrong material "would mint and render fine and then never block anything". A tool that mints, footers correctly and **digs like a fist** passes every other row here |

## The enchant surface — the regression rows

| # | action | expected | notes |
|---|---|---|---|
| T8 | Open the enchant table with the minted pickaxe in the input slot. | All three slots offer **Unbreaking and only Unbreaking**. | the pool-of-one, OBSERVED rather than predicted. No shipped enchant is gated on tools, so `poolFor(TOOL, ..)` returns the universal set alone and `candidateCount` clamps to 1. Not a defect — a content gap, recorded in `EnchantRoll` |
| T9 | **THE SILENT ONE.** With the pickaxe in the table, confirm it is NOT offered Protection, Growth or Mana Bank. Then run `/rpg enchant show` holding it. | Treated as a **tool**. No armor enchants anywhere. | **discriminating · sole witness** for the `HeldGear`/`PlacedGear` collapse. Their old `shield != null ? SHIELD : ARMOR` tail returned ARMOR for a tool — silently. The sibling accessors throw an NPE and announce themselves; **this one does not**, which is why it needs a row and they do not |
| T10 | **THE INVARIANT.** Put the minted pickaxe in the **crafting grid** with anything else. | No recipe matches. It is **never consumed**. | **discriminating · sole witness** for the tool arm of `CraftMatrixScreen.isGear`. That chain is the whitelist protecting minted items from vanilla recipes; a kind missing from it is not "not gear", it is gear the crafting surface will eat. Nothing compiles-fails when it falls behind |
| T11 | `/rpg give iron_pickaxe`, then `/rpg refresh`. | Given, and the refresh **counts it**. | the fourth arm of `GearRefresher`'s tag chain. Silent if forgotten: the tool would keep stale lore for ever |

## The loader's flat list

| # | action | expected | notes |
|---|---|---|---|
| T12 | In `run/plugins/Rpg/content/tools/iron.yml`, **delete the `kind:` line from ONE entry**. Restart. | That entry is **named and skipped**; the log says the other entries are unaffected; the count reads **4**. | **discriminating · sole witness** that a bad entry costs the ENTRY, not the file. `ArmorLoader` is all-or-nothing per tier deliberately, and this deliberately is not — a missing hoe is a missing hoe, and losing four good tools to one typo is worse. Restore the line afterwards |

## Re-run from earlier slices

| # | why it re-opens |
|---|---|
| N1 | its arithmetic MOVES — see T2 |
| N4 | now names a specific item: a **diamond pickaxe** (see T6) |
| 21, 22 | `RpgCommand.HeldGear` and `EnchantMenu.PlacedGear` were both rewritten. The enchant swaps and the end-to-end enchant are what say the collapse changed no behaviour for the three existing kinds |
| S1 | carries M6 below |

## Mutation 6 — WILL NOT BE RUN, closed 2026-09-02

| # | action | expected | notes |
|---|---|---|---|
| ~~M6~~ | ~~Build with the pin **re-read inside** `craftRepeatedly`'s loop rather than captured before it. Run **S1**.~~ | ~~The ingot count goes to **zero**.~~ | **WILL NOT BE RUN.** Skipped in slice 3 and again in slice 4, having been argued each time as cheap because a server was booting anyway. Twice is the answer: it is a separate BUILD rather than a row, so no gate run reaches it, and nobody is going to make a mutant build by hand to check a property they already believe. Not green, not deleted |

**THE CONSEQUENCE, PLAINLY: `craftRepeatedly`'s capture-once property has NO witness and is not
getting one.** If someone changes the pin to be re-read inside the loop, every test stays green,
every row in this file still passes, and a player crafting into a depleted grid gets their
remaining ingots converted to a recipe they never saw. That is the slice-3 defect, restored, with
a pin apparently in place.

**Three rows in this file do not pass, for three different reasons, and they are three different
words on purpose:**

| row | word | why it is not a pass |
|---|---|---|
| 12b | **IMPOSSIBLE — never a test** | the state it described cannot exist; written from reasoning and never checked |
| 20 | **SUPERSEDED** | the gesture changed, so its expected observable stopped being correct |
| M6 | **WILL NOT BE RUN** | runnable, correct, and declined twice — no witness exists and none is coming |

A permanently-owed row is worse than any of the three, because "owed" reads as coverage that is
arriving. This one is not arriving.

**What was done instead, and it is a smaller thing than a witness:** `craftRepeatedly` now takes
the pin as a PARAMETER rather than reading the field itself. The field is still a field and the
bug is still reachable — this makes re-reading it inside the loop *look wrong to a reader* instead
of *look natural*. Conspicuous, not witnessed. See `NEXT.md`'s unwitnessed table, which says the
same thing in the same words.

---

---

# Slice 5 — Quick Craft (FIRST HALF)

**RUN AND PASSED 2026-09-02, operator-confirmed: 34 of 34.** Q2 measured 298 microseconds against a
50000-microsecond tick.

> **THE ROW-BY-ROW BREAKDOWN WAS NOT RELAYED, and this file's own rule asks for it:** *"Report at row
> granularity — never 'the gate passed'."* "34 of 34" is the summary that rule exists to refuse.
> Recorded as given rather than expanded into a list nobody confirmed — inventing the breakdown
> would be exactly the miscredit rule 4 names. **Ask the operator which rows, and write them here.**
>
> The 34 is also more than the 20 runnable Q rows, so it evidently includes the re-gate list; which
> rows those were is part of the same missing answer.

**THE FOLLOW-UP RE-GATE RAN AND PASSED, operator-confirmed 2026-09-03: ELEVEN OF ELEVEN.**

| row | what it re-covered |
|---|---|
| **Q1** | the relayout — column 7, grid at 10-12/19-21/28-30, status bar in row 5 |
| **Q3** | a suggestion click after the column moved |
| **Q9** | the minted suggestion tooltip, and mint-without-roll on the received item |
| **Q15** | the tier order in three cells |
| **Q16** | neither vanilla nor armor appearing — passing by the column being all gear |
| **Q23** | the two grays in one glance |
| **S12** | black panes, after the row was rewritten for the chrome change |
| **S12b** | gray panes over the status bar |
| **S12c** | light gray panes over the empty suggestion cells |
| **6** | a drag touching chrome, after slot 22 became a pane |
| **1c** | own-inventory drags, same reason |

**This is the rule above working.** The eleven were named in the commit that asked for them, before
the run, so "eleven of eleven" reconstructs to a complete answer. Contrast the 34/34 above, which
cannot.

> An earlier wording listed "Q23" and "the new two-grays row" as separate entries. **They are the
> same row** — Q23 IS the two-grays row — so the list read as twelve. A count that double-counts is
> the shape rule 4 warns about from the other side: it credits coverage that is not there, by
> arithmetic rather than by a row that cannot fail.

**THE LAYOUT CHANGED AFTER THESE ROWS WERE WRITTEN AND BEFORE THEY WERE RUN.** The grid slid one
column left, the suggestion column moved to column 7 and shrank from nine cells to three, the close
button moved from slot 0 to 49, the chrome went black, and the bottom row became a status bar.
Running the gate before that change would have spent it on a layout that was about to move.

> **THAT SWEEP WAS INCOMPLETE, and the claim that it was complete is withdrawn.** This paragraph
> previously said "rows naming a slot were updated with it". **Q1 named three rows and was missed** —
> it still described row 4, nine cells and a chrome row 5 after all three had moved, and it was
> caught by a reader rather than by the sweep. Corrected 2026-09-02.
>
> The mechanism is the one `NEXT.md` records for the firework claim: a sweep that reports itself
> complete is trusted by everyone downstream, and nothing checks it. **Prose has no compiler.** When
> a layout moves, grep the gate for every slot NUMBER and every row/column WORD, not only for the
> constants that changed.
>
> **AND THAT REMEDY WAS STILL NOT ENOUGH.** The map above drew the close button at row 0 column 0
> for a further commit, in a diagram whose own legend already said `close ...... 49`. The label was
> corrected when the button moved; **the picture was not**, and the commit that edited rows 2-5 of
> this very map left row 0 alone.
>
> A glyph in an ASCII diagram is neither a slot number nor a row word, so grepping for both would
> have sailed straight past it. **The check that finds this is reading the map against the code, cell
> by cell** — which nobody had done, because a diagram looks like documentation rather than like a
> claim. It is a claim. Corrected 2026-09-03.

Crafting from the INVENTORY rather than the grid. **Three** suggestions beside the grid, clicked to
craft immediately, shift-clicked to craft repeatedly. The browser button is a visible placeholder
this half.

```
row 0    .   .   .   .  [I]  .   .   .   .      X  close ...... 49 (row 5)
row 1    .   G   G   G   .   .   .  [Q]  .      I  indicator .. 4
row 2    .   G   G   G   .  [R]  .  [Q] [»]     G  grid ....... 10 11 12 / 19 20 21 / 28 29 30
row 3    .   G   G   G   .   .   .  [Q]  .      R  result ..... 23
row 4    .   .   .   .   .   .   .   .   .      Q  suggestions  16 25 34
row 5   [S] [S] [S] [S] [X] [S] [S] [S] [S]     »  browser .... 26
                                                S  status bar . 45-48, 50-53
                                                   slot 22 is now plain filler
```

**The close button is INSIDE the status bar's row**, at slot 49, and the bar must never paint over
it — `STATUS_SLOTS` is the row minus that slot, pinned by a unit test. See row Q22.

**Setup:** a survival-mode player with a varied inventory — planks in several separate stacks,
milk buckets, iron, and at least one MINTED item (`/rpg give iron_pickaxe`). Several rows count
things, so bring a way to count.

## RUN Q2 FIRST — it can invalidate the rest

| # | action | expected | notes |
|---|---|---|---|
| Q2 | **Open the table. Read the log for `Quick Craft: first recompute took ...`.** | A duration **comfortably under 50000 microseconds** (one tick). | **RAN AND PASSED 2026-09-02, operator-confirmed: 298 microseconds.** 0.6% of a tick, ~168x headroom. **THE CADENCE STANDS** — no change to the recompute trigger, and Q5/Q6 are unaffected. This was the row to run first because it was the only one whose answer could have moved the others |

> **RESULT, 2026-09-02: `298` microseconds against a 50000-microsecond tick.**
>
> **THE COUNTS WERE NOT RELAYED.** The log line also prints craftable / probed / distinct-stack
> counts, and they did not reach the record. **They are not decoration:** the probe costs
> `distinct stacks × recipes`, so 298µs from three stacks and 298µs from thirty are different
> measurements wearing the same number. Ask the operator for them if the line is still in a log,
> and write them here. At this headroom it changes no decision — it changes what the number
> *means* to the next reader.
>
> **WHAT THIS MEASUREMENT DOES NOT COVER**, so it is never mistaken for a load test:
> - the **cold first** recompute only — a later, slower one was never timed
> - **one inventory**, whatever that player happened to be carrying
> - **one player**, on a **test server**, with no other load
>
> None of that is a concern at 168x headroom. It is the honest scope of what was observed.
>
> **THE INSTRUMENT HAS BEEN REMOVED**, per `PLAN-1b-swing-listener.md:134` — *"log once, observe on
> boot, then remove before the commit lands."* Taken out in the same commit that recorded this
> number, because **a passing row is exactly when "remove before merge" gets skipped.**
>
> It was never fit to stay: `measured` was per **menu instance** and a new `CraftingMenu` is built
> on every table open, so left in it would have logged for every player on every open, for ever.
> If a permanent measurement is ever wanted, that is a **different instrument** — a threshold
> warning that logs only above some bound — and a separate decision.

## The column

| # | action | expected | notes |
|---|---|---|---|
| Q1 | Open the table with materials for several recipes. | **Column 7 (slots 16, 25, 34)** shows suggestion icons with counts. The grid is at 10-12 / 19-21 / 28-30 with the result at 23. **Row 5 is the status bar**, with the close button at 49 inside it. The browser placeholder is at 26 | the surface exists and did not disturb the grid. **REWRITTEN: this row described the pre-relayout screen — row 4, nine cells, row 5 as chrome — long after the layout moved. See the note under the slice header** |
| Q15 | **THE TIER ORDER.** Carry materials for a shield AND plenty of planks (so torches/sticks are craftable in quantity). Read the column left to right. | The **shield sorts before** the sticks, even though far more sticks are makeable. Order is `WEAPON → ACCESSORY → TOOL → ARMOR → MATERIAL → VANILLA`, then most-craftable | **discriminating** for the ordering. A column sorted by count alone buries a minted shield under sixty-four torches |
| Q16 | **THE SQUEEZE.** Carry common materials only — planks, sticks, cobble, iron, leather, **and flint**. Read all THREE cells. | **NEITHER VANILLA NOR ARMOR appears**, and the **Flint Staff takes the top cell**. Three cells means the staff, the shield and one tool | **written down as INTENDED, not a bug.** The column went from nine cells to three in slice 5, so this is much stronger than "vanilla may be squeezed out": armor is squeezed out too, **every time**. "Sticks and torches vanished from the crafting helper" is exactly what this looks like from outside — and so does "my armor recipes are gone". **REWORDED in slice 7, and it would otherwise have kept passing:** flint was not on the old carry list, so the Flint Staff could never have appeared and the three cells would still have read shield-and-two-tools. With flint carried this becomes the **only** row that witnesses WEAPON outranking ACCESSORY and TOOL in the squeeze — the tier's first ordinal had been unreachable since it was written |
| Q3 | Click a suggestion. **Count the ingredients first.** | The item arrives **in the INVENTORY**, not on the cursor. Exactly one craft's ingredients leave. The count updates | **count** · the destination is the disambiguator between the two surfaces — see Q4 |
| Q5 | Stage a recipe in the grid, then read the suggestion counts. | They have **DROPPED** by what was staged | written down as EXPECTED, or someone reports it as a bug. The grid is deliberately NOT counted, because counting it would mean consuming it |

## The two surfaces

| # | action | expected | notes |
|---|---|---|---|
| Q4 | Stage a full recipe in the grid, then click a suggestion. | **BOTH hold:** the suggestion's item reaches the inventory AND the staged grid is **untouched** with its preview intact | **discriminating** · the two surfaces are independent. A grid craft goes to the CURSOR, a suggestion to the INVENTORY — if someone later routes the suggestion to the cursor "for consistency", that disambiguator is gone |

## The rows that carry the slice

| # | action | expected | notes |
|---|---|---|---|
| Q6 | **THE BULK TRAP.** Shift-click a suggestion with materials for many crafts. | Crafts repeatedly, with **no per-iteration stall**. One roster walk at the end, not sixty-four | **sole witness** for the bulk trap. The loop re-probes ONE recipe per pass; recomputing the roster inside it reads almost identically and is 64x the work |
| Q17 | **NOTHING REACHES THE GROUND.** Fill your inventory to a handful of free slots. Shift-click a suggestion for a NON-STACKABLE output (a shield or a tool) with materials for many. **Stand still and watch your feet.** | It stops when the inventory can no longer take one, says **"made N"**, and **NOT ONE ITEM DROPS** | **discriminating · sole witness** for `MenuSafety.fits`. The old behaviour was `give`'s drop branch firing up to 64 times — a pile of entities and the same message repeated. Non-stackable output is the case a lower `MAX_BULK_CRAFTS` could not have fixed |
| Q7 | **THE INVARIANT.** Hold a minted iron pickaxe plus plain materials. Read every suggestion. | The pickaxe is **never counted** toward any suggestion and is **never consumed** by one | **discriminating · sole witness** for `isGear`'s THIRD surface — after the grid screen and the Crafter guard. That chain is a whitelist with nothing that compile-fails, and slice 4 found it had already fallen behind the gear axis once. **The row to fail the slice over** |
| Q8 | **STALENESS, PINNED.** Stage almost everything so a suggestion shows a count it can no longer deliver. **Count your materials first.** Click it. | Refuses **cleanly and says why** — and **the ingredients are STILL THERE** | **count · discriminating** · the count is advisory, the click is authoritative. Above all it must not debit on a refusal: debit-before-craft is theft, on the path least likely to be hand-tested |
| Q9 | **Two halves, and the second is new.** (a) Look at the shield SUGGESTION ICON and note its rarity and stats. (b) Craft it and open the received item. | **(a) The icon shows the MINTED tooltip** — rarity-coloured name, `Damage Reduction`, flavour, rarity footer last — not a plain vanilla shield. **(b) The received shield's RARITY AND STATS MATCH what the icon showed, and it has enchant candidates the icon did NOT show.** | **discriminating · sole witness for mint-without-roll on the suggestion path.** The icon mints through the same `claimFor`-then-`GearItems.mint` the result slot uses, and deliberately does NOT roll: rarity and stats are deterministic from the definition, so showing them is a promise the craft keeps; candidates are a `ThreadLocalRandom` draw, so showing them would be a promise it breaks. **Open it rather than counting it** — a count passes on the very defect this catches.<br><br>**The LORE ORDERING is NOT gate-only** — `MenuIconsTest` pins chrome on top, exactly one blank, the item's own lore preserved in order, and no trailing blank when it has none. What needs a live server, and so genuinely needs this row, is the mint-versus-roll behaviour on a real `ItemMeta`: that the icon carries a gear tooltip at all, and that candidates appear only after the craft |
| Q10 | **THE SCOPE BOUNDARY, both halves.** Hold paper and gunpowder. **(a)** Check the suggestions for a BASIC firework rocket. **(b)** Then build a MULTI-STAR rocket in the grid — several firework stars plus paper and gunpowder. | **(a) The basic rocket DOES appear as a suggestion and crafts from it.** **(b) The multi-star rocket does NOT appear as a suggestion, and still crafts normally in the grid.** | **discriminating · sole witness** for where the enumerable boundary actually falls. The basic rocket is an ordinary shapeless recipe and enumerates fine; only the customizable ones are `ComplexRecipe`, which is a bare marker interface exposing no ingredients. **This row is what stops someone "restoring parity" by hand-implementing complex recipes** — and by distinguishing the two cases it says accurately WHERE the boundary is, rather than asserting a blanket absence that is simply false |

## The status bar — a three-arm switch needs its three arms observed

| # | action | expected | notes |
|---|---|---|---|
| Q18 | Open the table with an **empty grid**. Look at the bottom row. | **GRAY**, all eight cells — **and the close button is still there** at slot 49 | the EMPTY arm, and the first sighting of the close button inside the bar's row |
| Q19 | Lay a **valid** recipe. Then clear the grid. | **LIME** while it matches, back to **GRAY** when cleared | the VALID arm, **and that the bar goes BACK.** A bar that only ever turns green is indistinguishable from one that latches |
| Q20 | Put **junk** in the grid — items that form no recipe. | **RED** | the INVALID arm |
| Q21 | Put a **MINTED item** in the grid (`/rpg give iron_pickaxe`). | **RED** | **discriminating** · the CONTAINS_GEAR collapse, OBSERVED rather than assumed. `CraftMatrixScreen` hides that matrix from the matcher, so nothing will come of it and red is honest. RED deliberately means two things — "no such recipe" and "contains gear" — and a fourth state was not asked for |
| Q23 | **TWO GRAYS, ONE SCREEN.** Carry materials for FEWER THAN THREE craftable things, and leave the grid EMPTY. Look at the suggestion column and the status bar **in one glance**. | The empty suggestion cells and the empty-grid bar are **visibly DIFFERENT colours** — light gray beside gray | **discriminating** · the empty cells are `LIGHT_GRAY`, the bar's EMPTY state is `GRAY`, and they carry different meanings. **If someone later "tidies" both to one constant every other row still passes** — including S12b and S12c, which would then be testing the same material twice. This is the only row that looks at them together |
| Q22 | **THE CLOSE BUTTON SURVIVES REPAINTS, and the SEQUENCE is the point.** Open the table. Lay a valid recipe (**LIME**). Break it (**RED**). Clear it (**GRAY**). **NOW** click the X. | The menu **closes and the grid comes back** — the same path Esc uses | **discriminating · sole witness** for the `STATUS_SLOTS` exclusion surviving in play. The bar repaints on EVERY state change, so checking the button only on open would pass on a build where every repaint clobbers it. **And the failure is quiet**: Esc still works, so the symptom is "the X disappeared", not a broken menu |

## The consume path

| # | action | expected | notes |
|---|---|---|---|
| Q13 | **THE DEBIT.** Hold the materials for one craft **split across three separate stacks** (e.g. 3 + 2 + 1 planks). Note each stack's slot and size. Click the suggestion. | Exactly the right total leaves, **from the slots the probe counted**, and no other slot moves | **count · sole witness** that the debit applies to RECORDED slots rather than re-finding them by similarity. A second search can land on different stacks than the count used |
| Q14 | **THE REMAINDER.** Craft a cake from a suggestion (three milk buckets). **Count buckets first.** | Three **EMPTY BUCKETS** arrive in the inventory. Nothing is destroyed | **count · discriminating** · the case that makes "consumed = input minus resulting" incoherent — a milk bucket does not decrease, it BECOMES a bucket. On the inventory path the resulting matrix and the overflow both collapse to "give it to the player" |

## The browser — LIVE from slice 6

> ## GATE RESULT — 2026-09-03, operator-confirmed: **32 of 32 GREEN**
>
> **Q11, Q24, Q25, Q26, Q27, Q28, Q29, Q32, Q33, Q34, Q35** — every live row in this section —
> plus the twenty-one re-runs **Q1, Q3, Q6, Q7, Q8, Q9, Q10, Q13, Q14, Q15, Q16, Q17, Q22, 12, 12c,
> 16, 22, S1, S2, 13, N5b**. Q12, Q30 and Q31 are struck as superseded and correctly absent.
>
> ### Q34 WAS MISSING FROM THE NAMED SET, AND WAS CAUGHT IN REVIEW
>
> The set as first named held ten browser rows; this section holds **eleven**. It was assembled by
> extending the RE-RUN half twice for the UI changes and treating the NEW half as settled. **Q34
> landed in `8bec9e4` alongside Q32 and Q33**, and the tick-through page published for the operator
> carried the omission through.
>
> **The reviewer's check had the same shape as the mistake:** a grep for
> <code>\*\*Q(2[569]&#124;3[23])\*\*</code> — *the rows expected to have been added*, not the axis,
> which is EVERY LIVE `Q` ROW IN THIS SECTION. No pattern on either side could match Q34.
>
> > **Rule 1 applies to the grep you REVIEW with, not only the one you verify with.** Enumerate the
> > axis, not the cases you currently have. Recorded in `NEXT.md`.
>
> **And the reason it was catchable at all:** naming the set before the run did not make it
> complete — it made it **RECOVERABLE**. A set named afterwards would have been ten rows and
> self-consistent for ever.
>
> ### FIVE MEASUREMENTS ARE STILL OWED, AND THE INSTRUMENT SURVIVES BECAUSE OF IT
>
> | measurement | value |
> |---|---|
> | Q24 catalogue build time | **NOT RECORDED — OWED** |
> | entries | **NOT RECORDED — OWED** |
> | unkeyed skipped | **NOT RECORDED — OWED** |
> | not fully listable (Q29's evidence) | **NOT RECORDED — OWED** |
> | mutation 8 stageable? | **NOT RECORDED — OWED** |
>
> **NO NUMBER, NO DELETION.** The rule *"delete the instrument in the commit that records the
> number"* exists so a passing row cannot cause the removal to be skipped. **It cuts both ways:**
> deleting it before the number is written down makes an only-once measurement **permanently
> unrecoverable** rather than merely unrecorded.
>
> **Shipping it costs almost nothing, and that is VERIFIED rather than assumed.** `build()` is
> guarded by `entries == null` at both entry points; `entries` is assigned once, never reset, and
> assigned *before* the empty-catalogue early return — so even that path cannot re-run it. One
> instance exists, constructed once in `RpgListeners`. **The line logs AT MOST ONCE PER SERVER
> LIFETIME**, on the first browser open, and never at all if nobody opens the browser.
>
> That is the opposite of slice 5's instrument, which was per menu instance and would have logged
> for every player on every table open, for ever. **This one is a single console line that announces
> its own debt.** Whoever next reads a boot log can close it in a one-line commit.

> ## ⛔ TWO DECISIONS TO TAKE BEFORE THE FIRST ROW IS RUN
>
> **Both are decided from evidence only the operator has, and BOTH MUST BE WRITTEN DOWN HERE BEFORE
> THE GATE STARTS — not while writing the report.** Deciding a row's runnability afterwards is the
> failure rule 4 names: a row that turns out to be unrunnable, recorded after the fact, is
> indistinguishable in the report from a row that ran and passed.
>
> **DECISION 1 — WITHDRAWN. Q31 is superseded, so its runnability is moot.** It asked whether the
> catalogue's `not fully listable` count was non-zero. That count is **kept**, and still worth
> reading and writing down here, but it is now **Q29's** evidence rather than a row's runnability
> gate: a listed recipe whose ingredients cannot be fully enumerated still needs the honesty line
> *"(accepts more than can be listed)"*. That hazard was always about LORE, never about
> craftability — which is exactly why it survived a reversal that deleted the inert apparatus
> around it.
>
> Read it anyway when the server boots and record it here:
> `Recipe catalogue built: N entries in Nus (N unkeyed skipped, N not fully listable)`.
>
> **DECISION 2 — can mutation 8 be staged?** It reddens only against a **stale catalogue**. Attempt,
> before the gate: open the browser (building the cache), then `Bukkit.removeRecipe(key)` on a recipe
> you can see in it, then click that entry.
>
> | outcome | verdict to write here, now |
> |---|---|
> | the removal takes effect | **STAGEABLE.** Run it, watch it red |
> | it cannot be staged | **UNRUNNABLE — record the reason.** Do not leave it sitting in a table where the other twelve were watched red |
>
> **Write both verdicts into this file before running anything.** Row 12b's shape is the precedent: a
> mutation whose red state cannot exist credits coverage that does not exist.

> ## ⏳ THE INSTRUMENT SHIPS, DELIBERATELY — AND IS OWED A ONE-LINE COMMIT
>
> `RecipeCatalogue.build()` ends with a temporary `log().info("Recipe catalogue built: …")`. It
> exists to put a real number in **Q24**.
>
> > **THIS BANNER USED TO SAY "🚫 MERGE BLOCKER — the branch is NOT merge-ready until this is
> > deleted", and it was RIGHT AT THE TIME and is kept here rather than replaced silently.** Its
> > argument stands and has not been withdrawn: *Q24 will pass, and a passing row is exactly when
> > "remove before merge" gets skipped; nothing about a green gate creates pressure to go back and
> > delete a log line.* That is why the removal was made a merge condition.
> >
> > **What changed is which failure is worse.** The gate ran green and **the five figures were never
> > recorded** — so the rule *"delete the instrument in the commit that records the number"* had its
> > precondition unmet. **It cuts both ways: NO NUMBER, NO DELETION.** Deleting it now makes an
> > only-once measurement **permanently unrecoverable** rather than merely unrecorded, and the rule
> > was written to protect the number, not to protect the deletion.
>
> **The cost of shipping it is verified, not assumed** — see the gate-result block above:
> `entries == null` guards both entry points, `entries` is assigned once and never reset (and
> assigned *before* the empty-catalogue early return), and one instance exists. **At most one console
> line per server lifetime**, on the first browser open. Slice 5's instrument was per menu instance
> and would have logged for every player on every table open; this one is not that.
>
> **WHAT IS OWED, and it is one commit:** boot a server, open the browser once, read the line, write
> the five figures into the Q24 row and Q29's evidence, and delete the instrument in that same
> commit. Anyone with a boot log can close this.

**Q12 IS SUPERSEDED, NOT DELETED.** It read *"Navigates only; nothing is crafted or consumed"*,
which was true of the placeholder and is **false by design** now: the browser crafts. It stays
struck through and visible, as row 20 → S12 was handled — **the gesture changed, so its observable
stopped being correct**, which is a different thing from a row that was wrong.

**Q11 IS REWORDED, not merely re-enabled.** *"the last page is not short or duplicated"* is
ambiguous: **a genuinely short last page is CORRECT** — 1214 recipes at 45 a page ends with a short
one, every time. The two real defects are named instead.

| # | action | expected | notes |
|---|---|---|---|
| **Q11** | Open the browser (slot **26**). Page to the end and back to page 1. | **No entry appears on two pages, and no entry the catalogue holds is missing from the last page.** A SHORT final page is correct and is not a failure | **discriminating** · reworded this slice. The unit sweep `everyEntryAppearsExactlyOnceAcrossAllPages` already proves the arithmetic over every list size; this row proves the RENDERER uses it — the half no unit test can reach |
| ~~Q12~~ | ~~Click an entry in the browser.~~ | ~~Navigates only; nothing is crafted or consumed~~ | **SUPERSEDED by Q24-Q28.** Not a failure and not withdrawn as wrong: the browser became a crafting surface in slice 6, so this row's observable stopped being the correct one. Kept visible so the change of intent is on the record rather than looking like a deleted row |
| **Q24** | **THE LAZY BUILD'S COST.** **PASSED 2026-09-03 -- but the FIGURE WAS NOT RECORDED, so the instrument SURVIVES DELIBERATELY. Deleting it before the number is written down makes an only-once measurement PERMANENTLY UNRECOVERABLE rather than merely unrecorded; it logs at most once per server lifetime, so leaving it in costs one console line. Re-run this row, write the figures in, and delete it in that commit.** First player after a restart opens the browser. Watch for a hitch, then read the log line. | No perceptible stall. **Write the logged microsecond figure into this row.** | **SOLE WITNESS, and the only time the cost is ever paid.** The catalogue walk has NO early bail — keeping everything is the point — so **Q2's 298µs must not be cited for it**: that number is the suggestion probe, which dies on most recipes' first ingredient. The instrument is `Recipe catalogue built: N entries in Nus`, and it is **deleted in the commit that records this number** |
| **Q25** | **TIER ORDERING.** Open the browser at page 1. Walk down it. | Every minted-gear recipe appears **before** any vanilla one, **and the boundary is where the TIER changes, not where the page ends** | **discriminating** · REWORDED for the reversal: it checks the tier order among what IS CRAFTABLE. It is no longer "the row Q16 hands off to" -- under the craftable-now contract **Q16's squeeze is not answered anywhere**, by design. See the note below the table |
| **Q26** | **THE LIST GOES STALE UNDER YOU.** Open the browser, note an entry. **Without closing it**, spend those materials elsewhere (a hopper, a second player, a shift-click on another entry that shares them). Then click the noted entry. **Count materials first.** | Refuses with *"You do not have the materials for that."* **Nothing debited, nothing given** | **count · discriminating** · REWRITTEN for the reversal. *"Click something you cannot afford"* is now **impossible by construction on a fresh view** -- every listed entry was affordable when the list was built -- so the row would have been unrunnable as written. This is Q8's shape on the third surface, and the thing that must hold has not changed: the craft re-verifies against the LIVE inventory and a refusal costs nothing |
| **Q27** | **A BROWSER CRAFT OF MINTED GEAR.** Carry the materials. Click a claiming recipe. **Open the item.** | Arrives **minted AND rolled** -- stats, rarity footer, enchant candidates -- identical to the column and the grid | **discriminating** · *open it, do not count it.* A count sees an item arrive and cannot see that it is a plain iron sword. This is the row that proves the third surface shares `InventoryCraft` rather than reimplementing it |
| **Q28** | **BULK INTO A FULL INVENTORY.** Fill every slot but one. Shift-click an affordable entry. | Stops, says how many were made, **and NOTHING is on the ground** | **count · discriminating** · Q17's shape on the THIRD surface. `MenuSafety.fits` is checked before each pass, so a full inventory costs no ingredients |
| **Q29** | **INGREDIENT LORE vs REALITY.** Hover an entry, read "Needs:". Then craft it and watch what leaves your inventory. | The listed ingredients **are** what the craft actually consumes | **discriminating** · the lore is built from `RecipeProbe.ingredientsOf`, the same list the assembly draws against, so this row checks they have not drifted |
| ~~Q30~~ | ~~Find a multi-star firework rocket in the browser. Hover it, then click it.~~ | ~~Lore says "Cannot be crafted here" and "Use the crafting grid for this one"; clicking does nothing~~ | **SUPERSEDED by the craftable-now contract**, exactly as Q12 was. The inert-entry apparatus is GONE, so there is nothing to observe. It existed because a browser claiming to show EVERYTHING could not omit a grid-craftable recipe without that being a FALSE absence -- Q10's mistake in UI form. Under *"what you can craft here, right now"* a multi-star firework is absent **honestly**. Not deleted and not wrong: the contract changed, so the observable stopped being the correct one |
| ~~Q31~~ | ~~Read the boot log's `not fully listable` count; if non-zero, find one and hover it.~~ | ~~The honesty line appears~~ | **SUPERSEDED as a row, but its EVIDENCE survives and moves to Q29.** The `not fully listable` count is KEPT in the catalogue log: it stopped being this row's runnability gate and became the honesty line's. That hazard was always about **LORE**, never about craftability -- a listed recipe whose ingredients cannot be fully enumerated still needs *"(accepts more than can be listed)"*, and under the new contract such a recipe is still listed and still craftable |
| **Q32** | **THE LIST SHRINKS UNDER THE PLAYER.** Carry materials for a handful of recipes so the browser has **more than one page**. Go to the LAST page. **Bulk-craft (shift-click) everything on it** until the page empties. | You land on a **valid page** showing real entries -- never a blank grid, never an error | **SOLE WITNESS, and the failure mode the filter created.** Crafting shrinks the list, so the last page can cease to exist under you. `PageMath.clampPage` exists for this; the defect would be calling it only on NAVIGATION, which is the obvious moment and **not** the one that changes the page count. The browser re-clamps after every recompute |
| **Q33** | **AN EMPTY BROWSER IS NOT A BROKEN ONE.** Empty your inventory completely. Open the browser. **Hover the item in SLOT 22 — the centre of the third row — and read its whole tooltip.** The slot is named because an empty browser shows **TWO BARRIERS**: this notice and the close button in the footer. Hovering the wrong one gives a barrier with a name and no lore, which is what this row is looking for | The **NAME** reads **"Nothing you can make right now"** and there are **NO LORE LINES AT ALL** — in particular **not** *"Not implemented yet."* The icon is a **BARRIER** | **SOLE WITNESS · discriminating · TIGHTENED 2026-09-03, and it would have PASSED on the defect it now catches.** The row previously named the notice and not its lore, so an operator would have ticked a sole witness while the screen said the feature was missing: the empty state was built out of `MenuIcons.placeholder`, which always emits "Not implemented yet." **A row that cannot fail on the defect in front of it is not a witness.** `MenuIcons.icon`/`close`/`filler` have NO unit test — they need a live server — so this row's wording is the only check that exists |
| **Q34** | **ARMOR SORTS HEAD, CHEST, LEGS, FEET.** Carry materials for **two or more different armor pieces** (leather is cheapest: 24 leather makes all four). Open the browser and read them in order. | Helmet, chestplate, leggings, boots -- **in that order**, whatever their names | **discriminating**, and that is worth stating: a comparator that fell back to the recipe key would give **boots, chestplate, helmet, leggings** -- alphabetical, plausible, and wrong. Pinned in core by `CraftOrderTest`, which iterates `ArmorSlot.values()` rather than listing four constants, so a fifth slot joins the assertion automatically |
| **Q35** | **THE RECIPE BOOK BUTTON.** Open the crafting table. Look at slot **26**. Hover it, then click it. | A **green KNOWLEDGE_BOOK** named **"Recipe Book"**, lore *"Everything you can craft right now"* / *"Click to browse"*. **No "Not implemented yet."** Clicking opens the browser | **SOLE WITNESS.** Nothing anywhere observes that button's appearance. Was `MenuIcons.placeholder(Material.BOOK, …)` while the browser did not exist; the browser exists, so placeholder became the wrong constructor — reaching for it is a claim that something is NOT BUILT. **KNOWLEDGE_BOOK is the first icon in this plugin with a REAL vanilla effect** (right-click grants recipes): safe here because `Menu.handleClick` cancels unconditionally and slot 26 is not an input slot, so it can never be taken — see the call-site comment |

> ## THE BROWSER NO LONGER ANSWERS Q16, AND NOTHING ELSE DOES EITHER
>
> **Read this before running Q25, which used to be "the row Q16 hands off to".**
>
> Q16 records that the three-cell column shows neither vanilla nor armor — passing **by design**,
> because the column fills with gear. The browser was built to make those reachable. **It no longer
> does:** under the craftable-now contract it hides everything the player cannot currently afford,
> and armor is the gear kind a player is least likely to be able to afford at the moment they look.
>
> **So armor the player cannot yet afford is invisible EVERYWHERE.** Squeezed out of the column by
> tier order, hidden here by the filter. **No surface answers "what does a netherite helmet need?"**
>
> That is a **consequence of a product decision, not a defect**, and it is written into the gate
> rather than left to arrive as a complaint. Q16 still passes and still means what it said; what
> changed is that the sentence *"and the browser is where those become reachable"* is no longer
> true and must not be repeated. If it ever needs answering, the answer is a **third surface** — a
> lookup — not a filter flag on this one.

> **Q8 IS THE MUTATION THAT MAY HAVE NO RED STATE.** "The click trusts the cached recipe instead of
> re-resolving" reddens only against a **stale catalogue** — a recipe that left the roster after
> first open. Staging attempt: open the browser (building the cache), remove a recipe from the live
> roster with `Bukkit.removeRecipe(key)`, then click its entry. **If that cannot be staged on the dev
> server, record it as UNRUNNABLE with the reason** rather than leaving it in a table where every
> other mutation was watched red.


> ~~**THE BROWSER IS NO LONGER A CONVENIENCE.** With the column at THREE cells and thirty claiming
> definitions, it is the only route to anything below tier 2 — all armor, and every vanilla recipe.
> Shipping the first half without it leaves most craftable things unreachable from this menu
> entirely. That is a scope fact rather than a tuning detail, and it raises the cost of deferring the
> second half well above what it was when the column had nine cells.~~
>
> **WITHDRAWN IN PLACE, 2026-09-03 — the deferral it argued against did not happen.** The browser
> shipped in slice 6, so "the cost of deferring the second half" is no longer a live question. Kept
> struck through rather than deleted because it is the reasoning that *caused* the second half to be
> built immediately rather than parked, and a gate file that silently drops the arguments it acted on
> reads as though the decisions made themselves.
>
> **What it said that is still TRUE, and is now Q25's job to check:** the column cannot reach armor
> or vanilla, so the browser is the only route to them. That claim moved from prose into a
> discriminating row, which is where it belonged.
## Re-run from earlier slices

> **NAMED BEFORE THE RUN, per the rule added at `aee4fe1`. TWENTY-ONE rows:**
> **Q1, Q3, Q6, Q7, Q8, Q9, Q10, Q13, Q14, Q15, Q16, Q17, Q22, 12, 12c, 16, 22, S1, S2, 13, N5b.**
>
> **THREE ADDED 2026-09-03 FOR THE UI CHANGES, named here rather than at report time:**
>
> | row | why it re-opens |
> |---|---|
> | **16** | close with **Esc**. The close button's appearance changed, and this is the row that proves the return does NOT depend on clicking it — which is the reason the *"Returns your weapon."* lore came off in the first place |
> | **22** | the enchant table end to end. **`MenuIcons.close()` is SHARED**, so the enchant screen changed too. Slice 5's precedent exactly: *the enchant menu was recoloured; its behaviour must not have moved with its appearance* |
> | **Q22** | the close button surviving repaints. It observes that the button EXISTS at all, and the button was edited. Unaffected by lore in principle — re-run anyway, because "in principle" is what this list is for |
>
> Naming them here rather than in the report is the whole point: a list written afterwards is a list
> of what was run, not a list of what was owed, and the two are indistinguishable once the report is
> the only artefact.
>
> **The first draft of this list covered the shared CRAFT path and MISSED the shared PROBE.** It had
> Q3, Q6, Q7, Q8, Q13, Q14, Q17 — every row witnessing `commitCraft`, because a second caller enters
> it. But `RecipeProbe` and `SuggestionTier` are BOTH modified this slice and both feed the
> suggestion column, so the same reasoning that added 12/12c applies to them:
>
> - **Q10** — the enumerable boundary, and this slice changes what `RecipeProbe` EXPOSES;
> - **Q16** — three cells, and the row the browser now hands off to;
> - **Q15** — the tier order, and `SuggestionTier` is being edited;
> - **Q9** — the icon gains ingredient lines through `chromeOver`, and Q9 observes "rarity footer last";
> - **Q1** — the column's appearance, same cause.

> **Q10 IS A LIVE RISK, NOT ONLY A RE-RUN.** If exposing complex recipes changed what
> `Result.suggestions` contains, a multi-star firework could appear in the COLUMN — contradicting
> Q10(b), a discriminating sole-witness row.
>
> **It does not, and the reason is structural rather than careful: the exposure is ADDITIVE and
> SEPARATELY CONSUMED.** `ingredientsOf` changed visibility only; `RecipeProbe.of` still filters
> exactly as it did, and the catalogue reads the newly-public method on its own walk. Stated here AND
> at the call site, because two consumers sharing one walk is precisely how this would slip in later.

| # | why it re-opens |
|---|---|
| **Q1, Q3, Q9, Q15, Q16** | the shared PROBE, not the shared craft. `RecipeProbe` and `SuggestionTier` are both modified and both feed the column |
| **Q6, Q7, Q8, Q13, Q14, Q17** | `commitCraft`, `craftOneFromInventory` and `debit` all **changed file** — they now live in `InventoryCraft`. A pure move is still a change to the code these rows witness |
| **Q10** | see the risk note above. The one row that could be broken by a visibility edit |
| **Q15 (again), Q1, Q3** | **the COLUMN's comparator changed.** `CraftCount.RANKING`'s final term is now the shared `CraftOrder.WITHIN_TIER` instead of a local key comparison. Nothing a player can see should move — armor is squeezed out of the column, which is precisely why this is re-run rather than reasoned about. **The two orderings agreeing today is the whole hazard** |
| **12, 12c** | `commitCraft` changed shape a FOURTH time. Still the only witnesses for `getResultingMatrix` and `getOverflowItems` — **and they matter MORE this slice, not less**: on the inventory path both collapse into "give it to the player", so no Q row can tell the two calls apart |
| **S1, S2** | the pin and the `MAX_BULK_CRAFTS` bound — **and the GRID bulk loop itself changed.** `craftRepeatedly` now stops before the inventory overflows, which is a fix to a defect the grid has shipped since slice 3. These are not re-run out of caution; the code under them moved |
| S3 | the `ComplexRecipe` path through the grid — and now the other half of Q10 |
| **13** | `shiftClickDispatches` widened to the suggestion slots — it must still perform NO move — **and the bulk loop it dispatches into changed** |
| **N5b** | the bulk path minting, **through the same changed loop** |
| T10 | `isGear` on the crafting grid, now that a third caller shares the chain |
| 21, 22, S11 | `Menu` is a shared base and `EnchantMenu` is its other subclass — **and the enchant menu was RECOLOURED to black chrome.** Its behaviour must not have moved with its appearance |
| **1c, 1d, 6** | drags touching chrome. The chrome changed material, and these rows are about which slots a drag may reach |
| **16** | "load the grid, close with Esc" — the grid moved, and the close BUTTON moved into the status bar's row. Run it both ways: Esc, and the button |
| **12, 12c** | they place items in GRID slots, **and the grid slid one column left** (10-12 / 19-21 / 28-30). A row that names old slot numbers is testing filler |

> **`readMatrix` / `writeMatrix` walk `GRID_SLOTS`.** Verify by observation that matrix index 0-8
> still maps row-major onto the NEW raw slots — that a shaped recipe laid top-left in the visible
> grid still matches. A transpose or an offset here matches every shapeless recipe correctly and
> every shaped one wrongly, which reads as "some recipes are broken" rather than as a layout bug.
> `CraftingMenuLayoutTest` pins the inverse pair, so this is belt-and-braces on a unit-tested claim.
# Slice 7 — custom recipes, and the Flint Staff

The first slice in which crafting something the server did not previously know how to craft produces
RPG gear. Everything before this rode recipes Minecraft already had.

**THE SET, NAMED BEFORE THE RUN.** Live rows in this section: **R1, R2, R3, R4, R5, 7A, 7B, 7C, 7D,
7E, 7F, 7G** — twelve. Plus the re-runs listed at the bottom of this section. Row **7H** is struck
as IMPOSSIBLE and is correctly absent from any count.

> Counted from the LIVE rows in this section, not by extending slice 6's list. That is what Q34 cost.

## Registration — and the instrument that measures it

Every row here reads one boot line:

```
Custom recipes: N registered, M replaced, K refused, of A authored (I minting gear)
```

`A` comes from the REGISTRY and the rest from the registrar's own walk, so a dead registrar reads
wrong at a glance instead of reading self-consistently and wrong.

> **BEFORE EVERY BOOT IN THIS SECTION: kill every `java.exe` and confirm the deployed jar is not
> locked. AFTER: compare `target` and deployed mtimes.**
>
> Not caution — a standing condition, recorded in `NEXT.md`. `echo stop | dev-server.sh` does **not**
> stop the server: the piped command lands on the first tick after `Done`, throws
> `NullPointerException ... CommandSourceStack.getLevel() is null`, is swallowed as "an unexpected
> error", and the JVM runs on holding `run/plugins/rpg-*.jar`. The next boot then cannot `rm -f` it,
> `set -e` aborts **before deploying**, and the boot after that reads a STALE jar.
>
> **Every row here passes by reading a log line, and a stale jar prints a correct-looking one** —
> the previous build was also correct. There is no second signal. This is how a green row certifies
> the wrong build.

| # | action | expected | notes |
|---|---|---|---|
| R1 | **Fresh content.** `./scripts/dev-server.sh --refresh-content`. Read the line. | `1 registered, 0 replaced, 0 refused, of 1 authored (1 minting gear)` | **`--refresh-content` IS REQUIRED.** `content/recipes/` is a brand-new directory; `saveResource(path, false)` never overwrites, so a populated `run/` data folder predates it and ships nothing. `0 replaced` says the remove call is not spuriously matching on a virgin roster · **PASSED 2026-09-03**, exactly this line. **Provenance, because the first observation did not have it:** originally read on a jar built *before* the slice was committed, from a tree **asserted** rather than verified to match it. Re-run against a jar built by `./mvnw clean package` from the **clean committed tree** of the docs commit that added this sentence — so anyone can reproduce it by checking out that commit and rebuilding. An observation whose build you cannot name is a number, not a verification |
| R2 | `/reload confirm`, then read the same line again. | **Record the number, whichever it is.** `1 replaced` means the server kept our recipe across the reload; `0 replaced` means it wiped the roster | **THIS ROW IS THE ANSWER, not an assertion.** What `/reload` does to a plugin-added recipe is not documented on the pinned API, and the registration is deliberately written so the code is correct under BOTH answers — remove-then-add, unconditionally. Write the observed value in here and it stops being a question |
| R3 | After R2, craft the staff in the table. | A minted Flint Staff, not a plain stick | end-to-end after a reload. **NOT SUFFICIENT ALONE** — it passes identically whether we re-registered or the old registration survived, which is exactly why R2 carries the verdict and this row does not |
| **R4** | **THE INSTRUMENT'S OWN CONTROL.** Temporarily call `RecipeRegistrar.registerAll` **twice** in one `onEnable`. Boot. Read the line. | `1 replaced` on the second pass | **discriminating · run this BEFORE trusting R2.** If it prints `0 replaced`, the counter is dead and R2's number meant nothing — a number that cannot move is not a measurement. Revert the double call afterwards, and confirm the revert by re-reading R1's line |
| R5 | `/reload` twice, opening the browser each time. Compare `Recipe catalogue built: N entries`. | N identical both times | free duplicate-key detector: a recipe registered twice under one key would show up as a changed entry count |

## The rows that carry the slice

| # | action | expected | notes |
|---|---|---|---|
| 7A | **THE GRID.** Open a crafting table. Flint in the top cell, sticks in the two below it. | The result slot shows a **Flint Staff** — minted, with its stats and rarity footer — not a stick | the recipe registered AND the preview resolves the claim by RECIPE KEY. A plain stick here means the mint did not fire |
| 7B | **THE OTHER TWO SURFACES.** Carry flint and sticks. Read the **suggestion column**, then open the **browser**. Craft from **each**. | The staff appears on both and crafts from both | all three surfaces share `claimFor`. A surface showing a plain stick is the "you receive what you were shown" break the mint-in-preview machinery exists to prevent |
| 7C | **MINTED AND ROLLED, ON ALL THREE.** Craft the staff from the grid, from the column, and from the browser. **OPEN EACH ONE.** | All three arrive minted **and rolled** — stats, rarity footer, enchant candidates | **discriminating · sole witness** · *open them, do not count them.* Q27's discipline, three times. A count sees an item arrive and cannot see it is a plain stick |
| 7D | **A GHOST RECIPE.** Edit the deployed `content/recipes/flint_staff.yml` to `mints: no_such_weapon`. Boot. | A **named** warning saying the recipe mints something that is not any weapon, shield, armor piece or tool — **and the line reads `0 registered ... 1 refused`**. The recipe is NOT on the roster | **discriminating** · registering it anyway would hand a player a plain stick for their flint forever, with nothing saying why. Restore the file after |
| 7E | **THE CRAFTER BLOCK.** Put flint and two sticks in a Crafter and pulse it. | **Nothing comes out.** It jams, keeping its ingredients | **discriminating · sole witness** for the recipe arm of guard two. Our recipe's registered result is a plain STICK, which is not durable — so the existing durability test waves it straight through, and without the new arm a redstone Crafter is a flint-to-stick machine. A refused Crafter looks like a JAM, not an error; that is accepted and shared with every other guard-two refusal |
| 7F | **THE `fits` ROW. COUNT THIS ONE.** Fill every inventory slot, then free exactly one and put a **partial stack of plain sticks** in it. Carry flint and sticks in the crafting grid's reach. Shift-click the staff in the **suggestion column**. | It makes **at most what fits** and says so. **NOTHING lands on the floor.** | **discriminating · count** · the defect's signature is a number and a pile. `fits` credits a partial stick stack as room; the minted staff carries meta, is not `isSimilar`, and needs a whole slot. Unfixed, `fits` says yes 64 times and 64 weapons hit the ground |
| 7G | **THE STAFF ITSELF.** Right-click with it. | A bolt flies, deals 20 fire damage, and the target **burns for 4 seconds** | the burn is `setFireTicks`, so it is VANILLA-rated and does **not credit you** — that is the named debt in `NEXT.md`, not a bug. The bolt is a projectile and will NOT tumble or trail flame like the old thrown-flint item; also a known port gap |
| ~~7H~~ | ~~**Two definitions claiming one recipe key mint NOTHING.**~~ | — | **IMPOSSIBLE — never a test.** Under the recipe-names-the-gear direction the claim lives on the recipe, a recipe's id is its filename, and one directory cannot hold two files of one name. `RecipeRegistry` throws on a duplicate id, so the collision cannot be authored at all. The reachable neighbours are 7D (a recipe minting nothing) and the unit test `twoRecipesMayMintTheSameGear`, which pins that two recipes minting ONE weapon is a FEATURE |

## Re-runs — and the one row that changes wording

**Q16 IS REWORDED, and the reason is worth reading**: as previously written it would still have
PASSED. Its carry list was "planks, sticks, cobble, iron, leather" — **flint is not on it**, so the
Flint Staff could never have appeared and the three cells would still have been the shield and two
tools. Adding flint is what makes the row test the new roster.

| # | why it re-opens |
|---|---|
| **Q16** | **REWORDED IN PLACE** — the row itself, under "The column", now carries flint and names the staff. Read it there; it is not restated here, so the two cannot drift apart |
| **Q15** | **THE TIER ORDER, with its first ordinal finally occupied.** No shipped weapon has EVER claimed a craft, so `SuggestionTier.WEAPON` has been unreachable on every surface since it was written. The order `WEAPON → ACCESSORY → TOOL → ARMOR → MATERIAL → VANILLA` has only ever been observed from ACCESSORY down |
| **Q25** | the browser's tier ordering gains its first WEAPON-tier entry, which makes the tier order visible at the top rather than inferred |
| **T10, Q7** | `isGear` with a new gear item in play — **and for the first time one whose MATERIAL IS ITSELF A COMMON CRAFTING INGREDIENT.** A minted staff is a stick carrying `weapon_id`. The check was never material-based, so it should hold; the corollary to confirm is that a player holding only Flint Staves **cannot craft a torch from them** |
| **Q10** | the enumerable boundary now has a custom shaped recipe on OUR side of it. A `ShapedRecipe` we built exposes `getShape()` and `getChoiceMap()`, so it must enumerate exactly like a vanilla one |
| **Q27, Q29** | `claimFor` changed shape and the browser reads it. Q29's "Needs:" lore is built from `ingredientsOf` over a recipe we authored for the first time |
| **12, 12c, S1, S2, N5b** | `claimFor`'s signature changed and **the inventory bulk loop's `fits` check moved off `recipe.getResult()`**. These are not re-run out of caution; the code under them moved |

# Maintenance

- When a slice changes `MenuRouting`, `Menu` or `CraftingMenu`, list by number which rows
  re-open, from `NEXT.md`'s unwitnessed table. The suite passes either way for all of them.
- When a row is added, give it the same three columns: action, expected observable, and what
  visibly distinguishes a pass — plus its sole-witness claim if it has one.
- `NEXT.md`'s unwitnessed table should name the row id here that witnesses each entry, so the
  two documents are joined rather than parallel.
