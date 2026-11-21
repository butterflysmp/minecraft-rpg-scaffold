# GATE — THE FOUR DEV WEAPONS DELETED (feat/remove-dev-weapons), AND THE ONE BOOT FOR THE DELETION STACK

**Status: NOT RUN.** Written 2026-11-21 on `feat/remove-dev-weapons` (PR A), on master `988f82d8` (the squash of
#223), before any boot. **This file carries the ONE R0 and the ONE run sheet for the stack**: PR A (this branch) and
PR B, the owed fixes, stacked on it, which appends its own rows here before the boot.

**NO BOOT until the seat has diffed the code AND this gate from origin, and Ben says "ready" in his own words**; then
only CC boots, by `.claude/rules/booting.md`.

**Game mode:** SURVIVAL, every row. **Difficulty:** whatever the world holds; no row here reads a mob or a hit.
**Nothing on this boot can hurt him** (the arithmetic, below).

**Ben's words, verbatim:**
- 2026-11-21: "removing dev weapons is a good place to start".
- 2026-11-19 (`PLAN-weapon-tiers.md` 1.1): "Just a quick note, the emberblade should also be slated for deletion."

## WHAT THE BRANCH DOES

- **Deletes** `content/weapons/ironblade.yml`, `emberblade.yml`, `hunters_bow.yml` and `quiver_stone.yml`. 23 weapon
  files become 19. `volley_stone` is NOT touched.
- **The loot table:** `emberblade` leaves `cascade.yml`'s dev source rare pool, which now holds five:
  `ember_staff, locust, brawlers_gauntlet, fletchers_quiver, sages_scroll`.
- **The selftest:** every step that gave or held `emberblade` now gives and holds `iron_sword` (13 rows); SCA4 gives
  the minted `wooden_sword`; STA5 and DH6 are RETIRED, and DH3 loses its `quiver_stone` steps. **None of those rows is
  re-read on this boot**; each gate's appended note says so.
- **A new guard:** `SelfTestWeaponIdsTest` fails the build when a scenario names a weapon that does not ship. This gate
  is its one exception (DW1 and DW2 name the four on purpose), pinned step by step.
- **No Java behaviour changes.** Comments only, in main code.

## WHAT PR B ADDS (`feat/dev-weapons-owed`, stacked on PR A; appended before any boot)

- **Selftest staging makes room** (`SelfTestDrivers.hold`, `SelfTestStaging.hotbarPlan`): with no empty hotbar slot, a
  weapon outside the hotbar is SWAPPED with the first hotbar slot that is not the Ability Stone, logged
  **`STAGE swapped <id> from slot <f> with hotbar <t>, which held <x> (now in slot <f>)`**. Nothing is dropped or
  destroyed. Read by **HS1**.
- **A craft that mints a weapon logs it** (`InventoryCraft.commitCraft`): **`CRAFT minted player=<name> weapon=<id>
  material=<material> score=<score>`**. Read by **CR1**, Ben's craft, which also witnesses the carried STM1
  (`GATE-sword-tiers.md`).
- Docs only: Ben's three sword-boot answers in `PLAN-weapon-tiers.md`; a checks step in `merge-procedure`; the
  daylight rule in `.claude/rules/verification.md`.

## WHAT WAS READ, EACH MARKED

- **A held copy of a deleted weapon** (READ, our source): at login `RpgListeners.onJoin` calls `GearRefresher.refresh`
  over `PlayerInventory.getContents()`. An item whose `weapon_id` names no loaded weapon takes
  `RefreshVerdict.Dangling`; the refresher leaves it untouched and calls `AdapterContext.warnOnce("Refresh: item carries
  unknown gear id '<id>' -- left untouched; its content file is missing or renamed")`. **`warnOnce` logs a message the
  first time only, for the life of the server** (`warned.add(message)`), so at most ONE such line per id, however many
  copies he carries.
- **`probe_inventory`** (READ, `SelfTestDrivers.probeInventory`) reads the same `getContents()` and matches the
  item's `weapon_id` string through `WeaponItems.weaponId`, which reads the tag and does not consult the registry. So
  it finds a deleted id's copy exactly where the refresher saw it.
- **`/rpg give <unknown id>`** (READ, `RpgCommand.give`): the argument is a plain `word()`, so any id reaches the
  method; no registry finds it, and the replies are `Unknown weapon, shield, armor, tool or accessory: <id>` (red) and
  `Available: <every gear id, comma-separated>` (gray). Nothing is given.
- **`/rpg loot`** (READ, `LootDevCommand`): `odds` prints written and true chances per tier; `roll` prints per-tier
  counts and the items it picked. **Neither prints a source's pools**, so the brief's loot row ("the rare pool has 5
  entries and no emberblade, if the command prints pools") is LEFT OUT. The pool is pinned by
  `LootLoaderTest.theShippedDevExampleNamesRealItemsOfTheirOwnRarityAndNoEpic` instead.
- **`--refresh-content`** (READ, `scripts/dev-server.sh`): `rm -rf "$CONTENT_DIR"` before the boot, so the deployed
  copies of the four files under `run/plugins/Rpg/content/weapons/` go and are not re-copied.
- **`/rpg give` into a FULL inventory** (READ, `RpgCommand.give`): `firstEmpty() == -1` replies `Your inventory is
  full -- make room and try again.` and gives nothing; otherwise `addItem` puts the item in a free slot, which is in the
  main inventory when the hotbar is full (INFERRED: Bukkit fills the first empty slot, hotbar first). So a give into a
  full hotbar lands outside it, which is exactly the case `hold`'s swap now covers; a give into a full INVENTORY gives
  nothing, and the following `hold` still reports `no hotbar slot holds <id>`.
- **The run's inventory restore** (READ, `SelfTestSnapshot`): the contents and held slot captured at run start are
  written back at every gate boundary (`restoreInventory`) and at the run's end (`restore`). A swap is therefore undone.
- **The crafting table is ours** (READ): `RpgListeners`' hijack table opens `CraftingMenu` for `CRAFTING_TABLE`, and its
  craft click calls `InventoryCraft.commitCraft`, where the mint and the new line are.
- **What the loaded lines were at the last boot** (READ, `GATE-sword-convert.md` R0c, read AS PREDICTED 2026-11-20 at
  `90ea1517`): `… 23 weapons …` and `Mint-on-craft: 36 indexed, 36 claiming, of 53 gear definitions`. **No content
  file differs between `90ea1517` and this branch's base** (`git diff --stat 90ea1517 988f82d8 --
  paper/src/main/resources/content` printed nothing). The four deleted files carry no `craft_result:` line (READ, 0
  each at the base), and `allGear` in `RpgPlugin` is weapons + shields + armor + tools (READ), so 53 − 4 = 49 and the
  indexed and claiming counts do not move.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

The booter's checklist is `.claude/rules/booting.md`; this list is its instance for this boot.

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup:** `run/plugins/Rpg` and `run/world` copied, as they are, to a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-dev-weapons`. Report:
   - the file and directory counts;
   - `diff -rq lines: 0` for each;
   - the `ls` of `world/dimensions`;
   - the overworld's file count.

   Nothing is restored (Ben's word only).
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND IS SENT:** "booting now", and Ben told **NOT to log in until told R0
   passed.** The boot command goes in the next turn.
6. `./scripts/dev-server.sh --refresh-content`. **The REQUIRED deployed lines** (under `run/plugins/Rpg/content`):
   - `weapons/` holds **19** `.yml` files, and none of `ironblade.yml`, `emberblade.yml`, `hunters_bow.yml`,
     `quiver_stone.yml`;
   - `weapons/iron_sword.yml` reads `attack_damage: 23` (the control);
   - `loot/cascade.yml` reads `      rare: [ember_staff, locust, brawlers_gauntlet, fletchers_quiver, sages_scroll]`.
7. R0a to R0e read on **this boot's** log (its `[Rpg] Build:` names the stack top's head).
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* Report:
   - `rpgfight` empty;
   - the overworld's file count against step 2's;
   - `All dimensions are saved`;
   - no `java` process left.

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED. IF YOU ARE TOLD NOTHING, DO NOT LOG IN.**
1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest remove-dev-weapons` — **ONCE.** Three rows, a few seconds. You are given an Iron Sword, nine wooden
   shovels and a Boltor; all of it is taken back at the end and your own items are put back. **Nothing hurts you.**
3. `/give BaronVonYeetus minecraft:crafting_table`, then `/give BaronVonYeetus minecraft:iron_ingot 2`, then
   `/give BaronVonYeetus minecraft:stick 1`.
4. **CR1:** place the crafting table, craft ONE iron sword (two iron ingots and a stick) and take it. Hover it and note
   the Gear Score its tooltip shows.
5. `/stop`

Nothing else this boot. If a row stalls or aborts, do not intervene.

## THE ARITHMETIC (SELFTEST SAFETY): AT DEFENCE 0, AGAINST 100, AND WHERE EACH ROW PUTS HIM

| row | where he is | what can hurt him | the most it does | puts him down? |
|---|---|---|---|---|
| DW1, DW2, HS1 | wherever he stands, in the overworld; commands, gives, probes and a hotbar selection only. No mob, no fight world, no storm ring | nothing | 0 | no |
| CR1 | at a crafting table he places | nothing | 0 | no |

**No row puts him down.** No row sets `death_rules`, spawns a mob, enters the fight world or deals damage, so there is
no hit to multiply against 100 or 350, and no row puts him near the storm ring.

## R0 — THE BUILD, FOR THE WHOLE STACK. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, the stack top's head, `feat/dev-weapons-owed` (PR B), as `git ls-remote origin refs/heads/feat/dev-weapons-owed` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries the deletion

| prediction | instrument | READING |
|---|---|---|
| ABSENT: the entries `content/weapons/ironblade.yml`, `content/weapons/emberblade.yml`, `content/weapons/hunters_bow.yml`, `content/weapons/quiver_stone.yml`. PRESENT (the control): `content/weapons/iron_sword.yml` line `attack_damage: 23`. PRESENT: `content/loot/cascade.yml` line `      rare: [ember_staff, locust, brawlers_gauntlet, fletchers_quiver, sages_scroll]`; `selftest/GATE-remove-dev-weapons.yml` line `DW1:`; `selftest/index.yml` line `  - remove-dev-weapons`. ABSENT (the old pool): `cascade.yml` line `      rare: [ember_staff, emberblade, locust, brawlers_gauntlet, fletchers_quiver, sages_scroll]`. **PR B:** PRESENT `selftest/GATE-remove-dev-weapons.yml` line `HS1:`; `paper/selftest/SelfTestDrivers.class` carrying `STAGE swapped `; `paper/menu/InventoryCraft.class` carrying `CRAFT minted player=` | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Line($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Entry($name) { if ($zip.GetEntry($name) -eq $null) { "ABSENT  entry $name" } else { "PRESENT entry $name" } }
foreach ($w in @('ironblade', 'emberblade', 'hunters_bow', 'quiver_stone', 'iron_sword')) { Entry ('content/weapons/' + $w + '.yml') }
Line (Bytes 'content/weapons/iron_sword.yml') 'attack_damage: 23' 'iron_sword.yml'
$c = Bytes 'content/loot/cascade.yml'
Line $c '      rare: [ember_staff, locust, brawlers_gauntlet, fletchers_quiver, sages_scroll]' 'cascade.yml'
Line $c '      rare: [ember_staff, emberblade, locust, brawlers_gauntlet, fletchers_quiver, sages_scroll]' 'cascade.yml'
Line (Bytes 'selftest/GATE-remove-dev-weapons.yml') 'DW1:' 'GATE-remove-dev-weapons.yml'
Line (Bytes 'selftest/index.yml') '  - remove-dev-weapons' 'index.yml'
Line (Bytes 'selftest/GATE-remove-dev-weapons.yml') 'HS1:' 'GATE-remove-dev-weapons.yml'
$p = 'io/github/butterflysmp/rpg/'
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
Has ($p + 'paper/selftest/SelfTestDrivers.class') 'STAGE swapped '
Has ($p + 'paper/menu/InventoryCraft.class') 'CRAFT minted player='
$zip.Dispose()
```

### R0c — every Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| Exactly one of each, up to the first `joined the game`: **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 19 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`** (**weapons 23 -> 19**: 23 at the last boot, `GATE-sword-convert.md` R0c, read AS PREDICTED 2026-11-20 at `90ea1517`; every other figure unchanged); **`GATE-fight-revive.md`'s R0c fight line, word for word**; **`[Rpg] Loaded world damage: void takes 0.10 of a player's maximum health per void tick, defence bypassed; mobs unchanged`**; **`GATE-loot-roll.md`'s R0c loot line, word for word** (its tiers and its one source are unchanged; the pool is not printed); **`[Rpg] Loaded smithing: 1 upgrades [diamond_sword + netherite_upgrade_smithing_template + netherite_ingot -> netherite_sword]`**; **`[Rpg] Mint-on-craft: 36 indexed, 36 claiming, of 49 gear definitions`** (36, 36, 53 at the last boot; −4 weapons, none of which claimed); **`[Rpg] Convert-on-arrival: 7 materials {copper_sword=copper_sword, diamond_sword=diamond_sword, golden_sword=golden_sword, iron_sword=iron_sword, netherite_sword=netherite_sword, stone_sword=stone_sword, wooden_sword=wooden_sword}`**. **No `Refusing`** (in particular no `Refusing loot 'cascade'`, which an unknown pool item would print), no `Skipping`, `SEVERE` or `Exception`. **BEFORE his login: no `[Rpg]` WARN beyond `volley_stone`'s known pair.** No `No fights loaded`. **AFTER his login:** zero to four `[Rpg] Refresh: item carries unknown gear id '<id>' -- left untouched; its content file is missing or renamed` lines, `<id>` one of the four, each at most once; DW1 says which | `Select-String run\logs\latest.log -Pattern 'Loaded\|Mint-on-craft\|Convert-on-arrival\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded'`, each line's time beside the first `joined the game` | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, <N> scenarios across 34 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive, fight-site, fight-arena, fight-dragon, fight-rules, fight-end, fight-down, fight-revive, fight-template, loot-roll, sword-tiers, sword-smithing, sword-convert, remove-dev-weapons]`**, no refusal after it. `<N>` is the stack top's count: **251** on PR B (PR A alone: 250). **The control:** the last boot's `250 scenarios across 33 gates` is ABSENT. **The scenario count alone cannot tell PR A from the last boot** (two rows retired, two added), so the gate count and the list are what bind it | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (2026-11-21, PR A's tree, from `index.yml` and every gate file it names; `SelfTestScenariosTest` asserts
250 on PR A):
- the gates: **34**;
- `^[A-Za-z0-9_-]+:` over them: **284**;
- `^setup:`: **34**;
- so **250**.

**Control:** 283 (the last boot) − STA5 − DH6 (retired) + this gate's 3 (setup, DW1, DW2) = 284.

**RECOUNTED AGAIN on PR B's tree** (2026-11-21, the same three commands; `SelfTestScenariosTest` asserts 251): 34 gates,
**285** keys, 34 setups, so **251**. Control: 284 + HS1 = 285. **This is the figure R0d reads**, since the boot is of
the stack top.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, and no `deleting` and no `orphan` line | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | |

## THE ROWS (`/rpg selftest remove-dev-weapons`) — SURVIVAL

### DW1 — a copy he already holds: probed, and warned about once (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Four lines, `INVENTORY ironblade slots=[…]`, `INVENTORY emberblade slots=[…]`, `INVENTORY hunters_bow slots=[…]`, `INVENTORY quiver_stone slots=[…]`. **Which slots is not predicted: it is his saved inventory.** **For every id whose list is NOT empty, exactly one login `Refresh: item carries unknown gear id '<id>' …` WARN; for every id whose list IS empty, none.** If all four lists are empty, the Dangling arm is UNREAD on a server and the row says so | `GearRefresher.refresh` and `probeInventory` read the same `getContents()` (above); `warnOnce` | |

### DW2 — none of the four can be given; iron_sword can (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| For each of `ironblade`, `emberblade`, `hunters_bow`, `quiver_stone`, in that order: REPLY **`Unknown weapon, shield, armor, tool or accessory: <id>`**, then REPLY **`Available: …`** naming none of the four and naming `iron_sword`. Then `INVENTORY iron_sword slots=<A>`; REPLY **`Given Iron Sword`**; no ARRIVAL line (it is minted); `INVENTORY iron_sword slots=<B>`, **B is A plus one slot** | `RpgCommand.give` (above) | |

### HS1 — a full hotbar: the weapon is swapped in, nothing dropped (SURVIVAL) — PR B

| prediction | stands on | READING |
|---|---|---|
| (1) `INVENTORY boltor slots=<B0>`. **If `<B0>` names a hotbar slot (0-8), the row is VOID**: `hold` needs no staging and the swap is unread. (2) The shovels: the `give` REPLY. (3) REPLY **`Given Boltor`**, or **`Your inventory is full -- make room and try again.`**, in which case the hold reports `no hotbar slot holds boltor`, and the row reads that and nothing else. (4) `INVENTORY boltor slots=<B1>`: one more slot than `<B0>`, the new one **9 or above** (the hotbar was full). (5) Exactly one **`STAGE swapped boltor from slot <f> with hotbar <t>, which held <x> (now in slot <f>)`**: `<f>` the new slot from (4); `<t>` the first hotbar slot that does not hold the Ability Stone; `<x>` neither `stone` nor `empty`. **If a hotbar slot was still empty after the shovels, the line is `STAGE moved boltor from slot <f> to hotbar <t>` instead, and the SWAP arm is UNREAD.** Then `DRIVE hold … -><t> item=boltor`. (6) `INVENTORY boltor slots=<B2>` includes `<t>`. **No item is dropped**: no item entity appears (not probed; the code has no drop path, `SelfTestHotbarStagingTest`). At the run's end, `CLEAN … inventory=restored` | `SelfTestStaging.hotbarPlan` (`SelfTestHotbarStagingTest`); `SelfTestDrivers.hold`; the give's fill order (INFERRED) | |

### CR1 — Ben crafts one iron sword: the craft's log line (SURVIVAL, MANUAL) — PR B; witnesses the carried STM1

| prediction | stands on | READING |
|---|---|---|
| When he takes the crafted sword, exactly one **`[Rpg] CRAFT minted player=BaronVonYeetus weapon=iron_sword material=iron_sword score=<T>`**, word for word, where `<T>` is the Gear Score on the sword's tooltip (the one field set by his gear, banded on his average). No `ARRIVAL` line for it (it is minted at the table, so it arrives tagged). **No PASS or FAIL: the line is read beside his tooltip** | `CraftMintLineTest` (the text, word for word; the wiring after the stamp) | |

## WHAT THIS GATE DOES NOT READ

- **The 13 repointed rows and SCA4** (iron_sword and wooden_sword in place of the dev weapons): NOT RE-READ. Each
  gate's appended note says so, and a re-run needs a re-prediction under the next suffix.
- **What a held deleted weapon does when swung or fired.** Read from our source only (`PLAN-weapon-tiers.md` section
  3's status note); no row swings or fires one.
- **The `INPUTS min` re-take** that `NEXT.md` asks for before `quiver_stone` or `hunters_bow` is deleted: it cannot be
  taken on this boot, because the weapons are gone from it. Flagged to the seat.
