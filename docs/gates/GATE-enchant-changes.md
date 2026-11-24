# GATE — BEN'S FIVE CHANGES TO AIRBURST FLAMES AND LAPIS CRYSTAL

**Status: NOT RUN.** Written 2026-11-23 on `feat/lapis-crystal-block` (PR 2: the block body), stacked on
`feat/enchant-changes` (PR 1, #264: the seeking bursts, the split at 6, the drawn shot, 30/50/70, the LINGER fix), on
`docs/enchant-columns-owed` (#263), on master `caa95043`. **THIS FILE CARRIES THE ONE R0 FOR BOTH PRs AND THE ONE RUN
SHEET.** Every prediction below was written before any boot of this code. **NO BOOT until the seat has diffed both PRs
and this gate from origin and Ben has said "ready" in his own words.** SURVIVAL, every row. Scenario:
`paper/src/main/resources/selftest/GATE-enchant-changes.yml`, `/rpg selftest enchant-changes`. ALL AUTO, plus ONE ASK.

## BEN'S WORDS THIS GATE SERVES, VERBATIM (2026-11-23), RECORDED IN `docs/plans/PLAN-enchant-columns.md` SECTION 14

1. "I want the Airburst Flames to seek as well." — rows AF2, AF3.
2. "I want it to split into 3 and 6 blocks instead of 12", asked, answered **"B"** (split after 6 blocks into 3 pieces;
   pieces keep their 14) — rows AF1, AF4.
3. "I want to change the visual of the Lapis crystal to a Floating Lapis block with glowing" — rows LB1, LB4, and THE ASK.
4. "Draw a line with particles from the crystal to the target with the same particles the right click uses" — row LB2,
   and THE ASK.
5. "Increase the damage of the crystal to 30/50/70%" — rows LB2, LB3.

## WHAT WAS READ OR EXECUTED, EACH MARKED

- **The split (EXECUTED, jshell):** `ticksToTravel(6, 0.35)` = 18 and `18 x 0.35` = **6.3**; `104.5 + 18 x 0.35` =
  **110.8**. So the shot splits on its 18th step, 6.3 blocks out, and the bursts fly 20 steps of 0.7 (14 blocks).
- **The crystal's damage (EXECUTED):** `21 x 30 / 100` = **6.3**, `21 x 50 / 100` = **10.5**, `21 x 70 / 100` = **14.7**;
  `1000 - 10.5` = **989.5**; `1000 - 6.3 - 14.7` = **979.0**. `21` is the Lapis Beam's authored `amount`
  (`lapis_staff.yml`), read by the shot from the beam itself.
- **The bursts' targets (READ, `ProjectileFlight.steer`, `nearestMob`, `PaperCombatWorld.combatantsNear`):** each burst
  re-chooses, every tick, the nearest live mob it can see within its `search_radius` (7), from the split (`activation
  0`). Nothing locks a target or keeps bursts apart, so **two or three bursts may strike one mob**; a dead target is not
  returned (live bodies only), so a burst turns to the next nearest, or flies straight.
- **The geometry of AF2 and AF3 (EXECUTED):** H stands 16 east and 4 south of his feet. The split point is **10.617**
  from H's feet, and H is **16.492** from his feet (a probe of 18 reaches it; 18 less the lint's margin of 1 is 17). Each
  burst's STRAIGHT line, at H's distance east (9.7 past the split), passes H's centre by: the centre burst **4.000**, the
  right (south) burst **1.938** (`4 - 9.7 x tan 12°`, `tan 12°` = 0.21256), the left **6.062**. **A zombie is 0.6 wide**
  (READ, `EntityType`: `sized(0.6f, 1.95f)`), so **the nearest straight line clears its box by 1.638**: none of the three
  meets H without steering. Each burst flies at eye height (Y+1.62), inside H's 1.95, so the misses are sideways only.
- **The unit test in place of a content control.** The control a gate would want (the same press with the bursts'
  homing removed, reading no hit) needs a content edit and a second boot. In its place:
  `AirburstSeekTest.aBurstSteersToAMobItsStraightLineMisses` (homing on: the burst strikes a mob every straight line
  misses) and `anUnhomingBurstPassesTheSameMob` (homing off, the same geometry: nothing). The pair is the control; AF2's
  hits on H, which no straight line meets, are the server's half.
- **The crystal's line (READ, `CastExecutor.orbShots`):** one line per STRUCK mob, from the orb's rest point to the
  mob's sight point, in the beam of the ray that met the orb (`lapis_staff.yml`: `beam: lapis_beam`), with one `ORBSHOT
  owner=… from=… to=… visual=… tick=…` line under the trace. **The sight point is the feet plus the eye height; a
  zombie's is 1.74** (READ, `EntityType`: `eyeHeight(1.74f)`; `201 + 1.74f` EXECUTED = 202.74000000953674, printed to
  three places as `202.740`).
- **The body (READ; the account is `PaperCombatWorld.spawnOrbBlock`'s javadoc):** a `BlockDisplay` of the content's
  block; centred by a translation of `-scale/2` on each axis (`Transformation.compose` is
  `translation(t).rotate(left).scale(s).rotate(right)`, and a block model spans 0 to 1 from the entity's position);
  `setGlowing(true)` with `setGlowColorOverride` (`CraftDisplay`: `color.asRGB()`); `setTeleportDuration(1)` (the API:
  "move from current position to the updated one over one tick"); `setPersistent(false)` (`saveAsPassenger` returns false
  when `persist` is false, unless its caller forces it: which callers do, UNVERIFIED). **A display cannot be picked**
  (`Entity.isPickable` returns false and `Display` does not override it), **hit by a vanilla projectile**
  (`canBeHitByProjectile` = `isAlive && isPickable`) **or collided with** (`canBeCollidedWith` returns false): READ.
  **Our own rays cannot meet it: READ** (`castRay` and `sweepRay` take a body only through `LiveTargets.resolve`, which
  returns only a live `LivingEntity`).
- **The flight (ruling (a); `OrbBlockBodyTest`):** the display is PLACED where the item would have been driven, once a
  tick; both stand at `origin + (t + 1) x step` after tick `t`'s drive and reach the rest point after tick 9's; the ORB
  rest line is unchanged (tick 10 after the throw).
- **The backstop (ruling (b); `OrbBlockBodyTest`, `OrbBlockBodyLoadTest`):** scheduled at the throw at flight + life +
  100 = **410** ticks, it removes the body by its id and writes `ORB removed … cause=backstop`. Expiry, replacement and
  every paper removal cancel it. **No row here can make it fire** (each needs every other removal skipped): it is
  UNREAD BY DESIGN (below), and what a row CAN read is that it did NOT fire and left nothing scheduled.
- **LINGER (PR 1, item 6):** the row-start clean prints `LINGER work=<n> entities=<m> radius=48` (READ,
  `SelfTestRun`): `work` counts point-scheduled tasks it ended near the stand point, `entities` every projectile, cloud
  and MARKER-TAGGED entity it removed. **The display carries the marker tag**, so a body left behind would be counted
  and named. Every clean in this run is predicted `LINGER work=0 entities=0 radius=48`.

## SAFETY: THE MOST HITS THE RUN CAN LAND ON HIM, TIMES THE BIGGEST, AGAINST HIS HEALTH

| source | can it reach him | hits x biggest |
|---|---|---|
| the lantern's shot and its bursts | the shot never strikes its caster (its flight ignores his id); a burst SEEKS live MOBS only; all fly east, away from him | 0 x 0 |
| the Lapis Beam | a ray from his eye, east, ignoring him | 0 x 0 |
| the crystal's shots | never himself, never a player (B4, `OrbBeamTest`) | 0 x 0 |
| the zombies | NoAI, frozen; no row lets one act | 0 x 0 |
| the natural hostiles | killed by the setup; spawning, phantoms off; difficulty normal does not spawn on a barrier or in a run with spawning off | 0 x 0 |
| the fall | the pad (`SelfTestPadShapeTest`, 51,840 checks); nothing moves him off the stand point | 0 |

**Sum: 0.** **Where each row puts him:** every row's clean returns him to the pad's stand point (on `efa38636`
`104.50 201.00 -3.50`), and every row that fires turns him there to face due east (yaw -90, pitch 0) and waits 40 before
the first press. He is never anywhere else.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup** of `run/plugins/Rpg` and `run/world` in a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-enchant-changes` (first free `-2`, `-3`); counts, `diff -rq lines: 0` for each,
   the `ls` of `world/dimensions`, the overworld's file count.
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **The tree clean and pushed:** `git status --porcelain` prints nothing; `git rev-parse HEAD` equals the
   `git ls-remote --heads origin feat/lapis-crystal-block` line.
6. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND:** "booting now", and Ben told NOT to log in until told R0 passed.
7. `./scripts/dev-server.sh --refresh-content`. **Required deployed lines:** `content/enchants/` holds **14** `.yml`
   files, none `dev_`, among them `airburst_flames.yml` and `lapis_crystal.yml`; `airburst_flames.yml` holds
   `  travel_blocks: 6`; `lapis_crystal.yml` holds `      body: block`.
8. R0a to R0e on **this boot's** log, matched to this boot's own `Done` line.
9. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
10. *(After his `/stop`.)* `rpgfight` empty; the overworld's file count against step 2's; `All dimensions are saved`; no
    `java` process left.

## THE RUN SHEET — BEN TYPES THESE THREE LINES, AND ANSWERS ONE QUESTION

**IN PLAIN WORDS:** **stand anywhere in the dev world.** When you type line 2, the selftest lifts you onto its platform
in the sky, runs, and puts you back where you were. Your inventory is emptied during the run and **given back at the
end**. Do nothing while it runs, **but watch: in the SEVENTH part (about 47 seconds in) you throw a lapis block, then a
blue beam fires into it and it shoots one zombie.**

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.**
1. `/gamemode survival`
2. `/rpg selftest enchant-changes` — **ONCE.** About **62 seconds**: you appear on the platform facing east; the lantern
   fires four times (fire splitting into three), then you throw a floating lapis block four times, and blue beams fire
   into it.
3. `/stop`

**THE ONE QUESTION, answer yes or no after the run:** **"Did a blue beam line run from a glowing lapis block to the
zombie it hit?"**

**This run's length:** the scenario's waits are **1188 ticks** (recount: `tr -d '\r' < <the scenario> | awk '/- wait:
/{t+=$3} END{print t}'`), and about 5 per row for the cleans (8 rows: 40): **about 1228 ticks, 61 s**, plus the pad's
check at the start (a second run on the pad writes nothing).

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head, clean

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `<sha>` being `feat/lapis-crystal-block`'s head as `git ls-remote --heads origin feat/lapis-crystal-block` prints it, shortened as `git rev-parse --short HEAD` prints it. **Not `-dirty`, not `unknown`, not `efa38636`.** | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries both PRs and this gate

Counted: `awk '/^.--- controls.$/{exit} /^(ValueLine\|Has) /' <the block> | wc -l` prints **37**, and **9** after the
controls line.

```powershell
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
$a = Bytes 'content/enchants/airburst_flames.yml'
$lc = Bytes 'content/enchants/lapis_crystal.yml'
$g = Bytes 'selftest/GATE-enchant-changes.yml'
$i = Bytes 'selftest/index.yml'
$ce = Bytes 'io/github/butterflysmp/rpg/core/ability/CastExecutor.class'
$pcw = Bytes 'io/github/butterflysmp/rpg/paper/adapter/PaperCombatWorld.class'
$cv = Bytes 'io/github/butterflysmp/rpg/paper/content/ContentValidator.class'
$as = Bytes 'io/github/butterflysmp/rpg/paper/content/AbilitySchema.class'
$or = Bytes 'io/github/butterflysmp/rpg/paper/weapon/OrbRemovals.class'
ValueLine $a '  travel_blocks: 6' 'airburst_flames.yml'
ValueLine $a '  burst_travel_blocks: 14' 'airburst_flames.yml'
ValueLine $a '  damage_by_level: [25, 30, 35]' 'airburst_flames.yml'
ValueLine $a '  homing:' 'airburst_flames.yml'
ValueLine $a '    lerp: 0.45' 'airburst_flames.yml'
ValueLine $a '    activation_blocks: 0' 'airburst_flames.yml'
ValueLine $a '    search_radius: 7' 'airburst_flames.yml'
ValueLine $lc '        percent_by_level: [30, 50, 70]' 'lapis_crystal.yml'
ValueLine $lc '      body: block' 'lapis_crystal.yml'
ValueLine $lc '      block:' 'lapis_crystal.yml'
ValueLine $lc '        id: lapis_block' 'lapis_crystal.yml'
ValueLine $lc '        scale: 0.5' 'lapis_crystal.yml'
ValueLine $lc '        glow: "#2A5CFF"' 'lapis_crystal.yml'
ValueLine $lc '      life_ticks: 300' 'lapis_crystal.yml'
ValueLine $g 'setup:' 'GATE-enchant-changes.yml'
ValueLine $g 'AF1:' 'GATE-enchant-changes.yml'
ValueLine $g 'AF2:' 'GATE-enchant-changes.yml'
ValueLine $g 'AF3:' 'GATE-enchant-changes.yml'
ValueLine $g 'AF4:' 'GATE-enchant-changes.yml'
ValueLine $g 'LB1:' 'GATE-enchant-changes.yml'
ValueLine $g 'LB2:' 'GATE-enchant-changes.yml'
ValueLine $g 'LB3:' 'GATE-enchant-changes.yml'
ValueLine $g 'LB4:' 'GATE-enchant-changes.yml'
ValueLine $g '  - pad: ""' 'GATE-enchant-changes.yml'
ValueLine $g '  - difficulty: normal' 'GATE-enchant-changes.yml'
ValueLine $i '  - enchant-changes' 'index.yml'
ValueLine $i '  - enchant-columns' 'index.yml'
Has $ce 'backstop' 'CastExecutor.class'
Has $ce 'placeOrbBody' 'CastExecutor.class'
Has $ce 'traceOrbShot' 'CastExecutor.class'
Has $pcw 'ORBBODY owner=%s id=%s type=%s block=%s glowing=%s glow=%s scale=%.3f teleport_duration=%d persistent=%s tick=%d' 'PaperCombatWorld.class'
Has $pcw 'ORBSHOT owner=' 'PaperCombatWorld.class'
Has $pcw 'setTeleportDuration' 'PaperCombatWorld.class'
Has $cv "' is not a block: its throw will spawn no body" 'ContentValidator.class'
Has $cv 'is a ray that names no' 'ContentValidator.class'
Has $as "orb body: block takes no 'item:'" 'AbilitySchema.class'
Has $or 'backstopTask' 'OrbRemovals.class'
'--- controls'
ValueLine $a '  travel_blocks: 14' 'airburst_flames.yml CONTROL-THE-OLD-14'
ValueLine $a '  travel_blocks: 60' 'airburst_flames.yml CONTROL-PREFIX'
ValueLine $lc '        percent_by_level: [20, 30, 40]' 'lapis_crystal.yml CONTROL-THE-OLD-PERCENT'
ValueLine $lc '      item: heart_of_the_sea' 'lapis_crystal.yml CONTROL-THE-OLD-BODY'
ValueLine $g 'LB5:' 'GATE-enchant-changes.yml CONTROL-NONE'
ValueLine $i '  - enchant-change' 'index.yml CONTROL-PREFIX'
Has $pcw 'ORBBODY owner=%s id=%s type=%s block=%s glowing=%s glow=%s scale=%.4f' 'PaperCombatWorld.class CONTROL-PRECISION'
Has $ce 'ORBSHOT owner=' 'CastExecutor.class CONTROL-WRONG-CLASS'
Has $or 'heart_of_the_sea' 'OrbRemovals.class CONTROL-NONE'
$zip.Dispose()
```

**The prediction, line for line:** before `'--- controls'`, **37 `PRESENT`, 0 `ABSENT`**; after it, **9 `ABSENT`, 0
`PRESENT`**.

### R0c — every Loaded line, and nothing refused

`[Rpg] Loaded 11 abilities, 45 visuals, 5 statuses, 7 elements, 14 enchants, …` as on `efa38636` (no content file added
or removed by either PR), the candidates line and the `Mint-on-craft` and `Custom recipes` lines as there. **No `[Rpg]`
WARN beyond `volley_stone`'s known pair** — in particular **no line naming `lapis_crystal`** (neither `… is a ray that
names no beam …` nor `… is not a block: its throw will spawn no body`). No `Refusing`, `Skipping`, `SEVERE`,
`Exception`, `Dev-only`; no `ORB`, `ORBBODY` or `ORBSHOT` line at boot.

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 359 scenarios across 44 gates [melee-cell, …, enchant-columns, enchant-changes]`** (recount: 44 files, 403 keys, 44 setups; `SelfTestScenariosTest`). The controls: `351 scenarios` and `360 scenarios` ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

### R0e — the orphan sweep with nothing staged

As `GATE-enchant-columns.md` R0e: one `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`; and **no
`ORB removed … cause=orphan`** at enable (the sweep found no orb body, of either kind, in any loaded chunk).

## THE ROWS

Every damage prediction names its terms (`.claude/rules/verification.md`, *A DAMAGE PREDICTION NAMES EVERY TERM*):
`sent = ((amount x score/100) x enchantMultiplier + classDamageBonus + weaponLevelDamage) x chargeScale x crit`. Staged in
every row: **score 100** (`rpg gearscore set 100`: x 1.0); **enchantMultiplier 1.0** (the only enchant active is the
row's Airburst Flames or Lapis Crystal, neither a damage multiplier); **classDamageBonus 0** (nothing grants it);
**weaponLevelDamage 0** at **level 1** (`rpg playerxp set @s 1 levels`; `GATE-enchant-columns.md` EC14R2 read `sent=30.000`
on the same staging); **chargeScale 1.0** (an ability, not a melee charge); **crit x 1 or x 2**, the second the allowed
reading. Every row's mobs are frozen NoAI zombies at 1000 health, at night (18000): none burns in daylight. Every row
starts from `clear_state` (cooldowns cleared, his orb removed). **Every row-start clean after the setup: `LINGER work=0
entities=0 radius=48`.** `<X, Y, Z>` is `PROBE self`'s position (on `efa38636`, `104.50 201.00 -3.50`).

### setup — the pad, the ground, normal, night, no spawns, the trace on (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`PAD world=world stand=104,201,-4 floorY=200 interior=-2..45 east -13..13 south cells=7250 placed=0 repaired=0 ok=7250`** (the pad left standing since `efa38636`; a removed pad reads `placed=2066 repaired=0 ok=5184`, as there), **`PAD stood at=104.50 201.00 -3.50 yaw=-90.000 pitch=0.000`**, **`PREFLIGHT ok box=0..32 east -6..10 south air=1122/1122 floor solid=561/561`**; the difficulty step's line (recorded); `PROBE self` at `104.50 201.00 -3.50` | the pad, `GATE-enchant-columns.md` | |

### AF1 — THE SPLIT AT 6, AND NO MOB (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Living Lantern -- 1 slot(s)" [aqua]`, **`"  Slot 0: [*airburst_flames II]"`**, **`"  active: airburst_flames II (splits after 6 blocks into 3 bursts of 30 damage)"`**. **Exactly one `SPLIT caster=<him> source=living_lantern/right_click at=<X+6.300>,<Y+1.620>,<Z> bursts=3 tick=<t>`**, that is `at=110.800,202.620,-3.500`. **No PLAYERHIT.** The next row's clean: `LINGER work=0 entities=0` (the bursts end after their 20 steps; the row waits 60) | Ben's "B"; `AirburstSeekTest.theSplitIsTheEighteenthStepAtSixPointThreeBlocks` | |

### AF2 — THE BURSTS SEEK, LEVEL II: H, WHICH NO STRAIGHT LINE MEETS (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| AF1's `show` lines. **Exactly one `SPLIT … at=110.800,202.620,-3.500 bursts=3`** (H is 10.617 from the split and nothing is within 7 of the shot before it). **At least one PLAYERHIT `source=living_lantern/right_click target=<H> zombie element=fire sent=30.000`** (or `60.000 crit=true`); **HOW MANY (1, 2 or 3) IS A READING**; **each one's sum: `(30 x 100/100) x 1.0 + 0 + 0 = 30`, x 1.0, x crit**. **No `sent=45.000`** (the shot split; it struck nothing). Every hit on H is a burst that STEERED: the nearest straight line clears H's box by 1.638. Probe H (reach 18): `1000 - 30 x <hits>` less its burns (recorded). **Zero hits: the row FAILS** (the homing is not witnessed) | Ben's "seek as well"; `AirburstSeekTest` (the control, above) | |

### AF3 — LEVEL I AGAINST LEVEL III, AF2'S GEOMETRY (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `"  Slot 0: [*airburst_flames I]"`, `"  active: airburst_flames I (splits after 6 blocks into 3 bursts of 25 damage)"`; later `REPLY "Slot 0: Airburst Flames unlocked to III." [green]` (recorded). **Two `SPLIT … at=110.800,202.620,-3.500 bursts=3`**, 82 ticks apart (the lantern's cooldown is 16). After the first press, **each PLAYERHIT on H `sent=25.000`** (`(25 x 100/100) x 1.0 + 0 + 0`, or `50.000` on a crit); after the second, **each `sent=35.000`** (`(35 x 100/100) x 1.0 + 0 + 0`, or `70.000`). **How many land after each press is a READING; at least one after each.** Probe H (reach 18): recorded, with the burns | Ben's 25/30/35; `AirburstSeekTest.eachLevelLandsItsOwnBurstDamage` | |

### AF4 — A HIT BEFORE 6: THE 45, NO SPLIT (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| AF1's `show` lines. **One PLAYERHIT on N `source=living_lantern/right_click … element=fire sent=45.000`** (`(45 x 100/100) x 1.0 + 0 + 0 = 45`, or `90.000` on a crit): N stands 3 east on his aim line, inside its 1.95 at his eye's 1.62, so the shot meets it straight. **NO `SPLIT`** in the row. Probe N `1000 - 45` less its burns (recorded) | Ben's "Doesn't split if it hits something first"; `AirburstSeekTest.aMobBeforeSixTakesTheFortyFiveAndNothingSplits` | |

### LB1 — THE THROW AND ITS BODY, LEVEL II (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "Lapis Staff -- 1 slot(s)" [aqua]`, **`"  Slot 0: [*lapis_crystal II]"`**, **`"  active: lapis_crystal II (grants Left-Click: Lapis Crystal (Cooldown: 30.0s \| Mana Cost: 30); a beam into it strikes each foe within 8 for 10.5 damage)"`**. `DRIVE reload held=lapis_staff`. **Exactly one `ORBBODY owner=<him> id=<b> type=block_display block=minecraft:lapis_block glowing=true glow=#2A5CFF scale=0.500 teleport_duration=1 persistent=false tick=<t>`** — every value read back off the spawned body (the block's string form, `minecraft:lapis_block`, INFERRED). **Exactly one `ORB rest owner=<him> at=116.500,202.620,-3.500 tick=<t+10>`** (`<X+12.000>,<Y+1.620>,<Z>`, unchanged by the new body). At the row's end: **`ORB removed … at=116.500,202.620,-3.500 cause=selftest`**, then `STAGE clear_state … orb=removed`. **The next row's clean: `LINGER work=0 entities=0`** (the removal cancelled the expiry AND the backstop, and removed the body) | Ben's item 3; rulings (a), (b), (c); `OrbBlockBodyTest`, `OrbBlockBodyLoadTest` | |

### LB4 — GONE AFTER ITS LIFE, AND NOTHING LEFT (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| LB1's `show` lines; one `ORBBODY … type=block_display …`; **`ORB rest … at=116.500,202.620,-3.500 tick=<t>`**, then **exactly one `ORB removed … at=116.500,202.620,-3.500 cause=expired tick=<t + 300>`**; **no `cause=backstop` line, in this row or any later one.** **The next row's (LB2's) clean: `LINGER work=0 entities=0`**: the expiry removed the body and cancelled the backstop, so neither a task (it would read `work=1`) nor a body (`entities=1 [block_display …]`) is left | ruling (b); PR 1's item 6; `OrbBlockBodyTest.theBackstopDoesNotFireAfterTheExpiry` | |

### LB2 — A BEAM INTO THE ORB AT LEVEL II: N TAKES 10.5, ONE LINE TO N, F AND T NOTHING. THE ONE ASK (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| LB1's `show` lines; `ORBBODY …`; `ORB rest … at=116.500,202.620,-3.500`; `DRIVE use … item=lapis_staff yaw=-90.000 pitch=0.000`. **NO `lapis_staff/right_click` PLAYERHIT** (the beam stops at the orb). **Exactly one PLAYERHIT `source=lapis_crystal/crystal target=<N> zombie element=kinetic sent=10.500 crit=false triggerScore=100`**, or `sent=21.000 crit=true` (the beam's crit). **The sum:** `(21 x 50/100 = 10.5) x 100/100 x 1.0 + 0 + 0 = 10.5`, x 1.0, x the beam's crit. **Exactly one `ORBSHOT owner=<him> from=116.500,202.620,-3.500 to=116.500,202.740,1.500 visual=lapis_beam tick=<the hit's tick>`**: N's feet `116.5, 201, 1.5` plus its eye 1.74; **no ORBSHOT to F or T.** Nothing on F (9.145 from the orb) or T (10.130, behind it). Probes N **`989.500`** (or `979.000`), F **`1000.000`**, T (reach 24) **`1000.000`**. At the end `ORB removed … cause=selftest`, `orb=removed`; the next row's clean `LINGER work=0 entities=0`. **THE ASK (Ben, after the run): "Did a blue beam line run from a glowing lapis block to the zombie it hit?"** — the AUTO half witnesses the line's ENDS and VISUAL id and the body's settings on the server; **what he saw is only his** | Ben's items 4 and 5; `OrbShotDrawnTest`; `.claude/rules/verification.md` *AN AUTO READING IS A READING OF THE SERVER* | |

### LB3 — LEVELS I AND III: 6.3 AND 14.7 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `"  Slot 0: [*lapis_crystal I]"`, `"  active: lapis_crystal I (… a beam into it strikes each foe within 8 for 6.3 damage)"`. The first throw: `ORBBODY …`, `ORB rest …`, **one `lapis_crystal/crystal` PLAYERHIT on N `sent=6.300`** (`(21 x 30/100) x 1.0 x 1.0 + 0 + 0`; `12.600` on a crit) and one `ORBSHOT … to=116.500,202.740,1.500 visual=lapis_beam`. `ORB removed … cause=selftest`, `STAGE clear_state … orb=removed` (and its cooldown cleared); `REPLY "Slot 0: Lapis Crystal unlocked to III." [green]` (recorded). The second throw: a second `ORBBODY` (a new id), `ORB rest …`, **one PLAYERHIT on N `sent=14.700`** (`(21 x 70/100) x 1.0 x 1.0 + 0 + 0`; `29.400`) and one more `ORBSHOT` to N. **The shot's level is the THROW's** (`Orbs.Orb.level`), which is why each level throws its own orb. Probe N **`979.000`** (`1000 - 6.3 - 14.7`, no crit) | Ben's 30/50/70; `OrbShotDrawnTest.theThreeLevelsStrikeSixPointThreeTenPointFiveAndFourteenPointSeven` | |

## UNREAD BY DESIGN

- **The backstop FIRING.** It acts only when every other removal was skipped; no row can stage that without a fault the
  run does not have. Its firing is `OrbBlockBodyTest.theBackstopRemovesTheBodyWhenEveryOtherRemovalIsSkipped` and
  `theBackstopActsWhateverTheStoreSays`; what this gate reads is that it did NOT fire (LB4) and left no task (every
  `LINGER work=0`).
- **The flight's smoothness, the glow's colour and the block's look as seen.** Client-side; the ORBBODY line reads the
  settings on the server. THE ASK covers the look and the line.
- **What a crash leaves.** INFERRED: a display is never written to a chunk (`setPersistent(false)`, READ above), so a
  crash leaves nothing on disk; were one ever saved by a forcing caller (UNVERIFIED), the sweep at enable and at
  entity-add removes it by its orb tag (R0e's `cause=orphan` line is the witness, and none is predicted).
- **A burst steering onto a mob a SECOND burst already chose.** Allowed (READ above) and not staged on purpose: AF2's hit
  count is a reading.

## R0b, RUN BEFORE COMMIT

**THE JAR:** `paper/target/rpg-0.1.0-SNAPSHOT.jar`, 2,501,146 bytes, sha256 (of the jar file)
`0af7723b5a0752c64ac43437b5ebe6afb9f45f462456cd43c7defce44a89dd02`, written 2026-11-23 22:43:49 by `./mvnw -B clean
verify` (exit 0; core 2431, storage 99, paper 1920, 0 failures, 1 skipped) on the working tree of
`feat/lapis-crystal-block` at `07da32e7` with PR 2's code and this gate's files uncommitted: **the same bytes as PR 2's
code commit and this gate commit**, no source edited between that build and those commits (only this paragraph, a doc
the build does not read). **It is not the deployed jar:** the boot's R0b is this block re-run against
`run\plugins\rpg-0.1.0-SNAPSHOT.jar` as `dev-server.sh` builds it from the pushed head.

**THE INSTRUMENT:** this block extracted from this file (`tr -d '\r' | awk` between the fences, 63 lines; 37 `ValueLine`
or `Has` lines before the controls line, 9 after), run with `powershell -NoProfile -ExecutionPolicy Bypass -File …
-JarPath <the jar>`, exit 0.

**READING: AS PREDICTED.** Before `'--- controls'`: **37 `PRESENT`, 0 `ABSENT`**. After it: **9 `ABSENT`, 0 `PRESENT`**.
The `Has` lines' positive half is in the 37 (ten `PRESENT` from class files), so the instrument can express a class
string; their negative half is in the 9 (three `ABSENT`).

## THE RUN OF `78826389`, 2026-11-23 (23:01 boot, 23:02:45-23:03:47 run): R0 PASSED; EVERY ROW RAN; ONE DEFECT, CC'S

> **DATED NOTE, 2026-11-23, written after the run; no prediction above is edited.** Booted on the seat's approval of
> `78826389` and Ben's "ready". Boot command 23:00:31; deployed jar 23:01:04 (2,501,156 bytes, sha256
> `7f1a8ef4…e533adfd`); `latest.log` first line 23:01:05; this boot's `Done` 23:01:12. **A first wait for `Done` matched
> the PREVIOUS boot's log (20:34) before the deploy had happened; it was discarded, and the wait re-run against a marker
> made after the boot command.** R0 read 23:01:35; **Ben joined 23:02:34, after it.** Lines below are this boot's log;
> `<him>` is his id.

### R0, READ

- **R0a: AS PREDICTED.** `[23:01:11] … [Rpg] Build: 78826389`. Before the boot command: `git status --porcelain` printed
  nothing; `HEAD` and `ls-remote` both `788263896883c6a45ff328940fa3e34b21c05c89`.
- **R0b: AS PREDICTED.** The block extracted from the committed file (`git show HEAD:…`), `cmp` IDENTICAL to the copy run
  before the commit, run with `powershell -File` against the deployed `run\plugins\rpg-0.1.0-SNAPSHOT.jar`: **37
  `PRESENT`, 0 `ABSENT`**, then **9 `ABSENT`, 0 `PRESENT`**.
- **R0c: AS PREDICTED.** `[Rpg] Loaded 11 abilities, 45 visuals, 5 statuses, 7 elements, 14 enchants, …`; the
  candidates line, `Mint-on-craft: 38 indexed, 38 claiming, of 54 gear definitions`, `Custom recipes: 7 registered, 0
  replaced, 0 refused, of 7 authored (7 minting gear)`; the only `[Rpg]` WARN `volley_stone`'s pair; no line naming
  `lapis_crystal`; no `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Dev-only`; no `ORB`, `ORBBODY` or `ORBSHOT` line.
  The deployed folder: `content/enchants/` 14 `.yml`, 0 `dev_`, among them `airburst_flames.yml` (line 27 `  travel_blocks:
  6`) and `lapis_crystal.yml` (line 43 `      body: block`).
- **R0d: AS PREDICTED.** Exactly one `[Rpg] Selftest: dev server, 359 scenarios across 44 gates [melee-cell, …,
  enchant-columns, enchant-changes]`; `351 scenarios` and `360 scenarios` absent.
- **R0e: AS PREDICTED.** `FIGHTWORLD sweep path=…\rpgfight folders=0`; no `orphan`, no `deleting` at enable.

### THE SETUP AND THE CLEANS, READ

- `PAD world=world stand=104,201,-4 floorY=200 interior=-2..45 east -13..13 south cells=7250 placed=0 repaired=0
  ok=7250` (**the second-build line the seat expected**); `PAD stood at=104.50 201.00 -3.50 yaw=-90.000 pitch=0.000`;
  `PREFLIGHT ok box=0..32 east -6..10 south air=1122/1122 floor solid=561/561`; `DIFFICULTY found=NORMAL set=NORMAL`;
  `SELF … at=104.50 201.00 -3.50`. The setup's own clean `return=-98.48 63.00 24.06` (where he typed the command).
- **Every row's clean: `LINGER work=0 entities=0 radius=48` and `LEFTOVER living=0 radius=16`**, nine of each (setup,
  AF1-AF4, LB1, LB4, LB2, LB3), every `return=104.50 201.00 -3.50`. `removed=` 0, 0, 0, 1, 1, 1, 0, 0, 3 in that order
  (each row's own mobs, removed by the next row's clean).
- **STAGE:** each row's `clear_inventory cleared=… kept=nexus@8` and `clear_state cooldowns=cleared streaks=forgotten
  orb=none`; the in-row `clear_state` lines `orb=removed` in LB1, LB2, LB3 (twice). No `SKIPPED`, no `VOID`, no `ABORT`.
  `SELFTEST RUN FINISHED` 23:03:47, then `RESTORED … gamemode=SURVIVAL … difficulty=NORMAL … spawn_phantoms=true …
  orbsRemoved=0`.

### THE DEFECT: EVERY THROW'S BODY WAS SWEPT AS AN ORPHAN AT SPAWN, AND PAPER REFUSED THE REMOVAL

**Seven throws (four in the run, three of Ben's own after it), seven times on the tick of the `ORBBODY` line:**

```
[23:03:15] [Server thread/WARN]: Entity BlockDisplay['Block Display'/73, uuid='ff8fc9a3-…', … x=104.50, y=202.62, z=-3.50, …] is currently prevented from being removed from the world since it is processing section status updates
[23:03:15] … ORB removed owner=00000000-0000-0000-0000-000000000000 at=104.500,202.620,-3.500 cause=orphan tick=2464
[23:03:15] … ORBBODY owner=<him> id=ff8fc9a3-d935-4e29-9301-52badadb3a2d type=block_display … tick=2464
```

- **THE CAUSE, READ in CC's own code:** `PaperCombatWorld.spawnOrbBlock` sets the ORB TAG inside `world.spawn`'s
  consumer, so the body carries it when `EntityAddToWorldEvent` fires, inside the spawn, **before `orbs.place` has
  recorded its id**. `RpgListeners.onOrbBodyAppear` runs `OrbRemovals.sweep`, which finds a tagged body no orb owns and
  removes it. The item path (`spawnOrbMarker`) tags only after `spawnMarker` returns, so it never met this.
  `OrbBlockBodyLoadTest` pinned the tag inside the consumer; FakeWorld has no sweep. **Nothing in either suite could see
  it.**
- **WHAT THE SERVER DID, READ from the server jar:** `Entity.setRemoved` asks `EntityLookup.canRemoveEntity`, which is
  false while the entity `isUpdatingSectionStatus` (its own add); on false it logs that WARN and RETURNS without
  removing. **So no body was removed at spawn: each `cause=orphan` line reports a removal that did not happen.** The
  bodies stood; the proper removals took them later (`clear_state`, the expiry), and every following clean read
  `entities=0`.
- **The rows' readings below stand on that:** the store, the flight's arrival, the shots and the lines did what was
  predicted; each throw carries one unpredicted `cause=orphan` line and one Paper WARN.

### THE ROWS, IN THREE CLASSES

**MISS (four rows, one cause, the defect above):**

- **LB1 — MISS on one line.** Everything predicted read as predicted: `"  active: lapis_crystal II (… strikes each foe
  within 8 for 10.5 damage)"`; `DRIVE reload held=lapis_staff`; **`ORBBODY owner=<him> id=ff8fc9a3-… type=block_display
  block=minecraft:lapis_block glowing=true glow=#2A5CFF scale=0.500 teleport_duration=1 persistent=false tick=2464`**
  (the block string as INFERRED); **`ORB rest … at=116.500,202.620,-3.500 tick=2474`** (+10); `ORB removed …
  cause=selftest tick=2483`, `orb=removed`; LB4's clean `LINGER work=0 entities=0`. **Unpredicted: `ORB removed
  owner=00000000-… at=104.500,202.620,-3.500 cause=orphan tick=2464`.**
- **LB4 — MISS: "exactly one `ORB removed … cause=expired`" holds, but the row has TWO `ORB removed` lines.**
  `ORBBODY … id=8c8657bc-… tick=2533`; `ORB rest … tick=2543`; **`ORB removed … cause=expired tick=2843`** (2543 + 300,
  exact); no `cause=backstop` in the row or anywhere in the log; LB2's clean **`LINGER work=0 entities=0`**. Unpredicted:
  `cause=orphan tick=2533`.
- **LB2 — MISS on one line; its predictions all read.** `ORBBODY … id=5322dab5-… tick=2912`; `ORB rest … tick=2922`;
  `use … yaw=-90.000 pitch=0.000`; **no `lapis_staff/right_click` PLAYERHIT**; **exactly one `ORBSHOT owner=<him>
  from=116.500,202.620,-3.500 to=116.500,202.740,1.500 visual=lapis_beam tick=2933`**; **exactly one `PLAYERHIT …
  source=lapis_crystal/crystal target=<N> zombie element=kinetic sent=10.500 crit=false triggerScore=100`**; no ORBSHOT
  or hit on F or T; probes **N `989.500`** (d 13.000), **F `1000.000`** (d 15.000), **T `1000.000`** (d 22.000); `ORB
  removed … cause=selftest tick=2971`, `orb=removed`. Unpredicted: `cause=orphan tick=2912`.
- **LB3 — MISS on two lines; its predictions all read, the second hit on its crit.** `"  active: lapis_crystal I (…
  for 6.3 damage)"`; `ORBBODY … tick=3021`, `ORB rest … tick=3031`, **`ORBSHOT … to=116.500,202.740,1.500
  visual=lapis_beam tick=3042`**, **`sent=6.300 crit=false`**; `cause=selftest tick=3060`, `orb=removed`; `REPLY "Slot 0:
  Lapis Crystal unlocked to III." [green]`; `ORBBODY … tick=3063`, `ORB rest … tick=3073`, **`ORBSHOT … tick=3084`**,
  **`sent=29.400 crit=true`** (14.7 x 2, the allowed reading); probe **N `964.300`** (`1000 - 6.3 - 29.4`, the crit's
  reading); `cause=selftest tick=3102`. Unpredicted: `cause=orphan` at 3021 and at 3063.

**DID NOT TEST WHAT IT WAS MEANT TO: none.**

**READ AS PREDICTED (four rows):**

- **AF1.** `"  active: airburst_flames II (splits after 6 blocks into 3 bursts of 30 damage)"`; `SELF … at=104.50 201.00
  -3.50`; **exactly one `SPLIT caster=<him> source=living_lantern/right_click at=110.800,202.620,-3.500 bursts=3
  tick=1943`**; no PLAYERHIT.
- **AF2.** **`SPLIT … at=110.800,202.620,-3.500 bursts=3 tick=2052`**; **THREE PLAYERHITs on H, `sent=30.000
  crit=false triggerScore=100`, all three at 23:02:55**: all three bursts steered onto the mob none of their straight lines
  meets. **Their ticks are NOT READ: a PLAYERHIT line carries no tick**; the wall clock puts them in the same second, 1
  to 2 s after the split. No `sent=45.000`. Probe H **`880.000`** (`1000 - 90`, and burns of 30 recorded;
  `fireTicks=73`), d 16.492.
- **AF3.** `"  active: airburst_flames I (… 25 damage)"`; `SPLIT … tick=2181`, **three `sent=25.000`** (23:03:01);
  `REPLY "Slot 0: Airburst Flames unlocked to III." [green]`; `SPLIT … tick=2263` (82 later), **three `sent=35.000`**
  (23:03:06); all six `crit=false`, all on H; ticks NOT READ, as AF2. Probe H **`735.000`** (`1000 - 75 - 105`, and burns
  of 85 recorded), d 16.492.
- **AF4.** **One PLAYERHIT on N `sent=45.000 crit=false`**; **no `SPLIT`** in the row; probe N **`932.500`** (45 and a
  burn of 22.5, recorded), d 3.000.

### BEN'S ANSWER

**NOT STATED.** He typed no chat line between his join (23:02:34) and his `/stop` (23:05:32). Not asked again.

**Recorded without comment, after the run:** `/rpg give lapis_staff` (23:04:24); two throws of his own (`ORBBODY` at
ticks 4243 and 4848, each with its `cause=orphan` line and WARN, each `ORB removed … cause=expired` 300 after its rest);
`/rpg spawn knell` four times (23:04:57-58); `/stop` (23:05:32).

### AFTER HIS `/stop`

`rpgfight` 0 entries; the overworld 106 files (106 at the backup); `All dimensions are saved` 23:05:33; no `java`
process; **no `ORB` line reads `cause=stop`** (no orb was standing; his second throw expired at tick 5158, before the
stop); **his saved position `-98.56 63.00 34.60`** (READ off his player file's `Pos`), not the pad.

### UNREAD, AND WHY

- **Ben's answer to THE ASK:** not stated, so the look of the body, its glow and the drawn line as seen are unread.
- **Whether each body moved along the path:** no line reads a display's position during its flight.
- **The ticks of AF2's and AF3's burst hits:** PLAYERHIT prints none.
- **The backstop firing:** by design (above). It did not fire anywhere in the log.
