# GATE — Melee M1: the Fire Melee cell, all placeholders, and PLAYERHIT

**Status: NOT RUN.** Every prediction below was written **before** any boot of this branch, and **this file is
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

- **`/rpg mobtrace` first, and read its reply:** `Mob trace ON (MOBSEED / MOBREMOVE / MOBHIT / MOBHEAL / PLAYERHIT in
  the log).` The reply text is new on this branch, so it is also a witness that the jar carries the change.
- **`/time set 18000` and `/gamerule doMobSpawning false`.** Night, so a zombie does not catch fire in the sun (that fire
  would pollute MC12). No natural spawns to wander into a row.
- **The test zombie.** `/rpg spawn zombie 100` gives a `MOBSEED <uuid> zombie gs=100 source=stored max=100` line. Then,
  at the console: `data merge entity <uuid> {NoAI:1b,PersistenceRequired:1b}`. **NoAI** means it never attacks, turns
  or walks, so the geometry rows' distances hold. **PersistenceRequired** is there because the F20 boot lost a
  `/rpg spawn` mob to a despawn. Place it with `tp <uuid> <x> <y> <z>` at the console; each row gives the offset.
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

## R0 — THE DEPLOYED BUILD CARRIES THIS BRANCH. IF ANY R0 FAILS, STOP

### R0a — the build line names the head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, the PR's head as `gh pr view <n> --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries the trace and the cell

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `TracedHit`, `content/builds/melee_fire.yml` and `content/abilities/active_placeholder_melee.yml`. ABSENT: the control. All three are new on this branch, so master's jar reads ABSENT for them | the scan below | |

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
| `Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. Against F20's reading on `002352cb`: abilities 8 → 11, pools 2 → 3, aspects 4 → 6, every other count unchanged. **No** `Refusing`, `Skipping`, `SEVERE` or exception. The known WARNs are `volley_stone`'s 27-tick cooldown and its summary line | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | |

---

## THE CELL

### MC1 — the class picker offers Melee (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| The Build screen's CLASS options read **Mage, Melee, Ranger**, in that order. Picking **Melee** offers ELEMENT **Fire** only, and the cell's name reads **Fire Melee** in gold | |

### MC2 — the default loadout (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| A profile's first pick of Fire Melee shows **Q = Melee Ultimate (placeholder)**, **Left = Melee Thrust (placeholder)**, **Right = Melee Active (placeholder)** | |

### MC3 — the stone's three inputs cast the three abilities (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| Zombie at 3.0 blocks straight ahead (Ben at `X.5 Y Z.5` facing +X, zombie at `X+3.5 Y Z.5`), aim level at it. **Left:** one line `source=active_placeholder_melee_thrust … sent=7.000 crit=false triggerScore=100`. **Right:** two lines, `source=active_placeholder_melee … sent=9.000` then `… sent=5.000`, same target. **Q:** one line `source=ultimate_placeholder_melee … sent=25.000`. The stone never leaves its slot (Ben) | |

### MC4 — no regression in the other two cells (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Fire Ranger's default reads Q = Ranger Ultimate (placeholder), Left = Recall, Right = Solar Lance. Fire Mage's reads Q = Mage Ultimate (placeholder), Left = Ember Step, Right = Solar Grenade. Neither offers any Melee ability | |

### MC5 — Q with the sword in hand drops it (SURVIVAL) — witness: Ben, plus the log

| prediction | READING |
|---|---|
| As Fire Melee with `emberblade` in the main hand, Q: **the sword is dropped** and **no PLAYERHIT line** follows. This is **ruled behaviour** (Q5, O1: *"You can swap between them."*), recorded, not a defect | |

### MC6 — swapping from the stone resets the attack charge (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| With the zombie at 2 blocks: hold `emberblade`, wait 2 s, swing: `source=emberblade/left_click … sent=S` (a full charge). Swap to the stone and back, and swing **at once**: `sent=` **below S** (§5.3, read from the jar). S itself is not predicted: it is the minted blade's score-scaled stat. The sweep is not read here | |

### MC7 — the fragment picker (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Fire Melee's fragment picker offers exactly **Vigor, Focus, Keen, Mending and Ward** (in their own colours), and **not Ember Cache** | |

### MC8 — the Brawler's Gauntlet is live for Melee and only for Melee (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| Hold `emberblade`, zombie at 2 blocks, 2 s between swings so every swing is a full charge. **(1)** No Gauntlet: `sent=S`. **(2)** Gauntlet in the class slot, wait 1 s (the 5-tick reconcile): **`sent=S+3.000`**. **(3)** Switch the cell to Fire Ranger with the Gauntlet still in the slot: **`sent=S`** (inert). **(4)** Back to Fire Melee: **`sent=S+3.000`**. `class_damage` is a flat addend after the percentages, so the difference is exactly 3 at full charge (`HitDamage.hitBase`) | |

### MC9 — a stone cast is gear-blind: CONFIRMATION OF RULED BEHAVIOUR (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| **Ben's Q6: Actives are gear-blind BY DESIGN.** The same zombie at 2 blocks, stone in hand, the right-click arc. **Without the Gauntlet:** `sent=9.000` and `sent=5.000`, both `triggerScore=100`. **With the Gauntlet in the class slot** (wait 1 s): **the identical two lines**, `sent=9.000` and `sent=5.000`, `triggerScore=100`. Any difference is a FAIL | |

---

## THE ARC'S GEOMETRY — first live coverage of `CastExecutor.meleeTarget` (PLAN §6.3)

**Distances are read, not assumed.** Before each cast, `data get entity <zombie uuid> Pos` and `data get entity <Ben>
Pos` / `Rotation` at the console. **d** is the horizontal distance between the two feet. "Aim level" means the pitch
read by `Rotation` is within ±2°.

### MC10 — a hit at 2 blocks: the direct hit and the burst (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| d = 2.0 ± 0.1, aim level at the zombie, right-click: **two lines, `source=active_placeholder_melee … sent=9.000 …` then `… sent=5.000 …`**, the same target uuid, `triggerScore=100`. Popup (secondary): two numbers | |

### MC12 — a fire hit accrues Scorch; the burn is not traced (SURVIVAL) — witness: console `Fire`, plus the log

| prediction | READING |
|---|---|
| A fresh zombie. CONTROL: `data get entity <uuid> Fire` reads **0 or below** (not burning; `-1s` is expected, a vanilla default *not read from the jar*; this is why the set-up makes it night). One left-click thrust: the line `sent=7.000`, then within 1 s **`Fire` reads a positive value** (the accrued Scorch lights it, `BukkitCombatant.applyDamage`). **The burn ticks that follow add NO PLAYERHIT line**: only direct ability and weapon hits are traced (`TracedHit`) | |

### MC13 — point-blank: the direct hit misses, only the burst lands (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| **d below 0.935**, read (the collision boxes stop it near 0.6), aim level. Right-click: **exactly ONE line, `source=active_placeholder_melee … sent=5.000`**, and **no `sent=9.000`**. At d below ~0.94 the eye-to-feet cone misses (§6.3b, executed in `jshell`). The miss burst centres 3.5 ahead at eye height, and its 3.5 box still reaches the zombie. **If the read d is 0.935 or more, the row is VOID: re-stage** | |

### MC14 — diagonal at 4.5: the reach is a box (F2) (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| Ben at `X.5 Y Z.5`, zombie at `X+3.7 Y Z+3.7` (offset 3.2, 3.2: d = 4.53, **beyond `reach` 3.5**), aim AT it. **Two lines, `sent=9.000` then `sent=5.000`**: the direct hit connects, because `combatantsNear` is a hitbox-intersecting cube (F2). **One line, `sent=5.000` only, would mean a sphere**, and the finding would be wrong. F2 stays a finding either way (seat ruling 6): no fix | |

---

## THE REST

### MC15 — creative Q (CREATIVE) — witness: PLAYERHIT line, plus Ben

| prediction | READING |
|---|---|
| **CREATIVE.** Zombie at 3 blocks. **Hotbar Q** with the stone: `source=ultimate_placeholder_melee … sent=25.000`. **Inventory Q** (the stone dragged in the open inventory) is refused and casts nothing, with no line. This is ST12's shape re-read for a Melee cell | |

### MC16 — the trace toggles off and back on (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `/rpg mobtrace` again: `Mob trace OFF (…)`. A right-click arc on the zombie then adds **zero** PLAYERHIT lines. `/rpg mobtrace` once more: `Mob trace ON (…)`, and the next arc logs its two lines | |

### MC17 — a hit on a player is not traced (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| **Only if a second player is on the server; otherwise NOT READ, and said so.** A thrust that hits the other player adds **no** PLAYERHIT line (the line is player-to-mob, `PlayerHitTraceTest.aPlayerTargetIsNotTraced`) | |
