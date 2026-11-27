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
Unbreaking's and Kinetic Tremors' (conditional, T8). PR B adds Drumming Beat's own and is recounted there.

## PLACEHOLDERS — OPEN FOR BEN (SEAT PLACEHOLDER 2026-11-26)

- reach 3.0 and arc 120 (the swords');
- flavor: none;
- the skeleton drop's chance (the old game's 1 in 80 is a reference).
