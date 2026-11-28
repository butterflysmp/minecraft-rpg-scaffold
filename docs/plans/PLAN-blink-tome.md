# PLAN — THE BLINK TOME: A MAGE CLASS ACCESSORY WITH AN AUTOMATIC ESCAPE (2026-11-26)

**Status: BUILT, NOT BOOTED.** The gate is `docs/gates/GATE-blink-tome.md`. Stacked on the Healing Tome stack (#326).

## BEN'S WORDS, VERBATIM, 2026-11-26

- "Let's do the Blink tome. Same situation with the healing tome. Reworking it to a mage class accessory. It's ability:
  Blink, when an enemy gets to close, teleports you to somewhere safe nearby with 8 seconds of invisibility, losing all
  aggression. Attacking cancels the invisibility"
- "1. Any mob within 5 blocks radius 2. Any hostile mob that's targeting you. Other players don't count until we do a pvp
  pass. 3. Somewhere within 16 blocks that is safe (no mobs within 5 blocks) 4. 60s cooldown 5. Casting a spell does, a
  summon whistle does not 6. That is correct. Bosses are NOT exempt 7. Summons should keep their target until it is
  defeated or their target switches. This isn't related to the Blink tome. It has more to do with summon target
  priorities. They generally shouldn't retreat to their caster unless they don't have anything else to do. 8. Yes"

**NOT STATED by Ben:** a mana cost; whether taking damage cancels the invisibility (both SEAT PLACEHOLDERS below).

**The old game (CreaperCrusher/Butterfly-SMP at cfde822), a reference only:** BLINK_TOME, a book with a right-click
forward teleport; end city treasure at 0.06. Nothing of its mechanism carries over.

## THE ITEM (BEN'S), AS COMMITTED — `content/accessories/blink_tome.yml`

`blink_tome`, "Blink Tome", rare, `book`, `slot: class`, `class: mage`, `type: tome`, **no modifiers**, one behaviour:
`type: blink`, `name: "Blink"`, `trigger_radius: 5`, `range: 16`, `clear_radius: 5`, `invisibility_ticks: 160`,
`cooldown_ticks: 1200`, and the feedback keys `particle: poof`, `particle_count: 12`, `sound: entity.enderman.teleport`
(SEAT PLACEHOLDER; both keys READ in the 26.1.2 jars: `Particle.POOF` in paper-api, `entity.enderman.teleport` in
`SoundEvents`). Loot only: no source is built.

## THE STACK

| PR branch | what |
|---|---|
| `feat/blink-tome` | core: `AccessoryBehaviour.Blink`, the three pure decisions (`Blinks`), the loader, the content, the tooltip; this plan |
| `feat/blink-runs` | the clock, the blink, the state (`BlinkedPlayers`), the targeting seam, the attack sink, every hook |
| `feat/blink-selftest` | the three clears, `probe_invisibility`, `mob_target`'s tick, the landing enumerated over the pad |
| `feat/blast-fungus-gate` (#321) | moved up again, its four commits unchanged; `GATE-blink-tome.md` added |

## K1 TO K16, EACH WITH ITS READS

**K1, ONE BEHAVIOUR IN YAML.** `AccessoryBehaviour.Blink(name, triggerRadius, range, clearRadius, invisibilityTicks,
cooldownTicks, particle, particleCount, sound)`, the second member of the sealed interface. `AccessoryLoader.parseBlink`
reads `type: blink`; each malformed field refuses the file by name (`AccessoryLoaderTest.aBlinkBlockLoads…`: a missing
range, a zero trigger radius, a fractional tick count, a negative cooldown, an unknown particle, a non-number, an unknown
type). No accessory id in Java (`BlinkTomeGuardsTest.noMainJavaFileNamesATomesId`, with a control). **When it runs:**
exactly while the accessory CONTRIBUTES: `PlayerHealthSystem`'s reconcile pass hands its ONE reading's active behaviours
to `BlinkTomes.reconcile` (the pulse's H3 shape), and every check asks `accessoryContributionsFor` again. **Not in
creative or spectator** (the trigger is held: `Blinks.Held.EXEMPT`).

**K2, THE TRIGGER, a pure decision (`Blinks.trigger`, `BlinksTest.theTrigger_table`).** Fires when a living entity is
of the MOB side (`CombatSides.of`; a player never, a summon never), its current target is HIM, it is alive, in his world,
and its feet are within `trigger_radius` of his feet in three dimensions (5.0 exactly fires, 5.001 does not: CC'S CALL,
inclusive), and he is alive, not exempt, not blinked, off cooldown and not inside the nowhere-safe wait. The nearest such
mob is the trigger (the first found on a tie). **"Targeting" is READ as `Mob#getTarget`** (what the engine, `SummonGoal`
and the selftest's `probe_target` read): **READ, `CraftMob#getTarget` → `Mob.getTarget()` → `asValidTarget(this.target)`**
(null for a creative or spectator player). **WHERE A QUARRY IS KEPT THAT THIS READING DOES NOT SHOW, READ:** nine mobs
override `getTarget()` to return the BRAIN's `ATTACK_TARGET` (`getTargetFromBrain`): the axolotl, frog, zoglin, breeze,
creaking, hoglin, piglin and piglin brute (`AbstractPiglin`), and the warden. For those the reading IS the brain's, so the
trigger sees them. **NOT SEEN by this reading:** the warden's anger list (`AngerManagement`) before it becomes its target;
a neutral mob's persistent anger (`persistentAngerTarget`: enderman, iron golem, polar bear, wolf, bee, zombified piglin)
before a goal turns it into a target; and **the ender dragon**, whose phases hold their own target (K8). **The check's
period: 5 ticks (CC'S CALL; `Blinks.CHECK_PERIOD_TICKS`), on his own thread** (a `RepeatingTask` on his
`EntityTaskTarget`, the pulse's shape), so the check runs within 5 ticks of the condition becoming true. Alternative: 10,
the brief's bound, half the work.

**K3, THE DESTINATION, a pure decision (`Blinks.search` and `Blinks.choose`, `BlinksTest`).** The bound (CC'S CALL):
every integer column (dx, dz) with dx² + dz² ≤ 16² round his block, **797 columns** (EXECUTED; π × 256 ≈ 804 was the
guess, and it was wrong), and in each **five heights**, two below his feet's block to two above: **3,985 spots**. A spot is
SAFE when: its chunk is LOADED (asked first; an unloaded chunk is never read, so never loaded:
`BlinksTest.anUnloadedChunkIsNeverRead`), its column is inside the world border, the block under it is a SAFE floor, its
two body blocks are CLEAR, its feet (the block's centre on its floor) are within 16 of his feet, and no hostile mob's feet
are within 5 of its feet (inclusive). **The choice is uniform** over the safe spots by a random source passed in (seeded
in the tests, `SplittableRandom` on the server). **"Harmful to stand on", READ in the 26.1.2 server jar:** every block
class that overrides `stepOn`, `entityInside` or `fallOn` was listed (36), and of those the ones that hurt, freeze or
carry off whoever stands on or in them are refused: as a FLOOR, magma (`MagmaBlock.stepOn`), a campfire and a soul
campfire, cactus, a lava cauldron (`entityInside`), pointed dripstone (`fallOn`); as a BODY block, fire and soul fire
(`BaseFireBlock`), a sweet berry bush, a wither rose, powder snow, a nether portal, an end portal, an end gateway; fluid
of any kind, a waterlogged block included. CC'S CALL, refused as well: a sculk shrieker floor (its `stepOn` can summon a
warden), a big dripleaf floor (it tips and drops him), a cobweb body block (it holds him). Not refused: pressure plates,
tripwire, crops, farmland, honey, slime, hay, beds, turtle eggs, lily pads (no harm). **The vertical reach is TWO, not
the Knell's three (CC'S CALL):** at three a landing three below his feet has its head two below his feet, so standing on
a one-block floor (a bridge, the selftest's sky pad) he could be sent UNDER it, out of sight; at two that spot's head is
the floor he stands on. The alternative, three, reaches one more step of a hillside. **A passive animal does not block a
spot** (SEAT PLACEHOLDER, OPEN FOR BEN; his words are "no mobs within 5 blocks"): a mob blocks when it is of the MOB side
AND hostile by kind (`org.bukkit.entity.Enemy`), one of our own mobs (`mob_id`), or targeting anyone now. **No line of
sight is needed** (SEAT PLACEHOLDER). **NO SAFE SPOT:** nothing happens, the cooldown is NOT spent, the line `BLINK
NONE-SAFE` is written, and no search runs again for 20 ticks. **He arrives** standing on the floor, facing as he was, fall
distance cleared before the teleport and again when it lands. **The teleport is `Player#teleportAsync`, the engine's
existing way for a player** (the selftest's RETURN, `FightFixtures`). **READ:** `CraftEntity.teleportAsync` calls
`ServerLevel.loadChunksForMoveAsync`, whose callback runs `teleport0` through `MinecraftServer.scheduleOnMain`, so the
server moves him on its own thread; on Folia it is the cross-region teleport the API exists for (INFERRED: Folia's
contract, not read). The landing's chunk is already loaded.

**K4, THE BLINKED STATE.** `core/accessory/BlinkedPlayers`, one state per player with its start and end ticks and what it
did to his invisibility; one instance per server in `AdapterContext` (as `Orbs`). It begins at the blink and ends at the
FIRST of: 160 ticks (the expiry is scheduled on his thread and ends only that blink, `endIfStarted`), his attack (K5),
his death (`RpgListeners.onPlayerDeath`, a fight's cancelled death included), his quit (`RpgListeners.onQuit`, while he
is valid), and the selftest's clears (K9). **It is the engine's own state, never a reading of the potion effect.**
**CC'S CALL: unequipping the tome or changing class mid-blink does NOT end it**; it runs to its own end (the end's
arithmetic uses the state's own span, so it still removes only what it applied). Alternative: end it with reason
`inactive`.

**K5, "ATTACKING CANCELS".** Decided at two places, never per weapon id, through one sink (`AttackSink`, late-bound in
`AdapterContext`): **the fire seam**, `WeaponFire.attempt`'s `CastResult.Success` branch, which is reached only past the
cooldown and mana gates (READ: `weaponService.fire` charges both; every refusal returns before it), on any weapon whose
class is not SUMMONER; a summon cast and a direct order return before that branch (READ), so the whistle never ends it.
**The melee seam**, `RpgListeners.onPlayerMeleeAttack`, past the summon refusal and the broken weapon's cancel: a vanilla
`ENTITY_ATTACK` from him that was not cancelled is a hit that LANDED; a swing at air raises no event. **NOT an attack:** a
summoner weapon's triggers, a swing that hits nothing, his summon's hits, a Healing Tome pulse, **taking damage (SEAT
PLACEHOLDER, OPEN FOR BEN)**. **CC'S CALL, NOT an attack either: an Ability Stone's cast** (`StoneCaster`; not a weapon
trigger, and the brief scopes K5 to weapon triggers). Alternative: tell the sink from the stone's cast too ("Casting a
spell does" may mean it). **A quiver's reload is not an attack:** READ, `WeaponDefinition` refuses a weapon that
declares both a quiver and a `left_click` trigger (the comment in `boltor.yml`'s trigger block says so), so a reload is
never a trigger and never reaches the Success branch.

**K6, INVISIBILITY.** **READ, the 26.1.2 client jar (unobfuscated):** `LivingEntityRenderer.isBodyVisible` is false for an
invisible entity; `HumanoidArmorLayer`, `ItemInHandLayer`, `CustomHeadLayer`, `ArrowLayer`, `WingsLayer` and
`ParrotOnShoulderLayer` never test invisibility, so **his ARMOUR, HELD ITEMS, A HEAD ITEM, STUCK ARROWS, AN ELYTRA and A
SHOULDER PARROT STILL DRAW**; `CapeLayer` and `Deadmau5EarsLayer` do test it (hidden); **his NAME is drawn only when he
is not `isInvisibleTo(viewer)`** (hidden from other players; INFERRED: a teammate with see-friendly-invisibles sees him
translucent, vanilla's team rule, not read). **What stays visible is OPEN FOR BEN.** Applied with **no particles, not
ambient, its icon shown (CC'S CALL)**. **How a second application combines:** rather than trust vanilla's merge
(`MobEffectInstance.update` keeps a lower-amplifier instance hidden under a stronger one, INFERRED), the blink decides
itself (`Blinks.invisibilityAtBlink` / `invisibilityAtEnd`, `BlinksTest`): **he already has one lasting the whole state,
or forever: it is never touched and ours is not applied; a shorter one is taken off, remembered, and ours put on; at the
end ours is removed ONLY if it is still ours (amplifier 0, not infinite, no longer than ours had left), and what remains
of his shorter one is given back.** A longer or stronger one given meanwhile, or milk, is left alone.

**K7, LOSING AGGRESSION.** **At the blink:** every `Mob` within 64 blocks of where he stood (CC'S CALL) whose target is
him forgets him, each on its own thread: `setTarget(null)`; **and READ, the anger the target reading does not show:** a
wolf's (`CraftWolf.setAngry(false)` calls `Wolf.stopBeingAngry`), a zombified piglin's and a bee's anger timer
(`setAnger(0)` → `setTimeToRemainAngry(0)`), a warden's anger at him (`Warden#clearAnger`), a piglin's `ANGRY_AT` memory
when it names him (READ: `CraftMemoryMapper.toNms(null)` is null; INFERRED: `Brain.setMemory(type, null)` erases).
**COULD NOT CLEAR, listed:** the brain's `ATTACK_TARGET` of the nine brain-driven mobs (K2's list; the API exposes no key
for it and NMS is banned): such a mob still names him and writes `BLINK KEPT … reason=brain`; the persistent anger of an
enderman, an iron golem and a polar bear (no API); a warden's anger at anyone else. **While blinked no mob may take
him:** (1) **the engine's one targeting seam, `MobTargets.set`**: READ, `CraftMob#setTarget` passes a NULL reason and
`Mob.setTarget(target, null)` skips `callEntityTargetLivingEvent`, so OUR two `setTarget` calls (the aggro-on-hit in
`BukkitCombatant.applyNow` and the selftest's `mob_target`) never raised the event; both now go through the seam, and
`BlinkTomesWiringTest` forbids any other non-null `setTarget` in main code; (2) **vanilla's target event**,
`RpgListeners.onHiddenTarget` at HIGHEST: READ, every goal that targets through `Mob.setTarget(target, reason)` and a
brain's `StartAttacking` raise it; (3) **our own mob abilities' victim choice**: the Knell's beam (`MobBeamer.eligible`),
the Phalanx's choice and re-acquire (`MobShooter.chooseable`), the Skreel's pounce (`MobPouncer.choose`) skip a hidden
player. (The Phage's fuse reads the vanilla target only, `PhageFuser`, so the event's refusal covers it.) **NEITHER SEAM
COVERS:** the ender dragon (K8); a mob whose brain still holds him (above).
**HIDDEN, NOT INVULNERABLE:** a Phalanx shot already flying at him keeps him (its in-flight check is the old `eligible`),
and area damage, burns and blasts still reach him. When the state ends mobs may notice him again by their normal rules.

**K8, BOSSES.** **READ, the 26.1.2 server jar:** the dragon fight spawns a vanilla `EnderDragon` (`FightInstance`, phase
`CIRCLING`) and never sets its target; vanilla's dragon chooses a player in `DragonHoldingPatternPhase`,
`DragonLandingApproachPhase` and `DragonSittingScanningPhase` through `ServerLevel.getNearestPlayer(TargetingConditions…)`
and hands it to its own `DragonStrafePlayerPhase.setTarget` / `DragonChargePlayerPhase.setTarget`: **neither
`Mob.setTarget` nor the target event**. So K7's seams do not cover it, and making it forget him needs changes inside the
fight engine (and in vanilla's phases, which only NMS reaches). **Not built. OWED.** What it does today to a blinked
player: **it strafes and charges him as before** (INFERRED: `TargetingConditions.forCombat()` scales its range by his
visibility, so an invisible player with no armour is chosen only from much closer; not read). Our own dragon mobs (Knell,
Phalanx, Skreel) ARE covered (K7 (3)).

**K9, THE COOLDOWN.** 1200 ticks from a SUCCESSFUL blink, per player, in server ticks, **in `CooldownTracker`, the engine's
one cooldown store** (READ: keyed by player and id, a tick supplier; key `accessory:blink`). Re-equipping, a class change
and death do not reset it (nothing clears it but the two below). **CC'S CALL: it is not persisted across a restart, and
it is cleared on quit** (READ: `RpgListeners.onQuit` calls `cooldowns.clear(playerId)` for every cooldown). Alternative:
keep it across a quit. **The selftest's `clear_state` clears it (with his others), and the row-start CLEAN and every run
end reset ONE key** (`CooldownTracker.reset`, new); each also ends the blinked state and its invisibility (`reason=cleared`).
**NO MANA is read or spent (SEAT PLACEHOLDER, OPEN FOR BEN).**

**K10, HIS SUMMONS. The blink does NOTHING to a summon** (no teleport, no change of target, no recall; no summon code is
changed). **REPORT against Ben's sentence**, every line READ:
- **(a) What makes a summon drop or switch a target.** `SummonGuard.decide` (core/summon/SummonGuard.java:51-78), run
  every tick by `SummonGoal.tick` (paper/summon/SummonGoal.java:102), ranks three tiers: the DIRECTED mob (56-60), a mob
  that DAMAGED the owner within `damaged_window_ticks` 100 (61-62), a mob TARGETING the owner within `trigger_radius` 30
  (63-64); within a tier it keeps its current target while that target still qualifies, else the nearest (`keepOrNearest`,
  81-87); a higher tier pre-empts. It DROPS a target that is dead, gone, or beyond `chase_limit` 32 OF THE OWNER
  (`dropReason`, 94-98), and one that stops qualifying for its tier. No vanilla goal remains
  (SummonService.java:171, `removeAllGoals`; the one goal at 189).
- **(b) When the mob it fights stops targeting its owner:** the third tier reads `owner.equals(m.getTarget())`
  (SummonGoal.java:143). **A mob held only by that tier is dropped on the next tick** (it no longer qualifies), unless it
  damaged the owner in the last 100 ticks or was directed. **And from this stack:** a summon's hit credits its owner
  (SummonGoal.java:118-119), and the aggro-on-hit now goes through `MobTargets.set`, which REFUSES a blinked owner; before,
  each summon hit turned its victim back onto the owner and so kept it in the third tier. **So at a blink (which clears
  every target on him), his summon drops every target that is held only because it targeted him.**
- **(c) When its owner is suddenly up to 16 blocks away:** no teleport (only beyond `teleport_back` 48, SummonGuard.java:53,
  SummonGoal.java:104-107); with a target it keeps chasing it while the target is within 32 of the owner (98); with none
  and more than `guard_radius` 8 from him it walks back (SummonGoal.java:123-124, `needsReturn` SummonGuard.java:90-91).
- **(d) When it returns:** only when it has NO target (SummonGoal.java:114-127: a target, path to it; else beyond 8,
  path to him; else stop). Mid-fight only the 48-block teleport moves it to him.
- **Against Ben's words:** (d) matches "they generally shouldn't retreat to their caster unless they don't have anything
  else to do". **"Summons should keep their target until it is defeated or their target switches" DIFFERS today:** a
  summon also drops a target at 32 from the owner (a), and drops a third-tier target the moment that mob stops targeting
  the owner (b), which a blink now causes on purpose. Whether "their target switches" means the summon's or the mob's is
  for the seat. **Nothing changed; the seat rules a fix.**

**K11, FEEDBACK (SEAT PLACEHOLDER, OPEN FOR BEN):** `poof` x12 and `entity.enderman.teleport` (volume 1, pitch 1, to the
world) where he leaves and where he arrives; nothing for the cooldown. Keys in content.

**K12, TRACE LINES** (under `/rpg mobtrace`, the selftest's `trace`), pinned by `BlinkTomesWiringTest.theTraceLines`:
`BLINK wearer=<name> tick=<t> trigger=<mob type> <uuid> dist=<d> from=<x y z> to=<x y z> safe=<n> forgot=<n>`;
`BLINK NONE-SAFE wearer= tick= trigger= searched=<n>` (the spots tried); `BLINK REFUSED-TARGET mob=<type> <uuid> tick=`
(once per mob per blink: a goal re-asks every few ticks); `BLINK END wearer= tick= reason=<expired|cast|melee|death|quit>`
and the selftest's `cleared`; `BLINK START … tick= period=5` and `BLINK STOP … reason=<inactive|ended|quit>`; and
`BLINK KEPT mob= … reason=brain` and `BLINK feedback …` (CC'S CALL, two lines the brief did not list).

**K13, THE TOOLTIP AND THE SHEET, from the YAML's own numbers (SEAT PLACEHOLDER wording).** Predicted whole before it was
generated (`AccessoryLoreLinesTest.theBlinksLines_fromItsOwnNumbers`, a control with other numbers):

```
Blink                                                         (gold)
When a hostile mob targeting you comes within 5 blocks,       (gray)
you teleport to safety and turn invisible for 8s.             (gray)
Attacking ends the invisibility.                              (gray)
Cooldown: 60s                                                 (gray)
Mage class accessory                                          (gray)

Rare Accessory                                                (the rare colour)
```

The sheet: **`Blink Tome: Blink`** when active, **`Blink Tome: inactive (requires Mage)`** otherwise. **Golden:** predicted
as 10 insertions and 1 deletion with the 9 lines written out; read 10 / 1 with the 9 lines byte-identical; 153 → 154
renderings.

**K14, THE BOOK'S GUARDS AND A4** key on `accessory_id`'s presence (`AccessoryItems.isAccessory`), never an id:
`BlinkTomeGuardsTest` shows the three book guards ask `isAccessory`, no main Java names either tome, and the tome is a
`book` (the material H12 was READ for). A4's refusals are the same arms (read by command in the gate's BT1). **Nothing
added.**

**K15, LOOT ONLY.** No source is built. **CARRIED beside the others in the world-loot list:** the Blink Tome (the old
game's end city entry is a reference). Until the loot pass it is obtainable only by `/rpg give blink_tome`.

**K16, RECOUNTS WITH CONTROLS.** Accessories 7 → 8 (`ls content/accessories/*.yml | wc -l`; `AccessoryLoaderTest`,
`UniqueSlotContentTest`); unique-slot gear 68 → 69; golden renderings 153 → 154; gates 54 → 55, scenarios 519 → 530 (585
keys, control 573 + 12).

## THE TRIGGER'S DECISION TABLE (`BlinksTest.theTrigger_table`; he is ready, at the origin)

| entity near him | fires? |
|---|---|
| a hostile mob targeting him at 3 | yes |
| at 5.0 exactly | yes (inclusive, CC'S CALL) |
| at 5.001 (and 5.001 straight up) | no |
| at 0, on his feet | yes |
| within 5, NOT targeting him | no |
| a player targeting him | no (Ben: not until a pvp pass) |
| a summon | no |
| dead | no |
| another world, same coordinates | no |

And his own state holds it, whatever is near (`theWearersState_holdsIt`): dead, creative or spectator, already blinked, 1
tick of cooldown left, inside the nowhere-safe wait. The nearest qualifying mob is the trigger; a tie keeps the first.

## THE DESTINATION'S DECISION TABLE (`BlinksTest.theSafeSpot_table`; the spot (4, 65, 0) on flat stone, his feet at y 65)

| the spot | safe? |
|---|---|
| stone floor, air above | yes |
| a harmful floor | no |
| no solid floor | no |
| solid, fluid or harmful at his feet | no |
| solid, fluid or harmful at his head | no |
| outside the world border | no |
| in a chunk not loaded (and never read) | no |
| a hostile 5.0 from it / 5.001 | no / yes |
| 16.0 from him / 16.97 | yes / no |
| two up a ledge / three | yes / no |
| two down a pit / three | yes / no |

And: listed x, then z, then y; a seed fixes the choice; over 30,000 draws each of three spots is chosen about a third of
the time.

## CC'S CALLS

1. The check's period is 5 ticks (alternative: 10).
2. Distances are feet to feet in three dimensions, and every radius is inclusive (alternative: strict).
3. The trigger is the nearest qualifying mob (alternative: the first found).
4. The search is every column within 16 and five heights; the vertical reach is TWO (alternative: three, the Knell's).
5. A mob blocks a landing when hostile by kind, one of ours, or targeting anyone (alternative: any MOB-side mob, which a
   cow would then do).
6. The forget pass reaches 64 blocks from where he stood (alternative: the server's whole world, which Folia forbids).
7. Unequipping or a class change mid-blink does not end the state.
8. Our invisibility: no particles, not ambient, its icon shown.
9. A refusal is logged once per mob per blink.
10. An Ability Stone's cast is not an attack (the brief scopes K5 to weapon triggers).
11. The cooldown is cleared on quit and not persisted (the store's own rule).
12. A Phalanx shot already flying at him keeps him (damage on its way).
13. Two trace lines the brief did not list: `BLINK KEPT` and `BLINK feedback`.
14. The feedback: `poof` x12 and `entity.enderman.teleport` at volume 1, pitch 1, to the world.

## PLACEHOLDERS, OPEN FOR BEN (SEAT PLACEHOLDER 2026-11-26)

1. No mana cost (Ben did not say).
2. Taking damage does NOT end the invisibility (Ben did not say).
3. A passive animal does not block a landing spot (his words: "no mobs within 5 blocks").
4. No line of sight to the landing is needed.
5. The tooltip's lines and the sheet's line (K13).
6. The feedback: a small puff (`poof` x12) and the enderman's teleport sound at both ends; nothing for the cooldown.
7. What stays visible while invisible (K6): armour, held items, a head item, stuck arrows, an elytra, a shoulder parrot;
   his name is hidden.
8. Whether an Ability Stone's cast should end it ("Casting a spell does"), **Smolder included** (the Fire Mage's
   ultimate on Q; the seat's E5, 2026-11-27: no stone cast ends it today, Smolder's neither, and nothing changes until
   Ben rules; `PLAN-smolder.md` section 3.4, K5).

## OWED, CARRIED

- **The dragon (K8):** a blinked player is still chosen by the vanilla dragon's phases. Owed to the fight engine.
- **Brain-held targets and persistent anger K7 cannot clear** (`BLINK KEPT`): owed if Ben wants them cleared (needs an
  API this version does not expose, or NMS, which is banned).
- **K10:** Ben's summon sentence against today's behaviour: the seat rules a separate fix.
- **Not read by the gate's boot:** a live mob losing interest, the vanilla event's refusal, his look while invisible, the
  feedback's look and sound, the dragon, a second player.
- **Loot sources** (K15): the loot pass.
