# GATE — Mob scaling, slice 2: every mob-to-player hit is vanilla x 5 x GS/100 (M16)

**Status: NOT RUN.** Every prediction below was written **before** any boot of this slice, and **this file is
COMMITTED to `feat/mob-scaling-2` before the boot**, unlike slice 1, whose gate file first appeared in its readings
commit. The build that boots is the commit that contains this file, so a prediction can be checked against origin
for having existed first. Readings go **beside** a prediction, never over it, and a prediction is not edited once its
row has been read. **Readings count only if Ben's LOGIN appears in THIS boot's log.**

```
ROWS     32   R0a R0b R0c
              G8 G8b G9 G9b G10
              P-ARROW P-TRIDENT P-BLAZE P-GHAST P-SKULL P-SPIT P-SHULKER P-CREEPER P-WITCH P-BOOM P-FANGS
              P-GUARDIAN P-BREATH P-WIND P-DOT
              G13 G12 G14 G15 G15b G16 G11 G18 GX-ENV
         ──
         32   = 17 headings   git grep -c '^### R0\|^### G' <ref> -- GATE-mob-scaling-2.md
              + 15 probe rows git grep -c '^| \*\*P-' <ref> -- GATE-mob-scaling-2.md
```

**Plan:** `PLAN-mob-scaling.md` §1.2, §3 slice 2 and §4 (G8-G10). Rulings M1-M23, and **M16** above all.

## Set-up

**Declared game mode, PER ROW** (verification.md, *THE CREATIVE-DIVERGENCE REGISTER*). **Every row that reads
damage TO A PLAYER is SURVIVAL**: a creative player takes no damage and is not targeted, which removes the very
cost the row measures. Only the carried-forward nameplate rows are creative.

- An op player. Survival rows: `/gamemode survival`, **unarmoured**, and on **the server's difficulty, `easy`**
  (`run/server.properties`).
- **`/rpg mobtrace` ON for every damage row.** Each mob-to-player hit then logs one line:

  ```
  MOBHIT cause=<DamageCause> direct=<type> causing=<type> vanilla=<n> gs=<n|-> from=<DIRECT|CAUSING|ABSENT>
         custom=<bool> applied=<n> ratio=<applied/vanilla> victimMax=<player max>
  ```

  **The row reads `ratio` and `victimMax` off the server; nothing here needs a predicted HP number.** `vanilla` is
  the amount after the damage window and the shield. For every non-melee path it is vanilla's `getDamage()`, which
  **already includes vanilla's difficulty adjustment** (measured with `javap`: `Player.hurtServer` applies it before
  `LivingEntity.hurtServer` raises the Bukkit event). The ratio is unaffected by that; see *Findings* F6.
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
| `[Rpg] Build: <sha>`, where `<sha>` is the PR's head as `gh pr view <n> --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries the slice

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `MobDamagePricing`, `MobDamagePricing$Resolved`, `MobDamagePricing$MobFacts`, `RerouteDamagePrice`, `MobScaling`. ABSENT: the control | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/mob/MobDamagePricing.class',
               'io/github/butterflysmp/rpg/core/mob/MobDamagePricing$Resolved.class',
               'io/github/butterflysmp/rpg/core/mob/MobDamagePricing$MobFacts.class',
               'io/github/butterflysmp/rpg/core/combat/RerouteDamagePrice.class',
               'io/github/butterflysmp/rpg/core/mob/MobScaling.class',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — the boot log: content loads, nothing refused

| prediction | instrument | READING |
|---|---|---|
| `Loaded 8 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 2 pools, 6 fragments, 4 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`: the same as slice 1's boot, because this slice changes no content. **No** `Refusing` or `Skipping` line, and **no** `SEVERE` or exception. The known WARNs are `volley_stone`'s 27-tick cooldown and its summary line | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | |

---

## THE SLICE'S OWN ROWS (plan §4 G8-G10). ALL SURVIVAL

### G8 — melee at GS 100 is x5 (was GX-MELEE in slice 1) (SURVIVAL)

| prediction | READING |
|---|---|
| `/rpg spawn zombie 100`, and take one hit at max 100. `MOBHIT cause=ENTITY_ATTACK direct=zombie causing=zombie ... gs=100 from=DIRECT ratio=5.000`. `vanilla` is `mobinfo`'s vanilla `ATTACK_DAMAGE` (the attribute, not a difficulty-adjusted amount: F6). **Slice 1's GX-MELEE expected x1 here; this slice expects x5.** On its own this row cannot tell flat from share-of-max (both give 5 at max 100); G8b and G9b can | |

### G8b — melee at GS 300 is x15 (SURVIVAL)

| prediction | READING |
|---|---|
| `/rpg spawn zombie 300`, one hit at max 100: `gs=300 ratio=15.000`. `mobinfo`'s "custom attack" reads 15 x its vanilla `ATTACK_DAMAGE` | |

### G9 — a creeper and a skeleton at GS 300, at max 100 (SURVIVAL)

| prediction | READING |
|---|---|
| `/rpg spawn creeper 300` and let it blow; `/rpg spawn skeleton 300` and take an arrow. Both lines read **`ratio=15.000 victimMax=100.0`**. The creeper reads `cause=ENTITY_EXPLOSION direct=creeper causing=creeper from=DIRECT` (`explosion(entity, causing)`, both the creeper). The skeleton reads `cause=PROJECTILE direct=arrow causing=skeleton from=DIRECT` (the launch stamp) | |

### G9b — THE SAME, AT MAX 400. THE ROW THAT CANNOT PASS BY ACCIDENT (SURVIVAL)

| prediction | READING |
|---|---|
| Hold `/rpg healthboost 300`'s item in the main hand; confirm `victimMax=400.0` in the line. The creeper and the skeleton at GS 300 again: **`ratio=15.000 victimMax=400.0`** for both. **The old pricing reads 20.000; the stacking bug reads 300.000.** This is the seat's required row, "a player whose max HP is NOT 100", and the reason the unit test takes no player max | |

### G10 — the shooter dies mid-flight; the arrow keeps its stamp (SURVIVAL)

| prediction | READING |
|---|---|
| `/rpg spawn skeleton 300` at range. Right after it looses, `/kill` it (aimed with `@e[type=skeleton,limit=1,sort=nearest]`). The arrow lands: **`direct=arrow causing=none gs=300 from=DIRECT ratio=15.000`**. Without the launch stamp, a gone shooter leaves nothing mob-shaped, and the hit prices by share-of-max with no MOBHIT line at all | |

---

## THE PROBE TABLE — every §1.2 path that can be produced in game (SURVIVAL)

Each row: spawn the mob at the named GS, take the hit at max 100, and read the `MOBHIT` line. **Each ‡ cell in plan
§1.2 fills from this table's readings.** The `direct` and `causing` predictions come from the vanilla factory
signatures read with `javap` (`DamageSources.arrow(AbstractArrow, Entity)`, `indirectMagic(Entity, Entity)`,
`sonicBoom(Entity)` and the rest). **The `cause` column is NOT predicted where it is marked ‡**: which Bukkit
`DamageCause` a vanilla type maps to was not measured, and the pricing does not depend on it (it keys on the entities).

| row | set-up (GS) | predicted `direct` / `causing` / `from` | predicted `ratio` | READING |
|---|---|---|---|---|
| **P-ARROW** | `skeleton 200` (also `stray`, `bogged`, `pillager` if convenient) | `arrow` / `skeleton` / `DIRECT` | **10.000** | |
| **P-TRIDENT** | a `drowned 300` holding a trident (natural spawns only; `/rpg spawn` gives none: skip the row if none turns up) | `trident` / `drowned` / `DIRECT` | **15.000** | |
| **P-BLAZE** | `blaze 300` (Nether) | `small_fireball` / `blaze` / `DIRECT`, cause ‡ | **15.000** | |
| **P-GHAST** | `ghast 300` | impact: `fireball` / `ghast` / `DIRECT`, cause ‡. **The blast is a second hit**: `explosion(fireball, ghast)`, so it is also `DIRECT` from the fireball's stamp. Both lines read the same | **15.000** each | |
| **P-SKULL** | a Wither: `/rpg spawn wither 300` (a creative build, then survival to take the hit) | `wither_skull` / `wither` / `DIRECT`, cause ‡ | **15.000** | |
| **P-SPIT** | a llama you anger (hit it once); **passive by M21** | `llama_spit` / `llama` / `CAUSING`, **`gs=-`** (passive: no stamp, no GS) | **5.000** | |
| **P-SHULKER** | `shulker 300` | `shulker_bullet` / `shulker` / `DIRECT`, cause ‡ | **15.000** | |
| **P-CREEPER** | `creeper 200` | `creeper` / `creeper` / `DIRECT` | **10.000** | |
| **P-WITCH** | `witch 300`, and take a harming splash | `potion` (or `splash_potion`) / `witch` / `DIRECT`, cause ‡ (MAGIC expected) | **15.000** | |
| **P-BOOM** | `warden 300`, sonic boom | `warden` / `warden` / `DIRECT` (`sonicBoom(entity)`) | **15.000** | |
| **P-FANGS** | `evoker 300`, fangs | `evoker_fangs` / `evoker` / **`CAUSING`**: fangs are not a projectile, so they carry no stamp | **15.000** | |
| **P-GUARDIAN** | `guardian 300` (in water), beam | `guardian` / `guardian` / `DIRECT`, cause ‡ | **15.000** | |
| **P-BREATH** | the dragon's breath cloud, if reachable (the End; skip otherwise) | ‡ **UNPREDICTED**: the cloud applies instant damage through `indirectMagic(cloud, owner)` if it names its owner, and then `CAUSING` from the dragon. If it names none, **no MOBHIT** and the hit is share-of-max; that becomes a named finding | ‡ | |
| **P-WIND** | `breeze 300`, wind charge | `breeze_wind_charge` / `breeze` / `DIRECT` | **15.000** | |
| **P-DOT** | a cave spider's poison, or a wither skeleton's wither | **NO MOBHIT line for the ticks.** `wither()` and `magic()` take no entity (measured), so the ticks are not mob-sourced and stay on share-of-max, with no GS (§6 F5). **A MOBHIT line on a POISON or WITHER tick falsifies F5** | — | |

---

## CARRIED FORWARD FROM SLICE 1: ITS UNREAD ROWS, PREDICTIONS UNCHANGED

These were NOT READ on slice 1's boot (`GATE-mob-scaling-1.md`, readings `1a476d3`). Slice 2 does not change
seeding or storage, so their predictions stand as slice 1 wrote them.

### G13 — the GS survives a restart (437) (CREATIVE)

| prediction | READING |
|---|---|
| Near spawn, `/rpg spawn zombie 437`. The plate reads **`[437]`**. Stay near it. I restart the server through the console file (`stop`, then boot WITHOUT `--refresh-content`). Rejoin and look: still **`[437]`**, with `mobinfo` stored GS 437 | |

### G12 — the GS survives a chunk unload (CREATIVE)

| prediction | READING |
|---|---|
| `/rpg mobtrace` ON. `/rpg spawn zombie 222`, fly several hundred blocks away and come back. Still **`[222]`**, and the log shows **`MOBREMOVE <uuid>`**, then **`MOBSEED <uuid> ... gs=222 source=stored`** for the same uuid | |

### G14 — a mob through a Nether portal keeps its GS (SURVIVAL)

| prediction | READING |
|---|---|
| `/rpg spawn zombie 140`, and get it to walk or be pushed through a Nether portal. In the Nether it still reads **`[140]`**, while `mobinfo`'s "would roll" there reads at least 200. Full HP on arrival is §6 F1, and expected | |

### G15 — a stored GS of 0 is re-rolled (CREATIVE)

| prediction | READING |
|---|---|
| `/summon zombie ~ ~ ~ {BukkitValues:{"rpg:mob_gear_score":0}}` near spawn: re-rolled from position (~[100]), and the log has a WARN naming the invalid `mob_gear_score` | |

### G15b — a stored GS of 900 is re-rolled, not clamped (CREATIVE)

| prediction | READING |
|---|---|
| The same with `900`: ~[100], NOT [900] and NOT [500], with a WARN | |

### G16 — conversions inherit the GS (CREATIVE)

| prediction | READING |
|---|---|
| `/rpg spawn slime 300` (a large one) and kill it: **the children read `[300]`**. And/or a GS 300 zombie drowned until it converts: the drowned reads **`[300]`** | |

### G11 — environmental damage on a mob is proportional (M13) (CREATIVE)

| prediction | READING |
|---|---|
| Drop a `/rpg spawn zombie` and a `/rpg spawn zombie 500` from ~25 blocks: **both die** | |

### G18 — the Knell in lava: proportional (M14) (CREATIVE)

| prediction | READING |
|---|---|
| `/rpg spawn knell` and `/rpg spawn knell 200` (720 HP) in lava for the same few seconds lose **about the same FRACTION** of their bar, read with `mobinfo` before and after | |

### GX-ENV — a player's environmental damage did NOT change (SURVIVAL)

| prediction | READING |
|---|---|
| At max 400 (the healthboost held), fall far enough to take damage. **No `MOBHIT` line**, and the loss is a share of max as on master: a fall of vanilla amount `a` takes `a x 400/20 = 20a`. This is the other half of `RerouteDamagePrice`: only a mob-sourced hit leaves `DamageScale` | |

---

## Findings this gate carries (named, not fixed)

- **F6, SETTLED BY MEASUREMENT: melee is difficulty-blind; every other path is difficulty-adjusted.** `javap` on the
  pinned server jar: `Player.hurtServer` scales `amount` by difficulty (easy `min(a/2 + 1, a)`, hard `a x 3/2`) when
  `scalesWithDifficulty()`, then calls `LivingEntity.hurtServer`, which raises the Bukkit event. So Route B's
  `getDamage()` is post-difficulty, while Route A prices from the seeded attribute and never sees difficulty. The
  server runs `easy`. **This slice changes neither route**: making them agree is a ruling (which one is right?), not
  a repricing.
- **ABSENT is unreachable from shipped play.** Every hostile mob is seeded at world-add, and the seed stores a score,
  so a hostile mob with no score has to be untracked when it hits. The arm is guarded by
  `MobDamagePricingTest.anAbsentGsIsFiveTimesNeverOneAndNeverZero` and the absent-arm mutation, and **no row here
  can reach it**. Its WARN line has no gate witness.
- **A projectile fired before this build, or by an unseeded shooter, carries no stamp.** If its shooter is gone by
  impact, the hit is not mob-sourced and prices by share-of-max, with no MOBHIT line. The fangs of an evoker that
  dies mid-cast behave the same (fangs are never stamped).
- **The causing mob's PDC is read on the event's thread.** That is fine on Paper. On Folia a causing mob in another
  region needs the deferred `MobGearScores` map (PLAN §3 slice 2, resolution step 2).
