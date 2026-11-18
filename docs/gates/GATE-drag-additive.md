# GATE — THE DRAG, ADDITIVE: A PLAYER IS SENT ONLY THE ADD, AND THE SERVER NEVER KNOWS HIS MOMENTUM

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/drag-additive`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

**What it gates.** The seat's ruling of 2026-11-18 (PLAN-dragon-mobs.md, *THE DRAG MUST NOT DEPEND ON PACKET TIMING*):
no design may need the server to know the client's momentum. Each tick of the Phage's one-second drag, a PLAYER's own
client is sent an ADD, which it adds to whatever momentum he carries; the add is a function of his distance and his
footing alone. A mob's drag is unchanged (the server owns its motion). The seat's number: `drag` 0.28 → **0.33**.

**What it supersedes.** Every row of `selftest/GATE-implosion-drag.yml`, RE-PREDICTED here with the next suffix, the
staging unchanged: BL1R4 → BL1R5, BL2R4 → BL2R5, BL3R4 → BL3R5, BL4R4 → BL4R5, BL6R4 → BL6R5, BL7R4 → BL7R5, BL9R4 →
BL9R5, IM1R2-IM6R2 → IM1R3-IM6R3, FZ1R2/FZ3R2/FZ5R2 → FZ1R3/FZ3R3/FZ5R3, DS1R6 → DS1R7, DR1 → DR1R2, DR2 → DR2R2. **BQ2 is
RETIRED, not re-predicted:** its first question (the swell) was answered "yes" in both runs; its drag questions are asked
again in **BQ3**. New: **SD5** (still, from 5), **CS1** (the consistency row), **RB1** (no rubber-band, read across the
run), **BQ3**. The scenario file was renamed `GATE-drag-additive.yml`; `GATE-implosion-drag.yml` no longer ships, and
`GATE-implosion-drag.md` keeps every prediction and reading with a dated note naming these rows.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL; a creative or spectator player is not a Phage's
  target and is not dragged.
- **The difficulty is set by the selftest:** the setup's `difficulty: normal`, its `DIFFICULTY found=… set=NORMAL`, and
  `RESTORED … difficulty=<what it found>`.
- **Gear scores:** 100, except **60** for BL2R5, FZ3R3, FZ5R3, DS1R7 and both of DR2R2's Phages. A Phage's zero-distance
  damage is `60 + 22.5 × (stage − 1)` × g / 100 (`MobBlast.price`), unchanged by this branch.

## THE DRAG AS BUILT, AND ITS NUMBERS

**ON A PLAYER (`Implosion.addStep`):** each of the 20 ticks after the blast, the add toward the origin is
**`min(strength, gain × max(0, h − 0.5))`**: `strength = drag × (1 − d / reach) × (1 − res)` as before, and the HOLD,
`gain` the footing's (`Implosion.holdGain`), `h` his horizontal distance as the server sees him this tick. **His
momentum is never read, estimated or rebuilt.** It reaches him as a `ClientboundExplodePacket` whose knockback his
client ADDS to his motion (`DragImpulse`; B1-B4 below). At or inside the margin nothing is added.

**THE GAIN:** the largest that never carries a still victim past the margin while the server's view of him lags 0 to
`LAG_ALLOWANCE_TICKS` = **2** ticks (CHOSEN, about 100 ms of loop), with no strength to limit it and adds forever: the
impulse response bisected (`Implosion.holdsWithoutOvershoot`). **EXECUTED: stone 0.04067, slime 0.01609, ice 0.00278,
air 0.00195.** At an allowance of 3 the stone gain is 0.0333 and walking away from 3 outruns it (outcome b): the seat's.

| number | value | mark |
|---|---|---|
| `duration_ticks` | 20 | Ben's 1, unchanged |
| `drag` | **0.33** blocks/tick at the origin | **the seat's number**, told to Ben as the seat's (0.28 before) |
| `LAG_ALLOWANCE_TICKS` | 2 | CHOSEN 2026-11-18 |
| gain on stone | 0.04067 per tick per block | EXECUTED from the allowance and the jar's friction |
| margin | 0.5 | CHOSEN 2026-11-17, unchanged |
| the packet's centre | 256 blocks above his feet | CHOSEN (past the client's 32-block particle cut, READ) |

**WHAT IT GUARANTEES, AND WHAT IT DOES NOT.** A victim not himself moving toward the origin is never carried past the
margin, within the allowance (swept over drags 0.1 to 5, reaches 2 to 6, starts 0.55 to the reach, lags 0 to 2, and 200
seeds of jitter within 0 to 2: `aStillVictimIsNeverCarriedPastTheMarginAtAnyLagWithinTheAllowance`). **It does not
guarantee:** a player sprinting INTO the Phage passes it by his own legs (`aPlayerSprintingIntoTheOriginPassesItByHisOwnLegs`);
at a lag of 3 or 4 a still one CAN be carried past the margin (from 3 at lag 4 he rests 0.379 from the feet); near the
margin the add is small, so walking away from inside about 2.9 blocks he holds his ground or gains (`0.096040 / 0.04067`
= 2.36 outside the margin); and on ice or in the air the gain is a tenth or less of stone's, so a jumping player is barely
dragged while he is off the ground.

**THE FOUR OUTCOMES AT A STAGE 5 (reach 6), EXECUTED** through `ImplosionDrag`, `Implosion` and `PlayerTravelModel`
(the client's travel READ from the jar, with the server's view lagging L ticks), pinned by
`ImplosionDragTest.theOutcomeFiguresAsExecuted`:

| outcome | from | lag 0 | lag 1 | lag 2 | lag 3 | lag 4 |
|---|---|---|---|---|---|---|
| **a** still: end of the second (server's view) / rest | 2 | 0.727 / 0.679 | 0.697 / 0.631 | 0.659 / 0.578 | 0.610 / 0.512 | 0.561 / 0.429 |
| | 3 | 0.878 / 0.796 | 0.829 / 0.715 | 0.764 / 0.626 | 0.683 / 0.515 | 0.602 / 0.379 |
| | 4 | 1.056 / 0.935 | 1.008 / 0.832 | 0.947 / 0.711 | 0.888 / 0.565 | 0.868 / 0.389 |
| | 5 | 1.658 / 1.397 | 1.796 / 1.346 | 2.028 / 1.293 | 2.385 / 1.267 | 2.795 / 1.281 |
| **b** walking away, end: from rest / already walking | 3 | 2.920 / 2.964 | 2.918 / 2.964 | 2.916 / 2.964 | 2.917 / 2.972 | 2.928 / **3.001** |
| **c** sprinting from rest: the nearest start that gets out in the second | — | 3.9 | 4.0 | 4.1 | 4.2 | 4.3 |

**a:** every start is dragged in for all 20 ticks; within the allowance none rests nearer than 0.578. **b holds at lags
0 to 3, NARROWLY** (he ends about 0.03 to 0.08 nearer); at lag 4, already walking, he ends 3.001. **c:** from 4.4 or
farther he is out at every lag 0 to 4, and from 3.5 at none; **the seat expected the sprint-out distance to move
outward, and it did:** at 0.28 and lag 1 it was 3.8, at 0.33 it is 4.0. **d:** within the allowance no tick is nearer
than the margin; inside it nothing is ever sent, even to one drifting outward (IM4R2's miss closed: the old cap braked
his drift; this one never reads it).

**THE MODEL'S SENSITIVITY TO LAG, the basis of every band below.** The server's view lags by a number of ticks nothing
in the plugin controls (his move packet waiting for the server's tick, the network both ways). **Every band below is the
model's range over lag 0 to 4, widened by 0.10 each side (CHOSEN: no reading of this route exists yet; the ± 0.20 of
`GATE-implosion-drag.md` J5 was the old estimate's residual, which this branch removes).** Two figures are read, and lag
moves them differently: **the DRAG line's `end`** is where the server saw him at tick 20, so while he is still moving it
runs behind by the lag; **his feet 45 ticks after** (`SELF`) is where he came to rest.

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC

**The floor:** his maximum is at least 100. Every row that can hit him first runs `rpg heal 1000000`, and CS1 and BQ3 heal
before each blast. **The drag deals nothing.** **No fall:** no lift. The most any one heal must cover, before defence:
BL1R5 and FZ1R3 a live stage 1 at GS 100, **60**; BL2R5 and FZ5R3 a live stage 5 at GS 60, **90**; FZ3R3 **63**; BL9R5,
DR1R2 and BQ3's third **56.250** (at 3.00); IM3R3 **83.333**; IM4R3 **47.175**; CS1 and BQ3's first two **33.333** each,
a heal before each; SD5 at 5.00 **14.583**; DS1R7 two thawed children on one heal **85.500**; DR2R2 A at 3.00 then B no
nearer than 2.90, GS 60: 33.750 + 35.262 = **69.012**. Every figure is the shape's own (`vanillaShare`), as
`GATE-implosion-drag.md` computed it; SD5's and DR2R2's B are new, EXECUTED (scratch `e.jsh`, the same formula).

## B1-B4: WHAT WAS READ BEFORE BUILDING, AND THE ROUTE

**Every line READ with `C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe -c -p -cp <jar> <class>`** (`-v` for a
lambda's target), the client jar `C:\Users\Neb91\AppData\Roaming\.minecraft\versions\26.1.2\26.1.2.jar` where the
question is the client's, the server jar `run/versions/26.1.2/paper-26.1.2.jar` where it is the server's; PacketEvents
from its pinned 2.13.0 sources jar.

- **B1, THE ADDITIVE POSITION PACKET.** The client's `ClientPacketListener#handleMovePlayer(ClientboundPlayerPositionPacket)`
  calls `setValuesFromPositionPacket` → `PositionMoveRotation.calculateAbsolute(current, packet, relatives)` and
  `Entity#setDeltaMovement` of the result (READ; that a `DELTA_*` relative makes the delta current + packet is the
  brief's reading, NOT re-read here: `calculateAbsolute`'s body was not disassembled). Then, EVERY time, it sends
  `ServerboundAcceptTeleportationPacket(id)` AND a `MovePlayer.PosRot` of his position, calls
  `BlockStatePredictionHandler#onTeleport`, and `setOldPosAndRot` (READ). **Paper's API cannot send it with a delta:**
  `CraftEntity#teleport0` builds its `TeleportTransition` with `Vec3.ZERO` as the delta movement (READ); the
  `TeleportFlag.Relative.VELOCITY_*` flags (`deltaRelativeToNMS` → `Relative.DELTA_X/Y/Z`, READ) only KEEP his velocity.
  PacketEvents 2.13.0 could build it (`WrapperPlayServerPlayerPositionAndLook`), with an id we choose.
- **B2, WHAT THE SERVER DOES WHILE IT AWAITS THE CONFIRM** (`ServerGamePacketListenerImpl`, READ):
  `internalTeleport` increments `awaitingTeleport` (wrapping at `Integer.MAX_VALUE` to 0, so the server's ids are never
  negative), sets `awaitingPositionFromClient` to the position it computed, and sends the packet with that id.
  `handleMovePlayer` calls `updateAwaitingTeleport`, which while `awaitingPositionFromClient` is set returns true, and the
  move then keeps ONLY his rotation (`absSnapRotationTo`, then return): **every move packet until the confirm is dropped.**
  `handleAcceptTeleportPacket`: an id equal to `awaitingTeleport` with nothing awaited **disconnects him**
  (`multiplayer.disconnect.invalid_player_movement`); an equal id with a position awaited SNAPS the server's copy of him
  to that position (`absSnapTo`); any other id does nothing. **So, through the API, one every tick for 20 ticks: each
  send restarts the wait, his moves are dropped for most of the second, and each confirm snaps the server to where it
  sent him; every second tick is the same with half the sends. Not safe at either rate.** Through PacketEvents with an
  id the server never uses (negative), the server would ignore the confirm, but the client would still answer every
  packet with a confirm and an extra move, and reset its render interpolation each tick (`setOldPosAndRot`).
- **B3, WHAT ELSE THE API'S TELEPORT DOES:** not read further; B2 already rules the API route out at the drag's rate.
- **B4, THE EXPLOSION'S KNOCKBACK: ADDITIVE, AND THE ROUTE TAKEN.** `ClientPacketListener#handleExplosion` (READ): plays
  the packet's sound at its centre (`ClientLevel#playLocalSound`, volume `4.0f`), adds the packet's one particle at the
  centre (`ClientLevel#addParticle`), hands the block-particle list to `ClientLevel#trackExplosionEffects`, then
  `playerKnockback().ifPresent(player::addDeltaMovement)` (the lambda's target, `-v`: `Entity.addDeltaMovement`).
  `Entity#addDeltaMovement` is `setDeltaMovement(getDeltaMovement().add(v))` when the vector is finite, with no override
  in `LocalPlayer`, `AbstractClientPlayer`, `Player` or `LivingEntity` (READ). **No teleport id, no confirm, nothing on
  the server.** Invisible and silent: `ClientExplosionTracker#track` returns at once on an EMPTY list (READ);
  `ClientLevel#doAddParticle` drops a particle whose type's override-limiter is false beyond `1024.0d` (32 blocks
  squared) of the camera (READ), and `smoke` is registered with false (`ParticleTypes`: `register("smoke", false)`,
  READ); the sound `intentionally_empty` (`SoundEvents.EMPTY`) is special-cased by the client's `SoundEngine#play`
  (`SoundManager.INTENTIONALLY_EMPTY_SOUND`, READ; that it then plays nothing is INFERRED from that branch and the name),
  and the centre sits 256 blocks above him. PacketEvents' `WrapperPlayServerExplosion` writes, at V_1_21_9 and later,
  the seven fields in the order of the server's `ClientboundExplodePacket.STREAM_CODEC` (READ: centre, radius, block
  count, optional knockback, particle, sound, block particles).
- **THE ROUTE: B4.** It is safe at one packet a tick because there is nothing to await. **Invariant 4:** the API can say
  "set" (`setVelocity`, which replaces) and "teleport" (which waits for a confirm and drops his moves), never "add";
  `World#createExplosion` is a real explosion. Said in `DragImpulse`'s javadoc.

## WHAT THE ROWS STAND ON — EACH FACT, AND ITS MARK

- **K1. The add is ADDED by his client, READ** (B4), before his next tick (`Minecraft#runTick`: `processQueuedPackets`
  before `tick`, READ for #205).
- **K2. The add is a function of his distance and footing alone, BUILT** (`Implosion.addStep`); `ImplosionDrag` never
  tallies a player's add (`twoDragsOnOnePlayerInOneTickEachSendItsOwnAdd`).
- **K3. The lag is the one thing the model cannot know, so every band is its range over lag 0 to 4** (above).
- **K4. The drag's ticks:** tick 0 is the blast's (it only looks); ticks 1 to 20 pull. A player is now pulled from his
  FIRST sighting inside the reach, so one inside throughout reads `ticks=20`. `start` equals the BLAST entry's `d`.
- **K5. `sent`:** a player is sent an add on every pulled tick outside the margin, so `sent` is `ticks` unless he is at
  or inside the margin on some tick (the model's figure is given).
- **K6. The BLAST line is untouched** but for the head's `drag=0.330`: every victim entry is the string
  `GATE-implosion-drag.md` predicted and read for the row this one supersedes (this branch does not touch
  `MobBlaster.entry`), with `pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false`. **A figure at a
  rounding half is predicted from the game's own distance** (BL6R5 and FZ1R3, `1.9500000000000028` and
  `0.9500000000000028`, as read on 2026-11-18). **A Phage at exactly the inclusive reach is a spared entry** (DR2R2).
- **K7. The DRAG line**, as before, and with the instrument on, a player's ends ` movesMin=<n> movesMax=<n>`
  (`ImplosionDragsLineTest`).
- **K8. THE DRAGTICK LINE** (Part D): `[Rpg] DRAGTICK drag=<the Phage> t=<0..20> victim=<Ben> at=<x y z> d=<d>
  moves=<n|-> add=<vx vz|-> cap=<bound|-> ping=<ms> keys=<held|->`, one per drag tick per player victim, from the
  setup's `DRIVE drag_tick on=true` to the run's `RESTORED … drag_tick=false`. `at` and `d` are the server's view the add
  was computed from; `moves` his position-bearing move packets since this drag's last tick, counted on the Netty thread
  (`MoveCountListener`); `cap` the hold's bound. **Moves are RECORDED, never judged.**
- **K9. HURT:** every hit is predicted in its `HURT before=<dealt>` line, never in a health figure.

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-drag-additive"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   If the folder exists, add `-2`. Report the folder, both pairs of counts, and `diff -rq lines: 0` for each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **Boot this branch's head with `--refresh-content`:** `./scripts/dev-server.sh --refresh-content`, announcing
   "booting now", on Ben's "ready". **REQUIRED:** the deployed `phage.yml` reads `drag: 0.28`.
5. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` names this branch's head.
6. **Tell Ben to wait for the word that R0 passed before he logs in.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **Wait for my word that R0 passed before you log in.**
1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest drag-additive` — **ONCE.** It takes about **three minutes**, plus the time you take to answer (2415
   ticks of waits, about 121 seconds, plus each row's staging; 22 rows). Stand still and touch nothing, **except when
   gold text in chat tells you to do something, or a gold question appears**: do what the gold text says, and answer each
   question in chat. There are **six** questions, all at the end. **Many blasts will drag you toward them for a second;
   that is expected. Stand still again afterwards.** Wait for the chat to say the run finished.
3. `/stop`

Nothing else this boot. **A second run is welcome AFTER you have answered all six, if you tell me first**: the seat wants
to compare runs. It is read apart from the first.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/drag-additive` prints it, shortened. **Not** master's `401216aa`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries the add, the instrument, the 0.33 and the renamed rows

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/mobs/phage.yml` line `  drag: 0.33`, ABSENT `  drag: 0.28` (the control); `ImplosionDrags.class` carrying `DRAGTICK drag=` and `movesMin=`; `DragImpulse.class` carrying `INTENTIONALLY_EMPTY`; the entries `.../paper/packet/MoveCountListener.class` and `.../paper/adapter/DragTrace.class`; `Scenario$Verb.class` carrying `DRAG_TICK`; `Implosion.class` carrying `holdGain`; ABSENT the text control `NoSuchTextControl` in `ImplosionDrags.class`; `selftest/GATE-drag-additive.yml` PRESENT with keys `CS1:`, `BQ3:` and `DR2R2:`; the entry `selftest/GATE-implosion-drag.yml` ABSENT; `selftest/GATE-dragon-blast.yml` key `KS1:` PRESENT (the key control) | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$p = Bytes 'content/mobs/phage.yml'
foreach ($l in '  drag: 0.33', '  drag: 0.28') { if ($p -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT phage.yml '$l'" } else { "ABSENT  phage.yml '$l'" } }
$d = Bytes 'io/github/butterflysmp/rpg/paper/adapter/ImplosionDrags.class'
foreach ($t in 'DRAGTICK drag=', 'movesMin=', 'NoSuchTextControl') { if ($d -ne $null -and $d.Contains($t)) { "PRESENT ImplosionDrags '$t'" } else { "ABSENT  ImplosionDrags '$t'" } }
$i = Bytes 'io/github/butterflysmp/rpg/paper/adapter/DragImpulse.class'
if ($i -ne $null -and $i.Contains('INTENTIONALLY_EMPTY')) { "PRESENT DragImpulse 'INTENTIONALLY_EMPTY'" } else { "ABSENT  DragImpulse 'INTENTIONALLY_EMPTY'" }
foreach ($e in 'io/github/butterflysmp/rpg/paper/packet/MoveCountListener.class', 'io/github/butterflysmp/rpg/paper/adapter/DragTrace.class', 'selftest/GATE-implosion-drag.yml') { if ($zip.GetEntry($e) -ne $null) { "PRESENT entry $e" } else { "ABSENT  entry $e" } }
$s = Bytes 'io/github/butterflysmp/rpg/paper/selftest/Scenario$Verb.class'
if ($s -ne $null -and $s.Contains('DRAG_TICK')) { "PRESENT Scenario`$Verb 'DRAG_TICK'" } else { "ABSENT  Scenario`$Verb 'DRAG_TICK'" }
$c = Bytes 'io/github/butterflysmp/rpg/core/shape/Implosion.class'
if ($c -ne $null -and $c.Contains('holdGain')) { "PRESENT Implosion 'holdGain'" } else { "ABSENT  Implosion 'holdGain'" }
foreach ($k in @(@('drag-additive', 'CS1'), @('drag-additive', 'BQ3'), @('drag-additive', 'DR2R2'), @('dragon-blast', 'KS1'))) {
  if ((Bytes "selftest/GATE-$($k[0]).yml") -match "(?m)^$($k[1]):\s*$") { "PRESENT $($k[0]) $($k[1]):" } else { "ABSENT  $($k[0]) $($k[1]):" }
}
$zip.Dispose()
```

**A jar from master reads ABSENT on every PRESENT line above but `KS1:`**, PRESENT on `  drag: 0.28` and on the entry
`selftest/GATE-implosion-drag.yml`; `NoSuchTextControl` is ABSENT on both (the class needle's control).

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**: `GATE-implosion-drag.md` R0c's reading, unchanged (no content file added or removed); **5 mobs: `phage.yml` loads**. No `Refusing`, `Skipping`, `SEVERE` or `Exception`; no `[Rpg]` `WARN` beyond `volley_stone`'s known pair | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 169 scenarios across 21 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive]`**, and **no** `Refusing selftest` | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | |

**The 169 is RECOUNTED:** in `paper/src/main/resources/selftest/`, `grep -cE '^  - ' index.yml` prints **21**; `cat $(grep
-E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints **190**; with
`'^setup:'`, **21**: **169**. **The control: at master `401216aa` the same commands print 21, 188, 21: 167.**

### R0e — the swell packet's self-check

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Phage swell packet: index=16 type=int typeId=1 server=V_26_1_2 readFor=V_26_1`**, at INFO, and **no** line beginning `UNVERIFIED Phage swell packet` (nothing in this branch touches it) | `Select-String run\logs\latest.log -Pattern 'swell packet'` | |

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument for every row:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST drag-additive|\[Rpg\] BLAST|\[Rpg\] DRAG|\[Rpg\] SWELL|\[Rpg\] FUSED|\[Rpg\] SWIRL|\[Rpg\] GROWTH|\[Rpg\] SPLIT|CREEPERGUARD|moved wrongly|moved too quickly'`,
read after Ben's LOGIN, inside the row's own `SELFTEST drag-additive <row> START … DONE` bracket. `[Rpg] DRAG` also
matches every DRAGTICK line.

**In EVERY row (not repeated):** after `CLEAN …`, **`LEFTOVER living=0 radius=16`** and **`LINGER work=0 entities=0
radius=48`**; each spawn's `DRIVE spawn …` and its reply. **No `CREEPERGUARD prime` line. No second HURT for one hit.
Two SWIRL lines per blast, `ticks=20`. Every BLAST head reads `pull=0.00 lift=0.00 drag=0.330 duration=20`.** A
non-zero LEFTOVER or LINGER makes its row NOT AS PREDICTED. Every victim's `res=0.000`. **Every drag on him prints 21
DRAGTICK lines (`t=0` to `t=20`) before its DRAG line, `t=0` with `moves=- add=- cap=-`.** **His feet:** `SELF …
onGround=true fall=0.000`, y and z unchanged ± 0.10; x as each row says.

### BL1R5 — a LIVE stage-1 Phage walks in, lights inside its 1-block trigger, blows, and drags him in (SURVIVAL) — supersedes BL1R4

| prediction | stands on | READING |
|---|---|---|
| (1)-(6), (8), (9) as BL1R4 predicted them (BLOCKS, one SWELL `trigger=1.000 cancel=2.333`, D0 < 1.000, FUSED +30, one BLAST `stage=1 gs=100 base=60.000 reach=2.00 pull=0.00 lift=0.00 drag=0.330 duration=20 … victims=1 [player <Ben> d=<D> …]`, D < 1.000, the entry by the shape from D; two SWIRL `radius=2.000`; one HURT `before=<dealt>`; `ENTITIES CREEPER count=0`, the floor restored; no GROWTH, no RESET). (7) **Exactly one** `DRAG … start=<D> end=<E> ticks=20 sent=<s> outcome=held movesMin=<a> movesMax=<b>`: **if D ≥ 0.6, E in [0.40, 0.68]** (the model from 0.6 to 1.0, lag 0 to 4: 0.504 to 0.576, ± 0.10), `sent=20`; **if D ≤ 0.5, `sent=0` and E within 0.10 of D** (nothing is sent inside the margin); between, RECORDED | K4-K8; BL1R4's reading (D = 0.491) | |

### BL2R5 — a LIVE stage-5 Phage at GS 60, two blocks east, lights at once, blows at 2.000 and drags him in (SURVIVAL) — supersedes BL2R4

| prediction | stands on | READING |
|---|---|---|
| (0)-(6), (8), (9) as BL2R4 predicted them, the head `drag=0.330`; at D = 2.000 the entry `player <Ben> d=2.000 factor=0.555556 dealt=50.000 knock=0.666667 res=0.000 h=2.000 ground=true tf=2.202643 strength=0.000000 cap=0.681000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false` (BL2R4's reading, character for character); HURT `before=50.000`. (7) **Exactly one** `DRAG … start=2.000 end=<E> ticks=20 sent=20 outcome=held movesMin=<a> movesMax=<b>`, **E in [0.46, 0.83]** (model 0.561 to 0.727) | K4-K8 | |

### BL3R5 — a blast is a Void death: a stage-1 Phage 3.00 away grows to stage 2, and nobody is dragged (SURVIVAL) — supersedes BL3R4

| prediction | stands on | READING |
|---|---|---|
| As BL3R4 predicted and read, with the head `… drag=0.330 duration=20 visual=void_implosion victims=0 [] spared=0 []`: one GROWTH `from=1 to=2 … d=3.000 max=100`, `BODY tag=bl3b … stage=2`, `BODY tag=bl3x none within 16 blocks`, `ENTITIES CREEPER count=1`, two SWIRL `radius=2.000`. **No DRAG line and no DRAGTICK line** (no player ever inside the reach), no HURT, no SWELL | BL3R4's reading; K8 | |

### BL4R5 — a husk and a stage 5 in a STAGE-5 blast's reach are neither hurt NOR DRAGGED; a control proves the probe sees a velocity (SURVIVAL) — supersedes BL4R4

| prediction | stands on | READING |
|---|---|---|
| As BL4R4 predicted and read, with the head `drag=0.330`: the control `MOTION tag=bl4c … vz=0.5000`; `spared=2 [husk <bl4h> d=2.000 factor=0.555556, creeper/phage <bl4p> d=2.000 factor=0.555556]` (either order); all six later MOTION lines `vx=0.0000 vy=0.0000 vz=0.0000`; both PROBE lines unchanged; **no DRAG line and no DRAGTICK line** (the player at 7.280 is outside); `ENTITIES CREEPER count=1`; no HURT | BL4R4's reading; the predicate in `MobBlaster` | |

### BL6R5 — THE STAGE-1 EDGE, inside (1.95 of 2): a sliver of 60, and the drag brings him in (SURVIVAL) — supersedes BL6R4

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> <y0> <z0> onGround=true fall=0.000`. (2) `DRIVE detonate tag=bl6 <uuid>`. (3) the entry `d=1.950 factor=0.012812 dealt=0.769 knock=0.025000 res=0.000 h=1.950 ground=true tf=2.202643 strength=0.000000 cap=0.658300 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false`, **the factor from the game's distance `1.9500000000000028`** (BL6R4's reading). (4) Two SWIRL `radius=2.000`. (5) HURT `before=0.769`. (6) **Exactly one** `DRAG … start=1.950 end=<E> ticks=20 sent=20 outcome=held …`, **E in [0.79, 1.55]** (model 0.888 to 1.445: at the reach's edge his speed at tick 20 is high and the lag shows). (7) Forty-five ticks later `SELF … at=<x1> <y0> <z0>`, **x1 − x0 in [0.98, 1.25]** (model 1.080 to 1.147) | K3-K8 | |

### BL7R5 — THE STAGE-1 EDGE, outside (2.05 of 2): neither hit nor dragged (SURVIVAL) — supersedes BL7R4

| prediction | stands on | READING |
|---|---|---|
| As BL7R4 predicted and read, the head `drag=0.330 … victims=0 [] spared=0 []`: two SWIRL `radius=2.000`; **no HURT, no DRAG, no DRAGTICK**; `SELF … at=<x0> <y0> <z0>` again, the same to the printed place. BL6R5 is the control | the stage-1 reach | |

### BL9R5 — OUTCOME a AT 3: a STAGE-5 Phage at half its reach, and standing still he is dragged in (SURVIVAL) — supersedes BL9R4

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> …`. (2) `DRIVE detonate tag=bl9 <uuid>`. (3) the entry at `d=3.000` as BL9R4 read it (`factor=0.375000 dealt=56.250 knock=0.500000 … cap=1.135000 …`). (4) Two SWIRL `radius=6.000`. (5) HURT `before=56.250`. (6) **Exactly one** `DRAG … start=3.000 end=<E> ticks=20 sent=20 outcome=held …`, **E in [0.50, 0.98]** (model 0.602 to 0.878). (7) Forty-five ticks later `SELF … at=<x1> …`, **x1 − x0 in [2.10, 2.72]** (model 2.204 to 2.621); **within the allowance (lag 0 to 2) at most 2.374, he rests no nearer than 0.626 to the Phage's feet**; at lag 3 or 4 the model rests him 0.515 or 0.379 from them, which the DRAGTICK pings would then explain | K3-K8; `theOutcomeFiguresAsExecuted` | |

### SD5 — OUTCOME a AT 5: a STAGE-5 Phage five blocks east; standing still he is dragged in most of the way (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> …`. (2) `DRIVE detonate tag=sd5 <uuid>`. (3) **Exactly one** BLAST `stage=5 gs=100 base=150.000 reach=6.00 pull=0.00 lift=0.00 drag=0.330 duration=20 … victims=1 [player <Ben> d=5.000 factor=0.097222 dealt=14.583 knock=0.166667 res=0.000 h=5.000 ground=true tf=2.202643 strength=0.000000 cap=2.043000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []` (EXECUTED, scratch `e.jsh`: share (x² + x) / 2 with x = 1/6, the cap (5 − 0.5) / 2.202643, printed in the entry's own formats). (4) Two SWIRL `radius=6.000`. (5) HURT `before=14.583`. (6) **Exactly one** `DRAG … start=5.000 end=<E> ticks=20 sent=20 outcome=held …`, **E in [1.56, 2.90]** (model 1.658 to 2.795: he is still moving at tick 20, so the server's view at that tick is spread by the lag). (7) Forty-five ticks later `SELF … at=<x1> …`, **x1 − x0 in [3.50, 3.83]** (model 3.603 to 3.733: where he RESTS is barely moved by the lag) | K3-K8; `theOutcomeFiguresAsExecuted` | |

### CS1 — THE CONSISTENCY ROW: the still staging from 4 at a stage 5, FIVE TIMES (SURVIVAL) — new

**This is the point of the gate.** On 2026-11-18 the same staging read `end=0.491` and `end=1.347` in two runs of one
build. Here it is run five times in one row, the instrument on throughout.

| prediction | stands on | READING |
|---|---|---|
| For each of `cs1a` to `cs1e`, in order: a `SELF … at=<x0>` before; one BLAST `stage=5 gs=100 base=150.000 reach=6.00 … drag=0.330 … [player <Ben> d=4.000 factor=0.222222 dealt=33.333 knock=0.333333 res=0.000 h=4.000 ground=true tf=2.202643 strength=0.000000 cap=1.589000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []` (BQ2's reading at 4.000); one HURT `before=33.333`; two SWIRL `radius=6.000`; 21 DRAGTICK lines; one `DRAG … start=4.000 end=<E> ticks=20 sent=20 outcome=held movesMin=<a> movesMax=<b>`; a `SELF … at=<x1>` 45 ticks later. **EACH END:** E in **[0.77, 1.16]** (model 0.868 to 1.056) and x1 − x0 in **[2.97, 3.71]** (model 3.065 to 3.611). **THE SPREAD BETWEEN THE FIVE:** the largest E less the smallest **≤ 0.29** (the model's 0.188 over lag 0 to 4, plus 0.10); the largest x1 − x0 less the smallest **≤ 0.65** (the model's 0.546, plus 0.10). **The moves counts and pings are RECORDED, not judged:** if they show batching (a 0 beside a 2) and the five ends still agree within these bounds, that is the proof the drag no longer depends on packet timing. **Against #205:** its two readings of this staging, 0.491 and 1.347, differ by 0.856, three times this bound | K1-K8; `theOutcomeFiguresAsExecuted` | |

### RB1 — NO RUBBER-BAND: the server's copy of him is never snapped, and no movement complaint (SURVIVAL) — new, read across the run

**Whatever B2 found, as a row:** the route sends no position packet, so the server never awaits a confirm and never
snaps him; his moves are the only thing that moves the server's copy, as when he walks.

| prediction | stands on | READING |
|---|---|---|
| (1) **No line containing `moved wrongly` or `moved too quickly`** anywhere between the run's START and FINISHED. (2) **In each of CS1's five, his feet 45 ticks after the blast lie past the last DRAGTICK's `at=` by the slide still to come, toward the Phage:** x1 − (tick 20's x) in **[0.02, 0.58]** (the model's rest less its end, 0.121 to 0.479 over lag 0 to 4, ± 0.10); never back toward where he started. (3) Every SELF line in the run `onGround=true fall=0.000`. (4) BQ3's fifth question: Ben's feel | B2; B4; K1 | |

---

## THE IMPLOSION'S OWN ROWS, RE-PREDICTED

### IM1R3 — THE STAGE-5 EDGE, inside (5.90 of 6): a sliver of 150, and a drag faded almost to nothing (SURVIVAL) — supersedes IM1R2

| prediction | stands on | READING |
|---|---|---|
| (1)-(5) as IM1R2 predicted and read them (the entry at `d=5.900 factor=0.008472 dealt=1.271 knock=0.016667 … cap=2.451600 …`, HURT `before=1.271`), the head `drag=0.330`. (6) **Exactly one** `DRAG … start=5.900 end=<E> ticks=20 sent=20 outcome=pulled …`, **E in [5.25, 5.78]** (model 5.355 to 5.680). (7) Forty-five ticks later **x1 − x0 in [0.31, 0.80]** (model 0.413 to 0.695) | K3-K8 | |

### IM2R3 — THE STAGE-5 EDGE, outside (6.05 of 6): neither hit nor dragged (SURVIVAL) — supersedes IM2R2

| prediction | stands on | READING |
|---|---|---|
| As IM2R2 predicted and read, the head `drag=0.330 … victims=0 [] spared=0 []`: **no HURT, no DRAG, no DRAGTICK**; `SELF … at=<x0> …` again, the same to the printed place. IM1R3 is the control | the stage-5 reach | |

### IM3R3 — OUTCOME a AT 2, AND d: dragged in, never past the margin (SURVIVAL) — supersedes IM3R2

| prediction | stands on | READING |
|---|---|---|
| (1)-(4) as IM3R2 predicted and read them (the entry at `d=2.000 factor=0.555556 dealt=83.333 knock=0.666667 … cap=0.681000 …`, HURT `before=83.333`), the head `drag=0.330`. (5) **Exactly one** `DRAG … start=2.000 end=<E> ticks=20 sent=20 outcome=held …`, **E in [0.46, 0.83]** (model 0.561 to 0.727). (6) Forty-five ticks later **x1 − x0 in [1.22, 1.67]** (model 1.321 to 1.571); **within the allowance at most 1.422, no nearer than 0.578 to the Phage's feet** | K3-K8; `dReachingTheMarginHeIsNeverCarriedThrough` | |

### IM4R3 — OUTCOME d INSIDE THE MARGIN: nothing is sent and he is not moved; IM4R2's miss closed (SURVIVAL) — supersedes IM4R2

| prediction | stands on | READING |
|---|---|---|
| (1)-(4) as IM4R2 predicted and read them (xa within [x0 − 0.20, x0]; the entry `d=<D>`, D in [0.300, 0.500), `cap=0.000000`; one HURT `before=<dealt>`), the head `drag=0.330`. (5) **Exactly one** `DRAG … start=<D> end=<E> ticks=20 sent=0 outcome=held …`, **E within 0.10 of D**, and **every one of its 20 pulled DRAGTICK lines `add=- cap=0.000000`**. **This is the clause IM4R2 missed** (`sent=18`): the old cap braked his outward drift out of the overlap; the add never reads his drift, so inside the margin it is zero whatever he does (`dInsideTheMarginNothingIsEverSentEvenToOneDriftingOut`). (6) Forty-five ticks later **xb = xa ± 0.10** | K2, K5; IM4R2's reading | |

### IM5R3 — THE STAGE-3 EDGE, inside (3.95 of 4) (SURVIVAL) — supersedes IM5R2

| prediction | stands on | READING |
|---|---|---|
| (0)-(5) as IM5R2 predicted and read them (`REPLY "Spawned Phage (creeper, stage 3, 120 HP, GS 100 given)"`; the entry at `d=3.950 factor=0.006328 dealt=0.664 …`; two SWIRL `radius=4.000`; HURT `before=0.664`), the head `drag=0.330`. (6) **Exactly one** `DRAG … start=3.950 end=<E> ticks=20 sent=20 outcome=pulled …`, **E in [3.20, 3.86]** (model 3.296 to 3.764). (7) Forty-five ticks later **x1 − x0 in [0.29, 0.99]** (model 0.386 to 0.890) | K3-K8 | |

### IM6R3 — THE STAGE-3 EDGE, outside (4.05 of 4) (SURVIVAL) — supersedes IM6R2

| prediction | stands on | READING |
|---|---|---|
| As IM6R2 predicted and read, the head `drag=0.330 … victims=0 [] spared=0 []`: **no HURT, no DRAG, no DRAGTICK**; his feet unchanged to the printed place. IM5R3 is the control | the stage-3 reach | |

---

## THE FUSE BY STAGE, RE-PREDICTED

**Each row as `GATE-implosion-drag.md`'s FZ rows predicted and read clauses (a)-(i) and (k)-(l)** (`TARGET … target=self`;
one START at `d=<in>`; `fuse=<n>/30 dir=+1`, n in [8, 10]; one RESET at `d=<out>`; drained to `fuse=0/30 dir=-1`; a
second START and `FUSED … tick=<T3 + 30>`; the BLAST below and two SWIRL lines; HURT `before=<dealt>`; `ENTITIES CREEPER
count=0`; no CREEPERGUARD), the head now `drag=0.330`. **(j) His feet:** `SELF` just after the last step in at x = F0.x +
0.10, and 75 ticks later at **x = F0.x + 0.10 + T**, T below. **(m) Exactly one DRAG line.**

### FZ1R3 — stage 1: lights inside 1.000, resets beyond 2.333, and drags him in (SURVIVAL) — supersedes FZ1R2

| prediction | stands on | READING |
|---|---|---|
| (a)-(m), with: in `d=0.950`, out `d=2.400`; the entry at `d=0.950 factor=0.400312 dealt=24.019 knock=0.525000 … cap=0.204300 …`, **the factor from the game's `0.9500000000000028`** (FZ1R2's reading); SWIRL `radius=2.000`; DRAG `start=0.950 end=<E> ticks=20 sent=20 outcome=held …`, **E in [0.42, 0.67]** (model 0.518 to 0.568); **T in [0.29, 0.57]** (model 0.393 to 0.468) | K3-K8; FZ1R2's reading | |

### FZ3R3 — stage 3 at GS 60: lights inside 2.000, resets beyond 4.667, drags him in (SURVIVAL) — supersedes FZ3R2

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 3, 72 HP, GS 60 given)" [aqua, gray]`. (a)-(m), with: in `d=1.950`, out `d=4.750`; the entry at `d=1.950 factor=0.387578 dealt=24.417 knock=0.512500 … cap=0.658300 …`; SWIRL `radius=4.000`; DRAG `start=1.950 end=<E> ticks=20 sent=20 outcome=held …`, **E in [0.46, 0.82]** (model 0.559 to 0.719); **T in [1.18, 1.62]** (model 1.277 to 1.518) | as FZ1R3 | |

### FZ5R3 — stage 5 at GS 60: lights inside 3.000, resets beyond 7.000, drags him in (SURVIVAL) — supersedes FZ5R2

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`. (a)-(m), with: in `d=2.950`, out `d=7.050`; the entry at `d=2.950 factor=0.383368 dealt=34.503 knock=0.508333 … cap=1.112300 …`; SWIRL `radius=6.000`; DRAG `start=2.950 end=<E> ticks=20 sent=20 outcome=held …`, **E in [0.50, 0.97]** (model 0.600 to 0.870); **T in [2.06, 2.67]** (model 2.160 to 2.569) | as FZ1R3 | |

---

## THE SPLIT ROW WITH TWO BLASTS

### DS1R7 — split at GS 60; thawed; each child walks in, lights and blows, and each drags him toward its own feet (SURVIVAL) — supersedes DS1R6

| prediction | stands on | READING |
|---|---|---|
| (1)-(7), (9), (10) as DS1R6 predicted them, each head `drag=0.330`: within the 230 ticks one BLAST for each child, X then Y, X's `spared=<n>` naming Y exactly when Y stood within 2 of X, and then one GROWTH printed with the identical `d`; each victim entry `ground=true`, `h = d`; D1 < 1.000, D2 ≤ Y's cancel; exactly two HURT, X then Y; `ENTITIES CREEPER count=0`; one SPLIT; no CREEPERGUARD. (8) **Exactly two DRAG lines on him**, `id=<X>` then `id=<Y>`, each `ticks=<n>` in [1, 20], and **each `end` and `outcome` RECORDED, not judged:** two origins under a block apart pull toward their own feet, each add cut only by its own margin, and that case is **UNRULED** (DS1R6 read X's end at 0.138) | DS1R6's reading; K2 | |

---

## THE DRAG'S OWN ROWS

### DR1R2 — HE LEAVES THE REACH MID-DRAG: a step moves him out, and the drag stops adding (SURVIVAL) — supersedes DR1

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> …`. (2) `DRIVE detonate tag=dr1 <uuid>`. (3) the entry at `d=3.000` as DR1 read it, the head `drag=0.330`. (4) HURT `before=56.250`. (5) Two ticks after the step, `SELF … at=<x1> …`, **x1 − x0 in [−5.20, −4.85]** (the step's −6 and the drag's ticks east before it: model −5.068 to −4.957, lag 0 to 4). (6) **Exactly one** `DRAG … start=3.000 end=<E> ticks=<n> sent=<n> outcome=left …`, **n in [4, 7]**, **E > 6.000**; after the step every DRAGTICK reads `add=- cap=-`. (7) Forty ticks later `SELF … at=<x2> …`, **x2 − x1 in [−0.05, 0.17]** (the model's slide still to come, 0.057 to 0.073: DR1 read **0.39**, the old send's carried momentum, F-G; the add carries far less) | K2, K4; DR1's reading | |

### DR2R2 — TWO BLASTS FIVE TICKS APART: each drags toward its own origin (SURVIVAL) — supersedes DR2

| prediction | stands on | READING |
|---|---|---|
| (0) Two `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`. (1) `SELF … at=<x0> …`. (2) `DRIVE detonate tag=dr2a <uuid>`, then five ticks later `DRIVE detonate tag=dr2b <uuid>`. (3) A's BLAST, head `drag=0.330`, `[player <Ben> d=3.000 factor=0.375000 dealt=33.750 knock=0.500000 res=0.000 h=3.000 ground=true tf=2.202643 strength=0.000000 cap=1.135000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=1 [creeper/phage <dr2b> d=6.000 factor=0.000000]`: **B stands at A's inclusive reach and is a spared entry** (DR2's reading, F-H). (4) B's BLAST `[player <Ben> d=<DB> …] spared=0 []`, **DB in [2.90, 3.71]** (model 3.000 to 3.609), its fields by the shape from DB. (5) Exactly two HURT, `before=33.750`, then `before=<B's dealt>`. (6) **Exactly two DRAG lines**: A's `start=3.000 end=<EA> ticks=20 sent=20 outcome=held …`, **EA in [2.57, 3.10]** (model 2.669 to 3.002); then B's `start=<DB> end=<EB> ticks=20 sent=20 outcome=held …`, **EB in [2.30, 2.93]** (model 2.399 to 2.825). (7) Four SWIRL lines `radius=6.000`. (8) Fifty ticks after B, **x1 − x0 in [−1.49, −0.88]**: **B, nearer and later, carries him WEST**, past where he started (model −1.390 to −0.984). The two-origin case stays UNRULED | K2-K6; `twoDragsOnOnePlayerInOneTickEachSendItsOwnAdd` | |

---

## BEN'S ROW — BY THE ASK STEP

### BQ3 — the drag four ways, by his own feel, and how it feels (SURVIVAL) — new; BQ2 retired

Four stagings, each told in GOLD before it happens: **(1)** standing still, a stage 5 four blocks in front of him blows
(33.333 before his defence); **(2)** the same again; **(3)** facing a glass wall west and holding W, a stage 5 three blocks
behind him blows as the glass vanishes (model, from 3.2: he ends the second at 2.93 to 2.95, a quarter of a block back
toward it: **narrow**); **(4)** the same, sprinting, from 4.5 behind him (model, from 4.7: out of the reach by tick 8 to
14). Then two questions about the feel. **Each question waits 1200 ticks for his next chat line; his answer cell is his
line, verbatim.**

| prediction | READING |
|---|---|
| Recorded beside the answers, in order: `TELL QUESTION 1 …`; BLAST `… [player <Ben> d=4.000 … dealt=33.333 …]`; HURT `before=33.333`; `DRAG … start=4.000 end=<E> ticks=20 sent=20 outcome=held …`, E in [0.77, 1.16] (as CS1). `TELL QUESTION 2 …`; the same lines, the same bound. `TELL QUESTION 3 …`, `TELL STOP. Let go of W.`; BLAST `… [player <Ben> d=<D3> …]`, **D3 in [3.00, 3.25]**; `DRAG … start=<D3> end=<E3> …`, **E3 < D3 is outcome b's server half** (model from 3.2: 2.932 to 2.950); DRAGTICK `keys=forward` while he holds W (the key, not the direction: he faces west, away from the Phage; INFERRED from `Input#isForward`'s name, so a reading of anything else is RECORDED). `TELL QUESTION 4 …`, `TELL STOP. Let go of W and sprint.`; BLAST `d=<D4>`, **D4 in [4.50, 4.75]**; `DRAG … start=<D4> … ticks=<n> … outcome=left`, **n ≤ 16** (model 8 to 14), **outcome c's server half**, and his DRAGTICK keys include `sprint` | |
| `ASK 1 question="Standing still, did it drag you in toward where it stood, for about a second?" answer="<his line>" ticks=<t>` | |
| `ASK 2 question="Did that drag feel the same as the first one?" answer="<his line>" ticks=<t>` | |
| `ASK 3 question="Walking away, did the drag slow you down without stopping you, so you still went back toward it?" answer="<his line>" ticks=<t>` | |
| `ASK 4 question="Sprinting away from the edge, did you get out of the drag?" answer="<his line>" ticks=<t>` | |
| `ASK 5 question="During any of those drags, did you feel a stutter, or get snapped back to where you were?" answer="<his line>" ticks=<t>` | |
| `ASK 6 question="Does the pull feel right now?" answer="<his line>" ticks=<t>` | |

---

## WHAT NO ROW CAN STAGE — UNIT-ONLY, AND WHY

- **Outcomes b and c by the instrument:** the server cannot press his keys. Staged for Ben (BQ3) and pinned in core.
- **A lag beyond the allowance, on purpose:** the selftest cannot slow his connection. The DRAGTICK pings and moves record
  what lag he had; `theGainIsTheLargestThatHolds` pins that a lag of 3 can overshoot.
- **Ice and the air:** no row stands him on ice or jumps him; the gains are pinned in core.
- **The test-only fixture's drag on MOBS:** unchanged (`ImplosionEffectTest`).
- **A victim who dies, logs out, changes world or goes creative mid-drag:** as `GATE-implosion-drag.md`; the core half is
  `aDroppedVictimIsNeverPushedAgainAndEndsDropped`.

## THE MUTATIONS, EACH APPLIED, MEASURED AND RESTORED (2026-11-18, scratch `mut.sh`)

Each: the single occurrence replaced, `marker present` / `original left` / lines changed against a pristine copy taken
before the edit, the named tests run, then restored by `cp` (`diff lines 0, marker left 0` every time).

| mutation | the edit | reddened |
|---|---|---|
| MUTNOHOLD | `addStep` adds the whole strength, no hold | 7 rows of `ImplosionDragTest`, the sweep among them |
| MUTNORESP | `addStep` drops `(1 − res)` (a superset edit: original left 1, one line changed) | `theResistanceCutsTheStrength` only: its sole guard |
| MUTONELAG | `holdGain` checks lag 0 only | 7, the gain's two rows and the sweep among them |
| MUTNOMARGIN | the hold measured to the origin, not the margin | 8 |
| MUTPLAYERSET | a player's push SET, not ADDED | 8 (the model refuses a SET on a player) |
| MUTPLAYERTALLY | a player's add tallied like a mob's | `twoDragsOnOnePlayerInOneTickEachSendItsOwnAdd` only: its sole guard |
| MUTMOBADD | a mob's push its add alone, no base | 3: two of `ImplosionDragTest` and `ImplosionEffectTest`'s fixture drag |
| MUTSENDSET | `ImplosionDrags` sets a player's velocity | `aPlayersAddGoesThroughTheImpulseAndAMobsIsSet` |
| MUTTICKSWAP | DRAGTICK's ping and keys swapped | both DRAGTICK text rows |
| MUTTRACEON | the run's end turns the trace ON | `theCounterIsRegisteredOnceAndTheRunsEndTurnsTheTraceOff` |
| MUTSOUND | the packet's sound a real explosion's | `theImpulseIsSilentAndUnseen` |
| MUTBUKKITNETTY | `import org.bukkit.Bukkit` in the Netty listener (an insertion: delta read as 1 added) | `thePacketThreadsCounterTouchesNoBukkit` |
