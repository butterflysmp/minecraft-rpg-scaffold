# GATE — THE ENCHANT COLUMNS RE-CUT, THE LIVING LANTERN'S HOMING SHOT, KINETIC TREMORS AND AIRBURST FLAMES

**Status: NOT RUN.** Written 2026-11-23 on `feat/enchant-columns-gate`, stacked on `feat/airburst-flames` (#255, PR E), on
`feat/kinetic-tremors` (#254, PR C), on `feat/living-lantern-homing` (#253, PR B), on `feat/enchant-columns` (#252, PR A),
on `feat/boltor-sound-flint-text` (#251), #250, #249, #248, on master `383ded19`. **PR D (Lapis Crystal) was STOPPED and
has no branch and no row** (`PLAN-enchant-columns.md` section 4).

**THIS FILE CARRIES THE ONE R0 FOR THE WHOLE STACK FROM #248 UP, AND THE ONE RUN SHEET.** It supersedes
`GATE-unique-enchant-slot.md`'s R0 and its rows US1-US7 (never run; retired from its scenario, which now holds setup, US8,
BX1 and BX2). R1 (PR 1's lower-case row), US8, BX1 and BX2 ride along unchanged and are read under that file's own
predictions. **NO BOOT until the seat has diffed every PR and this gate from origin and Ben has said "ready" in his own
words; CC boots by `.claude/rules/booting.md`. NO MERGE without Ben's own words.**

**Game mode:** SURVIVAL, every row. **Every row of this gate is AUTO** (the stack's one ASK is BX2's, in the other file).
**Every mob is a frozen NoAI zombie staged at vanilla max 200**; nothing here targets a player (*SAFETY*).

## BEN'S WORDS THIS GATE SERVES, VERBATIM (2026-11-23), RECORDED IN `docs/plans/PLAN-enchant-columns.md`

- "The first column will be weapon specific enchantments that fundamentally change how the weapons work. The second
  column will be our unique enchantments we just made and the 3rd and 4th columns will be the normal enchantments" /
  "1. Three back to 2 (A)" / "2. Yes" / "We can remove the placeholder names/labels above the columns."
- "Instead of the cone of fire, I want it to shoot a slow moving homing projectile similar to the old repo, only this
  time 1 shot instead of three." / "1. Damage 45 mana 12 cooldown 0.8s" / "2. 7 blocks a second is fine ... Reach
  increased to 28 blocks for the normal projectile. ... Homing within 7 is fine."
- "Kinetic Tremors  Any kinetic weapon from Melee, Ranger, and Mage.  multiple consecutive hits within 3 seconds emits a
  damaging shockwave" / "1. 3 hits rolling 2. Same target 3. The mob that was hit 4. No 5. The damage and size" / "1.
  Damage will scale with the gear score 2. Multiple pellets count so one shot triggering it is fine" / "there is a .25s
  cooldown on shockwaves"
- "It splits into three faster not homing projectiles after 12 blocks of travel." / "I want air burst flames to go to 14
  blocks instead then. ... those projectiles go an additional 14 blocks." / "3. 25/30/35 damage each burst" / "5.
  Doesn't split if it hits something first" / "The name of the enchantment is Airburst Flames"

## WHAT WAS READ OR EXECUTED, EACH MARKED

- **A. The PLAYERHIT line (READ, `TracedHit.line`):** `PLAYERHIT caster=<uuid> source=<source> target=<uuid> <type>
  element=<e> sent=<x.xxx> crit=<bool> triggerScore=<n>`. A weapon trigger's source is its ability id,
  `<weapon>/<input>` (READ: `GATE-breacher-rework.md`'s `living_lantern/right_click`). A shockwave's is
  `kinetic_tremors/shockwave` (`BehaviourHits.shockwaveSource`).
- **B. The SPLIT line (READ, `PaperCombatWorld.splitLine`, pinned by `SplitLineTest`):** `SPLIT caster=<uuid>
  source=<source> at=<x>,<y>,<z> bursts=<n> tick=<server tick>`, three places, while `/rpg mobtrace` (the run's `trace:
  on`) is on.
- **C. A mob's health (READ, `GATE-breacher-rework.md`, `MobScaling.maxHealth`):** vanilla max x 5 x score / 100, so a
  zombie summoned at `base:200.0` seeds at **1000** (INFERRED that the attribute lands before the seed; the row's own
  `MOBSEED … max=1000` line reads it). Nothing in any row deals 1000.
- **D. Every damage sum (READ, the Damage arm):** `sent = (amount x score/100) x enchant + class bonus + level bonus`,
  then `x charge x crit`. Here: score 100 (staged, `rpg gearscore set 100`); enchant 1.0 (no damage enchant active: each
  row's `rpg enchant show` reads the active set); class bonus 0 (his gear is not changed by the run; `GATE-breacher.md`
  BR1 read `Damage 0.00`, not re-read); level bonus 0 (level 1, staged from EC7); charge 1.0 (every press is a trigger);
  crit x2.0 when it lands (one roll per cast, READ). A shockwave carries every term OF THE HIT THAT FIRED IT, its crit
  included (`BehaviourHits`' javadoc). An airburst burst carries the shot's (its caster is the shot's).
- **E. The radius (READ, and it changed the code):** the server's `combatantsNear` is `World#getNearbyEntities`, a BOX;
  `BehaviourHits` cuts it to a SPHERE, feet to the struck mob's feet. Every staged distance below is 1 or more block from
  its boundary.
- **F. The flights (EXECUTED in core, the fake world):** `LivingLanternFlightTest` (a mob 10 out and 3 off the line
  takes one 45; at 27 hit, at 29 not) and `AirburstTest` (14 / 0.35 = 40.0 exactly, EXECUTED; the split is the 40th step,
  **39 ticks after the press**, at exactly 14.0 blocks; the centre burst lands on a mob in its line). **The fake world is
  flat: in the world the shot leaves his EYE and homing steers at a mob's FEET (READ, `ProjectileFlight.steer`)**, so a
  row whose shot homes is predicted exactly in WHAT it lands and is a READING in WHETHER it lands.
- **G. B6, what the enchants spawn (READ):** nothing. Tremors presents a visual; Airburst's bursts copy the lantern's Look
  (a trail, no item, no body); the lantern's shot has no body. No entity, so nothing to remove on quit, death, world
  change or stop, and nothing for the sweep.

## SAFETY: THE MOST HITS THE RUN CAN LAND ON HIM, TIMES THE BIGGEST, AGAINST HIS HEALTH

His health at level 1 is 100 + G (G: his worn gear's health, unchanged by the run).

| rows | what could reach him | most hits x biggest hit |
|---|---|---|
| EC0-EC6 | commands, gives, clears, menu dumps | **0 x 0 = 0** |
| EC7, EC8, EC14-EC16 | the lantern's shot and the bursts: `castRay(…, caster.id())` ignores him (READ); homing skips players (`ProjectileFlight.nearestMob`, READ) | **0 x 0 = 0** |
| EC9-EC13 | the Lapis beam and the Breacher's pellets ignore him (READ); a shockwave skips him and every player (`BehaviourHits`, READ, `BehaviourHitsTest`) | **0 x 0 = 0** |
| every row | the frozen zombies (NoAI: they never act); Ignite (fires only on a scorched death, and nothing dies: most a mob takes is EC11's 1000 - 320 - 20 = 660 left); a natural hostile (the setup kills them within 64 and sets `spawn_mobs false`, night 18000 restored at the end) | **0** from the run |

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup** of `run/plugins/Rpg` and `run/world` in a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-enchant-columns` (first free `-2`, `-3`); counts, `diff -rq lines: 0` for each,
   the `ls` of `world/dimensions`, the overworld's file count.
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND:** "booting now", and Ben told NOT to log in until told R0 passed.
6. `./scripts/dev-server.sh --refresh-content`. **Required deployed lines:** `content/enchants/` holds **13** `.yml`
   files, **none** starting `dev_`, among them `kinetic_tremors.yml` and `airburst_flames.yml`;
   `content/enchant_candidates.yml` holds `    chance: 0.10`; `content/weapons/living_lantern.yml` holds
   `      speed: 0.35`.
7. R0a to R0e on **this boot's** log, matched to this boot's own `Done` line.
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* `rpgfight` empty; the overworld's file count against step 2's; `All dimensions are saved`; no
   `java` process left.

## THE RUN SHEET — BEN TYPES EXACTLY THESE FIVE LINES

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.** Stand on flat open ground with **about 32 clear blocks to the
   east** (EC0 records the spot). **Keep about 4 empty inventory slots.**
1. `/gamemode survival`
2. `/rpg selftest unique-enchant-slot us8` — **ONCE, in lower case, exactly as written** (PR 1's reading, R1).
3. `/rpg selftest unique-enchant-slot` — **ONCE.** US8, BX1 and BX2: screens open and close, then the Boltor fires
   straight up once and is pressed on an empty quiver. **Listen to both presses**, then answer its ONE question in chat
   with `yes` or `no`.
4. `/rpg selftest enchant-columns` — **ONCE.** About 70 seconds: you are turned east, zombies appear and stand still,
   your weapons fire by themselves. Do nothing.
5. `/stop`

Nothing else. If a row stalls or aborts, do not intervene.

**This run's length** (`wait:` summed per row; recount with `tr -d '\r' < <file> | awk '/^[A-Za-z0-9_-]+:/{row=$1}
/^  - wait:/{w[row]+=$3} END{for (r in w) print r, w[r]}'`): setup 40, EC0 0, EC1-EC4 6 each, EC5 9, EC6 9, EC7 124,
EC8 140, EC9 108, EC10 143, EC11 64, EC12 104, EC13 106, EC14 124, EC15 60, EC16 202, EC17 0: **1257**, plus about 5 per
row start (19: 95): **about 1350 ticks (68 s)**, no question.

## R0 — THE BUILD, FOR THE WHOLE STACK. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `feat/enchant-columns-gate`'s head as `git ls-remote` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries every PR

**Every value line is checked by `ValueLine`** (the value, then end of line or whitespace and `#`). **PRESENT, 37
`ValueLine` calls** (recount: `awk '/^.--- controls.$/{exit} /^ValueLine/' <the block> | wc -l`): the cascade (6), PR 4's
`boltor_cast`, `boltor` and `flint_staff` lines (7), `sharpness.yml`'s `class: melee` (1), the old gate's scenario `US8:`
and `BX2:` (2) and index line (1), Kinetic Tremors (6), Airburst Flames (5), the lantern's shot (5), the shockwave's visual
(1), this gate's scenario `EC0:` and `EC17:` (2) and index line (1); **then one more predicted line, the dev example's
ABSENCE read as an absent ENTRY** (`$dev -eq $null`, printed `ABSENT-ENTRY`). **ABSENT, 9 controls:** a prefix of 0.10;
the cone and the 14 in the lantern; a retired row `US1:`; `hits: 30` (a prefix control on `hits: 3`); a normal enchant
declaring an element; `EC18:`; an index entry `enchant-column`; `weapon: lapis_staff` in Airburst.

> **CORRECTED BEFORE COMMIT, 2026-11-23: this paragraph first said "PRESENT, 40", a hand sum that was 2 high** (one slip
> is named: it counted PR 4's lines as 8, where the block has 7; the other was not traced). The pre-commit run below
> printed 37 and 1; the count was then taken from the block by the command above, which also prints 37. The prediction is
> corrected; no line of the scan moved.

```powershell
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
$c = Bytes 'content/enchant_candidates.yml'
$s = Bytes 'content/enchants/sharpness.yml'
$t = Bytes 'content/enchants/kinetic_tremors.yml'
$a = Bytes 'content/enchants/airburst_flames.yml'
$dev = Bytes 'content/enchants/dev_kinetic_example.yml'
$ll = Bytes 'content/weapons/living_lantern.yml'
$tw = Bytes 'content/visuals/tremor_wave.yml'
$bc = Bytes 'content/visuals/boltor_cast.yml'
$bw = Bytes 'content/weapons/boltor.yml'
$fs = Bytes 'content/weapons/flint_staff.yml'
$ug = Bytes 'selftest/GATE-unique-enchant-slot.yml'
$eg = Bytes 'selftest/GATE-enchant-columns.yml'
$i = Bytes 'selftest/index.yml'
ValueLine $c 'tiers:' 'enchant_candidates.yml'
ValueLine $c '  - candidates: 4' 'enchant_candidates.yml'
ValueLine $c '    chance: 0.10' 'enchant_candidates.yml'
ValueLine $c '    chance: 0.40' 'enchant_candidates.yml'
ValueLine $c '    chance: 0.70' 'enchant_candidates.yml'
ValueLine $c '    chance: 1.0' 'enchant_candidates.yml'
ValueLine $bc '    key: item.crossbow.shoot' 'boltor_cast.yml'
ValueLine $bc '    volume: 1.0' 'boltor_cast.yml'
ValueLine $bc '    pitch: 1.3' 'boltor_cast.yml'
ValueLine $bw '        visual_id: boltor_cast' 'boltor.yml'
ValueLine $fs '  - "Prehistoric magicians learned you could"' 'flint_staff.yml'
ValueLine $fs '  - "create fire by slamming mana into flint."' 'flint_staff.yml'
ValueLine $fs '      - "Hurls a fragment of burning mana"' 'flint_staff.yml'
ValueLine $s 'class: melee' 'sharpness.yml'
ValueLine $ug 'US8:' 'GATE-unique-enchant-slot.yml'
ValueLine $ug 'BX2:' 'GATE-unique-enchant-slot.yml'
ValueLine $i '  - unique-enchant-slot' 'index.yml'
ValueLine $t 'effect: shockwave' 'kinetic_tremors.yml'
ValueLine $t 'element: kinetic' 'kinetic_tremors.yml'
ValueLine $t 'class: universal' 'kinetic_tremors.yml'
ValueLine $t '  hits: 3' 'kinetic_tremors.yml'
ValueLine $t '  window_ticks: 60' 'kinetic_tremors.yml'
ValueLine $t '  cooldown_ticks: 5' 'kinetic_tremors.yml'
ValueLine $a 'effect: airburst' 'airburst_flames.yml'
ValueLine $a 'weapon: living_lantern' 'airburst_flames.yml'
ValueLine $a '  travel_blocks: 14' 'airburst_flames.yml'
ValueLine $a '  burst_travel_blocks: 14' 'airburst_flames.yml'
ValueLine $a '  damage_by_level: [25, 30, 35]' 'airburst_flames.yml'
ValueLine $ll '      type: projectile' 'living_lantern.yml'
ValueLine $ll '      speed: 0.35' 'living_lantern.yml'
ValueLine $ll '      max_lifetime_ticks: 80' 'living_lantern.yml'
ValueLine $ll '        search_radius: 7' 'living_lantern.yml'
ValueLine $ll '        amount: 45' 'living_lantern.yml'
ValueLine $tw '    key: entity.generic.explode' 'tremor_wave.yml'
ValueLine $eg 'EC0:' 'GATE-enchant-columns.yml'
ValueLine $eg 'EC17:' 'GATE-enchant-columns.yml'
ValueLine $i '  - enchant-columns' 'index.yml'
if ($dev -eq $null) { "ABSENT-ENTRY content/enchants/dev_kinetic_example.yml" } else { "PRESENT-ENTRY content/enchants/dev_kinetic_example.yml" }
'--- controls'
ValueLine $c '    chance: 0.1' 'enchant_candidates.yml CONTROL-PREFIX'
ValueLine $ll '      type: cone' 'living_lantern.yml CONTROL-THE-CONE'
ValueLine $ll '        amount: 14' 'living_lantern.yml CONTROL-THE-OLD-14'
ValueLine $ug 'US1:' 'GATE-unique-enchant-slot.yml CONTROL-RETIRED'
ValueLine $t '  hits: 30' 'kinetic_tremors.yml CONTROL-PREFIX'
ValueLine $s 'element: kinetic' 'sharpness.yml CONTROL-NORMAL'
ValueLine $eg 'EC18:' 'GATE-enchant-columns.yml CONTROL-NONE'
ValueLine $i '  - enchant-column' 'index.yml CONTROL-PREFIX'
ValueLine $a 'weapon: lapis_staff' 'airburst_flames.yml CONTROL-WRONG-WEAPON'
$zip.Dispose()
```

**The prediction, line for line:** before `'--- controls'`, **37 `PRESENT` lines, 0 `ABSENT`, and one `ABSENT-ENTRY`
line**; after it, **9 `ABSENT` lines, 0 `PRESENT`**.

### R0c — every Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| Exactly one of each, up to the first `joined the game`: **`[Rpg] Loaded 11 abilities, 45 visuals, 5 statuses, 7 elements, 13 enchants, 3 pools, 6 fragments, 6 aspects, 24 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 7 recipes, 1 ores, 4 vein sizes`** (`GATE-unique-enchant-slot.md`'s R0c line with **12 -> 13 enchants**: the dev example deleted, Kinetic Tremors and Airburst Flames added, `ls content/enchants/*.yml \| wc -l` = 13; and **44 -> 45 visuals**: `tremor_wave`, `ls content/visuals/*.yml \| wc -l` = 45; derived, not a reading); **`[Rpg] Loaded enchant candidates: 4 at 10.00% (truly 10.000%), 3 at 40.00% (truly 36.000%), 2 at 70.00% (truly 37.800%), 1 at 100.00% (truly 16.200%)`**; **NO `Dev-only enchants not loaded`** (no `dev_` file ships); **no `Content:` line naming `kinetic_tremors`** (its visual loads, `ContentValidator.validateBehaviourVisuals`) and none naming an enchant icon; `Custom recipes: 7 registered, 0 replaced, 0 refused, of 7 authored (7 minting gear)`, `Mint-on-craft: 38 indexed, 38 claiming, of 54 gear definitions` and `Convert-on-arrival: 9 materials {…}` word for word `ac46c838`'s; the loot, fight, world-damage and smithing lines word for word that boot's. **No `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Recipe '`; no `[Rpg]` WARN beyond `volley_stone`'s known pair** | `Select-String run\logs\latest.log -Pattern 'Loaded\|Custom recipes\|Mint-on-craft\|Convert-on-arrival\|Dev-only\|Content:\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded\|Recipe '''` | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 351 scenarios across 43 gates [melee-cell, …, unique-enchant-slot, enchant-columns]`**. The controls: `340 scenarios across 42 gates` and `358 scenarios` are ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (`SelfTestScenariosTest` asserts 351): `cat GATE-*.yml | tr -d '\r' | grep -cE '^[A-Za-z0-9_-]+:'` = **394**,
`^setup:` **43**, so **351**; 43 gate files, 43 index entries. The control: 382 - 7 (US1-US7 retired) + 19 (setup, EC0-EC17)
= 394.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED, AND NOTHING AN ENCHANT LEAVES

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, no `deleting`, no `orphan`. **Extended (the brief's "no orb or enchant entity left after the run and after /stop"):** EC17's three counts read 0 during the run (G: the enchants spawn no entity); after `/stop`, carried-state step 9's lines and nothing else: there is no enchant entity type for a sweep to find, and none is added to the sweep. **No orb exists: PR D was not built** | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'`; EC17 | |

## THE ROWS

**Common to every row:** the stats bar's `REPLY actionbar "❤ …"` lines are EXPECTED and NOT COUNTED; each `clear`, `rpg
give`, `rpg gearscore`, `rpg mana`, `rpg playerxp`, `rpg enchant candidate/level/active/clear` REPLY and each `MOBSEED`,
`MOBREMOVE` and `kill` line is RECORDED, not predicted (a `MOBSEED … max=1000` is C's witness). The lantern's fire hits
write `DOTTICK … status=scorch … cap=22.500` burns (cap 45 x 0.5; a burst's is its own x 0.5): RECORDED, NOT COUNTED. A
`PLAYERHIT` not predicted here is NOT AS PREDICTED. `REPLY` lines from `/rpg enchant show` are `[aqua]` for the header,
`[gray]` for the slot and `active:` lines, `[dark_gray]` for `raw:`; from `/rpg enchant pool`, `[gray]`.

**HOW A RANDOM ROLL STAYS EXACT:** slots 0 and 1 are exact wherever their pool holds 0 or 1 enchant (every gear here);
slots 2-3 are predicted OVER THE SET (each candidate in the gear's normal pool, 1 to min(4, pool), distinct).

### setup — night, no spawns, the trace on, RPG level 13 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Set 13 level(s) for <n> (now level 13, …)." [green]`; `PROBE self …` (recorded) | the old gate's setup | |

### EC0 — THE SPOT (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The counts describe the spot: AIR at feet and head over 1..32 east, 6 either side (**832** if all clear: 32 x 13 x 2), no AIR and no WATER in the floor. **A short count is not a failure; it voids the rows whose line it blocks** (EC8's 30 and EC14/EC16's 22 and 28) | the corridor | |

### EC1 — THE LAPIS STAFF'S COLUMNS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Lapis Staff -- 4 slot(s)" [aqua]`; **`REPLY "  Slot 0: []" [gray]`** exactly (Lapis Crystal was not built); **`REPLY "  Slot 1: [kinetic_tremors locked]" [gray]`** exactly; `Slot 2:`, `Slot 3:` each 1 or 2 of `attunement locked` / `unbreaking locked`, distinct; no `active:`; `REPLY "  raw: v1;:-1;kinetic_tremors=0:-1;…" [dark_gray]`. **`REPLY "Pool: attunement, unbreaking"`**, **`REPLY "Unique pool: kinetic_tremors"`**, **`REPLY "Weapon pool: "`** | W2; `UniqueSlotContentTest` | |

### EC2 — THE LIVING LANTERN'S COLUMNS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Living Lantern -- 4 slot(s)" [aqua]`; **`REPLY "  Slot 0: [airburst_flames locked]" [gray]`**; **`REPLY "  Slot 1: []" [gray]`**; `Slot 2:`/`Slot 3:` over `attunement`/`unbreaking`; `raw: v1;airburst_flames=0:-1;:-1;…`. **`Pool: attunement, unbreaking`**, **`Unique pool: `**, **`Weapon pool: airburst_flames`** | W1, W2 | |

### EC3 — AN IRON SWORD'S COLUMNS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Iron Sword -- 4 slot(s)" [aqua]`; **`Slot 0: []`**; **`Slot 1: [kinetic_tremors locked]`**; `Slot 2:`/`Slot 3:` over `sharpness`/`unbreaking`; `raw: v1;:-1;kinetic_tremors=0:-1;…`. **`Pool: sharpness, unbreaking`**, **`Unique pool: kinetic_tremors`**, **`Weapon pool: `** | W2 | |

### EC4 — A PIECE OF ARMOR'S COLUMNS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`HELDID slot=<n> item=iron_chestplate`** (anything else VOIDS the row); `REPLY "Iron Chestplate -- 4 slot(s)" [aqua]`; **`Slot 0: []`**, **`Slot 1: []`**; `Slot 2:`/`Slot 3:` each 1 to 4 of `growth`/`mana_bank`/`protection`/`unbreaking`, distinct; `raw: v1;:-1;:-1;…`. **`Pool: growth, mana_bank, protection, unbreaking`**, **`Unique pool: `**, **`Weapon pool: `** | W2; U5 | |

### EC5 — THE TABLE ON AN IRON SWORD: NO LABEL, THE WEAPON MARKER, TREMORS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `SCREEN EnchantMenu title="Enchantments" [dark_gray]`; **NO `SLOT 1`** (W3: the label removed), and none at 0, 2, 3, 5, 6, 9; `SLOT 4 IRON_SWORD name="Iron Sword" …`; `SLOT 7 BOOKSHELF …` and `SLOT 8 ENCHANTING_TABLE …` as `GATE-unique-enchant-slot.md` US5 predicted them; **`SLOT 10 GRAY_DYE name="No weapon enchant" [dark_gray] lore=["Nothing is tied to this weapon yet." [dark_gray]]`**; **`SLOT 12 PISTON name="Kinetic Tremors" [dark_gray] lore=["3 hits on one foe in 3s: a 10 damage shockwave, 3 blocks" [gray]; "" []; "Locked. Click to unlock at I -- 352 XP." [dark_gray]]`**; **no `SLOT` at 19 or 21**; `14`/`16` and, where that slot rolled two, `23`/`25`: `IRON_SWORD "Sharpness"` (lore `"+5% damage, x1.05" [gray]`) or `ANVIL "Unbreaking"`, `[dark_gray]`, third lore line `"Locked. Click to unlock at I -- 352 XP."`; no `SLOT` at 28-43; `SLOT 48 ARROW …`, `SLOT 49 BARRIER …` | W3; `EnchantMenuLayoutTest`; `KineticTremorsContentTest` (the line); the old gate's A (352) | |

### EC6 — THE TABLE ON A LIVING LANTERN: AIRBURST, THE UNIQUE MARKER (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `SCREEN EnchantMenu …`; **NO `SLOT 1`**; `SLOT 4 LANTERN name="Living Lantern" …`; **`SLOT 10 FIRE_CHARGE name="Airburst Flames" [dark_gray] lore=["splits after 14 blocks into 3 bursts of 25 damage" [gray]; "" []; "Locked. Click to unlock at I -- 352 XP." [dark_gray]]`**; **`SLOT 12 GRAY_DYE name="No unique enchant" [dark_gray] lore=["Nothing unique is tied to this item's element yet." [dark_gray]]`**; no `SLOT` at 19 or 21; `14`/`16` (and `23`/`25` where two rolled): `BLAZE_ROD "Attunement"` (lore `"+5% damage, x1.05" [gray]`) or `ANVIL "Unbreaking"`; 48, 49 | W3; `AirburstFlamesContentTest` (the line) | |

### EC7 — THE LANTERN'S SHOT: ONE HIT OF 45 ON A MOB OFF HIS AIM LINE (SURVIVAL)

**Begins from a cleared cooldown:** the lantern's first press of the run.

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Set 1 level(s) …"` (recorded); `rpg enchant show` prints **no `active:` line** (a fresh roll: every candidate locked, so enchant 1.0 and no behaviour). Probe L before: **`1000.000/1000.000`**. `DRIVE use … item=living_lantern`. **Exactly one PLAYERHIT `source=living_lantern/right_click target=<L> zombie element=fire sent=45.000 crit=false triggerScore=100`** (or `sent=90.000 crit=true`). **The sum:** `(45 x 100/100) x 1.0 + 0 + 0 = 45`, x charge 1.0, x crit 1.0 (2.0). Probe L at +60: **recorded** (45 and whatever burns of 22.5 have landed; the hit's tick is a reading). **The exact half is WHAT it lands; WHETHER it lands is a READING** (F: in the world it homes at the feet from his eye) | B; `LivingLanternFlightTest`; D | |

### EC8 — BEYOND 28 BLOCKS: NOTHING (SURVIVAL)

**Begins from a cleared cooldown:** 40 ticks after EC7's press (the cooldown is 16) plus EC8's own waits.

| prediction | stands on | READING |
|---|---|---|
| Probe F: **`1000.000/1000.000`** before and after. `DRIVE use …`; **NO PLAYERHIT** in the row (F stands 30 east, its near face 29.7 from his eye; the shot ends at 28.0 of travel, READ `ProjectileFlight`, `LivingLanternContentTest.theReachIsTwentyEightBlocksOfTravel`). **Exact**: homing only bends a path, never lengthens it | B | |

### EC9 — KINETIC TREMORS AT LEVEL I: THREE HITS, ONE SHOCKWAVE, ITS RADIUS (SURVIVAL)

**Begins from a cleared cooldown:** the Lapis Staff's first press of the run; then 22 between presses (its cooldown is 20).

| prediction | stands on | READING |
|---|---|---|
| `REPLY "  Slot 1: [*kinetic_tremors I]" [gray]` and **`REPLY "  active: kinetic_tremors I (3 hits on one foe in 3s: a 10 damage shockwave, 3 blocks)" [gray]`**. **Three PLAYERHITs `source=lapis_staff/right_click target=<A> zombie element=kinetic sent=21.000`** (each `42.000 crit=true` on its own crit): `(21 x 100/100) x 1.0 + 0 + 0`. **After the third, exactly TWO PLAYERHITs `source=kinetic_tremors/shockwave … element=kinetic`, one on A and one on B, each `sent=10.000`** (`20.000 crit=true` if the THIRD beam crit; both equal): `(10 x 100/100) x 1.0 + 0 + 0 = 10`, x 1.0, x that beam's crit. **None on C** (4 from A, outside 3). **No shockwave after the first or second beam.** Probes: A `1000 - 63 - 10 = 927.000` (more on crits, by the crits named), B **`990.000`** (or 980.000), C **`1000.000`** | C's rule; `BehaviourHitsTest`; E | |

### EC10 — TWO HITS, 61 TICKS, ONE HIT: NO SHOCKWAVE (SURVIVAL)

**Begins from a cleared cooldown:** 40 after EC9's last press.

| prediction | stands on | READING |
|---|---|---|
| **Three PLAYERHITs `source=lapis_staff/right_click target=<D>` sent=21.000** (or 42.000) and **NO `kinetic_tremors/shockwave` PLAYERHIT** (the third press is 83 and 61 ticks after the first two, both outside the 60: `ShockwaveStreaksTest.twoHitsThenSixtyOneTicksThenOneFiresNothing`). Probe D **`937.000`** (no crit) | C's rule | |

### EC11 — ONE BREACHER SHOT ON ONE MOB: EXACTLY ONE SHOCKWAVE (SURVIVAL)

**Begins from a cleared cooldown:** the Breacher's first press of the run.

| prediction | stands on | READING |
|---|---|---|
| `active: kinetic_tremors I (…)`. **Four PLAYERHITs `source=breacher/right_click target=<E> zombie element=kinetic sent=40.000 crit=false`** (all four `80.000 crit=true` on the crit: a spread crits as a unit). **Exactly ONE `kinetic_tremors/shockwave` PLAYERHIT, on E, `sent=10.000`** (`20.000` on the crit). The third pellet fires it; the fourth meets the 5-tick cooldown (`ShockwaveStreaksTest.fourPelletsInOneTickFireExactlyOne`). Probe E **`830.000`** (`1000 - 160 - 10`), or **`660.000`** on the crit | Ben's "Multiple pellets count"; `GATE-breacher-rework.md` RW2 (the four PLAYERHITs) | |

### EC12 — HITS SPLIT ACROSS TWO MOBS: NONE (SURVIVAL)

**Begins from a cleared cooldown:** the Lapis Staff, 40 after EC10's last press; 22 between presses.

| prediction | stands on | READING |
|---|---|---|
| Three `lapis_staff/right_click` PLAYERHITs: on G, on H, on G (each sent=21.000 or 42.000). **NO shockwave** (G holds two, H one). Probes G `958.000`, H `979.000` (no crit). **If `aim: g` lands a beam on H (or none), the row is VOID, not failed** | C's rule ("Same target") | |

### EC13 — KINETIC TREMORS AT LEVEL III: DAMAGE 20, RADIUS 5 (SURVIVAL)

**Begins from a cleared cooldown:** 40 after EC12's last press.

| prediction | stands on | READING |
|---|---|---|
| `active: kinetic_tremors III (3 hits on one foe in 3s: a 20 damage shockwave, 5 blocks)`. Three beams on A as EC9. **Exactly THREE shockwave PLAYERHITs, on A, B and C, each `sent=20.000`** (`40.000` on the third beam's crit): `(20 x 100/100) x 1.0 + 0 + 0 = 20`. **None on K** (6 from A). **Against EC9: C (4 from A) took nothing at I and 20 at III.** Probes: A `917.000`, B `980.000`, C `980.000`, K `1000.000` (no crit) | C's rule; the seat's placeholders 3/5 and 10/20 | |

### EC14 — AIRBURST FLAMES AT LEVEL II: NO 45, A SPLIT AT 14, THE CENTRE BURST'S 30 (SURVIVAL)

**Begins from a cleared cooldown:** the lantern was last pressed in EC8.

| prediction | stands on | READING |
|---|---|---|
| `REPLY "  Slot 0: [*airburst_flames II]" [gray]` and **`active: airburst_flames II (splits after 14 blocks into 3 bursts of 30 damage)`**. `PROBE self` gives his feet `<X, Y, Z>`. **Exactly one `SPLIT caster=<him> source=living_lantern/right_click at=<X+14.000>,<Y+1.620>,<Z> bursts=3 tick=<t>`** (his eye is his feet + 1.62, INFERRED standing; he faces due east at pitch 0; no mob within 7 of the shot before 14, so it never bent; gravity 0: EXACT). `t` is 39 ticks after the press (F; the press's tick is not logged, so t is RECORDED). **Exactly one PLAYERHIT, on M: `source=living_lantern/right_click … element=fire sent=30.000`** (`60.000 crit=true`): `(30 x 100/100) x 1.0 + 0 + 0`. **NO `sent=45.000`.** The side bursts pass M 1.70 off (8 x tan 12 degrees) and land nothing | E's rule; `AirburstTest` | |

### EC15 — A HIT BEFORE 14 BLOCKS: THE 45, NO SPLIT (SURVIVAL)

**Begins from a cleared cooldown:** 80 + 20 after EC14's press.

| prediction | stands on | READING |
|---|---|---|
| **One PLAYERHIT on N `sent=45.000`** (`90.000`), **NO `SPLIT`** line in the row. **WHETHER it lands is a READING** (N is within 7 from the muzzle, so the shot homes at its feet from his eye, F); if it misses N it flies on and splits at 14: that is NOT a defect of the rule, and the row is VOID | Ben's "Doesn't split if it hits something first" | |

### EC16 — LEVEL I AGAINST LEVEL III (SURVIVAL)

**Begins from a cleared cooldown:** 40 after EC15's press, then 82 between the two presses.

| prediction | stands on | READING |
|---|---|---|
| Two `SPLIT … bursts=3` lines, each at `<X+14.000>,<Y+1.620>,<Z>` as EC14. **The first press's PLAYERHIT on P `sent=25.000`, the second's `sent=35.000`** (each doubled on its own crit). Probe P: `1000 - 25 - 35 = 940.000` and the burns (recorded) | Ben's 25/30/35 | |

### EC17 — NOTHING AN ENCHANT SPAWNS IS LEFT (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`PROBE entities item_display … count=0`**, **`block_display … count=0`**, **`marker … count=0`** (G). Dropped items are not counted (a `/kill` of a zombie drops its loot) | B6; G | |

## UNREAD BY DESIGN

- **Lapis Crystal**: not built (`PLAN-enchant-columns.md` section 4).
- **What he sees and hears**: the shockwave's particles and sound, the bursts' trail, the table as rendered.
- **A real click**: no row buys an enchant; staging is by `/rpg enchant`. A purchase is priced by the one `EnchantCost`
  path (`GATE-unique-enchant-slot.md` US7, retired, predicted it; unit-tested).
- **The vanilla bow's stamp (`ShotStamp` v2) and a sword's swing with Tremors**: no row drives a vanilla shot or a basic
  swing here; `ShotStampTest` and `BehaviourHitsTest.threeStatReadingHitsFireOneToo` hold them in the unit loop.
- **A quit, a death or a world change forgetting the records**: `ShockwaveStreaksTest` holds the logic; the listeners are
  unmutated and unread.
- **The volley re-pricing of behaviours** (flagged for the seat): no row presses the Cursed Emerald.

## R0b, RUN BEFORE COMMIT

**Against `paper\target\rpg-0.1.0-SNAPSHOT.jar` (2,423,688 bytes, 2026-11-23 15:56), built by `./mvnw -B clean verify`
from the working tree of `feat/enchant-columns-gate` (on #255's head `6438988f`) with all of this gate, its scenario, its
index line and the retired rows (the suite in the same build: core 2,391, storage 99, paper 1,849, 0 failures, 1
skipped):** the scan above, extracted verbatim from this file (`awk` between the fences) and run with `powershell
-File`, printed **37 `PRESENT`, 0 `ABSENT` and 1 `ABSENT-ENTRY`** before `'--- controls'` and **9 `ABSENT`, 0 `PRESENT`**
after it: as the corrected prediction says (and NOT as its first draft's 40 said; see the note under R0b). The boot's
reading is taken again on the deployed jar.
