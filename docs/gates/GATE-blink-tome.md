# GATE — THE BLINK TOME AND ITS BLINK (2026-11-26), WITH THE HEALING TOME GATE, THE BONE CLUB GATE, THE BLAST FUNGUS GATE AND TT20 TO TT27

**Status: NOT RUN.** The plan is `docs/plans/PLAN-blink-tome.md`. The scenario is
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
