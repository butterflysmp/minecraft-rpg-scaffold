# PLAN — THE BLAST FUNGUS, FAST AND FUNGAL (GUARANTEED) AND CLUSTER BOMBS (2026-11-26)

From the design seat's brief via Ben, 2026-11-26 (F1 to F13). Built on #316 (`feat/class-damage-labels` `fcfbefbb`,
accepted, not booted). The stack, bottom up:

| branch | what | the rulings |
|---|---|---|
| `feat/fused-projectile` | the engine: a fused projectile, its store, its detonation, its PvP path | F1 to F6 |
| `feat/blast-fungus` | the weapon, its visuals, vanilla mushroom use blocked, this plan | F3, F11, F12, F13 |
| `feat/fast-and-fungal` | the cooldown tune and the guaranteed enchant | F7, F8 |
| `feat/cluster-bombs` | the behaviour enchant | F9, F10 |
| `feat/blast-fungus-gate` | `GATE-blast-fungus.md` and its scenario; rides TT20 to TT27 | the gate |

NOT BOOTED. Nothing here is taken from a deleted dev weapon.

## 1. BEN'S WORDS, VERBATIM, 2026-11-26

- *"Let's bring another new weapon from the old repo, blast fungus"*
- *"1. 55x3 damage, 4s cooldown, 3 block blast radius, 30 mana 2. Keep 3. Mage, Uncommon, Nature, outside the tiers 4.
  Correct 5. Loot only, bastion chests, and pigging barters. 6. Cluster Bombs, throws 1 big blast fungus that splits
  into 3 dealing 55/80/105 7. Red mushroom"*
- *"1. No its not guaranteed, I'll have the guaranteed one for you in a moment 2. Yes, the damage numbers in the
  description is the damage, the 3 that split from it deal the normal 55 damage"*
- *"1. Split out in a triangle shape 2. Yes it rises Side note, make the blast size of the big bomb 5 blocks Another side
  note: increase the cooldown to 7s and the guaranteed enchant is Fast and Fungal, reduces cooldown by 1/2/3s"*

("pigging barters" is piglin bartering. The LAST cooldown, 7 s, is the weapon's: 140 ticks.)

**The old game (the seat's reading of CreaperCrusher/Butterfly-SMP at `cfde822`; NOT re-read here, so carried as the
seat's):** BLAST_FUNGUS, "Blast Fungus", uncommon, a RED_MUSHROOM, ability "Fungal Barrage", three NETHER_WART_BLOCK items
at 0.7 along the look and +/-25 degrees of yaw, falling, a smoke and crimson-spore trail, each going off 40 ticks after the
throw or at once within 0.7 of a living entity not the shooter or a summon; radius 3; 35 damage; 35 mana; 8 s; bastion
chests 6 %, piglin barter 2 %. Ben's numbers REPLACE the old ones.

## 2. F1 — THE FUSED PROJECTILE

A declared property of a projectile cast, `fuse_ticks:` (`CastSpec.Projectile.fuseTicks`, a tail field; 0 = none, every
projectile before it). Not a new executor: `ProjectileFlight.launchFused` is the flight with three changes, and its
decisions are ONE pure table, `core.combat.FuseRules`, tested row by row (`FusedProjectileTest.theTableEveryRow`):

| phase | fuse left | touch | outcome |
|---|---|---|---|
| FLYING | yes | NOTHING | FLY_ON |
| FLYING | yes | MOB | DETONATE_TOUCH (at the point it met the mob) |
| FLYING | yes | BLOCK | REST (at the point it met the block) |
| RESTING | yes | (not traced) | STAY |
| FLYING | no | (not traced) | DETONATE_FUSE (where it is) |
| RESTING | no | (not traced) | DETONATE_FUSE (where it rests) |

- **The clock is the THROW.** Step 0 runs inline on the throw's frame; step n, n ticks later. A fuse of 40 goes off on the
  step 40 ticks after the throw. **A press throws one tick after it is pressed** (READ: `WeaponFire.attempt` stamps the
  press tick, then hops the cast through `scheduler().onRegion`, the global scheduler's `runDelayed(1)`), so a press at
  tick T goes off at **T + 41**. The seat's row text "40 ticks after the press" is predicted in the gate as press + 41,
  with its phase named.
- **The fuse is asked first**: a chunk cannot travel on the tick it goes off.
- **"A mob" is the sides table's** (`PvpPath.FUSED_BURST`): a body it spares, another player, is no touch, and the chunk
  flies on through him. **CC'S CALL**; the alternative (rest against him) leaves a chunk hanging at his box. A summon is
  never met: the trace passes through one (Q3, `castRay`).
- **Exactly once**: a chunk goes off only by being TAKEN out of the store (`FusedChunks.take`); a removal takes it first
  and it never goes off (`aRemovedChunkNeverGoesOff`).
- Refused at load, by name: a fused projectile that homes; one with a `body:` (an item body only); one inside a volley;
  `max_lifetime_ticks` beside `fuse_ticks` (its life IS the fuse).

## 3. F2 — AT REST, AND B6'S GUARANTEES

**READ: what the body did on reaching the ground before this:** `ProjectileFlight.step` resolves on any `castRay` hit, a
block included, and `resolve` REMOVES the body there. It never rested.

**What changed (`launchFused`):** a block hit is a REST. The body sits at the step's start; it is driven ONCE by the
remaining displacement (`driveMarker(rest - position)`, the witnessed velocity mechanism; a reposition does not reach
the client, `ProjectileFlight.resolve`'s javadoc) and then by a ZERO step every resting tick, so it stays. Its gravity
is already off (`spawnMarker`: `setGravity(false)`).

| hazard | what it does (READ unless marked) | matters? |
|---|---|---|
| a player picking it up | refused: `setPickupDelay(MAX)` (`configureMarker`, READ there) | no |
| a mob picking it up | refused: `canMobPickup` false (`configureMarker`) | no |
| a hopper | refused at `InventoryPickupItemEvent` by the marker tag (`RpgListeners.onHopperPickup`, READ) | no |
| merging with another stack | refused: the same pickup delay disables `isMergable` (READ there) | no |
| despawning early | the marker's despawn is armed at its expected life: `6000 - fuse - 60` ticks lived at spawn (`spawnMarker`, `ORPHAN_GRACE_TICKS` 60), so it despawns 100 ticks after the throw at the earliest, after any fuse of 40 | no |
| water, pistons, explosions | push item entities (`spawnMarker`'s javadoc, INFERRED there). The zero step each tick resets its velocity, so a push moves it at most one tick's worth at a time (INFERRED) | **the look only**: the detonation is at the FLIGHT'S point (the rest point the store holds), never the body's |

**Owned by the store until it goes off:** `FusedChunks`, one per server (`AdapterContext.fused`). Never saved (the marker
is `setPersistent(false)`). Removed, each with a `FUSED removed … cause=<cause>` line:

| when | cause | where |
|---|---|---|
| his quit | `quit` | `RpgListeners.onQuit` |
| his death | `death` | `onOrbOwnerDeath` |
| a world change | `world_change` | `onOrbOwnerChangedWorld` |
| server stop | `stop` | `RpgPlugin.onDisable` |
| the selftest's `clear_state` | `selftest` (`fused=N` on the STAGE line) | `SelfTestDrivers.clearState` |
| row-start CLEAN | `selftest` (`fused=N` on the CLEAN line) | `SelfTestRun` |
| run end | `selftest` (`fused=N` on RESTORED) | `SelfTestRun.end` |
| an orphan body (tagged `Keys.fused`, owned by no live chunk) | `orphan` | where it appears, a tick later, and at enable (`FusedRemovals`) |

The backstop behind all of it is the armed despawn above. `FusedRemovalWiringTest` reads each site off the source.

**CC'S CALL: a chunk that meets a WALL rests against it, in the air**, where a vanilla item would fall to the floor. The
alternative (fall on after a wall) needs a bounce rule no ruling covers.

## 4. F3 — THE DETONATION

`EffectApplier.detonateFused`, centred where the chunk is: each `visual` plays there; each `burst` goes through
`applyToNearby`, the same per-target loop and the same Damage arm every burst uses, so a mob's defences apply
(`DefenseRule.APPLIES`), the kill is the caster's (`applyDamage(…, caster.id(), …)`), and the gear score scales the
literal (`GearScore.scaledDamage` inside `hitBase`). **Nothing is broken or set alight (READ):** a visual step is a
`particle`, a `sound` or a `spiral` (`VisualLoader`); no main code calls `createExplosion`; `setFireTicks` is reached only
through the scorch status, and `nature.yml` declares no `applies_status`.

**The radius is a BOX (READ, existing burst behaviour, not new):** `PaperCombatWorld.combatantsNear` is
`World#getNearbyEntities(center, r, r, r)`, a cube of half-side r tested against each body's box. So a zombie (half-width
0.3) is inside a radius-3 blast when its box reaches within 3 on every axis: feet 3.25 along x is inside, 3.5 is not.

**CC'S CALL: a fused on_hit is visuals and bursts only, with at least one burst** (`FusedPayload`, by the ability's id).
The alternative, landing targeted effects on the mob a chunk touched, gives a touch a second hit the fuse does not.

## 5. F4 — WHO IT HURTS

The ONE sides table, `MobHostility.harms`, on a new `PvpPath.FUSED_BURST(false)`, under the enum's "OPEN FOR BEN (PvP)"
header: **SPARED**, as `TRAVEL_END_BURST`. Pinned in `MobHostilityTableTest.playerToPlayerIsPerPathAsToday`. The caster is
excluded twice: by id in `applyToEach`, and as a PLAYER on a spared path. A summon: the table's SUMMON cell refuses it.
`FusedProjectileTest.onlyMobsAreHurt` executes all four.

## 6. F5 AND F6

- **F5, the three chunks:** the flight's existing spread (`SpreadPattern`, READ: one body down the aim and a ring of
  `count - 1`), `count 3, angle 25, rotation 0`: the ring's two sit at the shooter's right and left, 25 degrees off the
  aim. **This is the Breacher's mechanism.** The old game's +/-25 was a yaw rotation about world up; at level pitch the two
  are the same, pitched the ring stays in the view plane. **CC'S CALL**; the alternative is the Dragon's Plume's
  `executeFan` (`Vec3.rotateAboutY`), which is reached only from paper's fan path and spends a round per offset. Each body
  is its own flight, fuse and chunk. **Each blast's damage counts as one primary hit (READ):** the Damage arm calls
  `world.primaryHit` for every damage it lands, a burst's included; only an Area's pulse passes `asNotAHit`. So a mob
  inside three blasts takes three hits, as three bursts give today. No new rule.
- **F6:** the press pays once, in `CastExecutor.commit`, before the spread expands (unchanged). A chunk at rest blocks
  nothing: the cooldown is the stamp's, and the store is never asked by a press.

## 7. THE WEAPON (`content/weapons/blast_fungus.yml`)

mage, uncommon, nature, `red_mushroom`; right click "Fungal Barrage": `cooldown_ticks 140`, mana 30, a projectile at
speed 0.7, gravity 0.04, `fuse_ticks 40`, item `nether_wart_block`, trail `fungus_trail`, spread 3 / 25 / 0; on_hit the
visual `fungus_blast` and a burst of radius 3 carrying `damage 55 nature`. NO `craft_result`, NO recipe (outside the
tiers, as the Broken Bottle's K5). The tooltip, bare (golden):

```
Nature

Fungal Barrage  Right-Click
Lobs three spore chunks that burst
after 2 seconds, or on touching a mob.
Magic Damage: 55 x 3
Cooldown: 7.0s | Mana Cost: 30

It grew in the crimson dark,
and it does not like being picked.

Uncommon Magic Weapon
```

No tier line (READ: there is none to print). "x 3" is `WeaponLoreLines.deliveredShots`, the spread's count (READ); the
55 is found inside the burst by `DamagePayload.of` (READ: it recurses into a Burst).

**THE ARC, CC'S CALL:** gravity 0.04 is vanilla's item gravity (READ: `ItemEntity.getDefaultGravity` returns `0.04d`).
The flight has no drag term, where a vanilla item keeps 0.98 of its motion a tick (not read), so the arc is a little
longer than the old game's.

## 8. F11 — VANILLA RED MUSHROOM USE, BLOCKED ON A MINTED ONE ONLY

READ, every use, in the pinned server jar (`run/versions/26.1.2/paper-26.1.2.jar`, MANIFEST `26.1.2.build.74-stable`,
equal to `paper.version`). The table is on `MintedMushroomUse`'s javadoc; in short:

| what it does | blocked by |
|---|---|
| places as a block | main hand: `onRightClick` cancels the interaction (READ: `ServerPlayerGameMode.useItemOn` returns before the item's use); either hand: `onWeaponPlace` cancels `BlockPlaceEvent` for any minted weapon |
| pots itself (`FlowerPotBlock.useItemOn`) | `onMintedMushroomUse`: `setUseInteractedBlock(DENY)` on `flower_pot`, either hand |
| composts on a click (`ComposterBlock.useItemOn`) | the same, on `composter` |
| composts from a hopper (`CompostItemEvent`, NOT cancellable, READ in paper-api) | `onMintedMushroomMove`: `InventoryMoveItemEvent` into a composter cancelled. That the hopper's move event fires on this path is INFERRED |
| a zombie horse eats it (tag `zombie_horse_food`) | `onMintedMushroomFeed`: `PlayerInteractEntityEvent` on a zombie horse cancelled |
| crafting (mushroom stew, rabbit stew, 15 suspicious stews) | already: `onPrepareCraft`, `onCrafterCraft` |

Not a use (READ): food, brewing, fuel, smelting, stonecutting, smithing, a dispenser (it drops the item whole), a
trade (the only trade GIVES red mushrooms), a mooshroom. **CC'S CALL: a zombie horse is tempted by one held** (its
`TemptGoal`): cosmetic, not blocked. **The off hand was the gap** (READ: `onRightClick` returns for the off hand), which is
why `onMintedMushroomUse` runs for both hands. **Our right click still casts at a block** because `onRightClick` has no
`ignoreCancelled` and runs whatever the LOWEST handler set.

## 9. F12 — LOOT, IN BEN'S WORDS, NOT BUILT

Ben: *"Loot only, bastion chests, and pigging barters."* No loot hook is built and no source names `blast_fungus`
(`BlastFungusContentTest`, with controls). **Until the world-loot pass it is obtainable only by `/rpg give`.** Carried
beside the Broken Bottle's entry in `PLAN-broken-bottle.md` (CARRIED). The old game's chances (6 % and 2 %, the seat's
reading) are NOT Ben's and are not carried as his.

## 10. F13 — THE RECOUNTS, EACH WITH ITS COMMAND

| figure | at `fcfbefbb` | after this PR | command |
|---|---|---|---|
| weapons | 25 | 26 | `ls paper/src/main/resources/content/weapons/*.yml \| wc -l` |
| gear definitions (mint-on-craft line) | 59 | 60 | `BootCountLinesTest` |
| gear in `UniqueSlotContentTest`'s walk | 65 | 66 | the test's own control |
| T13 cases | 1260 (558 unmoved) | 1272 (564 unmoved) | the test's `TRUE TOOLTIPS T13:` line: Attunement 3 + Unbreaking 3, x 2 scores = +12; Unbreaking's 6 move nothing |
| golden renderings | 140 | 142 | predicted +27 / -1 (13 bare, 13 with Attunement III, the tail); **read 27 1** and `=== 142 renderings ===` |

Enchants, weapon-specific and guaranteed counts move in the next two PRs (sections 11 and 12).

**The pools at this PR (READ, `UniqueSlotContentTest`):** slots 2-3 (normal) `attunement, unbreaking`, as every mage
staff's; slot 0 (weapon) empty until Fast and Fungal and Cluster Bombs ship; slot 1 (unique) a nature mage weapon's,
which no shipped unique enchant admits (the unique pool is Kinetic Tremors, kinetic, and Mana Burn, fire mage).

## 11. F7 AND F8 — FAST AND FUNGAL, THE COOLDOWN TUNE (`feat/fast-and-fungal`)

- **The quantity:** `EnchantBehaviour.CastTune.Quantity.COOLDOWN`, whole ticks of 1 or more, SUBTRACTED from the cast's
  `cooldown_ticks`. **ONE PLACE:** the `COOLDOWN` arm of `CastTunes.apply`. The press's stamp reads it through
  `AbilityService.stampedCooldownTicks` (extracted from `resolve`, which now calls it: one function, not two copies), and
  the tooltip reads it through `CastTunes.tune` in `WeaponLore` (its Cooldown line is `ability.cooldownTicks()` of the
  tuned cast). A cut that leaves 0 or less throws, naming the ability; the loader builds every level at load
  (`EnchantLoader.castTune`, READ), so such an enchant is refused, by name (`FastAndFungalContentTest`, through the real
  loader, with a 1-tick control). A cooldown tune needs a cast with a cooldown.
- **At the press, by the weapon in hand (B2):** `WeaponFire.attempt` reads the held weapon's active behaviours ONCE
  (`pressBehaviours`, the list it already read for the granted triggers) and hands it to every charge
  (`WeaponService.fire(…, pressBehaviours)` → `AbilityService.fireTrigger(…, behaviours)`). The Success carries the
  UNTUNED cast; the executor tunes it at the commit as before. A cooldown already running keeps its stamped length.
- **The floor is unchanged:** `CastSpec.minimumCooldownTicks` is still asked of the UNTUNED cast (a volley's wind-up or
  shot tune moves no floor today; no ruling covers that change). READ: no shipped weapon's floor exceeds its cooldown
  (T13's new cooldown check reports no mismatch on any shipped weapon).
- **Fast and Fungal** (`content/enchants/fast_and_fungal.yml`): `weapon: blast_fungus`, `guaranteed: true`, `cast_tune`
  `quantity: cooldown`, `value_by_level: [20, 40, 60]` (Ben's 1 / 2 / 3 s). 140 → 120 / 100 / 80, all on the 4-tick grid.
  G1: over 500 seeded rolls it is the first candidate in slot 0 of every fresh Blast Fungus, at level 0 (locked), and in
  no other weapon's pool.
- **F8, the tooltip:** the Cooldown line shows the TRUE cooldown: `7.0s` bare, `6.0s` / `5.0s` / `4.0s` (golden: the
  Fast and Fungal III rendering reads `Cooldown: 4.0s | Mana Cost: 30`). **T13 now checks every ability block's Cooldown
  line against `AbilityService.stampedCooldownTicks`** (the stamp's own function, never the renderer), and counts a case
  as "the engine moves nothing" only when the stamped cooldowns are unchanged too (its first run counted Fast and Fungal
  as moving nothing). Fast and Fungal's own enchant line stays name and numeral (`EnchantLore`, untouched). The enchant
  table's effect line reads `-1s cooldown` / `-2s` / `-3s` (SEAT PLACEHOLDER wording, in Ben's unit).

**Recounts (this PR):** enchants 24 → 25 (`ls paper/src/main/resources/content/enchants/*.yml | wc -l`);
weapon-specific 11 → 12 (`grep -lE '^weapon:' …/enchants/*.yml | wc -l`); guaranteed 7 → 8
(`grep -lE '^guaranteed: true' …`). **Corrected while building:** this plan's first draft (and the brief's report notes)
gave 13 and 8 at the base; that read 18 grep LINES (`weapon:` and `guaranteed:` together) as file counts. The file
counts are 11 and 7 at `fcfbefbb`. T13 1272 → 1278 (Fast and Fungal 3 levels x 2 scores; none unmoved, 564). Golden
predicted +14 / -1 (`-- blast_fungus + fast_and_fungal 3`, 12 lines, the tail), **read 14 1**, `=== 143 renderings ===`.

## 12. F9 AND F10 — CLUSTER BOMBS (`feat/cluster-bombs`)

- **A new sealed behaviour type**, `EnchantBehaviour.ClusterBombs` (`effect: cluster_bombs`, block `cluster_bombs:`):
  `damage_by_level [55, 80, 105]` (BEN), `element nature`, `radius 5` (BEN), `item red_mushroom_block`, `split_count 3`
  (BEN), `split_distance 2.5`, `split_fuse_ticks 20`, `split_lift 0.25`. `weapon: blast_fungus`, NOT guaranteed (BEN). The
  loader refuses it, by name, on a weapon whose right click is not a fused projectile (`clusterWeapon`, with a control).
- **The press (`CastExecutor.launchCluster`):** chosen by `BehaviourCasts.clusterBombs` (the tooltip asks the same), at
  the top of `launch`, below the commit, so the press pays once (F6). ONE big chunk straight down the aim on the WEAPON's
  own flight (speed, gravity, fuse 40, trail, read at run time) and the same fuse rules (F1, F2); its body the enchant's
  item. **It goes off** through `detonateFused` with `BehaviourCasts.clusterBig`: the weapon's own on_hit, each burst's
  radius 5 and its damage the level's figure, priced by the same Damage arm (the gear score scales it, as Shatter Caps'
  stone and Airburst's bursts).
- **The split (`splitCluster`), at the same moment:** `core.combat.ClusterSplit`. The heading is the press's aim, made
  horizontal (straight up or down: forward from his right). The three directions are 0, 120 and 240 degrees about world
  up from it, the first ON the heading. Each chunk is thrown `direction x s + (0, 0.25, 0)` under the weapon's gravity,
  `s` found by stepping the flight's own arc so that on level ground it comes back to its start height 2.5 out (EXECUTED:
  `ClusterBombsTest` reads each rest point within 1e-9 of the predicted triangle point). Each has its own fuse of 20 from
  the split and goes off as the weapon's own chunk (its on_hit: 55, radius 3).
  - **CC'S CALL, the price of a split chunk:** the press's frozen caster (the item's score and its damage enchants AT THE
    PRESS), as every hit a press makes is priced. Ben's "Yes it rises" holds: at score 200 the big and the small both
    double (`theScoreScalesTheBigAndTheSmall`: 160 and 110 at level II). The alternative, re-reading the item at the
    split, reads the player's inventory off his thread.
  - **CC'S CALL, the throw:** an arc that lands 2.5 out, so a player sees three chunks thrown; the alternative places them
    at rest at once. On uneven ground each rests where its arc meets a block.
  - **A split born inside a block (READ):** `VoxelShape.clip` returns a hit AT the trace's first sample
    (`from + 0.001 x step`) when that point is inside the shape. So its first step meets the block, the fuse table says
    REST, and it rests at the centre and goes off on its fuse (`aSplitBornInsideABlockRestsAtTheCentre`). No new code.
  - **CC'S CALL, no scale:** an item body has none; the big chunk is told apart by its item (`red_mushroom_block`).
- **F10, the tooltip:** `BehaviourBlocks`' one exhaustive switch gains the case: a `Magic Damage: <big>` row (no count)
  and a `Magic Damage: <normal> x <split count>` row, both from the same resolvers; the prose is the enchant's authored
  `ability_description:`, which states "three" as Airburst's states its own count (no enchant changes it, so no token).
  T13's engine side gains the branch (`launchCluster`, then `splitCluster`'s chunks).
  **The tooltip at Cluster Bombs II (golden's form at III):**

  ```
  Nature

  Fungal Barrage  Right-Click
  Lobs one great spore chunk that
  bursts, then splits into three more.
  Magic Damage: 80
  Magic Damage: 55 x 3
  Cooldown: 7.0s | Mana Cost: 30
  ...
  ```
- **ONLY ONE IS ACTIVE (the trade-off):** Fast and Fungal and Cluster Bombs are both slot-0 candidates; a slot has one
  active candidate (`EnchantSlot.activeIndex`), so with Cluster Bombs active the cooldown is the bare 7.0 s, as on every
  weapon with two (the Living Lantern's Wildfire and Airburst Flames; the Lapis Staff's two). **The pools (READ,
  `CandidateCascade.count`):** a pool of one left after the guaranteed candidate always yields 1, so EVERY fresh Blast
  Fungus offers slot 0 exactly `[fast_and_fungal, cluster_bombs]`, in that order (500 of 500 seeded rolls,
  `ClusterBombsContentTest`).
- **FOUND BY A MUTATION, FIXED IN THE TEST:** MUTCBROWS (the split row's count read from the weapon's spread) stayed GREEN
  on the first run: shipped, the split count and the spread count are both 3, so no row could tell them apart (the
  verification rule: two equal quantities cannot detect a transposition). The row test now stages a split of 4 against a
  spread of 3, and the mutation reddens it. **T13 is blind on this axis for the same reason**, at shipped values; the
  core row is its guard.

**Recounts:** enchants 25 → 26; weapon-specific 12 → 13; guaranteed 8 (unchanged). T13 1278 → 1284 (3 levels x 2,
none unmoved, 564). Golden predicted +15 / -1 (`-- blast_fungus + cluster_bombs 3`, 13 lines, the tail), **read 15 1**,
`=== 144 renderings ===`.

## OPEN FOR BEN (SEAT PLACEHOLDER 2026-11-26 unless dated otherwise)

1. The fan: one down the aim, two at 25 degrees (the old game's), in the view plane.
2. The throw: speed 0.7, gravity 0.04 (READ, vanilla's item gravity), no drag.
3. The touch: the chunk's traced segment meeting a body's box (the flight's exact trace), where the old game used 0.7
   from the item.
4. The body: `nether_wart_block`.
5. The trail (`fungus_trail`: crimson spores and smoke), the throw (`fungus_cast`: a witch's throw over a fungus breaking),
   the blast (`fungus_blast`: spores, an explosion flash, the explode sound at 0.7).
6. The ability's name, "Fungal Barrage" (the old game's).
7. The description: *"Lobs {shots_word} spore chunks that burst / after 2 seconds, or on touching a mob."*
8. The flavor: *"It grew in the crimson dark, / and it does not like being picked."*
9. CC'S CALLS (sections 2, 3, 4, 6, 7, 8): another player in the path is no touch; a chunk rests against a wall; visuals
   and bursts only; the spread, not the yaw fan; no drag; the zombie horse's temptation.
10. The world loot (section 9).
 11. Fast and Fungal's icon (`red_mushroom`) and its effect-line wording (`-1s cooldown`).
 12. Cluster Bombs: the 2.5 blocks, the 20-tick split fuse, the triangle's orientation (one point along the heading), the
     split throw's lift (0.25), the big chunk's body (`red_mushroom_block`, no scale), its prose and effect-line wording,
     its icon, and its sounds (it uses the weapon's own `fungus_blast`).
