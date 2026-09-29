# GATE — Level slice: the active cap of 50, Ben's level bonuses, and the head's level screen

**Status: NOT RUN.**

Every prediction below was written **before** any boot of this branch, and **this file is COMMITTED before the
boot**, so a prediction can be checked against origin for having existed first. Readings go **beside** a
prediction, never over it, and a prediction is not edited once its row has been read. **NO BOOT until the seat has
diffed this file** (the seat's instruction, 2026-09-29).

**Readings count only if Ben's LOGIN appears in THIS boot's log.** The seat's 2026-09-28 ruling applies: a row whose
prediction is a LOG LINE is PASS when that line appears verbatim after the LOGIN, named by Ben or not. "Not itemised"
is only for rows that rest on Ben's own observation.

```
ROWS     14   R0a R0b R0c
              CP1 CP2 CP3
              BN1 BN2
              WD1 WD2 WD3
              LS1 LS2 LS3
         ──
         14   = 3 R0 + 3 CP + 2 BN + 3 WD + 3 LS     git grep -c '^### R0\|^### CP\|^### BN\|^### WD\|^### LS' <ref> -- GATE-level.md
```

**Plan:** `PLAN-level-bonuses.md` on `docs/batch-surveys` (`6a00d8a3`), section 0: Ben's words verbatim (0.1, 0.2,
0.5) and the seat's rulings L1-L6. **This branch stacks on `feat/melee-m1`** (seat ruling L5), so it is booted as
the STACK TOP, and #168's `GATE-melee-cell.md` is read in the same boot (see *THE ORDER*, below).

## GAME MODE

**SURVIVAL on every row.** No row here depends on a cost creative removes, but survival is what ships (the
creative-divergence register, `verification.md`), and #168's rows in the same boot are survival too.

## THE ORDER IN THE ONE BOOT, AND WHY THIS GATE GOES LAST

1. **R0**, once, for the stack top. The rows below are this gate's; #168's R0 rows are the same boot's.
2. **#168's MC rows, all of them, BEFORE any row here.** Every row here moves the player's level, and #168's rows
   were written without one. **No MC row is restated**, and that is measured, not assumed: MC6 and MC8 predict
   against their own baseline `S` (`S+3.000`, "below S") and every other `sent=` in that file is a STONE cast, which
   this slice keeps level-blind by construction (WD3 below). **The precondition is that Ben's level does not change
   DURING an MC row** — which it cannot, if this gate's rows come after. MC8's stats head gains one line at the
   BOTTOM (LS1); every line MC8 reads sits above it and does not move.
3. **This gate's rows, in file order.** They set the level with `/rpg playerxp`, which WRITES THE PROFILE TO DISK.
   The set-up records Ben's real lifetime XP first, and the last step restores it.

## Set-up

**Every command below is typed IN GAME by Ben, as an op.** None needs the console, and none names a UUID.

- **Boot:** `./scripts/dev-server.sh`. This slice adds **no content**, so `--refresh-content` is not needed for it
  (#168's gate may still ask for it; that flag is harmless here).
- **RECORD BEN'S REAL XP FIRST.** Click the Nexus star and read the stats head's `Lifetime XP` line. Write the number
  down. **The last step of this gate is `/rpg playerxp set @s <that number> xp`**, which puts it back.
- **`/rpg mobtrace`** (if #168's rows have not already turned it on; the reply says ON or OFF, so toggle until ON).
- **`/time set 18000`** and **`/gamerule doMobSpawning false`**, as #168's gate sets them.
- **Nothing in the ACCESSORY slots, no ARMOUR and no FRAGMENT slotted** for BN1 and BN2, so the sheet's baseline is the bare player. If
  anything is worn, the rows still read (they predict DELTAS against a level-1 baseline read in the same row), but a
  crit baseline that is not a whole percent can round a delta by one point; BN1 says where.
- **The build cell is Fire Melee**, as #168's MC1 leaves it, so the Ability Stone's Left input is the thrust (WD3).
- **`/rpg give emberblade`**, for BN2, WD1 and WD2.
- **Wait 1 s after every `/rpg playerxp`** before reading anything: the level reaches the stats through the reconcile
  loop, every 5 ticks.
- **The test zombie is #168's** — its section *THE TEST ZOMBIE*, the same four commands, offset `~2 ~ ~0` for every WD
  row, and a fresh one whenever the nameplate falls below half. `sent=` is pre-Defense, so a fresh adult is
  equivalent.
- **A line with `crit=true` is not comparable with a `crit=false` one** (#168's rule). At level 50 the crit chance is
  20% rather than 15%, so expect more re-swings.

---

## R0 — THE DEPLOYED BUILD CARRIES THIS BRANCH. IF ANY R0 FAILS, STOP

### R0a — the build line names this branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `gh pr view <n> --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown`, and **not #168's head** — the stack top is booted | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries the level slice, and #168 beneath it

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `progression/LevelBonus`, paper `menu/LevelMenu`, and #168's core `combat/TracedHit` (the slice below). ABSENT: the control. The first two are new on this branch, so #168's jar reads ABSENT for them | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/progression/LevelBonus.class',
               'io/github/butterflysmp/rpg/paper/menu/LevelMenu.class',
               'io/github/butterflysmp/rpg/core/combat/TracedHit.class',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — the boot log: content loads, nothing refused, and this slice moved no count

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes` — **identical to #168's R0c reading on `9ee59a2e`**, because this slice adds no content file (the slice below it is the baseline, seat ruling L5). **No** `Refusing`, `Skipping`, `SEVERE` or exception. The known WARNs are `volley_stone`'s 27-tick cooldown and its summary line | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | |

---

## THE CAP — A VIEW CLAMP AT 50 (seat ruling L2)

**The reply's format is `RpgCommand.playerXp`'s**: `Set <n> level(s) for <name> (now level <L>[ [curve <C>]], <into>/<rung> into it | MAX into it, <lifetime> lifetime).` The totals are the curve's, computed from `PlayerLevel.XP_TO_NEXT`: level 49 = 653,640, level 50 = 712,580, level 60 = 1,681,040; rung 49 → 50 = 58,940.

### CP1 — one level short of the cap (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `/rpg playerxp set @s 49 levels` replies **`Set 49 level(s) for <name> (now level 49, 0/58940 into it, 653640 lifetime).`** The star's stats head then reads, as its first three lines: **`Level        49`**, **`Lifetime XP  653,640`**, **`To Next      58,940`** | |

### CP2 — exactly at the cap (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `/rpg playerxp set @s 50 levels` replies **`Set 50 level(s) for <name> (now level 50, MAX into it, 712580 lifetime).`** The stats head reads **`Level        50 (MAX)`**, **`Lifetime XP  712,580`**, and **no `To Next` line** | |

### CP3 — past the cap: the curve moves, the view does not (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `/rpg playerxp set @s 60 levels` replies **`Set 60 level(s) for <name> (now level 50 [curve 60], MAX into it, 1681040 lifetime).`** The stats head reads **`Level        50 (MAX)`**, **`Lifetime XP  1,681,040`**, and no `To Next` line. `/rpg stats` reads exactly what it read at the end of CP2 (the bonus is the cap's) | |

**THE SAVE TRIGGER PAST THE CAP IS NOT A ROW HERE, AND THAT IS STATED RATHER THAN SKIPPED.** `ProfileService.addLifetimeXp` is unchanged by this slice; its write-to-disk trigger reads `PlayerLevel.levelFor`, which this slice did NOT clamp, so XP past 50 still writes at every curve rung. The claim is carried by `PlayerLevelTest.theCurveLevelStillMovesPastTheActiveCap`, and a boot row would need a crash between orb pickups to see it.

---

## THE BONUSES — READ OFF `/rpg stats`, AS DELTAS AGAINST LEVEL 1

### BN1 — health, regen, defense and crit at seven levels (SURVIVAL) — witness: Ben

**Staging:** empty main hand. `/rpg playerxp set @s 1 levels`, wait 1 s, `/rpg stats`: record **HP₁** (`Max Health`), **R₁** (`Health Regen`, `n.nn/5s`), **D₁** (`Defense`) and **C₁** (`Crit Chance`, a whole percent). Then for each level L below: `/rpg playerxp set @s <L> levels`, wait 1 s, `/rpg stats`.

| L | Max Health | Health Regen | Defense | Crit Chance | READING |
|---|---|---|---|---|---|
| 2 | HP₁ + 5 | R₁ | D₁ | C₁ | |
| 5 | HP₁ + 20 | R₁ | D₁ + 1 | C₁ | |
| 10 | HP₁ + 45 | R₁ + 1.00 | D₁ + 2 | C₁ + 1% | |
| 49 | HP₁ + 240 | R₁ + 8.00 | D₁ + 9 | C₁ + 4% | |
| 50 | HP₁ + 250 | R₁ + 9.00 | D₁ + 10 | C₁ + 5% | |
| 60 | as at 50 | as at 50 | as at 50 | as at 50 | |

- **With nothing worn and no fragment slotted in the Build screen, the level-1 baseline is `100`, `1.00/5s`, `0` and `15%`**, and level 50 reads **`350`**, **`10.00/5s`**, **`10`** and **`20%`** — Ben's *"+10 health per 5 seconds"* and *"+5% at level 50"* as the sheet prints them.
- **Crit rounds to a whole percent** (`StatsSheetLines.critChance`). A delta of one point can read one off only when C₁'s underlying chance is not a whole percent, which a worn accessory can cause. Recorded, not predicted.
- **The current HP does not rise with the maximum** (`DESIGN-stat-engine.md`, *Max-HP change semantics*: headroom, not health). Secondary; not predicted beyond that.

### BN2 — the Damage line adds the level only with a weapon in hand (SURVIVAL) — witness: Ben

**The sole witness for the stats sheet's half of seat ruling L1: mutation `MUTLVL-SHEET` (the level term removed from `StatsSheetProjection`) reddens NO unit test**, measured on `bbdab589`.

| prediction | READING |
|---|---|
| At level 1, `/rpg stats` with **emberblade in the main hand**: `Damage` reads **W₁**; with an **empty hand**: **E₁**. At level 50: emberblade **W₁ + 49.00**; empty hand **E₁**, unchanged | |

---

## WEAPON HITS ONLY — PLAYERHIT, AT LEVEL 1 AGAINST LEVEL 50 (seat ruling L1)

**Every row is a same-item comparison**, so the emberblade's rolled gear score cancels: the level-50 reading is the level-1 reading plus 49. `sent=` is `hitBase x charge x crit`, and the level term is inside `hitBase` beside class damage (`HitDamage.hitBase`), so a FULL charge and `crit=false` make the difference exactly 49.000.

### WD1 — a basic melee swing (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| **Staging:** the four zombie commands, offset `~2 ~ ~0`, aim level. Hold emberblade, **2 s between swings** (full charge). `set @s 1 levels`, wait 1 s, swing: `source=emberblade/left_click … sent=S₁ crit=false triggerScore=T`. `set @s 50 levels`, wait 1 s, swing: **`source=emberblade/left_click … sent=S₁+49.000 crit=false triggerScore=T`**, the same T | |

### WD2 — a weapon trigger with LITERAL damage: the case the refused attack-stat route missed (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| **Staging:** as WD1, `/rpg mana refill` before each cast. Right-click the emberblade at the zombie (its fireball, a `burst` of literal `damage 12`): at level 1, `source=emberblade/right_click … sent=F₁ crit=false triggerScore=T`; at level 50, **`… sent=F₁+49.000 crit=false triggerScore=T`**. The literal is score-scaled first and the level added after, so the difference is exactly 49 whatever T is | |

### WD3 — a STONE cast carries none of it (SURVIVAL) — witness: PLAYERHIT lines

**The in-game half of L1's named unit row** (`LevelDamageWeaponOnlyTest.aStoneCastCarriesWeaponTriggerFalseAndOnlyATriggerCarriesItTrue`).

| prediction | READING |
|---|---|
| **Staging:** as WD1; the Ability Stone in hand, Fire Melee cell. At level 50 (after WD2), `/rpg mana refill`, **Left**: **`source=active_placeholder_melee_thrust … sent=7.000 crit=false triggerScore=100`** — identical to #168's MC3 reading at whatever level MC3 was read, and to the level-1 figure. Ben: *"Only weapons, we'll tackle the ability damage pipeline later"* | |

**`DashAim`'s carry of the flag has NO row, because no shipped weapon can reach it.** `DashAim.resolve` copies a `Success` for a `CastSpec.Dash`, and **no file in `content/weapons/` authors `type: dash`** — only `ember_step`, `recall` and `recall_updraft`, all stone abilities, which carry the flag false either way. Mutation `MUTLVL-DASHAIM` (the carry replaced with `false`) reddens no unit test. The carry is kept because a copy that drops a record component is the defect the compiler caught when the component was added; it becomes testable the day a weapon authors a dash.

---

## THE LEVEL SCREEN — THE HEAD IS A BUTTON NOW

**Wording, colours and materials are defaults; Ben rules them** (the PR lists them). These rows read that the screen works, not that its words are right.

### LS1 — at level 1: the hint, the screen, page 1 (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `set @s 1 levels`, open the star. **The stats head's LAST lore line reads `Click for your level, its bonuses and unlocks.`**, below a blank line; every line above it is as before. Click the head: a screen titled **`Player Level`** opens. Slot 4 (top middle) is an **experience bottle** named **`Level 1`** with lore **`Lifetime XP: 0`**, **`To next level: 1,000 XP`**, blank, **`Your level grants nothing yet.`** The level cells show **levels 1-28** (page 1); **Previous page** is absent (a filler pane at bottom-left), **Next page** is present (paper, bottom-right). The **Level 1** cell is an experience bottle reading **`Where everyone starts.`**, blank, **`Reached`**. The **Level 3** cell is a **gray dye** named **`Level 3`** reading **`+5 Max Health`**, **`+1 Weapon Damage`**, **`Unlocks the Crafting station`**, blank, **`2,090 XP to go`** | |

### LS2 — at level 50: the totals, page 2, the last unlock (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `set @s 50 levels`, open the star, click the head. The summary is named **`Level 50 (MAX)`** with **`Lifetime XP: 712,580`**, **`You are at the level cap.`**, blank, **`Your level grants:`**, then **`+250 Max Health`**, **`+49 Weapon Damage`**, **`+9 Health Regen per 5s`**, **`+10 Defense`**, **`+5% Crit Chance`**, then **`Weapon Damage applies to weapon hits only.`** The screen opens on **page 2 (levels 29-50)**; the six cells after level 50 are filler. The **Level 50** cell is an experience bottle reading **`+10 Max Health`**, **`+1 Weapon Damage`**, **`+1 Health Regen per 5s`**, **`+1 Defense`**, **`+1% Crit Chance`**, **`Unlocks Vault page 7`**, blank, **`Reached`**. The **Level 30** cell is a **lime dye** reading **`+5 Max Health`**, **`+1 Weapon Damage`**, **`+1 Health Regen per 5s`**, **`+1 Defense`**, **`+1% Crit Chance`**, **`Unlocks Vault page 3`**, blank, **`Reached`** | |

### LS3 — paging, Back and Close (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| On LS2's screen: **Previous page** flips to levels 1-28 in place (no reopen: the cursor does not jump), and **Next page** flips back. **Back** (arrow, slot 48) returns to the **`Nexus Menu`**. Reopen the level screen: **Close** (barrier, slot 49) closes it. No item can be taken from or put into any slot | |

---

## LAST STEP — PUT BEN'S XP BACK

`/rpg playerxp set @s <the number recorded in the set-up> xp`, then open the star: the stats head's `Lifetime XP` line reads that number again.

---

## MUTATIONS, ON `bbdab589`, RECORDED BEFORE THE BOOT

Run with a pristine copy per file, a marker grep and a line delta against that copy, the module's WHOLE suite, and a restore by `cp` checked with `cmp` (every one: `restored: byte-identical`). Core suite 1539 tests; paper `-am` 1539 / 92 / 1080.

| mutation | edit | marker / delta | RED |
|---|---|---|---|
| `MUTLVL-RESOLVE-TRUE` | `cast`'s `resolve(…, false)` → `true` | 1 / 1 | 4: the flag row, the literal row, the WeaponDamage row, the volley row |
| `MUTLVL-DAMAGE-ARM` | the `Damage` arm drops `weaponLevelDamage()` | 1 / 1 | 3: the literal row, the volley row, the enchant row |
| `MUTLVL-WEAPON-ARM` | the `WeaponDamage` arm drops it | 1 / 1 | 2: the WeaponDamage row, the melee row |
| `MUTLVL-MELEE` | `landBasicMelee` drops `withWeaponHit()` | original gone 0 / 1 | 1: the melee row |
| `MUTLVL-VOLLEY` | `volley` drops it | 1 / 1 | 1: the volley row |
| `MUTLVL-COMMIT` | `commit` drops it | 1 / 1 | 4: the literal, WeaponDamage, enchant **and volley** rows — **the volley row was predicted to survive and did not**: the volley's flag is `source.weaponHit()` of the Caster `commit` built, so it inherits the loss. The code is right; the prediction was one row short |
| `MUTLVL-HP50` | the 49 → 50 level-up pays 5, not 10 | 1 / 1 | 5: three `LevelBonusTest` rows, two `LevelBonusLinesTest` rows |
| `MUTLVL-CLAMP` | `effectiveLevel` returns the curve level | 1 / 1 | 3 failures + 1 error: `PlayerLevelTest` ×2, `PlayerLevelLinesTest` ×1, and `LevelBonusTest.lifetimeXpPastTheCapGivesTheCapsBonus` ERRORS (level 60 refused, as designed) |
| `MUTLVL-WIRING` | defense's `fromLevel.sources(…)` → an empty map | 1 / 1 | 1: `LevelWiringSignatureTest` |
| `MUTLVL-SHEET` | the sheet's level term → `0.0` | 1 / 1 | **NONE** — BN2 is its only witness |
| `MUTLVL-DASHAIM` | the carry → `false` | 1 / 1 | **NONE** — unreachable from shipped content (WD3's note) |
