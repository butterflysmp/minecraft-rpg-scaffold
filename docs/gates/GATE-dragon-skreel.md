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
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-skreel` prints it, shortened. **Not** master's `185a6437`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1`, after this boot's `Done` | **PASS, 2026-11-17.** `run\logs\latest.log:42:[18:57:19] [Server thread/INFO]: [Rpg] Build: 45bc33ce`; `git ls-remote` printed `45bc33ce1b0babb624e4b695a2800747fc819abe refs/heads/feat/dragon-skreel`; `Done (5.866s)!` at 18:57:19 |

### R0b — the jar carries the pair, the cooldown and the gate

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `skreel.yml` `mount: withered_mount`, `pounce:`, `  windup_ticks: 20`; `withered_mount.yml` `ridden_speed: 1.3`, `max_health: 160`; `phalanx.yml` `  cooldown_ticks: 240`; `health/MobPouncer.class`; `GATE-dragon-skreel.yml` `PS3:`, `PQ2:`. **ABSENT, and PRESENT on master:** `withered_mount.yml` `speed: 1.3`. **The controls, PRESENT on both builds:** `phalanx.yml` `  life_ticks: 240`; `health/MobShooter.class`; `GATE-dragon-blink.yml` `BQ1:` | the scan below | **PASS, 2026-11-17**, the scan as printed: `PRESENT skreel.yml 'mount: withered_mount'`, `PRESENT skreel.yml 'pounce:'`, `PRESENT skreel.yml '  windup_ticks: 20'`, `PRESENT withered_mount.yml 'ridden_speed: 1.3'`, `PRESENT withered_mount.yml 'max_health: 160'`, `ABSENT  withered_mount.yml 'speed: 1.3'`, `PRESENT phalanx.yml '  cooldown_ticks: 240'`, `PRESENT phalanx.yml '  life_ticks: 240'`, `PRESENT health/MobPouncer.class`, `PRESENT health/MobShooter.class`, `PRESENT dragon-skreel PS3:`, `PRESENT dragon-skreel PQ2:`, `PRESENT dragon-blink BQ1:` |

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
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim the boot of `ef770b8a`'s (this branch adds no content file; it changes three). No `Refusing`, `Skipping`, `SEVERE` or `Exception`; no `[Rpg]` `WARN` beyond `volley_stone`'s known pair. **A `Skipping malformed mob` line (`4 mobs`) would mean a pounce, mount, ridden-speed or cooldown key was refused** | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception'` | **PASS, 2026-11-17.** Line 43: `[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`, verbatim. `[Rpg]` WARN: only lines 44-45, `volley_stone`'s known pair. No `Refusing`, `Skipping`, `SEVERE` or `Exception` in the whole log (read again after the stop). The other WARN lines are Paper's own version notice (`Paper Async Task Handler Thread`, lines 72-77) |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 157 scenarios across 20 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel]`**, no `Refusing selftest` | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | **PASS, 2026-11-17.** Exactly one line, line 50: `[Rpg] Selftest: dev server, 157 scenarios across 20 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel]`; no `Refusing selftest` |

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
| `DRIVE spawn tag=db6 R skreel frozen`; `SKREEL paired mount=M rider=R tick=<t> gs=300 speed=0.3900 base=0.3000`. **Two BODY lines, the mount's first** (nearest first, and the rider sits above it): **`BODY tag=db6 M type=spider mob=withered_mount stage=- scale=1.500 height=1.350 speedBase=0.3000 speedType=0.3000 speedX=1.000 maxHP=480.000 gs=300 element=none dead=false`** and **`BODY tag=db6 R type=wither_skeleton mob=skreel stage=- scale=1.000 height=2.400 speedBase=0.2500 speedType=0.2500 speedX=1.000 maxHP=360.000 gs=300 element=void dead=false`**. Two PAIR lines, in either order: **`PAIR tag=db6 M type=spider/withered_mount vehicle=none passengers=[R] stage=RECOVER since=<4 to 7> speed=0.3900 base=0.3000 ridden=true rooted=false widowed=false ai=false`** and **`PAIR tag=db6 R type=wither_skeleton/skreel vehicle=M passengers=[] stage=RECOVER since=<4 to 7> speed=0.2500 base=0.2500 ridden=false rooted=false widowed=false ai=false`**. **`ENTITIES SPIDER count=1 radius=8`** (DB6R2 read 0) | `RpgCommand.spawnMount` (the rider's 300 written before the mount's add); DB7R2's read `speedType=0.3000` and DB6R2's rider line; 120 × 3, 160 × 3; the ridden modifier 0.3000 × 1.3 = 0.39000001549720765 (jshell, DB7R2's note) | **2026-11-17. MISMATCH on the two PAIR lines (`stage`, `since`), from the instrument: they were read AFTER the kill.** Verbatim matches: `DRIVE spawn tag=db6 7f1ef1d3-ea6d-4631-b34a-9c06105728fa skreel frozen`; `SKREEL paired mount=e463bab1-e349-463e-b596-3f71aea56297 rider=7f1ef1d3-ea6d-4631-b34a-9c06105728fa tick=698 gs=300 speed=0.3900 base=0.3000`; both BODY lines, the mount's first, character for character as predicted (mount `maxHP=480.000 gs=300 element=none`, rider `maxHP=360.000 gs=300 element=void`); `ENTITIES SPIDER count=1 radius=8`. Replies: `Spawned Skreel (wither_skeleton, 360 HP, GS 300 given)`, `  riding Withered Mount (spider, 480 HP, GS 300)`. **Read instead of the PAIR predictions, in this order, after `SKREEL broken mount=e463bab1-e349-463e-b596-3f71aea56297 rider=7f1ef1d3-ea6d-4631-b34a-9c06105728fa tick=702 reason=mount-died rider_state=dead`:** `PAIR tag=db6 e463bab1-e349-463e-b596-3f71aea56297 type=spider/withered_mount vehicle=none passengers=[7f1ef1d3-ea6d-4631-b34a-9c06105728fa] stage=none since=- speed=0.3900 base=0.3000 ridden=true rooted=false widowed=false ai=false` and `PAIR tag=db6 7f1ef1d3-ea6d-4631-b34a-9c06105728fa type=wither_skeleton/skreel vehicle=e463bab1-e349-463e-b596-3f71aea56297 passengers=[] stage=none since=- speed=0.2500 base=0.2500 ridden=false rooted=false widowed=false ai=false`. Every field but `stage`/`since` matches. **Cause, read from the source:** `SelfTestDrivers`' `PROBE_PAIR` case logs through `run.scheduler().onEntity(e, …)`, one tick later, and the scenario's next step, `kill @e[tag=db6]`, runs on the same tick as the probe step, so the PAIR lines show the dead pair. The pair's live stage was not read |

### DB7R3 — the mount alone: its own speed, 160 health, no element, no rider (SURVIVAL, FROZEN)

| prediction | stands on | READING |
|---|---|---|
| **`BODY tag=db7 <M> type=spider mob=withered_mount stage=- scale=1.500 height=1.350 speedBase=0.3000 speedType=0.3000 speedX=1.000 maxHP=160.000 gs=100 element=none dead=false`**; **`PAIR tag=db7 <M> type=spider/withered_mount vehicle=none passengers=[] stage=none since=- speed=0.3000 base=0.3000 ridden=false rooted=false widowed=false ai=false`**; `ENTITIES WITHER_SKELETON count=0 radius=8`; **no SKREEL line in the row** | `withered_mount.yml` (no `speed:`); the ridden modifier is added only by a pairing | **VERBATIM MATCH, 2026-11-17.** `BODY tag=db7 f2da5080-1e15-41d6-a326-c2be24b04529 type=spider mob=withered_mount stage=- scale=1.500 height=1.350 speedBase=0.3000 speedType=0.3000 speedX=1.000 maxHP=160.000 gs=100 element=none dead=false`; `PAIR tag=db7 f2da5080-1e15-41d6-a326-c2be24b04529 type=spider/withered_mount vehicle=none passengers=[] stage=none since=- speed=0.3000 base=0.3000 ridden=false rooted=false widowed=false ai=false` (read after the kill, as DB6R3's, and nothing in it depends on that); `ENTITIES WITHER_SKELETON count=0 radius=8`; no SKREEL line in the row. Reply `Spawned Withered Mount (spider, 160 HP, GS 100 given)` |

### PS3 — the pounce lands: rooted 20 ticks, the leap, the hit, the push (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| `SKREEL paired … gs=100 speed=0.3900 base=0.3000`. `AT tag=ps3 M x=<x0+10> y=<y0> z=<z0>` (and R's). `DRIVE pounce tag=ps3 M aim=<x0 y0 z0>` (one line: the rider rides its tagged mount and is skipped). **`SKREEL windup rider=R mount=M tick=t0 forced=true at=<x0+10 y0 z0> target=player <Ben> aim=<x0 y0 z0> d=10.000`**. The `AT` ten ticks later: **M unchanged** to three places. **`SKREEL leap rider=R mount=M tick=t0+20 from=<x0+10 y0 z0> windup_from=<x0+10 y0 z0> aim=<x0 y0 z0> v=-0.3448 1.2000 0.0000 ticks=29 horizontal=10.000 gravity=0.0800`**: `from` = `windup_from` to three places is the root. **`SKREEL land … tick=<t0+49 or t0+50> how=landed age=<29, ±1> at=<within 0.25 of x0 y0 z0> aim=<x0 y0 z0> off=<0.000 to 0.250> gs=100 dealt=55.000 reach=3.00 victims=1 [player <Ben> d=<0.75 to 1.25> res=0.000 push=<0.65 to 0.70> 0.4000 <−0.20 to 0.20>] spared=0 []`**, then its **HURT `before=55.000 after=50.000 defense=10.000 dealerType=wither_skeleton/skreel`**, and **no other HURT** (no fall). `SELF customHP=<max − 50>/<max> at=<x greater than x0+2.00> …`: he was pushed east | the leap's T (MobPounceTest: 29, D/29); `v=-10/29` = −0.344828; the held horizontal; the landing rule (he is 1 from it); `MobPounce.push`; the fall table above. **INFERRED, not read:** the pair stands still before the force (STAND in the band); the order of our task and the mount's move in a tick (age 29 either way); the landing's `off` (only a stroll's path could add to it, at the air's 0.02); his client carrying the push (~5 blocks in 11 ticks of air) | **VERBATIM MATCH, 2026-11-17** (x0 = −100.5, y0 = 63, z0 = 4.5; he stood at x0+1 by the scenario's `tp @s ~1`). `SKREEL paired mount=614bc313-f5f3-4b98-ad7a-751b11601c94 rider=8aabc7c3-ef09-4496-8967-fa9a37d75cb8 tick=769 gs=100 speed=0.3900 base=0.3000`; `AT tag=ps3 614bc313-… x=-90.500 y=63.000 z=4.500`; one `DRIVE pounce tag=ps3 614bc313-f5f3-4b98-ad7a-751b11601c94 aim=-100.500 63.000 4.500`; **`SKREEL windup rider=8aabc7c3-ef09-4496-8967-fa9a37d75cb8 mount=614bc313-f5f3-4b98-ad7a-751b11601c94 tick=774 forced=true at=-90.500 63.000 4.500 target=player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 aim=-100.500 63.000 4.500 d=10.000`**; the next `AT tag=ps3 614bc313-… x=-90.500 y=63.000 z=4.500` (unchanged); **`SKREEL leap … tick=794 from=-90.500 63.000 4.500 windup_from=-90.500 63.000 4.500 aim=-100.500 63.000 4.500 v=-0.3448 1.2000 0.0000 ticks=29 horizontal=10.000 gravity=0.0800`** (t0+20; the root held); **`SKREEL land … tick=823 how=landed age=29 at=-100.500 63.000 4.500 aim=-100.500 63.000 4.500 off=0.000 gs=100 dealt=55.000 reach=3.00 victims=1 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 d=1.000 res=0.000 push=0.7000 0.4000 0.0000] spared=0 []`** (t0+49); **`HURT before=55.000 after=50.000 defense=10.000 dealer=8aabc7c3-ef09-4496-8967-fa9a37d75cb8 dealerType=wither_skeleton/skreel`**, the row's only HURT; `SELF customHP=300.000/350.000 at=-95.36 63.00 4.50 onGround=true fall=0.000`, pushed 4.14 east of where he stood |

### PS4 — out of the way in time; a frozen husk at the landing is spared (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| As PS3 to the leap (`d=10.000`, `ticks=29`). **`SKREEL land … how=landed … aim=<x0 y0 z0> … dealt=55.000 reach=3.00 victims=0 [] spared=1 [husk <H> d=<0.75 to 1.25>]`**; **no HURT in the row**; `PROBE tag=ps4h … husk customHP=100.000/100.000`; `SELF customHP=<max>/<max> at=<x0+4.00 y0 z0> …`. After it lands the pair backs off (no line) | he stands 4 from the aim, past the reach of 3 (`MobPounce.landing`); a mob is never a victim; BC4B's husk read 100/100 | **VERBATIM MATCH, 2026-11-17.** `SKREEL windup … tick=871 forced=true at=-90.500 63.000 4.500 … aim=-100.500 63.000 4.500 d=10.000`; `SKREEL leap … tick=891 … v=-0.3448 1.2000 0.0000 ticks=29 horizontal=10.000 gravity=0.0800`; **`SKREEL land rider=9ad74a14-f84b-4fcf-b977-9e493adcdaf0 mount=fba36c21-36d3-48ad-bdda-898aec188b51 tick=920 how=landed age=29 at=-100.500 63.000 4.500 aim=-100.500 63.000 4.500 off=0.000 gs=100 dealt=55.000 reach=3.00 victims=0 [] spared=1 [husk 462ae9ec-d71e-4b57-b26a-04f950fc2169 d=1.000]`**; no HURT in the row; `PROBE tag=ps4h 462ae9ec-d71e-4b57-b26a-04f950fc2169 husk customHP=100.000/100.000 fireTicks=0 dead=false d=4.123 dy=0.000`; `SELF customHP=350.000/350.000 at=-96.50 63.00 4.50 onGround=true fall=0.000` |

### PS5 — separated by a command: the pair breaks, the mount is widowed and keeps 1.3x (SURVIVAL, FROZEN)

| prediction | stands on | READING |
|---|---|---|
| `SKREEL paired …`; `CMD /ride …`; within two ticks **`SKREEL broken mount=M rider=R tick=<t> reason=separated`**; two PAIR lines, either order: **`PAIR tag=ps5 M type=spider/withered_mount vehicle=none passengers=[] stage=none since=- speed=0.3900 base=0.3000 ridden=true rooted=false widowed=true ai=false`** and **`PAIR tag=ps5 R type=wither_skeleton/skreel vehicle=none passengers=[] stage=none since=- speed=0.2500 base=0.2500 ridden=false rooted=false widowed=false ai=false`**; **no SKREEL line at the kill** (the loop is gone) | `MobPouncer.step`'s check, every tick; `widow()` keeps the modifier | **VERBATIM MATCH, 2026-11-17.** `SKREEL paired mount=7d60e610-361f-4065-b7fa-ed70fb7941cc rider=63846470-6353-445a-9275-c21d8642426f tick=983 …`; `CMD /ride @e[tag=ps5,type=wither_skeleton,limit=1] dismount`; **`SKREEL broken mount=7d60e610-361f-4065-b7fa-ed70fb7941cc rider=63846470-6353-445a-9275-c21d8642426f tick=987 reason=separated`** (the `/ride`'s own tick is not logged); `PAIR tag=ps5 7d60e610-361f-4065-b7fa-ed70fb7941cc type=spider/withered_mount vehicle=none passengers=[] stage=none since=- speed=0.3900 base=0.3000 ridden=true rooted=false widowed=true ai=false`; `PAIR tag=ps5 63846470-6353-445a-9275-c21d8642426f type=wither_skeleton/skreel vehicle=none passengers=[] stage=none since=- speed=0.2500 base=0.2500 ridden=false rooted=false widowed=false ai=false`; no SKREEL line at the kill |

### PS6 — the live cycle: its own second wind-up 120 ticks after the first (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| The forced `SKREEL windup … tick=t0 forced=true … aim=<x0 y0 z0>`; its leap; **its land `victims=0 []`** (he stepped 4 east). **Then `SKREEL windup … tick=t0+120 forced=false … target=player <Ben> aim=<x0+4 y0 z0> d=<11.5 to 12.5>`**: exactly 120 after the first began. Its `SKREEL leap … ticks=29 horizontal=<11.5 to 12.5>`; **its land `victims=1 [player <Ben> d=<0.00 to 0.50> …]`** after the heal, its **HURT `before=55.000 after=50.000`**. `PAIR tag=ps6 M … stage=RECOVER since=<55 to 65> … ridden=true …`; `SELF customHP=<max − 50>/<max>`. **No third wind-up** before the kill | `PounceCycle` (the period from the BEGIN; `theNextWindUpIsAHundredAndTwentyTicksAfterThePreviousBegan`). **INFERRED, not read:** that the pair is back in its band by t0+120. After the first landing it stands 4 from him and paths to 12 (8 blocks west) at speed 1.6 × 0.39; a mob's ground speed is about 0.98 × speed² / 0.454 a tick (vanilla's walk, from memory, NOT READ), so it arrives within about 20 ticks, and the margin is the ~70 ticks left | **2026-11-17. MISMATCH: NO SECOND WIND-UP.** Read: `SKREEL windup rider=5c025b96-c9a3-4406-9530-f387f89854c5 mount=03bb1ba7-2c5d-4a36-bd38-7315f81ab5e2 tick=1026 forced=true at=-90.426 63.000 5.164 target=player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 aim=-100.500 63.000 4.500 d=10.096`; `SKREEL leap … tick=1046 from=-90.426 63.000 5.164 windup_from=-90.426 63.000 5.164 aim=-100.500 63.000 4.500 v=-0.3474 1.2000 -0.0229 ticks=29 horizontal=10.096 gravity=0.0800`; `SKREEL land … tick=1075 how=landed age=29 at=-100.325 63.000 4.789 aim=-100.500 63.000 4.500 off=0.338 gs=100 dealt=55.000 reach=3.00 victims=0 [] spared=0 []`. **Then no SKREEL line at all until the kill's `SKREEL broken mount=03bb1ba7-2c5d-4a36-bd38-7315f81ab5e2 rider=5c025b96-c9a3-4406-9530-f387f89854c5 tick=1206 reason=mount-died rider_state=dead`**: the predicted `SKREEL windup … tick=1146 forced=false …`, its leap, its land `victims=1`, and its HURT are all absent. `SELF customHP=350.000/350.000 at=-96.50 63.00 4.50 onGround=true fall=0.000` (predicted max − 50). PAIR, read after the kill as DB6R3's: `PAIR tag=ps6 03bb1ba7-2c5d-4a36-bd38-7315f81ab5e2 type=spider/withered_mount vehicle=none passengers=[5c025b96-c9a3-4406-9530-f387f89854c5] stage=none since=- speed=0.3900 base=0.3000 ridden=true rooted=false widowed=false ai=true` and `PAIR tag=ps6 5c025b96-c9a3-4406-9530-f387f89854c5 type=wither_skeleton/skreel vehicle=03bb1ba7-2c5d-4a36-bd38-7315f81ab5e2 passengers=[] stage=none since=- speed=0.2500 base=0.2500 ridden=false rooted=false widowed=false ai=true`. **The INFERRED clause is what failed.** The mount died at x=-94.60 z=16.80, 12.4459 horizontally from his feet (awk, from the death line's two-place figures): outside the band's 12. `MobPounce.standOff` paths it to EXACTLY `bandFar` (12) and `inBand` requires `≤ bandFar`, so a path that stops short of its exact point leaves the mount just outside the band, in CLOSE_IN, and the wind-up never begins. That vanilla's pathfinder stops short of the point is INFERRED, not read. PQ1 shows the same failure. Also unpredicted: the pair had moved 0.67 before the force (`at=-90.426 63.000 5.164`), so the "stands still" inference failed too |

### PS7 — the mount dies first: the rider on foot, no further wind-up, and the Phage does NOT grow (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| `BODY tag=ps7p <P> type=creeper mob=phage stage=1 …`. After the spider's kill: **`SKREEL broken mount=M rider=R tick=<t> reason=mount-died rider_state=alive dismounted=<x0+10 ± 0.6, z0 ± 0.6>`**, and **no GROWTH line**. PAIR, the rider: **`PAIR tag=ps7 R type=wither_skeleton/skreel vehicle=none passengers=[] stage=none since=- speed=0.2500 base=0.2500 ridden=false rooted=false widowed=false ai=false`** (and the dying mount's own line, `stage=none`, not predicted further). `BODY tag=ps7p … stage=1 …` again. **After the rider's kill: `GROWTH mob=phage id=<P> from=1 to=2 fed=wither_skeleton/skreel R element=void d=<1.8 to 3.0> …`** and `BODY tag=ps7p … stage=2 …`: **the positive control** | the mount has no element (`withered_mount.yml`; the resolver, R1); the rider's is void; growth radius 6 (`phage.yml`); `MobPouncer.stopped` dismounts a dead mount's rider | **VERBATIM MATCH, 2026-11-17.** `BODY tag=ps7p e85545b4-0453-45c9-8ed7-325cd7d5d0f1 type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=80.000 gs=100 element=void dead=false`; **`SKREEL broken mount=0e584a30-147d-4ff5-a233-7b93f54efba0 rider=5a38669c-5d87-4a56-ae0f-ff1ebf8dff48 tick=1242 reason=mount-died rider_state=alive dismounted=-90.500 64.350 4.500`**, and no GROWTH line after it; `BODY tag=ps7p … stage=1 …` again; after the rider's kill, **`GROWTH mob=phage id=e85545b4-0453-45c9-8ed7-325cd7d5d0f1 from=1 to=2 fed=wither_skeleton/skreel 5a38669c-5d87-4a56-ae0f-ff1ebf8dff48 element=void d=2.329 max=100`** and `BODY tag=ps7p … stage=2 scale=0.882 height=1.500 … maxHP=100.000 …`. The rider's PAIR, `PAIR tag=ps7 5a38669c-5d87-4a56-ae0f-ff1ebf8dff48 type=wither_skeleton/skreel vehicle=none passengers=[] stage=none since=- speed=0.2500 base=0.2500 ridden=false rooted=false widowed=false ai=false`, is the predicted line. It printed after the rider's kill (the probe's one-tick lag, see DB6R3), not before the second BODY |

### PS8 — the rider dies first: the Phage DOES grow, the mount alone keeps 1.3x and never winds up (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| After the rider's kill: **`GROWTH mob=phage id=<P> from=1 to=2 fed=wither_skeleton/skreel R element=void d=<1.8 to 3.2> …`**, and **`SKREEL broken mount=M rider=R tick=<t> reason=rider-died`**. `BODY tag=ps8p … stage=2 …`. **`PAIR tag=ps8 M type=spider/withered_mount vehicle=none passengers=[] stage=none since=- speed=0.3900 base=0.3000 ridden=true rooted=false widowed=true ai=true`**, the same 140 ticks later; **no SKREEL line in those 140 ticks**, though he stands 10 from it, in what was its band. At the kill no SKREEL line | `MobPouncer.step` (rider dead: widow, end); a widowed mount has no loop. **The clause is not hollow:** the mount has its AI and he is in its band, so a loop that survived would wind up at its 120th tick | **VERBATIM MATCH, 2026-11-17.** `SKREEL broken mount=b0319800-15fd-4d4a-a84d-d8621019c831 rider=4660a486-a04d-4370-a5bc-8ba6cf5cc96a tick=1327 reason=rider-died`, then `GROWTH mob=phage id=4acb4388-2864-4cab-ac9e-a550395ba120 from=1 to=2 fed=wither_skeleton/skreel 4660a486-a04d-4370-a5bc-8ba6cf5cc96a element=void d=2.018 max=100`; `BODY tag=ps8p … stage=2 …`; `PAIR tag=ps8 b0319800-15fd-4d4a-a84d-d8621019c831 type=spider/withered_mount vehicle=none passengers=[] stage=none since=- speed=0.3900 base=0.3000 ridden=true rooted=false widowed=true ai=true` at 18:58:28 and the identical line at 18:58:35, 140 ticks later. The second printed after the kill, by the probe's lag. No SKREEL line between them or at the kill. The mount wandered: at its death it was at x=-97.67 z=13.75, 9.6732 from his feet (awk), inside the old band |

### PB1 — the cooldown: no decision for 239 ticks after a landed blink, one on the 240th (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| `DRIVE blink tag=pb1 <X> angle=90.000 distance=8.000`; **`BLINK mob=phalanx id=<X> tick=L forced=true trigger=none from=<x0+8 y0 z0> tried=1 [90.000 8.000 OK at=<x0+8 y0 z0+8>] landed=<x0+8 y0 z0+8> anchor=<y0>.000 cooldown=0`**; **`BLINK moved … tick=L at=<x0+8 y0 z0+8> invisible=true cooldown=240`**. Its first shot (`SHOT fired … forced=false`, about L+117) comes with **`BLINK visible … reason=fired`** and lands on him (HURT 40 → 36.364). **No BLINK line from L+1 to L+239**, though he stands 3 from it, inside its box, and it is visible from its first shot on. **Exactly one `BLINK mob=phalanx id=<X> tick=L+240 forced=false trigger=player <Ben> from=<x0+8 y0 z0+8> … anchor=<y0>.000 cooldown=0`** with `landed=` not none, and its `BLINK moved … cooldown=240`. A second shot (about L+237) and its HURT. `BOW suppressed` lines are not predicted and none is a failure | `MobBlink.Cooldown` (`afterALandedBlinkItRuns…`); the step's order (counted first, the block after the AI and invisibility checks); its speed 0 keeps it inside his reach; 8 attempts from the uniform draw land on this open floor (INFERRED) | **VERBATIM MATCH, 2026-11-17** (L = 1565). `BLINK mob=phalanx id=180ffdfb-c20d-469e-83f2-9ba4fdc70681 tick=1565 forced=true trigger=none from=-92.500 63.000 4.500 tried=1 [90.000 8.000 OK at=-92.500 63.000 12.500] landed=-92.500 63.000 12.500 anchor=63.000 cooldown=0`; `BLINK moved … tick=1565 at=-92.500 63.000 12.500 invisible=true cooldown=240`; `BLINK visible … tick=1682 reason=fired` with `SHOT fired … tick=1682` (L+117) and `HURT before=40.000 after=36.364 defense=10.000 dealer=180ffdfb-… dealerType=wither_skeleton/phalanx`; **no BLINK line from 1566 to 1804**; **`BLINK mob=phalanx id=180ffdfb-c20d-469e-83f2-9ba4fdc70681 tick=1805 forced=false trigger=player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 from=-92.500 63.000 12.500 tried=1 [180.536 15.807 OK at=-108.307 62.000 12.352] landed=-108.307 62.000 12.352 anchor=63.000 cooldown=0`** (L+240, the only one); `BLINK moved … tick=1805 … cooldown=240`; the second `SHOT fired … tick=1802` (L+237) and its HURT 40 → 36.364. Not predicted: the landing is at y=62, on the world's own ground west of the staged floor (the floor starts at x0−6), one block below its feet and within `level_tolerance` 3 |

### PB2 — a failed attempt does not start it (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| **`BLINK … tick=<t> forced=true trigger=none from=<x0+8 y0 z0> tried=8 [90.000 8.000 NO_FOOTING blocked=0 ungrounded=7, …eight times] landed=none anchor=<y0>.000 cooldown=0`**; no `BLINK moved`. He steps to 3 west of it. **Within two ticks, exactly one `BLINK … forced=false trigger=player <Ben> from=<x0+8 y0 z0> … landed=<not none> anchor=<y0>.000 cooldown=0`** and its `BLINK moved … cooldown=240`. No SHOT line | the pit (BB2's, read `ungrounded=7`); only a landing calls `landed()` (`onlyALandingStartsItAndZeroMeansNone`); a uniform draw falls in the 3×3 pit with probability about 9 / 691, so eight all failing is ~10⁻¹⁵ | **VERBATIM MATCH, 2026-11-17.** `BLINK mob=phalanx id=7c357b6a-40c6-46f2-af0f-fa1383007b51 tick=1880 forced=true trigger=none from=-92.500 63.000 4.500 tried=8 [ … ] landed=none anchor=63.000 cooldown=0`, the bracket holding eight identical entries `90.000 8.000 NO_FOOTING blocked=0 ungrounded=7`; no `BLINK moved`; **`BLINK … tick=1882 forced=false trigger=player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 from=-92.500 63.000 4.500 tried=1 [97.894 12.874 OK at=-94.268 63.000 17.252] landed=-94.268 63.000 17.252 anchor=63.000 cooldown=0`** (two ticks later); `BLINK moved … tick=1882 … cooldown=240`; no SHOT line |

### PB3 — R3: a blink decided at a fractional height lands; a forced blink during the cooldown says "ignored" (SURVIVAL, FROZEN)

| prediction | stands on | READING |
|---|---|---|
| `AT tag=pb3 <X> x=<x0+8> y=<y0+0.500> z=<z0>` (on the slab). **`BLINK … tick=L1 forced=true trigger=none from=<x0+8 y0+0.500 z0> tried=1 [90.000 8.000 OK at=<x0+8 y0 z0+8>] landed=<x0+8 y0 z0+8> anchor=<y0>.000 cooldown=0`**, `BLINK moved … at=<x0+8 y0 z0+8> … cooldown=240`. **Then `BLINK … tick=L2 forced=true trigger=none from=<x0+8 y0 z0+8> tried=1 [180.000 8.000 OK at=<x0 y0 z0+8>] landed=<x0 y0 z0+8> anchor=<y0>.000 cooldown=<240 − (L2 − L1)> ignored`**, expected `cooldown=235 ignored` (L2 − L1 = 5, ±1 on the force's tick); `BLINK moved … at=<x0 y0 z0+8> … cooldown=240`. The last `AT` at x0 y0 z0+8. **On master this row's first BLINK reads `tried=8 […NO_FOOTING blocked=3 ungrounded=4…] landed=none`**: the row discriminates | `MobBlink.anchor` (floor of 63.5 is 63); `aPhalanxOnASlabFailsFromItsFeetAndLandsFromItsBlock`; the forced branch ignores the cooldown | **VERBATIM MATCH, 2026-11-17.** `AT tag=pb3 ea4abcc7-7101-4ec7-87cc-3be1a8a0f594 x=-92.500 y=63.500 z=4.500`; **`BLINK mob=phalanx id=ea4abcc7-7101-4ec7-87cc-3be1a8a0f594 tick=1923 forced=true trigger=none from=-92.500 63.500 4.500 tried=1 [90.000 8.000 OK at=-92.500 63.000 12.500] landed=-92.500 63.000 12.500 anchor=63.000 cooldown=0`**; `BLINK moved … tick=1923 at=-92.500 63.000 12.500 invisible=true cooldown=240`; **`BLINK … tick=1928 forced=true trigger=none from=-92.500 63.000 12.500 tried=1 [180.000 8.000 OK at=-100.500 63.000 12.500] landed=-100.500 63.000 12.500 anchor=63.000 cooldown=235 ignored`**; `BLINK moved … tick=1928 at=-100.500 63.000 12.500 invisible=true cooldown=240`; `AT tag=pb3 … x=-100.500 y=63.000 z=12.500` |

## BEN'S ROWS — BY THE ASK STEP, SURVIVAL

### PQ1 — the Skreel: see it, dodge it, take one (SURVIVAL, LIVE)

| AUTO half, recorded beside his answers | Ben's half | READING |
|---|---|---|
| Every `SKREEL` line and every HURT in the row. **Not predicted: it is his play.** Each wind-up is at least 120 ticks after the last; every HURT is `dealerType=wither_skeleton/skreel`, `before=55.000` | `PQ1 ASK 1 question="Could you see the Skreel wind up before it leapt?"`; `ASK 2 question="Could you get out of the way in time?"`; `ASK 3 question="Did the landing hit feel right for a pounce?"` | **2026-11-17, in tick order.** Paired at tick 1963 (`SKREEL paired mount=f4d4bfe0-a761-4e25-a1be-dd192701cb60 rider=787d42b3-2a31-4a5e-aa3e-77ff497e078a tick=1963 gs=100 speed=0.3900 base=0.3000`), killed by the scenario at 18:59:27 (`SKREEL broken … tick=2562 reason=mount-died rider_state=dead`): 599 ticks live. **One wind-up in those 599 ticks, 473 ticks after the pairing**, though the gold text told him "pounces every 6 seconds": `SKREEL windup rider=787d42b3-2a31-4a5e-aa3e-77ff497e078a mount=f4d4bfe0-a761-4e25-a1be-dd192701cb60 tick=2436 forced=false at=-86.393 63.000 15.624 target=player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 aim=-95.042 63.000 7.401 d=11.935`; `SKREEL leap … tick=2456 from=-86.393 63.000 15.624 windup_from=-86.393 63.000 15.624 aim=-95.042 63.000 7.401 v=-0.2983 1.2000 -0.2836 ticks=29 horizontal=11.935 gravity=0.0800`; `SKREEL land … tick=2485 how=landed age=29 at=-95.042 63.000 7.401 aim=-95.042 63.000 7.401 off=0.000 gs=100 dealt=55.000 reach=3.00 victims=1 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 d=0.765 res=0.000 push=-0.5614 0.4000 -0.4181] spared=0 []`; `HURT before=55.000 after=50.000 defense=10.000 dealer=787d42b3-2a31-4a5e-aa3e-77ff497e078a dealerType=wither_skeleton/skreel`. The row's only HURT. "At least 120 apart" holds only vacuously. The wind-up began at d=11.935, just inside the band, after 473 ticks outside it: PS6's defect. **His answers, verbatim:** `PQ1 ASK 1 question="Could you see the Skreel wind up before it leapt?" answer="yes" ticks=65`; `PQ1 ASK 2 question="Could you get out of the way in time?" answer="I didn't try to" ticks=168`; `PQ1 ASK 3 question="Did the landing hit feel right for a pounce?" answer="yes" ticks=104`. He saw one pounce, not the several the text promised |

### PQ2 — the Phalanx with its 12-second cooldown (SURVIVAL, LIVE)

| AUTO half | Ben's half | READING |
|---|---|---|
| Every `BLINK`, `BLINK moved`, `BLINK visible`, `SHOT` and `SHIELD` line. **Not predicted.** Every own `BLINK` decision is at least **240 ticks** after the last `BLINK moved` | `PQ2 ASK 1 question="Does the Phalanx's blink feel right with the 12-second cooldown?"` | **2026-11-17, in tick order** (the Phalanx `ed4f5139-008d-41a3-af42-f658108ff3c0`; `BOW suppressed` at 2954, 3014 and 3058 not repeated): `BLINK mob=phalanx id=ed4f5139-008d-41a3-af42-f658108ff3c0 tick=3006 forced=false trigger=player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 from=-89.699 63.000 2.069 tried=1 [102.743 8.044 OK at=-91.474 63.000 9.915] landed=-91.474 63.000 9.915 anchor=63.000 cooldown=0`; `BLINK moved … tick=3006 at=-91.474 63.000 9.915 invisible=true cooldown=240`; twice `SHIELD knockback cancelled mob=ed4f5139-008d-41a3-af42-f658108ff3c0 cause=ENTITY_ATTACK shield=up` (one hit, two events: R5 again); `SHIELD absorbed mob=phalanx id=ed4f5139-008d-41a3-af42-f658108ff3c0 tick=3038 amount=56.000 by=dealer=b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 element=fire amount=56.000`; `BLINK visible … tick=3050 reason=fired`; `SHOT fired … tick=3050 … dealt=40.000`; `SHOT hit … age=3 … victim=player b6ae27e9-… dealt=40.000`; `HURT before=40.000 after=36.364 defense=10.000 dealer=ed4f5139-008d-41a3-af42-f658108ff3c0 dealerType=wither_skeleton/phalanx`. He killed it at 18:59:54 (`Phalanx was slain by BaronVonYeetus using [Emberblade]`), inside its cooldown. So it made one own blink and the 240-tick clause holds only vacuously. **His answer, verbatim:** `PQ2 ASK 1 question="Does the Phalanx's blink feel right with the 12-second cooldown?" answer="yes, but it needs to have a longer teleport range. Say 12-24 blocks" ticks=341`. The `phalanx.yml` blink draws `near: 6` / `far: 16` today. **His 12-24 is recorded, not built** |

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

## THE BOOT OF 2026-11-17 — WHAT RAN, AND WHAT IT FOUND

**Carried state.** No JVM before the boot. Backup `C:\Users\Neb91\IdeaProjects\rpg-run-backups\2026-11-17-pre-dragon-skreel`:
`run/plugins/Rpg` 129 files 394429 bytes, and the copy the same, `diff -rq lines: 0`; `run/world` 193 files 204963959 bytes,
and the copy the same, `diff -rq lines: 0`. `grep -n` printed `12:  dev-pack-on-join: true`. Booted with
`./scripts/dev-server.sh --refresh-content` on Ben's "ready" and the seat's approval at `45bc33ce`. Build line
`[Rpg] Build: 45bc33ce`. LOGIN `BaronVonYeetus … logged in … at 18:57:23`, so every reading above is after it.

**Difficulty and restore, verbatim:** `SELFTEST dragon-skreel setup DIFFICULTY found=NORMAL set=NORMAL`;
`SELFTEST RUN RESTORED xp=712595 cell=mage/fire gamemode=SURVIVAL held=3 mana=100.0 trace=false spawn_mobs=false time=6000 blocks=143613 mobsRemoved=0 orbsRemoved=0 keep_inventory=false immediate_respawn=false difficulty=NORMAL`.

**What Ben typed, from the log's `issued server command` lines, and nothing else:** `/gamemode survival` (18:57:44),
`/rpg selftest dragon-skreel` (18:57:54), `/stop` (19:00:52). `SELFTEST RUN FINISHED` at 19:00:43. **Departure, recorded
apart:** he logged in at 18:57:23, while R0 was being read. R0a, R0c and R0d were read in the minute after `Done`, and R0b
after them. Nothing records whether all four were read before his first command at 18:57:44. All four PASS, so no reading
changes.

**Every HURT line and what dealt it:** PS3 `wither_skeleton/skreel` 55 → 50; PB1 `wither_skeleton/phalanx` 40 → 36.364,
twice; PQ1 `wither_skeleton/skreel` 55 → 50; PQ2 `wither_skeleton/phalanx` 40 → 36.364. **No fall damage:** no other HURT,
and every SELF read `fall=0.000`. **LEFTOVER and LINGER:** every one read `living=0` and `work=0 entities=0`. **Refusing,
Skipping, SEVERE, Exception:** none. **WARN:** `volley_stone`'s known pair and Paper's version notice only. **The stop was
clean:** `Saving worlds`, `All dimensions are saved`, the build script exited 0, and no java process remained.

**THE SCORE: 9 of 11 AUTO rows verbatim; 2 mismatches; Ben's 4 answers recorded.**

- **THE BAND EDGE, a DEFECT, found by PS6 and shown again by PQ1: the pair parks just outside its band and never winds up.**
  `MobPounce.standOff` aims the path at exactly `bandFar` (12) from him. `inBand` needs `distance ≤ bandFar`. PS6's mount
  died 12.4459 from him after 180 ticks without a second wind-up. PQ1's pair took 473 ticks to start its only wind-up, at
  d=11.935. That vanilla's pathfinder ends short of the exact point is INFERRED and has not been read from the jar. Not
  fixed here. The fix and its test belong to the seat's next brief.
- **THE PROBE LAG, an INSTRUMENT DEFECT, found by DB6R3: `PROBE_PAIR` reads one tick late.** It logs through
  `run.scheduler().onEntity`, so a `kill` in the scenario's next step lands first. DB6R3's and PS6's PAIR lines show a dead
  pair's `stage=none since=-`. DB7R3, PS5, PS7 and PS8 predicted fields that do not change at death, and match.
- **Ben's PQ2 answer asks for a longer blink range, "Say 12-24 blocks", against today's `near: 6` / `far: 16`.**
  Recorded, not built; the number is his to rule.
  Neither has a PLAN number yet: `PLAN-dragon-mobs.md` already cites `PLAN-mob-scaling.md`'s F22, so the plan assigns
  their numbers when it records them.

---

# SECOND ROUND — RE-PREDICTED AFTER THE BOOT OF `45bc33ce`, 2026-11-17. **Status: NOT RUN.**

Everything below was written **before** any boot of this round, and is committed before it. **The predictions and
readings above are NOT edited.** The rows that failed or change are re-predicted here under their old name with the next
suffix; the rows that read as predicted and whose lines do not change are **not re-run** and have left the scenario file:
**DB7R3, PS3, PS4, PS5, PS7, PS8, PB3.** The seat's brief of 2026-11-17 and its rulings are recorded in
`PLAN-dragon-mobs.md` (RULINGS, *Ben, … the boot of `45bc33ce`* and *THE SEAT'S RULINGS AFTER THE BOOT OF `45bc33ce`*;
F22 and F23).

> **2026-11-17, BEFORE ANY BOOT OF THIS ROUND: THIS SECTION'S PREDICTIONS ARE REWRITTEN IN PLACE, BY THE SEAT'S LEAVE.**
> The seat's third brief on #203 ("the pair must end inside its band from any bearing") found that `4ba51506`'s goal,
> the middle point's own block, can leave the 2.1-wide mount stopped outside the band from a diagonal, so that it leaps
> only by the cannot-reach rule. This round had **not been booted or read**, so the seat allowed its predictions to be
> rewritten in this PR rather than re-predicted under new names. **Rewritten:** items 2 and 3 below, and new item 7; the
> survival table; R0b2 and R0d2; **PS6R2** (its wind-ups now at the normal period), **PS9**, **PS10** (the cannot-reach
> rule now measured by progress: P + 121, not P + 240); **added: PS11** (the diagonal); and the run sheet's length.
> Everything above this section, its predictions and its readings, is untouched.

## WHAT CHANGED, AND WHAT EACH CHANGE STANDS ON

1. **THE BAND EDGE (F22), FIXED.** The stand-off point is the band's **middle**, (8 + 12) / 2 = **10**, never an edge
   (`MobPounce.standOffDistance`). A pair nearer than 8 moves OUT to the same point; inside the band it holds.
2. **HOW NEAR THE PATHFINDER TAKES A MOB TO ITS GOAL, READ** (`javap -c -p -cp run/versions/26.1.2/paper-26.1.2.jar
   com.destroystokyo.paper.entity.PaperPathfinder` and `… net.minecraft.world.entity.ai.navigation.PathNavigation`):
   `PaperPathfinder#findPath(Location)` calls `createPath(x, y, z, 0)`, a path to the goal's own block;
   `PathNavigation#followThePath` sets `maxDistanceToWaypoint` to width / 2 when the width is over 0.75 (else 0.75 −
   width / 2), and passes a node when the mob is under that from the node's block centre on x and on z (and under 1 on
   y); `shouldTargetNextNodeInDirection` returns false on the last node, so the last is never passed early. **So a path
   is done with the mob under width / 2 + 0.5 from its goal on each axis.** The mount is 1.4 × 1.5 = 2.1 wide (READ,
   `EntityType` spider `sized(1.4f, 0.9f)`; DB6R3 read `height=1.350`, 0.9 × 1.5): **1.55 on an axis, 2.19 on a
   diagonal. THE BAND'S HALF-WIDTH, 2, EXCEEDS THE FIRST AND NOT THE SECOND.** So the middle point's own block is NOT
   the goal: **`MobPounce.goal` chooses, among the blocks within 3 of the middle point's, the one nearest it whose
   stopping box (that block's centre, ± the mount's tolerance 1.05 on each axis, grown by a CHOSEN margin of 0.25) lies
   wholly inside the band.** Then the walk always crosses into the band before its path can end, and the cycle stands it
   there the tick it does; a path that did end would end inside the band. **Why this mechanism of the seat's three:** it
   is the only one that needs no second walk (a correcting second goal costs a whole extra approach and still meets the
   same rule) and no vanilla internals (the mount's move control is not in the Bukkit API; reaching it would be NMS,
   banned here); it works on the rule as READ, at the one place the pair chooses where to go. Its price: the chosen
   block can sit up to 3.52 blocks from the middle point, so the pair circles a little off its bearing. Pinned in
   `MobPounceTest` (the sweep, below).
3. **THE CANNOT-REACH RULE (the seat's ruling; CHOSEN, for Ben), MEASURED BY PROGRESS.** A pair with its AI and a target
   within its acquire range (32), outside the band, that has not come **1 block nearer the band** (`MobPounce.bandGap`;
   `PROGRESS_BLOCKS`, CHOSEN) for a full period (120 ticks), and is ready, winds up **from where it stands**. A tick in
   the band, with no target or with no AI starts the wait again, and so does a wind-up. **A pair walking to its band in
   good faith never counts** (a walking mount covers a block in about two ticks), so the backstop is not what makes an
   ordinary approach work. A pair stuck from its first tick winds up on its 121st; one that stalls winds up a period
   after its last block of progress. **What it means in play, for Ben:** a Skreel that cannot get to its band (a wall,
   a pit) still leaps about every 6 seconds, from any distance up to 32, and the leap still comes down on the spot it
   aimed at (a leap of 30 flies about 1 block a tick). So does one chasing a player who runs from it faster than it
   closes, which is the rule working as meant.
4. **BEN'S BLINK RANGE, RULED: 12 TO 24** (`phalanx.yml` `blink.near: 12`, `blink.far: 24`). The uniform draw is
   near + u × (far − near) with u in [0, 1), so a draw nearer than 12 is impossible by construction (`MobBlinkRangeTest`:
   its two ends and 100 000 seeded draws). **The instrument's forced draw is a fixed point, not bound by near and far**,
   as the Knell's `teleport_draw` is: the merged forced rows' lines do not change.
5. **WHAT A 24-BLOCK BLINK DOES TO ITS SHOTS AND ITS TARGET.**
   - **A shot already in flight:** nothing. The blink is the last thing in the Phalanx's tick, after its fire and every
     shot's step; a shot keeps its own target, its origin, its life of 240 and its 35 from the origin.
   - **Its next shot:** a blink is triggered by a body within 5 of its feet, so after a landing 24 out that body's eye can
     be **33.1** from the Phalanx's eye (`MobBlinkRangeTest`: 24 + 5.3 × √2 across, 10.28 up or down), **past the
     acquire range of 30**. At X5's 16 it was at most 25.6. **Built (CHOSEN, put to Ben): from a blink that LANDS until
     its next shot, it chooses whom to fire at out to the re-acquire range, 35** (`MobShot#chooseRange`); its first shot
     puts it back on 30. So it keeps a target. PB4 reads both halves.
   - **Its vanilla target:** a wither skeleton's follow range is 16 (READ: `DefaultAttributes` maps `WITHER_SKELETON` to
     `AbstractSkeleton#createAttributes`, which adds only movement speed to `Mob#createMobAttributes`' `FOLLOW_RANGE
     16.0`), and `TargetGoal#canContinueToUse` drops a target farther than that. After most blinks of 12 to 24 its
     vanilla target drops, as it often did at 16. The shot's choice does not depend on it.
   - **The landing check's chunk rule:** a column whose chunk is not loaded is refused like any other unfit point (the
     Knell's port, `MobTeleporter.footing`). A landing 24 out is within 31.5 blocks of a player who triggered it, and
     this server's view and simulation distances are 10 chunks (`run/server.properties`), so it is never refused for a
     natural blink here.
6. **THE PROBE LAG (F23), FIXED BY WAITING.** The pair and shot probes still read on the mob's own thread, a tick later,
   so every reading already taken keeps its tick. A row must now wait 2 ticks between such a probe and a kill, and the
   loader refuses one that does not (`SelfTestStaging.probeLag`). **Applied wherever the lag could hide a reading:** every
   `probe_shot` followed by a kill in `GATE-dragon-shot.yml` (SH1R2, SH2R2, SH3R2, SH7R2, SH13, SH14, SH15, SH16) and
   `GATE-dragon-blink.yml` (BC2B, BC3), and this gate's own rows. Those merged rows are not re-run; on a re-run their
   probes read the same tick as before and their kills come 2 ticks later. Their dated notes say so.
7. **THE TARGET MOVES.** The goal is recomputed **every tick** the pair is outside its band (the CLOSE_IN step calls
   `MobPounce.goal` from where he stands that tick and hands it to the pathfinder at once; pinned in
   `DragonSkreelWiringTest`), and the cycle stands the pair the first tick it is inside the band. So a pair chasing a
   walking player follows his feet tick by tick; when he stops, the next goal is a block whose stopping box lies in the
   band round where he stands, and it settles there by the rule the sweep proves. A pair standing in its band while he
   walks stays put until he leaves the band, then closes in again. **A wind-up, once begun, is unaffected:** its aim is
   fixed at its first tick (as built in the first round) and it always leaps.

## THE PLAYER CANNOT DIE IN ANY ROW, THE FALL INCLUDED

| row | what can reach him | most between heals | his floor |
|---|---|---|---|
| DB6R4 | nothing (frozen) | 0 | 350 |
| PS6R2 | the pair, healed every 100 ticks; wind-ups 120 apart | one landing: 55, 50 after defence | 300 |
| PS9, PS10, PS11 | the pair, healed before its one landing | one landing, 50 | 300 |
| PB1R2 | its shots (period 120), healed at +145 and +265 | two 40s, 36.364 each | 277.3 |
| PB2R2 | nothing (no shot in 15 ticks) | 0 | 350 |
| PB4 | one forced shot, fired 32 away and ended by the kill ~10 ticks later (0.2 a tick: it cannot arrive) | 0 | 350 |
| BB4R2 | nothing (no shot in 30 ticks) | 0 | 350 |
| PQ3 | the pair, healed every 100 ticks | one landing, 50 | 300 |
| PQ4 | its shots, healed every 200 ticks | two 40s, 72.7 | 277.3 |

His floor is 100 (his max 350). **The fall:** a landing pushes at most 0.4 up, an apex of 1.153 blocks (the first
round's arithmetic), under the 3-block safe fall; a landing on his feet pushes nothing sideways (`MobPounce.push`: no
horizontal direction, straight up). Every survivor that could reach him is killed at the row's end.

## R0 FOR THIS ROUND — READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a2 — the build line names THIS round's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-skreel` prints it, shortened. **Not** `45bc33ce` (the first round's boot) and not `1c18e862` (its readings); not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1`, after this boot's `Done` | |

### R0b2 — the jar carries the fix, Ben's range and the new rows; the controls tell it from `45bc33ce` AND from `4ba51506`

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `phalanx.yml` `  near: 12`, `  far: 24`; `MobPounce.class` `standOffDistance`, `waypointTolerance`; `PounceCycle.class` `bestGap`; `MobShot.class` `chooseRange`; `SelfTestStaging.class` `the probe lag`; `GATE-dragon-skreel.yml` `PS10:`, `PS11:`, `PB4:`. **ABSENT, and PRESENT in `45bc33ce`'s jar:** `phalanx.yml` `  near: 6`, `  far: 16`; `GATE-dragon-skreel.yml` `PS3:`. **Telling it from `4ba51506`** (the second round as first pushed, never booted): there `waypointTolerance`, `bestGap` and `PS11:` read ABSENT and `PounceCycle.class` `stranded` PRESENT; here the reverse. **The controls, PRESENT in both:** `phalanx.yml` `  cooldown_ticks: 240`; `health/MobPouncer.class`; `GATE-dragon-blink.yml` `BQ1:` | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
foreach ($q in @(@('phalanx.yml', '  near: 12'), @('phalanx.yml', '  far: 24'), @('phalanx.yml', '  near: 6'), @('phalanx.yml', '  far: 16'), @('phalanx.yml', '  cooldown_ticks: 240'))) {
  $y = Bytes "content/mobs/$($q[0])"
  if ($y -ne $null -and $y -match ("(?m)^" + [regex]::Escape($q[1]) + "\s*$")) { "PRESENT $($q[0]) '$($q[1])'" } else { "ABSENT  $($q[0]) '$($q[1])'" }
}
foreach ($c in @(@('core/mob/MobPounce', 'standOffDistance'), @('core/mob/MobPounce', 'waypointTolerance'), @('core/mob/PounceCycle', 'bestGap'), @('core/mob/PounceCycle', 'stranded'), @('core/mob/MobShot', 'chooseRange'), @('paper/selftest/SelfTestStaging', 'the probe lag'), @('paper/health/MobPouncer', 'skreel-pounce'))) {
  $b = Bytes "io/github/butterflysmp/rpg/$($c[0]).class"
  if ($b -eq $null) { "MISSING $($c[0]).class" } elseif ($b.Contains($c[1])) { "PRESENT $($c[0]).class '$($c[1])'" } else { "ABSENT  $($c[0]).class '$($c[1])'" }
}
foreach ($q in @(@('dragon-skreel', 'PS10'), @('dragon-skreel', 'PS11'), @('dragon-skreel', 'PB4'), @('dragon-skreel', 'PS3'), @('dragon-blink', 'BQ1'))) {
  $y = Bytes "selftest/GATE-$($q[0]).yml"
  if ($y -eq $null) { "MISSING selftest/GATE-$($q[0]).yml" } elseif ($y -match "(?m)^$($q[1]):\s*$") { "PRESENT $($q[0]) $($q[1]):" } else { "ABSENT  $($q[0]) $($q[1]):" }
}
$zip.Dispose()
```

`45bc33ce`'s jar reads the opposite on the six new lines (`near: 12`, `far: 24`, `standOffDistance`, `chooseRange`,
`the probe lag`, `PS10:`/`PB4:`) and on the three old ones (`near: 6`, `far: 16`, `PS3:`); the controls are PRESENT in
both. (`  near: 6` is anchored to the line, so `  near: 12` cannot match it; the same for `far`.) **And `4ba51506`'s jar**
reads PRESENT on `stranded` and ABSENT on `waypointTolerance`, `bestGap` and `PS11:`, the reverse of this round's; every
other line as this round's. Both directions were run before this was committed (the report says on which jars).

### R0c2 — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim the first round's (this round adds no content file; it changes one). No `Refusing`, `Skipping`, `SEVERE` or `Exception`; no `[Rpg]` `WARN` beyond `volley_stone`'s known pair. **A `Refusing selftest/…: … (the probe lag)` line would mean a shipped row kills within 2 ticks of a probe** | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception'` | |

### R0d2 — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 155 scenarios across 20 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel]`**, no `Refusing selftest` | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | |

**The 155 is RECOUNTED from the files at this round's head**, the first round's commands: in
`paper/src/main/resources/selftest/`, the index's gates (`grep -cE '^  - ' index.yml`) **20**; their top-level keys
(`^[A-Za-z0-9-]+:`) **175**; their setups **20**; so **155**. At `4ba51506` the same commands print 20, 174, 20: 154; at `1c18e862`, 20, 177, 20: 157. This
gate's file holds 12 keys: its setup and the 11 rows below.

## THE ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**As the first round:** every row starts `LEFTOVER living=0 radius=16` and `LINGER work=0 entities=0 radius=48`; P0 =
his feet at the row's start, (x0, y0, z0); P the `SKREEL paired … tick=P` line's tick; a pair's loop takes its first step
on P + 1 (`RepeatingTask.start` schedules its first step one period, 1 tick, later), so its 120th step is P + 120. A pair
killed by one `kill` logs `SKREEL broken … reason=mount-died rider_state=dead`, not repeated below.

### DB6R4 — one spawn makes two, read LIVE (SURVIVAL, FROZEN)

| prediction | stands on | READING |
|---|---|---|
| As DB6R3, tag `db6r4`: `SKREEL paired … gs=300 speed=0.3900 base=0.3000`; the two BODY lines DB6R3 read, field for field; `ENTITIES SPIDER count=1 radius=8`. **The two PAIR lines BEFORE the kill's `SKREEL broken`**, either order: **`PAIR tag=db6r4 M type=spider/withered_mount vehicle=none passengers=[R] stage=RECOVER since=<4 to 7> speed=0.3900 base=0.3000 ridden=true rooted=false widowed=false ai=false`** and **`PAIR tag=db6r4 R type=wither_skeleton/skreel vehicle=M passengers=[] stage=RECOVER since=<4 to 7> speed=0.2500 base=0.2500 ridden=false rooted=false widowed=false ai=false`** | DB6R3's prediction and its reading; the `wait: 2` (F23). **The row discriminates:** without the wait it reads `stage=none since=-`, as DB6R3 did | |

### PS6R2 — the pair's own cycle, never forced, at the normal period (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| `SKREEL paired … tick=P gs=100 speed=0.3900 base=0.3000`. **`SKREEL windup … forced=false … target=player <Ben> … d=<8.000 to 12.000>` at EXACTLY P + 120, P + 240, P + 360, P + 480 and P + 600**: every one 120 after the last began, the normal period, and every one from inside the band; the first `at=<x0+10 ± 1, y0, z0 ± 1>` (it stands in its band from the start). Each of the first four's `SKREEL leap … ticks=29 …` (level ground), its **`SKREEL land … how=landed … victims=1 [player <Ben> d=<0.000 to 0.500> res=0.000 push=… 0.4000 …] spared=0 []`** and its **HURT `before=55.000 after=50.000 defense=10.000 … dealerType=wither_skeleton/skreel`** (the fifth's leap falls after the kill). No other HURT. **No wind-up with a `d` outside 8 to 12, and none at any other tick:** either would be the cannot-reach rule, which an ordinary approach must never need (the seat's third brief). `PAIR tag=ps6r2 M … stage=<WINDUP or HOLD> … ridden=true rooted=false widowed=false ai=true`, before the kill; `SELF customHP=350.000/350.000` (healed at about P + 600, after the fourth landing) | `PounceCycle` (`theNextWindUpIsAHundredAndTwentyTicksAfterThePreviousBegan`); `MobPounce.goal` (`fromEveryBearingAndDistanceThePairRestsInsideTheBand`); the landing on his feet. **INFERRED, not read:** each landing (49 after its wind-up began) puts the mount at his feet; it walks out, crosses the band's near edge and is stood there (PS3 read the mount moving 0.65 a tick: 8 blocks in about 13 ticks), long before its next 120 | |

### PS9 — too near: it moves out, and winds up from inside its band (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| `AT tag=ps9 M x=<x0+4 ± 0.1> y=<y0> z=<z0 ± 0.1>` two ticks after the spawn. 100 ticks later **`AT tag=ps9 M` between 8 and 12 from his feet** (it walks out toward its goal block, crosses the band's near edge and is stood there, then slides a little: item 2). **`SKREEL windup … tick=P+120 forced=false … d=<8.000 to 12.000>`**, the first wind-up of the row and the only one; its leap and its land `victims=1 [player <Ben> …]`; its HURT 55 → 50. `SELF customHP=300.000/350.000` | `PounceCycle` (`insideTheBandItHoldsAndNearerThanItItMovesOut`); `MobPounce.goal` (the sweep's starts at 0.5, 3, 6 and 7.9). **INFERRED:** it walks the 4 blocks to the near edge in well under 100 ticks | |

### PS10 — it cannot reach the band: it winds up from where it stands, a period after its last progress (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| Its mount's movement speed 0 (the `attribute` command's reply). `AT tag=ps10 M x=<x0+20> y=<y0> z=<z0>` two ticks after the spawn, and **the same to three places** 100 ticks later. **No `SKREEL windup` from P + 1 to P + 120** (at P + 120 it is ready, and has gone 119 ticks without progress). **Then `SKREEL windup … tick=P+121 forced=false at=<x0+20 y0 z0> target=player <Ben> aim=<x0 y0 z0> d=20.000`**; `SKREEL leap … from=<x0+20 y0 z0> … v=-0.6897 1.2000 0.0000 ticks=29 horizontal=20.000 gravity=0.0800`; **`SKREEL land … how=landed age=29 at=<x0 y0 z0 ± 0.25> … victims=1 [player <Ben> d=<0.000 to 0.250> res=0.000 push=<…> 0.4000 <…>] spared=0 []`**; HURT 55 → 50; `SELF customHP=300.000/350.000` | `PounceCycle` (`aPairThatMakesNoProgressTowardTheBandForAPeriodWindsUpFromWhereItStands`, at 20); `MobPounce.leap` (−20/29 = −0.68966); a speed of 0 moves nothing (`MoveControl#tick` reads the vehicle's speed: the first round's jar fact). **A stand-in for a wall, said so:** a spider climbs walls, so a wall does not strand one. **INFERRED:** its leap flies though its walking speed is 0 (the leap is a velocity, held every tick) | |

### PS11 — the diagonal: it comes to rest inside its band and winds up at the normal period (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| Spawned 12 east AND 12 south of his feet (a bearing of 45 degrees, 16.97 out, outside the band): `AT tag=ps11 M x=<x0+12 ± 0.1> y=<y0> z=<z0+12 ± 0.1>` two ticks after the spawn. 100 ticks later **`AT tag=ps11 M` between 8 and 12 from his feet**, within 3.6 of the 45-degree line's middle point (x0+7.07, z0+7.07) (the goal block can sit up to 3.52 off it). **`SKREEL windup … tick=P+120 forced=false at=<that AT> target=player <Ben> aim=<x0 y0 z0> d=<8.000 to 12.000>`**, the row's first and only wind-up, at the normal period; its leap (`ticks=29`) and its land `victims=1 [player <Ben> …]`; its HURT 55 → 50. `SELF customHP=300.000/350.000` | `MobPounce.goal` (the sweep's 45-degree cases from 12.5, 14, 18, 24 and 31.9; the mutation MUTNOCORRECT turns 28 of them red); `PounceCycle`. **INFERRED:** it walks in well under 100 ticks. **NOT A DISCRIMINATOR between this round and `4ba51506`, said so:** with him at a block centre, the middle point's own block on a 45-degree bearing has its centre 9.90 out and its stopping box's far corner 11.38 out, inside the band, so `4ba51506` would read this row the same. The failing diagonals are a target on or near a block's corner (the sweep's `(0, 0)`); the core sweep and its mutation are what tell the builds apart, and this row is a reading of the property on a server | |

### PB1R2 — the cooldown, its own blink now 12 to 24 out (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| As PB1, tag `pb1r2`: the forced `BLINK … tick=L forced=true trigger=none from=<x0+8 y0 z0> tried=1 [90.000 8.000 OK at=<x0+8 y0 z0+8>] landed=<x0+8 y0 z0+8> anchor=<y0>.000 cooldown=0` and its `BLINK moved … cooldown=240`; its first shot at about L + 117 with `BLINK visible … reason=fired`, HURT 40 → 36.364; **no BLINK line from L + 1 to L + 239**; **exactly one `BLINK … tick=L+240 forced=false trigger=player <Ben> from=<x0+8 y0 z0+8> tried=1 [<a> <d> OK at=<Q>] landed=<Q> anchor=<y0>.000 cooldown=0`, with `d` in [12, 24], Q that far from F horizontally, and Q.y = y0** (the floor reaches 25 round it); `BLINK moved … at=<Q> … cooldown=240`; the second shot at about L + 237 and its HURT | PB1's reading; Ben's 12 to 24 (`MobBlinkRangeTest`); the floor | |

### PB2R2 — a failed attempt does not start it, its own blink 12 to 24 out (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| **`BLINK … forced=true trigger=none from=<x0+8 y0 z0> tried=8 [ … ] landed=none anchor=<y0>.000 cooldown=0`**, eight entries `90.000 8.000 NO_FOOTING blocked=0 ungrounded=7`, as PB2 read; no `BLINK moved`. **Within two ticks, exactly one `BLINK … forced=false trigger=player <Ben> from=<x0+8 y0 z0> tried=1 [<a> <d> OK at=<Q>] landed=<Q> anchor=<y0>.000 cooldown=0` with `d` in [12, 24] and Q.y = y0**; `BLINK moved … cooldown=240`. No SHOT line | PB2's reading; the pit is 8 out, nearer than any blink of its own can land, so its own first draw is never the pit | |

### PB4 — Ben's far end, and the target a blinked Phalanx keeps (SURVIVAL, FROZEN)

| prediction | stands on | READING |
|---|---|---|
| **`BLINK … forced=true trigger=none from=<x0+8 y0 z0> tried=1 [0.000 24.000 OK at=<x0+32 y0 z0>] landed=<x0+32 y0 z0> anchor=<y0>.000 cooldown=0`**; `BLINK moved … at=<x0+32 y0 z0> invisible=true cooldown=240`; `AT tag=pb4 X x=<x0+32>`. **Then the first forced shot: `BLINK visible … reason=fired` and `SHOT fired mob=phalanx id=X gs=100 forced=true … target=player <Ben> from=<x0+32 y0+2.100 z0> aim=<x0 y0+1.620 z0> dealt=40.000`**: eye to eye 32.004, past 30 and inside 35. **Then the second: `SHOT none mob=phalanx id=X tick=<t> range=30.0`**: its first shot put it back on 30, and he is 32 away. At the kill, `SHOT ended … reason=owner-gone` for the first shot. No HURT | `MobShot#chooseRange` (`untilItsNextShotABlinkedPhalanxChoosesOutToTheReacquireRange`); `MobShooter.choose` (DragonSkreelWiringTest); the eye heights (2.100 read on every first-round `SHOT fired`; the player's 1.62, READ, `EntityType`). **Both halves:** on `45bc33ce`'s build the FIRST shot reads `SHOT none … range=30.0` too | |

### BB4R2 — a combatant in the box blinks it, now 12 to 24 out (SURVIVAL, LIVE)

| prediction | stands on | READING |
|---|---|---|
| As `GATE-dragon-blink.md`'s BB4, tag `bb4r2`: **exactly one `BLINK mob=phalanx … forced=false trigger=player <Ben> from=<F> tried=1 [<a> <d> OK at=<Q>] landed=<Q> anchor=<y0>.000 cooldown=0`, F = (x0+12, y0, z0), with `d` in [12, 24], Q that far from F horizontally, and Q.y = y0** (the floor reaches 25 round it); `BLINK moved … at=<Q> invisible=true cooldown=240`; no `SHOT fired` (the row ends ~26 ticks after the spawn). The AT is recorded, not predicted | BB4's reading (`d` 9.360 in [6, 16]); Ben's 12 to 24 | |

## BEN'S ROWS — BY THE ASK STEP, SURVIVAL

### PQ3 — the Skreel, leaping every six seconds now (SURVIVAL, LIVE)

| AUTO half, recorded beside his answers | Ben's half | READING |
|---|---|---|
| Every `SKREEL` line and every HURT in the row. **Not predicted: it is his play. Expected now (the first round's PQ1 wound up once in 599 ticks):** wind-ups about every 120 to 160 ticks while he is within 32, each at least 120 after the last; each in-band wind-up's `d` in 8 to 12; every HURT `dealerType=wither_skeleton/skreel`, `before=55.000` | `PQ3 ASK 1 question="With the Skreel leaping every six seconds, could you get out of the way in time?"`; `ASK 2 question="Does it leap often enough now?"`. The gold text asks him to TRY to dodge two and let the third land | |

### PQ4 — the Phalanx's range, 12 to 24 (SURVIVAL, LIVE)

| AUTO half | Ben's half | READING |
|---|---|---|
| Every `BLINK`, `BLINK moved`, `BLINK visible`, `SHOT` and `SHIELD` line. **Not predicted.** Each own `BLINK` decision at least **240 ticks** after the last `BLINK moved`; each own landing's draw `d` in [12, 24]. The gold text asks him to let it blink TWICE before killing it, the second 12 seconds after the first | `PQ4 ASK 1 question="Does the Phalanx's blink range feel right at 12 to 24 blocks?"` | |

## CARRIED STATE FOR THIS ROUND, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, as the first round's step 2, the folder suffix
   `-pre-dragon-skreel-2`. Report the folder, both pairs of counts, and `diff -rq lines: 0` each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report.
4. **The world's saved difficulty** is read by the setup's DIFFICULTY line and reported.
5. **Boot this round's head:** `./scripts/dev-server.sh --refresh-content`, announcing "booting now", on Ben's "ready"
   and the seat's approval.
6. **R0 binds to THIS boot**, and is read before Ben types anything. **Readings count only after Ben's LOGIN in that
   boot's log.**

## THE RUN SHEET FOR THIS ROUND — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL. NOTHING ELSE IS RUN

1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest dragon-skreel` — **ONCE. Wait for the chat to say the run finished, and answer all three gold
   questions, before the next command.** About **3 minutes** plus your answers: its waits sum to **2.81 minutes (3375
   ticks)**, and the eleven rows' starts and kills add a little. **Stand still and touch nothing until the first gold text**
   (about 1½ minutes): Skreels will leap on you and a Phalanx will shoot you (you cannot die). **At the first gold
   text,** a Skreel on its mount 14 blocks east is yours for **30 seconds**: TRY to get out of the way of two of its
   leaps, then let the third land on you; **do not attack it**; two questions. **At the second,** a Phalanx 12 blocks
   east is yours for **60 seconds** with the sword in your hand: walk up to it, follow it, **let it blink twice** (the
   second comes 12 seconds after the first), then kill it; one question. Answer each `yes` or `no`, or in your own words.
3. `/stop`

**Nothing else is run in this boot: no other selftest, no other command.** A departure is recorded as it happened, apart
from the readings.
