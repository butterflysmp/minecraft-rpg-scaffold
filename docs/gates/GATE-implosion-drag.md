# GATE — THE IMPLOSION'S DRAG: THE PHAGE'S PULL CONTINUES FOR A SECOND

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/implosion-drag`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

**What it gates.** Ben's four yeses of 2026-11-18 (PLAN-dragon-mobs.md, RULINGS): the Phage's blast DRAGS everything it
is hostile to inside its reach toward where it stood for the 20 ticks after the blast; the damage stays at the blast's
instant, once; walking away slows the drag, sprinting away from the edge escapes it; it never carries him past half a
block short of the centre, and holds him there. The seat's change: no lift, no single pull. The swirl lasts the 20 ticks.

**What it supersedes.** Every row of `selftest/GATE-dragon-blast.yml` that blows a Phage was MOVED to
`selftest/GATE-implosion-drag.yml` and is re-predicted here with the next suffix: BL1R3 → BL1R4, BL2R3 → BL2R4, BL3R3 →
BL3R4, BL4R3 → BL4R4, BL6R3 → BL6R4, BL7R3 → BL7R4, BL9R3 → BL9R4, IM1-IM6 → IM1R2-IM6R2, FZ1/FZ3/FZ5 → FZ1R2/FZ3R2/FZ5R2,
DS1R5 → DS1R6. **BQ1R3 is RETIRED, not re-predicted:** its four questions were answered on 2026-11-17 (the swirl and the
small Phage's threat confirmed; the swell NOT STATED, asked again here as BQ2's first question; the pull answered by the
ruling this gate builds). New: **DR1** (he leaves the reach mid-drag), **DR2** (two blasts five ticks apart), **BQ2**
(Ben's five). The rows left in `GATE-dragon-blast.yml` (BL5R3, BL8R3, KS1, DS2R5, DS5R5, DS6R5, DG6R5, DG9R5) blow
nothing; their lines do not change and they are not re-run. `GATE-void-implosion.md` keeps every prediction and reading
and carries a dated note naming these rows.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL; a creative or spectator player is not a Phage's
  target and is not dragged (`MobHostility`, `ImplosionFooting.exempt`).
- **The difficulty is set by the selftest:** the setup's `difficulty: normal`, its `DIFFICULTY found=… set=NORMAL`, and
  `RESTORED … difficulty=<what it found>`.
- **Gear scores, per row:** 100, except **60** for BL2R4, FZ3R2, FZ5R2, DS1R6 and both of DR2's Phages. A Phage's
  zero-distance damage is `60 + 22.5 × (stage − 1)` × g / 100 (`MobBlast.price`, P-b): stage 1 at 100, 60.000; stage 3 at
  100, 105.000, at 60, 63.000; stage 5 at 100, 150.000, at 60, 90.000.

## THE DRAG AS BUILT, AND ITS NUMBERS FOR BEN (the seat's starting values)

**THE QUANTITY.** `drag`: BLOCKS PER TICK of horizontal velocity ADDED toward the blast's origin on each of the 20 ticks
after the blast, at the origin; at feet-to-feet distance d it is **`drag × (1 − d / reach) × (1 − res)`**, the old
push's linear factor (`Implosion.factor`), `res` the victim's explosion knockback resistance. It is ADDED to what the
victim already carries, so it accumulates as a force and his own walking adds against it. **THE MARGIN:** each tick's
add is cut so the speed toward the origin is at most `(h − 0.5) / travelFactor(0)`, the speed whose slide on his own
footing ends half a block short (`Implosion.dragStep`; on stone `travelFactor(0) = 1 / (1 − 0.546) = 2.202643`). The old
`pull` multiplier is a velocity given ONCE at the blast; it stays the single pull's (duration 1) and is 0 for a drag.

| number | value | mark |
|---|---|---|
| `duration_ticks` | **20** (ticks 1-20 after the blast's tick 0) | Ben's 1 ("for 1 second") |
| `drag` | **0.28** blocks/tick at the origin | CHOSEN 2026-11-18, the seat's starting value |
| `pull`, `lift` | **0**, **0** | the seat's change, told to Ben |
| margin | 0.5 blocks | CHOSEN 2026-11-17, unchanged |
| swirl | `duration_ticks` 10 → **20** on both spirals | CHOSEN 2026-11-18 (Ben's 1: "the swirl lasting the same second") |

**THE JAR'S MOVEMENT FACTS THE NUMBER WAS CHOSEN FROM, READ** (`javap -c -p`, client
`.minecraft/versions/26.1.2/26.1.2.jar`, server `run/versions/26.1.2/paper-26.1.2.jar`): a held key's input is
`1 × 0.98f` (`LocalPlayer#modifyInput`) `× 0.98f` (`LivingEntity#applyInput`); on the ground the speed is
`getSpeed() × 0.21600002f / f³` (`LivingEntity#getFrictionInfluencedSpeed`), f = 0.6f on stone; a player's speed is
`0.10000000149011612` (`Player#createAttributes`), sprint `+0.30000001192092896` `ADD_MULTIPLIED_TOTAL`
(`LivingEntity.SPEED_MODIFIER_SPRINTING`); each tick the horizontal is then × `0.6f × 0.91f` = 0.546. **EXECUTED**
(`ImplosionDragTest.theReadMovementFacts`): **walking adds 0.096040 a tick (terminal 0.21154 blocks/tick), sprinting
0.124852 (terminal 0.27500).**

**THE FOUR OUTCOMES AT A STAGE 5 (reach 6), EXECUTED** through `ImplosionDrag`, `DragMotion` and the client model
(`PlayerTravelModel`; scratch `figures.jsh`, `sweep.jsh`), pinned by `ImplosionDragTest`:

| outcome | from | at the second's end (tick 20) | rest | the test |
|---|---|---|---|---|
| **a** standing still | 2 / 3 / 4 / 5 | 0.500 / 0.500 / 0.505 / 1.000 | **0.500** at every one | `aStandingStillHeIsDraggedToTheMarginFromTwoThreeFourAndFive` |
| **b** walking away | 3, from rest / already walking | **0.769 / 1.339** (both < 3) | — | `bWalkingAwayFromThreeHeEndsTheSecondNearerThanHeStarted` |
| **c** sprinting away | 4 / 4.5 / 5 / 5.9, from rest | out of the reach at ticks **14 / 9 / 6 / 1** | — | `cSprintingAwayFromTheOuterThirdHeLeavesTheReachInTheSecond` |
| **d** at the margin / inside it | 1, 2, 3 / 0.3 | never nearer than 0.500, no tick nearer than the one before / **nothing sent** | 0.500 / 0.300 | `dReachingTheMarginHeIsHeldWithNoOvershootAtAnyTick`, `dInsideTheMarginHeIsNeverMoved` |

**WHY 0.28 AND NOT ANOTHER** (EXECUTED, `sweep.jsh`, through the same classes): at **0.24** a still player from 5 rests at
1.207 and an already-walking one from 3 ends the second at 2.901, only 0.10 inside where he started; at **0.26**, 0.614
and 2.163; at **0.28**, 0.500 and 1.339, while sprinting from 4 still gets out at tick 14, six ticks to spare; at
**0.30**, 0.500 and 0.813, the sprint out at tick 15. **For Ben, as information:** at 0.28 WALKING away from 4 he ends
the second at 5.19, still inside, and at 0.24 he got out (6.02).

**BETWEEN TWO ORIGINS (the seat's lean, built):** each drag pulls toward its own origin and its own margin cuts only its
own add, and both adds go into ONE velocity that tick (`DragMotion.tally`). So between two Phages he is pulled toward
the nearer and stronger, and once one Phage's second ends the other's carries him back for its last ticks (DR2); and
with one origin BETWEEN him and the other, the far one's add is not cut by the near one's margin, so the far one can
carry him past the near one's centre. **Listed for Ben, unruled.**

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC, ROW BY ROW

**The floor:** his maximum is at least 100 (`CombatantStats.DEFAULT_PLAYER_BASE`). Every row that can hit him first runs
`rpg heal 1000000`. **The drag deals nothing**: no damage path is called after the blast's one hit. **No fall:** with no
lift nothing throws him up, so no landing can hurt him (no row predicts `safe=true`). Each figure is the most the row
can deal before his defence after its heal (EXECUTED, `gate.jsh`, or the stage's base where a live Phage could reach his
feet):

| row | what hits him after one heal | the worst case, before defence |
|---|---|---|
| BL1R4, FZ1R2 | a LIVE stage 1, GS 100 | **60** |
| BL2R4, FZ5R2 | a LIVE stage 5, GS 60 | **90** |
| FZ3R2 | a LIVE stage 3, GS 60 | **63** |
| BL6R4 | a frozen stage 1 at 1.95 | **0.769** |
| BL9R4, DR1 | a frozen stage 5 at 3.00 | **56.250** |
| IM1R2 | a frozen stage 5 at 5.90 | **1.271** |
| IM3R2 | a frozen stage 5 at 2.00 | **83.333** |
| IM4R2 | a frozen stage 1 at 0.300 or farther | **47.175** |
| IM5R2 | a frozen stage 3 at 3.95 | **0.664** |
| DS1R6 | two thawed children, ONE heal | 36.000 + 49.500 = **85.500** |
| DR2 | two frozen stage 5s at GS 60, A at 3.00, then B no nearer than 3.00 (A drags him AWAY from B) | 33.750 + 33.750 = **67.500** |
| BQ2 | four blasts, a heal before each; the nearest at 3.00 | **56.250** |

BL3R4, BL4R4, BL7R4, IM2R2 and IM6R2 place him beyond the reach; BQ2's first Phage is 12 away.

## WHAT THE ROWS STAND ON — EACH FACT, AND ITS MARK

- **J1. A velocity the server sends REPLACES the client's, READ** (D1): `ClientPacketListener#handleSetEntityMotion` →
  `Entity#lerpMotion` → `setDeltaMovement` (client jar). It is applied before his next tick (`Minecraft#runTick`:
  `processQueuedPackets` before `tick`), and his tick then ADDS that tick's input (`Entity#moveRelative`). So a send
  wipes the momentum he carried, never the input of the tick it lands in.
- **J2. A player's momentum is estimated, never read from the server, BUILT** (`DragMotion`): his last tick's
  displacement × the friction of the state it started in, vertical `(dy − 0.08) × 0.98f`. **That one move packet
  arrives a server tick, and each send reaches him the next tick, is INFERRED** (a local server). A displacement over
  1.0 blocks is a teleport: no estimate that tick.
- **J3. Nothing is sent to a victim the drag adds nothing to, BUILT:** inside the margin he is left wholly alone, and a
  `sent=0` DRAG line is that row's witness.
- **J4. The drag's ticks:** tick 0 is the blast's own (it only looks); ticks 1-20 pull. **A victim seen at ticks 0 and 1
  is pulled from tick 1, so one inside the reach throughout reads `ticks=20`.** `start` is his distance at tick 0, the
  same tick and the same position the BLAST line's `d` is read at, so **`start` equals the entry's `d`** to three places.
  `end` is his distance at the last tick the drag saw him (tick 20), before the last send has moved him: a victim still
  sliding reads a little farther out than he comes to rest.
- **J5. Where he comes to rest:** the model's rest point (EXECUTED, `gate.jsh`). **THE TOLERANCE, TAKEN FROM READINGS:
  ± 0.20 blocks on every figure that rests on his own physics** (`end`, and his feet in a SELF line). Its basis: of the
  five rest points `GATE-void-implosion.md` F-B read short, four re-derive to the printed two places once the sent lift
  is the server's resting −0.0784 lower (D2, below), and the fifth (BL9R3) sits 0.158 off that; 0.20 is that worst
  residual, rounded up. **A drag has no flight**, so none of D2's mechanism is in these figures; what the tolerance
  covers is J2's INFERRED estimate. **And never past the margin by more than 0.10:** `end` and his feet are predicted no
  nearer than 0.40 to the origin.
- **J6. `sent`, RECORDED, not judged:** how many ticks carried an add above zero depends on his client's motion tick by
  tick (J2). The model's count is given beside it.
- **J7. Only players are dragged by the Phage** (the predicate in `MobBlaster`, `MobHostility.hostileTo`): a husk or a
  Phage in the reach is never in a DRAG line and its velocity is never set (BL4R4, with its control).
- **J8. The BLAST line's victim entry** is today's format with `pull=0`, `lift=0`: `strength=0.000000`,
  `pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false`, `tf=2.202643` on stone and `cap = max(0,
  h − 0.5) / 2.202643`. Every exact string below was printed by `MobBlaster.entry` itself (EXECUTED, `gate.jsh`).
  **A figure at a rounding half is predicted from the game's own distance** (F-A): at 1.95 of 2 and 0.95 of 2 the factor
  is a half at the sixth place; the game's distance is `1.9500000000000028` / `0.9500000000000028` (BL6R3's and FZ1's
  readings), and the strings below are printed from those.
- **J9. The head:** `BLAST mob=phage id=<uuid> stage=<n> gs=<g> base=<b> reach=<R> pull=0.00 lift=0.00 drag=0.280
  duration=20 visual=void_implosion …` (`MobBlastLineTest`).
- **J10. The DRAG line:** `[Rpg] DRAG source=phage id=<the Phage> victim=player <Ben> start=<d> end=<d> ticks=<n>
  sent=<n> outcome=<held|left|dropped|pulled>` (`ImplosionDragsLineTest`): `held`, the margin cut his last pulled tick's
  add; `left`, last seen outside the reach; `pulled`, neither.
- **J11. The swirl:** two `[Rpg] SWIRL visual=void_implosion radius=<R> arms=3 ticks=20 at=<the Phage's feet>` per blast;
  pending work for 20 ticks, as the drag is, so every row that blows waits at least 24 before it ends.
- **J12. The instrument's LIGHT, BUILT** (`MobFuse#light`, `MobFuseTest`): a frozen Phage's fuse is lit with no target
  and held lit; its SWELL line `reason=light d=- trigger=- cancel=-`; `FUSED` follows 29 or 30 ticks later (the light and
  the fuse's own tick fall in one tick or the next, INFERRED). **That the client shows a frozen creeper's swell is
  INFERRED** (the client's `Creeper#tick` runs on the synched direction, AI or not): BQ2's first question asks.
- **J13. D3, the push out of an overlapping mob**, READ: `Entity#push` sets `needsSync`, never `hurtMarked`, so a push on
  a PLAYER is never sent; the server keeps it in its own copy of his velocity. IM4R2 is its control: nothing is sent to
  him now, so he cannot end 0.41 west.

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-implosion-drag"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   If the folder exists, add `-2`. Report the folder, both pairs of counts, and `diff -rq lines: 0` for each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **Boot this branch's head with `--refresh-content`:** `./scripts/dev-server.sh --refresh-content`, announcing "booting
   now", on Ben's "ready". **REQUIRED:** the deployed `phage.yml` lacks `duration_ticks` and `drag`; the loader refuses it
   by name (`MobLoaderTest.aMalformedBlastIsRefused`, `nodrag.yml`), and no Phage loads.
5. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` names this branch's head; readings count only
   after Ben's LOGIN.
6. **Tell Ben to wait for the word that R0 passed before he logs in.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **Wait for my word that R0 passed before you log in.**
1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest implosion-drag` — **ONCE. Run it one time only, even after it finishes.** It takes about **two and a
   half minutes**, plus the time you take to answer (2122 ticks of waits, about 106 seconds, plus each row's staging; 20
   rows). Stand still and touch nothing, **except when gold text in chat tells you to do something, or a gold question
   appears**: do what the gold text says, and answer each question `yes` or `no` in chat. There are **five** questions,
   all at the end. **Many blasts will drag you toward them for a second; that is expected. Stand still again
   afterwards.** Wait for the chat to say the run finished, and answer every gold question before that.
3. `/stop`

Nothing else this boot.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/implosion-drag` prints it, shortened. **Not** master's `24eb52e7`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries the drag, the swirl's second, the LIGHT and the moved rows

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/mobs/phage.yml` lines `  duration_ticks: 20`, `  drag: 0.28`, `  pull: 0`, `  lift: 0`, and ABSENT `  pull: 1.25` (the control); `content/visuals/void_implosion.yml` with `    duration_ticks: 20` PRESENT and `    duration_ticks: 10` ABSENT; `ImplosionDrags.class` carrying `DRAG source=`; `MobBlaster.class` carrying `drag=%.3f duration=%d`, ABSENT the text control `NoSuchTextControl`; the entries `io/github/butterflysmp/rpg/core/shape/ImplosionDrag.class` and `.../core/shape/DragMotion.class`; `Scenario$Verb.class` carrying `LIGHT` and `TELL`; in `selftest/GATE-implosion-drag.yml` the keys `BQ2:`, `DR2:` and `IM4R2:` PRESENT; in `selftest/GATE-dragon-blast.yml` `BL1R3:` ABSENT and `KS1:` PRESENT (the key control) | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$p = Bytes 'content/mobs/phage.yml'
foreach ($l in '  duration_ticks: 20', '  drag: 0.28', '  pull: 0', '  lift: 0', '  pull: 1.25') {
  if ($p -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT phage.yml '$l'" } else { "ABSENT  phage.yml '$l'" }
}
$v = Bytes 'content/visuals/void_implosion.yml'
foreach ($l in '    duration_ticks: 20', '    duration_ticks: 10') {
  if ($v -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT void_implosion.yml '$l'" } else { "ABSENT  void_implosion.yml '$l'" }
}
$d = Bytes 'io/github/butterflysmp/rpg/paper/adapter/ImplosionDrags.class'
if ($d -ne $null -and $d.Contains('DRAG source=')) { "PRESENT ImplosionDrags 'DRAG source='" } else { "ABSENT  ImplosionDrags 'DRAG source='" }
$b = Bytes 'io/github/butterflysmp/rpg/paper/health/MobBlaster.class'
foreach ($t in 'drag=%.3f duration=%d', 'NoSuchTextControl') { if ($b.Contains($t)) { "PRESENT MobBlaster '$t'" } else { "ABSENT  MobBlaster '$t'" } }
foreach ($e in 'io/github/butterflysmp/rpg/core/shape/ImplosionDrag.class', 'io/github/butterflysmp/rpg/core/shape/DragMotion.class') { if ($zip.GetEntry($e) -ne $null) { "PRESENT entry $e" } else { "ABSENT  entry $e" } }
$s = Bytes 'io/github/butterflysmp/rpg/paper/selftest/Scenario$Verb.class'
foreach ($t in 'LIGHT', 'TELL') { if ($s -ne $null -and $s.Contains($t)) { "PRESENT Scenario`$Verb '$t'" } else { "ABSENT  Scenario`$Verb '$t'" } }
foreach ($k in @(@('implosion-drag', 'BQ2'), @('implosion-drag', 'DR2'), @('implosion-drag', 'IM4R2'), @('dragon-blast', 'BL1R3'), @('dragon-blast', 'KS1'))) {
  if ((Bytes "selftest/GATE-$($k[0]).yml") -match "(?m)^$($k[1]):\s*$") { "PRESENT $($k[0]) $($k[1]):" } else { "ABSENT  $($k[0]) $($k[1]):" }
}
$zip.Dispose()
```

**A jar from master reads ABSENT on every PRESENT line above but `KS1:`** (the key control) and on `  pull: 0` and `  lift:
0`, PRESENT on `  pull: 1.25`, `    duration_ticks: 10` and `BL1R3:`; `NoSuchTextControl` is ABSENT on both (the class
needle's control).

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**: `GATE-void-implosion.md`'s reading, unchanged (no content file added or removed: `git diff --name-status 24eb52e7 -- paper/src/main/resources/content` lists only modifications); **5 mobs: `phage.yml` loads** (a stale copy is refused and reads 4); **11 abilities: the fixture does not ship**. No `Refusing`, `Skipping`, `SEVERE` or `Exception`; no `[Rpg]` `WARN` beyond `volley_stone`'s known pair | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 167 scenarios across 21 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, implosion-drag]`**, and **no** `Refusing selftest` | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | |

**The 167 is RECOUNTED:** in `paper/src/main/resources/selftest/`, `grep -cE '^  - ' index.yml` prints **21**; `cat $(grep
-E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints **188**; with
`'^setup:'`, **21**: **167**. **The control: at master `24eb52e7` the same commands print 20, 185, 20: 165.**

### R0e — the swell packet's self-check

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Phage swell packet: index=16 type=int typeId=1 server=V_26_1_2 readFor=V_26_1`**, at INFO, and **no** line beginning `UNVERIFIED Phage swell packet` (`GATE-void-implosion.md` R0e's reading; nothing in this branch touches it) | `Select-String run\logs\latest.log -Pattern 'swell packet'` | |

**If R0e reads `UNVERIFIED`, STOP:** no FZ row and no BQ2 question 1 is read.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument for every row:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST implosion-drag|\[Rpg\] BLAST|\[Rpg\] DRAG|\[Rpg\] SWELL|\[Rpg\] FUSED|\[Rpg\] SWIRL|\[Rpg\] GROWTH|\[Rpg\] SPLIT|CREEPERGUARD'`,
read after Ben's LOGIN, inside the row's own `SELFTEST implosion-drag <row> START … DONE` bracket. **A DRAG line prints
when its drag's tick 20 runs, about 20 ticks after its BLAST, inside the same row** (every row waits 24 or more).

**In EVERY row (not repeated):** after `CLEAN …`, **`LEFTOVER living=0 radius=16`** and **`LINGER work=0 entities=0
radius=48`**: the last row's drag and swirl ended inside it (J11); each spawn's `DRIVE spawn …` and its reply. **No
`CREEPERGUARD prime` line anywhere. No second HURT in any row for one hit** (no fall: no lift). **Two SWIRL lines per
blast, `ticks=20`.** **Every BLAST head reads `pull=0.00 lift=0.00 drag=0.280 duration=20`** (J9). **A non-zero LEFTOVER
or LINGER makes its row NOT AS PREDICTED.** Every victim's `res=0.000` (RECORDED 2026-11-17; a non-zero `res` scales the
drag by `1 − res` and the row is re-derived from the printed figure). **His feet:** `SELF … at=<x> <y> <z> onGround=true
fall=0.000`, y and z unchanged ± 0.10 in every row; x as each row says, ± 0.20 (J5).

### BL1R4 — a LIVE stage-1 Phage walks in, lights inside its 1-block trigger, blows, and drags him to its margin (SURVIVAL) — supersedes BL1R3

| prediction | stands on | READING |
|---|---|---|
| (1) `BLOCKS STONE count=189 box=-12 -1 -4 8 -1 4`. (2) **Exactly one** `SWELL mob=phage id=<bl1> dir=+1 reason=start … viewers=1 d=<D0> trigger=1.000 cancel=2.333 swell=0`, D0 < 1.000. (3) `FUSED … tick=<T+30> swell=30/30`. (4) **Exactly one** BLAST `stage=1 gs=100 base=60.000 reach=2.00 pull=0.00 lift=0.00 drag=0.280 duration=20 visual=void_implosion victims=1 [player <Ben> d=<D> factor=<…> dealt=<…> knock=<…> res=0.000 h=<D> ground=true tf=2.202643 strength=0.000000 cap=<max(0, D − 0.5) / 2.202643> pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []`, **D < 1.000**, factor, dealt and knock by the shape from the printed D. (5) Two SWIRL `radius=2.000 arms=3 ticks=20`. (6) **Exactly one** HURT `before=<dealt>`. (7) **Exactly one** `DRAG source=phage id=<bl1> victim=player <Ben> start=<D> end=<E> ticks=20 sent=<s> outcome=held`: **if D ≥ 0.5, E in [0.40, 0.70]**; **if D < 0.5, `sent=0` and E within 0.10 of D**. (8) `ENTITIES CREEPER count=0 radius=16`; after `return`, `BLOCKS STONE count=189 …`. (9) No GROWTH, no RESET | J4-J10; the walk is vanilla's AI (INFERRED), D bounded by the trigger | |

### BL2R4 — a LIVE stage-5 Phage at GS 60, two blocks east, lights at once, blows at 2.000 and drags him 1.5 east (SURVIVAL) — supersedes BL2R3

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`. (1) `BLOCKS STONE count=189 …`. (2) `SWELL … dir=+1 reason=start … viewers=1 d=<D0> trigger=3.000 cancel=7.000 swell=0`, D0 within 0.05 of 2.000. (3) `FUSED … tick=<T+30> swell=30/30`. (4) BLAST `stage=5 gs=60 base=90.000 reach=6.00 pull=0.00 lift=0.00 drag=0.280 duration=20 …`; **at D = 2.000 the entry `player <Ben> d=2.000 factor=0.555556 dealt=50.000 knock=0.666667 res=0.000 h=2.000 ground=true tf=2.202643 strength=0.000000 cap=0.681000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false`**, else re-derived from the printed D. (5) Two SWIRL `radius=6.000 … ticks=20`. (6) HURT `before=<dealt>`. (7) **Exactly one** `DRAG … start=<D> end=<E> ticks=20 sent=<s> outcome=held`, **E = 0.50 ± 0.20, never under 0.40** (model 0.500, sent 11). (8) `ENTITIES CREEPER count=0`: **no children**; no SPLIT. (9) `BLOCKS STONE count=189 …` after the return | J4-J10; `gate.jsh` | |

### BL3R4 — a blast is a Void death: a stage-1 Phage 3.00 away grows to stage 2, and nobody is dragged (SURVIVAL) — supersedes BL3R3

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl3x <uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<bl3x> stage=1 gs=100 base=60.000 reach=2.00 pull=0.00 lift=0.00 drag=0.280 duration=20 visual=void_implosion victims=0 [] spared=0 []`. (3) **Exactly one** `GROWTH mob=phage id=<bl3b> from=1 to=2 fed=creeper/phage <bl3x> element=void d=3.000 max=100`. (4) `BODY tag=bl3b … stage=2 scale=0.882 height=1.500 … maxHP=100.000 gs=100 …`. (5) `BODY tag=bl3x none within 16 blocks`. (6) `ENTITIES CREEPER count=1 radius=16`. (7) Two SWIRL `radius=2.000 … ticks=20`. (8) **No DRAG line** (no player ever inside the reach), no HURT, no SWELL | BL3R3's reading; J7, J10 | |

### BL4R4 — a husk and a stage 5 in a STAGE-5 blast's reach are neither hurt NOR DRAGGED, at the blast and through the second; a control proves the probe sees a velocity (SURVIVAL) — supersedes BL4R3

| prediction | stands on | READING |
|---|---|---|
| (1) Before: `PROBE tag=bl4h … husk customHP=100.000/100.000 … d=7.280 dy=0.000`, `PROBE tag=bl4p … creeper customHP=160.000/160.000 … d=7.280 dy=0.000`, both MOTION `vx=0.0000 vy=0.0000 vz=0.0000`. (2) **THE CONTROL:** `MOTION tag=bl4c … vz=0.5000`. (3) **Exactly one** BLAST `stage=5 gs=100 base=150.000 reach=6.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … victims=0 [] spared=2 [<either order: husk <bl4h> d=2.000 factor=0.555556, creeper/phage <bl4p> d=2.000 factor=0.555556>]`. (4) **In the blast's tick, ten ticks into the drag and twenty-four after the blast**, all six MOTION lines `vx=0.0000 vy=0.0000 vz=0.0000`: **not dragged** (had the drag reached them at 2.000 its add would be 0.186667 a tick). (5) The two PROBE lines unchanged. (6) **No DRAG line**: the player at 7.280 is beyond the reach and the mobs are not the Phage's victims. (7) No GROWTH. (8) `ENTITIES CREEPER count=1`. (9) Two SWIRL `radius=6.000 … ticks=20`. No HURT | J7, BL4R3's reading; I10 of `GATE-void-implosion.md` (a frozen mob's velocity is visible to the probe) | |

### BL6R4 — THE STAGE-1 EDGE, inside (1.95 of 2): a sliver of 60, and the drag brings him in from the edge (SURVIVAL) — supersedes BL6R3

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> <y0> <z0> onGround=true fall=0.000`. (2) `DRIVE detonate tag=bl6 <uuid>`. (3) **Exactly one** BLAST `stage=1 gs=100 base=60.000 reach=2.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … victims=1 [player <Ben> d=1.950 factor=0.012812 dealt=0.769 knock=0.025000 res=0.000 h=1.950 ground=true tf=2.202643 strength=0.000000 cap=0.658300 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []`: **the factor printed from the game's distance `1.9500000000000028`** (J8; the literal 1.95 would print 0.012813). (4) Two SWIRL `radius=2.000 … ticks=20`. (5) **Exactly one** HURT `before=0.769`. (6) **Exactly one** `DRAG … start=1.950 end=<E> ticks=20 sent=<s> outcome=held`, **E = 0.53 ± 0.20, never under 0.40** (model 0.526, sent 18). (7) Forty-five ticks later `SELF … at=<x1> <y0> <z0>`, **x1 = x0 + 1.45 ± 0.20, never beyond x0 + 1.55** | J4-J8; `gate.jsh` | |

### BL7R4 — THE STAGE-1 EDGE, outside (2.05 of 2): neither hit nor dragged (SURVIVAL) — supersedes BL7R3

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> <y0> <z0>`. (2) `DRIVE detonate tag=bl7 <uuid>`. (3) **Exactly one** BLAST `stage=1 gs=100 base=60.000 reach=2.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … victims=0 [] spared=0 []`. (4) Two SWIRL `radius=2.000 … ticks=20`. (5) **No HURT, no DRAG line.** (6) `SELF … at=<x0> <y0> <z0>` again, **the same to the printed place**. BL6R4 is the control | the stage-1 reach | |

### BL9R4 — OUTCOME a AT 3: a STAGE-5 Phage at half its reach: three eighths of 150, and standing still he is dragged 2.5 east to its margin (SURVIVAL) — supersedes BL9R3

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> <y0> <z0> onGround=true fall=0.000`. (2) `DRIVE detonate tag=bl9 <uuid>`. (3) **Exactly one** BLAST `stage=5 gs=100 base=150.000 reach=6.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … [player <Ben> d=3.000 factor=0.375000 dealt=56.250 knock=0.500000 res=0.000 h=3.000 ground=true tf=2.202643 strength=0.000000 cap=1.135000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []`. (4) Two SWIRL `radius=6.000 … ticks=20`. (5) HURT `before=56.250`. (6) **Exactly one** `DRAG … start=3.000 end=<E> ticks=20 sent=<s> outcome=held`, **E = 0.50 ± 0.20, never under 0.40** (model 0.500, sent 16). (7) Forty-five ticks later `SELF … at=<x1> <y0> <z0> onGround=true fall=0.000`, **x1 = x0 + 2.50 ± 0.20, never beyond x0 + 2.60** | J4-J8; `ImplosionDragTest.aStandingStill…`; `gate.jsh` | |

---

## THE IMPLOSION'S OWN ROWS, RE-PREDICTED FOR THE DRAG

### IM1R2 — THE STAGE-5 EDGE, inside (5.90 of 6): a sliver of 150, and a drag faded almost to nothing (SURVIVAL) — supersedes IM1

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> <y0> <z0>`. (2) `DRIVE detonate tag=im1 <uuid>`. (3) **Exactly one** BLAST `stage=5 gs=100 base=150.000 reach=6.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … [player <Ben> d=5.900 factor=0.008472 dealt=1.271 knock=0.016667 res=0.000 h=5.900 ground=true tf=2.202643 strength=0.000000 cap=2.451600 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []`, from `d = 5.9000000000000060` (BL6R2's measured start). (4) Two SWIRL `radius=6.000 … ticks=20`. (5) HURT `before=1.271`. (6) **Exactly one** `DRAG … start=5.900 end=<E> ticks=20 sent=<s> outcome=pulled`, **E = 5.50 ± 0.20** (model 5.499, sent 20). (7) Forty-five ticks later `SELF … at=<x1> …`, **x1 = x0 + 0.50 ± 0.20** (model rest 5.396 from the Phage) | J4-J8; `gate.jsh` | |

### IM2R2 — THE STAGE-5 EDGE, outside (6.05 of 6): neither hit nor dragged (SURVIVAL) — supersedes IM2

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> …`. (2) `DRIVE detonate tag=im2 <uuid>`. (3) **Exactly one** BLAST `stage=5 gs=100 base=150.000 reach=6.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … victims=0 [] spared=0 []`. (4) Two SWIRL `radius=6.000 … ticks=20`. (5) **No HURT, no DRAG line.** (6) `SELF … at=<x0> …` again, the same to the printed place. IM1R2 is the control | the stage-5 reach | |

### IM3R2 — OUTCOME a AT 2, AND d: dragged to half a block short and HELD there, never past (SURVIVAL) — supersedes IM3

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> <y0> <z0> onGround=true fall=0.000`. (2) `DRIVE detonate tag=im3 <uuid>`. (3) **Exactly one** BLAST `stage=5 gs=100 base=150.000 reach=6.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … [player <Ben> d=2.000 factor=0.555556 dealt=83.333 knock=0.666667 res=0.000 h=2.000 ground=true tf=2.202643 strength=0.000000 cap=0.681000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []`. (4) HURT `before=83.333`. (5) **Exactly one** `DRAG … start=2.000 end=<E> ticks=20 sent=<s> outcome=held`, **E = 0.50 ± 0.20, never under 0.40** (model 0.500, sent 11): **held**, the outcome d witness. (6) Forty-five ticks later `SELF … at=<x1> <y0> <z0> onGround=true fall=0.000`, **x1 = x0 + 1.50 ± 0.20, never beyond x0 + 1.60** | J3-J8; `ImplosionDragTest.dReachingTheMarginHeIsHeld…` | |

### IM4R2 — OUTCOME d INSIDE THE MARGIN: nothing is sent and he is not moved; the D3 control (SURVIVAL) — supersedes IM4

**The staging and its one hazard:** a stage-1 Phage spawned 0.30 east of his feet, inside his box. His client pushes him
out of the overlap before the blast (IM4's reading: 0.19 west, to D = 0.487; the overlap bound 0.476471, jshell). Either
way D < 0.500: inside the margin.

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<xa> <y0> <z0>`, xa within [x0 − 0.20, x0]. (2) `DRIVE detonate tag=im4 <uuid>`. (3) **Exactly one** BLAST `stage=1 gs=100 base=60.000 reach=2.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … [player <Ben> d=<D> factor=<falloff(D, 2)> dealt=<60 × F> knock=<1 − D/2> res=0.000 h=<D> ground=true tf=2.202643 strength=0.000000 cap=0.000000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []`, **D in [0.300, 0.500)**: dealt in (39.375, 47.175] (at 0.487, 39.864; `gate.jsh`). **No lift now: `apex=0.000 safe=false`, no fall at all.** (4) **Exactly one** HURT `before=<dealt>`. (5) **Exactly one** `DRAG … start=<D> end=<E> ticks=20 sent=0 outcome=held`, **E within 0.10 of D**: **nothing sent** (J3). (6) Forty-five ticks later `SELF … at=<xb> <y0> <z0>`, **xb = xa ± 0.10**. **THE D3 CONTROL:** on 2026-11-17 he ended 0.41 WEST, because the blast sent the server's own copy of his velocity, the overlap push in it, with the lift (J13); now nothing is sent to him, so nothing can carry him away | J3, J13; `ImplosionDragTest.dInsideTheMarginHeIsNeverMoved` | |

### IM5R2 — THE STAGE-3 EDGE, inside (3.95 of 4) (SURVIVAL) — supersedes IM5

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 3, 120 HP, GS 100 given)" [aqua, gray]`. (1) `SELF … at=<x0> …`. (2) `DRIVE detonate tag=im5 <uuid>`. (3) **Exactly one** BLAST `stage=3 gs=100 base=105.000 reach=4.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … [player <Ben> d=3.950 factor=0.006328 dealt=0.664 knock=0.012500 res=0.000 h=3.950 ground=true tf=2.202643 strength=0.000000 cap=1.566300 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []`. (4) Two SWIRL `radius=4.000 … ticks=20`. (5) HURT `before=0.664`. (6) **Exactly one** `DRAG … start=3.950 end=<E> ticks=20 sent=<s> outcome=pulled`, **E = 3.50 ± 0.20** (model 3.501, sent 20). (7) Forty-five ticks later `SELF … at=<x1> …`, **x1 = x0 + 0.60 ± 0.20** (model rest 3.353) | J4-J8; the stage-3 reach | |

### IM6R2 — THE STAGE-3 EDGE, outside (4.05 of 4) (SURVIVAL) — supersedes IM6

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> …`. (2) `DRIVE detonate tag=im6 <uuid>`. (3) **Exactly one** BLAST `stage=3 gs=100 base=105.000 reach=4.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … victims=0 [] spared=0 []`. (4) Two SWIRL `radius=4.000 … ticks=20`. (5) **No HURT, no DRAG line.** (6) `SELF … at=<x0> …` again, the same to the printed place. IM5R2 is the control | the stage-3 reach | |

---

## THE FUSE BY STAGE, RE-PREDICTED FOR THE DRAG

**Each row as `GATE-void-implosion.md`'s FZ rows:** a LIVE Phage at speed 0, its trigger's edge + 0.05 from his feet F0;
forty ticks for its target; he steps IN 0.10; ten ticks on OUT beyond its cancel; fifteen ticks later IN again; it blows
30 ticks after it lit. **Clauses (a)-(i) and (k)-(l) exactly as those rows predicted them** (`TARGET … target=self`; no
SWELL before the step in; one START at `d=<in>`; `fuse=<n>/30 dir=+1`, n in [8, 10]; one RESET at `d=<out>` with
`swell=<T2 − T1>`; drained to `fuse=0/30 dir=-1`; a second START and `FUSED … tick=<T3 + 30>`; the BLAST below and two
SWIRL lines at the stage's reach, now `ticks=20`; HURT `before=<dealt>`; `ENTITIES CREEPER count=0`; no CREEPERGUARD).
**(j) His feet:** `SELF` just after the last step in at x = F0.x + 0.10, and **75** ticks later (30 to the blast, 20 of
drag, 25 of slide) at **x = the Phage's x − 0.50 ± 0.20, never nearer than 0.40 to it**. **(m) Exactly one DRAG line.**

### FZ1R2 — stage 1: lights inside 1.000, resets beyond 2.333, and drags him to its margin (SURVIVAL) — supersedes FZ1

| prediction | stands on | READING |
|---|---|---|
| (a)-(m), with: in `d=0.950`, out `d=2.400`, `trigger=1.000 cancel=2.333`; BLAST `stage=1 gs=100 base=60.000 reach=2.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … [player <Ben> d=0.950 factor=0.400312 dealt=24.019 knock=0.525000 res=0.000 h=0.950 ground=true tf=2.202643 strength=0.000000 cap=0.204300 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []`, **the factor printed from the game's `0.9500000000000028`** (J8; FZ1's reading); SWIRL `radius=2.000`; DRAG `start=0.950 end=<E> ticks=20 sent=<s> outcome=held`, E = 0.50 ± 0.20 (model 0.500, sent 12); his feet after at **F0.x + 0.55 ± 0.20** (the Phage at F0.x + 1.05) | J4-J8; FZ1's reading; `gate.jsh` | |

### FZ3R2 — stage 3 at GS 60: lights inside 2.000, resets beyond 4.667, drags him in (SURVIVAL) — supersedes FZ3

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 3, 72 HP, GS 60 given)" [aqua, gray]`. (a)-(m), with: in `d=1.950`, out `d=4.750`, `trigger=2.000 cancel=4.667`; BLAST `stage=3 gs=60 base=63.000 reach=4.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … [player <Ben> d=1.950 factor=0.387578 dealt=24.417 knock=0.512500 res=0.000 h=1.950 ground=true tf=2.202643 strength=0.000000 cap=0.658300 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []`; SWIRL `radius=4.000`; DRAG `start=1.950 end=<E> ticks=20 sent=<s> outcome=held`, E = 0.50 ± 0.20 (model 0.500, sent 10); his feet after at **F0.x + 1.55 ± 0.20** | as FZ1R2 | |

### FZ5R2 — stage 5 at GS 60: lights inside 3.000, resets beyond 7.000, drags him in (SURVIVAL) — supersedes FZ5

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`. (a)-(m), with: in `d=2.950`, out `d=7.050`, `trigger=3.000 cancel=7.000`; BLAST `stage=5 gs=60 base=90.000 reach=6.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … [player <Ben> d=2.950 factor=0.383368 dealt=34.503 knock=0.508333 res=0.000 h=2.950 ground=true tf=2.202643 strength=0.000000 cap=1.112300 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []`; SWIRL `radius=6.000`; DRAG `start=2.950 end=<E> ticks=20 sent=<s> outcome=held`, E = 0.50 ± 0.20 (model 0.500, sent 10); his feet after at **F0.x + 2.55 ± 0.20** | as FZ1R2 | |

---

## THE SPLIT ROW WITH TWO BLASTS

### DS1R6 — split by his emberblade at GS 60; thawed; each child walks in, lights and blows, and each DRAGS him toward its own feet, the second overlapping the first (SURVIVAL) — supersedes DS1R5

| prediction | stands on | READING |
|---|---|---|
| (1)-(5) as DS1R5 read them (the spawn and damage replies, `DRIVE attack … d=3.000 charge=1.000`, two `CHILD … frozen=true`, one `SPLIT … stage=5 gs=60 children=2 childStage=1 [<A> dx=-0.50, <B> dx=0.50]`, `ENTITIES CREEPER count=2`, two stage-1 BODY lines `maxHP=48.000 gs=60`, the heal's reply, two `DRIVE thaw … ai=true`). (6) **Within the 230 ticks, for each child a `SWELL … dir=+1 reason=start`, a `FUSED`, and exactly one BLAST, X then Y**, each head `pull=0.00 lift=0.00 drag=0.280 duration=20`: X `stage=1 gs=60 base=36.000 reach=2.00 … victims=1 [player <Ben> d=<D1> …] spared=<n> […]`, n = 1 naming `creeper/phage <Y> d=<d> factor=<falloff(d, 2)>` exactly when Y stood within 2 of X; **if n = 1**, exactly one GROWTH `id=<Y> from=1 to=2 … d=<d>`, printed identically to X's spared `d`, and Y's BLAST `stage=2 gs=60 base=49.500 reach=3.00 …`; **if Y grew but was beyond 2 of X**, the GROWTH line still comes and X's `spared=0 []`. Each victim entry's fields by J8 at its own stage's reach, **`ground=true` and `h = d` in both** (no lift: Y's blast no longer finds him in the air, as DS1R5's did). **The bounds: D1 < 1.000; D2 ≤ Y's cancel** (2.333, or 3.500 if grown). (7) **Exactly two** HURT lines, dealer X then Y. (8) **Exactly two DRAG lines on him**, `id=<X>` then `id=<Y>`, each `ticks=<n>` with n in [1, 20] (RECORDED; 20 when he stays inside that child's reach), each `outcome=<held|pulled|left>` RECORDED, **each `end` no nearer than 0.40 to its own origin** (outcome d, per pull). (9) `ENTITIES CREEPER count=0`. (10) One SPLIT; no `CREEPERGUARD` | DS1R5's reading; J4-J10. **The timing depends on vanilla's AI** (INFERRED) | |

---

## THE DRAG'S OWN ROWS

### DR1 — HE LEAVES THE REACH MID-DRAG: a step moves him out, and the drag stops pulling him (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> <y0> <z0>`. (2) `DRIVE detonate tag=dr1 <uuid>`. (3) **Exactly one** BLAST `stage=5 gs=100 base=150.000 reach=6.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … [player <Ben> d=3.000 factor=0.375000 dealt=56.250 knock=0.500000 res=0.000 h=3.000 ground=true tf=2.202643 strength=0.000000 cap=1.135000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []`. (4) HURT `before=56.250`. (5) Two ticks after the step, `SELF … at=<x1> …`, **x1 − x0 in [−6.00, −4.00]**: the step's −6 and the drag's few ticks east (model: 1.33 east by the blast + 6, so −4.68; `gate.jsh`). (6) **Exactly one** `DRAG … start=3.000 end=<E> ticks=<n> sent=<s> outcome=left`, **n in [4, 7]** (the step comes 6 ticks after the blast, J4), **E > 6.000**. (7) Forty ticks later `SELF … at=<x2> …`, **x2 = x1 ± 0.10: no drag after he left** | J4, J10; `ImplosionDragTest.oneWhoLeavesTheReachIsNoLongerPulledAndEndsLeft` | |

### DR2 — TWO BLASTS FIVE TICKS APART: each drags toward its own origin, each add cut by its own margin (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (0) Two `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`. (1) `SELF … at=<x0> <y0> <z0>`. (2) `DRIVE detonate tag=dr2a <uuid>`, then five ticks later `DRIVE detonate tag=dr2b <uuid>`. (3) A's BLAST `stage=5 gs=60 base=90.000 reach=6.00 pull=0.00 lift=0.00 drag=0.280 duration=20 … [player <Ben> d=3.000 factor=0.375000 dealt=33.750 knock=0.500000 res=0.000 h=3.000 ground=true tf=2.202643 strength=0.000000 cap=1.135000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []`. (4) B's BLAST, the same head, `[player <Ben> d=<DB> …]`, **DB in [3.44, 4.53]** (model 3.964 at a five-tick lag, 3.642 to 4.325 at four to six; ± 0.2), its fields by J8 from the printed DB. (5) Exactly two HURT, `before=33.750`, then `before=<B's dealt>`. (6) **Exactly two DRAG lines**: A's `id=<dr2a> … start=3.000 end=<EA> ticks=20 sent=<s> outcome=held`, **EA = 0.56 ± 0.20, never under 0.40**; then B's `id=<dr2b> … start=<DB> end=<EB> ticks=20 sent=<s> outcome=pulled`, **EB = 5.26 ± 0.25** (model 5.263; 5.196 to 5.323 at four to six). (7) Four SWIRL lines `radius=6.000 … ticks=20`. (8) Fifty ticks after B, `SELF … at=<x1> …`, **x1 = x0 + 2.12 ± 0.25, never beyond x0 + 2.60** (A's margin): A wins while both run, and B carries him back west for its last five ticks | J4-J10; `ImplosionDragTest.twoDragsOnOneBodyInOneTickAddToOneVelocity`; `gate.jsh` | |

---

## BEN'S ROW — BY THE ASK STEP. EACH ANSWER IS ONE WORD

### BQ2 — the swell he could not be sure of, and the drag four ways, by his own eyes and feel (SURVIVAL) — new; BQ1R3 retired

Five stagings in one row, each told in GOLD before it happens (the TELL step): **(1)** a FROZEN stage-5 Phage 12 blocks
in front of him, twice its reach away, is LIT by the instrument (never by distance) and swells for 30 ticks before it
blows: question 1. **(2)** Standing still, a stage 5 four blocks in front of him blows: **33.333** before his defence,
dragged to its margin. Question 2. **(3)** Facing a glass wall west and holding W, a stage 5 three blocks behind him
blows as the glass vanishes: **walking away, still dragged in** (model: from 3.2, the second ends at 1.185). Question 3.
**(4)** The same, sprinting, from 4.5 behind him: **he gets out** (model: from 4.7, out of the reach by tick 8).
Question 4. **(5)** How the pull feels. **Each question waits 1200 ticks for his next chat line; his answer cell is his
line, verbatim.** "not sure", or no line within the minute, is NOT STATED and never re-asked in this boot.

| prediction | READING |
|---|---|
| Recorded beside the answers, in order: `TELL QUESTION 1 IS COMING. …`; `DRIVE light tag=bq2a <uuid>`; **exactly one** `SWELL mob=phage id=<bq2a> dir=+1 reason=light tick=<T> viewers=1 d=- trigger=- cancel=- swell=0`; `FUSED mob=phage id=<bq2a> tick=<T + 29 or T + 30> swell=30/30` (J12); BLAST `stage=5 gs=100 base=150.000 reach=6.00 pull=0.00 lift=0.00 drag=0.280 duration=20 visual=void_implosion victims=0 [] spared=0 []`; two SWIRL `radius=6.000 … ticks=20`; no HURT, no DRAG. Then `TELL QUESTION 2 …`; BLAST `… [player <Ben> d=4.000 factor=0.222222 dealt=33.333 knock=0.333333 res=0.000 h=4.000 ground=true tf=2.202643 strength=0.000000 cap=1.589000 pull=0.000000 capped=false lift=0.000000 apex=0.000 rest=0.000 safe=false] spared=0 []`; HURT `before=33.333`; `DRAG … start=4.000 end=<E> ticks=20 sent=<s> outcome=held`, E = 0.51 ± 0.20 (model 0.505, sent 15). Then `TELL QUESTION 3 …`, `TELL STOP. Let go of W.`; BLAST `… [player <Ben> d=<D3> …]`, **D3 in [3.00, 3.25]** (pressed against the glass, 0.2 farther than his block centre); HURT `before=<dealt>`; `DRAG … start=<D3> end=<E3> ticks=<n> …`, **E3 < D3 is outcome b's server half** (model from 3.2, walking from rest: 1.185, `held`). Then `TELL QUESTION 4 …`, `TELL STOP. Let go of W and sprint.`; BLAST `… [player <Ben> d=<D4> …]`, **D4 in [4.50, 4.75]**; HURT `before=<dealt>`; `DRAG … start=<D4> … ticks=<n> … outcome=left`, **n ≤ 12, `outcome=left` is outcome c's server half** (model from 4.7, sprinting from rest: out at tick 8, `ticks=7`). **Whether he walked, or sprinted, is RECORDED by the SELF lines and judged by nobody here: the instrument cannot drive his keys** | |
| `ASK 1 question="Did you see the big Phage swell up before it blew?" answer="<his line>" ticks=<t>` | |
| `ASK 2 question="Standing still, did it drag you in toward where it stood, for about a second?" answer="<his line>" ticks=<t>` | |
| `ASK 3 question="Walking away, did the drag slow you down without stopping you, so you still went back toward it?" answer="<his line>" ticks=<t>` | |
| `ASK 4 question="Sprinting away from the edge, did you get out of the drag?" answer="<his line>" ticks=<t>` | |
| `ASK 5 question="Does the pull feel right now?" answer="<his line>" ticks=<t>` | |

---

## THE ROWS ALREADY MERGED — THE AUDIT (2026-11-18, FOR THE DRAG)

**The question asked of every merged selftest row:** does it blow a Phage (its BLAST head now names the drag, a victim
is now dragged for 20 ticks, the swirl lasts 20), or does it end fewer than 24 ticks after a blast (the drag and the
swirl would still be pending at the next row's clean, which would end them: `LINGER work` above 0, and no DRAG line)?
**Instrument:** `grep -n "detonate\|live phage\|thaw" paper/src/main/resources/selftest/GATE-*.yml`, every hit read with
its row.

| file | rows | what changes | done |
|---|---|---|---|
| `GATE-dragon-blast.yml` | BL1R3-BL4R3, BL6R3, BL7R3, BL9R3, IM1-IM6, FZ1, FZ3, FZ5, DS1R5 (17) | every BLAST head; a victim's entry and a DRAG line; IM1/IM2/IM5/IM6, BL3R3, BL4R3, BL6R3, BL7R3 ended **12** ticks after their blast, and a 20-tick drag and swirl would outlive them; FZ rows read his feet 15 ticks into the drag | **MOVED and RE-PREDICTED** here; every row now waits 24 or more after a blast, and reads his feet 45 (or 75) ticks after |
| `GATE-dragon-blast.yml` | BQ1R3 | its two live Phages blow on him | **RETIRED** (answered 2026-11-17); BQ2 asks the drag's questions |
| `GATE-dragon-blast.yml` | BL5R3, BL8R3, KS1, DS2R5, DS5R5, DS6R5, DG6R5, DG9R5 | **none:** nothing blows (a reset fuse, a refused ignite, a Knell, kills that split) | **unchanged, not re-run** |
| `GATE-dragon-bodies.yml`, `-split.yml`, `-growth.yml`, `GATE-dragon-shot.yml` | every Phage row | **none:** every Phage frozen and never blown (`noScenarioOutsideTheBlastGateLeavesALivePhage`, which now also counts LIGHT as live) | **unchanged, not re-run** |
| the 13 closed manual gates that spawn live dragon mobs | — | a live Phage that blows now drags for a second instead of yanking once | **carried** (PLAN §8), not re-predicted |

## WHAT NO ROW CAN STAGE — UNIT-ONLY, AND WHY

- **Outcomes b and c by the instrument:** the server cannot press his keys; a teleport is not walking. Staged for Ben
  (BQ2, glass wall) and pinned in core against the client model (`ImplosionDragTest` b and c).
- **The test-only fixture's drag on MOBS:** the fixture does not ship (11 abilities), so no in-game row can name it.
  `ImplosionEffectTest.theFixturesDragPullsTheMobsInAndNoFartherThanTheMargin` runs its drag over mobs, `AbilityLoaderTest`
  loads it with `duration_ticks: 20` and `drag: 0.28`.
- **A victim who dies, logs out, changes world or goes creative mid-drag:** the selftest refuses a death outside
  `death_rules`, a logout ends the run, and the run holds survival. `ImplosionDragTest.aDroppedVictimIsNeverPushedAgain…`
  pins the core half; the paper half (`ImplosionDrags`'s lookup: absent, dead, other world, no longer allowed) is READ
  in the code, not run.
- **A drag ended by the row-start clean or a server stop:** no row ends inside a drag (by design, every row waits 24).
  The clean's half is the merged `PendingWork` mechanism the swirl already uses; a stop cancels the plugin's tasks
  (INFERRED).
- **A second player** (the selftest refuses one): two players in one drag are each their own victim, by the same code.

## THE BOOT — WHAT WAS READ

*Not booted.*
