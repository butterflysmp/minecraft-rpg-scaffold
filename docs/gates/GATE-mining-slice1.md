# GATE — MINING SLICE 1 (S1-a + S1-b, one boot): an iron patch you can mine, with the project pack

**Status: NOT RUN.** Written 2026-11-13, before any boot of this stack. **NO BOOT until the seat has diffed S1-b's code
AND this gate, and Ben says ready.**

**The stack.** `feat/mining-s1a` (PR #182: `bebe9680`, plus the F-A commit `e7308aa4`) is the bottom. `feat/mining-s1b`
is built on it and is the top. **The ONE R0 is this file's, and it binds the boot to the S1-b head**, so one boot reads
both slices (the seat's ruling, 2026-11-13; PLAN-mining.md 5.10). They merge bottom-up after the seat approves.

**AMENDED 2026-11-14, BEFORE ANY BOOT** (the seat's diff of 2026-11-13: G1–G7). Each amended passage says
*amended before any boot*. Nothing in this file has been read; the amendments follow two code fixes (D1: the regrowth
task is re-armed on the next look after a relog; D2: see M-RESPAWN) and the seat's hit budget.

**READ 2026-11-14** (the seat's approval of 2026-11-14 for `863bf10a` only, on Ben's "ready"). One boot,
`./scripts/dev-server.sh --refresh-content`, `[Rpg] Build: 863bf10a`; Ben's LOGIN at L79 of that boot's
`run/logs/latest.log` (05:25:35), `/stop` at L242 (05:42:19). Every `L<n>` below is a line of that log. Ben's sight
answers were relayed by the seat with his words quoted, and are recorded as given. Readings sit beside the
predictions, which are unchanged.

**THE READING IN ONE PLACE.** R0 PASS. **PASS:** M-TRACE, M-LOOK, M-HIT, M-CHARGE-17 (Δ = 16 and Δ = 17 both
witnessed), M-BREAK (two breaks; the first landed in M-HOLD under THE STANDING RULE), M-REGROW, M-FULL, M-LOGOUT
(D1 witnessed), M-PROTECT (by sight), LAST STEP. **Log half PASS, a half not read:** M-PACK (no prompt: NOT AS
PREDICTED), M-PLACE (refusal texts NOT READ), M-MISS, M-UNCHARGED (sound NOT READ), M-DENY (action bar a DEFECT),
M-RELOG (sight NOT STATED), M-RESPAWN (dimension half PASS; same-world respawn NOT READ). **NOT AS PREDICTED:**
M-HOLD (two click lines in the hold), M-PACK's prompt, M-DENY's action bar. **NOT READ:** M-SERVER-LIST (the test
results were not logged or relayed), M-SECOND-PLAYER (no second client). **AN UNPLANNED DEATH** in M-RESPAWN
(`/kill` at L226), recorded under that row with what Ben's player data shows.

**CARRIED TO THE NEXT BOOT, as hand rows** (the seat, 2026-11-14): **the SOUND halves of M-MISS, M-UNCHARGED and
M-BREAK** (miss sound plays; an uncharged click plays nothing; the break sound plays), NOT READ here because Ben's
audio was off (Q11, Q13, Q17). Never a pass. Also not read in this boot, and listed so they are not mistaken for
passes: the join toggle's ON half (M-PACK), the same-world respawn (M-RESPAWN), M-SERVER-LIST, M-SECOND-PLAYER, the
M-PLACE refusal texts, M-FULL's final ground check, and the remove step's "digs normally".

**ADDENDUM 2026-11-14, after the readings commit `9eb45815`** (Ben's words relayed by the seat; nothing above is
changed). To the four follow-up questions (Q23–Q25; M-PLACE's refusal texts; the M-SERVER-LIST and M-FULL chat
outputs; the missing pickaxes), Ben: *"questions 1-4 not sure and skip"*. On the pack after reconnecting, Ben: *"The
pack did drop and I had to re do it on join that's on my end. It looks like purple and black squares until i turned
it back on"*.
- **FINDING (the seat's miss): the project pack is NOT loaded after a reconnect until `/rpg mining pack` is run
  again**, and until then the patch shows as *"purple and black squares"*. It is **not** Ben's end:
  `mining.dev-pack-on-join` is false in `run/`, so nothing offers the pack at LOGIN, exactly as M-PACK predicted.
  The relog rows did not account for it. **Which sights had the pack** (offer and status times against each row's
  show lines) is recorded under M-RELOG, M-LOGOUT and M-RESPAWN. Their log halves stand on their own.
- **For the next boot of this stack (CARRIED, nothing changed now):** set `mining.dev-pack-on-join: true` in
  `run/plugins/Rpg/config.yml` (Ben's local file, not the repo default). That also lets the toggle's ON half be read.

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
| `[Rpg] Build: <sha>`: **`feat/mining-s1b`'s head as read from `git ls-remote origin refs/heads/feat/mining-s1b` at boot time**, shortened. Not `-dirty`, not `unknown`, not S1-a's `e7308aa4`, not master's `55425aad` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS.** L42 `[Rpg] Build: 863bf10a`: the S1-b head as read from `git ls-remote origin refs/heads/feat/mining-s1b` before the boot (`863bf10aa18cab36d425d5cf0c327884e514dc94`). Not `-dirty`, not `unknown` |

### R0b — the jar carries both slices and the pack

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `mining/Patch`, `mining/VeinSize`, `mining/MiningBook` (S1-a and S1-b); paper `content/MiningContentLoader` (S1-a); paper `mining/MiningService`, `mining/MiningPack` (S1-b); `content/ores/iron.yml`; `resourcepack/pack.mcmeta`; `resourcepack/assets/rpg/items/mining/iron_patch.json`. ABSENT: the control | the scan below | **PASS.** All nine PRESENT; `NoSuchClassControl.class` ABSENT. Read before Ben's LOGIN |

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
| `Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 14 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes, 1 ores, 1 vein sizes`. **That is spike 1's READ R0c line (`GATE-mining-spike.md`, L44 of that boot, master's content) with S1-a's two counts appended**; neither slice adds any other content file. **No `Content:` line names `ore 'iron'`**: its sizes load, `raw_iron` is an item, both sounds and `minecraft:poof` resolve, and all three models are in the jar's pack (S1-b's in-pack check, which this row depends on). No `Skipping malformed ore`, `Skipping malformed vein size`, `No ores loaded`, `No vein sizes loaded`, `SEVERE` or exception. The only expected `[Rpg]` WARN lines at enable are `volley_stone`'s known pair | `Select-String -Path run\logs\latest.log -Pattern 'Loaded \d+ abilities','Content:','Skipping','No ores','No vein','SEVERE','Exception'` | **PASS.** L43 is the predicted line exactly, ending `… 1 mobs, 3 recipes, 1 ores, 1 vein sizes`. No `Content:` line names `ore 'iron'`; no `Skipping`, `No ores`, `No vein`, `SEVERE` or exception. The only `[Rpg]` WARN lines are `volley_stone`'s known pair (L44, L45). No `[Rpg]` warning at any of Ben's three LOGINs (L79, L197, L211) |

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
| **From F6 and `MiningPack`.** One `MINING pack offer player=<Ben> url=http://127.0.0.1:<port>/rpg-pack.zip sha1=<40 hex> bytes=<n>`, then `MINING pack status ... status=ACCEPTED`, `DOWNLOADED`, `SUCCESSFULLY_LOADED`, in that order. **No disconnect** (`require-resource-pack=false`). A second `/rpg mining pack` in the same boot logs the **same sha1** (the zip is deterministic: `MiningPackTest`). **The join toggle is OFF** (CARRIED STATE): there is no `MINING pack offer` line at LOGIN. **The toggle ON is NOT READ in this boot**, because it needs a config edit and a restart | **Log half PASS; the prompt NOT AS PREDICTED.** L94 `MINING pack offer … url=http://127.0.0.1:58412/rpg-pack.zip sha1=1e11f1c1e75b20cb547c34de0734812ca7ad2134 bytes=3225`, then ACCEPTED (L95), DOWNLOADED (L96), SUCCESSFULLY_LOADED (L97), in that order. No disconnect. No `MINING pack offer` at any LOGIN (the toggle is OFF). Two later offers (L200 at 05:36:57, L221 at 05:38:18) log the **same sha1**. **The client did not ask**: Ben, Q1/Q2, "it auto accepted, didn't prompt me". The step said "when the client asks". Likely cause, UNREAD: the client's per-server pack setting from the 2026-11-12 spike boot. **The join toggle's ON half is NOT READ** (as planned) |

**Q1.** Did the client show a prompt to download a pack? (yes/no) **Ben: "No, I ran the command and it auto accepted"** (NOT AS PREDICTED: no prompt)
**Q2.** After choosing Yes, did it finish without an error? (yes/no) **Ben: "Yes it finished but it auto accepted, didn't prompt me"**

### M-TRACE — the trace turns on (SURVIVAL) — witness: the log

**Steps.** `/rpg mining trace`.

| prediction | READING |
|---|---|
| `MINING trace player=<Ben> on=true`, and the chat line "Mining trace ON." Every later row reads lines this enables | **PASS** (log). L99 `MINING trace player=BaronVonYeetus on=true`. The chat line "Mining trace ON." is not logged: NOT READ |

### M-PLACE — the slot is placed, and each refusal names its block (SURVIVAL) — witness: the log + chat

**Steps, in this order.**
1. Look at the **top** face of the wall's top-centre block. `/rpg mining place iron small`.
2. Look at the **upper** row's centre block (north face). `/rpg mining place iron small`.
3. Look at the **lower** row's centre block (north face). `/rpg mining place iron small`. **This is the slot.**
4. Same block again. `/rpg mining place iron small`.

| prediction | READING |
|---|---|
| Below, `<x>,<y>,<z>` is the **lower** row's centre block. **1:** red "A patch goes on a wall (north, south, east or west face), not up." No `MINING place` line. **2:** red "Block <x+1>,<y+2>,<z> is not solid.": the footprint runs one row UP from the looked-at block, that row is air, and its first block is the one on the player's LEFT (facing south, left is east, +X; `FaceFrame.footprint` order). No `MINING place`. **3:** green "Placed iron small at <world-uuid>:<x>,<y>,<z>:NORTH.", and one `MINING place slot=<world-uuid>:<x>,<y>,<z>:NORTH ore=iron size=small footprint=<6 blocks> by=<Ben>`, with the six blocks being the wall's. Then a `MINING show ... spots=<2 or 3 centres> live=1/<n>`. **4:** red "a slot already exists at <id>." | **Step 3 PASS; steps 1, 2 and 4 NOT READ.** L106 `MINING place slot=e404d973-…:-117,63,9:NORTH ore=iron size=small footprint=-116,63,9;-117,63,9;-118,63,9;-116,64,9;-117,64,9;-118,64,9`: six blocks, the first `-116` = x+1, the player's left (east) as predicted. L107 `MINING show … spots=20,17;10,25 live=1/2` (n = 2). The refusals' chat text is not logged and was not relayed, so steps 1, 2 and 4 are NOT READ. The log shows **five** place commands before the success (L100–L104, the first typed `small.` with a trailing full stop) and two after (L108–L109, the first again `small.`), none with a `MINING place` line: consistent with refusals, not a reading of their text |

### M-LOOK — the patch and the spot on screen (SURVIVAL, pack loaded) — witness: Ben's sight

F-M18: spike 1 never stated the model's size, facing or position. This row reads them, and how the seat's offsets
(0.0005 for the patch, 0.001 for a spot) look.

| prediction | READING |
|---|---|
| **From F5 and `MiningService`.** A flat placeholder patch (grey with tan flecks, a dark border) covering the whole 3 × 2 north face; ONE yellow spot with a dark border, 3/16 of a block square, at the first `show` centre; no flicker between the patch and the wall or the spot. **MEASUREMENT (client):** the facing and whether a 48 × 32 texture renders unstretched are client behaviour, and are read here, not predicted | **PASS by sight**, Ben Q3–Q8: as wide and as tall as the wall, flat and facing him, exactly one spot, "Gray", no flicker. Q3/Q4 read the 48 × 32 texture as covering the 3 × 2 face. (The colour prediction was "grey with tan flecks": Ben's one word is "Gray") |

**Q3.** Is the patch as wide as the 3-block wall? (yes/no) **Ben: "Yes"**
**Q4.** Is it as tall as the 2-block wall? (yes/no) **Ben: "Yes"**
**Q5.** Is it flat on the wall's face, facing you? (yes/no) **Ben: "Yes"**
**Q6.** Do you see exactly one small spot on it? (yes/no) **Ben: "Yes"**
**Q7.** One word for the patch's colour. **Ben: "Gray"**
**Q8.** Does anything flicker where the patch meets the wall or the spot? (yes/no) **Ben: "No"**

### M-SERVER-LIST — the displays are not server entities (SURVIVAL) — witness: the command output

**Steps.** Within 8 blocks of the wall:
1. `/execute if entity @e[type=minecraft:item_display,distance=..8]`
2. Positive control: `/summon minecraft:item_display ~ ~ ~`, then the same command, then `/kill @e[type=minecraft:item_display,distance=..2]`.

| prediction | READING |
|---|---|
| **1:** "Test failed": the patch and spot are packet-only. **2:** "Test passed, count: 1", then the kill removes it. If 2 fails, 1 proves nothing | **NOT READ.** The commands ran (step 1 twice, L110–L111; `/summon` "Summoned new Item Display" L112–L113; step 2's test L114; `/kill` "Killed Item Display" L115–L116), so the control's entity existed and was removed. The `/execute if` results ("Test failed" / "Test passed, count: 1") are not logged and were not relayed, so neither half has a reading |

### M-HIT — a charged hit cracks the live spot and reveals the next (SURVIVAL) — witness: the log + Ben's sight

**Steps.** Wait at least 1 second, then left-click the spot once.

| prediction | READING |
|---|---|
| One `MINING click ... outcome=HIT spot=1/<n> cooldown=1.0000 aim=<u>,<v>` with the aim IN the first `show` centre's box. The aim is read at the dig START (PLAN 5.4); spike 1's single-click aims were within 0.86 px of the centre. Then the spot becomes a crack and a second spot appears, and a click on the crack later is a MISS | **PASS** (log and sight). L124 `outcome=HIT spot=1/2 cooldown=1.0000 aim=20.59,18.00`, IN the box of the first centre (20,17): 18 ≤ 20.59 < 23, 15 ≤ 18.00 < 20; 0.51 px from the spot's centre (20.5, 17.5). Ben Q9/Q10: the spot cracked and a new one appeared. "A click on the crack later is a MISS": NOT WITNESSED (no later click landed in the crack's box). **Set-up step 7 was skipped**: the pickaxe was given at L122–L123 (05:31:53), after five DENIED clicks (see M-DENY) |

**Q9.** Did the spot change to a crack? (yes/no) **Ben: "Yes"**
**Q10.** Did a new spot appear somewhere else on the patch? (yes/no) **Ben: "Yes"**

### M-MISS — a charged click off the live spot changes nothing and plays the miss sound (SURVIVAL) — witness: the log + Ben's sight

**Steps.** Wait 1 second, then left-click the patch well away from the live spot (not on the crack's box either).

| prediction | READING |
|---|---|
| `MINING click ... outcome=MISS cooldown=1.0000` with the aim OUTSIDE the live spot's box. Nothing changes. **The miss sound plays to Ben only** (`sounds.miss`, the PLACEHOLDER `minecraft:block.stone.hit`). An uncharged click beside the spot is also a MISS and also plays it (the order of checks; `PatchTest.aimOutsideAndUnchargedIsAMiss`) | **Log half PASS; the sound NOT READ; Q12 NOT STATED.** L125 `outcome=MISS spot=2/2 cooldown=1.0000 aim=26.55,21.80`, outside the live box of (10,25). The next line still reads `spot=2/2`: nothing changed (the log half of "nothing changes"). An uncharged click beside the spot is a MISS: L127 `MISS cooldown=0.2700 aim=13.19,24.88` (u ≥ 13, outside). **The miss sound is NOT READ** (Ben, Q11: "I didn't have audio on"); CARRIED |

**Q11.** Did you hear a sound when you missed? (yes/no) **Ben: "I didn't have audio on"**: NOT STATED; the sound half is NOT READ and CARRIED
**Q12.** Did anything on the patch change? (yes/no) **Ben: "I'm not sure what this question is asking"**: NOT STATED

### M-UNCHARGED — an early click on the live spot changes nothing and plays nothing (SURVIVAL) — witness: the log + Ben's sight

**Steps (amended before any boot, G1: this row spends NO spot).** Left-click the patch **beside** the live spot
(outside its box, and off any crack). Then, **within half a second**, left-click **ON** the live spot.

| prediction | READING |
|---|---|
| The first: `MINING click ... outcome=MISS` (its swing resets the ticker, F1). The second: `MINING click ... outcome=UNCHARGED cooldown=<(Δ+0.5)/16.666666>`, Δ being its tick minus the first's (under 10 ticks, so under 0.6300), with the aim IN the live spot's box and `spot=` the SAME index as the MISS line's. **An UNCHARGED click plays nothing**, and nothing changes: the live spot stays live. (If the second click lands outside the box it is a MISS with the miss sound, recorded as such, and the row is repeated) | **Log half PASS; the sound NOT READ.** Two MISS-then-UNCHARGED pairs. (a) L130 MISS `cooldown=1.0000 aim=14.97,25.20` (outside) at tick 9887, then L131 `UNCHARGED spot=2/2 cooldown=0.5100 aim=10.10,25.52` (IN the box of (10,25)) at 9896: Δ = 9, under half a second; the same `spot=2/2`; nothing changed. The formula gives 0.5700 at Δ = 9, and **0.5100 is Δ = 8, one tick off** (the instrument phase; see M-CHARGE-17). Under 0.6300 as predicted. (b) An earlier pair: L127 MISS (0.2700, tick 9610), then L128 UNCHARGED 0.2700 at 9615 (Δ 5, the formula gives 0.3300, one tick off) and L129 UNCHARGED 0.7500 at 9627 (Δ 12, exact), both IN the box. M-FULL adds five more in-game UNCHARGED lines, all IN the box and none spending a spot. **No sound reading** (Q13, audio off); CARRIED |

**Q13.** On the fast second click on a spot, did you hear a sound? (yes/no) **Ben: "Didn't have audio on"**: NOT STATED; the sound half is NOT READ and CARRIED

### M-CHARGE-17 — the full-charge boundary spike 1 never isolated (SURVIVAL) — witness: the log

**Steps (amended before any boot, G1: this row spends NO spot).** Every click line carries the cooldown whatever
its outcome, so the boundary is read on clicks **beside** the live spot. Make **20 clicks on the patch away from the
live spot** (and off any crack), each one **the instant the crosshair meter looks full**.

| prediction | READING |
|---|---|
| 20 `MINING click ... outcome=MISS` lines. Δ is each line's tick minus the previous click line's. **At Δ = 16 the line reads `cooldown=0.9900`; at Δ = 17 it reads `cooldown=1.0000`.** **If neither Δ occurs, the boundary is NOT WITNESSED**, recorded as such and not as a pass. About 10% of spike 1's reads sat one tick off the logged tick difference (the instrument phase); that is recorded, not a failure. **What this row does not read:** the mapping from a cooldown to HIT or UNCHARGED (`ChargeRule`: exactly 1.0f) is unit-tested (`SpotBoxAndChargeTest`), and is read in game by M-HIT (`1.0000` → HIT) and M-UNCHARGED (below 1 → UNCHARGED) | **PASS: the boundary is WITNESSED.** 19 MISS lines (L132–L150, ticks 10099–10416; **19, not 20**), all at `aim=15.8x,25.5x`, outside the live box of (10,25). Deltas between consecutive lines: **Δ = 16 twice** (10329→10345, 10345→10361), both `cooldown=0.9900`, as predicted. **Δ = 17 five times**: four `1.0000` (10117→10134, 10134→10151, 10151→10168, 10293→10310) and one `0.9900` (10222→10239), one tick off, the instrument phase the prediction allows for. Every other Δ (18, 19) read 1.0000 |

### M-HOLD — holding left-click on the live spot hits at most once (SURVIVAL) — witness: the log

**Steps.** After a 1-second wait, hold left-click on the live spot for 3 seconds.

| prediction | READING |
|---|---|
| **Exactly one `MINING click` line for the whole hold**: the dig START's `BlockDamageEvent` (MS3 read one DIG_START for a 58-swing hold), which is the first click and may be a HIT, **or BROKE if it is the last spot (n = 2): then THE STANDING RULE applies** (amended before any boot, G1). No line for any held swing. The deepslate does not break (no block change; the crack and flicker Ben saw in spike 1 are a client visual he accepted, F-M14) | **NOT AS PREDICTED: two click lines in the hold, not one.** L151 `outcome=BROKE spot=2/2 cooldown=1.0000 aim=10.01,25.71` (IN the box of (10,25)) at tick 10629: **the hold's START took the last spot (n = 2), so THE STANDING RULE applies and this is M-BREAK's first reading.** Then L153 `outcome=NOT_LIVE cooldown=0.8100` at 10656, 27 ticks later, inside the 3-second hold. Cause UNREAD: a second press, or the client restarting the dig. No line for any held swing beyond those. Ben did not run `/rpg mining regrow` after the early break (L155–L157 are three `place` commands, refused); the patch regrew by itself (M-REGROW) |

### M-DENY — no pickaxe, no mining; any pickaxe will do (SURVIVAL) — witness: the log + Ben's sight

**Steps.** 1. Empty hand: wait 1 s, click the live spot. 2. `/give @s minecraft:iron_sword`, hold it, wait 1 s, click the live spot. 3. Back to the vanilla iron pickaxe: wait 1 s, click the live spot.

| prediction | READING |
|---|---|
| **1 and 2:** `MINING click ... outcome=DENIED`, and the action bar shows "You need a pickaxe to mine this." in red. Nothing changes, no sound. **3:** `HIT` (or `BROKE`, and then THE STANDING RULE applies; amended before any boot, G1): any item in `#minecraft:pickaxes` counts, minted or not (the seat's ruling 7) | **Log half PASS; the action bar NOT AS PREDICTED (a DEFECT).** On the regrown patch: L162–L165 four `DENIED spot=1/2` (05:34:33–35), after the sword was given (L158–L159). The log cannot separate step 1 (empty hand) from step 2 (sword); the last two read 0.8400 at Δ 10 and 0.7600 at Δ 9, which is the sword's 1.6 speed exactly ((Δ+0.5)/12.5, computed). Nothing changed (`spot=1/2` throughout). Step 3: L166 `HIT spot=1/2 cooldown=1.0000 aim=24.43,8.95`, IN the box of (24,8). **Ben, Q14: "Yes but the HP/defense/Mana overode it faster than it could be read"**: the DENIED line is overwritten by the HUD (`StatsBarSystem`, every 10 ticks); recorded as a finding, not fixed (Ben's ruling). Also: five DENIED lines at L117–L121 (05:31:42–44), before set-up step 7's pickaxe was given; two read `0.7000` at Δ 16 and Δ 4 with the held item unknown: UNEXPLAINED, no prediction attached |

**Q14.** With an empty hand, did a red message appear above the hotbar? (yes/no) **Ben: "Yes but the HP/defense/Mana overode it faster than it could be read"**: a DEFECT (the HUD overwrites the line)

### M-BREAK — the last spot breaks the patch and pays raw iron (SURVIVAL) — witness: the log + Ben's sight + inventory

**Steps.** With room in the inventory, keep making charged hits on the live spot until the patch breaks. Note the raw
iron count after; "before" is set-up step 5's count. **If an earlier row already broke a patch, THE STANDING RULE
says which break this row's reading is** (amended before any boot, G1).

| prediction | READING |
|---|---|
| The last hit logs `MINING click ... outcome=BROKE`, then `MINING break ... payout=raw_ironx<count> rolls=<one per spot> bonus=5 kept=<count> dropped=0 regrow_at=<tick + 1200>`. **Each roll is 2, 3 or 4; the count is their sum + 5, inside 9..17** (9..13 for 2 spots, 11..17 for 3; Ben: "2-4 per spot with a 5 completion bonus"). The inventory gains exactly `count` raw iron. Every mark goes; the break sound (`minecraft:block.stone.break`, PLACEHOLDER) and a poof (PLACEHOLDER) play to Ben only. A click straight after logs `NOT_LIVE` and plays nothing | **PASS (both breaks).** **First break (THE STANDING RULE: it landed in M-HOLD):** L152 `payout=raw_ironx9 rolls=2+2 bonus=5 kept=9 dropped=0 regrow_at=11829` (= 10629 + 1200). 2 + 2 + 5 = 9, inside 9..13 for 2 spots. **Ben, QA "0 Raw Iron", Q15 "9"**: the inventory gained exactly the count. L153, the click straight after, is `NOT_LIVE`. **Second break, as written:** L167 `BROKE spot=2/2 aim=7.70,21.18` (IN the box of (7,21)), L168 `payout=raw_ironx12 rolls=4+3 bonus=5 kept=12 dropped=0 regrow_at=14103` (= 12903 + 1200), inside 9..13. Cross-check: L169–L170 `/clear @s minecraft:raw_iron` "Removed 21 item(s)" = 9 + 12 with QA 0. Ben Q16 the patch disappeared, Q18 a puff of particles. **The break sound is NOT READ** (Q17, audio off); CARRIED |

**Q15.** How many raw iron did you gain? (one number) **Ben: "9"** (QA: "0 Raw Iron")
**Q16.** Did the patch disappear? (yes/no) **Ben: "Yes"**
**Q17.** Did you hear a break sound? (yes/no) **Ben: "Didn't have audio on"**: NOT STATED; the sound half is NOT READ and CARRIED
**Q18.** Did you see a puff of particles? (yes/no) **Ben: "Yes"**

### M-REGROW — the patch returns after 1200 ticks, re-rolled (SURVIVAL) — witness: the log + Ben's sight

**Steps.** Stay near the wall and wait about 60 seconds after M-BREAK.

| prediction | READING |
|---|---|
| At tick ≥ the break line's `regrow_at`, `MINING show ... live=1/<n>` and `MINING regrow ... spots=<n>`. **The `spots=` centres differ from the previous `show` line's** (re-rolled; a repeat is possible but unlikely, and is recorded). Ben sees a fresh patch with one spot. (Ben: "1 minute is fine for now"; it counts server ticks, so under 20 TPS it is longer.) | **PASS.** L160–L161 at 05:34:00: `MINING show … spots=24,8;7,21 live=1/2 tick=11829` and `MINING regrow … spots=2 tick=11829`: **exactly at the first break's `regrow_at` (11829)**, by itself (no `/rpg mining regrow` ran). The centres differ from the previous show's `20,17;10,25`: re-rolled. Ben Q19: "yes" |

**Q19.** Did the patch come back by itself? (yes/no) **Ben: "yes"**

### M-FULL — a full inventory drops the remainder at your feet (SURVIVAL) — witness: the log + Ben's sight

**Steps.** 1. `/clear @s minecraft:raw_iron`, so no part-stack of raw iron can absorb the payout. 2. Fill every free
inventory slot: `/give @s minecraft:cobblestone 2304` (36 stacks; the extra lands on the ground, and a full inventory
picks nothing up). 3. `/rpg mining regrow` if the patch is not up. 4. Break the patch.

| prediction | READING |
|---|---|
| `MINING regrow-all player=<Ben> shown=<0 or 1>`. The break line reads **`kept=0 dropped=<count>`** with `kept + dropped = count` (the seat's ruling: the remainder drops at the player's feet, as vanilla does). The raw iron lies at Ben's feet | **PASS** (log; the ground clean-up step 8 NOT READ). L171–L172 "Gave 2304 [Cobblestone]"; L175 `MINING regrow-all … shown=1` (show L174 `spots=15,18;4,7 live=1/2`). L176 HIT, then five UNCHARGED (L177–L181, all IN the box of (4,7), no spot spent), then L182 `BROKE spot=2/2 aim=5.08,7.25` and L183 `payout=raw_ironx9 rolls=2+2 bonus=5 kept=0 dropped=9 regrow_at=15250`: **kept 0, dropped 9**, as predicted. Ben Q20: "Yes". Clean-up: L184–L185 `/kill @e[type=minecraft:item,distance=..16]` "Killed 5 entities" (n ≥ 1, the positive control); L186–L187 `/clear @s minecraft:cobblestone` "Removed 2048 item(s)"; L188 the final `/execute if entity` ran, and its result is not logged and was not relayed: NOT READ. (5 entities fits four spilled cobblestone stacks plus one raw iron stack; an inference, not read) |

**Q20.** Did raw iron drop on the ground at your feet? (yes/no) **Ben: "Yes"**

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
| After the reconnect, one `MINING show player=<Ben> slot=<id> spots=<...> live=2/<n>` line whose **`spots=` centres are the SAME as the last `show` line before the disconnect** and whose live index is the one after the HIT (`live=2/<n>`). No `MINING click` line between the reconnect and that show. The trace stays ON across the reconnect (it is keyed by UUID and a quit does not clear it). By sight: the patch, ONE crack and ONE live spot, in the places they were | **Log half PASS; the sight half NOT STATED.** L189–L191 `/rpg mining regrow`: show `spots=17,24;6,17;41,25 live=1/3`. L192 `HIT spot=1/3 cooldown=1.0000 aim=17.64,24.29` (IN the box of (17,24)) at 05:36:33; disconnect L193 the same second; reconnect L197 05:36:39. **L198 `MINING show … spots=17,24;6,17;41,25 live=2/3 tick=15008`: the SAME centres and the live index after the HIT**, with no `MINING click` line between the reconnect and it. The trace stayed ON. Q23–Q25: Ben gave no answer: NOT STATED. **Not in the steps:** Ben re-ran `/rpg mining pack` at L199–L203 after the reconnect (the join toggle is OFF, so nothing re-offers the pack). Whether the client kept the pack across the reconnect is UNREAD |

**Q23.** After reconnecting, was the patch back on the wall? (yes/no) **Ben gave no answer**: NOT STATED
**Q24.** Was the crack still there? (yes/no) **Ben gave no answer**: NOT STATED
**Q25.** Was there exactly one live spot, where it was before? (yes/no) **Ben gave no answer**: NOT STATED

**Addendum 2026-11-14 (the pack).** The reconnect at L197 (05:36:39) left the pack unloaded; L198's show (05:36:39)
went to a client **without** the project models. The pack was re-offered at L199–L200 (05:36:57) and reached
SUCCESSFULLY_LOADED at L203 (05:36:59). **Any sight between 05:36:39 and 05:36:59 was without the models**
(*"purple and black squares"*). Q23–Q25 stay NOT STATED (*"not sure and skip"*). The log half stands.

### M-LOGOUT — regrowth is kept across logout (SURVIVAL) — witness: the log + Ben's sight

**Steps.** 1. Break the patch (`/rpg mining regrow` first if needed). 2. Disconnect within 20 seconds and reconnect
within 40 seconds of the break. 3. Look at the wall. 4. Wait until 60 seconds after the break. **Do not click the wall
between reconnecting and `regrow_at`, or the row is VOID** (amended before any boot, G3): a click calls `show` itself
and would read the click path, not the patch returning by itself.

| prediction | READING |
|---|---|
| On reconnecting before `regrow_at`: **no patch** and no `MINING show` line (the clock is kept across logout; the seat's ruling 4). After `regrow_at`: the patch appears **by itself** and a `MINING show` line prints. The trace stays ON across the reconnect. (If the reconnect lands after `regrow_at`, the first half is NOT WITNESSED.) **Why it is true now (amended before any boot, G3):** the quit drops the break's regrowth task (F8c); on reconnect the chunk load reaches `show`, which finds the patch regrowing and arms ONE new task on the new session's Player (D1). Before D1 the wall stayed bare until a click | **PASS (log and sight). D1 WITNESSED.** L204 `HIT spot=2/3` (IN (6,17)), L205 `BROKE spot=3/3 aim=41.27,25.37` (IN (41,25)), L206 `payout=raw_ironx14 rolls=3+3+3 bonus=5 kept=14 dropped=0 regrow_at=16864` (3 + 3 + 3 + 5 = 14, inside 11..17). Disconnect L207 05:37:24 (12 s after the break); reconnect L211 05:37:54 (**42 s after the break; the step said within 40 s**; still before `regrow_at`). On reconnect: no `MINING show` line (Ben Q21: the wall was bare). **No click line between the reconnect and `regrow_at`: the row is NOT VOID.** L216–L217 at 05:38:11: `MINING show … spots=33,20;14,8;24,27 live=1/3 tick=16864` and **`MINING regrow … tick=16864`, exactly `regrow_at`**. A `regrow` line is printed only by the regrowth task, so this is the task the reconnect's chunk load re-armed (D1). (Ben applied M-RESPAWN's two effects at L212 and L214 before `regrow_at`: no click, no effect on this row) |

**Q21.** When you came back, was the wall bare at first? (yes/no) **Ben: "yes"**

**Addendum 2026-11-14 (the pack).** The reconnect at L211 (05:37:54) left the pack unloaded, and it was not
re-offered until L220–L224 (05:38:18–05:38:19, in the nether). So **the patch's return at L216–L217 (05:38:11) was
shown to a client WITHOUT the project models.** Q21 (*"the wall was bare at first"*) is unaffected: before
`regrow_at` there was no display to draw, with or without the pack. The log half, including the `regrow` line at
`regrow_at`, stands.

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
| **Step 3:** no `MINING` line (the unloads clear `shown` and log nothing). Ben takes no damage. **Step 4:** within a few seconds of arriving, one `MINING show player=<Ben> slot=<id>` line whose `spots=` and `live=` equal step 1's line, and no `MINING click` line before it. By sight: the patch and its spot are back. **If no `show` line prints and the wall is bare, D2 is real** (a stale `shown`): FAIL, recorded, and a relog is the recovery | **Dimension half PASS (log and sight); the same-world respawn half NOT READ; and an UNPLANNED DEATH.** Step 1's line: L216 `spots=33,20;14,8;24,27 live=1/3`. Step 3: L218–L219 teleported to the nether roof; no `MINING` line while there. **First return FAILED**: L225 the placeholder `tp @s <x> <y> <z−3> 0 0` was typed literally and did nothing. **L226–L228 `/kill`, "BaronVonYeetus was killed" (05:39:27): a DEATH, which the staging said must not happen.** The respawn put him in the overworld, and L229 `MINING show … spots=33,20;14,8;24,27 live=1/3 tick=18410` re-showed the patch: an unplanned reading of a respawn WITH a dimension change (nether → overworld), **not** the same-world respawn this row leaves NOT READ. L230–L231 `/rpg mining regrow` `shown=0`: already shown, so `shown` was not stale. **The row as written, second attempt:** L232–L233 to the nether, L234–L235 back to `-116.5 63 6.5`, **L236 `MINING show … spots=33,20;14,8;24,27 live=1/3 tick=20208`, equal to step 1's line**, with no click line before it. Ben Q26 "no" damage, Q27 "yes", the patch was there. **The death's cost, from Ben's player data against the backup** (read-only): the Ability Stone (`rpg:build_stone`) and the Nexus star (`rpg:nexus`) are still in his inventory. **Five iron pickaxes in the backup (four named, with attribute modifiers, and one plain) are not**; whether the death dropped them or Ben moved them is UNREAD |

**Q26.** On the nether roof, did you take any damage? (yes/no) **Ben: "no"**
**Q27.** Back at the wall, without clicking, was the patch there? (yes/no) **Ben: "yes"**

**Addendum 2026-11-14 (the pack, and the pickaxes).** The pack reached SUCCESSFULLY_LOADED at L224 (05:38:19), after
step 1's show line (L216, 05:38:11, shown without the models; see M-LOGOUT). **No reconnect followed** (there is
no `logged in` line after L211; the death and respawn at L226–L229 are not a reconnect). So L229's show (05:39:29) and the
row's reading show at L236 (05:40:59), and with them **Q27, were with the pack loaded**, on the reading that a
respawn does not drop a server pack. That premise was not checked; Ben's report names only reconnects. **The
pickaxes:** Ben: *"questions 1-4 not sure and skip"*. The loss is recorded as read from his player data; nothing is
restored or minted.

### M-PROTECT — the slot's blocks cannot be broken in survival (SURVIVAL) — witness: the log + Ben's sight

**Steps.** With the vanilla iron pickaxe, hold left-click on a footprint block **away from the patch's live spot** for
10 seconds (deepslate breaks in about 2 seconds with an iron pickaxe when nothing protects it).

| prediction | READING |
|---|---|
| The block is still deepslate afterwards. No block change in the log. The client may show a crack or a brief vanish and return (F-M14, accepted by Ben as a client visual and deferred). **The creative half of the plan's M-PROTECT is not in this gate** (GAME MODE) | **PASS by sight; the log half is vacuous.** L237–L239 three `MISS spot=1/3` lines (cooldowns 0.1500, 0.0900, 0.2700; aims 32.3,25.1 and 20.3,25.2, outside the live box of (33,20)), the first and last 235 ticks apart. Ben Q22: "yes", the block was still there. "No block change in the log": **this log never records block changes**, so that half has no instrument and is not a reading. Three click lines in one hold: no count was predicted for this row (compare M-HOLD) |

**Q22.** After 10 seconds, was the block still there? (yes/no) **Ben: "yes"**

### M-SECOND-PLAYER — each player sees their own patch (SURVIVAL) — witness: a second client

| prediction | READING |
|---|---|
| **Only if a second client is available.** The second player sees their own fresh patch at the slot, with its own spots, not Ben's progress; Ben's sounds are not heard by them. **Otherwise this row is NOT READ**, and that is recorded, never treated as a pass | **NOT READ.** Ben, QB: "No" second client |

### THE REMOVE STEP (inside the LAST STEP's run-up; not a row)

`/rpg mining remove`, looking at the wall: green "Removed <id>.", `MINING remove slot=<id> by=<Ben>`, the marks
vanish, and the deepslate then digs normally. Recorded as a reading under the LAST STEP.

---

## LAST STEP — a clean /stop

| prediction | READING |
|---|---|
| `/stop` from the console. The log shows `Stopping`, `Disabling Rpg`, and `All dimensions are saved`, with no exception between them. No `java.exe` remains afterwards (`Get-Process java -ErrorAction SilentlyContinue` prints nothing). The 127.0.0.1 pack server stops with the plugin (`MiningService.shutdown`) | **PASS.** L241 `MINING remove slot=e404d973-…:-117,63,9:NORTH by=BaronVonYeetus` (the remove step; its chat line and "the deepslate then digs normally" are NOT READ). `/stop` was issued **from chat** by Ben (L242), not the console. L244 `Stopping server`, L245 `Disabling Rpg v0.1.0-SNAPSHOT`, L275 `All dimensions are saved`, no exception or SEVERE between them. `Get-Process java` afterwards: 0. The pack server has no stop line; the JVM is gone |
