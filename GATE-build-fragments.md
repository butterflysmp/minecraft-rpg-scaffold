# GATE — the build system, Slice 4: fragments (and ruling 20's arc_surge deletion)

**Status: READ at `4308622`, 2026-09-26. R0a, R0b and R0c PASS. EVERY BF ROW: reported good or skipped
-- not itemised by Ben.** Ben ran the rows he intended to and reports them good; he did not say which, so no
BF row is recorded as PASS on his report, and none is inferred from the server log.

**AND THEREFORE THE CARRY-FORWARD IS NOT CLOSED BY AN ITEMISED READING.** BF1 -- a build file read ON DISK
-- is among the unitemised rows. What IS on record is a fact about the folder, not a reading: before this
boot, `builds\<uuid>.json` already held a ranger/fire entry written at 01:08 in the slice 3 session (see
BF1's cell).

The boot followed the seat's verification of `953a864` from origin; `4308622` is ruling 22 on top of it.
Every prediction below was written **before** any boot. Readings go **beside** a prediction, never over
it, and a prediction is not edited once its row has been read. Readings are verdicts, not figures, unless the row asks for a figure.

**CARRIED FORWARD, AND IT IS ROW BF1:** slice 3's BB10 was never read, and slice 2 left `builds\` empty.
Nothing has yet read a build file on disk. BF1 reads one.

```
ROWS     16   R0a R0b R0c BF1 BF2 BF3 BF4 BF5 BF6 BF7 BF8 BF9 BF10 BF11 BF12 BF13
         ──
         16   = git grep -c '^### R0\|^### BF' <ref> -- GATE-build-fragments.md
```

**Plan:** `PLAN-build-system.md` §3.4, as built (§3.4.1), with **ruling 20** (arc_surge deleted) and
**ruling 21** (fragments are positive-only).

**Declared game mode: SURVIVAL, EVERY ROW.** Nothing here reads the game mode, survival is what ships, and
creative's own-inventory screen decomposes gestures differently (the creative-divergence register).

**Boot:** `./scripts/dev-server.sh --refresh-content`. The refresh is REQUIRED: this slice adds
`content/fragments/`, changes both pool files, and deletes `abilities/arc_surge.yml`. Restarts inside a
row are WITHOUT the flag.

**Set-up shared by most rows:** an op player in survival, class **Ranger**, element **Fire** (Build screen),
the Nexus star and the Ability Stone in the hotbar. The shipped fragments, all PLACEHOLDERS: **Vigor** +4
max health, **Focus** +10 max mana, **Keen** +2% crit chance, **Mending** +0.05/s health regen, **Ward**
+1 defense. Both Fire pools offer all five. Your build file is `run\plugins\Rpg\builds\<your uuid>.json`.
`/rpg stats` prints the stat sheet, then the Accessories block, then the **Fragments** block.

---

## *** R0 — THE FIRST ROWS OF THE FILE. If any R0 row fails, STOP. ***

### R0a — the build line names this branch's tip

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <tip>` naming the PR's tip SHA -- not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS** *(4308622, the `--refresh-content` boot, 05:50)*: `[Rpg] Build: 4308622` -- the tip, not `-dirty` |

### R0b — the jar carries the fragments, and NOT arc_surge

| prediction | instrument | READING |
|---|---|---|
| `FragmentLoader`, core `StatSourceBound`, core `FragmentContributions`, `FragmentSheet` and `content/fragments/fragment_vigor.yml` PRESENT; `content/abilities/arc_surge.yml` ABSENT (ruling 20); `content/visuals/arc_surge.yml` and `content/statuses/surge.yml` ABSENT (ruling 22); the control ABSENT | the scan below | **PASS** *(same boot)*: PRESENT FragmentLoader, StatSourceBound, FragmentContributions, FragmentSheet and `content/fragments/fragment_vigor.yml`; ABSENT `content/abilities/arc_surge.yml`, `content/visuals/arc_surge.yml`, `content/statuses/surge.yml` and the control. No stale copy of any of the three in the data folder |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/paper/content/FragmentLoader.class',
               'io/github/butterflysmp/rpg/core/build/StatSourceBound.class',
               'io/github/butterflysmp/rpg/core/build/FragmentContributions.class',
               'io/github/butterflysmp/rpg/paper/hud/FragmentSheet.class',
               'content/fragments/fragment_vigor.yml',
               'content/abilities/arc_surge.yml',
               'content/visuals/arc_surge.yml',
               'content/statuses/surge.yml',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

> **Four ABSENTs are expected and they mean different things.** The control never existed; the ability
> `arc_surge.yml` was deleted by ruling 20, and the visual and the status by ruling 22. A PRESENT on any of
> those three is a stale jar, and R0 stops.

### R0c — the boot log: five fragments, every accessory, one fewer ability, nothing skipped

| prediction | instrument | READING |
|---|---|---|
| the `Loaded ...` line reads **`7 abilities, 27 visuals, 4 statuses`** (8, 28 and 5 before rulings 20 and 22), **`2 pools, 5 fragments`** and **`6 accessories`** -- every shipped accessory loads, `fletchers_quiver` and `sages_scroll` included (ruling 21 kept their drawbacks legal). **No** line says `Skipping fragment`, `Skipping pool` or `name DELETED` -- the retired-file warning of the ability, visual or status loader (`--refresh-content` removed the stale copies) | `Select-String run\logs\latest.log -Pattern 'Loaded ','Skipping','DELETED'` | **PASS** *(same boot)*: `Loaded 7 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 2 pools, 5 fragments, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`; 0 lines with `Skipping` or `DELETED`; 0 SEVERE or ERROR |

---

## THE ROWS

### BF1 — CARRIED FROM SLICE 3's BB10: a save reaches the FILE within a second, and survives a restart

**Read the file ON DISK, not the screen.** This is the first reading of a build file this build system
has ever had.

| prediction | READING |
|---|---|
| as the Fire Ranger with nothing saved, Build, **Fragment 1**, choose **Vigor**: chat reads *Saved: Fragment 1 = Vigor*. **Within a second**, open `builds\<uuid>.json`: it exists and holds a `"classId": "ranger", "elementId": "fire"` entry with `"fragments": ["fragment_vigor", null, null, null]`, and -- because the save seeds them (§3.4.1) -- `"ultimate": "ultimate_placeholder_ranger"` and `"actives": ["rekindle", "solar_lance"]`. `/stop`, start **without** `--refresh-content`, read the file again: **the same content**. Rejoin: Fragment 1 still shows Vigor | reported good or skipped -- not itemised by Ben *(4308622)*. Not PASS on its own, and nothing inferred. **PRECONDITION, recorded before the reading:** the ranger/fire entry already EXISTED in the build file, written 01:08 in the slice 3 session (default abilities, no fragments) -- so this row updated an existing entry rather than creating one; the predicted file content is the same either way |

### BF2 — a slotted fragment moves its stat within a quarter-second, and the sheet names it

| prediction | READING |
|---|---|
| note your max health in `/rpg stats`. Slot **Vigor** (if BF1 did not): max health is **4 higher** on the next `/rpg stats` (the reconcile runs every 5 ticks). The sheet's last block reads **Fragments** then *Vigor: +4 Max Health*. The Nexus stats head's lore ends with the same block | reported good or skipped -- not itemised by Ben *(4308622)*. Not PASS on its own, and nothing inferred |

### BF3 — an accessory and a fragment on the same stat BOTH count (one reconcile, three sources)

| prediction | READING |
|---|---|
| wear **Keen Charm** (+5% crit chance) in a universal accessory slot, and slot the **Keen** fragment (+2%). `/rpg stats` crit chance reads **22%** -- base 15 + 5 + 2 -- and stays 22% over several readings a few seconds apart (a second reconcile would flicker one source away every pass). Take the charm off: **17%**. Put it back, empty the fragment: **20%** | reported good or skipped -- not itemised by Ben *(4308622)*. Not PASS on its own, and nothing inferred |

### BF4 — one of each: a slotted fragment is not offered again, and a hand-edited duplicate counts once

| prediction | READING |
|---|---|
| with **Vigor** in Fragment 1, open **Fragment 2**: Vigor is **not** offered (Focus, Keen, Mending, Ward are). `/stop`, edit the build file so the ranger/fire `"fragments"` read `["fragment_vigor", "fragment_vigor", null, null]`, start and join: `/rpg stats` shows *Vigor: +4 Max Health* **once**, and max health is +4, **not +8**. **No log line** names the duplicate -- the plan asked for one and it is not built (§3.4.1) | reported good or skipped -- not itemised by Ben *(4308622)*. Not PASS on its own, and nothing inferred |

### BF5 — the cell gate: another cell's fragments contribute nothing

| prediction | READING |
|---|---|
| as the Fire Ranger with Vigor slotted, Build, Class **Mage**: `/rpg stats` has **no** Fragments block and max health is 4 lower. Class **Ranger**: the block and the +4 **return** (the Ranger's fragments stayed in the file). **This row is the only witness of the cell gate** -- no unit test can build the `Stones` it lives in | reported good or skipped -- not itemised by Ben *(4308622)*. Not PASS on its own, and nothing inferred |

### BF6 — a restart keeps the fragments and their stats

| prediction | READING |
|---|---|
| with Vigor and Keen slotted, `/stop`, start (no refresh), rejoin: Build shows both, and `/rpg stats` lists both with the +4 and the +2% in the totals | reported good or skipped -- not itemised by Ben *(4308622)*. Not PASS on its own, and nothing inferred |

### BF7 — every shipped accessory still loads under the combined bound

| prediction | READING |
|---|---|
| `/rpg give <you> fletchers_quiver` and `/rpg give <you> sages_scroll` both succeed (neither was refused at load); their tooltips still read *-0.20/5s Health Regen* and *-3% Crit Chance* -- ruling 21 kept Ben's numbers | reported good or skipped -- not itemised by Ben *(4308622)*. Not PASS on its own, and nothing inferred |

### BF8 — the FIRST fragment on a cell with nothing saved keeps the default abilities

**The hazard §3.4.1 designs out:** a saved null ability reads EMPTY, not the default.

| prediction | READING |
|---|---|
| Class **Mage** (Fire), a cell with nothing saved: the stone's lore is the Mage default (Left: Ember Step, Right: Solar Grenade). Slot **Focus** in Fragment 1: the stone's lore is **still** the default -- not "(empty)" -- and left click casts Ember Step, right click Solar Grenade, Q the Mage Ultimate | reported good or skipped -- not itemised by Ben *(4308622)*. Not PASS on its own, and nothing inferred |

### BF9 — a slot can be emptied

| prediction | READING |
|---|---|
| open a FILLED fragment slot: its picker's last option is a barrier, *Empty this slot*. Choose it: *Saved: Fragment N = (empty)*, the cell reads *Fragment N: (empty)*, and the stat drops back. An EMPTY slot's picker has no such option | reported good or skipped -- not itemised by Ben *(4308622)*. Not PASS on its own, and nothing inferred |

### BF10 — the aspect row is still "Coming in a later update"

| prediction | READING |
|---|---|
| row 3 of the Build screen is still two barriers, *Aspect 1* and *Aspect 2*, *Coming in a later update*; clicking does nothing | reported good or skipped -- not itemised by Ben *(4308622)*. Not PASS on its own, and nothing inferred |

### BF11 — arc_surge is gone (ruling 20)

| prediction | READING |
|---|---|
| as the Fire Ranger, the **Left** picker offers exactly **Rekindle** and **Solar Lance**. As an op, `/rpg cast arc_surge` replies *Unknown ability: arc_surge*, and tab completion after `/rpg cast ` does not list it | reported good or skipped -- not itemised by Ben *(4308622)*. Not PASS on its own, and nothing inferred |

### BF12 — a fragment's cell shows its own icon and its modifiers

| prediction | READING |
|---|---|
| Vigor's cell is a **red dye** named *Fragment 1: Vigor*, lore *+4 Max Health* then *Placeholder fragment.* then *Click to change.*; the Fragment picker's options show each fragment's own icon (red dye, lapis, flint, glistering melon, iron nugget), and the current one glints | reported good or skipped -- not itemised by Ben *(4308622)*. Not PASS on its own, and nothing inferred |

### BF13 — nothing leaves the fragment picker

| prediction | READING |
|---|---|
| on the Fragment 2 picker, with the off hand and hotbar 1 empty: shift-click an option, press **1** over one, press **F** over one. Nothing moves; the inventory, hotbar 1 and the off hand are unchanged | reported good or skipped -- not itemised by Ben *(4308622)*. Not PASS on its own, and nothing inferred |

---

## MUTATIONS — run before the PR

Each was applied by literal substitution (exactly once, or aborted), proven applied (marker 1; lines removed
and added counted against a pristine copy taken BEFORE the edit), tested, restored with `cp` from the copy,
and proven restored (`cmp` identical, marker 0). Each module's reports were read separately. The
predictions were written into the harness before it ran.

| mutation | predicted to redden | result |
|---|---|---|
| R20 the retired-file skip disabled (commit 1, ruling 20) | `AbilityLoaderTest` | **KILLED** by `aRetiredAbilityFileIsSkippedAndNamedOnce` alone |
| G1 the visual loader's retired-file skip disabled (ruling 22) | `VisualLoaderTest` | **KILLED** by `aRetiredVisualFileIsSkippedAndNamedOnce` alone |
| G2 the status loader's retired-file skip disabled (ruling 22) | `StatusLoaderTest` | **KILLED** by `aRetiredStatusFileIsSkippedAndNamedOnce` alone |
| F1 a fragment may carry a negative (ruling 21 broken) | `FragmentDefinitionTest`, `StatSourceBoundTest`, `FragmentLoaderTest` | **KILLED** by all three |
| F2 the bound counts every fragment slot as a negative source (the plan's 8x) | `StatSourceBoundTest`, `AccessoryNegativesTest` | **KILLED** by both (4 rows) |
| F3 fragments reconciled in a SECOND call for one stat | `FragmentWiringSignatureTest` + BF3 | **KILLED** by `FragmentWiringSignatureTest` |
| F4 a fragment slotted elsewhere is offered again | `BuildRulesTest` + BF4 | **KILLED** by `BuildRulesTest` (3 rows) -- **on the second run**: the first applied as a COMPILE ERROR (its `// MUT_F4` comment swallowed the `.toList();` after it on the line), reported, and re-run with an inline `/* */` marker |
| F5 a duplicate fragment contributes twice | `FragmentContributionsTest` + BF4 | **KILLED** |
| F6 a saved fragment the pool no longer offers is still equipped | `LoadoutResolutionTest` | **KILLED** |
| F7 a pool naming an unknown fragment is accepted | `PoolLoaderTest` | **KILLED** |
| the plan's "drop the cell gate" | BF5 | **NOT RUN, and no unit test can see it:** the gate lives in `Stones.equippedFragments`, which needs a server to construct. **BF5 is its only witness** |
| the plan's "leave `AccessoryNegatives` at `COUNT`" | -- | **NOT RUNNABLE AS WRITTEN:** under ruling 21 the combined bound IS four, so the mutant and the original are the same number. F2 is its replacement: the mutation that would matter now is counting fragments as negative sources |
