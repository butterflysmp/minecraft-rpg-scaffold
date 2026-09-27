# GATE — Recall: the Rekindle rework, Ember Cache, and the Updraft recast

**Status: R0 READ at `e1cd21c`, 2026-09-27 03:02:28 -- R0a, R0b and R0c PASS. EVERY RC ROW, AND BA13: "reported
good or skipped -- not itemised by Ben", and NONE IS A READING OF `e1cd21c`.** Asked to itemise, Ben said *"just go.
I've run all the boot gates i intend to."*

> **THE `e1cd21c` BOOT HAD NO PLAYER ON IT, AND THAT IS WHY NO ROW IS PASS.** Its log, from the 03:02:28 boot to
> the time these readings were recorded, has **no player login, no command and no player activity**. Every RC row
> needs a player in the game. So whatever Ben ran was on the **tuning** jars (`632af16` and `3f05313`: the spike
> logger with tuning content). R0 binds every reading to `e1cd21c`, so those runs cannot be credited here. They
> are recorded as he reported them, and marked NOT a reading of this build.
>
> **The first gate launch (02:50) never booted at all.** `--refresh-content` could not delete
> `run/plugins/Rpg/content`: a misfired launch from 02:27 had left a process with its working directory inside it.
> So there was no server from 02:50:22 to 03:02:28. The process was killed, and the 03:02:28 boot is the one R0
> reads.

Every prediction below was written **before** any boot. It already reflects Ben's rulings
33-36 (2026-09-27), and each prediction they touched is marked **"edited before any reading (rulings 33-36)"**.
Ruling 37 (the shapes swap too) arrived during the TUNING boot, before any gate row was read; each prediction
it touched is marked **"edited before any reading (ruling 37)"** and lands in the tuning commit; so does ruling 39
(Recall's grounded/mid-jump distance), marked **"edited before any reading (ruling 39)"**. For 33-36,
this file is written for the first time here, so "edited" means *changed from the §7.9 text it was derived
from*, not changed after a reading. Readings go **beside** a prediction, never over it, and a prediction is not
edited once its row has been read.

**The one blank filled after the tuning boot and BEFORE any row is read:** the leap's measured baseline apex
(ruling 35), in RC9. It is a measurement, not a prediction, and the commit that fills it is the tuning commit,
which precedes the gate boot.

```
ROWS     24   R0a R0b R0c RC1 RC2 RC3 RC4a RC4b RC4c RC4d RC5a RC5b RC5c RC6a RC6b RC6c RC7 RC8 RC9 RC10 RC11 RC12 BA13 RC13
         ──
         24   = git grep -c '^### R0\|^### RC\|^### BA' <ref> -- GATE-recall.md
```

**Plan:** `PLAN-build-system.md` §7 (rulings 24-36), as built (§7.10); the tuning figures are §7.8's.

**Declared game mode: SURVIVAL, EVERY ROW.** Fall damage (RC6) is survival-only, mana is spent only in
survival, and creative changes costs (the creative-divergence register).

**Boot:** `./scripts/dev-server.sh --refresh-content` on `feat/recall`'s tip. The refresh is REQUIRED (this
branch changes `content/`). Restarts inside a row are WITHOUT the flag.

**Set-up shared by most rows:** an op player in survival as a **Fire Ranger**, the Ability Stone in the hotbar,
Recall in **Active 1** (left) and Solar Lance in Active 2 (right), flat open ground, and mobs to hit
(`/rpg spawn` or any). `/rpg mana refill` between rows as needed.

**What the shipped content says (the rows read against these):**

| thing | value | ruling |
|---|---|---|
| Recall | kinetic, 35 mana, **360-tick (18.0 s) cooldown**, a reverse-facing dash: **grounded ~5 blocks, mid-jump ~2x**, movement keys about ±1 | 24, 25, **34, 36, 39** |
| Ember Cache (fragment) | Recall throws **5 embers in an even RING around the player (0/±72/±144)**, landing **3-4 blocks** from the pre-dash position, each **8 fire in r4** | 27, 33, **37** |
| Updraft (aspect) | recast Recall at ticks **10-50** for **Updraft Leap**: free, no cooldown of its own | 28-30 |
| Updraft Leap | a leap **baseline + 2 up** (RC9), **3-4 back** at most, **3 embers IN FRONT (Rekindle's fan, 0/±40)**, each **60 fire in r3**; no fall damage on the landing | 28, 31, 32, 33, 35, **37** |

---

## R0 — THE DEPLOYED BUILD CARRIES THIS SLICE. IF ANY R0 FAILS, STOP

### R0a — the build line names the tip

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <tip>` naming `feat/recall`'s tip SHA -- not `-dirty`, not `unknown`, and **not the spike's** | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS** *(e1cd21c, the `--refresh-content` boot, 03:02:28)*: `[Rpg] Build: e1cd21c` -- the tip, not `-dirty`, not the spike |

### R0b — the jar carries Recall, and not the spike

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `RecastRule`, `RecastTracker`, `InputHold`, `SafeLanding`, `ClassPool`, `content/abilities/recall.yml`, `content/abilities/recall_updraft.yml`. ABSENT: `content/abilities/rekindle.yml`, `content/aspects/banked_embers.yml`, the tuning spike's `TuningLog`, and the control | the scan below | **PASS** *(same boot)*: PRESENT RecastRule, RecastTracker, InputHold, SafeLanding, ClassPool, `content/abilities/recall.yml`, `content/abilities/recall_updraft.yml`; ABSENT `rekindle.yml`, `banked_embers.yml`, `TuningLog`, the control |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/build/RecastRule.class',
               'io/github/butterflysmp/rpg/core/build/RecastTracker.class',
               'io/github/butterflysmp/rpg/core/build/InputHold.class',
               'io/github/butterflysmp/rpg/core/build/SafeLanding.class',
               'io/github/butterflysmp/rpg/core/build/ClassPool.class',
               'content/abilities/recall.yml',
               'content/abilities/recall_updraft.yml',
               'content/abilities/rekindle.yml',
               'content/aspects/banked_embers.yml',
               'io/github/butterflysmp/rpg/paper/adapter/TuningLog.class',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — the boot log: the content loads, nothing Recall-related refused

| prediction | instrument | READING |
|---|---|---|
| the `Loaded ...` line reads **`8 abilities`**, **`2 pools, 6 fragments, 4 aspects`** (the shipped directories at this branch; the class file `ranger.yml` merges into the cells and is not a pool of its own); **no** line says `Refusing` or `Skipping` about `recall`, `recall_updraft`, `fragment_ember_cache`, `updraft`, `ranger` or `ranger_fire`. The one known content WARN (`volley_stone`'s 27-tick cooldown) is pre-existing and not this slice's | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','Content:'` | **PASS** *(same boot)*: `Loaded 8 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 2 pools, 6 fragments, 4 aspects, ...`; 0 lines with `Refusing` or `Skipping`; the one `Content:` WARN is `volley_stone`'s, pre-existing |

---

## THE ROWS

### RC1 — Recall alone: the dash, and no ember (ruling 24)

| prediction | READING |
|---|---|
| no fragment, no aspect slotted. Recall: the reverse-facing dash and the whoosh; **no ember** leaves the caster | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC2 — Ember Cache: five embers, Rekindle's burst (rulings 27, 33)

| prediction | READING |
|---|---|
| **edited before any reading (rulings 33-36)**, **and edited before any reading (ruling 37)**: Ember Cache slotted. Recall throws exactly **5** embers in an **even ring around the pre-dash position** (0/±72/±144), each coming to rest roughly **3-4 blocks** from it. A zombie beside one takes the burst: **8, fire** | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC3 — Ember Cache inactive (§7.3; carries BA13)

| prediction | READING |
|---|---|
| server STOPPED; the build file for this player's Fire Ranger cell hand-edited **ON DISK** (quote the file before and after) so Active 1 is empty; boot. The Build screen shows the fragment *Inactive -- requires Recall equipped*; Solar Lance casts with **no ember**. Restore the file (quote it): RC2 holds again | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC4a — Updraft: the recast leaps (rulings 28-30, 33)

| prediction | READING |
|---|---|
| **edited before any reading (rulings 33-36)**, **and edited before any reading (ruling 37)**: Updraft slotted. Recall, then a clean press **11-49 ticks** later: the leap, and **3** embers thrown **in front** of the player in Rekindle's fan (**0/±40**). **No mana spent** (HUD), and no cooldown line for the recast | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC4b — a press before tick 10: no leap (the floor)

| prediction | READING |
|---|---|
| a press **before tick 10**, then nothing: **no leap**. The press shows Recall's cooldown line | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC4c — a press after tick 50: no leap (the window end)

| prediction | READING |
|---|---|
| a press **after tick 50**: Recall's cooldown line, **no leap** | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC4d — one recast per cast

| prediction | READING |
|---|---|
| Recall, recast, then a third press inside 50: **no second leap** | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC5a — a held left input does not recast

| prediction | READING |
|---|---|
| **hold left on a block** through the whole window: **no leap** | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC5b — a held right input does not recast; a fresh press does

| prediction | READING |
|---|---|
| Recall in **Active 2** (right). **Hold right in air** through the whole window: no leap. Release, pause ≥ 9 ticks, press: **the leap** | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC5c — the §7.4 residual, RECORDED rather than predicted

| prediction | READING |
|---|---|
| a left hold swept off the block for 9+ ticks and back: **recorded, not predicted** | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC6a — the leap's landing takes no fall damage (ruling 31)

| prediction | READING |
|---|---|
| the leap from flat ground: **no fall damage** and no hurt flash. HP read before and after, and equal | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC6b — the mark clears on landing

| prediction | READING |
|---|---|
| after RC6a, walk off a 6-block ledge: **normal fall damage** | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC6c — the mark clears in water

| prediction | READING |
|---|---|
| a leap into water, then a 6-block drop: normal damage on the drop | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC7 — Recall is class-wide (ruling 24)

| prediction | READING |
|---|---|
| a Fire Ranger's Active picker offers Recall; a Fire **Mage**'s does NOT | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC8 — the stale copies are named, never deleted (ruling 26)

| prediction | READING |
|---|---|
| **THE `run/` FOLDER HAS NO STALE COPY TO NAME**: measured before this file was written, `run/plugins/Rpg/content/abilities/` and `aspects/` hold only this branch's files (the 22:57 `8dc9674` boot refreshed them). So the row **stages** them: with the server stopped, write `rekindle.yml` (ability), `banked_embers.yml` (aspect) and `rekindle_cast.yml` (visual) into their `run/plugins/Rpg/content/` folders from `git show 35d2526:paper/src/main/resources/content/<dir>/<file>`, then boot **WITHOUT** `--refresh-content`. The boot log names each of the three **ONCE**, and all three files are still on disk afterwards. Banked Embers is in **no** picker | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC9 — the tuned figures (rulings 28, 33, 35, 37)

| prediction | READING |
|---|---|
| **edited before any reading (rulings 33-36)**, **and edited before any reading (ruling 37)**. From §7.8's final table, at the tuned sha: **median leap apex = BASELINE + 2**, where **BASELINE = 4.43 blocks** *(filled from the tuning boot's first measurements, before any row is read: the first four leaps at `lift 0.85`, `632af16`, each logged `apex=4.43`, so the median is 4.43 and n = 4)*, so the target is **6.43**; **pushback at most 3-4 blocks** (ruling 28's "no more than"), standing AND with S held; **Ember Cache's ring 3-4 blocks out** at the fuse; the leap's **front fan's** distance **reported, not ranged** (ruling 37 gives it Rekindle's throw as a starting point only). And by eye in this boot: the leap visibly clears roughly that height | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC10 — a saved `rekindle` loadout (§7.2)

| prediction | READING |
|---|---|
| a saved build naming `rekindle` shows that Active slot **EMPTY**, and nothing else changes | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC11 — Recall: grounded ~5 blocks, mid-jump ~2x (rulings 34, 39)

| prediction | READING |
|---|---|
| **edited before any reading (rulings 33-36)**, **and edited before any reading (ruling 39)**: flat ground, facing along an axis, no movement key held, read as the change in the F3 feet coordinate between the take-off and the point where the player comes to rest. (a) **Cast while standing on the ground: about 5 blocks back** (the tuning boot read 5.25 at rest, three times). (b) **Cast mid-jump: about twice that** (the tuning boot read 10.68), a deliberate movement trick. The difference is ground friction on the dash's first tick, accepted by ruling 39 | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC12 — Recall's cooldown is 18 s (ruling 36)

| prediction | READING |
|---|---|
| **edited before any reading (rulings 33-36)**: after a cast, a second press shows Recall's cooldown line counting from **18.0 s**; the tooltip/lore states **18.0 s** | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### BA13 — CARRIED: a build file read ON DISK

| prediction | READING |
|---|---|
| RC3 reads a build file on disk. If RC3's itemised reading quotes the file, BA13 closes there, and says so | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |

### RC13 — the leap's embers hit (ruling 32)

| prediction | READING |
|---|---|
| a zombie standing where one of the leap's 3 embers comes to rest takes the burst: **60, fire** | reported good or skipped -- not itemised by Ben. **NOT a reading of `e1cd21c`: that boot's log shows no player login** (see the Status line). Not PASS |
