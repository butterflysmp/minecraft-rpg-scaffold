# GATE — THE OWED WEAPON FILTER, AND FOCUSED BEAM

**Status: NOT RUN.** Written 2026-11-24 on `feat/focused-beam` (PR 2: Focused Beam), stacked on
`feat/behaviour-weapon-filter` (PR 1, #271: the weapon filter), on `docs/shatter-caps-owed` (#270, plan notes only), on
master `ee46a493`. **THIS FILE CARRIES THE ONE R0 FOR #270 AND BOTH PRs AND THE ONE RUN SHEET.** Every prediction below
was written before any boot of this code. **NO BOOT until the seat has diffed both PRs and this gate from origin and Ben
has said "ready" in his own words.** SURVIVAL, every row. Scenario: `paper/src/main/resources/selftest/GATE-focused-beam.yml`,
`/rpg selftest focused-beam`. ALL AUTO; no ASK.

## BEN'S WORDS THIS GATE SERVES, VERBATIM (2026-11-24), RECORDED IN `PLAN-enchant-columns.md` SECTION 18

- "I'd like to scope the next enchantment for the Cursed Emerald: Focused Beam. Fires one stronger beam after the wind
  up." — FB1, FB2, FB3.
- Asked the beam's damage by level, whether it pierces, whether the wind-up, mana, cooldown or range change, and whether
  it fires where he is looking when the wind-up ends: "1. 108/162/216 2. No piece 3. Everything the same except increase
  the range to 48 blocks. 4. Yes" — FB1-FB3 (the damages, the mana, the 20 ticks), FBP (no pierce), FBR and FBR0 (the
  48, and the plain volley's 32 standing: **the 48's scope is THE SEAT'S READING, OPEN FOR BEN**). His "4. Yes" (the aim
  at the wind-up's end) is held by `FocusedBeamTest.theAimIsReadAtTheWindupsEndNotAtThePress`; no row turns him during a
  wind-up (see *UNREAD BY DESIGN*).
- The owed weapon filter (the seat's ruling, `PLAN-enchant-columns.md` section 16) — AFR, CSS.

## WHAT WAS READ OR EXECUTED, EACH MARKED

- **`cursed_emerald.yml` at `ee46a493` (READ, as the seat read it; `FocusedBeamContentTest
  .theCursedEmeraldsVolleyIsAsTheSeatReadIt` pins it off the loaded weapon):** the right click "Cursed Volley",
  `cooldown_ticks` 30, mana 40, on_cast `emerald_windup`, cast `volley` with `windup_ticks` 20, `shots` 6,
  `interval_ticks` 2, of a `ray` of `range` 32, `beam` `emerald_beam`, no pierce, no tolerance; on_hit the visual
  `emerald_impact` and `damage` 27 kinetic. **Nothing disagreed with the brief.**
- **The mechanism: A FOURTH BEHAVIOUR TYPE, `EnchantBehaviour.FocusedBeam`, NOT A SEAM SHARED WITH SHATTER CAPS.** Shatter
  acts where a spread projectile is expanded (`CastExecutor.launch`); Focused Beam acts where a volley fires its first
  shot (`CastExecutor.volley`, then `focusedBeam`). `launch`, `launchOne`, `launchStone`, `shatter`, `launchToSplit` and
  `split` are not edited, so Shatter Caps and Airburst are byte-for-byte as `GATE-shatter-caps` and
  `GATE-enchant-changes` read them (`ShatterCapsTest`, `AirburstTest`, `AirburstSeekTest` unchanged and green).
- **THE AIM (READ, `CastExecutor.beginVolley`, `volley`; `PaperCombatWorld.aimOf`):** the plain volley's FIRST shot is
  scheduled on HIM (`scheduleOn`, the entity scheduler) `windup_ticks` after the press, and reads his aim THEN:
  `world.aimOf(casterId)`, which is `ViewAim.of(living.getEyeLocation())`, his eye and facing on that tick (an unlocked
  volley; the Cursed Emerald does not lock). **The Focused Beam is fired from inside that same first shot, off the same
  `live` aim, the same freshly built Caster (a new crit roll, the gear score read then) and the same behaviours read then.**
  So it is the plain volley's own read, not a second one.
- **WHAT INTERRUPTS A WIND-UP TODAY (READ), and the beam obeys the same, because it is fired from the same first shot:**
  - **His quit:** the shot is `PaperScheduler.onEntityLater` = `entity.getScheduler().runDelayed(plugin, …, null, 20)`
    with a null retired callback (READ, our source; the method's comment: "if the entity is gone before the delay, do
    nothing"). That Paper retires an entity's scheduled tasks when the entity is removed is the API's documented
    contract, **INFERRED, not read in the server jar**. And `volley` itself returns if `world.combatant(casterId)` is empty
    (READ). **No ray leaves.**
  - **His death:** `volley` returns if `!self.state().alive()`, which is `!entity.isDead()` (READ, `BukkitCombatant`). **No
    ray leaves if he is dead on the shot's tick.** Whether a death and a respawn BOTH inside the 20 ticks could leave him
    alive on that tick, and whether the entity scheduler survives a respawn, is **UNVERIFIED** (not read in the jar).
  - **A weapon swap: NOT an interrupt today.** Nothing in `volley` reads the held item except the per-shot re-read (the
    seat's ruling 1). A plain volley swapped away from keeps firing all six. **With Focused Beam:** the first shot reads
    the behaviours of whatever he holds THEN; if that is not a Cursed Emerald with Focused Beam active, the six bolts fire
    as authored (the 40 mana is already spent either way). **FLAGGED FOR THE SEAT** (the brief's "the Cursed Emerald that
    made the cast" and ruling 1's "the weapon in hand at each shot" disagree here; built to ruling 1). And the
    ability-id check means a Cursed Emerald swapped into ANOTHER weapon's volley wind-up (`knell_beam`'s, a volley of
    rays) never turns it into this beam (`FocusedBeamTest.notActiveOrOnAnotherWeaponsVolleySixAsToday`, MUTWEAPON).
- **The one ray (READ, `CastExecutor.focusedBeam`):** the volley's own inner `CastSpec.Ray`, read from `spec.of()` at run
  time: its beam visual (`emerald_beam`) and tolerance as authored, its range replaced by 48, pierce OFF. Walked by
  `launchRay` and `stepRay` exactly as a bolt is (drawn to what it struck or to its end; on a miss its on_hit goes off at
  the end, as a bolt's does). Its on_hit is the weapon's own with the Damage replaced by the level's amount, kinetic, on a
  derived ability that keeps the id `cursed_emerald/right_click`, so the PLAYERHIT's `source` is the weapon's.
- **The trace (READ, new):** `FOCUSED caster=<uuid> source=<ability> from=<x>,<y>,<z> range=<r> pressed=<t0> tick=<t1>`,
  logged while `/rpg mobtrace` is on, on the beam's tick (`PaperCombatWorld.focusedLine`, pinned by `FocusedLineTest`).
  **PLAYERHIT carries no tick** (`GATE-enchant-changes.md`'s readings say so), so this line is the one that carries the
  press's tick and the beam's. `pressed` is `Bukkit.getCurrentTick()` read in `beginVolley` on the press's own call.
- **The hit's tick against the beam's (READ, `stepRay`, `ChunkTraversal`):** a ray walks one chunk column per tick, the
  first inline. His eye stands at x 104.5, the chunk border at x 112 (112 = 7 x 16), so a mob 10 out (x 114.5) is met on
  the SECOND segment, **one tick after the `FOCUSED` line's tick**. The PLAYERHIT is therefore logged after the `FOCUSED`
  line, and the hit is at `t1 + 1`: at least 20 after the press. That `+1` is INFERRED from the walk; no line prints it.
- **The geometry on the server (READ: `PaperCombatWorld.RAY_SIZE` 0.0, a zero-width ray against each mob's exact box; a
  zombie `sized(0.6f, 1.95f)`):** his eye is 1.62 over his feet, inside a zombie's 1.95, at pitch 0 straight along z
  -3.5, the zombies' centre line. **A zombie `d` out has its front face `d - 0.3` from his eye:** 10 out at 9.7; 14 out at
  13.7 (behind the first); 40 out at **39.7, which is 8.3 inside 48 and 7.7 beyond 32**. The 48-block end itself is x
  152.5, past the pad's interior (it ends 45 east of the stand point, x 149.5).
- **The damage sums (`.claude/rules/verification.md`, *A DAMAGE PREDICTION NAMES EVERY TERM*):** `sent = ((amount x
  score/100) x enchantMultiplier + classDamageBonus + weaponLevelDamage) x chargeScale x crit`, staged: score 100
  (`rpg gearscore set 100`), enchantMultiplier 1.0 (no damage enchant active: every row runs `rpg enchant clear` and
  stages only the enchants it names), classDamageBonus 0 (no class gear), weaponLevelDamage 0 at
  level 1 (`rpg playerxp set @s 1 levels`; `GATE-shatter-caps.md` SR1 read `sent=40.000` on the same staging),
  chargeScale 1.0 (an ability), crit x 1 or x 2 (the second the allowed reading, drawn per shot, so per bolt in FB0).
  **A bolt: `(27 x 100/100) x 1.0 + 0 + 0 = 27`. The beam: `(108 x 100/100) x 1.0 + 0 + 0 = 108` at I, `162` at II,
  `216` at III** (Ben: the gear score term scales it). **A lantern burst (II): `(30 x 100/100) x 1.0 + 0 + 0 = 30`. The
  Crystal Shard Staff's shot: `(16 x 100/100) x 1.0 + 0 + 0 = 16`.**
- **CSS's counterfactual (EXECUTED: `ticksToTravel(6, 3.0)` = `max(1, ceil(6 / 3.0 - 1e-9))` = 2):** without the filter,
  Airburst staged on the staff would cut its 3.0-a-tick shot at its 2nd step, 6.0 blocks out, inside its 10-tick
  lifetime, BEFORE the zombie 10 out (face 9.7), and log a `SPLIT`. With it, the staff's own shot reaches the zombie on
  its 4th step (9 to 12 crosses 9.7). So the row tells the two apart.

## SAFETY: THE MOST HITS THE RUN CAN LAND ON HIM, TIMES THE BIGGEST, AGAINST HIS HEALTH

| source | can it reach him | hits x biggest |
|---|---|---|
| the beam, the bolts, the lantern's shot and bursts, the staff's shot | a ray and a projectile ignore their caster; all leave east, away from him | 0 x 0 |
| the shockwave (FBT) | never himself, never a player (`BehaviourHits`); and none fires | 0 x 0 |
| the zombies | NoAI, frozen | 0 x 0 |
| the natural hostiles | killed by the setup; spawning and phantoms off | 0 x 0 |
| the fall | the pad (`SelfTestPadShapeTest`) | 0 |

**Sum: 0.** **Where each row puts him:** every row's clean returns him to the pad's stand point (`104.50 201.00 -3.50` on
`8ed6e7ca`), and every row turns him there to face due east and waits 40 before its one press. He is never elsewhere.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup** of `run/plugins/Rpg` and `run/world` in a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-focused-beam` (first free `-2`, `-3`); counts, `diff -rq lines: 0` for each, the
   overworld's file count.
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `run/world/dimensions/rpgfight` holds nothing.
5. **The tree clean and pushed:** `git status --porcelain` prints nothing; `git rev-parse HEAD` equals the
   `git ls-remote --heads origin feat/focused-beam` line.
6. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND:** "booting now", and Ben told NOT to log in until told R0 passed.
7. A marker made, then `./scripts/dev-server.sh --refresh-content`. **Required deployed lines:** `content/enchants/` holds
   **16** `.yml` files, none `dev_`, among them `focused_beam.yml`; `content/weapons/cursed_emerald.yml` holds
   `        range: 32`.
8. R0a to R0e on **this boot's** log, its `Done` matched to a log newer than a deploy newer than the marker.
9. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
10. *(After his `/stop`.)* `rpgfight` empty; the overworld's file count against step 2's; `All dimensions are saved`; no
    `java` process left; his saved position not the pad.

## THE RUN SHEET — BEN TYPES THESE THREE LINES

**IN PLAIN WORDS:** **stand anywhere in the dev world.** The selftest lifts you onto its platform in the sky, runs, and
puts you back where you were. Your inventory is emptied during the run and given back at the end. Do nothing while it
runs: the Cursed Emerald fires eight times by itself (sometimes six green bolts, sometimes one beam), then the Living
Lantern once and the Crystal Shard Staff once.

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.**
1. `/gamemode survival`
2. `/rpg selftest focused-beam` — **ONCE.** About **61 seconds**.
3. `/stop`

**This run's length:** the scenario's waits are **1162 ticks** (recount: `tr -d '\r' < <the scenario> | awk '/- wait:
/{t+=$3} END{print t}'`), and about 5 per row for the cleans (10 rows: 50): **about 1212 ticks, 61 s**, plus the pad's
check.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

**#270 is plan notes only: nothing of it is in the jar**, so R0 reads PR 1 and PR 2.

### R0a — the build line names the stack top's head, clean

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `<sha>` being `feat/focused-beam`'s head as `git ls-remote --heads origin feat/focused-beam` prints it, shortened. **Not `-dirty`, not `unknown`, not `8ed6e7ca`.** | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries both PRs and this gate

Counted: `awk '/^.--- controls.$/{exit} /^(ValueLine\|Has) /' <the block> | wc -l` prints **29**, and **7** after the
controls line.

```powershell
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
$y = Bytes 'content/enchants/focused_beam.yml'
$c = Bytes 'content/weapons/cursed_emerald.yml'
$g = Bytes 'selftest/GATE-focused-beam.yml'
$i = Bytes 'selftest/index.yml'
$bi = Bytes 'io/github/butterflysmp/rpg/paper/weapon/BehaviourItems.class'
$ce = Bytes 'io/github/butterflysmp/rpg/core/ability/CastExecutor.class'
$fb = Bytes 'io/github/butterflysmp/rpg/core/enchant/EnchantBehaviour$FocusedBeam.class'
$el = Bytes 'io/github/butterflysmp/rpg/paper/content/EnchantLoader.class'
$ln = Bytes 'io/github/butterflysmp/rpg/paper/weapon/EnchantEffectLine.class'
$pw = Bytes 'io/github/butterflysmp/rpg/paper/adapter/PaperCombatWorld.class'
ValueLine $y 'effect: focused_beam' 'focused_beam.yml'
ValueLine $y 'weapon: cursed_emerald' 'focused_beam.yml'
ValueLine $y 'focused_beam:' 'focused_beam.yml'
ValueLine $y '  range: 48' 'focused_beam.yml'
ValueLine $y '  damage_by_level: [108, 162, 216]' 'focused_beam.yml'
ValueLine $y '  element: kinetic' 'focused_beam.yml'
ValueLine $c '      windup_ticks: 20' 'cursed_emerald.yml'
ValueLine $c '        range: 32' 'cursed_emerald.yml'
ValueLine $g 'setup:' 'GATE-focused-beam.yml'
ValueLine $g 'FB0:' 'GATE-focused-beam.yml'
ValueLine $g 'FB1:' 'GATE-focused-beam.yml'
ValueLine $g 'FB2:' 'GATE-focused-beam.yml'
ValueLine $g 'FB3:' 'GATE-focused-beam.yml'
ValueLine $g 'FBP:' 'GATE-focused-beam.yml'
ValueLine $g 'FBR:' 'GATE-focused-beam.yml'
ValueLine $g 'FBR0:' 'GATE-focused-beam.yml'
ValueLine $g 'FBT:' 'GATE-focused-beam.yml'
ValueLine $g 'AFR:' 'GATE-focused-beam.yml'
ValueLine $g 'CSS:' 'GATE-focused-beam.yml'
ValueLine $g '  - preflight: "0 -6 42 10"' 'GATE-focused-beam.yml'
ValueLine $i '  - focused-beam' 'index.yml'
ValueLine $i '  - shatter-caps' 'index.yml'
Has $bi '(Lio/github/butterflysmp/rpg/core/enchant/EnchantState;Ljava/lang/String;Ljava/util/function/Function;)Ljava/util/List;' 'BehaviourItems.class'
Has $ce 'focusedBeam' 'CastExecutor.class'
Has $ce 'traceFocusedBeam' 'CastExecutor.class'
Has $fb 'focused_beam range must be above 0' 'EnchantBehaviour$FocusedBeam.class'
Has $el 'is not a volley of rays, so there is nothing to focus' 'EnchantLoader.class'
Has $ln 'after the wind-up, fires one beam of ' 'EnchantEffectLine.class'
Has $pw 'FOCUSED caster=%s source=%s' 'PaperCombatWorld.class'
'--- controls'
Has $bi '(Lio/github/butterflysmp/rpg/core/enchant/EnchantState;Ljava/util/function/Function;)Ljava/util/List;' 'BehaviourItems.class CONTROL-THE-OLD-FORM'
ValueLine $y '  damage_by_level: [108, 162, 21]' 'focused_beam.yml CONTROL-PREFIX'
ValueLine $y '  range: 32' 'focused_beam.yml CONTROL-NONE'
ValueLine $g 'FB4:' 'GATE-focused-beam.yml CONTROL-NONE'
ValueLine $i '  - focused-bea' 'index.yml CONTROL-PREFIX'
Has $ce 'focusedBeams' 'CastExecutor.class CONTROL-NONE'
Has $fb 'shatter travel_blocks' 'EnchantBehaviour$FocusedBeam.class CONTROL-WRONG-CLASS'
$zip.Dispose()
```

**The prediction, line for line:** before `'--- controls'`, **29 `PRESENT`, 0 `ABSENT`**; after it, **7 `ABSENT`, 0
`PRESENT`**. **The first control is the old two-argument `activeOf`: ABSENT means PR 1's removal of it reached the jar.**
Run before the commit: see *R0b, RUN BEFORE COMMIT* at the end.

### R0c — every Loaded line, and nothing refused

`[Rpg] Loaded 11 abilities, 46 visuals, 5 statuses, 7 elements, 16 enchants, …` (**16 enchants: `focused_beam` added**;
every other count as on `8ed6e7ca`), the candidates, `Mint-on-craft` and `Custom recipes` lines as there. **No `[Rpg]`
WARN beyond `volley_stone`'s known pair**, in particular none naming `focused_beam` or `cursed_emerald`. No `Refusing`,
`Skipping`, `SEVERE`, `Exception`, `Dev-only`, `currently prevented`.

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 375 scenarios across 46 gates [melee-cell, …, shatter-caps, focused-beam]`** (recount: 46 files, 421 keys, 46 setups; `SelfTestScenariosTest`). The controls: `365 scenarios` and `376 scenarios` ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

### R0e — the orphan sweep with nothing staged

One `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`.

## THE ROWS

Every row: SURVIVAL, level 1, score 100, a fresh weapon, a cleared cooldown (`clear_state`), mana refilled, the zombies at
1000 health, night. **Every row-start clean: `LINGER work=0 entities=0 radius=48`.** `<X, Y, Z>` is `PROBE self`'s
position; his eye is `<Y+1.620>`. A PLAYERHIT's `crit=true` with its sent doubled is the allowed second reading
throughout. **Every FOCUSED line predicted below reads `from=104.500,202.620,-3.500 range=48.000`** and **`tick` minus
`pressed` = 20** (the brief's reading: at least 20; the wind-up is scheduled exactly 20 on him).

### setup — the pad, the ground, normal, night, no spawns, the trace on (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `PAD world=world stand=104,201,-4 … placed=0 repaired=0 ok=7250`; `PAD stood at=104.50 201.00 -3.50 yaw=-90.000 pitch=0.000`; **`PREFLIGHT ok box=0..42 east -6..10 south air=1462/1462 floor solid=731/731`** (43 x 17 = 731 cells, two of air over each: EXECUTED arithmetic, on `GATE-shatter-caps`' read 33 x 17 = 561 / 1122 for 0..32); `DIFFICULTY found=NORMAL set=NORMAL` (or `found=` whatever the world holds); `SELF … at=104.50 201.00 -3.50` | the pad | |

### FB0 — THE CONTROL, NO ENCHANT: SIX BOLTS OF 27 ON A ZOMBIE 10 OUT; 40 MANA ONCE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `"Cursed Emerald -- …"`'s show with no active enchant. **`MANA current=100.000 max=100.000`, then (the press) `MANA current=60.000 max=100.000`**, then after 60 ticks `MANA current=<60 + 60 ticks' regeneration, recorded>`, **never 20.000 or below** (one charge). **Exactly SIX PLAYERHITs `source=cursed_emerald/right_click target=<M> zombie element=kinetic sent=27.000`** (each its own crit: `54.000 crit=true` allowed per bolt), sum `(27 x 100/100) x 1.0 + 0 + 0 = 27`. **No `FOCUSED` line.** Probe M **`838.000`** (`1000 - 6 x 27`; 27 more per crit, recorded), d 10.000 | the volley as authored; `FocusedBeamTest.notActiveOrOnAnotherWeaponsVolleySixAsToday` | |

### FB1 — FOCUSED BEAM I: ONE HIT OF 108 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`"  Slot 0: [*focused_beam I]"`** and **`"  active: focused_beam I (after the wind-up, fires one beam of 108 damage reaching 48 blocks)"`**. **`MANA current=100.000`, then `MANA current=60.000`**, then after 80 ticks `<60 + regeneration, recorded>`, never 20.000 or below. **Exactly one `FOCUSED caster=<him> source=cursed_emerald/right_click from=104.500,202.620,-3.500 range=48.000 pressed=<t0> tick=<t0+20>`.** **Exactly ONE PLAYERHIT `source=cursed_emerald/right_click target=<M> zombie element=kinetic sent=108.000`** (or `216.000 crit=true`), sum `(108 x 100/100) x 1.0 + 0 + 0 = 108`, logged after the `FOCUSED` line; **no `sent=27.000`, and no further PLAYERHIT in the row's remaining 60 ticks.** Probe M **`892.000`** (or `784.000`), d 10.000 | Ben's 108, "everything the same"; `FocusedBeamTest.whileActiveOnePressFiresOneRayNotSix`, `theRayLeavesAtTheWindupsEndAndNotBefore`, `manaAndCooldownAreChargedOnce` | |

### FB2 — FOCUSED BEAM II: 162 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `"  active: focused_beam II (… one beam of 162 damage reaching 48 blocks)"`; `MANA` 100.000 then 60.000; one `FOCUSED … pressed=<t0> tick=<t0+20>`; **exactly one PLAYERHIT `sent=162.000`** (or `324.000 crit=true`), `(162 x 100/100) x 1.0 + 0 + 0`; no 27. Probe M **`838.000`** (or `676.000`) | Ben's 162; `FocusedBeamTest.eachLevelsDamage` | |

> **FB2's probe equals FB0's (`838.000`), stated so nobody reads it as a transposition:** six 27s and one 162 are both
> 162. The rows are told apart by their PLAYERHIT counts (six against one) and the `FOCUSED` line, not by the probe.

### FB3 — FOCUSED BEAM III: 216 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `"  active: focused_beam III (… one beam of 216 damage reaching 48 blocks)"`; `MANA` 100.000 then 60.000; one `FOCUSED … pressed=<t0> tick=<t0+20>`; **exactly one PLAYERHIT `sent=216.000`** (or `432.000 crit=true`), `(216 x 100/100) x 1.0 + 0 + 0`; no 27. Probe M **`784.000`** (or `568.000`) | Ben's 216 | |

### FBP — NO PIERCE: ZOMBIES 10 AND 14 OUT IN ONE LINE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One `FOCUSED … tick=<t0+20>`. **Exactly one PLAYERHIT, on N `sent=108.000`** (or 216 on a crit); **none on F.** Probe N **`892.000`** (or `784.000`), d 10.000; probe F (reach 16) **`1000.000`**, d 14.000 | Ben's "No piece" (the seat's reading: no pierce); `FocusedBeamTest.itStopsAtTheFirstOfTwoMobsInLine` | |

### FBR — THE REACH: A ZOMBIE 40 OUT TAKES THE BEAM (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One `FOCUSED … range=48.000 …`. **Exactly one PLAYERHIT on F `sent=108.000`** (or 216): its face at 39.7, 8.3 inside 48. Probe F (reach 42) **`892.000`**, d 40.000 | Ben's 48; `FocusedBeamTest.itsRangeIsFortyEightAtTheBoundaryBothWays` | |

### FBR0 — THE REACH'S CONTROL: THE SAME ZOMBIE TAKES NOTHING FROM THE PLAIN VOLLEY (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| No active enchant; **no `FOCUSED` line; no PLAYERHIT**: every bolt ends 32 from his eye, 7.7 short of F's face. Probe F (reach 42) **`1000.000`**, d 40.000 | the volley's 32 stands (the seat's reading of Ben's 3) | |

### FBT — WITH KINETIC TREMORS I: THE BEAM'S ONE HIT FIRES NO SHOCKWAVE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `"  Slot 0: [*focused_beam I]"`, `"  Slot 1: [*kinetic_tremors I]"`, both active lines. One `FOCUSED`; **exactly one PLAYERHIT `sent=108.000`** on M; **no `kinetic_tremors/shockwave`** (one hit of the three Tremors needs). Probe M **`892.000`** (or `784.000`), d 10.000 | Kinetic Tremors' rule; `FocusedBeamTest.withKineticTremorsOneHitFiresNoShockwave` | |

### AFR — PR 1 ON THE SERVER: AIRBURST FLAMES ON THE LIVING LANTERN, AS `GATE-enchant-changes` AF2 READ IT (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| AF2's staging, unchanged (H 16 east and 4 south, a probe of 18). `"  Slot 0: [*airburst_flames II]"`, `"  active: airburst_flames II (splits after 6 blocks into 3 bursts of 30 damage)"`. **Exactly one `SPLIT caster=<him> source=living_lantern/right_click at=110.800,202.620,-3.500 bursts=3 tick=<t>`** (the 18th step, 6.3 east of his eye: `GATE-enchant-changes`' own prediction and reading). **At least one PLAYERHIT `source=living_lantern/right_click target=<H> zombie element=fire sent=30.000`** (or `60.000 crit=true`); **how many (1, 2 or 3) is a READING** (AF2 read three); each `(30 x 100/100) x 1.0 + 0 + 0 = 30`. **No `sent=45.000`.** Probe H `1000 - the bursts - their burns` (recorded), d 16.492 | Ben's Airburst on its own weapon, unchanged by the filter; `BehaviourWeaponFilterTest.airburstActsOnTheLivingLanternAndOnNoOtherProjectileWeapon` | |

### CSS — PR 1 ON THE SERVER: AIRBURST FLAMES STAGED ON THE CRYSTAL SHARD STAFF DOES NOTHING THERE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `"  Slot 0: [*airburst_flames II]"` and its active line (the dev command stages it, as the seat's ruling 4 allows; it reads as active in `show`, which reads the enchant state, not the behaviour). **NO `SPLIT` line in the row.** **Exactly one PLAYERHIT `source=crystal_shard_staff/right_click target=<M> zombie element=kinetic sent=16.000`** (or `32.000 crit=true`), `(16 x 100/100) x 1.0 + 0 + 0 = 16`: the staff's own shot. **No `sent=30.000`, no `element=fire`.** Probe M **`984.000`** (or `968.000`), d 10.000 | the weapon filter (the seat's ruling, section 16); `BehaviourWeaponFilterTest`; MUTFILTER | |

## UNREAD BY DESIGN

- **The 48-block end ITSELF** (a zombie just inside and just outside 48): the pad's interior ends 45 east of the stand
  point, so no zombie can stand there, and the brief rules the pad is not enlarged. **Held by
  `FocusedBeamTest.itsRangeIsFortyEightAtTheBoundaryBothWays`** (47.9 struck, 48.1 not, and the beam drawn to the end on
  a miss). FBR reads that the reach exceeds 32 and reaches 40; FBR0 that the plain volley's 32 stands.
- **The aim read at the wind-up's end** (Ben's "4. Yes"): not in the brief's row list, so not staged. **Held by
  `FocusedBeamTest.theAimIsReadAtTheWindupsEndNotAtThePress`** and MUTLOCK, and READ above as the plain volley's own read.
  **A row CAN read it, offered to the seat, not added:** press facing east, `wait: 10`, a `tp` turning him to face a
  zombie standing south, `wait: 30`; the beam then strikes the south zombie and not one standing east. (The turn lands
  inside the wind-up, after the press, so it is not `ScenarioLint`'s TURN_THEN_PRESS shape, which is a press in the same
  pass as its turn.)
- **A weapon swap, a death or a quit during the wind-up:** READ above; no row stages one.
- **The beam's look** (`emerald_beam`, drawn to what it struck: the same look as a bolt, SEAT PLACEHOLDER) and the wind-up
  as heard: client-side; this gate has no ASK.
- **The hit's own tick:** PLAYERHIT carries none; the `FOCUSED` line carries the beam's, and the hit is INFERRED one tick
  later (the chunk walk, above).

## R0b, RUN BEFORE COMMIT

**THE JAR:** `paper/target/rpg-0.1.0-SNAPSHOT.jar`, 2,521,830 bytes, sha256 (of the jar file)
`0ed0ae4556214bfaa7ed68d614fbd03a8bfc752169863b85c3d24584ef416d1c`, written 2026-11-24 13:26:24 by `./mvnw -B clean verify`
(exit 0; core 2458 (1 skipped), storage 99, paper 1938, 0 failures) on the working tree of `feat/focused-beam` at
`1b6efcee` (PR 2's code commit, on PR 1's `5590e000`) with this gate's files, the index line, `SelfTestScenariosTest` and
PLAN section 18 uncommitted: **the same bytes as this gate's commit**, no source edited between that build and it (only
this paragraph, a doc the build does not read). It is not the deployed jar.

**THE INSTRUMENT:** this file's `powershell` block, extracted (`awk '/^```powershell/{f=1;next} /^```/{f=0} f'`, 54
lines; 29 `ValueLine` or `Has` lines before the controls line, 7 after), run with `powershell -NoProfile -ExecutionPolicy
Bypass -File … -JarPath <the jar>`, exit 0.

**READING: AS PREDICTED.** Before `'--- controls'`: **29 `PRESENT`, 0 `ABSENT`**. After it: **7 `ABSENT`, 0 `PRESENT`**.

**THE OLD-FORM CONTROL WAS SHOWN ABLE TO FIRE:** the same needle against `run\plugins\rpg-0.1.0-SNAPSHOT.jar` (the
`GATE-shatter-caps` boot's jar, written 2026-11-24 01:32:15, before PR 1) reads the old two-argument descriptor **True**
and the new one **False**. So its ABSENT above is PR 1's removal, not a needle that cannot match.

## THE RUN OF `3890aaa8`, 2026-11-24 (14:07 boot, 14:09:11-14:10:12 run): R0 PASSED; SIX ROWS MISS ON ONE SHARED CLAUSE

> **DATED NOTE, 2026-11-24, written after the run; no prediction above is edited.** Booted on the seat's approval of
> `3890aaa8` (#270, #271, #272) and Ben's "ready". Before the boot: `git status --porcelain` printed nothing; `HEAD` and
> `git ls-remote --heads origin feat/focused-beam` both `3890aaa87c9c7d2c089e5b0ad3c628dd649d26df`; master `ee46a493…`,
> #270 `6a1c4713…`, #271 `5590e000…` unchanged; push and pull-request CI SUCCESS on all three PR heads, matched by SHA.
> Carried state: no `java` process; backup `..\rpg-run-backups\2026-11-24-pre-focused-beam` (Rpg 170 / 170 files, world
> 193 / 193, `diff -rq` 0 lines each; the overworld 106 files); config `12:  dev-pack-on-join: true`; `rpgfight` 0
> entries. "Booting now" was its own message, before the boot command. A marker made at 14:06:53, then the boot command;
> deployed jar 14:07:17; `latest.log` first line 14:07:18; this boot's `Done` 14:07:23. R0 read 14:07:44; "R0 passed"
> was its own message; **Ben joined 14:09:01, after it**, typed `/gamemode survival` (14:09:07) and `/rpg selftest
> focused-beam` (14:09:11), and `/stop` (14:10:45). Nothing else was typed.

### R0, READ

- **R0a: AS PREDICTED.** `[14:07:22] … [Rpg] Build: 3890aaa8`.
- **R0b: AS PREDICTED.** The block extracted from the committed file, `cmp` IDENTICAL to the copy run before the commit,
  against the deployed jar: **29 `PRESENT`, 0 `ABSENT`**, then **7 `ABSENT`, 0 `PRESENT`**.
- **R0c: AS PREDICTED.** `[Rpg] Loaded 11 abilities, 46 visuals, 5 statuses, 7 elements, 16 enchants, …`;
  `Mint-on-craft: 38 indexed, 38 claiming, of 54 gear definitions`; `Custom recipes: 7 registered, 0 replaced, 0 refused,
  of 7 authored (7 minting gear)`; the candidates line once; the only `[Rpg]` WARN `volley_stone`'s pair; no `Refusing`,
  `Skipping`, `SEVERE`, `Exception`, `Dev-only`, `currently prevented`. Deployed: `content/enchants/` 16 `.yml`, 0 `dev_`,
  among them `focused_beam.yml`; `cursed_emerald.yml` line 182 `        range: 32`.
- **R0d: AS PREDICTED.** Exactly one `[Rpg] Selftest: dev server, 375 scenarios across 46 gates [melee-cell, …,
  shatter-caps, focused-beam]`; `365 scenarios` and `376 scenarios` absent.
- **R0e: AS PREDICTED.** `FIGHTWORLD sweep path=…\rpgfight folders=0`; no `orphan`, no `deleting`.

### THE SETUP AND THE CLEANS, READ

- `PAD world=world stand=104,201,-4 floorY=200 interior=-2..45 east -13..13 south cells=7250 placed=0 repaired=0
  ok=7250`; `PAD stood at=104.50 201.00 -3.50 yaw=-90.000 pitch=0.000`; **`PREFLIGHT ok box=0..42 east -6..10 south
  air=1462/1462 floor solid=731/731`** (as predicted); `DIFFICULTY found=NORMAL set=NORMAL`; `SELF customHP=122.000/350.000
  at=104.50 201.00 -3.50`. The setup's own clean `return=-106.27 63.00 18.98` (where he typed the command).
- **Every clean (setup and the ten rows): `LINGER work=0 entities=0 radius=48` and `LEFTOVER living=0 radius=16`**,
  every row's `return=104.50 201.00 -3.50`; `removed=` 0 at the setup and FB0, 2 at FBR (FBP's two zombies), 1 at every
  other row.
- **STAGE:** every row's `clear_inventory … kept=nexus@8` (`cleared=4` at FB0, `cleared=1` after) and `clear_state
  cooldowns=cleared streaks=forgotten orb=none`. No `SKIPPED`, `VOID`, `ABORT`. `SELFTEST RUN FINISHED` 14:10:12, then
  `RESTORED xp=712819 cell=ranger/fire gamemode=SURVIVAL held=1 mana=100.0 … spawn_mobs=false time=1000 … difficulty=NORMAL
  health=120.000 … spawn_phantoms=true …` and `HEALTH saved=120.000 customHP=120.000/350.000 wrote=120.000`.
- **Across the run (log lines 95-743): 16 `PLAYERHIT` (6 + 1 + 1 + 1 + 1 + 1 + 1 + 3 + 1, as the rows below), 6
  `FOCUSED`, exactly 1 `SPLIT` (AFR; none in CSS), 0 `kinetic_tremors/shockwave`, 0 `orphan`, 0 `backstop`, 0
  `Exception`, 0 `currently prevented`; no `[Rpg]` WARN beyond `volley_stone`'s pair.**
- **Recorded without comment:** `MOBSEED … drowned gs=111 source=stored max=111` at 14:09:13 (FB0), and twenty `MOBREMOVE`
  lines at 14:09:22 (during FB1), none of them a selftest mob's id.

### EVERY `FOCUSED` LINE, WITH `tick - pressed`

| row | `pressed` | `tick` | `tick - pressed` | predicted |
|---|---|---|---|---|
| FB1 | 2340 | 2359 | **19** | 20 |
| FB2 | 2469 | 2488 | **19** | 20 |
| FB3 | 2598 | 2617 | **19** | 20 |
| FBP | 2727 | 2746 | **19** | 20 |
| FBR | 2856 | 2875 | **19** | 20 |
| FBT | 3094 | 3113 | **19** | 20 |

Every one reads `from=104.500,202.620,-3.500 range=48.000`, as predicted.

### THE ROWS, IN THREE CLASSES

**MISS (six rows, ONE shared clause): FB1, FB2, FB3, FBP, FBR, FBT.** Each read **`tick - pressed` = 19** against the
predicted 20 (the rows' header: "`tick` minus `pressed` = 20 (the brief's reading: at least 20 …)"). **Every other clause
of each of the six read as predicted** (below). **The cause is NOT READ.** Two INFERRED candidates, neither checked
in the server jar: (a) Paper's entity scheduler counts its delay down on the entity's own tick, and the press (the
selftest's `use`, a synthetic `PlayerInteractEvent`) runs before his entity tick in the same server tick, so the first
count is spent on the press's own tick; (b) `Bukkit.getCurrentTick()` is read in two different phases of a tick. **The
plain volley's wind-up is scheduled by the same `scheduleOn`, so it is the same 19 on this clock** (FB0 carries no
`FOCUSED` line to show it). **The fake world fires `scheduleOn(…, 20, …)` exactly 20 later** (`FakeWorld.scheduleOn`,
READ), so `FocusedBeamTest.theRayLeavesAtTheWindupsEndAndNotBefore`'s 20 is the fake's, not the server's: a one-tick
divergence between the fake and the server, recorded, not fixed. The hit itself is INFERRED one tick after the `FOCUSED`
tick (the chunk walk), so at `pressed + 20`, but no line prints it: **the brief's "at least 20 apart" for the HIT is
UNREAD.**

- **FB1.** `"  Slot 0: [*focused_beam I]"`, **`"  active: focused_beam I (after the wind-up, fires one beam of 108 damage
  reaching 48 blocks)"`**; **`MANA current=100.000`, `MANA current=60.000`**, then `MANA current=64.000` after 80 ticks
  (never 20 or below); one `FOCUSED … pressed=2340 tick=2359`; **exactly one PLAYERHIT `source=cursed_emerald/right_click
  … element=kinetic sent=216.000 crit=true`** (the allowed second reading of 108), logged after the `FOCUSED` line; no
  27; nothing more in the row. Probe M **`784.000`** (the crit's allowed reading), d 10.000.
- **FB2.** `"  active: focused_beam II (… 162 damage reaching 48 blocks)"`; MANA 100.000, 60.000, 64.000; `FOCUSED …
  pressed=2469 tick=2488`; **exactly one PLAYERHIT `sent=162.000 crit=false`**; no 27. Probe M **`838.000`**, d 10.000.
- **FB3.** `"  active: focused_beam III (… 216 damage reaching 48 blocks)"`; MANA 100.000, 60.000, 64.000; `FOCUSED …
  pressed=2598 tick=2617`; **exactly one PLAYERHIT `sent=432.000 crit=true`** (the allowed second reading of 216); no 27.
  Probe M **`568.000`** (the crit's allowed reading), d 10.000.
- **FBP.** `FOCUSED … pressed=2727 tick=2746`; **exactly one PLAYERHIT, on N, `sent=108.000 crit=false`; none on F.**
  Probe N **`892.000`**, d 10.000; probe F **`1000.000`**, d 14.000.
- **FBR.** `FOCUSED … range=48.000 pressed=2856 tick=2875`; **exactly one PLAYERHIT on F `sent=108.000 crit=false`.**
  Probe F **`892.000`**, d 40.000.
- **FBT.** `"  Slot 0: [*focused_beam I]"`, `"  Slot 1: [*kinetic_tremors I]"`, both active lines; `FOCUSED …
  pressed=3094 tick=3113`; **exactly one PLAYERHIT `sent=108.000 crit=false`; no `kinetic_tremors/shockwave`.** Probe M
  **`892.000`**, d 10.000.

**DID NOT TEST WHAT IT WAS MEANT TO: none.**

**READ AS PREDICTED (four rows): FB0, FBR0, AFR, CSS.**

- **FB0.** `REPLY "This weapon carries no enchant data."`; **`MANA current=100.000`, then `MANA current=60.000`**, then
  `MANA current=63.000` after 60 ticks (never 20 or below); **exactly SIX PLAYERHITs `source=cursed_emerald/right_click …
  element=kinetic sent=27.000 crit=false`**; **no `FOCUSED` line.** Probe M **`838.000`**, d 10.000.
- **FBR0.** No enchant data; **no `FOCUSED` line; no PLAYERHIT.** Probe F **`1000.000`**, d 40.000.
- **AFR.** `"  Slot 0: [*airburst_flames II]"`, `"  active: airburst_flames II (splits after 6 blocks into 3 bursts of 30
  damage)"`; **exactly one `SPLIT caster=<him> source=living_lantern/right_click at=110.800,202.620,-3.500 bursts=3
  tick=3240`**; **THREE PLAYERHITs on H `element=fire sent=30.000 crit=false`** (the reading: three, as AF2 read); no
  `sent=45.000`; two `DOTTICK … status=scorch … sent=15.000 cap=15.000` (recorded). Probe H **`880.000`** (`1000 - 90 -
  30` of burns, recorded; `fireTicks=73`), d 16.492. **The same figures `GATE-enchant-changes.md` AF2 read: the filter
  left Airburst on its own weapon as it was.**
- **CSS.** `"Crystal Shard Staff -- 1 slot(s)"`, `"  Slot 0: [*airburst_flames II]"` and its active line; **NO `SPLIT` line
  in the row; exactly one PLAYERHIT `source=crystal_shard_staff/right_click … element=kinetic sent=16.000 crit=false`; no
  `sent=30.000`, no `element=fire`.** Probe M **`984.000`**, d 10.000. **The weapon filter, read on the server.**

### AFTER HIS `/stop`

`rpgfight` 0 entries; the overworld 106 files (106 at the backup); `All dimensions are saved` 14:10:46; no `java`
process; **his saved position `-103.38 63.00 20.12`** (READ off his player file's `Pos` with a scratchpad NBT reader,
controlled on the backup's file, which reads `-106.27 63.00 18.98`, the setup's `return=`), not the pad.

### UNREAD, AND WHY

- **The cause of the 19** (above): two INFERRED candidates, neither read in the server jar.
- **The hit's own tick:** PLAYERHIT carries none; INFERRED one tick after the `FOCUSED` line.
- **The 48-block end itself:** by design (the pad ends 45 out); `FocusedBeamTest` holds it.
- **The aim read at the wind-up's end (Ben's "4. Yes"): UNREAD ON A SERVER.** Held by
  `FocusedBeamTest.theAimIsReadAtTheWindupsEndNotAtThePress` and MUTLOCK. **By the seat's ruling of 2026-11-24 the row that
  turns him during the wind-up is added to the next gate that touches the Cursed Emerald.**
- **A weapon swap, a death or a quit during the wind-up:** READ in the code (above); no row staged one.
- **The beam's look and the wind-up as heard:** client-side; this gate has no ASK.

> **DATED NOTE, 2026-11-24 (`feat/press-tick`, the seat's merge approval of #270-#272, owed item 2): THE 19 IS
> EXPLAINED, AND THE SIX MISSES STAND AS RECORDED.** `PLAN-enchant-columns.md` section 19 READ it from the pinned jar
> and our source: the selftest's press runs on tick T in the player's entity phase; `WeaponFire`'s `onRegion` hop is the
> global scheduler's `runDelayed(1)`, so the cast (and `beginVolley`'s read of `pressed`) ran at **T + 1**; the
> wind-up's `scheduleOn(…, 20)`, filed from the global phase, ran at **T + 20**. **So `pressed` was the CAST's tick, and
> press to beam was 20.** The predictions were taken from the fake world's clock (press = cast) and are not edited. On
> `feat/press-tick` the press's tick is stamped in `WeaponFire` before the hop and carried in, so a `FOCUSED` line now
> reads `tick - pressed` = 20. **The next gate that touches a wind-up predicts by the server's clock and names the
> tick each read is taken on** (`verification.md`, the line added on the same branch). The two candidates under
> *UNREAD, AND WHY* above are superseded by that READ.
