# GATE — LEGACY-B: the Blaze King's Staff shoots a real fireball

**Status at `e82df2b3` (the stack-top boot, 2026-11-14; run 3, L2206-L3657, is the run of record proposed to the seat; the boot account is `GATE-deft-hands.md`'s): R0a-c SUPERSEDED (GATE-deft-hands.md R0, all four AS PREDICTED); LB2 AS PREDICTED (as amended: the crit); LB5 (a) and (b) AS PREDICTED, the counts READ (both flights last sighted at `dx 7.50`), (c) not driven, Ben's half "Yes" (not bound to one run); LB1, LB3, LB4, LB4b, LB6-LB11 NOT READ (optional; Ben's answer on the optional rows is NOT STATED). This gate's bars (LB2, LB5's AUTO half) are met, pending the seat.**

**Status at `61e722f2` (the stack-top boot, 2026-11-10; run 3 is the run of record; the boot account is `GATE-deft-hands.md`'s): R0a-c SUPERSEDED (GATE-deft-hands.md R0, PASS); LB2 FAIL as staged (a dying mob intercepted the fireball: `PaperCombatWorld.castRay` accepts dying entities); LB5 VACUOUS as written (its impact witness: (a) ended on the same kind of corpse, (b) reached the wall once the body's one-step lag is counted); LB1, LB3, LB4, LB4b, LB6-LB11 NOT READ. NOT a merge candidate: it waits for the castRay fix and the next boot.**

**Status at `38cc9903` (the stack-top boot, 2026-10-14): R0a-c SUPERSEDED (GATE-selftest.md R0, PASS); LB2 VOID (the
aim defect; carries to the next boot after #175's fix); LB5 PARTIAL (counts match, no impact witness, no control);
10 NOT READ (LB1, LB3, LB4, LB4b, LB6-LB11); 0 PASS, 0 FAIL. NOT a merge candidate this round: LB2 is its bar.**

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

*Amended 2026-11-06, BEFORE THE NEXT BOOT, at the seat's instruction (instrument only; no recorded reading moves): LB5 gains an IMPACT WITNESS.* The 38cc9903 reading found that LB5's counts "cannot tell a guard that held from a fireball that never arrived". #175's selftest now samples LB5's first (a) shot and its (b) shot every tick with `probe_positions` (`GATE-legacy-b.yml`), so the reading says where each body was last seen. The witness and its prediction sit beneath LB5's table. LB5's original prediction and reading stand as written.

Every prediction below was written **before** any row of this file was read, and **this file is COMMITTED before the
boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once its row has been read.
**NO BOOT until the seat says so**, after the seat has diffed every slice.

**The stack (the seat, 2026-10-01):** `#168 → #169 → #170 → #171 (ab23ce6f) → LEGACY-B (this) → WITHER-STATUS →
WITHERED-SHORTBOW`. **One boot, at the stack top**, and every gate beneath it is read in that boot. Nothing in
#168–#171 changes.

**Readings count only if Ben's LOGIN appears in THIS boot's log.** A row whose prediction is a LOG LINE is PASS when
that line appears verbatim after the LOGIN.

```
ROWS     15   R0a R0b R0c
              LB1 LB2 LB3 LB4 LB4b LB5 LB6 LB7 LB8 LB9 LB10 LB11
         ──
         15   = 3 R0 + 12 LB     git grep -c '^### R0\|^### LB' <ref> -- docs/gates/GATE-legacy-b.md
```

**Plan:** `PLAN-legacy-b.md` on `docs/batch-surveys` (`fe73f5ff`): Ben's words (2026-09-29), his Q-B1/Q-B4/Q-B6
answers, and the seat's Q-B2 (1-prime), Q-B3 (COMPENSATE), Q-B5 (the drive removes the body at the end of its
lifetime; the orphan accepted) and the 2026-10-01 stacking ruling. The rows are §6's draft, with the despawn-time and
visuals rows added for Q-B4 and Q-B6.

## GAME MODE

**SURVIVAL on every row.** The staff costs mana, and creative removes costs (the creative-divergence register).

## THE ORDER IN THE ONE BOOT, AND TWO ROWS BENEATH THAT THIS SLICE CONTRADICTS

#168's rows, then `GATE-level.md`, `GATE-nexus-polish.md`, `GATE-legacy-a.md`, then **these**, then
`GATE-wither-status.md` and `GATE-withered-shortbow.md`.

> **REPORTED TO THE SEAT, NOT AMENDED HERE** (nothing in #168–#171 changes). `GATE-legacy-a.md`'s **R0b predicts
> `content/weapons/blaze_kings_staff.yml` ABSENT** and its **R0c predicts 14 weapons and 27 visuals**. This slice
> re-adds the staff and two visuals, so a boot of this tree or of anything above it reads the staff **PRESENT**, and the
> counts higher. The same holds for every R0a beneath (each names its own PR's head). How a lower gate's R0 is read at
> a stack-top boot is the seat's call.

## Set-up

- **`/gamerule spawn_mobs false`** and **`/time set 18000`** (`GATE-level.md`'s set-up says why).
- **`/rpg playerxp set @s 1 levels`** (level 1, so no level damage). Record the stats head's `Lifetime XP` first if
  an earlier gate's restore has run; **this gate's last step restores it**.
- **No accessories and no armour.**
- **`/rpg mobtrace`** ON (the reply names PLAYERHIT).
- **`/rpg give blaze_kings_staff`**; with it in hand, **`/rpg gearscore set 100`** (score 100 scales nothing).
- **`/rpg mana refill` before each firing row.**
- **The test zombie is #168's** (`GATE-melee-cell.md`, *THE TEST ZOMBIE*): the four commands, `NoAI:1b`, a fresh one per
  row. **A line with `crit=true` is not comparable: re-fire.**
- **Do the vanilla-control rows (LB5c, LB7) in a cleared test area: they place real fire.**

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
| LB2 | AUTO | a synthetic right-click at the zombie at `~8`; the PLAYERHIT line and the absence of the three warnings | — |
| LB5 | AUTO-PARTIAL | (a) and (b) only: **the wall is built by `/fill`** (the corridor and wall are saved first and restored at the run's end); `BLOCKS FIRE` counts before and after three shots, then the TNT block's count and the primed-TNT entity count after one more. (c) is not driven | each fireball vanishes at the wall |

**The R0 moved again** (the seat's ruling c): the stack's one R0 is now `GATE-selftest.md`'s, on #175's head. The
SUPERSEDED note under this gate's R0 heading names `GATE-withered-shortbow.md`; read it as naming `GATE-selftest.md`.

---

## R0

> **INSTRUMENT NOTE, 2026-10-05, the seat's ruling 1, added before the stack-top boot. No prediction moves.** At the
> stack-top boot, this gate's R0 rows are SUPERSEDED by `GATE-withered-shortbow.md`'s R0 on `<sha>`; recorded
> SUPERSEDED, never PASS or FAIL. Intermediate facts in R0b/R0c are not read. *`<sha>` is the stack top's head as that
> R0a reads it in the boot: a commit beneath the top cannot name the top's hash, because the top is rebased onto this
> note.* This answers the report under *THE ORDER IN THE ONE BOOT* above.

> **2026-11-14, before the next boot, when the stack was un-parked (rebased onto master `af7a1bd5`). No prediction
> moves; nothing above is removed.** At the next stack-top boot this gate's R0 rows are **SUPERSEDED by
> `GATE-deft-hands.md`'s R0** (#180, the stack top), recorded SUPERSEDED, never PASS or FAIL. The note above names
> `GATE-withered-shortbow.md`, and the *SELFTEST WITNESS AMENDMENT* above names `GATE-selftest.md`: each describes
> the stack as it stood when it was written.

### R0a — the build line names this branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, LEGACY-B's head, shortened. Not `-dirty`, not `unknown`, not #171's `ab23ce6f` or any head beneath it | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **38cc9903, 2026-10-14: SUPERSEDED** -- GATE-selftest.md R0, read PASS at 38cc9903 (L42 `[Rpg] Build: 38cc9903`). Not read here. **61e722f2, 2026-11-10: SUPERSEDED** by `GATE-deft-hands.md`'s R0 (PASS). Never PASS or FAIL here. |

**e82df2b3, 2026-11-14: SUPERSEDED** by `GATE-deft-hands.md`'s R0 (AS PREDICTED: L42 `[Rpg] Build: e82df2b3`).

### R0b — the jar carries the fireball staff, the two visuals and the drive

| prediction | instrument | READING |
|---|---|---|
| Entries PRESENT: `content/weapons/blaze_kings_staff.yml`, `content/visuals/blaze_fireball_cast.yml`, `content/visuals/blaze_fireball_impact.yml`, paper `adapter/FireballDrive.class`, #171's `content/weapons/short_bow.yml`. ABSENT: the control. **In the staff's text, as YAML keys (anchored, so a comment cannot match):** PRESENT `body: fireball`, `gravity: 0`, `max_lifetime_ticks: 160`, `right_click:`; ABSENT `item: fire_charge`, `left_click:`. **Control:** `short_bow.yml` reads `body: arrow` PRESENT | the scan below | **38cc9903, 2026-10-14: SUPERSEDED** -- GATE-selftest.md R0b, read PASS at 38cc9903. The intermediate facts are not read. **61e722f2, 2026-11-10: SUPERSEDED** by `GATE-deft-hands.md`'s R0 (PASS). Never PASS or FAIL here. |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'content/weapons/blaze_kings_staff.yml', 'content/visuals/blaze_fireball_cast.yml',
               'content/visuals/blaze_fireball_impact.yml',
               'io/github/butterflysmp/rpg/paper/adapter/FireballDrive.class',
               'content/weapons/short_bow.yml',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
function Text($p) { $r = New-Object IO.StreamReader($zip.GetEntry($p).Open()); $t = $r.ReadToEnd(); $r.Close(); $t }
$staff = Text 'content/weapons/blaze_kings_staff.yml'
foreach ($k in 'body: fireball', 'gravity: 0', 'max_lifetime_ticks: 160', 'right_click:', 'item: fire_charge', 'left_click:') {
  if ($staff -match ('(?m)^\s+' + [regex]::Escape($k) + '\s*$')) { "PRESENT $k" } else { "ABSENT  $k" }
}
if ((Text 'content/weapons/short_bow.yml') -match '(?m)^\s+body: arrow\s*$') { 'PRESENT short_bow body: arrow' } else { 'ABSENT  short_bow body: arrow' }
$zip.Dispose()
```

**e82df2b3, 2026-11-14: SUPERSEDED** by `GATE-deft-hands.md`'s R0b (AS PREDICTED, both scans).

### R0c — the staff and two visuals, and nothing else moved

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, **29 visuals**, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, **15 weapons**, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. Against LEGACY-A's R0c prediction: **weapons 14 → 15, visuals 27 → 29**, every other count unchanged. No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair, and none naming `blaze_kings_staff` or `blaze_fireball` | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | **38cc9903, 2026-10-14: SUPERSEDED** -- GATE-selftest.md R0c, read PASS at 38cc9903 (L44). The intermediate facts are not read. **61e722f2, 2026-11-10: SUPERSEDED** by `GATE-deft-hands.md`'s R0 (PASS). Never PASS or FAIL here. |

**e82df2b3, 2026-11-14: SUPERSEDED** by `GATE-deft-hands.md`'s R0c (AS PREDICTED: L43, the re-predicted line).

---

## THE FIREBALL

### LB1 — the tooltip reads Right-Click, it does not stack, and left-click does nothing (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Hover (score 100). In order: **`Fire`**, blank, **`Fireball  Right-Click`**, `Throws a burning charge.`, `Fire Damage: 30`, `Cooldown: 0.4s \| Mana Cost: 8`, blank, `The Blaze King kept it lit.`, `It has not gone out since.` (italic), blank, **`Legendary Magic Weapon`** — the golden rendering. **A second `/rpg give blaze_kings_staff` lands in its own slot.** **Left-click at the air: no cast, no mana spent, no cooldown shown.** Whatever a left-click with `flint_staff` does today, it does here (*not surveyed*: record what is seen) | **38cc9903, 2026-10-14: NOT READ.** No second `/rpg give blaze_kings_staff` (the setup gave it once, L1742) and no left-click observation. **61e722f2, 2026-11-10: NOT READ** (not staged). |

**e82df2b3, 2026-11-14: NOT READ** (optional hand row; not run).

### LB2 — PLAYERHIT: 30 fire on a right-click, and no vanilla damage (SURVIVAL) — witness: PLAYERHIT line

| prediction | READING |
|---|---|
| Zombie offset `~8 ~0`, aim at it (`/tp @s ~ ~ ~ facing entity @e[tag=t,limit=1] eyes`), **right-click once**: **`PLAYERHIT … source=blaze_kings_staff/right_click … element=fire sent=30.000 crit=false triggerScore=100`**. **Exactly one** PLAYERHIT for the shot. **No `[plume] A MARKER BODY dealt damage` warning, no `[fireball] A MARKER BODY set` warning and no `[fireball] A MARKER BODY tried to place fire` warning** in the log. The zombie burns afterwards (Scorch, `fire.yml`) | **38cc9903, 2026-10-14: VOID** (the seat's ruling) -- the aim defect: `tp @s ~ ~ ~ facing entity @e[tag=t,limit=1] eyes` (L1763) turns from the FEET, so the eye ray passed 1.62 high over the zombie at 8 blocks. L1766 `DRIVE use ... item=blaze_kings_staff yaw=-90.000 pitch=-12.271` (eye-to-eye is -0.859); 0 PLAYERHIT in L1749-L1770; L1769 `PROBE ... 3a45dfe0 ... customHP=100.000/100.000 fireTicks=0 ... d=8.000`. No `MARKER BODY` warning anywhere in the log -- an absence that certifies nothing, since nothing was hit. Carries to the next boot after #175's AIM fix. See the AIM NOTE below. **61e722f2, 2026-11-10 (run 3): FAIL as staged, by a defect found in the reading.** L3547 `DRIVE aim ... eye->eye ... d=8.000 pitch=-0.859` (the aim SB2 hit with); L3548 `DRIVE use ... item=blaze_kings_staff`; 0 PLAYERHIT in the bracket; L3555 PROBE 100.000/100.000 fireTicks=0. **The cause (seat-accepted, 2026-11-10):** this row's own `/kill @e[tag=t]` (L3540) put SB2's zombie `98011576` into its death animation at the same spot (d=8), and it was removed at L3551, after the shot. `PaperCombatWorld.castRay` accepts any `LivingEntity` but the caster, dying ones included, so the flight ended on the corpse and dealt nothing. 0 `MARKER BODY` warnings in the log, which certifies nothing, since nothing live was hit. Fix owed before the next boot. |

**LB2 AMENDED, 2026-11-10, before the next boot (the seat's instruction: every single-hit prediction allows a crit,
as Sunder's rows already do).** The PLAYERHIT reads **either `sent=30.000 crit=false` or `sent=60.000 crit=true`**:
the base 30 fire, or 30 x 2.0, the crit multiplier WD1 verified at the `61e722f2` boot (`sent=112.000 crit=true`
against `56.000 crit=false`, GATE-selftest.md ST5). Every other clause of LB2's prediction stands. A `crit=true`
line at any other `sent` is a FAIL.

> **AIM NOTE, 2026-10-14 (instrument only; no prediction moves; written after the `38cc9903` boot, where this row
> was VOID).** **`/tp @s ~ ~ ~ facing entity @e[tag=t,limit=1] eyes` does not aim the player's eyes at the zombie's
> eyes.** `eyes` is the TARGET's anchor only; the player's own anchor is the command source's, and a player's source
> is built with `Anchor.FEET` (javap, Paper 26.1.2 build 74: `CommandSourceStack`'s 9-argument constructor;
> `LookAt$LookAtEntity.perform` passes `source.getAnchor()`; `Entity.lookAt` computes from it). So the turn is FEET to
> eyes, and the look ray -- which starts at the eyes -- runs parallel to the intended one, 1.62 blocks higher: over
> a zombie at 8 blocks. The boot logged `pitch=-12.271`; eye to eye is -0.859 (computed). **Wherever this file says
> to aim with that command** (this row's staging), **aim with** `/execute anchored eyes facing entity @e[tag=t,limit=1] eyes run tp @s ~ ~ ~ ~ ~`
> (`execute anchored eyes` sets the anchor `CommandSourceStack.facing` reads -- verified from the jar, not yet in a
> boot). The selftest's own scenario now aims with #175's `AIM` verb, eye to eye in Java.

**e82df2b3, 2026-11-14 (run 3): AS PREDICTED, as amended (the crit).** L2819 `DRIVE aim teleportAsync eye->eye target=4385ee2d-… zombie d=8.000 yaw=-90.000 pitch=-0.859`; L2820 `DRIVE use … item=blaze_kings_staff`; L2821 `PLAYERHIT caster=b6ae27e9-… source=blaze_kings_staff/right_click target=4385ee2d-… zombie element=fire sent=60.000 crit=true triggerScore=100`, which is 30 × 2.0, the crit the 2026-11-10 amendment allows. Exactly one PLAYERHIT in the bracket (L2804-L2824), and the only `blaze_kings_staff` PLAYERHIT in the log. **0 `MARKER BODY` lines in the whole log** (`grep -c 'MARKER BODY'`). The zombie burns: L2823 `PROBE … customHP=35.000/100.000 fireTicks=86`. *(Runs 1 and 2 ran on Peaceful and SKIPPED this row, `no entity tagged 't'`; see `GATE-deft-hands.md`'s boot account. Neither is a reading.)*

### LB3 — it is a fireball, not an item (SURVIVAL) — witness: Ben, with F3+B

| prediction | READING |
|---|---|
| Fire into open air with F3+B on. The body is **the blaze's fireball**: a small fire-charge sprite that faces the camera, trailing **smoke**, with a **~0.31-block hitbox cube**. It is **not** a dropped item (no spin, no bob, no item hitbox). It **appears a few blocks out**, not at the staff (vanilla's render gate, `PLAN-legacy-b.md` §5.6; *inferred* for the client). **Positive control, same session:** `/rpg give flint_staff`; its thrown flint looks different in each of these respects | **38cc9903, 2026-10-14: NOT READ.** No `flint_staff` control given; no observation. **61e722f2, 2026-11-10: NOT READ** (not staged). |

**e82df2b3, 2026-11-14: NOT READ** (optional hand row; not run).

### LB4 — a straight line: no drop (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Stand on a long flat surface. `/tp @s ~ ~ ~ -90 0` (east, level). Fire once. **The fireball stays at eye height for as far as it can be seen**, with no visible drop. **No range is predicted** (the seat's Q-B4 reading: no block range is authored). **Positive control:** `flint_staff` from the same spot (gravity 0.05) **visibly drops**, so this staging CAN show a drop | **38cc9903, 2026-10-14: NOT READ.** No `flint_staff` control; no observation. **61e722f2, 2026-11-10: NOT READ** (not staged). |

**e82df2b3, 2026-11-14: NOT READ** (optional hand row; not run).

### LB4b — it despawns at 8 seconds, not at 2 and not never (SURVIVAL) — witness: the server's own entity test

| prediction | READING |
|---|---|
| **Fired straight UP, so the body stays in Ben's chunk column**, where it is always tracked and simulated (fired level, it would leave simulation distance long before 8 s; `AbstractHurtingProjectile` is always activation-range ACTIVE, read from `ActivationRange.initializeEntityActivationState`). **Stand below y 70** (record the y) so 240 blocks of climb from the eye stays under the build limit of 319. **Control first:** `/execute if entity @e[type=small_fireball]` reads **`Test failed`**. Then `/tp @s ~ ~ ~ ~ -90`, fire once, and run the same command **at about 4 s: `Test passed, count: 1`**, and **at about 10 s: `Test failed`**. The lifetime is 160 ticks; the flight's fuse removes the body then (Q-B5). **A count still 1 at 10 s is the removal NOT happening**, and the 2 s of LEGACY-A's old lifetime would read failed at 4 s | **38cc9903, 2026-10-14: NOT READ.** No `/execute if entity @e[type=small_fireball]` command or reply in the log. **61e722f2, 2026-11-10: NOT READ** (not staged). |

**e82df2b3, 2026-11-14: NOT READ** (optional hand row; not run).

### LB5 — no block ignition, no TNT (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **(a)** Face a wall of oak planks 10 blocks east, with air in front of it. Fire three times: **no fire block appears**, and each fireball **vanishes at the wall**. **(b)** Replace one plank with TNT and fire at it: **it does not prime.** **(c) Positive control, vanilla, NOT OURS:** `/summon small_fireball ~ ~1.6 ~2 {Motion:[1.0d,0.0d,0.0d]}` aimed at the planks. Vanilla's default is incendiary, so **it places fire**. *UNVERIFIED*: the `Motion` key in 26.1; if the summon does not move, record it and do not read (c) as a pass. **(a)'s no-fire is the SOLE witness of `setIsIncendiary(false)`; (b) is the SOLE witness of the block-hit cancel in `onPlumeBodyHit`.** No unit test can raise either | **38cc9903, 2026-10-14: PARTIAL -- every server count matches, but no server line shows a fireball reaching the wall, and the positive control (c) was not driven.** AUTO (selftest, run 2), staging confirmed: the corridor and the top row were already air (L1783, L1786 `"No blocks were filled" [red]` -- a no-change fill, not a failure), the wall 20/20 planks (L1784 `"Successfully filled 20 block(s)"`), the TNT at -94, 64, 5 (L1805), aim level (`pitch=0.000`, L1790, L1792, L1795, L1807). (a) L1787 `BLOCKS FIRE count=0` before and L1801 `count=0` after three shots. (b) L1811 `BLOCKS TNT count=1` (the block remains), L1812 `ENTITIES TNT count=0 radius=16` (40 t after the shot, inside an 80 t fuse), L1813 `FIRE count=0`. **These counts cannot tell a guard that held from a fireball that never arrived** (a hollow fixture in OBSERVABILITY): the selftest logs no cast or impact line. (c) not driven. Ben's half (each fireball vanishes at the wall): NOT READ -- not on his hand-step list. **61e722f2, 2026-11-10 (run 3): VACUOUS as written.** Counts: L3574 `BLOCKS FIRE count=0` before; L3603 `count=0` after (a); L3626 `BLOCKS TNT count=1`, L3627 `ENTITIES TNT count=0`, L3628 `FIRE count=0`. Impact witness: (a) L3580-L3591, the last `n=1` at **dx 6.00**; (b) L3613-L3624, the last `n=1` at **dx 7.50**. Both are at or below 7.5, so as written neither body reached the wall and both counts are VACUOUS. (c) not driven. Ben's half not read. **Note (seat-accepted, 2026-11-10): the visible body runs one step behind the flight.** `ProjectileFlight.launch` spawns the body and runs the first `step` in the same tick; `step` moves the logical position on and only hands the body its velocity (`driveMarker`), which vanilla applies on the body's next tick. The first sample, 0.00, is that lag. So (a)'s flight stood at 7.5, and its next step crossed LB2's dying zombie (d=8, front face about 7.7; killed by this row's `/kill`, L3559; removed L3593, after the shot at L3577). (b)'s flight stood at 9.0, and its next step crossed the wall face at 9.5: **(b) did reach the wall.** The band is re-derived for the next boot. |

**LB5 IMPACT WITNESS, added 2026-11-06, before the next boot.** AUTO, from `/rpg selftest legacy-b`. After the first (a) shot, and again after the (b) shot, there are twelve lines, one per tick, each **`POSITIONS SMALL_FIREBALL n=<k> at=[<dx> <dy> <dz>; …] radius=16`**. Positions are relative to Ben's feet: he stands at a block centre facing east (yaw -90, pitch 0), and the wall's west face is at **dx = 9.5** (the wall is the block column `~10`, spanning dx 9.5 to 10.5).

- **Predicted:** a run of `n=1` samples whose `dx` rises each tick, with `dz` near 0. Then `n=0` for every remaining sample. **The last `n=1` sample has 7.5 < dx ≤ 9.5.** The upper bound is the face. The lower bound is the authored `speed 1.5` per tick (`blaze_kings_staff.yml`) plus half a block of margin: a body last seen further back ended in the air.
- **How it is read against the counts:** `n=0` on all twelve means the body never spawned or was never in range, and (a)'s or (b)'s no-fire/no-prime count is then VACUOUS, not PASS. A last `dx` at or below 7.5 is the same: it never reached the wall. **Any `dx` above 9.6 means the body passed the face**, a finding in its own right.
- **Not predicted:** `dy` (the body's spawn height is not traced here), and how many `n=1` samples there are, since the drive's per-tick step is not re-derived here.

**LB5's BAND RE-DERIVED, 2026-11-10, before the next boot (the seat's instruction; run 3's reading stays VACUOUS as
written, with its note).** The band above assumed the sampled body IS the flight. It is one step behind:
`ProjectileFlight.launch` spawns the body and runs the first `step` in the same tick, and `step` moves the logical
position on but only hands the body its velocity (`driveMarker`), which vanilla applies on the body's next tick.
Run 3's first sample, `dx 0.00`, is that lag. **The derivation:**
- the logical flight stands at `1.5 k` (`speed 1.5`, `gravity 0`, aim level);
- the wall face is at `dx 9.5`, so the step from `9.0` to `10.5` is the first to cross it, and the flight ends there;
- the body shows the previous position, so a flight that reaches the wall is **last sighted at `dx 7.50` (±0.01)**,
  then `n=0`.

**The new band:**
- **last `n=1` at `dx 7.50`:** the flight reached the wall, and the counts are READ;
- **last `n=1` at `dx 6.00` or below:** it ended short (as (a)'s did, on a dying mob), and the counts are VACUOUS;
- **any sample at `dx 9.00` or beyond:** the flight crossed the face without ending, which is a finding in its own
  right.

Run 3 fits it exactly: (b) last at 7.50 (L3618, then `n=0` from L3619), (a) last at 6.00 (L3584, then `n=0`
from L3585). **Valid only while the step is 1.5 and
the spawn samples at 0.00;** a changed speed or spawn point re-derives it.

**e82df2b3, 2026-11-14 (run 3): (a) and (b) AS PREDICTED; the counts are READ.** (a) L2851-L2862: `n=1` at `dx` 0.00, 1.50, 3.00, 4.50, 6.00, **7.50**, then `n=0` six times. The last sighting is 7.50, so by the re-derived band the flight reached the wall. L2845 `BLOCKS FIRE count=0` before; L2865 `count=0` after three shots. (b) L2874-L2885: the same run, last `n=1` at **7.50**, then `n=0`; L2886 `BLOCKS TNT count=1`, L2887 `ENTITIES TNT count=0`, L2888 `BLOCKS FIRE count=0`. No sample at `dx 9.00` or beyond. (c) not driven. **Ben's half (each fireball vanishes at the wall): "Yes"** (answer 6). It is not bound to one run: LB5 ran DONE in all three runs (L643-L705, L1885-L1947, L2825-L2889).

### LB6 — no explosion, at a block or at a mob (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Fire at the plank wall and at a test zombie. **No explosion sound, no explosion particles, no broken blocks, no crater, no push.** This row guards the entity TYPE: a `LargeFireball` spawned by mistake explodes in `onHit`, which a cancelled block hit still reaches | **38cc9903, 2026-10-14: NOT READ.** The zombie half was never hit (LB2 VOID); nothing itemised about explosions. **61e722f2, 2026-11-10: NOT READ** (not staged). |

**e82df2b3, 2026-11-14: NOT READ** (optional hand row; not run).

### LB7 — a small fireball cannot be punched back (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **This row witnesses the entity TYPE, not our driven body**, which moves 1.5 blocks a tick and cannot be staged for a punch. `/summon small_fireball ~ ~1.6 ~2 {Motion:[0.0d,0.0d,0.0d],acceleration_power:0.0d}`. Left-click it: **the crosshair does not select it and nothing happens.** **Positive control:** `/summon fireball ~ ~1.6 ~2 {Motion:[0.0d,0.0d,0.0d],acceleration_power:0.0d}` (the ghast's). Left-click it: **it is deflected** along the look direction. *UNVERIFIED*: the summon keys; if either summon does not hold still, record it and do not read the row | **38cc9903, 2026-10-14: NOT READ.** No `/summon small_fireball` or `/summon fireball` in the log. **61e722f2, 2026-11-10: NOT READ** (not staged). |

**e82df2b3, 2026-11-14: NOT READ** (optional hand row; not run).

### LB8 — the shooter is never hit (SURVIVAL) — witness: Ben + the log

| prediction | READING |
|---|---|
| Record health. `/tp @s ~ ~ ~ ~ 90` (straight down) and fire three times, then `/tp @s ~ ~ ~ ~ 60` and fire three more. **Health unchanged. Ben does not catch fire. No PLAYERHIT naming Ben. No backstop warning.** The ground under him does not ignite. Core's `castRay` excludes the caster, and the unowned body's own hit on him is cancelled | **38cc9903, 2026-10-14: NOT READ.** The straight-down and 60-degree shots were never staged. **61e722f2, 2026-11-10: NOT READ** (not staged). |

**e82df2b3, 2026-11-14: NOT READ** (optional hand row; not run).

### LB9 — the body arrives with the hit: the compensated drive (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Zombie offset `~30 ~0`, `NoAI`, aimed. Fire once. **The fireball reaches the zombie at the moment the damage lands**: no visible gap before it, no body carrying on past it (Q-B3 COMPENSATE predicts **no lead**). **The honest limit:** uncompensated, the lead would be `0.02 × ticks` blocks (awk), 0.4 blocks at this range, which may be invisible; so a PASS here does not distinguish compensated from uncompensated. **`FireballDriveTest` is the unit witness of the arithmetic; this row is the only witness that the jar's push and drag are still `0.1` and `0.95f`** | **38cc9903, 2026-10-14: NOT READ.** No zombie at `~30`. Still the sole witness of the jar constants. **61e722f2, 2026-11-10: NOT READ** (not staged). |

**e82df2b3, 2026-11-14: NOT READ** (optional hand row; not run).

### LB10 — the old sounds and effects (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **PROVISIONAL (Ben, Q-B6: "use the old sounds and effects").** On each right-click that casts: **a blaze shoot** (pitch a little high), at the press, not at the impact. At the zombie (LB2's staging) and at the plank wall (LB5a): **a burst of flame with a few lava drips and a fire-charge crack** where it lands. **A refused press is silent** (hold right-click until the mana runs out: 8 a shot, and `/rpg mana` has only `refill`) (`on_cast` fires only on a cast) | **38cc9903, 2026-10-14: NOT READ.** Nothing itemised; the refused-press half was never staged. **61e722f2, 2026-11-10: NOT READ** (not staged). |

**e82df2b3, 2026-11-14: NOT READ** (optional hand row; not run).

### LB11 — the fuse impact in mid-air (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **Not cfde822's behaviour, recorded rather than ruled:** the flight lands its 160-tick fuse as an impact with no target, so `blaze_fireball_impact` plays where the fireball runs out, in mid-air (`flint_impact.yml` has the same property). Fired straight up as in LB4b, the burst is 240 blocks up and **will not be seen or heard**; this row is therefore recorded **UNWITNESSED** unless Ben finds a staging that shows it, and it is listed so the property is not mistaken for absent | **38cc9903, 2026-10-14: NOT READ** (UNWITNESSED, as the row provides). **61e722f2, 2026-11-10: NOT READ** (not staged). |

**e82df2b3, 2026-11-14: NOT READ** (optional hand row; not run).

---

## LAST STEP — PUT BEN'S XP BACK

`/rpg playerxp set @s <the number recorded> xp`.

---

## WHAT THIS GATE CANNOT SEE

- **The body vanishing on a grazed block corner** while the flight flies on (`PLAN-legacy-b.md` §5.6). No cheap staging.
- **The orphan** (Q-B5, accepted; `NEXT.md`, *LEGACY-B — THE FIREBALL BODY'S ORPHAN*). A healthy build never produces one.
- **Folia ordering** of the drive against the entity tick (as for the arrow).
- **The two backstops** (`onMarkerBodyCombust`, `onMarkerBodyIgnite`) should never fire; their silence in LB2 and LB5 is
  the reading, and it cannot distinguish "the guard upstream held" from "the backstop is not registered".
- **`driveMarker`'s `instanceof SmallFireball` dispatch has NO witness that can tell it is there.** `FireballDriveTest`
  proves the arithmetic, not that `driveMarker` calls it; LB9's limit (0.4 blocks at 30) means a dropped dispatch may
  read PASS. Stated so a PASS on LB9 is not read as covering it.

## MUTATIONS (run 2026-10-01 against `15495a78`, before this file was committed)

Each: the needle unique before the edit, marker present 1 / original gone 0, line delta 2 against a pristine copy,
a focused run, restored by `cp` and proved byte-identical with `cmp`.

| mutation | edit | reddened |
|---|---|---|
| MUT-BODYCONST | `ProjectileFlight.launch` passes `"arrow"` for the body | `theBodyIdReachesThePortUnchangedForAFireballAndForAnArrow` only |
| MUT-FUSEKEEP | the fuse branch's `resolve` removed (hand edit, anchored on its comment; `resolve` calls 2 → 1) | the zero-gravity fireball row, `theBodyIsRemovedWhenTheFuseExpires`, `anArrowBodiedProjectileStillDraws…` |
| MUT-DRIFT | `nextVelocity` subtracts `gravity + 0.001` | the zero-gravity fireball row + 4 older flight rows |
| MUT-NOPUSH | `FireballDrive` drops `- ACCELERATION_POWER` | both `FireballDriveTest` drive rows |
| MUT-INERTIA | `INERTIA = 0.95` (not the jar's `0.95f`) | both `FireballDriveTest` drive rows |
| MUT-IDENTITY | `velocityFor` returns its argument | both `FireballDriveTest` drive rows |
| MUT-NOFIREBALL | `BODIES` without `fireball` | `aFireballBodyParsesItsTypoIsRefusedAndTheArrowStillParses` only |
| MUT-NOARROW | `BODIES` without `arrow` | that row + the two older arrow-body loader rows |

**Sole witnesses, by construction (no unit test reaches a spawned entity or a raised event):** LB5a for
`setIsIncendiary(false)`, LB5b for the fireball's block-hit cancel in `onPlumeBodyHit`, LB9 for the jar constants,
LB10 for the visuals being played (`VisualLoaderTest` pins that they LOAD), LB4b for the body actually despawning.
