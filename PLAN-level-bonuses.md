# PLAN — Character level polish, stat bonuses for levelling up, the level cap, and the level screen

**Phase 1: SURVEY. No Java, no content, no build, no boot.** Revised 2026-09-29 after Ben ruled the shape, the
stats, the numbers and the cap, and asked for a clickable head (§0). What Ben ruled is quoted verbatim. What
the seat read into it is labelled as the seat's. What is still open is left blank as `___ (Ben)` and listed in
§10.

Read against master **`2d60e9a3`** (#167, the branch `docs/batch-surveys` sits on it), and against
`origin/feat/melee-m1` at **`e305ab3a`** (PR #168, open, not merged) for §5. Citations name METHODS and
SECTIONS, never line numbers (CLAUDE.md, *the pointer names a SECTION*). The short paths follow
`PLAN-melee-class.md`: `P/` = `paper/src/main/java/io/github/butterflysmp/rpg/paper/`,
`C/` = `core/src/main/java/io/github/butterflysmp/rpg/core/`, `S/` =
`storage/src/main/java/io/github/butterflysmp/rpg/storage/`, `content/` = `paper/src/main/resources/content/`.

**The brief** (from the seat, not committed): *"Polish of the Character level and adding stat bonuses
for leveling up."*

**How each claim was got, marked in place:**

| mark | meaning |
|---|---|
| **[read]** | read in the source or a doc at `2d60e9a3` (or at `e305ab3a` where it says so) |
| **[executed]** | computed by running Java (JDK 26.0.1, `.jdks/openjdk-26.0.1`) against **every `core/src/main/java` file at `2d60e9a3`, compiled into the scratchpad** (the tree was clean), calling the real methods (`PlayerLevel`, `HeartScale`, `Defense`, `HitDamage`, `MobScaling`, `DimensionGearScore`, `StatsSheetLines`, `Crit`). The seat's level-bonus formulas were written out as a small scratch function (§2.5). Nothing is predicted |
| **[jar]** | read with `javap` from `run/versions/26.1.2/paper-26.1.2.jar` (the server) or `~/.m2/.../paper-api-26.1.2.build.74-stable.jar` (the API, which is `pom.xml`'s `paper.version`) |
| **[inferred]** | my reading of how read parts join up. Not traced end to end and not run |
| **UNVERIFIED** | not checked. A later slice checks it before anything depends on it |

---

## §0 RULINGS

### 0.1 Ben, 2026-09-29, VERBATIM: the request

Copied byte for byte from the seat's `ben-level.txt`:

```
Ben: "I want to lower max player level to 50. Keep the numbers for everything higher in the
game files but right now 50 is the max. Players get +5 max health and +1 damage per each
level. Every 5 levels they get +1 health regen starting at level 10. +1 defense every 5 levels
starting at level 5. +1 crit chance every 10 levels starting at level 10.
I also want to make the player head clickable and open into a new sub menu with info of what
level the player is, how much XP they need for the next level, a list of the levels available
and the bonuses and unlocks for each level."
```

### 0.2 Ben, 2026-09-29, VERBATIM: answers to the seat's questions

Copied byte for byte from the seat's `ben-level-answers.txt`. The fifth and sixth lines resolve Q1 and
**replace** the first line's reading:

```
  Q1 (does level 1 already get +5 HP?): "Do a +10 instead of +5 at level 50"
  Q2 (+1 crit chance = +1%?): "yes, +5% at level 50"
  Q3 (which hits does +1 damage reach?): "Only weapons, we'll tackle the ability damage
     pipeline later"
  Q1 resolved: "no, every level gives +5 and only the level
49 -> 50 gives +10 for a total of +250."
```

### 0.3 THE SEAT'S READING of 0.1 and 0.2 (labelled as the seat's, not Ben's)

| item | the seat's reading | status |
|---|---|---|
| **Cap** | the ACTIVE cap is **50**. `PlayerLevel.MAX_LEVEL` stays **99**, and the curve's rungs 1..98 stay in the file | the seat's. The mechanism is open (§4A) |
| **Max health** | **level 1 = +0.** Each level-up from 2 to 49 gives +5 (48 x 5 = 240). The 49 → 50 level-up gives **+10**. **Total at 50 = +250** | Ben's number (+250); the seat's per-level spelling. **ANSWERED** |
| **Damage** | **ASSUMED, NOT CONFIRMED BY BEN:** the same convention, +1 per level-up, **level 1 = +0, level 50 = +49**. The alternative is +50 (level x 1) | **OPEN**, Q-L11 |
| **Health regen** | +1 at levels 10, 15, 20, …, 50: **9 steps** | the count is ruled. **The UNIT is open** (§2.5, Q-L12) |
| **Defense** | +1 at levels 5, 10, …, 50: **10 steps** | ruled |
| **Crit chance** | +1 **percentage point** at levels 10, 20, 30, 40, 50: **+5% at 50** (Ben: *"yes, +5% at level 50"*) | ruled |
| **Which hits damage reaches** | **weapon hits only**: melee swings, bow and crossbow shots, weapon triggers. **Actives stay gear-blind AND level-blind for now** | ruled (Q3). The entry point is open (§3.5, Q-L13) |
| **The ability damage pipeline** | a FUTURE THREAD (Ben: *"we'll tackle the ability damage pipeline later"*) | §6 F20 |
| **The base** | stacked on `feat/melee-m1` | ruled by the brief. Not re-opened |

**Superseded, kept as history:** after 0.2's first line, the seat asked Ben to choose between (a) +10 per
level instead of +5 and (b) +5 per level with +10 on level 50 alone. Ben's fifth and sixth lines chose (b)
and ruled that level 1 gets nothing. The four-number table that crossed (a)/(b) with "level 1 gets it / does
not" is dropped. Under the ruling, only one number stands: **+250**.

### 0.4 Standing rulings this survey rests on, not re-derived

- **STORE THE TOTAL, DERIVE THE LEVEL** (`C/progression/PlayerLevel`, class javadoc) **[read]**. The profile
  holds lifetime XP. The level is a view of it. *"If you find yourself writing a level to the profile,
  stop."* The bonuses (§2.5) and the cap (§4A) both stand on it.
- **NAME THE QUANTITY, AND NAME THE SET OF THINGS THAT HAVE IT** (`.claude/rules/standing-decisions.md`)
  **[read]**. §2.4 goes stat by stat. **§3.5 shows that Ben's damage ruling meets this rule head-on.**
- **M1, M2, M5, M16, M21, M24, M25** (`PLAN-mob-scaling.md` §0) **[read]**. Players are 5x vanilla health.
  Mobs are 5x vanilla x GS/100. Mob-dealt damage is FLAT (M16) and ignores difficulty (M24). Mob GS depends
  only on the DIMENSION (M25), and is stored on the mob and never re-rolled (M5). **M10: balancing player
  weapons and abilities, loot and XP scaling are out of scope for mob scaling.**
- **GS: WHAT SCALES IS WEAPON DAMAGE AND ARMOUR DEFENSE. NOTHING ELSE** (`C/weapon/GearScore`, class
  javadoc) **[read]**.
- **Q6: ACTIVES STAY GEAR-BLIND BY DESIGN** (Ben, 2026-09-28, in `PLAN-melee-class.md` RULINGS, **on the #168
  branch only**) **[read at `e305ab3a`]**. Q3 in 0.2 now adds **level-blind** for Actives "for now".
- **`AccessoryStat`'s javadoc, *Excluded, and why*** **[read]**: *"attack damage — a player's attack base
  is 0 BY DESIGN (weapon-only melee…). A flat accessory bonus would make an unarmed punch deal damage.
  CLASS_DAMAGE is the route that respects this: it applies only while a weapon of the matching class is
  held."* §3.5 leans on this.
- **No custom item stacks above 1**, and the **Boltor's numbers are ruled** (CLAUDE.md *Standing
  decisions*). Neither is touched. The Boltor's `attack_damage 19` is used below only as an input to
  arithmetic.

---

### 0.5 Ben, 2026-09-29, VERBATIM: the second answers, and the seat's rulings on `0f24131d`

```
  Regen: "It's +1 per 5 seconds for a total of +10 health per 5 seconds"
  Crit on Actives: "Yes"
  Damage at 50: "Yes"
```

**THE SEAT'S READING (the seat's, not Ben's):**

- **Regen:** +1 on the per-5s figure at each of levels 10, 15, ..., 50 (9 steps), i.e. +0.2 HP/s per step in the
  engine's unit. The base is `HealthRegen.BASE_PER_SECOND` = 0.2 HP/s, which the sheet shows as `1.00/5s`, so Ben's
  "+10" is the sheet TOTAL at 50: `10.00/5s` (2.0 HP/s). The base IS exactly 1/5s, so the 9 steps and the "+10" agree.
  **Q-L12 is ANSWERED: reading (R-5s).**
- **Crit on Actives: intended.** The level's +5% crit goes through the shared crit stat and reaches stone casts.
  **Q-L16 ANSWERED.**
- **Damage at 50: +49** (L1 = 0, +1 per level-up). **No longer an assumption. Q-L11 ANSWERED.**

**SEAT RULINGS (mechanism), 2026-09-29, on `0f24131d`:**

- **L1. "Weapons only" damage: the FLAG ON `Caster`.** It is exact. The #168 overlap is not a conflict: the level
  slice stacks on melee-m1 and edits `Caster` after it. The attack-stat route is REFUSED (it fails NAME THE QUANTITY:
  staffs and fixed-damage weapons get nothing). The class-damage slot is REFUSED (the ~5-tick leak into stone casts).
  The flag is set only at the weapon build sites (weapon triggers, `landBasicMelee`, shots). A unit test pins that a
  stone cast carries it false.
- **L2. The cap is a VIEW clamp.** Lifetime XP is never clamped. The displayed and effective level is
  `min(curve level, ACTIVE_CAP = 50)`, and the curve's rungs to 99 stay. The SAVE trigger keys on the UNCLAMPED curve
  level, so XP past 50 keeps writing at each curve rung, and a crash loses at most one rung, as today. No level-up
  message fires past 50. A later cap raise giving no message is a §6 finding, not a stop.
- **L3.** The level bonuses are a pure core function of the EFFECTIVE (clamped) level, with unit tests pinning every
  rung Ben ruled.
- **L4.** M17 stays separate from the mob body-fields slice.
- **L5. Stack order, bottom-up:** #168 (M1) → LEVEL (bonuses + the head sub-menu) → NEXUS polish → LEGACY-A (Short
  Bow, Blaze King's Staff: content only) → later slices. Each slice has its own PR and gate, and each R0c predicts
  against the slice below it. Where polish touches text #168's MC rows read, the NEXUS gate RESTATES those rows.
  M2 (Sunder) stays parked.
- **L6.** Nothing is built until Ben answers his questions for that slice.

## §1 HOW LEVELLING WORKS TODAY

### 1.1 The curve: `C/progression/PlayerLevel`

**[read]** Pure, no dependencies. **Cap `MAX_LEVEL = 99`.** It is not a formula. It is a
**hand-authored table**, `XP_TO_NEXT` (the XP to go from level *i* to *i*+1, for *i* = 1..98), and its
javadoc says ***"HAND-AUTHORED. DO NOT REGENERATE IT FROM A FORMULA"***. The anchors are `[1] = 1000`,
`[60] = 150000` and `[98] = 400000`. `cumulative()` builds `TOTAL_FOR_LEVEL` once, **sized
`MAX_LEVEL + 1`**, as a running sum.

| method | what it does |
|---|---|
| `levelFor(long lifetimeXp)` | the level. It clamps at both ends (≤ 0 gives 1, at or past the `MAX_LEVEL` total gives `MAX_LEVEL`) and **never throws**. A linear scan down from `MAX_LEVEL`; its comment says it runs *"on a menu open rather than per tick"* |
| `totalForLevel(int level)` | cumulative XP to reach a level. **Throws** outside `1..MAX_LEVEL`, on purpose |
| `xpToNextLevel(long)` | `OptionalLong`, **empty at the cap** |
| `intoCurrentLevel(long)` | progress into the current level |
| `isMaxed(long)` | `levelFor(...) >= MAX_LEVEL` |
| `plus(long, long)` | adds without wrapping (it saturates at `Long.MAX_VALUE`/`MIN_VALUE`) |

**Curve values [executed]:**

| level | total XP to reach | XP to next |  | level | total XP to reach | XP to next |
|---|---|---|---|---|---|---|
| 2 | 1,000 | 1,090 | | 30 | 121,150 | 11,740 |
| 3 | 2,090 | 1,190 | | 40 | 298,340 | 27,440 |
| 7 | 7,500 | 1,660 | | **50** | **712,580** | 64,160 |
| 10 | 12,940 | 2,150 | | 60 | 1,681,040 | 150,000 |
| 13 | 19,980 | 2,770 | | 75 | 4,393,400 | 220,920 |
| 20 | 45,360 | 5,020 | | 90 | 8,388,180 | 325,370 |
| 25 | 75,330 | 7,680 | | 99 | 11,642,250 | — |

Level 50 takes **0.0612** of the level-99 total [executed]. **Under the active cap of 50, the reachable
curve is levels 1 to 50: 712,580 XP.**

### 1.2 Where XP comes from: exactly one source

**[read]** `ProfileService.addLifetimeXp` has **one** caller, `RpgListeners.onPlayerExpChange`
(`@EventHandler(priority = LOWEST)`, on `PlayerExpChangeEvent`), which passes `event.getAmount()` one for
one. The other writer is `ProfileService.setLifetimeXp`, called only by `RpgCommand.playerXp`
(`/rpg playerxp`, `Permissions.DEV`) through `C/progression/XpGrant.targetLifetimeXp`.

**[jar]** The server's `CraftEventFactory.callPlayerExpChangeEvent(Player, ExperienceOrb, int)` needs an orb.
The API event extends `PlayerEvent` and is **not `Cancellable`**. **Player XP comes only from ORB
PICKUP.** `/xp` does not reach it (the handler javadoc's jar reading). The enchant table and the
grindstone cannot move the level. The full list of vanilla orb sources is **UNVERIFIED**.

**`ProgressionWiringSignatureTest` pins the hook by reading the source text [read]:** the handler's
declaration, `LOWEST`, no `ignoreCancelled`, and **the `profiles.addLifetimeXp(` call within 8 lines of the
declaration, passing `event.getAmount()` with no `*` or `/`**. Level-up feedback added to this handler must
keep that call inside the window (§6 F1).

### 1.3 The level-up path

**[read]** `ProfileService.addLifetimeXp(UUID, long)` ignores amounts of zero or less. It computes
`after = PlayerLevel.plus(before, amount)` and updates the cache. **It writes to disk only when
`levelFor(before) != levelFor(after)`.** Otherwise the cache is saved by `onQuit` and `saveAllAndClear`
(called at shutdown from `RpgPlugin`). **There is no periodic autosave** (grep of `P/` for
`saveAll|autosave`). Any other write-through (`setNexusSlot`, `setLifetimeXp`, the star and stone setters)
also writes the whole cached profile, lifetime XP included **[read: each saves `updated`, built from the
cached profile]**. It returns `OptionalLong`.

**There is no `PlayerLevelListener`** (§6 F3). **Nothing reacts to a level-up**: `onPlayerExpChange` throws
away the return value (§6 F1).

### 1.4 What the player sees

| surface | shows the player level? | **[read]** detail |
|---|---|---|
| **Nexus hub stats head**, `NexusMenuLayout.STATS_SLOT` = **13** (`NexusStatsLore.lore`, `progressionLines`) | **YES, the only player-facing place** | `Level` (GOLD, `"<n> (MAX)"` at the cap), `Lifetime XP`, `To Next` (left out at the cap). Painted once, when the hub opens. **Clicking it does nothing**: `NexusMenu.onClick` falls through on purpose (*"the stats head's click is still unbuilt"*) |
| **`/rpg stats`** | **NO** | stat lines, then `AccessorySheet`, then `FragmentSheet` (§6 F4) |
| **Action bar** (`StatsBarText.of`) | **NO** | HP, mana, defense, quiver |
| **Vanilla XP bar and green number** | **NO. That is the VANILLA level**, the enchant wallet (`C/xp/XpCurve`, `EnchantMenu`) | §6 F2 |
| `/rpg playerxp` reply (operator) | yes, to the sender | raw numbers (§6 F6) |

### 1.5 Level is derived, not stored, with one leftover

**[read]** `S/PlayerProfile` is at `CURRENT_SCHEMA_VERSION = 5`. `lifetimeXp` is a primitive `long`
(a missing value reads as 0, which is level 1). The record **still has the dead `int level` and
`long experience`**, written `1` and `0` by `PlayerProfile.fresh` and never read (§6 F5).

### 1.6 Every reader of the level, and what each shows at the active cap 50

"At 50" assumes §4A's option C1 (the displayed level stops at 50, `isMaxed` is true at 50) **[inferred]**.

| reader | what it reads | **what it shows / does at 50** |
|---|---|---|
| `PlayerLevel.levelFor`, `isMaxed`, `xpToNextLevel`, `intoCurrentLevel` | the table | the cap mechanism itself (§4A) |
| `PlayerLevelLines.level` | `levelFor`, `isMaxed` | **`"50 (MAX)"`** |
| `PlayerLevelLines.toNext` | `xpToNextLevel` | **throws at the cap by design**. Callers must ask `isMaxed` first |
| `PlayerLevelLines.lifetime` | lifetime XP | **keeps growing past 712,580** (it is lifetime, not "into level") |
| `NexusStatsLore.progressionLines` | the three above | `Level 50 (MAX)`, `Lifetime XP n`, **no `To Next` line** |
| `NexusMenu.viewerLevel` → `NexusStationGate.unlocked` / `lockedLore` / `refusal` | `levelFor` | every station open (the highest is 20) |
| `NexusVaultMenu.viewerLevel` → `VaultPageGate.unlocked` / `unlockedPageCount` / `anyUnlocked` / `refusal` | `levelFor` | **all 7 pages open. Page 7 opens exactly AT the cap (50)** |
| `ProfileService.addLifetimeXp` | `levelFor(before) != levelFor(after)` | **decides when XP reaches disk. At a clamped 50 it never fires again** (§4A, §6 F13) |
| `RpgCommand.playerXp` | `levelFor`, `isMaxed`, `intoCurrentLevel`, `xpToNextLevel` | *"now level 50, MAX into it, <n> lifetime"* |
| `XpGrant.targetLifetimeXp` / `addLevels` / `clampLevel` | `levelFor`, `totalForLevel`, `MAX_LEVEL` | **`clampLevel` folds to `MAX_LEVEL`.** Whether `set 60 levels` lands on 50 or on the hidden 60 depends on which cap it reads (§4A) |
| the level-bonus function (§2.5, new) | the level | +250 HP, +49 damage (assumed), 9 regen steps, 10 defense steps, 5 crit steps |
| the level screen (§4B, new) | the level, `xpToNextLevel`, the unlock list | the whole list 1..50 reached |
| `StatsBarText` (action bar) | — | does not read the level |
| **tests** | | `PlayerLevelTest` (53 references; pins 99, `11,642,250`, `totalForLevel(100)` throwing), `PlayerLevelLinesTest` (`"99 (MAX)"`, `"98"` one XP short), `XpGrantTest` (`add Long.MAX_VALUE levels` → **99**), `NexusStationGateTest` (`unlocked(station, MAX_LEVEL)`), `NexusStatsLoreTest` (`"Level        99 (MAX)"`, 3 lines at `LEVEL_99 - 1`) |
| **prose that quotes 99** | | `PlayerLevel` and `PlayerLevelLines` javadocs (`11,642,250`), `ProfileService.addLifetimeXp`'s *"rung 98 is 400,000 XP"* bound, and `GATE-nexus.md`'s row reading `Level        99 (MAX)` (a dated reading; it stays as history) |

---

## §2 STAT BONUSES: THE ENGINE, AND HOW A PER-LEVEL GRANT ENTERS IT

### 2.1 The engine [read]

`DESIGN-stat-engine.md`, *The stat model*: `value = base + Σ(modifiers)`. **Every future source is a
modifier source, not a new mechanism.** In code:

- `C/combat/stat/Stat`: a base plus a map of `(source key → amount)`. `putModifier` **replaces** by key.
- `C/combat/stat/HealthState`: one `Stat` per stat on a combatant. `C/combat/stat/CombatantStats`: the store,
  with `reconcile<X>Modifiers(UUID, Map<String, Double>)` and `<x>Value(UUID)` per stat.
- `ModifierReconciler.reconcile` **removes every applied source missing from the map**, so the rule is
  **TWO SOURCES, ONE RECONCILE CALL: merge first, reconcile once.**
- **The loop:** `P/health/PlayerHealthSystem.startReconcileLoop`, every `RECONCILE_PERIOD_TICKS = 5`, on
  the player's entity thread. Each pass reads `accessoryContributions(id)` and `fragmentContributions(id)`
  once (both already read `profiles.profile(id)`), and merges them with the varargs
  `AccessoryContributions.merged(gear, others...)`.

### 2.2 A level source beside gear, accessories and fragments

**[inferred from 2.1. Not built, not run.]**

```
LevelContributions fromLevel = levelContributions(id);   // profile -> level -> LevelBonus (pure, core)
stats.reconcileMaxModifiers(id, AccessoryContributions.merged(desiredMax,
        fromAccessories.sources(MAX_HEALTH), fromFragments.sources(MAX_HEALTH), fromLevel.sources(MAX_HEALTH)));
```

- **The key vocabulary exists:** `C/accessory/AccessoryStat`, the same key fragments use
  (`FragmentContributions` is a `Map<AccessoryStat, Map<String, Double>>`).
- **One source key per stat** (for example `"level"`), so a level-up changes one amount.
- **`P/health/FragmentWiringSignatureTest`** pins the text `"fromAccessories.sources(AccessoryStat.X),
  fromFragments.sources(AccessoryStat.X)"` on one line at each of the seven merge points. A level map
  placed **after** the fragments' map still passes. One placed between them fails.
- **Timing.** Profiles load asynchronously, so a join shows no level bonus until the profile is loaded
  and the next pass runs (`onRespawn`'s javadoc accepts the same for gear).
- **`levelFor` per pass** runs 99 iterations per player every 5 ticks. Cheap **[inferred, not measured]**,
  but it breaks `levelFor`'s *"not per tick"* premise (§6 F7).

**Raising the BASE instead** (`Stat.setBase`) is worse by the repo's own rules **[inferred]**. `onJoin` and
`onRespawn` both `register(id, DEFAULT_PLAYER_BASE, true)`, `StatSourceBound` measures drawbacks against the
base, and the sheet could no longer show where a bonus came from.

### 2.3 Derived on read: nothing stored, nothing migrated

**[inferred from §1.1 and 2.2]** Ben's bonuses are a **pure function of the level** (§2.5), so:

- **nothing new is stored**, and `PlayerLevel`'s rule holds;
- **retuning the curve, the bonus table or the cap costs nothing**: the next reconcile pass re-derives it;
- **`/rpg playerxp set` downward shrinks the bonus on the next pass.** A lower max HP clamps current HP;
- **a crash cannot undo a level-up's bonus**, because `addLifetimeXp` writes on every level change. §4A's
  choice decides whether that stays true past 50 (§6 F13).

### 2.4 Stat by stat: NAME THE QUANTITY, with the actual base values

"Reaches Actives?" is **[inferred]** from `EffectApplier`'s `Damage` arm, which reads
`caster.critMultiplier()`, `caster.enchantDamagePercent()` and `caster.classDamageBonus()` (per-combatant
stats, snapshotted by `BukkitCombatant`) and `caster.triggerScore()` (the held item). **The stats are per
combatant. Only their SOURCES are keyed to the held item.**

| # | stat (`HealthState` field) | player base, as read | unit / shape | ARITHMETIC | ELIGIBILITY | reaches Actives? | verdict |
|---|---|---|---|---|---|---|---|
| 1 | **Max Health** (`max`) | **100.0** (`CombatantStats.DEFAULT_PLAYER_BASE`) | HP, continuous | passes. The heart bar is stepped (§2.5) but the HP value is not | passes | n/a | **SAFE** |
| 2 | **Health Regen** (`healthRegen`) | **0.2 HP/s** (`HealthRegen.BASE_PER_SECOND`) | HP/s, continuous. **x5 while saturated** | passes | passes | n/a | **SAFE** |
| 3 | **Max Mana** (`maxManaBonus`) | 0.0 (pool base `MAX_MANA = 100.0`, `RpgPlugin`) | continuous | passes | passes (every shipped Active costs mana) | yes (more casts) | SAFE (not ruled in) |
| 4 | **Mana Regen** (`manaRegenBonus`) | 0.0 (base rate 1.0/s **[executed]**) | continuous | passes | passes | yes | SAFE (not ruled in) |
| 5 | **Defense** (`defense`) | **0.0** | points: `damage x 100/(100+defense)` | passes | passes | n/a | **SAFE** |
| 6 | **Crit Chance** (`critChance`) | **0.15** (`Crit.BASE_CHANCE`) | probability, clamped to [0, 1] by `Crit.chance` | passes | passes | **yes** (accessory crit already does) | **SAFE, ceiling 1.0** |
| 7 | **Crit Damage** (`critDamage`) | 1.0 | bonus multiplier | passes | passes | yes | SAFE (not ruled in) |
| 8 | **Attack** (`attack`) | **0.0** (*"weapon-only: no weapon, no hit"*) | flat | passes | **FAILS**: only the `WeaponDamage` effect arm reads it, and **every staff, `cursed_emerald`, `volley_stone`, `quiver_stone` and `dragons_breath` hit with literal `type: damage`** | no | **FAILS ELIGIBILITY** (§3.5) |
| 9 | **Class Damage** (`classDamage`) | **0.0** | flat addend in `HitDamage.hitBase`, **read by BOTH damage arms** | passes | passes **if the level source is gated on holding a weapon** (§3.5) | **no, when gated on a held weapon** (apart from a lag of up to 5 ticks) | **the fit for "weapons only"** (§3.5) |
| 10 | **Enchant Damage %** (`enchantDamagePercent`) | 0.0 | percent | passes | — | — | not a fit (enchant-owned, per `AccessoryStat`'s javadoc) |
| 11 | **Attack Speed** | 1.0 | multiplier | **FAILS** (4-tick input grid) | — | — | not ruled in |
| 12, 13 | **Quiver Size, Reload Time** | 0 | whole numbers | **FAILS** | **FAILS** | — | not ruled in |

**Ben ruled in rows 1, 2, 5, 6 and a damage bonus.** Rows 1, 2, 5 and 6 pass both halves. **Damage is the
one that needs care**, in §3.5.

### 2.5 BEN'S FIVE BONUSES: the stat, the unit, the function, the totals at 50

**A pure core function of the level**, derived on read (§2.3). A sketch of its shape, not code to copy:
`LevelBonus.of(int level) → Map<AccessoryStat, Double>` (plus the damage entry, whose key depends on §3.5),
defined for `1..ACTIVE_CAP`. **The seat's formulas, run as a scratch function [executed]:**

```
maxHealth(L) = L <= 1 ? 0 : 5 * (min(L, 49) - 1) + (L >= 50 ? 10 : 0)    // L1 0, L2 5, L49 240, L50 250
damage(L)    = L - 1                     // ASSUMED (seat); the alternative is L
regenSteps(L)= L >= 10 ? (min(L,50) - 10) / 5 + 1 : 0                     // L9 0, L10 1, L14 1, L15 2, L50 9
defSteps(L)  = min(L,50) / 5                                              // L4 0, L5 1, L50 10
critSteps(L) = min(L,50) / 10                                             // L9 0, L10 1, L50 5
```

**Milestone totals [executed]** (HP / damage (assumed) / regen steps / defense steps / crit steps):

| L | 1 | 2 | 5 | 9 | 10 | 15 | 20 | 25 | 30 | 40 | 45 | 49 | **50** |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| HP | 0 | 5 | 20 | 40 | 45 | 70 | 95 | 120 | 145 | 195 | 220 | 240 | **250** |
| dmg | 0 | 1 | 4 | 8 | 9 | 14 | 19 | 24 | 29 | 39 | 44 | 48 | **49** |
| regen | 0 | 0 | 0 | 0 | 1 | 2 | 3 | 4 | 5 | 7 | 8 | 8 | **9** |
| def | 0 | 0 | 1 | 1 | 2 | 3 | 4 | 5 | 6 | 8 | 9 | 9 | **10** |
| crit | 0 | 0 | 0 | 0 | 1 | 1 | 2 | 2 | 3 | 4 | 4 | 4 | **5** |

**Stat by stat:**

| bonus | existing stat: enum (YAML key) → `HealthState` field → reconcile | what "+1" is in the engine's unit | **total at 50 [executed]** | what the player sees |
|---|---|---|---|---|
| **Max health** | `AccessoryStat.MAX_HEALTH` (`max_health`) → `max` → `reconcileMaxModifiers` | **custom HP.** Base 100 = 5x vanilla's 20. Same unit gear already authors in (`fragment_vigor` `max_health: 4`) | **+250 → max 350.0** | `/rpg stats` "Max Health 350". **Hearts [executed, `HeartScale.heartCount`]:** 10 at L1 (100), **11 from L2** (105), **12 from L22** (205), **13 from L42** (305); 13 at L50 (350). **A heart is 10 HP only below 100. Above 100 it is 100 HP**, so +5 is a twentieth of a heart there |
| **Damage** | **not an `AccessoryStat`.** Two candidates, §3.5: `ATTACK` (`attack` → `reconcileAttackModifiers`, no YAML key: `AccessoryStat` excludes it on purpose) or **`AccessoryStat.CLASS_DAMAGE`** (`class_damage` → `classDamage` → `reconcileClassDamageModifiers`) | flat damage, the same unit as a weapon's `attack_damage` (Boltor 19, `emberblade` 7) and a literal `amount` (`ember_staff` 16) | **+49 (assumed) or +50** | the `/rpg stats` Damage line (`HitDamage.hitBase` of attack, enchant %, class) |
| **Health regen** | `AccessoryStat.HEALTH_REGEN` (`health_regen`) → `healthRegen` → `reconcileHealthRegenModifiers`. **It exists as a stat** | **HP per SECOND** in the engine. **The sheet SHOWS it per FIVE seconds** (`StatsSheetLines.perFiveSeconds`: base 0.2/s reads `"1.00/5s"` [executed]) | **UNIT OPEN (Q-L12):** **(R-s)** +1 HP/s per step: 0.2 + 9 = **9.2 HP/s**, shown **`"46.00/5s"`**, **46.0 HP/s** while saturated. **(R-5s)** +1 on the shown number (= +0.2 HP/s) per step: 0.2 + 9 x 0.2 = **2.0 HP/s**, shown **`"10.00/5s"`**, **10.0 HP/s** saturated [all executed] | For scale, shipped gear: `mending_charm` +0.1 HP/s, `fragment_mending` +0.05 HP/s [read] |
| **Defense** | `AccessoryStat.DEFENSE` (`defense`) → `defense` → `reconcileDefenseModifiers` | defense points (vanilla armour points: full diamond 20, `ward_charm` +3 = *"Protection I"*, `fragment_ward` +1) | **+10** → damage reduction `Defense.damageReduction(10)` = **0.09090909090909091** [executed] | "Defense 10"; the vanilla armour bar moves (`ArmorBarOverride`) |
| **Crit chance** | `AccessoryStat.CRIT_CHANCE` (`crit_chance`) → `critChance` → `reconcileCritChanceModifiers` | a **probability fraction**: base 0.15. **+1 percentage point = +0.01**. Gear authors in the same unit (`keen_charm` `crit_chance: 0.05`) | **+0.05** → 0.15 + 0.05 = **0.2** [executed]. **Adding 0.01 five times gives 0.20000000000000004 [executed]**, so the function should compute `steps x 0.01` once (5 x 0.01 = **0.05** [executed]) | "Crit Chance 20%" (`StatsSheetLines.critChance`, `Math.round` to a whole percent, **20% either way** [executed]) |

**Ambiguities for Ben, beyond Q1 (now answered):**

1. **Damage at L1: +0 or +1?** (+49 or +50 at 50). The seat ASSUMED +49 by the HP convention. **Q-L11.**
2. **Health regen's "+1": per second (R-s) or per five seconds as shown (R-5s)?** They differ by 5x.
   R-s makes a level-50 player regenerate **46x the base rate**, about **92x `mending_charm`**
   [inferred from the executed totals and the read gear numbers]. **Q-L12.**
3. **"Every 5 levels starting at level 10"** includes 10 (the seat's reading: 10, 15, …, 50 = 9 steps).
   Reading it as the first step at 15 gives 8. The seat's reading is consistent with *"+1 defense every 5
   levels starting at level 5"*, where 5 itself gives +1. Stated so Ben can see it. **Q-L14.**
4. **Max health's unit.** The seat reads "+5" in custom HP, the unit every gear file uses. If Ben meant
   vanilla health points, where 1 = half a heart = 5 custom HP at 5x, the total would be 5x larger.
   **Q-L14** (one line; the seat's reading is the one written).
5. **Above 50, when the cap is raised:** does the +10 stay a one-off at 49 → 50, with +5 per level after?
   Do the regen, defense and crit steps continue? **The function is only defined to 50 today.**
   **Q-L15.**
6. **Crit past 100%** is unreachable from level alone (0.2 at 50). Recorded for when gear stacks on it:
   `Crit.chance` clamps.

**The unit test the slice must write (SPECIFIED here, not written; docs only):**
`LevelBonusTest`, in `core`, asserting **the whole grid L1..L50 for every stat** (memory: *assert the whole
grid, because the error lands in one cell*), generated from the ruled words and not from the function under
test. Its pinned rows at minimum:

- `maxHealth`: **L1 = 0, L2 = 5, L49 = 240, L50 = 250**, and L50 − L49 = 10;
- `damage`: L1 = 0, L2 = 1, L50 = 49 (**under the ASSUMPTION; Q-L11 may move it to L1 = 1, L50 = 50**);
- `regenSteps`: L9 = 0, **L10 = 1**, L14 = 1, L15 = 2, L50 = 9;
- `defSteps`: L4 = 0, **L5 = 1**, L50 = 10;
- `critSteps`: L9 = 0, **L10 = 1**, L50 = 5, and the crit amount at 50 **equal to the value the function
  computes by executing it** (the test must not type `0.05` by hand and compare with `==` unless that literal
  was checked by running it; [executed] above: `5 * 0.01` prints `0.05`);
- **a control row:** L0 and negative levels are not reachable (`levelFor` never returns below 1), so the
  function should refuse them the way `totalForLevel` refuses a bad level, and a row asserts that it throws.

---

## §3 INTERACTIONS

### 3.1 Mob scaling (M1, M16, M24, M25)

- **Mob GS does not read the player [read].** `DimensionGearScore.of`: overworld 100, Nether 200, End
  `round(300 + max(0, d − 1000) / 100)` capped at `MobGearScore.CAP`. **A level bonus feeds nothing in mob
  scaling.**
- **Mob hits are flat**, so bonus HP and defense are real extra survival against mobs. **Environmental
  damage on a player is PROPORTIONAL** (`DamageScale.toCustom`, denominator 20), so **+250 HP does not help
  against fall, lava or drowning** [inferred from the read].
- The numbers are in §3.6.

### 3.2 Gear score

**[read]** A level bonus is not an item, so it is not in `GearScore.averageOf`. It moves neither the Nexus
head's `Gear Score` line nor the drop band (`GearScoreItems.stampOnAcquire` rolls from `averageOf`).
Weapon damage from gear scales with GS. **A flat level damage bonus does not**: the weapon's
`attack_damage` enters `reconcileAttackModifiers` already scaled (`WeaponAttackItems`,
`GearScore.scaledDamage`), and a literal is scaled in the `Damage` arm (`scaledDamage(d.amount(),
triggerScore)`). A level source would sit beside them, unscaled **[inferred]**.

### 3.3 Max health and the custom health store

**[read] `DESIGN-stat-engine.md`, *Max-HP change semantics*:** *"Max increases: current HP is unchanged, you
gain headroom, not health."* **A level-up does not heal** (Q-L4). **[jar]** Vanilla `max_health` is a
`RangedAttribute` (default 20.0, minimum 1.0, maximum `SpigotConfig.maxHealth`, which defaults to **1024.0**;
`run/spigot.yml` has `1024.0`). The level bonus raises only the CUSTOM max, which has no cap **[inferred]**.

### 3.4 Actives: gear-blind (Q6) and now level-blind (Q3, 2026-09-29): ANSWERED

Ben: *"Only weapons, we'll tackle the ability damage pipeline later"*. **Level damage must not reach a
stone cast.** Crit is different: crit chance is one per-combatant stat, and **accessory crit already reaches
stone casts today** [read, the `Damage` arm reads `caster.critMultiplier()`]. So **the level's +5% crit WILL
reach Actives** unless a new mechanism excludes it. **Q3 was about DAMAGE, so this is flagged for Ben
(Q-L16), not assumed either way.** The level's HP, regen and defense are defensive and have no "reach".

### 3.5 "WEAPONS ONLY": THE ENTRY POINT, TRACED

**[read] The damage composition exists in exactly two places**, both in `C/ability/effect/EffectApplier`:

- the **`WeaponDamage` arm**: `HitDamage.dealt(HitDamage.hitBase(caster.attackDamage(),
  caster.enchantDamagePercent(), caster.classDamageBonus()), chargeScale, critMultiplier)`;
- the **`Damage` arm** (a literal `amount`): `HitDamage.dealt(HitDamage.hitBase(
  GearScore.scaledDamage(d.amount(), caster.triggerScore()), caster.enchantDamagePercent(),
  caster.classDamageBonus()), chargeScale, critMultiplier)`.

`HitDamage.hitBase(base, pct, cls) = base * DamageEnchants.multiplier(pct) + cls`, and
`dealt = hitBase * chargeScale * critMultiplier` [read]. The only other readers of these stats are the
stat sheet (`StatsSheetProjection`), the dev `mobinfo` line, and `RpgListeners`' mob-attacker path, which
reads the MOB's `attackValue` [read].

**How each kind of hit reaches them [read, each path]:**

| hit | path | the `Caster` it carries | which arm |
|---|---|---|---|
| **melee swing** (`emberblade`, `ironblade`) | vanilla `ENTITY_ATTACK` → `WeaponFire.landVanillaMelee` (only if `WeaponItems.heldWeaponId` → `vanillaMeleeTrigger()` is present) → `CastExecutor.landBasicMelee` → `detonate` | `Caster.of(snapshot, chargeScale)` + `withTriggerScore(triggerScoreOf)` | `WeaponDamage` (their basic attack). A sweep takes `SweepShare.of(primary.damage(), fraction)` of the primary hit, so it inherits the primary's number |
| **bow / crossbow shot** (Boltor `type: ray`, `hunters_bow`, `locust`) | `WeaponService` → `AbilityService.fireTrigger` → `resolve` → `CastExecutor.commit` | `Caster.of(snapshot)` + `withPayloadDamage` + `withTriggerScore`, **frozen at the press** | `WeaponDamage` for the Boltor, `hunters_bow` and `locust`. **`dragons_breath` and `quiver_stone` shoot LITERAL `damage`**, so they go through the `Damage` arm |
| **weapon trigger, staff** (`ember_staff` right-click: `burst` of `damage 16`) | the same `fireTrigger` → `commit` | the same | **`Damage`** (a literal) |
| **stone-cast Active** (`recall`, `solar_lance`, `ember_step`, …) | `AbilityService.cast` → **the same `resolve`** → the same `CastExecutor.commit` | **the same `Caster.of(snapshot)`**, with `triggerScoreOf` of the held item (the stone: no stamp → 100) | **`Damage`**. **No shipped Active uses `weapon_damage`** (grep of `content/abilities/` on master, and the three melee placeholders at `e305ab3a`) |

**THE CRUX [read]: a stone cast and a weapon trigger build the SAME `Caster` through the SAME `resolve` and
`commit`.** On master, `Caster` has no field that says "this was a weapon". (#168 adds `String source`,
`"<weapon>/<input>"` for a trigger, but its javadoc says *"nothing prices or gates on it"*.) **So the damage
arms themselves cannot tell a staff's bolt from a stone's Active. "Weapons only" has to come from WHERE THE
STAT'S SOURCE IS PRESENT, not from the arm.** Three entry points:

| entry | how "weapons only" is got | **which weapons get NOTHING** | **what it catches that is NOT a weapon hit** | cost |
|---|---|---|---|---|
| **E1. The ATTACK stat** (`reconcileAttackModifiers`, a `"level"` source) | only the `WeaponDamage` arm reads `attackDamage`, and only weapon triggers author `weapon_damage` today | **every literal-damage weapon: `ember_staff`, `flint_staff`, `lapis_staff`, `cursed_emerald`, `volley_stone`, `quiver_stone`, `dragons_breath`**, plus the literal triggers of `emberblade` (its right-click Fireball) and `dragons_plume` [read, `content/weapons/*.yml`]. **So "+1 damage reaches every weapon" is FALSE under E1. Every mage gets nothing.** **THIS IS THE NAME THE QUANTITY ELIGIBILITY FAILURE, AND BEN MUST SEE IT** | (a) the **stat sheet's Damage line shows +49 for a player holding a staff, or nothing at all**, a number the staff never deals: the tooltip dishonesty the rule exists to stop; (b) `AccessoryStat`'s javadoc says a flat attack bonus *"would make an unarmed punch deal damage"* (a claim in the javadoc, **not traced by me: UNVERIFIED**); (c) `DamagePayload.headlineDamage(onHit, caster.attackDamage())` becomes a status's CAP (`withPayloadDamage`), so **Scorch's cap rises by the level bonus** on `weapon_damage` weapons [inferred]; (d) Sharpness (`enchantDamagePercent`) **multiplies** it; (e) an Active that ever authors `weapon_damage` would read it (none does) | smallest code: one merge argument |
| **E2. The CLASS_DAMAGE slot, with the level source gated on "the main hand holds one of our weapons"** | `classDamage` is added in **both** arms (`hitBase`'s last term), so every weapon hit gets it, literal or stat. It is present only while a weapon is held, the same gate `AccessoryStat.CLASS_DAMAGE`'s javadoc describes (*"applies only while a weapon of the matching class is held"*), minus the class match | **none** among shipped weapons [inferred from the table above: every weapon hit passes through `hitBase`] | (a) **the reconcile lag: a stone cast within about 5 ticks of switching from a weapon still carries it**, the same lag `PLAN-melee-class.md` §5.4 records for class damage [read at `e305ab3a`]; (b) it lands **per hit**, so multi-hit weapons get it per pellet or target: **`dragons_breath` fires `count: 7`**, and the `burst`s on `ember_staff` and `emberblade` hit every target in range [read]; (c) the stat now means two things ("class damage" from gear and "level damage"), though the sheet line already composes both into one "Damage" | the class-damage merge point (`ClassDamageModifierItems.desiredModifiers`, with `fromAccessories.classGrants()`) gains a level grant, gated on `WeaponItems.heldWeaponId(...).isPresent()` [inferred]. **No `Caster` change, so no conflict with #168.** Not multiplied by Sharpness, and not GS-scaled |
| **E3. A new "weapon trigger" flag on `Caster`**, set on the `fireTrigger` / `landBasicMelee` paths and read in both arms | exact, with no lag | none | none by construction | **touches `Caster` (a certain conflict with #168's new `source` component, §5.1)**, `AbilityService` / `CastExecutor` plumbing (the paths only diverge before `resolve`), and a new `HitDamage` term (the arm's own comments call that composition *"the 14.2-vs-14.95 hazard"*) |

**E1 is the cheapest and fails Ben's words for every mage. E2 meets them, with a 5-tick lag. E3 is exact
and conflicts with #168.** **Q-L13 for Ben.** (The seat's note that the entry point should be "beside
class_damage in `HitDamage.hitBase`" matches E2's placement.)

**Also UNVERIFIED:** how a **vanilla** bow or crossbow (not one of our weapons) is priced. "Bow and crossbow
shots" above means our ranged weapons.

### 3.6 MOB SCALING, AS ARITHMETIC ONLY [executed]

**The formula, confirmed in code [read]:** `MobScaling.maxHealth(base, isCustom, isHostile, gs) = base x
vanillaFactor x gsFactor` and `MobScaling.attackDamage(...) = vanillaFactor x gsFactor x base`, with
`VANILLA_FACTOR = 5.0` (1.0 for a custom mob) and `gsFactor = gs / GS_BASELINE (100)` for a hostile mob.
Melee is priced from the seeded vanilla `ATTACK_DAMAGE` attribute (`MobNameplateManager.seedCombatStats`;
`VanillaDifficulty`'s javadoc: *"Melee needs none of this: it is priced from the seeded ATTACK_DAMAGE
attribute, which difficulty never touches"*). A mob's hit on a player takes the player's Defense
inside `CombatantStats.damage` (`Defense.applyDefense`, unless `DefenseRule.BYPASSED`) [read].
**A mob's own Defense is 0** (`HealthState`'s `defense` is `new Stat(0.0)`, and `reconcileDefenseModifiers`
is called only from `PlayerHealthSystem`) [read], so a player's hit lands in full.

**The two mobs [jar, `createAttributes`]:**

- **Zombie** (`net.minecraft.world.entity.monster.zombie.Zombie`): `ATTACK_DAMAGE 3.0`. It sets no
  `MAX_HEALTH`, so the attribute default **20.0** applies. **GS 100**: every hostile overworld mob (M25).
- **Enderman** (`net.minecraft.world.entity.monster.EnderMan`): `MAX_HEALTH 40.0`, `ATTACK_DAMAGE 7.0`.
  Hostile by M21 (Enderman is listed). **GS 500 in the End from horizontal distance ≥ 20,950 blocks from
  (0, 0)** (`DimensionGearScore.of(END, d)`: 20,949 gives 499, 20,950 gives 500; 999 and 1,000 give 300)
  [executed]. It is stored at spawn (M5).
- A plain adult is assumed. Vanilla's random spawn bonuses (for example leader zombies) are **UNVERIFIED**.

**Mob numbers [executed]:** zombie GS 100: **100.0 HP, 15.0 damage**. Enderman GS 500: **1000.0 HP, 175.0
damage** (and GS 300, at the End's start: 600.0 HP, 105.0 damage).

**The player:** L1 = **100 HP, Defense 0**. L50 = **350 HP, Defense 10**. There is **no gear** in either
column, so this is the level-only difference. Full charge, no crit, no enchant.

**A mob's hit on the player: `Defense.applyDefense(mobDamage, playerDefense)`**

| mob | player | hit | share of player's max HP | hits to kill the player |
|---|---|---|---|---|
| zombie GS 100 | L1 (100, def 0) | **15.0** | **0.15** | **7** |
| zombie GS 100 | L50 (350, def 10) | **13.636363636363637** | **0.03896103896103896** | **26** |
| zombie GS 100 | HP only (350, def 0) | 15.0 | 0.04285714285714286 | 24 |
| zombie GS 100 | Defense only (100, def 10) | 13.636363636363637 | 0.13636363636363635 | 8 |
| Enderman GS 500 | L1 (100, def 0) | **175.0** | **1.75** | **1** |
| Enderman GS 500 | L50 (350, def 10) | **159.0909090909091** | **0.45454545454545453** | **3** |
| Enderman GS 500 | HP only (350, def 0) | 175.0 | 0.5 | 2 |
| Enderman GS 500 | Defense only (100, def 10) | 159.0909090909091 | 1.5909090909090908 | 1 |

**The player's hit on the mob: `HitDamage.dealt(HitDamage.hitBase(base, 0, levelDamage), 1.0, 1.0)`**,
counted as hits to kill. The column is E2's (the level adds flat). **Under E1, `ember_staff` stays at +0.**

| weapon (its base) | +0 | **+49 (assumed)** | +50 (alt.) | zombie GS 100 (100 HP): hits at +0 / +49 / +50 | Enderman GS 500 (1000 HP): hits at +0 / +49 / +50 |
|---|---|---|---|---|---|
| Boltor (`attack_damage 19`, ruled) | 19.0 | 68.0 | 69.0 | **6 / 2 / 2** | **53 / 15 / 15** |
| `emberblade` (`attack_damage 7`) | 7.0 | 56.0 | 57.0 | **15 / 2 / 2** | **143 / 18 / 18** |
| `ember_staff` (literal `damage 16`) | 16.0 | 65.0 | 66.0 | **7 / 2 / 2** (E1: 7 / 7 / 7) | **63 / 16 / 16** (E1: 63 / 63 / 63) |

No judgement is offered. The numbers come from the formulas named in each table's heading.

---

## §4 THE SHAPE OF THE BONUS: ANSWERED by Ben 2026-09-29

Ben's ruling (§0.1, §0.2) is **a hybrid of flat per level and milestones**: flat per level for HP and
damage, milestones for regen, defense and crit. **It is derived** (§2.3): no storage, no schema bump, no
migration. The option table in the first draft (S1 flat, S2 milestones, S3 YAML table, S4 points, S5 per
class, S6 hybrid) is **retired**, and the ruling is S6 built from S1 and S2. **Still open from it: where the
numbers live (Q-L2)**, a YAML table under `content/` (CLAUDE.md invariant 2) or Java beside
`PlayerLevel.XP_TO_NEXT`. A YAML table adds a term to the boot line's `Loaded ...` (§5.3).

---

## §4A THE CAP: AN ACTIVE CAP OF 50 BESIDE THE KEPT CURVE

### 4A.1 Confirmed from the code: lifetime XP keeps accruing past any cap, so raising it needs no migration

**[read]**

- `ProfileService.addLifetimeXp`: `after = PlayerLevel.plus(before, amount)`. **No clamp to any level.**
  `plus` saturates only at `Long.MAX_VALUE`.
- `PlayerProfile.withLifetimeXp(long)` stores any `long`. `ProfileService.setLifetimeXp` floors at 0 only.
  **Nothing in `S/` clamps it**, and Gson writes the `long` as-is.
- `onPlayerExpChange` passes every orb's amount whatever the level (`ProgressionWiringSignatureTest`
  forbids a multiplier).

**CONFIRMED: a level-50 player keeps banking lifetime XP, and a later raise of the cap lifts them at once,
with no migration.** Two consequences that are **not** free:

- **A raise is silent** [inferred]. Feedback computed as `levelFor(before) != levelFor(after)` inside one
  orb (§6 F1) compares two levels read under the NEW cap, so a player who jumps from 50 to 57 on the day of
  the raise sees no level-up at all. Announcing it needs a "last level shown" that is **stored**, which
  §0.4's rule resists. §6 F14.
- **The save trigger** (§4A.3).

### 4A.2 The options

| option | what changes | cost | tests that move |
|---|---|---|---|
| **C1. One new constant, `ACTIVE_MAX_LEVEL = 50`, in `PlayerLevel`, beside `MAX_LEVEL = 99`** | `levelFor` clamps to `min(curve level, ACTIVE_MAX_LEVEL)`. `isMaxed` and `xpToNextLevel` use the active cap. `totalForLevel` keeps `1..MAX_LEVEL` (the table stays whole and tested). `XpGrant.clampLevel` must pick one: `set 60 levels` → 50 (the reachable) or the hidden 60 | smallest honest change. Both names are visible: "the curve's length" and "the cap in force". **A raise is one edit and a release** | `PlayerLevelTest` (the 99-at-cap rows split into "curve to 99" and "cap at 50"), `PlayerLevelLinesTest` (`"50 (MAX)"`), `XpGrantTest` (`add Long.MAX levels` → 50), `NexusStatsLoreTest` (`"Level        50 (MAX)"`), `NexusStationGateTest` (reads `MAX_LEVEL`: still true at 50 because the highest station is 20, but the row's meaning changes) |
| **C2. Lower `MAX_LEVEL` to 50 and leave `XP_TO_NEXT`'s 98 rungs in the file** | `cumulative()` sizes its array `MAX_LEVEL + 1`, so rungs 50..98 become **dead data**: kept, as Ben asked, but read by nothing and tested by nothing [read] | one-line change. **"Keep the numbers in the game files" is met literally, but the kept numbers are unexercised, and their javadoc (`11,642,250`, *"the three anchors"* including `[60]` and `[98]`) goes stale** | as C1, and the table's anchor rows at 60 and 98 **cannot be asserted** any more |
| **C3. The cap as data** (a `config.yml` key, or a content file) | `levelFor` takes the cap as a parameter, or reads a holder | the server owner can change it without a build. **Costs:** `PlayerLevel` is a static pure class today, so every caller (§1.6) must be handed the cap, or a static mutable holder appears (CLAUDE.md invariant 3, *no static mutable singletons*, argues against the holder); a loader and a refusal for a value outside `1..99`; and, for a content file, a boot-line term (§5.3) | every C1 row, plus a loader test |

### 4A.3 The save trigger at the cap (applies to C1 and C2)

**[read]** `addLifetimeXp` writes to disk only when `levelFor(before) != levelFor(after)`. **If `levelFor`
stops at 50, that never fires again**, and all XP earned past 50 reaches disk only at quit, at shutdown, or
by accident through another write-through (§1.3). A crash then loses **everything banked since the last
quit**. Today that loss is bounded by one rung (at most 400,000 near 99). At a clamped 50 it is
**unbounded** [inferred]. Options: (i) compare the **uncapped curve level** for the save trigger only (saves
continue at each hidden rung to 99); (ii) accept it; (iii) add a periodic save. **Q-L17.**

### 4A.4 "XP to next" at the active cap

Today, at the cap, `xpToNextLevel` is **empty**, and the `To Next` line is **absent by design**
(`PlayerLevelLines`' class javadoc: *"any value in that column is a lie about a quantity"*) [read]. At an
active cap of 50 **[inferred]**:

- **(a) Absent, as today.** The honest reading: there is no next level.
- **(b) Show progress toward the hidden 51** (64,160 XP [executed]). That advertises a level that cannot be
  reached, which is the defect `PlayerLevelLines` names.
- **(c) A different line**, for example "banked past the cap: n XP". It is a true quantity, and it tells the
  player that the grind is not wasted when the cap rises.

**Q-L18.** The level screen (§4B) needs the same answer.

---

## §4B THE LEVEL SCREEN: THE CLICKABLE HEAD

### 4B.1 The head today [read]

- **Slot:** `NexusMenuLayout.STATS_SLOT = 13`, the only item in row 2 (*"THE HEADER. The player head, and
  nothing else"*, `STATS_SLOT`'s javadoc, *THE HUB IS BANDS WITH MEANINGS*).
- **Item:** built in `NexusMenu.render`: `PLAYER_HEAD` with the viewer's skin, named by
  `NexusStatsLore.name()` ("Your Stats", GOLD). Its lore is `NexusStatsLore.lore(...)`: the progression block,
  then the average Gear Score, then the stat lines, then `AccessorySheet` and `FragmentSheet`.
- **Click:** nothing. `NexusMenu.onClick` falls through (*"Falling through rather than branching on
  STATS_SLOT deliberately: a no-op branch would read as a wired button whose body someone forgot to write"*).
  The `NexusMenu` constructor javadoc threads services *"so that the slice which makes the head clickable can
  REPAINT it"* (quoted by `PLAN-nexus-polish.md` §2.2).

### 4B.2 What "unlocks" can list today, and where each threshold lives [read]

| unlock | level | where | data or constant? |
|---|---|---|---|
| Crafting (hub) | 3 | `NexusStationGate.Station.CRAFTING(3, …)`, read by `Station.unlockLevel()` | **Java constant**, an enum argument. Package-private in `P/menu` |
| Anvil (hub) | 7 | `Station.ANVIL` | constant |
| Enchanting (hub) | 10 | `Station.ENCHANTING` | constant |
| Grindstone (hub) | 13 | `Station.GRINDSTONE` | constant |
| Vault hub shortcut | 20 | `Station.VAULT(VaultPageGate.HUB_SHORTCUT_LEVEL, …)`; `C/vault/VaultPageGate.HUB_SHORTCUT_LEVEL = 20` | **constant**, public in `core` |
| Vault pages 2 to 7 | 25, 30, 35, 40, 45, 50 | `VaultPageGate.UNLOCK_LEVELS = {FREE, 25, 30, 35, 40, 45, 50}` (private), read through the public `unlockLevel(page)` | **constant** |
| the level bonuses | every level | §2.5's function | derived (and data, if Q-L2 picks YAML) |

**Note [read]:** every hub station's world block works at every level (`NexusStationGate`'s javadoc, *THE
GATE IS ON THE HUB ONLY*). So "unlocks" means **the hub shortcut**, and the list must say so, or it
misleads the way `NexusStationGate.lockedLore`'s three sentences were written to avoid.

**One list, used twice.** The screen must read the same `Station` values and `VaultPageGate.unlockLevel`
that the gates read, never a copy (`PlayerLevelLines`' javadoc: *"two lists checked against each other, where
one list used twice removes the defect class"*). Because `Station` is package-private in `P/menu`, the
screen either lives in `P/menu` or the station levels move to `core` **[inferred]**.

### 4B.3 A proposed layout: options, none picked (wording and look are Ben's)

54 slots, like the hub and the recipe browser. Bottom row per the house chrome **[read]**: **Back 48**
(`MenuIcons.back(Material.ARROW, "the Nexus")`), **Close 49** (`MenuIcons.close()`), filler
`MenuIcons.filler()`. Paging precedent: `RecipeBrowserLayout` puts **Prev at 45 and Next at 53**, and hides
them on the first and last page. **But its page indicator sits at 49, where every other screen puts Close**
(`RecipeBrowserLayout.PAGE_SLOT = 49`, `BACK_SLOT = 48`) [read]. So a level screen must choose between
the browser's footer and the house Close. That is Nexus-polish ground too (§4B.5).

| part | option A | option B | option C |
|---|---|---|---|
| **Header** (row 1) | one item at 4: current level, XP into level / to next (§4A.4), lifetime XP | the player's head again at 4, with the same lore as the hub head | three items: level, XP to next, current total bonuses |
| **The list** | rows 2 to 5 = **36 cells** → page 1 is L1–36 and page 2 is L37–50 | rows 1 to 5 = **45 cells**, header moved into the footer → L1–45, then L46–50 | **only levels that grant a milestone or an unlock** (§2.5, §4B.2), one page |
| **A cell** | material by state: reached / current / locked (for example LIME / YELLOW / GRAY panes). **The filler is BLACK and the empty quick-craft cell is LIGHT_GRAY, so those two must not be reused** (`MenuIcons.FILLER`, `EMPTY_SUGGESTION` [read]) | one material, with the state in the name colour (the `NexusStationGate` locked-name convention: keep the material, dim the name) | an XP bottle with the stack size showing the level (stack sizes up to 64 cover 1–50) **[inferred; the item cap ruling is about minted custom items, not display stacks, UNVERIFIED as a reading]** |
| **A cell's lore** | "this level gives": +5 Max Health, +1 Damage, milestones, unlocks | the same, plus the **running total at that level** | totals only |

### 4B.4 How it fits the menu framework [read]

- A new `Menu` subclass (`P/menu/Menu`: `onClick`, `onClose`), holding **no input slots**. So the hub opens
  it the way it opens Settings: `adapters.scheduler().onEntity(viewer, () -> new LevelMenu(viewer, …,
  () -> new NexusMenu(…)).open())`, **hopping a tick with no explicit close** (`Menu.open`'s rule). The
  `Supplier<Menu>` is the breadcrumb for Back.
- **`MenuRouting` needs nothing**: it is a whitelist that cancels by default. A button-only screen's clicks
  never un-cancel.
- **Paging re-renders in place**, as `RecipeBrowserMenu` does.
- **Staleness:** painted once. A level-up while it is open shows stale until it is reopened (the same known
  staleness `NexusStatsLore` records for the hub).
- The `NexusMenu.onClick` fall-through comment (*"the stats head's click is still unbuilt"*) becomes false,
  so it is replaced by the real branch.

### 4B.5 Which plan owns it, and the overlap with `PLAN-nexus-polish.md`

`PLAN-nexus-polish.md` (scratchpad, 2026-09-29) **defers the screen to this plan**: its §4.2 S3 says the head
screen is *"Deferred to PLAN-level-bonuses.md"*, and its §5.7 lists the shared files [read]. **Owner: this
plan.** Shared files:

| file | this plan | Nexus polish |
|---|---|---|
| `P/menu/NexusMenu.java` | the STATS_SLOT branch in `onClick`, and the head's item in `render` | `render` (names, lore, the "stand out" options) and the ten Back suppliers (its X10) |
| `P/menu/NexusMenuLayout.java` | **none needed** (`STATS_SLOT` exists; the new screen has its own layout class) | its X4 |
| `P/menu/NexusStatsLore.java` | a "click to open" line; possibly moving the progression block onto the new screen | its S1/S2 (label style, class block) |
| `P/menu/MenuIcons.java` | only if new helpers are wanted (page arrows, level-cell icons) | its X3, X5, X8, X9 and SO1–SO5 helpers |
| `P/menu/MenuRouting.java` | none | its X2 (which click types press a button) |
| tests | `NexusStatsLoreTest`, a new layout test; the source scans that read `NexusMenu.java` (`ProgressionWiringSignatureTest`'s coupling row looks for `NexusStationGate.refusal(` and `viewer.sendMessage(`; the polish plan names `BuildScreenWiringSignatureTest`) | the same |

**The stack order is the seat's choice.** The polish plan proposes landing the shared `MenuIcons` helpers
first and building the level screen on them. The alternative is the level screen first, with the polish
restyling it afterwards. **Either way, whichever lands second rebases onto the other's `NexusMenu.onClick`
and `render`.**

---

## §5 STACKING ON `feat/melee-m1` (PR #168). THE BASE IS RULED

**[read]** `git diff 2d60e9a3 origin/feat/melee-m1 --stat`: **23 files, +1513 / −25**, merge base
`2d60e9a3`. #168 is the only open PR (`gh pr list`, 2026-09-28). Its gate reads **R0 PASS, MC rows NOT
READ**, *"NOT approved to merge"*.

### 5.1 Shared files

| file | #168 changes | this plan touches it? |
|---|---|---|
| `P/health/PlayerHealthSystem.java` | no | **yes** (the level source at the merge points) |
| `C/combat/stat/*`, `C/accessory/AccessoryStat.java` | no | no new stat needed (§2.5) |
| `C/progression/PlayerLevel.java`, `XpGrant.java` | no | **yes** (the cap, §4A) |
| `P/menu/NexusMenu.java`, `NexusStatsLore.java`, a new level screen | no | **yes** (§4B), **shared with Nexus polish** (§4B.5) |
| `P/profile/ProfileService.java`, `P/listener/RpgListeners.java` | no | yes (the save trigger §4A.3, feedback §6 F1) |
| `P/health/ClassDamageModifierItems.java` | no | **yes under E2** |
| **`C/combat/Caster.java`, `C/ability/effect/EffectApplier.java`** | **YES** (`Caster` gains `String source`; `EffectApplier` +14) | **only under E3**, and then a **certain conflict** |
| **`P/adapter/AdapterContext.java`, `P/RpgPlugin.java`** | **YES** | only if level data rides the adapters, or a YAML loader is added (Q-L2) |
| `P/command/RpgCommand.java` | the `mobtrace` literal | only if `playerxp`'s reply changes (F6) |

**Under E1 or E2 and a Java bonus table, this plan overlaps #168 in no file [inferred].**

### 5.2 Schema version bumps

**None.** Everything here is derived. `PlayerProfile.CURRENT_SCHEMA_VERSION` stays at 5 [read]. Only
removing the dead fields (Q-L10) would bump it.

### 5.3 Content counts asserted in gates and tests

- **`GATE-melee-cell.md` R0c** predicts `Loaded 11 abilities, …, 3 recipes` [read at `e305ab3a`]. **A YAML
  bonus table (Q-L2) adds a term** to `RpgPlugin`'s `"Loaded "` line, so each stacked slice's R0c must
  quote the line with every term below it (the coordinator's Q-L9 note).
- **Tests #168 edits that count content** (`PoolLoaderTest`, `AspectLoaderTest`,
  `ScorchContentInvariantTest`): **untouched here.**
- **`FragmentWiringSignatureTest`**: the level map goes after the fragments' map (§2.2).
- **`ProgressionWiringSignatureTest`**: the handler window of 8 lines (§1.2), and `NexusMenu`'s
  `refusal`/`sendMessage` needles (§4B.5).
- **The cap moves five test classes** (§1.6, §4A.2), none of them touched by #168.

### 5.4 Process hazards

- **Squash-merge:** when #168 squashes, a stacked branch is rebased onto the squash commit
  (`git rebase --onto <squash> e305ab3a <branch>`), and `./scripts/check-absorbed.sh` has **four**
  outcomes. **Read merge state from origin.**
- **#168 is not approved.** A stacked slice cannot merge before it, and a stacked boot also runs #168's
  content.
- **Q6's ruling exists only on #168's branch** (§6 F11).
- **Nexus polish shares `NexusMenu`/`NexusStatsLore`** (§4B.5). That is a second stacking decision.

---

## §6 FINDINGS: RECORDED, NOT FIXED

- **F1 (player-facing): a level-up is silent.** `onPlayerExpChange` throws away the result. A one-orb
  multi-level jump must report every level crossed. Feedback must keep `profiles.addLifetimeXp(` within 8
  lines of the handler's declaration (`ProgressionWiringSignatureTest`), or live in a method it calls.
  Threading: the handler runs on the player's thread (Folia **UNVERIFIED**). Anything scheduled goes
  through `Scheduler.onEntity`.
- **F2: two "levels" on screen, and the visible one is the wrong one** (the vanilla number is the enchant
  wallet).
- **F3 (stale text): `PlayerLevelListener` does not exist.** It is named in `PlayerLevel`'s class and
  `plus` javadocs, and in `PlayerProfile.withLifetimeXp`'s (*"the one writer"*, *"Nothing else writes this
  field"*, and both halves are false).
- **F4: `/rpg stats` has no progression block**, and would have no level-bonus block either.
- **F5: dead `level`/`experience` fields** in every profile.
- **F6: `/rpg playerxp`'s reply** prints raw longs, and *"Added N level(s)"* at the cap.
- **F7: `levelFor` per reconcile pass** breaks its *"not per tick"* premise.
- **F8: before this plan, nothing unlocked above level 50.** Under the cap of 50, **vault page 7 unlocks
  exactly at the cap**, and the level screen's list ends there.
- **F9: an orb picked up during the join-load window is not counted** (documented as accepted).
- **F10: the heart bar changes only at L2, L22 and L42** under the ruled HP [executed, §2.5]. Every other
  level-up's +5 is invisible on the bar.
- **F11: Q6 is ruled only on `feat/melee-m1`.**
- **F12: `StatSourceBound`'s *"4 + 4 = 8 sources"* comment** goes stale with a level source. The negative
  bound holds while level bonuses are positive-only.
- **F13 (new): the cap stops the disk-write trigger.** Past a clamped 50, lifetime XP reaches disk only at
  quit or shutdown (§4A.3).
- **F14 (new): a cap raise is silent**, and levels jump with no feedback (§4A.1).
- **F15 (new): health regen's display unit is per FIVE seconds, and its engine unit is per second.** Any
  number quoted to or by a player must say which (§2.5, Q-L12).
- **F16 (new): a stone cast and a weapon trigger are indistinguishable at the damage arms** (the same
  `resolve` → `commit` → `Caster.of`). "Weapons only" has to be decided at the source (§3.5).
- **F17 (new): under E1, the stat sheet's Damage line would advertise the level damage to a staff user who
  never receives it**, which is the NAME THE QUANTITY failure on a tooltip (§3.5).
- **F18 (new): the level's +5% crit reaches Actives** through the shared crit stat, while Q3 made Actives
  level-blind for DAMAGE (§3.4, Q-L16).
- **F19 (new): `RecipeBrowserLayout.PAGE_SLOT = 49`** is the slot every other screen uses for Close
  (§4B.3).
- **F20 (FUTURE THREAD, Ben 2026-09-29): "the ability damage pipeline."** Actives are gear-blind and
  level-blind **for now**. When it is tackled, E3's `Caster` flag (§3.5), Q6's O5 (`PLAN-melee-class.md`
  §5.5) and the crit question (Q-L16) meet in one place: which caster-side numbers an Active reads.

---

## §7 WHAT WAS NOT READ OR RUN

- The full list of vanilla orb sources: **UNVERIFIED**.
- Whether an unarmed punch would deal a flat attack bonus (`AccessoryStat`'s javadoc claims it): **not
  traced**.
- How a vanilla (non-plugin) bow or crossbow shot is priced: **UNVERIFIED**.
- Which damage paths pass `DefenseRule.BYPASSED`: **not enumerated**.
- Vanilla spawn-time health bonuses on zombies: **UNVERIFIED**.
- Whether `WeaponItems.heldWeaponId` is empty for the Ability Stone (E2's gate): **[inferred], not
  traced**.
- The cost of `levelFor` per pass: **not measured**. Folia threading: **UNVERIFIED**.
- No boot and no build of the repo. Code run: `core/src/main/java` compiled into the scratchpad, plus three
  scratch classes (`Lvl`, `Iso`, `Hearts`) calling it.

---

## §8 SLICES: A SKETCH FOR WHEN THE OPEN QUESTIONS ARE ANSWERED

**[inferred. The order the repo's habits suggest]** (CLAUDE.md: *the `core/` unit test before the `paper/`
wiring*; *keep changes small*). All of it is stacked on `feat/melee-m1` (ruled).

1. **The cap (core).** §4A's option, `PlayerLevelTest` / `PlayerLevelLinesTest` / `XpGrantTest` rows split
   into "curve to 99" and "cap in force", plus the save-trigger choice (Q-L17) in `ProfileService`.
2. **The bonus function (core).** `LevelBonus` with **`LevelBonusTest`, the whole L1..L50 grid, pinning
   max health at L1 = 0, L2 = 5, L49 = 240, L50 = 250**, and the other rows listed in §2.5. Floating-point
   expected values are **executed, never predicted**.
3. **The wiring (paper).** A fourth argument at the merge points for HP, regen, defense and crit; the damage
   entry point (Q-L13); a signature test beside `FragmentWiringSignatureTest`; a level block on the stat
   sheet and `/rpg stats` (F4).
4. **Feedback (paper).** F1, F3 and the cap-raise question (F14).
5. **The level screen (paper, §4B)**, in the order the seat picks against Nexus polish.

**Gate staging** would use `/rpg playerxp set <p> <n> levels` (it writes through, and `set` lands exactly on
the threshold, per `XpGrant`'s javadoc) and a bottle o' enchanting for a real level-up (the javadoc's cheap
legal instrument; `ProgressionWiringSignatureTest`'s class javadoc asks for one real-orb row). Rows at
**L1, L2, L9/L10, L21/L22, L49, L50**, and one **past** 50 (for example `set 800000 xp`) to show the cap
holding and lifetime XP still growing. Game mode is declared on every row (`verification.md`'s
CREATIVE-DIVERGENCE REGISTER).

---

## §10 QUESTIONS FOR BEN

1. **Q-L1: The shape.** **ANSWERED by Ben 2026-09-29**: flat per level for HP and damage, milestones for
   regen, defense and crit (§4).
2. **Q-L2: Where the numbers live.** A YAML table under `content/` (invariant 2) or Java like
   `PlayerLevel.XP_TO_NEXT`? It decides whether the boot line grows a term (§5.3). **OPEN.**
3. **Q-L3: Which stats, and how much.** **ANSWERED by Ben 2026-09-29**: max health, damage, health regen,
   defense, crit chance, with the numbers in §0. Open inside it: Q-L11, Q-L12, Q-L14, Q-L15.
   - *Sub-question (Ben's Q1): "does level 1 already get +5 HP?"* **ANSWERED by Ben 2026-09-29**: no.
     Every level-up gives +5, and only 49 → 50 gives +10, for a total of +250. (The intermediate reading was
     "ANSWERED, AMBIGUOUS: seat asked Ben (a)/(b)". It is resolved by the fifth and sixth lines of §0.2.)
   - *Sub-question (Ben's Q2): "+1 crit chance = +1%?"* **ANSWERED by Ben 2026-09-29**: yes, +5% at 50
     (+0.05 in the engine's unit).
4. **Q-L4: Heal on level-up?** A max-HP increase gives headroom, not health (§3.3). **OPEN.**
5. **Q-L5: The M1 ratio.** At level 50 the player has 350 HP against mobs priced at 5x vanilla (§3.6's
   numbers). Is that the reward, or does mob GS take it back later? **OPEN.**
6. **Q-L6: Gear score.** Should a level bonus count toward the player's GS average or mob GS? Today neither
   happens. **OPEN.**
7. **Q-L7: Actives.** **ANSWERED by Ben 2026-09-29** (his Q3): *"Only weapons, we'll tackle the ability
   damage pipeline later"*. Level damage does not reach Actives. The crit side is Q-L16.
8. **Q-L8: Feedback.** What a level-up shows, and whether the level appears on the action bar or in
   `/rpg stats`. **OPEN.**
9. **Q-L9: Stacking.** The core of this work shares no file with #168 (§5.1). The base is ruled: stacked on
   `feat/melee-m1`. Where in the stack does this thread sit relative to Nexus polish and the legacy port?
   (The legacy port moves the weapons count in the boot `Loaded ...` line; a YAML shape here adds a column to it, per §5.3. Each slice's R0c predicts against the slices below it.)
10. **Q-L10: The dead `level` and `experience` fields (F5).** Leave them, or remove them in a schema bump?
    **OPEN.**
11. **Q-L11 (new): Damage at level 1.** The seat ASSUMED +0 at L1, so **+49 at 50** (the HP convention).
    Or is it +1 at L1, so **+50**? **OPEN.**
12. **Q-L12 (new): Health regen's unit.** "+1" per step = +1 HP per **second** (9.2 HP/s at 50, shown
    `46.00/5s`), or +1 on the **shown per-5-seconds** number (2.0 HP/s at 50, shown `10.00/5s`)? (§2.5,
    F15). **OPEN.**
13. **Q-L13 (new): Where "weapons only" enters.** **E1** (the attack stat: cheapest, but **every staff and
    every literal-damage weapon gets nothing**, and the sheet would advertise a number a staff never deals),
    **E2** (the class-damage slot gated on a held weapon: reaches every weapon, with a lag of up to 5 ticks
    after swapping to the stone), or **E3** (a `Caster` flag: exact, and conflicts with #168). (§3.5)
    **OPEN.**
14. **Q-L14 (new): Two readings to confirm.** "Every 5 levels starting at level 10" includes 10 (9 steps,
    not 8). And "+5 max health" is in the custom-HP unit gear uses (base 100), not in vanilla half-hearts.
    **OPEN (confirm).**
15. **Q-L15 (new): Above 50, when the cap rises.** Is the +10 at 50 a one-off (+5 per level after)? Do the
    regen, defense and crit steps continue? **OPEN.**
16. **Q-L16 (new): Crit and Actives.** The level's +5% crit will reach Actives through the shared crit stat
    unless something excludes it. Allowed, or excluded until the ability damage pipeline thread? (§3.4,
    F18) **OPEN.**
17. **Q-L17 (new): The save trigger past the cap** (§4A.3): uncapped-curve trigger, accept, or a periodic
    save? **OPEN.**
18. **Q-L18 (new): "XP to next" at 50** (§4A.4): absent, progress to the hidden 51, or "banked past the
    cap"? **OPEN.**
19. **Q-L19 (new): The cap's mechanism** (§4A.2): C1 (a second constant), C2 (lower `MAX_LEVEL`, dead
    rungs), or C3 (data)? **OPEN.**
20. **Q-L20 (new): The level screen's look** (§4B.3): header, list and cell options A/B/C, and whether the
    screen replaces some of the head's lore. **OPEN.**

---

## REPORT CHECKLIST (for the seat)

- Every citation is a method, class or section, never a line number.
- Ben's words in §0.1 and §0.2 are inserted byte for byte from the seat's two files, by a script, not
  retyped.
- The curve values, bonus totals, heart counts, crit and regen sums, Defense reductions, mob numbers and
  hits-to-kill are **[executed]** against the real `core` classes at `2d60e9a3`. The max-health cap, the XP
  event, and the Zombie and Enderman attributes are **[jar]**.
- The damage row is marked ASSUMED (+49). The regen unit is open.
- #168 read at `e305ab3a`. Master at `2d60e9a3`.
