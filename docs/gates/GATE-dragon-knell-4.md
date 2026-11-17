# GATE — THE KNELL, 4: THE TELEPORT 28 TICKS AFTER THE SHOT (BEN'S NUMBER)

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-knell-4`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the content AND this gate, and
Ben says ready.**

**What this gate re-predicts, and why.** Ben, 2026-11-17, after the boot of `9d0cbffb` (`PLAN-dragon-mobs.md`, RULINGS,
*one more number*): *"let's try 1.4 seconds after the shot"* = **28 ticks**, replacing the seat's 44. **One number moves;
nothing else** (the 20-tick telegraph, the sounds, the damage, the arc and the landing checks are `GATE-dragon-knell-3.md`'s,
READ at `9d0cbffb`).

- **The teleport file:** every row that logs a move or a ticked teleport line is re-predicted HERE under the next suffix
  (KB10R4 → KB10R5, TP2R3 → TP2R4, TP14R2 → TP14R3, KB11R4 → KB11R5, TQ1R3 → TQ1R4). **TP5R3** (a miss) and **TP13R3**
  (a kill during the delay) log the same lines at any delay, keep their names, and are **CARRIED** here with
  `GATE-dragon-knell-3.md`'s predictions verbatim; they run again only because their file does. **The teleport file is
  paired with THIS gate** (`SelfTestScenariosTest.docOf`; the note in `selftest/index.yml`).
- **The beam file is NOT changed and NOT re-run**, and stays paired with `GATE-dragon-knell-3.md`: its hit rows kill their
  Knell before the move is due (B5 below), so none of their lines moves. **One exception, named rather than hidden:
  KQ1R3's server half predicts its first hit's move "at the FIRE + 44"; at this delay it lands at the FIRE + 28.** Its ASK
  lines (the beam's timing and the dodge) do not change, so it is not re-predicted; whether to re-run it is the seat's.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL.
- **The difficulty is set by the selftest:** the setup's `difficulty: normal`; `DIFFICULTY found=… set=NORMAL`;
  `RESTORED … difficulty=<what it found>`.
- **Gear score 100 on every Knell:** a hit is **65.000** before defence.

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC, ROW BY ROW

**The floor:** his maximum is at least 100 (it read **350**: `REPLY "Custom HP: 350/350"`). **65 before defence leaves at
least 35**; every row heals him before every beam that can land, and no row lands two beams between heals. **No Knell can
reach him:** every Knell in this file is frozen, except KB11R5's, whose walking speed base is 0.

| row | what can hit him after a heal | the worst case, before defence |
|---|---|---|
| KB10R5, TP2R4-TP4R4, TP6R4-TP12R4, TP13R3, TQ1R4 | one forced beam that hits | **65** |
| TP5R3 | a miss | **0** |
| TP14R3 | two forced beams, **a heal between** (23 ticks after the first force, after the first fire) | **65**, then **65** |
| KB11R5 | its own beams, at least 120 ticks apart, a heal every 100 ticks | **65 between two heals** |

## WHAT THE ROWS STAND ON

- **K1. The timings, READ at the boot of `9d0cbffb`:** every fire exactly 20 ticks after its telegraph (29 of 29), every
  move exactly `delay_ticks` after the fire of the hit that scheduled it (17 of 17). Only the number moves: **28**
  (`knell.yml`; `MobLoaderTest.theBundledKnellsDelayIsSeventyTwoTicksShorterThanItsCycle`).
- **K2. The geometry, READ at that boot:** every landing, wall, pit, step and sight line, relative to Ben's feet F and the
  Knell's K = F + (8, 0, 0). **The landings do not depend on the delay**, only the tick of the line.
- **K3. A forced beam during a pending move, READ at that boot (TP14R2):** `TELEPORT ignored … reason=pending`, the first
  move at its own fire + delay.
- **K4. A force is refused during a telegraph** (`MobBeamer#force`: `busy: telegraph n/20`), READ in our code. A force
  after the first fire and before the first move is accepted.

## THE ROW AUDIT (B5): WHERE EVERY TELEPORT LINE LANDS AT FIRE + 28

**A forced beam's telegraph comes one or two ticks after the BEAM step (call it the force), so its fire is at the force +
21 or 22 and a hit's move at the force + 49 or 50.** The same offset applies to every force in a row, so differences
between two forces in one row are exact.

| file | row | before (knell-3, at + 44) | at + 28 | changed? |
|---|---|---|---|---|
| beam | KB1R3, KB4R3, KB7R3, KB2R3 (hits) | kill at the force + 40, move due at + 65: skip inside the row | kill at + 40, move due at + 49: **still a skip inside the row, 9 ticks before the move** | no: the beam file is unchanged |
| beam | KB3R3, KB5R3, KB8R3, WA1 (misses); KB6R3, KB9R3 (no fire) | no teleport line | no teleport line | no |
| beam | KQ1R3 | the first move at the FIRE + 44, during ASK 1 | at the FIRE + 28, still during ASK 1 (the ASK waits for him) | **its predicted tick, only** (see the header) |
| teleport | KB10R5, TP2R4-TP4R4, TP6R4-TP11R4 | the move at + 65 or 66, the read at + 130 | the move at + 49 or 50, the read at + 130 | the TELEPORT tick: re-predicted |
| teleport | TP12R4 | he steps north at + 40, the move at + 65 | he steps at + 40, **9 ticks before the move at + 49**: still during the delay | the TELEPORT tick: re-predicted; the step's time kept |
| teleport | TP13R3 | the kill at + 35, the move due at + 65 | the kill at + 35, **14 ticks before the move**: still a skip | no: CARRIED |
| teleport | TP5R3 | a miss, no line | a miss, no line | no: CARRIED |
| teleport | **TP14R3** | the second force at + 30, its fire at + 51, **before** the move at + 65 | **at + 30 the second fire would land at + 51, AFTER the move at + 49: a second hit with nothing pending, which would schedule a SECOND move and test nothing.** **RE-TIMED:** the heal and the second force at **+ 23** (after the first fire at + 21 or 22, so the force is accepted, K4), its fire at **+ 44**, **5 ticks before the first move at + 49** | **yes: re-timed and re-predicted** |
| teleport | KB11R5 | each move at its fire + 44, the next telegraph 100 to 200 after the fire | each move at its fire + 28 | the TELEPORT ticks: re-predicted |
| teleport | TQ1R4 | the move at + 44, before the ASKs | at + 28, before the ASKs | the tick and both questions: re-predicted |
| merged | DB1R2, DB11R2, WS-C, WS2c | never (frozen, or no Knell) | never | no |
| closed manual gates (13) | a live Knell's own beam | the move 44 after a hit | 28 after | **a finding, carried**; not re-predicted |

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-dragon-knell-4"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   If the folder exists, add `-2` to its name. Report the folder, both pairs of counts, and `diff -rq lines: 0` each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report.
4. **Boot this branch's head:** `./scripts/dev-server.sh --refresh-content`, announcing "booting now", on Ben's "ready".
5. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` line names this branch's head. **Readings count
   only after Ben's LOGIN in that boot's log.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest dragon-teleport` — **ONCE.** Stand still and touch nothing, **except in its last part**: a Knell twelve
   blocks east beams you once. **After the hit, watch the Knell and listen to the ringing: it should now move 1.4 seconds
   after the shot.** Two questions; answer each `yes` or `no`.
3. `/stop`

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-knell-4` prints it, shortened. **Not** master's `6b587faa`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1`, after this boot's `Done` | |

### R0b — the jar carries the 28

| prediction | instrument | READING |
|---|---|---|
| In `content/mobs/knell.yml`: PRESENT `  delay_ticks: 28`; ABSENT `  delay_ticks: 44`. Keys: `GATE-dragon-teleport.yml` `KB10R5:` and `TP14R3:` PRESENT, `KB10R4:` and `TP14R2:` ABSENT. **The controls, the same on both builds:** `knell.yml` PRESENT `  telegraph_ticks: 20`; `GATE-dragon-teleport.yml` `TP5R3:` PRESENT; `GATE-dragon-beam.yml` `KB1R3:` PRESENT (the beam file unchanged); `MobBeamLines.class` PRESENT `BEAM sound mob=` | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$k = Bytes 'content/mobs/knell.yml'
foreach ($l in '  delay_ticks: 28', '  delay_ticks: 44', '  telegraph_ticks: 20') {
  if ($k -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT knell.yml '$l'" } else { "ABSENT  knell.yml '$l'" }
}
$b = Bytes 'io/github/butterflysmp/rpg/paper/health/MobBeamLines.class'
if ($b -ne $null -and $b.Contains('BEAM sound mob=')) { "PRESENT MobBeamLines.class 'BEAM sound mob='" } else { "ABSENT  MobBeamLines.class 'BEAM sound mob='" }
foreach ($q in @(@('dragon-teleport', 'KB10R5'), @('dragon-teleport', 'TP14R3'), @('dragon-teleport', 'KB10R4'), @('dragon-teleport', 'TP14R2'), @('dragon-teleport', 'TP5R3'), @('dragon-beam', 'KB1R3'))) {
  $y = Bytes "selftest/GATE-$($q[0]).yml"
  if ($y -eq $null) { "MISSING selftest/GATE-$($q[0]).yml" } elseif ($y -match "(?m)^$($q[1]):\s*$") { "PRESENT $($q[0]) $($q[1]):" } else { "ABSENT  $($q[0]) $($q[1]):" }
}
$zip.Dispose()
```

**A jar of master `6b587faa` reads ABSENT on `delay_ticks: 28`, `KB10R5:` and `TP14R3:`, and PRESENT on `delay_ticks:
44`, `KB10R4:` and `TP14R2:`**; the four controls read PRESENT on both.

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim the boot of `9d0cbffb`'s. No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN` beyond `volley_stone`'s known pair. **A `Skipping malformed mob 'knell.yml'` line would mean the teleport block was refused** (`4 mobs`) | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 117 scenarios across 17 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport]`**, and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | |

**The 117 is RECOUNTED from the files at this branch's head:** in `paper/src/main/resources/selftest/`,
`grep -cE '^  - ' index.yml` prints **17**; `cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/;
s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints **134**; the same with `'^setup:'` prints **17**: 134 − 17 = **117**.
**This PR renames rows and adds none, so master `6b587faa` prints the same 117 and the count CANNOT tell the two builds
apart; R0b's keys are the control that can.** `SelfTestScenariosTest` pins the 117 and every row name.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument:** `Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-teleport|\[Rpg\] (BEAM|TELEPORT)'`,
after Ben's LOGIN, inside each row's own `START … DONE` bracket.

**In EVERY row (not repeated):** `LEFTOVER living=0 radius=16` after its CLEAN; each spawn's `DRIVE spawn tag=<tag>
<uuid> knell frozen` (or `live`) and its `REPLY "Spawned Knell (wither_skeleton, 360 HP, GS 100 given)" [aqua, gray]`;
each `DRIVE beam` with no `refused=`; each `DRIVE teleport_draw … side=<s> first=<a>`. **No `[Rpg]` line names a Knell
that is not the row's own.**

**A FORCED HIT from a frozen Knell 8 east reads, as at `9d0cbffb`:** the strike and the resonance at the telegraph's tick
T, `BEAM telegraph … ticks=20`; the chime and `BEAM fire … tick=<T+20> … length=12.259 clip=block tolerance=1.00 tested=1
[player <Ben> gap=0.000 hit] dealt=65.000`; one REPORT; `HURT before=65.000 after=59.091 defense=10.000 …`. **A MOVE** is
`[Rpg] TELEPORT mob=knell id=<K> target=player <Ben> tick=<FIRE + 28> from=… center=… radius=… bearing=… side=… first=…
tried=<n> […] landed=… moved=…`.

### KB10R5 — supersedes GATE-dragon-knell-3.md KB10R4: the move 28 ticks after the fire (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| AT at K; the forced hit; **exactly one** `TELEPORT … tick=<FIRE + 28> from=<K> center=<F> radius=8.000 bearing=0.000 side=right first=50.000 tried=1 [right 50.000 ok y=<F.y> at=<F + (5.142, 0, 6.128)>] landed=<the same> moved=true`; the AT line there | K1, K2 | |

### TP2R4 — supersedes TP2R3: 50 degrees left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 28, `side=left first=50.000 tried=1 [left 50.000 ok y=<F.y> at=<F + (5.142, 0, −6.128)>] … moved=true`; AT there | K1, K2 | |

### TP3R4 — supersedes TP3R3: 30 degrees right (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 28, `side=right first=30.000 tried=1 [right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; AT there | K1, K2 | |

### TP4R4 — supersedes TP4R3: 70 degrees right (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 28, `side=right first=70.000 tried=1 [right 70.000 ok y=<F.y> at=<F + (2.736, 0, 7.518)>] … moved=true`; AT there | K1, K2 | |

### TP5R3 — CARRIED from GATE-dragon-knell-3.md: a miss moves nothing (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **GATE-dragon-knell-3.md's prediction, verbatim:** AT at K; the forced beam; the step; `… gap=2.700 miss] dealt=none`; no REPORT, no HURT, **no TELEPORT line of any kind** through the 130 ticks; AT at K | K1 | |

### TP6R4 — supersedes TP6R3: every right spot walled; it lands left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 28, `tried=6 [right 50.000 no-footing blocked=7 ungrounded=0, right 30.000 no-footing blocked=7 ungrounded=0, right 43.333 no-footing blocked=7 ungrounded=0, right 56.667 no-footing blocked=7 ungrounded=0, right 70.000 no-footing blocked=7 ungrounded=0, left 50.000 ok y=<F.y> at=<F + (5.142, 0, −6.128)>] … moved=true`; AT there | K1, K2 | |

### TP7R4 — supersedes TP7R3: walled in everywhere; none, and it stays (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 28, `tried=10`, all ten `no-footing blocked=7 ungrounded=0`, `landed=none moved=-`; AT at K before and after | K1, K2 | |

### TP8R4 — supersedes TP8R3: a pit two deep (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 28, `tried=1 [right 50.000 ok y=<F.y − 2> at=<F + (5.142, −2, 6.128)>] … moved=true`; AT there | K1, K2 | |

### TP9R4 — supersedes TP9R3: a step two high (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 28, `tried=1 [right 50.000 ok y=<F.y + 2> at=<F + (5.142, 2, 6.128)>] … moved=true`; AT there | K1, K2 | |

### TP10R4 — supersedes TP10R3: ground four down, refused (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 28, `tried=2 [right 50.000 no-footing blocked=0 ungrounded=7, right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; AT there | K1, K2 | |

### TP11R4 — supersedes TP11R3: no sight, refused (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 28, `tried=2 [right 50.000 no-sight y=<F.y>, right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; AT there | K1, K2 | |

### TP12R4 — supersedes TP12R3: he steps 2 north during the delay (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit at F; `CMD /tp @s ~ ~ ~-2` 40 ticks after the force (after the fire, **9 ticks before the move**); `TELEPORT … tick=<FIRE + 28> center=<F + (0, 0, −2)> radius=8.246 bearing=14.036 side=right first=50.000 tried=1 [right 50.000 ok … at=<F + (3.610, 0, 5.414)>] … moved=true`; AT there | K1, K2 | |

### TP13R3 — CARRIED from GATE-dragon-knell-3.md: the Knell killed during the delay (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **GATE-dragon-knell-3.md's prediction, verbatim:** the forced hit; `CMD /kill @e[tag=tp13r3]` 35 ticks after the force; **exactly one** `TELEPORT skipped mob=knell id=<K> target=<Ben> reason=knell-gone`; no decision line. (At this delay the kill is 14 ticks before the move.) | K1 | |

### TP14R3 — supersedes TP14R2, RE-TIMED: a beam forced while a move is pending (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The first forced hit: TELEGRAPH₁ at T₁, FIRE₁ at T₁ + 20, one HURT. (2) **23 ticks after the first force:** `CMD /rpg heal 1000000` and, in the same tick, `DRIVE beam` with no `refused=` (the first telegraph has ended, K4). (3) TELEGRAPH₂ at **T₂ = T₁ + 23** with both sounds; its chime and FIRE₂ at **T₁ + 43**, `… gap=0.000 hit] dealt=65.000`, REPORT, **exactly one** `TELEPORT ignored mob=knell id=<K> reason=pending`, one HURT. (4) **exactly one** decision, at **FIRE₁ + 28 = T₁ + 48** (5 ticks after FIRE₂; FIRE₂ + 28 would be T₁ + 71), `side=right first=50.000 tried=1 [right 50.000 ok … at=<F + (5.142, 0, 6.128)>] … moved=true`; AT there | K3, K4 | |

### KB11R5 — supersedes KB11R4: the live cycle, by bound, unbroken by a move (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) both spawns, `REPLY "The base value for attribute Speed for entity Knell set to 0" []`, the draw. (2) the first own TELEGRAPH `forced=false`, **99 ≤ lived ≤ 201**, at K, `ticks=20`, its two sound lines at its tick. (3) its FIRE 20 later, a hit; one HURT. (4) **a TELEPORT at that FIRE + 28**, `side=right first=50.000 … landed=<F + (5.142, 0, 6.128)> moved=true`. (5) each next own TELEGRAPH's `lived` − the previous one's − 20 **within 100 to 200**, with the previous hit's move between them. (6) each later hit's move at its FIRE + 28, the production draw, RECORDED, `radius=8.000`. (7) no line names the frozen Knell's uuid but its spawn. (8) between two heals at most one HURT. (9) the kills; anything pending cancelled inside the row, RECORDED | K1 | |

---

## BEN'S ROW — BY THE ASK STEP, SURVIVAL, DIFFICULTY NORMAL

**AUTO-PARTIAL:** the BEAM, sound and TELEPORT lines are the server's half (what it SENT); what he heard and saw is his
half. **His answers are his ear, never a measurement.**

### TQ1R4 — supersedes GATE-dragon-knell-3.md TQ1R3: the move 1.4 seconds after the shot (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Server half:** a heal; the gold line; one forced beam from a frozen Knell 12 east, a hit, the two telegraph sounds and the chime; **one TELEPORT at the FIRE + 28**, `radius=12.000 bearing=0.000`, the production draw, RECORDED. **His half, yes/no:** ASK 1 *"Did the Knell move at the right moment in the ringing?"*; ASK 2 *"Does the whole sequence feel timed right now?"* | K1; Ben's 28 | |
