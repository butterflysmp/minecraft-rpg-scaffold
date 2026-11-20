# GATE — STEP F9d: DOWN, ALL DOWN, AND THE MODE HE ENTERED IN

**Seat approval 2026-11-19: code and gate diffed at 5c8d8a9e, booted at cb821675, readings at 453e5c9a. Merge on Ben's words.** **Status: READ, 2026-11-19, at `cb821675`; FDM1 NOT RUN, carried** (was: NOT RUN). Written before any boot of `feat/fight-down`, which has **no boot of its own**. The one R0 and run
sheet are in `GATE-fight-revive.md` (F9e, stacked on this branch), and its boot is on F9e's head. Every prediction below
is for THIS branch's code. Where F9e changes a line these rows read (the copy, the revive), `GATE-fight-revive.md`
re-predicts the row.

> **SUPERSEDED IN PART, 2026-11-19 (written on `feat/fight-revive`, before any boot).** The boot of these rows is on
> F9e's head, whose code also lays his COPY. So every FD row's prediction here stands, **plus** the copy's lines listed
> in `GATE-fight-revive.md`, section *WHAT THIS BRANCH ADDS TO `GATE-fight-down.md`'s ROWS*. The R0 and the run sheet are
> `GATE-fight-revive.md`'s. The readings go in this file's cells, as each row's own.

**Game mode:** every row is **SURVIVAL**; each row's DOWN puts him in SPECTATOR on purpose, and each row ends with him
home in survival. **Difficulty normal** (each setup sets it).

**Ben's rulings** (PLAN-dragon-fight.md section 1, 0d and 0e, verbatim):
- "When a player dies, they are thrown into spectator for the remainder of the fight or until they are respawned by
  another player."
- "Keep inventory yes."
- "When all players are defeated at once they are all in spectator mode for a few seconds and get sent back to the
  normal world and put back to survival."

**READ, 2026-11-19, 22:34 to 22:37, at `cb821675`** (`feat/fight-revive`'s head; the boot is recorded in
`GATE-fight-revive.md`). FD1 to FD4 are read in their cells. **FDM1 was NOT RUN.**

## WHAT WAS READ, EACH MARKED

- **THE MECHANISM, AND WHY IT IS THE CLEANEST.** READ, the pinned server jar, `ServerPlayer.die`: it raises
  `PlayerDeathEvent`, and if the event is CANCELLED it sets his health to the event's revive health (when it is 0 or
  less) and RETURNS. That is before the death game event, before the drops and before the death screen. So cancelling
  the death is the one route with no screen to click through, nothing dropped and nothing respawned.
  - The other routes were not taken. A respawn, even immediate, still kills him and runs every respawn side effect.
    Spectator-before-death would mean catching every damage path before it is lethal.
  - `EntityDeathEvent` is `Cancellable` and has `setReviveHealth` (READ, javap on the pinned `paper-api`).
- **The only path that kills a player is ours:** `PlayerHealthSystem.onChange` calls `setHealth(0)` when the store
  reaches zero (READ, our source). So his store sits at 0 when he goes down; going down heals it to his maximum
  (`HealthState.heal` has no dead-stays-dead guard; READ).
- **A SPECTATOR IS HURT BY THE VOID.**
  - READ: `Player.hurtServer` refuses a hit to an invulnerable player only if the source is not
    `bypasses_invulnerability`, and that tag lists `minecraft:out_of_world` (READ, the jar's data).
  - So a down player takes NO damage of any kind (an `EntityDamageEvent` cancel at LOWEST; every pricing handler ignores
    a cancelled event, READ, our source).
  - One who went down below the floor is lifted to C + 8 (CC's placeholder).
- **A spectator is already no participant:** `ArenaSite.takesPart` asks survival or adventure (READ, our source). DOWN
  is now also asked by name, and the fight's damage (`applyDamageIf`) asks `takesPart`, so it never lands on him.
- **The ring, the boss bar and the chat went to participants only** (READ, our source). A down player now sees all
  three (the brief's b), through `BukkitFightPort.downWatchers`.
- **A kill was credited to `getKiller()` with no participant check** (READ). A down player's kills are now impossible.
- **The return's mode:** `FightReturn` gains `entryMode`, saved through `PlayerRepository` on entry. A file without
  it loads with none, and `restoreMode()` reads none (and anything not survival, adventure or creative) as SURVIVAL:
  **the migration is that rule, and no step is run** (`FightReturnProfileTest`).

## THE ARITHMETIC (SELFTEST SAFETY): EVERY ROW PUTS HIM DOWN ON PURPOSE

| row | what can hurt him | the most it does | against his health |
|---|---|---|---|
| FD1 | `/kill` | **lethal, on purpose** | he goes DOWN, not dead |
| FD2 | the void | **lethal, on purpose** | DOWN |
| FD3 | the powered kill, 10 x his maximum, defence bypassed (AE5R2's staging) | **lethal, on purpose** | DOWN |
| FD4 | `/kill` | **lethal, on purpose** | DOWN |

- **Nothing else can hurt him in these rows.** FD1, FD2 and FD4 are `enter nodragon` with no Mage, so every strike
  finds him inside the ring and deals 0. FD3's strikes are AE5R2's, which read 0 while he stood by the Mage.
- **Once down he takes nothing at all.**
- Every row sets `death_rules` and heals first. **"Down" replaces "dead" in every one**, and each row ends with him
  home, in survival, healed (`rpg heal 1000000`).

> **CORRECTED BY THE READING, 2026-11-19 (the boot at `cb821675`): FD2 IS NOT SAFE FROM THE RING.** FD2's `tp` puts him
> at x 60, outside the r=48 ring.
> - `STRIKE … outside` hit him at t=40, 80, 120 and 160: 20, 40, 80 and 160, or 272.727 after defence.
> - The void put him down at t=186, 14 ticks before strike k=5 (320, 290.909 after defence) would have.
> - **So FD2's down is the void's only because the void came first.** A slower void would make it the lightning's.
>
> The line above is not edited; this note corrects it. The full account is under *WHAT IT FOUND* in
> `GATE-fight-revive.md`.

> **THE SEAT'S OWN MISS, 2026-11-19 (the seat's words, recorded at its request).** The seat accepted "nothing else
> hurts him" for FD2 without checking that x 60 is outside the 48-block ring. Four strikes dealt 273 of his 350.

### FD1 — A SOLO DEATH INSIDE: DOWN, SPECTATOR, ALL DOWN, FAILED, FIVE SECONDS, HOME IN SURVIVAL (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The birth, as F9c's: `FIGHTWORLD end-config …`, `created … environment=THE_END`, `end-check … inert=true …`, `arena built`, `FIGHT start … site=arena`, and **`FIGHTWORLD entered player=BaronVonYeetus world=<n> return=world <x y z> mode=SURVIVAL`** (new: the mode). No dragon. (2) At `kill @s`: **`FIGHT down player=BaronVonYeetus t=<t> cause=<c> at=<dx dy dz>`**, at about `0.00 0.00 0.00` (the entry is C). c is INFERRED `KILL`; read. Whether a `HURT … dealer=self` line comes first (our reroute of the kill) is not predicted. **No `FIGHTWORLD returned … why=died`, no respawn.** (3) A tick later **`FIGHT down spectating player=BaronVonYeetus mode=SPECTATOR`**, and no `lifted` line. (4) The next fight tick **`FIGHT all-down t=<t'> down=[BaronVonYeetus] outcome=RUNNING returns_in=100`**, t' = t or t+1. (5) At t'+100, **`FIGHT all-down return t=<t'+100> outcome=RUNNING`**, `FIGHT ended reason=all-down-FAILED`, `ARENAPROBE when=end … fire=0 … dragons=0`, **`FIGHTWORLD mode player=BaronVonYeetus from=SPECTATOR to=SURVIVAL why=end`**, `FIGHTWORLD returned … to=world <x y z> why=end`, `unloaded`, `deleting`, `deleted … entries=<e>`. (6) The await ends; `Custom HP: 350/350`; `FIGHTWORLDS loaded=0 folders=0 marked=0 you=world`. **No SELFTEST abort and no respawn wait** | `DownRosterTest`, `FightDownSourceTest` *aDeathInsideIs…*, *allDownIsChecked…*, *theEntryMode…* | **AS PREDICTED, AND `all-down` WAS LOGGED ONE LINE BEFORE `spectating`.** (Read 2026-11-19 at `cb821675`, the boot of `GATE-fight-revive.md`.) (1) `end-config`, `created … environment=THE_END`, `end-check … inert=true`, `arena built`, `FIGHT start … site=arena`, **`FIGHTWORLD entered player=BaronVonYeetus world=rpgfight_fight_5327b9cc return=world -106.51 63.00 21.43 mode=SURVIVAL`**; no `FIGHT dragon` line. (2) **`FIGHT down player=BaronVonYeetus t=16 cause=KILL at=0.00 0.00 0.00`**; no `HURT` line in the row; no `returned … why=died`, no respawn. (3) `FIGHT down spectating player=BaronVonYeetus mode=SPECTATOR`, no `lifted`. **It was logged after (4)'s `all-down`, not before it.** (4) **`FIGHT all-down t=17 down=[BaronVonYeetus] outcome=RUNNING returns_in=100`** (t + 1). (5) **`FIGHT all-down return t=117 outcome=RUNNING`**, `FIGHT ended reason=all-down-FAILED`, `ARENAPROBE when=end … fire=0 … crystals=0 dragons=0`, **`FIGHTWORLD mode … from=SPECTATOR to=SURVIVAL why=end`**, `returned … to=world -106.51 63.00 21.43 why=end`. Then, not in the prediction, `FIGHTWORLD unload-retry name=rpgfight_fight_5327b9cc players=1 attempt=1`. Then `unloaded`, `deleting`, `deleted …fight_5327b9cc entries=17`. (6) `AWAIT fight ended ticks=101`; `Custom HP: 350/350`; `FIGHTWORLDS loaded=0 folders=0 marked=0 you=world`; `DONE ticks=186`, no abort. **F9e's lines:** `FIGHT copy laid player=BaronVonYeetus t=16 at=0.00 0.00 0.00 died=0.00 0.00 0.00 moved=false revivable=true`; every `COPYTICK` `inside_by=48.000`; `FIGHT copy removed player=BaronVonYeetus t=117 why=end` before `FIGHT ended` |

### FD2 — A DEATH IN THE VOID: DOWN, LIFTED ABOVE THE FLOOR (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The birth as FD1's. (2) After the `tp` to y -130, below the End world's void line (READ at F9c: an END world's void is at -64): **`FIGHT down player=BaronVonYeetus t=<t> cause=VOID at=<about 60.00> <below -129> <about 0.00>`**. (3) A tick later **`FIGHT down lifted player=BaronVonYeetus (below the floor) to C +8.0`**, then **`FIGHT down spectating … mode=SPECTATOR`**. **No damage line on him after the down line** (the LOWEST cancel). (4) and (5) as FD1's: `all-down`, five seconds, `all-down return`, `from=SPECTATOR to=SURVIVAL why=end`, `returned … why=end`, the world gone | `FightDownSourceTest` *aDeathInsideIs…* (the lift), *aDownPlayerTakesNoDamage…* | **EVERY PREDICTED LINE PRESENT. NOT IN THE PREDICTION: THE RING'S LIGHTNING HIT HIM FOUR TIMES BEFORE THE VOID PUT HIM DOWN** (see the dated note under *THE ARITHMETIC*). (1) The birth as FD1's, `entered … mode=SURVIVAL`. (2) After `/tp @s ~60 -130 ~`: **`FIGHT down player=BaronVonYeetus t=186 cause=VOID at=60.00 -671.72 0.00`**. (3) `FIGHT down lifted player=BaronVonYeetus (below the floor) to C +8.0`, then `spectating … mode=SPECTATOR`. **Both were logged after `FIGHT all-down t=187`**, as in FD1. **No damage line after the down line.** (4)(5) `all-down t=187 … RUNNING returns_in=100`; `all-down return t=287`; `ended reason=all-down-FAILED`; `mode … from=SPECTATOR to=SURVIVAL why=end`; `returned … why=end`; `unload-retry … attempt=1`; `deleted …fight_48f37d92 entries=17`. **Before the down:** `STRIKE t=40 k=1 damage=20.000 outside`, `t=80 k=2 40.000`, `t=120 k=3 80.000`, `t=160 k=4 160.000`, each with a `HURT before=<the same> after=18.182 / 36.364 / 72.727 / 145.455`, 272.727 in all. **He was at x 60, outside r=48.** **F9e's lines:** **`FIGHT copy laid player=BaronVonYeetus t=186 at=47.00 0.00 0.00 died=60.00 -671.72 0.00 moved=true revivable=true`**; 25 `COPYTICK`, every one `inside_by=1.000`; `copy removed … t=287 why=end` |

### FD3 — POWERED: EVERY PARTICIPANT DOWN, THE SAME ENDING (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) AE5R2's lines (GATE-fight-arena.md, read at `fce1f48a`): the landing, the slow close, the 68 kills each holding the ring open, the ten crystals. Every fight tick is shifted by the same amount s, READ: an End world starts its fight 4 ticks after `enter` and the first wait is 20, not 5. (2) **`FIGHT FAILED_POWERED t=<P>`** and the failure's chat line; **`FIGHT powered-kill t=<P> player=BaronVonYeetus amount=3500.000`** (10 x 350). (3) A tick later the kill lands: `HURT before=3500.000 after=3500.000 …` (defence bypassed), then **`FIGHT down player=BaronVonYeetus t=<P+1 or so> cause=<c> …`** and `spectating`. (4) **`FIGHT all-down t=<t'> down=[BaronVonYeetus] outcome=FAILED_POWERED returns_in=100`**. (5) 100 ticks later the ending as FD1's, outcome `FAILED_POWERED` | AE5R2; `FightDownSourceTest` *allDownIsChecked…* | **AS PREDICTED** (AE5R2's tick values not compared line by line). (1) `FIGHT land t=31 … result=STARTED`, `FIGHTPHASE t=31 IDLE->FAST_CLOSE`, `FIGHTPHASE t=221 FAST_CLOSE->SLOW_CLOSE`; **68 `FIGHTKILL`**, **10 `CRYSTAL`**; 65 `STRIKE`, **every one `k=0 damage=0.000 inside`**. (2) `FIGHTPHASE t=2621 SLOW_CLOSE->POWERED`, **`FIGHT FAILED_POWERED t=2621`**, the chat line "The fight has failed: FAILED_POWERED."; **`FIGHT powered-kill t=2621 player=BaronVonYeetus amount=3500.000`**. (3) **`HURT before=3500.000 after=3500.000 defense=10.000 dealer=self`**, then **`FIGHT down player=BaronVonYeetus t=2622 cause=KILL at=-2.00 0.00 0.00`** and `spectating`. (4) **`FIGHT all-down t=2623 down=[BaronVonYeetus] outcome=FAILED_POWERED returns_in=100`**, logged before `spectating`. (5) `all-down return t=2723 outcome=FAILED_POWERED`, `ended reason=all-down-FAILED`, `mode … from=SPECTATOR to=SURVIVAL why=end`, `returned … why=end`, `unload-retry … attempt=1`, `deleted …fight_b3d82c8f entries=17`. **F9e's lines:** `copy laid player=BaronVonYeetus t=2622 at=-2.00 0.00 0.00 died=-2.00 0.00 0.00 moved=false revivable=true`; `copy removed … t=2723 why=end`. **The row ran FOURTH**, after FD4 (the scenario file's order) |

### FD4 — A DOWN SPECTATOR LEAVES THE FIGHT WORLD: OUT AND RETURNED AT ONCE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) As FD1's to `all-down … returns_in=100`. (2) Twenty ticks after the kill, the teleport into the overworld: **`FIGHTWORLD left player=BaronVonYeetus world=<n> why=spectator-left to=world`**, **`FIGHTWORLD mode player=BaronVonYeetus from=SPECTATOR to=SURVIVAL why=spectator-left`**, **`FIGHTWORLD returned … to=world <x y z> why=spectator-left`**. (3) With nobody inside, on the next fight tick: **`FIGHT nobody inside t=<t''> outcome=RUNNING`** and `FIGHT ended reason=nobody-inside-RUNNING`, **before** the all-down return would have come. `ARENAPROBE when=end`; the world gone. (4) No second `returned` line | `FightDownSourceTest` *aDownSpectatorLeaving…* | **AS PREDICTED.** (1) As FD1's: `down … t=16 cause=KILL at=0.00 0.00 0.00`, `all-down t=17 … returns_in=100`, `spectating`. (2) At t=36, 20 ticks after the kill: `FIGHT copy removed player=BaronVonYeetus t=36 why=out`, **`FIGHTWORLD left player=BaronVonYeetus world=rpgfight_fight_c51e0227 why=spectator-left to=world`**, **`FIGHTWORLD mode … from=SPECTATOR to=SURVIVAL why=spectator-left`**, **`FIGHTWORLD returned … to=world -106.51 63.00 21.43 why=spectator-left`**. (3) **`FIGHT nobody inside t=37 outcome=RUNNING`**, `FIGHT ended reason=nobody-inside-RUNNING`, with no `all-down return`; `ARENAPROBE when=end`; `deleted …fight_c51e0227 entries=17`. (4) One `returned` line. **F9e's lines:** `copy laid … t=16 … moved=false revivable=true`; **`copy removed … why=out` before `FIGHT nobody inside`** |

### FDM1 — THE MANUAL ROW: DOWN, A QUIT, AND THE STALE LOGIN (Ben, after the selftest; SURVIVAL)

He enters (`/rpg fight enter nodragon`), walks off the disc's edge into the void, and **disconnects as soon as he is
spectating**, then reconnects after ten seconds. Like AQ1 (GATE-fight-arena.md).

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT down … cause=VOID …`, `lifted`, `spectating … mode=SPECTATOR`; `FIGHT all-down …` the next tick. (2) His quit: **`FIGHTWORLD left player=BaronVonYeetus world=<n> why=quit return=kept`**; then **`FIGHT nobody inside …`**, `FIGHT ended reason=nobody-inside-<outcome>` (or, if he took longer than five seconds, `all-down-FAILED` with him already home: then the row is not read as designed, and says so); the world unloaded and deleted, him offline. (3) His login: vanilla puts him at the same x, y, z of the overworld (READ at AQ1), **still in spectator** (his saved mode). **`FIGHTWORLD stale login caught player=BaronVonYeetus at=world <x y z> to=world <his return>`**, **`FIGHTWORLD mode player=BaronVonYeetus from=SPECTATOR to=SURVIVAL why=login`**, `FIGHTWORLD returned … why=login`. (4) He stands at his return location in survival | `FightDownSourceTest` *theEntryMode…* (the login route) | **NOT RUN** (2026-11-19, the boot at `cb821675`). Ben typed `/stop` at 22:42:40, 24 s after `SELFTEST RUN FINISHED` for fight-revive (22:42:16). He typed no `/rpg fight enter nodragon`, and there was no quit and no login before it. No reading |

> **FDM1'S CAUSE, RE-PREDICTED BEFORE ITS FIRST READING (2026-11-20, the seat's ruling 3 at the stack's boot
> approval).** FDM1 has never been read, so its cause may be rewritten, and only its cause is. **The text above is not
> edited**; this note replaces clause (1)'s `cause=VOID` and nothing else.
>
> - **Why:** walking from C to the disc's edge (56) takes him over the ring (48). Outside it, the ring's strikes find him
>   (20, then doubling, every 40 ticks; READ at FD2, `cb821675`) before and while he falls. Below the End void line
>   (y -64), Ben's void rule takes a tenth of his maximum every 10 ticks. **Whichever lands the last of his bar puts him
>   down.**
> - **The prediction:** `FIGHT down player=BaronVonYeetus t=<t> cause=<VOID or KILL> at=<x y z>`. KILL is how a ring
>   strike's down read at RV4 (`cause=KILL`). Read as `VOID` if the last HURT before the down ends `cause=void`, as
>   `KILL` if it was a strike's.
> - **Where his copy lies follows from it, by `CopyPlace`:**
>   - down outside the ring, on the disc or over the void: one block inside the edge, `at=<about 47 along his line> 0.00
>     <…> moved=true revivable=true`;
>   - down inside it: where he went down, `moved=false`.
> - **Everything else in (1) to (4) stands as written.** That includes the quit, the stale login's `mode … from=SPECTATOR
>   to=SURVIVAL why=login`, and him home in survival.
> - **The reading of this boot goes beside this note**, in the boot's after-run section of `GATE-loot-roll.md` and in a
>   dated line here.

## THE MERGED DEATH ROWS, RE-PREDICTED (the brief's g): NOT RE-RUN; FD1 TO FD4 STAND FOR THEM

**Every arena row that killed him now puts him DOWN.** Each one's death line, respawn and "returned … why=died" become
the four lines below, and its ending becomes ALL DOWN's:
1. `FIGHT down …`;
2. `spectating`;
3. `FIGHT all-down …`;
4. five seconds later `FIGHT all-down return`, `ended reason=all-down-FAILED`, `mode … from=SPECTATOR to=SURVIVAL
   why=end`, `returned … why=end`.

Their other lines are unchanged. Re-predicted under the next suffix:

### AE4R3 — CLOSED: the lightning puts him down (was AE4R2)
The strike that killed him puts him down. `FIGHT all-down … outcome=FAILED_CLOSED`, then the ending above, **instead
of** `FIGHT nobody inside`. Not re-run: FD1 stages the same down by `/kill`.

### AE5R3 — POWERED (was AE5R2)
FD3 is this row, with its first wait corrected for the End world (F9c), which AE5R2 never was.

### AE6R3 — the void (was AE6R2)
FD2 is this row.

### BA3R3 — the play row with no safety (was BA3R2, Ben's)
A death in it is a down, with ALL DOWN's ending, instead of "he reappears where he entered". Not re-run.

### DA8R2 — a failure with the dragon alive (was DA8)
As AE4R3, and the dragon is removed at the ending (`FIGHT dragon removed reason=all-down-FAILED`). Not re-run.

### DB2R2 and EB5R3 — the fight (were DB2 and EB5R2)
A death in the fight is a down. Solo, that is ALL DOWN, and he goes home in survival five seconds later. **F9e's
revive does not change this solo**: nobody alive can revive him. Not re-run here; the fight is not on this boot's
sheet.

## THE SELFTEST AND SPECTATOR MODE (the brief's h; as plan 6.7 did for a fight world)

- **The guard reads no game mode.** `SelfTestStaging.invalid` reads online, dead, which world, and how many are online.
  A down player is alive (his death was cancelled), so it cannot tell him from a living one, and nothing aborts.
- **No step reads his mode.** The rows' steps are cmd, wait, await_fight, death_rules and probe_entities. They run
  through `performCommand` and the run's own pollers.
- **No respawn wait is reached:** that wait asks `isDead`, and he never is.
- **The run's end restores the mode its start saved** (`SelfTestSnapshot`), which the selftest's own guard requires to
  be survival.
- **The walk** (`SelfTestDownWalkTest`):
  - FD1, FD2, FD3 and FD4's real steps, read from the jar's scenario file, walked with the DOWN model. He goes down
    alive in the fight's world, and the fight's end (or FD4's teleport) brings him home.
  - No step aborts.
  - **The control:** the run's-world-only guard aborts right after the enter, so the walk is not blind.

## WHAT THIS GATE DOES NOT READ

- **A second player.** ALL DOWN with two or three is read only in `DownRosterTest`; on a server, every row is solo.
- **Spectators are otherwise unconfined** (the brief's e, SEAT PLACEHOLDER, flagged): a down player may fly anywhere in
  the fight's world; only leaving it takes him out.
- **What he sees:** that a down player sees the ring, the bar and the chat is wired (`FightDownSourceTest`) and is
  not read by any row here. GATE-fight-revive.md's Ben rows ask.

## THE NEXT BOOT'S ROWS: FD2R2, AE6R4 AND THE PREDICTION FIXES (THE SEAT'S PART 2 AND BEN'S VOID RULING, 2026-11-19)

**Status: NOT RUN.** Written on `feat/fight-template` before any boot of it. The boot is the stack's one boot, whose R0
and run sheet are in `GATE-loot-roll.md`. **No prediction above is edited.** FD2 was read at `cb821675` and is retired
from the scenario file; FD2R2 is in its place. FDM1's prediction above is unchanged; it moves to the first line of the
next run sheet.

**Ben's void ruling** (PLAN-world-damage.md section 1, verbatim): "A player should lose 10% of their health each damage
tick (0.5s) to the void regardless of their max health." Built on this branch (`VoidShare`, `world_damage.yml`, the
player arm of `onEnvironmentalDamage`). The account, with the READ marks, is `PLAN-world-damage.md`.

### FD2R2 — A DEATH IN THE VOID UNDER BEN'S RULE, INSIDE THE RING'S FOOTPRINT (SURVIVAL; DOWN ON PURPOSE)

**Restaged from FD2:** `tp @s ~ -130 ~` from C, straight down, so he is at x 0, z 0, inside the 48-block ring, and no ring
strike can reach him. FD2's `~60` put him outside it (the seat's own miss, the note under *THE ARITHMETIC*). **FD2R2 is
also the row that reads Ben's rule on a server.**

**His maximum at the row is M**, read from the heal's reply `Custom HP: M/M` (350 at the last boot). Every figure below
is stated as a share of M, then at 350.

| prediction | stands on | READING |
|---|---|---|
| (1) The birth as FD1's, `FIGHTWORLD entered … mode=SURVIVAL`. (2) After the `tp`, he is already below the End world's void line (y -64, READ, PLAN-world-damage.md section 2). **One `HURT before=<M/10> after=<M/10> defense=<his> dealer=self dealerType=self cause=void` per void tick: 35.000 at 350, `before` equal to `after` (defence bypassed).** The ticks are 10 server ticks apart (READ). The HURT line carries no tick, so the spacing is read as the count between the `tp` and the down. (3) **TEN such lines of exactly M/10, then an ELEVENTH, `before=<r> after=<r> … cause=void`, where r is what regeneration paid between the first and the eleventh tick** (a few points; INFERRED from `HealthRegen`: about four or five payments of 1.0 when saturated, 0.2 when not, plus his regeneration stat). **DOWN on the ELEVENTH, not the tenth:** see the finding in PLAN-world-damage.md section 5. (4) **`FIGHT down player=BaronVonYeetus t=<d> cause=VOID at=0.00 <below -129> 0.00`**, d about 100 ticks after the first void tick. (5) **`FIGHT copy laid player=BaronVonYeetus t=<d> at=0.00 0.00 0.00 died=0.00 <y> 0.00 moved=false revivable=true`**: a death inside the ring lays the copy where he died, on the floor (READ, `CopyPlace`: `moved` is horizontal only). The seat's "one block inside the ring" is the rule for a death OUTSIDE it (FD2's). Then COPYTICK `inside_by=48.000`. (6) In this order, as read at the last boot (the fix below): `FIGHT all-down t=<d+1> … outcome=RUNNING returns_in=100`, `FIGHT down lifted … to C +8.0`, `FIGHT down spectating … mode=SPECTATOR`. **No HURT after the down line.** (7) The ending as FD1's, the copy removed `why=end`, `FIGHTWORLD mode … from=SPECTATOR to=SURVIVAL why=end`, and an `unload-retry … players=1 attempt=1` before `unloaded`. (8) Every `STRIKE … k=0 damage=0.000 inside` | `VoidShareTest`, `VoidRuleSourceTest`, `WorldDamageLoaderTest`; FD2's reading (the lift, the copy) | **AS PREDICTED (2026-11-20, the stack's boot at `5c2dee2c`, 10:44:27 to 10:44:41).** (1) The birth: `FIGHTWORLD entered … world=rpgfight_fight_966a7078 return=world -105.99 63.00 22.87 mode=SURVIVAL`, with M read from the heal's `Custom HP: 350/350`. (2)(3) **Eleven `HURT … dealer=self dealerType=self cause=void`, `defense=10.000`, `before` equal to `after` in every one: TEN of `35.000` (M/10 exactly) and an ELEVENTH of `10.000`**, the remainder regeneration paid. **The ticks, bracketed between `RINGTICK` lines 4 apart:** the hits fall in (16,20), (24,28), (36,40), (44,48), (56,60), (64,68), (76,80), (84,88), (96,100), (104,108), and after 116. That fits one cadence of 10, hits at about t=17, 27 … 117. (4) **`FIGHT down player=BaronVonYeetus t=118 cause=VOID at=0.00 -423.81 0.00`, DOWN ON THE ELEVENTH.** (5) **`FIGHT copy laid player=BaronVonYeetus t=118 at=0.00 0.00 0.00 died=0.00 -423.81 0.00 moved=false revivable=true`**, then 25 `COPYTICK … at=0.000 0.000 r=48.000 … inside_by=48.000 revivable=true`. (6) In order: `FIGHT all-down t=119 … outcome=RUNNING returns_in=100`, `FIGHT down lifted … to C +8.0`, `FIGHT down spectating … mode=SPECTATOR`. **No HURT after the down line.** (7) `all-down return t=219`, `copy removed … t=219 why=end`, `ended reason=all-down-FAILED`, `FIGHTWORLD mode … from=SPECTATOR to=SURVIVAL why=end`, `returned … why=end`, **`unload-retry name=rpgfight_fight_966a7078 players=1 attempt=1`**, `unloaded`, `deleted …fight_966a7078 entries=17`. (8) **Both strikes, t=40 and t=80, `k=0 damage=0.000 inside`: no strike landed.** `DONE ticks=288`; RESTORED `gamemode=SURVIVAL` |

**The arithmetic (at defence 0, against 100, and where he is):** inside the ring, so every strike deals 0. The void
takes 10 a tick, defence bypassed, so it is **lethal on purpose** at the eleventh tick (ten of 10, then what regeneration
paid). **Down, not dead.** Nothing else can reach him at y -130.

### AE6R4 — the void (was AE6R3; AE6R2's staging, `GATE-fight-arena.yml`)

AE6R2 put him at x 60, y -130: **outside the ring, below the void line.** Under Ben's rule, two clocks of ours now
hurt him there:
- the void's M/10 every 10 ticks, defence bypassed;
- the ring's strikes from t=40 (20 then doubling, every 40), defence applied.

The first to take the last of his bar puts him down, with `cause=` the cause of that hit. INFERRED for M=350, at defence
10 and with the first void tick at the `tp`:
- eight void ticks (280) and the strikes at t=40 and t=80 (18.182 + 36.364) leave him about 15 short by t=87;
- the ninth void tick takes the rest.

So he is down about 80 ticks after the `tp`, near half FD2's 166, and `cause=VOID`. Both are lethal on purpose. **Not re-run:** FD2R2 reads the void and FD2's reading stands
for the strikes outside.

### THE PREDICTION FIXES (the seat's item b): no behaviour change, each read at the last boot

1. **`FIGHT all-down` is logged before `lifted` and `spectating`, in every down** (all six at `cb821675`, each on the
   same tick). FD1 (3)/(4) and FD2 (3) wrote the other order. From the next boot every prediction reads: `down`,
   `copy laid`, `all-down`, `lifted` (if below the floor), `spectating`.
2. **`FIGHTWORLD unload-retry name=<n> players=1 attempt=1` is predicted on every ALL-DOWN ending.**
   - READ, `FightWorlds.unloadAttempt`: if the world still lists a player, it does not unload; it logs `unload-retry`
     and tries again a moment later (`later(...)`), up to `MAX_ATTEMPTS`. After that, the next startup's sweep gets the
     folder.
   - On an all-down ending the return teleport and the unload run in the same tick. So the world still lists him once
     (INFERRED: the cross-world move is not yet reflected in `World.getPlayers()`).
   - **Harmless, READ from the log:** all five such endings at `cb821675` were followed within a second by `unloaded`,
     `deleting` and `deleted … entries=17`.
   - An ending where he has already left (FD4, RV1-RV3) has no retry.
