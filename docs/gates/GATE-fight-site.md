# GATE — STEP F8a: ONE RUNTIME AT A SITE, BEN'S SECOND-BOOT TUNING, AND THE INSTRUMENT FIXES

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/fight-site`. This file is
committed before the boot. Readings go **beside** a prediction, never over it, and a prediction is not edited once its
row has been read; a correction is a dated note.

> **SUPERSEDED FOR THE BOOT, 2026-11-18 (the seat's ruling on the split):** this step has **no boot of its own.** Its
> rows (`/rpg selftest fight-site`) run at the **one boot of `feat/fight-arena`'s head**, and **R0 and the run sheet
> live in `docs/gates/GATE-fight-arena.md`**, not here. Read these rows only once that file's R0 has passed.

**What it gates.** PLAN-dragon-fight.md §6.4, step F8a (the F8 + F9 brief's §0 to §3), in an ordinary world through
the dev fixture `/rpg fight …`:
- Ben's second-boot tuning:
  - the Mage at 6,000;
  - a crystal every 240 ticks (12 s);
  - the wall at six rows 1.5 apart, every other row turning the other way.
- The instrument fixes carried from `GATE-fight-mage.md`'s readings.
- **One runtime at a SITE:** `FightInstance` takes a `FightSite`, and `OrdinaryWorldSite` is the only site in this step.

**What it supersedes.** It RETIRES `selftest/GATE-fight-mage.yml`. **Every row there MOVES**, so every one is
re-predicted here under its old name with the next suffix:

| old | new | why it moved |
|---|---|---|
| FR1R2–FR5R2, FR7R2, FR8R2 | FR1R3–FR5R3, FR7R3, FR8R3 | every RINGTICK is now **1812** points at r = 48, not 1208 (six rows) |
| FR6R2, FR9R2 | FR6R3, FR9R3 | the points, and their **crystal** lines at 240 a crystal |
| MG1–MG4 | MG1R2–MG4R2 | the points; the Mage at **6000**; MG2 holds a melee weapon (the fix); the mobdamage reply reads the store |
| MG5–MG8 | MG5R2–MG8R2 | the points; **240** a crystal (MG6 and MG8 now need a kill train to keep the ring open); MG7 reaches the listener with a void-type hit |
| MQ1 | MQ1R2 | its ring never moved (`REFUSED_NO_GROUND`); this time the landing has a floor |

**MQ2–MQ7 are RETIRED, not re-predicted:** Ben answered them at the second boot.

**Rows that did NOT move:** none. The six-row wall changes every RINGTICK line, and every row reads RINGTICK.

**New rows:**
- **WS1:** the wall turning, read from its points.
- **IP1:** the entity probe leaves a dying body out.
- **IA1:** AWAIT_FIGHT's two endings.

## GAME MODE, DIFFICULTY

**SURVIVAL on every row:** `/rpg selftest` refuses outside SURVIVAL, and the fixture's participants are survival or
adventure only. The damage rows (FR8R3, MQ1R2) are SURVIVAL by that rule. The difficulty is set by the setup's
`difficulty: normal`.

## THE NUMBERS THE ROWS RUN ON

| number | value | whose |
|---|---|---|
| border, Mage ring | 48, 12 | Ben's |
| `v_fast`, `v_slow` | 3.8, 0.6 | Ben's ("leave the fast close as 3.8") |
| crystals | 10, **one every 240 ticks** from the slow close | Ben's pace ("no make it 12 seconds each crystal"); the count is the brief's reading |
| the Mage | `wither_skeleton`, **6000** at GS 100, void, glowing, stationary | Ben's ("Change the Mage health to 6,000") |
| the wall | rows at **0.2 / 1.7 / 3.2 / 4.7 / 6.2 / 7.7**; shift 0.25; turn 1.0/s; **rows 1, 3, 5 turn the other way** | Ben's ("yes, maybe another row or 2"; "maybe alternate twists on every other row"); shift and turn the seat's |

**EXECUTED, never by hand:**
- The fast close at 3.8 lands on whole ticks 190, 222, 285, 379 for d = 0, 6, 18, 36 (`BensNumbersTest`).
- The slow close shuts 400 ticks after it starts.
- The wall at r = 48 is **1812 points** a redraw: 302 a row × 6 (`RingPointsTest`).
- Alternate rows turn ±1.0 block in 20 ticks (`RingPointsTest`, `RingFixtureTest`).

**THE PARTICLE COST, as the brief asked:** 1812 points a redraw × 5 redraws a second = **9060 packets a second per
player** at r = 48. That is **under the brief's 9,500 line**, by 440. One packet per point, READ
(`CraftPlayer.spawnParticle`).

**THE CRYSTAL ARITHMETIC THAT SHAPES THE ROWS:** the slow close lasts 400 ticks and the first crystal lights at 240.
So **without kills only ONE crystal can light**, and the ring closes at S+400. MG6R2 and MG8R2 hold it open with a
kill train: one kill every 35 ticks from S+35.
- The ring loses 1.05 blocks between kills and a counted kill gives back 1.0.
- So the radius just before kill n is **11 − 0.05n**: never above 11, so no kill is lost to the cap at 12.
- After 68 kills, at S+2400 it is **12 − 72 + 68 = 8.0**.

Computed (`perl`): kill 1 finds 10.95, kill 7 10.65, kill 14 10.30, kill 68 7.60.

## THE CLOCK

The fight clock is 0 at `start`, 1 on the first loop tick. **L (a landing's tick) and D (a death's tick) are read from
the log**, each with its window. Every transition is predicted relative to them; S = L + the fast close's length.

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC

His custom maximum is at least 100 (Ben's read 350). The largest single strike any row allows is **80 before armour**.
- **FR8R3:** healed before each of its strikes: 20, 40 and 80, each on a full bar.
- **MQ1R2:** healed every 10 ticks for the 120 ticks after the landing, the only window he can be outside. At most three
  strikes fall in it (k ≤ 3).
- **Every other row** keeps him inside every ring it draws:
  - **MG6R2 and MG8R2:** 2 west of M = C, while the ring stays above 7.6.
  - **The Mage rows:** 2 to 6 from M while the ring is at least 6.
  - Or he is stopped before the ring can shut on him.
- No row strikes outside after a POWERED, because a POWERED fight strikes no more.

## THE INSTRUMENT FIXES THIS FILE RELIES ON (READ from the pinned jar, build 74)

- **PROBE_ENTITIES leaves a dying body out.**
  - READ: `LivingEntity.tickDeath` removes a dead mob only at `deathTime` 20; `CraftEntity.isDead` is `!isAlive`, and
    `LivingEntity.isAlive` is `!isRemoved() && getHealth() > 0`.
  - The line now reads `ENTITIES <TYPE> count=<live> radius=<r> dying=<n>`.
- **ATTACK says when it cannot deliver a damage event.**
  - READ: `Player.attack` hurts only if `ATTACK_DAMAGE × baseDamageScaleFactor` or the enchanted bonus is above 0.
  - READ: `WeaponItems` pins a non-melee weapon's attack damage to 0 (the melee suppressor).
  - The DRIVE line now carries `attack_damage=`, and a zero adds `DRIVE attack NO_DAMAGE_EVENT …`. MG2 held Ben's staff
    (INFERRED from the run's `cell=mage/fire`), so MG2R2 holds `emberblade`.
- **The crystal listener is reached only past `setInvulnerable`.**
  - READ: `EndCrystal.hurtServer` returns on `isInvulnerableToBase` before the Bukkit event.
  - READ: `Entity.isInvulnerableToBase` lets an invulnerable entity be hurt only by a source in
    `BYPASSES_INVULNERABILITY` or one that `isCreativePlayer()`.
  - READ: build 74's tag holds `minecraft:out_of_world` and `minecraft:generic_kill`.
  - So MG7R2 reaches the listener with `out_of_world`. `/kill` reaches neither: READ, `EndCrystal.kill` → `Entity.kill`
    → `remove`, no damage event.
- **`/rpg mobdamage` replies from the store, 2 ticks after the hit.**
  - A refused hit reads the health the store kept.
  - A killed mob reads `0/<max>`: INFERRED, since the store forgets a mob at its removal (`MobNameplateManager.onMobRemove`),
    20 ticks after death (READ, `tickDeath`).
- **AWAIT_FIGHT `<ceiling>`** logs `AWAIT fight ended ticks=<n>` or `AWAIT fight ceiling ticks=<c> still running`.
- **FIGHTPROBE carries the WALL WITNESS:** `wall=<a0>,…,<a5>`, each row's first point as an arc from angle 0 along the
  ring, blocks, in [0, 301.593) at r = 48 (`RingFixture.wallStartArcs`, the points `tick` draws).

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST fight-site|\[Rpg\] (RINGTICK|FIGHT|STRIKE|MAGEGATE|CRYSTAL|AWAIT)'`,
read after Ben's LOGIN, inside the row's own `SELFTEST fight-site <row> START … DONE` bracket. `\[Rpg\] FIGHT` also
matches FIGHTPHASE, FIGHTKILL, FIGHTPROBE and FIGHTCRYSTAL. **A log-line row passes when the predicted line appears
VERBATIM** (a `<…>` is the value the row says it may take).

**In EVERY row (not repeated):**
- **The clean:** after `CLEAN …`, `LEFTOVER living=0 radius=16` and `LINGER work=0 entities=0 radius=48`.
- **The end:** the row's last two probes read **`ENTITIES END_CRYSTAL count=0 radius=48 dying=0`** and
  **`ENTITIES WITHER_SKELETON count=0 radius=16 dying=<n>`** (n = 0 unless a wither skeleton died in the last 20
  ticks, as the row says). EVERY row carries both, IP1's crystal probe included; the wording gap that
  `GATE-fight-mage.md`'s readings found (MQ rows without them) is closed.
- **Start and stop:** one `[Rpg] FIGHT start world=<w> centre=<x y z> by=BaronVonYeetus site=ordinary` per start; one
  `FIGHT stop`-ended pair per stop: `FIGHT ended reason=command`.
- **RINGTICK:** every one reads `viewers=1` and `points=` **6 × ceil(2π r)** for its `r` (**1812 at r=48.000**).
- **STRIKE:** every one is `k=0 damage=0.000 inside`, unless the row says otherwise; no HURT unless it says so.

A non-zero LEFTOVER or LINGER, or a live crystal or skeleton left, makes its row NOT AS PREDICTED.

### FR1R3 — start: the border at C, IDLE, six rows (SURVIVAL) — supersedes FR1R2

| prediction | stands on | READING |
|---|---|---|
| (1) `RINGTICK t=4 phase=IDLE centre=0.000 0.000 r=48.000 crystals=0 points=1812 viewers=1` and the same at `t=8`. (2) No FIGHTPHASE. (3) `FIGHT stop t=<8 to 11>` | `RingPointsTest` (1812) | |

### FR2R3 — land 0 0: 190 ticks to 12; SLOW_CLOSE on that tick; 0.6 after (SURVIVAL) — supersedes FR2R2

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=L dx=0.000 dz=0.000 result=STARTED`, L 18 to 23; `FIGHTPHASE t=L IDLE->FAST_CLOSE`; `FIGHT mage spawned t=L … at=0.00 0.00 0.00`. (2) **`FIGHTPHASE t=L+190 FAST_CLOSE->SLOW_CLOSE`**. (3) FAST_CLOSE RINGTICKs: `r` = 48 − 0.19 × (t − L), centre 0.000 0.000. (4) SLOW_CLOSE RINGTICKs: `r` = 12 − 0.03 × (t − L − 190), `crystals=0`. (5) Every STRIKE inside | `BensNumbersTest`, `MageCycleTest` | |

### FR3R3 — land 18 0: 285 ticks, the centre on the straight line (SURVIVAL) — supersedes FR3R2

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=L dx=18.000 dz=0.000 result=STARTED`, L 18 to 23; spawned `at=18.00 0.00 0.00`. (2) **`FIGHTPHASE t=L+285 FAST_CLOSE->SLOW_CLOSE`**. (3) Every FAST_CLOSE RINGTICK: dx = (48 − r) / 2 ± 0.0015, dz 0.000. (4) Every SLOW_CLOSE RINGTICK `centre=18.000 0.000`. (5) Every STRIKE inside | `BensNumbersTest` | |

### FR4R3 — land 36 0: 379 ticks (SURVIVAL) — supersedes FR4R2

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=L dx=36.000 dz=0.000 result=STARTED`, L 18 to 23; spawned `at=36.00 0.00 0.00`. (2) **`FIGHTPHASE t=L+379 FAST_CLOSE->SLOW_CLOSE`**. (3) Every FAST_CLOSE RINGTICK: dx = 48 − r ± 0.0015, dz 0.000. (4) Every SLOW_CLOSE RINGTICK `centre=36.000 0.000`. (5) Every STRIKE inside | `BensNumbersTest` | |

### FR5R3 — land 36.01 0: REFUSED_TOO_FAR, with ground there (SURVIVAL) — supersedes FR5R2

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=<5 to 8> dx=36.010 dz=0.000 result=REFUSED_TOO_FAR`; REPLY `"Mage lands at C + (36.01, 0.00): REFUSED_TOO_FAR"`. (2) No FIGHTPHASE, no `FIGHT mage spawned`. (3) Every RINGTICK `phase=IDLE centre=0.000 0.000 r=48.000 crystals=0 points=1812 viewers=1` | `FightRingTest` | |

### FR6R3 — no kills: CLOSED at S+400, ONE crystal lit and out (SURVIVAL) — supersedes FR6R2

| prediction | stands on | READING |
|---|---|---|
| (1) L 38 to 43; `FIGHTPHASE t=L+190 FAST_CLOSE->SLOW_CLOSE`. (2) **`CRYSTAL t=L+430 lit index=1`** (S+240), and no other `lit`. (3) **`FIGHTPHASE t=L+590 SLOW_CLOSE->CLOSED`**, **`CRYSTAL t=L+590 removed index=1`**, `FIGHT FAILED_CLOSED t=L+590` once; the chat `"The fight has failed: FAILED_CLOSED (fixture: nobody is killed)."`. (4) `STRIKE t=600 … k=0 damage=0.000 inside`; no STRIKE outside; `FIGHT stop` before 640. (5) No HURT | `MageCycleTest`, `BensNumbersTest` (400) | |

### FR7R3 — kills in the slow close: +1.0 from inside, capped at 12; none from outside (SURVIVAL) — supersedes FR7R2

| prediction | stands on | READING |
|---|---|---|
| (1) L 18 to 23; slow close from L+190. (2) Four `FIGHTKILL … killer=<Ben's uuid>`: kill 1 `counted=true`, `r_before` 9.5 to 10.1, `r_after` = `r_before` + 1.000; kill 2 `counted=true`, `r_before` 10.3 to 11.0, `r_after` = `r_before` + 1.000; kill 3 `counted=true`, `r_before` above 11.000, `r_after=12.000`; kill 4 (from 16 west of M) `counted=false`, `r_after` = `r_before`. (3) **No CRYSTAL line** (the first is due at S+240, after the stop). (4) Every STRIKE inside. (5) End `WITHER_SKELETON count=0 radius=16 dying=<0 or 1>` (the fourth Knell dies about 5 ticks before) | `MageCycleTest`, `RingFixtureTest` | |

### FR8R3 — the strikes through armour: 20, 40, 80; inside resets; 20 again (SURVIVAL) — supersedes FR8R2

| prediction | stands on | READING |
|---|---|---|
| C 60 east; no Mage. (1) `STRIKE t=40 … k=1 damage=20.000 outside`, then `HURT before=20.000 after=<20 × 100 / (100 + D)> defense=<D> dealer=self dealerType=self` (D = 10.000 gives 18.182). (2) `t=80 … k=2 damage=40.000 outside`; `HURT before=40.000 after=<40 × 100 / (100 + D)>`. (3) `t=120 … k=3 damage=80.000 outside`; `HURT before=80.000 after=<80 × 100 / (100 + D)>`. (4) `t=160 … k=0 damage=0.000 inside`, no HURT. (5) `t=200 … k=1 damage=20.000 outside`; `HURT before=20.000 …`. (6) One HURT per outside strike; no ABORTED | `MaxValueStrikeTest`, `FightPortSourceTest` | |

### FR9R3 — mage-die after the first crystal: it goes out, the ring home, IDLE (SURVIVAL) — supersedes FR9R2

| prediction | stands on | READING |
|---|---|---|
| (1) L 18 to 23; S = L + 190; **`CRYSTAL t=S+240 lit index=1`**. (2) **`FIGHT die t=D phase=RETURNING slow_close_started=t=S ticks_in_slow_close=<D − S>`**, D about S + 251; `FIGHTPHASE t=D SLOW_CLOSE->RETURNING` and **`CRYSTAL t=D removed index=1`** on that tick. (3) **`FIGHTPHASE t=H RETURNING->IDLE` with H − D = (D − S) + 190 exactly**. (4) From H, `phase=IDLE … r=48.000`. (5) No FIGHTKILL. (6) End `WITHER_SKELETON count=0 radius=16 dying=0` (the Mage died about 470 ticks before) | `RingFixtureTest` | |

### WS1 — the six-row wall, read from its own points: alternate rows turn opposite ways (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Two FIGHTPROBEs, at fight ticks t1 (about 20) and t2 (about 40), each `… mage=none crystals=0 beamsOnMage=0 wall=<a0>,<a1>,<a2>,<a3>,<a4>,<a5>`. **Each a_j = (0.25 j + s_j × t / 20) mod 301.593, to 3 places, s_j = +1 for rows 0, 2, 4 and −1 for rows 1, 3, 5**, t that probe's own `t=`. At t = 20 that is `wall=1.000,300.843,1.500,301.343,2.000,0.250`, at t = 40 `wall=2.000,299.843,2.500,300.343,3.000,300.843` (computed). So between the two: rows 0, 2, 4 **+1.000 × (t2 − t1) / 20**, rows 1, 3, 5 **−1.000 × (t2 − t1) / 20**, mod 301.593. Every RINGTICK `points=1812` | `RingFixtureTest` *theWallWitness…*, `RingPointsTest` | |

### IP1 — the entity probe leaves a dying body out (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE spawn tag=ip1 <uuid> knell frozen`, then **`ENTITIES WITHER_SKELETON count=1 radius=16 dying=0`**. (2) After `rpg mobdamage 1000000` and 3 ticks: **`ENTITIES WITHER_SKELETON count=0 radius=16 dying=1`** (the body inside its 20 death ticks). (3) 30 ticks later: **`ENTITIES WITHER_SKELETON count=0 radius=16 dying=0`** (removed). (4) The mobdamage REPLY reads **`"WITHER_SKELETON damaged: 0/360 custom HP"`** (the store's reading; the Knell is 360 at GS 100, as `GATE-fight-mage.md` FR7R2's spawn reply read). (5) No fight runs: `rpg fight stop` replies `"No fight fixture is running."` | `SelfTestFightStepsTest`, `MobDamageReplySourceTest` | |

### IA1 — AWAIT_FIGHT: its ceiling while a fight runs, at once when none does (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) **`AWAIT fight ceiling ticks=40 still running`**. (2) `FIGHT stop t=<about 40>`, `FIGHT ended reason=command`. (3) **`AWAIT fight ended ticks=0`** | `SelfTestFightStepsTest` | |

## THE MAGE (F6), at 6000

### MG1R2 — land spawns one tagged Mage at M, glowing, 6000 at gear score 100 (SURVIVAL) — supersedes MG1

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=L dx=6.000 dz=0.000 result=STARTED`; `FIGHT mage spawned t=L id=<uuid> at=6.00 0.00 0.00`. (2) **`FIGHTPROBE t=<L+2 to L+3> mage=<the same uuid> at=6.00 0.00 0.00 glowing=true ai=true health=6000.000/6000.000 defense=<D> crystals=0 beamsOnMage=0 wall=<six arcs>`**. (3) Mid-row `ENTITIES WITHER_SKELETON count=1 radius=16 dying=0` | `MobLoaderStanceTest`, `MobScaling` | |

### MG2R2 — in FAST_CLOSE a MELEE hit (a held melee weapon) and a store hit are both refused (SURVIVAL) — supersedes MG2

| prediction | stands on | READING |
|---|---|---|
| (1) `rpg give emberblade` and the HOLD. (2) **`DRIVE attack Player#attack target=<his uuid> wither_skeleton d=<about 2> charge=<c> attack_damage=<a>` with a > 0, and NO `NO_DAMAGE_EVENT` line**. (3) **`MAGEGATE t=<t> phase=FAST_CLOSE amount=<any> source=vanilla/entity_attack refused`** (the vanilla half of the gate, UNREAD on a server until now). (4) After `rpg mobdamage 50`: **`MAGEGATE t=<t> phase=FAST_CLOSE amount=50.000 source=hit refused`**, and its REPLY **`"WITHER_SKELETON damaged: 6000/6000 custom HP"`** (the store kept it). (5) **`FIGHTPROBE … health=6000.000/6000.000 …`**. **Exactly two MAGEGATE lines** | `MageGateTest`, `FightWiringSourceTest`, `SelfTestFightStepsTest`, `MobDamageReplySourceTest` | |

### MG3R2 — in SLOW_CLOSE a store hit of 100 lands by the pipeline's figure (SURVIVAL) — supersedes MG3

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHTPHASE t=L+222 FAST_CLOSE->SLOW_CLOSE`. (2) The first FIGHTPROBE (about L+242): `health=6000.000/6000.000 defense=<D>`. (3) No MAGEGATE in the row. (4) The second FIGHTPROBE: **`health=<6000 − 100 × 100 / (100 + D)>/6000.000`** (D = 0 gives **5900.000**), and the REPLY `"WITHER_SKELETON damaged: <the same, to 0 places>/6000 custom HP"` | `MageGateTest`, `Defense` | |

### MG4R2 — a Mage HELD while the ring returns appears at once and cannot be hurt (SURVIVAL) — supersedes MG4

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT die … phase=RETURNING …`; then `FIGHT land t=<t> dx=6.000 dz=0.000 result=HELD` and **`FIGHT mage spawned t=<the same t> …`**. (2) After `rpg mobdamage 50`: **`MAGEGATE t=<t> phase=RETURNING amount=50.000 source=hit refused`**, REPLY **`"WITHER_SKELETON damaged: 6000/6000 custom HP"`**. (3) **`FIGHTPROBE … mage=<the NEW uuid> … health=6000.000/6000.000 …`**. (4) End **`WITHER_SKELETON count=0 radius=16 dying=<0 or 1>`**: the first Mage's body, about 10 ticks dead, is LEFT OUT of the count (`GATE-fight-mage.md` counted it as 1) | `MageCycleTest`, `MageGateTest`, `SelfTestFightStepsTest` | |

### MG5R2 — his death with ONE crystal lit: RETURNING, the crystal out, the ring home (SURVIVAL) — supersedes MG5

| prediction | stands on | READING |
|---|---|---|
| (1) S = L + 222; **`CRYSTAL t=S+240 lit index=1`**. (2) `rpg mobdamage 1000000` at about S+250 is allowed (no MAGEGATE); **`FIGHTPHASE t=D SLOW_CLOSE->RETURNING`**, **`CRYSTAL t=D removed index=1`**, `FIGHT die t=D …` on that tick; REPLY **`"WITHER_SKELETON damaged: 0/6000 custom HP"`**. (3) No FIGHTKILL. (4) `FIGHTPROBE … mage=none crystals=0 beamsOnMage=0 …`. (5) **`FIGHTPHASE t=H RETURNING->IDLE` with H − D = (D − S) + 222** | `RingFixtureTest`, `MageCycleTest` | |

## THE CRYSTALS (F7), one every 240 ticks

### MG6R2 — a crystal every 240 ticks, held open by a kill train; counts and beams (SURVIVAL) — supersedes MG6

| prediction | stands on | READING |
|---|---|---|
| He stands 2 west of M = C. S = L + 190. (1) Fourteen `FIGHTKILL … counted=true`, about 35 apart from about S+35, each **`r_after` = `r_before` + 1.000** with **`r_before` ≤ 11.000** (computed: 10.95 at the first, 10.30 at the fourteenth). (2) **`CRYSTAL t=S+240 lit index=1`** and **`CRYSTAL t=S+480 lit index=2`**, exactly 240 apart. (3) The FIGHTPROBE after the seventh kill (about S+250): **`crystals=1 beamsOnMage=1`**; after the fourteenth (about S+495): **`crystals=2 beamsOnMage=2`**. (4) The RINGTICK nearest each probe reads the same `crystals=`. (5) No FIGHTPHASE to CLOSED | `RingFixtureTest`, `MageCycleTest` | |

### MG7R2 — the crystal's two refusals: the flag first, the listener for what passes it (SURVIVAL) — supersedes MG7

| prediction | stands on | READING |
|---|---|---|
| (1) The first FIGHTPROBE (about S+258): `crystals=1 beamsOnMage=1`. (2) `/damage … minecraft:player_attack by @s`: REPLY **`"Target is invulnerable to the given damage type"`**, and **no FIGHTCRYSTAL line** (the flag refuses first: READ). (3) `/damage … minecraft:out_of_world`: **exactly one `FIGHTCRYSTAL damage refused cause=<c> amount=5.000`** (out_of_world bypasses the flag: READ, and the listener cancels it); the reading names c. (4) The second FIGHTPROBE: **`crystals=1 beamsOnMage=1`**. (5) No `CRYSTAL … removed` before the stop | `FightWiringSourceTest`, the jar | |

### MG8R2 — POWERED on its exact tick at 240 a crystal, held open by 68 kills (SURVIVAL) — supersedes MG8

| prediction | stands on | READING |
|---|---|---|
| He stands 2 west of M = C. S = L + 190. (1) **Sixty-eight `FIGHTKILL … counted=true`**, each `r_after` = `r_before` + 1.000 with `r_before` ≤ 11.000 (computed: kill n finds 11 − 0.05 n; the last 7.60). (2) **`CRYSTAL t=S+240n lit index=n` for n = 1 to 10**, the tenth at **S+2400**. (3) **`FIGHTPHASE t=S+2400 SLOW_CLOSE->POWERED`** and **`FIGHT FAILED_POWERED t=S+2400`**. (4) No STRIKE after S+2400. (5) The FIGHTPROBE after: **`crystals=10 beamsOnMage=10`**; the end probe after the stop: `END_CRYSTAL count=0` | `MageCycleTest`, `RingFixtureTest` | |

---

## BEN'S ROW — HIS WORDS, VERBATIM; "not sure" IS NOT STATED; ONE THING PER QUESTION

**The instrument:** the `ASK` line, question and answer on one line. **No PASS or FAIL is written for a Ben row.** Its
auto parts are read by the rules above.

### MQ1R2 — the fast close at 3.8 with the wall ACTUALLY CHASING HIM (SURVIVAL) — supersedes MQ1

| prediction | READING |
|---|---|
| Auto: (1) **`FIGHT land t=<about 100> dx=36.000 dz=0.000 result=STARTED`** (the pad: no `REFUSED_NO_GROUND`), `FIGHT mage spawned … at=36.00 0.00 0.00`, `FIGHTPHASE … IDLE->FAST_CLOSE`. (2) Every FAST_CLOSE RINGTICK until the stop: dx = 48 − r ± 0.0015, `r` below 48 and falling (the ring MOVES). (3) The stop about 120 ticks after L. (4) At most three STRIKE `outside` (k = 1, 2, 3), each with one HURT `after = before × 100 / (100 + D)`. ASK 1 (`yesno`) "With the wall chasing you, did the fast close's speed feel right?" answered | |

---

## WHAT THIS GATE DOES NOT READ

- **What the client draws:** the six rows and their turn are read from the points (WS1), never as seen; Ben judges
  the look in `GATE-fight-arena.md`.
- **A creative player's hit on a crystal** (`isCreativePlayer()` passes the flag, READ): `/rpg selftest` runs in
  SURVIVAL only, so no row stages it.
- **Two fights at once:** possible in the code (`FightSiteSourceTest`), never staged. The dev command runs one.
