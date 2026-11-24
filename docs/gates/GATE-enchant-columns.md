# GATE — THE ENCHANT COLUMNS RE-CUT, THE LIVING LANTERN'S HOMING SHOT, KINETIC TREMORS AND AIRBURST FLAMES

**Status: NOT RUN.** Written 2026-11-23 on `feat/enchant-columns-gate`, stacked on `feat/airburst-flames` (#255, PR E), on
`feat/kinetic-tremors` (#254, PR C), on `feat/living-lantern-homing` (#253, PR B), on `feat/enchant-columns` (#252, PR A),
on `feat/boltor-sound-flint-text` (#251), #250, #249, #248, on master `383ded19`. **PR D (Lapis Crystal) was STOPPED and
has no branch and no row** (`PLAN-enchant-columns.md` section 4).

> **2026-11-23, RUN ONCE, ON `e774d4b8` (18:28 boot, 18:44:12-18:45:18 run): R0 PASSED; NINE ROWS DID NOT RUN AND NINE
> RAN VOID** (eight by a skipped row, EC12 by its own text), mostly because the run sheet asked for empty INVENTORY slots where the rows need empty HOTBAR slots. Read on their
> own condition and as predicted: setup, EC0's AIR counts, EC0R2's, EC1R2, EC2, EC17's counts. See *THE RUN OF
> `e774d4b8`*, under R0. The first boot that day (`004ba1cc-dirty`) is VOID (*THE VOID BOOT*).

> **2026-11-23, LATER: PR D WAS BUILT IN THREE** (the seat approved the split): D1 `feat/lapis-crystal-trigger` (#257),
> D2 `feat/lapis-crystal-orb` (#258), D3 `feat/lapis-crystal-shots` (#259), each on the one below, on #256; this gate's
> new rows (EC1R2, LC1-LC4, EC17 extended) and its R2 rows (R0bR2, R0cR2, R0dR2) are on `feat/lapis-crystal-gate`, on #259,
> which is now THE STACK TOP. Where this file says "PR D was STOPPED" it records the state before that; the R0 rows that
> name the older top (R0b, R0c, R0d) are superseded by their R2 rows and kept as the record of the jar they were run on.

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
- **D2. WHAT A SHOCKWAVE AND A CRYSTAL'S SHOT INHERIT (the seat's ruling 2, 2026-11-23: one rule for both).** Every
  term OF THE HIT (or the BEAM) THAT SET IT OFF: its gear score (x score / 100), its damage enchant (x the multiplier),
  the flat class bonus and level bonus (ADDED to every mob it strikes), its charge (x; 1 for every press here, all
  triggers) and its CRIT (x 2.0 when that hit crit). So every row that predicts one names: amount, score 100, enchant 1.0
  (no damage enchant active), class 0, level 0 (level 1), charge 1.0, and gives **the crit reading as the second allowed
  one**.
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
| LC1-LC4 | the throw lands no damage (an orb cast has no on_hit); the beam ignores him (READ, `castRay(…, caster.id())`); the crystal's shots skip him and every player (`CastExecutor.orbShots`, READ; `OrbBeamTest`); the orb's body is an item marker nobody can pick up (READ, `configureMarker`). He stands at the start of the strip; the orb rests 12 east of his eye | **0 x 0 = 0** |
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
   > **DATED NOTE, 2026-11-23 (the boot of `004ba1cc`): the stack top ships 14, not 13** (Lapis Crystal, D1-D3): the
   > required line is `content/enchants/` holds **14** `.yml` files, none `dev_`, among them `lapis_crystal.yml`.
7. R0a to R0e on **this boot's** log, matched to this boot's own `Done` line.
   > **DATED NOTE, 2026-11-23 (the second form): the next boot's R0 is R0aR3, R0bR3, R0cR3, R0dR3 and R0e**, under
   > *THE SECOND FORM*.
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* `rpgfight` empty; the overworld's file count against step 2's; `All dimensions are saved`; no
   `java` process left.

## THE RUN SHEET — BEN TYPES EXACTLY THESE FIVE LINES

> **SUPERSEDED, 2026-11-23, after the boot of `e774d4b8`:** the next boot's run sheet is *THE SECOND FORM*'s, under R0.
> This one is kept as the record of what that boot was given.

**IN PLAIN WORDS, BEFORE YOU TYPE ANYTHING (2026-11-23, corrected the same day for LC1's zombie F):** find **flat, open
ground**: a clear area **33 blocks long running EAST from where you stand, 6 blocks to your LEFT (north) and 10 blocks to
your RIGHT (south) as you face east**, with **nothing in it but air** at your feet and head height: no trees, walls,
water or mobs. *(This said "6 either side" until the seat found that LC1 puts a zombie 9 blocks to your right.)* The run turns you to face east and puts still
zombies in that strip, 1 to 30 blocks out; your weapons fire along it, and a floating lapis crystal is thrown 12 blocks
east. EC0 measures the strip; **a block in it spoils only the rows whose line it crosses**. The three selftest commands
are lines 2, 3 and 4 below, each typed once.

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.** Stand on that ground. **Keep about 4 empty inventory slots.**
1. `/gamemode survival`
2. `/rpg selftest unique-enchant-slot us8` — **ONCE, in lower case, exactly as written** (PR 1's reading, R1).
3. `/rpg selftest unique-enchant-slot` — **ONCE.** US8, BX1 and BX2: screens open and close, then the Boltor fires
   straight up once and is pressed on an empty quiver. **Listen to both presses**, then answer its ONE question in chat
   with `yes` or `no`.
4. `/rpg selftest enchant-columns` — **ONCE.** About 90 seconds: you are turned east, zombies appear and stand still,
   your weapons fire by themselves, and a lapis crystal is thrown and floats for 15 seconds. Do nothing.
5. `/stop`

Nothing else. If a row stalls or aborts, do not intervene.

**This run's length** (`wait:` summed per row; recount with `tr -d '\r' < <file> | awk '/^[A-Za-z0-9_-]+:/{row=$1}
/^  - wait:/{w[row]+=$3} END{for (r in w) print r, w[r]}'`): setup 40, EC0 0, EC1R2 and EC2-EC4 6 each, EC5 9, EC6 9,
EC7 124, EC8 140, EC9 108, EC10 143, EC11 64, EC12 104, EC13 106, EC14 124, EC15 60, EC16 202, LC1 84, LC2 10, LC3 54, LC4
310, EC17 0: **1715**, plus about 5 per row start (23: 115): **about 1830 ticks (92 s)**, no question. (Before D's rows:
1257 and about 68 s.)

## R0 — THE BUILD, FOR THE WHOLE STACK. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `feat/enchant-columns-gate`'s head as `git ls-remote` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **A MISS, ON THE VOID BOOT (2026-11-23 17:27:06):** `004ba1cc-dirty` (see *THE VOID BOOT*). Superseded by R0aR2 |

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
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, no `deleting`, no `orphan`. **Extended (the brief's "no orb or enchant entity left after the run and after /stop"):** EC17's three counts read 0 during the run (G: the enchants spawn no entity); after `/stop`, carried-state step 9's lines and nothing else: there is no enchant entity type for a sweep to find, and none is added to the sweep. **(Written before D: "No orb exists: PR D was not built". Since D2, EC17's extension and R0e's after-stop line read the orbs.)** | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'`; EC17 | **AS PREDICTED, the boot of `e774d4b8`:** one `[18:28:17] … FIGHTWORLD sweep path=C:\Users\Neb91\IdeaProjects\minecraft-rpg-scaffold\run\world\dimensions\rpgfight folders=0`, no `deleting`, no `orphan`. EC17's three counts 0. After `/stop`, step 9's lines and nothing else; **no `ORB` line in the whole log** (LC1 skipped, so no orb was ever thrown: the after-stop half is read on a run with no orb) |

## R0, SECOND FORM (2026-11-23, D1-D3 STACKED): R0bR2, R0cR2, R0dR2 SUPERSEDE R0b, R0c, R0d. R0a and R0e STAND

**R0a** names the NEW stack top (`feat/lapis-crystal-gate`'s head). **R0e** stands as written, with EC17's extension.

### R0bR2 — the jar carries every PR, D1-D3 included

**PRESENT: R0b's 37 `ValueLine` calls, and 12 more (49)**: `lapis_crystal.yml` `effect: grant_trigger`, `weapon:
lapis_staff`, `  input: left_click`, `    cooldown_ticks: 600`, `      amount: 30`, `      type: orb`, `      range: 12`,
`      life_ticks: 300`, `        percent_by_level: [20, 30, 40]`; `GATE-enchant-columns.yml` `EC1R2:`, `LC1:`, `LC4:`;
`living_lantern.yml` and `kinetic_tremors.yml` unchanged, so their lines stand; R0b's `EC0:`/`EC17:` stand. Then R0b's
`ABSENT-ENTRY` line. **ABSENT: R0b's 9 controls, and 4 more (13)**: `GATE-enchant-columns.yml` `EC1:` (renamed);
`lapis_crystal.yml` `      type: projectile` (D1's stub, gone), `    cooldown_ticks: 60` (a prefix of 600), `      range:
1` (a prefix of 12). **The scan is R0b's block with these lines added** (below the matching groups); recount: `awk
'/^.--- controls.$/{exit} /^ValueLine/' <the block> | wc -l` prints 49, and after the controls line 13 (counted from the block before the scan ran; a first draft of this paragraph said 51).

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
$lc = Bytes 'content/enchants/lapis_crystal.yml'
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
ValueLine $lc 'effect: grant_trigger' 'lapis_crystal.yml'
ValueLine $lc 'weapon: lapis_staff' 'lapis_crystal.yml'
ValueLine $lc '  input: left_click' 'lapis_crystal.yml'
ValueLine $lc '    cooldown_ticks: 600' 'lapis_crystal.yml'
ValueLine $lc '      amount: 30' 'lapis_crystal.yml'
ValueLine $lc '      type: orb' 'lapis_crystal.yml'
ValueLine $lc '      range: 12' 'lapis_crystal.yml'
ValueLine $lc '      life_ticks: 300' 'lapis_crystal.yml'
ValueLine $lc '        percent_by_level: [20, 30, 40]' 'lapis_crystal.yml'
ValueLine $ll '      type: projectile' 'living_lantern.yml'
ValueLine $ll '      speed: 0.35' 'living_lantern.yml'
ValueLine $ll '      max_lifetime_ticks: 80' 'living_lantern.yml'
ValueLine $ll '        search_radius: 7' 'living_lantern.yml'
ValueLine $ll '        amount: 45' 'living_lantern.yml'
ValueLine $tw '    key: entity.generic.explode' 'tremor_wave.yml'
ValueLine $eg 'EC0:' 'GATE-enchant-columns.yml'
ValueLine $eg 'EC1R2:' 'GATE-enchant-columns.yml'
ValueLine $eg 'LC1:' 'GATE-enchant-columns.yml'
ValueLine $eg 'LC4:' 'GATE-enchant-columns.yml'
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
ValueLine $eg 'EC1:' 'GATE-enchant-columns.yml CONTROL-RENAMED'
ValueLine $lc '      type: projectile' 'lapis_crystal.yml CONTROL-D1-STUB'
ValueLine $lc '    cooldown_ticks: 60' 'lapis_crystal.yml CONTROL-PREFIX'
ValueLine $lc '      range: 1' 'lapis_crystal.yml CONTROL-PREFIX'
$zip.Dispose()
```

**The prediction, line for line:** before `'--- controls'`, **49 `PRESENT`, 0 `ABSENT`, and one `ABSENT-ENTRY`**; after it,
**13 `ABSENT`, 0 `PRESENT`**. Run before the commit: see *R0bR2, RUN BEFORE COMMIT* at the end.

**READING, THE BOOT OF `e774d4b8` (2026-11-23): AS PREDICTED.** Against the deployed `run\plugins\rpg-0.1.0-SNAPSHOT.jar`
(2,453,563 bytes, sha256 `CE128574…9443527`), this block extracted verbatim from the committed file (`git show HEAD:…` and
the `awk` above; `cmp` IDENTICAL to the copy the void boot's re-read used; 62 `ValueLine` lines) and run with `powershell
-File`: **49 `PRESENT`, 0 `ABSENT`, 1 `ABSENT-ENTRY`**, then **13 `ABSENT`, 0 `PRESENT`**.

### R0cR2 — every Loaded line, and nothing refused

R0c's prediction with **13 -> 14 enchants** (Lapis Crystal; `ls content/enchants/*.yml | wc -l` = 14; derived) and every
other figure unchanged: `11 abilities` (a GRANTED ability is carried by its enchant, never registered in the ability
registry, READ `EnchantLoader.grantTrigger`), `45 visuals`. **No `Content:` line** naming `lapis_crystal`; **no
`Refusing`, `Skipping`** (a grant_trigger the loader refused would be a `Skipping malformed enchant 'lapis_crystal.yml'`
WARN); **no `ORB removed … cause=orphan`** during the enable (no body can survive a stop: none is saved).

**READING, THE BOOT OF `e774d4b8` (2026-11-23): AS PREDICTED**, by R0c's instrument: one each of `[Rpg] Loaded 11
abilities, 45 visuals, 5 statuses, 7 elements, 14 enchants, 3 pools, 6 fragments, 6 aspects, 24 weapons, 1 shields, 24
armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 7 recipes, 1 ores, 4 vein sizes`; `[Rpg] Loaded enchant
candidates: 4 at 10.00% (truly 10.000%), 3 at 40.00% (truly 36.000%), 2 at 70.00% (truly 37.800%), 1 at 100.00% (truly
16.200%)`; `Custom recipes: 7 registered, 0 replaced, 0 refused, of 7 authored (7 minting gear)`; `Mint-on-craft: 38
indexed, 38 claiming, of 54 gear definitions`; `Convert-on-arrival: 9 materials {bow=bow, …, wooden_sword=wooden_sword}`;
the loot, fight, world-damage and smithing lines; the only `[Rpg]` WARN `volley_stone`'s pair (`Content: weapon
'volley_stone' …` and `1 content problem(s)`); no `Dev-only`, `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Recipe '`;
no `Content:` line naming `lapis_crystal` or `kinetic_tremors`; no `ORB` line. The deployed folder: `content/enchants/`
14 `.yml`, 0 `dev_`, among them `airburst_flames.yml`, `kinetic_tremors.yml`, `lapis_crystal.yml`;
`enchant_candidates.yml:17:    chance: 0.10`; `living_lantern.yml:58:      speed: 0.35  # …`.

### R0dR2 — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 355 scenarios across 43 gates [melee-cell, …, unique-enchant-slot, enchant-columns]`**. The controls: `351 scenarios` and `340 scenarios` ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | **AS PREDICTED, the boot of `e774d4b8`:** exactly one, `[18:28:17] … [Rpg] Selftest: dev server, 355 scenarios across 43 gates [melee-cell, level, …, boltor-recipe, unique-enchant-slot, enchant-columns]`; `351` and `340` absent |

**RECOUNTED** (`SelfTestScenariosTest` asserts 355): `cat GATE-*.yml | tr -d '\r' | grep -cE '^[A-Za-z0-9_-]+:'` = **398**,
`^setup:` **43**, so **355**. The control: 394 + 4 (LC1-LC4) = 398 (EC1 -> EC1R2 is a rename).

### THE VOID BOOT OF `004ba1cc-dirty`, 2026-11-23: R0a MISSED, NO ROW RAN, NOTHING FROM IT CARRIES

**Booted** by `./scripts/dev-server.sh --refresh-content` on `feat/lapis-crystal-gate` at `004ba1cc`; the deployed jar
`run/plugins/rpg-0.1.0-SNAPSHOT.jar`, 2,453,564 bytes, 17:26:59, sha256 `706c098f…41cae8d0`; this boot's `Done (6.991s)`
at 17:27:07.

**R0a: A MISS.** `[17:27:06] [Server thread/INFO]: [Rpg] Build: 004ba1cc-dirty`; R0a predicts no `-dirty`. **The cause:
one uncommitted change in the working tree, carried-state step 6's dated note (14, not 13) in this file, written at the
seat's instruction, which did not say to commit it before the boot (the omission is the seat's, in its own words).**
`dev-server.sh` appends `-dirty` when `git diff --quiet HEAD` fails. **The boot is VOID.**

**The other R0 rows of that boot, recorded as THAT BOOT'S and NOT carried to the next.** Re-read after the stop from the
same `run/logs/latest.log`, and R0bR2 from a copy of the deployed jar (the sha256 above), the block extracted verbatim and
run with `powershell -File`: **R0bR2** 49 `PRESENT`, 0 `ABSENT`, 1 `ABSENT-ENTRY`, then 13 `ABSENT`, 0 `PRESENT`.
**R0cR2** `Loaded 11 abilities, 45 visuals, 5 statuses, 7 elements, 14 enchants, …` and the candidates line as predicted;
`volley_stone`'s pair the only `[Rpg]` WARN; no `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Dev-only`; no `ORB` line.
**R0dR2** one `Selftest: dev server, 355 scenarios across 43 gates [melee-cell, …, unique-enchant-slot,
enchant-columns]`. **R0e** one `FIGHTWORLD sweep path=…\run\world\dimensions\rpgfight folders=0`, no `deleting`, no
`orphan`.

**Ben was told NOT to log in, and no row ran.** CC could not type into the server console, so Ben logged in ONLY to stop
it. The log: `BaronVonYeetus joined the game` 17:48:06; `issued server command: /stop` 17:48:12, **the one command in the
log**; `Stopping server` 17:48:12; `All dimensions are saved` 17:48:13. **His join belongs to the void boot.** After the
stop: no `java` process; `find run/world/dimensions/rpgfight -mindepth 1` printed nothing; the overworld 106 files, the
backup's (`../rpg-run-backups/2026-11-23-pre-enchant-columns`) 106; no `ORB` line (none stood).

**The next boot reads every R0 again on its own log: R0a as R0aR2, below; R0bR2, R0cR2, R0dR2 and R0e as written.**

### R0aR2 — the build line names the NEW head, clean (2026-11-23, after the void boot)

> **DATED NOTE, 2026-11-23: R0a's text stands above, unedited, as the prediction the void boot missed.** R0aR2 supersedes
> it for the next boot, because the commit that records the void boot moves the stack top. A commit cannot name its own
> hash, so the head is read from the wire at boot time.

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `<sha>` being `feat/lapis-crystal-gate`'s head as `git ls-remote --heads origin feat/lapis-crystal-gate` prints it, shortened as `git rev-parse --short HEAD` prints it. **Not `004ba1cc`, not `-dirty`, not `unknown`.** Its precondition, read before the boot command: `git status --porcelain` prints nothing, and `git rev-parse HEAD` equals the `ls-remote` line | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **AS PREDICTED, the boot of `e774d4b8`:** `[18:28:16] [Server thread/INFO]: [Rpg] Build: e774d4b8`. Before the boot command: `git status --porcelain` printed nothing; `git rev-parse HEAD` and `ls-remote` both `e774d4b8640914f1b78092d581128db5089e4eb3` |

## THE RUN OF `e774d4b8`, 2026-11-23: WHAT RAN, AND WHY MOST ROWS DID NOT PRESENT THEIR CONDITION

**The boot.** `./scripts/dev-server.sh --refresh-content` printed `==> Building e774d4b8`; the deployed jar 2,453,563
bytes, mtime 18:28:10, sha256 `CE128574…9443527`; this boot's log begins `[18:28:11]` (after the deploy), `Done (6.817s)`
at 18:28:17. Backup `../rpg-run-backups/2026-11-23-pre-enchant-columns-2` (a new folder): `Rpg` 168 files, `world` 193,
`diff -rq lines: 0` each, `world/dimensions` = `minecraft rpgfight`, the overworld 106. Every R0 read as predicted by
18:28:41, with no `joined the game` line in the log; **Ben joined at 18:43:05**, after he was told R0 passed. He typed,
each once and in order, `/gamemode survival` (18:43:11), `/rpg selftest unique-enchant-slot us8` (18:43:15), `/rpg
selftest unique-enchant-slot` (18:43:51), `/rpg selftest enchant-columns` (18:44:12), `/stop` (18:45:20), and in chat
`yes` (18:44:05). Every reading below is from that log, after his login line.

**NINE ROWS DID NOT RUN: EC3, EC4, EC5, EC6, EC7, EC9, EC11, EC14, LC1**, each logging `SKIPPED no hotbar slot holds empty`
at its first `hold: empty`. EC1R2 and EC2 took the two empty hotbar slots there were (`hold … 2->2 item=empty`, then `2->3
item=empty`), and gave the Lapis Staff and the Living Lantern into them. **The run sheet asks for "about 4 empty
inventory slots"; the rows need empty HOTBAR slots**, and the scenario gives five distinct items (`lapis_staff`,
`living_lantern`, `iron_sword`, `iron_chestplate`, `breacher`: counted from its `rpg give` lines, not tested). The
sheet's words are the defect, not Ben's run.

**SO EIGHT ROWS RAN WITHOUT THEIR CONDITION: EC8, EC10, EC13, EC15, EC16, LC2, LC3, LC4.** Each takes its weapon, its turn
to face east, its enchant staging or its mobs from a row that skipped (READ, the scenario: EC8 from EC7, EC10 and EC13
from EC9 (EC13's own comment: "EC9's staging"), EC15 and EC16 from EC14, LC2-LC4 from LC1). Each row's `CLEAN` returns him
to `-98.48 63.00 24.06`, and a press with no turn of its own read `yaw=128.291 pitch=8.273` (and once each `-88.129
3.393`, `-107.723 10.225`, `-90.006 0.014`: the source of those is not established). **Each such row is VOID BY A
SKIPPED ROW: its lines are recorded beside its prediction, and it is not read as a pass or a miss of the code.**

**EC12 RAN ON ITS OWN STEPS AND IS VOID BY ITS OWN TEXT.** Its `aim:` is followed by `use:` with no `wait:`, and each
`use` read the PREVIOUS facing (the aim is a `teleportAsync`): the presses read `yaw=-90.000`, `-108.435` (G's), `-71.565`
(H's), so the beams struck nothing, G, H.

**AND A PROBE ONLY SEES 16 BLOCKS:** `PROBE tag=<x> none within 16 blocks`. F in EC8 stands 30 out and P in EC16 22,
and T in LC2/LC4 22, so those probes cannot read their mob's health as predicted on any run (a defect of the
predictions, found here; LC2-LC4's mobs were also never summoned).

**WHAT WAS READ ON ITS OWN CONDITION AND AS PREDICTED:** every R0; setup; EC0's and EC0R2's AIR counts; EC1R2; EC2; EC17;
and in `GATE-unique-enchant-slot.md` R1, setup, US8, BX1, BX2 (Ben: `yes`). **No row read a defect of the code.** **Not
as described:** EC0's floor holds WATER (255 and 104). **Outside the rows that predict one: no `ORB`, `SPLIT` or
`NEXUSHEAL` line anywhere in the log** (the rows that predict ORB and SPLIT lines skipped). **Homing:** the Living
Lantern fired 4 times (EC8 once, EC10 three); EC10's three each landed on D, EC8's met nothing (F 30 out, and the press
not facing it). **The RESTORED lines:** `xp=712697 … difficulty=NORMAL health=120.000 vanilla_xp=190+0.236` (R1's run),
`health=185.200` (the unique-enchant-slot run), `health=199.200` (this gate's run), `vanilla_xp=190+0.236` each; the RPG
level is not printed (`xp=712697` is the RPG XP restored), and `HEALTH saved=… wrote=…` follows each.

**After his `/stop`:** `Stopping server` 18:45:20; `All dimensions are saved` 18:45:21; no `java` process; `find
run/world/dimensions/rpgfight -mindepth 1` printed nothing; the overworld 106 files against the backup's 106; **no
`cause=stop` line** and no `ORB` line at all.

## THE SECOND FORM, 2026-11-23: THE INSTRUMENT FIXED, FOR THE BOOT AFTER `e774d4b8`

> **DATED NOTE, 2026-11-23 (the seat's brief "FIX THE INSTRUMENT, THEN ONE MORE BOOT").** The skips and voids of the
> run on `e774d4b8` were **the seat's, by its own word: it read the scenario and passed all four defects** (rows that
> needed empty hotbar slots; rows that ran on state a skipped row should have left; EC12's aim and press in one pass;
> probes whose mobs stood beyond 16). **A FIFTH was found while fixing them, and it is CC's, in the scenario CC wrote:**
> EC9 and EC11 ran `rpg enchant clear` and then `rpg enchant candidate 1 kinetic_tremors`, which the command REFUSES:
> a cleared item has NO slots, and `EnchantState.addCandidate` adds only the NEXT one (`slot > slots.size()` throws,
> READ). Both rows skipped before reaching it, so no reading shows it; with an empty hotbar, neither would have made
> Tremors active. `ScenarioLint`'s ENCHANT_ORDER rule finds both in the scenario as it was at `be6180b3`.

**WHAT IS NOT RE-RUN, AND IS NOT RE-PREDICTED:** EC0's counts, EC1R2, EC2 and EC17's counts (each read as predicted on
`e774d4b8`), and every row of `GATE-unique-enchant-slot.md` (R1, setup, US8, BX1, BX2, all read as predicted; Ben's
`yes`). Their scenario entries left `GATE-enchant-columns.yml`. EC0's description of the spot is replaced by the
PREFLIGHT, which refuses the spot instead of describing it. EC17's D2 extension (the run's `ORB` lines) is read from the
LC rows' own predictions below.

**EVERY OTHER ROW IS RE-WRITTEN AND RENAMED** (the old rows' text and readings stand above and below as the record):
EC3R2 to EC16R2, LC1R2 to LC4R2, eighteen rows, under the seat's four rules and the fifth:

1. **A gate asks nothing of his inventory.** Each row that gives empties his storage first (`clear_inventory`: every
   slot 0-35 but the Nexus item's; armour and off hand untouched). The snapshot holds his inventory from RUN START and
   the run's restore puts it all back.
2. **Every row is self-contained.** Each row that fires or probes sets, itself: his RPG level (`rpg playerxp set`), the
   weapon (a fresh `rpg give`, then `hold`), its gear score (100), its enchant state (`rpg enchant clear`, then exactly
   what the row needs, then `rpg enchant show`), mana (`rpg mana refill`), a cleared cooldown and a fresh record
   (`clear_state`: every cooldown, his behaviour streaks, his orb), his facing (the turn east), and its mobs (old ones
   killed first). The LC rows each throw their own orb and remove it at their end (`clear_state`, cause `selftest`).
3. **A wait between any turn and the press after it** (EC12R2's aims wait 2).
4. **Every probe's mob is inside the probe's reach.** The probe step takes a reach the scenario states, `probe: "f 32"`
   (an instrument change: `SelfTestStaging.probeArg`, refused at load outside 1-64, unit-tested in
   `SelfTestRowStagingTest`). **This way, not the PLAYERHIT lines, because the rows read a mob's HEALTH as well as its
   hits** (EC8's "nothing landed" is F's 1000 as much as no PLAYERHIT: a burn would show only in the health). No mob
   moved: EC8R2's F stays 30 out, beyond the 28.
5. **After `rpg enchant clear`, slot 0 first.** A row that stages one behaviour stages it in slot 0; the LC rows stage
   Lapis Crystal in 0 and Kinetic Tremors in 1. `EnchantState.effective()` reads every slot's active candidate alike
   (READ), so the slot changes the `show` lines and nothing the behaviour does.

**AND THE GROUND IS MEASURED FIRST.** The setup's first step is `preflight: "0 -6 32 10"`: every column 0 to 32 east
and 6 north (his left) to 10 south (his right) of his block must be AIR at feet and head height and SOLID underneath
(not air, not water or any liquid, not waterlogged, not soft). **If any cell fails, NO ROW RUNS**: he is told in two
lines what is wrong and where, the run ends (`SELFTEST RUN ABORTED preflight: the ground failed, no row ran`) and the
snapshot restores at once. Every summon below stands inside that box (`ScenarioLint`'s GROUND rule).

### THE RUN SHEET, SECOND FORM — BEN TYPES THESE THREE LINES

> **SUPERSEDED, 2026-11-23 (Ben's ruling A: the selftest builds its own ground):** the next boot's run sheet is *THE
> THIRD FORM*'s. This one asked him to find ground; that one asks nothing of his ground or his inventory.

**IN PLAIN WORDS, BEFORE YOU TYPE ANYTHING:** find **solid, flat ground** (grass, dirt, sand or stone: **not water, not
ice over water**). Face **east** (F3 shows `Facing: east`; the selftest turns you east anyway). The ground must be
clear for **33 blocks ahead of you, 6 blocks to your left and 10 blocks to your right**: nothing but air at your feet
and head height (no tall grass, flowers, trees, walls or mobs), and solid ground under all of it.

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.** Then stand on that ground.
1. `/gamemode survival`
2. `/rpg selftest enchant-columns` — **ONCE.** About 105 seconds: you are turned east, zombies appear and stand still,
   your weapons fire by themselves, and a lapis crystal is thrown and floats, four times. Do nothing.
3. `/stop`

**IF THE SPOT IS NOT GOOD ENOUGH**, nothing runs and you are told at once, in red, what is wrong and where, for
example: *"The selftest cannot start here: 12 floor blocks are water or another liquid (water), from 3 to 8 blocks east,
2 to 3 to your right."*, and in gold: *"Move to solid flat ground and type the command again."* **Then move and type
line 2 again**, as often as it takes: a refused try runs no row and changes nothing, and each try is a new run.

**This run's length** (recount: `tr -d '\r' < paper/src/main/resources/selftest/GATE-enchant-columns.yml | awk
'/^[A-Za-z0-9_-]+:/{row=$1} /^  - wait:/{w[row]+=$3} END{for (r in w) print r, w[r]}'`): setup 2, EC3R2 6, EC4R2 4,
EC5R2 49, EC6R2 49, EC7R2 104, EC8R2 144, EC9R2 108, EC10R2 147, EC11R2 64, EC12R2 110, EC13R2 108, EC14R2 124, EC15R2
84, EC16R2 206, LC1R2 84, LC2R2 74, LC3R2 118, LC4R2 374: **1959**, plus about 5 per row's clean and DONE (19: 95):
**about 2055 ticks (103 s)**, no question.

### SAFETY, SECOND FORM: THE MOST HITS THE RUN CAN LAND ON HIM, TIMES THE BIGGEST, AGAINST HIS HEALTH

**Where each row puts him:** every row's clean returns him to the block he typed the command on; every row that fires
turns him there to face due east (`tp … -90 0`, block-centred). No row moves him anywhere else. His health at level 1 is
100 + G; at level 13 (EC5R2, EC6R2) 160 + G (G: his worn gear's health; armour is never cleared).

| rows | what could reach him | most hits x biggest hit |
|---|---|---|
| EC3R2-EC6R2 | gives, `show`/`pool`, the table's dump | **0 x 0 = 0** |
| EC7R2, EC8R2, EC14R2-EC16R2 | the lantern's shot and the bursts ignore him (`castRay(…, caster.id())`); homing skips players (`ProjectileFlight.nearestMob`) | **0 x 0 = 0** |
| EC9R2-EC13R2 | the Lapis beam and the Breacher's pellets ignore him; a shockwave skips every player (`BehaviourHits`, `BehaviourHitsTest`) | **0 x 0 = 0** |
| LC1R2-LC4R2 | the throw lands no damage (an orb cast has no on_hit); the beam ignores him; the crystal's shots skip every player (`CastExecutor.orbShots`, `OrbBeamTest`); the orb is an item marker nobody can pick up | **0 x 0 = 0** |
| every row | the frozen zombies (NoAI); Ignite (only on a scorched death, and nothing dies: the most a mob takes is EC11R2's 160 + 10, or 340 on its crit); a natural hostile (the setup kills them within 64 and sets `spawn_mobs false`) | **0** from the run |

### R0, THIRD FORM (2026-11-23): R0aR3, R0bR3, R0cR3, R0dR3. R0e STANDS

#### R0aR3 — the build line names the new head, clean

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `<sha>` being `feat/enchant-columns-instrument`'s head as `git ls-remote --heads origin feat/enchant-columns-instrument` prints it, shortened as `git rev-parse --short HEAD` prints it. **Not `e774d4b8`, not `be6180b3`, not `-dirty`, not `unknown`.** Its precondition (`booting.md` step 3): `git status --porcelain` prints nothing and `git rev-parse HEAD` equals the `ls-remote` line | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

#### R0bR3 — the jar carries the second form

**PRESENT: R0bR2's 49 `ValueLine` calls less its five on rows that left the scenario (`EC0:`, `EC1R2:`, `LC1:`,
`LC4:`, `EC17:`), so 44, and 7 more: 51** (`EC3R2:`, `EC16R2:`, `LC1R2:`, `LC4R2:`, the preflight line, a
`clear_state` step, EC8R2's `probe: "f 32"`). Then R0bR2's `ABSENT-ENTRY`. **ABSENT: R0bR2's 13 controls and the five
rows that left (18).** Recount: `awk '/^.--- controls.$/{exit} /^ValueLine/' <the block> | wc -l` prints 51, and 18
after the controls line.

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
$lc = Bytes 'content/enchants/lapis_crystal.yml'
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
ValueLine $lc 'effect: grant_trigger' 'lapis_crystal.yml'
ValueLine $lc 'weapon: lapis_staff' 'lapis_crystal.yml'
ValueLine $lc '  input: left_click' 'lapis_crystal.yml'
ValueLine $lc '    cooldown_ticks: 600' 'lapis_crystal.yml'
ValueLine $lc '      amount: 30' 'lapis_crystal.yml'
ValueLine $lc '      type: orb' 'lapis_crystal.yml'
ValueLine $lc '      range: 12' 'lapis_crystal.yml'
ValueLine $lc '      life_ticks: 300' 'lapis_crystal.yml'
ValueLine $lc '        percent_by_level: [20, 30, 40]' 'lapis_crystal.yml'
ValueLine $ll '      type: projectile' 'living_lantern.yml'
ValueLine $ll '      speed: 0.35' 'living_lantern.yml'
ValueLine $ll '      max_lifetime_ticks: 80' 'living_lantern.yml'
ValueLine $ll '        search_radius: 7' 'living_lantern.yml'
ValueLine $ll '        amount: 45' 'living_lantern.yml'
ValueLine $tw '    key: entity.generic.explode' 'tremor_wave.yml'
ValueLine $eg 'EC3R2:' 'GATE-enchant-columns.yml'
ValueLine $eg 'EC16R2:' 'GATE-enchant-columns.yml'
ValueLine $eg 'LC1R2:' 'GATE-enchant-columns.yml'
ValueLine $eg 'LC4R2:' 'GATE-enchant-columns.yml'
ValueLine $eg '  - preflight: "0 -6 32 10"' 'GATE-enchant-columns.yml'
ValueLine $eg '  - clear_state: ""' 'GATE-enchant-columns.yml'
ValueLine $eg '  - probe: "f 32"' 'GATE-enchant-columns.yml'
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
ValueLine $eg 'EC1:' 'GATE-enchant-columns.yml CONTROL-RENAMED'
ValueLine $lc '      type: projectile' 'lapis_crystal.yml CONTROL-D1-STUB'
ValueLine $lc '    cooldown_ticks: 60' 'lapis_crystal.yml CONTROL-PREFIX'
ValueLine $lc '      range: 1' 'lapis_crystal.yml CONTROL-PREFIX'
ValueLine $eg 'EC0:' 'GATE-enchant-columns.yml CONTROL-LEFT'
ValueLine $eg 'EC1R2:' 'GATE-enchant-columns.yml CONTROL-LEFT'
ValueLine $eg 'LC1:' 'GATE-enchant-columns.yml CONTROL-LEFT'
ValueLine $eg 'LC4:' 'GATE-enchant-columns.yml CONTROL-LEFT'
ValueLine $eg 'EC17:' 'GATE-enchant-columns.yml CONTROL-LEFT'
$zip.Dispose()
```

**The prediction, line for line:** before `'--- controls'`, **51 `PRESENT`, 0 `ABSENT`, and one `ABSENT-ENTRY`**; after it,
**18 `ABSENT`, 0 `PRESENT`**. Run before the commit: see *R0bR3, RUN BEFORE COMMIT* at the end.

#### R0cR3 — every Loaded line, and nothing refused

R0cR2's prediction unchanged (no content file moved: `14 enchants`, `11 abilities`, `45 visuals`), and **no `Refusing
selftest/GATE-enchant-columns.yml` line**: every new step (`preflight`, `clear_inventory`, `clear_state`, a probe's
reach) is admitted by the loader, or its row would be refused whole and R0dR3 would read fewer.

#### R0dR3 — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 351 scenarios across 43 gates [melee-cell, …, unique-enchant-slot, enchant-columns]`**. The controls: `355 scenarios` and `340 scenarios` ABSENT. (351 is also the count before D, by coincidence: 398 - 22 + 18 = 394 keys, 43 setups) | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (`SelfTestScenariosTest` asserts 351): `cat GATE-*.yml | tr -d '\r' | grep -cE '^[A-Za-z0-9_-]+:'` = **394**,
`^setup:` **43**, so **351**.

### THE ROWS, SECOND FORM

**Common to every row (the first form's common lines stand):** each row's `STAGE clear_inventory cleared=<n> kept=<nexus@k
\| none>` is RECORDED; each `STAGE clear_state cooldowns=cleared streaks=forgotten orb=none` at a row's START is PREDICTED
(no orb outlives the row that threw it); each `rpg playerxp`, `rpg give`, `rpg gearscore`, `rpg mana`, `rpg enchant
clear/candidate/level/active` REPLY is RECORDED. The turn's `REPLY "Teleported <him> to <X>, <Y>, <Z>" []` is recorded,
and every press after it reads **`yaw=-90.000 pitch=0.000`** (nothing turns him after it; EC13 read exactly that on
`e774d4b8`), except EC12R2's, which read their aims. **The preflight's own line, in the setup:** `PREFLIGHT ok box=0..32
east -6..10 south air=1122/1122 floor solid=561/561` (33 x 17 = 561 columns), or the run ends there.

**D AND D2 STAND** (every damage sum names its terms; a shockwave and a crystal shot inherit every term of the hit that
set them off, its crit included, as the second allowed reading). Score 100, enchant 1.0 (a behaviour is not a damage
enchant; a cleared item has none), class bonus 0, level bonus 0 (level 1, the row's own), charge 1.0.

### EC3R2 — AN IRON SWORD'S COLUMNS, A FRESH ROLL (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| EC3's prediction, unchanged: `REPLY "Iron Sword -- 4 slot(s)" [aqua]`; **`Slot 0: []`**; **`Slot 1: [kinetic_tremors locked]`**; `Slot 2:`/`Slot 3:` over `sharpness`/`unbreaking`; `raw: v1;:-1;kinetic_tremors=0:-1;…`. **`Pool: sharpness, unbreaking`**, **`Unique pool: kinetic_tremors`**, **`Weapon pool: `**. `DRIVE hold … item=iron_sword` | W2 | **READ AS PREDICTED (20:38:27):** `STAGE clear_inventory cleared=11 kept=nexus@8`; `hold … 2->0 item=iron_sword`; `"Iron Sword -- 4 slot(s)"`; `"  Slot 0: []"`; `"  Slot 1: [kinetic_tremors locked]"`; `"  Slot 2: [unbreaking locked, sharpness locked]"`, `"  Slot 3: [unbreaking locked, sharpness locked]"`; `raw: v1;:-1;kinetic_tremors=0:-1;unbreaking=0,sharpness=0:-1;unbreaking=0,sharpness=0:-1`; `Pool: sharpness, unbreaking`, `Unique pool: kinetic_tremors`, `Weapon pool: ` |

### EC4R2 — A PIECE OF ARMOR'S COLUMNS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| EC4's prediction, unchanged: **`HELDID slot=<n> item=iron_chestplate`** (anything else VOIDS the row); `REPLY "Iron Chestplate -- 4 slot(s)" [aqua]`; **`Slot 0: []`**, **`Slot 1: []`**; `Slot 2:`/`Slot 3:` each 1 to 4 of `growth`/`mana_bank`/`protection`/`unbreaking`, distinct; `raw: v1;:-1;:-1;…`. **`Pool: growth, mana_bank, protection, unbreaking`**, **`Unique pool: `**, **`Weapon pool: `** | W2; U5 | **READ AS PREDICTED (20:38:27):** `STAGE clear_inventory cleared=1 kept=nexus@8`; `hold … 0->0 item=empty`; **`HELDID slot=0 item=iron_chestplate offhand=empty`**; `"Iron Chestplate -- 4 slot(s)"`; `Slot 0: []`, `Slot 1: []`; `"  Slot 2: [growth locked, unbreaking locked, mana_bank locked, protection locked]"`, `"  Slot 3: [protection locked, mana_bank locked]"`; `raw: v1;:-1;:-1;growth=0,unbreaking=0,mana_bank=0,protection=0:-1;protection=0,mana_bank=0:-1`; `Pool: growth, mana_bank, protection, unbreaking`, `Unique pool: `, `Weapon pool: ` |

### EC5R2 — THE TABLE ON AN IRON SWORD, AT LEVEL 13 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Set 13 level(s) for <him> (now level 13, 0/2770 into it, 19980 lifetime)." [green]`; then EC5's prediction, unchanged (`SCREEN EnchantMenu`, no `SLOT 1`, `SLOT 10 GRAY_DYE "No weapon enchant"`, `SLOT 12 PISTON "Kinetic Tremors"` with its three lore lines, no `SLOT` at 19 or 21, the normal columns, 48, 49) | W3; `EnchantMenuLayoutTest` | **READ AS PREDICTED (20:38:28-30):** `REPLY "Set 13 level(s) for BaronVonYeetus (now level 13, 0/2770 into it, 19980 lifetime)." [green]`; `SCREEN EnchantMenu title="Enchantments" [dark_gray]`; `SLOT 4 IRON_SWORD name="Iron Sword" …`, `SLOT 7 BOOKSHELF …`, `SLOT 8 ENCHANTING_TABLE …`; **no `SLOT 1`**, none at 0, 2, 3, 5, 6, 9, 19, 21; **`SLOT 10 GRAY_DYE name="No weapon enchant" [white, dark_gray] italic lore=["Nothing is tied to this weapon yet." [dark_gray]]`**; **`SLOT 12 PISTON name="Kinetic Tremors" [white, dark_gray] italic lore=["3 hits on one foe in 3s: a 10 damage shockwave, 3 blocks" [gray]; "" []; "Locked. Click to unlock at I -- 352 XP." [dark_gray]]`**; both normal slots rolled two: `14`/`16` `ANVIL "Unbreaking"` (`"consumes durability on 50% of uses"`), `23`/`25` `IRON_SWORD "Sharpness"` (`"+5% damage, x1.05"`), each `"Locked. Click to unlock at I -- 352 XP."`; `SLOT 48 ARROW`, `SLOT 49 BARRIER`. Names in the dump's `[white, <colour>] italic` form |

### EC6R2 — THE TABLE ON A LIVING LANTERN, AT LEVEL 13 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The level REPLY as EC5R2's; then EC6's prediction, unchanged (no `SLOT 1`; `SLOT 4 LANTERN`; `SLOT 10 FIRE_CHARGE "Airburst Flames"` with its three lore lines; `SLOT 12 GRAY_DYE "No unique enchant"`; no `SLOT` at 19 or 21; the normal columns; 48, 49) | W3; `AirburstFlamesContentTest` | **READ AS PREDICTED (20:38:30-33):** the level REPLY as EC5R2's; no `SLOT 1`; `SLOT 4 LANTERN name="Living Lantern" …`; **`SLOT 10 FIRE_CHARGE name="Airburst Flames" [white, dark_gray] italic lore=["splits after 14 blocks into 3 bursts of 25 damage" [gray]; "" []; "Locked. Click to unlock at I -- 352 XP." [dark_gray]]`**; **`SLOT 12 GRAY_DYE name="No unique enchant" [white, dark_gray] italic lore=["Nothing unique is tied to this item's element yet." [dark_gray]]`**; no `SLOT` at 19 or 21; `SLOT 14 BLAZE_ROD "Attunement"` (`"+5% damage, x1.05"`), `SLOT 16 ANVIL "Unbreaking"` (each slot rolled one, so no 23/25); 48, 49 |

### EC7R2 — THE LANTERN'S SHOT: ONE HIT OF 45 ON A MOB OFF HIS AIM LINE (SURVIVAL)

**Begins from a cleared cooldown:** `clear_state`.

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Set 1 level(s) for <him> (now level 1, 0/1000 into it, 0 lifetime)." [green]`; `REPLY "Enchant data cleared -- both keys removed." [green]`; **`REPLY "This weapon carries no enchant data." [yellow]`** (the `show`). Probe L before: **`1000.000/1000.000`**. `DRIVE use … item=living_lantern yaw=-90.000 pitch=0.000`. **Exactly one PLAYERHIT `source=living_lantern/right_click target=<L> zombie element=fire sent=45.000 crit=false triggerScore=100`** (or `sent=90.000 crit=true`). **The sum:** `(45 x 100/100) x 1.0 + 0 + 0 = 45`, x charge 1.0, x crit 1.0 (2.0). Probe L at +60 recorded. **WHAT it lands is exact; WHETHER it lands is a READING** (F) | B; `LivingLanternFlightTest`; D | **READ AS PREDICTED (20:38:33-38):** `STAGE clear_state cooldowns=cleared streaks=forgotten orb=none`; `"Set 1 level(s) … (now level 1, 0/1000 into it, 0 lifetime)."`; `"Enchant data cleared -- both keys removed." [green]`; **`"This weapon carries no enchant data." [yellow]`**; probe L **`1000.000/1000.000`** (d 10.440); `DRIVE use … item=living_lantern yaw=-90.000 pitch=0.000`; **exactly one `PLAYERHIT source=living_lantern/right_click target=<L> zombie element=fire sent=45.000 crit=false triggerScore=100`**: **the homing shot LANDED**; probe L at +60 `932.500/1000.000` (45 and one burn of 22.5) |

### EC8R2 — BEYOND 28 BLOCKS: NOTHING, AND F'S HEALTH READ (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The level, clear and `show` REPLYs as EC7R2's. **`PROBE tag=f <uuid> zombie customHP=1000.000/1000.000 … d=30.000 dy=0.000`** before and after (the reach of 32 sees it). `DRIVE use … item=living_lantern yaw=-90.000 pitch=0.000`; **NO PLAYERHIT**, no `DOTTICK` (F 30 east; the shot ends at 28.0 of travel). **Exact** | B; `LivingLanternContentTest.theReachIsTwentyEightBlocksOfTravel` | **READ AS PREDICTED (20:38:39-46):** **`PROBE tag=f <uuid> zombie customHP=1000.000/1000.000 fireTicks=0 dead=false d=30.000 dy=0.000`** before and after (the reach of 32 read it); `use … item=living_lantern yaw=-90.000 pitch=0.000`; **no PLAYERHIT, no DOTTICK** in the row |

### EC9R2 — KINETIC TREMORS AT LEVEL I: THREE HITS, ONE SHOCKWAVE, ITS RADIUS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Lapis Staff -- 1 slot(s)" [aqua]`, **`REPLY "  Slot 0: [*kinetic_tremors I]" [gray]`**, **`REPLY "  active: kinetic_tremors I (3 hits on one foe in 3s: a 10 damage shockwave, 3 blocks)" [gray]`**, the `raw:` line recorded. **Three PLAYERHITs `source=lapis_staff/right_click target=<A> zombie element=kinetic sent=21.000`** (each `42.000 crit=true` on its own crit): `(21 x 100/100) x 1.0 + 0 + 0`. **After the third, exactly TWO `kinetic_tremors/shockwave` PLAYERHITs, on A and on B, each `sent=10.000`** (`20.000` if the THIRD beam crit; both equal): `(10 x 100/100) x 1.0 + 0 + 0 = 10`, x 1.0, x that beam's crit. **None on C.** None after the first or second beam. Probes: A **`927.000`** (more on crits, by the crits named), B **`990.000`** (or `980.000`), C **`1000.000`** | C's rule; `BehaviourHitsTest`; E | **READ AS PREDICTED (20:38:46-52):** `"Lapis Staff -- 1 slot(s)"`, `"  Slot 0: [*kinetic_tremors I]"`, `"  active: kinetic_tremors I (3 hits on one foe in 3s: a 10 damage shockwave, 3 blocks)"`, `raw: v1;kinetic_tremors=1:0`; three `lapis_staff/right_click` on A `sent=21.000 crit=false`; after the third, **two `kinetic_tremors/shockwave`, on A and on B, `sent=10.000 crit=false`**, none on C, none before the third; probes A **`927.000`**, B **`990.000`**, C **`1000.000`** |

### EC10R2 — TWO HITS, 61 TICKS, ONE HIT: NO SHOCKWAVE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| EC9R2's `show` lines. **Three PLAYERHITs `source=lapis_staff/right_click target=<D>` `sent=21.000`** (or `42.000`) and **NO `kinetic_tremors/shockwave`** (the third press is 83 and 61 ticks after the first two, both outside the 60). Probe D **`937.000`** (no crit) | `ShockwaveStreaksTest.twoHitsThenSixtyOneTicksThenOneFiresNothing` | **READ AS PREDICTED (20:38:52-59):** EC9R2's `show` lines; three `lapis_staff/right_click` on D `sent=21.000 crit=false` (20:38:54, :55, :58); **no shockwave**; probe D **`937.000`** |

### EC11R2 — ONE BREACHER SHOT ON ONE MOB: EXACTLY ONE SHOCKWAVE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Breacher -- 1 slot(s)" [aqua]`, `"  Slot 0: [*kinetic_tremors I]"`, `"  active: kinetic_tremors I (…)"`. **Four PLAYERHITs `source=breacher/right_click target=<E> zombie element=kinetic sent=40.000 crit=false`** (all four `80.000 crit=true` on the crit). **Exactly ONE `kinetic_tremors/shockwave` PLAYERHIT, on E, `sent=10.000`** (`20.000` on the crit): `(10 x 100/100) x 1.0 + 0 + 0 = 10`. Probe E **`830.000`** (`1000 - 160 - 10`), or **`660.000`** on the crit | Ben's "Multiple pellets count"; `ShockwaveStreaksTest.fourPelletsInOneTickFireExactlyOne` | **READ AS PREDICTED (20:38:59-39:03):** `"Breacher -- 1 slot(s)"`, `"  Slot 0: [*kinetic_tremors I]"`, its active line; **four `breacher/right_click` on E `sent=40.000 crit=false`**, and **exactly one `kinetic_tremors/shockwave` on E `sent=10.000`**, logged after the third pellet and before the fourth; probe E **`830.000`** |

### EC12R2 — HITS SPLIT ACROSS TWO MOBS: NONE, EACH PRESS ON ITS OWN AIM (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| EC9R2's `show` lines. `DRIVE aim … target=<G> … yaw=-108.435 pitch=-2.173`, then (2 ticks later) **`DRIVE use … yaw=-108.435 pitch=-2.173`**; `aim … <H> yaw=-71.565`, then **`use … yaw=-71.565 pitch=-2.173`**; `aim … <G>`, then **`use … yaw=-108.435`** (the angles `e774d4b8`'s aims read). Three PLAYERHITs `lapis_staff/right_click`: on G, on H, on G (each `21.000` or `42.000`). **NO shockwave** (G two, H one). Probes G **`958.000`**, H **`979.000`** (no crit). **A press whose yaw is not its own aim's, or a beam on the wrong mob, VOIDS the row** | C's rule ("Same target"); rule 3 | **READ AS PREDICTED (20:39:03-08):** `aim … <G> yaw=-108.435 pitch=-2.173`, then **`use … yaw=-108.435 pitch=-2.173`**; `aim … <H> yaw=-71.565`, then **`use … yaw=-71.565 pitch=-2.173`**; `aim … <G>`, then **`use … yaw=-108.435`**: each press on its own aim; `lapis_staff/right_click` on G, H, G, each `sent=21.000 crit=false`; **no shockwave**; probes G **`958.000`**, H **`979.000`** |

### EC13R2 — KINETIC TREMORS AT LEVEL III: DAMAGE 20, RADIUS 5 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `"  Slot 0: [*kinetic_tremors III]"`, **`"  active: kinetic_tremors III (3 hits on one foe in 3s: a 20 damage shockwave, 5 blocks)"`**. Three beams on A as EC9R2. **Exactly THREE shockwave PLAYERHITs, on A, B and C, each `sent=20.000`** (`40.000` on the third beam's crit): `(20 x 100/100) x 1.0 + 0 + 0 = 20`. **None on K.** Probes: A **`917.000`**, B **`980.000`**, C **`980.000`**, K **`1000.000`** (no crit) | C's rule | **READ AS PREDICTED, THE SECOND BEAM'S CRIT THE ALLOWED READING (20:39:08-14):** `"  Slot 0: [*kinetic_tremors III]"`, `"  active: kinetic_tremors III (3 hits on one foe in 3s: a 20 damage shockwave, 5 blocks)"`, `raw: v1;kinetic_tremors=3:0`; beams on A `sent=21.000 crit=false`, **`sent=42.000 crit=true`**, `sent=21.000 crit=false`; after the third (no crit), **three shockwaves, on A, B and C, each `sent=20.000 crit=false`**, none on K; probes A **`896.000`** (= 917 less the second beam's crit's extra 21: `1000 - 21 - 42 - 21 - 20`), B **`980.000`**, C **`980.000`**, K **`1000.000`** |

### EC14R2 — AIRBURST FLAMES AT LEVEL II: NO 45, A SPLIT AT 14, THE CENTRE BURST'S 30 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Living Lantern -- 1 slot(s)" [aqua]`, **`"  Slot 0: [*airburst_flames II]"`**, **`"  active: airburst_flames II (splits after 14 blocks into 3 bursts of 30 damage)"`**. `PROBE self` gives his feet `<X, Y, Z>`. **Exactly one `SPLIT caster=<him> source=living_lantern/right_click at=<X+14.000>,<Y+1.620>,<Z> bursts=3 tick=<t>`** (EXACT, as EC14). **Exactly one PLAYERHIT, on M: `source=living_lantern/right_click … element=fire sent=30.000`** (`60.000 crit=true`). **NO `sent=45.000`.** Probe M (reach 24): `1000 - 30 = 970.000` less its burns (recorded) | E's rule; `AirburstTest` | **READ AS PREDICTED (20:39:14-20):** `"Living Lantern -- 1 slot(s)"`, `"  Slot 0: [*airburst_flames II]"`, `"  active: airburst_flames II (splits after 14 blocks into 3 bursts of 30 damage)"`; `SELF … at=104.50 201.00 -3.50`; **exactly one `SPLIT source=living_lantern/right_click at=118.500,202.620,-3.500 bursts=3 tick=6176`** (X+14.000, Y+1.620, Z: exact); **exactly one PLAYERHIT on M `element=fire sent=30.000 crit=false`, no 45**; probe M **`955.000`** (30 and one burn of 15.000, recorded), d 22.000 |

### EC15R2 — A HIT BEFORE 14 BLOCKS: THE 45, NO SPLIT (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| EC14R2's `show` lines. **One PLAYERHIT on N `sent=45.000`** (`90.000`), **NO `SPLIT`**. Probe N recorded. **WHETHER it lands is a READING** (N within 7 of the muzzle, so the shot homes at its feet); a miss that flies on and splits at 14 VOIDS the row | Ben's "Doesn't split if it hits something first" | **READ AS PREDICTED, ITS CRIT THE ALLOWED READING (20:39:20-25):** EC14R2's `show` lines; **one PLAYERHIT on N `sent=90.000 crit=true`**: **the homing shot LANDED**; **no `SPLIT`** in the row; probe N `887.500` (90 and one burn of 22.5, recorded) |

### EC16R2 — LEVEL I AGAINST LEVEL III (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `"  Slot 0: [*airburst_flames I]"` and `"  active: airburst_flames I (splits after 14 blocks into 3 bursts of 25 damage)"`; later `REPLY "Slot 0: Airburst Flames unlocked to III." [green]` (recorded). Two `SPLIT … bursts=3` lines, each at `<X+14.000>,<Y+1.620>,<Z>`. **The first press's PLAYERHIT on P `sent=25.000`, the second's `sent=35.000`** (each doubled on its own crit). Probe P (reach 24): `1000 - 25 - 35 = 940.000` less the burns (recorded) | Ben's 25/30/35 | **READ AS PREDICTED (20:39:25-35):** `"  Slot 0: [*airburst_flames I]"`, `"  active: airburst_flames I (splits after 14 blocks into 3 bursts of 25 damage)"`; **two `SPLIT … at=118.500,202.620,-3.500 bursts=3`** (ticks 6394, 6476); **P `sent=25.000`** after the first, **`sent=35.000`** after the second, neither a crit; probe P **`872.500`** (`1000 - 60 - 67.5`: burns of 12.5 x 4 and 17.5, recorded), d 22.000 |

### LC1R2 — LAPIS CRYSTAL'S THROW: 30 MANA, ITS 600-TICK COOLDOWN, AT REST 12 OUT, GONE AT THE ROW'S END (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Lapis Staff -- 2 slot(s)" [aqua]`, **`"  Slot 0: [*lapis_crystal II]"`**, **`"  Slot 1: [*kinetic_tremors I]"`**, **`"  active: lapis_crystal II (grants Left-Click: Lapis Crystal (Cooldown: 30.0s \| Mana Cost: 30); a beam into it strikes each foe within 8 for 6.3 damage)"`** and `"  active: kinetic_tremors I (…)"`, each `[gray]`. The tooltip as LC1 predicted it. `PROBE self` gives `<X, Y, Z>`. **`MANA current=<m>`, `DRIVE reload held=lapis_staff`, `MANA current=<m - 30.000>`**; the second `reload`: nothing thrown, nothing spent; the third `MANA` reads the second plus 20 ticks' regeneration only. **Exactly one `ORB rest owner=<him> at=<X+12.000>,<Y+1.620>,<Z> tick=<t>`**, 10 ticks after the first press. **At the row's end: `ORB removed owner=<him> at=<the same point> cause=selftest tick=<t'>`, then `STAGE clear_state … orb=removed`** | D1, D2; `OrbThrowTest` | **READ AS PREDICTED (20:39:35-40):** `"Lapis Staff -- 2 slot(s)"`, `"  Slot 0: [*lapis_crystal II]"`, `"  Slot 1: [*kinetic_tremors I]"`, both active lines word for word, `raw: v1;lapis_crystal=2:0;kinetic_tremors=1:0`; the tooltip (`HELD lapis_staff BREEZE_ROD … lore=["Lapis Crystal II" [gray]; "Kinetic Tremors I" [gray]; …]`, recorded); `SELF … at=104.50 201.00 -3.50`; **`MANA current=100.000 max=100.000`, `DRIVE reload held=lapis_staff`, `MANA current=70.000`**; the second `reload`: nothing thrown, `MANA current=71.000` (one of regeneration); **exactly one `ORB rest owner=<him> at=116.500,202.620,-3.500 tick=6576`** (X+12.000, Y+1.620, Z); at the end **`ORB removed … at=116.500,202.620,-3.500 cause=selftest tick=6605`**, then `STAGE clear_state … orb=removed`. The 10-tick flight is not read: the press's tick is not logged |

### LC2R2 — ITS OWN ORB; A BEAM INTO IT: N TAKES 6.3, F AND T NOTHING, THE BEAM NOTHING (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| LC1R2's `show` lines. `ORB rest owner=<him> at=<X+12.000>,<Y+1.620>,<Z> …` after the `reload`. `DRIVE use … item=lapis_staff yaw=-90.000 pitch=0.000`. **NO `lapis_staff/right_click` PLAYERHIT.** **Exactly one PLAYERHIT `source=lapis_crystal/crystal target=<N> zombie element=kinetic sent=6.300 crit=false triggerScore=100`**, or **`sent=12.600 crit=true`** (the beam's crit). **The sum (D2):** `(21 x 30 / 100 = 6.3) x 100/100 x 1.0 + 0 + 0 = 6.3`, x 1.0, x the beam's crit. **Nothing on F** (9.14 from the orb's centre) **or T** (10.13, behind it). Probes N **`993.700`** (or `987.400`), F **`1000.000`**, T (reach 24) **`1000.000`**. LC2's note (b) stands: the box-to-sphere cut stays UNREAD on a server. **Its note (a) is retired: the preflight refuses a wet floor, so F's half can no longer be voided by water.** At the row's end: `ORB removed … cause=selftest`, `orb=removed` | D3; `OrbBeamTest` | **READ AS PREDICTED (20:39:40-44):** LC1R2's `show` lines; `ORB rest … at=116.500,202.620,-3.500 tick=6665`; `use … yaw=-90.000 pitch=0.000`; **no `lapis_staff/right_click`**; **exactly one `lapis_crystal/crystal` on N `sent=6.300 crit=false triggerScore=100`**; nothing on F or T; probes N **`993.700`** (d 13.000), F **`1000.000`** (d 15.000), T **`1000.000`** (d 22.000, reach 24); `ORB removed … cause=selftest tick=6684`, `orb=removed`. The row-start clean read **`LINGER work=1 entities=0`** (see *THE RUN OF `efa38636`*) |

### LC3R2 — ITS OWN ORB, THREE BEAMS: THREE CRYSTAL SHOTS, NO SHOCKWAVE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| LC1R2's `show` lines; `ORB rest …`. **Three `lapis_crystal/crystal` PLAYERHITs on N**, each `sent=6.300` (or `12.600` on its own beam's crit): the orb is not used up. **NO `kinetic_tremors/shockwave`** although Kinetic Tremors I is active and N took three inside 60 ticks: crystal shots are never hits (B4). **No `lapis_staff/right_click` PLAYERHIT.** Probe N **`981.100`** (`1000 - 3 x 6.3`, no crit). At the row's end: `ORB removed … cause=selftest`, `orb=removed` | D3, B4; `OrbBeamTest.crystalShotsAloneNeverFireAShockwave` | **READ AS PREDICTED (20:39:44-50):** `ORB rest … tick=6744`; **three `lapis_crystal/crystal` on N, each `sent=6.300 crit=false`**; **no shockwave**, no `lapis_staff/right_click`; probe N **`981.100`**; `ORB removed … cause=selftest tick=6807`, `orb=removed`. Its clean read `LINGER work=1` |

### LC4R2 — ITS OWN ORB, GONE 300 TICKS FROM ARRIVAL, AND THEN THE BEAM PASSES (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| LC1R2's `show` lines. **`ORB rest owner=<him> at=<X+12.000>,<Y+1.620>,<Z> tick=<t>`**, then **exactly one `ORB removed owner=<him> at=<that point> cause=expired tick=<t + 300>`**. Then **one PLAYERHIT `source=lapis_staff/right_click target=<T> zombie element=kinetic sent=21.000`** (or `42.000`), **no `lapis_crystal/crystal`**, no shockwave (T took one hit). Probe T (reach 24) **`979.000`** (or `958.000`). At the row's end **`STAGE clear_state … orb=none`** (it expired) | D2; `OrbThrowTest` | **READ AS PREDICTED (20:39:50-40:09):** **`ORB rest … at=116.500,202.620,-3.500 tick=6867`**, then **`ORB removed … cause=expired tick=7167`** (6867 + 300, exact); then **one `lapis_staff/right_click` on T `sent=21.000 crit=false`**, no `lapis_crystal/crystal`, no shockwave; probe T **`979.000`** (d 22.000, reach 24); `STAGE clear_state … orb=none`. Its clean read `LINGER work=1` |

**THE RUN'S `ORB` LINES, ALL OF THEM (EC17's D2 extension, read here):** LC1R2, LC2R2 and LC3R2 each one `rest` and one
`removed … cause=selftest`; LC4R2 one `rest` and one `removed … cause=expired`; **and no other.** After `/stop`: no
`cause=stop`; at the next enable, no `cause=orphan`.

> **READING, THE RUN OF `efa38636`: READ AS PREDICTED.** `grep -c 'ORB ' run/logs/latest.log` = **8**: LC1R2 `rest` 6576
> and `removed … cause=selftest` 6605; LC2R2 `rest` 6665, `selftest` 6684; LC3R2 `rest` 6744, `selftest` 6807; LC4R2
> `rest` 6867 and `removed … cause=expired` 7167; every one at `116.500,202.620,-3.500`; no other. After `/stop`:
> `grep -c 'cause=stop'` = **0**. The next enable's `cause=orphan` is unread (no boot followed).

## THE THIRD FORM, 2026-11-23: THE PAD. THE SELFTEST BUILDS ITS OWN GROUND

> **DATED NOTE, 2026-11-23.** Offered *"A. The test builds its own ground. A one-time flat stone platform high in the
> sky of your dev world. Each run teleports you onto it and returns you to where you were when it ends. Every future
> test uses the same pad"* or *"B. You keep finding the ground yourself"*, **Ben answered: "A."** The seat's rulings
> P1-P9 are built on `feat/selftest-pad`, on #261. **The second form's ROWS are unchanged and keep their names** (none
> was run); only the setup changed. **What was read as predicted on `e774d4b8` stays read and is not re-run.**

**THE PAD (`SelfTestPadShape`, pure; `SelfTestPad`, the builder).** DEV SERVER ONLY (the `-Drpg.dev=true` marker the
selftest itself needs; the run is guarded by it, and `/rpg selftest pad build|remove` check it first). ONE pad, in the
dev world (the server's first world, the overworld): **the stand point is 200 blocks east of world spawn's x, at
spawn's z; the floor at Y 200, his feet at Y 201** (SEAT PLACEHOLDERS, OPEN FOR BEN: where, and plain stone). The build
height, READ from the server jar of build 74 (`data/minecraft/dimension_type/overworld.json`: `min_y -64`, `height 384`):
the top block is Y 319, so the floor and 40 blocks of air above it (to Y 240) are inside it. Its shape, from the stand
point: an INTERIOR x -2..45 and z -13..13 (48 x 27 = 1296 columns), a STONE floor one block thick under all of it, AIR
above the floor to 4 high (5184 cells), and a BARRIER RIM one block wide on all four sides, corners included, from floor
level to 4 above it (154 columns x 5 = 770). **7250 cells, and nothing outside them is ever read or written.**

**AT RUN START, IN THE SETUP, IN THIS ORDER:** the snapshot (taken when the command is typed, as every run's is); then
`pad`: the pad's chunks loaded asynchronously, every one confirmed still loaded on the pad's region, every cell put right
(a missing floor or rim cell PLACED; a wrong block anywhere, in the floor, the rim or the air, REPAIRED), the block
under the stand point confirmed solid, and only then is he teleported to the stand point, facing east. **From then on
every row's clean returns him to the stand point**; every end of the run returns him to where he typed the command.
Any failure: no teleport, one red chat line (*"The selftest could not set up its platform in the sky (<why>), so nothing
ran and you were not moved."*), the run ends, the snapshot restores. Then the PREFLIGHT, as before, on the pad: the
check that it is whole.

**THE RETURN, READ (P6).** Every end path runs the one restore, and it returns him to his RUN-START position and facing
(`SelfTestSnapshot.restore`: `teleportAsync(location)`, or `teleport` when `now`): FINISHED, STOPPED, an EXCEPTION
(`ABORTED in …`), and the server stop (`endAtDisable`, synchronous). **HIS QUIT: READ from the server jar, the old async
return could not be trusted** (`CraftEntity#teleportAsync` loads the destination's chunks and teleports in a callback;
`CraftPlayer#teleport0` returns false once his `connection` is null; his save follows the quit event), so he could have
been saved on the pad, **and the next run would then have taken the pad as its start**. Fixed here: the quit path is
`endAtQuit()`, whose return is SYNCHRONOUS, as at a server stop. Were he ever saved on the pad all the same, he would
rejoin on the floor inside the rim, which no interior cell can fall past (tested). **"Left the dev world":** the pad is in
the dev world, so standing on it does not trip the guard. **His death:** the run's guard aborts on it (`the player
died`); the restore runs on a dead player, and he respawns where vanilla puts him (his bed or the world spawn), not on
the pad and not at his start (READ: `SelfTestStaging.invalid`; where he respawns is vanilla's, NOT READ). Nothing on the
pad can kill him (SAFETY below).

**NOTHING HOSTILE ON IT, READ (P7).** During the run: the setup's kill lines remove every zombie, skeleton, spider,
creeper, enderman, witch, slime and phantom within 64 of him, which is all of the pad (its farthest cell is 48 from the
stand point); `gamerule spawn_mobs false` stops natural spawning; **and, ADDED, `gamerule spawn_phantoms false`, because
READ shows the need**: the server's `PhantomSpawner` reads `GameRules.SPAWN_PHANTOMS` and `canSeeSky`, never
`spawn_mobs`, and the pad is open sky at the run's night (18000). The snapshot saves and restores `spawn_phantoms` like
`spawn_mobs` (the RESTORED line names it). Between runs, natural spawning there needs a player near it (vanilla's
spawner works only near players: INFERRED, NOT READ this session), and nothing spawns on a barrier (INFERRED); a mob of
a type the kill lines miss (a stray, a husk, a bogged) on the pad at a run's start would be named by the first row's
`LEFTOVER` line. **No spawn-proofing beyond the phantom rule is added: READ showed no other need.**

**THE LINT (P9):** a scenario that opts into the pad has its preflight box and every summon inside the interior
(`ScenarioLint`, rule GROUND); one that does not is linted as before. Only `enchant-columns` opts in
(`SelfTestScenariosTest`).

### THE RUN SHEET, THIRD FORM — BEN TYPES THESE THREE LINES

**IN PLAIN WORDS:** **stand anywhere in the dev world.** When you type line 2, the selftest lifts you onto its own stone
platform high in the sky, runs, and puts you back exactly where you were when it ends. **You do not need to find any
ground.** Your inventory is emptied during the run (each row hands you what it needs) and **everything is given back at
the end**. Do nothing while it runs.

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.**
1. `/gamemode survival`
2. `/rpg selftest enchant-columns` — **ONCE.** About 105 seconds: you appear on the platform facing east, zombies appear
   and stand still, your weapons fire by themselves, and a lapis crystal is thrown and floats, four times.
3. `/stop`

If anything goes wrong before the run starts, you are told in red in chat and you are not moved; nothing ran.

**This run's length:** the second form's 1959 waited ticks (unchanged; recount as there) and about 5 per row (19: 95),
**about 2055 ticks (103 s)**, plus the pad's chunk loads and build at the start (NOT MEASURED: a first run builds 2066
blocks, a later one checks 7250 and writes none).

### SAFETY, THIRD FORM: WHERE EVERY ROW PUTS HIM, AND WHAT COULD REACH HIM

**Where:** every row's clean returns him to the STAND POINT, and every row that fires turns him there to face due east.
He is never anywhere else on the pad and never off it. **The fall he cannot take:** at floor level and each of the four
levels above, every interior cell's eight neighbours are interior or rim
(`SelfTestPadShapeTest.noInteriorCellIsBesideOpenAirAtFloorLevelOrTheFourAbove`, 1296 x 5 x 8 = 51,840 checks); nothing
in the run moves him off the stand point, and the rim's top is 4 above the floor, beyond a jump. **What could reach him:**
the second form's table, row for row, **0 x 0 = 0**; the natural hostiles (killed by the setup; spawning off; phantoms
off) **0**; the void below is behind a floor and a rim. **Sum: 0.**

### R0, FOURTH FORM (2026-11-23): R0aR4, R0bR4, R0cR4, R0dR4. R0e STANDS

#### R0aR4 — the build line names the pad's head, clean

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `<sha>` being `feat/selftest-pad`'s head as `git ls-remote --heads origin feat/selftest-pad` prints it, shortened as `git rev-parse --short HEAD` prints it. **Not `030f30b6`, not `e774d4b8`, not `-dirty`, not `unknown`.** Its precondition: `git status --porcelain` prints nothing and `git rev-parse HEAD` equals the `ls-remote` line | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **AS PREDICTED, the boot of `efa38636`:** `[20:34:09] [Server thread/INFO]: [Rpg] Build: efa38636`. Before the boot command: `git status --porcelain` printed nothing; `HEAD` and `ls-remote` both `efa38636ad45063623b1e65553f4896c1e82e626` |

#### R0bR4 — the jar carries the pad

**R0bR3's block with two more `ValueLine` calls (53 PRESENT):** `  - pad: ""` and `  - cmd: "gamerule spawn_phantoms
false"` in `GATE-enchant-columns.yml`, added after its `probe: "f 32"` line; its controls unchanged (18). Recount as R0bR3's:
`awk '/^.--- controls.$/{exit} /^ValueLine/' <the block> | wc -l` prints 53, and 18 after the controls line.

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
$lc = Bytes 'content/enchants/lapis_crystal.yml'
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
ValueLine $lc 'effect: grant_trigger' 'lapis_crystal.yml'
ValueLine $lc 'weapon: lapis_staff' 'lapis_crystal.yml'
ValueLine $lc '  input: left_click' 'lapis_crystal.yml'
ValueLine $lc '    cooldown_ticks: 600' 'lapis_crystal.yml'
ValueLine $lc '      amount: 30' 'lapis_crystal.yml'
ValueLine $lc '      type: orb' 'lapis_crystal.yml'
ValueLine $lc '      range: 12' 'lapis_crystal.yml'
ValueLine $lc '      life_ticks: 300' 'lapis_crystal.yml'
ValueLine $lc '        percent_by_level: [20, 30, 40]' 'lapis_crystal.yml'
ValueLine $ll '      type: projectile' 'living_lantern.yml'
ValueLine $ll '      speed: 0.35' 'living_lantern.yml'
ValueLine $ll '      max_lifetime_ticks: 80' 'living_lantern.yml'
ValueLine $ll '        search_radius: 7' 'living_lantern.yml'
ValueLine $ll '        amount: 45' 'living_lantern.yml'
ValueLine $tw '    key: entity.generic.explode' 'tremor_wave.yml'
ValueLine $eg 'EC3R2:' 'GATE-enchant-columns.yml'
ValueLine $eg 'EC16R2:' 'GATE-enchant-columns.yml'
ValueLine $eg 'LC1R2:' 'GATE-enchant-columns.yml'
ValueLine $eg 'LC4R2:' 'GATE-enchant-columns.yml'
ValueLine $eg '  - preflight: "0 -6 32 10"' 'GATE-enchant-columns.yml'
ValueLine $eg '  - clear_state: ""' 'GATE-enchant-columns.yml'
ValueLine $eg '  - probe: "f 32"' 'GATE-enchant-columns.yml'
ValueLine $eg '  - pad: ""' 'GATE-enchant-columns.yml'
ValueLine $eg '  - cmd: "gamerule spawn_phantoms false"' 'GATE-enchant-columns.yml'
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
ValueLine $eg 'EC1:' 'GATE-enchant-columns.yml CONTROL-RENAMED'
ValueLine $lc '      type: projectile' 'lapis_crystal.yml CONTROL-D1-STUB'
ValueLine $lc '    cooldown_ticks: 60' 'lapis_crystal.yml CONTROL-PREFIX'
ValueLine $lc '      range: 1' 'lapis_crystal.yml CONTROL-PREFIX'
ValueLine $eg 'EC0:' 'GATE-enchant-columns.yml CONTROL-LEFT'
ValueLine $eg 'EC1R2:' 'GATE-enchant-columns.yml CONTROL-LEFT'
ValueLine $eg 'LC1:' 'GATE-enchant-columns.yml CONTROL-LEFT'
ValueLine $eg 'LC4:' 'GATE-enchant-columns.yml CONTROL-LEFT'
ValueLine $eg 'EC17:' 'GATE-enchant-columns.yml CONTROL-LEFT'
$zip.Dispose()
```

**The prediction, line for line:** before `'--- controls'`, **53 `PRESENT`, 0 `ABSENT`, and one `ABSENT-ENTRY`**; after it,
**18 `ABSENT`, 0 `PRESENT`**. Run before the commit: see *R0bR4, RUN BEFORE COMMIT* at the end.

**READING, THE BOOT OF `efa38636`: AS PREDICTED.** Against the deployed `run\plugins\rpg-0.1.0-SNAPSHOT.jar` (2,491,100
bytes, sha256 `FB97BF5C…770160C7`), this block extracted from the committed file (`git show HEAD:…`, `cmp` IDENTICAL to
the copy run before the commit) and run with `powershell -File`: **53 `PRESENT`, 0 `ABSENT`, 1 `ABSENT-ENTRY`**, then **18
`ABSENT`, 0 `PRESENT`**.

#### R0cR4 — every Loaded line, and nothing refused

R0cR3's prediction unchanged (no content file moved), and **no `Refusing selftest/…` line**: `pad` is admitted by the
loader, or the setup would be refused whole and R0dR4 would still read 351 but the run would have no setup.

**READING, THE BOOT OF `efa38636`: AS PREDICTED** (every line at 20:34:09): `[Rpg] Loaded 11 abilities, 45 visuals, 5
statuses, 7 elements, 14 enchants, …`; the candidates line word for word; `Mint-on-craft: 38 indexed, 38 claiming, of 54
gear definitions`; `Custom recipes: 7 registered, 0 replaced, 0 refused, of 7 authored (7 minting gear)`; the only `[Rpg]`
WARN `volley_stone`'s pair; no `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Dev-only`; no `ORB` line. R0e: one
`FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`. The deployed folder: `content/enchants/` 14
`.yml`, 0 `dev_`, among them `airburst_flames.yml`, `kinetic_tremors.yml`, `lapis_crystal.yml`;
`enchant_candidates.yml:17:    chance: 0.10`; `living_lantern.yml:58:      speed: 0.35`.

#### R0dR4 — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 351 scenarios across 43 gates [melee-cell, …, unique-enchant-slot, enchant-columns]`** (the pad adds a setup step, not a row). The controls: `355 scenarios` and `350 scenarios` ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | **AS PREDICTED, the boot of `efa38636`:** exactly one, `[20:34:09] … [Rpg] Selftest: dev server, 351 scenarios across 43 gates [melee-cell, …, unique-enchant-slot, enchant-columns]` |

### THE SETUP'S LINES, PREDICTED EXACTLY, FOR A FIRST RUN AND A SECOND

`<sx>` and `<sz>` are world spawn's block x + 200 and its block z (logged, not predicted). The world is the dev world,
`world` (the boot log's `ServerLevel[world]`).

| run | prediction | READING |
|---|---|---|
| the FIRST run on a world with no pad | **`PAD world=world stand=<sx>,201,<sz> floorY=200 interior=-2..45 east -13..13 south cells=7250 placed=2066 repaired=0 ok=5184`** (1296 floor + 770 rim placed into air; the 5184 air cells already air: INFERRED for open sky at Y 200-204, and a reading if a peak or a tree reaches it, which would show as `repaired`), then **`PAD stood at=<sx>.50 201.00 <sz>.50 yaw=-90.000 pitch=0.000`**, then **`PREFLIGHT ok box=0..32 east -6..10 south air=1122/1122 floor solid=561/561`**; every row's `CLEAN … return=<sx>.50 201.00 <sz>.50` | **AS PREDICTED, the run of `efa38636` (20:38:26):** `PAD world=world stand=104,201,-4 floorY=200 interior=-2..45 east -13..13 south cells=7250 placed=2066 repaired=0 ok=5184`; `PAD stood at=104.50 201.00 -3.50 yaw=-90.000 pitch=0.000`; `PREFLIGHT ok box=0..32 east -6..10 south air=1122/1122 floor solid=561/561`; every row's `CLEAN … return=104.50 201.00 -3.50` (the setup's own, before the pad, `return=-98.48 63.00 24.06`). So world spawn's block is x -96, z -4 (READ from the stand: 104 - 200, -4) |
| a SECOND run (the pad left standing) | **`PAD world=world stand=<sx>,201,<sz> … cells=7250 placed=0 repaired=0 ok=7250`**, the same `PAD stood` line and the same `PREFLIGHT ok` line | |

**After the run:** the RESTORED line ends `… spawn_phantoms=<his world's value at RUN START>`, and he stands where he
typed the command (his eyes: the RESTORED line names no position, so where the restore put him is NOT AUTO-READ).

## THE RUN OF `efa38636`, 2026-11-23: EVERY ROW RAN, AND EVERY ROW READ AS PREDICTED

**The boot.** `./scripts/dev-server.sh --refresh-content` printed `==> Building efa38636`; the deployed jar 2,491,100
bytes, mtime 20:34:02, sha256 `FB97BF5C…770160C7`; this boot's log begins `[20:34:03]`, after the deploy; `Done (7.171s)`
at 20:34:09. The backup `../rpg-run-backups/2026-11-23-pre-enchant-columns-3`, a new folder: `Rpg` 168 files and `world`
193, each `diff -rq lines: 0`; the overworld 106. Every R0 line is stamped 20:34:09.

> **DATED NOTE, 2026-11-23 (`booting.md` step 8): HE JOINED BEFORE R0 WAS READ.** "BOOTING NOW" and "do NOT log in" went
> in their own message before the boot command (step 4, ticked). `BaronVonYeetus joined the game` at **20:34:11**, two
> seconds after `Done`, before R0 was read and before "R0 passed" was sent; he typed nothing, and left at 20:34:42. He
> joined again at **20:38:22**, after "R0 passed", and typed `/rpg selftest enchant-columns` at 20:38:26. **Every R0
> line predates the first join** (all at 20:34:09), the shape of the seat's ruling at `GATE-fight-arena.md`'s second
> boot. Every reading below is from after the second join.

**What he typed:** `/rpg selftest enchant-columns` (20:38:26) and `/stop` (20:40:13), and nothing else. **He did not type
`/gamemode survival`**: the guard admits only SURVIVAL, so he was in it already. One run, `SELFTEST RUN START
enchant-columns rows=18 selftest=efa38636` to `SELFTEST RUN FINISHED` at 20:40:09.

**The pad step's length:** `RUN START` and `PAD stood` are both stamped 20:38:26, and the whole setup (its clean, the
pad, the preflight, the commands, the kills) is `setup DONE ticks=9`. So the pad took **under 9 ticks** (its own ticks
are not logged).

**Every STAGE line, once per row:**
- `clear_inventory`: `cleared=11 kept=nexus@8` at EC3R2 (his own items), then `cleared=1 kept=nexus@8` at every later row
  (the last row's item).
- `clear_state`: `cooldowns=cleared streaks=forgotten orb=none` at the start of every firing row; at the end of LC1R2,
  LC2R2 and LC3R2 `orb=removed`, and at the end of LC4R2 `orb=none`.

**The lines outside the rows' predictions, every one:**
- **`LINGER work=1 entities=0 radius=48`** at the clean of LC2R2, LC3R2 and LC4R2: the row-start clean ended ONE
  point-scheduled chain round the stand point, after each row that threw an orb. Not predicted (every row predicts
  `LINGER work=0`). INFERRED: the removed orb's own scheduled work (its expiry or its shots' chain) outlives
  `OrbRemovals.remove`, and the clean ends it. **No reading moved by it.** A finding for the seat: what that chain is, and
  whether a removal should end it, is NOT READ.
- `MOBSEED e3ea719e-… drowned gs=111 source=stored max=111` (20:38:29, during EC5R2): a stored mob seeded where a chunk
  loaded. Every `LEFTOVER` line in the run reads `living=0`, so it was never within 16 of him.
- **Seventeen `MOBREMOVE` lines at 20:38:36** (during EC7R2) for mobs no row summoned. INFERRED: the chunks round where he
  typed the command unloading once he stood on the pad.
- **`SPLIT`: 3** (EC14R2's one, EC16R2's two). **`NEXUSHEAL`: 0. `SKIPPED`: 0. `LEFTOVER living=` above 0: none.**
- **Homing:** the lantern's shot LANDED in EC7R2 (on L) and in EC15R2 (on N); EC8R2's met nothing, as predicted; EC14R2's
  and EC16R2's flew straight and split.

**The RESTORED line, whole:** `SELFTEST RUN RESTORED xp=712697 cell=mage/fire gamemode=SURVIVAL held=2 mana=100.0
trace=false spawn_mobs=false time=1000 blocks=0 mobsRemoved=1 orbsRemoved=0 keep_inventory=false immediate_respawn=false
difficulty=NORMAL health=108.000 vanilla_xp=190+0.236 spawn_phantoms=true drag_tick=false`; then `SELFTEST RUN HEALTH
saved=108.000 customHP=108.000/350.000 wrote=108.000`. `spawn_phantoms=true` is his world's own value, put back. The log
names no position for him after the run.

**After his `/stop`:** `Stopping server` 20:40:13; `All dimensions are saved` 20:40:13; no `java` process; `find
run/world/dimensions/rpgfight -mindepth 1` printed nothing; the overworld 106 files, the same names as the backup's 106;
**no `cause=stop`**. **HE WAS NOT SAVED ON THE PAD, READ from his saved player file**
(`run/world/players/data/b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5.dat`, written 20:40:13): gunzipped, its `Pos` list reads
**`-98.4842527511075, 63, 24.0554157002669`**, exactly the position his login line gives and the run's first clean
returned him to (`return=-98.48 63.00 24.06`): where he typed the command, not the stand point `104.50 201.00 -3.50`.

**IN THREE CLASSES:** **READ AS PREDICTED: all eighteen** (EC3R2 to EC16R2, LC1R2 to LC4R2), and EC17's ORB-line
extension; EC13R2 and EC15R2 each on its allowed crit reading. **MISS: none. DID NOT TEST WHAT IT WAS MEANT TO: none.**

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
| `REPLY "Set 13 level(s) for <n> (now level 13, …)." [green]`; `PROBE self …` (recorded) | the old gate's setup | **AS PREDICTED (18:44:12-14):** `REPLY "Set 13 level(s) for BaronVonYeetus (now level 13, 0/2770 into it, 19980 lifetime)." [green]`; `SELF customHP=160.000/160.000 at=-98.48 63.00 24.06 onGround=true fall=0.000` (recorded; the line is `SELF`, not `PROBE self`); `trace on=true`, `time set 18000`, `spawn_mobs false`, `hold … 2->2 item=empty`, the kills (`No entity was found` each but the zombie's) |

### EC0 — THE SPOT (SURVIVAL)

> **EC0R2, 2026-11-23 (the seat's fix, before any boot): three probe lines ADDED to EC0's scenario, the first three
> unchanged.** The prediction below stands for them; EC0R2 is the added lines':
>
> | prediction | stands on | READING |
> |---|---|---|
> | `probe_blocks: "AIR 1 0 7 32 1 10"`: AIR at feet and head over 1..32 east, 7..10 SOUTH (his right as he faces east), **256** if all clear (32 x 4 x 2); `probe_blocks: "AIR 1 -1 7 32 -1 10"` and `"WATER 1 -1 7 32 -1 10"`: **0** each (a floor). **A short AIR count, or any AIR or WATER in that floor, VOIDS LC2's F half** (F's "nothing" is then not read: a block between the orb and F would refuse it for want of a clear line, the wrong reason). The counts describe the spot, not pass or fail | LC1's F at 12 east, 9 south | **READ (18:44:14):** `BLOCKS AIR count=256 box=1 0 7 32 1 10` (all clear); `BLOCKS AIR count=0 box=1 -1 7 32 -1 10`; **`BLOCKS WATER count=104 box=1 -1 7 32 -1 10`**: WATER in that floor, so by this row's text **LC2's F half is VOID** (moot on this run: LC1, which summons F, skipped) |

| prediction | stands on | READING |
|---|---|---|
| The counts describe the spot: AIR at feet and head over 1..32 east, 6 either side (**832** if all clear: 32 x 13 x 2), no AIR and no WATER in the floor. **A short count is not a failure; it voids the rows whose line it blocks** (EC8's 30 and EC14/EC16's 22 and 28) | the corridor | **READ (18:44:14):** the turn `tp … -90 0` (`Teleported BaronVonYeetus to -98.500000, 63.000000, 24.500000`); **`BLOCKS AIR count=832 box=1 0 -6 32 1 6`** (all clear); `BLOCKS AIR count=0 box=1 -1 -6 32 -1 6`; **`BLOCKS WATER count=255 box=1 -1 -6 32 -1 6`: NOT AS DESCRIBED, 255 of the floor's 416 blocks are water.** No line at feet or head is blocked, so no row is voided by it; the frozen zombies' probes read `dy=0.000` (they did not sink) |

**THE PROOF OF W2 IS THE ROLLED ITEMS OF EC1R2 AND EC2-EC4, NEVER THE DEV COMMAND (the seat's ruling 4, 2026-11-23).**
Each is a FRESH `rpg give`, rolled by the shipped roll, and read untouched. `/rpg enchant candidate` can stage any enchant
in any slot (a dev instrument); EC9-EC16 and LC1-LC4 use it only to make a behaviour ACTIVE, never to show which kind a
slot holds.

> **EC1 IS SUPERSEDED BY EC1R2, 2026-11-23, NEVER RUN:** it predicted slot 0 EMPTY because Lapis Crystal was not built.
> D1-D3 built it. Its scenario row was renamed EC1R2; EC1 is kept here as the record.

### EC1 — THE LAPIS STAFF'S COLUMNS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Lapis Staff -- 4 slot(s)" [aqua]`; **`REPLY "  Slot 0: []" [gray]`** exactly (Lapis Crystal was not built); **`REPLY "  Slot 1: [kinetic_tremors locked]" [gray]`** exactly; `Slot 2:`, `Slot 3:` each 1 or 2 of `attunement locked` / `unbreaking locked`, distinct; no `active:`; `REPLY "  raw: v1;:-1;kinetic_tremors=0:-1;…" [dark_gray]`. **`REPLY "Pool: attunement, unbreaking"`**, **`REPLY "Unique pool: kinetic_tremors"`**, **`REPLY "Weapon pool: "`** | W2; `UniqueSlotContentTest` | |

### EC1R2 — THE LAPIS STAFF'S COLUMNS, ROLLED, WITH LAPIS CRYSTAL (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Lapis Staff -- 4 slot(s)" [aqua]`; **`REPLY "  Slot 0: [lapis_crystal locked]" [gray]`** exactly (the weapon pool holds one, so the cascade's count is capped at one); **`REPLY "  Slot 1: [kinetic_tremors locked]" [gray]`** exactly; `Slot 2:`, `Slot 3:` each 1 or 2 of `attunement locked` / `unbreaking locked`, distinct; no `active:`; `REPLY "  raw: v1;lapis_crystal=0:-1;kinetic_tremors=0:-1;…" [dark_gray]`. **`REPLY "Pool: attunement, unbreaking"`**, **`REPLY "Unique pool: kinetic_tremors"`**, **`REPLY "Weapon pool: lapis_crystal"`** | W1, W2; `UniqueSlotContentTest` | **AS PREDICTED (18:44:15), a fresh `rpg give`:** `REPLY "Lapis Staff -- 4 slot(s)" [aqua]`; `"  Slot 0: [lapis_crystal locked]" [gray]`; `"  Slot 1: [kinetic_tremors locked]" [gray]`; `"  Slot 2: [attunement locked, unbreaking locked]" [gray]`; `"  Slot 3: [unbreaking locked]" [gray]`; no `active:`; `"  raw: v1;lapis_crystal=0:-1;kinetic_tremors=0:-1;attunement=0,unbreaking=0:-1;unbreaking=0:-1" [dark_gray]`; `"Pool: attunement, unbreaking" [gray]`, `"Unique pool: kinetic_tremors" [gray]`, `"Weapon pool: lapis_crystal" [gray]` |

### EC2 — THE LIVING LANTERN'S COLUMNS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Living Lantern -- 4 slot(s)" [aqua]`; **`REPLY "  Slot 0: [airburst_flames locked]" [gray]`**; **`REPLY "  Slot 1: []" [gray]`**; `Slot 2:`/`Slot 3:` over `attunement`/`unbreaking`; `raw: v1;airburst_flames=0:-1;:-1;…`. **`Pool: attunement, unbreaking`**, **`Unique pool: `**, **`Weapon pool: airburst_flames`** | W1, W2 | **AS PREDICTED (18:44:15), a fresh `rpg give`:** `REPLY "Living Lantern -- 4 slot(s)" [aqua]`; `"  Slot 0: [airburst_flames locked]" [gray]`; `"  Slot 1: []" [gray]`; `"  Slot 2: [attunement locked, unbreaking locked]" [gray]`; `"  Slot 3: [unbreaking locked, attunement locked]" [gray]`; `"  raw: v1;airburst_flames=0:-1;:-1;attunement=0,unbreaking=0:-1;unbreaking=0,attunement=0:-1" [dark_gray]`; `"Pool: attunement, unbreaking"`, `"Unique pool: "`, `"Weapon pool: airburst_flames"`, each `[gray]` |

### EC3 — AN IRON SWORD'S COLUMNS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Iron Sword -- 4 slot(s)" [aqua]`; **`Slot 0: []`**; **`Slot 1: [kinetic_tremors locked]`**; `Slot 2:`/`Slot 3:` over `sharpness`/`unbreaking`; `raw: v1;:-1;kinetic_tremors=0:-1;…`. **`Pool: sharpness, unbreaking`**, **`Unique pool: kinetic_tremors`**, **`Weapon pool: `** | W2 | **NOT RUN (18:44:16):** `EC3 SKIPPED no hotbar slot holds empty` (see *THE RUN OF `e774d4b8`*) |

### EC4 — A PIECE OF ARMOR'S COLUMNS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`HELDID slot=<n> item=iron_chestplate`** (anything else VOIDS the row); `REPLY "Iron Chestplate -- 4 slot(s)" [aqua]`; **`Slot 0: []`**, **`Slot 1: []`**; `Slot 2:`/`Slot 3:` each 1 to 4 of `growth`/`mana_bank`/`protection`/`unbreaking`, distinct; `raw: v1;:-1;:-1;…`. **`Pool: growth, mana_bank, protection, unbreaking`**, **`Unique pool: `**, **`Weapon pool: `** | W2; U5 | **NOT RUN (18:44:16):** `EC4 SKIPPED no hotbar slot holds empty` |

### EC5 — THE TABLE ON AN IRON SWORD: NO LABEL, THE WEAPON MARKER, TREMORS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `SCREEN EnchantMenu title="Enchantments" [dark_gray]`; **NO `SLOT 1`** (W3: the label removed), and none at 0, 2, 3, 5, 6, 9; `SLOT 4 IRON_SWORD name="Iron Sword" …`; `SLOT 7 BOOKSHELF …` and `SLOT 8 ENCHANTING_TABLE …` as `GATE-unique-enchant-slot.md` US5 predicted them; **`SLOT 10 GRAY_DYE name="No weapon enchant" [dark_gray] lore=["Nothing is tied to this weapon yet." [dark_gray]]`**; **`SLOT 12 PISTON name="Kinetic Tremors" [dark_gray] lore=["3 hits on one foe in 3s: a 10 damage shockwave, 3 blocks" [gray]; "" []; "Locked. Click to unlock at I -- 352 XP." [dark_gray]]`**; **no `SLOT` at 19 or 21**; `14`/`16` and, where that slot rolled two, `23`/`25`: `IRON_SWORD "Sharpness"` (lore `"+5% damage, x1.05" [gray]`) or `ANVIL "Unbreaking"`, `[dark_gray]`, third lore line `"Locked. Click to unlock at I -- 352 XP."`; no `SLOT` at 28-43; `SLOT 48 ARROW …`, `SLOT 49 BARRIER …` | W3; `EnchantMenuLayoutTest`; `KineticTremorsContentTest` (the line); the old gate's A (352) | **NOT RUN (18:44:16):** `EC5 SKIPPED no hotbar slot holds empty` |

### EC6 — THE TABLE ON A LIVING LANTERN: AIRBURST, THE UNIQUE MARKER (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `SCREEN EnchantMenu …`; **NO `SLOT 1`**; `SLOT 4 LANTERN name="Living Lantern" …`; **`SLOT 10 FIRE_CHARGE name="Airburst Flames" [dark_gray] lore=["splits after 14 blocks into 3 bursts of 25 damage" [gray]; "" []; "Locked. Click to unlock at I -- 352 XP." [dark_gray]]`**; **`SLOT 12 GRAY_DYE name="No unique enchant" [dark_gray] lore=["Nothing unique is tied to this item's element yet." [dark_gray]]`**; no `SLOT` at 19 or 21; `14`/`16` (and `23`/`25` where two rolled): `BLAZE_ROD "Attunement"` (lore `"+5% damage, x1.05" [gray]`) or `ANVIL "Unbreaking"`; 48, 49 | W3; `AirburstFlamesContentTest` (the line) | **NOT RUN (18:44:16):** `EC6 SKIPPED no hotbar slot holds empty` |

### EC7 — THE LANTERN'S SHOT: ONE HIT OF 45 ON A MOB OFF HIS AIM LINE (SURVIVAL)

**Begins from a cleared cooldown:** the lantern's first press of the run.

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Set 1 level(s) …"` (recorded); `rpg enchant show` prints **no `active:` line** (a fresh roll: every candidate locked, so enchant 1.0 and no behaviour). Probe L before: **`1000.000/1000.000`**. `DRIVE use … item=living_lantern`. **Exactly one PLAYERHIT `source=living_lantern/right_click target=<L> zombie element=fire sent=45.000 crit=false triggerScore=100`** (or `sent=90.000 crit=true`). **The sum:** `(45 x 100/100) x 1.0 + 0 + 0 = 45`, x charge 1.0, x crit 1.0 (2.0). Probe L at +60: **recorded** (45 and whatever burns of 22.5 have landed; the hit's tick is a reading). **The exact half is WHAT it lands; WHETHER it lands is a READING** (F: in the world it homes at the feet from his eye) | B; `LivingLanternFlightTest`; D | **NOT RUN (18:44:18):** its first steps ran (`REPLY "Set 1 level(s) for BaronVonYeetus (now level 1, 0/1000 into it, 0 lifetime)." [green]`; `SELF customHP=100.000/100.000 …`), then `EC7 SKIPPED no hotbar slot holds empty`: no give, no turn, no press. **He stayed at level 1 for every later row** |

### EC8 — BEYOND 28 BLOCKS: NOTHING (SURVIVAL)

**Begins from a cleared cooldown:** 40 ticks after EC7's press (the cooldown is 16) plus EC8's own waits.

| prediction | stands on | READING |
|---|---|---|
| Probe F: **`1000.000/1000.000`** before and after. `DRIVE use …`; **NO PLAYERHIT** in the row (F stands 30 east, its near face 29.7 from his eye; the shot ends at 28.0 of travel, READ `ProjectileFlight`, `LivingLanternContentTest.theReachIsTwentyEightBlocksOfTravel`). **Exact**: homing only bends a path, never lengthens it | B | **VOID BY A SKIPPED ROW (18:44:18-26):** F summoned 30 east (`MOBSEED ebcd4d0d-… zombie gs=100 source=rolled max=1000`); `PROBE tag=f none within 16 blocks` before and after (**a probe sees 16 blocks: F's health is unreadable on any run**); `DRIVE use … item=living_lantern yaw=128.291 pitch=8.273` (the lantern still held from EC2; NOT facing east: the turn was EC7's); **no PLAYERHIT**, recorded, not read: the shot did not fly the line it predicts |

### EC9 — KINETIC TREMORS AT LEVEL I: THREE HITS, ONE SHOCKWAVE, ITS RADIUS (SURVIVAL)

**Begins from a cleared cooldown:** the Lapis Staff's first press of the run; then 22 between presses (its cooldown is 20).

| prediction | stands on | READING |
|---|---|---|
| `REPLY "  Slot 1: [*kinetic_tremors I]" [gray]` and **`REPLY "  active: kinetic_tremors I (3 hits on one foe in 3s: a 10 damage shockwave, 3 blocks)" [gray]`**. **Three PLAYERHITs `source=lapis_staff/right_click target=<A> zombie element=kinetic sent=21.000`** (each `42.000 crit=true` on its own crit): `(21 x 100/100) x 1.0 + 0 + 0`. **After the third, exactly TWO PLAYERHITs `source=kinetic_tremors/shockwave … element=kinetic`, one on A and one on B, each `sent=10.000`** (`20.000 crit=true` if the THIRD beam crit; both equal): `(10 x 100/100) x 1.0 + 0 + 0 = 10`, x 1.0, x that beam's crit. **None on C** (4 from A, outside 3). **No shockwave after the first or second beam.** Probes: A `1000 - 63 - 10 = 927.000` (more on crits, by the crits named), B **`990.000`** (or 980.000), C **`1000.000`** | C's rule; `BehaviourHitsTest`; E | **NOT RUN (18:44:26):** `EC9 SKIPPED no hotbar slot holds empty` (its CLEAN removed EC8's F). No staff held, no Tremors staged, no turn |

### EC10 — TWO HITS, 61 TICKS, ONE HIT: NO SHOCKWAVE (SURVIVAL)

**Begins from a cleared cooldown:** 40 after EC9's last press.

| prediction | stands on | READING |
|---|---|---|
| **Three PLAYERHITs `source=lapis_staff/right_click target=<D>` sent=21.000** (or 42.000) and **NO `kinetic_tremors/shockwave` PLAYERHIT** (the third press is 83 and 61 ticks after the first two, both outside the 60: `ShockwaveStreaksTest.twoHitsThenSixtyOneTicksThenOneFiresNothing`). Probe D **`937.000`** (no crit) | C's rule | **VOID BY A SKIPPED ROW (18:44:26-33): THE WRONG WEAPON, AND NO TREMORS.** EC9's `hold: lapis_staff` and staging never ran, so the three presses were the Living Lantern's (`item=living_lantern`, yaws `128.291`, `128.291`, `-88.129`): **three PLAYERHITs `source=living_lantern/right_click target=<D> zombie element=fire sent=45.000 crit=false triggerScore=100`** (18:44:28, :30, :32), four `DOTTICK … status=scorch … sent=22.500 cap=22.500`, **no `kinetic_tremors/shockwave`**, probe D **`customHP=775.000/1000.000`** (`1000 - 3 x 45 - 4 x 22.5`). Recorded; not read against the staff's prediction. **Each of the three lantern shots landed** (D 3 out) |

### EC11 — ONE BREACHER SHOT ON ONE MOB: EXACTLY ONE SHOCKWAVE (SURVIVAL)

**Begins from a cleared cooldown:** the Breacher's first press of the run.

| prediction | stands on | READING |
|---|---|---|
| `active: kinetic_tremors I (…)`. **Four PLAYERHITs `source=breacher/right_click target=<E> zombie element=kinetic sent=40.000 crit=false`** (all four `80.000 crit=true` on the crit: a spread crits as a unit). **Exactly ONE `kinetic_tremors/shockwave` PLAYERHIT, on E, `sent=10.000`** (`20.000` on the crit, the second allowed reading): `(10 x 100/100) x 1.0 + 0 + 0 = 10`, x charge 1.0, x the shot's crit (D2: score, enchant, class, level, charge and crit all the triggering pellet's). The third pellet fires it; the fourth meets the 5-tick cooldown (`ShockwaveStreaksTest.fourPelletsInOneTickFireExactlyOne`). Probe E **`830.000`** (`1000 - 160 - 10`), or **`660.000`** on the crit | Ben's "Multiple pellets count"; `GATE-breacher-rework.md` RW2 (the four PLAYERHITs) | **NOT RUN (18:44:33):** `EC11 SKIPPED no hotbar slot holds empty`. No Breacher press in the run |

### EC12 — HITS SPLIT ACROSS TWO MOBS: NONE (SURVIVAL)

**Begins from a cleared cooldown:** the Lapis Staff, 40 after EC10's last press; 22 between presses.

| prediction | stands on | READING |
|---|---|---|
| Three `lapis_staff/right_click` PLAYERHITs: on G, on H, on G (each sent=21.000 or 42.000). **NO shockwave** (G holds two, H one). Probes G `958.000`, H `979.000` (no crit). **If `aim: g` lands a beam on H (or none), the row is VOID, not failed** | C's rule ("Same target") | **VOID BY ITS OWN TEXT (18:44:33-39): THE FIRST BEAM LANDED NONE.** The row's own steps ran (`hold … 3->2 item=lapis_staff`, mana refill, the turn, G at `~3 ~ ~-1`, H at `~3 ~ ~1`). **Each `use` read the PREVIOUS facing** (`aim:` then `use:` with no `wait:`; the aim is a `teleportAsync`): `aim … target=<G> … yaw=-108.435`, then `use … yaw=-90.000` (no hit); `aim … <H> yaw=-71.565`, then `use … yaw=-108.435`: **PLAYERHIT on G `sent=42.000 crit=true`**; `aim … <G>`, then `use … yaw=-71.565`: **PLAYERHIT on H `sent=42.000 crit=true`**. No shockwave (Tremors was not staged: EC9 skipped). Probes **G `958.000`, H `958.000`**. A defect of the scenario, found here: a `wait:` belongs between `aim:` and `use:` |

### EC13 — KINETIC TREMORS AT LEVEL III: DAMAGE 20, RADIUS 5 (SURVIVAL)

**Begins from a cleared cooldown:** 40 after EC12's last press.

| prediction | stands on | READING |
|---|---|---|
| `active: kinetic_tremors III (3 hits on one foe in 3s: a 20 damage shockwave, 5 blocks)`. Three beams on A as EC9. **Exactly THREE shockwave PLAYERHITs, on A, B and C, each `sent=20.000`** (`40.000` on the third beam's crit): `(20 x 100/100) x 1.0 + 0 + 0 = 20`. **None on K** (6 from A). **Against EC9: C (4 from A) took nothing at I and 20 at III.** Probes: A `917.000`, B `980.000`, C `980.000`, K `1000.000` (no crit) | C's rule; the seat's placeholders 3/5 and 10/20 | **VOID BY A SKIPPED ROW (18:44:39-44): TREMORS AT III BUT NEVER ACTIVE.** `REPLY "Slot 1: Kinetic Tremors unlocked to III." [green]`, then `"  Slot 1: [kinetic_tremors III]" [gray]` (no `*`) and **no `active:` line**; `raw: v1;lapis_crystal=0:-1;kinetic_tremors=3:-1;…`: the staging that made it active (`rpg enchant active 1 0`) was EC9's. Three beams `yaw=-90.000`: **three PLAYERHITs `source=lapis_staff/right_click target=<A> zombie element=kinetic sent=21.000 crit=false triggerScore=100`**, **no shockwave**. Probes A **`937.000`**, B `1000.000` (d 3.606), C `1000.000` (d 5.000), K `1000.000` (d 6.708). Recorded; not read as a miss of the code: the behaviour was not active |

### EC14 — AIRBURST FLAMES AT LEVEL II: NO 45, A SPLIT AT 14, THE CENTRE BURST'S 30 (SURVIVAL)

**Begins from a cleared cooldown:** the lantern was last pressed in EC8.

| prediction | stands on | READING |
|---|---|---|
| `REPLY "  Slot 0: [*airburst_flames II]" [gray]` and **`active: airburst_flames II (splits after 14 blocks into 3 bursts of 30 damage)`**. `PROBE self` gives his feet `<X, Y, Z>`. **Exactly one `SPLIT caster=<him> source=living_lantern/right_click at=<X+14.000>,<Y+1.620>,<Z> bursts=3 tick=<t>`** (his eye is his feet + 1.62, INFERRED standing; he faces due east at pitch 0; no mob within 7 of the shot before 14, so it never bent; gravity 0: EXACT). `t` is 39 ticks after the press (F; the press's tick is not logged, so t is RECORDED). **Exactly one PLAYERHIT, on M: `source=living_lantern/right_click … element=fire sent=30.000`** (`60.000 crit=true`): `(30 x 100/100) x 1.0 + 0 + 0`. **NO `sent=45.000`.** The side bursts pass M 1.70 off (8 x tan 12 degrees) and land nothing | E's rule; `AirburstTest` | **NOT RUN (18:44:44):** `EC14 SKIPPED no hotbar slot holds empty`. No lantern held, no Airburst staged, no turn, no `SPLIT` |

### EC15 — A HIT BEFORE 14 BLOCKS: THE 45, NO SPLIT (SURVIVAL)

**Begins from a cleared cooldown:** 80 + 20 after EC14's press.

| prediction | stands on | READING |
|---|---|---|
| **One PLAYERHIT on N `sent=45.000`** (`90.000`), **NO `SPLIT`** line in the row. **WHETHER it lands is a READING** (N is within 7 from the muzzle, so the shot homes at its feet from his eye, F); if it misses N it flies on and splits at 14: that is NOT a defect of the rule, and the row is VOID | Ben's "Doesn't split if it hits something first" | **VOID BY A SKIPPED ROW (18:44:44-48):** EC14's hold and turn never ran, so the press was the Lapis Staff's (`item=lapis_staff yaw=128.291 pitch=8.273`), N 6 east: **no PLAYERHIT, no `SPLIT`**, probe N **`1000.000/1000.000`** (d 6.000). Recorded, not read |

### EC16 — LEVEL I AGAINST LEVEL III (SURVIVAL)

**Begins from a cleared cooldown:** 40 after EC15's press, then 82 between the two presses.

| prediction | stands on | READING |
|---|---|---|
| Two `SPLIT … bursts=3` lines, each at `<X+14.000>,<Y+1.620>,<Z>` as EC14. **The first press's PLAYERHIT on P `sent=25.000`, the second's `sent=35.000`** (each doubled on its own crit). Probe P: `1000 - 25 - 35 = 940.000` and the burns (recorded) | Ben's 25/30/35 | **VOID BY A SKIPPED ROW (18:44:48-58): THE STAFF HELD, SO ITS LEVEL STEPS WROTE LAPIS CRYSTAL.** `REPLY "Slot 0: Lapis Crystal unlocked to I." [green]`, later `"… unlocked to III."` (slot 0 of the STAFF, not the lantern's Airburst); first press `item=lapis_staff yaw=-107.723 pitch=10.225`: nothing; second `yaw=-90.006 pitch=0.014`: **one PLAYERHIT `source=lapis_staff/right_click target=<P> … element=kinetic sent=21.000 crit=false`**; **no `SPLIT`**; `PROBE tag=p none within 16 blocks` (P stands 22 out: **unreadable on any run**) |

### LC1 — LAPIS CRYSTAL'S THROW: 30 MANA, ITS OWN 600-TICK COOLDOWN, AT REST 12 OUT (SURVIVAL)

**Begins from a cleared cooldown:** the staff has never been pressed in this run (the throw's id is its own,
`lapis_crystal/left_click`). The second press is 20 ticks after the first, INSIDE the 600: it is meant to be refused.

| prediction | stands on | READING |
|---|---|---|
| `REPLY "  Slot 0: [*lapis_crystal II]" [gray]`, `REPLY "  Slot 1: [*kinetic_tremors I]" [gray]`, and **`REPLY "  active: lapis_crystal II (grants Left-Click: Lapis Crystal (Cooldown: 30.0s \| Mana Cost: 30); a beam into it strikes each foe within 8 for 6.3 damage)" [gray]`** (the number, not the percent: 21 x 30 / 100, EXECUTED). **The tooltip (B5, `dump: held`):** the staff's own lines, then **`"Lapis Crystal  Left-Click" [gold, yellow]; "Throws a lapis crystal" [gray]; "that floats where it lands." [gray]; "Cooldown: 30.0s \| Mana Cost: 30" [dark_gray]`** (the granted ability's block, D1). `PROBE self` gives his feet `<X, Y, Z>`. **`MANA current=<m>`, `DRIVE reload held=lapis_staff`, `MANA current=<m - 30.000>`** (Ben's 30; the two probes are one tick apart with no wait between, so no regeneration lands between them: INFERRED from the step runner, which runs steps until a `wait:`; if a tick did pass, the second reads 30 less plus that tick's regeneration only). The second `DRIVE reload`: **nothing thrown, nothing spent** (the cooldown refuses it silently: `WeaponSwingListener` ignores an `OnCooldown`, READ); the third `MANA` reads the second plus 20 ticks of regeneration only (recorded), **never 30 less**. **Exactly one `ORB rest owner=<him> at=<X+12.000>,<Y+1.620>,<Z> tick=<t>`** (his eye is his feet + 1.62, INFERRED standing; due east at pitch 0; the strip is air, so no block stops it; EXACT), **10 ticks after the first press** (the flight; the press's tick is not logged, so `t` is RECORDED). **No second `ORB` line from the refused press** | D1, D2; `OrbThrowTest`; `LapisCrystalContentTest` | **NOT RUN (18:44:58):** `LC1 SKIPPED no hotbar slot holds empty` (its CLEAN removed EC16's P). No staging, no tooltip dump, no `MANA` line, no throw, **no `ORB` line** |

### LC2 — A BEAM INTO THE ORB: N TAKES THE CRYSTAL'S 6.3, F AND T NOTHING, THE BEAM NOTHING (SURVIVAL)

**Begins from a cleared cooldown:** the Lapis Beam's first press of the run.

| prediction | stands on | READING |
|---|---|---|
| `DRIVE use … item=lapis_staff`. **NO `lapis_staff/right_click` PLAYERHIT** (the beam stops at the orb: it passes the orb's centre exactly, 0 from it, inside 0.6, before reaching T 22 east). **Exactly one PLAYERHIT `source=lapis_crystal/crystal target=<N> zombie element=kinetic sent=6.300 crit=false triggerScore=100`**, or **`sent=12.600 crit=true`** (the second allowed reading: the BEAM's crit). **The sum (D2):** `(21 x 30 / 100 = 6.3) x 100/100 x 1.0 + 0 + 0 = 6.3`, x charge 1.0, x the beam's crit: amount 21 (the staff's right click, read when it met the orb), percent 30 (level II), score 100 (staged), enchant 1.0 (Lapis Crystal and Kinetic Tremors are behaviours, not damage enchants), class 0, level 0 (level 1). **Nothing on F** (its feet 9.14 from the orb's centre: sqrt(9² + 1.62²), outside 8) **or T** (10.13: beyond 8, and behind the orb). N's feet are 5.26 from it (sqrt(5² + 1.62²)). Probes: N `993.700` (or `987.400`), F `1000.000`, T `1000.000`. **ADDED 2026-11-23 (the seat's fix):** (a) **F's half is VOID, not read, if EC0R2's added probes are short** (a block in 7..10 south could refuse F for want of a clear line). (b) **What F's "nothing" proves, and what it does not:** F stands 0 east and 9 south of the orb's centre, so it is OUTSIDE the server's BOX of 8 as well as the sphere (|dz| 9 > 8): the server never even offers F to the cut, and F proves only that the 8 is not exceeded. **No mob in this gate stands inside a box but outside its sphere** (EC9's C: dz 4 against a box of 3; EC13's K: dz 6 against 5; F: dz 9 against 8; all outside their boxes too), **so the box-to-sphere cut is UNREAD ON A SERVER**: it is held only in the unit loop (`BehaviourHitsTest.theRadiusIsCutToASphereWhenTheWorldAnswersWithABox`, `OrbBeamTest.theEightIsCutToASphereWhenTheWorldAnswersWithABox`, each with `FakeWorld.boxSearch`) | D3; `OrbBeamTest` | **VOID BY A SKIPPED ROW (18:44:58-59): NO ORB AND NO MOBS** (both were LC1's). `use … item=lapis_staff yaw=128.291 pitch=8.273`; no PLAYERHIT; `PROBE tag=n`, `tag=f`, `tag=t`: each `none within 16 blocks`. F's half was also VOID by EC0R2's water |

### LC3 — A SECOND AND A THIRD BEAM FIRE IT AGAIN, AND NO SHOCKWAVE FROM CRYSTAL SHOTS ALONE (SURVIVAL)

**Begins from a cleared cooldown:** 22 ticks after each beam (the beam's cooldown is 20).

| prediction | stands on | READING |
|---|---|---|
| **Two more `lapis_crystal/crystal` PLAYERHITs on N**, each `sent=6.300` (or `12.600` on its own beam's crit): the orb is not used up. **NO `kinetic_tremors/shockwave` PLAYERHIT** although Kinetic Tremors is active and N took three shots inside 60 ticks: crystal shots are secondary, never hits (B4). **No `lapis_staff/right_click` PLAYERHIT.** Probe N `981.100` (`1000 - 3 x 6.3`, no crit) | D3, B4; `OrbBeamTest.crystalShotsAloneNeverFireAShockwave` | **VOID BY A SKIPPED ROW (18:44:59-45:02):** two `use … item=lapis_staff yaw=128.291 pitch=8.273`; no PLAYERHIT, no shockwave; `PROBE tag=n none within 16 blocks` (never summoned) |

### LC4 — GONE 300 TICKS AFTER ARRIVING, AND THEN THE BEAM PASSES (SURVIVAL)

**Begins from a cleared cooldown:** 300 ticks after LC3's last beam.

| prediction | stands on | READING |
|---|---|---|
| **Exactly one `ORB removed owner=<him> at=<LC1's point> cause=expired tick=<t + 300>`** (`t` LC1's rest tick: Ben's 15 s counted from arrival). Then **one PLAYERHIT `source=lapis_staff/right_click target=<T> zombie element=kinetic sent=21.000`** (or `42.000` on the crit): `(21 x 100/100) x 1.0 + 0 + 0`, and **no `lapis_crystal/crystal`**. No shockwave (T took one hit). Probe T `979.000` (or `958.000`) | D2; `OrbThrowTest` | **VOID BY A SKIPPED ROW (18:45:02-18):** the 300-tick wait, then `use … item=lapis_staff yaw=128.291 pitch=8.273`; **no `ORB removed` line** (no orb was thrown); no PLAYERHIT; `PROBE tag=t none within 16 blocks` (never summoned, and 22 out) |

### EC17 — NOTHING AN ENCHANT SPAWNS IS LEFT (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`PROBE entities item_display … count=0`**, **`block_display … count=0`**, **`marker … count=0`** (G). Dropped items are not counted (a `/kill` of a zombie drops its loot). **EXTENDED (D2): the run's `ORB` lines are exactly LC1's `ORB rest` and LC4's `ORB removed … cause=expired`, and no other**: no orb stands at the run's end. **After `/stop` (R0e): NO `ORB removed … cause=stop`** (none stands to remove), and at the NEXT enable no `cause=orphan` | B6; G; D2 | **AS PREDICTED in its counts (18:45:18):** `ENTITIES ITEM_DISPLAY count=0 radius=64 dying=0`, `BLOCK_DISPLAY count=0 …`, `MARKER count=0 …`. **Its D2 extension is not read:** the run's `ORB` lines are none, not LC1's and LC4's (LC1 skipped, so no orb was thrown). After `/stop`: no `cause=stop` |

## UNREAD BY DESIGN

- **Lapis Crystal** (since D1-D3): a REAL left click (the `reload` step drives the shipped handler, minus the packet);
  the orb as he SEES it (an item marker, heart of the sea); the removals on his quit, death and world change and at
  stop (no row causes them: `OrbRemovalWiringTest` reads their wiring; a stop with no orb standing writes nothing);
  the sweep of an orphan body (none can exist without a crash).
- **A beam aimed AT a mob in front of the orb** (the orb shields nothing in front of what the beam strikes):
  `OrbBeamTest` and `OrbIntercept`, not this boot.
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

## R0bR2, RUN BEFORE COMMIT

**Against `paper\target\rpg-0.1.0-SNAPSHOT.jar` (2,453,448 bytes, 2026-11-23 17:04), built by `./mvnw -B clean verify`
from the working tree of `feat/lapis-crystal-gate` (on #259's head) with all of D's rows, the renamed EC1R2 and the R2
rows (the suite in the same build: core 2,411, storage 99, paper 1,858, 0 failures, 1 skipped):** R0bR2's block,
extracted verbatim from this file (`awk` from its heading to the fence) and run with `powershell -File`, printed **49
`PRESENT`, 0 `ABSENT` and 1 `ABSENT-ENTRY`** before `'--- controls'` and **13 `ABSENT`, 0 `PRESENT`** after it, exactly
as predicted. R0dR2's 355 is `SelfTestScenariosTest`'s assertion in that same build (green). The boot reads both again
on the deployed jar.

**R0bR2 RE-RUN AFTER THE SEAT'S FIX (2026-11-23, EC0R2's probes, the run sheet's words, LC2's notes):** against
`paper\target\rpg-0.1.0-SNAPSHOT.jar` (2,453,555 bytes, 17:20), built by `./mvnw -B clean verify` from the working tree of
`feat/lapis-crystal-gate` with this fix (the suite in the same build: core 2,411, storage 99, paper 1,858, 0 failures, 1
skipped), R0bR2's block re-extracted verbatim and run with `powershell -File`: **49 `PRESENT`, 0 `ABSENT`, 1
`ABSENT-ENTRY`**, then **13 `ABSENT`, 0 `PRESENT`**, as predicted. R0dR2 unmoved: 398 keys, 43 setups, 355 (the fix adds
probe lines, not rows).

## R0bR3, RUN BEFORE COMMIT

**Against `paper\target\rpg-0.1.0-SNAPSHOT.jar` (2,475,077 bytes, 2026-11-23 19:36, sha256 `CA9F8299…4398D1454`), built by
`./mvnw -B clean verify` from the working tree of `feat/enchant-columns-instrument` (on #260's head `be6180b3`) with the
instrument, the rewritten scenario and this gate's second form (the suite in the same build: core 2,411, storage 99, paper
1,889, 0 failures, 1 skipped):** R0bR3's block, extracted verbatim from this file (`awk` from its heading to the fence; 91
lines, 51 `ValueLine` before the controls line and 18 after, counted before the scan ran) and run with `powershell
-File`: **51 `PRESENT`, 0 `ABSENT`, 1 `ABSENT-ENTRY`**, then **18 `ABSENT`, 0 `PRESENT`**, as predicted. R0dR3's 351 is
`SelfTestScenariosTest`'s assertion in that same build (green), and so is `ScenarioLint`'s zero for this gate. That jar's
stamp is `unknown` (a plain `mvnw`); the boot rebuilds it with the head, and reads every R0 again on the deployed jar.

## R0bR4, RUN BEFORE COMMIT

**Against `paper\target\rpg-0.1.0-SNAPSHOT.jar` (2,491,092 bytes, 2026-11-23 20:05, sha256 `10C2CC6E…08F56754B`), built by
`./mvnw -B clean verify` from the working tree of `feat/selftest-pad` (on #261's head `030f30b6`) with the pad, its
tests and this gate's third form (the suite in the same build: core 2,411, storage 99, paper 1,908, 0 failures, 1
skipped):** R0bR4's block, extracted verbatim (93 lines; 53 `ValueLine` before the controls line and 18 after, counted
before the scan ran; `diff` against R0bR3's block: exactly the two added lines) and run with `powershell -File`: **53
`PRESENT`, 0 `ABSENT`, 1 `ABSENT-ENTRY`**, then **18 `ABSENT`, 0 `PRESENT`**, as predicted. `ScenarioLint`: 0 findings for
this gate in the same build; R0dR4's 351, the same build's assertion. The stamp is `unknown`; the boot reads every R0
again on the deployed jar.
