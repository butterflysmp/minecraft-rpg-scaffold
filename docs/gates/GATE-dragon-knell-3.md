# GATE — THE KNELL, 3: BEN'S RE-TIMING OF THE BELL (THE RESONANCE AT THE WARNING, A 1-SECOND SHOT, THE MOVE 44 TICKS AFTER), AND THE KNELL WALKING AGAIN

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-knell-3`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

> **READ, 2026-11-17: booted at `9d0cbffb` on the seat's approval and Ben's "ready".** Server log `06:10:56` to the
> clean `/stop` at `06:15:48`. Carried state: no JVM; backup `rpg-run-backups/2026-11-17-pre-dragon-knell-3`
> (`run/plugins/Rpg` 129 files, 387280 bytes; `run/world` 193 files, 204963789 bytes; `diff -rq lines: 0` each);
> `12:  dev-pack-on-join: true`. Ben logged in at `06:11:33` and typed `/gamemode survival` first (`06:11:46`), then each
> selftest once; both ran to `SELFTEST RUN FINISHED`.
>
> **The result: every AUTO row AS PREDICTED, 25 of 25, and WA1 reads the Knell walking again after a beam** (its
> distance from him fell 1.562 blocks between the fire and 30 ticks later, against 0.20 predicted).
>
> **Ben's four answers: "yes", "yes"; then, to "Did the Knell move about a second before the ringing faded?", "it moved
> about a second after the rining stopped", and to "Does the whole sequence feel timed right now?", "not quite".** The
> last two are his words, not yes or no, recorded verbatim; **the 44 is NOT CONFIRMED.**
>
> **Counts:** 30 TELEGRAPH lines, each with exactly two sound lines at its tick, `block.bell.use` then
> `block.bell.resonate`; 29 FIRE lines (the 30th telegraph is KB9R3's, cancelled `elapsed=8/20`), each with exactly one,
> `block.amethyst_block.chime`, at its tick; **every fire 20 ticks after its telegraph (29 of 29)**; 17 TELEPORT
> decisions, **every one 44 ticks after the fire of the hit that scheduled it (17 of 17)**; 5 `skipped … knell-gone`, each
> inside its own row; 1 `ignored … pending` (TP14R2). 23 hits and 23 HURT lines, each `before=65.000 after=59.091
> defense=10.000 dealerType=wither_skeleton/knell`: no hit from anything but a beam. Every LEFTOVER line `living=0`. Both
> runs: `DIFFICULTY found=NORMAL set=NORMAL`, and `RESTORED … difficulty=NORMAL`.

**What this gate re-predicts, and why.** Ben, 2026-11-17, after the boot of `58c0f7a1` (`PLAN-dragon-mobs.md`, RULINGS,
*the re-timing of the bell*): *"1. Start playing the resonance at the same time as the bell 2. change the warning shot
time from 1.3s to 1s 3. change the teleport time to one second before the resonance sound ends"*. The seat's reading: the
strike and the resonance at the lock, the fire 20 ticks later, the move 44 ticks after a hit (the seat's arithmetic, not
his number). **Every row of `selftest/GATE-dragon-beam.yml` and `selftest/GATE-dragon-teleport.yml` changes in a time or
a sound**, so every row is re-predicted HERE under the next suffix (KB1R2 → KB1R3, KB10R3 → KB10R4, TP2R2 → TP2R3, TP14
→ TP14R2), and **WA1 is new**: the Knell walking again after a beam, which KB2R2's part (5) read unwalked. **Both
scenario files are paired with THIS gate** (`SelfTestScenariosTest.docOf`, and the note in `selftest/index.yml`). The old
rows' predictions and readings stay in `GATE-dragon-knell-2.md`, untouched, under a dated note.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL.
- **The difficulty is set by the selftest:** each gate's setup `difficulty: normal`; `DIFFICULTY found=… set=NORMAL`;
  `RESTORED … difficulty=<what it found>`.
- **Gear score 100 on every Knell:** a hit is 65 × 100 / 100 = **65.000** before defence (`MobBeam.price`;
  `MobLoaderTest.theBundledKnellBeamsAsBenRetimedIt`).

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC, ROW BY ROW

**The floor:** his maximum is at least 100 (it read **350** at the boot of `58c0f7a1`: `REPLY "Custom HP: 350/350"`).
**65 before defence leaves at least 35**, so every row heals him before every beam that can land, and no row can land two
beams between heals. **No Knell can reach him to melee** (a Wither tick from a melee hit is priced in proportion to his
maximum, so melee is ruled out by staging, never bounded):

- **every frozen Knell** (NoAI: it cannot walk or attack);
- **KB11R4's live Knell**, its walking speed BASE set to 0, at a horizontal 8 blocks, which every move keeps;
- **KB2R3's and WA1's live, WALKING Knells**, by distance and time (K2).

| row | what can hit him after a heal | the worst case, before defence |
|---|---|---|
| KB1R3, KB4R3, KB7R3, KB2R3 | one forced beam that hits | **65** |
| KB3R3, KB5R3, KB8R3, WA1 | one forced beam that misses | **0** predicted, 65 if it hit |
| KB6R3, KB9R3 | none (out of range; killed mid-telegraph) | **0** |
| KQ1R3 | two forced beams, **a heal between** | **65**, then **65** |
| KB10R4, TP2R3-TP4R3, TP6R3-TP13R3, TQ1R3 | one forced beam that hits | **65** |
| TP5R3 | a miss | **0** |
| TP14R2 | two forced beams, **a heal between** (25 ticks after the first force) | **65**, then **65** |
| KB11R4 | its own beams, at least 120 ticks apart (a 100-tick minimum wait after a fire, then a 20-tick telegraph), a heal every 100 ticks | **65 between two heals** |

## WHAT THE ROWS STAND ON — EACH FACT, AND ITS MARK

- **K1. The timings, READ in our code and at the boot of `58c0f7a1`:** the fire is exactly `telegraph_ticks` loop steps
  after the lock (28 of 28 read at 26), and the move exactly `delay_ticks` after the fire (16 of 16 read at 89). Only the
  numbers move: **20 and 44** (`knell.yml`; `MobLoaderTest`).
- **K2. THE WALKING KNELLS CANNOT REACH HIM.** READ from the server jar and EXECUTED (`awk`, the same recurrence as the
  jshell run of 2026-11-17, which it reproduces: acceleration 0.042995, steady 0.094703 blocks a tick):
  - **Sight and range** as `GATE-dragon-knell-2.md` K2 (must-see targeting, COLLIDER sight, `FOLLOW_RANGE` 16.0), READ;
    both rows stand him 15 blocks away on an open floor. **WA1's step south keeps him in range:** even had the Knell not
    walked at all, sqrt(15² + 3²) = 15.30 < 16.
  - **KB2R3:** live from its spawn to the row's kill at most **107 ticks** (wait 60, AT, wait 5, AT, force, wait 40, AT,
    kill; 2 ticks of spawning), of which the telegraph roots **20**: it walks at most **87 ticks, 8.24 blocks**.
  - **WA1:** at most **120 ticks** (wait 60, AT, wait 5, AT, force, wait 3, step, wait 48, AT, kill; and the same 2),
    rooted **20**: at most **100 ticks, 9.47 blocks**.
  - **To melee him it must close to about 3 blocks** (INFERRED, not read: its half-width 0.56, his 0.3, a reach under 2),
    **so it must walk at least 12. Margins: KB2R3 3.76 blocks (40 ticks at top speed); WA1 2.53 blocks (27 ticks).**
  - **Its own cycle cannot beam him in either row:** the first own attempt is at least 100 ticks after it appears, after
    the force (at about 65); the force's fire redraws the wait, 100 to 200 after the fire, after the row's kill.
- **K3. THE WALK AFTER A BEAM, DIAGNOSED (B4).** Every `javap` is `~/.jdks/openjdk-26.0.1/bin/javap -c -p -cp
  run/versions/26.1.2/paper-26.1.2.jar <class>`:
  - `com.destroystokyo.paper.entity.PaperPathfinder#stopPathfinding` calls `PathNavigation#stop`, which sets `path` to
    null and nothing else. **READ.**
  - `PathNavigation#isDone` is `path == null || path.isDone()`, so after a stop it is true. **READ.**
  - `MeleeAttackGoal#canContinueToUse`, constructed `(this, 1.2, false)` by `AbstractSkeleton#reassessWeaponGoal` at
    priority 4: with `followingTargetEvenIfNotSeen` false it returns `!navigation.isDone()`, **false after a stop**.
    **READ.**
  - `GoalSelector#tick` stops every running goal whose `canContinueToUse` is false, then starts each idle goal whose
    `canUse` is true; `Mob#serverAiStep` runs that full `tick` only when `(tickCount + id) % 2 == 0` (or `tickCount <=
    1`), else only the running goals' `tick`. **READ.**
  - `MeleeAttackGoal#canUse` returns false while `gameTime − lastCanUseCheck < 20` (`COOLDOWN_BETWEEN_CAN_USE_CHECKS`),
    and only when it passes that check does it record the time, need a live target, and `createPath(target, 0)`;
    `#start` calls `navigation.moveTo(path, 1.2)`. The stroll goal is priority 5, so the melee goal may take over from
    it. **READ.**
  - **Our root:** `MobBeamer#root` zeroes the horizontal velocity ONCE at the lock and adds a transient ×0 speed
    modifier; the ROOT step calls `stopPathfinding` every telegraph tick; `MobBeamer#unroot` at the FIRE removes the
    modifier by its key. **READ (our code).** Nothing else is left behind: the velocity is not re-zeroed, and the
    attribute returns to its base.
  - **THE CONSEQUENCE, INFERRED from those reads (the order of our entity-scheduler task against the mob's own AI within
    one server tick was not read):** during the telegraph each stop ends the melee goal; it restarts only through
    `canUse`, at most once every 20 game ticks, on alternate ticks; after the fire nothing stops it, so it restarts at
    the first passing `canUse`, **at most 21 ticks after the fire.** **NOT A DEFECT: a bounded delay.** KB2R2's reading
    (standing still 13 ticks after its fire) is inside that bound.
  - **WA1's read stands on it:** 30 ticks after the fire is at least 9 ticks of walking past the bound, which is at
    least **0.739 blocks** in a straight line from rest (executed above), against a predicted **0.20**.
- **K4. The geometry of every frozen row is `GATE-dragon-knell-2.md`'s, READ at the boot of `58c0f7a1`** (a Knell 8 east:
  `dir=-0.961705 -0.274086 0.000000`, `length=12.259 clip=block`; every landing, wall, pit and sight line). Only the
  times and the sounds move.
- **K5. A forced beam during a pending move, READ at that boot (TP14):** `TELEPORT ignored … reason=pending`, the first
  move at its own fire + delay. **The Knell's own cycle cannot do this** (100 > 44, the load-time check).
- **K6. Ben's feet F and a frozen Knell's K = F + (8, 0, 0)**; F is read from a TELEGRAPH's `to=` as F + (0, 1.080, 0).
- **K7. The sounds, READ in our code:** `MobBeamer.playSounds` plays the authored list in order and logs one `BEAM sound`
  line each, the telegraph's just before the TELEGRAPH line, the fire's just before the FIRE line. **What the client
  played and when it became audible is Ben's to hear.**

## WHAT THE RE-TIMING CHANGES (B3), READ IN THE CODE, CHANGED NOTHING

- **The resonance now starts at the lock, whether or not the beam will hit.** A miss rings on with no teleport to mark its
  end; only a hit's move does.
- **A Knell killed mid-telegraph leaves both telegraph sounds ringing** (KB9R3): `World#playSound(Location, …)` sends a
  sound at a position, and nothing recalls it. **The API CAN stop a sound already sent**, READ in the paper-api jar
  (`javap -cp ~/.m2/repository/io/papermc/paper/paper-api/26.1.2.build.74-stable/paper-api-26.1.2.build.74-stable.jar
  org.bukkit.entity.Player`): `Player#stopSound(String, SoundCategory)`, **per player, by key and category**, so it
  would also cut any other Knell's resonance that player hears. **NOT BUILT** (the brief: not in this step).
- **The window in which a hit Knell is free to walk and melee before it moves is 44 ticks, down from 89.**
- **A miss now has only the chime at its fire.**

## THE ROW AUDIT (B5): WHERE EVERY TELEPORT LINE LANDS, AT FIRE + 44

| file | rows | a hit? | where its TELEPORT line lands |
|---|---|---|---|
| beam | KB1R3, KB4R3, KB7R3, KB2R3 | yes | **inside the row:** the kill is 40 ticks after the force (about 19 after the fire), the move would be 44 after the fire: `TELEPORT skipped … reason=knell-gone` before the row's `SETTLED` |
| beam | KB3R3, KB5R3, KB8R3, WA1 | no | **none** |
| beam | KB6R3, KB9R3 | no fire | **none** |
| beam | KQ1R3 | yes, up to twice | the first move during ASK 1 (the production draw, inside the row); the second force at least 140 ticks after the first; the second hit's move inside the row, or skipped by the kill, RECORDED |
| teleport | KB10R4, TP2R3-TP4R3, TP6R3-TP11R3 | yes | **inside the row:** the move at most 66 after the force, the read at 130 |
| teleport | TP5R3 | no | **none** |
| teleport | TP12R3 | yes | inside the row: he steps north 40 ticks after the force, the move at about 65 |
| teleport | TP13R3 | yes, killed | the skip line inside the row: the kill 35 ticks after the force |
| teleport | TP14R2 | yes, twice | the `ignored` line and the first move inside the row: the second fire about 51 after the first force, the move about 65 |
| teleport | KB11R4 | its own | each move inside the row; anything pending at the end cancelled by the row's own kills, inside it |
| teleport | TQ1R3 | yes | inside the row, before the ASKs |
| merged | DB1R2, DB11R2, WS-C, WS2c | never (frozen, or no Knell) | **none** |
| closed manual gates (13) | a live Knell's own beam | yes | **a finding, carried**: such a row now hears the resonance at the warning, takes 65 one second later, and sees its Knell move 44 ticks after a hit; not re-predicted |

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-dragon-knell-3"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   If the folder exists, add `-2` to its name. Report the folder, both pairs of counts, and `diff -rq lines: 0` each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report.
4. **Boot this branch's head:** `./scripts/dev-server.sh --refresh-content`, announcing "booting now", on Ben's "ready".
5. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` line names this branch's head. **Readings count
   only after Ben's LOGIN in that boot's log.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest dragon-beam` — **ONCE.** Stand still and touch nothing, **except in its last part**: a Knell twelve
   blocks east beams you twice, with a gold line in chat before each. **Listen at the warning:** the bell STRIKE and the
   RINGING start together (a thin purple line appears with them); **the beam (a thick cyan line, with a chime) fires one
   second later.** **The first time, stand still. The second time, step sideways (A or D) as soon as you hear the
   strike.** Two questions; answer each `yes` or `no`.
3. `/rpg selftest dragon-teleport` — **ONCE.** Stand still and touch nothing, **except in its last part**: a Knell twelve
   blocks east beams you once. **After the hit, watch the Knell and listen to the ringing fade: it should move about a
   second before the ringing is gone.** Two questions; answer each `yes` or `no`.
4. `/stop`

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-knell-3` prints it, shortened. **Not** master's `232f82b4`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1`, after this boot's `Done` | READ 2026-11-17: `[06:10:56] [Server thread/INFO]: [Rpg] Build: 9d0cbffb`, this PR's head (`git ls-remote` `9d0cbffb6e41…`). **MATCH** |

### R0b — the jar carries the re-timing

| prediction | instrument | READING |
|---|---|---|
| In `content/mobs/knell.yml`: PRESENT `  telegraph_ticks: 20`, `  flash_at: [0, 15]`, `  delay_ticks: 44`, **the telegraph list holding the strike then the resonance**, and **the fire list holding the chime first**; ABSENT `  telegraph_ticks: 26`, `  flash_at: [0, 20]`, `  delay_ticks: 89`. Keys: `GATE-dragon-beam.yml` `KB1R3:` and `WA1:` PRESENT, `KB1R2:` ABSENT; `GATE-dragon-teleport.yml` `KB10R4:` and `TP14R2:` PRESENT, `KB10R3:` ABSENT. **The controls, the same on both builds:** `MobBeamLines.class` PRESENT `BEAM sound mob=`; `GATE-dragon-blast.yml` `BQ1R2:` PRESENT | the scan below | READ: all 16 lines as predicted: the three new number lines PRESENT and the three old ABSENT; `telegraph = strike, resonance` and `fire = chime first` PRESENT; `BEAM sound mob=` PRESENT; `KB1R3:`, `WA1:`, `KB10R4:`, `TP14R2:` PRESENT, `KB1R2:` and `KB10R3:` ABSENT; `BQ1R2:` PRESENT. **MATCH** |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$k = Bytes 'content/mobs/knell.yml'
foreach ($l in '  telegraph_ticks: 20', '  flash_at: [0, 15]', '  delay_ticks: 44', '  telegraph_ticks: 26', '  flash_at: [0, 20]', '  delay_ticks: 89') {
  if ($k -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT knell.yml '$l'" } else { "ABSENT  knell.yml '$l'" }
}
if ($k -match '(?m)^    telegraph:\r?\n      - \{ key: block\.bell\.use, volume: 1\.0, pitch: 1\.0 \}\r?\n      - \{ key: block\.bell\.resonate, volume: 1\.4, pitch: 1\.0 \}') { "PRESENT knell.yml telegraph = strike, resonance" } else { "ABSENT  knell.yml telegraph = strike, resonance" }
if ($k -match '(?m)^    fire:\r?\n      - \{ key: block\.amethyst_block\.chime, volume: 1\.0, pitch: 1\.0 \}') { "PRESENT knell.yml fire = chime first" } else { "ABSENT  knell.yml fire = chime first" }
$b = Bytes 'io/github/butterflysmp/rpg/paper/health/MobBeamLines.class'
if ($b -ne $null -and $b.Contains('BEAM sound mob=')) { "PRESENT MobBeamLines.class 'BEAM sound mob='" } else { "ABSENT  MobBeamLines.class 'BEAM sound mob='" }
foreach ($q in @(@('dragon-beam', 'KB1R3'), @('dragon-beam', 'WA1'), @('dragon-beam', 'KB1R2'), @('dragon-teleport', 'KB10R4'), @('dragon-teleport', 'TP14R2'), @('dragon-teleport', 'KB10R3'), @('dragon-blast', 'BQ1R2'))) {
  $y = Bytes "selftest/GATE-$($q[0]).yml"
  if ($y -eq $null) { "MISSING selftest/GATE-$($q[0]).yml" } elseif ($y -match "(?m)^$($q[1]):\s*$") { "PRESENT $($q[0]) $($q[1]):" } else { "ABSENT  $($q[0]) $($q[1]):" }
}
$zip.Dispose()
```

**A jar of master `232f82b4` reads the opposite on every `knell.yml` line and both list checks, ABSENT on the four new
keys and PRESENT on `KB1R2:` and `KB10R3:`**; the two controls read the same on both.

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim the boot of `58c0f7a1`'s: this PR changes `knell.yml` and adds no content file. No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN` beyond `volley_stone`'s known pair. **A `Skipping malformed mob 'knell.yml'` line would mean the beam or teleport block was refused** (`4 mobs`) | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | READ: the Loaded line verbatim as predicted; no `Refusing`, `Skipping`, `SEVERE` or `Exception` in the whole log; the only `[Rpg]` WARN lines are `volley_stone`'s known pair (and Paper's own update banner, not ours). **MATCH** |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 117 scenarios across 17 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport]`**, and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | READ: `[Rpg] Selftest: dev server, 117 scenarios across 17 gates [melee-cell, … dragon-beam, dragon-teleport]`, the list verbatim; no `Refusing selftest`. **MATCH** |

**The 117 is RECOUNTED from the files at this branch's head:** in `paper/src/main/resources/selftest/`,
`grep -cE '^  - ' index.yml` prints **17**; `cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/;
s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints **134**; the same with `'^setup:'` prints **17**: 134 − 17 = **117**.
**The control: at master `232f82b4` the same commands print 17, 133, 17: 116.** `SelfTestScenariosTest` pins the 117
and every row name.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument:** `Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-(beam|teleport)|\[Rpg\] (BEAM|TELEPORT)'`,
after Ben's LOGIN, inside each row's own `START … DONE` bracket.

**In EVERY row (not repeated):** `LEFTOVER living=0 radius=16` after its CLEAN; each spawn's `DRIVE spawn tag=<tag> <uuid>
knell frozen` (or `live`) and its `REPLY "Spawned Knell (wither_skeleton, 360 HP, GS 100 given)" [aqua, gray]`; each
`DRIVE beam tag=<tag> <uuid>` with no `refused=`; each `CMD /kill @e[tag=<tag>]` followed by the runner's `SETTLED
dying=0 ticks=<t> first=1`. **No `[Rpg]` line names a Knell that is not the row's own.**

**A FORCED HIT from a frozen Knell 8 east reads, in this order:**
1. `[Rpg] BEAM sound mob=knell id=<K> when=telegraph tick=<T> key=block.bell.use volume=1.00 pitch=1.00`
2. `[Rpg] BEAM sound mob=knell id=<K> when=telegraph tick=<T> key=block.bell.resonate volume=1.40 pitch=1.00`
3. `[Rpg] BEAM telegraph mob=knell id=<K> gs=100 forced=true tick=<T> lived=<L> at=<K> target=player <Ben> from=<K + (0, 3.360, 0)> to=<F + (0, 1.080, 0)> dir=-0.961705 -0.274086 0.000000 range=40.00 ticks=20`
4. **exactly one** `[Rpg] BEAM sound … when=fire tick=<T+20> key=block.amethyst_block.chime volume=1.00 pitch=1.00`
5. `[Rpg] BEAM fire … base=65.000 tick=<T+20> … length=12.259 clip=block tolerance=1.00 tested=1 [player <Ben> gap=0.000 hit] dealt=65.000`
6. `[Rpg] BEAM report … tick=<T+20> … to=<F + (-0.047, 1.066, 0.000)>`
7. `HURT before=65.000 after=<65 × 100 / (100 + Dp)> defense=<Dp> dealer=<K> dealerType=wither_skeleton/knell` (59.091 at his 10.000, as read)

**A MOVE** is `[Rpg] TELEPORT mob=knell id=<K> target=player <Ben> tick=<FIRE + 44> from=… center=… radius=… bearing=… side=…
first=… tried=<n> […] landed=… moved=…`.

### KB1R3 — supersedes GATE-dragon-knell-2.md KB1R2: a forced hit, two sounds at the lock, the chime 20 ticks later (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit's seven lines exactly as above. Then `CMD /kill @e[tag=kb1r3]`, **exactly one** `TELEPORT skipped mob=knell id=<K> target=<Ben> reason=knell-gone`, and `SETTLED`. **No TELEPORT decision line** | K1, K4, K7 | READ (F = -102.500 63.000 6.500 in this run): strike and resonance at `tick=1112`, TELEGRAPH `tick=1112 lived=6 at=-94.500 63.000 6.500 … dir=-0.961705 -0.274086 0.000000 range=40.00 ticks=20`; the chime and FIRE at `tick=1132 … length=12.259 clip=block tolerance=1.00 tested=1 [player <Ben> gap=0.000 hit] dealt=65.000`; one REPORT; `HURT before=65.000 after=59.091 defense=10.000`; after the kill one `TELEPORT skipped … reason=knell-gone`, then `SETTLED`; no decision line. **MATCH** |

### KB2R3 — supersedes KB2R2: the root on a walking Knell, at 20 ticks (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (0) `DRIVE spawn tag=kb2r3 <uuid> knell live`, 15 east. (1) **THE CONTROL:** `AT tag=kb2r3 <uuid> x=<x1> …` 60 ticks after the spawn, five ticks later `x=<x2>`, **x2 < x1 − 0.01**; if not, the row is HOLLOW, recorded as such, never as a pass. (2) the two telegraph sound lines and **exactly one** TELEGRAPH, `forced=true`, `ticks=20`, its `at=` RECORDED. (3) **The FIRE 20 ticks later, its `at=` equal to the TELEGRAPH's on all three coordinates, to three places.** (4) the chime; `[player <Ben> gap=0.000 hit] dealt=65.000`, `length` and `dir` RECORDED; one REPORT; `HURT before=65.000 …`. (5) the AT 40 ticks after the force (about 19 after the fire, inside K3's bound): **RECORDED, not predicted** (WA1 predicts the walk). (6) the kill, one `TELEPORT skipped … reason=knell-gone`. (7) no HURT but the beam's | K2, K3, I11 | READ: (0) `knell live`. (1) **CONTROL PASSED:** `x=-89.953`, then `x=-90.310` (0.357 west). (2) both telegraph sounds and one TELEGRAPH `tick=1247 lived=66 at=-90.381 63.000 7.002 … ticks=20`. (3) FIRE `tick=1267 at=-90.381 63.000 7.002`, **the same to three places: ROOTED. MATCH.** (4) the chime; `length=18.188 … [player <Ben> gap=0.000 hit] dealt=65.000`, `dir=-0.981945 -0.184736 -0.040693`; one REPORT; one HURT `before=65.000 after=59.091`. (5) RECORDED: the AT 40 ticks after the force read `x=-91.028 y=63.000 z=7.995` (it had walked 0.647 west and 0.993 south of the fire position). (6) one `TELEPORT skipped … reason=knell-gone`. (7) no HURT but the beam's. **MATCH** |

### WA1 — NEW: the Knell walks again after a beam (B4) (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (0) `DRIVE spawn tag=wa1 <uuid> knell live`, 15 east. (1) **THE CONTROL**, as KB2R3's: x2 < x1 − 0.01, or HOLLOW. (2) the two telegraph sound lines, one TELEGRAPH `forced=true ticks=20` at tick T, its `at=` RECORDED. (3) `CMD /tp @s ~ ~ ~3` after it. (4) the chime, and FIRE `tick=<T+20>` with `at=` the TELEGRAPH's (rooted) and **`[player <Ben> gap=<g> miss] dealt=none`, g ≥ 2.0** (he stands 3 south of the line's end), RECORDED; no REPORT, no HURT, and **no TELEPORT line of any kind**. (5) **THE WALK:** the AT line 51 ticks after the force (about 30 after the fire): **its horizontal distance from his feet, F + (0, 0, 3), is less than the FIRE's `at=` distance from them by more than 0.20.** If it is not, the Knell did not walk again inside K3's bound: **a defect, recorded, nothing changed before diagnosis.** (6) the kill, **no** TELEPORT line. (7) no HURT at all | K2, K3 | READ: (0) `knell live`. (1) **CONTROL PASSED:** `x=-89.953`, then `x=-90.310`. (2) both telegraph sounds and TELEGRAPH `tick=1382 lived=66 at=-90.381 63.000 7.002 ticks=20`. (3) `CMD /tp @s ~ ~ ~3`. (4) the chime and FIRE `tick=1402 at=-90.381 63.000 7.002` (rooted) `… [player <Ben> gap=2.581 miss] dealt=none` (g = 2.581 ≥ 2.0); no REPORT, no HURT, no TELEPORT line. (5) **THE WALK:** the AT 51 ticks after the force read `x=-91.791 y=63.000 z=8.016`. His feet F + (0, 0, 3) = (-102.500, 9.500): distance at the fire **12.374**, at fire + 30 **10.811**, **a drop of 1.562 > 0.20** (executed). (6) the kill; no TELEPORT line. (7) no HURT. **MATCH: the Knell walks again after a beam.** |

### KB3R3 — supersedes KB3R2: the line fixed at the lock; 3 south, missed (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Lines 1-3 of a forced hit; `CMD /tp @s ~ ~ ~3`; the chime; `BEAM fire … tick=<T+20> … [player <Ben> gap=2.700 miss] dealt=none`; no REPORT, no HURT; after the kill, no TELEPORT line | K4 | READ: TELEGRAPH `tick=1468`; FIRE `tick=1488 … [player <Ben> gap=2.700 miss] dealt=none`; no REPORT, HURT or TELEPORT line. **MATCH** |

### KB4R3 — supersedes KB4R2: 1.2 south, 0.900 off, still hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `… gap=0.900 hit] dealt=65.000`; one REPORT; `HURT before=65.000 …`; after the kill one `TELEPORT skipped … reason=knell-gone`, inside this row | K4 | READ: FIRE `tick=1563 … gap=0.900 hit] dealt=65.000`; one REPORT; one HURT `before=65.000 after=59.091`; one `TELEPORT skipped … knell-gone` inside this row. **MATCH** |

### KB5R3 — supersedes KB5R2: 1.4 south, 1.100 off, missed (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `… gap=1.100 miss] dealt=none`; no REPORT, no HURT, no TELEPORT line | K4 | READ: FIRE `tick=1638 … gap=1.100 miss] dealt=none`; no REPORT, HURT or TELEPORT line. **MATCH** |

### KB6R3 — supersedes KB6R2: beyond the range, nothing (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `BEAM none mob=knell id=<K> forced=true tick=<t> range=40.00 reason=no-target`; **no BEAM sound line**, no TELEGRAPH, no FIRE; `AT … ` 41.000 east of F; no TELEPORT line | the knell-2 reading of KB6R2 | READ: `BEAM none mob=knell id=<K> forced=true tick=1693 range=40.00 reason=no-target`; no sound, TELEGRAPH or FIRE line; `AT … x=-61.500 y=63.000 z=6.500` (41.000 east of F); no TELEPORT line. **MATCH** |

### KB7R3 — supersedes KB7R2: a husk in the line keeps its health (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| the husk `customHP=100.000/100.000` before and after; `BEAM fire … tested=2 [player <Ben> gap=0.000 hit, husk <uuid> gap=0.144 spared] dealt=65.000`; one REPORT; one `HURT before=65.000`; one `TELEPORT skipped … knell-gone` | K4 | READ: the husk `customHP=100.000/100.000` before and after; FIRE `tick=1768 … tested=2 [player <Ben> gap=0.000 hit, husk <uuid> gap=0.144 spared] dealt=65.000`; one REPORT; one HURT; one `TELEPORT skipped … knell-gone`. **MATCH** |

### KB8R3 — supersedes KB8R2: a glass wall stops it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `BEAM fire … length=3.639 clip=block tolerance=1.00 tested=1 [player <Ben> gap=4.200 miss] dealt=none`; no REPORT, no HURT, no TELEPORT line | K4 | READ: FIRE `tick=1843 … length=3.639 clip=block tolerance=1.00 tested=1 [player <Ben> gap=4.200 miss] dealt=none`; no REPORT, HURT or TELEPORT line. **MATCH** |

### KB9R3 — supersedes KB9R2: killed mid-telegraph, nothing fires; the resonance was already sent (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Both telegraph sound lines** (strike and resonance) and the TELEGRAPH at T; the kill 10 ticks after the force; **exactly one** `BEAM cancelled mob=knell id=<K> elapsed=<n>/20 reason=gone`, **1 ≤ n ≤ 19**, n RECORDED; **no fire sound line, no FIRE, REPORT, HURT or TELEPORT line** | I19; B3 | READ: both telegraph sound lines and the TELEGRAPH at `tick=1898`; `BEAM cancelled mob=knell id=<K> elapsed=8/20 reason=gone` (n = 8); no fire sound, FIRE, REPORT, HURT or TELEPORT line. **MATCH** |

### KB10R4 — supersedes KB10R3: the move 44 ticks after the fire, 50 degrees right (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| AT at K; the forced hit (FIRE at T + 20); **exactly one** `TELEPORT … tick=<FIRE + 44> from=<K> center=<F> radius=8.000 bearing=0.000 side=right first=50.000 tried=1 [right 50.000 ok y=<F.y> at=<F + (5.142, 0, 6.128)>] landed=<the same> moved=true`; the AT line there | K1, K4 | READ (F = -100.500 63.000 5.500 in this run): AT at K `-92.500 63.000 5.500`; FIRE `tick=2777` a hit; `TELEPORT … tick=2821 from=-92.500 63.000 5.500 center=-100.500 63.000 5.500 radius=8.000 bearing=0.000 side=right first=50.000 tried=1 [right 50.000 ok y=63.000 at=-95.358 63.000 11.628] landed=-95.358 63.000 11.628 moved=true` (2821 = 2777 + 44); AT there. **MATCH** |

### TP2R3 — supersedes TP2R2: 50 degrees left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 44, `side=left first=50.000 tried=1 [left 50.000 ok y=<F.y> at=<F + (5.142, 0, −6.128)>] … moved=true`; AT there | K1, K4 | READ: FIRE `tick=2917`; `TELEPORT … tick=2961 … side=left first=50.000 tried=1 [left 50.000 ok y=63.000 at=-95.358 63.000 -0.628] … moved=true`; AT there. **MATCH** |

### TP3R3 — supersedes TP3R2: the arc's near bound, 30 degrees right (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 44, `side=right first=30.000 tried=1 [right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; AT there | K1, K4 | READ: FIRE `tick=3057`; `TELEPORT … tick=3101 … side=right first=30.000 tried=1 [right 30.000 ok y=63.000 at=-93.572 63.000 9.500] … moved=true`; AT there. **MATCH** |

### TP4R3 — supersedes TP4R2: the arc's far bound, 70 degrees right (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 44, `side=right first=70.000 tried=1 [right 70.000 ok y=<F.y> at=<F + (2.736, 0, 7.518)>] … moved=true`; AT there | K1, K4 | READ: FIRE `tick=3197`; `TELEPORT … tick=3241 … side=right first=70.000 tried=1 [right 70.000 ok y=63.000 at=-97.764 63.000 13.018] … moved=true`; AT there. **MATCH** |

### TP5R3 — supersedes TP5R2: a miss moves nothing (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| AT at K; the forced beam; the step; `… gap=2.700 miss] dealt=none`; no REPORT, no HURT, **no TELEPORT line of any kind** through the 130 ticks; AT at K | K1 | READ: AT at K; FIRE `tick=3337 … gap=2.700 miss] dealt=none`; no REPORT, no HURT, no TELEPORT line of any kind; AT at K. **MATCH** |

**TP6R3 to TP11R3 re-read the walls, pits, steps and sight; each one's candidates and landing are predicted VERBATIM as
the row it supersedes read at the boot of `58c0f7a1` (`GATE-dragon-knell-2.md`)**, the move now at the FIRE + 44.

### TP6R3 — supersedes TP6R2: every right spot walled; it lands left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 44, `tried=6 [right 50.000 no-footing blocked=7 ungrounded=0, right 30.000 no-footing blocked=7 ungrounded=0, right 43.333 no-footing blocked=7 ungrounded=0, right 56.667 no-footing blocked=7 ungrounded=0, right 70.000 no-footing blocked=7 ungrounded=0, left 50.000 ok y=<F.y> at=<F + (5.142, 0, −6.128)>] … moved=true`; AT there | K1, K4 | READ: FIRE `tick=3477`; `TELEPORT … tick=3521 … tried=6 [right 50.000 no-footing blocked=7 ungrounded=0, right 30.000 …, right 43.333 …, right 56.667 …, right 70.000 no-footing blocked=7 ungrounded=0, left 50.000 ok y=63.000 at=-95.358 63.000 -0.628] … moved=true`; AT there. **MATCH** |

### TP7R3 — supersedes TP7R2: walled in everywhere; none, and it stays (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 44, `tried=10`, all ten `no-footing blocked=7 ungrounded=0`, `landed=none moved=-`; AT at K before and after | K1, K4 | READ: FIRE `tick=3617`; `TELEPORT … tick=3661 … tried=10 [` all ten `no-footing blocked=7 ungrounded=0` `] landed=none moved=-`; AT at K before and after. **MATCH** |

### TP8R3 — supersedes TP8R2: a pit two deep; 2 below his feet (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 44, `tried=1 [right 50.000 ok y=<F.y − 2> at=<F + (5.142, −2, 6.128)>] … moved=true`; AT there | K1, K4 | READ: FIRE `tick=3757`; `TELEPORT … tick=3801 … tried=1 [right 50.000 ok y=61.000 at=-95.358 61.000 11.628] … moved=true`; AT there. **MATCH** |

### TP9R3 — supersedes TP9R2: a step two high; 2 above his feet (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 44, `tried=1 [right 50.000 ok y=<F.y + 2> at=<F + (5.142, 2, 6.128)>] … moved=true`; AT there | K1, K4 | READ: FIRE `tick=3897`; `TELEPORT … tick=3941 … tried=1 [right 50.000 ok y=65.000 at=-95.358 65.000 11.628] … moved=true`; AT there. **MATCH** |

### TP10R3 — supersedes TP10R2: ground four down, refused; the next spot (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 44, `tried=2 [right 50.000 no-footing blocked=0 ungrounded=7, right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; AT there | K1, K4 | READ: FIRE `tick=4037`; `TELEPORT … tick=4081 … tried=2 [right 50.000 no-footing blocked=0 ungrounded=7, right 30.000 ok y=63.000 at=-93.572 63.000 9.500] … moved=true`; AT there. **MATCH** |

### TP11R3 — supersedes TP11R2: no sight, refused; the next spot (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 44, `tried=2 [right 50.000 no-sight y=<F.y>, right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; AT there | K1, K4 | READ: FIRE `tick=4177`; `TELEPORT … tick=4221 … tried=2 [right 50.000 no-sight y=63.000, right 30.000 ok y=63.000 at=-93.572 63.000 9.500] … moved=true`; AT there. **MATCH** |

### TP12R3 — supersedes TP12R2: he steps 2 north during the delay (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit at F; `CMD /tp @s ~ ~ ~-2` 40 ticks after the force (after the fire, before the move); `TELEPORT … tick=<FIRE + 44> center=<F + (0, 0, −2)> radius=8.246 bearing=14.036 side=right first=50.000 tried=1 [right 50.000 ok … at=<F + (3.610, 0, 5.414)>] … moved=true`; AT there | K1, K4 | READ: FIRE `tick=4317` at F; `CMD /tp @s ~ ~ ~-2`; `TELEPORT … tick=4361 … center=-100.500 63.000 3.500 radius=8.246 bearing=14.036 side=right first=50.000 tried=1 [right 50.000 ok y=63.000 at=-96.890 63.000 10.914] … moved=true` (F + (3.610, 0, 5.414)); AT there. **MATCH** |

### TP13R3 — supersedes TP13R2: the Knell killed during the delay (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; `CMD /kill @e[tag=tp13r3]` 35 ticks after the force; **exactly one** `TELEPORT skipped mob=knell id=<K> target=<Ben> reason=knell-gone`; no decision line | K1 | READ: FIRE `tick=4457`, a hit; `CMD /kill @e[tag=tp13r3]`; exactly one `TELEPORT skipped mob=knell id=<K> target=<Ben> reason=knell-gone`; no decision line. **MATCH** |

### TP14R2 — supersedes TP14: a beam forced while a move is pending (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The first forced hit (FIRE₁ at T₁ + 20, one HURT). (2) the heal 25 ticks after the first force. (3) the second force 30 ticks after the first: its two telegraph sound lines, TELEGRAPH₂ (T₂ = T₁ + 30), its chime, FIRE₂ at T₂ + 20 `… gap=0.000 hit] dealt=65.000`, REPORT, **exactly one** `TELEPORT ignored mob=knell id=<K> reason=pending`, one HURT. (4) **exactly one** decision, at **FIRE₁ + 44** (not FIRE₂ + 44), `side=right first=50.000 … at=<F + (5.142, 0, 6.128)> … moved=true`; AT there | K5 | READ: (1) TELEGRAPH₁ `tick=4567`, FIRE₁ `tick=4587` hit, one HURT. (2) the heal. (3) TELEGRAPH₂ `tick=4597 lived=36` (T₁ + 30) with both sounds, FIRE₂ `tick=4617 … gap=0.000 hit] dealt=65.000`, REPORT, `TELEPORT ignored mob=knell id=<K> reason=pending`, one HURT. (4) exactly one decision, `tick=4631` = **FIRE₁ + 44** (FIRE₂ + 44 would be 4661), `side=right first=50.000 tried=1 [right 50.000 ok y=63.000 at=-95.358 63.000 11.628] … moved=true`; AT there. **MATCH** |

### KB11R4 — supersedes KB11R3: the live cycle, by bound, unbroken by a move (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) both spawns, `REPLY "The base value for attribute Speed for entity Knell set to 0" []`, the draw. (2) the first own TELEGRAPH `forced=false`, **99 ≤ lived ≤ 201**, at K, `ticks=20`, its two sound lines at its tick. (3) its FIRE 20 later, a hit; one HURT. (4) **a TELEPORT at that FIRE + 44**, `side=right first=50.000 … landed=<F + (5.142, 0, 6.128)> moved=true`. (5) **THE CLOCK UNBROKEN:** each next own TELEGRAPH's `lived` − the previous one's − 20 **within 100 to 200**, with the previous hit's move between them. (6) each later hit's move at its FIRE + 44, the production draw, RECORDED, `radius=8.000`. (7) no line names the frozen Knell's uuid but its spawn. (8) between two heals at most one HURT. (9) the kills; anything pending cancelled inside the row, RECORDED | B9, I1, I2, I23; K1 | READ: (1) both spawns, the speed REPLY, the draw. (2) first own TELEGRAPH `forced=false tick=4845 lived=154` (inside 99 to 201) at K, `ticks=20`, both sounds at its tick. (3) FIRE `tick=4865` hit; one HURT. (4) `TELEPORT … tick=4909` (+44) `side=right first=50.000 … landed=-95.358 63.000 11.628 moved=true`. (5) next own TELEGRAPH `tick=5018 lived=327`: 327 − 154 − 20 = **153**; FIRE `tick=5038` hit; `TELEPORT … tick=5082` (+44) `bearing=50.000 side=left first=67.542 … landed=-92.872 63.000 3.089`; third own TELEGRAPH `tick=5140 lived=449`: 449 − 327 − 20 = **102**; FIRE `tick=5160` hit; `TELEPORT … tick=5204` (+44) `bearing=-17.542 side=right first=49.359 … landed=-93.702 63.000 9.718`, each `radius=8.000`, the production draw. (7) the frozen Knell's uuid on its spawn line only. (8) heals at 06:14:51, :56, 06:15:01, :06, :11, :16; HURTs at 06:14:59, 06:15:08, :14, one between each pair. (9) the kills; nothing was pending (the third move had landed). **MATCH** |

---

## BEN'S ROWS — BY THE ASK STEP, SURVIVAL, DIFFICULTY NORMAL

**AUTO-PARTIAL:** the BEAM, sound and TELEPORT lines are the server's half (what it SENT); what he heard and when it
became audible is his half, and only his answers read it. **His answers are his ear, never a measurement** of which
variant his client drew or of "playback speed equals pitch".

### KQ1R3 — supersedes GATE-dragon-knell-2.md KQ1R2: the shot as the ringing becomes audible; the 1 second (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Server half:** a heal; the gold line; the first forced beam from a frozen Knell 12 east, the strike and the resonance at its lock, the chime at the fire 20 later; a hit if he stood still (`dealt=65.000`, one HURT); its move at the FIRE + 44 (the production draw, `radius=12.000`, RECORDED). ASK 1; a heal; the second gold line; 100 ticks; the second forced beam, its outcome **his doing, RECORDED**; ASK 2; the kill (a pending move then skipped, RECORDED). **His half, yes/no:** ASK 1 *"Did the beam fire as the ringing became audible?"*; ASK 2 *"With 1 second, could you still step out of the way in time?"* | K7; Ben's ruling | READ, server half: the heal; the gold line; TELEGRAPH `tick=2033 lived=61 at=-90.500 63.000 6.500` with both sounds; the chime and FIRE `tick=2053 … length=18.001 … [player <Ben> gap=0.000 hit] dealt=65.000`; one HURT; `TELEPORT … tick=2097` (+44) `radius=12.000 bearing=0.000 side=left first=65.720 tried=1 [left 65.720 ok y=63.000 at=-97.566 63.000 -4.439] … moved=true`. The second force from that landing: TELEGRAPH `tick=2355`, FIRE `tick=2375 … [player <Ben> gap=1.949 miss] dealt=none`: **he stepped out of it**, so no second HURT, no move, no skip line. His half, verbatim: `ASK 1 question="Did the beam fire as the ringing became audible?" answer="yes" ticks=182`; `ASK 2 question="With 1 second, could you still step out of the way in time?" answer="yes" ticks=55` |

### TQ1R3 — supersedes GATE-dragon-knell-2.md TQ1R2: the move a second before the ringing fades; the whole (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Server half:** a heal; the gold line; one forced beam from a frozen Knell 12 east, a hit, the two telegraph sounds and the chime; **one TELEPORT at the FIRE + 44**, `radius=12.000 bearing=0.000`, the production draw, RECORDED. **His half, yes/no:** ASK 1 *"Did the Knell move about a second before the ringing faded?"*; ASK 2 *"Does the whole sequence feel timed right now?"* | K1, K7; the seat's 44 (by variant 0.58, 1.00 or 1.25 s before the sound ends) | READ, server half: the heal; the gold line; TELEGRAPH `tick=5362` with both sounds; the chime and FIRE `tick=5382 … hit] dealt=65.000`; one HURT; `TELEPORT … tick=5426` (+44) `radius=12.000 bearing=0.000 side=left first=58.513 tried=1 [left 58.513 ok y=63.000 at=-94.232 63.000 -4.733] … moved=true`. His half, verbatim: **`ASK 1 question="Did the Knell move about a second before the ringing faded?" answer="it moved about a second after the rining stopped" ticks=203`**; **`ASK 2 question="Does the whole sequence feel timed right now?" answer="not quite" ticks=105`**. Neither is yes or no: recorded as his words, his ear |
