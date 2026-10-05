# GATE — WITHERED-SHORTBOW: an Uncommon ranged bow of the Wither element (content only)

**Status: NOT RUN.**

*Amended 2026-10-05, BEFORE ANY BOOT, at the seat's ruling 1: R0 is now the ONE R0 for the whole stack* (R0b reads one
entry from each of #168-#174; R0c predicts the cumulative `Loaded` line, derived from the last line read). Every gate
beneath records its R0 SUPERSEDED. No WB prediction moved. Rows stay 10.

Every prediction below was written **before** any row of this file was read, and **this file is COMMITTED before the
boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once its row has been read.
**NO BOOT until the seat says so**, after the seat has diffed every slice. **This is the stack top**:
`#168 → #169 → #170 → #171 → LEGACY-B → WITHER-STATUS → this`. It is booted ONCE, and every gate beneath it is read in
the same boot (the seat's S1 ruling: this gate carries the weapon-capped path of the status).

**Readings count only if Ben's LOGIN appears in THIS boot's log.** A row whose prediction is a LOG LINE is PASS when
that line appears after the LOGIN with every predicted field as written.

```
ROWS     10   R0a R0b R0c
              WB1 WB2 WB3 WB4b WB5 WB6 WB7
         ──
         10   = 3 R0 + 7 WB     git grep -c '^### R0\|^### WB' <ref> -- GATE-withered-shortbow.md
```

**Plan:** `PLAN-wither.md` on `docs/batch-surveys` (`fe73f5ff`), section 9 and RULINGS: Ben's words, his "everything else
is fine" (9.1's PROVISIONAL column), his Q-W10 overrule (no tooltip line, so WB1b is deleted), and the seat's
2026-10-01 WB4b. The DOTTICK line and the MOBSEED rule are `GATE-wither-status.md`'s (*THE DOTTICK LINE*); they are not
restated.

## GAME MODE

**SURVIVAL on every row.** The magazine is a cost (WB7), and creative removes costs.

## THE ORDER IN THE ONE BOOT

**This file's R0 first, once, for the whole stack.** Then every gate beneath, bottom-up, through
`GATE-wither-status.md` (with `GATE-nexus-polish.md` R0d read in its place), then **these** WB rows.

## Set-up

- `GATE-legacy-a.md`'s set-up, unchanged: **`/gamerule spawn_mobs false`**, **`/time set 18000`**,
  **`/rpg playerxp set @s 1 levels`** (record `Lifetime XP` first if an earlier restore ran), **no accessories, no
  armour**, **`/rpg mobtrace`** ON.
- **`/rpg give withered_shortbow`**; with it in hand, **`/rpg gearscore set 100`** (score 100 scales nothing).
- **Zombies:** `GATE-wither-status.md`'s two recipes (GS-100 by `/summon … NoAI`, GS-20 by `/rpg spawn zombie 20` then
  NoAI), plus **GS-200: `/rpg spawn zombie 200`** then the same `/data merge … {NoAI:1b,PersistenceRequired:1b}`
  (expected **`MOBSEED <uuid> zombie gs=200 source=… max=200`**). Place each at the row's offset (`/tp` it, or summon
  there). **A fresh zombie per row. A `crit=true` PLAYERHIT is not comparable: re-fire on a fresh zombie.**
- **Aim:** `/tp @s ~ ~ ~ facing entity <the zombie> eyes`, then one right-click.

---

## R0 — THE ONE R0 FOR THE WHOLE STACK. READ IT FIRST, BEFORE ANY ROW OF ANY GATE. IF ANY R0 FAILS, STOP

**The seat's ruling 1, 2026-10-05.** This R0 is the stack's only R0: every gate beneath (#168-#173) carries a dated note
recording its own R0 rows **SUPERSEDED** at this boot, never PASS or FAIL. The one exception is `GATE-nexus-polish.md`
**R0d** (the ten screen commands), which is a #170 merge bar and is read as written. R0b reads one shipped entry from
**each** of the seven slices; R0c predicts the **cumulative** `Loaded` line. Intermediate facts of the gates beneath
(a lower R0b's ABSENT, a lower R0c's counts) are not read.

### R0a — the build line names the stack top's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, WITHERED-SHORTBOW's head (#174) as `gh pr view 174 --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown`, and not any head beneath. **This `<sha>` is the one every lower gate's SUPERSEDED note names** | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries all seven slices, and the bow as authored

| prediction | instrument | READING |
|---|---|---|
| Entries PRESENT, one per slice: #168 core `combat/TracedHit.class`; #169 core `progression/LevelBonus.class`; #170 paper `menu/NexusScreens.class`; #171 `content/weapons/short_bow.yml`; #172 paper `adapter/FireballDrive.class`; #173 paper `adapter/DotStatus.class` and `content/statuses/withering.yml`; #174 `content/weapons/withered_shortbow.yml`. ABSENT: the control. **In the bow's text, as YAML keys (anchored):** PRESENT `element: wither` (top level), `rarity: uncommon`, `cooldown_ticks: 16`, `- type: knockback`; ABSENT `- type: status` | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/combat/TracedHit.class',
               'io/github/butterflysmp/rpg/core/progression/LevelBonus.class',
               'io/github/butterflysmp/rpg/paper/menu/NexusScreens.class',
               'content/weapons/short_bow.yml',
               'io/github/butterflysmp/rpg/paper/adapter/FireballDrive.class',
               'io/github/butterflysmp/rpg/paper/adapter/DotStatus.class',
               'content/statuses/withering.yml',
               'content/weapons/withered_shortbow.yml',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$r = New-Object IO.StreamReader($zip.GetEntry('content/weapons/withered_shortbow.yml').Open()); $bow = $r.ReadToEnd(); $r.Close()
foreach ($k in '^element: wither\s*$', '^rarity: uncommon\s*$', '^\s+cooldown_ticks: 16\s*$', '^\s+- type: knockback\b', '^\s+- type: status\b') {
  if ($bow -match ('(?m)' + $k)) { "PRESENT $k" } else { "ABSENT  $k" }
}
$zip.Dispose()
```

### R0c — the cumulative Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, **29 visuals**, **5 statuses**, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, **16 weapons**, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. **Derived from the last line actually READ**, `GATE-melee-cell.md` R0c on `9ee59a2e` (13 weapons, 27 visuals, 4 statuses), plus the content files added since, from `git diff --name-status 9ee59a2e <top> -- paper/src/main/resources/content` (2026-10-05): **weapons +3** (`short_bow` #171, `blaze_kings_staff` #172, `withered_shortbow` #174), **visuals +2** (`blaze_fireball_cast`, `blaze_fireball_impact`, #172), **statuses +1** (`withering`, #173); the only other content edits are `elements/fire.yml` and `elements/wither.yml` (modified, not added); every other count unchanged. No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair, **none naming `withered_shortbow`, `blaze_kings_staff`, `blaze_fireball` or `withering`**, and **no `element 'wither' applies status` warning** (carried from `GATE-wither-status.md` R0c) | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception','applies status'` | |
---

## THE BOW

### WB1 — the tooltip names Wither only as the element, and the bow does not stack (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Hover (score 100). In order: **`Wither`** (dark gray), **`Quiver: --/8`**, blank, **`Ranged Damage: 16`**, **`Attack Speed: 1.3`**, blank, **`Uncommon Ranged Weapon`** — the golden rendering. **No "Inflicts Wither" line** (Ben overruled Q-W10) and no flavor (none is authored). A second `/rpg give withered_shortbow` lands in its own slot | |

### WB2 — PLAYERHIT: 16 wither on a right-click (SURVIVAL) — witness: PLAYERHIT line

| prediction | READING |
|---|---|
| GS-100 zombie at offset `~8 ~0`, aimed, one right-click: an **arrow** flies, and **`PLAYERHIT caster=<Ben's uuid> source=withered_shortbow/right_click target=<the zombie's uuid> … element=wither sent=16.000 crit=false triggerScore=100`**. Exactly one PLAYERHIT for the shot. **Record `caster=`; WB3 reads it** | |

### WB3 — the accrued Wither on the real path, and the CREDIT (SURVIVAL) — witness: DOTTICK

| prediction | READING |
|---|---|
| After WB2's hit, on the same zombie (expected `max=100`): **five** lines **`DOTTICK <the zombie's uuid> zombie status=withering applier=<WB2's caster=> sent=5.000 cap=8.000 tick=<n>`**, 40 apart, the first about 40 ticks after the hit. `cap=8.000` is half the 16 hit (Q-W2), and `sent` is `min(0.05 × 100, 8.0)`, the percent arm. **`applier=` equal to WB2's `caster=` is the credit row (WS4, moved here from the status gate, where `/rpg apply` passes no applier).** The vanilla wither swirl shows; no fire. This is the accrual path's first live instance | |

### WB4b — the WEAPON's cap binds, by mob scaling (SURVIVAL) — witness: DOTTICK (WITNESSED)

| prediction | READING |
|---|---|
| **The seat, 2026-10-01.** A **GS-200 zombie** (expected `max=200`; 5% = 10 > 8), one arrow: five lines with **`sent=8.000 cap=8.000`**: the weapon's cap binds, **not 2.0**. **The control:** WB3's GS-100 zombie read **`sent=5.000 cap=8.000`**, the percent arm. Each `sent` is `min(0.05 × max, 8.0)` from that mob's own `MOBSEED max=` line | |

### WB5 — WS2 on the real path: no explosion, by the Wither or by the arrow (SURVIVAL) — witness: Ben + the log

| prediction | READING |
|---|---|
| **(a) Killed by the Wither.** A fresh, never-scorched **GS-20** zombie (expected `max=20`) with a second zombie two blocks away. One arrow: `sent=16.000`, nameplate **4/20**. Then four lines `DOTTICK … status=withering applier=<Ben> sent=1.000 cap=8.000`, and **it dies on the fourth. NO explosion, no `ignite_blast`, the neighbour's nameplate does not move.** **(b) Killed by the arrow.** A fresh GS-20 zombie, `/rpg mobdamage 10` (**10/20**), with a neighbour. One arrow: `PLAYERHIT … element=wither sent=16.000` and **it dies to the arrow. NO explosion** (Ignite's second clause asks `accruesScorch`, false for wither: `ElementAccrualTest`). (a) is the Q-W8-narrowed WS2; (b) is K2 | |

### WB6 — it pushes (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| As `GATE-legacy-a.md` SB3: a zombie **without** `NoAI` (`/summon zombie ~8 ~ ~0 {PersistenceRequired:1b,IsBaby:0b,Tags:["t"]}`), shot as it walks in: **it is pushed back along the arrow's line** (`strength: 0.1`). Record the push as seen; the distance is not predicted. **Sole witness of the knockback being applied** (`WitheredShortbowContentTest` pins that it is AUTHORED) | |

### WB7 — eight arrows, then the reload (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Hold right-click with a fresh bow: the quiver readout counts **8 → 0**, one shot per **16 ticks** at most (0.8 s; on the 4-tick grid). At 0 it reloads for **60 ticks** (3 s), then reads **8** again | |

---

## LAST STEP — PUT BEN'S XP BACK

`/rpg playerxp set @s <the number recorded> xp`.

---

## MUTATIONS (run 2026-10-01 against `e3886d4a`, before this file was committed)

The slice is one content file and the tests it moves, so the mutations are against the shipped file and read the
guards that hold it. Each restored by `cp` and proved byte-identical with `cmp`.

| mutation | edit | reddened |
|---|---|---|
| MUT-NOKNOCKBACK | the knockback entry spliced out (a deletion, both lines; delta 2) | `itsWitherIsTheElementsAndItAuthorsKnockback` only |
| MUT-KINETIC | the on_hit's `element: wither` → `kinetic` | `itsWitherIsTheElementsAndItAuthorsKnockback` only |
| MUT-COOLDOWN13 | `cooldown_ticks: 16` → `13` (off the grid) | `itCarriesTheProvisionalNumbers`, `GoldenLoreTest` |
| MUT-NOQUIVER | `quiver_size: 8` commented out | the file no longer loads: all three pin rows, the ranger invariant, and `GoldenLoreTest`. *Why the loader skips it was not read* |

**Sole witnesses:** WB6 for the knockback landing, WB7 for the magazine and the reload in play, WB3 and WB4b for the
accrual path (`ElementAccrualTest` covers the decision, not the live application), WB5 for no explosion on a live death.
