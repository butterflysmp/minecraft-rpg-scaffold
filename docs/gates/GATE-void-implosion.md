# GATE — THE VOID IMPLOSION: THE PHAGE'S BLAST PULLS, AND THE KNELL LANDS BESIDE A TARGET ON A SLAB (F19)

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/void-implosion`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, has
ruled the two questions owed below, and Ben says ready.**

**What this gate re-predicts, and why.** Ben, 2026-11-17 (PLAN-dragon-mobs.md, RULINGS, *the Knell's slab bug, and a
void explosion that pulls*): "swap the push for the pull", the look black and dark purple swirling particles, the shape
reusable. **Every row of `selftest/GATE-dragon-blast.yml` is re-predicted here as a NEW row named for the
`GATE-dragon-blast-2.md` row it supersedes, with the next suffix** (BL1R2 → BL1R3, DS1R4 → DS1R5, BQ1R2 → BQ1R3), and
the file gains **IM1-IM4** (the implosion's edges, its cap, its centre) and **KS1** (F19, the Knell on a slab). The
old rows' predictions and readings stay in `GATE-dragon-blast-2.md` under a dated note. The file keeps its name, so the
run sheet's command is unchanged.

> **TWO QUESTIONS ARE OWED BY THE SEAT BEFORE THIS GATE MAY BE BOOTED, AND THE GATE IS WRITTEN SO EITHER ANSWER LEAVES
> ITS ROWS VALID OR VISIBLY STALE:**
> 1. **PART C (the Phage's own fuse, its distances by stage) STOPPED AT THE JAR** (PLAN, *PART C STOPPED AT THE JAR*):
>    no API route shows a client swell that resets at our distances. **No fuse row is staged here, and the reach is 6
>    at every stage.** The brief's fuse rows (start just outside and inside the trigger at stages 1, 3 and 5, the reset
>    beyond the cancel, the 30 ticks, the swell state) and the per-stage reach rows wait on the seat's route. **If the
>    route scales the reach, IM1, IM2, BL6R3, BL7R3, every `reach=6.00` and every cap below are re-predicted, not read.**
> 2. **THE PULL TRAVELS LESS THAN THE PUSH DID** (PLAN, *THE PULL TRAVELS LESS THAN THE PUSH DID*): at d = 2, 1.5
>    blocks against about 4.1. BQ1R3's ASK 3 puts it to Ben; the seat may rule the pull's shape first instead.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL, and vanilla's swell skips a creative target.
- **The difficulty is set by the selftest:** the setup's `difficulty: normal`, the run's `DIFFICULTY found=… set=NORMAL`
  and `RESTORED … difficulty=<what it found>`.
- **Gear scores, per row:** 100 except BL2R3 (**60**), DS1R5 (**60**), and the split rows DS2R5, DS5R5, DG6R5, DG9R5
  (300, as before). A Phage's zero-distance damage is `60 + 22.5 × (stage − 1)` × g / 100 (`MobBlast.price`, P-b,
  unchanged): stage 1 at 100 is 60.000, stage 5 at 100 is 150.000, stage 5 at 60 is 90.000, stage 1 at 60 is 36.000,
  stage 2 at 60 is 49.500. KS1's Knell at 100: a beam hit is **65.000** before defence (`GATE-dragon-knell-5.md`).

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC, ROW BY ROW

**The floor:** his maximum is at least 100 (`CombatantStats.DEFAULT_PLAYER_BASE`; INFERRED that no shipped modifier
lowers a player below it). **Ben's own 350 is NOT relied on.** Every row that can hit him first runs `rpg heal
1000000`. The figure is the most the row can deal **before his defence** after that heal; defence only lowers it. **A
stage 5 at gear score 100 deals up to 150 at its feet: no row lets one blow nearer than 2 blocks** (IM1 at 5.90,
BQ1R3's at exactly 2.00), and the one live stage 5 (BL2R3) is at gear score 60. Each figure EXECUTED in jshell
(scratch `gate.jsh`).

| row | what hits him after one heal | the worst case, before defence | how it was brought under 100 |
|---|---|---|---|
| BL1R3 | a LIVE stage 1, GS 100 | 60 × 1 = **60** | nothing needed |
| BL2R3 | a LIVE stage 5 | at GS 60, 90 × 1 = **90** | gear score 60 |
| BL6R3 | a frozen stage 1 at 5.90 | 60 × 0.008472 = **0.508** | nothing needed |
| BL9R3 | a frozen stage 1 at 3.00 | 60 × 0.375 = **22.500** | nothing needed |
| IM1 | a frozen stage 5 at 5.90 | 150 × 0.008472 = **1.271** | nothing needed |
| IM3 | a frozen stage 1 at 2.00 | 60 × 0.555556 = **33.333** | nothing needed |
| IM4 | a frozen stage 1 at 0.30 to 0.477 (see IM4) | 60 × 0.926250 = **55.575** | stage 1, not 5 (a stage 5 there: 138.938) |
| KS1 | one forced Knell beam | **65** | nothing needed |
| DS1R5 | two thawed children, ONE heal | **36.000 + 49.500 = 85.500** | the parent's gear score 60 |
| BQ1R3 | stage 1 at 2.00, then after a SECOND heal stage 5 at 2.00 | **33.333**, then **83.333** | a fixed distance instead of live |

BL3R3, BL4R3 and BL7R3 place him beyond every blast's reach; IM2 at 6.05 is beyond it; BL5R3, BL8R3 and the split
rows blow nothing near him. **The pull cannot make a fall:** it has no vertical part (`Implosion.pull`, the impulse's y
is 0, pinned by `ImplosionTest.fromTheEdgeToTheCentreTheModelledTravelNeverPassesTheOriginLessTheMargin`), every row's
floor is flat stone, and the pull carries him EAST, toward a Phage east of him, never past it, so onto the row's own
floor. **No second HURT line is predicted in any row; one would be a fall, named, and the row NOT AS PREDICTED.**

## WHAT THE ROWS STAND ON — EACH FACT, AND ITS MARK

- **I1. The pull's strength:** `(1 − d / 6) × 1.25 × (1 − resistance)`, vanilla's push curve (§7 F-B, READ) times the
  pull (`phage.yml`, `pull: 1.25`), pointed toward the Phage. `ImplosionTest.theStrengthIsVanillasLinearShareTimesThePull
  TimesOneMinusResistance`; `MobBlastTest.thePullsStrengthIsVanillasLinearShareOfTheReachTimesTheMultiplier`.
- **I2. The model the cap stands on, READ** (`LivingEntity#travelInAir`, `Entity#getBlockPosBelowThatAffectsMyMovement`,
  `BlockBehaviour$Properties.<init>`): each tick a body moves by its delta, then its horizontal delta is multiplied by
  `(onGround ? friction of the block 0.500001 below : 1.0f) × 0.91f`; stone's friction is the default `0.6f`. So
  `m = 0.546000` on stone, `0.910000` in the air, and a speed v slides `v / (1 − m)`. **That a player's CLIENT runs the
  same travel is INFERRED** (the client's `LivingEntity` is the same class; not read client-side).
- **I3. The cap:** the applied speed is `min(strength, max(0, h − 0.5) × (1 − m))`, h the HORIZONTAL distance to the
  Phage's feet, so the modelled slide stops 0.5 short of them. On stone the cap binds inside d = 2.229995 (jshell).
  `ImplosionTest` walks it every 0.01 from 6 to 0, on the ground and in the air.
- **I4. The direction:** horizontal, from his feet toward the Phage's. No lift.
- **I5. Only victims are pulled, and only they are hurt** (`DragonBlastWiringTest.onlyTheVictimsArePulledByTheShapeAnd
  TheVisualIsTheContents`); a spared creature's entry carries nothing after its factor (`MobBlastLineTest`).
- **I6. The resistance:** his `EXPLOSION_KNOCKBACK_RESISTANCE`, read at the blast. **Its value on Ben is NOT predicted**
  (his armour is his own), so every `res=` is RECORDED and the strength predicted as `knock × 1.25 × (1 − res)`. Read
  0.000 at every blast of 2026-11-17.
- **I7. A frozen mob cannot be moved, READ** (§7 F-B, F18): a spared frozen mob's "not pulled" is read in its VELOCITY
  (`probe_motion`, in the blast's own tick), never its position. BL4R3's control proves the probe sees a velocity.
- **I8. The look is content, and the server cannot read it:** `void_implosion` (two DUST spirals, black `[16, 16, 16]`
  and dark purple `[88, 24, 140]`, size 1.5, 3 arms each, 1.5 turns, 6 points per arm per tick, over 10 ticks, purple's
  arms 60 degrees on, 0.6 above the feet; and `block.respawn_anchor.deplete` at volume 2.0, pitch 0.5: **every number
  CHOSEN, listed for Ben**), drawn at the blast's reach (`present(…, r)`). Its geometry is `SpiralTest`'s; **that he
  sees it is BQ1R3's ASK 1** (`.claude/rules/verification.md`, *AN AUTO READING IS A READING OF THE SERVER*).
- **I9. The swirl is pending work for its 10 ticks** (`PaperCombatWorld.spiral` through `schedule`, the C2 site), so
  **every row that blows waits at least 12 ticks before it ends** and the next row's `LINGER` reads `work=0`.
- **I10. The tolerance on his feet after a pull: ± 0.10 blocks**, CHOSEN, not measured: `SELF at=` prints two places
  (± 0.005), and the rest stands on I2's INFERRED client half. **Never past:** his x after is at most the Phage's x −
  0.5 + 0.10. A reading outside the band is NOT AS PREDICTED and names which side.
- **I11. The slab (KS1), READ in our code:** `BeamTeleport.foot` anchors every column on `BeamTeleport.anchor`
  (floor(y), the 1e-6 snap): a target at y0 + 0.5 is searched from y0. `BeamTeleportTest.aTargetOnASlabStillGivesA
  LandingOnTheFloorBesideIt`; its mutation (MUTF19A, the column's own y) is red. The landing geometry is
  `GATE-dragon-knell-5.md` K3's, unchanged.
- **S10 (carried): the tolerance on a figure recomputed from a printed distance:** `d` prints three places, `factor`
  six, `dealt` three: factor within 0.000125 + 0.0000005; dealt within 0.0005 + base × 0.0000005; `knock` within
  0.0000839; **and `strength` within 1.25 × 0.0000839 = 0.000105, `cap` (on stone, slope 0.454) within 0.0005 × 0.454
  + 0.0000005 = 0.000228.**

## THE PULL MOVES THE PLAYER — WHAT IT DOES TO EACH ROW'S READINGS

**A pull changes where he stands, and nothing the blast already wrote.** The BLAST and HURT lines are written at the
blast, from the positions at the blast. What it can change: **a later relative step** (BL1R3, BL2R3 and BQ1R3 run
`return` and re-align before anything relative; BL9R3, IM3, IM4 read his feet and end), and **the second blast of
DS1R5**, which now finds him NEARER, not farther: a second child already swelling blows wherever he is inside 7
(F-finding of 2026-11-17), so **its bound is D ≤ 7**, as it was after a push.

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-void-implosion"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   If the folder exists (a boot the same day), add `-2` to its name. Report the folder, both pairs of counts, and
   `diff -rq lines: 0` for each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report.
4. **Boot this branch's head with `--refresh-content`:** `./scripts/dev-server.sh --refresh-content`, announcing
   "booting now", on Ben's "ready". **The refresh is REQUIRED, not habit:** the deployed `phage.yml` still carries
   `knockback: 1.0`, which the loader now REFUSES by name (`MobLoaderTest.aMalformedBlastIsRefused`, `staleknock.yml`);
   booted without it, R0c reads a `Refusing` line for `phage.yml` and no Phage loads.
5. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` line names this branch's head. **Readings count
   only after Ben's LOGIN in that boot's log.**
6. **Tell Ben to wait for the word that R0 passed before he logs in** (the run sheet's first line).

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **Wait for my word that R0 passed before you log in.**
1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest dragon-blast` — **ONCE. Run it one time only, even after it finishes.** It takes about **a minute
   and a half**, plus the time you take to answer. Stand still and touch nothing, **except when a gold question
   appears in chat**: then type your answer, `yes` or `no`, in chat. There are **three** questions. **Some blasts will
   pull you a little toward them; that is expected. Stand still again afterwards.** Wait for the chat to say the run
   finished, and answer every gold question before that.
3. `/stop`

Nothing else this boot.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/void-implosion` prints it, shortened. **Not** master's `f6a596ea`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries the implosion and F19

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/mobs/phage.yml` lines `  pull: 1.25` and `  visual: void_implosion`, and ABSENT `  knockback: 1.0` (the old line, the control); `content/visuals/void_implosion.yml` with the line `  - type: spiral`; `MobBlaster.class` carrying `pull=%.2f` and `h=%.3f`, ABSENT `knockback=%.2f` and the text control `NoSuchTextControl`; `io/github/butterflysmp/rpg/core/shape/Implosion.class` PRESENT as an entry; `BeamTeleport.class` carrying `ANCHOR_SNAP`; in `selftest/GATE-dragon-blast.yml` the keys `BL1R3:`, `IM3:` and `KS1:` PRESENT and `BL1R2:` ABSENT; and `dragon-bodies DB11R2:` PRESENT (the key control) | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$p = Bytes 'content/mobs/phage.yml'
foreach ($l in '  pull: 1.25', '  visual: void_implosion', '  knockback: 1.0') {
  if ($p -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT phage.yml '$l'" } else { "ABSENT  phage.yml '$l'" }
}
$v = Bytes 'content/visuals/void_implosion.yml'
if ($v -ne $null -and $v -match "(?m)^  - type: spiral\s*$") { "PRESENT void_implosion.yml '  - type: spiral'" } else { "ABSENT  void_implosion.yml '  - type: spiral'" }
$b = Bytes 'io/github/butterflysmp/rpg/paper/health/MobBlaster.class'
foreach ($t in 'pull=%.2f', 'h=%.3f', 'knockback=%.2f', 'NoSuchTextControl') { if ($b.Contains($t)) { "PRESENT MobBlaster '$t'" } else { "ABSENT  MobBlaster '$t'" } }
if ($zip.GetEntry('io/github/butterflysmp/rpg/core/shape/Implosion.class') -ne $null) { "PRESENT entry core/shape/Implosion.class" } else { "ABSENT  entry core/shape/Implosion.class" }
$t = Bytes 'io/github/butterflysmp/rpg/core/mob/BeamTeleport.class'
if ($t.Contains('ANCHOR_SNAP')) { "PRESENT BeamTeleport 'ANCHOR_SNAP'" } else { "ABSENT  BeamTeleport 'ANCHOR_SNAP'" }
foreach ($k in @(@('dragon-blast', 'BL1R3'), @('dragon-blast', 'IM3'), @('dragon-blast', 'KS1'), @('dragon-blast', 'BL1R2'), @('dragon-bodies', 'DB11R2'))) {
  if ((Bytes "selftest/GATE-$($k[0]).yml") -match "(?m)^$($k[1]):\s*$") { "PRESENT $($k[0]) $($k[1]):" } else { "ABSENT  $($k[0]) $($k[1]):" }
}
$zip.Dispose()
```

**A jar from master reads the opposite on every line but `DB11R2:`** (the key control), and `NoSuchTextControl`
(ABSENT on both: the control that the class needle does not find everything). The class texts are searched in the
DECOMPRESSED entry. **That `core` classes are packaged into the plugin jar at those paths is INFERRED** (the R0b rows of
earlier gates searched only `paper` classes); if the Implosion entry reads ABSENT on a jar whose other lines all read
this branch, the row is NOT AS PREDICTED on that clause and the seat rules, before anything else is read.

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**: `GATE-dragon-skreel.md`'s reading with **29 → 30 visuals** (`void_implosion.yml` added; `ls paper/src/main/resources/content/visuals/*.yml \| wc -l` prints 30 here and 29 at master `f6a596ea`). **11 abilities, not 12: the test fixture does not ship.** No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN` beyond `volley_stone`'s known pair (the sound `block.respawn_anchor.deplete` is READ in `SoundEvents`, so the validator names nothing) | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 160 scenarios across 20 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel]`**, and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | |

**The 160 is RECOUNTED from the files at this branch's head:** in `paper/src/main/resources/selftest/`,
`grep -cE '^  - ' index.yml` prints **20**; `cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/;
s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints **180**; the same with `'^setup:'` prints **20**: 180 − 20 = **160**.
**The control: at master `f6a596ea` the same commands print 20, 175, 20: 155**, the count `GATE-dragon-skreel.md` read
(its last reading). So **R0d tells this jar from master's**. `SelfTestScenariosTest` pins the 160 and the 21 names.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument for every row:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-blast|\[Rpg\] BLAST|\[Rpg\] GROWTH|\[Rpg\] SPLIT|\[Rpg\] TELEPORT|CREEPERGUARD'`,
read after Ben's LOGIN, inside the row's own `SELFTEST dragon-blast <row> START … DONE` bracket.

**In EVERY row below (and not repeated in each):** after the row's `CLEAN …` line, **`LEFTOVER living=0 radius=16`**
and **`LINGER work=0 entities=0 radius=48`** (I9); for each mob the row spawns, one `DRIVE spawn tag=<its tag> <uuid>
<mob> frozen` (or `live`) and the command's reply as `GATE-dragon-blast-2.md` lists them (a stage-1 Phage at GS 100
`… stage 1, 80 HP, GS 100 given)`, a stage 5 at 100 `… stage 5, 160 HP …`, at 60 `… 96 HP, GS 60 given)`, at 300 `…
480 HP …`, a stage 2 at 300 `… 300 HP …`, a stage 4 at 300 `… 420 HP …`, the husk `… 100 HP …`). **No `CREEPERGUARD
prime` line anywhere in the run. No SPLIT line in BL1R3-BL9R3, IM1-IM4, KS1 or BQ1R3. A non-zero LEFTOVER or LINGER
makes its row NOT AS PREDICTED.**

**A BLAST line** is now `[Rpg] BLAST mob=phage id=<uuid> stage=<n> gs=<g> base=<b> reach=6.00 pull=1.25
visual=void_implosion victims=<n> [<victim>, …] spared=<n> [<spared>, …]` (`MobBlastLineTest`), a victim `player <uuid>
d=<d> factor=<f> dealt=<x> knock=<k> res=<r> h=<h> ground=<g> m=<m> strength=<s> cap=<c> pull=<p> capped=<b>`, a
spared creature `<type>[/<mob id>] <uuid> d=<d> factor=<f>` with nothing after. **For every victim:** `k = 1 − d/6`;
`r` RECORDED (I6); `s = k × 1.25 × (1 − r)`; on stone with `ground=true`, `m=0.546000` (with `ground=false`,
`m=0.910000`); `c = max(0, h − 0.5) × (1 − m)`; `p = min(s, c)`; `capped=true` exactly when `s > c`; each by S10. **In
every row here he stands on the Phage's level, so `h = d`.** **A HURT line** is `HURT before=<x> after=<a>
defense=<Dp> dealer=<uuid> dealerType=<recorded>`, `a = x × 100 / (100 + Dp)` to three places. **Damage is predicted
against the HURT line, never a health figure** (`.claude/rules/verification.md`).

### BL1R3 — a LIVE stage-1 Phage blows on the player and PULLS him (SURVIVAL) — supersedes GATE-dragon-blast-2.md BL1R2

| prediction | stands on | READING |
|---|---|---|
| (1) `BLOCKS STONE count=189 box=-12 -1 -4 8 -1 4` before the spawn. (2) **Exactly one** BLAST line `id=<the bl1 uuid> stage=1 gs=100 base=60.000 reach=6.00 pull=1.25 visual=void_implosion victims=1 [player <Ben's uuid> d=<D> factor=<F> dealt=<X> knock=<K> res=<R> h=<D> ground=true m=0.546000 strength=<S> cap=<C> pull=<P> capped=<…>] spared=0 []`, every figure by the rules above; **the bound** D < 3 (he stands still through the swell after a heal), so X > 22.500. (3) **Exactly one** HURT line, `before=<X>`, `dealer=<the bl1 uuid>`. (4) `ENTITIES CREEPER count=0 radius=16`. (5) After `DRIVE return …`, `BLOCKS STONE count=189 …`. (6) No GROWTH | I1-I6, S10; BL1R2's reading (`d=2.000`) | |

### BL2R3 — a LIVE stage-5 Phage at gear score 60 blows for 90 × the falloff, pulls him, and leaves NO children (SURVIVAL) — supersedes BL2R2

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`. (1) `BLOCKS STONE count=189 …` before. (2) **Exactly one** BLAST line `id=<the bl2 uuid> stage=5 gs=60 base=90.000 reach=6.00 pull=1.25 visual=void_implosion victims=1 [player <Ben's uuid> d=<D> … ground=true m=0.546000 …] spared=0 []`, X = 90 × F, the same pull rules; **the bound** D < 3, X > 33.750. **The pull does not depend on the stage** (one reach, one multiplier): at a given D it is BL1R3's. (3) **Exactly one** HURT line, `before=<X>`. (4) `ENTITIES CREEPER count=0 radius=16`: **no children**. (5) **No SPLIT.** (6) `BLOCKS STONE count=189 …` after the return | I1-I6; BL2R2's reading | |

### BL3R3 — a blast is a Void death: a stage-1 Phage 3.00 away grows to stage 2 (SURVIVAL) — supersedes BL3R2

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl3x <uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl3x uuid> stage=1 gs=100 base=60.000 reach=6.00 pull=1.25 visual=void_implosion victims=0 [] spared=1 [creeper/phage <the bl3b uuid> d=3.000 factor=0.375000]`. (3) **Exactly one** `[Rpg] GROWTH mob=phage id=<the bl3b uuid> from=1 to=2 fed=creeper/phage <the bl3x uuid> element=void d=3.000 max=100`. (4) `BODY tag=bl3b <uuid> type=creeper mob=phage stage=2 scale=0.882 height=1.500 speedBase=0.3125 speedType=0.2500 speedX=1.250 maxHP=100.000 gs=100 element=void dead=false`. (5) `BODY tag=bl3x none within 16 blocks`. (6) `ENTITIES CREEPER count=1 radius=16`. (7) No HURT | BL3R2's reading; the wait after the blast 5 → **12** (I9), which only moves the reads later | |

### BL4R3 — a spared husk and a spared stage 5 inside the reach keep their health AND ARE NOT PULLED; a control proves the probe sees a velocity (SURVIVAL) — supersedes BL4R2

| prediction | stands on | READING |
|---|---|---|
| (1) Before: `PROBE tag=bl4h <uuid> husk customHP=100.000/100.000 fireTicks=0 dead=false d=7.280 dy=0.000`, `PROBE tag=bl4p <uuid> creeper customHP=160.000/160.000 fireTicks=0 dead=false d=7.280 dy=0.000`, `MOTION tag=bl4h <uuid> vx=0.0000 vy=0.0000 vz=0.0000`, `MOTION tag=bl4p <uuid> vx=0.0000 vy=0.0000 vz=0.0000`. (2) **THE CONTROL:** `MOTION tag=bl4c <uuid> vx=0.0000 vy=0.0000 vz=0.5000`. (3) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl4x uuid> stage=1 gs=100 base=60.000 reach=6.00 pull=1.25 visual=void_implosion victims=0 [] spared=2 [<both, either order: husk <the bl4h uuid> d=2.000 factor=0.555556, creeper/phage <the bl4p uuid> d=2.000 factor=0.555556>]`. (4) **In the blast's own tick**, both MOTION lines `vx=0.0000 vy=0.0000 vz=0.0000`: **not pulled.** Had the pull reached them at 2 blocks its strength would be 0.833333 (jshell), far above the 0.0001 the probe resolves. (5) Twelve ticks later the two PROBE lines unchanged. (6) No GROWTH. (7) `ENTITIES CREEPER count=1 radius=16` | I5, I7; BL4R2's reading | |

### BL5R3 — backing away cancels the fuse: nothing blows (SURVIVAL) — supersedes BL5R2

| prediction | stands on | READING |
|---|---|---|
| (1) `FUSE tag=bl5 <uuid> fuse=10/30 ignited=false waited=<recorded>`. (2) After the step and five ticks, `FUSE tag=bl5 <uuid> fuse=<n>/30 ignited=false`, **n < 10**. (3) `ENTITIES CREEPER count=1 radius=16`. (4) Two ticks later, `CMD /kill @e[tag=bl5]`, `SETTLED dying=0 ticks=<t> first=1`, `ENTITIES CREEPER count=0 radius=16`. (5) No BLAST, no HURT. (6) No SPLIT, no GROWTH | BL5R2's reading; **one step added**, `wait: 2` before the kill (the brief: the two-tick wait before a kill after a probe). The fuse is still vanilla's swell (Part C stopped) | |

### BL6R3 — just inside the reach (5.90): a sliver, and a pull of one forty-eighth (SURVIVAL) — supersedes BL6R2

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl6 <uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl6 uuid> stage=1 gs=100 base=60.000 reach=6.00 pull=1.25 visual=void_implosion victims=1 [player <Ben's uuid> d=5.900 factor=0.008472 dealt=0.508 knock=0.016667 res=<R> h=5.900 ground=true m=0.546000 strength=<0.020833 × (1 − R)> cap=2.451600 pull=<the same as strength> capped=false] spared=0 []`. (3) **Exactly one** HURT line `before=0.508`, `dealer=<the bl6 uuid>` | jshell from `d = 5.9000000000000060` (BL6's measured start): factor 0.008472, dealt 0.508, knock 0.016667, strength 0.020833, cap (5.9 − 0.5) × 0.454 = 2.451600 | |

### BL7R3 — just beyond the reach (6.05): neither hit nor pulled (SURVIVAL) — supersedes BL7R2

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl7 <uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl7 uuid> stage=1 gs=100 base=60.000 reach=6.00 pull=1.25 visual=void_implosion victims=0 [] spared=0 []`. (3) **No HURT line.** BL6R3 is the control | BL7R2's reading | |

### BL8R3 — flint and steel's route still cannot light a Phage (SURVIVAL) — supersedes BL8R2

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE ignite tag=bl8 <uuid>`, then `[Rpg] CREEPERGUARD ignite cancelled mob=phage id=<the bl8 uuid>`. (2) Forty ticks later `FUSE tag=bl8 <uuid> fuse=0/30 ignited=false`. (3) `ENTITIES CREEPER count=1 radius=16`. (4) No BLAST | BL8R2's reading, the steps unchanged | |

### BL9R3 — at half the reach (3.00): three eighths of 60, a pull the cap lets through WHOLE, and his feet 1.38 nearer (SURVIVAL) — supersedes BL9R2

| prediction | stands on | READING |
|---|---|---|
| (1) Before: `SELF customHP=<M>/<M> at=<x0> <y0> <z0> onGround=true fall=0.000`, x0 and z0 block centres (`.50`). (2) `DRIVE detonate tag=bl9 <uuid>`. (3) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl9 uuid> stage=1 gs=100 base=60.000 reach=6.00 pull=1.25 visual=void_implosion victims=1 [player <Ben's uuid> d=3.000 factor=0.375000 dealt=22.500 knock=0.500000 res=<R> h=3.000 ground=true m=0.546000 strength=<0.625 × (1 − R)> cap=1.135000 pull=<the same as strength> capped=false] spared=0 []`. (4) **Exactly one** HURT line `before=22.500`. (5) Forty ticks later `SELF … at=<x1> <y0> <z0> onGround=true fall=0.000` with **x1 = x0 + 1.38 ± 0.10** (R = 0: travel 0.625 / 0.454 = 1.376652, jshell), **x1 never beyond x0 + 2.50 + 0.10** (the Phage's x0 + 3, less the margin), **never below x0**; z and y unchanged within ± 0.10 | I1-I4, I10; jshell: falloff(3) 0.375, cap 2.5 × 0.454 = 1.135000 | |

---

## THE IMPLOSION'S OWN ROWS — SURVIVAL, DIFFICULTY NORMAL

### IM1 — the reach's edge at STAGE 5: 5.90 is inside (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 5, 160 HP, GS 100 given)" [aqua, gray]`. (1) `DRIVE detonate tag=im1 <uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the im1 uuid> stage=5 gs=100 base=150.000 reach=6.00 pull=1.25 visual=void_implosion victims=1 [player <Ben's uuid> d=5.900 factor=0.008472 dealt=1.271 knock=0.016667 res=<R> h=5.900 ground=true m=0.546000 strength=<0.020833 × (1 − R)> cap=2.451600 pull=<the same> capped=false] spared=0 []`: **the same reach as stage 1** (BL6R3), the damage stage 5's. (3) **Exactly one** HURT line `before=1.271` | jshell: 150 × 0.008472222 = 1.270833. **The reach is 6 at every stage until the seat rules Part C** (header) | |

### IM2 — the reach's edge at STAGE 5: 6.05 is outside (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=im2 <uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the im2 uuid> stage=5 gs=100 base=150.000 reach=6.00 pull=1.25 visual=void_implosion victims=0 [] spared=0 []`. (3) **No HURT line.** IM1 is the control | BL7R3, at stage 5 | |

### IM3 — the cap binds at 2.00: 0.833333 cut to 0.681000, and he slides 1.50 east, stopping half a block short (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (1) Before: `SELF customHP=<M>/<M> at=<x0> <y0> <z0> onGround=true fall=0.000`. (2) `DRIVE detonate tag=im3 <uuid>`. (3) **Exactly one** `[Rpg] BLAST mob=phage id=<the im3 uuid> stage=1 gs=100 base=60.000 reach=6.00 pull=1.25 visual=void_implosion victims=1 [player <Ben's uuid> d=2.000 factor=0.555556 dealt=33.333 knock=0.666667 res=<R> h=2.000 ground=true m=0.546000 strength=<0.833333 × (1 − R)> cap=0.681000 pull=0.681000 capped=true] spared=0 []` **for R < 0.1828** (where the strength stays above the cap; jshell, 1 − 0.681/0.833333); at a larger R, `pull=` is the strength and `capped=false`. (4) **Exactly one** HURT line `before=33.333`. (5) Forty ticks later `SELF … at=<x1> <y0> <z0>`: **x1 = x0 + 1.50 ± 0.10** (0.681 / 0.454 = 1.500, jshell), **never beyond x0 + 1.60** (the Phage's x0 + 2, less the margin, plus I10), never below x0 | I2-I4, I10; ImplosionTest's d = 2 row | |

### IM4 — almost at the centre: within half a block nothing is pulled, so he is not thrown through (SURVIVAL) — new

**The staging, and its one hazard, stated:** the Phage is spawned 0.30 east of his feet, inside his own box (a stage-1
Phage is 0.353 wide, he is 0.6: the boxes overlap below d = 0.476471, jshell). **His client may push him out of the
overlap during the five ticks before the blast (INFERRED, vanilla collision), but never beyond 0.476471**, where the
boxes stop touching: so the blast finds him at D in [0.300, 0.477], **inside the 0.5 margin either way**, and the cap is
0.

| prediction | stands on | READING |
|---|---|---|
| (1) Before: `SELF … at=<xa> <y0> <z0>`, xa within [x0 − 0.18, x0] (the push-out, if any, is west). (2) `DRIVE detonate tag=im4 <uuid>`. (3) **Exactly one** `[Rpg] BLAST mob=phage id=<the im4 uuid> stage=1 gs=100 base=60.000 reach=6.00 pull=1.25 visual=void_implosion victims=1 [player <Ben's uuid> d=<D> factor=<falloff(D)> dealt=<60 × F> knock=<1 − D/6> res=<R> h=<D> ground=true m=0.546000 strength=<k × 1.25 × (1 − R)> cap=0.000000 pull=0.000000 capped=true] spared=0 []`, **D in [0.300, 0.477]**, so dealt in [53.042, 55.575] (jshell). (4) **Exactly one** HURT line `before=<that dealt>`. (5) Forty ticks later `SELF … at=<xb> <y0> <z0>` with **xb = xa ± 0.10**: not moved by the pull, and never carried past the Phage's spot | I3, I10; `ImplosionTest.aVictimAlmostAtTheCentreIsNotThrownThrough` | |

---

## THE KNELL ON A SLAB — F19 — SURVIVAL, DIFFICULTY NORMAL

### KS1 — a hit on a player standing on a slab, then a landing on the floor beside him (SURVIVAL) — new

**The staging:** the teleport rows' (`GATE-dragon-knell-5.md`): his feet F0 at a block centre, a stone floor 23 × 21,
air 8 high, a FROZEN Knell spawned 8 east at K = F0 + (8, 0, 0); then he is lifted half a block and a bottom
`smooth_stone_slab` placed in the block his feet were in, so he stands ON it at **F = F0 + (0, 0.5, 0)**.
`teleport_draw: right 50` fixes the decision, so the landing is exact.

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<F.x> <F0.y + 0.50> <F.z> onGround=true fall=0.000`: **he stands on the slab.** (2) `AT tag=ks1 <uuid> x=<K.x> y=<K.y> z=<K.z>` (K.y = F0.y). (3) `DRIVE teleport_draw tag=ks1 <uuid> side=right first=50.000`, `DRIVE beam tag=ks1 <uuid>`, the beam's TELEGRAPH and a FIRE line **that hits** him, and **exactly one** HURT line `before=65.000`. (4) **Exactly one** `[Rpg] TELEPORT … tick=<FIRE + 24> from=<K> center=<F.x> <F0.y + 0.500> <F.z> radius=8.000 bearing=0.000 side=right first=50.000 tried=1 [right 50.000 ok y=<F0.y> at=<F.x + 5.142> <F0.y> <F.z + 6.128>] landed=<the same> moved=true`: **the candidate's y is the FLOOR, F0.y, half a block below the centre's.** (5) After the wait, `AT tag=ks1 <uuid>` at `<F.x + 5.142> <F0.y> <F.z + 6.128>`, three places. **Before the fix the same staging read every one of the ten candidates `no-footing blocked=<n> ungrounded=<n>` and `moved=false`** (MUTF19A in core) | I11; KB10R6's reading (`… tried=1 [right 50.000 ok y=63.000 at=-95.358 63.000 12.628] landed=… moved=true`), the same geometry on a floor F0.y | |

---

## THE SPLIT ROWS — SURVIVAL, DIFFICULTY NORMAL

**Before each SPLIT line, exactly two `CHILD parent=<that SPLIT's parent uuid> child=<uuid> frozen=true`. A kill step
logs `CMD /kill @e[tag=<t>]` and then `SETTLED dying=0 ticks=<t> first=1`.**

### DS1R5 — split by his emberblade at gear score 60; thawed; each child blows on him and PULLS him; a grown one's GROWTH distance EQUALS its sibling's spared distance (SURVIVAL) — supersedes DS1R4

| prediction | stands on | READING |
|---|---|---|
| (1)-(5) as DS1R4 read them: `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`, `REPLY "CREEPER damaged: 1/96 custom HP" [green]`, `DRIVE attack Player#attack target=<the ds1 uuid> creeper d=3.000 charge=1.000`, two `CHILD … frozen=true`, **exactly one** `[Rpg] SPLIT mob=phage parent=<the ds1 uuid> stage=5 gs=60 children=2 childStage=1 [<A> dx=-0.50, <B> dx=0.50]`, `ENTITIES CREEPER count=2 radius=16`, two stage-1 BODY lines `maxHP=48.000 gs=60`, `REPLY "Custom HP: <M>/<M>" [green]`, two `DRIVE thaw tag=ds1 <A or B> ai=true`. (6) **Within the 200 ticks, exactly two BLAST lines, `id=<A>` and `id=<B>`, either order, X then Y.** X: `stage=1 gs=60 base=36.000 reach=6.00 pull=1.25 visual=void_implosion victims=1 [player <Ben's uuid> d=<D1> …] spared=<n> […]`, n = 1 naming `creeper/phage <Y> d=<d> factor=<falloff(d)>` exactly when Y stood within 6 of X; **if n = 1**, exactly one `[Rpg] GROWTH mob=phage id=<Y> from=1 to=2 fed=creeper/phage <X> element=void d=<d> max=60`, its `d` printed IDENTICALLY to X's spared entry's, and Y's BLAST `stage=2 gs=60 base=49.500 …`; **if n = 0**, no GROWTH and Y's `stage=1 gs=60 base=36.000 …`. Y's: `victims=1 [player <Ben's uuid> d=<D2> …] spared=0 []`. Each pull field by the rules above. **The bounds: D1 < 3** (the first blast after a heal); **D2 ≤ 7** (he was pulled, and a swell already begun blows wherever he is inside 7). (7) **Exactly two** HURT lines, `before=` each `dealt`, dealer X then Y. (8) `ENTITIES CREEPER count=0 radius=16`. (9) One SPLIT; no `CREEPERGUARD prime` | DS1R4's reading and its dated diagnosis (D2 = 3.916 after a push); I1-I6. **The timing depends on vanilla's AI** (INFERRED) | |

### DS2R5 — a stage 5 killed by `/kill` splits; its children are born frozen (SURVIVAL) — supersedes DS2R4

| prediction | stands on | READING |
|---|---|---|
| `CMD /kill @e[tag=ds2]`, the two `CHILD parent=<the ds2 uuid> … frozen=true`, **exactly one** SPLIT `parent=<the ds2 uuid> stage=5 gs=300 children=2 childStage=1 [<uuid> dx=-0.50, <uuid> dx=0.50]`, `SETTLED dying=0 ticks=<t> first=1`, `ENTITIES CREEPER count=2 radius=16`, **exactly two** BODY lines `tag=ds2`, each stage 1 `maxHP=240.000 gs=300`. **No BLAST, no HURT** | DS2R4's reading, the steps unchanged | |

### DS5R5 — once only: two after the split, still two forty ticks later (SURVIVAL) — supersedes DS5R4

| prediction | stands on | READING |
|---|---|---|
| The two `CHILD … frozen=true`, **exactly one** SPLIT (`parent=<the ds5 uuid> stage=5 gs=300 children=2 childStage=1 …`), and `ENTITIES CREEPER count=2 radius=16` **twice**. **No BLAST, no HURT** | DS5R4's reading, the steps unchanged | |

### DS6R5 — the row-start clean removes DS5R5's children (SURVIVAL) — supersedes DS6R4

| prediction | stands on | READING |
|---|---|---|
| `CLEAN blocks=756 removed=2 inventory=kept …`, `LEFTOVER living=0 radius=16`, `LINGER work=0 entities=0 radius=48`, `ENTITIES CREEPER count=0 radius=16` | DS6R4's reading; DS6R5 directly after DS5R5 | |

### DG6R5 — a stage 5's death grows the bystander to 3 (SURVIVAL) — supersedes DG6R4

| prediction | stands on | READING |
|---|---|---|
| (0) The two `CHILD parent=<the dg6p uuid> … frozen=true`. (1) **Exactly one** SPLIT `parent=<the dg6p uuid> stage=5 gs=300 children=2 childStage=1 […]`. (2) **Exactly one** `[Rpg] GROWTH mob=phage id=<the dg6b uuid> from=2 to=3 fed=creeper/phage <the dg6p uuid> element=void d=2.000 max=360`. (3) A stage-3 BODY for dg6b (`scale=1.176 height=2.000 speedBase=0.2500 speedType=0.2500 speedX=1.000 maxHP=360.000 gs=300`). (4) Two stage-1 BODY lines for dg6p, `maxHP=240.000 gs=300`. (5) `ENTITIES CREEPER count=3 radius=16`. (6) No BLAST, no HURT | DG6R4's reading, the steps unchanged | |

### DG9R5 — a Phage grown to 5, then killed, splits into two frozen children (SURVIVAL) — supersedes DG9R4

| prediction | stands on | READING |
|---|---|---|
| **Exactly one** `[Rpg] GROWTH mob=phage id=<the dg9 uuid> from=4 to=5 fed=enderman <uuid> element=void d=2.000 max=480`, then a stage-5 BODY for dg9. Two ticks later its kill: the two `CHILD parent=<the dg9 uuid> … frozen=true`, **exactly one** SPLIT `parent=<the dg9 uuid> stage=5 gs=300 children=2 childStage=1 […]`, two stage-1 BODY lines for dg9 (`maxHP=240.000 gs=300`), `ENTITIES CREEPER count=2 radius=16`. No BLAST, no HURT | DG9R4's reading; **one step added**, `wait: 2` between the probe and the kill | |

---

## BEN'S ROW — BY THE ASK STEP. EACH ANSWER IS ONE WORD

### BQ1R3 — the swirl, the pull, and the pull against the old push, by his own eyes (SURVIVAL) — supersedes BQ1R2

A frozen stage-1 Phage is blown exactly 2 blocks east of him; he is put back, healed, and a frozen stage 5 is blown at
the same spot. **33.333 and 83.333 before his defence**; each pulls him **0.681000** (capped, R = 0), so he slides
**1.5 blocks** toward where it stood. **Each question waits 1200 ticks for his next chat line; his answer cell is his
line, verbatim.** The brief's fourth question, *does the small Phage still feel like a threat with its shorter
distances*, **is NOT asked: the shorter distances are not built** (Part C stopped; header).

| prediction | READING |
|---|---|
| Beside the answers, recorded: the small Phage's BLAST `stage=1 gs=100 base=60.000 … d=2.000 factor=0.555556 dealt=33.333 knock=0.666667 res=<R> h=2.000 ground=true m=0.546000 strength=<…> cap=0.681000 pull=<…> capped=<…>` and its HURT `before=33.333`; the big one's `stage=5 gs=100 base=150.000 … dealt=83.333 …`, **the same pull fields**, and HURT `before=83.333` | |
| `ASK 1 question="Did you see black and dark purple particles swirl in to where the Phage stood?" answer="<his line>" ticks=<t>` | |
| `ASK 2 question="Did the blast pull you toward where it stood?" answer="<his line>" ticks=<t>` | |
| `ASK 3 question="The old Phage pushed you away. Did this pull feel a little stronger than that push?" answer="<his line>" ticks=<t>`. **The seat should expect "no"** (header, question 2; PLAN, *THE PULL TRAVELS LESS THAN THE PUSH DID*) | |

"not sure", or no line within the minute (`answer=NOT STATED after 1200 ticks`), is recorded as NOT STATED and never
re-asked in this boot.

---

## THE ROWS ALREADY MERGED — THE AUDIT (2026-11-17)

**The question asked of every merged selftest row:** does it stage a live or exploding Phage, or a Knell teleport, and
if so, what do this branch's two changes (the pull in place of the push and the new BLAST line; the landing anchor)
change in its lines? **Instrument:** `grep -n "phage" paper/src/main/resources/selftest/GATE-*.yml`, and every row of
`GATE-dragon-teleport.yml` and `GATE-dragon-blink.yml` read for its target's and its Phalanx's footing.

| file | rows | what changes | done |
|---|---|---|---|
| `GATE-dragon-blast.yml` | all 16 | every BLAST line's head (`knockback=1.00` → `pull=1.25 visual=void_implosion`) and victim entries (the push → the pull, and `h=`); a pulled player stands nearer, not farther; BL4's "not pushed" is "not pulled". Rows that blow nothing (BL5, BL8, DS2, DS5, DS6, DG6, DG9) have no line that changes | **ALL RE-PREDICTED** here, the next suffix; BL5 and DG9 gain a `wait: 2` before their kills |
| `GATE-dragon-bodies.yml`, `-split.yml`, `-growth.yml` | DB2R2-DB4R2, DS3R2, DS4R2, DG1R2-DG8R2 | none: every Phage spawned `frozen` (`SelfTestScenariosTest.noScenarioOutsideTheBlastGateLeavesALivePhage`, its control the four live spawns above), so none swells or blows; their growth and split lines carry no push | **unchanged, not re-run** |
| `GATE-dragon-shot.yml` | SH9R2 | a frozen stage-1 Phage, never blown | **unchanged** |
| `GATE-dragon-teleport.yml`, `GATE-dragon-beam.yml` | KB10R6, TP2R5-TP14R4, KB11R6, TQ1R5 | **none:** every target stands on the whole-block stone floor, so its feet y is whole, and floor(y) = y (with the snap, a y within 1e-6 below a whole number anchors on it: `floatingPointDriftJustBelowAFloorAnchorsOnThatFloorNotTheBlockBelow`). Every candidate's searched heights are the ones the rows read | **unchanged, not re-run** |
| `GATE-dragon-blink.yml`, `GATE-dragon-skreel.yml` | BB1-BB5, PB1R2-PB4 | **none:** `MobShooter` already passes `MobBlink.anchor` (now the shared one, the same floor and snap) | **unchanged** |
| the 13 closed manual gates that spawn live dragon mobs | — | a live Phage's blast now pulls | **carried** (PLAN §8), not re-predicted |

---

## WHAT NO ROW CAN STAGE — UNIT-ONLY, AND WHY

- **The fuse, at any distance of ours** (start, reset, detonate, the swell state): **not built** (Part C stopped).
- **The reach at stages 1 and 3 differing from 5:** not built. IM1 and IM2 read the stage-5 edge, BL6R3 and BL7R3 the
  stage-1 edge, and both are 6.
- **A victim in the air at the blast** (`ground=false`, `m=0.910000`, the tighter cap): no step lifts him at a tick.
  `ImplosionTest` walks it, and `MobBlastLineTest.anAirborneVictimIsCappedByTheAirsWeakerDrag` pins its line.
- **The pull at 0, a resistance other than Ben's, a creative victim, a victim exactly at the reach:** as
  `GATE-dragon-blast-2.md`; `ImplosionTest.aBadShapeIsRefusedByName` (0 allowed),
  `theResistanceCutsTheStrengthBeforeTheCap`.
- **The shape's second use (an ability):** no ability ships one (the brief). The test-only fixture
  `fixtures/abilities/void_pull_fixture.yml` is loaded by `AbilityLoaderTest` and applied by `ImplosionEffectTest`
  (it pulls mobs, deals nothing); the handle's exemptions (a shot's carrier, creative, spectator) by
  `DragonBlastWiringTest.theHandlesPullExemptsAShotACreativeAndASpectatorAndUsesTheSameFooting`, which reads lines,
  not behaviour.
- **A Knell landing on a slab's top:** never tried, by design (the stated cost; `theCostOfTheAnchorASlabTopIsNeverTried`).
- **A target mid-jump when the move falls due:** no step can time a jump to the tick; core's mid-jump row is the
  witness.
