# GATE — WITHER-STATUS: our Wither, shaped like Scorch

**Status at `38cc9903` (the stack-top boot, 2026-10-14): R0a-c SUPERSEDED (GATE-selftest.md R0, PASS); WS-C, WS1,
WS2b, WS2c, WS3 VOID (the aim defect: every `/rpg apply` and `/rpg mobdamage` was refused "Look at a mob"; carried to
the next boot after #175's fix); WS3-P, WS4, WS5, WS6 NOT READ; WS8, WS9 UNWITNESSED as written; 0 PASS, 0 FAIL.
NOT a merge candidate this round: the VOID rows are its bar.**

**The boot, 2026-10-14, on `38cc9903` (#175's head, the stack top), Paper 26.1.2 build 74:**
- CC booted it with `./scripts/dev-server.sh --refresh-content` on the seat's word; Ben was the only player. His
  LOGIN is L80 (`BaronVonYeetus ... logged in`, 22:47:02). Line numbers are that boot's `run/logs/latest.log`.
- `/rpg selftest all` ran twice. **Run 1 (L86-L1087) was in CREATIVE and certifies NOTHING** (the seat's ruling;
  L1087 `RESTORED ... gamemode=CREATIVE`). **Run 2 (L1091-L2098, SURVIVAL, after `/gamemode survival` at L1088) is
  the run of record**: every AUTO reading below is from inside its row's own START ... DONE bracket in run 2.
- **Ben's hand steps.** His words, relayed by the seat: *"all done, everything looks good"*, then *"I ran all the
  self checks and they are fine."* **The seat's ruling, 2026-10-14:** the second is Ben's explicit confirmation of
  each check on the hand-step list he was given -- **MC3 (slot), MC1, MC2, LS1, D1, G1, G3, WB1, ST2, ST6** -- so
  those sight halves are PASS, itemised by that list. Ben was in CREATIVE from 23:15:46 to 23:16:09 (L2102-L2104)
  during his hand steps; which check, if any, fell in that window is not recorded. His hand window ran with the mob
  trace OFF (L2098 `trace=false`), so a hand cast has no log line either way.
- **The aim defect** (GATE-selftest.md ST5): `tp @s ~ ~ ~ facing entity <e> eyes` turns a player from the FEET, so
  every selftest row aimed that way missed: SB2, LB2, WB2, WB3, WS-C, WS1, WS2b, WS2c, WS3 are VOID this boot and
  carry to the next boot after #175's AIM fix.
- **Observation, unexplained:** MOBREMOVE lines run through run 2, many naming uuids that appear nowhere else.

*Amended 2026-10-01, BEFORE ANY BOOT, at the seat's rulings 5–7:* the `/rpg apply` reply now says **Refused** for an
immune target (WS2c reads it beside DOTTICK's absence); forgetting a live wither strips the vanilla potion (new row
WS9, UNWITNESSED); an unknown immune key is refused at load with a `Refusing` WARN (R0c's pattern). Rows 13 → 14.

Every prediction below was written **before** any row of this file was read, and **this file is COMMITTED before the
boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once its row has been read.
**NO BOOT until the seat says so**, after the seat has diffed every slice.

**The stack (the seat, 2026-10-01):** `#168 → #169 → #170 → #171 → LEGACY-B → WITHER-STATUS (this) →
WITHERED-SHORTBOW`. **One boot, at the stack top.** The seat's S1 ruling: **this gate and the bow's are read in the same
boot**, the clock and percent rows here through `/rpg apply`, the weapon-capped path in the bow's gate. No TEMP fixture.

**Readings count only if Ben's LOGIN appears in THIS boot's log.** A row whose prediction is a LOG LINE is PASS when
that line appears after the LOGIN with every predicted field as written.

```
ROWS     14   R0a R0b R0c
              WS-C WS1 WS2b WS2c WS3 WS3-P WS4 WS5 WS6 WS8 WS9
         ──
         14   = 3 R0 + 11 WS     git grep -c '^### R0\|^### WS' <ref> -- GATE-wither-status.md
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
- **To apply:** look at the mob and run `/rpg apply withering 200`. On a non-immune mob the reply is
  `Applied withering x1 (200t) to …`; **on an immune one it is `Refused: <type> is immune to withering
  (statuses/withering.yml); nothing was applied`** (ruling 5; `ApplyArgsTest` pins the text). The reply says what the
  command decided; **DOTTICK says what landed**, and the tick rows read DOTTICK.

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
| WS-C | AUTO-PARTIAL | two knells, **the second 2 blocks from the first** (`~4 ~ ~0` and `~4 ~ ~2`); both custom HPs before and after `/rpg mobdamage 1000000` | no blast particles seen |
| WS1 | AUTO | the GS-20 zombie (**teleported to `~4`** rather than Ben stepping back); `/rpg apply withering 200`; DOTTICK over 240 t | — |
| WS2b | AUTO | the GS-100 zombie at `~4`; the same | — |
| WS2c | AUTO-PARTIAL | the Knell and a wither skeleton at `~3`: the `Refused` reply and no DOTTICK over 240 t each; the GS-100 control's `Applied` reply and its DOTTICK | no wither swirl seen |
| WS3 | AUTO-PARTIAL | the GS-20 zombie at `~4` and its neighbour at `~4 ~ ~2`; `/rpg mobdamage 19`, the apply, DOTTICK, the target's death (`PROBE … none`) and the neighbour's custom HP | no explosion seen |

**The R0 moved again** (the seat's ruling c): the stack's one R0 is now `GATE-selftest.md`'s, on #175's head. The
SUPERSEDED note under this gate's R0 heading names `GATE-withered-shortbow.md`; read it as naming `GATE-selftest.md`.

---

## R0

> **INSTRUMENT NOTE, 2026-10-05, the seat's ruling 1, added before the stack-top boot. No prediction moves.** At the
> stack-top boot, this gate's R0 rows are SUPERSEDED by `GATE-withered-shortbow.md`'s R0 on `<sha>`; recorded
> SUPERSEDED, never PASS or FAIL. Intermediate facts in R0b/R0c are not read. *`<sha>` is the stack top's head as that
> R0a reads it in the boot: a commit beneath the top cannot name the top's hash, because the top is rebased onto this
> note.* R0c's two absences that are true of every tree above this one (no `Refusing` naming `withering.yml`, no
> `element 'wither' applies status` warning) are carried by the top's R0c.

### R0a — the build line names this branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, WITHER-STATUS's head, shortened. Not `-dirty`, not `unknown`, not LEGACY-B's or any head beneath | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **38cc9903, 2026-10-14: SUPERSEDED** -- GATE-selftest.md R0, read PASS at 38cc9903 (L42 `[Rpg] Build: 38cc9903`). Not read here. |

### R0b — the jar carries the status, the element's accrual and the shared store

| prediction | instrument | READING |
|---|---|---|
| Entries PRESENT: `content/statuses/withering.yml`, core `combat/DotRates.class`, core `combat/Wither.class`, paper `adapter/DotStatus.class`, paper `adapter/DotTick.class`, LEGACY-B's `adapter/FireballDrive.class`. ABSENT: the control. **As YAML keys (anchored):** `withering.yml` reads `kind: wither`; `elements/wither.yml` reads `applies_status: withering`; control `elements/fire.yml` reads `applies_status: scorch` | the scan below | **38cc9903, 2026-10-14: SUPERSEDED** -- GATE-selftest.md R0b, read PASS at 38cc9903. The intermediate facts are not read. |

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
| `Loaded 11 abilities, 29 visuals, **5 statuses**, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 15 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. Against LEGACY-B's R0c prediction: **statuses 4 → 5**, every other count unchanged. No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair; **no `element 'wither' applies status` warning** (the validator accepts a wither kind) | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception','applies status'` | **38cc9903, 2026-10-14: SUPERSEDED** -- GATE-selftest.md R0c, read PASS at 38cc9903 (L44; no `applies status` warning). The intermediate facts are not read. |

---

## THE STATUS

### WS-C — negative control, RUN FIRST: a plain death does not blast (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| `GATE-ignite.md` I7's shape. `/rpg spawn knell` twice, side by side (NoAI as above). Look at the first and `/rpg mobdamage 1000000`: it dies. **No blast, no `ignite_blast` particles, the second knell's nameplate does not move.** So a blast in WS3-P is caused by something, and silence in WS3 means something | **38cc9903, 2026-10-14: VOID** (the seat's ruling: everything AUTO in this file from this boot is VOID) -- the aim defect (`tp ... facing entity ... eyes` turns from the FEET). Staging was right: two Knells, L1834 and L1843 `MOBSEED ... wither_skeleton gs=100 ... max=360`, 360/360 (L1854). L1856 `/rpg mobdamage 1000000` replied L1860 `"Look at a mob." [red]`; the target stayed 360/360, alive (L1861) -- no death was presented. Observation: MOBREMOVE lines L1857-L1859 follow, unexplained. |

### WS1 — the clock and the percent arm, on a GS-20 zombie (SURVIVAL) — witness: DOTTICK

| prediction | READING |
|---|---|
| GS-20 zombie (expected `max=20`). `/rpg apply withering 200`. **Five** lines `DOTTICK <its uuid> zombie status=withering applier=- sent=1.000 cap=2.000 tick=<n>`, their `tick=` values **40 apart**, the first **about 40 ticks** after the command (the first tick is one period in, never on application), and **no sixth**. **No fire on the mob** (wither sets no fire ticks). Secondary: five dark-gray `✖ 1` popups | **38cc9903, 2026-10-14: VOID** -- the aim defect. The target was right (L1877 `MOBSEED 41d8bf79 zombie gs=20 source=stored max=20`); L1890 `/rpg apply withering 200` replied L1899 `"Look at a mob to apply a status." [red]`; 0 DOTTICK in the bracket; 20/20 before and after (L1889, L1900). This voids GATE-selftest.md ST5's WS1 half. |

### WS2b — the cap BINDS, by mob scaling (SURVIVAL) — witness: DOTTICK (WITNESSED)

| prediction | READING |
|---|---|
| **The seat, 2026-10-01.** A **GS-100 zombie** (expected `max=100`; 5% = 5 > 2.0). `/rpg apply withering 200`: five lines with **`sent=2.000 cap=2.000`**: the cap binds. **The control, same block:** WS1's GS-20 zombie read **`sent=1.000 cap=2.000`**, the percent arm. Each `sent` is `min(0.05 × max, 2.0)` from that mob's own `MOBSEED max=` line | **38cc9903, 2026-10-14: VOID** -- the aim defect. The target was right (L1909 `MOBSEED bc8b4974 zombie gs=100 source=rolled max=100`); L1920 apply replied L1932 `"Look at a mob to apply a status." [red]`; 0 DOTTICK; 100/100 (L1919, L1933). Its control, WS1, is VOID too. |

### WS2c — immunity: wither skeletons and the Knell (SURVIVAL) — witness: the REPLY + DOTTICK ABSENCE + Ben, with a control

| prediction | READING |
|---|---|
| Q-W6. `/rpg spawn knell`, and `/summon wither_skeleton ~3 ~ ~ {NoAI:1b,PersistenceRequired:1b}`. Look at each and run `/rpg apply withering 200`; **record the command and the target each time** (a null observation without them proves nothing). **The reply, for each (the Knell is a `wither_skeleton`): `Refused: wither_skeleton is immune to withering (statuses/withering.yml); nothing was applied`.** Over **240 ticks** after each: **no DOTTICK line naming that uuid, no wither swirl, the nameplate does not move.** **Control, immediately after:** the same command on a GS-100 zombie replies **`Applied withering x1 (200t) to ZOMBIE`** and **does** produce DOTTICK lines, so both instruments were live. *The Wither boss is not staged (it explodes on spawn); its immunity is by the same list and is recorded UNWITNESSED* | **38cc9903, 2026-10-14: VOID** -- the aim defect. All three applies were refused for the AIM, never for immunity: the Knell (L1944 max=360; L1957 to L1965), the wither skeleton (L1973 max=100; L1981 to L1987) and the zombie control (L1994; L2002 to L2004), each `"Look at a mob to apply a status." [red]`. No `Refused: ... immune` reply, and the control produced no DOTTICK, so every absence here is hollow. |

### WS3 — THE WS2 ROW: a never-scorched mob killed by Wither does NOT explode (SURVIVAL) — witness: Ben + DOTTICK

| prediction | READING |
|---|---|
| WS2 as narrowed by Q-W8. A **fresh GS-20 zombie**, never scorched, with a second zombie two blocks from it. Look at the first: `/rpg mobdamage 19` (nameplate **1/20**), then `/rpg apply withering 200`. About 40 ticks later the line `DOTTICK <its uuid> zombie status=withering … sent=1.000` appears and **it dies. NO explosion, no `ignite_blast`, the neighbour's nameplate does not move.** Unit halves: `ElementAccrualTest`, `DotStatusTest`; **`onEntityDeath` reading `scorch()` only has no unit witness, and this row is its only one** | **38cc9903, 2026-10-14: VOID** -- the aim defect. The target was right (L2017 `MOBSEED b3d1056a zombie gs=20 ... max=20`); L2035 `/rpg mobdamage 19` replied L2036 `"Look at a mob." [red]` and L2039 apply replied L2041 `"Look at a mob to apply a status." [red]`; 20/20, alive (L2042), the neighbour 100/100 (L2043). No death was presented. `onEntityDeath` reading `scorch()` only is still unwitnessed. |

### WS3-P — the positive control: scorched AND withered still explodes (SURVIVAL) — witness: Ben + DOTTICK

| prediction | READING |
|---|---|
| Q-W8, Ben: *"8, yes"*. A fresh GS-20 zombie with a neighbour, `/rpg mobdamage 18` (**2/20**). Look at it: `/rpg apply withering 200`, then **within one second** `/rpg apply scorch 120`. **Why that kills by wither (arith, not a reading):** with the scorch `d` ticks after the wither and `d < 20`, scorch's first tick (`d + 20`) lands before wither's (40), and wither's is the second 1.0. **Predicted lines, in order:** `DOTTICK … status=scorch … sent=1.000`, then `DOTTICK … status=withering … sent=1.000`, then **it dies and EXPLODES** (`ignite_blast`, the neighbour takes damage). **The last line before the death names `withering`.** If it names `scorch` (the second command came later than 20 ticks), Q-W8's exact case is **not** witnessed: re-stage, and record each attempt | **38cc9903, 2026-10-14: NOT READ.** Never staged: no `/rpg apply` among Ben's commands (L2099-L2108), and 0 `status=withering` lines in the log. |

### WS4 — credit (SURVIVAL) — witness: none here; MOVED to the bow's gate

| prediction | READING |
|---|---|
| **Not readable under `/rpg apply`**, by construction: the three-argument `applyStatus` passes no applier, so every line here reads `applier=-`. **The credit is read in `GATE-withered-shortbow.md` WB3**, where the arrow's applier is Ben (`applier=<Ben's uuid>`). Recorded here so the row is not counted as passed | **38cc9903, 2026-10-14: NOT READ.** Moved to GATE-withered-shortbow.md WB3, which is VOID at this boot. |

### WS5 — a re-apply resets the window and does not restart the clock (SURVIVAL) — witness: DOTTICK

| prediction | READING |
|---|---|
| GS-100 zombie. `/rpg apply withering 200`, then **repeat the command by hand as fast as chat allows for about 6 seconds**. **DOTTICK lines keep arriving every 40 ticks throughout** (`tick=` spacing 40, never a gap of 80 or more while re-applying). After the last re-apply, **five more lines**. The 10-tick re-hit case is `DotStatusTest`'s; this row is its live counterpart at human speed | **38cc9903, 2026-10-14: NOT READ.** Never staged; 0 `status=withering` lines. |

### WS6 — the look, and exactly one damage per tick (SURVIVAL) — witness: Ben + the nameplate

| prediction | READING |
|---|---|
| Q-W7 (a). A **fresh GS-100 zombie** (`100/100`). `/rpg apply withering 200`. **The vanilla wither swirl shows on it for the window.** After **at least 12 s**: five DOTTICK lines of `sent=2.000`, and the **nameplate reads 90/100**. **If the potion's own damage leaked past the gate**, each of its five 1.0 vanilla ticks would land as 5% of the custom max (proportional, M13) and it would read about **65/100**. *UNVERIFIED: whether each tokened potion tick flashes the hurt animation* | **38cc9903, 2026-10-14: NOT READ.** Never staged (no apply was issued); the sole witness for the WITHER damage gate and `witherLook` is still owed. |

### WS8 — players (SURVIVAL) — recorded UNWITNESSED

| prediction | READING |
|---|---|
| Q-W5 (it hurts players) is **not stageable**: nothing can hit a player with a weapon until PvP is live, and `/rpg apply` targets a mob (`rayTraceEntities` excludes players). **Recorded as unwitnessed, not as passed** | **38cc9903, 2026-10-14: UNWITNESSED**, as written (no PvP to stage). Not read, not passed. |

### WS9 — forgetting a live wither strips the vanilla potion (SURVIVAL) — recorded UNWITNESSED

| prediction | READING |
|---|---|
| Ruling 6. **Not stageable in this boot:** the case it guards is a withered PLAYER quitting (nothing can wither a player, WS8), and the mob case is a withered mob unloading with its chunk, whose strip-before-save ordering is UNVERIFIED (`WitherPotion`'s javadoc; the quit ordering was read from the jar). `WitherPotionTest` is the decision's witness; the strip call is not witnessed. **Recorded as unwitnessed, not as passed** | **38cc9903, 2026-10-14: UNWITNESSED**, as written. `WitherPotionTest` witnesses the decision. Not read, not passed. |

---

## WHAT THIS GATE CANNOT SEE

- **A real wither skeleton's vanilla wither on a player under ours is swallowed** while ours runs (PLAN 6.4). Live only
  with PvP.
- **The potion strip on forget** (WS9): a player cannot be withered, and the chunk-unload ordering is not read.
- *(Two items were here and were FIXED before any boot, rulings 6 and 7: a quitting player keeping the potion, and the
  immune list unchecked against the entity types. An unknown key now reads `Refusing immune entity type '…' in status
  'withering.yml'` at boot, which R0c's `Refusing` pattern catches.)*

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
| *rulings 5–7, run against `5ac914d1`:* | | |
| MUT-NOREGISTRY | the `KNOWN_ENTITY_TYPES` check disabled | `anUnknownImmuneKeyIsRefusedByNameAndTheRestLoads` only |
| MUT-NOREFUSAL | `immunityRefusal` always empty (the replacement contains the needle, so "original gone" reads 1 by construction; delta 2) | `anImmuneTargetIsRefusedByNameAndTheControlsAreNot` only |
| MUT-FORGETFALSE | `DotStatus.forget` always returns false | `WitherPotionTest.onlyALiveWitherHasItsPotionStripped` |
| MUT-STRIPALWAYS | `forgetAndStrip` strips whether or not the wither was live | `WitherPotionTest.onlyALiveWitherHasItsPotionStripped` |

**Sole witnesses, by construction (no unit test reaches a live entity or a raised event):** WS3 for `onEntityDeath`
reading the scorch store only; WS6 for the WITHER damage gate in `onEnvironmentalDamage` and for `witherLook`; WS2c for
the immunity check at the two apply sites in `BukkitCombatant` (the accrual's immune SET is unit-tested; the comparison
against the entity's type key is not).
