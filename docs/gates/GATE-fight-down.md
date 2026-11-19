# GATE — STEP F9d: DOWN, ALL DOWN, AND THE MODE HE ENTERED IN

**Status: NOT RUN.** Written before any boot of `feat/fight-down`, which has **no boot of its own**. The one R0 and run
sheet are in `GATE-fight-revive.md` (F9e, stacked on this branch), and its boot is on F9e's head. Every prediction below
is for THIS branch's code. Where F9e changes a line these rows read (the copy, the revive), `GATE-fight-revive.md`
re-predicts the row.

**Game mode:** every row is **SURVIVAL**; each row's DOWN puts him in SPECTATOR on purpose, and each row ends with him
home in survival. **Difficulty normal** (each setup sets it).

**Ben's rulings** (PLAN-dragon-fight.md section 1, 0d and 0e, verbatim):
- "When a player dies, they are thrown into spectator for the remainder of the fight or until they are respawned by
  another player."
- "Keep inventory yes."
- "When all players are defeated at once they are all in spectator mode for a few seconds and get sent back to the
  normal world and put back to survival."

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

### FD1 — A SOLO DEATH INSIDE: DOWN, SPECTATOR, ALL DOWN, FAILED, FIVE SECONDS, HOME IN SURVIVAL (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The birth, as F9c's: `FIGHTWORLD end-config …`, `created … environment=THE_END`, `end-check … inert=true …`, `arena built`, `FIGHT start … site=arena`, and **`FIGHTWORLD entered player=BaronVonYeetus world=<n> return=world <x y z> mode=SURVIVAL`** (new: the mode). No dragon. (2) At `kill @s`: **`FIGHT down player=BaronVonYeetus t=<t> cause=<c> at=<dx dy dz>`**, at about `0.00 0.00 0.00` (the entry is C). c is INFERRED `KILL`; read. Whether a `HURT … dealer=self` line comes first (our reroute of the kill) is not predicted. **No `FIGHTWORLD returned … why=died`, no respawn.** (3) A tick later **`FIGHT down spectating player=BaronVonYeetus mode=SPECTATOR`**, and no `lifted` line. (4) The next fight tick **`FIGHT all-down t=<t'> down=[BaronVonYeetus] outcome=RUNNING returns_in=100`**, t' = t or t+1. (5) At t'+100, **`FIGHT all-down return t=<t'+100> outcome=RUNNING`**, `FIGHT ended reason=all-down-FAILED`, `ARENAPROBE when=end … fire=0 … dragons=0`, **`FIGHTWORLD mode player=BaronVonYeetus from=SPECTATOR to=SURVIVAL why=end`**, `FIGHTWORLD returned … to=world <x y z> why=end`, `unloaded`, `deleting`, `deleted … entries=<e>`. (6) The await ends; `Custom HP: 350/350`; `FIGHTWORLDS loaded=0 folders=0 marked=0 you=world`. **No SELFTEST abort and no respawn wait** | `DownRosterTest`, `FightDownSourceTest` *aDeathInsideIs…*, *allDownIsChecked…*, *theEntryMode…* | |

### FD2 — A DEATH IN THE VOID: DOWN, LIFTED ABOVE THE FLOOR (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The birth as FD1's. (2) After the `tp` to y -130, below the End world's void line (READ at F9c: an END world's void is at -64): **`FIGHT down player=BaronVonYeetus t=<t> cause=VOID at=<about 60.00> <below -129> <about 0.00>`**. (3) A tick later **`FIGHT down lifted player=BaronVonYeetus (below the floor) to C +8.0`**, then **`FIGHT down spectating … mode=SPECTATOR`**. **No damage line on him after the down line** (the LOWEST cancel). (4) and (5) as FD1's: `all-down`, five seconds, `all-down return`, `from=SPECTATOR to=SURVIVAL why=end`, `returned … why=end`, the world gone | `FightDownSourceTest` *aDeathInsideIs…* (the lift), *aDownPlayerTakesNoDamage…* | |

### FD3 — POWERED: EVERY PARTICIPANT DOWN, THE SAME ENDING (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) AE5R2's lines (GATE-fight-arena.md, read at `fce1f48a`): the landing, the slow close, the 68 kills each holding the ring open, the ten crystals. Every fight tick is shifted by the same amount s, READ: an End world starts its fight 4 ticks after `enter` and the first wait is 20, not 5. (2) **`FIGHT FAILED_POWERED t=<P>`** and the failure's chat line; **`FIGHT powered-kill t=<P> player=BaronVonYeetus amount=3500.000`** (10 x 350). (3) A tick later the kill lands: `HURT before=3500.000 after=3500.000 …` (defence bypassed), then **`FIGHT down player=BaronVonYeetus t=<P+1 or so> cause=<c> …`** and `spectating`. (4) **`FIGHT all-down t=<t'> down=[BaronVonYeetus] outcome=FAILED_POWERED returns_in=100`**. (5) 100 ticks later the ending as FD1's, outcome `FAILED_POWERED` | AE5R2; `FightDownSourceTest` *allDownIsChecked…* | |

### FD4 — A DOWN SPECTATOR LEAVES THE FIGHT WORLD: OUT AND RETURNED AT ONCE (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) As FD1's to `all-down … returns_in=100`. (2) Twenty ticks after the kill, the teleport into the overworld: **`FIGHTWORLD left player=BaronVonYeetus world=<n> why=spectator-left to=world`**, **`FIGHTWORLD mode player=BaronVonYeetus from=SPECTATOR to=SURVIVAL why=spectator-left`**, **`FIGHTWORLD returned … to=world <x y z> why=spectator-left`**. (3) With nobody inside, on the next fight tick: **`FIGHT nobody inside t=<t''> outcome=RUNNING`** and `FIGHT ended reason=nobody-inside-RUNNING`, **before** the all-down return would have come. `ARENAPROBE when=end`; the world gone. (4) No second `returned` line | `FightDownSourceTest` *aDownSpectatorLeaving…* | |

### FDM1 — THE MANUAL ROW: DOWN, A QUIT, AND THE STALE LOGIN (Ben, after the selftest; SURVIVAL)

He enters (`/rpg fight enter nodragon`), walks off the disc's edge into the void, and **disconnects as soon as he is
spectating**, then reconnects after ten seconds. Like AQ1 (GATE-fight-arena.md).

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT down … cause=VOID …`, `lifted`, `spectating … mode=SPECTATOR`; `FIGHT all-down …` the next tick. (2) His quit: **`FIGHTWORLD left player=BaronVonYeetus world=<n> why=quit return=kept`**; then **`FIGHT nobody inside …`**, `FIGHT ended reason=nobody-inside-<outcome>` (or, if he took longer than five seconds, `all-down-FAILED` with him already home: then the row is not read as designed, and says so); the world unloaded and deleted, him offline. (3) His login: vanilla puts him at the same x, y, z of the overworld (READ at AQ1), **still in spectator** (his saved mode). **`FIGHTWORLD stale login caught player=BaronVonYeetus at=world <x y z> to=world <his return>`**, **`FIGHTWORLD mode player=BaronVonYeetus from=SPECTATOR to=SURVIVAL why=login`**, `FIGHTWORLD returned … why=login`. (4) He stands at his return location in survival | `FightDownSourceTest` *theEntryMode…* (the login route) | |

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
