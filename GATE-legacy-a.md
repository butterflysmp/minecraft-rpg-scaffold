# GATE — LEGACY-A: the Short Bow, ported from cfde822 (content only)

**Status: NOT RUN.**

*Amended 2026-09-29, BEFORE ANY BOOT, at the seat's instruction: Ben removed Smolder* (*"Let's remove the smolder
ability on the blaze kings staff"*). BK3 (Smolder's radius) was deleted, BK1's tooltip lost the Smolder block, and R0c
was re-read against the change (it did not move).

*Amended again 2026-09-29, BEFORE ANY ROW WAS READ, at the seat's instruction: THIS SLICE IS NOW THE SHORT BOW ONLY.*
Ben, having seen the staff in game: *"The Blaze King's Staff is supposed to shoot an actual fire ball not the item, it
also isn't supposed to be effected by gravity. A straight line. Also the ability should be Right Click not left."*
That needs engine work (a real fireball body, no gravity, on right-click), so the staff moves to a later slice,
LEGACY-B. **BK1 and BK2 are deleted, R0b drops the staff's file, and R0c is re-predicted: 14 weapons, not 15.**

**The one boot so far, stated so it is not mistaken for a reading.** The stack was booted once, at `eab57026`
(CC started it; Ben's LOGIN is in its log, 04:45:07). CC read R0 on it (PASS). Ben gave himself both weapons and issued
`/stop` at 04:49:34; **no row here was read on it.** Ben's *"Short bow looks good"* afterwards is NOT itemised, so if it
is recorded at all it goes in as *reported good -- not itemised*, never PASS. That boot's R0 does not carry to the next.

Every prediction below was written **before** any row of this file was read, and **this file is COMMITTED before the
boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once its row has been
read. **NO BOOT until the seat has diffed this file.** This is the stack top: #168 → #169 → #170 → this. It is
booted ONCE, and every gate beneath it is read in the same boot.

**Readings count only if Ben's LOGIN appears in THIS boot's log.** A row whose prediction is a LOG LINE is PASS when
that line appears verbatim after the LOGIN.

```
ROWS      7   R0a R0b R0c
              SB1 SB2 SB3 SB4
         ──
          7   = 3 R0 + 4 SB     git grep -c '^### R0\|^### SB' <ref> -- GATE-legacy-a.md
```

**Plan:** `PLAN-legacy-port.md` on `docs/batch-surveys`, section 2.3 and RULINGS: Ben's 2026-09-29 answers (old
numbers are the provisional starting values; acquisition is `/rpg give` only), the seat's L5, and Ben's staff
feedback that moved the staff to LEGACY-B.

## GAME MODE

**SURVIVAL on every row.** The Short Bow's magazine is a cost (SB4), and creative removes costs (the
creative-divergence register).

## THE ORDER IN THE ONE BOOT

#168's rows, then `GATE-level.md`, then `GATE-nexus-polish.md`, then **these**. **This slice adds one content file
and touches no code**, so no earlier row is restated: nothing an earlier row reads is in it.

## Set-up

- **`/gamerule spawn_mobs false`** and **`/time set 18000`** (26.1's rule name; `GATE-level.md`'s set-up says why).
- **`/rpg playerxp set @s 1 levels`**, so no level damage is added (a weapon hit carries +1 per level-up,
  `GATE-level.md`). The earlier gates' last steps restore Ben's XP; **this gate's last step restores it again**, so
  record the stats head's `Lifetime XP` before this line if the earlier restore has run.
- **No accessories and no armour**, so no class damage or crit gear is in play.
- **`/rpg mobtrace`** ON (the reply names PLAYERHIT).
- **`/rpg give short_bow`**; with it in hand, **`/rpg gearscore set 100`**, so every figure below is the authored one
  (score 100 scales nothing).
- **The test zombie is #168's** (its section *THE TEST ZOMBIE*): the four commands, `NoAI:1b`, a fresh one per row.
  **A line with `crit=true` is not comparable: re-fire.**

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
| SB2 | AUTO | a synthetic right-click (`PlayerInteractEvent` into `onRightClick`) at the zombie at `~8`, aimed with the gate's `tp … facing` | — |

**The R0 moved again** (the seat's ruling c): the stack's one R0 is now `GATE-selftest.md`'s, on #175's head. The
SUPERSEDED note under this gate's R0 heading names `GATE-withered-shortbow.md`; read it as naming `GATE-selftest.md`.

---

## R0

> **INSTRUMENT NOTE, 2026-10-05, the seat's ruling 1, added before the stack-top boot. No prediction moves.** At the
> stack-top boot, this gate's R0 rows are SUPERSEDED by `GATE-withered-shortbow.md`'s R0 on `<sha>`; recorded
> SUPERSEDED, never PASS or FAIL. Intermediate facts in R0b/R0c are not read. *`<sha>` is the stack top's head as that
> R0a reads it in the boot: a commit beneath the top cannot name the top's hash, because the top is rebased onto this
> note.*

### R0a — the build line names this branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head, shortened. Not `-dirty`, not `unknown`, not #170's, #169's or #168's head | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries the bow and the three slices beneath, and not the staff

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/weapons/short_bow.yml`, #170's `menu/NexusScreens`, #169's `menu/LevelMenu`, #168's `combat/TracedHit`. ABSENT: the control, **and `content/weapons/blaze_kings_staff.yml`** (removed; a stale `run/` content folder could still hold a copy, which is why the boot uses `--refresh-content`) | `GATE-level.md` R0b's scan, with these six entries | |

### R0c — one weapon more, and nothing else moved

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, **14 weapons**, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. Against #170's prediction: **weapons 13 → 14**, every other count unchanged (the bow adds no visual). **Re-predicted 2026-09-29 when the staff left** (it was 15). No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | |

---

## THE SHORT BOW (cfde822 `ShortBowWeapon`; PLAN-legacy-port.md 2.3)

### SB1 — the tooltip, and it does not stack (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Hover the bow (score set to 100). The lines read, in order: **`Kinetic`**, **`Quiver: --/8`**, blank, **`Ranged Damage: 16`**, **`Attack Speed: 1.7`**, blank, `Short to draw.`, `Shorter to answer.` (italic), blank, **`Common Ranged Weapon`** — the golden rendering. `/rpg give short_bow` a second time: **it lands in a separate slot**, never stacked (the standing decision: no custom stack above 1) | |

### SB2 — a shot lands for 16, kinetic (SURVIVAL) — witness: PLAYERHIT line

| prediction | READING |
|---|---|
| Zombie offset `~8 ~ ~0`, aim at it (`/tp @s ~ ~ ~ facing entity @e[tag=t,limit=1] eyes`), right-click once: an **arrow** flies (a visible arrow body), and **`PLAYERHIT … source=short_bow/right_click … element=kinetic sent=16.000 crit=false triggerScore=100`** | |

### SB3 — it pushes: the ruling's knockback, against the old bow (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Summon a zombie at `~8 ~ ~0` **without** `NoAI` (`/summon zombie ~8 ~ ~0 {PersistenceRequired:1b,IsBaby:0b,Tags:["t"]}`), and shoot it as it walks in. **It is pushed back along the arrow's line** (the ruled knockback, `strength: 0.1`). **Whether a `NoAI` mob takes knockback at all is UNVERIFIED**, which is why this row does not use one. Record the push as seen; the distance is not predicted | |

### SB4 — eight arrows, then the reload (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Hold right-click with a fresh bow: the quiver readout counts **8 → 0**, one shot per **12 ticks** at most (0.6 s; a held right-click cannot fire faster than its cooldown on the 4-tick grid). At 0 it reloads for **60 ticks** (3 s), then reads **8** again | |

---

## LAST STEP — PUT BEN'S XP BACK

`/rpg playerxp set @s <the number recorded> xp`.

---

## NO MUTATIONS

This slice is one content file and the test edits content moves. **Net against #170, `KNOWN_FIRE_DAMAGE_SITES` does
not move** (21: it went to 23 with the staff, 22 when Smolder left, and back to 21 when the staff left; each step read
from the test's own discovery). `KNOWN_RANGER_WEAPONS` 6 → 7, the test's own list. `golden-lore.txt` 92 → 93
renderings: the short bow's block and the footer. There is no new code for a mutation to reach.
