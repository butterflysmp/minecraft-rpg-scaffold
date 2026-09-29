# GATE — NEXUS polish: green names, the five defects, the wording defaults, and one command per screen

**Status: NOT RUN.**

Every prediction below was written **before** any boot of this branch, and **this file is COMMITTED before the
boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once its row has been
read. **NO BOOT until the seat has diffed this file.** The stack boots ONCE, at its top, after NEXUS and LEGACY-A
are built and diffed (the seat, 2026-09-29).

**Readings count only if Ben's LOGIN appears in THIS boot's log.** A row whose prediction is a LOG LINE is PASS when
that line appears verbatim after the LOGIN.

```
ROWS     17   R0a R0b R0c R0d
              D1 D2 D3 D4 D5
              G1 G2
              CM1 CM2 CM3 CM4 CM5 CM6
         ──
         17   = 4 R0 + 5 D + 2 G + 6 CM     git grep -c '^### R0\|^### D[0-9]\|^### G[0-9]\|^### CM' <ref> -- GATE-nexus-polish.md
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

## R0

### R0a — the build line names this branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head, shortened. Not `-dirty`, not `unknown`, not #169's or #168's head | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries this slice and the two beneath it

| prediction | instrument | READING |
|---|---|---|
| PRESENT: paper `menu/NexusScreens`, paper `command/ScreenCommands`, #169's `menu/LevelMenu`, #168's core `combat/TracedHit`. ABSENT: the control | `GATE-level.md` R0b's scan, with these four entries | |

### R0c — content loads, and this slice moved no count

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes` — identical to #169's R0c prediction (no content file here). No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | |

### R0d — the ten screen commands registered, and what register() returned

| prediction | instrument | READING |
|---|---|---|
| **Ten lines**, in this order: `Screen command /level registered as […]`, then `/build`, `/gear`, `/vault`, `/anvil`, `/craft`, `/recipes`, `/enchanting`, `/grindstone`, `/settings`. **Each set contains its bare name** (`level`, …). **Whether it also holds a namespaced label (e.g. `rpg:level`) is NOT predicted**: `Commands.register`'s return is not documented in the jar (`PLAN-nexus-polish.md` §9.1). **A set WITHOUT the bare name is a clash, and stops the gate** | `Select-String run\logs\latest.log -Pattern 'Screen command /'` | |

---

## THE FIVE DEFECTS (the seat's N1)

### D1 — the recipe browser keeps the way back to the Nexus (SURVIVAL) — witness: Ben

**Its only witness: mutation `MUTBROWSER-HUB` (the browser's Back drops the hub) reddens NO unit test.**

| prediction | READING |
|---|---|
| At level 3 or above: star → **Crafting** → **Recipe Book** → **Back to crafting**. The crafting screen that returns shows **`Back to the Nexus`** (arrow, green) and clicking it opens the **`Nexus Menu`**. (Before this slice, that crafting screen had no Back button) | |

### D2 — "Empty this slot" names its kind, and is a bucket (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| star → **Build** → an **aspect** slot → pick **Keen Arc**; open the same slot again. Its **Empty this slot** option is an **empty bucket** (not a barrier), name green, lore **`Click to remove the aspect here.`** Do the same on a **fragment** slot (pick **Vigor**): the lore reads **`Click to remove the fragment here.`** Empty both slots afterwards | |

### D3 — the anvil's confirm lore points at the right item (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| At level 7 or above: star → **Anvil**; put the emberblade in the left input and the Boltor in the middle input. The confirm button's lore reads **`Moves the sacrifice's score onto the item on the left.`**, then `The sacrifice is consumed.`. **Do not confirm**: take both items back out | |

### D4 — the enchant table says "below" (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| At level 10 or above: star → **Enchanting**, with nothing placed. The table icon (top middle) reads **`Place a weapon, shield, armor piece or tool below`**, `to see the enchants it can carry.` | |

### D5 — "Active on this item." (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| On D4's screen, place the emberblade. Every **active** candidate's lore carries **`Active on this item.`** (if none is active, unlock and activate one, then read it; that costs XP, which the last step restores). **The other three F6 lines are NOT rows**: `One item at a time.` needs a stack of our gear, which the mint caps at 1; the overflow line needs more slots than the table shows; the empty-input line needs a candidate click with no input, and no candidate is painted then. They are read-only fixes | |

---

## GREEN NAMES AND THE BARRIERS (Ben's "Green names"; the seat's N2)

### G1 — names are green; state colours are kept (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `/rpg playerxp set @s 5 levels`, open the star. **Green:** Build, Equipment, Settings, Crafting (open at 5), the stats head's name, every `Back to …` button. **Kept:** a locked station's name is **dark gray** (Anvil, Enchanting, Grindstone, Vault at level 5); **Close** is **red**. In Settings, `Nexus Slot` and `Ability Stone Slot` are green; the ON/OFF toggles keep green/gray by state | |

### G2 — one locked shape, on the hub and in the vault (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `/rpg playerxp set @s 20 levels`; star → **Vault**. The **page 2** button is a gray pane named **`Page 2`** in dark gray, lore **`Locked -- unlocks at level 25`**, **`You are level 20.`** — the hub's shape (`Locked -- unlocks at level N`, `You are level L.`). Click it: chat reads `Vault page 2 unlocks at level 25. You are level 20.` (`VaultPageGate.refusal`, unchanged) and page 1 stays open, its button green. **The locked STORAGE panes (renamed `Page N`, lore `Locked -- unlocks at level N`) are not a row**: the page buttons refuse a locked page, so the screen never shows one as the current page in ordinary play | |

**Barriers:** kept for "can't use / locked" and Ben's "nothing here" (NEXT.md, *TWO BARRIERS ON ONE SCREEN*). This
slice moves only **"Empty this slot"** (a bucket, D2) and **a bad fragment icon's fallback** (`AMETHYST_SHARD`; no
shipped fragment has a bad icon, so no row). **Close stays a barrier** — flagged to the seat, not decided here.

---

## THE COMMANDS (Ben: "Names are good")

### CM1 — the ungated four open at level 1, with a way back (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `/rpg playerxp set @s 1 levels`. **`/level`** opens `Player Level`; **`/build`** opens `Build`; **`/gear`** opens `Equipment`; **`/settings`** opens `Nexus Settings`. On each, **Back to the Nexus** opens the `Nexus Menu` | |

### CM2 — the gated six refuse at level 1, in the hub's own words (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| At level 1, each replies in **gray** and opens nothing: **`/craft`** and **`/recipes`** → `Crafting unlocks at level 3. You are level 1. A crafting table in the world still works.`; **`/anvil`** → `Anvil unlocks at level 7. You are level 1. An anvil in the world still works.`; **`/enchanting`** → `Enchanting unlocks at level 10. You are level 1. An enchanting table in the world still works.`; **`/grindstone`** → `Grindstone unlocks at level 13. You are level 1. A grindstone in the world still works.`; **`/vault`** → `Vault unlocks at level 20. You are level 1. An ender chest opens this same vault.` | |

### CM3 — at level 20 all six open, each with a way back (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `/rpg playerxp set @s 20 levels`. `/craft` → `Crafting`; `/recipes` → `Recipes`; `/anvil` → `Anvil`; `/enchanting` → `Enchantments`; `/grindstone` → `Grindstone`; `/vault` → `Vault`. Each has **Back to the Nexus**, which opens the `Nexus Menu`. From `/recipes`, **Back to crafting** then shows **Back to the Nexus** too (D1's fix, by a second route) | |

### CM4 — the boundary, one level each side (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `set @s 6 levels`: `/anvil` refuses (`… You are level 6. …`). `set @s 7 levels`: `/anvil` opens. The gate is the hub's: the same two levels on the hub's Anvil button refuse and open alike | |

### CM5 — creative opens, as /menu does (CREATIVE) — witness: Ben

| prediction | READING |
|---|---|
| `/gamemode creative`, level 20: `/anvil` opens the `Anvil`. Then `/gamemode survival` | |

### CM6 — dead, and the console (SURVIVAL) — witness: Ben, and the console

| prediction | READING |
|---|---|
| **Dead:** `/kill @s`, and on the death screen type `/build`: the reply is **`You can't open that while dead.`** and no screen opens (whether the client lets a dead player send a command is client behaviour, **UNVERIFIED**; if it cannot, record that and the row is NOT READ, not FAIL). Respawn. **Console:** type `build` in the server console: **`Players only.`** | |

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
