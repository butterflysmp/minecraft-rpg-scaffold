# GATE — THE VOID IMPLOSION: THE PHAGE'S OWN FUSE, ITS DISTANCES BY STAGE, A PULL WITH THE PUSH'S LIFT, AND THE KNELL ON A SLAB (F19)

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/void-implosion`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

**2026-11-17: BOOTED at `6d079ead` on the seat's approval and Ben's "ready", and READ. NOT 26/26: 19 rows AS PREDICTED
(IM3 at its tolerance's edge), 7 NOT AS PREDICTED (BL6R3, FZ1: a predicted floating-point half; BL9R3, FZ3, FZ5: the
rest point short; IM4: pushed out 0.01 further than bounded, and ended 0.41 west; DS1R5: the blanket `h = d` clause).
Readings beside each prediction; the account under THE BOOT OF 2026-11-17 at the end. No prediction was edited.**

> **2026-11-17, REWRITTEN BEFORE ANY BOOT, AND WHY.** This gate was first committed at `bf3bf2d9` with Part C (the
> Phage's own fuse and its distances by stage) STOPPED at the jar and the pull flat and capped on the ground. **It was
> never booted and no row of it was read.** The seat then ruled (its two rulings on #204): **Part C by route (b)**, our
> own fuse with the swell shown by packet, the distances by stage; and **the pull gets the push's lift**, the cap on the
> modelled rest point. Both change what every blast row reads: the reach is now the STAGE's (stage 1's is 2, so the old
> stage-1 rows at 3.00 and 5.90 no longer reach him), the BLAST line gains `lift`, `tf`, `apex`, `rest` and `safe`, and
> a live Phage now lights by our rules. **So every prediction is rewritten here, in place**, under the same row names (none
> was read), and the rows the first brief asked for and the stop left out are added: the fuse at stages 1, 3 and 5
> (FZ1, FZ3, FZ5), the reach's edge per stage (BL6R3/BL7R3 at 1, IM5/IM6 at 3, IM1/IM2 at 5), the swirl's width per stage,
> each pulled row's rest position, and Ben's four questions. `bf3bf2d9`'s text stays in git as the record.

**What this gate re-predicts.** Every row of `selftest/GATE-dragon-blast.yml` is a NEW row named for the
`GATE-dragon-blast-2.md` row it supersedes, with the next suffix (BL1R2 → BL1R3, DS1R4 → DS1R5, BQ1R2 → BQ1R3), plus
**IM1-IM6**, **FZ1, FZ3, FZ5** and **KS1**. The old rows' predictions and readings stay in `GATE-dragon-blast-2.md`,
under a dated note. The file keeps its name, so the run sheet's command is unchanged.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL; a creative or spectator player is not a Phage's
  target (`MobHostility`), so a fuse would never light.
- **The difficulty is set by the selftest:** the setup's `difficulty: normal`, `DIFFICULTY found=… set=NORMAL`, and
  `RESTORED … difficulty=<what it found>`.
- **Gear scores, per row:** 100, except **60** for BL2R3, FZ3, FZ5, DS1R5 and BQ1R3's big Phage (every LIVE stage 3 or 5,
  so at its feet it deals at most 63 or 90), and 300 for the split rows DS2R5, DS5R5, DG6R5, DG9R5. A Phage's
  zero-distance damage is `60 + 22.5 × (stage − 1)` × g / 100 (`MobBlast.price`, P-b): stage 1 at 100, 60.000; stage 3 at
  100, 105.000, at 60, 63.000; stage 5 at 100, 150.000, at 60, 90.000. KS1's Knell at 100: **65.000** a hit.

## THE DISTANCES BY STAGE, AS COMPUTED (`StagedDistanceTest`, `MobBlastTest`)

| stage | height | trigger (lights STRICTLY inside) | cancel (resets STRICTLY beyond) | reach (blast, pull, swirl) |
|---|---|---|---|---|
| 1 | 1.0 | 1.000 | 2.333 (7/3) | 2.000 |
| 2 | 1.5 | 1.500 | 3.500 | 3.000 |
| 3 | 2.0 | 2.000 | 4.667 (14/3) | 4.000 |
| 4 | 2.5 | 2.500 | 5.833 (35/6) | 5.000 |
| 5 | 3.0 | 3.000 | 7.000 | 6.000 |

Each is the last stage's authored number (3, 7, 6) times the stage's height over 3. The fuse is 30 ticks at every stage.

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC, ROW BY ROW

**The floor:** his maximum is at least 100 (`CombatantStats.DEFAULT_PLAYER_BASE`; INFERRED that no shipped modifier
lowers it). **Ben's own 350 is NOT relied on.** Every row that can hit him first runs `rpg heal 1000000`. The figure is
the most the row can deal **before his defence** after that heal. **A stage 5 at gear score 100 deals 150 at its feet:
the only stage 5s at 100 here are FROZEN and blown at 2.00 or farther** (IM1 5.90, IM2 6.05, IM3 2.00, BL9R3 3.00, BL4R3
7.28); **every LIVE stage 3 or 5 is at gear score 60**, so a live one that reached his feet would deal 63 or 90. Each
figure EXECUTED in jshell (scratch `gate2.jsh`).

| row | what hits him after one heal | the worst case, before defence |
|---|---|---|
| BL1R3, BL5R3 | a LIVE stage 1, GS 100 | 60 × 1 = **60** |
| BL2R3 | a LIVE stage 5, GS 60 | **90** |
| BL6R3 | a frozen stage 1 at 1.95 | 60 × 0.012813 = **0.769** |
| BL9R3 | a frozen stage 5 at 3.00 | 150 × 0.375 = **56.250** |
| IM1 | a frozen stage 5 at 5.90 | 150 × 0.008472 = **1.271** |
| IM3 | a frozen stage 5 at 2.00 | 150 × 0.555556 = **83.333** |
| IM4 | a frozen stage 1 at 0.300 to 0.477 | 60 × 0.786250 = **47.175** |
| IM5 | a frozen stage 3 at 3.95 | 105 × 0.006328 = **0.664** |
| FZ1 | a live stage 1, GS 100 | **60** (staged: 24.019 at 0.95) |
| FZ3 | a live stage 3, GS 60 | **63** (staged: 24.417 at 1.95) |
| FZ5 | a live stage 5, GS 60 | **90** (staged: 34.503 at 2.95) |
| KS1 | one forced Knell beam | **65** |
| DS1R5 | two thawed children, ONE heal | **36.000 + 49.500 = 85.500** |
| BQ1R3 | a live stage 5 at GS 60 (staged 2.5: 41.562), then after a SECOND heal a live stage 1 (staged 0.8: 28.800) | **90**, then **60** |

BL3R3, BL4R3, BL7R3, IM2 and IM6 place him beyond every blast's reach; BL8R3 and the split rows blow nothing near him.
**THE FALL:** the pull's lift is the push's own, never scaled (the seat's ruling 2). Its apex is under 2.5 blocks
wherever the victim stands farther than about 1.25 from the Phage (stage 5, jshell: d = 1.3, 2.449; d = 2, 1.250), and
above vanilla's 3-block safe fall inside about 1.08 (d = 1, 3.200). **Wherever the modelled apex reaches 2.5 the
victim is given the safe-landing mark** (the dash's, ruling 31; `Implosion.SAFE_FALL_MARGIN`), so no fall can hurt him:
IM4 (apex 3.380 to 4.298) and any live stage 1 that blows within about 0.6. **No second HURT line is predicted in any
row; one would be a fall, named, and the row NOT AS PREDICTED. Every SELF line read after a pull reads `fall=0.000`**
(read 40 or more ticks after the blast, so after the landing that resets it: a reading that cannot show a fall that
hurt him; the HURT line is the witness of that).

## WHAT THE ROWS STAND ON — EACH FACT, AND ITS MARK

- **I1. The swell's index and type, READ.** `ClassTreeIdRegistry#define` numbers a class's synched data after its
  superclass's last; counted in the pinned jar (`javap -c -p -cp run/versions/26.1.2/paper-26.1.2.jar
  net.minecraft.world.entity.<Class>`, each `static {}`): `Entity` 8, `LivingEntity` 7, `Mob` 1, `PathfinderMob` and
  `Monster` 0, then `Creeper.DATA_SWELL_DIR` first: **16**, an `INT` (`EntityDataSerializers.INT`, registered second).
  PacketEvents 2.13.0's `EntityDataTypes.INT` has id **1** at V_26_1 in its own `entity_data_serializer` mapping
  (EXECUTED from its pinned jar, `pe3.jsh`), and its list holds 43 serializers, as the server registers 43. **R0e reads
  the startup line that names both.**
- **I2. Vanilla's swell is gone, READ in our code** (`PhageFuser.onAppear`, pinned by `PhageFuseWiringTest`): the goal is
  removed at every appearance (a creeper read from disk gets a fresh one from `Creeper#registerGoals`, READ) and ours
  claims `MOVE` at vanilla's priority 2. **The server's own direction stays −1** (only `SwellGoal#tick` or an ignited
  `Creeper#tick` sets it, READ), so `swell` never rises and vanilla never calls `explodeCreeper`.
- **I3. Our fuse's rules** (`MobFuse`, `MobFuseTest`): the swell moves first, +1 or −1 floored at 0, and at 30
  DETONATES; then a down fuse lights when its target is STRICTLY inside the trigger with sight (vanilla's `< 9.0`), a lit
  one resets when the target is STRICTLY beyond the cancel, out of sight, or gone (`> 49.0`). **So a START logged at tick
  T is followed by `FUSED … tick=T+30`, and a RESET logged at tick T2 carries `swell=T2−T1`.**
- **I4. The target is the Phage's own** (`Mob#getTarget`, vanilla's `NearestAttackableTargetGoal` for a player), only
  when it is a combatant the Phage is hostile to and the Phage has AI. **That the target is set within 40 ticks is
  INFERRED** (vanilla's target goal re-checks on a random interval); each fuse row reads `TARGET … target=self` first, and
  a row that reads otherwise is NOT AS PREDICTED from there on.
- **I5. Every viewer**: each START and RESET is sent to `getTrackedBy()` and logs `viewers=<n>`: here, Ben alone, **1**. A
  new viewer of a lit Phage is sent +1 a tick later (`PlayerTrackEntityEvent` fires BEFORE `ServerEntity#addPairing`,
  READ); no row stages a second viewer (the selftest refuses a second player).
- **I6. A Phage blows ONE way**: our fuse's end and the instrument's DETONATE both call `PhageFuser.detonate`, which marks
  the prime and raises it; the handler blows only a marked prime. **A prime not raised there logs `CREEPERGUARD prime
  refused`, and NONE is predicted anywhere.**
- **I7. The pull, READ and EXECUTED** (`Implosion`, `ImplosionTest`): the old push was `k = (1 − d/R) × (1 − res)` along
  the victim's EYE minus the Phage's feet (F-B). Its horizontal part `k × h / len`, times **1.25**, toward the Phage; its
  vertical part `k × (eyeY − feetY) / len`, times **1** (the lift). The eye is 1.62 above a player's feet.
- **I8. The flight model, READ** (`LivingEntity#travelInAir`, `Attributes.GRAVITY` 0.08, `Block#updateEntityMovement
  AfterFallOn`, `BlockBehaviour$Properties` 0.6f): each tick move, then the horizontal × `(onGround at the tick's START ?
  0.6f : 1.0f) × 0.91f` and the vertical `(vy − 0.08) × 0.98f`; landing zeroes the vertical. `tf` is the rest per unit of
  horizontal speed; the horizontal is capped at `(h − 0.5) / tf`; **`rest = pull × tf`**. Where the cap does not bind on
  stone, `rest` is exactly 1.25 × the old push's modelled travel (`ImplosionTest.theTravelAtTwoThreeFourAndFiveBesideThe
  OldPush`). **That the player's client runs the same travel is INFERRED.**
- **I9. The resistance:** Ben's `EXPLOSION_KNOCKBACK_RESISTANCE`, read at the blast; RECORDED (every blast of 2026-11-17
  read `res=0.000`). **Every figure below is stated for R = 0;** a non-zero R scales both parts by `(1 − R)` and the row is
  re-derived from the printed `res`, not NOT AS PREDICTED.
- **I10. A frozen mob cannot be moved, READ** (F18): BL4R3 reads "not pulled" in VELOCITY, with its control.
- **I11. The swirl is drawn at the stage's reach**, one `SWIRL` line per spiral step (two: black, then purple), and it is
  pending work for 10 ticks, so every row that blows waits 12 before it ends.
- **I12. His feet after a pull: ± 0.10 blocks** on x, CHOSEN, not measured (`SELF at=` prints two places; the rest is
  I8's INFERRED client half). **Never past:** at most the Phage's x − 0.5 + 0.10. Read 40 or more ticks after the blast
  (the slide is under 0.001 a tick by then: 0.546^20, jshell).
- **I13. The slab (KS1)** (`BeamTeleport.foot` anchors on floor(y), MUTF19A red): a target at y0 + 0.5 is searched from y0.
- **S10 (carried):** `d` three places, `factor` six, `dealt` three: factor within 0.000125, dealt within 0.0005 + base ×
  0.0000005, knock within 0.0000839; **the pull's printed six-place figures within 0.000005 of the row's** (the row's
  are from the exact staged distance, which every frozen row prints exactly).

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-void-implosion"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   If the folder exists, add `-2`. Report the folder, both pairs of counts, and `diff -rq lines: 0` for each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **Boot this branch's head with `--refresh-content`:** `./scripts/dev-server.sh --refresh-content`, announcing
  "booting now", on Ben's "ready". **REQUIRED:** the deployed `phage.yml` still carries `knockback: 1.0` and no
  `trigger`/`cancel`/`lift`; the loader refuses it by name (`MobLoaderTest.aMalformedBlastIsRefused`), and no Phage loads.
5. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` names this branch's head; readings count only
  after Ben's LOGIN.
6. **Tell Ben to wait for the word that R0 passed before he logs in.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **Wait for my word that R0 passed before you log in.**
1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest dragon-blast` — **ONCE. Run it one time only, even after it finishes.** It takes about **two
   minutes**, plus the time you take to answer (1682 ticks of waits, about 84 seconds, plus each row's staging; 26 rows).
   Stand still and touch nothing, **except when a gold question appears in chat**: then type your answer, `yes` or `no`,
   in chat. There are **four** questions. **Some blasts will throw you up a little and pull you toward them; that is
   expected. Stand still again afterwards.** Wait for the chat to say the run finished, and answer every gold question
   before that.
3. `/stop`

Nothing else this boot.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/void-implosion` prints it, shortened. **Not** master's `f6a596ea`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **AS PREDICTED, 2026-11-17.** `[22:26:38] [Server thread/INFO]: [Rpg] Build: 6d079ead`; `git ls-remote origin refs/heads/feat/void-implosion` printed `6d079ead965bcc2f2b5ec3774dd869a0a60e4f47` |

### R0b — the jar carries Part C, the lift and F19

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/mobs/phage.yml` lines `  trigger: 3`, `  cancel: 7`, `  lift: 1.0`, `  pull: 1.25`, and ABSENT `  knockback: 1.0` (the control); `content/visuals/void_implosion.yml` with `  - type: spiral`; `PhageFuser.class` carrying `SWELL mob=` and `FUSED mob=`; `MobBlaster.class` carrying `lift=%.2f` and `rest=%.3f`, ABSENT the text control `NoSuchTextControl`; the entries `io/github/butterflysmp/rpg/core/mob/MobFuse.class` and `.../core/shape/Implosion.class`; `BeamTeleport.class` carrying `ANCHOR_SNAP`; in `selftest/GATE-dragon-blast.yml` the keys `FZ5:`, `IM6:` and `KS1:` PRESENT and `BL1R2:` ABSENT; `dragon-bodies DB11R2:` PRESENT (the key control) | the scan below | **AS PREDICTED, 2026-11-17.** `PRESENT phage.yml '  trigger: 3'`, `PRESENT phage.yml '  cancel: 7'`, `PRESENT phage.yml '  lift: 1.0'`, `PRESENT phage.yml '  pull: 1.25'`, `ABSENT  phage.yml '  knockback: 1.0'`, `PRESENT void_implosion.yml '  - type: spiral'`, `PRESENT PhageFuser 'SWELL mob='`, `PRESENT PhageFuser 'FUSED mob='`, `PRESENT MobBlaster 'lift=%.2f'`, `PRESENT MobBlaster 'rest=%.3f'`, `ABSENT  MobBlaster 'NoSuchTextControl'`, `PRESENT entry io/github/butterflysmp/rpg/core/mob/MobFuse.class`, `PRESENT entry io/github/butterflysmp/rpg/core/shape/Implosion.class`, `PRESENT BeamTeleport 'ANCHOR_SNAP'`, `PRESENT dragon-blast FZ5:`, `PRESENT dragon-blast IM6:`, `PRESENT dragon-blast KS1:`, `ABSENT  dragon-blast BL1R2:`, `PRESENT dragon-bodies DB11R2:` |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$p = Bytes 'content/mobs/phage.yml'
foreach ($l in '  trigger: 3', '  cancel: 7', '  lift: 1.0', '  pull: 1.25', '  knockback: 1.0') {
  if ($p -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT phage.yml '$l'" } else { "ABSENT  phage.yml '$l'" }
}
$v = Bytes 'content/visuals/void_implosion.yml'
if ($v -ne $null -and $v -match "(?m)^  - type: spiral\s*$") { "PRESENT void_implosion.yml '  - type: spiral'" } else { "ABSENT  void_implosion.yml '  - type: spiral'" }
$f = Bytes 'io/github/butterflysmp/rpg/paper/health/PhageFuser.class'
foreach ($t in 'SWELL mob=', 'FUSED mob=') { if ($f -ne $null -and $f.Contains($t)) { "PRESENT PhageFuser '$t'" } else { "ABSENT  PhageFuser '$t'" } }
$b = Bytes 'io/github/butterflysmp/rpg/paper/health/MobBlaster.class'
foreach ($t in 'lift=%.2f', 'rest=%.3f', 'NoSuchTextControl') { if ($b.Contains($t)) { "PRESENT MobBlaster '$t'" } else { "ABSENT  MobBlaster '$t'" } }
foreach ($e in 'io/github/butterflysmp/rpg/core/mob/MobFuse.class', 'io/github/butterflysmp/rpg/core/shape/Implosion.class') { if ($zip.GetEntry($e) -ne $null) { "PRESENT entry $e" } else { "ABSENT  entry $e" } }
$t = Bytes 'io/github/butterflysmp/rpg/core/mob/BeamTeleport.class'
if ($t.Contains('ANCHOR_SNAP')) { "PRESENT BeamTeleport 'ANCHOR_SNAP'" } else { "ABSENT  BeamTeleport 'ANCHOR_SNAP'" }
foreach ($k in @(@('dragon-blast', 'FZ5'), @('dragon-blast', 'IM6'), @('dragon-blast', 'KS1'), @('dragon-blast', 'BL1R2'), @('dragon-bodies', 'DB11R2'))) {
  if ((Bytes "selftest/GATE-$($k[0]).yml") -match "(?m)^$($k[1]):\s*$") { "PRESENT $($k[0]) $($k[1]):" } else { "ABSENT  $($k[0]) $($k[1]):" }
}
$zip.Dispose()
```

**A jar from master reads ABSENT on every PRESENT line above but `DB11R2:`** (the key control) and PRESENT on
`knockback: 1.0`; `NoSuchTextControl` is ABSENT on both (the class needle's control). **That `core` classes are packaged
into the plugin jar at those paths is INFERRED**; if both entries read ABSENT on a jar whose other lines read this
branch, the row is NOT AS PREDICTED on that clause and the seat rules first.

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**: `GATE-dragon-skreel.md`'s reading with **29 → 30 visuals** (`ls paper/src/main/resources/content/visuals/*.yml \| wc -l` prints 30 here and 29 at master `f6a596ea`); **11 abilities: the test fixture does not ship**; **5 mobs: `phage.yml` loads** (a stale copy would be refused and read 4). No `Refusing`, `Skipping`, `SEVERE` or `Exception`; no `[Rpg]` `WARN` beyond `volley_stone`'s known pair | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | **AS PREDICTED, 2026-11-17.** `[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`. The `[Rpg]` WARN lines are `volley_stone`'s pair (`[Rpg] Content: weapon 'volley_stone' trigger 'left_click' authors cooldown_ticks 0, …` and `[Rpg] 1 content problem(s) -- see the Content: lines above. The server is still running.`); the other WARN lines are Paper's version notice. No `Refusing`, `Skipping`, `SEVERE` or `Exception` |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 165 scenarios across 20 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel]`**, and **no** `Refusing selftest` | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | **AS PREDICTED, 2026-11-17.** Exactly one `[Rpg] Selftest: dev server, 165 scenarios across 20 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel]`; no `Refusing selftest` |

**The 165 is RECOUNTED:** in `paper/src/main/resources/selftest/`, `grep -cE '^  - ' index.yml` prints **20**; `cat $(grep
-E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints **185**; with
`'^setup:'`, **20**: **165**. **The control: at master `f6a596ea` the same commands print 20, 175, 20: 155.**

### R0e — the swell packet's self-check (the seat's ruling 1, condition 5)

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Phage swell packet: index=16 type=int typeId=1 server=V_26_1_2 readFor=V_26_1`**, at INFO, and **no** line beginning `UNVERIFIED Phage swell packet`. The server name is PacketEvents' detection of the running server: `V_26_1_2` is the enum constant for 26.1.2 (READ in its source); **that it detects this server as that constant is INFERRED** (any `V_26_1*` passes the check); `typeId=1` is I1's | `Select-String run\logs\latest.log -Pattern 'swell packet'` | **AS PREDICTED, 2026-11-17.** Exactly one `[22:26:38] [Server thread/INFO]: [Rpg] Phage swell packet: index=16 type=int typeId=1 server=V_26_1_2 readFor=V_26_1`, at INFO; no `UNVERIFIED`. So PacketEvents detected this server as `V_26_1_2`, READ here (it was INFERRED above) |

**If this reads `UNVERIFIED`, STOP:** the swell a player sees is unverified, and no FZ or BQ1R3 row is read.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument for every row:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-blast|\[Rpg\] BLAST|\[Rpg\] SWELL|\[Rpg\] FUSED|\[Rpg\] SWIRL|\[Rpg\] GROWTH|\[Rpg\] SPLIT|\[Rpg\] TELEPORT|CREEPERGUARD'`,
read after Ben's LOGIN, inside the row's own `SELFTEST dragon-blast <row> START … DONE` bracket.

**In EVERY row (not repeated):** after `CLEAN …`, **`LEFTOVER living=0 radius=16`** and **`LINGER work=0 entities=0
radius=48`**; each spawn's `DRIVE spawn tag=<t> <uuid> <mob> frozen|live` and its reply (a stage-1 Phage at GS 100
`… stage 1, 80 HP, GS 100 given)`; a stage 3 at 100 `… stage 3, 120 HP …`, at 60 `… 72 HP, GS 60 given)`; a stage 5 at
100 `… 160 HP …`, at 60 `… 96 HP …`, at 300 `… 480 HP …`; the husk `… 100 HP …`). **No `CREEPERGUARD prime` line of
any kind anywhere in the run** (I6). **No SPLIT line outside the split rows. A non-zero LEFTOVER or LINGER makes its row
NOT AS PREDICTED.**

**THE LINES:**
- **BLAST:** `[Rpg] BLAST mob=phage id=<uuid> stage=<n> gs=<g> base=<b> reach=<R, two places> pull=1.25 lift=1.00
  visual=void_implosion victims=<n> [<victim>, …] spared=<n> [<spared>, …]`; a victim `player <uuid> d=<d> factor=<f>
  dealt=<x> knock=<k> res=<r> h=<h> ground=<g> tf=<…> strength=<…> cap=<…> pull=<…> capped=<…> lift=<…> apex=<…>
  rest=<…> safe=<…>`; a spared one `<type>[/<mob id>] <uuid> d=<d> factor=<f>` (`MobBlastLineTest`). **In every row he
  stands on the Phage's level: `h = d`, `ground=true`.**
- **SWIRL:** two per blast, `[Rpg] SWIRL visual=void_implosion radius=<R, three places> arms=3 ticks=10 at=<the Phage's
  feet>`: **the swirl's width as the server draws it, the stage's reach.**
- **SWELL:** `[Rpg] SWELL mob=phage id=<uuid> dir=<+1|-1> reason=<start|reset|track> tick=<t> viewers=1 d=<d>
  trigger=<x> cancel=<y> swell=<n>` (`PhageFuseWiringTest`). **FUSED:** `[Rpg] FUSED mob=phage id=<uuid> tick=<t>
  swell=30/30`, the tick its blast is raised.
- **FUSE (the instrument's probe):** `FUSE tag=<t> <uuid> fuse=<n>/30 dir=<+1|-1> ignited=false`, our fuse (`SelfTestBlast`).
- **HURT:** `HURT before=<x> after=<a> defense=<Dp> dealer=<uuid> dealerType=<recorded>`, `a = x × 100 / (100 + Dp)`.
  **Damage is predicted against the HURT line, never a health figure** (`.claude/rules/verification.md`).

### BL1R3 — a LIVE stage-1 Phage walks in, lights inside its 1-block trigger, and blows (SURVIVAL) — supersedes GATE-dragon-blast-2.md BL1R2

| prediction | stands on | READING |
|---|---|---|
| (1) `BLOCKS STONE count=189 box=-12 -1 -4 8 -1 4`. (2) **Exactly one** `SWELL mob=phage id=<bl1> dir=+1 reason=start tick=<T> viewers=1 d=<D0> trigger=1.000 cancel=2.333 swell=0`, **D0 < 1.000**. (3) `FUSED mob=phage id=<bl1> tick=<T+30> swell=30/30`. (4) **Exactly one** BLAST `stage=1 gs=100 base=60.000 reach=2.00 … victims=1 [player <Ben> d=<D> …] spared=0 []`, **D < 1.000**, every figure by I7/I8 at R = 2; `safe=true` exactly when the printed `apex` ≥ 2.5. (5) Two SWIRL lines `radius=2.000`. (6) **Exactly one** HURT `before=<dealt>`. (7) `ENTITIES CREEPER count=0 radius=16`; after `return`, `BLOCKS STONE count=189 …`. (8) No GROWTH, no RESET | I3-I8, I11; the walk is vanilla's AI (INFERRED); D bounded by the trigger | **AS PREDICTED, 2026-11-17.** (1) `BLOCKS STONE count=189 box=-12 -1 -4 8 -1 4`. (2) Exactly one `[Rpg] SWELL mob=phage id=4c7796c8-9e7b-4581-99b8-ef762601aa12 dir=+1 reason=start tick=372 viewers=1 d=0.838 trigger=1.000 cancel=2.333 swell=0`: D0 = 0.838 < 1.000. (3) `[Rpg] FUSED mob=phage id=4c7796c8-9e7b-4581-99b8-ef762601aa12 tick=402 swell=30/30`: 372 + 30. (4) `[Rpg] BLAST mob=phage id=4c7796c8-9e7b-4581-99b8-ef762601aa12 stage=1 gs=100 base=60.000 reach=2.00 pull=1.25 lift=1.00 visual=void_implosion victims=1 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 d=0.491 factor=0.661642 dealt=39.699 knock=0.754306 res=0.000 h=0.491 ground=true tf=6.175949 strength=0.273687 cap=0.000000 pull=0.000000 capped=true lift=0.721830 apex=3.306 rest=0.000 safe=true] spared=0 []`: D = 0.491 < 1.000; apex 3.306 ≥ 2.5 and `safe=true`. Re-derived by hand from the printed `knock` (d = 0.491388): factor, dealt, rest; strength, tf, lift and apex NOT re-derived by hand. (5) Two `SWIRL visual=void_implosion radius=2.000 arms=3 ticks=10 at=-98.01 63.00 2.44`. (6) Exactly one `HURT before=39.699 after=36.090 defense=10.000`, and no second. (7) `ENTITIES CREEPER count=0 radius=16`; after `return`, `BLOCKS STONE count=189 …`. (8) No GROWTH, no reset |

### BL2R3 — a LIVE stage-5 Phage at GS 60, two blocks east, lights at once and blows at 2.000 (SURVIVAL) — supersedes BL2R2

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`. (1) `BLOCKS STONE count=189 …`. (2) `SWELL … dir=+1 reason=start … viewers=1 d=<D0> trigger=3.000 cancel=7.000 swell=0`, D0 within 0.05 of 2.000 (it stands; INFERRED that it turns, not walks, before its target is set). (3) `FUSED … tick=<T+30> swell=30/30`. (4) BLAST `stage=5 gs=60 base=90.000 reach=6.00 … [player <Ben> d=<D> factor=<…> dealt=<…> …]`; **at D = 2.000: `factor=0.555556 dealt=50.000 knock=0.666667 res=0.000 h=2.000 ground=true tf=5.343010 strength=0.647553 cap=0.280741 pull=0.280741 capped=true lift=0.419614 apex=1.250 rest=1.500 safe=false`**, else re-derived from the printed D by I7/I8. (5) Two SWIRL `radius=6.000`. (6) HURT `before=<dealt>`. (7) `ENTITIES CREEPER count=0`: **no children**; **no SPLIT**. (8) `BLOCKS STONE count=189 …` after the return | I3, I7, I8; jshell `gate2.jsh` | **AS PREDICTED, 2026-11-17.** (0) `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`. (1) `BLOCKS STONE count=189 …`. (2) `[Rpg] SWELL mob=phage id=e2f075f8-573e-4a0c-8d6b-89ebfec1a4a1 dir=+1 reason=start tick=475 viewers=1 d=2.000 trigger=3.000 cancel=7.000 swell=0`. (3) `[Rpg] FUSED mob=phage id=e2f075f8-573e-4a0c-8d6b-89ebfec1a4a1 tick=505 swell=30/30`: 475 + 30. (4) BLAST `stage=5 gs=60 base=90.000 reach=6.00`, the victim entry at D = 2.000 **character for character the predicted string** (fixed-string grep: 1; control `dealt=50.001`: 0). (5) Two SWIRL `radius=6.000`. (6) `HURT before=50.000 after=45.455 defense=10.000`. (7) `ENTITIES CREEPER count=0 radius=16`; no SPLIT. (8) `BLOCKS STONE count=189 …` |

### BL3R3 — a blast is a Void death: a stage-1 Phage 3.00 away grows to stage 2 (SURVIVAL) — supersedes BL3R2

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl3x <uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<bl3x> stage=1 gs=100 base=60.000 reach=2.00 pull=1.25 lift=1.00 visual=void_implosion victims=0 [] spared=0 []`: **the stage-1 reach is 2, so the stage-1 Phage 3.00 away is no longer listed as spared** (it was at reach 6). (3) **Exactly one** `[Rpg] GROWTH mob=phage id=<bl3b> from=1 to=2 fed=creeper/phage <bl3x> element=void d=3.000 max=100`: growth's own 6-block radius, unchanged. (4) `BODY tag=bl3b … stage=2 scale=0.882 height=1.500 … maxHP=100.000 gs=100 …`. (5) `BODY tag=bl3x none within 16 blocks`. (6) `ENTITIES CREEPER count=1 radius=16`. (7) Two SWIRL `radius=2.000`. (8) No HURT, no SWELL (both frozen) | BL3R2's reading; the stage-1 reach | **AS PREDICTED, 2026-11-17.** (1) `DRIVE detonate tag=bl3x f5c92e03-623b-4d2b-a843-cdbf4e86b957`. (2) `[Rpg] BLAST mob=phage id=f5c92e03-623b-4d2b-a843-cdbf4e86b957 stage=1 gs=100 base=60.000 reach=2.00 pull=1.25 lift=1.00 visual=void_implosion victims=0 [] spared=0 []`. (3) `[Rpg] GROWTH mob=phage id=715b4cfa-1bc9-491a-a525-38576ad87ad4 from=1 to=2 fed=creeper/phage f5c92e03-623b-4d2b-a843-cdbf4e86b957 element=void d=3.000 max=100`. (4) `BODY tag=bl3b 715b4cfa-1bc9-491a-a525-38576ad87ad4 type=creeper mob=phage stage=2 scale=0.882 height=1.500 speedBase=0.3125 speedType=0.2500 speedX=1.250 maxHP=100.000 gs=100 element=void dead=false`. (5) `BODY tag=bl3x none within 16 blocks`. (6) `ENTITIES CREEPER count=1 radius=16`. (7) Two SWIRL `radius=2.000`. (8) No HURT, no SWELL |

### BL4R3 — a spared husk and a spared stage 5, inside a STAGE-5 blast's reach, keep their health AND ARE NOT PULLED; a control proves the probe sees a velocity (SURVIVAL) — supersedes BL4R2

| prediction | stands on | READING |
|---|---|---|
| (1) Before: `PROBE tag=bl4h … husk customHP=100.000/100.000 … d=7.280 dy=0.000`, `PROBE tag=bl4p … creeper customHP=160.000/160.000 … d=7.280 dy=0.000`, both MOTION `vx=0.0000 vy=0.0000 vz=0.0000`. (2) **THE CONTROL:** `MOTION tag=bl4c … vz=0.5000`. (3) **Exactly one** `[Rpg] BLAST mob=phage id=<bl4x> stage=5 gs=100 base=150.000 reach=6.00 … victims=0 [] spared=2 [<either order: husk <bl4h> d=2.000 factor=0.555556, creeper/phage <bl4p> d=2.000 factor=0.555556>]`. (4) **In the blast's own tick**, both MOTION lines `vx=0.0000 vy=0.0000 vz=0.0000`: **not pulled** (had the pull reached them at 2.000, its horizontal would be 0.280741 and its lift 0.419614). (5) Twelve ticks later the two PROBE lines unchanged. (6) No GROWTH. (7) `ENTITIES CREEPER count=1`. (8) Two SWIRL `radius=6.000`. The player, at 7.280, beyond 6: no HURT | I5, I10; the staged blast is now stage 5, so they are inside its reach (stage 1's 2 would reach them only at the edge) | **AS PREDICTED, 2026-11-17.** (1) `PROBE tag=bl4h 745bbb0a-14e5-4743-a5e4-eea4b308f721 husk customHP=100.000/100.000 fireTicks=0 dead=false d=7.280 dy=0.000`, `PROBE tag=bl4p afc03e99-5262-4185-9d36-b21c90773aaf creeper customHP=160.000/160.000 fireTicks=0 dead=false d=7.280 dy=0.000`, both MOTION `vx=0.0000 vy=0.0000 vz=0.0000`. (2) The control: `MOTION tag=bl4c c472a08d-e856-47d0-b2ab-93a349c796e6 vx=0.0000 vy=0.0000 vz=0.5000`. (3) `[Rpg] BLAST mob=phage id=7b246d5d-b323-460f-b4fe-a58318b15abe stage=5 gs=100 base=150.000 reach=6.00 pull=1.25 lift=1.00 visual=void_implosion victims=0 [] spared=2 [husk 745bbb0a-14e5-4743-a5e4-eea4b308f721 d=2.000 factor=0.555556, creeper/phage afc03e99-5262-4185-9d36-b21c90773aaf d=2.000 factor=0.555556]`. (4) In the blast's tick both MOTION `vx=0.0000 vy=0.0000 vz=0.0000`: not pulled. (5) Both PROBE lines unchanged. (6) No GROWTH. (7) `ENTITIES CREEPER count=1 radius=16`. (8) Two SWIRL `radius=6.000`. No HURT |

### BL5R3 — a lit stage-1 Phage turns back when he steps beyond its 7/3-block cancel; nothing blows (SURVIVAL) — supersedes BL5R2

| prediction | stands on | READING |
|---|---|---|
| (1) `SWELL … dir=+1 reason=start … d=<D0> trigger=1.000 cancel=2.333 swell=0`, D0 < 1.000, at tick T1. (2) `FUSE tag=bl5 <uuid> fuse=10/30 dir=+1 ignited=false waited=<recorded>`. (3) After his step nine blocks west, **exactly one** `SWELL … dir=-1 reason=reset tick=<T2> viewers=1 d=<D1> … swell=<T2−T1>`, D1 > 2.333. (4) Five ticks later `FUSE … fuse=<n>/30 dir=-1 ignited=false`, **n < 10**. (5) `ENTITIES CREEPER count=1`. (6) Two ticks later `CMD /kill @e[tag=bl5]`, `SETTLED dying=0 ticks=<t> first=1`, `ENTITIES CREEPER count=0`. (7) No FUSED, no BLAST, no HURT, no SPLIT, no GROWTH | I3, I4; `await_fuse` reads our fuse | **AS PREDICTED, 2026-11-17.** (1) `[Rpg] SWELL mob=phage id=0ae747c4-3f12-4a41-8f1e-291fd1f64fea dir=+1 reason=start tick=670 viewers=1 d=0.838 trigger=1.000 cancel=2.333 swell=0`. (2) `FUSE tag=bl5 0ae747c4-3f12-4a41-8f1e-291fd1f64fea fuse=10/30 dir=+1 ignited=false waited=38`. (3) Exactly one `[Rpg] SWELL mob=phage id=0ae747c4-3f12-4a41-8f1e-291fd1f64fea dir=-1 reason=reset tick=681 viewers=1 d=9.488 trigger=1.000 cancel=2.333 swell=11`: D1 = 9.488 > 2.333, swell 11 = 681 − 670. (4) `FUSE tag=bl5 0ae747c4-3f12-4a41-8f1e-291fd1f64fea fuse=7/30 dir=-1 ignited=false`: 7 < 10. (5) `ENTITIES CREEPER count=1 radius=16`. (6) `CMD /kill @e[tag=bl5]`, `SETTLED dying=0 ticks=20 first=1`, `ENTITIES CREEPER count=0 radius=16`. (7) No FUSED, BLAST, HURT, SPLIT or GROWTH |

### BL6R3 — THE STAGE-1 EDGE, inside (1.95 of 2) (SURVIVAL) — supersedes BL6R2

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl6 <uuid>`. (2) **Exactly one** `[Rpg] BLAST … stage=1 gs=100 base=60.000 reach=2.00 … victims=1 [player <Ben> d=1.950 factor=0.012813 dealt=0.769 knock=0.025000 res=0.000 h=1.950 ground=true tf=2.640406 strength=0.024037 cap=0.549158 pull=0.024037 capped=false lift=0.015975 apex=0.016 rest=0.063 safe=false] spared=0 []`. (3) Two SWIRL **`radius=2.000`**: the stage-1 swirl. (4) **Exactly one** HURT `before=0.769` | jshell `gate2.jsh`; the stage-1 reach | **NOT AS PREDICTED ON ONE FIGURE, 2026-11-17: `factor=0.012812`, predicted `factor=0.012813`.** Read in full: `[Rpg] BLAST mob=phage id=4a0b64a0-6240-4fab-bb07-e79de35e5edd stage=1 gs=100 base=60.000 reach=2.00 pull=1.25 lift=1.00 visual=void_implosion victims=1 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 d=1.950 factor=0.012812 dealt=0.769 knock=0.025000 res=0.000 h=1.950 ground=true tf=2.640406 strength=0.024037 cap=0.549158 pull=0.024037 capped=false lift=0.015975 apex=0.016 rest=0.063 safe=false] spared=0 []`. Every other figure is the predicted one. **THE CAUSE, EXECUTED** (jshell, `Implosion.vanillaShare` from `core/target/classes`): the factor at 1.95 of 2 is exactly 0.0128125, a half at the sixth place. The in-game distance, from the feet at -98.5 and -96.55, is `1.9500000000000028`, whose factor is `0.01281249999999925…` and prints `0.012812`; the prediction's literal `1.95` gives `0.0128125000000000113…` and prints `0.012813`. **A prediction of a floating-point half, not a code defect.** (1) `DRIVE detonate tag=bl6 4a0b64a0-6240-4fab-bb07-e79de35e5edd`. (3) Two SWIRL `radius=2.000`. (4) Exactly one `HURT before=0.769 after=0.699 defense=10.000` |

### BL7R3 — THE STAGE-1 EDGE, outside (2.05 of 2): neither hit nor pulled (SURVIVAL) — supersedes BL7R2

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl7 <uuid>`. (2) **Exactly one** `[Rpg] BLAST … stage=1 gs=100 base=60.000 reach=2.00 … victims=0 [] spared=0 []`. (3) Two SWIRL `radius=2.000`. (4) **No HURT.** BL6R3 is the control | the stage-1 reach | **AS PREDICTED, 2026-11-17.** (1) `DRIVE detonate tag=bl7 edc7f8d3-318a-4afd-a16c-5f08e5d382ee`. (2) `[Rpg] BLAST mob=phage id=edc7f8d3-318a-4afd-a16c-5f08e5d382ee stage=1 gs=100 base=60.000 reach=2.00 pull=1.25 lift=1.00 visual=void_implosion victims=0 [] spared=0 []`. (3) Two SWIRL `radius=2.000`. (4) No HURT |

### BL8R3 — flint and steel's route still cannot light a Phage (SURVIVAL) — supersedes BL8R2

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE ignite tag=bl8 <uuid>`, then `[Rpg] CREEPERGUARD ignite cancelled mob=phage id=<bl8>`. (2) Forty ticks later `FUSE tag=bl8 <uuid> fuse=0/30 dir=-1 ignited=false`: **our fuse, unlit** (frozen: no target, I4). (3) `ENTITIES CREEPER count=1`. (4) No SWELL, no FUSED, no BLAST | BL8R2's reading; I2, I4 | **AS PREDICTED, 2026-11-17.** (1) `DRIVE ignite tag=bl8 8300d90a-7ef1-4eb1-ba63-a539e634a0d7`, `[Rpg] CREEPERGUARD ignite cancelled mob=phage id=8300d90a-7ef1-4eb1-ba63-a539e634a0d7`. (2) `FUSE tag=bl8 8300d90a-7ef1-4eb1-ba63-a539e634a0d7 fuse=0/30 dir=-1 ignited=false`. (3) `ENTITIES CREEPER count=1 radius=16`. (4) No SWELL, FUSED or BLAST |

### BL9R3 — a STAGE-5 Phage at half its reach (3.00): three eighths of 150, a pull the cap lets through WHOLE, and his feet 2.37 nearer (SURVIVAL) — supersedes BL9R2

| prediction | stands on | READING |
|---|---|---|
| (1) Before: `SELF customHP=<M>/<M> at=<x0> <y0> <z0> onGround=true fall=0.000`. (2) `DRIVE detonate tag=bl9 <uuid>`. (3) **Exactly one** BLAST `stage=5 gs=100 base=150.000 reach=6.00 … [player <Ben> d=3.000 factor=0.375000 dealt=56.250 knock=0.500000 res=0.000 h=3.000 ground=true tf=4.304538 strength=0.549941 cap=0.580782 pull=0.549941 capped=false lift=0.237574 apex=0.465 rest=2.367 safe=false] spared=0 []`: **`rest` is 1.25 × the old push's 1.893793 (jshell)**. (4) Two SWIRL `radius=6.000`. (5) HURT `before=56.250`. (6) Forty ticks later `SELF … at=<x1> <y0> <z0> onGround=true fall=0.000`, **x1 = x0 + 2.37 ± 0.10, never beyond x0 + 2.60**, z and y unchanged ± 0.10 | I7, I8, I12; `ImplosionTest.theTravelAtTwoThreeFourAndFiveBesideTheOldPush` | **NOT AS PREDICTED ON ONE CLAUSE, 2026-11-17: (6), his feet came 2.21 nearer, predicted 2.37 ± 0.10.** (1) `SELF customHP=350.000/350.000 at=-98.50 63.00 2.50 onGround=true fall=0.000`. (2) `DRIVE detonate tag=bl9 1eaacf3b-9994-4525-8771-7debe3a25b35`. (3) BLAST `stage=5 gs=100 base=150.000 reach=6.00`, the victim entry character for character the predicted string (`rest=2.367`). (4) Two SWIRL `radius=6.000`. (5) `HURT before=56.250 after=51.136 defense=10.000`. (6) Read in full: `SELF customHP=302.864/350.000 at=-96.29 63.00 2.50 onGround=true fall=0.000`: x1 − x0 = 2.21, outside [2.27, 2.47], short of the model, never beyond the 2.60 bound; y and z unchanged; `fall=0.000`. The rest point read short in every pulled row (THE BOOT section, F-B) |

---

## THE IMPLOSION'S OWN ROWS — SURVIVAL, DIFFICULTY NORMAL

### IM1 — THE STAGE-5 EDGE, inside (5.90 of 6) (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=im1 <uuid>`. (2) **Exactly one** BLAST `stage=5 gs=100 base=150.000 reach=6.00 … [player <Ben> d=5.900 factor=0.008472 dealt=1.271 knock=0.016667 res=0.000 h=5.900 ground=true tf=2.640406 strength=0.020090 cap=2.045140 pull=0.020090 capped=false lift=0.004413 apex=0.004 rest=0.053 safe=false] spared=0 []`. (3) Two SWIRL **`radius=6.000`**: the stage-5 swirl. (4) HURT `before=1.271` | jshell, from `d = 5.9000000000000060` (BL6R2's measured start) | **AS PREDICTED, 2026-11-17.** (1) `DRIVE detonate tag=im1 de6639ef-fd6e-4e13-8873-0562f0789bde`. (2) BLAST `stage=5 gs=100 base=150.000 reach=6.00`, the victim entry at `d=5.900` character for character the predicted string. (3) Two SWIRL `radius=6.000`. (4) `HURT before=1.271 after=1.155 defense=10.000` |

### IM2 — THE STAGE-5 EDGE, outside (6.05 of 6) (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=im2 <uuid>`. (2) **Exactly one** BLAST `stage=5 gs=100 base=150.000 reach=6.00 … victims=0 [] spared=0 []`. (3) Two SWIRL `radius=6.000`. (4) **No HURT**; IM1 is the control | the stage-5 reach | **AS PREDICTED, 2026-11-17.** (1) `DRIVE detonate tag=im2 6eb09856-179c-4460-a0f8-9e48ecb38209`. (2) `[Rpg] BLAST mob=phage id=6eb09856-179c-4460-a0f8-9e48ecb38209 stage=5 gs=100 base=150.000 reach=6.00 pull=1.25 lift=1.00 visual=void_implosion victims=0 [] spared=0 []`. (3) Two SWIRL `radius=6.000`. (4) No HURT |

### IM3 — the cap binds at 2.00: he is lifted 1.25 and comes to rest 1.50 east, half a block short (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<x0> <y0> <z0> onGround=true fall=0.000`. (2) `DRIVE detonate tag=im3 <uuid>`. (3) **Exactly one** BLAST `stage=5 gs=100 base=150.000 reach=6.00 … [player <Ben> d=2.000 factor=0.555556 dealt=83.333 knock=0.666667 res=0.000 h=2.000 ground=true tf=5.343010 strength=0.647553 cap=0.280741 pull=0.280741 capped=true lift=0.419614 apex=1.250 rest=1.500 safe=false] spared=0 []`. (4) HURT `before=83.333`. (5) Forty ticks later `SELF … at=<x1> <y0> <z0> onGround=true fall=0.000`, **x1 = x0 + 1.50 ± 0.10, never beyond x0 + 1.60** | I7, I8, I12; `ImplosionTest.atTwoBlocksTheCapBindsAndHeComesToRestHalfABlockShort` | **AS PREDICTED, AT THE TOLERANCE'S EDGE, 2026-11-17.** (1) `SELF customHP=350.000/350.000 at=-98.50 63.00 2.50 onGround=true fall=0.000`. (2) `DRIVE detonate tag=im3 813d1e1c-ec9f-4495-9a3e-8bb330b52df4`. (3) BLAST `stage=5 gs=100 base=150.000 reach=6.00`, the victim entry character for character the predicted string (`capped=true … rest=1.500`). (4) `HURT before=83.333 after=75.758 defense=10.000`. (5) `SELF customHP=278.242/350.000 at=-97.10 63.00 2.50 onGround=true fall=0.000`: x1 − x0 = 1.40, the LOWER EDGE of 1.50 ± 0.10 as printed to two places (the true figure may lie either side of it); never beyond 1.60. Short, as in every pulled row (F-B) |

### IM4 — almost at the centre: not thrown through, lifted over the safe fall, and spared the fall (SURVIVAL) — new

**The staging and its one hazard:** a stage-1 Phage spawned 0.30 east of his feet, inside his box (they overlap below
0.476471, jshell). His client may push him out of the overlap before the blast (INFERRED), never beyond 0.476471: the
blast finds him at D in [0.300, 0.477], inside the 0.5 margin either way, so no sideways pull.

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<xa> <y0> <z0>`, xa within [x0 − 0.18, x0]. (2) `DRIVE detonate tag=im4 <uuid>`. (3) **Exactly one** BLAST `stage=1 gs=100 base=60.000 reach=2.00 … [player <Ben> d=<D> factor=<falloff(D, 2)> dealt=<60 × F> knock=<1 − D/2> res=0.000 h=<D> ground=true tf=<…> strength=<…> cap=0.000000 pull=0.000000 capped=true lift=<…> apex=<…> rest=0.000 safe=true] spared=0 []`, **D in [0.300, 0.477]**: dealt in [40.261, 47.175], lift in [0.730811, 0.835790], apex in [3.380, 4.298] (jshell). (4) **Exactly one** HURT `before=<dealt>`, and **no second** (the safe landing). (5) Forty ticks later `SELF … at=<xb> <y0> <z0> onGround=true fall=0.000`, **xb = xa ± 0.10** | I7, I8, I12; `ImplosionTest.aVictimAlmostAtTheCentreIsNotThrownThroughButIsStillLifted`; the mark (ruling 31) | **NOT AS PREDICTED ON THREE CLAUSES, 2026-11-17: (1) xa, (3)'s bounds on D, and (5) xb.** What it was built to show **held**: no sideways pull (`cap=0.000000 pull=0.000000 capped=true … rest=0.000`), lifted over the safe fall (`apex=3.326`), `safe=true`, exactly one HURT and no second, `fall=0.000`. (1) `SELF customHP=350.000/350.000 at=-98.69 63.00 2.50 onGround=true fall=0.000`: xa 0.19 west of x0, outside [x0 − 0.18, x0] by 0.01. (3) Read in full: `[Rpg] BLAST mob=phage id=c0eebb89-a275-4258-9fb7-4da7bff3aa94 stage=1 gs=100 base=60.000 reach=2.00 pull=1.25 lift=1.00 visual=void_implosion victims=1 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 d=0.487 factor=0.664144 dealt=39.849 knock=0.756299 res=0.000 h=0.487 ground=true tf=6.175949 strength=0.272370 cap=0.000000 pull=0.000000 capped=true lift=0.724230 apex=3.326 rest=0.000 safe=true] spared=0 []`: D = 0.487 outside [0.300, 0.477], so dealt 39.849 lies outside [40.261, 47.175], lift 0.724230 outside [0.730811, 0.835790] and apex 3.326 outside [3.380, 4.298]. All three follow from D: his client pushed him 0.01 beyond the overlap bound 0.476471. Factor, dealt and knock were re-derived from the printed knock. (4) `HURT before=39.849 after=36.226 defense=10.000`, and no second. (5) `SELF customHP=317.774/350.000 at=-99.10 63.00 2.50 onGround=true fall=0.000`: **xb − xa = −0.41, he ended 0.41 WEST, away from the Phage**; predicted xa ± 0.10. **The cause is NOT KNOWN** (F-C) |

### IM5 — THE STAGE-3 EDGE, inside (3.95 of 4) (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 3, 120 HP, GS 100 given)" [aqua, gray]`. (1) `DRIVE detonate tag=im5 <uuid>`. (2) **Exactly one** BLAST `stage=3 gs=100 base=105.000 reach=4.00 … [player <Ben> d=3.950 factor=0.006328 dealt=0.664 knock=0.012500 res=0.000 h=3.950 ground=true tf=2.640406 strength=0.014456 cap=1.306617 pull=0.014456 capped=false lift=0.004743 apex=0.005 rest=0.038 safe=false] spared=0 []`. (3) Two SWIRL **`radius=4.000`**: the stage-3 swirl. (4) HURT `before=0.664` | jshell `gate2.jsh`; the stage-3 reach | **AS PREDICTED, 2026-11-17.** (0) `REPLY "Spawned Phage (creeper, stage 3, 120 HP, GS 100 given)" [aqua, gray]`. (1) `DRIVE detonate tag=im5 45084543-57b3-4c2d-ba46-5dc9f771b1fc`. (2) BLAST `stage=3 gs=100 base=105.000 reach=4.00`, the victim entry at `d=3.950` character for character the predicted string. (3) Two SWIRL `radius=4.000`. (4) `HURT before=0.664 after=0.604 defense=10.000` |

### IM6 — THE STAGE-3 EDGE, outside (4.05 of 4) (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=im6 <uuid>`. (2) **Exactly one** BLAST `stage=3 gs=100 base=105.000 reach=4.00 … victims=0 [] spared=0 []`. (3) Two SWIRL `radius=4.000`. (4) **No HURT**; IM5 is the control | the stage-3 reach | **AS PREDICTED, 2026-11-17.** (1) `DRIVE detonate tag=im6 8144b39f-12a6-4497-9014-76178905564e`. (2) `[Rpg] BLAST mob=phage id=8144b39f-12a6-4497-9014-76178905564e stage=3 gs=100 base=105.000 reach=4.00 pull=1.25 lift=1.00 visual=void_implosion victims=0 [] spared=0 []`. (3) Two SWIRL `radius=4.000`. (4) No HURT |

---

## THE FUSE BY STAGE — SURVIVAL, DIFFICULTY NORMAL

**Each row:** a LIVE Phage at speed 0, its trigger's edge + 0.05 from his feet F0. Forty ticks for its target. He
steps IN 0.10 (just inside): it lights. Ten ticks on he steps OUT just beyond its cancel: it turns back. Fifteen ticks
later (its swell drained) he steps IN again: it lights, and blows 30 ticks later where he stands.

**Common to the three:** (a) `TARGET tag=<t> <uuid> type=creeper target=self dead=false` (I4). (b) Before the step in,
`FUSE … fuse=0/30 dir=-1 ignited=false` and **no SWELL line yet: it does not light just outside the trigger**. (c) After
the step in, **exactly one** `SWELL … dir=+1 reason=start tick=<T1> viewers=1 d=<in> … swell=0`. (d) Ten ticks later
`FUSE … fuse=<n>/30 dir=+1`, **n in [8, 10]** (the step and the fuse's tick fall in one tick or the next; INFERRED).
(e) After the step out, **exactly one** `SWELL … dir=-1 reason=reset tick=<T2> viewers=1 d=<out> … swell=<T2 − T1>`.
(f) Fifteen ticks later `FUSE … fuse=0/30 dir=-1`: drained (its swell was at most 11). (g) After the step in,
`SWELL … dir=+1 reason=start tick=<T3> … swell=0`, then **`FUSED … tick=<T3 + 30> swell=30/30`**: it blew **30 ticks
after it lit**. (h) The BLAST below, at `d=<in>`, and two SWIRL lines at the stage's reach. (i) HURT `before=<dealt>`.
(j) His feet: `SELF` just after the last step in at x = F0.x + 0.10, and 45 ticks later at **x = F0.x + 0.10 +
`rest` ± 0.10, `fall=0.000`**. (k) `ENTITIES CREEPER count=0 radius=16`. (l) No `CREEPERGUARD`.

### FZ1 — stage 1: lights inside 1.000, resets beyond 2.333 (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (a)-(l), with: in `d=0.950`, out `d=2.400`, `trigger=1.000 cancel=2.333`; BLAST `stage=1 gs=100 base=60.000 reach=2.00 … [player <Ben> d=0.950 factor=0.400313 dealt=24.019 knock=0.525000 res=0.000 h=0.950 ground=true tf=5.498140 strength=0.331968 cap=0.081846 pull=0.081846 capped=true lift=0.452874 apex=1.440 rest=0.450 safe=false] spared=0 []`; SWIRL `radius=2.000`; his feet after at F0.x + 0.55 ± 0.10 (the Phage at F0.x + 1.05, less the margin) | I3, I4, I7, I8; jshell `gate2.jsh`; the table above | **NOT AS PREDICTED ON ONE FIGURE, 2026-11-17: `factor=0.400312`, predicted `factor=0.400313`**, BL6R3's cause, EXECUTED: the in-game `0.9500000000000028` gives `0.40031249999999851…`, the literal `0.95` gives `0.40031250000000001…`. Read in full: `[Rpg] BLAST mob=phage id=e308e7c7-44fd-4b51-b455-1bc7e9ee85ef stage=1 gs=100 base=60.000 reach=2.00 pull=1.25 lift=1.00 visual=void_implosion victims=1 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 d=0.950 factor=0.400312 dealt=24.019 knock=0.525000 res=0.000 h=0.950 ground=true tf=5.498140 strength=0.331968 cap=0.081846 pull=0.081846 capped=true lift=0.452874 apex=1.440 rest=0.450 safe=false] spared=0 []`. Every other clause as predicted: (a) `TARGET tag=fz1 e308e7c7-44fd-4b51-b455-1bc7e9ee85ef type=creeper target=self dead=false`. (b) `FUSE … fuse=0/30 dir=-1 ignited=false`, and no SWELL before the step in. (c) `[Rpg] SWELL mob=phage id=e308e7c7-44fd-4b51-b455-1bc7e9ee85ef dir=+1 reason=start tick=1085 viewers=1 d=0.950 trigger=1.000 cancel=2.333 swell=0`. (d) `fuse=9/30 dir=+1`. (e) `[Rpg] SWELL mob=phage id=e308e7c7-44fd-4b51-b455-1bc7e9ee85ef dir=-1 reason=reset tick=1095 viewers=1 d=2.400 trigger=1.000 cancel=2.333 swell=10`: 1095 − 1085. (f) `fuse=0/30 dir=-1`. (g) `[Rpg] SWELL mob=phage id=e308e7c7-44fd-4b51-b455-1bc7e9ee85ef dir=+1 reason=start tick=1110 viewers=1 d=0.950 trigger=1.000 cancel=2.333 swell=0`, `[Rpg] FUSED mob=phage id=e308e7c7-44fd-4b51-b455-1bc7e9ee85ef tick=1140 swell=30/30`: 1110 + 30. (h) Two SWIRL `radius=2.000`. (i) `HURT before=24.019 after=21.835 defense=10.000`. (j) `SELF customHP=350.000/350.000 at=-98.40 63.00 2.50 onGround=true fall=0.000`, then `SELF customHP=330.165/350.000 at=-97.98 63.00 2.50 onGround=true fall=0.000`: predicted F0.x + 0.55 = -97.95 ± 0.10, read 0.03 short, inside. (k) `ENTITIES CREEPER count=0 radius=16`. (l) No CREEPERGUARD |

### FZ3 — stage 3 at GS 60: lights inside 2.000, resets beyond 4.667 (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 3, 72 HP, GS 60 given)" [aqua, gray]`. (a)-(l), with: in `d=1.950`, out `d=4.750`, `trigger=2.000 cancel=4.667`; BLAST `stage=3 gs=60 base=63.000 reach=4.00 … [player <Ben> d=1.950 factor=0.387578 dealt=24.417 knock=0.512500 res=0.000 h=1.950 ground=true tf=4.985207 strength=0.492763 cap=0.290861 pull=0.290861 capped=true lift=0.327498 apex=0.807 rest=1.450 safe=false] spared=0 []`; SWIRL `radius=4.000`; his feet after at F0.x + 1.55 ± 0.10 | as FZ1 | **NOT AS PREDICTED ON ONE CLAUSE, 2026-11-17: (j), his feet at -97.08, predicted F0.x + 1.55 = -96.95 ± 0.10: 0.13 short.** Read in full: `SELF customHP=329.802/350.000 at=-97.08 63.00 2.50 onGround=true fall=0.000`. Every other clause as predicted: (0) `REPLY "Spawned Phage (creeper, stage 3, 72 HP, GS 60 given)" [aqua, gray]`. (a) `TARGET tag=fz3 b20cd854-8ade-4036-bbb0-f44cf61741c9 type=creeper target=self dead=false`. (b) `fuse=0/30 dir=-1`, no SWELL before the step in. (c) `[Rpg] SWELL mob=phage id=b20cd854-8ade-4036-bbb0-f44cf61741c9 dir=+1 reason=start tick=1200 viewers=1 d=1.950 trigger=2.000 cancel=4.667 swell=0`. (d) `fuse=9/30 dir=+1`. (e) `[Rpg] SWELL mob=phage id=b20cd854-8ade-4036-bbb0-f44cf61741c9 dir=-1 reason=reset tick=1210 viewers=1 d=4.750 trigger=2.000 cancel=4.667 swell=10`. (f) `fuse=0/30 dir=-1`. (g) `[Rpg] SWELL mob=phage id=b20cd854-8ade-4036-bbb0-f44cf61741c9 dir=+1 reason=start tick=1225 viewers=1 d=1.950 trigger=2.000 cancel=4.667 swell=0`, `[Rpg] FUSED mob=phage id=b20cd854-8ade-4036-bbb0-f44cf61741c9 tick=1255 swell=30/30`: 1225 + 30. (h) BLAST `stage=3 gs=60 base=63.000 reach=4.00`, the victim entry character for character the predicted string; two SWIRL `radius=4.000`. (i) `HURT before=24.417 after=22.198 defense=10.000`. (j) before: `SELF … at=-98.40 63.00 2.50`. (k) `ENTITIES CREEPER count=0 radius=16`. (l) No CREEPERGUARD |

### FZ5 — stage 5 at GS 60: lights inside 3.000, resets beyond 7.000 (SURVIVAL) — new

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`. (a)-(l), with: in `d=2.950`, out `d=7.050`, `trigger=3.000 cancel=7.000`; BLAST `stage=5 gs=60 base=90.000 reach=6.00 … [player <Ben> d=2.950 factor=0.383368 dealt=34.503 knock=0.508333 res=0.000 h=2.950 ground=true tf=4.553130 strength=0.556961 cap=0.538091 pull=0.538091 capped=true lift=0.244685 apex=0.486 rest=2.450 safe=false] spared=0 []`; SWIRL `radius=6.000`; his feet after at F0.x + 2.55 ± 0.10 | as FZ1 | **NOT AS PREDICTED ON ONE CLAUSE, 2026-11-17: (j), his feet at -96.24, predicted F0.x + 2.55 = -95.95 ± 0.10: 0.29 short.** Read in full: `SELF customHP=320.634/350.000 at=-96.24 63.00 2.50 onGround=true fall=0.000`. Every other clause as predicted: (0) `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`. (a) `TARGET tag=fz5 cde30548-f289-4c66-9a37-83859c7e2b08 type=creeper target=self dead=false`. (b) `fuse=0/30 dir=-1`, no SWELL before the step in. (c) `[Rpg] SWELL mob=phage id=cde30548-f289-4c66-9a37-83859c7e2b08 dir=+1 reason=start tick=1315 viewers=1 d=2.950 trigger=3.000 cancel=7.000 swell=0`. (d) `fuse=9/30 dir=+1`. (e) `[Rpg] SWELL mob=phage id=cde30548-f289-4c66-9a37-83859c7e2b08 dir=-1 reason=reset tick=1325 viewers=1 d=7.050 trigger=3.000 cancel=7.000 swell=10`. (f) `fuse=0/30 dir=-1`. (g) `[Rpg] SWELL mob=phage id=cde30548-f289-4c66-9a37-83859c7e2b08 dir=+1 reason=start tick=1340 viewers=1 d=2.950 trigger=3.000 cancel=7.000 swell=0`, `[Rpg] FUSED mob=phage id=cde30548-f289-4c66-9a37-83859c7e2b08 tick=1370 swell=30/30`: 1340 + 30. (h) BLAST `stage=5 gs=60 base=90.000 reach=6.00`, the victim entry character for character the predicted string; two SWIRL `radius=6.000`. (i) `HURT before=34.503 after=31.366 defense=10.000`. (j) before: `SELF … at=-98.40 63.00 2.50`. (k) `ENTITIES CREEPER count=0 radius=16`. (l) No CREEPERGUARD |

---

## THE KNELL ON A SLAB — F19 — SURVIVAL, DIFFICULTY NORMAL

### KS1 — a hit on a player standing on a slab, then a landing on the floor beside him (SURVIVAL) — new

**The staging:** the teleport rows' (`GATE-dragon-knell-5.md`): his feet F0 at a block centre, a stone floor 23 × 21,
air 8 high, a FROZEN Knell at K = F0 + (8, 0, 0); then he is lifted half a block and a bottom `smooth_stone_slab` placed
in the block his feet were in, so he stands ON it at **F = F0 + (0, 0.5, 0)**.

| prediction | stands on | READING |
|---|---|---|
| (1) `SELF … at=<F.x> <F0.y + 0.50> <F.z> onGround=true fall=0.000`: **he stands on the slab.** (2) `AT tag=ks1 <uuid>` at K. (3) `DRIVE teleport_draw tag=ks1 <uuid> side=right first=50.000`, `DRIVE beam tag=ks1 <uuid>`, the beam's TELEGRAPH and a FIRE line **that hits** him, and **exactly one** HURT `before=65.000`. (4) **Exactly one** `[Rpg] TELEPORT … tick=<FIRE + 24> from=<K> center=<F.x> <F0.y + 0.500> <F.z> radius=8.000 bearing=0.000 side=right first=50.000 tried=1 [right 50.000 ok y=<F0.y> at=<F.x + 5.142> <F0.y> <F.z + 6.128>] landed=<the same> moved=true`: **the candidate's y is the FLOOR, F0.y, half a block below the centre's.** (5) After the wait, `AT tag=ks1 <uuid>` there, three places. **Before the fix every one of the ten candidates read `no-footing` and `moved=false`** (MUTF19A, core). **That the beam hits a target half a block up is INFERRED** (its aim point is on the player; the tolerance is unchanged) | I13; KB10R6's reading, the same geometry on a floor F0.y | **AS PREDICTED, 2026-11-17.** (1) `SELF customHP=350.000/350.000 at=-98.50 63.50 2.50 onGround=true fall=0.000`: on the slab. (2) `AT tag=ks1 dc530d2d-7d9e-42e6-9e74-0a70155a124a x=-90.500 y=63.000 z=2.500`. (3) `DRIVE teleport_draw tag=ks1 dc530d2d-7d9e-42e6-9e74-0a70155a124a side=right first=50.000`, `DRIVE beam tag=ks1 dc530d2d-7d9e-42e6-9e74-0a70155a124a`, the telegraph at tick 1396, the FIRE at tick 1420 `… [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 gap=0.000 hit] dealt=65.000`, exactly one `HURT before=65.000 after=59.091 defense=10.000`. (4) `[Rpg] TELEPORT mob=knell id=dc530d2d-7d9e-42e6-9e74-0a70155a124a target=player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 tick=1444 from=-90.500 63.000 2.500 center=-98.500 63.500 2.500 radius=8.000 bearing=0.000 side=right first=50.000 tried=1 [right 50.000 ok y=63.000 at=-93.358 63.000 8.628] landed=-93.358 63.000 8.628 moved=true`: 1420 + 24; the candidate's y is the floor, 63.000. (5) `AT tag=ks1 dc530d2d-7d9e-42e6-9e74-0a70155a124a x=-93.358 y=63.000 z=8.628`. So the beam's hit on a target half a block up, INFERRED above, is READ here |

---

## THE SPLIT ROWS — SURVIVAL, DIFFICULTY NORMAL

**Before each SPLIT line, exactly two `CHILD parent=<that SPLIT's parent uuid> child=<uuid> frozen=true`. A kill step
logs `CMD /kill @e[tag=<t>]` and then `SETTLED dying=0 ticks=<t> first=1`.**

### DS1R5 — split by his emberblade at GS 60; thawed; each child walks in, lights inside its own trigger, and blows; a grown one's GROWTH distance EQUALS its sibling's spared distance (SURVIVAL) — supersedes DS1R4

| prediction | stands on | READING |
|---|---|---|
| (1)-(5) as DS1R4 read them (the spawn and damage replies, `DRIVE attack … d=3.000 charge=1.000`, two `CHILD … frozen=true`, one `SPLIT … stage=5 gs=60 children=2 childStage=1 [<A> dx=-0.50, <B> dx=0.50]`, `ENTITIES CREEPER count=2`, two stage-1 BODY lines `maxHP=48.000 gs=60`, the heal's reply, two `DRIVE thaw … ai=true`). (6) **Within the 200 ticks, for each child a `SWELL … dir=+1 reason=start`, a `FUSED`, and exactly one BLAST, X then Y**: X `stage=1 gs=60 base=36.000 reach=2.00 … victims=1 [player <Ben> d=<D1> …] spared=<n> […]`, n = 1 naming `creeper/phage <Y> d=<d> factor=<falloff(d, 2)>` exactly when Y stood within **2** of X (its stage-1 reach); **if n = 1**, exactly one GROWTH `id=<Y> from=1 to=2 … d=<d>`, **only if Y also stood within growth's 6** (always, here), printed identically to X's spared `d`, and Y's BLAST `stage=2 gs=60 base=49.500 reach=3.00 …`; **if Y grew but was beyond 2 of X**, the GROWTH line still comes (growth's radius is 6) and X's `spared=0 []`. Each pull field by I7/I8 at its own stage's reach. **The bounds: D1 < 1.000** (X lights inside its trigger while he stands); **D2 ≤ Y's cancel** (2.333, or 3.500 if grown): a lit fuse blows wherever he is inside it. (7) **Exactly two** HURT lines, dealer X then Y. (8) `ENTITIES CREEPER count=0`. (9) One SPLIT; no `CREEPERGUARD` | DS1R4's reading; I3-I8. **The timing depends on vanilla's AI** (INFERRED) | **AS PREDICTED ON ITS OWN CLAUSES (1)-(9), 2026-11-17; NOT AS PREDICTED ON THE AUTO ROWS' BLANKET CLAUSE "he stands on the Phage's level: h = d, ground=true", FOR Y's VICTIM ENTRY**, which found him in the air: `d=2.318 … h=0.934 ground=false tf=11.111114`. X's blast had lifted him 10 ticks before (`apex=2.799 … safe=true`), and Y took the airborne bound 1/(1 − 0.91), as the model says. (1)-(5) as DS1R4 read them: `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`, `REPLY "CREEPER damaged: 1/96 custom HP" [green]`, `DRIVE attack Player#attack target=936d1564-3a26-4d88-8a38-6fb4ec23b0de creeper d=3.000 charge=1.000`, two `CHILD … frozen=true`, `[Rpg] SPLIT mob=phage parent=936d1564-3a26-4d88-8a38-6fb4ec23b0de stage=5 gs=60 children=2 childStage=1 [a6b38c09-6a82-4a4b-a291-dde50bdd47f7 dx=-0.50, aba7d621-a15a-453a-b572-7df30a00dd21 dx=0.50]`, `ENTITIES CREEPER count=2 radius=16`, two stage-1 BODY lines `maxHP=48.000 gs=60`, `REPLY "Custom HP: 350/350" [green]`, two `DRIVE thaw … ai=true`. (6) X = a6b38c09: `[Rpg] SWELL mob=phage id=a6b38c09-6a82-4a4b-a291-dde50bdd47f7 dir=+1 reason=start tick=1635 viewers=1 d=0.729 trigger=1.000 cancel=2.333 swell=0`, `[Rpg] FUSED mob=phage id=a6b38c09-6a82-4a4b-a291-dde50bdd47f7 tick=1665 swell=30/30`, `[Rpg] BLAST mob=phage id=a6b38c09-6a82-4a4b-a291-dde50bdd47f7 stage=1 gs=60 base=36.000 reach=2.00 pull=1.25 lift=1.00 visual=void_implosion victims=1 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 d=0.598 factor=0.596333 dealt=21.468 knock=0.701110 res=0.000 h=0.598 ground=true tf=5.991050 strength=0.303391 cap=0.016321 pull=0.016321 capped=true lift=0.657758 apex=2.799 rest=0.098 safe=true] spared=1 [creeper/phage aba7d621-a15a-453a-b572-7df30a00dd21 d=0.547 factor=0.627368]`: D1 = 0.598 < 1.000; Y within 2, so n = 1, and `[Rpg] GROWTH mob=phage id=aba7d621-a15a-453a-b572-7df30a00dd21 from=1 to=2 fed=creeper/phage a6b38c09-6a82-4a4b-a291-dde50bdd47f7 element=void d=0.547 max=60`: **`d=0.547` printed identically**. Y = aba7d621: `[Rpg] SWELL mob=phage id=aba7d621-a15a-453a-b572-7df30a00dd21 dir=+1 reason=start tick=1645 viewers=1 d=0.962 trigger=1.000 cancel=2.333 swell=0`, `[Rpg] FUSED mob=phage id=aba7d621-a15a-453a-b572-7df30a00dd21 tick=1675 swell=30/30`, `[Rpg] BLAST mob=phage id=aba7d621-a15a-453a-b572-7df30a00dd21 stage=2 gs=60 base=49.500 reach=3.00 pull=1.25 lift=1.00 visual=void_implosion victims=1 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 d=2.318 factor=0.139521 dealt=6.906 knock=0.227353 res=0.000 h=0.934 ground=false tf=11.111114 strength=0.068802 cap=0.039025 pull=0.039025 capped=true lift=0.220590 apex=0.415 rest=0.434 safe=false] spared=0 []`: D2 = 2.318 ≤ 3.500. Factor, dealt, knock and rest were re-derived from the printed knock for X and Y. Two SWIRL lines at each stage's reach: `radius=2.000` for X and `radius=3.000` for Y. (7) Exactly two HURT, `HURT before=21.468 after=19.516 defense=10.000` dealer X, then `HURT before=6.906 after=6.278 defense=10.000` dealer Y; no third, so no fall damage. (8) `ENTITIES CREEPER count=0 radius=16`. (9) One SPLIT; no CREEPERGUARD |

### DS2R5 — a stage 5 killed by `/kill` splits; its children are born frozen (SURVIVAL) — supersedes DS2R4

| prediction | stands on | READING |
|---|---|---|
| `CMD /kill @e[tag=ds2]`, the two `CHILD … frozen=true`, **exactly one** SPLIT `parent=<ds2> stage=5 gs=300 children=2 childStage=1 […]`, `SETTLED dying=0 ticks=<t> first=1`, `ENTITIES CREEPER count=2`, **exactly two** BODY lines `tag=ds2`, each stage 1 `maxHP=240.000 gs=300`. **No SWELL, no BLAST, no HURT** | DS2R4's reading, the steps unchanged | **AS PREDICTED, 2026-11-17.** `CMD /kill @e[tag=ds2]`, `CHILD parent=fabe0a15-e7fc-495d-a5fe-5d86b2e66d98 child=f05b9f93-5ffe-4ded-a65b-2f2088034de8 frozen=true`, `CHILD parent=fabe0a15-e7fc-495d-a5fe-5d86b2e66d98 child=c6c0846a-47cd-42d8-b888-a9fd6a20ea18 frozen=true`, `[Rpg] SPLIT mob=phage parent=fabe0a15-e7fc-495d-a5fe-5d86b2e66d98 stage=5 gs=300 children=2 childStage=1 [f05b9f93-5ffe-4ded-a65b-2f2088034de8 dx=-0.50, c6c0846a-47cd-42d8-b888-a9fd6a20ea18 dx=0.50]`, `SETTLED dying=0 ticks=20 first=1`, `ENTITIES CREEPER count=2 radius=16`, two BODY lines `tag=ds2`, each `stage=1 … maxHP=240.000 gs=300`. No SWELL, BLAST or HURT |

### DS5R5 — once only: two after the split, still two forty ticks later (SURVIVAL) — supersedes DS5R4

| prediction | stands on | READING |
|---|---|---|
| The two `CHILD … frozen=true`, **exactly one** SPLIT, and `ENTITIES CREEPER count=2 radius=16` **twice**. **No SWELL, no BLAST, no HURT** | DS5R4's reading, the steps unchanged | **AS PREDICTED, 2026-11-17.** `CHILD parent=d67369b7-25ec-4763-b2c1-fb336100744b child=6c60f34a-a49d-43c6-a2db-c3f5efda2932 frozen=true`, `CHILD parent=d67369b7-25ec-4763-b2c1-fb336100744b child=26e9d74e-3304-4822-a908-6aed7abd54a5 frozen=true`, exactly one SPLIT, `ENTITIES CREEPER count=2 radius=16` twice. No SWELL, BLAST or HURT |

### DS6R5 — the row-start clean removes DS5R5's children (SURVIVAL) — supersedes DS6R4

| prediction | stands on | READING |
|---|---|---|
| `CLEAN blocks=756 removed=2 inventory=kept …`, `LEFTOVER living=0 radius=16`, `LINGER work=0 entities=0 radius=48`, `ENTITIES CREEPER count=0 radius=16` | DS6R4's reading; directly after DS5R5 | **AS PREDICTED, 2026-11-17.** `CLEAN blocks=756 removed=2 inventory=kept return=-98.60 63.00 2.35`, `LEFTOVER living=0 radius=16`, `LINGER work=0 entities=0 radius=48`, `ENTITIES CREEPER count=0 radius=16` |

### DG6R5 — a stage 5's death grows the bystander to 3 (SURVIVAL) — supersedes DG6R4

| prediction | stands on | READING |
|---|---|---|
| (0) Two `CHILD parent=<dg6p> … frozen=true`. (1) **Exactly one** SPLIT `parent=<dg6p> stage=5 gs=300 …`. (2) **Exactly one** `GROWTH … id=<dg6b> from=2 to=3 fed=creeper/phage <dg6p> element=void d=2.000 max=360`. (3) A stage-3 BODY for dg6b (`scale=1.176 height=2.000 … maxHP=360.000 gs=300`). (4) Two stage-1 BODY lines for dg6p, `maxHP=240.000 gs=300`. (5) `ENTITIES CREEPER count=3`. (6) No SWELL, no BLAST, no HURT | DG6R4's reading, the steps unchanged | **AS PREDICTED, 2026-11-17.** (0) Two `CHILD parent=10624d7d-a234-40be-8b7c-205c09f46d95 … frozen=true`. (1) `[Rpg] SPLIT mob=phage parent=10624d7d-a234-40be-8b7c-205c09f46d95 stage=5 gs=300 children=2 childStage=1 [0f72a848-7d68-4d91-a622-b81604e8a274 dx=-0.50, 90399f66-d5ad-4743-976a-064dc63bd35c dx=0.50]`. (2) `[Rpg] GROWTH mob=phage id=9e4fcd9c-4b6a-46e1-b589-046c54b7ebce from=2 to=3 fed=creeper/phage 10624d7d-a234-40be-8b7c-205c09f46d95 element=void d=2.000 max=360`. (3) `BODY tag=dg6b 9e4fcd9c-4b6a-46e1-b589-046c54b7ebce type=creeper mob=phage stage=3 scale=1.176 height=2.000 speedBase=0.2500 speedType=0.2500 speedX=1.000 maxHP=360.000 gs=300 element=void dead=false`. (4) Two stage-1 BODY lines `tag=dg6p`, `maxHP=240.000 gs=300`. (5) `ENTITIES CREEPER count=3 radius=16`. (6) No SWELL, BLAST or HURT |

### DG9R5 — a Phage grown to 5, then killed, splits into two frozen children (SURVIVAL) — supersedes DG9R4

| prediction | stands on | READING |
|---|---|---|
| **Exactly one** `GROWTH … id=<dg9> from=4 to=5 fed=enderman <uuid> element=void d=2.000 max=480`, then a stage-5 BODY for dg9. Two ticks later its kill: the two `CHILD … frozen=true`, **exactly one** SPLIT, two stage-1 BODY lines (`maxHP=240.000 gs=300`), `ENTITIES CREEPER count=2`. No SWELL, no BLAST, no HURT | DG9R4's reading; **one step added**, `wait: 2` between the probe and the kill | **AS PREDICTED, 2026-11-17.** `[Rpg] GROWTH mob=phage id=527ee495-5d77-46df-9d6f-023253f21f5b from=4 to=5 fed=enderman 3fa501f3-c192-473e-a1ca-946e2dd97707 element=void d=2.000 max=480`, `BODY tag=dg9 527ee495-5d77-46df-9d6f-023253f21f5b type=creeper mob=phage stage=5 scale=1.765 height=3.000 speedBase=0.1250 speedType=0.2500 speedX=0.500 maxHP=480.000 gs=300 element=void dead=false`; then `CMD /kill @e[tag=dg9]`, two `CHILD … frozen=true`, exactly one SPLIT, two stage-1 BODY lines `maxHP=240.000 gs=300`, `ENTITIES CREEPER count=2 radius=16`. No SWELL, BLAST or HURT |

---

## BEN'S ROW — BY THE ASK STEP. EACH ANSWER IS ONE WORD

### BQ1R3 — the swell, the swirl, the pull against the old push, and the small Phage's shorter distances, by his own eyes (SURVIVAL) — supersedes BQ1R2

A LIVE stage-5 Phage at GS 60 and speed 0, 2.5 east (inside its 3-block trigger), lights, swells and blows: **41.562
before his defence**; it pulls him **0.418467** (capped) and lifts him **0.317221**, so he comes to rest **2.000** east,
half a block short (jshell). Questions 1-3 follow. Then, put back and healed, a LIVE stage-1 Phage at speed 0, 0.8 east
(inside its 1-block trigger), lights and blows: **28.800**, rest **0.300**. Question 4 follows. **Each question waits
1200 ticks for his next chat line; his answer cell is his line, verbatim.**

| prediction | READING |
|---|---|
| Recorded beside the answers: the big Phage's `SWELL … dir=+1 reason=start … viewers=1 d=<D> trigger=3.000 cancel=7.000`, its `FUSED`, its BLAST `stage=5 gs=60 base=90.000 reach=6.00 …` (at D = 2.500: `factor=0.461806 dealt=41.562 knock=0.583333 res=0.000 h=2.500 ground=true tf=4.779348 strength=0.611924 cap=0.418467 pull=0.418467 capped=true lift=0.317221 apex=0.767 rest=2.000 safe=false`), two SWIRL `radius=6.000`, HURT `before=41.562`; the small one's `SWELL … trigger=1.000 cancel=2.333`, `FUSED`, BLAST `stage=1 gs=100 base=60.000 reach=2.00 …` (at D = 0.800: `factor=0.480000 dealt=28.800 knock=0.600000 res=0.000 h=0.800 ground=true tf=5.767770 strength=0.332085 cap=0.052013 pull=0.052013 capped=true lift=0.537978 apex=1.955 rest=0.300 safe=false`), two SWIRL `radius=2.000`, HURT `before=28.800` | **AS PREDICTED, 2026-11-17, every recorded line.** The big Phage: `[Rpg] SWELL mob=phage id=36cffc37-5837-46b8-b834-ac923fbff22f dir=+1 reason=start tick=2017 viewers=1 d=2.500 trigger=3.000 cancel=7.000 swell=0`, `[Rpg] FUSED mob=phage id=36cffc37-5837-46b8-b834-ac923fbff22f tick=2047 swell=30/30`, BLAST `stage=5 gs=60 base=90.000 reach=6.00` with the victim entry at D = 2.500 character for character the predicted string, two SWIRL `radius=6.000`, `HURT before=41.562 after=37.784 defense=10.000`. The small one: `[Rpg] SWELL mob=phage id=1501805f-329f-47b3-ab42-cebbf495a18a dir=+1 reason=start tick=3093 viewers=1 d=0.800 trigger=1.000 cancel=2.333 swell=0`, `[Rpg] FUSED mob=phage id=1501805f-329f-47b3-ab42-cebbf495a18a tick=3123 swell=30/30`, BLAST `stage=1 gs=100 base=60.000 reach=2.00` with the entry at D = 0.800 character for character the predicted string, two SWIRL `radius=2.000`, `HURT before=28.800 after=26.182 defense=10.000` |
| `ASK 1 question="Did you see the big Phage swell up before it blew?" answer="<his line>" ticks=<t>` | `ASK 1 question="Did you see the big Phage swell up before it blew?" answer="Not sure" ticks=144`. **Recorded as NOT STATED**, by this row's own rule ("not sure" is NOT STATED and never re-asked in this boot). So whether a player sees the swell is not answered by his eyes. The server's side of it, the +1 SWELL to `viewers=1`, read |
| `ASK 2 question="Did you see black and dark purple particles swirl in to where it stood?" answer="<his line>" ticks=<t>` | `ASK 2 question="Did you see black and dark purple particles swirl in to where it stood?" answer="yes" ticks=90` |
| `ASK 3 question="Did it pull you in, and did the pull feel a little stronger than the old push?" answer="<his line>" ticks=<t>`. **What it compares, stated:** at 2.5 the old push carried him about 2.34 blocks away (jshell, `flight.jsh`); this pull carries him 2.000 in, CAPPED (half a block short of the Phage). Uncapped, from 3 blocks out, it carries 1.25 × the push | `ASK 3 question="Did it pull you in, and did the pull feel a little stronger than the old push?" answer="yes it pulled me in, i think it needs to either be stronger or pull in multiple times or continue pulling in for a second" ticks=719`. **The first half is answered yes. The second half, "a little stronger than the old push", is not answered yes or no:** he asks for MORE pull, stronger, or several pulls, or a pull held for a second. That is a player-facing request, Ben's to rule; nothing is built from it here |
| `ASK 4 question="The small Phage now lights only within 1 block and its blast reaches 2. Does it still feel like a threat?" answer="<his line>" ticks=<t>` | `ASK 4 question="The small Phage now lights only within 1 block and its blast reaches 2. Does it still feel like a threat?" answer="yes" ticks=147` |

"not sure", or no line within the minute (`answer=NOT STATED after 1200 ticks`), is recorded as NOT STATED and never
re-asked in this boot.

---

## THE ROWS ALREADY MERGED — THE AUDIT (2026-11-17, REDONE FOR PART C AND THE LIFT)

**The question asked of every merged selftest row:** does it stage a live or exploding Phage, or a Knell teleport, and
what do this branch's changes (our fuse in place of vanilla's swell; distances by stage; the pull with its lift and the
new BLAST line; the landing anchor) change in its lines? **Instrument:** `grep -n "phage" paper/src/main/resources/
selftest/GATE-*.yml`, and every row of `GATE-dragon-teleport.yml` and `GATE-dragon-blink.yml` read for its target's and
its Phalanx's footing.

| file | rows | what changes | done |
|---|---|---|---|
| `GATE-dragon-blast.yml` | all 16 | every BLAST line (stage reach, pull, lift); a live Phage now lights by OUR rules at its STAGE's trigger (a live stage 1 walks to within 1, not 3); a stage-1 blow reaches 2, not 6 (BL6/BL7/BL9's staging moved to keep their distances meaningful) | **ALL RE-PREDICTED** here; BL5, DG9 gain a `wait: 2` before their kills |
| `GATE-dragon-bodies.yml`, `-split.yml`, `-growth.yml` | DB2R2-DB4R2, DS3R2, DS4R2, DG1R2-DG8R2 | **none in their lines**: every Phage spawned `frozen` (`noScenarioOutsideTheBlastGateLeavesALivePhage`), so it has no target (I4) and never lights; growth and split carry no fuse or pull. **One new line is possible in no row:** our fuse logs only at a START or RESET | **unchanged, not re-run** |
| `GATE-dragon-shot.yml` | SH9R2 | a frozen stage-1 Phage, never lit or blown | **unchanged** |
| `GATE-dragon-teleport.yml`, `GATE-dragon-beam.yml` | KB10R6, TP2R5-TP14R4, KB11R6, TQ1R5 | **none:** every target stands on a whole-block floor, floor(y) = y | **unchanged, not re-run** |
| `GATE-dragon-blink.yml`, `GATE-dragon-skreel.yml` | BB1-BB5, PB1R2-PB4 | **none:** `MobShooter` already passes the floored anchor | **unchanged** |
| the 13 closed manual gates that spawn live dragon mobs | — | a live Phage now lights at its stage's trigger and pulls with lift | **carried** (PLAN §8), not re-predicted |

---

## WHAT NO ROW CAN STAGE — UNIT-ONLY, AND WHY

- **A second viewer, and a viewer who starts tracking mid-fuse** (the `reason=track` line): the selftest refuses a
  second player. Pinned on the source (`PhageFuseWiringTest.everyViewerIsShownAndANewOneATickLater`), not run.
- **A Phage that unloads, splits or grows mid-fuse leaving no stale state:** its fuse is dropped at
  `EntityRemoveFromWorldEvent` (wiring row) and a split parent is dead; a growth re-reads the stage's distances each tick
  (`PhageFuser.step`). No row times a grow or an unload to a lit fuse.
- **A victim on ice, or already in the air:** no step stages ice under him or a jump at the tick. `ImplosionTest`
  (`onIceTheCapBindsFartherOut`, `aJumpingVictimIsCappedByTheAirBound`, the sweep).
- **What the client SHOWS** (the swelling, the swirl, the lift's height): AUTO reads only that the server SENT and
  drew them; BQ1R3 asks him (`.claude/rules/verification.md`, *AN AUTO READING IS A READING OF THE SERVER*).
- **The shape's second use (an ability):** no ability ships one. `AbilityLoaderTest` loads the test-only fixture
  (`lift: 0`), `ImplosionEffectTest` applies it.
- **A Knell landing on a slab's top:** never tried, by design (`theCostOfTheAnchorASlabTopIsNeverTried`).

## THE BOOT OF 2026-11-17 — WHAT WAS READ

**Dated 2026-11-17. Written after the clean stop; no prediction above was edited.**

- **The boot.** `6d079ead`, booted 22:26 by `./scripts/dev-server.sh --refresh-content` on the seat's approval and
  Ben's "ready", after the carried state: no JVM; backup `rpg-run-backups/2026-11-17-pre-void-implosion` (no earlier
  folder of that name today; `run/plugins/Rpg` 129 files 399274 bytes, `run/world` 193 files 204977218 bytes,
  `diff -rq lines: 0` each); `12:  dev-pack-on-join: true`. R0a-R0e PASS, the self-check line in full
  `[Rpg] Phage swell packet: index=16 type=int typeId=1 server=V_26_1_2 readFor=V_26_1`.
- **DEPARTURE: Ben's LOGIN (22:26:41) came before my word that R0 passed** (R0 was read after it, and passed). Recorded
  as a departure, as the seat said; every reading above is from the run that began after that LOGIN. **A second,
  harmless one:** he left at 22:29:38 and logged in again at 22:29:42, after `RESTORED` and before `/stop`; no row ran
  then.
- **The run sheet:** `/gamemode survival` (22:26:47), ONE `/rpg selftest dragon-blast` (22:26:55-22:29:26), `/stop`
  (22:29:47). The stop was clean (`All dimensions are saved`, `All RegionFile I/O tasks to complete`; no java.exe left).
  No second run.
- **The run's own lines:** `setup DIFFICULTY found=NORMAL set=NORMAL` and `SELFTEST RUN RESTORED xp=712667
  cell=mage/fire gamemode=SURVIVAL held=2 mana=100.0 trace=false spawn_mobs=false time=6000 blocks=28644 mobsRemoved=0
  orbsRemoved=1 keep_inventory=false immediate_respawn=false difficulty=NORMAL`. 27 `LEFTOVER living=0` and 27 `LINGER
  work=0 entities=0` (26 rows and the setup); ten `CHILD` lines, all `frozen=true`; both `DRIVE thaw` `ai=true`;
  **16 HURT lines, one per predicted hit, and no second HURT in any row, so no fall damage anywhere**, including IM4,
  BL1R3 and DS1R5's X, whose lifts crossed the 2.5 mark (`safe=true`); every victim's `res=0.000`; SPLIT lines only in
  DS1R5, DS2R5, DS5R5, DG6R5 and DG9R5; **no `CREEPERGUARD prime` line anywhere**, the one CREEPERGUARD line being BL8R3's
  `ignite cancelled`. Every SWELL line `viewers=1`; every FUSED tick its START tick + 30 (nine).
- **What Part C's rows show, read:** our fuse lit strictly inside the trigger at stages 1, 3 and 5 (`d=0.950`,
  `1.950`, `2.950`, not 0.05 outside), reset just beyond the cancel (`2.400`, `4.750`, `7.050`), drained, relit, and
  blew 30 ticks after it lit, each START and RESET sent to the one viewer. The reach, the blast, the pull and the swirl
  all followed the stage (2, 3, 4, 6; IM1/IM2, IM5/IM6, BL6R3/BL7R3 inside and outside each edge). **Not read by any
  row:** a second viewer, a mid-fuse tracker (`reason=track`), as stated above.

### F-A — TWO FIGURES MIS-PREDICTED: A FLOATING-POINT HALF (BL6R3, FZ1)

The factor at 1.95 of 2 is exactly 0.0128125, and at 0.95 of 2 exactly 0.4003125: both a half at the sixth place. The
predictions came from the literal distances; in game the distance is the difference of two feet, `1.9500000000000028`
and `0.9500000000000028`, and rounds the other way (EXECUTED, jshell, `Implosion.vanillaShare`; the figures are in
BL6R3's cell). **The code printed the right figure. The rule it breaks is the project's own: never predict floating
point; execute the expression on the operands the game will have.** A prediction of a row that lands on a half should
give both roundings or stage the distance off the half.

### F-B — THE REST POINT READ SHORT IN EVERY PULLED ROW WHERE HE STOOD STILL

| row | the Phage at | modelled travel | read travel (two places) | read / modelled | left between him and the Phage |
|---|---|---|---|---|---|
| FZ1 | -97.45 | 0.450 | 0.42 | 0.93 | 0.53 |
| IM3 | -96.50 | 1.500 | 1.40 | 0.93 | 0.60 |
| FZ3 | -96.45 | 1.450 | 1.32 | 0.91 | 0.63 |
| BL9R3 | -95.50 | 2.367 | 2.21 | 0.93 | 0.79 |
| FZ5 | -95.45 | 2.450 | 2.16 | 0.88 | 0.79 |

(The Phage's x from its SWIRL line; his from the SELF lines; each printed figure ± 0.005.) **Always short, never past:
the seat's rule that he never comes to rest past the origin less the margin held in every reading**, and the BLAST
lines are the model's figures exactly. **What is off is the model of HIS travel**, by 7-12%. The model is the server's
physics (READ); that a player's CLIENT runs the same travel was INFERRED (PLAN, *Part C and the lift, as built*), and
these readings weigh against it at that size. **The cause is NOT KNOWN** and nothing was changed. Ben's ASK 3 asks for
more pull, which this shortfall also points at.

### F-C — IM4: HE ENDED 0.41 WEST, AWAY FROM THE PHAGE

The pull was zero (`cap=0.000000 pull=0.000000`), the lift unscaled (`apex=3.326`), and the safe landing held (one
HURT, `fall=0.000`). But his feet went from -98.69 to -99.10. **The cause is NOT KNOWN.** INFERRED candidates, not
separable from this log: the client's own push out of the overlapping Phage left him a westward velocity the server
never set, which his airtime (air friction 0.91) carried; or he moved. His client also pushed him 0.01 beyond the
overlap bound before the blast, which accounts for (1) and (3) of the row.

### F-D — DS1R5: THE SECOND BLAST FOUND HIM IN THE AIR

X's blast lifted him (`apex=2.799`) and Y blew ten ticks later, finding him at `h=0.934 ground=false`. The auto rows'
blanket clause "he stands on the Phage's level: h = d, ground=true" did not hold for that entry. The model took the
airborne bound (`tf=11.111114`), and no fall damage followed. Only the blanket clause was wrong, as in DS1R4's "D < 3":
a blast after a lift breaks the premise that he stands.

**Ben's answers, verbatim:** `ASK 1 … answer="Not sure" ticks=144` (NOT STATED), `ASK 2 … answer="yes" ticks=90`,
`ASK 3 … answer="yes it pulled me in, i think it needs to either be stronger or pull in multiple times or continue
pulling in for a second" ticks=719`, `ASK 4 … answer="yes" ticks=147`. **Open for Ben, unruled:** a stronger pull, or
several pulls, or a pull held for a second (ASK 3); and whether the swell shows (ASK 1, not stated).
