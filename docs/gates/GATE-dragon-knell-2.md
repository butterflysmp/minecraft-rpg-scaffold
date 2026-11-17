# GATE — THE KNELL, 2: 65 DAMAGE, A 1.3-SECOND BELL WARNING, THE MOVE WHEN THE RINGING ENDS, AND THE ROOT ON A WALKING KNELL

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-knell-2`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

**2026-11-17: approved to merge by the review seat after the boot of 58c0f7a1.**

> **READ, 2026-11-17: booted at `58c0f7a1` on the seat's approval and Ben's "ready".** Server log `05:29:37` to the
> clean `/stop` at `05:33:55`. Carried state: no JVM; backup `rpg-run-backups/2026-11-17-pre-dragon-knell-2`
> (`run/plugins/Rpg` 129 files, 385586 bytes; `run/world` 193 files, 204963771 bytes; `diff -rq lines: 0` each);
> `12:  dev-pack-on-join: true`. Ben logged in at `05:29:40`. **The run sheet's first line was not typed** (no
> `/gamemode survival` in the log); he was already in SURVIVAL (the selftest ran, and both `RESTORED` lines read
> `gamemode=SURVIVAL`). Each selftest was run once; both ran to `SELFTEST RUN FINISHED`.
>
> **The result: 25 of 26 rows MATCH. One part of one row does not: KB2R2 (5)**: the walking Knell had not walked
> again 13 ticks after its fire. Its control passed, so the root on a walking Knell IS read (rooted for the 26).
> **Ben's five answers: yes, yes, yes; yes, and NO to "Did that wait feel right?"**
>
> **Counts:** 29 TELEGRAPH lines, each with one `block.bell.use` line at its tick; 28 FIRE lines (the 29th telegraph
> is KB9R2's, cancelled `elapsed=8/26`), each with one resonance and one chime line at its tick, **every fire 26
> ticks after its telegraph (28 of 28)**. 16 TELEPORT decisions, **every one 89 ticks after the fire of the hit that
> scheduled it (16 of 16)**; 6 `skipped … knell-gone`, each inside its own row; 1 `ignored … pending` (TP14).
> 23 hits and 23 HURT lines, each `before=65.000 after=59.091 defense=10.000 dealerType=wither_skeleton/knell`: no
> hit from anything but a beam. Every LEFTOVER line `living=0`. Both runs: `DIFFICULTY found=NORMAL set=NORMAL`, and
> `RESTORED … difficulty=NORMAL`.

**What this gate re-predicts, and why.** Ben, 2026-11-17, after the stacked boot of R5 and R6 (`PLAN-dragon-mobs.md`,
RULINGS, *the Knell's numbers after the boot*): 65 damage; *"The bell plays with the warning. The real shot comes when
the resonance part starts. The teleport happens when the sound is fully finished"*; a 1.3-second warning. **Every row of
`selftest/GATE-dragon-beam.yml` and `selftest/GATE-dragon-teleport.yml` changes in a figure or a time**, so every row is
re-predicted HERE as a new row named for the row it supersedes, with the next suffix (KB1 → KB1R2, KB10R2 → KB10R3,
TP2 → TP2R2), and TP14 is new. **Both scenario files are paired with THIS gate** (`SelfTestScenariosTest.docOf`, and
the rule's note in `selftest/index.yml`), as the three dragon gates are with `GATE-dragon-fix.md`; keeping both files
keeps Ben's two commands. The old rows' predictions and readings stay in `GATE-dragon-beam.md` and
`GATE-dragon-teleport.md`, untouched, each under a dated note naming what this gate supersedes.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL.
- **The difficulty is set by the selftest:** each gate's setup `difficulty: normal`; `DIFFICULTY found=… set=NORMAL`;
  `RESTORED … difficulty=<what it found>`.
- **Gear score 100 on every Knell:** a hit is the authored 65 × 100 / 100 = **65.000** before defence
  (`MobBeam.price`; `MobLoaderTest.theBundledKnellBeamsAsBenRuledAfterTheBoot`).

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC, ROW BY ROW

**The floor:** his maximum is at least 100. **65 before defence leaves at least 35**, so every row heals him before
every beam that can land, and no row can land two beams between heals. **No Knell can reach him to melee** (a Wither
tick from a melee hit is priced in proportion to his maximum, so melee is ruled out by staging, never bounded):

- **every frozen Knell** (NoAI: it cannot walk or attack; a teleport moves it, and it is frozen where it lands);
- **KB11R3's live Knell**, its walking speed BASE set to 0, at a horizontal 8 blocks, which every move keeps;
- **KB2R2's live, WALKING Knell**, by distance and time (below, K2).

| row | what can hit him after a heal | the worst case, before defence |
|---|---|---|
| KB1R2, KB4R2, KB7R2, KB2R2 | one forced beam that hits | **65** |
| KB3R2, KB5R2, KB8R2 | one forced beam that misses | **0** predicted, 65 if it hit |
| KB6R2, KB9R2 | none (out of range; killed mid-telegraph) | **0** |
| KQ1R2 | two forced beams, **a heal between** | **65**, then **65** |
| KB10R3, TP2R2-TP4R2, TP6R2-TP13R2, TQ1R2 | one forced beam that hits | **65** |
| TP5R2 | a miss | **0** |
| TP14 | two forced beams, **a heal between** (35 ticks after the first) | **65**, then **65** |
| KB11R3 | its own beams, at least 126 ticks apart (a 100-tick minimum wait after a fire, then a 26-tick telegraph), a heal every 100 ticks | **65 between two heals** |

## WHAT THE ROWS STAND ON — EACH FACT, AND ITS MARK

- **K1. The timings, READ in our code:** the fire is exactly `telegraph_ticks` (26) loop steps after the lock
  (`BeamCycle`), and the move exactly `delay_ticks` (89) after the fire (`MobTeleporter`, a one-shot task of that period on
  the Knell's own scheduler). **Read at the stacked boot: 25 of 25 fires were the TELEGRAPH + 12 and 17 of 17 moves the
  FIRE + 12, with the old numbers**, so the mechanism is read and only the numbers move.
- **K2. THE WALKING KNELL CANNOT REACH HIM, READ from the server jar (build 74; `javap -c -p -cp
  run/versions/26.1.2/paper-26.1.2.jar` on each class named) and EXECUTED in jshell.**
  - **It sees him:** `AbstractSkeleton.registerGoals` adds `NearestAttackableTargetGoal(this, Player.class, true)`, the
    `true` being must-see; `LivingEntity.hasLineOfSight(Entity)` clips eye to eye with `ClipContext$Block.COLLIDER`.
    **So any block with a collision shape between their eyes blinds it**, which is why KB2's glass pen read hollow.
    KB2R2 stands on an open floor with nothing between them. Its range: `Mob.createMobAttributes` gives
    `FOLLOW_RANGE` 16.0; it spawns 15 blocks away.
  - **Its top speed:** `AbstractSkeleton`'s melee goal is `MeleeAttackGoal(this, 1.2, false)`; `MoveControl`'s MOVE_TO
    calls `mob.setSpeed((float)(speedModifier × MOVEMENT_SPEED))`, and `Mob.setSpeed` also `setZza`s it, so the forward
    input IS that speed; `travelInAir` takes the ground block's friction (the default `0.6f`,
    `BlockBehaviour$Properties`) × 0.91 as the per-tick horizontal multiplier; `getFrictionInfluencedSpeed` on ground is
    speed × `0.21600002f` / friction³; `Entity.getInputVector` scales the input without normalising it (its length is
    under 1). With the Knell's speed base 0.15 (DB1R2's reading, `speedBase=0.1500`), jshell: speed 0.180000, friction
    0.546000, acceleration 0.042995, **steady displacement 0.094703 blocks a tick (1.89 b/s)**, which the velocity
    approaches from below.
  - **The arithmetic:** it is live from its spawn to the row's kill, at most **105 ticks** (wait 60, AT, wait 5, AT,
    force, wait 40, AT, kill), of which the telegraph roots **26**, so it walks at most **79 ticks: at most 7.48
    blocks**. A melee hit needs it within about 3 blocks of him (centre to centre: its half-width 0.56, his 0.3, and an
    attack reach under 2, INFERRED, not read). **From 15 blocks it would need to walk at least 12, so the margin is at
    least 4.5 blocks, 48 ticks at its top speed.** Its own cycle cannot beam him before the kill either: the forced beam's
    fire redraws its wait, 100 to 200 ticks after the fire.
  - **INFERRED, not read:** that the speed attribute is not otherwise raised on our Knell (no modifier of ours but the
    telegraph's ×0 root).
- **K3. The sounds, READ in our code:** `MobBeamer.playSounds` plays the authored list and logs one `BEAM sound` line
  for each, the telegraph's list **just before** the TELEGRAPH line, the fire's **just before** the FIRE line
  (`DragonBeamWiringTest.theSoundsComeFromContentAndEachIsLogged`). **The lines say what the SERVER sent; what the client
  played is Ben's to hear.**
- **K4. The geometry of every frozen row is `GATE-dragon-beam.md` B4's and `GATE-dragon-teleport.md` T3-T5's,
  unchanged** (a Knell 8 east: `dir=-0.961705 -0.274086 0.000000`, `length=12.259 clip=block`; the landings, walls,
  pits and sight lines), **all READ at the stacked boot of `e52e54b6`.** Only the damage, the times and the sounds move.
- **K5. A forced beam during a pending move (B6), READ in our code:** `MobTeleporter.onBeamHit` finds the pending task
  running and logs `TELEPORT ignored mob=knell id=<u> reason=pending`, scheduling nothing; the first move still lands at
  its own FIRE + 89. **The Knell's own cycle cannot do this** (its minimum wait after a fire, 100, exceeds the delay, 89:
  `MobLoaderTest.theBundledKnellsDelayIsElevenTicksShorterThanItsCycle`; a delay of 100 is refused at load,
  `MobTeleportTest`).
- **K6. Ben's feet F and a frozen Knell's feet K = F + (8, 0, 0)**, as before; F is read from the TELEGRAPH line's `to=`
  as F + (0, 1.080, 0).

## THE ROWS ALREADY MERGED — THE AUDIT (the brief's B8)

**With an 89-tick delay almost every beam row would end before its move is due**, so the answer to *where does the
TELEPORT line land* must never rest on the runner's tick count again (KB4, 2026-11-17). Every row now ends itself:

| file | rows | a hit? | where its TELEPORT line lands |
|---|---|---|---|
| beam | KB1R2, KB4R2, KB7R2, KB2R2 | yes | **inside the row:** it kills its Knell 40 ticks after the force (the fire + 12 to 14, the move still 75+ away); the removal cancels the move and logs `TELEPORT skipped … reason=knell-gone` before the row's `SETTLED` line |
| beam | KB3R2, KB5R2, KB8R2 | no (a miss) | **none**: nothing is pending; the kill logs no TELEPORT line |
| beam | KB6R2, KB9R2 | no fire | **none** |
| beam | KQ1R2 | yes, up to twice | the first hit's move lands inside the row, during the ASK steps (the production draw); the second force comes at least 140 ticks after the first (wait 40, two answers, wait 100), past the first move; the second hit's move is cancelled by the row's kill (`knell-gone`), or lands, if he took longer than 89 ticks to answer ASK 3, inside the row: RECORDED |
| teleport | KB10R3, TP2R2-TP4R2, TP6R2-TP11R2 | yes | **inside the row:** each waits 130 ticks after the force; the move is at most the force + 117 |
| teleport | TP5R2 | no | **none** through the 130 ticks |
| teleport | TP12R2 | yes | inside the row (he steps north 60 ticks after the force; the move at most + 117; the read at + 130) |
| teleport | TP13R2 | yes, killed | the skip line inside the row (kill at + 60, its removal about 20 later) |
| teleport | TP14 | yes, twice | the second hit's `TELEPORT ignored` and the first's move both inside the row (the read at the second force + 90) |
| teleport | KB11R3 | its own, two or three | each move inside the row while the row lasts (560 ticks); a move or telegraph still pending at the end is cancelled by the row's own kills: `TELEPORT skipped … knell-gone` and/or `BEAM cancelled`, inside the row |
| teleport | TQ1R2 | yes | inside the row (130 ticks after the force, before the ASKs) |
| merged | DB1R2, DB11R2, WS-C, WS2c | never (frozen, or no Knell) | **none**: a frozen Knell never begins a beam (I23), so it never hits and never moves |
| closed manual gates (13) | a live Knell's own beam | yes | **a finding, carried**: such a row now also takes 65 and moves its Knell 89 ticks later; not re-predicted |

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-dragon-knell-2"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   If the folder exists (a boot the same day), add `-2` to its name. Report the folder, both pairs of counts, and
   `diff -rq lines: 0` for each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report.
4. **Boot this branch's head:** `./scripts/dev-server.sh --refresh-content`, announcing "booting now", on Ben's "ready".
5. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` line names this branch's head (wait for THIS
   boot's `Build:` line: the previous boot's log is still `latest.log` until the server rolls it). **Readings count
   only after Ben's LOGIN in that boot's log.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest dragon-beam` — **ONCE.** Stand still and touch nothing, **except in its last part**: a Knell twelve
   blocks east beams you twice, with a gold line in chat before each. **Listen:** the bell STRIKE is the warning (a
   thin purple line appears with it); the beam fires as the bell starts to RING (a thick cyan line). **The first time,
   stand still. The second time, step sideways (A or D) as soon as you hear the strike.** Three questions; answer each
   `yes` or `no`.
3. `/rpg selftest dragon-teleport` — **ONCE.** Stand still and touch nothing, **except in its last part**: a Knell twelve
   blocks east beams you once. **After the hit, listen to the bell ring, and watch the Knell as the ringing ends.** Two
   questions; answer each `yes` or `no`.
4. `/stop`

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-knell-2` prints it, shortened. **Not** master's `7a3e9985`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1`, after this boot's `Done` | READ 2026-11-17: `[05:29:37] [Server thread/INFO]: [Rpg] Build: 58c0f7a1`, this PR's head (`git ls-remote` `58c0f7a19bf0…`). **MATCH** |

### R0b — the jar carries the follow-up

| prediction | instrument | READING |
|---|---|---|
| In `content/mobs/knell.yml`: PRESENT `  telegraph_ticks: 26`, `  flash_at: [0, 20]`, `  damage: 65`, `  delay_ticks: 89`, `      - { key: block.bell.use, volume: 1.0, pitch: 1.0 }`, `      - { key: block.bell.resonate, volume: 1.4, pitch: 1.0 }`; ABSENT the old `  telegraph_ticks: 12`, `  flash_at: [0, 9]`, `  damage: 35`, `  delay_ticks: 12`. Classes: `MobBeamLines.class` PRESENT `BEAM sound mob=`; `MobLoader.class` PRESENT `names no sound event the server knows`; `NoSuchTextControl` ABSENT in `MobBeamer.class` (the text control). Keys: `GATE-dragon-beam.yml` `KB1R2:`, `KB2R2:` PRESENT, `KB1:` ABSENT; `GATE-dragon-teleport.yml` `KB10R3:`, `TP14:` PRESENT, `KB10R2:` ABSENT; `GATE-dragon-blast.yml` `BQ1R2:` PRESENT (the key control) | the scan below | READ: all 20 lines as predicted: the six new `knell.yml` lines PRESENT and the four old ABSENT; `BEAM sound mob=` and the refusal text PRESENT; `NoSuchTextControl` ABSENT; `KB1R2:`, `KB2R2:`, `KB10R3:`, `TP14:` PRESENT, `KB1:` and `KB10R2:` ABSENT, `BQ1R2:` PRESENT. **MATCH** |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$k = Bytes 'content/mobs/knell.yml'
foreach ($l in '  telegraph_ticks: 26', '  flash_at: [0, 20]', '  damage: 65', '  delay_ticks: 89', '      - { key: block.bell.use, volume: 1.0, pitch: 1.0 }', '      - { key: block.bell.resonate, volume: 1.4, pitch: 1.0 }', '  telegraph_ticks: 12', '  flash_at: [0, 9]', '  damage: 35', '  delay_ticks: 12') {
  if ($k -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT knell.yml '$l'" } else { "ABSENT  knell.yml '$l'" }
}
foreach ($c in @(@('health/MobBeamLines.class', 'BEAM sound mob='), @('content/MobLoader.class', 'names no sound event the server knows'), @('health/MobBeamer.class', 'NoSuchTextControl'))) {
  $b = Bytes "io/github/butterflysmp/rpg/paper/$($c[0])"
  if ($b -eq $null) { "MISSING $($c[0])" } elseif ($b.Contains($c[1])) { "PRESENT $($c[0]) '$($c[1])'" } else { "ABSENT  $($c[0]) '$($c[1])'" }
}
foreach ($q in @(@('dragon-beam', 'KB1R2'), @('dragon-beam', 'KB2R2'), @('dragon-beam', 'KB1'), @('dragon-teleport', 'KB10R3'), @('dragon-teleport', 'TP14'), @('dragon-teleport', 'KB10R2'), @('dragon-blast', 'BQ1R2'))) {
  $y = Bytes "selftest/GATE-$($q[0]).yml"
  if ($y -eq $null) { "MISSING selftest/GATE-$($q[0]).yml" } elseif ($y -match "(?m)^$($q[1]):\s*$") { "PRESENT $($q[0]) $($q[1]):" } else { "ABSENT  $($q[0]) $($q[1]):" }
}
$zip.Dispose()
```

**A jar of master reads the opposite on every `knell.yml` line, ABSENT on both new class texts, ABSENT on the four new
keys and PRESENT on `KB1:` and `KB10R2:`**; `BQ1R2:` and `NoSuchTextControl` read the same on both.

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim the stacked boot's: this PR changes `knell.yml` and adds no content file. No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN`, beyond `volley_stone`'s known pair. **A `Skipping malformed mob 'knell.yml'` line would mean a sound key, the delay or the beam was refused** (`4 mobs`) | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | READ: the Loaded line verbatim as predicted; no `Refusing`, `Skipping`, `SEVERE` or `Exception` in the whole log; the only `[Rpg]` WARN lines are `volley_stone`'s known pair. (Paper's own "4 release(s) behind" banner is a Paper WARN, not ours.) **MATCH** |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 116 scenarios across 17 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport]`**, and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | READ: `[Rpg] Selftest: dev server, 116 scenarios across 17 gates [melee-cell, … dragon-beam, dragon-teleport]`, the list verbatim; no `Refusing selftest`. **MATCH** |

**The 116 is RECOUNTED from the files at this branch's head:** in `paper/src/main/resources/selftest/`,
`grep -cE '^  - ' index.yml` prints **17**; `cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/;
s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints **133**; the same with `'^setup:'` prints **17**: 133 − 17 = **116**.
**The control that tells this build apart: at master `7a3e9985` the same commands print 17, 132, 17: 115.**
`SelfTestScenariosTest` pins the 116 and every row name.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument:** `Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-(beam|teleport)|\[Rpg\] (BEAM|TELEPORT)'`,
after Ben's LOGIN, inside each row's own `START … DONE` bracket.

**In EVERY row (not repeated):** `LEFTOVER living=0 radius=16` after its CLEAN; each spawn's `DRIVE spawn tag=<tag> <uuid>
knell frozen` (or `live`) and `REPLY "Spawned Knell (wither_skeleton, 360 HP, GS 100 given)" [aqua, gray]`; each
`DRIVE beam tag=<tag> <uuid>` with no `refused=`; each `DRIVE teleport_draw tag=… side=<s> first=<a>`; each `CMD /kill
@e[tag=<tag>]` followed by the runner's `SETTLED dying=0 ticks=<t> first=1`. **No `[Rpg]` line names a Knell that is not
the row's own.**

**A FORCED HIT, from a frozen Knell 8 east, reads (the row's own lines, in this order):**
1. `[Rpg] BEAM sound mob=knell id=<K> when=telegraph tick=<T> key=block.bell.use volume=1.00 pitch=1.00`
2. `[Rpg] BEAM telegraph mob=knell id=<K> gs=100 forced=true tick=<T> lived=<L> at=<K feet> target=player <Ben> from=<K + (0, 3.360, 0)> to=<F + (0, 1.080, 0)> dir=-0.961705 -0.274086 0.000000 range=40.00 ticks=26`, T one or two ticks after the DRIVE
3. `[Rpg] BEAM sound … when=fire tick=<T+26> key=block.bell.resonate volume=1.40 pitch=1.00`, then `[Rpg] BEAM sound … when=fire tick=<T+26> key=block.amethyst_block.chime volume=1.00 pitch=1.00`
4. `[Rpg] BEAM fire … base=65.000 tick=<T+26> … length=12.259 clip=block tolerance=1.00 tested=1 [player <Ben> gap=0.000 hit] dealt=65.000`
5. `[Rpg] BEAM report … tick=<T+26> … to=<F + (-0.047, 1.066, 0.000)>`
6. `HURT before=65.000 after=<65 × 100 / (100 + Dp)> defense=<Dp> dealer=<K> dealerType=wither_skeleton/knell` (59.091 at Ben's 10.000, executed)

**A MOVE** is `[Rpg] TELEPORT mob=knell id=<K> target=player <Ben> tick=<T+26+89> from=… center=… radius=… bearing=… side=…
first=… tried=<n> […] landed=… moved=true`.

### KB1R2 — supersedes GATE-dragon-beam.md KB1: a forced hit at a still player, 26 ticks and 65 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit's six lines exactly as above (the fire at T + 26). Then `CMD /kill @e[tag=kb1r2]`, **exactly one** `[Rpg] TELEPORT skipped mob=knell id=<K> target=<Ben> reason=knell-gone`, and the row's `SETTLED`. **No TELEPORT decision line** | K1, K3, K4 | READ: strike `tick=744`, TELEGRAPH `tick=744 lived=6 at=-97.500 63.000 5.500 … from=-97.500 66.360 5.500 to=-105.500 64.080 5.500 dir=-0.961705 -0.274086 0.000000 range=40.00 ticks=26`; resonance and chime `tick=770`; FIRE `tick=770 … length=12.259 clip=block tolerance=1.00 tested=1 [player <Ben> gap=0.000 hit] dealt=65.000`; REPORT `to=-105.547 64.066 5.500`; `HURT before=65.000 after=59.091 defense=10.000`; after the kill one `TELEPORT skipped … reason=knell-gone`, then `SETTLED`; no decision line. **MATCH** |

### KB2R2 — supersedes GATE-dragon-beam.md KB2 (HOLLOW): the root, READ ON A WALKING KNELL (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (0) `DRIVE spawn tag=kb2r2 <uuid> knell live`, 15 east. (1) **THE CONTROL:** `AT tag=kb2r2 <uuid> x=<x1> …` 60 ticks after the spawn, and five ticks later `x=<x2>`, with **x2 < x1 − 0.01** (walking west, toward him). **If x2 ≥ x1 − 0.01 the row is HOLLOW again, recorded as such, never as a pass.** (2) `BEAM sound … when=telegraph … key=block.bell.use …` and **exactly one** TELEGRAPH, `forced=true`, its `at=` RECORDED. (3) **The fire 26 ticks later, its `at=` equal to the TELEGRAPH's on all three coordinates, to three places**: rooted for the whole 26. (4) The fire: an open line to him, so `[player <Ben> gap=0.000 hit] dealt=65.000`, `length` and `dir` RECORDED (they depend on where it stood); one REPORT; `HURT before=65.000 …`. (5) **It walks again:** the AT line 40 ticks after the force, x3 < the fire's `at` x − 0.01. (6) `CMD /kill @e[tag=kb2r2]`, one `TELEPORT skipped … reason=knell-gone`. **(7) No HURT but the beam's** (it never reached him: K2) | K2 (sight and the speed bound, read and executed), I11 | READ: (0) `knell live`. (1) **CONTROL PASSED:** `x=-93.024`, then `x=-93.381` five ticks later (0.357 west; not HOLLOW). (2) strike and TELEGRAPH `tick=879 lived=66 at=-93.452 63.000 6.002 … dir=-0.981737 -0.185791 -0.040916`. (3) FIRE `tick=905 at=-93.452 63.000 6.002`, **the same to three places: ROOTED for the 26. MATCH.** (4) `[player <Ben> gap=0.000 hit] dealt=65.000 length=18.085`; one REPORT; `HURT before=65.000 after=59.091`. **(5) MISMATCH: the AT 40 ticks after the force (13 after the fire) read `x=-93.452 y=63.000 z=6.002`, the fire's position exactly: it had NOT walked again.** Recorded, not diagnosed, nothing changed. (6) kill, one `TELEPORT skipped … reason=knell-gone`. (7) **MATCH:** no HURT but the beam's |

### KB3R2 — supersedes KB3: the line is fixed at the lock; 3 south, missed (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Lines 1-2 of a forced hit; `CMD /tp @s ~ ~ ~3` after the TELEGRAPH; the fire's two sound lines; `BEAM fire … tick=<T+26> … [player <Ben> gap=2.700 miss] dealt=none`; no REPORT, no HURT; after the kill, **no TELEPORT line** | K4; `GATE-dragon-beam.md` KB3's reading | READ: TELEGRAPH `tick=954`; FIRE `tick=980 … [player <Ben> gap=2.700 miss] dealt=none`; fire sounds at 980; no REPORT, HURT or TELEPORT line. **MATCH** |

### KB4R2 — supersedes KB4: 1.2 south, 0.900 off, still hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| As KB3R2 with `tp @s ~ ~ ~1.2`; `… gap=0.900 hit] dealt=65.000`; one REPORT; `HURT before=65.000 …`; after the kill, **one** `TELEPORT skipped … reason=knell-gone`, **inside this row** (the defect KB4 read on 2026-11-17, closed by the explicit kill) | K4 | READ: FIRE `tick=1055 … gap=0.900 hit] dealt=65.000`; one REPORT; `HURT before=65.000 after=59.091`; one `TELEPORT skipped … reason=knell-gone` **inside this row**, before its `SETTLED`. **MATCH** |

### KB5R2 — supersedes KB5: 1.4 south, 1.100 off, missed (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `… gap=1.100 miss] dealt=none`; no REPORT, no HURT; after the kill, no TELEPORT line | K4 | READ: FIRE `tick=1130 … gap=1.100 miss] dealt=none`; no REPORT, HURT or TELEPORT line. **MATCH** |

### KB6R2 — supersedes KB6: beyond the range, nothing (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `[Rpg] BEAM none mob=knell id=<K> forced=true tick=<t> range=40.00 reason=no-target`; **no BEAM sound line, no TELEGRAPH, no FIRE**; `AT tag=kb6r2 …` 41.000 east of F; after the kill, no TELEPORT line | `GATE-dragon-beam.md` KB6's reading | READ: `BEAM none mob=knell id=<K> forced=true tick=1179 range=40.00 reason=no-target`; no sound, TELEGRAPH or FIRE line; `AT … x=-64.500 y=63.000 z=5.500` (41.000 east of F); no TELEPORT line. **MATCH** |

### KB7R2 — supersedes KB7: a husk in the line keeps its health (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `PROBE tag=kb7r2h <uuid> husk customHP=100.000/100.000 …` before and after; `BEAM fire … tested=2 [player <Ben> gap=0.000 hit, husk <uuid> gap=0.144 spared] dealt=65.000`; one REPORT; one `HURT before=65.000 …`; after the kill, one `TELEPORT skipped … reason=knell-gone` | K4 | READ: the husk `customHP=100.000/100.000` before and after; FIRE `tick=1260 … tested=2 [player <Ben> gap=0.000 hit, husk <uuid> gap=0.144 spared] dealt=65.000`; one REPORT; one `HURT before=65.000`; one `TELEPORT skipped … knell-gone`. **MATCH** |

### KB8R2 — supersedes KB8: a glass wall stops it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `BEAM fire … length=3.639 clip=block tolerance=1.00 tested=1 [player <Ben> gap=4.200 miss] dealt=none`; no REPORT, no HURT; no TELEPORT line | K4 | READ: FIRE `tick=1335 … length=3.639 clip=block tolerance=1.00 tested=1 [player <Ben> gap=4.200 miss] dealt=none`; no REPORT, HURT or TELEPORT line. **MATCH** |

### KB9R2 — supersedes KB9: killed mid-telegraph, nothing fires (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The telegraph's sound line and TELEGRAPH at T; `CMD /kill @e[tag=kb9r2]` 10 ticks after the force; **exactly one** `[Rpg] BEAM cancelled mob=knell id=<K> elapsed=<n>/26 reason=gone`, **1 ≤ n ≤ 25**, n RECORDED; **no fire sound line, no FIRE, no REPORT, no HURT, no TELEPORT line** | I19; `GATE-dragon-beam.md` KB9's reading (`elapsed=3/12`) | READ: strike and TELEGRAPH `tick=1384`; `BEAM cancelled mob=knell id=<K> elapsed=8/26 reason=gone` (n = 8, inside 1 to 25); no fire sound, FIRE, REPORT, HURT or TELEPORT line. **MATCH** |

### KB10R3 — supersedes GATE-dragon-teleport.md KB10R2: the move 89 ticks after the fire, 50 degrees right (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `AT tag=kb10r3 …` at K. (2) The forced hit's six lines (the fire at T + 26). (3) **Exactly one** `TELEPORT … tick=<the FIRE's + 89> from=<K> center=<F> radius=8.000 bearing=0.000 side=right first=50.000 tried=1 [right 50.000 ok y=<F.y> at=<F + (5.142, 0, 6.128)>] landed=<the same> moved=true`. (4) 130 ticks after the force, the AT line at **F + (5.142, 0, 6.128)** | K1, K4 | READ: AT at K `-97.500 63.000 5.500`; the forced hit, FIRE `tick=2186`; `TELEPORT … tick=2275 from=-97.500 63.000 5.500 center=-105.500 63.000 5.500 radius=8.000 bearing=0.000 side=right first=50.000 tried=1 [right 50.000 ok y=63.000 at=-100.358 63.000 11.628] landed=-100.358 63.000 11.628 moved=true` (2275 = 2186 + 89; the landing F + (5.142, 0, 6.128)); AT there. **MATCH** |

### TP2R2 — supersedes TP2: the other side, 50 degrees left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 89, `side=left first=50.000 tried=1 [left 50.000 ok y=<F.y> at=<F + (5.142, 0, −6.128)>] … moved=true`; the AT line at that landing | K1, K4 | READ: FIRE `tick=2326`; `TELEPORT … tick=2415 … side=left first=50.000 tried=1 [left 50.000 ok y=63.000 at=-100.358 63.000 -0.628] … moved=true` (+89; F + (5.142, 0, −6.128)); AT there. **MATCH** |

### TP3R2 — supersedes TP3: the arc's near bound, 30 degrees right (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 89, `side=right first=30.000 tried=1 [right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; the AT line there | K1, K4 | READ: FIRE `tick=2466`; `TELEPORT … tick=2555 … side=right first=30.000 tried=1 [right 30.000 ok y=63.000 at=-98.572 63.000 9.500] … moved=true` (+89; F + (6.928, 0, 4.000)); AT there. **MATCH** |

### TP4R2 — supersedes TP4: the arc's far bound, 70 degrees right (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 89, `side=right first=70.000 tried=1 [right 70.000 ok y=<F.y> at=<F + (2.736, 0, 7.518)>] … moved=true`; the AT line there | K1, K4 | READ: FIRE `tick=2606`; `TELEPORT … tick=2695 … side=right first=70.000 tried=1 [right 70.000 ok y=63.000 at=-102.764 63.000 13.018] … moved=true` (+89; F + (2.736, 0, 7.518)); AT there. **MATCH** |

### TP5R2 — supersedes TP5: a miss moves nothing, through the 89 ticks and more (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| AT at K; the forced beam; `CMD /tp @s ~ ~ ~3` after the TELEGRAPH; `… gap=2.700 miss] dealt=none`; no REPORT, no HURT; **no TELEPORT line of any kind for its Knell** through the 130 ticks; AT at **K** | K1 | READ: AT at K; FIRE `tick=2746 … gap=2.700 miss] dealt=none`; no REPORT, no HURT, no TELEPORT line of any kind through the 130 ticks; AT at K `-97.500 63.000 5.500`. **MATCH** |

**TP6R2 to TP11R2 re-read the stacked boot's walls, pits, steps and sight at the new times; each one's candidates and
landing are predicted VERBATIM as the row it supersedes read on 2026-11-17 (`GATE-dragon-teleport.md`)**, the move now
at the FIRE + 89.

### TP6R2 — supersedes TP6: every right spot walled; it lands left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 89, `side=right first=50.000 tried=6 [right 50.000 no-footing blocked=7 ungrounded=0, right 30.000 no-footing blocked=7 ungrounded=0, right 43.333 no-footing blocked=7 ungrounded=0, right 56.667 no-footing blocked=7 ungrounded=0, right 70.000 no-footing blocked=7 ungrounded=0, left 50.000 ok y=<F.y> at=<F + (5.142, 0, −6.128)>] … moved=true`; the AT line there | K1, K4 | READ: FIRE `tick=2886`; `TELEPORT … tick=2975 … tried=6 [right 50.000 no-footing blocked=7 ungrounded=0, right 30.000 no-footing blocked=7 ungrounded=0, right 43.333 no-footing blocked=7 ungrounded=0, right 56.667 no-footing blocked=7 ungrounded=0, right 70.000 no-footing blocked=7 ungrounded=0, left 50.000 ok y=63.000 at=-100.358 63.000 -0.628] … moved=true`; AT there. **MATCH** |

### TP7R2 — supersedes TP7: walled in on every candidate; none, and it stays (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 89, `tried=10`, all ten `no-footing blocked=7 ungrounded=0`, `landed=none moved=-`; the AT line at K before and after | K1, K4 | READ: FIRE `tick=3026`; `TELEPORT … tick=3115 … tried=10 [` all ten `no-footing blocked=7 ungrounded=0` `] landed=none moved=-`; AT at K before and after. **MATCH** |

### TP8R2 — supersedes TP8: a pit two deep; it stands 2 below his feet (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 89, `tried=1 [right 50.000 ok y=<F.y − 2> at=<F + (5.142, −2, 6.128)>] … moved=true`; the AT line there | K1, K4 | READ: FIRE `tick=3166`; `TELEPORT … tick=3255 … tried=1 [right 50.000 ok y=61.000 at=-100.358 61.000 11.628] … moved=true`; AT there. **MATCH** |

### TP9R2 — supersedes TP9: a step two high; it stands 2 above his feet (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 89, `tried=1 [right 50.000 ok y=<F.y + 2> at=<F + (5.142, 2, 6.128)>] … moved=true`; the AT line there | K1, K4 | READ: FIRE `tick=3306`; `TELEPORT … tick=3395 … tried=1 [right 50.000 ok y=65.000 at=-100.358 65.000 11.628] … moved=true`; AT there. **MATCH** |

### TP10R2 — supersedes TP10: ground four down, refused; the next spot (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 89, `tried=2 [right 50.000 no-footing blocked=0 ungrounded=7, right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; the AT line there | K1, K4 | READ: FIRE `tick=3446`; `TELEPORT … tick=3535 … tried=2 [right 50.000 no-footing blocked=0 ungrounded=7, right 30.000 ok y=63.000 at=-98.572 63.000 9.500] … moved=true`; AT there. **MATCH** |

### TP11R2 — supersedes TP11: no sight, refused; the next spot (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 89, `tried=2 [right 50.000 no-sight y=<F.y>, right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; the AT line there | K1, K4 | READ: FIRE `tick=3586`; `TELEPORT … tick=3675 … tried=2 [right 50.000 no-sight y=63.000, right 30.000 ok y=63.000 at=-98.572 63.000 9.500] … moved=true`; AT there. **MATCH** |

### TP12R2 — supersedes TP12: he steps 2 north during the delay; the circle is around where he ends (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit at F; `CMD /tp @s ~ ~ ~-2` 60 ticks after the force (after the fire, before the move); `TELEPORT … tick=<the FIRE + 89> center=<F + (0, 0, −2)> radius=8.246 bearing=14.036 side=right first=50.000 tried=1 [right 50.000 ok … at=<F + (3.610, 0, 5.414)>] … moved=true`; AT at **F + (3.610, 0, 5.414)** | K1, K4 | READ: FIRE `tick=3726` at F; `CMD /tp @s ~ ~ ~-2` (05:32:46, after the fire, before the move); `TELEPORT … tick=3815 … center=-105.500 63.000 3.500 radius=8.246 bearing=14.036 side=right first=50.000 tried=1 [right 50.000 ok y=63.000 at=-101.890 63.000 10.914] … moved=true` (F + (3.610, 0, 5.414)); AT there. **MATCH** |

### TP13R2 — supersedes TP13: the Knell killed during the delay (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; `CMD /kill @e[tag=tp13r2]` 60 ticks after the force; **exactly one** `TELEPORT skipped mob=knell id=<K> target=<Ben> reason=knell-gone`; **no TELEPORT decision line** | K1 | READ: FIRE `tick=3866`, a hit; `CMD /kill @e[tag=tp13r2]`; exactly one `TELEPORT skipped mob=knell id=<K> target=<Ben> reason=knell-gone`; no decision line. **MATCH** |

### TP14 — NEW: a beam forced while a move is pending is ignored for the teleport (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The first forced hit (FIRE₁ at T₁ + 26, one HURT `before=65.000`). (2) `CMD /rpg heal 1000000` 35 ticks after the first force. (3) The second forced beam, 40 ticks after the first force: its sound lines, TELEGRAPH, FIRE₂ `… gap=0.000 hit] dealt=65.000`, REPORT, one HURT `before=65.000`, and then **exactly one** `[Rpg] TELEPORT ignored mob=knell id=<K> reason=pending`. (4) **Exactly one** TELEPORT decision, at **FIRE₁ + 89** (not FIRE₂ + 89), `side=right first=50.000 … at=<F + (5.142, 0, 6.128)> … moved=true`; the AT line there | K5 | READ: (1) TELEGRAPH₁ `tick=3970`, FIRE₁ `tick=3996 … hit] dealt=65.000`, one `HURT before=65.000 after=59.091`. (2) the heal. (3) TELEGRAPH₂ `tick=4010` (the first force + 40), FIRE₂ `tick=4036 … gap=0.000 hit] dealt=65.000`, REPORT, then `TELEPORT ignored mob=knell id=<K> reason=pending`, one `HURT before=65.000 after=59.091`. (4) exactly one decision, `tick=4085` = **FIRE₁ + 89** (not FIRE₂ + 89 = 4125), `side=right first=50.000 tried=1 [right 50.000 ok y=63.000 at=-100.358 63.000 11.628] … moved=true`; AT there. **MATCH** |

### KB11R3 — supersedes GATE-dragon-teleport.md KB11R2: the live cycle, by bound, unbroken by a move (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE spawn tag=kb11r3f … knell frozen`, `DRIVE spawn tag=kb11r3 … knell live`, `REPLY "The base value for attribute Speed for entity Knell set to 0" []`, `DRIVE teleport_draw tag=kb11r3 … side=right first=50.000`. (2) Its FIRST own TELEGRAPH `forced=false`, **99 ≤ lived ≤ 201**, `at=` K, `ticks=26`, with its strike line just before. (3) Its FIRE 26 later, a hit, `dealt=65.000`; one HURT. (4) **A TELEPORT at that FIRE + 89**, `side=right first=50.000 … landed=<F + (5.142, 0, 6.128)> moved=true`. (5) **THE CLOCK UNBROKEN:** its NEXT own TELEGRAPH `forced=false` at that landing, its `lived` − the first's − 26 **within 100 to 200** (the wait is drawn at the fire; the move, 89 later, touches nothing of it). (6) Every later own beam the same way; each hit's move at its FIRE + 89, its draw the production one, RECORDED, `radius=8.000`. (7) No line names the kb11r3f uuid. (8) Between two heal lines at most one HURT. (9) At the end the row kills both; a pending move or telegraph is cancelled INSIDE the row: `TELEPORT skipped … knell-gone` or `BEAM cancelled …`, RECORDED | B9, I1, I2, I23; K1 | READ: (1) both spawns, `REPLY "The base value for attribute Speed for entity Knell set to 0" []`, `teleport_draw … side=right first=50.000`. (2) first own TELEGRAPH `forced=false tick=4288 lived=184` (inside 99 to 201) at K, `ticks=26`, its strike at 4288. (3) FIRE `tick=4314` hit `dealt=65.000`; one HURT. (4) `TELEPORT … tick=4403` (+89) `side=right first=50.000 … landed=-100.358 63.000 11.628 moved=true`. (5) next own TELEGRAPH `tick=4437 lived=333` at that landing: 333 − 184 − 26 = **123**, inside 100 to 200. (6) FIRE `tick=4463` hit; `TELEPORT … tick=4552` (+89) `bearing=50.000 side=right first=53.202 … landed=-107.327 63.000 13.289` (the production draw), `radius=8.000`; third own TELEGRAPH `tick=4563 lived=459`: 459 − 333 − 26 = **100**, at the bound, 11 ticks after the move; FIRE `tick=4589` hit. (7) no line names `a13a0733` but its spawn. (8) heals at 05:33:08, :13, :18, :23, :28; HURTs at :14, :21, :27, one between each pair. (9) the kills; one `TELEPORT skipped … knell-gone` for the third hit's move, inside the row. **MATCH** |

---

## BEN'S ROWS — BY THE ASK STEP, SURVIVAL, DIFFICULTY NORMAL

**AUTO-PARTIAL:** the BEAM, sound and TELEPORT lines are the server's half (what it SENT); what he heard, saw and felt
is his half, and only his answers read it. The run sheet tells him what to listen for, and the selftest says it again
in gold chat before each beam.

### KQ1R2 — supersedes GATE-dragon-beam.md KQ1: the bell's warning, the 1.3 seconds, the 65 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Server half:** a heal; the gold line; the first forced beam from a frozen Knell 12 east, its strike line at the lock, its resonance and chime at the fire 26 later; a hit if he stood still (`dealt=65.000`, one HURT `before=65.000`); its move 89 later (the production draw, RECORDED). ASK 1 and ASK 2; a heal; the second gold line; at least 100 ticks; the second forced beam, its outcome **his doing, RECORDED**; ASK 3; the kill (a pending move then skipped, `knell-gone`, RECORDED). **His half, yes/no:** ASK 1 *"Did the bell strike warn you before the beam?"*; ASK 2 *"Did 65 feel right for a single shot?"*; ASK 3 *"With 1.3 seconds, could you step out of the way in time?"* | K3; Ben's rulings | READ, server half: the heal; the gold line; TELEGRAPH `tick=1519 lived=61 at=-93.500 63.000 5.500` with its strike; resonance and chime at FIRE `tick=1545 … length=18.001 … [player <Ben> gap=0.000 hit] dealt=65.000`; `HURT before=65.000 after=59.091`; `TELEPORT … tick=1634` (+89) `radius=12.000 bearing=0.000 side=right first=61.943 tried=1 [right 61.943 ok y=63.000 at=-99.856 63.000 16.090] … moved=true`. The second force from that landing, TELEGRAPH `tick=1838`, FIRE `tick=1864 … [player <Ben> gap=2.377 miss] dealt=none`: **he stepped out of it**; so no second HURT, no second move, and no skip line at the kill. His half, verbatim: `ASK 1 question="Did the bell strike warn you before the beam?" answer="yes" ticks=114`; `ASK 2 question="Did 65 feel right for a single shot?" answer="yes" ticks=65`; `ASK 3 question="With 1.3 seconds, could you step out of the way in time?" answer="yes" ticks=87` |

### TQ1R2 — supersedes GATE-dragon-teleport.md TQ1: the move as the ringing ends (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Server half:** a heal; the gold line; one forced beam from a frozen Knell 12 east, a hit; the resonance and chime lines at the fire; **one TELEPORT at the FIRE + 89**, `radius=12.000 bearing=0.000`, its draw the production one, RECORDED. **His half, yes/no:** ASK 1 *"Did the Knell move as the ringing ended?"*; ASK 2 *"Did that wait feel right?"* | K1, K3; the measured resonance (3.780 s; 4.447 s at the slowest variant, playback speed = pitch assumed) | READ, server half: the heal; the gold line; TELEGRAPH `tick=4775`; resonance and chime at FIRE `tick=4801 … hit] dealt=65.000`; one HURT; `TELEPORT … tick=4890` (+89) `radius=12.000 bearing=0.000 side=left first=49.135 tried=1 [left 49.135 ok y=63.000 at=-97.649 63.000 -3.575] … moved=true`. His half, verbatim: `ASK 1 question="Did the Knell move as the ringing ended?" answer="yes" ticks=130`; **`ASK 2 question="Did that wait feel right?" answer="no" ticks=45`**. The "yes" to ASK 1 is his ear, not a measurement of "playback speed equals pitch" (the seat's ruling) |
