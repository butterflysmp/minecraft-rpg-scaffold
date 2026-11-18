# GATE — THE RING DRAWN AND THE LIGHTNING, IN AN ORDINARY WORLD (STEPS F4 + F5)

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/fight-ring`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate from
origin, and Ben says ready. Then only CC boots.**

**What it gates.** `PLAN-dragon-fight.md` §6, steps F4 and F5, in the ruled order (Ben, 2026-11-18: "Order is fine"):
the storm ring drawn with forced particles, read only from core's `MageCycle`; the lightning outside it, Ben's ramp
20 × 2^(k−1), through our own damage pipeline. **No Mage mob, no crystals, no arena, no dragon.** A dev fixture,
`/rpg fight start | land <dx> <dz> | mage-die | stop`, drives one `MageCycle` and one `StrikeRamp` in an ordinary world.

## GAME MODE, DIFFICULTY

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL, and the fixture's participants are survival or
  adventure players only (creative and spectator are never struck or drawn to). The damage rows (FR8, BQ2, BQ5) are
  SURVIVAL by that rule.
- **The difficulty is set by the selftest:** the setup's `difficulty: normal`, its `DIFFICULTY found=… set=NORMAL`, and
  `RESTORED … difficulty=<what it found>`.

## THE NUMBERS THE ROWS RUN ON (`content/fights/dragon.yml`)

| number | value | whose |
|---|---|---|
| border, Mage ring | 48, 12 blocks | Ben's |
| `v_fast` | 5.0 blocks/s (the fastest point) | **seat placeholder, not Ben's** |
| `v_slow` | 0.4 blocks/s | **seat placeholder** |
| `e_kill` | 1.0 block | **seat placeholder** |
| crystals | 10, one every 80 ticks from the slow close | **seat placeholder** |
| strikes | every 40 ticks; the k-th in a row deals 20 × 2^(k−1) | Ben's |
| mitigation | none: no armour, no element (`DefenseRule.BYPASSED`, element null) | **seat placeholder, flagged to Ben (BQ5)** |
| the look | dust RGB 170, 0, 255 size 1.6 (cfde822's `StormSystem.STORM_PURPLE_DUST`, read at `cfde822`), a point every ≤ 1.0 block, at C's block y + 0.2, + 1.2, + 2.2, every 4 ticks | the seat's starting look; Ben judges (BQ1, BQ6) |
| participants | C's world, within 128 blocks of C, survival or adventure | the brief's |

**Derived, EXECUTED in core's tests (`FightRingTest`, `RingPointsTest`, `RingFixtureTest`):** the fast close lasts
(d + 36) / 5 s: **144, 216, 288 ticks** at d = 0, 18, 36. The slow close shuts 12 / 0.4 = 30 s = **600 ticks** after it
starts with no kills. **At radius 48 a redraw is 302 points a height, 906 in all**; at 12, 76 a height, 228 in all.

**THE PARTICLE COST, as the brief asked:** at radius 48, **906 particles per redraw, one packet each** (READ: one
`spawnParticle` call is one `ClientboundLevelParticlesPacket`, `CraftPlayer.spawnParticle`, build 74), 5 redraws a second:
**4530 packets a second per player** while the ring is at 48. cfde822's border sent 220 a redraw at the same period.
The spacing, the heights and the period are content values.

**THE FORCED SEND NEEDS NO PACKETEVENTS (READ, javap of `run/versions/26.1.2/paper-26.1.2.jar`, build 74):**
`CraftPlayer.spawnParticle(Particle, x, y, z, count, ox, oy, oz, extra, data, boolean force)` builds
`ClientboundLevelParticlesPacket(options, overrideLimiter = force, alwaysShow = false, …)` and sends it straight to that
player's connection, with no distance check. `alwaysShow` stays false (Ben: "I don't care about the minimal particle
setting").

## THE CLOCK, AND WHAT A TICK READING MEANS

The fight clock is **0 at `start` and 1 on the loop's first tick**; RINGTICK on every 4th tick, STRIKE on every 40th.
A command acts at the fight's current tick, so **a landing at L logs `FIGHT land t=L` and `FIGHTPHASE t=L
IDLE->FAST_CLOSE` on the same tick.** The fixture loop and the selftest's steps run on different schedules, so **L is
read from the log, never predicted to the tick:** each row says L's expected window (its waits ± 2), and every
transition is predicted RELATIVE to the L it reads.

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC

**His custom maximum is at least 100** (`CombatantStats.DEFAULT_PLAYER_BASE`; gear raises it). **The largest strike any
row lets land is 80**, and every row that can strike him heals him to full (`rpg heal 1000000`) before each strike tick
it can reach:
- **FR8, BQ5:** heals at about t = 30, 50, 90 (and 130 in FR8); strikes 20 at 40, 40 at 80, 80 at 120: each lands on a
  full bar. FR8's last strike (200) is 20, after a heal.
- **BQ2:** a heal every 10 ticks for the whole 120 ticks after the landing; caught at once, he takes at most 20, 40, 80,
  each on a bar healed within 10 ticks. The fixture stops at 120 ticks, before a fourth strike (160) could exist.
- **Every other row** keeps him at the Mage's point M, which lies inside every ring the cycle draws (core-tested:
  `|M − c(s)| = (1 − s)d ≤ 48 − 36s` for d ≤ 36), or stops before the ring can shut (FR6 stops at about L + 750, before
  the strike at 800; BQ3 stops before its question).
- **A fresh survival player, no gear, standing outside, NOT healed:** survives 2 strikes in a row (20 + 40 = 60) and
  dies on the third (80 more). **The rows never let that happen.**

## CARRIED STATE, DONE BY THE BOOTER (CC) BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-fight-ring"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type d | wc -l) dirs"; done; diff -rq run/plugins/Rpg "$B/Rpg" | wc -l | sed 's/^/diff -rq lines Rpg: /'; diff -rq run/world "$B/world" | wc -l | sed 's/^/diff -rq lines world: /'
   ```

   If the folder exists, add `-2`. Report the folder, both pairs of counts, and `diff -rq lines: 0` for each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **Boot this branch's head with `--refresh-content`:** `./scripts/dev-server.sh --refresh-content`, on Ben's "ready".
   **REQUIRED:** the deployed `run/plugins/Rpg/content/fights/dragon.yml` exists and reads `  first_strike_damage: 20.0`.
5. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` names this branch's head.
6. **Tell Ben R0 passed before he logs in.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **Wait for my word that R0 passed before you log in.**
1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest fight-ring` — **ONCE.** It takes about **four minutes** of rows, plus the time you take to answer
   (3,800 ticks of waits, about 190 seconds, plus staging; 15 rows). Stand still and touch nothing, **except when gold
   text in chat tells you to do something, or a gold question appears**: do what it says, and answer each question in
   chat. There are **seven** questions. **Lightning will strike you in two rows (you are healed between strikes); that
   is expected.** **Question 2 asks you to SPRINT EAST for six seconds: the run starts it where you stand, so stand in
   open ground with about 40 clear blocks to your east before you type line 2.** Wait for the chat to say the run
   finished.
3. `/stop`

Nothing else this boot.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/fight-ring` prints it, shortened. **Not** master's `efc29809`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries the fight content, the fixture, the port and the rows

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/fights/dragon.yml` line `  first_strike_damage: 20.0`; `RingFixture.class` carrying `RINGTICK t=` and `FIGHTPHASE t=`; `BukkitFightPort.class` carrying `strikeLightningEffect`; `FightCommand.class` carrying `mage-die`; `GATE-fight-ring.yml` carrying `FR8:` and `BQ6:`. ABSENT: `  first_strike_damage: 99.0` (the YAML needle's control) and `NoSuchTextControl` (the class needle's control) | the PowerShell below, from the repo root | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$y = Bytes 'content/fights/dragon.yml'
foreach ($l in '  first_strike_damage: 20.0', '  first_strike_damage: 99.0') { if ($y -ne $null -and $y -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT dragon.yml '$l'" } else { "ABSENT  dragon.yml '$l'" } }
$f = Bytes 'io/github/butterflysmp/rpg/core/fight/RingFixture.class'
foreach ($t in 'RINGTICK t=', 'FIGHTPHASE t=', 'NoSuchTextControl') { if ($f -ne $null -and $f.Contains($t)) { "PRESENT RingFixture '$t'" } else { "ABSENT  RingFixture '$t'" } }
$p = Bytes 'io/github/butterflysmp/rpg/paper/fight/BukkitFightPort.class'
if ($p -ne $null -and $p.Contains('strikeLightningEffect')) { "PRESENT BukkitFightPort 'strikeLightningEffect'" } else { "ABSENT  BukkitFightPort 'strikeLightningEffect'" }
$c = Bytes 'io/github/butterflysmp/rpg/paper/fight/FightCommand.class'
if ($c -ne $null -and $c.Contains('mage-die')) { "PRESENT FightCommand 'mage-die'" } else { "ABSENT  FightCommand 'mage-die'" }
$s = Bytes 'selftest/GATE-fight-ring.yml'
foreach ($k in 'FR8', 'BQ6') { if ($s -ne $null -and $s -match "(?m)^$($k):\s*$") { "PRESENT GATE-fight-ring $($k):" } else { "ABSENT  GATE-fight-ring $($k):" } }
$zip.Dispose()
```

**A jar from master reads ABSENT on every line.**

### R0c — the Loaded lines, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`** (as `GATE-drag-additive.md` R0c read it; this branch adds no file those counts cover), AND exactly one **`[Rpg] Loaded fight 'dragon': border 48.0, mage ring 12.0, v_fast 5.00, v_slow 0.40, e_kill 1.00, 10 crystals every 80 ticks, a strike every 40 ticks from 20.0; ring dust (170,0,255) size 1.60 every 1.00 blocks at [0.2, 1.2, 2.2] every 4 ticks; participants within 128.0`**. **No** line matching `Refusing\|Skipping\|SEVERE\|WARN\|Exception` from `[Rpg]`, and **no** `No fights loaded` line | `Select-String run\logs\latest.log -Pattern 'Loaded\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded'` | |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 184 scenarios across 22 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive, fight-ring]`**, and no scenario refusal after it | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**The 184 is RECOUNTED** (2026-11-18, on this branch's tree): in `paper/src/main/resources/selftest/`,
`grep -cE '^  - ' index.yml` prints **22**; `cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/;
s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints **206**; with `'^setup:'`, **22**: **184**. **The control:** without
`GATE-fight-ring.yml`'s 16 keys (a setup and 15 rows) the same count is 190 − 21 = **169**, the figure
`GATE-drag-additive.md` R0d read at master's boot.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument for every row:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST fight-ring|\[Rpg\] RINGTICK|\[Rpg\] FIGHT|\[Rpg\] STRIKE'`,
read after Ben's LOGIN, inside the row's own `SELFTEST fight-ring <row> START … DONE` bracket. `\[Rpg\] FIGHT` also
matches FIGHTPHASE and FIGHTKILL. **A log-line row passes when the predicted line appears VERBATIM** (a `<…>` part is
the value the row says it may take).

**In EVERY row (not repeated):** after `CLEAN …`, **`LEFTOVER living=0 radius=16`** and **`LINGER work=0 entities=0
radius=48`**. One `[Rpg] FIGHT start world=<w> centre=<x> <y> <z> by=<Ben>` per `start`, and one `[Rpg] FIGHT stop
t=<t>` and one `[Rpg] FIGHT ended reason=command` per `stop`. **Every RINGTICK reads `viewers=1`.** A STRIKE line on
each strike tick the row reaches: **`k=0 damage=0.000 inside`** unless the row says otherwise. **No HURT line in any
row except FR8's, at the ticks it names.** A non-zero LEFTOVER or LINGER makes its row NOT AS PREDICTED.

### FR1 — start: the ring is the border at C, IDLE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `RINGTICK t=4 phase=IDLE centre=0.000 0.000 r=48.000 crystals=0 points=906 viewers=1` and the same at `t=8`. (2) No FIGHTPHASE line. (3) `FIGHT stop t=<8 to 11>` | `RingFixtureTest` *startIsTheBorderAtC…*; `RingPointsTest` (906) | |

### FR2 — land 0 0: 48 to 12 in exactly 144 ticks; SLOW_CLOSE on that tick (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=L dx=0.000 dz=0.000 result=STARTED` with **L from 18 to 23**, and `FIGHTPHASE t=L IDLE->FAST_CLOSE`. (2) **`FIGHTPHASE t=L+144 FAST_CLOSE->SLOW_CLOSE`**, exactly 144 after L. (3) Each RINGTICK at t from L+1 to L+143: `phase=FAST_CLOSE centre=0.000 0.000 r=<48 − 0.25 × (t − L)>` to 3 places, `crystals=0`. (4) Each RINGTICK at t ≥ L+144: `phase=SLOW_CLOSE centre=0.000 0.000 r=<12 − 0.02 × (t − L − 144)>`, `crystals=0` (the first crystal is at L+224, after the stop), `points=228` while the radius is above 11.937 (76 a height: `ceil(2π r)` is 76 above 75 / 2π = 11.937), fewer below it. (5) Every STRIKE `inside` | `FightRingTest` (duration), `MageCycleTest` (exact tick) | |

### FR3 — land 18 0: 216 ticks, the centre on the straight line (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=L dx=18.000 dz=0.000 result=STARTED`, L from 18 to 23. (2) **`FIGHTPHASE t=L+216 FAST_CLOSE->SLOW_CLOSE`.** (3) Every FAST_CLOSE RINGTICK: `centre=<dx> 0.000` with **dx = (48 − r) / 2 ± 0.001** (the straight line: centre 18s, radius 48 − 36s), r falling 36/216 = 0.1667 a tick. (4) Every SLOW_CLOSE RINGTICK: `centre=18.000 0.000`. (5) Every STRIKE `inside` (he stands on M) | `FightRingTest` (line, nesting, duration) | |

### FR4 — land 36 0: 288 ticks, the centre on the straight line (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=L dx=36.000 dz=0.000 result=STARTED`, L from 18 to 23. (2) **`FIGHTPHASE t=L+288 FAST_CLOSE->SLOW_CLOSE`.** (3) Every FAST_CLOSE RINGTICK: **dx = 48 − r ± 0.001**, dz 0.000. (4) Every SLOW_CLOSE RINGTICK `centre=36.000 0.000`. (5) Every STRIKE `inside` | as FR3 | |

### FR5 — land 36.01 0: REFUSED_TOO_FAR, the ring unchanged (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=<5 to 8> dx=36.010 dz=0.000 result=REFUSED_TOO_FAR`, and the REPLY `Mage lands at C + (36.01, 0.00): REFUSED_TOO_FAR`. (2) No FIGHTPHASE line. (3) Every RINGTICK `phase=IDLE centre=0.000 0.000 r=48.000 crystals=0 points=906 viewers=1` | `FightRingTest` (d = 36.01 refused), `MageCycleTest` | |

### FR6 — the slow close with no kills: CLOSED 600 ticks after it starts (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) L from 18 to 23; `FIGHTPHASE t=L+144 FAST_CLOSE->SLOW_CLOSE`. (2) **`FIGHTPHASE t=L+744 SLOW_CLOSE->CLOSED`** and **`FIGHT FAILED_CLOSED t=L+744`**, once; the chat line `The fight has failed: FAILED_CLOSED (fixture: nobody is killed).` (3) The last SLOW_CLOSE RINGTICK reads `crystals=7` (600 / 80, floored); every RINGTICK after the close `phase=CLOSED … r=0.000 crystals=0 points=0`. (4) **No STRIKE `outside` line** (the strike at 760 finds him at M, inside a ring of radius 0.04 to 0.14 for L from 18 to 23; the stop at about L + 750 comes before 800). (5) No HURT line | `MageCycleTest` *withNoKills…*, `RingFixtureTest` *closedStrikesGoOn…* | |

### FR7 — kills: +1.0 from inside, capped at 12; a kill from outside does not count (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) L from 18 to 23. (2) Four `FIGHTKILL` lines, each `killer=<Ben's uuid>`, the first at about L + 228. **Kill 1:** `counted=true`, `r_before` from 10.0 to 10.6, **`r_after` = `r_before` + 1.000 exactly**. **Kills 2 and 3:** `counted=true`, `r_before` above 11.000, **`r_after=12.000`** (the cap). **Kill 4** (from 14 east of M, outside a ring of at most 12): **`counted=false`, `r_after` = `r_before`**. (3) The RINGTICK after each counted kill reads the larger radius. (4) Every STRIKE `inside` (he is at M at the strike ticks 240 and 280; kill 4 is between them). (5) Each spawn's `DRIVE spawn …` and its reply | `MageCycleTest` *aKillCountsOnly…*, `FightRingTest` *aKillAdds…*; `RingFixtureTest` *aKillIsLogged…* | |

### FR8 — the strikes: 20, 40, 80; inside resets; outside again 20 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| C 60 east of him: he is outside the 48 border. (1) `STRIKE t=40 player=<Ben> k=1 damage=20.000 outside`, then its **`HURT before=20.000 after=20.000 defense=<his> dealer=self dealerType=self`**. (2) `STRIKE t=80 … k=2 damage=40.000 outside`; `HURT before=40.000 after=40.000 …`. (3) `STRIKE t=120 … k=3 damage=80.000 outside`; `HURT before=80.000 after=80.000 …`. (4) At 45 from C: `STRIKE t=160 … k=0 damage=0.000 inside`, **no HURT**. (5) Back at 60: **`STRIKE t=200 … k=1 damage=20.000 outside`**; `HURT before=20.000 after=20.000 …`. (6) **after = before on every HURT** (no armour cut), and **exactly one HURT per outside strike**: a second would be vanilla lightning's own damage, which the effect-only bolt must not deal. (7) He does not die: no `ABORTED` | `StrikeRampTest`, `RingFixtureTest` *outsideTheRing…*; `FightPortSourceTest` (the effect-only bolt, BYPASSED) | |

### FR9 — mage-die in the slow close: back to 12 at 0.4/s, then the fast close reversed, home; IDLE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) L from 18 to 23; S = L + 144 its SLOW_CLOSE line. (2) `FIGHT die t=D phase=RETURNING` and `FIGHTPHASE t=D SLOW_CLOSE->RETURNING`, D about S + 100. (3) **`FIGHTPHASE t=H RETURNING->IDLE` with H − D = (D − S) + 144 exactly.** (4) RINGTICKs from D to D + (D − S): `centre=0.000 0.000`, `crystals=0`, r rising 0.02 a tick from its value at D; then r rising 0.25 a tick to 48. (5) After H: `phase=IDLE … r=48.000` | `MageCycleTest` *theMagesDeath…*, `RingFixtureTest` *aDeathLogsItsPhase…*, `FightRingTest` (return reversed) | |

---

## THE BEN ROWS — HIS WORDS, VERBATIM; "not sure" IS NOT STATED

**The instrument:** the `ASK` line, question and answer on one line. Each answer is recorded VERBATIM beside its row.
**The auto parts of these rows are read too, by the rules above** (a BQ row's STRIKEs, HURTs and LEFTOVER are
predictions like any other).

### BQ1 — standing at C with the ring at 48: can he see the whole ring clearly? (SURVIVAL)

| prediction | READING |
|---|---|
| ASK 1 answered. Auto: RINGTICKs `phase=IDLE r=48.000 points=906 viewers=1`; every STRIKE `inside` | |

### BQ2 — the fast close toward a far landing: can he outrun it sprinting; does it feel "slightly slower than a player sprinting"? (SURVIVAL)

| prediction | READING |
|---|---|
| ASK 2 answered. Auto: `FIGHT land … dx=36.000 dz=0.000 result=STARTED`; the stop about 120 ticks after L; **at most three STRIKE `outside` lines (k = 1, 2, 3) and their HURTs**, each `after = before`; he does not die | |

### BQ3 — the slow close: is the pace right? (SURVIVAL)

| prediction | READING |
|---|---|
| ASK 3 answered (after the stop). Auto: `FIGHTPHASE … FAST_CLOSE->SLOW_CLOSE` 144 after L; every STRIKE `inside` | |

### BQ4 — a kill's one-block pop: fine, or should it glide? (SURVIVAL)

| prediction | READING |
|---|---|
| ASK 4 answered. Auto: one `FIGHTKILL … counted=true` with `r_after` = `r_before` + 1.000 (the kill comes about 120 ticks into the slow close, radius about 9.6) | |

### BQ5 — the lightning: do 20, 40, 80 feel right, and should armour reduce it? (SURVIVAL)

| prediction | READING |
|---|---|
| ASK 5 and ASK 6 answered. Auto: STRIKEs `k=1 20.000`, `k=2 40.000`, `k=3 80.000`, all `outside`, each with one HURT `after = before` | |

### BQ6 — the ring's look: colour, height, density (SURVIVAL)

| prediction | READING |
|---|---|
| ASK 7 answered. Auto: as BQ1 | |

---

## WHAT THIS GATE DOES NOT READ

- **The ring as the CLIENT draws it** (visibility, density, the Minimal setting): the server sends; only Ben's eyes
  (BQ1, BQ6) read the result (`.claude/rules/verification.md`, *AN AUTO READING IS A READING OF THE SERVER*).
- **The bolt's look and sound:** effect-only by the call made (`FightPortSourceTest`); FR8 (6) reads that it dealt
  nothing of its own.
- **Folia:** the fixture reads participants from C's region thread, safe on Paper only (`BukkitFightPort`'s javadoc;
  PLAN §6.2).
