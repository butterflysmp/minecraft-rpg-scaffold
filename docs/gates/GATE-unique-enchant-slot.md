# GATE — THE UNIQUE ENCHANT SLOT, BEN'S CASCADE, THE TABLE'S LAYOUT, AND THE INFO ITEM AT THE TOP RIGHT

**Status: NOT RUN.** Written 2026-11-23 on `feat/info-top-right` (PR 3), stacked on `feat/unique-enchant-slot` (#249),
stacked on `feat/selftest-row-case` (#248), on master `383ded19`, before any boot. **This file carries the ONE R0 and the
ONE run sheet for the three PRs** — and, since 2026-11-23 later the same day, for a FOURTH: `feat/boltor-sound-flint-text`
(the seat's brief "THE BOLTOR'S SHOT SOUND AND THE FLINT STAFF'S TEXT"), stacked on PR 3, whose rows are BX1 and BX2.

> **2026-11-23, RUN ON `e774d4b8`, UNDER `GATE-enchant-columns.md`'s R0 (which PASSED): R1, THE SETUP, US8, BX1 AND BX2
> ALL READ AS PREDICTED**, Ben's answer `yes`. The readings are beside each row below; the boot and the run are recorded
> in `GATE-enchant-columns.md`, *THE RUN OF `e774d4b8`*.

> **SUPERSEDED IN PART, 2026-11-23 (later the same day), BEFORE ANY BOOT: THE COLUMNS WERE RE-CUT** (the seat's brief
> "THREE ENCHANT COLUMNS' WORTH OF WORK"; Ben: slot 0 weapon-specific, slot 1 unique, slots 2-3 normal), **and the dev
> example enchant was deleted** (W5). So:
> - **THIS FILE'S R0 IS SUPERSEDED** by `GATE-enchant-columns.md`'s, which is now the ONE R0 for the whole stack from
>   #248 up. Its R0b readings below were taken against #250's and #251's jars and stay as the record of those jars.
> - **US1-US7 ARE RETIRED, NEVER RUN**, and removed from this gate's scenario: each named slot 0 as the unique slot, the
>   dev example, or three normal slots. Their purposes are read by `GATE-enchant-columns.md` EC1-EC6 (the columns and the
>   table). The rows below are KEPT as the record of what was predicted.
> - **R1 (PR 1's lower-case row), the setup, US8, BX1 and BX2 RIDE ALONG UNCHANGED**, run by `GATE-enchant-columns.md`'s
>   run sheet (its lines 2 and 3), read against the predictions in this file. **The run-length sums below include US1-US7
>   and are stale:** this gate's run is now setup 42, US8 20, BX1 12, BX2 70 (recount with the command below).

**NO BOOT YET, BY THE SEAT'S BRIEF: the boot waits until Ben's first unique enchants are stacked on top, so one boot reads
both.** When that branch exists, its gate rows join this sheet, R0 is re-predicted against its head, and only then: the
seat diffs every PR and this gate from origin, Ben says "ready" in his own words, and CC boots by
`.claude/rules/booting.md`. **NO MERGE without Ben's own words.**

**Game mode:** SURVIVAL, every row. **Every row is AUTO except BX2, which carries the gate's ONE ASK** (one question: the
Boltor's shot sound, which the server cannot witness). **No mob is summoned; nothing in the rows can hurt him**
(*SAFETY*).

**Ben's words for PR 4, verbatim (2026-11-23), recorded in `PLAN-weapon-tiers.md` 1.14:** "1. Add a sound to the Boltor's
shots. Tell me what the old repo uses and give me a recomendation" / told the old repo's sound and the seat's
recommendation to reuse it: "use the old one" / "2. A change to the lore of the Flint staff. Replace the current
description with this: "Prehistoric magicians learned you could create fire by slamming mana into flint."" / "3. Change
the Ability description to: "Hurls a fragment of burning mana""

**Ben's words this gate serves, verbatim (2026-11-23), recorded in `docs/plans/PLAN-unique-enchant-slot.md` §1** (and,
for PR 1, `PLAN-weapon-tiers.md` 1.13 and the boot of `ac46c838`, where his `vb3r2` started no run):
- "the first enchantment is a "Unique" enchantment in that the enchantments in that slot are tied to the element more"
- "make it possible that an item has none if there are none in the pool" / "A"
- "1. Instead of 2 normal enchantment slots, I want 3 in addition to the unique enchantment."
- "2. Move the Enchantment Table info item to the top right corner and move the bookshelf to the left of that. Make the
  rest of the nexus menu sub menus and our custom UIs consistent with that too"
- "3. The Slot for the item that is being enchanted should go where the Enchantment table info item was."
- "4. The Unique enchantments should go in collum 2 and the normal ones where they are today"
- "5. I want to allow a chance for 4 enchantments in each instead of 3"
- "6. I want to change the chances of multiple enchants: 1 enchant = 100%; 2 enchants = 70%; 3 enchants = 40%; 4
  enchants = 10%. I want this roll to be cascading like we did with the loot tables. (First it rolls a 10% chance, if
  that misses it rolls the 40%, if that misses it rolls the 70% and so on)"

**THE CONSEQUENCE, KNOWN TO BEN (his "A"): until he designs unique enchants, every newly rolled item has three usable
slots.** On this dev server the 16 kinetic weapons' slot 0 offers the dev-only example and nothing else.

## WHAT WAS READ OR EXECUTED, EACH MARKED

- **A. The charge (EXECUTED, jshell over `core/target/classes`, 2026-11-23):** `XpCurve.totalPoints(30, 0)` = **1395**;
  `EnchantCost.xpPoints(1, 0)` (unlock at I, bookshelf 0 from the Nexus) = **352**; `xpPoints(2, 0)` = **910**; 1395 − 352 =
  1043 → `XpCurve.levelFor` **26**, `progressFor` **0.5**.
- **B. The candidate lore lines (EXECUTED, jshell over `paper/target/classes`, `EnchantEffectLine.bare` at level 1):** on a
  MELEE weapon Sharpness `+5% damage, x1.05`, Power `inert: a Ranged enchant on a Melee weapon`, Attunement `inert: a Magic
  enchant on a Melee weapon`, Unbreaking `consumes durability on 50% of uses`, the dev example `+1% damage, x1.01`; on a
  MAGE weapon Attunement `+5% damage, x1.05`, Unbreaking the same. `EnchantEffectLine.of(dev, 1, MELEE)` =
  ` (+1% damage, x1.01)`.
- **C. The screens (READ, our source):** `cmd: "enchanting"`, `"craft"`, `"anvil"`, `"grindstone"`, `"level"` open the Nexus
  screens (`NexusScreens.Screen`); RPG level 13 opens every station (`NexusStationGate`: crafting 3, anvil 7, enchanting
  10, grindstone 13). `dump: menu` prints `SCREEN <menu> title=…` and one `SLOT <raw> <TYPE> name=… lore=…` per item, filler
  panes (blank-named glass) left out, colours by `SelfTestLines.styled` (`"text" [colour]`, `italic` only when set).
  `shift_click: "bottom <id>"` and `hold: <id>` match a WEAPON id only (`SelfTestDrivers`), so the armor rows empty the
  held slot and give into it, and `probe_held` witnesses it (`HELDID slot=<n> item=iron_chestplate`).
- **D. The wallet (READ):** the table charges VANILLA XP (`XpCurve` over `getLevel`/`getExp`), not Lifetime XP. **The run
  now restores it**: `SelfTestSnapshot` captures vanilla level and progress at RUN START and writes them back
  (`SelfTestVanillaXpRestoreTest`), and the RESTORED line ends ` vanilla_xp=<level>+<progress>`.
- **E. The single-row run (READ, `SelfTestService.start`, PR 1):** a row typed in any case is matched to the gate's
  spelling, and the run line is `SELFTEST RUN START <gate> <row as spelled> rows=1 …`.
- **F. The cascade's figures (EXECUTED, jshell, ±3σ of a binomial at n = 10000):** four p 0.100 → **[910, 1090]**; three
  p 0.360 → **[3457, 3744]**; two p 0.378 → **[3635, 3925]**; one p 0.162 → **[1510, 1730]**; and on a pool of two, where
  three and four cap to two, two p 0.838 → **[8270, 8490]**.
- **G. PR 4 (READ):** the Boltor's `right_click` now binds `on_cast: boltor_cast`, ONE sound step `item.crossbow.shoot`
  1.0 / 1.3 (the old repo's `Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.3f`; the key READ in the pinned jar's
  `SoundEvents.class`). `on_cast` plays inside `CastExecutor.commit()`, reached only after `AbilityService` returns
  Success: a press ON COOLDOWN returns `OnCooldown` first, and an EMPTY or RELOADING quiver returns
  `Quivers.resolveForShot`'s refusal in `WeaponFire.attempt` before `weaponService.fire`. **So the sound plays only when a
  shot leaves.** **NO SERVER WITNESS:** `PaperCombatWorld.present` plays a visual's sound with `world.playSound` and logs
  nothing (the Knell's `BEAM sound` lines are `MobBeamer`'s own), so only an ear reads it. **The empty press is not
  silent:** `QuiverNotice.empty` plays `block.dispenser.fail` to the holder with its action-bar notice. The ray ignores its
  caster (`CombatWorld.castRay(from, to, ignoreId)`).

## SAFETY: THE MOST HITS THE RUN CAN LAND ON HIM, TIMES THE BIGGEST, AGAINST HIS HEALTH

| run | what can hurt him | most hits x biggest hit |
|---|---|---|
| `unique-enchant-slot` (either form) | no mob, no cast, no projectile: commands, gives, clears, menu clicks and dumps only | **0 x 0 = 0** |
| BX2 (PR 4) | ONE Boltor shot, fired STRAIGHT UP (pitch −90) at open sky, no mob summoned; its ray ignores its caster (G). It can strike only a block, or a natural mob overhead, and a hit lands on that mob, never on him; the second press is refused (empty quiver) and fires nothing | **0 x 0 = 0** to him |
| either | a natural hostile already near him (this gate changes no time and no spawning) | as anywhere he stands; not made by the run |

**What the run clears, and the restore returns (READ, `SelfTestSnapshot`):** at the start of their rows it clears every
**iron sword**, **stick** (the Flint Staff's material), **iron chestplate** and (BX2) **crossbow**-material item (his own
Boltors, Breachers and Crossbows included) in his inventory, his own included (worn pieces too: `clear` takes armor
slots). BX2 also turns him to face straight up; the restore returns his position and facing. The restore returns his whole inventory, held slot, game mode, position, Lifetime
XP (so his RPG level, set to 13 here), **vanilla level and progress (set to 30 and 0 here)**, time, gamerules, mana and
health, on finish, quit, stop and server stop.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup** of `run/plugins/Rpg` and `run/world` in a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-unique-enchant-slot` (first free `-2`, `-3`); counts, `diff -rq lines: 0` for each,
   the `ls` of `world/dimensions`, the overworld's file count.
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND:** "booting now", and Ben told NOT to log in until told R0 passed.
6. `./scripts/dev-server.sh --refresh-content`. **Required deployed lines:** `content/enchant_candidates.yml` holds
   `    chance: 0.10`; `content/enchants/` holds **12** `.yml` files, one of them `dev_kinetic_example.yml`.
7. R0a to R0e on **this boot's** log, matched to this boot's own `Done` line.
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* `rpgfight` empty; the overworld's file count against step 2's; `All dimensions are saved`; no
   `java` process left.

## THE RUN SHEET — BEN TYPES EXACTLY THESE FOUR LINES

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.** Stand anywhere. **Keep about 4 empty inventory slots.**
1. `/gamemode survival`
2. `/rpg selftest unique-enchant-slot us8` — **ONCE, in lower case, exactly as written: it is PR 1's reading** (the row is
   `US8`). A few seconds: screens open and close by themselves.
3. `/rpg selftest unique-enchant-slot` — **ONCE.** About 15 seconds, then ONE question: the enchanting table, the
   crafting screen, the anvil, the grindstone and the Level screen open and close by themselves; then you are turned to
   face straight up and the Boltor fires once, and is pressed again on an empty quiver two seconds later. **Listen to
   both presses.** Then answer the question in chat with `yes` or `no`.
4. `/stop`

Nothing else. If a row stalls or aborts, do not intervene.

**The runs' lengths** (`wait:` summed per row; recount with `tr -d '\r' < <file> | awk '/^[A-Za-z0-9_-]+:/{row=$1}
/^  - wait:/{w[row]+=$3} END{for (r in w) print r, w[r]}'`): setup 42, US1 6, US2 6, US3 6, US4 12, US5 15, US6 11, US7 15,
US8 20, BX1 12, BX2 70: **215**, plus about 5 per row start (11: 55), no `kill` step: **the whole run about 270 ticks
(13.5 s) before the question**, and the question waits up to 1200 ticks for his answer; **`us8` alone about 72 (3.6 s)**
(setup 42, US8 20, two row starts).

## R0 — THE BUILD, FOR ALL FOUR PRs. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, the stack top's head as `git ls-remote` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries all three PRs

**Every value line is checked by `ValueLine`** (the value, then end of line or whitespace and `#`).

| prediction | instrument | READING |
|---|---|---|
| **PRESENT, 13:** `content/enchant_candidates.yml` `tiers:`, `  - candidates: 4`, `    chance: 0.10`, `    chance: 0.40`, `    chance: 0.70`, `    chance: 1.0`; `content/enchants/dev_kinetic_example.yml` `element: kinetic`, `class: universal`, `effect: damage`; `selftest/GATE-unique-enchant-slot.yml` `US1:` and `US8:`; `selftest/index.yml` `  - unique-enchant-slot`; `content/enchants/sharpness.yml` `class: melee` (a real enchant, unchanged). **PR 4, PRESENT, 8 more (21 in all):** `content/visuals/boltor_cast.yml` `    key: item.crossbow.shoot`, `    volume: 1.0`, `    pitch: 1.3`; `content/weapons/boltor.yml` `        visual_id: boltor_cast`; `content/weapons/flint_staff.yml` `  - "Prehistoric magicians learned you could"`, `  - "create fire by slamming mana into flint."`, `      - "Hurls a fragment of burning mana"`; `GATE-unique-enchant-slot.yml` `BX2:`. **The controls, ABSENT, 6:** `enchant_candidates.yml` `    chance: 0.1` (a prefix of 0.10) and `  - candidates: 5`; `dev_kinetic_example.yml` `element: fire`; `sharpness.yml` `element: kinetic` (no real enchant declares one); `GATE-unique-enchant-slot.yml` `US0:`; `index.yml` `  - unique-enchant-slots`. **PR 4's controls, ABSENT, 4 more (10 in all):** `flint_staff.yml` the old flavor line `  - "A shard of flint, lashed and lit."` and the old description `      - "Hurls a mote of burning flint."`, and Ben's line WITH a full stop `      - "Hurls a fragment of burning mana."`; `boltor_cast.yml` `    pitch: 1.0` | the scan below, `-JarPath run\plugins\rpg-0.1.0-SNAPSHOT.jar` | |

```powershell
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
$c = Bytes 'content/enchant_candidates.yml'
$d = Bytes 'content/enchants/dev_kinetic_example.yml'
$s = Bytes 'content/enchants/sharpness.yml'
$g = Bytes 'selftest/GATE-unique-enchant-slot.yml'
$i = Bytes 'selftest/index.yml'
ValueLine $c 'tiers:' 'enchant_candidates.yml'
ValueLine $c '  - candidates: 4' 'enchant_candidates.yml'
ValueLine $c '    chance: 0.10' 'enchant_candidates.yml'
ValueLine $c '    chance: 0.40' 'enchant_candidates.yml'
ValueLine $c '    chance: 0.70' 'enchant_candidates.yml'
ValueLine $c '    chance: 1.0' 'enchant_candidates.yml'
ValueLine $d 'element: kinetic' 'dev_kinetic_example.yml'
ValueLine $d 'class: universal' 'dev_kinetic_example.yml'
ValueLine $d 'effect: damage' 'dev_kinetic_example.yml'
ValueLine $g 'US1:' 'GATE-unique-enchant-slot.yml'
ValueLine $g 'US8:' 'GATE-unique-enchant-slot.yml'
ValueLine $i '  - unique-enchant-slot' 'index.yml'
ValueLine $s 'class: melee' 'sharpness.yml'
$bc = Bytes 'content/visuals/boltor_cast.yml'
$bw = Bytes 'content/weapons/boltor.yml'
$fs = Bytes 'content/weapons/flint_staff.yml'
ValueLine $bc '    key: item.crossbow.shoot' 'boltor_cast.yml'
ValueLine $bc '    volume: 1.0' 'boltor_cast.yml'
ValueLine $bc '    pitch: 1.3' 'boltor_cast.yml'
ValueLine $bw '        visual_id: boltor_cast' 'boltor.yml'
ValueLine $fs '  - "Prehistoric magicians learned you could"' 'flint_staff.yml'
ValueLine $fs '  - "create fire by slamming mana into flint."' 'flint_staff.yml'
ValueLine $fs '      - "Hurls a fragment of burning mana"' 'flint_staff.yml'
ValueLine $g 'BX2:' 'GATE-unique-enchant-slot.yml'
'--- controls'
ValueLine $c '    chance: 0.1' 'enchant_candidates.yml CONTROL-PREFIX'
ValueLine $c '  - candidates: 5' 'enchant_candidates.yml CONTROL-BOUND'
ValueLine $d 'element: fire' 'dev_kinetic_example.yml CONTROL-WRONG'
ValueLine $s 'element: kinetic' 'sharpness.yml CONTROL-NORMAL'
ValueLine $g 'US0:' 'GATE-unique-enchant-slot.yml CONTROL-NONE'
ValueLine $i '  - unique-enchant-slots' 'index.yml CONTROL-NONE'
ValueLine $fs '  - "A shard of flint, lashed and lit."' 'flint_staff.yml CONTROL-OLD-FLAVOR'
ValueLine $fs '      - "Hurls a mote of burning flint."' 'flint_staff.yml CONTROL-OLD-DESCRIPTION'
ValueLine $fs '      - "Hurls a fragment of burning mana."' 'flint_staff.yml CONTROL-FULL-STOP'
ValueLine $bc '    pitch: 1.0' 'boltor_cast.yml CONTROL-PITCH'
$zip.Dispose()
```

### R0c — every Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| Exactly one of each, up to the first `joined the game`: **`[Rpg] Loaded 11 abilities, 44 visuals, 5 statuses, 7 elements, 12 enchants, 3 pools, 6 fragments, 6 aspects, 24 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 7 recipes, 1 ores, 4 vein sizes`** (`GATE-boltor-recipe.md`'s reading on `ac46c838` with **11 -> 12 enchants**: the dev example loads on a dev server; and **43 -> 44 visuals**: PR 4's `boltor_cast`, `ls content/visuals/*.yml | wc -l` = 44; both derived); **`[Rpg] Loaded enchant candidates: 4 at 10.00% (truly 10.000%), 3 at 40.00% (truly 36.000%), 2 at 70.00% (truly 37.800%), 1 at 100.00% (truly 16.200%)`** (MEASURED by `EnchantCandidatesLoaderTest` on the shipped file); **NO `Dev-only enchants not loaded`** (a dev server loads it); `Custom recipes: 7 registered, 0 replaced, 0 refused, of 7 authored (7 minting gear)`, `Mint-on-craft: 38 indexed, 38 claiming, of 54 gear definitions` and `Convert-on-arrival: 9 materials {…}` word for word that boot's; the loot, fight, world-damage and smithing lines word for word that boot's. **No `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Recipe '`; no `[Rpg]` WARN beyond `volley_stone`'s known pair** | `Select-String run\logs\latest.log -Pattern 'Loaded\|Custom recipes\|Mint-on-craft\|Convert-on-arrival\|Dev-only\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded\|Recipe '''` | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 340 scenarios across 42 gates [melee-cell, …, boltor-recipe, unique-enchant-slot]`**. The controls: `330 scenarios across 41 gates` and `338 scenarios` are ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (`SelfTestScenariosTest` asserts 340): 42 gates, `^[A-Za-z0-9_-]+:` over the `GATE-*.yml` files **382**,
`^setup:` **42**, so **340**. Control: 371 + 9 (setup, US1-US8) + 2 (BX1, BX2) = 382.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, no `deleting`, no `orphan` | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | |

## THE ROWS

**Common to every row:** the stats bar's `REPLY actionbar "❤ …"` lines are EXPECTED and NOT COUNTED; each `clear`, `rpg
give` and vanilla `xp set` REPLY is recorded, not predicted. A vanilla `xp query` REPLY's wording is vanilla's
(UNVERIFIED); only its NUMBER is predicted. `REPLY` lines from `/rpg enchant show` are `[aqua]` for the header, `[gray]`
for the slot and `active:` lines and `[dark_gray]` for `raw:`; from `/rpg enchant pool` and `rollstats`, `[gray]`.

**HOW EACH PREDICTION STAYS EXACT WHEN THE ROLL IS RANDOM:** slot 0 is exact wherever its pool holds 0 or 1 enchant (the
cascade's count is capped at the pool: always 0 or always 1). Slots 1-3 are predicted OVER THE SET (each candidate in the
gear's normal pool, 1 to min(4, pool) of them, distinct). US5 STAGES its state with `/rpg enchant`, so every cell is
exact. US4 is a READING beside the true odds and the ±3σ band (F), not a pass or fail.

### R1 — PR 1: THE ROW IN LOWER CASE (`/rpg selftest unique-enchant-slot us8`, run sheet line 2) (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`SELFTEST RUN START unique-enchant-slot US8 rows=1 selftest=<sha> …`** (the row as the gate spells it, rows=1), its setup and **`START scenario=selftest/GATE-unique-enchant-slot.yml#US8`**, US8's lines as US8 predicts, `SELFTEST RUN FINISHED`. **No run at all, or `rows=` other than 1, is the defect PR 1 fixes** | E; `SelfTestRowMatchTest` (an unknown row's refusal is unit-tested there, not read at the boot) | **AS PREDICTED, the boot of `e774d4b8` (2026-11-23 18:43:15-19), typed `/rpg selftest unique-enchant-slot us8`:** `SELFTEST RUN START unique-enchant-slot US8 rows=1 selftest=e774d4b8 player=<him>`; `setup START …#setup`, `setup DONE ticks=47`; **`US8 START scenario=selftest/GATE-unique-enchant-slot.yml#US8`**, US8's lines as read under US8, `US8 DONE ticks=25`; `SELFTEST RUN FINISHED`; `RESTORED … health=120.000 vanilla_xp=190+0.236 …` |

### setup — RPG level 13, 30 vanilla levels at 0 points, the hand empty (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `DRIVE hold … item=empty`; `REPLY "Set 13 level(s) for <n> (now level 13, …)." [green]`; the two `xp set` REPLYs (recorded) | C, D | **AS PREDICTED, both runs (18:43:16-18 and 18:43:51-53):** `DRIVE hold … 8->2 item=empty` (then `2->2 item=empty`); `REPLY "Set 13 level(s) for BaronVonYeetus (now level 13, 0/2770 into it, 19980 lifetime)." [green]`; `REPLY "Set 30 experience levels on BaronVonYeetus" []`, `REPLY "Set 0 experience points on BaronVonYeetus" []` |

### US1 — A FRESH KINETIC WEAPON: SLOT 0 IS THE DEV UNIQUE ENCHANT ALONE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Iron Sword -- 4 slot(s)" [aqua]`; **`REPLY "  Slot 0: [dev_kinetic_example locked]" [gray]`** exactly; `Slot 1:`, `Slot 2:`, `Slot 3:` each 1 or 2 of `sharpness locked` / `unbreaking locked`, distinct, **never `dev_kinetic_example`**; no `active:` line; `REPLY "  raw: v1;dev_kinetic_example=0:-1;…" [dark_gray]`. Then **`REPLY "Pool: sharpness, unbreaking" [gray]`** and **`REPLY "Unique pool: dev_kinetic_example" [gray]`** | U3/V1 (`EnchantRoll.roll`); `UniqueSlotContentTest` | |

### US2 — A FIRE WEAPON: SLOT 0 EMPTY, THE OTHERS AS BEFORE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Flint Staff -- 4 slot(s)" [aqua]`; **`REPLY "  Slot 0: []" [gray]`** exactly; `Slot 1:`-`Slot 3:` each 1 or 2 of `attunement locked` / `unbreaking locked`, distinct; `REPLY "  raw: v1;:-1;…" [dark_gray]`. Then **`REPLY "Pool: attunement, unbreaking" [gray]`** (master's pool for the Flint Staff, `UniqueSlotContentTest`'s pin) and **`REPLY "Unique pool: " [gray]`** | Ben's A; U8 (an empty slot stored and shown) | |

### US3 — A PIECE OF ARMOR: SLOT 0 EMPTY (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`HELDID slot=<n> item=iron_chestplate`**; `REPLY "Iron Chestplate -- 4 slot(s)" [aqua]`; **`REPLY "  Slot 0: []" [gray]`**; `Slot 1:`-`Slot 3:` each 1 to 4 of `growth` / `mana_bank` / `protection` / `unbreaking` (all `locked`), distinct; `raw: v1;:-1;…`. Then **`REPLY "Pool: growth, mana_bank, protection, unbreaking" [gray]`** and **`REPLY "Unique pool: " [gray]`**. **`HELDID … item=` anything else means the give did not land in his hand: the row is VOID, not failed** | U5 (armor declares no element); C | |

### US4 — THE CASCADE OVER 10000 ROLLS, A READING (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **The Iron Sword:** `ROLLSTATS iron_sword element=kinetic rolls=10000 uniquePool=1 normalPool=2`; **`ROLLSTATS slot 0 (unique): 0=0 1=10000 2=0 3=0 4=0`** exactly; each of `slot 1:`-`slot 3:` reads **`0=0`, `3=0`, `4=0`** exactly and `1=` + `2=` = 10000, with `1=` beside **[1510, 1730]** and `2=` beside **[8270, 8490]**; `ROLLSTATS true odds (before the pool cap): 4=10.000% 3=36.000% 2=37.800% 1=16.200%`. **The Iron Chestplate:** `HELDID … item=iron_chestplate`; `ROLLSTATS iron_chestplate element=none rolls=10000 uniquePool=0 normalPool=4`; **`ROLLSTATS slot 0 (unique): 0=10000 1=0 2=0 3=0 4=0`** exactly; each of `slot 1:`-`slot 3:` reads `0=0` exactly and the four counts sum to 10000, recorded beside `1=` [1510, 1730], `2=` [3635, 3925], `3=` [3457, 3744], `4=` [910, 1090]; the same true-odds line. **The exact parts are predictions; the banded counts are a reading** | V3; F; `EnchantRollStatsTest` (the lines' shape) | |

### US5 — THE TABLE STAGED FULL: EVERY INDEX (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Before the table: `REPLY "  Slot 0: [dev_kinetic_example locked, sharpness locked, power locked, attunement locked]" [gray]` and `Slot 1:`, `Slot 2:`, `Slot 3:` each `[sharpness locked, power locked, attunement locked, unbreaking locked]`. The dump: **`SCREEN EnchantMenu title="Enchantments" [dark_gray]` and EXACTLY 22 `SLOT` lines:** **`SLOT 1 NAME_TAG name="Unique" [light_purple] lore=["Enchants tied to this item's element." [gray]]`**; **`SLOT 4 IRON_SWORD name="Iron Sword" …`** (the input); **`SLOT 7 BOOKSHELF name="Bookshelf Power 0/30" [dark_gray] lore=["0% off unlocks and level-ups." [dark_gray]; "Shelves in a ring around the table." [dark_gray]]`**; **`SLOT 8 ENCHANTING_TABLE name="Enchanting" [green] lore=["Click a candidate to unlock it," [gray]; "to make it active, or to level it." [gray]; "" []; "Swapping keeps the level you paid for." [dark_gray]]`**; the unique column **`SLOT 10 AMETHYST_SHARD name="Dev Kinetic Example" [dark_gray] lore=["+1% damage, x1.01" [gray]; "" []; "Locked. Click to unlock at I -- 352 XP." [dark_gray]]`**, **`SLOT 19 IRON_SWORD name="Sharpness" [dark_gray] lore=["+5% damage, x1.05" [gray]; …]`**, **`SLOT 28 BOW name="Power" [dark_gray] lore=["inert: a Ranged enchant on a Melee weapon" [gray]; …]`**, **`SLOT 37 BLAZE_ROD name="Attunement" [dark_gray] lore=["inert: a Magic enchant on a Melee weapon" [gray]; …]`**; then for EACH normal column, rows top to bottom, **`12 / 21 / 30 / 39`**, **`14 / 23 / 32 / 41`** and **`16 / 25 / 34 / 43`**: `IRON_SWORD "Sharpness"`, `BOW "Power"`, `BLAZE_ROD "Attunement"`, `ANVIL "Unbreaking"` (lore `"consumes durability on 50% of uses" [gray]`), every one `[dark_gray]` with third lore line `"Locked. Click to unlock at I -- 352 XP." [dark_gray]`; **`SLOT 48 ARROW name="Back to the Nexus" [green]`**; **`SLOT 49 BARRIER name="Close" [red]`**. **No `SLOT` at 0, 2, 3, 5, 6 or 9.** | Ben's 2, 3, 4 and 5; the seat's rows 2-5; A, B; `EnchantMenuLayoutTest` (every index) | |

### US6 — AN EMPTY UNIQUE SLOT IN THE TABLE, AND A CLICK ON IT CHANGES NOTHING (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The first dump: `SCREEN EnchantMenu …`; `SLOT 1 NAME_TAG name="Unique" …`; `SLOT 4 STICK name="Flint Staff" …`; `SLOT 7 BOOKSHELF …`; `SLOT 8 ENCHANTING_TABLE …` (as US5); **`SLOT 10 GRAY_DYE name="No unique enchant" [dark_gray] lore=["Nothing unique is tied to this item's element yet." [dark_gray]]`**; **no `SLOT` at 19, 28, 37**; `SLOT 12`, `14`, `16` each `BLAZE_ROD "Attunement"` (lore `"+5% damage, x1.05" [gray]`) or `ANVIL "Unbreaking"`, `[dark_gray]`, `"Locked. Click to unlock at I -- 352 XP."`; `21`/`23`/`25` present exactly where that slot rolled two (the other of the two); **no `SLOT` at 30, 32, 34, 39, 41, 43** (a pool of two); 48, 49. Then the queries: **levels 30, points 0**. The click: **`DRIVE click InventoryClickEvent LEFT PICKUP_ALL slot=10 "No unique enchant" [dark_gray]`**, and **NO `REPLY`** before the second dump (`applyCandidateClick` returns before any price). The second dump: **the same `SLOT` lines, line for line**. The queries again: **levels 30, points 0** | U8; Ben's A | |

### US7 — BUYING THE UNIQUE CANDIDATE CHARGES AND ACTIVATES IT AS A NORMAL ONE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Query: **levels 30**. The click: **`DRIVE click InventoryClickEvent LEFT PICKUP_ALL slot=10 "Dev Kinetic Example" [dark_gray]`**, then **`REPLY "Unlocked Dev Kinetic Example I and made it active for 352 XP. (+1% damage, x1.01)" [gray]`**. The dump: **`SLOT 10 AMETHYST_SHARD name="Dev Kinetic Example I" [green] lore=["+1% damage, x1.01" [gray]; "" []; "Active on this item." [green]; "Click to raise its level -- 910 XP." [gray]]`**, no `SLOT 19` (one candidate). Query: **levels 26** (1395 − 352 = 1043 points, A). After close: **`REPLY "  Slot 0: [*dev_kinetic_example I]" [gray]`** and **`REPLY "  active: dev_kinetic_example I (+1% damage, x1.01)" [gray]`**; `HELD iron_sword IRON_SWORD x1 name="Iron Sword" … lore=["Dev Kinetic Example I" [gray]; "" []; …]` (the enchant line first, as any active enchant's) | A, B; `EnchantMenu.applyCandidateClick` (one path for every candidate) | |

### US8 — THE INFO ITEM AT THE TOP RIGHT ON EVERY MOVED SCREEN, AND ONE UNMOVED (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Crafting:** `SCREEN CraftingMenu …` with **`SLOT 8 CRAFTING_TABLE name="Crafting" [green] lore=["Lay a recipe in the grid," [gray]; "or click a suggestion." [gray]]`** and **no `SLOT 4`**. **Anvil:** `SCREEN AnvilMenu …` with **`SLOT 8 ANVIL name="Anvil" [green] lore=["Move a gear score onto a better item." [gray]; "Same kind only, and the sacrifice must score higher." [gray]; …]`** and **no `SLOT 4`**. **Grindstone:** `SCREEN GrindstoneMenu …` with **`SLOT 8 GRINDSTONE name="Grindstone" [green] lore=["Place weapons, shields, armor or tools below." [gray]; …]`** and **no `SLOT 4`**. **THE CONTROL, Level (listed for Ben, NOT moved):** `SCREEN LevelMenu …` with **`SLOT 4 EXPERIENCE_BOTTLE name="Level 13" [green] …`** and **no `SLOT 8`** | PR 3 (`InfoTopRightTest`); the table below | **AS PREDICTED, both runs, line for line the same (18:43:18-19 and 18:43:53-54).** Every name prints in the dump's form `[white, <colour>] italic` (as `GATE-boltor-recipe.md` BR1 read it), its last colour the predicted one. **Crafting:** `SCREEN CraftingMenu title="Crafting" [dark_gray]`; `SLOT 8 CRAFTING_TABLE name="Crafting" [white, green] italic lore=["Lay a recipe in the grid," [gray]; "or click a suggestion." [gray]]`; then only `SLOT 26 KNOWLEDGE_BOOK "Recipe Book"`, `48`, `49`: **no `SLOT 4`**. **Anvil:** `SLOT 8 ANVIL name="Anvil" [white, green] italic lore=["Move a gear score onto a better item." [gray]; "Same kind only, and the sacrifice must score higher." [gray]; "" []; "Preview only -- nothing is spent or consumed yet." [dark_gray]]`; `24`, `31`, `48`, `49`: **no `SLOT 4`**. **Grindstone:** `SLOT 8 GRINDSTONE name="Grindstone" [white, green] italic lore=["Place weapons, shields, armor or tools below." [gray]; "Stripping clears every enchant they carry" [gray]; "and refunds 35% of the XP they cost." [gray]; "" []; "The slot roll is kept." [dark_gray]]`; `40`, `48`, `49`: **no `SLOT 4`**. **Level (the control):** `SCREEN LevelMenu title="Player Level" [dark_gray]`; **`SLOT 4 EXPERIENCE_BOTTLE name="Level 13" [yellow, green] italic lore=["Lifetime XP: 19,980" [gray]; …]`**; slots 10-16, 19-25, 28-34, 37-43, 48, 49, 53: **no `SLOT 8`** |

### BX1 — PR 4: THE FLINT STAFF'S TOOLTIP, BEN'S FLAVOR AND DESCRIPTION (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The `clear` and `rpg give` REPLYs (recorded); the score REPLY (recorded). **`HELD flint_staff STICK x1 name="Flint Staff" … lore=["Fire" [red]; "Gear Score: 100" [gray, aqua]; "" []; "Fire Bolt  Right-Click" [gold, yellow]; "Hurls a fragment of burning mana" [gray]; "Fire Damage: 20" [gray, red]; "Cooldown: 1.2s \| Mana Cost: 5" [dark_gray]; "" []; "Prehistoric magicians learned you could" [gray] italic; "create fire by slamming mana into flint." [gray] italic; "" []; "Common Magic Weapon" [white]]`**, every line. **No enchant line** (a fresh roll is all locked). **`"Hurls a fragment of burning mana."` (a full stop), `"Hurls a mote of burning flint."`, `"A shard of flint, lashed and lit."` or `"Cheap enough to lose. Hot enough to matter."` anywhere is a DEFECT** | `golden-lore.txt` `-- flint_staff` (the Gear Score line inserted after the element line at runtime, `WeaponLore.build`); `BoltorSoundFlintTextTest`; score 100 keeps the authored 20 (`GATE-breacher-rework.md` RW1's staging) | **AS PREDICTED (18:43:54-55):** `REPLY "Removed 1 item(s) from player BaronVonYeetus" []`; `REPLY "Given Flint Staff" [aqua, white]`; `REPLY "Set Gear Score: 100" [aqua, white]`; **`HELD flint_staff STICK x1 name="Flint Staff" [white] italic lore=["Fire" [red]; "Gear Score: 100" [gray, aqua]; "" []; "Fire Bolt  Right-Click" [gold, yellow]; "Hurls a fragment of burning mana" [gray]; "Fire Damage: 20" [gray, red]; "Cooldown: 1.2s \| Mana Cost: 5" [dark_gray]; "" []; "Prehistoric magicians learned you could" [gray] italic; "create fire by slamming mana into flint." [gray] italic; "" []; "Common Magic Weapon" [white]]`**, every line; no enchant line; none of the four DEFECT strings |

### BX2 — PR 4: THE BOLTOR'S SHOT SOUND: ONE SHOT, THEN ONE PRESS ON AN EMPTY QUIVER (SURVIVAL; THE GATE'S ONE ASK)

**Begins from a cleared cooldown:** it is the run's first Boltor press, 20 ticks after the turn (the cooldown is 16). The
second press is 40 ticks after `rpg rounds 0`, past the cooldown, so its refusal is the quiver's, not the cooldown's.

| prediction | stands on | READING |
|---|---|---|
| The turn; the first press: **no PLAYERHIT** (nothing to hit straight up; a block or a passing natural mob is recorded, not predicted); the dump: **`"Quiver: 7/8" [gray]`** in the Boltor's lore (one round left the quiver: the shot left). `REPLY "Rounds: 0/8 (…)"` (recorded). The second press: **`REPLY actionbar "Your quiver is empty -- left-click to reload." [gray]` exactly once**, no PLAYERHIT; the dump: **`"Quiver: 0/8" [gray]`**. **THE ASK, one line: the question and his answer verbatim; predicted `yes`** (the first press played `item.crossbow.shoot` at pitch 1.3; the second played no shot, only `QuiverNotice`'s `block.dispenser.fail` click). **AUTO-PARTIAL**: the quiver and refusal lines are the server's; the sound is his ear's alone | G; `boltor_cast.yml`; `QuiverNotice.empty` (READ); `GATE-breacher-rework.md` BR6R2 (the notice's reading) | **AS PREDICTED (18:43:55-44:05):** `REPLY "Given Boltor" [aqua, green]`; the turn `tp … 0 -90`; the first press `use … item=boltor yaw=0.000 pitch=-90.000`: **no PLAYERHIT**; `HELD boltor CROSSBOW … lore=[…; "Quiver: 7/8" [gray]; …]`; `REPLY "Rounds: 0/8 (boltor)" [green]`; the second press: **`REPLY actionbar "Your quiver is empty -- left-click to reload." [gray]` exactly once** (once in the whole log), no PLAYERHIT; `"Quiver: 0/8" [gray]`. **THE ASK:** `ASK 1 question="Two Boltor presses just happened. Did the FIRST play a crossbow shot sound and the SECOND play no shot sound (only the empty-quiver click)?" answer="yes" ticks=122`; **Ben, verbatim, in chat: `yes`** (`<BaronVonYeetus> yes`, 18:44:05), **as predicted** |

## PR 3: EVERY SCREEN, READ (our source at `feat/info-top-right`)

Every screen is a 54-slot chest, nine columns. "Row 1, column 9" is index 8.

| screen | info / hint item today (before PR 3) | what was at index 8 | PR 3 |
|---|---|---|---|
| Enchanting table | `INFO_SLOT` 4 | the bookshelf | **moved to 8** (PR 2, Ben's own words); bookshelf to 7 |
| Anvil | `INFO_SLOT` 4 ("Anvil") | filler | **moved to 8** |
| Grindstone | `INFO_SLOT` 4 ("Grindstone") | filler | **moved to 8** |
| Crafting | `INDICATOR_SLOT` 4 ("Crafting", the screen's icon) | filler | **moved to 8** |
| Recipe book | none | a recipe entry (entries are 0-44) | **left; for Ben** |
| Vault | none | its NEXT page arrow (`NEXT_SLOT`) | **left; for Ben** |
| Level | `SUMMARY_SLOT` 4: his level readout ("Level N"), not a hint | filler | **left; for Ben** (is a readout an info item?) |
| Build picker | `PICKER_TITLE_SLOT` 4: names what is being picked | filler | **left; for Ben** |
| Nexus hub | none (stats at 13) | filler | **left; for Ben** |
| Build | none (class 3, element 5) | filler | **left; for Ben** |
| Equipment | none (labels at 1 and 2) | filler | **left; for Ben** |
| Slot picker | none | filler | **left; for Ben** |
| Settings | none | filler | **left; for Ben** |

**Back (48) and Close (49) do not move on any screen** (`InfoTopRightTest`).

## UNREAD BY DESIGN

- **What he sees:** the icons, colours and layout as rendered; the `SLOT` and `HELD` lines are the server's view.
- **A real click's decode**: `click` is a synthetic `InventoryClickEvent` (`AN AUTO READING IS A READING OF THE SERVER`).
- **An item rolled under the old three-slot shape in the table**: none exists to stage without hand-writing its PDC;
  `OldThreeSlotItemTest` and `EnchantMenuLayoutTest` read it in the unit loop (V4).
- **A real server without the dev marker**: the dev example's absence is `EnchantLoaderTest`'s, not this boot's.
- **An unknown row's refusal** (PR 1): `SelfTestRowMatchTest`; the boot reads only the lower-case match.
- **The Boltor's sound by any instrument but his ear** (G: the server logs no visual's sound); its volume and pitch as
  he hears them; and that a press ON COOLDOWN plays nothing (READ in `AbilityService`, not staged here).
- **The Flint Staff's tooltip as rendered**, including whether a 40-character line looks right beside the others.
- **`/rpg enchant`'s new slot and candidate bounds (3 and 3)**: no row types a slot or candidate index above 2 except
  US5's `candidate 3 …`, which reads the slot bound; the candidate bound is unread.

## R0b, RUN BEFORE COMMIT

**Against `paper\target\rpg-0.1.0-SNAPSHOT.jar` (2,381,993 bytes), built by `./mvnw -B clean verify` from the working tree
of `feat/info-top-right` (on #249's head `7e273b5b`) with all of PR 3, this gate, its scenario and its index line
(2026-11-23, 01:14; the suite in the same build: core 2,348, storage 99, paper 1,829, 0 failures, 1 skipped):** the scan
above, extracted verbatim from this file and run with `powershell -File`, printed **13 PRESENT and 0 ABSENT** before
`'--- controls'` and **6 ABSENT, 0 PRESENT** after it, exactly as predicted. The boot's reading is taken again on the
deployed jar, and R0 is re-predicted when Ben's unique enchants are stacked on top.

**PR 4, the same scan widened (21 and 10), against `paper\target\rpg-0.1.0-SNAPSHOT.jar` (2,384,111 bytes), built by
`./mvnw -B clean verify` from the working tree of `feat/boltor-sound-flint-text` (on #250's head `1ee718b6`) with all of
PR 4 and these rows (2026-11-23; the suite in the same build: core 2,348, storage 99, paper 1,833, 0 failures, 1
skipped):** extracted verbatim and run with `powershell -File`, it printed **21 PRESENT and 0 ABSENT** before
`'--- controls'` and **10 ABSENT, 0 PRESENT** after it, exactly as predicted.
