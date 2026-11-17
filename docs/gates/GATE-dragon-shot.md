# GATE — THE PHALANX'S SHOT (STEP R7a): DESTROYABLE, HOMING, LIGHT PURPLE; THE BOW IT NEVER FIRES; THE KNELL'S ONE FLASH

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-shot`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

> **REWRITTEN BEFORE ANY BOOT, 2026-11-17, on the seat's ruling on #201 (the homing matches the old game).** The homing
> moved from 0.1 to **0.0513 a tick** (`phalanx.yml`; the plan's 5.R.9 X-c), so the flight was re-run in jshell at the
> new value and every prediction resting on it re-checked. **Changed:** S4's expression; SH6's `at` and `flown` (its
> age, 176, did not move); E1's straight figure and turning radius; SH10's second wait, 100 to 140, so its second hit
> does not rest on a thin margin; the run's length. **Unchanged, re-run and the same to three places:** SH1, SH4, SH5,
> SH7, SH8, SH12 and SQ1, whose targets stand dead ahead, so the homing has nothing to turn. The gate had been neither
> booted nor read, so no reading is touched.

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
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-shot` prints it, shortened. **Not** master's `8365e7f6`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1`, after this boot's `Done` | |

### R0b — the jar carries the shot, the bow and the one flash

| prediction | instrument | READING |
|---|---|---|
| `content/mobs/phalanx.yml` PRESENT `main_hand: bow`, `  period_ticks: 120`, `  life_ticks: 240`, `  max_distance: 35`; `content/mobs/knell.yml` PRESENT `  flash_at: [0]`, ABSENT `  flash_at: [0, 18]`; classes PRESENT `health/MobShooter.class`, `health/MobShotLines.class`, `adapter/ShotSink.class`; `GATE-dragon-shot.yml` PRESENT `SH1:`, `SQ1:`, `KQ1R6:`. **The controls, the same on both builds:** `knell.yml` PRESENT `  telegraph_ticks: 24`; `scheduler/InlineScheduler.class` PRESENT; `GATE-dragon-beam.yml` PRESENT `KQ1R5:` | the scan below | |

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
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim the boot of `07d06837`'s (this branch adds no file, it changes two). No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN` beyond `volley_stone`'s known pair. **A `Skipping malformed mob 'phalanx.yml'` line would mean the shot block was refused** (`4 mobs`); `'knell.yml'` the flash | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 131 scenarios across 18 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot]`**, and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | |

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
| The fired line with `from=` = his feet + (8, 2.100, 0) and `aim=` = his feet + (0, 1.620, 0). Three `probe_shot` reads, 10, 20 and 50 ticks after the `shot:` step: the first two `SHOT at … age=<a> at=<x y z> flown=<f>` each matching S4's table AT ITS OWN AGE to three places (the ages are the runner's: about 9 and 19); the third `live=0`. **One `SHOT hit … age=33 at=<feet + 1.412 1.705 0.000> flown=6.600 … dealt=40.000`**, then the HURT line for 40 before defence | S4 (jshell); `MobShot.step`, `ending`, `victim`; the hit is the one priced by `MobScaling.attackDamage(40, true, true, 100)` through the two-argument `applyDamage`, the beam's final call | |

### SH2 — one melee hit with the emberblade destroys it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `DRIVE attack Player#attack target=<S> bat …`, then **`[Rpg] SHOT destroyed mob=phalanx id=<P> shot=<S> age=<a> … by=vanilla cause=ENTITY_ATTACK damager=player <Ben>`**, `a` from 12 to 15 (the attack is 15 ticks after the step; the fire is 1 or 2 ticks after it), its `at` S4's table at that age. **No HURT line in the row.** The `probe_shot` reads `live=0` | `Player#attack` → `Bat#hurtServer` → `LivingEntity#hurtServer` → the event (S1); `onShotDamaged` LOWEST cancels and strikes | |

### SH3 — one vanilla arrow destroys it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The `CMD /execute at @e[tag=sh3_shot …] run summon arrow …` line; then **`[Rpg] SHOT destroyed … by=vanilla cause=PROJECTILE damager=arrow <uuid>`**, age from 13 to 17. **No HURT line in the row** (the arrow rises through the shot; its deflection, if it survives, falls to the floor, away from him). `live=0` | an arrow's hit raises `EntityDamageByEntityEvent` cause `PROJECTILE` (INFERRED: the arrow's `onHitEntity` → `hurtOrSimulate`, not opened for this gate) | |

### SH4 — one ability burst destroys it: the solar grenade cast at it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The `DRIVE aim` line at the shot; the `CMD /rpg cast solar_grenade` line; then **`[Rpg] SHOT destroyed … by=ability dealer=<Ben> element=fire amount=<a4>`** within the 20 ticks before the `probe_shot`, at an age from 33 to 52 (before its hit at 53). **No HURT line in the row.** `live=0` | the grenade's `on_hit` has no direct damage: its burst (radius 4, `damage 6 fire`) reaches the shot through `combatantsNear` → the handle → `strike` (S2). The Phalanx (12 east) is more than 4 from the shot at every age before 53 except the first 20, so it may take the burst too; that is recorded, not predicted | |

### SH5 — one area effect that catches it destroys it: the grenade's field (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The `DRIVE aim_block` and `CMD /rpg cast solar_grenade` lines; the shot fired 30 ticks later; then **`[Rpg] SHOT destroyed … by=ability dealer=<Ben> element=fire amount=<a5>`**, at an age from 27 to 52, **`a5` less than SH4's `a4`** (the field's authored 2 against the burst's 6, priced by the same chain). **No HURT line in the row.** `live=0` | the field lasts 100 ticks and pulses every 20 over a cube of half-extent 4 round the landing point 3 east (`EffectApplier.tickArea` → `combatantsNear`); the shot is inside it from about x 7 (age ≈ 25) to its hit at 53, more than one pulse's 20; its pulse is ours, so `applyDamage` → `strike` | |

### SH6 — a shot left to expire, the player moved out of its path (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The fired line; his `CMD /tp @s ~-38 ~ ~ -90 0` 3 ticks after the step; **`[Rpg] SHOT expired … age=176 at=<feet + −27.195…−27.194, 1.680…1.662, 0.000> flown=35.198…35.196 limit=range`** (S4; the first `feet` is where he began the row). No hit, no HURT. After `return`, `live=0` | S4, SH6 table; E1 says why it is range and not life | |

### SH7 — a shot that meets a wall (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`[Rpg] SHOT blocked … age=18 at=<feet + 4.406 1.884 0.000> flown=3.600 block=stone`**. No hit, no HURT. `live=0` | S4, SH7; the block rule is the beam's (`rayTraceBlocks`, `FluidCollisionMode.NEVER`, passable ignored) from the old position to the new | |

### SH8 — a frozen husk in its path is not hurt (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Two `PROBE` lines for `sh8h`, before and after, **with the same custom HP**; the shot passes through the husk's box (at x 4 the shot is at y ≈ 1.86, inside a husk 1.95 tall) and **hits him at age 33** exactly as SH1 (`victim=player`, `dealt=40.000`), with SH1's HURT line. **No `[Rpg] SHOT` line names the husk** | `MobShot.victim` passes over `Side.MOB` (S3); a mob never changes the flight | |

### SH9 — a destroyed shot pays nothing, and a small Phage beside it does not grow (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `BODY tag=sh9p … mob=phage stage=1 …` before and after, **the same line**; **no `[Rpg] GROWTH` line in the row**; the two `xp query @s points` REPLY lines the same, and the two `levels` lines the same; SH2's `destroyed … by=vanilla cause=ENTITY_ATTACK` line, struck at about x 5.4, within the Phage's 6-block growth radius (the Phage at 4 east, 1 north). No HURT | S3: no death, so nothing a death does; no coins exist to pay | |

### SH10 — the live cycle (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Two `[Rpg] SHOT fired … forced=false` lines whose `tick=` differ by exactly 120**, the first about 120 ticks after the row's spawn; each followed by **one `hit`** and its **HURT line for 40 before defence** (its age not predicted: the live Phalanx strafes); **at least three `[Rpg] BOW suppressed mob=phalanx id=<P>` lines**; `ENTITIES ARROW count=0 radius=16` | the clock (`MobShot.Clock`, `theClockAttemptsEvery120Ticks…`); S5; the first attempt is 120 after its add (as the Knell's I3), and it fires because he is within 30 (eye to eye, ≈ 10) | |

### SH11 — no arrow in a bow's window, before its clock (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **At least one `[Rpg] BOW suppressed` line** in the 100 ticks; **no `[Rpg] SHOT fired` line** (its first attempt is 120 after it appears); `ENTITIES ARROW count=0 radius=16`. No HURT | S5: once it targets him it releases about 20 ticks into its first draw; 100 ticks leaves its target goal 80 to find him | |

### SH12 — after its Phalanx is killed, no carrier is left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The fired line; a `SHOT at …` read (age about 9, S4's 12-east figures); the `CMD /kill @e[tag=sh12]`; then **`[Rpg] SHOT ended … reason=owner-gone`**; **`ENTITIES BAT count=0 radius=16`** and **`ENTITIES BLOCK_DISPLAY count=0 radius=16`** | the Phalanx's loop stops the tick it is dead (`isActive`), and its `onStop` ends every shot it owns, removing the bat and the display | |

---

## BEN'S ROWS — BY THE ASK STEP, SURVIVAL, DIFFICULTY NORMAL

**The rule:** an AUTO half never makes these rows pass (`verification.md`, *AN AUTO READING IS A READING OF THE SERVER*):
the colour, the flashes, whether he could see it and whether it feels fair are his.

### SQ1 — see it, hit it, fair (SURVIVAL)

| AUTO half | Ben's half | READING |
|---|---|---|
| Two fired lines from `sq1`; the first ends in a `hit` and its HURT (he lets it hit him); the second ends in `destroyed … by=vanilla cause=ENTITY_ATTACK damager=player <Ben>` if he hit it, else a `hit` | `SQ1 ASK 1 question="Could you see the shot coming (light purple, with pink flashes)?" answer=…`; `ASK 2 question="Did hitting it destroy it?" answer=…`; `ASK 3 question="Does the Phalanx's shot feel fair now?" answer=…` | |

### KQ1R6 — supersedes GATE-dragon-knell-5.md KQ1R5's flash: the second purple line is gone (SURVIVAL)

| AUTO half | Ben's half | READING |
|---|---|---|
| `GATE-dragon-knell-5.md`'s forced-beam lines from a frozen Knell 12 east: the two telegraph sounds and the TELEGRAPH at T; the two fire sounds and the FIRE at T + 24; its hit and HURT for 65 before defence. **The flash is drawn by the client and logged nowhere**, so the AUTO half cannot see it | `KQ1R6 ASK 1 question="Is the second purple line gone?" answer=…` | |
