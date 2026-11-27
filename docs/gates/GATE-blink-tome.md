# GATE — THE BLINK TOME AND ITS BLINK (2026-11-26), WITH THE HEALING TOME GATE, THE BONE CLUB GATE, THE BLAST FUNGUS GATE AND TT20 TO TT27

**Status: RUN 2026-11-26 on `1f447197`, the whole command; readings at the end, under READINGS.** The plan is `docs/plans/PLAN-blink-tome.md`. The scenario is
`paper/src/main/resources/selftest/GATE-blink-tome.yml`. The stack, bottom up: #316 `feat/class-damage-labels`, #317
`feat/fused-projectile`, #318 `feat/blast-fungus`, #319 `feat/fast-and-fungal`, #320 `feat/cluster-bombs`, #322
`feat/bone-club`, #323 `feat/drumming-beat`, #324 `feat/healing-tome`, #325 `feat/tome-pulse`, #326 `feat/tome-selftest`,
then `feat/blink-tome`, `feat/blink-runs`, `feat/blink-selftest`, and the ONE gate branch at the top,
`feat/blast-fungus-gate` (#321), MOVED UP onto `feat/blink-selftest` (its four commits unchanged, `git range-diff` `=`
for all four), carrying this gate as well. **Every prediction below was written before any boot, and is frozen from the
seat's approval.** No boot without Ben's own "ready" and the seat's approval of the code AND this gate from origin.

## WHAT BEN TYPES — ONE COMMAND FOR THE WHOLE STACK

```
/rpg selftest bone-club+blast-fungus+true-tooltips.TT20.TT21.TT22.TT23.TT24.TT25.TT26.TT27+healing-tome+blink-tome
```

- `bone-club`, `blast-fungus`, `true-tooltips.TT20 … TT27` and `healing-tome` exactly as `GATE-healing-tome.md` lists
  them (unchanged).
- `blink-tome`, this whole gate, **LAST**: its setup, then BT1 to BT11. **It asks nothing.** The two tome gates run after
  both asks (BF9's and TT27's), the Healing Tome's first, then this one.
- The queue, step for step, is `BlinkTomeGateCommandTest` (parsed against the shipped scenarios; the lower gates' queue is
  `HealingTomeGateCommandTest`'s, unchanged).
- **This gate's R0 supersedes the R0 of every gate below it** (theirs are left as the record).

He types it on the pad's server, standing anywhere; the first setup takes him to the pad. **He answers TWO questions in the
whole command, BF9's and TT27's, as before: this gate adds none.**

**How long it runs (an ESTIMATE from the scenarios' waits, not a measurement):** this gate's `wait:` steps sum to 1395
ticks (setup 40; BT1 48, BT2 62, BT3 242, BT4 74, BT5 74, BT6 229, BT7 288, BT8 90, BT9 90, BT10 86, BT11 72), about 70 s,
with each row's CLEAN about a minute more in all. With `GATE-healing-tome.md`'s estimate for the rest (about six and a
half minutes, plus two answers): **about eight and a half minutes, plus two answers.**

## SAFETY — 19.000 FOR THE WHOLE COMMAND; THIS GATE DEALS HIM 0.000

- **No row wounds him.** No `/rpg damage`, no `damage` command, no live mob.
- **Every mob in this gate is a NoAI zombie** (`{NoAI:1b,…}` in every `summon`, read back by
  `BlinkTomeGateCommandTest.everyBlinkingRowStagesTheEnumeratedLayout`; lint `MOB_ACT_UNSTAGED` green). **READ, the 26.1.2
  server jar:** `Mob.isEffectiveAi()` returns false when `isNoAi()`, and `LivingEntity.aiStep` calls `serverAiStep` (the
  goal and target selectors, the melee goal) only when `isEffectiveAi()`. So a NoAI zombie never acts; and nothing clears
  the `target` field `mob_target` sets (no target selector runs), so it HOLDS its target.
- **His own hits and casts land on zombies only.** BT4's Flint Staff and BT5's wooden sword hit mobs; the sides table
  never lets them reach a player. BT6's wolf is his summon: READ, `CombatSides.refused` refuses SUMMON → PLAYER.
- **The blink cannot hurt him:** every landing is on the pad's stone floor with two clear body blocks (the decision), the
  fall distance is cleared before and after the teleport, and the floor is the one he stood on (y 1 in every enumerated
  spot, `BlinkPadLandingTest`): no fall.
- **This gate: 0.000.** `GATE-healing-tome.md`: 19.000 (7 + 8 + 2 + 2, each EXECUTED in its own test). `GATE-bone-club.md`:
  0.000. `GATE-blast-fungus.md`: 0.000. TT20 to TT27: 0.000.
- **THE SUM FOR THE WHOLE COMMAND: 0.000 + 0.000 + 0.000 + 19.000 + 0.000 = 19.000, at or under 20.000.**

## CARRIED STATE — THE BOOTER'S CHECKLIST, IN THIS ORDER (`.claude/rules/booting.md`, each step ticked in the report)

1. The seat's approval of the code AND this gate from origin; Ben's own "ready".
2. **The heads:** `git ls-remote --heads origin` for the fourteen branches, equal to what the seat diffed; CI green on each.
   `git status --porcelain` prints nothing and `git rev-parse HEAD` equals `feat/blast-fungus-gate`'s head.
3. **No JVM:** `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` counts 0.
4. **The backup**, to a NEW folder `C:\Users\Neb91\IdeaProjects\rpg-run-backups\<date>-pre-blink-tome`: `run\world`,
   `run\plugins\Rpg` (which holds `accessories\<his uuid>.json` and `players\<his uuid>.json`) with their file counts and
   copy counts, and his player `.dat` with its bytes. **His accessory file's bytes and his profile's `archetypeId` are
   quoted before the boot**, so the run's CAPTURED line has a third witness.
5. **The world's saved difficulty,** read from the decompressed `level.dat`, before and after the run.
6. **Config:** `dev-pack-on-join: true`. No `run\rpgfight`.
7. **"BOOTING NOW", in its own message, BEFORE the boot command**, with: "Ben: do NOT log in until I tell you R0 passed."
8. `./scripts/dev-server.sh --refresh-content`; the `Done` matched to a log newer than the deployed jar.
9. R0 read on this boot's log; **"R0 PASSED" in its own message, before he logs in.** His first `joined the game` time
   reported beside R0's.
10. After his `/stop`: the clean-stop lines, no `java` process left; **his accessory file's bytes and his profile's
    `archetypeId` read again and compared with step 4's** (H15's third witness).

**HIS CLASS, ACCESSORIES AND INVISIBILITY, BEFORE AND AFTER (H15, K9):** the run's CAPTURED line is as in
`GATE-healing-tome.md`. **The prediction for the RESTORED line, after BT11:** its `accessories=[…]` equal to the CAPTURED
line's, no `NOT-RESTORED=accessories(…)`, `cell=<his class>/<his element>` with the CAPTURED class, `xp=<his captured
lifetime XP>`, `max_summons_base=<captured>` (BT6 resets it), and the run-end fields, in this order, at its end:
**`… summons=0 fused=0 blink=none drag_tick=false`** (`blink=none`: BT11 ends with `clear_state`, and nothing blinked in
it).

> **A NOTE ON `GATE-healing-tome.md`'s H15 WORDING, 2026-11-26 (CC, found writing this gate; that file is not edited, its
> commits stay unchanged).** HT predicts the RESTORED line "ends `… armour=<n> offhand=<0|1> saturation=<s>
> accessories=[…]`". READ, `SelfTestSnapshot.restore` and `SelfTestRun.end`: the line runs `… saturation=<s>
> accessories=[…] armour=<n> offhand=<0|1>` and then ` summons=<n> fused=<n>`, now ` blink=<…>`, and ` drag_tick=false`.
> The FIELDS HT names are all present and its claims about their values stand; only "ends" and the order are loose. Read
> HT's H15 by its fields, not by the line's end. **The seat's ruling is asked.**

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

- **R0a:** `[Rpg] Build: <sha>`, `feat/blast-fungus-gate`'s head as `git ls-remote --heads origin feat/blast-fungus-gate`
  prints it, shortened. Not `-dirty`, not `unknown`.
- **R0b:** ALL FOUR scripts on the DEPLOYED jar.
  - `docs/gates/GATE-blink-tome-r0b.ps1`: before `'--- absent'` **27 `PRESENT`, 0 `ABSENT`** (9 `Entry`, 6 `Has`, 12
    `ValueLine`); between `'--- absent'` and `'--- controls'` **4 `ABSENT`, 0 `PRESENT`** (no `modifiers:`, no
    `mana_cost:`, in no loot source, no `ResourcePool` in `BlinkTomes`); after `'--- controls'` **2 `PRESENT`, 0
    `ABSENT`**.
  - `docs/gates/GATE-healing-tome-r0b.ps1`: **25 / 0, 4 / 0, 2 / 0**, unchanged.
  - `docs/gates/GATE-bone-club-r0b.ps1`: **21 / 0, 5 / 0, 2 / 0**, unchanged.
  - `docs/gates/GATE-blast-fungus-r0b.ps1`: **18 / 0, 4 / 0, 2 / 0**, unchanged.
  - **Run before commit on a jar built from this tree: quoted under THE SCAN RUN BEFORE COMMIT.**
- **R0c:** `[Rpg] Loaded …, 27 weapons, …, 8 accessories, …` (recounted: `ls content/accessories/*.yml | wc -l` = 8, 7
  at `7a708145`; the weapons and enchants unchanged from `GATE-healing-tome.md`'s 27 and 27). `Mint-on-craft: 42 indexed,
  42 claiming, of 61 gear definitions`, unchanged (accessories are not gear definitions of the crafting index, ruling A3).
  **0** `WARN]: [Rpg]` lines (control: `INFO]: [Rpg] Loaded` reads 1 or more); none of `Refusing`, `Skipping`,
  `SEVERE`, `Exception`.
- **R0d:** exactly one **`[Rpg] Selftest: dev server, 530 scenarios across 55 gates [melee-cell, …, bone-club,
  healing-tome, blink-tome]`**. Recount: 55 files, 585 keys (`cat GATE-*.yml | grep -cE '^[A-Za-z0-9_-]+:'`), 55 setups,
  so 530. Control: 573 + 12 (its setup and BT1 to BT11) = 585. Controls: `519 scenarios` ABSENT, `54 gates` ABSENT.
- **R0e:** one `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`; and no `FUSED removed …
  cause=orphan`.

## THE ROWS

Each row's lines are those between `SELFTEST blink-tome <row> START` and its `DONE`. **Every row is AUTO, SURVIVAL, on the
pad, and stages itself:** level 2, empty inventory and state (`STAGE clear_state … drum=0 blink=none`), `accessory:
clear`, `class: mage` (unless the row says), 40 ticks. Every row-start CLEAN line now ends **` drum=0 blink=none`** (the
last row ended its own blink).

**THE CLOCK: the server tick.** A `wait: N` resumes exactly N ticks later, and steps with no wait between them run in one
tick (READ, `SelfTestRun.advance`). **E** is the tick of `STAGE accessory equip slot=0 id=blink_tome tick=E`. **T0, THE
ANCHOR,** is printed by the clock: `BLINK START wearer=<him> accessory=blink_tome slot=0 tick=T0 period=5`, written by the
first reconcile pass that sees the tome contribute, so **T0 ∈ [E, E + 5]** (READ, `PlayerHealthSystem`'s 5-tick pass;
the pulse's anchor). The check runs at **T0 + 5, T0 + 10, …** (READ: `RepeatingTask.start` arms one period out).
**M** is the tick printed by `DRIVE mob_target <uuid> tag=<t> tick=M`. **B** is the BLINK line's `tick=`.
**Every BLINK line in this gate has B ∈ [M, M + 5]**: in every row the clock has started (T0 + 5 ≤ E + 10 ≤ M) and the
condition becomes true at M; the next check is at most 5 ticks later (the check and the step may share a tick; their
order inside it is not read, so M itself is allowed).

**THE STAND (BlinkPadLandingTest, `everyBlinkingRowStagesTheEnumeratedLayout`):** every row but BT1 summons, at the pad's
stand and before he moves, four NoAI blocker zombies (tag `bkb`) at pad (17, 13), (23, 13), (17, −13), (23, −13), and its
own zombies, then `tp @s ~20 ~ ~`: **he stands at pad (20, 1, 0), the block centre (20.5, 201.000 in world Y, 0.5)**. From
there, with every cell outside the pad taken at its worst, **every spot the Blink can choose is on the pad's interior
floor (feet y 201.000) inside the preflight's box (0..36 east, −13..13 south): 508 spots with a trigger three east, 479 in
BT9 (EXECUTED)**. A zombie summoned after a blink is summoned at his feet, wherever he landed.

**THE BLINK LINE (K12):** `BLINK wearer=<him> tick=B trigger=zombie <uuid> dist=<d> from=<x y z> to=<x y z> safe=<n>
forgot=<n>`, each coordinate to three places. Around it, `BLINK feedback wearer=<him> at=<from> particle=poofx12
sound=entity.enderman.teleport` (before it, at the departure) and a second `BLINK feedback … at=<to> …` when the teleport
lands (READ: `teleportAsync` runs `teleport0` through `scheduleOnMain`; INFERRED: within the next tick or two).

**In world coordinates, with the stand's block (sx, 200, sz) from the PAD line:** from= is **`<sx+20.5> 201.000 <sz+0.5>`**;
the trigger three east is at **(sx+23.5, 201, sz+0.5)**, so **`dist=3.000`** (EXECUTED, `BlinkTomeGateArithmeticTest`).
**Every to= is `<x> 201.000 <z>` with x − sx in [4.5, 36.5] and z − sz in [−12.5, 13.5], a block centre (x.500, z.500),
within 16.000 of from=, and more than 5.000 from the trigger and from each blocker** (EXECUTED over every choosable spot).
Which spot is RANDOM (uniform over the 508): **a reading, not a prediction.**

### BT1 — THE TOOLTIP, WHOLE, AND A4'S REFUSALS BY TEXT

Level 2, a mage, the tome given and held (NOT equipped).
1. `HELDID slot=<n> item=blink_tome`.
2. `dump: held`, WHOLE (EXECUTED, `BlinkTomeGateArithmeticTest.theHeldLine`, through `WeaponItems.displayName`,
   `AccessoryLore.build` and the instrument's own `SelfTestLines.styled`; predicted before it was run, and it matched):
   **`HELD blink_tome BOOK x1 name="Blink Tome" [blue] lore=["Blink" [gold]; "When a hostile mob targeting you comes
   within 5 blocks," [gray]; "you teleport to safety and turn invisible for 8s." [gray]; "Attacking ends the
   invisibility." [gray]; "Cooldown: 60s" [gray]; "Mage class accessory" [gray]; "" []; "Rare Accessory" [blue]]`**. No
   modifier line, no flavour, no gear score, no enchant block (A4).
3. **A4's existing words, each a REPLY** (P-R1): `rpg enchant show` → **`REPLY "Accessories cannot be enchanted." [red]`**;
   `rpg gearscore set 100` → **`REPLY "Accessories carry no gear score." [red]`**; `rpg durability damage 1` → **`REPLY
   "Accessories do not wear out." [red]`** (K14: the same arms that refuse the Healing Tome, keyed on `accessory_id`).
4. No `BLINK` line of any kind (carried, not equipped).

### BT2 — THE BLINK

1. `STAGE accessory equip slot=0 id=blink_tome tick=E`; **`BLINK START wearer=<him> accessory=blink_tome slot=0 tick=T0
   period=5`, T0 ∈ [E, E + 5].**
2. Before: `SELF … at=<sx+20.50> 201.00 <sz+0.50> …`; **`TARGET tag=bt <uuid> type=zombie target=none dead=false`** (the
   control: it targets nobody until staged).
3. **`DRIVE mob_target <uuid> tag=bt tick=M`** (no `refused`), then **`BLINK wearer=<him> tick=B trigger=zombie <bt's
   uuid> dist=3.000 from=<sx+20.500> 201.000 <sz+0.500> to=<x> 201.000 <z> safe=508 forgot=1`, B ∈ [M, M + 5]**, to= as
   above.
4. After (M + 8): **`SELF … at=<to's x> 201.00 <to's z> …`** (his position equals to=, to two places); **`TARGET tag=bt
   <uuid> type=zombie target=none dead=false`** (it forgot him; the forget runs on the zombie's own thread a tick after
   the blink); **`INVIS present=true ticks=<t> amp=0 particles=false blinked=true`** with **t = 160 − (M + 8 − B), within
   1** (INFERRED: the effect counts down once a tick in the entity tick, whose phase against the step is not read), so t
   ∈ [151, 161].
5. **The sheet** (`rpg stats`): its REPLY lines include **`REPLY "Accessories" [gold]`** and **`REPLY "  Blink Tome:
   Blink" [gray]`**.
6. `clear_state`: **`BLINK END wearer=<him> tick=<t> reason=cleared`**, then `STAGE clear_state … blink=ended`; then
   **`INVIS present=false ticks=0 amp=0 particles=false blinked=false`** (only the blink's effect, removed).

### BT3 — HIDDEN, THEN THE EXPIRY

The blink as BT2 (`… safe=508 forgot=1`, B ∈ [M, M + 5]). Then, at M + 8, a zombie summoned at his feet (tag `bh`):
1. **`BLINK REFUSED-TARGET mob=zombie <bh's uuid> tick=R`, then `DRIVE mob_target <bh's uuid> tag=bh refused tick=R`**
   (the refusal is logged inside the seam, before the driver's line).
2. **`TARGET tag=bh <uuid> type=zombie target=none dead=false`**.
3. **`INVIS present=true ticks=<160 − (R − B), within 1> amp=0 particles=false blinked=true`**.
4. **THE EXPIRY: `BLINK END wearer=<him> tick=<B + 160> reason=expired`, exactly 160 ticks after the BLINK line**
   (READ: the expiry is scheduled with `onEntityLater(player, …, 160)`; INFERRED: Paper runs a task delayed by 160 exactly
   160 ticks later, the pulse's own inference). Its tick is read against B.
5. At R + 160 (≥ B + 163): **`INVIS present=false ticks=0 amp=0 particles=false blinked=false`**.
6. **`DRIVE mob_target <bh's uuid> tag=bh tick=<R + 160>`** (no `refused`: the state is over), and two ticks later
   **`TARGET tag=bh <uuid> type=zombie target=self dead=false`**.
7. **No second `BLINK wearer=` line in the row**, though `bh` stands on his feet targeting him: the cooldown (1200 ticks
   from B).

### BT4 — A SPELL CANCELS

The blink as BT2. Then: `INVIS present=true … blinked=true`; the Flint Staff given and held, turned east, mana refilled.
**`DRIVE use PlayerInteractEvent RIGHT_CLICK_AIR HAND item=flint_staff yaw=-90.000 pitch=0.000 cadence=excluded`, then
`BLINK END wearer=<him> tick=<U> reason=cast`, then `USE result …`**: the END is written inside the press's own call
(READ: `SelfTestDrivers.use` logs the DRIVE line, dispatches the event synchronously, then logs the result; `WeaponFire`
tells the sink inside the Success branch, before its region hop), so **U is the press's tick.** Then **`INVIS present=false
ticks=0 amp=0 particles=false blinked=false`**. `clear_state … blink=none`.

### BT5 — A MELEE HIT CANCELS

The blink as BT2. Then a zombie at his feet (tag `bm`), the wooden sword given and held, `DRIVE aim … target=<bm> zombie
…`; `INVIS present=true … blinked=true`; **`DRIVE attack Player#attack target=<bm's uuid> zombie d=0.000 charge=<c>
attack_damage=<a>` (no `NO_DAMAGE_EVENT` line: a wooden sword has attack damage), then `BLINK END wearer=<him> tick=<the
same tick> reason=melee`** (READ: `Player#attack` raises the damage event synchronously and `onPlayerMeleeAttack` tells
the sink past the summon refusal and the broken cancel). Then **`INVIS present=false … blinked=false`**. `clear_state …
blink=none`.

### BT6 — THE WHISTLE DOES NOT

The blink as BT2 (`rpg maxsummons reset` first: READ, `MaxSummons.DEFAULT` is 1, so a mage may summon one). Then the Bone
Whistle given and held, turned east, mana refilled: **`DRIVE use … item=bone_whistle yaw=-90.000 pitch=0.000
cadence=excluded`, then `USE result …`, with NO `BLINK END` line between them** (a SUMMONER weapon's trigger; READ, a
summon cast returns before `WeaponFire`'s Success branch). Five ticks later **`SUMMONPROBE weapon=bone_whistle body=<u>
alive=true …`** and **`INVIS present=true … blinked=true`**. **The state runs on: `BLINK END wearer=<him> tick=<B + 160>
reason=expired`**, then **`INVIS present=false … blinked=false`**. The row's `clear_state` removes the summon:
**`STAGE clear_state … summons=1 … blink=none`**.

### BT7 — THE COOLDOWN

**The control is the row's own first blink:** BT2's BLINK line (`safe=508 forgot=1`) and `BLINK END … tick=<B + 160>
reason=expired`. Then, at M + 168, a zombie at his feet (tag `bk2`): **`DRIVE mob_target <bk2's uuid> tag=bk2 tick=<M +
168>`** (no `refused`). Across the next 30 ticks (six checks), **no `BLINK wearer=` line**, and **`TARGET tag=bk2 <uuid>
type=zombie target=self dead=false`**: a hostile mob targets him within 5 and nothing fires, because the cooldown has
about 1030 ticks left. **Re-equipping:** `accessory: clear` at X → **`BLINK STOP wearer=<him> tick=<t> reason=<inactive
or ended>`, t ∈ [X, X + 5]** (READ: the reconcile pass stops it with `inactive`, the check's own re-check with `ended`;
which comes first is their phase, not read); six ticks later, given, held, `equip 0` → a NEW **`BLINK START …
tick=T0'`, T0' ∈ [E', E' + 5]**; across the next 30
ticks **no `BLINK wearer=` line**, and `TARGET tag=bk2 … target=self` again. **Exactly one `BLINK wearer=` line in the
row.** The boundary itself (fires at 1200, not at 1199) is not read here, for want of a verb that stages a cooldown's
remainder: it is a unit table (`BlinksTest.theWearersState_holdsIt`: 1 tick left holds it, 0 does not;
`CooldownTrackerTest`: ready at its tick, `>=`).

### BT8 — NOT TARGETING

The zombie three east (tag `bt`) is NOT targeted. **`BLINK START … tick=T0`, T0 ∈ [E, E + 5]**; across E to M = E + 40 (at
least seven checks) **no `BLINK wearer=` line**; **`TARGET tag=bt <uuid> type=zombie target=none dead=false`**.
**The control, in the row:** `DRIVE mob_target <uuid> tag=bt tick=M`, then **`BLINK … trigger=zombie <bt's uuid>
dist=3.000 … safe=508 forgot=1`, B ∈ [M, M + 5]**.

### BT9 — FARTHER THAN 5

A zombie six east (tag `bf6`, at sx+26.5) staged as targeting him: **`DRIVE mob_target <bf6's uuid> tag=bf6 tick=M1`**;
across M1 to M1 + 30 (six checks) **no `BLINK wearer=` line**; **`TARGET tag=bf6 <uuid> type=zombie target=self
dead=false`** (it targets him, at 6.000). **The control, in the row:** the zombie three east (tag `bt`): `DRIVE mob_target
<uuid> tag=bt tick=M2`, then **`BLINK … trigger=zombie <bt's uuid> dist=3.000 … safe=479 forgot=2`, B ∈ [M2, M2 + 5]**
(the trigger is the nearer; both forget him; the far one is a hostile near his stand too, so 479, EXECUTED).

### BT10 — INACTIVE, AND LIVE AGAIN

Equipped at E as a mage, then **`class: ranger` in the same tick** (`STAGE class found=mage set=ranger …`), then `DRIVE
mob_target <uuid> tag=bt tick=E` (lands: he is not blinked). **INACTIVE: across E to E + 30, no `BLINK` line of any kind**
(no START: no reconcile pass ever saw him a mage wearing it). **The sheet** (`rpg stats`, at E + 30): **`REPLY "  Blink
Tome: inactive (requires Mage)" [dark_gray]`**. **LIVE AGAIN, the control:** `class: mage` at C = E + 32 → **`BLINK START …
tick=T0`, T0 ∈ [C, C + 5]**, then **`BLINK … dist=3.000 … safe=508 forgot=1` at B = T0 + 5** (the condition is already
true; READ: the first check is one period after the start; INFERRED: exactly 5). `clear_state … blink=ended`.

### BT11 — CARRIED, NOT EQUIPPED

A mage holding the tome in his main hand (`HELDID slot=<n> item=blink_tome`), his slots empty, `DRIVE mob_target <uuid>
tag=bt tick=M` (lands): **no `BLINK` line of any kind across 30 ticks.** Control: BT2 (the same staging, equipped, blinked).

### H15 — AFTER THE LAST ROW

See CARRIED STATE: the RESTORED line's fields, ending **`… summons=0 fused=0 blink=none drag_tick=false`**.

## THE COORDINATE TABLE (pad coordinates: x east and z south of the stand column, y 1 his feet on the floor)

| what | where | in the box 0..36 × −13..13 and the interior |
|---|---|---|
| the pad's stand (setup, every row-start CLEAN) | (0, 1, 0) | yes |
| his stand in BT2 to BT11 (`tp @s ~20 ~ ~`) | (20, 1, 0) | yes |
| the four blockers (`bkb`) | (17, 13), (23, 13), (17, −13), (23, −13) | yes |
| the trigger three east (`bt`) | (23, 0) | yes |
| BT9's zombie six east (`bf6`) | (26, 0) | yes |
| every landing (to=) | x 4..36, z −13..13, y 1: 508 spots (479 in BT9), ENUMERATED | yes, every one (`BlinkPadLandingTest`) |
| zombies at his feet after a blink (`bh`, `bm`, `bk2`) | his landing | yes (a landing) |
| BT6's wolf | his summon's own spawn beside him | INFERRED inside (the summon's spawn is not this gate's); removed by the row |

The preflight `0 -13 36 13` measures the whole box (every column: feet and head air, a solid floor) before any row.

## NOT READ BY THIS BOOT, SAID PLAINLY, AND OWED

- **A live, moving mob losing interest** (every mob here is NoAI): that a real zombie chasing him turns away at the blink,
  and that its goals re-asking are refused by `onHiddenTarget` at vanilla's target event. **This gate's REFUSED-TARGET
  line is the engine seam's (`MobTargets.set`, which the selftest's `mob_target` uses), NOT the vanilla event's**: the
  event listener is pinned by `BlinkTomesWiringTest` and READ in the jar, and is NOT READ here.
- **What he looks like while invisible** (the client's rendering: armour, held items and stuck arrows still drawn, his name
  hidden; READ in the 26.1.2 client jar, `PLAN-blink-tome.md` K6). AUTO reads the server's effect, never the client.
- **The feedback's look and sound** (only that it was called, by its trace line).
- **The dragon** (K8): what the dragon fight does to a blinked player.
- **The brain-driven mobs K7 cannot clear** (the `BLINK KEPT` line), a warden's anger, a piglin's memory: none staged.
- **A second player** (players never trigger it; Ben: "until we do a pvp pass").

## THE SCAN RUN BEFORE COMMIT

- `ScenarioLint` over all 55 gates: this gate's findings **none** (`BlinkTomeGateCommandTest.theGateLintsClean_andAsksNothing`:
  every rule), and no new finding anywhere. `AskWindowLintTest` unchanged: this gate has no ASK. The gate-document test
  sees both sections.
- **The R0b scripts, run on a jar built from this tree (`paper/target/rpg-0.1.0-SNAPSHOT.jar`, 2026-11-26):
  27 / 0, 4 / 0, 2 / 0 (this gate); 25 / 0, 4 / 0, 2 / 0; 21 / 0, 5 / 0, 2 / 0; and 18 / 0, 4 / 0, 2 / 0, as predicted.**
- The full suite on this tree (`./mvnw clean test`): core 2711 run (1 skipped), storage 103, paper 2213, 0 failures.

## CC'S CALLS IN THIS GATE

1. **The gate runs LAST in the command**, after the Healing Tome's, so its staging touches no lower row.
2. **He stands 20 east of the pad's stand, with four NoAI blockers,** so every landing the decision can choose is inside
   the box whatever lies outside the pad (the alternative, a larger pad, changes every gate's ground).
3. **BT3 reads HIDDEN and the EXPIRY on one blink**; **BT8, BT9 and BT10 carry their controls in the row.**
4. **The cooldown's boundary is a unit table**, for want of a verb that stages a cooldown's remainder (the brief's
   alternative); the row reads its inside.

## PLACEHOLDERS OPEN FOR BEN

`PLAN-blink-tome.md`, PLACEHOLDERS: the tooltip and sheet wording, the feedback (puff and sound), no mana cost, taking
damage does not end it, a passive animal does not block a landing, no line of sight needed, what stays visible.

## WHAT ONLY THE BOOT CAN SHOW

That the server's tick spacing agrees with the READs (every `tick=`); that the reconcile pass anchors the clock within 5
ticks of an equip; that `teleportAsync` lands him where to= says on a real server; that the forget runs and the zombie's
target reads empty; that the invisibility effect is applied and taken off as the decision says; that the selftest's
three clears run on a real store; that the tooltip renders as dumped.

## READINGS — THE BOOT OF `1f447197`, 2026-11-26 (THE WHOLE COMMAND: BONE CLUB, BLAST FUNGUS, TT20 TO TT27, HEALING TOME, BLINK TOME)

**No prediction in any of the five gates was edited.** This section holds the readings of every gate the one command
ran; each lower gate carries a dated pointer here. Rows are classed READ AS PREDICTED, MISS, or DID NOT TEST WHAT IT WAS
MEANT TO. Every tick and figure below is quoted from this boot's `latest.log` (copied whole before the next boot).

### DATED NOTE, 2026-11-26 — THE SEAT'S RULING ON LINE WORDING, IN ITS WORDS

> *"A PREDICTED LINE IS READ BY ITS NAMED FIELDS, NOT BY WHERE THE LINE ENDS OR THE ORDER OF FIELDS THE PREDICTION DID
> NOT NAME. A line that has since gained fields (the RESTORED line's summons= fused= blink= drag_tick=; a CLEAN or
> clear_state line's blink= after drum=; DRIVE mob_target's tick=; the longer CAPTURED line; HEALED lines in lower
> gates' rows) is NOT a miss for that. A named field with a different value IS a miss."*

It covers every frozen gate, and it answers the question this gate's note on `GATE-healing-tome.md`'s H15 asked. Every
row below is read by it.

### CARRIED STATE, AS IT WAS ACTUALLY RUN

1. The seat's approval of `1f4471979a00` (via Ben, 2026-11-26); Ben's "ready", typed above it.
2. **Heads:** `git ls-remote` read master `cd13fa873416`, `feat/class-damage-labels` `fcfbefbb90ca`, `feat/fused-projectile`
   `9b19aefa1a8e`, `feat/blast-fungus` `5e2be889b92f`, `feat/fast-and-fungal` `e0d3d37b27a0`, `feat/cluster-bombs`
   `305e13612aba`, `feat/bone-club` `921c953c39e2`, `feat/drumming-beat` `01bf729a4329`, `feat/healing-tome`
   `e9ef23c30933`, `feat/tome-pulse` `cb8c8af7cc45`, `feat/tome-selftest` `7a708145633d`, `feat/blink-tome` `248d6ae14b5e`,
   `feat/blink-runs` `1ca71f7fdb1a`, `feat/blink-selftest` `039d7f2c4485`, `feat/blast-fungus-gate` `1f4471979a00`: all
   fifteen the seat's. CI `build=success` twice on each of the fourteen branch heads. `git status --porcelain` printed
   nothing; `HEAD` = `1f4471979a0038098a803926fe7ec3e0f7b48e18`.
3. **No JVM:** `java.exe` count 0.
4. **The backup**, to a NEW folder `C:\Users\Neb91\IdeaProjects\rpg-run-backups\2026-11-26-pre-blink-tome` (absent
   before): `run\world` 193 files, copy 193; `run\plugins\Rpg` 181 files, copy 181; his player `.dat`
   (`world\players\data\<uuid>.dat`) 5,490 bytes, copy 5,490. **His accessory file, before the boot: 95 bytes**, SHA-256
   `FF48C3A3…573C4EF2`, `{"schemaVersion": 1, "playerId": "b6ae27e9-…", "entries": []}` (four empty slots). **His
   profile: `archetypeId: "ranger"`**, `elementId: "fire"`, `lifetimeXp: 712849`.
5. **The world's saved difficulty:** `difficulty_settings.difficulty = normal` (decompressed `level.dat`), before and after.
6. **Config line 12:** `  dev-pack-on-join: true`. No `run\rpgfight`.
7. **"BOOTING NOW" was sent in its own message, before the boot command**, which went in the next turn after Ben's "go
   ahead".
8. **The marker** was written in the boot command's own call: marker 21:11:40; deployed jar 21:12:05.93; `latest.log`'s
   first line `[21:12:06]`; `Done (5.548s)` at `[21:12:11]`.
9. **"R0 PASSED" was sent in its own message** after R0 was read (21:12:11 to 21:12:43; `joined the game` 0 at 21:12:43).
   **Ben joined at 21:34:31**, after R0. He typed the command at 21:34:37 (`RUN START … rows=57 selftest=1f447197`);
   `RUN FINISHED` at 21:41:34; `/stop` at 21:47:21; the clean-stop lines through `All RegionFile I/O tasks to complete`;
   no `java` process left.
10. **His files after `/stop`**: see H15 below. They differ from step 4, and the difference is his own play AFTER the run.

### R0, BEFORE ANY LOGIN — ALL AS PREDICTED

- **R0a:** `[Rpg] Build: 1f447197`.
- **R0b**, the DEPLOYED jar (21:12:05): `GATE-blink-tome-r0b.ps1` **27 PRESENT / 0 ABSENT** (9 Entry, 6 Has, 12 ValueLine),
  **4 ABSENT / 0 PRESENT**, controls **2 PRESENT / 0 ABSENT**; `GATE-healing-tome-r0b.ps1` **25 / 0, 4 / 0, 2 / 0**;
  `GATE-bone-club-r0b.ps1` **21 / 0, 5 / 0, 2 / 0**; `GATE-blast-fungus-r0b.ps1` **18 / 0, 4 / 0, 2 / 0**.
- **R0c:** `Loaded 11 abilities, 47 visuals, 5 statuses, 7 elements, 27 enchants, 3 pools, 6 fragments, 6 aspects, 27
  weapons, 1 shields, 28 armor, 5 tools, 8 accessories, 7 mobs, …`; `Mint-on-craft: 42 indexed, 42 claiming, of 61 gear
  definitions`; **0** `WARN]: [Rpg]` (control: `INFO]: [Rpg] Loaded` 7); `Refusing` 0, `Skipping` 0, `SEVERE` 0,
  `Exception` 0.
- **R0d:** exactly one `Selftest: dev server, 530 scenarios across 55 gates [melee-cell, …, blast-fungus, bone-club,
  healing-tome, blink-tome]`; `519 scenarios` 0, `54 gates` 0.
- **R0e:** one `FIGHTWORLD sweep path=…\rpgfight folders=0`; `deleting` 0, `orphan` 0 (so no `FUSED removed …
  cause=orphan`).

### SAFETY — 19.000, READ

**Every `HURT` line of the whole run:** HT2 `before=7.000 after=7.000`, HT3 `before=8.000 after=8.000`, HT4
`before=2.000 after=2.000`, HT6 `before=2.000 after=2.000`, each `defense=0.000 dealer=self dealerType=self`. **The
sum: 7 + 8 + 2 + 2 = 19.000**, the prediction, at or under 20.000. **No `HURT` line in any blink row** (no breach), none in
the Bone Club, Blast Fungus or TT rows. BF5's two `SELF` lines both read `customHP=100.000/100.000`.

### H15 AND K9 — HIS CLASS, SLOTS, ARMOUR, OFF HAND AND INVISIBILITY

**CAPTURED, whole:**
`SELFTEST RUN CAPTURED armour=0 offhand=0 class=ranger accessories=[-,-,-,-]`

**RESTORED, whole:**
`SELFTEST RUN RESTORED xp=712849 cell=ranger/fire gamemode=SURVIVAL held=3 mana=100.0 trace=false spawn_mobs=false
time=1000 blocks=1 blocksBack=0/0 mobsRemoved=5 orbsRemoved=0 keep_inventory=false immediate_respawn=false
difficulty=NORMAL health=112.000 vanilla_xp=135+0.874 spawn_phantoms=true max_summons_base=null saturation=0.0
accessories=[-,-,-,-] armour=0 offhand=0 summons=0 fused=0 blink=none drag_tick=false`

- **His class came back:** `cell=ranger/fire`, the CAPTURED `class=ranger`. **His four slots came back:**
  `accessories=[-,-,-,-]`, equal to CAPTURED. **His armour and off hand:** `armour=0 offhand=0`, as captured. **No
  `NOT-RESTORED=` anywhere in the log** (0). `xp=712849`, his pre-boot `lifetimeXp`. The run-end fields end **`… summons=0
  fused=0 blink=none drag_tick=false`**, as predicted. **H15: READ AS PREDICTED. K9 (no blink state outlived the run):
  READ AS PREDICTED** (`blink=none`; the last blink, BT10's, ended `reason=cleared` at 35183, the path BT2 shows taking the
  effect off: `INVIS present=false`). No INVIS reading follows BT10's END. After the run a Knell's beam chose him (`BEAM
  telegraph … target=player <his uuid>`), which `MobBeamer.eligible` refuses for a blinked player (K7 (3)): a witness
  to the blinked state being over, not to the potion effect.
- **Not readable from the log:** `saturation=0.0` and `max_summons_base=null` against CAPTURED, which prints neither (the
  RESTORED line prints what the snapshot put back).
- **HIS FILES AFTER `/stop` DIFFER FROM THE PRE-BOOT READING, AND IT IS HIS OWN PLAY AFTER `RUN FINISHED`, NOT THE
  RESTORE.** After `/stop`: the accessory file **582 bytes** (SHA-256 `c8bc8b80…182eeb42`), one entry, slot 0, which
  decodes to a `healing_tome` (`rpg:accessory_id healing_tome`); the profile **`archetypeId: "mage"`**, `lifetimeXp:
  712599`. Between `RUN FINISHED` (21:41:34) and `/stop` (21:47:21) the log has his own commands: `/rpg give blast_fungus`,
  `/rpg give blink_tome`, `/rpg give healing_tome` (21:41:56 to 21:42:03), eight `/rpg spawn knell`, `/rpg mana refill`,
  `/rpg give flint_staff`, `/rpg playerxp set BaronVonYeetus 1 level` (21:44:49) and `… 50 level` (21:45:24). The class
  change and the equip are not logged (INFERRED: the Build and accessory screens). **His files were not touched.** The
  pre-run files are in the step 4 backup.

### THE BLINK TOME — BT1 TO BT11

Stand block (sx, 200, sz) = (104, 200, −4) (`PAD … stand=104,201,-4`), so from= is `124.500 201.000 -3.500` in every row;
the trigger `bt` at 127.5, the blockers at (121.5, 9.5), (127.5, 9.5), (121.5, −16.5), (127.5, −16.5), BT9's `bf6` at
130.5.

**Every BLINK line of the run, with E, T0, M and B, and to= against the box (x − sx in [4.5, 36.5], z − sz in [−12.5,
13.5]), the 16 and the 5 (EXECUTED over each line by script):**

| row | E | T0 | M | B | to= | pad (x−sx, z−sz) | from= to to= | nearest mob | safe / forgot |
|---|---|---|---|---|---|---|---|---|---|
| BT2 | 33942 | 33943 | 33952 | 33953 | `114.500 201.000 -5.500` | (10.5, −1.5) in | 10.198 | 13.038 | 508 / 1 |
| BT3 | 34010 | 34013 | 34020 | 34023 | `135.500 201.000 -9.500` | (31.5, −5.5) in | 12.530 | 10.000 | 508 / 1 |
| BT4 | 34258 | 34263 | 34268 | 34268 | `131.500 201.000 0.500` | (27.5, 4.5) in | 8.062 | 5.657 | 508 / 1 |
| BT5 | 34338 | 34343 | 34348 | 34348 | `110.500 201.000 -3.500` | (6.5, 0.5) in | 14.000 | 17.000 | 508 / 1 |
| BT6 | 34418 | 34423 | 34428 | 34428 | `128.500 201.000 4.500` | (24.5, 8.5) in | 8.944 | 5.099 | 508 / 1 |
| BT7 | 34653 | 34658 | 34663 | 34663 | `133.500 201.000 9.500` | (29.5, 13.5) in | 15.811 | 6.000 | 508 / 1 |
| BT8 | 34947 | 34948 | 34987 | 34988 | `117.500 201.000 -11.500` | (13.5, −7.5) in | 10.630 | 6.403 | 508 / 1 |
| BT9 | 35043 | 35048 | 35083 (M2) | 35083 | `136.500 201.000 4.500` | (32.5, 8.5) in | 14.422 | 10.000 | 479 / 2 |
| BT10 | 35139 | 35173 (C) | 35139 | 35178 | `139.500 201.000 -5.500` | (35.5, −1.5) in | 15.133 | 12.166 | 508 / 1 |

Every one: `trigger=zombie <bt's uuid> dist=3.000`, T0 ∈ [E, E + 5] (BT10: [C, C + 5]), B ∈ [M, M + 5], to= a block centre
at y 201.000, within 16 of from=, more than 5 from every mob. Each BLINK line has a `BLINK feedback … at=124.50 201.00
-3.50 particle=poofx12 sound=entity.enderman.teleport` before it and one `at=<to>` after it.

**Two more BLINK lines, NOT from the stand and not predicted** (each after its row's last `clear_state`, see BT3 and BT5):
`BLINK … tick=34213 trigger=zombie <bh's uuid> dist=0.000 from=135.500 201.000 -9.500 to=147.500 201.000 -14.500 safe=394
forgot=1` and `BLINK … tick=34373 trigger=zombie <bm's uuid> dist=0.000 from=110.500 201.000 -3.500 to=102.500 201.000
-15.500 safe=441 forgot=1`. Both land at y 201.000 on a block centre within 16 (13.000, 14.422), outside the preflighted
box (pad (43.5, −10.5) and (−1.5, −11.5)) but on the pad's interior (x −2..45, z −13..13, `PAD … interior=-2..45 east
-13..13 south ok=7250`): **floored, no fall, no HURT.**

**Every INVIS line:** BT2 `present=true ticks=153 …blinked=true` (at M + 8 = 33960: 160 − (33960 − 33953) = 153) and
`present=false ticks=0 …blinked=false`; BT3 `present=true ticks=155` (at R = 34028: 160 − 5 = 155) and `present=false`;
BT4 `present=true ticks=152`, `present=false`; BT5 `present=true ticks=148`, `present=false`; BT6 `present=true
ticks=143`, `present=false`. Each `amp=0 particles=false`.

**Every BLINK START / END / STOP / REFUSED-TARGET line,** with its tick: START at each T0 above (and BT7's second at
34873); END `reason=cleared` 33962 (BT2), `expired` 34183 (BT3), `cleared` 34214 (BT4's CLEAN), `cast` 34280 (BT4),
`melee` 34360 (BT5), `cleared` 34374 (BT6's CLEAN), `expired` 34588 (BT6), `expired` 34823 (BT7), `cleared` 34995 (BT8),
`cleared` 35091 (BT9), `cleared` 35183 (BT10); STOP `reason=ended` at the start of BT3 to BT11 (33973, 34218, 34298, 34378,
34613, 34908, 35003, 35098, 35193: the previous row's tome, cleared by `accessory: clear`) and BT7's mid-row 34863;
REFUSED-TARGET `mob=zombie <bh's uuid> tick=34028` (BT3), once. **No `BLINK NONE-SAFE`, no `BLINK KEPT`.** His `SELF`
after BT2's blink: `at=114.50 201.00 -5.50 onGround=true fall=0.000`, to= to two places.

| row | class | the reading |
|---|---|---|
| BT1 | **MISS** (the name's style only) | `HELDID slot=0 item=blink_tome`; the lore **whole, as predicted**; A4's three REPLYs as predicted; no BLINK line. **The name reads `name="Blink Tome" [white, blue] italic`, predicted `[blue]`**: the same `[white, <colour>] italic` shape every HELD line of the run has (`Bone Club [white] italic`, `Blast Fungus [white, green] italic`, …), which `BlinkTomeGateArithmeticTest.theHeldLine` did not produce. The cause is not read here. |
| BT2 | READ AS PREDICTED | as the table; `SELF` before `at=124.50 201.00 -3.50`, after at to=; `TARGET … target=none` before and after; INVIS 153; the sheet `REPLY "Accessories" [gold]`, `REPLY "  Blink Tome: Blink" [gray]`; `BLINK END … tick=33962 reason=cleared`, `STAGE clear_state … blink=ended`, `INVIS present=false`. |
| BT3 | **MISS** (item 7) | Items 1 to 6 as predicted: `BLINK REFUSED-TARGET … tick=34028` then `DRIVE mob_target <bh> tag=bh refused tick=34028`; `TARGET tag=bh … target=none`; INVIS 155; **`BLINK END … tick=34183 reason=expired` = B + 160 exactly**; `INVIS present=false`; `DRIVE mob_target <bh> tag=bh tick=34188` (R + 160, no `refused`); `TARGET tag=bh … target=self`. **Item 7, "no second `BLINK wearer=` line in the row", did not hold:** the checks at 34193, 34198, 34203 and 34208 wrote none (the cooldown held with `bh` at 0.000 targeting him), then the row's last step, `clear_state`, cleared his cooldowns with the tome still equipped and `bh` still targeting him, and the next check blinked him (34213, before `DONE`). Its END came at 34214 inside BT4's CLEAN. |
| BT4 | READ AS PREDICTED (the row); its CLEAN, **MISS** | `INVIS present=true`; `DRIVE use … item=flint_staff yaw=-90.000 pitch=0.000 cadence=excluded`, then `BLINK END … tick=34280 reason=cast`, then `USE result …`; `INVIS present=false`; `clear_state … blink=none`. **Its row-start CLEAN reads `drum=0 blink=ended`, predicted `blink=none`**: BT3's extra blink. |
| BT5 | READ AS PREDICTED | `DRIVE attack Player#attack target=<bm> zombie d=0.000 charge=0.500 attack_damage=11.000`, no `NO_DAMAGE_EVENT`, then `BLINK END … tick=34360 reason=melee`; `INVIS present=false`; `clear_state … blink=none`. (The hit: `PLAYERHIT … wooden_sword/left_click … sent=0.400 crit=false`, not predicted.) **Then, before `DONE`, the second unpredicted blink (34373, trigger `bm`)**: `bm` was targeting him because the hit's aggro was set AFTER the state ended (the END is logged before the PLAYERHIT), and `clear_state` had cleared the cooldown. |
| BT6 | READ AS PREDICTED (the row); its CLEAN, **MISS** | No `BLINK END` between `DRIVE use … item=bone_whistle …` and `USE result`; `SUMMONPROBE weapon=bone_whistle … alive=true`; `INVIS present=true ticks=143`; `BLINK END … tick=34588 reason=expired` (= B + 160); `INVIS present=false`; `STAGE clear_state … summons=1 … blink=none`. **Its row-start CLEAN reads `blink=ended`**: BT5's extra blink. |
| BT7 | READ AS PREDICTED | The control blink (508 / 1); `BLINK END … tick=34823 reason=expired` (B + 160); `DRIVE mob_target <bk2> tag=bk2 tick=34831` (M + 168, no `refused`); no `BLINK wearer=` across the next 30 ticks; `TARGET tag=bk2 … target=self`; `accessory: clear` → `BLINK STOP … tick=34863 reason=ended`; `equip` at E' = 34869 → `BLINK START … tick=34873`; no `BLINK wearer=`; `TARGET … target=self`. **Exactly one `BLINK wearer=` line in the row.** |
| BT8 | READ AS PREDICTED | No `BLINK wearer=` from E (34947) to M (34987 = E + 40); `TARGET tag=bt … target=none`; the control blink at 34988. |
| BT9 | READ AS PREDICTED | `DRIVE mob_target <bf6> tag=bf6 tick=35053` (M1); no `BLINK wearer=` to M1 + 30; `TARGET tag=bf6 … target=self`; `DRIVE mob_target <bt> tag=bt tick=35083` (M2), the blink at 35083, `dist=3.000 … safe=479 forgot=2`. |
| BT10 | READ AS PREDICTED | `STAGE accessory equip … tick=35139`, `STAGE class found=mage set=ranger` and `DRIVE mob_target … tick=35139` in the same tick; no BLINK line of any kind to E + 30; `REPLY "  Blink Tome: inactive (requires Mage)" [dark_gray]`; `class: mage`, `BLINK START … tick=35173`, the blink at **35178 = T0 + 5**; `clear_state … blink=ended`. |
| BT11 | READ AS PREDICTED | `HELDID slot=0 item=blink_tome`; `DRIVE mob_target … tick=35231`; no BLINK line of any kind across the 30 ticks after it (the row's one BLINK line, `STOP … tick=35193 reason=ended`, is BT10's tome cleared at the row's start, before M). |

**The clause "every row-start CLEAN line now ends ` drum=0 blink=none`": MISS on two lines (BT4, BT6), `blink=ended`;
the other nine read `blink=none`.** Both trace to one cause: a row's last `clear_state` clears his cooldowns while the tome
is still equipped and a staged zombie still targets him at 0.000, so the 5-tick check can blink him again before the row
ends. Only the selftest's own clear can produce this (in play the cooldown is 1200 ticks). **Recorded for the seat; no
code or row is changed here.**

### THE HEALING TOME — HT1 TO HT8

**REGEN read `rate=0.200 saturation=0.0` on every REGEN line of every row**, so no figure is restated.

| row | class | the reading |
|---|---|---|
| HT1 | **MISS** (the name's style only) | `HELDID slot=0 item=healing_tome`; the lore **whole, as predicted** (`Radiant Pulse \| Every 4s, heals hurt players within 8 blocks \| for 1 health per your level \| Mage class accessory \| \| Rare Accessory`); the three A4 REPLYs as predicted; no TOMEPULSE line. **The name reads `[white, blue] italic`, predicted `[blue]`** (BT1's shape; the gate marked `effectiveName` INFERRED). |
| HT2 | READ AS PREDICTED | E 32088, `TOMEPULSE START … tick=32093 interval=80`; HURT 7; **n1 = 4**: pulse 1 `level=2 tick=32173 candidates=1 healed=1`, `HEALED amount=2.000 after=100.800/105.000 dealer=self`, `TOMEPULSE target=BaronVonYeetus before=98.800 amount=2.000 after=100.800`, the feedback line; **n2 = 8**: pulse 2 `tick=32253 … healed=1`, `before=101.600 amount=2.000 after=103.600`, feedback; `TOMEPULSE STOP … tick=32268 reason=inactive` (≤ E + 181); REGEN `customHP=103.800/105.000` (n = 9), then `105.000/105.000`. |
| HT3 | READ AS PREDICTED | E 32322, T0 32323; HURT 8; n1 = 4: `level=3 tick=32403 … healed=1`, `before=102.800 amount=3.000 after=105.800`; n2 = 8: `tick=32483`, `before=106.600 amount=3.000 after=109.600`; STOP 32498; ends `110.000/110.000`. |
| HT4 | READ AS PREDICTED | E 32556, T0 32558; HURT 2; **n1 = 3**: `level=3 tick=32638 candidates=1 healed=1`, `HEALED amount=1.400 after=110.000/110.000`, `before=108.600 amount=1.400 after=110.000`; pulse 2 `tick=32718 candidates=1 healed=0` and no target, HEALED or feedback line after it. |
| HT5 | READ AS PREDICTED | T0 32793 (E 32790); `level=2 tick=32873 … healed=0` and `tick=32953 … healed=0`; no target, feedback or HEALED line after the heal's own. |
| HT6 | READ AS PREDICTED | E 33016, `class found=mage set=ranger` and HURT 2 in the same tick; no TOMEPULSE line until the START at 33188; nine 0.200 payments in the window (103.200 to 104.800, the wound open throughout); the sheet `REPLY "  Healing Tome: inactive (requires Mage)" [dark_gray]` (the REPLYs of the one `rpg stats` before the class change are logged in two batches, 21:39:42 and 21:39:51; P-R1 gives all of them to that command); `class: mage` → `TOMEPULSE START … tick=33188`; `REPLY "  Healing Tome: Radiant Pulse" [gray]`; one pulse `tick=33268 … healed=0`; STOP 33278, before T0 + 160. |
| HT7 | READ AS PREDICTED | E1 33334, T0 33338; pulse 33418; STOP 33438 (≤ E1 + 105); E2 33466 (E1 + 132), T0' 33468; its first pulse **33548 = T0' + 80**; nothing at T0 + 160 (33498); STOP 33568. **Two pulse lines.** |
| HT8 | READ AS PREDICTED | `HELDID slot=0 item=healing_tome`; no TOMEPULSE line in the row. |

### THE BONE CLUB AND DRUMMING BEAT — BC1 TO BC6, DB1 TO DB8

**Every `DRIVE reset_charge` line of the run reads `cooldown=0.0200->0.0200`** (the iron sword's `0.0400->0.0400`),
**predicted `1.0000->0.0200` after a full hit and `0.6200->0.0200` after DB6's third: a MISS on that field, every line.**
The charge was already reset when the driver ran, so `Player#attack` DID reset the attack-strength ticker, against this
gate's READ ("`Player.attack` NEVER resets the ticker"). Our own `RpgListeners` records the same as a 2026-08-28
measurement (`onPrePlayerAttack`'s javadoc: *"vanilla calls `resetAttackStrengthTicker()` before `hurt()`"*). **It moves no
other reading:** the charge each hit was stashed with (`DRIVE attack … charge=`, read before the attack) is `1.000` on
every full hit and **`0.620` on DB6's third**, as predicted, so no row is hollow. `reset_charge` was redundant, not wrong.

`rpg stats` read **B = 15** (`  Crit Chance  15%`) in BC4 and every DB row.

| row | class | the reading |
|---|---|---|
| BC1 | READ AS PREDICTED | `Slot 0: [drumming_beat locked]`, `Slot 1: [kinetic_tremors locked]`, `Bone Club -- 4 slot(s)`; `HELD bone_club BONE x1 name="Bone Club" [white] italic`, lore `Kinetic \| Gear Score: 100 \| \| Melee Damage: 44 \| Attack Speed: 0.8 \| \| Common Melee Weapon`. |
| BC2 | READ AS PREDICTED | `ATTRIBUTE attack_speed value=1.600 base=4.000`, then `value=0.800 base=4.000`. |
| BC3 | READ AS PREDICTED | both `ROLLSTATS` headers as predicted; the club slot 0 `1=500`, slot 1 `1=500`; the sword slot 0 `0=500`, slot 1 `1=500`. |
| BC4 | READ AS PREDICTED | two PROBEs `1000.000` each time; at 100 `sent=44.000 crit=false triggerScore=100`, `STREAK n=0 step=0 chance=0.150 charged=true result=plain`, then `956.000`; at 200 `sent=88.000 crit=false triggerScore=200`, the same STREAK, then `912.000`. |
| BC5 | READ AS PREDICTED | the club: `u` `1000.000` three times, `t` `sent=44.000`, one `STREAK … plain`; the sword: `u` `1000.000` twice then **`977.000`**, `t` `sent=46.000 crit=true` (the allowed reading), no STREAK. `u` lost 23.000, half of `t`'s 46.000. The control fell: not VOID. |
| DB1 | READ AS PREDICTED | `  Slot 0: [*drumming_beat III]`, `  active: drumming_beat III (+5% Crit Chance per consecutive hit)`; lore `Drumming Beat III \| \| Kinetic \| Gear Score: 100 \| \| Melee Damage: 44 \| Attack Speed: 0.8 \| \| Common Melee Weapon`. |
| DB2 | READ AS PREDICTED | STREAK `n=0 … 0.150`, `n=1 … 0.200`, `n=2 … 0.250`, `n=3 … 0.300`, `n=4 … 0.350`, all `counted`. **Hits 1, 3 and 4 crit** (`sent=88.000 crit=true`); **the next STREAK reads `n=1`, `n=3`, `n=4`**: a crit did not reset. PROBE `648.000`. DB3's CLEAN `drum=5`. |
| DB3 | READ AS PREDICTED | `n=0 step=3 chance=0.150`, `n=1 step=3 chance=0.180`; CLEAN `drum=5`; next `drum=2`. |
| DB4 | READ AS PREDICTED | `n=0 step=4 chance=0.150`, `n=1 step=4 chance=0.190`; CLEAN `drum=2`; next `drum=2`. |
| DB5 | READ AS PREDICTED | `n=0 … counted`, `n=1 … chance=0.200 … counted` (gap 100), `n=0 step=5 chance=0.150 charged=true result=reset:gap` (101); CLEAN `drum=2`; next `drum=1`. |
| DB6 | READ AS PREDICTED | charges `1.000`, `1.000`, **`0.620`**, `1.000`; the third `STREAK n=2 step=5 chance=0.150 charged=false result=reset:uncharged` with `sent=44.662 crit=true` (the allowed reading); the fourth `n=0 … 0.150 … counted`. CLEAN `drum=1`; next `drum=1`. |
| DB7 | READ AS PREDICTED | `n=0`, `n=1`; `hold: iron_sword`, `hold: bone_club`, `HELDID slot=1 item=bone_club`; `STREAK n=0 step=5 chance=0.150 charged=true result=counted`. CLEAN `drum=1`; next `drum=1`. |
| DB8 | READ AS PREDICTED | PROBE `2000.000/2000.000`; hits 1 to 17 `n=0 … n=16`, chances 0.150 to **0.950** in steps of 0.050, all `counted`; **hit 18 `STREAK n=17 step=5 chance=1.000 charged=true result=counted`, `sent=88.000 crit=true triggerScore=100`; hit 19 `n=18 … 1.050`, crit; hit 20 `n=19 … 1.100`, crit.** Every PLAYERHIT: hits 6 and 13 to 20 `sent=88.000 crit=true`, the other eleven `sent=44.000 crit=false`; 11 × 44 + 9 × 88 = 1276.000 = 2000 − **724.000** (the PROBE after). Every STREAK follows its own PLAYERHIT. BC6's CLEAN `drum=20`. |
| BC6 | READ AS PREDICTED (AUTO-PARTIAL, as written) | **`DRIVE interact_entity PlayerInteractAtEntityEvent HAND item=bone_club …`** → `INTERACT result cancelled=true tamed=false held=bone_club amount=1`; **`item=bone_whistle`** → `cancelled=true … held=bone_whistle`; **`item=bone`** → `cancelled=false tamed=false held=bone amount=1`, then `ITEMS material=bone count=1 gear=0 slots=[0:1]`. **The control's item is a plain bone** (`item=bone`, `gear=0`): the control is valid. The real click stays NOT READ (owed, as the row says). |

### THE BLAST FUNGUS GATE — BF1 TO BF9, FF1 TO FF3, CB1 TO CB4

F = `104.500 201.000 -3.500` (`PAD stood at=104.50 201.00 -3.50`). Every `FUSED throw` at F+(0, 1.620, 0) =
`104.500,202.620,-3.500` except BF6's (below). Every row-start CLEAN `fused=0 … drum=0 blink=none`; RESTORED `fused=0`.

| row | class | the reading |
|---|---|---|
| BF1 | READ AS PREDICTED | `Slot 0: [fast_and_fungal locked, cluster_bombs locked]`, `Slot 1: []`; `HELD blast_fungus RED_MUSHROOM x1 name="Blast Fungus" [white, green] italic`, the lore whole as predicted. |
| BF2 | READ AS PREDICTED | `ROLLSTATS blast_fungus element=nature rolls=500 weaponPool=2 uniquePool=0 normalPool=2`; slot 0 `2=500`; slot 1 `0=500`; slots 2 and 3 `1=84 2=416`, `1=85 2=415`. |
| BF3 | **MISS** (the ITEM samples' count) | MANA `100.000` → `70.000`; `USE result useItemInHand=DENY useInteractedBlock=DENY`; three throws T = 29284; three rests **29293 (T + 9)** at `111.150,201.000,-3.500`, `110.527,201.000,-0.690`, `110.527,201.000,-6.310`; three `detonate … cause=fuse` at **29324 (T + 40)**; `ba` three `sent=55.000`, **835.000**; `bo` one, **945.000**; `bn` none, **1000.000**. **The three samples each read `ITEM n=7`, predicted three**: the three rest points (`6.65 0.00 0.00; 6.03 0.00 2.81; 6.03 0.00 -2.81`) are there, identical across the samples, beside **four other items** at F+(1.98, 0, −0.64), (2.87, 0, 0.70), (2.17, 0, 0.30), (10.59, 0, −0.07), the same in every sample. **The fourth sample reads `ITEM n=4` (those four), predicted no ITEM line.** They are not chunks: the store read `fused=0` at every CLEAN and no `FUSED removed … cause=orphan` was written. What they are is not read; INFERRED, the drops of the zombies the Bone Club rows killed with `/kill @e[tag=t]` at block (2, 0) (the fourth, near x 10.6, unexplained). |
| BF4 | READ AS PREDICTED | `detonate id=4 at=108.200,202.163,-3.500 cause=touch tick=29384` (T + 5, F+(3.700, 1.163, 0)); the sides rest at 29388 and go off at 29419 (T + 40); PROBE **945.000**, then **835.000**. |
| BF5 | READ AS PREDICTED | pitch 90; rests at **29476 (T + 2)** at `104.500,201.000,-3.500`, `-2.779`, `-4.221` (F + (0, 0, ±0.721)); `detonate … cause=fuse` at 29514; no HURT, no PLAYERHIT; both SELF `100.000/100.000`. |
| BF6 | READ AS PREDICTED (the readings); the rest points, **MISS** at 0.002 | The wolf `SUMMONPROBE … at=103.50 201.00 -3.50 … health=100.000/100.000` before and after; no PLAYERHIT names it; `bs` three `sent=55.000`, **835.000**. **The throw and rest points read x `104.529`**, not F's `104.500` (rests `104.529,201.000,-3.500`, `-2.779`, `-4.221`): his feet were 0.029 east of F at the press, so BF5's points are offset by 0.029, outside 0.002 against F and exact against his own throw point. |
| BF7 | READ AS PREDICTED | throws at 29645; a press `REPLY "On cooldown for 0.2s"` with no throw; throws at **29785 (+ 140)**. Six throws. The row has no MANA line, so "no mana spent" at the refusal is NOT READ. |
| FF1 | READ AS PREDICTED | `[*fast_and_fungal I]`, `active: fast_and_fungal I (-1s cooldown)`; `Cooldown: 6.0s \| Mana Cost: 30`, the lore whole; refused `On cooldown for 0.2s`; throws 29850 → **29970 (+ 120)**. |
| FF2 | READ AS PREDICTED | `… II (-2s cooldown)`; `Cooldown: 5.0s`; throws 30035 → **30135 (+ 100)**. |
| FF3 | READ AS PREDICTED | `… III (-3s cooldown)`; `Cooldown: 4.0s`, the lore whole as quoted; throws 30200 → **30280 (+ 80)**. |
| CB1 | READ AS PREDICTED | `active: cluster_bombs I (throws one big chunk of 55 damage in 5 blocks that splits into 3 of the usual)`; one throw 30375; rest 30384 at `111.150,201.000,-3.500`; detonate 30415; `SPLIT … at=111.150,201.000,-3.500 bursts=3 tick=30415`; three throws there at 30415; rests **30428 (T + 53)** at `113.650,201.000,-3.500`, `109.900,201.000,-5.665`, `109.900,201.000,-1.335`; detonate **30435 (T + 60)**; `c4` 945.000 → 890.000, `c5` 1000.000 both, `cs` 945.000 → 890.000; big `sent=55.000`, splits `sent=55.000`. |
| CB2 | READ AS PREDICTED | big `sent=80.000`; `c4` 920.000 → 865.000, `c5` 1000.000, `cs` 920.000 → 865.000; the block whole as quoted. |
| CB3 | READ AS PREDICTED | big `sent=105.000`; 895.000 → 840.000 (`c4`, `cs`), `c5` 1000.000. |
| CB4 | READ AS PREDICTED | `Gear Score: 200`, `Magic Damage: 160`, `Magic Damage: 110 x 3`; `cs` `sent=160.000 … triggerScore=200` → **840.000**, `sent=110.000` → **730.000**. |
| BF8 | **MISS** | **`ENTITIES ITEM count=4 radius=32`, predicted `count=0`**: BF3's four stray items. `STAGE clear_state … fused=0`, every CLEAN `fused=0`, RESTORED `fused=0`, and no `FUSED removed … cause=orphan`: no fused body was left, but this row's instrument counts every item and could not show it. |
| BF9 | READ AS PREDICTED | before: `POTTED_RED_MUSHROOM count=0`, `FLOWER_POT count=1`, `INVENTORY blast_fungus slots=[0x1]`; his first click cast (`FUSED throw` 53 to 55, rested by the pot, `cause=fuse`); **Ben, verbatim: "yes"** (`<BaronVonYeetus> yes`, 21:38:16; `answer="yes" ticks=469`); after: `POTTED_RED_MUSHROOM count=1`, `FLOWER_POT count=0`, `INVENTORY blast_fungus slots=[40x1]` (his off hand). |

### TT20 TO TT27 — READ AS PREDICTED

TT20, TT26 and TT27's dumps: `Large Sparks III \| \| Fire \| Gear Score: 100 \| \| Fire Bolt  Right-Click \| Hurls a fragment
of burning mana \| Magic Damage: 32 \| Cooldown: 1.2s \| Mana Cost: 5 \| \| Early magicians learned you could \| create fire
by striking flint with mana. \| \| Common Magic Weapon`. TT21 bare `Magic Damage: 27 x 3`, then Focused Beam II `Channel,
then loose one focused \| beam down your line of sight. \| Magic Damage: 162`. TT22 `Ranged Damage: 85 \| Ranged Damage: 40
x 4` (quiver `2/2`). TT23 `Magic Damage: 21` and `Magic Damage: 14.7`. TT24 `Melee Damage: 23 \| Attack Speed: 1.6`. TT25
`Ranged Damage: 16 \| Attack Speed: 1.7` (quiver `8/8`). **No element-labelled damage line anywhere in the log** (`Fire
Damage`, `Kinetic Damage`, `Void Damage`, `Nature Damage`: 0). TT26: PROBE `1000.000`, one `PLAYERHIT
source=flint_staff/right_click … element=fire sent=32.000 crit=false triggerScore=100`, PROBE after **936.000**. **TT27,
Ben, verbatim: "yes"** (`<BaronVonYeetus> yes`, 21:38:48; `answer="yes" ticks=385`).

### SUMMARY

- **MISS:** BT1 and HT1 (the name's style), BT3 item 7, the row-start CLEAN clause on BT4 and BT6, every `reset_charge`
  line's before-value (GATE-bone-club), BF3's ITEM counts, BF6's rest points at 0.002, BF8.
- **DID NOT TEST WHAT IT WAS MEANT TO:** none.
- **Everything else READ AS PREDICTED.** SAFETY 19.000 read. H15 and K9 read as predicted.
- **NOT READ, as each gate says:** a live mob losing interest, the vanilla target event's refusal, his look while
  invisible, the feedback's look and sound, the dragon, a second player (both tomes), BC6's real click, BF7's mana at the
  refusal.
- **For the seat (not ruled here):** the extra blink after a row's last `clear_state` (BT3, BT5); `Player#attack` resets the
  charge (GATE-bone-club's READ and its `reset_charge` premise); stray items on the pad (BF3, BF8); the HELD name's
  `[white, …] italic` against two tome predictions.
- **Seen in passing:** the boot log says Paper `26.1.2` is "4 release(s) behind the latest stable release (26.3)" (the
  upgrade procedure's Step 0).
