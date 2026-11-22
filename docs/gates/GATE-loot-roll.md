# GATE — THE DEV ROLL COMMAND (PLAN-loot.md, THE WIRING ORDER'S FIRST STEP; THE SEAT'S QUEUE, ITEM 6), AND THE STACK'S ONE BOOT

**Seat approval 2026-11-20: code and gate diffed at a342e77f, booted at 5c2dee2c, readings at ecab8229. Merge on Ben's words.** **Status: READ, 2026-11-20, at `5c2dee2c`; every row on the sheet; TB1-TB6 carried** (was: NOT RUN). Written 2026-11-19 on `feat/loot-cascade`, before any boot of it. **This file carries the ONE R0 and
the ONE run sheet for the whole stack** (the seat's part 2, 2026-11-19): `feat/fight-template` (#218) on master
`cf3547e3`, and this branch (#219) rebased onto it. #219's hold is lifted: it rides the stack and merges after #218. The
boot is on this branch's head. **NO BOOT until the seat has diffed the stack and this gate from origin and Ben says
"ready"**; then only CC boots, by `.claude/rules/booting.md`.

The rows it boots, each predicted in its own gate:
- **FDM1** and **FD2R2** — `GATE-fight-down.md`;
- **MG5R3** and **MG7R3** — `GATE-fight-site.md`, unchanged;
- **TA1** — `GATE-fight-template.md`;
- **LR1-LR5** — this file, below.

**Game mode:** SURVIVAL for every row. **Difficulty:** normal (every selftest setup sets it). **FDM1 and FD2R2 put him
down on purpose; nothing else can hurt him** (the arithmetic below).

**Ben's rulings it stands on** (PLAN-loot.md, its head, verbatim):
- "First the rarest rarity is rolled. then if that doesn't land, the next rarest rarity is rolled and so on";
- "If a rarity lands but that source has no item general skip that rarity";
- "Yes equally likely across every item";
- the wiring order, "That's fine".

And for FD2R2, the void (PLAN-world-damage.md, verbatim): "A player should lose 10% of their health each damage tick
(0.5s) to the void regardless of their max health."

## WHAT IT IS

- **`/rpg loot roll <source|default> [count]`:** dev only (`Permissions.DEV` AND the dev-server marker, as
  `/rpg mining` is). It rolls that table `count` times (1 by default, at most 100,000, `LootRolls.MAX_ROLLS`) with a
  real random source (`new SplittableRandom()`).
  - **It tells the caller, per tier:** how many gave (or landed, for the default), how many were skipped for an empty
    pool, and the true odds; how many rolls gave nothing; and, for a source, every item picked with its count.
  - **One `LOOTROLL` log line per call** carries the same figures.
  - **It gives nothing:** `LootDevCommandTest` scans the class for any inventory, world or give path.
- **`/rpg loot odds <source|default>`:** the written chance and the true odds per tier.
- **The example source, `dev_example_not_a_drop`:** under `dev_sources:` in `content/loot/cascade.yml`, loaded ONLY
  on a dev server.
  - Every dev source starts `dev_`, and no real source may, so it cannot be mistaken for a drop.
  - Its pools are existing items, each under its own rarity: exotic 1, legendary 3, rare 6, uncommon 7, common 2.
  - **No epic pool** (no content is epic), so an epic that lands is skipped. The deletion set's dev weapons are left
    out.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

The booter's checklist is `.claude/rules/booting.md`; this list is its instance for this boot.

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup:** `run/plugins/Rpg` and `run/world` copied, as they are, to a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-loot-stack`. Report:
   - the file and directory counts;
   - `diff -rq lines: 0` for each;
   - the `ls` of `world/dimensions`;
   - the overworld's file count (`world/dimensions/minecraft/overworld`).

   Nothing is restored (Ben's word only).
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **No arena files yet:** `ls run/plugins/Rpg/arenas` reports no such folder, or an empty one; any file in it is listed
   by name. TA1 writes `ta_one.nbt` there.
6. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND IS SENT:** "booting now", and Ben told **NOT to log in until told R0
   passed.** The boot command goes in the next turn.
7. `./scripts/dev-server.sh --refresh-content`. **The REQUIRED deployed lines** (under `run/plugins/Rpg/content`):
   - `fights/dragon.yml` reads `  source: generated`, `  all_down_seconds: 5` and `  revive_seconds: 3`;
   - `world_damage.yml` reads `  share_of_max_per_tick: 0.10`;
   - `loot/cascade.yml` reads `  dev_example_not_a_drop:`.
8. R0a to R0e read on **this boot's** log (its `[Rpg] Build:` names this branch's head).
9. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
10. *(After his `/stop`.)* Report:
    - `rpgfight` empty;
    - the overworld's file count against step 2's;
    - `run/plugins/Rpg/arenas/ta_one.nbt` present, with its size in bytes;
    - `All dimensions are saved`;
    - no `java` process left.

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED. IF YOU ARE TOLD NOTHING, DO NOT LOG IN.**
1. `/gamemode survival` — **alone, the first line after login.**
2. **THE MANUAL ROW FIRST (FDM1, `GATE-fight-down.md`). IT PUTS YOU DOWN ON PURPOSE:**
   - Type `/rpg fight enter nodragon`.
   - Walk off the edge of the floor into the void.
   - **As soon as you are spectating, disconnect.**
   - Wait ten seconds, reconnect, and say where you are and whether you are in survival.
3. Stand on flat open ground with room all round.
4. `/rpg selftest fight-down FD2R2` — **ONCE. It puts you down on purpose** (the void, a tenth of your health each half
   second), then sends you home in survival with everything you carried. About fifteen seconds. Do not move.
5. `/rpg selftest fight-site MG5R3`, then `/rpg selftest fight-site MG7R3` — once each, about a minute each. **Nothing
   hurts you.**
6. `/rpg selftest fight-template` — **ONCE.** One row, TA1: the arena is exported to a file. **Nothing hurts you.**
7. `/rpg selftest loot-roll` — **ONCE.** Five rows: rolls and odds in chat. **Nothing hurts you and nothing is given.**
8. `/stop`

Nothing else this boot. If a row stalls or aborts, do not intervene. If his reconnect fails or he is left in spectator
anywhere: CC STOPS, reports the log lines, and touches nothing in `run/world`.

### WHY TB1-TB6 ARE NOT ON THIS SHEET (the seat asked how the file gets to the loader)

- **The file needs no moving.** READ, `RpgPlugin`: `/rpg fight export <name>` (and TA1) writes
  `run/plugins/Rpg/arenas/<name>.nbt`. `ArenaTemplates` is built on that same folder (`new File(getDataFolder(),
  "arenas")`), and `source: template` reads `arenas/<template_file>.nbt` from it. So after TA1, `ta_one.nbt` is already
  where the loader reads.
- **But the source is chosen at enable and cannot be switched inside one start.** READ, `FightLoader`: `arena.source`
  and `template_file` are read from `content/fights/dragon.yml` when the plugin enables, and nothing re-reads it while
  the server runs.
  - TB1-TB3 need the deployed `dragon.yml` to say `source: template` and `template_file: ta_one`;
  - TB4 needs `template_file: no_such`;
  - TB5 needs a malformed file.
  - **That is three more starts, each with an edit under `run/plugins/Rpg`**, which the standing rules forbid except by
    the checklist.
- **For the seat to choose** (neither is built):
  - (a) a second start with the deployed `dragon.yml` edited by CC under a ruling, for TB1-TB3, with TB4 and TB5 later;
  - (b) a dev-only override, e.g. `/rpg fight enter template <file>`, so TB1-TB5 run in one start with no content
    edit.
- **TB6** waits for Ben's own arena either way.

## THE ARITHMETIC (SELFTEST SAFETY): AT DEFENCE 0, AGAINST 100, AND WHERE EACH ROW PUTS HIM

**Against 100, the base maximum with no gear.** His real maximum was 350 at the last boot; each row's heal prints it.
The ring is 48 blocks about C; the disc is 56.

| row | where he is | what can hurt him | the most it does, at defence 0 against 100 | puts him down? |
|---|---|---|---|---|
| FDM1 | walks from C over the ring (48) to the disc's edge (56), then falls past the End void line (y -64) | the ring's strikes once outside (20, then doubling, every 40 ticks); then the void (10 a tick, every 10 ticks) | **lethal, on purpose.** Two strikes (60) before he falls are likely, and the third (80) would finish 100. So **he may go down to the LIGHTNING before the void.** FDM1's `cause=VOID` (its prediction, unchanged on the seat's word) may read `cause=KILL` instead, as RV4.s strike-down did at `cb821675`: flagged, not re-predicted | **YES, on purpose** |
| FD2R2 | straight down from C to y -130: inside the ring's footprint, below the void line | the void only; every strike finds him inside and deals 0 | **lethal, on purpose:** ten ticks of 10, then what regeneration paid | **YES, on purpose** |
| MG5R3 | the ordinary world, 1 west of the Mage, inside every ring | strikes, which find him inside | 0 (every `STRIKE … k=0 damage=0.000 inside`, the rows' own prediction) | no |
| MG7R3 | the same | strikes; the two `damage` commands hit the crystal, not him | 0 | no |
| TA1 | C, no dragon, no Mage | strikes, which find him inside | 0 | no |
| LR1-LR5 | wherever he stands; no fight | nothing | 0 | no |

**The rows that put him down: FDM1 and FD2R2.** Both set or need no `death_rules` of their own beyond what they have:
FD2R2's scenario sets `death_rules` and heals first; FDM1 is manual, and a down is not a death (F9d), so nothing is
dropped either way.

## R0 — THE BUILD, FOR THE WHOLE STACK. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `feat/loot-cascade`'s head as `git ls-remote origin refs/heads/feat/loot-cascade` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **AS PREDICTED (2026-11-20, 10:24:07).** `[Rpg] Build: 5c2dee2c`; `git ls-remote origin refs/heads/feat/loot-cascade` printed `5c2dee2cfd7d6adfd82705eac00d5446962229c5`, the head after the FDM1 note (the seat's ruling 3). Not `-dirty`, not `unknown` |

### R0b — the jar carries every step of the stack

| prediction | instrument | READING |
|---|---|---|
| PRESENT: F9d and F9e as `GATE-fight-revive.md`'s R0b (`dragon.yml` `  all_down_seconds: 5`, `  revive_seconds: 3`; `FIGHT copy laid`; `FIGHT down player=`; `FIGHT revived player=`; `DownRoster`, `CopyPlace`, `ReviveTracker`); the template (`TemplateLayout.class`, `ArenaTemplates.class` carrying `template exported`, `dragon.yml` `  source: generated`); the void (`VoidShare.class`, `WorldDamageLoader.class` carrying `Loaded world damage`, `world_damage.yml` `  share_of_max_per_tick: 0.10`); the loot (`CascadeTable.class`, `LootRolls.class`, `LootDevCommand.class` carrying `LOOTROLL`, `cascade.yml` `  dev_example_not_a_drop:`); the scenarios `GATE-fight-down.yml` `FD2R2:`, `GATE-fight-template.yml` `TA1:`, `GATE-loot-roll.yml` `LR2:`. ABSENT (the controls): `dragon.yml` `  environment: normal`; `NoSuchTextControl` in `FightInstance.class`; `cascade.yml` `  dev_try_me:`; `GATE-fight-down.yml` `FD2:` (retired) | the PowerShell below, from the repo root | **AS PREDICTED.** The jar written 10:23:59: every PRESENT line PRESENT (the three `dragon.yml` lines, `world_damage.yml`'s share, `cascade.yml`'s example, `FIGHT copy laid`, `FIGHT down player=`, `FIGHT revived player=`, the four core fight entries, `template exported`, `VoidShare`, `Loaded world damage`, `CascadeTable`, `LootRolls`, `LOOTROLL`, `FD2R2:`, `TA1:`, `LR2:`). The four controls ABSENT: `  environment: normal`, `NoSuchTextControl`, `  dev_try_me:`, `FD2:` |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Line($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
function Entry($entry) { if ($zip.GetEntry($entry) -ne $null) { "PRESENT entry $entry" } else { "ABSENT  entry $entry" } }
$d = Bytes 'content/fights/dragon.yml'
Line $d '  all_down_seconds: 5' 'dragon.yml'; Line $d '  revive_seconds: 3' 'dragon.yml'; Line $d '  source: generated' 'dragon.yml'; Line $d '  environment: normal' 'dragon.yml'
$w = Bytes 'content/world_damage.yml'; Line $w '  share_of_max_per_tick: 0.10' 'world_damage.yml'
$c = Bytes 'content/loot/cascade.yml'; Line $c '  dev_example_not_a_drop:' 'cascade.yml'; Line $c '  dev_try_me:' 'cascade.yml'
$p = 'io/github/butterflysmp/rpg/'
Has ($p + 'paper/fight/FightInstance.class') 'FIGHT copy laid'; Has ($p + 'paper/fight/FightInstance.class') 'NoSuchTextControl'
Has ($p + 'paper/fight/FightFixtures.class') 'FIGHT down player='; Has ($p + 'paper/fight/FightFixtures.class') 'FIGHT revived player='
foreach ($c2 in 'DownRoster', 'CopyPlace', 'ReviveTracker', 'TemplateLayout') { Entry ($p + "core/fight/$c2.class") }
Has ($p + 'paper/fight/ArenaTemplates.class') 'template exported'
Entry ($p + 'core/combat/VoidShare.class'); Has ($p + 'paper/content/WorldDamageLoader.class') 'Loaded world damage'
Entry ($p + 'core/loot/CascadeTable.class'); Entry ($p + 'core/loot/LootRolls.class'); Has ($p + 'paper/command/LootDevCommand.class') 'LOOTROLL'
$f = Bytes 'selftest/GATE-fight-down.yml'; Line $f 'FD2R2:' 'GATE-fight-down.yml'; Line $f 'FD2:' 'GATE-fight-down.yml'
Line (Bytes 'selftest/GATE-fight-template.yml') 'TA1:' 'GATE-fight-template.yml'
Line (Bytes 'selftest/GATE-loot-roll.yml') 'LR2:' 'GATE-loot-roll.yml'
$zip.Dispose()
```

**A jar from master reads ABSENT on the template, void and loot lines**, and the four controls read ABSENT on either jar.

### R0c — every Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| Exactly one of each, up to the first `joined the game`: **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`** (unchanged); **`GATE-fight-revive.md`'s R0c fight line, word for word** (unchanged on `source: generated`; `FightLoaderTest` asserts it verbatim); **`[Rpg] Loaded world damage: void takes 0.10 of a player's maximum health per void tick, defence bypassed; mobs unchanged`** (`WorldDamageLoaderTest` asserts it verbatim); **`[Rpg] Loaded loot 'cascade': default exotic 1.00% (truly 1.000%), legendary 3.00% (truly 2.970%), epic 8.00% (truly 7.682%), rare 12.00% (truly 10.602%), uncommon 20.00% (truly 15.549%), common 40.00% (truly 24.879%), nothing truly 37.318%; 1 sources [dev_example_not_a_drop]`** (`LootLoaderTest` asserts it verbatim). No `Refusing`, `Skipping`, `SEVERE` or `Exception`. **No `[Rpg]` WARN beyond `volley_stone`'s known pair.** No `No fights loaded` | `Select-String run\logs\latest.log -Pattern 'Loaded\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded'` | **AS PREDICTED.** The four Loaded lines (content, fight, world damage, loot), once each, every one equal to its predicted text by a scripted string comparison (CR stripped). `[Rpg]` WARN only `volley_stone`'s pair (10:24:08). No `Refusing`, `Skipping`, `SEVERE`, `Exception` or `No fights loaded`, before his join (10:42:36) or anywhere in the log |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 235 scenarios across 30 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive, fight-site, fight-arena, fight-dragon, fight-rules, fight-end, fight-down, fight-revive, fight-template, loot-roll]`**, no refusal after it. **The control:** the last boot's `228 scenarios across 28 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | **AS PREDICTED.** `[Rpg] Selftest: dev server, 235 scenarios across 30 gates [...]` once, equal to the predicted text by a scripted comparison; `228 scenarios across 28 gates` ABSENT (0 lines) |

**RECOUNTED** (2026-11-19, this branch's tree, from `index.yml` and every gate file it names; `SelfTestScenariosTest`
asserts 235):
- the gates in `index.yml`: **30**;
- `^[A-Za-z0-9_-]+:` over them: **265**;
- `^setup:`: **30**;
- so **235**.

**Control:** 256 (the last boot) + fight-template's 2 (setup, TA1) + EB4R3's 1 − FD2's 1 + FD2R2's 1 + loot-roll's 6
(setup, LR1-LR5) = 265.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, and no `deleting` and no `orphan` line | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | **AS PREDICTED.** One `FIGHTWORLD sweep path=<repo>\run\world\dimensions\rpgfight folders=0` (10:24:08); no `deleting` and no `orphan` line before his join |

## THE ROWS

### LR1 — the odds, written and true (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `rpg loot odds default`: REPLY `Loot odds 'default':`, then `  exotic: written 1.00%, truly 1.000%`, `  legendary: written 3.00%, truly 2.970%`, `  epic: written 8.00%, truly 7.682%`, `  rare: written 12.00%, truly 10.602%`, `  uncommon: written 20.00%, truly 15.549%`, `  common: written 40.00%, truly 24.879%`, `  nothing: truly 37.318%`. (2) `rpg loot odds dev_example_not_a_drop`: the same lines under its name (it uses the default's tiers) | `CascadeTableTest` *theTrueOdds…*; `LootDevCommandTest` *theOddsLines…* | **AS PREDICTED (2026-11-20, 11:01:46).** (1) `Loot odds 'default':`, then the seven lines word for word: exotic `written 1.00%, truly 1.000%`, legendary `3.00%, 2.970%`, epic `8.00%, 7.682%`, rare `12.00%, 10.602%`, uncommon `20.00%, 15.549%`, common `40.00%, 24.879%`, `nothing: truly 37.318%`. (2) `Loot odds 'dev_example_not_a_drop':` with the same seven lines |

### LR2 — 10,000 rolls of the example source: each tier near its true odds, epic always skipped, nothing given (SURVIVAL)

**The expected counts, computed** (10,000 rolls). Each tolerance is 4 standard deviations of a binomial, rounded inwards.
**Every epic that lands is SKIPPED and the roll goes on**, so the tiers below epic give more often than their table odds:
- a roll reaches rare with probability 0.99 x 0.97 = 0.9603, epic counting as a miss;
- the command still prints each tier's TABLE true odds beside its count, and the gap between them is the skip's.

| tier | what is counted | p (computed) | expected | 4 sigma | accepted range |
|---|---|---|---|---|---|
| exotic | gave | 0.010000 | 100.0 | 39.8 | 61..139 |
| legendary | gave | 0.029700 | 297.0 | 67.9 | 230..364 |
| epic | **gave 0; SKIPPED** | 0.076824 | 768.2 skipped | 106.5 | **gave exactly 0; skipped 662..874 (above zero)** |
| rare | gave | 0.115236 | 1152.4 | 127.7 | 1025..1280 |
| uncommon | gave | 0.169013 | 1690.1 | 149.9 | 1541..1840 |
| common | gave | 0.270420 | 2704.2 | 177.7 | 2527..2881 |
| nothing | gave nothing | 0.405631 | 4056.3 | 196.4 | 3860..4252 |

| prediction | stands on | READING |
|---|---|---|
| (1) Before: **`INVENTORY volley_stone slots=<s1>`** and **`INVENTORY locust slots=<s2>`** (whatever he carries). (2) The reply begins **`Loot roll 'dev_example_not_a_drop' x10000 (nothing given to anyone):`**, and each tier's line carries a count in the range above, `skipped 0` for every tier but epic, and its table true odds. (3) **One `LOOTROLL table=dev_example_not_a_drop rolls=10000 …` line**, ending **`given_to_players=0 picked={…}`**: the picked items sum to the gave counts (all but nothing's), each item under its own rarity's pool. (4) After: **the same two `INVENTORY` lines as before, unchanged** — the roll gave nothing | `LootTallyTest`, `LootRollsTest`, `LootDevCommandTest` *theCommandGivesNothing* | **AS PREDICTED.** (1) Before: `INVENTORY volley_stone slots=[]`, `INVENTORY locust slots=[]`. (2) `Loot roll 'dev_example_not_a_drop' x10000 (nothing given to anyone):`, and every count is in its range: exotic gave **96** (61..139), legendary **287** (230..364), **epic gave 0, skipped 819** (662..874), rare **1162** (1025..1280), uncommon **1771** (1541..1840), common **2626** (2527..2881), nothing **4058** (3860..4252). Every tier but epic reads `skipped 0`, with its table true odds. (3) One `LOOTROLL table=dev_example_not_a_drop rolls=10000 … items=5942 given_to_players=0 picked={…}`: **the picked items sum to 5,942, the gave counts' sum, and each pool's picks equal its tier's gave count** (exotic 96, legendary 287, rare 1162, uncommon 1771, common 2626, a scripted sum). 19 distinct items, exactly the 19 pool members. (4) After: **the same two lines, `slots=[]`, unchanged.** He carried neither item, so a give of either would have shown as a slot. The probe watches 2 of the 19 pool items; the rest of the give-nothing claim stands on `given_to_players=0` and `LootDevCommandTest` |

### LR3 — 10,000 rolls of the default table: landings near the table's true odds, nothing skipped (SURVIVAL)

The expected counts, computed (no pools, so every landing counts and nothing is skipped). Each tolerance is 4 sigma,
rounded inwards:

| tier | p (the table's true odds) | expected | accepted range |
|---|---|---|---|
| exotic | 0.010000 | 100.0 | 61..139 |
| legendary | 0.029700 | 297.0 | 230..364 |
| epic | 0.076824 | 768.2 | 662..874 |
| rare | 0.106017 | 1060.2 | 938..1183 |
| uncommon | 0.155492 | 1554.9 | 1410..1699 |
| common | 0.248787 | 2487.9 | 2315..2660 |
| nothing | 0.373180 | 3731.8 | 3539..3925 |

| prediction | stands on | READING |
|---|---|---|
| REPLY `Loot roll 'default' x10000 (nothing given to anyone):`. Each tier reads **`landed <n>`** in its range, with **`skipped 0`**; nothing in its range. **Against LR2: epic LANDS here and is skipped there**, and the tiers below it give more there. The pair shows the skip. One `LOOTROLL table=default rolls=10000 … items=0 given_to_players=0` line, with no `picked=` | `LootRollsTest` *withoutPoolsItTalliesRarities* | **AS PREDICTED.** `Loot roll 'default' x10000 (nothing given to anyone):`. Every tier `landed` in range with `skipped 0`: exotic **91**, legendary **309**, **epic 746** (662..874), rare **1007**, uncommon **1580**, common **2505**, nothing **3762**. **Against LR2: epic LANDED 746 here and was SKIPPED 819 there**, and the tiers below it gave more there (rare 1162 against 1007). One `LOOTROLL table=default rolls=10000 … items=0 given_to_players=0`, with no `picked=` |

### LR4 — one roll when no count is given (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| REPLY `Loot roll 'dev_example_not_a_drop' x1 (nothing given to anyone):`. Its tier counts sum, with nothing's, to 1; one `LOOTROLL … rolls=1 …` line | `LootDevCommand` (the default count, 1) | **AS PREDICTED.** `Loot roll 'dev_example_not_a_drop' x1 (nothing given to anyone):`. common gave 1, every other tier 0, nothing 0: the sum is 1. One `LOOTROLL … rolls=1 … items=1 given_to_players=0 picked={short_bow=1}` |

### LR5 — an unknown table is refused (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| REPLY **`No loot table 'no_such_table'. Tables: [default, dev_example_not_a_drop]`**, and no `LOOTROLL` line | `LootDevCommandTest` *defaultIsTheDefaultTable…* | **AS PREDICTED.** REPLY **`No loot table 'no_such_table'. Tables: [default, dev_example_not_a_drop]`**, and no `LOOTROLL` line in the row (0) |

## WHAT THIS GATE DOES NOT READ

- **Any real drop source:** none exists; `sources: {}` ships empty.
- **The uniform pick's evenness within a pool:** the `picked=` counts show it, and no range is predicted. A pool of 6
  over 1152 rare rolls is about 192 each.
- **A non-dev server:** there the example is not loaded and the command is absent (`LootLoaderTest`).

## THE STACK'S BOOT, 2026-11-20, 10:24 TO 11:02 — THE CHECKLIST, THE AFTER-RUN, AND WHAT IT FOUND

Written after Ben's `/stop`. **No prediction above was edited.** The readings are in the cells above and in each row's
own gate:
- FDM1 and FD2R2: `GATE-fight-down.md`;
- MG5R3 and MG7R3: `GATE-fight-site.md`;
- TA1: `GATE-fight-template.md`.

**Every row on the sheet ran.** None stalled or aborted.

### The booter's checklist (`.claude/rules/booting.md`), ticked

1. **The seat's word and Ben's:** the seat's `BOOT APPROVED for the stack PR #218 + PR #219, 2026-11-20` (with its
   rulings 1 to 3), then Ben's "ready" (his own message).
2. **The heads:** `git ls-remote` printed:
   - `feat/loot-cascade` `5c2dee2cfd7d6adfd82705eac00d5446962229c5` (`a342e77f` plus the one FDM1 docs commit the seat's
     ruling 3 allowed);
   - `feat/fight-template` `114b109bc1a267bc62f1523d7e66b1b59b6ac04d`;
   - master `cf3547e375fc9ace0ade04bf8ad044942a7848e2`.

   The tree was clean.
3. **The carried state, in order:**
   1. `Get-Process java` printed nothing.
   2. Backup `../rpg-run-backups/2026-11-20-pre-loot-stack` (a new folder, 10:22:57):
      - Rpg `files=133 dirs=24`, `diff -rq lines: 0`;
      - world `files=193 dirs=32`, `diff -rq lines: 0`;
      - `world/dimensions`: `minecraft`, `rpgfight`;
      - the overworld: **106** files.
   3. `12:  dev-pack-on-join: true`.
   4. `find run/world/dimensions/rpgfight -mindepth 1` printed nothing.
   5. `run/plugins/Rpg/arenas`: no such folder.
4. **"Booting now", in its own message**, with "Ben: do NOT log in until I tell you R0 passed". The turn ended there,
   and the boot command came after Ben's "ok".
5. `./scripts/dev-server.sh --refresh-content`; `Done` at 10:24:08. The REQUIRED deployed lines:
   - `dragon.yml` `137:  all_down_seconds: 5`, `142:  source: generated`, `153:  revive_seconds: 3`;
   - `world_damage.yml` `12:  share_of_max_per_tick: 0.10`;
   - `cascade.yml` `40:  dev_example_not_a_drop:`.
6. **R0a to R0e read on this boot's log**, `[Rpg] Build: 5c2dee2c`: all as predicted.
7. **"R0 passed", in its own message**, with the R0 lines read at 10:24:38.
8. **His login against it:** every R0 line is at 10:24:07 to 10:24:08; his first `joined the game` is **10:42:36**, after
   "R0 passed".
9. **After his `/stop` (11:02:03):**
   - `rpgfight` empty;
   - **the overworld 106 files, against the backup's 106**;
   - `run/plugins/Rpg/arenas/ta_one.nbt` present, **569,338 bytes**;
   - `Saving worlds`, `ThreadedAnvilChunkStorage: All dimensions are saved` and `All RegionFile I/O tasks to complete`,
     all at 11:02:04;
   - `Get-Process java` printed nothing.

### What he typed, in order (every `issued server command` line)

1. `/gamemode spectator` (10:42:44). **Not on the sheet: its first line is `/gamemode survival`.** He was told, and the
   next line followed.
2. `/gamemode survival` (10:42:48).
3. FDM1: `/rpg fight enter nodragon` (10:43:16), the walk, the quit (10:43:32), and the reconnect (10:43:46). His chat at
   10:44:05: "I am where I was and in survival".
4. `/rpg selftest fight-down FD2R2` (10:44:27).
5. He disconnected at 10:45:19 and rejoined at 10:58:30, between rows, and nothing ran in between.
6. `/rpg selftest fight-site MG5R3` (10:58:35), then `/rpg selftest fight-site MG7R3` (11:00:16).
7. `/weather clear` (11:01:24). **Not on the sheet;** it changes the weather only.
8. `/rpg selftest fight-template` (11:01:25), then `/rpg selftest loot-roll` (11:01:46).
9. `/stop` (11:02:03).

### The lines the seat asked for

- **FDM1:**
  - the down: `FIGHT down … t=293 cause=VOID at=-2.56 -233.41 -61.49`, after three strikes outside (20, 40, 80);
  - the stale login: `FIGHTWORLD stale login caught … at=world 0.50 73.00 0.50 to=world -105.05 63.00 21.68`;
  - **his mode after the reconnect: SURVIVAL**, read from the server's `mode … from=SPECTATOR to=SURVIVAL why=login`
    and from FD2R2's next `entered … mode=SURVIVAL`.
- **FD2R2:**
  - **eleven HURT, all `cause=void`, `before` equal to `after`: ten of 35.000 (exactly M/10 at M=350) and one of
    10.000** (the remainder regeneration paid);
  - the ticks, bracketed between ring ticks, fit one 10-tick cadence (about t=17, 27 … 117);
  - **DOWN on the eleventh, t=118**;
  - **no strike landed** (both `k=0 damage=0.000 inside`).
- **MG5R3:** 24 STRIKE lines; **MG7R3:** 12. **Every one is `inside`, `k=0 damage=0.000`, and neither row has a HURT
  line.**
- **TA1:**
  - `file=plugins\Rpg\arenas\ta_one.nbt`, relative to `run`; on disk, 569,338 bytes;
  - `size=113 16 113 corner=-56 62 -56 markers=11 ms=278`.
- **LR1-LR5:** every count is in its range.
  - LR2's epic: gave 0, **skipped 819**.
  - LR3's epic: landed 746.
  - **His inventory in LR2:** `volley_stone slots=[]` and `locust slots=[]`, before and after, unchanged.
- **The Loaded lines, as R0c read them:**
  - `[Rpg] Loaded world damage: void takes 0.10 of a player's maximum health per void tick, defence bypassed; mobs
    unchanged`;
  - `[Rpg] Loaded loot 'cascade': default exotic 1.00% (truly 1.000%), … nothing truly 37.318%; 1 sources
    [dev_example_not_a_drop]`.
- **`FIGHTWORLD deleting` / `deleted`, 3 each, each `entries=17`:** `<repo>\run\world\dimensions\rpgfight\fight_abb5a8c9`
  (FDM1), `…\fight_966a7078` (FD2R2), and `…\fight_77bdf54d` (TA1). MG5R3 and MG7R3 ran in the ordinary world.
- **`[Rpg]` lines matching `Refusing|Skipping|SEVERE|WARN|Exception`:** only `volley_stone`'s pair, in the whole log.
- **`unload-retry`:** one line, on FD2R2's ALL-DOWN ending, as its fix predicted. FDM1's world unloaded with him gone,
  with no retry.

### WHAT IT FOUND

1. **The void rule reads on a server as built.** FD2R2's eleven hits are M/10 with defence bypassed, at the server's
   10-tick cadence. He went down on the eleventh, after regeneration paid a remainder of 10.
2. **FDM1's cause was VOID, after three strikes outside the ring.** The note's two-branch prediction held on its first
   branch. The walk and the fall took long enough for strikes k=1 to k=3 to land before the void took the rest.
3. **TA1's `file=` prints a path relative to the server's directory**, not the absolute path predicted. It is the same
   file. The prediction's form was wrong, not the export.
4. **Two commands were off the sheet:** `/gamemode spectator` first, corrected four seconds later before any row, and
   `/weather clear` between MG7R3 and TA1. Neither touched a row's reading.
5. **TB1-TB6 are carried**, by the seat's ruling 2.

**2026-11-21, a dated note (feat/knell-beam): "no content is epic" in this gate is no longer true.** `knell_beam` is the
first epic item. It joins NO pool (Ben: "It will be a dragon drop later"), so the dev example source still has no
epic pool, an epic that lands is still skipped, and LR2's prediction would still hold on a re-run. Nothing above is edited.
