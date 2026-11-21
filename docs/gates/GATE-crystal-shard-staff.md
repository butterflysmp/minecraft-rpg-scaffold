# GATE — THE CRYSTAL SHARD STAFF (feat/crystal-shard-staff), THE EMBER STAFF DELETED, AND THE OWED INPUTS MIN READING

**Status: NOT RUN.** Written 2026-11-21 on `feat/crystal-shard-staff` (PR A), on master `e2ae5f6f` (the squash of
#225), before any boot. **This file carries the ONE R0 and the ONE run sheet for the stack**: PR A (this branch) and
PR B, `ember_staff` deleted, stacked on it, which appends its own rows here before the boot.

**NO BOOT until the seat has diffed the code AND this gate from origin, and Ben says "ready" in his own words**; then
only CC boots, by `.claude/rules/booting.md`. **NO MERGE without Ben's own words.**

**Game mode:** SURVIVAL, every row. **Difficulty:** whatever the world holds; the zombies are NoAI and frozen, and no
row reads a hit on him. **Time:** the selftest sets night (18000), so no zombie burns (`.claude/rules/verification.md`,
the daylight rule). **Nothing on this boot can hurt him** (the arithmetic, below).

**Ben's words, verbatim (2026-11-21, spelling as typed):**
- "First a side note: The Ember staff is also a dev weapon and should be slated for deletion."
- "Let's finish fleshing out the Mage progression tree. The wood tier weapon is already there, the flint staff, so we
  need a stone tier weapon now. The Lapis Staff and Emerald Beam are Diamond/Netherite tier weapons."
- "Let's grab the Crystal shard staff from the old repo and rework it a bit."
- "The old version was a slow traveling homing shot. I want this one to be fast hitting. keep the homing but not too
  much damage. Say 16 to start. Kinetic damage type keep the 8 mana cost with a 0.7s cooldown"
- "Keep the item and recipe delete the ember staff this pass"

## WHAT THE BRANCH DOES (PR A)

- **The seat's correction, first** (`3f031d30`): the 2026-11-21 ruling's reason (a), *"every shipped held right-click
  cooldown is a multiple of 4"*, is withdrawn as false (`cursed_emerald` authors 30), appended to `docs/NEXT.md` and
  `.claude/rules/standing-decisions.md` with every shipped weapon trigger cooldown listed by command. **The `INPUTS min`
  reading is OWED on this boot**, on this staff: row **HF1**.
- **A new weapon, content only**: `weapons/crystal_shard_staff.yml`, `recipes/crystal_shard_staff.yml`,
  `visuals/crystal_cast.yml`, `crystal_trail.yml`, `crystal_impact.yml`. **No Java.** Weapons 19 -> 20, recipes 3 -> 4,
  visuals 30 -> 33.
- **Not in the loot table**: `LootLoaderTest` checks that every listed id ships at its own rarity; it does not require
  every shipped item to be listed (READ, `theShippedDevExampleNamesRealItemsOfTheirOwnRarityAndNoEpic`).
- **Docs**: Ben's words and the mage tiers in `PLAN-weapon-tiers.md` 1.6; the waves note in `PLAN-dragon-fight.md` §7,
  item 18 ("Channelling is when the slow close starts but I don't want to start waves yet"). Waves are NOT started.

## THE SEAT PLACEHOLDERS, FOR BEN (each marked `SEAT PLACEHOLDER 2026-11-21` in the file)

| placeholder | value | why |
|---|---|---|
| ability name | **"Crystal Shard"** | the old "Crystal Volley" fired ONE shard; Ben has been asked |
| speed | **3.0** blocks a tick | "fast hitting"; the old one was 1.1. It cannot pass through a mob between ticks (READ, below) |
| gravity | **0** | the old shard was gravity-less |
| lifetime | **7** ticks | the old range was 20 blocks; there is no range key, and 7 x 3.0 = 21 is the first lifetime that reaches 20 |
| homing | **lerp 0.18, activation_blocks 0, search_radius 18** | the old turn and radius, mapped (below); activation 0 because the old shard steered from its first tick |
| description | "Fires a homing crystal shard" / "at the nearest enemy." | the old tooltip's two lines, reworded |
| flavour | **none** | the old weapon had no flavour line |
| visuals | `crystal_cast`, `crystal_trail`, `crystal_impact` | the old sounds and particles, unchanged in value |

## WHAT WAS READ, EACH MARKED

- **No pass-through at 3.0** (READ, our source): `ProjectileFlight.step` traces the segment each tick actually travelled
  (`castRay(position, position + velocity)`), and `PaperCombatWorld.castRay` is one `World#rayTrace` over that
  segment's whole length, blocks and entities together. **That Paper's `rayTrace` tests entities along the full length
  is its documented contract, INFERRED, not read from the jar.**
- **The homing fields** (READ, `CastSpec.Homing` and `ProjectileFlight.steer`): `lerp` blends the direction that far
  toward the target each tick, then keeps the bolt's current speed; `activation_blocks` is how far from the spawn point
  it flies straight first; `search_radius` is a sphere, re-searched every tick. **The old blend** (cfde822,
  `launchCrystalShardBolt`): `velocity x 0.82 + toTarget x 0.18`, both at 1.1, which is the same direction. **The old
  search** was a box of 18 that kept only mobs nearer than 18, so a sphere of 18. **Two differences kept** (READ): ours
  chases ANY non-player mob it can SEE (`nearestMob`: no hostile filter, a sight trace from the bolt), and aims at the
  mob's FEET (`position()`); the old one chased hostiles only, without sight, at the mob's middle.
- **The catch** (`CrystalShardCatchAngleTest`, core, in the fake world's geometry: a sphere of 0.3 around the mob's
  feet, level with the aim): **at 10 blocks the shard hits up to 3.0 degrees off the aim line, and misses at 3.5. An
  unsteered shard hits to 1.5. The old speed (1.1) with the same turn hits to 9.0.** At speed 3.0, a turn of 0.30 hits
  to 4.5, 0.45 to 9.5, 0.65 to 37.5. **REPORTED, NOT A PASS OR FAIL.** On a server the shard leaves the eye, steers
  toward the feet and meets a 0.6 x 1.95 box: the real edge is INFERRED to be near this one, so CS3 and CS4 stand
  well inside and well outside it.
- **The tooltip for 14 ticks** (READ, `WeaponLoreLines.cooldownLabel`: `"%.1fs"` of ticks / 20, the AUTHORED value):
  **`Cooldown: 0.7s | Mana Cost: 8`**, pinned in `golden-lore.txt` and `aMintedStaffsTooltipReads`. Under held fire
  the untested fit `ceil(n/4) x 4` predicts it fires every 16 (0.8 s), which the tooltip would not say.
- **The loader and 14 ticks** (READ): nothing in main code tests a cooldown against the grid (`git grep -n '% 4'` over
  `core/src/main` and `paper/src/main`: no hit). `WeaponLoader` warns only on unknown keys and scalar lists; the
  floor warning (`AbilityService`'s `minimumCooldownTicks`) is 0 for a projectile. **So no boot line for it.**
- **`/rpg firerate`** (READ, `RpgCommand.fireRate` and `cooldownLine`): it prints and CLEARS. For this staff its third
  line reads **`cooldown_ticks 14 authored, 14 effective (not a basic attack -- attack speed does not scale it)`**: a
  literal `damage` is not a basic attack, so his gear's attack speed cannot move the 14.
- **Mana** (READ): `AbilityService`'s shared tail spends the cost before returning Success, so a cast that reaches its
  PLAYERHIT has spent 8. His base pool is 100 (`RpgPlugin.MAX_MANA`); Mana Bank armour raises it. **No selftest probe
  reads mana**, so "falls by 8" is READ from source, not read on the server.
- **A recipe craft logs the CRAFT line** (READ, `InventoryCraft.commitCraft`): the crafting table is ours; its commit
  asks `claimFor(pin, result)`, which resolves a custom recipe's `mints` (`RecipeProbe.claimedBy`), mints, rolls,
  stamps the score, and for a weapon logs **`CRAFT minted player=<name> weapon=<id> material=<material> score=<T>`**.
- **The enchant pool** (READ, `GearItems.gearClassOf` and `EnchantRoll.poolFor`; pinned by
  `theEnchantPoolIsAttunementAndUnbreaking`): **attunement** (effect damage; it reaches a literal damage, READ in
  `EffectApplier`'s Damage arm) and **unbreaking** (effect durability). **`amethyst_shard` has no durability, so
  Unbreaking rolls and grants nothing**: NAME THE QUANTITY's eligibility half fails, as on the flint staff (a stick).
  Reported, not fixed.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

The booter's checklist is `.claude/rules/booting.md`; this list is its instance for this boot.

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup:** `run/plugins/Rpg` and `run/world` copied, as they are, to a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-crystal-staff`. Report the file and directory counts, `diff -rq lines: 0` for
   each, the `ls` of `world/dimensions` and the overworld's file count. Nothing is restored (Ben's word only).
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND IS SENT:** "booting now", and Ben told **NOT to log in until told R0
   passed.** The boot command goes in the next turn.
6. `./scripts/dev-server.sh --refresh-content`. **The REQUIRED deployed lines** (under `run/plugins/Rpg/content`):
   `weapons/crystal_shard_staff.yml` reads `    cooldown_ticks: 14`; `recipes/` holds 4 `.yml` files.
7. R0a to R0e read on **this boot's** log (its `[Rpg] Build:` names the stack top's head).
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* Report `rpgfight` empty, the overworld's file count against step 2's, `All dimensions are
   saved`, and no `java` process left.

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED. IF YOU ARE TOLD NOTHING, DO NOT LOG IN.**
1. `/gamemode survival` — **alone, the first line after login.**
2. `/give BaronVonYeetus minecraft:crafting_table`, then `/give BaronVonYeetus minecraft:amethyst_shard 1`, then
   `/give BaronVonYeetus minecraft:cobblestone 1`, then `/give BaronVonYeetus minecraft:stick 1`.
3. **CR2 (FIRST, the carried tooltip half):** place the crafting table and craft ONE Crystal Shard Staff (amethyst
   shard on top, cobblestone in the middle, stick at the bottom, one column). Take it, hover it, and **type in chat
   the Gear Score its tooltip shows**, e.g. `gear score 100`.
4. **ASK (type the answer in chat):** "Is the staff's name and tooltip what you expect?"
5. `/rpg selftest crystal-shard-staff` — **ONCE.** Four rows, under a minute. Frozen zombies appear in front of you
   and are shot by the staff; they cannot move or hit you. Your items are put back at the end.
6. **ASK (type the answer in chat):** "In the selftest, did the shard find the zombie it was roughly aimed at?"
7. **HF1, THE HELD-FIRE READING.** Stand somewhere open with no mobs in front of you (the shard chases any mob it can
   see within 18 blocks). Hold the staff you crafted in step 3. Then:
   - `/rpg mana refill`
   - `/rpg firerate` (this clears any old sample; it may say there is none)
   - **hold right-click** until "not enough mana" appears, then let go
   - `/rpg firerate`
   - **type in chat the two lines that start `INPUTS` and `FIRES`, exactly as shown** (or take a screenshot).
8. **ASK (type the answer in chat):** "Did holding right-click feel like 0.7 seconds between shots, or slower?"
9. `/stop`

Nothing else this boot. If a row stalls or aborts, do not intervene. An answer of "not sure" is recorded as NOT STATED.

## THE ARITHMETIC (SELFTEST SAFETY): AT DEFENCE 0, AGAINST 100 AND AGAINST 350, AND WHERE EACH ROW PUTS HIM

| row | where he is | what can hurt him | most hits x biggest hit | puts him down at 100? at 350? |
|---|---|---|---|---|
| CS1 | where he stands, overworld | nothing | 0 | no / no |
| CS2, CS3, CS4 | teleported to the centre of the block he stands on, facing east; one NoAI zombie 8 to 10 blocks east | a NoAI zombie does not move or attack; the shard skips its caster (`castRay`'s ignore id) | 0 x 0 = 0 | no / no |
| CR2, HF1 | where he chooses, overworld | nothing; his own shards cannot hit him | 0 | no / no |

**No row puts him down.** No row sets `death_rules`, enters the fight world or deals damage to him. Each zombie is
killed at the start of the next row and at the end of CS4.

## R0 — THE BUILD, FOR THE WHOLE STACK. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, the stack top's head (PR B), as `git ls-remote origin` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries both changes

| prediction | instrument | READING |
|---|---|---|
| PRESENT: entries `content/weapons/crystal_shard_staff.yml` (line `    cooldown_ticks: 14`), `content/recipes/crystal_shard_staff.yml` (line `mints: crystal_shard_staff`), `content/visuals/crystal_cast.yml`, `crystal_trail.yml`, `crystal_impact.yml`; `selftest/GATE-crystal-shard-staff.yml` line `CS4:`; `selftest/index.yml` line `  - crystal-shard-staff`. **PR B:** ABSENT `content/weapons/ember_staff.yml`; PRESENT `content/loot/cascade.yml` line `      rare: [locust, brawlers_gauntlet, fletchers_quiver, sages_scroll]`, ABSENT the old line `      rare: [ember_staff, locust, brawlers_gauntlet, fletchers_quiver, sages_scroll]` | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Line($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Entry($name) { if ($zip.GetEntry($name) -eq $null) { "ABSENT  entry $name" } else { "PRESENT entry $name" } }
foreach ($e in @('weapons/crystal_shard_staff.yml', 'recipes/crystal_shard_staff.yml', 'visuals/crystal_cast.yml', 'visuals/crystal_trail.yml', 'visuals/crystal_impact.yml', 'weapons/ember_staff.yml', 'weapons/flint_staff.yml')) { Entry ('content/' + $e) }
Line (Bytes 'content/weapons/crystal_shard_staff.yml') '    cooldown_ticks: 14' 'crystal_shard_staff.yml'
Line (Bytes 'content/recipes/crystal_shard_staff.yml') 'mints: crystal_shard_staff' 'recipes/crystal_shard_staff.yml'
Line (Bytes 'selftest/GATE-crystal-shard-staff.yml') 'CS4:' 'GATE-crystal-shard-staff.yml'
Line (Bytes 'selftest/index.yml') '  - crystal-shard-staff' 'index.yml'
$c = Bytes 'content/loot/cascade.yml'
Line $c '      rare: [locust, brawlers_gauntlet, fletchers_quiver, sages_scroll]' 'cascade.yml'
Line $c '      rare: [ember_staff, locust, brawlers_gauntlet, fletchers_quiver, sages_scroll]' 'cascade.yml'
$zip.Dispose()
```

`weapons/flint_staff.yml` PRESENT is the control: the scan can see a weapon entry.

### R0c — every Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| Exactly one of each, up to the first `joined the game`: **`[Rpg] Loaded 11 abilities, 33 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 19 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 4 recipes, 1 ores, 4 vein sizes`** (against `GATE-remove-dev-weapons.md` R0c, read AS PREDICTED at `38e6e62e`: **visuals 30 -> 33**, **recipes 3 -> 4**, **weapons 19 -> 19**, every other figure unchanged). **THE WEAPONS FIGURE CANNOT PROVE EITHER CHANGE: 19 + 1 - 1 = 19**, the same as the last boot. The new recipe count and visuals count prove PR A's files loaded; **R0b's ABSENT `ember_staff.yml` and PRESENT `crystal_shard_staff.yml` are what prove both changes**, and CS1's `Given Crystal Shard Staff` and ES1's refusal read them on the server. **`GATE-fight-revive.md`'s R0c fight line, word for word**; **`[Rpg] Loaded world damage: void takes 0.10 of a player's maximum health per void tick, defence bypassed; mobs unchanged`**; **`GATE-loot-roll.md`'s R0c loot line, word for word** (its tiers and source are unchanged; the pool is not printed); **`[Rpg] Loaded smithing: 1 upgrades [diamond_sword + netherite_upgrade_smithing_template + netherite_ingot -> netherite_sword]`**; **`[Rpg] Mint-on-craft: 36 indexed, 36 claiming, of 49 gear definitions`** (unchanged: neither staff claims a `craft_result`, and the gear definitions are weapons + shields + armor + tools, 19 + 1 + 24 + 5, READ `RpgPlugin`); **`[Rpg] Convert-on-arrival: 7 materials {copper_sword=copper_sword, diamond_sword=diamond_sword, golden_sword=golden_sword, iron_sword=iron_sword, netherite_sword=netherite_sword, stone_sword=stone_sword, wooden_sword=wooden_sword}`**. **No `Refusing`, no `Skipping`, `SEVERE` or `Exception`; no `Recipe '…'` warning. BEFORE his login: no `[Rpg]` WARN beyond `volley_stone`'s known pair.** The 14-tick cooldown logs NOTHING (READ, above), so no line is predicted for it. No `No fights loaded`. **AFTER his login:** zero or one `[Rpg] Refresh: item carries unknown gear id 'ember_staff' -- left untouched; its content file is missing or renamed`, at most once; ES1 says which | `Select-String run\logs\latest.log -Pattern 'Loaded\|Mint-on-craft\|Convert-on-arrival\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded\|Recipe '''`, each line's time beside the first `joined the game` | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, <N> scenarios across 35 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive, fight-site, fight-arena, fight-dragon, fight-rules, fight-end, fight-down, fight-revive, fight-template, loot-roll, sword-tiers, sword-smithing, sword-convert, remove-dev-weapons, crystal-shard-staff]`**, no refusal after it. `<N>` is the stack top's count (PR A alone: **255**; PR B's figure is appended below). **The control:** the last boot's `251 scenarios across 34 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (2026-11-21, PR A's tree, from `index.yml` and every gate file it names; `SelfTestScenariosTest` asserts
255): 35 gates; `^[A-Za-z0-9_-]+:` over them **290**; `^setup:` **35**; so **255**. Control: 285 (the last boot) +
this gate's 5 (setup, CS1-CS4) = 290.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, and no `deleting` and no `orphan` line | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | |

## THE ROWS — SURVIVAL

### CR2 — Ben crafts one staff: the CRAFT line, and the carried tooltip half (SURVIVAL, MANUAL; FIRST on the sheet)

| prediction | stands on | READING |
|---|---|---|
| When he takes the crafted staff, exactly one **`[Rpg] CRAFT minted player=BaronVonYeetus weapon=crystal_shard_staff material=amethyst_shard score=<T>`**, word for word. No `ARRIVAL` line for it. **His chat line names the Gear Score on the tooltip, and it equals `<T>`.** This is CR1's carried tooltip half (`GATE-remove-dev-weapons.md`), read on a crafted item. No PASS or FAIL beyond that equality | `InventoryCraft.commitCraft` (READ, above); `CraftMintLineTest` | |

### CS1 — given, held, and its tooltip (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| REPLY **`Given Crystal Shard Staff`**; `INVENTORY crystal_shard_staff slots=[…]` naming the given slot; `DRIVE hold …` holding it (or a `STAGE` line first, if the hotbar was full); the gearscore REPLY; then the `HELD crystal_shard_staff` dump: **name `"Crystal Shard Staff"` in the common (white) colour**, lore, in order: **`Kinetic`**, **`Gear Score: 100`**, `` (blank), **`Crystal Shard  Right-Click`**, **`Fires a homing crystal shard`**, **`at the nearest enemy.`**, **`Kinetic Damage: 16`**, **`Cooldown: 0.7s | Mana Cost: 8`**, `` (blank), **`Common Magic Weapon`** | `CrystalShardStaffContentTest.aMintedStaffsTooltipReads` (the same ten lines from the real file at score 100) | |

### CS2 — one cast straight ahead: 16 kinetic at GS 100 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The zombie seeded (`MOBSEED`); the aim line; `DRIVE use … item=crystal_shard_staff`; then **exactly one `PLAYERHIT caster=<his uuid> source=crystal_shard_staff/right_click target=<the zombie's uuid> zombie element=kinetic sent=16.000 crit=false triggerScore=100`**, or the same with **`sent=32.000 crit=true`** (a crit, x 2.0; any other `sent` with `crit=true` is a FAIL). Read against the PLAYERHIT line, never against a health figure; the probe's HP is context only. **Mana**: falls by 8 (READ, source only; no probe reads it) | `TracedHit.line`; the staff's file | |

### CS3 — off the aim line, inside the catch: a hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Facing due east, level (`tp … -90 0`, no aim step); the zombie at `~9.99 ~ ~0.44`, 2.5 degrees to the right of the line at 10 blocks. **Exactly one PLAYERHIT** on it, `element=kinetic`, `sent=16.000 crit=false` or `sent=32.000 crit=true`. **Without homing this is a miss** (0.44 blocks off the line against a 0.3 half-width; `CrystalShardCatchAngleTest`), so the hit is the homing's | `CrystalShardCatchAngleTest` (hits to 3.0 degrees in the fake) | |

### CS4 — the control: outside the catch, no hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The same staging, the zombie at `~9.95 ~ ~1.05`, 6 degrees to the right: twice the 3.0 the core test reports. **0 PLAYERHIT** in the row; the probe shows the zombie at its full custom health. **If it is hit**, the server's catch is wider than the fake's by at least 3 degrees, and CS3 then proves nothing about homing: both rows are reported together | the same | |

### HF1 — THE OWED READING: held fire on a 14-tick cooldown (SURVIVAL, MANUAL)

| prediction | stands on | READING |
|---|---|---|
| `/rpg firerate`'s lines for `crystal_shard_staff, right_click`: `INPUTS count … min <i>t`, `FIRES count … min <f>t`, the line **`cooldown_ticks 14 authored, 14 effective (not a basic attack -- attack speed does not scale it)`**, and a verdict line. **By `NEXT.md`'s held-fire table, restated for 14:** **`FIRES min 16`, `INPUTS min 4`: inconclusive** (a clean stream; re-take). **`FIRES min 16`, `INPUTS min 3`: the fit survives** the refutation of its own mechanism. **`FIRES min 14` or `15`: `ceil(n/4) x 4` IS FALSIFIED as stated.** `FIRES min` below 14 would be a cooldown defect (`isReady` is `>=`). **Recorded beside it, verbatim: his answer to the 0.7-seconds question.** **Mana does not end the burst too soon**: at 8 a cast on a 100 pool, plus about 1 a second of regeneration, he gets 12 or 13 fires (100 / 8 = 12.5, plus about 9 regenerated over the burst), 11 or 12 intervals, before "not enough mana", against the Boltor's 8-shot magazine (7 intervals) on which the grid was first read. `/rpg mana refill` immediately before stages it | `RpgCommand.fireRate` (READ); `NEXT.md`, the held-fire entry and its 2026-11-21 correction | |

### THE ASK STEPS (recorded verbatim; "not sure" is NOT STATED)

| ask | his answer | recorded as |
|---|---|---|
| "Is the staff's name and tooltip what you expect?" | | |
| "In the selftest, did the shard find the zombie it was roughly aimed at?" | | |
| "Did holding right-click feel like 0.7 seconds between shots, or slower?" (beside HF1) | | |

## WHAT THIS GATE DOES NOT READ

- **What the shard looks and sounds like** (the trail, the burst, the chime): AUTO readings witness the server, never
  the client. Only his answers speak to it.
- **Unbreaking granting nothing on the staff**: pinned in a test, not read on a server.
- **The shard chasing a mob behind a wall, or a passive mob**: READ from source only.
