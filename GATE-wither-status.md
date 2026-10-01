# GATE — WITHER-STATUS: our Wither, shaped like Scorch

**Status: NOT RUN.**

Every prediction below was written **before** any row of this file was read, and **this file is COMMITTED before the
boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once its row has been read.
**NO BOOT until the seat says so**, after the seat has diffed every slice.

**The stack (the seat, 2026-10-01):** `#168 → #169 → #170 → #171 → LEGACY-B → WITHER-STATUS (this) →
WITHERED-SHORTBOW`. **One boot, at the stack top.** The seat's S1 ruling: **this gate and the bow's are read in the same
boot**, the clock and percent rows here through `/rpg apply`, the weapon-capped path in the bow's gate. No TEMP fixture.

**Readings count only if Ben's LOGIN appears in THIS boot's log.** A row whose prediction is a LOG LINE is PASS when
that line appears after the LOGIN with every predicted field as written.

```
ROWS     13   R0a R0b R0c
              WS-C WS1 WS2b WS2c WS3 WS3-P WS4 WS5 WS6 WS8
         ──
         13   = 3 R0 + 10 WS     git grep -c '^### R0\|^### WS' <ref> -- GATE-wither-status.md
```

**Plan:** `PLAN-wither.md` on `docs/batch-surveys` (`fe73f5ff`): Ben's words and answers (2026-09-30), the seat's WS1–WS4,
S1–S3 and its Q-W6/Q-W7 fills, Ben's Q-W10 overrule, and the seat's 2026-10-01 mob-scaling cap rows. The rows are §10's
draft, re-drafted against DOTTICK's exact text now that it exists (§10 said they would be).

## GAME MODE

**SURVIVAL on every row** (the creative-divergence register).

## THE ORDER IN THE ONE BOOT

Every gate beneath, through `GATE-legacy-b.md`, then **these**, then `GATE-withered-shortbow.md`.

## THE DOTTICK LINE (the instrument for every tick row)

One line per DoT tick, from the shared store, for scorch AND wither, **written before the damage it reports**, while
`/rpg mobtrace` is ON (`DotTick.line`, pinned by `DotTickTest`):

```
DOTTICK <victim uuid> <type> status=<status id> applier=<uuid or -> sent=<%.3f> cap=<%.3f> tick=<server tick>
```

- `sent` is `min(0.05 × max, cap)`, what the store sent. `cap` is the cap in force. `tick` is the server's tick.
- **Under `/rpg apply` the applier is `-`.** `RpgCommand.apply` calls the three-argument `applyStatus`, which passes no
  applier and no damage (`CombatantHandle`), so the cap is the shared undeclared one, **2.0**, and there is no credit.
- **Every amount below is predicted from that mob's own `MOBSEED … max=` line**, read in the boot (the seat,
  2026-10-01): `sent = min(0.05 × max, cap)`. A zombie's max is its gear score (`MobScaling`: `20 × 5 × gs / 100`), so
  the expected `max=` is written beside each row; a different `max=` is a reading, and the prediction follows the line.

## Set-up

- **`/gamerule spawn_mobs false`**, **`/time set 18000`**, **`/rpg mobtrace`** ON (the reply now names DOTTICK).
- **No accessories and no armour**; level as the earlier gates leave it (no weapon is used in this gate).
- **Two zombie recipes**, both `NoAI` so a survival Ben is not attacked, and both read off their `MOBSEED` line:
  - **GS-100**: #168's test zombie (`GATE-melee-cell.md`, *THE TEST ZOMBIE*): `/summon zombie ~<dx> ~ ~<dz>
    {NoAI:1b,PersistenceRequired:1b,IsBaby:0b,Tags:["t"]}` → **`MOBSEED <uuid> zombie gs=100 source=rolled max=100`**.
  - **GS-20**: `/rpg spawn zombie 20` (it appears at Ben's feet), then at once `/data merge entity
    @e[type=zombie,sort=nearest,limit=1] {NoAI:1b,PersistenceRequired:1b}` and step back four blocks →
    **`MOBSEED <uuid> zombie gs=20 source=… max=20`**. *`/rpg spawn` randomises vanilla spawn data, so it may be a
    baby or carry gear; the `max=` line is what is read, and a baby zombie's base max is also 20.*
- **To apply:** look at the mob and run `/rpg apply withering 200`. **The reply ("Applied withering x1 (200t) to …")
  is printed whether or not the status landed** (the command replies before the entity thread runs the arm, and the
  immunity refusal is inside it), **so a reply is never a reading**; DOTTICK is.

---

## R0

### R0a — the build line names this branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, WITHER-STATUS's head, shortened. Not `-dirty`, not `unknown`, not LEGACY-B's or any head beneath | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries the status, the element's accrual and the shared store

| prediction | instrument | READING |
|---|---|---|
| Entries PRESENT: `content/statuses/withering.yml`, core `combat/DotRates.class`, core `combat/Wither.class`, paper `adapter/DotStatus.class`, paper `adapter/DotTick.class`, LEGACY-B's `adapter/FireballDrive.class`. ABSENT: the control. **As YAML keys (anchored):** `withering.yml` reads `kind: wither`; `elements/wither.yml` reads `applies_status: withering`; control `elements/fire.yml` reads `applies_status: scorch` | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'content/statuses/withering.yml',
               'io/github/butterflysmp/rpg/core/combat/DotRates.class',
               'io/github/butterflysmp/rpg/core/combat/Wither.class',
               'io/github/butterflysmp/rpg/paper/adapter/DotStatus.class',
               'io/github/butterflysmp/rpg/paper/adapter/DotTick.class',
               'io/github/butterflysmp/rpg/paper/adapter/FireballDrive.class',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
function Text($p) { $r = New-Object IO.StreamReader($zip.GetEntry($p).Open()); $t = $r.ReadToEnd(); $r.Close(); $t }
foreach ($pair in @(@('content/statuses/withering.yml', 'kind: wither'),
                    @('content/elements/wither.yml', 'applies_status: withering'),
                    @('content/elements/fire.yml', 'applies_status: scorch'))) {
  if ((Text $pair[0]) -match ('(?m)^' + [regex]::Escape($pair[1]) + '\s*$')) { "PRESENT $($pair[0]) $($pair[1])" } else { "ABSENT  $($pair[0]) $($pair[1])" }
}
$zip.Dispose()
```

### R0c — one status more, and nothing else moved

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 29 visuals, **5 statuses**, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 15 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. Against LEGACY-B's R0c prediction: **statuses 4 → 5**, every other count unchanged. No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair; **no `element 'wither' applies status` warning** (the validator accepts a wither kind) | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception','applies status'` | |

---

## THE STATUS

### WS-C — negative control, RUN FIRST: a plain death does not blast (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `GATE-ignite.md` I7's shape. `/rpg spawn knell` twice, side by side (NoAI as above). Look at the first and `/rpg mobdamage 1000000`: it dies. **No blast, no `ignite_blast` particles, the second knell's nameplate does not move.** So a blast in WS3-P is caused by something, and silence in WS3 means something | |

### WS1 — the clock and the percent arm, on a GS-20 zombie (SURVIVAL) — witness: DOTTICK

| prediction | READING |
|---|---|
| GS-20 zombie (expected `max=20`). `/rpg apply withering 200`. **Five** lines `DOTTICK <its uuid> zombie status=withering applier=- sent=1.000 cap=2.000 tick=<n>`, their `tick=` values **40 apart**, the first **about 40 ticks** after the command (the first tick is one period in, never on application), and **no sixth**. **No fire on the mob** (wither sets no fire ticks). Secondary: five dark-gray `✖ 1` popups | |

### WS2b — the cap BINDS, by mob scaling (SURVIVAL) — witness: DOTTICK (WITNESSED)

| prediction | READING |
|---|---|
| **The seat, 2026-10-01.** A **GS-100 zombie** (expected `max=100`; 5% = 5 > 2.0). `/rpg apply withering 200`: five lines with **`sent=2.000 cap=2.000`**: the cap binds. **The control, same block:** WS1's GS-20 zombie read **`sent=1.000 cap=2.000`**, the percent arm. Each `sent` is `min(0.05 × max, 2.0)` from that mob's own `MOBSEED max=` line | |

### WS2c — immunity: wither skeletons and the Knell (SURVIVAL) — witness: DOTTICK ABSENCE + Ben, with a control

| prediction | READING |
|---|---|
| Q-W6. `/rpg spawn knell`, and `/summon wither_skeleton ~3 ~ ~ {NoAI:1b,PersistenceRequired:1b}`. Look at each and run `/rpg apply withering 200`; **record the command and the target each time** (a null observation without them proves nothing). Over **240 ticks** after each: **no DOTTICK line naming that uuid, no wither swirl, the nameplate does not move.** **Control, immediately after:** the same command on a GS-100 zombie **does** produce DOTTICK lines, so the instrument was live. *The Wither boss is not staged (it explodes on spawn); its immunity is by the same list and is recorded UNWITNESSED* | |

### WS3 — THE WS2 ROW: a never-scorched mob killed by Wither does NOT explode (SURVIVAL) — witness: Ben + DOTTICK

| prediction | READING |
|---|---|
| WS2 as narrowed by Q-W8. A **fresh GS-20 zombie**, never scorched, with a second zombie two blocks from it. Look at the first: `/rpg mobdamage 19` (nameplate **1/20**), then `/rpg apply withering 200`. About 40 ticks later the line `DOTTICK <its uuid> zombie status=withering … sent=1.000` appears and **it dies. NO explosion, no `ignite_blast`, the neighbour's nameplate does not move.** Unit halves: `ElementAccrualTest`, `DotStatusTest`; **`onEntityDeath` reading `scorch()` only has no unit witness, and this row is its only one** | |

### WS3-P — the positive control: scorched AND withered still explodes (SURVIVAL) — witness: Ben + DOTTICK

| prediction | READING |
|---|---|
| Q-W8, Ben: *"8, yes"*. A fresh GS-20 zombie with a neighbour, `/rpg mobdamage 18` (**2/20**). Look at it: `/rpg apply withering 200`, then **within one second** `/rpg apply scorch 120`. **Why that kills by wither (arith, not a reading):** with the scorch `d` ticks after the wither and `d < 20`, scorch's first tick (`d + 20`) lands before wither's (40), and wither's is the second 1.0. **Predicted lines, in order:** `DOTTICK … status=scorch … sent=1.000`, then `DOTTICK … status=withering … sent=1.000`, then **it dies and EXPLODES** (`ignite_blast`, the neighbour takes damage). **The last line before the death names `withering`.** If it names `scorch` (the second command came later than 20 ticks), Q-W8's exact case is **not** witnessed: re-stage, and record each attempt | |

### WS4 — credit (SURVIVAL) — witness: none here; MOVED to the bow's gate

| prediction | READING |
|---|---|
| **Not readable under `/rpg apply`**, by construction: the three-argument `applyStatus` passes no applier, so every line here reads `applier=-`. **The credit is read in `GATE-withered-shortbow.md` WB3**, where the arrow's applier is Ben (`applier=<Ben's uuid>`). Recorded here so the row is not counted as passed | |

### WS5 — a re-apply resets the window and does not restart the clock (SURVIVAL) — witness: DOTTICK

| prediction | READING |
|---|---|
| GS-100 zombie. `/rpg apply withering 200`, then **repeat the command by hand as fast as chat allows for about 6 seconds**. **DOTTICK lines keep arriving every 40 ticks throughout** (`tick=` spacing 40, never a gap of 80 or more while re-applying). After the last re-apply, **five more lines**. The 10-tick re-hit case is `DotStatusTest`'s; this row is its live counterpart at human speed | |

### WS6 — the look, and exactly one damage per tick (SURVIVAL) — witness: Ben + the nameplate

| prediction | READING |
|---|---|
| Q-W7 (a). A **fresh GS-100 zombie** (`100/100`). `/rpg apply withering 200`. **The vanilla wither swirl shows on it for the window.** After **at least 12 s**: five DOTTICK lines of `sent=2.000`, and the **nameplate reads 90/100**. **If the potion's own damage leaked past the gate**, each of its five 1.0 vanilla ticks would land as 5% of the custom max (proportional, M13) and it would read about **65/100**. *UNVERIFIED: whether each tokened potion tick flashes the hurt animation* | |

### WS8 — players (SURVIVAL) — recorded UNWITNESSED

| prediction | READING |
|---|---|
| Q-W5 (it hurts players) is **not stageable**: nothing can hit a player with a weapon until PvP is live, and `/rpg apply` targets a mob (`rayTraceEntities` excludes players). **Recorded as unwitnessed, not as passed** | |

---

## WHAT THIS GATE CANNOT SEE

- **A real wither skeleton's vanilla wither on a player under ours is swallowed** while ours runs (PLAN 6.4). Live only
  with PvP.
- **A player who quits mid-wither keeps the vanilla potion** (vanilla saves effects) while our store forgets them; on
  rejoin the potion's ticks reroute as victim-credited damage until it expires. Live only with PvP.
- **The immune list naming a real entity type** is not checked at boot (it needs the registry); a typo there would make
  wither skeletons non-immune, and WS2c is the only witness.

## MUTATIONS (run 2026-10-01 against `ba21906d`, before this file was committed)

Each: the needle unique, marker present 1 / original gone 0 (except where noted), line delta against a pristine copy,
a focused run, restored by `cp` and proved byte-identical with `cmp`.

| mutation | edit | reddened |
|---|---|---|
| MUT-WITHERASSCORCH | `kindOf` maps `Wither` to `SCORCH` | `aWitherHitNeverAnswersAccruesScorchAndAFireHitStillDoes`, `aSurvivingWitherHitAccruesWitherAtWithersNumbers` |
| MUT-ONEINSTANCE | `AdapterContext.wither()` overridden to return `scorch` (an insertion; delta 1) | `DotStatusTest.aWitherApplicationNeverScorches` |
| MUT-CTXRATES | the context builds wither at `Scorch.RATES` | `aWitherApplicationNeverScorches` |
| MUT-WITHERWINDOW | `accrue` uses `Scorch.RATES` for WITHER | `aSurvivingWitherHitAccruesWitherAtWithersNumbers` |
| MUT-NOIMMUNE | the accrual drops the immune set | `aSurvivingWitherHitAccruesWitherAtWithersNumbers` |
| MUT-TRACEAFTER | the trace moved after `sink.deal` (the replacement contains the needle, so "original gone" reads 1 by construction; delta 2) | `scorchAndWitherBothTraceEveryTickBeforeItsDamage` |
| MUT-NOTRACE | the trace call deleted | `scorchAndWitherBothTraceEveryTickBeforeItsDamage` |
| MUT-IDFALLBACK | `statusIdOf` always returns the kind name | `theStoreIsNamedForTheLoadedStatusOfItsKind` |
| MUT-LOADERARM | `StatusLoader` without the `wither` arm | the three wither `StatusLoaderTest` rows |

**Sole witnesses, by construction (no unit test reaches a live entity or a raised event):** WS3 for `onEntityDeath`
reading the scorch store only; WS6 for the WITHER damage gate in `onEnvironmentalDamage` and for `witherLook`; WS2c for
the immunity check at the two apply sites in `BukkitCombatant` (the accrual's immune SET is unit-tested; the comparison
against the entity's type key is not).
