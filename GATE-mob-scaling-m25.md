# GATE — Mob scaling M25: flat overworld and Nether gear score, the new End curve

**Status: READ 2026-09-28 on `210f5b8f` -- 7 of 33 PASS: R0a-c, M25-OW, M25-NETHER, M25-END-C and M25-END-11K.**
- **The boot:** it began at 04:22:34 (`Build: 210f5b8f`). Ben's LOGIN is at 04:24:26, and he wore no armour.
- **M25-SPAWN:** the End half was reported as 400 by Ben but has no attributable log line (three enderman seeds
  share its second); the overworld half was NOT READ.
- **Everything else is NOT READ and CARRIED FORWARD:** G10, the other carried rows and the 14 probe rows. Ben ended
  the session after the M25 rows.
- **Witnesses:** the M25 PASSes rest on `MOBSEED` log lines, because `/rpg mobinfo` prints to chat only.
- **Stop:** through the console file at 04:29:57, clean, with zero `java.exe` afterwards.

(Before the boot, this line read "**Status: NOT RUN.**"; every prediction below was written **before** any boot of this branch, and **this file is
COMMITTED before the boot**, so a prediction can be checked against origin for having existed first.) Readings go
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
| `[Rpg] Build: <sha>`, the PR's head as `gh pr view <n> --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS.** `[04:22:39] [Server thread/INFO]: [Rpg] Build: 210f5b8f`, a bare hash. The tree was clean and local equalled origin before the build |

### R0b — the jar carries M25

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `DimensionGearScore`, `GearScoreSource`, `MeleeSeed`, `MobDamagePricing`. ABSENT: `DistanceGearScore` (renamed) and the control | the scan below | **PASS.** 804 entries: `DimensionGearScore`, `GearScoreSource`, `MeleeSeed` and `MobDamagePricing` PRESENT; `DistanceGearScore` and the control ABSENT |

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
| `Loaded 8 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 2 pools, 6 fragments, 4 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`, as at `ad686c9`, because this branch changes no content. **No** `Refusing`, `Skipping`, `SEVERE` or exception. The known WARNs are `volley_stone`'s 27-tick cooldown and its summary line | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | **PASS.** `[04:22:39] [Server thread/INFO]: [Rpg] Loaded 8 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 2 pools, 6 fragments, 4 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. No `Refusing`, `Skipping`, `SEVERE` or exception; the WARNs are `volley_stone`'s and its summary line |

---

## THE M25 ROWS — NATURAL SPAWNS, READ FROM THE `MOBSEED` LINE (SURVIVAL)

**Why these cannot pass on the old build:** at every chosen position the pre-M25 curves give a different number.
Each row names what the old curve would read.

### M25-OW — a far overworld hostile spawns at [100] (SURVIVAL)

| prediction | READING |
|---|---|
| `/tp @s 10000 ~ 10000`, at night (`/time set night`), and wait for a hostile to spawn naturally. **Its `MOBSEED` line reads `gs=100 source=rolled`**, and its plate reads `[100]`. **Pre-M25 it would read `gs=500`** (≈14,000 blocks from spawn, past the old 10,000-block cap) | **PASS.** READ 2026-09-28 on `210f5b8f` (boot 04:22:34, Ben's LOGIN 04:24:26; no armour: `equipment` absent at 04:24:41 with the `Health 20.0f` control). After `/tp @s 10000 100 10000` (04:25:31), **every one of 120 hostile seeds reads `gs=100 source=rolled`**: 73 guardian, 3 elder_guardian, 13 skeleton, 11 creeper, 9 drowned, 8 zombie, 2 spider, 1 zombie_villager, and none at any other value. VERBATIM, the drowned Ben named: `[04:25:32] [Rpg] MOBSEED 43822ba8-c8da-498d-aef4-8312533e281f drowned gs=100 source=rolled max=100`. Pre-M25 these would read 500. Ben's plate reading: a drowned at `[100]` |

### M25-NETHER — a far Nether hostile spawns at [200] (SURVIVAL)

| prediction | READING |
|---|---|
| `/execute in minecraft:the_nether run tp @s 2000 ~ 2000`, and wait for a natural hostile (a zombified piglin, a piglin, a ghast, …). **`MOBSEED … gs=200 source=rolled`**, plate `[200]`. **Pre-M25 it would read `gs=500`** (≈2,800 blocks, past the old 937.5-block cap) | **PASS.** READ 2026-09-28 on `210f5b8f` (boot 04:22:34, Ben's LOGIN 04:24:26; no armour: `equipment` absent at 04:24:41 with the `Health 20.0f` control). After `/execute in minecraft:the_nether run tp @s 2000 100 2000` (04:26:40), **every Nether-type seed reads `gs=200 source=rolled`**: 63 zombified_piglin, 9 piglin, 1 ghast. VERBATIM: `[04:26:41] [Rpg] MOBSEED 0e428485-cfc7-44a3-b7f8-4c485a2725c9 piglin gs=200 source=rolled max=160`. Pre-M25: 500. (A guardian and a zombie at `gs=100` in the same window are overworld mobs from chunks still loaded there, which fits M25-OW.) Ben's plate reading: a piglin at `[200]` |

### M25-END-C — an End hostile near the centre spawns at [300] (SURVIVAL)

| prediction | READING |
|---|---|
| On the End's main island, within ~150 blocks of (0, 0), a naturally spawned enderman: **`MOBSEED … enderman gs=300 source=rolled`**, exactly 300 (M25: flat to 1,000 blocks). **Pre-M25 it would read `300 + d/25`**, e.g. 304 at 100 blocks, so **a reading above 300 here is the old curve** | **PASS.** READ 2026-09-28 on `210f5b8f` (boot 04:22:34, Ben's LOGIN 04:24:26; no armour: `equipment` absent at 04:24:41 with the `Health 20.0f` control). After `/execute in minecraft:the_end run tp @s 0 100 0` (04:27:22), **all 81 enderman seeds read exactly `gs=300 source=rolled`, and none above 300**. VERBATIM: `[04:27:23] [Rpg] MOBSEED e9ca1072-581c-40b0-a5b1-04081672af4c enderman gs=300 source=rolled max=600`. The pre-M25 curve would have spread them over 300 to ~306. Ben's plate reading: an enderman at `[300]` |

### M25-END-11K — an End hostile ~11,000 blocks out spawns at [400] (SURVIVAL)

| prediction | READING |
|---|---|
| `/execute in minecraft:the_end run tp @s 7778 ~ 7778` (onto an outer island), and wait for a natural enderman. **`MOBSEED … enderman gs=` a value from 399 to 401, `source=rolled`.** **Basis, computed with awk, not predicted:** the teleport point is 11,000.46 blocks from (0, 0), which gives exactly **400**. A natural spawn lands 24–128 blocks from the player, so its distance runs ~10,872 to ~11,128, giving 398.72 to 401.28, which round to **399–401**. **It must also equal `/rpg mobinfo`'s "would roll" for that mob**, which is computed from the mob's own position. **Pre-M25 it would read `gs=500`** (the old End curve capped at 5,000 blocks) | **PASS.** READ 2026-09-28 on `210f5b8f` (boot 04:22:34, Ben's LOGIN 04:24:26; no armour: `equipment` absent at 04:24:41 with the `Health 20.0f` control). After `/execute in minecraft:the_end run tp @s 7778 100 7778` (04:27:57), **76 enderman seeds: 73 at `gs=400`, 1 at `399`, 2 at `401`, all `source=rolled`**, inside the predicted 399–401. **POSITION-MATCHED, per the seat's instruction**: the console's nearest-enderman reads at 04:28:25 gave `Health 40.0f` (the control), `Pos [7772.664819939682d, 60.0d, 7779.304682057455d]` and `UUID [I; -803743291, 491014616, -1529779573, 200836382]`, which is `d017d9c5-1d44-49d8-a4d1-6a8b0bf8851e`. Its distance is 10,996.90 blocks, so the prediction is **400 ± 1** (computed with awk). VERBATIM, its own seed: `[04:27:57] [Rpg] MOBSEED d017d9c5-1d44-49d8-a4d1-6a8b0bf8851e enderman gs=400 source=rolled max=800`. Caveat: the Pos was read 28 s after the seed, and endermen wander, so it is the mob's position at the read, not at the seed. Pre-M25: 500 |

### M25-SPAWN — `/rpg spawn` with no [gs] reads the new curve (SURVIVAL)

| prediction | READING |
|---|---|
| At the M25-OW position, `/rpg spawn zombie` (no score): **`MOBSEED … zombie gs=100 source=rolled`**. At the M25-END-11K teleport point (don't move after the `/tp`), `/rpg spawn enderman`: **`MOBSEED … enderman gs=400 source=rolled`** exactly, because the mob is at Ben's feet, 11,000.46 blocks from (0, 0) (400 by the awk computation above). **Pre-M25: 500 and 500** | **END HALF: REPORTED 400 BY BEN, BUT NO ATTRIBUTABLE LOG LINE, SO NOT A PASS under the 2026-09-28 ruling.** READ 2026-09-28 on `210f5b8f` (boot 04:22:34, Ben's LOGIN 04:24:26; no armour: `equipment` absent at 04:24:41 with the `Health 20.0f` control). `/rpg spawn enderman` at 04:28:23. Ben had landed, so it was not the teleport point: his Pos at 04:28:53 was (7775.55, 59, 7779.69), 10,999.22 blocks out, predicting 400. **Three enderman seeds share that second**: `91f2951f-… gs=401`, `ad457cae-… gs=401` and `16cd3042-4eef-48f6-9c5d-d59acda78f65 gs=400`. A console read by UUID at 04:29:42 found the first two gone and the third at (7789.8, 7800.8), 25 blocks from Ben, so which seed was the `/rpg spawn` cannot be shown. Ben's chat reading was 400. **OVERWORLD HALF: NOT READ**; no `/rpg spawn zombie` was issued. Both halves are CARRIED FORWARD |

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
| `/rpg spawn skeleton 300` at range (15+ blocks gives the arrow more flight time). The moment it looses, `/kill @e[type=skeleton,limit=1,sort=nearest]`. The arrow lands: **`MOBHIT … direct=arrow causing=none gs=300 from=DIRECT ratio=15.000`**. Skipped by Ben on `ad686c9` (the kill could not be timed); covered by `MobDamagePricingTest.aShooterThatIsGoneStillPricesAtItsStamp`. Unaffected by M25 (an explicit [gs]) | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |

### G8b — melee at GS 300 is x15 (SURVIVAL) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn zombie 300`, one hit at max 100: **`MOBHIT cause=ENTITY_ATTACK … gs=300 … ratio=15.000`** | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |

### GX-ENV — a player's environmental damage did NOT change (SURVIVAL) — CARRIED

| prediction | READING |
|---|---|
| At max 400 (`/rpg healthboost 300`, held), fall far enough to take damage. **No `MOBHIT` line**, and the loss is a share of max: a fall of vanilla amount `a` takes `a x 400/20` | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |

### G13 — the GS survives a restart (437) (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| Near spawn, `/rpg spawn zombie 437`, plate `[437]`. Session B restarts through the console file (no `--refresh-content`). Rejoin: still **`[437]`**, and the `MOBSEED` for its uuid after the restart reads **`gs=437 source=stored`**. Under M25 nothing near spawn can roll 437 (the overworld is always 100), so a re-roll cannot fake it | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |

### G12 — the GS survives a chunk unload (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn zombie 222`, fly several hundred blocks away and come back. Still **`[222]`**, with **`MOBREMOVE <uuid>`** then **`MOBSEED <uuid> … gs=222 source=stored`** in the log | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |

### G14 — a mob through a Nether portal keeps its GS (SURVIVAL) — REWRITTEN under M25

| prediction | READING |
|---|---|
| `/rpg spawn zombie 140`, and get it to walk or be pushed through a Nether portal. In the Nether it still reads **`[140]`** (`MOBSEED … gs=140 source=stored` on arrival), while `mobinfo`'s "would roll" there reads **exactly 200** (M25: flat). *Rewritten: this said "at least 200" under the old curve.* Full HP on arrival is §6 F1, and expected | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |

### G15 — a stored GS of 0 is re-rolled (CREATIVE) — REWRITTEN under M25

| prediction | READING |
|---|---|
| `/summon zombie ~ ~ ~ {BukkitValues:{"rpg:mob_gear_score":0}}` anywhere in the overworld: a WARN naming the invalid `mob_gear_score`, then **`MOBSEED … gs=100 source=rolled`**, exactly 100. *Rewritten: this said "~[100]" under the old curve, which only approximated 100 near spawn* | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |

### G15b — a stored GS of 900 is re-rolled, not clamped (CREATIVE) — REWRITTEN under M25

| prediction | READING |
|---|---|
| The same with `900`: a WARN, then **`gs=100 source=rolled`**, NOT 900 and NOT 500 | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |

### G16 — conversions inherit the GS (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn slime 300`. **Re-spawn until it is size 4**: `execute at BaronVonYeetus run data get entity @e[type=slime,limit=1,sort=nearest] Size` prints **`3`** (`Size` is saved as size − 1), after a `Health` read as the control. Kill it: **each child's `MOBSEED` reads `gs=300 source=stored`**. And/or a GS 300 zombie drowned until it converts: the drowned's `MOBSEED` reads `gs=300 source=stored` | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |

### G11 — environmental damage on a mob is proportional (M13) (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| Drop a `/rpg spawn zombie` and a `/rpg spawn zombie 500` from ~25 blocks: **both die** | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |

### G18 — the Knell in lava: proportional (M14) (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn knell` and `/rpg spawn knell 200` in lava for the same few seconds lose **about the same FRACTION** of their bar, read with `mobinfo` before and after | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |

### THE PROBE ROWS — CARRIED, every one with an explicit [gs], so M25 does not touch them (SURVIVAL)

Each row: spawn at the named GS, take the hit at max 100, and read the `MOBHIT` line. Predictions as in
`GATE-mob-scaling-2.md` (P-SHULKER PASSED there and is not carried).

| row | set-up (GS) | predicted `direct` / `causing` / `from` | predicted `ratio` | READING |
|---|---|---|---|---|
| **P-ARROW** | `skeleton 200` (spawns with its bow since F16c) | `arrow` / `skeleton` / `DIRECT` | **10.000** | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-TRIDENT** | `drowned 300`, re-spawned until one holds a trident (rolled since F16c) | `trident` / `drowned` / `DIRECT` | **15.000** | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-BLAZE** | `blaze 300` | `small_fireball` / `blaze` / `DIRECT` | **15.000** | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-GHAST** | `ghast 300` | `fireball` / `ghast` / `DIRECT`; the blast is a second line from the same stamp | **15.000** each | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-SKULL** | `wither 300` | `wither_skull` / `wither` / `DIRECT` | **15.000** | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-SPIT** | an angered llama (passive) | `llama_spit` / `llama` / `CAUSING`, `gs=-` | **5.000** | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-CREEPER** | `creeper 200` | `creeper` / `creeper` / `DIRECT` | **10.000** | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-WITCH** | `witch 300`, a harming splash | the potion / `witch` / `DIRECT` | **15.000** | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-BOOM** | `warden 300`, sonic boom | `warden` / `warden` / `DIRECT` | **15.000** | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-FANGS** | `evoker 300`, fangs | `evoker_fangs` / `evoker` / **`CAUSING`** | **15.000** | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-GUARDIAN** | `guardian 300`, beam | `guardian` / `guardian` / `DIRECT` | **15.000** | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-BREATH** | the dragon's breath cloud, if reachable | **UNPREDICTED**: `CAUSING` from the dragon if the cloud names its owner, else no MOBHIT (a named finding) | — | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-WIND** | `breeze 300`, wind charge | `breeze_wind_charge` / `breeze` / `DIRECT` | **15.000** | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-DOT** | a cave spider's poison | **NO `MOBHIT` line for the poison ticks** (F5) | — | **NOT READ on `210f5b8f`**: not attempted. Ben ended the session after the M25 rows ("looks good, let's wrap it up"). **CARRIED FORWARD**, not waived. |
