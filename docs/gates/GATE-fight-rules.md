# GATE — STEP F9b: THE FIGHT'S RULES AFTER THE DRAGON BOOT

**Status: READ AT THE ONE BOOT OF 2026-11-19, on `feat/fight-end`'s head `e15cf9ee` (`GATE-fight-end.md` carries its R0, all as predicted, and its checklist).** RA1 and RA2 as predicted; **RA3, RA4 and RA5 NOT as predicted** (see *THE BOOT OF 2026-11-19* at the end). Every prediction below was written **before** any boot of `feat/fight-rules`, and this file was
committed before the boot.
- Readings go **beside** a prediction, never over it.
- A prediction is not edited once its row has been read; a correction is a dated note.
- **NO BOOT until the seat has diffed the code AND this gate (and `GATE-fight-end.md`) from origin and Ben says
  "ready" in his own words.** Then only CC boots.

> **SUPERSEDED FOR R0 AND THE RUN SHEET, 2026-11-19 (the seat's split ruling: four PRs, two boots).** This gate has
> **NO BOOT OF ITS OWN.** The one R0 (a to e) and the one run sheet for this gate AND `GATE-fight-end.md` live in
> **`docs/gates/GATE-fight-end.md`**, and that file's boot, on `feat/fight-end`'s head, reads this file's rows. Ben's
> rows for both steps (the brief's 1 to 5 and 9) are there too.

**The step** (the seat's F9b brief, 2026-11-19; plan section 1, *Twelfth*): sections 0, 1, 2, 4, 6 and 7.
- **1:** the dragon is immune while a Mage is OWED, not only while one is alive.
- **2:** the fight's crystals do not heal the dragon.
- **4:** its bite (80), wing (40) and breath (30 a second), through armour.
- **6:** a lightning EFFECT at the Mage's landing.
- **7:** `FIGHTHIT` names the dragon's phase; a fireball probe; why the Mage's death message credited the dragon (fixed:
  the dragon no longer hurts its own Mage in vanilla).

**Deaths stay as they are on master** (the respawn rules are F9d and F9e). Not in this step: waves, Endermen, loot,
dialogue, entry rules, player-count scaling, the boss bar.

**GAME MODE: SURVIVAL for every row.** Damage rows are predicted against the HURT line (`.claude/rules/verification.md`,
*PREDICT DAMAGE AGAINST THE HURT LINE*). No row uses `save_blocks` (refused in a fight world). **No row kills him.**

**Witness:** these AUTO rows are read by CC from the server's log; they witness only what the selftest drives. An AUTO
reading is a reading of the server, never of the client.

---

## WHICH MERGED ROWS THIS BRANCH MOVES, AND WHICH THE BOOT RE-RUNS

**NONE IS RE-RUN.** The boot runs `fight-rules` and `fight-end` only. Each moved row of `GATE-fight-dragon.md` is
re-predicted below under its old name with the next suffix, as the exact change to its merged prediction; its merged
reading stands for everything else. **These six are NOT RUN at this boot and carry no scenario.**

### DA2R2 — DA2, re-predicted (NOT RE-RUN)

DA2's prediction, with its one FIGHTHIT on the dragon now ending **` phase=HOVER`**: `FIGHTHIT t=<t> target=dragon
amount=1234.500 before_defense=1234.500 health=18765.500/20000.000 dealer=<his uuid> element=null phase=HOVER`.

### DA3R2 — DA3, re-predicted (NOT RE-RUN)

DA3's prediction with four changes:
- ` phase=HOVER` on its two FIGHTHITs on the dragon;
- its refused hit now reads **`DRAGONGATE t=<t> phase=FAST_CLOSE amount=100.000 source=hit refused why=mage-alive`**;
- after `FIGHT mage spawned`, **`FIGHT lightning t=<D> at=<dx> 0.00 <dz> effect=true`**;
- **the order the boot read**: `FIGHT land`, then `FIGHTPHASE IDLE->FAST_CLOSE`, then `FIGHT mage spawned` (DA3 had listed
  the spawn first).

Nothing else moves: its last hit lands with nothing owed (the Mage landed and died).

### DA5R2 — DA5, re-predicted (NOT RE-RUN)

DA5's prediction with **both crossings `pending=2`** (the boot read it: the code logs each crossing after the queue has
taken the whole hit), ` phase=HOVER` on its FIGHTHIT, and a `FIGHT lightning` line after each of its two spawns. The
owed-Mage rule does not move it: it never hits the dragon while a Mage is owed.

### DA7R2 — DA7, re-predicted (NOT RE-RUN)

DA7's prediction with its kill's FIGHTHIT ending ` phase=HOVER`.

### DA8R2 — DA8, re-predicted (NOT RE-RUN)

DA8's prediction plus **`FIGHT lightning t=<L> at=0.00 0.00 0.00 effect=true`** after its `FIGHT mage spawned`.

### DA9R2 — DA9, re-predicted (NOT RE-RUN)

DA9's prediction with each of its FIGHTHIT lines on the dragon (the head's, the wing's and the burn's) ending
` phase=HOVER`.

**The rest, not re-predicted row by row:**
- **DB1, DB2** (Ben's): the dragon's hits now land (80, 40, 30 a second) with `DRAGONATTACK` and `HURT … attack=<a>`
  lines, and `FIGHT fireballs`, `DRAGONHEAL`, `DRAGONMAGE` lines may appear. Their successors are
  `GATE-fight-end.md`'s Ben rows.
- **Every row of `GATE-fight-site.md` and `GATE-fight-arena.md` that lands a Mage** gains a `FIGHT lightning …
  effect=true` line after each `FIGHT mage spawned` (both sites). No merged prediction names that line's absence, and
  no predicted line changes.
- **`GATE-fight-arena.md` and `GATE-fight-dragon.md` R0c**: the Loaded fight line gains `, landing lightning on` and
  `, bite 80.0, wing 40.0, breath 30.0/s`. Superseded by `GATE-fight-end.md`'s R0c.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST fight-rules|\[Rpg\] (FIGHT|DRAGON|BOSSBAR|STRIKE|MAGEGATE|CRYSTAL|HURT)'`
inside the row's `START … DONE` bracket. `FIGHT` matches FIGHTWORLD, FIGHTPHASE, FIGHTHIT and FIGHTPROBE too; `DRAGON`
matches DRAGONPROBE, DRAGONCROSS, DRAGONGATE, DRAGONHEAL, DRAGONATTACK and DRAGONMAGE. **A log-line row passes on the
VERBATIM line** (`<…>` as the row says).

**Every row, not repeated:** the arena's birth and the dragon's spawn as `GATE-fight-dragon.md`'s *Every row* (the
world's `created`, `arena built … blocks=30615 …`, `FIGHT start … site=arena`, `FIGHT dragon spawned t=0 … phase=CIRCLING
…`, `FIGHTWORLD entered`, `BOSSBAR shown`), then `DRAGON phase-set t=<t> phase=HOVER now=HOVER`; the end:
`FIGHTWORLD returned … why=left`, `BOSSBAR hidden … why=left`, `FIGHT nobody inside … outcome=RUNNING`, `FIGHT stop`,
`FIGHT mage-health … at=end …`, `FIGHT dragon removed reason=nobody-inside-RUNNING was_valid=true …`, `FIGHT ended`,
`FIGHTWORLD unloaded`, `deleting`, `deleted … entries=<e>`, `FIGHTWORLDS loaded=0 folders=0 marked=0 you=world`, the end
probes 0.

**THE LANDING'S LIGHTNING, IN EVERY ROW THAT LANDS A MAGE** (section 6): directly after each `FIGHT mage spawned t=<L> id=<m>
at=<dx> 0.00 <dz>`, **`FIGHT lightning t=<L> at=<dx> 0.00 <dz> effect=true`**, the same tick and spot.
- READ: `CraftWorld.strikeLightningEffect` makes a visual-only bolt.
- READ: `LightningBolt.tick` skips its entity hurt for one, and `spawnFire` returns at once.
- So no HURT line and no fire comes from it. Whether Ben sees it is his row.

### RA1 — immune while a Mage is OWED: the crossing hit lands whole; owed, then alive; his death, and a hit lands (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `land 0 0`: `FIGHT land t=<L> dx=0.000 dz=0.000 result=STARTED`, `FIGHTPHASE t=<L> IDLE->FAST_CLOSE`, `FIGHT mage spawned t=<L> … at=0.00 0.00 0.00`, **`FIGHT lightning t=<L> at=0.00 0.00 0.00 effect=true`**. (2) `mage-die` about 40 ticks later: `FIGHT die t=<K> phase=RETURNING slow_close_started=never`, `FIGHTPHASE t=<K> FAST_CLOSE->RETURNING`. (3) `dragon hit 4000.5`, the ring RETURNING, nothing owed: **`FIGHTHIT t=<t> target=dragon amount=4000.500 before_defense=4000.500 health=15999.500/20000.000 dealer=<his uuid> element=null phase=HOVER`** (the crossing hit lands WHOLE), then **`DRAGONCROSS t=<t> threshold=0.80 health=15999.500/20000.000 pending=1`**. (4) `dragon hit 100`: **`DRAGONGATE t=<t> phase=RETURNING amount=100.000 source=hit refused why=mage-owed`**, no FIGHTHIT. (5) **`DRAGONPROBE … health=15999.500/20000.000 … immune=true … progress=0.8000 … crossed=1 pending=1 …`**. (6) On the tick the ring is home (the return lasts as long as the fast close ran, about 40 ticks): `FIGHTPHASE t=<H> RETURNING->IDLE`, **`DRAGON mage-due t=<H> pending=1`**, `FIGHT land t=<H> dx=<x> dz=<z> result=STARTED`, `FIGHTPHASE t=<H> IDLE->FAST_CLOSE`, `FIGHT mage spawned t=<H> …`, **`FIGHT lightning t=<H> at=<x> 0.00 <z> effect=true`**, **`DRAGON mage-landed t=<H> pending=0`**. (7) `dragon hit 100`: **`DRAGONGATE t=<t> phase=FAST_CLOSE amount=100.000 source=hit refused why=mage-alive`**; the probe **`… immune=true … crossed=1 pending=0 …`**. (8) `mage-die`, then `dragon hit 100`: **`FIGHTHIT t=<t> target=dragon amount=100.000 before_defense=100.000 health=15899.500/20000.000 dealer=<his uuid> element=null phase=HOVER`**; the probe **`… immune=false … progress=0.7950 … crossed=1 pending=0 …`**. (9) The end, `outcome=RUNNING`. **The dx/dz of the second landing are random and READ** | `DragonCrossingsTest` (*theHitThatCrosses…*, *aLivingMage…*), `FightDragonSourceTest` *theDragonsGateRuleIsNoMageAliveAndNoneOwed* | **AS PREDICTED (2026-11-19, boot of `e15cf9ee`, in an END world).** Birth: `FIGHTWORLD end-config … fight_f0b5232f …`, `FIGHTWORLD created name=rpgfight_fight_f0b5232f … ms=33 environment=THE_END`, `FIGHTWORLD end-check name=rpgfight_fight_f0b5232f inert=true fight=present bar_visible=false fight_dragon=none portal=none bedrock_at_centre=0 dragons=0`, then the arena and the dragon. (1) `FIGHT land t=8 dx=0.000 dz=0.000 result=STARTED`, `FIGHTPHASE t=8 IDLE->FAST_CLOSE`, `FIGHT mage spawned t=8 … at=0.00 0.00 0.00`, **`FIGHT lightning t=8 at=0.00 0.00 0.00 effect=true`**. (2) `FIGHT die t=49 phase=RETURNING slow_close_started=never`, `FIGHTPHASE t=49 FAST_CLOSE->RETURNING`. (3) **`FIGHTHIT t=51 target=dragon amount=4000.500 before_defense=4000.500 health=15999.500/20000.000 dealer=b6ae27e9-… element=null phase=HOVER`**, **`DRAGONCROSS t=51 threshold=0.80 health=15999.500/20000.000 pending=1`**. (4) **`DRAGONGATE t=52 phase=RETURNING amount=100.000 source=hit refused why=mage-owed`**. (5) `DRAGONPROBE t=54 … health=15999.500/20000.000 … immune=true … progress=0.8000 … crossed=1 pending=1 …`. (6) `FIGHTPHASE t=90 RETURNING->IDLE`, **`DRAGON mage-due t=90 pending=1`**, `FIGHT land t=90 dx=12.437 dz=15.701 result=STARTED` (20.030 out), `FIGHTPHASE t=90 IDLE->FAST_CLOSE`, `FIGHT mage spawned t=90 …`, **`FIGHT lightning t=90 at=12.44 0.00 15.70 effect=true`**, **`DRAGON mage-landed t=90 pending=0`**. (7) **`DRAGONGATE t=115 phase=FAST_CLOSE amount=100.000 source=hit refused why=mage-alive`**; the probe `immune=true … crossed=1 pending=0`. (8) **`FIGHTHIT t=119 target=dragon amount=100.000 before_defense=100.000 health=15899.500/20000.000 … phase=HOVER`**; the probe `immune=false … progress=0.7950 … crossed=1 pending=0`. (9) The end, `deleted … entries=17` |

### RA2 — no crystal heals the dragon: its health does not rise; the refusal logged once (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `dragon hit 1000`: `FIGHTHIT t=<t> target=dragon amount=1000.000 before_defense=1000.000 health=19000.000/20000.000 dealer=<his uuid> element=null phase=HOVER`; no crossing. (2) **`DRAGON moved t=<t> to=30.00 15.00 0.00 ok=true phase=HOVER`** (READ: vanilla looks for a crystal within the dragon's box inflated by 32; beside crystal 1 at (40.5, 77, 0.5) it is well inside). (3) `land 0 0` with its lightning; the slow close **`FIGHTPHASE t=<S> FAST_CLOSE->SLOW_CLOSE`** about 190 ticks later (DA8 read 190); **`CRYSTAL t=<S+240> lit index=1`**, `CRYSTALSPOT index=1 at=40.50 77.00 0.50 …`. (4) Within the 70 ticks after it: **exactly one `DRAGONHEAL t=<h> refused reason=ENDER_CRYSTAL vanilla=1.000 crystals_lit=1 (once per crystal lit)`** (READ: `checkCrystals` tries a heal every 10 ticks near a crystal; `vanilla=1.000` INFERRED from its float constant). (5) The probe **`DRAGONPROBE … health=19000.000/20000.000 … immune=true … crossed=0 pending=0 …`**: **unchanged**, against F9's DB1, which rose 1400. (6) The end. **IF NO DRAGONHEAL LINE APPEARS, the crystal never came into vanilla's search, the row is HOLLOW (an unchanged health then proves nothing) and says so** | `CrystalHealLogTest`, `FightRulesSourceTest` *aCrystalHealOnAFightsDragon…*, *theHealRefusalIsForTheFightsOwnDragonOnly* | **AS PREDICTED.** (1) `FIGHTHIT t=9 target=dragon amount=1000.000 … health=19000.000/20000.000 … phase=HOVER`. (2) **`DRAGON moved t=10 to=30.00 15.00 0.00 ok=true phase=HOVER`**. (3) `FIGHT land t=14 …`, `FIGHT lightning t=14 … effect=true`; **`FIGHTPHASE t=204 FAST_CLOSE->SLOW_CLOSE`** (190 on); **`CRYSTAL t=444 lit index=1`** (S+240). Also, F9c's: `FIGHT crystal-fire cancelled crystal=e2dc406b-… at=40 77 0 world=rpgfight_fight_2fc9132f` the same tick. (4) **Exactly one `DRAGONHEAL t=463 refused reason=ENDER_CRYSTAL vanilla=1.000 crystals_lit=1 (once per crystal lit)`**. (5) **`DRAGONPROBE t=514 … health=19000.000/20000.000 … immune=true … crossed=0 pending=0 …`: unchanged.** Not hollow: the heal line proves the crystal was in vanilla's search. (6) The end |

### RA3 — the bite: 80 before his armour, named (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRAGON touch t=<t> part=head at=<x y z> phase=HOVER`. (2) Within the 4 ticks: at least one **`DRAGONATTACK t=<t> attack=bite player=BaronVonYeetus base=80.000 gs=100 amount=80.000 vanilla=10.000 phase=HOVER`**, each followed by **`HURT before=80.000 after=72.727 defense=10.000 dealer=<the dragon's id> dealerType=ender_dragon/ender_dragon attack=bite`**. If his defence reads other than 10, after = 80 x 100 / (100 + D) (DA8's figures read that curve at 10). READ: the head's box, inflated by 1, hurts for 10 as a mob attack each tick the dragon is not flinching. `vanilla=10.000` is INFERRED: Normal difficulty leaves a mob attack unscaled. (3) No `attack=unknown->wing`. (4) The end | `DragonAttacksTest`, `FightRulesSourceTest` *theMobRiderAsksTheFightsForADragonsHitFirst*, *eachAttackIsScaledByGearScore…* | **NOT AS PREDICTED: THE BITE LANDED TWICE A TICK, FOUR TICKS RUNNING, AND KILLED HIM.** (1) `DRAGON touch t=31 part=head at=0.00 18.50 -6.50 phase=HOVER`. (2) **`DRAGONATTACK t=31 attack=bite player=BaronVonYeetus base=80.000 gs=100 amount=80.000 vanilla=10.000 phase=HOVER`** as predicted in every field (**`vanilla=10.000` READ**), **but TWO such lines at each of t=31, 32, 33 and 34 (eight)**, and **ten `HURT before=80.000 after=72.727 defense=10.000 dealer=37e433b0-… dealerType=ender_dragon/ender_dragon attack=bite`** (the last two landing after he had been returned). INFERRED: the head's and the neck's boxes both held him (two `hurt` calls a tick), and no vanilla invulnerability window held between them or between ticks, since every event carried the full 10. Then `FIGHTWORLD returned … why=left`, **`BaronVonYeetus was slain by Ender Dragon`**, `BaronVonYeetus died` (three death lines in all), **in the overworld, where keep-inventory is off** (no death rules in this row). The row predicted "no row kills him". (3) No `attack=unknown->wing`. (4) The end |

### RA4 — the wing: 40 before his armour, named; vanilla's push (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRAGON touch t=<t> part=wing at=<x y z> phase=HOVER`. (2) At least one **`DRAGONATTACK t=<t> attack=wing player=BaronVonYeetus base=40.000 gs=100 amount=40.000 vanilla=5.000 phase=HOVER`**, each followed by **`HURT before=40.000 after=36.364 defense=10.000 dealer=<the dragon's id> dealerType=ender_dragon/ender_dragon attack=wing`** (READ: a wing hurts for 5, only when the phase is not a sitting one; HOVER is not). (3) The end. His push is vanilla's and is not read here | as RA3 | **NOT AS PREDICTED: NO WING HIT.** (1) `DRAGON touch t=31 part=wing at=4.50 19.00 0.00 phase=HOVER`. (2) **No `DRAGONATTACK` and no HURT** in the 4 ticks before he left. Why is not in the log: either he was not in the wing's attack box, or vanilla's own condition on the wing's hurt was not met (READ only partly: the hurt follows a check on his last-hurt-by-mob time). (3) The end |

### RA5 — the breath: 30 a second, each hit 30 x interval / 20, named (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRAGON phase-set t=<t> phase=BREATH_ATTACK now=BREATH_ATTACK` (READ: the API's phases map to vanilla's by order, and `BREATH_ATTACK` is vanilla's `SITTING_FLAMING`). (2) **`DRAGON to-breath t=<t> cloud=<uuid> at=<x y z> radius=5.00 interval=20`** (READ: the cloud spawns 10 ticks into the phase, on the floor below a point 5 ahead of the head, radius 5, the default reapplication 20). (3) One or more **`DRAGONATTACK t=<t> attack=breath player=BaronVonYeetus base=30.000 gs=100 amount=30.000 vanilla=<v> phase=BREATH_ATTACK`**, each followed by **`HURT before=30.000 after=27.273 defense=10.000 dealer=<the dragon's id> dealerType=ender_dragon/ender_dragon attack=breath`**, consecutive ones **20 ticks apart**. **`<v>` IS THE READING** of vanilla's breath hit (INFERRED 3.000). (4) The end. **IF `cloud=none`, the API cannot stage the breath; the row says so, and the breath is read in Ben's rows** | `DragonAttacksTest` *theBreathCosts…*, `FightRulesSourceTest` *theBreathIsRidden…* | **NOT AS PREDICTED: NO BREATH HIT.** (1) **`DRAGON phase-set t=8 phase=BREATH_ATTACK now=BREATH_ATTACK`**. (2) **`DRAGON to-breath t=23 cloud=4399b190-… at=0.00 0.00 -9.00 radius=5.00 interval=20`**: the cloud existed where READ, and **its interval READ 20**. (3) **No `DRAGONATTACK attack=breath`, no HURT** in the 45 ticks on it (READ defaults predict hits about 20 and 40 ticks after its spawn). **`vanilla=` was never read.** Why is not in the log: no line shows whether the cloud applied to him at all (an instrument to add: the cloud's apply event). Ben in EB4: "yes but I didn't take damage" (`GATE-fight-end.md`). (4) The end |

---

## WHAT THIS GATE DOES NOT READ

- **Ben's eyes and feel** (the lightning, the attack numbers, the fight): `GATE-fight-end.md`'s Ben rows.
- **A real flight into a player**: RA3 to RA5 put him in the boxes; Ben's fight reads the dragon finding him itself.
- **The dragon's vanilla hit on its own Mage** (now cancelled, `DRAGONMAGE`): it cannot be staged by the API; Ben's fight
  reads any `DRAGONMAGE` line, and the Mage's death message.
- **A fireball entity**: `FIGHT fireballs t=<t> count=<n>` logs on a change only; with the fireball cancelled (F9), Ben's
  fight is predicted to log none.
- **A bite and a wing in one tick** (a player inside both boxes): called a bite (INFERRED rare; the head is far from the
  wings).

---

## THE BOOT OF 2026-11-19 — WHAT THIS FILE'S ROWS FOUND

Read on `feat/fight-end`'s head `e15cf9ee`. Its R0, carried state and checklist are in `GATE-fight-end.md`. Every row
ran in an END world checked inert. **No prediction above was edited.**

**FINDINGS:**

1. **THE BITE IS LETHAL IN FOUR TICKS: IT LANDS TWICE A TICK, EVERY TICK (RA3).**
   - Eight `DRAGONATTACK … attack=bite` lines at t=31 to 34, two a tick, and ten `HURT before=80.000 after=72.727 …
     attack=bite`. That is about 727 against his 350.
   - INFERRED cause: the head's and the neck's boxes both held him, so vanilla called `hurt` twice a tick. Every
     event carried the full 10 (`vanilla=10.000`), so no vanilla invulnerability window held between them or between
     ticks.
   - The mob rider's own note ("a player is hit at most once per ~0.5s window") does not hold for the dragon's hurt.
   - **A fix is owed and not made here:** the fight's dragon needs its own window per player, so that one bite is one
     bite.
2. **HE DIED IN THE OVERWORLD, WITH KEEP-INVENTORY OFF (RA3).**
   - The last two hits landed after he had been returned (`applyDamage` is deferred): `BaronVonYeetus was slain by
     Ender Dragon`, then `died`, three death lines in all.
   - RA rows set no death rules, so **his inventory was likely dropped at his return location** (INFERRED).
   - He gave himself a new `cursed_emerald` at 12:53:22, during EB1.
   - Owed: a hit priced in a fight must not land once its victim has left the fight.
3. **THE WING STRUCK NOTHING (RA4; EB3 likewise).**
   - The touch put him at `4.50 19.00 0.00` from the centre, and no `DRAGONATTACK` followed in 4 ticks.
   - Not in the log: whether he was inside the wing's attack box, and whether vanilla's condition on the wing's hurt
     (his last-hurt-by-mob time) held.
   - Instrument to add: the touch line prints his box and the part's attack box.
4. **THE BREATH HIT NOTHING (RA5; EB4 likewise).**
   - The cloud existed as READ (`radius=5.00 interval=20`); no hit in 45 ticks on it.
   - Instrument to add: the cloud's apply event (`AreaEffectCloudApplyEvent`), naming who it applied to.
5. **AND THE BITE DID NOT LAND IN EB3** (`GATE-fight-end.md`), whose touch read `part=head at=-0.85 18.49 -7.24`: the
   same staging as RA3, and none of RA3's eight hits. Item 3's instrument answers both.
