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
