# PLAN — the enchant table UI, and the menu base under it

Closes `NEXT.md`'s *"The enchant TABLE UI is not built; `/rpg enchant` stands in for it."*

Branched off `origin/master` at `75854fc` (PR #18, Enchant Pass 2), which is where the enchant
state model landed.

## What this pass is

The project's **first custom GUI**, built as two things at once on purpose: a reusable menu base,
and the enchant table as its first consumer. The base exists because the anvil UI, the class-select
screen and the stat screen are all coming, and each one re-solving shift-click, drag and
hotbar-swap is how the same item-duplication bug gets written three times. The table exists because
it is a real consumer that exercises the base rather than a hypothetical one.

Unlocks are **free** this pass. The XP economy and the bookshelf discount are the next pass and
wire into the same click. The per-instance roll comes after that — until it lands, real weapons
open with empty slots and candidates are dev-assigned with `/rpg enchant candidate`.

## The seven commits

| # | commit | what it is |
|---|---|---|
| 1 | `aa02e7e` refactor: the effect describer leaves the command package | `EnchantEffectLine` → `paper.weapon`, public, `of()`/`bare()` split |
| 2 | `6e6b530` feat: an enchant's icon is content | `icon:` on the enchant schema + boot-time validation |
| 3 | `6bada63` feat: the enchant grid, pinned | `EnchantMenuLayout` — pure geometry + the 3×3 bound |
| 4 | `c337653` feat: the click a candidate answers | `EnchantClickIntent` — the pure interaction model |
| 5 | `39d678e` feat: the menu base, and nothing may leave it | `Menu`/`MenuRouting`/`MenuSafety` + a render-only table |
| 6 | `6216480` feat: a click unlocks, swaps and levels | the click → write path |
| 7 | this file | the record and the deferrals |

Commits 3 and 4 land with nothing importing them, and 5 lands with no write path, so **item safety
is boot-provable before anything can edit a weapon**. That split is the reason rows 5–10c below can
be run and believed before row 12 exists.

## The decisions worth re-reading

**Identity is the `InventoryHolder`, never the title.** A title is a string a resource pack can
change and a second menu can duplicate; a holder is object identity. It also means there is no
registry to keep in step and `RpgListeners`' constructor did not grow.

**Cancellation is not a subclass's decision.** `Menu.handleClick` is `final` and cancels first,
unconditionally. `MenuClick` deliberately carries no `InventoryClickEvent`, so a consumer cannot
un-cancel. "Forgot to cancel" is not a mistake this base permits.

**The router is a whitelist.** A blacklist is one Minecraft drop away from incomplete — a new
`InventoryAction` constant would fall through it as permitted. Here it lands in the cancelled arm.

**The input model is "an empty slot ← exactly one item", and the router owns it.**
`InventoryClickEvent` fires *before* the place applies, so a handler reading the input slot sees it
empty — acceptance is decided against the **cursor**. And vanilla *merges* a place onto a matching
stack rather than swapping, which a cursor-only check cannot see. Occupancy is the router's gate,
validity is the menu's, both before the place. A refusal is a place that never happened.

**A locked candidate shows its real enchant.** The table exists so a player can decide what to
spend on; `???` removes the informed choice the screen is for. It is described at the level it
would *become* — at its own level 0, Unbreaking reads *"consumes durability on 100% of uses"*,
which is backwards and reads as a curse.

**The glow is `setEnchantmentGlintOverride`**, not `addEnchant` + `HIDE_ENCHANTS`. No `Enchantment`
instance, on an item that is never player-held.

**Slot 8 says "Not implemented yet", not `0%`.** A readout showing a zero when nothing is counted
is indistinguishable from a working readout that measured zero — CLAUDE.md's own failure mode, in a
place a player can see. *This is the one deliberate deviation from the layout brief.*

**An oversized weapon is refused, not truncated.** The extra slots survive every transition and
keep working, so rendering only the first nine leaves an enchant that is **active and invisible**.

## Boot gate

**Every row is owed by a human.** A console log can only prove the plugin loaded; everything below
needs a `Player`. Run `./scripts/dev-server.sh --refresh-content`.

Setup: `/rpg give ironblade`, then `/rpg enchant candidate 0 sharpness`, `candidate 0 unbreaking`,
`candidate 1 power`.

| # | action | expect | proves |
|---|---|---|---|
| 1 | boot | `Loaded ... 4 enchants`, no `Skipping malformed enchant`, no `names icon` | the `icon:` key parses on all four |
| 2 | right-click a table, empty hand | the custom menu opens; **the vanilla enchant screen never appears** | the intercept and the cancel |
| 3 | right-click a table holding `emberblade`, **not** sneaking | menu opens; no Fireball, no energy spent | the table wins, ahead of `WeaponFire.attempt` |
| 4 | same, **sneaking** | Fireball casts; no menu | the escape hatch — without it a Mage can never open the table |
| 5 | click slots 0/8/49 and any filler | nothing moves, nothing reaches the cursor; slot 0 CLOSES | default-cancel, and the close button |
| 6 | **shift-click** `ironblade` from your inventory | it does not move | `MOVE_TO_OTHER_INVENTORY` |
| 7 | **double-click a glass pane in your OWN inventory** | **no filler panes leave the menu** | `COLLECT_TO_CURSOR` from the BOTTOM inventory — the actual exploit |
| 8 | **1–9** and **F** over a filler pane | nothing swaps | `NUMBER_KEY` / `SWAP_OFFHAND` |
| 9 | drag a stack across menu + player slots | nothing lands in the menu | `handleDrag` |
| 10 | weapon in slot 4, **Esc**; again, **slot 0** | back in your inventory **both times**, identical item | one return path — Close and Esc are the same code |
| 10b | inventory 36/36 full, weapon in, Esc | drops at your feet **with the yellow line** | the leftover branch, said out loud |
| 10c | weapon in slot 4, pick it onto the **cursor**, Esc | returned; not lost, not duplicated | the cursor branch |
| 11 | place `ironblade` | col 2 = Sharpness + Unbreaking (grey, "Click to unlock at I."), col 4 = Power, col 6 filler | the layout literals, real icons, empty-slot filler |
| 11b | hover locked Sharpness, then locked Unbreaking | `+5% damage, x1.05` — **not** `+0%`; and **not** `100% of uses` | described at the level it would BECOME |
| 12 | click locked Sharpness | it **glints**, reads **I**, and the weapon's tooltip gains `Sharpness I` | unlock+activate in one click, right order, plus the re-mint |
| 13 | click twice more | **II**, then **III**; tooltip follows each time | `LEVEL_UP` |
| 14 | click a fourth time | no change + *"Sharpness is already at its maximum."* | `AT_MAX` is a no-op **with feedback**, not silence |
| 15 | click Unbreaking (same column) | Unbreaking glints at **I**; **Sharpness loses the glint but still reads III** | THE SWAP — the previous active keeps its level |
| 15b | click Sharpness again | re-glints at **III**, not I | swapping back cost nothing; a re-charge would show I |
| 16 | take it out, hold it, swing a mob | **9** (plain is 8) | the write reached the item, reconciled with no explicit call |
| 17 | take it out, `/rpg enchant show` | the raw blob matches what the menu rendered, slot for slot | the menu writes the command's grammar |
| 18 | build a 4th candidate in slot 0, then open the table | **refused with a red sentence naming the count**, returned, logged **once** | the 3×3 bound refuses loudly; `warnOnce` does not spam |
| 18b | try to insert a **dirt block**, and a vanilla iron sword | refused; **the item never leaves your cursor** | `acceptsInput` — a refusal is a place that did not happen |
| 19 | `/rpg give ember_staff` twice, stack them, insert the stack | refused *"one weapon at a time"*; **the stack of 2 is still on your cursor**, both intact | the only shipped path to enchanting two items with one write |
| 19a | with staff **A resting in slot 4**, left-click a second identical staff onto it | refused; slot holds **exactly one**; the second stays on your cursor | the empty-slot requirement — vanilla MERGES onto a matching stack |
| 19b | insert a single staff, enchant it, count after every click | still exactly one staff, never two, never zero | the `applyCandidateClick` amount gate |
| 19c | with a weapon in slot 4, click several candidates in a row | the weapon is never blanked, swapped or duplicated by the repaint | `render()` does not write `INPUT_SLOT` |
| 20 | menu open with the weapon in it, **`stop`** the server | after restart, the weapon is in your inventory | `onDisable`'s close-all, ahead of the flush |
| 21 | menu open with the weapon in it, `/kill` | it is in your inventory after respawn | the death path + existing `keepInventory` |
| 22 | menu open with the weapon in it, **disconnect**, rejoin | it is in your inventory | `onQuit` close, ahead of the save |
| 23 | boot with `sharpness.yml`'s `icon:` misspelled | `Content: enchant 'sharpness' names icon '...'` at WARNING; server still runs | the typo is named at boot, not rendered as a silent book |

**Count the item before and after every one of rows 6–10c and 18–22.** A dupe that creates a second
weapon and a theft that removes one look identical in a screenshot of the *after* state.

Rows 11b, 14, 15, 15b, 18 and 19 carry information rather than confirming a change: 15/15b are the
level-retention property the whole candidate model exists for; 18 and 19 are the two silent
data-loss paths.

Row 19b cannot be reached while `acceptsInput` holds — that is what makes it defence in depth.
Witness it by temporarily commenting out `acceptsInput`'s amount check, confirming a stack of 2 now
inserts, and confirming the click **refuses** rather than collapsing it. Restore from a scratchpad
copy, never with `git checkout --`.

## What was verified, and how

`./mvnw clean package` → **BUILD SUCCESS, 584 tests** across all modules (core 320, storage 17,
paper 247). `scripts/check-jar.sh` and `scripts/check-tests.sh` both pass.

The compile is also what verifies `ItemMeta.setEnchantmentGlintOverride` and
`InventoryCloseEvent.Reason` exist on the pinned Paper `26.1.2.build.74-stable`, rather than being
assumed from memory.

**Three mutations were run and witnessed**, each after confirming the marker was in the source and
`test-compile` was clean — a mutation that does not compile is not a mutation:

| mutation | result |
|---|---|
| drop the `isBlank()` arm of `EnchantDefinition`'s icon normalisation | `aBlankIconFallsBackRatherThanRenderingAsNothing` failed: *expected `<enchanted_book>` but was `<>`* |
| `COLUMN_STRIDE` 2 → 1 | `theThreeColumnsSitAtTheDesignedSlots` failed *expected `<22>` but was `<21>`*; `chromeAndFillerAddressNoCell` failed *"the gap between columns is not a cell"*. `everyCellRoundTripsThroughItsRawSlot` stayed **green** — which is exactly why the literals are pinned beside the round-trip: a consistent change in both directions is a relayout and only the literals catch it |
| `EnchantClickIntent` cap → `EnchantState.MAX_LEVEL`, ignoring the definition's own | `aPerEnchantMaxBelowTheModelsIsHonoured` failed: *expected `<AT_MAX>` but was `<LEVEL_UP>`* |

Each was restored from a scratchpad copy and re-verified green.

Note for whoever runs the gate: `-Dtest=` needs `-Dsurefire.failIfNoSpecifiedTests=false` on
Surefire 3.5.2, and `BUILD SUCCESS` with no `Tests run:` line means **zero tests ran** — `-q`
suppresses the report, so do not run a focused check quietly and believe it.
