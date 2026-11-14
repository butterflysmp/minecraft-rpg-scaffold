# GATE — MINING-AUTO: `/rpg selftest` drives mining's server-side rows; the settle fix (ST9) re-read; `/rpg mining` under the dev guard

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/mining-selftest`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read. **NO BOOT until the seat has diffed the code AND this gate, and Ben says ready.**

**Plan:** `PLAN-mining.md` (§2's 2026-11-14 guard ruling; §6's *CARRIED ROWS, 2026-11-14 (selftest)*, which names the
carried rows this gate turns AUTO) and `PLAN-selftest.md` (the framework). **The rule every row answers to:**
`.claude/rules/verification.md`, *AN AUTO READING IS A READING OF THE SERVER, NEVER OF THE CLIENT*.

```
ROWS     21   R0a R0b R0c R0d
              ST9R G1 G2
              A1 A2 A3 A4 A5 A6 A7 A8 A9 A10 A11 A12
              LAST NOT-AUTO
         ──
         21   = 4 R0 + 3 + 12 A + 2      git grep -c -E '^### ' <ref> -- docs/gates/GATE-mining-auto.md
```

The twelve `A` rows are the scenarios in `paper/src/main/resources/selftest/GATE-mining-auto.yml`, added LAST in
`selftest/index.yml`; `SelfTestScenariosTest` pins that each scenario key is a `### <id> ` heading here.

## THE PRINCIPLES (the framework's, restated because they bind every row)

- **P1, the REAL path.** A mining click is the `dig` verb: a `BlockDamageEvent` for the block the eye ray meets
  within the player's block-interaction range, carrying its face and the MAIN-HAND item, fired into every listener.
  `RpgListeners.onMiningBlockDamage` hands it to `MiningService.onBlockDamage`, which reads the charge, the eye and
  the item from the player as it does for Ben's click. Then `Player#resetCooldown`, which is what the click's arm
  swing does on the server (`ServerPlayer.swing` → `resetAttackStrengthTicker`, PLAN-mining.md F1). Every other step
  is a real command (`CMD`, through the one Brigadier dispatcher).
- **P2, NEVER PASS OR FAIL.** The witnesses are the `MINING` and `HUD` lines the shipped code already logs, and the
  instrument's `DRIVE`, `CMD`, `REPLY`, `SETTLED` and `RESPAWNED` lines. A person judges each row against this file.
- **P3, THE SERVER, NEVER THE CLIENT.** No AUTO reading here covers what Ben sees or hears: no patch art, no crack,
  no sound heard, no pack, no client dig (crack animation, ghost break, the client's own dig sound). **Every row
  below is AUTO for its log half only**, and says what it leaves to Ben.
- **P4, NO TRACE.** Blocks are SAVED before every fill and put back by the framework's row-start clean and its
  restore; the inventory (the pickaxe and stick put in the main hand, every payout) by the restore; the game rules,
  time and position likewise. Every row removes the slot it placed. **What the framework does NOT restore, stated:**
  (1) the MINING trace stays ON after the run (an in-memory dev log toggle, lost at restart; the `mining_trace` verb
  turns it on and never off, so a second run cannot toggle it off by accident); (2) a slot left by a row that was
  SKIPPED or STOPPED mid-way stays registered until the next mining row's opening `remove`, or the restart; (3)
  payout DROPPED at the feet (a full inventory) is not removed by the restore, which takes orbs and tagged mobs only.

**What the driver skips, read from the code and the pinned jar** (each a thing Ben's click does and the instrument
does not, so no AUTO reading here covers it): the dig START packet and its decode; the server's reach check on it
(`canInteractWithBlock`); the `PlayerInteractEvent LEFT_CLICK_BLOCK` before it; the swing packet, its animation and
`PlayerArmSwingEvent`; the client's crack and dig sound.

## GAME MODE

**SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL (GATE-selftest.md ST7). CREATIVE breaks a block
before `BlockDamageEvent` (PLAN-mining.md F2b), so no mining row means anything in it. A4 stages one death (the
player's own, `kill @s`, under `death_rules`), and no row leaves the overworld.

## THE JAR FACTS EVERY CHARGE PREDICTION STANDS ON (javap, Paper 26.1.2 build 74, read 2026-11-14)

Server jar `run/versions/26.1.2/paper-26.1.2.jar` (MANIFEST `Implementation-Version: 26.1.2-74-e4e17fc`, `Build-Number:
74`), read with `C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe -c -p -cp <jar> <class>`; API
`paper-api-26.1.2.build.74-stable.jar` with `javap -cp`.

| fact | class#member |
|---|---|
| the read: `getAttackCooldown()` = `getAttackStrengthScale(0.5f)` = `Mth.clamp((attackStrengthTicker + 0.5f) / getCurrentItemAttackStrengthDelay(), 0, 1)` | `CraftHumanEntity#getAttackCooldown`, `Player#getAttackStrengthScale(F)` |
| the delay: `(float)(1.0 / ATTACK_SPEED * 20.0)` | `Player#getCurrentItemAttackStrengthDelay` |
| an iron pickaxe's speed modifier is `-2.8f` on the base `4.0`: ATTACK_SPEED 1.2 | `Items` static init: `ldc "iron_pickaxe"`, `ToolMaterial.IRON`, `ldc -2.8f`, `Item$Properties.pickaxe(ToolMaterial;FF)` |
| the API reset: `resetCooldown()` → `ServerPlayer.resetAttackStrengthTicker()`, which sets the ticker to 0 | `CraftPlayer#resetCooldown`, `Player#resetAttackStrengthTicker` |
| the ticker rises by 1 per tick in `Player#tick` (bytecode 264-271), and THEN resets if the main-hand item TYPE changed (`ItemStack.isSameItem` false, 314) | `Player#tick` |
| `Player#tick` runs once per tick for a valid non-spectator player: `ServerPlayer#doTick` (22) ← `ServerGamePacketListenerImpl#tickPlayer` (50) ← `tick` | as named |
| the entity schedulers run BEFORE the world and the connections tick: `EntityScheduler.executeTick` at `tickChildren` 81, `ServerLevel.tick` at 431, `tickConnection` at 507 | `MinecraftServer#tickChildren` |
| `BlockDamageEvent(Player, Block, BlockFace, ItemStack, boolean)` exists, and carries `getItemInHand()` and `getBlockFace()` | the API's `org.bukkit.event.block.BlockDamageEvent` |
| `MiningService` tests THE EVENT'S item for the pickaxe tag: `Tag.ITEMS_PICKAXES.isTagged(event.getItemInHand().getType())` | `MiningService.onBlockDamage` (this repo) |
| the existing `swing` verb does NOT reset the ticker: it builds a `PlayerArmSwingEvent` and calls it, and nothing else | `SelfTestDrivers.swing` (this repo) |

**So a `dig` at scenario tick T resets the ticker to 0 (the scheduler phase), `Player#tick` raises it to 1 in the same
tick, and a `dig` n ticks later reads ticker n.** At ATTACK_SPEED 1.2 the delay is `16.666666f`, and, executed in
jshell (`float delay = (float)(1.0 / (4.0 + (double) -2.8f) * 20.0)`, then `Math.max(0f, Math.min(1f, (n + 0.5f) /
delay))`; the same with `1.0 / 1.2` gives the same floats):

| n ticks after the reset | 1 | 5 | 7 | 15 | **16** | **17** | 18 |
|---|---|---|---|---|---|---|---|
| `getAttackCooldown()` | 0.09 | 0.33 | 0.45000002 | 0.93 | **0.99** | **1.0** | 1.0 |
| the click line's `cooldown=%.4f` | 0.0900 | 0.3300 | 0.4500 | 0.9300 | **0.9900** | **1.0000** | 1.0000 |
| charged (`== 1.0f`, `ChargeRule`) | no | no | no | no | **no** | **yes** | yes |

**A charged click in these scenarios is `aim_patch: live`, `wait: 17`, `dig`**, 17 ticks after the previous dig's
reset: `cooldown=1.0000`. Every row's first charged dig comes at least 19 ticks after any reset the row made.

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root (`mkdir` without `-p`
   refuses an existing folder):

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-mining-selftest"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   Report the folder, both pairs of counts, and `diff -rq lines: 0` for each.
3. **Check, do not assume, the join toggle in Ben's local file:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml`
   must print `12:  dev-pack-on-join: true`. If it does not, STOP and report; do not edit it without the seat. (A4's
   pack question needs the pack offered at LOGIN.)
4. **Boot:** `./scripts/dev-server.sh --refresh-content`.
5. **R0 binds to THIS boot** (PLAN-mining.md F-M44): read R0 only on a `latest.log` with a fresh mtime whose
   `[Rpg] Build:` line names this PR's head.

**Also carried, not actions:**
- **The world's saved difficulty wins over `server.properties`**, and it was PEACEFUL on a 2026-11-14 boot (runs 1
  and 2 of `e82df2b3` certified nothing for it). The run sheet READS it (`/difficulty`, no argument) and SETS it with
  a literal command before the run. Mining rows summon nothing, but `/rpg selftest all` runs every gate, and five of
  them summon mobs.
- **The pack offered at LOGIN** (`mining.dev-pack-on-join: true`): predicted unchanged from S2,
  `sha1=49f2e465b2b6bbb24380425830c8572f14acd11c bytes=8227`, because nothing it is built from moved:
  `git diff --stat 01727a8c HEAD -- paper/src/main/resources/resourcepack paper/src/main/java/io/github/butterflysmp/rpg/paper/mining/MiningPack.java`
  prints nothing. A different sha1 is recorded, not an R0 failure.
- **No mining slot exists at boot** (in memory, lost on restart). Every slot in this gate is placed by its row.
- **A death keeps the inventory** (`RpgListeners.onPlayerDeath`, PLAN-mining.md F-M30), and A4's `death_rules` also
  sets `keep_inventory` and `immediate_respawn` for that row only.
- **Inventory room:** the gate pays at most 46 (A3) + 17 + 26 + 36 (A6) + 17 (A12) = **142 raw iron**, three
  stacks. Ben's own inventory is what the run starts this gate with (the gate boundary restores it), so the run
  sheet asks him to keep **four slots free**. A `dropped=` above 0 is recorded, and those items stay on the ground.

## R0 — THIS PR'S BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

`GATE-deft-hands.md`'s R0 rows (the last stack top) are SUPERSEDED at this boot, never PASS or FAIL.

### R0a — the build line names this PR's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/mining-selftest` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | _(not run)_ |

### R0b — the jar carries this branch

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `selftest/GATE-mining-auto.yml`, paper `selftest/SelfTestMiningAim.class`, paper `mining/MiningService$PatchView.class`; in `selftest/index.yml`, the line `  - mining-auto`. ABSENT: the control `selftest/NoSuchClassControl.class` | the scan below | _(not run)_ |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'selftest/GATE-mining-auto.yml',
               'io/github/butterflysmp/rpg/paper/selftest/SelfTestMiningAim.class',
               'io/github/butterflysmp/rpg/paper/mining/MiningService$PatchView.class',
               'io/github/butterflysmp/rpg/paper/selftest/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$r = New-Object IO.StreamReader($zip.GetEntry('selftest/index.yml').Open()); $idx = $r.ReadToEnd(); $r.Close()
if ($idx -match '(?m)^  - mining-auto\s*$') { "PRESENT index mining-auto" } else { "ABSENT  index mining-auto" }
$zip.Dispose()
```

### R0c — the Loaded line, unchanged, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes, 1 ores, 4 vein sizes`**, exactly the line `GATE-deft-hands.md` R0c READ at `e82df2b3`, because this branch changes nothing the loaders read: `git diff --stat 01727a8c HEAD -- paper/src/main/resources/content` prints nothing. No `Refusing`, `Skipping`, `SEVERE` or `Exception` beyond `volley_stone`'s known pair; no `No ores`, no `No vein`, no `Content:` line naming `ore 'iron'` | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception','No ores','No vein','Content:'` | _(not run)_ |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 50 scenarios across 10 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto]`**, and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | _(not run)_ |

**The 50 and the 10 are DERIVED, not copied.** From the repo root at this head, a scenario is a top-level YAML key
and each gate has one `setup:`:

```bash
cd paper/src/main/resources/selftest
grep -cE '^  - ' index.yml                                   # gates: 10
cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'   # keys: 60
cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^setup:'           # setups: 10
```

Printed when this was written: **10, 60, 10**, so 60 − 10 = **50** rows. The same count at `01727a8c` gives 9 gates
and 38 rows, which is the line `GATE-deft-hands.md` R0d READ at `e82df2b3` (the control).
`SelfTestScenariosTest.theScenariosAreExactlyTheCoreRows` pins the 50.

---

## THE INSTRUMENT'S ROWS

**The log instrument for every row below:** `Select-String run\logs\latest.log -Pattern 'SELFTEST|MINING|HUD hold-end'`,
read after Ben's LOGIN, inside the row's own `SELFTEST mining-auto <row> START … DONE` bracket unless a row says
otherwise.

### ST9R — after a kill, the settle now waits for the body (SURVIVAL) — witness: the log

The live re-read of `GATE-selftest.md` ST9 after this branch's fix (`SelfTestStaging.dying(living, dead)`; the
SETTLED line gains `first=`). Read over the whole `/rpg selftest all` run.

| prediction | READING |
|---|---|
| **32** `CMD /kill …` lines (31 at `01727a8c` + A4's `kill @s`: `git grep -h -E '^\s*- cmd: "kill ' HEAD -- 'paper/src/main/resources/selftest/*.yml' \| wc -l` printed 32), and after each, before the row's next step line, exactly one **`SETTLED dying=0 ticks=<t> first=<k>`**. **After a kill whose REPLY is `Killed <a mob>`:** `first` ≥ 1 and **`ticks=20`** — the body is dead from the kill, `tickDeath` removes it on the tick its `deathTime` reaches 20, the kill runs in the scheduler phase before that tick's world tick, so the poll 20 ticks later is the first to find it gone. **After `No entity was found`, or a kill of the PLAYER (SU8's and A4's `kill @s`; `getNearbyEntities` excludes the player):** `ticks=0 first=0`. **How many of each, derived from run 3 of `e82df2b3`** (the same 31 kill steps): `sed -n 2206,3657p run/logs/latest.log \| grep -cE 'REPLY "Killed '` printed 11, of which one is `Killed BaronVonYeetus`, so **10 waits at `ticks=20`** and **22 at `ticks=0`** (20 `No entity was found`, SU8, A4). The 10 depends on what each kill finds that day, so the per-line rule is the prediction and the 10 is its expected count. **`dying=<k>` with k > 0 means the 40-tick cap was hit: a finding. Any `ticks=` other than 20 after a mob kill is NOT AS PREDICTED and is recorded.** **The live consequence:** no `PROBE tag=t … dead=true` line follows a SETTLED in the same row (run 3 had two, in WS2c, L3037 and L3053) | _(not run)_ |

### G1 — on this dev boot, `/rpg mining` is present for Ben (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| Ben is an op on a server `dev-server.sh` booted (R0d), so `MiningDevCommand.permitted(true, true)`. Every `CMD /rpg mining …` line in the mining-auto rows is answered by the mining code: the first `CMD /rpg mining place iron medium` (A1) is followed by `MINING place slot=…`, and **no `CMD-UNHANDLED /rpg mining` line appears anywhere in the run** | _(not run)_ |

### G2 — off a dev server, `/rpg mining` is absent (UNIT ONLY) — witness: the unit rows

**Not readable on a dev boot, by construction:** the marker is a JVM constant, and only `dev-server.sh` boots this
server. `MiningWiringSignatureTest.theMiningRuleIsTheDevPermissionAndADevServerAndNothingElse` (all four inputs),
`theMiningBranchIsGatedByTheDevRuleOnTheLineAfterItsLiteral` (the `requires` line, anchored at both ends) and
`theDevMarkerIsDefinedOnceReadOnceAndHandedToBothGuards` (one `"rpg.dev"` literal, one property read, both hand-offs)
stand for it. **Recorded UNIT ONLY, never a live pass.** What a non-dev server answers an op who types `/rpg mining`
is Brigadier's own reply for a node the sender cannot use; this gate does not predict its text.

---

## READING THE MINING ROWS

- **X0, Y0, Z0** are the run's start block: each coordinate of the `CLEAN … return=x y z` line, floored. Every
  unchained row starts there, and its `execute align xz …` keeps the block.
- **The slot** is anchored on the wall block (X0, Y0, Z0 + 2), face NORTH: its id is
  **`<world uuid>:X0,Y0,<Z0+2>:NORTH`**, the same in every row.
- **A charged click** reads `cooldown=1.0000`; its `aim=` is the live spot's box centre, **`cu.50,cv.50`** with
  `(cu, cv)` the live spot from the latest `show` (`SelfTestMiningAimTest` casts every legal centre of all four sizes
  through the server's own arithmetic and asserts the read point is within 0.005 px of the centre, and in the box).
- **Its `DRIVE dig` line** repeats the cooldown the click line reads, with `attack_speed=1.200`, `face=NORTH`,
  `cancelled=true`, and `item=iron_pickaxe` (or `stick` in A10).
- **A spare click** (a row clicks a size's MAXIMUM spot count; a patch with fewer breaks early): its `DRIVE aim_patch …
  patch=none aim unchanged`, then `MINING click … outcome=NOT_LIVE spot=- cooldown=1.0000 … miss_sound=-`.
- **Formats** are `GATE-mining-s2.md`'s (*Reading a MINING line*), pinned by `MiningTraceLineTest`.
- Payout ranges (PLAN-mining.md §5.12, executed there): small 9..17, medium 14..26, large 20..36, extra_large
  26..46; bonus 5 / 6 / 8 / 10; `regrow_at` = the BROKE click's tick + 1200.

| row | AUTO (the server's log) | Ben's half, or why not AUTO |
|---|---|---|
| A1 | both refusals, and the control roll | — (no sight, no sound) |
| A2 | 21 rolls at a medium slot | — |
| A3 | the extra_large break, payout | the patch's look: NOT AUTO |
| A4 | the re-show after a same-world respawn; no pack line | **the pack's art after the death: Ben, Q1** |
| A5 | the three place refusals | — |
| A6 | small, medium, large mined to the break | the look and the break sound: NOT AUTO |
| A7 | the charge boundary at 16 and 17 ticks | the crosshair indicator: NOT AUTO |
| A8 | MISS beside; UNCHARGED in the box changes nothing | the miss sound: NOT AUTO (the `miss_sound=` FIELD is the server's decision, and is read) |
| A9 | the miss-sound cooldown, as the `miss_sound=` field | the sound heard: NOT AUTO |
| A10 | DENIED's hold, `sound=` field, one hold-end | the action-bar text seen, the sound heard: NOT AUTO |
| A11 | a click outside the patch is judged as nothing | — |
| A12 | a real regrowth at `regrow_at` | the patch coming back, seen: NOT AUTO |

---

## THE ROWS

### A1 — the two roll refusals, and the control (SURVIVAL) — witness: the log

**Carried row 4** (PLAN-mining.md §6). A medium slot; `roll large`, `roll huge`, then `roll medium`.

| prediction | READING |
|---|---|
| `MINING place slot=<id> ore=iron max_size=medium footprint=` 15 blocks `by=BaronVonYeetus`, then a traced `MINING show … size=small` or `size=medium`. **1:** `MINING roll-refused player=BaronVonYeetus reason=Size 'large' (7 x 3) does not fit this slot, whose maximum is 'medium' (5 x 3).` **2:** `MINING roll-refused player=BaronVonYeetus reason=No vein size 'huge' is loaded.` Each with its red REPLY, the same text. **No `MINING roll player=` line for either.** **3, the control:** `MINING roll player=BaronVonYeetus slot=<id> size=medium spots=` 4 or 5, then `MINING show … size=medium … live=1/n`. Then `MINING remove slot=<id>`. **NOT STAGEABLE:** the third refusal (*"Ore 'iron' does not list size …"*), iron lists all four sizes; unit-tested only (`MiningBookTest`), NOT READ, never a pass | _(not run)_ |

### A2 — a medium slot never shows large or extra_large, at 21 rolls (SURVIVAL) — witness: the log

**Carried row 5** (S2-MEDIUM-CAP), at the 21 placements `GATE-mining-s2.md` derived: a mis-wire to the ore's largest
size goes unseen with chance (80/100)^21 = **0.0092**, to large with (80/95)^21 = **0.027** (re-executed in jshell for
this file). Each placement shows a fresh patch through `MiningBook.patch`, the creation point regrowth uses (F-M38).

| prediction | READING |
|---|---|
| **21** `MINING place slot=<id> ore=iron max_size=medium …` lines, each followed by one traced `MINING show … size=small` (`spots=` 2 or 3) **or** `size=medium` (`spots=` 4 or 5), `live=1/n`, and then `MINING remove slot=<id>`. **NONE `size=large` or `size=extra_large`.** **Both sizes appear**: all 21 small has chance (50/80)^21 = 5.17e-5 and all medium (30/80)^21 = 1.13e-9 (jshell); one size only is recorded as a reading, not a defect. The reading tallies small : medium | _(not run)_ |

### A3 — an extra_large patch mined to the break (SURVIVAL) — witness: the log

**Carried row 6** (the extra_large break half).

| prediction | READING |
|---|---|
| `MINING roll … size=extra_large spots=` n, **n = 8 or 9**; `MINING show … size=extra_large … live=1/n`. Then **n − 1 `HIT` and one `BROKE`**, the k-th at `spot=k/n`, every one `cooldown=1.0000` and `aim=cu.50,cv.50` for the k-th centre of the show line (so IN its box). The break line: `MINING break … size=extra_large payout=raw_ironxN rolls=` n rolls each 2..4 `bonus=10 kept=N dropped=0 regrow_at=` the BROKE tick + 1200, **N = the rolls + 10, inside 26..46** (26..42 if n = 8, 28..46 if n = 9). **If n = 8, the 9th click is a spare** (`NOT_LIVE`). Then `MINING remove` | _(not run)_ |

### A4 — a same-world death and respawn re-shows the patch; no pack line (SURVIVAL) — witness: the log + Ben (Q1)

**Carried rows 2 and 3.** A small patch, one HIT (so its live index is 2, where a fresh patch's is 1), a marker
entity at the feet, `death_rules`, `kill @s`. After the respawn, `tp` to the marker, then 100 ticks for Ben's look.

| prediction | READING |
|---|---|
| `MINING roll … size=small spots=` n and its show `spots=` S `live=1/n`; one `HIT … spot=1/n`. `DEATH_RULES keep_inventory=…->true immediate_respawn=…->true`; `CMD /kill @s`; `REPLY "Killed BaronVonYeetus"`; `SETTLED dying=0 ticks=0 first=0` (the player is not counted); **`RESPAWNED ticks=<r>`**. **Then, before the row's DONE: a traced `MINING show player=BaronVonYeetus slot=<id> size=small spots=` S, the SAME list, `live=2/n`**, and **no `MINING click` line between the kill and that show**. It comes at the respawn if the respawn point is within view distance of the slot (the old chunks unload and are sent again, PLAN-mining.md F8a), or after the `tp` otherwise; **the reading records which**, by whether it precedes `CMD /tp`. **No `MINING pack offer` and no `MINING pack status` line between `CMD /kill @s` and DONE** (the pack was offered once, at LOGIN; a respawn is not a reconnect). `RULES restored keep_inventory=… immediate_respawn=…` at the row's end. **If the tp replies `No entity was found` and no show came at the respawn, the re-show half is NOT READ** (the marker's chunk was unloaded), recorded so | _(not run)_ |

**Q1 (Ben, carried row 3's sight half).** *"In the mining part of the run, right after your screen flashed (the run
killed you and you came straight back), the wall in front of you showed a patch for about five seconds. Was it drawn
with its art, NOT purple-and-black squares?"* yes / no / did not see. **Bound to this row only if the log shows the
re-show above.**

### A5 — the three place refusals (SURVIVAL) — witness: the log

The `MINING place-refused` line is always logged, with the chat text verbatim (S1-c).

| prediction | READING |
|---|---|
| **1, an UP face** (the floor block in front, (X0, Y0 − 1, Z0 + 1), aimed at its top): `MINING place-refused player=BaronVonYeetus reason=A patch goes on a wall (north, south, east or west face), not up.` **2, an air row** (the wall cut to 2 high; a medium footprint is 3 high): `… reason=Block <X0+2>,<Y0+2>,<Z0+2> is not solid.`: the footprint is checked row by row from the bottom, each row from the left as the player sees it, and facing south the player's left is +X, so the first air block is the top row's left end. **3, the control, then the duplicate:** the wall rebuilt, `MINING place slot=<id> ore=iron max_size=medium …` (placed), then `… reason=a slot already exists at <id>`, the same id. Each refusal with its red REPLY | _(not run)_ |

### A6 — small, medium and large, each mined to the break (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| Three segments at one extra_large slot, each `MINING roll … size=S spots=n` and its show `live=1/n`, then n − 1 `HIT` and one `BROKE`, every click `cooldown=1.0000` and `aim=cu.50,cv.50` IN its box, then the break line `size=S … bonus=B kept=N dropped=0`, and any spare clicks as `NOT_LIVE`. **small:** n = 2 or 3, bonus 5, N in 9..17 (9..13 if 2, 11..17 if 3), 3 clicks. **medium:** n = 4 or 5, bonus 6, N in 14..26 (14..22 / 16..26), 5 clicks. **large:** n = 6 or 7, bonus 8, N in 20..36 (20..32 / 22..36), 7 clicks. **Each `roll` follows the previous break with no `regrow-all`**: `force` ends that slot's regrowth | _(not run)_ |

### A7 — the charge boundary: 16 ticks is UNCHARGED, 17 is HIT (SURVIVAL) — witness: the log

A large patch (6 or 7 spots, so none of these three clicks breaks it).

| prediction | READING |
|---|---|
| **1:** `HIT spot=1/n cooldown=1.0000` (its `dig` resets the ticker: the known reset). **2, 16 ticks later:** `UNCHARGED spot=2/n cooldown=0.9900`, `aim=` IN spot 2's box, tick = click 1's tick + 16. **3, 17 ticks after click 2** (click 2's `dig` reset the ticker too, as a real click's swing does): `HIT spot=2/n cooldown=1.0000`, tick = click 2's + 17. **The pair 0.9900 / 1.0000 at 16 / 17 is the jar's arithmetic above, executed.** The `DRIVE dig` lines read the same three cooldowns. **Spot 2 is still live at click 3**: the UNCHARGED click moved nothing | _(not run)_ |

### A8 — a MISS beside the live spot, and an UNCHARGED click that changes nothing (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| **1:** `DRIVE aim_patch … target=beside pixel=` the live centre ± 5 px on u; `MISS spot=1/n cooldown=1.0000 aim=` that pixel `miss_sound=yes` (no miss sound played in the 8 ticks before). **2, 5 ticks later:** `UNCHARGED spot=1/n cooldown=0.3300 aim=cu.50,cv.50` (IN the box), `miss_sound=-`. **3, 17 ticks later:** `HIT spot=1/n cooldown=1.0000`: **the live spot is STILL spot 1**, so the UNCHARGED click changed nothing. No `click-outside` line (`beside` stays on the patch: `SelfTestMiningAimTest`) | _(not run)_ |

### A9 — the miss-sound cooldown at 7 and 8 ticks (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| Three `MISS` lines, all `spot=1/n`, at ticks **t0, t0 + 7, t0 + 8**: **`miss_sound=yes`, `miss_sound=no`, `miss_sound=yes`** (8 ticks after the last PLAYED sound plays; a silent miss does not restart the count; `MissSoundCooldown`, `MiningNotices.MISS_SOUND_COOLDOWN_TICKS = 8`). Cooldowns `1.0000`, `0.4500` (7 after the first dig's reset), `0.0900` (1 after the second's). The ear's half is NOT AUTO | _(not run)_ |

### A10 — DENIED with a non-pickaxe: one sound per message, one hold-end (SURVIVAL) — witness: the log

The main hand becomes a stick (`item replace`), so the event carries what a real click would. **The ticker:** the
stick's type change resets it inside `Player#tick` (after that tick's increment) and a stick's attack speed is the
base 4.0, a delay of 5.0; so the first DENIED, 5 ticks after the swap, reads `(4 + 0.5) / 5.0` = **0.9000**, and the
second, 10 ticks after the first's reset, **1.0000** (side readings, executed in jshell; DENIED does not read them).

| prediction | READING |
|---|---|
| **1, tick t1:** `DENIED spot=1/n … miss_sound=-` (DRIVE `item=stick attack_speed=4.000 cooldown=0.9000`), then **`MINING hold player=BaronVonYeetus reason=denied start=t1 until=t1+30 sound=yes`** (no hold was live). **2, tick t2 = t1 + 10:** `DENIED`, then **`hold … start=t2 until=t2+30 sound=no`** (t1's hold was live: it REPLACES it). **Exactly one `HUD hold-end player=BaronVonYeetus reason=denied start=t2 until=t2+30 tick=` e, with e in t2 + 30 .. t2 + 39** (the stats bar's first 10-tick beat at or after the end), inside the row; **none with `start=t1`** | _(not run)_ |

### A11 — a click on a slot block outside the current patch (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| A small patch at an extra_large slot covers columns X0 − 1..X0 + 1, rows Y0..Y0 + 1. **1:** `DRIVE dig … block=<X0+3>,<Y0+3>,<Z0+2> face=NORTH`, then **`MINING click-outside player=BaronVonYeetus slot=<id> size=small block=<X0+3>,<Y0+3>,<Z0+2> tick=t`**, and **NO `MINING click` line at tick t**. **2, the control:** a charged click on the live spot, `HIT spot=1/n`: the same path judges a click on the patch | _(not run)_ |

### A12 — a real regrowth, at regrow_at (SURVIVAL) — witness: the log

A small patch forced at a MEDIUM slot and broken, then 1210 ticks.

| prediction | READING |
|---|---|
| The break line `size=small … regrow_at=` **R** (the BROKE tick + 1200). **At tick R exactly:** a traced `MINING show … size=` S `… live=1/m tick=R` and `MINING regrow … size=S spots=m tick=R`, with **S small or medium** (the slot's maximum is medium; 50/80 and 30/80) and m in S's range. **No `MINING click` line between the break and R except at most one `NOT_LIVE`** (the third click, if the patch had 2 spots), within 20 ticks of the break | _(not run)_ |

---

### LAST — the run ends and restores (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `SELFTEST RUN FINISHED`, then `SELFTEST RUN RESTORED xp=<Ben's> … gamemode=SURVIVAL … keep_inventory=<as found> immediate_respawn=<as found>`. **Under `all`: `SELFTEST RUN START all rows=50`; ten setups in index order, `mining-auto` last; 60 scenario STARTs, each with exactly one `DONE` or `SKIPPED`** (every SKIPPED is a reading, with its reason). **No `SELFTEST` line contains `PASS` or `FAIL` as a word** (`grep SELFTEST \| grep -cwE 'PASS\|FAIL'` = 0) | _(not run)_ |

### NOT-AUTO — what stays HAND, and why (recorded NOT READ, never a pass)

- **Every sight and every sound**: the patch art, the spot and crack models, the break particle, the miss, break and
  DENIED sounds heard, the action-bar text seen, the crosshair indicator. P3.
- **Whether the pack's art survives a death**: Ben's Q1, bound to A4.
- **Relog rows** (the part-mined relog, the logout and `regrow_at` while away): a run cannot disconnect its own
  player, and the quit ENDS the run (`SelfTestService.onQuit`).
- **A second player** (M-SECOND-PLAYER, carried row 1): the run refuses with two players online (`SelfTestGuard`).
- **The client's dig**: the crack, a ghost break, the dig sound, what a held click sends (PLAN-mining.md F-M27).
- **G2**, the non-dev half of the guard: unit only.

## HOW LONG IT RUNS (computed from the files, not measured)

From the repo root, per gate: the sum of `wait:` steps, plus 2 ticks per scenario end (the framework's `rowDone`
delay), setups included:

```bash
cd paper/src/main/resources/selftest; for g in $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - //'); do f=GATE-$g.yml; echo "$g waits=$(grep -E '^\s*- wait:' $f | tr -d '\r' | awk '{s+=$3} END{print s+0}') scenarios=$(grep -cE '^[A-Za-z0-9-]+:' $f)"; done
```

- **The mining gate:** waits **2055** + 2 × 13 scenarios = **2081 ticks, about 104 s at 20 TPS**, plus A4's respawn
  round trip (a few ticks).
- **The whole `/rpg selftest all`:** waits **7617** + 2 × 60 = **7737 ticks**, plus the settle's new waits, about 20
  ticks for each kill that leaves a body (10 in run 3, ST9R): **≈ 7937 ticks, about 6 min 37 s at 20 TPS.**
  **The calibration:** at `01727a8c` the same formula gives 5656 ticks, and run 3 of `e82df2b3` measured **5701**
  (its `DONE ticks=` summed: `sed -n 2206,3657p run/logs/latest.log | grep -oE "DONE ticks=[0-9]+" | awk -F= '{s+=$2}
  END{print s}'`), 285 s by its own timestamps (09:07:30 to 09:12:15): +45 ticks, SU8's respawn among them. So
  expect **about 6 min 40 s**, more under a slower tick rate. **Ben does nothing while it runs** except look at the
  wall for A4.

## THE RUN SHEET, IN OUTLINE (the booter posts it literally, filled from readings in hand; no placeholder)

1. Ben logs in. Read `MINING pack offer` and its three status lines (the carried state's prediction).
2. **`/gamemode survival`**, alone, the first line after LOGIN (F-M46), read as `BaronVonYeetus issued server
   command: /gamemode survival`.
3. **`/difficulty`** (it prints the world's difficulty; recorded), then **`/difficulty hard`** (the difficulty run 3
   of `e82df2b3` ran at), each read as an `issued server command` line.
4. Ben stands where the earlier gates already need him (flat open ground, about 12 blocks clear to the east), with
   **four inventory slots free**, facing anywhere. The mining rows turn him south and build, then put back, their own
   box: 9 wide, 6 deep, from the floor to 5 above his feet (`save_blocks: "-4 -1 -1 4 5 4"`).
5. **`/rpg selftest all`**. Wait for `SELFTEST RUN RESTORED` (about 6 min 40 s). During the mining part, when the
   screen flashes (A4's death), look at the wall.
6. **Questions (three at most):** Q1 (A4, above); Q2 *"Is the item that was in your hand before the run back in your
   hand?"* (the restore of the main hand the mining rows replaced); Q3 *"Any chat line in red during the run that
   was not about mining?"* (a SKIPPED or refusal the log should already show; a cross-check).
7. **OPTIONAL, offered, not required:** Sunder's carried **SU10** by hand (a Sunder leap off a ledge takes no fall
   damage), as `GATE-sunder.md` writes it.
8. **`/stop`**.

## UNIT ROWS AND MUTATIONS (run 2026-11-14 against this branch's working tree, before this file was committed)

**The unit rows.** `SelfTestMiningAimTest` (6): the face centres; the four targets and their refusals; **every legal
spot centre of all four sizes (9,224 casts per verb pair, counted by the row) cast through float yaw and pitch,
Bukkit's direction, `FaceFrame.worldToPatch` and `SpotBox`**: `live` lands IN the box within 0.005 px of its centre,
`beside` lands OUTSIDE it and ON the patch, every target's block is in the patch footprint and off every block edge,
and the farthest aim is inside the 4.5-block dig reach; the control (the FEET anchor misses). `SelfTestMiningDriversTest`
(3): the dig's read, event, fire and reset order and its main-hand item and reach; the aim from the eye; the drivers'
MiningService calls are `peek`, `isTracing`, `trace` (only to turn it on). `MiningBookTest` +2: `peek` never creates
and never draws; it is empty while regrowing and leaves the clock alone. `SelfTestScenariosTest`: ten gates,
`mining-auto` last, 50 rows, A1-A12 paired with this file's headings.

Each mutation: one literal edit by `mut.sh` (refuses unless the original occurs exactly once), marker grep, line delta
against a pristine copy taken before the edit, `-Dtest='SelfTest*Test,MiningBookTest'`, restored by `cp`,
`cmp`-identical, markers left 0.

| mutation | edit | marker / original / lines | reddened |
|---|---|---|---|
| MUT-M-RESETFIRST | `p.resetCooldown();` before the dig's read | 1 / **1** / 1 | `SelfTestMiningDriversTest.aDigReadsTheChargeThenFiresTheEventThenResetsAsTheSwingDoes` |
| MUT-M-FEET | `turnEyeTo` passes `0.0` for the eye height | 1 / 0 / 1 | `SelfTestMiningDriversTest.theMiningAimsTurnTheEyeNotTheFeet` |
| MUT-M-BESIDE | `BESIDE_PX = 2.0` | 1 / 0 / 1 | `SelfTestMiningAimTest.theTargetsAreBoxCentresAndPixelCentres`, `…everyLegalSpotOfEverySizeIsHit…` |
| MUT-M-NOFALLBACK | `beside` always moves right | 1 / 0 / 1 | the same two |
| MUT-M-PEEKANY | `MiningBook.peek` reads any player's patches | 1 / 0 / 1 | `MiningBookTest.peekReadsTheExistingPatchAndNeverCreatesOne` |
| MUT-M-TOGGLE | `mining_trace` toggles whatever the state | 1 / 0 / 1 | `SelfTestMiningDriversTest.theVerbsReadTheMiningServiceAndChangeNothingButTheTrace` |
| MUT-M-PAIR | `A7:` → `A7x:` in the scenarios | 1 / 0 / 1 | `SelfTestScenariosTest.everyScenarioNamesARowOfItsGate`, `theScenariosAreExactlyTheCoreRows` |

**MUT-M-RESETFIRST's `original 1` is not a failed edit:** its replacement CONTAINS the original statement (it adds a
reset before it), so the marker grep cannot be read (`verification.md`, the eighth row); the line delta, 1, is the
witness, and the row it reddened asserts the reset appears ONCE.

**What no unit row can see, and is this gate's to read:** that the server's tick order puts the dig's read at exactly
n ticks after a reset (A7), that the shipped listeners receive the synthetic event (every A row's MINING click line),
and that a respawn re-shows the patch (A4).
