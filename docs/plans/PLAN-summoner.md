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
