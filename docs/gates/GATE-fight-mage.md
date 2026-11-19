# GATE — THE DARK MAGE AND THE CRYSTALS (STEPS F6 + F7), AND BEN'S TUNING FROM THE FIRST BOOT

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/fight-mage`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate from
origin, and Ben says ready. Then only CC boots.**

**What it gates.** `PLAN-dragon-fight.md` §6, steps F6 (the Dark Mage) and F7 (the crystals), in an ordinary world
through the dev fixture `/rpg fight …`, with Ben's tuning of 2026-11-18:
- `v_fast` 3.8 and `v_slow` 0.6;
- the lightning through armour;
- the four-row wall, shifted and turning.

No arena, no dragon, no waves.

**What it supersedes.** It RETIRES `selftest/GATE-fight-ring.yml`:
- **Every FR row moves**, RE-PREDICTED here with the next suffix (FR1 → FR1R2 … FR9 → FR9R2): each reads the wall (1208
  points, not 906), the new speeds, or the armour.
- **BQ1–BQ6 are RETIRED, not re-predicted.** Ben answered all seven of their questions at `f85280ee`. Their subjects
  come back as MQ1–MQ7 with the new numbers:
  - BQ2's fast close is MQ1.
  - BQ3's slow close is MQ2.
  - BQ1 and BQ6's look are MQ3.
  - BQ4's pop is ruled ("the jump is fine").
  - BQ5's lightning is ruled (armour "yes").

`GATE-fight-ring.md` keeps every prediction and reading, with a dated note naming these rows.

## GAME MODE, DIFFICULTY

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL, and the fixture's participants are survival or
  adventure only. The damage rows (FR8R2, MQ1, MQ7) are SURVIVAL by that rule.
- **The difficulty is set by the selftest:** the setup's `difficulty: normal`, its `DIFFICULTY found=… set=NORMAL`, and
  `RESTORED … difficulty=<what it found>`.

## THE NUMBERS THE ROWS RUN ON (`content/fights/dragon.yml`, `content/mobs/dark_mage.yml`)

| number | value | whose |
|---|---|---|
| border, Mage ring | 48, 12 | Ben's |
| `v_fast` | **3.8** | **Ben's** (eighth message) |
| `v_slow` | **0.6** | **Ben's** (seventh, answer 3) |
| strikes | every 40 ticks, the k-th 20 × 2^(k−1) before armour, **through armour** (`DefenseRule.APPLIES`), no element | Ben's; the element is OPEN |
| the Mage | `wither_skeleton`, 1500 at GS 100, void, glowing, stationary | Ben's 1500 "for now"; the name "Withered Dark Mage" is the **seat's placeholder** |
| refused-hit cue | `item.shield.block` 1.0 / 0.8 at the Mage | **seat placeholder** |
| crystals | 10, one every 80 ticks from the slow close; spots round a circle about C, radius 40, 12 up; cue `block.beacon.power_select` | **seat placeholders** |
| the wall | rows at +0.2 / +1.7 / +3.2 / +4.7 (Ben's "yes"), shift 0.25, turn 1.0/s | shift and turn the **seat's** |

**EXECUTED, never by hand** (`BensNumbersTest`, and `Calc.java` run against `core/target/classes`):
- **The fast close at 3.8 lands on whole ticks 190, 222, 285 and 379 for d = 0, 6, 18 and 36.** The exact lengths
  are 189.47, 221.05, 284.21 and 378.95; the slow close starts on the first whole tick at which progress has reached 1.
- **The slow close at 0.6 shuts 400 ticks after it starts.**
- **The strikes through defence 10:** 20 → 18.182, 40 → 36.364, 80 → 72.727.

**THE PARTICLE COST, as the brief asked.** At radius 48 a redraw is **1208 points** (302 a row, 4 rows), one packet
each (READ, `CraftPlayer.spawnParticle`), 5 redraws a second: **6040 packets a second per player.** That is under the
brief's 6500 line. At radius 12 it is 304 a redraw.

## THE CLOCK

The fight clock is 0 at `start`, 1 on the first loop tick. A command acts at the fight's current tick. **L (a landing's
tick) and D (a death's tick) are read from the log**, each with its window; every transition is predicted relative to
them.

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC

**His custom maximum is at least 100** (Ben's read 350 at `f85280ee`). The largest single strike any row allows is **80
before armour**.
- **FR8R2:** healed before each strike tick it reaches: 20, 40 and 80, each on a full bar.
- **MQ1:** healed every 10 ticks for the 120 ticks after the landing, the only window he can be outside. At most three
  strikes fall in it (k ≤ 3, at most 80).
- **MQ7, the play row:** the fixture heals every participant to full every 10 ticks (`keepalive`). It STOPS ITSELF after
  any participant's third consecutive strike (`strike_limit=3`; `RingFixtureTest`, *theStrikeLimitStops…*). So no strike
  above 80 can land, and each lands on a bar healed within 10 ticks.
- **Every other row** keeps him inside every ring it draws:
  - at the Mage's point, or 1 to 2 blocks from it;
  - or 6 from M while the ring is at least 6 (MG2–MG5, all read before the slow close is 200 ticks old);
  - or he is stopped before the ring can shut on him (FR6R2).

## THE LINGER RADIUS AND THE CRYSTALS

`LINGER` (radius 48) removes and counts only projectiles, area clouds and our markers (`SelfTestStaging.lingering`).
**A crystal is none of those, so LINGER cannot see one, at any radius.** That is a finding against the brief, which
asked LINGER to cover them.

The witness used instead: every row ends with `rpg fight stop`, then `probe_entities: "end_crystal 48"`.
- Its 48 covers every spot: a spot is √(40² + 12²) = **41.8** from C, and the player ends each row at C or within 7 of
  it.
- It must read **0**.
- Beside it, `probe_entities: "wither_skeleton 16"` must read **0** (no Mage left).

## CARRIED STATE, DONE BY THE BOOTER (CC) BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-fight-mage"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type d | wc -l) dirs"; done; diff -rq run/plugins/Rpg "$B/Rpg" | wc -l | sed 's/^/diff -rq lines Rpg: /'; diff -rq run/world "$B/world" | wc -l | sed 's/^/diff -rq lines world: /'
   ```

   If the folder exists, add `-2`. Report the folder, both pairs of counts, and `diff -rq lines: 0` for each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **Boot this branch's head with `--refresh-content`:** `./scripts/dev-server.sh --refresh-content`, on Ben's "ready".
   **REQUIRED:** the deployed `run/plugins/Rpg/content/fights/dragon.yml` reads `  fast_close_blocks_per_second: 3.8`, and
   `content/mobs/dark_mage.yml` exists.
5. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` names this branch's head.
6. **Tell Ben R0 passed before he logs in.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **Wait for my word that R0 passed before you log in.**
1. `/gamemode survival` — **alone, the first line after login.**
2. **Before line 3:**
   - Stand on flat open ground with about 40 clear blocks to your east and room all round. Question 1 has you sprint
     east, and the fight (question 11) lands the Mage up to 36 blocks away in any direction.
   - Hold the weapon you want to fight with.
3. `/rpg selftest fight-mage` — **ONCE.** About **ten minutes** of rows (about 10,600 ticks of waits), plus your
   answers and up to 90 seconds of fighting.
   - Stand still and touch nothing, **except when gold text in chat tells you to do something, or a gold question
     appears**: do what it says, and answer each question in chat.
   - There are **eleven** questions. Some ask yes or no, some ask in your own words; the question says which.
   - **Lightning will strike you in some rows; you are healed so it cannot kill you.**
   - Wait for the chat to say the run finished.
4. `/stop`

Nothing else this boot.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/fight-mage` prints it, shortened. **Not** master's `8c9a41b6`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries the Mage, the crystals, the gate, the tuning and the rows

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/fights/dragon.yml` line `  fast_close_blocks_per_second: 3.8`; `content/mobs/dark_mage.yml` line `stationary: true`; `DamageGate.class` carrying `allowNext`; `RingFixture.class` carrying `MAGEGATE t=` and `CRYSTAL t=`; `FightFixtures.class` carrying `FIGHTPROBE t=`; `GATE-fight-mage.yml` carrying `MG8:` and `MQ7:`. ABSENT: `  fast_close_blocks_per_second: 5.0` (the YAML needle's control), `NoSuchTextControl` (the class needle's control), and the entry `selftest/GATE-fight-ring.yml` (retired) | the PowerShell below, from the repo root | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Line($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
$y = Bytes 'content/fights/dragon.yml'
Line $y '  fast_close_blocks_per_second: 3.8' 'dragon.yml'; Line $y '  fast_close_blocks_per_second: 5.0' 'dragon.yml'
Line (Bytes 'content/mobs/dark_mage.yml') 'stationary: true' 'dark_mage.yml'
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
Has 'io/github/butterflysmp/rpg/core/fight/DamageGate.class' 'allowNext'
Has 'io/github/butterflysmp/rpg/core/fight/RingFixture.class' 'MAGEGATE t='
Has 'io/github/butterflysmp/rpg/core/fight/RingFixture.class' 'CRYSTAL t='
Has 'io/github/butterflysmp/rpg/core/fight/RingFixture.class' 'NoSuchTextControl'
Has 'io/github/butterflysmp/rpg/paper/fight/FightFixtures.class' 'FIGHTPROBE t='
$s = Bytes 'selftest/GATE-fight-mage.yml'
foreach ($k in 'MG8', 'MQ7') { if ($s -ne $null -and $s -match "(?m)^$($k):\s*$") { "PRESENT GATE-fight-mage $($k):" } else { "ABSENT  GATE-fight-mage $($k):" } }
if ($zip.GetEntry('selftest/GATE-fight-ring.yml') -ne $null) { "PRESENT entry selftest/GATE-fight-ring.yml" } else { "ABSENT  entry selftest/GATE-fight-ring.yml" }
$zip.Dispose()
```

**A jar from master reads ABSENT on every PRESENT line, PRESENT on `  fast_close_blocks_per_second: 5.0` and on the
entry `selftest/GATE-fight-ring.yml`.**

### R0c — the Loaded lines, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 6 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`** (**6 mobs**: `dark_mage.yml` is added; every other count as `GATE-fight-ring.md` R0c read it), AND exactly one **`[Rpg] Loaded fight 'dragon': border 48.0, mage ring 12.0, v_fast 3.80, v_slow 0.60, e_kill 1.00, 10 crystals every 80 ticks, a strike every 40 ticks from 20.0; ring dust (170,0,255) size 1.60 every 1.00 blocks at [0.2, 1.7, 3.2, 4.7], row shift 0.25, turn 1.00/s, every 4 ticks; mage 'dark_mage' refused item.shield.block; crystals at 40.0, 12.0 up, lit block.beacon.power_select; participants within 128.0`**. **No `Refusing`, `Skipping`, `SEVERE` or `Exception`; no `[Rpg]` WARN beyond `volley_stone`'s known pair** (`.claude/rules/verification.md`, R0c's standing clause); no `No fights loaded` | `Select-String run\logs\latest.log -Pattern 'Loaded\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded'` | |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 193 scenarios across 22 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive, fight-mage]`**, and no scenario refusal after it | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**The 193 is RECOUNTED** (2026-11-18, on this branch's tree): in `paper/src/main/resources/selftest/`:
- `grep -cE '^  - ' index.yml` prints **22**;
- `cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints
  **215**;
- the same with `'^setup:'` prints **22**.

So 215 − 22 = **193**. **The control:** the retired `GATE-fight-ring.yml` had 16 keys and the new file 25, so 206 − 16
+ 25 = 215. 206 is the key count `GATE-fight-ring.md` R0d recounted.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument for every row:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST fight-mage|\[Rpg\] (RINGTICK|FIGHT|STRIKE|MAGEGATE|CRYSTAL)'`,
read after Ben's LOGIN, inside the row's own `SELFTEST fight-mage <row> START … DONE` bracket. `\[Rpg\] FIGHT` also
matches FIGHTPHASE, FIGHTKILL, FIGHTPROBE and FIGHTCRYSTAL. **A log-line row passes when the predicted line appears
VERBATIM** (a `<…>` is the value the row says it may take).

**In EVERY row (not repeated):**
- **The clean:** after `CLEAN …`, **`LEFTOVER living=0 radius=16`** and **`LINGER work=0 entities=0 radius=48`**.
- **The end:** the row's last probes read **`end_crystal` 0 within 48** and **`wither_skeleton` 0 within 16**.
- **Start and stop:** one `[Rpg] FIGHT start …` per start; one `[Rpg] FIGHT stop t=<t>` and one
  `[Rpg] FIGHT ended reason=command` per stop.
- **RINGTICK:** every one reads `viewers=1`, and `points=` equals 4 × `ceil(2π r)` for its `r` (**1208 at r=48.000**).
- **STRIKE:** every one is `k=0 damage=0.000 inside`, unless the row says otherwise; **no HURT** unless the row says
  otherwise.
- **A landing** that spawns the Mage logs `[Rpg] FIGHT mage spawned t=L id=<uuid> at=<dx> 0.00 <dz>`.

A non-zero LEFTOVER or LINGER, or a crystal or skeleton left, makes its row NOT AS PREDICTED.

### FR1R2 — start: the border at C, IDLE, now four rows (SURVIVAL) — supersedes FR1

| prediction | stands on | READING |
|---|---|---|
| (1) `RINGTICK t=4 phase=IDLE centre=0.000 0.000 r=48.000 crystals=0 points=1208 viewers=1` and the same at `t=8`. (2) No FIGHTPHASE. (3) `FIGHT stop t=<8 to 11>` | `RingPointsTest` (1208) | |

### FR2R2 — land 0 0: 48 to 12 in exactly 190 ticks; SLOW_CLOSE on that tick; the slow close at 0.6 (SURVIVAL) — supersedes FR2

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=L dx=0.000 dz=0.000 result=STARTED`, L from 18 to 23; `FIGHTPHASE t=L IDLE->FAST_CLOSE`; `FIGHT mage spawned t=L … at=0.00 0.00 0.00`. (2) **`FIGHTPHASE t=L+190 FAST_CLOSE->SLOW_CLOSE`.** (3) Each FAST_CLOSE RINGTICK: `centre=0.000 0.000`, `r` = 48 − 0.19 × (t − L) to 3 places. (4) Each SLOW_CLOSE RINGTICK: `centre=0.000 0.000`, `r` = 12 − 0.03 × (t − L − 190), `crystals=0` (the first is at L+270, after the stop). (5) Every STRIKE inside | `BensNumbersTest` (190), `MageCycleTest` | |

### FR3R2 — land 18 0: 285 ticks, the centre on the straight line (SURVIVAL) — supersedes FR3

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=L dx=18.000 dz=0.000 result=STARTED`, L from 18 to 23; the Mage spawned `at=18.00 0.00 0.00`. (2) **`FIGHTPHASE t=L+285 FAST_CLOSE->SLOW_CLOSE`.** (3) Every FAST_CLOSE RINGTICK: dx = (48 − r) / 2 ± 0.0015, dz 0.000. (4) Every SLOW_CLOSE RINGTICK `centre=18.000 0.000`. (5) Every STRIKE inside (he stands at C + 17) | `BensNumbersTest` (285) | |

### FR4R2 — land 36 0: 379 ticks, the centre on the straight line (SURVIVAL) — supersedes FR4

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=L dx=36.000 dz=0.000 result=STARTED`, L from 18 to 23; the Mage spawned `at=36.00 0.00 0.00`. (2) **`FIGHTPHASE t=L+379 FAST_CLOSE->SLOW_CLOSE`.** (3) Every FAST_CLOSE RINGTICK: dx = 48 − r ± 0.0015, dz 0.000. (4) Every SLOW_CLOSE RINGTICK `centre=36.000 0.000`. (5) Every STRIKE inside (he stands at C + 35) | `BensNumbersTest` (379) | |

### FR5R2 — land 36.01 0: REFUSED_TOO_FAR, with ground there (SURVIVAL) — supersedes FR5

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=<5 to 8> dx=36.010 dz=0.000 result=REFUSED_TOO_FAR` (the ground exists, so the refusal is the distance's, not `REFUSED_NO_GROUND`), and the REPLY `"Mage lands at C + (36.01, 0.00): REFUSED_TOO_FAR"`. (2) No FIGHTPHASE line and **no `FIGHT mage spawned`**. (3) Every RINGTICK `phase=IDLE centre=0.000 0.000 r=48.000 crystals=0 points=1208 viewers=1` | `FightRingTest` (36.01 refused) | |

### FR6R2 — no kills: CLOSED 400 ticks after the slow close (SURVIVAL) — supersedes FR6

| prediction | stands on | READING |
|---|---|---|
| (1) L from 38 to 43; `FIGHTPHASE t=L+190 FAST_CLOSE->SLOW_CLOSE`. (2) **`FIGHTPHASE t=L+590 SLOW_CLOSE->CLOSED`** and **`FIGHT FAILED_CLOSED t=L+590`**, once; the chat `"The fight has failed: FAILED_CLOSED (fixture: nobody is killed)."`. (3) **Four** `CRYSTAL t=L+190+80n lit index=n`, n = 1 to 4. **The fifth is never lit:** it is due at L+590, the tick the ring closes, and CLOSED wins that tie (`MageCycle` checks it first) with a snapshot showing 0. Then **`CRYSTAL t=L+590 removed index=1`** to **`index=4`**, on the closing tick. (4) The STRIKE at 600: `k=0 damage=0.000 inside` (the ring is 0.84 to 0.99 wide there: 12 − 0.03 × (600 − L − 190)). No STRIKE outside; `FIGHT stop` before 640. (5) No HURT | `MageCycleTest`, `BensNumbersTest` (400) | |

### FR7R2 — kills in the slow close: +1.0 from inside, capped at 12; none from outside (SURVIVAL) — supersedes FR7

| prediction | stands on | READING |
|---|---|---|
| (1) L from 18 to 23; he stands 2 west of M; the slow close from L+190. (2) Four `FIGHTKILL … killer=<Ben's uuid>`, the first at about L+264. **Kill 1:** `counted=true`, `r_before` from 9.5 to 10.1, **`r_after` = `r_before` + 1.000 exactly**. **Kill 2:** `counted=true`, `r_before` from 10.3 to 11.0, `r_after` = `r_before` + 1.000. **Kill 3:** `counted=true`, `r_before` above 11.000, **`r_after=12.000`**. **Kill 4** (from 16 west of M): **`counted=false`, `r_after` = `r_before`**. (3) Every STRIKE inside | `MageCycleTest`, `RingFixtureTest` | |

### FR8R2 — the strikes THROUGH ARMOUR: 20, 40, 80 before defence; inside resets; 20 again (SURVIVAL) — supersedes FR8

| prediction | stands on | READING |
|---|---|---|
| C 60 east of him; no Mage. (1) `STRIKE t=40 … k=1 damage=20.000 outside`, then **`HURT before=20.000 after=<20 × 100 / (100 + D)> defense=<D> dealer=self dealerType=self`**, D his defence as the HURT line prints it (Ben's read 10.000 at `f85280ee`, which gives 18.182). (2) `t=80 … k=2 damage=40.000 outside`; `HURT before=40.000 after=<40 × 100 / (100 + D)>`. (3) `t=120 … k=3 damage=80.000 outside`; `HURT before=80.000 after=<80 × 100 / (100 + D)>`. (4) `t=160 … k=0 damage=0.000 inside`, no HURT. (5) `t=200 … k=1 damage=20.000 outside`; `HURT before=20.000 after=<20 × 100 / (100 + D)>`. (6) Exactly one HURT per outside strike. (7) No ABORTED | `MaxValueStrikeTest` (the curve on the ramp's figures), `FightPortSourceTest` (APPLIES) | |

### FR9R2 — mage-die in the slow close: the crystal out, back to 12 at 0.6, the fast close reversed; IDLE (SURVIVAL) — supersedes FR9

| prediction | stands on | READING |
|---|---|---|
| (1) L from 18 to 23; S = L + 190 its SLOW_CLOSE line; `CRYSTAL t=S+80 lit index=1`. (2) **`FIGHT die t=D phase=RETURNING slow_close_started=t=S ticks_in_slow_close=<D − S>`**, D about S + 101 (the kill lands a tick after the command); **`FIGHTPHASE t=D SLOW_CLOSE->RETURNING`** and **`CRYSTAL t=D removed index=1`** on the same tick. (3) **`FIGHTPHASE t=H RETURNING->IDLE` with H − D = (D − S) + 190 exactly** (the slow part replays 0.03 a tick, then the 190-tick fast close reversed). (4) From H, `phase=IDLE … r=48.000`. (5) No `FIGHTKILL` (his death is not a kill) | `RingFixtureTest` *aDeathLogs…*, *theDeathLine…*, *crystals…GoOut…* | |

## THE MAGE (F6)

### MG1 — land spawns one tagged Mage at M, glowing, 1500 at gear score 100 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=L dx=6.000 dz=0.000 result=STARTED`; `FIGHT mage spawned t=L id=<uuid> at=6.00 0.00 0.00`. (2) **`FIGHTPROBE t=<L+2 to L+3> mage=<the same uuid> at=6.00 0.00 0.00 glowing=true ai=true health=1500.000/1500.000 defense=<D> crystals=0 beamsOnMage=0`**. (3) The PROBE_ENTITIES `wither_skeleton` within 16: **1** (only him) | `MobLoaderStanceTest`, the seed (`MobScaling`: authored × GS / 100) | |

### MG2 — in FAST_CLOSE a melee hit and a store hit are both refused; his health unchanged (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE attack Player#attack target=<his uuid> wither_skeleton …`, then **`MAGEGATE t=<t> phase=FAST_CLOSE amount=<a> source=vanilla/entity_attack refused`** (a the vanilla hit's amount, any). (2) After `rpg mobdamage 50`: **`MAGEGATE t=<t> phase=FAST_CLOSE amount=50.000 source=hit refused`**. (3) **`FIGHTPROBE … health=1500.000/1500.000 …`** after both. (4) No damage number reached him (no store change). Exactly two MAGEGATE lines | `MageGateTest`, `FightWiringSourceTest` | |

### MG3 — in SLOW_CLOSE a store hit of 100 lands: health falls by the pipeline's exact figure (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHTPHASE t=L+222 FAST_CLOSE->SLOW_CLOSE`. (2) The first FIGHTPROBE (about L+242): `health=1500.000/1500.000 defense=<D>`. (3) **No MAGEGATE line** in the row. (4) The second FIGHTPROBE: **`health=<1500 − 100 × 100 / (100 + D)>/1500.000`**, D as the first probe printed it (`Defense.applyDefense`; D = 0 gives **1400.000**) | `MageGateTest` (allowed when damageable), `Defense` | |

### MG4 — a Mage HELD while the ring returns appears at once and cannot be hurt (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The first Mage dies (`FIGHT die … phase=RETURNING …`); then `FIGHT land t=<t> dx=6.000 dz=0.000 result=HELD` and **`FIGHT mage spawned t=<the same t> …`** (he appears at once). (2) After `rpg mobdamage 50`: **`MAGEGATE t=<t> phase=RETURNING amount=50.000 source=hit refused`**. (3) **`FIGHTPROBE … mage=<the NEW uuid> … health=1500.000/1500.000 …`** | `MageCycleTest` (held), `MageGateTest` | |

### MG5 — his death in SLOW_CLOSE: RETURNING on that tick, every crystal out, the ring home (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) S = L + 222; `CRYSTAL t=S+80 lit index=1` and `CRYSTAL t=S+160 lit index=2`. (2) `rpg mobdamage 1000000` at about S+170 is ALLOWED (no MAGEGATE); **`FIGHTPHASE t=D SLOW_CLOSE->RETURNING`**, **`CRYSTAL t=D removed index=1`** and **`CRYSTAL t=D removed index=2`** on that one tick, and `FIGHT die t=D …`. (3) No FIGHTKILL. (4) `FIGHTPROBE … mage=none crystals=0 beamsOnMage=0`. (5) **`FIGHTPHASE t=H RETURNING->IDLE` with H − D = (D − S) + 222**; from H every RINGTICK `phase=IDLE … r=48.000` | `RingFixtureTest`, `MageCycleTest` | |

## THE CRYSTALS (F7)

### MG6 — one crystal every 80 ticks from the slow close; the live count equals RINGTICK's; every beam on the Mage (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| He stands at C + 5, 1 from M. (1) S = L + 222; **`CRYSTAL t=S+80 lit index=1`, `t=S+160 lit index=2`, `t=S+240 lit index=3`**, one each, exactly 80 apart. (2) The three FIGHTPROBEs, at about S+88, S+168 and S+248: **`crystals=1 beamsOnMage=1`, `crystals=2 beamsOnMage=2`, `crystals=3 beamsOnMage=3`**. (3) The RINGTICK nearest each probe reads the same `crystals=` | `RingFixtureTest` *crystalsLight…* | |

### MG7 — a crystal hit by the player and caught in a blast stays (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The first FIGHTPROBE (about S+98): `crystals=1 beamsOnMage=1`. (2) **`FIGHTCRYSTAL damage refused cause=entity_attack amount=5.000`** (vanilla `/damage … player_attack by @s`). (3) **`FIGHTCRYSTAL damage refused cause=entity_explosion amount=<any>`** (the TNT, its fuse key read from the jar: `PrimedTnt` reads `fuse`). (4) The second FIGHTPROBE: **`crystals=1 beamsOnMage=1`**. (5) No `CRYSTAL … removed` before the stop | `FightWiringSourceTest` | |

### MG8 — POWERED on a server: fifteen spaced kills hold the ring open; the tenth crystal powers him on its exact tick (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| He stands 2 west of M = C. **(1) The staging, computed:** kills every 50 ticks from S+50 to S+750 add 15 blocks; the radius at S+800 is 12 − 0.03 × 800 + 15 = **3.0** > 0, and before every kill it is at most 11.0 (the first, at S+50, finds 10.5), so no kill is lost to the cap. (2) Fifteen `FIGHTKILL … counted=true`. (3) **`CRYSTAL t=S+80n lit index=n` for n = 1 to 10.** (4) **`FIGHTPHASE t=S+800 SLOW_CLOSE->POWERED`** and **`FIGHT FAILED_POWERED t=S+800`**, S = L + 190. (5) No STRIKE after S+800. (6) The FIGHTPROBE after: `crystals=10 beamsOnMage=10` (POWERED keeps them); the end probe after the stop: `end_crystal` 0 | `MageCycleTest` *theLastCrystalPowers…*, `RingFixtureTest` *poweredCrystalsStay…* | |

**The lightning through armour is FR8R2**; the brief's row of that name is not repeated.

---

## THE BEN ROWS — HIS WORDS, VERBATIM; "not sure" IS NOT STATED; ONE THING PER QUESTION

**The instrument:** the `ASK` line, question and answer on one line. Each prompt now says which answer it wants
(`yesno`: "Answer yes or no in chat."; `words`: "Answer in your own words in chat."). **No PASS or FAIL is written for
a Ben row.** Its auto parts are read by the rules above.

### MQ1 — the fast close at 3.8 (SURVIVAL)

| prediction | READING |
|---|---|
| ASK 1 (`yesno`) "Did the fast close's speed feel right?" answered. Auto: `FIGHT land … dx=36.000 … STARTED`; the stop about 120 ticks after L; at most three STRIKE `outside` (k = 1, 2, 3), each with one HURT `after = before × 100 / (100 + D)` | |

### MQ2 — the slow close at 0.6 (SURVIVAL)

| prediction | READING |
|---|---|
| ASK 2 (`yesno`) "Is the slow close's pace right now?" answered (after the stop). Auto: `FIGHTPHASE … FAST_CLOSE->SLOW_CLOSE` 190 after L; every STRIKE inside | |

### MQ3 — the wall: height, gaps, twist (SURVIVAL)

| prediction | READING |
|---|---|
| ASK 3 (`yesno`) "Is the wall's height right?", ASK 4 (`yesno`) "Are the gaps between the rows right?", ASK 5 (`yesno`) "Is the twist right?" answered. Auto: RINGTICKs `phase=IDLE r=48.000 points=1208 viewers=1` | |

### MQ4 — hitting the Mage while he cannot be hurt (SURVIVAL)

| prediction | READING |
|---|---|
| ASK 6 (`yesno`) "When you hit him, was it clear that the hit did not count?" and ASK 7 (`words`) "What should a hit that does not count look or sound like?" answered. Auto: one `MAGEGATE … phase=FAST_CLOSE … refused` per hit he lands, his health unchanged (no store change) | |

### MQ5 — the crystals: the beams, and their pace (SURVIVAL)

| prediction | READING |
|---|---|
| ASK 8 (`yesno`) "Could you see a beam from each crystal to the Mage?" and ASK 9 (`yesno`) "Ten crystals, one every four seconds: is that pace right?" answered. **The beam outside the End is UNVERIFIED (PLAN §4 e); ASK 8 is its only witness.** Auto: `FIGHTPROBE … crystals=4 beamsOnMage=4` near S+338 | |

### MQ6 — the Mage's name (SURVIVAL)

| prediction | READING |
|---|---|
| ASK 10 (`yesno`) "His name is "Withered Dark Mage". Keep it?" answered | |

### MQ7 — THE PLAY ROW: Ben fights the Mage with his own weapon (SURVIVAL)

| prediction | READING |
|---|---|
| (1) `FIGHT land …` at a random spot at least 15 from C; `FIGHT play on t=<t> keepalive=10 strike_limit=3`. (2) It ends ONE of three ways, and the log says which: **`FIGHT play ended t=<t> reason=mage-died`** (then `FIGHT die … ticks_in_slow_close=<the time to kill, ticks>`); **`FIGHT play ended … reason=FAILED_CLOSED`** or **`FAILED_POWERED`** (then a FIGHTPROBE with his health); **`FIGHT strike-limit …`**; or the 90-second `rpg fight stop` (a FIGHTPROBE with his health first). **Damage dealt** = 1500 − the last FIGHTPROBE's health (1500 if killed). (3) He does not die. ASK 11 (`yesno`) "For one player, is 1500 health right for the Mage?" answered | |

---

## WHAT THIS GATE DOES NOT READ

- **The beam rendering outside the End** (UNVERIFIED; MQ5's ASK 8).
- **What the client draws:** the wall's look, the glow and the name are read only by Ben (MQ3, MQ6).
- **Folia:** the fixture reads participants and pins the Mage from C's region thread, safe on Paper only.
- **A mob's own melee on the Mage** while he is damageable lands on vanilla health, not the store. The survey of
  2026-11-18 found mob-on-mob melee is never rerouted. That is UNVERIFIED in game and not staged here; the gate refuses
  it while he cannot be hurt.
