# GATE — THE BONE CLUB AND DRUMMING BEAT (2026-11-26), WITH THE BLAST FUNGUS GATE AND TT20 TO TT27 BY REFERENCE

**Status: NOT RUN.** The plan is `docs/plans/PLAN-bone-club.md`. The scenario is
`paper/src/main/resources/selftest/GATE-bone-club.yml`. The stack, bottom up: #316 `feat/class-damage-labels`, #317
`feat/fused-projectile`, #318 `feat/blast-fungus`, #319 `feat/fast-and-fungal`, #320 `feat/cluster-bombs`,
`feat/bone-club`, `feat/drumming-beat`, and the ONE gate branch at the top, `feat/blast-fungus-gate` (#321), MOVED UP onto
`feat/drumming-beat` (its two Blast Fungus commits unchanged, `git range-diff` `=`), carrying this gate as well.
**Every prediction below was written before any boot, and is frozen from the seat's approval.** No boot without Ben's own
"ready" and the seat's approval of the code AND this gate from origin.

## WHAT BEN TYPES — ONE COMMAND FOR THE WHOLE STACK

```
/rpg selftest bone-club+blast-fungus+true-tooltips.TT20.TT21.TT22.TT23.TT24.TT25.TT26.TT27
```

- `bone-club`, this whole gate: its setup, then BC1 to BC5, DB1 to DB8, BC6 (the file's order). **It asks nothing.**
- `blast-fungus`, that whole gate (`GATE-blast-fungus.md`, unchanged): its setup, then BF1 … BF7, FF1 … FF3, CB1 … CB4,
  BF8, BF9. **BF9 is its one ASK** (3600 ticks).
- `true-tooltips.TT20 … TT27` by reference (`GATE-true-tooltips.md`, "THE CLASS LABELS"). **TT27 is its one ASK, last.**
- The queue, step for step, is `BoneClubGateCommandTest` (parsed against the shipped scenarios).
- **This gate's R0 supersedes the R0 of both gates below it** (theirs are left as the record).

He types it on the pad's server, standing anywhere; the first setup takes him to the pad. **He answers TWO questions in
the whole command, BF9's and TT27's, as the run already had: this gate adds none.**

**How long it runs (an ESTIMATE from the scenarios' waits, not a measurement):** this gate's `wait:` steps sum to 1872
ticks (setup 40; BC1 6, BC2 24, BC3 8, BC4 104, BC5 106, DB1 4, DB2 217, DB3 92, DB4 92, DB5 253, DB6 147, DB7 142, DB8
627, BC6 10), about 94 s, with each row's CLEAN about a minute and three quarters. The Blast Fungus gate's own estimate
is about a minute and a half, then his BF9 answer; TT20 to TT27 about half a minute, then his TT27 answer. **About three
and three quarter minutes, plus two answers.**

## SAFETY — 0.000

- **No row of this gate deals him damage.** No `damage @s`, no `rpg damage`, no blast, no projectile. Every mob is a
  zombie with `NoAI:1b` (it never swings: `ScenarioLint` MOB_ACT_UNSTAGED is green) or BC6's **wild wolf with `NoAI:1b`**
  (a wild wolf is neutral and, frozen, never acts). He deals every hit; nothing is priced to him, so S-1 has no term to
  execute and S-3 has no dealer whose score could move.
- **The Blast Fungus gate (by reference): 0.000**, every blast executed (`GATE-blast-fungus.md`, SAFETY).
- **TT20 to TT27 (by reference): 0.000** (`GATE-true-tooltips.md`, their SAFETY).
- **THE SUM FOR THE WHOLE COMMAND: 0.000 + 0.000 + 0.000 = 0.000**, at or under 20.000.

## CARRIED STATE — THE BOOTER'S CHECKLIST, IN THIS ORDER (`.claude/rules/booting.md`, each step ticked in the report)

1. The seat's approval of the code AND this gate from origin; Ben's own "ready".
2. **The heads:** `git ls-remote --heads origin` for the eight branches, equal to what the seat diffed; CI green on each.
   `git status --porcelain` prints nothing and `git rev-parse HEAD` equals `feat/blast-fungus-gate`'s head.
3. **No JVM:** `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` counts 0.
4. **The backup**, to a NEW folder `C:\Users\Neb91\IdeaProjects\rpg-run-backups\<date>-pre-bone-club`: `run\world` and
   `run\plugins\Rpg` with their file counts and copy counts, and his player `.dat` with its bytes.
5. **The world's saved difficulty,** read from the decompressed `level.dat`, before and after the run.
6. **Config:** `dev-pack-on-join: true`. No `run\rpgfight`.
7. **"BOOTING NOW", in its own message, BEFORE the boot command**, with: "Ben: do NOT log in until I tell you R0 passed."
8. `./scripts/dev-server.sh --refresh-content`; the `Done` matched to a log newer than the deployed jar.
9. R0 read on this boot's log; **"R0 PASSED" in its own message, before he logs in.** His first `joined the game` time
   reported beside R0's.
10. After his `/stop`: the clean-stop lines, no `java` process left.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

- **R0a:** `[Rpg] Build: <sha>`, `feat/blast-fungus-gate`'s head as `git ls-remote --heads origin feat/blast-fungus-gate`
  prints it, shortened. Not `-dirty`, not `unknown`.
- **R0b:** BOTH scripts on the DEPLOYED jar.
  - `docs/gates/GATE-bone-club-r0b.ps1`: before `'--- absent'` **21 `PRESENT`, 0 `ABSENT`** (5 `Entry`, 5 `Has`, 11
    `ValueLine`); between `'--- absent'` and `'--- controls'` **5 `ABSENT`, 0 `PRESENT`** (no `craft_result:`, no
    `sweep:`, no `crit_chance:`, in no loot source, the old `onSummonerWeaponEntityInteract` gone); after `'--- controls'`
    **2 `PRESENT`, 0 `ABSENT`**.
  - `docs/gates/GATE-blast-fungus-r0b.ps1`, its own prediction unchanged: **18 / 0, 4 / 0, 2 / 0**.
  - **Run before commit on a jar built from this tree (`paper/target/rpg-0.1.0-SNAPSHOT.jar`, 2026-11-26): 21 / 0, 5 / 0,
    2 / 0 and 18 / 0, 4 / 0, 2 / 0, as predicted.**
- **R0c:** `[Rpg] Loaded …, 27 enchants, …, 27 weapons, 1 shields, 28 armor, …` (recounted: `ls content/enchants/*.yml |
  wc -l` = 27, 26 at `921c953c`; `ls content/weapons/*.yml | wc -l` = 27, 26 at `305e1361`). `Mint-on-craft: 42
  indexed, 42 claiming, of 61 gear definitions` (`BootCountLinesTest`; the club claims no craft). The loot line word for
  word #316's: no source gains an item. **0** `WARN]: [Rpg]` lines (control: `INFO]: [Rpg] Loaded` reads 1 or more);
  none of `Refusing`, `Skipping`, `SEVERE`, `Exception`.
- **R0d:** exactly one **`[Rpg] Selftest: dev server, 511 scenarios across 53 gates [melee-cell, …, true-tooltips,
  blast-fungus, bone-club]`**. Recount: 53 files, 564 keys (`cat GATE-*.yml | grep -cE '^[A-Za-z0-9_-]+:'`), 53 setups,
  so 511. Control: 549 + 15 (its setup and 14 rows) = 564. Controls: `497 scenarios` ABSENT, `52 gates` ABSENT.
- **R0e:** one `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`; and no `FUSED removed …
  cause=orphan`.

## THE ROWS

Each row's lines are those between `SELFTEST bone-club <row> START` and its `DONE`. **Clock: the server tick.** A
`wait: N` resumes exactly N ticks later, and steps with no wait between them run in one tick (READ, `SelfTestRun.advance`,
as `GATE-blast-fungus.md` records). **A hit's tick is its `attack` step's tick:** `Player#attack` raises the damage event
synchronously, and `landVanillaMelee` reads `Bukkit.getCurrentTick()` inside it. So two hits with `reset_charge` and
`wait: N` between them are exactly N ticks apart.

**Positions:** every row block-centres him on the stand (`execute align xz positioned ~0.5 ~ ~0.5 … -90 0`, facing east);
every mob is at `~2` east (block (2, 0)), BC5's `u` at `~2 ~ ~1` (block (2, 1)). **The setup preflights `-2 -2 4 2`**
(x −2..4 east, z −2..2 south, whole blocks from his: READ, `SelfTestPreflight.Box`), so every coordinate, (0, 0), (2, 0)
and (2, 1), lies inside it.

**THE CHARGE, READ (the pinned jar):** `Player.getCurrentItemAttackStrengthDelay` = `1 / attack_speed × 20` = **25.0**
ticks at the club's attribute (EXECUTED: the attribute is 0.7999999999999998, and `(float)(1 / it × 20)` is exactly 25.0);
`getAttackStrengthScale(p)` = `clamp((ticker + p) / 25, 0, 1)`, so a full charge (exactly 1.0) is reached on the **25th**
tick after a reset (24 reads 0.98; `BoneClubGateArithmeticTest`). **`Player.attack` NEVER resets the ticker** (READ: no
`resetAttackStrengthTicker` in it; `CraftLivingEntity.attack` calls only `Player.attack`); a real click's reset is
`ServerPlayer.swing`'s. So every hit here is followed by **`reset_charge`** (new, `HumanEntity#resetCooldown`, the swing's
server half), and the ticker also resets when the main-hand ITEM changes (`Player.tick`, READ). **Every full-charge hit
waits 40 (30 in DB8) after its reset or hand change, 15 or more above the 25 it needs;** DB6's uncharged hit waits 15.
`DRIVE attack … charge=` prints the charge each hit was stashed with (three decimals): **`charge=1.000`** on every full
hit, **`charge=0.620`** on DB6's third (EXECUTED: `(15 + 0.5) / 25`; the `+ 0.5` partial tick INFERRED from
`getAttackCooldown`). `DRIVE reset_charge HumanEntity#resetCooldown cooldown=1.0000->0.0200` after a full hit (`0.0200`
INFERRED the same way; DB6's third reads `0.6200->0.0200`).

**THE SUM, every term (a bare hit's; the rows stage each):**
`sent = ((44 x score/100) x enchantMultiplier + classDamageBonus + weaponLevelDamage) x chargeScale x critMultiplier`
= `((44 x 100/100) x 1.0 + 0 + 0) x 1.0 x (1 or 2)` = **44.000, or 88.000 on a crit**:
enchantMultiplier 1.0 (`rpg enchant clear`; Sharpness could roll and is not active; Drumming Beat is no damage percent);
classDamageBonus 0 (his inventory is cleared, no gear grants class damage); weaponLevelDamage 0 (level 1, EXECUTED
`LevelBonus.at(1).weaponDamage() = 0.0`); chargeScale 1.0 (`AttackCharge.scale(1.0)`, a full charge); critMultiplier 1,
**or 2 on a crit, the allowed second reading.** At score 200: **88.000, or 176.000.** All EXECUTED in
`BoneClubGateArithmeticTest`.

**A PLAYERHIT** here reads `PLAYERHIT caster=<him> source=bone_club/left_click target=<t> zombie element=kinetic sent=<s>
crit=<b> triggerScore=<score>`. **A STREAK line** (M8) is logged while trace is on after every landed Bone Club hit on a
mob, **immediately after that hit's PLAYERHIT** (READ: the trace is synchronous inside `landBasicMelee`):
`STREAK n=<n> step=<points> chance=<fraction> charged=<b> result=<counted|reset:uncharged|reset:gap|plain>`.

**THE BASELINE B.** No verb clears his accessories or fragments, so each Drumming Beat row reads `rpg stats` first:
**`  Crit Chance  B%`**. Every chance below is written at **B = 15** (the base, EXECUTED: `StatsSheetLines.critChance
(0.15)` = `15%`) and in general as `B/100 + step × n` (to three decimals, as the line prints it). **The first STREAK line
of each row reads the exact baseline to three decimals; every later one in a counted run is exactly one step above the
one before.**

**THE ROW-START CLEAN** now ends ` drum=<n>`: the streak the previous row left, forgotten. Predicted per row below.
`STAGE clear_state … fused=0 drum=0` in every row (the CLEAN already forgot it).

### BC1 — THE TOOLTIP, BARE, AND A FRESH ROLL'S SLOTS

1. `HELDID slot=<n> item=bone_club`.
2. `rpg enchant show` on the fresh roll: **`  Slot 0: [drumming_beat locked]`**, **`  Slot 1: [kinetic_tremors locked]`**,
   `-- 4 slot(s)`. Slots 2 and 3 not predicted.
3. After `rpg enchant clear` and score 100, `dump: held`: **`HELD bone_club BONE x1`**, the name `Bone Club` in the
   common colour, and the lore, WHOLE, in order (EXECUTED, `BoneClubTooltipGateTest.theBareClub`, through
   `WeaponItems.applyLore`'s own calls):
   `Kinetic | Gear Score: 100 | | Melee Damage: 44 | Attack Speed: 0.8 | | Common Melee Weapon`.
   **No crit line, no tier line** (none exists for any weapon), no flavor.

CLEAN `drum=0`.

### BC2 — THE ATTACK SPEED, READ ON THE SERVER

`ATTRIBUTE attack_speed value=1.600 base=4.000` (the iron sword, the control), then **`ATTRIBUTE attack_speed
value=0.800 base=4.000`** (the club). EXECUTED: `VANILLA_BASE + AttackSpeedAttribute.modifier(0.8, 1.0)` =
0.7999999999999998, printed `0.800` to three decimals; the sword's is exactly 1.6. The base 4.000 is INFERRED to be the
player's, and the line reads it. CLEAN `drum=0`.

### BC3 — THE POOLS OVER 500 ROLLS

| weapon | `rpg enchant show` | `ROLLSTATS …` | slot 0 | slot 1 |
|---|---|---|---|---|
| bone_club | `Slot 0: [drumming_beat locked]`, `Slot 1: [kinetic_tremors locked]` | `ROLLSTATS bone_club element=kinetic rolls=500 weaponPool=1 uniquePool=1 normalPool=2` | `1=500` | `1=500` |
| iron_sword (control) | `Slot 0: []`, `Slot 1: [kinetic_tremors locked]` | `ROLLSTATS iron_sword element=kinetic rolls=500 weaponPool=0 uniquePool=1 normalPool=2` | `0=500` | `1=500` |

The normal pool of two is `sharpness` and `unbreaking` for both (`DrummingBeatContentTest`, 500 rolls each). CLEAN `drum=0`.

### BC4 — ONE FULL-CHARGE HIT AT 100 AND ONE AT 200, THE BARE CLUB

Staging: a fresh NoAI zombie `t` at `~2`, `max_health` 200 (EXECUTED: `MobScaling.maxHealth(200, false, true, 100)` =
1000.0), at night, enchants cleared, `rpg stats` read (B). The two PROBEs before each hit are the control, predicted
EQUAL.

| score | PROBE `t` before (twice) | PLAYERHIT | STREAK | PROBE `t` after |
|---|---|---|---|---|
| 100 | `customHP=1000.000/1000.000` | `sent=44.000 crit=false triggerScore=100`, or `sent=88.000 crit=true` | `STREAK n=0 step=0 chance=0.150 charged=true result=plain` (chance = B/100) | below 1000.000 (by what its defence lets through; not predicted) |
| 200 | `customHP=1000.000/1000.000` | `sent=88.000 crit=false triggerScore=200`, or `sent=176.000 crit=true` | the same `plain` line | below 1000.000 |

- **88.000 appears in both rows, and the line tells them apart:** at 100 it is `crit=true triggerScore=100`, at 200
  `crit=false triggerScore=200`. A swap of score and crit would show in those two fields.
- **The bare club's hits carry STREAK … `result=plain`, `step=0`, and no bonus** (the chance is the baseline): M8 for
  the bare club. CLEAN `drum=0`.

### BC5 — NO SWEEP, WITH THE IRON SWORD AS THE CONTROL

Two NoAI zombies, `t` at `~2` and `u` at `~2 ~ ~1`, `max_health` 200, a full charge, one hit on `t`.
- **The club:** `u`'s three PROBEs read `customHP=1000.000/1000.000`, **all three EQUAL**. No sweep. `t`'s hit: one
  PLAYERHIT (`sent=44.000` or `88.000`) and one `STREAK … result=plain`.
- **The iron sword, the same staging:** `u`'s two PROBEs before are EQUAL; its PROBE after reads **below them**, by no
  more than half of what `t` took (`SweepShare.of(primary, 0.5)`, EXECUTED: 11.5 of 23.0). Its PLAYERHIT `sent=23.000`
  or `46.000`, and **no STREAK line** (a weapon no Drumming Beat names).
- **If the control does not fall, the row is VOID, not a pass.** CLEAN `drum=0`.

### DB1 — DRUMMING BEAT III: THE SHOW AND THE TOOLTIP, WHOLE

- `rpg enchant show`: **`  Slot 0: [*drumming_beat III]`**, **`  active: drumming_beat III (+5% Crit Chance per
  consecutive hit)`**.
- `dump: held`: **`HELD bone_club BONE x1`**, the lore WHOLE (EXECUTED, `BoneClubTooltipGateTest.withDrummingBeatThree`):
  `Drumming Beat III | | Kinetic | Gear Score: 100 | | Melee Damage: 44 | Attack Speed: 0.8 | | Common Melee Weapon`.
  **Every stat line is BC1's, unmoved** (T8: conditional; T13 pins its 6 cases among the 582 unmoved). CLEAN `drum=0`.

### DB2 — DRUMMING BEAT III, FIVE FULL-CHARGE HITS 40 TICKS APART

`rpg stats` reads B. Five hits on one zombie (`max_health` 200), 40 ticks apart (each within the 100-tick gap).

| hit | STREAK (B = 15) | in general | PLAYERHIT |
|---|---|---|---|
| 1 | `STREAK n=0 step=5 chance=0.150 charged=true result=counted` | B/100 | `sent=44.000 crit=false` or `sent=88.000 crit=true` |
| 2 | `STREAK n=1 step=5 chance=0.200 charged=true result=counted` | + 0.050 | the same two readings |
| 3 | `STREAK n=2 step=5 chance=0.250 charged=true result=counted` | + 0.100 | 〃 |
| 4 | `STREAK n=3 step=5 chance=0.300 charged=true result=counted` | + 0.150 | 〃 |
| 5 | `STREAK n=4 step=5 chance=0.350 charged=true result=counted` | + 0.200 | 〃 |

EXECUTED (`BoneClubGateArithmeticTest.theStreakLinesByLevel`). **A crit does not reset (how the row reads it, whichever
hits crit):** every hit's STREAK reads `n = k − 1` for the k-th hit, so for any `crit=true` PLAYERHIT at hit k < 5, the
NEXT STREAK reads `n=k`, not `n=0`. If no hit of the five crits (at B = 15 the chance of that is 0.85 × 0.80 × 0.75 ×
0.70 = 0.357 for the first four, a probability, not predicted), this row is silent on the crit and DB8 is the certain
witness. PROBE after: below 1000.000. The NEXT row's CLEAN reads **`drum=5`**.

### DB3 — DRUMMING BEAT I: THE STEP IS 3

`STREAK n=0 step=3 chance=0.150 …counted`, then **`STREAK n=1 step=3 chance=0.180 charged=true result=counted`** (B/100,
then + 0.030). PLAYERHITs `sent=44.000` or `88.000`. CLEAN (this row's start) `drum=5`; the next row's `drum=2`.

### DB4 — DRUMMING BEAT II: THE STEP IS 4

`STREAK n=0 step=4 chance=0.150 …counted`, then **`STREAK n=1 step=4 chance=0.190 charged=true result=counted`** (+ 0.040).
CLEAN `drum=2`; the next row's `drum=2`.

### DB5 — THE GAP, AT ITS BOUNDARY

Three hits at T, **T + 100** and **T + 201**.

| hit | gap from the last counted | STREAK (B = 15) |
|---|---|---|
| 1 | — | `STREAK n=0 step=5 chance=0.150 charged=true result=counted` |
| 2 | **exactly 100**: continues | **`STREAK n=1 step=5 chance=0.200 charged=true result=counted`** |
| 3 | **101**: resets, and this hit starts the new streak | **`STREAK n=0 step=5 chance=0.150 charged=true result=reset:gap`** |

EXECUTED (`theResetLines`; the boundary row of `DrummingBeatTest`). CLEAN `drum=2`; the next row's `drum=1`.

### DB6 — AN UNCHARGED HIT RESETS

Hits at T, T + 40, **T + 55** (15 after the second's reset: `charge=0.620`) and T + 95.

| hit | `DRIVE attack … charge=` | STREAK (B = 15) | PLAYERHIT |
|---|---|---|---|
| 1 | `1.000` | `STREAK n=0 step=5 chance=0.150 charged=true result=counted` | `44.000` / `88.000` |
| 2 | `1.000` | `STREAK n=1 step=5 chance=0.200 charged=true result=counted` | 〃 |
| 3 | **`0.620`** | **`STREAK n=2 step=5 chance=0.150 charged=false result=reset:uncharged`**: rolled with NO bonus (chance = B/100), not counted | **`sent=22.331 crit=false`**, or **`sent=44.662 crit=true`** (EXECUTED: 44 × `AttackCharge.scale(0.6200000047683716)`) |
| 4 | `1.000` | **`STREAK n=0 step=5 chance=0.150 charged=true result=counted`** | `44.000` / `88.000` |

The third hit lands: 15 ticks is past the rider's 10-tick window (`MeleeHits.WINDOW_TICKS`, READ) and the zombie's
re-hit limit. Its `n=2` is the streak it lost. CLEAN `drum=1`; the next row's `drum=1`.

### DB7 — SWITCHING AWAY AND BACK RESETS

Two hits (`n=0`, `n=1`, as DB6's first two), then `hold: iron_sword` (`DRIVE hold PlayerItemHeldEvent+setHeldItemSlot`),
10 ticks, `hold: bone_club`, `HELDID … item=bone_club`, 40 ticks, a third hit: **`STREAK n=0 step=5 chance=0.150
charged=true result=counted`**. Without the switch it would read `n=2` (DB2's third). The reset is
`onDrumSlotChange` (`PlayerItemHeldEvent`, MONITOR). CLEAN `drum=1`; the next row's `drum=1`.

### DB8 — THE TRUTH ROW: TWENTY CONSECUTIVE FULL-CHARGE HITS AT III

**Staging:** one NoAI zombie `t` at `~2`, **`max_health` 400 → `customHP=2000.000/2000.000`** (EXECUTED
`MobScaling.maxHealth(400, false, true, 100)` = 2000.0; the first PROBE reads it). **THE SUM IT MUST SURVIVE: twenty
hits, every one a crit at worst, 20 × 88.000 = 1760.000 sent before its defence, under 2000** (EXECUTED). Twenty hits,
30 ticks apart (5 above the 25 a full charge needs; every gap 30 ≤ 100).

| hit k | STREAK (B = 15) | PLAYERHIT |
|---|---|---|
| 1 … 17 | `STREAK n=<k−1> step=5 chance=<0.150 + 0.050 × (k−1)> charged=true result=counted`: 0.150, 0.200, 0.250, 0.300, 0.350, 0.400, 0.450, 0.500, 0.550, 0.600, 0.650, 0.700, 0.750, 0.800, 0.850, 0.900, **0.950** | each `sent=44.000 crit=false` or `sent=88.000 crit=true`, the two allowed readings |
| **18** | **`STREAK n=17 step=5 chance=1.000 charged=true result=counted`** | **`sent=88.000 crit=true triggerScore=100`**: certain |
| **19** | **`STREAK n=18 step=5 chance=1.050 charged=true result=counted`** | **`sent=88.000 crit=true triggerScore=100`** |
| **20** | **`STREAK n=19 step=5 chance=1.100 charged=true result=counted`** | **`sent=88.000 crit=true triggerScore=100`** |

- **EXECUTED, the 18th:** `0.15 + DrumBeats.bonus(5, 17)` is **exactly 1.0**, and `Crit.crits(1.0, nextDown(1.0))` is
  true, so no draw of `nextDouble()` can miss. The 17th's sum is 0.9500000000000001 (it can miss). **No ceiling:** the
  19th and 20th read above 1.000 and are rolled clamped at 1.
- **In general:** the first certain crit is the first k with `B/100 + 0.05 × (k − 1) ≥ 1`; at B = 15 it is 18.
- **A crit does not reset, CERTAINLY:** hit 18 crits, and hit 19's STREAK reads `n=18`; hit 19 crits, and hit 20's reads
  `n=19`.
- PROBE after: above 0 (the zombie lives: at most 1760 sent of its 2000). The NEXT row's CLEAN reads **`drum=20`**.

### BC6 — M2: THE SERVER'S VERDICT ON A RIGHT CLICK ON A WOLF

A wild wolf `w` (`NoAI:1b`, `PersistenceRequired`) at `~2`. Each item right-clicks it through the new **`interact_entity`**
driver: a synthetic **`PlayerInteractAtEntityEvent(HAND)`**, the event the real click raises (READ), into every listener.

| held | `INTERACT result` |
|---|---|
| the Bone Club | **`cancelled=true tamed=false held=bone_club amount=1`** |
| the Bone Whistle | **`cancelled=true tamed=false held=bone_whistle amount=1`** (the same ONE guard) |
| a plain bone (the control, `give @s minecraft:bone 1`) | **`cancelled=false tamed=false held=bone amount=1`**, then `ITEMS material=bone count=1` |

- **AUTO-PARTIAL, AND SAID SO (`verification.md`, AN AUTO READING IS A READING OF THE SERVER):** the row reads the
  server's VERDICT (`cancelled`) only. Nothing vanilla follows a synthetic event (no `interactOn`, so no
  `Wolf.mobInteract`), so `tamed=false` and `amount=1` are true of the control too and witness NOTHING about the guard;
  **only `cancelled` differs, and it is the reading.** That a real click with the club neither tames nor is eaten follows
  from the READ (`handleInteract` returns before `interactOn` when the event is cancelled), and is **NOT witnessed**: it
  would need his click, and the brief allows this gate no ASK. **Owed, NOT READ.**
- **How the wolf goes:** the row's own `kill @e[tag=w]`, and at the latest the next row-start CLEAN (`removed=`), which
  removes every `selftest`-tagged entity. CLEAN `drum=20`.

### THE BLAST FUNGUS GATE AND TT20 TO TT27, BY REFERENCE

As `GATE-blast-fungus.md` (every row, unchanged: no file its rows read moves in the Bone Club PRs) and
`GATE-true-tooltips.md`, "THE CLASS LABELS". Each of their row-start CLEAN lines now ends ` drum=0`; nothing else in
them changes. **The first Blast Fungus CLEAN reads `drum=0`** (BC6's own CLEAN forgot DB8's 20; BC6 lands no club hit).

## LINT AND THE SCAN RUN BEFORE COMMIT

`ScenarioLint` over all 53 gates: no new finding (`SelfTestScenariosTest`'s pins unchanged but for the counts and lists
re-pinned above; `AskWindowLintTest` unchanged: this gate has no ASK). The gate-document test sees both sections. The two
R0b runs are quoted under R0.

## CC'S CALLS IN THIS GATE

1. **Two new drivers,** `reset_charge` (the swing's server half) and `interact_entity` (the entity click's verdict).
   Without the first, every hit through `Player#attack` reads full charge and an uncharged row is hollow (READ). Without
   the second, M2 has no AUTO reading. The alternative is an ASK, which the brief forbids.
2. **One zombie per Drumming Beat row**, every hit on it, so the window and re-hit limit are the only per-target terms; a
   different target is not a reset (M5) and is pinned in core (`DrummingBeatTest` never keys on a target).
3. **DB5 stages the gap at its exact boundary** (100 and 101), not around it.
4. **DB2's crit clause is conditional** (no crit may happen in five hits); DB8 is the certain witness.

## PLACEHOLDERS OPEN FOR BEN

The plan's list (`PLAN-bone-club.md`, PLACEHOLDERS): reach and arc, flavor, the drop chance, the icon, the line's
wording, the feedback sounds and the sheet showing the standing chance.

## WHAT ONLY THE BOOT CAN SHOW

That the server's tick spacing, charge and window agree with the READs (every STREAK line); that a real NoAI zombie
takes twenty hits as staged; that the drum and the reset sound PLAY (a sound is the client's: AUTO cannot hear it, and
this gate asks nothing, so **the feedback is NOT READ**); that the tooltip RENDERS as dumped (TT27 asks the class labels
only); and that a real click on a wolf with the club neither tames it nor eats it (**NOT READ**: BC6 reads the verdict).
