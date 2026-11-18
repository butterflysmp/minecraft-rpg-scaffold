# GATE — THE KNELL, 5: THE SHOT IN THE MIDDLE (24 AND 24), AND THE FIRST BELL BACK TO 1.0

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-knell-5`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the content AND this gate, and
Ben says ready.**

**2026-11-17: approved to merge by the review seat after the boot of 07d06837.**

> **2026-11-17, `feat/void-implosion`: NO ROW OF THIS GATE IS SUPERSEDED.** F19 (the landing search anchored on the
> target's fractional feet) is fixed in the shared `BeamTeleport.foot`, and its first server reading is
> `GATE-void-implosion.md` KS1, a NEW row (a target standing on a slab), in the blast gate's file. The audit there
> (*THE ROWS ALREADY MERGED*) finds no line of KB10R6, TP2R5-TP14R4, KB11R6 or TQ1R5 changed: every target here stands
> on a whole-block floor, where floor(y) = y. **Every prediction and reading above stands as written.**

> **READ, 2026-11-17: booted at `07d06837` on the seat's approval and Ben's "ready".** Server log `08:14:24` to the
> clean `/stop` at `08:18:55`. Carried state: no JVM; backup `rpg-run-backups/2026-11-17-pre-dragon-knell-5`
> (`run/plugins/Rpg` 129 files, 387488 bytes; `run/world` 193 files, 204963724 bytes; `diff -rq lines: 0` each);
> `12:  dev-pack-on-join: true`. Ben logged in at `08:14:32`, typed `/gamemode survival` first (`08:14:57`), then each
> selftest once, each to `SELFTEST RUN FINISHED`, then `/stop`.
>
> **The result: every AUTO row AS PREDICTED, 28 of 28, RE1 and the re-purposed TP14R4 included.** No run was stopped,
> so the server-stop restore stays UNREAD on a server.
>
> **Ben's two answers: "yes" (the shot is in the middle now, the way he wanted); and, to "Does the whole sequence feel
> right now?", "it's almost there. The second purple line is out of place. remove it other than that everything is
> perfect"**, his words, not yes or no, recorded verbatim (the second purple line is `flash_at`'s tick 18).
>
> **Counts:** 28 TELEGRAPH lines, each with exactly two sound lines at its tick, `block.bell.use pitch=1.00` then
> `block.bell.resonate`; 27 FIRE lines (the 28th telegraph is KB9R5's, cancelled `elapsed=8/24`), each with exactly two,
> `block.bell.use pitch=1.50` then `block.amethyst_block.chime`; **every fire 24 ticks after its telegraph (27 of
> 27)**; 17 TELEPORT decisions, **every one 24 ticks after the fire of the hit that scheduled it (17 of 17)**; 5
> `skipped … knell-gone`, each inside its own row; **no `ignored … pending` line** (unreachable at 24 and 24). 22 hits
> and 22 HURT lines, each `before=65.000 after=59.091 defense=10.000 dealerType=wither_skeleton/knell`: no hit from
> anything but a beam. Every LEFTOVER line `living=0`. Both runs: `DIFFICULTY found=NORMAL set=NORMAL`, `RESTORED …
> difficulty=NORMAL`, no `ABORTED` or `NOT-RESTORED` line.

**What this gate re-predicts, and why.** Ben, after the boot of `0b4037ec` (`PLAN-dragon-mobs.md`, RULINGS): *"the timing
of the warning and teleport are good, I want the shot to be in the middle"*, and *"no"* to the lower first bell. The
seat's reading, told to him before the brief and built here: **the lock and the move stay 48 ticks apart, the fire at
their middle: a 24-tick telegraph (was 20) and a 24-tick teleport delay (was 28), a pair summing to 48**; and the
telegraph's bell **back to pitch 1.0**. The shot's bell (1.5) and the chime are unchanged. **Every row of both Knell
scenario files changes** (the telegraph's length and its bell line print in every beam row), so every row is
re-predicted HERE under the next suffix (KB1R4 → KB1R5, WA1R2 → WA1R3, KQ1R4 → KQ1R5; KB10R5 → KB10R6, TP2R4 → TP2R5,
TP14R3 → TP14R4, KB11R5 → KB11R6, TQ1R4 → TQ1R5). **Both files are paired with THIS gate** (`SelfTestScenariosTest.docOf`;
the note in `selftest/index.yml`). The old rows' predictions and readings stay in `GATE-dragon-knell-4.md`, under a
dated note.

> **TP14 CHANGES WHAT IT TESTS, AND THIS IS THE SEAT'S TO RULE.** Its old purpose (a second hit while a move is pending
> is ignored) is **UNREACHABLE at 24 and 24**: a forced second beam is accepted only after the first fire (a force is
> refused during a telegraph, `MobBeamer#force`), its telegraph begins a tick later, and it fires 24 after that, so the
> second fire is at least **25** ticks after the first, while the first move lands **24** after it. No beam, forced or
> the Knell's own, can hit while a move is pending. **`TELEPORT ignored … reason=pending` is now unreachable in shipped
> content**; its only exercise is `MobTeleporter`'s unit tests. TP14R4 instead reads that **the pending move is cleared
> when it lands**: a second hit after the first move schedules a second move.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL.
- **The difficulty is set by the selftest:** each gate's setup `difficulty: normal`; `DIFFICULTY found=… set=NORMAL`;
  `RESTORED … difficulty=<what it found>`.
- **Gear score 100 on every Knell:** a hit is **65.000** before defence.

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC, ROW BY ROW

**The floor:** his maximum is at least 100 (it read **350**). **65 before defence leaves at least 35**; every row heals him
before every beam that can land, and no row lands two beams between heals. **No Knell can reach him:** every Knell is
frozen (NoAI) except KB11R6's (walking speed base 0, 8 blocks away) and the two walking Knells of KB2R5 and WA1R3,
killed before they could close to melee (K4).

| row | what can hit him after a heal | the worst case, before defence |
|---|---|---|
| KB1R5, KB4R5, KB7R5, KB2R5, KQ1R5 | one forced beam that hits | **65** |
| KB3R5, KB5R5, KB8R5, WA1R3 | one forced beam that misses | **0** predicted, 65 if it hit |
| KB6R5, KB9R5 | none (out of range; killed mid-telegraph) | **0** |
| KB10R6, TP2R5-TP4R5, TP6R5-TP13R5, TQ1R5 | one forced beam that hits | **65** |
| TP5R5 | a miss | **0** |
| TP14R4 | two forced beams, **a heal between** (55 ticks after the first force, after the first move) | **65**, then **65** |
| KB11R6 | its own beams, at least 124 ticks apart (a 100-tick minimum wait after a fire, then a 24-tick telegraph), a heal every 100 ticks | **65 between two heals** |

## WHAT THE ROWS STAND ON

- **K1. The timings, READ at the boots of `9d0cbffb` and `0b4037ec`:** every fire exactly `telegraph_ticks` after its
  telegraph (29 of 29, 27 of 27), every move exactly `delay_ticks` after the fire of the hit that scheduled it (17 of
  17, 16 of 16). Only the numbers move: **24 and 24** (`knell.yml`; `MobLoaderTest.theBundledKnellBeamsInTheMiddle`,
  `theBundledKnellsDelayIsSeventySixTicksShorterThanItsCycle`).
- **K2. The sounds, READ at `0b4037ec`:** each list plays in order, each sound logging one `BEAM sound` line at its tick,
  the telegraph's before the TELEGRAPH line and the fire's before the FIRE line (28 of 28, 27 of 27). Only the
  telegraph's bell pitch moves, to **1.00**.
- **K3. The geometry, READ at `9d0cbffb` and `0b4037ec`:** every landing, wall, pit, step and sight line, relative to
  Ben's feet F and a frozen Knell's K = F + (8, 0, 0); the beam rows' gaps, lengths and directions. **None depends on
  the timings or the sounds.**
- **K4. THE WALKING KNELLS, recomputed for a 24-tick root.** READ and EXECUTED (`GATE-dragon-knell-3.md` K2: sight in
  the open, a 15-block start inside the 16-block follow range, a steady 0.094703 blocks a tick, about 12 blocks needed
  to melee; the same recurrence, run in `awk` for this gate):
  - **KB2R5:** live at most **107 ticks** (wait 60, AT, wait 5, AT, force, wait 40, AT, kill; 2 of spawning), rooted
    **24**: at most **83 ticks, 7.86 blocks**. **Margin 4.14 blocks, 44 ticks.**
  - **WA1R3:** its read MOVED from the force + 51 to the force + **55** (the wait after the step 48 → **52**), so it
    stays about 30 ticks after the fire (the fire now at the force + 25 or 26). Live at most **122 ticks**, rooted
    **24**: at most **98 ticks, 9.28 blocks**. **Margin 2.72 blocks, 29 ticks.** Ben's step south keeps him inside the
    follow range (sqrt(15² + 3²) = 15.30 < 16).
  - **The walk after a beam** resumes at most 21 ticks after the fire (INFERRED, `GATE-dragon-knell-3.md` K3); WA1R3's
    read is at least 8 ticks past that, **at least 0.645 blocks** from rest (executed), against a predicted **0.20**.
    READ twice: drops of 1.562 and 1.614 blocks.
- **K5. A force is refused during a telegraph** (`MobBeamer#force`: `busy: telegraph n/24`), READ in our code; accepted
  after the fire. **A move lands, then the pending entry is gone** (`MobTeleporter`, the one-shot task's end), READ in
  our code; TP14R4 is its first server reading at these numbers.

## THE ROW AUDIT: EVERY STEP TIMED AGAINST THE OLD 20 OR 28, AND WHERE EVERY TELEPORT LINE LANDS

**A forced beam's telegraph comes one or two ticks after the BEAM step (the force), so its fire is at the force + 25
or 26, and a hit's move at the force + 49 or 50** (the same as before: 24 + 24 = 20 + 28 = 48). The same offset
applies to every force in a row.

| row | the step | at 24 / 24 | changed? |
|---|---|---|---|
| KB1R5, KB4R5, KB7R5, KB2R5 (hits) | the kill at the force + 40 | after the fire (+ 25 or 26), **9 or 10 ticks before the move**: one skip line inside the row | no |
| KB3R5, KB4R5, KB5R5, WA1R3, TP5R5 (dodges) | the step 3 ticks after the force | inside the telegraph (which now runs to + 25 or 26) | no |
| KB9R5 | the kill 10 ticks after the force | mid-telegraph, `elapsed` 1 to 23 of 24 | no |
| KB2R5 | rooted for the telegraph | **24 ticks** (was 20); the arithmetic recomputed (K4); the read at + 40 is fire + 14 or 15, recorded | arithmetic only |
| **WA1R3** | the read 30 ticks after the fire | at + 51 it would have been only 25 or 26 after the fire | **RE-TIMED: the wait 48 → 52**, the read at + 55 (fire + 29 or 30), at least 8 ticks past K4's bound |
| KQ1R5 | the ASKs after a 60-tick wait | the move at + 49 or 50 lands before the ASKs; the kill after them, no skip | no |
| KB10R6, TP2R5-TP4R5, TP6R5-TP11R5 | the read at + 130 | the move at + 49 or 50 | no |
| TP12R5 | the step 40 ticks after the force | after the fire, **9 or 10 ticks before the move** | no |
| TP13R5 | the kill 35 ticks after the force | after the fire (+ 25 or 26), **14 or 15 ticks before the move** | no |
| **TP14R4** | the second force | at + 23 it would be refused (mid-telegraph); no time exists at which its fire precedes the first move (header) | **RE-TIMED: the heal and the second force at + 55**, after the first move (+ 49 or 50) by **5 or 6 ticks**; its fire at + 80 or 81, its move at + 104 or 105, the read at + 145 |
| KB11R6 | its own cycle | each beam at least 124 ticks after the last, each move 24 after its fire | no |
| TQ1R5 | the ASK after a 130-tick wait | the move at + 49 or 50 lands before it | no |
| merged | DB1R2, DB11R2, WS-C, WS2c | never (frozen, or no Knell) | no |
| closed manual gates (13) | a live Knell's own beam | now a 24-tick warning, the bell at 1.0, and a move 24 after a hit | **a finding, carried**; not re-predicted |

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-dragon-knell-5"; [ -e "$B" ] && B="$B-2"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   Report the folder, both pairs of counts, and `diff -rq lines: 0` each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report.
4. **Boot this branch's head:** `./scripts/dev-server.sh --refresh-content`, announcing "booting now", on Ben's "ready".
5. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` line names this branch's head. **Readings count
   only after Ben's LOGIN in that boot's log.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest dragon-beam` — **ONCE. Wait for the chat to say the run finished, and answer every gold question,
   before the next command.** About **1 minute** plus the time you take to answer (its waits sum to 0.62 minutes; the
   same eleven rows took 59 seconds at the boot of `0b4037ec`, answers included). Stand still and touch nothing. **In
   its last part** a Knell twelve blocks east beams you once: **the bell and the ringing at the warning, the higher bell
   at the shot, and the Knell moving as long again after it.** One question; answer `yes` or `no`.
3. `/rpg selftest dragon-teleport` — **ONCE. Wait for the chat to say the run finished, and answer every gold
   question, before the next command.** About **2½ minutes** plus the time you take to answer (its waits sum to 2.19
   minutes; the same sixteen rows took 165 seconds at that boot, answers included). Stand still and touch nothing. **In
   its last part** a Knell twelve blocks east beams you once, the same sequence. One question; answer `yes` or `no`.
4. `/stop`

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-knell-5` prints it, shortened. **Not** master's `2d7b593d`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1`, after this boot's `Done` | READ 2026-11-17: `[08:14:24] [Server thread/INFO]: [Rpg] Build: 07d06837`, this PR's head (`git ls-remote` `07d06837e465…`). **MATCH** |

### R0b — the jar carries 24 and 24 and the bell at 1.0

| prediction | instrument | READING |
|---|---|---|
| In `content/mobs/knell.yml`: PRESENT `  telegraph_ticks: 24`, `  flash_at: [0, 18]`, `  delay_ticks: 24`, and **the telegraph list holding the bell at pitch 1.0 then the resonance**; ABSENT `  telegraph_ticks: 20`, `  flash_at: [0, 15]`, `  delay_ticks: 28`, `      - { key: block.bell.use, volume: 1.0, pitch: 0.8 }`. Keys: `GATE-dragon-beam.yml` `KB1R5:`, `WA1R3:` PRESENT, `KB1R4:` ABSENT; `GATE-dragon-teleport.yml` `TP14R4:`, `KB10R6:` PRESENT, `TP14R3:` ABSENT. **The controls, the same on both builds:** `knell.yml` PRESENT `      - { key: block.bell.use, volume: 1.0, pitch: 1.5 }`; `scheduler/InlineScheduler.class` PRESENT; `GATE-dragon-blast.yml` `BQ1R2:` PRESENT | the scan below | READ: all 17 lines as predicted: `telegraph_ticks: 24`, `flash_at: [0, 18]`, `delay_ticks: 24` and the telegraph list (bell 1.0, then the resonance) PRESENT; 20, [0, 15], 28 and the 0.8 bell ABSENT; `KB1R5:`, `WA1R3:`, `TP14R4:`, `KB10R6:` PRESENT, `KB1R4:` and `TP14R3:` ABSENT; the three controls PRESENT. **MATCH** |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$k = Bytes 'content/mobs/knell.yml'
foreach ($l in '  telegraph_ticks: 24', '  flash_at: [0, 18]', '  delay_ticks: 24', '  telegraph_ticks: 20', '  flash_at: [0, 15]', '  delay_ticks: 28', '      - { key: block.bell.use, volume: 1.0, pitch: 0.8 }', '      - { key: block.bell.use, volume: 1.0, pitch: 1.5 }') {
  if ($k -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT knell.yml '$l'" } else { "ABSENT  knell.yml '$l'" }
}
if ($k -match '(?m)^    telegraph:\r?\n      - \{ key: block\.bell\.use, volume: 1\.0, pitch: 1\.0 \}\r?\n      - \{ key: block\.bell\.resonate, volume: 1\.4, pitch: 1\.0 \}') { "PRESENT knell.yml telegraph = bell 1.0, resonance" } else { "ABSENT  knell.yml telegraph = bell 1.0, resonance" }
if ((Bytes 'io/github/butterflysmp/rpg/paper/scheduler/InlineScheduler.class') -ne $null) { "PRESENT scheduler/InlineScheduler.class" } else { "ABSENT  scheduler/InlineScheduler.class" }
foreach ($q in @(@('dragon-beam', 'KB1R5'), @('dragon-beam', 'WA1R3'), @('dragon-beam', 'KB1R4'), @('dragon-teleport', 'TP14R4'), @('dragon-teleport', 'KB10R6'), @('dragon-teleport', 'TP14R3'), @('dragon-blast', 'BQ1R2'))) {
  $y = Bytes "selftest/GATE-$($q[0]).yml"
  if ($y -eq $null) { "MISSING selftest/GATE-$($q[0]).yml" } elseif ($y -match "(?m)^$($q[1]):\s*$") { "PRESENT $($q[0]) $($q[1]):" } else { "ABSENT  $($q[0]) $($q[1]):" }
}
$zip.Dispose()
```

**A jar of master `2d7b593d` reads the opposite on the seven `knell.yml` lines that move and on the list check, ABSENT
on the four new keys and PRESENT on `KB1R4:` and `TP14R3:`**; the three controls read PRESENT on both.

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim the boot of `0b4037ec`'s. No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN` beyond `volley_stone`'s known pair. **A `Skipping malformed mob 'knell.yml'` line would mean the beam or teleport block was refused** (`4 mobs`) | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | READ: the Loaded line verbatim as predicted; no `Refusing`, `Skipping`, `SEVERE` or `Exception` in the whole log; the only `[Rpg]` WARN lines are `volley_stone`'s known pair (and Paper's own update banner, not ours). **MATCH** |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 117 scenarios across 17 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport]`**, and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | READ: `[Rpg] Selftest: dev server, 117 scenarios across 17 gates [melee-cell, … dragon-beam, dragon-teleport]`, the list verbatim; no `Refusing selftest`. **MATCH** |

**The 117 is RECOUNTED from the files at this branch's head:** in `paper/src/main/resources/selftest/`,
`grep -cE '^  - ' index.yml` prints **17**; `cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/;
s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints **134**; the same with `'^setup:'` prints **17**: 134 − 17 = **117**.
**This PR renames rows and adds none, so master `2d7b593d` prints the same 117; R0b's keys are the control.**

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument:** `Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-(beam|teleport)|SELFTEST RUN|\[Rpg\] (BEAM|TELEPORT)'`,
after Ben's LOGIN, inside each row's own `START … DONE` bracket.

**In EVERY row (not repeated):** `LEFTOVER living=0 radius=16` after its CLEAN; each spawn's `DRIVE spawn tag=<tag>
<uuid> knell frozen` (or `live`) and its `REPLY "Spawned Knell (wither_skeleton, 360 HP, GS 100 given)" [aqua, gray]`;
each `DRIVE beam` with no `refused=`; each `CMD /kill @e[tag=<tag>]` followed by `SETTLED dying=0 ticks=<t> first=1`.
**No `[Rpg]` line names a Knell that is not the row's own.**

**EVERY TELEGRAPH, at its tick T, is preceded by exactly two sound lines, in this order:**
`[Rpg] BEAM sound mob=knell id=<K> when=telegraph tick=<T> key=block.bell.use volume=1.00 pitch=1.00`, then
`… when=telegraph tick=<T> key=block.bell.resonate volume=1.40 pitch=1.00`.
**EVERY FIRE, exactly 24 ticks later, is preceded by exactly two, in this order:**
`… when=fire tick=<T+24> key=block.bell.use volume=1.00 pitch=1.50`, then
`… when=fire tick=<T+24> key=block.amethyst_block.chime volume=1.00 pitch=1.00`.

**A FORCED HIT from a frozen Knell 8 east:** the two telegraph sounds and `BEAM telegraph … tick=<T> … dir=-0.961705
-0.274086 0.000000 range=40.00 ticks=24`; the two fire sounds and `BEAM fire … tick=<T+24> … length=12.259 clip=block
tolerance=1.00 tested=1 [player <Ben> gap=0.000 hit] dealt=65.000`; one REPORT; `HURT before=65.000 after=59.091
defense=10.000 dealer=<K> dealerType=wither_skeleton/knell`. **A MOVE** is `[Rpg] TELEPORT … tick=<FIRE + 24> …`, 48
after the lock.

### KB1R5 — supersedes GATE-dragon-knell-4.md KB1R4: a forced hit at the new length (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit's lines exactly as above (the fire at T + 24). Then the kill, **exactly one** `TELEPORT skipped mob=knell id=<K> target=<Ben> reason=knell-gone`, then `SETTLED`; no decision line | K1, K2, K3 | READ (F = -100.500 63.000 6.500 in both runs): both telegraph sounds (bell 1.00, then the resonance) and both fire sounds (bell 1.50, then the chime) at their ticks; TELEGRAPH `tick=737 … ticks=24`; FIRE `tick=761 … length=12.259 clip=block tolerance=1.00 tested=1 [player <Ben> gap=0.000 hit] dealt=65.000` (737 + 24); one REPORT; one HURT `before=65.000 after=59.091 defense=10.000`; the kill, one `TELEPORT skipped … reason=knell-gone`, then `SETTLED`. **MATCH** |

### KB2R5 — supersedes KB2R4: the root on a walking Knell, 24 ticks (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (0) `knell live`, 15 east. (1) **THE CONTROL:** two AT lines 5 ticks apart, the second's x < the first's − 0.01, or the row is HOLLOW. (2) the two telegraph sounds and one TELEGRAPH `forced=true ticks=24`, its `at=` RECORDED. (3) the FIRE **24** later, its `at=` the TELEGRAPH's on all three coordinates. (4) the two fire sounds; `[player <Ben> gap=0.000 hit] dealt=65.000`, `length` and `dir` RECORDED; one REPORT; one HURT. (5) the AT 40 ticks after the force (fire + 14 or 15): RECORDED. (6) the kill, one skip line. (7) no HURT but the beam's | K2, K4 | READ: (1) **CONTROL PASSED:** `x=-88.108`, then `x=-88.465`. (2) both telegraph sounds (bell 1.00, then the resonance) and both fire sounds (bell 1.50, then the chime) at their ticks; TELEGRAPH `tick=872 lived=66 at=-88.536 63.000 7.000 … ticks=24`. (3) FIRE `tick=896 at=-88.536 63.000 7.000`, **the same: ROOTED for the 24.** (4) `length=17.963 … [player <Ben> gap=0.000 hit] dealt=65.000`, `dir=-0.981494 -0.187049 -0.041020`; one REPORT; one HURT. (5) RECORDED: the AT 40 ticks after the force (fire + 14 or 15) `x=-88.536 y=63.000 z=7.000`, where it fired (inside K4's bound). (6) one skip line. (7) no HURT but the beam's. **MATCH** |

### WA1R3 — supersedes WA1R2, RE-TIMED: the Knell walks again after a beam (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (0) `knell live`, 15 east. (1) **THE CONTROL**, or HOLLOW. (2) the two telegraph sounds, one TELEGRAPH at T. (3) `CMD /tp @s ~ ~ ~3`. (4) the two fire sounds and FIRE `tick=<T+24>`, `at=` the TELEGRAPH's, **`[player <Ben> gap=<g> miss] dealt=none`, g ≥ 2.0**; no REPORT, no HURT, **no TELEPORT line**. (5) **THE WALK:** the AT **55** ticks after the force (fire + 29 or 30); its horizontal distance from F + (0, 0, 3) **less than the FIRE's `at=` distance by more than 0.20**. (6) the kill; no TELEPORT line. (7) no HURT | K2, K4 | READ: (1) **CONTROL PASSED:** `x=-88.024`, then `x=-88.381`. (2) both telegraph sounds (bell 1.00, then the resonance) and both fire sounds (bell 1.50, then the chime) at their ticks; TELEGRAPH `tick=1007 lived=66 at=-88.452 63.000 7.002`. (3) the step. (4) FIRE `tick=1031 at=-88.452 63.000 7.002` (rooted) `… [player <Ben> gap=2.580 miss] dealt=none`; no REPORT, HURT or TELEPORT line. (5) **THE WALK:** the AT 55 ticks after the force `x=-88.892 y=63.000 z=7.803`; from F + (0, 0, 3) = (-100.500, 9.500): **12.304 at the fire, 11.731 at fire + 30, a drop of 0.573 > 0.20** (executed). (6) the kill; no TELEPORT line. (7) no HURT. **MATCH** |

### KB3R5 — supersedes KB3R4: the line fixed at the lock; 3 south, missed (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The telegraph's lines; `CMD /tp @s ~ ~ ~3`; the fire's sounds; `BEAM fire … tick=<T+24> … [player <Ben> gap=2.700 miss] dealt=none`; no REPORT, HURT or TELEPORT line | K3 | READ: TELEGRAPH `tick=1097`; FIRE `tick=1121 … [player <Ben> gap=2.700 miss] dealt=none`; no REPORT, HURT or TELEPORT line. **MATCH** |

### KB4R5 — supersedes KB4R4: 1.2 south, still hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `… gap=0.900 hit] dealt=65.000`; one REPORT; one HURT; after the kill one `TELEPORT skipped … knell-gone`, inside this row | K3 | READ: FIRE `tick=1196 … gap=0.900 hit] dealt=65.000`; one REPORT; one HURT; one `TELEPORT skipped … knell-gone` inside this row. **MATCH** |

### KB5R5 — supersedes KB5R4: 1.4 south, missed (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `… gap=1.100 miss] dealt=none`; no REPORT, HURT or TELEPORT line | K3 | READ: FIRE `tick=1271 … gap=1.100 miss] dealt=none`; no REPORT, HURT or TELEPORT line. **MATCH** |

### KB6R5 — supersedes KB6R4: beyond the range, nothing (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `BEAM none mob=knell id=<K> forced=true tick=<t> range=40.00 reason=no-target`; **no BEAM sound line**, no TELEGRAPH, no FIRE; `AT …` 41.000 east of F; no TELEPORT line | K3 | READ: `BEAM none mob=knell id=<K> forced=true tick=1322 range=40.00 reason=no-target`; no sound, TELEGRAPH or FIRE line; `AT … x=-59.500 y=63.000 z=6.500` (41.000 east of F); no TELEPORT line. **MATCH** |

### KB7R5 — supersedes KB7R4: a husk in the line keeps its health (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| the husk `customHP=100.000/100.000` before and after; `BEAM fire … tested=2 [player <Ben> gap=0.000 hit, husk <uuid> gap=0.144 spared] dealt=65.000`; one REPORT; one HURT; one skip line | K3 | READ: the husk `customHP=100.000/100.000` before and after; FIRE `tick=1401 … tested=2 [player <Ben> gap=0.000 hit, husk <uuid> gap=0.144 spared] dealt=65.000`; one REPORT; one HURT; one skip line. **MATCH** |

### KB8R5 — supersedes KB8R4: a glass wall stops it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `BEAM fire … length=3.639 clip=block tolerance=1.00 tested=1 [player <Ben> gap=4.200 miss] dealt=none`; no REPORT, HURT or TELEPORT line | K3 | READ: FIRE `tick=1476 … length=3.639 clip=block tolerance=1.00 tested=1 [player <Ben> gap=4.200 miss] dealt=none`; no REPORT, HURT or TELEPORT line. **MATCH** |

### KB9R5 — supersedes KB9R4: killed mid-telegraph, nothing fires (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The two telegraph sound lines and the TELEGRAPH at T; the kill 10 ticks after the force; **exactly one** `BEAM cancelled mob=knell id=<K> elapsed=<n>/24 reason=gone`, **1 ≤ n ≤ 23**, n RECORDED; no fire sound line, no FIRE, REPORT, HURT or TELEPORT line | K2 | READ: both telegraph sound lines and the TELEGRAPH at `tick=1527`; `BEAM cancelled mob=knell id=<K> elapsed=8/24 reason=gone` (n = 8); no fire sound line, no FIRE, REPORT, HURT or TELEPORT line. **MATCH** |

### KB10R6 — supersedes KB10R5: the move 24 ticks after the fire (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| AT at K; the forced hit; **exactly one** `TELEPORT … tick=<FIRE + 24> from=<K> center=<F> radius=8.000 bearing=0.000 side=right first=50.000 tried=1 [right 50.000 ok y=<F.y> at=<F + (5.142, 0, 6.128)>] landed=<the same> moved=true`; the AT line there | K1, K3 | READ: AT at K `-92.500 63.000 6.500`; FIRE `tick=1968` a hit; `TELEPORT … tick=1992 from=-92.500 63.000 6.500 center=-100.500 63.000 6.500 radius=8.000 bearing=0.000 side=right first=50.000 tried=1 [right 50.000 ok y=63.000 at=-95.358 63.000 12.628] landed=-95.358 63.000 12.628 moved=true` (1992 = 1968 + 24); AT there. **MATCH** |

### TP2R5 — supersedes TP2R4: 50 degrees left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 24, `side=left first=50.000 tried=1 [left 50.000 ok y=<F.y> at=<F + (5.142, 0, −6.128)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=2108`; `TELEPORT … tick=2132 … side=left first=50.000 tried=1 … landed=-95.358 63.000 0.372 moved=true` (F + (5.142, 0, −6.128)); AT there. **MATCH** |

### TP3R5 — supersedes TP3R4: 30 degrees right (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 24, `side=right first=30.000 tried=1 [right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=2248`; `TELEPORT … tick=2272 … side=right first=30.000 tried=1 … landed=-93.572 63.000 10.500 moved=true`; AT there. **MATCH** |

### TP4R5 — supersedes TP4R4: 70 degrees right (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 24, `side=right first=70.000 tried=1 [right 70.000 ok y=<F.y> at=<F + (2.736, 0, 7.518)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=2388`; `TELEPORT … tick=2412 … side=right first=70.000 tried=1 … landed=-97.764 63.000 14.018 moved=true`; AT there. **MATCH** |

### TP5R5 — supersedes TP5R4: a miss moves nothing (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| AT at K; the forced beam's sounds and lines; the step; `… gap=2.700 miss] dealt=none`; no REPORT, no HURT, **no TELEPORT line of any kind** through the 130 ticks; AT at K | K1 | READ: AT at K; FIRE `tick=2528 … gap=2.700 miss] dealt=none`; no REPORT, HURT or TELEPORT line; AT at K. **MATCH** |

### TP6R5 — supersedes TP6R4: every right spot walled; it lands left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 24, `tried=6 [right 50.000 no-footing blocked=7 ungrounded=0, right 30.000 no-footing blocked=7 ungrounded=0, right 43.333 no-footing blocked=7 ungrounded=0, right 56.667 no-footing blocked=7 ungrounded=0, right 70.000 no-footing blocked=7 ungrounded=0, left 50.000 ok y=<F.y> at=<F + (5.142, 0, −6.128)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=2668`; `TELEPORT … tick=2692 … tried=6 [right 50.000, 30.000, 43.333, 56.667, 70.000 each no-footing blocked=7 ungrounded=0, left 50.000 ok …] landed=-95.358 63.000 0.372 moved=true`; AT there. **MATCH** |

### TP7R5 — supersedes TP7R4: walled in everywhere; none, and it stays (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 24, `tried=10`, all ten `no-footing blocked=7 ungrounded=0`, `landed=none moved=-`; AT at K before and after | K1, K3 | READ: FIRE `tick=2808`; `TELEPORT … tick=2832 … tried=10 [` all ten `no-footing blocked=7 ungrounded=0` `] landed=none moved=-`; AT at K before and after. **MATCH** |

### TP8R5 — supersedes TP8R4: a pit two deep (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 24, `tried=1 [right 50.000 ok y=<F.y − 2> at=<F + (5.142, −2, 6.128)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=2948`; `TELEPORT … tick=2972 … tried=1 … landed=-95.358 61.000 12.628 moved=true`; AT there. **MATCH** |

### TP9R5 — supersedes TP9R4: a step two high (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 24, `tried=1 [right 50.000 ok y=<F.y + 2> at=<F + (5.142, 2, 6.128)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=3088`; `TELEPORT … tick=3112 … tried=1 … landed=-95.358 65.000 12.628 moved=true`; AT there. **MATCH** |

### TP10R5 — supersedes TP10R4: ground four down, refused (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 24, `tried=2 [right 50.000 no-footing blocked=0 ungrounded=7, right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=3228`; `TELEPORT … tick=3252 … tried=2 [right 50.000 no-footing blocked=0 ungrounded=7, right 30.000 ok …] landed=-93.572 63.000 10.500 moved=true`; AT there. **MATCH** |

### TP11R5 — supersedes TP11R4: no sight, refused (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 24, `tried=2 [right 50.000 no-sight y=<F.y>, right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=3368`; `TELEPORT … tick=3392 … tried=2 [right 50.000 no-sight y=63.000, right 30.000 ok …] landed=-93.572 63.000 10.500 moved=true`; AT there. **MATCH** |

### TP12R5 — supersedes TP12R4: he steps 2 north during the delay (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit at F; `CMD /tp @s ~ ~ ~-2` 40 ticks after the force (after the fire, 9 or 10 before the move); `TELEPORT … tick=<FIRE + 24> center=<F + (0, 0, −2)> radius=8.246 bearing=14.036 side=right first=50.000 tried=1 [right 50.000 ok … at=<F + (3.610, 0, 5.414)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=3508` at F; `CMD /tp @s ~ ~ ~-2`; `TELEPORT … tick=3532 … center=-100.500 63.000 4.500 radius=8.246 bearing=14.036 side=right first=50.000 tried=1 … landed=-96.890 63.000 11.914 moved=true` (F + (3.610, 0, 5.414)); AT there. **MATCH** |

### TP13R5 — supersedes TP13R4: the Knell killed during the delay (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit (its fire 9 or 10 ticks before the kill); `CMD /kill @e[tag=tp13r5]` 35 ticks after the force; **exactly one** `TELEPORT skipped mob=knell id=<K> target=<Ben> reason=knell-gone`; no decision line | K1 | READ: FIRE `tick=3648`, a hit; `CMD /kill @e[tag=tp13r5]`; exactly one `TELEPORT skipped mob=knell id=<K> target=<Ben> reason=knell-gone`; no decision line. **MATCH** |

### TP14R4 — supersedes TP14R3, RE-PURPOSED (the header): a hit after the move lands moves the Knell again (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The first forced hit: TELEGRAPH₁ at T₁, FIRE₁ at T₁ + 24, one HURT. (2) **exactly one** TELEPORT at **FIRE₁ + 24 = T₁ + 48**, `side=right first=50.000 tried=1 [right 50.000 ok … at=<F + (5.142, 0, 6.128)>] … moved=true` (the fixed draw). (3) **55 ticks after the first force**, in one tick: `CMD /rpg heal 1000000`, then `DRIVE beam` with no `refused=`. (4) TELEGRAPH₂ at **T₂ = T₁ + 55**, `at=<F + (5.142, 0, 6.128)>` (where it landed), with its two sounds; its two fire sounds and FIRE₂ at **T₁ + 79**, `… [player <Ben> gap=<g> hit] dealt=65.000` (an open line from the landing; `length`, `dir` and `g` RECORDED), REPORT, one HURT; **no `TELEPORT ignored` line**. (5) **a second TELEPORT at FIRE₂ + 24 = T₁ + 103**, `from=<F + (5.142, 0, 6.128)> center=<F> radius=8.000 bearing=50.000`, the production draw, `moved=true`, RECORDED; the AT line at its landing | K1, K3, K5 | READ: (1) TELEGRAPH₁ `tick=3754`, FIRE₁ `tick=3778` (T₁ + 24) hit, one HURT. (2) `TELEPORT … tick=3802` (**T₁ + 48**) `side=right first=50.000 tried=1 … landed=-95.358 63.000 12.628 moved=true`. (3) the heal and `DRIVE beam` with no `refused=`. (4) TELEGRAPH₂ `tick=3809 lived=61` (**T₁ + 55**) `at=-95.358 63.000 12.628` with both sounds; FIRE₂ `tick=3833` (**T₁ + 79**) `dir=-0.618172 -0.274086 -0.736709 length=12.259 … [player <Ben> gap=0.000 hit] dealt=65.000`, REPORT, one HURT; **no `TELEPORT ignored` line.** (5) the second `TELEPORT … tick=3857` (**T₁ + 103**) `from=-95.358 63.000 12.628 center=-100.500 63.000 6.500 radius=8.000 bearing=50.000 side=right first=38.423 tried=1 … landed=-100.280 63.000 14.497 moved=true` (the production draw); the AT there. **MATCH: the pending move is cleared when it lands** |

### KB11R6 — supersedes KB11R5: the live cycle, by bound, unbroken by a move (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) both spawns, `REPLY "The base value for attribute Speed for entity Knell set to 0" []`, the draw. (2) the first own TELEGRAPH `forced=false`, **99 ≤ lived ≤ 201**, at K, `ticks=24`, its two sounds at its tick. (3) its FIRE 24 later with its two sounds, a hit; one HURT. (4) **a TELEPORT at that FIRE + 24**, `side=right first=50.000 … landed=<F + (5.142, 0, 6.128)> moved=true`. (5) each next own TELEGRAPH's `lived` − the previous one's − 24 **within 100 to 200**, with the previous hit's move between them. (6) each later hit's move at its FIRE + 24, the production draw, RECORDED, `radius=8.000`. (7) no line names the frozen Knell's uuid but its spawn. (8) between two heals at most one HURT. (9) the kills; anything pending cancelled inside the row, RECORDED | K1 | READ: (1) both spawns, the speed REPLY, the draw. (2) first own TELEGRAPH `forced=false tick=4078 lived=175` (inside 99 to 201) at K, `ticks=24`, both sounds. (3) FIRE `tick=4102` hit; one HURT. (4) `TELEPORT … tick=4126` (+24) `side=right first=50.000 … landed=-95.358 63.000 12.628 moved=true`. (5) next own TELEGRAPH `tick=4290 lived=387`: 387 − 175 − 24 = **188**. (6) FIRE `tick=4314` hit; `TELEPORT … tick=4338` (+24) `bearing=50.000 side=left first=46.293 … landed=-92.517 63.000 7.017`, `radius=8.000`, the production draw. (7) the frozen Knell's uuid on its spawn line only. (8) heals at 08:17:39, :44, :49, :54, :59, 08:18:04; HURTs at 08:17:49 (after that second's heal) and 08:18:00, each alone between two heals. (9) the kills; nothing pending. **MATCH** |

### RE1 — as in knell-4: the NORMAL end of each run still logs its RESTORED line unchanged (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| For EACH run: `SELFTEST RUN FINISHED`, then **exactly one** `SELFTEST RUN RESTORED xp=<n> cell=<class>/<element> gamemode=SURVIVAL held=<h> mana=<m> trace=false spawn_mobs=<what it found> time=<what it found> blocks=<n> mobsRemoved=<n> orbsRemoved=<n> keep_inventory=false immediate_respawn=false difficulty=NORMAL`, **its fields and their order as at `0b4037ec`**; **no `ABORTED` and no `NOT-RESTORED` line** in either run. (A run stopped mid-way would instead log `ABORTED reason=server-stop`, then RESTORED: the server-stop restore is pinned by `SelfTestDisableWiringTest` only and has never been read on a server) | K1; `GATE-dragon-knell-4.md` B5 | READ: dragon-beam `08:15:55 SELFTEST RUN FINISHED`, then `SELFTEST RUN RESTORED xp=712595 cell=mage/fire gamemode=SURVIVAL held=0 mana=100.0 trace=false spawn_mobs=false time=6000 blocks=22879 mobsRemoved=0 orbsRemoved=0 keep_inventory=false immediate_respawn=false difficulty=NORMAL`; dragon-teleport `08:18:43 SELFTEST RUN FINISHED`, then `… held=0 … blocks=101754 mobsRemoved=1 orbsRemoved=0 keep_inventory=false immediate_respawn=false difficulty=NORMAL`. The fields and their order as at `0b4037ec`. **No `ABORTED` and no `NOT-RESTORED` line in either run. MATCH** |

---

## BEN'S ROWS — BY THE ASK STEP, SURVIVAL, DIFFICULTY NORMAL

**AUTO-PARTIAL:** the BEAM, sound and TELEPORT lines are the server's half (what it SENT); what he heard and saw is his
half. **His answers are his ear, never a measurement.** Two questions in all.

### KQ1R5 — supersedes GATE-dragon-knell-4.md KQ1R4: the shot in the middle (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Server half:** a heal; the gold line; one forced beam from a frozen Knell 12 east while he stands still: the two telegraph sounds, the TELEGRAPH `ticks=24`, the two fire sounds, FIRE at T + 24 `… length=18.001 … [player <Ben> gap=0.000 hit] dealt=65.000`, one HURT; **its move at the FIRE + 24** (`radius=12.000 bearing=0.000`, the production draw, RECORDED), 48 after the lock; then the ASK; the kill, no skip line. **His half, yes/no:** ASK 1 *"Is the shot in the middle now, the way you wanted?"* | K1, K2; Ben's ruling | READ, server half: the heal; the gold line; both telegraph sounds (bell 1.00, then the resonance) and both fire sounds (bell 1.50, then the chime) at their ticks; TELEGRAPH `tick=1662 lived=61 at=-88.500 63.000 6.500 … ticks=24`; FIRE `tick=1686 … length=18.001 … [player <Ben> gap=0.000 hit] dealt=65.000`; one HURT; **the move at the FIRE + 24: `TELEPORT … tick=1710 … radius=12.000 bearing=0.000 side=right first=42.460 tried=1 … landed=-91.647 63.000 14.601 moved=true`**, 48 after the lock; the ASK; the kill, no skip line. His half, verbatim: **`ASK 1 question="Is the shot in the middle now, the way you wanted?" answer="yes" ticks=73`** |

### TQ1R5 — supersedes GATE-dragon-knell-4.md TQ1R4: the whole sequence (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Server half:** a heal; the gold line; one forced beam from a frozen Knell 12 east, a hit, the two telegraph sounds and the two fire sounds; **one TELEPORT at the FIRE + 24**, `radius=12.000 bearing=0.000`, the production draw, RECORDED. **His half, yes/no:** ASK 1 *"Does the whole sequence feel right now?"* | K1, K2 | READ, server half: the heal; the gold line; both telegraph sounds (bell 1.00, then the resonance) and both fire sounds (bell 1.50, then the chime) at their ticks; FIRE `tick=4598 … hit] dealt=65.000`; one HURT; `TELEPORT … tick=4622` (+24) `radius=12.000 bearing=0.000 side=left first=30.683 tried=1 … landed=-90.180 63.000 0.377 moved=true`. His half, verbatim: **`ASK 1 question="Does the whole sequence feel right now?" answer="it's almost there. The second purple line is out of place. remove it other than that everything is perfect" ticks=463`**: not yes or no, recorded as his words |
