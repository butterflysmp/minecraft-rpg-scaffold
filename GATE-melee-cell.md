# GATE — Melee M1: the Fire Melee cell, all placeholders, and PLAYERHIT

**Status: R0 PASS; MC rows NOT READ (Ben ran no rows); NOT approved to merge.**
Counts: 3 PASS (R0a-c), 0 FAIL, 0 not itemised, 16 NOT READ (MC1-MC10, MC12-MC17).

**The boot, 2026-09-28, on `9ee59a2e`:**
- **It was Ben's.** He ran `dev-server.sh --refresh-content` from his own Git Bash window, and the server started at
  22:25:36. Process chain: `java` → `bash.exe --login -i …\scripts\dev-server.sh --refresh-content`, started 22:25:17.
- **CC's own boot of the same tip ran at the same moment in the same working tree, and FAILED.** `maven-jar-plugin`
  reported `Error assembling JAR: …\core\target\classes\…\RerouteDamagePrice.class`, because Ben's `clean package`
  cleared `target/` mid-run. It deployed nothing and started no server. A core-only rerun afterwards packaged
  cleanly. The tree was checked clean afterwards: `git status --porcelain` and `git stash list` empty, HEAD
  `9ee59a2e`, `git diff --quiet HEAD` true. **The one-booter rule that follows is in PLAN-melee-class.md §6.5.**
- **R0 was read by CC on that server** (the jar scan while it ran, the log lines as quoted in each row). The seat
  ruled it this gate's R0.
- **Ben's LOGIN, and everything after it, verbatim.** Ben then declined further testing.
  ```
  [22:26:17] [User Authenticator #0/INFO]: UUID of player BaronVonYeetus is b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5
  [22:26:18] [Server thread/INFO]: BaronVonYeetus joined the game
  [22:26:18] [Server thread/INFO]: BaronVonYeetus[/192.168.68.94:52724] logged in with entity id 61 at ([minecraft:overworld]-94.6897295799853, 63.0, 2.391039596178532)
  [22:26:24] [Server thread/INFO]: Environment: Environment[sessionHost=https://sessionserver.mojang.com, …]
  [22:27:35] [Server thread/INFO]: BaronVonYeetus issued server command: /rpg mana refill
  ```
  There is no `/rpg mobtrace`, no staging command, no MOBSEED and no PLAYERHIT line. The log did not change from
  22:27:35 to at least 22:45:51. **Every MC row is NOT READ, and each is carried in PLAN-melee-class.md §6.5.**
- **The stop:** Ben typed `stop` in his console. The log reads:
  - `[22:52:45] [Server thread/INFO]: Stopping the server`
  - `[Rpg] Disabling Rpg v0.1.0-SNAPSHOT`
  - `BaronVonYeetus lost connection: Server closed`
  - every world through `ThreadedAnvilChunkStorage: All dimensions are saved`
  - `All RegionFile I/O tasks to complete`

  Afterwards **zero `java.exe`** were running. The whole log holds one `issued server command` line, the mana refill.

**#168 stays OPEN.** It needs a later boot that reads R0 + MC1, MC2 and MC3 before the seat approves it. M2 does
not start until it merges.

(Before the boot, the status line read:) **Status: NOT RUN.** *Amended 2026-09-29, before any boot, at the seat's request (A-E):*
- a fresh adult zombie per PLAYERHIT row, by `/summon` with NBT (read from the jar);
- in-game selector commands, and no console or UUIDs;
- the gives and the class slot;
- MC7 reads the aspect picker too;
- Ben's exact words in MC5.

Staging and instruments only: no other prediction moved. Every prediction below was written **before** any boot of this branch, and **this file is
COMMITTED before the boot**, so a prediction can be checked against origin for having existed first. Readings go
**beside** a prediction, never over it, and a prediction is not edited once its row has been read. **NO BOOT until the
seat has diffed this file.**

**Readings count only if Ben's LOGIN appears in THIS boot's log.** The seat's 2026-09-28 ruling applies: a row whose
prediction is a LOG LINE is PASS when that line appears verbatim after the LOGIN, named by Ben or not. "Not itemised" is
only for rows that rest on Ben's own observation.

```
ROWS     19   R0a R0b R0c
              MC1 MC2 MC3 MC4 MC5 MC6 MC7 MC8 MC9 MC10 MC12 MC13 MC14 MC15 MC16 MC17
         ──
         19   = 3 R0 + 16 MC     git grep -c '^### R0\|^### MC' <ref> -- GATE-melee-cell.md
```

**Plan:** `PLAN-melee-class.md`, RULINGS (Ben's Q1-Q7 and the seat's rulings 1-6) and §9.2's M1 table, which this file
supersedes. **Row numbers follow §9.2**, so MC11 does not exist here (it is a Sunder knockback row, M2's). MC16 and MC17
are new.

## GAME MODE

**SURVIVAL on every row except MC15, which is CREATIVE and says so.** MC15 is the creative Q split and can only be read
there. Every other row is survival because survival is what ships (the creative-divergence register, `verification.md`).

## Set-up

**Every command below is typed IN GAME by Ben, as an op.** None needs the console, and none names a UUID.

- **Boot:** `./scripts/dev-server.sh --refresh-content`. This branch adds content files, and R0c is what shows they
  arrived.
- **`/rpg mobtrace` first, and read its reply:** `Mob trace ON (MOBSEED / MOBREMOVE / MOBHIT / MOBHEAL / PLAYERHIT in
  the log).` The reply text is new on this branch, so it is also a witness that the jar carries the change.
- **`/time set 18000` and `/gamerule doMobSpawning false`.** Night, so a zombie does not catch fire in the sun (that fire
  would pollute MC12). No natural spawns to wander into a row.

  > **INSTRUMENT NOTE, 2026-09-29, the seat's, added before any MC row was read. No reading and no prediction
  > moves.** `doMobSpawning` is not a game rule in 26.1: the pinned `paper-26.1.2.jar`'s
  > `net.minecraft.world.level.gamerules.GameRules` names the rule **`spawn_mobs`**, and `doMobSpawning` occurs 0
  > times in it (`javap -c -p`, 60 rule names read). **Type `/gamerule spawn_mobs false`** instead. This file is
  > read in the stack-top boot, and the top's gate (`GATE-level.md`, #169) states the same replacement.
- **Stand on flat, solid ground with open space for 5 blocks to the east (+X) and to the south-east.** Every
  placement below is relative to Ben's feet.

### THE GIVES, AND THE CLASS SLOT

- **The Ability Stone is not given.** It is issued to hotbar slot 8 (on screen) by default, as `GATE-build-stone.md`
  ST1 read it. Its casts need a class and an element: star → **Build** → Class **Melee** → Element **Fire** (MC1).
- **`/rpg give emberblade`**: the sword, for MC5, MC6 and MC8.
- **`/rpg give brawlers_gauntlet`**: the Gauntlet, for MC8 and MC9.
- **The Gauntlet into the class slot:** click the Nexus star → click the armour stand named **Equipment** (slot 22)
  → move the Gauntlet from your inventory into **accessory slot 0, the top accessory slot** (the class slot;
  `GATE-accessories-b.md` B13, B16). **Out of the slot:** the same screen, and move it back into your inventory.
  Wait **1 s** after either before reading a hit: class damage is reconciled every 5 ticks.

### THE TEST ZOMBIE — A FRESH ONE FOR EVERY ROW THAT READS PLAYERHIT

**A dead zombie logs no PLAYERHIT, and that reads exactly like a miss.** MC3 alone lands 46 on a 100-HP zombie, and
every fire hit accrues Scorch and burns it. So **at the start of every row that reads PLAYERHIT, clear the old
zombie and place a new one**, and do it again **mid-row if its nameplate falls below half**. Because `sent=` is
pre-Defense, a fresh adult zombie is equivalent to the old one for every comparison below.

**The four commands, in this order, each time:**

1. **Clear:** `/kill @e[tag=t]`
2. **Centre Ben on his block, facing +X (east), pitch level:**
   `/execute align xz positioned ~0.5 ~ ~0.5 run tp @s ~ ~ ~ -90 0`
   `align xz` floors x and z to the block corner, and `positioned ~0.5 ~ ~0.5` moves to its centre. Yaw **-90** faces
   +X; pitch **0** is level.
3. **Summon the zombie at the row's offset** (each row gives `<dx> <dz>`), relative to Ben's feet:
   `/summon zombie ~<dx> ~ ~<dz> {NoAI:1b,PersistenceRequired:1b,IsBaby:0b,Tags:["t"]}`
   - **NoAI:** it never attacks, turns or walks.
   - **PersistenceRequired:** the F20 boot lost a spawned mob to a despawn.
   - **Tags:** every later command finds it by `@e[tag=t,limit=1]`.
4. **Read the seed line in the log:** **`MOBSEED <uuid> zombie gs=100 source=rolled max=100`**.
   - **`source=rolled`:** a summoned zombie carries no stored score, so it rolls from its position.
   - **`gs=100`:** the overworld is a blanket 100 at every distance (M25, `DimensionGearScore.OVERWORLD`).
   - **`max=100`:** 20 × 5 × 100/100.

**Why `/summon` WITH NBT and not `/rpg spawn`: it is an adult, plain zombie by construction. READ FROM THE PINNED JAR.**

- **The jar:** `run/versions/26.1.2/paper-26.1.2.jar`, build 74, the same build as `paper.version`. Read with `javap
  -c -p net.minecraft.server.commands.SummonCommand`.
- **The NBT form runs `lambda$register$2`.** It reads the `nbt` argument (`getCompoundTag(ctx, "nbt")`), then pushes
  **`iconst_0`** as `spawnEntity`'s last argument, the "finalize" flag.
- **`createEntity` gates `Mob.finalizeSpawn` on that flag** (`iload 4; ifeq 160`, which jumps over the
  `finalizeSpawn` call at offset 156).
- **The two forms without NBT** (`lambda$register$0` and `$1`) push `iconst_1`.
- **So a summon WITH NBT never calls `finalizeSpawn`:** no baby roll, no chicken jockey, no rolled armour or weapon.
  `/rpg spawn` does run vanilla's spawn set-up, and could roll a baby, whose smaller hitbox would break MC13 and MC14.
- **`IsBaby:0b` is therefore belt-and-braces, and no check row is needed.**

**Readings Ben takes in game (the replies are in chat):**
- `/data get entity @e[tag=t,limit=1] Pos` gives the zombie's feet.
- `/data get entity @s Pos` and `/data get entity @s Rotation` give Ben's feet, then yaw and pitch.
- **The PLAYERHIT line**, from `TracedHit.line`:
  `PLAYERHIT caster=<Ben's uuid> source=<id> target=<zombie uuid> zombie element=fire sent=<n.nnn> crit=<bool> triggerScore=<n>`.
  **`sent=` is BEFORE the zombie's Defense. The popup shows the post-Defense figure, so the popup is a SECONDARY read and
  is not predicted.** A line with `crit=true` is not comparable with a `crit=false` one: if one appears in a comparison
  row, re-cast.
- **Nothing is equipped in the aspect slots.** `keen_arc` changes the arc's 9 and 5, and every arc prediction below is
  unmodified.
- **Mana:** the thrust costs 10, the arc 15 and the Ultimate 60, from 100. Wait for mana between rows if a cast is
  refused. A refused cast logs no PLAYERHIT line, which would look like a miss.
- **The stone's gear score is 100** (it carries no stamp). Every stone-cast line predicts `triggerScore=100`.

---

## SELFTEST WITNESS AMENDMENT, 2026-10-05, BEFORE ANY BOOT (the seat's rulings c and d)

**No prediction moves.** `/rpg selftest <gate>` (#175, `GATE-selftest.md`) stages and drives this gate's CORE rows on
the shipped paths. Its lines are judged against the predictions below exactly as Ben's would be. **An AUTO reading is a
reading of the server, never of the client, and an AUTO half never makes a row PASS on its own**
(`.claude/rules/verification.md`, *AN AUTO READING IS A READING OF THE SERVER*). **The instrument never prints PASS or
FAIL**: a reading is recorded `AUTO PASS (selftest <sha>)` beside the row, quoting the real lines between that row's
`SELFTEST <gate> <row> START` and `DONE` (PLAYERHIT, DOTTICK, MOBSEED, CMD, REPLY, SLOT, PROBE). Staging is the gate's
own commands; where the scenario differs, the row says so. Every row not listed keeps its witness as written.

| row | witness now | the server's half, AUTO (`paper/src/main/resources/selftest/GATE-<gate>.yml#<row>`) | Ben's half |
|---|---|---|---|
| MC1 | AUTO-PARTIAL | the Build screen and both pickers dumped (option names, order, colours as built), and the pick's chat reply | the cell's name reads gold on his screen |
| MC2 | AUTO-PARTIAL | the Build screen's three loadout cells, dumped | a glance |
| MC3 | AUTO-PARTIAL | the three casts' PLAYERHIT lines, driven by `PlayerArmSwingEvent` (Left), `PlayerInteractEvent` (Right) and `dropItem(false)` (Q, the Q packet's own `ServerPlayer.drop`); the held item after Q | his own three inputs cast them, once: the input decode |

**The R0 moved again** (the seat's ruling c): the stack's one R0 is now `GATE-selftest.md`'s, on #175's head. The
SUPERSEDED note under this gate's R0 heading names `GATE-withered-shortbow.md`; read it as naming `GATE-selftest.md`.

---

## R0 — THE DEPLOYED BUILD CARRIES THIS BRANCH. IF ANY R0 FAILS, STOP

> **INSTRUMENT NOTE, 2026-10-05, the seat's ruling 1, added before the stack-top boot. No prediction moves.** At the
> stack-top boot, this gate's R0 rows are SUPERSEDED by `GATE-withered-shortbow.md`'s R0 on `<sha>`; recorded
> SUPERSEDED, never PASS or FAIL. Intermediate facts in R0b/R0c are not read. *`<sha>` is the stack top's head as that
> R0a reads it in the boot: a commit beneath the top cannot name the top's hash, because the top is rebased onto this
> note.* **The R0 readings below, on `9ee59a2e` (2026-09-28), are that earlier boot's and stand as written**; they
> are not re-read, and they do not carry to the stack-top boot.

### R0a — the build line names the head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, the PR's head as `gh pr view <n> --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS.** `[22:25:42] [Server thread/INFO]: [Rpg] Build: 9ee59a2e`, a bare hash, equal to the pushed head (`9ee59a2eb672…` on `git ls-remote`). Ben's boot (see Status) |

### R0b — the jar carries the trace and the cell

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `TracedHit`, `content/builds/melee_fire.yml` and `content/abilities/active_placeholder_melee.yml`. ABSENT: the control. All three are new on this branch, so master's jar reads ABSENT for them | the scan below | **PASS.** 817 entries: `PRESENT` TracedHit.class; `PRESENT` content/builds/melee_fire.yml; `PRESENT` content/abilities/active_placeholder_melee.yml; `ABSENT` NoSuchClassControl.class (the control). Read from `run\plugins\rpg-0.1.0-SNAPSHOT.jar` while Ben's server ran |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/combat/TracedHit.class',
               'content/builds/melee_fire.yml',
               'content/abilities/active_placeholder_melee.yml',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — the boot log: content loads, nothing refused

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. Against F20's reading on `002352cb`: abilities 8 → 11, pools 2 → 3, aspects 4 → 6, every other count unchanged. **No** `Refusing`, `Skipping`, `SEVERE` or exception. The known WARNs are `volley_stone`'s 27-tick cooldown and its summary line | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | **PASS.** `[22:25:42] [Server thread/INFO]: [Rpg] Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. No `Refusing`, `Skipping`, `SEVERE` or exception; the only content WARNs are `volley_stone`'s pair. `--refresh-content` ran: every `run/plugins/Rpg/content/builds/*.yml` was rewritten at 22:25:42 |

---

## THE CELL

### MC1 — the class picker offers Melee (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| The Build screen's CLASS options read **Mage, Melee, Ranger**, in that order. Picking **Melee** offers ELEMENT **Fire** only, and the cell's name reads **Fire Melee** in gold | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

### MC2 — the default loadout (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| A profile's first pick of Fire Melee shows **Q = Melee Ultimate (placeholder)**, **Left = Melee Thrust (placeholder)**, **Right = Melee Active (placeholder)** | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

### MC3 — the stone's three inputs cast the three abilities (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| **Staging:** the four zombie commands with offset **`~3 ~ ~0`** (`/summon zombie ~3 ~ ~0 …`): 3.0 blocks straight ahead. Ben is left facing +X at pitch 0, which is level aim at it; do not move the mouse. **Left:** one line `source=active_placeholder_melee_thrust … sent=7.000 crit=false triggerScore=100`. **Right:** two lines, `source=active_placeholder_melee … sent=9.000` then `… sent=5.000`, same target. **Q:** one line `source=ultimate_placeholder_melee … sent=25.000`. The stone never leaves its slot (Ben) | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

### MC4 — no regression in the other two cells (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Fire Ranger's default reads Q = Ranger Ultimate (placeholder), Left = Recall, Right = Solar Lance. Fire Mage's reads Q = Mage Ultimate (placeholder), Left = Ember Step, Right = Solar Grenade. Neither offers any Melee ability | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

### MC5 — Q with the sword in hand drops it (SURVIVAL) — witness: Ben, plus the log

| prediction | READING |
|---|---|
| As Fire Melee with `emberblade` in the main hand, Q: **the sword is dropped** and **no PLAYERHIT line** follows. This is **ruled behaviour** (Q5, O1: *"yes you can swap between them"*), recorded, not a defect | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

### MC6 — swapping from the stone resets the attack charge (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| **Staging:** the four zombie commands with offset **`~2 ~ ~0`**, aim level (as left by the `tp`). Hold `emberblade`, wait 2 s, swing: `source=emberblade/left_click … sent=S` (a full charge). Swap to the stone and back, and swing **at once**: `sent=` **below S** (§5.3, read from the jar). S itself is not predicted: it is the minted blade's score-scaled stat. The sweep is not read here | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

### MC7 — the fragment and aspect pickers (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Fire Melee's **fragment** picker offers exactly **Vigor, Focus, Keen, Mending and Ward** (in their own colours), and **not Ember Cache**. Its **aspect** picker offers exactly **Keen Arc** and **Restless Quake** (added by the seat, 2026-09-29, before any boot) | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

### MC8 — the Brawler's Gauntlet is live for Melee and only for Melee (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| **Staging:** the four zombie commands with offset **`~2 ~ ~0`**, aim level, and **replace the zombie (the same four commands) whenever its nameplate is below half**: a fresh adult is equivalent, since `sent=` is pre-Defense. Hold `emberblade`, 2 s between swings so every swing is a full charge. The Gauntlet goes in and out by the Equipment screen (set-up), and the cell changes by star → Build. **(1)** No Gauntlet: `sent=S`. **(2)** Gauntlet in the class slot, wait 1 s (the 5-tick reconcile): **`sent=S+3.000`**. **(3)** Switch the cell to Fire Ranger with the Gauntlet still in the slot: **`sent=S`** (inert). **(4)** Back to Fire Melee: **`sent=S+3.000`**. `class_damage` is a flat addend after the percentages, so the difference is exactly 3 at full charge (`HitDamage.hitBase`) | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

### MC9 — a stone cast is gear-blind: CONFIRMATION OF RULED BEHAVIOUR (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| **Ben's Q6: Actives are gear-blind BY DESIGN.** **Staging:** the four zombie commands with offset **`~2 ~ ~0`**, aim level. Replace the zombie between the two halves if its nameplate is below half: a fresh adult is equivalent, since `sent=` is pre-Defense. Stone in hand, the right-click arc. **Without the Gauntlet:** `sent=9.000` and `sent=5.000`, both `triggerScore=100`. **With the Gauntlet in the class slot** (wait 1 s): **the identical two lines**, `sent=9.000` and `sent=5.000`, `triggerScore=100`. Any difference is a FAIL | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

---

## THE ARC'S GEOMETRY — first live coverage of `CastExecutor.meleeTarget` (PLAN §6.3)

**Distances are read, not assumed.** Before each cast, Ben types `/data get entity @e[tag=t,limit=1] Pos`,
`/data get entity @s Pos` and `/data get entity @s Rotation`. **d** is the horizontal distance between the two feet:
√((x₁−x₂)² + (z₁−z₂)²). "Aim level" means the second `Rotation` number, the pitch, is within ±2°. The centring `tp`
leaves it at exactly 0.

### MC10 — a hit at 2 blocks: the direct hit and the burst (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| **Staging:** the four zombie commands with offset **`~2 ~ ~0`**, aim level. d = 2.0 ± 0.1, right-click: **two lines, `source=active_placeholder_melee … sent=9.000 …` then `… sent=5.000 …`**, the same target uuid, `triggerScore=100`. Popup (secondary): two numbers | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

### MC12 — a fire hit accrues Scorch; the burn is not traced (SURVIVAL) — witness: `/data get … Fire`, plus the log

| prediction | READING |
|---|---|
| **Staging:** the four zombie commands with offset **`~3 ~ ~0`**, aim level. CONTROL: `/data get entity @e[tag=t,limit=1] Fire` reads **0 or below** (not burning; `-1s` is expected, a vanilla default *not read from the jar*; this is why the set-up makes it night). One left-click thrust: the line `sent=7.000`, then within 1 s **`Fire` reads a positive value** (the accrued Scorch lights it, `BukkitCombatant.applyDamage`). **The burn ticks that follow add NO PLAYERHIT line**: only direct ability and weapon hits are traced (`TracedHit`) | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

### MC13 — point-blank: the direct hit misses, only the burst lands (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| **Staging:** the four zombie commands with offset **`~0.6 ~ ~0`**: the two hitboxes (half-width 0.3 each) just touch. Collision may nudge either body, so read d **after** the summon and before the cast. **d below 0.935**, aim level (pitch 0 from the `tp`). Right-click: **exactly ONE line, `source=active_placeholder_melee … sent=5.000`**, and **no `sent=9.000`**. At d below ~0.94 the eye-to-feet cone misses (§6.3b, executed in `jshell`). The miss burst centres 3.5 ahead at eye height, and its 3.5 box still reaches the zombie. **If the read d is 0.935 or more, the row is VOID: re-stage** | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

### MC14 — diagonal at 4.5: the reach is a box (F2) (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| **Staging:** the four zombie commands with offset **`~3.2 ~ ~3.2`** (d = 4.53, **beyond `reach` 3.5**). Then aim AT the zombie's feet: **`/tp @s ~ ~ ~ facing entity @e[tag=t,limit=1] feet`**. That keeps Ben's position and turns him so his look passes through its feet; the jar's `TeleportCommand` carries `facing`, `entity`, `facingEntity` and `facingAnchor`. Do not move the mouse before the right-click. **Two lines, `sent=9.000` then `sent=5.000`**: the direct hit connects, because `combatantsNear` is a hitbox-intersecting cube (F2). **One line, `sent=5.000` only, would mean a sphere**, and the finding would be wrong. F2 stays a finding either way (seat ruling 6): no fix | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

---

## THE REST

### MC15 — creative Q (CREATIVE) — witness: PLAYERHIT line, plus Ben

| prediction | READING |
|---|---|
| **CREATIVE.** **Staging:** the four zombie commands with offset **`~3 ~ ~0`**. **Hotbar Q** with the stone: `source=ultimate_placeholder_melee … sent=25.000`. **Inventory Q** (the stone dragged in the open inventory) is refused and casts nothing, with no line. This is ST12's shape re-read for a Melee cell | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

### MC16 — the trace toggles off and back on (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| **Staging:** the four zombie commands with offset **`~2 ~ ~0`**, aim level, with the trace still ON so its MOBSEED line is read. Then `/rpg mobtrace` again: `Mob trace OFF (…)`. A right-click arc on the zombie then adds **zero** PLAYERHIT lines. `/rpg mobtrace` once more: `Mob trace ON (…)`, and the next arc logs its two lines | **NOT READ.** Ben ran no rows: after his LOGIN at 22:26:18 the log holds one command, `/rpg mana refill` (22:27:35), and no staging, no MOBSEED and no PLAYERHIT line (excerpt in Status) |

### MC17 — a hit on a player is not traced (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| **Only if a second player is on the server; otherwise NOT READ, and said so.** A thrust that hits the other player adds **no** PLAYERHIT line (the line is player-to-mob, `PlayerHitTraceTest.aPlayerTargetIsNotTraced`) | **NOT READ.** No second player joined, and Ben ran no rows (Status) |
