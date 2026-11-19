# PLAN — THE DRAGON FIGHT: BEN'S RULINGS OF 2026-11-18, THE cfde822 SURVEY, AND WHAT THE SERVER ALLOWS

> **READ THIS FIRST.**
> - **Docs only.** No Java, no content, no gate, no boot. Nothing below is built.
> - **Base:** `master` at `25d5e870` (the squash of #206).
> - **What governs:** §1 (Ben's words) and the parts of §2 marked as BEN'S RULING. Everything else in §2 is the seat's
>   reading and says so. §3 is a fact about cfde822 and is never adopted by being written down. §4 is a fact about
>   the pinned jar. §6 is a proposal; Ben rules the order. §7 is open.
> - **The mobs are not here.** The four fight mobs (the Knell, the Phage, the Phalanx, the Skreel and its mount) are
>   built and live in `PLAN-dragon-mobs.md`; its §6 holds the fight's design constraints C1 to C6, which bind this
>   plan.

---

## 1. RULINGS

*Ben's words, VERBATIM, his spelling included, all dated **2026-11-18**. Where the seat asked a question, the
question comes first, in the seat's words, then Ben's answer.*

### First message: answering the seat's nine decisions

*The nine decisions, as the seat put them: (1) the dragon, (2) the storm, (3) the mini-bosses, (4) the crystals,
(5) the waves, (6) the Endermen, (7) entry, group size and death mid-fight, (8) loot, (9) dialogue.*

> "1. Not sure yet, for now it stays vanilla with increased health
> 2. Yes keep the storm largely the same but with some tweaks ill speak to in a moment
> 3. Yes to the dark mage, no to the commander, again with changes
> 4. Not sure yet
> 5. we will rework it entirely
> 6. drop the war, the enderman are now the main fodder enemy
> 7. not sure yet
> 8. we will keep one guaranteed item per player but I want to build a cascading loot table system for all drops, not just this fight
> 9. Likely keep it"

### Second message: the problems with the old design, and the new storm

> "There was a few problems with the old design:
> 1. The random circle appearing with lightning was very hard to keep track of for a few reasons: players focusing on the fight and the particle render distance not showing the circle. This is less important for the arena border.
> 2. The Mage and Dragon both were basically immune to the entire melee class.
> My solutions to this:
> Keep the 48 block border ring. Instead of a random circle appearing, the Mage teleports somewhere in the arena (glowing) and channels the storm towards him, pulling the border circle closed around him. I want the circle to close in 2 ways. First it quickly closes (Slightly slower than a player sprinting) from 48 blocks around the center to 12 blocks around the Mage. Once it gets to this point, the storm starts slowly closing and the end crystals respawn one at a time aiming at the mage. If all the crystals power the mage before it is killed, the players die and the fight ends. Killing mobs while the PLAYER is inside the storm expands it. Im sure im probably missing some details so ask questions if you have any"

### Third message: answering the seat's questions

*The seat's questions: (1) does "inside the storm" mean inside the ring; (2) is outside still lightning every 2 s;
(3) can players destroy crystals; (4) does a fully closed ring also kill; (5) how far can kills expand the ring;
(6) does the Mage stay put, and can he be hurt from landing; (7) on the Mage's death, does the ring return to 48,
do the crystals go out, does the dragon become hurtable; (8) one Mage per phase; (9) the dragon and melee.*

> "1. Yes, a player inside the ring, specifically the 12 block ring while the mage is channeling
> 2. Yes, but we will change the damage to ramp based on how many times in a row the player has been struck
> 3. no they cannot
> 4. Technically yes, if the ring fully closes they it cannot be reopened and players die to lightning
> 5. The 12 blocks around the mage
> 6. Yes he stays put. Only damagable while channeling
> 7. Correct, the 48 block ring should be returned gradually, an inverse of it's shrinking instead of a snap back
> 8. One mage per phase
> 9. Leave it alone for now. One change I want to make is removing the lingering fireball attack from the dragon."

### Fourth message: when the Mage can be hurt

> "No he cannot be hurt during the fast close, only once the slow close starts"

### Fifth message: the fireball and the perched breath

*The seat asked whether the lingering cloud should go from the fireball only, or from the perched breath too.*

> "Only the fireball"

**So:** the dragon's fireball attack is removed. **The perched breath stays vanilla, its lingering cloud included.**

### Sixth message: how the ring is drawn, and the build order

*The seat asked (1) how the ring is drawn: solid display segments, forced particles, or the square world border;
(2) whether CC's proposed build order stands.*

> "1. I would like to keep particles. We can try forced particles if that helps. I don't care about the minimal particle setting.
> 2. Order is fine. Lightning ramping damage numbers can start at 20, 40, 80 and keeps doubling"

**Seat readings of the sixth message, not Ben's words:**
- The ring is drawn with **particles sent with force** (step F4).
- Visibility under the client's **Minimal** particle setting is **accepted as not required**.
- §6's build order is **RULED**.
- A player's **k-th consecutive strike deals 20 × 2^(k−1)**, with **no cap**.

### Seventh: Ben's answers at the first boot (`GATE-fight-ring.md`, booted at `f85280ee`)

*Each question as the selftest asked it, then Ben's answer, verbatim from the gate's ASK lines.*

1. "Standing here, can you see the whole ring clearly, all the way round?" — **"yes"**
2. "Sprinting east, did the ring's west edge catch you? And did its speed feel slightly slower than a player
   sprinting?" — **"no"** *(the answer does not say which of the two questions it answers)*
3. "The slow close (after it reached 12 blocks): is its pace right?" — **"I think it should be a little faster. Maybe
   .6 blocks per second"**
4. "When the mob died the ring jumped out one block at once. Is that fine, or should it glide out?" — **"the jump is
   fine"**
5. "Do 20, then 40, then 80 feel right for lightning outside the ring?" — **"yes"**
6. "Should armour reduce the lightning's damage?" — **"yes"**
7. "The ring's look: is the colour, the height and the density right? Say what you would change." — **"Higher wall and
   a larger gap between each row. Adding a slight twist to the particles would be cool"**

### Eighth message: the fast close, the Mage's health, the wall, the Mage's body

*The seat asked (1) whether 5 blocks a second felt right for the fast close; (2) the Mage's starting health, suggesting
1,500; (3) the wall as four rows 1.5 blocks apart, each row shifted round from the one below, the whole wall turning
slowly; (4) keep the Mage as a glowing wither skeleton.*

> "1. Let's lower it to 3.8 blocks a second to allow players to react and catch up when struct
> 2. 1500 works for now
> 3. yes
> 4. yes for now"

**So, Ben's from here on:**
- `v_fast` **3.8** and `v_slow` **0.6** (seventh, answer 3).
- The Mage's health **1500** ("for now").
- **Armour reduces the lightning** (seventh, answer 6).
- **A kill's pop stays a jump** (seventh, answer 4).
- **The wall:** four rows 1.5 apart, shifted and turning.
- **The Mage:** a glowing wither skeleton ("for now").

---

## 2. SEAT READINGS

*Each item is marked. Only (a) is Ben's; every other item is the seat's reading of his words, not his words, and
stands until he rules otherwise.*

- **a. BEN'S RULING, not a seat reading:** the Mage cannot be hurt during the fast close. **He is damageable only from
  the start of the slow close (the ring at 12 blocks around him) until he dies.** *The seat's earlier reading, that he
  is damageable from the moment he lands, was wrong and is withdrawn* (fourth message, §1).
- **b. Seat reading, not Ben's words.** A fully closed ring is not an instant wipe. No point is inside it, so every
  player is outside, and the ramping lightning kills. It cannot be reopened.
- **c. Seat reading, not Ben's words.** A player's strike count (the "how many times in a row" of the third message,
  answer 2) resets when that player is back inside the ring.
- **d. Seat reading, not Ben's words.** Kills expand the ring only during the slow close, only when the killer is
  inside it, and never past 12 blocks around the Mage.
- **e. Seat reading, not Ben's words.** The Knight Commander is removed ("no to the commander"). The Goldrot Goon was
  already retired (`PLAN-dragon-mobs.md`, its RULINGS of 2026-11-15).
- **f. Seat reading, not Ben's words, FLAGGED TO BEN AS A CONSEQUENCE, NOT RULED.** Endermen are Void
  (`PLAN-dragon-mobs.md` §6 C6), so **an Enderman that dies within 6 blocks of a small Phage grows it**. With
  Endermen as the main fodder (first message, answer 6), every fodder kill near a Phage feeds it. Whether that is
  intended is not ruled.
- **g. Seat reading, not Ben's words.** The dragon is immune while the Mage is alive (cfde822's rule, §3, kept).
  **Crystals as a separate immunity gate: OPEN** (Ben: "Not sure yet").

### SEAT MECHANISM RULING: THE RING IS ONE PURE SHAPE IN `core/`

*The seat's ruling on the mechanism, not Ben's words.* The ring is **one pure function in `core/`**: given the fight's
clock, it returns a centre and a radius. No Bukkit. Everything that draws the ring, strikes outside it or measures
"inside" reads the same function, so the picture and the damage cannot disagree.

**The quantities, named.** Every number not quoted from Ben is OPEN (§7).

| name | what it is | value |
|---|---|---|
| `C` | the arena's centre (from the template, never (0, 0); C1 and C3) | template |
| `R_border` | the border ring's radius about `C` | **48** (Ben, "Keep the 48 block border ring") |
| `M` | where the Mage lands | OPEN (where he may land, §7) |
| `R_mage` | the ring's radius about `M` at the end of the fast close | **12** (Ben) |
| `d` | the distance from `C` to `M`, horizontal | follows from `M` |
| `v_fast` | the speed of the ring's FASTEST-MOVING point during the fast close, blocks per second | **5.0**, *seat placeholder, not Ben's; he sets it at the first boot* ("Slightly slower than a player sprinting") |
| `v_slow` | the slow close's rate, blocks of radius per second | **0.4**, *seat placeholder, not Ben's; he sets it at the first boot* |
| `e_kill` | the radius one counted kill adds back during the slow close, blocks | **1.0**, *seat placeholder, not Ben's; he sets it at the first boot* |
| crystals | how many light, and how often, from the start of the slow close | **10, one every 80 ticks**, *seat placeholder, not Ben's; he sets it at the first boot* |
| the return's speeds | how fast the ring returns after the Mage's death | **the same as the close**: the slow part at `v_slow`, the fast part capped at `v_fast`; *seat placeholder, not Ben's; he sets it at the first boot* |
| strike damage | the k-th consecutive strike outside the ring | **20 × 2^(k−1), no cap** (Ben, sixth message; the seat's reading of "start at 20, 40, 80 and keeps doubling"). **Whether it is also multiplied by gear score stays OPEN** (`PLAN-dragon-mobs.md` Q3); F2 does not scale. |
| `r(t)` | the ring's radius at time `t` | follows |
| **SINCE 2026-11-18 (eighth message): `v_fast`** | | **3.8, Ben's** ("Let's lower it to 3.8 blocks a second"). The 5.0 above was the seat's placeholder |
| **`v_slow`** | | **0.6, Ben's** ("Maybe .6 blocks per second"). The 0.4 above was the seat's placeholder |
| **strike mitigation** | | **armour reduces it, Ben's** (seventh, answer 6: "yes"); **no element, OPEN** (whether Void resistance or any element applies) |
| **the Mage's health** | | **1500, Ben's** ("1500 works for now"), at gear score 100 |
| the Mage's name | | "Withered Dark Mage", *seat placeholder (cfde822's name)* |
| the refused-hit cue | | `item.shield.block` at the Mage, volume 1.0, pitch 0.8, *seat placeholder* |
| crystal spots | | `crystal_count` spots evenly round a circle about C, radius 40, 12 above C's ground, *seat placeholder until the arena* |
| the crystal cue | | `block.beacon.power_select` at the crystal, *seat placeholder* |
| the wall | | rows at +0.2, +1.7, +3.2, +4.7 (Ben's "yes" to four rows 1.5 apart); `row_shift_blocks` 0.25 and `turn_blocks_per_second` 1.0, *seat starting values* |

**The fast close** (*seat derivation, to be pinned by a `core/` unit test before anything draws it*). The ring moves
from (`C`, 48) to (`M`, 12) along one straight interpolation: at progress `s` from 0 to 1, the centre is
`C + s·(M − C)` and the radius is `48 − 36·s`. A point on the ring at angle `θ` then moves, per unit of `s`, by
`(M − C) − 36·u(θ)`, where `u(θ)` is the unit vector at `θ`. **Its fastest point** is the one where `u(θ)` points
away from `M`, and it moves `d + 36` blocks per unit of `s`. **So no point outruns `v_fast` when `s` advances at
`v_fast / (d + 36)` per second, and the fast close lasts `(d + 36) / v_fast` seconds.** Two consequences:
- **The rings nest only when `d ≤ 36`.** Each later ring lies inside every earlier one exactly when the Mage lands at
  most 36 blocks from `C` (`R_border − R_mage`). Past that, part of the 12-block ring lies outside the 48 border, and
  a player standing still can find the ring sweeping OUTWARD over them. **This bounds where the Mage may land**
  (§7), and it is the seat's derivation, not a ruling.
- **The fast close's duration depends on where he lands.** A Mage at `d = 0` closes in `36 / v_fast` seconds; one at
  `d = 36` in `72 / v_fast`.

**The slow close.** The centre stays at `M`; the radius falls from 12 at `v_slow`. A counted kill (§2 d) adds
`e_kill`, capped at 12. At 0 the ring is closed for good (Ben, third message, answer 4).

**The return after the Mage's death** is the same path run backwards (Ben, answer 7: "an inverse of it's shrinking
instead of a snap back"): from (`M`, its radius at his death) back up to (`M`, 12), then from (`M`, 12) back to
(`C`, 48) with the same fastest-point cap. **Whether the slow part of the return runs at `v_slow` or faster is OPEN**
(§7). *(Since 2026-11-18 a seat placeholder runs it at `v_slow`, in the quantity table; Ben sets it at the first
boot.)*

**Lightning.** Outside the ring, a player is struck every 40 ticks (2 s, Ben's answer 2 keeps it). The damage ramps
with that player's consecutive strikes, `k`, and `k` resets inside (§2 c). The ramp's numbers are OPEN. Outside the
48 border, the same lightning applies (cfde822's border, §3; Ben's second message keeps the ring).

---

## 3. THE OLD FIGHT (cfde822 SURVEY)

*Read at `CreaperCrusher/Butterfly-SMP` commit `cfde8222ec2ac35661963776e9d8f0582bc4e12a`, fetched 2026-11-18. Paths
are under `src/main/java/org/example1/butterflysmp/`. **Line citations are safe here and only here**: they name a
fixed commit, which no insertion can move.* Each fact the seat reported is marked CONFIRMED or WRONG. Where the code
adds something the brief did not say, it follows as **Also**.

**Tally: 33 facts checked, 33 CONFIRMED, 0 WRONG.** Several carry an **Also** that changes how they read; those are
the ones to read.

### 3.1 The dragon

1. **HP 18000.** CONFIRMED. `DRAGON_PDC_MAX_HP = 18000.0` (`encounter/dragonfight/DragonfightManager.java:89`). The
   real HP lived in PDC; the engine's max health was pinned at 1000 (`:87`) and re-pinned every second (`:147-177`).
2. **+6000 per player beyond 3 in the largest party.** CONFIRMED (`:93`, `:96`, `:476`). **Also:** counted once at
   the fight's start, over the players in the End world, grouped by party leader, a solo player as a group of one
   (`computeAdaptiveExtraPlayers`, `:568-578`). Never recounted.
3. **Thresholds in code 0.80 / 0.60 / 0.40; comments say 75 / 50 / 25.** CONFIRMED. `double[] thresholds = {0.80,
   0.60, 0.40}` (`:822`); the comments at `:53` and `:833` say 75%, 50%, 25%. Three crossings, then no more phases
   (`:813`).
4. **Immune while any End crystal, Dark Mage or Knight Commander lives.** CONFIRMED
   (`encounter/dragonfight/DragonfightCombatListener.java:87-132`, `onDragonfightImmunityGate`, cancels the event).
   **Also:** "any crystal" means any live `EnderCrystal` in the dragon's world, not the fight's own (`:127`). And a
   stale comment in the manager says the opposite, *"no crystal invulnerability"* (`DragonfightManager.java:804`).
5. **Perched dragon takes × 0.4 from non-melee.** CONFIRMED (`DragonfightCombatListener.java:63-81`). Melee means a
   `Player` damager with cause `ENTITY_ATTACK` or `ENTITY_SWEEP_ATTACK`. **Also:** "perched" is the phase being one of
   `LAND_ON_PORTAL`, `LEAVE_PORTAL`, `BREATH_ATTACK`, `SEARCH_FOR_BREATH_ATTACK_TARGET`, `ROAR_BEFORE_ATTACK`
   (`encounter/BossService.java:208-215`). And every `MAGIC` hit was × 4, to undo vanilla's 0.25
   (`DragonfightCombatListener.java:50-56`).
6. **No invulnerability frames.** CONFIRMED. Each hit schedules `setNoDamageTicks(0)` for the next tick
   (`DragonfightManager.java:795-800`), so it is cleared one tick late, not prevented.

### 3.2 The Dark Mage (`encounter/withered/WitheredDarkMageBehavior.java`)

7. **HP 7200.** CONFIRMED (`:54`).
8. **+2400 per extra player.** CONFIRMED (`:98-101`; `DragonfightManager.java:99`), using the dragon's count from
   fact 2.
9. **On a pillar top.** CONFIRMED (`:79`, `pickRandomPillarTop`). **Also:** the pillars were found by scanning the
   End within ±60 of (0, 0) for columns 5 or more blocks above the central island (`:183-240`), falling back to ten
   hardcoded pillar coordinates (`:49-52`).
10. **AI off.** CONFIRMED (`m.setAI(false)`, `:84`); gravity off too (`:85`); knockback resistance 1.0 (`:103-104`).
11. **Glowing.** CONFIRMED (a permanent `GLOWING` effect, `:107-108`).
12. **Teleports every 8 to 14 s.** CONFIRMED. `160 + random × (280 − 160)` ticks (`:55-56`, `:127-142`), to a
    different pillar at least 5 blocks away when one exists (`:155-166`).

**Also, the Mage's damage gate:** he took damage only while the storm was in GRACE or ACTIVE
(`encounter/StormSystem.java:602-611`, `onWitheredDarkMageStormGate`, cancels the event).

**Also, one Mage per threshold crossing:** `spawnPhaseWave` spawns one at every crossing
(`DragonfightReinforcements.java:184-188`), refused while one is alive by the Mage cap of 1
(`DragonfightManager.java:114-117`; `WitheredDarkMageBehavior.java:73-78`). The wave's own comment says phase 0's
wave has none (`DragonfightReinforcements.java:137-141`); the code spawns one at the first crossing too.

### 3.3 The storm (`encounter/StormSystem.java`)

13. **Brew 80 t.** CONFIRMED (`STORM_BREWING_TICKS`, `:70`).
14. **Grace 160 t.** CONFIRMED (`:71`).
15. **Active 800 t.** CONFIRMED. `STORM_DURATION_TICKS = 960` is grace plus active (`:72`); ACTIVE begins at 240
    ticks and ends at 80 + 960 = 1040 (`:283`, `:293`).
16. **Circle radius 8.** CONFIRMED (`:78`, set at `:276`).
17. **Centred 12 to 25 from the middle.** CONFIRMED (`:76-77`, `:268-275`). **Also:** "the middle" is (0, 0), not the
    arena; the comment says 15 to 30 (`:267`).
18. **Shrink 0.4 / 0.6 / 0.8 blocks per s by phase.** CONFIRMED (`:81`, `:374-384`), read from the dragon's current
    phase each step.
19. **Lightning every 40 t.** CONFIRMED (`:73`, `:299-302`), in ACTIVE only.
20. **160 / 200 / 240 outside.** CONFIRMED (`STORM_LIGHTNING_DAMAGE`, `:89`, for phases 1, 2, 3; `:446-470`).
    **Also:** a real `world.strikeLightning` at the player PLUS `p.damage(damage, LIGHTNING_BOLT)` (`:467-468`), so
    the vanilla bolt's own damage and fire came on top. A player outside 48 is skipped here, because the border
    strikes them (`:461-463`).
21. **+1 radius per kill by a player inside.** CONFIRMED (`:543-567`, `onStormMobKillGrowth`). **Also:** in ACTIVE
    only, not GRACE (`:557`); capped at the starting 8 (`:563-565`); ANY non-player death counted, not only fight
    mobs (`:547`); a summon's kill counted for its owner (`:549-554`, `:575-596`).
22. **Radius 0 wipes and fails.** CONFIRMED (`:311-317`). The wipe is `p.damage(health + 1000, LIGHTNING_BOLT)` to
    every non-creative, non-spectator player in the world (`:472-481`).
23. **3 Phages every 60 t around it.** CONFIRMED (`:98-101`, `:393-416`). **Also:** placed 20 to 35 blocks OUTSIDE
    the circle's edge (`:408-410`); in GRACE as well as ACTIVE (`:289`, `:303`); under the Phage cap. And while the
    storm is in GRACE or ACTIVE, the reinforcement trickle spawns only Phages, 3 per tick
    (`DragonfightReinforcements.java:242-246`).
24. **Next storm 600 t.** CONFIRMED (`STORM_GAP_TICKS = {-1, 600, 600, 600}`, `:92`). **Also:** the comment says
    90 / 60 / 30 s (`:91`); every phase is 600.
25. **Or 400 t after a phase change.** CONFIRMED (`:95`, `:202-209`, consumed once at `:517-519`). **Also:** the
    Mage's death ends the storm and cancels the next brew, so no storm comes until the next crossing (`:214-218`).

**How the old code measured "player inside the circle"** (the brief's question): the player's feet position,
horizontal only, `dx² + dz² ≤ radius²` against the circle's centre (`:464-466`); height ignored. The kill-growth
check measures the KILLER's position the same way (`:560-562`). The border measures from (0, 0), not from any
arena: `x² + z² > 48²` and `< 300²` (`:366-368`).

### 3.4 The arena border

26. **Radius 48.** CONFIRMED (`ARENA_BORDER_RADIUS = 48.0`, `StormSystem.java:109`).
27. **The same lightning outside it.** CONFIRMED: the same 40 t cadence (`:114`) and the same per-phase damage array
    (`:236-247`). **Also, and it matters:** in phase 0 the damage is 0, so **the border did nothing before the first
    crossing** (`:240-241`); it struck **every living entity except the dragon**, fight mobs and Endermen included,
    between 48 and 300 blocks from (0, 0) (`:354-372`); it was drawn as 220 `DUST` particles at ground height every
    4 ticks (`:116`, `:341-349`), with no `force` flag.

### 3.5 Waves, caps and trickle (SUPERSEDED: Ben, "we will rework it entirely"; kept only as history)

*Already tabled in `PLAN-legacy-port.md` §3.0; restated here so this file stands alone.*
`Phage` was `spawnWitheredGrunt`, `Goon` was `spawnWitheredRouge`, `Phalanx` was `spawnWitheredArcher`, `Knell` was
`spawnWitheredKnight`.

**The wave at each crossing** (`DragonfightReinforcements.java:144-194`; the wave index is the phase being left):

| crossing | Phage | Goon | Phalanx | Knell | Skreel pairs | also |
|---|---|---|---|---|---|---|
| 80% | 6 | 3 | 0 | 0 | 0 | a Mage |
| 60% | 4 | 3 | 2 | 1 | 0 | a Mage |
| 40% | 2 | 2 | 3 | 3 | 2 | a Mage and the Knight Commander (`:168-179`) |

**The per-type caps on live mobs, by phase** (`PHASE_TYPE_CAPS`, `DragonfightManager.java:112-118`):

| phase | Phage | Goon | Phalanx | Knell | Skreel pair | Mage | Commander |
|---|---|---|---|---|---|---|---|
| 0 | 10 | 4 | 0 | 0 | 0 | 0 | 0 |
| 1 | 8 | 4 | 3 | 2 | 0 | 1 | 0 |
| 2 | 8 | 3 | 4 | 4 | 3 | 1 | 1 |
| 3 | 7 | 2 | 4 | 5 | 4 | 1 | 1 |

**The trickle:** one attempt every **160 / 120 / 80 / 60 ticks** by phase (`DragonfightReinforcements.java:40`,
`:200-210`), started at the fight's first tick (`DragonfightManager.java:545`). Each attempt tries one Phage and one
Goon, plus a Phalanx and a Knell from phase 1, plus a Skreel pair from phase 2 (`:248-259`), at one random anchor.
**The anchors:** 3, evenly spaced on a **radius-15 ring around (0, 0)** with a random rotation, each spawn jittered
±2 blocks (`:43-45`, `:115-132`, `:263-267`).

**The Knight Commander** (removed by Ben; history only; `encounter/withered/WitheredCommanderEncounter.java`): HP
5600 (`:133`), netherite armour with Protection III and a Sharpness V netherite sword (`:107-128`), follow range 64
(`:135`), a wither-bomb small fireball every 100 ticks at a target within 30 blocks (`:70-71`, `:161-211`), and kept
20 or more blocks from the storm's centre (`DragonfightManager.java:589-648`).

**The war:** every second, each fight mob without a real target was pointed at the nearest Enderman within 30
blocks, and each idle Enderman at the nearest fight mob (`DragonfightFactionListener.java:46-114`); natural Enderman
spawns were refused within ±200 of (0, 0) while a fight ran (`:135-144`). Dropped by Ben (first message, answer 6).

### 3.6 Failure

28. **All players within 48 dead.** CONFIRMED (`DragonfightManager.java:849-878`). Creative and spectator do not
    count as alive; the dying player is excluded; "within 48" is horizontal from (0, 0).
29. **Or the storm closed.** CONFIRMED (`StormSystem.java:311-317`).
30. **Mobs dropped nothing.** CONFIRMED. A death tagged `DRAGONFIGHT_MINION` clears drops and sets XP to 0
    (`DragonfightManager.java:893-899`).

### 3.7 Win

31. **Top three damage announced.** CONFIRMED (`encounter/BossLootListener.java:392-409`, called from
    `DragonfightManager.java:929`), broadcast server-wide.
32. **Each player in the End gets one item at 2% legendary / 13% epic / 85% rare.** CONFIRMED
    (`BossLootListener.java:216-233`: `roll < 0.02`, `< 0.15`, else). **Also:** "each player" is every player in the
    dragon's world, with no presence, damage or game-mode check; the method's own comment says non-creative,
    non-spectator (`:213`), and the code does not filter (`:220`). Overflow drops at the player's feet; each find is
    broadcast server-wide (`:259-283`).
33. **The item ids per tier** (`BossLootListener.java:176-210`; ids from `item/ItemFactory.java`):

| tier | ids |
|---|---|
| Legendary (1) | `DRAGONS_PLUME` |
| Epic (4) | `WITHERED_FALCHION`, `WHISPER_OF_THE_VOID`, `KNELL_BEAM`, `VOIDBLOOM_CHARM` |
| Rare (20) | the four pieces each of the `WITHERED_KNIGHT`, `DRAGON_SCALE`, `GOLDROT` and `VOID_BEARER` diamond armour sets (16, non-Elite); `WITHERED_GAUNTLET`, `VOID_QUIVER`, `GOLDROT_CHALICE`, `VOIDREN_HEART` |

*`combat/DragonsRageListener.java` was in the brief's file list. It is the Dragon Scale **set bonus** (+100% speed
for 20 s at 40% health or below; elite +160% and +25% ranged), not part of the fight; recorded and not surveyed
further.*

### 3.8 The Dark Mage's attacks, if any

**None.** His AI was off (`WitheredDarkMageBehavior.java:84`), and his targeting of players and Endermen was
cancelled (`:274-283`). A search of all of `src/main` for `Dark Mage`, `DARK_MAGE` and `DarkMage` finds only his
spawn, his registration, the storm gate and the immunity gate. **His threat was the storm; he dealt no damage
himself.**

### 3.9 Dialogue, verbatim

**The three phase scripts** (`DragonfightReinforcements.java:54-88`): eight lines per crossing, 80 ticks apart
(`:42`, `:269-278`), sent to the End world only. `§a` is the green speaker, `§b` the cyan; `§c§kXXXXXXXX` is an
obfuscated red name.

```
At the 80% crossing ("Phase 1 (75%)" in the source):
§aSomething strange is happening...
§bWhat's going on?
§c§kXXXXXXXX§r§a is breaking down.
§aThere's a tear!
§aThey're getting through!!
§bCalm down. Can we seal it?
§aNot for long. And not alone.
§aWe need help.

At the 60% crossing ("Phase 2 (50%)"):
§aThey're showing up all over the Nexus.
§aThey're spreading like a cancer.
§bSever the corrupted worlds from the Nexus.
§bBefore it's too late.
§aNo! We can't!
§aTrillions will die!
§bAnd many more if we don't!
§bWe can't save everyone.

At the 40% crossing ("Phase 3 (25%)"):
§aWait! The people of this world are fighting back.
§aAnd they're winning!
§bIt doesn't matter.
§bSever the connection now!
§aNo! They can win.
§bFine, §c§kXXXXXXXX§r§b.
§bLet them tear it all down.
§bThe worlds will burn.
```

**The fight's other lines:**

```
Start, server-wide (DragonfightManager.java:561):
§6[Boss] §5The Ender Dragon §6awakens! §7The fight has begun.
Failure, server-wide (:725-726), the reason one of "all challengers have fallen", "the storm consumed the arena",
"manual failure triggered by <name>":
§6[Boss] §5The Ender Dragon §6retreats into the void... §c[The fight has failed: <reason>]
A Mage spawns, End world (DragonfightReinforcements.java:187):
§5✦ A Withered Dark Mage appears atop a pillar...
Storm brewing / raging / fading, End world (StormSystem.java:490, :284, :496):
§5⚡ The Storm is brewing…
§5⚡ The Storm rages!
§5⚡ The Storm has faded.
A Mage dies, End world (DragonfightManager.java:911, :915):
§6[Dragonfight] §5The Withered Dark Mage §6has fallen! §7The dragon is exposed.
§5⚡ The Storm fades with the Mage's fall.
The Commander dies, End world (WitheredCommanderEncounter.java:361):
§6[Dragonfight] §4The Withered Knight Commander §6has fallen!
Win, server-wide (DragonfightManager.java:944; BossLootListener.java:400, :407, :277-282):
§6[Boss] §5The Ender Dragon §6has fallen!
§6[Boss] §7Top damagers vs §f§5The Ender Dragon§7:
  §6① §f<name> §7— §c<damage> dmg      (then §7② and §c③)
✦ <name> found <item name>!
```

*All of these are legacy `§` text; a port writes them as MiniMessage (CLAUDE.md, BANNED PATTERNS).*

---

## 4. WHAT THE SERVER ALLOWS

*Read 2026-11-18 by javap against `run/versions/26.1.2/paper-26.1.2.jar` (mojang-mapped) and from the paper-api
`26.1.2.build.74-stable` sources jar. **The two are the same build:** the server jar's manifest reads
`Implementation-Version: 26.1.2-74-e4e17fc`, `Build-Number: 74`, matching `pom.xml`'s `paper.version`. Nothing from
memory; no boot. Each claim is marked **READ** (seen in bytecode or source), **INFERRED** (follows from what was
read; says from what) or **UNVERIFIED** (not settled). The reading was delegated to a subagent; **the seat
re-ran the decisive one (c, the dragon's nodes) with a control** and it held.*

> **CORRECTED 2026-11-18: CC ran that control, not the seat.** The line above, and "The seat's control" in the next
> section, credit the seat with a check that CC (Claude Code) ran; the seat verifies from origin and did not re-run
> the jar. The finding stands; only its attribution was wrong. Kept as written, with this note, so the correction is
> visible.

### THE FINDING THAT SHAPES THE ARENA: THE DRAGON ORBITS (0, 0) OF ITS WORLD, WHATEVER WE SET

**READ:** `EnderDragon.findClosestNode()` builds its 24 path nodes once, on rings of radius **60** (nodes 0–11),
**40** (12–19) and **20** (20–23), from `Mth.cos` / `Mth.sin` of fixed angles, with **no `fightOrigin` term**. The
seat's control *(CC's, not the seat's: see the correction of 2026-11-18 above)*: `fightOrigin` appears 4 times in the class's bytecode and 0 times in that method. The node height is
`max(73, the MOTION_BLOCKING_NO_LEAVES heightmap + an offset)` (READ: 73, and the first ring's offset 5; the other
rings' offsets not read). **INFERRED: the arena's centre `C` must be (0, 0) of the temporary world.**

> **THIS MEETS C1** (`PLAN-dragon-mobs.md` §6), which forbids *"fixed coordinates as fight geometry ((0, 0), …)"*.
> One temporary world per group keeps it honest: (0, 0) is then the template's own origin, not a shared world's
> landmark, and no other fight is there. **But the template must be placed centred on (0, 0), and that is a
> constraint the dragon imposes, not a choice.** Flagged in §7.

### a. A temporary world: create, unload, delete

- **READ** `CraftServer.createWorld(WorldCreator)`: refuses only during server STARTUP. **INFERRED main thread
  only:** `prepareLevel` pumps server tasks until its pending chunks reach 0.
- **Cost. READ:** there is no spawn-radius preload any more. `WorldCreator.keepSpawnLoaded` is documented
  *"completely unfunctional"*, and `World.setKeepSpawnInMemory` *"No longer functional since 1.21.9"*. **READ:**
  without `WorldCreator.forcedSpawnPosition(...)`, a new world searches for a spawn, which loads chunks (INFERRED);
  with it, *"without loading any chunks"* (its javadoc). **INFERRED:** with a forced spawn, the cost is building the
  level and its generator, not chunks.
- **Unload. READ** `CraftServer.unloadWorld(World, boolean)`: returns **false if any player is in it**, moving no
  one; fires a cancellable `WorldUnloadEvent`; saves if asked; closes chunks and entities. **So everyone is moved
  out first.** UNVERIFIED: whether region files are released before it returns, so a delete retries.
- **Where it lives. READ:** `CraftWorld.getWorldPath()`. INFERRED: `world/dimensions/<namespace>/<key>`.
- **At a server stop. READ** `MinecraftServer.stopServer`: players are saved, then every level. **The temporary
  world and each player's position in it are written to disk.**
- **After a crash or a stop. READ:** at boot `PaperWorldLoader.loadInitialWorlds` loads only the registered
  dimensions, so **INFERRED: an API world is not reloaded**, and its folder is an orphan. **READ, decisive:**
  `PrepareSpawnTask.start` looks the saved world up and, if it is missing, uses the OVERWORLD with the **same saved
  position**. **INFERRED: a player who logged out or crashed inside an arena logs back in at the same x, y, z of
  the overworld.** **READ:** `AsyncPlayerSpawnLocationEvent` exists, which can send such a player elsewhere.
- **Against C1, C2, C4.** One world per group is buildable on the API alone. The fight must own the cleanup (move
  out, unload, delete), sweep orphans at boot, and catch stale joins. Q12 (a restart mid-fight) gets this answer
  from the server: the world is gone and the player is somewhere unsafe unless we catch it.

### b. The arena from a template file

- **READ:** `StructureManager.loadStructure(File)` and `(InputStream)`; `Structure.place(Location, includeEntities,
  StructureRotation, Mirror, palette, integrity, Random)`, plus overloads taking a `RegionAccessor` and
  block/entity transformers.
- **READ** `CraftStructure.place`: places with block flag 2 (client update, no neighbour updates); the only size
  check is a minimum of 1×1×1. The API javadoc says it may exceed the structure block's 48×48×48. No thread check;
  **INFERRED: synchronous on the main thread, loading every chunk it touches.**
- **Another route, INFERRED** from the `RegionAccessor` overload: place it during the temporary world's own
  generation, moving the cost into chunk generation.
- **Against C3:** a template file is supported directly. Place it with entities off, and spawn the fight's own
  entities ourselves (C4: the fight owns its list).

### c. An Ender Dragon outside the End's battle

- **READ:** `EnderDragon.aiStep` runs the phase manager; every use of `dragonFight` in `aiStep`, `findClosestNode`,
  `findPath`, `kill`, `tickDeath` and `getExpReward` is **null-guarded. Nothing breaks with no fight.**
- **READ:** a level gets its own vanilla `EnderDragonFight` when its dimension type has one. **INFERRED:** an
  END-environment temporary world would build a vanilla fight (a podium, its own dragon); **a NORMAL world does not.**
- **With no fight, READ:** the path and the holding, strafe and takeoff phases treat "no fight" as **no crystals
  alive**: the node floor is 12 (the inner rings, 40 and 20), and the holding pattern goes to landing on one draw in
  3 after each path. **INFERRED: it flies as if every crystal were destroyed, perching often; our own crystals do
  not change its AI.**
- **The perch. READ:** `getPodium()` returns a podium if one was set, else one derived from `fightOrigin`, which
  defaults to (0, 0, 0). The landing, takeoff and death phases use it. **READ Bukkit API:** `getPhase`, `setPhase`,
  `getDragonBattle` (null with no fight), `getPodium`, **`setPodium(Location)`** (same world only). `fightOrigin`
  itself is not in the API (NMS only: a cost, not needed).
- **READ:** `checkDespawn()` is empty (it never despawns). It breaks blocks only with `mobGriefing` on, only
  non-dragon-immune blocks, through a cancellable `EntityExplodeEvent`.
- **Against C1, C3:** a NORMAL world, the arena centred on (0, 0), `setPodium` on the template's perch,
  `mobGriefing` off. No NMS.

### d. The dragon's attacks

- **The fireball. READ** `DragonStrafePlayerPhase.doServerTick`: builds a `DragonFireball` and fires Paper's
  **`EnderDragonShootFireballEvent`**. Not cancelled: a sound (level event 1017) and the spawn. Cancelled: the
  fireball is discarded. **Either way the phase then resets its charge, advances its path and goes to
  `HOLDING_PATTERN`.** With no target it returns to `HOLDING_PATTERN` too.
- **The lingering cloud from the fireball. READ** `DragonFireball.onHit`: it makes an `AreaEffectCloud`
  (`DRAGON_BREATH`, instant damage) and fires `EnderDragonFireballHitEvent`; cancelled, the cloud is discarded.
- **The cleanest removal (Ben's fifth message):** **cancel `EnderDragonShootFireballEvent`.** READ: the phase machine
  runs on exactly as if it had fired, and no fireball means no cloud. Redirecting the phase instead is worse: READ,
  `EnderDragonPhaseManager.setPhase` ends the old phase BEFORE firing `EnderDragonChangePhaseEvent`, and READ,
  `DragonChargePlayerPhase` with no target aborts back to `HOLDING_PATTERN`, so a redirect bounces.
- **The perched breath, a fact about the jar only; RULED to stay vanilla (§1, fifth message).** READ
  `DragonSittingFlamingPhase.doServerTick`: at flame tick 10 it makes its own `AreaEffectCloud` (radius 5, duration
  200, `DRAGON_BREATH`, instant damage) and fires `EnderDragonFlameEvent`; at 200 it moves to takeoff or scanning.
  **Nothing removes it unless we cancel that event, and we will not.** READ, also: while perched,
  `AbstractDragonSittingPhase.onHurt` makes arrows and wind charges deal 0 (a fact for the melee question, which
  Ben left alone: "Leave it alone for now").

### e. End crystals

- **READ:** spawned with `world.spawn(..., EnderCrystal.class)`, removed with `remove()`. `setBeamTarget(Location)`
  (same world only) and `setShowingBottom` exist and are synced. READ: a crystal with a beam target always renders
  at any distance on the server's side. **UNVERIFIED: whether the beam renders outside the End**; it is client
  code. **A boot must look.**
- **Unbreakable. READ** `EndCrystal.hurtServer`: the base invulnerability check, then a dragon attacker is refused,
  then **a cancellable damage event**; then explosion damage removes it with no new blast, and any other damage primes
  a radius-6 explosion and removes it. **READ:** `setInvulnerable(true)` is bypassed by `/kill`-class damage and by a
  creative player. **So: cancel the damage event for our crystals** (players, projectiles and explosions alike),
  with `setInvulnerable(true)` as the backstop.
- **READ:** fire under a crystal and Paper's invulnerable-crystal fix apply only with a dragon fight, so neither
  acts in a NORMAL world.

### f. A moving ring at radius 48, seen by every player

| option | READ | limit |
|---|---|---|
| **Particles with `force`** | `CraftWorld.spawnParticle(..., force)` sends to players within **512** blocks with force, **32** without (`ServerLevel.sendParticles`). **The API never sets the packet's `alwaysShow` flag**; the `ParticleBuilder.force` javadoc says without force *"the particle will show according to the client's particle settings"*. PacketEvents 2.13.0's particle wrapper exposes both `longDistance` and `alwaysShow`. | **UNVERIFIED (client):** whether force alone survives "Minimal"; `alwaysShow` is the flag meant for that, and only a packet can set it. A 48 ring is ~301 blocks round: hundreds of particles per redraw per player. **This is Ben's complaint about the old ring** (§1, second message), which used neither flag (§3 fact 27). |
| **Display entities** | Block, item and text displays track at 10 chunks; this server's `spigot.yml` sets `entity-tracking-range.display: 128`. `setTeleportDuration` (0 to 59 ticks), `setInterpolationDuration`, `setTransformation`, `setViewRange` exist. | **INFERRED:** independent of particle settings, and every ring point is within ~96 blocks of a player inside it, so the ring stays tracked. A smooth ring is some tens of stretched segments (INFERRED, ~48 to 96); moving and resizing it means teleporting each segment with a duration and interpolating its transform. |
| **A per-player world border** | `Player.setWorldBorder`, `Server.createWorldBorder`, `WorldBorder.setCenter` (instant) and `changeSize(size, ticks)` (smooth). | **INFERRED from the server's own bounds check: SQUARE**, and nothing makes it round. Moving it is repeated `setCenter`, a jump (client interpolation UNVERIFIED). A per-player border is packets only; the world's own border is the damaging one. |

**INFERRED, the seat's reading of the table:** display entities are the only round option that does not depend on
the client's particle setting; particles through PacketEvents with `alwaysShow` are an accent; the border only if a
square is acceptable. **Ben picks** (F4 waits on it). And the ring's damage never reads the display: it reads the
`core/` shape (§2), so the picture is never the test.

### g. Lightning as effect only

- **READ:** `World.strikeLightningEffect` marks the bolt as an effect; `LightningBolt` then skips its fire, its
  entity hits and its lightning-rod branch. **No damage and no fire.** It still fires `LightningStrikeEvent`.
- **READ:** the thunder and impact sounds are played client-side; the API javadoc says the sound *"is now client
  side and cannot be removed"*. Tracking range 16 chunks; `setFlashCount` exists.
- **So:** the effect, and our own damage through our pipeline. **cfde822 used the REAL bolt plus its own damage**
  (§3 fact 20), so vanilla's bolt damage and fire landed on top; the effect bolt removes both.

### h. Endermen as fodder

- **Hostility. READ:** `EnderMan.setTarget` applies its attack speed and its "creepy" flag. **READ:** `aiStep` renews
  the anger timer every tick while a target exists, ending only if the target dies (and is a mob), is creative or
  spectator, or the difficulty is PEACEFUL. **INFERRED: `setTarget(player)` sticks.** `setScreaming` and
  `setHasBeenStaredAt` are cosmetic flags (READ).
- **Teleporting. READ:** Paper's **`EndermanEscapeEvent`** covers every self-teleport: running from daylight,
  drowning, a critical hit, a projectile (which it is always immune to), and the stare. **READ, the exception:**
  `teleportTowards` (target more than 16 blocks away, in the stare goal) raises no escape event. **READ:** every
  teleport ends in `randomTeleport`, which fires a cancellable **`EntityTeleportEvent`** with a settable destination.
- **So:** cancel both events for the fight's Endermen, or clamp the destination inside the ring. Keep the arena dry
  and out of rain (water triggers the drowning escape). **And note C6:** these are Void, and their deaths grow
  Phages (§2 f).

### i. Damageable only while the slow close runs

*The state is "slow close running" (§2 a), not "channelling".*

- **`setInvulnerable(true)`. READ:** `LivingEntity.hurtServer` returns at its first check: no event, no red flash,
  no knockback, no hurt sound; and creative players still hit it.
- **Cancelling `EntityDamageEvent`. READ:** `actuallyHurt` returns false when cancelled, so `hurtServer` does too.
  **All feedback is lost**, the same as invulnerable.
- **Setting the damage to 0, uncancelled. READ:** for a non-player, `actuallyHurt` proceeds: invulnerability ticks
  start, the hurt time is set, the damage event is broadcast (the red flash), knockback 0.4 and the hurt sound play.
  The early return on 0 damage is for a player target only. **INFERRED: a 0-damage hit plays every piece of
  feedback, knockback and invulnerability frames included.**
- **INFERRED, and it matters more than the three above:** our weapon and ability hits do not raise this vanilla
  event at all. They land through our own health store (`.claude/rules/standing-decisions.md`, *A TRAVELLING RANGED
  WEAPON AUTHORS KNOCKBACK*: *"every shape routes through the custom health store, so nothing calls
  `entity.damage()`"*). **So the Mage's gate must live in our damage pipeline**, keyed on the fight's slow-close
  state, and the vanilla event is only the path for vanilla hits. Which feedback a refused hit shows is a design
  choice for F6, not settled here.

---

## 5. WHAT WE ALREADY HAVE

**The old loot ids against our content today** (searched 2026-11-18 at `25d5e870`: each id as a file name and as an
`id:` line under `paper/src/main/resources/content/`, then a wider case-insensitive search for each name's words;
**the positive control is `dragons_plume`, which both searches found**):

| old id | in our content? |
|---|---|
| `DRAGONS_PLUME` | **YES**: `weapons/dragons_plume.yml` |
| `WITHERED_FALCHION`, `WHISPER_OF_THE_VOID`, `KNELL_BEAM`, `VOIDBLOOM_CHARM` | no |
| `WITHERED_KNIGHT`, `DRAGON_SCALE`, `GOLDROT`, `VOID_BEARER` armour sets | no (our `armor/` holds only the six vanilla materials) |
| `WITHERED_GAUNTLET`, `VOID_QUIVER`, `GOLDROT_CHALICE`, `VOIDREN_HEART` | no (our `accessories/` holds `brawlers_gauntlet`, `fletchers_quiver`, and four others; none of these) |

**So 1 of the 25 old ids exists, the legendary; none of the epic or rare pools does.** The wider search's other hits
are names that share a word (`withered_shortbow`, `withering`, `withered_mount`), not these items.

**Drops and loot tables today** (searched for `loot`, `getDrops()`, `setDrops`, `LootTable` and `dropItem` across
`core/`, `storage/` and `paper/src/main`, and for `drops` / `loot` keys in content): **our game has no loot table
and no drop system.** A mob dies with its vanilla drops, except a Phalanx's shot, whose death clears them
(`RpgListeners`, `onShotDeath`); a player's death drops nothing (`RpgListeners`, the death handler). The one thing
that pays items out is mining: an ore's `payout` block (`ores/iron.yml`) pays a plain vanilla item per spot plus a
completion bonus, overflow dropping at the player's feet (`MiningService`, THE PAYOUT). Our custom items reach a
player through `/rpg give` and recipes; the mint paths say they are built for "a loot drop" later
(`EnchantRollItems`, `GearScoreItems`, their class comments). **The cascading loot system Ben named is a separate
design and is not designed here.**

---

## 6. PROPOSED BUILD ORDER

**RULED 2026-11-18** (sixth message: "Order is fine"). *(Was: proposal only; Ben rules the order.)* Small steps, each bootable on its own, auto rows preferred, fewest boots.
**Steps F1 to F3 need no server at all**, and **F4 to F7 need no arena and no dragon**: they run in an ordinary
world around a fixed point.

| step | what | needs | boot |
|---|---|---|---|
| **F1** | The ring as one pure shape in `core/` (§2, the mechanism ruling): fast close, slow close, kill expansion, closed-for-good, the return. Unit tests pin the fastest-point cap, the nesting bound `d ≤ 36`, and the return as the same path. | nothing | none |
| **F2** | The strike ramp in `core/`: a per-player count of consecutive strikes, reset inside (§2 c). Unit tests. | the ramp's numbers, or placeholders marked as such | none |
| **F3** | One Mage cycle as a pure state machine in `core/`: landed → fast close (undamageable) → slow close (damageable; crystals on their schedule) → his death (return) or all crystals (fail). Unit tests. | crystal count and pace, or placeholders | none |
| **F4** | The ring drawn and struck in an ordinary world about a fixed point, driven by a dev command. Auto rows: the server's radius each tick against F1's. | §4 f picks the display | 1 (with F5) |
| **F5** | Lightning as effect only, our damage through our pipeline, the ramp. Auto rows on the HURT line. | §4 g | (with F4) |
| **F6** | The Dark Mage as a content mob with the channel: lands, glows, stays put, undamageable until the slow close. Auto rows. | §4 i; Mage health | 1 (with F7) |
| **F7** | The crystals: spawned one at a time, beams aimed at the Mage, unbreakable, all powered → the fight fails. | §4 e | (with F6) |
| **F8** | The temporary world per group and the arena from a template file. | §4 a, b; entry and group size | 1 |
| **F9** | The dragon in that world: increased health, the fireball removed, immune while the Mage lives. | §4 c, d; dragon health | (with F8) |
| **F10** | Endermen as fodder; the new waves. | §4 h; Ben's wave tables | 1 |
| **F11** | One guaranteed item per player. | the cascading loot system, designed first | later |
| **F12** | Dialogue. | Ben's lines | with any boot |

**Fewest boots: four** (F4+F5, F6+F7, F8+F9, F10), with F1 to F3 at zero.

### 6.1 F1 TO F3 AS BUILT (`feat/fight-core`, 2026-11-18): WHAT THE CODE SHOWED

*The classes are in `core/.../core/fight/`: `FightSettings`, `Ring`, `Landing`, `FastClosePath`, `SlowClose`,
`ReturnPath`, `StrikeRamp`, `MageCycle`. Every finding below is a seat choice or a gap in the brief, recorded here so
the first boot can rule on it. None stops F4.*

- **WITH THE SEAT PLACEHOLDERS, THE RING ALWAYS CLOSES BEFORE THE CRYSTALS FINISH, UNLESS THE PLAYERS KILL.** The slow
  close lasts 12 / 0.4 = 30 s = **600 ticks**; ten crystals at 80 ticks take **800**. Each counted kill adds 1 block,
  which is 50 ticks of slow close, so **at least 4 net kills inside the ring** are needed before POWERED can happen at
  all. With no kills, every Mage cycle ends CLOSED at 30 s. A property of the placeholders, not of the code; Ben sets
  them at the first boot (`MageCycleTest`, *withNoKillsTheRingClosesBeforeTheCrystalsFinish*, and the six-kill staging
  in *theLastCrystalPowersTheMageOnTheExactTick*).
- **"No cap" and "finite for any k" meet at k ≈ 1020.** 20 × 2^(k−1) passes the largest finite double there, so
  `StrikeRamp.damageForStrike` returns `Double.MAX_VALUE` from that k on. Any finite value that large kills, so this is
  not a gameplay cap. **Owed to F5:** the damage pipeline must take a `Double.MAX_VALUE` hit without overflowing (a
  multiplier above 1 on it gives infinity). A dev-only edge: a player struck ten times in a row has already taken
  20 × 2^9 = 10240 on his tenth.
- **A closed ring contains no point, its centre included.** "Inside" is inclusive at the edge, and a radius-0 ring
  would otherwise contain exactly its centre. `Ring.contains` refuses every point at radius 0 (§2 reading b).
- **"One every 80 ticks from the start of the slow close" is read as the first at +80**, the tenth at +800. The other
  reading, the first at +0, would power him at +720. Seat choice.
- **Ties on one tick go against the players.** The machine places every transition on its exact tick before it acts
  on that tick's input, so a kill on the tick the ring closes is too late, and a Mage death on the tick the last
  crystal lights is too late (the fight fails POWERED). If the ring closes and the last crystal lights on the same
  tick, the outcome is CLOSED. Seat choices.
- **A held Mage counts as alive.** From a landing HELD during the return, the dragon's immunity flag (`mageAlive`) is
  true again, though his fast close has not begun. A held Mage's death cancels the held landing. Seat choices; the
  brief did not say.
- **A kill's expansion is a snap, not a glide.** One counted kill moves every point of the ring 1 block in one tick,
  20 blocks per second, above `v_fast`. The brief caps only the fast close and the return. F4 draws it; Ben may want
  it eased.
- **Two checks the settings record adds:** `mageRingRadius < borderRadius`, and **`v_slow ≤ v_fast`** (otherwise the
  return's slow part would outrun the cap the brief requires of the return).
- **Fractional durations land on whole ticks.** The slow close starts on the first whole tick at which the fast
  close's progress has reached 1; the return's slow part ends on the first whole tick at which the radius is back at
  12. The return's fast part then replays the fast close's own tick positions in reverse, so the "visits the close's
  positions in reverse" property holds exactly. With the placeholders and d = 0, 18, 36 every duration is already
  whole (144, 216, 288 ticks).
- **The strike clock is the fight's tick**, a strike on every multiple of 40 from the fight's tick 0, not restarted per
  Mage cycle. The brief did not say which clock.
- **`mageDamageable` is advice to the wiring.** The machine cannot stop a hit; F6's damage gate (in our own pipeline,
  §4 i) must read it. A Mage death fed in during the fast close (an admin kill) is handled: the ring returns from where
  it stands.

### 6.2 F4 + F5 AS BUILT (`feat/fight-ring`, 2026-11-18): WHAT THE CODE SHOWED

*The fight file is `content/fights/dragon.yml`, the fixture `/rpg fight start | land <dx> <dz> | mage-die | stop`
(`paper/.../fight/`), its logic core's `RingFixture`. The gate is `docs/gates/GATE-fight-ring.md`, NOT RUN at this
writing.*

- **A `Double.MAX_VALUE` strike is finite on the path the fixture takes, and becomes INFINITY on the defence path.**
  MEASURED, not reasoned: `MaxValueStrikeTest` passes with `DefenseRule.BYPASSED`, and the same strike with
  `DefenseRule.APPLIES` (armour 40) reads `dealt Infinity`. `Defense.applyDefense` computes `damage × 100 / (100 +
  defence)`, and `MAX_VALUE × 100` overflows before the division. **So if Ben rules that armour reduces the lightning
  (GATE-fight-ring.md BQ5), the ramp's saturation must come down to `MAX_VALUE / 100`, or the curve must divide first
  (`damage × (100 / (100 + d))`; that reorders a floating-point product every hit in the game reaches, so it is not done
  here).** A dev-only edge: a strike count of about 1014 in a row.
- **The forced ring costs 906 packets per redraw at radius 48, 4530 a second per player** (5 redraws a second; one
  `ClientboundLevelParticlesPacket` per point, READ in `CraftPlayer.spawnParticle`). cfde822's border sent 220 a redraw.
  Spacing, heights and period are content.
- **No PacketEvents for the forced send** (READ): `CraftPlayer.spawnParticle(..., force)` sets the packet's
  `overrideLimiter` to force and `alwaysShow` to false, and sends to that player with no distance check.
- **Folia, owed by the real fight:** the dev fixture reads participants' positions from C's region thread, safe on
  Paper only. The fight in its temporary world must read each player on his own scheduler.
- **Kill credit is `getKiller()`**: `MobDeathSystem` sets it for a player's store damage, vanilla for a vanilla kill. **A
  summon's kill is not credited** (cfde822 credited the summon's owner, §3 fact 21). Which kills count is still §7
  item 7.
- **`start` takes C from the command's source**, not the player's feet, so a gate row can put C 60 blocks away with
  `execute positioned` instead of teleporting the player into unknown ground. A seat choice.
- **Lines the brief did not ask for, added so rows read exact ticks:** `FIGHTPHASE` (each change, on its own tick, a
  landing's and a death's included), `FIGHTKILL` (radius before and after), and RINGTICK's `points=` and `viewers=`.
- **Every selftest run's end stops a fixture a row left running** (one `SELFTEST RUN fight-fixture …` line), so an
  aborted row cannot leave lightning striking the player; plugin disable stops it too.

### 6.3 F6 + F7 AS BUILT (`feat/fight-mage`, 2026-11-18): WHAT THE CODE SHOWED

*The Mage is `content/mobs/dark_mage.yml`, his gate core's `DamageGate`, the crystals the fixture's port. The gate is
`docs/gates/GATE-fight-mage.md`, NOT RUN at this writing.*

- **The gate has ONE custom choke point and ONE vanilla one, and both are needed.**
  - The custom one: every route to a mob's custom health ends in `BukkitCombatant.Handle.applyDamage` (a read-only
    survey of every caller), and the gate is asked there before the store.
  - Why the vanilla one too: `/kill`, the void and **mob-on-mob melee never reach the store** (`VanillaDamagePolicy`
    passes `KILL` and `VOID`; no handler reroutes a mob's melee on a mob). So `RpgListeners.onGatedDamage` cancels any
    vanilla damage on him at LOWEST.
  - **While he IS damageable, a mob's melee on him lands on vanilla health, not the store.** That is UNVERIFIED in game
    and a hazard for when the fight has mobs near him.
- **Knockback cannot be stopped by the attribute alone.** Our own pushes (`applyKnockback`, `applyImpulse`, pulls) set
  a velocity and ignore knockback resistance. So the fixture **pins** the Mage's x and z each tick. The real fight
  needs the same pin, or an "anchored" check at every push site.
- **"Stationary" removes every goal** (Paper's `MobGoals.removeAllGoals`, read) and keeps AI on, so gravity acts.
  `NoAI` was not used: it would have to be READ whether it stops gravity, and goals-off needs no such question.
- **Non-persistence, READ:** `CraftEntity.setPersistent` writes `Entity.persist`, and `Entity.saveAsPassenger` writes
  nothing when it is false (its force flag aside). The Mage and every crystal are not persisted.
- **A closed ring puts its crystals out.** `MageCycle`'s CLOSED snapshot reports 0 crystals, and the fixture follows
  the snapshot. POWERED keeps them. **A seat choice; the brief named only POWERED.**
- **LINGER cannot see a crystal**, which is not a projectile, a cloud or a marker. The gate's witness is a
  PROBE_ENTITIES of `end_crystal` within 48 at each row's end; a spot is 41.8 from C.
- **The selftest's ATTACK reaches only 16 blocks**, and every crystal is further. MG7 hits one with vanilla's
  `/damage … player_attack by @s` instead.
- **POWERED needs fifteen spaced kills at Ben's numbers.** The slow close lasts 400 ticks against 800 for ten crystals,
  so the ring must gain 12 blocks or more. MG8 stages it and shows the arithmetic.
- **The play row cannot promise "cannot die" by healing alone.** A sixth consecutive strike (640 before armour)
  outruns any heal. So the fixture's play mode stops after a third consecutive strike: 80 at most, each on a bar
  healed within 10 ticks.
- **The wall at Ben's four rows costs 6040 packets a second per player** at radius 48, under the 6500 line.
- **The armour path's overflow is guarded in the ramp** (`StrikeRamp.CEILING_DAMAGE` = `MAX_VALUE / 200`, executed
  finite at defences 0 to `MAX_VALUE`). `Defense.applyDefense` is not reordered.

---

## 7. OPEN, FOR BEN

*Listed, not answered.*

1. **Crystals as an immunity gate** on the dragon, separate from the Mage (Ben: "Not sure yet").
2. **Crystal count and pace** during the slow close.
3. **ANSWERED 2026-11-18** (eighth message): "Let's lower it to 3.8 blocks a second to allow players to react and
   catch up when struct". *(Asked as:)* **Fast-close speed** `v_fast` in blocks per second ("Slightly slower than a
   player sprinting").
4. **ANSWERED 2026-11-18** (seventh, answer 3): "I think it should be a little faster. Maybe .6 blocks per second".
   *(Asked as:)* **Slow-close speed** `v_slow`.
5. **ANSWERED 2026-11-18** (sixth message): "Lightning ramping damage numbers can start at 20, 40, 80 and keeps
   doubling". *(Asked as:)* **The lightning ramp's numbers**: the first strike's damage, the step, any cap.
6. **The Mage's health: ANSWERED "for now" 2026-11-18** (eighth message): "1500 works for now". **Still open: where
   he may land** (the seat's nesting bound, §2: no more than 36 blocks from the
   centre, or the ring sweeps outward over a standing player).
7. **Expansion per kill** `e_kill`. *(Seat-added: which deaths count, any mob or the fight's own, and a summon's kill
   for its owner, as cfde822 did, §3 fact 21.)*
8. **The wave tables** (Ben: "we will rework it entirely").
9. **Endermen growing Phages** (§2 f): intended or not.
10. **Entry, group size, and death mid-fight** (Ben: "not sure yet").
11. **The dragon's health** ("for now it stays vanilla with increased health": how much).
12. **Dialogue** ("Likely keep it": as is, or rewritten).
13. *(Seat-added.)* **The return's slow part:** does it run back at `v_slow`, or faster? *(A seat placeholder runs it
    at `v_slow` until Ben sets it.)*
14. **ANSWERED 2026-11-18** (sixth message): "I would like to keep particles. We can try forced particles if that
    helps. I don't care about the minimal particle setting." *(Asked as:)* *(Seat-added, from §4.)* **How the ring
    is drawn:** display entities (round, independent of particle settings),
    particles through PacketEvents with `alwaysShow`, or a world border (square). §4 f.
15. *(Seat-added, from §4, a FLAG, not a choice.)* **The dragon orbits (0, 0) of its world**, so the arena's
    template is centred there, which C1 otherwise forbids as fight geometry. One world per group keeps it safe; Ben
    to note it.
16. **Every fight question still open in `PLAN-dragon-mobs.md`**, QUESTIONS FOR BEN: **Q1** (fight-only mobs or
    world mobs too), **Q2** (the Knell's hit strength), **Q3** (mob numbers × gear score / 100), **Q9** (the
    fight's gear score), **Q11** (do the fight's mobs drop or pay), **Q12** (a restart mid-fight), **Q20** (a Void
    death during a Phage's fuse), **Q27** (split children against the fight's caps), **Q29** (growing in too small
    a space), **Q30** (a newborn's moment of no growth), **Q31** (is the blast Void damage), **Q32** (does a shield
    block the blast), **Q34** (does cover protect from the blast).
    **Q10 (the mobs against Endermen) is ANSWERED by the first message, answer 6:** "drop the war, the enderman are
    now the main fodder enemy".
