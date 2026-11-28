# PLAN: Smolder, the Fire Mage's ultimate, and water puts out Scorch

**Status: PART 1 BUILT on `feat/smolder` (not booted, not merged). PART 2 is `feat/water-douses-scorch`. The gate is
`feat/smolder-gate`, last in the stack.** Stacked on `feat/regen-tooltips-sheet-unit` (#336, `d696b50d`), from master
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

## 2. The three STOPs and their rulings

| STOP | READ | ruling |
|---|---|---|
| 1 | A fire hit applies Scorch BY THE ELEMENT (`ElementAccrual.forHit`): cap = `(amount / crit) x Scorch.CAP_FRACTION 0.5`, window `Scorch.DEFAULT_DURATION_TICKS 120`. An authored `status: scorch` caps at the UNHALVED `payloadDamage` (`BukkitCombatant.applyStatus`). The brief had cap 160 and `duration_ticks 80`. | **A1**: the damage only, Scorch by the element (cap 80 uncrit, 120 ticks, restarted by each hit). **A2**: `scorch.yml`'s comment corrected. |
| 2 | `LoadoutResolution`: an id the pool no longer offers reads EMPTY, never the default slot by slot ("a visible hole"; `savedEmptySlotsStayEmpty`). Z8 wanted the default. | **A3**: Z8's fallback withdrawn; the existing rule stands. |
| 3 | Scorch's FLAME rides vanilla `fireTicks` (`setFireTicks` at accrual and in `applyStatus`); only its damage is our own clock. | **A4**: a douse clears the flame too (Part 2). |

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
  changes Ember Step, Solar Grenade and the rest as well; OPEN FOR THE SEAT.

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
12 blocks and scorches." **READ: the build menu shows the cooldown ("Cooldown: 300s") and NOT the mana cost**
(`BuildMenu.abilityLore`); unchanged.

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
  the pulse's look (particle, sound), the description wording, the empty ultimate on a saved build (A3).
- **OPEN FOR THE SEAT:** K5 (above).
- **Only the boot can show:** the ring as drawn (its spacing, height and speed as Ben sees them), the pulse sound, a
  pulse on a real entity scheduler arriving on its tick, his death ending the run on a real server.
