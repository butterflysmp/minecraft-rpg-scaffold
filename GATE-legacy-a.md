# GATE — LEGACY-A: the Short Bow and the Blaze King's Staff, ported from cfde822 (content only)

**Status: NOT RUN.**

Every prediction below was written **before** any boot of this branch, and **this file is COMMITTED before the
boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once its row has been
read. **NO BOOT until the seat has diffed this file.** This is the stack top: #168 → #169 → #170 → this. It is
booted ONCE, and every gate beneath it is read in the same boot.

**Readings count only if Ben's LOGIN appears in THIS boot's log.** A row whose prediction is a LOG LINE is PASS when
that line appears verbatim after the LOGIN.

```
ROWS     10   R0a R0b R0c
              SB1 SB2 SB3 SB4
              BK1 BK2 BK3
         ──
         10   = 3 R0 + 4 SB + 3 BK     git grep -c '^### R0\|^### SB\|^### BK' <ref> -- GATE-legacy-a.md
```

**Plan:** `PLAN-legacy-port.md` on `docs/batch-surveys` (`48053ddb`), sections 2.3 and 2.6 and RULINGS: Ben's
2026-09-29 answers (old numbers are the provisional starting values; acquisition is `/rpg give` only) and the
seat's L5 (LEGACY-A is these two; Scattershot waits for E6).

## GAME MODE

**SURVIVAL on every row.** The Short Bow's magazine is a cost (SB4), and creative removes costs (the
creative-divergence register).

## THE ORDER IN THE ONE BOOT

#168's rows, then `GATE-level.md`, then `GATE-nexus-polish.md`, then **these**. **This slice adds two content files
and touches no code**, so no earlier row is restated: nothing an earlier row reads is in these two files.

## Set-up

- **`/gamerule spawn_mobs false`** and **`/time set 18000`** (26.1's rule name; `GATE-level.md`'s set-up says why).
- **`/rpg playerxp set @s 1 levels`**, so no level damage is added (a weapon hit carries +1 per level-up,
  `GATE-level.md`). The earlier gates' last steps restore Ben's XP; **this gate's last step restores it again**, so
  record the stats head's `Lifetime XP` before this line if the earlier restore has run.
- **No accessories and no armour**, so no class damage or crit gear is in play.
- **`/rpg mobtrace`** ON (the reply names PLAYERHIT).
- **`/rpg give short_bow`** and **`/rpg give blaze_kings_staff`**. **For each, in hand: `/rpg gearscore set 100`**,
  so every figure below is the authored one (score 100 scales nothing).
- **The test zombie is #168's** (its section *THE TEST ZOMBIE*): the four commands, `NoAI:1b`, a fresh one per row.
  **A line with `crit=true` is not comparable: re-fire.**

---

## R0

### R0a — the build line names this branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head, shortened. Not `-dirty`, not `unknown`, not #170's, #169's or #168's head | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries both files and the three slices beneath

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/weapons/short_bow.yml`, `content/weapons/blaze_kings_staff.yml`, #170's `menu/NexusScreens`, #169's `menu/LevelMenu`, #168's `combat/TracedHit`. ABSENT: the control | `GATE-level.md` R0b's scan, with these five entries | |

### R0c — two weapons more, and nothing else moved

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, **15 weapons**, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. Against #170's prediction: **weapons 13 → 15**, every other count unchanged (neither file adds a visual). No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | |

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

## THE BLAZE KING'S STAFF (cfde822 `StaffListener`; PLAN-legacy-port.md 2.6)

### BK1 — the tooltip, and it does not stack (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Hover the staff (score set to 100). The golden rendering: **`Fire`**, blank, **`Fireball  Left-Click`**, `Throws a burning charge.`, **`Fire Damage: 30`**, **`Cooldown: 0.4s \| Mana Cost: 8`**, blank, **`Smolder  Right-Click`**, `Everything near you starts to burn.`, **`Fire Damage: 1`**, **`Cooldown: 20.0s \| Mana Cost: 40`**, blank, the two flavour lines, blank, **`Legendary Magic Weapon`**. A second `/rpg give blaze_kings_staff` lands in a **separate slot** (a vanilla blaze rod stacks; the mint caps ours at 1) | |

### BK2 — the Fireball: 30 fire, on a left-click (SURVIVAL) — witness: PLAYERHIT line

| prediction | READING |
|---|---|
| `/rpg mana refill`. Zombie offset `~5 ~ ~0`, aim at it, **left-click**: a **fire charge** flies, and **`PLAYERHIT … source=blaze_kings_staff/left_click … element=fire sent=30.000 crit=false triggerScore=100`**. The zombie then burns (Scorch, from `fire.yml`) | |

### BK3 — Smolder: 1 fire to everything within 10, nothing beyond (SURVIVAL) — witness: PLAYERHIT lines

| prediction | READING |
|---|---|
| `/kill @e[tag=t]`, then two zombies: `/summon zombie ~6 ~ ~0 {NoAI:1b,PersistenceRequired:1b,IsBaby:0b,Tags:["t"]}` and `/summon zombie ~14 ~ ~0 {NoAI:1b,PersistenceRequired:1b,IsBaby:0b,Tags:["t"]}` (6 and 14 blocks away). `/rpg mana refill`, **right-click**: **exactly one** `PLAYERHIT … source=blaze_kings_staff/right_click … element=fire sent=1.000 …`, whose target is the zombie at 6; none for the zombie at 14 (radius 10). Ben is not hit (a burst skips its caster). **The burn is small, and that is the carried number** (the file's header): Scorch is capped by a 1-damage hit | |

**Not a row: whether Smolder should hit other players.** It does today, as every caster-centred burst does
(PLAN-legacy-port.md F6), and cfde822's skipped its party. That is Ben's open **Q-P5**, recorded, not staged here.

---

## LAST STEP — PUT BEN'S XP BACK

`/rpg playerxp set @s <the number recorded> xp`.

---

## NO MUTATIONS

This slice is two content files and the three test edits content moves: `KNOWN_FIRE_DAMAGE_SITES` 21 → 23, recounted
per file with the test's own pattern; `KNOWN_RANGER_WEAPONS` 6 → 7, the test's own list; and `golden-lore.txt` 92 →
94 renderings, regenerated, whose diff is the two new blocks plus the footer line and nothing else (+29 / −1). There is
no new code for a mutation to reach. The loaders and the invariants that read these files are the existing suite's.
