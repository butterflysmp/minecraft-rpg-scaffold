# GATE — THE PHAGE'S BLAST, 2: 60 TO 150, VANILLA'S PUSH, AND THE GROWTH DECISION FIXED AT THE DEATH

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-blast-2`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

**What this gate re-predicts, and why.** The seat's brief of 2026-11-17 (PLAN-dragon-mobs.md, RULINGS, *the 60 and the
150, and knockback*): the blast's damage goes to 60 (stage 1) and 150 (stage 5), it now pushes its victims as a vanilla
blast does, and growth's decision is fixed at the death (§8 F5). **Every row of `selftest/GATE-dragon-blast.yml` is
re-predicted here as a NEW row named for the `GATE-dragon-blast.md` row it supersedes, with the next suffix** (BL1 →
BL1R2, DS1R3 → DS1R4, BQ1 → BQ1R2). The old rows' predictions and readings stay in `GATE-dragon-blast.md`, untouched,
under a dated note naming this file. Six rows read figures that change (BL1, BL2, BL6, BL9, DS1R3 and BQ1: the damage
and the push); BL3, DG6R3 and DG9R3 read a GROWTH line whose `d` now prints three places; BL4 gains the velocity reads;
the other seven are carried with the same steps (diffed, 0 lines) so that the run's every row has a prediction in ONE
file, and a reading has somewhere to go.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL, and vanilla's swell skips a creative target.
- **The difficulty is set by the selftest:** the setup's `difficulty: normal`, the run's `DIFFICULTY found=… set=NORMAL`
  and `RESTORED … difficulty=<what it found>`.
- **Gear scores, per row:** 100 except BL2R2 (**60**), DS1R4 (**60**), and the split rows DS2R4, DS5R4, DG6R4, DG9R4
  (300, as before). At gear score g a blast's zero-distance damage is the authored `60 + 22.5 × (stage − 1)` × g / 100
  (`MobBlast.price`, P-b): stage 1 at 100 is 60.000, stage 5 at 100 is 150.000, stage 5 at 60 is 90.000, stage 1 at 60
  is 36.000, stage 2 at 60 is 49.500 (each executed in jshell).

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC, ROW BY ROW

**The floor:** his maximum is at least 100 (`CombatantStats.DEFAULT_PLAYER_BASE`, a player's base; INFERRED that no
shipped modifier lowers a player below it). **Ben's own 350 is NOT relied on.** Every row that can hit him first runs
`rpg heal 1000000`, which heals him to that maximum. The figure below is the most the row can deal **before his
defence**, after that heal; defence only lowers it (`Defense.applyDefense`: `damage × 100 / (100 + defense)`). The old
argument (50 at most) no longer holds: a stage 5 at gear score 100 at his feet deals **150**, and DS1R3 at gear score
200 could have dealt 120 + 165.

| row | what hits him after one heal | the worst case, before defence | how it was brought under 100 |
|---|---|---|---|
| BL1R2 | a LIVE stage 1, GS 100 | 60 × 1 = **60** (the factor is at most 1) | nothing needed |
| BL2R2 | a LIVE stage 5 | at GS 100 it would be 150; at **GS 60**, 90 × 1 = **90** | gear score 100 → 60 (`bl2 live phage stage 5 60`) |
| BL6R2 | a frozen stage 1 at 5.90 | 60 × 0.008472 = **0.508** | nothing needed |
| BL7R2 | a frozen stage 1 at 6.05 | **0** (beyond the reach) | — |
| BL9R2 | a frozen stage 1 at 3.00 | 60 × 0.375 = **22.500** | nothing needed |
| DS1R4 | two thawed children, both after ONE heal | the first at stage 1, **36.000**; the second may have grown to stage 2, **49.500**: **85.500** | the parent's gear score 200 → **60** (the children inherit it) |
| BQ1R2 small | a frozen stage 1 at exactly 2.00 | 60 × 0.555556 = **33.333** | blown at a fixed distance instead of live |
| BQ1R2 big | a frozen stage 5 at exactly 2.00, after a SECOND heal | 150 × 0.555556 = **83.333** | blown at a fixed distance instead of live (live, at his feet, 150) |

Every other row places him outside every blast's reach (BL3R2, BL4R2 at 7 blocks) or blows nothing (BL5R2, BL8R2, the
split rows). **A fall is not in this table:** see the push's own section.

## WHAT THE ROWS STAND ON — EACH FACT, AND ITS MARK

- **K1. The push's strength, READ** (§7 F-B, `ServerExplosion#hurtEntities`): `(1 − d / reach) × multiplier × (1 −
  resistance)`, exposure fixed at 1. Ours: `MobBlast.knockbackStrength`, pinned by
  `MobBlastTest.thePushIsVanillasLinearShareOfTheReach` against jshell's values from that expression (d = 2 →
  0.666666667; 3 → 0.5; 5.9 → 0.016666667).
- **K2. The direction, READ:** the victim's EYE minus the blast's centre (the Phage's feet), normalised; ours
  `MobBlast.pushVector`. Pinned in core; its wiring by `DragonBlastWiringTest.onlyTheVictimsArePushedAndThePushIsVanillas`.
- **K3. Only victims are pushed, READ in our code:** the push is inside the victims' loop (the same wiring row). A
  spared creature has no `knock=`/`push=` in its entry (`MobBlastLineTest.aSparedEntryCarriesNeitherDamageNorPush`).
- **K4. The resistance:** the victim's `Attribute.EXPLOSION_KNOCKBACK_RESISTANCE` (READ in the API). **Its value on
  Ben is NOT predicted:** a Blast Protection enchantment raises it (INFERRED from vanilla; not read in the jar), and his
  armour at the boot is his own. So every victim's `res=` is RECORDED and its `push=` predicted as `knock × (1 − res)`.
- **K5. The multiplier is 1.00** (`phage.yml`, `knockback: 1.0`; `MobLoaderTest`).
- **K6. A frozen mob cannot be moved by a push, READ** (§7 F-B: `LivingEntity#aiStep` calls `travel` only with
  effective AI), **but its velocity holds one:** only components under 0.003 are zeroed. So a frozen spared mob's
  "not pushed" is read in its VELOCITY (`probe_motion`), never its position, which would be a hollow reading.
- **K7. The instrument reads velocity in the same tick as the blast, READ in our code:** `SelfTestRun.advance` runs
  every step up to the next `wait` in one pass, and `MobBlaster.detonate` applies the push synchronously inside
  `Creeper#explode()`. **That a vanilla `data merge … {Motion:[…]}` sets the delta movement, and that it reads back
  unchanged in the same tick, is INFERRED** (it is BL4R2's control; if it reads otherwise the row says so).
- **K8. The growth decision is fixed at the death, READ in our code** (§8 F5): `MobGrower.reportDeath` decides who
  grows and the distance from the positions at the death (`MobGrowth.resolve`, each `Grow` carrying its distance);
  `applyGrowth` re-reads only the stage (`MobGrowth.restage`, which takes no position). Pinned by
  `MobGrowthTest.eachGrowCarriesTheDistanceAtTheDeath`, `theLaterTickReReadsOnlyTheStageSoTwoDeathsAddUp` and
  `DragonGrowthWiringTest.theLaterTickReReadsOnlyTheStage` (its mutation, the re-decision restored, is red).
- **K9. The GROWTH line prints `d` to three places**, as the BLAST line prints a spared distance
  (`DragonGrowthWiringTest.theGrowthLinePrintsTheDistanceAsTheBlastLineDoes`). In a blast the two `d`s are the SAME
  number: both are the dead Phage's feet (read before `remove()`) to the other Phage's feet, in the same tick, and the
  blast calls growth before it removes the Phage. **So they print the same three places.**
- **K10. The thaw, READ in our code:** `thaw: <tag>` calls `Mob#setAI(true)` on every tagged mob within 16 blocks and
  logs `DRIVE thaw tag=<t> <uuid> ai=<hasAI() read back>`.
- **K11. A creeper swells only at a target within 3 blocks, and a live child's targets are players and whoever hurt
  it** (`GATE-dragon-blast.md` S2, S12, READ). Nothing hurts the children; Ben is the one player.
- **S10 (carried from `GATE-dragon-blast.md`): the tolerance on a figure recomputed from a printed distance.** `d`
  prints three places, `factor` six, `dealt` three. A factor recomputed from the printed `d` agrees with the printed
  one within 0.000125 + 0.0000005; `dealt` with `base × printed factor` within 0.0005 + base × 0.0000005. **And for
  `knock` (linear, slope 1/6 per block):** within 0.0005 / 6 + 0.0000005 = **0.0000839**.

## THE PUSH MOVES THE PLAYER — WHAT IT DOES TO EACH ROW'S READINGS

**A push changes where he stands, and nothing the blast already wrote.** The BLAST and HURT lines are written at the
blast, from the positions at the blast, so the push cannot change them. What it can change:

- **A later relative step.** Every row that pushes him either ends at the blast (BL6R2, BL9R2) or runs `return` and
  re-aligns before anything relative (BL1R2, BL2R2: the floor count is taken after `return`; BQ1R2: `return`, the
  align, then the second Phage). DS1R4 has nothing relative after its thaw.
- **The floor count.** Unchanged by a push: nothing of ours touches a block. BL1R2 and BL2R2 count 189 before and after
  (the box `-12 -1 -4 8 -1 4` is 21 × 1 × 9 = 189), taken from the returned spot.
- **Where he lands.** Every row that pushes him lays its floor to x − 12, so he lands on it. **INFERRED, not computed:**
  at d = 2 his push is about 0.52 west and 0.42 up (blocks per tick; jshell, from an eye 1.62 above the feet), which
  lifts him about a block, under vanilla's fall-damage height; so **no second HURT line** (a fall, `dealerType` not the
  Phage's) is predicted. A live Phage that blew at his feet would throw him straight up; every live Phage of 2026-11-17
  blew at d = 2.000. **If a second HURT appears, the row is NOT AS PREDICTED and the fall is named.**
- **The client.** Whether his screen shows the push is not a server reading: BQ1R2 asks him.

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-dragon-blast-2"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   If the folder exists (a boot the same day), add `-2` to its name. Report the folder, both pairs of counts, and
   `diff -rq lines: 0` for each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report.
4. **Boot this branch's head:** `./scripts/dev-server.sh --refresh-content`, announcing "booting now", on Ben's "ready".
5. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` line names this branch's head. **Readings count
   only after Ben's LOGIN in that boot's log.** (On 2026-11-17 the log rolled at midnight while the server waited; if
   it does again, R0 is in the rolled `.log.gz` and the readings in the new `latest.log`, the same JVM.)

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest dragon-blast` — **ONCE. Run it one time only, even after it finishes.** Stand still and touch
   nothing, **except when a gold question appears in chat**: then type your answer, `yes` or `no`, in chat. There are
   three questions, and they ask you to **watch your health bar**. You will be pushed back by some blasts; that is
   expected, stand still again afterwards. Wait for the chat to say the run finished.
3. `/stop`

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-blast-2` prints it, shortened. **Not** master's `ca0ae38a`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries the follow-up

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/mobs/phage.yml` with the lines `  damage: { first: 60, last: 150 }` and `  knockback: 1.0`, and **no** `  damage: { first: 20, last: 50 }` (the control: the old line, ABSENT); `MobBlaster.class` carrying the text `knockback=%.2f` and `SelfTestBlast.class` carrying `DRIVE thaw tag=` and `MOTION tag=%s`, with the control text `NoSuchTextControl` ABSENT from `MobBlaster.class`; in `selftest/GATE-dragon-blast.yml` the keys `BL1R2:`, `DS1R4:` and `BQ1R2:` PRESENT and `BL1:`, `DS1R3:` and `BQ1:` ABSENT; and `dragon-bodies DB11R2:` PRESENT (the key control) | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $s = $zip.GetEntry($name).Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$p = Bytes 'content/mobs/phage.yml'
foreach ($l in '  damage: { first: 60, last: 150 }', '  knockback: 1.0', '  damage: { first: 20, last: 50 }') {
  if ($p -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT phage.yml '$l'" } else { "ABSENT  phage.yml '$l'" }
}
$b = Bytes 'io/github/butterflysmp/rpg/paper/health/MobBlaster.class'
foreach ($t in 'knockback=%.2f', 'NoSuchTextControl') { if ($b.Contains($t)) { "PRESENT MobBlaster '$t'" } else { "ABSENT  MobBlaster '$t'" } }
$s = Bytes 'io/github/butterflysmp/rpg/paper/selftest/SelfTestBlast.class'
foreach ($t in 'DRIVE thaw tag=', 'MOTION tag=%s') { if ($s.Contains($t)) { "PRESENT SelfTestBlast '$t'" } else { "ABSENT  SelfTestBlast '$t'" } }
foreach ($k in @(@('dragon-blast', 'BL1R2'), @('dragon-blast', 'DS1R4'), @('dragon-blast', 'BQ1R2'), @('dragon-blast', 'BL1'), @('dragon-blast', 'DS1R3'), @('dragon-blast', 'BQ1'), @('dragon-bodies', 'DB11R2'))) {
  if ((Bytes "selftest/GATE-$($k[0]).yml") -match "(?m)^$($k[1]):\s*$") { "PRESENT $($k[0]) $($k[1]):" } else { "ABSENT  $($k[0]) $($k[1]):" }
}
$zip.Dispose()
```

**A jar from master reads the opposite on every line but `DB11R2:`**, which is the control that the key needle can find
a key at all; `NoSuchTextControl` is the control that the class needle does not find everything. The class texts are
searched in the DECOMPRESSED entry (a zip's own bytes are deflated, so a search of the jar file finds nothing).

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim `GATE-dragon-blast.md` R0c's reading: this PR changes `phage.yml` and adds no content file. No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN`, beyond `volley_stone`'s known pair | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 90 scenarios across 15 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast]`**, and **no** `Refusing selftest` line (a malformed `thaw` or `probe_motion` is refused at load, by name) | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | |

**The 90 is RECOUNTED from the files at this branch's head:** in `paper/src/main/resources/selftest/`,
`grep -cE '^  - ' index.yml` prints **15**; `cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/;
s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints **105**; the same with `'^setup:'` prints **15**: 105 − 15 = **90**.
**The control: at master `ca0ae38a` the same commands print 15, 105, 15, also 90**, because this PR renames the blast
gate's sixteen rows and adds none. **So R0d cannot tell this jar from master's; R0a and R0b's keys do.**
`SelfTestScenariosTest` pins the 90 and the sixteen new names.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument for every row:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-blast|\[Rpg\] BLAST|\[Rpg\] GROWTH|\[Rpg\] SPLIT|CREEPERGUARD'`,
read after Ben's LOGIN, inside the row's own `SELFTEST dragon-blast <row> START … DONE` bracket.

**In EVERY row below (and not repeated in each):** after the row's `CLEAN …` line, **`LEFTOVER living=0 radius=16`**;
for each mob the row spawns, one `DRIVE spawn tag=<its tag> <uuid> <mob> frozen` (or `live` where the row says) and the
command's reply: a stage-1 Phage at GS 100 `REPLY "Spawned Phage (creeper, stage 1, 80 HP, GS 100 given)" [aqua, gray]`,
a stage 5 at GS 100 `… stage 5, 160 HP, GS 100 given)`, a stage 5 at GS 60 `… stage 5, 96 HP, GS 60 given)` (160 ×
0.6), at GS 300 `… stage 5, 480 HP, GS 300 given)`, a stage 2 at 300 `… stage 2, 300 HP …`, a stage 4 at 300 `… stage 4,
420 HP …`, the husk `REPLY "Spawned husk (husk, 100 HP, GS 100 given)" [aqua, gray]`. **No `CREEPERGUARD prime` line
anywhere in the run. No `SPLIT` line in BL1R2-BL9R2 or BQ1R2. A non-zero LEFTOVER makes its row NOT AS PREDICTED.**

**A BLAST line** is now `[Rpg] BLAST mob=phage id=<uuid> stage=<n> gs=<g> base=<b> reach=6.00 knockback=1.00
victims=<n> [<victim>, …] spared=<n> [<spared>, …]`, where a victim is `player <uuid> d=<d> factor=<f> dealt=<x>
knock=<k> res=<r> push=<p>` and a spared creature `<type>[/<mob id>] <uuid> d=<d> factor=<f>`, with NO knock, res or
push (`MobBlastLineTest`). **For every victim: `k = 1 − d/6` (K1, within S10's 0.0000839 of the printed d), `r`
RECORDED (K4), `p = k × (1 − r)` to six places.** **A HURT line** is `HURT before=<x> after=<a> defense=<Dp>
dealer=<uuid> dealerType=<recorded>`, with `a = x × 100 / (100 + Dp)` to three places (Dp ≤ 0: a = x). **A row that
heals** logs `REPLY "Custom HP: <M>/<M>" [green]`, M recorded.

### BL1R2 — a LIVE stage-1 Phage blows on the player for 60 × the falloff, and pushes him (SURVIVAL) — supersedes GATE-dragon-blast.md BL1

| prediction | stands on | READING |
|---|---|---|
| (1) `BLOCKS STONE count=189 box=-12 -1 -4 8 -1 4` before the spawn. (2) **Exactly one** BLAST line `id=<the bl1 uuid> stage=1 gs=100 base=60.000 reach=6.00 knockback=1.00 victims=1 [player <Ben's uuid> d=<D> factor=<F> dealt=<X> knock=<K> res=<R> push=<P>] spared=0 []`: F = falloff(D) and X = 60 × F within S10; K = 1 − D/6 within 0.0000839; P = K × (1 − R). **A BOUND:** D < 3 (BL1's), so F > 0.375, X > 22.500, K > 0.5. (3) **Exactly one** HURT line, `before=<X>`, `dealer=<the bl1 uuid>`. (4) `ENTITIES CREEPER count=0 radius=16`. (5) After `DRIVE return …`, `BLOCKS STONE count=189 box=-12 -1 -4 8 -1 4`. (6) No GROWTH | K1-K5, S10; `GATE-dragon-blast.md` BL1's reading (d=2.000 there) | |

### BL2R2 — a LIVE stage-5 Phage at gear score 60 blows for 90 × the falloff, pushes him, and leaves NO children (SURVIVAL) — supersedes BL2

| prediction | stands on | READING |
|---|---|---|
| (0) `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]`. (1) `BLOCKS STONE count=189 …` before. (2) **Exactly one** BLAST line `id=<the bl2 uuid> stage=5 gs=60 base=90.000 reach=6.00 knockback=1.00 victims=1 [player <Ben's uuid> d=<D> factor=<F> dealt=<X> knock=<K> res=<R> push=<P>] spared=0 []`, X = 90 × F, the same K and P rules; **the bound** D < 3, so X > 33.750. (3) **Exactly one** HURT line, `before=<X>`. (4) `ENTITIES CREEPER count=0 radius=16`: **no children**. (5) **No SPLIT line.** (6) `BLOCKS STONE count=189 …` after the return | K1-K5; 150 × 60 / 100 = 90 (jshell); a blast is a removal (BL2's reading) | |

### BL3R2 — a blast is a Void death: a stage-1 Phage 3.00 away grows to stage 2, its GROWTH line at three places (SURVIVAL) — supersedes BL3

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl3x <uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl3x uuid> stage=1 gs=100 base=60.000 reach=6.00 knockback=1.00 victims=0 [] spared=1 [creeper/phage <the bl3b uuid> d=3.000 factor=0.375000]`. (3) **Exactly one** `[Rpg] GROWTH mob=phage id=<the bl3b uuid> from=1 to=2 fed=creeper/phage <the bl3x uuid> element=void d=3.000 max=100` (**the same `d` as the spared entry**, K9). (4) `BODY tag=bl3b <uuid> type=creeper mob=phage stage=2 scale=0.882 height=1.500 speedBase=0.3125 speedType=0.2500 speedX=1.250 maxHP=100.000 gs=100 element=void dead=false`. (5) `BODY tag=bl3x none within 16 blocks`. (6) `ENTITIES CREEPER count=1 radius=16`. (7) No HURT | K8, K9; BL3's reading at 2026-11-17 (`d=3.00` then) | |

### BL4R2 — a spared husk and a spared stage 5 inside the reach keep their health AND ARE NOT PUSHED; a control proves the probe sees a velocity (SURVIVAL) — supersedes BL4

| prediction | stands on | READING |
|---|---|---|
| (1) Before: `PROBE tag=bl4h <uuid> husk customHP=100.000/100.000 fireTicks=0 dead=false d=7.280 dy=0.000`, `PROBE tag=bl4p <uuid> creeper customHP=160.000/160.000 fireTicks=0 dead=false d=7.280 dy=0.000`, then `MOTION tag=bl4h <uuid> vx=0.0000 vy=0.0000 vz=0.0000` and `MOTION tag=bl4p <uuid> vx=0.0000 vy=0.0000 vz=0.0000` (INFERRED: a frozen mob spawned still, never travelling). (2) **THE CONTROL:** after the data merge, `MOTION tag=bl4c <uuid> vx=0.0000 vy=0.0000 vz=0.5000` (K7, INFERRED). (3) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl4x uuid> stage=1 gs=100 base=60.000 reach=6.00 knockback=1.00 victims=0 [] spared=2 [<both, either order: husk <the bl4h uuid> d=2.000 factor=0.555556, creeper/phage <the bl4p uuid> d=2.000 factor=0.555556>]`, neither entry carrying knock, res or push. (4) **In the blast's own tick**, `MOTION tag=bl4h … vx=0.0000 vy=0.0000 vz=0.0000` and `MOTION tag=bl4p … vx=0.0000 vy=0.0000 vz=0.0000`: **unchanged, so not pushed.** Had a 1.0 push been applied at 2 blocks, its strength would be 0.666667 (jshell), far above the 0.0001 the probe resolves. (5) Five ticks later the two PROBE lines unchanged. (6) No GROWTH. (7) `ENTITIES CREEPER count=1 radius=16` | K3, K6, K7; `d=7.280` is √53 (jshell); a husk is not a growth candidate and a stage 5 does not grow | |

### BL5R2 — backing away cancels the fuse: nothing blows (SURVIVAL) — supersedes BL5

| prediction | stands on | READING |
|---|---|---|
| (1) `FUSE tag=bl5 <uuid> fuse=10/30 ignited=false waited=<recorded>`. (2) After the step and five ticks, `FUSE tag=bl5 <uuid> fuse=<n>/30 ignited=false` with **n < 10** (a bound; BL5 read 7). (3) `ENTITIES CREEPER count=1 radius=16`. (4) `CMD /kill @e[tag=bl5]`, `SETTLED dying=0 ticks=<t> first=1`, `ENTITIES CREEPER count=0 radius=16`. (5) No BLAST, no HURT. (6) No SPLIT, no GROWTH | BL5's prediction and reading, the steps unchanged (diffed) | |

### BL6R2 — just inside the reach (5.90 blocks): a sliver, and one sixtieth of a push (SURVIVAL) — supersedes BL6

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl6 <uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl6 uuid> stage=1 gs=100 base=60.000 reach=6.00 knockback=1.00 victims=1 [player <Ben's uuid> d=5.900 factor=0.008472 dealt=0.508 knock=0.016667 res=<R> push=<0.016667 × (1 − R), six places>] spared=0 []`. (3) **Exactly one** HURT line `before=0.508`, `dealer=<the bl6 uuid>` | jshell, from `d = 5.9000000000000060` (BL6's measured start): factor 0.008472, dealt 0.508, knock 0.016667 | |

### BL7R2 — just beyond the reach (6.05 blocks): neither hit nor pushed (SURVIVAL) — supersedes BL7

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl7 <uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl7 uuid> stage=1 gs=100 base=60.000 reach=6.00 knockback=1.00 victims=0 [] spared=0 []`. (3) **No HURT line.** BL6R2 is the control | BL7's reading; K1: 0 beyond the reach | |

### BL8R2 — flint and steel's route still cannot light a Phage (SURVIVAL) — supersedes BL8

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE ignite tag=bl8 <uuid>`, then `[Rpg] CREEPERGUARD ignite cancelled mob=phage id=<the bl8 uuid>`. (2) Forty ticks later `FUSE tag=bl8 <uuid> fuse=0/30 ignited=false`. (3) `ENTITIES CREEPER count=1 radius=16`. (4) No BLAST | BL8's reading, the steps unchanged | |

### BL9R2 — at half the reach (3.00): three eighths of 60, and half of vanilla's push (SURVIVAL) — supersedes BL9

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl9 <uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl9 uuid> stage=1 gs=100 base=60.000 reach=6.00 knockback=1.00 victims=1 [player <Ben's uuid> d=3.000 factor=0.375000 dealt=22.500 knock=0.500000 res=<R> push=<0.5 × (1 − R)>] spared=0 []`. (3) **Exactly one** HURT line `before=22.500`, `dealer=<the bl9 uuid>` | jshell: falloff(3) 0.375, × 60 = 22.5; knock 0.5 exactly | |

---

## THE SPLIT ROWS — SURVIVAL, DIFFICULTY NORMAL

**Before each SPLIT line, exactly two `CHILD parent=<that SPLIT's parent uuid> child=<uuid> frozen=true`, the two child
uuids the SPLIT line's two. A kill step logs `CMD /kill @e[tag=<t>]` and then `SETTLED dying=0 ticks=<t> first=1`.**

### DS1R4 — split by the player's emberblade at gear score 60; thawed by the instrument; each child blows on him, and a grown one's GROWTH distance EQUALS its sibling's spared distance (SURVIVAL) — supersedes DS1R3

**What changed from DS1R3, and why:** the gear score is **60** (survival: 36 + 49.5 = 85.5 < 100; at 200 it would be
120 + 165), so `rpg mobdamage 95` brings the stage 5's 96 to 1. The thaw is the instrument's `thaw: ds1` (K10). And the
reading that failed on 2026-11-17 (the second run: spared `d=0.737`, GROWTH `d=0.75`) is predicted to agree now (K8,
K9).

| prediction | stands on | READING |
|---|---|---|
| (1) `REPLY "Spawned Phage (creeper, stage 5, 96 HP, GS 60 given)" [aqua, gray]` and `REPLY "CREEPER damaged: 1/96 custom HP" [green]`. (2) `DRIVE attack Player#attack target=<the ds1 uuid> creeper d=3.000 charge=1.000`, the two `CHILD … frozen=true` lines, then **exactly one** `[Rpg] SPLIT mob=phage parent=<the ds1 uuid> stage=5 gs=60 children=2 childStage=1 [<A> dx=-0.50, <B> dx=0.50]`. (3) `ENTITIES CREEPER count=2 radius=16`. (4) **Exactly two** BODY lines `BODY tag=ds1 <uuid> type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=48.000 gs=60 element=void dead=false`, the uuids A and B. (5) `REPLY "Custom HP: <M>/<M>" [green]`, then **two** `DRIVE thaw tag=ds1 <A or B> ai=true` (one each, either order). (6) **Within the 200 ticks, exactly two BLAST lines, one `id=<A>`, one `id=<B>`, either order; call them X then Y.** X: `stage=1 gs=60 base=36.000 reach=6.00 knockback=1.00 victims=1 [player <Ben's uuid> d=<D1> factor=<F1> dealt=<36 × F1> knock=<1 − D1/6> res=<R> push=<…>] spared=<n> […]`, with **n = 1, naming `creeper/phage <Y> d=<d> factor=<falloff(d)>`, exactly when Y stood within 6 of X**. **If n = 1:** exactly one `[Rpg] GROWTH mob=phage id=<Y> from=1 to=2 fed=creeper/phage <X> element=void d=<d> max=60` after X's BLAST, **its `d` printed IDENTICALLY to X's spared entry's `d`, three places to three places** (K9), and Y's BLAST `stage=2 gs=60 base=49.500 …`. **If n = 0:** no GROWTH line, and Y's `stage=1 gs=60 base=36.000 …`. Either way Y's is `victims=1 [player <Ben's uuid> d=<D2> …] spared=0 []`. Each F, K and P by S10 and K1; **the bound D < 3**. (7) **Exactly two** HURT lines, `before=` each `dealt`, dealer X then Y. (8) `ENTITIES CREEPER count=0 radius=16`. (9) One SPLIT; no `CREEPERGUARD prime` | (1)-(4) DS1R3's reading at GS 60: 160 × 0.6 = 96, 80 × 0.6 = 48, stage 2's 100 × 0.6 = 60, 60 × 0.6 = 36, 82.5 × 0.6 = 49.5 (jshell); K8-K11. **The timing depends on vanilla's AI**, INFERRED from DS1R3's reading (blasts 2 and 3 seconds after the thaw). **Not covered:** X and Y in the SAME tick; and Y walking out of 6 between the blast and its growth tick is now harmless (K8), which is the defect this row's (6) closes | |

### DS2R4 — a stage 5 killed by `/kill` splits; its children are born frozen (SURVIVAL) — supersedes DS2R3

| prediction | stands on | READING |
|---|---|---|
| `CMD /kill @e[tag=ds2]`, the two `CHILD parent=<the ds2 uuid> … frozen=true` lines, **exactly one** SPLIT line `parent=<the ds2 uuid> stage=5 gs=300 children=2 childStage=1 [<uuid> dx=-0.50, <uuid> dx=0.50]`, `SETTLED dying=0 ticks=<t> first=1`, `ENTITIES CREEPER count=2 radius=16`, **exactly two** BODY lines `tag=ds2`, each `type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`. **No BLAST, no HURT** | DS2R3's reading, the steps unchanged | |

### DS5R4 — once only: two after the split, still two forty ticks later (SURVIVAL) — supersedes DS5R3

| prediction | stands on | READING |
|---|---|---|
| The two `CHILD … frozen=true` lines, **exactly one** SPLIT line (`parent=<the ds5 uuid> stage=5 gs=300 children=2 childStage=1 …`), and `ENTITIES CREEPER count=2 radius=16` **twice**. **No BLAST, no HURT** | DS5R3's reading, the steps unchanged | |

### DS6R4 — the row-start clean removes DS5R4's children (SURVIVAL) — supersedes DS6R3

| prediction | stands on | READING |
|---|---|---|
| `CLEAN blocks=756 removed=2 inventory=kept …`, `LEFTOVER living=0 radius=16`, `ENTITIES CREEPER count=0 radius=16` | DS6R3's reading; DS6R4 directly after DS5R4 | |

### DG6R4 — a stage 5's death grows the bystander to 3, at three places (SURVIVAL) — supersedes DG6R3

| prediction | stands on | READING |
|---|---|---|
| (0) The two `CHILD parent=<the dg6p uuid> … frozen=true`. (1) **Exactly one** SPLIT `parent=<the dg6p uuid> stage=5 gs=300 children=2 childStage=1 [<uuid> dx=-0.50, <uuid> dx=0.50]`. (2) **Exactly one** `[Rpg] GROWTH mob=phage id=<the dg6b uuid> from=2 to=3 fed=creeper/phage <the dg6p uuid> element=void d=2.000 max=360`; none names a child. (3) A stage-3 BODY for dg6b (`scale=1.176 height=2.000 speedBase=0.2500 speedType=0.2500 speedX=1.000 maxHP=360.000 gs=300`). (4) Two stage-1 BODY lines for dg6p, `maxHP=240.000 gs=300`. (5) `ENTITIES CREEPER count=3 radius=16`. (6) No BLAST, no HURT | DG6R3's reading (`d=2.00` then); K9 | |

### DG9R4 — a Phage grown to 5, then killed, splits into two frozen children (SURVIVAL) — supersedes DG9R3

| prediction | stands on | READING |
|---|---|---|
| **Exactly one** `[Rpg] GROWTH mob=phage id=<the dg9 uuid> from=4 to=5 fed=enderman <uuid> element=void d=2.000 max=480`, then a stage-5 BODY for dg9. After its kill: the two `CHILD parent=<the dg9 uuid> … frozen=true`, **exactly one** SPLIT `parent=<the dg9 uuid> stage=5 gs=300 children=2 childStage=1 […]`, two stage-1 BODY lines for dg9 (`maxHP=240.000 gs=300`), `ENTITIES CREEPER count=2 radius=16`. No BLAST, no HURT | DG9R3's reading (`d=2.00` then); K9 | |

---

## BEN'S ROW — BY THE ASK STEP. EACH ANSWER IS ONE WORD

### BQ1R2 — the push, and the small and big blasts' damage, by his own eyes (SURVIVAL) — supersedes BQ1

A frozen stage-1 Phage is blown exactly 2 blocks east of him; he is put back, healed, and a frozen stage 5 is blown at
the same spot. **Gear score 100, so the authored 60 and 150, times 0.555556 at 2 blocks: 33.333 and 83.333 before his
defence** (jshell); each pushes him with strength 0.666667 × (1 − his resistance). 2 blocks is where every live Phage
blew on 2026-11-17, so this is the hit he would meet. **Each question waits 1200 ticks for his next chat line, and his
answer cell is his line, verbatim.**

| prediction | READING |
|---|---|
| Beside the answers, recorded: the small Phage's BLAST `stage=1 gs=100 base=60.000 … d=2.000 factor=0.555556 dealt=33.333 knock=0.666667 res=<R> push=<…>` and its HURT `before=33.333`; the big one's `stage=5 gs=100 base=150.000 … d=2.000 factor=0.555556 dealt=83.333 knock=0.666667 …` and HURT `before=83.333` | |
| `ASK 1 question="Did the blast push you back?" answer="<his line>" ticks=<t>` | |
| `ASK 2 question="Your design says the small Phage explodes 'dealing a little damage'. Watch your health bar: did that feel like a little?" answer="<his line>" ticks=<t>` | |
| `ASK 3 question="Your design says the big Phage explodes 'dealing a lot of damage'. Watch your health bar: did that feel like a lot?" answer="<his line>" ticks=<t>` | |

"not sure", or no line within the minute (`answer=NOT STATED after 1200 ticks`), is recorded as NOT STATED and never
re-asked in this boot. **The questions are asked AFTER each blast**, because the ASK step has no way to speak before
one; they tell him to watch his health bar, which still shows the loss when he reads the question.

---

## WHAT NO ROW CAN STAGE — UNIT-ONLY, AND WHY

- **The multiplier at 0.** No scenario step can change content, so a row cannot set `knockback: 0`.
  `MobBlastTest.theMultiplierAndTheResistanceScaleThePush` reads 0 at three distances, and
  `MobLoaderTest.aMalformedBlastIsRefused` refuses a negative one.
- **A resistance other than Ben's.** No step sets the attribute. The rule is
  `MobBlastTest.theMultiplierAndTheResistanceScaleThePush` (0.333333333 at d = 2, resistance 0.5).
- **A creative victim, and a victim exactly at the reach:** unchanged from `GATE-dragon-blast.md` (the selftest refuses
  a second player and a creative runner; exactly 6.0 cannot be promised in floating point).
- **A live Phage that walks past 6 between a death and its growth tick.** The defect K8 closes (§8 F6). No row can
  time a walk to the tick; the rule is pinned in core (`restage` takes no position) and by the wiring row.
