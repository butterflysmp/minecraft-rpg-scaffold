# PLAN: Smolder, the Fire Mage's ultimate, and water puts out Scorch

**Status: PART 1 BUILT on `feat/smolder` (not booted, not merged). PART 2 BUILT on `feat/water-douses-scorch`
(section 6; not booted, not merged). The gate is `feat/smolder-gate`, last in the stack.** Stacked on `feat/regen-tooltips-sheet-unit` (#336, `d696b50d`), from master
`16713667`.

Sources: Ben's words of 2026-11-27 (relayed; quoted below), the seat's Smolder brief of 2026-11-27 (Z1 to Z11, Y1 to Y7,
the gate), CC's three STOPs the same day, and the seat's rulings on them (A1 to A6). This file is the account; the
briefs are not committed.

## 1. Ben's words, verbatim, 2026-11-27

> "Fire Mage Ultimate: Smolder, a rework of the old repo's Blaze Kings Staff secondary ability."

> "1. 160 Damage Per pulse. 2. 12 Block Radius 3. Mana Cost is 80, Cooldown is 300 Seconds 4. Each Pulse originates
> from the caster 5. Yes, Water should be able to put out every fire ability that scorches and ignites 6. Every hostile.
> No Summons and No players for now just like the summoner weapon. We'll expand on this during the PVP pass 7. Yes it
> applies scorch 8. Instead of one burst, it's a burst of 4 pulses every 3 seconds. Each pulse does the 160 damage and
> scorches targets. 9. Leave it alone for now"

> "1. A, B, and D, we'll come back to C when we do our Water Element pass 2. The moment you cast and then 3 6 and 9
> Side note, the look: Instead of the old smolder look, I Want a ring of particles to come from the casters feet and
> expand outward to the edge of the radius. This ring does NOT move with the caster. The origin of new pulses DOES. I'd
> like each pulse to take about 1.5 seconds to reach the edge."

NOT STATED by Ben: whether a wet mob is immune to new Scorch (Y3, a SEAT PLACEHOLDER).

## 2. The STOPs and their rulings

| STOP | READ | ruling |
|---|---|---|
| 1 | A fire hit applies Scorch BY THE ELEMENT (`ElementAccrual.forHit`): cap = `(amount / crit) x Scorch.CAP_FRACTION 0.5`, window `Scorch.DEFAULT_DURATION_TICKS 120`. An authored `status: scorch` caps at the UNHALVED `payloadDamage` (`BukkitCombatant.applyStatus`). The brief had cap 160 and `duration_ticks 80`. | **A1**: the damage only, Scorch by the element (cap 80 uncrit, 120 ticks, restarted by each hit). **A2**: `scorch.yml`'s comment corrected. |
| 2 | `LoadoutResolution`: an id the pool no longer offers reads EMPTY, never the default slot by slot ("a visible hole"; `savedEmptySlotsStayEmpty`). Z8 wanted the default. | **A3**: Z8's fallback withdrawn; the existing rule stands. |
| 3 | Scorch's FLAME rides vanilla `fireTicks` (`setFireTicks` at accrual and in `applyStatus`); only its damage is our own clock. | **A4**: a douse clears the flame too (Part 2). |
| 4 (Part 2) | Y4 said an unscorched body "does not ignite, by the existing rule". `BukkitCombatant`'s Ignite SECOND clause detonates any body a FIRE hit kills, scorched or not: `!wasScorched && dealt > 0 && newCurrent <= 0 && accruesScorch(...)`. | **E1**: a wet body does not ignite by either clause (Y4's "add no second gate" withdrawn). **E2**: a splash is a moment, not a state. **E3**: an armed detonation is not cancelled. **E4**: a cauldron does not douse. **E5**: K5 stays. **E6**: Part 1 accepted on the report. **E7**: the gate. |

## 3. Part 1: Smolder (the seat's Z1 to Z11 as A1 to A6 left them)

### 3.1 The shape: `CastSpec.Pulses` (Z1)

`type: pulses` with `count`, `interval_ticks`, `radius`, `travel_ticks` (all required) and optional `ring` and `pulse`
visual ids. No ability id in Java: the trace label is the ability id upper-cased. The pure decisions are
`core.combat.PulseFront`:

- **WHEN:** pulse `n` (1-based) fires at `castTick + (n - 1) x interval` (`pulseTick`). The executor computes each next
  delay from it, counted from the tick the previous pulse ran, so a late pulse does not push the rest late.
- **WHERE:** the caster's feet on the pulse's own tick, then FIXED for that pulse's life. Pulse 1 reads the cast's
  snapshot; each later pulse is scheduled ON THE CASTER (`scheduleOnCancellable`) and reads `combatant(id).position()`
  on his thread.
- **WHEN THE RING REACHES A BODY (Z2, the one rule):** `t` ticks after its pulse the ring is at
  `radius x t / travel` (`radius` from `t = travel` on). A body at HORIZONTAL distance `d` from the pulse's fixed
  origin is struck on the first whole `t` in `0..travel` with `radiusAt(t) >= d`, computed by walking `radiusAt` itself
  (exact arithmetic: `ceil(d x travel / radius)`). `d = 0` is tick 0; `d > radius` never. A body is struck at most once
  per pulse (a struck set carried across ticks); a body that enters behind the ring is not struck by that pulse.

**THE TABLE, EXECUTED (`PulseFrontTest`, radius 12, travel 30, so 0.4 blocks a tick):**

| horizontal distance | reach tick (after its pulse) |
|---|---|
| 0 | 0 |
| 0.4 | 1 |
| 0.41 | 2 |
| 3.0 | 8 |
| 4.0 | 10 |
| 6.0 | 15 |
| 11.5 | 29 |
| 12.0 | 30 |
| 12.5 | never |

**IN RANGE (A5):** the candidates are `combatantsNear(origin, radius)`, the query a burst lands on: in production
`World#getNearbyEntities(center, r, r, r)`, a BOX of half-extent 12 about the origin, so the vertical reach is the box's
(a body's box intersecting `y` in `[origin - 12, origin + 12]`). AND the horizontal distance is at most 12, which cuts
the box's corners (a body at `(9, 0, 9)` is 12.73 out, not struck: `PulseCastTest`).

### 3.2 Each hit (Z3 as A1 replaced it)

The ability's `on_hit` (`damage 160 element fire`) through `EffectApplier`'s `Damage` arm, at the body's feet, credited to
the caster. **Every term, READ in `EffectApplier` and `CastExecutor.commit`:**

```
sent = HitDamage.dealt( hitBase( 160 x score/100, enchantPercent, classBonus + levelBonus ), charge, crit ) x chain
```

| term | for a Smolder hit | READ |
|---|---|---|
| `score` | the gear score of the weapon he HOLDS at the cast; 100 (BASELINE) for an empty hand | `commit` sets `withTriggerScore(world.triggerScoreOf(id))` for every cast, stone casts included |
| `enchantPercent` | the damage enchants on the weapon he holds, gated on its class; 0 for an empty hand | the snapshot's `enchantDamagePercent` |
| `classBonus` | his "+N <Class> Damage" gear matching the held weapon's class; 0 for an empty hand | the snapshot's `classDamageBonus` |
| `levelBonus` | **0**: only a weapon hit adds it, and a stone cast is not one | `Caster.weaponLevelDamage` returns it only when `weaponHit` |
| `charge` | 1.0 | `Caster.of` takes `FULL_CHARGE` |
| `crit` | rolled ONCE at the cast (`BukkitCombatant.snapshot`: his crit chance and crit damage) and frozen for all four pulses | **an ability literal CAN crit** |
| `chain` | 1.0: Wildfire needs behaviours, and a stone cast carries none | `BehaviourHits.chainFactor`; `commit` freezes behaviours only for a weapon trigger |

Then on the mob: its defence (`DefenseRule.APPLIES`), and Scorch by the element with cap
`(sent / crit) x 0.5` before defence (so a crit does not raise the cap; `BukkitCombatant`'s `declaredMagnitude`), window
120, restarted by each hit. `DamagePayload`'s "reads no stat at all" is about the ATTACK_DAMAGE stat only (its own
javadoc: "IDENTITY, not a claim about which modifiers apply"), so it does not contradict the terms above.

### 3.3 Who is hit (Z4)

The sides table only. A new `MobHostility.PvpPath.PULSE(false)` (SPARED: "No players for now"), pinned in
`MobHostilityTableTest`. A summon is refused by PLAYER -> SUMMON's "no" (`harms(PLAYER, SUMMON) = false`, read off the
table). The caster by his id. Hostile = the MOB side.

### 3.4 The cast (Z5)

- 80 mana and the 6000-tick cooldown are taken at the cast, once (`AbilityService`: check, spend, then the effects).
  `CastSpec.minimumCooldownTicks` floors a Pulses cooldown at `(count - 1) x interval + travel` = 210 (binds nothing at
  6000).
- He may cast actives and use weapons while pulses are pending: nothing reads the run but its own links.
- **The pending pulses END** on his death, quit, world change, server stop, the selftest's `clear_state` and its row-start
  clean (`PulseRemovals`, on `OrbRemovals`' pattern), each writing `<LABEL> cancelled ... cause=`. A newer cast replaces
  a pending run (cause `replaced`; only reachable through a cleared cooldown).
- **CC'S CALL: a pulse ALREADY EXPANDING finishes** (its ring is point work at its own origin, like a thrown grenade's
  blast after its thrower dies). The alternative, ending it too, was allowed by Z5.
- A class or build change ends nothing: nothing calls `PulseRemovals` from one.
- **K5 (the Blink Tome: "a spell cast ends the invisibility"), READ:** only `WeaponFire` passes the seam
  (`attacks().attacked(player, EndReason.CAST)`, on a weapon trigger's Success). `StoneCaster`, which casts every
  ability on the Ability Stone (Q included), does NOT. **CC'S CALL: Smolder joins every other stone cast and does not end
  the blink**, so no stone ability changes behaviour without a ruling. The alternative (any stone cast ends the blink)
  changes Ember Step, Solar Grenade and the rest as well. **RULED, the seat's E5 (2026-11-27): it stays as it is,
  and Smolder is added to `PLAN-blink-tome.md`'s OPEN FOR BEN line 8 ("whether an Ability Stone cast should end it").**

### 3.5 The scheduler (Z6), and why it is region-safe

Each next pulse rides `CombatWorld.scheduleOnCancellable(caster, lastKnown, delay, task)` (CC'S CALL, a new port
method): a `PendingWork` link registered at the pulse's origin and run on the CASTER's own scheduler, so it reads his
feet on the only thread that may, and `cancelScheduled`, a removal's `work().take` and the pad's `cancelIn` all end it.
Each ring tick after tick 0 is `world.schedule` at the pulse's fixed origin (a `PendingWork` link at that point). No
per-tick scan of players. The Folia debt is the cone's (recorded on `CastExecutor.conePass`): a ring's 12-block query
may read a neighbouring region's chunks. Paper runs one thread.

### 3.6 The ring (Z7)

`ring` (`smolder_ring`) is presented at `PulseFront.ringPoints(origin, radiusAt(t))` on every tick `t = 0..30`: points
about 0.75 blocks apart round a horizontal circle (101 at 12 blocks), 0.1 above the origin's feet; the same
`radiusAt` the hits use. `pulse` (`smolder_pulse`) is presented once at each origin. Both visuals are SEAT PLACEHOLDERS
(a still flame per point; a flame burst and `entity.blaze.shoot` at pitch 0.8). `ContentValidator` refuses a sound in a
ring, as in a cone's front. NO line of sight is asked (CC'S CALL: a burst asks none; the alternative is the cone's eye
trace).

### 3.7 The pool (Z8 as A3 left it)

`mage_fire.yml` names `smolder` as its one ultimate and its default; `ultimate_placeholder_mage.yml` is deleted and added
to `AbilityLoader.RETIRED_FILES`, so a stale data-folder copy is skipped and named. A saved loadout naming the deleted
id reads an EMPTY Q (unchanged rule). **READ: the plan's "logged once" for such an id had no code.** CC'S CALL: it is
written now, once per server run per player, cell, slot and id, from `Stones.equippedFor`:
`BUILD unoffered player=<uuid> cell=mage/fire slot=ULTIMATE id=ultimate_placeholder_mage reads=empty kept=file`
(`LoadoutResolution.unoffered`, tested in core; the line pinned in `StonesUnofferedLineTest`).

**OPEN FOR BEN:** a Fire Mage who saved a build sees an empty ultimate until he picks Smolder in the build menu.

### 3.8 The description (Z9)

A standalone ability's `description:` was not read before (`AbilityLoader` passed `List.of()`); it is now, as a YAML list
(a bare string refused by name). SEAT PLACEHOLDER text, every number the file's own (`SmolderContentTest` rebuilds each
sentence from the parsed definition): "Four pulses, 3s apart, burst from your feet." / "Each deals 160 fire damage within
12 blocks and scorches." **READ, CORRECTED ON `feat/smolder-gate` (2026-11-27): two surfaces, and they differ.** The
PICKER (`BuildPickerMenu`, through `BuildMenu.abilityLore`) shows the description and `Cooldown: 300s`, NOT the mana
cost. The Build screen's loadout CELL (`BuildMenu.loadoutIcon`, through `numberLines` and `AspectLore.lines`) shows the
description and `Damage: 160`, `Cost: 80`, `Cooldown: 300.0 s`. The first READ named the picker alone and was reported
as "the build menu"; GATE-smolder's SM1 reads both. Unchanged.

### 3.9 The trace lines (Z10), while `/rpg mobtrace` is on

```
SMOLDER cast caster=<uuid> tick=<t>
SMOLDER pulse n=<1..4> tick=<t> origin=<x y z> caster=<uuid>
SMOLDER hit n=<n> target=<type uuid> dist=<horizontal> tick=<t> sent=<amount> caster=<uuid>
SMOLDER cancelled caster=<uuid> remaining=<n> cause=<death|quit|world_change|stop|selftest|replaced> tick=<t>
```

CC'S CALL: `caster=` is added to the pulse and hit lines and `tick=` to the cancelled line, beside the seat's fields.
`sent=` is the amount the applier dealt (its direct-damage listener), before the mob's defence, as `PLAYERHIT`'s is.
Shapes pinned in `PulseRunsTest`.

### 3.10 Recounts (Z11), on `feat/smolder`

Recount each, never adjust: abilities `ls paper/src/main/resources/content/abilities/*.yml | wc -l`; builds
`ls paper/src/main/resources/content/builds/*.yml | wc -l`; the PvP table `grep -c '^        [A-Z_]*(\(true\|false\))'
core/src/main/java/io/github/butterflysmp/rpg/core/mob/MobHostility.java`. The figures are in the PR body, taken on its
head.

## 4. Tests and mutations (Part 1)

Core: `PulseFrontTest` (the tables), `PulseRunsTest` (the registry and the four lines), `PulseCastTest` (the cast on the
fake world, 14 rows), `LoadoutResolutionTest` (+2: A3), `MobHostilityTableTest` (PULSE pinned). Paper:
`SmolderContentTest` (5), `ContentValidatorTest` (+1), `PulseRemovalWiringTest` (3), `StonesUnofferedLineTest`,
`ScorchYmlCommentTest` (2, A2), `PoolLoaderTest` and `AspectLoaderTest` pointed at `smolder`, and
`ScorchContentInvariantTest`'s record (19 -> 19, -1 +1).

Mutations, each applied by a literal one-line splice (marker present 1, original gone 0, delta 1 against a scratchpad
copy), run, and restored from the copy (markers left 0):

| mutation | the edit | reddened |
|---|---|---|
| MUTREACH | `radiusAt(t) >= d` -> `>` | PulseFrontTest (2), PulseCastTest (1) |
| MUTORIGIN | the next pulse at the FIRST origin | PulseCastTest.eachPulseOriginatesAtHisFeet... |
| MUTSIDE | `PULSE(false)` -> `true` | PulseCastTest (another player struck), MobHostilityTableTest |
| MUTSTRUCK | the struck set not carried | PulseCastTest.aBodyThatRunsOutwardAheadOfTheRing... |
| MUTHORIZ | 3-D distance for horizontal | PulseFrontTest (2), PulseCastTest.inRangeIsTheBurstsBox... |
| MUTTICK | `pulseTick` + 1 | PulseFrontTest (1), PulseCastTest (5) |
| MUTUNOFFERED | `unoffered` always empty | LoadoutResolutionTest (2) |
| MUTREPLACE | a replaced run's link not cancelled | PulseCastTest.aSecondCastReplaces... (8 pulses, not 5) |
| MUTVALID | the ring check skipped | ContentValidatorTest.aPulsesRing... |
| MUTDESC | a string description accepted | SmolderContentTest.aDescriptionThatIsOneString... |
| MUTWIRE2 | the death removal deleted | PulseRemovalWiringTest (death) |
| MUTRETIRED | the placeholder not retired | SmolderContentTest.aStaleCopy... |
| MUTA2 | `scorch.yml` states CAP_FRACTION 1.0 | ScorchYmlCommentTest |

MUTWIRE (the first attempt) is VOID: its needle began mid-name, the edit left a dangling package prefix, and the build
did not compile. A compile error is not a mutation; it was re-run as MUTWIRE2.

## 5. Owed and open

- **OPEN FOR BEN:** Z2 (the damage travels with the ring; alternative: all struck at the pulse's tick), the ring's and
  the pulse's look (particle, sound), the description wording, the empty ultimate on a saved build (A3). Part 2's are in
  section 6.9.
- **K5:** ruled by the seat's E5 (section 3.4).
- **Only the boot can show:** the ring as drawn (its spacing, height and speed as Ben sees them), the pulse sound, a
  pulse on a real entity scheduler arriving on its tick, his death ending the run on a real server.

## 6. Part 2: water puts out Scorch (the seat's Y1 to Y7 as E1 to E4 left them)

Built on `feat/water-douses-scorch`, from `feat/smolder` (`619058dc`). A rule of the STATUS: it holds for every fire
weapon and ability, because every Scorch reaches the one store (`ScorchStatus`) and every Ignite one of its two clauses.

### 6.1 What counts as wet (Y1), READ by javap on the pinned 26.1.2 server

The adapter's one reading is `EntityScorchSink.wetness(entity)` = `Douse.wet(entity.isInWater(), entity.isInRain())`.

- **(A) In water: `isInWater()`.** READ: `CraftEntity.isInWater` is `Entity.isInWater`, which returns the field
  `wasTouchingWater`, set each tick by `Entity.updateFluidInteraction` from `EntityFluidInteraction.update`, which walks
  the blocks of the fluid box and asks each one's `getFluidState()` for the WATER tag and its height. So it covers:
  - water source and flowing water;
  - a **waterlogged** block (its `getFluidState` is water when `WATERLOGGED`, READ on `SlabBlock`);
  - a **bubble column** (`BubbleColumnBlock.getFluidState` = `Fluids.WATER.getSource(false)`, READ);
  - partly submerged: the height is compared against the box, so any overlap counts (INFERRED from the walk; the
    comparison's operands were not read one by one).
  - **NOT a water cauldron (E4, READ):** neither `LayeredCauldronBlock` nor `AbstractCauldronBlock` declares
    `getFluidState`, nor does `Block`, so it is `BlockBehaviour.getFluidState` = `Fluids.EMPTY.defaultFluidState()`.
    **Vanilla's own flame IS put out there** (`LayeredCauldronBlock`'s `entityInside` extinguishes and lowers the
    level, READ earlier this day) **while our burn continues: OPEN FOR BEN.**
- **(B) In the rain: `isInRain()`.** READ: `CraftEntity.isInRain` is `Entity.isInRain` = `Level.isRainingAt(block
  position) || isRainingAt(the top of the box)`; `isRainingAt` = the level is raining AND `canSeeSky(pos)` AND the
  position is at or above the `MOTION_BLOCKING` heightmap AND the biome's precipitation there is RAIN. So no rain under
  a roof, in a desert, or in snow.
- **(D) A splash water bottle:** `WaterBottleSplashEvent.getToExtinguish()`, the server's own set. READ:
  `AbstractThrownPotion.onHitAsWater` takes the living bodies in the bottle's box inflated (4, 2, 4), keeps those with
  `distanceToSqr < 16` that are `isOnFire() && isAlive()`, and `CraftEventFactory.callWaterBottleSplashEvent` hands
  them to the event. `isOnFire` = `!fireImmune() && remainingFireTicks > 0` (READ), so a scorched FIRE-IMMUNE body is
  not in the set.
- **(C) Soaked is NOT a douser** (Ben: *"we'll come back to C when we do our Water Element pass"*). Carried.

### 6.2 When it is checked (Y2, Y3, E1): one decision, three seams

| seam | where | wet means |
|---|---|---|
| an APPLICATION (Y3) | `ScorchStatus.apply` (the 8-argument one every route reaches) | no Scorch, no flame, `SCORCH refused` |
| a BURN TICK (Y2), before its damage | `DotStatus`'s tick asks `dousedBeforeBurn`; only `ScorchStatus` answers | the tick deals none, the burn ends, the flame is cleared, `SCORCH doused` |
| a DEATH (E1) | clause 1: `RpgListeners.onEntityDeath`; clause 2: `BukkitCombatant`'s killing blow | no detonation, `IGNITE refused` |
| a SPLASH (D, E2) | `RpgListeners.onWaterBottleSplash` | the live Scorch ends at once, the flame is cleared, `SCORCH doused cause=splash` |

**E1's decision table** (`Douse.atDeath(scorched, fireKill, wet)`, pinned whole by `DouseTest`):

| scorched at death | killing blow fire | wet | result |
|---|---|---|---|
| no | no | either | NONE (no line) |
| no | yes | dry | CLAUSE_2 detonates |
| no | yes | water or rain | REFUSED |
| yes | either | dry | CLAUSE_1 detonates |
| yes | either | water or rain | REFUSED |

Clause 1 asks it AFTER `forget` (the once-ness guard must run whatever is decided). Clause 2 is asked only of an
unscorched body (`wasScorched` keeps the two from both firing), so one death writes at most one `IGNITE refused`.

**What the death reads (INFERRED):** `isInWater` on the death frame is `wasTouchingWater` from the body's last base
tick; the death and that tick are the same server tick, or the body would have been doused or refused first.

### 6.3 The flame (A4): before and after

- **Before this branch, READ:** vanilla put the FLAME out in water (`WaterFluid.entityInside` →
  `InsideBlockEffectType.EXTINGUISH` → `Entity.clearFire`, from `Entity.checkInsideBlocks`) and in the rain
  (`Entity.applyEffectsFromBlocks`: `if (isInRain()) clearFire()`), and the BURN went on for its whole window: up to six
  more ticks of min(5% max, the cap), on a body with no visible fire.
- **After:** the burn ends at its next burn tick (at most 20 ticks later), dealing nothing that tick, and the flame is
  cleared by our call too (`clearFlame` = `setFireTicks(min(0, current))`, vanilla's `clearFire` exactly). A refused
  application sets no flame: both `setFireTicks` sites in `BukkitCombatant` now follow a TRUE `apply`.
- **Can the code tell Scorch's flame from another fire? No:** fire ticks are one number on the body. So it clears them
  only where vanilla clears them anyway (a wet body), or on the set vanilla extinguishes (a splash).

### 6.4 Who (Y5), and what is not touched (Y6)

- **Y5, READ:** no mob scorches a player today (every mob damage path passes element `null`). A player can be scorched
  by another player's fire hit on a landing PvP path (RAY, PROJECTILE, CONE, BURST, AREA, IMPLOSION) and by `/rpg
  apply`; the rule holds for him as for a mob (`EntityScorchSink` reads any entity). The death listener skips players,
  as before.
- **Y6:** changed: `ScorchStatus` (the refusal, the douse, the trace), `DotStatus` (one protected hook answering false,
  so Wither is unchanged: `ScorchDouseTest.witherIsNotPutOutByWaterBecauseTheRuleIsScorchsAlone`), `ScorchSink` (two
  abstract methods), the two flame sites and the two Ignite sites. No damage path changed: the HIT's own damage lands
  before any of this is asked. Vanilla fire (`kind: fire`) is untouched. A burning FIELD on the ground (the grenade's)
  is not removed by water; each of its applications on a wet body is refused.

### 6.5 The lines (Y7, E1), while `/rpg mobtrace` is on, pinned by `DouseTest`

```
SCORCH doused target=<type uuid> cause=<water|rain|splash> tick=<t>
SCORCH refused target=<type uuid> cause=<water|rain> tick=<t>
IGNITE refused target=<type uuid> cause=<water|rain> tick=<t>
```

### 6.6 CC'S CALLS

1. **Water names itself when both hold** (`cause=water`); the alternative changes only the word on the line.
2. **A body already scorched and now wet is REFUSED at a new application**, and its live burn is doused at its next
   burn tick (the alternative, dousing it at the application, would add a fourth seam Y2 does not name).
3. **The splash set is the server's `getToExtinguish()` exactly**, as Y1 asks, so a scorched fire-immune body is not
   splashed out. The alternative: every living body within the bottle's 4 blocks.
4. **The splash douse runs inline when this region owns the body, else one entity hop.**
5. **An application that buys no stacks is not a refusal** and writes nothing.

### 6.7 E2 and E3

- **E2:** a body splashed and then killed by a fire hit is DRY for clause 2, and ignites. **OPEN FOR BEN.**
- **E3:** a detonation already armed on a dead body (Ignite's 20-tick fuse) is not cancelled by water. **SEAT
  PLACEHOLDER, OPEN FOR BEN.**

### 6.8 Tests and mutations (Part 2)

New: `DouseTest` (3, core), `ScorchDouseTest` (7), `DouseWiringTest` (5), one row in `ScorchYmlCommentTest`.
`ScorchStampStatusTest.thePortHandsTheHitsStampToTheScorchItApplies` moved its anchor (the apply is now the `if`'s
condition, ending at `{` instead of `;`); still anchored at both ends.

Each mutation: a literal splice (`mut.sh`: marker present, original gone, line delta against a scratchpad copy), run
against its named test, restored by `cp` (markers left 0).

| mutation | the change | red |
|---|---|---|
| MUTWETORDER | rain before water | `DouseTest.theWetDecision...` |
| MUTDEATH | wet ignored at death | `DouseTest.eOnesTable...` |
| MUTTICK | the burn tick never asks | `ScorchDouseTest.aBurnTickOnAWetBody...` |
| MUTREFUSE | the application never refuses | `ScorchDouseTest.aWetBodyGainsNoScorch...` |
| MUTFLAME | the tick's flame clear deleted | `ScorchDouseTest.aBurnTickOnAWetBody...` |
| MUTSPLASHFORGET | the splash does not end the burn | `ScorchDouseTest.aSplashDouses...` |
| MUTWITHER2 | wither's hook douses too | `ScorchDouseTest.witherIsNotPutOut...` |
| MUTCLAUSE2 | clause 2 reads dry | `DouseWiringTest.clauseTwo...` |
| MUTCLAUSE1 | clause 1 reads dry | `DouseWiringTest.clauseOne...` |
| MUTSPLASH | `getAffectedEntities` for the set | `DouseWiringTest.aSplashDouses...` |
| MUTAPPLIED | the status arm's flame unconditional | `DouseWiringTest.bothScorchFlameSites...` |
| MUTTRACE | production's store gets no douse trace | `DouseWiringTest.productionsStore...` |

**MUTWITHER (the first attempt) is VOID:** its two-line needle was LF against a CRLF file and matched nothing (original
before 0, marker 0). Re-run on one line as MUTWITHER2. **MUTWITHER2's replacement contains its original**, so its
"original gone" figure is uninterpretable (the eighth row of the mutation-lies table); its line delta of 1 against the
scratchpad copy is the measurement.

### 6.9 OPEN FOR BEN (Part 2)

- Y3: a wet body gains no new Scorch (SEAT PLACEHOLDER).
- E2: a splashed body killed by fire still ignites (a splash is a moment).
- E3: an armed detonation is not cancelled by water (SEAT PLACEHOLDER).
- E4: a water cauldron puts out the flame (vanilla) but not the burn.
- K5 (the seat's E5): whether an Ability Stone cast, Smolder included, should end the Blink Tome's invisibility.

**Only the boot can show:** the douse on a real body in water and in rain, the splash event's set on a real throw, and
the death reading on a real death frame.

## 7. The gate branch (`feat/smolder-gate`): `docs/gates/GATE-smolder.md`

- **One command**, the regen-tooltip rows first and the one ask last: `/rpg selftest
  armor-sets.AS1.AS2+mana-battery.MB1+smolder`. Rows SM1 to SM7, WA1 to WA3 and WA6; WA4 (rain) and WA5 (splash) owed
  to the unit table, with the reasons in the gate.
- **CC'S CALL, a new selftest verb `stone:`**: `clear_inventory` empties the Ability Stone and nothing but a join or a
  respawn mints it again (READ: `RpgListeners.convergeStone` is called from those two only), so a row converges it by the
  join's own call (`NexusSlots.converge` with `Stones.lockedItem`). Logs `STAGE stone slot= found= held=`.
- **THE CLOCK, READ by javap** (the gate's section of that name): the ring's hits run on the global region scheduler
  before every entity scheduler in a tick, and a burn booked from that phase first lands 19 ticks later. So four hits
  60 apart burn fifteen times, never colliding with a hit. Executed through the real store (`SmolderGateArithmeticTest`).

  > **DATED CORRECTION, 2026-11-27 (the seat's X6 (a), after the boot of `01be9d3b`): THE BULLET ABOVE IS WRONG IN TWO
  > PLACES, AND THE READ IT CITES IS RIGHT.** The boot read pulse 2 at **C+59** on every row (pulses 3 and 4 on C+120
  > and C+180) and every first burn at **hit + 20**, not + 19; at SM2's 6239 a hit and a burn landed on one tick, the
  > hit logged first. **The phase order and the two scheduler rules quoted above were READ correctly and still hold**
  > (re-read by javap, `PLAN-owed-fixes.md` *X6 AS BUILT* (a)). **What went wrong is which booking each rule was applied
  > to:** the bullet said a burn is "booked in a hit's tick" (from the global phase, so one short), without reading that
  > the hit's damage first hops onto the mob (`BukkitCombatant`'s `onEntity`, which runs later in the same tick's ENTITY
  > phase) and books the burn from there (exact: 20); and it took pulse 2 as exact, without reading that
  > `StoneCaster.run` hands the cast to `onRegion` (the GLOBAL phase), from which pulse 2 was booked (one short: 59).
  > **A READ of the scheduler without a READ of the call path into it.** Also: every trace line prints ONE clock,
  > `Bukkit.getCurrentTick()`; the selftest's `tick=` reading one less than the cast line is the press (P) against the
  > cast one tick later (C = P + 1, the `onRegion` hop), not two clocks. **From `feat/x6-clock` on, every pulse lands
  > exactly on C + interval x (n - 1)** (`CastExecutor.bookOnCaster`, the settle hop). The frozen gate's predictions are
  > not edited; its successor re-predicts.

- **Correction to section 3.8, found while writing SM1:** the Build screen's cell DOES show the mana (`Cost: 80`); only
  the picker does not. Section 3.8 now says so.
- **Tests:** `SmolderGateArithmeticTest` (13: every figure the gate quotes, and the SAFETY `harms` calls),
  `SmolderGateCommandTest` (5: the queue, the lint, the one ask last at 3600, 18 frozen tagged summons, the stone verb).
  The recount guards moved with the gate: `SelfTestScenariosTest` 542 to 553 and its two gate lists,
  `GateDocumentSectionsTest` 57 to 58. The lint caught six turn-then-press steps on the first draft (a 1-tick wait added).
- **Mutations:**

| mutation | the change | red |
|---|---|---|
| MUTSTONEARG | `stone:` dropped from the takes-nothing verbs | `SmolderGateCommandTest.theStoneVerbTakesNothing...` |
| MUTPHASE2 | each later hit modelled one tick EARLY (hit before burn) | the 15-, 12- and 9-burn rows |
| MUTNOAI2 | one witness summoned with `NoAI:0b` | `everyMobThisGateSummonsIsFrozenAndTagged` and the lint |
| MUTASK | the ask's timeout 1200 | `itLintsClean_asksOnceLast...` |

**MUTPHASE (`+ 1` to `+ 0`) stayed GREEN, and it is an EQUIVALENT mutation, not a gap:** the fake clock already runs a
due burn before a same-tick application, so both mappings model the burn-first order the READ gives. MUTPHASE2 is the
order that would change the figures, and it reddens. **MUTNOAI (the first attempt) was a broken edit:** it appended
`NoAI:0b` and left the original `NoAI:1b` in the same compound, so `contains("NoAI:1b")` still held. Re-run as
MUTNOAI2 by replacing the flag.
