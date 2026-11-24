# GATE — THE BREACHER'S REACH OF 18, AND SHATTER CAPS

**Status: NOT RUN.** Written 2026-11-24 on `feat/shatter-caps` (PR 2: Shatter Caps), stacked on `feat/breacher-reach`
(PR 1, #268: the reach), on `docs/enchant-changes-owed` (#267, plan notes only), on master `89253ed4`. **THIS FILE CARRIES
THE ONE R0 FOR #267 AND BOTH PRs AND THE ONE RUN SHEET.** Every prediction below was written before any boot of this
code. **NO BOOT until the seat has diffed both PRs and this gate from origin and Ben has said "ready" in his own words.**
SURVIVAL, every row. Scenario: `paper/src/main/resources/selftest/GATE-shatter-caps.yml`, `/rpg selftest shatter-caps`.
ALL AUTO; no ASK.

## BEN'S WORDS THIS GATE SERVES, VERBATIM (2026-11-24), RECORDED IN `PLAN-enchant-columns.md` SECTION 16

- "Let's do a new enchantment for the Breacher. Shatter Caps. Instead of shooting 4 cobble right away, it shoots 1 stone
  that splits into 4 cobble after traveling 5 blocks. The damage of the Stone is 35/60/85" — SC1, SC3, SC2.
- Asked what the four chunks do after the split, how far they fly, and whether they keep the pattern: "1. the usual 40
  2. their full reach 3. the same pattern." — SC2, SF1, SF2.
- "One extra note, I want to increase the range that the cobble travels normally to 18 blocks" — SR1, SR2.
- "Another note, I want the damage of the shatter caps stone to follow with gearscore, just like the lapis crystal and
  airburst flames" — every stone sum below carries the score term.

## WHAT WAS READ OR EXECUTED, EACH MARKED

- **`breacher.yml` at `89253ed4` (READ, as the seat read it):** the right click is a projectile, speed 3.0, gravity 0,
  `max_lifetime_ticks` 4, item cobblestone, trail `breacher_trail`, spread count 4 / angle_degrees 3 / rotation_degrees 90;
  on_hit `breacher_hit`, damage 40 kinetic, knockback 0.1; quiver_size 2, reload_ticks 60, ammo cobblestone,
  cooldown_ticks 20. **PR 1 changes `max_lifetime_ticks` to 6 and nothing else** (6 x 3.0 = 18.0, EXECUTED).
- **The split (EXECUTED):** `ticksToTravel(5, 2.5)` = `max(1, ceil(5 / 2.5 - 1e-9))` = **2**; `2 x 2.5` = **5.0**. So the
  stone is launched with its first step inline at the press (0 to 2.5) and splits at the end of its 2nd step, **exactly
  5.000 from his eye along the aim, one tick after the press**.
- **The mechanism: A SECOND BEHAVIOUR TYPE, `EnchantBehaviour.Shatter`, BESIDE `Airburst`, NOT A SHARED SEAM.** Airburst
  cuts the AUTHORED shot short and launches the ENCHANT's bursts; Shatter replaces the authored spread with the ENCHANT's
  stone and, at the split, launches the WEAPON's own cast. A shared seam would have re-routed Airburst's path; a separate
  type leaves it untouched (`CastExecutor.launchToSplit` and `split` are byte-for-byte as `GATE-enchant-changes` read them;
  `AirburstTest` and `AirburstSeekTest` unchanged and green). The stone is taken in `CastExecutor.launch`, where the
  spread is expanded, BELOW the commit, so the press bills one round and one cooldown as the spread does
  (`WeaponFire`'s account, READ: a spread projectile passes no yaw offsets, so one round per press).
- **The weapon check (READ):** `BehaviourItems.activeOf` returns every behaviour on the held item's enchant state, with no
  weapon filter, and the dev command can stage any enchant on any weapon; so `Shatter` carries its `weaponId` and acts only
  when the cast's ability id is `<weaponId>/...`, as the Lapis Crystal's grant does. **FINDING, not fixed here (Airburst
  must stay as read): `Airburst` has no such check**, so Airburst Flames staged by the dev command on another projectile
  weapon would split that weapon's shot. No roll can produce that state (W1).
- **The basis at the split (READ, `SpreadPattern.directionsFor`, `Aim`):** the spread is built from `forward`, `right` and
  `up = right x forward`. At a press `right` is the shooter's own (`Aim` carries it; the paper side derives it from his
  yaw), so it is defined at every pitch, straight up included. **At the split the same `aim.right()` is carried in**, with
  `forward` the stone's direction of travel (gravity 0 and no homing, so the aim itself): the four leave in exactly the
  press's pattern, rotation 90 putting the triangle's point UP (`ShatterCapsTest`, both pitch 0 and straight up).
- **A stone that meets a BLOCK before 5 (READ, `ProjectileFlight.step`, `EffectApplier.apply`):** the flight calls
  `at(null, point)`; with a null target the targeted effects (the damage, the knockback) are skipped and the untargeted
  one (the `breacher_hit` visual) plays where it stopped. **It ends as a chunk ends there: the hit visual, no damage, and
  no split.**
- **The chunks' geometry on the server (READ: `PaperCombatWorld.RAY_SIZE` 0.0, so a zero-width ray against each mob's
  exact box; a zombie `sized(0.6f, 1.95f)`, READ):** each ring chunk leaves at `tan 3 = 0.05241` (EXECUTED) per block
  forward; the up chunk climbs that, the lower pair sink `0.02620` and spread `0.04539` sideways. His eye is 1.62 over his
  feet, the zombie's head 1.95. **EXECUTED at each mob's front face:**

| mob, distance from where the chunks leave to its front face | up chunk y | pair y | pair \|z\| | lands |
|---|---|---|---|---|
| 17 out, no enchant (16.7 from his eye) | 2.495 (0.545 over) | 1.182 | 0.758 (0.458 outside) | **the centre chunk only** |
| 8 out, at the split (2.7 past the split point) | 1.7615 (0.1885 under) | 1.549 | 0.1225 (0.1775 inside) | **all four** |
| 22 out, after the split (16.7 past the split) | 2.495 (0.545 over) | 1.182 | 0.758 (0.458 outside) | **the centre chunk only** |

  The ends: no enchant, the chunks stop 18.000 from his eye, and a zombie 19 out has its face at 18.7 (**0.7 beyond**); a
  zombie 17 out has its face at 16.7 (**1.3 inside**). After the split they stop at 5 + 18 = 23.000 from his eye; a zombie
  24 out has its face at 23.7 (**0.7 beyond**), one 22 out at 21.7 (**1.3 inside**). The stone reaches a zombie 3 out on its
  2nd step (2.5 to 5.0 crosses its face at 2.7). **That the chunks' knockback of 0.1 does not move a frozen NoAI zombie is
  INFERRED** (the probes' `d=` read it).
- **The damage sums (`.claude/rules/verification.md`, *A DAMAGE PREDICTION NAMES EVERY TERM*):** `sent = ((amount x
  score/100) x enchantMultiplier + classDamageBonus + weaponLevelDamage) x chargeScale x crit`, staged: score 100
  (`rpg gearscore set 100`), enchantMultiplier 1.0 (no damage enchant active), classDamageBonus 0, weaponLevelDamage 0 at
  level 1 (`rpg playerxp set @s 1 levels`; `GATE-enchant-columns.md` EC14R2 read `sent=30.000` on the same staging),
  chargeScale 1.0 (an ability), crit x 1 or x 2 (the second the allowed reading). **A chunk: `(40 x 100/100) x 1.0 + 0 + 0
  = 40`. The stone: `(35 x 100/100) x 1.0 + 0 + 0 = 35` at I, `85` at III** (Ben: "follow with gearscore": the score term
  is the one that scales it). **A shockwave (Kinetic Tremors I): `(10 x the hit's score 100/100) x 1.0 + 0 + 0 = 10`,** x
  the hit's crit (`PLAN-enchant-columns.md` section 7, ruling 2).

## SAFETY: THE MOST HITS THE RUN CAN LAND ON HIM, TIMES THE BIGGEST, AGAINST HIS HEALTH

| source | can it reach him | hits x biggest |
|---|---|---|
| the chunks and the stone | a projectile's flight ignores its caster; all fly east, away from him | 0 x 0 |
| the shockwave | never himself, never a player (`BehaviourHits`) | 0 x 0 |
| the zombies | NoAI, frozen | 0 x 0 |
| the natural hostiles | killed by the setup; spawning and phantoms off | 0 x 0 |
| the fall | the pad (`SelfTestPadShapeTest`) | 0 |

**Sum: 0.** **Where each row puts him:** every row's clean returns him to the pad's stand point (`104.50 201.00 -3.50` on
`01ee2d65`), and every row turns him there to face due east and waits 40 before its one press. He is never elsewhere.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup** of `run/plugins/Rpg` and `run/world` in a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-shatter-caps` (first free `-2`, `-3`); counts, `diff -rq lines: 0` for each, the
   overworld's file count.
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `run/world/dimensions/rpgfight` holds nothing.
5. **The tree clean and pushed:** `git status --porcelain` prints nothing; `git rev-parse HEAD` equals the
   `git ls-remote --heads origin feat/shatter-caps` line.
6. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND:** "booting now", and Ben told NOT to log in until told R0 passed.
7. A marker made, then `./scripts/dev-server.sh --refresh-content`. **Required deployed lines:** `content/enchants/` holds
   **15** `.yml` files, none `dev_`, among them `shatter_caps.yml`; `content/weapons/breacher.yml` holds
   `      max_lifetime_ticks: 6`.
8. R0a to R0e on **this boot's** log, its `Done` matched to a log newer than a deploy newer than the marker.
9. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
10. *(After his `/stop`.)* `rpgfight` empty; the overworld's file count against step 2's; `All dimensions are saved`; no
    `java` process left; his saved position not the pad.

## THE RUN SHEET — BEN TYPES THESE THREE LINES

**IN PLAIN WORDS:** **stand anywhere in the dev world.** The selftest lifts you onto its platform in the sky, runs, and
puts you back where you were. Your inventory is emptied during the run and given back at the end. Do nothing while it
runs: the Breacher fires nine times by itself, sometimes four chunks, sometimes one stone that bursts into four.

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.**
1. `/gamemode survival`
2. `/rpg selftest shatter-caps` — **ONCE.** About **31 seconds**.
3. `/stop`

**This run's length:** the scenario's waits are **578 ticks** (recount: `tr -d '\r' < <the scenario> | awk '/- wait:
/{t+=$3} END{print t}'`), and about 5 per row for the cleans (9 rows: 45): **about 623 ticks, 31 s**, plus the pad's check.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

**#267 is plan notes only: nothing of it is in the jar**, so R0 reads PR 1 and PR 2.

### R0a — the build line names the stack top's head, clean

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `<sha>` being `feat/shatter-caps`'s head as `git ls-remote --heads origin feat/shatter-caps` prints it, shortened. **Not `-dirty`, not `unknown`, not `01ee2d65`.** | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries both PRs and this gate

Counted: `awk '/^.--- controls.$/{exit} /^(ValueLine\|Has) /' <the block> | wc -l` prints **28**, and **7** after the
controls line.

```powershell
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
$b = Bytes 'content/weapons/breacher.yml'
$s = Bytes 'content/enchants/shatter_caps.yml'
$g = Bytes 'selftest/GATE-shatter-caps.yml'
$i = Bytes 'selftest/index.yml'
$ce = Bytes 'io/github/butterflysmp/rpg/core/ability/CastExecutor.class'
$eb = Bytes 'io/github/butterflysmp/rpg/core/enchant/EnchantBehaviour$Shatter.class'
$el = Bytes 'io/github/butterflysmp/rpg/paper/content/EnchantLoader.class'
$ln = Bytes 'io/github/butterflysmp/rpg/paper/weapon/EnchantEffectLine.class'
ValueLine $b '      max_lifetime_ticks: 6' 'breacher.yml'
ValueLine $b '      speed: 3.0' 'breacher.yml'
ValueLine $b '        count: 4' 'breacher.yml'
ValueLine $s 'effect: shatter' 'shatter_caps.yml'
ValueLine $s 'weapon: breacher' 'shatter_caps.yml'
ValueLine $s '  travel_blocks: 5' 'shatter_caps.yml'
ValueLine $s '  speed: 2.5' 'shatter_caps.yml'
ValueLine $s '  item: stone' 'shatter_caps.yml'
ValueLine $s '  damage_by_level: [35, 60, 85]' 'shatter_caps.yml'
ValueLine $s '  element: kinetic' 'shatter_caps.yml'
ValueLine $g 'setup:' 'GATE-shatter-caps.yml'
ValueLine $g 'SR1:' 'GATE-shatter-caps.yml'
ValueLine $g 'SR2:' 'GATE-shatter-caps.yml'
ValueLine $g 'SC1:' 'GATE-shatter-caps.yml'
ValueLine $g 'SC3:' 'GATE-shatter-caps.yml'
ValueLine $g 'SC2:' 'GATE-shatter-caps.yml'
ValueLine $g 'SF1:' 'GATE-shatter-caps.yml'
ValueLine $g 'SF2:' 'GATE-shatter-caps.yml'
ValueLine $g 'ST1:' 'GATE-shatter-caps.yml'
ValueLine $g 'ST2:' 'GATE-shatter-caps.yml'
ValueLine $g '  - pad: ""' 'GATE-shatter-caps.yml'
ValueLine $i '  - shatter-caps' 'index.yml'
ValueLine $i '  - enchant-changes' 'index.yml'
Has $ce 'launchStone' 'CastExecutor.class'
Has $ce 'splitTicks' 'CastExecutor.class'
Has $eb 'shatter travel_blocks must be above 0' 'EnchantBehaviour$Shatter.class'
Has $el 'is not a projectile with a spread, so there is nothing to shatter into' 'EnchantLoader.class'
Has $ln 'fires one stone of ' 'EnchantEffectLine.class'
'--- controls'
ValueLine $b '      max_lifetime_ticks: 4' 'breacher.yml CONTROL-THE-OLD-4'
ValueLine $s '  damage_by_level: [35, 60, 8]' 'shatter_caps.yml CONTROL-PREFIX'
ValueLine $s '  speed: 2.0' 'shatter_caps.yml CONTROL-NONE'
ValueLine $g 'SR3:' 'GATE-shatter-caps.yml CONTROL-NONE'
ValueLine $i '  - shatter-cap' 'index.yml CONTROL-PREFIX'
Has $ce 'launchStones' 'CastExecutor.class CONTROL-NONE'
Has $eb 'airburst travel_blocks' 'EnchantBehaviour$Shatter.class CONTROL-WRONG-CLASS'
$zip.Dispose()
```

**The prediction, line for line:** before `'--- controls'`, **28 `PRESENT`, 0 `ABSENT`**; after it, **7 `ABSENT`, 0
`PRESENT`**. Run before the commit: see *R0b, RUN BEFORE COMMIT* at the end.

### R0c — every Loaded line, and nothing refused

`[Rpg] Loaded 11 abilities, 46 visuals, 5 statuses, 7 elements, 15 enchants, …` (**15 enchants: `shatter_caps` added**;
every other count as on `01ee2d65`), the candidates, `Mint-on-craft` and `Custom recipes` lines as there. **No `[Rpg]`
WARN beyond `volley_stone`'s known pair**, in particular none naming `shatter_caps` or `breacher`. No `Refusing`,
`Skipping`, `SEVERE`, `Exception`, `Dev-only`, `currently prevented`.

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 365 scenarios across 45 gates [melee-cell, …, enchant-changes, shatter-caps]`** (recount: 45 files, 410 keys, 45 setups; `SelfTestScenariosTest`). The controls: `356 scenarios` and `366 scenarios` ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

### R0e — the orphan sweep with nothing staged

One `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`.

## THE ROWS

Every row: SURVIVAL, level 1, score 100, a fresh Breacher with its quiver full (2), one zombie on his aim line at 1000
health, night. **Every row-start clean: `LINGER work=0 entities=0 radius=48`.** `<X, Y, Z>` is `PROBE self`'s position;
his eye is `<Y+1.620>`. A PLAYERHIT's `crit=true` with its sent doubled is the allowed second reading throughout.

### setup — the pad, the ground, normal, night, no spawns, the trace on (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `PAD world=world stand=104,201,-4 … placed=0 repaired=0 ok=7250`; `PAD stood at=104.50 201.00 -3.50 yaw=-90.000 pitch=0.000`; **`PREFLIGHT ok box=0..32 east -6..10 south air=1122/1122 floor solid=561/561`**; `DIFFICULTY found=NORMAL set=NORMAL` (or `found=` whatever the world holds); `SELF … at=104.50 201.00 -3.50` | the pad | |

### SR1 — THE REACH, NO ENCHANT: 17 OUT TAKES THE CENTRE CHUNK (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `"Breacher -- …"`'s show with no active enchant. **Exactly one PLAYERHIT `source=breacher/right_click target=<F> zombie element=kinetic sent=40.000`**, sum `(40 x 100/100) x 1.0 + 0 + 0 = 40`: **the centre chunk only** (the up chunk passes 0.545 over its head, the lower pair 0.458 outside its sides, EXECUTED). No `SPLIT`. Probe F (reach 19) `960.000` (or `920.000`), d 17.000 | Ben's 18; `BreacherContentTest.theReachIsEighteenBlocks` | |

### SR2 — THE REACH, NO ENCHANT: 19 OUT TAKES NONE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **No PLAYERHIT**: every chunk stops 18.000 from his eye, 0.7 short of F's face. Probe F (reach 21) **`1000.000`**, d 19.000 | Ben's 18 | |

### SC1 — SHATTER CAPS I, A HIT BEFORE 5: THE STONE'S 35, NO SPLIT, THE QUIVER 2 THEN 1 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `"  Slot 0: [*shatter_caps I]"` and **`"  active: shatter_caps I (fires one stone of 35 damage that shatters after 5 blocks into the usual chunks)"`**. The first `HELD breacher …` line's lore holds **`Quiver: 2/2`**. **Exactly one PLAYERHIT `source=breacher/right_click target=<N> zombie element=kinetic sent=35.000`**, sum `(35 x 100/100) x 1.0 + 0 + 0 = 35`; **no `sent=40.000`; no `SPLIT`.** The second `HELD` line holds **`Quiver: 1/2`** (one round for the one stone). Probe N `965.000` (or `930.000`), d 3.000 | Ben's 35; `ShatterCapsTest.aMobBeforeFiveTakesTheStoneAtEachLevelAndNothingSplits` | |

### SC3 — SHATTER CAPS III, THE SAME PLACING: 85 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `"  active: shatter_caps III (fires one stone of 85 damage …)"`; `Quiver: 2/2` then `Quiver: 1/2`; **exactly one PLAYERHIT `sent=85.000`** (`(85 x 100/100) x 1.0 + 0 + 0`), no 40, no SPLIT. Probe N `915.000` (or `830.000`) | Ben's 85 | |

### SC2 — THE SPLIT: AT EXACTLY 5.000, FOUR CHUNKS ON A ZOMBIE 8 OUT (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Exactly one `SPLIT caster=<him> source=breacher/right_click at=<X+5.000>,<Y+1.620>,<Z> bursts=4 tick=<t>`**, that is `at=109.500,202.620,-3.500`. **No stone hit** (no `sent=35.000`). **PLAYERHITs on M, each `sent=40.000`** (`(40 x 100/100) x 1.0 + 0 + 0`); **how many is a READING; the geometry predicts all FOUR**, the thinnest margins 0.1885 (the up chunk under M's head) and 0.1775 (the lower pair inside its sides), EXECUTED. Probe M `840.000` at four (or as many 40s as land), d 8.000 | Ben's "the usual 40", "the same pattern"; `ShatterCapsTest.theStoneSplitsOnItsSecondStepAtExactlyFive` | |

### SF1 — THE FAR REACH: 22 OUT (5 + 17) TAKES THE CENTRE CHUNK (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One `SPLIT … at=109.500,202.620,-3.500 bursts=4`; **exactly one PLAYERHIT on F `sent=40.000`**: the centre chunk only (16.7 past the split, the margins as SR1's). Probe F (reach 24) `960.000`, d 22.000 | Ben's "their full reach"; `ShatterCapsTest.theFourLeaveWithTheWeaponsSpreadSpeedDamageAndReach` | |

### SF2 — THE FAR REACH: 24 OUT TAKES NONE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One `SPLIT … bursts=4`; **no PLAYERHIT**: the chunks stop 23.000 from his eye, 0.7 short of F's face. Probe F (reach 26) **`1000.000`**, d 24.000 | the same | |

### ST1 — WITH KINETIC TREMORS I: THE SPLIT'S FOUR CHUNKS FIRE EXACTLY ONE SHOCKWAVE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `"  Slot 0: [*shatter_caps I]"`, `"  Slot 1: [*kinetic_tremors I]"`. One `SPLIT`; four `sent=40.000` on M (as SC2); **exactly one PLAYERHIT `source=kinetic_tremors/shockwave target=<M> … element=kinetic sent=10.000`** (`(10 x 100/100) x 1.0 + 0 + 0`, x the third hit's crit), on the third chunk's tick: three hits on M within 60 ticks, and its 5-tick cooldown allows one. Probe M `830.000` (`1000 - 4 x 40 - 10`, no crit) | Kinetic Tremors' rule; `ShatterCapsTest.withKineticTremorsTheChunksFireOneShockwaveAndTheStoneAloneNone` | |

### ST2 — WITH KINETIC TREMORS I: THE STONE ALONE FIRES NONE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Exactly one PLAYERHIT `sent=35.000`** on N (the stone, one hit), **no `kinetic_tremors/shockwave`**, no SPLIT. Probe N `965.000`, d 3.000 | the same | |

## UNREAD BY DESIGN

- **The stone's look** (a stone item, the Breacher's trail) and the four chunks' pattern **as seen:** client-side.
- **A stone meeting a block before 5:** the pad has no block on his aim line; READ above, and `ProjectileFlight`'s own
  tests cover a block hit.
- **The weapon check:** the dev command could stage Shatter Caps on another weapon, but no row does; `ShatterCapsTest`
  reads it.

## R0b, RUN BEFORE COMMIT

**THE JAR:** `paper/target/rpg-0.1.0-SNAPSHOT.jar`, 2,514,587 bytes, sha256 (of the jar file)
`13fac69c6b9b809c369bc5bccebef6733190ff3c1bc02d23c3faa6f64527aa67`, written 2026-11-24 01:06:53 by `./mvnw -B clean verify`
(exit 0; core 2448, storage 99, paper 1927, 0 failures, 1 skipped) on the working tree of `feat/shatter-caps` at
`4862cfce` (PR 1's head) with PR 2's code and this gate's files uncommitted: **the same bytes as PR 2's commits**, no
source edited between that build and them (only this paragraph, a doc the build does not read). It is not the deployed
jar.

**THE INSTRUMENT:** this file's `powershell` block, extracted (`tr -d '\r' | awk`, 51 lines; 28 `ValueLine` or `Has`
lines before the controls line, 7 after), run with `powershell -NoProfile -ExecutionPolicy Bypass -File … -JarPath <the
jar>`, exit 0.

**READING: AS PREDICTED.** Before `'--- controls'`: **28 `PRESENT`, 0 `ABSENT`**. After it: **7 `ABSENT`, 0 `PRESENT`**.
