# GATE — THE PHALANX'S BLINK AND ONE-HIT SHIELD (STEP R7b), AND THE SHOT'S FOUR CARRIED ITEMS (C1-C4)

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-blink`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

The plan is `PLAN-dragon-mobs.md` (RULINGS, *Ben, 2026-11-17, VERBATIM: the Phalanx's shield*; §5.R.10 as built). The
scenario file is `paper/src/main/resources/selftest/GATE-dragon-blink.yml`, run by `/rpg selftest dragon-blink`.

**WHAT THIS GATE SUPERSEDES:** `GATE-dragon-shot.md`'s carried items C1-C4, closed by BC1X/BC1Z (C1), BC2A/BC2B (C2),
BC3 (C3) and BC4A/BC4B (C4). Its SH1R2 (`inside=false`), SH7R2 (the wall, UNREAD), SH15 (`no live shot after 0 ticks`)
are re-run here as BC1X, BC2B and BC3. Nothing in `GATE-dragon-shot.md` is edited but its dated note.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **Every row is SURVIVAL.** The blink's trigger and the shot's targeting both skip a creative or spectator player
  (`MobHostility.hostileTo`), so a creative bench reads neither: the run sheet's first line is `/gamemode survival`.
- **Difficulty NORMAL**, set by the gate's own `setup:`, restored at the run's end.
- **Every Phalanx is spawned at GS 100**: 240 health (DB5R2's reading), a shot 40.000 before defence.

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC

His custom health floor is 100 (every row heals him first with `rpg heal 1000000`).

| row | what can land on him after its heal | before defence | can he die |
|---|---|---|---|
| BC1X, BC1Z, BC4A, BC4B | one shot | 40 | no: 40 < 100 |
| BC2B, BC3, BB5 | nothing (the wall, the strike, the kill end it); at worst one shot | 40 | no |
| BC2A, BB1-BB3, BS1-BS4 | nothing: no shot is forced | 0 | no |
| BB4 | nothing: the row ends ~26 ticks after the spawn, under its first shot's 120 | 0 | no |
| BQ1 | a live Phalanx fires every 120 ticks; he is healed every 200, so at most **two** shots between heals | 80 | no: 80 < 100 |

## THE PHALANX'S SURVIVAL, AS BUILT

At GS 100 it has **240** health. Its shield eats the **first hit of any positive amount whole**, so the damage it takes
to die is **240 + its first hit**: a first hit of 30 (BS1's) means 270 dealt in all; a first hit of 240 means 480. A
`/kill` or the void kill it with the shield up (BS4).

## INHERITED OR CHOSEN — THE BLINK AND THE SHIELD, ITEM BY ITEM

cfde822 is the old game (`PLAN-dragon-mobs.md` §3.4). **READ** means the item is in that record; **NOT IN THE RECORD**
means it is not, and the line says who chose it.

| # | item | inherited or chosen | source |
|---|---|---|---|
| 1 | the trigger is a non-creative, non-spectator player | INHERITED | READ, §3.4 D4; as built `MobHostility.hostileTo` (a player; never a mob) |
| 2 | his box must meet a box of half-extent 5 round its feet (a box, not a distance) | INHERITED | READ, D4 (X4: 5) |
| 3 | not while it is invisible | INHERITED | READ |
| 4 | its OWN blink needs AI (a frozen Phalanx never blinks of itself, as it never fires of itself) | CHOSEN | NOT IN THE RECORD |
| 5 | 8 attempts | INHERITED | READ (X6) |
| 6 | 6 to 16 blocks out | INHERITED | READ (X5) |
| 7 | the angle uniform in [0, 360), the distance uniform in [6, 16] | CHOSEN | NOT IN THE RECORD ("random points") |
| 8 | the landing check is the Knell's (`BeamTeleport.foot`): loaded, fits, grounded, the nearest height within 3, NO sight | CHOSEN, the seat's ruling 3 | departs from READ: cfde822 stood it on "the column's highest block + 1", which lands on a roof |
| 9 | the first that stands wins; none, it stays | INHERITED | READ |
| 10 | invisible after a blink; firing clears it | INHERITED | READ |
| 11 | squid ink at both ends, 12 puffs | puffs INHERITED, count CHOSEN | READ / NOT IN THE RECORD |
| 12 | the enderman-teleport sound, volume 1.0 pitch 1.0 | sound INHERITED, numbers CHOSEN | READ / NOT IN THE RECORD |
| 13 | its path stopped after a blink; its yaw and pitch kept | CHOSEN | NOT IN THE RECORD |
| 14 | the blink runs LAST in its Phalanx's tick, after the fire and every shot's step | CHOSEN, for the seat's "must not fight or orphan shots" | NOT IN THE RECORD |
| 15 | the shield eats the first damage of ANY kind | Ben's ruling, 2026-11-17: "1. yes the first damage of any kind" | and READ, D5 |
| 16 | absorbed WHOLE: no health lost | INHERITED | READ ("cancelled") |
| 17 | one per life: nothing gives it back | INHERITED | READ (nothing re-adds the byte) |
| 18 | a shield in its off hand while up, cleared when it absorbs, never dropped | held and cleared INHERITED; never dropped CHOSEN | READ / NOT IN THE RECORD |
| 19 | the shield-break sound, `item.shield.break` at 1.0 and 1.0 | sound INHERITED, key and numbers CHOSEN | READ / NOT IN THE RECORD |
| 20 | a `crit` particle, 16 of them | CHOSEN | NOT IN THE RECORD |
| 21 | a hit of zero does not spend it | the seat's brief | NOT IN THE RECORD |
| 22 | `/kill` and the void ignore it | the seat's brief | as built: both PASS to vanilla before the point |
| 23 | nothing rides through: a status, push or impulse of ours on it in the same tick as the absorb is dropped; a vanilla push while it is up, or in its absorb's tick, is cancelled | CHOSEN, for the brief's "nothing rides through" | NOT IN THE RECORD. **Not caught:** a rider a payload applies BEFORE its own damage, in that tick |
| 24 | the state survives an unload: a BYTE in the mob's PDC | the seat's brief | as built; **this gate stages no unload (UNREAD)** |

## THE ONE DAMAGE POINT, AND THE PATHS PINNED

`BukkitCombatant.Handle.applyDamage` holds the only write of damage to custom health in the paper tree
(`ctx.stats().damage(`, a census in `DragonBlinkWiringTest`); the absorb sits directly after the shot's strike, before
that write. Every `.applyDamage(` call in the tree is a handle call (pinned): the melee exchange, its sweep and the
thorns reflect, the environmental reroute (fall, fire, explosion, a vanilla projectile), the scorch tick, `/rpg
mobdamage`, the Knell's beam, the Phage's blast, the Phalanx's shot, and our abilities through `CombatWorld`. `/kill`
and the void are `PASS` in `VanillaDamagePolicy`, returned before the reroute (pinned).

## WHAT CAN OUTLIVE A ROW (C2), MEASURED FROM THE CODE

| thing | ended by the row-start clean? |
|---|---|
| a tagged mob, and a shot (its carrier and display are tagged) | YES, `CLEAN removed=` (as before) |
| a point-scheduled chain of core work: an area field's pulses, a projectile's flight, a ray's steps, an ember's tracking, an Ignite fuse | **YES, new:** `LINGER work=` (every link is registered in `PendingWork`; a cancelled link never runs, so never reschedules) |
| a vanilla or ability projectile, an area-effect cloud, a marker of ours (an Item or a fireball carrying `marker_entity`) within 48 | **YES, new:** `LINGER entities=` |
| a status ON THE PLAYER (scorch, wither), and his fire ticks | **NO.** Entity-bound work on him; the heal does not clear it. No row here gives him one |
| vanilla drops (items, XP) of a killed mob | **NO.** A plain Item has no marker; the inventory goes back only at a gate boundary |
| a blink's invisibility, or a spent shield | dies with its Phalanx (both are on the mob) |

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-blink` prints it, shortened. **Not** master's `275e9d0d`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1`, after this boot's `Done` | |

### R0b — the jar carries the blink, the shield and the four fixes

| prediction | instrument | READING |
|---|---|---|
| `content/mobs/phalanx.yml` PRESENT `blink:`, `  trigger_range: 5`, `shield:`, `  item: shield`; classes PRESENT `health/MobShielder.class`, `adapter/PendingWork.class`, `adapter/ShieldSink.class`, `selftest/SelfTestBlink.class`; `GATE-dragon-blink.yml` PRESENT `BC1X:`, `BQ1:`. **The controls, the same on both builds:** `phalanx.yml` PRESENT `  life_ticks: 240`; `health/MobShooter.class` PRESENT; `GATE-dragon-shot.yml` PRESENT `SQ1R2:` | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$p = Bytes 'content/mobs/phalanx.yml'
foreach ($l in 'blink:', '  trigger_range: 5', 'shield:', '  item: shield', '  life_ticks: 240') {
  if ($p -ne $null -and $p -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT phalanx.yml '$l'" } else { "ABSENT  phalanx.yml '$l'" }
}
foreach ($c in 'health/MobShielder', 'adapter/PendingWork', 'adapter/ShieldSink', 'selftest/SelfTestBlink', 'health/MobShooter') {
  if ((Bytes "io/github/butterflysmp/rpg/paper/$c.class") -ne $null) { "PRESENT $c.class" } else { "ABSENT  $c.class" }
}
foreach ($q in @(@('dragon-blink', 'BC1X'), @('dragon-blink', 'BQ1'), @('dragon-shot', 'SQ1R2'))) {
  $y = Bytes "selftest/GATE-$($q[0]).yml"
  if ($y -eq $null) { "MISSING selftest/GATE-$($q[0]).yml" } elseif ($y -match "(?m)^$($q[1]):\s*$") { "PRESENT $($q[0]) $($q[1]):" } else { "ABSENT  $($q[0]) $($q[1]):" }
}
$zip.Dispose()
```

**A jar of master `275e9d0d` reads the opposite:** ABSENT on the four new `phalanx.yml` lines and the four new classes,
and `MISSING selftest/GATE-dragon-blink.yml`; the three controls PRESENT on both.

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim the boot of `3dc0e184`'s (this branch adds no content file, it changes one). No `Refusing`, `Skipping`, `SEVERE` or `Exception`; no `[Rpg]` `WARN` beyond `volley_stone`'s known pair. **A `Skipping malformed mob 'phalanx.yml'` line (`4 mobs`) would mean a blink or shield key was refused** | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception'` | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 146 scenarios across 19 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink]`**, no `Refusing selftest` | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | |

**The 146 is RECOUNTED from the files at this branch's head:** in `paper/src/main/resources/selftest/`,
`grep -cE '^  - ' index.yml` prints **19**; summing `grep -cE '^[A-Za-z0-9-]+:'` over the nineteen `GATE-<g>.yml`
prints **165**; the same with `'^setup:'` prints **19**: 165 − 19 = **146**. **The control:** at `be7bf586` (master plus
the docs commit) the same three commands print 18, 147, 18: 129, `GATE-dragon-shot.md` R0dR2's reading.

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**Every row starts** `LEFTOVER living=0 radius=16` and, new, `LINGER work=0 entities=0 radius=48`, **except BC2B**, whose
LINGER names BC2A's field. **Every forced shot** logs `SHOT fired … dealt=40.000`, then (new, C1) **two** `SHOT
calibrated … shot=<S>` lines: the first `offset=0.000 0.000 0.000` at the shot's first step (the display was spawned at
the carrier's feet), the second with the frog's real passenger offset after its first ride tick, **`offset=0.000 0.450
-0.400`** (the boot of `3dc0e184`'s SH1R2 displacement, carried as a prediction: its z is READ, the frog's attachment
−0.25 × the scale 1.6; **its y is NOT derived**: 0.375 × 1.6 is 0.600, and the 0.150 between them is unexplained, F17),
and no third: the carrier's yaw is always 0. **No `SHOT refused` line appears in any row.** No row predicts a `BOW
suppressed` line, and none is a failure (BB4's and BQ1's Phalanxes have AI and draw).

### BC1X — C1: the shape inside the box at three ages, flying WEST (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Three `SHOT at … age=<a> … flown=<0.2a> v=… box=<c ± 0.400> shape=<c ± 0.200> inside=true` at ages about 5, 15 and 25 (each 10 after the last, ±1), flown 0.2 × age to three places (SH1R2's reading at age 10: `flown=2.000`); **the shape's centre equals the box's centre on all three axes to three places**; then `SHOT hit … age=33 at=<feet + 1.412 1.705 0.000> flown=6.600 … dealt=40.000` and its HURT | SH1R2's staging and readings; `calibrate` measured before the teleport every tick | |

### BC1Z — C1, the second axis: flying SOUTH (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| As BC1X, rotated: three `SHOT at …` reads at ages about 5, 15, 25, `inside=true`, the centres equal to three places; `SHOT hit … age=33 at=<feet + 0.000 1.705 -1.412> flown=6.600 … dealt=40.000` and its HURT. **The frog's passenger point is toward its own −z, and the shot now flies along +z, so this is the axis the old defect would show on** | BC1X by symmetry (the homing turns toward his eye in both) | |

### BC2A — C2's first half: a grenade field left pulsing (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `LINGER work=0 entities=0 radius=48` at its start; the cast's own lines; no `SHOT` line; nothing else predicted | the clean | |

### BC2B — C2: the wall, re-staged directly after the field (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`LINGER work=<1 or more> …`** at its start, BC2A's field ended (1 expected: a field is one chain with one pending link at a time, INFERRED, not read); then **`SHOT blocked … age=16 at=<feet + 4.806 1.908 0.000> flown=3.200 block=stone`**; no `SHOT destroyed`, no `SHOT refused`, no HURT | SH7R2's prediction (the swept box); the clean ending the field the boot of `3dc0e184` read destroying SH7R2's shot | |

### BC3 — C3: the strike asked for before the shot exists (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **No** `no live shot after 0 ticks` line; `DRIVE strike_at_age tag=bc3_shot age=32 waited=<32 to 34>`; **`SHOT destroyed … age=32 … by=vanilla cause=ENTITY_ATTACK damager=player <Ben>`**; no `SHOT hit`, no HURT | SH15's prediction; `SelfTestStrike.next` (no shot yet is a wait) | |

### BC4A — C4, the player: his feet across a hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Two `SELF customHP=… at=<x y z> …` lines whose `at=` are **identical** (to the two places the line prints); between them one `SHOT hit … age=33` and its HURT. **What this row can and cannot see:** the hit lands when his box grown by 1.2 holds the shot's centre (1.5 from his centre), and the carrier's half-edge is 0.4 against his 0.3 (0.7): **the carrier can never touch a survival player**, so this row reads the HIT's push (none: the shot authors no knockback, and our damage raises no vanilla knockback), not the team's. BC4B reads the team | `MobShot.victim`; `BukkitCombatant.applyDamage` (no `entity.damage`) | |

### BC4B — C4, a body the shot flies through: the team (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Two `AT tag=bc4bh <uuid> x=… y=… z=…` lines **identical to three places**, before the shot and after its hit; `SHOT hit … age=33` on him (the shot passed through the husk); the husk's `PROBE … customHP=<full>/<full>` (never hit). **Why this is not a hollow row:** the husk has AI (speed 0), so a push WOULD move it (`LivingEntity#tick` calls `aiStep` unless removed; `aiStep` calls `pushEntities` on both paths and `travel` only under `isEffectiveAi`, READ); and the carrier's box (half 0.4, centre about 1.9 up as it passes x+4) overlaps the husk's (half 0.3, 1.95 tall), INFERRED from the flight. Without the team, the frog would push it | `EntitySelector.pushableBy` (a team rule NEVER refuses both ways, READ); `DragonBlinkWiringTest` C4 | |

### BB1 — a forced blink lands (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `AT tag=bb1 … x=<P.x> y=<P.y> z=<P.z>` (P = his feet + 8 east); `DRIVE blink tag=bb1 <uuid> angle=90.000 distance=8.000`; **`BLINK mob=phalanx id=<uuid> tick=<t> forced=true trigger=none from=<P> tried=1 [90.000 8.000 OK at=<P + 0.000 0.000 8.000>] landed=<P + 0.000 0.000 8.000>`**; `BLINK moved … at=<P + 0 0 8> invisible=true`; the second `AT` at P + 8 south | `MobBlink.decide`; `BeamTeleport.foot` (a stone floor, air above: height 0 fits and is grounded) | |

### BB2 — no ground under any height: it stays (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`BLINK … forced=true trigger=none from=<P> tried=8 [90.000 8.000 NO_FOOTING blocked=0 ungrounded=7, …eight times] landed=none`**; no `BLINK moved`; `AT` at P | the pit: air from 6 below to 6 above, so every height from −3 to +3 fits and none has a block under it | |

### BB3 — no height fits: it stays (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`BLINK … tried=8 [90.000 8.000 NO_FOOTING blocked=7 ungrounded=0, …eight times] landed=none`**; no `BLINK moved`; `AT` at P | the column filled with stone from 4 below to 7 above: no height's 2.4-tall box fits | |

### BB4 — the trigger: he steps into its box, and it blinks of itself (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Exactly one** `BLINK mob=phalanx … forced=false trigger=player <Ben> from=<F> tried=1 [<a> <d> OK at=<L>] landed=<L>`, with `d` in [6, 16], `L` at that horizontal distance from `F` and **`L.y = F.y`** (a flat floor everywhere within 16); `BLINK moved … at=<L> invisible=true`; no `SHOT fired` (the row ends ~26 ticks after the spawn, under 120). The `AT` is recorded, not predicted: it has AI and walks. **Not staged here, pinned in core:** a creative or spectator player never triggers it (`MobBlinkTest`) | `MobBlink.triggered` (his box 3 off, inside 5); the uniform draw | |

### BB5 — firing makes it visible (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| As BB1's `BLINK` and `BLINK moved … invisible=true`; then `SHOT fired … from=<x and z of P + 0 0 8>` and **`BLINK visible mob=phalanx id=<uuid> tick=<t> reason=fired`** | `fire()`: cfde822's "Firing clears invisibility" (READ) | |

### BS1 — the first hit is eaten whole, and only the first (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `SHIELD tag=bs1 <uuid> up=true offhand=shield`; **`SHIELD absorbed mob=phalanx id=<uuid> tick=<t> amount=30.000 by=dealer=<Ben> element=null amount=30.000`**; `SHIELD tag=bs1 <uuid> up=false offhand=none`; `PROBE … customHP=240.000/240.000`; after the second `rpg mobdamage 30`, **`PROBE … customHP=210.000/240.000`**, and no second `SHIELD absorbed` | `MobShielder.absorb`; `/rpg mobdamage`'s two-argument `applyDamage` (element null) | |

### BS2 — a melee hit's push goes with it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `MOTION tag=bs2 … vx=0.0000 vy=0.0000 vz=0.0000` before; after the first hit **`SHIELD knockback cancelled mob=<uuid> cause=ENTITY_ATTACK shield=up`** in the hit's tick, then **`SHIELD absorbed … amount=<the emberblade's hit>`** a tick later (our damage is scheduled through `onEntity`, one tick on); `MOTION … vx=0.0000 vy=0.0000 vz=0.0000`; `PROBE … customHP=240.000/240.000`. After the second hit: no SHIELD line; **`MOTION … vx=<positive>`** (pushed east, away from him: **the row's positive control**); `PROBE … customHP=<240 − that hit>/240.000`. **If the second `MOTION` also reads zero, the instrument cannot see a push on a frozen mob and the row's push half is VOID, not PASS** | `RpgListeners.onShieldedKnockback`; `onCombatKnockback` passes a landed melee hit | |

### BS3 — nothing rides through (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The first arrow: **`SHIELD absorbed … amount=<the short bow's hit>`** and, **in the same tick**, **`SHIELD rider suppressed mob=phalanx id=<uuid> tick=<the same> what=knockback=0.100`**; `MOTION … vx=0.0000 vy=0.0000 vz=0.0000`; `SHIELD … up=false offhand=none`; `PROBE … customHP=240.000/240.000`. The second arrow: no SHIELD line; **`MOTION … vx=<positive>`** (the positive control, as BS2's); `PROBE … customHP=<240 − that hit>/240.000` | `short_bow.yml`'s `on_hit` (`weapon_damage`, then `knockback 0.1`); `MobShielder.suppressRider` | |

### BS4 — /kill ignores it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `SHIELD tag=bs4 <uuid> up=true offhand=shield`; after the kill, `SHIELD tag=bs4 none within <REACH> blocks` (`SelfTestDrivers.tagged`'s none line, whose head
this branch gave PROBE_SHIELD: it read `MOTION` for every caller but thaw); **no `SHIELD absorbed` line in the row** | `VanillaDamagePolicy`: KILL is PASS, returned before the reroute | |

## BEN'S ROW — BY THE ASK STEP, SURVIVAL

### BQ1 — see it, take a shot, chase it, hit it (SURVIVAL)

| AUTO half, recorded beside his answers | Ben's half | READING |
|---|---|---|
| Every `SHOT fired`, `SHOT calibrated`, `SHOT hit`/`destroyed`, `BLINK`, `BLINK moved`, `BLINK visible`, `SHIELD absorbed`, `SHIELD rider suppressed` and `SHIELD knockback cancelled` line in the row. **Not predicted: it is his play.** Every `BLINK` line has `trigger=player <Ben>` and at most one follows each `BLINK visible` or the spawn | `BQ1 ASK 1 question="Does the glowing shape sit where you can hit it?" answer=…`; `ASK 2 question="Did the shot shove you?"`; `ASK 3 question="Did the Phalanx blink away when you got close?"`; `ASK 4 question="Did its shield visibly eat your first hit?"` | |

## THE ROWS ALREADY MERGED — THE AUDIT

Every merged row that spawns a Phalanx (`grep -n "phalanx" paper/src/main/resources/selftest/*.yml` at `be7bf586`):
**the twelve rows of `GATE-dragon-shot.yml` and `GATE-dragon-bodies.yml`'s DB5R2.** All thirteen spawn it FROZEN, so
none blinks of itself (item 4), and no merged row has a `blink:` step. Each ends with `kill @e[tag=<row>]`, a `/kill`,
which the shield ignores. **No merged row predicts a Phalanx's health or its off hand:** `probe_body` prints no
equipment. **The one merged row whose damage may reach a Phalanx** is SH4R2, whose grenade field may touch it; that adds
a `SHIELD absorbed` line beside its reading and changes no prediction. **New in every merged row:** a `LINGER` line after
`CLEAN`, and two `SHOT calibrated` lines for each shot. Neither line is predicted absent anywhere.

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, as `GATE-dragon-shot.md` step 2, the folder suffix
   `-pre-dragon-blink`. Report the folder, both pairs of counts, and `diff -rq lines: 0` each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report.
4. **The world's saved difficulty** is read and reported (the gate's `setup:` sets NORMAL and restores it).
5. **Boot this branch's head:** `./scripts/dev-server.sh --refresh-content`, announcing "booting now", on Ben's "ready".
6. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` line names this branch's head. **Readings count
   only after Ben's LOGIN in that boot's log.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL. NOTHING ELSE IS RUN

1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest dragon-blink` — **ONCE. Wait for the chat to say the run finished, and answer all four gold
   questions, before the next command.** About **1½ minutes** plus your answers: its waits sum to 1.11 minutes
   (1332 ticks), and one step waits about 1.7 seconds more for a shot to reach its age; the seventeen rows' cleans add a
   little. **Stand still and touch nothing for the first half minute**: you will be shot four times (you cannot die).
   **Then, at the gold text,** a Phalanx twelve blocks east is yours for **40 seconds** with the sword in your hand:
   **watch a shot and let one hit you; walk up to the Phalanx, and it should vanish; find it, and hit it.** Four
   questions; answer each `yes` or `no`, or in your own words.
3. `/stop`

**Nothing else is run: no other selftest, no other command.** A departure is recorded as it happened, apart from the
readings.
