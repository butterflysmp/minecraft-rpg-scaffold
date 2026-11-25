# PLAN — THE SUMMONER CLASS, FIRST SLICE: THE BONE WHISTLE AND ITS WOLF

**Status: PR 1 BUILT (#289); PR 2a STOPPED at H2 with C1 and today's PvP pinned (section 2). NOT BOOTED.** The seat's brief of 2026-11-24 (Ben's words, verbatim, are in it and in
section 0). The design for PR 3 onward (D1-D10) is PROPOSED in the report to the seat and is NOT built until ruled.

## 0. BEN'S WORDS AND THE SEAT'S DEFAULTS (2026-11-24)

- Identity: *"Mostly commander but I do want to add some Melee style weapons to summoners so they can do some fighting
  back."* Cap: *"Default is 1 max summon, it will be a new stat in player profile. Certain gear will increase this number
  too."* Life: *"Until they die or are recast."* Souls are not the Summoner's (the Undead element's, unbuilt).
- Modes: guard within 8 of the caster; attack a mob that attacks him, or targets him within 30; a left click with the
  summon weapon on a mob directs that summon. *"each summon weapon can only summon one."* Start: the bone whistle.
- The Bone Whistle: *"100 health, 20 damage, 40 mana, 10s cooldown. Yes stats scale with gearscore. The same goes for all
  summon weapons"*; *"Common, wood tier, Nature damage"*; a recast replaces it fresh at full cost (*"Correct"*); at the
  cap, *"Cast refused"*; recipe and look *"Keep"*.
- **The seat's defaults, told to Ben and not objected to:** after a directed target dies the summon guards again; it
  does not attack a mob merely because he hit it; removed when he dies, logs out or changes world; teleports to him when
  far behind; his own attacks cannot hurt it and it never attacks players; mobs can kill it and it does not heal;
  "Summoner" is a fourth weapon class.

## 1. PR 1 — THE CLASS AND THE STAT (`feat/summoner-class`, on #288)

**THE CLASS.** `WeaponClass.SUMMONER` and `GearClass.SUMMONER`. **Every site, by how it was found:**

| site | found by | what it does for SUMMONER |
|---|---|---|
| `GearClass.of` | the compiler (no default arm) | `SUMMONER -> SUMMONER` |
| `TransferKey.of` and the enum | the compiler | a new key `SUMMONER("summoner weapons")`: **a summoner weapon trades score only with a summoner weapon**, as each fighting class does (my call; the seat may rule otherwise) |
| `GearScore.scoreable` | an exhaustive switch, edited before the compiler reached it | `true`: Ben, *"stats scale with gearscore"* |
| `GearClassLabel.of`, `.describe`, `.describeEnchant` | the compiler | "Summoner", "a Summoner weapon", "a Summoner enchant" |
| `WeaponClassLabel.of` | the compiler | "Summoner"; the footer reads **"Common Summoner Weapon"** |
| `/rpg classdamage`'s unknown-class hint | grep (a hand-typed "melee, ranger, mage") | now derived from `WeaponClass.values()` |
| `AccessoryLoader`'s unknown-class message | grep (the same) | now derived |
| `EnchantDefinition` `MAIN_HAND_ONLY` (an allowlist of MELEE, RANGER, MAGE) | grep | **UNCHANGED, deliberately:** a damage enchant authored `class: summoner` is refused at load. No summoner enchant exists, and a summon's hit is never a "hit" for an enchant (B4). **OPEN FOR BEN** when the first one is wanted |
| `AccessoryType` (gauntlet, quiver, scroll) | grep | no summoner type; `class: summoner` on an accessory now parses with no type. No content does |
| `content/builds/` (melee, ranger, mage) | reading | **NO SUMMONER BUILD: no player can CHOOSE the class.** Class-slot accessories and class damage key on the chosen class. **OPEN FOR BEN** |
| `AccessorySlots.classToken`, `ClassDamageModifierItems`, `WeaponLoader` | grep | derived from the enum; nothing to change |

The pin tests that held the three-class axis, each updated with a line saying SUMMONER moved it: `WeaponClassTest`,
`GearClassTest` (two), `TransferKeyTest` (three), `AccessorySlotsTest`, and `WeaponLoaderTest`'s unknown-class fixture
(was `class: summoner`, now `class: summoners`).

**THE POOLS** (`SummonerPoolsTest`, measured from the 23 shipped enchants, then pinned): slots 2-3 (normal)
**`unbreaking` alone**; slot 1 (unique, nature) **none**; slot 0 (weapon) **none**. Control: the same roster gives a
mage weapon `attunement`.

**THE STAT `max_summons`.** `core/summon/MaxSummons`: `DEFAULT = 1` (Ben), `effective(base, grants) = max(0, base + sum)`.
The profile stores only the BASE, boxed (`maxSummons` key; absent = 1); gear grants are summed at the read
(`ProfileService.maxSummons`) and **no gear grants it in this slice**. **A profile written before this change has no key
and reads 1: no migration, no schema bump** (`fightReturn`'s reason). An older build reading a newer file drops only a dev
override. **Unknown (loading, unreadable, untracked) reads EMPTY**, not 1 and not 0; the cast (a later PR) decides. Dev
command: `/rpg maxsummons` (read), `/rpg maxsummons <0-64>` (set the base), `/rpg maxsummons reset`; every reply is the
effective value. **The override persists across a restart**, so a gate that sets it resets it.

**THE SEAT'S S1-S3 (ride this PR):**
- **S1:** the MOBSEED line ends `at=<x y z> stand_d=<distance from the pad's stand>` (`MobSeedLineTest`; the prefix up to
  `max=` is unchanged). The one-rule setup sweep is NOT built.
- **S2: THE OWED DATED NOTES, NOW FIVE:** `sword-smithing#SSA1`, `vanilla-bows#VX1`, `vanilla-bows#VX5R2` (the L3 rows,
  `PLAN-enchant-columns.md` section 33) and **`unique-enchant-slot#US8`** (anvil and grindstone opened with no level
  staged). No old row edited. `sword-tiers#STA4`'s is paid by S3.
- **S3:** `GATE-sword-tiers.md`'s dated note names `HELD_READ_THEN_CHANGE` as the cause of STA4's UNREAD iron half, and
  closes `PLAN-weapon-tiers.md`'s carried item 5 as **EXPLAINED, still UNREAD**.

**Tests:** `MaxSummonsTest` (4), `MaxSummonsProfileTest` (4), `ProfileServiceTest` (+1), `SummonerPoolsTest` (1),
`SummonerLabelTest` (1), `MobSeedLineTest` (2), `GearScoreTest` (+1 line). **Mutations, all red:** `MUTDEFAULT`,
`MUTSUM`, `MUTFLOOR`, `MUTCARRY` (one `with` drops the base), `MUTABSENT`, `MUTUNKNOWN` (unknown reads 1), `MUTSCORE`,
`MUTLABEL`, `MUTSEED`.

## 2. PR 2a (`feat/summoner-sides-core-a`, on #289): STOPPED AT THE SEAT'S H2, WITH C1 AND TODAY'S PvP PINNED

**The seat's ruling of 2026-11-24 (via Ben):** the split is 2a core, 2b paper; H1 one table, attacker side x victim side x
arm (DIRECT: a ray's, projectile's or melee primary hit; INDIRECT: area, splash, chain, status tick, homing pick, any
secondary); H2 PLAYER -> PLAYER unchanged, filled with what the code does TODAY per arm, pinned BEFORE the refactor, and
**"If today's behaviour differs between two paths of the same arm, STOP and list them; do not average them."**

**IT DIFFERS, so the refactor is not built.** PLAYER -> PLAYER today, path by path (a player's ability, another player
as the victim). READ = a characterisation row in `PvpTodayCharacterisationTest` (or the existing pin named); SOURCE = read
in the code, not run:

| arm | path | today | how known |
|---|---|---|---|
| DIRECT | the swing (`CastSpec.Melee`, `CastExecutor` nearest-in-arc) | **LANDS** | READ |
| DIRECT | the ray (`castRay`'s first body) | **LANDS** | READ (and `PlayerHitTraceTest.aPlayerTargetIsNotTraced`) |
| DIRECT | the projectile (`ProjectileFlight.step`'s `castRay`) | **LANDS** | SOURCE (the same `castRay` filter) |
| ? | the cone (`ConeShell.reached`, every body in arc and range) | **LANDS** | READ. Which arm a cone is, the seat's to say |
| ? | the dash's line (`SweptLine.enemiesAlong`, "mob-only: players are not hit") | **SPARED** | READ. Which arm a dash is, the seat's to say |
| INDIRECT | a burst (`EffectApplier.applyToNearby`) | **LANDS** | READ |
| INDIRECT | an area (`applyArea` -> `applyToNearby`, as not-a-hit) | **LANDS** | SOURCE (the same method) |
| INDIRECT | a travel-end burst (`applyToNearbyMobs`) | **SPARED** | SOURCE |
| INDIRECT | the shockwave (`BehaviourHits`, "never a player") | **SPARED** | READ (`BehaviourHitsTest`) |
| INDIRECT | the Lapis Crystal's shot (`CastExecutor`, "never a player") | **SPARED** | READ (`OrbBeamTest`) |
| INDIRECT | Ignite's splash (`Ignite`) | **SPARED** | SOURCE |
| INDIRECT | the homing pick (`ProjectileFlight.nearestMob`, "R2") | **SPARED** | SOURCE |
| INDIRECT | an implosion (`EffectApplier.implode`) | **BY ITS `affects:`** (mobs: spared; players: lands) | SOURCE |
| INDIRECT | a status tick (scorch on a player hit directly) | **UNVERIFIED** | not read |

**So the INDIRECT arm is not one behaviour today** (a burst and an area land on a player; a travel-end burst, the
shockwave, the crystal shot, Ignite and the homing pick spare him), and if the dash is DIRECT, nor is that arm. **H2's
table cannot be filled per arm without averaging.** Paper hands players to core on every pick (`LiveTargets.resolve`
admits a `Player`; `combatantsNear`, `castRay` and `sweepRay` all use it), so every LANDS above is INFERRED to reach a
server. **"Ability PvP works today on the direct arm" stays INFERRED, not READ on a server (H8).**

**What the five rows assert is what IS.** Each LANDS row would read `damageCalls 0` under a player filter; the dash row
carries a mob as its positive control. They are to stay green before and after the refactor, however the seat rules.

### C1 — THE SELFTEST SNAPSHOT RESTORES max_summons (in this PR)

`SelfTestSnapshot` captures the profile's STORED base, boxed (`maxSummonsOrNull`, null = never set), at the run's start,
and `restore` writes it back (`setMaxSummonsBase`, null restoring absence) and names it on the RESTORED line
(`max_summons_base=<v>|NOT-RESTORED`). Every end (END, abort, stop, quit, disable) reaches that one `restore` through
`SelfTestRun.end` (`SelfTestDisableWiringTest` pins disable). **Test:** `SelfTestMaxSummonsRestoreTest`, a source scan
(the snapshot needs a live Player), bounded by the constructor and by `restore`'s own return, not by a line count. The
behaviour of a `null` write (back to 1) is `ProfileServiceTest`'s, from PR 1. **Mutations, all red:** `MUTCAPNULL`,
`MUTNOWRITE`, `MUTNOLINE`. #289's head is not rewritten.

### 2.1 THE TABLE, BUILT (the seat's P1-P4, 2026-11-24)

`core/mob/MobHostility` is the ONE home: `harms(attacker, victim, victimExempt, path)`. PLAYER -> PLAYER reads the enum
`PvpPath` (TODAY'S BEHAVIOUR, UNRULED, OPEN FOR BEN (PvP); Ben's answer is a change to that enum alone); every other
cell is plain. `hostileTo` is the MOB row; `refusedSummonCell` (the paper guard's question) reads `harms` and is true
only for PLAYER -> SUMMON, SUMMON -> PLAYER and SUMMON -> SUMMON.

| attacker \ victim | PLAYER | MOB | SUMMON |
|---|---|---|---|
| PLAYER | PER PATH, AS TODAY (`PvpPath`) | yes | no |
| MOB | yes, unless creative/spectator | no | yes |
| SUMMON | no | yes | no |

`PvpPath`, every value READ by a test: SWING, RAY, PROJECTILE, CONE, BURST, AREA, IMPLOSION (for what its `affects:`
admits), STATUS_TICK **land**; DASH_LINE, TRAVEL_END_BURST, SHOCKWAVE, CRYSTAL_SHOT, IGNITE_SPLASH, HOMING_PICK
**spared**. The SOURCE rows of section 2 and the status tick were each READ by a new characterisation row before the
refactor (`PvpTodayCharacterisationTest`, 9 rows) and **every one read as the table said**; nothing was corrected.
STATUS_TICK's core half is READ (a player's direct hit puts its status on a player); its tick half is side-blind by
construction (`DotStatus.apply` and `ScorchStatus.apply` receive only the holder's UUID).

**TWO DECISIONS OF MINE, FOR THE SEAT:**
1. **`ENGINE_ATTACKER = PLAYER`, a constant, not a side read off the caster.** READ: the engine's only drivers are a
   player's casts (`StoneCaster`, `RpgCommand`, `VanillaShots`, `WeaponFire` x2) and Ignite's applier is a player's id.
   The first build read the caster's own side and turned **90 rows red**, one cause: core fixtures cast from a default
   (mob) dummy, so MOB -> MOB refused their hits. The engine never asked the attacker before, so a constant keeps every
   behaviour; it becomes a caster field the day a summon casts through the engine.
2. **The enchant accounting is NOT the table.** `BehaviourHits.chainFactor`, `.scorchStamp`, `.onHit` and
   `EffectApplier.trace` asked "is the victim a player"; they now ask `countsAsAMobHit` (victim is a MOB). Routing them
   through `harms` would make a ray's PvP hit start counting for his enchants, which changes PvP.

**Where a summon is refused on a DIRECT pick:** the plain ray and the projectile still END on it (their first body is
the port's `castRay`) and land UNTARGETED at the point; the swept ray, the cone and the swing pass over it. Whether the
port's `castRay` should skip a summon is 2b's question (the port filter), not answered here.

**Sites converted** (file:line at this commit): `CastExecutor` 384 CONE, 823 CRYSTAL_SHOT, 1166 RAY, 1217 RAY (swept),
1273 SWING; `BehaviourHits` 61, 82, 95 (accounting), 135 SHOCKWAVE; `EffectApplier` 243 BURST, 282 IMPLOSION, 340
(accounting), 353 (the burst and area filter), 398 TRAVEL_END_BURST, 407 AREA; `Ignite` 301 IGNITE_SPLASH;
`ProjectileFlight` 216 PROJECTILE, 425 HOMING_PICK; `SweptLine` 45 DASH_LINE. **No `state().player()` filter remains in
core main** (`grep -rn "state()\.player()" core/src/main` is empty). The snapshot carries `Side side` (its five builders:
`BukkitCombatant` as `instanceof Player` until 2b's classifier, `ShotStamp` copies it, `FakeWorld` and `ShotStampTest`).

**Proof (H7):** `SummonSidesCoreTest` (11): each path run twice on one staging, the victim a mob (the control, hit) then
a fake summon (spared): swing, ray, swept ray, projectile, cone, burst, area, travel-end burst, dash line, Ignite,
implosion (both settings). Beside their own player rows: `BehaviourHitsTest.theShockwaveSparesASummonStandingInIt`,
`OrbBeamTest.theCrystalShotSparesASummon`, `ProjectileHomingTest.aBoltNeverChasesASummonEvenWhenTheSummonIsNearer`.
`MobHostilityTableTest` (5): every cell. **Mutations, all red:** MUTPVP, MUTSUMCELL, MUTMOBSUM, MUTCONE, MUTSWING,
MUTNEARBY, MUTIMPLODE, MUTPROJ, MUTIGNITE, MUTSHOCK. (The first run of MUTCONE, MUTSWING, MUTIMPLODE and MUTSHOCK did
not compile: the capture swallowed the `if`'s paren. Re-run correctly, each red.)

### 2.2 2b, THE PAPER SIDE (`feat/summoner-sides-paper`, on #290)

- **The one classifier, `adapter/CombatSides`:** a player is PLAYER; a body carrying the summon tag is SUMMON; anything
  else is MOB. It replaced all **seven** `instanceof Player ? PLAYER : MOB` copies (`MobBeamer`, `MobBlaster` x2,
  `MobPouncer`, `MobShooter`, `MobTeleporter`, `PhageFuser`) and `BukkitCombatant`'s snapshot builder, which now takes
  `Keys` (its seven callers pass `adapters.keys()`). `MobBeamer.eligible` and `.candidate`, and the three `side(l)`
  helpers, are instance methods now (they read the key). A test fails if any `instanceof Player ? ... Side.` returns.
- **The tag key, `Keys.summonOwner`** (`summon_owner`, STRING: the owner's UUID). **Nothing sets it before PR 3.**
- **The guard, `BukkitCombatant.Handle.refusedBySides`:** FIRST in `applyNow` and in `applyStatus`'s task, before the
  Phalanx shot's check. It refuses only `MobHostility.refusedSummonCell` (PLAYER -> SUMMON, SUMMON -> PLAYER,
  SUMMON -> SUMMON) and logs `SIDES refused <source>-><victim> <what> source=<id> victim=<id>`. The source's side comes
  from its id (`CombatSides.ofSource`: an online player, else the entity found by id). **FOLIA DEBT, named:**
  `Bukkit.getEntity` reads another entity off its region; it is correct on Paper today.
- **The three vanilla listeners** (`onPlayerMeleeAttack`, `onPlayerSweepAttack`, `onVanillaShotHit`) call
  `refusedForPlayer` before any damage is written; a refusal CANCELS the event, so neither vanilla's damage nor ours
  lands. Player -> player is untouched (the swing already returns for a player victim; the sweep already cancels one).
- **Targeting, `onSidesTarget` (`EntityTargetLivingEntityEvent`):** a targeting the table refuses is cancelled; today
  that is only a summon targeting a player or a summon. A mob may target a summon (H5).

**Proof (H7):** `CombatSidesTest` (5): the classifier on FAKE TAGGED BODIES (reflection proxies, as `LiveTargetsTest`
stubs entities: a tagged wolf is SUMMON, an untagged wolf MOB, a tagged player PLAYER); the guard's decision; the source
wiring of the damage port, the status port, the three listeners and targeting, each bounded by its own method; and no
side copy left. **What it cannot do:** fire a listener; that needs a server. **Mutations, all red:** MUTCLASSIFY,
MUTPLAYERFIRST, MUTGUARD, MUTMELEE, MUTTARGET.

## 3. PR 3a (`feat/summoner-3a`, on #291): THE DEFINITION, THE CAST TYPE AND THE CAP

**The summon file, `content/summons/<id>.yml`** (`SummonLoader`, the only class that knows it; `core/summon/
SummonDefinition` validates). Keys: `entity`, `name` (`{owner}` is his name), `element`, `health`, `damage`, and D4's six
numbers, **every one required, no default**: `guard_radius` (8, BEN), `trigger_radius` (30, BEN), `chase_limit` (32,
SEAT PLACEHOLDER, from the caster), `teleport_back` (48, SEAT PLACEHOLDER), `attack_cadence_ticks` (20, SEAT
PLACEHOLDER), `damaged_window_ticks` (100, SEAT PLACEHOLDER: how long a mob that damaged him stays a reason). **Refused,
the file named and skipped:** an unknown entity or element; health or damage not above 0; a missing D4 number; a
fractional tick count; a `chase_limit` below the `trigger_radius` (the seat's D4: it would make Ben's 30 false); a
`teleport_back` not beyond the `guard_radius`. **No summon ships:** `content/summons/` does not exist in main. The
TEST-ONLY fixture is `paper/src/test/resources/fixtures/summons/test_wolf.yml` (the brief's numbers).

**One question for the seat, not decided:** D2 put `element` in the summon file; D3 says the hit is "in the weapon's
element". The file carries it, as D2 accepted; whether the hit reads the summon's or the weapon's (and whether a
mismatch is refused) is PR 4's, when the hit is built.

**The cast type `summon`** (`type: summon`, `summon: <id>`; `CastSpec.Summon`). The ENGINE NEVER RUNS ONE: the summon
service decides the cap before the cast is charged (D6), so `CastExecutor`'s arm throws, naming it a wiring defect. It
is not repeatable (a volley of summons is refused). Sites the sealed type reached: `AbilitySchema` (parse, repeatable),
`CastSpec.minimumCooldownTicks` (0: a recast replaces), `CastExecutor` (dispatch, repeat), `WeaponLoreLines` (one
payload per press), `ContentValidator` (names no visual; the summon id is resolved when 3b wires the registry into the
boot).

**The cap, `core/summon/SummonCap.decide(weaponId, liveWeaponIds, maxSummons)`** (D6, counted by weapon id):

| live (weapon ids) | max_summons | verdict |
|---|---|---|
| none | 1 | NEW |
| this one | 1 | REPLACE (full cost) |
| another | 1 | REFUSED_FULL |
| another | 2 | NEW |
| another + this one | 2 | REPLACE |
| another + this one | 1 | REFUSED_FULL (lowered below what lives: others 1 + 1 > 1) |
| none, or this one | 0 | REFUSED_FULL |
| any | empty (loading) | REFUSED_UNKNOWN |

`Verdict.charges()` is true only for NEW and REPLACE: a refusal costs no mana and starts no cooldown. The one notice and
the wiring before the charge are 3b's.

**Tests:** `SummonCapTest` (2), `SummonLoaderTest` (3: the fixture, no shipped summon, every refusal caused and named),
`SummonCastSchemaTest` (3), `SummonCastEngineTest` (2). **Mutations, all red:** MUTOFFBYONE, MUTOWNCOUNTED, MUTCHARGES,
MUTEMPTY, MUTREQUIRED, MUTCHASE.

## 4. PR 3b: STOPPED BEFORE THE BODY, ON A D1 READ (2026-11-24)

The seat: "BEFORE 3b's body code, the owed D1 READS, each marked ... If any READ contradicts D1 as ruled, STOP on 3b
with 3a pushed." All five were read from `run/versions/26.1.2/paper-26.1.2.jar` (mojang-mapped) with javap. **One bears
on whether a Wolf is the body at all, so 3b is stopped before any body code; nothing of 3b is built.**

**(a) Nothing re-adds goals: READ.** In the whole chain (`Wolf`, `TamableAnimal`, `Animal`, `AgeableMob`,
`PathfinderMob`, `Mob`), `GoalSelector.addGoal` is called only in `Wolf.registerGoals`, and `registerGoals` only from
`Mob`'s constructor. `setTame`, `applyTamingSideEffects` and `tame` add none. A body never saved is never reloaded, so
the constructor never runs again for it. (Paper's own server code re-adding vanilla goals: not read; UNVERIFIED.)

**(b) Never saved, as the orbs' proof: READ.** `Entity.persist` (set by `setPersistent(false)`); `Entity.saveAsPassenger(out,
a, b, force)` returns false when `persist` is false unless `force`; the one-argument `saveAsPassenger(out)` passes
`(true, false, false)`, force false. Which other callers force it: UNVERIFIED, as for orbs; D7's tag and sweep is the
backstop.

**(c) What a goal-less untamed wolf still does each tick: READ.** `Wolf.tick`: the wet SHAKE (sound, particles, a client
event) and the head tilt while "interested" (set only by `BegGoal`, removed). `Wolf.aiStep`: starts the shake, and calls
`NeutralMob.updatePersistentAnger`, which reads `getTarget()` and can call `startPersistentAngerTimer` and
`setPersistentAngerTarget` (the branch conditions not decoded: INFERRED). **So once PR 4 gives it a target, anger can
start with no goal; with no target goal its effect is the angry LOOK.** Proposal: harmless; or clear it each tick
(`stopBeingAngry`) if Ben does not want the angry face. The tail's health look reads only when tamed (it is not).

**(d) Which vanilla mobs react to a Wolf BY TYPE: READ** (every class under `net/minecraft/world/entity`, 1125 of
1125 extracted; the control found the 10 wolf-package classes; each hit read with javap):

| mob | what it does | changes a guard decision? |
|---|---|---|
| **AbstractSkeleton** (skeleton, stray, bogged, wither skeleton) | `AvoidEntityGoal<Wolf>`: FLEES it | **YES.** A skeleton targeting him within 30 is a trigger; it runs from the summon, so the chase stretches and past 32 (from him) is dropped. The guard mostly cannot catch archers. |
| **Llama** (and trader llama) | `LlamaAttackWolfGoal`: ATTACKS it | No decision, but it is hurt by them (MOB -> SUMMON yes) and an attacker of the summon is not a D4 trigger, so it does not fight back unless directed |
| **Rabbit** | flees it; the killer variant (`setVariant`) TARGETS it | the same as the llama, for a killer rabbit |
| **Fox** | `AvoidEntityGoal<Wolf>`: flees it | No (not hostile) |

**(e) Attributes: READ.** `Wolf.createAttributes`: movement speed 0.3, max_health 8, attack_damage 4; taming would set
max_health 40 (cancelled). Proposal: its health is our customHP like any mob's; vanilla max_health left at 8 (the store
is authoritative); attack_damage set to 0, since our goal applies our damage directly (D3) and a vanilla swing must
deal nothing even if one fired.

**THE QUESTION FOR THE SEAT AND BEN:** keep the Wolf (skeletons flee it, llamas and killer rabbits hunt it) and accept
or answer it (cancel the skeleton's avoid by removing nothing of theirs; we cannot without editing every skeleton's
goals), or choose another body that no vanilla mob reacts to by type. The look is Ben's; the mechanism is the seat's.

**Q1, PROPOSED (not built):** `CombatantHandle.applyDamage` gains an explicit attacker side (an overload); the summon's
hit passes `SUMMON` with the owner's id as the credit; `refusedBySides` reads the explicit side when given and classifies
by source id only when not. Test and mutation as ruled.

**Q4, PROPOSED (not decided), with today's READ:** `VanillaDamagePolicy.forCause` REROUTES a tracked mob's FALL, LAVA,
FIRE, FIRE_TICK, HOT_FLOOR, CAMPFIRE, CONTACT, DROWNING, SUFFOCATION, FREEZE and the rest into its customHP, and PASSES
KILL and VOID. Proposal: the summon takes NONE of fall, fire, lava, drowning, suffocation (it follows him and teleports
back; he cannot shield it from terrain), and VOID removes it with the cause `void`. Ben's call on feel.

**D7's lifecycle, as designed (not built):** removal causes and their homes: its death (`EntityDeathEvent`), his death
(`PlayerDeathEvent`), his quit (`PlayerQuitEvent`), his world change (`PlayerChangedWorldEvent`), a replace (the cast),
server stop (`onDisable`), the selftest's clear_state, the orphan sweep at enable and at entity-add (`OrbRemovals`'
pattern, tag after the store owns the body, sweep one tick later), and a backstop (proposed period 20 ticks) removing a
body the store does not own or whose owner is not online in its world. Trace lines proposed: `SUMMON spawn`, `SUMMON
target`, `SUMMON hit`, `SUMMON teleport`, `SUMMON removed cause=<death|owner-death|quit|world|replace|stop|clear|orphan|
backstop|void>`.

## 5. PR 3b (`feat/summoner-3b`, on #292): THE BODY, THE CAST, THE LIFECYCLE, Q1, Q4 (2026-11-24)

The seat: the Wolf stays for 3b; nothing assumes Ben's A/B/C. **No Java in the summon code names a wolf**
(`SummonWiringTest` asserts it); the entity comes from the YAML and is checked BY TYPE (`RpgPlugin.isSummonableEntity`:
a registered type whose class is a `Mob`). A 3b summon STANDS WHERE IT WAS SPAWNED AND DOES NOTHING.

**E1:** no `element` in the summon file; the loader refuses the key. **Q1:** `CombatantHandle.applyDamageAs(attacker,
amount, creditId, ...)`; the guard reads the explicit side first (`CombatSides.attackerOf`). **Q4:**
`SummonWorldDamage.decide` (VOID removes, KILL removes, a mob-sourced hit lands, every other cause refused), applied by
`RpgListeners.onSummonDamaged` at LOWEST: a mob's hit is priced (`MobDamagePricing`) and applied with side MOB; the
vanilla event is cancelled. READ before Q4: a vanilla mob's melee on a NON-player was priced nowhere.

**The cast (D6):** `WeaponFire.attempt` hands a `type: summon` cast to `SummonService.cast` before
`weaponService.fire` (the charge); `SummonCap` decides; a refusal returns `CastResult.SummonRefused(notice)` (a new
variant; the right-click and left-click listeners show it, `/rpg cast` and the Stone cannot produce it). **The scaling
(D3):** the held weapon's score (`GearScoreItems.heldScore`) read at the cast; health and damage = authored x
clamp(score) / 100 (READ: `GearScore` clamps to its band, MIN 100; the summon keeps the score it was scaled at).

**The body (D1), every call:** `World.spawn(Location, Class, Consumer)` with, inside the pre-add consumer,
`setPersistent(false)` (D1(b) READ), `MobGoals.removeAllGoals` (D1(a) READ), `Tameable.setTamed(false)` and
`setOwner(null)`, `ATTACK_DAMAGE` base 0 (D1(e)), the name from the YAML, `SummonStore.place` and
`CombatantStats.bootstrapIfAbsent(scaled health)` (the store owns it before the add); the tag `summon_owner` set AFTER the
add. Interactions, leashing and taming of a summon are cancelled. **D1(c) anger, READ in full** (`NeutralMob.
updatePersistentAnger`): it sets or clears the anger target and timer and never sets a target or deals a hit; so in a
goal-less summon anger is the LOOK only (OPEN FOR BEN), and in 3b, with no target, it never starts.

**The lifecycle (D7):** removal cause and its wiring: DEATH, OWNER_DEATH, WORLD_CHANGE, VOID, KILLED (`RpgListeners`),
QUIT (`RpgListeners.onQuit`), CLEAR (`SelfTestDrivers` clear_state), STOP (`RpgPlugin.onDisable` via
`SummonRemovals.removeAll`), ORPHAN (`SummonRemovals.sweep`, at enable and one tick after an add), REPLACE
(`SummonService.spawn`), BACKSTOP (`SummonService.backstopTick`, every 20 ticks, self-rescheduled on the global
scheduler; also restrips a body whose goals came back and logs `SUMMON goals restored`). **FOLIA DEBT:** the backstop and
`CombatSides.ofSource` read bodies by id off their regions.

**Tests:** `SummonLifecycleTest` (5), `SummonWorldDamageTest`, `CombatSidesTest` (+2), `SummonWiringTest` (6, with a control
on its member bound), `SummonLoaderTest` (E1). `VaultWiringSignatureTest`'s quit bound is now structural (it was "within 40
lines", which the quit handler's three new lines broke while the call stayed inside). **Mutations, all red:** MUTELEMENT,
MUTEXPLICIT, MUTASNULL, MUTVOID, MUTMOBHIT, MUTEND (the bound's control), MUTCHARGEGATE, MUTPERSIST, MUTRESTRIP,
MUTOWNERHERE, MUTREPLACEBACK, MUTQ4FIRST.

## 6. BEN'S RULING ON THE SKELETONS, AND PR 4 (2026-11-24)

**BEN'S RULING, 2026-11-24: A.** The Wolf stays; skeletons flee it as vanilla does. Offered and not taken: **B**
(skeletons stand and fight a summon) and **C** (another body). With A, a fleeing skeleton that passes the chase limit is
dropped and may be taken again when it qualifies again; no skeleton is special-cased.

### The owed reads, O1-O4 (before any gate row)

- **O1, CAN A SUMMON DIE: YES, AS BUILT.** READ, the chain: our store's damage (`BukkitCombatant.applyNow` writes only
  `ctx.stats().damage`; it calls no vanilla `damage()` and no `setHealth`) -> `HealthChange.reachedZero` ->
  `MobDeathSystem.shouldKill` (`reachedZero && !targetIsPlayer`; a summon is bootstrapped as a non-player) ->
  `mob.setHealth(0)` -> `CraftLivingEntity.setHealth` (javap: `handle.setHealth` then `LivingEntity.die(generic)`) ->
  `die` -> `dropAllDeathLoot` -> `CraftEventFactory.callEntityDeathEvent`. **No EntityDamageEvent on the chain**, so Q4's
  refusals never see it. **ONE FIX:** the death event is CANCELLABLE (a revive); the summon's death hook was NORMAL and
  would have let the store go for a death another plugin cancelled, leaving a tagged orphan only an enable sweep could
  find. It is now `MONITOR, ignoreCancelled = true`. Left to the boot: the death itself and its `SUMMON removed
  cause=death` line.
- **O2:** a body already gone is logged `at=unknown`, never `0.00 0.00 0.00`.
- **O3:** the backstop counts foreign goals BY KEY (`SummonBackstop.foreignGoals`: each goal's key namespace against
  ours), so PR 4's own goals never read as foreign.
- **O4, READ:** the only throttled refusal notices are `BrokenNotice` and `QuiverNotice`: the player's `CooldownTracker`
  under a reserved key, 40 ticks. **The cooldown and no-mana notices are NOT throttled** (they send on every press), so
  the seat's premise that they are was not what the code does. The refused summon uses THAT mechanism (`SummonNotice`,
  key `__summon_refused_notice`, 40 ticks: SEAT PLACEHOLDER), no second one.

**Tests:** `SummonLifecycleTest` +2 (O2, O3), `SummonWiringTest` +2 (O1's chain as far as a test reaches, O4).
**Mutations, all red:** MUTFOREIGN, MUTUNKNOWN, MUTDEATHMON (first run overreached to five handlers sharing the annotation;
redone scoped to the death hook's line, one line changed), MUTTHROTTLE.

### 6.1 THE GUARD, DECIDED PURELY (`core/summon/SummonGuard`), AND WHERE PR 4 STOPS

`SummonGuard.decide(def, him, summonAt, held, candidates, now)` returns the target, its `Why` (DIRECTED, DAMAGED,
TARGETING or NONE), the held target's `Dropped` reason (DEAD, GONE, LIMIT) and whether to teleport. `needsReturn` says
when an idle summon walks back. Tables (`SummonGuardTest`, 5):

| case | result |
|---|---|
| directed C (25), damaged A (20, 50 ticks ago), targeting B (10) | C, DIRECTED |
| damaged A and targeting B | A, DAMAGED (though farther) |
| targeting B alone | B, TARGETING |
| a mob that did neither (he only hit it) | none |
| damaged exactly 100 ticks ago / 101 | taken / forgotten |
| targeting him at 30 / at 30.01 | taken / not a trigger |
| two targeting him, the summon nearer the farther one | the one NEAREST TO HIM |
| a PLAYER or a SUMMON, directed, damaging and targeting | never |
| held target at 33 from him | dropped LIMIT |
| held target dead / not among candidates | dropped DEAD / GONE |
| summon at 48.01 / 48 from him | teleport / not |
| idle at 8.01 / 8 | walks back / stays |

**Mutations, all red:** MUTPRIO, MUTWINDOW, MUTTRIGGER, MUTNEAREST, MUTSIDE, MUTLIMIT, MUTTELE, MUTRETURN.

**STOPPED HERE, BY THE SEAT'S RULE ("any PR is bigger than it reads ... STOP there with the earlier work pushed").** What
PR 4 still needs, each a review on its own, proposed as the split:
- **4b, the server goal:** a Paper `Goal` (keyed in our namespace, O3) that runs the decision each tick, paths with the
  mob's pathfinder, applies OUR hit by `applyDamageAs(SUMMON, ...)` every `attack_cadence_ticks` in reach, the swing and
  sound as visuals, the trace lines; and the feed for "damaged him" and "targets him" from the events that exist,
  with a mob's projectile counted as that mob's. Every server call READ by javap first.
- **4c, the left click:** the `direct_summon` cast type (core `CastSpec`, schema, its sealed arms), fired through the one
  left-click path, the first MOB on our ray; the READ of how a client's left click on an entity versus air reaches it.
- **4d, the selftest steps and lint rules** D8 needs, with their grammar.
Then PR 5 (the Bone Whistle content and the two bone READs) and the gate, with its SAFETY sum and R rows.

### 6.2 F1-F4 (on #294) AND 4b, THE GOAL ON THE SERVER (`feat/summoner-4b`)

**F1-F4 (the seat's defect):** `SummonGuard.decide(def, him, summonAt, directed, current, candidates, now)`: directed
(set only by the left click; cleared and reported dropped when dead, gone or beyond the chase limit) and current (last
tick's) are separate; the tier is re-decided every tick; a higher tier pre-empts; within a tier the current target is
kept while it qualifies; `why` is this tick's tier. Rows in `SummonGuardTest.theTierTable`; mutations MUTKEEP,
MUTPREEMPT, MUTHISTORY, MUTSAMETICK, MUTCURDROP.

**4b, the goal (`paper/summon/SummonGoal`)**, one per body, keyed `<our namespace>:summon_guard` (O3), added after the
tag. Each tick on the body's thread: candidates near HIM within the chase limit (plus the directed and current ones by
id), `SummonGuard.decide`, then: clear the order if told; teleport to him (`SUMMON teleport`); a change of target or a
drop logged (`SUMMON target ... why=<tier> [dropped=<reason>]`); path to the target and, within reach, every
`attack_cadence_ticks`, swing (visual) and OUR hit: `applyDamageAs(SUMMON, damage, owner, NORMAL, APPLIES, weapon
element, weapon accrual)`, logged `SUMMON hit` with every term; idle, walk back beyond the guard radius. Server calls
READ in paper-api: `Goal`, `GoalKey.of`, `GoalType`, `Mob.getPathfinder().moveTo(Entity, double)`, `stopPathfinding`,
`LivingEntity.swingMainHand`, `Mob.getTarget`.

**"Damaged him"** is `core/summon/SummonFeeds`, a listener in the store's composite: a non-player dealer damaging a
player for more than nothing, at the tick; forgotten on his quit. READ: `RpgListeners.attributableId` names the CAUSING
entity, so a mob's projectile is that mob's damage. **"Targets him"** is read live: `Mob.getTarget()` is him.

**CC'S CALLS (4b), each with its alternative:** pathing speed 1.0 (a YAML number); reach 2.0 blocks centre to centre
(vanilla's attack bounding box); the feed keeps entries 1200 ticks (the window alone); the goal does not call
`setTarget` (calling it would raise `EntityTargetEvent` and start vanilla anger, the look); the restrip removes only goals
outside our namespace (removing all and re-adding ours). The goal's types are MOVE and LOOK.

**Tests:** `SummonFeedsTest` (3; one row added when MUTFEEDTARGET first SURVIVED: the target check had no row of its own),
`SummonWiringTest` +2. **Mutations, all red:** MUTHITSIDE, MUTCREDIT, MUTCRIT, MUTRESTRIPALL, MUTFEEDPLAYERDEALER,
MUTFEEDTARGET.

### 6.3 4c, THE LEFT CLICK (`feat/summoner-4c`)

**READ (our code), how a click reaches the one left-click path:** `WeaponSwingListener` fires the held weapon's
`left_click` on the client's main-hand ARM-SWING packet (`PacketType.Play.Client.ANIMATION`), hopped to the player's
thread, then `WeaponFire.attempt`. **INFERRED (client behaviour, not readable from the server jar):** a vanilla client
sends that swing on a left click on AIR and on an ENTITY (with the attack). A click ON a mob is also vanilla's melee
event, so `onPlayerMeleeAttack` STANDS DOWN for a weapon whose `left_click` is a `direct_summon` (cancelled: no damage,
no flash), D5's "the swing deals no damage of its own".

**`type: direct_summon` (`CastSpec.DirectSummon(reach)`, reach REQUIRED):** `WeaponFire` hands it to
`SummonService.direct` BEFORE the charge. Nothing of his alive for this weapon, air, or a first body that is not a MOB
(`directable`: `harms(SUMMON, side)`): EMPTY, which the left-click path renders as nothing. A picked mob charges the
cast (so it costs only what the YAML authors) and, on success, becomes that summon's DIRECTED target
(`SummonStore.direct`); the goal logs `SUMMON target ... why=directed` the next tick.

**CC'S CALLS (4c):** the pick's ray passes THROUGH summons (as his shots do, Q3) and STOPS at a player (alternative: pass
through players too, to the first mob); nothing picked returns empty rather than a new silent `CastResult`
(alternative: a variant). **Tests:** `SummonDirectTest` (3). **Mutations, all red:** MUTDIRECTABLE, MUTCHARGEFIRST,
MUTSTANDDOWN, MUTREACHDEFAULT.

### 6.4 4d, THE SELFTEST STEPS AND LINT RULES (`feat/summoner-4d`)

**What D8 needs, and what already existed (READ in `Scenario.Verb`):** a left click aimed at a tagged mob is
`aim: "<tag>"` then `reload: ""` (RELOAD drives `WeaponSwingListener.swing`, the method the arm-swing packet hops to);
what a mob is targeting is `probe_target: "<tag>"`; a vanilla mob with its AI and a staged attack is `cmd: "summon <type>
~ ~ ~ {Tags:[...],attributes:[{id:\"minecraft:attack_damage\",base:<n>}]}"`. **New:**
- `probe_summon: "<reach>"`: one line per summon of his, `SUMMONPROBE weapon=<w> body=<u> alive=<b> health=<cur>/<max>
  at=x y z dist_him=<d> within=<reach>:<b> target=<u|none> directed=<u|none>`; `SUMMONPROBE none owner=<u> store=<n>` when
  he has none. Never judges.
- `mob_target: "<tag>"`: the nearest living mob with that tag within 16 targets him (`Mob#setTarget`); `DRIVE mob_target
  <uuid> tag=<tag>`, or a skip naming the tag.

**Lint:** `MOB_ACT_UNSTAGED`: a `cmd: summon` with no `NoAI:1b` must carry `attack_damage` (READ: 0 such summons in the
shipped scenarios at this commit, so only `ScenarioLintTest` causes it). `PROBE_SUMMON_REACH`: `probe_summon` names a
positive number. **Mutations, all red:** MUTFROZEN, MUTATTACK.

## 7. PR 5, THE BONE WHISTLE (`feat/summoner-5`, on #297)

`content/weapons/bone_whistle.yml`: `bone_whistle`, "Bone Whistle", class summoner, common, element nature, material
bone; right click `type: summon` (`bone_whistle_wolf`), 40 mana, `cooldown_ticks` 200; left click `type: direct_summon`,
reach 32. `content/summons/bone_whistle_wolf.yml`: wolf, "{owner}'s Wolf", 100 / 20, 8 / 30 (BEN), 32 / 48 / 20 / 100
(SEAT PLACEHOLDERS). `content/recipes/bone_whistle.yml`: "B","B","S", bone and stick. **"Wood tier" is a convention, not
a key** (READ: no weapon declares a tier; common rarity and a basic recipe). The tooltip, from the golden file: Nature;
"Call the Wolf", Right-Click, "Summons a wolf: 100 health, 20 damage / (x gear score / 100). Recast to replace it.",
"Cooldown: 10.0s | Mana Cost: 40"; "Sic", Left-Click, "Sends your wolf at the mob you point at / (up to 32 blocks)."; the
flavor; "Common Summoner Weapon". Ability names, descriptions and flavor: SEAT PLACEHOLDERS (numbers real, B5).

**THE TWO BONE READS:**
- **As a crafting ingredient:** READ, our code: `onPrepareCraft` -> `CraftMatrixScreen.verdict` blanks the result of any
  vanilla matrix holding an item of ours; `onCrafterCraft` covers the Crafter block. So a minted bone makes no bone meal.
- **Clicked on a vanilla wolf:** READ, javap: `Wolf.mobInteract` reads `Items.BONE` and calls `tryToTame` (which
  consumes the item). STOPPED by a new hook, `onSummonerWeaponEntityInteract`: any entity click with a minted SUMMONER
  weapon in that hand is cancelled. CC'S CALL: scoped to summoner-class weapons (alternative: every minted weapon whose
  material vanilla uses on an entity).

**Invariants recounted, each with its command:** weapons 23 -> 24 (`ls content/weapons/*.yml | wc -l`), gear
definitions 53 -> 54, recipes 7 -> 8, shipped summons 0 -> 1, the master pool snapshot +1 (`bone_whistle=unbreaking`, the
summoner's universal pool), gear asked 59 -> 60; the golden lore regenerated with its diff predicted first: one 15-line
block and the footer (102 -> 103 renderings), +16 / -1, read. **Mutation:** MUTTAME, red.

## 8. THE GATE (`feat/summoner-gate`, on #298)

`docs/gates/GATE-summoner.md`, `selftest/GATE-summoner.yml` (setup, SM1, SM2, SM3A, SM3B, SM3C, SM4-SM11), its R0b scan
`docs/gates/GATE-summoner-r0b.ps1`. Typed as `/rpg selftest mage-guaranteed+summoner`. SAFETY: 5.000 bound on him
(one scripted 0.2 at gs <= 500). Lint green and pinned clean. Rows recounted: 410 across 48 gates.

**CC'S CALLS (each with its alternative):**
- **Difficulty EASY for this gate** (alternative: NORMAL, as the mage gate; EASY only lowers the bound, and the
  restore puts the world's own difficulty back).
- **Mob hits staged by the vanilla `damage <e> <n> minecraft:mob_attack by <frozen zombie>` command** (SM3A, SM7, SM9),
  so no mob acts on him (alternative: a live zombie with a small attack set on him; its hit count is a range).
- **SM4's "merely hit" uses an iron sword** (alternative: the whistle; its left click stands down and deals nothing,
  so it hits nothing at all).
- **SM6 puts the wolf on the ray's line by a same-tick `tp ^ ^-0.6 ^3`** (alternative: a ground-level target; a wolf
  is shorter than the eye-to-body line, so it would not be between them).

**OWED, NOT WRITTEN:** the seat's sides rows for a shockwave, a crystal shot and a burst (only the ray is written);
the load-time ContentValidator check that a weapon's summon cast names a loaded summon (the cast refuses at runtime with
a warnOnce, READ).

## 9. THE GATE REWORKED (the seat's X1-X11, 2026-11-25, on `feat/summoner-gate`)

The gate was not approved: rows were chained, nothing read the wolf attacking or following, SM6 could pass without
testing, and the owed sides rows were missing. All fixed on the gate branch; the rows are now setup, SM1, SM2,
SM3A-SM3D, SM4-SM7, SM8A-SM8C, SM9-SM17 (419 scenarios across 48 gates).

**CODE ON THIS BRANCH (each with a test and a mutation, all red):**
- **X1, the lint:** `SUMMON_UNSTAGED` (a row reading a summon with no `use` after a `clear_state` earlier in it);
  `SUMMON_SELECTORS` pinned to the shipped summons' entities. MUTLINTCAST; and a planted unstaged SM13 in the real gate
  reddened `SelfTestScenariosTest` with two findings.
- **X2:** the SUMMON hit line ends ` tick=<n>` (the server tick at the goal's tick), so the cadence is readable.
- **X4(a):** `castRay`'s filter logs `SIDES pass attacker=<side> body=<uuid>` under trace, only for a refused body ON the
  line (READ: `CraftWorld.rayTraceEntities` filters before the box test). MUTSIDESPASS, MUTSIDESBOX.
- **X8:** the run's `end()` removes all his summons (cause `clear`) after the restore; the restore already removed every
  `selftest`-tagged mob on every exit. MUTENDSUMMONS.
- **X10:** `WeaponLoader.requireLoadedSummons` refuses at load a weapon whose summon cast names no loaded summon.
  MUTSUMMONREFUSE (first run did not apply: a brace in the replacement; re-run with `#` delimiters, red).

**CC'S CALLS (each with its alternative):**
- **The lint's "own cast" is a `use` after a `clear_state`** (alternative: a `use` while holding a summoner weapon, which
  the pure lint cannot know without the registry).
- **The selectors are a fixed list held to the shipped summons by a test** (alternative: pass the registry into the lint).
- **The run's end removes summons with cause `clear`** (alternative: a twelfth cause, END, in the core's enum).
- **The hit line carries the tick** (alternative: a separate trace line per hit with the tick only).
- **SM2 stays on the pad:** he steps to (-2, -6) and the wolf goes to (45, 10), 49.65 apart (alternative, the seat's:
  stand + 50, past the barrier rim at x 46 and over the void).
- **SM6 and SM16 stand the wolf on one saved stone block** so its box crosses a line at his eye height (alternative:
  a same-tick tp into the air, which falls, and whose order against the ray is INFERRED).
- **SM3D drives `attack` then `reload` in one tick** (the two packets of a real entity click).
- **The "burst or area" row is Airburst Flames' bursts** (the shipped area alternative, Sunder's burst, needs his class
  cell and a saved loadout the restore does not undo).

**NOT STAGEABLE YET (X7):** the cap with another weapon's summon; covered by `SummonCap`'s table.
