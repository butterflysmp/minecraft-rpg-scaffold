# GATE — MINING S1-c: the DENIED message holds the action bar, and slice 1's carried rows

**Status: NOT RUN.** Written 2026-11-14, before any boot of `feat/mining-s1c`. **NO BOOT until the seat has diffed
S1-c's code AND this gate, and Ben says ready.**

**READ 2026-11-14** (the seat's approval of 2026-11-14 for `0578366d` only, on Ben's "ready"). One boot,
`./scripts/dev-server.sh --refresh-content`, `[Rpg] Build: 0578366d`; Ben's LOGIN at L81 of that boot's
`run/logs/latest.log` (06:40:50), `/stop` at L242 (06:52:07). Every `L<n>` below is a line of that log. Ben's answers
are recorded as he gave them; readings sit beside the predictions, which are unchanged.

**CARRIED STATE, AS DONE.** No JVM (0). Backup `C:\Users\Neb91\IdeaProjects\rpg-run-backups\2026-11-14-pre-mining-s1c`,
`diff -rq` empty: `run/plugins/Rpg` 114 files / 349382 bytes, source and copy; `run/world` 193 files / 204963643
bytes, source and copy. Then `run/plugins/Rpg/config.yml`: last three lines BEFORE `  #   too loose (e.g. 1.0) ...`,
`  # The target is "neither buzzes nor drifts." ...`, `  anchor-drift-blocks: 0.4`; AFTER `  anchor-drift-blocks: 0.4`,
`mining:`, `  dev-pack-on-join: true` (10 lines to 12, 0 CRs; the file is git-ignored). Working tree clean at
`0578366d`, equal to `origin/feat/mining-s1c`; Paper build 74; PacketEvents 2.13.0.

**THE READING IN ONE PLACE.** R0 PASS. **PASS:** C-PACK-JOIN (carried row 7, the toggle ON, READ), C-TRACE,
C-DENY-HOLD (13 DENIED clicks, 13 holds of 30 ticks, 6 releases each 0 to 8 ticks after its end, no release for any
of the 7 replaced holds), C-BREAK-SOUND, LAST. **Log half PASS, ear count NOT AS PREDICTED:** C-MISS-SOUND and
C-UNCHARGED-SILENT. The client plays its own deepslate dig sound on every click, so Ben heard one more sound per click
than predicted; ours played exactly as ruled (a miss sound on each MISS, none on UNCHARGED, Ben: "The miss sound
didn't play"). **Q3 named no sound** ("I don't likeit"): whether the sound rows count under that precondition is the
seat's ruling. **Step 3 only:** C-PLACE (the three refusals were not run). **NOT READ, NOT RUN:** C-SERVER-LIST,
C-FULL-GROUND, C-RELOG-SIGHT, C-LOGOUT-SIGHT, C-REMOVE-DIG. Ben stopped the server after C-BREAK-SOUND, and he
answered Q14 to Q21 although those rows have no lines in the log; the answers are recorded and bound to no reading.
**NOT READ:** C-SECOND-PLAYER (no second client). **Set-up step 1 never ran**, so SURVIVAL rests on an inference
(C-TRACE's reading). **Out of the steps, read anyway:** an unplanned break right after the place (L110 to L114, with
`0.9900` read as UNCHARGED on an in-box click), two natural regrowths at exactly `regrow_at` (L117, L236), and a held
sweep of 72 MISS lines 1 to 9 ticks apart (C-MISS-SOUND).

**STILL CARRIED TO THE NEXT MINING GATE** (never passes): the three refusals (row 8), M-SERVER-LIST (row 4), the
ground check (row 9), the post-remove dig (row 10), M-RELOG's and M-LOGOUT's sight with the pack (row 12),
M-SECOND-PLAYER (row 5), the same-world respawn (row 6) and whether a death drops the pack (row 11). Carried row 7
(the toggle ON) is READ here. Rows 1 to 3 (the sounds) await the seat's ruling on Q3.

**What it reads.** (1) Ben's ruling, 2026-11-14: *"Is it possible to have the pickaxe message over ride the action
bar for 1.5s"*. The DENIED notice now owns the action bar for `MiningNotices.DENIED_HOLD_TICKS` = **30 server ticks**
(`core.hud.ActionBarHolds`; the stats bar re-sends it on each of its 10-tick beats inside the hold). (2) The twelve
CARRIED HAND ROWS from `PLAN-mining.md` §6, each restated in full below, plus the `MINING place-refused` line that
makes M-PLACE's refusals a log reading.

Every prediction below was written **before** any row was read, and **this file is COMMITTED before the boot**.
Readings go **beside** a prediction, never over it. A prediction is not edited once its row has been read. Readings
count only if Ben's LOGIN appears in THIS boot's log.

```
ROWS 17   R0a R0b R0c / C-PACK-JOIN C-TRACE C-PLACE C-SERVER-LIST C-DENY-HOLD C-MISS-SOUND C-UNCHARGED-SILENT
          C-BREAK-SOUND C-FULL-GROUND C-RELOG-SIGHT C-LOGOUT-SIGHT C-REMOVE-DIG C-SECOND-PLAYER LAST
          = 3 R0 + 13 C + LAST
          git grep -c -E '^### (R0|C-|LAST)' origin/feat/mining-s1c -- docs/gates/GATE-mining-s1c.md
```

**NO PLACEHOLDER IN ANY COMMAND (PLAN F-M25).** Every command below is typed exactly as written. Where a value is
known only at run time (C-REMOVE-DIG's block), the row says so, and **the booter reads it from the log and posts the
exact command** before Ben needs it.

**Ben's sight answers** are numbered **Q1, Q2, ...** in the order he meets them, each **yes / no / one word / one
number**. "Not sure" is recorded as NOT STATED, never interpreted, and never re-asked.

## GAME MODE

**SURVIVAL on every row.** CREATIVE breaks a block before `BlockDamageEvent` (PLAN F2b) and removes costs; ADVENTURE
never reaches `BlockDamageEvent` (F2), and Ben ruled players are never in it. **No row sends Ben to another
dimension, and no row stages a death.** Confirm with `/gamemode survival` in the set-up. A row read in any other mode
is VOID.

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, taken with no server running: `run/plugins/Rpg` and `run/world` to a new folder
   `C:\Users\Neb91\IdeaProjects\rpg-run-backups\` named by the boot date and `-pre-mining-s1c`, with `cp -a`; report
   each tree's file count and byte total for source and copy (`find run/plugins/Rpg -type f | wc -l` and
   `find run/world -type f | wc -l`, the same for the copy's two trees, and the sum of `-printf '%s\n'` over each)
   and that `diff -rq` of each pair prints nothing. `run/world` holds the nether
   and the end under `dimensions/`. **The backup is taken BEFORE step 3**, so it holds the config as it was.
3. **Turn the join toggle ON in Ben's local file, not the repo default** (the seat, 2026-11-14; PLAN F-M26).
   `run/plugins/Rpg/config.yml` has **no `mining:` section** (read 2026-11-14: `grep -n mining` finds nothing; the
   file is 10 lines and ends at `anchor-drift-blocks: 0.4`). Append these two lines at the end, `mining:` at column 0:
   ```yaml
   mining:
     dev-pack-on-join: true
   ```
   Record the file's last three lines before and after, and `git status` showing nothing (the file is not tracked).
   `--refresh-content` clears only `run/plugins/Rpg/content` (`scripts/dev-server.sh`, its *Refresh content*
   section), so the edit survives the boot.
4. **Boot:** `./scripts/dev-server.sh --refresh-content`.

**Also carried, not actions:**
- **Ben's saved build names `sunder`**, which master does not have. Slice 1's boot read no `[Rpg]` warning at any of
  three LOGINs. **Prediction: none again. A warning at LOGIN is recorded, not an R0 failure.** Ben should not open
  the build or loadout menu, and should not use the enchanting table, anvil or grindstone.
- **The previous session ended with a clean `/stop`** (slice 1's LAST STEP, 2026-11-14).
- **Slice 1's deepslate wall is still in the world** at x −118 to −116, y 63 to 64, z 9, with no slot on it (it was
  removed). This gate builds a new wall with a relative `/fill`, wherever Ben stands.
- `run/server.properties` keeps `require-resource-pack=false`; this gate does not change it.

## THE HIT BUDGET

A Small patch has **n = 2 or 3** spots: the first n − 1 charged clicks IN the live spot are `HIT`, the n-th is
`BROKE`. DENIED, MISS and UNCHARGED clicks spend nothing.

| row | spots spent | how the patch starts |
|---|---|---|
| C-DENY-HOLD, C-MISS-SOUND, C-UNCHARGED-SILENT | **0** | the placed patch |
| C-BREAK-SOUND | all (it breaks) | the same patch |
| C-FULL-GROUND | all (it breaks) | fresh, from `/rpg mining regrow` |
| C-RELOG-SIGHT | 1 (a HIT, since n ≥ 2) | fresh, from `/rpg mining regrow` |
| C-LOGOUT-SIGHT | the rest (it breaks) | C-RELOG-SIGHT's part-mined patch |

**THE STANDING RULE: if the patch breaks before the row that should break it, run `/rpg mining regrow` and carry on.**
The break line is recorded under the row it landed in.

## THE ORDER IN THE ONE BOOT

Carried state → R0 (before Ben logs in) → C-PACK-JOIN (his LOGIN) → set-up → C-TRACE → C-PLACE → C-SERVER-LIST →
C-DENY-HOLD → C-MISS-SOUND → C-UNCHARGED-SILENT → C-BREAK-SOUND → C-FULL-GROUND → C-RELOG-SIGHT → C-LOGOUT-SIGHT →
C-REMOVE-DIG → C-SECOND-PLAYER → LAST.

## Set-up (after Ben logs in and C-PACK-JOIN's lines are read)

1. `/gamemode survival`, `/difficulty peaceful`, `/gamerule spawn_mobs false`, `/time set 6000`, `/weather clear`.
2. **Turn GAME AUDIO ON** (the client's Music & Sounds: Master and Blocks above 0). Then run
   **`/playsound minecraft:block.note_block.bell master @s`** and answer Q3. No sound row is read until Q3 names a
   sound.
3. Stand on flat ground facing **south**. Run **`/fill ~-1 ~ ~3 ~1 ~1 ~3 minecraft:deepslate`**: a wall 3 wide and
   2 high, three blocks south, its north face toward you. Clear anything in front of it.
4. **`/give @s minecraft:iron_pickaxe`** and hold it for every row unless the row says otherwise.
5. Leave **one hotbar slot EMPTY** (C-DENY-HOLD uses an empty hand).
6. Answer Q4 (raw iron in your inventory now) and Q5 (a second client).

**Q3.** One word for the sound `/playsound` just played. **Ben: "I don't likeit"**: not a one-word name of the sound; the sound rows' precondition is the seat's ruling
**Q4.** How many raw iron are in your inventory now? (one number) **Ben: "13"** (answered after the unplanned first break, which paid 13)
**Q5.** Is a second Minecraft client available on this machine? (yes/no) **Ben: "No"**

**Reading a MINING line** (formats pinned by `MiningTraceLineTest` and `ActionBarHoldsTest`). Lines marked *traced*
print only while the trace is on (C-TRACE): `click`, `show`, `break`, `regrow`, `hold`, `HUD hold-end`. These always
print: `MINING place`, `MINING place-refused`, `MINING remove`, `MINING trace`, `MINING regrow-all`, `MINING pack offer`
and `MINING pack status`.
- `MINING show player=P slot=ID spots=u,v;u,v... live=i/n tick=t`: spot centres in patch pixels.
- `MINING click player=P slot=ID outcome=O spot=i/n cooldown=c aim=u,v tick=t`.
- `MINING hold player=P reason=denied start=S until=T`, and later `HUD hold-end player=P reason=denied start=S
  until=T tick=E`, logged by the stats bar on its first beat at or after T.
- `MINING place-refused player=P reason=TEXT`: TEXT is the chat text verbatim, to the end of the line.

**A click is IN the live spot** when `cu − 2 ≤ aim.u < cu + 3` and `cv − 2 ≤ aim.v < cv + 3`, with `(cu, cv)` the live
spot's centre from the latest `show` line.

---

## R0 — THE BUILD. READ BEFORE BEN LOGS IN. IF ANY R0 FAILS, STOP

### R0a — the build line names the S1-c head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build:` followed by `feat/mining-s1c`'s head as read from `git ls-remote origin refs/heads/feat/mining-s1c` just before the boot, shortened. Not `-dirty`, not `unknown`, not master's `ef2f1b42` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS.** L43 `[Rpg] Build: 0578366d`: `feat/mining-s1c`'s head as read from `git ls-remote origin refs/heads/feat/mining-s1c` just before the boot (`0578366d493e8c762a34c39937ab55eae371d80e`). Not `-dirty`, not `unknown` |

### R0b — the jar carries S1-c

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `hud/ActionBarHolds`, `hud/ActionBarHolds$Beat$Held`, `mining/MiningNotices`; paper `hud/StatsBarSystem`, `mining/MiningService`; `resourcepack/pack.mcmeta`. ABSENT: the control `NoSuchClassControl` | the scan below | **PASS.** All six PRESENT; `NoSuchClassControl.class` ABSENT. Read before Ben's LOGIN |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/hud/ActionBarHolds.class',
               'io/github/butterflysmp/rpg/core/hud/ActionBarHolds$Beat$Held.class',
               'io/github/butterflysmp/rpg/core/mining/MiningNotices.class',
               'io/github/butterflysmp/rpg/paper/hud/StatsBarSystem.class',
               'io/github/butterflysmp/rpg/paper/mining/MiningService.class',
               'resourcepack/pack.mcmeta',
               'io/github/butterflysmp/rpg/paper/mining/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — content loads, unchanged from slice 1

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 14 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes, 1 ores, 1 vein sizes`, **exactly slice 1's READ R0c line** (`GATE-mining-slice1.md`, L43 of that boot): S1-c adds no content file. No `Content:` line names `ore 'iron'`; no `Skipping`, `No ores`, `No vein`, `SEVERE` or exception. The only `[Rpg]` WARN lines at enable are `volley_stone`'s known pair | `Select-String -Path run\logs\latest.log -Pattern 'Loaded \d+ abilities','Content:','Skipping','No ores','No vein','SEVERE','Exception'` | **PASS.** L45 is the predicted line exactly. No `Content:` line names `ore 'iron'`; no `Skipping`, `No ores`, `No vein`, `SEVERE` or exception. The only `[Rpg]` WARN lines are `volley_stone`'s pair (L46, L47). No `[Rpg]` warning at either LOGIN (L81, L91) |

---

## THE ROWS

### C-PACK-JOIN — the join toggle ON offers the pack at LOGIN, with no command (SURVIVAL) — witness: the log

**Carried row 7** (the join toggle's ON half). **Steps.** Ben logs in. He runs no command.

| prediction | READING |
|---|---|
| In the same second as Ben's `logged in` line, and with **no `issued server command: /rpg mining pack` before it**: `MINING pack offer player=BaronVonYeetus url=http://127.0.0.1:` then a port, `/rpg-pack.zip sha1=1e11f1c1e75b20cb547c34de0734812ca7ad2134 bytes=3225`. **The sha1 and bytes are slice 1's** (the zip is deterministic, `MiningPackTest`, and neither the pack nor `MiningPack` changed since `863bf10a`; `git diff --stat 863bf10a -- paper/src/main/resources/resourcepack paper/src/main/java/io/github/butterflysmp/rpg/paper/mining/MiningPack.java` is empty). The port is whatever the server picked. Then `MINING pack status ... status=ACCEPTED`, `DOWNLOADED`, `SUCCESSFULLY_LOADED`, in that order. **The same at EVERY later LOGIN in this boot** (C-RELOG-SIGHT, C-LOGOUT-SIGHT). **MEASUREMENT (client):** whether a prompt is shown; in slice 1 the client accepted without one | **PASS; the prompt is a MEASUREMENT.** L79 `MINING pack offer player=BaronVonYeetus url=http://127.0.0.1:55004/rpg-pack.zip sha1=1e11f1c1e75b20cb547c34de0734812ca7ad2134 bytes=3225`, in the same second as the LOGIN L81 (06:40:50), with **no command before it**: the sha1 and bytes are slice 1's, as predicted. Then ACCEPTED (L82), DOWNLOADED (L83), SUCCESSFULLY_LOADED (L84), in order. **A later LOGIN also offered it:** after two disconnects (L85 06:41:23, L87 06:41:27), L89's offer (same sha1) came with the LOGIN at L91 (06:41:33), then L92–L94. **Carried row 7, the toggle's ON half: READ.** Ben, Q1 "Yes" (a prompt was shown this time; slice 1's client showed none), Q2 "Yes" |

**Q1.** At login, did the game show a prompt to download a pack? (yes/no) **Ben: "Yes"**
**Q2.** Did the pack finish loading without an error? (yes/no) **Ben: "Yes"**

### C-TRACE — the trace turns on (SURVIVAL) — witness: the log

**Steps.** `/rpg mining trace`

| prediction | READING |
|---|---|
| `MINING trace player=BaronVonYeetus on=true` | **PASS.** L106 `MINING trace player=BaronVonYeetus on=true`. **Set-up step 1 never ran** (no `/gamemode`, `/difficulty`, `/gamerule`, `/time` or `/weather` in the log), so SURVIVAL was not confirmed by command. It is inferred from the jar facts: every `/rpg mining` click produced a `MINING click` line, which CREATIVE cannot (it breaks before `BlockDamageEvent`, F2b) and ADVENTURE cannot (it never reaches it, F2). Recorded as an inference, not a reading |

### C-PLACE — three refusals, as log lines, then the slot (SURVIVAL) — witness: the log

**Carried row 8** (M-PLACE's refusal texts), now a log reading. **Steps, in this order.**
1. Look at the **top** face of the wall's top-centre block. `/rpg mining place iron small`
2. Look at the **upper** row's centre block (north face). `/rpg mining place iron small`
3. Look at the **lower** row's centre block (north face). `/rpg mining place iron small` — **this is the slot.**
4. The same block again. `/rpg mining place iron small`

| prediction | READING |
|---|---|
| Below, **X, Y, Z are the three numbers in step 3's slot id** (the lower row's centre block), and **W is the world uuid** before them. **1:** `MINING place-refused player=BaronVonYeetus reason=A patch goes on a wall (north, south, east or west face), not up.` and no `MINING place` line. **2:** `MINING place-refused player=BaronVonYeetus reason=Block` then `X+1,Y+2,Z` `is not solid.`: the footprint runs one row up from the looked-at block, its first block on the player's LEFT (east, +X), and the row above the wall is air. No `MINING place`. **3:** `MINING place slot=W:X,Y,Z:NORTH ore=iron size=small footprint=` six blocks `by=BaronVonYeetus`, then a traced `MINING show ... live=1/2` or `live=1/3`. **4:** `MINING place-refused player=BaronVonYeetus reason=a slot already exists at W:X,Y,Z:NORTH`, **with no full stop** (`SlotRegistry.place`; slice 1's prediction carried one the code does not print, PLAN F-M31). **Exactly three `place-refused` lines in this row**; any other refusal line (a mistyped command, as in slice 1) is recorded with its text | **Step 3 PASS; steps 1, 2 and 4 NOT READ (not run).** One place command (L107) and L108 `MINING place slot=e404d973-…:-112,63,10:NORTH ore=iron size=small footprint=-111,63,10;-112,63,10;-113,63,10;-111,64,10;-112,64,10;-113,64,10`: six blocks, the first `-111` = X+1, the player's left (east). L109 `MINING show … spots=44,19;10,9 live=1/2`. **No `MINING place-refused` line exists in this boot: the three refusal steps were not run, so carried row 8 stays NOT READ** (never a pass). **Out of order, not in any step:** L110–L113 straight after the place: `HIT` (aim 43.92,19.60, IN (44,19)), `UNCHARGED cooldown=0.9900` at Δ 17 (aim IN (10,9); **0.9900 maps to UNCHARGED, read in game on an in-box click**), `UNCHARGED 0.6900`, `BROKE` (IN). L114 `payout=raw_ironx13 rolls=4+4 bonus=5 kept=13 dropped=0 regrow_at=11946` (= 10746 + 1200; 13 inside 9..13). THE STANDING RULE applied (a break before the row that should break it); `/rpg mining regrow` was not run, and the patch regrew BY ITSELF at L117–L118, `tick=11946` exactly, re-rolled to `44,11;45,27` |

### C-SERVER-LIST — the displays are not server entities, read in the log (SURVIVAL) — witness: the log

**Carried row 4.** **Steps**, standing within 8 blocks of the wall:
1. `/execute if entity @e[type=minecraft:item_display,distance=..8] run say C-DISPLAY-FOUND`
2. `/summon minecraft:item_display ~ ~ ~`
3. `/execute if entity @e[type=minecraft:item_display,distance=..8] run say C-CONTROL-FOUND`
4. `/kill @e[type=minecraft:item_display,distance=..2]`

| prediction | READING |
|---|---|
| **Read the `say` output, not the command echo**: each `issued server command` line also contains its token, so the reading is a line of the form `[Not Secure] [BaronVonYeetus] TOKEN` (spike 1's MS6 form, L578 of that boot). **1:** no `] C-DISPLAY-FOUND` line: the patch and spot are packet-only. **2:** `Summoned new Item Display`. **3:** exactly one `[Not Secure] [BaronVonYeetus] C-CONTROL-FOUND`. **This is the control: the instrument finds an item display when one exists. If 3 prints nothing, 1 proves nothing.** **4:** `Killed Item Display` | **NOT READ (not run).** None of the four commands is in the log, and no `C-DISPLAY-FOUND` or `C-CONTROL-FOUND` line exists. Carried row 4 stays NOT READ |

### C-DENY-HOLD — the DENIED message holds the bar for 30 ticks, and a second DENIED restarts it (SURVIVAL) — witness: the log + Ben's sight

**New row (Ben's ruling).** **Steps.**
1. Select the **empty** hotbar slot.
2. Left-click the live spot **once**. Watch the bar for 3 seconds.
3. Still empty-handed: left-click the live spot **twice, about half a second apart**. Watch the bar for 3 seconds.

| prediction | READING |
|---|---|
| **Step 2**, all traced: `MINING click ... outcome=DENIED spot=1/n ... tick=A`, then in the same tick `MINING hold player=BaronVonYeetus reason=denied start=A until=A+30` (`DENIED_HOLD_TICKS`; the two numbers **30 apart**). Then **one** `HUD hold-end player=BaronVonYeetus reason=denied start=A until=A+30 tick=E` with **A+30 ≤ E ≤ A+39**: the stats bar beats every 10 ticks at its own phase, so its first beat at or after the end lands in those ten ticks. Nothing changes on the patch (`spot=1/n` stays), no sound. **Step 3:** two DENIED lines at ticks **B1 < B2**, B2 − B1 under 30, each followed by its hold line, `start=B1 until=B1+30` and `start=B2 until=B2+30`. **The second hold REPLACES the first, so there is NO `HUD hold-end` line for `until=B1+30`**, and exactly **one** after the pair: `start=B2 until=B2+30 tick=E2` with **B2+30 ≤ E2 ≤ B2+39**. (If B2 − B1 is 30 or more, the first hold ends on its own: recorded, and step 3 repeated faster) | **PASS (log and sight).** **Step 2:** L121 `DENIED spot=1/2 … tick=12285`, L122 `MINING hold … start=12285 until=12315` (30 apart), L123 `HUD hold-end … start=12285 until=12315 tick=12319` (E − T = 4, inside 0..9). **Step 3 and repeats:** every one of the **13** DENIED clicks (L121–L150) has its hold line, each `until − start = 30`. **Six** `HUD hold-end` lines, each for the LAST hold of a run of clicks, with E − T of 4, 8, 2, 6, 0 and 3 (L123, L128, L131, L136, L147, L152): all inside 0..9, and **L147 lands exactly ON its end tick (13679)**. **The seven replaced holds have NO end line**: 13250 (replaced at 13271, Δ 21), 13354 (replaced at **13383, Δ 29, one tick before its end 13384**), 13607, 13627, 13637, 13645 (a run of five clicks 4–20 ticks apart, L137–L145), and 14048 (replaced at 14056). `spot=1/2` throughout: nothing changed. Ben, Q6 "Yes, we should add a sound to go with it" (a design note for Ben's ruling, not a reading), Q7 "yes", Q8 "Yes" |

**Q6.** After the first click, could you read the red message? (yes/no) **Ben: "Yes, we should add a sound to go with it"**
**Q7.** Did the health / defense / mana line come back by itself afterwards? (yes/no) **Ben: "yes"**
**Q8.** After the two quick clicks, did the red message stay up past the second click? (yes/no) **Ben: "Yes"**

### C-MISS-SOUND — a miss plays the miss sound, once per miss (SURVIVAL) — witness: the log + Ben's ear

**Carried row 1.** Back to the iron pickaxe. **Steps.**
1. Wait 1 second, then left-click the patch **well away from the live spot** (not on a crack). Answer Q9.
2. Left-click the patch away from the live spot **twice, about half a second apart**. Answer Q10.

| prediction | READING |
|---|---|
| **1:** one `MINING click ... outcome=MISS cooldown=1.0000` with the aim outside the live box; **the miss sound (`sounds.miss`, `minecraft:block.stone.hit`, PLACEHOLDER) plays to Ben only, once.** **2:** two MISS lines (the second's cooldown below 1: an uncharged click beside the spot is a MISS, by the order of checks) and **two** sounds. **Step 2 is the control for C-UNCHARGED-SILENT: it shows Ben's ear counts two sounds half a second apart** | **The miss sound PASS by ear; the COUNT NOT AS PREDICTED.** **Step 1:** L153 `MISS cooldown=1.0000 aim=32.97,17.04` (outside the live box of (44,11)). Ben, Q9: "2 sounds, the deepslate mining behind it and the sound for a miss": the miss sound played, and **a second sound, the client's own deepslate dig sound, plays on every click on the wall**; the prediction counted one. **Step 2:** L154 `MISS 1.0000` and L155 `MISS 0.4500` (Δ 8). Ben, Q10: "The same sounds but twice". **Then a held sweep not in the steps:** L156–L227, **72 more MISS lines in 246 ticks** (15059 → 15305) with the aim moving across the patch: gaps of 1 to 9 ticks between lines (1 tick 5 times, 2 ticks 37 times, 3 ticks 12 times, 4 ticks 8 times) apart from two pauses of 23 and 26, and **65 of the 72 cooldowns equal their gap's `(Δ+0.5)/16.666666` exactly** (Δ 1 → 0.0900, Δ 2 → 0.1500, Δ 3 → 0.2100); the other 7 read LOWER, consistent with a swing between click lines that logs nothing (L185 reads 0.5100 after a 23-tick pause, i.e. 8 ticks after some swing): a held button restarting the dig every 1–4 ticks (PLAN F-M27's open question, read here for the first time; see the readings commit). **Q3, the precondition, did not name a sound** (Ben: "I don't likeit"); recorded, and whether these ear readings count under it is the seat's ruling |

**Q9.** For the one click, how many sounds did you hear? (one number) **Ben: "2 sounds, the deepslate mining behind it and the sound for a miss."**
**Q10.** For the two quick clicks, how many sounds did you hear? (one number) **Ben: "The same sounds but twice"**

### C-UNCHARGED-SILENT — an uncharged click IN the live spot plays nothing (SURVIVAL) — witness: the log + Ben's ear

**Carried row 2.** **Steps.** Left-click the patch **beside** the live spot (outside its box, off any crack). Then,
**within half a second**, left-click **ON** the live spot. Answer Q11.

| prediction | READING |
|---|---|
| `MINING click ... outcome=MISS` (it plays the miss sound, and its swing resets the ticker), then `MINING click ... outcome=UNCHARGED cooldown=` below `0.6300`, with the aim IN the live box and the same `spot=` index. **The UNCHARGED click plays nothing, so Ben hears ONE sound for the two clicks** (against C-MISS-SOUND step 2's two). Nothing changes on the patch. (If the second click lands outside the box it is a MISS: recorded, and the row repeated) | **UNCHARGED plays nothing of ours: PASS by Ben's words; the count NOT AS PREDICTED.** L229 `MISS 1.0000 aim=34.38,14.52` (outside), L230 `UNCHARGED cooldown=0.5100 aim=44.33,11.67` (IN (44,11)) at Δ 9: the formula gives 0.5700, **one tick off**, as in slice 1. Repeated: L231 MISS, L232 `UNCHARGED 0.5100` at Δ 8 (exact), IN. Same `spot=1/2`, nothing changed. Ben, Q11: "2 sounds on the first click, 1 sound on the second. **The miss sound didn't play**". So the UNCHARGED click played no sound of ours; the one sound he heard is the client's dig sound (C-MISS-SOUND). Predicted "one sound for the two clicks"; read three, with ours exactly as ruled |

**Q11.** For these two clicks, how many sounds did you hear? (one number) **Ben: "2 sounds on the first click, 1 sound on the second. The miss sound didn't play"**

### C-BREAK-SOUND — the break plays the break sound and pays raw iron (SURVIVAL) — witness: the log + Ben's ear + inventory

**Carried row 3.** **Steps.** With room in the inventory, make charged hits on the live spot (wait 1 second before
each) until the patch breaks. Answer Q12 and Q13.

| prediction | READING |
|---|---|
| n − 1 `HIT` lines, then `MINING click ... outcome=BROKE` and `MINING break ... payout=raw_ironx` the count, `rolls=` one roll per spot, each 2, 3 or 4, `bonus=5 kept=` the count `dropped=0 regrow_at=` the break tick + 1200. **The count is the rolls plus 5**, inside 9..13 for 2 spots and 11..17 for 3. **The break sound (`sounds.break`, `minecraft:block.stone.break`, PLACEHOLDER) plays to Ben**, and a poof. **Q13 minus Q4 equals the count** | **PASS.** L233 `HIT spot=1/2` (aim 44.84,11.31, IN (44,11)), L234 `BROKE spot=2/2` (aim 45.39,27.68, IN (45,27)), L235 `payout=raw_ironx9 rolls=2+2 bonus=5 kept=9 dropped=0 regrow_at=19196` (= 17996 + 1200; 9 inside 9..13). Ben, Q12 "Yes" (the break sound); **Q13 22 minus Q4 13 = 9 = the count**. (Q4 was answered after the unplanned first break, which paid 13; 13 = 0 + 13.) **After the answers, not in any step:** the patch regrew by itself at L236–L237, `tick=19196` exactly, re-rolled to 3 spots; L238–L240 HIT, HIT, BROKE (each IN its box) and L241 `payout=raw_ironx14 rolls=3+2+4 bonus=5 kept=14` (inside 11..17) |

**Q12.** When the patch broke, did you hear a break sound? (yes/no) **Ben: "Yes"**
**Q13.** How many raw iron are in your inventory now? (one number) **Ben: "22"**

### C-FULL-GROUND — a full inventory drops the payout, and the ground check reads in the log (SURVIVAL) — witness: the log + Ben's sight

**Carried row 9.** **Steps, in this order.**
1. `/clear @s minecraft:raw_iron`
2. `/give @s minecraft:cobblestone 2304`
3. `/rpg mining regrow`
4. Make charged hits on the live spot until the patch breaks. Answer Q14.
5. `/execute if entity @e[type=minecraft:item,distance=..16] run say C-GROUND-BEFORE`
6. `/kill @e[type=minecraft:item,distance=..16]`
7. `/execute if entity @e[type=minecraft:item,distance=..16] run say C-GROUND-AFTER`
8. `/clear @s minecraft:cobblestone`

| prediction | READING |
|---|---|
| **3:** `MINING regrow-all player=BaronVonYeetus shown=1` (the patch was regrowing after C-BREAK-SOUND). **4:** the break line reads **`kept=0 dropped=`** the count (the seat's ruling: the remainder drops at the feet). **5:** exactly one `[Not Secure] [BaronVonYeetus] C-GROUND-BEFORE`: **the control, items are on the ground and the instrument sees them.** **6:** `Killed` N `entities`, N ≥ 1, while the inventory is still full so nothing is picked up first. **7:** **no `] C-GROUND-AFTER` line**: nothing is left on the ground. **8:** `Removed` some cobblestone. If 5 prints nothing, 7 proves nothing | **NOT READ (not run).** No `/clear`, `/give … cobblestone`, `/kill` or `C-GROUND-*` line in the log. Ben answered Q14 ("yes"), but no row ran in this boot for it to describe: recorded as given and **not bound to a reading**. Carried row 9 stays NOT READ |

**Q14.** Did raw iron drop on the ground at your feet? (yes/no) **Ben: "yes"**: the row did not run in this boot; NOT READ

### C-RELOG-SIGHT — a part-mined patch is back after a relog, seen WITH the pack (SURVIVAL) — witness: the log + Ben's sight

**Carried row 12, M-RELOG's half.** **Steps.**
1. `/rpg mining regrow`
2. Wait 1 second, then left-click the live spot once: one crack and a new live spot.
3. Disconnect, and reconnect.
4. **Wait 5 seconds after joining** (the pack loads), then look at the wall without clicking it. Answer Q15–Q17.

| prediction | READING |
|---|---|
| **1:** `MINING regrow-all ... shown=1` and a `MINING show ... live=1/n`. **2:** `HIT spot=1/n`, IN the box. **3:** at the new LOGIN, C-PACK-JOIN's offer line and its three status lines, **with no command**, and a `MINING show` whose **`spots=` equal step 1's show line** and whose live index is **`live=2/n`**, with no `MINING click` line between the LOGIN and it. **The reading records the times**: the `show` line's, and `SUCCESSFULLY_LOADED`'s. The show usually lands before the pack loads; **the sight questions are answered after `SUCCESSFULLY_LOADED`**, so the patch is drawn with the project models (slice 1's sight was without them: *"purple and black squares"*, PLAN F-M26) | **NOT READ (not run).** No `/rpg mining regrow`, and no disconnect after the slot was placed (the only relog, L85–L91, came before it). Ben answered Q15–Q17 ("Yes", "Yes", "Yes"); not bound to a reading. Carried row 12's M-RELOG half stays NOT READ |

**Q15.** Was the patch drawn with its own art, NOT purple and black squares? (yes/no) **Ben: "Yes"**: the row did not run in this boot; NOT READ
**Q16.** Was the crack still there? (yes/no) **Ben: "Yes"**: the row did not run in this boot; NOT READ
**Q17.** Was there exactly one live spot, where it was before? (yes/no) **Ben: "Yes"**: the row did not run in this boot; NOT READ

### C-LOGOUT-SIGHT — after a relog the patch comes back by itself, seen WITH the pack (SURVIVAL) — witness: the log + Ben's sight

**Carried row 12, M-LOGOUT's half.** **Steps.**
1. Make charged hits on the live spot until the patch breaks.
2. **Disconnect within 20 seconds** of the break, and **reconnect within 40 seconds** of it.
3. Look at the wall. **Do not click it until 60 seconds after the break**, or the row is VOID.
4. Answer Q18–Q20.

| prediction | READING |
|---|---|
| **1:** `BROKE`, and a break line with `regrow_at=` R (the break tick + 1200). **2:** at the new LOGIN, the offer line and its three status lines, with no command, and **no `MINING show` line** (the patch is regrowing; the chunk load re-arms the regrowth task, D1). **3:** at tick **R exactly**: `MINING show ... live=1/n` and `MINING regrow ... tick=R`, with **no `MINING click` line between the LOGIN and R** (else VOID). The reading records `SUCCESSFULLY_LOADED`'s time against R's line: the pack is predicted loaded well before R, so the return is seen with the models | **NOT READ (not run).** No disconnect after the slot was placed. Ben answered Q18–Q20 ("Yes", "Yes", "Yes"); not bound to a reading. Carried row 12's M-LOGOUT half stays NOT READ |

**Q18.** When you came back, was the wall bare at first? (yes/no) **Ben: "Yes"**: the row did not run in this boot; NOT READ
**Q19.** Did the patch come back by itself? (yes/no) **Ben: "Yes"**: the row did not run in this boot; NOT READ
**Q20.** When it came back, was it drawn with its own art, NOT purple and black squares? (yes/no) **Ben: "Yes"**: the row did not run in this boot; NOT READ

### C-REMOVE-DIG — after `/rpg mining remove`, a wall block digs normally, read in the log (SURVIVAL) — witness: the log + Ben's sight

**Carried row 10.** **The booter posts two commands before step 2**, built from C-PLACE step 3's slot id
(X, Y, Z: the slot's block, the lower row's centre): **an `/execute if block` at X Y Z testing
`minecraft:deepslate` with `run say C-DIG-BEFORE`, and the same at X Y Z testing `minecraft:air` with
`run say C-DIG-GONE`**, written out with the numbers filled in. Ben types them exactly as posted. **Steps.**
1. Look at the slot's block (the lower row's centre). `/rpg mining remove`
2. Run the two posted commands, deepslate first.
3. Mine that block with the iron pickaxe until it breaks.
4. Run the two posted commands again, in the same order. Answer Q21.

| prediction | READING |
|---|---|
| **1:** `MINING remove slot=W:X,Y,Z:NORTH by=BaronVonYeetus`; the marks vanish. **2:** exactly one `[Not Secure] [BaronVonYeetus] C-DIG-BEFORE`, and **no `] C-DIG-GONE` line**: the block is deepslate, and the air test is the control that reads false. **3:** no `MINING click` line (no slot is left to judge the dig), and the block breaks in about 2 seconds as unprotected deepslate does. **4:** exactly one `[Not Secure] [BaronVonYeetus] C-DIG-GONE`, and **no new `] C-DIG-BEFORE` line**. Both directions are read, so neither result can come from a command that never matches | **NOT READ (not run).** No `/rpg mining remove` and no `C-DIG-*` line; the two commands posted at 06:42:58's place line (`-112 63 10`) were not used. Ben answered Q21 ("yes"); not bound to a reading. Carried row 10 stays NOT READ |

**Q21.** Did the block break when you mined it? (yes/no) **Ben: "yes"**: the row did not run in this boot; NOT READ

### C-SECOND-PLAYER — each player sees their own patch (SURVIVAL) — witness: a second client

**Carried row 5.**

| prediction | READING |
|---|---|
| **Read only if Q5 is yes.** Placed again first (C-REMOVE-DIG removed the slot): the second player sees their own fresh patch, with their own spots in their own `MINING show` line, not Ben's progress, and Ben's sounds are not heard by them. **If Q5 is no, this row is NOT READ**, recorded as such and never as a pass | **NOT READ.** Ben, Q5: "No" second client. Carried row 5 stays NOT READ |

### LAST — a clean /stop

| prediction | READING |
|---|---|
| `/stop` (console or Ben's chat; say which). The log shows `Stopping server`, `Disabling Rpg`, and `All dimensions are saved`, with no exception between them. Afterwards `Get-Process java -ErrorAction SilentlyContinue` prints nothing | **PASS.** `/stop` from **Ben's chat** (L242). L244 `Stopping server`, L245 `Disabling Rpg v0.1.0-SNAPSHOT`, L275 `All dimensions are saved`, no exception or SEVERE between them. `Get-Process java` afterwards: 0 |

---

## NOT STAGED IN THIS BOOT, RECORDED NOT READ, NEVER A PASS

- **Carried row 6, the same-world respawn re-shows the patch**, and **carried row 11, whether a death and respawn
  drops a server-offered pack.** Both need a death, and Ben's inventory holds his Ability Stone and Nexus star, so
  no row stages one (the seat's brief). **For the seat:** `RpgListeners.onPlayerDeath` keeps inventory and drops
  nothing on every death (PLAN F-M30); whether that makes a staged death acceptable later is the seat's and Ben's
  call.
- **C-SECOND-PLAYER** is NOT READ unless Q5 is yes.
