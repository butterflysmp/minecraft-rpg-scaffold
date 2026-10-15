# GATE — NEXUS polish: green names, the five defects, the wording defaults, and one command per screen

**Status at `38cc9903` (the stack-top boot, 2026-10-14): R0a-c SUPERSEDED (GATE-selftest.md R0, PASS); R0d PASS;
D1, G1, G3, CM1, CM2 PASS (G1's gray-when-OFF clause never presented); 9 NOT READ (D2-D5, G2, CM3-CM6: their
staging -- levels 7, 10, 20, `/anvil`, `/kill`, the console -- appears nowhere in the boot); 0 FAIL. NOT approved
to merge: the seat approves, from origin.**

**The boot, 2026-10-14, on `38cc9903` (#175's head, the stack top), Paper 26.1.2 build 74:**
- CC booted it with `./scripts/dev-server.sh --refresh-content` on the seat's word; Ben was the only player. His
  LOGIN is L80 (`BaronVonYeetus ... logged in`, 22:47:02). Line numbers are that boot's `run/logs/latest.log`.
- `/rpg selftest all` ran twice. **Run 1 (L86-L1087) was in CREATIVE and certifies NOTHING** (the seat's ruling;
  L1087 `RESTORED ... gamemode=CREATIVE`). **Run 2 (L1091-L2098, SURVIVAL, after `/gamemode survival` at L1088) is
  the run of record**: every AUTO reading below is from inside its row's own START ... DONE bracket in run 2.
- **Ben's hand steps.** His words, relayed by the seat: *"all done, everything looks good"*, then *"I ran all the
  self checks and they are fine."* **The seat's ruling, 2026-10-14:** the second is Ben's explicit confirmation of
  each check on the hand-step list he was given -- **MC3 (slot), MC1, MC2, LS1, D1, G1, G3, WB1, ST2, ST6** -- so
  those sight halves are PASS, itemised by that list. Ben was in CREATIVE from 23:15:46 to 23:16:09 (L2102-L2104)
  during his hand steps; which check, if any, fell in that window is not recorded. His hand window ran with the mob
  trace OFF (L2098 `trace=false`), so a hand cast has no log line either way.
- **The aim defect** (GATE-selftest.md ST5): `tp @s ~ ~ ~ facing entity <e> eyes` turns a player from the FEET, so
  every selftest row aimed that way missed: SB2, LB2, WB2, WB3, WS-C, WS1, WS2b, WS2c, WS3 are VOID this boot and
  carry to the next boot after #175's AIM fix.
- **Observation, unexplained:** MOBREMOVE lines run through run 2, many naming uuids that appear nowhere else.

*Amended 2026-09-29, BEFORE ANY ROW WAS READ: G3 added* (Ben's "also accessible via" line), *and G2's Close note
records the seat's ruling. The stack was booted once at `eab57026` (R0 read by CC, PASS; Ben gave two items and
issued `/stop`); no row in this file was read on it, and that boot's R0 does not carry to the next boot.*

Every prediction below was written **before** any boot of this branch, and **this file is COMMITTED before the
boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once its row has been
read. **NO BOOT until the seat has diffed this file.** The stack boots ONCE, at its top, after NEXUS and LEGACY-A
are built and diffed (the seat, 2026-09-29).

**Readings count only if Ben's LOGIN appears in THIS boot's log.** A row whose prediction is a LOG LINE is PASS when
that line appears verbatim after the LOGIN.

```
ROWS     18   R0a R0b R0c R0d
              D1 D2 D3 D4 D5
              G1 G2 G3
              CM1 CM2 CM3 CM4 CM5 CM6
         ──
         18   = 4 R0 + 5 D + 3 G + 6 CM     git grep -c '^### R0\|^### D[0-9]\|^### G[0-9]\|^### CM' <ref> -- GATE-nexus-polish.md
```

**Plan:** `PLAN-nexus-polish.md` on `docs/batch-surveys` (`48053ddb`): Ben's words (*"Green names"*, *"Names are
good"*) and the seat's N1-N3, verbatim in its RULINGS. **This branch stacks on #169** (`feat/level-bonuses`), which
stacks on #168.

## GAME MODE

**SURVIVAL on every row except CM5, which is CREATIVE and says so.**

## THE ORDER IN THE ONE BOOT

1. R0 once, for the stack top.
2. #168's MC rows, then #169's rows (`GATE-level.md`), then **these**.
3. **No MC row and no LEVEL row is restated by this slice, and that was READ, not assumed.** MC1 reads the Build
   screen's class options (a content colour, untouched) and the cell's name (a pool's MiniMessage, untouched); MC2
   reads content names; MC7 reads WHICH fragments and aspects are offered, not the empty option's icon; MC8 reads
   `sent=`. `GATE-level.md` predicts no colour on any name. What this slice changes is listed in G1 and G2.

## Set-up

- **`/gamerule spawn_mobs false`** if not already set (26.1's name; `GATE-level.md`'s set-up says why).
- **This gate moves the level with `/rpg playerxp`.** If `GATE-level.md`'s last step (restoring Ben's XP) has
  already run, record the stats head's `Lifetime XP` again now; **this gate's last step restores it.**
- **Fire Melee cell**, as #168 leaves it. **`/rpg give emberblade`** and **`/rpg give boltor`** (two scored items for
  the anvil, D3).

---

## SELFTEST WITNESS AMENDMENT, 2026-10-05, BEFORE ANY BOOT (the seat's rulings c and d)

**No prediction moves.** `/rpg selftest <gate>` (#175, `GATE-selftest.md`) stages and drives this gate's CORE rows on
the shipped paths. Its lines are judged against the predictions below exactly as Ben's would be. **An AUTO reading is a
reading of the server, never of the client, and an AUTO half never makes a row PASS on its own**
(`.claude/rules/verification.md`, *AN AUTO READING IS A READING OF THE SERVER*). **The instrument never prints PASS or
FAIL**: a reading is recorded `AUTO PASS (selftest <sha>)` beside the row, quoting the real lines between that row's
`SELFTEST <gate> <row> START` and `DONE` (PLAYERHIT, DOTTICK, MOBSEED, CMD, REPLY, SLOT, PROBE). Staging is the gate's
own commands; where the scenario differs, the row says so. Every row not listed keeps its witness as written.

| row | witness now | the server's half, AUTO (`paper/src/main/resources/selftest/GATE-<gate>.yml#<row>`) | Ben's half |
|---|---|---|---|
| D1 | AUTO-PARTIAL | Crafting → Recipe Book → Back to crafting → Back to the Nexus by synthetic clicks into `onMenuClick`, each screen dumped | the button is where it says |
| G1 | AUTO-PARTIAL | every name's colour as built, the hub at level 5 and Settings | green reads green on his screen |
| G3 | AUTO-PARTIAL | every button's last lore line and its colour, the hub at level 5 and Crafting's Recipe Book | a glance |
| CM1 | AUTO | `performCommand` for each of the four, the screen's holder and title, then Back by a click | — |
| CM2 | AUTO | the six commands at level 1: `SCREEN none` after each, and the six replies with their colour | — |

**R0d is unchanged**: it is a boot log line, read as written, and no scenario drives it.

**The R0 moved again** (the seat's ruling c): the stack's one R0 is now `GATE-selftest.md`'s, on #175's head. The
SUPERSEDED note under this gate's R0 heading names `GATE-withered-shortbow.md`; read it as naming `GATE-selftest.md`.

---

## R0

> **INSTRUMENT NOTE, 2026-10-05, the seat's ruling 1, added before the stack-top boot. No prediction moves.** At the
> stack-top boot, this gate's R0a, R0b and R0c are SUPERSEDED by `GATE-withered-shortbow.md`'s R0 on `<sha>`; recorded
> SUPERSEDED, never PASS or FAIL. Intermediate facts in R0b/R0c are not read. *`<sha>` is the stack top's head as that
> R0a reads it in the boot: a commit beneath the top cannot name the top's hash, because the top is rebased onto this
> note.* **R0d is NOT superseded:** it is one of #170's merge bars, it reads a log line of the same boot (the ten
> `Screen command /…` lines), and it is read in that boot as written.

### R0a — the build line names this branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head, shortened. Not `-dirty`, not `unknown`, not #169's or #168's head | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **38cc9903, 2026-10-14: SUPERSEDED** -- GATE-selftest.md R0, read PASS at 38cc9903 (L42 `[Rpg] Build: 38cc9903`). Not read here. |

### R0b — the jar carries this slice and the two beneath it

| prediction | instrument | READING |
|---|---|---|
| PRESENT: paper `menu/NexusScreens`, paper `command/ScreenCommands`, #169's `menu/LevelMenu`, #168's core `combat/TracedHit`. ABSENT: the control | `GATE-level.md` R0b's scan, with these four entries | **38cc9903, 2026-10-14: SUPERSEDED** -- GATE-selftest.md R0b, read PASS at 38cc9903. The intermediate facts are not read. |

### R0c — content loads, and this slice moved no count

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes` — identical to #169's R0c prediction (no content file here). No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | **38cc9903, 2026-10-14: SUPERSEDED** -- GATE-selftest.md R0c, read PASS at 38cc9903 (L44). The intermediate facts are not read. |

### R0d — the ten screen commands registered, and what register() returned

| prediction | instrument | READING |
|---|---|---|
| **Ten lines**, in this order: `Screen command /level registered as […]`, then `/build`, `/gear`, `/vault`, `/anvil`, `/craft`, `/recipes`, `/enchanting`, `/grindstone`, `/settings`. **Each set contains its bare name** (`level`, …). **Whether it also holds a namespaced label (e.g. `rpg:level`) is NOT predicted**: `Commands.register`'s return is not documented in the jar (`PLAN-nexus-polish.md` §9.1). **A set WITHOUT the bare name is a clash, and stops the gate** | `Select-String run\logs\latest.log -Pattern 'Screen command /'` | **38cc9903, 2026-10-14: PASS** (read as written; a #170 merge bar). `Select-String 'Screen command /'` returns exactly 10 lines, L53-L62: `/level`, `/build`, `/gear`, `/vault`, `/anvil`, `/craft`, `/recipes`, `/enchanting`, `/grindstone`, `/settings`, in that order; every set holds its bare name (e.g. L53 `registered as [level, rpg:level]`, L62 `[settings, rpg:settings]`), so no clash. Each set also holds `rpg:<name>` (not predicted; recorded). They are plugin-enable lines, before the LOGIN at L80, in the same boot (one build line, L42). |

---

## THE FIVE DEFECTS (the seat's N1)

### D1 — the recipe browser keeps the way back to the Nexus (SURVIVAL) — witness: Ben

**Its only witness: mutation `MUTBROWSER-HUB` (the browser's Back drops the hub) reddens NO unit test.**

| prediction | READING |
|---|---|
| At level 3 or above: star → **Crafting** → **Recipe Book** → **Back to crafting**. The crafting screen that returns shows **`Back to the Nexus`** (arrow, green) and clicking it opens the **`Nexus Menu`**. (Before this slice, that crafting screen had no Back button) | **38cc9903, 2026-10-14: PASS.** AUTO (selftest, run 2), level 3 (L1440), the hub opened by `CMD /menu` (L1441), not the star: Crafting, then Recipe Book L1450 `RecipeBrowserMenu title="Recipes"` with L1452 `SLOT 48 CRAFTING_TABLE name="Back to crafting" [white, green]`; back on L1455 `CraftingMenu title="Crafting"`, L1458 `SLOT 48 ARROW name="Back to the Nexus" [white, green]`; its click (L1460) opens L1462 `NexusMenu title="Nexus Menu"`. Ben's half (the button is where it says): **PASS**, itemised (see Status). |

### D2 — "Empty this slot" names its kind, and is a bucket (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| star → **Build** → an **aspect** slot → pick **Keen Arc**; open the same slot again. Its **Empty this slot** option is an **empty bucket** (not a barrier), name green, lore **`Click to remove the aspect here.`** Do the same on a **fragment** slot (pick **Vigor**): the lore reads **`Click to remove the fragment here.`** Empty both slots afterwards | **38cc9903, 2026-10-14: NOT READ.** Not in the selftest, and not on Ben's itemised list; a star click logs nothing. |

### D3 — the anvil's confirm lore points at the right item (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| At level 7 or above: star → **Anvil**; put the emberblade in the left input and the Boltor in the middle input. The confirm button's lore reads **`Moves the sacrifice's score onto the item on the left.`**, then `The sacrifice is consumed.`. **Do not confirm**: take both items back out | **38cc9903, 2026-10-14: NOT READ.** Its staging (level 7 or more, the emberblade and the Boltor) never appears in the log. |

### D4 — the enchant table says "below" (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| At level 10 or above: star → **Enchanting**, with nothing placed. The table icon (top middle) reads **`Place a weapon, shield, armor piece or tool below`**, `to see the enchants it can carry.` | **38cc9903, 2026-10-14: NOT READ.** Level 10 was never staged. |

### D5 — "Active on this item." (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| On D4's screen, place the emberblade. Every **active** candidate's lore carries **`Active on this item.`** (if none is active, unlock and activate one, then read it; that costs XP, which the last step restores). **The other three F6 lines are NOT rows**: `One item at a time.` needs a stack of our gear, which the mint caps at 1; the overflow line needs more slots than the table shows; the empty-input line needs a candidate click with no input, and no candidate is painted then. They are read-only fixes | **38cc9903, 2026-10-14: NOT READ.** D4's screen was never staged. |

---

## GREEN NAMES AND THE BARRIERS (Ben's "Green names"; the seat's N2)

### G1 — names are green; state colours are kept (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `/rpg playerxp set @s 5 levels`, open the star. **Green:** Build, Equipment, Settings, Crafting (open at 5), the stats head's name, every `Back to …` button. **Kept:** a locked station's name is **dark gray** (Anvil, Enchanting, Grindstone, Vault at level 5); **Close** is **red**. In Settings, `Nexus Slot` and `Ability Stone Slot` are green; the ON/OFF toggles keep green/gray by state | **38cc9903, 2026-10-14: PASS, with one clause NOT PRESENTED.** AUTO (selftest, run 2), level 5 (L1478): the hub L1480 -- green Your Stats L1481, Build, Equipment, Crafting, Settings; dark gray Vault, Anvil, Enchanting, Grindstone; red Close L1489. Settings L1492 -- green Nexus Slot, Ability Stone Slot, Back to the Nexus L1497; red Close L1498; the toggles L1494 `"Nexus Star: ON" [white, green]` and L1496 `"Ability Stone: ON" [white, green]`. **Both toggles were ON, so "gray when OFF" was never on screen and is NOT READ** -- neither half covers it. Ben's half (green reads green): **PASS**, itemised (see Status). |

### G2 — one locked shape, on the hub and in the vault (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `/rpg playerxp set @s 20 levels`; star → **Vault**. The **page 2** button is a gray pane named **`Page 2`** in dark gray, lore **`Locked -- unlocks at level 25`**, **`You are level 20.`** — the hub's shape (`Locked -- unlocks at level N`, `You are level L.`). Click it: chat reads `Vault page 2 unlocks at level 25. You are level 20.` (`VaultPageGate.refusal`, unchanged) and page 1 stays open, its button green. **The locked STORAGE panes (renamed `Page N`, lore `Locked -- unlocks at level N`) are not a row**: the page buttons refuse a locked page, so the screen never shows one as the current page in ordinary play | **38cc9903, 2026-10-14: NOT READ.** Level 20 was never staged. |

**Barriers:** kept for "can't use / locked" and Ben's "nothing here" (NEXT.md, *TWO BARRIERS ON ONE SCREEN*). This
slice moves only **"Empty this slot"** (a bucket, D2) and **a bad fragment icon's fallback** (`AMETHYST_SHARD`; no
shipped fragment has a bad icon, so no row). **Close stays a barrier** — ruled by the seat, 2026-09-29: Ben's
2026-09-03 "nothing here" ruling governs, and N2 yields to it.

### G3 — every button with a command says so, last, in gray (SURVIVAL) — witness: Ben

**Added 2026-09-29, before any boot of this file's rows** (Ben: *"now we need a line in the lore that says "also
accessible via" and the slash command"*).

| prediction | READING |
|---|---|
| At level 5, open the star. The **LAST** lore line, in **gray**, of each button: **Build** `Also accessible via /build`; **Equipment** `/gear`; **Settings** `/settings`; **Crafting** (open) `/craft`; **Anvil**, **Enchanting**, **Grindstone** and **Vault** (all LOCKED at 5, below their three locked lines) `/anvil`, `/enchanting`, `/grindstone`, `/vault`; the **stats head** `Also accessible via /level`, below `Click for your level, its bonuses and unlocks.` Then Crafting → the **Recipe Book** button's last line: `Also accessible via /recipes` | **38cc9903, 2026-10-14: PASS.** AUTO (selftest, run 2), level 5 (L1505): all 9 hub buttons (L1509-L1518) end in `Also accessible via /<command>` [gray] -- the locked four below their three locked lines (e.g. L1512 Vault), the stats head below `Click for your level, its bonuses and unlocks.` (L1509); Crafting's Recipe Book L1522 ends `Also accessible via /recipes` [gray]. Ben's half (a glance): **PASS**, itemised (see Status). |

---

## THE COMMANDS (Ben: "Names are good")

### CM1 — the ungated four open at level 1, with a way back (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `/rpg playerxp set @s 1 levels`. **`/level`** opens `Player Level`; **`/build`** opens `Build`; **`/gear`** opens `Equipment`; **`/settings`** opens `Nexus Settings`. On each, **Back to the Nexus** opens the `Nexus Menu` | **38cc9903, 2026-10-14: PASS.** AUTO (selftest, run 2), level 1 (L1531): `/level` opens L1533 `LevelMenu "Player Level"`, `/build` L1581 `BuildMenu "Build"`, `/gear` L1609 `EquipmentMenu "Equipment"`, `/settings` L1636 `SettingsMenu "Nexus Settings"`; each Back to the Nexus opens `NexusMenu "Nexus Menu"` (L1567, L1596, L1623, L1644). |

### CM2 — the gated six refuse at level 1, in the hub's own words (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| At level 1, each replies in **gray** and opens nothing: **`/craft`** and **`/recipes`** → `Crafting unlocks at level 3. You are level 1. A crafting table in the world still works.`; **`/anvil`** → `Anvil unlocks at level 7. You are level 1. An anvil in the world still works.`; **`/enchanting`** → `Enchanting unlocks at level 10. You are level 1. An enchanting table in the world still works.`; **`/grindstone`** → `Grindstone unlocks at level 13. You are level 1. A grindstone in the world still works.`; **`/vault`** → `Vault unlocks at level 20. You are level 1. An ender chest opens this same vault.` | **38cc9903, 2026-10-14: PASS.** AUTO (selftest, run 2), level 1 (L1660): six refusals, each verbatim and [gray] -- L1663 `/craft` `Crafting unlocks at level 3. You are level 1. A crafting table in the world still works.`, L1666 `/recipes` the same, L1669 `/anvil` (level 7), L1673 `/enchanting` (level 10), L1676 `/grindstone` (level 13), L1679 `/vault` `Vault unlocks at level 20. You are level 1. An ender chest opens this same vault.`; 6/6 followed by `SCREEN none (open view type CRAFTING)` (L1664, L1667, L1670, L1674, L1677, L1680). |

### CM3 — at level 20 all six open, each with a way back (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `/rpg playerxp set @s 20 levels`. `/craft` → `Crafting`; `/recipes` → `Recipes`; `/anvil` → `Anvil`; `/enchanting` → `Enchantments`; `/grindstone` → `Grindstone`; `/vault` → `Vault`. Each has **Back to the Nexus**, which opens the `Nexus Menu`. From `/recipes`, **Back to crafting** then shows **Back to the Nexus** too (D1's fix, by a second route) | **38cc9903, 2026-10-14: NOT READ.** Level 20 was never staged. |

### CM4 — the boundary, one level each side (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `set @s 6 levels`: `/anvil` refuses (`… You are level 6. …`). `set @s 7 levels`: `/anvil` opens. The gate is the hub's: the same two levels on the hub's Anvil button refuse and open alike | **38cc9903, 2026-10-14: NOT READ.** No `set 6` or `set 7`, and no `/anvil`, in the log. |

### CM5 — creative opens, as /menu does (CREATIVE) — witness: Ben

| prediction | READING |
|---|---|
| `/gamemode creative`, level 20: `/anvil` opens the `Anvil`. Then `/gamemode survival` | **38cc9903, 2026-10-14: NOT READ.** Ben's creative window (L2102-L2104) was at level 5 with no `/anvil` -- not this row's staging. |

### CM6 — dead, and the console (SURVIVAL) — witness: Ben, and the console

| prediction | READING |
|---|---|
| **Dead:** `/kill @s`, and on the death screen type `/build`: the reply is **`You can't open that while dead.`** and no screen opens (whether the client lets a dead player send a command is client behaviour, **UNVERIFIED**; if it cannot, record that and the row is NOT READ, not FAIL). Respawn. **Console:** type `build` in the server console: **`Players only.`** | **38cc9903, 2026-10-14: NOT READ.** No `/kill @s` and no console `build` in the log. |

---

## LAST STEP — PUT BEN'S XP BACK

`/rpg playerxp set @s <the number recorded> xp`; the stats head's `Lifetime XP` reads it again.

---

## MUTATIONS, RECORDED BEFORE THE BOOT

Same instrument as `GATE-level.md`'s: a pristine copy, a marker grep and a line delta against it, the WHOLE suite
(`-pl paper -am`: 1539 / 92 / 1085), and a restore by `cp` checked with `cmp`, every one byte-identical.

| mutation | edit | marker / delta | RED |
|---|---|---|---|
| `MUTNAME-GRAY` | `MenuIcons.name` → gray | 1 / 1 | 1: `MenuIconsTest.buttonNamesAreGreenAndTheBackButtonIsOne` |
| `MUTSCREEN-GATE` | `/vault` loses its gate | 1 / 1 | 1: `ScreenPermissionsTest.eachCommandCarriesItsHubButtonsLevelGate` |
| `MUTSCREEN-YML` | `rpg.command.anvil` → `default: op` | 4 / 1 (three other `default: op` nodes exist; the delta is the reading) | 1: `ScreenPermissionsTest.everyScreenCommandsNodeIsDeclaredForEveryone` |
| `MUTBROWSER-HUB` | the browser's Back hands crafting `null` | 1 / 1 | **NONE** — D1 is its only witness |
| `MUTALSO-SETTINGS` | the Settings button's also-via line → a blank line (added with G3) | original gone 0 / 1 | 1: `ScreenPermissionsTest.everyScreensButtonCarriesItsAlsoViaLine` |
