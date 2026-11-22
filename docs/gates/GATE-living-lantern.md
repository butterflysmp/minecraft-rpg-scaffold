# GATE — THE LIVING LANTERN (feat/living-lantern), THE CONE, THE OWED FIXES, AND THE OWED READINGS

**Status: NOT RUN.** Written 2026-11-22 on `feat/living-lantern` (PR 3), stacked on `feat/lantern-cone` (PR 2), stacked
on `feat/lantern-owed-fixes` (PR 1), on master `e487ade5` (the squash of #230), before any boot. **This file carries the
ONE R0 and the ONE run sheet for the three.**

**Status, 2026-11-22: BOOTED ONCE at `d98c28bb`; R0 NOT AS PREDICTED (R0b, one line: the gate's instrument); NO ROW RUN.**
Readings in *READINGS, 2026-11-22* at the end of this file. No prediction above was edited.

**Status, 2026-11-22, later: the seat ruled R0b's miss the gate's instrument; R0b is re-predicted as R0bR2** (*THE SEAT'S
RULINGS ON THE FIRST BOOT, AND R0bR2*, at the end). **The next boot reads R0a, R0bR2, R0c, R0d and R0e afresh.**

**Status, 2026-11-22, 14:04: SECOND BOOT at `42ef53c8`, R0 PASSED, ALL 13 ROWS RAN. ONE READING NOT AS PREDICTED: the end
of the run's `health=`, 124 against the predicted 126; the prediction named the wrong instant** (*READINGS, SECOND BOOT*,
at the end).

**Seat approval 2026-11-22: code diffed at d98c28bb, gate diffed at 42ef53c8, booted at 42ef53c8, readings at 85e27e32.
Merge on Ben's words.** Ben's words, verbatim: "good to merge". The seat ruled 124 the right figure (the prediction
compared two instants); HF1 closed as a measurement; LL9's `cap=3.000` lines owed as a source read, not fixed here.

**NO BOOT until the seat has diffed the code AND this gate from origin, and Ben says "ready" in his own words**; then
only CC boots, by `.claude/rules/booting.md`. **NO MERGE without Ben's own words.**

**Game mode:** SURVIVAL, every row. **Every row is AUTO** (`/rpg selftest living-lantern`); **Ben types three lines**,
and his answer to the one question when the selftest asks it. Two gold lines ask him to DO something: CR3 (place a table
and craft one Living Lantern) and HF1R3 (hold right-click). **Time:** the setup sets night (18000) and `spawn_mobs
false`; the run's restore puts both back. **Every mob is a frozen NoAI zombie; no fight world.** The setup kills the
natural hostiles within 64 blocks by named type and stages his level at 1, **which lowers his health for the run; since
PR 1 the run's end puts it back** (*HIS HEALTH*, below).

**Ben's words, verbatim (2026-11-22, spelling as typed),** recorded in `PLAN-weapon-tiers.md` 1.8:
- "I think we'll take the Living Lantern and rework it for the iron tier weapon."
- "1. The rework: I'm thinking of an almost Flamethrower-like weapon. An area of effect arc in front of the player that
  travels for about 6 blocks dealing a decent bit of damage. / 2. Fire element / 3. 12 mana 0.8s second cooldown 14
  damage 60 degree arc / 4. Keep the lantern item, what was the old recipe?"
- "Before I sent this I would like to change the recipe / I = Iron Nugget / F = Fire Charge / I I I / I F I / I I I"
- On `GATE-knell-beam.md`'s HF1R2 (recorded there): "1. Yes I fired the Crystal shard staff by i was spamming right
  click not holding / 2. Yes I Did I thought the test wanted the knell beam not the crystal shard staff"

## WHAT THE THREE PRs DO

**PR 1, `feat/lantern-owed-fixes`:** the HF1R2 note in `GATE-knell-beam.md` (first commit); a fire-cadence sample per
WEAPON (`FireCadence`'s key gains the weapon; `/rpg firerate` prints one reading per weapon; MIXED is gone with its
case); the selftest's own `use:` presses kept out of every sample (`FireCadence.excluding`, and a count printed); the
selftest snapshot holds his HEALTH and puts it back (`SELFTEST RUN HEALTH`); two verbs, **`hold_only: <weapon>`** (every
other hotbar item moved to empty inventory slots, never dropped; refused whole if they do not fit) and
**`probe_held`** (`HELDID slot=<n> item=<id> offhand=<id>`); and two READ notes in `GATE-knell-beam.md` (KB1's yellow,
KB2's tick).

**PR 2, `feat/lantern-cone`:** `type: cone` (`CastSpec.Cone`): `range` (blocks, eye to a body's sight point),
`arc_degrees` (the FULL angle), `travel_ticks` (0 = instant), `front` (a visual drawn each tick as a ring at the front:
its centre and 8 points at the cone's width). The angle test is the melee swing's, extracted unchanged to
`Aim.withinArc` and now used by both. No content.

**PR 3, `feat/living-lantern`:** `content/weapons/living_lantern.yml`, its recipe, three visuals, its golden block,
`LivingLanternContentTest`; a minted weapon is never placed as a block (`RpgListeners.onWeaponPlace`); the fire-site count
17 -> 18; `PLAN-weapon-tiers.md` 1.8; this gate and its scenario. **It joins NO loot pool.**

## THE SEAT PLACEHOLDERS, FOR BEN (each marked `SEAT PLACEHOLDER 2026-11-22` in the file)

| placeholder | value | why |
|---|---|---|
| id, name, ability | **`living_lantern`, "Living Lantern", "Lantern's Breath"** | the old weapon's name; a new ability name |
| description | "Breathes a cone of flame" / "that rolls 6 blocks ahead." | states the reach |
| rarity, class | **common, mage** | the iron sword's rarity; the mage ladder |
| range | **6.0 blocks**, from his eye to a body's eye | Ben's "about 6 blocks" |
| travel | **6 ticks** to the range (1 block a tick); 0 would be instant | "travels"; no number given |
| the 60 degrees | **the FULL angle of a CONE** round his aim: 30 either side, up and down as well | "60 degree arc" |
| per body | **struck ONCE per cast, the full 14, no falloff** | the simplest reading |
| blocks | **a body with no clear line from his eye is not struck** | a flame does not pass a wall |
| look and sound | the old FLAME and DUST 255,140,0; lantern place and allay-with-item on cast; lantern break on hit | the old weapon's |
| volumes, pitches, particle counts | the seat's | the old file gave the sounds, not their settings |

## WHAT WAS READ, EACH MARKED

**The cone (PR 2).**
- **The angle test is the melee swing's (READ):** `CastExecutor.meleeTarget` skipped a candidate when the dot of the
  unit direction to it with the aim fell under `cos(arc / 2)`. That test is now `Aim.withinArc`, used by both. **The
  point tested differs, and it is said:** the swing tests a body's FEET (unchanged), the cone its SIGHT POINT (feet +
  eye height, `CombatantSnapshot.sightPoint`), the point the line of sight is traced to. MUTHALF (halving removed) reddened
  two melee rows and two cone rows: the test is shared.
- **Geometry, EXECUTED** (jshell over the staged positions; his eye 1.62 above his feet, a zombie's 1.74, READ in the
  jar by `GATE-knell-beam.md` d)): a zombie `x` east, `z` south, at his feet height, is `sqrt(x^2 + z^2 + 0.12^2)` from
  his eye. The front is at `k` blocks on its `k`-th tick. Every row below quotes its zombie's distance, angle and tick.
- **When it lands, by tick (READ path; the last step INFERRED):** the press is tick T; the cast runs on T+1 in the
  global tick (`WeaponFire.attempt`'s `onRegion` hop; `GATE-knell-beam.md`'s KB2 note); the front's `k`-th tick is
  `world.schedule` from there, so **T+1+k**; the struck body's health write is `onEntity` from the global tick, so it
  lands in that SAME server tick (INFERRED) or, by entity order, the next. **Every probe below sits clear of both.**

**Ruling 4: another player, and the caster (READ).** A spell's `on_hit` strikes whatever target the cast hands it:
`EffectApplier`'s `Damage` arm has no player filter, the ability path no PvP gate, and **no PLAYERHIT line is written for
a player target**. **So the cone strikes another player in it, and scorches him** (below). **It never strikes its caster**:
`ConeShell.reached` leaves his id out, as a ray (`castRay`'s ignore id) and the swing (`casterId`) do. Kept.
`ConeCastTest.itNeverStrikesItsCaster` causes it (his own sight point inside the cone, only the id excluding it).

**Ruling 5: what a fire hit adds beyond the 14 (READ unless marked).** `fire.yml` declares `applies_status: scorch`. In
`BukkitCombatant.applyNow`, when the hit leaves the target alive:
- **Scorch** (`ElementAccrual.forHit`, `ScorchStatus`/`DotStatus`): a burn of 120 ticks; it burns every 20 ticks, **the
  first 20 ticks after the hit**, six times (at +20, +40, ... +120); each burn is **`min(5% of the victim's max, cap)`**
  with **cap = 14 x 0.5 = 7** (the AUTHORED 14, `Scorch.CAP_FRACTION`; a crit does not raise it). **On a 100-max zombie,
  `min(5, 7)` = 5 a burn, 30 over the six.** Each burn logs a `DOTTICK` line (the trace is on).
- **A second hit on a burning body REFRESHES the window and keeps its 20-tick clock** (`DotStatus.apply`: `remaining`
  reset, the task not restarted). So LL8's second hit 16 ticks after the first adds no burn inside the window.
- **Vanilla fire ticks:** `setFireTicks(max(current, 120))` on the struck body, the flames you see. Vanilla's own
  `FIRE_TICK` damage is suppressed while scorched (`applyNow`'s comment, citing `RpgListeners`; that site NOT re-read).
  Every probe below is inside the first 40 ticks after a hit, so the window's end is never read.
- **A hit that KILLS an unscorched body** detonates `Ignite` 20 ticks later: radius 4, 6 damage, **skipping players and
  the victim**. Only LL9 can kill a zombie (14 a hit, from 100).
- **Fire blocks: none.** No path writes a fire block; the only fire write is an entity's fire ticks.
- **The caster:** nothing (he is never struck).

**Ruling 7: the recipe, the lantern, the fire charge.**
- **The route (READ):** `RecipeRegistrar` registers a Bukkit `ShapedRecipe` per file (result: the plain material),
  and our crafting menu (a crafting table is hijacked, `RpgListeners.openHijackedBlock`) mints the weapon at commit
  (`InventoryCraft.commitCraft`, logging `CRAFT minted …`). The grid afterwards is **the server's resulting matrix**, so
  each slot gives ONE item per craft (vanilla's shaped rule, INFERRED). A full 3x3 cannot be made in the 2x2 player grid.
  The result needs no durability, as the three staves' do not. **The vanilla crafter block** is answered by the existing
  `CrafterCraftEvent` handler, as for the other staves (not re-read).
- **No vanilla clash (READ, the pinned jar's 1,515 recipe files):** `lantern.json` is eight iron nuggets round a TORCH;
  `fire_charge` appears only in `fire_charge.json` and `firework_star.json`; none pairs it with an iron nugget.
- **A lantern PLACES (READ):** in the MAIN hand, `RpgListeners.onRightClick` cancels the interaction whenever the held
  weapon binds `right_click`, fired or refused, which in the pinned paper-api denies both the block use and the item use.
  **In the OFF hand it would have been placed**: that event is handed to vanilla and only a stone is cancelled there.
  **Pinned: `RpgListeners.onWeaponPlace` cancels any `BlockPlaceEvent` whose item carries a weapon id** (keyed by the id,
  never the material, so a vanilla lantern still places), whatever hand. Its test reads the handler from source; no gate
  row places one.
- **A fire charge IGNITES on right-click:** vanilla's, and only an ingredient here; our code does nothing with it (READ:
  no site). CR3's gold line tells him not to.

**PR 1's answers (recorded in `GATE-knell-beam.md`, under KB1 and KB2).** **The yellow** is vanilla's rarity colour on the
wrapper `effectiveName()` builds (`echo_shard` is UNCOMMON, yellow; READ); the client shows our inner dark purple
(INFERRED). A lantern is COMMON (white), so the Living Lantern's name dumps `[white] italic` (white on white collapses).
**The hit's tick:** the Knell Beam's shot fires in his entity tick T+24 and the zombie's health moves on T+25
(`onEntity` is a one-tick run; READ path, INFERRED order), so KB2's tick-25 probe could read 100. **KB2R2** reads it.

## HIS HEALTH (PR 1)

- The setup's first `SELF customHP=<c0>/<m0>` is his own level (`m0 = 350 + G`); after the staging, **`SELF
  customHP=<c1>/<m1>` with `m0 - m1 = 250.000` and `c1 = min(c0, m1)`** (as `GATE-knell-beam.md` read: 160/350 -> 100/100).
- Nothing in the run can lower him (*SAFETY*). Natural regeneration at level 1 is his gear's only (`LevelBonus` grants 0).
- **At the end:** `SELFTEST RUN RESTORED … health=<c0>`, then **10 ticks later** `SELFTEST RUN HEALTH saved=<c0>
  customHP=<c>/<m0> wrote=<w>`: **if `c0` is above his health then, `wrote=<c0>` and `customHP=<c0>/<m0>`**; if not (he
  began at or under `100 + G`), `wrote=none` and `customHP` at least `<c0>`. **A `customHP` max that is not `m0`** means the
  write came before the reconcile raised it: NOT AS PREDICTED, and the restore did not work.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup:** `run/plugins/Rpg` and `run/world` copied to a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-living-lantern`; file and directory counts, `diff -rq lines: 0` for each, the
   `ls` of `world/dimensions`, the overworld's file count. Nothing is restored (Ben's word only).
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND:** "booting now", and Ben told **NOT to log in until told R0 passed.**
6. `./scripts/dev-server.sh --refresh-content`. **Required deployed lines:** `weapons/living_lantern.yml` reads
   `    cooldown_ticks: 16`; `weapons/` holds 21 `.yml` files; `recipes/living_lantern.yml` and
   `visuals/lantern_front.yml` exist.
7. R0a to R0e on **this boot's** log, matched to **this boot's own** `Done` line.
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* `rpgfight` empty; the overworld's file count against step 2's **plus any chunk his crafting
   table landed in** (CR3 leaves the table placed); `All dimensions are saved`; no `java` process left.

## THE RUN SHEET — BEN TYPES EXACTLY THESE THREE LINES

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.**
   **Stand on open, flat ground with a clear view for about 30 blocks, and face any way you like** (the rows turn you
   east and put you back on the same spot each time).
   **For the run your level is set to 1, so your health drops to about 100; it is put back at the end.**
1. `/gamemode survival`
2. `/rpg selftest living-lantern` — **ONCE.** About two and a half minutes plus your craft. Frozen zombies appear; they
   cannot move or hit you. **Three rows ask something of you, in gold chat:** one gives you a crafting table, nuggets
   and a fire charge (**place the table and craft ONE Living Lantern**, 60 seconds; do not right-click the fire charge);
   one says **HOLD RIGHT-CLICK with the Crystal Shard Staff** (hold it until your mana runs out, then let go; it is the
   only thing in your hotbar); and the last gives you 30 seconds with the Living Lantern, then asks a question: **type
   your answer in chat.** Your items, time, level and health are put back at the end.
3. `/stop`

Nothing else. If a row stalls or aborts, do not intervene. **His chat answer to the one question is the only line he
types beyond these three**; "not sure" is NOT STATED.

## THE ARITHMETIC (SAFETY): THE MOST HITS THAT CAN LAND ON BEN TIMES THE BIGGEST HIT, AGAINST THE HEALTH HE HAS IN THE ROW

**His health in every row is `100 + G`** (the setup's second `SELF` line; G is his gear's max health). His defence is
his gear's alone (level 1 grants 0). Below, G = 0 and defence 0: the worst case.

| row | where he is | what can hurt him | most hits x biggest hit | puts him down at 100 + G? |
|---|---|---|---|---|
| setup, LL0, LL1 | the spot he chose | nothing the run makes | 0 | no |
| LL2-LL8, KB2R2 | the run's start, facing east; NoAI zombies 2 to 10 blocks away | a NoAI zombie does not move or attack, **burning or not** (a zombie sets a target alight only by a melee hit, INFERRED vanilla; NoAI makes none). **His own cone cannot strike or scorch him** (excluded by id, READ) and **places no fire block** (READ); the Knell Beam's line skips him (READ, `GATE-knell-beam.md`) | 0 x 0 = 0 | no |
| CR3 | where he walks to place a table, 60 seconds | as below, natural mobs; **and a fire charge he right-clicks would light a fire block** (vanilla) that burns him if he stands in it: his act, against the gold line | not bounded (his own act) | only if he stands in a fire he lit |
| HF1R3 | the run's start | his shards skip him (`castRay`'s ignore id, READ) | 0 | no |
| LL9 | the run's start; six NoAI zombies, 30 seconds in which he may walk | the same; **a zombie his cone kills IGNITES 20 ticks later: radius 4, 6 damage, SKIPPING PLAYERS** (READ) | 0 | no |
| every row | the overworld at night, after the setup's kills | **a natural hostile that was beyond 64 blocks at the kill and closes in** (as `GATE-knell-beam.md`'s table: none has him as a target from there; spawns off; spawners, phantoms and patrols as there) | at gs 100, 5 x vanilla: a zombie about 15 a hit, a close creeper about 215 (vanilla UNVERIFIED) | **at 100:** 7 zombie hits, or one close creeper. Possible, not likely; a death aborts the run and costs no items |

**The run's length** (`wait:` steps summed per row; recount with `awk '/^[A-Za-z0-9_-]+:/{row=$1}
/^  - wait:/{w[row]+=$3} END{for (r in w) print r, w[r]}' paper/src/main/resources/selftest/GATE-living-lantern.yml`):
setup 40, LL2 90, LL3 70, LL4 72, LL5 94, LL6 69, LL7 72, LL8 90, KB2R2 33, CR3 1,200, HF1R3 300, LL9 600: **2,730**,
plus the ASK's 0 to 1,200. Plus about 5 ticks per row-start clean (13 rows: 65) and up to 40 per `kill` step (38:
`grep -cE '^\s+- cmd: "kill '`, 27 in the setup). **Worst case 2,730 + 1,200 + 65 + 1,520 = 5,515 ticks** (4 min 36 s);
with nothing to kill, a prompt craft and a prompt answer, about 2,800 (2 min 20 s).

## R0 — THE BUILD, FOR THE WHOLE STACK. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `feat/living-lantern`'s head as `git ls-remote` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries all three PRs

**SUPERSEDED for the next boot by R0bR2** (at the end of this file): its cooldown-line check cannot match a line that
carries a comment. Kept as written, with its reading.

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/weapons/living_lantern.yml` (line `    cooldown_ticks: 16`), `content/recipes/living_lantern.yml`, the three `lantern_*` visuals, `selftest/GATE-living-lantern.yml` (line `HF1R3:`), `selftest/index.yml` (line `  - living-lantern`); the classes `rpg/core/combat/ConeShell.class` (PR 2), `rpg/paper/listener/RpgListeners.class` carrying `onWeaponPlace` (PR 3), `rpg/paper/selftest/SelfTestDrivers.class` carrying `HELDID slot=` and `rpg/paper/selftest/SelfTestSnapshot.class` carrying `HEALTH saved=` (PR 1). The control `content/weapons/knell_beam.yml`: PRESENT | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Line($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Entry($name) { if ($zip.GetEntry($name) -eq $null) { "ABSENT  entry $name" } else { "PRESENT entry $name" } }
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
foreach ($e in @('weapons/living_lantern.yml', 'recipes/living_lantern.yml', 'visuals/lantern_cast.yml', 'visuals/lantern_front.yml', 'visuals/lantern_hit.yml', 'weapons/knell_beam.yml')) { Entry ('content/' + $e) }
Line (Bytes 'content/weapons/living_lantern.yml') '    cooldown_ticks: 16' 'living_lantern.yml'
Line (Bytes 'selftest/GATE-living-lantern.yml') 'HF1R3:' 'GATE-living-lantern.yml'
Line (Bytes 'selftest/index.yml') '  - living-lantern' 'index.yml'
$p = 'io/github/butterflysmp/rpg/'
Entry ($p + 'core/combat/ConeShell.class')
Has ($p + 'paper/listener/RpgListeners.class') 'onWeaponPlace'
Has ($p + 'paper/selftest/SelfTestDrivers.class') 'HELDID slot='
Has ($p + 'paper/selftest/SelfTestSnapshot.class') 'HEALTH saved='
$zip.Dispose()
```

### R0c — every Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| Exactly one of each, up to the first `joined the game`: **`[Rpg] Loaded 11 abilities, 40 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 21 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 5 recipes, 1 ores, 4 vein sizes`** (against `GATE-knell-beam.md` R0c, read AS PREDICTED at `61b617ef`: **visuals 37 -> 40, weapons 20 -> 21, recipes 4 -> 5**; every other figure unchanged); `GATE-fight-revive.md`'s R0c fight line and `GATE-loot-roll.md`'s R0c loot line, word for word (the lantern joins no pool); the world-damage and smithing lines as at the last boot; **`[Rpg] Mint-on-craft: 36 indexed, 36 claiming, of 51 gear definitions`** (50 -> 51: 21 + 1 + 24 + 5; the lantern claims no craft_result); `Convert-on-arrival: 7 materials {…}` as before. **No `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Recipe '`; no `[Rpg]` WARN beyond `volley_stone`'s known pair** (a 3x3 recipe draws no 2x2 warning; a cone has no cooldown floor). No `No fights loaded` | `Select-String run\logs\latest.log -Pattern 'Loaded\|Mint-on-craft\|Convert-on-arrival\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded\|Recipe '''` | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 280 scenarios across 37 gates [melee-cell, …, knell-beam, living-lantern]`**. The control: the last boot's `267 scenarios across 36 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (2026-11-22, PR 3's tree, from `index.yml` and every gate file it names; `SelfTestScenariosTest` asserts
280): 37 gates, `^[A-Za-z0-9_-]+:` **317**, `^setup:` **37**, so **280**. Control: 303 + 14 (setup, LL0-LL9, KB2R2, CR3,
HF1R3) = 317.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, no `deleting`, no `orphan` | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | |

## THE ROWS (`/rpg selftest living-lantern`) — SURVIVAL, ALL AUTO

**EVERY DAMAGE PREDICTION NAMES EVERY TERM** (`.claude/rules/verification.md`):
`sent = (amount x score/100) x enchant + class bonus + level bonus`, then `x charge x crit`.
**For the Living Lantern:** `(14 x 100/100) x 1.0 + 0 + 0 = 14`, `x 1.0`, and `x 2.0` on a crit: **`sent=14.000
crit=false` or `sent=28.000 crit=true`**. Terms: amount 14 (the file); score 100 (`rpg gearscore set 100`, staged); enchant
1.0 (no active enchant on a given item; LL2's `rpg enchant show` reads it); class bonus 0 (nothing grants class damage);
level bonus 0 (level 1, staged; `LevelBonus.at(1).weaponDamage()` executed 0.0 in `GATE-knell-beam.md`); charge 1.0 (not
a melee charge). **Then RULING 5's terms on the zombie's health, by name:** the hit (14, or 28 on a crit); then
**Scorch, 5 a burn** (`min(5% of 100, 14 x 0.5)`) at 20, 40, ... ticks after the hit, each a `DOTTICK` line. **No
other term.** **For the Knell Beam (KB2R2):** `(63 x 100/100) x 1.0 + 0 + 0 = 63`; `sent=63.000` or `126.000`.
A zombie here has `max=100` (`MOBSEED … gs=100 … max=100`). **Any other `sent` is NOT AS PREDICTED.**

**THE PLAYERHIT LINE:** `PLAYERHIT … source=living_lantern/right_click target=<uuid> zombie element=fire sent=14.000
crit=false triggerScore=100` (or `28.000 crit=true`). Probes below give "86 (72 on a crit)" and so on.

### setup — night, no spawns, the natural hostiles killed, his level at 1 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| As `GATE-knell-beam.md`'s setup, line for line: its own `LEFTOVER`; the `time set` and `gamerule` REPLYs; **`SELF customHP=<c0>/<m0> …`**; 27 kill lines, each `CMD /kill …` then `REPLY "No entity was found" [red]`, `"Killed <name>"` or `"Killed <n> entities"`, and a `SETTLED` line (recorded); `REPLY "Set 1 level(s) for BaronVonYeetus (now level 1, 0/1000 into it, 0 lifetime)." [green]`; then **`SELF customHP=<c1>/<m1>` with `m0 - m1 = 250.000` and `c1 = min(c0, m1)`** | *HIS HEALTH* | |

### LL0 — THE SPOT: open and flat? (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`LEFTOVER living=0 radius=16`** or only types not on the kill list. The teleport's REPLY. `SELF …`. **`BLOCKS AIR count=480 box=1 0 -4 30 1 3`**; **`BLOCKS AIR count=0 box=1 -1 -4 30 -1 3`**; **`BLOCKS WATER count=0 box=1 -1 -4 30 -1 3`**. A low corridor count is the spot, not the build, and is read beside LL2-LL9 (`GATE-knell-beam.md` c)): a block between his eye and a zombie's stops the cone (LOS), so a hit row reads no PLAYERHIT | `SelfTestDrivers.probeBlocks` | |

### LL1 — given, held, and its tooltip (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| REPLY `Given Living Lantern`; `INVENTORY living_lantern slots=[…]`; `DRIVE hold …`; the gearscore REPLY; then **`HELD living_lantern LANTERN x1 name="Living Lantern" [white] italic lore=["Fire" [red]; "Gear Score: 100" [gray, aqua]; "" []; "Lantern's Breath  Right-Click" [gold, yellow]; "Breathes a cone of flame" [gray]; "that rolls 6 blocks ahead." [gray]; "Fire Damage: 14" [gray, red]; "Cooldown: 0.8s \| Mana Cost: 12" [dark_gray]; "" []; "Common Magic Weapon" [white]]`**. **The cooldown line reads 0.8s** | `LivingLanternContentTest.aMintedLanternsTooltipReads` (the same ten lines from the real file); `golden-lore.txt`; the name's `[white]` is vanilla COMMON white round our white, collapsed (PR 1's KB1 note) | |

### LL2 — one cast at a zombie 3 ahead: 12 mana, and the hit (SURVIVAL)

Its zombie: 3.0024 from his eye, 2.29 degrees off the aim (EXECUTED), reached on the front's 4th tick: **press + 5**.

| prediction | stands on | READING |
|---|---|---|
| `rpg enchant show`: every slot's active choice `-1`. `MOBSEED … zombie gs=100 … max=100`. **`MANA current=100.000 max=100.000`**, `DRIVE use … item=living_lantern … cadence=excluded`, **`MANA current=88.000 max=100.000`**, read in the press's own tick: **12 less, no regeneration term**. Probe at **press + 3**: **`customHP=100.000/100.000`** (not yet reached). Exactly one **`PLAYERHIT … element=fire sent=14.000 crit=false triggerScore=100`** (or 28.000). Probe at **press + 8**: **`86.000/100.000`** (100 - 14; 72 on a crit). Probe at **press + 30**: **`81.000/100.000`** (86 - one Scorch burn of 5, at the hit + 20; 67 on a crit), with **one `DOTTICK` line before it**, amount 5 | the sum above; ruling 5 | |

### LL3 — THE ARC: 20 degrees off is struck, 40 off is not (SURVIVAL)

A at `(2.819, 1.026)`: 3.0023 out, **20.12** degrees off; B at `(2.298, 1.928)`: 3.0021 out, **40.05** off (EXECUTED).
60 degrees is **30 either side**.

| prediction | stands on | READING |
|---|---|---|
| `POSITIONS ZOMBIE n=2` holding `2.82 0.00 1.03` and `2.30 0.00 1.93` (two places, as `probe_positions` prints). Exactly **one** PLAYERHIT, on the FIRST `MOBSEED` (A), `sent=14.000` (or 28.000). Probe at press + 10: **A `86.000/100.000`** (72 on a crit), **B `100.000/100.000`** | `ConeCastTest.aBodyInsideTheAngleAndRangeIsStruckAndOneJustOutsideEachIsNot`; `Aim.withinArc` | |

### LL4 — THE RANGE: 5 is struck, 7.5 is not (SURVIVAL)

5.0014 out (reached on the front's 6th and last tick, press + 7) and 7.5010 (beyond 6) (EXECUTED).

| prediction | stands on | READING |
|---|---|---|
| Exactly **one** PLAYERHIT, on the first `MOBSEED` (the zombie at 5), `sent=14.000` (or 28.000). Probe at press + 12: **the first `86.000/100.000`** (72), **the second `100.000/100.000`** | `ConeCastTest` (5.9 struck, 6.1 not) | |

### LL5 — EACH ONCE: three zombies, three hits, nearest first (SURVIVAL)

Summoned in this order: Z1 `(5, -1.5)` 5.2215 out, 16.75 degrees, the front's 6th tick; Z2 `(2, -0.5)` 2.0650, 14.42,
3rd; Z3 `(3.5, 1)` 3.6420, 16.05, 4th (EXECUTED). **Summon order is not distance order on purpose.**

| prediction | stands on | READING |
|---|---|---|
| Exactly **three** `PLAYERHIT … element=fire` lines, **each target once**, in the order **Z2, Z3, Z1** (the 2nd, 3rd, 1st MOBSEED), each `sent=14.000` (a crit roll per cast: all three 28.000 or none). Probe at press + 12: **all three `86.000/100.000`** (72). Probe at press + 34: **all three `81.000/100.000`** (one burn each: at press + 24, 25 and 27; 67 on a crit), three `DOTTICK` lines between the probes | `ConeCastTest.threeBodiesInsideTheConeAreEachStruckOnce`, `…NotStruckAgainAfterMovingAheadOfTheFront` | |

### LL6 — THE TRAVEL: the near zombie three ticks before the far one (SURVIVAL)

`n` at 2: 2.0036 out, the front's 3rd tick, **press + 4**; `f` at 5: 5.0014, 6th tick, **press + 7** (EXECUTED). A write
may land a tick later by entity order (*WHAT WAS READ*), so each is bracketed clear of both its tick and the next.

| prediction | stands on | READING |
|---|---|---|
| `REPLY "The game time is <g> tick(s)"` with the press. **`n` at press + 3: `100.000/100.000`; at press + 6: `86.000/100.000`** (72). **`f` at press + 6: `100.000/100.000`; at press + 9: `86.000/100.000`** (72). Two PLAYERHITs, **`n`'s first**. **So `n` was struck at press + 4 or 5 and `f` at press + 7 or 8: three ticks apart.** An instant cone would read `n` and `f` both struck by press + 3; a cone one tick slow reads `f` at 100 at press + 9 only if two ticks slow | `ConeCastTest.theNearBodyIsStruckThreeTicksBeforeTheFarOne` | |

### LL7 — A WALL: no hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `STAGE saved 2 blocks for the restore, box=2 0 0 2 1 0`; `Successfully filled 2 block(s)`; a zombie at 4 (4.0018 out). **0 PLAYERHIT**; probe at press + 12: **`100.000/100.000`**. The next row's `CLEAN blocks=2` puts the stone back. **LL2 is its control** (no wall, a hit) | `ConeCastTest.aWallStopsIt` (MUTLOS) | |

### LL8 — THE COOLDOWN: refused at 12, fires at 16 (SURVIVAL)

The zombie at 3 (press + 5 after each firing press).

| prediction | stands on | READING |
|---|---|---|
| **`MANA current=100.000`**; press 1, `REPLY "The game time is <g1> tick(s)"`; press 2 **12 ticks later**: **`REPLY "On cooldown for 0.2s" [gray]`** (4 of 16 left), `<g2>`, **`MANA current=88.600`** (100 - 12 + **12 ticks of regeneration, 0.6** at the 0.05 a tick `GATE-knell-beam.md` KB7 read; the refused press spent nothing); press 3 **4 ticks after that**: fires, no refusal, `<g3>`, **`MANA current=76.800`** (88.6 - 12 + 0.2). **`g2 - g1 = 12`, `g3 - g1 = 16`.** **Exactly two PLAYERHITs**, each 14.000 (or 28.000). Probe at press 3 + 7: **`72.000/100.000`** (100 - 14 - 14, before the first burn at press 1 + 25). Probe at press 3 + 14: **`67.000/100.000`** (one burn of 5; the second hit refreshed the burn and kept its clock, READ `DotStatus.apply`). **If the regeneration reads otherwise, the mana figures move by that term only** (its granularity UNVERIFIED) | `AbilityService` stamps at the click, `isReady` is `>=` (READ, `GATE-knell-beam.md`); `"On cooldown for %.1fs"` over 4/20 = 0.2 | |

### KB2R2 — OWED: the Knell Beam's hit, probed on ticks 24, 25 and 26 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `REPLY "The game time is <g> tick(s)"` with the press. One **`PLAYERHIT … source=knell_beam/right_click … element=void sent=63.000 crit=false`** (or 126.000). Probe at press + 24: **`100.000/100.000`** (READ: the health write is a tick after the hit at the earliest; **whether this probe's line comes before or after the PLAYERHIT line is NOT predicted**: both sit in his entity tick, the probe's wait queued first, INFERRED probe first). Probe at press + 25: **`100.000/100.000`** (INFERRED: the player's scheduler runs before the zombie's in a tick; **`37.000` here is the other order, NOT a defect**). Probe at press + 26: **`37.000/100.000`** (READ), or dead on a crit | `GATE-knell-beam.md`, KB2's tick note | |

### CR3 — OWED: THE CRAFT, AND ITS TOOLTIP HALF (SURVIVAL)

Carried since `GATE-remove-dev-weapons.md` (CR1's tooltip half) and `GATE-crystal-shard-staff.md` (CR2).

| prediction | stands on | READING |
|---|---|---|
| `clear` REPLY (the rows' lanterns gone); `INVENTORY living_lantern slots=[]`; three `give` REPLYs; the TELL. **If he crafts:** **`CRAFT minted player=BaronVonYeetus weapon=living_lantern material=lantern score=<s>`** (word for word, `InventoryCraft.craftMintLine`); after the 60 seconds, `INVENTORY living_lantern slots=[<one slot>]`, `DRIVE hold …`, and **`HELD living_lantern LANTERN x1 name="Living Lantern" [white] italic lore=[…; "Gear Score: <s>" [gray, aqua]; …; "Fire Damage: <14 x s / 100, as WeaponLore formats it>" [gray, red]; …]`** with **the dump's Gear Score EQUAL to the CRAFT line's `score=`**. **If he does not craft:** no CRAFT line, and the hold reads `SKIPPED … no hotbar slot holds living_lantern`: **UNREAD** | `CraftMintLineTest`; `LivingLanternContentTest.bensRecipeMintsIt` | |

### HF1R3 — OWED: held fire on the 14-tick staff, ONLY that staff in his hotbar (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Two `clear` REPLYs; `Given Crystal Shard Staff`; `STAGE hold_only moved <id> from hotbar <i> to slot <j>` for each other hotbar item (or none), then **`STAGE hold_only hotbar items=1 offhand=<id>`**, then `DRIVE hold … item=crystal_shard_staff`. **If too few slots:** `SKIPPED … too few empty inventory slots`, and HF1 is UNREAD a fourth time. The first `rpg firerate` (the row's own) prints, in the log as `FIRERATE player=BaronVonYeetus <line>` and as the REPLY: **`No fire-cadence sample. … (19 selftest-driven records kept out of every sample)`** (19: 2 for each firing press in LL2-LL7 and KB2R2, 5 in LL8, INPUT and FIRE; recount from the scenario) **or**, if he fired weapons of his own after logging in, one reading per weapon he used, the `19 selftest-driven records kept out` line, and `Sample cleared.`. **`HELDID slot=<k> item=crystal_shard_staff offhand=<id>`** at the start and **the same** at the end of the 15 seconds. The TELL names the staff. The second `rpg firerate`: **`Fire cadence -- crystal_shard_staff, right_click`**, **`  INPUTS  count … min <i>t`**, **`  FIRES   count … min <f>t`**, **`  cooldown_ticks 14 authored, 14 effective (not a basic attack -- attack speed does not scale it)`**, a verdict line, `Sample cleared.`; **no `weapons sampled` line and no excluded line.** **By `GATE-crystal-shard-staff.md`'s table, restated:** **`FIRES min 16`, `INPUTS min 4`: inconclusive**; **`FIRES min 16`, `INPUTS min 3`: the fit survives**; **`FIRES min 14` or `15`: `ceil(n/4) x 4` IS FALSIFIED**; below 14, a cooldown defect. **If he did not hold:** `No fire-cadence sample.`: UNREAD again | PR 1 (`FireCadence` per weapon, `excluding`, `HOLD_ONLY`, `PROBE_HELD`) | |

### LL9 — A FREE GO, THEN THE ONE QUESTION (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Six `MOBSEED` lines (four inside the cone from the start: 2.5, 4.27, 4.47 and 5.52 out at 2.75, 20.62, 26.61 and 5.34 degrees; two outside: 6.58 out, 53.14 off, EXECUTED); the TELL; 30 seconds in which any PLAYERHITs are his, each `sent=14.000` or `28.000` (recorded, none predicted: he turns and walks); a zombie that dies may be followed 20 ticks later by its ignite's hits on the others; then **`ASK 1 question="Did the flames reach as far and as wide as they looked?" answer="<his words>"`**, verbatim, or `answer=NOT STATED after 1200 ticks` | `SelfTestAsk` | |

### THE END OF THE RUN

| prediction | stands on | READING |
|---|---|---|
| `SELFTEST RUN RESTORED xp=<his lifetime xp> … health=<c0>`, then **10 ticks later** `SELFTEST RUN HEALTH saved=<c0> customHP=<…>/<m0> wrote=<…>`, as *HIS HEALTH* predicts | PR 1, `SelfTestHealthRestoreTest` | |

## WHAT THIS GATE DOES NOT READ

- **What he sees and hears**: the front's flames and dust, the sounds, the name's colour. AUTO reads the server, never the
  client; only his one answer speaks to it.
- **A lantern placed as a block**: no row drives a `BlockPlaceEvent`; the guard is pinned by source
  (`LivingLanternContentTest.aMintedWeaponIsNeverPlaced`). **The off hand's right-click** is untested in game.
- **The cone against another player**: there is one player. Source only (ruling 4).
- **Unbreaking granting nothing on the lantern** (no durability): source only, carried open with the other staves.
- **The vanilla crafter making a lantern**: the existing `CrafterCraftEvent` handler, not re-read here.

## READINGS, 2026-11-22 — ONE BOOT AT `d98c28bb`, READ FROM `run/logs/latest.log` BY CC. R0 NOT AS PREDICTED; NO ROW RUN

**Written after the boot. No prediction above was edited.** This boot's own lines: `[Rpg] Build: d98c28bb` (13:12:07),
`Done (7.123s)!` (13:12:08).

### THE BOOT, BY `booting.md` AND THE CARRIED STATE

| step | read |
|---|---|
| 1. the seat's word and Ben's | the seat's boot approval (2026-11-22, relayed); Ben's own word, verbatim: **"ready"**, then **"go ahead"** before the boot command |
| 2. the heads | `git ls-remote`: `feat/living-lantern` `d98c28bba6a574de31d9ab9578edaa6ecbe3d88e`, as the seat diffed; #231, #232 and #233: OPEN, MERGEABLE, both `build` checks `pass` on each current head |
| CS 1. no JVM | `no java process` |
| CS 2. backup | `../rpg-run-backups/2026-11-22-pre-living-lantern` (new): Rpg 149 files / 26 dirs both sides, `diff -rq lines: 0`; world 193 files / 32 dirs both sides, `diff -rq lines: 0`; `world/dimensions`: `minecraft rpgfight`; overworld 106 files |
| CS 3. config | `12:  dev-pack-on-join: true` |
| CS 4. fight folders | `rpgfight` entries: 0 |
| CS 5. "booting now" | sent in its own message, with "Ben: do NOT log in until I tell you R0 passed"; the boot command in the next turn, after Ben's "go ahead" |
| CS 6. boot, deployed lines | `==> Building d98c28bb`; `weapons/living_lantern.yml:42:    cooldown_ticks: 16                     # BEN: 0.8 s. …` (the predicted text, followed by its comment); `weapons/` 21 `.yml`; `recipes/living_lantern.yml` and `visuals/lantern_front.yml` present. The boot's build runs no unit tests (0 `Tests run:` lines) |
| CS 7. R0a to R0e | below: **R0b NOT AS PREDICTED**, the rest as predicted |
| CS 8. "R0 passed" | **NOT SENT.** CC sent "R0 did NOT pass. Ben: do NOT log in", and stopped for the seat |
| login | **His `joined the game`: 13:13:29**, after R0 was read (13:12-13:13) and NOT passed. **Dated note, 2026-11-22: he logged in while R0 stood failed and the instruction was not to log in; whose omission is for the seat to record** (CC's STOP message was sent before 13:13:29; its exact time is not in the log). He issued ONE command, `/stop`, at 13:14:19. **0 `SELFTEST` lines: no row ran, so no reading below a row exists** |
| CS 9. after `/stop` | `rpgfight` entries: 0; overworld **106** files (step 2: 106); `[13:14:20] … All dimensions are saved`; `no java process` |

### R0

- **R0a — AS PREDICTED.** `[13:12:07] [Server thread/INFO]: [Rpg] Build: d98c28bb`. Not `-dirty`, not `unknown`.
- **R0b — NOT AS PREDICTED, ONE LINE OF THIRTEEN.** The scan as written printed PRESENT for twelve (the six content
  entries including the `knell_beam.yml` control, `HF1R3:`, `  - living-lantern`, `ConeShell.class`, `onWeaponPlace`,
  `HELDID slot=`, `HEALTH saved=`) and **`ABSENT  living_lantern.yml '    cooldown_ticks: 16'`**.
  - **What the jar holds (read after the STOP, as a test of the explanation, not as a pass):** exactly ONE
    `cooldown_ticks` line in `content/weapons/living_lantern.yml`, start-anchored:
    `    cooldown_ticks: 16                     # BEN: 0.8 s. On the 4-tick grid; not a basic attack, so no scaling`.
    The scan's `Line` check requires the line to END after `16` (`\s*$`), which a trailing comment defeats. **Control:**
    the same end-anchored check on `knell_beam.yml`'s comment-free `    cooldown_ticks: 48` returns True in this jar.
  - **So the value deployed is 16 and the miss is the gate's instrument**, written for a comment-free line and copied to
    a line that carries a comment. It stays NOT AS PREDICTED until the seat rules.
- **R0c — AS PREDICTED.** `[Rpg] Loaded 11 abilities, 40 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6
  fragments, 6 aspects, 21 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 5 recipes, 1
  ores, 4 vein sizes`, once; `Mint-on-craft: 36 indexed, 36 claiming, of 51 gear definitions`; `Convert-on-arrival: 7
  materials {…}`. The loot, fight, world-damage and smithing lines are **byte-identical** (timestamps stripped, 4 lines)
  to the last boot's (`run/logs/2026-11-22-1.log.gz`, `Build: 61b617ef`). No `Refusing`, `Skipping`, `SEVERE`,
  `Exception`, `Recipe '`, `No fights loaded`. `[Rpg]` WARN: only `volley_stone`'s pair. Paper's "4 release(s) behind"
  block is not `[Rpg]`.
- **R0d — AS PREDICTED.** `[Rpg] Selftest: dev server, 280 scenarios across 37 gates [melee-cell, …, knell-beam,
  living-lantern]`. Control `267 scenarios across 36 gates`: 0 occurrences.
- **R0e — AS PREDICTED.** `[Rpg] FIGHTWORLD sweep path=C:\…\run\world\dimensions\rpgfight folders=0`, once; no
  `deleting`, no `orphan`.

### WHAT IS UNREAD AFTER THIS BOOT

**Every row**: setup, LL0-LL9, KB2R2, CR3, HF1R3 and the end-of-run HEALTH line. No `/rpg selftest` was issued.

## THE SEAT'S RULINGS ON THE FIRST BOOT, AND R0bR2 — 2026-11-22, WRITTEN AFTER THE READINGS

**Written on the seat's rulings of 2026-11-22, relayed by Ben, before any second boot.** No prediction and no reading
above is edited; **R0b's reading stays NOT AS PREDICTED.** Gate files only; nothing under `src/` changes.

### THE SEAT'S RULINGS, RECORDED

1. **R0b's miss is THE GATE'S INSTRUMENT, not the build**, on CC's evidence in the readings: the jar holds exactly one
   `cooldown_ticks` line in `living_lantern.yml`, `    cooldown_ticks: 16` followed by a comment, and the same
   end-anchored check passes on `knell_beam.yml`'s comment-free line.
2. **The seat approved this gate without reading its R0b section.** Recorded at the seat's instruction.
3. **Ben's login at 13:13:29** came after CC's STOP message and before any "R0 passed". **He logged in early, issued only
   `/stop`, and nothing followed from it.** The seat has reminded him. (This is the seat's record for the login row in
   the readings above, which left it to the seat.)
4. CC's STOP was right: **a failed R0 is a STOP even when the cause looks harmless.**

### R0bR2 — the jar carries all three PRs (R0b re-predicted: the cooldown line may carry a comment)

| prediction | instrument | READING |
|---|---|---|
| The thirteen of R0b, each PRESENT: the six content entries (`living_lantern.yml`, `recipes/living_lantern.yml`, `lantern_cast`, `lantern_front`, `lantern_hit`, and the control `knell_beam.yml`); `living_lantern.yml '    cooldown_ticks: 16'`; `GATE-living-lantern.yml 'HF1R3:'`; `index.yml '  - living-lantern'`; `ConeShell.class`; `onWeaponPlace`; `HELDID slot=`; `HEALTH saved=`. **And the two controls: `ABSENT  living_lantern.yml CONTROL-1 '    cooldown_ticks: 1'`; `PRESENT knell_beam.yml CONTROL-EOL '    cooldown_ticks: 48'`** | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Line($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Entry($name) { if ($zip.GetEntry($name) -eq $null) { "ABSENT  entry $name" } else { "PRESENT entry $name" } }
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
foreach ($e in @('weapons/living_lantern.yml', 'recipes/living_lantern.yml', 'visuals/lantern_cast.yml', 'visuals/lantern_front.yml', 'visuals/lantern_hit.yml', 'weapons/knell_beam.yml')) { Entry ('content/' + $e) }
ValueLine (Bytes 'content/weapons/living_lantern.yml') '    cooldown_ticks: 16' 'living_lantern.yml'
Line (Bytes 'selftest/GATE-living-lantern.yml') 'HF1R3:' 'GATE-living-lantern.yml'
Line (Bytes 'selftest/index.yml') '  - living-lantern' 'index.yml'
$p = 'io/github/butterflysmp/rpg/'
Entry ($p + 'core/combat/ConeShell.class')
Has ($p + 'paper/listener/RpgListeners.class') 'onWeaponPlace'
Has ($p + 'paper/selftest/SelfTestDrivers.class') 'HELDID slot='
Has ($p + 'paper/selftest/SelfTestSnapshot.class') 'HEALTH saved='
'--- controls'
ValueLine (Bytes 'content/weapons/living_lantern.yml') '    cooldown_ticks: 1' 'living_lantern.yml CONTROL-1'
ValueLine (Bytes 'content/weapons/knell_beam.yml') '    cooldown_ticks: 48' 'knell_beam.yml CONTROL-EOL'
$zip.Dispose()
```

**What changed from R0b: one function, `ValueLine`, used for the cooldown line only.** Everything else is R0b's scan
unchanged. `ValueLine` requires the given line from the start of a line, then EITHER the end of the line
(`[ \t]*\r?$`) OR whitespace and a `#` (`[ \t]+#`).
- **Tabs and spaces only, never `\s`**, so neither branch can run on into the next line.
- **The `\r?` is needed, not a precaution. MEASURED against the `d98c28bb` jar:** its content files are CRLF
  (`living_lantern.yml` CR=65 LF=65, `knell_beam.yml` CR=72 LF=72; Maven packs the working tree, which is CRLF
  here), and .NET's multiline `$` matches only before `\n`. The same pattern without `\r?` returned **False** on
  `knell_beam.yml`'s line, and with it **True**. R0b's `\s*$` absorbed the CR by accident of `\s`.

**The controls, and what each proves:**
- **CONTROL-1, `    cooldown_ticks: 1` against the same file: must read ABSENT.** The character after `1` is `6`, which
  neither branch accepts. So the check cannot match a value as the prefix of a longer number, and `16` is accepted only
  when followed by the end of the line or by ` #`.
- **CONTROL-EOL, `knell_beam.yml`'s comment-free `    cooldown_ticks: 48`: must read PRESENT.** This proves the
  end-of-line branch, with its `\r?`, still matches a CRLF line. Without it, a check that only accepts a comment
  would pass CONTROL-1 and the lantern line and could no longer read a bare line.

**THE INSTRUMENT, RUN ONCE BEFORE THE BOOT, AGAINST THE `d98c28bb` JAR (`build-id.properties`: `commit=d98c28bb`,
deployed 13:12:00).** This is a check that the new scan can pass and can fail. **It is NOT R0bR2's reading**, which
belongs to the next boot's own jar. It printed the thirteen PRESENT, then `ABSENT  living_lantern.yml CONTROL-1
'    cooldown_ticks: 1'` and `PRESENT knell_beam.yml CONTROL-EOL '    cooldown_ticks: 48'`.

### EVERY OTHER R0 CHECK, FOR THE SAME FAULT

The fault: an end anchor, or an exact-line match, against a line that carries a comment or trailing text in the file as
committed. Lines read at `3e3c81ea` with `git show … | grep -n | cat -A`.

| check | what it reads | exposed? |
|---|---|---|
| R0a, `Select-String '\[Rpg\] Build:'` | the log line `[Rpg] Build: <sha>` | **NO.** Unanchored substring; the prediction compares the sha. A log line, not a committed file |
| R0b `Entry` x7 (six content files, `ConeShell.class`) | the jar's entry names | **NO.** Compares a whole entry name, not a line |
| R0b `Line`, `living_lantern.yml '    cooldown_ticks: 16'` | `paper/src/main/resources/content/weapons/living_lantern.yml:42`, `    cooldown_ticks: 16                     # BEN: 0.8 s. …` | **YES: the fault.** Fixed as R0bR2 |
| R0b `Line`, `GATE-living-lantern.yml 'HF1R3:'` | `selftest/GATE-living-lantern.yml:247`, `HF1R3:` and nothing after (line 242, a comment, contains `HF1R3` but does not start with it) | **NO.** Read PRESENT at `d98c28bb`. Exposed only if a comment is ever added to that line |
| R0b `Line`, `index.yml '  - living-lantern'` | `selftest/index.yml:92`, `  - living-lantern` and nothing after | **NO.** Read PRESENT at `d98c28bb`. Same caveat |
| R0b `Has` x3 (`onWeaponPlace`, `HELDID slot=`, `HEALTH saved=`) | class bytes, `String.Contains` | **NO.** Unanchored at both ends, so trailing text cannot defeat it |
| R0c, `Select-String 'Loaded\|Mint-on-craft\|…'` | log lines; the Loaded and Mint-on-craft lines compared word for word | **NO.** Unanchored pattern over log lines, not committed files. The compared lines end at their last figure; read AS PREDICTED |
| R0d, `Select-String 'Selftest:'` | the log line | **NO.** Unanchored; read AS PREDICTED |
| R0e, `Select-String 'FIGHTWORLD (sweep\|deleting\|orphan)'` | the log line | **NO.** Unanchored; read AS PREDICTED |

**Only one check is exposed, so R0bR2 is the only R2 row.** **Outside R0, the same text:** CARRIED STATE step 6's
required line, `weapons/living_lantern.yml` reads `    cooldown_ticks: 16`, names no instrument. At the first boot it
was read with `grep -n` (unanchored) and recorded with its comment (CS 6 in the readings). Not changed here. **It is
read as the line BEGINNING with that text**, the same reading R0bR2 now writes down.

### THE NEXT BOOT

- **It reads R0a, R0bR2, R0c, R0d and R0e afresh**, on its own log and its own jar. R0b's scan is not run again.
- **R0a's `Build:` line will name this commit's head**, not `d98c28bb`: this commit moves `feat/living-lantern`, and the
  jar is built from the head.
- **The rows (setup, LL0-LL9, KB2R2, CR3, HF1R3, the end of the run) and the run sheet are unchanged.**
- **One thing for the seat, not changed here: CARRIED STATE step 2's backup folder already exists.**
  `../rpg-run-backups/2026-11-22-pre-living-lantern` is the first boot's (`Test-Path`: True), and the step asks for a
  NEW folder. A second boot today needs another name. CC proposes `2026-11-22-pre-living-lantern-2`.

## READINGS, SECOND BOOT, 2026-11-22 — `42ef53c8`, READ FROM `run/logs/latest.log` BY CC. R0 PASSED; ALL 13 ROWS RAN

**Written after the boot. No prediction above was edited.** This boot's own lines: `[Rpg] Build: 42ef53c8` (13:59:41),
`Done (6.266s)!` (13:59:42). **Every row reading below is from after his `joined the game` at 14:00:30.** The seat's
second approval (2026-11-22) ruled: R0bR2 and the list for c accepted; the backup folder is
`2026-11-22-pre-living-lantern-2`; the first approval's rulings 5 and 6 stand (the HEALTH line lands 10 ticks after the
end; say so if the crafting table was placed in the corridor east of the start before calling an HF1R3 or LL9 miss).

### NOT AS PREDICTED (FIRST, AS THE SEAT ASKED)

- **THE END OF THE RUN: `health=124.000`, predicted `<c0>` = 126.000.** `SELFTEST RUN RESTORED xp=712672 cell=mage/fire
  gamemode=SURVIVAL held=3 mana=100.0 … difficulty=NORMAL health=124.000 drag_tick=false`, then 1 s later `SELFTEST RUN
  HEALTH saved=124.000 customHP=124.000/350.000 wrote=124.000`. The setup's first `SELF` read `customHP=126.000/350.000`.
  - **The prediction named the wrong instant (READ, source):** `SelfTestRun.start` takes the snapshot
    (`SelfTestSnapshot.take`) BEFORE it logs `RUN START`, so before the setup's first `SELF`. *HIS HEALTH* equated the
    two. The 2 between them is INFERRED to be survival regeneration in the ticks between (no line records it). The
    `PREDICT DAMAGE AGAINST THE HURT LINE` rule's lesson, on a restore: two health figures at two instants.
  - **What the line does show:** the max is `350.000` = `m0`, so the write came after the reconcile raised it (the
    defect arm *HIS HEALTH* names is NOT seen), and `wrote=124.000` = `saved`. **The restore restored the snapshot.**
    Whether 124 or 126 is the right figure to restore is the seat's; the code was not changed.

**Nothing else read NOT AS PREDICTED.**

### THE BOOT, BY `booting.md` AND THE CARRIED STATE

| step | read |
|---|---|
| 1. the seat's word and Ben's | the seat's second boot approval (2026-11-22, relayed); Ben's own word, verbatim: **"ready"**, then **"go ahead"** before the boot command |
| 2. the heads | `git ls-remote`: `feat/living-lantern` `42ef53c8bc54f0b1d17abf2f56a6195a92cee709`, as the seat diffed, read before the backup and again before "booting now"; #231 `9e29c621`, #232 `ad96c460`, #233 `42ef53c8`: OPEN, MERGEABLE, both `build` checks `pass` on each |
| CS 1. no JVM | 0 `java.exe` |
| CS 2. backup | `../rpg-run-backups/2026-11-22-pre-living-lantern-2` (new; the first boot's folder untouched): Rpg 154 files / 26 dirs both sides, `diff -rq lines: 0`; world 193 files / 32 dirs both sides, `diff -rq lines: 0`; `world/dimensions`: `minecraft rpgfight`; overworld (`dimensions/minecraft/overworld`) 106 files |
| CS 3. config | `12:  dev-pack-on-join: true` |
| CS 4. fight folders | `rpgfight` entries: 0 |
| CS 5. "booting now" | sent in its own message, with "Ben: do NOT log in until I tell you R0 passed"; the boot command in the next turn, after Ben's "go ahead" |
| CS 6. boot, deployed lines | `==> Building 42ef53c8`; `weapons/living_lantern.yml:42:    cooldown_ticks: 16                     # BEN: 0.8 s. …`; `weapons/` 21 `.yml`; `recipes/living_lantern.yml` and `visuals/lantern_front.yml` present. The boot's build ran no unit tests (0 `Tests run:` lines) |
| CS 7. R0 | below, all five AS PREDICTED, read 13:59:42 to 14:00:01 |
| CS 8. "R0 passed" | sent in its own message after a `joined the game` count of **0** at 14:00:01 |
| login | **`joined the game` 14:00:30, after "R0 passed".** Commands, all his: `/gamemode survival` 14:00:36; `/rpg selftest living-lantern` 14:00:43; **`/rpg give living_lantern` 14:03:29, NOT on the sheet** (after the HEALTH line, so after every reading); `/stop` 14:03:51. Chat: `yes` (14:03:08, the ASK) |
| CS 9. after `/stop` | `rpgfight` entries: 0; overworld **106** files (step 2: 106; the crafting table's chunk added none); `[14:03:52] … All dimensions are saved`; 0 `java.exe` |

### R0 — ALL AS PREDICTED

- **R0a.** `[13:59:41] [Server thread/INFO]: [Rpg] Build: 42ef53c8`. Not `-dirty`, not `unknown`. The jar's
  `build-id.properties`: `commit=42ef53c8`.
- **R0bR2.** The scan as written, on this boot's jar (deployed 13:59:35): **thirteen PRESENT**, then **`ABSENT
  living_lantern.yml CONTROL-1 '    cooldown_ticks: 1'`** and **`PRESENT knell_beam.yml CONTROL-EOL '    cooldown_ticks:
  48'`**.
- **R0c.** The Loaded line word for word, once; `Mint-on-craft: 36 indexed, 36 claiming, of 51 gear definitions`;
  `Convert-on-arrival: 7 materials {…}`. The loot, fight, world-damage and smithing lines **byte-identical** (timestamps
  stripped, 4 lines, `cmp`) to the first boot's (`run/logs/2026-11-22-2.log.gz`, `Build: d98c28bb`). No `Refusing`,
  `Skipping`, `SEVERE`, `Exception`, `Recipe '`, `No fights loaded`; `[Rpg]` WARN only `volley_stone`'s pair; Paper's
  "4 release(s) behind" block is not `[Rpg]`.
- **R0d.** `[Rpg] Selftest: dev server, 280 scenarios across 37 gates [melee-cell, …, knell-beam, living-lantern]`, once;
  control `267 scenarios across 36 gates`: 0.
- **R0e.** `[Rpg] FIGHTWORLD sweep path=C:\…\run\world\dimensions\rpgfight folders=0`, once; no `deleting`, no `orphan`.

### THE ROWS — `SELFTEST RUN START living-lantern rows=13 selftest=42ef53c8`, 14:00:43; `RUN FINISHED` 14:03:08

**Each row's lines are between its `START` and `DONE` in `run/logs/latest.log`; quoted here by their witness values.**

- **setup — AUTO PASS (selftest 42ef53c8).** `LEFTOVER living=0 radius=16`; the `time set 18000` and `spawn_mobs false`
  REPLYs; **`SELF customHP=126.000/350.000`** (c0 = 126, m0 = 350); 27 `CMD /kill` lines, each with a `SETTLED dying=0`
  line and every REPLY `"No entity was found" [red]`; `REPLY "Set 1 level(s) for BaronVonYeetus (now level 1, 0/1000
  into it, 0 lifetime)." [green]`; **`SELF customHP=100.000/100.000`**: m0 - m1 = 250, c1 = min(126, 100) = 100.
- **LL0 — AUTO PASS.** `LEFTOVER living=0`; `Teleported BaronVonYeetus to -114.500000, 63.000000, 16.500000`; `SELF
  customHP=100.000/100.000`; **`BLOCKS AIR count=480`**, **`BLOCKS AIR count=0`**, **`BLOCKS WATER count=0`**. The spot was
  open and flat.
- **LL1 — AUTO PASS (server half).** `Given Living Lantern`; `INVENTORY living_lantern slots=[0x1]`; `DRIVE hold … 3->0`;
  `Set Gear Score: 100`; the **`HELD` line word for word as predicted**, `Cooldown: 0.8s | Mana Cost: 12`. What he sees is
  the client's (*WHAT THIS GATE DOES NOT READ*).
- **LL2 — AUTO PASS.** `rpg enchant show`: three slots, `raw: v1;…:-1;…:-1;…:-1` (every active choice -1); `MOBSEED …
  gs=100 … max=100`; **`MANA current=100.000` → `DRIVE use … cadence=excluded` → `MANA current=88.000`**; probe at
  press + 3 **`100.000/100.000`**; one **`PLAYERHIT … element=fire sent=14.000 crit=false triggerScore=100`**; probe at
  + 8 **`86.000/100.000`**; one **`DOTTICK … sent=5.000 cap=7.000`**; probe at + 30 **`81.000/100.000`**.
- **LL3 — AUTO PASS (the crit arm).** `POSITIONS ZOMBIE n=2 at=[2.30 0.00 1.93; 2.82 0.00 1.03]`; one PLAYERHIT, on the
  FIRST `MOBSEED` (A, `c076aede`), **`sent=28.000 crit=true`**; **A `72.000/100.000`**, **B `100.000/100.000`**.
- **LL4 — AUTO PASS.** One PLAYERHIT on the first `MOBSEED` (at 5, `afd6a9ad`), `sent=14.000 crit=false`; **first
  `86.000/100.000` (d=5.000)**, **second `100.000/100.000` (d=7.500)**.
- **LL5 — AUTO PASS.** Three PLAYERHITs, each target once, in the order **`3d89e1e2` (2nd MOBSEED, Z2), `5e253602` (3rd,
  Z3), `8db8e7d4` (1st, Z1)**, all `sent=14.000 crit=false`; first probe **all three `86.000/100.000`**; three `DOTTICK`s
  at ticks **1609, 1610, 1612** (offsets 0, 1, 3: the predicted + 24, 25, 27); second probe **all three `81.000`**.
- **LL6 — AUTO PASS.** `The game time is 20382431 tick(s)`; **`n` `100.000` then PLAYERHIT then `86.000`**; **`f`
  `100.000` then PLAYERHIT then `86.000`**; two PLAYERHITs, `n`'s first, both 14.000. The probes bracket each hit on the
  predicted ticks, so **`n` was struck at press + 4 or 5, `f` at + 7 or 8**.
- **LL7 — AUTO PASS.** `STAGE saved 2 blocks for the restore, box=2 0 0 2 1 0`; `Successfully filled 2 block(s)`; **0
  PLAYERHIT**; probe **`100.000/100.000`** (d=4.000). LL8's `CLEAN blocks=2` put the stone back.
- **LL8 — AUTO PASS.** `MANA current=100.000`; press 1, **`g1` = 20382582**; press 2, **`MANA current=88.600`**, **`"On
  cooldown for 0.2s" [gray]`**, **`g2` = 20382594**; press 3, **`MANA current=76.800`**, **`g3` = 20382598**. **`g2 - g1 =
  12`, `g3 - g1 = 16`.** Two PLAYERHITs, both 14.000; **`72.000/100.000`**, one `DOTTICK` (5.000), **`67.000/100.000`**.
- **KB2R2 — AUTO PASS.** `The game time is 20382624 tick(s)`; probe at + 24 **`100.000/100.000`**, logged BEFORE the
  PLAYERHIT line (the order the gate did not predict; INFERRED "probe first" holds); **`PLAYERHIT …
  source=knell_beam/right_click … element=void sent=63.000 crit=false`**; probe at + 25 **`100.000/100.000`**; probe at
  + 26 **`37.000/100.000`**. **KB2 is now READ: the health write lands on press + 26, not + 25.**
- **CR3 — AUTO PASS (the "he crafts" arm).** `Removed 8 item(s)` (the rows' eight lanterns); `INVENTORY living_lantern
  slots=[]`; three `Gave` REPLYs; the TELL. **`CRAFT minted player=BaronVonYeetus weapon=living_lantern material=lantern
  score=100`** (14:01:25, 8 s after the TELL). After the 60 s: `INVENTORY living_lantern slots=[1x1]`; `DRIVE hold …
  3->1`; the `HELD` line with **`"Gear Score: 100"`, EQUAL to the CRAFT line's `score=100`**, and `"Fire Damage: 14"`
  (14 x 100/100). Recorded beside it: in the 60 s he right-clicked the lantern (one `"On cooldown for 0.2s"`, seven
  `"Not enough mana: 12 needed, …"`), **0 PLAYERHIT** in the row. Two endermen (`gs=109`, `111`, `source=stored`) and
  three turtles were seeded at 14:02:13 as chunks loaded, and removed at 14:02:24: no line records them near him, and
  no hurt line exists in this log. **Where he placed the table was not read.**
- **HF1R3 — AUTO PASS; THE VERDICT BY THE TABLE: INCONCLUSIVE.** `Removed 1 item(s)` (lantern); `Removed 2 item(s)`
  (echo shard: the row's Knell Beam and one of his own, which the run's restore puts back by `restoreInventory`, READ
  source, not seen in the log); `Given Crystal Shard Staff`; **`STAGE hold_only moved crystal_shard_staff from hotbar 2
  to slot 9`**, **`moved nether_star from hotbar 8 to slot 10`**, **`STAGE hold_only hotbar items=1 offhand=empty`**;
  `DRIVE hold … 1->0 item=crystal_shard_staff`.
  - **The first `rpg firerate`: the arm for weapons of his own.** `Fire cadence -- 2 weapons sampled, one reading
    each: living_lantern, crystal_shard_staff`; **`(19 selftest-driven records kept out of every sample)`**;
    living_lantern `INPUTS count 19 … min 6t`, `FIRES count 11 … min 18t`, `cooldown_ticks 16 authored, 16 effective`;
    crystal_shard_staff one input and one fire, `NO REPEAT … 68t elapsed` (his own, in CR3's window); `Sample cleared.`
  - **`HELDID slot=0 item=crystal_shard_staff offhand=empty`** at the start and **the same** at the end of the 15 s.
  - **The second `rpg firerate`:** `Fire cadence -- crystal_shard_staff, right_click`; **`INPUTS count 55 window 216t
    mean 4.00t min 4t`**; **`FIRES count 13 window 192t mean 16.00t min 16t`**; `cooldown_ticks 14 authored, 14
    effective (not a basic attack -- attack speed does not scale it)`; the verdict line `COOLDOWN-LIMITED …`; `Sample
    cleared.`; no `weapons sampled` line, no excluded line.
  - **By `GATE-crystal-shard-staff.md`'s table: `FIRES min 16`, `INPUTS min 4`: INCONCLUSIVE.** HF1 is read, held
    correctly, on the right staff, and cannot tell the `ceil(n/4) x 4` fit from a coincidence at 16. **Held fire on a
    14-tick cooldown fires every 16 ticks** (FIRES mean = min = 16, 13 fires): that much is measured.
- **LL9 — AUTO PASS (server half); ASK READ.** Six `MOBSEED` lines; the TELL; **19 PLAYERHITs, his own: 12 at
  `sent=14.000 crit=false`, 7 at `sent=28.000 crit=true`**, none other; all six zombies removed (14:02:43 to 14:02:51);
  then **`ASK 1 question="Did the flames reach as far and as wide as they looked?" answer="yes"`**, verbatim (his chat
  line: `<BaronVonYeetus> yes`).
  - **RECORDED, NOT PREDICTED, FOR THE SEAT: six `DOTTICK` lines at `sent=3.000 cap=3.000`** (on `bfed67cc`,
    `126cacaf`, `cca65717`), the first at tick 3630, in the second (14:02:43) the first two zombies' removals are logged;
    the tick each died is not logged, so the gap from a death is NOT measured. Every scorch line
    before it read `cap=7.000`. `3 = 6 x 0.5` fits Ignite's authored 6 (INFERRED; no line names the ignite or its hits).
    **Two things follow if that is Ignite, both NOT READ:** (a) ruling 5 above says Ignite detonates on killing an
    UNSCORCHED body, and every zombie that died here was burning (each had `DOTTICK`s before its death); (b) on
    `cca65717` a `cap=7.000` burn (tick 3644) was followed by `cap=3.000` burns (3664 to 3704) until his next hit put it
    back to 7 (3724): **a weaker hit LOWERED a burning body's cap.** Neither is a prediction of this gate, and no
    code is changed.
  - Ruling 6: where he placed the crafting table was not read, and **nothing in HF1R3 or LL9 is called a miss**.
- **THE END OF THE RUN — NOT AS PREDICTED** (first section above). The HEALTH line did land, 1 s after `RESTORED`; he
  issued `/stop` 42 s after it.

### WHICH ARMS ARE UNREAD

- **The crit arm** of LL2, LL4, LL5, LL6, LL8 and KB2R2 (each read non-crit); **the non-crit arm** of LL3 (read crit).
- **CR3's "he does not craft" arm** (he crafted).
- **HF1R3:** the `too few empty inventory slots` arm; the first `rpg firerate`'s `No fire-cadence sample` arm (he had
  fired his own); the "did not hold" arm; and **every conclusive arm of the table** (`INPUTS min 3`, `FIRES min 14/15`,
  below 14): the outcome was the inconclusive one.
- **The END's `wrote=none` arm** (he began above 100 + G).
- **LL9's `NOT STATED` arm** (he answered).
- **The client, everywhere** (*WHAT THIS GATE DOES NOT READ*): his one word "yes" is the only reading of what he saw.
