# GATE — MINING S2: four vein sizes rolled per patch, the DENIED sound, the miss-sound cooldown, and the carried rows

**Status: NOT RUN.** Written 2026-11-14, before any boot of `feat/mining-s2`. **NO BOOT until the seat has diffed
S2's code AND this gate, and Ben says ready.**

**Seat read 2026-11-14 from origin at 37534ac2: readings accepted; approved to merge with the carried rows listed in
PLAN-mining.md**

**READ 2026-11-14** (the seat's boot approval of 2026-11-14 for `03bd1bf0` only, on Ben's "ready"). One boot,
`./scripts/dev-server.sh --refresh-content`, `[Rpg] Build: 03bd1bf0`; Ben's LOGIN at L80 of that boot's
`run/logs/latest.log` (07:53:03), `/stop` at L171 (08:02:57). Every `L<n>` below is a line of that log. Ben's answers
are recorded as he gave them; readings sit beside the predictions, which are unchanged.

**CARRIED STATE, AS DONE.** No JVM (0). Backup `C:\Users\Neb91\IdeaProjects\rpg-run-backups\2026-11-14-pre-mining-s2`:
`run/plugins/Rpg` 114 files / 349415 bytes, source and copy; `run/world` 193 files / 204963695 bytes, source and copy;
`diff -rq` empty for both. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` printed `12:  dev-pack-on-join: true`
(checked, not assumed). Working tree clean at `03bd1bf0`, equal to `git ls-remote origin refs/heads/feat/mining-s2`;
Paper `26.1.2.build.74-stable`, PacketEvents 2.13.0 (`run/plugins/packetevents-spigot-2.13.0.jar`).

**AN R0 READ I DISCARDED, recorded so nobody quotes it.** My first R0 read ran before the new server had started:
my wait loop matched the PREVIOUS boot's `Done` line in a `latest.log` last written at 06:52 (S1-c's, `Build:
0578366d`), and the jar scan read S1-c's jar before the new one was deployed. Both readings were of the wrong subject.
I discarded them before Ben logged in and re-read R0 on this boot's own log (mtime fresh, `Build: 03bd1bf0`) and the
jar deployed at 07:51:37. Only that second read is recorded below.

**THE SEAT'S RULINGS FOR THIS BOOT, 2026-11-14, recorded beside each affected row:** (1) S2-MEDIUM-CAP at 5
placements, not 21 (the boot approval); (2) then, before the run sheet was posted: S2-SIZE-LARGE and S2-SIZE-XL roll
and sight only, their break halves NOT RUN; S2-MEDIUM-CAP NOT RUN; one remove-and-place at medium before
S2-ROLL-REFUSED; S2-SECOND-PLAYER NOT READ unless Ben has a second client. Rows NOT RUN by ruling are NOT READ, never
passes, and do not block the merge.

**THE READING IN ONE PLACE.** R0 PASS. **PASS:** S2-PACK-JOIN, S2-PLACE-XL, S2-SIZE-SMALL, S2-SIZE-MEDIUM,
S2-SIZE-LARGE (roll and sight), S2-SIZE-XL (roll and sight), S2-DENIED-SOUND, S2-MISS-COOLDOWN (with the 8-tick
boundary read in game, L132), S2-REGROW, LAST. **PASS, NOT AS PREDICTED in one detail:** S2-OUTSIDE (the rule held
on three other outside blocks; the predicted top-row centre block was not the one clicked); S2-TRACE (on, but the
set-up's five `/gamemode`…`/weather` commands never ran, so SURVIVAL is an inference again, as S1-c's F-M35).
**NOT RUN BY THE SEAT'S RULING, NOT READ:** S2-MEDIUM-CAP, the break halves of S2-SIZE-LARGE and S2-SIZE-XL (Ben
broke the large anyway: read out of the steps, below). **NOT READ (not run: Ben stopped the server after step 13's
`remove`):** S2-ROLL-REFUSED, S2-RESPAWN, S2-PACK-AFTER-DEATH. **NOT READ:** S2-SECOND-PLAYER (Q4 "no"). Ben answered
Q21 ("not sure", NOT STATED) and Q22 ("yes") before those rows ran; they are recorded as given and bound to no reading.

**STILL CARRIED TO THE NEXT MINING GATE** (never passes): M-SECOND-PLAYER, the same-world respawn, whether a death drops
the pack (PLAN §6), and S2-ROLL-REFUSED's two refusal texts.

**What it reads.** The seat's S2 rulings 1–11 (`PLAN-mining.md` §2), as built in §5.12:
- a slot placed at a MAXIMUM size, and each patch rolling its own size, restricted to sizes that fit (rulings 1–3);
- a click on a slot block outside the current patch is judged as nothing (ruling 4);
- each size's payout inside its executed range (ruling 5) and drawn with its own art (ruling 6);
- `/rpg mining roll <size>` (ruling 7);
- the DENIED sound once per message (ruling 8);
- the miss-sound cooldown of 8 ticks, read from the click line's `miss_sound=` field (ruling 9);
- the three rows still carried (§6, *CARRIED HAND ROWS … AFTER BEN'S (a)*): the second player, the same-world
  respawn, and whether a death drops the pack.

Every prediction below was written **before** any row was read, and **this file is COMMITTED before the boot**.
Readings go **beside** a prediction, never over it. A prediction is not edited once its row has been read. Readings
count only if Ben's LOGIN appears in THIS boot's log.

```
ROWS 20   R0a R0b R0c / S2-PACK-JOIN S2-TRACE S2-PLACE-XL S2-SIZE-SMALL S2-SIZE-MEDIUM S2-SIZE-LARGE S2-SIZE-XL
          S2-OUTSIDE S2-DENIED-SOUND S2-MISS-COOLDOWN S2-REGROW S2-MEDIUM-CAP S2-ROLL-REFUSED S2-RESPAWN
          S2-PACK-AFTER-DEATH S2-SECOND-PLAYER LAST
          = 3 R0 + 16 S2 + LAST
          git grep -c -E '^### (R0|S2-|LAST)' origin/feat/mining-s2 -- docs/gates/GATE-mining-s2.md
```

**NO PLACEHOLDER IN ANY COMMAND (PLAN F-M25).** Every command below is typed exactly as written. Where a value is
known only at run time (S2-RESPAWN's way back to the wall), the row says so, and **the booter reads it from the log
and posts the exact command** before Ben needs it.

**Anything once read from chat is read from the log.** Every `/rpg mining` reply that matters has its own always-on
log line (`place`, `place-refused`, `remove`, `roll`, `roll-refused`, `regrow-all`, `trace`, `pack offer`,
`pack status`). Inventory facts use the `run say TOKEN` form with a positive control: **read the `say` output, not
the command echo**, i.e. a line of the form `[Not Secure] [BaronVonYeetus] TOKEN`.

**Ben's sight and ear answers** are numbered **Q1, Q2, ...** in the order he meets them, each **yes / no / one word /
one number**. "Not sure" is recorded as NOT STATED, never interpreted, and never re-asked.

## GAME MODE

**SURVIVAL on every row.** CREATIVE breaks a block before `BlockDamageEvent` (PLAN F2b) and removes costs; ADVENTURE
never reaches `BlockDamageEvent` (F2), and Ben ruled players are never in it. **Set it by command in the set-up**
(S1-c's set-up step 1 never ran, PLAN F-M35). A row read in any other mode is VOID. **One row stages a death**
(S2-RESPAWN); none sends Ben to another dimension.

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, taken with no server running: `run/plugins/Rpg` and `run/world` to a new folder
   `C:\Users\Neb91\IdeaProjects\rpg-run-backups\` named by the boot date and `-pre-mining-s2`, with `cp -a`; report
   each tree's file count and byte total for source and copy (`find <tree> -type f | wc -l` and the sum of
   `find <tree> -type f -printf '%s\n'`), and that `diff -rq` of each pair prints nothing.
3. **Check, do not assume, the join toggle in Ben's local file:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml`
   must print `12:  dev-pack-on-join: true` (read 2026-11-14 while writing this gate: lines 11–12 are `mining:` and
   `  dev-pack-on-join: true`). If it does not, STOP and report; do not edit it without the seat.
   `--refresh-content` clears only `run/plugins/Rpg/content`, so the file survives the boot.
4. **Boot:** `./scripts/dev-server.sh --refresh-content`.

**Also carried, not actions:**
- **Ben's saved build names `sunder`**, which master does not have. Predicted: no `[Rpg]` warning at LOGIN, as in
  slice 1 and S1-c. A warning at LOGIN is recorded, not an R0 failure. Ben should not open the build or loadout
  menu, nor use the enchanting table, anvil or grindstone.
- **Game audio on**, confirmed not by naming a sound but by Ben's later answers telling sounds apart (S2-OUTSIDE's
  Q15 and Q17 must differ, S2-DENIED-SOUND's Q18 must be yes). **If they do not, the ear rows are recorded as such,
  and the log halves stand alone.**
- **The client plays its own block-dig sound on every click on the wall** (PLAN F-M33). Every ear question below
  asks about sounds OTHER than that one.
- **A death keeps the inventory and drops nothing** (`RpgListeners.onPlayerDeath`, `event.setKeepInventory(true)`,
  PLAN F-M30), so S2-RESPAWN's `/kill` costs nothing Ben carries. The Ability Stone and the Nexus star stay.
- `run/server.properties` keeps `require-resource-pack=false`.

## THE HIT BUDGET, EVERY SIZE

Spot counts from the shipped vein-size files (*design doc, relayed*; small RULED). The first n − 1 charged clicks
IN the live spot are `HIT`, the n-th is `BROKE`. DENIED, MISS, UNCHARGED and click-outside spend nothing.

| size | n (spots) | charged hits to break | payout per break (executed, PLAN §5.12) |
|---|---|---|---|
| small | 2 or 3 | 2 or 3 | 9..17 (bonus 5) |
| medium | 4 or 5 | 4 or 5 | 14..26 (bonus 6) |
| large | 6 or 7 | 6 or 7 | 20..36 (bonus 8) |
| extra_large | 8 or 9 | 8 or 9 | 26..46 (bonus 10) |

| row | spots spent | how the patch starts |
|---|---|---|
| S2-SIZE-SMALL / -MEDIUM / -LARGE / -XL | all (each breaks) | forced by `/rpg mining roll <size>` |
| S2-OUTSIDE, S2-DENIED-SOUND, S2-MISS-COOLDOWN | **0** | forced small, by `/rpg mining roll small` |
| S2-REGROW | all (it breaks) | the same small patch |
| S2-MEDIUM-CAP | 0 | each `place` shows a fresh patch |

**THE STANDING RULE: if a patch breaks before the row that should break it, run `/rpg mining roll` for the size that
row needs, and carry on.** The break line is recorded under the row it landed in. A total of up to 24 charged hits
for the four size rows, about one per second.

## THE ORDER IN THE ONE BOOT

Carried state → R0 (before Ben logs in) → S2-PACK-JOIN (his LOGIN) → set-up → S2-TRACE → S2-PLACE-XL →
S2-SIZE-SMALL → S2-SIZE-MEDIUM → S2-SIZE-LARGE → S2-SIZE-XL → S2-OUTSIDE → S2-DENIED-SOUND → S2-MISS-COOLDOWN →
S2-REGROW → S2-MEDIUM-CAP → S2-ROLL-REFUSED → S2-RESPAWN → S2-PACK-AFTER-DEATH → S2-SECOND-PLAYER → LAST.

## Set-up (after Ben logs in and S2-PACK-JOIN's lines are read)

1. `/gamemode survival`, `/difficulty peaceful`, `/gamerule spawn_mobs false`, `/time set 6000`, `/weather clear`.
   **Each is read in the log** as an `issued server command` line.
2. Stand on flat ground facing **south**. Run **`/fill ~-3 ~ ~1 ~3 ~3 ~2 minecraft:air`**, then
   **`/fill ~-3 ~ ~3 ~3 ~3 ~3 minecraft:deepslate`**: a wall 7 wide and 4 high, three blocks south, its north face
   toward you, with the two rows in front of it cleared.
3. **`/give @s minecraft:iron_pickaxe`** and hold it for every row unless the row says otherwise.
4. Leave **one hotbar slot EMPTY** (S2-DENIED-SOUND uses an empty hand), and keep **room for about 150 raw iron**.
5. Answer Q3 and Q4.

**Q3.** How many raw iron are in your inventory now? (one number) **Ben: "0"**
**Q4.** Is a second Minecraft client available on this machine? (yes/no) **Ben: "no"**

**The set-up, as read:** `/fill ~-3 ~ ~1 ~3 ~3 ~2 minecraft:air` twice (L84, L85), `/fill ~-3 ~ ~3 ~3 ~3 ~3
minecraft:deepslate` (L87), `/give @s minecraft:iron_pickaxe` (L89). **Step 1's five commands (`/gamemode survival`,
`/difficulty`, `/gamerule`, `/time`, `/weather`) are not in the log: step 1 never ran**, as in S1-c (PLAN F-M35).

**Reading a MINING line** (formats pinned by `MiningTraceLineTest` and `ActionBarHoldsTest`; PLAN §5.12). *Traced*
lines print only while the trace is on: `click`, `click-outside`, `show`, `break`, `regrow`, `hold`, `HUD hold-end`.
These always print: `place`, `place-refused`, `remove`, `roll`, `roll-refused`, `trace`, `regrow-all`, `pack offer`,
`pack status`.
- `MINING show player=P slot=ID size=S spots=u,v;u,v... live=i/n tick=t`: centres in the PATCH's pixels.
- `MINING click player=P slot=ID outcome=O spot=i/n cooldown=c aim=u,v tick=t miss_sound=yes|no|-`.
- `MINING click-outside player=P slot=ID size=S block=x,y,z tick=t`.
- `MINING break player=P slot=ID size=S payout=raw_ironxN rolls=r1+r2+... bonus=b kept=k dropped=d regrow_at=R`.
- `MINING hold player=P reason=denied start=s until=u sound=yes|no`.
- `MINING roll player=P slot=ID size=S spots=n`; `MINING roll-refused player=P reason=TEXT` (TEXT to end of line).

**A click is IN the live spot** when `cu − 2 ≤ aim.u < cu + 3` and `cv − 2 ≤ aim.v < cv + 3`, with `(cu, cv)` the
live spot's centre from the latest `show` line.

---

## R0 — THE BUILD. READ BEFORE BEN LOGS IN. IF ANY R0 FAILS, STOP

### R0a — the build line names the S2 head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build:` followed by `feat/mining-s2`'s head as read from `git ls-remote origin refs/heads/feat/mining-s2` just before the boot, shortened. Not `-dirty`, not `unknown`, not master's `6470ee8d` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS.** L42 `[Rpg] Build: 03bd1bf0`: `feat/mining-s2`'s head from `git ls-remote` just before the boot (`03bd1bf0400cdb52fc243e62749a44d0eb206469`). Not `-dirty`, not `unknown`. Read before Ben's LOGIN (see the discarded read above) |

### R0b — the jar carries S2

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `mining/SizeRoll`, `mining/MissSoundCooldown`; paper `mining/MiningService`; `resourcepack/assets/rpg/textures/item/mining/iron_patch_extra_large.png`; `content/vein_sizes/extra_large.yml`. ABSENT: `resourcepack/assets/rpg/items/mining/iron_patch.json` (slice 1's name, renamed `iron_patch_small` in S2: present means a slice 1 jar) and the control `NoSuchClassControl` | the scan below | **PASS.** On the jar deployed at 07:51:37: all five PRESENT; `iron_patch.json` ABSENT; `NoSuchClassControl.class` ABSENT. Read before Ben's LOGIN |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/mining/SizeRoll.class',
               'io/github/butterflysmp/rpg/core/mining/MissSoundCooldown.class',
               'io/github/butterflysmp/rpg/paper/mining/MiningService.class',
               'resourcepack/assets/rpg/textures/item/mining/iron_patch_extra_large.png',
               'content/vein_sizes/extra_large.yml',
               'resourcepack/assets/rpg/items/mining/iron_patch.json',
               'io/github/butterflysmp/rpg/paper/mining/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — content loads, with four vein sizes

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 14 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes, 1 ores, 4 vein sizes`: **S1-c's READ R0c line** (`GATE-mining-s1c.md`, L45 of that boot) **with `1 vein sizes` → `4 vein sizes`**, because the only content change since that boot's build `0578366d` is the mining directories (`git diff --stat 0578366d 1cefaea9 -- paper/src/main/resources/content`: `iron.yml`, `small.yml`, and the three new size files). No `Content:` line names `ore 'iron'`; no `Skipping`, `No ores`, `No vein`, `SEVERE` or exception. The only `[Rpg]` WARN lines at enable are `volley_stone`'s known pair. **The control:** a broken `iron.yml` would load `0 ores` and print `No ores`; a size file refused would print `Skipping malformed vein size` | `Select-String -Path run\logs\latest.log -Pattern 'Loaded \d+ abilities','Content:','Skipping','No ores','No vein','SEVERE','Exception'` | **PASS.** L43 is the predicted line exactly, ending `1 ores, 4 vein sizes`. No `Content:` line names `ore 'iron'`; no `Skipping`, `No ores`, `No vein`; 0 `SEVERE` or `Exception` lines in the whole log. The only `[Rpg]` WARN lines are `volley_stone`'s pair (L44, L45), so none at LOGIN |

---

## THE ROWS

### S2-PACK-JOIN — the join toggle offers S2's pack at LOGIN (SURVIVAL) — witness: the log

**Steps.** Ben logs in. He runs no command.

| prediction | READING |
|---|---|
| In the same second as Ben's `logged in` line, with **no `/rpg mining pack` before it**: `MINING pack offer player=BaronVonYeetus url=http://127.0.0.1:` a port `/rpg-pack.zip sha1=49f2e465b2b6bbb24380425830c8572f14acd11c bytes=8227`. **The sha1 and bytes are S2's, not slice 1's `1e11f1c1…`/3225**: computed from the committed blobs at `1cefaea9` by a stand-alone copy of `MiningPack.build` whose control reproduces slice 1's figures from master `6470ee8d` (PLAN §5.12). Then `MINING pack status … status=ACCEPTED`, `DOWNLOADED`, `SUCCESSFULLY_LOADED`, in that order. **MEASUREMENT (client):** whether a prompt is shown | **PASS; the prompt is a MEASUREMENT.** L78 `MINING pack offer player=BaronVonYeetus url=http://127.0.0.1:65337/rpg-pack.zip sha1=49f2e465b2b6bbb24380425830c8572f14acd11c bytes=8227`, in the same second as the LOGIN at L80 (07:53:03), with no command before it: S2's sha1 and bytes exactly. Then ACCEPTED (L81), DOWNLOADED (L82), SUCCESSFULLY_LOADED (L83), in order. Ben, Q1 "yes" (a prompt), Q2 "Yes" |

**Q1.** At login, did the game show a prompt to download a pack? (yes/no) **Ben: "yes"**
**Q2.** Did the pack finish loading without an error? (yes/no) **Ben: "Yes"**

### S2-TRACE — the trace turns on (SURVIVAL) — witness: the log

**Steps.** After the set-up: `/rpg mining trace`

| prediction | READING |
|---|---|
| `MINING trace player=BaronVonYeetus on=true`, AFTER the set-up's five `issued server command` lines (`/gamemode survival` among them) | **The trace PASS; the set-up NOT AS PREDICTED.** L92 `MINING trace player=BaronVonYeetus on=true`. The five set-up lines it was to follow do not exist (0 matches for `issued server command: /(gamemode\|difficulty\|gamerule\|time\|weather)`). **SURVIVAL is therefore an inference, not a command reading**: every click on the slot reached `BlockDamageEvent` and logged a `MINING click` or `click-outside` line, which CREATIVE cannot (it breaks first, F2b) and ADVENTURE cannot (it never reaches the event, F2) |

### S2-PLACE-XL — a slot at the maximum, extra_large (SURVIVAL) — witness: the log + Ben's sight

**Steps.** Look at the wall's **bottom row, centre block** (straight ahead at your feet' level, three blocks south).
`/rpg mining place iron extra_large`

| prediction | READING |
|---|---|
| `MINING place slot=W:X,Y,Z:NORTH ore=iron max_size=extra_large footprint=` **28** blocks `by=BaronVonYeetus`: rows y = Y..Y+3, columns X+3 down to X−3 (the player's left is east, +X, first). Then a traced `MINING show … size=S …` with **S any of the four** and its spot count inside S's range. **If the place is refused** (`MINING place-refused … reason=Block … is not solid.`), the anchor missed the centre column: recorded, and the step repeated on the centre block | **PASS.** L94 `MINING place slot=e404d973-…:-112,63,10:NORTH ore=iron max_size=extra_large footprint=` 28 blocks, x −109 down to −115 (X+3 to X−3, east first), y 63 to 66. L95 `MINING show … size=large spots=34,20;51,25;92,35;107,11;33,25;84,3;91,27 live=1/7 tick=3832`: a drawn size, 7 spots (inside large's 6–7). No refusal. (The anchor −112,63,10 is the same block as S1-c's slot; the new wall was built over it) |

**Q5.** Is a patch drawn on the wall? (yes/no) **Ben: "Yes"**

### S2-SIZE-SMALL — a forced small patch: its size, its place on the wall, its payout (SURVIVAL) — witness: the log + Ben's sight

**Steps.** Look at the wall. `/rpg mining roll small`. Answer Q6 and Q7. Then make charged hits on the live spot
(wait 1 second before each) until the patch breaks.

| prediction | READING |
|---|---|
| `MINING roll player=BaronVonYeetus slot=` S2-PLACE-XL's id `size=small spots=` n, **n = 2 or 3**; then `MINING show … size=small spots=` n centres `live=1/n`, each centre inside `[2, 45] × [2, 29]`. Then n − 1 `HIT` and one `BROKE`, each IN its box; then `MINING break … size=small payout=raw_ironxN rolls=` n rolls each 2..4 `bonus=5 kept=N dropped=0 regrow_at=` the break tick + 1200, with **N = the rolls + 5, inside 9..17** (9..13 if n = 2, 11..17 if n = 3) | **PASS.** L97 `roll … size=small spots=2`; L98 `show … size=small spots=11,20;19,6 live=1/2`, both centres inside `[2, 45] × [2, 29]`. L99 `HIT` aim 11.86,20.16 (IN (11,20)); L100 `BROKE` aim 19.24,6.42 (IN (19,6)). L101 `break … size=small payout=raw_ironx11 rolls=3+3 bonus=5 kept=11 dropped=0 regrow_at=6367` (= 5167 + 1200); 11 = 6 + 5, inside 9..13. Ben, Q6 "Yes", Q7 "yes" |

**Q6.** Is the patch 3 blocks wide and 2 blocks tall? (yes/no) **Ben: "Yes"**
**Q7.** Is its bottom edge on the wall's bottom row, and is it centred left-to-right on the wall? (yes/no) **Ben: "yes"**

### S2-SIZE-MEDIUM — a forced medium patch (SURVIVAL) — witness: the log + Ben's sight

**Steps.** `/rpg mining roll medium`. Answer Q8 and Q9. Then charged hits until it breaks.

| prediction | READING |
|---|---|
| `MINING roll … size=medium spots=` n, **n = 4 or 5**, with **no `regrow-all` needed**: the roll ends S2-SIZE-SMALL's regrowth for that slot. `MINING show … size=medium … live=1/n`, centres inside `[2, 77] × [2, 45]`. n − 1 `HIT`, one `BROKE`; break line `size=medium … bonus=6 … dropped=0`, **N = the rolls + 6, inside 14..26** (14..22 if n = 4, 16..26 if n = 5) | **PASS.** L103 `roll … size=medium spots=5` at tick 5512, inside S2-SIZE-SMALL's regrowth (to 6367), with no `regrow-all`: the roll ended it. L104 `show … size=medium spots=54,30;5,21;41,44;9,11;30,29 live=1/5`, all inside `[2, 77] × [2, 45]`. L105–L108 four `HIT`, L109 `BROKE`, each aim IN its box (54.20,31.01; 5.47,21.59; 41.55,44.91; 9.00,10.99; 30.59,29.62). L110 `break … size=medium payout=raw_ironx22 rolls=4+3+3+3+3 bonus=6 kept=22 dropped=0 regrow_at=7163` (= 5963 + 1200); 22 = 16 + 6, inside 16..26. **The payout rule follows the rolled size: bonus 6 at an extra_large slot.** Ben, Q8 "yes", Q9 "yes" |

**Q8.** Is the patch 5 blocks wide and 3 blocks tall? (yes/no) **Ben: "yes"**
**Q9.** Is its bottom edge on the wall's bottom row, and is it centred left-to-right on the wall? (yes/no) **Ben: "yes"**

### S2-SIZE-LARGE — a forced large patch (SURVIVAL) — witness: the log + Ben's sight

**Steps.** `/rpg mining roll large`. Answer Q10 and Q11. Then charged hits until it breaks.

| prediction | READING |
|---|---|
| `MINING roll … size=large spots=` n, **n = 6 or 7**. `MINING show … size=large … live=1/n`, centres inside `[2, 109] × [2, 45]`. n − 1 `HIT`, one `BROKE`; break line `size=large … bonus=8 … dropped=0`, **N = the rolls + 8, inside 20..36** (20..32 if n = 6, 22..36 if n = 7) | **Roll and sight PASS; the break half NOT RUN by the seat's ruling of 2026-11-14 (NOT READ, never a pass), but Ben broke it anyway, read out of the steps.** L112 `roll … size=large spots=7`; L113 `show … size=large spots=43,25;23,45;23,30;108,21;28,10;48,29;95,42 live=1/7`, all inside `[2, 109] × [2, 45]`. Ben, Q10 "yes", Q11 "Yes". **Out of the steps:** L114–L119 six `HIT` (each aim IN its box), L120 one `MISS` (cooldown 0.0900, aim 47.63,29.44, outside the live spot (95,42); `miss_sound=yes`), L121 `BROKE` aim 96.08,42.28 (IN (95,42)); L122 `break … size=large payout=raw_ironx30 rolls=4+3+3+4+3+3+2 bonus=8 kept=30 dropped=0 regrow_at=8037` (= 6837 + 1200); 30 = 22 + 8, inside 22..36 |

**Q10.** Is the patch 7 blocks wide (the whole wall) and 3 blocks tall? (yes/no) **Ben: "yes"**
**Q11.** Is its bottom edge on the wall's bottom row, leaving the wall's TOP row bare? (yes/no) **Ben: "Yes"**

### S2-SIZE-XL — a forced extra_large patch (SURVIVAL) — witness: the log + Ben's sight + inventory

**Steps.** `/rpg mining roll extra_large`. Answer Q12 and Q13. Then charged hits until it breaks. Answer Q14.

| prediction | READING |
|---|---|
| `MINING roll … size=extra_large spots=` n, **n = 8 or 9**. `MINING show … size=extra_large … live=1/n`, centres inside `[2, 109] × [2, 61]`. n − 1 `HIT`, one `BROKE`; break line `size=extra_large … bonus=10 … dropped=0`, **N = the rolls + 10, inside 26..46** (26..42 if n = 8, 28..46 if n = 9). **Q14 minus Q3 equals the sum of the four break lines' N** | **Roll and sight PASS; the break half NOT RUN by the seat's ruling of 2026-11-14 (NOT READ, never a pass).** L124 `roll … size=extra_large spots=9`; L125 `show … size=extra_large spots=51,36;80,44;19,24;6,32;41,36;19,18;87,54;70,26;39,50 live=1/9`, all inside `[2, 109] × [2, 61]`. Not broken. Ben, Q12 "yes", Q13 "Yes". **Q14, checked against the breaks that happened (the seat's ruling):** 63 − Q3 0 = 63 = 11 + 22 + 30 (L101, L110, L122). Ben: "63 but I broke the Large" |

**Q12.** Does the patch cover the whole wall, 7 blocks wide and 4 blocks tall? (yes/no) **Ben: "yes"**
**Q13.** Is it drawn with art (not purple and black squares)? (yes/no) **Ben: "Yes"**
**Q14.** How many raw iron are in your inventory now? (one number) **Ben: "63 but I broke the Large"**

### S2-OUTSIDE — a click on the slot outside the current patch does nothing (SURVIVAL) — witness: the log + Ben's ear

**Steps.**
1. `/rpg mining roll small`
2. Wait 1 second. Left-click the wall's **TOP row, centre block** (outside the small patch, inside the slot). Answer
   Q15 and Q16.
3. Wait 1 second. Left-click **the small patch, away from the live spot** (the control). Answer Q17.

| prediction | READING |
|---|---|
| **1:** `MINING roll … size=small …` and its `show`. **2:** exactly one `MINING click-outside player=BaronVonYeetus slot=` the slot id `size=small block=X,Y+3,Z tick=` t (the top row's centre: Y + 3), and **NO `MINING click` line at tick t**; no sound of ours; the block does not break (the event is cancelled). **3, the control:** one `MINING click … outcome=MISS … miss_sound=yes`, and the miss sound plays. **Q15 no and Q17 yes: the ear tells the two apart, which is also this gate's audio check** | **PASS on the rule; NOT AS PREDICTED in which block.** L127 `roll … size=small spots=3`, L128 `show … size=small spots=22,26;30,9;40,18 live=1/3`: the patch covers x −111..−113, y 63..64. **The predicted block (−112,66,10, the top row's centre) was never clicked.** Eight `click-outside` lines name three OTHER slot blocks outside the patch: L143 `block=-115,65,10` (tick 8541), L144 and L146 `block=-114,64,10` (8544, 8594), L150–L154 `block=-112,65,10` five times (9588, 9616, 9621, 9640, 9649), each `size=small`. **No `MINING click` line at any of those eight ticks**, and the patch's `spot=1/3` never moved across them. **The control:** L145 `MISS … miss_sound=yes` (tick 8569, on the patch, away from the live spot). Ben, Q15 "no", Q16 "No", Q17 "yes": the ear told the two apart, so audio was on. (The steps' order was not followed: the miss-cooldown and denied clicks, L129–L141, came before these; recorded, it changes no rule) |

**Q15.** For the top-row click, did you hear anything apart from the block-digging sound? (yes/no) **Ben: "no"**
**Q16.** Did the top-row block break? (yes/no) **Ben: "No"**
**Q17.** For the click on the patch, did you hear a sound apart from the block-digging sound? (yes/no) **Ben: "yes"**

### S2-DENIED-SOUND — the DENIED sound plays once per message (SURVIVAL) — witness: the log + Ben's ear

**Steps.**
1. Select the **empty** hotbar slot.
2. Left-click the small patch's live spot **once**. Answer Q18. Wait 3 seconds.
3. Still empty-handed: left-click it **twice, about half a second apart**. Answer Q19.

| prediction | READING |
|---|---|
| **2:** `MINING click … outcome=DENIED … tick=A miss_sound=-`, then `MINING hold player=BaronVonYeetus reason=denied start=A until=A+30 sound=yes` (no hold was live), and the DENIED sound (`sounds.denied`, `minecraft:entity.villager.no`, PLACEHOLDER) plays. **3:** two DENIED lines at B1 < B2 with B2 − B1 under 30; `hold … start=B1 … sound=yes` (A's hold ended at A + 30, at least 3 s before) and `hold … start=B2 … sound=no` (B1's hold was live). **One sound for the pair.** (If B2 − B1 is 30 or more, B2's line reads `sound=yes`: recorded, and step 3 repeated faster) | **PASS (log and ear).** Five DENIED clicks, each with its hold line, `until − start = 30` every time, and **`sound=` is `yes` exactly when no hold was live**: L137/L138 start 8432 `sound=yes` (no hold before); L139/L140 start 8455 `sound=no` (8432's hold live until 8462; Δ 23); L155/L156 start **A = 10203** `sound=yes` (8455's hold ended at 8485); L158/L159 start **B1 = 10324** `sound=yes` (10203's ended at 10233); L160/L161 start **B2 = 10334** `sound=no` (B1's live until 10354; Δ 10). `HUD hold-end` lines: L142 for 8455 (tick 8492), L157 for 10203 (10242), L162 for 10334 (10372), each 7 to 9 ticks after its end; **none for the two replaced holds (8432, 10324)**. A first pair (8432/8455) came before the steps' single click; recorded. Ben, Q18 "Yes", Q19 "1": one sound for the quick pair, as the log says |

**Q18.** After the one click, did you hear a sound apart from the block-digging sound? (yes/no) **Ben: "Yes"**
**Q19.** For the two quick clicks, how many times did you hear that same sound? (one number) **Ben: "1"**

### S2-MISS-COOLDOWN — a miss inside 8 ticks of the last PLAYED miss sound is silent, read from the log (SURVIVAL) — witness: the log

**Steps.** Back to the iron pickaxe.
1. Wait 1 second. Left-click the small patch **away from the live spot five times, as fast as you can**.
2. Wait 1 second. Left-click it away from the live spot **three times, about one second apart**.

| prediction | READING |
|---|---|
| Eight `MISS` lines (step 1's later clicks are uncharged, and an uncharged click beside the spot is a MISS by the order of checks). **The rule, checked on every line:** `miss_sound=yes` exactly when its tick minus the tick of the last line that read `yes` is **at least 8**; otherwise `no`. Step 1's first line reads `yes` (S2-OUTSIDE's control was over a second earlier); its fast clicks (gaps under 8) read `no` until one lands 8 or more after the last `yes`. Step 2's three lines read `yes` (each about 20 ticks after the last). **The reading lists each line's tick, gap since the last `yes`, and field.** At least one gap under 8 and one at 8 or more must be read, or the row is repeated faster. A gap of exactly 8, if one occurs, must read `yes` (the boundary is unit-tested either way) | **PASS, with the 8-tick boundary read in game.** All **14** `MISS` lines in the boot obey the rule (tick, gap since the last `yes`, field): L120 6816 – yes; L129 8084 1268 yes; L130 8119 35 yes; L131 8128 9 yes; **L132 8136 8 yes (exactly the cooldown)**; L133 8143 **7 no**; L134 8157 21 yes; L135 8163 **6 no**; L136 8180 23 yes; L141 8485 305 yes; L145 8569 84 yes; L147 8614 45 yes; L148 8652 38 yes; L149 9128 476 yes. **Gaps under 8 (7, 6) read `no`; 8 and over read `yes`; the two silent misses did not restart the count** (L134 is 21 after the last `yes`, 14 after the silent L133). The steps' 5-fast-then-3-slow shape is not separable in the log (L129–L136 are eight lines with gaps 35, 9, 8, 7, 14, 6, 17); recorded, the rule is read on every line |

### S2-REGROW — a real regrowth rolls a size (SURVIVAL) — witness: the log + Ben's sight

**Steps.** Charged hits on the small patch's live spot until it breaks. Then **do not click the wall for 65 seconds**.
Answer Q20.

| prediction | READING |
|---|---|
| A `BROKE`, then a break line `size=small … regrow_at=` R. At tick **R exactly**: `MINING show … size=` S and `MINING regrow … size=S spots=` n `tick=R`, with **S any of the four** (the extra_large slot fits all four; the size is drawn, not forced) and n in S's range, and no `MINING click` line between the break and R | **PASS.** L163, L164 `HIT` and L165 `BROKE` (aims IN (22,26), (30,9), (40,18)); L166 `break … size=small payout=raw_ironx14 rolls=4+3+2 bonus=5 kept=14 dropped=0 regrow_at=12284` (= 11084 + 1200; 14 inside 11..17). At **tick 12284 exactly**: L167 `show … size=medium spots=66,28;37,45;17,10;68,15 live=1/4` and L168 `regrow … size=medium spots=4 tick=12284`. **A real regrowth rolled a DIFFERENT size (medium) from the patch that broke (small)**, with 4 spots inside medium's range and bounds. No click between L166 and L167. Ben, Q20 "Yes" |

**Q20.** Did a patch come back on the wall by itself? (yes/no) **Ben: "Yes"**

### S2-MEDIUM-CAP — a medium slot never shows large or extra_large (SURVIVAL) — witness: the log

**Why 21 rolls, and why remove-then-place.** The fit rule and the renormalised draw are unit-tested exhaustively
(`SizeRollTest`); this row reads the WIRING: that the paper side rolls against the slot's maximum. If it rolled
against the ore's largest size instead, each roll would show large or extra_large with chance 20/100; **21 rolls
miss that with chance (80/100)^21 = 0.0092**, under 1 in 100 (executed in jshell; a large-maximum mis-wire would be
missed with (80/95)^21 = 0.027). Under the correct wiring, all 21 rolls small has chance (50/80)^21 = 0.000052 and
all medium 1.1e-9, so both sizes are expected. **A real regrowth costs 4–5 charged hits per roll** (about 100 clicks
for 21), so each roll is staged as `remove` then `place`, which shows a fresh patch through the same
`MiningBook.patch` creation point regrowth uses (PLAN F-M38). S2-REGROW reads the regrowth path itself.

**Steps.**
1. Look at the wall's bottom row, centre block. `/rpg mining remove`
2. `/rpg mining place iron medium`
3. Repeat steps 1 and 2 until **21** `place` commands have run in this row (the up arrow recalls them).

| prediction | READING |
|---|---|
| **1 (first time):** `MINING remove slot=` S2-PLACE-XL's id. **Each `place`:** `MINING place slot=` the same id `ore=iron max_size=medium footprint=` **15** blocks (rows Y..Y+2, columns X+2..X−2), then a traced `MINING show … size=small` or `size=medium`. **21 show lines, every one small or medium, NONE large or extra_large**, both sizes among them. The reading tallies them (small : medium) | **NOT RUN by the seat's ruling of 2026-11-14; NOT READ, never a pass.** Two rulings, both recorded: the boot approval first ruled 5 placements, not 21 ("to spare Ben 32 commands; at 5 a mis-wire to the ore's largest size would go unseen with chance (80/100)^5 = 0.33, so this is a WEAK witness; the guards are SizeRollTest, the mutation that rolled against the ore's largest size, and S2-ROLL-REFUSED"); the seat's second ruling, before the run sheet was posted, set the row NOT RUN. The wiring it would have witnessed rests on `SizeRollTest`, `MiningBookTest.theBooksDrawIsBoundedByTheSlotsMaximumNotByTheOresLargestSize` and its mutation (B5) |

### S2-ROLL-REFUSED — the roll command refuses a size that does not fit, and one not loaded (SURVIVAL) — witness: the log

**Steps.** Looking at the medium slot:
1. `/rpg mining roll large`
2. `/rpg mining roll huge`

| prediction | READING |
|---|---|
| **1:** `MINING roll-refused player=BaronVonYeetus reason=Size 'large' (7 x 3) does not fit this slot, whose maximum is 'medium' (5 x 3).` and no `MINING roll` line. **2:** `MINING roll-refused player=BaronVonYeetus reason=No vein size 'huge' is loaded.` **NOT STAGEABLE with shipped content:** the third refusal, *"Ore 'iron' does not list size …"*, because iron lists all four sizes; it is unit-tested only (`MiningBookTest`), recorded NOT READ, never a pass | **NOT READ (not run).** By the seat's ruling of 2026-11-14 this row was to follow ONE `/rpg mining remove` and `/rpg mining place iron medium`. L169/L170 `MINING remove slot=e404d973-…:-112,63,10:NORTH` ran; **no `place` followed**: Ben ran `/stop` at L171, eight seconds later. No `roll-refused` line exists. Both refusal texts stay NOT READ (unit-tested in `MiningBookTest.forceRefuses…`) |

### S2-RESPAWN — a same-world respawn re-shows the patch (SURVIVAL) — witness: the log + Ben's sight

**Carried row 2** (PLAN §6, *after Ben's (a)*). A death keeps the inventory and drops nothing (F-M30).

**The booter posts ONE command before step 3**, built from S2-MEDIUM-CAP's last `MINING place` line's slot id
`W:X,Y,Z:NORTH`: **`/tp @s` with X + 0.5, Y, Z − 2.5, then `0 0`** (three blocks north of the anchor, facing south),
written out with the numbers filled in. Ben types it exactly as posted.

**Steps.**
1. `/execute if items entity @s container.* minecraft:iron_pickaxe run say S2-PICK-BEFORE`
2. `/kill @s`, and press Respawn.
3. Run the booter's posted `/tp` command.
4. `/execute if items entity @s container.* minecraft:iron_pickaxe run say S2-PICK-AFTER`
5. `/execute if items entity @s container.* minecraft:barrier run say S2-BARRIER`
6. Look at the wall **without clicking it**. Answer Q21.

| prediction | READING |
|---|---|
| **1, the control:** exactly one `[Not Secure] [BaronVonYeetus] S2-PICK-BEFORE` (the instrument finds the pickaxe when it is there; if it prints nothing, 4 proves nothing). **2:** a death message for BaronVonYeetus. **After the respawn** (PLAN F8a/F8b: the old chunks unload and are sent again): a traced `MINING show` for the slot with **the same `size=`, the same `spots=` and the same `live=`** as the last `show` before the death, and **no `MINING click` line between the death and it**. **4:** exactly one `… S2-PICK-AFTER` (kept). **5:** **no** `] S2-BARRIER` line (the negative control). **The reading records whether the show came at the respawn or after the `/tp`** (it depends on the respawn point's distance) | **NOT READ (not run).** The medium place line it was to be built from never appeared, so no `/tp` was posted, and no `S2-PICK-*` line, death message or `/kill` exists in the log. Carried row 2 stays NOT READ. Ben answered Q21 before the row could run: "not sure", NOT STATED, bound to no reading |

**Q21.** Is the patch back on the wall? (yes/no) **Ben: "not sure"**: NOT STATED; the row did not run in this boot

### S2-PACK-AFTER-DEATH — whether a death and respawn drops the server-offered pack (SURVIVAL) — witness: Ben's sight + the log

**Carried row 3.** Read in the same look as S2-RESPAWN's step 6.

| prediction | READING |
|---|---|
| **A MEASUREMENT, predicted KEPT:** a respawn is not a reconnect, and the pack was offered once, at LOGIN. **Log:** no `MINING pack offer` and no `MINING pack status` line between the death and Q22's answer (any that appears is recorded with its status). **Sight:** the patch drawn with its art | **NOT READ (not run).** No death in this boot. Ben answered Q22 "yes" before the row could run; recorded as given and **not bound to a reading** (a patch drawn with art before any death says nothing about after one). Carried row 3 stays NOT READ |

**Q22.** Is the patch drawn with its art, NOT purple and black squares? (yes/no) **Ben: "yes"**: the row did not run in this boot; NOT READ

### S2-SECOND-PLAYER — each player sees their own patch (SURVIVAL) — witness: a second client

**Carried row 1.**

| prediction | READING |
|---|---|
| **Read only if Q4 is yes.** The second player sees their own fresh patch, with their own `size=` and spots in their own `MINING show` line, not Ben's progress, and Ben's sounds are not heard by them. **If Q4 is no, this row is NOT READ**, recorded as such and never as a pass | **NOT READ.** Ben, Q4 "no" second client; and by the seat's ruling of 2026-11-14, NOT READ unless Ben has one. Carried row 1 stays NOT READ |

### LAST — a clean /stop

| prediction | READING |
|---|---|
| `/stop` (console or Ben's chat; say which). The log shows `Stopping server`, `Disabling Rpg`, and `All dimensions are saved`, with no exception between them. Afterwards `Get-Process java -ErrorAction SilentlyContinue` prints nothing | **PASS.** `/stop` from **Ben's chat** (L171, 08:02:57), right after step 13's `remove`. L173 `Stopping server`, L174 `Disabling Rpg v0.1.0-SNAPSHOT`, L204 `All dimensions are saved`, no exception, error or SEVERE between them. `Get-Process java` afterwards: 0 |

---

## NOT STAGED IN THIS BOOT, RECORDED NOT READ, NEVER A PASS

- **The roll refusal for a size the ore does not list** (S2-ROLL-REFUSED): shipped iron lists all four sizes.
- **The miss-sound cooldown at a gap of exactly 8 ticks**, unless one occurs by hand (S2-MISS-COOLDOWN).
- **S2-SECOND-PLAYER** is NOT READ unless Q4 is yes.
- **Closed by Ben's ruling 2026-11-14, NOT READ, never passes** (PLAN §2): the place refusal texts, the server-list
  check, the full-inventory ground check, the relog and logout sights, the post-remove dig. No row here re-runs them.
