# GATE — THE KNELL BEAM (feat/knell-beam), ITS MECHANISMS, THE OWED FIXES, AND THE OWED READINGS

**Status: NOT RUN.** Written 2026-11-21 on `feat/knell-beam` (PR 3), stacked on `feat/knell-mechanisms` (PR 2), stacked
on `feat/knell-owed-fixes` (PR 1), on master `141308e1` (the squash of #227), before any boot. **This file carries the
ONE R0 and the ONE run sheet for the three.**

**Status, 2026-11-22: RUN ONCE, at `61b617ef`. R0 AS PREDICTED; FIVE READINGS NOT AS PREDICTED (KB0's floor water, KB1's
name colours, KB2's after-hit probe, KB7's last mana line, and HF1R2 sampled the wrong weapon, so it is UNREAD).**
Every reading is in *READINGS, 2026-11-22* at the end of this file. No prediction above was edited.

**Seat approval 2026-11-22: code and gate diffed at 61b617ef, booted at 61b617ef, readings at 911de62f. Merge on Ben's
words.** Ben's words, 2026-11-22, verbatim: "go ahead with the merge".

**NO BOOT until the seat has diffed the code AND this gate from origin, and Ben says "ready" in his own words**; then
only CC boots, by `.claude/rules/booting.md`. **NO MERGE without Ben's own words.**

**Game mode:** SURVIVAL, every row. **Every row is AUTO** (`/rpg selftest knell-beam`); **Ben types three lines**, and
his answer to the one question when the selftest asks it. **Time:** the scenario's setup sets night (18000) and
`spawn_mobs false`; the run's restore puts both back. **Every mob is a frozen NoAI zombie; no fight world.** The setup
also kills the natural hostiles within 64 blocks by named type, and stages his level at 1, **which lowers his health
for the run and leaves it low after** (*THE SEAT'S FOLLOW-UP*, below).

**Ben's words, verbatim (2026-11-21, spelling as typed),** recorded in `PLAN-weapon-tiers.md` 1.7:
- "slight detour in that the next weapon isn't apart of our current progress tree focus. Let's bring the Knell Beam
  from the old repo into the new one"
- "1. Slight rework, I want the windup time to match the Knell mob in our repo and the cooldown to be double the wind
  up time. Also I want it to require more accuracy. The old beam was very forgiving and because of that, very over
  powered. I still want it to have decent coverage because of stuff like lag and desync but it shouldn't be a "Kill
  everything in that direction" weapon / 2. Void Weapon / 3. Nothing right now, It will be a dragon drop later / 4.
  ignore the crystals for now / 5. yes"
- On the selftests, honoured here: "these self tests have lost a lot of their automatic components since the handoff"
  and "i'm not typing the lines in chat just read them".

## WHAT THE THREE PRs DO

**PR 1, `feat/knell-owed-fixes` (the owed fixes):**
- `/rpg firerate` writes every line it shows to the server log too, as **`FIRERATE player=<name> <line>`**
  (`RpgCommand.say`). HF1 went unread because the lines reached his chat only.
- **`probe_mana`**, a selftest verb: **`MANA current=<c> max=<m>`** from the pool casts spend from.
- The seat's HF1 ruling recorded in `NEXT.md` and `standing-decisions.md`; the new rule **A DAMAGE PREDICTION NAMES
  EVERY TERM OF THE SUM** in `.claude/rules/verification.md`, with its pointer row in `CLAUDE.md`. 0 words removed.

**PR 2, `feat/knell-mechanisms` (rulings 1 to 7; no content; every option defaults off):**

| option | where | what it does |
|---|---|---|
| **`pierce: true`** | a ray cast | the walk goes on past a body to the range or the first block; each body struck ONCE (struck ids ride across chunk-column hops as values); each takes the same `on_hit`, nearest first |
| **`tolerance: <blocks>`** | a ray cast | a body is struck when the line meets its box grown by this much: the Knell mob's `BeamLine.meets`, reused. A margin-only hit needs a clear sight line from the line to the body |
| **`locked_aim: true`** | a volley | the origin and direction are captured at the cast; every shot fires down that line |
| **`warning: { beam: <visual>, flash_at: [ticks] }`** | a volley | a beam visual drawn along the locked line on those windup ticks. Refused by name without `locked_aim`, on a non-ray, or with a tick outside the windup |

The names follow the schema's own: `beam` is a ray's drawn line, `flash_at` and `tolerance` are the Knell mob's keys.
A ray with neither `pierce` nor `tolerance` takes the old path unchanged (`castRay`); every shipped ray is byte-identical.

**PR 3, `feat/knell-beam` (the weapon):** `content/weapons/knell_beam.yml` (a 24-tick locked volley of one piercing ray,
range 40, tolerance 0.3), four visuals, its golden block, `KnellBeamContentTest`. **The first EPIC item**; it joins NO
loot pool and has NO recipe. Every "no content is epic" site carries a dated note.

## THE SEAT PLACEHOLDERS, FOR BEN (each marked `SEAT PLACEHOLDER 2026-11-21` in the file)

| placeholder | value | why |
|---|---|---|
| tolerance | **0.3 blocks** | Ben wants more accuracy than the old 1.0 and gave no number |
| damage, mana, range | **63, 13, 40** | the old weapon's |
| rarity, material | **epic, echo_shard** | the old weapon's |
| id, name, ability | **`knell_beam`, "Knell Beam", "Knell's Toll"** | the old weapon's names |
| description | "Locks a line where you look," / "then tolls a beam down it 1.2s later." | the old tooltip, reworded to state the windup |
| warning | **ONE flash, at the click** (`flash_at: [0]`) | as Ben ruled for the Knell mob on 2026-11-17 |
| visuals | the mob's warning dust (95,35,165) and beam dust (40,230,235); the mob's telegraph sounds at the click; the mob's fire sounds and the old per-target bell where it lands | the Knell mob's where it has them, the old weapon's otherwise |
| piercing | **every body the line crosses** | the old weapon's; a cap is Ben's to add |
| **OPEN FOR BEN** | there is **no sound AT THE FIRE, AT THE STAFF**: a volley has no hook that presents a visual when its shot fires, so the mob's fire sounds play where the beam lands | adding the hook is Java; named, not built |

## WHAT WAS READ, EACH MARKED

**The seat's a) to d):**
- **a) READ.** `content/mobs/knell.yml`: `beam.telegraph_ticks: 24`, `flash_at: [0]`, `range: 40`, `tolerance: 1.0`,
  `damage: 65`.
- **b) READ.** `CastSpec.Volley` with `shots 1` and a windup is a telegraphed single cast, and `CastExecutor.volley`
  re-reads the caster's live aim (`world.aimOf`) before every shot: the aim was NOT locked. (Its "fresh price" is each
  shot's Caster stats; mana is spent once, at the click, in `AbilityService`.)
- **c) READ.** `CastExecutor.stepRay` stops at the first body, and its comment says piercing needs a set of already-hit
  ids threaded through the calls.
- **d) READ.** No player cast drew a line along its path before it fired: a ray's `beam` is drawn as it walks.

**The rulings:**
- **How today's ray decides a hit (READ, `PaperCombatWorld.castRay`):** one `World#rayTrace` over the segment,
  `FluidCollisionMode.NEVER`, passable blocks ignored, `RAY_SIZE 0.0` (each body's exact bounding box), filter
  `LiveTargets.except(caster)`; the nearer of the first block and the first body wins. Tolerance 0 in the new sweep is
  that exact box, on the mob's `BeamLine.meets`.
- **Can tolerance reach through a wall? Not along the line, and not round a wall either.** The sweep clips each segment
  at its first block (`rayTraceBlocks`, the same configuration), and a body struck only in its tolerance margin is
  refused unless `lineOfSightClear(the struck point, the body's sight point)` (`CastExecutor.stepSweep`; caused and
  mutated in `KnellBeamMechanismsTest`, MK5).
- **The Knell mob's warning line is NOT clipped at blocks (READ, `MobBeamer`):** it draws `WARNING` on the locked full
  line, and clips only the fired beam (`rayTraceBlocks`, then `BeamLine.clip`). The weapon does the same: its warning is
  drawn to the full range, per chunk-column segment; its beam is clipped at the first block.
- **End crystals (READ):** a ray's body filter is `LiveTargets` (live LIVING entities), so an end crystal is never a
  body: a plain ray and the beam pass through one and do nothing to it. The sweep keeps that filter, pinned in
  `KnellBeamSchemaTest.theSweepKeepsCastRaysFilters`.
- **Players and the caster (READ):** another player IS a body for a ray (and the sweep); the caster never is (excluded
  by id). **So the beam cannot strike its caster, and his own locked beam does nothing to him if he walks into its
  line.** Unchanged.
- **Folia (READ, our source):** the locked line is two `Vec3` values; the struck set is a set of UUIDs copied per hop; no
  entity handle crosses a tick. The mob's beam code is unchanged (`git diff` of `MobBeamer`, `MobBeam`, `BeamLine`,
  `BeamBox`: empty).

**PR 1's answers:**
- **CS1's `"[white] italic"` (READ):** a minted weapon's name is set in `WeaponItems.displayName` with
  `.decoration(TextDecoration.ITALIC, false)`, explicitly NOT italic. The dump prints `italic` when ANY component in the
  name's tree says ITALIC TRUE (`SelfTestLines.styled`), and it reads `ItemStack#effectiveName()`. **Every weapon's dump
  prints it:** all 37 `HELD` dumps in the 386 archived server logs (`run/logs/*.log.gz`) read `italic`, for every weapon
  and the stone, 0 plain. **So it is the instrument's reading of `effectiveName()`'s wrapper, not a defect of the
  staff** (INFERRED that Paper wraps a custom name in an italic component; not read from Paper's jar). Ben read the
  staff's name as "look good". Reported, not fixed.
- **The mana pool** (READ): `probe_mana` reads `ResourcePool.current` and `max` for `mana`, the pool `AbilityService`
  spends from.

**Weapon facts:**
- **The tooltip** (READ, `WeaponLoreLines.cooldownLabel`, and pinned by `KnellBeamContentTest.aMintedBeamsTooltipReads`
  from the real file): `Cooldown: 2.4s | Mana Cost: 13`.
- **No boot warning for its cooldown** (READ): the volley's floor is its windup, 24 (`minimumCooldownTicks`); 48 is
  above it.
- **The enchant pool:** attunement (it reaches a literal damage) and unbreaking. `echo_shard` has no durability, so
  Unbreaking rolls and grants nothing (as on the other staves). Pinned; carried open.
- **A given item's enchants are not ACTIVE** (READ in `GATE-sword-smithing.md`'s reading: candidates roll on acquire,
  the active choice is `-1` until the player picks), so the enchant multiplier is 1.0. KB2's `rpg enchant show` reads it.
- **His level is staged at 1** in the setup (`rpg playerxp set @s 1 levels`, then 40 ticks), and **executed**:
  `LevelBonus.at(1).weaponDamage()` = **0.0** (`at(50)` = 49.0).

## THE SEAT'S FOLLOW-UP, 2026-11-21: HIS HEALTH, THE NATURAL MOBS, THE SPOT, A ZOMBIE'S WIDTH, KB7'S TICKS

Written after the seat's rulings on PR 1 to 3, before any boot. Gate files only; nothing under `src/main/java`.

**a) HIS HEALTH AT LEVEL 1 (READ, our source, unless marked).**
- **What level grants** (`LevelBonus.at`): max health `5 x (level - 1)` to level 49, and +10 more at 50, so **+250 at
  50, 0 at 1**; defence `level / 5`, so **10 at 50, 0 at 1**; health regen `(level - 10) / 5 + 1` steps of 0.2 HP a
  second from level 10, so **1.8 at 50, 0 at 1**; crit chance `(level / 10) / 100`, so **0.05 at 50, 0 at 1**. **Mana:
  nothing** (`LevelBonus.sources` answers 0.0 for `MAX_MANA` and `MANA_REGEN`). His mana pool is the same at either
  level.
- **His own level: 50** (READ, the crystal-shard-staff boot of 2026-11-21: CS2's `sent=65.000` is 16 + 49, and
  an archived run's `REPLY "Set 50 level(s) for BaronVonYeetus (now level 50, MAX into it, 712580 lifetime)."` in
`run/logs/*.log.gz`).
  **So his maximum is `100 + 250 + G` at his level and `100 + G` at level 1** (`CombatantStats.DEFAULT_PLAYER_BASE` is
  100; G is his worn gear's, accessories' and fragments' max health).
- **What the staging does to his CURRENT health:** the reconcile loop (every 5 ticks, `PlayerHealthSystem`) applies the
  level's max-health modifier through `reconcileMaxModifiers`, and **a fall in max CLAMPS current to the new max**
  (`HealthState`: "increase is headroom, decrease clamps"). The setup waits 40 ticks. **So at full health he goes from
  `350 + G` to `100 + G`.**
- **What the restore does:** `SelfTestSnapshot.restore` puts back his lifetime XP (`setLifetimeXp`), so the next pass
  raises his max by 250 again, **and a rise is headroom: current is UNCHANGED.** The snapshot holds no health at all.
  **So he ends the run at his level-1 health, out of `350 + G`, and regains the rest by regeneration** (his level's 1.8
  a second plus whatever else he has). Not read by any row: the run has ended by then.
- **The run sheet says it in one plain line** (step 0). **The safety table is redone against `100 + G`** (above).
- **The rows read it:** the setup's first `SELF customHP=<c0>/<m0>` (his own level) and last `SELF customHP=<c1>/<m1>`
  (level 1). **Predicted: `m0 - m1 = 250.000`, and `c1 = min(c0, m1)`.** The `SELF` figures witness that he is alive and
  what his maximum is; no row reads a hit on him (`.claude/rules/verification.md`, *PREDICT DAMAGE AGAINST THE HURT
  LINE*).

**b) THE NATURAL MOBS (the setup's 27 kills).**
- **The list**, each one `kill @e[type=minecraft:<t>,distance=..64,tag=!selftest]`: zombie, zombie_villager, husk,
  drowned, skeleton, stray, bogged, parched, spider, cave_spider, creeper, witch, enderman, slime, phantom, silverfish,
  endermite, pillager, vindicator, evoker, ravager, vex, guardian, elder_guardian, breeze, creaking, warden. **Every
  name READ** as a `MobCategory.MONSTER` registration in the pinned server jar (`net.minecraft.world.entity.EntityType`,
  build 74), so none is an unknown type that would make its command fail.
- **Left off, on purpose:** every animal, villager, pet and item (not MONSTER, or not a living type); **the three
  MONSTER mounts `zombie_horse`, `camel_husk`, `zombie_nautilus`** (a mount may be his); and the nether and end ones
  (blaze, ghast, magma_cube, the piglins, hoglin, zoglin, wither_skeleton, zombified_piglin, shulker, the dragon,
  the wither), plus giant and illusioner (no natural spawn). `tag=!selftest` spares anything the instrument made.
  **A hostile he has NAME-TAGGED is on the list by type and would be killed**; the selector cannot tell. Named, not
  guarded.
- **What each kill prints** (READ: the pinned jar's `en_us.json`, and the archived logs' REPLY lines): `CMD /kill …`,
  then **`REPLY "No entity was found" [red]`** (none), **`REPLY "Killed <name>" []`** (one, by its display name, e.g.
  `"Killed Zombie"`), or **`REPLY "Killed <n> entities" []`** (more). Then one **`SETTLED dying=0 ticks=<t> first=<f>`**:
  the run waits until no killed body within 48 is still dying (READ, `SelfTestRun.settle`). Recorded, not predicted:
  how many he has near him is the world's.
- **The first LEFTOVER after the kills is KB0's**, and it is predicted: **`LEFTOVER living=0 radius=16`, or a list
  naming only types NOT on the kill list** (an animal, a villager, a pet, a mount). **A listed type in it is NOT AS
  PREDICTED.** (The setup's own LEFTOVER comes before the kills and is recorded as the baseline.)
- **Follow ranges, READ in the jar** (`createAttributes`, `FOLLOW_RANGE`): zombie (and its kin) 35; pillager, ravager,
  creaking 32; breeze, warden 24; vindicator, evoker 12; enderman 64; every other listed type the `Mob` default, 16.
  Our source sets no follow range (READ: no `FOLLOW_RANGE` under `paper/src/main`). The "every row" line above stands on
  these.
- **What still spawns** (READ): natural monster spawning needs `spawn_mobs` (`ServerLevel.isSpawningMonsters`), which
  the setup turns off. Spawner blocks have their own rule, `spawner_blocks_work`; phantoms and patrols read
  `spawn_phantoms` and `spawn_patrols` (`PhantomSpawner`, `PatrolSpawner`). **Whether `spawn_mobs` also gates those
  two: UNVERIFIED** (their call site was not read). The run touches none of them.

**c) WHERE HE STANDS.**
- **Every row stages from the same spot: where he stood when he typed the command.** At each row's start the run
  teleports him back there (READ, `SelfTestRun.nextScenario`: `player.teleportAsync(snapshot.start())`), and each row
  then turns him to face east (`yaw -90`). So one bad spot spoils every row the same way.
- **The run sheet's precondition** (step 0): open, flat ground with a clear view for about 30 blocks.
- **KB0, a cheap auto row at the start, reads the spot:** a `SELF` line (feet, `onGround`), then three block counts
  relative to his block, east being +x: the corridor the zombies stand in, **`BLOCKS AIR count=480 box=1 0 -4 30 1 3`**
  (30 east x 2 high x 8 across, from 4 north to 3 south, covering every zombie of KB2-KB8 but KB5's B); the floor
  under it, **`BLOCKS AIR count=0 box=1 -1 -4 30 -1 3`** and **`BLOCKS WATER count=0 box=1 -1 -4 30 -1 3`**; and KB5's
  B, **`BLOCKS AIR count=2 box=0 0 10 0 1 10`**. It reads, never judges. **A corridor count under 480 is something
  standing in it**: a block, but also grass, a flower or a snow layer, which the beam ignores (passable blocks are
  ignored by `rayTraceBlocks`, READ) and this count does not. **So a low count is read beside KB2-KB8's hits, not as a
  failure by itself.** A floor `AIR` above 0 is a drop; `WATER` above 0 is water.
- **How a bad spot shows in each row, so it is not read as a defect** (the zombies are summoned at his own feet height;
  a NoAI mob does not fall, INFERRED from the frozen Knell's reading in `GATE-dragon-teleport.md`):
  - **MOBSEED carries no position** (READ, `MobNameplateManager.onMobAppear`), and **a `PROBE`'s `dy` is the mob's
    feet minus his, so it reads 0.000 for every zombie summoned at `~`** whatever the ground does. **Neither can see
    the spot; KB0 is the witness.**
  - **Ground higher than his feet** puts a zombie inside a block: KB0's corridor count is low, and that zombie can
    lose health before the shot (in-wall damage, INFERRED vanilla), so **KB2's tick-23 probe reads under 100**.
  - **A block on the line** clips the beam: **KB2 and KB7 read no PLAYERHIT; KB3 reads fewer than three, stopping at
    the block; CS2R2 reads none.**
  - **A block beside the line, between it and a body struck only in its margin**: KB4's first zombie (0.2 off) is such a
    body, and the sweep refuses it without a clear sight line (READ, `CastExecutor.stepSweep`), so **KB4 reads no
    PLAYERHIT where one is predicted.**
  - **KB5's B inside a block** reads under 100 on its probe without being struck.
  - **KB6 cannot show a bad spot**: it predicts no hit, and a bad spot also gives none. **KB2 is its control**: the
    same zombie at the same 10 blocks, no wall, and a hit.

**d) A ZOMBIE'S WIDTH: READ, AND PRINTED BY THE ROW.**
- **READ in the pinned server jar** (build 74, `Implementation-Version: 26.1.2-74-e4e17fc`): `EntityType`'s `zombie`
  is `Builder.of(…, MobCategory.MONSTER).sized(0.6f, 1.95f).eyeHeight(1.74f)`. **An adult zombie is 0.6 wide and 1.95
  tall.** The rows summon `IsBaby:0b`.
- **And KB4 prints its own:** `probe_body: t` logs each zombie's `scale=` and `height=` (READ, the `PROBE_BODY` driver),
  predicted **`scale=1.000 height=1.950`** for all three (our `MobBodyApplier` puts a body only on a custom mob, READ in
  its javadoc; a summoned zombie is not one, INFERRED). **The width is 0.6 x scale**, so the reading carries it; and
  `probe_positions: "zombie 16"` logs their feet relative to his, predicted **`8.00 0.00 0.50`, `11.00 0.00 0.80` and
  `14.00 0.00 1.20`** in any order, which carries the offsets.
- KB4's "UNVERIFIED" is withdrawn (the prediction cell keeps its words and gains a dated note).

**e) KB7'S PRESSES AT 40 AND 48.**
- **Can a step land a tick late? Not by the run's own clock (READ).** A `wait:` is `onEntityLater`, which is
  `EntityScheduler.runDelayed` on his own scheduler (`PaperScheduler`); the server runs every live entity scheduler's
  `executeTick` once per tick from `MinecraftServer.tickChildren` (READ in the jar), and `runDelayed` counts those
  ticks. The cooldown's clock is `Bukkit.getCurrentTick` (`RpgPlugin` builds `CooldownTracker` with it), the server's
  tick counter. **Both count server ticks, so lag slows both alike.** Between press 1 and press 2 the row has one
  `time query` and the 40-tick wait; between press 2 and press 3, one `time query`, one `probe_mana` and the 8. None of
  those waits. **The one way a press lands late is his scheduler not being ticked (he left or died), and then the run
  aborts.** INFERRED: the synthetic click reaches `AbilityService` in the same tick (the refusal REPLY is sent from it).
- **The row now carries its own spacing:** each press is followed, in the same tick, by `time query gametime`, whose
  REPLY is **`"The time is <g>"`** (READ: `TimeCommand.queryGameTime` sends `ServerLevel.getGameTime`; the text is
  `commands.time.query` in `en_us.json`, and that this is the key it uses is INFERRED). **Predicted: `g2 - g1 = 40` and
  `g3 - g1 = 48`.** (INFERRED: game time advances one a tick; nothing here freezes ticking.)
- **What the presses read if one is off by one** (the REPLY text EXECUTED in jshell, `"On cooldown for %.1fs"` over
  `ticksRemaining / 20.0`; `ticksRemaining` is `ready - now`, `isReady` is `now >= ready`, READ):
  - **press 3 at 47**: REFUSED, `REPLY "On cooldown for 0.1s"` (1 tick left), its `MANA` unchanged, and **one
    PLAYERHIT in the row, not two**;
  - **press 3 at 49**: it fires exactly as at 48. **The presses cannot tell 49 from 48; the game-time lines can**;
  - **press 2 at 41** reads `"On cooldown for 0.4s"` (7 left), **the same text as at 40**, and at 39 `"0.5s"`. **So the
    refusal's text cannot tell 40 from 41 either; again the game-time lines can.**

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup:** `run/plugins/Rpg` and `run/world` copied to a NEW folder
   `../rpg-run-backups/<yyyy-mm-dd>-pre-knell-beam`; file and directory counts, `diff -rq lines: 0` for each, the `ls`
   of `world/dimensions`, the overworld's file count. Nothing is restored (Ben's word only).
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND:** "booting now", and Ben told **NOT to log in until told R0 passed.**
6. `./scripts/dev-server.sh --refresh-content`. **Required deployed lines:** `weapons/knell_beam.yml` reads
   `    cooldown_ticks: 48`; `weapons/` holds 20 `.yml` files; `visuals/knell_warning.yml` exists.
7. R0a to R0e on **this boot's** log, matched to **this boot's own** `Done` line.
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* `rpgfight` empty; the overworld's file count against step 2's; `All dimensions are saved`; no
   `java` process left.

## THE RUN SHEET — BEN TYPES EXACTLY THESE THREE LINES

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED.**
   **Stand on open, flat ground with a clear view for about 30 blocks, and face any way you like** (the rows turn you
   east and put you back on the same spot each time).
   **For the run your level is set to 1, so your health drops to about 100 (from about 350) and stays there after the
   run until it regenerates.**
1. `/gamemode survival`
2. `/rpg selftest knell-beam` — **ONCE.** About two minutes. Frozen zombies appear; they cannot move or hit you. **Two
   rows ask something of you, in gold chat:** one says **HOLD RIGHT-CLICK** (hold it until your mana runs out, then let
   go), and the last gives you 30 seconds with the Knell Beam, then asks a question: **type your answer in chat.**
   Your items, time and level are put back at the end.
3. `/stop`

Nothing else. If a row stalls or aborts, do not intervene. **His chat answer to the one question is the only line he
types beyond these three**, and it is the selftest's own ASK step, logged verbatim; "not sure" is NOT STATED.

## THE ARITHMETIC (SAFETY): THE MOST HITS THAT CAN LAND ON BEN TIMES THE BIGGEST HIT, AGAINST THE HEALTH HE HAS IN THE RUN

**The health he has during the run is `100 + G`, not his own `350 + G`** (G: what his worn gear, accessories and
fragments add to max health; unknown here, and printed by the setup's two `SELF` lines). His defence is his gear's
alone (level 1 grants 0). Below, G = 0 and defence 0: the worst case. *(This table read "against 100 and 350" until the
seat's follow-up of 2026-11-21; the run never has him at 350.)*

| row | where he is | what can hurt him | most hits x biggest hit | puts him down at 100 + G? |
|---|---|---|---|---|
| setup, KB0, KB1 | the spot he chose | nothing the run makes | 0 | no |
| KB2-KB7, CS2R2 | the run's start, his block's centre, facing east; NoAI zombies 6 to 14 blocks away | a NoAI zombie does not move or attack; **his own beam cannot strike him** (the sweep excludes the caster by id, READ) and his shard skips him (`castRay`'s ignore id) | 0 x 0 = 0 | no |
| HF1R2 | the run's start | the same; his shards skip him | 0 | no |
| KB8 | the run's start; six NoAI zombies, 30 seconds in which he may walk | the same; **walking into his own locked beam's line does nothing to him** (excluded by id, READ) | 0 | no |
| every row | the overworld at night, **after the setup's kills** | **a natural hostile that was BEYOND 64 blocks at the kill and closes in during the run.** None has him as a target from there: the longest follow range among the listed types is the enderman's 64, which attacks when provoked or stared at (vanilla, INFERRED); every other is 35 or less (READ, below). New natural spawns are off (`spawn_mobs false`, READ). **Not off: a spawner block, and phantoms and patrols if `spawn_mobs` does not gate them (UNVERIFIED).** Each row's `LEFTOVER … radius=16` line names any living body near the start, 12 times in the run | at gs 100 (every overworld hostile, READ), 5 x vanilla (READ, `MobScaling`); vanilla UNVERIFIED: a zombie 3 -> 15 a hit, a creeper at point blank about 43 -> about 215 | **at 100:** 7 zombie hits, or one close creeper. The run is at most 4,232 ticks (about 3 min 32 s; below), and a mob from beyond 64 must first wander inside its follow range. Possible, not likely; a death aborts the run (INFERRED: `SelfTestStaging.invalid` takes `isDead`) and costs no items (keep-inventory is forced, READ in `PlayerHealthSystem`'s note on `RpgListeners`) |

**The run's length, from the scenario's own waits** (`wait:` steps, summed per row; recount with
`grep -E '^\s+- wait:' paper/src/main/resources/selftest/GATE-knell-beam.yml`):

| row | waits | ticks |
|---|---|---|
| setup | 40 | 40 |
| KB0, KB1 | none | 0 |
| KB2 | 5 + 2 + 23 + 2 | 32 |
| KB3 | 60 + 30 | 90 |
| KB4 | 5 + 55 + 30 | 90 |
| KB5 | 60 + 5 + 30 | 95 |
| KB6 | 60 + 30 | 90 |
| KB7 | 60 + 40 + 8 + 30 | 138 |
| CS2R2 | 5 + 2 + 10 | 17 |
| HF1R2 | 300 | 300 |
| KB8 | 600, then the ASK's up to 1,200 | 600 to 1,800 |
| **sum** | | **1,492, plus the ASK's 0 to 1,200** |

On top of that: about 5 ticks per row for the row-start clean (READ, an archived run: `setup DONE ticks=5` with no
waits), so about 60 for 12 rows; and each `kill` step waits for the bodies it killed to finish dying, up to 40 ticks
(`SelfTestStaging.SETTLE_CAP_TICKS`, READ). **Worst case: 1,492 + 1,200 + 60 + 37 kill steps x 40 = 4,232 ticks**
(37: `grep -cE '^\s+- cmd: "kill '` on the scenario, 27 in the setup and 10 in the rows). With
nothing to kill and a prompt answer it is about 1,600 ticks (80 seconds) plus his answer.

## R0 — THE BUILD, FOR THE WHOLE STACK. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `feat/knell-beam`'s head as `git ls-remote` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries all three PRs

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/weapons/knell_beam.yml` (line `    cooldown_ticks: 48`), the four `knell_*` visuals, `selftest/GATE-knell-beam.yml` (line `HF1R2:`), `selftest/index.yml` (line `  - knell-beam`); the classes `rpg/core/combat/RaySweep.class` (PR 2) and, by content, `rpg/paper/command/RpgCommand.class` carrying `FIRERATE player=` and `rpg/paper/selftest/SelfTestDrivers.class` carrying `MANA current=` (PR 1). The control `content/weapons/flint_staff.yml`: PRESENT | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Line($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Entry($name) { if ($zip.GetEntry($name) -eq $null) { "ABSENT  entry $name" } else { "PRESENT entry $name" } }
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
foreach ($e in @('weapons/knell_beam.yml', 'visuals/knell_toll_cast.yml', 'visuals/knell_warning.yml', 'visuals/knell_toll_line.yml', 'visuals/knell_toll_hit.yml', 'weapons/flint_staff.yml')) { Entry ('content/' + $e) }
Line (Bytes 'content/weapons/knell_beam.yml') '    cooldown_ticks: 48' 'knell_beam.yml'
Line (Bytes 'selftest/GATE-knell-beam.yml') 'HF1R2:' 'GATE-knell-beam.yml'
Line (Bytes 'selftest/index.yml') '  - knell-beam' 'index.yml'
$p = 'io/github/butterflysmp/rpg/'
Entry ($p + 'core/combat/RaySweep.class')
Has ($p + 'paper/command/RpgCommand.class') 'FIRERATE player='
Has ($p + 'paper/selftest/SelfTestDrivers.class') 'MANA current='
$zip.Dispose()
```

### R0c — every Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| Exactly one of each, up to the first `joined the game`: **`[Rpg] Loaded 11 abilities, 37 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 20 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 4 recipes, 1 ores, 4 vein sizes`** (against `GATE-crystal-shard-staff.md` R0c, read AS PREDICTED at `4d275653`: **visuals 33 -> 37, weapons 19 -> 20**; every other figure unchanged); `GATE-fight-revive.md`'s R0c fight line and `GATE-loot-roll.md`'s R0c loot line, word for word (the loot line prints no pool, and the beam joins none); the world-damage and smithing lines as at the last boot; **`[Rpg] Mint-on-craft: 36 indexed, 36 claiming, of 50 gear definitions`** (49 -> 50: 20 + 1 + 24 + 5; the beam claims no craft_result); `Convert-on-arrival: 7 materials {…}` as before. **No `Refusing`, `Skipping`, `SEVERE`, `Exception`, `Recipe '`; no `[Rpg]` WARN beyond `volley_stone`'s known pair**: the beam's 48 is above its volley floor of 24, so it warns nothing (READ). No `No fights loaded` | `Select-String run\logs\latest.log -Pattern 'Loaded\|Mint-on-craft\|Convert-on-arrival\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded\|Recipe '''` | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 267 scenarios across 36 gates [melee-cell, …, crystal-shard-staff, knell-beam]`** (266 until the seat's follow-up added KB0, 2026-11-21). The control: the last boot's `256 scenarios across 35 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (2026-11-21, PR 3's tree, from `index.yml` and every gate file it names; `SelfTestScenariosTest` asserts
266): 36 gates, `^[A-Za-z0-9_-]+:` **302**, `^setup:` **36**, so **266**. Control: 291 + 11 (setup, KB1-KB8, CS2R2,
HF1R2) = 302.
**RECOUNTED AGAIN** (2026-11-21, after the seat's follow-up, by the same method; `SelfTestScenariosTest` asserts 267):
36 gates, `^[A-Za-z0-9_-]+:` **303**, `^setup:` **36**, so **267**. Control: 302 + 1 (KB0) = 303.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, no `deleting`, no `orphan` | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | |

## THE ROWS (`/rpg selftest knell-beam`) — SURVIVAL, ALL AUTO

**EVERY DAMAGE PREDICTION NAMES EVERY TERM** (`.claude/rules/verification.md`):
`sent = (amount x score/100) x enchant + class bonus + level bonus`, then `x charge x crit`.
**For the Knell Beam:** `(63 x 100/100) x 1.0 + 0 + 0 = 63`, `x 1.0`, and `x 2.0` on a crit: **`sent=63.000 crit=false`
or `sent=126.000 crit=true`**. Terms: amount 63 (the file); score 100 (`rpg gearscore set 100`, staged); enchant 1.0
(no active enchant on a given item; KB2's `rpg enchant show` reads it); class bonus 0 (nothing grants class damage);
level bonus 0 (level 1, staged; executed 0.0); charge 1.0 (not a melee charge).
**For the Crystal Shard Staff (CS2R2):** `(16 x 100/100) x 1.0 + 0 + 0 = 16`: **`sent=16.000 crit=false` or
`sent=32.000 crit=true`**. Any other `sent` is a FAIL. A zombie here has `max=100` (`MOBSEED … gs=100 … max=100`).

### setup — night, no spawns, the natural hostiles killed, his level at 1 (SURVIVAL)

*(Added by the seat's follow-up, 2026-11-21, before any boot.)*

| prediction | stands on | READING |
|---|---|---|
| Its own `LEFTOVER … radius=16` first (recorded: the world's, before the kills). The `time set` and `gamerule` REPLYs. **`SELF customHP=<c0>/<m0> at=… onGround=true fall=0.000`** (his own level: `m0 = 350 + G`). **27 kill lines**, each `CMD /kill @e[type=minecraft:<t>,distance=..64,tag=!selftest]` then one of `REPLY "No entity was found" [red]`, `REPLY "Killed <name>" []`, `REPLY "Killed <n> entities" []`, then `SETTLED dying=0 ticks=<t> first=<f>` (recorded, not predicted). `REPLY "Set 1 level(s) for BaronVonYeetus (now level 1, 0/1000 into it, 0 lifetime)." [green]` (as archived runs read it). After the 40 ticks, **`SELF customHP=<c1>/<m1> …` with `m0 - m1 = 250.000` and `c1 = min(c0, m1)`** | *THE SEAT'S FOLLOW-UP* a) and b) | |

### KB0 — THE SPOT: open and flat for 30 blocks? (SURVIVAL)

*(Added by the seat's follow-up, 2026-11-21, before any boot.)*

| prediction | stands on | READING |
|---|---|---|
| **`LEFTOVER living=0 radius=16`**, or a list naming only types NOT on the setup's kill list (a listed type is NOT AS PREDICTED). The teleport's REPLY, facing east. **`SELF customHP=<c1>/<m1> …`** (its `onGround` recorded, not predicted: it is read in the teleport's own tick). **`BLOCKS AIR count=480 box=1 0 -4 30 1 3`**; **`BLOCKS AIR count=0 box=1 -1 -4 30 -1 3`**; **`BLOCKS WATER count=0 box=1 -1 -4 30 -1 3`**; **`BLOCKS AIR count=2 box=0 0 10 0 1 10`**. **Any other count is not a defect of the build: it is the spot**, and KB2-KB8 are read beside it (*THE SEAT'S FOLLOW-UP* c) says how each row shows it). A corridor count under 480 from grass or flowers alone does not touch the beam | `SelfTestDrivers.probeBlocks` (a box relative to his block); passable blocks ignored by the beam (READ) | |

### KB1 — given, held, and its tooltip (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| REPLY `Given Knell Beam`; `INVENTORY knell_beam slots=[…]`; `DRIVE hold …`; the gearscore REPLY; then `HELD knell_beam ECHO_SHARD x1 name="Knell Beam" [dark_purple] italic` (the `italic` every weapon's dump prints, above) and lore, in order: **`Void`**, **`Gear Score: 100`**, `` (blank), **`Knell's Toll  Right-Click`**, **`Locks a line where you look,`**, **`then tolls a beam down it 1.2s later.`**, **`Void Damage: 63`**, **`Cooldown: 2.4s \| Mana Cost: 13`**, `` (blank), **`Epic Magic Weapon`** | `KnellBeamContentTest.aMintedBeamsTooltipReads` (the same ten lines from the real file); `golden-lore.txt` (`#AA00AA`, dark_purple, for void and epic) | |

### KB2 — one cast straight ahead: 13 mana, and the hit on tick 24 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `rpg enchant show`: its REPLY shows no active enchant (every slot's active choice `-1`). `MOBSEED … zombie gs=100 … max=100`; the aim `d=10.000`; **`MANA current=<M> max=<M>`** (just refilled), `DRIVE use … item=knell_beam`, **`MANA current=<M-13> max=<M>`**, read on the press's own tick: **13 less**. Then the probe 23 ticks after the press: **`customHP=100.000/100.000`** (not yet struck); then exactly one **`PLAYERHIT … source=knell_beam/right_click … zombie element=void sent=63.000 crit=false triggerScore=100`** (or 126.000 crit=true); then the probe 25 ticks after: **`customHP=37.000/100.000`** (100 - 63), or dead on a crit. **So the shot lands on tick 24, between the two probes.** The warning line drawn at the click is NOT read by any instrument (client render) | the sum above; `AbilityService` spends at the click (READ); `CastSpec.Volley` windup 24 | |

### KB3 — PIERCE: three on the line, three hits, nearest first (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Three `MOBSEED` lines in summon order (6, 10, 14 blocks). After the press, exactly **three** `PLAYERHIT … element=void` lines, **each `sent=63.000 crit=false`** (or all three 126.000: one crit roll per shot), their `target=` uuids **in the MOBSEED order: nearest first**. The probe: all three at `37.000/100.000` (or dead) | `KnellBeamMechanismsTest.aPiercingRayStrikesThreeBodiesInALineOnceEachNearestFirst` | |

### KB4 — ACCURACY: 0.2 off is struck, 0.5 and 0.9 off are not (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Facing due east, level; the line runs along his eye at z = 0. The zombies' FEET at z +0.5, +0.8 and +1.2 (at 8, 11 and 14 east). **A zombie is 0.6 wide (UNVERIFIED in the jar)**, so its box's near side is 0.3 nearer the line: **gaps 0.2, 0.5 and 0.9**. Tolerance 0.3: **0.2 < 0.3 struck; 0.5 > 0.3 not; 0.9 > 0.3 not, though 0.9 < 1.0, the old weapon's.** Exactly **one** PLAYERHIT, on the first `MOBSEED`'s uuid, `sent=63.000` (or 126.000 crit). The probe: the first at `37.000/100.000`, the other two at `100.000/100.000`. **Added 2026-11-21 (the seat's follow-up d), before any boot: the width is now READ in the jar (`sized(0.6f, 1.95f)`), and the row prints its own.** Five ticks after the summons: **`POSITIONS ZOMBIE n=3 at=[…] radius=16`** holding `8.00 0.00 0.50`, `11.00 0.00 0.80` and `14.00 0.00 1.20` in any order; then three **`BODY tag=t <uuid> type=zombie … scale=1.000 height=1.950 …`** lines. **If a scale reads other than 1.000, that zombie's width is 0.6 x scale and its gap is recomputed from it before the hit is judged** | `KnellBeamMechanismsTest.aBodyInsideTheToleranceIsStruckAndOneOutsideIsNot` and its 1.0 control; *THE SEAT'S FOLLOW-UP* d) | |

### KB5 — LOCKED AIM: he turns, the shot goes down the old line (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Zombie A (first `MOBSEED`) due east at 10, zombie B (second) due south at 10. The press facing east; 5 ticks later `tp @s ~ ~ ~ 0 0` turns him south, to B. Exactly **one** PLAYERHIT, on **A**; the probe: A at `37.000/100.000`, **B at `100.000/100.000`** | `KnellBeamMechanismsTest.aLockedShotGoesDownTheLineCapturedAtTheCast` and its unlocked control | |

### KB6 — A WALL: no hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Two stone blocks 5 east at his feet and eye height (`save_blocks` first; the restore puts them back), a zombie 10 east. **0 PLAYERHIT**; the probe: `100.000/100.000` | the clip at the first block (`KnellBeamMechanismsTest.aBlockEndsAPiercingWalk`) | |

### KB7 — COOLDOWN: refused at 40, fires at 48 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `MANA current=<M>`; press 1 (fires 24 ticks later); press 2, **40 ticks after press 1**: REPLY **`On cooldown for 0.4s`** [gray] (8 ticks left of 48, READ: `RpgListeners`' OnCooldown arm), then `MANA current=<x>` with **x = M - 13 plus 40 ticks of regeneration** (the refused press spent nothing); press 3, **48 ticks after press 1**: fires (no refusal REPLY), then `MANA current=<x - 13>` (to within the regeneration of the same tick: none). **Exactly two PLAYERHITs** (press 1 and press 3), each `sent=63.000` (or 126.000); the second kills the zombie (37 left after the first), so the probe finds it dead or gone. **Added 2026-11-21 (the seat's follow-up e), before any boot:** after each press, in the same tick, **`REPLY "The time is <g1>"`**, **`<g2>`**, **`<g3>`**, with **`g2 - g1 = 40` and `g3 - g1 = 48`**. Off by one, the row reads: press 3 at 47, `REPLY "On cooldown for 0.1s"`, mana unchanged, ONE PLAYERHIT; at 49, the same as 48; press 2 at 41, the same `0.4s` text as at 40. **Only the game-time lines tell those apart** | `AbilityService` stamps the cooldown at the click, `isReady` is `>=` (READ); *THE SEAT'S FOLLOW-UP* e) | |

### CS2R2 — OWED: the Crystal Shard Staff at level 1 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Exactly one **`PLAYERHIT … source=crystal_shard_staff/right_click … element=kinetic sent=16.000 crit=false triggerScore=100`** (or `sent=32.000 crit=true`): `(16 x 100/100) x 1.0 + 0 + 0`. The probe: `84.000/100.000`. Against `GATE-crystal-shard-staff.md` CS2's 65.000 at level 50 | the sum above; the seat's ruling 1 | |

### HF1R2 — OWED: held fire on the 14-tick staff, read FROM THE LOG (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The first `rpg firerate` (the row's own) prints whatever an earlier row left and clears it. `TELL HOLD RIGHT-CLICK NOW …`. 15 seconds. Then the row's own `rpg firerate`, whose lines land in the log TWICE each, as `FIRERATE player=BaronVonYeetus <line>` and as the selftest's REPLY: `Fire cadence -- crystal_shard_staff, right_click`, **`  INPUTS  count … min <i>t`**, **`  FIRES   count … min <f>t`**, **`  cooldown_ticks 14 authored, 14 effective (not a basic attack -- attack speed does not scale it)`**, a verdict line, `Sample cleared.` **By `NEXT.md`'s held-fire table, restated for 14:** **`FIRES min 16`, `INPUTS min 4`: inconclusive**; **`FIRES min 16`, `INPUTS min 3`: the fit survives**; **`FIRES min 14` or `15`: `ceil(n/4) x 4` IS FALSIFIED**; below 14, a cooldown defect. **If he did not hold:** `No fire-cadence sample. …`, and the reading is UNREAD again. Mana: about 12 fires (100 / 8, plus regeneration) | `RpgCommand.say` (`FireRateLogTest`); the seat's ruling 2 | |

### KB8 — A FREE GO, THEN THE ONE QUESTION (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| Six `MOBSEED` lines; `TELL 30 seconds with the Knell Beam …`; 30 seconds in which any PLAYERHITs are his, each `sent=63.000` or `126.000` (level 1, score untouched since KB7: **he holds a new beam, unscored here, so `triggerScore` may read 100 by default or as rolled**; any sent is recorded, none predicted); then `ASK 1 question="Could you tell where the beam would land, and did it need a careful aim?" answer="<his words>"`, verbatim, or `answer=NOT STATED after 1200 ticks` | `SelfTestAsk` | |

## WHAT THIS GATE DOES NOT READ

- **What he sees and hears**: the warning line, the beam, the sounds. AUTO reads the server, never the client
  (`.claude/rules/verification.md`); only his one answer speaks to it.
- **The crafted item's tooltip Gear Score** (CR1, carried twice): it rides on the next boot that has a craft in it for
  another reason, read from a selftest dump after that craft. No craft is asked of Ben here.
- **Unbreaking granting nothing on the beam**, and **the shard or beam against a player**: source only.

## READINGS, 2026-11-22 — ONE RUN AT `61b617ef`, READ FROM `run/logs/latest.log` BY CC

**Written after the run. No prediction above was edited; where a reading differs, it says so here.** Every line quoted is
from this boot's log (`[Rpg] Build: 61b617ef`, the one `Done` at 10:39:54). Readings count only after his LOGIN line
(10:42:51).

### THE BOOT, BY `booting.md` AND THE CARRIED STATE

| step | read |
|---|---|
| 1. the seat's word and Ben's | the seat's boot approval (relayed 2026-11-22); Ben's own reply to "say ready in your own words", verbatim: **"ok"** |
| 2. the heads | `git ls-remote`: `feat/knell-beam` `61b617ef11083634224f3290e959d89875afd51b`, `feat/knell-mechanisms` `7a3e46db…`, `feat/knell-owed-fixes` `4df507f4…`, `master` `141308e1…`, as the seat diffed. Checks on all three heads: `build COMPLETED SUCCESS` x2, `CLEAN` |
| CS 1. no JVM | `no java process` |
| CS 2. backup | `../rpg-run-backups/2026-11-22-pre-knell-beam` (new): Rpg 144 files / 26 dirs both sides, `diff -rq lines: 0`; world 193 files / 32 dirs both sides, `diff -rq lines: 0`; `world/dimensions`: `minecraft rpgfight`; overworld 106 files |
| CS 3. config | `12:  dev-pack-on-join: true` |
| CS 4. fight folders | `find run/world/dimensions/rpgfight -mindepth 1`: nothing |
| CS 5. "booting now" | sent in its own message, with "Ben: do NOT log in until I tell you R0 passed"; the boot command in the next turn |
| CS 6. boot, deployed lines | `==> Building 61b617ef`; `weapons/knell_beam.yml:45:    cooldown_ticks: 48`; `weapons/` 20 `.yml`; `visuals/knell_warning.yml` present. **The boot's build runs no unit tests** (no `Tests run:` line in its output); the three PRs' CI is the test witness |
| CS 7. R0a to R0e | below, all AS PREDICTED |
| CS 8. "R0 passed" | sent in its own message before he logged in. **His first `joined the game`: 10:42:51**, after R0 was read (Done 10:39:54) |
| CS 9. after `/stop` | `rpgfight` empty; overworld **106** files (step 2: 106); `[10:46:05] … All dimensions are saved`; `no java process` |

### R0 — AS PREDICTED, EVERY ROW

- **R0a:** `[10:39:53] [Server thread/INFO]: [Rpg] Build: 61b617ef`. Not `-dirty`, not `unknown`.
- **R0b:** the scan printed PRESENT for all twelve predicted items (the six content entries including the `flint_staff`
  control, `    cooldown_ticks: 48`, `HF1R2:`, `  - knell-beam`, `RaySweep.class`, `FIRERATE player=`, `MANA current=`).
  An extra control, not in the gate: `content/weapons/ember_staff.yml` ABSENT.
- **R0c:** `[Rpg] Loaded 11 abilities, 37 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects,
  20 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 4 recipes, 1 ores, 4 vein sizes`,
  exactly once; `Mint-on-craft: 36 indexed, 36 claiming, of 50 gear definitions`; `Convert-on-arrival: 7 materials {…}`.
  The loot, fight, world-damage and smithing lines are **byte-identical** (timestamp stripped) to the last boot's
  (`run/logs/2026-11-21-2.log.gz`, `Build: 4d275653`, the crystal-shard-staff boot). No `Refusing`, `Skipping`, `SEVERE`,
  `Exception`, `Recipe '`, `No fights loaded`. `[Rpg]` WARN: only `volley_stone`'s pair. Paper's own "4 release(s)
  behind" WARN block is not `[Rpg]`.
- **R0d:** `[Rpg] Selftest: dev server, 267 scenarios across 36 gates [melee-cell, …, crystal-shard-staff, knell-beam]`.
  Control `256 scenarios across 35 gates`: 0 occurrences.
- **R0e:** `[Rpg] FIGHTWORLD sweep path=C:\…\run\world\dimensions\rpgfight folders=0`, once; no `deleting`, no `orphan`.

### WHAT BEN TYPED, AGAINST THE SHEET

| time | command | on the sheet? |
|---|---|---|
| 10:43:13 | `/gamemode survival` | yes (1) |
| 10:43:18 | `/rpg mana refill` | **EXTRA**, before the run. Touches mana only; KB2 to KB8 each refill their own |
| 10:43:21 | `/rpg selftest knell-beam` | yes (2), once |
| 10:44:50 | chat `yes` | the ASK answer |
| 10:44:59 | `/rpg give knell_beam` | **EXTRA**, after `SELFTEST RUN FINISHED` (10:44:50) |
| 10:45:19 | `/rpg spawn knell` | **EXTRA**, after the run. The Knell mob telegraphed and fired at him once (`BEAM fire … gap=4.791 miss] dealt=none`) and died at 10:45:33. Not a gate row |
| 10:46:04 | `/stop` | yes (3) |

No `/rpg heal`.

### THE ROWS

**setup — AS PREDICTED.** Baseline `LEFTOVER living=0 radius=16`. `time set 18000` and `spawn_mobs false` REPLYs.
**`SELF customHP=160.000/350.000 at=-109.89 63.00 21.00 onGround=true fall=0.000`**: so `m0 = 350`, **G = 0**, and he
was NOT at full health (160). 27 kill lines; **27 `REPLY "No entity was found" [red]` and 0 `Killed`
lines in the setup** (counted: `sed -n '97,194p' run/logs/latest.log | grep -c 'setup REPLY "No entity was found"'`
and the same for `"Killed`; the REPLYs interleave with the next CMD, so they are counted, not paired); every
`SETTLED dying=0 ticks=0 first=0`. Nothing listed was within 64. `REPLY "Set 1 level(s) for
BaronVonYeetus (now level 1, 0/1000 into it, 0 lifetime)." [green]`. **`SELF customHP=100.000/100.000`**:
`m0 - m1 = 350 - 100 = 250.000` as predicted, and `c1 = min(160, 100) = 100` as predicted. `setup DONE ticks=45`.

**KB0 — NOT AS PREDICTED, ONE COUNT.** `LEFTOVER living=0 radius=16` (as predicted). Teleport to `-109.500000,
63.000000, 21.500000`, facing east. `SELF customHP=100.000/100.000 at=-109.50 63.00 21.50 onGround=true`.
`BLOCKS AIR count=480 box=1 0 -4 30 1 3` (as predicted); `BLOCKS AIR count=0 box=1 -1 -4 30 -1 3` (as predicted);
**`BLOCKS WATER count=26 box=1 -1 -4 30 -1 3`** (predicted 0); `BLOCKS AIR count=2 box=0 0 10 0 1 10` (as predicted).
**So the spot had 26 water blocks in the 240-block floor under the corridor.** The corridor itself was clear (480 of
480). By the gate's own c): a NoAI zombie does not fall (INFERRED), the beam ignores fluids, and every row below landed
its hits where predicted, so **no row reads as spoiled by it**. Recorded, not explained further.

**KB1 — TEXT AS PREDICTED; THE NAME'S COLOUR LIST NOT AS PREDICTED.**
`HELD knell_beam ECHO_SHARD x1 name="Knell Beam" [yellow, dark_purple] italic lore=["Void" [dark_purple]; "Gear Score:
100" [gray, aqua]; "" []; "Knell's Toll  Right-Click" [gold, yellow]; "Locks a line where you look," [gray]; "then tolls
a beam down it 1.2s later." [gray]; "Void Damage: 63" [gray, red]; "Cooldown: 2.4s | Mana Cost: 13" [dark_gray]; "" [];
"Epic Magic Weapon" [dark_purple]]`. All ten lore lines are word for word the prediction. **The name printed
`[yellow, dark_purple]` against a predicted `[dark_purple]`.** The dump lists every colour in the name's component tree
(`SelfTestLines.styled`, as the gate's CS1 note says for `italic`). INFERRED, NOT READ: the outer `yellow` is the
wrapper `effectiveName()` puts round a custom name, in `ECHO_SHARD`'s vanilla item rarity, and the inner `dark_purple`
is ours and is what renders. What the client shows is not an AUTO reading.

> **KB1's YELLOW, READ 2026-11-22 (`feat/lantern-owed-fixes`, no boot; the INFERRED above is now READ where marked).**
> - **READ, our source:** `SelfTestLines.styled` lists every colour in the component tree it is handed, and the dump
>   hands it `ItemStack#effectiveName()`.
> - **READ, the pinned server jar:** `CraftItemStack.effectiveName()` returns vanilla `ItemStack.getStyledHoverName()`,
>   which appends the hover name (our custom name) to an EMPTY outer component, sets that outer component's colour
>   to the item's `Rarity.color()`, and sets ITALIC on it when the stack has a `CUSTOM_NAME`. `Items.ECHO_SHARD` is
>   registered `Rarity.UNCOMMON`, whose colour is YELLOW. So **the yellow is vanilla's rarity colour on the wrapper**,
>   and the `italic` every dump prints is the same wrapper's custom-name flag (CS1's "INFERRED" is now READ).
>   `getRarity` raises the rarity when the stack carries VANILLA enchantments (READ); ours are not vanilla's.
> - **What the client shows: dark purple, upright (INFERRED, not an AUTO reading).** Our inner component sets
>   `dark_purple` and `ITALIC false` explicitly, and an explicit child style overrides the inherited one (Adventure
>   and vanilla style inheritance). Ben read the staff's name as "look good" (CS1).
> - **Fixed: nothing.** A `lantern` is registered with default properties, COMMON (READ), whose colour is WHITE, so the
>   Living Lantern's dump will read `[white, <ours>]`.

**KB2 — NOT AS PREDICTED, ONE LINE: THE PROBE AFTER THE HIT.** `REPLY "  raw:
v1;unbreaking=0:-1;attunement=0:-1;unbreaking=0,attunement=0:-1"`, so every slot's active choice is `-1` (as
predicted). `MOBSEED 2e1303ea-… zombie gs=100 source=rolled max=100`; `DRIVE aim … d=10.000`; **`MANA
current=100.000`, the press, `MANA current=87.000`** (13 less, as predicted). The probe 23 ticks after the press:
`customHP=100.000/100.000` (as predicted). Then exactly one `PLAYERHIT … source=knell_beam/right_click
target=2e1303ea-… zombie element=void sent=63.000 crit=false triggerScore=100` (as predicted). **Then the probe 25
ticks after the press: `customHP=100.000/100.000 … dead=false`, predicted `37.000/100.000`.** The zombie was cleared
by KB3's CLEAN (`MOBREMOVE 2e1303ea-…`) with no later probe, so **whether the 63 reached its health is UNREAD on this
row.** INFERRED, NOT READ: `EffectApplier.trace` writes the `PLAYERHIT` line after `applyDamage`, and
`CombatantHandle` only dispatches, so the health write may land a tick or more after the line, after the tick-25
probe. KB3, KB4 and KB5 probe 25 to 30 ticks later and do read the 63, so the hit lands by then. **Owed: read the
dispatch path, and whether the shot lands on tick 24 or 25.**

> **KB2's TICK, READ 2026-11-22 (`feat/lantern-owed-fixes`, no boot). Each step marked.** T is the press's tick.
> 1. **The press, tick T (READ, our source):** the `use:` step's synthetic click reaches `WeaponFire.attempt`, which runs
>    `AbilityService` inline (mana spent, cooldown stamped at T), then hops with `adapters.scheduler().onRegion(eye, …)`.
> 2. **The cast executes on T+1, in the GLOBAL tick (READ, the pinned jar):** on Paper, `onRegion` is
>    `FallbackRegionScheduler.execute` → the global region scheduler's `run`, delay 1; `MinecraftServer` ticks the
>    global region scheduler first, then each entity scheduler's `executeTick`.
> 3. **The shot fires in his entity tick at T+24 (READ):** the volley's `scheduleOn(caster, 24)`, made from the global
>    tick of T+1 before his scheduler has ticked that tick, keys the task at his scheduler's `tickCount + 24`. The
>    `PLAYERHIT` line is written there, by `EffectApplier.trace`, after `applyDamage` returns.
> 4. **The health write is NOT inline (READ, our source):** `BukkitCombatant.applyDamage` is
>    `ctx.scheduler().onEntity(entity, () -> applyNow(…))`, and `PaperScheduler.onEntity` is
>    `entity.getScheduler().run(…)`, which is `runDelayed(…, 1)` (READ, the jar). The store write is inside `applyNow`.
> 5. **So the zombie's health moves on T+25 (READ path; the result INFERRED):** the entity schedulers to run in a tick
>    are copied before any runs (READ, `getAllSchedulers`), so the zombie's write, queued at T+24, lands at T+25.
>    Whether his tick-25 probe step runs BEFORE or AFTER the zombie's write within T+25 is their order in the server's
>    scheduler list: **INFERRED player first** (registered earlier), which is what KB2's `100.000` at tick 25 shows.
> - **Predicted for KB2R2 (probes on ticks 24, 25 and 26 after the press):** **24 `100.000/100.000`** (READ: the write is a
>   tick after the hit at the earliest. Which of the probe's line and the PLAYERHIT line comes first in tick 24 is NOT
>   predicted: both are due in his entity tick, the probe's wait queued at the press and the shot at the press + 1, so
>   INFERRED the probe first); **25 `100.000/100.000`** (INFERRED, from the order; `37.000` if the order is the other way); **26
>   `37.000/100.000`**, or dead on a crit (READ). Carried as a row in `GATE-living-lantern.md`.
> - **What it means for any cast whose hit is made from the GLOBAL tick** (the cast frame itself, and anything deferred
>   with `CombatWorld.schedule`, which is `onRegionLater`, READ): the zombie's write is queued before that tick's
>   entity-scheduler copy, so it lands in the SAME server tick as the hit (READ path, INFERRED result). A hit made on
>   `scheduleOn(caster, …)` lands one tick later.

**KB3 — AS PREDICTED.** MOBSEEDs `e90247c3` (6), `04f95ba5` (10), `a948a6e4` (14). Three `PLAYERHIT … element=void
sent=63.000 crit=false triggerScore=100`, targets `e90247c3`, `04f95ba5`, `a948a6e4`: **nearest first, in MOBSEED
order**. Probes: all three `customHP=37.000/100.000`.

**KB4 — AS PREDICTED.** `POSITIONS ZOMBIE n=3 at=[8.00 0.00 0.50; 11.00 0.00 0.80; 14.00 0.00 1.20] radius=16`; three
`BODY … type=zombie … scale=1.000 height=1.950 …`. Exactly one `PLAYERHIT`, on the first MOBSEED (`b93f5d05`),
`sent=63.000 crit=false`. Probes: `b93f5d05` `37.000/100.000`, `74e4fabf` `100.000/100.000`, `8d761895`
`100.000/100.000`. **So 0.2 off was struck and 0.5 and 0.9 off were not, at tolerance 0.3.**

**KB5 — AS PREDICTED.** A = `eef9bb61` (east 10), B = `08b662b6` (south 10). Press facing east, then `tp @s ~ ~ ~ 0 0`.
One `PLAYERHIT`, on A. Probes: A `37.000/100.000`, B `100.000/100.000`.

**KB6 — AS PREDICTED.** `STAGE saved 2 blocks for the restore, box=5 0 0 5 1 0`; `Successfully filled 2 block(s)`;
zombie `beb75a8b` at 10. **0 PLAYERHIT** in the row; probe `100.000/100.000`. The next row's `CLEAN blocks=2` put the
two blocks back.

**KB7 — NOT AS PREDICTED, ONE LINE: THE LAST MANA READING.** `MANA current=100.000`; press 1; `REPLY "The game time is
20375405 tick(s)"`; `PLAYERHIT … sent=63.000 crit=false`; press 2; **`REPLY "On cooldown for 0.4s" [gray]`**;
`MANA current=89.000`; `REPLY "The game time is 20375445 tick(s)"`; press 3, no refusal; `MANA current=76.400`;
`PLAYERHIT … sent=63.000 crit=false`; `REPLY "The game time is 20375453 tick(s)"`; probe `customHP=0.000/100.000 …
dead=true`.
- **`g2 - g1 = 40` and `g3 - g1 = 48`, as predicted.** The refusal at 40, its `0.4s`, two PLAYERHITs, and the dead
  zombie: as predicted. (The REPLY's text reads "The game time is <g> tick(s)", not the predicted "The time is <g>";
  the predicted figures are what the row is judged on, and they match.)
- **`x = 89.000` = 100 - 13 + 2.0**: as predicted ("M - 13 plus 40 ticks of regeneration"; the 40 ticks gave 2.0).
- **After press 3: `76.400`, predicted `x - 13` = `76.000` "to within the regeneration of the same tick: none".** The
  `89.000` was read at press 2 and press 3 came 8 ticks later, so 8 ticks of regeneration sit between the two
  readings: at the rate the 40 ticks show (2.0 per 40), that is **0.4, exactly the difference**. **The prediction left
  out that term; the reading is consistent with the mechanism.** Not a defect of the build.

**CS2R2 — AS PREDICTED.** `PLAYERHIT … source=crystal_shard_staff/right_click target=fd93a336-… zombie element=kinetic
sent=16.000 crit=false triggerScore=100`; probe `customHP=84.000/100.000`. Against CS2's 65.000 at level 50, the 49
was the level bonus, as the seat ruled.

**HF1R2 — UNREAD: THE HELD-FIRE SAMPLE IS THE KNELL BEAM'S, NOT THE STAFF'S.** The row gave and held the staff (`DRIVE
hold PlayerItemHeldEvent+setHeldItemSlot 0->0 item=crystal_shard_staff`). The row's first `rpg firerate` printed and
cleared an earlier sample (`crystal_shard_staff … INPUTS count 57 … min 3t`, `FIRES count 22 … min 14t`, `MIXED -- the
weapon changed mid-sample`), the selftest's own presses across rows, mixed, so not a reading. **He held right-click
(the run's chat shows a stream of `On cooldown for 2.2s` … `0.2s` refusals, the Knell Beam's 48-tick cooldown), but the
second `rpg firerate` reads:**
`FIRERATE player=BaronVonYeetus Fire cadence -- knell_beam, right_click`;
`  INPUTS  count 59   window 232t   mean 4.00t    min 4t`;
`  FIRES   count 5    window 192t   mean 48.00t   min 48t`;
`  cooldown_ticks 48 authored, 48 effective (not a basic attack -- attack speed does not scale it)`;
`  COOLDOWN-LIMITED -- fires slower. …`; `Sample cleared.` Both copies of each line are in the log (`FIRERATE
player=…` and the selftest REPLY), as PR 1 predicts. **So the sample is on the Knell Beam (slot 1), not the staff the
row held (slot 0).** How the hand moved to slot 1 is NOT READ: no instrument logs a player's own slot change. Ask Ben.
**By the gate's table, which is for the 14-tick staff: UNREAD.** HF1 is owed again. **Recorded, not judged:** on the
beam (48 authored, a multiple of 4), `INPUTS min 4`, `FIRES min 48` and `mean 48.00`. It cannot tell the
`ceil(n/4) x 4` question apart, because 48 is already on the grid.

> **HF1R2, BEN'S ANSWERS, 2026-11-22 (appended after the run, on `feat/lantern-owed-fixes`; no word above removed, no
> prediction edited).** The seat asked Ben two questions, relayed verbatim:
> (1) "Before you ran the selftest, did you fire the Crystal Shard Staff by holding right-click?"
> (2) "During 'hold right-click', did you switch to the Knell Beam yourself, or was it already in your hand?"
> **Ben, verbatim, 2026-11-22:** "1. Yes I fired the Crystal shard staff by i was spamming right click not holding
> 2. Yes I Did I thought the test wanted the knell beam not the crystal shard staff"
> - **The seat's count:** 9 `use:` steps precede HF1R2 (KB2 1, KB3 1, KB4 1, KB5 1, KB6 1, KB7 3, CS2R2 1), so the
>   cleared sample's 22 fires were not the selftest's; by answer 1 they were his own CLICKED presses before the run,
>   mixed with later presses.
> - **The seat's reading:** that sample (`crystal_shard_staff`, `INPUTS min 3t`, `FIRES min 14t`, `MIXED`) is **clicked
>   input, not held; no verdict on ceil(n/4) x 4.** It is NOT the owed held-fire reading. Recorded as an observation:
>   the 14-tick cooldown permits fires 14 ticks apart under clicked input.
> - **By answer 2, HF1R2's cause is the gold line not naming the weapon.** The owed fixes are carried into
>   `feat/lantern-owed-fixes` (a per-weapon sample, a hotbar holding only the named weapon, the gold line naming it),
>   and the reading is owed as HF1R3 in `GATE-living-lantern.md`.

**KB8 — AS PREDICTED.** Six MOBSEEDs (`21d83494` 8,0; `69caa519` 12,1.5; `f86b6a30` 16,-1; `9ad01c42` 20,3;
`990397ef` 10,-4; `eeb1eef5` 25,0.6); the TELL. **12 PLAYERHIT lines, all `source=knell_beam/right_click
element=void`: 11 at `sent=63.000 crit=false`, 1 at `sent=126.000 crit=true` (on `990397ef`, 10:44:31); every one
`triggerScore=100`.** Pairs in the same second (`21d83494` + `eeb1eef5`, `69caa519` + `9ad01c42` twice) are one shot
piercing two bodies (INFERRED from the timestamps; the log has no tick). All six zombies were removed (`MOBREMOVE`
x6). Two `Not enough mana: 13 needed, 6 available` refusals near the end. **`ASK 1 question="Could you tell where the
beam would land, and did it need a careful aim?" answer="yes" ticks=210`**: his answer, verbatim, **`yes`**. It is
STATED. It is one word to a two-part question, so which half it answers is not read.

**The restore:** `SELFTEST RUN RESTORED xp=712667 cell=mage/fire gamemode=SURVIVAL held=0 mana=100.0 trace=false
spawn_mobs=false time=1000 blocks=2 mobsRemoved=0 orbsRemoved=21 keep_inventory=false immediate_respawn=false
difficulty=NORMAL drag_tick=false`. `xp=712667` is his lifetime XP back (level 50). `spawn_mobs=false` and
`time=1000` are what the last run's restore also printed (`2026-11-21-2.log.gz`, 16:19:44), so they are the world's
values before this run (INFERRED from the two restore lines). His health after the restore is not read (the gate
predicts it stays at its level-1 figure).

### WHAT IS UNREAD AFTER THIS RUN

- **HF1R2**, the staff's held fire: the sample was on the beam. **HF1 is owed a third time.**
- **KB2's after-hit health**, and on which tick (24 or 25) the shot's damage reaches a mob's health.
- **Everything the client renders**: the warning line, the beam, the sounds (AUTO never reads them). His `yes` is the
  only word on it.
- KB7's off-by-one arms (47, 49, 41) were not presented: the run's spacing was exact, as predicted.
