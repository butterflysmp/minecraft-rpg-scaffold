# GATE — THE SKREEL AND ITS MOUNT (STEP R8), AND THE PHALANX'S BLINK COOLDOWN AND ITS CARRIED ITEMS

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-skreel`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

The plan is `PLAN-dragon-mobs.md` (RULINGS, *Ben, 2026-11-17, VERBATIM: the Skreel's numbers* and *the Phalanx's blink
cooldown*; §5.R.11 as built). The scenario file is `paper/src/main/resources/selftest/GATE-dragon-skreel.yml`, run by
`/rpg selftest dragon-skreel`.

**WHAT THIS GATE SUPERSEDES AND CLOSES:**
- `GATE-dragon-fix.md`'s **DB6R2** (the Skreel alone, `ENTITIES SPIDER count=0`) and **DB7R2** (the mount at
  `speedBase=0.3900`, 120 health): a Skreel now spawns riding its mount, and the mount alone has the spider's own speed
  and 160 health. Re-predicted here as **DB6R3** and **DB7R3**, and moved out of `GATE-dragon-bodies.yml`.
- `GATE-dragon-blink.md`'s carried items: **R2** (the cooldown, Ben's, built here: PB1, PB2, PQ2), **R3** (a decision
  from a fractional height: diagnosed and fixed, PB3), **R4** (BC4B's cleanup: fixed in its scenario), **R5** (two
  pushes cancelled, one absorb: READ from the jar below, no row).
- Nothing in those gates is edited but their dated notes.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **Every row is SURVIVAL.** The pair's target and its landing's victims, the blink's trigger and the shot's target all
  skip a creative or spectator player (`MobHostility.hostileTo`), so a creative bench reads none of them: the run
  sheet's first line is `/gamemode survival`.
- **Difficulty NORMAL**, set by the gate's own `setup:`, restored at the run's end.
- **Every mob at GS 100** (the overworld near (0, 0) rolls 100), **except DB6R3 at 300**, where the rolled score (100) and
  the shared one (300) differ, so the row can see the mount take the rider's score (the mutation PSCOREGUARD survived every
  other row).

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC, THE FALL INCLUDED

His custom health floor is **100**; every row heals him first (`rpg heal 1000000`). His defence at the last boot was
**10** (`GATE-dragon-blink.md`: `before=40.000 after=36.364 defense=10.000`, so a hit is cut to 100 / 110 of itself); a
landing's **55** is then **50.000**, a shot's 40 is 36.364.

| row | what can land on him after its heal | before defence | can he die |
|---|---|---|---|
| DB6R3, DB7R3, PS5, PB3 | nothing: every mob is frozen | 0 | no |
| PS3 | one landing | 55 | no: 55 < 100 |
| PS4 | nothing: he stands 4 from the landing, outside its 3 | 0 | no |
| PS6 | the first landing is dodged; he is healed 100 ticks after the force, before the second lands (~169) | 55 | no |
| PS7, PS8 | nothing: no wind-up is forced; the rider on foot (PS7) is frozen 3 ticks after its mount dies, 10 blocks away; the widowed mount (PS8) has no target (below) | 0 | no |
| PB1 | a live Phalanx fires every 120 ticks; he is healed after each shot lands (at +143 and +263 after the blink step; shots land about +130 and +250) | 40 | no |
| PB2 | nothing: the row ends ~22 ticks after the spawn, under its first shot's 120 | 0 | no |
| PQ1 | a live pair's landings, at least **120 ticks apart** (the period, from one wind-up's start to the next's); he is healed every **100**, so at most **one** between heals | 55 | no |
| PQ2 | a live Phalanx's shots, at most two between heals 200 apart (as BQ1) | 80 | no |

**THE FALL, COUNTED.** A landing pushes him **0.4 blocks a tick up** and 0.7 out (× 1 − his knockback resistance, 0 in
his gear). Executed in jshell (2026-11-17), vanilla's air step from a vertical speed of 0.4 (y += v; v = (v − 0.08) ×
0.98f): **the highest he rises is 1.153 blocks**, back to his starting height after 11 ticks. **Fall damage starts past 3
blocks** (vanilla's safe fall distance), so **no fall damage can follow a push on the flat floor every pounce row
stages**, at least 16 blocks of stone round the landing. A rider on foot or a plain mount never reaches him in a row that
does not mean it to: **PS7's rider is frozen 3 ticks after its mount's death**, 10 blocks off; **PS8's mount has no
target in daylight** (`time set 6000`; a spider takes a target in the dark only, INFERRED from vanilla's
`Spider$SpiderTargetGoal`, not read), and even a hit by it is a bare spider's, far under 100.

## THE SKREEL, INHERITED OR CHOSEN — FOR BEN TO SEE

cfde822 is the old game (`PLAN-dragon-mobs.md` §3.5). **READ** = in that record; **NOT IN THE RECORD** = it is not, and
the line says who chose it. **ACCEPTED** = the seat's numbers Ben accepted on 2026-11-17 ("I like your numbers. Let's
start there"), none typed by him.

| # | behaviour | mark | as built |
|---|---|---|---|
| K1 | how the pair is made | READ ("Mounted with `spider.addPassenger(ws)`") | `/rpg spawn skreel` spawns the rider, then its mount at its feet through the same pre-spawn stamp, then `addPassenger`. Each has its own body, health, tags and nameplate; **the mount carries the rider's gear score** (CHOSEN, as a split child carries its parent's) |
| K2 | who steers | READ from the jar (below), not the record | **the rider**: `Mob#getControllingPassenger` is the first passenger when it is a Mob and the vehicle has AI; the rider's goals move the mount through the mount's own navigation and speed |
| K3 | vanilla targeting | READ ("every tick, `setTarget(null)` on both") | **every target event of either half is cancelled while paired** (`RpgListeners.onPairTargeted`): the same effect at its source. Nothing is removed from either mob |
| K4 | who it targets | READ ("the nearest non-creative, non-spectator player within 50 blocks of the spider") | the nearest combatant `MobHostility` names within **32** (ACCEPTED), feet to feet from the MOUNT, inclusive |
| K5 | "retreat" (the old RETREAT) | READ ("if the player is 8–12 blocks away, go to WINDUP; else pathfind (speed arg 1.6) to the point 12 blocks from the player along the player-to-spider axis") | **RECOVER / HOLD**: outside the band it paths to that point at speed 1.6 (READ); inside it, it **stands** (CHOSEN: the record went straight to WINDUP, which a period now forbids); with no target it stands |
| K6 | the band | ACCEPTED, and READ (8 to 12) | **8 to 12, feet to feet, 3D, both included** (*3D feet to feet* CHOSEN; the record says "blocks away") |
| K7 | what starts a wind-up | NOT IN THE RECORD (it had no period) | **HOLD with its target in the band**; HOLD begins **120 ticks** (ACCEPTED 6 s) after the last wind-up **began**, or after the pair appeared (CHOSEN: the Phalanx's first shot is 120 after it appears) |
| K8 | the wind-up | ACCEPTED 1 s, cfde822 15 t | **20 ticks rooted**, launch on the 20th. The root is the Knell's: stop the path every tick, a TRANSIENT movement-speed × 0 on the MOUNT, its horizontal velocity zeroed once |
| K9 | the wind-up aborted | READ ("If the player leaves 6.4–18 blocks, back to RETREAT") | **NEVER: once begun it always leaps**, whatever the target does (CHOSEN, the Knell's beam: a warning that can be walked away from by the target ending it is not a dodge) |
| K10 | the warning's sight and sound | READ ("a spider-ambient sound and 15 crit particles" on entry) | as read, and the **15 crit repeated every 5 ticks** of the wind-up (CHOSEN, so it is visible for the whole second); volume and pitch 1.0 (CHOSEN) |
| K11 | the leap's aim | READ: re-aimed at the player every tick (homing) | **FIXED at the wind-up's FIRST tick: the target's feet then.** No homing (CHOSEN: the leap is the dodge; a homing pounce cannot be got out of) |
| K12 | the leap's arc | READ (vertical 1.2; horizontal = distance / 28, re-set every tick) | vertical **1.2** (READ); the horizontal speed **held every tick** (READ: re-set each tick) at **distance / T**, T the flight's ticks to the aim's height under vanilla's gravity and drag (below), not the record's nominal 28. On level ground it rises **8.19 blocks**, T = **29** |
| K13 | the leap's sound | READ ("spider-hurt sound") | as read, 1.0 / 1.0 (CHOSEN) |
| K14 | the landing | READ ("when it touches ground after leaving it") | the first tick the mount is on the ground after it has left it |
| K15 | stuck | READ ("aborts to RETREAT if, once airborne, its horizontal speed falls below half the launch speed") | **dropped**: the speed is held every tick, so it cannot fall. **Blocked** (a wall): it presses on, falls, and lands where it lands |
| K16 | the timeout | READ (40 t) | **40 ticks** after the launch; the hit is **tested where it is** then, and the velocity is no longer held (the brief) |
| K17 | who the landing hits | READ ("the player within 2.5 blocks of the spider") | **every combatant within 3** (ACCEPTED) of the mount's FEET, feet to feet, inclusive; **never a mob** (the seat's ruling 5) |
| K18 | the damage | ACCEPTED 55, scaled by gear score | **55 × GS / 100** (`MobScaling.attackDamage`), through `applyDamage(dealt, rider)`: defence applies, **no element**, no crit, no shield block. **The rider is the damager** (READ) |
| K19 | the knockback | READ (0.7 horizontal away from the spider, 0.4 vertical), ACCEPTED "moderate" | **0.7 out from the landing point, 0.4 up, × (1 − KNOCKBACK_RESISTANCE)**, ADDED to his velocity by `setVelocity` (the blast's route). A victim exactly under the landing is pushed straight up |
| K20 | the landing's sound | READ ("a crit sound") | `entity.player.attack.crit` 1.0 / 1.0 (key and numbers CHOSEN) |
| K21 | after the landing | READ ("Then RETREAT") | RECOVER |
| K22 | the spear between leaps | READ ("the leap's damage is scripted, the spear is for melee after a dismount") | held, never used while paired (no target); **never dropped**. What it adds to a mob's melee on foot: **NOT READ** (`PLAN-legacy-port.md` F8) |
| K23 | the mount dies first | ACCEPTED ("the rider drops and fights on foot as a plain melee mob"); READ (`leaves the vehicle`) | the rider **dismounts where it is** the tick after (vanilla would seat it on the corpse for 20 ticks, INFERRED), its target events are no longer cancelled, its speed is its own (never changed) |
| K24 | the rider dies first | ACCEPTED ("the mount carries on as a plain fast spider with no leap"); READ (speed ÷ 1.3) | **it KEEPS 1.3x** (the seat's reading of "fast", and the brief's), widowed; vanilla spider AI, with vanilla's own small hop (`LeapAtTargetGoal`) back: **"no leap" read as no POUNCE** (CHOSEN, for Ben) |
| K25 | separation without a death | NOT IN THE RECORD | **as if the other had died, and it never re-pairs** (the seat's lean): the mount is widowed and keeps 1.3x; the rider is a plain melee mob |
| K26 | either death, or a separation | READ ("The loop ends either way") | the loop ends the same tick it sees it (below) |
| K27 | elements | Ben ("No it's no element"; the Skreel Void) | the rider's death feeds a small Phage; the mount's feeds nothing (PS7, PS8) |
| K28 | a frozen (NoAI) pair | NOT IN THE RECORD | never begins a wind-up of its own; the POUNCE step still forces one (but a frozen mount never moves, F18) |
| K29 | the mount's fall | NOT IN THE RECORD | **no fall damage from its own leap**: its fall distance is reset every tick of the flight (CHOSEN; an 8-block drop would cost it 5 every leap) |

**WHAT VANILLA DOES TO SEPARATE A PAIR (for K25), each INFERRED from the jar's names, none staged but `/ride`:** **water**
(`LivingEntity` dismounts a passenger whose eyes are under water when it `dismountsUnderwater`, NOT READ for these
types); **a boat** (a boat takes only a creature narrower than itself and not already riding: the 2.1-wide mount and the
riding Skreel both fail, NOT READ); **a teleport** (Paper refuses to teleport a vehicle with passengers, and a teleported
passenger leaves its vehicle, NOT READ); **a portal** (a vehicle takes its passengers through; the loop ends with the old
entity and resumes on the new one's appearance, NOT READ); **a chunk edge** (the pair is saved together; the loop ends on
the unload and resumes on the load, the rider found by the uuid in the mount's PDC); **a command** (`/ride … dismount`:
PS5). Each of them is caught by the same per-tick check (the rider no longer the mount's passenger), whatever did it.

## THE JAR FACTS: WHO STEERS, AND THE GOALS THAT WOULD FIGHT THE CYCLE

`javap -c -p -cp run/versions/26.1.2/paper-26.1.2.jar <class>` (build 74), READ 2026-11-17:
- **`net.minecraft.world.entity.Mob#getControllingPassenger`**: the first passenger when it is a `Mob`,
  `canControlVehicle()` (true unless tagged `NON_CONTROLLING_RIDER`) and the vehicle `!isNoAi()`.
- **`Mob#getNavigation`, `Mob#getMoveControl`**: a mob whose controlled vehicle is a `Mob` returns the VEHICLE's.
  **`net.minecraft.world.entity.ai.control.MoveControl#tick`** sets the speed from `this.mob.getAttributeValue(MOVEMENT_SPEED)`,
  its mob the vehicle's own: **the mount's speed moves the pair.**
- **`Mob#updateControlFlags`**: a mob whose controlling passenger is a `Mob` has its MOVE, JUMP and LOOK goal flags off.
- **`net.minecraft.world.entity.monster.spider.Spider#registerGoals`**: `FloatGoal`, `AvoidEntityGoal(Armadillo)`,
  **`LeapAtTargetGoal(0.4)`** (flags JUMP and MOVE, `LeapAtTargetGoal.<init>`; `canUse` needs `getTarget()` and the
  ground), **`Spider$SpiderAttackGoal`**, `WaterAvoidingRandomStrollGoal(0.8)`, `LookAtPlayerGoal`, and the target goals.
- **`net.minecraft.world.entity.monster.skeleton.AbstractSkeleton#registerGoals` / `#reassessWeaponGoal`**:
  `RestrictSunGoal`, `FleeSunGoal`, `AvoidEntityGoal(Wolf)`, `WaterAvoidingRandomStrollGoal`, `LookAtPlayerGoal`,
  `RandomLookAroundGoal`, `HurtByTargetGoal`, `NearestAttackableTargetGoal`; and **`MeleeAttackGoal` for any main hand
  but `Items.BOW`**, so the spear is melee.
- **`Mob#setTarget(LivingEntity, EntityTargetEvent$TargetReason)`** calls `CraftEventFactory.callEntityTargetLivingEvent`
  and returns at once when it is cancelled (offsets 108-122); **`NearestAttackableTargetGoal#start`** calls it.

**WHAT IS BLOCKED, AND HOW (nothing is removed):** the spider's `LeapAtTargetGoal` and `SpiderAttackGoal` are off by the
control flags while it is ridden, and need a target; the rider's `MeleeAttackGoal` needs a target; **every target goal of
both goes through the cancelled event**, so neither ever has one while paired. The rider's stroll and look goals are not
blocked: a stroll's path is replaced or stopped by the cycle every tick it moves or roots, and the root's × 0 holds the
mount still whatever the path says. Pinned on the source: `DragonSkreelWiringTest`.

## THE CYCLE, A STATE MACHINE (`core.mob.PounceCycle`), AND ITS TEST POINTS

Inputs each tick: can it act (the mount has AI), the distance to its target (or none), the mount on the ground.

| stage | each tick | to |
|---|---|---|
| **RECOVER** (start; after every leap) | outside the band CLOSE_IN, inside it or with no target STAND | HOLD once 120 ticks have passed since the last wind-up BEGAN (or the loop began), in that tick |
| **HOLD** | as RECOVER | **WINDUP** the first tick the target is in the band and it can act, or at once when forced |
| **WINDUP** | BEGIN on tick 0 (fix the aim, root, warn), ROOT on ticks 1–19 | **LEAP**: LAUNCH on tick 20 |
| **LEAP** | FLY (hold the velocity) | **RECOVER**: LANDED the first tick on the ground after leaving it; TIMED_OUT on tick 40 after the launch |

`PounceCycleTest` (11 rows): the first wind-up on the 120th tick of the loop and not before; CLOSE_IN at 20 and 3, STAND
at 8 and 12 and with none, CLOSE_IN at 12.001 and 7.999; ready but out of band holds, then begins at 12; a frozen pair
stands 400 ticks; the launch exactly 20 after the begin, 19 ROOTs between; a wind-up whose target leaves or vanishes
still launches; FLY on the ground before leaving, 27 FLY in the air, LANDED; 39 FLY then TIMED_OUT at 40, in the air and
on a ceilinged ground; the next BEGIN at exactly tick 120 after the last (a landing at 26 between); the force begins on
the next tick with no target and no AI, and is refused during a wind-up and a leap.

## THE LEAP'S VELOCITY, AS AN EXPRESSION (`MobPounce.leap`), AND ITS TEST POINTS

With the mount's feet F, the aim A, gravity g (the mount's own `GRAVITY` attribute, logged), drag d = `(double) 0.98f` =
0.9800000190734863 (READ, `LivingEntity#travelInAir`, `ldc 0.98f`):

```
v(0) = 1.2;  v(i+1) = (v(i) - g) x d;  y(n) = v(0) + ... + v(n-1)
T = the first n in 1..40 with v(n-1) < 0 and y(n) <= A.y - F.y     (else 40)
D = hypot(A.x - F.x, A.z - F.z)
velocity = ( (A.x - F.x)/D x D/T ,  1.2 ,  (A.z - F.z)/D x D/T )   (D = 0: (0, 1.2, 0))
```

The horizontal velocity is set again every tick of the flight, so after T ticks it has flown exactly D.
`MobPounceTest` (each checked by replaying the air step over the returned velocity): **level, 10 east: T = 29,
v = (10/29, 1.2, 0)**; **one block down, (6, −1, 8): T = 30, v = (0.2, 1.2, 8/30)**; **two up, (0, 2, −9): T = 27**;
**above the arc's top (8.19), (3, 9, 4): T = 15** (its first falling tick, landing short); **a timeout of 20: T = 20**;
**straight below: (0, 1.2, 0), T = 29**. From jshell, 2026-11-17: T 29 / 30 / 27 / 20 / 15 at a rise of 0 / −1 / 2 / 7 /
9, the arc's top 8.187664.

## THE PHALANX: THE COOLDOWN, R3, R5

- **THE COOLDOWN** (`MobBlink.cooldown_ticks: 240`, required by the loader, refused below 0 by name). A counter on the
  Phalanx's live loop (not in its PDC: an unload and a reload start a fresh loop with none, as the shot clock does).
  Counted down **first** every loop tick; a blink that **lands** sets it to 240, and **nothing else does**: a blink that
  lands at tick L leaves 239 ticks that cannot decide, and **L + 240 can**. While it runs, a combatant inside the trigger
  box causes **no decision and no BLINK line: the Phalanx fights as it stands** (its shot's clock, its bow goal and its
  shield run on). **A forced blink ignores it** and says `cooldown=<ticks left> ignored`; a forced blink that lands starts
  it too. Every BLINK line now ends `anchor=<y> cooldown=<n>`, and every `BLINK moved` line `cooldown=240`.
- **R3, DIAGNOSED.** `BeamTeleport.foot` tries **whole blocks** k = 0, +1, −1, … ±3 from the column's y, and `MobBlink.decide`
  put the column at the Phalanx's own feet. A full block's top is a whole number. **From 63.305 every tried height is
  63.305 + k**: k = −1, −2, −3 put the body 0.695 inside the floor (it does not fit: `blocked=3`), k = 0…3 hang it 0.305
  above a whole block (the ground slab under its feet, 0.05 to 0.001 deep, is air: `ungrounded=4`). **That is exactly
  BQ1's `NO_FOOTING blocked=3 ungrounded=4`**, reproduced by `MobBlinkCooldownTest` on a floor topped at 63. (Its
  `blocked=0 ungrounded=7` draws are columns where nothing stood within 3.3 below either, INFERRED: off the staged
  floor.) **The same failure on the ground: a Phalanx standing on a slab (63.5).**
- **R3, FIXED: the search is anchored on `floor(feet.y)`**, the whole block its feet are in (+ 10⁻⁶, so 62.9999999999 is
  63), and every height tried is whole. **Why not the two leans offered:** the ground beneath it is 63.5 on a slab, the
  same defect; the triggering combatant's feet: a forced blink has none, a combatant mid-jump is fractional too, and the
  6-16 ring is measured from the Phalanx. **The cost:** a landing on a slab's top is never tried, as it never was from a
  whole height. **And the latent twin, NOT FIXED here:** the Knell's teleport anchors on its target's feet, so a target
  mid-jump at the move fails every candidate the same way (the plan's F19).
- **R5, READ (no row):** **one hit, two knockback events.** `LivingEntity#hurt` raises its knockback with
  `EntityKnockbackEvent$Cause.ENTITY_ATTACK` for a player's attack (offsets ~571-577); **`Player#causeExtraKnockback`**
  raises a second, also `ENTITY_ATTACK`, **only when the extra strength is above 0** (a sprint hit, or a knockback bonus:
  `fcmpl; ifle`, offsets 2-54). Our `onShieldedKnockback` logs each it cancels, and the damage is absorbed once, a tick
  later. **So two lines and one absorb is one sprinting hit with both pushes cancelled while the shield was up: correct,
  not a fault.** BS2's single unsprinted `Player#attack` logged one line, which fits. *That Ben's hit was a sprint is
  INFERRED; no line records it.*
- **R4 (BC4B), FIXED in its scenario:** `kill @e[tag=bc4bh]` after its `kill @e[tag=bc4b]`.

## THE ROWS ALREADY MERGED — THE AUDIT

`grep -n "skreel\|withered_mount\|phalanx" paper/src/main/resources/selftest/*.yml` at master `185a6437`:
- **The Skreel and the mount:** only `GATE-dragon-bodies.yml`'s DB6R2 and DB7R2 (`GATE-dragon-fix.md`). **Moved and
  re-predicted here** (DB6R3, DB7R3); the bodies file keeps a note in their place.
- **The Phalanx:** the twelve rows of `GATE-dragon-shot.yml` and DB5R2: **all frozen, none blinks** (a frozen Phalanx
  never blinks of itself, and none has a BLINK step), so the cooldown changes nothing there. **`GATE-dragon-blink.yml`:**
  BB1, BB2, BB3 and BB5 force a blink on a fresh Phalanx at a whole height, so their verdicts are unchanged and **each
  BLINK line gains `anchor=<its y> cooldown=0`** and each `BLINK moved` `cooldown=240`; **BB4** (a live Phalanx's first
  blink) the same; **BQ1** (Ben's live play) would now see at most one landed blink per 240 ticks, and no second decision
  from the air (R3). BC4B kills its husk. Their predictions are not edited; a dated note in `GATE-dragon-blink.md` says so.
- **Not affected:** every Knell and Phage row (no file touched but `phalanx.yml`, `skreel.yml`, `withered_mount.yml`).
- **The scans:** `noScenarioOutsideTheSkreelGateLeavesALivePair` (new: a live `skreel` or `withered_mount` spawn, a bare
  `rpg spawn` of one, or a POUNCE step, only in this gate; the control finds this gate's six live pairs and three POUNCE
  steps); `noScenarioOutsideTheShotGateMakesAPhalanxFire` now counts this gate a shot gate (PB1, PB2, PQ2 are live).

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-skreel` prints it, shortened. **Not** master's `185a6437`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1`, after this boot's `Done` | |

### R0b — the jar carries the pair, the cooldown and the gate

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `skreel.yml` `mount: withered_mount`, `pounce:`, `  windup_ticks: 20`; `withered_mount.yml` `ridden_speed: 1.3`, `max_health: 160`; `phalanx.yml` `  cooldown_ticks: 240`; `health/MobPouncer.class`; `GATE-dragon-skreel.yml` `PS3:`, `PQ2:`. **ABSENT, and PRESENT on master:** `withered_mount.yml` `speed: 1.3`. **The controls, PRESENT on both builds:** `phalanx.yml` `  life_ticks: 240`; `health/MobShooter.class`; `GATE-dragon-blink.yml` `BQ1:` | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
foreach ($q in @(@('skreel.yml', 'mount: withered_mount'), @('skreel.yml', 'pounce:'), @('skreel.yml', '  windup_ticks: 20'), @('withered_mount.yml', 'ridden_speed: 1.3'), @('withered_mount.yml', 'max_health: 160'), @('withered_mount.yml', 'speed: 1.3'), @('phalanx.yml', '  cooldown_ticks: 240'), @('phalanx.yml', '  life_ticks: 240'))) {
  $y = Bytes "content/mobs/$($q[0])"
  if ($y -ne $null -and $y -match ("(?m)^" + [regex]::Escape($q[1]) + "\s*$")) { "PRESENT $($q[0]) '$($q[1])'" } else { "ABSENT  $($q[0]) '$($q[1])'" }
}
foreach ($c in 'health/MobPouncer', 'health/MobShooter') {
  if ((Bytes "io/github/butterflysmp/rpg/paper/$c.class") -ne $null) { "PRESENT $c.class" } else { "ABSENT  $c.class" }
}
foreach ($q in @(@('dragon-skreel', 'PS3'), @('dragon-skreel', 'PQ2'), @('dragon-blink', 'BQ1'))) {
  $y = Bytes "selftest/GATE-$($q[0]).yml"
  if ($y -eq $null) { "MISSING selftest/GATE-$($q[0]).yml" } elseif ($y -match "(?m)^$($q[1]):\s*$") { "PRESENT $($q[0]) $($q[1]):" } else { "ABSENT  $($q[0]) $($q[1]):" }
}
$zip.Dispose()
```

**A jar of master `185a6437` reads the opposite:** ABSENT on the six new content lines and `MobPouncer.class`, PRESENT on
`speed: 1.3`, `MISSING selftest/GATE-dragon-skreel.yml`; the three controls PRESENT on both. (`speed: 1.3` is anchored at
column 0, so `ridden_speed: 1.3` cannot match it.)

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim the boot of `ef770b8a`'s (this branch adds no content file; it changes three). No `Refusing`, `Skipping`, `SEVERE` or `Exception`; no `[Rpg]` `WARN` beyond `volley_stone`'s known pair. **A `Skipping malformed mob` line (`4 mobs`) would mean a pounce, mount, ridden-speed or cooldown key was refused** | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception'` | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 157 scenarios across 20 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel]`**, no `Refusing selftest` | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | |

**The 157 is RECOUNTED from the files at this branch's head:** in `paper/src/main/resources/selftest/`,
`grep -cE '^  - ' index.yml` prints **20**; summing `grep -cE '^[A-Za-z0-9-]+:'` over the twenty `GATE-<g>.yml` prints
**177**; the same with `'^setup:'` prints **20**: 177 − 20 = **157**. **The control:** the same three commands over
`git show master:…` print 19, 165, 19: **146**, `GATE-dragon-blink.md` R0d's reading. 157 = 146 − 2 (DB6R2, DB7R2 moved)
+ 13 (this gate).

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**Every row starts** `LEFTOVER living=0 radius=16` and `LINGER work=0 entities=0 radius=48`, and **ends with an empty
LINGER at the next row's start**. **Notation:** P0 = his feet at the row's start, (x0, y0, z0), a block centre; R and M
the rider's and the mount's uuids; t0 a wind-up's BEGIN tick, read from its line. Every `SKREEL` line is `[Rpg]`'s. A
pair killed by one `kill` logs **`SKREEL broken mount=M rider=R tick=<t> reason=mount-died rider_state=dead`** (the mount's
next tick finds it dead and its rider dead), predicted for every row that ends that way and not repeated below.

### DB6R3 — one spawn makes two, mounted, sharing GS 300 (SURVIVAL, FROZEN)

| prediction | stands on | READING |
|---|---|---|
| `DRIVE spawn tag=db6 R skreel frozen`; `SKREEL paired mount=M rider=R tick=<t> gs=300 speed=0.3900 base=0.3000`. **Two BODY lines, the mount's first** (nearest first, and the rider sits above it): **`BODY tag=db6 M type=spider mob=withered_mount stage=- scale=1.500 height=1.350 speedBase=0.3000 speedType=0.3000 speedX=1.000 maxHP=480.000 gs=300 element=none dead=false`** and **`BODY tag=db6 R type=wither_skeleton mob=skreel stage=- scale=1.000 height=2.400 speedBase=0.2500 speedType=0.2500 speedX=1.000 maxHP=360.000 gs=300 element=void dead=false`**. Two PAIR lines, in either order: **`PAIR tag=db6 M type=spider/withered_mount vehicle=none passengers=[R] stage=RECOVER since=<4 to 7> speed=0.3900 base=0.3000 ridden=true rooted=false widowed=false ai=false`** and **`PAIR tag=db6 R type=wither_skeleton/skreel vehicle=M passengers=[] stage=RECOVER since=<4 to 7> speed=0.2500 base=0.2500 ridden=false rooted=false widowed=false ai=false`**. **`ENTITIES SPIDER count=1 radius=8`** (DB6R2 read 0) | `RpgCommand.spawnMount` (the rider's 300 written before the mount's add); DB7R2's read `speedType=0.3000` and DB6R2's rider line; 120 × 3, 160 × 3; the ridden modifier 0.3000 × 1.3 = 0.39000001549720765 (jshell, DB7R2's note) | |

### DB7R3 — the mount alone: its own speed, 160 health, no element, no rider (SURVIVAL, FROZEN)

| prediction | stands on | READING |
|---|---|---|
| **`BODY tag=db7 <M> type=spider mob=withered_mount stage=- scale=1.500 height=1.350 speedBase=0.3000 speedType=0.3000 speedX=1.000 maxHP=160.000 gs=100 element=none dead=false`**; **`PAIR tag=db7 <M> type=spider/withered_mount vehicle=none passengers=[] stage=none since=- speed=0.3000 base=0.3000 ridden=false rooted=false widowed=false ai=false`**; `ENTITIES WITHER_SKELETON count=0 radius=8`; **no SKREEL line in the row** | `withered_mount.yml` (no `speed:`); the ridden modifier is added only by a pairing | |

### PS3 — the pounce lands: rooted 20 ticks, the leap, the hit, the push (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| `SKREEL paired … gs=100 speed=0.3900 base=0.3000`. `AT tag=ps3 M x=<x0+10> y=<y0> z=<z0>` (and R's). `DRIVE pounce tag=ps3 M aim=<x0 y0 z0>` (one line: the rider rides its tagged mount and is skipped). **`SKREEL windup rider=R mount=M tick=t0 forced=true at=<x0+10 y0 z0> target=player <Ben> aim=<x0 y0 z0> d=10.000`**. The `AT` ten ticks later: **M unchanged** to three places. **`SKREEL leap rider=R mount=M tick=t0+20 from=<x0+10 y0 z0> windup_from=<x0+10 y0 z0> aim=<x0 y0 z0> v=-0.3448 1.2000 0.0000 ticks=29 horizontal=10.000 gravity=0.0800`**: `from` = `windup_from` to three places is the root. **`SKREEL land … tick=<t0+49 or t0+50> how=landed age=<29, ±1> at=<within 0.25 of x0 y0 z0> aim=<x0 y0 z0> off=<0.000 to 0.250> gs=100 dealt=55.000 reach=3.00 victims=1 [player <Ben> d=<0.75 to 1.25> res=0.000 push=<0.65 to 0.70> 0.4000 <−0.20 to 0.20>] spared=0 []`**, then its **HURT `before=55.000 after=50.000 defense=10.000 dealerType=wither_skeleton/skreel`**, and **no other HURT** (no fall). `SELF customHP=<max − 50>/<max> at=<x greater than x0+2.00> …`: he was pushed east | the leap's T (MobPounceTest: 29, D/29); `v=-10/29` = −0.344828; the held horizontal; the landing rule (he is 1 from it); `MobPounce.push`; the fall table above. **INFERRED, not read:** the pair stands still before the force (STAND in the band); the order of our task and the mount's move in a tick (age 29 either way); the landing's `off` (only a stroll's path could add to it, at the air's 0.02); his client carrying the push (~5 blocks in 11 ticks of air) | |

### PS4 — out of the way in time; a frozen husk at the landing is spared (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| As PS3 to the leap (`d=10.000`, `ticks=29`). **`SKREEL land … how=landed … aim=<x0 y0 z0> … dealt=55.000 reach=3.00 victims=0 [] spared=1 [husk <H> d=<0.75 to 1.25>]`**; **no HURT in the row**; `PROBE tag=ps4h … husk customHP=100.000/100.000`; `SELF customHP=<max>/<max> at=<x0+4.00 y0 z0> …`. After it lands the pair backs off (no line) | he stands 4 from the aim, past the reach of 3 (`MobPounce.landing`); a mob is never a victim; BC4B's husk read 100/100 | |

### PS5 — separated by a command: the pair breaks, the mount is widowed and keeps 1.3x (SURVIVAL, FROZEN)

| prediction | stands on | READING |
|---|---|---|
| `SKREEL paired …`; `CMD /ride …`; within two ticks **`SKREEL broken mount=M rider=R tick=<t> reason=separated`**; two PAIR lines, either order: **`PAIR tag=ps5 M type=spider/withered_mount vehicle=none passengers=[] stage=none since=- speed=0.3900 base=0.3000 ridden=true rooted=false widowed=true ai=false`** and **`PAIR tag=ps5 R type=wither_skeleton/skreel vehicle=none passengers=[] stage=none since=- speed=0.2500 base=0.2500 ridden=false rooted=false widowed=false ai=false`**; **no SKREEL line at the kill** (the loop is gone) | `MobPouncer.step`'s check, every tick; `widow()` keeps the modifier | |

### PS6 — the live cycle: its own second wind-up 120 ticks after the first (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| The forced `SKREEL windup … tick=t0 forced=true … aim=<x0 y0 z0>`; its leap; **its land `victims=0 []`** (he stepped 4 east). **Then `SKREEL windup … tick=t0+120 forced=false … target=player <Ben> aim=<x0+4 y0 z0> d=<11.5 to 12.5>`**: exactly 120 after the first began. Its `SKREEL leap … ticks=29 horizontal=<11.5 to 12.5>`; **its land `victims=1 [player <Ben> d=<0.00 to 0.50> …]`** after the heal, its **HURT `before=55.000 after=50.000`**. `PAIR tag=ps6 M … stage=RECOVER since=<55 to 65> … ridden=true …`; `SELF customHP=<max − 50>/<max>`. **No third wind-up** before the kill | `PounceCycle` (the period from the BEGIN; `theNextWindUpIsAHundredAndTwentyTicksAfterThePreviousBegan`). **INFERRED, not read:** that the pair is back in its band by t0+120. After the first landing it stands 4 from him and paths to 12 (8 blocks west) at speed 1.6 × 0.39; a mob's ground speed is about 0.98 × speed² / 0.454 a tick (vanilla's walk, from memory, NOT READ), so it arrives within about 20 ticks, and the margin is the ~70 ticks left | |

### PS7 — the mount dies first: the rider on foot, no further wind-up, and the Phage does NOT grow (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| `BODY tag=ps7p <P> type=creeper mob=phage stage=1 …`. After the spider's kill: **`SKREEL broken mount=M rider=R tick=<t> reason=mount-died rider_state=alive dismounted=<x0+10 ± 0.6, z0 ± 0.6>`**, and **no GROWTH line**. PAIR, the rider: **`PAIR tag=ps7 R type=wither_skeleton/skreel vehicle=none passengers=[] stage=none since=- speed=0.2500 base=0.2500 ridden=false rooted=false widowed=false ai=false`** (and the dying mount's own line, `stage=none`, not predicted further). `BODY tag=ps7p … stage=1 …` again. **After the rider's kill: `GROWTH mob=phage id=<P> from=1 to=2 fed=wither_skeleton/skreel R element=void d=<1.8 to 3.0> …`** and `BODY tag=ps7p … stage=2 …`: **the positive control** | the mount has no element (`withered_mount.yml`; the resolver, R1); the rider's is void; growth radius 6 (`phage.yml`); `MobPouncer.stopped` dismounts a dead mount's rider | |

### PS8 — the rider dies first: the Phage DOES grow, the mount alone keeps 1.3x and never winds up (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| After the rider's kill: **`GROWTH mob=phage id=<P> from=1 to=2 fed=wither_skeleton/skreel R element=void d=<1.8 to 3.2> …`**, and **`SKREEL broken mount=M rider=R tick=<t> reason=rider-died`**. `BODY tag=ps8p … stage=2 …`. **`PAIR tag=ps8 M type=spider/withered_mount vehicle=none passengers=[] stage=none since=- speed=0.3900 base=0.3000 ridden=true rooted=false widowed=true ai=true`**, the same 140 ticks later; **no SKREEL line in those 140 ticks**, though he stands 10 from it, in what was its band. At the kill no SKREEL line | `MobPouncer.step` (rider dead: widow, end); a widowed mount has no loop. **The clause is not hollow:** the mount has its AI and he is in its band, so a loop that survived would wind up at its 120th tick | |

### PB1 — the cooldown: no decision for 239 ticks after a landed blink, one on the 240th (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| `DRIVE blink tag=pb1 <X> angle=90.000 distance=8.000`; **`BLINK mob=phalanx id=<X> tick=L forced=true trigger=none from=<x0+8 y0 z0> tried=1 [90.000 8.000 OK at=<x0+8 y0 z0+8>] landed=<x0+8 y0 z0+8> anchor=<y0>.000 cooldown=0`**; **`BLINK moved … tick=L at=<x0+8 y0 z0+8> invisible=true cooldown=240`**. Its first shot (`SHOT fired … forced=false`, about L+117) comes with **`BLINK visible … reason=fired`** and lands on him (HURT 40 → 36.364). **No BLINK line from L+1 to L+239**, though he stands 3 from it, inside its box, and it is visible from its first shot on. **Exactly one `BLINK mob=phalanx id=<X> tick=L+240 forced=false trigger=player <Ben> from=<x0+8 y0 z0+8> … anchor=<y0>.000 cooldown=0`** with `landed=` not none, and its `BLINK moved … cooldown=240`. A second shot (about L+237) and its HURT. `BOW suppressed` lines are not predicted and none is a failure | `MobBlink.Cooldown` (`afterALandedBlinkItRuns…`); the step's order (counted first, the block after the AI and invisibility checks); its speed 0 keeps it inside his reach; 8 attempts from the uniform draw land on this open floor (INFERRED) | |

### PB2 — a failed attempt does not start it (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| **`BLINK … tick=<t> forced=true trigger=none from=<x0+8 y0 z0> tried=8 [90.000 8.000 NO_FOOTING blocked=0 ungrounded=7, …eight times] landed=none anchor=<y0>.000 cooldown=0`**; no `BLINK moved`. He steps to 3 west of it. **Within two ticks, exactly one `BLINK … forced=false trigger=player <Ben> from=<x0+8 y0 z0> … landed=<not none> anchor=<y0>.000 cooldown=0`** and its `BLINK moved … cooldown=240`. No SHOT line | the pit (BB2's, read `ungrounded=7`); only a landing calls `landed()` (`onlyALandingStartsItAndZeroMeansNone`); a uniform draw falls in the 3×3 pit with probability about 9 / 691, so eight all failing is ~10⁻¹⁵ | |

### PB3 — R3: a blink decided at a fractional height lands; a forced blink during the cooldown says "ignored" (SURVIVAL, FROZEN)

| prediction | stands on | READING |
|---|---|---|
| `AT tag=pb3 <X> x=<x0+8> y=<y0+0.500> z=<z0>` (on the slab). **`BLINK … tick=L1 forced=true trigger=none from=<x0+8 y0+0.500 z0> tried=1 [90.000 8.000 OK at=<x0+8 y0 z0+8>] landed=<x0+8 y0 z0+8> anchor=<y0>.000 cooldown=0`**, `BLINK moved … at=<x0+8 y0 z0+8> … cooldown=240`. **Then `BLINK … tick=L2 forced=true trigger=none from=<x0+8 y0 z0+8> tried=1 [180.000 8.000 OK at=<x0 y0 z0+8>] landed=<x0 y0 z0+8> anchor=<y0>.000 cooldown=<240 − (L2 − L1)> ignored`**, expected `cooldown=235 ignored` (L2 − L1 = 5, ±1 on the force's tick); `BLINK moved … at=<x0 y0 z0+8> … cooldown=240`. The last `AT` at x0 y0 z0+8. **On master this row's first BLINK reads `tried=8 […NO_FOOTING blocked=3 ungrounded=4…] landed=none`**: the row discriminates | `MobBlink.anchor` (floor of 63.5 is 63); `aPhalanxOnASlabFailsFromItsFeetAndLandsFromItsBlock`; the forced branch ignores the cooldown | |

## BEN'S ROWS — BY THE ASK STEP, SURVIVAL

### PQ1 — the Skreel: see it, dodge it, take one (SURVIVAL, LIVE)

| AUTO half, recorded beside his answers | Ben's half | READING |
|---|---|---|
| Every `SKREEL` line and every HURT in the row. **Not predicted: it is his play.** Each wind-up is at least 120 ticks after the last; every HURT is `dealerType=wither_skeleton/skreel`, `before=55.000` | `PQ1 ASK 1 question="Could you see the Skreel wind up before it leapt?"`; `ASK 2 question="Could you get out of the way in time?"`; `ASK 3 question="Did the landing hit feel right for a pounce?"` | |

### PQ2 — the Phalanx with its 12-second cooldown (SURVIVAL, LIVE)

| AUTO half | Ben's half | READING |
|---|---|---|
| Every `BLINK`, `BLINK moved`, `BLINK visible`, `SHOT` and `SHIELD` line. **Not predicted.** Every own `BLINK` decision is at least **240 ticks** after the last `BLINK moved` | `PQ2 ASK 1 question="Does the Phalanx's blink feel right with the 12-second cooldown?"` | |

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, as `GATE-dragon-blink.md` step 2, the folder suffix
   `-pre-dragon-skreel`. Report the folder, both pairs of counts, and `diff -rq lines: 0` each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report.
4. **The world's saved difficulty** is read and reported (the gate's `setup:` sets NORMAL and restores it).
5. **Boot this branch's head:** `./scripts/dev-server.sh --refresh-content`, announcing "booting now", on Ben's "ready".
6. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` line names this branch's head. **Readings count
   only after Ben's LOGIN in that boot's log.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL. NOTHING ELSE IS RUN

1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest dragon-skreel` — **ONCE. Wait for the chat to say the run finished, and answer all four gold
   questions, before the next command.** About **2½ minutes** plus your answers: its waits sum to 1.92 minutes (2298
   ticks), and the thirteen rows' cleans add a little. **Stand still and touch nothing until the first gold text** (about
   1 minute): mobs will leap at you and a Phalanx will shoot you (you cannot die). **At the first gold text,** a Skreel on
   its mount 14 blocks east is yours to watch for **30 seconds**: let it crouch and spark, get out of the way of a few,
   let one land on you, **do not attack it**; three questions. **At the second,** a Phalanx 12 blocks east is yours for
   **40 seconds** with the sword in your hand: walk up to it, chase it, hit it; one question. Answer each `yes` or `no`,
   or in your own words.
3. `/stop`

**Nothing else is run in this boot: no other selftest, no other command.** A departure is recorded as it happened, apart
from the readings.
