# GATE — STEP F9c: AN END-TYPE FIGHT WORLD (AND THE ONE BOOT FOR F9b's GATE TOO)

**Status: READ, ONE BOOT, 2026-11-19, at `e15cf9ee`** (the seat's boot approval of 2026-11-19; Ben's "ready"). R0 passed. EA1 read the INERT branch, as predicted; EB1 to EB4 read with Ben's five answers; **EB5 ABORTED** (he disconnected 32 seconds in), so questions 6 and 7 were not asked. The checklist, the after-run and the findings are in *THE BOOT OF 2026-11-19* at the end. Every prediction below was written **before** any boot of `feat/fight-end`, and this file is
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
