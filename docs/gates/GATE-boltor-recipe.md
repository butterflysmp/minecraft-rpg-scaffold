# GATE — THE BOLTOR'S RECIPE (THE RANGER'S NETHERITE TIER), AND VB3R2

**Status: NOT RUN.** Written 2026-11-22 on `feat/boltor-recipe`, stacked on `feat/vb3r2` (#246), on master `d7c6d659`,
before any boot. **This file carries the ONE R0 and the ONE run sheet for the two PRs:** #246 (VB3R2 only; its row is in
`GATE-vanilla-bows.md`) and this one.

**NO BOOT until the seat has diffed both PRs AND this gate from origin, and Ben says "ready" in his own words**; then
only CC boots, by `.claude/rules/booting.md`. **NO MERGE without Ben's own words.**

**Game mode:** SURVIVAL, every row. **Every row is AUTO**: no prompt, no ASK, no crafting table to place (the crafts go
through the Recipe Book, out of his inventory). **No mob is summoned; nothing in the rows can hurt him** (*SAFETY*).

**Ben's words this gate serves, verbatim (2026-11-22), recorded in `PLAN-weapon-tiers.md` 1.13:**
- "The Boltor is Netherite tier. What is the crafting recipe for it from the old repo?"
- Shown the old recipe: "keep the old recipe"

## WHAT WAS READ, EACH MARKED

- **A. NO VANILLA CLASH (READ, the pinned 26.1.2 server jar, its 1,515 `data/minecraft/recipe/*.json` and 207 item
  tags, extracted to a scratch folder):** vanilla's `crossbow.json` is `"#&#"`, `"~$~"`, `" # "` with `#` stick, `&`
  iron_ingot, `~` string, `$` tripwire_hook: the Boltor's grid with an IRON ingot where it has netherite. Only three
  recipes name `netherite_ingot` (`netherite_block`, `netherite_ingot`, `netherite_ingot_from_netherite_block`) and three
  name `tripwire_hook` (`crossbow`, `trapped_chest`, `tripwire_hook`). The four item tags that hold either
  (`beacon_payment_items`, `netherite_tool_materials`, `repairs_netherite_armor`, `trim_materials`) are used only by
  SMITHING recipes, and no tag nests them (26 tag files nest other tags, so the search could find one). **No crafting
  recipe of vanilla's matches the Boltor's grid.** `BoltorRecipeTest` pins the one-cell difference against a copy of the
  jar's pattern.
- **B. WHICH WEAPON A CRAFT MINTS (READ, our source):** `InventoryCraft.commitCraft` asks the SERVER which recipe the grid
  matches (`Bukkit.getCraftingRecipe`) and refuses unless it equals the pinned key; `RecipeProbe.claimedBy` then looks a
  recipe of OURS up by its id FIRST (`forRecipe`) and only a recipe that is not ours (vanilla's `minecraft:crossbow`) by
  its result material (`forResult`, which `crossbow.yml`'s `craft_result: crossbow` claims). So the Breacher's grid
  (`rpg:breacher`) mints the Breacher, the Boltor's (`rpg:boltor`) the Boltor, vanilla's the Crossbow. **Nothing is
  contested:** `CraftResultIndex` counts a contest only on the material axis (two gears claiming one `craft_result`);
  the recipe axis is a separate map with no contest check, and only `crossbow.yml` claims `crossbow`. **Arrival cannot
  touch a crafted Breacher or Boltor:** `commitCraft` mints and stamps it before it reaches any inventory, so it arrives
  TAGGED, and `ArrivalClaims` leaves a tagged item alone (`LEAVE_TAGGED`; that the converter keys on that tag is
  INFERRED from `ArrivalClaims`, READ). **The Breacher's craft still mints a Breacher on master: no defect.**
- **C. THE BOOT LINES (MEASURED where a test can run it):** the Loaded line's recipes go **6 -> 7**; the registrar's
  `Custom recipes:` line **6 -> 7** in each count (derived: one more file, minting gear; the registrar needs a server);
  **`Mint-on-craft` and `Convert-on-arrival` do NOT move, MEASURED by `BootCountLinesTest`** over the real content with
  the new recipe in it: a recipe never enters the material map.
- **The route the rows drive (READ):** `/recipes` opens the Recipe Book (`RecipeBrowserMenu`): only what he can craft
  NOW from his inventory, WEAPON tier first (`SuggestionTier.WEAPON`, ordinal 0), 45 entries a page, each icon the
  minted item. A click crafts once through `InventoryCraft.craft` -> `commitCraft`, which logs **`CRAFT minted
  player=<n> weapon=<id> material=<m> score=<T>`**. `click` presses the first icon whose plain name STARTS WITH its
  argument (`SelfTestDrivers`). The Crafting screens unlock at **level 3** (`NexusStationGate.CRAFTING`): the setup
  stages it with `GATE-nexus-polish.md` D1's line. `dump: menu` prints `SCREEN <menu> title=…` and one `SLOT <raw>
  <TYPE> name=… lore=…` per item, filler panes left out.
- **A single row (READ, `SelfTestCommand` and `SelfTestService.start`):** `/rpg selftest <gate> <row>` runs that gate's
  setup and then the row alone.

## SAFETY: THE MOST HITS THE RUN CAN LAND ON HIM, TIMES THE BIGGEST, AGAINST HIS HEALTH

| run | what can hurt him | most hits x biggest hit |
|---|---|---|
| `vanilla-bows VB3R2` | its setup kills the natural hostiles within 64 blocks and sets night with spawns off (as on the boot of `52579e8a`); VB3R2 gives three weapons and runs three commands | **0 x 0 = 0** |
| `boltor-recipe` | no mob, no cast, no projectile: commands, gives, clears and menu clicks only | **0 x 0 = 0** |
| either | a natural hostile already near him (this gate changes no time and no spawning in `boltor-recipe`) | as anywhere he stands; not made by the run |

**What the runs clear, and the restore returns (READ, `GATE-vanilla-bows.md` *THE SNAPSHOT, THE RESTORE AND THE
MIRROR*):** `boltor-recipe` clears every **crossbow**-material item (his own Boltors, Breachers and Crossbows included),
**sticks, string, tripwire hooks, netherite ingots, iron ingots and gunpowder**, at the start of each row; `VB3R2` clears
**bows and crossbows**. Each run's restore returns his inventory, level, time, health and gamerules on finish, quit,
stop and server stop. **His level is set to 3 for `boltor-recipe` (1 for `vanilla-bows`) and put back.**

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup** of `run/plugins/Rpg` and `run/world` in a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-boltor-recipe` (first free `-2`, `-3`); counts, `diff -rq lines: 0` for each, the
   `ls` of `world/dimensions`, the overworld's file count.
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND:** "booting now", and Ben told NOT to log in until told R0 passed.
6. `./scripts/dev-server.sh --refresh-content`. **Required deployed lines:** `recipes/boltor.yml` holds `mints: boltor`;
   `recipes/` holds **7** `.yml` files; `weapons/` holds **24**.
7. R0a to R0e on **this boot's** log, matched to this boot's own `Done` line.
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* `rpgfight` empty; the overworld's file count against step 2's; `All dimensions are saved`; no
   `java` process left.

## THE RUN SHEET — BEN TYPES EXACTLY THESE FOUR LINES

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.** Stand anywhere. **Keep about 8 empty inventory slots** (each craft
   needs room for what it makes).
1. `/gamemode survival`
2. `/rpg selftest vanilla-bows VB3R2` — **ONCE.** It runs that gate's setup and the ONE row VB3R2 (READ), not the whole
   gate: a few seconds, about a minute at worst. (The whole `vanilla-bows` run would take up to 6 min 58 s; it is not
   asked for.)
3. `/rpg selftest boltor-recipe` — **ONCE.** About 5 seconds, all automatic: the Recipe Book opens and closes by itself.
4. `/stop`

Nothing else. If a row stalls or aborts, do not intervene.

**The runs' lengths** (`wait:` summed per row; recount with `tr -d '\r' < <file> | awk '/^[A-Za-z0-9_-]+:/{row=$1}
/^  - wait:/{w[row]+=$3} END{for (r in w) print r, w[r]}'`): **`vanilla-bows VB3R2`:** its setup 40 and VB3R2 12, plus
about 5 per row start (2: 10) and up to 40 per `kill` step (27, the setup's): **worst case 1,142 ticks (57 s)**, about 62
with nothing to kill. **`boltor-recipe`:** setup 20, BR1 12, BR2 10, BR3 10, BR4 13: **65**, plus about 5 per row start
(5: 25), no `kill` step: **worst case about 90 ticks (4.5 s).**

## R0 — THE BUILD, FOR BOTH PRs. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `feat/boltor-recipe`'s head as `git ls-remote` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries both PRs

**Every value line is checked by `ValueLine`** (the value, then end of line or whitespace and `#`).

| prediction | instrument | READING |
|---|---|---|
| **PRESENT, 15:** `recipes/boltor.yml` `mints: boltor`, `  - "SNS"`, `  - "GHG"`, `  - " S "`, `  S: stick`, `  N: netherite_ingot`, `  G: string`, `  H: tripwire_hook`; `weapons/boltor.yml` `rarity: uncommon` and `attack_damage: 19` (no value changed); `recipes/breacher.yml` `mints: breacher` (B's control, still in the jar); #246 `GATE-vanilla-bows.yml` `VB3R2:`; `GATE-boltor-recipe.yml` `BR1:` and `BR4:`; `index.yml` `  - boltor-recipe`. **The controls, ABSENT, 6:** `recipes/boltor.yml` `  N: iron_ingot` (vanilla's cell) and `mints: crossbow`; `weapons/boltor.yml` `rarity: common`, `attack_damage: 1` (a prefix of 19) and `craft_result: crossbow` (the Boltor claims no material); `GATE-vanilla-bows.yml` `VB3:` (renamed VB3R2) | the scan below, `-JarPath run\plugins\rpg-0.1.0-SNAPSHOT.jar` | |

```powershell
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
$r = Bytes 'content/recipes/boltor.yml'
$w = Bytes 'content/weapons/boltor.yml'
$vb = Bytes 'selftest/GATE-vanilla-bows.yml'
$br = Bytes 'selftest/GATE-boltor-recipe.yml'
ValueLine $r 'mints: boltor' 'recipes/boltor.yml'
ValueLine $r '  - "SNS"' 'recipes/boltor.yml'
ValueLine $r '  - "GHG"' 'recipes/boltor.yml'
ValueLine $r '  - " S "' 'recipes/boltor.yml'
ValueLine $r '  S: stick' 'recipes/boltor.yml'
ValueLine $r '  N: netherite_ingot' 'recipes/boltor.yml'
ValueLine $r '  G: string' 'recipes/boltor.yml'
ValueLine $r '  H: tripwire_hook' 'recipes/boltor.yml'
ValueLine $w 'rarity: uncommon' 'weapons/boltor.yml'
ValueLine $w 'attack_damage: 19' 'weapons/boltor.yml'
ValueLine (Bytes 'content/recipes/breacher.yml') 'mints: breacher' 'recipes/breacher.yml'
ValueLine $vb 'VB3R2:' 'GATE-vanilla-bows.yml'
ValueLine $br 'BR1:' 'GATE-boltor-recipe.yml'
ValueLine $br 'BR4:' 'GATE-boltor-recipe.yml'
ValueLine (Bytes 'selftest/index.yml') '  - boltor-recipe' 'index.yml'
'--- controls'
ValueLine $r '  N: iron_ingot' 'recipes/boltor.yml CONTROL-VANILLA'
ValueLine $r 'mints: crossbow' 'recipes/boltor.yml CONTROL-WRONG'
ValueLine $w 'rarity: common' 'weapons/boltor.yml CONTROL-UNCHANGED'
ValueLine $w 'attack_damage: 1' 'weapons/boltor.yml CONTROL-PREFIX'
ValueLine $w 'craft_result: crossbow' 'weapons/boltor.yml CONTROL-NONE'
ValueLine $vb 'VB3:' 'GATE-vanilla-bows.yml CONTROL-RENAMED'
$zip.Dispose()
```

### R0c — every Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| Exactly one of each, up to the first `joined the game`: **`[Rpg] Loaded 11 abilities, 43 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 24 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 7 recipes, 1 ores, 4 vein sizes`** (`GATE-vanilla-bows.md`'s reading on `52579e8a` with **6 -> 7 recipes**); **`[Rpg] Custom recipes: 7 registered, 0 replaced, 0 refused, of 7 authored (7 minting gear)`** (that boot's line with each 6 -> 7; derived); **`Mint-on-craft: 38 indexed, 38 claiming, of 54 gear definitions`** and **`Convert-on-arrival: 9 materials {bow=bow, copper_sword=copper_sword, crossbow=crossbow, diamond_sword=diamond_sword, golden_sword=golden_sword, iron_sword=iron_sword, netherite_sword=netherite_sword, stone_sword=stone_sword, wooden_sword=wooden_sword}`** (MEASURED by `BootCountLinesTest` with the new recipe in the content: unchanged); the loot, fight, world-damage and smithing lines word for word that boot's. **No `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Recipe '`; no `[Rpg]` WARN beyond `volley_stone`'s known pair** | `Select-String run\logs\latest.log -Pattern 'Loaded\|Custom recipes\|Mint-on-craft\|Convert-on-arrival\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded\|Recipe '''` | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 330 scenarios across 41 gates [melee-cell, …, vanilla-bows, boltor-recipe]`**. The control: `326 scenarios across 40 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (`SelfTestScenariosTest` asserts 330): 41 gates, `^[A-Za-z0-9_-]+:` over the `GATE-*.yml` files **371**,
`^setup:` **41**, so **330**. Control: 366 + 5 (setup, BR1-BR4) = 371; VB3 -> VB3R2 is a rename.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, no `deleting`, no `orphan` | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | |

## THE ROWS

**`/rpg selftest vanilla-bows VB3R2`:** the setup's rows and VB3R2 as written in `GATE-vanilla-bows.md` (its setup and
the VB3R2 section). Read there.

**`/rpg selftest boltor-recipe` — SURVIVAL, ALL AUTO.** Common to every row: the stats bar's `REPLY actionbar "❤ …"`
lines are EXPECTED and NOT COUNTED; each row's `clear` REPLYs ("Removed …" or "No items were found …") are his
inventory's and are recorded, not predicted. `<T>` is the score its CRAFT line prints.

### setup — his level at 3, the hand empty (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Set 3 level(s) for <n> (now level 3, …)." [green]`; `DRIVE hold … item=empty` | `NexusStationGate.CRAFTING` (3); `GATE-nexus-polish.md` D1 | |

### BR1 — THE BOLTOR'S GRID MINTS A BOLTOR (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Before: `INVENTORY boltor slots=[]`. The menu: **`SCREEN RecipeBrowserMenu …`**, with **a `SLOT <s> CROSSBOW name="Boltor" …` line and NO `SLOT` named `"Crossbow"` or `"Breacher"`** (no iron, no gunpowder). The click: **exactly one `CRAFT minted player=<n> weapon=boltor material=crossbow score=<T>`** and no other CRAFT line; `INVENTORY boltor slots=[<one slot>]`. **Held: `HELD boltor CROSSBOW x1 name="Boltor" [white, green] italic lore=["Kinetic" [white]; "Gear Score: <T>" [gray, aqua]; "Quiver: 8/8" [gray]; "" []; "Ranged Damage: 19" [gray, red]; "Attack Speed: 1.3" [gray, red]; "" []; "Eight bolts, then three seconds" [gray] italic; "of being a person holding a crossbow." [gray] italic; "" []; "Uncommon Ranged Weapon" [green]]`** | `golden-lore.txt` `-- boltor` (the Gear Score line inserted at runtime); `GATE-vanilla-bows.md` VM2's server reading of a given Boltor (Quiver 7/8 there, after one shot); `BoltorRecipeTest` | |

### BR2 — THE BREACHER'S GRID STILL MINTS A BREACHER (the control for B) (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The menu: **a `SLOT … CROSSBOW name="Breacher" …` line**, no `"Boltor"`, no `"Crossbow"` (no sticks, no tripwire hook). The click: **exactly one `CRAFT minted player=<n> weapon=breacher material=crossbow score=<T>`**; `INVENTORY breacher slots=[<one slot>]`. **`weapon=crossbow` here would be the claim order inverted: a DEFECT** | B; `BreacherContentTest.bensRecipeMintsIt`; `BoltorRecipeTest.eachGridMintsItsOwnWeapon` | |

### BR3 — VANILLA'S CROSSBOW GRID MINTS THE CROSSBOW; IT IS THE BOLTOR'S GRID WITH IRON, AND IT MINTS NO BOLTOR (SURVIVAL)

The two bullets of the seat's brief are ONE grid (READ: `crossbow.json`; `BoltorRecipeTest` pins it), so one row reads
both.

| prediction | stands on | READING |
|---|---|---|
| The menu: **a `SLOT … CROSSBOW name="Crossbow" …` line and NO `SLOT` named `"Boltor"`** (no netherite) and no `"Breacher"`. The click: **exactly one `CRAFT minted player=<n> weapon=crossbow material=crossbow score=<T>`**; `INVENTORY crossbow slots=[<one slot>]`; **`INVENTORY boltor slots=[]`** | the claim (`crossbow.yml` `craft_result: crossbow`); A | |

### BR4 — BOTH GRIDS AFFORDABLE AT ONCE: EACH CLICK MINTS ITS OWN (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The first menu: **both a `SLOT … name="Boltor"` and a `SLOT … name="Crossbow"` line.** Click `Crossbow`: **exactly one `CRAFT minted … weapon=crossbow …`**. The second menu: **the `"Boltor"` entry still offered** (stick 3, netherite, string 2, tripwire hook 1 remain) and no `"Crossbow"` (the iron is spent). Click `Boltor`: **exactly one `CRAFT minted … weapon=boltor …`**. Then `INVENTORY crossbow slots=[<one>]` and `INVENTORY boltor slots=[<one>]`. **Two CRAFT lines in the row, one of each weapon, in that order** | B (the key decides, not the inventory); `BoltorRecipeTest` | |

## UNREAD BY DESIGN

- **A hand-built grid.** The selftest cannot place items into grid cells (no verb does; `click` takes from a menu), so
  every craft here goes through the Recipe Book, which lays the matrix out from the recipe. The SERVER'S matcher is
  still in every craft (`commitCraft`'s match against the pin), but a grid laid out by hand, mirrored, or in a world
  crafting table's own 3x3 is not read. Shaped matching is the server's own (READ, `CraftingMenu`'s javadoc).
- **The Crafter block** (its own guard: a recipe of ours is refused there, READ `RpgListeners`).
- **What he sees:** the icons, the colours as rendered, the Recipe Book's layout. The `SLOT` and `HELD` lines are the
  server's view.
- **His other craftables:** the menu lists anything his own items can make; only the named entries are predicted.

## R0b, RUN BEFORE COMMIT

**Against `paper\target\rpg-0.1.0-SNAPSHOT.jar` (2,366,282 bytes), built by `./mvnw -B clean verify` from the working
tree of `feat/boltor-recipe` at `ec367430` plus this commit's gate, scenario, index line and test count (2026-11-22,
22:17; the suite in the same build: core 2,325, storage 99, paper 1,798, total 4,222, 0 failures, 1
skipped):** the scan above, extracted verbatim from this file and run with `powershell -File`, printed **15 PRESENT and
0 ABSENT** before `'--- controls'` and **6 ABSENT, 0 PRESENT** after it, exactly as predicted. The boot's reading is
taken again on the deployed jar.

## READINGS, 2026-11-22: THE BOOT OF `ac46c838`. R0 AS PREDICTED; EVERY ROW AS PREDICTED

Boot approved by the seat's message of 2026-11-22 (#246 and #247) on Ben's own "ready", by `.claude/rules/booting.md`.
Read from `run/logs/latest.log` of this boot (a copy kept in the session's scratchpad) and the jar in place. No
prediction above was edited.

### CARRIED STATE AND R0 (read before he logged in)

- **Before the boot:** `git ls-remote` master `d7c6d659`, #246 `c3a5d2a0`, #247 `ac46c838`, as the seat diffed; both
  PRs' two `build` checks `COMPLETED SUCCESS` on those heads; no `java` process; the backup in a NEW folder
  `rpg-run-backups/2026-11-22-pre-boltor-recipe` (`Rpg` 161 files / 26 dirs, `world` 193 / 32, `diff -rq` lines 0 each;
  `world/dimensions` = `minecraft`, `rpgfight`; overworld 106 files); `12:  dev-pack-on-join: true`; no fight folders;
  "booting now" in its own message. The last log carried `difficulty=NORMAL`.
- **The boot:** `Done (7.613s)` at 22:40:53. Deployed `recipes/boltor.yml` line 16 `mints: boltor`; 7 recipe files; 24
  weapon files.
- **R0a:** `[Rpg] Build: ac46c838`.
- **R0b**, on the deployed jar (2,366,291 bytes): **15 PRESENT, 0 ABSENT; the 6 controls ABSENT, 0 PRESENT.**
- **R0c:** `Loaded 11 abilities, 43 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 24
  weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 7 recipes, 1 ores, 4 vein sizes`;
  **`Custom recipes: 7 registered, 0 replaced, 0 refused, of 7 authored (7 minting gear)`** (the derived line, as
  predicted); **`Mint-on-craft: 38 indexed, 38 claiming, of 54 gear definitions`**; **`Convert-on-arrival: 9 materials
  {bow=bow, …, wooden_sword=wooden_sword}`** word for word; the loot, fight, world-damage and smithing lines identical to
  the boot of `52579e8a` (`diff`, 0 lines); `[Rpg]` WARN only `volley_stone`'s pair; no `Refusing`, `Skipping`,
  `SEVERE`, `Exception`, `Recipe '`.
- **R0d:** `Selftest: dev server, 330 scenarios across 41 gates [… vanilla-bows, boltor-recipe]`; `326` ABSENT.
- **R0e:** `FIGHTWORLD sweep path=…\run\world\dimensions\rpgfight folders=0`; no `deleting`, no `orphan`.
- **R0 read by 22:41:14; "R0 passed" sent in its own message; his login 22:45:23, after it.**

### HIS COMMANDS, AGAINST THE SHEET

`/weather clear` 22:45:29 (off the sheet); `/gamemode survival` 22:45:32; `/rpg selftest vanilla-bows vb3r2` 22:45:47
(lower case: no run started, off the sheet); `/rpg selftest vanilla-bows VB3R2` 22:45:53 (FINISHED 22:45:56; read in
`GATE-vanilla-bows.md`); `/rpg selftest boltor-recipe` 22:46:07 (`RUN START boltor-recipe rows=4 selftest=ac46c838`,
FINISHED 22:46:12); `/stop` 22:46:18.

### THE ROWS: ALL AS PREDICTED (no miss)

| row | READING |
|---|---|
| setup | `REPLY "Set 3 level(s) for BaronVonYeetus (now level 3, 0/1190 into it, 2090 lifetime)." [green]`; `DRIVE hold … item=empty` |
| BR1 | `INVENTORY boltor slots=[]`; **`SCREEN RecipeBrowserMenu title="Recipes" [dark_gray]`** with **`SLOT 0 CROSSBOW name="Boltor" [white, green] italic …`** and `SLOT 1 FISHING_ROD name="Fishing Rod"`, **no `"Crossbow"`, no `"Breacher"`**; the click: **exactly one `CRAFT minted player=BaronVonYeetus weapon=boltor material=crossbow score=100`** (T = 100); `INVENTORY boltor slots=[0x1]`; **`HELD boltor CROSSBOW x1 name="Boltor" [white, green] italic lore=["Kinetic" [white]; "Gear Score: 100" [gray, aqua]; "Quiver: 8/8" [gray]; "" []; "Ranged Damage: 19" [gray, red]; "Attack Speed: 1.3" [gray, red]; "" []; "Eight bolts, then three seconds" [gray] italic; "of being a person holding a crossbow." [gray] italic; "" []; "Uncommon Ranged Weapon" [green]]`**, every line as predicted |
| BR2 | **`SLOT 0 CROSSBOW name="Breacher" [white] italic …`** (with Shears, Heavy Weighted Pressure Plate, Iron Nugget), **no `"Boltor"`, no `"Crossbow"`**; **exactly one `CRAFT minted … weapon=breacher material=crossbow score=100`**; `INVENTORY breacher slots=[0x1]`. **The claim order holds on a server: the Breacher's grid mints the Breacher** |
| BR3 | **`SLOT 0 CROSSBOW name="Crossbow" [white] italic …`** (with Iron Shovel, Fishing Rod, Iron Nugget, Iron Spear), **no `"Boltor"`, no `"Breacher"`**; **exactly one `CRAFT minted … weapon=crossbow material=crossbow score=100`**; `INVENTORY crossbow slots=[0x1]`; **`INVENTORY boltor slots=[]`**. The iron variant offers no Boltor and mints the Crossbow |
| BR4 | the first menu: **`SLOT 1 CROSSBOW name="Crossbow"` and `SLOT 2 CROSSBOW name="Boltor"`** (with `SLOT 0 BOW name="Bow"`: 3 sticks and 3 of the 4 strings make one, not predicted, allowed); click Crossbow: **exactly one `CRAFT minted … weapon=crossbow …`**; the second menu: **`SLOT 0 CROSSBOW name="Boltor"`**, no `"Crossbow"`; click Boltor: **exactly one `CRAFT minted … weapon=boltor …`**; `INVENTORY crossbow slots=[1x1]`, `INVENTORY boltor slots=[0x1]`. **Two CRAFT lines, one of each, in that order** |
| the END | `SELFTEST RUN FINISHED`; `RESTORED … gamemode=SURVIVAL … difficulty=NORMAL health=182.000 …`; **`SELFTEST RUN HEALTH saved=182.000 customHP=182.000/350.000 wrote=182.000`** |

**Also recorded:** exactly 5 `CRAFT minted` lines in the whole log, all in these rows; no `NEXUSHEAL`, `NEXUSRESTORE` or
`ARRIVAL` line.

### AFTER HIS `/stop` (CHECKLIST STEP 9)

`rpgfight` holds 0 entries; the overworld holds **106** files, **the same file list as the backup's** (`diff` 0 lines);
`ThreadedAnvilChunkStorage: All dimensions are saved` at 22:46:18; **no `java.exe` left.**

### UNREAD, AND WHY

Only what *UNREAD BY DESIGN* lists: a grid placed by hand, mirrored, or in a world crafting table's own 3x3 (no verb
places items into grid cells); the Crafter block; what he sees.
