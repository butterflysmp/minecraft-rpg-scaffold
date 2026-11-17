# GATE — THE PHALANX'S BLINK AND ONE-HIT SHIELD (STEP R7b), AND THE SHOT'S FOUR CARRIED ITEMS (C1-C4)

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-blink`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

**2026-11-17: RUN once, on the boot of `ef770b8a`** (approved to boot by the review seat; Ben's "ready"). The readings
sit beside each row; the run's own account, BQ1 in order by tick, and what the run found are under **READINGS** at the
end. No prediction above was edited.

**2026-11-17: approved to merge by the review seat after the boot of ef770b8a.**

**2026-11-17, STEP R8 (`feat/dragon-skreel`, before any boot of it): `GATE-dragon-skreel.md` closes this gate's carried items R2-R5** (READINGS, *What the run found*): **R2** the cooldown, Ben's 240 ticks, built and read by its PB1, PB2 and PQ2; **R3** the decision from a fractional height, diagnosed (whole-block heights from a fractional anchor) and fixed (the search anchored on floor(y)), read by its PB3; **R4** BC4B's cleanup, fixed in `GATE-dragon-blink.yml` (it now kills `bc4bh`); **R5** the two pushes cancelled against one absorb, READ from the jar there: one sprinting hit raises two knockback events. **What R8 changes in this file's rows, none re-run:** every BLINK line now ends `anchor=<y> cooldown=<n>` (`cooldown=0` on BB1-BB5, each a fresh Phalanx), every `BLINK moved` line `cooldown=240`; BB1-BB5's verdicts are unchanged (a whole height anchors on itself); BQ1's live Phalanx would now decide at most one landed blink per 240 ticks. **No prediction or reading below is changed.**

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
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-blink` prints it, shortened. **Not** master's `275e9d0d`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1`, after this boot's `Done` | READ 2026-11-17: `[12:41:25] [Server thread/INFO]: [Rpg] Build: ef770b8a`, this PR's head (`git ls-remote` `ef770b8acae6…`). **MATCH** |

### R0b — the jar carries the blink, the shield and the four fixes

| prediction | instrument | READING |
|---|---|---|
| `content/mobs/phalanx.yml` PRESENT `blink:`, `  trigger_range: 5`, `shield:`, `  item: shield`; classes PRESENT `health/MobShielder.class`, `adapter/PendingWork.class`, `adapter/ShieldSink.class`, `selftest/SelfTestBlink.class`; `GATE-dragon-blink.yml` PRESENT `BC1X:`, `BQ1:`. **The controls, the same on both builds:** `phalanx.yml` PRESENT `  life_ticks: 240`; `health/MobShooter.class` PRESENT; `GATE-dragon-shot.yml` PRESENT `SQ1R2:` | the scan below | READ 2026-11-17: all thirteen lines `PRESENT`: the four new `phalanx.yml` lines and `  life_ticks: 240`; the five classes; `dragon-blink BC1X:`, `dragon-blink BQ1:`, `dragon-shot SQ1R2:`. None ABSENT, none MISSING. **MATCH** |

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
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim the boot of `3dc0e184`'s (this branch adds no content file, it changes one). No `Refusing`, `Skipping`, `SEVERE` or `Exception`; no `[Rpg]` `WARN` beyond `volley_stone`'s known pair. **A `Skipping malformed mob 'phalanx.yml'` line (`4 mobs`) would mean a blink or shield key was refused** | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception'` | READ 2026-11-17: `[12:41:26] [Server thread/INFO]: [Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`, verbatim. No Refusing, Skipping, SEVERE or Exception. The only `[Rpg]` WARN lines are `volley_stone`'s pair; the other WARN lines are Paper's version notice (`You are running the latest build for your Minecraft version (26.1.2)` … `4 release(s) behind the latest stable release (26.3)!`), not ours. **MATCH** |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 146 scenarios across 19 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink]`**, no `Refusing selftest` | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | READ 2026-11-17: `[12:41:26] [Server thread/INFO]: [Rpg] Selftest: dev server, 146 scenarios across 19 gates [melee-cell, … dragon-shot, dragon-blink]`, verbatim; no `Refusing selftest`. **MATCH** |

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

> **2026-11-17, READ on the boot of `ef770b8a`: the second offset is NOT the prediction above.** All eleven shots of the
> run log exactly two `SHOT calibrated` lines, `age=0 offset=0.000 0.000 0.000` and **`age=1 offset=0.000 0.600
> -0.400`**, and no third. **The y is 0.600, not the predicted 0.450: a MISMATCH.** The z matches. 0.600 is the
> 0.375 × 1.6 that F17 had set aside as not borne out, so the frog's passenger offset is the derived one. **F17 is
> restated, not closed:** the 0.45 that SH1R2 read was the shape's displacement from the box under the old one-time
> calibration, not the offset itself, and what made it 0.45 rather than 0.60 is unexplained. The C1 fix stands on
> measuring, so no row's centre moved: BC1X and BC1Z read the centres equal. `SHOT refused`: 0 lines. `BOW suppressed`:
> 8 lines, all in BQ1, none a failure.

### BC1X — C1: the shape inside the box at three ages, flying WEST (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Three `SHOT at … age=<a> … flown=<0.2a> v=… box=<c ± 0.400> shape=<c ± 0.200> inside=true` at ages about 5, 15 and 25 (each 10 after the last, ±1), flown 0.2 × age to three places (SH1R2's reading at age 10: `flown=2.000`); **the shape's centre equals the box's centre on all three axes to three places**; then `SHOT hit … age=33 at=<feet + 1.412 1.705 0.000> flown=6.600 … dealt=40.000` and its HURT | SH1R2's staging and readings; `calibrate` measured before the teleport every tick | READ 2026-11-17: `inside=true` at ages 5, 15 and 25; `flown=1.000`, `3.000`, `5.000`; the box's and the shape's centres equal to three places at every age (`-92.498 65.040 6.500`; `-94.495 64.920 6.500`; `-96.491 64.801 6.500`); `SHOT hit … age=33 at=-98.088 64.705 6.500 flown=6.600 victim=player b6ae27e9-… dealt=40.000`, his feet `-99.5 63 6.5` + `1.412 1.705 0.000`; its HURT. **MATCH.** Its calibrated offset is not the paragraph's: see the dated note under THE AUTO ROWS |

### BC1Z — C1, the second axis: flying SOUTH (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| As BC1X, rotated: three `SHOT at …` reads at ages about 5, 15, 25, `inside=true`, the centres equal to three places; `SHOT hit … age=33 at=<feet + 0.000 1.705 -1.412> flown=6.600 … dealt=40.000` and its HURT. **The frog's passenger point is toward its own −z, and the shot now flies along +z, so this is the axis the old defect would show on** | BC1X by symmetry (the homing turns toward his eye in both) | READ 2026-11-17: `inside=true` at ages 5, 15 and 25; the centres equal to three places (`-99.500 65.040 -0.502`; `-99.500 64.920 1.495`; `-99.500 64.801 3.491`); `SHOT hit … age=33 at=-99.500 64.705 5.088 flown=6.600 … dealt=40.000`, his feet + `0.000 1.705 -1.412`; its HURT. **MATCH** |

### BC2A — C2's first half: a grenade field left pulsing (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `LINGER work=0 entities=0 radius=48` at its start; the cast's own lines; no `SHOT` line; nothing else predicted | the clean | READ 2026-11-17: `BC2A LINGER work=0 entities=0 radius=48`; `CMD /rpg cast solar_grenade` and no other line from the cast; no SHOT line. **MATCH** |

### BC2B — C2: the wall, re-staged directly after the field (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`LINGER work=<1 or more> …`** at its start, BC2A's field ended (1 expected: a field is one chain with one pending link at a time, INFERRED, not read); then **`SHOT blocked … age=16 at=<feet + 4.806 1.908 0.000> flown=3.200 block=stone`**; no `SHOT destroyed`, no `SHOT refused`, no HURT | SH7R2's prediction (the swept box); the clean ending the field the boot of `3dc0e184` read destroying SH7R2's shot | READ 2026-11-17: **`BC2B LINGER work=1 entities=0 radius=48`** (the INFERRED 1 is now READ); `SHOT blocked … age=16 at=-94.694 64.908 6.500 flown=3.200 block=stone`, his feet + `4.806 1.908 0.000`; no `SHOT destroyed`, no `SHOT refused`, no HURT. **MATCH.** SH7R2's wall is READ |

### BC3 — C3: the strike asked for before the shot exists (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **No** `no live shot after 0 ticks` line; `DRIVE strike_at_age tag=bc3_shot age=32 waited=<32 to 34>`; **`SHOT destroyed … age=32 … by=vanilla cause=ENTITY_ATTACK damager=player <Ben>`**; no `SHOT hit`, no HURT | SH15's prediction; `SelfTestStrike.next` (no shot yet is a wait) | READ 2026-11-17: no `no live shot` line in the whole log (count 0); `DRIVE strike_at_age tag=bc3_shot age=32 waited=33`; `SHOT destroyed … age=32 at=-97.889 64.717 6.500 flown=6.400 by=vanilla cause=ENTITY_ATTACK damager=player b6ae27e9-…`; no `SHOT hit`, no HURT. **MATCH** |

### BC4A — C4, the player: his feet across a hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Two `SELF customHP=… at=<x y z> …` lines whose `at=` are **identical** (to the two places the line prints); between them one `SHOT hit … age=33` and its HURT. **What this row can and cannot see:** the hit lands when his box grown by 1.2 holds the shot's centre (1.5 from his centre), and the carrier's half-edge is 0.4 against his 0.3 (0.7): **the carrier can never touch a survival player**, so this row reads the HIT's push (none: the shot authors no knockback, and our damage raises no vanilla knockback), not the team's. BC4B reads the team | `MobShot.victim`; `BukkitCombatant.applyDamage` (no `entity.damage`) | READ 2026-11-17: `SELF customHP=350.000/350.000 at=-99.50 63.00 6.50 onGround=true fall=0.000`, then `SHOT hit … age=33` and its HURT, then `SELF customHP=313.636/350.000 at=-99.50 63.00 6.50 onGround=true fall=0.000`: the two `at=` identical. **MATCH** |

### BC4B — C4, a body the shot flies through: the team (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Two `AT tag=bc4bh <uuid> x=… y=… z=…` lines **identical to three places**, before the shot and after its hit; `SHOT hit … age=33` on him (the shot passed through the husk); the husk's `PROBE … customHP=<full>/<full>` (never hit). **Why this is not a hollow row:** the husk has AI (speed 0), so a push WOULD move it (`LivingEntity#tick` calls `aiStep` unless removed; `aiStep` calls `pushEntities` on both paths and `travel` only under `isEffectiveAi`, READ); and the carrier's box (half 0.4, centre about 1.9 up as it passes x+4) overlaps the husk's (half 0.3, 1.95 tall), INFERRED from the flight. Without the team, the frog would push it | `EntitySelector.pushableBy` (a team rule NEVER refuses both ways, READ); `DragonBlinkWiringTest` C4 | READ 2026-11-17: `AT tag=bc4bh a2e8c957-… x=-95.500 y=63.000 z=6.500` before and after, identical; `SHOT hit … age=33 … victim=player b6ae27e9-…`; `PROBE tag=bc4bh … husk customHP=100.000/100.000`. **MATCH.** Not predicted: the row's `/kill @e[tag=bc4b]` does not select the husk (its tag is `bc4bh`), and BB1's clean removed it (`BB1 CLEAN blocks=1080 removed=1`) |

### BB1 — a forced blink lands (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `AT tag=bb1 … x=<P.x> y=<P.y> z=<P.z>` (P = his feet + 8 east); `DRIVE blink tag=bb1 <uuid> angle=90.000 distance=8.000`; **`BLINK mob=phalanx id=<uuid> tick=<t> forced=true trigger=none from=<P> tried=1 [90.000 8.000 OK at=<P + 0.000 0.000 8.000>] landed=<P + 0.000 0.000 8.000>`**; `BLINK moved … at=<P + 0 0 8> invisible=true`; the second `AT` at P + 8 south | `MobBlink.decide`; `BeamTeleport.foot` (a stone floor, air above: height 0 fits and is grounded) | READ 2026-11-17: `AT tag=bb1 … x=-91.500 y=63.000 z=6.500`; `DRIVE blink tag=bb1 … angle=90.000 distance=8.000`; `BLINK mob=phalanx id=afe6801e-… tick=1758 forced=true trigger=none from=-91.500 63.000 6.500 tried=1 [90.000 8.000 OK at=-91.500 63.000 14.500] landed=-91.500 63.000 14.500`; `BLINK moved … tick=1758 at=-91.500 63.000 14.500 invisible=true`; `AT … x=-91.500 y=63.000 z=14.500`. **MATCH** |

### BB2 — no ground under any height: it stays (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`BLINK … forced=true trigger=none from=<P> tried=8 [90.000 8.000 NO_FOOTING blocked=0 ungrounded=7, …eight times] landed=none`**; no `BLINK moved`; `AT` at P | the pit: air from 6 below to 6 above, so every height from −3 to +3 fits and none has a block under it | READ 2026-11-17: `BLINK … forced=true trigger=none from=-91.500 63.000 6.500 tried=8 […] landed=none`, its brackets holding eight `90.000 8.000 NO_FOOTING blocked=0 ungrounded=7`; no `BLINK moved`; `AT … x=-91.500 y=63.000 z=6.500`. **MATCH** |

### BB3 — no height fits: it stays (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`BLINK … tried=8 [90.000 8.000 NO_FOOTING blocked=7 ungrounded=0, …eight times] landed=none`**; no `BLINK moved`; `AT` at P | the column filled with stone from 4 below to 7 above: no height's 2.4-tall box fits | READ 2026-11-17: `BLINK … tried=8 […] landed=none`, its brackets holding eight `90.000 8.000 NO_FOOTING blocked=7 ungrounded=0`; no `BLINK moved`; `AT … x=-91.500 y=63.000 z=6.500`. **MATCH** |

### BB4 — the trigger: he steps into its box, and it blinks of itself (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **Exactly one** `BLINK mob=phalanx … forced=false trigger=player <Ben> from=<F> tried=1 [<a> <d> OK at=<L>] landed=<L>`, with `d` in [6, 16], `L` at that horizontal distance from `F` and **`L.y = F.y`** (a flat floor everywhere within 16); `BLINK moved … at=<L> invisible=true`; no `SHOT fired` (the row ends ~26 ticks after the spawn, under 120). The `AT` is recorded, not predicted: it has AI and walks. **Not staged here, pinned in core:** a creative or spectator player never triggers it (`MobBlinkTest`) | `MobBlink.triggered` (his box 3 off, inside 5); the uniform draw | READ 2026-11-17: exactly one `BLINK mob=phalanx id=3f4edb55-… tick=1877 forced=false trigger=player b6ae27e9-… from=-87.500 63.000 6.500 tried=1 [95.805 9.360 OK at=-88.447 63.000 15.812] landed=-88.447 63.000 15.812`: d 9.360 is in [6, 16], the landing is 9.360 from F horizontally, and L.y = F.y; `BLINK moved … at=-88.447 63.000 15.812 invisible=true`; no `SHOT fired`. The AT, recorded: `x=-88.428 y=63.000 z=15.322`. **MATCH** |

### BB5 — firing makes it visible (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| As BB1's `BLINK` and `BLINK moved … invisible=true`; then `SHOT fired … from=<x and z of P + 0 0 8>` and **`BLINK visible mob=phalanx id=<uuid> tick=<t> reason=fired`** | `fire()`: cfde822's "Firing clears invisibility" (READ) | READ 2026-11-17: the BLINK and `BLINK moved … at=-91.500 63.000 14.500 invisible=true` as BB1's; `BLINK visible mob=phalanx id=4ea2e62a-… tick=1938 reason=fired` and `SHOT fired … tick=1938 … from=-91.500 65.100 14.500`, the same tick (visible logged first). **MATCH.** Not predicted: `SHOT ended … age=4 … reason=owner-gone` after the row's kill |

### BS1 — the first hit is eaten whole, and only the first (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `SHIELD tag=bs1 <uuid> up=true offhand=shield`; **`SHIELD absorbed mob=phalanx id=<uuid> tick=<t> amount=30.000 by=dealer=<Ben> element=null amount=30.000`**; `SHIELD tag=bs1 <uuid> up=false offhand=none`; `PROBE … customHP=240.000/240.000`; after the second `rpg mobdamage 30`, **`PROBE … customHP=210.000/240.000`**, and no second `SHIELD absorbed` | `MobShielder.absorb`; `/rpg mobdamage`'s two-argument `applyDamage` (element null) | READ 2026-11-17: `SHIELD tag=bs1 … up=true offhand=shield`; `SHIELD absorbed mob=phalanx id=281c78cb-… tick=1978 amount=30.000 by=dealer=b6ae27e9-… element=null amount=30.000`; `SHIELD tag=bs1 … up=false offhand=none`; `customHP=240.000/240.000`; after the second, `customHP=210.000/240.000`; one `SHIELD absorbed`. **MATCH** |

### BS2 — a melee hit's push goes with it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `MOTION tag=bs2 … vx=0.0000 vy=0.0000 vz=0.0000` before; after the first hit **`SHIELD knockback cancelled mob=<uuid> cause=ENTITY_ATTACK shield=up`** in the hit's tick, then **`SHIELD absorbed … amount=<the emberblade's hit>`** a tick later (our damage is scheduled through `onEntity`, one tick on); `MOTION … vx=0.0000 vy=0.0000 vz=0.0000`; `PROBE … customHP=240.000/240.000`. After the second hit: no SHIELD line; **`MOTION … vx=<positive>`** (pushed east, away from him: **the row's positive control**); `PROBE … customHP=<240 − that hit>/240.000`. **If the second `MOTION` also reads zero, the instrument cannot see a push on a frozen mob and the row's push half is VOID, not PASS** | `RpgListeners.onShieldedKnockback`; `onCombatKnockback` passes a landed melee hit | READ 2026-11-17: `MOTION … vx=0.0000 vy=0.0000 vz=0.0000`; `SHIELD knockback cancelled mob=903b68e1-… cause=ENTITY_ATTACK shield=up`; `SHIELD absorbed … tick=2018 amount=56.000 … element=fire amount=56.000`; MOTION zero; `customHP=240.000/240.000`. The second hit: no SHIELD line; **`MOTION … vx=0.4000 vy=0.0000 vz=0.0000`**, the positive control positive, so the push half is **not VOID**; `customHP=184.000/240.000`. **MATCH.** *A tick later* is UNREAD: the cancel line carries no tick |

### BS3 — nothing rides through (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The first arrow: **`SHIELD absorbed … amount=<the short bow's hit>`** and, **in the same tick**, **`SHIELD rider suppressed mob=phalanx id=<uuid> tick=<the same> what=knockback=0.100`**; `MOTION … vx=0.0000 vy=0.0000 vz=0.0000`; `SHIELD … up=false offhand=none`; `PROBE … customHP=240.000/240.000`. The second arrow: no SHIELD line; **`MOTION … vx=<positive>`** (the positive control, as BS2's); `PROBE … customHP=<240 − that hit>/240.000` | `short_bow.yml`'s `on_hit` (`weapon_damage`, then `knockback 0.1`); `MobShielder.suppressRider` | READ 2026-11-17: `SHIELD absorbed … tick=2074 amount=65.000 … element=kinetic amount=65.000` and `SHIELD rider suppressed … tick=2074 what=knockback=0.100`, the same tick; MOTION zero; `up=false offhand=none`; `customHP=240.000/240.000`. The second arrow: no SHIELD line; **`MOTION … vx=0.0170 vy=-0.0985 vz=0.0000`**, the positive control positive (small: BS2's was 0.4000), so **not VOID**; `customHP=175.000/240.000`. **MATCH** |

### BS4 — /kill ignores it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `SHIELD tag=bs4 <uuid> up=true offhand=shield`; after the kill, `SHIELD tag=bs4 none within <REACH> blocks` (`SelfTestDrivers.tagged`'s none line, whose head
this branch gave PROBE_SHIELD: it read `MOTION` for every caller but thaw); **no `SHIELD absorbed` line in the row** | `VanillaDamagePolicy`: KILL is PASS, returned before the reroute | READ 2026-11-17: `SHIELD tag=bs4 … up=true offhand=shield`; after the kill, `SHIELD tag=bs4 none within 16 blocks`; no `SHIELD absorbed` in the row. **MATCH** |

## BEN'S ROW — BY THE ASK STEP, SURVIVAL

### BQ1 — see it, take a shot, chase it, hit it (SURVIVAL)

| AUTO half, recorded beside his answers | Ben's half | READING |
|---|---|---|
| Every `SHOT fired`, `SHOT calibrated`, `SHOT hit`/`destroyed`, `BLINK`, `BLINK moved`, `BLINK visible`, `SHIELD absorbed`, `SHIELD rider suppressed` and `SHIELD knockback cancelled` line in the row. **Not predicted: it is his play.** Every `BLINK` line has `trigger=player <Ben>` and at most one follows each `BLINK visible` or the spawn | `BQ1 ASK 1 question="Does the glowing shape sit where you can hit it?" answer=…`; `ASK 2 question="Did the shot shove you?"`; `ASK 3 question="Did the Phalanx blink away when you got close?"`; `ASK 4 question="Did its shield visibly eat your first hit?"` | READ 2026-11-17: four shots fired and four hits; five BLINK decisions and four moves; one absorb and two vanilla pushes cancelled; slain by Ben with the Emberblade at 12:43:42. Every BLINK line has `trigger=player b6ae27e9-…`. **"At most one follows each `BLINK visible`" is NOT MET:** after `BLINK visible … tick=2647` came two decisions, tick 2647 `tried=8 … landed=none` (from y 63.305, in the air) and tick 2648 `tried=1 … landed`; one MOVE per visible holds. His answers: ASK 1 `"yes"`; ASK 2 `"I don't think so"`; ASK 3 `NOT STATED after 1200 ticks`; ASK 4 captured `"yes but it needs a larger cooldown. Say 12 seconds. Answer for question 3"`; in chat after the run, 12:46:02, `question 4: yes it did`. All in order by tick under READINGS |

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

## READINGS — THE BOOT OF `ef770b8a`, 2026-11-17

### The carried state, as done

1. No JVM: `Get-Process java` printed nothing.
2. The backup `C:\Users\Neb91\IdeaProjects\rpg-run-backups\2026-11-17-pre-dragon-blink` (no suffix needed):
   `run/plugins/Rpg` 129 files 392039 bytes against the copy's 129 files 392039 bytes, `diff -rq lines: 0`;
   `run/world` 193 files 204963880 bytes against 193 files 204963880 bytes, `diff -rq lines: 0`.
3. `12:  dev-pack-on-join: true`.
4. The world's saved difficulty, read by the setup: `SELFTEST dragon-blink setup DIFFICULTY found=NORMAL set=NORMAL`.
5. `./scripts/dev-server.sh --refresh-content` at the branch's head (`git rev-parse HEAD` `ef770b8a…`, tree clean);
   `Done (7.065s)!` at 12:41:26.
6. Ben's LOGIN: `[12:41:33] … BaronVonYeetus[/…] logged in with entity id 10`. Every reading is after it.

### The run

`[12:42:27] … [Rpg] SELFTEST RUN START dragon-blink rows=17 selftest=ef770b8a player=b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5`,
then all 17 rows `DONE`, then `SELFTEST RUN FINISHED` at 12:45:56 and
`SELFTEST RUN RESTORED xp=712595 cell=mage/fire gamemode=SURVIVAL held=3 mana=100.0 trace=false spawn_mobs=false
time=6000 blocks=45200 mobsRemoved=0 orbsRemoved=0 keep_inventory=false immediate_respawn=false difficulty=NORMAL`.
The stop was clean: `/stop` at 12:46:18, `Saving players`, `Saving worlds`, every dimension's chunks saved, and the JVM
gone (`Get-Process java` printed nothing).

**The departure, recorded apart:** Ben's first command after login was `[12:41:56] … issued server command: /give
BaronVonYeetus minecraft:cooked_beef 4`, before `/gamemode survival` (12:42:10). It touches no row: every row heals
him first and none reads his inventory. The boot was not restarted.

**Every LEFTOVER and LINGER line that is not empty:** one, `BC2B LINGER work=1 entities=0 radius=48`, as predicted.
Every LEFTOVER reads `living=0 radius=16`. **Every line matching Refusing, Skipping, SEVERE, WARN or an exception
after the login:** none.

**Every HURT line, eight:** BC1X, BC1Z, BC4A, BC4B and four in BQ1, each `before=40.000 after=36.364 defense=10.000`
with `dealerType=wither_skeleton/phalanx`, each directly after a `SHOT hit` on him. **No hit came from anything but a
shot.**

### BQ1, in order by tick (P is the Phalanx `546b5df1-…`, Ben is `b6ae27e9-…`)

| time | line |
|---|---|
| 12:43:20 | `SHOT fired P gs=100 forced=false tick=2287 shot=c22a5087-… from=-91.822 65.100 3.995 aim=-99.500 64.620 6.500 dealt=40.000` |
| 12:43:22 | `SHOT hit P shot=c22a5087-… age=33 at=-98.085 64.708 6.039 flown=6.600 victim=player Ben dealt=40.000`, and its HURT |
| 12:43:24 | `CMD /rpg heal 1000000` |
| 12:43:26 | `BLINK P tick=2390 forced=false trigger=player Ben from=-91.867 63.000 3.603 tried=1 [324.649 6.865 OK at=-86.268 63.000 -0.369] landed=-86.268 63.000 -0.369`; `BLINK moved … invisible=true` |
| 12:43:26 | `BLINK visible P tick=2407 reason=fired`; `SHOT fired … tick=2407 shot=d4d0a3d1-…` |
| 12:43:28 | `SHOT hit … shot=d4d0a3d1-… age=31 at=-91.638 64.723 1.137 flown=6.142`, and its HURT |
| 12:43:32 | `BLINK P tick=2514 … from=-84.754 63.000 -2.684 tried=1 [330.785 8.458 OK at=-77.371 63.000 -6.813] landed=…`; `BLINK moved … invisible=true` |
| 12:43:32 | `BLINK visible P tick=2527 reason=fired`; `SHOT fired … tick=2527 shot=1fab721e-…` |
| 12:43:33 | `BLINK P tick=2535 … from=-77.664 63.000 -5.987 tried=3 [307.908 12.890 NO_FOOTING blocked=0 ungrounded=7, 10.285 13.606 NO_FOOTING blocked=0 ungrounded=7, 117.185 13.648 OK at=-83.900 63.000 6.154] landed=-83.900 63.000 6.154`; `BLINK moved … invisible=true` |
| 12:43:33 | `SHOT hit … shot=1fab721e-… age=14 at=-80.127 65.145 -4.875 flown=2.799`, and its HURT: **the shot flew on after its owner blinked** |
| 12:43:34 | `CMD /rpg heal 1000000` |
| 12:43:35 | `SHIELD knockback cancelled P cause=ENTITY_ATTACK shield=up`, **twice** |
| 12:43:36 | `SHIELD absorbed P tick=2590 amount=56.000 by=dealer=Ben element=fire amount=56.000`: **while it was invisible** (blinked at 2535, not visible again until 2647) |
| 12:43:38 | `BLINK visible P tick=2647 reason=fired`; `SHOT fired … tick=2647 shot=c517c853-… from=-76.631 65.405 8.430` |
| 12:43:38 | `BLINK P tick=2647 … from=-76.631 63.305 8.430 tried=8 [297.823 9.626 NO_FOOTING blocked=3 ungrounded=4, 10.341 9.977 NO_FOOTING blocked=0 ungrounded=7, 115.412 8.258 NO_FOOTING blocked=3 ungrounded=4, 339.498 11.626 NO_FOOTING blocked=0 ungrounded=7, 287.156 12.994 NO_FOOTING blocked=3 ungrounded=4, 193.250 9.815 NO_FOOTING blocked=3 ungrounded=4, 327.053 11.763 NO_FOOTING blocked=0 ungrounded=7, 352.917 13.564 NO_FOOTING blocked=0 ungrounded=7] landed=none` |
| 12:43:38 | `BLINK P tick=2648 … from=-76.473 63.000 8.519 tried=1 [260.325 12.208 OK at=-78.525 63.000 -3.515] landed=-78.525 63.000 -3.515`; `BLINK moved … invisible=true` |
| 12:43:39 | `SHOT hit … shot=c517c853-… age=5 at=-77.354 65.163 7.782 flown=1.000`, and its HURT |
| 12:43:42 | vanilla: `Named entity WitherSkeleton['Phalanx'/86, uuid='546b5df1-…', … x=-75.37, y=63.00, z=-8.12 …] died: Phalanx was slain by BaronVonYeetus using [Emberblade]` |
| 12:43:44 | `CMD /rpg heal 1000000` |

No `SWING`, `SHOT destroyed` or `SHIELD rider suppressed` line in the row. Eight `BOW suppressed` lines, at ticks 2192,
2252, 2312, 2372, 2432, 2492, 2552, 2610 and 2670, none a failure. The row's kill found nothing (`SETTLED dying=0
ticks=0 first=0`): it was already dead.

**His four answers, beside them, verbatim:**

| ASK | question | answer, as the ASK step logged it |
|---|---|---|
| 1 | Does the glowing shape sit where you can hit it? | `answer="yes" ticks=266` |
| 2 | Did the shot shove you? | `answer="I don't think so" ticks=331` |
| 3 | Did the Phalanx blink away when you got close? | `answer=NOT STATED after 1200 ticks` |
| 4 | Did its shield visibly eat your first hit? | `answer="yes but it needs a larger cooldown. Say 12 seconds. Answer for question 3" ticks=627` |

**And in chat after the run, not through the ASK step:** `[12:46:02] [Async Chat Thread - #3/INFO]: <BaronVonYeetus>
question 4: yes it did`. **So, by his own words:** the text the step captured for ASK 4 is his answer to question 3
(the Phalanx blinked, and it needs a longer cooldown, "say 12 seconds"), and question 4's answer is "yes it did". The
ASK step's record is quoted as it logged; this paragraph is the reading of it, and it is his.

### What the run found

- **R1, F17 restated:** the frog's passenger offset is `0.000 0.600 -0.400`, the derived value, not the 0.450 the gate
  carried. See the dated note under THE AUTO ROWS.
- **R2, the blink has no cooldown, and Ben asks for one: "Say 12 seconds"** (240 ticks). That is UNRULED and not
  built. The run shows why: BQ1's blinks came at ticks 2390, 2514, 2535 and 2648. The gap 2514 to 2535 is 21 ticks,
  because firing makes it visible, and visible within reach is the trigger again. It was invisible 2390 to 2407 (17 ticks), 2514
  to 2527 (13), 2535 to 2647 (112) and from 2648 to its death, and visible 2407 to 2514 (107), 2527 to 2535 (8) and
  2647 to 2648 (1). **The cadence of his play, not a defect against the record**: cfde822 had no
  cooldown (NOT IN THE RECORD).
- **R3, a decision from the air fails every attempt:** at tick 2647 its feet were at y 63.305, mid-jump, and all eight
  draws on a flat floor read `NO_FOOTING` (`blocked=3 ungrounded=4` or `blocked=0 ungrounded=7`). INFERRED, not read in
  the code: the heights are tried as whole steps from the feet's own y, so from a fractional y none sits on a block's
  top. The next tick, from y 63.000, it landed. It cost one tick, not a blink; but it is a second decision after one
  `BLINK visible`, which is why BQ1's "at most one" line is NOT MET.
- **R4, BC4B's husk outlived its row:** the row's `/kill @e[tag=bc4b]` does not select it (`bc4bh`), and BB1's clean
  removed it (`removed=1`) before its LEFTOVER (`living=0`). The clean worked as built; the row's own end did not.
- **R5, two vanilla pushes cancelled, one absorb:** at 12:43:35 two `SHIELD knockback cancelled` lines, then one
  `SHIELD absorbed` (tick 2590). INFERRED: two swings in the shield's last tick, the first one's damage scheduled a
  tick on; the second one's damage met the shield down. No line reads a melee hit's damage on a mob, so this is UNREAD.
- **R6, the shield ate a hit while the Phalanx was invisible:** Ben found it and hit it while invisible (it blinked
  at 2535 and fired at 2647; the absorb is at 2590).
- **The seat's findings, recorded here as ruled:**
  - what outlives a row and is not ended: a status on the player, his fire ticks, vanilla drops and XP;
  - a server whose own plugins manage scoreboard teams could conflict with `rpg_shot`;
  - a rider that a payload applies BEFORE its own damage, in the absorb's tick, is not caught. **No shipped payload
    does that (READ, the content files at this head):** each list that carries a `knockback` or a `status` has a
    `damage` or `weapon_damage` before it in the same list (`ember_step`, `solar_grenade`'s burst, `sunder`'s burst,
    `void_slash`'s burst, `dragons_breath`, `short_bow`, `withered_shortbow`). That the engine runs a list in order is
    the premise, and this read did not reopen it;
  - UNREAD: zero damage not spending the shield (core test only); the shield byte surviving an unload. BS2's and BS3's
    push halves are not VOID: their positive controls read `vx=0.4000` and `vx=0.0170`.
