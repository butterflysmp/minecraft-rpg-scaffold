# GATE — the build system, Slice 2: kits removed, and build storage

**Status: READ at `8422b68`, 2026-09-25. R0 PASS. Every BS row and the ST re-run line: reported
good or skipped -- not itemised by Ben.** Ben ran the rows he judged important and reports them all
good; he declined to say which, so no row below is recorded as PASS on his report, and none is inferred
from the server log. The boot followed the seat's verification of `81b0c06` from origin; `8422b68` is
the comment fix that verification asked for. Every prediction below was written **before** any boot.
Readings go **beside** a prediction, never over it, and a prediction is not edited once its row has
been read. Readings are verdicts, not figures, unless the row asks for a figure.

```
ROWS     16   R0a R0b R0c BS1 BS2 BS3 BS3b BS4 BS5 BS6 BS7 BS8 BS9 BS10 BS11 BS12
         ──
         16   = git grep -c '^### R\|^### BS' <ref> -- GATE-build-storage.md
         +    the STONE REGRESSION list at the end: GATE-build-stone.md rows, re-run here
```

**Plan:** `PLAN-build-system.md` §3.2, as built (§3.2.1), with the clarification under RULINGS that
`/rpg class` and `/rpg element` are kept, weapon-free, until slice 3.

**Declared game mode: SURVIVAL, EVERY ROW.** Nothing in this slice reads the game mode, and survival is what
ships.

**Boot:** `./scripts/dev-server.sh --refresh-content`. The refresh is REQUIRED: this slice deletes
`content/kits/` and strips `archetype:` from six ability files, and `saveResource` never overwrites. BS9 is
the one row that deliberately puts a stale kit file back.

**Set-up shared by most rows:** an op player, survival, `/rpg class ranger` then `/rpg element fire`. The Fire
Ranger's pool default is Left = Rekindle, Right = Solar Lance, Q = the placeholder Ranger Ultimate. The Fire
Mage's is Left = Ember Step, Right = Solar Grenade, Q = the placeholder Mage Ultimate. Your build file is
`run\plugins\Rpg\builds\<your uuid>.json`.

---

## *** R0 — THE FIRST ROWS OF THE FILE. If any R0 row fails, STOP. ***

### R0a — the build line names this branch's tip

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <tip>` naming the PR's tip SHA -- not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS** *(8422b68, the `--refresh-content` boot, 22:19)*: `[Rpg] Build: 8422b68` -- the tip, not `-dirty` |

### R0b — the jar carries the build store, and NOT the kit code

| prediction | instrument | READING |
|---|---|---|
| `FileBuildRepository`, `BuildService` and core `LoadoutResolution` PRESENT; **`KitLoader`, `KitRegistry` and `KitDefinition` ABSENT** (the deletion); the control ABSENT | the scan below | **PASS** *(same boot)*: PRESENT FileBuildRepository, BuildService, LoadoutResolution; ABSENT KitLoader, KitRegistry, KitDefinition, `content/kits/ranger_fire.yml` and the control. `run\plugins\Rpg\content\kits` does not exist |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/storage/FileBuildRepository.class',
               'io/github/butterflysmp/rpg/paper/build/BuildService.class',
               'io/github/butterflysmp/rpg/core/build/LoadoutResolution.class',
               'io/github/butterflysmp/rpg/paper/content/KitLoader.class',
               'io/github/butterflysmp/rpg/core/kit/KitRegistry.class',
               'io/github/butterflysmp/rpg/core/kit/KitDefinition.class',
               'content/kits/ranger_fire.yml',
               'io/github/butterflysmp/rpg/paper/build/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

> **Five ABSENTs are expected, and they mean different things.** The control never existed. The three kit
> classes and `content/kits/ranger_fire.yml` were deleted by this slice. A PRESENT on any of those four is a
> stale jar, and R0 stops there.

### R0c — the boot log: no kits, no retired-key warning, no stale-kit warning

| prediction | instrument | READING |
|---|---|---|
| the `Loaded ...` line contains **no** `kits` count and still reads `2 pools`; **no** line mentions `archetype:` (the six shipped files no longer declare it); **no** line says kit files "are no longer read" (`--refresh-content` removed them) | `Select-String run\logs\latest.log -Pattern 'Loaded ','archetype','no longer read'` | **PASS** *(same boot)*: `[Rpg] Loaded 8 abilities, 28 visuals, 5 statuses, 7 elements, 10 enchants, 2 pools, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes` -- no kits count, 2 pools; no line mentions `archetype`; no `no longer read`; 0 SEVERE or ERROR lines |

---

## THE ROWS

### BS1 — `/rpg class` and `/rpg element` grant NOTHING, however often they are run

| prediction | READING |
|---|---|
| empty your inventory except the star and the stone. `/rpg class ranger`, `/rpg element fire`: *"You are now Fire Ranger"* and **no weapon appears**, and no "Given:" or "Unlocked:" line. Run both again, then `/rpg class mage` and `/rpg class ranger`: **still no weapon** -- the duplicate-weapons defect is gone with the grant. `/rpg class melee` is refused, listing `mage, ranger` | reported good or skipped -- not itemised by Ben *(8422b68)*. Which rows were read: **NOT GIVEN** -- Ben declined to itemise; not inferred |

### BS2 — a fresh player receives no weapon anywhere

| prediction | READING |
|---|---|
| a player with no profile (stop the server, delete your `run\plugins\Rpg\players\<uuid>.json`, start) joins: the inventory holds only the star and the stone -- **no weapon**. Choosing a class and element adds none (BS1) | reported good or skipped -- not itemised by Ben *(8422b68)*. Which rows were read: **NOT GIVEN** -- Ben declined to itemise; not inferred |

### BS3 — a saved slot is cast, and survives a restart

| prediction | READING |
|---|---|
| as the Fire Ranger, `/rpg build set active1 solar_lance`: *"Saved: active1 = solar_lance ..."*. The stone's lore reads **Left click: Solar Lance, Right click: Rekindle** -- setting Active 1 to the ability Active 2 held SWAPS them. Left click casts the LANCE (the ray), right click casts Rekindle. `/stop`, start, rejoin: the lore and the casts are unchanged | reported good or skipped -- not itemised by Ben *(8422b68)*. Which rows were read: **NOT GIVEN** -- Ben declined to itemise; not inferred |

### BS3b — the file holds every slot

| prediction | READING |
|---|---|
| open `run\plugins\Rpg\builds\<uuid>.json`: one entry `"classId": "ranger", "elementId": "fire"` with `"actives": ["solar_lance", "rekindle"]`, the ultimate named, and `"aspects"` and `"fragments"` lists of `null`s (2 and 4) -- empty slots written explicitly, not omitted | reported good or skipped -- not itemised by Ben *(8422b68)*. Which rows were read: **NOT GIVEN** -- Ben declined to itemise; not inferred |

### BS4 — one loadout PER CELL

| prediction | READING |
|---|---|
| after BS3: `/rpg class mage`. The stone's lore is the **Mage default** (Left: Ember Step, Right: Solar Grenade). `/rpg build set active2 solar_lance`: Right = Solar Lance. `/rpg class ranger`: the lore is **the Ranger build from BS3** (Left: Solar Lance, Right: Rekindle), not the Mage one and not the Ranger default. `/rpg class mage`: Right: Solar Lance again | reported good or skipped -- not itemised by Ben *(8422b68)*. Which rows were read: **NOT GIVEN** -- Ben declined to itemise; not inferred |

### BS5 — cooldowns are kept across a cell change

| prediction | READING |
|---|---|
| as the Fire Ranger, press Q (the Ultimate, 60 s). At once `/rpg class mage`, then `/rpg class ranger`, then Q: **no cast**, and the action bar reads *Ranger Ultimate (placeholder) -- ready in ~5x.xs* | reported good or skipped -- not itemised by Ben *(8422b68)*. Which rows were read: **NOT GIVEN** -- Ben declined to itemise; not inferred |

### BS6 — `/rpg cast` is the loadout for a non-op, anything for an op

| prediction | READING |
|---|---|
| as an op: `/rpg cast void_slash` casts, free. `/deop <self>` (console): `/rpg cast void_slash` -> *void_slash is not in your loadout*; `/rpg cast solar_lance` (in the BS3 loadout) casts; tab completion offers exactly the three equipped ids. `/op <self>` afterwards | reported good or skipped -- not itemised by Ben *(8422b68)*. Which rows were read: **NOT GIVEN** -- Ben declined to itemise; not inferred |

### BS7 — a corrupt build file: unavailable for the session, the default cast, and said so

| prediction | READING |
|---|---|
| `/stop`. In `builds\<uuid>.json`, duplicate the whole `ranger`/`fire` entry inside `"cells"` (two loadouts for one cell). Start and join: the log has a **SEVERE** line naming the build file as *structurally invalid*; the stone's lore reads the **Ranger DEFAULT** plus a red line **"Build unavailable -- casting the default."**; left click casts **Rekindle** (the default); `/rpg build set active1 solar_lance` is refused as *unavailable*. `/stop`, restore the file, start: the BS3 build is back (the corrupt file was never overwritten) | reported good or skipped -- not itemised by Ben *(8422b68)*. Which rows were read: **NOT GIVEN** -- Ben declined to itemise; not inferred |

### BS8 — an old kit-era profile still loads, and keeps its cell

| prediction | READING |
|---|---|
| `/stop`. In `players\<uuid>.json`, set `"unlockedAbilities": ["arc_surge", "void_slash"]` (a pre-slice-2 grant) and keep `"archetypeId": "ranger"`, `"elementId": "fire"`. Start and join: the profile loads, you are still the Fire Ranger (stone lore as before), and as a **non-op** `/rpg cast void_slash` is **refused** -- the old grant is not read. `/rpg class ranger` again: the file's `unlockedAbilities` becomes `[]` | reported good or skipped -- not itemised by Ben *(8422b68)*. Which rows were read: **NOT GIVEN** -- Ben declined to itemise; not inferred |

### BS9 — a stale kit file is named once, and is inert

| prediction | READING |
|---|---|
| `/stop`. Copy any `ranger_fire.yml` into `run\plugins\Rpg\content\kits\` (create the folder). Start WITHOUT `--refresh-content`: **one** WARN line naming `[ranger_fire.yml]` as "no longer read". `/rpg class ranger` still grants **no** weapon. Delete the folder afterwards | reported good or skipped -- not itemised by Ben *(8422b68)*. Which rows were read: **NOT GIVEN** -- Ben declined to itemise; not inferred |

### BS10 — an id the pool no longer offers reads EMPTY

| prediction | READING |
|---|---|
| `/stop`. In `builds\<uuid>.json`, change the Ranger `"ultimate"` to `"no_such_ability"`. Start and join: the stone's lore reads **Q: (empty)**; pressing Q shows *Nothing is equipped in that slot.* on the action bar, and nothing casts; left and right still cast the saved Actives | reported good or skipped -- not itemised by Ben *(8422b68)*. Which rows were read: **NOT GIVEN** -- Ben declined to itemise; not inferred |

### BS11 — the build loads after the stone is placed, and the lore catches up

| prediction | READING |
|---|---|
| with the BS3 build saved, rejoin and read the stone's lore at once: it names **Solar Lance** on Left (the saved build), not Rekindle (the default). *(The lore is re-rendered when the build load settles; a stone showing the default would be that refresh missing.)* | reported good or skipped -- not itemised by Ben *(8422b68)*. Which rows were read: **NOT GIVEN** -- Ben declined to itemise; not inferred |

### BS12 — a player whose build never loaded cannot be saved into

| prediction | READING |
|---|---|
| covered by BS7's refused `/rpg build set` -- **stated as a separate row so its absence would be visible**, not because it needs a separate boot | reported good or skipped -- not itemised by Ben *(8422b68)*. Which rows were read: **NOT GIVEN** -- Ben declined to itemise; not inferred |

---

## STONE REGRESSION — re-run at this slice's tip

Slice 2 changed what the stone casts (`Stones.equippedFor`), its lore (`StoneItems`) and `/rpg cast`. So the
`GATE-build-stone.md` rows that read those are re-run here, on this tip:

| source | rows | READING |
|---|---|---|
| GATE-build-stone.md | ST1 (issued, lore), ST2 (left cast + action bar), ST5 (Q both ways), ST6 (right, main hand only), ST10 (no class: the notice), ST13 (Q never casts Active 1), ST14 and ST15 (`/rpg cast`), ST17 (held right re-casts) | reported good or skipped -- not itemised by Ben *(8422b68)*. Which rows were read: **NOT GIVEN** -- Ben declined to itemise; not inferred |

The lock, the picker and convergence did not change in this slice, so the star regression list and the
stone's lock rows are **not** re-run: ST7, ST8, ST9, ST11, ST12, ST16 and the GATE-nexus.md list.

---

## MUTATIONS — run before the PR

Each was applied by literal substitution (exactly once, or aborted), proven applied (marker 1, one line
changed against a pristine copy), tested, restored with `cp` from the copy, and proven restored (`cmp`
identical, marker 0). Each module's reports were read separately.

| mutation | predicted to redden | result |
|---|---|---|
| S1 resolve from the pool default even when a loadout is saved | `LoadoutResolutionTest` + BS3 | **KILLED** by `LoadoutResolutionTest` (5 rows) |
| S2 drop the per-cell key (return the first saved loadout for any cell) | `PlayerBuildTest` + BS4 | **KILLED** by `PlayerBuildTest` |
| S3 drop the null-safety in `LoadoutResolution.offers` | `LoadoutResolutionTest` | **KILLED** by `LoadoutResolutionTest` (the NPE this slice found) |
| S4 `withCell` keeps the old `unlockedAbilities` | `PlayerProfileMigrationTest`, `ProfileServiceTest` + BS8 | **KILLED** by both |
| S5 the `archetype:` warning never fires | `AbilityLoaderTest` | **KILLED** by `AbilityLoaderTest` |
| S6 a duplicate fragment is kept | `PlayerBuildTest` | **KILLED** by `PlayerBuildTest` |
| S7 two loadouts for one cell load | `PlayerBuildTest`, `FileBuildRepositoryTest` + BS7 | **KILLED** by both |
| leave `castable` = `unlockedAbilities` | the plan's row | **NOT RUNNABLE AS WRITTEN** -- `/rpg cast` stopped reading the field in slice 1; S4 guards what slice 2 adds (plan §3.2.1 item 9) |
