# PLAN — the Bone Club and its guaranteed enchant, Drumming Beat (2026-11-26)

The seat's brief of 2026-11-26, via Ben. Ben's words, verbatim:

> "Let's bring the Bone Club in."
>
> "1. 44 damage, 0.8s 2. Drumming beat, every consecutive hit adds 3/4/5 critical chance. Same conditions 3. No doubled
> hit anymore 4. No doubled hit anymore 5. Yes guaranteed enchant 6. Melee, Kinetic, common, outside the tiers 7. Yes 8.
> No sweep"
>
> "1. Yes it is 2. No there is no bonus ceiling, landing a crit doesn't reset it"

("0.8s": attack speed 0.8. "Same conditions": the old game's three resets. "Yes it is": Drumming Beat IS the guaranteed
enchant, and the bare club is plain.) Ben's numbers REPLACE the old game's.

**The old game, RE-READ here** (CreaperCrusher/Butterfly-SMP at `cfde8222ec2a`, by `gh api …/contents`):
- `SwordData`: `BONE_CLUB` → `COMMON`, base damage `70`, name `"Bone Club"`, in `REBALANCED_CUSTOM_SWORDS`;
  `ItemFactory.createBoneClub()` → `Material.BONE`. **Confirmed.**
- `MeleeWeaponListener.onBoneClubHit` (Drumming Beat), on the PRIMARY target only, main hand, not broken:
  - `getAttackCooldown() < 1.0f` → counter 0, last hit 0, `BLOCK_BONE_BLOCK_BREAK` (volume 1.0, pitch 0.8), and the hit
    does not count. **Confirmed.**
  - `now - lastHit > 5000` ms → counter 0 (STRICTLY more than 5 s). **Confirmed.**
  - the counter + 1 reaching the threshold doubled the hit and reset it: **the threshold was 4, or 3 at sword level
    30 and above** (the seat's "every 4th" is the level-below-30 case). GONE (Ben).
  - otherwise `BLOCK_NOTE_BLOCK_BASEDRUM`, volume 0.8, pitch `0.7 + count x 0.2` (count after the increment).
- `onBoneClubSwapOff`: a `PlayerItemHeldEvent` away from the club with a count above 0 → counter 0 and the bone-break
  sound. **The old game reset ONLY on a slot change**; the seat's M5c (a swap, a drop, the item replaced, the enchant
  ceasing) is wider, and is what is built.
- `MobLootListener.onSkeletonDeath`: a `Skeleton` killed by a PLAYER, `1 in bone-club-drop-chance` (config default 80).
  **Confirmed.**
- Attack speed 0.8 and no sweep: **NOT re-read** (the seat's reading; Ben's ruling is the source either way).

## THE STACK (all on #320, `feat/cluster-bombs`)

| PR | branch | what |
|---|---|---|
| A | `feat/bone-club` | the weapon (M1), the one minted-bone guard (M2), the loot carried (M11), its recounts (M12) |
| B | `feat/drumming-beat` | the enchant: `drumming_beat` behaviour (M3), the streak (M4, M5), the roll (M6), feedback (M7), the trace (M8), the line (M9), the pools (M10) |
| gate | `feat/blast-fungus-gate` (#321), MOVED UP onto B | `GATE-bone-club.md`, its scenario, the one typed command for the whole stack |

## M1 — THE WEAPON, BUILT AS THE BROKEN BOTTLE WAS (PR A)

`content/weapons/bone_club.yml`: `bone_club`, "Bone Club", `class: melee`, `rarity: common`, `element: kinetic`,
`material: bone`, `attack_damage: 44`, `attack_speed: 0.8`, no `sweep`, no `craft_result`, no `crit_chance`, the
melee left click of the swords (reach 3.0, arc 120: SEAT PLACEHOLDER). No weapon id in Java.

- **Damage scales with the gear score (K1's path, READ):** `GearScore.scaledDamage(44, score)`: 44 at 100, 88 at 200
  (`BoneClubContentTest`).
- **No sweep (K4's path, READ):** absent `sweep` is `SweepShare.NONE`, and `RpgListeners.onPlayerSweepAttack` cancels a
  sweep whose share does not sweep. Nothing built.
- **No tier (K5):** a weapon has no tier field; no `craft_result`, no recipe names it, no smithing line. The tooltip has
  no tier line (golden).
- **Durability, READ:** `Items` registers `bone` with `registerItem(String)` (javap, the pinned server jar, `Items.<clinit>`
  offset 14530: `ldc "bone"`, `invokestatic registerItem:(Ljava/lang/String;)Item`, `putstatic BONE`): bare properties, no
  `.durability()`. So it never wears or breaks, as the bottle. `Material.getMaxDurability()` returning 0 is INFERRED
  (as K6). **Unbreaking stays in its pool**, as the seat's (b) on K6 ruled for the bottle (carried).

The golden, predicted before regenerating as 15 insertions / 1 deletion, and regenerated as exactly that:

```
-- bone_club
   "Kinetic"  |  ""  |  "Melee Damage: 44"  |  "Attack Speed: 0.8"  |  ""  |  "Common Melee Weapon"
-- bone_club + sharpness 3
   ... "Melee Damage: 50.6" ...
=== 146 renderings ===          (was 144)
```

## M2 — A BONE'S VANILLA BEHAVIOUR, BLOCKED ON A MINTED BONE ONLY (PR A)

**READ, the whole jar.** Every class in `run/versions/26.1.2/paper-26.1.2.jar` (= `paper.version`
`26.1.2.build.74-stable`) whose constant pool names the field `BONE` (`grep -laP '\x00\x04BONE'` over the extracted
classes), and every data file naming `"minecraft:bone"`:

| class or file | what it does with a bone | blocked by |
|---|---|---|
| `Wolf.mobInteract` (offset 470: `getstatic Items.BONE`, then `consume(1)`, then `tryToTame`) | an untamed wolf is tamed by it (a chance), the bone consumed | the ONE guard below |
| `BegGoal` (offset 32) | a wolf begs at a player holding one | **not blocked: cosmetic** (CC'S CALL, as the zombie horse's tempt at a minted mushroom) |
| `data/minecraft/recipe/bone_meal.json` | crafts to 3 bone meal | already: `onPrepareCraft` / `onCrafterCraft` refuse any minted gear in the matrix |
| `Items`, `CreativeModeTabs`, the data generators, loot tables | register it, list it, DROP it | nothing to block |

**Nothing else names it:** no block class (so no composter, no cauldron, no block use), no dispenser behaviour, no fuel,
no brewing, no trade, no tag. A bone is a plain `Item` with no `use`, so a right click in the air or on a block does
nothing.

**How the wolf's click is stopped, READ.** `ServerGamePacketListenerImpl.handleInteract` builds a
`PlayerInteractAtEntityEvent` (offset 222), calls it, and at offset 502 `isCancelled()` → `return` before
`ServerPlayer.interactOn` (offset 518), which is what reaches `Wolf.mobInteract`. `PlayerInteractAtEntityEvent` declares
NO handler list of its own (javap, paper-api: no `getHandlerList`), so it shares `PlayerInteractEntityEvent`'s, and a
listener on the latter receives it.

**ONE GUARD, NOT TWO (the seat: "make ONE guard that covers a minted bone of either weapon").** PR 5's
`onSummonerWeaponEntityInteract` is REPLACED by `onMintedItemEntityInteract`, which cancels when
`MintedBoneUse.refuseEntityUse(minted, material, summonerClass)`: a minted bone (the Bone Club, the Bone Whistle) or, as
before, any minted summoner weapon. The table is `MintedBoneUseTest`; the wiring is
`SummonWiringTest.aSummonerWeaponTamesNothing` (re-pointed, and it asserts the old method is gone).
- **CC'S CALL:** the summoner clause is KEPT beside the bone clause. The alternative is the bone clause alone, which
  covers every summoner weapon shipped today (the whistle is the only one, READ: `grep -l "class: summoner"`).
- **`GATE-summoner-r0b.ps1` still names `onSummonerWeaponEntityInteract`.** That gate ran; its script is the record of
  that jar and is NOT edited. Run on a jar of this stack, that one line reads ABSENT, as it should.

## M3 — DRUMMING BEAT, A NEW SEALED BEHAVIOUR DECLARED IN YAML (PR B)

`EnchantBehaviour.DrummingBeat(weaponId, gapTicks, stepPercentByLevel, beatSound, resetSound)`, the file's
`drumming_beat:` block, `effect: drumming_beat` (`EnchantEffect.DRUMMING_BEAT`), in Wildfire's family: weapon-specific
(the block's weapon is the enchant's `weapon:`, which the loader must see; the definition refuses a mismatch), gate
MAIN_HAND_ONLY, no top-level curve, one step per level. Its pure decision is `core.enchant.DrumBeats.decide`, beside
`BehaviourHits`; the per-player store is `core.enchant.DrumStreaks`, held by the server's one `BehaviourHits` as
`drums()`, beside the shockwave streaks and the hit chains. Every exhaustive switch answers it: `BehaviourHits.onHit`
(acts at the roll, not after a hit), `BehaviourBlocks` (conditional: no block, T8), `EnchantDefinition` (gate, curve,
levels), `EnchantEffectLine` (M9).

`drumming_beat.yml`, as committed:

```yaml
display_name: "Drumming Beat"              # BEN
icon: note_block                           # SEAT PLACEHOLDER 2026-11-26
max_level: 3                               # BEN: three values
effect: drumming_beat
class: universal                           # the convention of every weapon-specific enchant
weapon: bone_club                          # W1: the Bone Club's slot 0
guaranteed: true                           # G1 (BEN: "Yes guaranteed enchant")
drumming_beat:
  gap_ticks: 100                           # BEN: "Same conditions" (the old game's 5 s); exactly 100 continues the streak
  step_percent_by_level: [3, 4, 5]         # BEN: "adds 3/4/5 critical chance", percent points per consecutive hit
  beat_sound: block.note_block.basedrum    # M7, SEAT PLACEHOLDER 2026-11-26: the old game's drum, pitch rising with n
  reset_sound: block.bone_block.break      # M7, SEAT PLACEHOLDER 2026-11-26: the old game's reset, on an uncharged hit
```

## M4, M5 — THE STREAK AND ITS RESETS (PR B)

**The decision table** (`DrummingBeatTest.theDecisionTable`; `n` is the streak the hit is ROLLED at, the bonus a fraction,
step 5 at III):

| streak before (n, last tick) | this hit's tick | charged | Drumming Beat active | rolled at n | bonus | result | streak after |
|---|---|---|---|---|---|---|---|
| none | 1000 | yes | III | 0 | 0 | `counted` | (1, 1000) |
| (1, 1000) | 1040 | yes | III | 1 | 0.05 | `counted` | (2, 1040) |
| (4, 1120) | 1160 | yes | III | 4 | 0.2 | `counted` | (5, 1160) |
| (2, 1050) | 1150 (exactly 100) | yes | III | 2 | 0.1 | `counted` | (3, 1150) |
| (2, 1050) | 1151 (101) | yes | III | **0** | 0 | `reset:gap` | (1, 1151): this hit is the first |
| (3, 1000) | 1010 | **no** | III | 3 | **0** | `reset:uncharged` | none: not counted |
| none | 1010 | no | III | 0 | 0 | `reset:uncharged` | none |
| (3, 1000) | 1010 | yes | **none** (bare, or another weapon) | 3 | 0 | `plain` | none |
| (1, 1000), (2, 1040) | +40 | yes | I | 1, 2 | 0.03, 0.06 | `counted` | n + 1 |
| (1, 1000), (2, 1040) | +40 | yes | II | 1, 2 | 0.04, 0.08 | `counted` | n + 1 |

- **A QUALIFYING hit** (M4): a primary melee hit through `onPlayerMeleeAttack` → `WeaponFire.landVanillaMelee` that
  LANDS (the damage seam reported it) on a victim whose side is `MOB` (`MobHostility.countsAsAMobHit`, B4's own test),
  with a weapon some Drumming Beat names. A non-mob victim neither counts nor resets. **CC'S CALL:** the same test B4
  uses for "a hit on a mob"; the alternative counts any living non-player victim the rider reaches.
- **"FULL CHARGE", READ:** the engine's charge term is `AttackCharge.scale(charge)`, where `charge` is
  `Player#getAttackCooldown()` stashed by `onPrePlayerAttack` before vanilla resets it (`MeleeHits.Swing.charge`).
  `getAttackCooldown()` clamps at 1.0 (`ChargeRule`, PLAN-mining F1). **Full is `charge >= AttackCharge.FULL_CHARGE`,
  i.e. exactly 1.0** (1.0f widens to exactly 1.0). `landVanillaMelee` now takes the RAW charge and computes the same
  scale inside, so one reading feeds both the damage term and the streak.
- **The gap** (M5b): from the last COUNTED hit's server tick (`Bukkit.getCurrentTick()`) to this hit's; `> 100` resets,
  `100` continues. **A gap reset restarts the streak AT this hit** (rolled at 0, counted as 1): CC'S CALL, as the old
  game did (it zeroed the counter, then counted the hit). The alternative counts nothing on that hit.
- **No ceiling** (Ben): `DrumBeats.bonus` is never capped; `Crit.chance` clamps the sum at 1 at the roll. EXECUTED: at III
  from 0.15 the 17th hit rolls at **0.9500000000000001**, the 18th at **exactly 1.0** (always crits: `roll < 1.0`, and
  `nextDouble` never draws 1.0), the 20th at 1.1 (`thereIsNoCeilingAndTheEighteenthHitReachesOne`).
- **A crit cannot reset it:** `decide` takes no crit (its five parameters are pinned).
- **M5c and the lifecycle, each forgets:** `PlayerItemHeldEvent`, `PlayerSwapHandItemsEvent`, `PlayerDropItemEvent`,
  `InventoryClickEvent`, `InventoryDragEvent` (MONITOR, ignoring cancelled ones), `PlayerDeathEvent`,
  `PlayerChangedWorldEvent`, `onQuit`, the selftest's `clear_state` (`drum=<n>` on its STAGE line) and row-start CLEAN
  (`drum=<n>` at its end). **And a hit with any weapon no Drumming Beat names forgets it** (a hand changed by a command,
  which raises none of those events). Drumming Beat ceasing to be active is read at the next hit (`plain`, forgotten).
  **CC'S CALL: ANY inventory click or drag resets**; the alternative reads each click for whether it touched the held slot.
- NOT resets: a swing that hits nothing (no damage event reaches the rider), a crit, another target, a summon's hit or
  any secondary damage (none reaches `landVanillaMelee`).

## M6 — THE BONUS ENTERS AT THE ROLL (PR B)

READ: the one place a melee hit's crit is rolled is `BukkitCombatant.snapshot` (`Crit.multiplier(critChance, critDamage,
critRoll)`, one draw per cast), called by `landVanillaMelee`. Built: `snapshot(entity, stats, keys, critBonus)`, which adds
the term to `stats.critChanceValue(…)` right before that roll; the three-argument form passes 0.0. The decision is made
before the snapshot and the streak kept only after the hit lands. **Not written to the stat:** the crit-chance stat is
the 5-tick reconcile's, so `/rpg stats` shows the standing chance, never the streak (**SEAT PLACEHOLDER, OPEN FOR BEN**).
This changes the shared snapshot by one term that is 0 for every other caller, which M6 rules.

## M7 — FEEDBACK (PR B; SEAT PLACEHOLDER, OPEN FOR BEN)

The old game's, RE-READ: on a counted hit (a gap reset's hit included) `beat_sound` at volume 0.8, pitch
`min(2.0, 0.7 + 0.2 x n after the hit)`: 0.9, 1.1, 1.3 … reaching the 2.0 limit at the 7th hit; on an uncharged hit that
LOST a streak, `reset_sound` at 1.0, pitch 0.8. Played to HIM only, on his thread. **No visual step.** CC'S CALL: no
reset sound when the streak was already 0 (nothing was lost); the old game played it on every uncharged hit. The sound
keys are content, not Java. **What only the boot can show:** that he hears them (a sound is the client's).

## M8 — THE TRACE LINE (PR B)

While trace is on, after every landed Bone Club hit on a mob (the bare club too):
`STREAK n=<n rolled at> step=<points> chance=<resolved chance + bonus, 3 decimals, unclamped> charged=<b>
result=<counted|reset:uncharged|reset:gap|plain>`. Pinned whole by `DrummingBeatTest.theTraceLine`. **Where, READ:**
`EffectApplier`'s `WeaponDamage` arm calls `trace(…)` (the PLAYERHIT line) synchronously inside `landBasicMelee`, before
it returns, and the STREAK line is logged just after it returns; so **each STREAK line follows its own hit's PLAYERHIT
line**, same thread, same tick. (`applyDamage` itself hops to the target's scheduler; the trace does not wait for it.)

## M9 — THE TOOLTIP (PR B)

CONDITIONAL (T8): `BehaviourBlocks` gives it no block, so the club's stat lines never move for it (T13,
`TrueTooltipsConsistencyTest`: its 6 cases are among the 582 unmoved). Its enchant line is `Drumming Beat <numeral>`; its
`EnchantEffectLine` reads the real step: **`+3% Crit Chance per consecutive hit`**, `+4%`, `+5%` (wording SEAT
PLACEHOLDER).

## M10 — THE POOLS (PR B), READ AND PINNED (`DrummingBeatContentTest`, 500 rolls each)

| pool | the Bone Club | an iron sword (the control) |
|---|---|---|
| slot 0, weapon | `drumming_beat`, guaranteed, locked at 0 | none |
| slot 1, unique | `kinetic_tremors` (the only `element: kinetic` enchant) | `kinetic_tremors` |
| normal | `sharpness`, `unbreaking` (Unbreaking on an undamageable weapon: carried, as the bottle's) | `sharpness`, `unbreaking` |

The Broken Bottle's are the model, and they are identical but for slot 0.

## M11 — LOOT, CARRIED (PR A)

Ben: skeleton drops, "Yes" to carrying it. **Built nothing.** `bone_club` is in no loot source and no dev source
(`BoneClubContentTest`, with controls). Carried in `PLAN-broken-bottle.md`'s world-loot list beside the bottle's and the
fungus's: **the Bone Club drops from SKELETONS**, the old game's 1 in 80 a reference and not a ruling, and a MOB-DROP
source is a new kind beside the chests. **Until that pass it is obtainable only by `/rpg give`.**

## M12 — RECOUNTS, EACH FROM ITS SOURCE (PR A)

| figure | command | at 305e1361 | now |
|---|---|---|---|
| weapons | `ls content/weapons/*.yml \| wc -l` | 26 | 27 |
| kinetic weapons | `grep -l '^element: kinetic' content/weapons/*.yml \| wc -l` | 16 | 17 |
| gear definitions (mint-on-craft line) | `BootCountLinesTest`, read off the boot | 60 | 61 |
| gear asked by the pool walks | `UniqueSlotContentTest` | 66 | 67 |
| true-tooltip cases | `TrueTooltipsConsistencyTest`'s own line | 1284 (594 / 672 / 18, 564 unmoved) | 1302 (612 / 672 / 18, 576 unmoved) |
| golden renderings | the golden's tail | 144 | 146 |

The 18 new tooltip cases: Sharpness, Unbreaking and Kinetic Tremors, three levels each, at two scores; the 12 unmoved are
Unbreaking's and Kinetic Tremors' (conditional, T8).

**PR B's recounts:**

| figure | command | at 921c953c | now |
|---|---|---|---|
| enchants | `ls content/enchants/*.yml \| wc -l` | 26 | 27 |
| weapon-specific | `grep -l '^weapon:' content/enchants/*.yml \| wc -l` | 13 (11 at fcfbefbb) | 14 |
| guaranteed | `grep -l '^guaranteed: true' content/enchants/*.yml \| wc -l` | 8 (7 at fcfbefbb) | 9 |
| true-tooltip cases | `TrueTooltipsConsistencyTest`'s line | 1302 (612 / 672 / 18, 576 unmoved) | 1308 (618 / 672 / 18, 582 unmoved) |
| golden renderings | the golden's tail | 146 | 146 (Drumming Beat moves no line, so the golden has no block for it) |

The controls at fcfbefbb (11 / 7) are the figures recorded when that tree was measured.

## PLACEHOLDERS — OPEN FOR BEN (SEAT PLACEHOLDER 2026-11-26)

- reach 3.0 and arc 120 (the swords');
- flavor: none;
- the skeleton drop's chance (the old game's 1 in 80 is a reference);
- Drumming Beat's icon (`note_block`);
- the line's wording, `+5% Crit Chance per consecutive hit`;
- the feedback (M7): the two sounds, their volumes and the pitch rule, and that there is no visual;
- the stats sheet shows the standing chance, not the streak (M6).

## CC'S CALLS

| call | chosen | the alternative |
|---|---|---|
| M2: the one entity guard | the bone clause AND PR 5's summoner clause, in one handler | the bone clause alone (covers every summoner weapon today) |
| M2: a wolf begging at a held club | not blocked (cosmetic) | cancel the beg (no event exists for it; it would take an AI change) |
| M4: "a hit on a mob" | B4's `countsAsAMobHit` (side `MOB`) | any living non-player the rider reaches |
| M5b: a gap reset's hit | counted as the first of a new streak (the old game's order) | counted as nothing |
| M5c: inventory screens | any click or drag by him resets | only a click that touched the held slot |
| M5c: a hand changed with no event | the next hit with a weapon no Drumming Beat names forgets | a per-tick watch of the hand |
| M7: the reset sound | only when a streak was lost (n > 0) | on every uncharged hit (the old game) |
| M7: where the sounds live | content (`beat_sound`, `reset_sound` keys) | constants in Java |
| M8: the trace's position | logged when the hit returns, so right after its PLAYERHIT (READ) | logged inside the damage seam |
