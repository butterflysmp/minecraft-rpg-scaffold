# GATE — SELFTEST: `/rpg selftest` stages and drives the CORE rows of #168-#174 (and carries the stack's one R0)

**Status: NOT RUN.**

Every prediction below was written **before** any boot of this branch, and **this file is COMMITTED before the boot**.
Readings go **beside** a prediction, never over it, and a prediction is not edited once its row has been read. **NO
BOOT until the seat says so.** **This is the stack top**: `#168 → #169 → #170 → #171 → #172 → #173 → #174 → this`.
It is booted ONCE, at this branch's head, and every gate beneath it is read in the same boot.

**Readings count only if Ben's LOGIN appears in THIS boot's log.** A row whose prediction is a LOG LINE is PASS when
that line appears after the LOGIN with every predicted field as written.

```
ROWS     10   R0a R0b R0c R0d
              ST1 ST2 ST3 ST4 ST5 ST6
         ──
         10   = 4 R0 + 6 ST     git grep -c '^### R0\|^### ST' <ref> -- GATE-selftest.md
```

**Plan:** `PLAN-selftest.md` on `docs/batch-surveys`: the Phase 1 survey, the seat's rulings a-d (2026-10-05), and §8's
six reads, done in Phase 2 and recorded there. **The rule the readings answer to:** `.claude/rules/verification.md`,
*AN AUTO READING IS A READING OF THE SERVER, NEVER OF THE CLIENT*.

## GAME MODE

**SURVIVAL on every row.** The instrument changes nothing about the mode. It snapshots it and restores it.

## THE ORDER IN THE ONE BOOT

1. **R0, below, once, for the whole stack.** If any R0 fails, STOP.
2. **`/rpg selftest all`**, typed by Ben, standing on flat open ground with about 12 blocks clear to the east (+X).
   It walks the seven gates in boot order (`selftest/index.yml`), each gate's setup first, and restores at the end.
   It runs for about **2 minutes**: the YAML's `wait:` steps sum to **2,229 ticks**, plus 2 per scenario end (31,
   setups included) = **2,291 ticks, ~115 s at 20 TPS**. That is computed from the files, not measured, and a slower
   tick rate stretches it. **Ben does nothing while it runs.**
3. **Ben's halves** of the AUTO-PARTIAL rows, from each gate's *SELFTEST WITNESS AMENDMENT*, as the run sheet lists
   them.
4. **ST1-ST6**, below.
5. Optional rows by hand, from each gate as written.

## Set-up

- **Boot with `./scripts/dev-server.sh --refresh-content`.** That script is the only place `-Drpg.dev=true` is set;
  R0d reads that it arrived.
- **Record the stats head's `Lifetime XP` first.** The instrument restores it, and ST2 reads that it did.
- **No second player online** (the instrument refuses, by ruling b).

---

## R0 — THE ONE R0 FOR THE WHOLE STACK (the seat's ruling c). READ IT FIRST. IF ANY R0 FAILS, STOP

Every gate beneath carries a dated note recording its own R0 rows **SUPERSEDED** at this boot, never PASS or FAIL.
`GATE-withered-shortbow.md`'s note names this file. The one exception is unchanged: `GATE-nexus-polish.md` **R0d**, a
#170 merge bar, is read as written.

### R0a — the build line names #175's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, SELFTEST's head (#175) as `gh pr view 175 --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown`, and not any head beneath. **This `<sha>` is the one every lower gate's SUPERSEDED note names** | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries all eight slices, and the bow as authored

| prediction | instrument | READING |
|---|---|---|
| Entries PRESENT, one or more per slice: #168 core `combat/TracedHit.class`; #169 core `progression/LevelBonus.class`; #170 paper `menu/NexusScreens.class`; #171 `content/weapons/short_bow.yml`; #172 paper `adapter/FireballDrive.class`; #173 paper `adapter/DotStatus.class` and `content/statuses/withering.yml`; #174 `content/weapons/withered_shortbow.yml`; **#175 paper `selftest/SelfTestService.class` and `selftest/index.yml`**. ABSENT: the control. **In the bow's text, as YAML keys (anchored):** PRESENT `element: wither` (top level), `rarity: uncommon`, `cooldown_ticks: 16`, `- type: knockback`; ABSENT `- type: status` | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/combat/TracedHit.class',
               'io/github/butterflysmp/rpg/core/progression/LevelBonus.class',
               'io/github/butterflysmp/rpg/paper/menu/NexusScreens.class',
               'content/weapons/short_bow.yml',
               'io/github/butterflysmp/rpg/paper/adapter/FireballDrive.class',
               'io/github/butterflysmp/rpg/paper/adapter/DotStatus.class',
               'content/statuses/withering.yml',
               'content/weapons/withered_shortbow.yml',
               'io/github/butterflysmp/rpg/paper/selftest/SelfTestService.class',
               'selftest/index.yml',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$r = New-Object IO.StreamReader($zip.GetEntry('content/weapons/withered_shortbow.yml').Open()); $bow = $r.ReadToEnd(); $r.Close()
foreach ($k in '^element: wither\s*$', '^rarity: uncommon\s*$', '^\s+cooldown_ticks: 16\s*$', '^\s+- type: knockback\b', '^\s+- type: status\b') {
  if ($bow -match ('(?m)' + $k)) { "PRESENT $k" } else { "ABSENT  $k" }
}
$zip.Dispose()
```

### R0c — the cumulative Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes` — **identical to `GATE-withered-shortbow.md`'s R0c prediction**, because #175 adds no file under `content/` (its scenarios are under `selftest/`, which the content loaders never read). That prediction's derivation stands: #168's R0c reading on `9ee59a2e` plus weapons +3, visuals +2, statuses +1. No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair, none naming `withered_shortbow`, `blaze_kings_staff`, `blaze_fireball` or `withering`, and no `element 'wither' applies status` warning | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception','applies status'` | |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 24 scenarios across 7 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow]`**, and **no** `Refusing selftest` line. *"NOT a dev server"* here means `dev-server.sh` did not boot this server, and **stops the gate** | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | |

---

## THE INSTRUMENT'S OWN ROWS

**The log instrument for every row below:** `Select-String run\logs\latest.log -Pattern 'SELFTEST'`, read after Ben's
LOGIN.

### ST1 — a run brackets every row and judges none (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| In `/rpg selftest all`'s log: `SELFTEST RUN START all rows=24 selftest=<R0a's sha> player=<Ben's uuid>`; then, for each gate in index order, `SELFTEST <gate> setup START …` and `DONE`, then each CORE row's `START scenario=selftest/GATE-<gate>.yml#<row> …` and exactly one `DONE ticks=<n>` or `SKIPPED <why>`; then `SELFTEST RUN FINISHED` and `SELFTEST RUN RESTORED xp=<the recorded Lifetime XP> …`. **No `SELFTEST` line contains `PASS` or `FAIL`** (`SelfTestNeverJudgesTest` pins the compiled constants; this row reads the log). **Every SKIPPED is a reading**: record each one and its reason | |

### ST2 — stop restores what the run changed (SURVIVAL) — witness: the log + Ben

| prediction | READING |
|---|---|
| Record `Lifetime XP`, the hotbar, the position. `/rpg selftest level`; when the log shows `SELFTEST level WD1 START`, type **`/rpg selftest stop`**. The reply: **`Selftest stopped; everything it changed is restored (the log says what).`** The log: `SELFTEST RUN STOPPED by /rpg selftest stop`, then `SELFTEST RUN RESTORED xp=<the recorded number> … gamemode=SURVIVAL …`. **Ben:** the stats head's `Lifetime XP` reads the recorded number; **no emberblade** in the inventory (WD1 gave one); he stands where he stood; no tagged zombie remains near him. **No further `SELFTEST level` line** after the stop | |

### ST3 — the reply reader exists only during a run (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| **During** ST1's run, the replies appear as `SELFTEST <gate> <row> REPLY …` (e.g. under `level BN1`, the `/rpg stats` lines). **After** the run ends, Ben types `/rpg stats`: the reply shows in his chat, and **no `REPLY` line** follows in the log. The reader's copy-only shape is `ReplyReaderCopyOnlyTest`'s; this row reads its lifetime | |

### ST4 — every command the instrument runs is logged by the instrument (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| Each scenario command appears as `SELFTEST <gate> <row> CMD /<command>`. **Read from the jar, not from a run:** `performCommand` skips the `issued server command` line, so for those commands **no** `issued server command` line appears. Count both over ST1's run: CMD lines > 0, and the only `issued server command` lines are those Ben typed (`/rpg selftest all` itself, and any he typed by hand) | |

### ST5 — the instrument can read a DIFFERENCE (SURVIVAL) — witness: the log — the control for every AUTO row

| prediction | READING |
|---|---|
| **The positive control** (`verification.md`, *A CHECK THAT DID NOT RUN*): an instrument that drove nothing would print brackets with nothing between them. Under ST1's `level WD1`: **four** `PLAYERHIT … source=emberblade/left_click` lines; the two after the `CMD /rpg playerxp set @s 50 levels` line are each **exactly 49.000 above** the two before it, wherever both lines of a pair are `crit=false`. Under `wither-status WS1` and `WS2b`: `sent=1.000` and `sent=2.000` respectively. **If WD1's four `sent=` are equal, the instrument did not move the level, and every AUTO reading of `GATE-level.md` is VOID** | |

### ST6 — one run at a time (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| During a run, Ben types `/rpg selftest level` again: the reply **`Refusing: a selftest is already running. /rpg selftest stop ends it.`**, logged as `SELFTEST … REPLY "Refusing: a selftest is already running. …" [gray]` under whichever row is running, and **no second `SELFTEST RUN START`** | |

---

## WHAT THIS GATE CANNOT SEE

- **The refusals that need a different server.** A non-dev boot, the nether, and a second player can't be staged in
  this boot. `SelfTestGuardTest` covers the decision; the wiring is `SelfTestService.start`, and it has no live witness.
- **A crash mid-run.** The restore is in memory. A server crash mid-run leaves the XP, the inventory and the saved
  blocks changed. **The restore file proposed in `PLAN-selftest.md` §4 was NOT BUILT**: it was not ruled, and it is the
  bulk of the restore's work. A quit, a death, a stop and a finish all restore.
- **Folia.** Every step runs on the player's own scheduler, as written for Folia. Nothing here boots Folia.
- **Every bypass in `PLAN-selftest.md` §1:** reach, minimum charge, the swing packet, the use-item decode, the
  preprocess event. No AUTO reading covers them, by rule.

---

## MUTATIONS (run 2026-10-05 against the working tree that became `3d75de46`, before this file was committed)

Each was restored by `cp`, proved byte-identical with `cmp`, and run as `-Dtest='SelfTest*Test,ReplyReader*Test'`.

| mutation | edit | reddened |
|---|---|---|
| MUT-COPYONLY | `event.setCancelled(false);` added to `ReplyReader.onPacketSend` | `ReplyReaderCopyOnlyTest.theReaderMakesNoMutatingCall` only |
| MUT-PAIRING | `WB2:` → `WB2x:` in `selftest/GATE-withered-shortbow.yml` | `SelfTestScenariosTest.everyScenarioNamesARowOfItsGate`, `theScenariosAreExactlyTheCoreRows` |
| MUT-GUARD | the guard's `playersOnline != 1` → `playersOnline < 1` | `SelfTestGuardTest.aSecondPlayerOnlineIsRefusedAndSoIsNoneAtAll` only |
| MUT-VERDICT | `"DONE ticks="` → `"DONE PASS ticks="` in `SelfTestRun` | `SelfTestNeverJudgesTest.noSelftestClassCarriesAVerdict` only |

**Sole witnesses:** ST2 for the restore, ST3 for the reader's lifetime, ST5 for the drivers reaching anything at all.
No unit test drives a live server.
