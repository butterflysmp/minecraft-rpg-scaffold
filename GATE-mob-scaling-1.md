# GATE — Mob scaling, slice 1: health x5 x GS/100, the stored gear score, the [GS] nameplate

> **M25 NOTE, 2026-09-28. Written beside the readings, never over them.** Ben's M25 replaced the distance
> curves this gate was written against: the overworld is a flat GS 100, the Nether a flat 200, and the End
> 300 to 1,000 blocks from (0, 0), then +100 per 10,000. Verdicts, and no prediction below is edited:
> - **RETIRED, superseded by M25:** **G19** (12,000 out → `[500]`, now `[100]`), **G6** (`[200]`/`[360]`/`[500]`,
>   now `[200]` at all three), **G2** (2,500 out → `[200]`, now `[100]`), and **G1b**'s 2,500-block spider
>   (`[200]`, now `[100]`). Their PASS-as-reported readings stand for the curve they were read against.
> - **Still true under M25:** **G7**'s End centre `[300]`, G1, G1b's spawn spider, G3, G4 and G5.
> - **Carried into `GATE-mob-scaling-m25.md`** (unread here, and still unread after slice 2): G13, G12, G14,
>   G15, G15b, G16, G11 and G18, with G14 and G15/G15b REWRITTEN there for M25. GX-MELEE became slice 2's G8,
>   which PASSED.

**Status: READ 2026-09-27 on `e946882` -- 12 of 21 PASS, 9 NOT READ.** R0a/R0b/R0c PASS (read by Session B
from the 19:35:09 boot). Ben's LOGIN is in that boot's log (`BaronVonYeetus joined the game` at 19:40:48), so
readings count. **The 9 row PASSes are AS REPORTED by Ben**, in one multi-select: G1 G3 G5 G19 G6 G7 G1b G2 G4. Not
named, so NOT READ: G13 G12 G14 G15 G15b G16 G11 G18 GX-MELEE. Ben ended the boot himself with an in-game `/stop`
at 19:47:35. It was a clean shutdown (every dimension saved), and afterwards zero `java.exe` were running.

**What the log shows beside the reported PASSes. Read this before crediting any witness below.** This boot logs
every command (`spigot.yml` `commands.log: true`: `/time`, `/tp`, `/kill`, `/gamemode` and `/summon` all appear).
It shows **no `/rpg spawn` and no `/rpg mobinfo` at all.** The commands after login were `/time set night`,
`/tp 2000 100 2000`, `/tp 20000 100 20000` (three times), `/kill` (three times), `/gamemode spectator|creative`, and
`/summon minecraft:warden` at 19:45:04. So the readings came from nameplates on naturally spawned or `/summon`ed
mobs. **None of the `mobinfo` witnesses in the predictions was taken**: G3's vanilla `MAX_HEALTH` 500.0, G1b's 16.0,
the origin lines in G6 and G7, and the "would roll" values. G5's `/rpg spawn cow 300` refusal was not exercised
either. The log does not record portal travel, so it neither shows nor rules out the Nether and End visits in G6/G7.
It has no `MOBSEED`/`MOBREMOVE` line and no `mob_gear_score` WARN, which fits G12 and G15 being NOT READ. The seat
directed that named rows go down as PASS as reported, and this note is what the log adds to them.

Every prediction below was written **before** the boot, from `PLAN-mob-scaling.md` §4. The
file was UNTRACKED at boot time, so the build stayed exactly `e946882`. It is committed together with the readings,
and the file's timestamp is earlier than the boot's R0a line. Readings go **beside** a prediction, never over it, and
a prediction is not edited once its row has been read. **Readings count only if Ben's LOGIN appears in THIS boot's
log (`e946882`).**

```
ROWS     21   R0a R0b R0c G1 G3 G5 G19 G6 G7 G13 G1b G2 G4 G11 G12 G14 G15 G15b G16 G18 GX-MELEE
         ──
         21   = git grep -c '^### R0\|^### G' <ref> -- GATE-mob-scaling-1.md
```

**Plan:** `PLAN-mob-scaling.md` §3 slice 1 and §4. Rulings M1-M23 (§0).

**Declared game mode, PER ROW:**
- **CREATIVE** for the nameplate and number rows, which read a plate or `/rpg mobinfo` and never damage the player.
- **SURVIVAL** for G14, where the mob must walk through the portal on its own.

Creative removes costs that no row here reads, so creative is safe for those rows.

**Boot:** `./scripts/dev-server.sh --refresh-content` on `feat/mob-scaling-1` at `e946882`. Restarts inside a row
(G13) are WITHOUT the flag, through the console file.

**Set-up shared by most rows:**
- an op player;
- `/rpg mobinfo` looks at the mob in your crosshair, and prints its type, HOSTILE/PASSIVE, the stored GS, the GS
  its position WOULD roll, the origin used, its custom HP and attack, and the VANILLA `MAX_HEALTH` and
  `ATTACK_DAMAGE`;
- `/rpg spawn <mob> [gs]` spawns at your feet. Without `[gs]` the mob rolls from its position; `[gs]` is 1..500;
- `/rpg mobtrace` toggles `MOBSEED`/`MOBREMOVE` lines in the log (G12).

**MOB MELEE IS STILL VANILLA STRENGTH IN THIS SLICE, AND THAT IS EXPECTED.** Health scales now; mob damage (M1's x5,
M16's flat pricing) is slice 2. A zombie with 100 HP that hits like a vanilla zombie is correct for this build,
not a defect.

---

## R0 — THE DEPLOYED BUILD CARRIES THIS SLICE. IF ANY R0 FAILS, STOP

### R0a — the build line names the tip

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: e946882` -- not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS.** `[19:35:09] [Server thread/INFO]: [Rpg] Build: e946882`, a bare hash. `git diff --quiet HEAD` passed before the build, and `dev-server.sh` echoed `==> Building e946882` |

### R0b — the jar carries the slice

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `MobScaling`, `DistanceGearScore`, `MobGearScore`, `MobDimension`, `GearScoreSource`; paper `MobClassifier`, `MobOrigin`. ABSENT: the Recall tuning spike's `TuningLog`, and the control | the scan below | **PASS.** Run against the jar deployed for the 19:35:09 boot (793 entries): the 7 slice classes are `PRESENT`, and `TuningLog` and `NoSuchClassControl` are `ABSENT` |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/mob/MobScaling.class',
               'io/github/butterflysmp/rpg/core/mob/DistanceGearScore.class',
               'io/github/butterflysmp/rpg/core/mob/MobGearScore.class',
               'io/github/butterflysmp/rpg/core/mob/MobDimension.class',
               'io/github/butterflysmp/rpg/core/mob/GearScoreSource.class',
               'io/github/butterflysmp/rpg/paper/health/MobClassifier.class',
               'io/github/butterflysmp/rpg/paper/health/MobOrigin.class',
               'io/github/butterflysmp/rpg/paper/adapter/TuningLog.class',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — the boot log: content loads, nothing refused, no seed errors

| prediction | instrument | READING |
|---|---|---|
| the `Loaded ...` line reads `8 abilities`, `2 pools, 6 fragments, 4 aspects` and `1 mobs` (as master `de999fa`; this slice adds no content). **No** `Refusing` or `Skipping` line, and **no** `SEVERE` or exception mentioning `MobScaling`, `gear score` or `mob_gear_score`. The one known content WARN (`volley_stone`'s 27-tick cooldown) is pre-existing | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | **PASS.** `[19:35:09] [Server thread/INFO]: [Rpg] Loaded 8 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 2 pools, 6 fragments, 4 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. No `Refusing`, `Skipping`, `SEVERE` or `Exception` line in the boot. The WARNs are `volley_stone`'s 27-tick one plus its `1 content problem(s)` summary, and Paper's "2 releases behind 26.2" banner |

---

## THE ROWS — the seat's seven first

### G1 — a zombie at overworld spawn: "[100] Zombie 100/100 ❤" (CREATIVE)

| prediction | READING |
|---|---|
| Within ~10 blocks of overworld spawn, `/rpg spawn zombie` (no `[gs]`). The plate reads **`[100] Zombie 100/100 ❤`**: the `[100]` in **AQUA (light blue)**, "Zombie" and the numbers in their usual colour, the heart still red (M22). `/rpg mobinfo`: HOSTILE, stored GS 100, vanilla `MAX_HEALTH` 20.0. A zombie's GS equals its max at every GS, so this row cannot tell those two apart on its own; G1b and G3 can | **PASS as reported by Ben** (one multi-select, 2026-09-27). No `/rpg` command in the log, see the Status note. log: nameplate only, no `/rpg spawn` and no `mobinfo` |

### G3 — a Warden at spawn: 2500/2500, no attribute error (CREATIVE)

| prediction | READING |
|---|---|
| `/rpg spawn warden` near spawn. The plate reads **`[100] Warden 2500/2500 ❤`**. **`/rpg mobinfo` shows vanilla `MAX_HEALTH` = 500.0, NOT 2500** -- that attribute READ is the witness for M9, because "no attribute error" would pass for free (nothing writes the attribute). No `SEVERE`, and no attribute error in the log | **PASS as reported by Ben** (one multi-select, 2026-09-27). No `/rpg` command in the log, see the Status note. log: NO `SEVERE` and no attribute line anywhere in the boot. The Warden is `/summon minecraft:warden` at 19:45:04 (after a `/kill` respawn), not `/rpg spawn`, and no `mobinfo` was issued, so **the vanilla `MAX_HEALTH` 500.0 read, the M9 witness, was NOT taken** |

### G5 — a cow: 50 HP, no GS (CREATIVE)

| prediction | READING |
|---|---|
| A cow near spawn: **`Cow 50/50 ❤`**, with no `[..]` prefix at all (M7). `/rpg mobinfo`: PASSIVE, stored GS `none`. `/rpg spawn cow 300` is **refused** with a red "passive mob ... takes no gear score" line | **PASS as reported by Ben** (one multi-select, 2026-09-27). No `/rpg` command in the log, see the Status note. log: `/rpg spawn cow 300` was never issued, so **the refusal half was not exercised** |

### G19 — a zombie 12,000 blocks out: [500], not [580] (CREATIVE)

| prediction | READING |
|---|---|
| Teleport about 12,000 blocks from overworld spawn (e.g. `/tp @s 12000 ~ 0`, then onto the ground) and `/rpg spawn zombie`. The plate reads **`[500] Zombie 500/500 ❤`**, not 580 (M20). `mobinfo`'s "would roll" also reads 500 | **PASS as reported by Ben** (one multi-select, 2026-09-27). No `/rpg` command in the log, see the Status note. log: the far read was at `/tp ... 20000 100 20000`, d ≈ 28,285 (hypot from (0, 0) to the teleport target, run with awk; the true origin is the world spawn point, which the log does not print), not 12,000. Both are past the 10,000-block cap, so it is the same prediction |

### G6 — the Nether: [200] at spawn, [360] at 500 blocks, [500] at 2,500 (CREATIVE)

| prediction | READING |
|---|---|
| In the Nether, read `mobinfo`'s origin line first (the Nether's spawn point). Then `/rpg spawn zombified_piglin` **at the Nether spawn**, **500 blocks** from it, and **2,500 blocks** from it: **`[200]`**, then **`[360]`**, then **`[500]`**, capped (M20). The 500-block reading is what shows the 8x rate, since the 2,500 one is capped. A zombified piglin's vanilla max is 20, so the maxes read 200, 360 and 500 | **PASS as reported by Ben** (one multi-select, 2026-09-27). No `/rpg` command in the log, see the Status note. log: no Nether command logged (portal travel is not logged); no origin line read |

### G7 — the End: [300] at (0, 0) (CREATIVE)

| prediction | READING |
|---|---|
| In the End, `/rpg spawn enderman` within a few blocks of **(0, 0)**: **`[300]`**, with `mobinfo`'s origin reading **(0, 0)** (M23). If the End's spawn point is NOT (0, 0), a second enderman at that spawn point reads `300 + its distance from (0, 0) / 25`, as its "would roll" says -- not 300 | **PASS as reported by Ben** (one multi-select, 2026-09-27). No `/rpg` command in the log, see the Status note. log: no End command logged (portal travel is not logged); no origin line read |

### G13 — the GS survives a restart (437) (CREATIVE)

| prediction | READING |
|---|---|
| Near spawn, `/rpg spawn zombie 437` (437 is a value no position near spawn can roll). The plate reads **`[437]`**. Stay near it. I restart the server through the console file (`stop`, then boot WITHOUT `--refresh-content`). Rejoin and look at it: still **`[437]`**, with `mobinfo` stored GS 437 | **NOT READ.** Not named in the multi-select |

---

## THE REST

### G1b — a spider: the max is computed, not printed from the GS (CREATIVE)

| prediction | READING |
|---|---|
| A spider at spawn: **`[100] Spider 80/80 ❤`**. A spider 2,500 blocks out: **`[200] Spider 160/160 ❤`**. `mobinfo`'s vanilla `MAX_HEALTH` is 16.0 | **PASS as reported by Ben** (one multi-select, 2026-09-27). No `/rpg` command in the log, see the Status note. log: no `mobinfo`, so the 16.0 read was not taken |

### G2 — a zombie 2,500 blocks out: [200] (CREATIVE)

| prediction | READING |
|---|---|
| About 2,500 blocks from overworld spawn, `/rpg spawn zombie`: **`[200] Zombie 200/200 ❤`**, and "would roll" is 200 | **PASS as reported by Ben** (one multi-select, 2026-09-27). No `/rpg` command in the log, see the Status note. log: the one mid-range teleport is `/tp ... 2000 100 2000`, d ≈ 2,829 (the same basis). So the prediction's 2,500-block distance is not the distance the log shows. The log does not say where the mob that was read stood |

### G4 — the Knell at spawn: 360, not 1800 (CREATIVE)

| prediction | READING |
|---|---|
| `/rpg spawn knell` near spawn: **`[100] Knell 360/360 ❤`** (M4: no vanilla x5 on a custom mob) | **PASS as reported by Ben** (one multi-select, 2026-09-27). No `/rpg` command in the log, see the Status note. log: `/rpg spawn knell` was never issued |

### G11 — environmental damage on a mob is proportional (M13) (CREATIVE)

| prediction | READING |
|---|---|
| Drop an ordinary zombie (`/rpg spawn zombie`) and a **`/rpg spawn zombie 500`** from the same height, one high enough that it kills a vanilla zombie (about 25 blocks). **Both die** -- a GS 500 zombie with 500 HP takes a proportionally larger hit (M13's own sentence). No probe log exists in this slice, so the reading is "died / survived" | **NOT READ.** Not named in the multi-select |

### G12 — the GS survives a chunk unload (CREATIVE)

| prediction | READING |
|---|---|
| `/rpg mobtrace` ON. Near spawn, `/rpg spawn zombie 222`. Fly far enough away that its chunk unloads (several hundred blocks), then come back. The plate reads **`[222]`** again. The log shows a **`MOBREMOVE <uuid>`**, then a **`MOBSEED <uuid> ... gs=222 source=stored`** for the same uuid. Without that pair the row is hollow in time: a mob that never unloaded would print the same number | **NOT READ.** Not named in the multi-select. log: no `MOBSEED`/`MOBREMOVE` line |

### G14 — a mob through a Nether portal keeps its GS (SURVIVAL)

| prediction | READING |
|---|---|
| `/rpg spawn zombie 140`, and get it to walk or be pushed through a Nether portal. In the Nether it still reads **`[140]`**, while `mobinfo`'s "would roll" there reads at least 200. Its HP reads full on arrival; that is §6 F1, and expected | **NOT READ.** Not named in the multi-select |

### G15 — a stored GS of 0 is re-rolled (CREATIVE)

| prediction | READING |
|---|---|
| `/summon zombie ~ ~ ~ {BukkitValues:{"rpg:mob_gear_score":0}}` near spawn: it is re-rolled from position (reads ~[100]), and the log has a WARN naming the invalid `mob_gear_score` | **NOT READ.** Not named in the multi-select. log: no `mob_gear_score` WARN |

### G15b — a stored GS of 900 is re-rolled, not clamped (CREATIVE)

| prediction | READING |
|---|---|
| The same with `900`: re-rolled from position (~[100]), NOT [900] and NOT [500], with a WARN | **NOT READ.** Not named in the multi-select |

### G16 — conversions inherit the GS (CREATIVE, with a survival-style night for the drowned case)

| prediction | READING |
|---|---|
| `/rpg spawn slime 300` (a large one) and kill it: **the children read `[300]`**. And/or a GS 300 zombie drowned in water until it converts: the drowned reads **`[300]`** | **NOT READ.** Not named in the multi-select |

### G18 — the Knell in lava: proportional (M14) (CREATIVE)

| prediction | READING |
|---|---|
| A `/rpg spawn knell` and a `/rpg spawn knell 200` (720 HP) standing in lava for the same few seconds lose **about the same FRACTION** of their bar, read with `mobinfo` before and after. There is no probe log in this slice, so the reading is the two fractions | **NOT READ.** Not named in the multi-select |

### GX-MELEE — mob melee is unchanged in this slice (SURVIVAL)

| prediction | READING |
|---|---|
| A GS 100 zombie's hit on an unarmoured survival player takes about **vanilla's** damage off the custom bar: `mobinfo`'s vanilla `ATTACK_DAMAGE`, not x5. **Expected in this slice** (slice 2 scales it). A reading of "x5" here would be the defect | **NOT READ.** Not named in the multi-select |
