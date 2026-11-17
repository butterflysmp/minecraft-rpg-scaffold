# GATE — THE PHALANX'S SHOT (STEP R7a): DESTROYABLE, HOMING, LIGHT PURPLE; THE BOW IT NEVER FIRES; THE KNELL'S ONE FLASH

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-shot`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

> **AFTER THE BOOT OF `8ec16e89`: THE RE-PREDICTION IS THE LAST SECTION OF THIS FILE** (*RE-PREDICTED AFTER THE BOOT OF
> `8ec16e89`*, the seat's rulings on #201), NOT RUN. Nothing above it is edited by that section.

> **REWRITTEN BEFORE ANY BOOT, 2026-11-17, on the seat's ruling on #201 (the homing matches the old game).** The homing
> moved from 0.1 to **0.0513 a tick** (`phalanx.yml`; the plan's 5.R.9 X-c), so the flight was re-run in jshell at the
> new value and every prediction resting on it re-checked. **Changed:** S4's expression; SH6's `at` and `flown` (its
> age, 176, did not move); E1's straight figure and turning radius; SH10's second wait, 100 to 140, so its second hit
> does not rest on a thin margin; the run's length. **Unchanged, re-run and the same to three places:** SH1, SH4, SH5,
> SH7, SH8, SH12 and SQ1, whose targets stand dead ahead, so the homing has nothing to turn. The gate had been neither
> booted nor read, so no reading is touched.

> **READ, 2026-11-17: booted at `8ec16e89` on the seat's approval and Ben's "ready".** Server log `09:30:43` to the clean
> `/stop` at `09:33:43`. Carried state: no JVM; backup `rpg-run-backups/2026-11-17-pre-dragon-shot` (`run/plugins/Rpg`
> 129 files, 387612 bytes; `run/world` 193 files, 204963734 bytes; `diff -rq lines: 0` each); `12:  dev-pack-on-join:
> true`. Ben logged in at `09:31:03`, typed `/gamemode survival` first (`09:31:09`), then `/rpg selftest dragon-shot`
> once (`09:31:14`) to `SELFTEST RUN FINISHED` (`09:33:15`), `DIFFICULTY found=NORMAL set=NORMAL`, and `RESTORED …
> difficulty=NORMAL`.
>
> **THE DEPARTURE, recorded as it happened:** after the run finished Ben typed `/rpg selftest dragon-beam` (`09:33:22`, not
> on this run sheet), which ran KB1R5 and started KB2R5; he stopped it with `/rpg selftest stop` (`09:33:29`: `SELFTEST
> RUN STOPPED by /rpg selftest stop`, then `RESTORED … mobsRemoved=1 … difficulty=NORMAL`), then `/stop` (`09:33:43`).
> **That run is GATE-dragon-knell-5.md's, not this gate's, and none of its lines is read here.** The server-stop restore
> stays UNREAD: no run was stopped by `/stop`.
>
> **The result: R0a-R0d MATCH; 9 of the 12 AUTO rows AS PREDICTED; SH3, SH7 and SH9 NOT AS PREDICTED** (each in its
> row): the arrow never hit the shot; the wall destroyed it a tick early by SUFFOCATION; the row's own kill of its Void
> Phalanx grew the Phage. Every LEFTOVER read `living=0`; no `SHOT orphan removed` line; every HURT line came from a
> shot or the Knell's beam. **Ben's four answers: "yes", "yes", "yes", "yes"; his "yes" to "Did hitting it destroy
> it?" disagrees with the server, which logged his second shot as a HIT** (SQ1).

The plan is `PLAN-dragon-mobs.md` (RULINGS, *the Phalanx's shot starts from the old game's numbers*; §3.4; §7 F-S). The
scenario file is `paper/src/main/resources/selftest/GATE-dragon-shot.yml`, run by `/rpg selftest dragon-shot`.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **Every row is SURVIVAL.** The shot's predicate (`MobHostility.hostileTo`) skips a creative or spectator player, so a
  creative bench reads no row here: the run sheet's first line is `/gamemode survival`.
- **Difficulty NORMAL**, set by the gate's own `setup:` (`difficulty: normal`), restored at the run's end. It matters to
  SH10 and SH11: a skeleton's bow interval is 40 ticks off HARD, 20 on it (READ, `AbstractSkeleton#reassessWeaponGoal`).
- **Every Phalanx is spawned at GS 100** (`spawn: "<tag> frozen phalanx 100"`), so a hit is **40.000 before defence**.

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC

His custom health floor is 100 (every row heals him first with `rpg heal 1000000`). The largest thing that can land on
him after a heal:

| row | what can land after its heal | before defence | can he die |
|---|---|---|---|
| SH1, SH8 | one shot | 40 | no: 40 < 100 |
| SH2, SH3, SH4, SH5, SH9 | one shot, only if the row's destroying step failed | 40 | no |
| SH6, SH7, SH12 | nothing (it expires, meets the wall, ends with its Phalanx); at worst one shot | 40 | no |
| SH10 | the first shot (≈ tick 163), then the row heals (tick 200), then the second (≈ tick 283); the row ends at 340, before a third fire (360) | 40, then 40 | no: one 40 per heal |
| SH11 | nothing (100 ticks, before its clock's 120; the bow never fires) | 0 | no |
| SQ1 | one shot; the row heals; then one more if he misses it | 40, then 40 | no: one 40 per heal |
| KQ1R6 | one Knell beam | 65 | no: 65 < 100 |

**No Phalanx in this file can melee him:** the frozen ones have no AI, and a live Phalanx holds a bow, so vanilla gives it
the bow goal in place of melee (READ, `AbstractSkeleton#reassessWeaponGoal`: a bow → `RangedBowAttackGoal` at priority 4,
the melee goal removed). Its releases are cancelled before the arrow exists (READ, below).

## WHAT THE ROWS STAND ON

Every server fact below was read in the pinned jar (`~/.jdks/openjdk-26.0.1/bin/javap.exe -c -p -cp
run/versions/26.1.2/paper-26.1.2.jar <class>`) or in the pinned API source (`paper-api-26.1.2.build.74-stable-sources.jar`),
2026-11-17. **READ** = opened; **INFERRED** = reasoned from what was opened.

### S1 — THE CARRIER: A BAT, AND WHAT WAS REJECTED

| property | class#member, as read | mark |
|---|---|---|
| it is LIVING, so our weapons reach it | `PaperCombatWorld.combatantsNear` and `castRay` keep `LiveTargets.isLive` (a `LivingEntity`); `Bat extends AmbientCreature extends Mob` | READ (our code; `javap` of `Bat`) |
| its size | `EntityType.<clinit>`: `bat` `sized(0.5f, 0.9f)`. A display rides at its feet + (0, 0.9, 0) (`Entity#positionRider`, `EntityAttachment.PASSENGER` → `AT_HEIGHT`; `Display` has no vehicle attachment) | READ |
| a swing and an arrow raise the event we cancel | `Bat#hurtServer`: `isInvulnerableTo` → false; wakes if resting; then `LivingEntity#hurtServer`, the ordinary path that raises `EntityDamageEvent` | READ |
| no AI, no movement of its own | `LivingEntity#aiStep`: `serverAiStep` and `travel` only when `isEffectiveAi()`; `Mob#isEffectiveAi` = `super && !isNoAi()`. So it moves only by our `teleport`, which carries its passenger (`Entity#teleportSameDimension` teleports each passenger; `TeleportFlag.EntityState` is deprecated, "functionally done by default") | READ |
| no gravity | `Entity#setGravity(false)` (API); and `travel` never runs without AI | READ (API) / INFERRED |
| silent | `Mob#baseTick` plays ambient sound with no AI gate; `Entity#playSound(SoundEvent,float,float)` returns at `isSilent()` (offset 0–4) | READ |
| invisible | `CraftEntity#setInvisible`: `persistentInvisibility = b; setSharedFlag(5, b)`. No invisibility filter in `CraftWorld#getNearbyEntities` or `rayTraceEntities` | READ |
| never saved | `CraftEntity#setPersistent(b)`: `entity.persist = b`; `Entity#saveAsPassenger` returns false when `!persist` (offset 31–48), so nothing is written; a chunk unload discards it; a passenger of an unsaved vehicle is lost with it. Server stop uses the same chunk save | READ / server stop INFERRED |
| no despawn surprise | `Mob#checkDespawn` skips `persistenceRequired` (`CraftLivingEntity#setRemoveWhenFarAway(false)`) and any mob with a passenger (`requiresCustomPersistence`) | READ |
| no targeting by other mobs | no goal names `Bat`; Iron and Snow Golems take `Enemy` only (a bat is not one); the **Warden's** `canTargetEntity` accepts any living body. `RpgListeners.onShotTargeted` cancels every target event naming a shot | READ; the cancel is ours |
| no collision | `Bat#isCollidable` false; `doPush` a no-op; and `setCollidable(false)` | READ |

**Rejected:**
- **an ARMOUR STAND** (the obvious "no AI at all" body): READ, `ArmorStand#hurtServer` passes `this.invisible` as the
  pre-cancelled flag of every damage event (`handleNonLivingEntityDamageEvent(…, true, this.invisible)`), so an
  invisible stand's damage arrives already cancelled; a marker stand returns before any event; and our own LEFTOVER count
  and seed both skip armour stands by type. A stand we could not see would be one LEFTOVER could not see either.
- **an ENDERMITE or SILVERFISH** (the smallest, 0.4 × 0.3): READ, `EnderMan` targets `Endermite`, golems target any
  `Enemy`, and a `Monster` is discarded on Peaceful. More behaviour to suppress in a fight that is full of Endermen.
- **an ALLAY** (0.35 × 0.6): a right-click with an item gives it the item (`Allay#mobInteract`), which our removal would
  then delete.
- **a Shulker bullet or an Interaction**: not living, so our weapons pass through (seat ruling 8, §7 F-S).
- **Size is not the deciding fact:** any living body can be scaled with `Attribute.SCALE` (`LivingEntity#getDimensions`
  scales by `getScale()`, §7 F-C), so the type was chosen on behaviour. The bat is unscaled today: a 0.5 × 0.9 box
  around a 0.4 block, more generous to hit than the block looks (listed for Ben).

### S2 — THE ATTACK PATHS, AND HOW EACH REACHES IT

| path | how it reaches the shot | destroyed line's `by=` |
|---|---|---|
| a player's MELEE swing, any held item (our weapons' basic attack is vanilla's swing, `BasicMelee`) | vanilla `EntityDamageByEntityEvent`, cause `ENTITY_ATTACK` (or `ENTITY_SWEEP_ATTACK`), cancelled at LOWEST by `RpgListeners.onShotDamaged` | `vanilla cause=ENTITY_ATTACK damager=player <uuid>` |
| a VANILLA ARROW (any bow, or none) | vanilla event, cause `PROJECTILE` | `vanilla cause=PROJECTILE damager=arrow <uuid>` |
| a vanilla explosion, fire, lava, `/kill`, any environmental damage | vanilla event, its cause | `vanilla cause=<cause>` |
| an ability's direct hit, a RAY (the Boltor), a PROJECTILE cast | `castRay` → `BukkitCombatant` handle → `applyDamage` → `ShotSink.strike` first | `ability dealer=<uuid> element=<e> amount=<a>` |
| a BURST, a SWEEP, the dash's swept line, an AREA pulse, Ignite's blast | `combatantsNear` / `SweptLine` → the same handle | the same |
| a knockback, an impulse, a status (Wither, Scorch, Freeze, Rooted, Soaked) | the handle's `applyKnockback` / `applyImpulse` / `applyStatus`, each striking first | `ability knockback=…`, `ability impulse`, `ability status=<id>` |
| a player HOMING projectile (`ProjectileFlight.nearestMob`) | the shot is a non-player living body, so homing may pick it; then the direct hit above | as above |

### S3 — EACH "DOES NOT", AND WHAT GUARANTEES IT

| it does not | guaranteed by | pinned by |
|---|---|---|
| have health, a bar, a nameplate, a gear score | `MobNameplateManager.seed` returns null for a `mob_shot` body, the one seed | `DragonShotWiringTest.theSeedRefusesAShot`; MUTSEED |
| die, so pay XP or drops, count as a kill, feed growth or split | it is only ever `remove()`d (`discard(PLUGIN)`, no death event); never in the store, so `MobDeathSystem` cannot kill it; `onMobDeath` returns for a shot before `MobGrower` and `MobSplitter`; `onShotDeath` clears drops and XP for a death we did not make | `DragonShotWiringTest.theListenersMeetAShotFirstEverywhere`, `…theCarrierIsSetUpBeforeItsAddAndOnlyEverRemoved`; MUTDEATH |
| take a status, a push, a health change | every handle strikes first | `DragonShotWiringTest.everyHandleCallAsksTheSinkBeforeItActs`; MUTSINK1 |
| survive any vanilla damage | cancelled and struck at LOWEST, not ignoreCancelled | `…theListenersMeetAShotFirstEverywhere`; MUTCANCEL |
| pay COINS | **no code in this repository pays coins for any mob** (`grep -rln -i coins paper/src/main/java` prints nothing at `43469d0c`) | nothing to pin |
| hurt a mob | `MobShot.victim` passes over any `Side.MOB` | `MobShotTest.aPlayerInTheBoxIsHitAndAMobInTheSameBoxNever`; MUTMOBS |

### S4 — THE FLIGHT, AS EXECUTED (jshell, 2026-11-17, the expression in `MobShot`'s javadoc, not our class)

`toward = normalize(aim − position)`; `flight = normalize(0.9487 × flight + 0.0513 × toward)`; `position += 0.2 × flight`.
**Positions are RELATIVE TO HIS FEET** (the row's `CLEAN … return=` line is his feet); the Phalanx's eye is its feet +
`(double) 2.1f` (READ, `EntityType`: `wither_skeleton` `eyeHeight(2.1f)`), his eye his feet + `(double) 1.62f`. A hit is
the new position inside his box (0.6 × 1.8) grown by 1.2: x within ±1.5, y −1.2 to 3.0, z within ±1.5.

**From 8 east (SH1, SH8)**, `flown` = 0.2 × age exactly:

| age | at (rel.) | flown |
|---|---|---|
| 8 | 6.403 2.004 0.000 | 1.600 |
| 9 | 6.203 1.992 0.000 | 1.800 |
| 10 | 6.004 1.980 0.000 | 2.000 |
| 11 | 5.804 1.968 0.000 | 2.200 |
| 12 | 5.604 1.956 0.000 | 2.400 |
| 18 | 4.406 1.884 0.000 | 3.600 |
| 19 | 4.207 1.872 0.000 | 3.800 |
| 20 | 4.007 1.860 0.000 | 4.000 |
| 21 | 3.808 1.848 0.000 | 4.200 |
| 22 | 3.608 1.836 0.000 | 4.400 |
| **33, HIT** | **1.412 1.705 0.000** | **6.600** |

**From 12 east (SH4, SH5, SH12):** age 8 at 10.401 2.036 0; age 15 at 9.002 1.980 0; age 35 at 5.006 1.820 0; **HIT at age
53**, 1.408 1.676 0.

**SH6, chased to 38 west after k ticks** (k = his move's tick minus the fire's), **EXPIRED by range at age 176 for every k
from 0 to 6**: at x −27.195 to −27.194, y 1.680 to 1.662, z 0; `flown` 35.198 to 35.196.

**E1 — THE LIFE OF 240 IS UNREACHABLE AT SHIPPED NUMBERS, SAID PLAINLY.** A shot flying straight passes 35 blocks from
its origin at age 176 (jshell, above: a target 10000 west, `RANGE age=176 flown=35.197465`), 64 ticks before 240; and the
homing turns it only toward a target it then reaches (≈ 3.7 blocks' turning radius at 0.2 a tick and 0.0513: at most 3.1 degrees a tick). So
`limit=life` needs a shot that circles within 35 of where it was fired for 12 seconds without reaching anyone, which no
row here can stage. **Its only exercise is `MobShotTest.theLifeEndsItAtTick240AndNotAt239` (killed by MUTLIFE).** The
brief's "a shot left to expire at 240 ticks" is SH6, which expires by RANGE.

**SH7, the wall:** stone at the block 4 east (x 3.5 to 4.5 from his feet); **BLOCKED at age 18**, at 4.406 1.884 0.000.

### S5 — THE BOW

READ, `RangedBowAttackGoal#tick`: while using the bow with sight, `getTicksUsingItem() >= 20` → `stopUsingItem`,
`performRangedAttack`, `attackTime = attackIntervalMin`; while not using, `--attackTime <= 0` starts the draw. Off HARD
the interval is 40 (`reassessWeaponGoal`). READ, `AbstractSkeleton#performRangedAttack`: the arrow is a
`Projectile.Delayed` that is not yet spawned; `callEntityShootBowEvent(…)`; cancelled → `getProjectile().remove()`,
`return`, **so `spawn()` is never reached and no arrow ever enters the world, with no shoot sound.** So a live Phalanx
that targets him releases about 20 ticks after it starts drawing and every 60 after: each release is one
`[Rpg] BOW suppressed mob=phalanx id=<P> tick=<t>` line. **When it acquires him is vanilla's** (its target goal's
random interval), so the COUNT of BOW lines is a bound, never a number.

## THE ROWS ALREADY MERGED (the seat's ruling 12)

**Every merged scenario that spawns a Phalanx:** `grep -n -i phalanx paper/src/main/resources/selftest/*.yml` at
`43469d0c` prints two lines, both `GATE-dragon-bodies.yml`'s **DB5R2**: a FROZEN Phalanx, alive 5 ticks, read by
`probe_body`. **It cannot fire:** its own clock needs AI (the plan's X-o) and 120 ticks, and no step forces it. **It
cannot draw:** NoAI stops its goals (`Mob#isEffectiveAi`). It now holds a bow, which `probe_body`'s `BODY` line does not
print. **So DB5R2's prediction (`GATE-dragon-fix.md`) stands unchanged, and it is not re-run.**

**Every row that counts entities, reads LEFTOVER, or picks a "nearest mob":** a carrier exists only while a Phalanx's
shot is in flight, and no merged row makes one fire, so none of them can meet a carrier. **LEFTOVER would count one** (a
living bat, `SelfTestStaging.leftover`), and every shot carries `selftest`, so the row-start clean removes it.

**The guard, as for the Phage and the Knell:** `SelfTestScenariosTest.noScenarioOutsideTheShotGateMakesAPhalanxFire`
fails on a live Phalanx spawn, a bare `rpg spawn phalanx` not frozen at once, or a `shot:` step in any other gate. **Its
control:** it must find SH10's and SH11's live spawns and this gate's twelve `shot:` steps (killed by MUTSCAN2, which
blinded its spawn pattern).

**The Knell's flash in the closed gates:** every merged Knell row's readings stay as read; none predicts the flash (it is
logged nowhere). KQ1R6 re-asks Ben.

---

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-dragon-shot"; [ -e "$B" ] && B="$B-2"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   Report the folder, both pairs of counts, and `diff -rq lines: 0` each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report.
4. **Boot this branch's head:** `./scripts/dev-server.sh --refresh-content`, announcing "booting now", on Ben's "ready".
5. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` line names this branch's head. **Readings count
   only after Ben's LOGIN in that boot's log.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest dragon-shot` — **ONCE. Wait for the chat to say the run finished, and answer every gold question,
   before the next command.** About **2½ minutes** plus the time you take to answer (its waits sum to 1.41 minutes; the
   fourteen rows' cleans and teleports add a few seconds each). **Stand still and touch nothing until the gold text
   asks you to.** You will be shot several times; you cannot die (every shot is 40 and you are healed before each).
   **Near the end** a Phalanx fifteen blocks east fires at you twice: **watch the first shot come at you and let it hit
   you; then the row puts a sword in your hand, and you hit the second shot with it before it reaches you.** Then a Knell
   twelve blocks east beams you once: **watch its purple warning line.** Four questions in all; answer each `yes` or
   `no`, or in your own words.
3. `/stop`

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-shot` prints it, shortened. **Not** master's `8365e7f6`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1`, after this boot's `Done` | READ 2026-11-17: `[09:30:43] [Server thread/INFO]: [Rpg] Build: 8ec16e89`, this PR's head (`git ls-remote` `8ec16e891f22…`). **MATCH** |

### R0b — the jar carries the shot, the bow and the one flash

| prediction | instrument | READING |
|---|---|---|
| `content/mobs/phalanx.yml` PRESENT `main_hand: bow`, `  period_ticks: 120`, `  life_ticks: 240`, `  max_distance: 35`; `content/mobs/knell.yml` PRESENT `  flash_at: [0]`, ABSENT `  flash_at: [0, 18]`; classes PRESENT `health/MobShooter.class`, `health/MobShotLines.class`, `adapter/ShotSink.class`; `GATE-dragon-shot.yml` PRESENT `SH1:`, `SQ1:`, `KQ1R6:`. **The controls, the same on both builds:** `knell.yml` PRESENT `  telegraph_ticks: 24`; `scheduler/InlineScheduler.class` PRESENT; `GATE-dragon-beam.yml` PRESENT `KQ1R5:` | the scan below | READ: all 15 lines as predicted: the four `phalanx.yml` lines PRESENT; `flash_at: [0]` PRESENT, `[0, 18]` ABSENT; `MobShooter`, `MobShotLines`, `ShotSink` PRESENT; `SH1:`, `SQ1:`, `KQ1R6:` PRESENT; the three controls PRESENT. (Also read, not predicted: `  homing: 0.0513` PRESENT.) **MATCH** |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$p = Bytes 'content/mobs/phalanx.yml'
foreach ($l in 'main_hand: bow', '  period_ticks: 120', '  life_ticks: 240', '  max_distance: 35') {
  if ($p -ne $null -and $p -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT phalanx.yml '$l'" } else { "ABSENT  phalanx.yml '$l'" }
}
$k = Bytes 'content/mobs/knell.yml'
foreach ($l in '  flash_at: [0]', '  flash_at: [0, 18]', '  telegraph_ticks: 24') {
  if ($k -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT knell.yml '$l'" } else { "ABSENT  knell.yml '$l'" }
}
foreach ($c in 'health/MobShooter', 'health/MobShotLines', 'adapter/ShotSink', 'scheduler/InlineScheduler') {
  if ((Bytes "io/github/butterflysmp/rpg/paper/$c.class") -ne $null) { "PRESENT $c.class" } else { "ABSENT  $c.class" }
}
foreach ($q in @(@('dragon-shot', 'SH1'), @('dragon-shot', 'SQ1'), @('dragon-shot', 'KQ1R6'), @('dragon-beam', 'KQ1R5'))) {
  $y = Bytes "selftest/GATE-$($q[0]).yml"
  if ($y -eq $null) { "MISSING selftest/GATE-$($q[0]).yml" } elseif ($y -match "(?m)^$($q[1]):\s*$") { "PRESENT $($q[0]) $($q[1]):" } else { "ABSENT  $($q[0]) $($q[1]):" }
}
$zip.Dispose()
```

**A jar of master `8365e7f6` reads the opposite:** ABSENT on the four `phalanx.yml` lines and the three new classes,
`flash_at: [0]` ABSENT and `[0, 18]` PRESENT, and `MISSING selftest/GATE-dragon-shot.yml`; the three controls PRESENT on
both. (The `[0]` pattern is anchored at the line's end, so it cannot match `[0, 18]`.)

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim the boot of `07d06837`'s (this branch adds no file, it changes two). No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN` beyond `volley_stone`'s known pair. **A `Skipping malformed mob 'phalanx.yml'` line would mean the shot block was refused** (`4 mobs`); `'knell.yml'` the flash | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | READ: the Loaded line verbatim as predicted; no `Refusing`, `Skipping`, `SEVERE` or `Exception` in the whole log; the only `[Rpg]` WARN lines are `volley_stone`'s known pair (and Paper's own update banner, not ours). **MATCH** |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 131 scenarios across 18 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot]`**, and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | READ: `[Rpg] Selftest: dev server, 131 scenarios across 18 gates [melee-cell, … dragon-teleport, dragon-shot]`, the list verbatim; no `Refusing selftest`. **MATCH** |

**The 131 is RECOUNTED from the files at this branch's head:** in `paper/src/main/resources/selftest/`,
`grep -cE '^  - ' index.yml` prints **18**; summing `grep -cE '^[A-Za-z0-9-]+:'` over the eighteen `GATE-<g>.yml`
prints **149**; the same with `'^setup:'` prints **18**: 149 − 18 = **131**. **The control:** the same three commands over
the first seventeen gates (master's index) print 17, 134, 17: 117, `GATE-dragon-knell-5.md` R0d's reading.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument:** `Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-shot|SELFTEST RUN|\[Rpg\] (SHOT|BOW|BEAM)'`,
after Ben's LOGIN, inside each row's own `START … DONE` bracket.

**In EVERY row (not repeated):** `LEFTOVER living=0 radius=16` after its CLEAN (a stray carrier is a living bat, so the
count names it: `SelfTestStaging.leftover` excludes only players, armour stands and the dead); each spawn's `DRIVE spawn
tag=<tag> <uuid> phalanx frozen` (or `live`) and its `REPLY` line in the Knell rows' form, naming the Phalanx, 240 HP and GS 100 (its exact text not predicted);
each `DRIVE shot tag=<tag> <uuid>` with no `refused=`; each forced shot's `[Rpg] SHOT fired mob=phalanx id=<P> gs=100
forced=true tick=<F> shot=<S> target=player <Ben> from=<P's eye> aim=<his eye> dealt=40.000`, then `SHOTTAG owner=<P>
shot=<S> tags=[<tag>_shot, selftest, t_shot]`; **exactly one ending line per fired line** (`hit`, `destroyed`,
`expired`, `blocked` or `ended`); and after every ending, `SHOT tag=<tag> <P> live=0` at the row's later `probe_shot`.
**No `[Rpg] SHOT` line names a Phalanx that is not the row's own, and no `HURT` line appears that the row does not
predict.**

**A HIT** is `[Rpg] SHOT hit mob=phalanx id=<P> shot=<S> age=<a> at=<x y z> flown=<f> victim=player <Ben> dealt=40.000`,
then `HURT before=40.000 after=36.364 defense=10.000 dealer=<P> dealerType=wither_skeleton/phalanx` (40 × 100 / 110, if
his defence is still the 10 the Knell rows read; a different defence changes only `after=` and `defense=`).

### SH1 — a forced shot at a still player: the flight, the hit, the HURT (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The fired line with `from=` = his feet + (8, 2.100, 0) and `aim=` = his feet + (0, 1.620, 0). Three `probe_shot` reads, 10, 20 and 50 ticks after the `shot:` step: the first two `SHOT at … age=<a> at=<x y z> flown=<f>` each matching S4's table AT ITS OWN AGE to three places (the ages are the runner's: about 9 and 19); the third `live=0`. **One `SHOT hit … age=33 at=<feet + 1.412 1.705 0.000> flown=6.600 … dealt=40.000`**, then the HURT line for 40 before defence | S4 (jshell); `MobShot.step`, `ending`, `victim`; the hit is the one priced by `MobScaling.attackDamage(40, true, true, 100)` through the two-argument `applyDamage`, the beam's final call | **AUTO PASS** (selftest 8ec16e89): feet `-100.5 63 5.5`; `SHOT fired … tick=634 … from=-92.500 65.100 5.500 aim=-100.500 64.620 5.500 dealt=40.000`; `SHOT at … age=10 at=-94.496 64.980 5.500 flown=2.000` (rel. 6.004 1.980 0, S4's age 10 **exactly**); `age=20 at=-96.493 64.860 5.500 flown=4.000` (rel. 4.007 1.860 0, **exactly**); `SHOT hit … age=33 at=-99.088 64.705 5.500 flown=6.600 victim=player <Ben> dealt=40.000` (rel. 1.412 1.705 0); `HURT before=40.000 after=36.364 defense=10.000 dealer=<P> dealerType=wither_skeleton/phalanx`; the third read `live=0`. **AS PREDICTED** |

### SH2 — one melee hit with the emberblade destroys it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `DRIVE attack Player#attack target=<S> bat …`, then **`[Rpg] SHOT destroyed mob=phalanx id=<P> shot=<S> age=<a> … by=vanilla cause=ENTITY_ATTACK damager=player <Ben>`**, `a` from 12 to 15 (the attack is 15 ticks after the step; the fire is 1 or 2 ticks after it), its `at` S4's table at that age. **No HURT line in the row.** The `probe_shot` reads `live=0` | `Player#attack` → `Bat#hurtServer` → `LivingEntity#hurtServer` → the event (S1); `onShotDamaged` LOWEST cancels and strikes | **AUTO PASS**: `DRIVE attack Player#attack target=<S> bat d=5.412 charge=1.000`; `SHOT destroyed … age=14 at=-95.295 64.932 5.500 flown=2.800 by=vanilla cause=ENTITY_ATTACK damager=player <Ben>` (rel. x 5.205 = 8 − 14 × 0.2 × 0.998205); no HURT; `live=0`. **AS PREDICTED** |

### SH3 — one vanilla arrow destroys it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The `CMD /execute at @e[tag=sh3_shot …] run summon arrow …` line; then **`[Rpg] SHOT destroyed … by=vanilla cause=PROJECTILE damager=arrow <uuid>`**, age from 13 to 17. **No HURT line in the row** (the arrow rises through the shot; its deflection, if it survives, falls to the floor, away from him). `live=0` | an arrow's hit raises `EntityDamageByEntityEvent` cause `PROJECTILE` (INFERRED: the arrow's `onHitEntity` → `hurtOrSimulate`, not opened for this gate) | **NOT AS PREDICTED.** `CMD /execute at @e[tag=sh3_shot,limit=1] run summon arrow …` and `REPLY "Summoned new Arrow"`, then **no destroyed line**: `SHOT hit … age=33 at=-99.088 64.705 5.500 flown=6.600 victim=player <Ben> dealt=40.000` and `HURT before=40.000 after=36.364 defense=10.000 …`. The row's `kill @e[type=arrow,distance=..16]` replied `"No entity was found"`, so the arrow had left 16 blocks: it rose past the shot without a hit. **Cause UNREAD** (the arrow's entity-hit test was not opened for this gate, S2 marks the path INFERRED). **A vanilla arrow destroying a shot is UNWITNESSED.** He took one 40, survivable as the arithmetic allowed |

### SH4 — one ability burst destroys it: the solar grenade cast at it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The `DRIVE aim` line at the shot; the `CMD /rpg cast solar_grenade` line; then **`[Rpg] SHOT destroyed … by=ability dealer=<Ben> element=fire amount=<a4>`** within the 20 ticks before the `probe_shot`, at an age from 33 to 52 (before its hit at 53). **No HURT line in the row.** `live=0` | the grenade's `on_hit` has no direct damage: its burst (radius 4, `damage 6 fire`) reaches the shot through `combatantsNear` → the handle → `strike` (S2). The Phalanx (12 east) is more than 4 from the shot at every age before 53 except the first 20, so it may take the burst too; that is recorded, not predicted | **AUTO PASS**: `DRIVE aim … target=<S> bat d=6.205`; `CMD /rpg cast solar_grenade`; `SHOT destroyed … age=36 at=-95.694 64.812 5.500 flown=7.200 by=ability dealer=<Ben> element=fire amount=8.000` (age within 33–52); no HURT; `live=0`. **AS PREDICTED** |

### SH5 — one area effect that catches it destroys it: the grenade's field (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The `DRIVE aim_block` and `CMD /rpg cast solar_grenade` lines; the shot fired 30 ticks later; then **`[Rpg] SHOT destroyed … by=ability dealer=<Ben> element=fire amount=<a5>`**, at an age from 27 to 52, **`a5` less than SH4's `a4`** (the field's authored 2 against the burst's 6, priced by the same chain). **No HURT line in the row.** `live=0` | the field lasts 100 ticks and pulses every 20 over a cube of half-extent 4 round the landing point 3 east (`EffectApplier.tickArea` → `combatantsNear`); the shot is inside it from about x 7 (age ≈ 25) to its hit at 53, more than one pulse's 20; its pulse is ours, so `applyDamage` → `strike` | **AUTO PASS**: `DRIVE aim_block … block=-98,62,5 stone face=UP`; `CMD /rpg cast solar_grenade`; the fire 30 ticks later; `SHOT destroyed … age=33 at=-95.095 64.836 5.500 flown=6.600 by=ability dealer=<Ben> element=fire amount=4.000` (age within 27–52; 4.000 less than SH4's 8.000); no HURT; `live=0`. **AS PREDICTED** |

### SH6 — a shot left to expire, the player moved out of its path (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The fired line; his `CMD /tp @s ~-38 ~ ~ -90 0` 3 ticks after the step; **`[Rpg] SHOT expired … age=176 at=<feet + −27.195…−27.194, 1.680…1.662, 0.000> flown=35.198…35.196 limit=range`** (S4; the first `feet` is where he began the row). No hit, no HURT. After `return`, `live=0` | S4, SH6 table; E1 says why it is range and not life | **AUTO PASS**: `CMD /tp @s ~-38 ~ ~ -90 0`; `SHOT expired … age=176 at=-127.695 64.674 5.500 flown=35.197 limit=range` (rel. −27.195 1.674 0, inside −27.195…−27.194, 1.680…1.662, 35.198…35.196); no hit, no HURT; after `return`, `live=0`. **AS PREDICTED** |

### SH7 — a shot that meets a wall (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`[Rpg] SHOT blocked … age=18 at=<feet + 4.406 1.884 0.000> flown=3.600 block=stone`**. No hit, no HURT. `live=0` | S4, SH7; the block rule is the beam's (`rayTraceBlocks`, `FluidCollisionMode.NEVER`, passable ignored) from the old position to the new | **NOT AS PREDICTED.** `SHOT destroyed … age=17 at=-95.894 64.896 5.500 flown=3.400 by=vanilla cause=SUFFOCATION`, one tick before the predicted `blocked … age=18`. No hit, no HURT, `live=0`. **The code did what it says** (any vanilla damage destroys it); **the prediction missed** that the carrier's BOX (a bat, 0.5 wide, its front 0.25 ahead of the shot's centre) enters the wall before the centre's path does, and a body in a wall takes `IN_WALL` (INFERRED: `LivingEntity#baseTick`'s `isInWall`, not opened). A finding for the seat, not fixed here |

### SH8 — a frozen husk in its path is not hurt (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Two `PROBE` lines for `sh8h`, before and after, **with the same custom HP**; the shot passes through the husk's box (at x 4 the shot is at y ≈ 1.86, inside a husk 1.95 tall) and **hits him at age 33** exactly as SH1 (`victim=player`, `dealt=40.000`), with SH1's HURT line. **No `[Rpg] SHOT` line names the husk** | `MobShot.victim` passes over `Side.MOB` (S3); a mob never changes the flight | **AUTO PASS**: `PROBE tag=sh8h … husk customHP=100.000/100.000 … d=4.000` before and after, the same; `SHOT hit … age=33 at=-99.088 64.705 5.500 flown=6.600 victim=player <Ben> dealt=40.000`; SH1's HURT line; no SHOT line names the husk. **AS PREDICTED** |

### SH9 — a destroyed shot pays nothing, and a small Phage beside it does not grow (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `BODY tag=sh9p … mob=phage stage=1 …` before and after, **the same line**; **no `[Rpg] GROWTH` line in the row**; the two `xp query @s points` REPLY lines the same, and the two `levels` lines the same; SH2's `destroyed … by=vanilla cause=ENTITY_ATTACK` line, struck at about x 5.4, within the Phage's 6-block growth radius (the Phage at 4 east, 1 north). No HURT | S3: no death, so nothing a death does; no coins exist to pay | **NOT AS PREDICTED on one clause, the code right.** `BODY tag=sh9p … mob=phage stage=1 …` before and after the shot's destruction, the same line; `SHOT destroyed … age=14 … by=vanilla cause=ENTITY_ATTACK damager=player <Ben>`; XP `1304 experience points` / `191 experience levels` before and after, the same; no HURT. **But** after the row's `kill @e[tag=sh9]`: `GROWTH mob=phage id=<sh9p> from=1 to=2 fed=wither_skeleton/phalanx <the row's Phalanx> element=void d=4.123 max=100`: **the Phalanx's own death** (a Void mob, 4.123 from the Phage) grew it, as growth is built to. The prediction "no GROWTH line in the row" forgot the row's cleanup kill. **The shot fed nothing**: the second BODY line, read after its destruction and before the kill, is stage 1 |

### SH10 — the live cycle (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Two `[Rpg] SHOT fired … forced=false` lines whose `tick=` differ by exactly 120**, the first about 120 ticks after the row's spawn; each followed by **one `hit`** and its **HURT line for 40 before defence** (its age not predicted: the live Phalanx strafes); **at least three `[Rpg] BOW suppressed mob=phalanx id=<P>` lines**; `ENTITIES ARROW count=0 radius=16` | the clock (`MobShot.Clock`, `theClockAttemptsEvery120Ticks…`); S5; the first attempt is 120 after its add (as the Knell's I3), and it fires because he is within 30 (eye to eye, ≈ 10) | **AUTO PASS**: `SHOT fired … forced=false tick=1747 …` and `tick=1867`: **120 apart**; each hit (`age=44`, `age=45`, `dealt=40.000`) with its HURT (40.000 / 36.364); **six** `BOW suppressed` lines (ticks 1650, 1710, 1770, 1830, 1890, 1950); `ENTITIES ARROW count=0 radius=16`. **AS PREDICTED** |

### SH11 — no arrow in a bow's window, before its clock (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **At least one `[Rpg] BOW suppressed` line** in the 100 ticks; **no `[Rpg] SHOT fired` line** (its first attempt is 120 after it appears); `ENTITIES ARROW count=0 radius=16`. No HURT | S5: once it targets him it releases about 20 ticks into its first draw; 100 ticks leaves its target goal 80 to find him | **AUTO PASS**: two `BOW suppressed` lines (ticks 2024, 2084); no `SHOT fired`; `ENTITIES ARROW count=0 radius=16`; no HURT. **AS PREDICTED** |

### SH12 — after its Phalanx is killed, no carrier is left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The fired line; a `SHOT at …` read (age about 9, S4's 12-east figures); the `CMD /kill @e[tag=sh12]`; then **`[Rpg] SHOT ended … reason=owner-gone`**; **`ENTITIES BAT count=0 radius=16`** and **`ENTITIES BLOCK_DISPLAY count=0 radius=16`** | the Phalanx's loop stops the tick it is dead (`isActive`), and its `onStop` ends every shot it owns, removing the bat and the display | **AUTO PASS**: the fired line (tick 2133); `SHOT ended … age=9 at=-90.299 65.028 5.500 flown=1.800 reason=owner-gone`; `ENTITIES BAT count=0 radius=16`; `ENTITIES BLOCK_DISPLAY count=0 radius=16`. (The `probe_shot` after the 10-tick wait read `live=0`: its read ran after the kill's death had ended the shot, so no `SHOT at` line, the one predicted read not seen.) **AS PREDICTED on the ending and the counts; NOT AS PREDICTED on the in-flight read**: the `probe_shot` runs on the Phalanx's thread a tick later, and the `kill` in the same tick had already ended the shot |

---

## BEN'S ROWS — BY THE ASK STEP, SURVIVAL, DIFFICULTY NORMAL

**The rule:** an AUTO half never makes these rows pass (`verification.md`, *AN AUTO READING IS A READING OF THE SERVER*):
the colour, the flashes, whether he could see it and whether it feels fair are his.

### SQ1 — see it, hit it, fair (SURVIVAL)

| AUTO half | Ben's half | READING |
|---|---|---|
| Two fired lines from `sq1`; the first ends in a `hit` and its HURT (he lets it hit him); the second ends in `destroyed … by=vanilla cause=ENTITY_ATTACK damager=player <Ben>` if he hit it, else a `hit` | `SQ1 ASK 1 question="Could you see the shot coming (light purple, with pink flashes)?" answer=…`; `ASK 2 question="Did hitting it destroy it?" answer=…`; `ASK 3 question="Does the Phalanx's shot feel fair now?" answer=…` | READ: shot 1 `SHOT hit … age=68 … dealt=40.000`, HURT 40.000 / 36.364. **Shot 2 ALSO `SHOT hit … age=68 … dealt=40.000`**, HURT 40.000 / 36.364: **no destroyed line**, so no swing of his reached it on the server. Ben, verbatim: `SQ1 ASK 1 question="Could you see the shot coming (light purple, with pink flashes)?" answer="yes" ticks=90`; `SQ1 ASK 2 question="Did hitting it destroy it?" answer="yes" ticks=88`; `SQ1 ASK 3 question="Does the Phalanx's shot feel fair now?" answer="yes" ticks=78`. **His ASK 2 "yes" and the server disagree**; recorded as both said, not re-asked |

### KQ1R6 — supersedes GATE-dragon-knell-5.md KQ1R5's flash: the second purple line is gone (SURVIVAL)

| AUTO half | Ben's half | READING |
|---|---|---|
| `GATE-dragon-knell-5.md`'s forced-beam lines from a frozen Knell 12 east: the two telegraph sounds and the TELEGRAPH at T; the two fire sounds and the FIRE at T + 24; its hit and HURT for 65 before defence. **The flash is drawn by the client and logged nowhere**, so the AUTO half cannot see it | `KQ1R6 ASK 1 question="Is the second purple line gone?" answer=…` | READ: `BEAM telegraph … tick=2839 …`, `BEAM fire … tick=2863 … tested=1 [player <Ben> gap=0.000 hit]`, `HURT before=65.000 after=59.091 defense=10.000 … dealerType=wither_skeleton/knell`, and its move `TELEPORT … tick=2887` (24 after the fire, the production draw). Ben, verbatim: `KQ1R6 ASK 1 question="Is the second purple line gone?" answer="yes" ticks=113`. **The flash ruled removed: CONFIRMED BY PLAY** |

---

# RE-PREDICTED AFTER THE BOOT OF `8ec16e89` (the seat's rulings on #201, 2026-11-17) — NOT RUN

**Status: NOT RUN.** Everything below was written **before** any boot of the code it predicts. The predictions and
readings ABOVE are the boot of `8ec16e89`'s and are **not edited**. Rows that failed or whose lines change are
re-predicted here **under their old name with the next suffix**. SH13-SH16 are new. **Not re-run**, because they read as
predicted and their lines do not change: **SH5, SH6, SH8, SH10, SH11, KQ1R6**. They are kept in the same file because
the readings above are the evidence for the diagnosis below. The scenario file now carries only this section's rows.

> **READ, 2026-11-17: booted at `3dc0e184` on the seat's approval and Ben's "ready".** Server log `10:27:44` to the clean
> `/stop` at `10:30:09`. Carried state: no JVM; backup `rpg-run-backups/2026-11-17-pre-dragon-shot-r2`
> (`run/plugins/Rpg` 129 files, 391492 bytes; `run/world` 193 files, 204963748 bytes; `diff -rq lines: 0` each);
> `12:  dev-pack-on-join: true`. Ben logged in at `10:27:47`, ran `/rpg selftest dragon-shot` once (`10:28:10`) to
> `SELFTEST RUN FINISHED` (`10:29:09`): `DIFFICULTY found=NORMAL set=NORMAL`, `RESTORED … gamemode=SURVIVAL … difficulty=NORMAL`.
>
> **THE DEPARTURES, recorded as they happened:**
> - **He did not type `/gamemode survival`.** The run's restore put back `gamemode=SURVIVAL`, the mode it found at RUN
>   START, so every row ran in survival as declared.
> - **After the run, off the sheet**, he typed `/rpg spawn phalanx` (`10:29:15`) and played against a LIVE Phalanx for
>   50 seconds, then `/kill @e[type=minecraft:wither_skeleton]` (`10:30:04`) and `/stop`. Its lines are not this
>   gate's and none is read as a row. Recorded because one bears on F14: a shot that chased him as he moved
>   **hit at `age=184`** (`shot=fe96b267…`), so a curving shot does live long.
>
> **The result: R0aR2-R0dR2 MATCH. AS PREDICTED: SH2R2, SH3R2, SH4R2, SH9R2, SH12R2, SH13, SH16. NOT AS PREDICTED:**
> - **SH1R2:** `inside=false`, the shape 0.45 above and 0.4 to the side of the box.
> - **SH7R2:** a leftover grenade field from SH4R2 destroyed the shot before the wall.
> - **SH14:** on one cause only, `CUSTOM` for `PROJECTILE`.
> - **SH15:** the strike never ran.
>
> **SQ1R2: Ben's own swing DESTROYED the shot at age 67, one tick before the reach, and he answered "yes", "yes".**
> Every LEFTOVER read `living=0`; no orphan line; the run's two HURT lines (SH1R2, SH14) are shots. **No line records a
> push** of or by a carrier: the shot's logged positions are the flight's own, set by teleport each tick, and no line
> reads the player's or a mob's motion, so a push is neither seen nor ruled out by the log.

## D1-D3 — THE DIAGNOSIS, EACH WITH ITS EVIDENCE

Every jar read uses `~/.jdks/openjdk-26.0.1/bin/javap.exe -c -p -cp <jar> <class>`. The server jar is
`run/versions/26.1.2/paper-26.1.2.jar`. The client jar is `~/AppData/Roaming/.minecraft/versions/26.1.2/26.1.2.jar`,
unobfuscated.

### D1 — why Ben's swing did not destroy the shot: CANNOT BE TOLD FROM THIS BOOT; what is ruled out, and what a boot must show

| candidate | evidence | mark | verdict |
|---|---|---|---|
| **any event naming his second shot between its fire and its hit** | the log from `09:32:48` (fired, `tick=2483`) to `09:32:51` (hit, age 68): only the fired, SHOTTAG, hit and HURT lines. `RpgListeners.onShotDamaged` logs a `destroyed` line for ANY damage event on a carrier, cancelled or not. **Our code logged no arm swing, attack or interact**, so their absence says nothing | READ (the log) | **no damage event reached the shot.** Whether his client sent an attack is unknowable from this boot |
| **the server refused an attack it received** | `ServerGamePacketListenerImpl#handleAttack`: the world border, then `isWithinAttackRange(item, the server's box, 3.0)`, then `cannotAttackWithItem(item, 5)`, then `ServerPlayer.attack`. **There is no `isPickable` test.** `attack` reaches the damage event we log | READ | possible only by **reach** (his eye to the SERVER box, more than 3.0) or **charge** (a re-swing inside 5 ticks of the last). Both are unread |
| **the client cannot pick it** | client `LivingEntity#isPickable` = `!isRemoved()` (offsets 0-12). The client's `LocalPlayer#pick` filters entities with `EntitySelector.CAN_BE_PICKED`, over the box `expandTowards(view × reach).inflate(1)`. No invisibility filter | READ (client jar) | **ruled out:** an invisible, NoAI carrier is pickable on the client |
| **what he sees against what he can hit** | the bat's box was 0.5 × 0.9. The 0.4 block was centred in it from the PASSENGER rule `(0, height, 0)` (READ for a bat, `EntityType`) | READ / INFERRED (the client positions a rider by the same `Entity#positionRider`) | **ruled out for the bat.** For the FROG it would NOT hold: its passenger attachment is `(0, 0.375, −0.25)` (READ, `EntityType` frog `passengerAttachments`). Hence the calibration below |
| **the client's box lags the server's** | a bat's `updateInterval` is the Builder's default **3** (READ, `EntityType$Builder.<init>`: `iconst_3`, `putfield updateInterval`; `bat` sets none). `ServerEntity#sendChanges` sends a position when `tickCount % updateInterval == 0`, or on `needsSync`, a dirty data set or a forced resync (READ, offsets 301-340) | READ (the send) / INFERRED (the client's interpolation between sends) | the client's box trails the server's by up to 3 ticks, **0.6 blocks**. The display rides the CLIENT's box, so what he sees stays inside what his client can pick. The server's box is NEARER him for an incoming shot, so the server's reach test is not made harder by the lag. **Not fixed: no API forces a per-tick position send**; the swing log measures it |
| **the window** | from 15 east the bat's near face (0.25 ahead of the centre) comes within his 3.0 at about age 59 (the centre at x 3.25, 0.1999 a tick), and the shot hits at 68: about **9 ticks, 0.45 s** (est., from the line's geometry, not executed) | INFERRED | a tight window; ruling 2's 0.8 box (its face 0.4 ahead) widens it by about 1 tick |
| **the order in a tick** | packets run as tasks on the main thread before the next tick's world work (`PacketUtils.ensureRunningOnSameThread`) | INFERRED | an attack that arrives before a tick is processed before that tick's step. Pinned in our code below |

**What a boot must show to tell:** with `watch_swings` on, one `SWING kind=arm` line per swing he makes, carrying
`along=` (his look's entry distance into the SERVER box) or `miss` and `gap=`. Then:
- an `attack` line naming the shot means his client sent an attack and the server passed the reach and charge tests;
- a `SHOT destroyed` line means it reached the damage event.

The reading is: **arm swings with `miss`** = his look did not pass through the server box (aim, or lag); **`along` ≤ 3
with no attack line** = his client did not pick it; **an attack line with no `destroyed`** = our code.

### D2 — the arrow (SH3): A DEFECT OF OURS, FIXED

The staging was sound. The arrow was summoned 0.8 under the carrier's feet, rising 1.5 a tick, and its swept, inflated
box met the bat's on the next tick. **The arrow could not hit the carrier at all.** `Projectile#canHitEntity` →
`Entity#canBeHitByProjectile` = `isAlive() && isPickable()`, and the server's `LivingEntity#isPickable` = `!isRemoved()
&& this.collides` (offsets 1-19; READ). `collides` is the field Bukkit's `LivingEntity#setCollidable` sets, and
`MobShooter` called `b.setCollidable(false)`. **Fixed:** the call is gone, pinned by `DragonShotWiringTest`
(killed by MUTCOLLIDE).

### D3 — suffocation (SH7): CONFIRMED, and the other causes

The destroyed line read `by=vanilla cause=SUFFOCATION`. The carrier's box front, 0.25 ahead of its centre, entered the
wall at age 17. A living body's in-wall damage is `LivingEntity#baseTick`'s `isInWall` → `IN_WALL`, Bukkit's
`SUFFOCATION` (INFERRED: the cause in the log is the reading; the bytecode branch was not opened). **Every other cause
that could reach the carrier, and what each did at `8ec16e89`:** all of them destroyed it, because any damage did.
- the void: `VOID`;
- cramming: `CRAMMING`;
- a falling block: `FALLING_BLOCK`;
- fire: `FIRE`, `FIRE_TICK`;
- lava: `LAVA`;
- drowning: `DROWNING`;
- freezing: `FREEZE`;
- lightning: `LIGHTNING`;
- the world border: `WORLD_BORDER`;
- `/kill`: `KILL`.

**Now:** every one is REFUSED except `/kill`, which ENDS it (ruling 1).

## WHAT CHANGED FOR EACH RULING

| ruling | as built | where | pinned by |
|---|---|---|---|
| 1, only attacks destroy it | vanilla damage on a carrier is always cancelled, then `ShotDamage.classify(cause name, byCombatant)`. **ATTACK**: `ENTITY_ATTACK`, `ENTITY_SWEEP_ATTACK`, `PROJECTILE`, `ENTITY_EXPLOSION`, `MAGIC` or `SONIC_BOOM`, each only if the damage source's CAUSING entity is living. It is destroyed. **END**: `KILL`, which ends it `reason=kill`. **REFUSED**: everything else, logged once per cause per shot (`SHOT refused … cause=`). Our ability paths are attacks at the handles, as before | `core.mob.ShotDamage`, `RpgListeners.onShotDamaged`, `MobShooter.refuse` | `ShotDamageTest` (MUTCOMBATANT, MUTKILL); `DragonShotWiringTest` (MUTREFUSESTRIKE) |
| 1, the body never enters a block | `blocked` is decided by the carrier's BOX at the old centre joined with its box at the new, against every block's COLLISION shape (`Block#getCollisionShape`, strict `overlaps`), before the move. The centre's ray is gone | `MobShooter.swept`, `firstColliding` | `DragonShotWiringTest` |
| 2, what he sees is what he can hit | `shot.hit_box: 0.8` (BLOCKS, the hittable cube's edge, at least `look.size`). The carrier is a **FROG** (a 0.5 cube, READ) given that box by `Attribute.SCALE` = 0.8 / 0.5 = **1.6**. The display's translation is set from the frog's READ passenger attachment, then **re-measured on the server** the tick after it rides and corrected, so the shape's centre is the box's centre. **The shot's own hit test is unchanged** (its centre against the target's box grown by 1.2), so it is not easier for the shot. The block sweep uses the bigger box, so a wall stops it sooner | `phalanx.yml`, `MobShot.ShotLook.hitBox`, `MobShooter.fire`, `calibrate` | `MobShotTest.theHittableBoxMustHoldTheShape` (MUTHITBOX), `MobLoaderShotTest`; the `inside=` field of every `SHOT at` line |
| 2, the lag | not fixed. No API forces a per-tick position send, and the display rides the client's box, so what he sees is inside what his client picks (D1) | | the SWING lines |
| 3, the order in a tick | `strike` ends the shot at once, and `advance` returns first for an ended shot, so an attack processed before a step wins over that step's reach. A packet is processed before the tick it precedes (INFERRED, D1). SH15 reads a strike in the last tick before the reach | `MobShooter.advance` | `DragonShotWiringTest` (MUTORDERTICK2) |
| 4, the instrument | `watch_swings: on/off`: every arm swing, attack and interact of the run's player, each logged against the nearest live shot with `along`/`miss`, `gap`, eye and look, and one tick later `SWING after … live=`. `aim_shape`: his eye to the visible shape. `strike_look`: the server rays ALONG HIS LOOK out to his `ENTITY_INTERACTION_RANGE` and `Player#attack`s the pick. `strike_at_age`: the same, toward the shape, in the tick the shot's age reaches N | `SelfTestStrike`, `SelfTestRun`, `SelfTestDrivers`, `RpgListeners.onShotWatch*` | `SelfTestStrikeTest` (MUTALONG) |
| 5, the arrow | D2's fix; and `shoot_arrow`, a real arrow with the player as its shooter | `SelfTestDrivers.shootArrow` | SH3R2 |
| 6, SH9 | the row never kills its Phalanx. The next row's clean removes it without a death | the scenario | SH9R2 |

## THE FLIGHT FOR THESE ROWS (jshell, 2026-11-17, at homing 0.0513; positions relative to his feet)

From 8 east, the shot's centre and the box and shape:

| age | centre | box (the frog, ±0.4) | shape (the block, ±0.2) |
|---|---|---|---|
| 10 | 6.004 1.980 0.000 | 5.604 1.580 −0.400 .. 6.404 2.380 0.400 | 5.804 1.780 −0.200 .. 6.204 2.180 0.200 |
| 20 | 4.007 1.860 0.000 | 3.607 1.460 −0.400 .. 4.407 2.260 0.400 | 3.807 1.660 −0.200 .. 4.207 2.060 0.200 |
| 32 | 1.611 1.717 0.000 | 1.211 1.317 −0.400 .. 2.011 2.117 0.400 | 1.411 1.517 −0.200 .. 1.811 1.917 0.200 |

**The hit is still at age 33**, at 1.412 1.705 0, because the tolerance test is the shot's centre. **SH7R2:** the
swept box meets the wall block (x 3.5 to 4.5) first at **age 16**, centre 4.806 1.908 0 (its box would span 4.406 to
5.206). **SH16's reach:** his eye (0, 1.62, 0) to the box's near face along his look is about **2.8 at age 24** and
**2.4 to 2.6 at ages 25-26**, inside his 3.0.

## THE PLAYER CANNOT DIE

Each row heals him first. No row lets more than one 40 land after a heal. The rows where a shot can hit him are
SH1R2, SH14 and SQ1R2, plus any row whose destroying step fails. 40 < 100.

## R0 FOR THIS BOOT

### R0aR2 — the build line names this branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-shot` prints it, shortened; not `8ec16e89`, not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | READ 2026-11-17: `[10:27:44] [Server thread/INFO]: [Rpg] Build: 3dc0e184`, this PR's head (`git ls-remote` `3dc0e184411f…`). **MATCH** |

### R0bR2 — the jar carries the frog, the box and the refusal

| prediction | instrument | READING |
|---|---|---|
| `phalanx.yml` PRESENT `  hit_box: 0.8` and `  homing: 0.0513`. Classes PRESENT `core/mob/ShotDamage.class` (in the shaded jar under `io/github/butterflysmp/rpg/core/mob/`) and `paper/selftest/SelfTestStrike.class`. `GATE-dragon-shot.yml` PRESENT `SH7R2:`, `SH16:`, `SQ1R2:`, ABSENT `SH7:`. **Controls, the same on both builds:** `phalanx.yml` PRESENT `  life_ticks: 240`; `health/MobShooter.class` PRESENT | R0b's scan above, with these names | READ: `hit_box: 0.8`, `homing: 0.0513`, `life_ticks: 240` PRESENT; `ShotDamage.class`, `SelfTestStrike.class`, `MobShooter.class` PRESENT; `SH7R2:`, `SH16:`, `SQ1R2:` PRESENT, `SH7:` ABSENT. **MATCH** |

### R0cR2 — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| The boot of `8ec16e89`'s Loaded line, verbatim (`… 5 mobs, 1 mob elements …`); no `Refusing`, `Skipping`, `SEVERE` or exception; no `[Rpg]` WARN beyond `volley_stone`'s pair | as R0c above | READ: the Loaded line verbatim; no `Refusing`, `Skipping`, `SEVERE` or exception; the only `[Rpg]` WARN lines `volley_stone`'s pair. **MATCH** |

### R0dR2 — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Selftest: dev server, 129 scenarios across 18 gates [… dragon-teleport, dragon-shot]`**, no `Refusing selftest` | as R0d above | READ: `[Rpg] Selftest: dev server, 129 scenarios across 18 gates [… dragon-teleport, dragon-shot]`; no `Refusing selftest`. **MATCH** |

**The 129 is RECOUNTED from the files:** `grep -cE '^  - ' index.yml` prints 18. Summed over the eighteen files,
`grep -cE '^[A-Za-z0-9-]+:'` prints 147 and `'^setup:'` prints 18: 147 − 18 = 129. At `ef242e95` the same three
commands print 18, 149, 18: 131.

## THE ROWS (AUTO; SURVIVAL, DIFFICULTY NORMAL; every row `LEFTOVER living=0 radius=16`)

**Every forced shot** logs `SHOT fired … dealt=40.000` and a `SHOTTAG … tags=[<tag>_shot, selftest, t_shot]`, with
exactly one ending line. **No `SHOT refused` line appears in any row but SH14**: no row stages a vanilla
non-attack.

### SH1R2 — supersedes SH1: the flight, with the box and the shape as numbers (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Two `SHOT at … age=<a> at=<centre> flown=<0.2a> v=… box=<centre ± 0.4> shape=<centre ± 0.2> inside=true`, at about ages 9-11 and 19-21, each matching the table to three places at its own age; the third read `live=0`; `SHOT hit … age=33 at=<feet + 1.412 1.705 0.000> flown=6.600 … dealt=40.000` and its HURT | the flight table; `calibrate` (the shape read from the display's server position plus its translation) | **NOT AS PREDICTED on `inside=`.** Feet `-100.5 63 6.5`. `SHOT at … age=10 at=-94.496 64.980 6.500 flown=2.000 v=-0.200 -0.012 0.000 box=-94.896 64.580 6.100..-94.096 65.380 6.900 shape=-94.696 65.230 5.900..-94.296 65.630 6.300 inside=false`; `age=20 at=-96.493 64.860 6.500 flown=4.000 … box=-96.893 64.460 6.100..-96.093 65.260 6.900 shape=-96.693 65.110 5.900..-96.293 65.510 6.300 inside=false`. **The centre and the box are as predicted to three places** (rel. 6.004 1.980 0 and 4.007 1.860 0; box ±0.4). **The shape is not centred:** its centre is 0.450 ABOVE the box's and 0.400 toward −z, so its top 0.25 and its −z face 0.2 lie outside the box. The calibration did not take; the shape is where the first translation (from the READ attachment, scaled) put it against a real rider offset of about (0, 1.05, −0.80) (derived from the reading, not measured directly). The hit: `SHOT hit … age=33 at=-99.088 64.705 6.500 flown=6.600 … dealt=40.000` and its HURT, **as predicted**. Cause of the missed calibration UNREAD |

### SH2R2 — supersedes SH2: Player#attack on the known shot, the carrier now a frog (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `DRIVE attack Player#attack target=<S> frog d=…`; `SHOT destroyed … age=<12..15> … by=vanilla cause=ENTITY_ATTACK damager=player <Ben>` (no `causer=`: the damager is the causer); no HURT | SH2's reading; ruling 1's ATTACK | **AUTO PASS**: `DRIVE attack Player#attack target=<S> frog d=5.426 charge=1.000`; `SHOT destroyed … age=14 at=-95.295 64.932 6.500 flown=2.800 by=vanilla cause=ENTITY_ATTACK damager=player <Ben>` (no `causer=`); no HURT. **AS PREDICTED** |

### SH3R2 — supersedes SH3: a real arrow, the player its shooter (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `DRIVE shoot_arrow tag=sh3r2_shot arrow=<A> shooter=<Ben> d=<about 5>`; `SHOT destroyed … age=<15..20> … by=vanilla cause=PROJECTILE damager=arrow <A> causer=player <Ben>`; no HURT | D2 (pickable again); `Projectile#canHitEntity` (READ); ruling 1's ATTACK (a projectile a combatant fired) | **AUTO PASS**: `DRIVE shoot_arrow tag=sh3r2_shot arrow=f7737ef9-288c-4089-9dfd-22e4f2fc51be shooter=<Ben> d=5.276`; `SHOT destroyed … age=17 at=-95.894 64.896 6.500 flown=3.400 by=vanilla cause=PROJECTILE damager=arrow f7737ef9-288c-4089-9dfd-22e4f2fc51be causer=player <Ben>`; no HURT. **AS PREDICTED: D2's fix read** (a vanilla arrow destroys the shot) |

### SH4R2 — supersedes SH4: the grenade cast at the shot, the carrier now a frog (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `DRIVE aim … frog d=…`; `SHOT destroyed … age=<33..52> … by=ability dealer=<Ben> element=fire amount=8.000`; no HURT | SH4's reading (`amount=8.000`) | **AUTO PASS**: `DRIVE aim … target=<S> frog d=6.205`; `SHOT destroyed … age=36 at=-95.694 64.812 6.500 flown=7.200 by=ability dealer=<Ben> element=fire amount=8.000`; no HURT. **AS PREDICTED** |

### SH7R2 — supersedes SH7: a wall ends it BLOCKED, the body outside (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`SHOT blocked … age=16 at=<feet + 4.806 1.908 0.000> flown=3.200 block=stone`**; no `refused` line; no HURT | the swept box (table); `Block#getCollisionShape` | **NOT AS PREDICTED, AND THE ROW DID NOT PRESENT ITS CONDITION** (a hollow fixture in TIME). `SHOT destroyed … age=9 at=-94.297 64.992 6.500 flown=1.800 by=ability dealer=<Ben> element=fire amount=2.000`, before the wall at age 16. An ability of HIS, fire, with no ability cast in this row: INFERRED to be **SH4R2's grenade FIELD (100 ticks, cast 4 seconds earlier at the same spot) pulsing into this row** (the amount alone does not identify it: the first boot's SH5 field read 4.000), and the row-start clean removes tagged mobs and blocks, not an ability's area. No `blocked` line, no `refused` line, no HURT. **The swept-box wall is UNREAD.** My staging defect: SH7R2 followed SH4R2 directly; at `8ec16e89` SH7 followed SH6's 10 seconds |

### SH9R2 — supersedes SH9: a destroyed shot feeds nothing, the Phalanx never killed (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `BODY tag=sh9r2p … stage=1 …` before and after, the same; the XP replies the same before and after; `SHOT destroyed … by=vanilla cause=ENTITY_ATTACK …`; **no `[Rpg] GROWTH` line in this row or in the next row's bracket** (the clean removes the Phalanx with `remove()`, no death) | S3 above; SH9's reading | **AUTO PASS**: `BODY tag=sh9r2p … stage=1 …` before and after, the same line; XP `1304 experience points` and `191 experience levels`, before and after the same; `SHOT destroyed … age=14 … by=vanilla cause=ENTITY_ATTACK damager=player <Ben>`; **no `GROWTH` line anywhere in the run** (and none in SH12R2's bracket, where the clean removed the Phalanx); no HURT. **AS PREDICTED** |

### SH12R2 — supersedes SH12: after its Phalanx is killed, no carrier is left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `SHOT ended … reason=owner-gone`; **`ENTITIES FROG count=0 radius=16`** and **`ENTITIES BLOCK_DISPLAY count=0 radius=16`** | SH12's reading | **AUTO PASS**: `SHOT ended … age=9 at=-90.299 65.028 6.500 flown=1.800 reason=owner-gone`; `ENTITIES FROG count=0 radius=16`; `ENTITIES BLOCK_DISPLAY count=0 radius=16`. **AS PREDICTED** |

### SH13 — /kill ends it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `CMD /kill @e[tag=sh13_shot]`; **`SHOT ended … reason=kill`**; no `destroyed`, no hit, no HURT; `live=0` | `LivingEntity#kill` hurts with `genericKill` (READ, `RpgListeners.onMobDeath`'s javadoc, javap build 74); its cause is `KILL`; ruling 1's END | **AUTO PASS**: `CMD /kill @e[tag=sh13_shot]`; `SHOT ended … age=9 at=-94.297 64.992 6.500 flown=1.800 reason=kill`; no destroyed, no hit, no HURT; `live=0`. **AS PREDICTED** |

### SH14 — environmental damage is refused, once per cause (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Four `SHOT refused … cause=<c>` lines, one each for **SUFFOCATION, FIRE, CRAMMING, PROJECTILE**, in that order and **SUFFOCATION once though it is dealt twice**; no `destroyed`; **`SHOT hit … age=33`** and its HURT for 40 | `/damage` with no source has no causing entity (INFERRED); the damage types `in_wall`, `in_fire`, `cramming` and `arrow` map to those causes (INFERRED, the mapping not opened); ruling 1's REFUSED | **NOT AS PREDICTED on one cause.** `SHOT refused … age=4 … cause=SUFFOCATION`, `cause=FIRE`, `cause=CRAMMING`, then **`cause=CUSTOM`** (predicted `PROJECTILE`: the INFERRED mapping of the ownerless `minecraft:arrow` damage type was wrong; it is refused either way); SUFFOCATION once though dealt twice; no destroyed; `SHOT hit … age=33 … dealt=40.000` and its HURT 40.000 / 36.364. **The refusal and the once-per-cause log: AS PREDICTED** |

### SH15 — a strike in the last tick before the reach wins (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `DRIVE strike_at_age tag=sh15_shot age=32 waited=<w>`; `DRIVE strike_at_age … picked=<S> frog at=<about 1.2>`; **`SHOT destroyed … age=32 … by=vanilla cause=ENTITY_ATTACK damager=player <Ben>`**; no hit, no HURT | `SelfTestRun.strikeAtAge`: whichever of the player's and the Phalanx's tasks runs first in a tick, the strike lands before the shot's next step | **NOT AS PREDICTED: the strike never ran** (a hollow fixture in TIME). `DRIVE strike_at_age tag=sh15_shot no live shot after 0 ticks`, **before** the `SHOT fired … tick=1260` line: the SHOT step's force lands a tick or two later, and `strikeAtAge` gave up on "no live shot" instead of waiting. The shot then `ended … age=19 … reason=owner-gone` at the row's kill. **The last-tick strike is UNREAD.** My defect: no wait between the force and the strike, and a poll that ends on a not-yet-live shot |

### SH16 — the aimed swing (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `DRIVE watch_swings on`; `DRIVE aim_shape … target=<S> shape=<x y z>`; **`DRIVE strike_look tag=sh16_shot reach=3.000 picked=<S> frog at=<2.3..2.7>`**; **`SHOT destroyed … age=<25..27> … by=vanilla cause=ENTITY_ATTACK damager=player <Ben>`**; no HURT. A `SWING kind=attack …` line from the instrument's own attack is RECORDED, not predicted (whether `Player#attack` raises `PrePlayerAttackEntityEvent` was not read) | SH16's reach (the table); his `ENTITY_INTERACTION_RANGE` (3.0 in survival, INFERRED); the ray picks the frog only if the box is where the shape is | **AUTO PASS**: `DRIVE watch_swings on`; `DRIVE aim_shape teleportAsync eye->shape target=<S> shape=-97.092 65.274 6.100 yaw=-96.694 pitch=-10.798`; **`DRIVE strike_look tag=sh16_shot reach=3.000 picked=<S> frog at=2.470`**; `SWING kind=attack target=<S> shot=<S> along=2.470 gap=0.000 eye=-100.500 64.620 6.500 look=0.976 0.187 -0.114`; **`SHOT destroyed … age=26 at=-97.691 64.789 6.500 flown=5.200 by=vanilla cause=ENTITY_ATTACK damager=player <Ben>`**; `SWING after shot=<S> live=false`; no HURT. **AS PREDICTED** (age 26, at 2.470, inside 25-27 and 2.3-2.7). Note: the look aimed at the SHAPE (off-centre, SH1R2) and still entered the 0.8 box |

## BEN'S ROW — BY THE ASK STEP, SURVIVAL

### SQ1R2 — supersedes SQ1's second question: did YOUR swing destroy it (SURVIVAL)

| AUTO half, recorded beside his answers | Ben's half | READING |
|---|---|---|
| Every `SWING kind=arm …` line he makes during the shot's flight, with `along`/`miss` and `gap`; any `SWING kind=attack target=<S>` line; the one ending line, `SHOT destroyed … by=vanilla cause=ENTITY_ATTACK damager=player <Ben>` or `SHOT hit … age=68` and its HURT. **Not predicted: it is his swing** | `SQ1R2 ASK 1 question="Did your swing destroy it before it reached you?" answer=…`; `SQ1R2 ASK 2 question="Was it easy enough to hit?" answer=…` | READ, in order by tick: `SHOT fired … tick=1452 shot=4105c217-d524-435a-a66b-63cc025733a5 … from=-85.500 65.100 6.500 aim=-100.500 64.620 6.500 dealt=40.000`; then `SWING kind=attack target=4105c217-… shot=4105c217-… along=1.265 gap=0.000 eye=-100.500 64.620 6.500 look=0.954 0.238 -0.180`; then **`SHOT destroyed … age=67 at=-98.893 64.671 6.500 flown=13.400 by=vanilla cause=ENTITY_ATTACK damager=player <Ben>`**; then `SWING kind=arm shot=none` (the arm event after the attack, the shot already gone); then `SWING after shot=4105c217-… live=false`. No HURT. Ben, verbatim: `SQ1R2 ASK 1 question="Did your swing destroy it before it reached you?" answer="yes" ticks=37`; `SQ1R2 ASK 2 question="Was it easy enough to hit?" answer="yes" ticks=66`. **D1's case: DESTROYED.** His own swing's look entered the server box at 1.265 and his attack destroyed the shot at age 67, ONE tick before the reach at 68. **His answer and the server agree.** The swing came late in the window: one swing, one attack, no misses logged |

## CARRIED STATE AND THE RUN SHEET

Carried state as above (the backup's folder suffix `-pre-dragon-shot-r2`). **The run sheet. Ben types these, in this
order; every line is literal:**

1. `/gamemode survival`, alone, the first line after login.
2. `/rpg selftest dragon-shot`, **ONCE. Wait for the chat to say the run finished, and answer both gold questions.**
   About **1 minute** plus your answers (its waits sum to 0.65 minutes, and one step waits about 1.6 seconds more for a shot to reach an age). Stand still and touch nothing until the gold
   text asks. **At the end** a Phalanx fifteen blocks east fires one shot at you, with a sword in your hand: **hit it
   before it reaches you.** Two questions; answer `yes` or `no`.
3. `/stop`

**Nothing else is run in this boot.**
