# GATE — STEP F9c: AN END-TYPE FIGHT WORLD (AND THE ONE BOOT FOR F9b's GATE TOO)

**Seat approval 2026-11-19: code and gate diffed at e772f8b5, readings at 961bcea2. Merge on Ben's words.** **Status: THE SECOND BOOT IS READ, 2026-11-19, at `e772f8b5`** (R0 passed; EB3R2, EB4R2 and EB5R2 read with Ben's five answers; he won the fight; *THE SECOND BOOT, READ*, at the end). **The first boot: READ, 2026-11-19, at `e15cf9ee`** (the seat's boot approval of 2026-11-19; Ben's "ready"). R0 passed. EA1 read the INERT branch, as predicted; EB1 to EB4 read with Ben's five answers; **EB5 ABORTED** (he disconnected 32 seconds in), so questions 6 and 7 were not asked. The checklist, the after-run and the findings are in *THE BOOT OF 2026-11-19* at the end. Every prediction below was written **before** any boot of `feat/fight-end`, and this file is
committed before the boot.
- Readings go **beside** a prediction, never over it.
- A prediction is not edited once its row has been read; a correction is a dated note.
- **NO BOOT until the seat has diffed both branches' code (`feat/fight-rules`, `feat/fight-end`) and both gates
  (`GATE-fight-rules.md`, this file) from origin, and Ben says "ready" in his own words.** Then only CC boots, on this
  branch's head, following `.claude/rules/booting.md`.

**THIS FILE CARRIES THE ONE R0 AND THE ONE RUN SHEET** for itself and `GATE-fight-rules.md` (the seat's split ruling,
2026-11-19), and Ben's rows for both steps (the brief's questions 1 to 5 and 9).

**The step** (the seat's F9b brief, section 3; plan §6.9 option a; Ben: "yes let's try it"):
- The fight world's environment becomes content (`arena.environment`), `end` by default.
- For `end`, BEFORE the world is made, its folder gets our marker and a Paper per-world file switching
  `entities.spawning.scan-for-legacy-ender-dragon` off.
- Four ticks after it is made, a self-check from READ facts: inert, or that world is ended and a NORMAL one made
  instead.
- Our crystals set no fire.
- Not in this step: anything else (the respawn rules are F9d and F9e).

**GAME MODE: SURVIVAL for every row.** No row uses `save_blocks`. Damage is predicted against the HURT line.

**Witness:** EA1 is read by CC from the server's log. Ben's rows (EB) are his words, verbatim; "not sure" is NOT
STATED; no PASS or FAIL on a Ben row.

**THE SEAT'S RULING ON THE PROBE (2026-11-19), recorded:** `World.getEnderDragonBattle()` is read on the fight's own
world, in the self-check only (`FightWorlds.endSelfCheck`), to confirm vanilla's fight is inert. It is never used to
find, drive or identify our fight (C1 stands). `FightEndSourceTest` asserts one call in all of `paper`'s main code,
there.

---

## WHICH MERGED ROWS THIS BRANCH MOVES, AND WHICH THE BOOT RE-RUNS

**Every arena fight is now made in an END world, `nodragon` included.** For every row that enters one, of
`GATE-fight-arena.md` (AE1R2-AE7R2, BA1R2-BA3R2), `GATE-fight-dragon.md` (DA1-DA9, DB1, DB2) and
`GATE-fight-rules.md` (RA1-RA5):
- **Before** the world's `FIGHTWORLD created`:
  `FIGHTWORLD end-config path=<abs>\run\world\dimensions\rpgfight\<n> file=paper-world.yml scan-for-legacy-ender-dragon=false`.
- **`FIGHTWORLD created name=<n> … ms=<t> environment=THE_END`**: the created line now ends with its environment.
- **The `enter` reply** is now `Preparing an End world for the fight; you enter once vanilla's dragon fight is checked inert.`
- **Four ticks later** `FIGHTWORLD end-check name=<n> inert=true …` (EA1 has its exact form). Only then the arena's
  `arena built`, `FIGHT start`, the dragon, `FIGHTWORLD entered` and the chat line `Entered the fight in <n>.`.
- **The fight's lines** (rings, crystals, the Mage, the dragon, strikes, deaths and endings) are unchanged. An END
  world changes the void, the light, the sky and the music, and none of those is in a predicted line.

**The void:** AE6R2 stages him at y −130. That is below both void heights: an END world's is −64 (`min_y` 0, and
Paper's `void-damage-min-build-height-offset` −64.0), a NORMAL one's −128. **No row assumed the overworld's; none is
changed.**

**Re-run at this boot:** `GATE-fight-rules.md`'s RA1-RA5 (`fight-rules`, in END worlds) and this file's rows. **Not
re-run:** the arena's and the dragon's merged rows. They are re-predicted here, as the list above, under their old
names: **no new suffix is spent on a change that touches only the birth's lines**, and none of their own predicted
lines moves. `GATE-fight-rules.md` carries a dated note pointing here for its rows' birth lines.

**Site rows** (`GATE-fight-site.md`): a site fight runs in an ordinary world and makes no fight world. **Not moved.**

---

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

The booter's checklist is `.claude/rules/booting.md`; this list is its instance for this boot.

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup:** `run/plugins/Rpg` and `run/world` copied to
   `../rpg-run-backups/2026-11-<dd>-pre-fight-end` (`-2` if it exists): file and directory counts, `diff -rq lines: 0`
   each, the `ls` of `world/dimensions` and the overworld's file count.
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND IS SENT:** "booting now", and Ben told **NOT to log in until told R0
   passed.**
6. `./scripts/dev-server.sh --refresh-content`. **The REQUIRED deployed lines:**
   - `content/fights/dragon.yml` reads `  environment: end`, `  landing_lightning: true`, `  bite: 80.0`;
   - `content/mobs/ender_dragon.yml` reads `max_health: 20000`.
7. R0a to R0e read on **this boot's** log (its `[Rpg] Build:` names this branch's head).
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* `rpgfight` empty; the overworld's file count against step 2's; `All dimensions are saved`;
   no `java` process left.

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED. IF YOU ARE TOLD NOTHING, DO NOT LOG IN.**
1. `/gamemode survival` — **alone, the first line after login.**
2. Stand on flat open ground with room all round, and hold the weapon you want to fight with.
3. `/rpg selftest fight-rules` — **ONCE.** About two minutes of rows; you are taken into the fight's own world and
   back after each, and nothing kills you. **Some rows put you inside the dragon on purpose: it bites, strikes and
   breathes on you, at full health, and you float down slowly.**
4. `/rpg selftest fight-end` — **ONCE.** One row of about half a minute, then **seven** questions over five rows, and
   up to **ten minutes** of fighting at the end.
   - **The last row (EB5, the fight) can kill you: you reappear at once where you entered, with everything you
     carried.**
   - Each question says whether it wants yes or no, or your own words.
   - **`/rpg mana refill` is yours to use or not; the log records it, so a number judged with it is read with it.**
5. `/stop`

Nothing else this boot.

---

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `feat/fight-end`'s head as `git ls-remote origin refs/heads/feat/fight-end` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **AS PREDICTED (2026-11-19, 12:49:17).** `[Rpg] Build: e15cf9ee`; `git ls-remote --heads origin feat/fight-end` printed `e15cf9ee6bb4d7c9daa4dd4f9f854b86b420945a` (and `feat/fight-rules` `1c1f3e06…`, what the seat diffed). Not `-dirty`, not `unknown` |

### R0b — the jar carries both steps

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `dragon.yml` lines `  environment: end`, `  landing_lightning: true`, `  bite: 80.0`; `FightWorlds.class` carrying `scan-for-legacy-ender-dragon: false`; `FightInstance.class` carrying `DRAGONATTACK`; `RpgListeners.class` carrying `onFightDragonBreath`; the entries `io/github/butterflysmp/rpg/core/fight/EndFightCheck.class` and `…/DragonAttacks.class`; `GATE-fight-end.yml` carrying `EB5:`; `GATE-fight-rules.yml` carrying `RA5:`. ABSENT: `dragon.yml` line `  environment: normal` (the YAML control), `NoSuchTextControl` in `FightInstance.class` (the class control) | the PowerShell below, from the repo root | **AS PREDICTED.** `PRESENT dragon.yml '  environment: end'`, `'  landing_lightning: true'`, `'  bite: 80.0'`; `ABSENT  dragon.yml '  environment: normal'`; `PRESENT …/FightWorlds.class 'scan-for-legacy-ender-dragon: false'`; `PRESENT …/FightInstance.class 'DRAGONATTACK'`; `ABSENT  …/FightInstance.class 'NoSuchTextControl'`; `PRESENT …/RpgListeners.class 'onFightDragonBreath'`; `PRESENT entry EndFightCheck.class`; `PRESENT entry DragonAttacks.class`; `PRESENT GATE-fight-end.yml EB5:`; `PRESENT GATE-fight-rules.yml RA5:` |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Line($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
$d = Bytes 'content/fights/dragon.yml'
Line $d '  environment: end' 'dragon.yml'; Line $d '  landing_lightning: true' 'dragon.yml'; Line $d '  bite: 80.0' 'dragon.yml'; Line $d '  environment: normal' 'dragon.yml'
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
Has 'io/github/butterflysmp/rpg/paper/fight/FightWorlds.class' 'scan-for-legacy-ender-dragon: false'
Has 'io/github/butterflysmp/rpg/paper/fight/FightInstance.class' 'DRAGONATTACK'
Has 'io/github/butterflysmp/rpg/paper/fight/FightInstance.class' 'NoSuchTextControl'
Has 'io/github/butterflysmp/rpg/paper/listener/RpgListeners.class' 'onFightDragonBreath'
foreach ($c in 'EndFightCheck', 'DragonAttacks') { if ($zip.GetEntry("io/github/butterflysmp/rpg/core/fight/$c.class") -ne $null) { "PRESENT entry $c.class" } else { "ABSENT  entry $c.class" } }
$e = Bytes 'selftest/GATE-fight-end.yml'; if ($e -ne $null -and $e -match "(?m)^EB5:\s*$") { "PRESENT GATE-fight-end.yml EB5:" } else { "ABSENT  GATE-fight-end.yml EB5:" }
$r = Bytes 'selftest/GATE-fight-rules.yml'; if ($r -ne $null -and $r -match "(?m)^RA5:\s*$") { "PRESENT GATE-fight-rules.yml RA5:" } else { "ABSENT  GATE-fight-rules.yml RA5:" }
$zip.Dispose()
```

**A jar from master reads ABSENT on every PRESENT line**, and the two controls ABSENT on either jar.

### R0c — the Loaded lines, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`** (the F9 boot's, unchanged), AND exactly one **`[Rpg] Loaded fight 'dragon': border 48.0, mage ring 12.0, v_fast 3.80, v_slow 0.60, e_kill 1.00, 10 crystals every 240 ticks, a strike every 40 ticks from 20.0; ring dust (170,0,255) size 1.60 every 1.00 blocks at [0.2, 1.7, 3.2, 4.7, 6.2, 7.7], row shift 0.25, turn 1.00/s alternating, every 4 ticks; mage 'dark_mage' refused item.shield.block, landing lightning on; crystals at 40.0, 12.0 up, lit block.beacon.power_select; participants within 128.0; arena at (0, 0) floor y 64, disc 56 x 3, 10 pillars 3 wide on 40.0 to +12, time 18000, entry (0.0, 0.0), environment end; dragon 'ender_dragon' Mages below [0.8, 0.6, 0.4, 0.2], win return 200 ticks, bar purple, bite 80.0, wing 40.0, breath 30.0/s`** (`FightLoaderTest` asserts it verbatim). **No `Refusing`, `Skipping`, `SEVERE` or `Exception`; no `[Rpg]` WARN beyond `volley_stone`'s known pair**; no `No fights loaded` | `Select-String run\logs\latest.log -Pattern 'Loaded\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded'` (up to the first `joined the game`) | **AS PREDICTED.** Both Loaded lines verbatim at 12:49:17, exactly one fight line (ending `environment end; dragon 'ender_dragon' Mages below [0.8, 0.6, 0.4, 0.2], win return 200 ticks, bar purple, bite 80.0, wing 40.0, breath 30.0/s`). `[Rpg]` WARN: `volley_stone`'s pair only. No `Refusing`, `Skipping`, `SEVERE`, `Exception` or `No fights loaded` before the join (12:49:51). Paper's own "release(s) behind" banner is not `[Rpg]` |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 221 scenarios across 26 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive, fight-site, fight-arena, fight-dragon, fight-rules, fight-end]`**, no refusal after it. **The control:** the F9 boot's `210 scenarios across 24 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | **AS PREDICTED.** Exactly one `[Rpg] Selftest: dev server, 221 scenarios across 26 gates [melee-cell, … fight-dragon, fight-rules, fight-end]`; `210 scenarios across 24 gates` ABSENT |

**RECOUNTED** (2026-11-19, this branch's tree; `SelfTestScenariosTest` asserts 221): the gates in `index.yml` **26**;
`^[A-Za-z0-9_-]+:` over them **247**; `^setup:` **26**; so **221**. **Control:** 234 (F9) + fight-rules' 6 +
fight-end's 7 = 247.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, and no `deleting` and no `orphan` line | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | **AS PREDICTED.** Exactly one `FIGHTWORLD sweep path=C:\Users\Neb91\IdeaProjects\minecraft-rpg-scaffold\run\world\dimensions\rpgfight folders=0`; no `deleting`, no `orphan` during the enable |

---

## THE AUTO ROW — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST fight-end|\[Rpg\] (FIGHT|DRAGON|BOSSBAR|STRIKE|CRYSTAL|ARENAPROBE)|Scanning for legacy'`
inside the row's `START … DONE` bracket. **A log-line row passes on the VERBATIM line.**

### EA1 — an END world, its self-check, the arena in it, one dragon, and no crystal fire (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **IF THE CHECK READS INERT** (predicted): (1) **`FIGHTWORLD end-config path=<abs>\run\world\dimensions\rpgfight\<n> file=paper-world.yml scan-for-legacy-ender-dragon=false`**, then **`FIGHTWORLD created name=<n> key=rpgfight:<n> folder=<the same path> ms=<t> environment=THE_END`** (t READ). (2) Four ticks later: **`FIGHTWORLD end-check name=<n> inert=true fight=present bar_visible=false fight_dragon=none portal=none bedrock_at_centre=0 dragons=0`**. READ: the fight's tick hides its bar exactly when `dragonKilled`; Paper's branch sets that with no scan. **UNVERIFIED until this line: that Paper reads our file for our world.** (3) Then `FIGHTWORLD arena built name=<n> blocks=30615 end_stone=29535 obsidian=1080 ms=<t>`, `FIGHT start … site=arena`, `FIGHT dragon spawned t=0 … phase=CIRCLING …`, `FIGHTWORLD entered player=BaronVonYeetus world=<n> return=world <x y z>`, `BOSSBAR shown`, `DRAGON phase-set … phase=HOVER now=HOVER`. (4) **NO `Scanning for legacy world dragon fight...` line anywhere in the row** (vanilla's line, READ: `scanState` logs it first). (5) **`ARENAPROBE world=<n> environment=THE_END centre_top=END_STONE bedrock_in_column=0 fire=0 (y 65..79) crystals=0 dragons=1`**. (6) `land 0 0` with `FIGHT lightning … effect=true`; `FIGHTPHASE t=<S> FAST_CLOSE->SLOW_CLOSE` about 190 ticks on; **`CRYSTAL t=<S+240> lit index=1`**; then **one `FIGHT crystal-fire cancelled crystal=<uuid> at=40 77 0 world=<n>`** (READ: `EndCrystal.tick` sets fire on its block in a level with a fight object, through the ignite event; the crystal stands at (40.5, 77, 0.5), so its block is (40, 77, 0)). (7) **`ARENAPROBE … environment=THE_END centre_top=END_STONE bedrock_in_column=0 fire=0 (y 65..79) crystals=1 dragons=1`**. (8) The end, as every arena row, `deleted … entries=<e>` (e READ) | `EndFightCheckTest`, `FightEndSourceTest` | **THIS BRANCH WAS READ: AS PREDICTED.** (1) **`FIGHTWORLD end-config path=C:\Users\Neb91\IdeaProjects\minecraft-rpg-scaffold\run\world\dimensions\rpgfight\fight_606d5dd2 file=paper-world.yml scan-for-legacy-ender-dragon=false`**, **`FIGHTWORLD created name=rpgfight_fight_606d5dd2 key=rpgfight:fight_606d5dd2 folder=<the same path> ms=16 environment=THE_END`**. (2) **`FIGHTWORLD end-check name=rpgfight_fight_606d5dd2 inert=true fight=present bar_visible=false fight_dragon=none portal=none bedrock_at_centre=0 dragons=0`** the same second: **Paper read our file for our world (no longer UNVERIFIED).** (3) `arena built … blocks=30615 end_stone=29535 obsidian=1080 ms=93`, `FIGHT start … site=arena`, `FIGHT dragon spawned t=0 … phase=CIRCLING …`, `FIGHTWORLD entered … return=world -110.31 63.00 15.97`, `BOSSBAR shown t=1`, `DRAGON phase-set t=16 phase=HOVER now=HOVER`. (4) **`Scanning for legacy world dragon fight...`: 0 lines in the whole log.** (5) **`ARENAPROBE world=rpgfight_fight_606d5dd2 environment=THE_END centre_top=END_STONE bedrock_in_column=0 fire=0 (y 65..79) crystals=0 dragons=1`**. (6) `FIGHT land t=18 …`, `FIGHT lightning t=18 … effect=true`; `FIGHTPHASE t=208 FAST_CLOSE->SLOW_CLOSE`; **`CRYSTAL t=448 lit index=1`**; **`FIGHT crystal-fire cancelled crystal=8166863a-… at=40 77 0 world=rpgfight_fight_606d5dd2`**, once. (7) **`ARENAPROBE … environment=THE_END centre_top=END_STONE bedrock_in_column=0 fire=0 (y 65..79) crystals=1 dragons=1`**. (8) The end, `deleted … entries=17`. Off the sheet: at 12:52:13 he typed `/rpg manaregen 20` (inside this row) |
| **IF THE CHECK READS NOT INERT** (a branch, predicted so the boot still reads everything else): (2) **`FIGHTWORLD end-check name=<n> inert=FALSE …` -- vanilla's fight is NOT inert: this world is ended and a NORMAL world made instead, so the fight never runs beside vanilla's** at WARN, naming which reading failed; `FIGHTWORLD unloaded name=<n>`, `deleting`, `deleted`; then **`FIGHTWORLD created name=<m> … environment=NORMAL`** with no `end-config` line before it; the rest in `<m>`, and the probes read `environment=NORMAL`. **Then option a has failed for this build, and the row says so** | the same | **NOT READ: the check read inert.** No `inert=FALSE`, no fallback and no NORMAL world in the whole log |

**RA1-RA5 at this boot** (`GATE-fight-rules.md`) run in END worlds made this way. Their birth lines are this file's
list above; their own predictions are unchanged.

---

## BEN'S ROWS — HIS WORDS, VERBATIM; "not sure" IS NOT STATED; ONE THING PER QUESTION; NO PASS OR FAIL

### EB1 — the End world, looked at: questions 1 and 2 (SURVIVAL)

| prediction | READING |
|---|---|
| A minute in the arena with the dragon held still. Then ASK 1 (`yesno`) "The End world: is the sky what you wanted?" and ASK 2 (`words`) "Is anything about the End world wrong (the music, the light, fire, the edge)? Say what, or say none." **INFERRED, his eyes CONFIRM or not:** the End's sky model (`skybox: end`, READ from `the_end.json`), its colours, its flat light and its music. **Auto, reported:** the row's birth lines (an END world, inert), any crystal-fire line (none predicted: no Mage lands) | **HIS WORDS:** ASK 1 **"yes"**; ASK 2 **"its good"**. Auto: `FIGHTWORLD end-check name=rpgfight_fight_dbeaa591 inert=true …`, `DRAGON phase-set t=16 phase=HOVER now=HOVER`; no crystal-fire line (no Mage landed). Off the sheet, inside this row: **`/rpg give cursed_emerald` at 12:53:22** and **`/rpg mana refill` at 12:53:28** (after RA3's death in the overworld; see the findings) |

### EB2 — the landing's lightning: question 3 (SURVIVAL)

| prediction | READING |
|---|---|
| A Mage lands 10 east of the centre: `FIGHT land t=<L> dx=10.000 dz=0.000 result=STARTED`, `FIGHT mage spawned … at=10.00 0.00 0.00`, **`FIGHT lightning t=<L> at=10.00 0.00 0.00 effect=true`**, and no HURT line from it. Then ASK 3 (`yesno`) "The lightning at the Mage's landing: is it right?" | **HIS WORDS:** ASK 3 **"yes"**. Auto, as predicted: `FIGHT land t=116 dx=10.000 dz=0.000 result=STARTED`, `FIGHT mage spawned t=116 … at=10.00 0.00 0.00`, **`FIGHT lightning t=116 at=10.00 0.00 0.00 effect=true`**, and no HURT line from it |

### EB3 — the bite and the wing: question 4 (SURVIVAL)

| prediction | READING |
|---|---|
| As RA3 then RA4: `DRAGON touch … part=head`, `DRAGONATTACK … attack=bite … amount=80.000 …`, `HURT before=80.000 after=<80 x 100/(100+D)> defense=<D> … attack=bite`; healed; `DRAGON touch … part=wing`, `DRAGONATTACK … attack=wing … amount=40.000 …`, `HURT before=40.000 … attack=wing`. Then ASK 4 (`yesno`) "The dragon's bite (80) and wing (40): do they feel right?" | **HIS WORDS:** ASK 4 **"yes"**. **Auto, NOT as predicted: neither attack landed.** `DRAGON touch t=76 part=head at=-0.85 18.49 -7.24 phase=HOVER` and `DRAGON touch t=140 part=wing at=4.19 18.99 1.17 phase=HOVER`, and **no `DRAGONATTACK` and no HURT** in the row (RA3, the same staging, had eight bites). So his "yes" was given with no bite and no wing taken; in EB4 he wrote "Im not even sure I took damage from the bite and wing" |

### EB4 — the breath: question 5 (SURVIVAL)

| prediction | READING |
|---|---|
| As RA5, for about three of the cloud's hits, 20 ticks apart: `DRAGONATTACK … attack=breath … amount=30.000 …`, `HURT before=30.000 … attack=breath`. Then ASK 5 (`yesno`) "The breath, 30 a second: does it feel right?" | **HIS WORDS:** ASK 5 **"yes but I didn't take damage. Im not even sure I took damage from the bite and wing"**. **Auto, NOT as predicted: no breath hit.** `DRAGON phase-set t=76 phase=BREATH_ATTACK now=BREATH_ATTACK`, **`DRAGON to-breath t=91 cloud=838327b1-… at=-1.24 0.00 -9.71 radius=5.00 interval=20`**, and **no `DRAGONATTACK attack=breath` and no HURT** in the 65 ticks on it |

### EB5 — THE FIGHT, NO SAFETY: questions 6 and 7 (SURVIVAL; he may die)

| prediction | READING |
|---|---|
| Up to ten minutes (`await_fight 12000`) in the END world. It ends ONE of these ways, and the log says which: the win (`FIGHT win …`); his death (`FIGHTWORLD died-inside`, then `FIGHT nobody inside …`); a failure; or the ceiling (`AWAIT fight ceiling ticks=12000 still running`, then `rpg fight stop`). **Reported:** every `DRAGONCROSS`; every Mage's landing (with its `FIGHT lightning`), his health at each phase change, and his time to kill; **every `DRAGONGATE` with its `why=`** (predicted: from each crossing until that Mage is dead, `why=mage-owed` before he lands and `why=mage-alive` while he lives); **if he reaches 20%, four Mages**; every `DRAGONATTACK` and every HURT with its `attack=`; every `DRAGONHEAL` (the dragon's health never rising); every `DRAGONMAGE`; every `FIGHT fireballs` line (predicted none: the fireball is cancelled); every `FIGHT crystal-fire cancelled`; his death; the ending. Then ASK 6 (`yesno`) "For one player, is the dragon's 20,000 health right, now that every phase has its Mage?" and ASK 7 (`words`) "What most needs to change before waves are built?" | **ABORTED: NOT READ. ASKS 6 AND 7 WERE NOT ASKED.** `FIGHTWORLD end-check name=rpgfight_fight_134a1d0f inert=true …`; `DEATH_RULES keep_inventory=false->true immediate_respawn=false->true`. In the 650 ticks he was in: **no FIGHTHIT on the dragon, no DRAGONCROSS, no Mage, no DRAGONGATE, no DRAGONATTACK, no HURT, no DRAGONHEAL, no DRAGONMAGE, no `FIGHT fireballs` line, no crystal-fire line**; one **`FIGHT fireball-cancelled t=105 dragon=35fca59f-… phase=STRAFING`**; no death; no `/rpg mana refill` in the row. At 12:55:08 he wrote in chat **"my cursed emerald isn't working"**. **12:55:22 `BaronVonYeetus lost connection: Disconnected`** (t=650), then `RULES restored`, `FIGHT stop t=650`, `BOSSBAR hidden … why=fight-ended`, `FIGHT dragon removed reason=selftest-run-end was_valid=true`, `FIGHT ended reason=selftest-run-end`, `FIGHTWORLD returned … why=end`, `FIGHTWORLD unload-retry … attempt=1`, **`SELFTEST RUN ABORTED the player left`**, `RESTORED …`; `deleted … fight_134a1d0f entries=17` |

---

## WHAT THE END ENVIRONMENT CHANGES, EACH HANDLED OR RECORDED (the brief's 3 d)

- **Crystal fire:** cancelled in a fight world (`onFightCrystalFire`), logged once per crystal. EA1 reads it.
- **The void:** −64 in an END world against −128 (above). No row changed.
- **End music:** left (the brief).
- **Natural spawns:** stay off. The fight world's spawn game rules are set as before.
- **The gear-score curve for unstamped mobs:**
  - READ: `MobOrigin.dimensionOf` maps `THE_END` to the End curve, measured from (0, 0).
  - The dragon and the Mage are stamped GS 100, so they don't move.
  - **Recorded for waves: an unstamped mob in a fight world would read the End curve.** Nothing to do now.
- **The dragon's flight, podium, death, egg, gateway and XP:**
  - With the fight inert nothing adopts our dragon. READ: its `aiStep` attaches the level's fight only when the
    fight's `dragonUUID` equals its own, and an inert fight's is null.
  - So its death runs no `setDragonKilled`: no egg, no portal, no gateway. READ: `tickDeath` calls it only with a fight
    attached.
  - Its XP is the F9 boot's 500, suppressed as before.
  - Its flight and podium are ours, as in a normal world (INFERRED: nothing in its movement reads the environment).
- **Time of day:** not applied in an END world (its type fixes the time); kept for normal.
- **Weather:** the End biome has none; the fight world's weather is fixed as before.

## WHAT THIS GATE DOES NOT READ

- **What Ben's client draws** (the sky, the light, the lightning): his rows.
- **A second player in the same End world:** one player.
- **The fallback in practice**, if the check reads inert: the not-inert branch is pinned by `FightEndSourceTest` and
  `EndFightCheckTest` only.

---

## THE BOOT OF 2026-11-19 — THE CHECKLIST, THE AFTER-RUN, WHAT THE RUN SHEET DID NOT SAY, AND THE FINDINGS

Written after Ben's `/stop`. **No prediction above was edited.**

### The booter's checklist (`.claude/rules/booting.md`), ticked

1. **The seat's word and Ben's:** the seat's `BOOT APPROVED for the stack PR #214 + PR #215`, 2026-11-19, then Ben's
   "ready" (his own message).
2. **The heads:** `git ls-remote` printed `feat/fight-end` `e15cf9ee6bb4d7c9daa4dd4f9f854b86b420945a` and
   `feat/fight-rules` `1c1f3e067b7f039bd0f20bba4bcabc7f3ba59ef0`, what the seat diffed; the tree clean.
3. **The carried state, in order:**
   1. `Get-Process java` printed nothing.
   2. Backup `../rpg-run-backups/2026-11-19-pre-fight-end`: Rpg `files=133 dirs=24`, `diff -rq lines: 0`; world
      `files=193 dirs=32`, `diff -rq lines: 0`. `world/dimensions`: `minecraft`, `rpgfight`. The overworld: **106**
      files.
   3. `12:  dev-pack-on-join: true`.
   4. `find run/world/dimensions/rpgfight -mindepth 1` printed nothing.
4. **"Booting now", in its own message**, with "Ben: do NOT log in until I tell you R0 passed", the turn ended before
   the boot command.
5. `./scripts/dev-server.sh --refresh-content` (`==> Building e15cf9ee`); `Done (7.843s)` at 12:49:18. REQUIRED
   deployed lines: `70:  landing_lightning: true`, `88:  bite: 80.0`, `129:  environment: end` (`dragon.yml`);
   `11:max_health: 20000` (`ender_dragon.yml`).
6. **R0a to R0e read on this boot's log**, `[Rpg] Build: e15cf9ee`: all as predicted.
7. **"R0 passed", in its own message**, sent with the R0 lines read at 12:49:35.
8. **His login against it:** R0's lines are all at 12:49:17; his first `joined the game` is **12:49:51**, after.
9. **After his `/stop` (12:56:07):**
   - `rpgfight` empty;
   - **the overworld 106 files, against the backup's 106**;
   - `ThreadedAnvilChunkStorage: All dimensions are saved` and `All RegionFile I/O tasks to complete` at 12:56:07;
   - `Get-Process java` printed nothing.

### EVERY FIGHT WORLD OF THE BOOT: twelve, every one END and checked inert

- **Every world had all three lines:** `FIGHTWORLD end-config` 12, `FIGHTWORLD created … environment=THE_END` 12,
  `FIGHTWORLD end-check … inert=true fight=present bar_visible=false fight_dragon=none portal=none bedrock_at_centre=0
  dragons=0` 12. No `inert=FALSE`; no NORMAL world.
- **`Scanning for legacy world dragon fight...` appears 0 times in the whole log.**
- **Deleted, each `entries=17`**, under `…\run\world\dimensions\rpgfight\`:
  - fight-rules: `fight_f0b5232f` (RA1), `fight_2fc9132f` (RA2), `fight_c24277f1` (RA3), `fight_c6b1644c` (RA4),
    `fight_01e1898e` (RA5);
  - fight-end: `fight_606d5dd2` (EA1), `fight_dbeaa591` (EB1), `fight_2c50ef37` (EB2), `fight_d46a4812` (EB3),
    `fight_480756a1` (EB4), `fight_134a1d0f` (EB5);
  - `fight_b59d6697`: his own `/rpg fight enter` after the abort, ended `at=disable` by the `/stop`.
- **At each fight's END, the count of dragons and of centre-column bedrock is in NO line.**
  - Only EA1's `ARENAPROBE`, mid-fight, carries them.
  - **Instrument to add:** the arena probe's reading at every fight's end.

### The log's warnings

`[Rpg]` lines matching Refusing, Skipping, SEVERE, WARN or Exception: **`volley_stone`'s pair only**, at the enable. No
end-check WARN (the fallback never fired).

### What the run sheet did not say, recorded as read (no PASS or FAIL)

- **Off the sheet during the selftests:**
  - `/rpg manaregen 20` at 12:52:13 (EA1);
  - **`/rpg give cursed_emerald` at 12:53:22** and `/rpg mana refill` at 12:53:28 (EB1).
- **EB5 aborted:** `BaronVonYeetus lost connection: Disconnected` at 12:55:22, 650 ticks into the fight.
- **After the abort:**
  - he rejoined at 12:55:27, logged in at the overworld's `0.5, 65.0, 0.5`; no stale-login line, his return having been
    cleared at `why=end`;
  - typed `/gamemode creative`, `/gamemode survival`, then **`/rpg fight enter`** (12:55:56): another END world,
    `end-check … inert=true`, a `fireball-cancelled` at t=218;
  - then **`/stop` from inside it**. `FIGHTWORLD returned … why=end-disable`, `unloaded … at=disable`, deleted, `FIGHT
    ended reason=plugin-disable`.
- **His chat in EB5:** "my cursed emerald isn't working". No line in the log says why: no cast, no refusal, and no
  `FIGHTHIT` from him in that fight.

### FINDINGS

1. **OPTION A WORKS ON THIS BUILD.** Every one of twelve END worlds read inert. Paper read our per-world file for our
   world (UNVERIFIED until now). Vanilla's `Scanning…` line never appeared. EA1's probes read the centre end stone,
   no bedrock, no fire, one dragon. **Ben: the sky "yes"; anything wrong: "its good".**
2. **THE DRAGON'S ATTACKS DID NOT WORK AS BUILT** (`GATE-fight-rules.md`, its findings):
   - the bite landed twice a tick for four ticks in RA3, and killed him;
   - in EB3 the same staging landed nothing;
   - the wing landed nothing in RA4 or EB3;
   - the breath landed nothing in RA5 or EB4.

   Ben, EB4: "yes but I didn't take damage. Im not even sure I took damage from the bite and wing". **His ASK 4 "yes"
   and ASK 5 "yes" were given about hits he did not take**, and are read with that.
3. **RA3's death was in the overworld with keep-inventory off** (`GATE-fight-rules.md`, finding 2). His inventory was
   likely dropped there. He gave himself a new cursed emerald during EB1, and wrote in EB5 that it "isn't working".
   Whether these are connected is not in the log.
4. **The fight itself (EB5) was not read:** no crossing, no Mage, no hit. Questions 6 and 7 are owed.

---

## THE SECOND RUN, 2026-11-19, 13:09 to 13:14 — BEN'S OWN BOOT, NOT CC'S

**Recorded as read; no prediction above is edited and the first boot's readings stand beside them.**

**HOW IT WAS RUN, AND WHAT THAT LEAVES OUT.**
- Ben booted the server himself (`latest.log` from 13:09:01). The rule is that only CC boots.
- **None of the carried state was taken:** no backup, no "booting now", no "R0 passed" word.
- He ran `/rpg selftest fight-end` once (`fight-rules` was not re-run), then `/stop`.

**R0, read on that log afterwards:**
- `[Rpg] Build: 61ce1271`, `feat/fight-end`'s head (the readings commit; its code is byte-identical to the booted
  `e15cf9ee`), not `-dirty`.
- The fight's Loaded line as R0c.
- `221 scenarios across 26 gates`.
- `FIGHTWORLD sweep … folders=0`.
- Only `volley_stone`'s `[Rpg]` WARN pair.
- The jar scanned afterwards (built 13:09:00): `environment: end` PRESENT, `environment: normal` ABSENT,
  `DRAGONATTACK` PRESENT, `NoSuchTextControl` ABSENT.

**Every fight world:** six, every one `environment=THE_END` and `end-check … inert=true`. `Scanning for legacy world
dragon fight...` appears 0 times. Each deleted, `entries=17`: `fight_145423d9`, `fight_9ea2af44`, `fight_f893923b`,
`fight_27e8224d`, `fight_3451074d`, `fight_ece4e8fc`.

**After his `/stop` (13:14:37):** `All dimensions are saved` at 13:14:38; no `java` process; `rpgfight` empty; the
overworld 106 files (the 12:49 backup's figure).

### HIS WORDS IN THE SECOND RUN, VERBATIM

1. "The End world: is the sky what you wanted?" — **"yes"**
2. "Is anything about the End world wrong (the music, the light, fire, the edge)? Say what, or say none." —
   **"good"**
3. "The lightning at the Mage's landing: is it right?" — **"yes"**
4. "The dragon's bite (80) and wing (40): do they feel right?" — **"pass"**
5. "The breath, 30 a second: does it feel right?" — **"remove the knock back from it"**
6. "For one player, is the dragon's 20,000 health right, now that every phase has its Mage?" — **"yes"**
7. "What most needs to change before waves are built?" — **"nothing right now"**

### THE ROWS, AS READ

- **EA1:** the inert branch again, as the first boot.
- **EB3, THE BITE KILLED HIM INSIDE THE FIGHT, BEFORE THE WING WAS STAGED.**
  - `DRAGON touch t=76 part=head at=-2.23 18.50 -6.27 phase=HOVER`.
  - Then **two `DRAGONATTACK … attack=bite … amount=80.000 vanilla=10.000` lines at each of t=76, 77, 78 and 79**,
    and ten `HURT before=80.000 after=72.727 … attack=bite`.
  - **`FIGHTWORLD died-inside`**, `BaronVonYeetus was slain by Ender Dragon`, `returned … why=died`. In the fight
    world, so its keep-inventory held.
  - The wing step then read `No fight fixture is running.`: the fight had ended at his death.
  - So **the first boot's RA3 is reproduced** (`GATE-fight-rules.md`, finding 1), and the first boot's EB3 (no bite)
    is NOT.
- **EB4, THE BREATH HIT, AS RA5 PREDICTED.**
  - `DRAGON to-breath t=91 cloud=ea365e93-… at=-3.10 0.00 -8.62 radius=5.00 interval=20`.
  - Then three `DRAGONATTACK t=105, 125, 145 attack=breath … base=30.000 gs=100 amount=30.000 vanilla=3.000
    phase=BREATH_ATTACK`, **20 ticks apart**, each with `HURT before=30.000 after=27.273 defense=10.000 … attack=breath`.
  - **`vanilla=3.000` READ** (INFERRED 3.000 before).
  - The first boot's RA5 and EB4 (no hit) are NOT reproduced. Why they missed is still not in the log.
- **EB5, THE FIGHT: READ IN FULL, ENDED BY HIS DEATH.**
  - `DEATH_RULES keep_inventory=false->true immediate_respawn=false->true`.
  - **Crossings:**
    - `DRAGONCROSS t=679 threshold=0.80 health=15915.000/20000.000 pending=1`;
    - `DRAGONCROSS t=1533 threshold=0.60 health=11830.000/20000.000 pending=1`.
  - **Mage 1:**
    - landed t=680 at (6.929, −28.240), **29.078** from the centre, with `FIGHT lightning t=680 … effect=true`;
    - slow close t=1023, killed t=1239 (`ticks_in_slow_close=216`), **559 ticks after landing**;
    - trail 6000 → 5795.750 (t=1023) … 185.250 (t=1236) → 0 (t=1239): 26 hits;
    - **one of the 26, at t=1070, was `amount=300.000 … dealer=<the Mage's own id> element=null`**, a self-attributed
      environmental hit whose cause is not in the log. INFERRED: the dragon's push, which is not an event and is not
      cancelled.
  - **Mage 2:**
    - landed t=1798 at (26.393, 5.559), **26.972** from the centre, with its lightning;
    - slow close t=2130; at the fight's end `health=4570.250/6000.000` (6 hits).
  - **Mages landed: two.** The fight ended at 59.15% (11830 of 20000), so a third was not yet owed.
  - **`DRAGONGATE`:**
    - five `why=mage-alive` (t=681 to 689, Mage 1 alive);
    - **seventeen `why=mage-owed` (t=1534 to 1667, the ring RETURNING after Mage 1's death, 0.60 crossed)**: the F9b
      rule, in play.
  - **`MAGEGATE`:** seventeen refused, during Mage 1's fast close.
  - **The dragon's attacks:**
    - **eight wing hits**, at t=850, 1044, 1047, 1050, 1053, 1056, 1529 and 1532. Each was `amount=40.000
      vanilla=5.000`, with HURT `after=36.364 … attack=wing`. Five of them came within 12 ticks (1044 to 1056), three
      apart.
    - **the bite at t=2214, 2215 and 2216**: seven `DRAGONATTACK … attack=bite`, **one of them `vanilla=5.000`**, a
      wing's vanilla hit called a bite because he was in both boxes. Then HURT `after=72.727 … attack=bite`.
    - **`FIGHTWORLD died-inside` at t=2216.**
  - **`DRAGONMAGE`:** thirty-eight `cancelled (the dragon's vanilla hit on its Mage)`, t=1032 to 1057 and t=2208 to
    2217. Twice a tick at times. The fix of section 7 is firing.
  - **Other lines:**
    - `DRAGONHEAL`: none (no crystal lit: each slow close was under 240 ticks);
    - `FIGHT fireballs`: none, the count never changed from 0;
    - `FIGHT fireball-cancelled`: t=201 and t=426;
    - crystal-fire: none.
  - **The ending:**
    - `FIGHT nobody inside t=2218 outcome=RUNNING`, `FIGHT mage-health … at=end health=4570.250/6000.000`;
    - `dragon removed … was_valid=true`, the world deleted;
    - `AWAIT fight ended ticks=2202`.
  - **Dragon hits:** 35 FIGHTHITs on the dragon by phase:
    - FLY_TO_PORTAL 12;
    - LAND_ON_PORTAL 11;
    - CIRCLING 6;
    - STRAFING 6.
  - **`/rpg mana refill`, twelve times** (wall clock, and the last fight tick logged before it):
    - 13:12:42 (t=364), 13:12:52 (t=576), 13:13:02 (t=780), 13:13:13 (t=980);
    - 13:13:15 (t=1035), 13:13:20 (t=1120), 13:13:24 (t=1204), 13:13:30 (t=1332);
    - 13:13:33 (t=1396), 13:13:39 (t=1500), 13:13:43 (t=1596), 13:13:48 (t=1688).
  - **Off the sheet:** `/rpg selftest fight-end`, typed at 13:12:49 and 13:13:00 during the run. Each was refused:
    `Refusing: you are not in the dev world (the server's first world).`.

### WHAT THE TWO RUNS TOGETHER SAY

1. **The bite is lethal in four ticks, every time it lands.**
   - Three readings agree: the first boot's RA3, and the second run's EB3 and EB5.
   - It lands twice a tick, the head's and the neck's boxes both holding him, with no invulnerability window.
   - **A per-player window on the dragon's hits is owed before any further fight.**
2. **The wing lands in play, and too often.**
   - Five hits in 12 ticks in EB5.
   - It never landed when staged (the first boot's RA4 and EB3).
3. **The breath lands as built** (the second run's EB4: 30 a second, `vanilla=3.000`).
   - The first boot's two misses are unexplained.
   - **Ben: "remove the knock back from it".**
4. **The owed-Mage immunity works in play:** seventeen `why=mage-owed` during a return.
5. **The End world:** eighteen of eighteen worlds over two runs read inert; Ben said yes twice.

---

## THE SECOND BOOT (the seat's fix brief and its addendum, 2026-11-19) — ITS R0, CARRIED STATE, RUN SHEET, AND EB3R2, EB4R2, EB5R2

**Status of this section: READ at the second boot, 2026-11-19, at `e772f8b5`.** Written before it, and committed before it. It is the one R0 and run
sheet for both gates: `GATE-fight-rules.md`'s RA3R2, RA4R2, RA5R2 and RB1, and this file's EB3R2, EB4R2 and EB5R2.
**Not re-run:** RA1, RA2, EA1, EB1, EB2. Everything above this section stands as read.

> **BEN'S OWN BOOT AT 13:09 (build `61ce1271`) IS A BOOT OUTSIDE THE PROCEDURE** (the seat's ruling, 2026-11-19).
> - Its readings stand, in *THE SECOND RUN* above.
> - It had no backup, no "booting now" and no R0 word before he logged in.
> - **It is not the second boot.** This is.

> **FOR THE RECORD, A CORRECTION CC MADE TO THE SEAT (2026-11-19):** at the first boot only questions 6 and 7 went
> unasked (EB5 aborted). CC's earlier "four ASKs" was wrong.

### WHAT CHANGED SINCE THE FIRST BOOT, IN THE LINES THE ROWS READ

- **One contact hit per window** (F9b fix A):
  - At most one bite or wing `DRAGONATTACK` per player per 20 ticks.
  - Those suppressed are counted in **`DRAGONATTACK suppressed player=<n> count=<k> window=<a>..<b>`**.
  - **The name follows vanilla's amount:** a `vanilla=10.000` hit is a bite, `vanilla=5.000` a wing (at Normal).
- **No fight damage outside the fight** (fix B): a refusal is **`FIGHTDAMAGE refused t=<t> player=<n> source=<s>
  amount=<a> why=not-in-this-fight`** (or `why=fight-gone`).
- **The breath:**
  - its vanilla event is cancelled (fix F), so no push;
  - **`DRAGONBREATH apply t=… cloud=… age=… radius=… affected=[…] players=[…]`** at each of its applications;
  - **`DRAGONBREATH velocity t=… player=… at_hit=… next_tick=…`** at each hit.
- **The touch** now ends `box=<his> attack_box=<the part's> inside=<true|false>` (fix D).
- **Every right-click in a fight world** logs **`FIGHTCAST player=<n> trigger=right_click result=<r> held=<item>`**, and
  `FIGHTCAST … dealt=nothing within=40` when a successful cast lands no hit in 40 ticks (fix D).
  - READ first, on this tree: no weapon, mana or cast code reads the world's environment or asks whether it is a fight
    world.
    - `git grep -nE "getEnvironment\(\)|World\.Environment|Environment\.(THE_END|NORMAL|NETHER)" -- paper/src/main`
      names only `FightLoader`, `FightFixtures`, `FightWorlds` and `MobOrigin`. The control: it hits `FightWorlds`'
      `.environment(` line, as it must.
    - `isFightWorld(` is called only by `FightFixtures` (including this new line), the crystal-fire listener and the
      selftest.
    - **A bare `Environment` needle is the wrong instrument:** it matches "environmental damage" in eight unrelated
      classes.
    - So nothing of ours is known to stop a weapon in an End world. What happened to the cursed emerald stays Ben's to
      say.
- **The arena is probed at every arena fight's end:** **`ARENAPROBE when=end world=… environment=… centre_top=…
  bedrock_in_column=… fire=… crystals=… dragons=…`**. The command's probe now reads `when=command`.

### THE ARITHMETIC (F9b fix E): EVERY ROW THAT CAN HURT HIM, AT DEFENCE 0, AGAINST 100 HEALTH

The largest hit after armour at defence 0 is the hit itself.

| row | what can land in it | the most that can land between heals | against 100 |
|---|---|---|---|
| `fight-rules` RA3R2 | one 4-tick touch of the head; window 20 | ONE contact hit, 80 | **80 < 100** |
| `fight-rules` RA4R2 | one 4-tick touch of a wing | ONE contact hit, at most 80 | **80 < 100** |
| `fight-rules` RA5R2 | 35 ticks on the breath, at most one application per 20 | TWO, 60 | **60 < 100** |
| `fight-rules` RB1 | one tick in the head's box, then he leaves; fix B refuses all after | ONE contact hit, 80 | **80 < 100** |
| EB3R2 | a 4-tick head touch; healed; a 4-tick wing touch (44 ticks later, so the window may allow it) | ONE contact hit before each heal, 80 | **80 < 100** |
| EB4R2 | 55 ticks on the breath: a fourth application would need 60 | THREE, 90 | **90 < 100** |
| **EB5R2** | **the fight, no safety** | **not bounded, by design** | **CAN KILL HIM** |

- **The premises, named in `GATE-fight-rules.md` under its arithmetic, and the same here.**
  - (a) Each staged row puts him on the floor 20 blocks south (`tp @s 0.5 65 20.5`) as soon as the dragon is held in
    HOVER, and back there after each touch. That spot is out of reach (INFERRED: in seven staged rows over two boots
    he waited at the centre, nearer, and no contact hit landed before any staging).
  - (b) On the cloud: no contact hit at the spot he is put (READ: 5 blocks ahead of the head, against the head's
    reach of 1.8; a sitting dragon's wing does not hurt). **It holds while he stands still; a bite on top of the breath
    would be 80 + 30 > 100.**
- **Each premise is witnessed** by the row's own `DRAGONATTACK … attack=bite|wing` lines, with their ticks.
- **Every row sets `death_rules` and heals first**, so even a death in one keeps his inventory. EB5R2 sets it too: a
  death there is real, he reappears where he entered, and he keeps everything he carried.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

The booter's checklist is `.claude/rules/booting.md`; this list is its instance for the second boot.

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup:** `run/plugins/Rpg` and `run/world` copied, as they are, to
   `../rpg-run-backups/2026-11-19-pre-fight-end-2` (`2026-11-19-pre-fight-end` exists). Report:
   - the file and directory counts;
   - `diff -rq lines: 0` for each;
   - the `ls` of `world/dimensions`;
   - the overworld's file count.

   **Nothing is restored, moved or touched in `run/world` or `run/plugins/Rpg`.** Whether his player file is restored
   from `2026-11-19-pre-fight-end` is Ben's decision; **without his words it is not, and the boot runs on the world as
   it is.**
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **The world's saved difficulty** (it wins over `server.properties`): both gates' setups set `difficulty: normal`
   before any row, so a saved PEACEFUL cannot void a row. The fight worlds are new each row.
6. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND IS SENT:** "booting now", and Ben told **NOT to log in until told R0
   passed.**
7. `./scripts/dev-server.sh --refresh-content`. **The REQUIRED deployed lines:**
   - `content/fights/dragon.yml` reads `  contact_hit_interval_ticks: 20`, `  environment: end`, `  bite: 80.0`;
   - `content/mobs/ender_dragon.yml` reads `max_health: 20000`.
8. R0a-R2 to R0e-R2 read on **this boot's** log (its `[Rpg] Build:` names `feat/fight-end`'s head).
9. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
10. *(After his `/stop`.)* Report:
    - `rpgfight` empty;
    - the overworld's file count against step 2's;
    - `All dimensions are saved`;
    - no `java` process left.

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED. IF YOU ARE TOLD NOTHING, DO NOT LOG IN.**
1. `/gamemode survival` — **alone, the first line after login.**
2. Stand on flat open ground with room all round, and hold the weapon you want to fight with.
3. `/rpg selftest fight-rules` — **ONCE.** Four rows, about two minutes, and **all four hurt you**: RA3R2 (a bite),
   RA4R2 (a wing), RA5R2 (the breath), RB1 (a bite as you leave).
   - **None can kill you:** the most any of them can do is 80 (one bite) against your 100, before your armour. The
     arithmetic is in the table above.
   - **Do not move while it runs.** You are moved for every row, and you float down slowly.
4. `/rpg selftest fight-end` — **ONCE.** Three rows and five questions, and up to **ten minutes** of fighting at the
   end.
   - **EB3R2 and EB4R2 hurt you and cannot kill you:** 80 at most between heals, then 90 at most (three breaths).
   - **In EB4R2, stand still where you are put.** Walking into the dragon's head is the one way it could kill you.
   - **EB5R2, the fight, CAN kill you:** you reappear at once where you entered, with everything you carried.
   - Each question says whether it wants yes or no, or your own words.
   - **`/rpg mana refill` is yours to use or not; the log records it.**
5. `/stop`

Nothing else this boot.

## R0 (SECOND BOOT) — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a-R2 — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`: `feat/fight-end`'s head as `git ls-remote origin refs/heads/feat/fight-end` prints it, shortened. Not `-dirty`, not `unknown`. **Not `e15cf9ee`, not `61ce1271`** (the two earlier builds) | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **AS PREDICTED (2026-11-19, 14:30:19).** `[Rpg] Build: e772f8b5`; `git ls-remote --heads origin feat/fight-end` printed `e772f8b55ec5f189aaaf9720c726e1fcef2f250b`, the head the seat diffed. Not `-dirty`, not `unknown`, not `e15cf9ee`, not `61ce1271` |

### R0b-R2 — the jar carries the fix

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `dragon.yml` lines `  contact_hit_interval_ticks: 20`, `  environment: end`; `FightInstance.class` carrying `DRAGONATTACK suppressed` and `FIGHTDAMAGE refused`; `FightFixtures.class` carrying `FIGHTCAST` and `ARENAPROBE when=`; the entry `io/github/butterflysmp/rpg/core/fight/ContactHitWindow.class`; `GATE-fight-end.yml` carrying `EB5R2:`; `GATE-fight-rules.yml` carrying `RB1:`. ABSENT: `dragon.yml` line `  environment: normal`, `NoSuchTextControl` in `FightInstance.class`, and **`GATE-fight-rules.yml` carrying `RA5:`** (the first boot's row, retired) | the PowerShell below, from the repo root | **AS PREDICTED.** `PRESENT dragon.yml '  contact_hit_interval_ticks: 20'`, `'  environment: end'`; `ABSENT  dragon.yml '  environment: normal'`; `PRESENT …/FightInstance.class 'DRAGONATTACK suppressed'`, `'FIGHTDAMAGE refused'`; `ABSENT  …/FightInstance.class 'NoSuchTextControl'`; `PRESENT …/FightFixtures.class 'FIGHTCAST'`, `'ARENAPROBE when='`; `PRESENT entry ContactHitWindow.class`; `PRESENT GATE-fight-end.yml EB5R2:`; `PRESENT GATE-fight-rules.yml RB1:`; `ABSENT  GATE-fight-rules.yml RA5:` |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Line($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
$d = Bytes 'content/fights/dragon.yml'
Line $d '  contact_hit_interval_ticks: 20' 'dragon.yml'; Line $d '  environment: end' 'dragon.yml'; Line $d '  environment: normal' 'dragon.yml'
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
Has 'io/github/butterflysmp/rpg/paper/fight/FightInstance.class' 'DRAGONATTACK suppressed'
Has 'io/github/butterflysmp/rpg/paper/fight/FightInstance.class' 'FIGHTDAMAGE refused'
Has 'io/github/butterflysmp/rpg/paper/fight/FightInstance.class' 'NoSuchTextControl'
Has 'io/github/butterflysmp/rpg/paper/fight/FightFixtures.class' 'FIGHTCAST'
Has 'io/github/butterflysmp/rpg/paper/fight/FightFixtures.class' 'ARENAPROBE when='
if ($zip.GetEntry('io/github/butterflysmp/rpg/core/fight/ContactHitWindow.class') -ne $null) { "PRESENT entry ContactHitWindow.class" } else { "ABSENT  entry ContactHitWindow.class" }
$e = Bytes 'selftest/GATE-fight-end.yml'; if ($e -ne $null -and $e -match "(?m)^EB5R2:\s*$") { "PRESENT GATE-fight-end.yml EB5R2:" } else { "ABSENT  GATE-fight-end.yml EB5R2:" }
$r = Bytes 'selftest/GATE-fight-rules.yml'
if ($r -ne $null -and $r -match "(?m)^RB1:\s*$") { "PRESENT GATE-fight-rules.yml RB1:" } else { "ABSENT  GATE-fight-rules.yml RB1:" }
if ($r -ne $null -and $r -match "(?m)^RA5:\s*$") { "PRESENT GATE-fight-rules.yml RA5:" } else { "ABSENT  GATE-fight-rules.yml RA5:" }
$zip.Dispose()
```

**The first boot's jar (`e15cf9ee`) reads ABSENT on every PRESENT line but `environment: end`, and PRESENT on `RA5:`.**

### R0c-R2 — the Loaded lines, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`** (unchanged). AND exactly one **`[Rpg] Loaded fight 'dragon': border 48.0, mage ring 12.0, v_fast 3.80, v_slow 0.60, e_kill 1.00, 10 crystals every 240 ticks, a strike every 40 ticks from 20.0; ring dust (170,0,255) size 1.60 every 1.00 blocks at [0.2, 1.7, 3.2, 4.7, 6.2, 7.7], row shift 0.25, turn 1.00/s alternating, every 4 ticks; mage 'dark_mage' refused item.shield.block, landing lightning on; crystals at 40.0, 12.0 up, lit block.beacon.power_select; participants within 128.0; arena at (0, 0) floor y 64, disc 56 x 3, 10 pillars 3 wide on 40.0 to +12, time 18000, entry (0.0, 0.0), environment end; dragon 'ender_dragon' Mages below [0.8, 0.6, 0.4, 0.2], win return 200 ticks, bar purple, bite 80.0, wing 40.0, breath 30.0/s, one contact hit per 20 ticks`** (`FightLoaderTest` asserts it verbatim). No `Refusing`, `Skipping`, `SEVERE` or `Exception`. **No `[Rpg]` WARN beyond `volley_stone`'s known pair.** No `No fights loaded` | `Select-String run\logs\latest.log -Pattern 'Loaded\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded'` (up to the first `joined the game`) | **AS PREDICTED.** Both Loaded lines verbatim at 14:30:20 (compared byte for byte against the predicted text), and exactly one fight line, ending `breath 30.0/s, one contact hit per 20 ticks`. `[Rpg]` WARN: `volley_stone`'s pair only. No `Refusing`, `Skipping`, `SEVERE`, `Exception` or `No fights loaded` before the join (14:31:16) |

### R0d-R2 — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 217 scenarios across 26 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive, fight-site, fight-arena, fight-dragon, fight-rules, fight-end]`**, no refusal after it. **The control:** the first boot's `221 scenarios across 26 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | **AS PREDICTED.** Exactly one `[Rpg] Selftest: dev server, 217 scenarios across 26 gates [melee-cell, … fight-rules, fight-end]`; `221 scenarios across 26 gates` ABSENT |

**RECOUNTED** (2026-11-19, this branch's tree; `SelfTestScenariosTest` asserts 217):
- the gates in `index.yml`: **26**;
- `^[A-Za-z0-9_-]+:` over them: **243**;
- `^setup:`: **26**;
- so **217**.

**Control:** 247 (the first boot) − fight-rules' RA1-RA5 (5) + its four (4) − fight-end's EA1, EB1-EB5 (6) + its three
(3) = 243.

### R0e-R2 — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, and no `deleting` and no `orphan` line | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | **AS PREDICTED.** Exactly one `FIGHTWORLD sweep path=C:\Users\Neb91\IdeaProjects\minecraft-rpg-scaffold\run\world\dimensions\rpgfight folders=0`; no `deleting` and no `orphan` line during the enable |

## THE ROWS (SECOND BOOT) — SURVIVAL, DIFFICULTY NORMAL. BEN'S ANSWERS VERBATIM; "not sure" IS NOT STATED; NO PASS OR FAIL ON BEN'S HALF

Every row is born as before: `FIGHTWORLD end-config …`, `FIGHTWORLD created … environment=THE_END`, `FIGHTWORLD
end-check … inert=true …`, `arena built`, `FIGHT start … site=arena`, `FIGHT dragon spawned`, `FIGHTWORLD entered`.

It ends as before, now with **`ARENAPROBE when=end … environment=THE_END … fire=0 …`** before `FIGHTWORLD returned`,
then `deleted … entries=<e>`.

### EB3R2 — the bite and the wing, with the window: question 4, re-asked (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRAGON phase-set … now=HOVER`; the `tp` to the floor; **no `DRAGONATTACK` for him before the touch**. (2) `DRAGON touch t=<T> part=head … inside=true`; **at most ONE contact `DRAGONATTACK`** for him before the heal: if it lands, **`… attack=bite … amount=80.000 vanilla=10.000 phase=HOVER`** and `HURT before=80.000 after=72.727 … attack=bite`; any further in **`DRAGONATTACK suppressed player=BaronVonYeetus count=<k> window=<T'>..<T'+19>`**. (3) `DRAGON touch … part=wing … inside=true`; **at most ONE**: if it lands, `… attack=wing … amount=40.000 vanilla=5.000 …` and `HURT before=40.000 after=36.364 … attack=wing`. **None is also predicted** (vanilla's own condition; unreadable). (4) No contact `DRAGONATTACK` in the 30 ticks on the floor after. (5) Ben's answer to **4**, verbatim | `GATE-fight-rules.md` RA3R2 and RA4R2 | **AS PREDICTED; THE WING ON ITS "NONE" BRANCH.** (1) `DRAGON phase-set t=16 phase=HOVER now=HOVER`; no `DRAGONATTACK` before the touch. (2) **`DRAGON touch t=76 part=head at=-0.85 18.52 -7.24 phase=HOVER box=-0.65,83.52,-7.04..-0.05,85.32,-6.44 attack_box=-1.85,83.02,-8.24..1.15,86.02,-5.24 inside=true`**; **one `DRAGONATTACK t=76 attack=bite … amount=80.000 vanilla=10.000 phase=HOVER`** and **`HURT before=80.000 after=72.727 defense=10.000 … attack=bite`**; **`DRAGONATTACK suppressed player=BaronVonYeetus count=7 window=76..95`** (printed at the fight's end). (3) **`DRAGON touch t=120 part=wing at=4.19 19.02 1.17 phase=HOVER box=4.39,84.02,1.37..4.99,85.82,1.97 attack_box=-1.31,83.02,-4.33..10.69,89.02,7.67 inside=true`**, 44 ticks after the bite, the window long closed: **no `DRAGONATTACK`**, so vanilla raised no event (as RA4R2). (4) No contact hit on the floor after. The end: `ARENAPROBE when=end … fire=0 … dragons=0`, `entries=17`. (5) Ben, to **4** ("The dragon's bite (80) and wing (40), at most one of them a second: do they feel right?"): **"yes"**. **The wing he answered about did not hit him in this row** |

### EB4R2 — the breath with no push: questions 5 and 8 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRAGON phase-set … phase=BREATH_ATTACK`; `DRAGON to-breath t=<t> cloud=<c> … radius=5.00 interval=20`. (2) **One to three `DRAGONBREATH apply … affected=[BaronVonYeetus] …` lines, 20 ticks apart**, each with **`DRAGONATTACK … attack=breath … amount=30.000 vanilla=3.000 phase=BREATH_ATTACK`** and `HURT before=30.000 after=27.273 … attack=breath`. (3) **No contact `DRAGONATTACK` (bite or wing) in the row.** (4) Each hit's `DRAGONBREATH velocity … at_hit=<v> next_tick=<v>`. **How it is read:** a vanilla knockback sets his server-side motion, which this line would show as a horizontal jump at `next_tick` (INFERRED). **A player's motion is driven by his own client, so the server's figure is not the witness.** Ben's answer to **8** is. (5) Ben's answers to **5** and **8**, verbatim | `FightRulesSourceTest` *theBreathIsCancelled…*; `GATE-fight-rules.md` RA5R2 | **AS PREDICTED.** (1) `DRAGON phase-set t=76 phase=BREATH_ATTACK now=BREATH_ATTACK`; `DRAGON to-breath t=91 cloud=bb5c6a81-… at=-1.24 0.00 -9.71 radius=5.00 interval=20`. (2) **Three `DRAGONBREATH apply … affected=[BaronVonYeetus] players=[BaronVonYeetus(xz=0.00 dy=0.00)]`, at t=105 (age 20), t=125 (age 40) and t=145 (age 60), 20 ticks apart**; the checks between read `affected=[]`. Each had **`DRAGONATTACK … attack=breath … base=30.000 gs=100 amount=30.000 vanilla=3.000 phase=BREATH_ATTACK`** and **`HURT before=30.000 after=27.273 defense=10.000 … attack=breath`**. **The third landed at t=145; the `tp` is 55 ticks after `to-breath` (t=91), so about t=146, and its tick is not logged**: three, as the arithmetic allowed (90). (3) **No contact `DRAGONATTACK` in the row.** (4) **`DRAGONBREATH velocity t=105 at_hit=0.000 -0.078 0.000 next_tick=0.000 -0.078 0.000`**, **t=125 the same**, **t=145 `at_hit=0.000 -0.078 0.000 next_tick=0.000 0.000 0.000`**: no horizontal motion on the server (INFERRED reading). (5) Ben, to **5** ("The breath, 30 a second, with no push: does it feel right?"): **"yes"**; to **8** ("Did the breath push you at all?"): **"no"** |

### EB5R2 — THE FIGHT, NO SAFETY: questions 6 and 7 (SURVIVAL; he may die)

| prediction | stands on | READING |
|---|---|---|
| (1) The fight's lines as the first boot's EB5 predicted them: rings, crystals, a Mage at each of 80, 60, 40 and 20 percent, the dragon's immunity while one is owed, `STRIKE` lines, and its end (`FIGHT won` or `FIGHT stopped`). (2) **Any two contact `DRAGONATTACK` lines for one player are at least 20 ticks apart** (the window); suppressed ones counted in `DRAGONATTACK suppressed` lines. (3) **No `HURT` line on him while he is outside the fight world**; any priced then is a `FIGHTDAMAGE refused` line. (4) A `FIGHTCAST … result=<r>` line for each of his right-clicks; any `FIGHTCAST … dealt=nothing within=40` is read as it stands. (5) **`ARENAPROBE when=end …`** once, at the end. (6) Every `mana refill` in the log, counted. (7) Ben's answers to **6** and **7**, verbatim | `ContactHitWindowTest`; `FightRulesSourceTest` *everyFightDamageIsApplied…* | **READ: HE WON. FOUR MAGES LANDED; HE DID NOT DIE.** (1) Born at t=0 (`end-check … inert=true`, `arena built … ms=98`, `dragon spawned t=0 … at=0.00 20.00 0.00 phase=CIRCLING`). **Crossings:** `DRAGONCROSS t=349 threshold=0.80 health=15915.000`, `t=1469 0.60 11830.000`, `t=2257 0.40 7949.250`, `t=3560 0.20 3864.250`. **The Mages** (distance from the centre computed from the `land` offsets): t=350 at (-8.10, 30.37), 31.4; t=1568 at (20.61, 5.66), 21.4; t=2522 at (-1.81, -26.20), 26.3; t=3586 at (26.28, -5.43), 26.8. **Each Mage's health:** 6000/6000 at the slow close, first hit 5795.750 (t=745, 1897, 2925, 3949), 0.000 at `FIGHT die` t=959, 2045, 3054, 4064. From landing that is 609, 477, 532 and 478 ticks; from the slow close 254, 175, 204 and 147; 27, 23, 24 and 24 hits. **MAGEGATE:** 7 refusals, all `phase=FAST_CLOSE … source=hit/kinetic`. **DRAGONGATE:** 34 refusals, `why=mage-owed` 24, `why=mage-alive` 10. **The end:** `FIGHT win t=4235 … exp_suppressed=500 drops_suppressed=0 return_in=200`, the dragon dead at t=4235, `win-return … after=200`, `FIGHTWORLD returned … why=end`. (2) **Four contact hits, all wings at `vanilla=5.000`, 40.000 each, `HURT … after=36.364`:** t=826 (CHARGE_PLAYER), t=846 (CIRCLING), t=1495 (LAND_ON_PORTAL), t=4090 (CHARGE_PLAYER). **No two closer than 20 ticks (826 to 846 is exactly 20). The most in any 20 ticks: 1.** Suppressed: `count=17 window=826..845`, `count=2 window=846..865`, `count=2 window=1495..1514`, `count=9 window=4090..4109`. No bite and no breath hit on him in the fight. (3) **No `HURT` after `FIGHTWORLD returned`; no `FIGHTDAMAGE refused` in the row.** (4) **110 `FIGHTCAST` lines:** 48 `result=Success`, 41 `OnCooldown`, 5 `InsufficientResource` (all `held=EMERALD`), 5 `no-weapon` (`held=COOKED_BEEF`), and **11 `dealt=nothing within=40`, all `held=EMERALD`**. (5) **`ARENAPROBE when=end … environment=THE_END centre_top=END_STONE bedrock_in_column=0 fire=0 (y 65..79) crystals=0 dragons=0`**, once. (6) **27 `/rpg mana refill`** (times and ticks in *THE SECOND BOOT, READ*). (7) Ben, to **6** ("For one player, is the dragon's 20,000 health right, now that every phase has its Mage?"): **"yes"**; to **7** ("What most needs to change before waves are built?"): **"nothing"** |

### OPEN FOR BEN, NOT ASKED BY ANY ROW (the seat's brief)

- **`contact_hit_interval_ticks`:** 20 is the seat's placeholder, and his to change.
- **Whether his player file is restored** from `2026-11-19-pre-fight-end`.
- **What happened to his cursed emerald.**

### THE SECOND BOOT, READ (2026-11-19, 14:30 to 14:37, at `e772f8b5`)

**The booter's checklist (`.claude/rules/booting.md`), ticked:**

1. **The seat's word and Ben's:** the seat's SECOND BOOT APPROVED (2026-11-19, from origin `95277db4` and `e772f8b5`);
   Ben's "ready", in his own words.
2. **The heads:** `git ls-remote` printed `feat/fight-end` `e772f8b55ec5f189aaaf9720c726e1fcef2f250b` and
   `feat/fight-rules` `95277db4a1cacf5727ebc918cd026688a83c9f02`, the two the seat diffed. The plan entry the approval
   called owed was already in `bde06c28`, so nothing was committed and the head did not move.
3. **Carried state:**
   - no JVM (`no java process`);
   - backup `../rpg-run-backups/2026-11-19-pre-fight-end-2`: `Rpg` 133 files, 24 dirs, `diff -rq lines: 0`; `world` 193
     files, 32 dirs, `diff -rq lines: 0`; `world/dimensions`: `minecraft`, `rpgfight`; the overworld **106** files;
     nothing restored;
   - `12:  dev-pack-on-join: true`;
   - `rpgfight` empty.
4. **"Booting now"**, with "Ben: do NOT log in until I tell you R0 passed", in its own message; the boot command in
   the next turn, after Ben's "ok".
5. `./scripts/dev-server.sh --refresh-content`. The jar was deployed at 14:30:14. Deployed: `dragon.yml`
   `  contact_hit_interval_ticks: 20`, `  environment: end`, `  bite: 80.0` (one each); `ender_dragon.yml`
   `max_health: 20000`.
6. R0a-R2 to R0e-R2 read on this boot's log, **all as predicted** (their cells). `[Rpg] Build: e772f8b5` at 14:30:19,
   `Done (5.813s)` at 14:30:20.
7. **"R0 passed"**, in its own message, at **14:30:54**, with no `joined the game` line in the log.
8. **His login: `BaronVonYeetus joined the game` at 14:31:16, after R0 was read (14:30:54).** `/gamemode survival` at
   14:31:40 was his first command; `/rpg selftest fight-rules` at 14:31:52 and `/rpg selftest fight-end` at 14:33:03,
   each once.
9. **After his `/stop` (14:37:40):**
   - `ThreadedAnvilChunkStorage: All dimensions are saved` at 14:37:41;
   - `rpgfight` empty; the overworld **106** files, as backed up;
   - `no java process`.

**Every fight world of the boot: seven, each END, each checked inert, each probed and deleted.**
- Each has `FIGHTWORLD end-check … inert=true fight=present bar_visible=false fight_dragon=none portal=none
  bedrock_at_centre=0 dragons=0` and `ARENAPROBE when=end … environment=THE_END centre_top=END_STONE
  bedrock_in_column=0 fire=0 (y 65..79) crystals=0 dragons=0`.
- Deleted, each `entries=17`, in boot order: `fight_0fa88a45` (RA3R2), `fight_2215e04d` (RA4R2), `fight_458201f2`
  (RA5R2), `fight_b6d8fe9f` (RB1), `fight_d721c6d4` (EB3R2), `fight_1cc79869` (EB4R2), `fight_0575b512` (EB5R2). Each
  is a `FIGHTWORLD deleting path=<abs>\run\world\dimensions\rpgfight\<n>` line, then its `deleted`.
- EB5R2's world first logged **`FIGHTWORLD unload-retry name=rpgfight_fight_0575b512 players=1 attempt=1`**: he was
  still in it on the first try, returned the same tick. `unloaded` followed a second later.
- `Scanning for legacy world dragon fight...`: **0** lines.

**The log's warnings:** no `[Rpg]` line matching `Refusing`, `Skipping`, `SEVERE`, `WARN` or `Exception` beyond
`volley_stone`'s known pair; no `Exception` or `SEVERE` anywhere.

**What the seat's list asks, beyond the cells:**

- **Every `DRAGONATTACK`, with its tick, name, `vanilla=` and amount:**

  | row | t | name | vanilla | amount |
  |---|---|---|---|---|
  | RA3R2 | 41 | bite | 10.000 | 80.000 |
  | RA5R2 | 47, 67 | breath | 3.000 | 30.000 |
  | RB1 | 41 | bite | 10.000 | 80.000 (then refused) |
  | EB3R2 | 76 | bite | 10.000 | 80.000 |
  | EB4R2 | 105, 125, 145 | breath | 3.000 | 30.000 |
  | EB5R2 | 826, 846, 1495, 4090 | wing | 5.000 | 40.000 |

- **Every `suppressed` line:**
  - RA3R2: `count=7 window=41..60`;
  - RB1: `count=3 window=41..60`;
  - EB3R2: `count=7 window=76..95`;
  - EB5R2: `count=17 window=826..845`, `count=2 window=846..865`, `count=2 window=1495..1514`, `count=9 window=4090..4109`.
- **THE LARGEST NUMBER OF PRICED CONTACT HITS ON HIM INSIDE ANY 20 TICKS, THE WHOLE LOG: 1.** Counted per fight from
  every bite and wing `DRAGONATTACK` tick.
- **Every `HURT` he took, by `attack=`:**
  - bite 2 (RA3R2, EB3R2), each `80.000 -> 72.727`;
  - breath 5 (RA5R2 two, EB4R2 three), each `30.000 -> 27.273`;
  - wing 4 (EB5R2), each `40.000 -> 36.364`;
  - and 4 lines with `dealer=self dealerType=self`, each beside a ring strike while he stood outside the ring:
    `STRIKE t=1080 k=1 damage=20.000 outside`, `t=1200 k=1 20.000`, `t=1720 k=1 20.000`, `t=1760 k=2 40.000`
    (`HURT 20.000 -> 18.182` three times, `40.000 -> 36.364` once).
- **His lowest health: NOT READ.** No line carries his health inside a row; every heal reply reads `Custom HP: 350/350`.
  The arithmetic, not a reading: EB5R2's `HURT after` total is 236.366 against his 350.
- **`FIGHTDAMAGE refused`:** one, RB1's (`t=42 … source=dragon-contact amount=80.000 why=not-in-this-fight`). **No
  `HURT` after any `FIGHTWORLD returned` line.**
- **Every `DRAGON touch`:**
  - RA3R2 head `inside=true`;
  - RA4R2 wing `inside=true` (no event);
  - RB1 head `inside=true`;
  - EB3R2 head `inside=true`, and wing `inside=true` (no event).
- **Every `DRAGONBREATH apply` and velocity line:**
  - the staged rows' are in RA5R2's and EB4R2's cells;
  - every application that named him had `xz=0.00 dy=0.00`;
  - every velocity line is `0.000 -0.078 0.000` or `0.000 0.000 0.000`.
  - EB5R2 has 136 `apply` lines and no breath hit on him.
- **EB5R2:**
  - crossings, each Mage's distance, health trail and time to kill, every `DRAGONGATE` `why=`, the four landings and
    the ending: its cell;
  - **he did not die;**
  - **every `/rpg mana refill`, 27:** 14:33:49 (t≈128), 14:33:56 (≈272), 14:34:01 (≈372), 14:34:23 (≈796),
    14:34:27 (≈880), 14:34:31 (≈968), 14:34:50 (≈1352), 14:34:55 (≈1440), 14:34:59 (≈1532), 14:35:21 (≈1976),
    14:35:25 (≈2044), 14:35:34 (≈2220), 14:35:39 (≈2320), 14:35:52 (≈2585), 14:35:56 (≈2676), 14:36:11 (≈2974),
    14:36:15 (≈3040), 14:36:17 (≈3084), 14:36:21 (≈3172), 14:36:26 (≈3260), 14:36:33 (≈3404), 14:36:39 (≈3528),
    14:36:43 (≈3616), 14:37:02 (≈3980), 14:37:06 (≈4060), 14:37:12 (≈4192), 14:37:16 (≈4235). Each tick is the nearest
    earlier fight-tick line's, not the command's own; the command logs none.
  - **Every cast-refused line, with its reason:** `OnCooldown` 41, `InsufficientResource` 5 (each the emerald),
    `no-weapon` 5 (cooked beef in hand); and 11 `dealt=nothing within=40` after a success, all the emerald. **No
    refusal names the world.**
  - Also in the row: 16 `DRAGONMAGE … cancelled` (t=830 to 845); 5 `FIGHT fireball-cancelled`, each `phase=STRAFING`.

**FOR THE RECORD (CC, 2026-11-19):** the approval called the plan section 1 entry owed. It was not: it had been
committed in `bde06c28` and was on the wire. CC's report before the approval had said it was owed, wrongly.

---

## EB4R3 — EB4R2 WITH ITS MARGIN (added 2026-11-19, the seat's queue item 4; NOT RUN, and not re-run unless a later boot wants it)

**Why:**
- The cloud applies at most once every 20 ticks (READ at F9b), so its applications in a stay starting at s fall at s,
  s+20, s+40 and s+60.
- EB4R2 stood him on it for 55 ticks: three breaths (90 before armour, under 100), with a fourth (120, over it) only 5
  ticks past the stay.
- EB4R3's stay is **50 ticks**, so a fourth needs **10 ticks more than the row gives**.
- `SelfTestBreathMarginTest` reads the shipped row and holds that margin.

### EB4R3 — the breath with no push, 50 ticks on the cloud: questions 5 and 8 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| As EB4R2's (1) to (5), with this difference: **at most THREE `DRAGONBREATH apply … affected=[BaronVonYeetus]` lines, and at most two if the first comes more than 10 ticks into the stay** (EB4R2's first came 14 ticks in, at age 20). So: two or three `DRAGONATTACK … attack=breath … amount=30.000` and `HURT before=30.000 after=27.273 …`, 20 apart. **The arithmetic: at most 3 x 30 = 90 against 100** (and his 350), with a fourth 10 ticks out of reach. No contact `DRAGONATTACK`. His answers to 5 and 8, verbatim | `SelfTestBreathMarginTest`; EB4R2 | |
