# GATE — STEP F9: THE DRAGON

**Status: READ, ONE BOOT, 2026-11-19, at `7e2ebfa8`** Seat approval 2026-11-19: code and gate diffed at `7e2ebfa8`, readings at `ff9cf9ef`, plan 6.9 at `9d1023a7`. Merge on Ben's words. (the seat's boot approval of 2026-11-19; Ben's "ready"). R0
passed; every AUTO row ran; no ABORTED line; Ben's six answers are in DB1 and DB2. The readings, the checklist and the
findings are in *THE BOOT OF 2026-11-19* at the end. Every prediction below was written **before** any boot of
`feat/fight-dragon`, and this file was committed before the boot (at `7e2ebfa8`, NOT RUN).
- Readings go **beside** a prediction, never over it.
- A prediction is not edited once its row has been read; a correction is a dated note.
- **NO BOOT until the seat has diffed the code AND this gate from origin and Ben says "ready" in his own words.** Then
  only CC boots.

**The step** (`PLAN-dragon-fight.md` §6.8; the seat's F9 brief, 2026-11-18): the arena fight's Ender Dragon.
- 20,000 in our store.
- Hit through its parts.
- Its boss bar.
- Immune while a Mage is alive.
- No fireball.
- A Mage at each of 80, 60, 40 and 20 percent, queued.
- Its death is the win.

The arena site only. **Not in this step:** waves, Endermen, loot, dialogue, entry rules, player-count scaling.

**GAME MODE: SURVIVAL for every row.** Damage rows are predicted against the HURT line (`.claude/rules/verification.md`,
*PREDICT DAMAGE AGAINST THE HURT LINE*). Every arena death happens inside an `await_fight`, and no row uses
`save_blocks` (refused in a fight world).

**Witness:** the AUTO rows (DA) are read by CC from the server's log; they witness only what the selftest drives (an
AUTO reading is a reading of the server, never of the client). The BEN rows (DB) are his words, verbatim; "not sure"
is NOT STATED; no PASS or FAIL on a Ben row.

---

## WHICH MERGED ROWS THIS BRANCH MOVES, AND WHICH THE BOOT RE-RUNS

- **The arena rows of `GATE-fight-arena.md` (AE1R2 to AE7R2, BA1R2 to BA3R2).** A new arena fight now spawns the
  dragon, which would have moved every one of them. **Their scenarios now enter with `rpg fight enter nodragon`**, so
  no dragon is spawned and **none of their predicted lines moves**. They are **not re-predicted and not re-run** at
  this boot. That file carries a dated note.
- **Every fight (both sites) now also prints:**
  - `FIGHT mage-health t=<t> at=<from>-><to> …` on the loop tick after each phase change, and `… at=end …` at the end;
  - a `FIGHTHIT …` line for every hit that LANDS on a Mage.

  No merged prediction names the absence of either, and no predicted line changes. **The merged rows are not
  re-predicted for these, and not re-run.** Any later reading of them will see the extra lines.
- **`GATE-fight-arena.md` R0c and R0d** (the Loaded fight line now ends with the dragon clause; `7 mobs`; `210
  scenarios across 24 gates`): superseded at this boot by this file's R0c and R0d. Not re-run.
- **`LiveTargets`** (a part resolves to its dragon): for any entity that is not a part, every filter answers as before.
  No merged row is moved.
- **MG5R3 and MG7R3** (`GATE-fight-site.md`) stay **UNREAD**, carried; this boot does not run them.

**The boot re-runs this file's rows only.**

---

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

The booter's checklist is `.claude/rules/booting.md`; this list is its instance for this boot.

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup:** `run/plugins/Rpg` and `run/world` copied to
   `../rpg-run-backups/2026-11-<dd>-pre-fight-dragon` (with `-2` appended if that exists).
   - Report the file and directory counts, and `diff -rq lines: 0` for each.
   - Report the `ls` of `world/dimensions` and the overworld's file count.
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND IS SENT:** announce **"booting now"**, and tell Ben **NOT to log in
   until told R0 passed.**
6. `./scripts/dev-server.sh --refresh-content`. **The three REQUIRED deployed lines:**
   - `content/fights/dragon.yml` reads `  mage_thresholds: [0.80, 0.60, 0.40, 0.20]`;
   - `content/mobs/ender_dragon.yml` reads `max_health: 20000`;
   - `content/mobs/dark_mage.yml` reads `max_health: 6000`.
7. R0a to R0e read on **this boot's** log (its `[Rpg] Build:` names this branch's head).
8. **IN ITS OWN MESSAGE: tell Ben R0 passed.** Any R0 not as predicted: STOP, tell the seat, and tell Ben NOT to log
   in.
9. *(After his `/stop`.)*
   - `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
   - The overworld's file count is reported against step 2's.
   - The clean-stop lines (`All dimensions are saved`) are read, and no `java` process is left.

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED. IF YOU ARE TOLD NOTHING, DO NOT LOG IN.**
1. `/gamemode survival` — **alone, the first line after login.**
2. Stand on flat open ground with room all round, and hold the weapon you want to fight with.
3. `/rpg selftest fight-dragon` — **ONCE.** About ten minutes of rows, three minutes of watching the dragon, your
   answers, and up to **ten minutes** of fighting.
   - You will be taken into the fight's own world and brought back after every row.
   - **ONE ROW KILLS YOU ON PURPOSE (DA8), AND THE LAST ROW (DB2, THE FIGHT) CAN: you reappear at once where you
     entered, with everything you carried.**
   - There are **six** questions; each says whether it wants yes or no, or your own words.
   - Stand still and touch nothing unless gold text tells you to.
4. `/stop`

Nothing else this boot.

---

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/fight-dragon` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **AS PREDICTED (2026-11-19, 08:30:55).** `[Rpg] Build: 7e2ebfa8`; `git ls-remote --heads origin feat/fight-dragon` printed `7e2ebfa8ebe0704290a34a05b0ec824177b8ad68`. Not `-dirty`, not `unknown` |

### R0b — the jar carries this step

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/fights/dragon.yml` line `  mage_thresholds: [0.80, 0.60, 0.40, 0.20]`; `content/mobs/ender_dragon.yml` line `max_health: 20000`; `FightInstance.class` carrying `FIGHT dragon spawned`; `RingFixture.class` carrying `DRAGONGATE`; the entry `io/github/butterflysmp/rpg/core/fight/DragonCrossings.class`; `GATE-fight-dragon.yml` carrying `DA9:`; `GATE-fight-arena.yml` carrying `rpg fight enter nodragon`. ABSENT: `content/mobs/ender_dragon.yml` line `max_health: 18000` (the YAML control), `NoSuchTextControl` in `FightInstance.class` (the class control) | the PowerShell below, from the repo root | **AS PREDICTED.** `PRESENT dragon.yml '  mage_thresholds: [0.80, 0.60, 0.40, 0.20]'`; `PRESENT ender_dragon.yml 'max_health: 20000'`; `ABSENT  ender_dragon.yml 'max_health: 18000'`; `PRESENT …/FightInstance.class 'FIGHT dragon spawned'`; `ABSENT  …/FightInstance.class 'NoSuchTextControl'`; `PRESENT …/RingFixture.class 'DRAGONGATE'`; `PRESENT selftest/GATE-fight-arena.yml 'rpg fight enter nodragon'`; `PRESENT entry DragonCrossings.class`; `PRESENT selftest/GATE-fight-dragon.yml DA9:` |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Line($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
Line (Bytes 'content/fights/dragon.yml') '  mage_thresholds: [0.80, 0.60, 0.40, 0.20]' 'dragon.yml'
$m = Bytes 'content/mobs/ender_dragon.yml'
Line $m 'max_health: 20000' 'ender_dragon.yml'; Line $m 'max_health: 18000' 'ender_dragon.yml'
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
Has 'io/github/butterflysmp/rpg/paper/fight/FightInstance.class' 'FIGHT dragon spawned'
Has 'io/github/butterflysmp/rpg/paper/fight/FightInstance.class' 'NoSuchTextControl'
Has 'io/github/butterflysmp/rpg/core/fight/RingFixture.class' 'DRAGONGATE'
Has 'selftest/GATE-fight-arena.yml' 'rpg fight enter nodragon'
if ($zip.GetEntry('io/github/butterflysmp/rpg/core/fight/DragonCrossings.class') -ne $null) { "PRESENT entry DragonCrossings.class" } else { "ABSENT  entry DragonCrossings.class" }
$s = Bytes 'selftest/GATE-fight-dragon.yml'; if ($s -ne $null -and $s -match "(?m)^DA9:\s*$") { "PRESENT selftest/GATE-fight-dragon.yml DA9:" } else { "ABSENT  selftest/GATE-fight-dragon.yml DA9:" }
$zip.Dispose()
```

**A jar from master reads ABSENT on every PRESENT line**, and the two controls ABSENT on either jar.

### R0c — the Loaded lines, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`** (the arena boot's line with `7 mobs` for `6`: `ender_dragon.yml` added), AND exactly one **`[Rpg] Loaded fight 'dragon': border 48.0, mage ring 12.0, v_fast 3.80, v_slow 0.60, e_kill 1.00, 10 crystals every 240 ticks, a strike every 40 ticks from 20.0; ring dust (170,0,255) size 1.60 every 1.00 blocks at [0.2, 1.7, 3.2, 4.7, 6.2, 7.7], row shift 0.25, turn 1.00/s alternating, every 4 ticks; mage 'dark_mage' refused item.shield.block; crystals at 40.0, 12.0 up, lit block.beacon.power_select; participants within 128.0; arena at (0, 0) floor y 64, disc 56 x 3, 10 pillars 3 wide on 40.0 to +12, time 18000, entry (0.0, 0.0); dragon 'ender_dragon' Mages below [0.8, 0.6, 0.4, 0.2], win return 200 ticks, bar purple`** (`FightLoaderTest` asserts it verbatim). **No `Refusing`, `Skipping`, `SEVERE` or `Exception`; no `[Rpg]` WARN beyond `volley_stone`'s known pair**; no `No fights loaded` | `Select-String run\logs\latest.log -Pattern 'Loaded\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded'` (up to the first `joined the game`) | **AS PREDICTED.** Both Loaded lines verbatim at 08:30:55 (`… 6 accessories, 7 mobs, 1 mob elements …`; the fight line ending `dragon 'ender_dragon' Mages below [0.8, 0.6, 0.4, 0.2], win return 200 ticks, bar purple`), exactly one fight line. `[Rpg]` WARN: `volley_stone`'s pair only. No `Refusing`, no `Skipping`, no `SEVERE`, no `Exception`, no `No fights loaded` before the join (08:35:38). The other WARNs are Paper's own "4 release(s) behind" banner, not `[Rpg]` |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 210 scenarios across 24 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive, fight-site, fight-arena, fight-dragon]`**, no refusal after it. **The control:** the arena boot's `200 scenarios across 23 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | **AS PREDICTED.** Exactly one `[Rpg] Selftest: dev server, 210 scenarios across 24 gates [melee-cell, … fight-site, fight-arena, fight-dragon]`, no refusal after it; `200 scenarios across 23 gates` ABSENT |

**RECOUNTED** (2026-11-18, this branch's tree; `SelfTestScenariosTest` asserts 210):
- The gates in `index.yml`: **24**.
- The key count (`^[A-Za-z0-9_-]+:` over the 24 files) prints **234**.
- `^setup:` prints **24**.
- So 234 − 24 = **210**.
- **Control:** the arena boot's 223 + fight-dragon's 11 = 234.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, and **no `FIGHTWORLD deleting` line and no `orphan` line at all** | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | **AS PREDICTED.** Exactly one `FIGHTWORLD sweep path=C:\Users\Neb91\IdeaProjects\minecraft-rpg-scaffold\run\world\dimensions\rpgfight folders=0`; no `deleting`, no `orphan` line during the enable |

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST fight-dragon|\[Rpg\] (FIGHT|DRAGON|BOSSBAR|STRIKE|MAGEGATE|CRYSTAL|AWAIT|HURT|RINGTICK)'`
inside the row's `START … DONE` bracket. `FIGHT` also matches FIGHTWORLD, FIGHTWORLDS, FIGHTPHASE, FIGHTKILL,
FIGHTPROBE and FIGHTHIT; `DRAGON` matches DRAGONPROBE, DRAGONCROSS and DRAGONGATE. **A log-line row passes on the
VERBATIM line** (`<…>` as the row says).

**Every row, not repeated:**
- **The clean:** `LEFTOVER living=0 radius=16`, `LINGER work=0 entities=0 radius=48`.
- **The world's birth**, as the arena gate's:
  - `FIGHTWORLD created name=<n> … ms=<t>` (**t READ and reported**);
  - `FIGHTWORLD arena built name=<n> blocks=30615 end_stone=29535 obsidian=1080 ms=<t>`;
  - `FIGHT start world=<n> centre=0.5 65.0 0.5 by=BaronVonYeetus site=arena`.
- **THEN THE DRAGON**, before he is entered:
  **`FIGHT dragon spawned t=0 id=<uuid> at=0.00 20.00 0.00 podium=-0.50 0.00 -0.50 phase=CIRCLING mob=ender_dragon
  thresholds=[0.8, 0.6, 0.4, 0.2]`**.
  - The podium is a BLOCK position: READ, `CraftEnderDragon.setPodium` takes `CraftLocation.toBlockPos`, so the centre
    0.5 65.0 0.5 is block 0 65 0, -0.50 0.00 -0.50 from the centre.
  - The phase is CIRCLING because the fight sets it: READ, a new dragon's phase manager starts at HOVERING and
    `aiStep` moves nothing while it HOVERS.
- **Then:** `FIGHTWORLD entered player=BaronVonYeetus world=<n> return=world <x y z>`, and on a loop tick after it
  **`BOSSBAR shown t=<t> player=BaronVonYeetus`**, exactly once.
- **Every AUTO row but DA6** then sets **`DRAGON phase-set t=<t> phase=HOVER now=HOVER`**: the dragon holds 20 above
  the centre, out of his reach.
- **The end:**
  - `FIGHTWORLD returned … why=<left|died>`;
  - **`BOSSBAR hidden t=<t> player=BaronVonYeetus why=left`**. Hidden on the loop tick after he stops being an entrant
    (leave or death), and before that tick's `FIGHT nobody inside`: INFERRED from the loop's order and
    `ArenaSite.takesPart` requiring an entrant;
  - `FIGHT nobody inside t=<t> outcome=<o>`, `FIGHT stop t=<t>`;
  - **`FIGHT mage-health t=<t> at=end <health=…|mage=none>`**;
  - **`FIGHT dragon removed reason=nobody-inside-<o> was_valid=true id=<the dragon's>`**;
  - `FIGHT ended reason=nobody-inside-<o>`;
  - `FIGHTWORLD unloaded`, `deleting`, `deleted … entries=<e>`;
  - **`FIGHTWORLDS loaded=0 folders=0 marked=0 you=world`**;
  - the end probes 0.

**THE DRAGON'S DEFENCE IS PREDICTED 0** (INFERRED: `ender_dragon.yml` authors none, and the Mage at gear score 100
probes `defense=0.000`). **The DRAGONPROBE's `defense=` READS it**, so every "landed = carried" below rests on that
reading.

**A COLLISION, NAMED:** 15999.5 of 20000 is 0.799975, below 80%, and its boss-bar progress prints `0.8000` (computed by
`jshell`: the float, `%.4f`). **So the probe's progress cannot witness a crossing; only `DRAGONCROSS` can.**

### DA1 — enter: one dragon at 20000/20000, its podium the centre, the bar shown; leave: bar hidden, dragon removed (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The birth, the dragon's spawn line, entered, `BOSSBAR shown …`, `DRAGON phase-set … phase=HOVER now=HOVER`. (2) **`DRAGONPROBE t=<t> dragon=<the spawn line's id> health=20000.000/20000.000 defense=0.000 phase=HOVER podium=-0.50 0.00 -0.50 immune=false bar=PURPLE progress=1.0000 viewers=1 crossed=0 pending=0 dragons_in_world=1 fireballs_in_world=0`**. (3) `FIGHTWORLDS loaded=1 folders=1 marked=1 you=<n>`. (4) Leave and the end, `outcome=RUNNING`, **`FIGHT mage-health … at=end mage=none`**, **`FIGHT dragon removed reason=nobody-inside-RUNNING was_valid=true`**. (5) After it, the probe replies **`No fight fixture is running.`** | `FightDragonSourceTest`, `FightLoaderTest` | **AS PREDICTED.** (1) `FIGHTWORLD created name=rpgfight_fight_21f301ea … ms=58`; `arena built … blocks=30615 end_stone=29535 obsidian=1080 ms=1187`; `FIGHT start … site=arena`; **`FIGHT dragon spawned t=0 id=8bddcb44-1a0d-480a-aabe-0d1bd0f5cf39 at=0.00 20.00 0.00 podium=-0.50 0.00 -0.50 phase=CIRCLING mob=ender_dragon thresholds=[0.8, 0.6, 0.4, 0.2]`**; `FIGHTWORLD entered … return=world -111.46 63.00 16.40`; `BOSSBAR shown t=1 player=BaronVonYeetus`; `DRAGON phase-set t=10 phase=HOVER now=HOVER`. (2) **`DRAGONPROBE t=12 dragon=8bddcb44-… health=20000.000/20000.000 defense=0.000 phase=HOVER podium=-0.50 0.00 -0.50 immune=false bar=PURPLE progress=1.0000 viewers=1 crossed=0 pending=0 dragons_in_world=1 fireballs_in_world=0`**: defence READ 0.000. (3) `FIGHTWORLDS loaded=1 folders=1 marked=1 you=rpgfight_fight_21f301ea`. (4) `BOSSBAR hidden t=13 … why=left`, `FIGHT nobody inside t=13 outcome=RUNNING`, `FIGHT stop t=13`, **`FIGHT mage-health t=13 at=end mage=none`**, **`FIGHT dragon removed reason=nobody-inside-RUNNING was_valid=true id=8bddcb44-…`**, `FIGHT ended …`, unloaded, deleting, `deleted … entries=16`, `FIGHTWORLDS loaded=0 folders=0 marked=0 you=world`. (5) `No fight fixture is running.` |

### DA2 — a store hit with no Mage alive lands by the pipeline's exact figure (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `dragon hit 1234.5`: **`FIGHTHIT t=<t> target=dragon amount=1234.500 before_defense=1234.500 health=18765.500/20000.000 dealer=<his uuid> element=null`**, exactly one; **no DRAGONGATE and no DRAGONCROSS** (0.938275 is above 0.80). (2) **`DRAGONPROBE … health=18765.500/20000.000 defense=0.000 phase=HOVER … immune=false bar=PURPLE progress=0.9383 viewers=1 crossed=0 pending=0 …`** (0.9383 computed by `jshell`). (3) Leave and the end, `outcome=RUNNING` | `FightDragonSourceTest` *theLandedHitLine…*; `DragonCrossingsTest` | **AS PREDICTED.** (1) **`FIGHTHIT t=13 target=dragon amount=1234.500 before_defense=1234.500 health=18765.500/20000.000 dealer=b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 element=null`**, exactly one; no DRAGONGATE, no DRAGONCROSS. (2) **`DRAGONPROBE t=14 … health=18765.500/20000.000 defense=0.000 phase=HOVER … immune=false bar=PURPLE progress=0.9383 viewers=1 crossed=0 pending=0 …`**. (3) `outcome=RUNNING`, `dragon removed reason=nobody-inside-RUNNING was_valid=true`, the end |

### DA3 — below 80% lands one Mage; a hit while he lives is refused; his death: the dragon hurt again, the ring home (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `dragon hit 4000.5`: **`FIGHTHIT … target=dragon amount=4000.500 before_defense=4000.500 health=15999.500/20000.000 …`**, then **`DRAGONCROSS t=<T> threshold=0.80 health=15999.500/20000.000 pending=1`**. (2) On a later loop tick: **`DRAGON mage-due t=<D> pending=1`**, `FIGHT land t=<D> dx=<x> dz=<z> result=STARTED`, `FIGHT mage spawned t=<D> …`, `FIGHTPHASE t=<D> IDLE->FAST_CLOSE`, **`DRAGON mage-landed t=<D> pending=0`**; the next tick `FIGHT mage-health t=<D+1> at=IDLE->FAST_CLOSE health=6000.000/6000.000`. (3) `dragon hit 100`: **`DRAGONGATE t=<t> phase=FAST_CLOSE amount=100.000 source=hit refused`**, and no FIGHTHIT for it. (4) **`DRAGONPROBE … health=15999.500/20000.000 … immune=true … progress=0.8000 … crossed=1 pending=0 …`**. (5) `mage-die`: **`FIGHTHIT … target=mage amount=60000.000 before_defense=60000.000 health=0.000/6000.000 …`**, `FIGHT die t=<t> phase=RETURNING slow_close_started=never`. (6) `dragon hit 100`: **`FIGHTHIT … target=dragon amount=100.000 … health=15899.500/20000.000 …`**; **`DRAGONPROBE … health=15899.500/20000.000 … immune=false … progress=0.7950 … crossed=1 pending=0 …`**. (7) Within the 100-tick wait, **`FIGHTPHASE t=<H> RETURNING->IDLE`** (a return from a fast close lasts as long as the fast close ran: `ReturnPath.fromFastClose`), and **no second `DRAGON mage-due`**. The last FIGHTPROBE `mage=none`. (8) Leave and the end, `outcome=RUNNING`. **The dx/dz of the landing are random and READ** | `DragonCrossingsTest`, `RingFixtureTest` *aMageIsAlive…*, `FightDragonSourceTest` | **AS PREDICTED, WITH TWO LINES IN (2) IN ANOTHER ORDER.** (1) `FIGHTHIT t=13 … amount=4000.500 … health=15999.500/20000.000 …`, **`DRAGONCROSS t=13 threshold=0.80 health=15999.500/20000.000 pending=1`**. (2) `DRAGON mage-due t=14 pending=1`, `FIGHT land t=14 dx=-5.919 dz=12.100 result=STARTED` (13.470 from the centre), **then `FIGHTPHASE t=14 IDLE->FAST_CLOSE`, then `FIGHT mage spawned t=14 id=39fbc873-… at=-5.92 0.00 12.10`** (the prediction listed spawned before the phase line), `DRAGON mage-landed t=14 pending=0`; `FIGHT mage-health t=15 at=IDLE->FAST_CLOSE health=6000.000/6000.000`. (3) **`DRAGONGATE t=18 phase=FAST_CLOSE amount=100.000 source=hit refused`**, no FIGHTHIT for it. (4) **`DRAGONPROBE t=19 … health=15999.500/20000.000 … immune=true … progress=0.8000 … crossed=1 pending=0 …`**. (5) **`FIGHTHIT t=20 target=mage amount=60000.000 before_defense=60000.000 health=0.000/6000.000 dealer=39fbc873-…`** (the dealer is the Mage's own id: the dev kill is self-attributed), `FIGHT die t=20 phase=RETURNING slow_close_started=never`. (6) **`FIGHTHIT t=22 target=dragon amount=100.000 … health=15899.500/20000.000 …`**; **`DRAGONPROBE t=23 … immune=false … progress=0.7950 … crossed=1 pending=0 …`**. (7) **`FIGHTPHASE t=26 RETURNING->IDLE`** (the fast close ran 6 ticks, 14 to 20, and the return 6, 20 to 26); no second `mage-due`; the last FIGHTPROBE `mage=none`. Also, unpredicted and as the code says: `FIGHT mage-health t=21 at=FAST_CLOSE->RETURNING mage=none` and `t=26 at=RETURNING->IDLE mage=none`. (8) `outcome=RUNNING`, `dragon removed … was_valid=true`, the end |

### DA5 — one hit across two thresholds: two Mages, in sequence, never together (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `dragon hit 8000.5`: `FIGHTHIT … health=11999.500/20000.000 …`, then **`DRAGONCROSS t=<T> threshold=0.80 … pending=1`** and **`DRAGONCROSS t=<T> threshold=0.60 … pending=2`**, the same tick. (2) **`DRAGON mage-due t=<D> pending=2`**, the landing STARTED, **`DRAGON mage-landed t=<D> pending=1`**. **No second landing while he lives.** (3) The probe 40 ticks on: **`… immune=true … progress=0.6000 … crossed=2 pending=1 …`**. (4) `mage-die`, then on the tick the ring is home: **`FIGHTPHASE t=<H> RETURNING->IDLE`**, **`DRAGON mage-due t=<H> pending=1`**, `FIGHT land t=<H> … result=STARTED`, `FIGHT mage spawned t=<H> …`, **`DRAGON mage-landed t=<H> pending=0`**: **the second lands on the very tick the first's ring is home, never before** (the FIGHT die line is between the two `FIGHT mage spawned`). (5) The probe 100 ticks after the death: **`… crossed=2 pending=0 immune=true …`**. (6) `mage-die` again; the last probe `immune=false`. (7) Leave and the end. **If the second has not landed by the 100-tick probe, the probe reads `pending=1` and the row says so** | `DragonCrossingsTest` *oneHitAcrossTwoThresholds…*, `FightDragonSourceTest` *aQueuedMageLands…* | **AS PREDICTED EXCEPT ONE FIELD: BOTH CROSSINGS READ `pending=2`.** (1) `FIGHTHIT t=13 … health=11999.500/20000.000 …`, then **`DRAGONCROSS t=13 threshold=0.80 … pending=2`** and **`DRAGONCROSS t=13 threshold=0.60 … pending=2`**, the same tick. The prediction said `pending=1` on the first: the code logs each crossing after `observe` has returned both, so it prints the queue's depth after the hit, not a running count. A prediction error, not a code one. (2) `DRAGON mage-due t=14 pending=2`, `FIGHT land t=14 dx=28.617 dz=0.899 result=STARTED` (28.631 out), `FIGHT mage spawned t=14 …`, **`DRAGON mage-landed t=14 pending=1`**; no second landing while he lived. (3) The probe at t=52 (39 ticks on): **`immune=true … progress=0.6000 … crossed=2 pending=1`**. (4) `mage-die`: `FIGHT die t=53 …`; **`FIGHTPHASE t=92 RETURNING->IDLE`**, **`DRAGON mage-due t=92 pending=1`**, `FIGHT land t=92 dx=33.257 dz=9.000 result=STARTED` (34.453 out), `FIGHT mage spawned t=92 id=b1dfd4a7-…`, **`DRAGON mage-landed t=92 pending=0`**: the second landed on the tick the ring was home, with the FIGHT die line between the two spawns. (5) The probe at t=152 (99 ticks after the death): **`crossed=2 pending=0 … immune=true`**. (6) `mage-die` at t=153; the last probe t=157 `immune=false`. (7) `outcome=RUNNING`, `dragon removed … was_valid=true`, the end |

### DA6 — the fireball: the shooting phase set by the API; staged, or why not (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **PREDICTED: IT CANNOT BE STAGED BY THE API.** READ, the pinned server jar: only `DragonStrafePlayerPhase` raises `EnderDragonShootFireballEvent`; its `begin()` nulls its target, and only the holding pattern sets one. So: (1) **`DRAGON phase-set t=<t> phase=STRAFING now=STRAFING`**. (2) The next tick, vanilla's own **`[Server thread/WARN]: Skipping player strafe phase because no player was found`** (the jar's string), and the dragon back in its holding pattern. (3) **`DRAGONPROBE … phase=CIRCLING … fireballs_in_world=0`**. (4) **No `FIGHT fireball-cancelled` line.** The cancel itself is witnessed only by a fireball the dragon shoots on its own (DB1, DB2): **each one there must log `FIGHT fireball-cancelled … dragon=<its id>` and leave no fireball**. (5) Leave and the end. *If (2) does not appear and a fireball is cancelled here, the staging worked after all, and the row says so* | `FightDragonSourceTest` *theFireballCancelIsForTheFightsOwnDragonOnly* | **AS PREDICTED: IT CANNOT BE STAGED.** (1) **`DRAGON phase-set t=10 phase=STRAFING now=STRAFING`**. (2) **`[08:37:11] [Server thread/WARN]: Skipping player strafe phase because no player was found`**, vanilla's own line. (3) **`DRAGONPROBE t=15 … phase=CIRCLING … fireballs_in_world=0`**. (4) No `FIGHT fireball-cancelled` in the row. In Ben's rows: **three** `FIGHT fireball-cancelled … phase=STRAFING`, each naming the fight's own dragon (DB1 t=319; DB2 t=167, t=456). (5) `outcome=RUNNING`, `dragon removed … was_valid=true`, the end |

### DA7 — its death is the WIN: the line, the message, 200 ticks, the return; no dragon, no bar (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `dragon kill`: **`FIGHTHIT … target=dragon amount=200000.000 before_defense=200000.000 health=0.000/20000.000 …`**. (2) The same tick: **`FIGHT win t=<W> dragon=<its id> exp_suppressed=500 drops_suppressed=0 return_in=200`** (500: READ, `EnderDragon.getExpReward` is 500 with no vanilla fight and `mob_drops` on; that the death event carries it is INFERRED), `FIGHT mage-health t=<W> at=win mage=none`, and the chat **`The Ender Dragon is dead. You win.`**. (3) **200 ticks later:** `FIGHT win-return t=<W> after=200` (the fight's clock stops with the ring), `FIGHT mage-health … at=end mage=none`, **`BOSSBAR hidden … why=fight-ended`**, **`FIGHT dragon removed reason=win was_valid=<true or false> …`**. That is a RANGE, named: vanilla removes a dying dragon at its own 200th death tick, the same tick our return falls on, and which runs first is not predicted. Then `FIGHT ended reason=win`, **`FIGHTWORLD returned … why=end`**, unloaded, deleting, deleted. (4) **`AWAIT fight ended ticks=<about 200>`**. (5) `FIGHTWORLDS loaded=0 folders=0 marked=0 you=world`; the probe **`No fight fixture is running.`**. No experience orb near him (the end probes) | `FightDragonSourceTest` *theDragonsDeathIsTheWin…* | **AS PREDICTED IN EVERY NAMED LINE; FOUR KINDS OF LINE NOT PREDICTED.** (1) **`FIGHTHIT t=13 target=dragon amount=200000.000 before_defense=200000.000 health=0.000/20000.000 dealer=9e78f985-…`** (the dealer is the dragon's own id). (2) **`FIGHT win t=13 dragon=9e78f985-3b0c-4a42-83ee-6a392d4157b1 exp_suppressed=500 drops_suppressed=0 return_in=200`**, `FIGHT mage-health t=13 at=win mage=none`; the chat **`The Ender Dragon is dead. You win.`** [gold]. (3) 200 ticks later (08:37:16 to 08:37:26): `FIGHT win-return t=13 after=200`, `FIGHT mage-health t=13 at=end mage=none`, **`BOSSBAR hidden t=13 … why=fight-ended`**, **`FIGHT dragon removed reason=win was_valid=false id=9e78f985-…`** (inside the named range: vanilla's own death removal ran first), `FIGHT ended reason=win`, **`FIGHTWORLD returned … why=end`**, unloaded, deleting, deleted. (4) **`AWAIT fight ended ticks=201`**. (5) `FIGHTWORLDS loaded=0 folders=0 marked=0 you=world`; `No fight fixture is running.` **NOT PREDICTED:** four `DRAGONCROSS t=13 threshold=0.80/0.60/0.40/0.20 health=0.000/20000.000 pending=4` before the win line (no Mage lands: the fight stops the same tick); `FIGHT stop t=13`; **`FIGHTWORLD unload-retry name=rpgfight_fight_65308ec6 players=1 attempt=1`** after the return, then unloaded a tick later. **Experience orbs: no probe in this row counts them** (the end probes read END_CRYSTAL and WITHER_SKELETON); the run's `RESTORED … orbsRemoved=0` is the only orb figure. Vanilla's death message reads the dragon at `y=85.00`, 20 above the floor (HOVER) |

### DA8 — a failure with the dragon alive: the dragon removed, the folder gone (SURVIVAL; DIES ON PURPOSE)

| prediction | stands on | READING |
|---|---|---|
| AE4R2's mechanism with the dragon present. (1) `DEATH_RULES …`; `FIGHT land t=<L> dx=0.000 dz=0.000 result=STARTED`. (2) **`FIGHT mage-health … at=IDLE->FAST_CLOSE health=6000.000/6000.000`**, **`… at=FAST_CLOSE->SLOW_CLOSE health=6000.000/6000.000`**, `FIGHTPHASE t=<L+590> SLOW_CLOSE->CLOSED`, `FIGHT FAILED_CLOSED t=<L+590>`, the chat `"The fight has failed: FAILED_CLOSED."`, and **`… at=SLOW_CLOSE->CLOSED health=6000.000/6000.000`**. (3) STRIKEs outside k = 1, 2, … with their HURT lines (k=5 killed him in AE4R2: 20, 40, 80, 160, 320 before armour; 18.182, 36.364, 72.727, 145.455, 290.909 after it at defence 10). (4) `FIGHTWORLD died-inside`, `FIGHTWORLD returned … why=died`, **`BOSSBAR hidden … why=left`**, `FIGHT nobody inside t=<t> outcome=FAILED_CLOSED`, **`FIGHT mage-health … at=end health=6000.000/6000.000`**, **`FIGHT dragon removed reason=nobody-inside-FAILED_CLOSED was_valid=true`**, the end. (5) `AWAIT fight ended ticks=<n>`. **No RESPAWNED line** (the arena boot read why: immediate respawn returns him alive on the death's tick, so `await_fight` takes the live branch). (6) **No DRAGONGATE** (nothing hits the dragon) | `RingFixtureTest`, `FightDragonSourceTest` *theDragonIsRemovedAtEveryEnding* | **AS PREDICTED.** (1) `DEATH_RULES keep_inventory=false->true immediate_respawn=false->true`; `FIGHT land t=20 dx=0.000 dz=0.000 result=STARTED`. (2) `FIGHT mage-health t=21 at=IDLE->FAST_CLOSE health=6000.000/6000.000`, `t=210 at=FAST_CLOSE->SLOW_CLOSE health=6000.000/6000.000`, **`FIGHTPHASE t=610 SLOW_CLOSE->CLOSED`** (L+590), `FIGHT FAILED_CLOSED t=610`, `t=610 at=SLOW_CLOSE->CLOSED health=6000.000/6000.000`; the chat `The fight has failed: FAILED_CLOSED.` (3) STRIKE k=1 to k=5 outside, t=640 to t=800, with HURT `before=20.000 after=18.182`, `40.000/36.364`, `80.000/72.727`, `160.000/145.455`, `320.000/290.909`, all `defense=10.000 dealer=self`; k=5 killed him. (4) `FIGHTWORLD died-inside`, `BaronVonYeetus died`, `FIGHTWORLD returned … why=died`, **`BOSSBAR hidden t=802 … why=left`**, `FIGHT nobody inside t=802 outcome=FAILED_CLOSED`, **`FIGHT mage-health t=802 at=end health=6000.000/6000.000`**, **`FIGHT dragon removed reason=nobody-inside-FAILED_CLOSED was_valid=true`**, the end, `deleted … entries=16`. (5) `AWAIT fight ended ticks=782`; no RESPAWNED line. (6) No DRAGONGATE. Unpredicted and harmless: `CRYSTAL t=450 lit index=1` and `t=610 removed index=1` |

### DA9 — a melee hit on a PART reaches the dragon (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| He holds `emberblade` (MG2R2's staging: a melee weapon, so `Player.attack` raises a damage event). (1) **`DRAGON strike t=<t> part=head part_id=<uuid, not the dragon's> by=BaronVonYeetus`**, and the reply `Struck the dragon's head.` (the part named from its box; READ, head 1 x 1). (2) Then **`FIGHTHIT … target=dragon amount=<A> before_defense=<A> health=<20000 − A>/20000.000 dealer=<his uuid> element=fire`**, **A > 0**, its first. **That line IS the witness that the swing was recorded against the dragon**: before this branch `onPrePlayerAttack` returned on a part and the store took nothing. (3) The same for **`part=wing`** (READ, wing 4 x 2): a second FIGHTHIT, health = 20000 − A − B. (4) The probe: `health=<the last FIGHTHIT's>`. **Any later FIGHTHIT in the row with element `fire` is emberblade's own burn (Scorch), not a swing; it is reported, not predicted.** A **reply `No part 'head'; the parts are [...]`** would mean the box naming failed, and the row says so | `LiveTargetsTest` *aPartOfALiveDragon…*, *everyHitSiteResolvesAPart…*; `DragonPartsTest` | **AS PREDICTED.** `DRIVE hold … item=emberblade`. (1) **`DRAGON strike t=35 part=head part_id=14849056-4c79-4e32-9a8a-e03670236963 by=BaronVonYeetus`** (not the dragon's id), the reply `Struck the dragon's head.` (2) **`FIGHTHIT t=36 target=dragon amount=56.000 before_defense=56.000 health=19944.000/20000.000 dealer=b6ae27e9-… element=fire`**: A = 56. Then the burn, reported: `FIGHTHIT t=57 … amount=28.000 … health=19916.000/20000.000 … element=fire`. (3) **`DRAGON strike t=60 part=wing part_id=898fbc51-…`**, `Struck the dragon's wing.`, **`FIGHTHIT t=60 … amount=56.000 … health=19860.000/20000.000 … element=fire`**: B = 56, and the health is 20000 − 56 − 28 − 56 (the burn between them). (4) `DRAGONPROBE t=62 … health=19860.000/20000.000 … progress=0.9930 …`. No `No part` reply |

---

## BEN'S ROWS — HIS WORDS, VERBATIM; "not sure" IS NOT STATED; ONE THING PER QUESTION; NO PASS OR FAIL

### DB1 — the dragon, watched and hit: questions 1 to 4 (SURVIVAL)

| prediction | READING |
|---|---|
| Three minutes with the dragon free (CIRCLING from its spawn), then ASK 1 (`yesno`) "The dragon: did it fly and land as you expect, with no fireball?", ASK 2 (`yesno`) "Could you hit it with your own weapon while it was in the air?", ASK 3 (`yesno`) "Could you hit it with your own weapon while it had landed?", ASK 4 (`yesno`) "The boss bar at the top of the screen: is it right?". **Auto, reported:** every FIGHTHIT on the dragon (tick, amount, health after); every `DRAGONGATE`; every `FIGHT fireball-cancelled` (and `fireballs_in_world=0` if probed); any DRAGONCROSS and any Mage that lands. **INFERRED, to be read here:** a perched dragon takes 0 from an arrow (READ: the sitting phases' `onHurt` zeroes an arrow), which our weapons do not fire | **HIS WORDS:** ASK 1 **"yes"**; ASK 2 **"yes"**; ASK 3 **"yes"**; ASK 4 **"not we'll get to that"**. **THE FIGHT ENDED INSIDE THIS ROW: HE KILLED THE DRAGON** (`FIGHT win t=2984 dragon=e0a4907b-… exp_suppressed=500 drops_suppressed=0 return_in=200`; 08:40:50, 2.5 minutes in), not predicted; the return at t=2984 + 200 (`win-return`, `BOSSBAR hidden … why=fight-ended`, `dragon removed reason=win was_valid=false`, `FIGHTWORLD returned … why=end`, `unload-retry … attempt=1`, deleted). The questions came after. **Hits on the dragon:** 94 FIGHTHIT, all from him: 88 kinetic (204.250 or 408.500) and 6 fire (28 to 112, t=828 to 869). First t=320 health 19591.500, last t=2984 health 0.000. **The dragon's phase at each hit is NOT KNOWN**: FIGHTHIT does not carry it. Vanilla's death line puts it at `x=0.61, y=65.86, z=0.04`, on the podium, when it died. **DRAGONCROSS:** 0.80 at t=869 (15987.500), 0.60 at t=2086 (11872.750), 0.40 at t=2723 (7992.000), 0.20 at t=2830 (3907.000, `pending=2`). **Mages:** two landed (t=870, 15.681 out; t=2191, 15.338 out), both killed by him; **the 0.40 and 0.20 Mages never landed** (crossed while the ring was RETURNING; the dragon died first). **DRAGONGATE:** 14 refused: 2 `vanilla/entity_attack` 2.750, 6 `hit/fire` 28.000, 6 `hit/kinetic` 204.250 or 408.500. **THE DRAGON HEALED 1400**: 15987.500 after t=869, 17387.500 before t=1766, while the fight's crystal was lit (t=1383 to 1530): see the findings. **Fireballs:** one `FIGHT fireball-cancelled t=319 dragon=e0a4907b-… phase=STRAFING`. Whether a fireball entity ever existed: NOT READ (no probe ran in this row); INFERRED none, because the event is cancelled before the spawn. **The perched breath: NOT READ.** No HURT line with any cause but the dragon and `self`; whether he stood in a breath cloud is not in the log. **The dragon's own hits:** 10 HURT `before=0.000 after=0.000 defense=10.000 dealer=e0a4907b-… dealerType=ender_dragon/ender_dragon` (t≈08:39:00 to 08:39:02 and 08:39:55 to 56): **it lands 0 on his custom health, as INFERRED.** Vanilla damage to him: not in our log (INFERRED tokened). **STRIKE outside:** k=1 at t=1520 (HURT 20.000 to 18.182) and k=2 at t=1560 (40.000 to 36.364). **He did not die** |

### DB2 — THE FIGHT, NO SAFETY: questions 5 and 6 (SURVIVAL; he may die)

| prediction | READING |
|---|---|
| Up to ten minutes (`await_fight 12000`) of the dragon and the Mages with his own weapon. It ends ONE of these ways, and the log says which: **the win** (`FIGHT win …`, then the return 200 ticks later); **his death** (`FIGHTWORLD died-inside`, then `FIGHT nobody inside … outcome=<o>`: one player, so one death ends it); **a failure** (FAILED_CLOSED or FAILED_POWERED, then his death by it); or **the ceiling** (`AWAIT fight ceiling ticks=12000 still running`, then `rpg fight stop`). **Reported:** each crossing (`DRAGONCROSS`); each Mage's landing, his health at each phase change (`FIGHT mage-health`) and at every landed hit (`FIGHTHIT target=mage`), and his time to kill (`FIGHT die … ticks_in_slow_close=<n>`); every FIGHTHIT on the dragon; his deaths; every STRIKE and HURT; every `DRAGONGATE`; every fireball cancelled; the ending. Then ASK 5 (`yesno`) "For one player, is the dragon's 20,000 health right?" and ASK 6 (`words`) "What most needs to change before waves are built?" | **HIS WORDS:** ASK 5 **"yes"**; ASK 6 **"not much if anything"**. **ENDING: THE WIN.** `FIGHT win t=2487 dragon=c764da6b-… exp_suppressed=500 drops_suppressed=0 return_in=200` (08:44:10), the return 200 ticks later, `AWAIT fight ended ticks=2687`, then the row's `rpg fight stop` replied `No fight fixture is running.`; `unload-retry … attempt=1`; deleted. **He did not die; no STRIKE outside; no Ben HURT but the dragon's** (16 HURT `before=0.000 after=0.000 … dealerType=ender_dragon/ender_dragon`, 08:43:55 to 56). **Crossings:** 0.80 at t=717 (15915.000, `pending=1`), **0.60 at t=1494, 0.40 at t=1625 and 0.20 at t=1762 (3964.250, `pending=3`), all three while the ring was RETURNING from the first Mage's death** (t=1285 to 1852). **Mages:** M1 landed t=718 at 21.612 out; slow at t=1022; killed t=1285, `ticks_in_slow_close=263`; trail 6000 → 5591.5 at t=1048 … 70.763 at t=1272 → 0 at t=1285, 29 hits (25 kinetic, 4 fire). M2 landed t=1852 at 22.565 out (`pending=2` after); slow at t=2161; killed t=2433, `ticks_in_slow_close=272`; trail 6000 → 5591.5 at t=2247 … 76.750 at t=2383 → 0 at t=2433, 25 hits, all his in our store, **but vanilla's death line reads `Withered Dark Mage was slain by Ender Dragon`**. **Two Mages still owed at the win** (0.40's and 0.20's): the dragon was hittable again in M2's RETURNING and died at t=2487. **MAGEGATE:** 30 refused during M2's FAST_CLOSE (t=1942 to 2096). **DRAGONGATE:** 12 refused (`hit/kinetic`). **Hits on the dragon:** 82 FIGHTHIT, all kinetic from him, first t=614 (19795.750), last t=2487 (0.000). **The dragon healed** +100 (while the crystal was lit, t=1262 to 1285) and +300 (crystal lit t=2401 to 2433). **Fireballs:** two cancelled (t=167, t=456, `phase=STRAFING`, the fight's own dragon). The dragon died at `x=30.66, y=83.42, z=26.65`, in the air |

---

## WHAT THE DRAGON'S OWN ATTACKS DO TODAY (the brief's 3 g: READ AND REPORTED, NOTHING CHANGED)

The account is `PLAN-dragon-fight.md` §6.8. In short:
- **Its head and its wings deal 0 to a player's custom health.**
  - READ: vanilla deals 10 and 5, as `mob_attack` from the dragon.
  - READ: the dragon has no `ATTACK_DAMAGE` attribute, and our seed reads 0.0 when there is none.
  - INFERRED: our mob-melee rider then lands its seeded 0, and vanilla's number is tokened.
- **The wings' push is vanilla's.**
- **The perched breath is an open question:**
  - READ: Instant Damage at level 0, 3 a hit, `indirect_magic`.
  - INFERRED: Bukkit names it ENTITY_ATTACK with the cloud as damager, so neither our mob rider (the damager is not
    living) nor a reroute catches it.
- **DB1 and DB2 read whatever HURT lines the dragon's attacks produce.**

## WHAT THIS GATE DOES NOT READ

- **What Ben's client draws** (the dragon's flight, its perch, the bar): his rows.
- **A real attack packet on a part**: DA9 drives `Player.attack`, which skips the packet's reach and charge gates;
  DB1 is the real swing.
- **A ray or burst on a part**: `LiveTargetsTest` pins the filter and the scan. Ben's own weapon in DB1 and DB2 reads
  it in play.
- **Folia**: the fight's lines are written on the region and entity threads, which on Paper are the main thread.
- **Two fights at once**: possible in the code, not staged.

---

## THE BOOT OF 2026-11-19 — THE CHECKLIST, THE AFTER-RUN, WHAT THE RUN SHEET DID NOT SAY, AND THE FINDINGS

Written after Ben's `/stop`. **No prediction above was edited**; each READING sits beside its prediction.

### The booter's checklist (`.claude/rules/booting.md`), ticked

1. **The seat's word and Ben's:** Ben's "ready" (his own message); then the seat's `BOOT APPROVED for PR #213`,
   2026-11-19, having diffed the code and this gate from origin at `7e2ebfa8`.
2. **The heads:** `git ls-remote --heads origin feat/fight-dragon` printed `7e2ebfa8ebe0704290a34a05b0ec824177b8ad68`,
   what the seat diffed; local HEAD the same; the tree clean.
3. **The carried state, in order:**
   1. `Get-Process java` printed nothing.
   2. Backup `../rpg-run-backups/2026-11-19-pre-fight-dragon`: Rpg `files=132 dirs=24`, `diff -rq lines: 0`; world
      `files=193 dirs=32`, `diff -rq lines: 0`. `world/dimensions`: `minecraft`, `rpgfight`. The overworld: **106**
      files.
   3. `12:  dev-pack-on-join: true`.
   4. `find run/world/dimensions/rpgfight -mindepth 1` printed nothing.
4. **"Booting now", in its own message**, with "Ben: do NOT log in until I tell you R0 passed", sent and the turn ended
   before the boot command was sent.
5. `./scripts/dev-server.sh --refresh-content` (`==> Building 7e2ebfa8`); `Done (7.414s)` at 08:30:55. The three
   REQUIRED deployed lines: `76:  mage_thresholds: [0.80, 0.60, 0.40, 0.20]`; `11:max_health: 20000`
   (`ender_dragon.yml`); `14:max_health: 6000` (`dark_mage.yml`).
6. **R0a to R0e read on this boot's log**, `[Rpg] Build: 7e2ebfa8`: all as predicted (the rows above).
7. **"R0 passed", in its own message**, sent after R0 was read and before he logged in.
8. **His login against it:** the R0 lines are all at 08:30:55; R0 was read and "R0 passed" sent before his first
   `BaronVonYeetus joined the game` at **08:35:38**. He joined after.
9. **After his `/stop` (08:44:44):** `find run/world/dimensions/rpgfight -mindepth 1` printed nothing; **the overworld
   106 files, against the backup's 106**; `ThreadedAnvilChunkStorage: All dimensions are saved` and `All RegionFile
   I/O tasks to complete` read at 08:44:44; `Get-Process java` printed nothing.

### Every FIGHTWORLD deleting and deleted path

Ten fights, ten `deleting`/`deleted` pairs, each `entries=16`, each under
`C:\Users\Neb91\IdeaProjects\minecraft-rpg-scaffold\run\world\dimensions\rpgfight\`: `fight_21f301ea` (DA1),
`fight_a775948e` (DA2), `fight_eda9377b` (DA3), `fight_31a7eb7c` (DA5), `fight_e1be96a1` (DA6), `fight_65308ec6`
(DA7), `fight_06bf457d` (DA8), `fight_2eb0a96d` (DA9), `fight_7e933e68` (DB1), `fight_cba3b60d` (DB2).
`FIGHTWORLD created` took 31 to 58 ms; `arena built` 660 to 1187 ms.

### The log's warnings

- **`[Rpg]` lines matching Refusing, Skipping, SEVERE, WARN or Exception:** `volley_stone`'s pair only, at the enable.
- **Vanilla's own `[Server thread/WARN]: Skipping player strafe phase because no player was found`** at 08:37:11:
  DA6's, predicted.
- Paper's "4 release(s) behind" banner (Paper Async Task Handler), not ours.

### What the run sheet did not say, recorded as read (no PASS or FAIL)

- **`/rpg mana refill`, typed 35 times**: once at 08:35:59, between `/gamemode survival` and the selftest, and 34
  times during DB1 and DB2. Not on the run sheet. Its effect is on his mana only; no row reads mana.
- **At 08:38:45 (DB1) he pasted the seat's End question into game chat** and then wrote "oops" and "ignore".
- `/rpg selftest fight-dragon` ran ONCE; `/stop` followed `SELFTEST RUN FINISHED` and `RESTORED … keep_inventory=false
  immediate_respawn=false difficulty=NORMAL`.

### FINDINGS — the readings above that the predictions or the plan did not expect

1. **THE FIGHT'S OWN END CRYSTAL HEALS THE DRAGON IN OUR STORE.** DB1: +1400 (15987.500 to 17387.500) while the
   crystal was lit, t=1383 to 1530; DB2: +100 (t=1262 to 1285) and +300 (t=2401 to 2433).
   - INFERRED mechanism: vanilla's dragon heals 1 point from its nearest crystal every 10 ticks.
   - READ, our source: `MobHealPolicy` (M15) maps `ENDER_CRYSTAL` to REROUTE, and `healTrackedMob` heals the store by
     `DamageScale.toCustom`, which is 1 / 200 × 20000 = 100 a heal. Fourteen heals is 1400.
   - **`PLAN-dragon-fight.md` §6.8 said a crystal heals only the dragon's vanilla health ("VanillaHealPolicy acts
     only on tracked players"). That was wrong:** it missed the mob arm, `MobHealPolicy`.
   - Nothing is changed here. Whether a crystal should heal the dragon is Ben's call; the crystal is the Mage's.
2. **THE DRAGON CAN BE HIT WHILE A MAGE IS OWED AND NOT YET LANDED.** The ring's return after a Mage's death is
   RETURNING, not IDLE, so the queued Mage waits and the dragon is not immune (built as the brief said: "immune while a
   Mage is alive").
   - DB2: 0.60, 0.40 and 0.20 were all crossed during one return (t=1285 to 1852, about 28 seconds), `pending=3`.
   - **In both of Ben's fights only TWO of the four Mages landed before the win**, and two were still owed.
   - Ben's answer to question 6 was "not much if anything". A ruling for the seat and Ben: whether the dragon should be
     immune while a Mage is OWED, or the queued Mage should land without waiting for the ring.
3. **DA5's `pending=` on the first of two crossings was mispredicted** (`2`, not `1`): the code logs every crossing
   after `observe` returns them all. A prediction error.
4. **AT THE WIN, `FIGHTWORLD unload-retry … players=1 attempt=1`** (DA7, DB1, DB2): the world is unloaded a tick after
   the return, once he has left it. The fight's other endings did not need it. Not predicted; the unload then succeeded
   each time.
5. **Vanilla credits the dragon with a kill our store credits to Ben** (DB2: `Withered Dark Mage was slain by Ender
   Dragon`; our last FIGHTHIT on him is Ben's 408.500 at t=2433). Vanilla's death message only; nothing reads it.
6. **The dragon's own hits land 0 on him**, as INFERRED: 26 HURT `before=0.000 after=0.000 … dealerType=ender_dragon`
   across DB1 and DB2. **The perched breath was NOT READ** (no HURT with any other cause), and the dragon's phase at
   any hit is not in the log.
7. **A kill crosses every threshold at once** (DA7: four DRAGONCROSS, `pending=4`); the fight stops the same tick, so
   no Mage lands. Not predicted; as the code says.

---

> **2026-11-21, THE FOUR DEV WEAPONS DELETED** (`feat/remove-dev-weapons`, `GATE-remove-dev-weapons.md`; the seat's
> brief, rulings 1 and 2). **This gate's scenario changed: DA9 now gives and holds `iron_sword`**, where it gave and held
> `emberblade`, a dev weapon deleted that day. **Every prediction and reading above was taken with the old weapon** and
> none is edited. **DA9's prediction names the FIRE element** (`element=fire` on each FIGHTHIT, and a later fire
> FIGHTHIT as the emberblade's Scorch burn): the sword is kinetic, so that text cannot hold as written, and the row is
> LISTED FOR THE SEAT rather than judged here. **A re-run of DA9 needs a re-prediction under the next suffix.** NOT
> RE-READ on a server.
>
> **2026-11-21, THE SEAT RULED ON DA9: KEEP IT REPOINTED to `iron_sword`.** Its subject is that a melee hit on a dragon
> PART reaches the dragon by the melee path and the store takes the weapon's figure; fire was the weapon's, not the
> row's. **A re-run is DA9R2**, predicting `element=kinetic` and no Scorch carve-out. BS2, MG2R2 and WD1 (in their own
> gates) stand the same way.
