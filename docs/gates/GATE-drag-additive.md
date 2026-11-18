# GATE — THE DRAG, ADDITIVE: A PLAYER IS SENT ONLY THE ADD, AND THE SERVER NEVER KNOWS HIS MOMENTUM

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/drag-additive`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

**2026-11-18: BOOTED at `5ee0587a` on the seat's approval and Ben's "ready", and READ. 27 of 28 rows AS PREDICTED (R0a-R0e
and 22 of the 23 others), 1 NOT AS PREDICTED on one clause: IM4R3 (`sent=20`, predicted 0; he was not moved). CS1's five
ends read 1.056, all five: spread 0.000. BQ3's six answers: "yes", "not sure", "it basically stopped me which is what I
want for walking", "yes", "no", "feels good". Readings beside each prediction; the account under THE BOOT at the end. No
prediction was edited.**

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
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/drag-additive` prints it, shortened. **Not** master's `401216aa`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **AS PREDICTED, 2026-11-18.** `[13:28:35] [Server thread/INFO]: [Rpg] Build: 5ee0587a`; `git ls-remote origin refs/heads/feat/drag-additive` printed `5ee0587abe48c07dac93a4e18e9eb6d9678febea` |

### R0b — the jar carries the add, the instrument, the 0.33 and the renamed rows

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/mobs/phage.yml` line `  drag: 0.33`, ABSENT `  drag: 0.28` (the control); `ImplosionDrags.class` carrying `DRAGTICK drag=` and `movesMin=`; `DragImpulse.class` carrying `INTENTIONALLY_EMPTY`; the entries `.../paper/packet/MoveCountListener.class` and `.../paper/adapter/DragTrace.class`; `Scenario$Verb.class` carrying `DRAG_TICK`; `Implosion.class` carrying `holdGain`; ABSENT the text control `NoSuchTextControl` in `ImplosionDrags.class`; `selftest/GATE-drag-additive.yml` PRESENT with keys `CS1:`, `BQ3:` and `DR2R2:`; the entry `selftest/GATE-implosion-drag.yml` ABSENT; `selftest/GATE-dragon-blast.yml` key `KS1:` PRESENT (the key control) | the scan below | **AS PREDICTED, 2026-11-18.** `PRESENT phage.yml '  drag: 0.33'`, `ABSENT  phage.yml '  drag: 0.28'`, `PRESENT ImplosionDrags 'DRAGTICK drag='`, `PRESENT ImplosionDrags 'movesMin='`, `ABSENT  ImplosionDrags 'NoSuchTextControl'`, `PRESENT DragImpulse 'INTENTIONALLY_EMPTY'`, `PRESENT entry io/github/butterflysmp/rpg/paper/packet/MoveCountListener.class`, `PRESENT entry io/github/butterflysmp/rpg/paper/adapter/DragTrace.class`, `ABSENT  entry selftest/GATE-implosion-drag.yml`, `PRESENT Scenario$Verb 'DRAG_TICK'`, `PRESENT Implosion 'holdGain'`, `PRESENT drag-additive CS1:`, `PRESENT drag-additive BQ3:`, `PRESENT drag-additive DR2R2:`, `PRESENT dragon-blast KS1:` |

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
| **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**: `GATE-implosion-drag.md` R0c's reading, unchanged (no content file added or removed); **5 mobs: `phage.yml` loads**. No `Refusing`, `Skipping`, `SEVERE` or `Exception`; no `[Rpg]` `WARN` beyond `volley_stone`'s known pair | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | **AS PREDICTED, 2026-11-18.** `[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`. The `[Rpg]` WARN lines are `volley_stone`'s pair; the others are Paper's version notice. No `Refusing`, `Skipping`, `SEVERE` or `Exception` anywhere in the log |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 169 scenarios across 21 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive]`**, and **no** `Refusing selftest` | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | **AS PREDICTED, 2026-11-18.** Exactly one `[Rpg] Selftest: dev server, 169 scenarios across 21 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive]`; no `Refusing selftest` |

**The 169 is RECOUNTED:** in `paper/src/main/resources/selftest/`, `grep -cE '^  - ' index.yml` prints **21**; `cat $(grep
-E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints **190**; with
`'^setup:'`, **21**: **169**. **The control: at master `401216aa` the same commands print 21, 188, 21: 167.**

### R0e — the swell packet's self-check

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Phage swell packet: index=16 type=int typeId=1 server=V_26_1_2 readFor=V_26_1`**, at INFO, and **no** line beginning `UNVERIFIED Phage swell packet` (nothing in this branch touches it) | `Select-String run\logs\latest.log -Pattern 'swell packet'` | **AS PREDICTED, 2026-11-18.** Exactly one `[13:28:36] [Server thread/INFO]: [Rpg] Phage swell packet: index=16 type=int typeId=1 server=V_26_1_2 readFor=V_26_1`; no `UNVERIFIED` |

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
| (1)-(6), (8), (9) as BL1R4 predicted them (BLOCKS, one SWELL `trigger=1.000 cancel=2.333`, D0 < 1.000, FUSED +30, one BLAST `stage=1 gs=100 base=60.000 reach=2.00 pull=0.00 lift=0.00 drag=0.330 duration=20 … victims=1 [player <Ben> d=<D> …]`, D < 1.000, the entry by the shape from D; two SWIRL `radius=2.000`; one HURT `before=<dealt>`; `ENTITIES CREEPER count=0`, the floor restored; no GROWTH, no RESET). (7) **Exactly one** `DRAG … start=<D> end=<E> ticks=20 sent=<s> outcome=held movesMin=<a> movesMax=<b>`: **if D ≥ 0.6, E in [0.40, 0.68]** (the model from 0.6 to 1.0, lag 0 to 4: 0.504 to 0.576, ± 0.10), `sent=20`; **if D ≤ 0.5, `sent=0` and E within 0.10 of D** (nothing is sent inside the margin); between, RECORDED | K4-K8; BL1R4's reading (D = 0.491) | **AS PREDICTED, 2026-11-18 (the D ≤ 0.5 branch).** `BLOCKS STONE count=189 …`; one `SWELL … reason=start … viewers=1 d=0.838 trigger=1.000 cancel=2.333 swell=0`; `FUSED` 30 ticks later; `BLAST … stage=1 gs=100 base=60.000 reach=2.00 pull=0.00 lift=0.00 drag=0.330 duration=20 visual=void_implosion victims=1 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 d=0.491 factor=0.661642 dealt=39.699 knock=0.754306 res=0.000 h=0.491 ground=true tf=2.202643 strength=0.000000 cap=0.000000 …] spared=0 []`; `HURT before=39.699 after=36.090 defense=10.000`; `DRAG … start=0.491 end=0.491 ticks=20 sent=0 outcome=held movesMin=0 movesMax=1`: D < 0.5, `sent=0`, E = D; `ENTITIES CREEPER count=0`; the floor restored |

### BL2R5 — a LIVE stage-5 Phage at GS 60, two blocks east, lights at once, blows at 2.000 and drags him in (SURVIVAL) — supersedes BL2R4

| prediction | stands on | READING |
|---|---|---|
| (0)-(6), (8), (9) as BL2R4 predicted them, the head `drag=0.330`; at D = 2.000 the entry `player <Ben> d=2.000 factor=0.555556 dealt=50.000 knock=0.666667 res=0.000 h=2.000 ground=true tf=2.202643 strength=0.000000 cap=0.681000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false` (BL2R4's reading, character for character); HURT `before=50.000`. (7) **Exactly one** `DRAG … start=2.000 end=<E> ticks=20 sent=20 outcome=held movesMin=<a> movesMax=<b>`, **E in [0.46, 0.83]** (model 0.561 to 0.727) | K4-K8 | **AS PREDICTED, 2026-11-18.** `SWELL … d=2.000 trigger=3.000 cancel=7.000`; the entry at `d=2.000` character for character; `HURT before=50.000 after=45.455 defense=10.000`; `DRAG … start=2.000 end=0.725 ticks=20 sent=20 outcome=held movesMin=0 movesMax=1`: 0.725 in [0.46, 0.83] (model at lag 0: 0.727); `ENTITIES CREEPER count=0`, no SPLIT |

### BL3R5 — a blast is a Void death: a stage-1 Phage 3.00 away grows to stage 2, and nobody is dragged (SURVIVAL) — supersedes BL3R4

| prediction | stands on | READING |
|---|---|---|
| As BL3R4 predicted and read, with the head `… drag=0.330 duration=20 visual=void_implosion victims=0 [] spared=0 []`: one GROWTH `from=1 to=2 … d=3.000 max=100`, `BODY tag=bl3b … stage=2`, `BODY tag=bl3x none within 16 blocks`, `ENTITIES CREEPER count=1`, two SWIRL `radius=2.000`. **No DRAG line and no DRAGTICK line** (no player ever inside the reach), no HURT, no SWELL | BL3R4's reading; K8 | **AS PREDICTED, 2026-11-18.** `BLAST … stage=1 gs=100 base=60.000 reach=2.00 pull=0.00 lift=0.00 drag=0.330 duration=20 visual=void_implosion victims=0 [] spared=0 []`; `GROWTH … from=1 to=2 … d=3.000 max=100`; `BODY tag=bl3b … stage=2 scale=0.882 … maxHP=100.000`; `BODY tag=bl3x none within 16 blocks`; `ENTITIES CREEPER count=1`; no DRAG, no DRAGTICK, no HURT, no SWELL |

### BL4R5 — a husk and a stage 5 in a STAGE-5 blast's reach are neither hurt NOR DRAGGED; a control proves the probe sees a velocity (SURVIVAL) — supersedes BL4R4

| prediction | stands on | READING |
|---|---|---|
| As BL4R4 predicted and read, with the head `drag=0.330`: the control `MOTION tag=bl4c … vz=0.5000`; `spared=2 [husk <bl4h> d=2.000 factor=0.555556, creeper/phage <bl4p> d=2.000 factor=0.555556]` (either order); all six later MOTION lines `vx=0.0000 vy=0.0000 vz=0.0000`; both PROBE lines unchanged; **no DRAG line and no DRAGTICK line** (the player at 7.280 is outside); `ENTITIES CREEPER count=1`; no HURT | BL4R4's reading; the predicate in `MobBlaster` | **AS PREDICTED, 2026-11-18.** The control `MOTION tag=bl4c … vx=0.0000 vy=0.0000 vz=0.5000`; `BLAST … drag=0.330 … victims=0 [] spared=2 [husk e9fe1b3c-6d81-4533-b903-d50b29177d42 d=2.000 factor=0.555556, creeper/phage 8e2392af-7c5b-4139-9a93-5f571b948577 d=2.000 factor=0.555556]`; all six later MOTION lines `vx=0.0000 vy=0.0000 vz=0.0000`; both PROBE lines unchanged (`customHP=100.000/100.000`, `160.000/160.000`, `d=7.280`); no DRAG, no DRAGTICK, no HURT; `ENTITIES CREEPER count=1` |

### BL6R5 — THE STAGE-1 EDGE, inside (1.95 of 2): a sliver of 60, and the drag brings him in (SURVIVAL) — supersedes BL6R4

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> <y0> <z0> onGround=true fall=0.000`. (2) `DRIVE detonate tag=bl6 <uuid>`. (3) the entry `d=1.950 factor=0.012812 dealt=0.769 knock=0.025000 res=0.000 h=1.950 ground=true tf=2.202643 strength=0.000000 cap=0.658300 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false`, **the factor from the game's distance `1.9500000000000028`** (BL6R4's reading). (4) Two SWIRL `radius=2.000`. (5) HURT `before=0.769`. (6) **Exactly one** `DRAG … start=1.950 end=<E> ticks=20 sent=20 outcome=held …`, **E in [0.79, 1.55]** (model 0.888 to 1.445: at the reach's edge his speed at tick 20 is high and the lag shows). (7) Forty-five ticks later `SELF … at=<x1> <y0> <z0>`, **x1 − x0 in [0.98, 1.25]** (model 1.080 to 1.147) | K3-K8 | **AS PREDICTED, 2026-11-18.** `SELF … at=-107.50 63.00 7.50`; the entry `d=1.950 factor=0.012812 dealt=0.769 knock=0.025000 … cap=0.658300 …`, the game's distance's rounding; `HURT before=0.769 after=0.699`; `DRAG … start=1.950 end=0.894 ticks=20 sent=20 outcome=held movesMin=0 movesMax=1`: in [0.79, 1.55] (model at lag 0: 0.888); `SELF … at=-106.36 63.00 7.50 onGround=true fall=0.000`: x1 − x0 = 1.14, in [0.98, 1.25] |

### BL7R5 — THE STAGE-1 EDGE, outside (2.05 of 2): neither hit nor dragged (SURVIVAL) — supersedes BL7R4

| prediction | stands on | READING |
|---|---|---|
| As BL7R4 predicted and read, the head `drag=0.330 … victims=0 [] spared=0 []`: two SWIRL `radius=2.000`; **no HURT, no DRAG, no DRAGTICK**; `SELF … at=<x0> <y0> <z0>` again, the same to the printed place. BL6R5 is the control | the stage-1 reach | **AS PREDICTED, 2026-11-18.** `SELF … at=-107.50 63.00 7.50`; `BLAST … drag=0.330 … victims=0 [] spared=0 []`; no HURT, no DRAG, no DRAGTICK; `SELF … at=-107.50 63.00 7.50` again |

### BL9R5 — OUTCOME a AT 3: a STAGE-5 Phage at half its reach, and standing still he is dragged in (SURVIVAL) — supersedes BL9R4

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> …`. (2) `DRIVE detonate tag=bl9 <uuid>`. (3) the entry at `d=3.000` as BL9R4 read it (`factor=0.375000 dealt=56.250 knock=0.500000 … cap=1.135000 …`). (4) Two SWIRL `radius=6.000`. (5) HURT `before=56.250`. (6) **Exactly one** `DRAG … start=3.000 end=<E> ticks=20 sent=20 outcome=held …`, **E in [0.50, 0.98]** (model 0.602 to 0.878). (7) Forty-five ticks later `SELF … at=<x1> …`, **x1 − x0 in [2.10, 2.72]** (model 2.204 to 2.621); **within the allowance (lag 0 to 2) at most 2.374, he rests no nearer than 0.626 to the Phage's feet**; at lag 3 or 4 the model rests him 0.515 or 0.379 from them, which the DRAGTICK pings would then explain | K3-K8; `theOutcomeFiguresAsExecuted` | **AS PREDICTED, 2026-11-18.** `SELF … at=-107.50 63.00 7.50`; the entry at `d=3.000 factor=0.375000 dealt=56.250 knock=0.500000 … cap=1.135000 …`; `HURT before=56.250 after=51.136`; `DRAG … start=3.000 end=0.875 ticks=20 sent=20 outcome=held movesMin=0 movesMax=1`: in [0.50, 0.98] (model at lag 0: 0.878); `SELF … at=-105.29 63.00 7.50 onGround=true fall=0.000`: x1 − x0 = 2.21, in [2.10, 2.72] and under 2.374: **0.79 from the Phage's feet** (model at lag 0: 0.796) |

### SD5 — OUTCOME a AT 5: a STAGE-5 Phage five blocks east; standing still he is dragged in most of the way (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> …`. (2) `DRIVE detonate tag=sd5 <uuid>`. (3) **Exactly one** BLAST `stage=5 gs=100 base=150.000 reach=6.00 pull=0.00 lift=0.00 drag=0.330 duration=20 … victims=1 [player <Ben> d=5.000 factor=0.097222 dealt=14.583 knock=0.166667 res=0.000 h=5.000 ground=true tf=2.202643 strength=0.000000 cap=2.043000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []` (EXECUTED, scratch `e.jsh`: share (x² + x) / 2 with x = 1/6, the cap (5 − 0.5) / 2.202643, printed in the entry's own formats). (4) Two SWIRL `radius=6.000`. (5) HURT `before=14.583`. (6) **Exactly one** `DRAG … start=5.000 end=<E> ticks=20 sent=20 outcome=held …`, **E in [1.56, 2.90]** (model 1.658 to 2.795: he is still moving at tick 20, so the server's view at that tick is spread by the lag). (7) Forty-five ticks later `SELF … at=<x1> …`, **x1 − x0 in [3.50, 3.83]** (model 3.603 to 3.733: where he RESTS is barely moved by the lag) | K3-K8; `theOutcomeFiguresAsExecuted` | **AS PREDICTED, 2026-11-18.** `SELF … at=-107.50 63.00 7.50`; `BLAST … [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 d=5.000 factor=0.097222 dealt=14.583 knock=0.166667 res=0.000 h=5.000 ground=true tf=2.202643 strength=0.000000 cap=2.043000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []`, character for character; `HURT before=14.583 after=13.258`; `DRAG … start=5.000 end=1.783 ticks=20 sent=20 outcome=held movesMin=0 movesMax=2`: in [1.56, 2.90]; `SELF … at=-103.89 63.00 7.50 onGround=true fall=0.000`: x1 − x0 = 3.61, in [3.50, 3.83] (model at lag 0: 3.603) |

### CS1 — THE CONSISTENCY ROW: the still staging from 4 at a stage 5, FIVE TIMES (SURVIVAL) — new

**This is the point of the gate.** On 2026-11-18 the same staging read `end=0.491` and `end=1.347` in two runs of one
build. Here it is run five times in one row, the instrument on throughout.

| prediction | stands on | READING |
|---|---|---|
| For each of `cs1a` to `cs1e`, in order: a `SELF … at=<x0>` before; one BLAST `stage=5 gs=100 base=150.000 reach=6.00 … drag=0.330 … [player <Ben> d=4.000 factor=0.222222 dealt=33.333 knock=0.333333 res=0.000 h=4.000 ground=true tf=2.202643 strength=0.000000 cap=1.589000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []` (BQ2's reading at 4.000); one HURT `before=33.333`; two SWIRL `radius=6.000`; 21 DRAGTICK lines; one `DRAG … start=4.000 end=<E> ticks=20 sent=20 outcome=held movesMin=<a> movesMax=<b>`; a `SELF … at=<x1>` 45 ticks later. **EACH END:** E in **[0.77, 1.16]** (model 0.868 to 1.056) and x1 − x0 in **[2.97, 3.71]** (model 3.065 to 3.611). **THE SPREAD BETWEEN THE FIVE:** the largest E less the smallest **≤ 0.29** (the model's 0.188 over lag 0 to 4, plus 0.10); the largest x1 − x0 less the smallest **≤ 0.65** (the model's 0.546, plus 0.10). **The moves counts and pings are RECORDED, not judged:** if they show batching (a 0 beside a 2) and the five ends still agree within these bounds, that is the proof the drag no longer depends on packet timing. **Against #205:** its two readings of this staging, 0.491 and 1.347, differ by 0.856, three times this bound | K1-K8; `theOutcomeFiguresAsExecuted` | **AS PREDICTED, 2026-11-18: THE FIVE AGREE EXACTLY.** Each of the five: `SELF … at=-107.50 63.00 7.50` before; the entry at `d=4.000 factor=0.222222 dealt=33.333 knock=0.333333 … cap=1.589000 …`; `HURT before=33.333 after=30.303 defense=10.000`; 21 DRAGTICK lines; `DRAG … start=4.000 end=1.056 ticks=20 sent=20 outcome=held movesMin=0 movesMax=1`; `SELF … at=-104.43 63.00 7.50 onGround=true fall=0.000` after. **Ends: 1.056, 1.056, 1.056, 1.056, 1.056, spread 0.000** (bound 0.29); **feet travel: 3.07 five times, spread 0.00** (bound 0.65). The model at lag 0 is 1.056 and 3.065. **Moves, RECORDED:** in every one of the five, `moves=-` at t=0, `moves=0` at t=1, `moves=1` at t=2 to 20 (min 0, max 1); **ping 0 at every tick of all five.** The whole first sequence is quoted under THE BOOT. Against #205's 0.491 and 1.347 on this staging |

### RB1 — NO RUBBER-BAND: the server's copy of him is never snapped, and no movement complaint (SURVIVAL) — new, read across the run

**Whatever B2 found, as a row:** the route sends no position packet, so the server never awaits a confirm and never
snaps him; his moves are the only thing that moves the server's copy, as when he walks.

| prediction | stands on | READING |
|---|---|---|
| (1) **No line containing `moved wrongly` or `moved too quickly`** anywhere between the run's START and FINISHED. (2) **In each of CS1's five, his feet 45 ticks after the blast lie past the last DRAGTICK's `at=` by the slide still to come, toward the Phage:** x1 − (tick 20's x) in **[0.02, 0.58]** (the model's rest less its end, 0.121 to 0.479 over lag 0 to 4, ± 0.10); never back toward where he started. (3) Every SELF line in the run `onGround=true fall=0.000`. (4) BQ3's fifth question: Ben's feel | B2; B4; K1 | **AS PREDICTED, 2026-11-18.** (1) No `moved wrongly` or `moved too quickly` line anywhere in the log. (2) In all five of CS1, tick 20's `at=-104.556` and the feet 45 ticks after the blast `at=-104.43`: x1 − (tick 20's x) = +0.126, toward the Phage, in [0.02, 0.58] (model at lag 0: 0.121). (3) Every SELF line in the run `onGround=true fall=0.000`. (4) BQ3's fifth answer: "no" |

---

## THE IMPLOSION'S OWN ROWS, RE-PREDICTED

### IM1R3 — THE STAGE-5 EDGE, inside (5.90 of 6): a sliver of 150, and a drag faded almost to nothing (SURVIVAL) — supersedes IM1R2

| prediction | stands on | READING |
|---|---|---|
| (1)-(5) as IM1R2 predicted and read them (the entry at `d=5.900 factor=0.008472 dealt=1.271 knock=0.016667 … cap=2.451600 …`, HURT `before=1.271`), the head `drag=0.330`. (6) **Exactly one** `DRAG … start=5.900 end=<E> ticks=20 sent=20 outcome=pulled …`, **E in [5.25, 5.78]** (model 5.355 to 5.680). (7) Forty-five ticks later **x1 − x0 in [0.31, 0.80]** (model 0.413 to 0.695) | K3-K8 | **AS PREDICTED, 2026-11-18.** The entry at `d=5.900 factor=0.008472 dealt=1.271 knock=0.016667 … cap=2.451600 …`; `HURT before=1.271 after=1.155`; `DRAG … start=5.900 end=5.360 ticks=20 sent=20 outcome=pulled movesMin=0 movesMax=1`: in [5.25, 5.78] (model at lag 0: 5.355); `SELF … at=-106.81`: x1 − x0 = 0.69, in [0.31, 0.80] |

### IM2R3 — THE STAGE-5 EDGE, outside (6.05 of 6): neither hit nor dragged (SURVIVAL) — supersedes IM2R2

| prediction | stands on | READING |
|---|---|---|
| As IM2R2 predicted and read, the head `drag=0.330 … victims=0 [] spared=0 []`: **no HURT, no DRAG, no DRAGTICK**; `SELF … at=<x0> …` again, the same to the printed place. IM1R3 is the control | the stage-5 reach | **AS PREDICTED, 2026-11-18.** `BLAST … stage=5 … drag=0.330 … victims=0 [] spared=0 []`; no HURT, no DRAG, no DRAGTICK; `SELF … at=-107.50 63.00 7.50` before and after |

### IM3R3 — OUTCOME a AT 2, AND d: dragged in, never past the margin (SURVIVAL) — supersedes IM3R2

| prediction | stands on | READING |
|---|---|---|
| (1)-(4) as IM3R2 predicted and read them (the entry at `d=2.000 factor=0.555556 dealt=83.333 knock=0.666667 … cap=0.681000 …`, HURT `before=83.333`), the head `drag=0.330`. (5) **Exactly one** `DRAG … start=2.000 end=<E> ticks=20 sent=20 outcome=held …`, **E in [0.46, 0.83]** (model 0.561 to 0.727). (6) Forty-five ticks later **x1 − x0 in [1.22, 1.67]** (model 1.321 to 1.571); **within the allowance at most 1.422, no nearer than 0.578 to the Phage's feet** | K3-K8; `dReachingTheMarginHeIsNeverCarriedThrough` | **AS PREDICTED, 2026-11-18.** The entry at `d=2.000 factor=0.555556 dealt=83.333 knock=0.666667 … cap=0.681000 …`; `HURT before=83.333 after=75.758`; `DRAG … start=2.000 end=0.725 ticks=20 sent=20 outcome=held movesMin=0 movesMax=1`: in [0.46, 0.83]; `SELF … at=-106.18`: x1 − x0 = 1.32, in [1.22, 1.67] and under 1.422: **0.68 from the Phage's feet** (model at lag 0: 0.679) |

### IM4R3 — OUTCOME d INSIDE THE MARGIN: nothing is sent and he is not moved; IM4R2's miss closed (SURVIVAL) — supersedes IM4R2

| prediction | stands on | READING |
|---|---|---|
| (1)-(4) as IM4R2 predicted and read them (xa within [x0 − 0.20, x0]; the entry `d=<D>`, D in [0.300, 0.500), `cap=0.000000`; one HURT `before=<dealt>`), the head `drag=0.330`. (5) **Exactly one** `DRAG … start=<D> end=<E> ticks=20 sent=0 outcome=held …`, **E within 0.10 of D**, and **every one of its 20 pulled DRAGTICK lines `add=- cap=0.000000`**. **This is the clause IM4R2 missed** (`sent=18`): the old cap braked his outward drift out of the overlap; the add never reads his drift, so inside the margin it is zero whatever he does (`dInsideTheMarginNothingIsEverSentEvenToOneDriftingOut`). (6) Forty-five ticks later **xb = xa ± 0.10** | K2, K5; IM4R2's reading | **NOT AS PREDICTED ON ONE CLAUSE, 2026-11-18: (5)'s `sent=0` and "every DRAGTICK `add=- cap=0.000000`"; it read `sent=20`.** Read in full: `DRAG source=phage id=15981eb4-… victim=player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 start=0.487 end=0.547 ticks=20 sent=20 outcome=held movesMin=0 movesMax=1`. **What the row was built to show held:** E = 0.547, within 0.10 of D; his feet `SELF … at=-107.69` before and `at=-107.75` after, xb − xa = −0.06, inside ± 0.10: he was not moved by the drag. Every other clause as predicted: the entry `d=0.487 factor=0.664144 dealt=39.849 knock=0.756299 … cap=0.000000 …`; `HURT before=39.849 after=36.226`. **The cause (F-I below): the prediction was wrong, the code did what it says.** His client pushed him out of the overlap during the drag's first ticks, to 0.547, just past the margin, so the hold gave adds of 0.000869 to 0.001916 a tick, each under the client's own 0.003 speed cut, and none moved him |

### IM5R3 — THE STAGE-3 EDGE, inside (3.95 of 4) (SURVIVAL) — supersedes IM5R2

| prediction | stands on | READING |
|---|---|---|
| (0)-(5) as IM5R2 predicted and read them (`REPLY "Spawned Phage (creeper, stage 3, 120 HP, GS 100 given)"`; the entry at `d=3.950 factor=0.006328 dealt=0.664 …`; two SWIRL `radius=4.000`; HURT `before=0.664`), the head `drag=0.330`. (6) **Exactly one** `DRAG … start=3.950 end=<E> ticks=20 sent=20 outcome=pulled …`, **E in [3.20, 3.86]** (model 3.296 to 3.764). (7) Forty-five ticks later **x1 − x0 in [0.29, 0.99]** (model 0.386 to 0.890) | K3-K8 | **AS PREDICTED, 2026-11-18.** `REPLY "Spawned Phage (creeper, stage 3, 120 HP, GS 100 given)"`; the entry at `d=3.950 factor=0.006328 dealt=0.664 …`; `HURT before=0.664 after=0.604`; `DRAG … start=3.950 end=3.307 ticks=20 sent=20 outcome=pulled movesMin=0 movesMax=1`: in [3.20, 3.86]; `SELF … at=-106.62`: x1 − x0 = 0.88, in [0.29, 0.99] |

### IM6R3 — THE STAGE-3 EDGE, outside (4.05 of 4) (SURVIVAL) — supersedes IM6R2

| prediction | stands on | READING |
|---|---|---|
| As IM6R2 predicted and read, the head `drag=0.330 … victims=0 [] spared=0 []`: **no HURT, no DRAG, no DRAGTICK**; his feet unchanged to the printed place. IM5R3 is the control | the stage-3 reach | **AS PREDICTED, 2026-11-18.** `BLAST … stage=3 … drag=0.330 … victims=0 [] spared=0 []`; no HURT, no DRAG, no DRAGTICK; his feet `at=-107.50 63.00 7.50` before and after |

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
| (a)-(m), with: in `d=0.950`, out `d=2.400`; the entry at `d=0.950 factor=0.400312 dealt=24.019 knock=0.525000 … cap=0.204300 …`, **the factor from the game's `0.9500000000000028`** (FZ1R2's reading); SWIRL `radius=2.000`; DRAG `start=0.950 end=<E> ticks=20 sent=20 outcome=held …`, **E in [0.42, 0.67]** (model 0.518 to 0.568); **T in [0.29, 0.57]** (model 0.393 to 0.468) | K3-K8; FZ1R2's reading | **AS PREDICTED, 2026-11-18.** `TARGET … target=self`; `FUSE … fuse=0/30 dir=-1`; `SWELL … reason=start … d=0.950`; `fuse=9/30 dir=+1`; `SWELL … reason=reset … d=2.400 … swell=10`; `fuse=0/30 dir=-1`; a second START and `FUSED` 30 ticks after it; the entry at `d=0.950 factor=0.400312 dealt=24.019 knock=0.525000 … cap=0.204300 …`; `HURT before=24.019 after=21.835`; `ENTITIES CREEPER count=0`; no CREEPERGUARD; `DRAG … start=0.950 end=0.567 ticks=20 sent=20 outcome=held movesMin=0 movesMax=1`: in [0.42, 0.67]; `SELF … at=-107.40` then `at=-107.01`: T = 0.39, in [0.29, 0.57] |

### FZ3R3 — stage 3 at GS 60: lights inside 2.000, resets beyond 4.667, drags him in (SURVIVAL) — supersedes FZ3R2

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 3, 72 HP, GS 60 given)" [aqua, gray]`. (a)-(m), with: in `d=1.950`, out `d=4.750`; the entry at `d=1.950 factor=0.387578 dealt=24.417 knock=0.512500 … cap=0.658300 …`; SWIRL `radius=4.000`; DRAG `start=1.950 end=<E> ticks=20 sent=20 outcome=held …`, **E in [0.46, 0.82]** (model 0.559 to 0.719); **T in [1.18, 1.62]** (model 1.277 to 1.518) | as FZ1R3 | **AS PREDICTED, 2026-11-18.** The fuse clauses as FZ1R3's, at `d=1.950`, reset at `d=4.750`; the entry at `d=1.950 factor=0.387578 dealt=24.417 knock=0.512500 … cap=0.658300 …`; `HURT before=24.417 after=22.198`; `DRAG … start=1.950 end=0.717 ticks=20 sent=20 outcome=held movesMin=0 movesMax=1`: in [0.46, 0.82]; `SELF … at=-107.40` then `at=-106.12`: T = 1.28, in [1.18, 1.62] |

### FZ5R3 — stage 5 at GS 60: lights inside 3.000, resets beyond 7.000, drags him in (SURVIVAL) — supersedes FZ5R2

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`. (a)-(m), with: in `d=2.950`, out `d=7.050`; the entry at `d=2.950 factor=0.383368 dealt=34.503 knock=0.508333 … cap=1.112300 …`; SWIRL `radius=6.000`; DRAG `start=2.950 end=<E> ticks=20 sent=20 outcome=held …`, **E in [0.50, 0.97]** (model 0.600 to 0.870); **T in [2.06, 2.67]** (model 2.160 to 2.569) | as FZ1R3 | **AS PREDICTED, 2026-11-18.** The fuse clauses at `d=2.950`, reset at `d=7.050`; the entry at `d=2.950 factor=0.383368 dealt=34.503 knock=0.508333 … cap=1.112300 …`; `HURT before=34.503 after=31.366`; `DRAG … start=2.950 end=0.867 ticks=20 sent=20 outcome=held movesMin=0 movesMax=1`: in [0.50, 0.97]; `SELF … at=-107.40` then `at=-105.24`: T = 2.16, in [2.06, 2.67] |

---

## THE SPLIT ROW WITH TWO BLASTS

### DS1R7 — split at GS 60; thawed; each child walks in, lights and blows, and each drags him toward its own feet (SURVIVAL) — supersedes DS1R6

| prediction | stands on | READING |
|---|---|---|
| (1)-(7), (9), (10) as DS1R6 predicted them, each head `drag=0.330`: within the 230 ticks one BLAST for each child, X then Y, X's `spared=<n>` naming Y exactly when Y stood within 2 of X, and then one GROWTH printed with the identical `d`; each victim entry `ground=true`, `h = d`; D1 < 1.000, D2 ≤ Y's cancel; exactly two HURT, X then Y; `ENTITIES CREEPER count=0`; one SPLIT; no CREEPERGUARD. (8) **Exactly two DRAG lines on him**, `id=<X>` then `id=<Y>`, each `ticks=<n>` in [1, 20], and **each `end` and `outcome` RECORDED, not judged:** two origins under a block apart pull toward their own feet, each add cut only by its own margin, and that case is **UNRULED** (DS1R6 read X's end at 0.138) | DS1R6's reading; K2 | **AS PREDICTED, 2026-11-18.** `DRIVE attack … d=3.000 charge=1.000`; two `CHILD … frozen=true`; `SPLIT … stage=5 gs=60 children=2 childStage=1`; `ENTITIES CREEPER count=2`; two stage-1 BODY lines `maxHP=48.000 gs=60`; two `DRIVE thaw … ai=true`; X (63c2435b): `SWELL … d=0.728`, `FUSED`, `BLAST … stage=1 gs=60 base=36.000 reach=2.00 … [player … d=0.858 factor=0.448561 dealt=16.148 …] spared=1 [creeper/phage 14d5f64e-c20a-4a73-b5a6-d4d117458a99 d=0.563 factor=0.617617]`, then `GROWTH … from=1 to=2 … d=0.563`, printed identically; Y: `SWELL … d=0.812`, `FUSED` 2 ticks after X's, `BLAST … stage=2 gs=60 base=49.500 reach=3.00 … [player … d=0.579 factor=0.728981 dealt=36.085 …] spared=0 []`; D1 = 0.858 < 1.000, D2 = 0.579; `HURT before=16.148 after=14.680` then `HURT before=36.085 after=32.804`; two DRAG lines, X then Y, RECORDED: `start=0.858 end=0.552 ticks=20 sent=20 outcome=held movesMin=1 movesMax=1` and `start=0.579 end=0.402 ticks=20 sent=4 outcome=held movesMin=0 movesMax=1` (the two-origin case, UNRULED: X's drag carried him 0.402 from Y's feet); `ENTITIES CREEPER count=0`; one SPLIT |

---

## THE DRAG'S OWN ROWS

### DR1R2 — HE LEAVES THE REACH MID-DRAG: a step moves him out, and the drag stops adding (SURVIVAL) — supersedes DR1

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> …`. (2) `DRIVE detonate tag=dr1 <uuid>`. (3) the entry at `d=3.000` as DR1 read it, the head `drag=0.330`. (4) HURT `before=56.250`. (5) Two ticks after the step, `SELF … at=<x1> …`, **x1 − x0 in [−5.20, −4.85]** (the step's −6 and the drag's ticks east before it: model −5.068 to −4.957, lag 0 to 4). (6) **Exactly one** `DRAG … start=3.000 end=<E> ticks=<n> sent=<n> outcome=left …`, **n in [4, 7]**, **E > 6.000**; after the step every DRAGTICK reads `add=- cap=-`. (7) Forty ticks later `SELF … at=<x2> …`, **x2 − x1 in [−0.05, 0.17]** (the model's slide still to come, 0.057 to 0.073: DR1 read **0.39**, the old send's carried momentum, F-G; the add carries far less) | K2, K4; DR1's reading | **AS PREDICTED, 2026-11-18.** `SELF … at=-107.50`; the entry at `d=3.000`; `HURT before=56.250 after=51.136`; `SELF … at=-112.46` two ticks after the step: x1 − x0 = −4.96, in [−5.20, −4.85]; `DRAG … start=3.000 end=7.857 ticks=6 sent=6 outcome=left movesMin=0 movesMax=1`: n = 6 in [4, 7], E > 6.000; `SELF … at=-112.36` forty ticks later: x2 − x1 = +0.10, in [−0.05, 0.17], against DR1's 0.39 |

### DR2R2 — TWO BLASTS FIVE TICKS APART: each drags toward its own origin (SURVIVAL) — supersedes DR2

| prediction | stands on | READING |
|---|---|---|
| (0) Two `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`. (1) `SELF … at=<x0> …`. (2) `DRIVE detonate tag=dr2a <uuid>`, then five ticks later `DRIVE detonate tag=dr2b <uuid>`. (3) A's BLAST, head `drag=0.330`, `[player <Ben> d=3.000 factor=0.375000 dealt=33.750 knock=0.500000 res=0.000 h=3.000 ground=true tf=2.202643 strength=0.000000 cap=1.135000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=1 [creeper/phage <dr2b> d=6.000 factor=0.000000]`: **B stands at A's inclusive reach and is a spared entry** (DR2's reading, F-H). (4) B's BLAST `[player <Ben> d=<DB> …] spared=0 []`, **DB in [2.90, 3.71]** (model 3.000 to 3.609), its fields by the shape from DB. (5) Exactly two HURT, `before=33.750`, then `before=<B's dealt>`. (6) **Exactly two DRAG lines**: A's `start=3.000 end=<EA> ticks=20 sent=20 outcome=held …`, **EA in [2.57, 3.10]** (model 2.669 to 3.002); then B's `start=<DB> end=<EB> ticks=20 sent=20 outcome=held …`, **EB in [2.30, 2.93]** (model 2.399 to 2.825). (7) Four SWIRL lines `radius=6.000`. (8) Fifty ticks after B, **x1 − x0 in [−1.49, −0.88]**: **B, nearer and later, carries him WEST**, past where he started (model −1.390 to −0.984). The two-origin case stays UNRULED | K2-K6; `twoDragsOnOnePlayerInOneTickEachSendItsOwnAdd` | **AS PREDICTED, 2026-11-18.** A's `BLAST … [player … d=3.000 factor=0.375000 dealt=33.750 knock=0.500000 … cap=1.135000 …] spared=1 [creeper/phage f837f6e8-8322-48e4-a7cc-de6f26ebde0b d=6.000 factor=0.000000]`: B at A's inclusive reach, spared; B's `BLAST … [player … d=3.609 factor=0.278616 dealt=25.075 knock=0.398461 … cap=1.411593 …] spared=0 []`: DB = 3.609 in [2.90, 3.71] (model at lag 0: 3.609); `HURT before=33.750` then `HURT before=25.075`; A's `DRAG … start=3.000 end=2.959 ticks=20 sent=20 outcome=held movesMin=0 movesMax=1`, in [2.57, 3.10]; B's `DRAG … start=3.609 end=2.399 ticks=20 sent=20 outcome=held movesMin=1 movesMax=1`, in [2.30, 2.93]; `SELF … at=-108.48` fifty ticks after B: x1 − x0 = −0.98, in [−1.49, −0.88] (model at lag 0: −0.984): B carried him west |

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
| Recorded beside the answers, in order: `TELL QUESTION 1 …`; BLAST `… [player <Ben> d=4.000 … dealt=33.333 …]`; HURT `before=33.333`; `DRAG … start=4.000 end=<E> ticks=20 sent=20 outcome=held …`, E in [0.77, 1.16] (as CS1). `TELL QUESTION 2 …`; the same lines, the same bound. `TELL QUESTION 3 …`, `TELL STOP. Let go of W.`; BLAST `… [player <Ben> d=<D3> …]`, **D3 in [3.00, 3.25]**; `DRAG … start=<D3> end=<E3> …`, **E3 < D3 is outcome b's server half** (model from 3.2: 2.932 to 2.950); DRAGTICK `keys=forward` while he holds W (the key, not the direction: he faces west, away from the Phage; INFERRED from `Input#isForward`'s name, so a reading of anything else is RECORDED). `TELL QUESTION 4 …`, `TELL STOP. Let go of W and sprint.`; BLAST `d=<D4>`, **D4 in [4.50, 4.75]**; `DRAG … start=<D4> … ticks=<n> … outcome=left`, **n ≤ 16** (model 8 to 14), **outcome c's server half**, and his DRAGTICK keys include `sprint` | **AS PREDICTED, 2026-11-18.** `TELL QUESTION 1 …`; `BLAST … [player … d=4.000 factor=0.222222 dealt=33.333 …]`; `HURT before=33.333 after=30.303`; `DRAG … start=4.000 end=1.056 ticks=20 sent=20 outcome=held movesMin=0 movesMax=1`, in [0.77, 1.16]; `SELF … at=-104.43`. `TELL QUESTION 2 …`; the same lines, `DRAG … start=4.000 end=1.056 ticks=20 sent=20 outcome=held movesMin=1 movesMax=1`. `TELL QUESTION 3 …`, `TELL STOP. Let go of W.`; `BLAST … [player … d=3.200 factor=0.342222 dealt=51.333 …]`: D3 = 3.200 in [3.00, 3.25]; `HURT before=51.333 after=46.667`; `DRAG … start=3.200 end=2.991 ticks=20 sent=20 outcome=held movesMin=1 movesMax=1`: E3 < D3 (the model said 2.932 to 2.950: he ended 0.04 farther out than it, still nearer than he started); every one of its 21 DRAGTICK lines `keys=forward`. `TELL QUESTION 4 …`, `TELL STOP. Let go of W and sprint.`; `BLAST … [player … d=4.700 factor=0.131806 dealt=19.771 …]`: D4 = 4.700 in [4.50, 4.75]; `HURT before=19.771 after=17.973`; `DRAG … start=4.700 end=9.235 ticks=8 sent=8 outcome=left movesMin=1 movesMax=1`: n = 8 ≤ 16, `left`; every one of its 21 DRAGTICK lines `keys=forward,sprint` |
| `ASK 1 question="Standing still, did it drag you in toward where it stood, for about a second?" answer="<his line>" ticks=<t>` | `ASK 1 question="Standing still, did it drag you in toward where it stood, for about a second?" answer="yes" ticks=287` |
| `ASK 2 question="Did that drag feel the same as the first one?" answer="<his line>" ticks=<t>` | `ASK 2 question="Did that drag feel the same as the first one?" answer="not sure" ticks=401`. **NOT STATED** (his line, verbatim). Both drags read identically on the server: `start=4.000 end=1.056` each |
| `ASK 3 question="Walking away, did the drag slow you down without stopping you, so you still went back toward it?" answer="<his line>" ticks=<t>` | `ASK 3 question="Walking away, did the drag slow you down without stopping you, so you still went back toward it?" answer="it basically stopped me which is what I want for walking" ticks=298`. His line, verbatim: **not the question's yes**; he says the drag stopped his walk, and that this is what he wants. The server half: `start=3.200 end=2.991`, 0.209 back toward it, DRAGTICK `keys=forward` all 21 ticks |
| `ASK 4 question="Sprinting away from the edge, did you get out of the drag?" answer="<his line>" ticks=<t>` | `ASK 4 question="Sprinting away from the edge, did you get out of the drag?" answer="yes" ticks=71` |
| `ASK 5 question="During any of those drags, did you feel a stutter, or get snapped back to where you were?" answer="<his line>" ticks=<t>` | `ASK 5 question="During any of those drags, did you feel a stutter, or get snapped back to where you were?" answer="no" ticks=79`. **The client half of RB1, and of B4's invisible, silent packet: read by his own feel** |
| `ASK 6 question="Does the pull feel right now?" answer="<his line>" ticks=<t>` | `ASK 6 question="Does the pull feel right now?" answer="feels good" ticks=281` |

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

## THE BOOT — WHAT WAS READ

**Dated 2026-11-18. Written after the clean stop; no prediction above was edited.** Lines are quoted from
`run/logs/latest.log`; timestamps, the thread and `[Rpg]` dropped where the gate's own lines omit them.

- **The boot.** `5ee0587a`, booted 13:28 by `./scripts/dev-server.sh --refresh-content` on the seat's approval and Ben's
  "ready", after the carried state: no JVM (`Get-Process java` printed nothing); backup
  `rpg-run-backups/2026-11-18-pre-drag-additive` (no earlier folder of that name today; `run/plugins/Rpg` 130 files
  404498 bytes, `run/world` 193 files 204976286 bytes, `diff -rq lines: 0` each); `12:  dev-pack-on-join: true`. R0a-R0e
  PASS, the self-check line in full `[Rpg] Phage swell packet: index=16 type=int typeId=1 server=V_26_1_2
  readFor=V_26_1`. Ben was told R0 passed before he logged in (13:34:18, `BaronVonYeetus[/192.168.68.90:59827]`).
- **DEPARTURE, harmless:** he did not type `/gamemode survival`; his first command was `/rpg selftest drag-additive`
  (13:34:24). The run's own lines record survival (`RESTORED … gamemode=SURVIVAL`, and the selftest refuses outside it).
- **ONE run, 13:34:24 to 13:37:41**, then `/stop` (13:37:44). No second run. Its own lines: `setup DIFFICULTY
  found=NORMAL set=NORMAL`; `SELFTEST RUN RESTORED xp=712667 cell=mage/fire gamemode=SURVIVAL held=6 mana=100.0
  trace=false spawn_mobs=false time=6000 blocks=22617 mobsRemoved=0 orbsRemoved=0 keep_inventory=false
  immediate_respawn=false difficulty=NORMAL drag_tick=false`. Every LEFTOVER `living=0 radius=16` and every LINGER
  `work=0 entities=0 radius=48`. **26 HURT lines, one per blast that reached him, no second; 31 BLAST lines, 62 SWIRL;
  546 DRAGTICK lines (26 drags on him × 21); every SELF line `onGround=true fall=0.000`; no CREEPERGUARD line; no `moved
  wrongly` or `moved too quickly` line; every DRAGTICK `ping=0`.**
- **The stop was clean:** `Stopping server`, `All dimensions are saved`, `All RegionFile I/O tasks to complete`; no
  java.exe left.

### CS1's FIRST DRAGTICK SEQUENCE, IN FULL (all five read the same moves range, 0 to 1; the first is quoted)

```
DRAGTICK drag=02986175-… t=0 victim=<Ben> at=-107.500 63.000 7.500 d=4.000 moves=- add=- cap=- ping=0 keys=-
DRAGTICK … t=1 at=-107.500 63.000 7.500 d=4.000 moves=0 add=0.110000 0.000000 cap=0.142329 ping=0 keys=-
DRAGTICK … t=2 at=-107.390 63.000 7.500 d=3.890 moves=1 add=0.116050 0.000000 cap=0.137856 ping=0 keys=-
DRAGTICK … t=3 at=-107.214 63.000 7.500 d=3.714 moves=1 add=0.125736 0.000000 cap=0.130694 ping=0 keys=-
DRAGTICK … t=4 at=-106.992 63.000 7.500 d=3.492 moves=1 add=0.121671 0.000000 cap=0.121671 ping=0 keys=-
DRAGTICK … t=5 at=-106.749 63.000 7.500 d=3.249 moves=1 add=0.111796 0.000000 cap=0.111796 ping=0 keys=-
DRAGTICK … t=6 at=-106.505 63.000 7.500 d=3.005 moves=1 add=0.101858 0.000000 cap=0.101858 ping=0 keys=-
DRAGTICK … t=7 at=-106.270 63.000 7.500 d=2.770 moves=1 add=0.092290 0.000000 cap=0.092290 ping=0 keys=-
DRAGTICK … t=8 at=-106.049 63.000 7.500 d=2.549 moves=1 add=0.083313 0.000000 cap=0.083313 ping=0 keys=-
DRAGTICK … t=9 at=-105.845 63.000 7.500 d=2.345 moves=1 add=0.075024 0.000000 cap=0.075024 ping=0 keys=-
DRAGTICK … t=10 at=-105.659 63.000 7.500 d=2.159 moves=1 add=0.067447 0.000000 cap=0.067447 ping=0 keys=-
DRAGTICK … t=11 at=-105.489 63.000 7.500 d=1.989 moves=1 add=0.060567 0.000000 cap=0.060567 ping=0 keys=-
DRAGTICK … t=12 at=-105.336 63.000 7.500 d=1.836 moves=1 add=0.054348 0.000000 cap=0.054348 ping=0 keys=-
DRAGTICK … t=13 at=-105.199 63.000 7.500 d=1.699 moves=1 add=0.048742 0.000000 cap=0.048742 ping=0 keys=-
DRAGTICK … t=14 at=-105.075 63.000 7.500 d=1.575 moves=1 add=0.043699 0.000000 cap=0.043699 ping=0 keys=-
DRAGTICK … t=15 at=-104.963 63.000 7.500 d=1.463 moves=1 add=0.039168 0.000000 cap=0.039168 ping=0 keys=-
DRAGTICK … t=16 at=-104.863 63.000 7.500 d=1.363 moves=1 add=0.035102 0.000000 cap=0.035102 ping=0 keys=-
DRAGTICK … t=17 at=-104.773 63.000 7.500 d=1.273 moves=1 add=0.031454 0.000000 cap=0.031454 ping=0 keys=-
DRAGTICK … t=18 at=-104.693 63.000 7.500 d=1.193 moves=1 add=0.028184 0.000000 cap=0.028184 ping=0 keys=-
DRAGTICK … t=19 at=-104.621 63.000 7.500 d=1.121 moves=1 add=0.025252 0.000000 cap=0.025252 ping=0 keys=-
DRAGTICK … t=20 at=-104.556 63.000 7.500 d=1.056 moves=1 add=0.022624 0.000000 cap=0.022624 ping=0 keys=-
```

**What it shows:** the strength binds for three ticks (0.110000, 0.116050, 0.125736, each under its cap), the hold
from t=4 (the add equals the cap). `moves=0` at t=1 alone: his first move after the blast had not yet arrived, so the
server read him at 4.000 again, and the add was the same as it would have been. **The run tracks the lag-0 model to the
printed place** (end 1.056; rest 0.935, his feet 0.93 from the Phage). This one LAN boot read a lag of 0, at ping 0; a
remote player's is not read here.

### F-I — IM4R3: PUSHED OUT OF THE OVERLAP, JUST PAST THE MARGIN (A PREDICTION MISS)

His DRAGTICK lines: `t=0 … d=0.487 … add=- cap=-`, `t=1 … d=0.521 moves=1 add=0.000869 0.000000 cap=0.000869`, `t=2 …
d=0.539 … add=0.001588`, `t=3 … d=0.547 … add=0.001916`, and from t=4 to t=20 `d=0.547 moves=0 add=0.001916`. His own
client pushed him out of the Phage's box (D3 of `GATE-implosion-drag.md`) past the margin at 0.5, so the hold gave him
`0.04067 × 0.047` = 0.0019 a tick, under the client's 0.003 cut (`LivingEntity#aiStep`, READ for this branch), which
zeroes it before he moves: `moves=0` from t=4, his feet −0.06. **The prediction assumed he stayed inside the margin;
the staging puts him 0.047 outside it.** Not a defect: the drag did not move him, as "held" says.

### BEN'S ANSWERS, VERBATIM, AND WHAT THE SERVER READ BESIDE EACH

| question | the server's line | his answer |
|---|---|---|
| 1 still, from 4 | `start=4.000 end=1.056 … movesMin=0 movesMax=1` | "yes" |
| 2 still, again | `start=4.000 end=1.056 … movesMin=1 movesMax=1` | "not sure" |
| 3 walking away (forward held all 21 ticks) | `start=3.200 end=2.991 … movesMin=1 movesMax=1` | "it basically stopped me which is what I want for walking" |
| 4 sprinting (forward and sprint held all 21 ticks) | `start=4.700 end=9.235 ticks=8 … outcome=left` | "yes" |
| 5 stutter or snap back | — | "no" |
| 6 the pull now | — | "feels good" |

**CONFIRMED BY PLAY:** the still drag; the sprint out from 4.7; no stutter or snap back from the explode packet (and no
sound or sight of it reported). **Ben's own words on walking:** the drag "basically stopped" his walk, "which is what I
want for walking"; the seat had put "slows without cancelling" to him, and this is his answer to it, recorded and not
ruled here. **"feels good"** is his answer on the pull at 0.33.
