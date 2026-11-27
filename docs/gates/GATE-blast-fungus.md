# GATE — THE BLAST FUNGUS, FAST AND FUNGAL AND CLUSTER BOMBS (2026-11-26), WITH TT20 TO TT27 BY REFERENCE

**Status: RUN 2026-11-26 on `1f447197`, inside `GATE-blink-tome.md`'s one command; readings there, under READINGS.** The plan is `docs/plans/PLAN-blast-fungus.md`. The scenario is
`paper/src/main/resources/selftest/GATE-blast-fungus.yml`. The stack, bottom up: #316 `feat/class-damage-labels`, then
#317 `feat/fused-projectile`, #318 `feat/blast-fungus`, #319 `feat/fast-and-fungal`, #320 `feat/cluster-bombs` and this
gate, `feat/blast-fungus-gate`. **Every prediction below was written before any boot, and is frozen from the seat's
approval.** No boot without Ben's own "ready" and the seat's approval of the code AND this gate from origin.

> **SUPERSEDED FOR THE BOOT, 2026-11-26 (the seat's Bone Club brief: "no boot of this stack alone").** This gate's branch
> was MOVED UP onto `feat/drumming-beat` and now also carries `GATE-bone-club.md`. **Ben types that gate's ONE command**,
> `/rpg selftest bone-club+blast-fungus+true-tooltips.TT20.TT21.TT22.TT23.TT24.TT25.TT26.TT27`, and **its R0 supersedes
> the R0 below** (kept as the record). Every row below runs unchanged; each row-start CLEAN line now ends ` drum=0`
> (Drumming Beat's streak, forgotten). The command below is the record of this gate alone.

## WHAT BEN TYPES — ONE COMMAND

```
/rpg selftest blast-fungus+true-tooltips.TT20.TT21.TT22.TT23.TT24.TT25.TT26.TT27
```

- `blast-fungus`, the whole gate: its setup, then BF1 to BF8, FF1 to FF3, CB1 to CB4, and BF9 (the gate's order is the
  file's: BF1 … BF7, FF1 … FF3, CB1 … CB4, BF8, BF9).
- `true-tooltips.TT20 … TT27`: the true-tooltips gate's setup, then only TT20 to TT27 (`GATE-true-tooltips.md`, the
  section "THE CLASS LABELS", by reference; its steps are `GATE-true-tooltips.yml`'s, never copied). **Their own R0 rows
  are superseded by this gate's R0** (left there as the record).

He types it on the pad's server, standing anywhere; the first setup takes him to the pad. **He answers TWO questions in
the whole command, one per gate: BF9's (last of this gate) and TT27's (last of all).** Each waits 3600 ticks and says
`You have 180 seconds.` **The brief asked for at most one ASK; this gate has one, and TT27 is the referenced gate's own.**
Flagged for the seat: dropping either loses a witness only Ben can give (F11's vanilla block use; the class labels as
the client renders them).

**How long it runs (an ESTIMATE from the scenario's waits, not a measurement):** this gate's `wait:` steps sum to about
1560 ticks (78 s); with each row's CLEAN and its leftover wait, about a minute and a half, then his BF9 answer. TT20 to
TT27 with their setup add about half a minute, then his TT27 answer. **About two and a half minutes, plus two answers.**

## SAFETY — 0.000; EVERY TERM EXECUTED

- **No row deals him damage.** No `damage @s`, no `rpg damage`. Every mob is a zombie with `NoAI:1b` (it never swings;
  `ScenarioLint` MOB_ACT_UNSTAGED is green), and BF6's wolf is his own summon, frozen with `NoAI:1b` by the row.
- **Every blast near him is EXECUTED as dealing him nothing (S-1, S-2):** `BlastFungusGateArithmeticTest
  .straightDownTheyRestAtHisFeetAndHeTakesNothing` runs the real `CastExecutor` → `ProjectileFlight.launchFused` →
  `EffectApplier.detonateFused` with BF5's staging (straight down, three blasts within 0.721 of his feet): **his health
  100.000 → 100.000; another player standing 0.5 away 100.000 → 100.000; a summon untouched; the zombie beside them
  1000 → 835.** Two exclusions, both executed: by his id (`applyToEach`) and by the table (`PvpPath.FUSED_BURST`,
  SPARED; `FusedProjectileTest.onlyMobsAreHurt`; pinned in `MobHostilityTableTest`).
- **A chunk's body touching him does nothing (READ):** the flight's trace ignores the caster
  (`castRay(…, caster.id(), …)`), so his own chunk never meets him; another player's body is no touch
  (`FusedProjectileTest.anotherPlayerInThePathIsNoTouch`). The body is an item marker no player can pick up
  (`configureMarker`: `setPickupDelay(MAX)`).
- **S-3:** no dealer acts on him at any score; the score scales only what mobs take.
- **TT20 to TT27 (by reference): 0.000** (`GATE-true-tooltips.md`, their SAFETY: TT26's one mob is NoAI).
- **THE SUM: 0.000 + 0.000 = 0.000**, at or under 20.000.

## CARRIED STATE — THE BOOTER'S CHECKLIST, IN THIS ORDER (`.claude/rules/booting.md`, each step ticked in the report)

1. The seat's approval of the code AND this gate from origin; Ben's own "ready".
2. **The heads:** `git ls-remote --heads origin` for the six branches, equal to what the seat diffed; CI green on each.
   `git status --porcelain` prints nothing and `git rev-parse HEAD` equals `feat/blast-fungus-gate`'s head.
3. **No JVM:** `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` counts 0.
4. **The backup**, to a NEW folder `C:\Users\Neb91\IdeaProjects\rpg-run-backups\<date>-pre-blast-fungus`: `run\world`
   and `run\plugins\Rpg` with their file counts and copy counts, and his player `.dat` with its bytes.
5. **The world's saved difficulty,** read from the decompressed `level.dat`, before and after the run.
6. **Config:** `dev-pack-on-join: true`. No `run\rpgfight`.
7. **"BOOTING NOW", in its own message, BEFORE the boot command**, with: "Ben: do NOT log in until I tell you R0 passed."
8. `./scripts/dev-server.sh --refresh-content`; the `Done` matched to a log newer than the deployed jar.
9. R0 read on this boot's log; **"R0 PASSED" in its own message, before he logs in.** His first `joined the game` time
   reported beside R0's.
10. After his `/stop`: the clean-stop lines, no `java` process left.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

- **R0a:** `[Rpg] Build: <sha>`, `feat/blast-fungus-gate`'s head as `git ls-remote --heads origin
  feat/blast-fungus-gate` prints it, shortened. Not `-dirty`, not `unknown`.
- **R0b:** `docs/gates/GATE-blast-fungus-r0b.ps1` on the DEPLOYED jar. **Prediction, line for line:** before
  `'--- absent'` **18 `PRESENT`, 0 `ABSENT`** (6 `Entry`, 3 `Has`, 9 `ValueLine`); between `'--- absent'` and
  `'--- controls'` **4 `ABSENT`, 0 `PRESENT`** (no `craft_result:`, in no loot source, Cluster Bombs not guaranteed, not
  the old 35); after `'--- controls'` **2 `PRESENT`, 0 `ABSENT`**. **Run before commit on a jar built from this tree
  (`paper/target/rpg-0.1.0-SNAPSHOT.jar`, 2026-11-26): 18 / 0, 4 / 0, 2 / 0, as predicted.**
- **R0c:** `[Rpg] Loaded …, 26 enchants, …, 26 weapons, 1 shields, 28 armor, …` (recounted: `ls
  content/enchants/*.yml | wc -l` = 26, 24 at `fcfbefbb`, + `fast_and_fungal`, + `cluster_bombs`; `ls
  content/weapons/*.yml | wc -l` = 26, + `blast_fungus`). `Mint-on-craft: 42 indexed, 42 claiming, of 60 gear
  definitions` (it claims no craft: `BootCountLinesTest`). The loot line word for word #316's: no source gains an item.
  **0** `WARN]: [Rpg]` lines (control: `INFO]: [Rpg] Loaded` reads 1 or more); none of `Refusing`, `Skipping`,
  `SEVERE`, `Exception`.
- **R0d:** exactly one **`[Rpg] Selftest: dev server, 497 scenarios across 52 gates [melee-cell, …, true-tooltips,
  blast-fungus]`**. Recount: 52 files, 549 keys (`cat GATE-*.yml | grep -cE '^[A-Za-z0-9_-]+:'`), 52 setups, so 497.
  Control: 532 + 17 (its setup and 16 rows) = 549. Controls: `481 scenarios` ABSENT, `51 gates` ABSENT.
- **R0e:** one `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`; and **no `FUSED removed …
  cause=orphan`** (the enable sweep found no fused body).

## THE ROWS

Each row's lines are those between `SELFTEST blast-fungus <row> START` and its `DONE`. **Clock: the server tick.**
- **The THROW tick is the `FUSED throw` line's `tick=`.** The PRESS is one tick earlier (READ: `WeaponFire.attempt`
  hops the cast through `onRegion`, the global scheduler's `runDelayed(1)`); a `wait: N` after a step resumes exactly N
  ticks later, and steps with no wait between them run in one tick (READ, `SelfTestRun.advance`). So "the seat's 40 after
  the press" is predicted as **detonate = throw + 40 = press + 41**.
- **Positions:** `F` is his feet as the row aligned them (`execute align xz positioned ~0.5 ~ ~0.5 …`, block-centred on
  the pad, the floor at F's y); +x east, +z south (his right at yaw -90). A `FUSED … at=` is absolute; it is predicted as
  `F + (dx, dy, dz)`, **to 0.002** (the lines print three decimals; the figures are EXECUTED in
  `BlastFungusGateArithmeticTest` on a flat floor; the pad is flat, READ by its preflight).
- **A blast is a BOX (READ, `combatantsNear` = `getNearbyEntities(c, r, r, r)`):** a zombie is inside when its box
  (half-width 0.3, height 1.95) meets the cube `c ± r`. Every membership below is executed with that test.
- **THE SUM, every term (the rows stage each):**
  `sent = ((amount x score/100) x enchantMultiplier + classDamageBonus + weaponLevelDamage) x chargeScale x critMultiplier`
  = `((55 x 100/100) x 1.0 + 0 + 0) x 1.0 x 1 = 55.000`: enchantMultiplier 1.0 (`rpg enchant clear`, or only Fast and
  Fungal / Cluster Bombs active, neither a damage percent; Attunement could roll and is not active); classDamageBonus 0
  (his inventory is cleared, no gear grants class damage); weaponLevelDamage 0 (level 1, EXECUTED
  `LevelBonus.at(1).weaponDamage() = 0.0`); chargeScale 1.0 (an ability); critMultiplier 1, **or 2 on a crit, the allowed
  second reading: the crit is drawn ONCE per press (`BukkitCombatant.snapshot`), so every hit of one press crits or none
  does.** A zombie's custom health falls by `sent` exactly (no defence: `GATE-shatter-caps.md` SC1 read 1000 → 965 on a
  35).
- **A PLAYERHIT** here reads `source=blast_fungus/right_click … element=nature sent=<s> crit=<b> triggerScore=<score>`.
- **Every row begins from a cleared cooldown:** each row's `clear_state` clears every cooldown before its first press.

### EVERY COORDINATE LIES INSIDE THE PREFLIGHTED BOX (the seat's fix, 2026-11-26)

**The setup preflights `-2 -8 32 10`** (it was `0 -8 32 10`, which BF6's wolf at block x −1 fell outside). READ,
`SelfTestPreflight`: the box is in whole blocks from his own block, along the world's axes (x east, z south), each bound
−64..64; at every column his feet and head height must be AIR and the floor SOLID. READ, `SelfTestPadShape` (as
`GATE-summoner.md`): the pad's interior is x −2..45, z −13..13, floored, so the box is measurable. **Every row starts on
the stand** (the row-start CLEAN teleports him to `stage()`), and each row's `execute align xz positioned ~0.5 ~ ~0.5`
centres him in that same block, so F is the stand block's centre. **A position F+(dx, dz) lies in block
(⌊0.5 + dx⌋, ⌊0.5 + dz⌋)** from his. Heights are not in the box (it is columns); every position here is on or above the
pad's floor. Rest points are the EXECUTED ones (`BlastFungusGateArithmeticTest`).

| row | him | mobs (F+) → block | blocks | rest and detonation points (F+) → block | wolf |
|---|---|---|---|---|---|
| BF1, BF2, BF8 | (0,0) | — | — | — | — |
| BF3 | (0,0) | ba (7.5, 0) → (8, 0); bo (6, 5.5) → (6, 6); bn (6, −6.5) → (6, −6) | — | (6.650, 0) → (7, 0); (6.027, ±2.810) → (6, 3), (6, −3) | — |
| BF4 | (0,0) | bp (4, 0) → (4, 0) | — | touch (3.700, 0) → (4, 0); sides as BF3 | — |
| BF5 | (0,0), looking down | — | — | (0, 0) → (0, 0); (0, ±0.721) → (0, 1), (0, −1) | — |
| BF6 | (0,0), looking down | bs (1.5, 0) → (2, 0) | — | as BF5 | (−1, 0) → **(−1, 0)** |
| BF7, FF1, FF2, FF3 | (0,0) | — | — | as BF3 (no mob) | — |
| CB1, CB2, CB3 | (0,0) | c4 (6.65, 4.5) → (7, 5); c5 (6.65, −5.5) → (7, −5); cs (10.15, 0) → (10, 0) | — | big (6.650, 0) → (7, 0); splits (9.150, 0) → (9, 0), (5.400, ±2.165) → (5, 2), (5, −2) | — |
| CB4 | (0,0) | cs (10.15, 0) → (10, 0) | — | as CB1 | — |
| BF9 | (0,0) | — | flower pot (2, 0) → (2, 0) | his own clicks: the chunks rest at the pot | — |

**Every block x lies in −1..10 and every z in −6..6, inside x −2..32, z −8..10.** The only coordinate the old box missed
was BF6's wolf (block x −1); no other was outside. The chunks' paths run between his eye and these points, inside the
same columns.

### BF1 — THE TOOLTIP, BARE, AND A FRESH SLOT 0

1. `HELDID slot=<n> item=blast_fungus`.
2. `rpg enchant show` on the fresh roll: **`  Slot 0: [fast_and_fungal locked, cluster_bombs locked]`** (Fast and Fungal
   first: G1; Cluster Bombs always there: READ, `CandidateCascade.count` caps every tier at the one candidate left), and
   `-- 4 slot(s)`. **Slot 1 is empty** (no unique enchant admits a nature mage weapon). Slots 2 and 3 not predicted.
3. After `rpg enchant clear` and score 100, `dump: held`: **`HELD blast_fungus RED_MUSHROOM x1`**, the name `Blast
   Fungus` in the uncommon colour, and the lore, whole, in order:
   `Nature | Gear Score: 100 | | Fungal Barrage  Right-Click | Lobs three spore chunks that burst | after 2 seconds, or on
   touching a mob. | Magic Damage: 55 x 3 | Cooldown: 7.0s | Mana Cost: 30 | | It grew in the crimson dark, | and it
   does not like being picked. | | Uncommon Magic Weapon`. **The class label `Magic Damage`; no element-labelled damage
   line; no tier line** (none exists for any weapon).

### BF2 — THE POOLS, 500 ROLLS

`ROLLSTATS blast_fungus element=nature rolls=500 weaponPool=2 uniquePool=0 normalPool=2`, then **`ROLLSTATS slot 0 …
0=0 1=0 2=500 3=0 4=0`** (two candidates every time: the guaranteed one and the one left) and **`ROLLSTATS slot 1 … 0=500
…`**. Slots 2 and 3: `1=` and `2=` summing to 500 (random; the true odds line says 83.8% / 16.2% before the cap; not
predicted per run). The ORDER in slot 0 is BF1's `show` (one roll); every roll's order is the same code
(`ClusterBombsContentTest`, 500 of 500).

### BF3 — THE THROW: THREE CHUNKS, THREE FUSES, ONE MANA COST

Staging: zombies `ba` at F+(7.5, 0, 0), `bo` at F+(6, 0, 5.5), `bn` at F+(6, 0, -6.5); first PROBEs
`customHP=1000.000/1000.000` each.
- **Mana:** `MANA current=100.000 max=100.000` then, in the press's tick, **`MANA current=70.000 max=100.000`**: 30 once.
- `DRIVE use … item=blast_fungus …` and **`USE result useItemInHand=DENY useInteractedBlock=DENY`** (F11's main hand:
  `onRightClick` cancels the interaction).
- **Three `FUSED throw … at=F+(0.000, 1.620, 0.000)`** at the throw tick T.
- **Three `FUSED rest`** at **T + 9**, at **F+(6.650, 0.000, 0.000)**, **F+(6.027, 0.000, 2.810)** and **F+(6.027, 0.000,
  -2.810)** (EXECUTED: the floor crossed halfway through the 10th step, 0.7 x 9.5 = 6.65 along each direction).
- **The bodies REST:** each of the three `probe_positions: item 12` samples, at press + 15, + 25, + 35, lists **three
  `ITEM` positions, the SAME to 0.001 across the three samples**, each within 0.1 of a rest point (INFERRED that the
  platform carries the body exactly there; the detonation is at the flight's point, never the body's).
- **Three `FUSED detonate … cause=fuse`** at **T + 40** (press + 41), at the three rest points.
- **`ba`, inside all three:** three PLAYERHITs, `sent=55.000` each (or 110.000 each on the crit), PROBE **835.000** (or
  670.000). **`bo`, inside one** (the +z one): one PLAYERHIT, **945.000** (or 890.000). **`bn`, inside none** (3.690
  along z past the nearest centre): **no PLAYERHIT, 1000.000.**
- The fourth `probe_positions` (press + 45): **no `ITEM` line** (each body went with its chunk).

### BF4 — THE EARLY DETONATION

A zombie `bp` at F+(4, 0, 0), in the centre chunk's path. **One `FUSED detonate … cause=touch` at T + 5** (the 6th step,
which meets the box's front face x 3.7), **at F+(3.700, 1.163, 0.000)**, before T + 40; then the two side chunks rest
(T + 9) and go off at **T + 40, `cause=fuse`**. PROBE at press + 10: **945.000** (or 890.000); at press + 45: **835.000**
(or 670.000): bp stands inside both side blasts (EXECUTED).

### BF5 — HE STANDS INSIDE HIS OWN BLASTS

Straight down (pitch 90). **Three `FUSED rest` at T + 2, at F+(0.000, 0.000, 0.000), F+(0.000, 0.000, 0.721) and
F+(0.000, 0.000, -0.721)**; three `detonate … cause=fuse` at T + 40. **No `HURT` line in the row; no PLAYERHIT.** His two
`SELF` lines read the same custom health (he is healed to full first, so regeneration cannot move it).

### BF6 — A SUMMON INSIDE THE BLASTS

His Bone Whistle's wolf (summoned, frozen `NoAI:1b`, at F+(-1, 0, 0)) and a zombie `bs` at F+(1.5, 0, 0); straight
down; both inside all three blasts (EXECUTED). **The wolf's `SUMMONPROBE` health is the same before and after; no
PLAYERHIT names it.** `bs`: three PLAYERHITs, **835.000** (or 670.000). The wolf is removed at the next row's CLEAN
(`summons=1`).

### BF7 — THE COOLDOWN

Three presses: at P, at P + 136 and at P + 140.
- P: three `FUSED throw`.
- P + 136: **`REPLY "On cooldown for 0.2s"`** (4 ticks left, READ: `CooldownTracker.isReady` is `now >= press +
  cooldown`), **no `FUSED throw`, no mana spent** (the cooldown is checked before the mana, READ `AbilityService.resolve`).
- P + 140: three `FUSED throw` (**accepted exactly at 140**). Six `FUSED throw` lines in the row.

### FF1 — FAST AND FUNGAL I

The first line of the table below.

### FF2 — FAST AND FUNGAL II

Its second line.

### FF3 — FAST AND FUNGAL III

Its third line. The three rows, together:

| row | `show` | `dump: held`'s Cooldown line | refused at | its reply | accepted at |
|---|---|---|---|---|---|
| FF1 | `  Slot 0: [*fast_and_fungal I]`, `  active: fast_and_fungal I (-1s cooldown)` | `Cooldown: 6.0s \| Mana Cost: 30` | P + 116 | `On cooldown for 0.2s` | P + 120 |
| FF2 | `… II (-2s cooldown)` | `Cooldown: 5.0s \| Mana Cost: 30` | P + 96 | `On cooldown for 0.2s` | P + 100 |
| FF3 | `… III (-3s cooldown)` | `Cooldown: 4.0s \| Mana Cost: 30` | P + 76 | `On cooldown for 0.2s` | P + 80 |

Each dump's lore, whole: `Fast and Fungal <n> | | Nature | Gear Score: 100 | | Fungal Barrage  Right-Click | Lobs three
spore chunks that burst | after 2 seconds, or on touching a mob. | Magic Damage: 55 x 3 | Cooldown: <c> | Mana Cost: 30
| | It grew in the crimson dark, | and it does not like being picked. | | Uncommon Magic Weapon`. **Fast and Fungal III,
quoted whole:** `Fast and Fungal III | | Nature | Gear Score: 100 | | Fungal Barrage  Right-Click | Lobs three spore chunks
that burst | after 2 seconds, or on touching a mob. | Magic Damage: 55 x 3 | Cooldown: 4.0s | Mana Cost: 30 | | It grew
in the crimson dark, | and it does not like being picked. | | Uncommon Magic Weapon`. Six `FUSED throw` lines a row.

### CB1 — CLUSTER BOMBS I

The table's first line below, B = 55.

### CB2 — CLUSTER BOMBS II

Its second line, B = 80; its block is quoted whole below.

### CB3 — CLUSTER BOMBS III

Its third line, B = 105. The three rows, together:

Zombies `c4` at F+(6.65, 0, 4.5) (4.5 from the big centre), `c5` at F+(6.65, 0, -5.5) (5.5), `cs` at F+(10.15, 0, 0)
(1.0 past the heading's split point). `show`: **`  active: cluster_bombs <n> (throws one big chunk of <B> damage in 5
blocks that splits into 3 of the usual)`**.
- **ONE `FUSED throw`** at T; **one `FUSED rest` at F+(6.650, 0.000, 0.000), T + 9**; **one `FUSED detonate …
  cause=fuse`, T + 40**.
- **`SPLIT … at=F+(6.650, 0.000, 0.000) bursts=3 tick=T+40`**; three `FUSED throw` from that point at T + 40; **three
  `FUSED rest` at T + 53, at F+(9.150, 0.000, 0.000), F+(5.400, 0.000, -2.165) and F+(5.400, 0.000, 2.165)** (the
  triangle, one point on his heading, 2.5 from the centre); **three `detonate … cause=fuse` at T + 60** (20 after the
  split).
- PROBEs at press + 45 (after the big, before the splits) and press + 65:

| row | B | `c4` at + 45 → + 65 | `c5` | `cs` at + 45 → + 65 |
|---|---|---|---|---|
| CB1 | 55 | 945.000 → 890.000 | 1000.000 both | 945.000 → 890.000 |
| CB2 | 80 | 920.000 → 865.000 | 1000.000 both | 920.000 → 865.000 |
| CB3 | 105 | 895.000 → 840.000 | 1000.000 both | 895.000 → 840.000 |

  (On the crit, every hit of the press doubles: CB2 c4 840.000 → 730.000, and so on.) `c4` is inside the big blast
  (4.5 ≤ 5) and ONE split's (+z); `c5` inside neither (5.5 > 5; the -z split's cube ends 0.035 short of its box);
  `cs` inside the big one and the heading split's only. PLAYERHITs: the big one `sent=<B>.000`, each split `sent=55.000`.
- **The rewritten block, CB2, quoted whole:** `Cluster Bombs II | | Nature | Gear Score: 100 | | Fungal Barrage
  Right-Click | Lobs one great spore chunk that | bursts, then splits into three more. | Magic Damage: 80 | Magic
  Damage: 55 x 3 | Cooldown: 7.0s | Mana Cost: 30 | | It grew in the crimson dark, | and it does not like being picked. |
  | Uncommon Magic Weapon`. CB1 and CB3: the same with `Cluster Bombs I` / `55` and `Cluster Bombs III` / `105`.

### CB4 — CLUSTER BOMBS II AT SCORE 200

`dump: held` as CB2 but `Gear Score: 200`, **`Magic Damage: 160`** and **`Magic Damage: 110 x 3`**. `cs`: PLAYERHIT
`sent=160.000 … triggerScore=200`, PROBE **840.000**; then `sent=110.000`, **730.000** (or 680.000 → 460.000 on the
crit). Both doubled (Ben: "Yes it rises"); 160 and 110 differ, so a swap of the two shows.

### BF8 — NOTHING LEFT

`ENTITIES ITEM count=0 radius=32` (no fused body anywhere near him); `STAGE clear_state … fused=0`. Every row's `CLEAN`
line reads `fused=0` (each row's chunks went off inside it: the longest, CB, ends at press + 65, after its last
detonation at press + 61), and **`RESTORED … fused=0`** at the run's end.

### BF9 — THE ONE ASK OF THIS GATE, LAST OF IT (F11)

A flower pot at F+(2, 0, 0) (a saved block), the Blast Fungus held, a plain red mushroom in his hotbar.
- **AUTO, before:** `BLOCKS POTTED_RED_MUSHROOM … count=0`, `BLOCKS FLOWER_POT … count=1`, the Blast Fungus in his
  inventory.
- **The ASK, 3600 ticks:** *"Right-click the flower pot 2 blocks east of you three times: first holding the Blast
  Fungus; then press F to put the Blast Fungus in your off hand and right-click the pot with your main hand empty; then
  holding the plain red mushroom. Did only the third click put a mushroom in the pot, and do you still have the Blast
  Fungus?"* **Predicted: yes.** (The first click casts: its chunks rest at the pot and go off harmlessly.)
- **AUTO, after:** `POTTED_RED_MUSHROOM … count=1`, `FLOWER_POT … count=0`, the Blast Fungus still in his inventory.
- **Why an ASK and not an AUTO row:** the `use:` driver fires a synthetic `RIGHT_CLICK_AIR`, which no vanilla block use
  follows, so a plain red mushroom would not be potted by it either: the control would be hollow. Only his real clicks
  reach `FlowerPotBlock.useItemOn`. The plain mushroom is the control that the pot DOES take a mushroom.

### TT20 TO TT27, BY REFERENCE

As `GATE-true-tooltips.md`, "THE CLASS LABELS", predicted there and unchanged by this stack (no file they read moves).

## LINT AND THE SCAN RUN BEFORE COMMIT

`ScenarioLint` over all 52 gates: no new finding (its pins in `SelfTestScenariosTest` and `AskWindowLintTest` unchanged:
BF9 waits 3600). The gate-document test sees both sections. The R0b script's run is quoted under R0.

## CC'S CALLS IN THIS GATE

1. **Two questions in one command** (BF9 here, TT27 by reference), flagged above.
2. **BF6 freezes the wolf** (`data merge … {NoAI:1b}`, then a teleport) so it stands inside the blasts by staging, not
   by its AI; the alternative reads wherever it wandered, which can leave the row hollow.
3. **Predictions to 0.002** on positions (three printed decimals and the flight's doubles).

## PLACEHOLDERS OPEN FOR BEN

The plan's list (`PLAN-blast-fungus.md`, OPEN FOR BEN), and BF9's wording.

## WHAT ONLY THE BOOT CAN SHOW

That the chunks are seen thrown, resting and bursting (the trail, the bodies, the blast; their sounds); that a resting
body stays put on the real pad; that the server's box test and real floor agree with the model's 0.002; that vanilla
refuses the pot to a minted mushroom in both hands (BF9); and that the Cooldown line he hovers is legible (TT27 asks the
labels only).
