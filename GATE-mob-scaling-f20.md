# GATE — Mob scaling F20: the vanilla mirror (M26)

**Status: READ 2026-09-28 on `002352cb` -- 9 of 34 PASS: R0a-c, NO-RECURSION, F20b-HEAL, NEVER-KILLED, F20a-HEAL,
M26-WITHER and M26-GOLEM** (the last two with Ben's observations named: "the bar dropped and the aura showed", "cracks
went light, medium, heavy, repair sound played").
- **The boot:** it began at 05:53:13 (`Build: 002352cb`). Ben's LOGIN is at 05:54:19, with no armour: `equipment {}`
  at 05:54:27, beside the `Health 20.0f` control, in creative (`playerGameType 1`).
- **Every console Health read followed a control read of the same mob**, and every predicted value was executed in
  Java.
- **Instrument gaps, stated:**
  - F20b's final `16.0f` was not read: the spider despawned.
  - F20a's `166.66667f` was not read: the crystal healed within a second. The read taken, `182.66667f`, is the mirror
    of the store at that moment.
  - M26-GOLEM's single-ingot `45.0f` was not read: Ben repaired it to full in one burst.
  - M26-WITHER's step 2 was a weapon (the Dragon's Breath) instead of `/rpg mobdamage 1500`. Its step 3 (the aura going
    away again) was not read: it was killed first.
- **Staging traps met this boot (§6 F23):**
  - A `/rpg spawn` mob is NOT persistent, and vanilla despawned the F20b spider mid-row. The NEVER-KILLED spider was made
    persistent by console first.
  - Two dragons summoned together damage each other.
  - A `/rpg mobdamage` that logs no re-seed line in its second has missed (four did).
  - This file's M26-WITHER text wrongly said its REGEN logs only below full; that source is ungated.
- **Carried forward, NOT READ:** M25-SPAWN, G10, G8b, GX-ENV, G13, G12, G14, G15, G15b, G16, G18 (the fall) and the
  14 probe rows. Ben ended the session ("wrap it up").
- **Stop:** through the console file at 06:07:33, clean, with zero `java.exe` afterwards.

(Before the boot, this line read "**Status: NOT RUN.**") Every prediction below was written **before** any boot of this branch, and **this file is
COMMITTED before the boot**, so a prediction can be checked against origin for having existed first. Readings go
**beside** a prediction, never over it, and a prediction is not edited once its row has been read. **Readings count
only if Ben's LOGIN appears in THIS boot's log.** The seat's 2026-09-28 ruling applies: a row whose prediction is a
LOG LINE is PASS when that line appears verbatim after the LOGIN, named by Ben or not. "Not itemised" is only for
rows that rest on Ben's own observation.

```
ROWS     34   R0a R0b R0c
              NO-RECURSION F20b-HEAL NEVER-KILLED F20a-HEAL M26-WITHER M26-GOLEM
              M25-SPAWN G10 G8b GX-ENV G13 G12 G14 G15 G15b G16 G18
              P-ARROW P-TRIDENT P-BLAZE P-GHAST P-SKULL P-SPIT P-CREEPER P-WITCH P-BOOM P-FANGS P-GUARDIAN
              P-BREATH P-WIND P-DOT
         ──
         34   = 20 headings   git grep -c '^### R0\|^### M25-\|^### G\|^### F20\|^### NO-\|^### NEVER-\|^### M26-' <ref> -- GATE-mob-scaling-f20.md
              + 14 probe rows git grep -c '^| \*\*P-' <ref> -- GATE-mob-scaling-f20.md
```

**Plan:** `PLAN-mob-scaling.md` §7 (F20, the mirror), §0 **M26** (every health-keyed vanilla behaviour comes back) and
**M27** (the perched dragon is deferred, §6 F22).

## Set-up

- **Every row declares its game mode and its witness.** The mirror rows are CREATIVE: they read a mob's health, and a
  creative player is neither damaged nor targeted. The carried rows keep their own modes.
- **`/rpg mobtrace` first.** It turns on `MOBSEED`, `MOBREMOVE`, `MOBHIT` and `MOBHEAL`.
- **`/time set day`, and stand somewhere enclosed.** On slice 3 a natural skeleton hit the first spider and spoilt a
  row. **Do not use `/difficulty peaceful`** to clear mobs: it also removes the spiders and the Wither these rows
  spawn.
- **`/rpg mobdamage <n>` is the ability path.** It calls `BukkitCombatant.applyDamage`, "the same entry point abilities
  use" (§6 F20's table), so a row that uses it reads what a ray, projectile or ability hit does.

### THE CONSOLE `Health` READ, AND ITS CONTROL

Session B reads a mob's vanilla health with `data get entity <uuid> Health`, the uuid taken from its `MOBSEED` line.
The reply lands in the log as `<Type> has the following entity data: <value>f`. **Every Health prediction below is
preceded by a CONTROL read of the same mob BEFORE the action.** The control must equal the vanilla max, which the
seed's mirror writes (a fresh store is full). If the control is not the max, the mob was touched before the row, and
the row is void.

**The values were computed by executing the mirror expression in Java, not predicted by reasoning:**
`clamp(current / customMax × vanillaMax, min(vanillaMax, 1.0), vanillaMax)`, printed as a float.

### WHAT A MOB'S VANILLA HEALTH NOW DOES — read before the rows

- **It follows the store** within a scheduler hop of every write: every damage path, every heal, and the seed.
- **At full it equals the vanilla max exactly**, so the Regeneration and crystal heals STOP (F18 is fixed). A
  `MOBHEAL` line with `before = after = max` is now a FAIL.
- **A mob hurt only by our own damage now heals** (F20 is fixed): no punch is needed.

---

## R0 — THE DEPLOYED BUILD CARRIES THIS BRANCH. IF ANY R0 FAILS, STOP

### R0a — the build line names the head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, the PR's head as `gh pr view <n> --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS.** `[05:53:17] [Server thread/INFO]: [Rpg] Build: 002352cb`, a bare hash, equal to the pushed head (`002352cba2a7…` on `git ls-remote` and locally before the build) |

### R0b — the jar carries the mirror

| prediction | instrument | READING |
|---|---|---|
| PRESENT: core `VanillaMirror`, paper `MobVanillaMirror`, and slice 3's `MobHealPolicy`. ABSENT: the control. Both mirror classes are new in this slice, so slice 3's jar reads ABSENT for them | the scan below | **PASS.** 810 entries: `VanillaMirror`, `MobVanillaMirror` and `MobHealPolicy` PRESENT; the control ABSENT |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/combat/stat/VanillaMirror.class',
               'io/github/butterflysmp/rpg/paper/health/MobVanillaMirror.class',
               'io/github/butterflysmp/rpg/paper/health/MobHealPolicy.class',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — the boot log: content loads, nothing refused

| prediction | instrument | READING |
|---|---|---|
| `Loaded 8 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 2 pools, 6 fragments, 4 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`, as read on `8d7e0c90`, because this branch changes no content. **No** `Refusing`, `Skipping`, `SEVERE` or exception. The known WARNs are `volley_stone`'s 27-tick cooldown and its summary line | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | **PASS.** `[05:53:17] [Server thread/INFO]: [Rpg] Loaded 8 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 2 pools, 6 fragments, 4 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. No `Refusing`, `Skipping`, `SEVERE` or exception |

---

## THE MIRROR ROWS (CREATIVE)

**NO-RECURSION and F20b-HEAL are read on ONE spider, in that order.** The first half proves the write raises no event;
the second applies the heal.

### NO-RECURSION — the mirror's write raises no event (CREATIVE) — witness: the log, plus a controlled Health read

| prediction | READING |
|---|---|
| `/rpg spawn spider 300`: **`MOBSEED <uuid> spider gs=300 source=stored max=240`**. Console CONTROL: **`Spider has the following entity data: 16.0f`**. Look at it and `/rpg mobdamage 100` (never punch it). Console again: **`9.333333f`** (140 / 240 × 16), which is **the mirror's write, witnessed**. For the next **10 seconds**: **zero `MOBHEAL` and zero `MOBHIT` lines name its uuid**, and there is **no exception, `StackOverflowError` or `SEVERE`** in the log. `setHealth` raised no regain or damage event; a recursion would show a line here or a stack overflow | **PASS.** READ 2026-09-28 on `002352cb` (Ben's LOGIN 05:54:19). `[05:55:45] [Rpg] MOBSEED 57be978a-e8f2-491d-aeaa-1cd718951716 spider gs=300 source=stored max=240`. CONTROL: `[05:55:53] Spider has the following entity data: 16.0f`. Ben's first `/rpg mobdamage 100` (05:56:10) missed: no re-seed line, so it hit nothing. The second (05:56:12) hit (the re-seed line in that second). Then **`[05:56:20] Spider has the following entity data: 9.333333f`**, the mirror's write, witnessed. From 05:56:12 until the log passed 05:56:22: **zero `MOBHEAL`, `MOBHIT`, `Exception`, `StackOverflow` or `SEVERE` lines** (Grep count 0). `setHealth` raised no event |

### F20b-HEAL — the same spider heals under Regeneration with NO punch, and stops at full (CREATIVE) — witness: MOBHEAL lines, plus Health reads

| prediction | READING |
|---|---|
| On the NO-RECURSION spider, still untouched: `/effect give @e[type=spider,limit=1,sort=nearest] minecraft:regeneration 60 1`. **The heals begin with no punch** (on slice 3 this spider logged nothing for 20 s). **Exactly seven lines, one per heal period (about 25 ticks), never two in one tick:** `MOBHEAL <uuid> spider reason=MAGIC_REGEN vanilla=1.000 custom=15.000 ratio=15.000 before=140.000 after=155.000 max=240.0 vanillaMax=16.0`, then `after=` 170, 185, 200, 215, 230, and **`before=230.000 after=240.000`**. **Then NO further line for that uuid** for as long as the effect runs: the gate closed at full, so F18 is fixed. A `before=240.000 after=240.000` line is a FAIL. Console, after the last line: **`16.0f`**. *The line shape and ratio are slice 3's G17, read here as its regression* | **PASS on its MOBHEAL lines. The final Health read was NOT TAKEN: the spider despawned.** READ 2026-09-28 on `002352cb`. `/effect give @e[type=spider,limit=1,sort=nearest] minecraft:regeneration 60 1` at 05:57:02, **with no punch**. VERBATIM, the first heal, in the same second: `[05:57:02] [Server thread/INFO]: [Rpg] MOBHEAL 57be978a-e8f2-491d-aeaa-1cd718951716 spider reason=MAGIC_REGEN vanilla=1.000 custom=15.000 ratio=15.000 before=140.000 after=155.000 max=240.0 vanillaMax=16.0`. On slice 3 this logged nothing for 20 s. **Exactly seven lines** (Grep count 7): 155, 170, 185, 200, 215, 230, and `before=230.000 after=240.000` at 05:57:09, one per second, never two in one tick. **Then no eighth line** for the 7 s until the spider was removed at 05:57:16 (`MOBREMOVE 57be978a…`, no command and no death). That is vanilla's random despawn of a non-persistent mob. So no `before = after = max` line appeared, and F18 is fixed. The `16.0f` read found "No entity was found" (05:57:29). *The NEVER-KILLED spider was made persistent by console after this* (`data merge … {PersistenceRequired:1b}`) |

### NEVER-KILLED — the mirror floors a live mob above 0, and death stays ours (CREATIVE) — witness: Health reads and MOBREMOVE

| prediction | READING |
|---|---|
| A FRESH `/rpg spawn spider 300` (`MOBSEED … spider gs=300 source=stored max=240`). Console CONTROL: `16.0f`. Look at it and `/rpg mobdamage 239`: the store is 1. Console: **`1.0f`**, the floor, not `0.06666667` (1 / 240 × 16). **Alive**: a second console read **10 seconds later still answers `1.0f`** for that uuid. Then `/rpg mobdamage 1`: the store is 0 and **it dies ONCE**. **One `MOBREMOVE <uuid>` line**, the death drops, and a console read afterwards finds no entity. **No revive**: the mirror skips the killing change (`reachedZero`). *Changed from §7.8's first draft: that draft punched the spider at store 1, but a punch also deals the player's custom damage (at least the 1 HP left), so it would kill through our store. That is death being ours, not the floor being tested* | **PASS.** READ 2026-09-28 on `002352cb`. `[05:58:19] MOBSEED 35cdb26c-4b05-4126-a685-d7d31b7cffd6 spider gs=300 source=stored max=240`, made persistent (`[05:58:27] Modified entity data of Spider`). CONTROL: `[05:58:27] Spider has the following entity data: 16.0f`. `/rpg mobdamage 239` at 05:58:55 (re-seed 05:58:56), then **`[05:59:04] Spider has the following entity data: 1.0f`**: the floor, not `0.06666667`. **Still alive, and still `1.0f`, at 05:59:23.** `/rpg mobdamage 1` at 05:59:34: **one `[05:59:35] MOBREMOVE 35cdb26c…`**, then `[05:59:44] No entity was found`. There was no revive and no heal line. **Ben: it dropped string** (the death's loot) |

### STAGING THE DRAGON (as on slice 3: this world's own dragon is dead)

1. `/execute in minecraft:the_end run tp @s 0 100 40`, then fly up a few blocks.
2. `/summon ender_dragon ~ ~8 ~15` **ONCE** (slice 3 summoned two by accident). **Do NOT add `NoAI`**: its hit parts
   would never move, so it could not be aimed at.
3. `/summon end_crystal ~ ~ ~5`.

A summoned dragon has no boss bar: `EnderDragonFight.updateDragon` updates only the fight's own dragon, and that
dragon is dead. So the boss bar half of M26 is read on the Wither.

### F20a-HEAL — the crystal heals a dragon hurt only by our damage, with NO punch, and stops at full (CREATIVE) — witness: MOBHEAL lines, plus Health reads

| prediction | READING |
|---|---|
| **`MOBSEED <uuid> ender_dragon gs=300 source=rolled max=3000`**. Console CONTROL: **`Ender Dragon has the following entity data: 200.0f`**. Look at its middle and `/rpg mobdamage 500` (never punch it). Console: **`166.66667f`** (2500 / 3000 × 200). **The crystal heals it within about a second, with no punch** (on slice 3 it logged nothing for 14 s): `MOBHEAL <uuid> ender_dragon reason=ENDER_CRYSTAL vanilla=1.000 custom=15.000 ratio=15.000 before=2500.000 after=2515.000 max=3000.0 vanillaMax=200.0`, two a second, **34 lines to `before=2995.000 after=3000.000`**. **Then NO further line** while the crystal stands (a capped line is a FAIL; on slice 3 they continued, F18). Console: **`200.0f`**. *The line shape is slice 3's G20, read here as its regression.* Afterwards, `/kill @e[type=ender_dragon]`, so it plays no part in M26-WITHER | **PASS.** READ 2026-09-28 on `002352cb`. **Staging incidents, stated:** <br>- The slice 3 dragon `88fc8a16` was still alive and re-seeded as Ben arrived: `[06:00:21] MOBSEED 88fc8a16… ender_dragon gs=300 source=stored max=3000`. **A bonus witness of the seed's mirror:** slice 3 left its vanilla health at about 199.99, because its cancelled heals never raised it. It read `[06:00:35] Ender Dragon has the following entity data: 200.0f`. <br>- Ben had also summoned `b35d3bfb` (06:00:28). The two dragons hurt each other, since a dragon's head damages what it touches, so `88fc8a16`'s store dipped between heals. Its heals are therefore NOT this row's reading, though it too stopped logging at full (3000, 06:00:49). <br>- Ben killed both (06:01:05). <br>**The read dragon:** `[06:01:10] MOBSEED 46db9044-9bf2-4bd8-b1fc-6560a01bbe39 ender_dragon gs=300 source=rolled max=3000`. CONTROL: `[06:01:24] Ender Dragon has the following entity data: 200.0f`. `/rpg mobdamage 500` at 06:01:49 (re-seed that second), **never punched**. VERBATIM, the first heal, **within a second**: `[06:01:50] [Server thread/INFO]: [Rpg] MOBHEAL 46db9044-9bf2-4bd8-b1fc-6560a01bbe39 ender_dragon reason=ENDER_CRYSTAL vanilla=1.000 custom=15.000 ratio=15.000 before=2500.000 after=2515.000 max=3000.0 vanillaMax=200.0`. **34 lines** (Grep count 34), two a second, to `before=2995.000 after=3000.000` at 06:02:06. **Zero capped lines** (count 0), and none in the 21 s after. **`[06:02:27] Ender Dragon has the following entity data: 200.0f`.** *Instrument note:* the predicted `166.66667f` was NOT read, because the crystal healed before the console read. The read taken, `[06:01:57] … 182.66667f`, is the mirror of the preceding line's `after=2740` (2740 / 3000 × 200, executed in Java: `182.66667f`) |

### M26-WITHER — the Wither's boss bar follows an ability hit, and it arms at half the CUSTOM bar (CREATIVE) — witness: Health reads, plus Ben's observation

Stage it in the End, well away from the dragon's spot: `/execute in minecraft:the_end run tp @s 200 100 0`. The
Wither shoots skulls at other mobs; a creative player is not its target.

| prediction | READING |
|---|---|
| `/rpg spawn wither 300`: **`MOBSEED <uuid> wither gs=300 source=stored max=4500`** (300 × 5 × 3). Console CONTROL: **`Wither has the following entity data: 300.0f`**. **The Wither heals itself 1.0 every 20 ticks** (`WitherBoss.customServerAiStep`, REGEN, ungated; read from the jar). Under slice 3 that is `MOBHEAL … reason=REGEN vanilla=1.000 custom=15.000 ratio=15.000`, once a second while it is below full. **So every Health read below is predicted as `after / 4500 × 300` of the LAST `MOBHEAL` line for that uuid before the read**, or of the mobdamage result if none. **Step 1:** `/rpg mobdamage 1000`. Store 3500, so Health **`233.33333f`** if no heal has landed yet. **Ben: the boss bar drops to about 78%, and there is NO armour aura** (it is above half). **Step 2:** `/rpg mobdamage 1500`. Store about 2000 (plus any heals since), so Health about `133.33333f`, below `getMaxHealth() / 2` = 150 (`WitherBoss.isPowered`, read from the jar). **Ben: the boss bar drops to about 44%, and the BLUE ARMOUR AURA appears.** **Step 3, vanilla's own behaviour:** at +15 a second the store passes 2250 (half) after roughly 17 seconds; when a `MOBHEAL` `after=` passes 2250, **the aura goes away again**. Before this slice the Wither never armoured (vanilla health sat at about full). Afterwards, `/kill @e[type=wither]` | **PASS as reported by Ben, with the Health reads PASS on the log.** READ 2026-09-28 on `002352cb`. `[06:03:14] MOBSEED a20f9c79-9cb8-4393-b422-7b7bdbd96293 wither gs=300 source=stored max=4500`. CONTROL: `[06:03:23] Wither has the following entity data: 300.0f`. **A prediction miss in the staging text, not in the code:** this row said the Wither heals "once a second while it is below full". Its REGEN is UNGATED (as the row itself read from the jar), so it also logs `before=4500.000 after=4500.000` once a second AT full. That is expected for an ungated source, and it is not F18. **Step 1:** `/rpg mobdamage 1000` at 06:03:47 missed (no re-seed); at 06:03:50 it hit, and the store went 4500 to 3500 (the next heal line reads `before=3500.000`). **`[06:03:56] Wither has the following entity data: 239.33333f`** = the mirror of the preceding line's `after=3590` (executed in Java). **Step 2 was done with a WEAPON, not `/rpg mobdamage 1500`:** Ben attacked with the **Dragon's Breath**, a weapon whose hits land through `applyDamage`, the path F20 is about. The store fell 3725, 3414.5, 2679, 2398.6, and below half (2250) by 06:04:10 (`before=2169.875`), then on down. **`[06:04:20] Wither has the following entity data: 41.808334f`** = the mirror of `after=627.125` (executed in Java: `41.808334f`). It was killed (`[06:04:25] MOBREMOVE a20f9c79…`). **Ben: "the bar dropped and the aura showed".** Step 3 (the aura going away as it regenerated past half) was NOT READ: it was killed first |

### M26-GOLEM — an iron golem's cracks follow the custom bar, and an iron ingot repairs it proportionally (CREATIVE) — witness: Health reads and a MOBHEAL line, plus Ben's observation

| prediction | READING |
|---|---|
| In the overworld: `/rpg spawn iron_golem`: **`MOBSEED <uuid> iron_golem gs=- source=passive max=500`** (passive: not an `Enemy`; 100 × 5). Console CONTROL: **`Iron Golem has the following entity data: 100.0f`**. The cracks are `Crackiness.GOLEM` (read from the jar): **HIGH below 0.25, MEDIUM below 0.5, LOW below 0.75**. Then: `/rpg mobdamage 150` gives **`70.0f`, and LOW cracks appear**; `/rpg mobdamage 150` gives **`40.0f`, MEDIUM**; `/rpg mobdamage 100` gives **`20.0f`, HIGH**. **Repair:** right-click it with an iron ingot. `IronGolem.mobInteract` heals 25 with reason CUSTOM, which MobHealPolicy REROUTEs: **`MOBHEAL <uuid> iron_golem reason=CUSTOM vanilla=25.000 custom=125.000 ratio=5.000 before=100.000 after=225.000 max=500.0 vanillaMax=100.0`**. Console: **`45.0f`, and the cracks go back to MEDIUM**. Ben hears the repair sound: vanilla plays it only when health changed, which it did not before this slice (the cancelled heal left vanilla health unmoved). The golem turns on Ben after `/rpg mobdamage` (aggro on hit); a creative player takes nothing | **PASS, with the cracks and the sound as reported by Ben.** READ 2026-09-28 on `002352cb`. `[06:05:18] MOBSEED 537080c3-6614-47b1-bc19-f0cfb3cc1822 iron_golem gs=- source=passive max=500`. CONTROL: `[06:05:25] Iron Golem has the following entity data: 100.0f`. `/rpg mobdamage 150` gave **`[06:05:50] … 70.0f`**; again, **`[06:06:18] … 40.0f`**; `/rpg mobdamage 100` (a miss at 06:06:27, then a hit at 06:06:28) gave **`[06:06:38] … 20.0f`**. **Ben: "cracks went light, medium, heavy"** (LOW, MEDIUM, HIGH). **Iron ingot:** VERBATIM, the first repair: `[06:06:50] [Server thread/INFO]: [Rpg] MOBHEAL 537080c3-6614-47b1-bc19-f0cfb3cc1822 iron_golem reason=CUSTOM vanilla=25.000 custom=125.000 ratio=5.000 before=100.000 after=225.000 max=500.0 vanillaMax=100.0`. Ben kept clicking: 225 to 350, 350 to 475, and 475 to 500, then a capped `before=500.000 after=500.000` at 06:06:52. That is expected: the golem's repair heal is ungated in the jar (heal first, then compare). So the predicted single-ingot `45.0f` was NOT read. The read taken, `[06:07:00] … 100.0f`, is the mirror of the full store. **Ben: "repair sound played".** Vanilla plays it only when health changed, which it could not before this slice |

---

## CARRIED FORWARD — EVERY STILL-UNREAD ROW FROM `GATE-mob-scaling-3.md`, WITH ITS F20 VERDICT

Unread on `GATE-mob-scaling-3.md` (merged as `6dd1eeb9`). **The mirror writes only a tracked MOB's vanilla health,
and none of these rows reads it, so each is CARRIED with its prediction verbatim**, except G18, which F21 rewrites.
Players are excluded from the mirror, so GX-ENV and the probes (mob-to-player hits) are untouched.

### M25-SPAWN — `/rpg spawn` with no [gs] reads the new curve (SURVIVAL) — CARRIED, both halves

| prediction | READING |
|---|---|
| At the M25-OW position (`/tp @s 10000 ~ 10000`), `/rpg spawn zombie` (no score): **`MOBSEED … zombie gs=100 source=rolled`**. At the M25-END-11K teleport point (`/execute in minecraft:the_end run tp @s 7778 ~ 7778`, don't move after the `/tp`), `/rpg spawn enderman`: **`MOBSEED … enderman gs=400 source=rolled`**. **Match each by UUID** (the standing tip): on M25 three enderman seeds shared the spawn's second and the End half could not be attributed. Pre-M25: 500 and 500 | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |

### G10 — the shooter dies mid-flight; the arrow keeps its stamp (SURVIVAL) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn skeleton 300` at range (15+ blocks). The moment it looses, `/kill @e[type=skeleton,limit=1,sort=nearest]`. The arrow lands: **`MOBHIT … direct=arrow causing=none gs=300 from=DIRECT ratio=15.000`**. Skipped by Ben on `ad686c9` (the kill could not be timed); covered by `MobDamagePricingTest.aShooterThatIsGoneStillPricesAtItsStamp` | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |

### G8b — melee at GS 300 is x15 (SURVIVAL) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn zombie 300`, one hit at max 100: **`MOBHIT cause=ENTITY_ATTACK … gs=300 … ratio=15.000`** | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |

### GX-ENV — a player's environmental damage did NOT change (SURVIVAL) — CARRIED

| prediction | READING |
|---|---|
| At max 400 (`/rpg healthboost 300`, held), fall far enough to take damage. **No `MOBHIT` line**, and the loss is a share of max: a fall of vanilla amount `a` takes `a x 400/20` | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |

### G13 — the GS survives a restart (437) (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| Near spawn, `/rpg spawn zombie 437`, plate `[437]`. Session B restarts through the console file (no `--refresh-content`). Rejoin: still **`[437]`**, and the `MOBSEED` for its uuid after the restart reads **`gs=437 source=stored`** | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |

### G12 — the GS survives a chunk unload (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn zombie 222`, fly several hundred blocks away and come back. Still **`[222]`**, with **`MOBREMOVE <uuid>`** then **`MOBSEED <uuid> … gs=222 source=stored`** in the log | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |

### G14 — a mob through a Nether portal keeps its GS (SURVIVAL) — CARRIED as rewritten under M25

| prediction | READING |
|---|---|
| `/rpg spawn zombie 140`, and get it through a Nether portal. In the Nether it still reads **`[140]`** (`MOBSEED … gs=140 source=stored` on arrival), while `mobinfo`'s "would roll" there reads **exactly 200**. Full HP on arrival is §6 F1, and expected | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |

### G15 — a stored GS of 0 is re-rolled (CREATIVE) — CARRIED as rewritten under M25

| prediction | READING |
|---|---|
| `/summon zombie ~ ~ ~ {BukkitValues:{"rpg:mob_gear_score":0}}` anywhere in the overworld: a WARN naming the invalid `mob_gear_score`, then **`MOBSEED … gs=100 source=rolled`**, exactly 100 | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |

### G15b — a stored GS of 900 is re-rolled, not clamped (CREATIVE) — CARRIED as rewritten under M25

| prediction | READING |
|---|---|
| The same with `900`: a WARN, then **`gs=100 source=rolled`**, NOT 900 and NOT 500 | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |

### G16 — conversions inherit the GS (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn slime 300`. **Re-spawn until it is size 4**: `execute at BaronVonYeetus run data get entity @e[type=slime,limit=1,sort=nearest] Size` prints **`3`**, after a `Health` read as the control. Kill it: **each child's `MOBSEED` reads `gs=300 source=stored`**. And/or a GS 300 zombie drowned until it converts: the drowned's `MOBSEED` reads `gs=300 source=stored` | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |

### G18 — the Knell by FALL: proportional (M14) (CREATIVE) — REWRITTEN (F21), and now log-witnessed by the mirror

| prediction | READING |
|---|---|
| *Rewritten: slice 3's lava staging was hollow, because the Knell is a `wither_skeleton`, immune to lava.* `/rpg spawn knell` (**`MOBSEED … wither_skeleton gs=100 source=rolled max=360`**) and `/rpg spawn knell 200` (**`… gs=200 source=stored max=720`**). Console CONTROL for each: **`<its display name> has the following entity data: 20.0f`**. Drop both from the **same height, about 15 blocks** (build a pillar, or spawn them standing on it, and push them off; well under the ~23 blocks that would kill a 20 HP vanilla mob). **Each loses the same FRACTION of its bar (M14)**, and the mirror now makes that a number: **the two Knells' console `Health` reads, taken after they land, are EQUAL** to within the difference between their two fall heights (each block of fall is 1.0 of vanilla damage, so the reads differ by at most the whole blocks between their drops). Both are well above `1.0f`. `/rpg mobinfo` on each (chat) should read the same fraction of 360 and 720 | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |

### THE PROBE ROWS — CARRIED, every one with an explicit [gs] (SURVIVAL)

Each row: spawn at the named GS, take the hit at max 100, and read the `MOBHIT` line. Predictions as in
`GATE-mob-scaling-2.md` and `GATE-mob-scaling-m25.md`, verbatim. The mirror never touches a player.

| row | set-up (GS) | predicted `direct` / `causing` / `from` | predicted `ratio` | READING |
|---|---|---|---|---|
| **P-ARROW** | `skeleton 200` (spawns with its bow since F16c) | `arrow` / `skeleton` / `DIRECT` | **10.000** | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-TRIDENT** | `drowned 300`, re-spawned until one holds a trident | `trident` / `drowned` / `DIRECT` | **15.000** | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-BLAZE** | `blaze 300` | `small_fireball` / `blaze` / `DIRECT` | **15.000** | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-GHAST** | `ghast 300` | `fireball` / `ghast` / `DIRECT`; the blast is a second line from the same stamp | **15.000** each | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-SKULL** | `wither 300` | `wither_skull` / `wither` / `DIRECT` | **15.000** | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-SPIT** | an angered llama (passive) | `llama_spit` / `llama` / `CAUSING`, `gs=-` | **5.000** | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-CREEPER** | `creeper 200` | `creeper` / `creeper` / `DIRECT` | **10.000** | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-WITCH** | `witch 300`, a harming splash | the potion / `witch` / `DIRECT` | **15.000** | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-BOOM** | `warden 300`, sonic boom | `warden` / `warden` / `DIRECT` | **15.000** | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-FANGS** | `evoker 300`, fangs | `evoker_fangs` / `evoker` / **`CAUSING`** | **15.000** | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-GUARDIAN** | `guardian 300`, beam | `guardian` / `guardian` / `DIRECT` | **15.000** | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-BREATH** | the dragon's breath cloud, if reachable | **UNPREDICTED**: `CAUSING` from the dragon if the cloud names its owner, else no MOBHIT (a named finding) | — | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-WIND** | `breeze 300`, wind charge | `breeze_wind_charge` / `breeze` / `DIRECT` | **15.000** | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-DOT** | a cave spider's poison | **NO `MOBHIT` line for the poison ticks** (F5) | — | **NOT READ on `002352cb`**: not attempted. Ben ended the session after M26-GOLEM ("wrap it up"). **CARRIED FORWARD**, not waived. |
