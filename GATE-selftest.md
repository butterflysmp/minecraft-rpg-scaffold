# GATE — SELFTEST: `/rpg selftest` stages and drives the CORE rows of #168-#174 (and carries the stack's one R0)

**Status at `38cc9903` (the stack-top boot, 2026-10-14): R0a-d PASS (the stack's one R0); ST2, ST4, ST6 PASS; ST1's log
half PASS on run 2 and its restore half NOT READ (no baseline); ST3's log half PASS and its sight half NOT READ (not
on Ben's list); ST5 FAIL in part (the aim defect: the control held for GATE-level.md and failed for
GATE-wither-status.md). After the boot: the AIM fix (the note under ST5). NOT a merge candidate this round.**

**The boot, 2026-10-14, on `38cc9903` (#175's head, the stack top), Paper 26.1.2 build 74:**
- CC booted it with `./scripts/dev-server.sh --refresh-content` on the seat's word; Ben was the only player. His
  LOGIN is L80 (`BaronVonYeetus ... logged in`, 22:47:02). Line numbers are that boot's `run/logs/latest.log`.
- `/rpg selftest all` ran twice. **Run 1 (L86-L1087) was in CREATIVE and certifies NOTHING** (the seat's ruling;
  L1087 `RESTORED ... gamemode=CREATIVE`). **Run 2 (L1091-L2098, SURVIVAL, after `/gamemode survival` at L1088) is
  the run of record**: every AUTO reading below is from inside its row's own START ... DONE bracket in run 2.
- **Ben's hand steps.** His words, relayed by the seat: *"all done, everything looks good"*, then *"I ran all the
  self checks and they are fine."* **The seat's ruling, 2026-10-14:** the second is Ben's explicit confirmation of
  each check on the hand-step list he was given -- **MC3 (slot), MC1, MC2, LS1, D1, G1, G3, WB1, ST2, ST6** -- so
  those sight halves are PASS, itemised by that list. Ben was in CREATIVE from 23:15:46 to 23:16:09 (L2102-L2104)
  during his hand steps; which check, if any, fell in that window is not recorded. His hand window ran with the mob
  trace OFF (L2098 `trace=false`), so a hand cast has no log line either way.
- **The aim defect** (GATE-selftest.md ST5): `tp @s ~ ~ ~ facing entity <e> eyes` turns a player from the FEET, so
  every selftest row aimed that way missed: SB2, LB2, WB2, WB3, WS-C, WS1, WS2b, WS2c, WS3 are VOID this boot and
  carry to the next boot after #175's AIM fix.
- **Observation, unexplained:** MOBREMOVE lines run through run 2, many naming uuids that appear nowhere else.

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
| `[Rpg] Build: <sha>`, SELFTEST's head (#175) as `gh pr view 175 --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown`, and not any head beneath. **This `<sha>` is the one every lower gate's SUPERSEDED note names** | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS, 2026-10-14.** L42 `[Rpg] Build: 38cc9903`, equal to `gh pr view 175 --json headRefOid` at the boot (`38cc9903a70ad53dbf7b683b8edc3cf788e3714f`, OPEN), shortened; not `-dirty`, not `unknown`. **After the boot the stack was rebased upward to carry the readings**, so #175's head is no longer `38cc9903`; `git diff --name-only 38cc9903 <the new head>` lists 7 `GATE-*.md` files and **0 other paths**, so the booted code is the new head's code. |

### R0b — the jar carries all eight slices, and the bow as authored

| prediction | instrument | READING |
|---|---|---|
| Entries PRESENT, one or more per slice: #168 core `combat/TracedHit.class`; #169 core `progression/LevelBonus.class`; #170 paper `menu/NexusScreens.class`; #171 `content/weapons/short_bow.yml`; #172 paper `adapter/FireballDrive.class`; #173 paper `adapter/DotStatus.class` and `content/statuses/withering.yml`; #174 `content/weapons/withered_shortbow.yml`; **#175 paper `selftest/SelfTestService.class` and `selftest/index.yml`**. ABSENT: the control. **In the bow's text, as YAML keys (anchored):** PRESENT `element: wither` (top level), `rarity: uncommon`, `cooldown_ticks: 16`, `- type: knockback`; ABSENT `- type: status` | the scan below | **PASS, 2026-10-14.** The scan on `run\plugins\rpg-0.1.0-SNAPSHOT.jar` (deployed from 38cc9903): all 10 slice entries PRESENT; `NoSuchClassControl.class` ABSENT (the control); in the bow PRESENT `^element: wither`, `^rarity: uncommon`, `cooldown_ticks: 16`, `- type: knockback`, and ABSENT `- type: status`. |

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
| `Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes` — **identical to `GATE-withered-shortbow.md`'s R0c prediction**, because #175 adds no file under `content/` (its scenarios are under `selftest/`, which the content loaders never read). That prediction's derivation stands: #168's R0c reading on `9ee59a2e` plus weapons +3, visuals +2, statuses +1. No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair, none naming `withered_shortbow`, `blaze_kings_staff`, `blaze_fireball` or `withering`, and no `element 'wither' applies status` warning | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception','applies status'` | **PASS, 2026-10-14.** L44 `[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`, identical to the prediction. No `Refusing`, `Skipping`, `SEVERE`, `Exception` or `applies status` line. The only content WARNs are `volley_stone`'s known pair (L45-L46), at WARN, which the instrument's patterns do not match. Paper itself warned (L72-L77) that 26.1.2 build 74 is 4 releases behind 26.3: a Step 0 notice, no action (the seat). |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 24 scenarios across 7 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow]`**, and **no** `Refusing selftest` line. *"NOT a dev server"* here means `dev-server.sh` did not boot this server, and **stops the gate** | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | **PASS, 2026-10-14.** Exactly one L50 `[Rpg] Selftest: dev server, 24 scenarios across 7 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow]`; no `Refusing selftest` line. |

---

## THE INSTRUMENT'S OWN ROWS

**The log instrument for every row below:** `Select-String run\logs\latest.log -Pattern 'SELFTEST'`, read after Ben's
LOGIN.

### ST1 — a run brackets every row and judges none (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| In `/rpg selftest all`'s log: `SELFTEST RUN START all rows=24 selftest=<R0a's sha> player=<Ben's uuid>`; then, for each gate in index order, `SELFTEST <gate> setup START …` and `DONE`, then each CORE row's `START scenario=selftest/GATE-<gate>.yml#<row> …` and exactly one `DONE ticks=<n>` or `SKIPPED <why>`; then `SELFTEST RUN FINISHED` and `SELFTEST RUN RESTORED xp=<the recorded Lifetime XP> …`. **No `SELFTEST` line contains `PASS` or `FAIL`** (`SelfTestNeverJudgesTest` pins the compiled constants; this row reads the log). **Every SKIPPED is a reading**: record each one and its reason | **Run 1 certifies nothing** (L86-L1087, CREATIVE: L1087 `gamemode=CREATIVE`; the seat's ruling). **Run 2 (L1091-L2098, SURVIVAL), log half PASS:** L1091 `SELFTEST RUN START all rows=24 selftest=38cc9903 player=b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5`; the seven gates in index order, each setup START ... DONE then its rows; 31 `START scenario=` and 31 `DONE ticks=` (24 rows + 7 setups), **0 SKIPPED**; the DONE ticks sum to 2291 (the computed 2,291); `RUN FINISHED`, then L2098 `RUN RESTORED xp=0 ... gamemode=SURVIVAL ...`. **0 `SELFTEST` lines in the whole log contain PASS or FAIL.** **The restore half (xp = the recorded Lifetime XP): NOT READ, no baseline** -- the seat's ruling. Beside it: the session's chat carries "Lifetime XP before run 2 was 0", unattributed; the seat ruled Ben's baseline not given. |

### ST2 — stop restores what the run changed (SURVIVAL) — witness: the log + Ben

| prediction | READING |
|---|---|
| Record `Lifetime XP`, the hotbar, the position. `/rpg selftest level`; when the log shows `SELFTEST level WD1 START`, type **`/rpg selftest stop`**. The reply: **`Selftest stopped; everything it changed is restored (the log says what).`** The log: `SELFTEST RUN STOPPED by /rpg selftest stop`, then `SELFTEST RUN RESTORED xp=<the recorded number> … gamemode=SURVIVAL …`. **Ben:** the stats head's `Lifetime XP` reads the recorded number; **no emberblade** in the inventory (WD1 gave one); he stands where he stood; no tagged zombie remains near him. **No further `SELFTEST level` line** after the stop | **PASS.** Log half: `/rpg selftest level` (L2108); L2167 `SELFTEST level WD1 START`; `/rpg selftest stop` (L2193); L2194 `SELFTEST RUN STOPPED by /rpg selftest stop`; L2195 `RUN RESTORED xp=0 ... gamemode=SURVIVAL ... mobsRemoved=2`; no `SELFTEST level` line after the stop. The run had moved the XP: its own CP2 replied `712580 lifetime` (L2122). Ben's half (the reply text, Lifetime XP, no emberblade, his position, no tagged zombie): **PASS**, itemised (see Status). **Beside it, from the log:** the next run's snapshot (L2197, 23:18:07) holds `xp=4` (L2407's restore) -- Ben picked up 4 XP of orbs between 23:17:45 and 23:18:07 (`PlayerExpChangeEvent`, the only path to Lifetime XP, needs an orb). A head read after that pickup shows 4, not 0; the restore does not clear the orbs its staging's kills drop. An instrument gap, carried to the fix. |

### ST3 — the reply reader exists only during a run (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| **During** ST1's run, the replies appear as `SELFTEST <gate> <row> REPLY …` (e.g. under `level BN1`, the `/rpg stats` lines). **After** the run ends, Ben types `/rpg stats`: the reply shows in his chat, and **no `REPLY` line** follows in the log. The reader's copy-only shape is `ReplyReaderCopyOnlyTest`'s; this row reads its lifetime | **Log half PASS.** `/rpg stats` at L2408 (23:21:32), after every run had ended, is the log's last line: 0 `REPLY` lines follow it. The control: during runs the reader did copy this command's reply (run 2, `SELFTEST level BN1 REPLY "Your Stats" [gold]` and its stat lines, L1255-L1265). Ben's half (the reply shows in his chat): NOT READ -- ST3 is not on his hand-step list. |

### ST4 — every command the instrument runs is logged by the instrument (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| Each scenario command appears as `SELFTEST <gate> <row> CMD /<command>`. **Read from the jar, not from a run:** `performCommand` skips the `issued server command` line, so for those commands **no** `issued server command` line appears. Count both over ST1's run: CMD lines > 0, and the only `issued server command` lines are those Ben typed (`/rpg selftest all` itself, and any he typed by hand) | **PASS.** Over run 2 (L1091-L2098): 143 `SELFTEST <gate> <row> CMD /...` lines, and 0 `issued server command` lines inside the span; Ben's `/rpg selftest all` is L1090, just before it. |

### ST5 — the instrument can read a DIFFERENCE (SURVIVAL) — witness: the log — the control for every AUTO row

| prediction | READING |
|---|---|
| **The positive control** (`verification.md`, *A CHECK THAT DID NOT RUN*): an instrument that drove nothing would print brackets with nothing between them. Under ST1's `level WD1`: **four** `PLAYERHIT … source=emberblade/left_click` lines; the two after the `CMD /rpg playerxp set @s 50 levels` line are each **exactly 49.000 above** the two before it, wherever both lines of a pair are `crit=false`. Under `wither-status WS1` and `WS2b`: `sent=1.000` and `sent=2.000` respectively. **If WD1's four `sent=` are equal, the instrument did not move the level, and every AUTO reading of `GATE-level.md` is VOID** | **FAIL in part -- the control held for GATE-level.md and FAILED for GATE-wither-status.md.** `level WD1`: four `PLAYERHIT ... source=emberblade/left_click`, `sent=7.000`, `7.000` before `CMD /rpg playerxp set @s 50 levels` and `56.000`, `56.000` after (L1305, L1321, L1334, L1352), all `crit=false` -- each exactly 49.000 above; the level moved, so GATE-level.md's AUTO readings stand. `wither-status WS1` and `WS2b`: **no `sent=` line at all** -- every `/rpg apply withering 200` replied `"Look at a mob to apply a status." [red]` (L1899, L1932), because the scenario's `tp @s ~ ~ ~ facing entity ... eyes` turns from the FEET (javap: a player's command source anchors at `Anchor.FEET`) and the look ray passed over the target; the boot's aimed DRIVE lines read `pitch=-12.271` at 8 blocks against -0.859 eye to eye. **Every AUTO reading of GATE-wither-status.md from this boot is VOID**, and with it SB2, LB2, WB2, WB3 (the same aim). The fix is the `AIM` verb (see the note under this table). |

> **THE AIM FIX, 2026-10-14, after the `38cc9903` boot (instrument; no prediction moves).** The scenarios' ten
> `tp @s ~ ~ ~ facing entity @e[tag=t,limit=1] eyes` steps (legacy-a SB2, legacy-b LB2, withered-shortbow WB2,
> wither-status x7) are now **`aim: t`**, a new verb: `SelfTestAim.at` computes the yaw and pitch from the player's
> EYE (`feetY + eyeHeight`) to the nearest living tagged entity's eye, in Java, and the driver turns the player with
> `teleportAsync` (the Folia shape; each scenario already waits 2 t after the aim) and logs
> `DRIVE aim teleportAsync eye->eye target=<uuid> <type> d=... yaw=... pitch=...`; the USE driver's own DRIVE line
> still logs the pitch the server holds, so the next boot cross-checks it. **`SelfTestAimTest`** (5 tests) casts an
> exact ray from the eye along that rotation into an exact box (no lenience) and asserts it hits a zombie and a wither
> skeleton at d = 2, 3, 4 and 8, and off-axis (WS3's neighbour, MC14's diagonal); the sizes (player eye 1.62, zombie
> 0.6 x 1.95 eye 1.74, wither skeleton 0.7 x 2.4 eye 2.1) are read from the pinned jar's `EntityType`. Its control
> reproduces this boot's `pitch=-12.271` from the FEET and asserts that ray misses. **Mutation MUT-AIM-FEET**
> (`targetEyeY - (feetY + eyeHeight)` to `targetEyeY - feetY`, restored by `cp`, `cmp`-identical): **4 of 5 red**
> -- the three hitbox tests and `yawFollowsBukkitsConvention` (its level-pitch assertion); the control stays green, as
> it must. The suite at the fix: core 1546, storage 92, paper 1139 run, 0 failed (surefire reports; `@Test` in
> source 1546 / 92 / 1134 + 5). **Not yet read in a boot**: the next boot reads the VOID rows with it.

### ST6 — one run at a time (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| During a run, Ben types `/rpg selftest level` again: the reply **`Refusing: a selftest is already running. /rpg selftest stop ends it.`**, logged as `SELFTEST … REPLY "Refusing: a selftest is already running. …" [gray]` under whichever row is running, and **no second `SELFTEST RUN START`** | **PASS.** During the run started at L2196-L2197, Ben's second `/rpg selftest level` (L2274) was refused: L2275 `SELFTEST level WD1 REPLY "Refusing: a selftest is already running. /rpg selftest stop ends it." [gray]`; no `RUN START` between L2197 and that run's restore (L2407): 4 in the whole log, one per run. Ben's half: **PASS**, itemised (see Status). |

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
