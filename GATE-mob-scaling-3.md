# GATE — Mob scaling slice 3: proportional healing on mobs (M15)

**Status, 2026-09-28: merge approved by the seat 2026-09-28; F20 moves to its own slice; unread rows carried. G18 is
rewritten as a FALL on the Knell (F21), carried forward.**

**Status: READ 2026-09-28 on `8d7e0c90` (docs-only on `eb1c8d34`) -- 10 of 34 PASS: R0a-c, F20b, G17, G17-UNDEAD, F20a,
G20, and G11 as Ben reported it.**
- **The boot:** it began at 04:58:43 (`Build: 8d7e0c90`). Ben's LOGIN is at 05:01:05, with no armour: `equipment {}`
  at 05:01:14, beside the `Health 20.0f` control, in creative (`playerGameType 1`).
- **G18 was SKIPPED by Ben, and the row is HOLLOW.** The Knell is a `wither_skeleton`, which is immune to lava in
  vanilla, so this staging can never read M14 (§6 F21).
- **Carried forward, NOT READ:** M25-SPAWN, G10, G8b, GX-ENV, G13, G12, G14, G15, G15b, G16 and the 14 probe rows. Ben
  ended the session ("skip the rest, wrap it up").
- **Instrument gaps:**
  - F20a and F20b pass on their log lines alone. Their console `Health` reads were not taken on the mobs that were
    read: on the spider because the tool's safety check was down, and on the dragon because Ben stopped the console
    write.
  - G17 and G20 each rest on one unlogged step, Ben's punch, which he confirmed both times.
- **Unplanned observation (not a row):**
  - A natural skeleton horse's own regeneration logged `[05:02:40] MOBHEAL f7bd4e95-… skeleton_horse reason=REGEN
    vanilla=1.000 custom=5.000 ratio=5.000 before=75.000 after=75.000 max=75.0 vanillaMax=15.0`, and a second horse
    did the same at 05:03:09. That is the predicted shape on an unscored mob (×5), capped (F18).
  - **It also shows that vanilla's "below max" gate lives in each heal SOURCE, not in `LivingEntity.heal`.** A horse's
    regen fires at full health, while the Regeneration effect and the crystal gate themselves, which is what F20
    rests on.
- **Stop:** through the console file at 05:14:09, clean, with zero `java.exe` afterwards.

(Before the boot, this line read "**Status: NOT RUN.**") Every prediction below was written **before** any boot of this branch, and **this file is
COMMITTED before the boot**, so a prediction can be checked against origin for having existed first. Readings go
**beside** a prediction, never over it, and a prediction is not edited once its row has been read. **Readings count
only if Ben's LOGIN appears in THIS boot's log.** The seat's 2026-09-28 ruling applies: a row whose prediction is a
LOG LINE is PASS when that line appears verbatim after the LOGIN, named by Ben or not. "Not itemised" is only for
rows that rest on Ben's own observation.

```
ROWS     34   R0a R0b R0c
              F20b G17 G17-UNDEAD F20a G20
              G11 G18
              M25-SPAWN G10 G8b GX-ENV G13 G12 G14 G15 G15b G16
              P-ARROW P-TRIDENT P-BLAZE P-GHAST P-SKULL P-SPIT P-CREEPER P-WITCH P-BOOM P-FANGS P-GUARDIAN
              P-BREATH P-WIND P-DOT
         ──
         34   = 20 headings   git grep -c '^### R0\|^### M25-\|^### G\|^### F20' <ref> -- GATE-mob-scaling-3.md
              + 14 probe rows git grep -c '^| \*\*P-' <ref> -- GATE-mob-scaling-3.md
```

**Plan:** `PLAN-mob-scaling.md` §3 *Slice 3*, §0 **M15** (heals are proportional), and §6 **F17**–**F19**, which
this slice added. **G17 is RE-STAGED from the plan (F17): a zombie cannot take Regeneration at all.**

## Set-up

- **Mode is per row.** The heal rows are CREATIVE: they read a heal ON a mob, and a creative player is neither
  damaged nor targeted, which keeps the mob still while its bar is read. The carried rows keep their own modes.
- **`/rpg mobtrace` first.** Each seed then logs `MOBSEED <uuid> <type> gs=<n> source=<rolled|stored|passive>
  max=<n>`, each mob-to-player hit logs `MOBHIT`, and **each vanilla heal on a tracked mob now logs `MOBHEAL`**.
- **STANDING TIP (the seat, 2026-09-28):** a `/rpg spawn` in a freshly loaded area lands in the same second as
  natural seeds. **Match its `MOBSEED` by UUID** (console `data get entity @e[…,sort=nearest] UUID`, converted to
  the dashed form), **or spawn after the chunks have settled.**

### THE `MOBHEAL` LINE — its shape, predicted

One line per `EntityRegainHealthEvent` on a TRACKED mob, while `/rpg mobtrace` is on:

```
[hh:mm:ss] [Rpg] MOBHEAL <uuid> <type> reason=<RegainReason> vanilla=<a> custom=<c> ratio=<c/a> before=<b> after=<min(max, b+c)> max=<m> vanillaMax=<v>
```

- **`ratio` = `max / vanillaMax`**, to three decimals, on every line (M15: the custom max over the VANILLA
  attribute). For a mob seeded by mob scaling that is **5 × gs / 100**: 15.000 at GS 300.
- **`after` = `min(max, before + custom)`**: the store is written synchronously, before the line is printed.
- **`vanillaMax` is the attribute, never the custom max** (M9). If it ever equals `max`, the ratio is 1 and the
  denominator is wrong.
- **An UNTRACKED mob logs nothing** (the heal passes to vanilla untouched). No `MOBHEAL` line exists on master.

### WHY A HEAL KEEPS FIRING — read before G17 and G20

Every player or environmental hit on a tracked mob sets its vanilla damage to a **0.01 token**, so its vanilla
health sits just below its vanilla max. **Both heals read in this file only fire while vanilla health is below max**
(read from the pinned server jar: `RegenerationMobEffect.applyEffectTick` and `EnderDragon.checkCrystals` both test
`getHealth() < getMaxHealth()`, and both heal **1.0**). **The mob must be HIT ONCE before the heal**, or no event
fires and the row is hollow. And since the reroute cancels the vanilla heal, vanilla health never climbs back, so
**the heals continue after the store is full**, logged with `before = after = max` (§6 F18).

---

## R0 — THE DEPLOYED BUILD CARRIES THIS BRANCH. IF ANY R0 FAILS, STOP

### R0a — the build line names the head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, the PR's head as `gh pr view <n> --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS.** `[04:58:49] [Server thread/INFO]: [Rpg] Build: 8d7e0c90`, a bare hash, equal to the pushed head (`git ls-remote` read `8d7e0c90bc04…` before the build). `8d7e0c90` is a docs-only commit on `eb1c8d34`: the gate's F20a/F20b rows and PLAN F20, committed before this boot |

### R0b — the jar carries slice 3

| prediction | instrument | READING |
|---|---|---|
| PRESENT: paper `MobHealPolicy` and `MobHealPolicy$Action`, core `DamageScale`. ABSENT: the control. `MobHealPolicy` is new in this slice, so the M25 jar reads ABSENT for it | the scan below | **PASS.** 807 entries: `MobHealPolicy`, `MobHealPolicy$Action` and `DamageScale` PRESENT; the control ABSENT |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/paper/health/MobHealPolicy.class',
               'io/github/butterflysmp/rpg/paper/health/MobHealPolicy$Action.class',
               'io/github/butterflysmp/rpg/core/combat/DamageScale.class',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — the boot log: content loads, nothing refused

| prediction | instrument | READING |
|---|---|---|
| `Loaded 8 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 2 pools, 6 fragments, 4 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`, as read on `210f5b8f` (M25), because this branch changes no content. **No** `Refusing`, `Skipping`, `SEVERE` or exception. The known WARNs are `volley_stone`'s 27-tick cooldown and its summary line | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | **PASS.** `[04:58:49] [Server thread/INFO]: [Rpg] Loaded 8 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 2 pools, 6 fragments, 4 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`. No `Refusing`, `Skipping`, `SEVERE` or exception |

---

## THE HEAL ROWS (M15) — READ FROM THE `MOBHEAL` LINE (CREATIVE)

**ADDED BEFORE THE BOOT, 2026-09-28, at the seat's request: F20b and F20a (§6 F20).** Our own damage
(`BukkitCombatant.applyDamage`: every ability and weapon effect, and `/rpg mobdamage`, "the same entry point abilities
use") never touches vanilla health, so a mob hurt ONLY that way keeps its vanilla max and neither heal fires. **Each
F20 row is read FIRST on the same mob as its G row, because the G row's single punch is what lowers vanilla health,
and it cannot be undone.** So on the spider, G17 continues from F20b with the Regeneration already running: its
"hit it once" is the only step left, and the heals start from 140. On the dragon, G20's heals start from 2500 and
climb 15 a line to 3000 (34 lines, about 17 seconds), then continue at `before = after = max` (F18). Console reads use `execute at BaronVonYeetus run data get entity
@e[type=<type>,limit=1,sort=nearest] Health`, whose reply lands in the log.

### F20b — a spider hurt ONLY by `/rpg mobdamage` takes no Regeneration heal (CREATIVE) — before G17, same spider

| prediction | READING |
|---|---|
| `/rpg spawn spider 300` (plate `[300] Spider 240/240`). Console: its `Health` reads **`16.0f`** (the control: the vanilla max). Look at it and `/rpg mobdamage 100`: chat `SPIDER damaged: 140/240 custom HP`, the plate drops to 140. Console again: **`Health` still `16.0f`**. Then `/effect give @e[type=spider,limit=1,sort=nearest] minecraft:regeneration 60 1` and wait **10 seconds** (8 heal periods of 25 ticks): **NO `MOBHEAL` line names its uuid**, and the plate stays at 140. **A `MOBHEAL` line here, or a Health below 16.0f, refutes F20** | **PASS on the log line (its absence), with the Health controls NOT TAKEN on the spider read.** READ 2026-09-28 on `8d7e0c90` (Ben's LOGIN 05:01:05). **The first spider was spoilt and replaced:** `41bd828d` read `Health 16.0f` at 05:01:57, one second after its `/rpg mobdamage 100` (05:01:56), then `15.99f` at 05:02:09 with no command in between. That is one 0.01 token from an unlogged hit (a punch, a natural skeleton's arrow or a fall; it was night, with skeletons seeding round Ben). So it no longer met the row's condition. **The read spider:** Ben cleared hostiles with `/difficulty peaceful` then `/difficulty hard` (05:03:16-17), then `[05:03:21] [Rpg] MOBSEED 6cfe6aaf-00fb-4e0e-b923-f357809f1fd9 spider gs=300 source=stored max=240`. He ran `/rpg mobdamage 100` at 05:03:23 and `/effect give @e[type=spider,limit=1,sort=nearest] minecraft:regeneration 60 1` at 05:04:26. **No `MOBHEAL` line named `6cfe6aaf` for 20 seconds** (05:04:26 to 05:04:46; 16 heal periods, against the 10 s predicted), until Ben's punch (G17). Its console Health reads were NOT taken: the tool's safety check was down for that minute, so the first spider's `16.0f` is the supporting control, not this row's |

### G17 — Regeneration on a damaged GS 300 spider heals x15 (CREATIVE) — RE-STAGED from a zombie (F17)

| prediction | READING |
|---|---|
| `/rpg spawn spider 300` (plate `[300] Spider 240/240`: 16 × 5 × 3). **Hit it once** with an empty hand so its vanilla health drops below 16 (see *WHY A HEAL KEEPS FIRING*). Then `/effect give @e[type=spider,limit=1,sort=nearest] minecraft:regeneration 30 1` (level II heals every 25 ticks). **Every heal logs `MOBHEAL <its uuid> spider reason=MAGIC_REGEN vanilla=1.000 custom=15.000 ratio=15.000 before=<b> after=<min(240, b+15)> max=240.0 vanillaMax=16.0`**, and its plate's current rises by 15 per heal until 240. **The discriminating number is 15.000**: master logs no line and its plate never moves; x1 would read 1.000, and M8's withdrawn flat x5 would read 5.000. *Re-staged: the plan's zombie ignores Regeneration (F17). A spider's ratio is the same 5 × gs / 100, and its vanilla max 16 ≠ 20 also keeps `vanillaMax` from being read as a constant.* | **PASS.** READ 2026-09-28 on `8d7e0c90` (Ben's LOGIN 05:01:05). On spider `6cfe6aaf`, continuing from F20b with Regeneration II running, Ben punched it once at about 05:04:45 (confirmed by Ben; melee is not logged). VERBATIM, the first heal: `[05:04:46] [Server thread/INFO]: [Rpg] MOBHEAL 6cfe6aaf-00fb-4e0e-b923-f357809f1fd9 spider reason=MAGIC_REGEN vanilla=1.000 custom=15.000 ratio=15.000 before=140.000 after=155.000 max=240.0 vanillaMax=16.0`. Then 155, 170, 185, 200, 215, 230, one heal about every 25 ticks, and `before=230.000 after=240.000` (capped) at 05:04:53. From 05:04:55 the lines read `before=240.000 after=240.000` (F18) |

### G17-UNDEAD — the plan's zombie takes no Regeneration, so it logs no heal (CREATIVE) — the F17 control

| prediction | READING |
|---|---|
| `/rpg spawn zombie 300`, hit it once, then `/effect give @e[type=zombie,limit=1,sort=nearest] minecraft:regeneration 30 1`. **The command is refused** (vanilla's "Unable to apply this effect" message: `#minecraft:undead` is in `ignores_poison_and_regen`, read from the pinned jar), and **no `MOBHEAL` line names that zombie's uuid**. This row is why G17 does not read a zombie; if a `MOBHEAL` line DOES appear, the jar reading is wrong and G17's re-staging must be revisited | **PASS.** READ 2026-09-28 on `8d7e0c90`. `[05:05:43] [Rpg] MOBSEED f7e2111f-4935-47a8-bdfa-f0a83d484139 zombie gs=300 source=stored max=300`. Ben punched it, then ran `/effect give @e[type=zombie,limit=1,sort=nearest] minecraft:regeneration 30 1` (05:05:53). **Chat: "Unable to apply this effect"** (Ben's reading; a player's command feedback is not logged). **Zero `MOBHEAL` lines name `f7e2111f`** in the whole log (Grep, count 0). The jar reading behind F17 stands |

### STAGING THE DRAGON (added before the boot) — THIS WORLD'S DRAGON IS DEAD

The End's `ender_dragon_fight.dat` reads `dragon_killed 1` and `previously_killed 1`, and no `ender_dragon`
`MOBSEED` appears in any log, including M25's visit to (0, 100, 0). So the dragon is **summoned**:

1. `/execute in minecraft:the_end run tp @s 0 100 40`, then fly up a little so the dragon has air around it.
2. `/summon ender_dragon ~ ~8 ~15`. A summoned dragon starts HOVERING and stays put. **Do NOT add `NoAI`**: read from
   the jar, `checkCrystals` runs before the NoAI return, but its hit parts are only moved after it, so a NoAI dragon
   could not be hit at all. Expect **`MOBSEED <uuid> ender_dragon gs=300 source=rolled max=3000`** (200 × 5 × 3;
   M25: within 1,000 blocks of (0, 0)).
3. `/summon end_crystal ~ ~ ~5`. The dragon finds any crystal within 32 blocks of its bounding box (jar).

### F20a — the dragon hurt ONLY by `/rpg mobdamage` is NOT healed by the crystal (CREATIVE) — before G20, same dragon

| prediction | READING |
|---|---|
| With the dragon and crystal staged and the dragon never touched: console `Health` **`200.0f`** (the control). Look at the dragon and `/rpg mobdamage 500`: chat `ENDER_DRAGON damaged: 2500/3000 custom HP`. Console again: **`Health` still `200.0f`**. Wait **10 seconds** (20 crystal ticks): **NO `MOBHEAL` line names the dragon's uuid.** This is F20's player-facing case: a ray or projectile weapon's hit is the same `applyDamage` call. *If the chat says "Look at a mob.", the ray missed the dragon's body: re-aim at its middle. That is a missed staging, not a reading* | **PASS on the log line (its absence), with the Health controls NOT TAKEN.** READ 2026-09-28 on `8d7e0c90`. **Two dragons were summoned by accident** (`76dfceb2` at 05:06:42, `1faf693f` at 05:06:49), and Ben killed both at 05:07:02. **The read dragon:** `[05:07:05] [Server thread/INFO]: [Rpg] MOBSEED 88fc8a16-7b45-43af-9f5e-232d0e407f87 ender_dragon gs=300 source=rolled max=3000`, exactly as staged. The crystal (the staged `/summon end_crystal`) went down at 05:07:13. `/rpg mobdamage 500` ran at 05:07:30, and the same second has a MOBSEED line for `88fc8a16` from the command's re-registration, so the aim was on the dragon. **No `MOBHEAL` line named `88fc8a16` for 14 seconds** (05:07:30 to 05:07:44; about 28 crystal ticks with a crystal standing), until Ben's punch (G20). The console `Health` reads (`200.0f` before and after) were NOT taken: Ben stopped the console write. **F20 is therefore witnessed on the dragon by the heal's absence and its onset at the punch** |

### G20 — an End crystal heals the dragon proportionally (CREATIVE)

| prediction | READING |
|---|---|
| In the End, with `/rpg mobtrace` on: note the dragon's own `MOBSEED … ender_dragon gs=<g> source=<rolled|stored> max=<m>` line as it loads (if no such line appears this boot, read its uuid and GS with `/rpg mobinfo` looking at it, and note that here). **Hit the dragon once** while a crystal stands. **Every 10 ticks** (2 a second, read from the jar) it logs **`MOBHEAL <uuid> ender_dragon reason=ENDER_CRYSTAL vanilla=1.000 custom=<5g/100> ratio=<5g/100> before=<b> after=<min(m, b+c)> max=<m>.0 vanillaMax=200.0`**. **At `gs=300` that is `custom=15.000 ratio=15.000 max=3000.0`**, which M25 predicts for a dragon seeded on this build within 1,000 blocks of (0, 0); a dragon seeded on an older build keeps its stored GS (M5), and the ratio must still equal `max / 200`. **Once `after` reaches `max`, the lines continue with `before = after = max`** (F18). Breaking the crystal stops them | **PASS.** READ 2026-09-28 on `8d7e0c90`, on dragon `88fc8a16` (seeded `gs=300 source=rolled max=3000` at 05:07:05; this world's own dragon is dead). After F20a, Ben punched it once at about 05:07:43 (confirmed by Ben). VERBATIM, the first heal: `[05:07:44] [Server thread/INFO]: [Rpg] MOBHEAL 88fc8a16-7b45-43af-9f5e-232d0e407f87 ender_dragon reason=ENDER_CRYSTAL vanilla=1.000 custom=15.000 ratio=15.000 before=2500.000 after=2515.000 max=3000.0 vanillaMax=200.0`. **34 lines, 2500 to 3000, from 05:07:44 to 05:08:01**: two a second, 15 each, the last capped at `before=2995.000 after=3000.000`. That is the staging note's "34 lines, about 17 seconds". Then `[05:08:01] … before=3000.000 after=3000.000 max=3000.0 vanillaMax=200.0`, the F18 line |

---

## THE REGRESSIONS (M13, M14) — CARRIED, unchanged by this slice

The environmental damage branch is unchanged (plan §3 slice 3). **These stay OBSERVATION rows: no log line reads a
mob's environmental damage** (§6 F19), so the plan §4's *"the probe logs the FALL"* has no instrument. The core
arithmetic is pinned by `DamageScaleTest`'s new rows (GS 300 → x15; GS 500 → x25, 20 → exactly 500; Knell 360 → x18,
720 → x36).

### G11 — environmental damage on a mob is proportional (M13) (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| Drop a `/rpg spawn zombie` (GS 100 under M25 near spawn) and a `/rpg spawn zombie 500` from ~25 blocks: **both die** (a lethal-for-vanilla fall is lethal at every GS) | **PASS as reported by Ben (an observation row).** READ 2026-09-28 on `8d7e0c90`. GS 100: `[05:11:09] [Rpg] MOBSEED 25d7174b-8226-485e-a05f-39cda9d103da zombie gs=100 source=rolled max=100` (the only zombie seed in the command's second). GS 500: `[05:11:05] [Rpg] MOBSEED db395b68-1af4-4187-9737-cdc93a449984 zombie gs=500 source=stored max=500`. Ben: "both died". **A staging trap, seen live:** an earlier GS 500 spawn at 05:10:44 (`a7519b81`) seeded `max=1240`, a LEADER zombie (vanilla spawn setup since F16c raised its MAX_HEALTH to 49.6). It would survive the drop, as in vanilla, so it was not used (§6 F21) |

### G18 — the Knell in lava: proportional (M14) (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn knell` and `/rpg spawn knell 200` in lava for the same few seconds lose **about the same FRACTION** of their bar, read with `mobinfo` before and after | **SKIPPED by Ben, and the row is HOLLOW.** Ben: "the knell is a wither skeleton which is immune to lava by default in vanilla minecraft". The seeds were right: `[05:12:08] MOBSEED 8317a616-b2e1-4f8f-aa53-b08e6478e508 wither_skeleton gs=100 source=rolled max=360` and `[05:12:10] MOBSEED 99b9abd0-328b-4a62-87b2-f945b8570ae7 wither_skeleton gs=200 source=stored max=720` (360 and 720, M14's custom maxes). But lava deals them nothing, so this staging cannot read M14 on any build. It was carried from slice 1 unread each time. **§6 F21**: re-stage by fall if a live witness is wanted; `DamageScaleTest`'s Knell rows pin the arithmetic |

---

## CARRIED FORWARD — EVERY STILL-UNREAD ROW, WITH ITS SLICE 3 VERDICT

Unread on `GATE-mob-scaling-m25.md` (merged as `b3445a12`). **Slice 3 changes only the heal path, so every row
below is CARRIED with its M25 prediction verbatim**; none reads a mob's heal.

### M25-SPAWN — `/rpg spawn` with no [gs] reads the new curve (SURVIVAL) — CARRIED, both halves

| prediction | READING |
|---|---|
| At the M25-OW position (`/tp @s 10000 ~ 10000`), `/rpg spawn zombie` (no score): **`MOBSEED … zombie gs=100 source=rolled`**. At the M25-END-11K teleport point (`/execute in minecraft:the_end run tp @s 7778 ~ 7778`, don't move after the `/tp`), `/rpg spawn enderman`: **`MOBSEED … enderman gs=400 source=rolled`**. **Match each by UUID** (the standing tip): on M25 three enderman seeds shared the spawn's second and the End half could not be attributed. Pre-M25: 500 and 500 | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |

### G10 — the shooter dies mid-flight; the arrow keeps its stamp (SURVIVAL) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn skeleton 300` at range (15+ blocks). The moment it looses, `/kill @e[type=skeleton,limit=1,sort=nearest]`. The arrow lands: **`MOBHIT … direct=arrow causing=none gs=300 from=DIRECT ratio=15.000`**. Skipped by Ben on `ad686c9` (the kill could not be timed); covered by `MobDamagePricingTest.aShooterThatIsGoneStillPricesAtItsStamp` | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |

### G8b — melee at GS 300 is x15 (SURVIVAL) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn zombie 300`, one hit at max 100: **`MOBHIT cause=ENTITY_ATTACK … gs=300 … ratio=15.000`** | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |

### GX-ENV — a player's environmental damage did NOT change (SURVIVAL) — CARRIED

| prediction | READING |
|---|---|
| At max 400 (`/rpg healthboost 300`, held), fall far enough to take damage. **No `MOBHIT` line**, and the loss is a share of max: a fall of vanilla amount `a` takes `a x 400/20` | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |

### G13 — the GS survives a restart (437) (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| Near spawn, `/rpg spawn zombie 437`, plate `[437]`. Session B restarts through the console file (no `--refresh-content`). Rejoin: still **`[437]`**, and the `MOBSEED` for its uuid after the restart reads **`gs=437 source=stored`** | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |

### G12 — the GS survives a chunk unload (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn zombie 222`, fly several hundred blocks away and come back. Still **`[222]`**, with **`MOBREMOVE <uuid>`** then **`MOBSEED <uuid> … gs=222 source=stored`** in the log | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |

### G14 — a mob through a Nether portal keeps its GS (SURVIVAL) — CARRIED as rewritten under M25

| prediction | READING |
|---|---|
| `/rpg spawn zombie 140`, and get it through a Nether portal. In the Nether it still reads **`[140]`** (`MOBSEED … gs=140 source=stored` on arrival), while `mobinfo`'s "would roll" there reads **exactly 200**. Full HP on arrival is §6 F1, and expected | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |

### G15 — a stored GS of 0 is re-rolled (CREATIVE) — CARRIED as rewritten under M25

| prediction | READING |
|---|---|
| `/summon zombie ~ ~ ~ {BukkitValues:{"rpg:mob_gear_score":0}}` anywhere in the overworld: a WARN naming the invalid `mob_gear_score`, then **`MOBSEED … gs=100 source=rolled`**, exactly 100 | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |

### G15b — a stored GS of 900 is re-rolled, not clamped (CREATIVE) — CARRIED as rewritten under M25

| prediction | READING |
|---|---|
| The same with `900`: a WARN, then **`gs=100 source=rolled`**, NOT 900 and NOT 500 | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |

### G16 — conversions inherit the GS (CREATIVE) — CARRIED

| prediction | READING |
|---|---|
| `/rpg spawn slime 300`. **Re-spawn until it is size 4**: `execute at BaronVonYeetus run data get entity @e[type=slime,limit=1,sort=nearest] Size` prints **`3`**, after a `Health` read as the control. Kill it: **each child's `MOBSEED` reads `gs=300 source=stored`**. And/or a GS 300 zombie drowned until it converts: the drowned's `MOBSEED` reads `gs=300 source=stored` | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |

### THE PROBE ROWS — CARRIED, every one with an explicit [gs] (SURVIVAL)

Each row: spawn at the named GS, take the hit at max 100, and read the `MOBHIT` line. Predictions as in
`GATE-mob-scaling-2.md` and `GATE-mob-scaling-m25.md`, verbatim.

| row | set-up (GS) | predicted `direct` / `causing` / `from` | predicted `ratio` | READING |
|---|---|---|---|---|
| **P-ARROW** | `skeleton 200` (spawns with its bow since F16c) | `arrow` / `skeleton` / `DIRECT` | **10.000** | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-TRIDENT** | `drowned 300`, re-spawned until one holds a trident | `trident` / `drowned` / `DIRECT` | **15.000** | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-BLAZE** | `blaze 300` | `small_fireball` / `blaze` / `DIRECT` | **15.000** | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-GHAST** | `ghast 300` | `fireball` / `ghast` / `DIRECT`; the blast is a second line from the same stamp | **15.000** each | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-SKULL** | `wither 300` | `wither_skull` / `wither` / `DIRECT` | **15.000** | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-SPIT** | an angered llama (passive) | `llama_spit` / `llama` / `CAUSING`, `gs=-` | **5.000** | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-CREEPER** | `creeper 200` | `creeper` / `creeper` / `DIRECT` | **10.000** | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-WITCH** | `witch 300`, a harming splash | the potion / `witch` / `DIRECT` | **15.000** | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-BOOM** | `warden 300`, sonic boom | `warden` / `warden` / `DIRECT` | **15.000** | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-FANGS** | `evoker 300`, fangs | `evoker_fangs` / `evoker` / **`CAUSING`** | **15.000** | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-GUARDIAN** | `guardian 300`, beam | `guardian` / `guardian` / `DIRECT` | **15.000** | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-BREATH** | the dragon's breath cloud, if reachable | **UNPREDICTED**: `CAUSING` from the dragon if the cloud names its owner, else no MOBHIT (a named finding) | — | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-WIND** | `breeze 300`, wind charge | `breeze_wind_charge` / `breeze` / `DIRECT` | **15.000** | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |
| **P-DOT** | a cave spider's poison | **NO `MOBHIT` line for the poison ticks** (F5) | — | **NOT READ on `8d7e0c90`**: not attempted. Ben ended the session after G11 ("skip the rest, wrap it up"). **CARRIED FORWARD**, not waived. |
