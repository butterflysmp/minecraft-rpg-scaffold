# PLAN — THE ENCHANT COLUMNS RE-CUT, THE LIVING LANTERN REWORKED, AND BEN'S FIRST BEHAVIOURAL ENCHANTS

Written 2026-11-23 from the seat's brief of that day ("THREE ENCHANT COLUMNS' WORTH OF WORK"), stacked on
`feat/boltor-sound-flint-text` (#251), on #250, #249, #248, on master `383ded19`. Every server or code fact is marked READ
(our source, at the commit named), EXECUTED, or INFERRED. **One gate and ONE R0 for the whole stack from #248 up; no boot
until Ben says "ready" and the seat approves code and gate from origin.** Each PR appends its own section below.

## 1. PR A — THE COLUMNS RE-CUT (`feat/enchant-columns`)

### Ben's words, verbatim, 2026-11-23

> "I want to change enchantments a little more. The first column will be weapon specific enchantments that
> fundamentally change how the weapons work. The second column will be our unique enchantments we just made and the 3rd
> and 4th columns will be the normal enchantments"

Asked whether normal drops from three slots to two (A) or the table grows to five columns (B): **"1. Three back to 2
(A)"**. Shown the seat's assumptions (a weapon with none designed has an empty first column; armor, shields and tools have
an empty first column; the first column offers 1 to 4 by the same cascade; a weapon-specific enchant belongs to one named
weapon, not a family): **"2. Yes"**. **"We can remove the placeholder names/labels above the columns."**

### The seat's rulings, as built

- **W1.** WEAPON-SPECIFIC when the file declares `weapon: <weapon id>` (`EnchantLoader`'s `weapon` section). Refused by
  name: an id that is not a loaded WEAPON (a shield id included), and both `weapon:` and `element:` (the record,
  `EnchantDefinition`, owns that refusal; the loader does not repeat it). **One refusal the brief did not name, stated so
  it is not missed:** a `class:` other than `universal` that differs from the named weapon's class is refused too,
  because such an enchant could never roll (the weapon pool applies the class filter, as the unique pool does).
- **W2.** Four slots. `EnchantRoll.kindOf` is the one place that says what each index holds: **0 WEAPON, 1 UNIQUE, 2 and
  3 NORMAL**. `weaponPoolFor(gearId, …)` filters by the enchant's `weapon` equal to the gear's id, then class, then
  `requires`. `normalPoolFor` now skips every non-NORMAL kind (it skipped only unique ones). The three pools are disjoint
  by construction (`EnchantRollWeaponTest.noPoolHoldsAnotherKind`).
- **W3.** The cells did not move (column 2 slot 0, 4 slot 1, 6 and 8 slots 2 and 3; `EnchantMenuLayout.rawSlotFor`
  unchanged). The "Unique" NAME_TAG at index 1 and its constant `UNIQUE_LABEL_SLOT` are REMOVED; index 1 is filler. The
  empty-slot marker stays, one per column that can be empty (`EnchantMenuLayout.emptyMarkerFor`), both SEAT PLACEHOLDERS.
- **W4.** `EnchantRoll.roll(String gearId, Optional<String> element, …)`: a null or blank id is refused
  (`IllegalArgumentException`), as a null element is. Over every shipped gear (`UniqueSlotContentTest`): each slot's
  candidates are only its own kind over 200 seeded rolls of each of the 60; the normal pool (slots 2 and 3) equals
  master's `383ded19` snapshot for all 60; the weapon pool is EMPTY for all 60 at this PR.
- **W5.** `dev_kinetic_example.yml` stays until the PR that lands Kinetic Tremors deletes it.

### What else moved, stated so the diff does not have to be read to learn it

- **The load order (READ, `RpgPlugin.onEnable`):** the enchants loaded right after the elements and BEFORE the weapons;
  they now load right after the weapons, because the loader asks the weapon registry. READ: nothing between the old and
  new position reads `enchants` (its next use is the `Loaded …` summary line). `EnchantLoadOrderTest` pins the order and
  the lookup from the source.
- **`/rpg enchant pool`** prints a THIRD line, `Weapon pool: <ids>` (gray), after `Pool:` and `Unique pool:`, whose
  wording is unchanged: `Pool:` is now slots 2-3's and `Unique pool:` slot 1's.
- **`/rpg enchant rollstats`**: the first line gains `weaponPool=<n>` before `uniquePool=`; the slot lines read
  `slot 0 (weapon):`, `slot 1 (unique):`, `slot 2:`, `slot 3:` (`EnchantRollItems.slotLabel`).
- **The dev command's `candidate` op still writes any enchant into any slot.** W2's "can never appear" binds the ROLL; the
  dev staging instrument is left able to stage a mixed column (the gate's full-table row depends on it). Not a shipped
  surface: `/rpg enchant` is dev-server only.
- **An item rolled under #249's shape (slot 0 unique, 1-3 normal)** exists only on the dev world, never merged: by Ben's
  V4 ("we don't care about existing items"), no migration. In the table its unique candidates sit in the weapon column
  and its first normal slot's in the unique column; nothing throws, dupes or charges differently (the same code path V4
  read for the three-slot shape).

### OPEN FOR BEN (built as SEAT PLACEHOLDER 2026-11-23)

1. **The empty weapon slot's marker:** a GRAY_DYE in column 2's first cell (index 10), named **`No weapon enchant`** (dark
   gray), lore **`Nothing is tied to this weapon yet.`** (dark gray).
2. **The empty unique slot's marker:** #249's, unchanged, now in column 4's first cell (index 12): **`No unique
   enchant`** / **`Nothing unique is tied to this item's element yet.`**

### Tests and mutations

New: `EnchantRollWeaponTest` (12 rows, `grep -c @Test`), `EnchantLoadOrderTest` (1); new rows in `EnchantLoaderTest` (2),
`EnchantMenuLayoutTest` (1), `UniqueSlotContentTest` (1); rewritten for the moved index: `EnchantRollTest`,
`EnchantRollUniqueTest`, `EnchantMenuLayoutTest`, `EnchantRollStatsTest`, `EnchantQuantityGateTest`,
`UniqueSlotContentTest`. Ten mutations, each a single-site literal replace with a marker, each printing `marker
present: 1  original gone: 0  line delta: 0`, each restored by `cp` and checked `cmp` byte-identical:

| mutation | the edit | reddened |
|---|---|---|
| MUTWPOOLID | weapon filter accepts any weapon enchant | 7 rows of `EnchantRollWeaponTest` |
| MUTNORMALKIND | normal pool skips only unique (the old line) | 4 rows, among them `noPoolHoldsAnotherKind` |
| MUTKINDOF | `kindOf(1)` answers NORMAL | 12 rows across the three roll tests |
| MUTGEARID | the gear-id refusal disabled | `aNullOrBlankGearIdIsRefused` |
| MUTBOTH | the Rollable's both-kinds refusal disabled | `aRollableCannotBeBothUniqueAndWeaponSpecific` |
| MUTLOADERWEAPON | the not-a-loaded-weapon refusal disabled | both new `EnchantLoaderTest` rows |
| MUTLOADERCLASS | the class-mismatch refusal disabled | the W1 row (`wrong_class`) |
| MUTRECORDBOTH | the record's both-keys refusal disabled | the W1 row (`both`) |
| MUTMARKER | the weapon marker removed | `theWeaponAndUniqueColumnsHaveEmptyMarkersAndTheNormalOnesDoNot` |
| MUTLOOKUP | the loader handed no weapon lookup | `EnchantLoadOrderTest` |

**UNMUTATED, said so:** the ORDER half of `EnchantLoadOrderTest` (moving a block is not a single-site replace); the
`Weapon pool:` line's presence is pinned by `EnchantQuantityGateTest`'s source read, not mutated.

## 2. PR B — THE LIVING LANTERN REWORKED (`feat/living-lantern-homing`; content, no enchant)

### Ben's words, verbatim, 2026-11-23

> "Instead of the cone of fire, I want it to shoot a slow moving homing projectile similar to the old repo, only this
> time 1 shot instead of three." / "1. Damage 45 mana 12 cooldown 0.8s" / "2. 7 blocks a second is fine ... Reach
> increased to 28 blocks for the normal projectile. ... Homing within 7 is fine."

### As built (`content/weapons/living_lantern.yml`, `right_click`)

`type: projectile`, ONE body: **45 fire (BEN)**, **12 mana (BEN)**, **`cooldown_ticks: 16` (BEN, 0.8 s, on the 4-tick
grid)**, **speed 0.35 (BEN, 7 / 20)**, **gravity 0**, **`max_lifetime_ticks: 80`**, **homing `search_radius: 7` (BEN)**,
**`lerp: 0.45` (SEAT PLACEHOLDER, the old repo's `LIVING_LANTERN_HOMING_TURN`, READ by the seat at `cfde822`)**,
`activation_blocks: 0` (PLACEHOLDER, below). Name "Lantern's Breath" kept; recipe, tier, rarity, class and the cast and
hit visuals unchanged.

**READ: how the engine caps travel.** By LIFETIME only: `ProjectileFlight.step` ends a flight on a hit, or when its
elapsed ticks reach `max_lifetime_ticks`, and goes off at that point (`onImpact.at(null, next)`; with no target the
Damage arm lands nothing). There is no distance cap. Homing keeps the velocity's LENGTH (`steer` rescales the turned
direction to the old speed), so travel is 0.35 a tick whatever it chases: **28 / 0.35 = 80 ticks; the last step lands at
80 x 0.35 = 28.0 blocks** (`LivingLanternContentTest.theReachIsTwentyEightBlocksOfTravel`, executed).

**THE CONE STAYS IN THE ENGINE AND NO SHIPPED WEAPON USES IT NOW** (`CastSpec.Cone`, `CastExecutor.beginCone` and their
tests untouched; `LivingLanternContentTest.noShippedWeaponCastsACone` walks every loaded trigger).

**FIRE'S BURN, EVERY TERM IT ADDS (READ, `fire.yml` `applies_status: scorch`; core `Scorch`):** the hit accrues scorch
(one stack per 2 damage landed, a yes/no gate); the burn deals `min(5% of the victim's max, cap)` every
`Scorch.PERIOD_TICKS` 20 for `Scorch.DEFAULT_DURATION_TICKS` 120 (the ability authors no duration); **cap = the hit x
`Scorch.CAP_FRACTION` 0.5 = 22.5** at score 100 (was 7 under the cone's 14). On a 100-max mob a burn is
`min(5.0, 22.5) = 5.0`. A scorched mob that dies Ignites (`Ignite.DAMAGE` 6, fire, every non-player within 4, 20 ticks
later). Nothing here is new; only the cap moved, by the hit's own number.

**THE FLIGHT, EXECUTED** (`core/.../LivingLanternFlightTest`, the real `CastExecutor` and `ProjectileFlight` on the fake
world, half-width 0.3): a mob 10 out and 3 off the aim line takes exactly ONE hit of 45 (health 100 -> 55); 8 off the line
it is never sought (100); dead ahead at 27 it is hit, at 29 it is not.

### OPEN FOR BEN (SEAT PLACEHOLDER 2026-11-23)

1. **The description:** `"Looses a slow, seeking flame"` / `"that hunts within 7 blocks."` (was the cone's two lines).
2. **The trail:** the cone's front visual `lantern_front` (the old FLAME and orange dust), reused; **no body** (no item,
   no display), so the shot is particles only.
3. **`activation_blocks: 0`:** it seeks from the muzzle, as the Crystal Shard Staff's does.

### FOR THE SEAT: AN OWED ROW THIS INVALIDATES

**`GATE-breacher-rework.md` LL9R2 (OWED, never run: that gate booted once and no row ran)** predicts the burn's cap on the
CONE: both zombies inside a 6-block, 60-degree cone, struck on the front's 4th tick, `sent=14.000`, `cap=7.000`. Under PR
B the same staging fires one homing shot that strikes ONE of them, for 45, cap 22.5. The row's purpose (PR 1 of that
stack: `DotStatus.apply` keeps the LARGER cap when a weaker Ignite burst lands) still stands; its staging and every
number do not. **Not edited** (another gate's row); the seat's to re-home or retire.

### Tests and mutations

`LivingLanternContentTest` rewritten for the shot (Ben's numbers, every placeholder, the tooltip, the reach, no cone
anywhere); `golden-lore.txt`'s three lantern lines; new `core/.../LivingLanternFlightTest` (3 rows). Five mutations,
each `marker present: 1  original gone: 0  line delta: 0`, each restored and `cmp` byte-identical: MUTSPEED (0.36: 3
rows red), MUTLIFE (81: 2), MUTSEARCH (8: 2), MUTDMG (44: 3), MUTNOSEEK (`ProjectileFlight.steer` never turns: the
off-the-line flight row red).

## 3. PR C — KINETIC TREMORS (`feat/kinetic-tremors`), AND THE BEHAVIOUR ENGINE IT NEEDS FIRST

### Ben's words, verbatim, 2026-11-23

> "The first: Kinetic Tremors  Any kinetic weapon from Melee, Ranger, and Mage.  multiple consecutive hits within 3
> seconds emits a damaging shockwave"

Asked how many hits; same target or any; where it comes from; whether the count resets; what levels improve: **"1. 3 hits
rolling 2. Same target 3. The mob that was hit 4. No 5. The damage and size"**. **"Extra notes: 1. Damage will scale with
the gear score 2. Multiple pellets count so one shot triggering it is fine"** / **"one more note, there is a .25s cooldown
on shockwaves"**.

### WHAT THIS PR IS BIGGER THAN IT READS BY, STATED FIRST

Kinetic Tremors' own hook is small (a primary-hit call in the two damage arms). **But B2 ("the weapon stamped on the
cast") had no carrier: `Caster` held no weapon and no enchant (READ).** So this PR also builds the behaviour engine that
Lapis Crystal and Airburst Flames stand on:

- **`Caster` gains two components.** `behaviours` (`List<ActiveBehaviour>`): the active behavioural enchants of the weapon
  that MADE the cast, frozen at `CastExecutor.commit` for a WEAPON trigger only (a stone cast carries none), at a basic
  swing (`landBasicMelee`), at a vanilla shot's release (the stamp, below), and per shot of a weapon-pressed volley.
  `countsAsHit`: false on an area's tick (`asNotAHit`) and on every secondary damage (`asSecondary`, which also drops the
  behaviours and sets the PLAYERHIT source).
- **`CombatWorld` gains three abstract methods:** `behavioursOf(id)` (read where `triggerScoreOf` is read),
  `primaryHit(caster, target)` (called by both damage arms inside the same gate as the PLAYERHIT trace), `currentTick()`.
- **`BehaviourHits`** (core), ONE per server on `AdapterContext`, decides and executes; **`ShockwaveStreaks`** (core) is
  the pure decision.
- **`ShotStamp` v2** carries the bow's behavioural enchants (`id:level;...`) as a tenth field; a v1 arrow still decodes,
  carrying none.
- **The schema:** `EnchantEffect.SHOCKWAVE`; `EnchantDefinition` gains `behaviour`; `EnchantLoader` reads a `shockwave:`
  block and refuses a malformed one BY THE KEY'S NAME; a stat effect with a block, a behavioural one without, and levels
  that disagree with `max_level` are refused by the record. `ContentValidator.validateBehaviourVisuals` names, at boot, a
  shockwave visual that is not loaded.

**The volley rule, flagged, not decided:** a weapon-pressed volley re-reads the held weapon's behaviours PER SHOT, exactly
as it already re-reads the gear score per shot (the inherited, UNRULED volley re-pricing that `cursed_emerald.yml`
documents). B2's "the weapon stamped on the cast" would freeze them at the press instead. **The seat's to rule.**

### The rule, as built (`ShockwaveStreaks`, `BehaviourHits`)

- **A HIT** is a damage `EffectApplier` actually applied (inside the `amount > 0 && alive` gate), by a caster that
  `countsAsHit` and carries a behaviour, on a target that is not a player. Each pellet is its own damage, so its own hit.
  An `Area`'s tick is not a hit; a shockwave is not a hit. Scorch, wither and Ignite never pass through `EffectApplier`
  (READ: `DotStatus`, `Ignite.detonate`), so they never reach the hook at all.
- **Per player, enchant and target**, the times of his hits. A hit fires when it makes 3 or more on that target with
  `now - t < 60` (a hit 59 ticks old counts, 60 does not: SEAT PLACEHOLDER for the boundary tick) AND `now - lastFired >=
  5` (per PLAYER). The count is not cleared. Four pellets in one tick fire exactly one.
- **The shockwave** leaves the struck mob's position and strikes every living MOB whose feet are within the level's radius
  of it, CENTRE TO CENTRE, the struck one included (SEAT PLACEHOLDER), never himself, never a player.
  **READ, AND IT CHANGED THE CODE:** the server's `PaperCombatWorld.combatantsNear` is `World#getNearbyEntities(center, r,
  r, r)`, a BOX; `BehaviourHits` cuts it to a sphere, and `FakeWorld.boxSearch` lets a test answer as the server does.
- **Every term of its damage** (the Damage arm, READ): `GearScore.scaledDamage(amount, score)` = amount x score / 100
  (Ben's gear-score scaling), x `DamageEnchants.multiplier(enchantDamagePercent)` (Sharpness, Power or Attunement on the
  same weapon), + `classDamageBonus` + `weaponLevelDamage` (his class gear; his level's bonus on a weapon hit), x
  `chargeScale` (a basic swing's charge; 1 for a trigger), x `critMultiplier` (the HIT's crit, drawn once at its cast).
  **All of the hit's, which the seat may want narrower** (a half-charged swing halves its shockwave; a crit hit crits it).
- **Forgotten** when the target dies (`RpgListeners.onBehaviourTargetDeath`), when its newest hit is a window old (pruned
  on his next hit), and on his quit (`RpgListeners.onQuit`). **B6: it spawns nothing** (one visual, particles and a
  sound); the state is memory, gone at stop.
- **Threading:** a hit lands on the thread that owns the struck mob; `ShockwaveStreaks` is `synchronized`.

### Eligibility under U2 (the brief's "say how the file declares that")

`element: kinetic` with `class: universal`. Universal admits every class; the element admits only gear whose element is
kinetic, which is only weapons (U5: shields, armour, tools and accessories declare none), and every shipped weapon is
melee, ranger or mage. **So it reaches every kinetic weapon of the three classes and nothing else**
(`UniqueSlotContentTest`: the unique pool of all 16 kinetic weapons is `[kinetic_tremors]`, of every other gear empty, on
a real server as well as a dev one).

### W5

`content/enchants/dev_kinetic_example.yml` is DELETED; no dev enchant remains. The `dev_` mechanism stays (held by
`EnchantLoaderTest.noDevEnchantShipsAndTheDevMechanismStillGatesAWrittenOne` on a written file). The roster is 12 on
every server (11 normal and Tremors).

### OPEN FOR BEN (SEAT PLACEHOLDER 2026-11-23)

1. **Damage 10 / 15 / 20 and radius 3 / 4 / 5 blocks by level** (Ben: levels improve "the damage and size").
2. **The struck mob takes the shockwave too.**
3. **The boundary tick:** a hit exactly 60 ticks old has left the window.
4. **Three levels.**
5. **The icon:** a piston.
6. **The look and sound** (`visuals/tremor_wave.yml`): sweep and crit particles with a grey dust ring, a low
   `entity.generic.explode` at volume 0.5, pitch 1.6.
7. **The line in the table and the tooltip:** `3 hits on one foe in 3s: a 15 damage shockwave, 4 blocks` (level II's).

### Tests and mutations

New: `core/.../enchant/ShockwaveStreaksTest` (9), `core/.../BehaviourHitsTest` (9), `paper/.../KineticTremorsContentTest`
(3); new rows in `ShotStampTest` (1) and `EnchantLoaderTest` (2, and one rewritten for W5); `EnchantLoaderTest`'s shipped
loads now know the shipped elements (`shippedElementsLoader`); `UniqueSlotContentTest` re-pinned to Tremors. Eighteen
mutations, each `marker present: 1  original gone: 0  line delta: 0`, each restored and `cmp` byte-identical, each red:

| mutation | the edit | reddened |
|---|---|---|
| MUTWINDOW | `>=` to `>` on the window | the boundary row |
| MUTCOOL | `>=` to `>` on the cooldown | the cooldown row |
| MUTHITS | `>=` to `>` on the count | 8 rows |
| MUTTARGET | hits keyed by player, not target | the split-target row and two more |
| MUTPLAYERSKIP | the shockwave's player skip removed | "never a player" |
| MUTCOUNTS | `countsAsHit` ignored | `anAreasTickIsNotAHit` (its only guard: a shockwave also carries no behaviour) |
| MUTFROZEN | behaviours read from the world at the hit | the frozen-at-cast row |
| MUTSTAMP | commit stamps no behaviour | 5 rows |
| MUTSTONE | a stone cast stamped too | `aCastNotMadeByTheWeaponCarriesNoBehaviour` |
| MUTHOOK, MUTHOOKWD | the Damage, then the WeaponDamage, arm's hook removed | 5 rows; the stat-reading row |
| MUTAREA | the area's tick counts | `anAreasTickIsNotAHit` |
| MUTSHOTENC | the stamp's tenth field empty | 2 `ShotStampTest` rows |
| MUTSPHERE | the sphere cut removed | the box row |
| MUTREQINT | a missing `hits` defaults | the malformed-block row |
| MUTSTATBLOCK | a stat effect may carry a block | the malformed-block row |
| MUTACTIVEOF | every active enchant becomes a behaviour | the B2 row, by `ActiveBehaviour`'s own refusal (an error, not an assertion) |
| MUTLINE | the line shows level I's damage | the line row |

**UNMUTATED, said so:** the two forgets in `RpgListeners` (a listener; their logic is `ShockwaveStreaksTest`'s); the
paper `behavioursOf` read of the held item and `VanillaShots`' stamp-and-resolve (server-side; the gate's Breacher and bow
rows are their witness); `asSecondary` dropping the behaviours (a second guard beside `countsAsHit`, which bites first).

## 4. PR D — LAPIS CRYSTAL: STOPPED, WITH A PROPOSED SPLIT (no branch)

**The brief's own rule: "If any one of C, D or E is bigger than it reads or needs an engine change beyond its own hook,
STOP on that one with a proposed split and carry on with the others that do not depend on it."** D needs THREE engine
changes beyond its own hook, each READ:

1. **A trigger the weapon's file does not declare.** `WeaponFire.attempt` resolves `weapon.trigger(input)`, and a weapon
   with no binding for the input is left to vanilla (READ: today a left click with the Lapis Staff swings the breeze rod,
   with no ability). Granting one means a trigger binding that comes from an ENCHANT, through `WeaponFire` and
   `AbilityService` (its cooldown and mana), with its ability registered in the `AbilityRegistry` at load. **READ: it
   collides with nothing.** The quiver's reload is left-click on ranger weapons only (`WeaponFire`'s `hasQuiver` arms),
   and `lapis_staff` is mage with no quiver.
2. **A persistent world object.** The orb is a display entity that outlives the cast by 300 ticks; B6 then needs its
   removal on expiry, his quit, his death, a world change and server stop, and the orphan sweep. Nothing an enchant
   spawned existed before; every hook is new.
3. **A hook inside the ray's walk.** `CastExecutor.stepRay` asks `world.castRay` for bodies and blocks only. Stopping a
   Lapis Beam AT the orb means testing each segment against his orb's position before the hit, from region threads.

**Proposed split, each its own PR on the stack:**

- **D1, enchant-granted triggers:** a behaviour `grant_trigger` whose block names an input and declares an ability in
  the existing ability grammar (`AbilitySchema`), so the 30 mana and the 600-tick cooldown are content. The throw is
  stubbed (a projectile that ends at 12 blocks). Tests: the trigger exists only while the enchant is active at level 1 or
  more; it binds no input the weapon already binds; the quiver's left click is untouched.
- **D2, the orb:** the throw's flight (12 blocks along the aim, stopping short at a solid block, about 10 ticks), the
  display entity, one per player, 300 ticks from arrival, and every B6 removal and the sweep. A `CRYSTAL placed at=` log
  line for the gate.
- **D3, the crystal's shots:** the `stepRay` hook (his own beam only, within 0.6 of the orb, before the beam's target), and
  the shots: every mob within 8 of the orb with a clear line, `pct x` the beam's AUTHORED damage read from the staff's
  `right_click` at run time, secondary (B4), so they never feed Kinetic Tremors. The line shows the number (B5).

**What E did NOT need from D:** nothing. E stands on B (the lantern's shot) and C (the engine), so it was built on C.

## 5. PR E — AIRBURST FLAMES (`feat/airburst-flames`, on PR C)

### Ben's words, verbatim, 2026-11-23

> "The enchant is called Air Burst Flames. It splits into three faster not homing projectiles after 12 blocks of travel."
> then: "I want air burst flames to go to 14 blocks instead then. ... The Air Burst Flames projectile goes the 14 blocks
> and then those projectiles go an additional 14 blocks." / "3. 25/30/35 damage each burst" / "5. Doesn't split if it
> hits something first" / "Also side note: The name of the enchantment is Airburst Flames"

### Its own hook, and nothing else

`ProjectileFlight.Impact` gains `expired(point, velocity)`: the flight RAN OUT of lifetime having struck nothing. Its
default is what every flight did before (`at(null, point)`), so no other caller changed. A block hit is still
`at(null, point)`: "something" first, so no split. `CastExecutor.launchOne` asks the cast's frozen behaviours (C's
engine) for an airburst; with one, and with the split distance inside the authored lifetime, it flies the AUTHORED shot,
homing and all, cut to `ticksToTravel(14, 0.35)` = **40 steps** (EXECUTED: 14 / 0.35 = 40.0 exactly), and on `expired`
splits. **The split happens 39 ticks after the press** (the first step runs on the press's own tick), at exactly 14.0
blocks of travel (`AirburstTest`, executed).

**The bursts:** `count` 3 from that point, `SpreadPattern` with `Spread(3, 12, 0)`: the centre straight on and one 12
degrees either side, **sideways = horizontal and perpendicular to the direction of travel at the split** (`SplitBasis`:
`(0 - z, 0, x)`, the same right-hand convention as `Aim`), so travelling east the side bursts go 12 degrees south and
north. **Straight up or down there is no horizontal perpendicular, so sideways is the right of the aim he fired with**
(the brief's question). Each flies `ticksToTravel(14, 0.7)` = 20 steps, never homing, and lands the lantern's on_hit with
its Damage replaced by the level's 25 / 30 / 35 fire: a PRIMARY hit (its caster is the shot's, its payload the burst's
own amount, so scorch's cap is the burst's x 0.5), with the hit visual and fire's burn. Bursts are launched directly,
never through `launchOne`, so they never split again. A `SPLIT caster=… source=… at=x,y,z bursts=3 tick=…` line is
logged while `/rpg mobtrace` is on (`CombatWorld.traceSplit`; shape pinned by `SplitLineTest`).

**The YAML as committed:** `content/enchants/airburst_flames.yml` (weapon-specific, `weapon: living_lantern`,
`class: universal`, `effect: airburst`, block `travel_blocks 14, count 3, angle_degrees 12, speed 0.7,
burst_travel_blocks 14, damage_by_level [25, 30, 35], element fire`). The loader's `BEHAVIOUR_BLOCKS` now names two
blocks and refuses a file that declares both.

**FOR THE GATE, A HOMING FACT THAT MOVES A ROW:** the shot seeks any mob within 7 of ITSELF (`ProjectileFlight.steer`,
READ), so a mob 20 out on the aim line is sought from 13 blocks, and the shot bends toward its FEET (`position()`, READ)
before the split. A mob 22 out is never inside 7 before 14 blocks, so its row is exact.

### OPEN FOR BEN (SEAT PLACEHOLDER 2026-11-23)

1. **The bursts' speed, 0.7 a tick:** "faster" read as double the lantern's 0.35.
2. **The fan, 12 degrees apart, sideways:** the old repo's spread.
3. **The icon:** a fire charge.
4. **The line:** `splits after 14 blocks into 3 bursts of 30 damage` (level II's).
5. **The bursts' look:** the lantern's own trail and hit visual, reused.

### Tests and mutations

New: `core/.../AirburstTest` (9), `paper/.../AirburstFlamesContentTest` (3), `paper/.../adapter/SplitLineTest` (1);
`UniqueSlotContentTest` (the lantern's weapon pool is `[airburst_flames]`, every other gear's empty, now with a control
that the loader knew the weapons), `EnchantLoaderTest` (its shipped loads know the shipped weapons), and
`ScorchContentInvariantTest` (fire damage sites 18 -> 19, with its dated note). Seven mutations, each `marker present: 1
original gone: 0  line delta: 0`, each restored and `cmp` byte-identical, each red:

| mutation | the edit | reddened |
|---|---|---|
| MUTSPLITTICKS | the shot is not cut at 14 blocks | 4 rows |
| MUTEXPIRE | the flight's expiry reported as a miss | 4 rows |
| MUTSPLITDMG | the bursts keep the 45 | 3 rows |
| MUTSIDEWAYS | sideways always the aim's right | the `SplitBasis` row |
| MUTEPS | the travel ticks' epsilon removed | the 2.1 / 0.7 row (green until that row existed: see below) |
| MUTREQDOUBLE | a missing `speed` defaults | the malformed-block row |
| MUTABLINE | the line shows level I's damage | the line row |

**A FALSE CLAIM, CAUGHT BY A MUTATION, CORRECTED:** `ticksToTravel`'s javadoc first said 14 / 0.35 lands a hair above 40
in doubles. MUTEPS stayed GREEN; jshell then printed `40.0`. The javadoc now carries the executed case (2.1 / 0.7 =
3.0000000000000004) and its own row, which MUTEPS reddens.

## 6. THE GATE (`feat/enchant-columns-gate`, on PR E)

`docs/gates/GATE-enchant-columns.md` and `selftest/GATE-enchant-columns.yml` (`/rpg selftest enchant-columns`, setup and
EC0-EC17, every row AUTO and SURVIVAL). It carries THE ONE R0 for the stack from #248 up and the one run sheet (five
lines). It supersedes `GATE-unique-enchant-slot.md`'s R0 and its US1-US7 (never run; removed from that scenario with a
dated note in both files); that gate's R1, setup, US8, BX1 and BX2 ride along unchanged. The scenario count is 351 across
43 gates (`SelfTestScenariosTest`, recounted from the files). R0b was run before the commit against a jar built from the
working tree: see the gate's last section.

## 7. THE SEAT'S RULINGS ON THE FOUR QUESTIONS (its message of 2026-11-23, after reading A, B, C, E and the gate)

1. **VOLLEYS (beside B2, section 3): ACCEPTED, ONE RULE FOR BOTH.** A weapon-pressed volley re-reads the held weapon's
   behaviours PER SHOT exactly as it re-reads the gear score per shot (`CastExecutor.volley`). "The weapon stamped on the
   cast" means, for a volley, the weapon in hand at each shot. One rule, not two.
2. **WHAT A SHOCKWAVE INHERITS: ACCEPTED AS BUILT, OPEN FOR BEN** (section 10's list, in plain words). Every gate row
   that predicts a shockwave names each inherited term and gives the crit reading as the second allowed one.
3. **W1's extra refusal** (a `class:` that could never match the named weapon): ACCEPTED.
4. **THE DEV COMMAND staging any enchant in any slot: ACCEPTED for a dev command.** No row uses it as the proof of W2;
   the ROLLED items of the gate's column rows are that proof (the gate says so beside them).
5. **An area's tick not counting as a hit (`asNotAHit`): ACCEPTED, OPEN FOR BEN.**

## 8. A CORRECTION: LL9R2 WAS READ, NOT OWED (section 2's "FOR THE SEAT" was wrong)

Section 2 said `GATE-breacher-rework.md`'s LL9R2 was "OWED, never run: that gate booted once and no row ran". **That is
false.** The file's own *READINGS, 2026-11-22, THE SECOND BOOT (`52579e8a`)* records LL9R2 **AS PREDICTED** (S at P+40
`75.000/100.000`, at P+80 `65.000/100.000`, three `DOTTICK … sent=5.000 cap=7.000`). I read the first boot's status line
(which ran no row) and the row's heading ("OWED", written before either boot) and never reached the readings section.
The seat's record was right. The row's purpose was therefore READ on the cone; what PR B changes is only that the row
**cannot be re-run as written**, and that is the dated note added to that file on this branch. Section 2 is kept as
written; this is its correction.

## 9. PR D1 — THE ENCHANT-GRANTED TRIGGER (`feat/lapis-crystal-trigger`, on #256)

**The engine:** `EnchantBehaviour.GrantTrigger(weaponId, input, ability)`, `effect: grant_trigger`. The block's
`ability:` is the EXISTING grammar (`AbilitySchema`: name, description, cooldown_ticks, cost, on_cast, cast, on_hit); the
ability's id is `<enchant>/<input>` (`lapis_crystal/left_click`), so its cooldown is its OWN, apart from the Lapis Beam's
(`AbilityService` keys cooldowns by ability id, READ). **One resolver, `core GrantedTriggers`,** for the firing path
(`WeaponFire.attempt`, which now fires `WeaponService.fire(caster, binding, aim)`) and the tooltip (`WeaponItems.applyLore`
-> `WeaponLore.build(…, granted)`): the weapon's own binding wins; otherwise an ACTIVE grant (B2: the active candidate at
level 1 or more, read by `behavioursOf`) whose `weaponId` is THIS weapon. A granted trigger is a weapon trigger: it
carries the level bonus and the weapon's behaviours, as the beam does.

**The loader** (`EnchantLoader.withWeapons`, now the server's: it is handed the weapons themselves, their class for W1 and
their gestures and quiver for a grant) refuses by name: an enchant that is not weapon-specific; a weapon it cannot see;
an input other than left_click or right_click; a weapon that already binds the gesture; a weapon with a quiver; an
ability with no whole-number `cooldown_ticks`. The record refuses a grant whose weapon is not the enchant's `weapon:`.

**READ, AND IT COLLIDES WITH NOTHING:** `lapis_staff.yml` binds only `right_click`; a left click reaches
`WeaponFire.attempt` (`WeaponSwingListener.onSwing`, after the quiver's reload, which only a quiver weapon takes) and,
with no binding, returned empty, so vanilla swung the breeze rod. `lapis_staff.yml` is not edited.

**D1's cast is a STUB** (a body-less projectile at 1.2 a tick for 10 ticks, landing nothing); D2 replaces it with the orb.

**The YAML as committed (D1), comments stripped:**

```yaml
display_name: "Lapis Crystal"
icon: heart_of_the_sea
max_level: 3
effect: grant_trigger
class: universal
weapon: lapis_staff
grant_trigger:
  input: left_click
  ability:
    name: "Lapis Crystal"
    description:
      - "Throws a lapis crystal"
      - "that floats where it lands."
    cooldown_ticks: 600
    cost:
      resource: mana
      amount: 30
    cast:
      type: projectile
      speed: 1.2
      gravity: 0
      max_lifetime_ticks: 10
```

**Tests:** `core/.../weapon/GrantedTriggersTest` (5: granted when active; absent when not; absent on another weapon
carrying the same state, as the dev command can stage it; a weapon's own trigger never replaced; its own id);
`paper/.../LapisCrystalContentTest` (3: Ben's numbers as content; the tooltip shows the grant only while active; six
refusals by name and a loader that cannot see the weapons). The shipped-roster loads in `EnchantLoaderTest`,
`UniqueSlotContentTest` and `AirburstFlamesContentTest` now use `withWeapons`; the roster is 14; the Lapis Staff's weapon
pool is `[lapis_crystal]`. **Mutations, each `marker present: 1  original gone: 0  line delta: 0`, restored and `cmp`
byte-identical, each red:** MUTGRANTWEAPON (the weapon-id check: the other-weapon row), MUTOWNFIRST (own binding first: 2
rows), MUTGRANTQUIVER, MUTGRANTBOUND (each its refusal row), MUTGRANTLORE (the tooltip row). **UNMUTATED:**
`WeaponFire`'s use of the resolver (server-side; the gate's throw row is its witness).

## 10. PR D2 — THE ORB (`feat/lapis-crystal-orb`, on D1)

**The cast:** `CastSpec.Orb(range, flightTicks, lifeTicks, radius, item)`, YAML `type: orb`, every key required
(`AbilitySchema`). A new member of the sealed cast type, so every exhaustive switch answers for it: `CastExecutor`'s
dispatch (`throwOrb`) and its volley refusal, `AbilitySchema`'s inner-cast refusal ("one orb per caster"),
`WeaponLoreLines.deliveredShots` (1), `CastSpec.minimumCooldownTicks` (0: a second throw REPLACES), and
`ContentValidator`'s visual walk (it names no visual).

**The throw (`CastExecutor.throwOrb`):** the rest point is `range` along the aim, or 0.5 short of the first solid block
(`OrbPath.restPoint`, over `sweepRay`'s `blockAt`, READ: bodies do not stop it). A standing orb of his is removed first
(`ORB removed … cause=replaced`). The body spawns at his eye and is driven a step a tick to the rest point over
`flight_ticks`, as a projectile's marker is; on arrival it RESTS (`ORB rest owner=… at=… tick=…`) and `life_ticks` start;
at their end it is removed (`cause=expired`) unless a newer throw or a removal took it first (the generation, `Orbs`).
The orb remembers the GRANT that threw it (weapon, enchant, level), for D3.

**THE STATE: `core Orbs`, one per server** (on `AdapterContext`; the fake world holds its own), one orb per player,
synchronized.

**B6, EVERY REMOVAL, EACH NAMED:**

| cause | where | witness |
|---|---|---|
| expiry, 300 ticks from arrival | `CastExecutor.throwOrb` | `OrbThrowTest` |
| a new throw | `CastExecutor.throwOrb` | `OrbThrowTest` |
| his quit | `RpgListeners.onQuit` -> `OrbRemovals.remove(…, "quit")` | `OrbRemovalWiringTest` (source) |
| his death | `RpgListeners.onOrbOwnerDeath` | the same |
| a world change | `RpgListeners.onOrbOwnerChangedWorld` | the same |
| server stop | `RpgPlugin.onDisable`, right after the selftest's restore, before the worlds save | the same |
| never saved with the world | the body is a marker: `setPersistent(false)` (READ, `configureMarker`), so a save writes it nowhere | READ |
| the sweep | a body tagged `Keys.orb` that no live orb owns is removed where it appears (`onOrbBodyAppear`) and in the enable-time walk of every entity, with `ORB removed … cause=orphan` | `OrbRemovalWiringTest` (source) |

**The look** is an ITEM MARKER (a heart of the sea), not a display entity as the brief's placeholder said: the marker
already carries every B6 property above (never saved, never picked up by a player, a mob or a hopper, gravity off,
vanilla's despawn armed as a backstop) and needs no new entity path. SEAT PLACEHOLDER, OPEN FOR BEN.

**The trace lines** (`PaperCombatWorld.orbLine`, pinned by `OrbLineTest`), logged ALWAYS, not only under mobtrace (one
per throw and per removal; the boot's after-stop check reads `cause=stop`): `ORB rest owner=<uuid> at=<x>,<y>,<z>
tick=<n>` and `ORB removed owner=<uuid> at=<x>,<y>,<z> cause=<cause> tick=<n>`.

**The YAML as committed (D2: the cast only changed), comments stripped:**

```yaml
    cast:
      type: orb
      range: 12
      flight_ticks: 10
      life_ticks: 300
      radius: 0.6
      item: heart_of_the_sea
```

**Tests:** `core/.../OrbThrowTest` (5: rests 12 out at tick 10 and is gone 300 after arriving; a wall 8 out rests it at
7.5; a new throw replaces the old and the old's expiry does nothing; an orb no grant threw remembers no weapon; the
registry owns only live bodies); `paper/.../OrbRemovalWiringTest` (2), `paper/.../adapter/OrbLineTest` (2),
`LapisCrystalContentTest` (the cast pinned). **Mutations, each red, each restored and `cmp` byte-identical:** MUTORBLIFE
(life counted from the throw: 2 rows), MUTORBREPLACE (the old body kept), MUTORBWALL (the wall ignored), MUTORBGEN
(`removeIf` ignores the generation; its check also stands in `arrive`, so this one was applied to the SECOND occurrence
by an occurrence counter, marker 1, the other occurrence left, line delta 0), MUTORBDEATH (the death removal unwired).
**UNMUTATED, said so:** the body's per-tick drive (visual only; the logged rest point is core's), and paper's entity
removal inside `OrbRemovals` (server-side; the gate reads its lines).

## 11. PR D3 — THE BEAM AND THE CRYSTAL'S SHOTS (`feat/lapis-crystal-shots`, on D2)

**THE SEAM, AND IT IS ITS OWN (so D3 did not stop):** two insertions in `CastExecutor.stepRay`, the plain (non-sweeping)
ray's walk, which the Lapis Beam uses. After the segment's `castRay`, `meetsOrb` asks for HIS OWN orb (looked up by the
beam's caster), RESTING, with a shot, and only when the beam's ability is a trigger of the weapon that GRANTED the orb (its
id is `<that weapon>/<input>`; the orb remembers the grant). `OrbIntercept.along` (pure) tests the segment ONLY AS FAR AS
THE BEAM REACHES (its first body or block): the nearest point to the orb's centre within the orb's 0.6. If it meets: the
beam is drawn to that point, nothing is detonated, and `orbShots` fires. So the beam's own target BEHIND the orb is
untouched, and a target IN FRONT of the orb is struck as before. No other cast path changed.

**The shots (`CastExecutor.orbShots`):** every living MOB whose feet are within 8 of the orb's centre (a TRUE SPHERE, cut
from the server's box as the shockwave's is; `FakeWorld.boxSearch` shows it) and to whose sight point the orb has a clear
line (`lineOfSightClear`), each once for **beam's authored damage x percent / 100**, the beam's FIRST Damage effect read
from the beam's own ability when it meets the orb (21 today; never copied into the enchant). Kinetic. SECONDARY (B4):
source `lapis_crystal/crystal`, never a hit (so Kinetic Tremors never counts it), no behaviour, never himself, never a
player; a plain `EffectApplier`, so no direct-damage listener hears it. **It carries every term of the BEAM that set it
off, exactly as a shockwave carries its hit's (ruling 2):** `(21 x percent / 100) x score / 100` (the beam's gear score),
`x DamageEnchants.multiplier(enchant %)` (Attunement active on the staff), `+ class bonus + level bonus` (the flat terms,
added to every mob it strikes), `x charge` (1 for a trigger), `x crit` (the beam's crit). The orb is not used up: every
beam into it fires again.

**Shown as the number (B5):** the line in the table and the dev command reads the staff's OWN right-click damage off its
loaded definition (`EnchantEffectLine.orbShotClause`, handed the weapons): **4.2 / 6.3 / 8.4** at levels I / II / III.
EXECUTED (jshell): `21 * 20 / 100.0` = 4.2, `21 * 30 / 100.0` = 6.3, `21 * 40 / 100.0` = 8.4, each exact in doubles, so
`number` prints them as they are; no rounding rule is needed or applied. A renderer with no weapons (the `ACTIVE` reply's
suffix) shows `30% of the beam`.

**The YAML as committed (D3: the orb cast gained its `shot:`), comments stripped:**

```yaml
      shot:
        radius: 8
        percent_by_level: [20, 30, 40]
        element: kinetic
```

**Tests:** `core/.../OrbBeamTest` (10: each mob within 8 with a clear line struck for 6.3, none beyond, no player, none
behind a wall, the beam's own target behind the orb untouched; the no-orb control 21; a second beam fires again; levels I
and III, 4.2 and 8.4; the beam's score 150 makes 9.45 (EXECUTED); an orb in flight, another player's orb and another
weapon's beam each pass; the box cut to a sphere; three crystal shots with Tremors active fire no shockwave);
`LapisCrystalContentTest` (+2: the line's number at each level and its fallback; a shot refused by name for its level
count and its element). **Mutations, each red, restored, `cmp` byte-identical:** MUTORBOWNER (`Orbs.of` answers any
owner's orb: the other-player row), MUTORBRESTING (the in-flight row), MUTORBWEAPON (the other-weapon row), MUTORBSTOP
(the beam continues past the orb: 2 rows; applied by line number, the search spanning two lines), MUTORBLOS (3 rows),
MUTORBSPHERE (the box row; it was GREEN before that row existed, the fixture searching a sphere already, and the row was
added for it), MUTORBSECONDARY (the shots counted as hits: the Tremors row), MUTORBINTERCEPT (never meets: 5 rows),
MUTORBLINE (the percent shown: the line row), MUTORBLEVELS (the level count unchecked: the refusal row).

## 12. THE GATE FOR D (`feat/lapis-crystal-gate`, on D3)

`GATE-enchant-columns.md` gains, on this branch: **EC1R2** (the Lapis Staff's rolled columns with Lapis Crystal in slot
0; EC1 superseded, never run, with a dated note); **LC1-LC4** (the throw's 30 mana and its own silent cooldown, the
orb's resting point 12 out from the `ORB rest` line, a beam into it striking N for 6.3 and neither F beyond 8 nor T
behind it, a second and third beam firing again with Kinetic Tremors active and no shockwave, gone 300 ticks after
arriving with `cause=expired` and then the beam passing to T); **EC17 extended** (no orb left at the run's end; no
`cause=stop` after `/stop`); **R0bR2, R0cR2, R0dR2** (the scan widened to D's lines, 14 enchants, 355 scenarios across
43 gates); the seat's ruling 2 named once for every shockwave and crystal row (the gate's D2); the seat's ruling 4
beside the column rows; SAFETY for LC1-LC4 (0); and the run sheet's plain-words paragraph on the open ground (a strip 33
east by 13 wide, air at feet and head) and the three selftest lines Ben types. R0bR2 was run before this commit against a
jar built from this tree: see the gate's last section.
