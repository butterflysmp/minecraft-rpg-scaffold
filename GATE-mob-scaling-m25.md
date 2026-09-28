# GATE — Mob scaling M25: flat overworld and Nether gear score, the new End curve

**Status: NOT RUN.** Every prediction below was written **before** any boot of this branch, and **this file is
COMMITTED before the boot**, so a prediction can be checked against origin for having existed first. Readings go
**beside** a prediction, never over it, and a prediction is not edited once its row has been read. **Readings count
only if Ben's LOGIN appears in THIS boot's log.** The seat's 2026-09-28 ruling applies: a row whose prediction is a
LOG LINE is PASS when that line appears verbatim after the LOGIN, named by Ben or not. "Not itemised" is only for
rows that rest on Ben's own observation.

```
ROWS     33   R0a R0b R0c
              M25-OW M25-NETHER M25-END-C M25-END-11K M25-SPAWN
              G10 G8b GX-ENV G13 G12 G14 G15 G15b G16 G11 G18
              P-ARROW P-TRIDENT P-BLAZE P-GHAST P-SKULL P-SPIT P-CREEPER P-WITCH P-BOOM P-FANGS P-GUARDIAN
              P-BREATH P-WIND P-DOT
         ──
         33   = 19 headings   git grep -c '^### R0\|^### M25-\|^### G' <ref> -- GATE-mob-scaling-m25.md
              + 14 probe rows git grep -c '^| \*\*P-' <ref> -- GATE-mob-scaling-m25.md
```

**Plan:** `PLAN-mob-scaling.md` §0 **M25** (Ben, 2026-09-28) and the seat's defaults stated there.

## Set-up

- **Every row is SURVIVAL** unless it says otherwise. An op player, **no armour** (Session B checks it from the
  console with a `Health` control first), **`/gamemode survival`**.
- **`/rpg mobtrace` first.** Each seed then logs `MOBSEED <uuid> <type> gs=<n> source=<rolled|stored|passive>
  max=<n>`, and each mob hit logs a `MOBHIT` line. **The M25 rows read the `MOBSEED` line**, which the server writes
  whether or not anyone looks at a nameplate.
- **The world's difficulty does not matter to any M25 row.** A GS is rolled from position and dimension only.
- **Existing mobs keep their stored GS (M5).** A mob seeded before this build still shows its old GS, which is the
  seat's accepted default. **Every M25 row therefore reads a mob seeded on THIS boot** (`source=rolled` in its
  `MOBSEED` line, after the LOGIN).

---

## R0 — THE DEPLOYED BUILD CARRIES THIS BRANCH. IF ANY R0 FAILS, STOP

### R0a — the build line names the head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, the PR's head as `gh pr view <n> --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries M25

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `DimensionGearScore`, `GearScoreSource`, `MeleeSeed`, `MobDamagePricing`. ABSENT: `DistanceGearScore` (renamed) and the control | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/mob/DimensionGearScore.class',
               'io/github/butterflysmp/rpg/core/mob/GearScoreSource.class',
               'io/github/butterflysmp/rpg/core/mob/MeleeSeed.class',
               'io/github/butterflysmp/rpg/core/mob/MobDamagePricing.class',
               'io/github/butterflysmp/rpg/core/mob/DistanceGearScore.class',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — the boot log: content loads, nothing refused

| prediction | instrument | READING |
|---|---|---|
| `Loaded 8 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 2 pools, 6 fragments, 4 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`, as at `ad686c9`, because this branch changes no content. **No** `Refusing`, `Skipping`, `SEVERE` or exception. The known WARNs are `volley_stone`'s 27-tick cooldown and its summary line | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | |

---

## THE M25 ROWS — NATURAL SPAWNS, READ FROM THE `MOBSEED` LINE (SURVIVAL)

**Why these cannot pass on the old build:** at every chosen position the pre-M25 curves give a different number.
Each row names what the old curve would read.

### M25-OW — a far overworld hostile spawns at [100] (SURVIVAL)

| prediction | READING |
|---|---|
| `/tp @s 10000 ~ 10000`, at night (`/time set night`), and wait for a hostile to spawn naturally. **Its `MOBSEED` line reads `gs=100 source=rolled`**, and its plate reads `[100]`. **Pre-M25 it would read `gs=500`** (≈14,000 blocks from spawn, past the old 10,000-block cap) | |

### M25-NETHER — a far Nether hostile spawns at [200] (SURVIVAL)

| prediction | READING |
|---|---|
| `/execute in minecraft:the_nether run tp @s 2000 ~ 2000`, and wait for a natural hostile (a zombified piglin, a piglin, a ghast, …). **`MOBSEED … gs=200 source=rolled`**, plate `[200]`. **Pre-M25 it would read `gs=500`** (≈2,800 blocks, past the old 937.5-block cap) | |

### M25-END-C — an End hostile near the centre spawns at [300] (SURVIVAL)

| prediction | READING |
|---|---|
| On the End's main island, within ~150 blocks of (0, 0), a naturally spawned enderman: **`MOBSEED … enderman gs=300 source=rolled`**, exactly 300 (M25: flat to 1,000 blocks). **Pre-M25 it would read `300 + d/25`**, e.g. 304 at 100 blocks, so **a reading above 300 here is the old curve** | |

### M25-END-11K — an End hostile ~11,000 blocks out spawns at [400] (SURVIVAL)

| prediction | READING |
|---|---|
| `/execute in minecraft:the_end run tp @s 7778 ~ 7778` (onto an outer island), and wait for a natural enderman. **`MOBSEED … enderman gs=` a value from 399 to 401, `source=rolled`.** **Basis, computed with awk, not predicted:** the teleport point is 11,000.46 blocks from (0, 0), which gives exactly **400**. A natural spawn lands 24–128 blocks from the player, so its distance runs ~10,872 to ~11,128, giving 398.72 to 401.28, which round to **399–401**. **It must also equal `/rpg mobinfo`'s "would roll" for that mob**, which is computed from the mob's own position. **Pre-M25 it would read `gs=500`** (the old End curve capped at 5,000 blocks) | |

### M25-SPAWN — `/rpg spawn` with no [gs] reads the new curve (SURVIVAL)

| prediction | READING |
|---|---|
| At the M25-OW position, `/rpg spawn zombie` (no score): **`MOBSEED … zombie gs=100 source=rolled`**. At the M25-END-11K teleport point (don't move after the `/tp`), `/rpg spawn enderman`: **`MOBSEED … enderman gs=400 source=rolled`** exactly, because the mob is at Ben's feet, 11,000.46 blocks from (0, 0) (400 by the awk computation above). **Pre-M25: 500 and 500** | |

---

## CARRIED FORWARD — EVERY STILL-UNREAD ROW, WITH ITS M25 VERDICT

These were unread on slice 1 (`GATE-mob-scaling-1.md`) and/or slice 2 (`GATE-mob-scaling-2.md`, merged as
`9cfd3a5` with G10 carried forward). **Each carries a verdict: CARRIED (the prediction stands), REWRITTEN under M25,
or RETIRED.**

**RETIRED, superseded by M25, and NOT rows here:** slice 1's **G19** (12,000 out → `[500]`), **G6** (Nether
`[200]`/`[360]`/`[500]`), **G2** (2,500 out → `[200]`) and **G1b**'s 2,500-block spider. Each was read PASS-as-reported
against the old curve, and M25-OW, M25-NETHER and M25-SPAWN are the rows that read the new one. Slice 1's **G7** (the
End centre `[300]`) is **rewritten** as M25-END-C.

### G10 — the shooter dies mid-flight; the arrow keeps its stamp (SURVIVAL) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn skeleton 300` at range (15+ blocks gives the arrow more flight time). The moment it looses, `/kill @e[type=skeleton,limit=1,sort=nearest]`. The arrow lands: **`MOBHIT … direct=arrow causing=none gs=300 from=DIRECT ratio=15.000`**. Skipped by Ben on `ad686c9` (the kill could not be timed); covered by `MobDamagePricingTest.aShooterThatIsGoneStillPricesAtItsStamp`. Unaffected by M25 (an explicit [gs]) | |

### G8b — melee at GS 300 is x15 (SURVIVAL) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn zombie 300`, one hit at max 100: **`MOBHIT cause=ENTITY_ATTACK … gs=300 … ratio=15.000`** | |

### GX-ENV — a player's environmental damage did NOT change (SURVIVAL) — CARRIED

| prediction | READING |
|---|---|
| At max 400 (`/rpg healthboost 300`, held), fall far enough to take damage. **No `MOBHIT` line**, and the loss is a share of max: a fall of vanilla amount `a` takes `a x 400/20` | |

### G13 — the GS survives a restart (437) (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| Near spawn, `/rpg spawn zombie 437`, plate `[437]`. Session B restarts through the console file (no `--refresh-content`). Rejoin: still **`[437]`**, and the `MOBSEED` for its uuid after the restart reads **`gs=437 source=stored`**. Under M25 nothing near spawn can roll 437 (the overworld is always 100), so a re-roll cannot fake it | |

### G12 — the GS survives a chunk unload (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn zombie 222`, fly several hundred blocks away and come back. Still **`[222]`**, with **`MOBREMOVE <uuid>`** then **`MOBSEED <uuid> … gs=222 source=stored`** in the log | |

### G14 — a mob through a Nether portal keeps its GS (SURVIVAL) — REWRITTEN under M25

| prediction | READING |
|---|---|
| `/rpg spawn zombie 140`, and get it to walk or be pushed through a Nether portal. In the Nether it still reads **`[140]`** (`MOBSEED … gs=140 source=stored` on arrival), while `mobinfo`'s "would roll" there reads **exactly 200** (M25: flat). *Rewritten: this said "at least 200" under the old curve.* Full HP on arrival is §6 F1, and expected | |

### G15 — a stored GS of 0 is re-rolled (CREATIVE) — REWRITTEN under M25

| prediction | READING |
|---|---|
| `/summon zombie ~ ~ ~ {BukkitValues:{"rpg:mob_gear_score":0}}` anywhere in the overworld: a WARN naming the invalid `mob_gear_score`, then **`MOBSEED … gs=100 source=rolled`**, exactly 100. *Rewritten: this said "~[100]" under the old curve, which only approximated 100 near spawn* | |

### G15b — a stored GS of 900 is re-rolled, not clamped (CREATIVE) — REWRITTEN under M25

| prediction | READING |
|---|---|
| The same with `900`: a WARN, then **`gs=100 source=rolled`**, NOT 900 and NOT 500 | |

### G16 — conversions inherit the GS (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn slime 300`. **Re-spawn until it is size 4**: `execute at BaronVonYeetus run data get entity @e[type=slime,limit=1,sort=nearest] Size` prints **`3`** (`Size` is saved as size − 1), after a `Health` read as the control. Kill it: **each child's `MOBSEED` reads `gs=300 source=stored`**. And/or a GS 300 zombie drowned until it converts: the drowned's `MOBSEED` reads `gs=300 source=stored` | |

### G11 — environmental damage on a mob is proportional (M13) (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| Drop a `/rpg spawn zombie` and a `/rpg spawn zombie 500` from ~25 blocks: **both die** | |

### G18 — the Knell in lava: proportional (M14) (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn knell` and `/rpg spawn knell 200` in lava for the same few seconds lose **about the same FRACTION** of their bar, read with `mobinfo` before and after | |

### THE PROBE ROWS — CARRIED, every one with an explicit [gs], so M25 does not touch them (SURVIVAL)

Each row: spawn at the named GS, take the hit at max 100, and read the `MOBHIT` line. Predictions as in
`GATE-mob-scaling-2.md` (P-SHULKER PASSED there and is not carried).

| row | set-up (GS) | predicted `direct` / `causing` / `from` | predicted `ratio` | READING |
|---|---|---|---|---|
| **P-ARROW** | `skeleton 200` (spawns with its bow since F16c) | `arrow` / `skeleton` / `DIRECT` | **10.000** | |
| **P-TRIDENT** | `drowned 300`, re-spawned until one holds a trident (rolled since F16c) | `trident` / `drowned` / `DIRECT` | **15.000** | |
| **P-BLAZE** | `blaze 300` | `small_fireball` / `blaze` / `DIRECT` | **15.000** | |
| **P-GHAST** | `ghast 300` | `fireball` / `ghast` / `DIRECT`; the blast is a second line from the same stamp | **15.000** each | |
| **P-SKULL** | `wither 300` | `wither_skull` / `wither` / `DIRECT` | **15.000** | |
| **P-SPIT** | an angered llama (passive) | `llama_spit` / `llama` / `CAUSING`, `gs=-` | **5.000** | |
| **P-CREEPER** | `creeper 200` | `creeper` / `creeper` / `DIRECT` | **10.000** | |
| **P-WITCH** | `witch 300`, a harming splash | the potion / `witch` / `DIRECT` | **15.000** | |
| **P-BOOM** | `warden 300`, sonic boom | `warden` / `warden` / `DIRECT` | **15.000** | |
| **P-FANGS** | `evoker 300`, fangs | `evoker_fangs` / `evoker` / **`CAUSING`** | **15.000** | |
| **P-GUARDIAN** | `guardian 300`, beam | `guardian` / `guardian` / `DIRECT` | **15.000** | |
| **P-BREATH** | the dragon's breath cloud, if reachable | **UNPREDICTED**: `CAUSING` from the dragon if the cloud names its owner, else no MOBHIT (a named finding) | — | |
| **P-WIND** | `breeze 300`, wind charge | `breeze_wind_charge` / `breeze` / `DIRECT` | **15.000** | |
| **P-DOT** | a cave spider's poison | **NO `MOBHIT` line for the poison ticks** (F5) | — | |
