# GATE — the build system, Slice 5: aspects

**Status: NOT RUN.** Every prediction below was written **before** any boot. **NO BOOT until the seat has
diffed the PR from origin.** Readings go **beside** a prediction, never over it, and a prediction is not
edited once its row has been read. Readings are verdicts, not figures, unless the row asks for a figure.

**CARRIED FORWARD, AND IT IS ROW BA13:** no itemised reading has yet read a build file ON DISK (slice 3's
BB10 and slice 4's BF1 were both unitemised). The only fact on record is that the file existed at 01:08 in
the slice 3 session.

```
ROWS     17   R0a R0b R0c BA1 BA2 BA3 BA4 BA5 BA6 BA7 BA8 BA9 BA10 BA11 BA12 BA13 BA14
         ──
         17   = git grep -c '^### R0\|^### BA' <ref> -- GATE-build-aspects.md
```

**Plan:** `PLAN-build-system.md` §3.5 with §2.4 and §2.4.1, as built (§3.5.1); **ruling 23** (an aspect slot
can be emptied).

**Declared game mode: SURVIVAL, EVERY ROW.** Nothing here reads the game mode, survival is what ships, and
creative changes costs and drops (the creative-divergence register).

**Boot:** `./scripts/dev-server.sh --refresh-content`. The refresh is REQUIRED: this slice adds
`content/aspects/` and changes both pool files. Restarts inside a row are WITHOUT the flag.

**Set-up shared by most rows:** an op player in survival, the Nexus star and the Ability Stone in the hotbar,
mobs to hit (`/rpg spawn` or any). The shipped aspects, all PLACEHOLDERS:

| aspect | cell | target | what it does |
|---|---|---|---|
| **Searing Lance** | Fire Ranger | Solar Lance (a ray) | its hit **12 -> 9**, and adds a 2.5-block burst of **4** fire at the impact |
| **Banked Embers** | Fire Ranger | Rekindle (a dash) | two more embers (angles 70 / -70), cost **35 -> 45**, cooldown **10.0 s -> 12.0 s** |
| **Cinder Wake** | Fire Mage | Ember Step (a dash) | a 3-block burning patch (2 fire per second for 3 s) at the PRE-dash spot; cooldown **8.0 s -> 10.0 s** |
| **Lingering Sun** | Fire Mage | Solar Grenade | its field lasts **5 s -> 6 s** (6 pulses, not 5) and its root **60 -> 80 ticks** |

The Fire Ranger's default loadout is Left = Rekindle, Right = Solar Lance; the Fire Mage's Left = Ember Step,
Right = Solar Grenade.

---

## THE STATUS KINDS, TRACED BEFORE `status.duration_ticks` SHIPPED (§3.5)

Read from source at this slice's base (`c615291`); the plan required it before the field could be allowed on
any kind. **A kind traced CONTINUOUS gets the field; every other kind is refused it at load.**

| kind | where the duration goes | verdict |
|---|---|---|
| `rooted`, `freeze` (Immobilize) | `ImmobilizeStatus.apply`: `remaining` counted down by a `RepeatingTask` of period **1**, one tick per run | **CONTINUOUS -- allowed** |
| `soaked` (Soaked) | `SoakedStatus.apply`: the same per-tick countdown (period **1**) | **CONTINUOUS -- allowed** |
| `scorch` | excluded by the plan: its burn count is `ceil(duration / 20)` | **REFUSED** |
| `fire` | `setFireTicks` -- vanilla burns in periodic steps | **REFUSED: NOT TRACED** (vanilla's burn cadence was not re-read from the pinned jar) |
| `potion` | `new PotionEffect(type, durationTicks, amplifier)` -- vanilla's countdown | **REFUSED: NOT TRACED** (not re-read from the pinned jar; no shipped content uses a potion status since ruling 22) |

---

## *** R0 — THE FIRST ROWS OF THE FILE. If any R0 row fails, STOP. ***

### R0a — the build line names this branch's tip

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <tip>` naming the PR's tip SHA -- not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | _(not run)_ |

### R0b — the jar carries the aspects

| prediction | instrument | READING |
|---|---|---|
| `AspectLoader`, core `AspectApplication`, core `NumberResolution`, core `AspectLore` and `content/aspects/searing_lance.yml` PRESENT; the control ABSENT | the scan below | _(not run)_ |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/paper/content/AspectLoader.class',
               'io/github/butterflysmp/rpg/core/build/AspectApplication.class',
               'io/github/butterflysmp/rpg/core/build/NumberResolution.class',
               'io/github/butterflysmp/rpg/core/build/AspectLore.class',
               'content/aspects/searing_lance.yml',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

### R0c — the boot log: four aspects, none refused

| prediction | instrument | READING |
|---|---|---|
| the `Loaded ...` line reads **`2 pools, 5 fragments, 4 aspects`**; **no** line says `Refusing aspect`, `Skipping aspect` or `Skipping pool` -- every shipped aspect passed its pool's single and pair checks | `Select-String run\logs\latest.log -Pattern 'Loaded ','aspect','Skipping pool'` | _(not run)_ |

---

## THE ROWS

### BA1 — Searing Lance: the lance detonates where it lands

| prediction | READING |
|---|---|
| as the Fire Ranger, slot **Searing Lance** in Aspect 1. Right click (Solar Lance) at a mob with a second mob within 2.5 blocks of it: a **solar detonation** shows at the impact, and the **second mob is damaged** too | _(not run)_ |

### BA2 — Cinder Wake: the patch is where the dash STARTED

| prediction | READING |
|---|---|
| as the Fire Mage, slot **Cinder Wake**. Left click (Ember Step) from a marked spot: a **burning patch** is left at the **start** of the dash, not the end; a mob standing on the start spot takes small fire damage for about 3 s | _(not run)_ |

### BA3 — Banked Embers: five embers, not three

| prediction | READING |
|---|---|
| as the Fire Ranger, slot **Banked Embers**. Left click (Rekindle): **five** embers fly -- the usual three (0, 40, -40) and two more thrown wide (70, -70) | _(not run)_ |

### BA4 — the target unequipped: the aspect goes INACTIVE, changes nothing, and comes back by itself

| prediction | READING |
|---|---|
| as the **Fire Mage** (default Left = Ember Step, Right = Solar Grenade), slot **Lingering Sun** (target Solar Grenade) in Aspect 1. Open the **Right** picker and choose **Solar Lance**, so Solar Grenade is in neither Active. The Build screen's Aspect 1 reads **"Inactive -- requires Solar Grenade equipped"** in red, and nothing about any cast changes. Choose **Solar Grenade** for Right again: the red line is gone with no other action, and BA14 holds | _(not run)_ |

> **Why the Mage, not the Ranger:** the Fire Ranger's pool offers exactly two Actives (Rekindle and Solar
> Lance, since ruling 20 deleted arc_surge), so both are always equipped and a Ranger aspect can never go
> inactive. The Fire Mage's pool offers three.

### BA5 — per player, not global: a second player casts the BASE lance

| prediction | READING |
|---|---|
| with **Searing Lance** active on your Ranger, a **second player** (an alt account) as the Fire Ranger with **no** aspect right-clicks Solar Lance at a mob: **no** detonation, and the hit is the base **12**. *(Needs a second account; if none, SKIP and say so.)* | _(not run)_ |

### BA6 — Cinder Wake's cooldown is 10.0 s

| prediction | READING |
|---|---|
| as the Fire Mage with **Cinder Wake**: left click, then left click again at once. The action bar reads *Ember Step -- ready in 9.9s* or *10.0s* -- **not** 7.9 / 8.0 s | _(not run)_ |

### BA7 — Searing Lance's direct hit is 9, and the appended burst is still 4

| prediction | READING |
|---|---|
| with **Searing Lance**, right click a mob that has a second mob beside it: the struck mob's damage popup reads **9** for the direct hit (plus **4** from the burst it also stands in); the second mob's reads **4**. The burst was appended AFTER the modify, so -25% never reached it | _(not run)_ |

### BA8 — the Build screen's numbers are the resolved ones, with the base in gray

| prediction | READING |
|---|---|
| with **Searing Lance**: the Build screen's Right (Solar Lance) icon reads **Damage: 9  (12)** with the (12) gray. With **Banked Embers**: the Left (Rekindle) icon reads **Cost: 45  (35)** and **Cooldown: 12.0 s  (10.0 s)**. With no aspect, the same icons show no gray bases | _(not run)_ |

### BA9 — an operator's /rpg cast is the BASE ability (ruling 10)

| prediction | READING |
|---|---|
| as an op with **Searing Lance** active, `/rpg cast solar_lance` at a mob: the hit is **12**, **no** detonation, no mana spent, and a stone right-click straight after still casts (no cooldown started) | _(not run)_ |

### BA10 — ruling 23: each aspect slot can be EMPTIED

| prediction | READING |
|---|---|
| open a FILLED aspect slot: its picker's last option is a barrier, *Empty this slot*. Choose it: *Saved: Aspect N = (empty)*, the cell reads *Aspect N: (empty)*, and the aspect's effect is gone from the next cast (BA1's detonation no longer shows). Do it for **both** slots. An EMPTY slot's picker has no such option | _(not run)_ |

### BA11 — the seat's row: a changed number in the CAST matches the LORE

| prediction | READING |
|---|---|
| with **Searing Lance**: read the Build screen's Solar Lance icon (**Damage: 9  (12)**), then strike a lone mob with Solar Lance: the damage popup reads **9**. The number the lore promised is the number the cast dealt -- one derive feeds both | _(not run)_ |

### BA12 — the same aspect twice is refused

| prediction | READING |
|---|---|
| with **Searing Lance** in Aspect 1, open **Aspect 2**: Searing Lance is **not** offered (Banked Embers is) | _(not run)_ |

### BA13 — CARRIED: an aspect save reaches the FILE within a second, and survives a restart

| prediction | READING |
|---|---|
| after slotting **Searing Lance** (chat *Saved: Aspect 1 = Searing Lance*), within a second open `run\plugins\Rpg\builds\<uuid>.json`: the ranger/fire entry has `"aspects": ["searing_lance", null]`, and its `"ultimate"` and `"actives"` are named (seeded). `/stop`, start **without** `--refresh-content`, read it again: **the same content**. Rejoin: Aspect 1 still shows Searing Lance | _(not run)_ |

### BA14 — Lingering Sun: the field pulses 6 times, and the root holds 80 ticks

| prediction | READING |
|---|---|
| as the Fire Mage with **Lingering Sun**, right click (Solar Grenade) onto a mob that stays in the field: its **area** popups of **2** come **6** times (not 5), and the mob stays rooted about **4 s** (80 ticks), not 3 s | _(not run)_ |

---

## MUTATIONS — run before the PR

Each was applied by literal substitution (exactly once, or aborted), proven applied (marker 1; one line removed
and one added against a pristine copy taken BEFORE the edit), tested, restored with `cp` from the copy, and
proven restored (`cmp` identical, marker 0). Every marker was a `/* */` comment, so none could swallow the rest
of its line (slice 4's F4). Each module's reports were read separately; the predictions were written into
the harness before it ran.

| mutation (the plan's §3.5 list) | predicted to redden | result |
|---|---|---|
| A1 prepend instead of append | `AspectApplicationTest` (headline) | **KILLED** by `AspectApplicationTest` and `AspectLoreTest` (the lore reads the same derive) |
| A2 drop the inactive check | `AspectApplicationTest` + BA4 | **KILLED** by `AspectApplicationTest` |
| derive into the global registry | BA5 | **NOT RUN, and no unit test can see it:** only a SECOND PLAYER casting the base shows it. **BA5 is its only witness** |
| A4 append first, modify second (the modify reaches appended effects) | `AspectApplicationTest` (appended-effect row) + BA7 | **KILLED** by `AspectApplicationTest` and `AspectLoreTest` |
| A5 multiply the percents instead of summing | `NumberResolutionTest` (pair row) | **KILLED** by `NumberResolutionTest`, and `AspectApplicationTest`'s illegal pair (multiplied, -60% twice is a legal 16%) |
| A6 round instead of refuse (the 4-tick grid check dropped) | `NumberResolutionTest` (230) | **KILLED** by `NumberResolutionTest` and `AspectApplicationTest` (228 / 232) |
| A7 drop the zero-match refusal | `AspectLoaderTest` (`knockback.strength`) | **KILLED** by `AspectApplicationTest` and `AspectLoaderTest` |
| A8 drop the pair check | `AspectLoaderTest` (pair row) | **KILLED** by `AspectLoaderTest` |
| A9 render the lore from the base definition | the plan's `BuildLoreTest` + BA8 | **KILLED** by `AspectWiringSignatureTest` -- paper tests cannot render a menu, so the lore's DATA is `AspectLoreTest` (core) and WHICH definition paper hands it is pinned from source |
