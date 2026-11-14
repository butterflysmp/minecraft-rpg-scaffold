# GATE — MINING SLICE 1 (S1-a + S1-b, one boot): an iron patch you can mine, with the project pack

**Status: NOT RUN.** Written 2026-11-13, before any boot of this stack. **NO BOOT until the seat has diffed S1-b's code
AND this gate, and Ben says ready.**

**The stack.** `feat/mining-s1a` (PR #182: `bebe9680`, plus the F-A commit `e7308aa4`) is the bottom. `feat/mining-s1b`
is built on it and is the top. **The ONE R0 is this file's, and it binds the boot to the S1-b head**, so one boot reads
both slices (the seat's ruling, 2026-11-13; PLAN-mining.md 5.10). They merge bottom-up after the seat approves.

**AMENDED 2026-11-14, BEFORE ANY BOOT** (the seat's diff of 2026-11-13: G1–G7). Each amended passage says
*amended before any boot*. Nothing in this file has been read; the amendments follow two code fixes (D1: the regrowth
task is re-armed on the next look after a relog; D2: see M-RESPAWN) and the seat's hit budget.

Every prediction below was written **before** any row was read, and **this file is COMMITTED before the boot**.
Readings go **beside** a prediction, never over it. A prediction is not edited once its row has been read. Readings
count only if Ben's LOGIN appears in THIS boot's log.

**Plan:** `docs/plans/PLAN-mining.md` on `docs/mining-plan` (§5 is slice 1). Every number below is either RULED (§1
quotes Ben) or DERIVED there.

```
ROWS 22   R0a R0b R0c / M-PACK M-TRACE M-PLACE M-LOOK M-HIT M-MISS M-UNCHARGED M-CHARGE-17 M-HOLD M-DENY
          M-BREAK M-FULL M-REGROW M-RELOG M-LOGOUT M-RESPAWN M-PROTECT M-SERVER-LIST M-SECOND-PLAYER
          = 3 R0 + 19 M
          git grep -c -E '^### (R0|M-)' <ref> -- docs/gates/GATE-mining-slice1.md
```
(The LAST STEP is a step, not a row. **Amended before any boot, G7:** 20 → 22, adding M-RELOG (G2) and M-RESPAWN
(G4); recounted with the command above, not adjusted by the delta. Sight questions now run Q1–Q27: Q23–Q25 are
M-RELOG's, Q26–Q27 M-RESPAWN's.)

**Ben's sight answers.** Every question Ben answers is numbered **Q1, Q2, ...** and is answered **yes / no** or in **one
word**. "Not sure" is a valid answer and is recorded as NOT STATED, never interpreted.

## GAME MODE

**SURVIVAL on every row** (the seat's ruling).
- Ben ruled players are never in ADVENTURE (PLAN §1), and ADVENTURE never reaches `BlockDamageEvent` (F2).
- CREATIVE breaks a block before `BlockDamageEvent` (F2b), so creative cannot mine at all, and it removes costs (the
  creative-divergence register). **The plan's M-PROTECT had a creative half; it is NOT in this gate**, because the
  seat ruled SURVIVAL on every row. That half rests on F2b and F-M5 (`BlockBreakEvent` is cancelled on slot blocks)
  and stays UNREAD in game.

**Confirm with `/gamemode survival` before the set-up is complete.** A row read in any other mode is VOID.

## CARRIED STATE, READ BEFORE BOOTING

- **The deployed content MUST be refreshed: boot with `./scripts/dev-server.sh --refresh-content`.**
  `saveResource(false)` never overwrites. The last boot (spike 1, `1dbcc156`, 2026-11-12) refreshed
  `run/plugins/Rpg/content` to master-plus-spike content, which this stack's code expects except for its two new
  directories. Without the refresh the new `content/ores/` and `content/vein_sizes/` would still be copied (they are
  missing files), but nothing else would be guaranteed to match this jar. With it, R0c's line is exact.
- **Ben's saved build names `sunder`**, which this stack does not have (it is on the parked `feat/sunder`). At the
  spike boot it produced no `[Rpg]` warning at any of three LOGINs (`GATE-mining-spike.md`, the CARRIED STATE note).
  `FileBuildRepository.load` and `BuildMigrations.migrate` check structure only, and only `BuildPickerMenu` writes
  the build file. **Prediction: no `[Rpg]` warning at LOGIN again. A warning at LOGIN is recorded, not an R0 failure.**
  **Ben should not open the build picker during this boot.**
- **The previous session ended with a clean `/stop`** (spike 1's LAST STEP: L863 Stopping, L865 Disabling Rpg, L895
  All dimensions are saved).
- **Backup (amended before any boot, G5: the seat ruled YES).** Taken 2026-11-14 with no server running, to
  `C:\Users\Neb91\IdeaProjects\rpg-run-backups\2026-11-14-pre-mining-slice1` (`cp -a`; `diff -rq` of each pair
  printed nothing):

  | tree | source files | source bytes | copy files | copy bytes |
  |---|---|---|---|---|
  | `run/plugins/Rpg` | 112 | 345190 | 112 | 345190 |
  | `run/world` | 193 | 204812107 | 193 | 204812107 |

  Instrument: `find <dir> -type f | wc -l` and the sum of `find <dir> -type f -printf '%s\n'`. **If anything boots
  `run/` before this gate's boot, this backup is stale: take a new dated one and record it here.** The previous one
  is `rpg-run-backups\2026-11-12-pre-mining-spike`.
- `run/server.properties` has `require-resource-pack=false` and an empty `resource-pack` (L55–56). This gate does not
  change them.
- `run/plugins/Rpg/config.yml` predates the `mining.dev-pack-on-join` key, and the code reads it with a **false**
  default. So the join toggle is OFF for this boot.

## THE TEST WALL

**`minecraft:deepslate`, 3 wide × 2 high, its north face exposed.** Deepslate has no `attack` behaviour (F-M8), it is
not the surrounding terrain, and it is the spike's tested block. The patch's slot covers exactly these six blocks.

## THE ORDER IN THE ONE BOOT

R0 → set-up → M-PACK → M-TRACE → M-PLACE → M-LOOK → M-SERVER-LIST → M-HIT → M-MISS → M-UNCHARGED → M-CHARGE-17 →
M-HOLD → M-DENY → M-BREAK → M-REGROW → M-FULL → M-RELOG → M-LOGOUT → M-RESPAWN → M-PROTECT → M-SECOND-PLAYER →
LAST STEP. (Amended before any boot: M-RELOG and M-RESPAWN added, G2 and G4; the spot budget is THE HIT BUDGET.)

## Set-up (after R0 passes)

1. `/gamemode survival`, `/difficulty peaceful`, `/gamerule spawn_mobs false`, `/time set 6000`, `/weather clear`.
   The world's difficulty persists from a saved `/difficulty`, so read it live.
2. Stand on flat ground facing **south**. Run **`/fill ~-1 ~ ~3 ~1 ~1 ~3 minecraft:deepslate`**: a wall 3 wide and
   2 high, three blocks south, whose north face looks at you. Clear anything in front of it.
3. **`/give @s minecraft:iron_pickaxe`**: a plain vanilla iron pickaxe, attack speed 1.2 (Ben: "Vanilla speed for
   now is fine"). Hold it for every row unless the row says otherwise.
4. Video settings: **Attack Indicator: Crosshair** (client-side; record what it was).
5. **Ben notes his raw iron count now, before the first hit of the run** (amended before any boot, G1). It is
   M-BREAK's "before" figure, whichever row the first break lands in.

## THE HIT BUDGET (amended before any boot, G1)

A Small patch has **n = 2 or 3** spots (`small.yml`: 2–3). A charged click IN the live spot spends one spot: the
first n − 1 are `HIT`, the n-th is `BROKE`. So **a patch holds n − 1 HITs before it breaks**, and the run must work
for both n.

| row | spots spent | after it, n = 2 | after it, n = 3 |
|---|---|---|---|
| M-HIT | 1 | the last spot is live | 2 left |
| M-MISS, M-UNCHARGED, M-CHARGE-17 | **0** (every click is a MISS or UNCHARGED) | unchanged | unchanged |
| M-HOLD | at most 1 | **may BREAK** (standing rule) | the last spot is live |
| M-DENY step 3 | at most 1 | (on a fresh patch if M-HOLD broke) HIT | **may BREAK** (standing rule) |
| M-BREAK | the rest | breaks | breaks |

**THE STANDING RULE.** If the patch breaks before M-BREAK, **that `MINING break` line IS M-BREAK's reading**
(record it under M-BREAK, naming the row it landed in, with Ben's raw iron count from set-up step 5 as "before"). Then
`/rpg mining regrow` and carry on with the row you were in. M-BREAK is then read again as written on whatever patch is
up; its second break line is recorded as an additional reading, and the inventory question Q15 still refers to the
FIRST break.

**The arithmetic every cooldown prediction uses** (executed, PLAN §5.7): at 1.2 the delay is `16.666666`, a read Δ
ticks after a reset is `(Δ + 0.5) / 16.666666` (Δ=0 → 0.0300, Δ=13 → 0.8100, Δ=16 → **0.9900**), and **the first
full read (1.0000) is at Δ = 17**. A block click's first swing resets the ticker (F1, F-M12).

---

## R0 — THE STACK TOP'S BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the S1-b head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`: **`feat/mining-s1b`'s head as read from `git ls-remote origin refs/heads/feat/mining-s1b` at boot time**, shortened. Not `-dirty`, not `unknown`, not S1-a's `e7308aa4`, not master's `55425aad` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | _(not run)_ |

### R0b — the jar carries both slices and the pack

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `mining/Patch`, `mining/VeinSize`, `mining/MiningBook` (S1-a and S1-b); paper `content/MiningContentLoader` (S1-a); paper `mining/MiningService`, `mining/MiningPack` (S1-b); `content/ores/iron.yml`; `resourcepack/pack.mcmeta`; `resourcepack/assets/rpg/items/mining/iron_patch.json`. ABSENT: the control | the scan below | _(not run)_ |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/mining/Patch.class',
               'io/github/butterflysmp/rpg/core/mining/VeinSize.class',
               'io/github/butterflysmp/rpg/core/mining/MiningBook.class',
               'io/github/butterflysmp/rpg/paper/content/MiningContentLoader.class',
               'io/github/butterflysmp/rpg/paper/mining/MiningService.class',
               'io/github/butterflysmp/rpg/paper/mining/MiningPack.class',
               'content/ores/iron.yml',
               'resourcepack/pack.mcmeta',
               'resourcepack/assets/rpg/items/mining/iron_patch.json',
               'io/github/butterflysmp/rpg/paper/mining/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — content loads, the two mining counts appear, and no Content: warning names an ore

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 14 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes, 1 ores, 1 vein sizes`. **That is spike 1's READ R0c line (`GATE-mining-spike.md`, L44 of that boot, master's content) with S1-a's two counts appended**; neither slice adds any other content file. **No `Content:` line names `ore 'iron'`**: its sizes load, `raw_iron` is an item, both sounds and `minecraft:poof` resolve, and all three models are in the jar's pack (S1-b's in-pack check, which this row depends on). No `Skipping malformed ore`, `Skipping malformed vein size`, `No ores loaded`, `No vein sizes loaded`, `SEVERE` or exception. The only expected `[Rpg]` WARN lines at enable are `volley_stone`'s known pair | `Select-String -Path run\logs\latest.log -Pattern 'Loaded \d+ abilities','Content:','Skipping','No ores','No vein','SEVERE','Exception'` | _(not run)_ |

---

## THE ROWS

**Reading a MINING line.** Lines are printed only while the trace is on (M-TRACE), except `MINING place`,
`MINING remove`, `MINING trace`, `MINING regrow-all` and the two `MINING pack` lines, which always print. Formats
(pinned by `MiningTraceLineTest`):
- `MINING show player=<p> slot=<id> spots=<u,v;u,v...> live=<i>/<n> tick=<t>`: the spot centres in reveal order,
  in patch pixels (u to the right, v up, from the wall's bottom-left as you face it).
- `MINING click player=<p> slot=<id> outcome=<HIT|BROKE|MISS|UNCHARGED|DENIED|NOT_LIVE> spot=<i>/<n> cooldown=<c> aim=<u>,<v> tick=<t>`:
  `spot` is the live spot BEFORE the click; `aim` is where the server's ray met the wall.
- `MINING break ... payout=raw_ironx<count> rolls=<r1>+<r2>[+<r3>] bonus=<b> kept=<k> dropped=<d> regrow_at=<t>`.
- `MINING regrow player=<p> slot=<id> spots=<n> tick=<t>`.

**A click is IN the live spot when** `cu − 2 ≤ aim.u < cu + 3` and `cv − 2 ≤ aim.v < cv + 3`, where `(cu, cv)` is the
live spot's centre from the latest `show` line (the 5 × 5 box, low edge in, high edge out; `SpotBox`).

### M-PACK — the dev pack is offered and loads (SURVIVAL) — witness: the log + Ben's sight

**Steps.** `/rpg mining pack`. When the client asks, choose **Yes**.

| prediction | READING |
|---|---|
| **From F6 and `MiningPack`.** One `MINING pack offer player=<Ben> url=http://127.0.0.1:<port>/rpg-pack.zip sha1=<40 hex> bytes=<n>`, then `MINING pack status ... status=ACCEPTED`, `DOWNLOADED`, `SUCCESSFULLY_LOADED`, in that order. **No disconnect** (`require-resource-pack=false`). A second `/rpg mining pack` in the same boot logs the **same sha1** (the zip is deterministic: `MiningPackTest`). **The join toggle is OFF** (CARRIED STATE): there is no `MINING pack offer` line at LOGIN. **The toggle ON is NOT READ in this boot**, because it needs a config edit and a restart | _(not run)_ |

**Q1.** Did the client show a prompt to download a pack? (yes/no)
**Q2.** After choosing Yes, did it finish without an error? (yes/no)

### M-TRACE — the trace turns on (SURVIVAL) — witness: the log

**Steps.** `/rpg mining trace`.

| prediction | READING |
|---|---|
| `MINING trace player=<Ben> on=true`, and the chat line "Mining trace ON." Every later row reads lines this enables | _(not run)_ |

### M-PLACE — the slot is placed, and each refusal names its block (SURVIVAL) — witness: the log + chat

**Steps, in this order.**
1. Look at the **top** face of the wall's top-centre block. `/rpg mining place iron small`.
2. Look at the **upper** row's centre block (north face). `/rpg mining place iron small`.
3. Look at the **lower** row's centre block (north face). `/rpg mining place iron small`. **This is the slot.**
4. Same block again. `/rpg mining place iron small`.

| prediction | READING |
|---|---|
| Below, `<x>,<y>,<z>` is the **lower** row's centre block. **1:** red "A patch goes on a wall (north, south, east or west face), not up." No `MINING place` line. **2:** red "Block <x+1>,<y+2>,<z> is not solid.": the footprint runs one row UP from the looked-at block, that row is air, and its first block is the one on the player's LEFT (facing south, left is east, +X; `FaceFrame.footprint` order). No `MINING place`. **3:** green "Placed iron small at <world-uuid>:<x>,<y>,<z>:NORTH.", and one `MINING place slot=<world-uuid>:<x>,<y>,<z>:NORTH ore=iron size=small footprint=<6 blocks> by=<Ben>`, with the six blocks being the wall's. Then a `MINING show ... spots=<2 or 3 centres> live=1/<n>`. **4:** red "a slot already exists at <id>." | _(not run)_ |

### M-LOOK — the patch and the spot on screen (SURVIVAL, pack loaded) — witness: Ben's sight

F-M18: spike 1 never stated the model's size, facing or position. This row reads them, and how the seat's offsets
(0.0005 for the patch, 0.001 for a spot) look.

| prediction | READING |
|---|---|
| **From F5 and `MiningService`.** A flat placeholder patch (grey with tan flecks, a dark border) covering the whole 3 × 2 north face; ONE yellow spot with a dark border, 3/16 of a block square, at the first `show` centre; no flicker between the patch and the wall or the spot. **MEASUREMENT (client):** the facing and whether a 48 × 32 texture renders unstretched are client behaviour, and are read here, not predicted | _(not run)_ |

**Q3.** Is the patch as wide as the 3-block wall? (yes/no)
**Q4.** Is it as tall as the 2-block wall? (yes/no)
**Q5.** Is it flat on the wall's face, facing you? (yes/no)
**Q6.** Do you see exactly one small spot on it? (yes/no)
**Q7.** One word for the patch's colour.
**Q8.** Does anything flicker where the patch meets the wall or the spot? (yes/no)

### M-SERVER-LIST — the displays are not server entities (SURVIVAL) — witness: the command output

**Steps.** Within 8 blocks of the wall:
1. `/execute if entity @e[type=minecraft:item_display,distance=..8]`
2. Positive control: `/summon minecraft:item_display ~ ~ ~`, then the same command, then `/kill @e[type=minecraft:item_display,distance=..2]`.

| prediction | READING |
|---|---|
| **1:** "Test failed": the patch and spot are packet-only. **2:** "Test passed, count: 1", then the kill removes it. If 2 fails, 1 proves nothing | _(not run)_ |

### M-HIT — a charged hit cracks the live spot and reveals the next (SURVIVAL) — witness: the log + Ben's sight

**Steps.** Wait at least 1 second, then left-click the spot once.

| prediction | READING |
|---|---|
| One `MINING click ... outcome=HIT spot=1/<n> cooldown=1.0000 aim=<u>,<v>` with the aim IN the first `show` centre's box. The aim is read at the dig START (PLAN 5.4); spike 1's single-click aims were within 0.86 px of the centre. Then the spot becomes a crack and a second spot appears, and a click on the crack later is a MISS | _(not run)_ |

**Q9.** Did the spot change to a crack? (yes/no)
**Q10.** Did a new spot appear somewhere else on the patch? (yes/no)

### M-MISS — a charged click off the live spot changes nothing and plays the miss sound (SURVIVAL) — witness: the log + Ben's sight

**Steps.** Wait 1 second, then left-click the patch well away from the live spot (not on the crack's box either).

| prediction | READING |
|---|---|
| `MINING click ... outcome=MISS cooldown=1.0000` with the aim OUTSIDE the live spot's box. Nothing changes. **The miss sound plays to Ben only** (`sounds.miss`, the PLACEHOLDER `minecraft:block.stone.hit`). An uncharged click beside the spot is also a MISS and also plays it (the order of checks; `PatchTest.aimOutsideAndUnchargedIsAMiss`) | _(not run)_ |

**Q11.** Did you hear a sound when you missed? (yes/no)
**Q12.** Did anything on the patch change? (yes/no)

### M-UNCHARGED — an early click on the live spot changes nothing and plays nothing (SURVIVAL) — witness: the log + Ben's sight

**Steps (amended before any boot, G1: this row spends NO spot).** Left-click the patch **beside** the live spot
(outside its box, and off any crack). Then, **within half a second**, left-click **ON** the live spot.

| prediction | READING |
|---|---|
| The first: `MINING click ... outcome=MISS` (its swing resets the ticker, F1). The second: `MINING click ... outcome=UNCHARGED cooldown=<(Δ+0.5)/16.666666>`, Δ being its tick minus the first's (under 10 ticks, so under 0.6300), with the aim IN the live spot's box and `spot=` the SAME index as the MISS line's. **An UNCHARGED click plays nothing**, and nothing changes: the live spot stays live. (If the second click lands outside the box it is a MISS with the miss sound, recorded as such, and the row is repeated) | _(not run)_ |

**Q13.** On the fast second click on a spot, did you hear a sound? (yes/no)

### M-CHARGE-17 — the full-charge boundary spike 1 never isolated (SURVIVAL) — witness: the log

**Steps (amended before any boot, G1: this row spends NO spot).** Every click line carries the cooldown whatever
its outcome, so the boundary is read on clicks **beside** the live spot. Make **20 clicks on the patch away from the
live spot** (and off any crack), each one **the instant the crosshair meter looks full**.

| prediction | READING |
|---|---|
| 20 `MINING click ... outcome=MISS` lines. Δ is each line's tick minus the previous click line's. **At Δ = 16 the line reads `cooldown=0.9900`; at Δ = 17 it reads `cooldown=1.0000`.** **If neither Δ occurs, the boundary is NOT WITNESSED**, recorded as such and not as a pass. About 10% of spike 1's reads sat one tick off the logged tick difference (the instrument phase); that is recorded, not a failure. **What this row does not read:** the mapping from a cooldown to HIT or UNCHARGED (`ChargeRule`: exactly 1.0f) is unit-tested (`SpotBoxAndChargeTest`), and is read in game by M-HIT (`1.0000` → HIT) and M-UNCHARGED (below 1 → UNCHARGED) | _(not run)_ |

### M-HOLD — holding left-click on the live spot hits at most once (SURVIVAL) — witness: the log

**Steps.** After a 1-second wait, hold left-click on the live spot for 3 seconds.

| prediction | READING |
|---|---|
| **Exactly one `MINING click` line for the whole hold**: the dig START's `BlockDamageEvent` (MS3 read one DIG_START for a 58-swing hold), which is the first click and may be a HIT, **or BROKE if it is the last spot (n = 2): then THE STANDING RULE applies** (amended before any boot, G1). No line for any held swing. The deepslate does not break (no block change; the crack and flicker Ben saw in spike 1 are a client visual he accepted, F-M14) | _(not run)_ |

### M-DENY — no pickaxe, no mining; any pickaxe will do (SURVIVAL) — witness: the log + Ben's sight

**Steps.** 1. Empty hand: wait 1 s, click the live spot. 2. `/give @s minecraft:iron_sword`, hold it, wait 1 s, click the live spot. 3. Back to the vanilla iron pickaxe: wait 1 s, click the live spot.

| prediction | READING |
|---|---|
| **1 and 2:** `MINING click ... outcome=DENIED`, and the action bar shows "You need a pickaxe to mine this." in red. Nothing changes, no sound. **3:** `HIT` (or `BROKE`, and then THE STANDING RULE applies; amended before any boot, G1): any item in `#minecraft:pickaxes` counts, minted or not (the seat's ruling 7) | _(not run)_ |

**Q14.** With an empty hand, did a red message appear above the hotbar? (yes/no)

### M-BREAK — the last spot breaks the patch and pays raw iron (SURVIVAL) — witness: the log + Ben's sight + inventory

**Steps.** With room in the inventory, keep making charged hits on the live spot until the patch breaks. Note the raw
iron count after; "before" is set-up step 5's count. **If an earlier row already broke a patch, THE STANDING RULE
says which break this row's reading is** (amended before any boot, G1).

| prediction | READING |
|---|---|
| The last hit logs `MINING click ... outcome=BROKE`, then `MINING break ... payout=raw_ironx<count> rolls=<one per spot> bonus=5 kept=<count> dropped=0 regrow_at=<tick + 1200>`. **Each roll is 2, 3 or 4; the count is their sum + 5, inside 9..17** (9..13 for 2 spots, 11..17 for 3; Ben: "2-4 per spot with a 5 completion bonus"). The inventory gains exactly `count` raw iron. Every mark goes; the break sound (`minecraft:block.stone.break`, PLACEHOLDER) and a poof (PLACEHOLDER) play to Ben only. A click straight after logs `NOT_LIVE` and plays nothing | _(not run)_ |

**Q15.** How many raw iron did you gain? (one number)
**Q16.** Did the patch disappear? (yes/no)
**Q17.** Did you hear a break sound? (yes/no)
**Q18.** Did you see a puff of particles? (yes/no)

### M-REGROW — the patch returns after 1200 ticks, re-rolled (SURVIVAL) — witness: the log + Ben's sight

**Steps.** Stay near the wall and wait about 60 seconds after M-BREAK.

| prediction | READING |
|---|---|
| At tick ≥ the break line's `regrow_at`, `MINING show ... live=1/<n>` and `MINING regrow ... spots=<n>`. **The `spots=` centres differ from the previous `show` line's** (re-rolled; a repeat is possible but unlikely, and is recorded). Ben sees a fresh patch with one spot. (Ben: "1 minute is fine for now"; it counts server ticks, so under 20 TPS it is longer.) | _(not run)_ |

**Q19.** Did the patch come back by itself? (yes/no)

### M-FULL — a full inventory drops the remainder at your feet (SURVIVAL) — witness: the log + Ben's sight

**Steps.** 1. `/clear @s minecraft:raw_iron`, so no part-stack of raw iron can absorb the payout. 2. Fill every free
inventory slot: `/give @s minecraft:cobblestone 2304` (36 stacks; the extra lands on the ground, and a full inventory
picks nothing up). 3. `/rpg mining regrow` if the patch is not up. 4. Break the patch.

| prediction | READING |
|---|---|
| `MINING regrow-all player=<Ben> shown=<0 or 1>`. The break line reads **`kept=0 dropped=<count>`** with `kept + dropped = count` (the seat's ruling: the remainder drops at the player's feet, as vanilla does). The raw iron lies at Ben's feet | _(not run)_ |

**Q20.** Did raw iron drop on the ground at your feet? (yes/no)

**Clean-up, in this order (amended before any boot, G6)**, so the next rows start clean. M-BREAK's inventory count
was taken BEFORE this row (set-up step 5 and the first break), and step 1's `/clear` only happens after it.
1. Answer Q20 first.
2. **`/kill @e[type=minecraft:item,distance=..16]`** while the inventory is still full, so nothing is picked up in
   between: it removes the dropped raw iron and the cobblestone that `/give` spilled. It kills item entities only.
   Positive control: the chat reports `Killed <n> entities` with n ≥ 1 (if it reports none, the row's drop was not
   where predicted; record it).
3. `/clear @s minecraft:cobblestone`.
4. `/execute if entity @e[type=minecraft:item,distance=..16]` reads **"Test failed"**: no item is left on the ground.

### M-RELOG — a part-mined patch is kept across logout (SURVIVAL) — witness: the log + Ben's sight

**New row, amended before any boot (G2).** It is the only row that reads the chunk-load path
(`PlayerChunkLoadEvent` → `MiningService.onChunkLoad` → `show`) and the seat's ruling 4, *a part-mined patch is kept
across logout*.

**Steps.** 1. `/rpg mining regrow` if no patch is up. 2. Wait 1 second, then left-click the live spot once (a fresh
patch has n ≥ 2, so this is a `HIT`: one crack and a live spot). Note the `MINING show` line that is now the latest,
and the HIT line's `spot=`. 3. Disconnect, and reconnect. 4. Stand where the wall is in view; do NOT click it.

| prediction | READING |
|---|---|
| After the reconnect, one `MINING show player=<Ben> slot=<id> spots=<...> live=2/<n>` line whose **`spots=` centres are the SAME as the last `show` line before the disconnect** and whose live index is the one after the HIT (`live=2/<n>`). No `MINING click` line between the reconnect and that show. The trace stays ON across the reconnect (it is keyed by UUID and a quit does not clear it). By sight: the patch, ONE crack and ONE live spot, in the places they were | _(not run)_ |

**Q23.** After reconnecting, was the patch back on the wall? (yes/no)
**Q24.** Was the crack still there? (yes/no)
**Q25.** Was there exactly one live spot, where it was before? (yes/no)

### M-LOGOUT — regrowth is kept across logout (SURVIVAL) — witness: the log + Ben's sight

**Steps.** 1. Break the patch (`/rpg mining regrow` first if needed). 2. Disconnect within 20 seconds and reconnect
within 40 seconds of the break. 3. Look at the wall. 4. Wait until 60 seconds after the break. **Do not click the wall
between reconnecting and `regrow_at`, or the row is VOID** (amended before any boot, G3): a click calls `show` itself
and would read the click path, not the patch returning by itself.

| prediction | READING |
|---|---|
| On reconnecting before `regrow_at`: **no patch** and no `MINING show` line (the clock is kept across logout; the seat's ruling 4). After `regrow_at`: the patch appears **by itself** and a `MINING show` line prints. The trace stays ON across the reconnect. (If the reconnect lands after `regrow_at`, the first half is NOT WITNESSED.) **Why it is true now (amended before any boot, G3):** the quit drops the break's regrowth task (F8c); on reconnect the chunk load reaches `show`, which finds the patch regrowing and arms ONE new task on the new session's Player (D1). Before D1 the wall stayed bare until a click | _(not run)_ |

**Q21.** When you came back, was the wall bare at first? (yes/no)

### M-RESPAWN — the patch comes back after a dimension change (SURVIVAL) — witness: the log + Ben's sight

**New row, amended before any boot (G4: the seat's D2).** The question: after the client drops its entities, does
`show` re-send, or does a stale `shown` entry hide the patch until relog? **From the pinned jar (PLAN F8), no code
changed:** a dimension change AND a same-world respawn both remove the player from the old level's chunk loader,
which fires `PlayerChunkUnloadEvent` for every sent chunk, naming the OLD world (`CraftEntity#getWorld` reads the
level, and `ServerPlayer#teleport` sets the new level at offset 534, after the removal at 467; `PlayerList#respawn`
removes at 77 and calls `spawnIn` at 180). `onChunkUnload` therefore clears `shown`, and the re-add sends every chunk
again with `PlayerChunkLoadEvent`, which reaches `show`. This row reads that in game, for the dimension half.

**THE STAGING, AND WHY IT IS THE SAFEST ONE.** Ben's inventory holds his Ability Stone and Nexus star, and the plugin
has its own death rules, so **nothing in this row may risk a death.**
- **No death is staged at all.** A same-world respawn needs a death, so **the respawn half is NOT READ** in this
  boot, for that reason; it rests on F8a/F8b/F8c only. Never recorded as a pass.
- **The dimension change goes to the NETHER ROOF**, not to a blind spot: `y = 127` is the nether's top bedrock layer,
  so `y = 128` stands on it. Difficulty is peaceful (set-up step 1), so nothing hostile spawns. No portal is used (a
  portal can place the player beside lava).
- **Insurance before leaving:** `/effect give @s minecraft:resistance 120 4 true` and
  `/effect give @s minecraft:fire_resistance 120 0 true` (resistance V cancels damage).
- **The way back is a command, not a walk**, to the set-up spot read from the slot id.
- If anything looks wrong on arrival (not standing on bedrock, any damage), run step 4 at once and record the row NOT
  READ with what was seen.

**Steps.** 1. Note the latest `MINING show` line (the patch is up after M-LOGOUT). 2. The two effects above.
3. `/execute in minecraft:the_nether run tp @s 0.5 128 0.5`. Wait 5 seconds; confirm the feet are on bedrock.
4. `/execute in minecraft:overworld run tp @s <x> <y> <z−3> 0 0`, where `<x>,<y>,<z>` is the slot id's block (the
   lower row's centre) and `0 0` faces south, at the wall. 5. Do NOT click the wall until the readings are taken.

| prediction | READING |
|---|---|
| **Step 3:** no `MINING` line (the unloads clear `shown` and log nothing). Ben takes no damage. **Step 4:** within a few seconds of arriving, one `MINING show player=<Ben> slot=<id>` line whose `spots=` and `live=` equal step 1's line, and no `MINING click` line before it. By sight: the patch and its spot are back. **If no `show` line prints and the wall is bare, D2 is real** (a stale `shown`): FAIL, recorded, and a relog is the recovery | _(not run)_ |

**Q26.** On the nether roof, did you take any damage? (yes/no)
**Q27.** Back at the wall, without clicking, was the patch there? (yes/no)

### M-PROTECT — the slot's blocks cannot be broken in survival (SURVIVAL) — witness: the log + Ben's sight

**Steps.** With the vanilla iron pickaxe, hold left-click on a footprint block **away from the patch's live spot** for
10 seconds (deepslate breaks in about 2 seconds with an iron pickaxe when nothing protects it).

| prediction | READING |
|---|---|
| The block is still deepslate afterwards. No block change in the log. The client may show a crack or a brief vanish and return (F-M14, accepted by Ben as a client visual and deferred). **The creative half of the plan's M-PROTECT is not in this gate** (GAME MODE) | _(not run)_ |

**Q22.** After 10 seconds, was the block still there? (yes/no)

### M-SECOND-PLAYER — each player sees their own patch (SURVIVAL) — witness: a second client

| prediction | READING |
|---|---|
| **Only if a second client is available.** The second player sees their own fresh patch at the slot, with its own spots, not Ben's progress; Ben's sounds are not heard by them. **Otherwise this row is NOT READ**, and that is recorded, never treated as a pass | _(not run)_ |

### THE REMOVE STEP (inside the LAST STEP's run-up; not a row)

`/rpg mining remove`, looking at the wall: green "Removed <id>.", `MINING remove slot=<id> by=<Ben>`, the marks
vanish, and the deepslate then digs normally. Recorded as a reading under the LAST STEP.

---

## LAST STEP — a clean /stop

| prediction | READING |
|---|---|
| `/stop` from the console. The log shows `Stopping`, `Disabling Rpg`, and `All dimensions are saved`, with no exception between them. No `java.exe` remains afterwards (`Get-Process java -ErrorAction SilentlyContinue` prints nothing). The 127.0.0.1 pack server stops with the plugin (`MiningService.shutdown`) | _(not run)_ |
