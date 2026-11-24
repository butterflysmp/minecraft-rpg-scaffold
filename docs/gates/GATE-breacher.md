# GATE — THE BREACHER (feat/breacher), PER-WEAPON AMMUNITION, AND THE QUEUED ITEMS

**Status: NOT RUN.** Written 2026-11-22 on `feat/breacher` (PR 3), stacked on `feat/breacher-ammo` (PR 2), stacked on
`feat/breacher-queued-items` (PR 1), on master `90482c94` (the squash of #233), before any boot. **This file carries the
ONE R0 and the ONE run sheet for the three.**

**Status, 2026-11-22, 16:15-16:18: BOOTED ONCE at `7a5e9286`, R0 PASSED, ALL 14 ROWS RAN** (BR10's tooltip half SKIPPED:
he crafted, then the Breacher left his inventory). **NOT AS PREDICTED: BR0's `WATER` count (86, not 0); the action-bar
notices predicted as `REPLY actionbar` lines in BR6, BR7 and BR8 (the instrument captures none); BR6's first press refused
by the cooldown BR5 left (so its third press fired and the empty refusal was not read); BR9's two health probes (`none
within 16 blocks`: the probe's radius).** Readings in *READINGS, 2026-11-22* at the end. No prediction above was edited.

**Seat approval 2026-11-22: code and gate diffed at 7a5e9286, booted at 7a5e9286, readings at 58515ca7. Merge on Ben's
words.** Ben's words, verbatim: "good to merge". Merge order: #234, then #235, then #236.

**Note, 2026-11-22, after the merge: BR10's missing Breacher is CLOSED.** Asked by the seat where the crafted Breacher
went, Ben answered, verbatim: **"2. I dropped it"**. So there is no missing-item defect, and the read of the held-stack
write-back (`Quivers`' `setItemInMainHand` when the held slot changes) that the seat's ruling 7 would have owed is NOT
owed. BR10's reading above is unchanged: the CRAFT line read `score=100`; the tooltip half stays UNREAD and was already
closed by `GATE-living-lantern.md`'s CR3.

**NO BOOT until the seat has diffed the code AND this gate from origin, and Ben says "ready" in his own words**; then
only CC boots, by `.claude/rules/booting.md`. **NO MERGE without Ben's own words.**

**Game mode:** SURVIVAL, every row. **Every row is AUTO** (`/rpg selftest breacher`); **Ben types three lines**, and his
answer to the one question when the selftest asks it. Two gold lines ask him to DO something: BR10 (place a table beside
or behind him and craft one Breacher) and HF2 (hold right-click). **Time:** the setup sets night (18000) and `spawn_mobs
false`; the run's restore puts both back. **Every mob is a frozen NoAI zombie; no fight world.** The setup kills the
natural hostiles within 64 blocks by named type and stages his level at 1, which lowers his health for the run; the
run's end puts it back (*HIS HEALTH*).

**Ben's words, verbatim (2026-11-22, spelling as typed),** recorded in `PLAN-weapon-tiers.md` 1.9 and 1.10:
- "Let's design the Iron tier ranger weapon, The Breacher / 1. Common Rarity / 2. Kinetic / 3. Quiver Size 2 / 4. Instead
  of Arrows it uses cobblestone as ammunition / 5. It fires like a shotgun with a 25 degree and 1s cooldown between shots.
  / 6. Crafted like this: / I = Iron Ingot / G = Gunpowder / S = String / S G I / S G I / X X X"
- To the seat: "1. fine" (7 pellets at 6), "2. Fine" (one cobblestone per round loaded; none, no reload, a message),
  "3. 3 seconds", "4. Fine" (a crossbow), "5. Fine" (fast cobblestone chunks, about 12 blocks), "6. Name should just be
  "Breacher"".
- "Emerald beam meant cursed emerald, yeah we can set the cooldown of the crystal shard staff to 0.8s. Can we also
  increase the range of it", and "half a second is fine".

## WHAT THE THREE PRs DO

**PR 1, `feat/breacher-queued-items`:** `crystal_shard_staff.yml` `cooldown_ticks` 14 -> 16 and `max_lifetime_ticks` 7
-> 10 (30 blocks), each marked BEN; its tests and golden line; `PLAN-weapon-tiers.md` 1.9 ("Emerald Beam" is
`cursed_emerald`); the seat's MEASURED HF1 text in `NEXT.md` and `standing-decisions.md`, and one dated line in
`CLAUDE.md`'s pointer; **THE BURN, READ** (appended to `GATE-living-lantern.md`; nothing fixed).

**PR 2, `feat/breacher-ammo`:** the `ammo:` weapon key (DEFAULT `arrow`; every shipped file unchanged); `QuiverAmmo`
takes the weapon's ammo (its decision made pure: `plan`, `consumes`, `remaining`, `resolve`); `ContentValidator.validateAmmo`
at boot; the no-ammo notice names the weapon's ammo; `RpgListeners.onWeaponCrossbowLoad` (a minted crossbow never
loads); two selftest verbs, **`reload`** (the shipped left-click method, `WeaponSwingListener.swing`) and
**`probe_items: <material>`** (`ITEMS material= count= gear= slots=`, the storage slots a reload draws from).

**PR 3, `feat/breacher`:** `content/weapons/breacher.yml`, `recipes/breacher.yml`, three `breacher_*` visuals,
`BreacherContentTest`, the golden block, `PLAN-weapon-tiers.md` 1.10, this gate and its scenario. **It joins NO loot
pool.**

## THE SEAT PLACEHOLDERS, AND WHAT IS OPEN, FOR BEN (each marked `SEAT PLACEHOLDER 2026-11-22` in the file)

| placeholder | value | why |
|---|---|---|
| the spread's angle | **`angle_degrees: 12.5`**: the RING'S OFFSET from the aim (READ, `SpreadPattern.directionsFor`: each ring direction makes exactly that angle with the aim), so the two horizontal pellets fly 25 degrees apart | Ben's "25 degree", read by the seat as the FULL width |
| the pellets | an ITEM body of `cobblestone`, speed **3.0**, gravity **0**, `max_lifetime_ticks` **4** (12 blocks of path) | "fast flying cobblestone chunks", "about 12 blocks" |
| description | "Seven chunks of cobblestone" / "in a blast 25 degrees wide." | states the blast |
| flavour | "Iron, gunpowder, string," / "and whatever was lying around." | the recipe |
| looks and sounds | cast: `item.crossbow.shoot` 1.0/0.8, `entity.generic.explode` 0.3/1.8, SMOKE; trail: grey DUST 1 a tick; hit: CRIT and `block.stone.hit` 0.5/1.2 (READ in the pinned paper-api's `Sound`) | none from a deleted dev weapon |
| **knockback** | **`strength: 0.1` a pellet: AUTHORED, AGAINST THE BRIEF'S "no knockback"** | **Ben's standing ruling (2026-09-13, CLAUDE.md): a travelling (`type: projectile`) ranged weapon authors knockback.** The seat's placeholder contradicted it; the standing ruling is Ben's, so it is followed and reported. The strength is Dragon's Breath's 0.1 (not ruled either). **OPEN FOR BEN: the rule, the strength, or both** |
| the tooltip's `x 7` | **"Kinetic Damage: 6 x 7"** under the existing rule (READ, `WeaponLoreLines.deliveredShots` prints the spread's count). The full seven-pellet hit exists only inside 0.6 / (2 tan 12.5) = **1.35 blocks** (Dragon's Breath: 5.72 at 3 degrees). **No rule changed** | OPEN FOR BEN |
| Expanded Quiver on a 2-round magazine | +1/+2/+3 is **+50% / +100% / +150%**, the largest relative effect on any shipped weapon | OPEN FOR BEN |
| the iron-tier placement | `PLAN-weapon-tiers.md` 1.10, in Ben's words | recorded |
| the Crystal Shard Staff at 16 | Ben's 0.8 s; whether it stays is his | recorded |

## WHAT WAS READ, EACH MARKED

**What the seat had wrong about ammunition (READ, `QuiverAmmo`, `Quivers.beginReload`).** Nothing beyond what the brief
already corrected: a reload costs plain `ARROW` (exact material) from the STORAGE slots, one per round, taken at the
START of the reload (an interrupted reload keeps none back), free in CREATIVE (supplies the reload, draws nothing), gear
skipped, partial reloads load what there is, and with room and none `QuiverNotice.noAmmo` refuses. **One more fact:**
ADVENTURE and SPECTATOR pay as SURVIVAL does (a `switch` with no default).

**Ruling 3: every other place that assumes arrows (READ), and what changed.**
- **The tooltip** (`WeaponLoreLines`, `Quiver: <loaded>/<capacity>`): counts ROUNDS, never arrows. **Unchanged.**
- **The HUD** (`StatsBarText`): the quiver field's glyph is `➹`, an arrow symbol, beside rounds. **Unchanged** (a glyph,
  listed for Ben).
- **Accessories** (`AccessoryDefinition.MATERIALS`): four inert materials, `ARROW` refused because the debit re-checks
  only the material; cobblestone is not on the list (and is placeable, so never would be). **Unchanged.**
- **Expanded Quiver** (`EnchantEffectLine`): its line reads `+N Quiver Arrows`; on a Breacher that is rounds of
  cobblestone. **Unchanged** (the wording is listed for Ben).
- **Deft Hands** (`ReloadTime.resolve`): the reload's length only, ammo-free. **Unchanged.**
- **The Dragon's Plume** (`PlumeDraw`): a BOW draw (vanilla `BowItem.use` needs an arrow to start the draw, measured in
  `PLAN-dragons-plume.md`) and its quiver reload, which names no `ammo:` and so loads arrows. **Unchanged.**
- **`quiver_size_boost`** (`QuiverSizeModifierItems`): a dev instrument minted as SPECTRAL_ARROW, `+N arrows` in its
  name. **Unchanged.**
- **The no-ammo notice:** CHANGED (ruling 2): it names the weapon's ammo; arrows keep their sentence.
- **`QuiverAmmo`:** CHANGED (ruling 1): the weapon's ammo in place of the constant; `AMMO` (`ARROW`) is the default.

**Ruling 4: the recipe (READ).** Our recipe route (`RecipeLoader` -> `RecipeRegistrar`, a Bukkit `ShapedRecipe` whose
result is the plain material, minted at commit by our crafting table, `InventoryCraft`, logging `CRAFT minted …`) is
the staves' route, and it mints **whatever material the weapon names**: nothing in it assumes a non-durable item.
**A 3x2 shape is accepted**: `RecipeDefinition` takes 1 to 3 rows of equal width (the Crystal Shard Staff ships 1x3).
**It wears like the Boltor**: a crossbow has vanilla durability (`WeaponDurability.maxOf` reads
`Material.getMaxDurability`), and `WEAR_PER_USE` is 1 a use. A redstone Crafter refuses it by recipe key
(`onCrafterCraft`, GUARD TWO, first arm). **No new code.** No vanilla clash (READ, the pinned jar's 1,515 recipe files:
no recipe uses gunpowder with an iron ingot, or with string). A two-row shape matching in either the top two or bottom
two rows: vanilla's shaped matching, INFERRED.

**Ruling 5: the vanilla crossbow (READ).** In the MAIN hand, `RpgListeners.onRightClick` cancels the interaction for a
held weapon that binds `right_click`, which in the pinned paper-api denies the item use, so vanilla's crossbow never
charges. **In the OFF hand it was handed to vanilla** (`getHand() != HAND -> return`; only a stone is cancelled there),
and nothing else stopped a minted crossbow's LOAD (`onManagedBowShot` covers only the Plume's draw). What vanilla then
does (charge, load an arrow from the inventory, fire it) is INFERRED. **PR 2 adds `onWeaponCrossbowLoad`**:
`EntityLoadCrossbowEvent` (cancellable, READ by `javap` in the pinned paper-api) is cancelled for any item carrying a
weapon id, so no minted crossbow loads in either hand. **This also closes the gap for the Boltor, the Locust and
Dragon's Breath.** NO ROW reads it (an off-hand crossbow is a client input the instrument cannot drive): listed below.

**The pattern (EXECUTED, an awk model of `SpreadPattern` and `castRay`'s exact boxes, RAY_SIZE 0).** His eye at 1.62, a
zombie 0.6 x 1.95 x 0.6 at his feet' height, the ring's horizontal pellets 12.5 off the aim and its four diagonals at
60-degree steps (lateral 0.1108 and vertical 0.1920 a block):
- **A zombie 1 block ahead: ALL SEVEN strike**, each at 0.70 to 0.72 blocks of path, in the first tick's segment.
- **At 4 blocks:** the aim pellet strikes C (on the aim), the horizontal ring pellet strikes R (12.5 off), nothing
  strikes O (25 off, the other side). **At the brief's 3 blocks C would ALSO take the two lower diagonal pellets**,
  0.0007 inside its half-width at its front face: a coin toss, so **BR3 stages at 4**.
- **A zombie at 10: the aim pellet strikes it at 9.70; one at 14 is not reached** (the flight ends at 12).

**The staff's reach (EXECUTED, an awk model of `ProjectileFlight.steer`: lerp 0.45 toward the FEET, search 18).** A
10-tick shard strikes a zombie at 25 at x 24.70, y 0.096 above its feet, on its 9th segment; one at 33 is not reached
(it ends at x 29.95). **Control: the old 7-tick lifetime misses at 25.** The ground is not modelled beyond y < 0.

**When a hit lands (READ path, the last step INFERRED):** the press is T; the cast runs on T+1 (`WeaponFire.attempt`'s
`onRegion` hop); a pellet's first segment is traced inline in the cast, its k-th segment on T+1+k; the struck body's
health write is `onEntity`, that tick or the next. Every probe sits clear of both.

**His own pellets cannot strike him (READ):** `ProjectileFlight.step` calls `castRay(position, next, caster.id())` and
`LiveTargets.except` drops his id. **Kinetic accrues nothing** (no burn, no Ignite): only `fire.yml` applies a status.

**THE BURN (PR 1's read): see `GATE-living-lantern.md`, *THE BURN, READ*.** Ignite strikes already-scorched bodies and
its 3.000 cap REPLACES a stronger one (`DotStatus.apply`: "most recent applier owns the cap"). A fix (highest cap wins),
its unit row and its gate row are proposed there; **nothing is fixed; the seat's ruling is awaited.**

## HIS HEALTH

- The setup's first `SELF customHP=<c0>/<m0>` is his own level; after the staging, **`SELF customHP=<c1>/<m1>` with
  `m0 - m1 = 250.000` and `c1 = min(c0, m1)`**, as `GATE-living-lantern.md` read.
- Nothing in the run can lower him (*SAFETY*). Natural regeneration at level 1 is his gear's only.
- **At the end:** `SELFTEST RUN RESTORED … health=<c0>`, then **10 ticks later** `SELFTEST RUN HEALTH saved=<s>
  customHP=<s>/<m0> wrote=<s>`, where **`<s>` is the SNAPSHOT's figure, taken in `SelfTestRun.start` before `RUN START`
  is logged (READ), NOT the setup's first SELF line** (the seat's ruling on `GATE-living-lantern.md`'s second boot: the
  two are different instants; there 124 against 126). If he began at or under `100 + G`, `wrote=none`.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup:** `run/plugins/Rpg` and `run/world` copied to a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-breacher` (**if that name exists, `-2`, `-3`, the first free one, and say so**);
   file and directory counts, `diff -rq lines: 0` for each, the `ls` of `world/dimensions`, the overworld's file count.
   Nothing is restored (Ben's word only).
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND:** "booting now", and Ben told **NOT to log in until told R0 passed.**
6. `./scripts/dev-server.sh --refresh-content`. **Required deployed lines:** `weapons/breacher.yml` holds the line
   `    cooldown_ticks: 20`; `weapons/` holds 22 `.yml` files; `recipes/breacher.yml` and `visuals/breacher_hit.yml` exist.
7. R0a to R0e on **this boot's** log, matched to **this boot's own** `Done` line.
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* `rpgfight` empty; the overworld's file count against step 2's **plus any chunk his crafting
   table landed in** (BR10 leaves it placed); `All dimensions are saved`; no `java` process left.

## THE RUN SHEET — BEN TYPES EXACTLY THESE THREE LINES

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.**
   **Stand on open, flat ground with a clear view for about 35 blocks, and face any way you like** (the rows turn you
   east and put you back on the same spot each time).
   **For the run your level is set to 1, so your health drops to about 100; it is put back at the end.**
1. `/gamemode survival`
2. `/rpg selftest breacher` — **ONCE.** About two and a half minutes plus your craft. Frozen zombies appear; they cannot
   move or hit you. **Three rows ask something of you, in gold chat:** one gives you a crafting table, string, gunpowder
   and iron (**place the table BESIDE OR BEHIND you and craft ONE Breacher**, 60 seconds); one says **HOLD RIGHT-CLICK
   with the Living Lantern** (hold it until your mana runs out, then let go; it is the only thing in your hotbar); and
   the last gives you 30 seconds with the Breacher and cobblestone, then asks a question: **type your answer in chat.**
   Your items, time, level and health are put back at the end.
3. `/stop`

Nothing else. If a row stalls or aborts, do not intervene. **His chat answer to the one question is the only line he
types beyond these three**; "not sure" is NOT STATED.

## THE ARITHMETIC (SAFETY): THE MOST HITS THAT CAN LAND ON BEN TIMES THE BIGGEST HIT, AGAINST THE HEALTH HE HAS IN THE ROW

**His health in every row is `100 + G`** (the setup's second `SELF` line; G is his gear's max health). His defence is
his gear's alone (level 1 grants 0). Below, G = 0 and defence 0: the worst case.

| row | where he is | what can hurt him | most hits x biggest hit | puts him down at 100 + G? |
|---|---|---|---|---|
| setup, BR0, BR1 | the spot he chose | nothing the run makes | 0 | no |
| BR2-BR9 | the run's start, facing east; NoAI zombies 1 to 33 blocks away | a NoAI zombie does not move or attack (INFERRED vanilla: NoAI makes no attack). **His own pellets, shards and bolt cannot strike him** (excluded by id, READ). **Kinetic places no fire and accrues no burn** (READ). Cobblestone and arrows given are inert in an inventory | 0 x 0 = 0 | no |
| BR10 | where he walks to place a table, 60 seconds | natural mobs, as below; nothing he is given can hurt him (a table, string, gunpowder, iron: none has a use that damages; gunpowder is inert unless crafted) | 0 from the run | no |
| HF2 | the run's start | his cone skips him (`ConeShell`, READ); no zombie is in it, so nothing dies and nothing ignites; even an ignite skips players (READ) | 0 | no |
| BR11 | the run's start; six NoAI zombies, 30 seconds in which he may walk | the same; kinetic kills ignite nothing | 0 | no |
| every row | the overworld at night, after the setup's kills | **a natural hostile that was beyond 64 blocks at the kill and closes in** (as `GATE-living-lantern.md`'s table: spawns off) | at gs 100, a zombie about 15 a hit, a close creeper about 215 (vanilla UNVERIFIED) | **at 100:** 7 zombie hits, or one close creeper. Possible, not likely; a death aborts the run and costs no items |

**The run's length** (`wait:` steps summed per row; recount with `tr -d '\r' < paper/src/main/resources/selftest/GATE-breacher.yml
| awk '/^[A-Za-z0-9_-]+:/{row=$1} /^  - wait:/{w[row]+=$3} END{for (r in w) print r, w[r]}'`): setup 40, BR0 0, BR1 10,
BR2 46, BR3 46, BR4 73, BR5 66, BR6 109, BR7 46, BR8 67, BR9 95, BR10 1,200, HF2 300, BR11 600: **2,698**, plus the ASK's 0
to 1,200. Plus about 5 ticks per row-start clean (14 rows: 70) and up to 40 per `kill` step (39: `grep -cE '^\s+- cmd:
"kill '`, 27 in the setup). **Worst case 2,698 + 1,200 + 70 + 1,560 = 5,528 ticks** (4 min 36 s); with nothing to kill,
a prompt craft and a prompt answer, about 2,800 (2 min 20 s).

## R0 — THE BUILD, FOR THE WHOLE STACK. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `feat/breacher`'s head as `git ls-remote` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries all three PRs

**Every value line is checked by `ValueLine`** (GATE-living-lantern's R0bR2 function): the value followed by end of line
or by whitespace and `#`, so a line that carries a comment is matched (three of the lines below do), and its prefix is
not. **Run against the committed files before this gate was committed**: see *R0b, RUN BEFORE COMMIT*, below.

| prediction | instrument | READING |
|---|---|---|
| PRESENT: the entries `content/weapons/breacher.yml`, `content/recipes/breacher.yml`, `content/visuals/breacher_cast.yml`, `content/visuals/breacher_trail.yml`, `content/visuals/breacher_hit.yml`, `selftest/GATE-breacher.yml`; the lines `breacher.yml` `quiver_size: 2` (comment), `ammo: cobblestone` (comment), `    cooldown_ticks: 20`, `        angle_degrees: 12.5`; `crystal_shard_staff.yml` `    cooldown_ticks: 16`, `      max_lifetime_ticks: 10` (PR 1); `GATE-breacher.yml` `HF2:`; `index.yml` `  - breacher`; the classes `rpg/paper/listener/RpgListeners.class` carrying `onWeaponCrossbowLoad`, `rpg/paper/weapon/QuiverNotice.class` carrying ` loads this weapon.`, `rpg/paper/selftest/SelfTestDrivers.class` carrying `ITEMS material=`, `rpg/core/weapon/WeaponDefinition.class` carrying `but no quiver_size, so nothing ever loads it` (PR 2). **The controls:** `content/weapons/knell_beam.yml` PRESENT; `ValueLine` of `    cooldown_ticks: 2` (a prefix of 20) and `      max_lifetime_ticks: 1` (a prefix of 10) ABSENT; `ValueLine` of `quiver_size: 2` against `boltor.yml` (whose line is `quiver_size: 8`) ABSENT | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Entry($name) { if ($zip.GetEntry($name) -eq $null) { "ABSENT  entry $name" } else { "PRESENT entry $name" } }
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
foreach ($e in @('content/weapons/breacher.yml', 'content/recipes/breacher.yml', 'content/visuals/breacher_cast.yml', 'content/visuals/breacher_trail.yml', 'content/visuals/breacher_hit.yml', 'selftest/GATE-breacher.yml', 'content/weapons/knell_beam.yml')) { Entry $e }
$b = Bytes 'content/weapons/breacher.yml'
ValueLine $b 'quiver_size: 2' 'breacher.yml'
ValueLine $b 'ammo: cobblestone' 'breacher.yml'
ValueLine $b '    cooldown_ticks: 20' 'breacher.yml'
ValueLine $b '        angle_degrees: 12.5' 'breacher.yml'
$c = Bytes 'content/weapons/crystal_shard_staff.yml'
ValueLine $c '    cooldown_ticks: 16' 'crystal_shard_staff.yml'
ValueLine $c '      max_lifetime_ticks: 10' 'crystal_shard_staff.yml'
ValueLine (Bytes 'selftest/GATE-breacher.yml') 'HF2:' 'GATE-breacher.yml'
ValueLine (Bytes 'selftest/index.yml') '  - breacher' 'index.yml'
$p = 'io/github/butterflysmp/rpg/'
Has ($p + 'paper/listener/RpgListeners.class') 'onWeaponCrossbowLoad'
Has ($p + 'paper/weapon/QuiverNotice.class') ' loads this weapon.'
Has ($p + 'paper/selftest/SelfTestDrivers.class') 'ITEMS material='
Has ($p + 'core/weapon/WeaponDefinition.class') 'but no quiver_size, so nothing ever loads it'
'--- controls'
ValueLine $b '    cooldown_ticks: 2' 'breacher.yml CONTROL-PREFIX'
ValueLine $c '      max_lifetime_ticks: 1' 'crystal_shard_staff.yml CONTROL-PREFIX'
ValueLine (Bytes 'content/weapons/boltor.yml') 'quiver_size: 2' 'boltor.yml CONTROL-OTHER'
$zip.Dispose()
```

**THE CORE CLASS IS IN THE PLUGIN JAR (READ):** the shaded jar carries `rpg/core/…` classes, as R0b's `ConeShell.class`
check read on `GATE-living-lantern.md`'s boot.

### R0c — every Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| Exactly one of each, up to the first `joined the game`: **`[Rpg] Loaded 11 abilities, 43 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 22 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 6 recipes, 1 ores, 4 vein sizes`** (against `GATE-living-lantern.md` R0c, read AS PREDICTED at `42ef53c8`: **visuals 40 -> 43, weapons 21 -> 22, recipes 5 -> 6**; every other figure unchanged; recount with `ls paper/src/main/resources/content/<dir>/*.yml \| wc -l`); `GATE-fight-revive.md`'s R0c fight line and `GATE-loot-roll.md`'s R0c loot line, word for word (the Breacher joins no pool); the world-damage and smithing lines as at the last boot; **`[Rpg] Mint-on-craft: 36 indexed, 36 claiming, of 52 gear definitions`** (51 -> 52: 22 + 1 + 24 + 5; the Breacher claims no craft_result); `Convert-on-arrival: 7 materials {…}` as before. **No `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Recipe '`; no `[Rpg]` WARN beyond `volley_stone`'s known pair** (a 3x2 recipe is three wide, so no 2x2 warning; every quiver weapon's ammo is carriable, so `validateAmmo` adds nothing) | `Select-String run\logs\latest.log -Pattern 'Loaded\|Mint-on-craft\|Convert-on-arrival\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded\|Recipe '''` | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 293 scenarios across 38 gates [melee-cell, …, living-lantern, breacher]`**. The control: the last boot's `280 scenarios across 37 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (2026-11-22, PR 3's tree; `SelfTestScenariosTest` asserts 293): 38 gates, `^[A-Za-z0-9_-]+:` **331**,
`^setup:` **38**, so **293**. Control: 317 + 14 (setup, BR0-BR11, HF2) = 331.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, no `deleting`, no `orphan` | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | |

## THE ROWS (`/rpg selftest breacher`) — SURVIVAL, ALL AUTO

**EVERY DAMAGE PREDICTION NAMES EVERY TERM** (`.claude/rules/verification.md`):
`sent = (amount x score/100) x enchant + class bonus + level bonus`, then `x charge x crit`, **per pellet**.
**For the Breacher:** `(6 x 100/100) x 1.0 + 0 + 0 = 6`, `x 1.0`, and `x 2.0` on a crit: **`sent=6.000 crit=false` or
`sent=12.000 crit=true`**. Terms: amount 6 (the file); score 100 (`rpg gearscore set 100`, staged); enchant 1.0 (no
active enchant on a given item; BR2's `rpg enchant show` reads it); **class bonus 0 unless BR1's `rpg stats` shows an
active ranger damage bonus, in which case every pellet adds exactly that figure** (an accessory's CLASS_DAMAGE, `HitDamage`
adds it per hit; it is READ on the row, not assumed); level bonus 0 (level 1, staged); charge 1.0. **A spread crits as a
UNIT** (READ, `CastExecutor.launch`: one snapshot, one roll per cast), **so one press's PLAYERHITs are all 6.000 or all
12.000**, never mixed. **For the Crystal Shard Staff (BR9):** `(16 x 100/100) x 1.0 + 0 + 0 = 16`; `sent=16.000` or
`32.000`. A zombie here has `max=100` (`MOBSEED … gs=100 … max=100`) and takes the hit whole (LL2 read 14 -> 86).
**Kinetic adds no burn** (READ). **Any other `sent` is NOT AS PREDICTED.**

**THE PLAYERHIT LINE:** `PLAYERHIT … source=breacher/right_click target=<uuid> zombie element=kinetic sent=6.000
crit=false triggerScore=100` (or `12.000 crit=true`).

### setup — night, no spawns, the natural hostiles killed, his level at 1 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| As `GATE-living-lantern.md`'s setup, line for line: its own `LEFTOVER`; the `time set` and `gamerule` REPLYs; **`SELF customHP=<c0>/<m0> …`**; 27 kill lines, each `CMD /kill …` then `REPLY "No entity was found" [red]`, `"Killed <name>"` or `"Killed <n> entities"`, and a `SETTLED` line (recorded); `REPLY "Set 1 level(s) for BaronVonYeetus (now level 1, 0/1000 into it, 0 lifetime)." [green]`; then **`SELF customHP=<c1>/<m1>` with `m0 - m1 = 250.000` and `c1 = min(c0, m1)`** | *HIS HEALTH* | |

### BR0 — THE SPOT: open and flat? (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`LEFTOVER living=0 radius=16`** or only types not on the kill list. The teleport's REPLY. `SELF …`. **`BLOCKS AIR count=480 box=1 0 -4 30 1 3`**; **`BLOCKS AIR count=0 box=1 -1 -4 30 -1 3`**; **`BLOCKS WATER count=0 box=1 -1 -4 30 -1 3`**. A low corridor count is the spot, not the build, and is read beside BR2-BR11: a block in a pellet's path stops it. **BR9's 33-block zombie is beyond the corridor; a block between 30 and 33 is not probed** | `SelfTestDrivers.probeBlocks` | |

### BR1 — given, held, its tooltip, and his stats (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| REPLY `Given Breacher`; `INVENTORY breacher slots=[…]`; `DRIVE hold …`; the gearscore REPLY; then **`HELD breacher CROSSBOW x1 name="Breacher" [white] italic lore=["Kinetic" [white]; "Gear Score: 100" [gray, aqua]; "Quiver: 2/2" [gray]; "" []; "Breacher  Right-Click" [gold, yellow]; "Seven chunks of cobblestone" [gray]; "in a blast 25 degrees wide." [gray]; "Kinetic Damage: 6 x 7" [gray, red]; "Cooldown: 1.0s" [dark_gray]; "" []; "Iron, gunpowder, string," [gray] italic; "and whatever was lying around." [gray] italic; "" []; "Common Ranged Weapon" [white]]`** (the colours as `golden-lore.txt` renders them; the italic flags as `I=T` there). **No mana line** (no cost). Then `rpg stats`'s REPLY lines, **recorded**: the class-bonus term for BR2-BR5 is read off them | `BreacherContentTest.aMintedBreachersTooltipReads` (the same fourteen lines from the real file); `golden-lore.txt`; the mint stamps a full magazine (`WeaponItems.mint`: `QuiverItems.stampFull`, READ) | |

### BR2 — ONE SHOT at a zombie 1 block ahead: seven pellets, the quiver 2 -> 1 (SURVIVAL)

Its zombie's front face is 0.70 blocks from his eye; **all seven pellets strike it** (EXECUTED, *The pattern*), all in the
cast's first segment, so **press + 1** (the write that tick or the next).

| prediction | stands on | READING |
|---|---|---|
| `rpg enchant show`: every slot's active choice `-1`. `MOBSEED … zombie gs=100 … max=100`. Probe before the press: **`customHP=100.000/100.000`**. `DRIVE use … item=breacher … cadence=excluded`. **Exactly SEVEN `PLAYERHIT … source=breacher/right_click target=<the one zombie> zombie element=kinetic sent=6.000 crit=false triggerScore=100`** (or all seven `12.000 crit=true`). Probe at **press + 6**: **`58.000/100.000`** (100 - 7 x 6; **16.000** on a crit). Then the dump's lore reads **`"Quiver: 1/2"`** (one round spent, `Quivers.spendRounds`, the lore refreshed by `QuiverItems.setLoaded`, READ) | *The pattern*; the sum above | |

### BR3 — THE PATTERN: on the aim and on the ring are struck once each; 25 degrees off is not (SURVIVAL)

At **4 blocks**, not the brief's 3, and why: *The pattern* (at 3 the centre zombie would take two diagonal pellets by
0.0007 of a block). **C** at `~4 ~ ~0` (on the aim), **R** at `~3.9052 ~ ~0.8658` (4 blocks out, 12.5 degrees: the
horizontal ring pellet's line), **O** at `~3.6252 ~ ~-1.6905` (4 out, 25 degrees, the other side). The ring is symmetric
left and right, so which side is the shooter's right does not change any prediction.

| prediction | stands on | READING |
|---|---|---|
| `POSITIONS ZOMBIE n=3` holding `4.00 0.00 0.00`, `3.91 0.00 0.87` and `3.63 0.00 -1.69`. **Exactly TWO PLAYERHITs**: one on the FIRST `MOBSEED` (C) and one on the SECOND (R), each `sent=6.000` (both `12.000` on a crit). **None on O.** Probe at press + 6: **C `94.000/100.000`, R `94.000/100.000`** (88 and 88 on a crit), **O `100.000/100.000`** | *The pattern*; `SpreadPattern.directionsFor` | |

### BR4 — THE REACH: struck at 10, not at 14 (SURVIVAL)

Two presses 25 ticks apart (the cooldown is 20), each with ONE zombie in the corridor, so the far one is never shadowed by
the near. The aim pellet's 4th segment (9 to 12 blocks) meets the zombie at 10 at 9.70: **press + 4** (or + 5).

| prediction | stands on | READING |
|---|---|---|
| Press 1: **exactly ONE PLAYERHIT, on `n`**, `sent=6.000` (or 12.000); probe at press 1 + 8: **`n` `94.000/100.000`** (88). Press 2: **NO PLAYERHIT**; probe at press 2 + 8: **`f` `100.000/100.000`**. **Control:** press 1 reached 10 with the same flight, so press 2's miss is the reach, not a dead weapon | *The pattern*; `ProjectileFlight.step` (4 segments of 3) | |

> **DATED NOTE, 2026-11-24 (`feat/breacher-reach`; Ben: "I want to increase the range that the cobble travels normally
> to 18 blocks"): BR4 CANNOT BE RE-RUN AS WRITTEN.** The chunks now fly 6 ticks at 3.0, 18 blocks (was 4 ticks, 12), so
> BR4's far zombie at 14 is inside the reach and would be struck. **Its readings stand as read**, of the 12-block flight
> it was predicted on. The 18-block reach is read by `GATE-shatter-caps.md`.

### BR5 — THE COOLDOWN: refused at 16, fires at 20 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Press 1 fires; **`REPLY "The game time is <g1> tick(s)"`**. Press 2, 16 ticks later: **`REPLY "On cooldown for 0.2s" [gray]`** (4 of 20 left; a literal-damage cast is not a basic attack, so the refusal speaks, as LL8 read), `<g2>`. Press 3, 4 ticks after that: fires, no refusal, `<g3>`. **`g2 - g1 = 16`, `g3 - g1 = 20`.** **Exactly two PLAYERHITs**, both on `t` (only the aim pellet reaches a zombie at 4: *The pattern*), each `6.000` (each press rolls its own crit: `12.000` possible on either). Probe at press 3 + 6: **`88.000/100.000`** (100 - 6 - 6; 82 with one crit, 76 with two). The dump: **`"Quiver: 0/2"`** (the refused press spent nothing: rounds are spent only on Success, READ `WeaponFire`) | `AbilityService` stamps at the click, `isReady` is `>=` (READ, `GATE-knell-beam.md`); `"On cooldown for %.1fs"` over 4/20 = 0.2 | |

### BR6 — EMPTY, THEN A RELOAD THAT TAKES EXACTLY 2 COBBLESTONE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Two `clear` REPLYs (his cobblestone and arrows, if any, put back at the run's end). Presses 1 and 2 fire (no target: no PLAYERHIT); press 3: **`REPLY actionbar "Your quiver is empty -- left-click to reload." [gray]`**, no fire; the dump: **`"Quiver: 0/2"`**. `Gave 5 [Cobblestone]`; **`ITEMS material=cobblestone count=5 gear=0 slots=[<i>:5]`**; **`DRIVE reload held=breacher`**; **`REPLY actionbar "Reloading..." [gray]`**; the trace's **`RELOAD player=<uuid> weapon=breacher authored=60 resolved=60 …`** (resolved 60 unless his gear grants a reload bonus: then the figure his gear gives, recorded); **`ITEMS material=cobblestone count=3 gear=0 slots=[<i>:3]`** (taken at the START, READ `Quivers.beginReload`). Dump at reload + 30: **`"Quiver: 0/2"`** (pending; the rounds land at maturity). Dump at reload + 65: **`"Quiver: 2/2"`** (the cue settles it at the deadline, `QuiverReloadCue` -> `Quivers.settleMaturedReload`, READ) | PR 2's `PerWeaponAmmoTest` (cobblestone, not arrows; the debit by slot); `Quivers.beginReload` | |

### BR7 — NO AMMO: refused, the notice names cobblestone, the arrows untouched (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Two presses fire (no target); `Gave 16 [Arrow]`; **`ITEMS material=arrow count=16 gear=0 slots=[<i>:16]`**; **`ITEMS material=cobblestone count=0 gear=0 slots=[]`**; **`DRIVE reload held=breacher`**; **`REPLY actionbar "You have no cobblestone -- Cobblestone loads this weapon." [gray]`**; **no `RELOAD` trace line**; then **`ITEMS material=arrow count=16 …`** (untouched) and **`ITEMS material=cobblestone count=0 …`**; the dump: **`"Quiver: 0/2"`** | `QuiverState.reloadVerdict` (room, zero rounds -> `NO_AMMO`, READ); `PerWeaponAmmoTest.theNoticeNamesTheItem`, `…ArrowsAlone…` | |

### BR8 — THE CONTROL: the Boltor takes ONE ARROW and leaves the cobblestone (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `Gave 5 [Cobblestone]`, `Gave 16 [Arrow]`, `Given Boltor`; one press (its ray: a PLAYERHIT only if a leftover zombie stands on it, recorded); **`ITEMS material=arrow count=16 …`**, **`ITEMS material=cobblestone count=5 …`**; `DRIVE reload held=boltor`; **`REPLY actionbar "Reloading..." [gray]`**; **`RELOAD … weapon=boltor authored=60 …`**; **`ITEMS material=arrow count=15 …`** and **`ITEMS material=cobblestone count=5 …`** (the Boltor names no `ammo:`, so it loads arrows, exactly as before PR 2). Dump at reload + 65: **`"Quiver: 8/8"`** | `PerWeaponAmmoTest.aWeaponNamingNothingReloadsFromArrowsNotCobblestone` | |

### BR9 — THE STAFF: 0.8 s in its tooltip; struck at 25, not at 33 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The dump: **`"Kinetic Damage: 16"`** and **`"Cooldown: 0.8s \| Mana Cost: 8"`** (PR 1; was 0.7s). Press 1: **exactly ONE PLAYERHIT, on `n`**, `source=crystal_shard_staff/right_click … element=kinetic sent=16.000` (or 32.000), its shard reaching 24.70 on its 9th segment (*The staff's reach*), **press + 9 or + 10**; probe at press 1 + 15: **`n` `84.000/100.000`** (68). Press 2 (30 ticks later; the cooldown is 16): **NO PLAYERHIT**; probe at press 2 + 15: **`f` `100.000/100.000`**. **Control:** press 1 struck at 25, which the old 7-tick shard could not reach | *The staff's reach*; `CrystalShardStaffContentTest` (16, lifetime 10) | |

> **DATED NOTE, 2026-11-23 (the seat's item 6; `ScenarioLint`, rule PROBE_REACH): BR9's PROBES STOOD BEYOND THEIR
> REACH.** The scenario's `probe: n` (25 out) and `probe: f` (33 out) were read with the probe's then-fixed reach of 16,
> so any `PROBE` line this row printed could only say `none within 16 blocks`: neither mob's health was readable by that
> instrument. **The row's readings stand as read**; what rests on its `PLAYERHIT` lines is untouched by this. Since
> `feat/enchant-columns-instrument` a probe states its reach (`probe: "n 27"`); a re-run of BR9 would.

### BR10 — THE CRAFT, AND ITS TOOLTIP (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The `clear` REPLY (the rows' Breachers and Boltor, and any crossbow of his own, put back at the run's end); **`INVENTORY breacher slots=[]`**; four `Gave` REPLYs; the TELL. **If he crafts:** **`CRAFT minted player=BaronVonYeetus weapon=breacher material=crossbow score=<s>`** within the 60 s; after it **`INVENTORY breacher slots=[<k>x1]`**, `DRIVE hold …`, and the `HELD` line with **`"Gear Score: <s>"`, EQUAL to the CRAFT line's `<s>`**, **`"Quiver: 2/2"`**, and **`"Kinetic Damage: <6 x s/100> x 7"`** (at `s = 100`, `6 x 7`). **If he does not:** the hold is SKIPPED by name and the row is UNREAD | `InventoryCraft.craftMintLine`; `BreacherContentTest.bensRecipeMintsIt`; GATE-living-lantern's CR3 | |

### HF2 — HELD FIRE ON THE LIVING LANTERN, ONLY THE LANTERN IN HIS HOTBAR (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Four `clear` REPLYs; `Given Living Lantern`; `STAGE hold_only moved …` for each other hotbar item (or none), then **`STAGE hold_only hotbar items=1 offhand=<id>`**, `DRIVE hold … item=living_lantern`. **If too few slots:** `SKIPPED … too few empty inventory slots`: UNREAD. The first `rpg firerate`: **`No fire-cadence sample. … (28 selftest-driven records kept out of every sample)`** (28: INPUT and FIRE for each firing `use:` press, 13 of them, and INPUT alone for BR5's refused and BR6's empty press; a `reload:` records nothing, since neither crossbow binds `left_click`, READ `WeaponFire.attempt`) **or**, if he fired weapons of his own after logging in, one reading per weapon he used, the excluded line, and `Sample cleared.`. **`HELDID slot=<k> item=living_lantern offhand=<id>`** at the start and **the same** at the end. The second `rpg firerate`: **`Fire cadence -- living_lantern, right_click`**, `INPUTS count … min <i>t`, `FIRES count … min <f>t`, **`cooldown_ticks 16 authored, 16 effective (not a basic attack -- attack speed does not scale it)`**, a verdict line, `Sample cleared.`. **PREDICTED: `FIRES min 16` with `INPUTS min 4` (or 3): the lantern's held inputs arrive on the 4-tick grid and 16, on it, delivers 16.** **What `FIRES min 18` with `INPUTS min 6` would mean:** this item's held inputs arrive every 6 ticks, not 4, so the grid is not a property of the button alone; at 6 the inputs fall at 0, 6, 12, 18 and the first one at or after 16 is 18: **the multiples-of-4 rule would not hold for this item**, and the tooltip's 0.8s would promise 0.8 while it delivers 0.9. **If he did not hold:** `No fire-cadence sample.`: UNREAD | `GATE-living-lantern.md` HF1R3; PR 1's MEASURED text | |

### BR11 — A FREE GO, THEN THE ONE QUESTION (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Six `MOBSEED` lines; `Gave 64 [Cobblestone]`; the TELL; 30 seconds in which any PLAYERHITs are his, each `sent=6.000` or `12.000` (recorded, none predicted: he turns, walks and reloads), and any `RELOAD … weapon=breacher` lines are his left-clicks; then **`ASK 1 question="Did the blast spread about as wide as you expected?" answer="<his words>"`**, verbatim, or `answer=NOT STATED after 1200 ticks` | `SelfTestAsk` | |

## R0b, RUN BEFORE COMMIT (the brief: "run every R0 scan line against the files as committed")

Run 2026-11-22 against a jar built from this tree (`./mvnw -q package -DskipTests`, `paper/target/rpg-0.1.0-SNAPSHOT.jar`),
with `ValueLine` checked against lines that carry a comment. **Its printed lines are in the PR 3 commit body**, so this
file is not edited after the run (a gate's predictions are written before the boot, and this section records only that
the scan was run, where, and what it printed, in the commit that carries the gate).

## WHAT THIS GATE DOES NOT READ

- **The off-hand crossbow guard** (`onWeaponCrossbowLoad`): no row loads a crossbow in the off hand; the instrument drives
  `RIGHT_CLICK_AIR, HAND` only. Its only witness is `WeaponCrossbowLoadGuardTest`, a source read.
- **The client:** the spread as he sees it (his one answer is the only reading), the cobblestone bodies, the trail, the
  hit sparks, every sound, the HUD's `➹` field.
- **Knockback:** every zombie is frozen (a NoAI mob is not moved by a push, `PROBE_MOTION`'s note), so its 0.1 a pellet
  is not read. Ruled by Ben's standing rule; its strength is OPEN.
- **Durability:** a shot's wear (`WEAR_PER_USE` 1) is not read; the Boltor's is the same mechanism.
- **Creative:** every row is survival; creative's free reload is `PerWeaponAmmoTest.creativeIsFree`, a unit row.
- **`validateAmmo` firing:** no shipped content names a bad ammo, so R0c predicts its silence; its arm is caused by
  `WeaponLoaderTest.anAmmoNoItemAnswersToIsNamedAtBoot`.
- **The two-row recipe in the TOP two rows:** the gold line asks for Ben's own layout (top two, bottom empty); the other
  placement is vanilla's slide, INFERRED.

## READINGS, 2026-11-22 (the boot of `7a5e9286`)

Boot approved by the seat's message of 2026-11-22 on Ben's own "ready". Read from `run/logs/latest.log` of this boot; the
jar read in place (`run\plugins\rpg-0.1.0-SNAPSHOT.jar`). **Readings count only after his login (16:15:45).**

### NOT AS PREDICTED (first)

- **BR0 `WATER`: `BLOCKS WATER count=86 box=1 -1 -4 30 -1 3`** against the predicted 0. The FLOOR of the corridor holds 86
  water blocks; `BLOCKS AIR count=0` for the floor (as predicted) and `BLOCKS AIR count=480` for the corridor (as
  predicted). **Its effect on the rows: none read.** Water under his feet' level is below every pellet's, shard's and
  bolt's path (all leave his eye, 1.62 up, level or near level), and every hit row read as predicted. Recorded as the
  spot, not the build.
- **The action-bar notices are NOT captured by the instrument.** The gate predicted `REPLY actionbar "Your quiver is
  empty -- left-click to reload."` (BR6), `REPLY actionbar "Reloading..."` (BR6, BR8) and `REPLY actionbar "You have no
  cobblestone -- Cobblestone loads this weapon."` (BR7). **No action-bar REPLY appears anywhere in the run.**
  The cause, READ in our source: `ReplyReader` returns on any packet but `SYSTEM_CHAT_MESSAGE`; that `Player#sendActionBar`
  goes out as the separate action-bar packet is INFERRED (Paper not read). **So the notice text is UNREAD on a server**; the REFUSAL itself
  is read (BR7: no `RELOAD` line, the arrows untouched, `Quiver: 0/2`), and the wording is `PerWeaponAmmoTest`'s.
- **BR6's first press was refused by the cooldown BR5 left:** `REPLY "On cooldown for 0.5s" [gray]` (BR5's third press
  fired about 10 ticks before; the cooldown is per player and ability, not per item). So BR6's second and third presses
  fired (`Quiver: 0/2` at the dump, as predicted), and **the empty-magazine refusal on a third press was NOT READ.** The
  gate's prediction missed the carried cooldown.
- **BR9's health probes: `PROBE tag=n none within 16 blocks`, `PROBE tag=f none within 16 blocks`.** `PROBE` reads within
  16 blocks (its own javadoc); the zombies stood at 25 and 33. **The hit and the miss are still read** by the PLAYERHIT
  lines (below); the `84.000` and `100.000` figures are UNREAD.

### R0 (read before he logged in)

- **R0a — AS PREDICTED.** `[Rpg] Build: 7a5e9286`.
- **R0b — AS PREDICTED.** The scan as committed, on the deployed jar: all 19 PRESENT (the two comment-carrying lines
  `quiver_size: 2` and `ammo: cobblestone` included); the controls `CONTROL-PREFIX '    cooldown_ticks: 2'`,
  `CONTROL-PREFIX '      max_lifetime_ticks: 1'` and `CONTROL-OTHER 'quiver_size: 2'` (boltor.yml) ABSENT.
- **R0c — AS PREDICTED.** `[Rpg] Loaded 11 abilities, 43 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6
  fragments, 6 aspects, 22 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 6 recipes, 1
  ores, 4 vein sizes`; `Mint-on-craft: 36 indexed, 36 claiming, of 52 gear definitions`; the loot, fight, world-damage,
  smithing and convert-on-arrival lines word for word those of the previous boot's archived log
  (`2026-11-22-3.log.gz`); `[Rpg]` WARN only `volley_stone`'s known pair; no `Refusing`, `Skipping`, `SEVERE`,
  `Exception`, `Recipe '`.
- **R0d — AS PREDICTED.** `[Rpg] Selftest: dev server, 293 scenarios across 38 gates [… living-lantern, breacher]`; the
  control `280 scenarios across 37 gates` ABSENT.
- **R0e — AS PREDICTED.** `FIGHTWORLD sweep path=…\run\world\dimensions\rpgfight folders=0`; no `deleting`, no `orphan`.
- Carried state: `Done` at 16:15:03; the deployed `breacher.yml` line 58 `    cooldown_ticks: 20`; 22 weapon files;
  `recipes/breacher.yml` and `visuals/breacher_hit.yml` present. **His login, 16:15:45, after R0 was read.**

### HIS COMMANDS, AGAINST THE SHEET

`/gamemode survival` (16:15:53), `/rpg selftest breacher` (16:15:58), **`/rpg give crystal_shard_staff` (16:17:33, NOT
ON THE SHEET**, inside BR10's window; HF2's `clear @s minecraft:amethyst_shard` then removed one item, and the run's
restore put his RUN START inventory back), `/stop` (16:18:38). And his one chat line, the answer below.

### THE ROWS

- **setup — AS PREDICTED.** `SELF customHP=126.000/350.000`, then after the staging `SELF customHP=100.000/100.000`:
  `350 - 100 = 250`, `100 = min(126, 100)`. `Set 1 level(s) for BaronVonYeetus (now level 1, 0/1000 into it, 0
  lifetime).` [green].
- **BR0 — AIR as predicted; WATER NOT AS PREDICTED** (above). `LEFTOVER living=0 radius=16`.
- **BR1 — AS PREDICTED.** `HELD breacher CROSSBOW x1 name="Breacher" [white] italic lore=["Kinetic" [white]; "Gear
  Score: 100" [gray, aqua]; "Quiver: 2/2" [gray]; "" []; "Breacher  Right-Click" [gold, yellow]; "Seven chunks of
  cobblestone" [gray]; "in a blast 25 degrees wide." [gray]; "Kinetic Damage: 6 x 7" [gray, red]; "Cooldown: 1.0s"
  [dark_gray]; "" []; "Iron, gunpowder, string," [gray] italic; "and whatever was lying around." [gray] italic; "" [];
  "Common Ranged Weapon" [white]]`. `rpg stats`: `⚔ Damage 0.00`, `Crit Chance 15%`, `Quiver 2`, `Reload 60t (3.00s)`:
  **the class-bonus term is 0.**
- **BR2 — AS PREDICTED.** `rpg enchant show`: `raw: v1;power=0,deft_hands=0,expanded_quiver=0:-1;power=0:-1;power=0:-1`
  (every active choice -1). Probe before: `100.000/100.000`. **Exactly seven `PLAYERHIT … source=breacher/right_click
  target=9a33ff37… zombie element=kinetic sent=6.000 crit=false triggerScore=100`.** Probe at press + 6: **`58.000/100.000`**.
  The dump: **`"Quiver: 1/2"`**.
- **BR3 — AS PREDICTED.** `POSITIONS ZOMBIE n=3 at=[4.00 0.00 0.00; 3.91 0.00 0.87; 3.63 0.00 -1.69]`. Exactly two
  PLAYERHITs, on C (`2a41764c`, the first MOBSEED) and R (`87e654fe`, the second), each `sent=6.000`; none on O. Probes:
  C `94.000`, R `94.000`, O (`e6a5a320`) `100.000`.
- **BR4 — AS PREDICTED.** Press 1: one PLAYERHIT on `n` (`3d7b0dcd`), `sent=6.000`; `n` `94.000/100.000` d=10.000. Press 2:
  no PLAYERHIT; `f` (`97586ff3`) `100.000/100.000` d=14.000.
- **BR5 — AS PREDICTED.** `The game time is 20387150`, then `REPLY "On cooldown for 0.2s" [gray]` and `20387166`, then
  `20387170`: **16 and 20**. Exactly two PLAYERHITs on `92b64a41`, each `6.000`. Probe at press 3 + 6: **`88.000/100.000`**.
  The dump: **`"Quiver: 0/2"`**.
- **BR6 — the reload AS PREDICTED; the presses NOT AS PREDICTED** (above). `On cooldown for 0.5s` on press 1; the dump
  `"Quiver: 0/2"`. `Gave 5 [Cobblestone] to BaronVonYeetus`; **`ITEMS material=cobblestone count=5 gear=0 slots=[2:5]`**;
  `DRIVE reload held=breacher`; **`RELOAD player=b6ae27e9… weapon=breacher authored=60 resolved=60 start=1486
  completesAt=1546`**; **`ITEMS material=cobblestone count=3 gear=0 slots=[2:3]`** (exactly 2 taken, at the start).
  Dump at reload + 30: **`"Quiver: 0/2"`**; at reload + 65: **`"Quiver: 2/2"`**. No action-bar REPLY (above).
- **BR7 — AS PREDICTED, except the notice's text (UNREAD, above).** Both presses fired. `Gave 16 [Arrow] to
  BaronVonYeetus`; **`ITEMS material=arrow count=16 gear=0 slots=[2:16]`**, **`ITEMS material=cobblestone count=0 gear=0
  slots=[]`**; `DRIVE reload held=breacher`; **no `RELOAD` line**; **`ITEMS material=arrow count=16 gear=0 slots=[2:16]`**,
  **`ITEMS material=cobblestone count=0 gear=0 slots=[]`**: the arrows untouched. The dump: **`"Quiver: 0/2"`**.
- **BR8 — AS PREDICTED, except `Reloading...` (UNREAD, above).** `Given Boltor`; one press, no PLAYERHIT; **`ITEMS
  material=arrow count=16 gear=0 slots=[2:16]`**, **`ITEMS material=cobblestone count=5 gear=0 slots=[1:5]`**;
  `DRIVE reload held=boltor`; **`RELOAD … weapon=boltor authored=60 resolved=60 start=1609 completesAt=1669`**; **`ITEMS
  material=arrow count=15 gear=0 slots=[2:15]`**, **`ITEMS material=cobblestone count=5 gear=0 slots=[1:5]`**. Dump at
  reload + 65: **`"Quiver: 8/8"`**.
- **BR9 — the hit and the miss AS PREDICTED; the health probes NOT AS PREDICTED** (above). The dump: `"Kinetic Damage:
  16"`, **`"Cooldown: 0.8s | Mana Cost: 8"`**. Press 1: **one `PLAYERHIT … source=crystal_shard_staff/right_click
  target=be364f4d… zombie element=kinetic sent=16.000 crit=false`** (the zombie at 25). Press 2: **no PLAYERHIT** (the
  zombie at 33, `370f675b`).
- **BR10 — the CRAFT AS PREDICTED; the tooltip half UNREAD.** `clear` removed 1 (the rows' last Breacher);
  `INVENTORY breacher slots=[]`; four `Gave` REPLYs; the TELL at 16:16:33. **`CRAFT minted player=BaronVonYeetus
  weapon=breacher material=crossbow score=100`** at 16:16:38. **Then, recorded:** three `RELOAD … weapon=breacher` lines
  (16:16:44, :49, :58: he fired the crafted Breacher and reloaded it from BR8's leftover cobblestone), four `On cooldown`
  REPLYs, and his off-sheet `/rpg give crystal_shard_staff` (16:17:33). At the 60 s: **`INVENTORY breacher slots=[]`**
  (searched over every slot) and **`SKIPPED no hotbar slot holds breacher`**: **the crafted Breacher was no longer in his
  inventory; where it went is NOT in the log** (no drop line is logged). So the dump's `Gear Score` is UNREAD; the
  CRAFT line's `score=100` is read. **Where he placed the table was not recorded** (no row probes it); no reading in HF2 or
  BR11 is a miss, so none depends on it.
- **HF2 — AS PREDICTED.** Four `clear` REPLYs (one amethyst shard: his own staff from 16:17:33); `STAGE hold_only moved
  living_lantern from hotbar 1 to slot 9`, `… stone from hotbar 7 to slot 10`, `… nether_star from hotbar 8 to slot 11`,
  **`STAGE hold_only hotbar items=1 offhand=empty`**, `DRIVE hold … 1->0 item=living_lantern`. The first `rpg firerate`
  (the arm "he fired weapons of his own"): `2 weapons sampled, one reading each: living_lantern, breacher`, **`(28
  selftest-driven records kept out of every sample)`** (as predicted, 28), his own `living_lantern` `INPUTS count 9 …
  min 28t`, `FIRES count 9 … min 28t`, and `breacher` `INPUTS count 14 … min 3t`, `FIRES count 7 … min 20t`, `Sample
  cleared.`. **`HELDID slot=0 item=living_lantern offhand=empty`** at both ends. The second: **`Fire cadence --
  living_lantern, right_click`**, **`INPUTS count 44 window 172t mean 4.00t min 4t`**, **`FIRES count 9 window 164t mean
  20.50t min 16t`**, `cooldown_ticks 16 authored, 16 effective (…)`, `COOLDOWN-LIMITED …`, `Sample cleared.`.
  **By the prediction: `FIRES min 16` with `INPUTS min 4`: the lantern's held inputs arrive on the 4-tick grid and its
  16 delivers 16.** The mean 20.50 is the mana running out (nine `Not enough mana: 12 needed, 10-12 available` REPLYs).
  **Not `FIRES min 18 / INPUTS min 6`.**
- **BR11 — AS PREDICTED.** Six MOBSEED; `Gave 64 [Cobblestone]`; the TELL; his 30 seconds (six `RELOAD … weapon=breacher`
  lines, his left-clicks; three zombies removed by his fire before the row's kill, which killed 3). **`ASK 1
  question="Did the blast spread about as wide as you expected?" answer="Yes but i want to change it" ticks=260`.**
  **His answer, verbatim: "Yes but i want to change it".** How he wants to change it is NOT STATED: OPEN FOR BEN.
- **The END — AS PREDICTED.** `SELFTEST RUN FINISHED`; `SELFTEST RUN RESTORED xp=712672 cell=mage/fire gamemode=SURVIVAL
  … spawn_mobs=false time=1000 … health=126.000`; **`SELFTEST RUN HEALTH saved=126.000 customHP=126.000/350.000
  wrote=126.000`** (the snapshot's figure).

### AFTER HIS `/stop`

`rpgfight` holds 0 entries; the overworld holds **106** files, the same 106 as the backup's (the file lists compared: no
difference, so his table's chunk was already on disk); `ThreadedAnvilChunkStorage: All dimensions are saved` (16:18:39);
no `java` process left.

### UNREAD, BY ARM

- The empty-magazine refusal (BR6's third press fired; see above).
- Every action-bar notice's text (BR6, BR7, BR8): the instrument does not capture the action bar.
- BR9's two health figures (the probe's 16-block radius).
- BR10's tooltip half and `Gear Score` (the crafted Breacher left his inventory).
- BR2-BR5's crit arms (no crit rolled), and HF2's "did not hold" and `INPUTS min 6` arms.
- The off-hand crossbow guard, the client, knockback, durability, creative (*WHAT THIS GATE DOES NOT READ*).
