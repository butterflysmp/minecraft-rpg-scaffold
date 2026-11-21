# GATE — THE CRYSTAL SHARD STAFF (feat/crystal-shard-staff), THE EMBER STAFF DELETED, AND THE OWED INPUTS MIN READING

**Seat approval 2026-11-21: code and gate diffed at 4d275653, booted at 4d275653, readings at 1f501ae8. Merge on Ben's
words.** Ben, 2026-11-21: "go ahead and merge".

**Status: READ, 2026-11-21, booted at `4d275653`.** The readings are under **READINGS**, at the foot; nothing above
them is edited, and the status line below is the one written before the boot. **CS2 and CS3 read `sent=65.000`
against a predicted 16 (the prediction left out his level bonus); HF1 has NO WITNESS** (see READINGS).

**Status: NOT RUN.** Written 2026-11-21 on `feat/crystal-shard-staff` (PR A), on master `e2ae5f6f` (the squash of
#225), before any boot. **This file carries the ONE R0 and the ONE run sheet for the stack**: PR A (this branch) and
PR B, `ember_staff` deleted, stacked on it, which appends its own rows here before the boot.

**NO BOOT until the seat has diffed the code AND this gate from origin, and Ben says "ready" in his own words**; then
only CC boots, by `.claude/rules/booting.md`. **NO MERGE without Ben's own words.**

**Game mode:** SURVIVAL, every row. **Difficulty:** whatever the world holds; the zombies are NoAI and frozen, and no
row reads a hit on him. **Time:** the selftest sets night (18000), so no zombie burns (`.claude/rules/verification.md`,
the daylight rule). **Nothing in the selftest can hurt him; CR2 and HF1 are manual rows in ordinary survival play, where natural mobs can** (the arithmetic, below, amended 2026-11-21 by the seat's follow-up c).

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

## WHAT PR B ADDS (`feat/ember-staff-deleted`, stacked on PR A; appended before any boot)

- **Deletes** `content/weapons/ember_staff.yml`, by the four dev weapons' rulings (`GATE-remove-dev-weapons.md`,
  `standing-decisions.md`). Weapons 20 -> 19. **The loot table:** `ember_staff` leaves `cascade.yml`'s dev source rare
  pool, which now holds four: `locust, brawlers_gauntlet, fletchers_quiver, sages_scroll`.
- **No Java behaviour changes.** Every surviving mention in main code and content is marked `(deleted 2026-11)`: 29
  sites in 24 files; in main Java, 20 lines at the base, 20 comments, 20 marked. Two javadoc texts the marker broke
  were rewritten by hand (`LootLoader`'s YAML example, `WeaponItems`' quoted command).
- **Nothing lost its last shipped user** (READ, content grep): its `type: burst` is authored by six abilities and
  `searing_lance`; its `solar_detonation` by six; a projectile with no body and no trail still ships in
  `solar_grenade` (that last one by a 14-line window after each `type: projectile`, INFERRED complete).
- **The selftest:** no scenario named `ember_staff` before this branch. **ES1** below names it on purpose, and
  `SelfTestWeaponIdsTest` pins its three steps as the guard's second exception.
- `CLAUDE.md` and `standing-decisions.md` name it with the four, with Ben's words; the pointer also names the two
  weapons that ship off the 4-tick grid (`cursed_emerald` 30, this staff 14).

### THE COVERAGE LEDGER: every test that loaded the real `ember_staff.yml`, and where its rule went

| test | the rule it carried | now | mutation (run on PR B) |
|---|---|---|---|
| `WeaponLoaderTest.bundledEmberStaffContentLoads` | a shipped staff is a costed (mana) right-click projectile on a mage weapon | **repointed** to `flint_staff`, renamed `bundledFlintStaffContentLoads` | MB1 |
| `WeaponLoaderTest.onlyTheBowsRightClickIsABasicAttack` | a staff's right-click is NOT a basic attack, so a refused press keeps its feedback | **repointed** to `flint_staff` | MB2 |
| `WeaponLoreTest.everyShippedWeaponRendersAgainstTheShippedElements` | the one LITERAL-damage weapon of the three: no class-labelled damage line and no Attack Speed without a basic attack; an element colour unlike its rarity's | **repointed** to `flint_staff` (fire, common) | MB3 |
| `LootLoaderTest.theShippedDevExampleNamesRealItemsOfTheirOwnRarityAndNoEpic` | the rare pool, as written | **updated**: four entries, and `ember_staff` named as banned | MB4 |
| `ScorchContentInvariantTest.theFireDamageSitesAreALLDISCOVERED` | the discovered count of fire damage sites | **18 -> 17** (its burst's fire damage); the deletion run reddened with "expected 18 … found 17" | MB5 |
| `GoldenLoreTest` (`golden-lore.txt`) | every shipped tooltip | its block removed: predicted **1 / 11**, read **1 / 11** | the deletion run |

**In-memory fixtures that only use the name are left** (`DurabilityTest`, `EffectApplierTest`, `ProjectileFlightTest`,
`DamageEnchantsTest`, `AbilityLoaderTest`, `WeaponItemsTest`): they build their own definitions and load no file.

## THE SEAT PLACEHOLDERS, FOR BEN (each marked `SEAT PLACEHOLDER 2026-11-21` in the file)

| placeholder | value | why |
|---|---|---|
| ability name | **"Crystal Shard"** | the old "Crystal Volley" fired ONE shard; Ben has been asked |
| speed | **3.0** blocks a tick | "fast hitting"; the old one was 1.1. It cannot pass through a mob between ticks (READ, below) |
| gravity | **0** | the old shard was gravity-less |
| lifetime | **7** ticks | the old range was 20 blocks; there is no range key, and 7 x 3.0 = 21 is the first lifetime that reaches 20 |
| homing | **lerp 0.45, activation_blocks 0, search_radius 18** | the old radius, mapped (below); activation 0 because the old shard steered from its first tick. **The turn is the seat's, 2026-11-21: at speed 3.0 the old 0.18 caught 3.0 degrees at 10 blocks; 0.45 catches 9.5, about the old staff's 9. Ben has been asked** |
| description | "Fires a homing crystal shard" / "at the nearest enemy." | the old tooltip's two lines, reworded |
| flavour | **none** | the old weapon had no flavour line |
| visuals | `crystal_cast`, `crystal_trail`, `crystal_impact` | the old sounds and particles, unchanged in value |
| **OPEN FOR BEN** (the seat's ruling 4, 2026-11-21) | the shard chases ANY non-player mob it can see, while the tooltip says "nearest enemy" | left as built, as `dragons_plume` does; Ben has been told |

## WHAT WAS READ, EACH MARKED

- **No pass-through at 3.0** (READ, our source): `ProjectileFlight.step` traces the segment each tick actually travelled
  (`castRay(position, position + velocity)`), and `PaperCombatWorld.castRay` is one `World#rayTrace` over that
  segment's whole length, blocks and entities together. **That Paper's `rayTrace` tests entities along the full length
  is its documented contract, INFERRED, not read from the jar.**
- **The homing fields** (READ, `CastSpec.Homing` and `ProjectileFlight.steer`): `lerp` blends the direction that far
  toward the target each tick, then keeps the bolt's current speed; `activation_blocks` is how far from the spawn point
  it flies straight first; `search_radius` is a sphere, re-searched every tick. **The old blend** (cfde822,
  `launchCrystalShardBolt`): `velocity x 0.82 + toTarget x 0.18`, both at 1.1, which is the same direction (the shipped turn is now 0.45, the seat's). **The old
  search** was a box of 18 that kept only mobs nearer than 18, so a sphere of 18. **Two differences kept** (READ): ours
  chases ANY non-player mob it can SEE (`nearestMob`: no hostile filter, a sight trace from the bolt), and aims at the
  mob's FEET (`position()`); the old one chased hostiles only, without sight, at the mob's middle.
- **The catch** (`CrystalShardCatchAngleTest`, core, in the fake world's geometry: a sphere of 0.3 around the mob's
  feet, level with the aim): **at turn 0.45, at 10 blocks the shard hits up to 9.5 degrees off the aim line, and misses at 10.0**
  (re-taken 2026-11-21 after the seat raised the turn; no hit beyond that edge out to 90). **An unsteered shard hits to
  1.5. The old turn 0.18 at this speed hit to 3.0; the old speed (1.1) with 0.18 hit to 9.0.** At speed 3.0, a turn of
  0.30 hits to 4.5, 0.65 to 37.5. **REPORTED, NOT A PASS OR FAIL.** On a server the shard leaves the eye, steers
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
1a. `/time set 1000` — the line straight after it (morning: the seat's follow-up c, 2026-11-21).
2. `/give BaronVonYeetus minecraft:crafting_table`, then `/give BaronVonYeetus minecraft:amethyst_shard 1`, then
   `/give BaronVonYeetus minecraft:cobblestone 1`, then `/give BaronVonYeetus minecraft:stick 1`.
3. **CR2 (FIRST, the carried tooltip half):** place the crafting table and craft ONE Crystal Shard Staff (amethyst
   shard on top, cobblestone in the middle, stick at the bottom, one column). Take it, hover it, and **type in chat
   the Gear Score its tooltip shows**, e.g. `gear score 100`.
4. **ASK (type the answer in chat):** "Is the staff's name and tooltip what you expect?"
5. `/rpg selftest crystal-shard-staff` — **ONCE.** Five rows, under a minute. Frozen zombies appear in front of you
   and are shot by the staff; they cannot move or hit you. The last row tries to give you the deleted Ember Staff and
   is refused. Your items are put back at the end.
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
| ES1 | where he stands | nothing: a probe and a refused give | 0 | no / no |
| CR2 | where he chooses, open overworld, in SURVIVAL, manual; the time set to 1000 (morning) by the run sheet's line 1a | **natural overworld mobs, as in ordinary play**; his own shards cannot hit him | **not bounded**: ordinary play. A zombie's hit at defence 0 is about **15** (3 x 5, UNVERIFIED vanilla 3), so **7 hits** at 100 (105) and **24** at 350 (360). A creeper at point blank is about **215** (43 x 5, UNVERIFIED) | **at 100: yes** (7 zombie hits, or one close creeper) **/ at 350: yes** (24 zombie hits), **not by one creeper** |
| HF1 | the same; **his mana at ZERO when the burst ends**, so the staff cannot answer a mob until it regenerates (about 1 a second, so 8 seconds a cast) | the same | the same | the same |

**THE MOB SUM, AND WHERE EACH FIGURE COMES FROM (the seat's follow-up c, 2026-11-21):**
- **READ, our source:** a natural hostile mob's hit on a player is priced `MobScaling.attackDamage` = **5 x (gs / 100)
  x the vanilla amount** (`VANILLA_FACTOR 5`, `GS_BASELINE 100`), the vanilla amount being the event's own with
  vanilla's difficulty scaling undone (`traceMobHit`'s javadoc). **Every overworld hostile rolls gs 100**
  (`DimensionGearScore.OVERWORLD`), so in the overworld a hit is **5 x vanilla**. A mob CONVERTED from another keeps
  its score (`MobNameplateManager`), so one carried over from the Nether would be gs 200 and hit 10 x vanilla: not
  expected here.
- **UNVERIFIED, vanilla, not read from the jar:** the vanilla amounts. A zombie's melee 3, so 15 here; a skeleton's
  arrow up to about 4 or 5, so 20 to 25; a creeper's blast at point blank about 43, so about **215**: **the biggest
  single hit an ordinary overworld mob is expected to land**. A warden (30, so 150) lives only in the deep dark. That
  a creeper's blast is priced as mob-sourced at all is INFERRED (`mobSourceOf` reads the damage source's direct and
  causing entity; the creeper is both).
- **At 100 health and defence 0:** one close creeper blast (about 215) puts him down; zombies take **7** hits (7 x 15 =
  105). **At 350:** a creeper blast (about 215) does not; zombies take **24** hits (24 x 15 = 360). His armour lowers
  all of these; the table is at defence 0.
- **Time 1000** is morning: undead under open sky burn, creepers and spiders do not (UNVERIFIED which mobs the burn
  tag holds; the daylight rule's account did not read it). So **CR2 and HF1 carry the risk of ordinary survival play and nothing more.**

**No SELFTEST row puts him down.** No row sets `death_rules`, enters the fight world or deals damage to him. Each
zombie is killed at the start of the next row and at the end of CS4. **CR2 and HF1 can, as ordinary play can.**

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

**RECOUNTED AGAIN on PR B's tree** (2026-11-21, the same three commands; `SelfTestScenariosTest` asserts 256): 35
gates, **291** keys, 35 setups, so **256**. Control: 290 + ES1 = 291. **This is the figure R0d reads**, since the boot
is of the stack top: `256 scenarios across 35 gates`.

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
| **RE-STAGED 2026-11-21 for the turn 0.45 (the seat's follow-up b; NOT RUN, so rewritten before the boot).** Facing due east, level (`tp … -90 0`, no aim step); the zombie at **`~9.96 ~ ~0.87`**, **5 degrees** to the right of the line at 10 blocks (10 cos 5 = 9.962, 10 sin 5 = 0.872): well inside the 9.5 catch and well outside the unsteered 1.5. **Exactly one PLAYERHIT** on it, `element=kinetic`, `sent=16.000 crit=false` or `sent=32.000 crit=true`. **Without homing this is a miss** (0.87 blocks off the line against a 0.3 half-width; `CrystalShardCatchAngleTest.homingTurnsAMissIntoAHit` stages this very angle), so the hit is the homing's | `CrystalShardCatchAngleTest` (hits to 9.5 degrees in the fake at turn 0.45) | |

### CS4 — the control: outside the catch, no hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **RE-STAGED 2026-11-21 for the turn 0.45.** The same staging, the zombie at **`~9.40 ~ ~3.42`**, **20 degrees** to the right (10 cos 20 = 9.397, 10 sin 20 = 3.420): more than twice the 9.5 the core test reports, and the fake finds no hit beyond 9.5 out to 90. **0 PLAYERHIT** in the row; the probe shows the zombie at its full custom health. **If it is hit**, the server's catch is wider than the fake's by at least 10 degrees, and CS3 then proves nothing about homing: both rows are reported together | the same | |

### ES1 — ember_staff is deleted: a held copy probed, the give refused (SURVIVAL) — PR B

| prediction | stands on | READING |
|---|---|---|
| `INVENTORY ember_staff slots=<A>`. **If `<A>` is not empty, exactly one login WARN `[Rpg] Refresh: item carries unknown gear id 'ember_staff' -- left untouched; its content file is missing or renamed`** (R0c); if empty, none, and the Dangling arm is UNREAD on a server again. Then REPLY **`Unknown weapon, shield, armor, tool or accessory: ember_staff`**, then REPLY **`Available: …`** naming `crystal_shard_staff` and `flint_staff` and NOT `ember_staff`. Then `INVENTORY ember_staff slots=<A>` again, the same list: nothing was given | `RpgCommand.give`, `GearRefresher` and `warnOnce` (READ in `GATE-remove-dev-weapons.md`, unchanged since) | |

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

## READINGS — 2026-11-21, THE BOOT OF `4d275653` (ADDED AFTER HIS `/stop`; NOTHING ABOVE IS EDITED)

Instrument for every line: `run/logs/latest.log` of this boot, read after the stop. Its first `[Rpg] Build:` is
`4d275653`, and it holds exactly one `Done (` line, this boot's own (16:16:46). **Every row below is read from lines
AFTER his login at 16:17:44.** L-numbers are lines of that log.

### The boot, by `.claude/rules/booting.md`

1. **Words:** the seat's BOOT APPROVED for `4d275653`, and Ben's own "ready", both in hand.
2. **Heads:** `git ls-remote --heads origin feat/ember-staff-deleted` printed
   `4d2756532796ebaae0f28f676863768dc93e82f8`. Checks before the boot: #226 at `2655ca81` CLEAN, `build COMPLETED
   SUCCESS` twice; #227 at `4d275653` CLEAN, `build COMPLETED SUCCESS` twice.
3. **Carried state:** no java process; backup `../rpg-run-backups/2026-11-21-pre-crystal-staff` (Rpg 140 files / 26
   dirs, world 193 files / 32 dirs, each equal to its source, `diff -rq lines: 0` each); `world/dimensions` =
   `minecraft rpgfight`; overworld files 13 (`run/world` without `dimensions`); `12:  dev-pack-on-join: true`;
   `rpgfight` 0 entries.
4. **"booting now"** sent in its own message, with "Ben: do NOT log in until I tell you R0 passed".
5. **The boot:** `./scripts/dev-server.sh --refresh-content`, in the next turn. Deployed lines: `weapons/
   crystal_shard_staff.yml` line 40 `    cooldown_ticks: 14`; `recipes/` 4 files; no `weapons/ember_staff.yml`; 19
   weapon files; `rare: [locust, brawlers_gauntlet, fletchers_quiver, sages_scroll]`; `lerp: 0.45`.
6. **R0 read on this boot's log** (below).
7. **"R0 passed"** sent in its own message before he logged in.
8. **His login:** `16:17:44 BaronVonYeetus joined the game`, after every R0 line (the last at 16:16:46) and after the
   R0-passed message.
9. **After his `/stop` (16:22:16):** `rpgfight` 0 entries; overworld files 13, equal to step 3's;
   `16:22:17 … All dimensions are saved`; no `java.exe` left.

### Every command he issued, in order, against the sheet

| time | command or chat line | sheet |
|---|---|---|
| 16:17:49 | `/gamemode creative` | **NOT ON THE SHEET**, before line 1 |
| 16:17:56 | `/gamemode survival` | line 1 |
| 16:18:01 | `/time set 1000` | line 1a |
| 16:18:05 | `/give BaronVonYeetus minecraft:crafting_table` | line 2 |
| 16:18:11 | chat: `minecraft:amethyst_shard 1, then minecraft:cobblestone 1` | line 2's text typed into chat, not a command |
| 16:18:15 | chat: `minecraft:stick 1` | the same |
| 16:18:33 | `/give BaronVonYeetus minecraft:amethyst_shard 1` | line 2 |
| 16:18:38 | `/give BaronVonYeetus minecraft:stick 1` | line 2 |
| 16:18:47 | `/give BaronVonYeetus minecraft:cobblestone 1` | line 2 |
| 16:19:28 | chat: `name and lore and tool tips look good` | line 4, the first ASK |
| 16:19:36 | `/rpg selftest crystal-shard-staff` | line 5, once |
| 16:19:55 | chat: `yes` | line 6, the second ASK |
| 16:20:13 | `/rpg mana refill` | line 7 |
| 16:20:17 | `/rpg firerate` | line 7 |
| 16:20:38 | `/rpg firerate` | line 7 |
| 16:21:12 | chat: `i'm not typing the lines in chat just read them` | line 7's last bullet |
| 16:21:49 | chat: `these self tests have lost a lot of their automatic components since the handoff` | not a sheet line |
| 16:22:16 | `/stop` | line 9 |

**Skipped:** line 3's "type the Gear Score" (no chat line names one) and line 8, the third ASK (no answer).
**Extra:** `/gamemode creative` at 16:17:49, seven seconds before `/gamemode survival`. Every row below ran after
16:17:56, in survival (the selftest's RESTORED line reads `gamemode=SURVIVAL`).

### R0a

**AS PREDICTED.** `[16:16:45] [Rpg] Build: 4d275653`. Not `-dirty`, not `unknown`.

### R0b

**AS PREDICTED: all thirteen lines.** PRESENT: the staff, its recipe and its three visuals; `    cooldown_ticks: 14`;
`mints: crystal_shard_staff`; `CS4:`; `  - crystal-shard-staff`; the four-entry rare pool. ABSENT: `ember_staff.yml`
and the old five-entry pool. The control `flint_staff.yml`: PRESENT.

### R0c

**AS PREDICTED.**
- **`[Rpg] Loaded 11 abilities, 33 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 19
  weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 4 recipes, 1 ores, 4 vein sizes`**.
- The fight line and the loot line are each EQUAL, by scripted string comparison, to `GATE-fight-revive.md`'s and
  `GATE-loot-roll.md`'s R0c lines (the loot gate's full 275-character line; its other match is a wrapped fragment).
- The world-damage line; `Loaded smithing: 1 upgrades […]`; **`Mint-on-craft: 36 indexed, 36 claiming, of 49 gear
  definitions`**; `Convert-on-arrival: 7 materials {…}`. Each exactly once.
- 0 lines of `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Recipe '` or `No fights loaded`.
- `[Rpg]` WARN: 2, `volley_stone`'s pair, both at 16:16:46, before his login. **The 14-tick cooldown logged nothing.**
- **After his login: 0 `[Rpg]` WARN lines, and 0 `unknown gear id` lines** (ES1 explains why).
- The six `***` WARN lines are Paper's version notice, not ours.

### R0d

**AS PREDICTED.** Once: `[Rpg] Selftest: dev server, 256 scenarios across 35 gates [melee-cell, … remove-dev-weapons,
crystal-shard-staff]`. `251 scenarios across 34 gates`: ABSENT (0).

### R0e

**AS PREDICTED.** One `FIGHTWORLD sweep path=…\run\world\dimensions\rpgfight folders=0`; no `deleting`, no `orphan`.

### CR2

**THE LOG HALF AS PREDICTED; THE TOOLTIP HALF UNREAD.** At 16:18:52, after his three gives, exactly one
**`[Rpg] CRAFT minted player=BaronVonYeetus weapon=crystal_shard_staff material=amethyst_shard score=100`**. No
`ARRIVAL` line in the log (0). **He typed no Gear Score**, so the equality with his tooltip is UNREAD. **CR1's carried
tooltip half is still UNREAD** after two boots. The craft itself, a recipe mint through the table, is witnessed.

### CS1

**AS PREDICTED in every lore line; the name's style NOT PREDICTED.**
- REPLY `"Given Crystal Shard Staff" [aqua, white]`; `INVENTORY crystal_shard_staff slots=[0x1, 1x1]` (slot 0: the
  staff he crafted; slot 1: the one given); `DRIVE hold … 0->0`; REPLY `"Set Gear Score: 100"`.
- `HELD crystal_shard_staff AMETHYST_SHARD x1 name="Crystal Shard Staff" [white] italic lore=["Kinetic" [white];
  "Gear Score: 100" [gray, aqua]; "" []; "Crystal Shard  Right-Click" [gold, yellow]; "Fires a homing crystal shard"
  [gray]; "at the nearest enemy." [gray]; "Kinetic Damage: 16" [gray, red]; "Cooldown: 0.7s | Mana Cost: 8"
  [dark_gray]; "" []; "Common Magic Weapon" [white]]`. **All ten lines, in order, as predicted.**
- **NOT PREDICTED: `italic` on the name.** The dump prints it only when the name component says ITALIC explicitly
  (`SelfTestLines.styled`, READ), and it reads `effectiveName()`. Why the name carries it is UNVERIFIED (INFERRED: Paper
  styles a custom name as vanilla does). **Ben's answer to the first ASK: "name and lore and tool tips look good".**

### CS2

**NOT AS PREDICTED IN `sent`: 65.000, NOT 16.000 (OR 32.000 ON A CRIT). THE PREDICTION WAS WRONG, NOT THE CODE.**
- `MOBSEED b91dd947… zombie gs=100 source=rolled max=100`; the aim `d=8.000 yaw=-90.000 pitch=-0.859`; `DRIVE use …
  item=crystal_shard_staff`; then exactly one
  **`PLAYERHIT caster=b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 source=crystal_shard_staff/right_click target=b91dd947-4178-4283-a62a-d27edea4c706 zombie element=kinetic sent=65.000 crit=false triggerScore=100`**.
  `element=kinetic`, `crit=false` and `triggerScore=100` are as predicted. The probe read `customHP=35.000/100.000`
  (context only).
- **Why 65** (READ, `EffectApplier`'s Damage arm): `sent = 16 x score/100 x (enchant multiplier) + classDamageBonus +
  weaponLevelDamage`, and `Caster.weaponLevelDamage` is the player's level bonus on every weapon hit. **Executed**,
  with the real classes (`core/target/classes`, jshell): `LevelBonus.forLifetimeXp(712667)` = **level 50,
  weaponDamage 49.0** (his profile's `lifetimeXp` is 712667; control: xp 0 gives 0.0). **16 + 49 = 65 exactly**, so
  no enchant multiplier was active and the class bonus was 0. **The gate predicted 16 x his score / 100 and left out
  the flat level term.** (`GATE-withered-shortbow.md` WB2 read 16 on 2026-11-14 at an earlier level; not re-derived
  here.)
- **Mana:** REPLY `"Mana refilled to 100."` before the cast; the fall by 8 is READ from source only (no probe).

### CS3

**THE HIT AS PREDICTED; `sent` NOT, FOR CS2's REASON.** Teleported to `-111.5 63 23.5` facing `-90 0`; the zombie
summoned at `~9.96 ~ ~0.87` (`MOBSEED 74fb90a3… gs=100`); `DRIVE use … yaw=-90.000 pitch=0.000`; then exactly one
**`PLAYERHIT caster=b6ae27e9-… source=crystal_shard_staff/right_click target=74fb90a3-1f02-4871-9eea-1b93d01ac45d zombie element=kinetic sent=65.000 crit=false triggerScore=100`**.
The probe read `d=9.998 dy=0.000`, `customHP=35.000/100.000`. **A shard fired level along the line hit a zombie 0.87
blocks off it at 10 blocks**, which an unsteered shard misses (`CrystalShardCatchAngleTest`). **The homing is
witnessed on a server.** Ben's answer to the second ASK ("did the shard find the zombie it was roughly aimed at?"):
**"yes"**.

### CS4

**AS PREDICTED.** The zombie at `~9.40 ~ ~3.42` (`MOBSEED 595f7bd2… gs=100`); `DRIVE use … yaw=-90.000
pitch=0.000`; **0 PLAYERHIT in the row**; the probe read `customHP=100.000/100.000`, `d=10.003`. Then `Killed Zombie`.
So the catch on the server lies between CS3's 5 degrees (hit) and CS4's 20 (miss), as the fake's 9.5 does.

### ES1

**AS PREDICTED, ON THE EMPTY ARM.** `INVENTORY ember_staff slots=[]`; REPLY `"Unknown weapon, shield, armor, tool or
accessory: ember_staff" [red]`; REPLY `"Available: …"`: by grep on the full line, `crystal_shard_staff` 1,
`flint_staff` 1, `ember_staff` 0; then `INVENTORY ember_staff slots=[]` again. No `unknown gear id` WARN anywhere (0),
which an empty list predicts. **He held no ember_staff, so the Dangling arm is UNREAD on a server again.**

The run ended `SELFTEST RUN FINISHED`, then `SELFTEST RUN RESTORED … gamemode=SURVIVAL … spawn_mobs=false time=1000
…`: his time and game mode were put back.

### HF1 — THE OWED READING: NO WITNESS

**UNREAD.** He ran `/rpg mana refill` (16:20:13), `/rpg firerate` (16:20:17) and, after the burst, `/rpg firerate`
(16:20:38). **The command's replies are NOT in the log**: `Fire cadence`, `INPUTS` and `FIRES` each occur 0 times
in `latest.log`. `RpgCommand.fireRate` sends them to the player with `sendMessage` (READ), and only the selftest's
reply reader logs replies. **He did not type them**: at 16:21:12, verbatim, "i'm not typing the lines in chat just
read them". No screenshot was received. **So the `INPUTS min` reading is NOT TAKEN, and the table's verdict cannot be
applied: `ceil(n/4) x 4` remains an UNTESTED EMPIRICAL FIT.** No shard cast is logged either (no mob was hit, so no
PLAYERHIT), so the server holds nothing on the held burst at all.

**The third ASK** ("Did holding right-click feel like 0.7 seconds between shots, or slower?"): **no answer, NOT
STATED.**

### BEN'S REMARK, VERBATIM, FOR THE SEAT

At 16:21:49: **"these self tests have lost a lot of their automatic components since the handoff"**. Recorded here,
not answered here.

### UNREAD ARMS, IN ONE PLACE

- **HF1**: the INPUTS min and FIRES min readings (the reply is not logged; not typed).
- **CR2's tooltip half** (CR1's, carried for a second boot): no Gear Score typed.
- **The third ASK.**
- **ES1's Dangling arm**: he held no `ember_staff`.
- **Unbreaking granting nothing**, and the shard chasing a non-hostile or walled mob: source only, as stated above.
