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
