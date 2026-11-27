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
