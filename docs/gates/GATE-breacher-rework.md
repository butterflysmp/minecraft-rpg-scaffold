# GATE — THE BREACHER REWORKED (feat/breacher-rework), THE BURN FIX, AND THE OWED RE-RUNS

**Status: NOT RUN.** Written 2026-11-22 on `feat/breacher-rework` (PR 2), stacked on `feat/breacher-rework-owed`
(PR 1), on master `13a31174` (the squash of #236), before any boot. **This file carries the ONE R0 and the ONE run sheet
for the two.**

**Status, 2026-11-22, 17:11-17:17: BOOTED ONCE at `426392e1`, R0 PASSED, AND NO ROW OF THIS GATE RAN.** Ben typed
`/rpg selftest breacher` (GATE-breacher's scenario, the previous gate) where the sheet says `/rpg selftest
breacher-rework`. **Every row below is UNREAD; this gate needs another boot.** Readings in *READINGS, 2026-11-22* at the
end. No prediction above was edited.

**NO BOOT until the seat has diffed the code AND this gate from origin, and Ben says "ready" in his own words**; then
only CC boots, by `.claude/rules/booting.md`. **NO MERGE without Ben's own words.**

**Game mode:** SURVIVAL, every row. **Every row is AUTO** (`/rpg selftest breacher-rework`); **Ben types three lines**,
and his answer to the one question when the selftest asks it. No row asks him to do anything but the last (a free go).
**Time:** the setup sets night (18000) and `spawn_mobs false`; the run's restore puts both back. **Every mob is a frozen
NoAI zombie; no fight world.** The setup kills the natural hostiles within 64 blocks by named type and stages his level
at 1; the run's end puts his health back (*HIS HEALTH*). **Every row that presses a weapon begins from a cleared
cooldown** (`.claude/rules/verification.md`, *A ROW BEGINS FROM A CLEARED COOLDOWN*): each first press sits at least 25
ticks after the row starts and after any earlier press of that weapon, against the Breacher's 20.

**Ben's words this gate serves, verbatim (2026-11-22, spelling as typed),** recorded in `PLAN-weapon-tiers.md` 1.11:
- "1. First the damage is way to low, you should be able to easily kill a 100hp mob with one shot. Second I want to
  change the pellets from 7 to 3 and up the damage to 40 per pellet. Third tighten the spread to let's say 8 degrees
  total. I want the pellets to be in a triangle shape with the point up"
- "drop the degree to 6 and add a fourth pellet to the center"

## WHAT THE TWO PRs DO

**PR 1, `feat/breacher-rework-owed`:** `DotStatus.apply` keeps the LARGER cap (a weaker burn never lowers a stronger
one; withering too; the credit stays the newest applier's); `ReplyReader` copies the `ACTION_BAR` packet as a Reply
with overlay true, printed `REPLY actionbar …`; `verification.md`'s cooldown rule and its CLAUDE.md index row; the
dated note closing `GATE-breacher.md`'s ruling 7 (Ben: "2. I dropped it").

**PR 2, `feat/breacher-rework`:** `rotation_degrees` on the spread block (`CastSpec.Spread.rotationDegrees`, default 0,
counter-clockwise from the shooter's right toward his up); `breacher.yml` at four pellets, 3 degrees off the aim,
rotated 90, 40 kinetic each; its content test, golden block, `PLAN-weapon-tiers.md` 1.11; this gate and its scenario.

## THE PLACEHOLDERS AND WHAT IS OPEN, FOR BEN (each marked `SEAT PLACEHOLDER 2026-11-22` in `breacher.yml`)

| item | value | status |
|---|---|---|
| the description | "Four chunks of cobblestone," / "a tight triangle 6 degrees wide." | SEAT PLACEHOLDER, reworded for the rework |
| the flavour | "Iron, gunpowder, string," / "and whatever was lying around." | SEAT PLACEHOLDER |
| the pellets' flight | a cobblestone ITEM body, speed 3.0, gravity 0, 4 ticks (12 blocks) | SEAT PLACEHOLDER (Ben's "Fine" to "about 12 blocks") |
| "6 degrees total" | each ring pellet 3 degrees off the aim | the seat's reading, undisputed |
| the three visuals and their sounds | `breacher_cast`, `breacher_trail`, `breacher_hit` | SEAT PLACEHOLDER |
| **knockback and its strength** | 0.1 a pellet, by Ben's standing ruling (a travelling ranged weapon authors knockback) | **OPEN FOR BEN** |
| **the tooltip's "40 x 4"** | `WeaponLoreLines.deliveredShots` prints the count; the full hit exists out to 6.60 blocks | **OPEN FOR BEN** |
| **Expanded Quiver's wording** | "+N Quiver Arrows" on a weapon that loads cobblestone | **OPEN FOR BEN** |
| **the reach of the full hit** | all four out to **6.60** blocks (160); three from 6.60 to **6.91** (120); the centre alone beyond 6.91 (40), to 12 | recorded (EXECUTED, below) |

## WHAT WAS READ, EACH MARKED

**The pattern (READ, `SpreadPattern.directionsFor`):** the aim pellet first, then ring pellet i at phi = 2 pi i / ring
+ the rotation, measured from the shooter's RIGHT toward his UP, each `angle_degrees` off the aim. Count 4 is a ring
of three: unrotated at 0, 120, 240 (pointing RIGHT, the seat's read, confirmed); rotated 90 at 90, 210, 330 (POINT UP:
one straight above the aim, two below and either side, each `sin 210 = -1/2` of the top one's height). "Up" is his own
up vector, so the triangle tilts with his pitch; every row here shoots level (pitch 0).

**The geometry (EXECUTED, an awk model of the pattern and of `castRay`'s exact boxes, `RAY_SIZE` 0; his eye 1.62 above
his feet; a zombie 0.6 x 1.95 x 0.6 at his feet' height, aimed dead centre).** A ring pellet sits `x tan 3 = 0.05241 x`
off the aim at `x` blocks of path; the top pellet that far ABOVE the aim, the lower two `0.02620 x` below and `0.04539 x`
to either side.
- **All four land** while the top pellet is under the head at the zombie's front face (`0.33 / tan 3 = 6.30`, so a
  zombie centred at **6.60**) and the lower pair inside its half-width (`0.3 / (cos 30 tan 3) = 6.61`, centred **6.91**).
- **Three land** (the top clears the head) from 6.60 to 6.91; **the centre alone** beyond 6.91, to the 12-block reach.
- The seat's "about 6 blocks, then the centre pellet only" is replaced by these figures (the seat's own ruling).

| zombie on the aim at | point UP (90) | point RIGHT (0) | point DOWN (270) | the margins, at the front face |
|---|---|---|---|---|
| 1 | **4** | 4 | 4 | all deep inside |
| 4 | **4** | 4 | 4 | top pellet 0.136 under the head; lower pair 0.132 inside the half-width |
| 9 | **1** | 2 | 2 | UP: top 0.126 over the head, lower pair 0.095 outside; RIGHT's 240-degree pellet 0.072 INSIDE (y 1.23); DOWN's bottom pellet at y 1.16, laterally centred |
| 10 | **1** | 2 | 2 | UP: top 0.178 over, lower pair 0.140 outside |

**One shot, and how many PLAYERHITs (READ).** Each pellet's target is a `Combatant` built by `castRay` from a SNAPSHOT
taken at that ray (`BukkitCombatant.of` -> `snapshot`), and `applyDamage` defers the health write to the entity's
scheduler (`ctx.scheduler().onEntity(… applyNow …)`). The four pellets are launched in one loop inside the cast
(`CastExecutor.launch`), and each first segment is traced inline. So **every pellet passes `EffectApplier`'s `alive()`
gate and writes its PLAYERHIT before any health moves: four PLAYERHITs, even though 100 runs out on the third.** The
fourth's write lands on a dead body (INFERRED: harmless; not read).

**A target that survives 160 (READ, `MobNameplateManager.seed` and `MobScaling.maxHealth`):** a hostile's custom maximum
is its VANILLA max health x 5 (`VANILLA_FACTOR`) x its gear score / 100. A zombie summoned with
`attributes:[{id:"minecraft:max_health",base:40.0}]` seeds at **40 x 5 x 100/100 = 200**, and survives four pellets at
40. That the summon's attribute is applied before the seed reads it is INFERRED; the row's own `MOBSEED … max=200`
line reads it. A zombie with base **2.0** seeds at **10** (LL9R2's W).

**The burn (READ, PR 1):** `DotStatus.apply`'s refresh arm is `a.cap = Math.max(a.cap, cap)`; Ignite's blast (6, fire,
radius 4, 20 ticks after the death, skipping players and the dead body, READ `Ignite.detonate`) accrues Scorch at cap
6 x 0.5 = 3, which no longer lowers a standing cap 7.

**The action bar (READ, PR 1):** `ReplyReader.onPacketSend` copies `SYSTEM_CHAT_MESSAGE` and now `ACTION_BAR` (as overlay
true); `SelfTestRun.drainReplies` prints an overlay Reply as `REPLY actionbar "<text>" [<colours>]`. That
`Player#sendActionBar` sends one of those two packets is INFERRED (Paper not read); both are read now.

**The notices' words (READ, `QuiverNotice`):** empty: `"Your quiver is empty -- left-click to reload."` gray; no ammo,
for the Breacher: `noAmmoText("cobblestone")` = `"You have no cobblestone -- Cobblestone loads this weapon."` gray. Each
behind a 40-tick throttle keyed per player and notice; neither has fired in this run before its row.

**His own pellets and cone cannot strike him (READ):** `ProjectileFlight.step` casts `castRay(…, caster.id())` and
`LiveTargets.except` drops his id; the Living Lantern's `ConeShell` leaves his id out. **An ignite burst skips players**
(`Ignite.detonate`: `if (c.state().player()) continue;`).

## HIS HEALTH

- The setup's first `SELF customHP=<c0>/<m0>`; after the staging **`SELF customHP=<c1>/<m1>` with `m0 - m1 = 250.000`
  and `c1 = min(c0, m1)`**.
- **At the end:** `SELFTEST RUN RESTORED … health=<s>`, then **10 ticks later** `SELFTEST RUN HEALTH saved=<s>
  customHP=<s>/<m0> wrote=<s>`, where **`<s>` is the SNAPSHOT's figure** (taken in `SelfTestRun.start` before `RUN
  START`, READ), not the setup's first SELF line; `wrote=none` if he began at or under `100 + G`. If he stops at once
  and the line is missing, it is recorded, not a defect (the seat's ruling).

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup:** `run/plugins/Rpg` and `run/world` copied to a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-breacher-rework` (**if that name exists, `-2`, `-3`, the first free one**); file
   and directory counts, `diff -rq lines: 0` for each, the `ls` of `world/dimensions`, the overworld's file count.
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND:** "booting now", and Ben told **NOT to log in until told R0 passed.**
6. `./scripts/dev-server.sh --refresh-content`. **Required deployed lines:** `weapons/breacher.yml` holds the line
   `        amount: 40`; `weapons/` holds 22 `.yml` files.
7. R0a to R0e on **this boot's** log, matched to **this boot's own** `Done` line.
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* `rpgfight` empty; the overworld's file count against step 2's; `All dimensions are saved`; no
   `java` process left.

## THE RUN SHEET — BEN TYPES EXACTLY THESE THREE LINES

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.**
   **Stand on open, flat ground with a clear view for about 15 blocks, and face any way you like** (the rows turn you
   east and put you back on the same spot each time; the farthest zombie stands 10 blocks out and a pellet flies 12).
   **For the run your level is set to 1, so your health drops to about 100; it is put back at the end.**
1. `/gamemode survival`
2. `/rpg selftest breacher-rework` — **ONCE.** About a minute, then 30 seconds with the Breacher. Frozen zombies appear;
   they cannot move or hit you. **The last row gives you the Breacher and cobblestone for 30 seconds (RIGHT-click
   fires, LEFT-click reloads), then asks a question: type your answer in chat.** Your items, time, level and health are
   put back at the end.
3. `/stop`

Nothing else. If a row stalls or aborts, do not intervene. **His chat answer to the one question is the only line he
types beyond these three**; "not sure" is NOT STATED.

## THE ARITHMETIC (SAFETY): THE MOST HITS THAT CAN LAND ON BEN TIMES THE BIGGEST HIT, AGAINST THE HEALTH HE HAS IN THE ROW

**His health in every row is `100 + G`** (the setup's second `SELF` line). His defence is his gear's alone (level 1
grants 0). Below, G = 0 and defence 0: the worst case.

| row | where he is | what can hurt him | most hits x biggest hit | puts him down at 100 + G? |
|---|---|---|---|---|
| setup, RW0, RW1 | the spot he chose | nothing the run makes | 0 | no |
| RW2-RW5, BR6R2, BR7R2 | the run's start, facing east; NoAI zombies 1 to 10 blocks out | a NoAI zombie does not move or attack (INFERRED vanilla). **His pellets cannot strike him** (excluded by id, READ). Kinetic places no fire and accrues no burn (READ). Arrows given are inert | 0 x 0 = 0 | no |
| LL9R2 | the run's start; two NoAI zombies 3 to 3.7 blocks out | **his cone cannot strike him** (READ); **W's ignite burst, radius 4 round W, 20 ticks after its death, skips players** (READ), so though he stands 3.0 from W it does not reach him; the Lantern places no fire block (READ, `GATE-living-lantern.md`) | 0 | no |
| RW9 | the run's start; six NoAI zombies, 30 seconds in which he may walk | the same; kinetic kills ignite nothing | 0 | no |
| every row | the overworld at night, after the setup's kills | **a natural hostile from beyond 64 blocks** (spawns off) | at gs 100, a zombie about 15 a hit, a close creeper about 215 (vanilla UNVERIFIED) | **at 100:** 7 zombie hits, or one close creeper. Possible, not likely; a death aborts the run and costs no items |

**What the run clears, and the restore puts back** (the run's snapshot and restore, READ on `GATE-living-lantern.md`'s
review): **every crossbow** (the rows' Breachers, and any of his own), **cobblestone**, **arrows** and **lanterns** (LL9R2's
Living Lantern, and any of his own), by `clear @s minecraft:<item>`; his inventory, time, level, health and the
gamerules are put back by the run's restore on finish, quit, stop and server stop.

**The run's length** (`wait:` steps summed per row; recount with `tr -d '\r' <
paper/src/main/resources/selftest/GATE-breacher-rework.yml | awk '/^[A-Za-z0-9_-]+:/{row=$1} /^  - wait:/{w[row]+=$3}
END{for (r in w) print r, w[r]}'`): setup 40, RW0 0, RW1 10, RW2 46, RW3 46, RW4 48, RW5 81, BR6R2 71, BR7R2 71, LL9R2
120, RW9 600: **1,133**, plus the ASK's 0 to 1,200. Plus about 5 ticks per row-start clean (11: 55) and up to 40 per
`kill` step (38, 27 in the setup). **Worst case 1,133 + 1,200 + 55 + 1,520 = 3,908 ticks** (3 min 15 s); with nothing
to kill and a prompt answer, about 1,200 (1 min).

## R0 — THE BUILD, FOR THE WHOLE STACK. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `feat/breacher-rework`'s head as `git ls-remote` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries both PRs

**Every value line is checked by `ValueLine`** (`GATE-living-lantern.md`'s R0bR2 function: the value, then end of line
or whitespace and `#`). Three of the `breacher.yml` lines below carry a comment. **The burn fix is one call
(`Math.max`) and has no string a scan can tell from the old code**: R0a binds the jar to the commit that carries it, and
LL9R2 is its server witness.

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `breacher.yml` lines `        count: 4` (comment), `        angle_degrees: 3` (comment), `        rotation_degrees: 90` (comment), `        amount: 40`; `GATE-breacher-rework.yml` `LL9R2:`; `index.yml` `  - breacher-rework`; the classes `rpg/paper/selftest/ReplyReader.class` carrying `WrapperPlayServerActionBar` (PR 1), `rpg/core/ability/CastSpec$Spread.class` carrying `spread rotation_degrees must be finite` and `rpg/paper/content/AbilitySchema.class` carrying `rotation_degrees` (PR 2). **The controls, each ABSENT:** `breacher.yml` `        count: 7` (the old count), `        amount: 4` (a prefix of 40), `        rotation_degrees: 9` (a prefix of 90); `dragons_breath.yml` `        rotation_degrees: 90` (a file that authors none) | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
$b = Bytes 'content/weapons/breacher.yml'
ValueLine $b '        count: 4' 'breacher.yml'
ValueLine $b '        angle_degrees: 3' 'breacher.yml'
ValueLine $b '        rotation_degrees: 90' 'breacher.yml'
ValueLine $b '        amount: 40' 'breacher.yml'
ValueLine (Bytes 'selftest/GATE-breacher-rework.yml') 'LL9R2:' 'GATE-breacher-rework.yml'
ValueLine (Bytes 'selftest/index.yml') '  - breacher-rework' 'index.yml'
$p = 'io/github/butterflysmp/rpg/'
Has ($p + 'paper/selftest/ReplyReader.class') 'WrapperPlayServerActionBar'
Has ($p + 'core/ability/CastSpec$Spread.class') 'spread rotation_degrees must be finite'
Has ($p + 'paper/content/AbilitySchema.class') 'rotation_degrees'
'--- controls'
ValueLine $b '        count: 7' 'breacher.yml CONTROL-OLD'
ValueLine $b '        amount: 4' 'breacher.yml CONTROL-PREFIX'
ValueLine $b '        rotation_degrees: 9' 'breacher.yml CONTROL-PREFIX'
ValueLine (Bytes 'content/weapons/dragons_breath.yml') '        rotation_degrees: 90' 'dragons_breath.yml CONTROL-NONE'
$zip.Dispose()
```

### R0c — every Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| Exactly one of each, up to the first `joined the game`: **`[Rpg] Loaded 11 abilities, 43 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 22 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 6 recipes, 1 ores, 4 vein sizes`** (as `GATE-breacher.md` R0c read at `7a5e9286`: no content file is added or removed); **`Mint-on-craft: 36 indexed, 36 claiming, of 52 gear definitions`**; the loot, fight, world-damage, smithing and convert-on-arrival lines word for word those of `GATE-breacher.md`'s boot. **No `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Recipe '`; no `[Rpg]` WARN beyond `volley_stone`'s known pair** | `Select-String run\logs\latest.log -Pattern 'Loaded\|Mint-on-craft\|Convert-on-arrival\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded\|Recipe '''` | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 303 scenarios across 39 gates [melee-cell, …, breacher, breacher-rework]`**. The control: `293 scenarios across 38 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (PR 2's tree; `SelfTestScenariosTest` asserts 303): 39 gates, `^[A-Za-z0-9_-]+:` **342**, `^setup:` **39**,
so **303**. Control: 331 + 11 (setup, RW0-RW5, BR6R2, BR7R2, LL9R2, RW9) = 342.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, no `deleting`, no `orphan` | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | |

## ADDED 2026-11-22, BEFORE ANY BOOT: THE STATS BAR IN THE LOG (the seat's follow-up; the gate is NOT RUN)

**The gap the seat found, and it is real.** PR 1's reader copies EVERY `ACTION_BAR` packet sent to him for the whole
run, and the HUD's stats bar is one. Nothing above predicted it. Nothing above is edited; this section extends the
predictions before the boot.

**What the stats bar is (READ, `StatsBarSystem.start` and `StatsBarText.of`):**
- A per-player `RepeatingTask` every **`BAR_PERIOD_TICKS = 10`** ticks, started at his join, sends
  `player.sendActionBar(StatsBarText.of(…))`: **health, then the quiver (only while the main hand holds a weapon with a
  magazine), then defence (only when it rounds to 1 or more), then mana**, joined by four spaces, each field `<icon>
  <current>/<max>` with the icons `❤`, `➹`, `⛨`, `✦`. So in the log, **`REPLY actionbar "❤ <h>/<H>    ➹ <r>/<c>    ✦
  <m>/<M>" [red, aqua, blue]`** (the `➹` field and its colour only with a Breacher held; a `⛨` field only with defence).
  It skips a beat while his health is not yet tracked, which in a run it always is.
- **It leaves by the `ACTION_BAR` packet (INFERRED, and the inference has a reading behind it):** `GATE-breacher.md`'s boot
  ran with the bar sending every 10 ticks and its log held **no** `REPLY actionbar` line at all, when the reader then
  copied only `SYSTEM_CHAT_MESSAGE` (overlay included). So it is not system chat; Paper's `sendActionBar` was not read.
- **A HOLD** (`ActionBarHolds`, READ): while a hold is live, each beat RE-SENDS the held message in the bar's place
  (`StatsBarSystem` line 126, `held.hold().message()`), and the first beat after its end draws the stats line again.
  **Only two things start a hold: `MiningService`'s DENIED notice and the fight fixtures** (READ, every `holds.hold`
  call in main). **No row here mines or starts a fight**, so no hold is live in this run.
- **The quiver's notices are NOT held** (READ, `QuiverNotice`): `empty`, `noAmmo` and `reloadStarted` call
  `player.sendActionBar` once, behind a 40-tick throttle per notice. The next bar beat (within 10 ticks) overwrites it
  on his screen; in the log it is **one line**.

**PREDICTED, for the whole run: the stats-bar lines are EXPECTED and NOT COUNTED.** About **one per 10 ticks** for every
tick of the run after his join, so roughly 110 to 390 lines over the 1,133 to 3,908 ticks above (a rate, not a count to
check). **The pattern that sets them aside:** a `REPLY actionbar` line whose text begins **`"❤ `** (the heart and a
space). Every notice this gate predicts begins otherwise (`"Your quiver`, `"You have no`, `"Reloading`). A reader
filters with `Select-String -NotMatch 'REPLY actionbar "❤ '`, or reads them as a record of his health, quiver and mana.
**They do not make the log or any row unreadable**: at two lines a second they sit between the row's own lines, and
no prediction above is a count of `REPLY` lines.

**Row by row, the REPLY predictions this touches:**

| row | effect |
|---|---|
| setup, RW0 | stats-bar lines only (no quiver field: nothing with a magazine is held) |
| RW1-RW5 | stats-bar lines with a `➹ <r>/2` field while the Breacher is held; their PLAYERHIT and PROBE predictions are untouched |
| **BR6R2** | **`REPLY actionbar "Your quiver is empty -- left-click to reload." [gray]` EXACTLY ONCE** (not held; one press on an empty magazine; the throttle fresh), among the stats lines; the stats line after it reads `➹ 0/2` |
| **BR7R2** | **`REPLY actionbar "You have no cobblestone -- Cobblestone loads this weapon." [gray]` EXACTLY ONCE** (not held; one `reload:`; the throttle fresh); the stats lines read `➹ 0/2` |
| LL9R2 | stats-bar lines with no quiver field (the Lantern has no magazine) and the mana dropping by 12 on the cast |
| RW9 | stats-bar lines throughout his 30 seconds; any `"Reloading..."`, `"Your quiver is empty…"` or `"You have no cobblestone…"` lines are his own presses, recorded and not predicted |
| the END | stats-bar lines until the run's reader is unregistered at its finish (READ, `SelfTestRun.end`, on every exit path) |

## THE ROWS (`/rpg selftest breacher-rework`) — SURVIVAL, ALL AUTO

**EVERY DAMAGE PREDICTION NAMES EVERY TERM** (`.claude/rules/verification.md`), per pellet:
`sent = (amount x score/100) x enchant + class bonus + level bonus`, then `x charge x crit`.
**For the Breacher:** `(40 x 100/100) x 1.0 + 0 + 0 = 40`, `x 1.0`, and `x 2.0` on a crit: **`sent=40.000 crit=false`
or `sent=80.000 crit=true`**. Terms: amount 40 (the file); score 100 (`rpg gearscore set 100`, staged); enchant 1.0 (no
active enchant on a given item; RW2's `rpg enchant show` reads it); class bonus 0 (`GATE-breacher.md` BR1's `rpg stats`
read `Damage 0.00`; his gear is not changed by the run); level bonus 0 (level 1, staged); charge 1.0. **A spread crits as
a UNIT** (one roll per cast, READ `CastExecutor.launch`): one press's PLAYERHITs are all 40.000 or all 80.000.
**For the Living Lantern (LL9R2):** `(14 x 100/100) x 1.0 + 0 + 0 = 14`, or 28 on a crit. A zombie takes a hit whole
(`GATE-breacher.md` read 6 -> 94). **Any other `sent` is NOT AS PREDICTED.**

**THE PLAYERHIT LINE:** `PLAYERHIT … source=breacher/right_click target=<uuid> zombie element=kinetic sent=40.000
crit=false triggerScore=100` (or `80.000 crit=true`).

**WHEN A HIT LANDS (READ path, the last step INFERRED):** the press is T; the cast runs on T+1; a pellet's segment k (the first is
0, traced inline in the cast; 3 blocks each) on T+1+k; the health write that tick or the next. Every probe sits clear of both.

### setup — night, no spawns, the natural hostiles killed, his level at 1 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| As `GATE-breacher.md`'s setup, line for line: its own `LEFTOVER`; the `time set` and `gamerule` REPLYs; **`SELF customHP=<c0>/<m0> …`**; 27 kill lines; `REPLY "Set 1 level(s) for BaronVonYeetus (now level 1, 0/1000 into it, 0 lifetime)." [green]`; then **`SELF customHP=<c1>/<m1>` with `m0 - m1 = 250.000` and `c1 = min(c0, m1)`** | *HIS HEALTH* | |

### RW0 — THE SPOT (its counts describe the spot; not pass or fail) (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `LEFTOVER living=0 radius=16` or only types not on the kill list; the teleport's REPLY; `SELF …`; `BLOCKS AIR count=<a> box=1 0 -4 15 1 3` (240 on open ground), `BLOCKS AIR count=<b> box=1 -1 -4 15 -1 3`, `BLOCKS WATER count=<w> box=1 -1 -4 15 -1 3`, **recorded** (as ruled for `GATE-knell-beam.md`'s KB0 and `GATE-breacher.md`'s BR0). Read beside RW2-RW9: a block between his eye and a zombie stops a pellet | `SelfTestDrivers.probeBlocks` | |

### RW1 — THE TOOLTIP (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The `clear` REPLY; `Given Breacher`; `DRIVE hold …`; `Set Gear Score: 100`; then **`HELD breacher CROSSBOW x1 name="Breacher" [white] italic lore=["Kinetic" [white]; "Gear Score: 100" [gray, aqua]; "Quiver: 2/2" [gray]; "" []; "Breacher  Right-Click" [gold, yellow]; "Four chunks of cobblestone," [gray]; "a tight triangle 6 degrees wide." [gray]; "Kinetic Damage: 40 x 4" [gray, red]; "Cooldown: 1.0s" [dark_gray]; "" []; "Iron, gunpowder, string," [gray] italic; "and whatever was lying around." [gray] italic; "" []; "Common Ranged Weapon" [white]]`** | `BreacherContentTest.aMintedBreachersTooltipReads`; `golden-lore.txt` (`"40 x 4"`) | |

### RW2 — ONE SHOT KILLS: a 100-health zombie one block ahead (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `rpg enchant show`: every slot's active choice `-1`. `MOBSEED … zombie gs=100 … max=100`. Probe before the press: **`customHP=100.000/100.000`**. **Exactly FOUR `PLAYERHIT … target=<the zombie> … element=kinetic sent=40.000`** (or four `80.000`): the health runs out on the third, and the fourth's line is written anyway (*One shot, and how many PLAYERHITs*). Probe at press + 6: **the zombie `dead=true`** (vanilla's death animation keeps the body about 20 ticks, INFERRED), **or absent** with a `MOBREMOVE` line before the probe | *The geometry* (all four at 1 block); the sum | |

### RW3 — FOUR HITS COUNTED: a zombie that survives 160 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **`MOBSEED … zombie gs=100 … max=200`** (*A target that survives 160*). Probe before: **`customHP=200.000/200.000`**. **Exactly four `PLAYERHIT … sent=40.000`** on it. Probe at press + 6: **`40.000/200.000`** (200 - 4 x 40). On a crit, four `80.000` and the zombie dead (320 > 200) | the sum | |

### RW4 — POINT UP: one pellet at 9 blocks, where right or down would land two (SURVIVAL)

The staging and its arithmetic: *The geometry*'s table, the 9-block row. A zombie on the aim at 9: **pointing UP, only
the aim pellet lands** (the top pellet 0.126 over its head, the lower pair 0.095 outside its half-width, at the front
face); **pointing RIGHT (the default, 0), the 240-degree pellet lands as well** (0.072 inside, at y 1.23); **pointing
DOWN (270), the bottom pellet lands as well** (laterally centred, at y 1.16). So a reading of ONE tells point-up from
both; TWO would be one of the others. The core witness of the shape itself is `SpreadRotationTest`.

| prediction | stands on | READING |
|---|---|---|
| `MOBSEED … max=200`. **Exactly ONE `PLAYERHIT … sent=40.000`** (or 80.000), from the aim pellet's third segment (6 to 9 blocks, meeting the face at 8.7): traced at press + 3, the write then or at press + 4. Probe at press + 8: **`160.000/200.000`** (120 on a crit). **Two PLAYERHITs (`120.000/200.000`) would mean the triangle is not pointing up** | *The geometry*; `SpreadRotationTest.aRingOfThreeAtNinetyPointsUp` | |

### RW5 — THE FULL HIT'S REACH: four at 4 blocks, one at 10 (SURVIVAL)

Inside 6.60: a zombie at **4** (the top pellet 0.136 under the head, the lower pair 0.132 inside, at the front face).
Beyond 6.91: alone, a zombie at **10** (the top pellet 0.178 over, the lower pair 0.140 outside). **Nothing between 6.60
and 6.91 is staged**: the margins there are too thin to read on a server.

| prediction | stands on | READING |
|---|---|---|
| Press 1: **four `PLAYERHIT … sent=40.000` on `n`**; probe at press 1 + 8: **`n` `40.000/200.000`** (dead on a crit). Press 2 (33 ticks later): **exactly ONE `PLAYERHIT … sent=40.000` on `f`** (the aim pellet's fourth segment, 9 to 12, meeting the face at 9.7: traced at press + 4, the write then or at + 5); probe at press 2 + 8: **`f` `160.000/200.000`** (120 on a crit) | *The geometry* | |

### BR6R2 — OWED: EMPTY, AND THE EMPTY NOTICE READ (SURVIVAL)

The cooldown cleared first: 25 ticks after the row starts (RW5's last press was 8 ticks before its row ended, so at
least 33 before this row's first, against the 20).

| prediction | stands on | READING |
|---|---|---|
| The `clear` REPLY; `Given Breacher`; presses 1 and 2 fire (no target: no PLAYERHIT, no refusal); press 3, 22 ticks after press 2: **`REPLY actionbar "Your quiver is empty -- left-click to reload." [gray]`**, and no fire. The dump: **`"Quiver: 0/2"`** | `QuiverNotice.empty` (READ); PR 1's action-bar reader | |

### BR7R2 — OWED: NO AMMO, THE NOTICE NAMING COBBLESTONE READ (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Three `clear` REPLYs; two presses fire (the cooldown cleared, 25 ticks); `Gave 16 [Arrow] to BaronVonYeetus`; **`ITEMS material=arrow count=16 gear=0 slots=[<i>:16]`**, **`ITEMS material=cobblestone count=0 gear=0 slots=[]`**; `DRIVE reload held=breacher`; **`REPLY actionbar "You have no cobblestone -- Cobblestone loads this weapon." [gray]`**; **no `RELOAD` line**; then **`ITEMS material=arrow count=16 …`** (untouched) and **`ITEMS material=cobblestone count=0 …`**; the dump: **`"Quiver: 0/2"`** | `QuiverNotice.noAmmoText` (READ); `GATE-breacher.md` BR7's read | |

### LL9R2 — OWED: THE BURN KEEPS ITS CAP WHEN AN IGNITE BURST LANDS ON IT (SURVIVAL)

**S** at `~3.5 ~ ~1.2` (100 health; 3.70 from his eye, 18.9 degrees off the aim) and **W** at `~3 ~ ~0` (vanilla max 2,
seeded at **10**; 3.00 from his eye, on the aim): both inside the Living Lantern's cone (range 6.0, 30 degrees either
side, READ `living_lantern.yml`), both reached on the front's 4th tick (**press + 5**, as `GATE-living-lantern.md`'s LL2
read a zombie at 3). W and S are **1.30 blocks apart**, inside Ignite's radius 4.

**Every term, on S, by tick from the press (P):**
- **P+5: the Lantern's hit, `14`** (28 on a crit; the sum above). S is scorched: **cap = 14 x 0.5 = 7.000** (the
  AUTHORED 14, `Scorch.CAP_FRACTION`; a crit does not raise it). W takes 14 of its 10 and dies, unscorched, to a fire hit:
  **Ignite's second clause detonates, 20 ticks later** (READ `BukkitCombatant.applyNow`).
- **P+25: S's first burn, `min(5% of 100, 7) = 5.000`** (a burn every 20 ticks from the hit, READ `DotStatus`).
- **About P+25: W's burst on S, `6`** (fire; S takes a hit whole). Its accrual is cap 6 x 0.5 = **3.000**, and with PR
  1's rule **the standing 7.000 stays**. Whether the burst lands just before or just after the P+25 burn is not
  predicted (two 20-tick clocks from ticks a tick apart); either way the burn after it reads 7.
- **P+45 and P+65: burns of `5.000`, each `DOTTICK <S> zombie status=scorch applier=<his uuid> sent=5.000 cap=7.000`.**

| prediction | stands on | READING |
|---|---|---|
| Probe before: S **`100.000/100.000`**. `DRIVE use … item=living_lantern`; **two PLAYERHITs** `source=living_lantern/right_click … element=fire sent=14.000` (both `28.000` on a crit), on W and on S; W `MOBREMOVE` (dead). Probe S at **P+40: `75.000/100.000`** (100 - 14 - 5 - 6; **61.000** on a crit). Probe S at **P+80: `65.000/100.000`** (75 - 5 - 5; 51 on a crit). **Every `DOTTICK` on S reads `sent=5.000 cap=7.000`**: three by P+80 (P+25, P+45, P+65). **Under the OLD rule the burns after the burst would read `sent=3.000 cap=3.000` and S `69.000` at P+80.** **IF THE BURST DOES NOT REACH S:** S reads `81.000` at P+40 and `71.000` at P+80 (no `-6`), and the `cap=7.000` lines then say nothing about the fix (no lower cap ever arrived): **the row is UNREAD for the fix**, and recorded as the staging's miss | PR 1's `BurnCapNeverLoweredTest`; `GATE-living-lantern.md` THE BURN, READ | |

### RW9 — A FREE GO, THEN THE ONE QUESTION (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Two `clear` REPLYs; `Given Breacher`; `Gave 64 [Cobblestone]`; six `MOBSEED` lines; the TELL; 30 seconds in which any PLAYERHITs are his, each `sent=40.000` or `80.000`, and any `RELOAD … weapon=breacher` lines his left-clicks (recorded, none predicted); then **`ASK 1 question="Is the Breacher's blast what you wanted now?" answer="<his words>"`**, verbatim, or `answer=NOT STATED after 1200 ticks` ("not sure" is NOT STATED) | `SelfTestAsk` | |

## R0b, RUN BEFORE COMMIT

Run against a jar built from this tree (`./mvnw -q package -DskipTests`, `paper/target/rpg-0.1.0-SNAPSHOT.jar`); its
printed lines are in the commit body that carries this gate.

## WHAT THIS GATE DOES NOT READ

- **The client:** the triangle as he sees it (his one answer is the only reading), every visual and sound.
- **The 6.60 to 6.91 band** (three pellets): not staged, too thin; the model and the core test carry it.
- **Point-up at a pitch other than 0:** every row shoots level; the tilt with his pitch is `SpreadPattern`'s basis, READ.
- **Knockback** (frozen zombies), **durability**, **creative**, **the off-hand crossbow guard** (as `GATE-breacher.md`).
- **The Breacher's crit arm, unless one rolls.**

## READINGS, 2026-11-22 (the boot of `426392e1`): R0 PASSED; THIS GATE'S ROWS DID NOT RUN

Boot approved by the seat's message on Ben's own "ready". Read from `run/logs/latest.log` of this boot and the jar in
place.

### R0 (read before he logged in) — AS PREDICTED

- **R0a:** `[Rpg] Build: 426392e1`.
- **R0b:** the scan as committed, on the deployed jar: all 9 PRESENT (three comment-carrying lines included); the
  controls `count: 7`, `amount: 4`, `rotation_degrees: 9` and `dragons_breath.yml` `rotation_degrees: 90` ABSENT.
- **R0c:** `Loaded 11 abilities, 43 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 22
  weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 6 recipes, 1 ores, 4 vein sizes`;
  `Mint-on-craft: 36 indexed, 36 claiming, of 52 gear definitions`; the loot, fight, world-damage, smithing and
  convert-on-arrival lines word for word those of `GATE-breacher.md`'s boot (`2026-11-22-4.log.gz`); `[Rpg]` WARN only
  `volley_stone`'s pair; no `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Recipe '`.
- **R0d:** `Selftest: dev server, 303 scenarios across 39 gates [… breacher, breacher-rework]`; `293 … 38` ABSENT.
- **R0e:** `FIGHTWORLD sweep path=…\rpgfight folders=0`; no `deleting`, no `orphan`.
- Carried state: `Done` at 17:11:18; the deployed `breacher.yml` line 114 `        amount: 40`; 22 weapon files.
  **His login 17:14:33, after R0 was read.**

### HIS COMMANDS, AGAINST THE SHEET — NOT AS THE SHEET

The sheet: `/gamemode survival`, `/rpg selftest breacher-rework`, `/stop`. He issued, in order: **`/weather clear`**
(17:14:39), **`/give BaronVonYeetus minecraft:cooked_beef 64`** (17:15:02), **`/rpg selftest breacher`** (17:15:22: the
PREVIOUS gate's scenario, `SELFTEST RUN START breacher rows=13`), and inside that run **`/rpg spawn knell`** (17:16:23: a
live Knell, whose beam then telegraphed and fired at him; no `HURT` line is logged) and **`/give BaronVonYeetus
minecraft:cobblestone 64`** (17:16:52); then `/stop` (17:17:47). **`/gamemode survival` was not typed**; the run's
restore line reads `gamemode=SURVIVAL`, so he was already in survival (INFERRED from that line). **`/rpg selftest
breacher-rework` was never issued, so EVERY ROW OF THIS GATE IS UNREAD.**

### INCIDENTAL, ON THIS BUILD, FROM GATE-breacher's SCENARIO — RECORDED, NOT THIS GATE'S READINGS

These lines come from rows written for the previous gate and run on the reworked build. They witness PR 1's reader on a
server; they read no row of this gate.

- **The action-bar reader works (PR 1):** **305** `REPLY actionbar` lines. **286** are the stats bar
  (`grep -c 'REPLY actionbar "❤ '`), over the run's 17:15:22 to 17:17:45 (about 2,860 ticks): **about one per 10
  ticks**, as the added section predicted. Their form: `REPLY actionbar "❤ 278/350    ⛨ 10    ✦ 100/100" [red, green,
  blue]` before the staging, `"❤ 100/100    ✦ 100/100" [red, blue]` after it.
- **The quiver's notices, each once per firing:** `BR6 REPLY actionbar "Reloading..." [gray]` (1), `BR7 REPLY actionbar
  "You have no cobblestone -- Cobblestone loads this weapon." [gray]` (**exactly 1**), `BR8 REPLY actionbar
  "Reloading..." [gray]` (1); in BR10's window, his own presses produced `"Your quiver is empty -- left-click to
  reload."`, `"Reloading..."` and `"Reloading -- 0.1s"`.
- The old scenario's question was answered: `ASK 1 question="Did the blast spread about as wide as you expected?"
  answer="yes"`. **It is GATE-breacher's question asked of the reworked Breacher, not this gate's ("Is the Breacher's
  blast what you wanted now?"), which is NOT STATED.**
- **The end:** `SELFTEST RUN RESTORED … gamemode=SURVIVAL …`; **`SELFTEST RUN HEALTH saved=278.000
  customHP=278.000/350.000 wrote=278.000`**.

### AFTER HIS `/stop`

`rpgfight` holds 0 entries; the overworld holds 106 files, the same list as the backup's; `All dimensions are saved`
(17:17:48); no `java` process left.

### UNREAD

**Every row of this gate:** RW0-RW5, BR6R2, BR7R2, LL9R2, RW9, and its setup's SELF lines. The next boot runs
`/rpg selftest breacher-rework` on the same jar.
