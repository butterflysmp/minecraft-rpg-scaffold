# GATE — STEP F9: THE DRAGON

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/fight-dragon`, and this file is
committed before the boot.
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
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/fight-dragon` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries this step

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/fights/dragon.yml` line `  mage_thresholds: [0.80, 0.60, 0.40, 0.20]`; `content/mobs/ender_dragon.yml` line `max_health: 20000`; `FightInstance.class` carrying `FIGHT dragon spawned`; `RingFixture.class` carrying `DRAGONGATE`; the entry `io/github/butterflysmp/rpg/core/fight/DragonCrossings.class`; `GATE-fight-dragon.yml` carrying `DA9:`; `GATE-fight-arena.yml` carrying `rpg fight enter nodragon`. ABSENT: `content/mobs/ender_dragon.yml` line `max_health: 18000` (the YAML control), `NoSuchTextControl` in `FightInstance.class` (the class control) | the PowerShell below, from the repo root | |

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
| **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`** (the arena boot's line with `7 mobs` for `6`: `ender_dragon.yml` added), AND exactly one **`[Rpg] Loaded fight 'dragon': border 48.0, mage ring 12.0, v_fast 3.80, v_slow 0.60, e_kill 1.00, 10 crystals every 240 ticks, a strike every 40 ticks from 20.0; ring dust (170,0,255) size 1.60 every 1.00 blocks at [0.2, 1.7, 3.2, 4.7, 6.2, 7.7], row shift 0.25, turn 1.00/s alternating, every 4 ticks; mage 'dark_mage' refused item.shield.block; crystals at 40.0, 12.0 up, lit block.beacon.power_select; participants within 128.0; arena at (0, 0) floor y 64, disc 56 x 3, 10 pillars 3 wide on 40.0 to +12, time 18000, entry (0.0, 0.0); dragon 'ender_dragon' Mages below [0.8, 0.6, 0.4, 0.2], win return 200 ticks, bar purple`** (`FightLoaderTest` asserts it verbatim). **No `Refusing`, `Skipping`, `SEVERE` or `Exception`; no `[Rpg]` WARN beyond `volley_stone`'s known pair**; no `No fights loaded` | `Select-String run\logs\latest.log -Pattern 'Loaded\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded'` (up to the first `joined the game`) | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 210 scenarios across 24 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive, fight-site, fight-arena, fight-dragon]`**, no refusal after it. **The control:** the arena boot's `200 scenarios across 23 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (2026-11-18, this branch's tree; `SelfTestScenariosTest` asserts 210):
- The gates in `index.yml`: **24**.
- The key count (`^[A-Za-z0-9_-]+:` over the 24 files) prints **234**.
- `^setup:` prints **24**.
- So 234 − 24 = **210**.
- **Control:** the arena boot's 223 + fight-dragon's 11 = 234.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, and **no `FIGHTWORLD deleting` line and no `orphan` line at all** | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | |

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
| (1) The birth, the dragon's spawn line, entered, `BOSSBAR shown …`, `DRAGON phase-set … phase=HOVER now=HOVER`. (2) **`DRAGONPROBE t=<t> dragon=<the spawn line's id> health=20000.000/20000.000 defense=0.000 phase=HOVER podium=-0.50 0.00 -0.50 immune=false bar=PURPLE progress=1.0000 viewers=1 crossed=0 pending=0 dragons_in_world=1 fireballs_in_world=0`**. (3) `FIGHTWORLDS loaded=1 folders=1 marked=1 you=<n>`. (4) Leave and the end, `outcome=RUNNING`, **`FIGHT mage-health … at=end mage=none`**, **`FIGHT dragon removed reason=nobody-inside-RUNNING was_valid=true`**. (5) After it, the probe replies **`No fight fixture is running.`** | `FightDragonSourceTest`, `FightLoaderTest` | |

### DA2 — a store hit with no Mage alive lands by the pipeline's exact figure (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `dragon hit 1234.5`: **`FIGHTHIT t=<t> target=dragon amount=1234.500 before_defense=1234.500 health=18765.500/20000.000 dealer=<his uuid> element=null`**, exactly one; **no DRAGONGATE and no DRAGONCROSS** (0.938275 is above 0.80). (2) **`DRAGONPROBE … health=18765.500/20000.000 defense=0.000 phase=HOVER … immune=false bar=PURPLE progress=0.9383 viewers=1 crossed=0 pending=0 …`** (0.9383 computed by `jshell`). (3) Leave and the end, `outcome=RUNNING` | `FightDragonSourceTest` *theLandedHitLine…*; `DragonCrossingsTest` | |

### DA3 — below 80% lands one Mage; a hit while he lives is refused; his death: the dragon hurt again, the ring home (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `dragon hit 4000.5`: **`FIGHTHIT … target=dragon amount=4000.500 before_defense=4000.500 health=15999.500/20000.000 …`**, then **`DRAGONCROSS t=<T> threshold=0.80 health=15999.500/20000.000 pending=1`**. (2) On a later loop tick: **`DRAGON mage-due t=<D> pending=1`**, `FIGHT land t=<D> dx=<x> dz=<z> result=STARTED`, `FIGHT mage spawned t=<D> …`, `FIGHTPHASE t=<D> IDLE->FAST_CLOSE`, **`DRAGON mage-landed t=<D> pending=0`**; the next tick `FIGHT mage-health t=<D+1> at=IDLE->FAST_CLOSE health=6000.000/6000.000`. (3) `dragon hit 100`: **`DRAGONGATE t=<t> phase=FAST_CLOSE amount=100.000 source=hit refused`**, and no FIGHTHIT for it. (4) **`DRAGONPROBE … health=15999.500/20000.000 … immune=true … progress=0.8000 … crossed=1 pending=0 …`**. (5) `mage-die`: **`FIGHTHIT … target=mage amount=60000.000 before_defense=60000.000 health=0.000/6000.000 …`**, `FIGHT die t=<t> phase=RETURNING slow_close_started=never`. (6) `dragon hit 100`: **`FIGHTHIT … target=dragon amount=100.000 … health=15899.500/20000.000 …`**; **`DRAGONPROBE … health=15899.500/20000.000 … immune=false … progress=0.7950 … crossed=1 pending=0 …`**. (7) Within the 100-tick wait, **`FIGHTPHASE t=<H> RETURNING->IDLE`** (a return from a fast close lasts as long as the fast close ran: `ReturnPath.fromFastClose`), and **no second `DRAGON mage-due`**. The last FIGHTPROBE `mage=none`. (8) Leave and the end, `outcome=RUNNING`. **The dx/dz of the landing are random and READ** | `DragonCrossingsTest`, `RingFixtureTest` *aMageIsAlive…*, `FightDragonSourceTest` | |

### DA5 — one hit across two thresholds: two Mages, in sequence, never together (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `dragon hit 8000.5`: `FIGHTHIT … health=11999.500/20000.000 …`, then **`DRAGONCROSS t=<T> threshold=0.80 … pending=1`** and **`DRAGONCROSS t=<T> threshold=0.60 … pending=2`**, the same tick. (2) **`DRAGON mage-due t=<D> pending=2`**, the landing STARTED, **`DRAGON mage-landed t=<D> pending=1`**. **No second landing while he lives.** (3) The probe 40 ticks on: **`… immune=true … progress=0.6000 … crossed=2 pending=1 …`**. (4) `mage-die`, then on the tick the ring is home: **`FIGHTPHASE t=<H> RETURNING->IDLE`**, **`DRAGON mage-due t=<H> pending=1`**, `FIGHT land t=<H> … result=STARTED`, `FIGHT mage spawned t=<H> …`, **`DRAGON mage-landed t=<H> pending=0`**: **the second lands on the very tick the first's ring is home, never before** (the FIGHT die line is between the two `FIGHT mage spawned`). (5) The probe 100 ticks after the death: **`… crossed=2 pending=0 immune=true …`**. (6) `mage-die` again; the last probe `immune=false`. (7) Leave and the end. **If the second has not landed by the 100-tick probe, the probe reads `pending=1` and the row says so** | `DragonCrossingsTest` *oneHitAcrossTwoThresholds…*, `FightDragonSourceTest` *aQueuedMageLands…* | |

### DA6 — the fireball: the shooting phase set by the API; staged, or why not (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **PREDICTED: IT CANNOT BE STAGED BY THE API.** READ, the pinned server jar: only `DragonStrafePlayerPhase` raises `EnderDragonShootFireballEvent`; its `begin()` nulls its target, and only the holding pattern sets one. So: (1) **`DRAGON phase-set t=<t> phase=STRAFING now=STRAFING`**. (2) The next tick, vanilla's own **`[Server thread/WARN]: Skipping player strafe phase because no player was found`** (the jar's string), and the dragon back in its holding pattern. (3) **`DRAGONPROBE … phase=CIRCLING … fireballs_in_world=0`**. (4) **No `FIGHT fireball-cancelled` line.** The cancel itself is witnessed only by a fireball the dragon shoots on its own (DB1, DB2): **each one there must log `FIGHT fireball-cancelled … dragon=<its id>` and leave no fireball**. (5) Leave and the end. *If (2) does not appear and a fireball is cancelled here, the staging worked after all, and the row says so* | `FightDragonSourceTest` *theFireballCancelIsForTheFightsOwnDragonOnly* | |

### DA7 — its death is the WIN: the line, the message, 200 ticks, the return; no dragon, no bar (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `dragon kill`: **`FIGHTHIT … target=dragon amount=200000.000 before_defense=200000.000 health=0.000/20000.000 …`**. (2) The same tick: **`FIGHT win t=<W> dragon=<its id> exp_suppressed=500 drops_suppressed=0 return_in=200`** (500: READ, `EnderDragon.getExpReward` is 500 with no vanilla fight and `mob_drops` on; that the death event carries it is INFERRED), `FIGHT mage-health t=<W> at=win mage=none`, and the chat **`The Ender Dragon is dead. You win.`**. (3) **200 ticks later:** `FIGHT win-return t=<W> after=200` (the fight's clock stops with the ring), `FIGHT mage-health … at=end mage=none`, **`BOSSBAR hidden … why=fight-ended`**, **`FIGHT dragon removed reason=win was_valid=<true or false> …`**. That is a RANGE, named: vanilla removes a dying dragon at its own 200th death tick, the same tick our return falls on, and which runs first is not predicted. Then `FIGHT ended reason=win`, **`FIGHTWORLD returned … why=end`**, unloaded, deleting, deleted. (4) **`AWAIT fight ended ticks=<about 200>`**. (5) `FIGHTWORLDS loaded=0 folders=0 marked=0 you=world`; the probe **`No fight fixture is running.`**. No experience orb near him (the end probes) | `FightDragonSourceTest` *theDragonsDeathIsTheWin…* | |

### DA8 — a failure with the dragon alive: the dragon removed, the folder gone (SURVIVAL; DIES ON PURPOSE)

| prediction | stands on | READING |
|---|---|---|
| AE4R2's mechanism with the dragon present. (1) `DEATH_RULES …`; `FIGHT land t=<L> dx=0.000 dz=0.000 result=STARTED`. (2) **`FIGHT mage-health … at=IDLE->FAST_CLOSE health=6000.000/6000.000`**, **`… at=FAST_CLOSE->SLOW_CLOSE health=6000.000/6000.000`**, `FIGHTPHASE t=<L+590> SLOW_CLOSE->CLOSED`, `FIGHT FAILED_CLOSED t=<L+590>`, the chat `"The fight has failed: FAILED_CLOSED."`, and **`… at=SLOW_CLOSE->CLOSED health=6000.000/6000.000`**. (3) STRIKEs outside k = 1, 2, … with their HURT lines (k=5 killed him in AE4R2: 20, 40, 80, 160, 320 before armour; 18.182, 36.364, 72.727, 145.455, 290.909 after it at defence 10). (4) `FIGHTWORLD died-inside`, `FIGHTWORLD returned … why=died`, **`BOSSBAR hidden … why=left`**, `FIGHT nobody inside t=<t> outcome=FAILED_CLOSED`, **`FIGHT mage-health … at=end health=6000.000/6000.000`**, **`FIGHT dragon removed reason=nobody-inside-FAILED_CLOSED was_valid=true`**, the end. (5) `AWAIT fight ended ticks=<n>`. **No RESPAWNED line** (the arena boot read why: immediate respawn returns him alive on the death's tick, so `await_fight` takes the live branch). (6) **No DRAGONGATE** (nothing hits the dragon) | `RingFixtureTest`, `FightDragonSourceTest` *theDragonIsRemovedAtEveryEnding* | |

### DA9 — a melee hit on a PART reaches the dragon (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| He holds `emberblade` (MG2R2's staging: a melee weapon, so `Player.attack` raises a damage event). (1) **`DRAGON strike t=<t> part=head part_id=<uuid, not the dragon's> by=BaronVonYeetus`**, and the reply `Struck the dragon's head.` (the part named from its box; READ, head 1 x 1). (2) Then **`FIGHTHIT … target=dragon amount=<A> before_defense=<A> health=<20000 − A>/20000.000 dealer=<his uuid> element=fire`**, **A > 0**, its first. **That line IS the witness that the swing was recorded against the dragon**: before this branch `onPrePlayerAttack` returned on a part and the store took nothing. (3) The same for **`part=wing`** (READ, wing 4 x 2): a second FIGHTHIT, health = 20000 − A − B. (4) The probe: `health=<the last FIGHTHIT's>`. **Any later FIGHTHIT in the row with element `fire` is emberblade's own burn (Scorch), not a swing; it is reported, not predicted.** A **reply `No part 'head'; the parts are [...]`** would mean the box naming failed, and the row says so | `LiveTargetsTest` *aPartOfALiveDragon…*, *everyHitSiteResolvesAPart…*; `DragonPartsTest` | |

---

## BEN'S ROWS — HIS WORDS, VERBATIM; "not sure" IS NOT STATED; ONE THING PER QUESTION; NO PASS OR FAIL

### DB1 — the dragon, watched and hit: questions 1 to 4 (SURVIVAL)

| prediction | READING |
|---|---|
| Three minutes with the dragon free (CIRCLING from its spawn), then ASK 1 (`yesno`) "The dragon: did it fly and land as you expect, with no fireball?", ASK 2 (`yesno`) "Could you hit it with your own weapon while it was in the air?", ASK 3 (`yesno`) "Could you hit it with your own weapon while it had landed?", ASK 4 (`yesno`) "The boss bar at the top of the screen: is it right?". **Auto, reported:** every FIGHTHIT on the dragon (tick, amount, health after); every `DRAGONGATE`; every `FIGHT fireball-cancelled` (and `fireballs_in_world=0` if probed); any DRAGONCROSS and any Mage that lands. **INFERRED, to be read here:** a perched dragon takes 0 from an arrow (READ: the sitting phases' `onHurt` zeroes an arrow), which our weapons do not fire | |

### DB2 — THE FIGHT, NO SAFETY: questions 5 and 6 (SURVIVAL; he may die)

| prediction | READING |
|---|---|
| Up to ten minutes (`await_fight 12000`) of the dragon and the Mages with his own weapon. It ends ONE of these ways, and the log says which: **the win** (`FIGHT win …`, then the return 200 ticks later); **his death** (`FIGHTWORLD died-inside`, then `FIGHT nobody inside … outcome=<o>`: one player, so one death ends it); **a failure** (FAILED_CLOSED or FAILED_POWERED, then his death by it); or **the ceiling** (`AWAIT fight ceiling ticks=12000 still running`, then `rpg fight stop`). **Reported:** each crossing (`DRAGONCROSS`); each Mage's landing, his health at each phase change (`FIGHT mage-health`) and at every landed hit (`FIGHTHIT target=mage`), and his time to kill (`FIGHT die … ticks_in_slow_close=<n>`); every FIGHTHIT on the dragon; his deaths; every STRIKE and HURT; every `DRAGONGATE`; every fireball cancelled; the ending. Then ASK 5 (`yesno`) "For one player, is the dragon's 20,000 health right?" and ASK 6 (`words`) "What most needs to change before waves are built?" | |

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
