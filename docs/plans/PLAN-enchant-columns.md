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
   > **RULED AGAIN FOR FOCUSED BEAM, 2026-11-24 (the seat's boot approval of `3890aaa8`), in its words:** "the enchant
   > applies by the weapon in hand at the shot, as built (ruling 1: a volley re-reads per shot). A swap during the
   > wind-up fires the six plain bolts. The brief's "that made the cast" was loose." (Section 18's flagged disagreement,
   > closed.)
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

## 13. MERGED, 2026-11-23, AND WHAT IS OWED (NOTES ONLY: NO CODE UNTIL THE SEAT'S BRIEF OF BEN'S FIVE CHANGES)

**The stack #248-#262 is MERGED** (master `caa95043`, its tree `4382b8e1…` equal to the tree of `7d0457d1`, the readings'
head), every row of `GATE-enchant-columns.md` and `GATE-unique-enchant-slot.md` read. The seat's merge approval of
2026-11-23 stands on Ben's words, verbatim: "merge it". **The next PR waits for the seat's one message briefing Ben's five
changes to the enchants.** Until then, these are owed and recorded here, and nothing is built:

1. **THE `LINGER work=1` AT THE START OF LC2R2, LC3R2 AND LC4R2 IS TO BE READ AND EXPLAINED IN THE NEXT PR.** On the run
   of `efa38636`, the row-start clean after each row whose orb `OrbRemovals.remove` had removed (cause `selftest`) ended
   ONE point-scheduled chain round the stand point. Owed: WHAT task it is (READ from `CastExecutor`'s orb scheduling and
   `PendingWork`, not inferred), and that `OrbRemovals.remove` leaves nothing scheduled that can ACT (strike, re-spawn a
   body, or log) once the orb is gone, with a unit row that holds it.
2. **A DATED NOTE ON `GATE-breacher.md`: BR9's probes stood beyond 16.** `ScenarioLint` (PROBE_REACH): `probe n: 25 out`
   and `probe f: 33 out`, each against a reach of 16, so those probes could never read their mob. Recorded against the
   gate's readings, which stand as read; no row is re-run by the note.
3. **A DATED NOTE ON `GATE-dragon-blink.md`: BS3 presses in the same pass as its turn** (`ScenarioLint`, TURN_THEN_PRESS,
   twice): each `use` there reads the facing before its turn. The same: a note against the readings, no re-run.
4. **THE CARRIED LIST FROM THE BOOT APPROVAL (the seat's, 2026-11-23), still open:**
   - `clear_inventory` rests on the in-memory snapshot: a JVM killed mid-run after a player save could save a cleared
     inventory (the pre-boot backup is the recovery; Ben has been told).
   - The pad holds no chunk tickets (the adapter context carries no plugin handle): a chunk that unloads between its load
     and the build fails the run and touches nothing.
   - `ScenarioLint` does not yet enforce score, mana, enchant and cooldown staging (its SELF_STAGED rule reads facing,
     the held weapon and the mobs only).
   - The box-to-sphere cut (`BehaviourHits` and the orb's 8) is UNREAD on a server: no mob in either gate stands inside a
     search box but outside its sphere.
   - `breacher#BR9` and `dragon-blink#BS3`, items 2 and 3 above.
5. **For Ben, from the gate's third form (seat placeholders, still open):** the pad's place (200 east of world spawn, at
   spawn's z; it stands at x 104, floor Y 200, z -4 in the dev world) and that it is plain stone.

## 14. BEN'S FIVE CHANGES TO AIRBURST FLAMES AND LAPIS CRYSTAL (2026-11-23): PR 1 `feat/enchant-changes`, PR 2 `feat/lapis-crystal-block`

**Ben, verbatim, after seeing the run on the pad:** "1. I want the Airburst Flames to seek as well." / "2. I want it to
split into 3 and 6 blocks instead of 12" (asked, "B. Split after 6 blocks into 3 pieces; pieces keep their 14.": "B") /
"3. I want to change the visual of the Lapis crystal to a Floating Lapis block with glowing" / "4. Draw a line with
particles from the crystal to the target with the same particles the right click uses" / "5. Increase the damage of the
crystal to 30/50/70%".

**THE SPLIT, the seat's ruling on CC's proposal:** PR 1 carries items 1, 2, 4, 5 and the owed item 6; PR 2 (on PR 1)
carries item 3, the body, which is a new kind of orb body (a block display has no physics and no despawn, READ). ONE gate,
`GATE-enchant-changes.md`, on the top; ONE boot after PR 2; ONE question for Ben.

**PR 1, AS BUILT:**
1. **The bursts seek.** `airburst_flames.yml` declares the bursts' OWN `homing:` block (SEAT PLACEHOLDER 2026-11-23: the
   Living Lantern's 0.45 / 0 / 7), parsed by `EnchantLoader` into `EnchantBehaviour.Airburst.homing` (all three keys
   required when present; absent, straight, as before). They still fan 12 degrees at the split. **READ
   (`ProjectileFlight.steer`, `nearestMob`, `PaperCombatWorld.combatantsNear`):** each burst re-chooses, every tick, the
   nearest live mob it can see within 7; nothing locks a target or keeps two bursts apart, so **two bursts may choose the
   same mob and both strike it**; a target that dies is not returned by `combatantsNear` (live bodies only), so **the
   burst turns to the nearest other live mob, or flies straight if none is in range**.
2. **The split at 6.** EXECUTED (jshell): `ticksToTravel(6, 0.35)` = 18 and `18 x 0.35` = 6.3, so the split is the 18th
   step, 17 ticks after the press, at 6.3 blocks (17 steps would be 5.95). The bursts' own travel stays 20 ticks
   (`ticksToTravel(14, 0.7)`). **The lantern's own homing reaches 7, beyond the split's 6: a mob within 7 of the shot's
   path before the split can now DRAW the shot, which then hits it (the 45) and never splits.** A gate row that wants the
   split stands no mob within 7 of the first 6.3 blocks of path.
3. **The drawn line.** `CastExecutor.orbShots` takes the RAY's own `beam` (the ray that met the orb, at the moment it
   fires; nothing copied into the enchant file) and, for each STRUCK mob, presents it from the orb's rest point to the
   mob's sight point, with an `ORBSHOT owner=… from=… to=… visual=… tick=…` line under mobtrace. A ray naming no beam
   draws nothing, and `ContentValidator.validateOrbBeams` warns at load for any weapon granted an orb with a shot whose
   ray trigger names no beam.
4. **30/50/70.** `lapis_crystal.yml` `percent_by_level: [30, 50, 70]`. EXECUTED: `21 x 30 / 100` = 6.3, `21 x 50 / 100`
   = 10.5, `21 x 70 / 100` = 14.7; the table line and the tooltip read `… within 8 for 6.3 damage` / `10.5` / `14.7`
   (`LapisCrystalContentTest`).
5. **The LINGER work=1 (item 6), READ and fixed.** It was the expiry `throwOrb` schedules on the orb's arrival, left
   scheduled by `OrbRemovals.remove`; the row-start clean ended it. It could not act (`Orbs.removeIf` matches only its
   own generation; `OrbExpiryTest.anExpiryLeftScheduledAfterARemovalCannotAct`). Now the orb carries its expiry's handle
   (`CombatWorld.scheduleCancellable`, `Orbs.expiring`), a replacement cancels it in core, and `OrbRemovals.discard`
   (every paper removal) cancels it: a row-start clean after an orb row finds `work=0`. The flight's own tasks (ten
   driving steps and the arrival, within 10 ticks of a throw) are not cancelled: a removal mid-flight leaves them to run
   against a gone body and a gone generation, which does nothing.
6. **The owed dated notes**: `GATE-breacher.md` BR9, `GATE-dragon-blink.md` BS3, and `GATE-enchant-columns.md`'s note
   that EC14R2-EC16R2 and LC1R2-LC4R2 cannot be re-run as written.

**PR 2, AS BUILT (`feat/lapis-crystal-block`, the seat's rulings (a) to (c)):**
1. **The body kind is the orb's** (`CastSpec.Orb.block`, YAML `body: block` with a `block:` section of `id`, `scale`,
   `glow`; `body: item`, the default, keeps D2's item marker, and each refuses the other's key). `lapis_crystal.yml`
   now declares `body: block`, `id: lapis_block`, `scale: 0.5`, `glow: "#2A5CFF"`.
2. **(a) The flight stays:** the display is PLACED where the item would have been driven, once a tick
   (`CastExecutor.flightStep`), the last placement the rest point itself; `setTeleportDuration(1)` eases each placement
   over its tick (the API's javadoc, READ). The rest point, the rest tick and the ORB rest line are unchanged
   (`OrbBlockBodyTest.theBlockBodyWalksTheItemBodysPathAndRestsAtTheSameTick`).
3. **(b) The backstop:** scheduled at the throw at flight + life + 100 ticks (`CastExecutor.ORB_BACKSTOP_MARGIN_TICKS`,
   CC's figure), it removes THAT body by id whatever the store says (`ORB removed … cause=backstop`); expiry,
   replacement and every paper removal (`OrbRemovals.discard`) cancel it. Only a block body has one (an item marker has
   vanilla's despawn).
4. **(c) Centred and blue:** a translation of `-scale/2` on each axis (READ, `Transformation.compose`), the glow by
   `setGlowing(true)` and `setGlowColorOverride` (READ, `CraftDisplay`). Never saved (`setPersistent(false)`; READ,
   `saveAsPassenger`). Tagged as an orb AND a marker, so the sweep, `removeMarker` and the row-start clean's LINGER all
   find it. One `ORBBODY` line per spawn, read back off the body. A block id that names no block is warned at load
   (`ContentValidator.validateOrbBodies`) and spawns no body.
5. **READ, for the gate:** a display cannot be picked, hit by a vanilla projectile or collided with; our own rays take a
   body only through `LiveTargets.resolve` (a live `LivingEntity`), so they cannot meet it (READ, not inferred).
6. **The gate:** `GATE-enchant-changes.md`, on this branch, carrying the one R0 for both PRs.

**OPEN FOR BEN (SEAT PLACEHOLDERS 2026-11-23):** the bursts' homing values (0.45 / 0 / 7, the lantern's own); and, PR 2's,
the crystal body's scale (0.5, centred on the rest point) and glow colour (`#2A5CFF`, blue). CC's own figure, not a
placeholder for Ben: the backstop's margin of 100 ticks.

**2026-11-23, AFTER THE RUN OF `78826389` (the seat's brief): THE FIX, ON `feat/lapis-crystal-block`, AND BEN'S FOUR
CHANGES TO THE CRYSTAL, ON `feat/lapis-crystal-look` (one PR on top, so the fix reads on its own).** The run's LB rows
were MISS on one defect, CC's and the seat's: the block body carried the orb tag from inside its spawn, the entity-add
sweep found it unowned and logged a false `cause=orphan` (Paper refused the removal). Fixed: the body is claimed only
once the store owns it; the sweep at the add defers a tick and logs only a removal that happened
(`GATE-enchant-changes.md`, the dated note). **Ben, told to the seat after the run:** "1. Make it float up and down and
spin slightly similar to a dropped item" / "2. Remove the glow, it is excessive" / "3. Increase the range of the shots
from the lapis crystal to 15 blocks." / "4. Add a few particles around the lapis crystal". **As built:** the look at rest
only (a bob of 0.1 over 60 ticks, a turn per 120, re-set and interpolated every 10, the rest point never moving); no glow;
`shot: radius: 15`; `crystal_motes` presented at the rest point every beat. The gate's second form re-predicts the LB
rows and adds LB5 (the radius, and the box-to-sphere cut).

**OPEN FOR BEN (SEAT PLACEHOLDERS 2026-11-23), replacing the list above for the crystal:** the bob (0.1 block), its
period (60 ticks), the turn (one per 120 ticks), the beat (10 ticks), at rest only; the motes (`crystal_motes`: DUST,
size 0.6, count 3, spread 0.3, twice a second); the body's scale (0.5). The glow is gone (Ben). Still open from before:
the bursts' homing (0.45 / 0 / 7).

## 15. MERGED, 2026-11-24: #263 TO #266. WHAT IS CARRIED, AND EVERYTHING OPEN FOR BEN IN ONE PLACE (NOTES ONLY)

**Merged by `.claude/skills/merge-procedure`, bottom-up, on Ben's "merge it" and the seat's approval of 2026-11-24:**
#263 as `510790b4`, #264 as `e48df9ad`, #265 as `09cf866b`, #266 as `89253ed4`. Each squash's tree equals its branch's;
`check-absorbed.sh` read ABSORBED (control PASS) for each; #264, #265 and #266 were rebased `--onto` master with every
patch id unchanged. **Master's tree `b1e16178…` equals the tree of `ebfd08fc`**, the booted stack's top. Every row of
`GATE-enchant-changes.md` is READ AS PREDICTED (AF1-AF4 on `78826389`; LB1R2, LB4R2, LB2R2, LB3R2 and LB5 on
`01ee2d65`); the first-form LB rows stand as MISS on the defect `09cf866b` fixed. Ben's answer to the ask: "yes".
**The box-to-sphere cut is READ on a server** (LB5's X: inside the server's box, 0.704 outside the 15-block sphere, no
shot) and comes off the carried list.

### THE CARRIED LIST, AS IT NOW STANDS

> **UPDATED 2026-11-24, after the merge of #267 to #269 (`ee46a493`): the owed weapon filter for behaviours now heads
> this list** (section 16's finding, the seat's ruling recorded at its end), and the items below it are renumbered by
> one; their text is unchanged.

> **UPDATED AGAIN 2026-11-24, after the merge of #270 to #272 (`c1ebabab`), by the seat's merge approval of that date:
> the one-tick question now heads this list, the owed aim row sits beside it, and the weapon filter (the old first item)
> is DONE (#271) and is kept below, marked, its text unchanged.** Every item is renumbered by two; the only words removed
> are the ordinals.

1. **OWED, ITS OWN PR, BEFORE ANY CODE IS RULED: THE SERVER AND THE FAKE WORLD DISAGREE BY ONE TICK ABOUT "N TICKS
   LATER".** `GATE-focused-beam.md`'s run of `3890aaa8` read every `FOCUSED` line's `tick - pressed` as **19**, where a
   task scheduled 20 ticks on (`CombatWorld.scheduleOn`) was predicted at 20 and the fake world runs it at exactly 20.
   The seat's ruling: not a defect of Focused Beam (the beam leaves on the volley's own first shot, by the volley's own
   scheduler call). **READ, do not infer** (javap on the pinned jar, and our scheduler adapter) exactly why a task
   scheduled for 20 ticks ran at press + 19: which call, which tick each of the two reads is taken on, and whether every
   `scheduleOn` / `schedule` delay in the game runs one short of its authored ticks or only this path. **Then PROPOSE, do
   not build**, which of three is right: (a) the adapter adds the tick so content means what it says; (b) the fake is
   corrected to the server's clock and content stays; (c) the trace's two reads are taken at different phases and nothing
   real is short. Say what each would change for shipped weapons and for gates already read. The seat rules before any
   code.
2. **OWED, BESIDE IT: THE AIM ROW.** Ben's "4. Yes" (Focused Beam fires where he looks when the wind-up ends) is UNREAD
   on a server; `FocusedBeamTest.theAimIsReadAtTheWindupsEndNotAtThePress` and MUTLOCK hold it. The row that turns him
   during the wind-up (press facing east, `wait: 10`, a `tp` facing a zombie to the south, `wait: 30`; the south zombie
   takes the beam, one standing east nothing) goes into **the next gate that touches the Cursed Emerald** (section 18).
3. **DONE 2026-11-24 (#271, squashed as `35060659`; read on the server as `GATE-focused-beam.md` CSS, with AFR
   re-reading Airburst on its own weapon in place of AF1-AF4, by the seat's brief).**
   **OWED: A BEHAVIOUR THAT NAMES A WEAPON ACTS ONLY ON THAT WEAPON, ENFORCED ONCE WHERE BEHAVIOURS ARE READ FROM THE
   ITEM.** `BehaviourItems.activeOf` has no weapon filter, so Airburst Flames staged by the dev command on another
   projectile weapon would split it (no roll can produce that). Shatter Caps and the Lapis Crystal's grant check their own
   weapon at the cast; Airburst does not. When it is done, `GATE-enchant-changes.md`'s AF1-AF4 are re-read.
4. **The selftest's snapshot lives in memory.** A server crash mid-run leaves his inventory, position and the world's
   rules as the run left them; the restore runs on every END path the plugin sees (finish, stop, exception, quit, server
   stop), not on a crash.
5. **The pad holds no chunk tickets** (the adapter context carries no plugin handle): a chunk that unloads between its
   load and the build fails the run and touches nothing (section 13's account, unchanged).
6. **The scenario lint does not yet enforce the staging of the gear score, mana, the enchant state and a cleared
   cooldown.** It enforces the inventory, the turn-then-wait, probe reach, self-staging and the ground; a row that
   forgets `rpg gearscore set`, `rpg mana refill`, `rpg enchant clear` or `clear_state` is not caught.
7. **Unread on a server, by design:** a REAL orphan's removal one tick after it appears (no row stages an orphan; the
   path is read by `OrbClaimTest`, `OrbBlockBodyLoadTest`'s pins and five mutations), and the BACKSTOP's firing (it acts
   only when every other removal was skipped; `OrbBlockBodyTest` and `OrbLookTest`).

### OPEN FOR BEN, IN ONE PLACE (every value as the content reads at `89253ed4`)

| what | current value | where |
|---|---|---|
| The crystal's bob | 0.1 block over 60 ticks, at rest only | `lapis_crystal.yml` `bob_blocks`, `bob_period_ticks` |
| The crystal's turn | one turn per 120 ticks | `turn_ticks` |
| The crystal's beat | 10 ticks (the look re-set and the motes shown twice a second) | `beat_ticks` |
| The crystal's motes | DUST rgb(40, 90, 240), size 0.6, count 3, spread 0.3, twice a second | `visuals/crystal_motes.yml` |
| The crystal's size | scale 0.5 block, centred on the rest point | `scale` |
| The bursts' homing | lerp 0.45, activation 0 blocks, search radius 7 | `airburst_flames.yml` `homing:` |
| The bursts' spread and speed | 12 degrees apart, 0.7 a tick | `angle_degrees`, `speed` |
| Kinetic Tremors' damage | 10 / 15 / 20 | `kinetic_tremors.yml` `damage_by_level` |
| Kinetic Tremors' radius | 3 / 4 / 5 blocks | `radius_by_level` |
| What a shockwave inherits | amount x the hit's gear score / 100, then the hit's damage enchant, class and level bonus, charge and crit (section 7, ruling 2) | `kinetic_tremors.yml` header |
| The Living Lantern's homing | lerp 0.45 (activation 0, search 7: Ben's 7) | `living_lantern.yml` `homing:` |
| The Living Lantern's description | "Looses a slow, seeking flame" / "that hunts within 7 blocks." | `living_lantern.yml` `description:` |
| The two empty-column markers | weapon: "No weapon enchant" / "Nothing is tied to this weapon yet."; unique: "No unique enchant" / "Nothing unique is tied to this item's element yet." | `EnchantMenuLayout.emptyMarkerFor` |
| The pad's place | 200 east of world spawn at spawn's z: stand x 104, floor Y 200, z -4 in the dev world; plain stone | `SelfTestPadShape` |
| The four screens whose info item was left alone | Level (its item at 4 is a readout), Build picker (its item at 4 titles the pick), Vault (no info item; 8 is NEXT), Recipe book (no info item; 8 is an entry) | `PLAN-unique-enchant-slot.md` section 8 |
| Shatter Caps' stone speed (added 2026-11-24) | 2.5 a tick, so the split is exactly the 2nd step at 5.0 | `shatter_caps.yml` `speed` |
| Shatter Caps' stone look | `stone` (Ben: "1 stone") | `shatter_caps.yml` `item` |
| Shatter Caps' icon | `cobblestone` | `shatter_caps.yml` `icon` |
| Shatter Caps' line | "fires one stone of 35 damage that shatters after 5 blocks into the usual chunks" (35 / 60 / 85 by level) | `EnchantEffectLine`, the `SHATTER` arm |
| The Breacher's own description with Shatter Caps active | NOT EDITED: still "Four chunks of cobblestone," / "a tight triangle 6 degrees wide." | `breacher.yml` `description:` |
| Focused Beam's 48: its scope (added 2026-11-24) | THE SEAT'S READING of Ben's "increase the range to 48 blocks": this beam only; the plain Cursed Volley keeps its 32 | `focused_beam.yml` `range` |
| Focused Beam's look | the bolt's own `emerald_beam`, unchanged, drawn to what it struck or to 48: whether the one beam should look heavier | `cursed_emerald.yml` `beam` (read at run time) |
| Focused Beam's icon | `ender_eye` | `focused_beam.yml` `icon` |
| Focused Beam's line | "after the wind-up, fires one beam of 108 damage reaching 48 blocks" (108 / 162 / 216 by level) | `EnchantEffectLine`, the `FOCUSED_BEAM` arm |
| The Cursed Emerald's own description with Focused Beam active | NOT EDITED: still "Channel, then loose six" / "bolts down your line of sight." | `cursed_emerald.yml` `description:` |

**No code is owed by this section.** The next stack opens from `89253ed4`.

> **DATED NOTE, 2026-11-24, after the merge of #270 to #272 (master `c1ebabab`):** the line above is the record of
> section 15 as it was written. Since then #267 to #272 have merged; the next stack opens from `c1ebabab`, and the
> carried list's first item owes a READ and a proposal, not code, before the seat rules.

## 16. SHATTER CAPS (THE BREACHER'S WEAPON-SPECIFIC ENCHANT) AND THE BREACHER'S REACH, 2026-11-24

**Ben, verbatim (the seat's brief of 2026-11-24):** "Let's do a new enchantment for the Breacher. Shatter Caps. Instead of
shooting 4 cobble right away, it shoots 1 stone that splits into 4 cobble after traveling 5 blocks. The damage of the
Stone is 35/60/85" / asked what the four chunks do after the split, how far they fly (A: the rest of the normal reach, B:
a full reach more), and whether they keep today's pattern: "1. the usual 40 2. their full reach 3. the same pattern." /
"One extra note, I want to increase the range that the cobble travels normally to 18 blocks" / "Another note, I want the
damage of the shatter caps stone to follow with gearscore, just like the lapis crystal and airburst flames".

**PR 1, `feat/breacher-reach` (#268):** `breacher.yml` `max_lifetime_ticks` 4 -> 6 (6 x 3.0 = 18 blocks); dated notes on
`GATE-breacher.md` (BR4 cannot be re-run as written) and `GATE-breacher-rework.md` (no row stages a mob beyond 12; its
stated flight is now 6 ticks); `PLAN-weapon-tiers.md` 1.15.

**PR 2, `feat/shatter-caps`, AS BUILT:**
1. **A second behaviour type, `EnchantBehaviour.Shatter`, beside `Airburst`, not a shared seam** (`effect: shatter`, the
   file's `shatter:` block: `travel_blocks`, `speed`, `item`, `damage_by_level`, `element`). Airburst cuts the authored
   shot short and launches the enchant's bursts; Shatter replaces the authored spread with the enchant's stone and
   launches the weapon's own cast at the split. Airburst's path is untouched (its tests unchanged and green).
2. **The stone** (`CastExecutor.launchStone`), taken where the spread is expanded, below the commit: one round and one
   cooldown a press, as the spread. One body at 2.5 a tick (SEAT PLACEHOLDER), item stone (SEAT PLACEHOLDER), the
   Breacher's trail, carrying the Breacher's own on_hit with its damage replaced by 35 / 60 / 85 kinetic (BEN), through
   the same applier as every hit: the gear score scales it (BEN), the knockback and the hit visual ride as authored, and it
   is one primary hit for Kinetic Tremors.
3. **The split** (`CastExecutor.shatter`): `ticksToTravel(5, 2.5)` = 2, so the 2nd step, exactly 5.0 (EXECUTED). The
   Breacher's OWN cast leaves that point: its spread, read from `breacher.yml` at run time, built about the stone's
   direction with the press's own right vector (so rotation 90 keeps the point up, straight up included), each chunk the
   weapon's shot with its speed, item, 40 and full 6-tick reach (BEN: "the usual 40", "their full reach", "the same
   pattern").
4. **A hit before 5** lands the level's damage and never splits; **a block before 5** ends the stone as it ends a chunk
   (the hit visual where it stopped, no damage, no split; READ).
5. **The weapon check:** `Shatter` carries `weapon: breacher` and acts only on a cast whose ability is `breacher/...`, as
   the Lapis Crystal's grant does (the dev command can stage any enchant on any weapon). **Found, not fixed: `Airburst`
   has no such check** (Airburst Flames staged on another projectile weapon by the dev command would split its shot; no
   roll can produce that). A candidate for a later PR, which would change Airburst's path and need its own gate.
6. **The loader refuses, by name:** a weapon whose right click is not a projectile with a spread; no `weapon:`; a level
   list not matching `max_level`; a non-positive distance or speed. The line (B5): "fires one stone of 35 damage that
   shatters after 5 blocks into the usual chunks" (wording SEAT PLACEHOLDER).
7. **The gate:** `GATE-shatter-caps.md`, on this branch, carrying the one R0 for #267 and both PRs.

**OPEN FOR BEN (SEAT PLACEHOLDERS 2026-11-24):** the stone's speed (2.5 a tick, so the split is exactly the 2nd step) and
its look (`stone`); the enchant's icon (`cobblestone`); its line's wording; **the Breacher's own description, NOT EDITED,
which still reads "Four chunks of cobblestone," / "a tight triangle 6 degrees wide." with the enchant active.**

**OWED, RECORDED 2026-11-24 (the seat's ruling on the finding of section 16, item 5): A BEHAVIOUR THAT NAMES A WEAPON ACTS
ONLY ON THAT WEAPON, ENFORCED ONCE WHERE BEHAVIOURS ARE READ FROM THE ITEM.** Today `BehaviourItems.activeOf` hands every
behaviour on the held item's enchant state to the cast with no weapon filter, so Airburst Flames staged by the dev command
on another projectile weapon would split that weapon's shot (no roll can produce that state: W1). Shatter Caps and the
Lapis Crystal's grant each check their own weapon at the cast; Airburst does not. **The fix belongs in one place, where
behaviours are read from the item, and when it is done `GATE-enchant-changes.md`'s Airburst rows (AF1-AF4) are re-read.**
Not done in `feat/shatter-caps`, by the seat's ruling: Airburst's path stays as `GATE-enchant-changes` read it.

## 17. PR 1 OF THE SEAT'S BRIEF OF 2026-11-24: THE OWED WEAPON FILTER (`feat/behaviour-weapon-filter`, on #270)

**The carried list's item 1 (section 15), built.** A behaviour whose enchant names a weapon acts ONLY on that weapon,
enforced ONCE, in `BehaviourItems.activeOf`, which now takes the id of the weapon the state was read from and drops any
behaviour whose `weapon:` is another (or any weapon at all, for a null id). A behaviour that names no weapon (a unique,
element enchant: Kinetic Tremors) passes as before.

**WHO READS THROUGH IT (READ, `git grep -n "BehaviourItems\.\|behavioursOf("` at `6a1c4713`):** `PaperCombatWorld
.behavioursOf` (every cast's read: commit, a basic swing, each volley shot; and `WeaponFire`'s read for a granted
trigger; and `VanillaShots`' stamp, which goes through it) now passes the held weapon's id; `WeaponItems.applyLore` (the
tooltip's granted trigger) passes the weapon being rendered. `VanillaShots`' hit-time resolution re-reads only the ids
the stamp already filtered. There is no other caller. The two-argument form is GONE, so a new caller cannot forget the
weapon: it does not compile.

**SHATTER CAPS' OWN `startsWith` CHECK IS KEPT, AND WHY (the brief asked).** The filter answers "is this the weapon's
ITEM"; the check in `CastExecutor.launch` answers "is this the weapon's CAST". They differ only on a volley's per-shot
re-read (section 7, ruling 1: the weapon in hand at each shot), where a weapon swapped in mid-volley lends its behaviours
to a cast another weapon pressed. No shipped volley fires a spread, so for Shatter it is a belt; Focused Beam (PR 2)
needs the same check for real, because `knell_beam`'s right click is a volley of rays. The Lapis Crystal's own checks
(`GrantedTriggers.granted`'s weapon id, `throwOrb`'s ability id) are kept on the same reasoning and are unchanged.

**Airburst's behaviour on the Living Lantern is untouched** (its path in `CastExecutor` is not edited; `AirburstTest` and
`AirburstSeekTest` unchanged and green). What changed is only that the dev command's staging on another weapon now
reaches nothing there.

**Tests:** new `paper/.../weapon/BehaviourWeaponFilterTest` (5, over the SHIPPED enchant and weapon files): Airburst on
the lantern as before and on the Crystal Shard Staff and the Breacher nothing; Shatter Caps on the Breacher and on the
Boltor and lantern nothing; Lapis Crystal on the Lapis Staff (and its granted left click, the control) and on the Flint
Staff nothing; Kinetic Tremors in slot 1 on all 16 kinetic weapons unchanged; a null weapon id passes only Tremors.
`KineticTremorsContentTest`'s four reads now name a weapon (`boltor`). **What a cast does with no behaviour is core's,
held by `AirburstTest.withoutTheEnchantTheShotIsTheLanterns` and `ShatterCapsTest.notActiveOrOnAnotherWeaponFourChunksAsToday`:
the weapon fires as authored.**

**Mutations, each `marker present: 1  original gone: 0  lines changed: 1` against a pristine copy, restored and `cmp`
byte-identical, `markers left: 0`:** MUTFILTER (the filter passes everything: 4 of the 5 rows red, the Tremors row green,
as it must be); MUTUNIQUE (only weapon-specific behaviours pass: 2 rows here and `KineticTremorsContentTest
.onlyAnActiveBehaviouralEnchantBecomesABehaviour` red). MUTUNIQUE's first form reddened by a `NullPointerException`
(`def.weapon().equals` on a null), not an assertion; it was re-written null-safe and re-run before being counted.
**UNMUTATED, said so:** the two paper call sites passing the id (server-side; the gate's CSS row on PR 2 is the witness
that the held weapon's id reaches the filter).

**`GATE-enchant-changes.md`'s AF1-AF4 are not re-run;** the seat's brief instead re-reads ONE Airburst row on the server
(PR 2's gate, `AFR`), with the Crystal Shard Staff row (`CSS`) beside it.

## 18. PR 2: FOCUSED BEAM, THE CURSED EMERALD'S WEAPON-SPECIFIC ENCHANT (`feat/focused-beam`, on PR 1), 2026-11-24

**Ben, verbatim (the seat's brief of 2026-11-24):** "I'd like to scope the next enchantment for the Cursed Emerald: Focused
Beam. Fires one stronger beam after the wind up." / asked the beam's damage by level, whether it pierces, whether the
wind-up, mana, cooldown or range change, and whether it fires where he is looking when the wind-up ends: "1. 108/162/216
2. No piece 3. Everything the same except increase the range to 48 blocks. 4. Yes". The seat reads "No piece" as no
pierce, and 3 as the beam's range being 48 while the plain volley keeps its 32 (**the 48's scope: SEAT'S READING, OPEN
FOR BEN**).

**What the seat read at `ee46a493`, confirmed** (`FocusedBeamContentTest.theCursedEmeraldsVolleyIsAsTheSeatReadIt`, off
the loaded weapon): "Cursed Volley", cooldown 30, mana 40, on_cast `emerald_windup`, a volley of wind-up 20, 6 shots 2
apart, of a ray of range 32 drawing `emerald_beam`; on_hit a visual and 27 kinetic. Nothing disagreed.

**AS BUILT:**
1. **A fourth behaviour type, `EnchantBehaviour.FocusedBeam`, not a seam shared with Shatter Caps** (`effect:
   focused_beam`; block `range`, `damage_by_level`, `element`). Shatter acts where a spread projectile is expanded
   (`launch`); this acts where a volley fires its first shot (`volley`, then `focusedBeam`). `launch`, `launchOne`,
   `launchStone`, `shatter`, `launchToSplit` and `split` are not edited.
2. **The one ray:** at the volley's FIRST shot (the wind-up's end), off that shot's own aim, Caster (a fresh crit, the score
   read then) and behaviours. The volley's own inner ray, read from the weapon's spec at run time: its beam and tolerance
   as authored, range 48, pierce OFF. The weapon's on_hit with its damage replaced by 108 / 162 / 216 kinetic, on a derived
   ability keeping the id `cursed_emerald/right_click`. No further shot is scheduled. One primary hit.
3. **The weapon check:** acts only on a `cursed_emerald/...` cast. NOT a belt here: `knell_beam`'s right click is a volley
   of rays, and a Cursed Emerald swapped in during its wind-up lends its behaviours to the shot (ruling 1).
4. **The trace:** `FOCUSED caster=… source=… from=x,y,z range=… pressed=<t0> tick=<t1>` under mobtrace
   (`CombatWorld.traceFocusedBeam`, abstract; `FocusedLineTest`). The press's tick is read in `beginVolley` and threaded as
   a value; nothing decides on it.
5. **The loader refuses, by name:** a weapon whose right click is not a volley of rays; no `weapon:`; a level list not
   matching `max_level` (the record's); a non-positive range.

**READ, THE TWO ANSWERS THE BRIEF ASKED FOR:**
- **The aim:** the plain volley's first shot is scheduled on him `windup_ticks` after the press and reads
  `world.aimOf(casterId)` then (`ViewAim.of(getEyeLocation())`); the beam is fired from inside that same shot, off that
  same aim. The same read.
- **What interrupts a wind-up today:** his QUIT (the entity scheduler's task, retired callback null; and `volley` returns
  when `combatant` is empty) and his DEATH on the shot's tick (`volley` returns when `!alive`, which is `!isDead()`). No
  ray leaves after either. **A WEAPON SWAP DOES NOT INTERRUPT IT**: the volley fires, re-reading the held item's
  behaviours at the shot (ruling 1); swapped away from a Cursed Emerald with Focused Beam, the six bolts fire as
  authored. That Paper retires entity tasks on removal is INFERRED (the API's contract, not read in the jar); a death and
  respawn both inside 20 ticks is UNVERIFIED.

**FOR THE SEAT, ONE DISAGREEMENT IN THE BRIEF, BUILT ONE WAY AND FLAGGED:** "the Cursed Emerald that made the cast" (the
press) against ruling 1, "the weapon in hand at each shot". They differ only on a swap during the wind-up. Built to ruling
1 (the shot's read), because the volley path already reads that way; freezing it at the press would be the other ruling.

**OPEN FOR BEN (SEAT PLACEHOLDERS 2026-11-24):** the 48's scope (this beam only; the plain volley keeps 32); the beam's look
(the bolt's `emerald_beam`, unchanged; whether the one beam should look heavier); the icon (`ender_eye`); the line's
wording ("after the wind-up, fires one beam of 108 damage reaching 48 blocks"); **the Cursed Emerald's own description,
NOT EDITED, which still reads "Channel, then loose six" / "bolts down your line of sight." with the enchant active.**

**Tests:** `core/.../FocusedBeamTest` (10), `paper/.../FocusedBeamContentTest` (5), `paper/.../adapter/FocusedLineTest`
(1); `EnchantLoaderTest` (16 enchants; four weapon-specific) and `UniqueSlotContentTest` (16; the Cursed Emerald's weapon
pool `[focused_beam]`, every other gear's as before) re-pinned. **Mutations, each a single-site edit with `marker present:
1`, its needle's count down by 1, `lines changed: 1` against a pristine copy, each red, restored and `cmp`
byte-identical, `markers left: 0`:**

| mutation | the edit | reddened |
|---|---|---|
| MUTSIX | the beam fires and the volley carries on (no `return`) | 8 `FocusedBeamTest` rows, the Tremors row among them |
| MUTWEAPON | the ability-id check removed | `notActiveOrOnAnotherWeaponsVolleySixAsToday` (its only guard) |
| MUTSHOT | the beam at the second shot, not the first | 7 rows |
| MUTRANGE | the ray keeps the volley's 32 | the boundary row |
| MUTPIERCE | pierce on | the two-mob row, and the boundary row (a piercing ray walks the sweep, whose box model reaches 48.1) |
| MUTDMG | level I's damage at every level | `eachLevelsDamage` |
| MUTLOCK | every volley's aim locked at the press | `theAimIsReadAtTheWindupsEndNotAtThePress` |
| MUTPRESS | the traced press tick a constant 0 | the wind-up row (it presses at tick 7 so a zero cannot pass; first written at tick 0, which a zero would have passed, and moved before any mutation ran) |
| MUTCOST | `AbilityService` charges twice the cost | `manaAndCooldownAreChargedOnce` |
| MUTLOADER | the not-a-volley-of-rays refusal disabled | the refusal row |
| MUTLINE | the line shows level I's damage | the line row |
| MUTCONTENT | `focused_beam.yml` 216 -> 215 | 3 content rows |
| MUTPOOL | `focused_beam.yml` `weapon: knell_beam` | 2 content rows and `UniqueSlotContentTest`'s pool row (its needle counted 2 before: the file's comment also says `weapon: cursed_emerald`; the edit was anchored to `^weapon:` and changed 1 line) |

**UNMUTATED, said so:** the cooldown half of `manaAndCooldownAreChargedOnce` (no code in this PR touches the cooldown;
the row pins that the beam adds none); paper's `traceFocusedBeam` gate on mobtrace (server-side; the gate's `FOCUSED` lines
are its witness).

**The gate:** `GATE-focused-beam.md`, on this branch, carrying the one R0 for #270 and both PRs.

**THE SEAT'S TWO NOTES OF 2026-11-24 (its boot approval of `3890aaa8`), recorded after the run's readings:**
1. **The swap ruling** is recorded beside B2, in section 7's first ruling, in the seat's words: the enchant applies by
   the weapon in hand at the shot; a swap during the wind-up fires the six plain bolts.
2. **OWED: THE AIM ROW.** Ben's "4. Yes" (the beam fires where he looks when the wind-up ends) is UNREAD on a server;
   `FocusedBeamTest.theAimIsReadAtTheWindupsEndNotAtThePress` holds it. **The row that turns him during the wind-up is
   added to the next gate that touches the Cursed Emerald** (press facing east, `wait: 10`, a `tp` facing a zombie to the
   south, `wait: 30`; the south zombie takes the beam, one standing east nothing). Not run in this boot, by the seat's
   ruling.

**FOR THE SEAT, FROM THE RUN (`GATE-focused-beam.md`, its readings): every `FOCUSED` line read `tick - pressed` = 19, not
20.** The same `scheduleOn` times the plain volley's wind-up, so it is the volley's behaviour too. The fake world fires
exactly 20 later, so the unit row's 20 is the fake's. The cause is not read (two INFERRED candidates in the gate).
Nothing is changed; the seat's to rule whether it is owed a fix, a fake change, or a note.

## 19. THE ONE TICK, READ (the seat's merge approval of 2026-11-24: "READ, do not infer ... Then PROPOSE, do not build")

**The instrument:** `javap -c -p` (JDK 26.0.1) on `run/versions/26.1.2/paper-26.1.2.jar`, whose manifest reads
`Implementation-Version: 26.1.2-74-e4e17fc`, `Build-Number: 74`: the build `pom.xml` pins (`26.1.2.build.74-stable`)
and the boot log named (`Paper version 26.1.2-74-ver/26.1.2@e4e17fc`). And our own source at `c1ebabab`.

### READ, STEP BY STEP

1. **The entity scheduler (`io.papermc.paper.threadedregions.EntityScheduler`).** `schedule(run, retired, delay)` files
   the task under the key `tickCount + max(1, delay)`. `executeTick()` FIRST does `tickCount = tickCount + 1`, THEN
   removes and runs the tasks filed under the new `tickCount`. So a delay `d` runs on the `d`-th `executeTick` after it
   was filed.
2. **The global scheduler (`FoliaGlobalRegionScheduler`).** `execute(plugin, r)` is `run(plugin, …)`, which is
   `runDelayed(plugin, …, 1)`; `scheduleInternal` files under `tickCount + delay`; `tick()` FIRST increments
   `tickCount`, THEN runs what is filed under it. **The region scheduler is `FallbackRegionScheduler`**
   (`CraftServer.regionizedScheduler`), whose `execute`, `run`, `runDelayed` and `runAtFixedRate` each hand straight to
   `getGlobalRegionScheduler()`.
3. **The order inside one server tick (`MinecraftServer`).** `runServer` does `currentTick = currentTick + 1` before the
   tick; `tickChildren` then runs, in this order: `CraftScheduler.mainThreadHeartbeat()` (Bukkit tasks),
   `FoliaGlobalRegionScheduler.tick()` (the global and every region task), then `executeTick()` for every entity
   scheduler. `Bukkit.getCurrentTick()` is `MinecraftServer.currentTick` throughout the tick.
4. **Our path, from the press to the beam (our source):**
   - The selftest's `use` step runs inside the PLAYER's entity scheduler (`SelfTestRun`: a `wait: n` resumes by
     `onEntityLater(player, this::advance, n)`), on tick **T**, after T's global tick.
   - `WeaponFire.attempt` charges the cost and the cooldown there, then hops the cast: `adapters.scheduler().onRegion(eye,
     …)` = `getRegionScheduler().execute(…)` = the global `runDelayed(…, 1)`, filed under global `g + 1`, where T's
     global tick has already made it `g`. **So the cast (`CastExecutor.execute`, `commit`, `beginVolley`) runs at
     T + 1**, in that tick's global phase, BEFORE T + 1's entity phase.
   - `beginVolley` reads `pressed = Bukkit.getCurrentTick()` = **T + 1**, and files the first shot by
     `scheduleOn(player, 20)` = `onEntityLater` = the player's `runDelayed(…, 20)`, under `e + 20`, where `e` is the
     player's `tickCount` from T (T + 1's `executeTick` has not run yet).
   - T + 1's `executeTick` makes it `e + 1`; the task runs when it reaches `e + 20`: **tick T + 20**. The `FOCUSED` line
     reads `tick` = T + 20.
   - **So `tick - pressed` = (T + 20) - (T + 1) = 19, exactly as read six times. From the press (T) to the beam, 20.**
5. **Which delays run one short, and of what:** an ENTITY-scheduler delay `d` (`scheduleOn`) filed from the GLOBAL or
   region phase (or the Bukkit heartbeat) of a tick runs `d - 1` ticks after it was filed, because that tick's
   `executeTick` still counts. Filed from inside the entity phase, it runs `d` later. A GLOBAL/region delay `d`
   (`schedule`, `scheduleCancellable` = `onRegionLater` = global `runDelayed`) filed from the global phase or after it
   runs `d` later; filed from the heartbeat, `d - 1` (nothing of ours files from the heartbeat that I found; NOT
   audited across all of paper/). **In `CastExecutor`, which runs in the global phase after the hop, exactly two calls
   are entity-scheduled at the cast: the volley's wind-up (`beginVolley`) and the Knell Beam's warning flashes (`warn`).
   Both land one tick short OF THE CAST and exactly on time FROM THE PRESS**, because the hop already cost one. Every
   other cast-time delay (`schedule`, `scheduleCancellable`: the ray's and sweep's segments, the cone, the orb's flight,
   rest, life, beat and backstop) lands exactly `d` after the cast, which is `d + 1` after the press. Delays filed later
   from inside the entity phase (a volley's `interval_ticks`, the selftest's `wait:`) are exact.
6. **The fake world** (`FakeWorld.scheduleOn`, `schedule`): files `now + d` and runs it at exactly `now + d`, with no
   phases and no hop (`CastExecutor.execute` is called directly). So in the fake, press = cast and press -> beam = 20, as
   on the server; cast -> beam is 20 in the fake and 19 on the server, and `FOCUSED`'s `pressed` was the CAST.

**NOT READ, said so:** where a REAL client's right click is handled. If its packet is processed between ticks or in the
entity phase (after that tick's global tick), it is the selftest's case: press T, cast T + 1, beam T + 20. If it were
processed before that tick's global tick, the cast would run the same tick and the beam would be 19 after the press.
INFERRED to be the first; not read in the jar.

### THE PROPOSAL (do not build; the seat rules)

- **(c) IS RIGHT IN SUBSTANCE: NOTHING REAL IS SHORT FROM THE PRESS.** The wind-up is 20 ticks from the press, as Ben's
  "Everything the same" and the content's `windup_ticks: 20` say. What read 19 is the trace: its `pressed` is taken at
  the cast, one tick after the press, because of `WeaponFire`'s region hop. The two reads are not "different phases of
  one tick" as candidate (c) was worded in the gate; they are ticks T + 1 and T + 20, each read correctly.
  **Change for shipped weapons: none. Change for gates already read: none**; `GATE-focused-beam.md`'s six misses stand as
  a prediction taken from the fake's clock (press = cast). **The one change worth making, as a small PR the seat may
  rule:** stamp the press tick at the press (`WeaponFire.attempt`, before the hop) and carry it to the trace, so a
  `FOCUSED` line reads `tick - pressed` = 20; or rename the field `cast=`. Either is trace-only.
- **(a), the adapter adds a tick: WRONG.** It would make every entity-scheduled cast-time delay land one tick LATE from
  the press: the Cursed Emerald's wind-up 21, the Knell Beam's 24 becomes 25 and its warning flashes shift. It would
  also make delays filed from the entity phase (a volley's interval, the selftest's waits) one tick long.
- **(b), the fake corrected to the server's clock: NOT NOW.** Correct in principle, but it means modelling the hop and the
  three phases (a cast one tick after its press; an entity delay filed in the global phase landing one short). Every
  core test that counts ticks after a press would move by one, with no change in shipped behaviour. If the seat wants
  the fake honest about the hop, the smaller form is to model ONLY the hop (the cast one tick after the press) and keep
  `scheduleOn` exact, and that is not what the 19 asks for.

## 20. THE SEAT'S BRIEF OF 2026-11-24 ("THE GUARANTEED WEAPON ENCHANT, FOR THE MAGE WEAPONS; MANA BURN; AND THREE SMALL CONTENT CHANGES"): PR A, THE PRESS TICK (`feat/press-tick`, on #274)

**Ruled by the seat at #274's reading: (c), nothing real is short; owed, the trace tells the truth.** Built in the
carried form, not the rename: `WeaponFire` stamps `Bukkit.getCurrentTick()` on the player's thread BEFORE its `onRegion`
hop and hands it to `CastExecutor.execute(success, pressTick)` (and `executeFan(…, pressTick)`), which carries it through
`dispatch` to `beginVolley`. The one-argument forms pass `world.currentTick()`, so every caller with no hop (a stone, a
dev cast, a vanilla shot, every core test) is unchanged. **On the server a `FOCUSED` line now reads `tick - pressed` = 20
(press T, beam T + 20; section 19, step 4), not 19.** Nothing decides on the value.

**Tests and mutation:** `FocusedBeamTest.theTracedPressTickIsTheOneStampedAtThePressNotTheCasts` (press 5, cast 7, beam
27 in the fake; no two equal). MUTPRESSCAST (`execute` passes the cast's tick in place of the press's): marker present 1,
original gone 0, lines changed 1 against a pristine copy; that row red; restored, `cmp` identical, markers left 0.
**UNMUTATED, said so:** `WeaponFire`'s stamp (server-side; the next gate's wind-up rows are its witness).

**The other owed items of the seat's approval, here:** the approved `verification.md` line, in a commit of its own; the
dated note on `GATE-focused-beam.md` (its six misses stand). **Carried as UNREAD, not audited** (the seat's word: only
when a second timing surprise appears): where a REAL client's right click is handled (INFERRED: the same phase as the
selftest's press), and whether any paper caller files a delay from the Bukkit heartbeat.

### Ben's words of 2026-11-24, VERBATIM (the brief's; recorded once here for sections 20 to 26)

> "There is a fundamental flaw with the weapon enchantments that we need to fix. If you pick it, it cannot be unpicked if
> there's only one. There's a few ways we could go about this. What we're going to do is add one that comes with every
> weapon guaranteed. This will just be the normal attack of the weapon and will slightly increase the stats."
> "Let's start with the mage weapons."
> "Flint Staff - Large Sparks - Increases the damage by +4/8/12
>  Crystal Shard Staff - Sharp Crystals - Increases the damage by +3/6/9
>  Living Lantern - Hot Potato - Consecutive hits increase damage by +5/10/15% each hit (+60% cap)
>  Lapis Staff - Polished Scepter - Increases range by 4/8/12 blocks
>  Cursed Emerald - Increases volley count to 4/5/6 (Side note decrease the normal volley count to 3)
>  Knell Beam - Early Toll - Decreases wind-up time by 0.1/0.2/0.4
>  Blaze King's Staff - leave as is for now, this weapon is due for a massive rework
>  Volley Stone - Dev weapon slated for deletion"
> "1. In the lore of the Flint staff, change the word "Slamming" to "Flinging"
>  2. Delete the volley stone in this pass
>  3. A Fire Element enchantment for the Flint staff and Living Lantern (Fire/Mage weapon specific, goes in the second
>  slot) Mana Burn, Increases Scorch and Ignite damage by 10/20/30%"
> Asked the Cursed Emerald enchant's name; Hot Potato's rules; whether these cost XP or come already active; whether Mana
> Burn may also reach Blaze King's Staff; and the same trap in the second column: "1. Echo Beam 2. Same Target, 3s
> rolling just like Kinetic Tremors, no 3. Yes they cost XP, no they don't come with these 4. Yes, BKS can take it too
> 5. We'll cover that later, the difference is the second column doesn't have trade off like the first column does so
> it's not as big of a deal"
> Asked whether the streak counts only hits of the last 3 seconds (A) or is a chain kept alive while each hit lands
> within 3 seconds of the one before, climbing to the cap (B): "B, also side note, change the name of Hot Potato to
> Wildfire"

## 21. PR B, THE VOLLEY STONE DELETED: STOPPED (no branch), WITH WHAT HANGS ON IT

**The brief's rule: "If anything real hangs on it (a mechanism only it exercises, an item class, a stone of the build
system), STOP ON THIS PR ALONE, say what, and carry on with the others."** READ at `c1ebabab`, three things only it
exercises in shipped content, each checked by a command:

1. **The per-definition `unscored: true` declaration.** `git grep -n 'unscored: true' -- paper/src/main/resources/content`
   returns `volley_stone.yml:70` alone (and `WeaponLoader`'s `unscored` section says "Only volley_stone declares it").
   Deleted, the instance-level exclusion (`WeaponLoader` reads it; `GearScore.carriesScore`, `PaperCombatWorld.heldScore`,
   `GearScoreItems`, `WeaponLore`, `AnvilMenu`, `Side` compose or render it) keeps its code and loses its only shipped
   user; `WeaponLoaderTest.theUnscoredDeclarationReachesTheDefinitionAndOnlyVolleyStoneCarriesIt` reads the shipped file
   and goes red. Whether the mechanism is kept with a test-only fixture or deleted with it is a ruling.
2. **A volley whose inner cast is a PROJECTILE** (Scatter). Every other shipped volley's inner cast is a ray
   (`cursed_emerald`, `knell_beam`; READ by `grep -A3 'of:'` over the weapon files). `AbilitySchema` says projectile was
   "admitted on day one and exercised by volley_stone". Deleted, `CastExecutor.fireInner`'s projectile arm has no
   shipped user (core tests still build one).
3. **The only EXOTIC item.** `grep -l 'rarity: exotic'` returns `volley_stone.yml` alone; it is the dev loot source's only
   exotic pool (`cascade.yml` `dev_sources`), and `LootLoader` refuses the WHOLE file for an id that does not ship;
   `GATE-loot-roll.yml` probes it twice.

**Its other names, which would be prose or tests, not behaviour:** comments in core `Side` and `GearScore`, paper
`Keys` (two), `PaperCombatWorld`, `StoneItems`, `AbilitySchema` (two), `WeaponLoader`, `AnvilMenu`, `GearScoreItems`
(two), `QuiverItems`, `WeaponFire`, `WeaponLore`, `boltor.yml`, `cursed_emerald.yml` (two), `visuals/volley_beam.yml` and
`visuals/volley_cast.yml` (its only users); tests `WeaponLoreLinesTest`, `CascadeLootTest`, `LootTallyTest`,
`GearScoreTest` (core, own fixtures), and paper `VolleyFixtureTest` (whole class), `WeaponLoaderTest`,
`GearScoreWiringSignatureTest`, `WeaponLoreTest` (own fixture), `CursedEmeraldContentTest`, `LootLoaderTest`,
`UniqueSlotContentTest`, `StoneIdentityTest`, `AbilityLoaderTest`, `golden-lore.txt`; and `verification.md`'s R0c
clause ("no `[Rpg]` WARN beyond `volley_stone`'s known pair", which goes in the same commit as the deletion).

**NOT done, so: every gate on this stack keeps R0c's carve-out, and the "no volley_stone anywhere" row is not run.**
Nothing else in the stack depends on it.

## 22. PR C, TWO CONTENT CHANGES (`feat/mage-content-changes`, on PR A)

- **`flint_staff.yml`:** "slamming" becomes "flinging" (BEN). The flavor now reads "Prehistoric magicians learned you
  could" / "create fire by flinging mana into flint."; the break is where it was.
- **`cursed_emerald.yml`:** `shots: 6` becomes `3` (BEN: "decrease the normal volley count to 3"); its description's
  "six" becomes "three", every other word unchanged (SEAT PLACEHOLDER 2026-11-24 for the wording, OPEN FOR BEN). The
  derived cooldown floor is now 20 + (3 - 1) x 2 = 24; the authored 30 is KEPT (Ben named no cooldown change; it boots
  with no warning; the tooltip's 1.5 s is the real cooldown, max(30, 24)), with a dated note in the file. **Focused Beam
  is untouched.** The plain volley is now **3 x 27 = 81** at score 100 against Focused Beam's **108 / 162 / 216**, a
  comparison Ben has been shown.

**Tests:** the pins moved to Ben's values (`BoltorSoundFlintTextTest` 2 lines, `CursedEmeraldContentTest` 4,
`FocusedBeamContentTest` 3); `golden-lore.txt` regenerated, predicted and read **3 insertions / 3 deletions** (the flint
line, "loose three", "27 x 3"); `FocusedBeamTest`'s fixture, which says it is copied from the file, moved to 3 shots (7
lines). **Mutations, each marker present 1, original gone 0, lines changed 1, restored `cmp` identical, markers left
0:** MUTSHOTS (`shots: 4`: 4 rows red across `CursedEmeraldContentTest`, `FocusedBeamContentTest`, `GoldenLoreTest`);
MUTFLING ("slamming" back: `BoltorSoundFlintTextTest` and `GoldenLoreTest` red).

## 23. PR D, THE GUARANTEED CANDIDATE AND FIVE TUNES OF A WEAPON'S OWN CAST (`feat/guaranteed-cast-tunes`, on PR C)

**THE FLAW (READ by the seat at `c1ebabab`):** `EnchantMenu` only calls `withActive`; nothing in the table calls
`withoutActive`, so a weapon whose slot 0 offers ONE behavioural enchant cannot go back to its plain attack once it is
active. **Ben's fix is content: a guaranteed candidate beside it. No deactivate click is added.**

### G1 to G3, AS BUILT

- **`guaranteed: true`** on an enchant file (`EnchantLoader`'s `guaranteed` section: a YAML boolean or refused by name),
  carried on `EnchantDefinition.guaranteed` and `EnchantRoll.Rollable.guaranteed`. The record refuses it on an enchant that
  names no weapon (both records, by name).
- **The roll (`EnchantRoll.roll`):** each slot's pool is split; its guaranteed enchants are its FIRST candidates, in roster
  order, with no draw; the cascade then counts the REST, `cascade.count(min(rest, MAX_CANDIDATES - guaranteed), draws)`, and
  picks as before. An empty rest takes no draw (`CandidateCascade.count` takes none for a pool of 0, READ). A weapon no
  guaranteed enchant names rolls exactly as before (draw for draw: `aWeaponNoGuaranteedEnchantNamesRollsAsToday`).
- **The loader** refuses, by name and for every one of them, more guaranteed enchants for one weapon than
  `MAX_CANDIDATES` (a cross-file rule, checked after every file is parsed and before any is registered).
- **G2: nothing else is special.** Locked at level 0, priced by the same curve, three levels, activated by the same click
  (`EnchantMenu` is NOT edited): activating the guaranteed one makes it the slot's active candidate and the behavioural one
  beside it stops acting, which is the fix. **G3:** no migration; items already rolled keep their slot 0.

### G4: THE ONE SEAM, AND ITS SCHEMA

`EnchantBehaviour.CastTune(weaponId, quantity, valueByLevel)`, `effect: cast_tune`, applied by ONE pure function,
`core CastTunes.tune(ability, behaviours)`, to the ability whose id is `<weapon>/right_click` only (a stone cast, another
gesture, another weapon's cast, a granted trigger come back the same instance). **Where it is applied (`CastExecutor`):**
at the commit, over the behaviours the commit froze (every shape is dispatched with the tuned ability; the Caster's
headline damage is re-read off the tuned on_hit, so scorch's cap basis is the tuned hit); and **at each volley shot, off
the behaviours THAT SHOT reads (ruling 1)**, for its damage, range and the count the volley runs to. **A volley's wind-up
is the press's**: the tuned spec times it in `beginVolley`, since nothing can re-read a wind-up already running.

```yaml
cast_tune:
  quantity: damage              # damage | range | shots | windup
  value_by_level: [4, 8, 12]    # one per level
```

| quantity | what it does | the right click must have | refused values |
|---|---|---|---|
| `damage` | ADDED to the authored `amount` of every literal `damage` in on_hit, before every other term (G5) | a literal damage | not above 0 |
| `range` | ADDED to a ray's range (a volley's inner ray's too) | a ray, or a volley of rays | not above 0 |
| `shots` | SETS a volley's shot count | a volley | not a whole number, below 1 |
| `windup` | SUBTRACTED from a volley's wind-up, in ticks | a volley | not a whole number, below 1 |

**The loader refuses, by name (ruling H's spirit):** no `weapon:`; a weapon it cannot see; a weapon with no right click; a
quantity that is not one of the four; a quantity the right click does not have; and **any level that would make the cast
itself invalid, found by BUILDING the tuned cast for every level at load** (`CastSpec.Volley`'s own refusals: a wind-up
below 0, a warning flash at or after the shortened wind-up). The level count against `max_level` is the record's.

**G5, every sum names the term:** `sent = (((amount + TUNE) x score / 100) x enchantMultiplier + classDamageBonus +
weaponLevelDamage) x chargeScale x critMultiplier`. EXECUTED in `CastTuneTest`: at score 150, Large Sparks III reads
(20 + 12) x 1.5 = 48, where the addition after the score would read 42 and no enchant 30.

### THE FIVE FILES, AS COMMITTED (comments stripped)

```yaml
# large_sparks.yml
display_name: "Large Sparks"
icon: flint
max_level: 3
effect: cast_tune
class: universal
weapon: flint_staff
guaranteed: true
cast_tune:
  quantity: damage
  value_by_level: [4, 8, 12]
# sharp_crystals.yml: "Sharp Crystals", icon amethyst_cluster, weapon crystal_shard_staff, damage [3, 6, 9]
# polished_scepter.yml: "Polished Scepter", icon lapis_lazuli, weapon lapis_staff, range [4, 8, 12]
# echo_beam.yml: "Echo Beam", icon prismarine_crystals, weapon cursed_emerald, shots [4, 5, 6]
# early_toll.yml: "Early Toll", icon bell, weapon knell_beam, windup [2, 4, 8]
```

Every other key of the four is the first's (`max_level: 3`, `effect: cast_tune`, `class: universal`, `guaranteed: true`).
Early Toll's arithmetic, in its file: Ben's 0.1 / 0.2 / 0.4 s x 20 ticks = 2 / 4 / 8.

### READ: THE KNELL BEAM'S WIND-UP, AND WHAT HANGS ON IT (`knell_beam.yml`, `CastExecutor.beginVolley`/`warn`)

- **The locked aim** is captured AT THE CAST (`beginVolley`: `locked = castAim`), as values; the wind-up's length does not
  touch it. At 22, 20 and 16 the bolt still fires down the line he faced at the cast.
- **The warning flashes:** `flash_at: [0]`, ONE flash, drawn inline on the cast frame (`warn`: tick 0 runs `draw.run()`), so
  there is no spacing to move. `CastSpec.Volley` refuses a flash at or after the wind-up; 0 is inside 22, 20 and 16. A
  cut to 24 would put it AT the shot and is refused at load (`windup_too_long`).
- **The cooldown stays 48** (told to Ben); the derived floor is the wind-up plus nothing (one shot), 22 / 20 / 16, all below.
- **The clock (section 19, with PR A):** the press on tick T, the cast at T + 1 (the hop), the first shot filed by
  `scheduleOn(…, windup)` from the global phase and run at **T + windup**: so the new `VOLLEYSHOT … shot=0 pressed=T tick=…`
  line (under mobtrace, `CombatWorld.traceVolleyShot`) reads `tick - pressed` = **16** at Early Toll III and **24** with none.

**Polished Scepter beside Lapis Crystal:** only one acts at a time (one slot). With Polished Scepter NOT active the beam
is 26 and the crystal's shots read the beam's authored 21, unchanged. With it active, an orb thrown BEFORE the swap still
rests out its life; a beam into it fires the crystal's shots at the same authored 21 (the tune changes the range only).

### OPEN FOR BEN (SEAT PLACEHOLDERS 2026-11-24)

1. The five icons: `flint`, `amethyst_cluster`, `lapis_lazuli`, `prismarine_crystals`, `bell`.
2. The five lines (`EnchantEffectLine`, the `CAST_TUNE` arm): `+12 damage`; `+6 damage`; `+12 blocks of range`;
   `volleys of 6 shots`; `-0.4s wind-up` (each level's own number).
3. **The weapons' own tooltips are NOT edited with a tune active:** the Flint Staff still reads its authored 20 (the
   enchant's line says +12), the Cursed Emerald "27 x 3".
4. Which of the two weapon-specific candidates should LEAD the slot: built with the guaranteed one first (the brief's).

### Tests and mutations

New: `core/.../enchant/EnchantRollGuaranteedTest` (5), `core/.../CastTuneTest` (10), `paper/.../CastTuneContentTest` (6), a
`VOLLEYSHOT` row in `FocusedLineTest`; re-pinned: `EnchantLoaderTest` (21 enchants; nine weapon-specific) and
`UniqueSlotContentTest` (21; the five weapons' pools). **Seventeen mutations, each marker present 1, original gone 0,
lines changed 1 against a pristine copy, each red, restored `cmp` identical, markers left 0:**

| mutation | the edit | reddened |
|---|---|---|
| MUTGUARFIRST | no candidate treated as guaranteed | 3 `EnchantRollGuaranteedTest` rows |
| MUTGUARCAP | the cascade counts the rest uncapped | `theSlotNeverHoldsMoreThanTheTable` (by `EnchantSlot`'s own refusal, an error) |
| MUTTUNEWEAPON | the `<weapon>/right_click` check removed | `anotherWeaponsCastIsUntouched` |
| MUTTUNEDMG | the damage addition is + 0 | 3 rows |
| MUTTUNERANGE | the range addition is + 0 | `aRangeTuneLengthensTheRay` |
| MUTTUNESHOTS | the shot count kept | `aShotsTuneSetsTheVolleyCount` |
| MUTTUNEWINDUP | the wind-up cut is 0 | 2 rows |
| MUTPERSHOT | the count read at the press, not each shot | `theShotCountIsReadAtEachShot` |
| MUTPAYLOAD | the headline never re-read | `aStatusTheTunedCastAppliesCarriesTheTunedHeadline` |
| MUTLOADERHAS | the "has the quantity" refusal disabled | the refusal row |
| MUTLOADERBUILD | no level built at load | the refusal row (`windup_too_long`, `zero_shots`... its first miss) |
| MUTGUARREAD | `guaranteed` always read false | 4 `CastTuneContentTest` rows |
| MUTOVERFLOW | the per-weapon cap 99 | the overflow row |
| MUTWINDUPLINE | the line's seconds / 10 | `theLines` |
| MUTROSTERG | the roster drops `guaranteed` | `slotZeroOverManyRolls` |
| MUTSPARKS | `large_sparks.yml` 12 -> 13 | 3 content rows |
| MUTVOLLEYLINE | `pressed` and `tick` swapped in the line | the `VOLLEYSHOT` row |

**UNMUTATED, said so:** paper's `traceVolleyShot` gate on mobtrace (server-side; the gate's `VOLLEYSHOT` lines are its
witness); `executeFan`'s tuning (no fanned weapon has a tune; it is the same two calls as `execute`).

## 24. PR E, WILDFIRE (`feat/wildfire`, on PR D)

**As built.** `EnchantBehaviour.HitChain(weaponId, windowTicks, capPercent, stepPercentByLevel)`, `effect: hit_chain`;
the pure store `core HitChains` beside `ShockwaveStreaks`, held by the server's one `BehaviourHits` (`chains()`); the hook
`BehaviourHits.chainFactor`, asked through the new abstract `CombatWorld.hitChainFactor` by BOTH damage arms of
`EffectApplier`, INSIDE their gate (a hit that lands) and BEFORE `applyDamage`, so the number dealt, traced (PLAYERHIT
`sent=`) and reported is the multiplied one.

- **The chain:** per player, enchant and MOB (Ben: "Same Target"). A hit extends it when it lands within 60 ticks of the
  PREVIOUS hit on that mob (Ben: "B"); otherwise it starts again at this hit. **The boundary: exactly 60 after starts
  again** (SEAT PLACEHOLDER 2026-11-24, OPEN FOR BEN; the same side Kinetic Tremors' window takes). Nothing else resets it.
- **The factor of the n-th hit:** `1 + min(0.60, step x (n - 1))`, step 0.05 / 0.10 / 0.15 by level; the first hit has
  none. **EXECUTED (jshell, 2026-11-24), level III on the Lantern's 45:** 45, 51.74999999999999, 58.5, 65.25, 72.0, 72.0
  (PLAYERHIT prints `%.3f`: 51.750).
- **Which hits:** only a caster that `countsAsHit` (so a scorch tick, Ignite, a shockwave, a crystal's shot and every
  secondary damage never extend a chain and are never multiplied: READ, scorch and Ignite never pass through
  `EffectApplier` at all, and `asSecondary` / `asNotAHit` clear `countsAsHit`), on a target that is not a player, whose
  caster's source is the chain's weapon's (`living_lantern/...`).
- **Forgotten** on the mob's death (`RpgListeners.onBehaviourTargetDeath`), on his quit (`onQuit`), and a chain 60 ticks
  idle on his next hit (the prune).
- **Its term in the sum:** `sent = hitBase x chargeScale x critMultiplier x WILDFIRE`, with `hitBase = (amount x score /
  100) x enchantMultiplier + classDamageBonus + weaponLevelDamage`. **Against the other percent terms:** the damage
  enchant's percent (Attunement) multiplies the SCALED BASE only, before the flat bonuses; Wildfire multiplies the WHOLE
  dealt amount, the flat class and level bonuses included, like the crit.
- **What it does to scorch's cap (READ, `BukkitCombatant.applyDamage`):** the cap basis is the dealt amount divided by the
  crit (`declaredMagnitude = amount / crit.multiplier()`), so it INCLUDES Wildfire's factor: at x1.60 the Lantern's burn cap
  is 0.5 x 72 = 36 against 22.5. A tick is `min(5% of max, cap)`, so on a mob whose 5% is below 22.5 (any mob of 450 max
  health or less) nothing a player sees moves.
- **Airburst Flames shares slot 0** (pinned: `WildfireContentTest.itSharesSlotZeroWithAirburstFlames`), so its bursts
  never meet a chain; the roll offers Wildfire FIRST with Airburst beside it (`CastTuneContentTest.slotZeroOverManyRolls`).

**A FINDING, FIXED IN THIS PR:** MUTCHAINWIN (`<` to `<=` on the extend test) stayed GREEN. The 60-tick boundary was held
at TWO sites, the idle prune (which runs first) and the extend test, and each masked a mutation of the other. The extend
test is gone: the prune is the one site (`HitChains.hit`'s comment says why), and MUTCHAINWIN2 on it reddens.

```yaml
# wildfire.yml, comments stripped
display_name: "Wildfire"
icon: blaze_powder
max_level: 3
effect: hit_chain
class: universal
weapon: living_lantern
guaranteed: true
hit_chain:
  window_ticks: 60
  cap_percent: 60
  step_percent_by_level: [5, 10, 15]
```

**OPEN FOR BEN (SEAT PLACEHOLDERS 2026-11-24):** the boundary (exactly 60 starts again); the icon (`blaze_powder`); the line
`+15% per hit on one foe within 3s of the last (max +60%)` (level III's).

**Tests:** `core/.../enchant/HitChainsTest` (6), `core/.../WildfireTest` (5), `paper/.../WildfireContentTest` (4); the
roster re-pinned to 22 (`EnchantLoaderTest`, `UniqueSlotContentTest`), the Lantern in `CastTuneContentTest`'s many-rolls
row. **Mutations, each marker present 1, original gone 0, lines changed 1, red, restored `cmp` identical, markers left 0:**
MUTCHAINWIN2 (the prune's `>=` to `>`: 2 rows), MUTCHAINCAP (no cap: 2), MUTCHAINFIRST (`n` for `n - 1`: 8), MUTCHAINTARGET
(keyed by player: 3), MUTCHAINSEC (`countsAsHit` ignored: the secondary row), MUTCHAINWEAPON (the weapon check removed: the
other-weapon row), MUTWILDHOOK (the Damage arm multiplies by 1.0: 3 rows), MUTWILDCAP (`cap_percent: 61`: 2), MUTWILDLINE
(the window shown /10: the line row). MUTCHAINSEC's first run used a two-line needle that cannot match across CRLF: 0
occurrences, ABORTED by the harness before any edit, then re-run on one line. **UNMUTATED, said so:** the two forgets in
`RpgListeners` (listeners; their logic is `HitChainsTest`'s); the WeaponDamage arm's hook (no shipped chain weapon reads
it; its line is the Damage arm's).

## 25. PR F, MANA BURN: STOPPED (no branch), WITH THE READ AND A PROPOSED SPLIT

### READ: HOW SCORCH AND IGNITE ARE BUILT (at `c1ebabab`)

- **`scorch.yml`** declares only `kind: scorch`; the numbers are core `Scorch`'s: a tick every `PERIOD_TICKS` 20 for
  `DEFAULT_DURATION_TICKS` 120 (six ticks), each `min(RATE_PER_SECOND 0.05 x victim max, cap)`; a hit buys stacks
  (`stacksFor`: one per 2 landed), read ONLY as a yes/no gate.
- **Where a scorch is applied:** INSIDE the damage port's handle, not in `EffectApplier`. `BukkitCombatant.applyDamage`
  drains the store, then `ElementAccrual.forHit` decides accrual from the hit's element (fire's `applies_status: scorch`)
  and `ctx.scorch().apply(id, …, stacks, cap, sourceId, duration, element, depth)`. **The cap is `CAP_FRACTION` 0.5 x the
  hit's DECLARED magnitude** (`amount / crit`, so the charge, the gear score, the flat bonuses and now Wildfire are in it;
  the crit is not).
- **Two appliers (`DotStatus.apply`'s refresh arm):** the window refreshes; `cap = max(old, new)` (the seat's 2026-11-22
  ruling); `applierId` (the credit) and the element = the NEWEST. A refresh never burns.
- **Ignite:** on a scorched mob's death, `RpgListeners.onEntityDeath` reads the scorch's applier and depth, forgets the
  scorch, and calls `Ignite.detonate(at, applier, victim, depth + 1)`: 20 ticks later, `DAMAGE` 6.0 fire, `DefenseRule.APPLIES`,
  to every combatant `combatantsNear(at, RADIUS 4.0)` except the corpse and **EXCEPT EVERY PLAYER** (`c.state().player()`
  is skipped). Credited to the scorch's applier (whose it is: the newest). Chains to depth 4. A second clause in
  `BukkitCombatant` ignites an UNSCORCHED mob killed by a fire hit, credited to the hit's dealer.
- **Ignite and a player: it cannot damage one, at any range** (the player skip, READ). On the server `combatantsNear` is a
  BOX of half-width 4 (corners at 4 x sqrt(3) = 6.93), not a sphere; the skip makes the box irrelevant for him.

### WHY IT STOPS (the brief's rule: "a step bigger than it reads: STOP on THAT PR with a split proposed")

The seat's ruling stamps the bonus "when a hit from the weapon carrying the active enchant applies" the scorch. **The
scorch is applied inside `BukkitCombatant.applyDamage`, where the caster's behaviours never arrive**: the port carries the
amount, the dealer's id, the crit, the defence rule, the element and a `HitAccrual` (rule and depth). So the stamp needs a
value carried THROUGH THE DAMAGE PORT (and `FakeWorld`'s handle), then `DotStatus.Active`, `ScorchStatus.apply`, the death
listener's read and `Ignite.detonate`'s signature: an engine change across five classes beyond Mana Burn's own hook, on
the path every fire hit and every Ignite takes.

### PROPOSED SPLIT (each its own PR, on E or D; F does not depend on E)

- **F1, the stamp carried:** `HitAccrual` (the per-hit descriptor the port already carries) gains `burnBonusPercent`;
  `EffectApplier`'s Damage arm sets it from an active `mana_burn` behaviour on the caster (a primary hit by the weapon
  carrying it); `BukkitCombatant` passes it to `scorch().apply`; `DotStatus.Active` keeps it. Unit-tested in core on the
  fake's handle (which records the accrual it was handed).
- **F2, the tick and Ignite:** the tick deals `min(5% of max, cap) x (1 + bonus/100)` (**the TICK, not the cap: PROPOSED**,
  because a tick is `min(5% of max, cap)` and the 5% is the smaller on every mob of 450 max health or less under the
  Lantern's 45 (cap 22.5), 200 or less under the Flint Staff's 20 (cap 10), 300 or less under Blaze King's Staff's 30
  (cap 15), each at score 100, so a raised cap shows a player NOTHING there; a raised tick shows 5.0 -> 6.5 at
  level III on a 100-health mob); `RpgListeners` reads the stamp
  with the applier and depth before the forget, and `Ignite.detonate` deals `6 x (1 + bonus/100)` = 7.8 at III. The
  unscorched-kill clause has no scorch, so no stamp: unchanged. **Two appliers (PROPOSED, the seat's to rule):** the stamp
  follows the CREDIT (newest wins), because it is the newest applier's enchant that owns the burn and its Ignite; the cap
  stays the larger. So a plain hit refreshing a Mana Burn scorch drops the stamp to 0, and a Mana Burn hit on a plain
  scorch stamps it.
- **F3, the content:** `mana_burn.yml`, unique (`element: fire`, `class: mage`, NOT guaranteed), three levels 10 / 20 / 30
  (BEN), eligible on the Flint Staff, the Living Lantern and Blaze King's Staff (every fire mage weapon: READ, `grep -l
  '^element: fire'` over the weapons with `class: mage`), and on no other gear (the unique pool's element + class filters).

**Nothing built.** The gate on top therefore carries no Mana Burn row.
