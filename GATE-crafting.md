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

**A FOLLOW-UP LANDED AFTER THAT RUN** — the UI changes below — so a NAMED SUBSET is re-run rather
than the whole gate. **ELEVEN ROWS: Q1, Q3, Q9, Q15, Q16, Q23, S12, S12b, S12c, 6, 1c.** Everything
else was unaffected.

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
| Q16 | **THE SQUEEZE.** Carry common materials only — planks, sticks, cobble, iron, leather. Read all THREE cells. | **NEITHER VANILLA NOR ARMOR appears.** With 30 claiming definitions and the tier order, three cells means the shield and two tools | **written down as INTENDED, not a bug.** The column went from nine cells to three in slice 5, so this is now much stronger than "vanilla may be squeezed out": armor is squeezed out too, **every time**. If the three cells are a shield and two tools, the row PASSES. "Sticks and torches vanished from the crafting helper" is exactly what this looks like from outside — and so does "my armor recipes are gone" |
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

## The browser — NOT YET APPLICABLE

| # | action | expected | notes |
|---|---|---|---|
| ~~Q11~~ | ~~Open the browser. Page forward and back.~~ | ~~Pages navigate; the last page is not short or duplicated~~ | **NOT YET APPLICABLE — the browser is a placeholder this half.** Slot **26** says "Not implemented yet". Recorded rather than omitted, the same discipline row 1b uses: a row that cannot be run must say so rather than sit among rows that can |
| ~~Q12~~ | ~~Click an entry in the browser.~~ | ~~Navigates only; nothing is crafted or consumed~~ | **NOT YET APPLICABLE**, as Q11. These two become live in the second half and are the reason the browser is navigate-only |

> **THE BROWSER IS NO LONGER A CONVENIENCE.** With the column at THREE cells and thirty claiming
> definitions, it is **the only route to anything below tier 2** — all armor, and every vanilla
> recipe. Shipping the first half without it leaves most craftable things unreachable from this menu
> entirely. That is a scope fact rather than a tuning detail, and it raises the cost of deferring the
> second half well above what it was when the column had nine cells.

## Re-run from earlier slices

| # | why it re-opens |
|---|---|
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
# Maintenance

- When a slice changes `MenuRouting`, `Menu` or `CraftingMenu`, list by number which rows
  re-open, from `NEXT.md`'s unwitnessed table. The suite passes either way for all of them.
- When a row is added, give it the same three columns: action, expected observable, and what
  visibly distinguishes a pass — plus its sole-witness claim if it has one.
- `NEXT.md`'s unwitnessed table should name the row id here that witnesses each entry, so the
  two documents are joined rather than parallel.
