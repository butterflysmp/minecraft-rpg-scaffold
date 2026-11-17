# GATE — THE KNELL, 4: THE TELEPORT 28 TICKS AFTER THE SHOT, A SECOND BELL, AND A SERVER STOP THAT RESTORES

**Status: NOT RUN.** Every prediction below was written **before** any boot that counts for this gate, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code, the content AND this
gate, and Ben says ready.**

**2026-11-17: approved to merge by the review seat after the boot of 0b4037ec.**

> **DATED NOTE, 2026-11-17, the shot in the middle (`GATE-dragon-knell-5.md`). NOTHING BELOW IS EDITED.** Every row of both
> Knell scenario files is now that gate's, renamed with the next suffix: **KB1R4-KB9R4, WA1R2 and KQ1R4; KB10R5, TP2R4-TP13R4,
> TP14R3, KB11R5 and TQ1R4 are SUPERSEDED** by KB1R5-KB9R5, WA1R3 (re-timed) and KQ1R5; KB10R6, TP2R5-TP13R5, TP14R4
> (re-purposed: its pending case is unreachable at 24 and 24), KB11R6 and TQ1R5. RE1 is re-predicted there unchanged.

> **READ, 2026-11-17: booted at `0b4037ec` on the seat's approval and Ben's "ready".** Server log `07:45:15` to the
> clean `/stop` at `07:49:36`. Carried state: no JVM; backup `rpg-run-backups/2026-11-17-pre-dragon-knell-4-2`
> (`run/plugins/Rpg` 129 files, 386884 bytes; `run/world` 193 files, 204963703 bytes; `diff -rq lines: 0` each);
> `12:  dev-pack-on-join: true`. Ben logged in at `07:45:23`, typed `/gamemode survival` first (`07:45:36`), then each
> selftest once, each to `SELFTEST RUN FINISHED`, then `/stop`.
>
> **The result: every AUTO row AS PREDICTED, 28 of 28, RE1 included.** The server-stop restore was not exercised (no
> run was stopped): it stays pinned by the wiring test only and UNREAD on a server.
>
> **Ben's three answers: "yes" (the higher bell at the shot gives it what was missing); "no" (the lower first bell is
> not better than before); and, to "Does the whole sequence feel right now?", "the timing of the warning and teleport
> are good, I want the shot to be in the middle"**, his words, not yes or no, recorded verbatim.
>
> **Counts:** 28 TELEGRAPH lines, each with exactly two sound lines at its tick, `block.bell.use pitch=0.80` then
> `block.bell.resonate`; 27 FIRE lines (the 28th telegraph is KB9R4's, cancelled `elapsed=8/20`), each with exactly two,
> `block.bell.use pitch=1.50` then `block.amethyst_block.chime`; **every fire 20 ticks after its telegraph (27 of
> 27)**; 16 TELEPORT decisions, **every one 28 ticks after the fire of the hit that scheduled it (16 of 16)**; 5
> `skipped … knell-gone`, each inside its own row; 1 `ignored … pending` (TP14R3). 22 hits and 22 HURT lines, each
> `before=65.000 after=59.091 defense=10.000 dealerType=wither_skeleton/knell`: no hit from anything but a beam. Every
> LEFTOVER line `living=0`. Both runs: `DIFFICULTY found=NORMAL set=NORMAL`, `RESTORED … difficulty=NORMAL`, and no
> `ABORTED` or `NOT-RESTORED` line.

> **DATED NOTE, 2026-11-17: THIS GATE'S PREDICTIONS WERE REWRITTEN IN THIS PR, AND THAT IS ALLOWED, BECAUSE NO ROW OF
> IT HAS BEEN READ.** The first version (committed at `90768891`, predicting the 28 alone) was booted once and that run
> was stopped 23 seconds in (the note below); **a stopped run is a note, not a reading**, so no prediction here had been
> read when Ben then ruled a second bell. The predictions now carry his second bell and the restore on a server stop.
> The first version stands in this file's history at `90768891`.

> **THE STOPPED RUN OF 90768891, 2026-11-17. NONE OF IT COUNTS AS THIS GATE'S READING.** R0a-R0d passed (`[Rpg] Build:
> 90768891`; `delay_ticks: 28` PRESENT and `44` ABSENT with the controls; the Loaded line verbatim; `117 scenarios across
> 17 gates`). Ben typed `/stop` at 07:23:18, **23 seconds in, during TP4R4**. **KB10R5, TP2R4 and TP3R4 read as
> predicted, their moves at the fire + 28** (14108 → 14136, 14248 → 14276, 14388 → 14416), landing at F + (5.142, 0,
> 6.128), F + (5.142, 0, −6.128) and F + (6.928, 0, 4.000). **TP4R4 fired and hit** (`tick=14528`, `dealt=65.000`) and
> **its move was not read.** TP14R3 and both ASK questions were not reached. **No `SELFTEST RUN ABORTED` and no
> `RESTORED` line**: the plugin was disabled mid-row and TP4R4's staging was saved into the world (B5 below). The world
> was restored from `rpg-run-backups/2026-11-17-pre-dragon-knell-4` on Ben's word (193 files, 204963703 bytes both;
> `diff -rq` 0 lines), after a copy of the world as the stop left it was kept at
> `rpg-run-backups/2026-11-17-stopped-run-90768891`.

**What this gate predicts, and why.**
- **The teleport 28 ticks after the fire.** Ben, after the boot of `9d0cbffb`: *"let's try 1.4 seconds after the shot"*.
- **A second bell.** Ben, after the stopped run (`PLAN-dragon-mobs.md`, RULINGS): *"The timing is perfect on the sounds
  but something is still missing"*; *"Add a second, higher pitched bell for the actual shot. Maybe even lower the pitch
  of the initial bell"*. **The bell, at pitch 0.8, then the resonance at the telegraph; the bell at pitch 1.5, then the
  chime at the fire.** That there is a second, higher bell and that the first is lower are Ben's; **0.8 and 1.5 are the
  seat's starting values.**
- **Every row of both Knell scenario files prints the beam's sound lines**, so every row's lines change, and every row
  is predicted HERE under the next suffix (KB1R3 → KB1R4, WA1 → WA1R2, KQ1R3 → KQ1R4 in the beam file; the teleport
  file's KB10R5 … TQ1R4, TP5R4 and TP13R4). **Both files are paired with THIS gate** (`SelfTestScenariosTest.docOf`;
  the note in `selftest/index.yml`). The old rows' predictions and readings stay in `GATE-dragon-knell-3.md`, under a
  dated note. **KQ1's first move at the fire + 28, unread until now, is predicted here** (KQ1R4).
- **A server stop during a run now restores** (B5). **Pinned by `SelfTestDisableWiringTest` ONLY and NEVER READ ON A
  SERVER**: the seat will not ask Ben to stop a run on purpose. The one new row, RE1, reads the NORMAL end unchanged.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL.
- **The difficulty is set by the selftest:** each gate's setup `difficulty: normal`; `DIFFICULTY found=… set=NORMAL`;
  `RESTORED … difficulty=<what it found>`.
- **Gear score 100 on every Knell:** a hit is **65.000** before defence.

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC, ROW BY ROW

**The floor:** his maximum is at least 100 (it read **350**: `REPLY "Custom HP: 350/350"`). **65 before defence leaves at
least 35**; every row heals him before every beam that can land, and no row lands two beams between heals. **No Knell
can reach him:** every Knell is frozen (NoAI) except KB11R5's (walking speed base 0, 8 blocks away) and the two walking
Knells of KB2R4 and WA1R2, which are killed before they could close to melee (K4).

| row | what can hit him after a heal | the worst case, before defence |
|---|---|---|
| KB1R4, KB4R4, KB7R4, KB2R4, KQ1R4 | one forced beam that hits | **65** |
| KB3R4, KB5R4, KB8R4, WA1R2 | one forced beam that misses | **0** predicted, 65 if it hit |
| KB6R4, KB9R4 | none (out of range; killed mid-telegraph) | **0** |
| KB10R5, TP2R4-TP4R4, TP6R4-TP13R4, TQ1R4 | one forced beam that hits | **65** |
| TP5R4 | a miss | **0** |
| TP14R3 | two forced beams, **a heal between** (23 ticks after the first force, after the first fire) | **65**, then **65** |
| KB11R5 | its own beams, at least 120 ticks apart, a heal every 100 ticks | **65 between two heals** |

## WHAT THE ROWS STAND ON

- **K1. The timings, READ at the boot of `9d0cbffb`:** every fire exactly 20 ticks after its telegraph (29 of 29), every
  move exactly `delay_ticks` after the fire of the hit that scheduled it (17 of 17); and at the stopped run of
  `90768891`, 3 of 3 moves at the fire + 28. `knell.yml`; `MobLoaderTest`.
- **K2. The sounds, READ in our code and at `9d0cbffb`:** `MobBeamer.playSounds` plays the authored list in order and
  logs one `BEAM sound` line each at the tick it plays, the telegraph's just before the TELEGRAPH line and the fire's
  just before the FIRE line (30 of 30 telegraphs and 29 of 29 fires in order at that boot). The new lines are the new
  list (`MobLoaderTest.theBundledKnellBeamsWithTwoBells`). **What the client played, and how it sounded, is Ben's.**
  **Lengths, still assuming playback speed equals pitch (not read):** the bell file is 2.492 s, so 3.115 s at 0.8 and
  1.661 s at 1.5.
- **K3. The geometry, READ at `9d0cbffb`:** every landing, wall, pit, step and sight line, relative to Ben's feet F and a
  frozen Knell's K = F + (8, 0, 0); the beam rows' gaps, lengths and directions. **None depends on the delay or the
  sounds**, only the ticks and the sound lines.
- **K4. The walking Knells (KB2R4, WA1R2), READ and EXECUTED for `GATE-dragon-knell-3.md`'s K2 and READ at `9d0cbffb`:**
  sight in the open, a 15-block start inside the 16-block follow range, at most 0.0947 blocks a tick, killed before
  they can walk the 12 blocks melee needs (margins 3.76 and 2.53 blocks). WA1 read a drop of 1.562 blocks 30 ticks after
  its fire. **The bound on when it walks again (at most 21 ticks after the fire) stays INFERRED.**
- **K5. A forced beam while a move is pending, READ at `9d0cbffb` (TP14R2):** `TELEPORT ignored … reason=pending`, the
  first move at its own fire + delay; a force is refused only during a telegraph (`MobBeamer#force`).

## WHAT A SERVER STOP DURING A RUN NOW DOES (B5) — PINNED BY A WIRING TEST, NEVER READ ON A SERVER

**THE DEFECT, READ at the stopped run:** the log reads `Stopping server`, `[Rpg] Disabling Rpg`, `Saving players`, the
quit, `Saving worlds`, with no ABORTED and no RESTORED line.

- **What a disconnect does that a disable did not:** `RpgListeners.onQuit` hands the quit to `SelfTestService.onQuit`,
  which ends the run, and `SelfTestRun.end` restores through the plugin's scheduler (mob removal on each mob's
  scheduler, the game rules, time and difficulty on the global one) and `teleportAsync`. **On a `/stop` the quit comes
  AFTER the plugin is disabled, so our listener never sees it, and nothing in `onDisable` reached the run.** READ in our
  code.
- **The order on `/stop`, READ** (`~/.jdks/openjdk-26.0.1/bin/javap -c -p -cp run/versions/26.1.2/paper-26.1.2.jar
  net.minecraft.server.MinecraftServer`): `MinecraftServer#stopServer` calls `CraftServer.disablePlugins` (offset 141)
  **before** `PlayerList.saveAll` (185), `PlayerList.removeAll` (196) and `saveAllChunks` (312). **So no save can
  precede the disable, and a synchronous restore in `onDisable` is written before both saves.**
- **What `onDisable` cannot use, READ** (`javap -c -p -cp <the paper-api jar> org.bukkit.plugin.java.JavaPlugin` and
  `… org.bukkit.craftbukkit.scheduler.CraftScheduler` in the server jar): `JavaPlugin#setEnabled(false)` clears
  `isEnabled` (offset 10) **before** it calls `onDisable` (offset 59), and `CraftScheduler#validate` checks
  `Plugin.isEnabled`. **So the plugin's scheduler is refused inside `onDisable`** (the refusal's exact exception is
  INFERRED). **What IS usable:** the server's main thread itself, so every Bukkit call made directly; and the logger.
- **THE FIX, as built:** `RpgPlugin.onDisable` calls `SelfTestService.onDisable` FIRST (before the profile flush, which
  must carry the lifetime XP and cell the restore writes back). It calls `SelfTestRun.endAtDisable`, which runs **the
  same restore** through an `InlineScheduler` (every task at once, on the calling thread) and teleports with
  `teleport`, not `teleportAsync`. **It restores what a normal end restores:** the row's and the run's blocks, the mobs
  it tagged and the orbs, the player's inventory, held slot, game mode, position, lifetime XP, cell, mana, the trace,
  `spawn_mobs`, `keep_inventory`, `immediate_respawn`, time and difficulty. It logs `SELFTEST RUN ABORTED
  reason=server-stop` and the same `SELFTEST RUN RESTORED …` line a normal end logs.
- **WHAT IT CANNOT RESTORE IS LOGGED BY NAME:** any task the restore asks for LATER or off-thread is refused by the
  `InlineScheduler` and logged as `SELFTEST RUN NOT-RESTORED at server-stop: a task asked for <what>`. **The restore asks
  for none today**, so none is expected.
- **STILL UNBUILT, by the seat's scope: a crash-survival file for a JVM that dies without disabling** (a kill, a power
  loss). It would need the run's snapshot (the saved blocks with their states, the tagged mobs' ids, the player's saved
  state, the game rules) written to disk at RUN START and at every row's staging, and read back at the next enable,
  before any player joins, with its own restore and log line.

## THE ROW AUDIT: WHERE EVERY TELEPORT LINE LANDS AT FIRE + 28

**A forced beam's telegraph comes one or two ticks after the BEAM step (the force), so its fire is at the force + 21 or
22, and a hit's move at the force + 49 or 50.** The same offset applies to every force in a row.

| file | row | where its TELEPORT line lands |
|---|---|---|
| beam | KB1R4, KB4R4, KB7R4, KB2R4 (hits) | the kill at the force + 40, **9 ticks before the move**: one `TELEPORT skipped … knell-gone` inside the row |
| beam | KB3R4, KB5R4, KB8R4, WA1R2 (misses); KB6R4, KB9R4 (no fire) | none |
| beam | KQ1R4 | **the move at its FIRE + 28** (the production draw), during the 60-tick wait before the ASKs |
| teleport | KB10R5, TP2R4-TP4R4, TP6R4-TP11R4 | the move at the force + 49 or 50, the read at + 130 |
| teleport | TP5R4 | none (a miss) |
| teleport | TP12R4 | he steps north at + 40, **9 ticks before the move** |
| teleport | TP13R4 | the kill at + 35, **14 ticks before the move**: one skip line |
| teleport | TP14R3 | the heal and the second force at + 23; the second fire at + 44, **5 ticks before the first move** at + 49: one `ignored … pending` |
| teleport | KB11R5 | each move at its fire + 28; anything pending at the end cancelled inside the row |
| teleport | TQ1R4 | the move at + 49 or 50, before the ASK |
| merged | DB1R2, DB11R2, WS-C, WS2c | never (frozen, or no Knell) |
| closed manual gates (13) | a live Knell's own beam | **a finding, carried**: now two bells and a move 28 ticks after a hit; not re-predicted |

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-dragon-knell-4"; [ -e "$B" ] && B="$B-2"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
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
   before the next command.** About **1 minute** plus the time you take to answer (its waits sum to 0.62 minutes; with
   each row's clean and settle, the same eleven rows took 69 seconds at the last full boot, answers included). Stand
   still and touch nothing. **In its last part** a Knell twelve blocks east beams you once: **listen for a low bell and
   the ringing at the warning, and a higher bell at the shot one second later.** Two questions; answer each `yes` or
   `no`.
3. `/rpg selftest dragon-teleport` — **ONCE. Wait for the chat to say the run finished, and answer every gold
   question, before the next command.** About **2½ minutes** plus the time you take to answer (its waits sum to 2.17
   minutes; the same sixteen rows took 153 seconds at the last full boot, answers included). Stand still and touch
   nothing. **In its last part** a Knell twelve blocks east beams you once: the same two bells, and the Knell moving
   1.4 seconds after the shot. One question; answer `yes` or `no`.
4. `/stop`

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-knell-4` prints it, shortened. **Not** master's `6b587faa`, **not** the stopped run's `90768891`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1`, after this boot's `Done` | READ 2026-11-17: `[07:45:15] [Server thread/INFO]: [Rpg] Build: 0b4037ec`, this PR's head (`git ls-remote` `0b4037ec2c61…`). **MATCH** |

### R0b — the jar carries the 28, the two bells and the restore

| prediction | instrument | READING |
|---|---|---|
| In `content/mobs/knell.yml`: PRESENT `  delay_ticks: 28`, `      - { key: block.bell.use, volume: 1.0, pitch: 0.8 }`, `      - { key: block.bell.use, volume: 1.0, pitch: 1.5 }`; ABSENT `  delay_ticks: 44`, `      - { key: block.bell.use, volume: 1.0, pitch: 1.0 }`. Classes: `scheduler/InlineScheduler.class` PRESENT; `selftest/SelfTestRun.class` PRESENT `reason=server-stop` and `NOT-RESTORED at server-stop`. Keys: `GATE-dragon-beam.yml` `KB1R4:`, `WA1R2:`, `KQ1R4:` PRESENT, `KB1R3:` ABSENT; `GATE-dragon-teleport.yml` `TP5R4:`, `TP13R4:` PRESENT, `TP5R3:` ABSENT. **The controls, the same on both builds:** `knell.yml` PRESENT `  telegraph_ticks: 20`; `MobBeamLines.class` PRESENT `BEAM sound mob=`; `GATE-dragon-blast.yml` `BQ1R2:` PRESENT | the scan below | READ: all 18 lines as predicted: `delay_ticks: 28` and both new bell lines PRESENT, `delay_ticks: 44` and the pitch-1.0 bell ABSENT; `InlineScheduler.class` PRESENT; `reason=server-stop` and `NOT-RESTORED at server-stop` PRESENT in `SelfTestRun.class`; `KB1R4:`, `WA1R2:`, `KQ1R4:`, `TP5R4:`, `TP13R4:` PRESENT, `KB1R3:` and `TP5R3:` ABSENT; the three controls PRESENT. **MATCH** |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$k = Bytes 'content/mobs/knell.yml'
foreach ($l in '  delay_ticks: 28', '      - { key: block.bell.use, volume: 1.0, pitch: 0.8 }', '      - { key: block.bell.use, volume: 1.0, pitch: 1.5 }', '  delay_ticks: 44', '      - { key: block.bell.use, volume: 1.0, pitch: 1.0 }', '  telegraph_ticks: 20') {
  if ($k -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT knell.yml '$l'" } else { "ABSENT  knell.yml '$l'" }
}
if ((Bytes 'io/github/butterflysmp/rpg/paper/scheduler/InlineScheduler.class') -ne $null) { "PRESENT scheduler/InlineScheduler.class" } else { "ABSENT  scheduler/InlineScheduler.class" }
foreach ($c in @(@('selftest/SelfTestRun.class', 'reason=server-stop'), @('selftest/SelfTestRun.class', 'NOT-RESTORED at server-stop'), @('health/MobBeamLines.class', 'BEAM sound mob='))) {
  $b = Bytes "io/github/butterflysmp/rpg/paper/$($c[0])"
  if ($b -eq $null) { "MISSING $($c[0])" } elseif ($b.Contains($c[1])) { "PRESENT $($c[0]) '$($c[1])'" } else { "ABSENT  $($c[0]) '$($c[1])'" }
}
foreach ($q in @(@('dragon-beam', 'KB1R4'), @('dragon-beam', 'WA1R2'), @('dragon-beam', 'KQ1R4'), @('dragon-beam', 'KB1R3'), @('dragon-teleport', 'TP5R4'), @('dragon-teleport', 'TP13R4'), @('dragon-teleport', 'TP5R3'), @('dragon-blast', 'BQ1R2'))) {
  $y = Bytes "selftest/GATE-$($q[0]).yml"
  if ($y -eq $null) { "MISSING selftest/GATE-$($q[0]).yml" } elseif ($y -match "(?m)^$($q[1]):\s*$") { "PRESENT $($q[0]) $($q[1]):" } else { "ABSENT  $($q[0]) $($q[1]):" }
}
$zip.Dispose()
```

**A jar of master `6b587faa` reads the opposite on the five `knell.yml` lines that move, ABSENT on `InlineScheduler.class`
and on both `SelfTestRun.class` texts, ABSENT on the five new keys and PRESENT on `KB1R3:` and `TP5R3:`**; the three
controls read PRESENT on both. (The stopped run's `90768891` reads PRESENT on `delay_ticks: 28` and ABSENT on everything
else new.)

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim the boot of `9d0cbffb`'s. No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN` beyond `volley_stone`'s known pair. **A `Skipping malformed mob 'knell.yml'` line would mean a sound was refused** (`4 mobs`) | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | READ: the Loaded line verbatim as predicted; no `Refusing`, `Skipping`, `SEVERE` or `Exception` in the whole log; the only `[Rpg]` WARN lines are `volley_stone`'s known pair (and Paper's own update banner, not ours). **MATCH** |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 117 scenarios across 17 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport]`**, and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | READ: `[Rpg] Selftest: dev server, 117 scenarios across 17 gates [melee-cell, … dragon-beam, dragon-teleport]`, the list verbatim; no `Refusing selftest`. **MATCH** |

**The 117 is RECOUNTED from the files at this branch's head:** in `paper/src/main/resources/selftest/`,
`grep -cE '^  - ' index.yml` prints **17**; `cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/;
s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints **134**; the same with `'^setup:'` prints **17**: 134 − 17 = **117**.
**This PR renames rows and adds none, so master `6b587faa` prints the same 117; R0b's keys are the control.**
`SelfTestScenariosTest` pins the 117 and every row name.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument:** `Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-(beam|teleport)|SELFTEST RUN|\[Rpg\] (BEAM|TELEPORT)'`,
after Ben's LOGIN, inside each row's own `START … DONE` bracket.

**In EVERY row (not repeated):** `LEFTOVER living=0 radius=16` after its CLEAN; each spawn's `DRIVE spawn tag=<tag>
<uuid> knell frozen` (or `live`) and its `REPLY "Spawned Knell (wither_skeleton, 360 HP, GS 100 given)" [aqua, gray]`;
each `DRIVE beam` with no `refused=`; each `CMD /kill @e[tag=<tag>]` followed by `SETTLED dying=0 ticks=<t> first=1`.
**No `[Rpg]` line names a Knell that is not the row's own.**

**EVERY TELEGRAPH, at its tick T, is preceded by exactly two sound lines, in this order:**
`[Rpg] BEAM sound mob=knell id=<K> when=telegraph tick=<T> key=block.bell.use volume=1.00 pitch=0.80`, then
`… when=telegraph tick=<T> key=block.bell.resonate volume=1.40 pitch=1.00`.
**EVERY FIRE, exactly 20 ticks later, is preceded by exactly two, in this order:**
`… when=fire tick=<T+20> key=block.bell.use volume=1.00 pitch=1.50`, then
`… when=fire tick=<T+20> key=block.amethyst_block.chime volume=1.00 pitch=1.00`.

**A FORCED HIT from a frozen Knell 8 east:** the two telegraph sounds and `BEAM telegraph … tick=<T> … dir=-0.961705
-0.274086 0.000000 range=40.00 ticks=20`; the two fire sounds and `BEAM fire … tick=<T+20> … length=12.259 clip=block
tolerance=1.00 tested=1 [player <Ben> gap=0.000 hit] dealt=65.000`; one REPORT; `HURT before=65.000 after=59.091
defense=10.000 dealer=<K> dealerType=wither_skeleton/knell`. **A MOVE** is `[Rpg] TELEPORT … tick=<FIRE + 28> …`.

### KB1R4 — supersedes GATE-dragon-knell-3.md KB1R3: a forced hit, the two bells (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit's lines exactly as above. Then the kill, **exactly one** `TELEPORT skipped mob=knell id=<K> target=<Ben> reason=knell-gone`, then `SETTLED`; no decision line | K1, K2, K3 | READ (F = -100.500 63.000 5.500 in both runs): both telegraph sounds (bell 0.80, then the resonance) and both fire sounds (bell 1.50, then the chime) at their ticks; TELEGRAPH `tick=544 … dir=-0.961705 -0.274086 0.000000 range=40.00 ticks=20`; FIRE `tick=564 … length=12.259 clip=block tolerance=1.00 tested=1 [player <Ben> gap=0.000 hit] dealt=65.000`; one REPORT; one HURT `before=65.000 after=59.091 defense=10.000`; the kill, one `TELEPORT skipped … reason=knell-gone`, then `SETTLED`. **MATCH** |

### KB2R4 — supersedes KB2R3: the root on a walking Knell (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (0) `knell live`, 15 east. (1) **THE CONTROL:** two AT lines 5 ticks apart, the second's x < the first's − 0.01, or the row is HOLLOW. (2) the two telegraph sounds and one TELEGRAPH `forced=true ticks=20`, its `at=` RECORDED. (3) the FIRE 20 later, its `at=` the TELEGRAPH's on all three coordinates. (4) the two fire sounds; `[player <Ben> gap=0.000 hit] dealt=65.000`, `length` and `dir` RECORDED; one REPORT; one HURT. (5) the AT 40 ticks after the force: RECORDED. (6) the kill, one skip line. (7) no HURT but the beam's | K2, K4 | READ: (0) `knell live`. (1) **CONTROL PASSED:** `x=-87.953`, then `x=-88.310`. (2) both telegraph sounds (bell 0.80, then the resonance) and both fire sounds (bell 1.50, then the chime) at their ticks; TELEGRAPH `tick=679 lived=66 at=-88.381 63.000 6.002 … ticks=20`. (3) FIRE `tick=699 at=-88.381 63.000 6.002`, **the same: ROOTED.** (4) `length=18.188 … [player <Ben> gap=0.000 hit] dealt=65.000`, `dir=-0.981945 -0.184736 -0.040693`; one REPORT; one HURT. (5) RECORDED: the AT 40 ticks after the force `x=-89.028 y=63.000 z=6.995`. (6) one skip line. (7) no HURT but the beam's. **MATCH** |

### WA1R2 — supersedes WA1: the Knell walks again after a beam (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (0) `knell live`, 15 east. (1) **THE CONTROL**, as KB2R4's, or HOLLOW. (2) the two telegraph sounds, one TELEGRAPH at T. (3) `CMD /tp @s ~ ~ ~3`. (4) the two fire sounds and FIRE `tick=<T+20>`, `at=` the TELEGRAPH's, **`[player <Ben> gap=<g> miss] dealt=none`, g ≥ 2.0**; no REPORT, no HURT, **no TELEPORT line**. (5) **THE WALK:** the AT 51 ticks after the force; its horizontal distance from F + (0, 0, 3) **less than the FIRE's `at=` distance by more than 0.20**. (6) the kill; no TELEPORT line. (7) no HURT | K2, K4 | READ: (1) **CONTROL PASSED:** `x=-88.024`, then `x=-88.381`. (2) both telegraph sounds (bell 0.80, then the resonance) and both fire sounds (bell 1.50, then the chime) at their ticks; TELEGRAPH `tick=814 lived=66 at=-88.452 63.000 6.002`. (3) the step. (4) FIRE `tick=834 at=-88.452 63.000 6.002` (rooted) `… [player <Ben> gap=2.580 miss] dealt=none`; no REPORT, HURT or TELEPORT line. (5) **THE WALK:** the AT 51 ticks after the force `x=-89.915 y=63.000 z=7.003`; from F + (0, 0, 3) = (-100.500, 8.500): **12.304 at the fire, 10.690 at fire + 30, a drop of 1.614 > 0.20** (executed). (6) the kill; no TELEPORT line. (7) no HURT. **MATCH** |

### KB3R4 — supersedes KB3R3: the line fixed at the lock; 3 south, missed (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The telegraph's lines; `CMD /tp @s ~ ~ ~3`; the fire's sounds; `BEAM fire … tick=<T+20> … [player <Ben> gap=2.700 miss] dealt=none`; no REPORT, HURT or TELEPORT line | K3 | READ: TELEGRAPH `tick=900`; FIRE `tick=920 … [player <Ben> gap=2.700 miss] dealt=none`; no REPORT, HURT or TELEPORT line. **MATCH** |

### KB4R4 — supersedes KB4R3: 1.2 south, still hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `… gap=0.900 hit] dealt=65.000`; one REPORT; one HURT; after the kill one `TELEPORT skipped … knell-gone`, inside this row | K3 | READ: FIRE `tick=995 … gap=0.900 hit] dealt=65.000`; one REPORT; one HURT; one `TELEPORT skipped … knell-gone` inside this row. **MATCH** |

### KB5R4 — supersedes KB5R3: 1.4 south, missed (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `… gap=1.100 miss] dealt=none`; no REPORT, HURT or TELEPORT line | K3 | READ: FIRE `tick=1070 … gap=1.100 miss] dealt=none`; no REPORT, HURT or TELEPORT line. **MATCH** |

### KB6R4 — supersedes KB6R3: beyond the range, nothing (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `BEAM none mob=knell id=<K> forced=true tick=<t> range=40.00 reason=no-target`; **no BEAM sound line**, no TELEGRAPH, no FIRE; `AT …` 41.000 east of F; no TELEPORT line | K3 | READ: `BEAM none mob=knell id=<K> forced=true tick=1125 range=40.00 reason=no-target`; no sound, TELEGRAPH or FIRE line; `AT … x=-59.500 y=63.000 z=5.500` (41.000 east of F); no TELEPORT line. **MATCH** |

### KB7R4 — supersedes KB7R3: a husk in the line keeps its health (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| the husk `customHP=100.000/100.000` before and after; `BEAM fire … tested=2 [player <Ben> gap=0.000 hit, husk <uuid> gap=0.144 spared] dealt=65.000`; one REPORT; one HURT; one `TELEPORT skipped … knell-gone` | K3 | READ: the husk `customHP=100.000/100.000` before and after; FIRE `tick=1200 … tested=2 [player <Ben> gap=0.000 hit, husk <uuid> gap=0.144 spared] dealt=65.000`; one REPORT; one HURT; one skip line. **MATCH** |

### KB8R4 — supersedes KB8R3: a glass wall stops it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `BEAM fire … length=3.639 clip=block tolerance=1.00 tested=1 [player <Ben> gap=4.200 miss] dealt=none`; no REPORT, HURT or TELEPORT line | K3 | READ: FIRE `tick=1275 … length=3.639 clip=block tolerance=1.00 tested=1 [player <Ben> gap=4.200 miss] dealt=none`; no REPORT, HURT or TELEPORT line. **MATCH** |

### KB9R4 — supersedes KB9R3: killed mid-telegraph, nothing fires (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **The two telegraph sound lines** and the TELEGRAPH at T; the kill 10 ticks after the force; **exactly one** `BEAM cancelled mob=knell id=<K> elapsed=<n>/20 reason=gone`, **1 ≤ n ≤ 19**, n RECORDED; **no fire sound line** (neither the second bell nor the chime), no FIRE, REPORT, HURT or TELEPORT line | K2 | READ: both telegraph sound lines and the TELEGRAPH at `tick=1330`; `BEAM cancelled mob=knell id=<K> elapsed=8/20 reason=gone` (n = 8); no fire sound line, no FIRE, REPORT, HURT or TELEPORT line. **MATCH** |

### KB10R5 — supersedes GATE-dragon-knell-3.md KB10R4: the move 28 ticks after the fire (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| AT at K; the forced hit; **exactly one** `TELEPORT … tick=<FIRE + 28> from=<K> center=<F> radius=8.000 bearing=0.000 side=right first=50.000 tried=1 [right 50.000 ok y=<F.y> at=<F + (5.142, 0, 6.128)>] landed=<the same> moved=true`; the AT line there | K1, K3 | READ: AT at K `-92.500 63.000 5.500`; FIRE `tick=1883` a hit; `TELEPORT … tick=1911 from=-92.500 63.000 5.500 center=-100.500 63.000 5.500 radius=8.000 bearing=0.000 side=right first=50.000 tried=1 [right 50.000 ok y=63.000 at=-95.358 63.000 11.628] landed=-95.358 63.000 11.628 moved=true` (1911 = 1883 + 28); AT there. **MATCH** |

### TP2R4 — supersedes TP2R3: 50 degrees left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 28, `side=left first=50.000 tried=1 [left 50.000 ok y=<F.y> at=<F + (5.142, 0, −6.128)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=2023`; `TELEPORT … tick=2051 … side=left first=50.000 tried=1 [left 50.000 ok y=63.000 at=-95.358 63.000 -0.628] … moved=true`; AT there. **MATCH** |

### TP3R4 — supersedes TP3R3: 30 degrees right (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 28, `side=right first=30.000 tried=1 [right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=2163`; `TELEPORT … tick=2191 … side=right first=30.000 tried=1 [right 30.000 ok y=63.000 at=-93.572 63.000 9.500] … moved=true`; AT there. **MATCH** |

### TP4R4 — supersedes TP4R3: 70 degrees right (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; one TELEPORT at the FIRE + 28, `side=right first=70.000 tried=1 [right 70.000 ok y=<F.y> at=<F + (2.736, 0, 7.518)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=2303`; `TELEPORT … tick=2331 … side=right first=70.000 tried=1 [right 70.000 ok y=63.000 at=-97.764 63.000 13.018] … moved=true`; AT there. **MATCH** |

### TP5R4 — supersedes TP5R3: a miss moves nothing (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| AT at K; the forced beam's sounds and lines; the step; `… gap=2.700 miss] dealt=none`; no REPORT, no HURT, **no TELEPORT line of any kind** through the 130 ticks; AT at K | K1 | READ: AT at K; FIRE `tick=2443 … gap=2.700 miss] dealt=none`; no REPORT, HURT or TELEPORT line; AT at K. **MATCH** |

### TP6R4 — supersedes TP6R3: every right spot walled; it lands left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 28, `tried=6 [right 50.000 no-footing blocked=7 ungrounded=0, right 30.000 no-footing blocked=7 ungrounded=0, right 43.333 no-footing blocked=7 ungrounded=0, right 56.667 no-footing blocked=7 ungrounded=0, right 70.000 no-footing blocked=7 ungrounded=0, left 50.000 ok y=<F.y> at=<F + (5.142, 0, −6.128)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=2583`; `TELEPORT … tick=2611 … tried=6 [right 50.000 no-footing blocked=7 ungrounded=0, right 30.000 …, right 43.333 …, right 56.667 …, right 70.000 no-footing blocked=7 ungrounded=0, left 50.000 ok y=63.000 at=-95.358 63.000 -0.628] … moved=true`; AT there. **MATCH** |

### TP7R4 — supersedes TP7R3: walled in everywhere; none, and it stays (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 28, `tried=10`, all ten `no-footing blocked=7 ungrounded=0`, `landed=none moved=-`; AT at K before and after | K1, K3 | READ: FIRE `tick=2723`; `TELEPORT … tick=2751 … tried=10 [` all ten `no-footing blocked=7 ungrounded=0` `] landed=none moved=-`; AT at K before and after. **MATCH** |

### TP8R4 — supersedes TP8R3: a pit two deep (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 28, `tried=1 [right 50.000 ok y=<F.y − 2> at=<F + (5.142, −2, 6.128)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=2863`; `TELEPORT … tick=2891 … tried=1 [right 50.000 ok y=61.000 at=-95.358 61.000 11.628] … moved=true`; AT there. **MATCH** |

### TP9R4 — supersedes TP9R3: a step two high (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 28, `tried=1 [right 50.000 ok y=<F.y + 2> at=<F + (5.142, 2, 6.128)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=3003`; `TELEPORT … tick=3031 … tried=1 [right 50.000 ok y=65.000 at=-95.358 65.000 11.628] … moved=true`; AT there. **MATCH** |

### TP10R4 — supersedes TP10R3: ground four down, refused (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 28, `tried=2 [right 50.000 no-footing blocked=0 ungrounded=7, right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=3143`; `TELEPORT … tick=3171 … tried=2 [right 50.000 no-footing blocked=0 ungrounded=7, right 30.000 ok y=63.000 at=-93.572 63.000 9.500] … moved=true`; AT there. **MATCH** |

### TP11R4 — supersedes TP11R3: no sight, refused (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEPORT at the FIRE + 28, `tried=2 [right 50.000 no-sight y=<F.y>, right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=3283`; `TELEPORT … tick=3311 … tried=2 [right 50.000 no-sight y=63.000, right 30.000 ok y=63.000 at=-93.572 63.000 9.500] … moved=true`; AT there. **MATCH** |

### TP12R4 — supersedes TP12R3: he steps 2 north during the delay (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit at F; `CMD /tp @s ~ ~ ~-2` 40 ticks after the force (9 before the move); `TELEPORT … tick=<FIRE + 28> center=<F + (0, 0, −2)> radius=8.246 bearing=14.036 side=right first=50.000 tried=1 [right 50.000 ok … at=<F + (3.610, 0, 5.414)>] … moved=true`; AT there | K1, K3 | READ: FIRE `tick=3423` at F; `CMD /tp @s ~ ~ ~-2`; `TELEPORT … tick=3451 … center=-100.500 63.000 3.500 radius=8.246 bearing=14.036 side=right first=50.000 tried=1 [right 50.000 ok y=63.000 at=-96.890 63.000 10.914] … moved=true`; AT there. **MATCH** |

### TP13R4 — supersedes TP13R3: the Knell killed during the delay (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; `CMD /kill @e[tag=tp13r4]` 35 ticks after the force (14 before the move); **exactly one** `TELEPORT skipped mob=knell id=<K> target=<Ben> reason=knell-gone`; no decision line | K1 | READ: FIRE `tick=3563`, a hit; `CMD /kill @e[tag=tp13r4]`; exactly one `TELEPORT skipped mob=knell id=<K> target=<Ben> reason=knell-gone`; no decision line. **MATCH** |

### TP14R3 — supersedes TP14R2, RE-TIMED: a beam forced while a move is pending (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The first forced hit: TELEGRAPH₁ at T₁, FIRE₁ at T₁ + 20, one HURT. (2) **23 ticks after the first force**, in one tick: `CMD /rpg heal 1000000`, then `DRIVE beam` with no `refused=`. (3) TELEGRAPH₂ at **T₂ = T₁ + 23** with its two sounds; its two fire sounds and FIRE₂ at **T₁ + 43**, `… gap=0.000 hit] dealt=65.000`, REPORT, **exactly one** `TELEPORT ignored mob=knell id=<K> reason=pending`, one HURT. (4) **exactly one** decision, at **FIRE₁ + 28 = T₁ + 48** (5 ticks after FIRE₂; FIRE₂ + 28 would be T₁ + 71), `side=right first=50.000 tried=1 [right 50.000 ok … at=<F + (5.142, 0, 6.128)>] … moved=true`; AT there. **If the `ignored` line is missing or a second move is scheduled, the row is NOT AS PREDICTED and is diagnosed before anything changes** | K5 | READ: (1) TELEGRAPH₁ `tick=3673`, FIRE₁ `tick=3693` hit, one HURT. (2) the heal and `DRIVE beam` with no `refused=`. (3) TELEGRAPH₂ `tick=3696 lived=29` (**T₁ + 23**) with both sounds; FIRE₂ `tick=3716` (**T₁ + 43**) `… gap=0.000 hit] dealt=65.000`, REPORT, `TELEPORT ignored mob=knell id=<K> reason=pending`, one HURT. (4) exactly one decision, `tick=3721` = **FIRE₁ + 28 = T₁ + 48**, 5 ticks after FIRE₂, `side=right first=50.000 tried=1 [right 50.000 ok y=63.000 at=-95.358 63.000 11.628] … moved=true`; AT there. **MATCH** |

### KB11R5 — supersedes KB11R4: the live cycle, by bound, unbroken by a move (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) both spawns, `REPLY "The base value for attribute Speed for entity Knell set to 0" []`, the draw. (2) the first own TELEGRAPH `forced=false`, **99 ≤ lived ≤ 201**, at K, `ticks=20`, its two sounds at its tick. (3) its FIRE 20 later with its two sounds, a hit; one HURT. (4) **a TELEPORT at that FIRE + 28**, `side=right first=50.000 … landed=<F + (5.142, 0, 6.128)> moved=true`. (5) each next own TELEGRAPH's `lived` − the previous one's − 20 **within 100 to 200**, with the previous hit's move between them. (6) each later hit's move at its FIRE + 28, the production draw, RECORDED, `radius=8.000`. (7) no line names the frozen Knell's uuid but its spawn. (8) between two heals at most one HURT. (9) the kills; anything pending cancelled inside the row, RECORDED | K1 | READ: (1) both spawns, the speed REPLY, the draw. (2) first own TELEGRAPH `forced=false tick=3982 lived=192` (inside 99 to 201) at K, `ticks=20`, both sounds. (3) FIRE `tick=4002` hit; one HURT. (4) `TELEPORT … tick=4030` (+28) `side=right first=50.000 … landed=-95.358 63.000 11.628 moved=true`. (5) next own TELEGRAPH `tick=4155 lived=365`: 365 − 192 − 20 = **153**. (6) FIRE `tick=4175` hit; `TELEPORT … tick=4203` (+28) `bearing=50.000 side=left first=48.629 … landed=-92.502 63.000 5.691`, `radius=8.000`, the production draw. (7) the frozen Knell's uuid on its spawn line only. (8) heals at 07:48:25, :30, :35, :40, :45, :50; HURTs at 07:48:36 and :44, each alone between two heals. (9) the kills; nothing pending (two own beams in the row). **MATCH** |

### RE1 — NEW: the NORMAL end of each run still logs its RESTORED line unchanged (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| For EACH run (dragon-beam, then dragon-teleport): `SELFTEST RUN FINISHED`, then **exactly one** `SELFTEST RUN RESTORED xp=<n> cell=<class>/<element> gamemode=SURVIVAL held=<h> mana=<m> trace=false spawn_mobs=<what it found> time=<what it found> blocks=<n> mobsRemoved=<n> orbsRemoved=<n> keep_inventory=false immediate_respawn=false difficulty=NORMAL`, **its fields and their order as at `9d0cbffb`** (`… gamemode=SURVIVAL held=0 mana=100.0 trace=false spawn_mobs=false time=6000 blocks=22879 mobsRemoved=0 …` and `… blocks=101754 mobsRemoved=1 …`); **no `ABORTED` line and no `NOT-RESTORED` line** in either run | K1; B5 (the shared restore) | READ: dragon-beam `07:46:41 SELFTEST RUN FINISHED`, then `SELFTEST RUN RESTORED xp=712595 cell=mage/fire gamemode=SURVIVAL held=1 mana=100.0 trace=false spawn_mobs=false time=6000 blocks=22879 mobsRemoved=0 orbsRemoved=0 keep_inventory=false immediate_respawn=false difficulty=NORMAL`; dragon-teleport `07:49:33 SELFTEST RUN FINISHED`, then `… held=1 … blocks=101754 mobsRemoved=1 orbsRemoved=0 keep_inventory=false immediate_respawn=false difficulty=NORMAL`. The fields and their order as at `9d0cbffb` (`held` read 1 here, 0 there: a value, as predicted `held=<h>`). **No `ABORTED` and no `NOT-RESTORED` line in either run. MATCH** |

---

## BEN'S ROWS — BY THE ASK STEP, SURVIVAL, DIFFICULTY NORMAL

**AUTO-PARTIAL:** the BEAM, sound and TELEPORT lines are the server's half (what it SENT); what he heard is his half.
**His answers are his ear, never a measurement.** Three questions in all; the dodge question is not asked again (he
answered it).

### KQ1R4 — supersedes GATE-dragon-knell-3.md KQ1R3: the two bells (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Server half:** a heal; the gold line; one forced beam from a frozen Knell 12 east while he stands still: the two telegraph sounds, the TELEGRAPH, the two fire sounds, FIRE `… length=18.001 … [player <Ben> gap=0.000 hit] dealt=65.000`, one HURT; **its move at the FIRE + 28** (`radius=12.000 bearing=0.000`, the production draw, RECORDED); then the ASKs; the kill, no skip line (the move has landed). **His half, yes/no:** ASK 1 *"Does the higher bell at the shot give it what was missing?"*; ASK 2 *"Is the lower first bell better than before?"* | K1, K2; Ben's ruling | READ, server half: the heal; the gold line; both telegraph sounds (bell 0.80, then the resonance) and both fire sounds (bell 1.50, then the chime) at their ticks; TELEGRAPH `tick=1465 lived=61 at=-88.500 63.000 5.500`; FIRE `tick=1485 … length=18.001 … [player <Ben> gap=0.000 hit] dealt=65.000`; one HURT; **the move at the FIRE + 28: `TELEPORT … tick=1513 … radius=12.000 bearing=0.000 side=right first=31.577 tried=1 [right 31.577 ok y=63.000 at=-90.277 63.000 11.784] … moved=true`**; the ASKs; the kill, no skip line. His half, verbatim: `ASK 1 question="Does the higher bell at the shot give it what was missing?" answer="yes" ticks=88`; **`ASK 2 question="Is the lower first bell better than before?" answer="no" ticks=64`** |

### TQ1R4 — supersedes GATE-dragon-knell-3.md TQ1R3: the whole sequence (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Server half:** a heal; the gold line; one forced beam from a frozen Knell 12 east, a hit, the two telegraph sounds and the two fire sounds; **one TELEPORT at the FIRE + 28**, `radius=12.000 bearing=0.000`, the production draw, RECORDED. **His half, yes/no:** ASK 1 *"Does the whole sequence feel right now?"* | K1, K2; Ben's 28 and his second bell | READ, server half: the heal; the gold line; both telegraph sounds (bell 0.80, then the resonance) and both fire sounds (bell 1.50, then the chime) at their ticks; FIRE `tick=4481 … hit] dealt=65.000`; one HURT; `TELEPORT … tick=4509` (+28) `radius=12.000 bearing=0.000 side=left first=56.444 tried=1 [left 56.444 ok y=63.000 at=-93.867 63.000 -4.500] … moved=true`. His half, verbatim: **`ASK 1 question="Does the whole sequence feel right now?" answer="the timing of the warning and teleport are good, I want the shot to be in the middle" ticks=547`**: not yes or no, recorded as his words |
