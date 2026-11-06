# GATE — the build system, Slice 1: the Ability Stone

**Status: READ on `1bf4e53`, 2026-09-25 -- every row reported good by Ben, including the star regression rows and B9. Four requested readings were NOT GIVEN: ST17's count and hold time, ST18 (a) and (b) counts, whether anything flashed in ST6b, and whether ST10 was read or skipped. Each row says which.** Every prediction below was written **before** any boot. **NO BOOT until the seat had
diffed the PR from origin.** Readings go **beside** a prediction, never over it, and a prediction is not
edited once its row has been read. Readings are verdicts, not figures, unless the row asks for a figure.

```
ROWS     22   R0a R0b R0c ST0 ST1 ST2 ST3 ST4 ST5 ST6 ST6b ST6c ST7 ST8 ST9 ST10 ST11 ST12 ST13 ST14 ST15 ST16
         +    ST17 ST18 = 24
         ──
         24   = git grep -c '^### R\|^### ST' <ref> -- GATE-build-stone.md
         +    the STAR REGRESSION list at the end: GATE-nexus.md and GATE-accessories-b.md rows, re-run here
```

**Plan:** `PLAN-build-system.md` §3.1, with rulings 2, 10, 12, 13, 14 and 19 and the clarification under
RULINGS (Q with a screen open casts nothing). The spike these rows rest on is §3.1.0.1, measured at `d7b4087`.

**Declared game mode: SURVIVAL, EVERY ROW, except ST12 (CREATIVE).** Creative splits one gesture into
several single-slot writes (GATE-nexus.md row 8), its in-screen Q arrives as a different click type
(measured, §3.1.0.1), and a creative block breaks instantly -- each of which would make a survival
prediction hollow.

**Boot:** `./scripts/dev-server.sh --refresh-content`. **`--refresh-content` IS REQUIRED**: this slice adds
`content/builds/` and two abilities and DELETES `content/weapons/ability_stone.yml`, and `saveResource`
never deletes -- without the refresh the old file stays on disk (R0c reads that).

**Set-up shared by most rows:** an op player, `/rpg class ranger` then `/rpg element fire` (the Fire Ranger:
Left = Rekindle, Right = Solar Lance, Q = the placeholder Ranger Ultimate), full mana, and a stone block and a
crafting table within reach. Cooldowns: Rekindle 200 ticks (10.0 s), Solar Lance 100 (5.0 s), the placeholder
Ultimate 1200 (60.0 s).

---

## *** R0 — THE FIRST ROWS OF THE FILE. If any R0 row fails, STOP. ***

### R0a — the build line names this branch's tip

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <tip>` naming the PR's tip SHA -- not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS** *(1bf4e53)* -- `[20:59:36] [Server thread/INFO]: [Rpg] Build: 1bf4e53`, the only build line in the file (`Select-String` count 1); no `-dirty`  |

### R0b — the deployed jar carries the stone's classes

| prediction | instrument | READING |
|---|---|---|
| `StoneItems`, `StoneCaster`, `LockedItem`, `PoolLoader` and core `ConvergePlan` PRESENT; the control ABSENT | the scan below | **PASS** *(1bf4e53)* -- `PRESENT` StoneItems, StoneCaster, LockedItem, PoolLoader, ConvergePlan; `ABSENT` NoSuchClassControl (the control)  |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/paper/build/StoneItems.class',
               'io/github/butterflysmp/rpg/paper/build/StoneCaster.class',
               'io/github/butterflysmp/rpg/paper/nexus/LockedItem.class',
               'io/github/butterflysmp/rpg/paper/content/PoolLoader.class',
               'io/github/butterflysmp/rpg/core/build/ConvergePlan.class',
               'io/github/butterflysmp/rpg/paper/build/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — the content: two pools load, and the old weapon file is gone from disk AND from the jar

| prediction | instrument | READING |
|---|---|---|
| the boot's `Loaded ...` line reads **`2 pools`**; `run\plugins\Rpg\content\weapons\ability_stone.yml` does **not** exist; the jar holds `content/builds/ranger_fire.yml` and **no** `content/weapons/ability_stone.yml` | `Select-String run\logs\latest.log -Pattern 'pools,'`; `Test-Path run\plugins\Rpg\content\weapons\ability_stone.yml` (predict `False`); the R0b scan with those two entry names | **PASS** *(1bf4e53)* -- `[20:59:36] [Server thread/INFO]: [Rpg] Loaded 8 abilities, 28 visuals, 5 statuses, 7 elements, 10 enchants, 2 kits, 2 pools, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`; `Test-Path ...\weapons\ability_stone.yml` -> `False`; content\builds on disk: `mage_fire.yml, ranger_fire.yml`; jar: `PRESENT content/builds/ranger_fire.yml`, `ABSENT content/weapons/ability_stone.yml`  |

---

## THE ROWS

### ST0 — the old weapon is gone

| prediction | READING |
|---|---|
| `/rpg give ability_stone` is refused as an unknown id; tab completion after `/rpg give ` does not offer it | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST1 — the stone is issued, in slot 7, and does not stack

| prediction | READING |
|---|---|
| after the set-up, **rejoin**: an echo shard named **Ability Stone** sits in **hotbar slot 8 on screen (index 7)**, beside the Nexus star in the last cell. Its lore reads *Left click: Rekindle / Right click: Solar Lance / Q: Ranger Ultimate (placeholder)*. `/give @s echo_shard 1` then trying to drop the plain shard onto the stone: they do **not** merge | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST2 — left click in air casts Active 1, and a refusal is the action bar only

| prediction | READING |
|---|---|
| left click in air: Rekindle casts (the backward dash and the three embers). Left click again at once: **no cast**, and the **action bar** reads *Rekindle -- ready in N.Ns*. **Chat stays empty** | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST3 — the stone never damages a block

| prediction | READING |
|---|---|
| hold left on a stone block for 5 s: **no crack appears and the block does not break**. (The casts are ST18's.) | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST4 — left on a mob casts, and does not hit

| prediction | READING |
|---|---|
| left click a zombie: Rekindle casts once; the zombie shows **no hurt flash** and takes no damage from the click itself (its HP bar/nameplate unchanged unless the embers reach it) | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST5 — Q from the hotbar casts the Ultimate; Q in the inventory casts nothing

| prediction | READING |
|---|---|
| (a) holding the stone, press Q: the placeholder Ultimate casts (a long ray, 30 fire damage on a target), and **the stone stays in its slot**. (b) open the inventory (E), hover the stone, press Q: **nothing casts** and the stone stays | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST6 — right click casts Active 2, main hand only; the block does not open

| prediction | READING |
|---|---|
| right click in air: Solar Lance casts. Right click the crafting table: Solar Lance casts and **no crafting screen opens**. With a torch in the off hand, right click the stone block: Solar Lance casts and **no torch is placed**. Mana drops by **25**, not 50, per click (read the HUD) -- one cast, not one per hand | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST6b — right click on a villager

| prediction | READING |
|---|---|
| right click a villager: Solar Lance casts; **no trade screen** opens | **PASS** *(1bf4e53)* -- as reported by Ben  |
| **(added on the seat's review of `46fb8b8`)** with Solar Lance READY, right click the villager ONCE: it casts **exactly once** (one ray, mana down 25, not 50), and **no "Solar Lance -- ready in ..." line flashes on the action bar in the same moment**. `PlayerInteractEntityEvent` may arrive with a second right-click input in the same tick (S3, never measured). That second input would be refused by the cooldown the first just started, and would show as that flash. **A flash is recorded as a FINDING** (the duplicate exists, and the cooldown absorbed it), not as a pass | reported good by Ben *(1bf4e53)*. Whether anything flashed on the action bar: **NOT GIVEN** -- Ben chose to give no figures; not inferred. A flash would have been a finding; its absence is not recorded either  |

### ST6c — SNEAK right click on a hijacked block opens it (ruling 14)

| prediction | READING |
|---|---|
| sneak and right click the crafting table: **our crafting menu opens** and **nothing casts** (mana unchanged). *(The inverse of a weapon, where sneaking is the escape hatch that lets the weapon cast -- §2.5.)* | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST7 — the stone's picker: hotbar only, and the star's cell is reserved

| prediction | READING |
|---|---|
| `/menu` -> Settings -> **Ability Stone Slot** (slot 31): the storage rows are **plain filler**; the hotbar row shows the stone's cell **lime**, the Nexus's cell as a **red "Reserved" barrier** ("Holds your Nexus."). Clicking the reserved cell: a red line, nothing moves. Clicking hotbar 3: the stone **moves to hotbar 3** and whatever sat there is handed back. And the mirror: the **Nexus Slot** picker shows the stone's cell as **Reserved** ("Holds your Ability Stone.") | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST8 — the toggle

| prediction | READING |
|---|---|
| Settings **Ability Stone: ON** (slot 33) -> OFF: the stone disappears and its cell is **usable** (put a block in it). -> ON with the cell occupied: refused, naming the cell. Empty it, -> ON: the stone returns there | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST9 — death, respawn, rejoin

| prediction | READING |
|---|---|
| `/kill`, respawn: **exactly one** stone, in its slot, and one star. Rejoin: the same | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST10 — no class, no loadout

| prediction | READING |
|---|---|
| a player whose class is `none` (a fresh profile, or `/rpg class` never run): the stone is issued (ON by default); left, right and Q each show the action-bar line *Choose a class and an element first* and **cast nothing** | reported good by Ben *(1bf4e53)*. Whether this row was READ or SKIPPED: **NOT GIVEN** -- Ben chose to give no figures; not inferred  |

### ST11 — the stone is inert

| prediction | READING |
|---|---|
| the stone cannot be placed in a brewing stand's ingredient slot, a furnace's fuel slot or a smithing table (the lock refuses moving it out of its slot at all); a villager offers no trade for it | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST12 — CREATIVE: hotbar Q casts, inventory Q refuses

| prediction | READING |
|---|---|
| **`/gamemode creative`.** (a) holding the stone, Q: the Ultimate casts, **Active 1 does not**, and the stone stays. (b) inventory open, hover the stone, Q: **nothing casts**, and the stone **stays in its slot** (inspect with `/data get entity @s Inventory` from the console, as GATE-nexus.md row 8 does -- the client view can lie in creative) | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST13 — Q never also casts Active 1 (the Q-swing guard)

| prediction | READING |
|---|---|
| wait until Rekindle is READY. (a) hotbar Q: the Ultimate casts; **no Rekindle dash**, and an immediate left click then **casts** Rekindle (proving Q did not start its cooldown). (b) Rekindle ready, inventory open, Q on the stone: **no dash, no Ultimate**; close, left click: Rekindle casts | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST14 — an operator's `/rpg cast` is free and unlimited (ruling 10)

| prediction | READING |
|---|---|
| as an op: `/rpg cast void_slash` (in no pool) casts; mana unchanged; again at once: casts again; `/rpg cast solar_lance` then immediately right click with the stone: Solar Lance **casts** (the dev cast started no cooldown). Tab completion after `/rpg cast ` offers **every** ability | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST15 — a non-op's `/rpg cast` is the loadout

| prediction | READING |
|---|---|
| `/deop <self>` (console): `/rpg cast void_slash` -> *void_slash is not in your loadout*; `/rpg cast solar_lance` casts, spends 25 mana, and a stone right click at once shows *Solar Lance -- ready in ...* on the action bar. Tab completion offers exactly **ultimate_placeholder_ranger, rekindle, solar_lance**. `/op <self>` afterwards | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST16 — echo-shard identity: a Ward Charm is never the stone, the stone never an accessory

| prediction | READING |
|---|---|
| `/rpg give ward_charm` (an echo shard). Holding the **charm**: left, right and Q do nothing stone-like -- no cast, and Q **drops** it. The charm equips into an Equipment accessory slot; the stone is **refused** there, and `/rpg stats` shows no accessory line for it. Settings stone toggle OFF removes the stone and **not** the charm. The stone cannot be moved into the inventory crafting grid (no recovery compass). The charm and the stone never stack or merge on a cursor | **PASS** *(1bf4e53)* -- as reported by Ben  |

### ST17 — a held right click re-casts whenever the cooldown allows (ruling 19)

| prediction | READING |
|---|---|
| Solar Lance ready, full mana. **Hold right click in air for 12 s (timed; 240 ticks).** Casts = floor(240 / 100) + 1 = **3**: at once, then about 5 s and 10 s in. Between casts only the action bar speaks; **chat stays empty**. *(Held inputs arrive every 4 ticks, measured, so each re-cast lands within 4 ticks of its cooldown ending; 12 s is chosen far from the 10 s and 15 s boundaries.)* | **PASS** *(1bf4e53)* -- as reported by Ben. Cast count and hold time: **NOT GIVEN** -- Ben chose to give no figures; not inferred |

### ST18 — a held left click on a block re-casts; in air it casts once

| prediction | READING |
|---|---|
| Rekindle ready, full mana. (a) **Hold left on the stone block for 12 s.** Casts = floor(240 / 200) + 1 = **2**, at once and about 10 s in; the block untouched (ST3). **If only ONE cast happens, that is a FINDING, not a pass:** the spike measured the swing-per-tick stream with block damage UNcancelled, and whether the client keeps swinging once its dig is refused was never measured. (b) **Hold left in AIR for 12 s: ONE cast** -- the client sends one swing for a held air click (measured) | **PASS** *(1bf4e53)* -- as reported by Ben. (a) and (b) cast counts: **NOT GIVEN** -- Ben chose to give no figures; not inferred |

---

## STAR REGRESSION — re-run at this slice's tip (PLAN-build-system.md §3.1, amendment 4)

The `LockedItem` generalisation touched `NexusSlots` (convergence now executes `ConvergePlan`), the six
`onNexus*` handlers, the Settings screen and the picker. **Every star row that reads those is re-run here,
beside the stone rows, on the same tip.** Each reading goes on this page with the sha; the original page is
not edited.

| source | rows | what they guard | READING |
|---|---|---|---|
| GATE-nexus.md, the lock | ROW 1 (1a-1c), ROW 2, ROW 3, ROW 4, ROW 6 (6.1-6.5, 6.4′), ROW 8 (8a-8f; 8e and 8f are the ship condition) | the six handlers, the conversion, creative duplication | **PASS** *(1bf4e53)* -- as reported by Ben  |
| GATE-nexus.md, convergence | ROW 5 (5a-5d), ROWS 20-24 (ROW 22 re-read on a v4 file -> v5) | `converge` -- now `ConvergePlan` executed | **PASS** *(1bf4e53)* -- as reported by Ben  |
| GATE-nexus.md, Settings and the picker | ROWS 25-30 | the Settings screen gained two cells; the picker gained a mode | **PASS** *(1bf4e53)* -- as reported by Ben  |
| GATE-nexus.md, slice 7 | ROWS 49-57, especially 53 (Q, F, number-key refuse) and 56 (creative) | the open gesture beside a second locked item | **PASS** *(1bf4e53)* -- as reported by Ben  |
| GATE-nexus.md, slice 8 | ROWS 58-65 | **the star's range was not narrowed to the stone's** | **PASS** *(1bf4e53)* -- as reported by Ben  |
| GATE-nexus.md, slice 10 | ROWS 80-91 | `/menu`, labels, the picker's clones, the toggle | **PASS** *(1bf4e53)* -- as reported by Ben  |
| GATE-accessories-b.md | B9 | the star cannot enter an armour or accessory slot | **PASS** *(1bf4e53)* -- as reported by Ben  |

**Not re-run, each with its reason:** GATE-nexus.md ROWS 9-19 (hub rendering and the stats head -- the hub
is unchanged in this slice, and ROW 30 is the control), 31-47 (stations), 66-77 (player level), 92-114 (the
vault). None reads a `NexusSlots`, `converge` or handler path.

---

## MUTATIONS — run before the PR, recorded here so the gate rows that back them are named

Run with `perl` literal substitutions that must match exactly once, each applied, proven applied
(marker present AND the file differs from HEAD), tested, reverted from HEAD, and proven reverted. Each
module's surefire reports read separately.

| mutation | predicted to redden | result |
|---|---|---|
| M1 the picker's RESERVED branch deleted (`LockedSlots` returns null for the other item's slot) | `LockedSlotsTest` + ST7 | **KILLED** by `LockedSlotsTest` |
| M2 converge the stone BEFORE the star | the plan said `LockedSlotsTest`; **that was wrong** | **SURVIVED** -- as it should: the order is not visible to any unit test, and it is harmless while `LockedSlots` keeps the two targets distinct, which `LockedSlotsTest` does prove. Recorded, not "fixed" |
| M3 Q with a screen open casts | `StoneInputTest` + ST5(b) | **KILLED** by `StoneInputTest` |
| M4 the off-hand half of a right click casts | `StoneInputTest` + ST6 | **KILLED** by `StoneInputTest` |
| M5 `castUnchecked` goes through `resolve` | `AbilityServiceTest` + ST14 | **KILLED** by `AbilityServiceTest` |
| M6 `isStone` keys on the material | `StoneIdentityTest` + ST16 | **KILLED** by `StoneIdentityTest` |
| M7 the Q-swing guard removed | `SwingGuardTest` + ST13 | **KILLED** by `SwingGuardTest` |
| M8 `MUTWELDDEFAULT`: convergence ignores the chosen slot | `ConvergePlanTest` (the row that did not exist before this slice) | **KILLED** by `ConvergePlanTest` |
| M9 a pool naming an unknown ability loads | `PoolLoaderTest` | **KILLED** by `PoolLoaderTest` |
| the `BlockDamageEvent` cancel deleted | ST3 only | **owed to the boot** -- no unit test can see a Bukkit event handler |
| the `onPrePlayerAttack` stone cancel deleted | ST4 only | **owed to the boot** |
