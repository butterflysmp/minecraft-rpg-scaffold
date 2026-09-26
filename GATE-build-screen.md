# GATE — the build system, Slice 3: the Build screen

**Status: NOT RUN.** Every prediction below was written **before** any boot. **NO BOOT until the seat has
diffed the PR from origin.** Readings go **beside** a prediction, never over it, and a prediction is not
edited once its row has been read. Readings are verdicts, not figures, unless the row asks for a figure.

```
ROWS     20   R0a R0b R0c BB0 BB1 BB2 BB3 BB4 BB5 BB6 BB7 BB8a BB8b BB8c BB8d BB9 BB10 BB11 BB12 BB13
         ──
         17   = git grep -c '^### R0\|^### BB' <ref> -- GATE-build-screen.md     (headings)
       +  3   BB8's one heading carries FOUR rows, one per gesture, as table rows:
          4   = git grep -c '^| BB8[a-d] |' <ref> -- GATE-build-screen.md          (so 17 - 1 + 4 = 20)
```

**Plan:** `PLAN-build-system.md` §3.3, as built (§3.3.1).

**Declared game mode: SURVIVAL, EVERY ROW.** Nothing on these screens reads the game mode, survival is
what ships, and creative's own-inventory screen decomposes gestures differently (the creative-divergence
register), which BB8 must not be read through.

**Boot:** `./scripts/dev-server.sh --refresh-content`. Restarts inside a row are WITHOUT the flag.

**Set-up shared by most rows:** an op player, survival, the Nexus star and the Ability Stone in the
hotbar. Open the hub by clicking the star. **The Fire Ranger's pool** offers Ultimate: the placeholder
Ranger Ultimate; Actives: Rekindle, Solar Lance, Arc Surge; default Left = Rekindle, Right = Solar Lance.
**The Fire Mage's** offers Ultimate: the placeholder Mage Ultimate; Actives: Ember Step, Solar Grenade,
Solar Lance; default Left = Ember Step, Right = Solar Grenade. Your build file is
`run\plugins\Rpg\builds\<your uuid>.json`.

---

## *** R0 — THE FIRST ROWS OF THE FILE. If any R0 row fails, STOP. ***

### R0a — the build line names this branch's tip

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <tip>` naming the PR's tip SHA -- not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | _(not run)_ |

### R0b — the jar carries the Build screen, and NOT the deleted commands

| prediction | instrument | READING |
|---|---|---|
| `BuildMenu`, `BuildPickerMenu` and core `BuildRules` PRESENT; the control class ABSENT. In `RpgCommand.class`'s bytes: the control string `Players only.` FOUND; the three deleted commands' own strings `Unknown class: `, `has no pool, so it has no loadout to set.` and `/rpg class <class>` NOT FOUND | the scan below | _(not run)_ |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/paper/menu/BuildMenu.class',
               'io/github/butterflysmp/rpg/paper/menu/BuildPickerMenu.class',
               'io/github/butterflysmp/rpg/core/build/BuildRules.class',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$entry = $zip.GetEntry('io/github/butterflysmp/rpg/paper/command/RpgCommand.class')
$ms = New-Object IO.MemoryStream; $s = $entry.Open(); $s.CopyTo($ms); $s.Dispose()
$text = [Text.Encoding]::GetEncoding('ISO-8859-1').GetString($ms.ToArray())
foreach ($needle in 'Players only.', 'Unknown class: ', 'has no pool, so it has no loadout to set.', '/rpg class <class>') {
  if ($text.Contains($needle)) { "FOUND     $needle" } else { "NOT FOUND $needle" }
}
$zip.Dispose()
```

> **The plan named a `BuildDevCommand` class to prove ABSENT. It never existed**: the dev command lived
> inside `RpgCommand`, as `/rpg class` and `/rpg element` did. So the deletion is proven by
> `RpgCommand.class`'s own constant pool: each needle is a string only the deleted code carried, and
> `Players only.` is the control -- a string the surviving commands still use, so a NOT FOUND there
> means the scan is blind, not that the code is gone.

### R0c — the permission node is gone from the deployed plugin

| prediction | instrument | READING |
|---|---|---|
| `paper-plugin.yml` in the jar has `rpg.command.cast:` (the control) and **no** `rpg.command.class:` | `$zip` as above, entry `paper-plugin.yml`, read as text, both needles | _(not run)_ |

---

## THE ROWS

### BB0 — the three commands are gone

| prediction | READING |
|---|---|
| `/rpg class ranger`, `/rpg element fire` and `/rpg build set active1 solar_lance` are each refused by the server as an unknown or incomplete command. Tab completion after `/rpg ` lists **none** of `class`, `element`, `build` | _(not run)_ |

### BB1 — the hub shows Build at 21, left of Equipment, at level 1

| prediction | READING |
|---|---|
| open the hub: a **lectern** named **Build** ("Your class, element and abilities.") sits in row 3, column 4 -- **directly left of** the Equipment armour stand. It is there at level 1 (ungated) and opens the Build screen | _(not run)_ |

### BB2 — a `none` player: class offers Mage and Ranger; element offers Fire only

| prediction | READING |
|---|---|
| `/stop`, delete `run\plugins\Rpg\players\<uuid>.json`, start, join. Left click with the stone: the action bar reads *Choose a class and an element first: open the Nexus, then Build.* Open Build: Class reads **none**, Element reads **none**, and row 2 is three gray panes (*Choose a class and an element first.*). The Class picker offers exactly **Mage, Ranger**; the Element picker offers exactly **Fire** | _(not run)_ |

### BB3 — choose Ranger then Fire: the default loadout, and the stone agrees

| prediction | READING |
|---|---|
| after BB2, pick Class **Ranger** (*"Now choose your element."*), then Element **Fire** (*"You are now Fire Ranger"*). Row 2 reads **Q: Ranger Ultimate (placeholder)**, **Left: Rekindle**, **Right: Solar Lance**, each lore *"... -- the default"*. The stone's lore reads the same three, **without rejoining**. No weapon appears | _(not run)_ |

### BB4 — a picker offers exactly the pool's members in that role

| prediction | READING |
|---|---|
| as the Fire Ranger, open the **Left** picker: exactly **Rekindle, Solar Lance, Arc Surge** -- no Ember Step, no Solar Grenade (Mage-only), no Void Slash (in no pool). The **Q** picker: exactly one option, the Ranger Ultimate | _(not run)_ |

### BB5 — picking the other Active's ability swaps the two

| prediction | READING |
|---|---|
| open the **Left** picker: Rekindle glints and reads *Current*; Solar Lance reads *"In Right now -- choosing it swaps the two."* Choose **Solar Lance**: the Build screen reopens with **Left: Solar Lance, Right: Rekindle**, chat reads *Saved: Left = Solar Lance*, the stone's lore agrees, and left click casts the lance, right click Rekindle | _(not run)_ |

### BB6 — switching cell keeps each cell's choices

| prediction | READING |
|---|---|
| after BB5, Class **Mage**: *"You are now Fire Mage"*, row 2 is the **Mage default** (Left: Ember Step, Right: Solar Grenade). Class **Ranger**: row 2 is **BB5's** build (Left: Solar Lance, Right: Rekindle), not the Ranger default | _(not run)_ |

### BB7 — a cell switch mid-flight: the projectile lands with its original effects

| prediction | READING |
|---|---|
| as the Fire Mage, look well up and right click (Solar Grenade). **While it is still in the air**, click the star, Build, Class, **Ranger**. The grenade lands and bursts **as a Solar Grenade** -- its normal blast and damage on anything under it -- and the Ranger's loadout is now on the stone. (Section 2.6: a live projectile carries its own resolved cast, so the change cannot reach it.) | _(not run)_ |

### BB8a / BB8b / BB8c / BB8d — nothing leaves either screen, by any gesture

**One row per gesture, never collapsed** (GATE-nexus.md ROWS 83-85's method): `MenuRouting` handles
shift-click, the number keys and F on different paths, so one passing says nothing about the others.
Each is read on the **Build screen** AND on **one picker** (the Left picker). Empty your off hand and
hotbar slot 1 first, so a swap has nowhere to hide.

| row | gesture | prediction | READING |
|---|---|---|---|
| BB8a | shift-click an ability icon, the Class icon, and an option | nothing moves; inventory unchanged | _(not run)_ |
| BB8b | press **1** hovering an icon | nothing lands in hotbar 1 | _(not run)_ |
| BB8c | press **F** hovering an icon | nothing lands in the off hand | _(not run)_ |
| BB8d | put any item on the cursor from your inventory, **double-click** it while the screen is open; then try to **drag** it across two icons | the double-click collects nothing from the screen; the drag places nothing on it; the item returns intact on close | _(not run)_ |

### BB9 — the Equipment button and screen are unchanged (the control)

| prediction | READING |
|---|---|
| the hub's slot 22 is still the Equipment armour stand; it opens the Equipment screen with the armour column and accessory column as before, and its Back returns to the hub | _(not run)_ |

### BB10 — a save reaches the FILE within a second, and survives a restart

**The seat's row, closing slice 2's empty-`builds\` question.** Read the file ON DISK, not the screen.

| prediction | READING |
|---|---|
| after BB5, within a second of *Saved: Left = Solar Lance*, open `run\plugins\Rpg\builds\<uuid>.json`: it exists, and holds a `"classId": "ranger", "elementId": "fire"` entry with `"actives": ["solar_lance", "rekindle"]`, the Ultimate named, `"aspects"` 2 nulls and `"fragments"` 4 nulls. (After BB6 there is also a mage/fire entry **only if** a Mage slot was picked; BB6 picks none, so there is **one** entry.) `/stop`, start **without** `--refresh-content`, read the file again: **byte-for-byte the same content**. Rejoin: the Build screen and the stone show Left: Solar Lance, Right: Rekindle | _(not run)_ |

### BB11 — the aspect and fragment rows are "Coming in a later update"

| prediction | READING |
|---|---|
| row 3 holds two **barriers** named *Aspect 1* and *Aspect 2*, row 4 four named *Fragment 1*-*4*, each with the lore *Coming in a later update*. Clicking one does nothing | _(not run)_ |

### BB12 — an unusable build store: the screen says so, and a pick is refused

| prediction | READING |
|---|---|
| `/stop`, copy your build file somewhere safe, then duplicate the ranger/fire entry inside `"cells"`. Start, join: a **SEVERE** line names the file *structurally invalid*. Open Build: the loadout cells show the **Ranger default** with the red line *Build unavailable -- changes cannot be saved.* Choosing any ability in a picker is refused with *Your build is unavailable this session ...* and the screen stays on the picker. `/stop`, restore the file, start: BB5's build is back (the corrupt file was never overwritten) | _(not run)_ |

### BB13 — a non-op's `/rpg cast` with no cell points at the Build screen

| prediction | READING |
|---|---|
| as a **non-op** with class `none` (BB2's state, before BB3), `/rpg cast rekindle` replies *Choose a class and an element: open the Nexus, then Build.* `/op` yourself afterwards | _(not run)_ |

---

## MUTATIONS — run before the PR

Each was applied by literal substitution (exactly once, or aborted), proven applied (marker 1; the lines
removed and added counted against a pristine copy taken BEFORE the edit), tested, restored with `cp`
from the copy, and proven restored (`cmp` identical, marker 0). Each module's reports were read
separately. The predictions were written into the harness before it ran.

| mutation | predicted to redden | delta | result |
|---|---|---|---|
| B1 a slot offers every ability the pool lists, in any role (the plan's "offer every registered ability", at the core seam: core cannot see the registry) | `BuildRulesTest` + BB4 | -1 +1 | **KILLED** by `BuildRulesTest` (2 rows) |
| B2 no swap: picking the other Active's ability leaves it in both | `BuildRulesTest` + BB5 | -1 +1 | **KILLED** by `BuildRulesTest` (3 rows) |
| B3 a real item rendered on the Build screen (a clone of the player's hotbar item) | `BuildScreenWiringSignatureTest` + BB8 | -0 +1 | **KILLED** by `BuildScreenWiringSignatureTest` |
| B4 `/rpg build` comes back | `BuildScreenWiringSignatureTest` + BB0 | -0 +1 | **KILLED** by `BuildScreenWiringSignatureTest` |
| B5 picking a class always keeps the element | `BuildRulesTest` | -1 +1 | **KILLED** by `BuildRulesTest` (1 row) |
| B6 every pooled element offered to every class | `BuildRulesTest` | -1 +1 | **KILLED** by `BuildRulesTest` (2 rows) |
| B7 Build moves off slot 21 | `NexusMenuLayoutTest` + BB1 | -1 +1 | **KILLED** by `NexusMenuLayoutTest` |

**Not guarded by any unit test, and said so:** the picker's USE of `BuildRules` -- a `BuildPickerMenu`
that listed the ability registry instead of `BuildRules.choices` would pass every row above, because
paper tests have no server to render a menu. **BB4 is its only witness.**
