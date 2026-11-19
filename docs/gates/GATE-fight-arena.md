# GATE — STEP F8b: THE FIGHT'S OWN WORLD AND ITS GENERATED ARENA (AND THE ONE BOOT FOR F8a's GATE TOO)

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/fight-arena`, and this file is
committed before the boot. Readings go **beside** a prediction, never over it; a prediction is not edited once its row
has been read; a correction is a dated note. **NO BOOT until the seat has diffed both branches' code and both gates
from origin and Ben says "ready" in his own words. Then only CC boots.**

**ONE BOOT, TWO GATES (the seat's ruling on the split, 2026-11-18).**
- This file carries **the R0 and the run sheet** for its own rows (`/rpg selftest fight-arena`) AND for
  `GATE-fight-site.md`'s rows (`/rpg selftest fight-site`, F8a).
- Both gates are read on the one boot of `feat/fight-arena`'s head. `GATE-fight-site.md` carries a dated SUPERSEDED note
  pointing here.

**What it gates.** PLAN-dragon-fight.md §6.4, step F8b: the F8 + F9 brief's §4 and §5 i, j, with no dragon.
- The fight's own temporary world and the generated stand-in arena.
- Enter and leave; the return location; death, quit and the stale login.
- Every ending; the orphan sweep (R0e).
- Failure made real inside the arena.
- The border lightning from the first tick.
- Ring, Mage and crystals run in the arena.

## WHY DELETING A FIGHT WORLD IS SAFE (the seat's condition, READ before any delete code was written)

From the pinned server jar (`run/versions/26.1.2/paper-26.1.2.jar`, build 74), by `javap`:

| question | answer | mark |
|---|---|---|
| Where does an API world live? | `CraftServer.createWorld` keys it as a dimension of the main save (`PaperWorldLoader.dimensionKey`); its folder is `DimensionType.getStorageFolder` = `<level>/dimensions/<namespace>/<path>`; `CraftWorld.getWorldFolder` returns that folder | READ (namespace-then-path order INFERRED from `dimensions/minecraft/overworld` on disk) |
| Does the main save record it outside its folder? | No. `createWorld` writes its `WorldGenSettings` into a `SavedDataStorage` at `getDimensionPath(key)/data`; `loadWorldData` reads its metadata, PDC and `PaperLevelOverrides` from there; `PaperLevelOverrides.attach` sets only two `transient` fields. The one record outside: a player saved while inside names the dimension in his playerdata. That is the brief's stale login, caught by `onJoinSettled` | READ; nothing in `createWorld` registers a stem: INFERRED from its call list |
| What does the next boot do with the folder gone? | `PaperWorldLoader.loadInitialWorlds` loads the `LEVEL_STEM` registry's worlds only; a fight world is never in it, so nothing tries to load it | READ (the loop); INFERRED (that it is never in it) |
| Does `unloadWorld` release the region files? | Yes. `CraftServer.unloadWorld` refuses while players are in it, else `ServerChunkCache.close(save)` → `ChunkHolderManager.close`: halts, saves, flushes IO, halts IO, then `RegionFileStorage.close()` for chunks, entities and POI, synchronously | READ |

**The four refusals** (`FightFolders`, core; `FightFoldersTest`): not directly in `dimensions/rpgfight/`; no `fight_`
prefix; no `.rpg-fight-world` marker FILE; real path not strictly one level inside the namespace's real path, or under
`dimensions/minecraft`. The symlink arm of the fourth is **skipped on this machine** (this Windows account cannot make a
symbolic link); the fourth is exercised by the `dimensions/minecraft` arm and the path-only containment test.

## CC'S CHOICES, FLAGGED FOR BEN (not in the brief)

- **KEEP_INVENTORY true and IMMEDIATE_RESPAWN true in a fight world.**
  - What a death there drops would be deleted with the world, so nothing is dropped.
  - A death sends him straight back to where he entered, with no death screen in a world that may be unloading.
- **The entry spot:** the arena's centre (`entry_offset_blocks: [0.0, 0.0]`).
- **The namespace (`rpgfight`), prefix (`fight_`) and marker (`.rpg-fight-world`) are Java constants, not content:** a
  YAML typo must never point a delete elsewhere.

## THE NUMBERS (executed: `ArenaShapeTest`)

- The arena: centre (0, 0), the floor's top at y 64 (feet at 65).
- The disc: radius 56, 3 thick (y 62 to 64). **9845 columns, so 29535 end stone.**
- Ten 3×3 obsidian pillars, tops at y 76, so **1080 obsidian. Total 30615 blocks.**
- Pillar centres, block x, z: `40,0` `32,24` `12,38` `-12,38` `-32,24` `-40,0` `-32,-24` `-12,-38` `12,-38` `32,-24`.
- Crystal spot n: the pillar's centre + 0.5, y **77**.
- C for the ring: (0.5, 65, 0.5).

## GAME MODE, DIFFICULTY

**SURVIVAL on every row, both gates.** `/rpg selftest` refuses outside SURVIVAL; the arena's participants are survival
or adventure entrants. The difficulty is set by each gate's `difficulty: normal`.

## THE ROWS THAT KILL HIM ON PURPOSE, AND THE ARITHMETIC FOR THE REST

- **On purpose: AE4** (the closed ring's lightning), **AE5** (POWERED), **AE6** (the void past the disc's edge). In the
  play row **BA3** he may die by his own play. Each is a `death_rules` row, and each death happens inside an
  `await_fight`, which survives a death. **He reappears where he entered.**
- **Every other row cannot kill him:**
  - AE1 and AE7: no landing, inside the border.
  - AE2: 1 from the Mage, inside every ring.
  - AE3: one strike, k = 1, 20 before armour, on a full bar.
  - BA1 and BA2: inside 48, no landing.
  - `GATE-fight-site.md`'s rows: its own arithmetic.

## WHAT ENDS EACH ROW, AND HOW "NO FIGHT WORLD AND NO FIGHT FOLDER" IS READ

Every row ends:
- back in the world he started in;
- with **`FIGHTWORLDS loaded=0 folders=1 marked=0 you=world`**: no fight world loaded; the one folder is R0e's
  look-alike, which has no marker and is left alone all run;
- with the end probes `END_CRYSTAL count=0` and `WITHER_SKELETON count=0`.

After the run, the booter lists the namespace folder (CARRIED STATE, step 9).

## CARRIED STATE, DONE BY THE BOOTER (CC) BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, no server running, ONE bash invocation from the repo root (add `-2` if the folder exists):

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-fight-arena"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type d | wc -l) dirs"; done; diff -rq run/plugins/Rpg "$B/Rpg" | wc -l | sed 's/^/diff -rq lines Rpg: /'; diff -rq run/world "$B/world" | wc -l | sed 's/^/diff -rq lines world: /'; ls "$B/world/dimensions"; echo "overworld files: $(find "$B/world/dimensions/minecraft/overworld" -type f | wc -l)"
   ```

   **Report** the folder, both pairs of counts, `diff -rq lines: 0` for each, **the `ls` of the backup's
   `world/dimensions` (it must list `minecraft`), and the overworld's file count**: the "before" for step 9.
   - *This file's own listing, 2026-11-18, of the previous backup:* `ls .../2026-11-18-pre-fight-mage/world/` printed
     `data datapacks dimensions level.dat level.dat_old players session.lock`, so `cp -a run/world` carries
     `dimensions`.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **R0e's two folders**, created AFTER the backup and before the boot, in ONE bash invocation from the repo root:

   ```bash
   N=run/world/dimensions/rpgfight; mkdir -p "$N/fight_r0e_orphan" "$N/fight_r0e_lookalike" && printf 'rpg fight world r0e\n' > "$N/fight_r0e_orphan/.rpg-fight-world" && printf 'x\n' > "$N/fight_r0e_orphan/region.mca" && printf 'x\n' > "$N/fight_r0e_lookalike/region.mca" && find "$N" | sort
   ```

   It prints the namespace folder, the two folders, and their three files.
5. Announce "booting now"; **boot this branch's head with `--refresh-content`:** `./scripts/dev-server.sh --refresh-content`.
   **REQUIRED:** the deployed `run/plugins/Rpg/content/fights/dragon.yml` reads `  alternate_turns: true` and
   `  floor_y: 64`, and `content/mobs/dark_mage.yml` reads `max_health: 6000`.
6. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` names this branch's head.
7. **Tell Ben R0 passed before he logs in.**
8. *(After the run.)* The R0e look-alike is removed: `rm -r run/world/dimensions/rpgfight/fight_r0e_lookalike`.
9. *(After the run, before the removal in 8.)* **The namespace folder holds no fight folder, and the overworld is
   untouched:** `find run/world/dimensions/rpgfight -mindepth 1 -maxdepth 1` prints only `.../fight_r0e_lookalike`;
   `find run/world/dimensions/minecraft/overworld -type f | wc -l` against step 2's count. The run writes to the
   overworld, so a difference is READ, not predicted; report both numbers.

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **Wait for my word that R0 passed before you log in.**
1. `/gamemode survival` — **alone, the first line after login.**
2. **Before line 3:**
   - Stand on flat open ground with about 40 clear blocks to your east and room all round.
   - Hold the weapon you want to fight with.
3. `/rpg selftest fight-site` — **ONCE.** About **twelve minutes** of rows (two of them hold the ring open with long
   kill trains).
   - Stand still and touch nothing **except when gold text tells you to**. Answer the gold question in chat.
   - There is **one** question.
   - Lightning will strike you in some rows; you are healed so it cannot kill you.
   - Wait for the chat to say the run finished.
4. `/rpg selftest fight-arena` — **ONCE.** About **ten minutes**, plus your answers and up to three minutes of
   fighting.
   - You will be taken into the fight's own world, a stone disc with ten pillars in the dark, and brought back after
     every row.
   - **THREE ROWS KILL YOU ON PURPOSE (AE4, AE5, AE6), AND THE LAST ROW (BA3) CAN, BECAUSE IT HAS NO SAFETY: you
     reappear at once where you entered, with everything you carried.**
   - There are **five** questions (2 to 6); each says whether it wants yes or no, or your own words.
   - Wait for the chat to say the run finished.
5. `/rpg fight enter` — then, **when you are in the dark arena, wait five seconds and DISCONNECT** (quit to the title
   screen).
6. **Wait ten seconds, then reconnect.** You should be put back where you typed line 5.
7. `/stop`

Nothing else this boot.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/fight-arena` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries both steps

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/fights/dragon.yml` lines `  alternate_turns: true` and `  floor_y: 64`; `content/mobs/dark_mage.yml` line `max_health: 6000`; `FightWorlds.class` carrying `FIGHTWORLD orphan swept`; `FightFolders.class` carrying `ESCAPES`; `FightInstance.class` carrying `FIGHT powered-kill`; `SelfTestAwaitFight.class` carrying `AWAIT fight ended`; `GATE-fight-arena.yml` carrying `AE5:`; `GATE-fight-site.yml` carrying `MG8R2:`. ABSENT: `  crystal_interval_ticks: 80` (the YAML control), `NoSuchTextControl` in `FightWorlds.class` (the class control), the entry `selftest/GATE-fight-mage.yml` (retired) | the PowerShell below, from the repo root | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Line($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
$y = Bytes 'content/fights/dragon.yml'
Line $y '  alternate_turns: true' 'dragon.yml'; Line $y '  floor_y: 64' 'dragon.yml'; Line $y '  crystal_interval_ticks: 80' 'dragon.yml'
Line (Bytes 'content/mobs/dark_mage.yml') 'max_health: 6000' 'dark_mage.yml'
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
Has 'io/github/butterflysmp/rpg/paper/fight/FightWorlds.class' 'FIGHTWORLD orphan swept'
Has 'io/github/butterflysmp/rpg/paper/fight/FightWorlds.class' 'NoSuchTextControl'
Has 'io/github/butterflysmp/rpg/core/fight/FightFolders.class' 'ESCAPES'
Has 'io/github/butterflysmp/rpg/paper/fight/FightInstance.class' 'FIGHT powered-kill'
Has 'io/github/butterflysmp/rpg/paper/selftest/SelfTestAwaitFight.class' 'AWAIT fight ended'
foreach ($p in @(@('selftest/GATE-fight-arena.yml', 'AE5'), @('selftest/GATE-fight-site.yml', 'MG8R2'))) { $s = Bytes $p[0]; if ($s -ne $null -and $s -match "(?m)^$($p[1]):\s*$") { "PRESENT $($p[0]) $($p[1]):" } else { "ABSENT  $($p[0]) $($p[1]):" } }
if ($zip.GetEntry('selftest/GATE-fight-mage.yml') -ne $null) { "PRESENT entry selftest/GATE-fight-mage.yml" } else { "ABSENT  entry selftest/GATE-fight-mage.yml" }
$zip.Dispose()
```

**A jar from master reads ABSENT on every PRESENT line, and PRESENT on `  crystal_interval_ticks: 80` and on the entry
`selftest/GATE-fight-mage.yml`.**

### R0c — the Loaded lines, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 6 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`** (as `GATE-fight-mage.md` R0c read it), AND exactly one **`[Rpg] Loaded fight 'dragon': border 48.0, mage ring 12.0, v_fast 3.80, v_slow 0.60, e_kill 1.00, 10 crystals every 240 ticks, a strike every 40 ticks from 20.0; ring dust (170,0,255) size 1.60 every 1.00 blocks at [0.2, 1.7, 3.2, 4.7, 6.2, 7.7], row shift 0.25, turn 1.00/s alternating, every 4 ticks; mage 'dark_mage' refused item.shield.block; crystals at 40.0, 12.0 up, lit block.beacon.power_select; participants within 128.0; arena at (0, 0) floor y 64, disc 56 x 3, 10 pillars 3 wide on 40.0 to +12, time 18000, entry (0.0, 0.0)`**. **No `Refusing`, `Skipping`, `SEVERE` or `Exception`; no `[Rpg]` WARN beyond `volley_stone`'s known pair** (`.claude/rules/verification.md`, R0c's standing clause); no `No fights loaded` | `Select-String run\logs\latest.log -Pattern 'Loaded\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded'` | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 200 scenarios across 23 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive, fight-site, fight-arena]`**, no refusal after it | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (2026-11-18, this branch's tree):
- `grep -cE '^  - ' index.yml` prints **23**.
- The key count (`^[A-Za-z0-9-]+:` over the 23 files) prints **223**.
- `^setup:` prints **23**.
- So 223 − 23 = **200**.
- **Control:** fight-site's 212 keys + fight-arena's 11 = 223.

### R0e — THE ORPHAN SWEEP, on this boot (carried state step 4's two folders)

| prediction | instrument | READING |
|---|---|---|
| In this order, during the enable: (1) **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=2`**; (2) **`FIGHTWORLD deleting path=<abs>\fight_r0e_orphan`**, then **`FIGHTWORLD orphan swept path=<abs>\fight_r0e_orphan entries=2`** (its marker and its file); (3) **`FIGHTWORLD orphan refused path=<abs>\fight_r0e_lookalike reason=NO_MARKER`**. (4) After the boot, `Test-Path run\world\dimensions\rpgfight\fight_r0e_lookalike` is **True** and `...\fight_r0e_orphan` is **False** | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'`, then the two `Test-Path` | |

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST fight-arena|\[Rpg\] (RINGTICK|FIGHT|STRIKE|MAGEGATE|CRYSTAL|AWAIT|HURT)'`
inside the row's `START … DONE` bracket. `FIGHT` also matches FIGHTWORLD, FIGHTWORLDS, FIGHTPHASE, FIGHTKILL and
FIGHTPROBE. **A log-line row passes on the VERBATIM line** (`<…>` as the row says).

**Every row, not repeated:**
- **The clean:** `LEFTOVER living=0 radius=16`, `LINGER work=0 entities=0 radius=48`.
- **The world's birth:** `FIGHTWORLD created name=<n> key=rpgfight:fight_<8 hex> folder=<abs>\run\world\dimensions\rpgfight\fight_<8 hex> ms=<t>`
  (**t, the world-creation cost, READ and reported**);
- `FIGHTWORLD arena built name=<n> blocks=30615 end_stone=29535 obsidian=1080 ms=<t>`;
- `FIGHT start world=<n> centre=0.5 65.0 0.5 by=BaronVonYeetus site=arena`;
- `FIGHTWORLD entered player=BaronVonYeetus world=<n> return=world <x y z>`.
- **The end:**
  - `FIGHTWORLD returned player=BaronVonYeetus to=world <the same x y z> why=<left|died>`;
  - `FIGHT nobody inside t=<t> outcome=<o>` and `FIGHT ended reason=nobody-inside-<o>`;
  - `FIGHTWORLD unloaded name=<n>` (after any number of `FIGHTWORLD unload-retry name=<n> players=1 attempt=<a>` while
    his teleport completes);
  - `FIGHTWORLD deleting path=<abs>`, then `FIGHTWORLD deleted path=<the same> entries=<e>`;
  - **`FIGHTWORLDS loaded=0 folders=1 marked=0 you=world`**;
  - the end probes **0**.
- **RINGTICK:** `viewers=1`, `points=` 6 × ceil(2π r) (**1812 at r=48.000**).

### AE1 — enter: the world, the arena's blocks, he is inside; leave: home, the fight ends, the folder goes (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The birth lines above; `FIGHTWORLDS loaded=1 folders=2 marked=1 you=<n>`. (2) **`BLOCKS END_STONE count=1`** at `0 -1 0`, `0 -3 0`, `56 -1 0` and `0 -1 -56`; **`BLOCKS AIR count=1`** at `0 -4 0`, `57 -1 0`, `0 -1 -57` and `40 12 0`; **`BLOCKS OBSIDIAN count=108`** in `39 0 -1 41 11 1` (pillar 1 whole); **`BLOCKS OBSIDIAN count=1`** at each of the ten tops `<px> 11 <pz>`. (3) The second `enter` replies `"You are already in a fight world."`. (4) `FIGHTWORLD left player=BaronVonYeetus world=<n> why=leave`, then the end lines, `outcome=RUNNING` | `ArenaShapeTest`, `FightArenaSourceTest` | |

### AE2 — the ring, the Mage at 6000, and a crystal ON ITS PILLAR in the arena (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=L dx=6.000 dz=0.000 result=STARTED`, L 18 to 23; `FIGHT mage spawned t=L id=<uuid> at=6.00 0.00 0.00`. (2) `FIGHTPROBE … health=6000.000/6000.000 … crystals=0 …`. (3) `FIGHTPHASE t=L+222 FAST_CLOSE->SLOW_CLOSE`; **`CRYSTAL t=L+462 lit index=1`** and **`CRYSTALSPOT index=1 at=40.50 77.00 0.50 world=<n>`** (on pillar 1's top). (4) The second probe: **`crystals=1 beamsOnMage=1`**. (5) Every STRIKE inside. (6) The end lines, `outcome=RUNNING` | `ArenaShapeTest` *crystalSpotN…* | |

### AE3 — the border lightning from the fight's FIRST tick (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| No landing. (1) **`STRIKE t=40 player=BaronVonYeetus k=1 damage=20.000 outside`** and `HURT before=20.000 after=<20 × 100 / (100 + D)> defense=<D> …` (D 10 gives 18.182). (2) The strike at t=80: `k=0 damage=0.000 inside`. **The flag: cfde822 waited for the first crossing; this strikes from the first tick (SEAT PLACEHOLDER)**. (3) The end lines, `outcome=RUNNING` | `RingFixtureTest`, `StrikeRamp` | |

### AE4 — CLOSED in the arena: the lightning kills him; home; the fight fails and ends (SURVIVAL; DIES ON PURPOSE)

| prediction | stands on | READING |
|---|---|---|
| (1) `DEATH_RULES …`. L 18 to 23; S = L + 190; `CRYSTAL t=S+240 lit index=1`. (2) **`FIGHTPHASE t=L+590 SLOW_CLOSE->CLOSED`**, `CRYSTAL t=L+590 removed index=1`, `FIGHT FAILED_CLOSED t=L+590`, and the chat **`"The fight has failed: FAILED_CLOSED."`** (no "(fixture: nobody is killed)" here). (3) From the next strike tick, STRIKEs `outside` k = 1, 2, 3, … at 20, 40, 80, 160, 320 before armour, each with one HURT, **until one takes his health to 0** (at a 350 maximum and defence 10: k = 5, or 6 if regeneration covers the gap). (4) **`FIGHTWORLD died-inside player=BaronVonYeetus world=<n>`**, `RESPAWNED ticks=<r>`, **`FIGHTWORLD returned … why=died`**, `FIGHT nobody inside t=<t> outcome=FAILED_CLOSED`, the end lines. (5) **`AWAIT fight ended ticks=<n>`** | `RingFixtureTest` *theFailureMessage…*, `FightArenaSourceTest` | |

### AE5 — POWERED in the arena kills every participant and ends the fight (SURVIVAL; DIES ON PURPOSE)

| prediction | stands on | READING |
|---|---|---|
| (1) `DEATH_RULES …`; he stands 2 west of M = C. S = L + 190. (2) **68 `FIGHTKILL … counted=true`**, each `r_after` = `r_before` + 1.000, `r_before` ≤ 11.000 (kill n finds 11 − 0.05n). (3) `CRYSTAL t=S+240n lit index=n` with `CRYSTALSPOT index=n at=<pillar n + 0.5> 77.00 <…>` for n = 1 to 10. (4) **`FIGHTPHASE t=S+2400 SLOW_CLOSE->POWERED`**, `FIGHT FAILED_POWERED t=S+2400`, the chat `"The fight has failed: FAILED_POWERED."`, and **`FIGHT powered-kill t=S+2400 player=BaronVonYeetus amount=<10 × his maximum>`**, exactly one. (5) His death: `HURT before=<that amount> after=<the same> …` (defence BYPASSED), **`FIGHTWORLD died-inside`**, `RESPAWNED`, **`FIGHTWORLD returned … why=died`**, `FIGHT nobody inside t=<t> outcome=FAILED_POWERED`, the end lines. (6) `AWAIT fight ended ticks=<n>` | `MageCycleTest`, `FightArenaSourceTest` | |

### AE6 — falling off the disc is a death like any other (SURVIVAL; DIES ON PURPOSE)

| prediction | stands on | READING |
|---|---|---|
| (1) `DEATH_RULES …`; he is put 60 east of C at y −130, in the void past the disc's edge. (2) **`FIGHTWORLD died-inside player=BaronVonYeetus world=<n>`**, `RESPAWNED`, **`FIGHTWORLD returned … why=died`**, `FIGHT nobody inside t=<t> outcome=RUNNING`, the end lines. (3) `AWAIT fight ended ticks=<n>` | `FightArenaSourceTest` | |

### AE7 — entering while inside is refused and changes nothing (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The birth lines once; the second `enter` REPLY **`"You are already in a fight world."`**, and no second `FIGHTWORLD created`. (2) `FIGHTWORLDS loaded=1 folders=2 marked=1 you=<n>`. (3) Leave and the end lines | `FightArenaSourceTest` | |

### AQ1 — QUIT INSIDE, THEN LOG BACK IN: THE STALE LOGIN (Ben's run-sheet lines 5 and 6, outside the selftest)

| prediction | stands on | READING |
|---|---|---|
| (1) The birth lines; `FIGHTWORLD entered … return=world <x y z>`. (2) On the disconnect: **`FIGHTWORLD left player=BaronVonYeetus world=<n> why=quit return=kept`**, then `FIGHT nobody inside … outcome=RUNNING`, `FIGHT ended …`, `FIGHTWORLD unloaded name=<n>`, `deleting`, `deleted`. (3) On the reconnect, once his profile settles: **`FIGHTWORLD stale login caught player=BaronVonYeetus at=<world> <x y z> to=world <the saved x y z>`** and **`FIGHTWORLD returned … why=login`**. **The `at=` IS THE READING of where vanilla put a player whose saved world is gone** (the brief says the same x, y, z of the overworld; UNVERIFIED until this line) | `FightArenaSourceTest` *aStaleLogin…* | |

---

## BEN'S ROWS — HIS WORDS, VERBATIM; "not sure" IS NOT STATED; ONE THING PER QUESTION; NO PASS OR FAIL

### BA1 — the arena stand-in and the night sky (SURVIVAL)

| prediction | READING |
|---|---|
| ASK 2 (`words`) "The arena stand-in, its floor size and its pillars: is there anything that must change before waves are built?" and ASK 3 (`yesno`) "The night sky: is it fine as a stand-in?" answered. Auto: the birth lines; RINGTICKs `phase=IDLE r=48.000 points=1812`; leave and the end lines | |

### BA2 — the six-row wall, alternate rows turning opposite ways (SURVIVAL)

| prediction | READING |
|---|---|
| ASK 4 (`yesno`) "The six-row wall, alternate rows turning opposite ways: is it right?" answered. Auto: as BA1 | |

### BA3 — THE PLAY ROW WITHOUT A DRAGON: NO SAFETY (SURVIVAL; he may die)

| prediction | READING |
|---|---|
| (1) `FIGHT land …` at a random spot at least 15 from C; **`FIGHT play on t=<t> unsafe`** (no heal, no strike limit). (2) It ends ONE of these ways, and the log says which: **the Mage's death** (`FIGHT die … ticks_in_slow_close=<the time to kill>` then `FIGHT play ended … reason=mage-died`), **his death** (`FIGHTWORLD died-inside`, then `FIGHT nobody inside … outcome=<o>`), a **failure** (FAILED_CLOSED or FAILED_POWERED, then his death by it), or the **three-minute ceiling** (`AWAIT fight ceiling ticks=3600 still running`, then `rpg fight stop`). Reported: the Mage's time to kill, every STRIKE and HURT, every death. (3) ASK 5 (`yesno`) "For one player, is the Mage's 6,000 health right?" and ASK 6 (`yesno`) "Twelve seconds a crystal: is that right?" answered | |

---

## WHAT THIS GATE DOES NOT READ

- **The template-file arena (C3):** not built (no file exists to test it). A deviation from C3, owed when Ben builds an
  arena.
- **Two fights at once:** possible in the code, not staged (one player).
- **Folia:** the end's teleports and unload run on the global scheduler; on Paper that is the main thread.
- **The dragon:** F9.
