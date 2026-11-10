# GATE — DEFT HANDS: the Ranger reload enchant, the one-tick floor, the RELOAD line (and the stack's one R0)

**Status at `61e722f2` (the stack-top boot, 2026-11-10): R0a-d PASS (the stack's one R0); DH5 PASS (unit); DH1, DH3, DH6 SKIPPED (a staging defect: no hotbar slot for the Boltor); DH2, DH4, DH7, DH8 NOT READ (Ben paused; carried). NOT a merge candidate.**

**The boot, 2026-11-10, on `61e722f2` (#180's head, the stack top), Paper 26.1.2 build 74, PacketEvents 2.13.0.** CC booted it with `./scripts/dev-server.sh --refresh-content` on the seat's word; Ben was the only player (LOGIN L81, 09:32:48). Line numbers are this boot's `run/logs/latest.log`.
- ST7 first (L82-L88), then four selftest runs.
- **The level run** (L88-L302): ST7's start half.
- **Run 1**, `all` (L304-L1551): ABORTED. Ben drowned in the Sunder rows (L1522, 09:38:54) and then disconnected (L1549). It certifies nothing.
- **Run 2**, `all` (L1557-L2903): STOPPED by Ben (`/rpg selftest stop`, L2900) after he drowned (L2831, 09:43:59, in SU11's opening wait). The Sunder rows stage relative to the player, so each drop lowered the next row's floor (y 63, 59, 56, about 51) into water. Its SU10 is VOID; it certifies nothing else.
- **Run 3**, `all` (L2917-L4311): **the run of record** (the seat, 2026-11-10). Ben started it himself, and it finished.
- **Ben's hand rows**, after L4311 with the mob trace on (L4312): SB1 and WB1 only (L4539-L4819). He paused before DH2, DH4 and SU3 (11:02:40). **No clean stop is in the log:** Ben left without `/stop`, and the JVM was left running, not killed (the seat's word).

**Defects found in the reading (seat-accepted, 2026-11-10; fixes owed before the next boot):** (1) `PaperCombatWorld.castRay` accepts a dying `LivingEntity`, so a shot ends on the previous row's corpse (GATE-legacy-b.md LB2 and LB5 (a), GATE-withered-shortbow.md WB2). (2) The selftest's staging leaks between rows: dying targets take the look-ray (GATE-wither-status.md WS2b, WS2c, WS3); `n`-tagged mobs are never removed (GATE-sunder.md SU2); placed blocks persist until the run ends (run 2's flood); the inventory fills across gates (DH1 below).

**Status: NOT RUN.** Written 2026-11-06, before any boot of this branch. **NO BOOT until the seat says so.** This is the
new STACK TOP (`#168 → … → #175 → #178 Sunder → this`), so the stack's one R0 moves here from `GATE-sunder.md`, which
records its R0 SUPERSEDED by this file.

Every prediction below was written **before** any row of this file was read, and **this file is COMMITTED before the
boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once its row has been read.

**Plan:** `docs/plans/PLAN-deft-hands.md`. **Ben, 2026-11-06:** *"I'd like to pivot to adding a new Enchantment, Deft
hands, increases reload speed by 10/20/30% Ranger weapon exclusive"*, and on the plan's question 4: *"a"*, meaning
reload **TIME** × 0.9 / 0.8 / 0.7. **The seat's rulings, 2026-11-06:**
- the percent is converted once, against the held weapon's AUTHORED `reload_ticks`, in integer arithmetic;
- the scanner gates on `ReloadTime.declares`;
- the gate is `class: ranger` plus `DeftHandsContentInvariantTest` (`hunters_bow` can roll it uselessly: a finding);
- **the resolved reload never goes below 1 tick**;
- the RELOAD trace line exists;
- the display name is "Deft Hands" and the icon is feather.

## GAME MODE

**SURVIVAL on every row.** `/rpg selftest` refuses any other mode (GATE-selftest.md ST7). **A reload consumes arrows
in SURVIVAL** (`QuiverAmmo`), so Ben's hand rows carry arrows.

```
ROWS     12   R0a R0b R0c R0d
              DH1 DH2 DH3 DH4 DH5 DH6 DH7 DH8
         ──
         12   = 4 R0 + 8 DH     git grep -c '^### R0\|^### DH' <ref> -- docs/gates/GATE-deft-hands.md
```

## THE WITNESSES, AND THE HONESTY RULE

- **`RELOAD player=<uuid> weapon=<id> authored=<n> resolved=<n> start=<tick> completesAt=<tick>`**: new in this slice,
  under `/rpg mobtrace`. It is printed by `Quivers.beginReload` from the numbers it just stamped, and formatted by
  `ReloadTime.line`, which `ReloadTimeTest` pins.
- **`/rpg reloadtime <bonus>`'s reply**, `<authored> -> <resolved> ticks (<s> -> <s>)`: the held weapon's reload as
  `ReloadTime.resolve` prices it from the live stat plus `<bonus>`. **It also mints a clock item into the inventory**
  (the dev instrument). The clock counts only when it is EQUIPPED, so the readings below never equip it. During a
  selftest run the reply is a `REPLY` line. **Its full text is `Gave reload_time_boost (<±n> ticks, <SLOWER|FASTER|no change>: <outcome>). Takes effect on your NEXT reload, not on one already running.`; every row below predicts the `<outcome>` as a CONTAINED substring.** It is formatted with the JVM's default locale (`String.format`, no `Locale`), so the seconds read `3.00s` on an English-locale JVM; a comma separator is that, not a FAIL.
- **A reload begins from the left-click PACKET** (`WeaponSwingListener`, a PacketEvents listener), so the instrument
  cannot drive one: its synthetic `PlayerArmSwingEvent` never reaches that path. **DH2 and DH4 are Ben's input, with
  the server's `RELOAD` line as the reading.** No verb was added to bypass the packet path, because a reading through
  a bypass would not be a reading of the shipped path.

| row | witness | the instrument | Ben's half / why not AUTO |
|---|---|---|---|
| DH1 | AUTO-PARTIAL | enchants a held Boltor III through the dev commands, dumps its lore, reads `/rpg reloadtime 0` | the stats sheet's `Reload` line, a glance |
| DH2 | AUTO-PARTIAL | — | **Ben** empties and reloads; the reading is the `RELOAD` line (a packet starts a reload) |
| DH3 | AUTO | the tiers on 60, 40 and 34 through `/rpg reloadtime 0` | — |
| DH4 | BEN | — | held right-click through a reload; the instrument cannot hold a key |
| DH5 | UNIT | `EnchantRollTest.deftHandsEntersTheRangerPoolAndNoOther` | candidates are rolled at random per item, so one weapon's table is not a reading of the pool |
| DH6 | AUTO | Deft III on `quiver_stone` plus `/rpg reloadtime -30`: the floor | — |
| DH7 | BEN | — | the reload cue is a sound, the client's |
| DH8 | BEN | — | the grindstone refund through the real screen |

---

## R0 — THE ONE R0 FOR THE WHOLE STACK (moved here from GATE-sunder.md). READ IT FIRST. IF ANY R0 FAILS, STOP

Every gate beneath carries its R0 SUPERSEDED, never PASS or FAIL. `GATE-sunder.md`'s note names this file.

### R0a — the build line names this PR's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `gh pr view <n> --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown`, and not Sunder's (#178) or any head beneath | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **61e722f2, 2026-11-10: PASS.** L43 `[Rpg] Build: 61e722f2`, #180's head (`gh pr view 180 --json headRefOid` printed `61e722f2d5e39fd04589dbc81ec3b2f99556afb5`); not `-dirty`, not `unknown`. |

### R0b — the jar carries every slice, Sunder, and Deft Hands

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `GATE-sunder.md` R0b's thirteen slice entries (unchanged), and **this slice's** `content/enchants/deft_hands.yml` and paper `health/DeftHandsModifierItems.class`. ABSENT: `content/abilities/active_placeholder_melee_thrust.yml` (Sunder deleted it) and the control. In `deft_hands.yml`, as anchored YAML: PRESENT `effect: reload_time`, `class: ranger`, `icon: feather`; ABSENT `class: universal` | the scan below | **61e722f2, 2026-11-10: PASS.** The scan above, run against `run\plugins\rpg-0.1.0-SNAPSHOT.jar`: all 15 slice entries PRESENT; `content/abilities/active_placeholder_melee_thrust.yml` and the control ABSENT. In `deft_hands.yml`: PRESENT `^effect: reload_time`, `^class: ranger`, `^icon: feather`; ABSENT `^class: universal`. |

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
               'io/github/butterflysmp/rpg/paper/selftest/SelfTestService.class',
               'selftest/index.yml',
               'io/github/butterflysmp/rpg/core/build/Touchdown.class',
               'content/abilities/sunder.yml',
               'content/builds/melee.yml',
               'content/enchants/deft_hands.yml',
               'io/github/butterflysmp/rpg/paper/health/DeftHandsModifierItems.class',
               'content/abilities/active_placeholder_melee_thrust.yml',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$r = New-Object IO.StreamReader($zip.GetEntry('content/enchants/deft_hands.yml').Open()); $d = $r.ReadToEnd(); $r.Close()
foreach ($k in '^effect: reload_time\s*$', '^class: ranger\s*$', '^icon: feather\s*$', '^class: universal\s*$') {
  if ($d -match ('(?m)' + $k)) { "PRESENT $k" } else { "ABSENT  $k" }
}
$zip.Dispose()
```

### R0c — the cumulative Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, **11 enchants**, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. That is GATE-sunder.md's R0c with enchants 10 → 11 (`deft_hands.yml`; `ls content/enchants/*.yml` = 11 at this head). No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair, and **none naming `deft_hands` or `reload_time`** | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | **61e722f2, 2026-11-10: PASS.** L44 `[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`, exactly as predicted. No `Refusing`, `Skipping`, `SEVERE` or `Exception` line at all (not even `volley_stone`'s known pair). |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 37 scenarios across 9 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands]`** (34 + this gate's 3: DH1, DH3, DH6), and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | **61e722f2, 2026-11-10: PASS.** L50 `[Rpg] Selftest: dev server, 37 scenarios across 9 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands]`, exactly; no `Refusing selftest` line. |

---

## THE ROWS

**The log instrument for every AUTO row:** `Select-String run\logs\latest.log -Pattern 'SELFTEST deft-hands','RELOAD '`,
read after Ben's LOGIN.

### DH1 — Deft Hands III on a Boltor: the lore line, and the reload it buys (SURVIVAL) — witness: the log + a glance

| prediction | READING |
|---|---|
| `/rpg give boltor`, held; `/rpg enchant clear`, `candidate 0 deft_hands`, `level 0 0 3`, `active 0 0`; 40 t for the health scan. The held `DUMP`'s lore carries a line naming **`Deft Hands`** and **`(-30% Reload Time)`**. `/rpg reloadtime 0`'s `REPLY`: **`60 -> 42 ticks (3.00s -> 2.10s)`**. Ben's half: the stats sheet's `Reload` line reads the same 2.1 s | **61e722f2, 2026-11-10 (run 3): SKIPPED, a staging defect.** L4304 `CMD /rpg give boltor`, then L4305 `SKIPPED no hotbar slot holds boltor` in the same tick. Every earlier gate's gives stay in the inventory until the run's final restore, so the likeliest cause is a full hotbar; that is not proven, since no inventory probe precedes the hold. Carried; a per-gate inventory snapshot is owed in the selftest (seat, 2026-11-10). |

### DH2 — a real reload on the Deft III Boltor takes 42 ticks (SURVIVAL) — witness: the RELOAD line

| prediction | READING |
|---|---|
| **Ben's input, with `/rpg mobtrace` ON:** DH1's Boltor and a stack of arrows. Fire all 8, then left-click to reload. **One `RELOAD player=<Ben> weapon=boltor authored=60 resolved=42 start=<t> completesAt=<t+42>`**, with `completesAt − start = 42` exactly. Then once more with the enchant cleared (`/rpg enchant clear`, 40 t): `authored=60 resolved=60`, a difference of 60 | **61e722f2, 2026-11-10: NOT READ.** Ben paused before it (seat, 2026-11-10); carried. |

### DH3 — every tier on every shipped reload length (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| Via `/rpg reloadtime 0`'s `REPLY`, 40 t after each enchant change. **Boltor: I `60 -> 54 ticks (3.00s -> 2.70s)`, II `60 -> 48 ticks (3.00s -> 2.40s)`. `dragons_breath` I: `40 -> 36 ticks (2.00s -> 1.80s)`. `quiver_stone` I: `34 -> 30 ticks (1.70s -> 1.50s)`**, where 3.4 floors to a 4-tick cut. The full 18-point grid is `ReloadTimeTest.deftHandsCutsEveryTierOnEveryShippedReloadAndNoneRoundsToZero`; this row reads four of its cells through the shipped stat | **61e722f2, 2026-11-10 (run 3): SKIPPED** (L4307: chained to DH1). Carried. |

### DH4 — the first shot after a shortened reload (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **Hand row.** The Deft III Boltor, empty, with arrows: reload, then hold right-click. The first shot comes no earlier than the `RELOAD` line's `completesAt`, and within one held-input spacing of it (the 4-tick grid; `GATE-locust.md` row 1 measured inputs no closer than 3 t). **The felt gap is RECORDED, not predicted**: maturity is read on the next input (`QuiverState`), so part of a short saving can be swallowed | **61e722f2, 2026-11-10: NOT READ.** Ben paused before it (seat, 2026-11-10); carried. |

### DH5 — eligibility is by class: every ranger pool, no other (UNIT) — witness: the unit row

| prediction | READING |
|---|---|
| `EnchantRollTest.deftHandsEntersTheRangerPoolAndNoOther` green in the suite at this head (ranger in; mage, melee, shield, armor out), and `DeftHandsContentInvariantTest` green (every ranger weapon but `hunters_bow` authors `reload_ticks`). **Not a boot read:** a weapon's table shows candidates rolled at random, so its absence or presence on one item says nothing about the pool. **`hunters_bow` CAN roll Deft Hands and gain nothing:** a recorded finding, by ruling | **61e722f2, 2026-11-10: PASS (unit).** `./mvnw clean test` at `61e722f2`, run before the boot: core 1567, storage 92, paper 1172, 0 failures, which includes `EnchantRollTest.deftHandsEntersTheRangerPoolAndNoOther` and `DeftHandsContentInvariantTest`. |

### DH6 — stacking reaches the one-tick floor and stops (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `quiver_stone` with Deft III (34 → 23), then `/rpg reloadtime -30`'s `REPLY`: **`34 -> 1 ticks (1.70s -> 0.05s)`**. 34 − 11 − 30 would be −7, an instant reload; the floor (`ReloadTime.MIN_TICKS`, the seat's ruling) holds it at one tick. The clock item the command mints is NOT equipped, so this is the price the command quotes, not a stat change | **61e722f2, 2026-11-10 (run 3): SKIPPED** (L4309: chained to DH3). Carried. |

### DH7 — the reload cue sounds at the shortened completion (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **Hand row**, with DH2's reload. The reload-complete sound plays about 2.1 s after the left-click (42 t), not 3 s. `Quivers.beginReload` hands the cue the resolved `reloadTicks`; this row is the client's ear | **61e722f2, 2026-11-10: NOT READ.** Carried. |

### DH8 — the grindstone strips it like any enchant (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **Hand row.** The Deft III Boltor into the grindstone, stripped. The refund matches any level-III enchant's (`GrindstoneRefund`, 35% of the list spend; Deft Hands has no cost of its own), and `/rpg reloadtime 0` then reads `60 -> 60 ticks` | **61e722f2, 2026-11-10: NOT READ.** Carried. |

---

## UNIT ROWS AND MUTATIONS (run 2026-11-06 against this branch's working tree, before this file was committed)

Rows:
- `ReloadTimeTest` (11; four new, one re-ruled);
- `DeftHandsModifierItemsTest` (5);
- `DeftHandsContentInvariantTest` (3);
- `EnchantLoaderTest` (+2);
- `EnchantEffectLineTest` (+2);
- `EnchantRollTest` (+1).

Each mutation landed with marker 1 and one changed line, its test reddened, and the file was restored byte-identical:

| mutation | killed by |
|---|---|
| `DeftHandsModifierItems.desired`: `declares` replaced by `cut > ReloadTime.NONE` (the seat's required mutant) | `DeftHandsModifierItemsTest` (3 rows, incl. `aBoltorWithDeftHandsIIIDeclaresANegativeEighteenTickCut`) |
| `ReloadTime.resolve`: the `MIN_TICKS` floor removed | `ReloadTimeTest.theResolvedDurationGoesDownToTheOneTickFloorAndNoFurther`, `…StopsAtTheOneTickFloor` |
| `ReloadTime.percentCut`: truncation (`-(R*p/100)`) instead of `floorDiv` | `ReloadTimeTest` grid, `aFractionalCutFloorsToTheLargerCut`, the stacking row |
| `EnchantDefinition`: `RELOAD_TIME` gated `MAIN_HAND_ONLY` (admits universal) | `EnchantLoaderTest.aReloadTimeEnchantOnAnyClassButRangerIsRefused` |
| `EnchantEffectLine`: ruling (b)'s wording `+N% Reload Speed` | `EnchantEffectLineTest.deftHandsSaysTheReloadTimeCutAtEveryLevel` |
| `DeftHandsContentInvariantTest`: its reload pattern made blind | its own positive control, and the invariant row |
