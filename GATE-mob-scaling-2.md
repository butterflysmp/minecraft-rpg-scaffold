# GATE — Mob scaling, slice 2: every mob-to-player hit is vanilla x 5 x GS/100 (M16)

> **THE SEAT'S RULING, 2026-09-28 (a correction of the seat's own earlier rule; the readings below had followed
> that rule as written).** A row whose prediction is a LOG LINE is **PASS when that line appears verbatim in a boot
> whose log shows Ben's LOGIN, whether or not Ben names the row.** "Reported good or skipped -- not itemised"
> applies **only** to rows that rest on Ben's own observation: feel, visuals, chat he saw.

**CURRENT TALLY (after the ruling):**
- **PASS:** R0a, R0b and R0c (both boots); **G8**, the **G9 creeper** half, the **G9b creeper** half and **G-DIFFb**,
  each citing its re-run `MOBHIT` line quoted in its cell.
- **NO READING:** the **G9** and **G9b skeleton** halves (no bow: PLAN §6 F16).
- **Everything else:** unchanged below.

**Status (history): READ 2026-09-28 on `ef2d91c` -- 3 of 34 PASS (R0 only); 31 "reported good or skipped -- not itemised";
NO ROW HAS A WITNESS.** R0a/R0b/R0c PASS, read by Session B from the boot that began at 00:35:46 (`Build: ef2d91c`
at 00:35:52). Ben's LOGIN is in that boot's log (`BaronVonYeetus joined the game` at 01:22:56), so readings count.
Ben said "gates look good" and then **declined the one question** asking which rows he ran. So, following the
seat's rule, every row is recorded as **"reported good or skipped -- not itemised", never as PASS.** Session B
stopped the server through the console file at 01:33:25 (`Stopping the server` with no in-game `/stop` line). It
was a clean shutdown, all dimensions saved, and afterwards zero `java.exe` were running.

**RE-RUN, 2026-09-28, of G8, G9, G9b and G-DIFFb only (the seat's instruction). STILL NOT ITEMISED, BUT NOW
WITNESSED IN THE LOG.**
- **The boot:** it began at 01:38:46, built from `ef2d91c` checked out detached (so `Build: ef2d91c`; `249397b`
  differs only in this file). R0 was re-read: `[01:38:50] [Rpg] Build: ef2d91c`, 800 entries with all 7 slice
  classes and no control, and the same `Loaded ...` line. Ben's LOGIN is at 01:40:33.
- **Armour:** checked from the console with a control. At 01:40:55, `data get entity BaronVonYeetus Health` returned
  `22.0f` and `equipment` returned `{}` (after Ben's `/clear` at 01:40:35 and `/kill` at 01:40:42). At 01:48:50,
  `Health` returned `12.04f` and `equipment` was absent (nothing equipped). No armour `/rpg give` on this boot.
- **Ben ran `/gamemode survival` and `/rpg mobtrace`** (01:41:06 and 01:41:11), and then **did not name the rows he
  ran**. So each of the four keeps "not itemised", and the `MOBHIT` lines are quoted verbatim beside each row for
  the seat.
- **What those lines show:** every one matches its prediction. The arrow halves of G9 and G9b are **NO READING**:
  `/rpg spawn skeleton` spawns with no bow (PLAN §6 F16), and the log has no `/item replace ... with bow` line.
- **THE WORLD WAS ON HARD, NOT EASY.** This file's set-up said "the server's difficulty, `easy`". That was wrong.
  `run/server.properties` says easy, but Ben's `/difficulty hard` of 2026-09-25 (log `2026-09-25-4`) persists in
  the world's saved data, and `MOBHIT` printed `difficulty=HARD` until his `/difficulty easy` at 01:46:32. G-DIFFb
  set each difficulty explicitly, so it is unaffected.
- **Not in the four, recorded as observations only:** a GS 300 shulker bullet read `ratio=15.000` on both HARD
  (01:45:24) and EASY (01:47:09). That is P-SHULKER-shaped evidence; its status stays not itemised.
- The server was stopped through the console file at 01:49:03, cleanly, with zero `java.exe` left.

**What the log shows (the FIRST session, 00:35:46 boot). Read this before crediting any row.** This boot logs every command (`commands.log: true`).
Between the login and the stop there is **no `/rpg mobtrace`, so there is NO `MOBHIT` line at all**, and no
`/rpg spawn`, `/rpg mobinfo`, `/difficulty` or `/rpg healthboost`. The commands were `/tp` to (7500, 7500),
(7500, 0) and (5000, 0); `/gamemode survival|creative`; `/rpg give` of four netherite armour pieces (the helmet seven
times, the leggings five, the boots and chestplate once each) and `fletchers_quiver`; `/rpg playerxp set`; `/rpg heal`; `/rpg gearscore set 500` (four
times); and `/kill`. **The damage rows were written for an UNARMOURED player, and netherite was handed out**, so
even an in-game observation of a mob hit would have gone through armour. There is no `resolved NO gear score` WARN,
no `SEVERE` and no exception. **So the ratio rows (G8, G8b, G9, G9b, G10, G-DIFF, G-DIFFb, the probe table and
GX-ENV) are unwitnessed**: their `MOBHIT` lines were never produced. The carried-forward slice 1 rows are
unwitnessed too, for the same reason as before (no `/rpg spawn`, no `MOBSEED`/`MOBREMOVE`, no restart).

Every prediction below was written **before** any boot of this slice, and **this file is
COMMITTED to `feat/mob-scaling-2` before the boot**, unlike slice 1, whose gate file first appeared in its readings
commit. The build that boots is the commit that contains this file, so a prediction can be checked against origin
for having existed first. Readings go **beside** a prediction, never over it, and a prediction is not edited once its
row has been read. **Readings count only if Ben's LOGIN appears in THIS boot's log.**

> **EDITED 2026-09-28, BEFORE ANY READING, FOR M24 (mob damage ignores difficulty).** No row had been read and this
> slice had not booted. Every changed prediction carries **"edited before any reading (M24)"**. G-DIFF and G-DIFFb
> are new. The first version of this file is `6df2566`, and `git diff 6df2566 -- GATE-mob-scaling-2.md` shows every
> change.

```
ROWS     35   R0a R0b R0c
              G8 G8b G9 G9b G10 G-DIFF G-DIFFb F16a
              P-ARROW P-TRIDENT P-BLAZE P-GHAST P-SKULL P-SPIT P-SHULKER P-CREEPER P-WITCH P-BOOM P-FANGS
              P-GUARDIAN P-BREATH P-WIND P-DOT
              G13 G12 G14 G15 G15b G16 G11 G18 GX-ENV
         ──
         35   = 20 headings   git grep -c '^### R0\|^### G\|^### F16' <ref> -- GATE-mob-scaling-2.md
              + 15 probe rows git grep -c '^| \*\*P-' <ref> -- GATE-mob-scaling-2.md
              (F16a added 2026-09-28 with the F16 fix, before any boot of it; the heading grep widened to count it)
```

**Plan:** `PLAN-mob-scaling.md` §1.2, §3 slice 2 and §4 (G8-G10). Rulings M1-M24, and **M16** and **M24** above all.

## Set-up

**Declared game mode, PER ROW** (verification.md, *THE CREATIVE-DIVERGENCE REGISTER*). **Every row that reads
damage TO A PLAYER is SURVIVAL**: a creative player takes no damage and is not targeted, which removes the very
cost the row measures. Only the carried-forward nameplate rows are creative.

- An op player. Survival rows: `/gamemode survival`, **unarmoured**, and on **the server's difficulty, `easy`**
  (`run/server.properties`).
- **`/rpg mobtrace` ON for every damage row.** Each mob-to-player hit then logs one line:

  ```
  MOBHIT cause=<DamageCause> direct=<type> causing=<type> difficulty=<EASY|NORMAL|HARD> raw=<n> vanilla=<n>
         gs=<n|-> from=<DIRECT|CAUSING|ABSENT> custom=<bool> applied=<n> ratio=<applied/vanilla> victimMax=<player max>
  ```

  **The row reads `ratio` and `victimMax` off the server; nothing here needs a predicted HP number.**
  *Edited before any reading (M24):* `raw` is the event's own `getDamage()`, which **includes vanilla's difficulty
  scaling** (`Player.hurtServer` applies it before `LivingEntity.hurtServer` raises the Bukkit event, measured with
  `javap`). `vanilla` is that amount **with the scaling undone** (M24), after the damage window and the shield. On a
  source that scales with difficulty, `raw` and `vanilla` are related by vanilla's formula: EASY `raw = min(v/2 + 1, v)`,
  NORMAL `raw = v`, HARD `raw = 1.5v`. For melee, both are the attribute.
- `/rpg spawn <mob> <gs>` spawns a mob at your feet at that score, and `/rpg mobinfo` reads the mob in your
  crosshair. **Every row names its GS, and all of them avoid 100** unless the row is about 100.
- **A player max that is NOT 100:** `/rpg healthboost 300` mints a `health_boost_TEMP`. **Held in the main hand**,
  your max is 400, and `MOBHIT`'s `victimMax=400.0` is the witness. Drop it to return to 100.

**THE ONE PREDICTION EVERY MOB ROW SHARES:** `ratio = 5 x GS/100` for a vanilla mob, which is **15.000** at GS 300,
**at any `victimMax`.** The old share-of-max pricing (master `6ec09cc`) reads `ratio = victimMax/20` whatever the
GS: 5 at max 100 and 20 at max 400. The stacking bug reads their product: 75 at max 100, 300 at max 400. **A ratio
of 5.000 at max 100 is AMBIGUOUS** (flat GS 100, or the old pricing), which is why the load-bearing rows use GS 300
and max 400.

---

## R0 — THE DEPLOYED BUILD CARRIES THIS SLICE. IF ANY R0 FAILS, STOP

### R0a — the build line names the head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, where `<sha>` is the PR's head as `gh pr view <n> --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS.** `[00:35:52] [Server thread/INFO]: [Rpg] Build: ef2d91c`, a bare hash. The PR head was `ef2d91c`, the tree was clean against HEAD, and `dev-server.sh` echoed `==> Building ef2d91c` |

### R0b — the jar carries the slice

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `MobDamagePricing`, `MobDamagePricing$Resolved`, `MobDamagePricing$MobFacts`, `RerouteDamagePrice`, `MobScaling`, and *(edited before any reading (M24))* `VanillaDifficulty` and `VanillaDifficulty$Difficulty`. ABSENT: the control | the scan below | **PASS.** The jar deployed for the 00:35:52 boot has 800 entries: `MobDamagePricing`, `$Resolved`, `$MobFacts`, `RerouteDamagePrice`, `MobScaling`, `VanillaDifficulty` and `VanillaDifficulty$Difficulty` are `PRESENT`, and `NoSuchClassControl` is `ABSENT` |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/mob/MobDamagePricing.class',
               'io/github/butterflysmp/rpg/core/mob/MobDamagePricing$Resolved.class',
               'io/github/butterflysmp/rpg/core/mob/MobDamagePricing$MobFacts.class',
               'io/github/butterflysmp/rpg/core/combat/RerouteDamagePrice.class',
               'io/github/butterflysmp/rpg/core/mob/MobScaling.class',
               'io/github/butterflysmp/rpg/core/mob/VanillaDifficulty.class',
               'io/github/butterflysmp/rpg/core/mob/VanillaDifficulty$Difficulty.class',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — the boot log: content loads, nothing refused

| prediction | instrument | READING |
|---|---|---|
| `Loaded 8 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 2 pools, 6 fragments, 4 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`: the same as slice 1's boot, because this slice changes no content. **No** `Refusing` or `Skipping` line, and **no** `SEVERE` or exception. The known WARNs are `volley_stone`'s 27-tick cooldown and its summary line | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | **PASS.** `[00:35:52] [Server thread/INFO]: [Rpg] Loaded 8 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 2 pools, 6 fragments, 4 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. No `Refusing`, `Skipping`, `SEVERE` or exception lines. The WARNs are `volley_stone`'s 27-tick one plus its summary line, and Paper's version banner |

---

## THE SLICE'S OWN ROWS (plan §4 G8-G10). ALL SURVIVAL

### G8 — melee at GS 100 is x5 (was GX-MELEE in slice 1) (SURVIVAL)

| prediction | READING |
|---|---|
| `/rpg spawn zombie 100`, and take one hit at max 100. `MOBHIT cause=ENTITY_ATTACK direct=zombie causing=zombie ... gs=100 from=DIRECT ratio=5.000`. `raw` and `vanilla` are both `mobinfo`'s vanilla `ATTACK_DAMAGE`, the attribute, on every difficulty. *(Edited before any reading (M24): this said "not a difficulty-adjusted amount: F6". Melee was already difficulty-blind; M24 makes the other paths match it.)* **Slice 1's GX-MELEE expected x1 here; this slice expects x5.** On its own this row cannot tell flat from share-of-max (both give 5 at max 100); G8b and G9b can | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note **RE-RUN, 2026-09-28, boot of 01:38:46 (`Build: ef2d91c`, login 01:40:33): STILL NOT ITEMISED.** Ben did not name the rows he ran, so under the seat's rule none becomes PASS or FAIL. The log's own lines, VERBATIM: `[01:41:20] [Rpg] MOBHIT cause=ENTITY_ATTACK direct=zombie causing=zombie difficulty=HARD raw=3.000 vanilla=3.000 gs=100 from=DIRECT custom=false applied=15.000 ratio=5.000 victimMax=100.0`, after `/rpg spawn zombie 100` at 01:41:19. It matches the prediction (ratio 5.000, `raw` = `vanilla` = the attribute on HARD) **PASS, by the seat's ruling of 2026-09-28** (see the header): the predicted log line appears verbatim above, in the 01:38:46 boot, whose log shows Ben's LOGIN (01:40:33). |

### G8b — melee at GS 300 is x15 (SURVIVAL)

| prediction | READING |
|---|---|
| `/rpg spawn zombie 300`, one hit at max 100: `gs=300 ratio=15.000`. `mobinfo`'s "custom attack" reads 15 x its vanilla `ATTACK_DAMAGE` | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |

### G9 — a creeper and a skeleton at GS 300, at max 100 (SURVIVAL)

| prediction | READING |
|---|---|
| `/rpg spawn creeper 300` and let it blow; `/rpg spawn skeleton 300` and take an arrow. Both lines read **`ratio=15.000 victimMax=100.0`**. The creeper reads `cause=ENTITY_EXPLOSION direct=creeper causing=creeper from=DIRECT` (`explosion(entity, causing)`, both the creeper). The skeleton reads `cause=PROJECTILE direct=arrow causing=skeleton from=DIRECT` (the launch stamp). *Edited before any reading (M24):* on this server's `easy`, each line's `raw` is `min(vanilla/2 + 1, vanilla)`: the undoing is visible in the line, and the ratio is taken against the undone `vanilla` | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note **RE-RUN, 2026-09-28, boot of 01:38:46 (`Build: ef2d91c`, login 01:40:33): STILL NOT ITEMISED.** Ben did not name the rows he ran, so under the seat's rule none becomes PASS or FAIL. The log's own lines, VERBATIM: creeper: `[01:42:20] [Rpg] MOBHIT cause=ENTITY_EXPLOSION direct=creeper causing=creeper difficulty=HARD raw=51.277 vanilla=34.185 gs=300 from=DIRECT custom=false applied=512.771 ratio=15.000 victimMax=100.0`, after `/rpg spawn creeper 300` at 01:42:19. It matches (ratio 15.000 at max 100; `raw` = 1.5 x `vanilla`, HARD undone). **Skeleton ARROW half: NO READING.** The `/rpg spawn skeleton 300` at 01:42:56 had NO BOW (PLAN §6 F16), and **the log carries no `/item replace ... with bow` line**, so it was never armed. Its only lines are melee, not the arrow the row predicts: `[01:42:58] [Rpg] MOBHIT cause=ENTITY_ATTACK direct=skeleton causing=skeleton difficulty=HARD raw=2.000 vanilla=2.000 gs=300 from=DIRECT custom=false applied=30.000 ratio=15.000 victimMax=100.0` (logged twice at 01:42:58) **CREEPER HALF: PASS, by the seat's ruling of 2026-09-28** (see the header), citing the 01:42:20 line above, in the boot whose log shows Ben's LOGIN (01:40:33). **SKELETON HALF: NO READING**, unchanged. |

### G9b — THE SAME, AT MAX 400. THE ROW THAT CANNOT PASS BY ACCIDENT (SURVIVAL)

| prediction | READING |
|---|---|
| Hold `/rpg healthboost 300`'s item in the main hand; confirm `victimMax=400.0` in the line. The creeper and the skeleton at GS 300 again: **`ratio=15.000 victimMax=400.0`** for both. **The old pricing reads 20.000; the stacking bug reads 300.000.** This is the seat's required row, "a player whose max HP is NOT 100", and the reason the unit test takes no player max | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note **RE-RUN, 2026-09-28, boot of 01:38:46 (`Build: ef2d91c`, login 01:40:33): STILL NOT ITEMISED.** Ben did not name the rows he ran, so under the seat's rule none becomes PASS or FAIL. The log's own lines, VERBATIM: creeper: `[01:44:01] [Rpg] MOBHIT cause=ENTITY_EXPLOSION direct=creeper causing=creeper difficulty=HARD raw=50.346 vanilla=33.564 gs=300 from=DIRECT custom=false applied=503.456 ratio=15.000 victimMax=400.0`, after `/rpg healthboost` (01:43:35) and `/rpg spawn creeper 300` (01:43:59). **It matches: ratio 15.000 at `victimMax=400.0`**, where the old pricing reads 20 and stacking 300. **Skeleton ARROW half: NO READING.** No skeleton was spawned at max 400, and no `/item` line exists **CREEPER HALF: PASS, by the seat's ruling of 2026-09-28** (see the header), citing the 01:44:01 line above, in the boot whose log shows Ben's LOGIN (01:40:33). **SKELETON HALF: NO READING**, unchanged. |

### G10 — the shooter dies mid-flight; the arrow keeps its stamp (SURVIVAL)

| prediction | READING |
|---|---|
| `/rpg spawn skeleton 300` at range. Right after it looses, `/kill` it (aimed with `@e[type=skeleton,limit=1,sort=nearest]`). The arrow lands: **`direct=arrow causing=none gs=300 from=DIRECT ratio=15.000`**. Without the launch stamp, a gone shooter leaves nothing mob-shaped, and the hit prices by share-of-max with no MOBHIT line at all. *Edited before any reading (M24), and deliberately a RANGE:* with no causing entity, whether vanilla scaled this hit depends on the arrow damage type's scaling setting, **which was not read**. So `raw = vanilla` (it did not scale) and `raw = min(vanilla/2 + 1, vanilla)` (it did) are both consistent with M24. The row's claim is `ratio=15.000`; the reading says which of the two it was | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |

### G-DIFF — the same skeleton at GS 100 hits for the same amount on easy and hard (SURVIVAL) — NEW (M24)

| prediction | READING |
|---|---|
| `/rpg spawn skeleton 100`. Take **five** arrows on `/difficulty easy`, then `/difficulty hard` and five more. **Every line reads `ratio=5.000`.** On easy, each `raw` is `min(vanilla/2 + 1, vanilla)`; on hard, each `raw` is `1.5 x vanilla`. **The `vanilla` values come from the same spread on both** (whole numbers, as `ceil(speed x base)` is), with no upward shift on hard. **Why five, and why "the same spread" rather than "the same number":** vanilla's arrow damage is random per shot (`setBaseDamageFromMob` draws `triangle(.., 0.57425)`, and the impact speed varies), so two shots on ONE difficulty can already differ. What M24 removes is the shift *(edited before any reading (M24 reference): this said "+0.11 x id on the base"; it is measured from NORMAL, so NORMAL is untouched)*: the base has (id - 2) x 0.11 subtracted at launch, so easy's base gains 0.11 and hard's loses 0.11, landing both on NORMAL's; and x1.5 (hard) or `/2 + 1` (easy) is undone at impact. **The `vanilla` spread on both is therefore vanilla-NORMAL's**, not a difficulty-free one below it. **Before M24, hard's `raw` would run 1.5 x easy's pre-scaling amount and no `vanilla` field existed. After it, `applied` is 5 x `vanilla` on both.** G-DIFFb is the deterministic twin | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |

### G-DIFFb — a GS 100 shulker's bullet is exactly 20 on easy and on hard (SURVIVAL) — NEW (M24)

| prediction | READING |
|---|---|
| `/rpg spawn shulker 100`. One bullet on `/difficulty easy`, one on `/difficulty hard`. The bullet is a fixed `4.0f` (`ShulkerBullet.onHitEntity`, `mobProjectile`, read with `javap`), so there is no randomness. **Easy: `raw=3.000 vanilla=4.000 applied=20.000`. Hard: `raw=6.000 vanilla=4.000 applied=20.000`.** Identical `applied` on both is M24. **Before M24, the flat price would have been 15 on easy and 30 on hard.** The two `raw` values assume the `mob_projectile` damage type scales with difficulty, **which was not read**. If both read `raw=4.000`, the type does not scale, vanilla never adjusted the bullet, and the row still holds on `applied` Set `/difficulty easy` back afterwards | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note **RE-RUN, 2026-09-28, boot of 01:38:46 (`Build: ef2d91c`, login 01:40:33): STILL NOT ITEMISED.** Ben did not name the rows he ran, so under the seat's rule none becomes PASS or FAIL. The log's own lines, VERBATIM: hard: `[01:45:08] [Rpg] MOBHIT cause=PROJECTILE direct=shulker_bullet causing=shulker difficulty=HARD raw=6.000 vanilla=4.000 gs=100 from=DIRECT custom=false applied=20.000 ratio=5.000 victimMax=100.0`; easy (after `/difficulty easy` at 01:46:32): `[01:46:40] [Rpg] MOBHIT cause=PROJECTILE direct=shulker_bullet causing=shulker difficulty=EASY raw=3.000 vanilla=4.000 gs=100 from=DIRECT custom=false applied=20.000 ratio=5.000 victimMax=100.0`. **Both match the prediction EXACTLY** (easy `raw=3` / hard `raw=6`, `vanilla=4`, `applied=20` on both). The unread assumption in this row, that `mob_projectile` scales with difficulty, is now measured: it does **PASS, by the seat's ruling of 2026-09-28** (see the header): the predicted log line appears verbatim above, in the 01:38:46 boot, whose log shows Ben's LOGIN (01:40:33), both halves (01:45:08 HARD and 01:46:40 EASY). |

### F16a — `/rpg spawn skeleton 100` holds a bow (CREATIVE) — NEW (F16, predicted before the boot)

| prediction | READING |
|---|---|
| `/rpg spawn skeleton 100`, then on the **server console** (Session B runs these, so the reading is the server's own), anchored at Ben so "nearest" means the skeleton beside him: (1) `execute at BaronVonYeetus run data get entity @e[type=skeleton,limit=1,sort=nearest] Health`, the **positive control**: a number such as `20.0f` proves the selector found the skeleton. (2) `execute at BaronVonYeetus run data get entity @e[type=skeleton,limit=1,sort=nearest] equipment.mainhand`. **Predicted: `... has the following entity data: {count: 1, id: "minecraft:bow"}`**, possibly with extra keys; the row reads `id: "minecraft:bow"`. **Before F16 (`ef2d91c`), (2) reads `Found no elements matching equipment.mainhand`** (the bow never existed). **Why this path:** 26.1 has no `HandItems`. `LivingEntity` saves and loads equipment under `equipment` (`addAdditionalSaveData` / `readAdditionalSaveData`, read with `javap`), and `mainhand` is `EquipmentSlot`'s serialised name (its static initialiser). `execute at` pins where "nearest" is measured from, because where a bare console selector measures from was not read | |

---

## THE PROBE TABLE — every §1.2 path that can be produced in game (SURVIVAL)

Each row: spawn the mob at the named GS, take the hit at max 100, and read the `MOBHIT` line. **Each ‡ cell in plan
§1.2 fills from this table's readings.** The `direct` and `causing` predictions come from the vanilla factory
signatures read with `javap` (`DamageSources.arrow(AbstractArrow, Entity)`, `indirectMagic(Entity, Entity)`,
`sonicBoom(Entity)` and the rest). **The `cause` column is NOT predicted where it is marked ‡**: which Bukkit
`DamageCause` a vanilla type maps to was not measured, and the pricing does not depend on it (it keys on the entities).

| row | set-up (GS) | predicted `direct` / `causing` / `from` | predicted `ratio` | READING |
|---|---|---|---|---|
| **P-ARROW** | `skeleton 200` (also `stray`, `bogged`, `pillager` if convenient) | `arrow` / `skeleton` / `DIRECT` | **10.000** | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |
| **P-TRIDENT** | a `drowned 300` holding a trident (natural spawns only; `/rpg spawn` gives none: skip the row if none turns up) | `trident` / `drowned` / `DIRECT` | **15.000** | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |
| **P-BLAZE** | `blaze 300` (Nether) | `small_fireball` / `blaze` / `DIRECT`, cause ‡ | **15.000** | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |
| **P-GHAST** | `ghast 300` | impact: `fireball` / `ghast` / `DIRECT`, cause ‡. **The blast is a second hit**: `explosion(fireball, ghast)`, so it is also `DIRECT` from the fireball's stamp. Both lines read the same | **15.000** each | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |
| **P-SKULL** | a Wither: `/rpg spawn wither 300` (a creative build, then survival to take the hit) | `wither_skull` / `wither` / `DIRECT`, cause ‡ | **15.000** | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |
| **P-SPIT** | a llama you anger (hit it once); **passive by M21** | `llama_spit` / `llama` / `CAUSING`, **`gs=-`** (passive: no stamp, no GS) | **5.000** | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |
| **P-SHULKER** | `shulker 300` | `shulker_bullet` / `shulker` / `DIRECT`, cause ‡ | **15.000** | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |
| **P-CREEPER** | `creeper 200` | `creeper` / `creeper` / `DIRECT` | **10.000** | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |
| **P-WITCH** | `witch 300`, and take a harming splash | `potion` (or `splash_potion`) / `witch` / `DIRECT`, cause ‡ (MAGIC expected) | **15.000** | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |
| **P-BOOM** | `warden 300`, sonic boom | `warden` / `warden` / `DIRECT` (`sonicBoom(entity)`) | **15.000** | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |
| **P-FANGS** | `evoker 300`, fangs | `evoker_fangs` / `evoker` / **`CAUSING`**: fangs are not a projectile, so they carry no stamp | **15.000** | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |
| **P-GUARDIAN** | `guardian 300` (in water), beam | `guardian` / `guardian` / `DIRECT`, cause ‡ | **15.000** | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |
| **P-BREATH** | the dragon's breath cloud, if reachable (the End; skip otherwise) | ‡ **UNPREDICTED**: the cloud applies instant damage through `indirectMagic(cloud, owner)` if it names its owner, and then `CAUSING` from the dragon. If it names none, **no MOBHIT** and the hit is share-of-max; that becomes a named finding | ‡ | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |
| **P-WIND** | `breeze 300`, wind charge | `breeze_wind_charge` / `breeze` / `DIRECT` | **15.000** | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |
| **P-DOT** | a cave spider's poison, or a wither skeleton's wither | **NO MOBHIT line for the ticks.** `wither()` and `magic()` take no entity (measured), so the ticks are not mob-sourced and stay on share-of-max, with no GS (§6 F5). **A MOBHIT line on a POISON or WITHER tick falsifies F5** | — | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |

---

## CARRIED FORWARD FROM SLICE 1: ITS UNREAD ROWS, PREDICTIONS UNCHANGED

These were NOT READ on slice 1's boot (`GATE-mob-scaling-1.md`, readings `1a476d3`). Slice 2 does not change
seeding or storage, so their predictions stand as slice 1 wrote them.

### G13 — the GS survives a restart (437) (CREATIVE)

| prediction | READING |
|---|---|
| Near spawn, `/rpg spawn zombie 437`. The plate reads **`[437]`**. Stay near it. I restart the server through the console file (`stop`, then boot WITHOUT `--refresh-content`). Rejoin and look: still **`[437]`**, with `mobinfo` stored GS 437 | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |

### G12 — the GS survives a chunk unload (CREATIVE)

| prediction | READING |
|---|---|
| `/rpg mobtrace` ON. `/rpg spawn zombie 222`, fly several hundred blocks away and come back. Still **`[222]`**, and the log shows **`MOBREMOVE <uuid>`**, then **`MOBSEED <uuid> ... gs=222 source=stored`** for the same uuid | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |

### G14 — a mob through a Nether portal keeps its GS (SURVIVAL)

| prediction | READING |
|---|---|
| `/rpg spawn zombie 140`, and get it to walk or be pushed through a Nether portal. In the Nether it still reads **`[140]`**, while `mobinfo`'s "would roll" there reads at least 200. Full HP on arrival is §6 F1, and expected | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |

### G15 — a stored GS of 0 is re-rolled (CREATIVE)

| prediction | READING |
|---|---|
| `/summon zombie ~ ~ ~ {BukkitValues:{"rpg:mob_gear_score":0}}` near spawn: re-rolled from position (~[100]), and the log has a WARN naming the invalid `mob_gear_score` | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |

### G15b — a stored GS of 900 is re-rolled, not clamped (CREATIVE)

| prediction | READING |
|---|---|
| The same with `900`: ~[100], NOT [900] and NOT [500], with a WARN | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |

### G16 — conversions inherit the GS (CREATIVE)

| prediction | READING |
|---|---|
| `/rpg spawn slime 300` (a large one) and kill it: **the children read `[300]`**. And/or a GS 300 zombie drowned until it converts: the drowned reads **`[300]`** | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |

### G11 — environmental damage on a mob is proportional (M13) (CREATIVE)

| prediction | READING |
|---|---|
| Drop a `/rpg spawn zombie` and a `/rpg spawn zombie 500` from ~25 blocks: **both die** | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |

### G18 — the Knell in lava: proportional (M14) (CREATIVE)

| prediction | READING |
|---|---|
| `/rpg spawn knell` and `/rpg spawn knell 200` (720 HP) in lava for the same few seconds lose **about the same FRACTION** of their bar, read with `mobinfo` before and after | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |

### GX-ENV — a player's environmental damage did NOT change (SURVIVAL)

| prediction | READING |
|---|---|
| At max 400 (the healthboost held), fall far enough to take damage. **No `MOBHIT` line**, and the loss is a share of max as on master: a fall of vanilla amount `a` takes `a x 400/20 = 20a`. This is the other half of `RerouteDamagePrice`: only a mob-sourced hit leaves `DamageScale` | **Reported good or skipped -- not itemised.** Ben declined the one question (2026-09-28). The log has no `MOBHIT` line and no `/rpg spawn`: see the Status note |

---

## Findings this gate carries (named, not fixed)

- ~~**F6: melee is difficulty-blind; every other path is difficulty-adjusted.**~~ **CLOSED BY M24** *(edited before
  any reading (M24))*. The measurement stands: `Player.hurtServer` scales when `scalesWithDifficulty()`, EASY
  `min(a/2 + 1, a)` and HARD `a x 3/2`, before the Bukkit event. Every other difficulty channel into a hit's AMOUNT
  was found by sweeping every `getDifficulty` reference in `net.minecraft.world.entity`: the guardian beam's HARD +2,
  and the mob arrow's `+ id x 0.11`. All three are moved to NORMAL (`VanillaDifficulty`), M24's reference, and
  G-DIFF and G-DIFFb are the rows. *(Edited before any reading (M24 reference): this said "undone", and the arrow
  was first normalised to PEACEFUL.)*
  **What M24 cannot reach**, per `PLAN-mob-scaling.md` §6 F6:
  - PEACEFUL, where a scaled hit never lands;
  - how often a mob hits (inaccuracy, the Wither's extra skulls);
  - effect durations (F5);
  - equipment rolled from local difficulty.
- **ABSENT is unreachable from shipped play.** Every hostile mob is seeded at world-add, and the seed stores a score,
  so a hostile mob with no score has to be untracked when it hits. The arm is guarded by
  `MobDamagePricingTest.anAbsentGsIsFiveTimesNeverOneAndNeverZero` and the absent-arm mutation, and **no row here
  can reach it**. Its WARN line has no gate witness.
- **A projectile fired before this build, or by an unseeded shooter, carries no stamp.** If its shooter is gone by
  impact, the hit is not mob-sourced and prices by share-of-max, with no MOBHIT line. The fangs of an evoker that
  dies mid-cast behave the same (fangs are never stamped).
- **The causing mob's PDC is read on the event's thread.** That is fine on Paper. On Folia a causing mob in another
  region needs the deferred `MobGearScores` map (PLAN §3 slice 2, resolution step 2).
