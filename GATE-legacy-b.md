# GATE — LEGACY-B: the Blaze King's Staff shoots a real fireball

**Status: NOT RUN.**

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
         15   = 3 R0 + 12 LB     git grep -c '^### R0\|^### LB' <ref> -- GATE-legacy-b.md
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

## R0

> **INSTRUMENT NOTE, 2026-10-05, the seat's ruling 1, added before the stack-top boot. No prediction moves.** At the
> stack-top boot, this gate's R0 rows are SUPERSEDED by `GATE-withered-shortbow.md`'s R0 on `<sha>`; recorded
> SUPERSEDED, never PASS or FAIL. Intermediate facts in R0b/R0c are not read. *`<sha>` is the stack top's head as that
> R0a reads it in the boot: a commit beneath the top cannot name the top's hash, because the top is rebased onto this
> note.* This answers the report under *THE ORDER IN THE ONE BOOT* above.

### R0a — the build line names this branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, LEGACY-B's head, shortened. Not `-dirty`, not `unknown`, not #171's `ab23ce6f` or any head beneath it | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries the fireball staff, the two visuals and the drive

| prediction | instrument | READING |
|---|---|---|
| Entries PRESENT: `content/weapons/blaze_kings_staff.yml`, `content/visuals/blaze_fireball_cast.yml`, `content/visuals/blaze_fireball_impact.yml`, paper `adapter/FireballDrive.class`, #171's `content/weapons/short_bow.yml`. ABSENT: the control. **In the staff's text, as YAML keys (anchored, so a comment cannot match):** PRESENT `body: fireball`, `gravity: 0`, `max_lifetime_ticks: 160`, `right_click:`; ABSENT `item: fire_charge`, `left_click:`. **Control:** `short_bow.yml` reads `body: arrow` PRESENT | the scan below | |

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

### R0c — the staff and two visuals, and nothing else moved

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, **29 visuals**, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, **15 weapons**, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. Against LEGACY-A's R0c prediction: **weapons 14 → 15, visuals 27 → 29**, every other count unchanged. No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair, and none naming `blaze_kings_staff` or `blaze_fireball` | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | |

---

## THE FIREBALL

### LB1 — the tooltip reads Right-Click, it does not stack, and left-click does nothing (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Hover (score 100). In order: **`Fire`**, blank, **`Fireball  Right-Click`**, `Throws a burning charge.`, `Fire Damage: 30`, `Cooldown: 0.4s \| Mana Cost: 8`, blank, `The Blaze King kept it lit.`, `It has not gone out since.` (italic), blank, **`Legendary Magic Weapon`** — the golden rendering. **A second `/rpg give blaze_kings_staff` lands in its own slot.** **Left-click at the air: no cast, no mana spent, no cooldown shown.** Whatever a left-click with `flint_staff` does today, it does here (*not surveyed*: record what is seen) | |

### LB2 — PLAYERHIT: 30 fire on a right-click, and no vanilla damage (SURVIVAL) — witness: PLAYERHIT line

| prediction | READING |
|---|---|
| Zombie offset `~8 ~0`, aim at it (`/tp @s ~ ~ ~ facing entity @e[tag=t,limit=1] eyes`), **right-click once**: **`PLAYERHIT … source=blaze_kings_staff/right_click … element=fire sent=30.000 crit=false triggerScore=100`**. **Exactly one** PLAYERHIT for the shot. **No `[plume] A MARKER BODY dealt damage` warning, no `[fireball] A MARKER BODY set` warning and no `[fireball] A MARKER BODY tried to place fire` warning** in the log. The zombie burns afterwards (Scorch, `fire.yml`) | |

### LB3 — it is a fireball, not an item (SURVIVAL) — witness: Ben, with F3+B

| prediction | READING |
|---|---|
| Fire into open air with F3+B on. The body is **the blaze's fireball**: a small fire-charge sprite that faces the camera, trailing **smoke**, with a **~0.31-block hitbox cube**. It is **not** a dropped item (no spin, no bob, no item hitbox). It **appears a few blocks out**, not at the staff (vanilla's render gate, `PLAN-legacy-b.md` §5.6; *inferred* for the client). **Positive control, same session:** `/rpg give flint_staff`; its thrown flint looks different in each of these respects | |

### LB4 — a straight line: no drop (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Stand on a long flat surface. `/tp @s ~ ~ ~ -90 0` (east, level). Fire once. **The fireball stays at eye height for as far as it can be seen**, with no visible drop. **No range is predicted** (the seat's Q-B4 reading: no block range is authored). **Positive control:** `flint_staff` from the same spot (gravity 0.05) **visibly drops**, so this staging CAN show a drop | |

### LB4b — it despawns at 8 seconds, not at 2 and not never (SURVIVAL) — witness: the server's own entity test

| prediction | READING |
|---|---|
| **Fired straight UP, so the body stays in Ben's chunk column**, where it is always tracked and simulated (fired level, it would leave simulation distance long before 8 s; `AbstractHurtingProjectile` is always activation-range ACTIVE, read from `ActivationRange.initializeEntityActivationState`). **Stand below y 70** (record the y) so 240 blocks of climb from the eye stays under the build limit of 319. **Control first:** `/execute if entity @e[type=small_fireball]` reads **`Test failed`**. Then `/tp @s ~ ~ ~ ~ -90`, fire once, and run the same command **at about 4 s: `Test passed, count: 1`**, and **at about 10 s: `Test failed`**. The lifetime is 160 ticks; the flight's fuse removes the body then (Q-B5). **A count still 1 at 10 s is the removal NOT happening**, and the 2 s of LEGACY-A's old lifetime would read failed at 4 s | |

### LB5 — no block ignition, no TNT (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **(a)** Face a wall of oak planks 10 blocks east, with air in front of it. Fire three times: **no fire block appears**, and each fireball **vanishes at the wall**. **(b)** Replace one plank with TNT and fire at it: **it does not prime.** **(c) Positive control, vanilla, NOT OURS:** `/summon small_fireball ~ ~1.6 ~2 {Motion:[1.0d,0.0d,0.0d]}` aimed at the planks. Vanilla's default is incendiary, so **it places fire**. *UNVERIFIED*: the `Motion` key in 26.1; if the summon does not move, record it and do not read (c) as a pass. **(a)'s no-fire is the SOLE witness of `setIsIncendiary(false)`; (b) is the SOLE witness of the block-hit cancel in `onPlumeBodyHit`.** No unit test can raise either | |

### LB6 — no explosion, at a block or at a mob (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Fire at the plank wall and at a test zombie. **No explosion sound, no explosion particles, no broken blocks, no crater, no push.** This row guards the entity TYPE: a `LargeFireball` spawned by mistake explodes in `onHit`, which a cancelled block hit still reaches | |

### LB7 — a small fireball cannot be punched back (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **This row witnesses the entity TYPE, not our driven body**, which moves 1.5 blocks a tick and cannot be staged for a punch. `/summon small_fireball ~ ~1.6 ~2 {Motion:[0.0d,0.0d,0.0d],acceleration_power:0.0d}`. Left-click it: **the crosshair does not select it and nothing happens.** **Positive control:** `/summon fireball ~ ~1.6 ~2 {Motion:[0.0d,0.0d,0.0d],acceleration_power:0.0d}` (the ghast's). Left-click it: **it is deflected** along the look direction. *UNVERIFIED*: the summon keys; if either summon does not hold still, record it and do not read the row | |

### LB8 — the shooter is never hit (SURVIVAL) — witness: Ben + the log

| prediction | READING |
|---|---|
| Record health. `/tp @s ~ ~ ~ ~ 90` (straight down) and fire three times, then `/tp @s ~ ~ ~ ~ 60` and fire three more. **Health unchanged. Ben does not catch fire. No PLAYERHIT naming Ben. No backstop warning.** The ground under him does not ignite. Core's `castRay` excludes the caster, and the unowned body's own hit on him is cancelled | |

### LB9 — the body arrives with the hit: the compensated drive (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Zombie offset `~30 ~0`, `NoAI`, aimed. Fire once. **The fireball reaches the zombie at the moment the damage lands**: no visible gap before it, no body carrying on past it (Q-B3 COMPENSATE predicts **no lead**). **The honest limit:** uncompensated, the lead would be `0.02 × ticks` blocks (awk), 0.4 blocks at this range, which may be invisible; so a PASS here does not distinguish compensated from uncompensated. **`FireballDriveTest` is the unit witness of the arithmetic; this row is the only witness that the jar's push and drag are still `0.1` and `0.95f`** | |

### LB10 — the old sounds and effects (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **PROVISIONAL (Ben, Q-B6: "use the old sounds and effects").** On each right-click that casts: **a blaze shoot** (pitch a little high), at the press, not at the impact. At the zombie (LB2's staging) and at the plank wall (LB5a): **a burst of flame with a few lava drips and a fire-charge crack** where it lands. **A refused press is silent** (hold right-click until the mana runs out: 8 a shot, and `/rpg mana` has only `refill`) (`on_cast` fires only on a cast) | |

### LB11 — the fuse impact in mid-air (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **Not cfde822's behaviour, recorded rather than ruled:** the flight lands its 160-tick fuse as an impact with no target, so `blaze_fireball_impact` plays where the fireball runs out, in mid-air (`flint_impact.yml` has the same property). Fired straight up as in LB4b, the burst is 240 blocks up and **will not be seen or heard**; this row is therefore recorded **UNWITNESSED** unless Ben finds a staging that shows it, and it is listed so the property is not mistaken for absent | |

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
