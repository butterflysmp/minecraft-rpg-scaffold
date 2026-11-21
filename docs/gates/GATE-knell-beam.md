# GATE — THE KNELL BEAM (feat/knell-beam), ITS MECHANISMS, THE OWED FIXES, AND THE OWED READINGS

**Status: NOT RUN.** Written 2026-11-21 on `feat/knell-beam` (PR 3), stacked on `feat/knell-mechanisms` (PR 2), stacked
on `feat/knell-owed-fixes` (PR 1), on master `141308e1` (the squash of #227), before any boot. **This file carries the
ONE R0 and the ONE run sheet for the three.**

**NO BOOT until the seat has diffed the code AND this gate from origin, and Ben says "ready" in his own words**; then
only CC boots, by `.claude/rules/booting.md`. **NO MERGE without Ben's own words.**

**Game mode:** SURVIVAL, every row. **Every row is AUTO** (`/rpg selftest knell-beam`); **Ben types three lines**, and
his answer to the one question when the selftest asks it. **Time:** the scenario's setup sets night (18000) and
`spawn_mobs false`; the run's restore puts both back. **Every mob is a frozen NoAI zombie; no fight world.**

**Ben's words, verbatim (2026-11-21, spelling as typed),** recorded in `PLAN-weapon-tiers.md` 1.7:
- "slight detour in that the next weapon isn't apart of our current progress tree focus. Let's bring the Knell Beam
  from the old repo into the new one"
- "1. Slight rework, I want the windup time to match the Knell mob in our repo and the cooldown to be double the wind
  up time. Also I want it to require more accuracy. The old beam was very forgiving and because of that, very over
  powered. I still want it to have decent coverage because of stuff like lag and desync but it shouldn't be a "Kill
  everything in that direction" weapon / 2. Void Weapon / 3. Nothing right now, It will be a dragon drop later / 4.
  ignore the crystals for now / 5. yes"
- On the selftests, honoured here: "these self tests have lost a lot of their automatic components since the handoff"
  and "i'm not typing the lines in chat just read them".

## WHAT THE THREE PRs DO

**PR 1, `feat/knell-owed-fixes` (the owed fixes):**
- `/rpg firerate` writes every line it shows to the server log too, as **`FIRERATE player=<name> <line>`**
  (`RpgCommand.say`). HF1 went unread because the lines reached his chat only.
- **`probe_mana`**, a selftest verb: **`MANA current=<c> max=<m>`** from the pool casts spend from.
- The seat's HF1 ruling recorded in `NEXT.md` and `standing-decisions.md`; the new rule **A DAMAGE PREDICTION NAMES
  EVERY TERM OF THE SUM** in `.claude/rules/verification.md`, with its pointer row in `CLAUDE.md`. 0 words removed.

**PR 2, `feat/knell-mechanisms` (rulings 1 to 7; no content; every option defaults off):**

| option | where | what it does |
|---|---|---|
| **`pierce: true`** | a ray cast | the walk goes on past a body to the range or the first block; each body struck ONCE (struck ids ride across chunk-column hops as values); each takes the same `on_hit`, nearest first |
| **`tolerance: <blocks>`** | a ray cast | a body is struck when the line meets its box grown by this much: the Knell mob's `BeamLine.meets`, reused. A margin-only hit needs a clear sight line from the line to the body |
| **`locked_aim: true`** | a volley | the origin and direction are captured at the cast; every shot fires down that line |
| **`warning: { beam: <visual>, flash_at: [ticks] }`** | a volley | a beam visual drawn along the locked line on those windup ticks. Refused by name without `locked_aim`, on a non-ray, or with a tick outside the windup |

The names follow the schema's own: `beam` is a ray's drawn line, `flash_at` and `tolerance` are the Knell mob's keys.
A ray with neither `pierce` nor `tolerance` takes the old path unchanged (`castRay`); every shipped ray is byte-identical.

**PR 3, `feat/knell-beam` (the weapon):** `content/weapons/knell_beam.yml` (a 24-tick locked volley of one piercing ray,
range 40, tolerance 0.3), four visuals, its golden block, `KnellBeamContentTest`. **The first EPIC item**; it joins NO
loot pool and has NO recipe. Every "no content is epic" site carries a dated note.

## THE SEAT PLACEHOLDERS, FOR BEN (each marked `SEAT PLACEHOLDER 2026-11-21` in the file)

| placeholder | value | why |
|---|---|---|
| tolerance | **0.3 blocks** | Ben wants more accuracy than the old 1.0 and gave no number |
| damage, mana, range | **63, 13, 40** | the old weapon's |
| rarity, material | **epic, echo_shard** | the old weapon's |
| id, name, ability | **`knell_beam`, "Knell Beam", "Knell's Toll"** | the old weapon's names |
| description | "Locks a line where you look," / "then tolls a beam down it 1.2s later." | the old tooltip, reworded to state the windup |
| warning | **ONE flash, at the click** (`flash_at: [0]`) | as Ben ruled for the Knell mob on 2026-11-17 |
| visuals | the mob's warning dust (95,35,165) and beam dust (40,230,235); the mob's telegraph sounds at the click; the mob's fire sounds and the old per-target bell where it lands | the Knell mob's where it has them, the old weapon's otherwise |
| piercing | **every body the line crosses** | the old weapon's; a cap is Ben's to add |
| **OPEN FOR BEN** | there is **no sound AT THE FIRE, AT THE STAFF**: a volley has no hook that presents a visual when its shot fires, so the mob's fire sounds play where the beam lands | adding the hook is Java; named, not built |

## WHAT WAS READ, EACH MARKED

**The seat's a) to d):**
- **a) READ.** `content/mobs/knell.yml`: `beam.telegraph_ticks: 24`, `flash_at: [0]`, `range: 40`, `tolerance: 1.0`,
  `damage: 65`.
- **b) READ.** `CastSpec.Volley` with `shots 1` and a windup is a telegraphed single cast, and `CastExecutor.volley`
  re-reads the caster's live aim (`world.aimOf`) before every shot: the aim was NOT locked. (Its "fresh price" is each
  shot's Caster stats; mana is spent once, at the click, in `AbilityService`.)
- **c) READ.** `CastExecutor.stepRay` stops at the first body, and its comment says piercing needs a set of already-hit
  ids threaded through the calls.
- **d) READ.** No player cast drew a line along its path before it fired: a ray's `beam` is drawn as it walks.

**The rulings:**
- **How today's ray decides a hit (READ, `PaperCombatWorld.castRay`):** one `World#rayTrace` over the segment,
  `FluidCollisionMode.NEVER`, passable blocks ignored, `RAY_SIZE 0.0` (each body's exact bounding box), filter
  `LiveTargets.except(caster)`; the nearer of the first block and the first body wins. Tolerance 0 in the new sweep is
  that exact box, on the mob's `BeamLine.meets`.
- **Can tolerance reach through a wall? Not along the line, and not round a wall either.** The sweep clips each segment
  at its first block (`rayTraceBlocks`, the same configuration), and a body struck only in its tolerance margin is
  refused unless `lineOfSightClear(the struck point, the body's sight point)` (`CastExecutor.stepSweep`; caused and
  mutated in `KnellBeamMechanismsTest`, MK5).
- **The Knell mob's warning line is NOT clipped at blocks (READ, `MobBeamer`):** it draws `WARNING` on the locked full
  line, and clips only the fired beam (`rayTraceBlocks`, then `BeamLine.clip`). The weapon does the same: its warning is
  drawn to the full range, per chunk-column segment; its beam is clipped at the first block.
- **End crystals (READ):** a ray's body filter is `LiveTargets` (live LIVING entities), so an end crystal is never a
  body: a plain ray and the beam pass through one and do nothing to it. The sweep keeps that filter, pinned in
  `KnellBeamSchemaTest.theSweepKeepsCastRaysFilters`.
- **Players and the caster (READ):** another player IS a body for a ray (and the sweep); the caster never is (excluded
  by id). **So the beam cannot strike its caster, and his own locked beam does nothing to him if he walks into its
  line.** Unchanged.
- **Folia (READ, our source):** the locked line is two `Vec3` values; the struck set is a set of UUIDs copied per hop; no
  entity handle crosses a tick. The mob's beam code is unchanged (`git diff` of `MobBeamer`, `MobBeam`, `BeamLine`,
  `BeamBox`: empty).

**PR 1's answers:**
- **CS1's `"[white] italic"` (READ):** a minted weapon's name is set in `WeaponItems.displayName` with
  `.decoration(TextDecoration.ITALIC, false)`, explicitly NOT italic. The dump prints `italic` when ANY component in the
  name's tree says ITALIC TRUE (`SelfTestLines.styled`), and it reads `ItemStack#effectiveName()`. **Every weapon's dump
  prints it:** all 37 `HELD` dumps in the 386 archived server logs (`run/logs/*.log.gz`) read `italic`, for every weapon
  and the stone, 0 plain. **So it is the instrument's reading of `effectiveName()`'s wrapper, not a defect of the
  staff** (INFERRED that Paper wraps a custom name in an italic component; not read from Paper's jar). Ben read the
  staff's name as "look good". Reported, not fixed.
- **The mana pool** (READ): `probe_mana` reads `ResourcePool.current` and `max` for `mana`, the pool `AbilityService`
  spends from.

**Weapon facts:**
- **The tooltip** (READ, `WeaponLoreLines.cooldownLabel`, and pinned by `KnellBeamContentTest.aMintedBeamsTooltipReads`
  from the real file): `Cooldown: 2.4s | Mana Cost: 13`.
- **No boot warning for its cooldown** (READ): the volley's floor is its windup, 24 (`minimumCooldownTicks`); 48 is
  above it.
- **The enchant pool:** attunement (it reaches a literal damage) and unbreaking. `echo_shard` has no durability, so
  Unbreaking rolls and grants nothing (as on the other staves). Pinned; carried open.
- **A given item's enchants are not ACTIVE** (READ in `GATE-sword-smithing.md`'s reading: candidates roll on acquire,
  the active choice is `-1` until the player picks), so the enchant multiplier is 1.0. KB2's `rpg enchant show` reads it.
- **His level is staged at 1** in the setup (`rpg playerxp set @s 1 levels`, then 40 ticks), and **executed**:
  `LevelBonus.at(1).weaponDamage()` = **0.0** (`at(50)` = 49.0).

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup:** `run/plugins/Rpg` and `run/world` copied to a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-knell-beam`; file and directory counts, `diff -rq lines: 0` for each, the `ls`
   of `world/dimensions`, the overworld's file count. Nothing is restored (Ben's word only).
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND:** "booting now", and Ben told **NOT to log in until told R0 passed.**
6. `./scripts/dev-server.sh --refresh-content`. **Required deployed lines:** `weapons/knell_beam.yml` reads
   `    cooldown_ticks: 48`; `weapons/` holds 20 `.yml` files; `visuals/knell_warning.yml` exists.
7. R0a to R0e on **this boot's** log, matched to **this boot's own** `Done` line.
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* `rpgfight` empty; the overworld's file count against step 2's; `All dimensions are saved`; no
   `java` process left.

## THE RUN SHEET — BEN TYPES EXACTLY THESE THREE LINES

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.**
1. `/gamemode survival`
2. `/rpg selftest knell-beam` — **ONCE.** About two minutes. Frozen zombies appear; they cannot move or hit you. **Two
   rows ask something of you, in gold chat:** one says **HOLD RIGHT-CLICK** (hold it until your mana runs out, then let
   go), and the last gives you 30 seconds with the Knell Beam, then asks a question: **type your answer in chat.**
   Your items, time and level are put back at the end.
3. `/stop`

Nothing else. If a row stalls or aborts, do not intervene. **His chat answer to the one question is the only line he
types beyond these three**, and it is the selftest's own ASK step, logged verbatim; "not sure" is NOT STATED.

## THE ARITHMETIC (SAFETY): THE MOST HITS THAT CAN LAND ON BEN TIMES THE BIGGEST HIT, AT DEFENCE 0, AGAINST 100 AND 350

| row | where he is | what can hurt him | most hits x biggest hit | puts him down at 100? at 350? |
|---|---|---|---|---|
| KB1 | where he stands | nothing | 0 | no / no |
| KB2-KB7, CS2R2 | teleported to his block's centre, facing east; NoAI zombies 6 to 14 blocks away | a NoAI zombie does not move or attack; **his own beam cannot strike him** (the sweep excludes the caster by id, READ) and his shard skips him (`castRay`'s ignore id) | 0 x 0 = 0 | no / no |
| HF1R2 | where KB2-CS2R2 left him | the same; his shards skip him | 0 | no / no |
| KB8 | the same; six NoAI zombies, 30 seconds of free play | the same; **walking into his own locked beam's line does nothing to him** (excluded by id, READ) | 0 | no / no |
| every row | the overworld at night | **natural mobs ALREADY ALIVE nearby**: `spawn_mobs false` stops new spawns, but kills none. Each row's `LEFTOVER living=<n> radius=16` line counts what is near him | at gs 100 (every overworld hostile, READ), 5 x vanilla (READ, `MobScaling`); vanilla UNVERIFIED: a zombie 3 -> 15 a hit, a creeper at point blank about 43 -> about 215 | **at 100:** 7 zombie hits, or one close creeper. **At 350:** 24 zombie hits; not one creeper. Ordinary night play, nothing more |

## R0 — THE BUILD, FOR THE WHOLE STACK. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `feat/knell-beam`'s head as `git ls-remote` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries all three PRs

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/weapons/knell_beam.yml` (line `    cooldown_ticks: 48`), the four `knell_*` visuals, `selftest/GATE-knell-beam.yml` (line `HF1R2:`), `selftest/index.yml` (line `  - knell-beam`); the classes `rpg/core/combat/RaySweep.class` (PR 2) and, by content, `rpg/paper/command/RpgCommand.class` carrying `FIRERATE player=` and `rpg/paper/selftest/SelfTestDrivers.class` carrying `MANA current=` (PR 1). The control `content/weapons/flint_staff.yml`: PRESENT | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Line($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Entry($name) { if ($zip.GetEntry($name) -eq $null) { "ABSENT  entry $name" } else { "PRESENT entry $name" } }
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
foreach ($e in @('weapons/knell_beam.yml', 'visuals/knell_toll_cast.yml', 'visuals/knell_warning.yml', 'visuals/knell_toll_line.yml', 'visuals/knell_toll_hit.yml', 'weapons/flint_staff.yml')) { Entry ('content/' + $e) }
Line (Bytes 'content/weapons/knell_beam.yml') '    cooldown_ticks: 48' 'knell_beam.yml'
Line (Bytes 'selftest/GATE-knell-beam.yml') 'HF1R2:' 'GATE-knell-beam.yml'
Line (Bytes 'selftest/index.yml') '  - knell-beam' 'index.yml'
$p = 'io/github/butterflysmp/rpg/'
Entry ($p + 'core/combat/RaySweep.class')
Has ($p + 'paper/command/RpgCommand.class') 'FIRERATE player='
Has ($p + 'paper/selftest/SelfTestDrivers.class') 'MANA current='
$zip.Dispose()
```

### R0c — every Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| Exactly one of each, up to the first `joined the game`: **`[Rpg] Loaded 11 abilities, 37 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 20 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 4 recipes, 1 ores, 4 vein sizes`** (against `GATE-crystal-shard-staff.md` R0c, read AS PREDICTED at `4d275653`: **visuals 33 -> 37, weapons 19 -> 20**; every other figure unchanged); `GATE-fight-revive.md`'s R0c fight line and `GATE-loot-roll.md`'s R0c loot line, word for word (the loot line prints no pool, and the beam joins none); the world-damage and smithing lines as at the last boot; **`[Rpg] Mint-on-craft: 36 indexed, 36 claiming, of 50 gear definitions`** (49 -> 50: 20 + 1 + 24 + 5; the beam claims no craft_result); `Convert-on-arrival: 7 materials {…}` as before. **No `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Recipe '`; no `[Rpg]` WARN beyond `volley_stone`'s known pair**: the beam's 48 is above its volley floor of 24, so it warns nothing (READ). No `No fights loaded` | `Select-String run\logs\latest.log -Pattern 'Loaded\|Mint-on-craft\|Convert-on-arrival\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded\|Recipe '''` | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 266 scenarios across 36 gates [melee-cell, …, crystal-shard-staff, knell-beam]`**. The control: the last boot's `256 scenarios across 35 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (2026-11-21, PR 3's tree, from `index.yml` and every gate file it names; `SelfTestScenariosTest` asserts
266): 36 gates, `^[A-Za-z0-9_-]+:` **302**, `^setup:` **36**, so **266**. Control: 291 + 11 (setup, KB1-KB8, CS2R2,
HF1R2) = 302.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, no `deleting`, no `orphan` | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | |

## THE ROWS (`/rpg selftest knell-beam`) — SURVIVAL, ALL AUTO

**EVERY DAMAGE PREDICTION NAMES EVERY TERM** (`.claude/rules/verification.md`):
`sent = (amount x score/100) x enchant + class bonus + level bonus`, then `x charge x crit`.
**For the Knell Beam:** `(63 x 100/100) x 1.0 + 0 + 0 = 63`, `x 1.0`, and `x 2.0` on a crit: **`sent=63.000 crit=false`
or `sent=126.000 crit=true`**. Terms: amount 63 (the file); score 100 (`rpg gearscore set 100`, staged); enchant 1.0
(no active enchant on a given item; KB2's `rpg enchant show` reads it); class bonus 0 (nothing grants class damage);
level bonus 0 (level 1, staged; executed 0.0); charge 1.0 (not a melee charge).
**For the Crystal Shard Staff (CS2R2):** `(16 x 100/100) x 1.0 + 0 + 0 = 16`: **`sent=16.000 crit=false` or
`sent=32.000 crit=true`**. Any other `sent` is a FAIL. A zombie here has `max=100` (`MOBSEED … gs=100 … max=100`).

### KB1 — given, held, and its tooltip (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| REPLY `Given Knell Beam`; `INVENTORY knell_beam slots=[…]`; `DRIVE hold …`; the gearscore REPLY; then `HELD knell_beam ECHO_SHARD x1 name="Knell Beam" [dark_purple] italic` (the `italic` every weapon's dump prints, above) and lore, in order: **`Void`**, **`Gear Score: 100`**, `` (blank), **`Knell's Toll  Right-Click`**, **`Locks a line where you look,`**, **`then tolls a beam down it 1.2s later.`**, **`Void Damage: 63`**, **`Cooldown: 2.4s \| Mana Cost: 13`**, `` (blank), **`Epic Magic Weapon`** | `KnellBeamContentTest.aMintedBeamsTooltipReads` (the same ten lines from the real file); `golden-lore.txt` (`#AA00AA`, dark_purple, for void and epic) | |

### KB2 — one cast straight ahead: 13 mana, and the hit on tick 24 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `rpg enchant show`: its REPLY shows no active enchant (every slot's active choice `-1`). `MOBSEED … zombie gs=100 … max=100`; the aim `d=10.000`; **`MANA current=<M> max=<M>`** (just refilled), `DRIVE use … item=knell_beam`, **`MANA current=<M-13> max=<M>`**, read on the press's own tick: **13 less**. Then the probe 23 ticks after the press: **`customHP=100.000/100.000`** (not yet struck); then exactly one **`PLAYERHIT … source=knell_beam/right_click … zombie element=void sent=63.000 crit=false triggerScore=100`** (or 126.000 crit=true); then the probe 25 ticks after: **`customHP=37.000/100.000`** (100 - 63), or dead on a crit. **So the shot lands on tick 24, between the two probes.** The warning line drawn at the click is NOT read by any instrument (client render) | the sum above; `AbilityService` spends at the click (READ); `CastSpec.Volley` windup 24 | |

### KB3 — PIERCE: three on the line, three hits, nearest first (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Three `MOBSEED` lines in summon order (6, 10, 14 blocks). After the press, exactly **three** `PLAYERHIT … element=void` lines, **each `sent=63.000 crit=false`** (or all three 126.000: one crit roll per shot), their `target=` uuids **in the MOBSEED order: nearest first**. The probe: all three at `37.000/100.000` (or dead) | `KnellBeamMechanismsTest.aPiercingRayStrikesThreeBodiesInALineOnceEachNearestFirst` | |

### KB4 — ACCURACY: 0.2 off is struck, 0.5 and 0.9 off are not (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Facing due east, level; the line runs along his eye at z = 0. The zombies' FEET at z +0.5, +0.8 and +1.2 (at 8, 11 and 14 east). **A zombie is 0.6 wide (UNVERIFIED in the jar)**, so its box's near side is 0.3 nearer the line: **gaps 0.2, 0.5 and 0.9**. Tolerance 0.3: **0.2 < 0.3 struck; 0.5 > 0.3 not; 0.9 > 0.3 not, though 0.9 < 1.0, the old weapon's.** Exactly **one** PLAYERHIT, on the first `MOBSEED`'s uuid, `sent=63.000` (or 126.000 crit). The probe: the first at `37.000/100.000`, the other two at `100.000/100.000` | `KnellBeamMechanismsTest.aBodyInsideTheToleranceIsStruckAndOneOutsideIsNot` and its 1.0 control | |

### KB5 — LOCKED AIM: he turns, the shot goes down the old line (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Zombie A (first `MOBSEED`) due east at 10, zombie B (second) due south at 10. The press facing east; 5 ticks later `tp @s ~ ~ ~ 0 0` turns him south, to B. Exactly **one** PLAYERHIT, on **A**; the probe: A at `37.000/100.000`, **B at `100.000/100.000`** | `KnellBeamMechanismsTest.aLockedShotGoesDownTheLineCapturedAtTheCast` and its unlocked control | |

### KB6 — A WALL: no hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Two stone blocks 5 east at his feet and eye height (`save_blocks` first; the restore puts them back), a zombie 10 east. **0 PLAYERHIT**; the probe: `100.000/100.000` | the clip at the first block (`KnellBeamMechanismsTest.aBlockEndsAPiercingWalk`) | |

### KB7 — COOLDOWN: refused at 40, fires at 48 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `MANA current=<M>`; press 1 (fires 24 ticks later); press 2, **40 ticks after press 1**: REPLY **`On cooldown for 0.4s`** [gray] (8 ticks left of 48, READ: `RpgListeners`' OnCooldown arm), then `MANA current=<x>` with **x = M - 13 plus 40 ticks of regeneration** (the refused press spent nothing); press 3, **48 ticks after press 1**: fires (no refusal REPLY), then `MANA current=<x - 13>` (to within the regeneration of the same tick: none). **Exactly two PLAYERHITs** (press 1 and press 3), each `sent=63.000` (or 126.000); the second kills the zombie (37 left after the first), so the probe finds it dead or gone | `AbilityService` stamps the cooldown at the click, `isReady` is `>=` (READ) | |

### CS2R2 — OWED: the Crystal Shard Staff at level 1 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Exactly one **`PLAYERHIT … source=crystal_shard_staff/right_click … element=kinetic sent=16.000 crit=false triggerScore=100`** (or `sent=32.000 crit=true`): `(16 x 100/100) x 1.0 + 0 + 0`. The probe: `84.000/100.000`. Against `GATE-crystal-shard-staff.md` CS2's 65.000 at level 50 | the sum above; the seat's ruling 1 | |

### HF1R2 — OWED: held fire on the 14-tick staff, read FROM THE LOG (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The first `rpg firerate` (the row's own) prints whatever an earlier row left and clears it. `TELL HOLD RIGHT-CLICK NOW …`. 15 seconds. Then the row's own `rpg firerate`, whose lines land in the log TWICE each, as `FIRERATE player=BaronVonYeetus <line>` and as the selftest's REPLY: `Fire cadence -- crystal_shard_staff, right_click`, **`  INPUTS  count … min <i>t`**, **`  FIRES   count … min <f>t`**, **`  cooldown_ticks 14 authored, 14 effective (not a basic attack -- attack speed does not scale it)`**, a verdict line, `Sample cleared.` **By `NEXT.md`'s held-fire table, restated for 14:** **`FIRES min 16`, `INPUTS min 4`: inconclusive**; **`FIRES min 16`, `INPUTS min 3`: the fit survives**; **`FIRES min 14` or `15`: `ceil(n/4) x 4` IS FALSIFIED**; below 14, a cooldown defect. **If he did not hold:** `No fire-cadence sample. …`, and the reading is UNREAD again. Mana: about 12 fires (100 / 8, plus regeneration) | `RpgCommand.say` (`FireRateLogTest`); the seat's ruling 2 | |

### KB8 — A FREE GO, THEN THE ONE QUESTION (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Six `MOBSEED` lines; `TELL 30 seconds with the Knell Beam …`; 30 seconds in which any PLAYERHITs are his, each `sent=63.000` or `126.000` (level 1, score untouched since KB7: **he holds a new beam, unscored here, so `triggerScore` may read 100 by default or as rolled**; any sent is recorded, none predicted); then `ASK 1 question="Could you tell where the beam would land, and did it need a careful aim?" answer="<his words>"`, verbatim, or `answer=NOT STATED after 1200 ticks` | `SelfTestAsk` | |

## WHAT THIS GATE DOES NOT READ

- **What he sees and hears**: the warning line, the beam, the sounds. AUTO reads the server, never the client
  (`.claude/rules/verification.md`); only his one answer speaks to it.
- **The crafted item's tooltip Gear Score** (CR1, carried twice): it rides on the next boot that has a craft in it for
  another reason, read from a selftest dump after that craft. No craft is asked of Ben here.
- **Unbreaking granting nothing on the beam**, and **the shard or beam against a player**: source only.
