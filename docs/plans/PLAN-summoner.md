# PLAN — THE SUMMONER CLASS, FIRST SLICE: THE BONE WHISTLE AND ITS WOLF

**Status: PR 1 and PR 2 BUILT, NOT BOOTED.** The seat's brief of 2026-11-24 (Ben's words, verbatim, are in it and in
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
