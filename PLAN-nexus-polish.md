# PLAN — NEXUS POLISH: THE HUB AND EVERY SCREEN BEHIND IT

**Phase 1: SURVEY ONLY. No Java, no content, no boot, and no decision made on Ben's behalf.** Read against master
**`2d60e9a3`** (`2d60e9a3a2b76dcc92ac16bbe6fa3468337580c1`, quoted from `git ls-remote origin master` on 2026-09-28,
#167). The stacking base named in the brief is **`origin/feat/melee-m1` at `e305ab3a`**
(`e305ab3aade3cdf49f248814c53bcc73611cfc20`, same `ls-remote`; PR #168, `OPEN` per `gh pr view 168`). Citations
name METHODS and SECTIONS, never line numbers. Short paths are `PLAN-build-system.md`'s: `P/` =
`paper/src/main/java/io/github/butterflysmp/rpg/paper/`, `C/` = `core/src/main/java/io/github/butterflysmp/rpg/core/`,
`content/` = `paper/src/main/resources/content/`.

**The brief is not in this repo.** It came from the seat: *"Polish of the Nexus Menu and all things inside of it."*

**Engine claims are read from the pinned API jar, not from memory.** `pom.xml`'s `paper.version` is
`26.1.2.build.74-stable`; the jar is `~/.m2/.../paper-api-26.1.2.build.74-stable.jar`, read with `javap` from
`.jdks/openjdk-26.0.1`. **A claim marked UNVERIFIED is client behaviour or vanilla behaviour this survey did not
read from a jar**, and a gate row reads it before anything rests on it.

**How to read the marks.** **READ** = I read it in the code or doc named. **INFERRED** = follows from code I read,
but nobody has watched it happen in game. **UNVERIFIED** = neither. Every number that is not already ruled is left
as `___ (Ben)`.

**Who rules what.** Items marked **MECHANISM** are the seat's (the technical reviewer). Items marked
**LOOK/FEEL/WORDING** are Ben's: options are given, **none is picked**.

---

## RULINGS — DECIDED; NOT RE-DERIVED HERE

### BEN'S BRIEF, 2026-09-29, VERBATIM (via the seat)

Copied byte for byte from the seat's file (`ben-nexus.txt` in the session scratchpad; not committed), inside a fence
so no markup touches it:

```
Ben: "I want to polish up the various buttons and their names and lore. Make them stand out
more and add slash commands for players to easily reach them. We'll talk more about this in a
minute."
```

**What this survey takes from it, and nothing more:** the weight is on **buttons — their names and lore — and making
them stand out** (§4.0b), plus **player slash commands to reach the screens** (§9). Everything else in the original
brief (every screen, every inconsistency) is still surveyed, at lower weight. **"We'll talk more about this in a
minute"** means more is coming: see *BEN'S FURTHER SPECIFICS (pending)*, below.

### BEN'S FURTHER SPECIFICS (pending)

*(Empty on purpose. The seat has said more is coming; it goes here verbatim when it arrives.)*

### THE STANDING RULINGS THIS SURVEY MUST NOT RE-DERIVE

These are already decided, in code javadoc or earlier plans. The polish list below works **around** them. Where an
item would touch one, it says so and becomes a question, never a quiet change.

| ruled | where it is recorded (READ) |
|---|---|
| The hub is **54 slots**, and the size is Ben's call, not derived | `NexusMenuLayout`, class javadoc, *THE SIZE IS RULED, NOT DERIVED* |
| The hub is **BANDS**: row 2 header (the head alone), row 3 other features, row 4 crafting-type menus, row 5 unassigned, row 6 chrome. **No fill order inside a band; a feature's KIND picks its band** | `NexusMenuLayout.STATS_SLOT` javadoc, *THE HUB IS BANDS WITH MEANINGS* |
| The station row is **29 vault, 30 anvil, 31 crafting, 32 enchanting, 33 grindstone** (the predecessor's row) | `NexusMenuLayout.GRINDSTONE_SLOT` and `VAULT_SLOT` javadoc |
| **Close is 49 on every screen**; hub Settings is **50**, adjacent to Close | `NexusMenuLayout.CLOSE_SLOT`, `SETTINGS_SLOT` |
| A back button is **named for its destination** ("Back to the Nexus", never a bare "Back"), **no lore**, material **ARROW** (overturning an earlier contextual-icon ruling), at **48** | `MenuIcons.backName` and `MenuIcons.back` javadoc, *ARROW, AND THAT OVERTURNS AN EARLIER RULING* |
| Close is **name-only**: the lore line was taken off *"for a calmer screen"* | `NexusCollisionNotice` class javadoc, *NO SOUND…* |
| **The Nexus is kept quiet on purpose** (Ben's instruction): the star lock refuses **silently** (no chat, no sound, no title), and the collision notice plays **no sound** | `NexusCollisionNotice`, *NO SOUND, AND THAT IS A DELIBERATE DEPARTURE FROM BrokenNotice*; `NexusOpenGesture` class javadoc |
| **The grindstone's silence ruling:** *"a click on a button that is not LIME does nothing and says nothing, because the button has already said it"* — and it **does not transfer** to locked hub stations, which speak | `NexusStationGate.refusal` javadoc, *THE GRINDSTONE'S SILENCE RULING DOES NOT TRANSFER* |
| Station unlock levels **3 / 7 / 10 / 13 / 20** (crafting, anvil, enchanting, grindstone, vault shortcut); vault pages **free, 25, 30, 35, 40, 45, 50** | `NexusStationGate.Station`; `C/vault/VaultPageGate` |
| Filler is **BLACK** stained glass, changed in one place so every screen moves together; **LIGHT_GRAY** is the empty quick-craft cell and **GRAY** is the crafting status bar's EMPTY colour — *"anything that collapses [them] is a REGRESSION"* | `MenuIcons.FILLER`, `MenuIcons.EMPTY_SUGGESTION` |
| The star's name and the hub's title are both **"Nexus Menu"** (Ben: *"both, they should match"*, then gave the string) | `NexusMenu` constructor comment |
| Equipment columns 5-7, rows 1-4 (**slots 14-16, 23-25, 32-34, 41-43**) are **RESERVED for Slice C (Equipment Sets)** | `EquipmentMenuLayout` class javadoc; `EquipmentMenuLayoutTest.theSliceCReservationIsPlainFiller` |
| The star opens the hub on **LEFT or RIGHT click on its own slot, empty cursor, own-inventory screen**; **creative is excluded by the click type**, and widening it is *"Ben's question, not this slice's"* | `NexusOpenGesture` class javadoc |
| The anvil's preview cell (24) **never becomes cargo** — *"Making 24 takeable is a slice, not a patch"* | `AnvilMenuLayout` output-slot javadoc; `AnvilMenu`'s preview-lore comment |

---

## 1. REACHABILITY — TRACED FROM THE CODE, NOT ASSUMED

### 1.1 The three doors onto the hub (READ)

| door | where | how it opens |
|---|---|---|
| Right-click (air or block) holding the star | `RpgListeners`, the interact handler's `NexusItems.isNexus` branch | direct `new NexusMenu(...).open()`, no hop; the event is cancelled unconditionally. A right-click on a hijacked block also sends `NexusCollisionNotice.shadowedBlock` |
| Left/right click on the star's own inventory slot | `RpgListeners`, the own-inventory click handler, via `NexusOpenGesture.opensHub` | `scheduler().onEntity(...)` hop |
| `/menu` | `P/command/MenuCommand.build` | direct open; refuses a non-player and a profile that is loading/unreadable. Permission `rpg.command.menu`, `default: true` (`paper-plugin.yml`) |

### 1.2 The graph (READ from every `onClick`)

```
Nexus Menu (hub)
├─ 13  Your Stats (player head) ........ no click handler: inert today (PLAN-level-bonuses.md proposes a screen)
├─ 21  Build ─────────────── BuildMenu
│                             ├─ 3/5      class / element ─── BuildPickerMenu (CLASS / ELEMENT)
│                             ├─ 11/13/15 Q / Left / Right ─── BuildPickerMenu (ULTIMATE / ACTIVE_1 / ACTIVE_2)
│                             ├─ 21/23    Aspect 1-2 ──────── BuildPickerMenu (ASPECT, index)
│                             └─ 28-34    Fragment 1-4 ────── BuildPickerMenu (FRAGMENT, index)
│                                          every picker: Back → a NEW BuildMenu; a successful pick also → BuildMenu
├─ 22  Equipment ─────────── EquipmentMenu (armour + accessories, live edits)
├─ 29  Vault (lvl 20) ────── NexusVaultMenu (7 pages)
├─ 30  Anvil (lvl 7) ─────── AnvilMenu
├─ 31  Crafting (lvl 3) ──── CraftingMenu ── 26 Recipe Book ── RecipeBrowserMenu ── 48 "Back to crafting"
│                                                                  └─→ a NEW CraftingMenu WITHOUT the hub (F1)
├─ 32  Enchanting (lvl 10) ─ EnchantMenu (unpowered: no block, bookshelf power 0)
├─ 33  Grindstone (lvl 13) ─ GrindstoneMenu
├─ 49  Close
└─ 50  Settings ──────────── SettingsMenu
                              ├─ 22 Nexus Slot ─────────── NexusSlotPickerMenu (STAR)
                              ├─ 24 Nexus Star ON/OFF ···· toggles in place
                              ├─ 31 Ability Stone Slot ─── NexusSlotPickerMenu (STONE)
                              └─ 33 Ability Stone ON/OFF ·· toggles in place
```

**Twelve screen classes are reachable from the star** (the hub, Settings, the slot picker, Build, the Build picker,
Equipment, Vault, Crafting, the recipe browser, Anvil, Enchanting, Grindstone). The brief's list is complete except
that **two names in it are not screens** (READ):

- **`CraftMatrixScreen` is not a screen.** It is a static verdict (`verdict`, `isGear`) on a VANILLA crafting matrix
  — does it hold our gear. Nothing in it is shown. It is out of scope for a menu polish.
- **`NexusStationGate` and `NexusStatsLore` are not screens** either: the level lock for the five station buttons,
  and the lore builder for the head.

**A SCREEN-TO-BE, NOT DESIGNED HERE: the level screen behind the head.** A separate survey, `PLAN-level-bonuses.md`
(being drafted in parallel; **not in the repo at `2d60e9a3`**), proposes that clicking the head (13) opens a new
sub-menu: level, XP to next, and a paged list of levels 1-50 with their bonuses and unlocks. **This plan does not
design it.** It is listed so the two surveys do not both claim the head: §4.2's S3 defers to that plan, and §5.6 names
the files the two would share.

### 1.3 The same screens, reached WITHOUT the star (READ, `RpgListeners`, the `hijackedBlocks` table)

Right-clicking a world **enchanting table, crafting table, grindstone, anvil (all three materials) or ender chest**
opens our screen for it, **with no hub supplier**. Each screen then drops its Back button: crafting, anvil and
grindstone paint the status bar through 48; the enchant table and the vault paint filler there. **A polish change to
any station screen changes the world-block route too.** Nothing in the polish list below may assume a station screen
was opened from the hub.

---

## 2. INVENTORY — SCREEN BY SCREEN (all READ unless marked)

### 2.0 What every screen shares

- **Base class `Menu`.** Every click on the open menu is **cancelled first** (`Menu.handleClick`), then routed by
  `MenuRouting.route`. A screen's `onClick` sees a `MenuClick(slot, click type, action, itemMoved)`.
- **Which clicks reach a button (READ in `MenuRouting.route`; INFERRED in game).** A click in the top inventory on a
  non-input slot reaches `onClick` **whatever its click type**, except: number keys and the offhand key (only act on
  input slots), double-click (collect-to-cursor, input slots only), `CREATIVE`, and the actions `COLLECT_TO_CURSOR`,
  `CLONE_STACK`, `UNKNOWN`. **Shift-click reaches a button only where the screen opts in** (`shiftClickDispatches`:
  crafting's result and suggestions, the recipe browser's entries, Equipment's armour and accessory cells). So on
  the hub, **the Q (drop) key or a middle-click over a button acts as a click, while shift-click does nothing**. Only
  `EquipmentMenu.onClick` filters click types (`if (!shift && !plain) return;`). See F4.
- **Title:** every screen's title is plain text in `DARK_GRAY` through `MenuIcons.line`.
- **Size:** all twelve are **54**.
- **Filler:** `MenuIcons.filler()` — BLACK pane, empty display name. `MenuIcons.pane(material)` is a second method
  with the identical body, used for readouts (status bars, empty suggestions).
- **Close:** `MenuIcons.close()` — `BARRIER`, name **"Close"** in `RED`, no lore. It calls `viewer.closeInventory()`.
- **Back:** `MenuIcons.back(Material.ARROW, destination)` — name **"Back to <destination>"** in `GRAY`, no lore.
- **SOUNDS: NONE.** `grep` for `playSound`, `org.bukkit.Sound` and `Sound.` across `P/menu/` and `P/nexus/` finds
  **no call at all**. The only sound-playing classes in `P/` are `PaperCombatWorld`, `VisualSpec`, `BrokenNotice`,
  `PlumeDraw`, `PlumeNotice`, `QuiverNotice`, `QuiverReloadCue` and `ShieldBrokenNotice`. **Every screen below is
  silent on open, click, success and refusal**, including the anvil transfer, the grindstone strip and an enchant
  unlock, whose world blocks make a sound in vanilla (UNVERIFIED: that the server, not the client, plays those; it
  does not matter here, because our screens replace the vanilla ones).
- **"Placeholder" icons: none.** `MenuIcons.placeholder(...)` (*"Not implemented yet."*) has **zero callers**. Its
  javadoc keeps it as *"a decision with a date on it"* because *"the anvil, class-select and stat screens are still
  ahead"*, and says *"Delete it if a third graduation arrives with none of those screens built."* The anvil and the
  class select (the Build screen) are now built. See F9.
- **TODO / FIXME / TEMP markers:** `grep -n 'TODO\|FIXME\|TEMP\|XXX'` over `P/menu/` and `P/nexus/` finds **none** in
  code. The only `TEMP` hits are javadoc naming the `health_boost_TEMP` dev star (`NexusItems`).

### 2.1 The hub — `NexusMenu`, layout `NexusMenuLayout`

**Title:** "Nexus Menu". **Opened by:** §1.1. **Back:** none (it is the root). **Close:** 49. **Sounds:** none.

```
row 1   0  1  2  3  4  5  6  7  8      all filler
row 2   9 10 11 12 [13] 14 15 16 17     13 = Your Stats (head)
row 3  18 19 20 [21][22] 23 24 25 26    21 = Build, 22 = Equipment
row 4  27 28 [29][30][31][32][33] 34 35 29 Vault, 30 Anvil, 31 Crafting, 32 Enchanting, 33 Grindstone
row 5  36 .. 44                         all filler ("unassigned")
row 6  45 46 47 48 [49][50] 51 52 53    49 Close, 50 Settings
```

| slot | item | name (colour) | lore, verbatim (all `DARK_GRAY`) | click |
|---|---|---|---|---|
| 13 | `PLAYER_HEAD` (the viewer's skin) | "Your Stats" (`GOLD`) | see §2.2 | nothing |
| 21 | `LECTERN` | "Build" (`GRAY`) | "Your class, element and abilities." | opens Build |
| 22 | `ARMOR_STAND` | "Equipment" (`GRAY`) | "Your armour and accessories." | opens Equipment |
| 29 | `ENDER_CHEST` | "Vault" | "Seven pages, 252 slots." / "Pages open as you level." (the 252 is `VaultShape.TOTAL_SLOTS`; "Seven" is a literal word) | opens Vault (lvl 20) |
| 30 | `ANVIL` | "Anvil" | "Move a gear score onto a better item." / "Same kind only, and the sacrifice must score higher." | opens Anvil (lvl 7) |
| 31 | `CRAFTING_TABLE` | "Crafting" | "The full grid, and the recipe book." | opens Crafting (lvl 3) |
| 32 | `ENCHANTING_TABLE` | "Enchanting" | "Unpowered -- no bookshelves here." / "A real table with shelves reaches 30." | opens Enchanting (lvl 10) |
| 33 | `GRINDSTONE` | "Grindstone" | "Strip enchants from your gear." / "Refunds 35% of what they cost." (35 is `GrindstoneRefund.REFUND_PERCENT`) | opens Grindstone (lvl 13) |
| 49 | `BARRIER` | "Close" (`RED`) | — | closes |
| 50 | `REDSTONE_TORCH` | "Settings" (`GRAY`) | "Choose where the Nexus sits." | opens Settings |

**A locked station (`NexusMenu.station`)** keeps its material, dims its name to `DARK_GRAY`, and swaps the lore for
`NexusStationGate.lockedLore`: *"Locked -- unlocks at level N"* / *"You are level L."* / the world route —
*"A crafting table in the world still works."*, *"An anvil in the world still works."*, *"An enchanting table in the
world still works."*, *"A grindstone in the world still works."*, *"An ender chest opens this same vault."*
**Clicking it** sends one `GRAY` chat line, `NexusStationGate.refusal`: *"Grindstone unlocks at level 13. You are
level 7. A grindstone in the world still works."*

**How children are opened:** every button builds the child inside `scheduler().onEntity(viewer, ...)` and hands it a
`Supplier<Menu>` that builds a **fresh** hub for its Back button. The hub is never re-used; it re-reads level, stats
and gates each time it is built. **It does not repaint while open** (INFERRED: `render()` runs once, from the
constructor, and nothing else calls it), so a level-up while it is open shows a stale lock until it is reopened.

### 2.2 The stats head — `NexusStatsLore`

Name **"Your Stats"** (`StatsSheetLines.HEADER`, `GOLD`). Lore, top to bottom, with **no blank line between the
blocks** (READ in `NexusStatsLore.lore` and `NexusMenu.render`):

1. **Progression** (only if the profile is loaded): "Level" (value `GOLD`), "Lifetime XP", "To Next" (dropped at
   level 99). Label `DARK_GRAY`, padded to 13 characters by `StatsSheetLines.label`. **No icon, no indent.**
2. **Gear Score**: "Gear Score" + `GearScoreItems.averageOf` (value `AQUA`). No icon, no indent.
3. **The stat sheet** (`StatsSheet.statLines`): "Max Health", "Health Regen", "Max Mana", "Mana Regen", "Defense",
   "Damage", "Crit Chance", "Crit Damage", and "Quiver"/"Reload" when a quiver weapon is held. **These carry an icon
   or a two-space indent, and a `GRAY` label** — a different shape from the two blocks above. If untracked: one
   `RED` line, *"No stats tracked yet -- try rejoining."*
4. **Accessories** (`AccessorySheet.lines`): a `GOLD` "Accessories" header then indented lines, or *"unavailable
   this session -- see the server log"* in `RED`.
5. **Fragments** (`FragmentSheet.lines`): a `GOLD` "Fragments" header, numbers-only fragments only; behaviour
   fragments are skipped.

**Not shown on the head (READ):** class, element, or the loadout. The `NexusMenu` constructor javadoc says the
services are threaded *"so that the slice which makes the head clickable can REPAINT it"* — **a clickable head was
anticipated and never built.**

### 2.3 Settings — `SettingsMenu`, layout `SettingsMenuLayout`

**Title:** "Nexus Settings". **Back:** 48, "Back to the Nexus" (a fresh hub). **Close:** 49. **Sounds:** none.
Painted: **22, 24, 31, 33, 48, 49**; everything else filler.

| slot | item | name | lore (`DARK_GRAY`) | click |
|---|---|---|---|---|
| 22 | `ITEM_FRAME` | "Nexus Slot" (`GRAY`) | "Currently: <slot>" / "Click to choose a different cell." | opens the slot picker (STAR) |
| 24 | `LIME_DYE` / `GRAY_DYE` | "Nexus Star: ON" (`GREEN`) / "Nexus Star: OFF" (`GRAY`) | ON: "Click to remove it and free the slot." — OFF: "Click to put it back in <slot>." / "/menu opens this hub either way." | toggles in place and repaints |
| 31 | `ITEM_FRAME` | "Ability Stone Slot" (`GRAY`) | "Currently: <slot>" / "Click to choose a different hotbar cell." | opens the slot picker (STONE) |
| 33 | `LIME_DYE` / `GRAY_DYE` | "Ability Stone: ON" / "Ability Stone: OFF" | ON: "Click to remove it and free the slot." — OFF: "Click to put it back in <slot>." | toggles in place |

`<slot>` is `NexusSlotPickerLayout.slotName` ("Hotbar 9", "Row 1, slot 1") or the literal **"not known yet"** when
the profile has no slot, which renders as *"Click to put it back in not known yet."* (INFERRED).

**Chat (`setStar`, `setStone`):** occupied → `RED` *"<slot> is occupied. Clear it or use the slot picker first."* (the
stone's says *"...use the Ability Stone slot picker first."*); on → `AQUA` *"The Nexus is back, in <slot>."* / *"The
Ability Stone is back, in <slot>."*; off → `AQUA` *"The Nexus is off. <slot> is yours to use; open this menu again
with /menu."* / *"The Ability Stone is off. <slot> is yours to use."*; profile loading/unreadable →
`ProfileService.STILL_LOADING` (`GRAY`) / `UNREADABLE_PROFILE` (`RED`).

### 2.4 The slot picker — `NexusSlotPickerMenu`, layout `NexusSlotPickerLayout`

**Title:** "Nexus Slot" or "Ability Stone Slot". **Back:** 48, **"Back to Settings"**. **Close:** 49. **Sounds:** none.
Menu slots **9-44** mirror the player's inventory: **9-35 = storage rows 1-3, 36-44 = hotbar 1-9** (the hotbar sits at
the bottom, as it does in the real inventory). Row 1 (0-8) and row 6 (except 48/49) are filler. For the STONE target,
every storage cell above `LockedSlots.STONE_MAX_SLOT` is painted **filler** (the stone is hotbar-only).

| cell state (`cellIcon`) | item | name | lore |
|---|---|---|---|
| where this item sits now | `LIME_STAINED_GLASS_PANE` | "Current slot (<slot>)" `GREEN` | "The Nexus sits here." / "The Ability Stone sits here." |
| where the OTHER locked item sits | **`BARRIER`** | "Reserved (<slot>)" `RED` | "Holds your Ability Stone." / "Holds your Nexus." |
| empty | `LIGHT_GRAY_STAINED_GLASS_PANE` | "Empty (<slot>)" `GRAY` | "Click to move the Nexus here." |
| occupied by the player's item | **a clone of that item**, its own tooltip | — | — |

**Click:** a cell → `choose`; a reserved cell → `RED` *"<slot> holds your <other>. Choose another cell."*; success →
`AQUA` *"The Nexus now sits in <slot>."* (with *" -- once you switch it back on."* when it is off). What happens to an
item already in the chosen cell is `NexusSlots.converge`'s business (not re-read for this survey).

### 2.5 Build — `BuildMenu`, layout `BuildMenuLayout`

**Title:** "Build". **Back:** 48 "Back to the Nexus". **Close:** 49. **Sounds:** none.

```
row 1   .  .  .  [3] .  [5] .  .  .      3 Class, 5 Element
row 2   .  . [11] . [13] . [15] .  .     Q (Ultimate), Left (Active 1), Right (Active 2)
row 3   .  .  .  [21] . [23] .  .  .     Aspect 1, Aspect 2
row 4   . [28] . [30] . [32] . [34] .    Fragment 1-4
row 5   all filler
row 6   .  .  .  [48][49] .  .  .  .     Back, Close
```

| slot | item | name | lore | click |
|---|---|---|---|---|
| 3 | `NAME_TAG` | "Class: " + capitalised id in `WHITE` (e.g. "Class: Ranger"), or "Class: none" | "Click to choose." — or, profile missing, the loading/unreadable text in `RED` | class picker |
| 5 | `GLOWSTONE_DUST` | "Element: " + the element's MiniMessage `display_name` | "Click to choose." | element picker |
| 11 / 13 / 15 | `AMETHYST_CLUSTER` (Q) / `PRISMARINE_SHARD` (Left, Right) | "Q: " / "Left: " / "Right: " + the ability's `display_name`, or "(empty)" | "Ultimate" or "Active", plus *" -- the default"* when unchosen; the ability's `description` in `GRAY`; `numberLines` ("Damage: …", "Cost: …", "Cooldown: 2.0 s", with the un-aspected base in brackets when an aspect moved it); a blank; "Click to change." — or `RED` *"Build unavailable -- changes cannot be saved."* | ability picker |
| 11 / 13 / 15 when the class or element is unchosen | `GRAY_STAINED_GLASS_PANE` | "Q: Ultimate" etc. `DARK_GRAY` | "Choose a class and an element first." | chat, `YELLOW`: same sentence |
| 21, 23 | `FIRE_CHARGE` / `LIGHT_GRAY_STAINED_GLASS_PANE` if empty / `GRAY_STAINED_GLASS_PANE` if no cell | "Aspect 1: <name>" / "Aspect 1: (empty)" / "Aspect 1" | `RED` *"Inactive -- requires <ability> equipped"* when its target is not equipped; `aspectLore` ("Changes <ability>" `GOLD`, each change in `BLUE`, description `GRAY`); blank; "Click to choose." / "Click to change." | aspect picker |
| 28-34 | the fragment's own `icon` material (**`BARRIER` if the material name is bad**) / panes as above | "Fragment 1: <name>" etc. | behaviour fragments: `RED` *"Inactive -- requires <ability> equipped"*; `fragmentLore` ("Changes <ability>" `GOLD`, *"Adds N effect(s) on hit"* `BLUE`, modifiers `BLUE`, description `GRAY`); footer as above | fragment picker |

### 2.6 The Build pickers — `BuildPickerMenu`

**Title:** "Choose a Class", "Choose an Element", "Choose an Ultimate (Q)", "Choose an Active (Left)", "Choose an Active
(Right)", "Choose an Aspect (slot N)", "Choose a Fragment (slot N)". **Slot 4** repeats the title as an icon (the
kind's material, `GRAY`, no lore). **Options:** the 28 cells 10-16, 19-25, 28-34, 37-43, filled in order; unused
cells are filler. **Back:** 48 **"Back to Build"**. **Close:** 49. **Sounds:** none.

- An option's name is the display name (class: capitalised id in `WHITE`), with lore by kind: abilities get
  `BuildMenu.abilityLore` (description + **"Cooldown: 2s"**), aspects `aspectLore`, fragments `fragmentLore`. The
  other Active's pick carries `YELLOW` *"In Right now -- choosing it swaps the two."*; the current choice ends
  **"Current"** in `GREEN` with an enchant glint; the rest end "Click to choose.".
- An aspect or fragment slot that holds something offers one extra option, `EMPTY_THE_SLOT`: **`BARRIER`**, "Empty this
  slot", lore **"Click to remove the fragment here." — also on the ASPECT picker** (`optionIcon`; see F2).
- **A pick saves and returns to Build.** Chat: `AQUA` *"You are now <pool display name>"* (class/element completing a
  cell) or `YELLOW` *"Now choose your element."* / *"Now choose your class."*; `AQUA` *"Saved: Left = <name>"*,
  *"Saved: Aspect 1 = <name>"*, *"Saved: Fragment 1 = <name>"*; `RED` failures (*"<id> is not offered there."*,
  *"<id> cannot go in that slot."*, *"Your build is still loading -- try again in a moment."*, *"Your build is
  unavailable this session (see the server log); nothing can be saved."*, *"The build could not be written; see the
  server log."*). **A refused pick stays on the picker.**

### 2.7 Equipment — `EquipmentMenu`, layout `EquipmentMenuLayout`

**Title:** "Equipment". **Back:** 48 "Back to the Nexus". **Close:** 49. **Sounds:** none. **The only screen that
edits live gear in place** (the player's armour slots and the accessory store).

```
row 1   . [1][2] .  .  .  .  .  .      1 "Accessories" (ECHO_SHARD), 2 "Armour" (IRON_CHESTPLATE, attributes hidden)
row 2   . [10][11] .  .  R  R  R  .    10 accessory 0 (class slot), 11 Head
row 3   . [19][20] .  .  R  R  R  .    19 accessory 1, 20 Chest
row 4   . [28][29] .  .  R  R  R  .    28 accessory 2, 29 Legs
row 5   . [37][38] .  .  R  R  R  .    37 accessory 3, 38 Feet
row 6   .  .  .  [48][49] .  .  .  .   R = reserved for Slice C
```

- **Armour cell:** the worn piece (a clone), or `GRAY_STAINED_GLASS_PANE` "Head"/"Chest"/"Legs"/"Feet" + "Empty".
  Left/right = place one / take / swap via `ArmorPlacement.decide`; shift = take out to the inventory. Every refusal
  but two is **silent** (`REFUSE_WRONG_SLOT`, `REFUSE_NOT_FOR_PLAYER`, `REFUSE_OCCUPIED`, `REFUSE_STACK`,
  `REFUSE_STAR` → `{ }`); `REFUSE_BOUND` says *"That is bound to you: Curse of Binding."*; a full inventory says *"There
  is no room in your inventory."* Shift-clicking armour **from the player's inventory** equips it (`shiftInElsewhere`).
- **Accessory cell:** the item, with `RED` *"Inactive — requires class: <class>"* appended when it does not
  contribute (**an em dash**, where every other screen writes `--`); empty class slot = `ORANGE_STAINED_GLASS_PANE`
  "Class accessory slot" (`GOLD`) or **`BARRIER`** "Choose a class to unlock" (`RED`); empty universal slot =
  `LIGHT_GRAY_STAINED_GLASS_PANE` "Universal accessory slot"; store loading = `GRAY_STAINED_GLASS_PANE` "Loading...";
  store dead = **`BARRIER`** "Accessories unavailable" / "This session; see the server log."; unreadable = **`BARRIER`**
  "Unreadable item" / "Kept, not deleted. Contributes nothing."
- **Chat** is `GRAY` through `say`: *"Only accessories go in these slots."*, *"The class slot is locked: choose a class
  first."*, *"This slot takes a class accessory."*, *"These slots take universal accessories; a class accessory goes
  in the top one."*, *"That accessory is for <class>."*, *"Take the accessory out first."*, the FAILED-to-reach-disk
  lines, and others.
- **Only this screen filters click types**: anything but LEFT/RIGHT/SHIFT_LEFT/SHIFT_RIGHT returns early.

### 2.8 Vault — `NexusVaultMenu`, layout `NexusVaultLayout`

**Title:** "Vault" (the page is not in the title). **Back:** 48 "Back to the Nexus" **only from the hub**; filler from
an ender chest. **Close:** 49. **Sounds:** none.

```
row 1   [0] [1][2][3][4][5][6][7] [8]   0 Previous page, 1-7 page buttons, 8 Next page
rows 2-5  9 .. 44                       the page's 36 storage cells (live, stacking)
row 6   .  .  .  [48][49] .  .  .  .
```

- **Previous / Next:** `ARROW`, "Previous page" / "Next page" (`GRAY`), lore "Page N" (`DARK_GRAY`); filler at the
  ends.
- **Page button (`pageButton`):** locked = `GRAY_STAINED_GLASS_PANE` "Page N -- locked" (`DARK_GRAY`) / "Unlocks at
  level X" / "You are level L."; current = `LIME_STAINED_GLASS_PANE` "Page N" (`GREEN`) / "You are here."; other =
  `WHITE_STAINED_GLASS_PANE` "Page N" / "Click to open."
- **Storage when dead (`paintDeadStorage`):** locked page = 36 × `GRAY_STAINED_GLASS_PANE` "Locked" / "Unlocks at
  level X"; vault unavailable = 36 × `RED_STAINED_GLASS_PANE` "Vault unavailable" / *"A write failed. Nothing more
  will be saved this session."* or *"Still loading -- try again in a moment."*; an unreadable entry = **`BARRIER`**
  "Unreadable item" / "This server cannot read it." / "It is KEPT, not deleted."
- **Chat:** a locked page flip → `GRAY` `VaultPageGate.refusal` (*"Vault page 3 unlocks at level 30. You are level
  22."*); the `RED` degrade lines (*"...CLOSE THIS SCREEN and your items will be handed back to you."*); the `AQUA` /
  `YELLOW` one-time migration lines.
- **Back closes first** (`closeInventory()` then `onEntity` → hub), because the vault has input cells.

### 2.9 Crafting — `CraftingMenu`, layout `CraftingMenuLayout`

**Title:** "Crafting". **Back:** 48 "Back to the Nexus" **only from the hub**. **Close:** 49. **Sounds:** none.

```
row 1   .  .  .  . [4] .  .  .  .      4 "Crafting" (CRAFTING_TABLE, WHITE): "Lay a recipe in the grid," / "or click a suggestion."
row 2   . [10][11][12] .  . [16] .     3x3 grid 10-12/19-21/28-30; suggestions 16/25/34
row 3   . [19][20][21] . [23] . [25][26]  23 result; 26 "Recipe Book" (KNOWLEDGE_BOOK, GREEN)
row 4   . [28][29][30] .  .  . [34] .
row 5   all filler
row 6   status bar (GRAY / LIME / RED panes) except 49 Close, and 48 Back from the hub
```

- **Recipe Book (26):** lore "Everything you can craft right now" (`GRAY`) / "Click to browse" (`DARK_GRAY`).
  Click → `closeInventory()`, then `onEntityLater(..., 1)` → `new RecipeBrowserMenu(viewer, adapters, catalogue)` —
  **no hub supplier is passed** (F1).
- **Suggestions (16/25/34):** the result item with lore "Craft N more" / "Uses items from your inventory"; an empty
  cell is a `LIGHT_GRAY_STAINED_GLASS_PANE`. Click crafts one; shift-click crafts in bulk.
- **Result (23):** click takes one to the cursor; shift-click crafts repeatedly; double-click is ignored.
- **Chat (`say`, `GRAY`):** *"Your cursor is holding something else."*, *"No room on your cursor for that."*, *"Your
  inventory is full -- made N."*, *"That recipe is no longer available."*, *"You no longer have the materials for
  that."*
- **Back closes first**, then opens the hub **one tick later** (`onEntityLater(..., 1)`).

### 2.10 The recipe browser — `RecipeBrowserMenu`, layout `RecipeBrowserLayout`

**Title:** **"Recipes"** (the button that opens it says "Recipe Book"). **Back:** 48 — **a `CRAFTING_TABLE` named "Back
to crafting"**, minted inline, **not** `MenuIcons.back` and **not** an arrow. **Close: NONE** — 49 is the page readout.
**Sounds:** none.

- **Entries:** slots **0-44** (45 per page), each the result item with lore "Craft N more", the ingredient lines
  (`IngredientLore.of`), "Uses items from your inventory". Click crafts one from the inventory; shift-click crafts in
  bulk. Chat: *"Your inventory is full -- made N."*, **"You do not have the materials for that."** (crafting's
  suggestion says *"You no longer have…"*), *"That recipe is no longer available."*
- **Empty state (22):** `BARRIER`, "Nothing you can make right now" (`GRAY`), no lore.
- **Footer:** 45 "Previous page" (`ARROW`), 49 `PAPER` "Page N of M" (`WHITE`) / "K you can craft now", 53 "Next page"
  (`ARROW`), 48 Back, the rest filler.
- **Back** builds a NEW `CraftingMenu` with the three-argument constructor (no hub) **without closing first**.

### 2.11 Anvil — `AnvilMenu`, layout `AnvilMenuLayout`

**Title:** "Anvil". **Back:** 48 from the hub (closes, then one tick later the hub). **Close:** 49. **Sounds: none,
including on a successful transfer.**

```
row 1   .  .  .  . [4] .  .  .  .      4 info (ANVIL, "Anvil", WHITE)
row 3   .  . [20] . [22] . [24] .  .   20 target (input), 22 sacrifice (input), 24 preview (never takeable)
row 4   .  .  .  . [31] .  .  .  .     31 confirm
row 6   status bar (7 cells + Back + Close from the hub; 8 + Close from a block)
```

- **Info (4):** "Move a gear score onto a better item." / "Same kind only, and the sacrifice must score higher." /
  blank / "Preview only -- nothing is spent or consumed yet." (`DARK_GRAY`)
- **Confirm (31, `confirmIcon`):** a dye (`AnvilFace.dyeFor`: lime ready, yellow arming, red refused, gray empty),
  named by the face text — **"Raise to 310 -- 910 XP"**, **"Arming... 3"**, or the refusal sentence — in the state's
  colour; lore **"Moves the sacrifice's score onto the item above."** / "The sacrifice is consumed." / blank / "This
  cannot be undone." (F3: the cell directly above 31 is **22, the sacrifice**.) Arming is 60 ticks
  (`AnvilButton.ARM_TICKS`).
- **Preview (24):** the upgraded item with `ready.sentence()`, "Costs N XP.", "Preview only -- you cannot take this.",
  **"Confirm upgrades the item on the LEFT."**; or a `BARRIER` with the refusal / shortfall sentence and *"Nothing is
  spent until you confirm."* / *"Earn the XP and come back -- nothing is spent yet."*
- **Chat (`say`, `GRAY`):** `AccessoryRefusals.ANVIL`, *"That is not one of your weapons, shields, armor or tools."*,
  *"One item at a time."*, success *"Raised to N for X XP."*

### 2.12 Enchanting — `EnchantMenu`, layout `EnchantMenuLayout`

**Title: "Enchantments"** (the hub button and the info icon both say "Enchanting"). **Back:** 48 from the hub.
**Close:** 49. **Sounds: none, including on unlock and level-up.** From the hub the table is **unpowered** (bookshelf
power 0).

```
row 1   .  .  .  . [4] .  .  . [8]      4 info, 8 "Bookshelf Power N/30" (BOOKSHELF, stack size = power)
row 3   . [19] [21] . [23] . [25] .     19 the input; candidates: columns 21/23/25 = enchant slots 1-3
row 4   .  .  . [30] . [32] . [34] .    rows 3-5 = candidates 1-3 of each slot
row 5   .  .  . [39] . [41] . [43] .
row 6   filler, 48 Back (hub only), 49 Close          (NO status bar on this screen)
```

- **Info (4), empty:** "Enchanting" (`WHITE`); "Place a weapon, shield, armor piece or tool **above**" / "to see the
  enchants it can carry." / blank / "Unlocks are paid for in XP." — **the input (19) is BELOW the info icon (4)**, one
  row down and three columns left (F5). **With gear:** "Click a candidate to unlock it," / "to make it active, or to
  level it." / blank / "Swapping keeps the level you paid for."
- **Bookshelf (8):** "Bookshelf Power N/30" (`DARK_GRAY`) / "N% off unlocks and level-ups." / "Shelves in a ring around
  the table."; glint at 30.
- **Candidate:** name `EnchantLoreLines.label` in `DARK_GRAY` (locked) / `GREEN` (active, glint) / `WHITE`; lore the
  effect line, blank, then *"Locked. Click to unlock at I -- N XP."* / **"Active on this weapon."** (+ *"Click to raise
  its level -- N XP."*) / *"Unlocked. Click to make it active."*
- **Chat (`say`):** **"Put one of your weapons or shields in the slot above."**, **"One weapon at a time."**, *"Only one
  item at a time -- take the stack out and re-insert a single one."* (`RED`), **"This weapon carries more than this
  table can show (...)"**, *"That is not one of your weapons, shields, armor or tools."*, *"<name> costs N XP; you have
  W."* (`RED`), *"<name> is already at its maximum."* — **four of these still say "weapon" or "weapons or shields"**
  although armour and tools are accepted (F6).

### 2.13 Grindstone — `GrindstoneMenu`, layout `GrindstoneMenuLayout`

**Title:** "Grindstone". **Back:** 48 from the hub. **Close:** 49. **Sounds: none, including on a strip.**

```
row 1   .  .  .  . [4] .  .  .  .      4 info (GRINDSTONE, "Grindstone", WHITE)
rows 2-4  tray: 10-16, 19-25, 28-34 (21 input cells)
row 5   .  .  .  . [40] .  .  .  .     40 confirm
row 6   status bar (+ Back from the hub) + 49 Close
```

- **Info (4):** "Place weapons, shields, armor or tools below." / "Stripping clears every enchant they carry" / "and
  refunds 35% of the XP they cost." / blank / "The slot roll is kept."
- **Confirm (40):** a dye by state; name **always `WHITE`** (the anvil colours its name by state) — "Add items to
  strip", "These have nothing to strip", "Arming... 3", "Strip 9 items -- +1463 XP"; lore "Strips EVERY enchant from
  every item here." / "The roll is kept; the levels are not." / **"Refunds 35% of list price."**
- **Chat:** `AccessoryRefusals.GRINDSTONE`, the same *"That is not one of your weapons…"*, *"One item at a time."*,
  success *"Stripped N items for X XP."* (`GRAY`).
- **One rule, three phrasings of the refund (READ):** the hub says *"Refunds 35% of what they cost."*, the info icon
  *"…refunds 35% of the XP they cost."*, the confirm button *"Refunds 35% of list price."*

---

## 3. THE CONVENTIONS, SIDE BY SIDE (READ)

| screen | title | the button that opens it | Back (48) | Close (49) | status bar | name colour of the info icon |
|---|---|---|---|---|---|---|
| hub | Nexus Menu | (the star: "Nexus Menu", cyan-to-purple gradient) | — | yes | — | — |
| Settings | **Nexus Settings** | Settings | ARROW "Back to the Nexus" | yes | — | — |
| slot picker | Nexus Slot / Ability Stone Slot | Nexus Slot / Ability Stone Slot | ARROW "Back to Settings" | yes | — | — |
| Build | Build | Build | ARROW "Back to the Nexus" | yes | — | — |
| Build picker | Choose a … | Class / Q / Aspect 1 … | ARROW "Back to Build" | yes | — | slot 4, `GRAY` |
| Equipment | Equipment | Equipment | ARROW "Back to the Nexus" | yes | — | — |
| Vault | Vault | Vault | ARROW (hub only) | yes | — | — |
| Crafting | Crafting | Crafting | ARROW (hub only) | yes | yes | slot 4, `WHITE` |
| recipe browser | **Recipes** | **Recipe Book** | **CRAFTING_TABLE "Back to crafting"** | **NO** | — | — |
| Anvil | Anvil | Anvil | ARROW (hub only) | yes | yes | slot 4, `WHITE` |
| Enchanting | **Enchantments** | **Enchanting** | ARROW (hub only) | yes | **no** | slot 4, `WHITE` |
| Grindstone | Grindstone | Grindstone | ARROW (hub only) | yes | yes | slot 4, `WHITE` |

**Three Back mechanics coexist (READ):** (a) `onEntity(open)` without closing — Settings, slot picker, Build, Build
picker, Equipment, recipe browser; (b) `closeInventory()` then `onEntity(open)` — Vault; (c) `closeInventory()` then
`onEntityLater(open, 1)` — Crafting, Anvil, Enchanting, Grindstone. The javadoc reason for closing first is that a
screen with input cells must run `returnEverything` before the switch (`NexusMenu.onClick`'s vault-branch comment). Why
(b) and (c) differ by one tick was not traced for this survey.

**Chat colour for the same kind of line (READ):** a refusal is `GRAY` on the hub, Equipment, Crafting, Anvil and
Grindstone; `YELLOW` for Build's *"Choose a class and an element first."*; `RED` for Build-picker and Settings
refusals and for three enchant lines. Success is `AQUA` in Settings, the slot picker and Build; `GRAY` for the anvil's
*"Raised to…"* and the grindstone's *"Stripped…"*.

**`BARRIER` has ten roles across nine sites (READ):** Close (every screen); "Reserved" in the slot picker; "Choose a class to
unlock", "Accessories unavailable" and "Unreadable item" in Equipment; "Empty this slot" in the Build picker; a bad
fragment icon (`BuildMenu.fragmentMaterial`); "Nothing you can make right now" in the recipe browser; "Unreadable
item" in the Vault; the anvil preview's refusal. **Only the first is a button that closes.**

**Spelling (READ):** "Armour" / "armour" on the hub and Equipment; "armor" in the anvil, enchant and grindstone lines.
**Dashes:** `--` everywhere except Equipment's "Inactive — requires class:".
**Cooldown format:** "Cooldown: 2.0 s" on the Build screen (`AspectLore.seconds`) and "Cooldown: 2s" on the Build
picker (`BuildMenu.formatSeconds`), for the same ability (F7).

---

## 4. THE POLISH LIST — A PROPOSAL, GROUPED BY SCREEN

**M = MECHANISM (the seat rules). L = LOOK/FEEL/WORDING (Ben rules; options given, none picked).** Each item names the
finding in §6 it answers. An item that would touch a standing ruling (table above) says so, and is then a question,
not a change.

### 4.0 Every screen

| # | kind | item | options / note |
|---|---|---|---|
| X1 | **L** | **Sounds.** None exist anywhere in the menus (F28). The standing posture is *"the Nexus is being kept quiet on purpose"* (`NexusCollisionNotice`, Ben's instruction), recorded for the lock and the collision notice. **Whether it covers the menus is itself the first question** | (a) stay silent everywhere; (b) sounds only on a **completed action**: anvil transfer, grindstone strip, enchant unlock or level-up, a craft, a saved Build pick; (c) (b) plus a quiet click on navigation buttons; (d) (c) plus a "refused" sound. Candidate constants, **each present on the pinned jar** (`javap org.bukkit.Sound`): `UI_BUTTON_CLICK`, `BLOCK_ANVIL_USE`, `BLOCK_GRINDSTONE_USE`, `BLOCK_ENCHANTMENT_TABLE_USE`, `BLOCK_ENDER_CHEST_OPEN`, `ITEM_ARMOR_EQUIP_GENERIC`, `ITEM_BOOK_PAGE_TURN`, `ENTITY_EXPERIENCE_ORB_PICKUP`, `ENTITY_PLAYER_LEVELUP`, `ENTITY_VILLAGER_NO`. Which sound for which event: Ben's. Volume ___ (Ben), pitch ___ (Ben). `Player.playSound(Entity, Sound, float, float)` and `playSound(Location, Sound, float, float)` are on the jar; the existing notices use `player.playSound(player.getLocation(), …)`, so only that player hears it |
| X2 | **M** | **Which click types press a button** (F4). Today Q (drop) and middle-click press every hub, Settings, Build and picker button; shift-click presses none of them; Equipment alone filters | (a) leave; (b) one filter in `Menu`/`MenuRouting` so a non-input button reacts to LEFT/RIGHT only; (c) per screen, as Equipment does. Seat's call; Ben may have a feel view on shift-click |
| X3 | **L** | **`BARRIER` has ten roles** (§3, F20); only one is Close | (a) keep (NEXT.md *TWO BARRIERS ON ONE SCREEN*: changed to `STRUCTURE_VOID` and **reverted on operator instruction, 2026-09-03**, because *"Barrier is this plugin's 'nothing here' icon"*); (b) keep `BARRIER` for Close and refusals, move "empty" / "reserved" / "remove" to other materials — which ___ (Ben); (c) move Close off `BARRIER`. **(b) and (c) touch the 2026-09-03 ruling** |
| X4 | **M** | **The owed icon-collision guard** (NEXT.md *OWED: NOTHING IN THE REPO CAN DETECT THE NEXT ICON COLLISION*): layouts own their chrome MATERIALS beside their slot constants, and the layout test asserts screen-wide uniqueness | NEXT.md: *"Do it when something else already has those files open"* — a polish slice is that moment. Depends on X3: under (a) the assertion needs the ruled pair as an allowed exception |
| X5 | **L** | **Chat colour per kind of line** (F23) | (a) leave; (b) one colour per kind: refusal ___ (Ben), success ___ (Ben), failed save ___ (Ben); (c) (b) through one helper in `MenuIcons`, so it is one edit next time |
| X6 | **L** | **Dash and spelling** (F22) | one dash, one spelling. `ArmorSlot`, `ArmorItems` are code names, not player text |
| X7 | **L** | **Screen titles** are plain `DARK_GRAY` | (a) leave; (b) style them ___ (Ben); (c) the Vault's page in its title. `InventoryView.setTitle(String)` is on the jar; **whether it updates an open screen cleanly is UNVERIFIED** |
| X8 | **L** | **Filler shows an empty tooltip on hover.** `ItemMeta.setHideTooltip(boolean)` is on the jar | (a) leave; (b) hide it on filler. **What the client then shows is UNVERIFIED**; a gate row reads it |
| X9 | **M** | `MenuIcons.filler()` and `MenuIcons.pane(material)` have identical bodies (F27) | one calls the other; nothing visible changes |
| X10 | **M** | Three Back mechanics (F26) | (a) leave, each with its reason; (b) one helper that closes first only when the departing screen has input cells. **§7.2's source scans pin some of these lines** |

### 4.0b "MAKE THEM STAND OUT" — BEN'S WORDS, SO EVERY ROW HERE IS **L**, AND NONE IS PICKED

**What a button looks like today (READ):**

- **Every button name on the hub, Settings, Build and the pickers is `GRAY`**, the same weight as most lore; a locked
  station is `DARK_GRAY`; the in-screen info icons (crafting, anvil, enchant, grindstone at slot 4) are `WHITE`; the
  only strong colours are Close (`RED`), the stats head (`GOLD`), ON toggles and "Current" (`GREEN`), and content
  display names that carry their own MiniMessage colour (ability, aspect, fragment, element names).
- **Nothing in `P/menu/` or `P/nexus/` is bold** (`grep` for `BOLD` finds none) and every line goes through
  `MenuIcons.line`, which forces non-italic.
- **Enchant glint already means something on three screens:** the current choice in the Build picker
  (`BuildPickerMenu.optionIcon`), the active candidate on the enchant table (`EnchantMenu.candidateIcon`), and full
  bookshelf power (`EnchantMenu.bookshelfIcon`).
- **Lore has no fixed shape.** Hub buttons are one or two description lines and **no action line**; Settings, Build and
  the pickers end with a `DARK_GRAY` "Click to choose." / "Click to change."; the vault's page buttons end "Click to
  open."; the crafting Recipe Book ends "Click to browse".
- **No resource pack is configured** (`run/server.properties`: `resource-pack=` empty), so anything needing custom
  textures or models is out of reach without one.

**Options, each independent, for Ben to take any, all or none:**

| # | option | what it would look like | cost / what it collides with (READ unless marked) |
|---|---|---|---|
| SO1 | **Coloured button names** | one accent colour for "a button that opens a screen", e.g. ___ (Ben); `GRAY` kept for lore | colour choice is Ben's. Must stay distinct from `GREEN` (current / ON), `RED` (Close, errors), `DARK_GRAY` (locked) |
| SO2 | **Bold button names** | `TextDecoration.BOLD` on names only | none in code today, so it would read as new; one change in a `MenuIcons` helper |
| SO3 | **A colour per KIND of button** | e.g. stations one colour, features (Build, Equipment) another, chrome (Settings) a third — matching the hub's bands | ties colour to the band table; three colours ___ (Ben) |
| SO4 | **Enchant glint on live buttons** | `ItemMeta.setEnchantmentGlintOverride(true)` (on the jar) on every unlocked hub button | **collides with glint's three existing meanings** (current choice, active enchant, max bookshelf power); on the hub alone it does not share a screen with them |
| SO5 | **One lore shape for every button** | line 1: what it is (`GRAY`); blank; last line: the action ("Click to open", in a fixed colour ___ (Ben)) | the hub gains an action line on nine buttons; the locked-station lore (H3, drafted) would take the same shape |
| SO6 | **Distinct icon materials** | give the ten `BARRIER` roles their own materials (X3), so a barrier always means Close | touches the 2026-09-03 barrier revert (X3) |
| SO7 | **A frame around the buttons** | a different pane behind the buttons' rows than the black filler | **collides with the filler ruling** (`MenuIcons.FILLER`: black everywhere; gray and light gray are readouts). Needs a colour no screen uses as a readout ___ (Ben) |
| SO8 | **A count badge** | the item's stack size as a number on the icon, as the bookshelf already does (power = amount): e.g. the Vault showing unlocked pages | `NexusItems` and the no-stacks rule are about MINTED items, not icons; the bookshelf precedent exists (`EnchantMenu.bookshelfIcon`) |
| SO9 | **Rarity or item-name styling** | `ItemMeta.setRarity(ItemRarity)` and `setItemName(String)` are on the jar | what the client renders for rarity on a named item is **UNVERIFIED** |
| SO10 | **Custom models / tooltip styles** | `ItemMeta.setItemModel(NamespacedKey)`, `setTooltipStyle(NamespacedKey)` are on the jar | **needs a resource pack**, which the server does not have; a project decision, not polish |

**Every option moves text or materials that §7 pins**, so whichever Ben takes, P2/P3 (§8) carry the restaged rows.

### 4.1 The hub

| # | kind | item | options / note |
|---|---|---|---|
| H1 | **L** | **"Back to the Nexus"** while the title and the star say "Nexus Menu". The `NexusMenu` constructor comment: *"THAT IS A GUESS AT AN UNASKED QUESTION rather than a decision -- flagged so it costs one word to correct"* (F11) | (a) keep; (b) "Back to the Nexus Menu"; (c) other ___ (Ben). Pinned by `MenuIconsTest` and many rows (§7) |
| H2 | **L** | **Settings lore** *"Choose where the Nexus sits."*; Settings now holds four settings, two of them the Ability Stone's (F12) | (a) keep; (b) new wording ___ (Ben) |
| H3 | **L** | **Locked-station lore: "DRAFTED, NOT SETTLED -- Ben has the wording"** (F16) | Ben's words for the three lines. Pinned by `NexusStationGateTest` |
| H4 | **M** | "Seven pages, 252 slots." — "Seven" is a literal beside a computed number (F19) | derive it from `VaultShape.PAGE_COUNT`, or write a numeral |
| H5 | **M** | **The hub paints once** (F18): a level-up leaves a stale lock; the head's Quiver pair goes stale on a weapon swap (GATE-nexus *ROW 18*, parked *"so the slice that makes this head clickable finds the answer"*) | (a) leave; (b) repaint on a timer; (c) repaint on the events. Timer period, if (b): ___ (Ben) |
| H6 | — | **Rows 1 and 5 are empty.** Row 5 is *"unassigned"* by ruling | nothing, unless Ben gives row 5 a meaning. The `STATS_SLOT` javadoc: *"do not treat the next feature as an open question: its KIND picks its band"* |

### 4.2 The stats head

| # | kind | item | options / note |
|---|---|---|---|
| S1 | **L** | **Two label styles in one tooltip**, no blank line between five blocks (F21) | (a) leave; (b) one style ___ (Ben); (c) blank lines between blocks. **Moves `NexusStatsLoreTest` and GATE-nexus Rows 15, 75** |
| S2 | **L** | Class, element and loadout are not on the head | (a) leave (Build shows them); (b) add a block ___ (Ben) |
| S3 | — | The head is inert; its services were threaded so a click could repaint it | **Deferred to `PLAN-level-bonuses.md`**, which proposes the click open a level screen. Not designed here; S1 and S2 should be ruled together with that plan, since its screen may take over what the head's lore carries |

### 4.3 Settings and the slot picker

| # | kind | item | options / note |
|---|---|---|---|
| T1 | **L** | *"/menu opens this hub either way."* is on the **Settings** screen; chat says *"open this menu again with /menu"*; `/menu` opens the hub (F13) | wording ___ (Ben) |
| T2 | **M** | *"Click to put it back in not known yet."* (F14, INFERRED) | a separate sentence when the slot is unknown |
| T3 | **L** | Title "Nexus Settings" vs button "Settings" (F24) | match, either way. GATE-nexus Row 25 pins "Nexus Settings" |
| T4 | **L** | "Reserved" is a `BARRIER` | per X3 |
| T5 | **L** | An occupied cell shows the player's own item with no hint that a click moves the star there | (a) leave; (b) append a line through `MenuIcons.chromeOver`, the shape the crafting suggestions already use |

### 4.4 Build and its pickers

| # | kind | item | options / note |
|---|---|---|---|
| B1 | **M** | **"Click to remove the fragment here." on the ASPECT picker** (F2) | name the kind. Pinned by GATE-build-aspects / GATE-build-fragments rows |
| B2 | **M** | **Two cooldown formats for one ability** (F7), and "Recast within 2.5s" | one formatter (M); which format ___ (Ben) |
| B3 | **L** | Picker options show description + cooldown; the Build screen also shows Damage and Cost | (a) leave; (b) the same `numberLines` on the picker |
| B4 | **L** | Class shown as a capitalised id in `WHITE`; element as its content `display_name` (F25) | (a) leave; (b) a class display name from content. **No class display name exists in content** (a pool's `display_name` is the cell's, e.g. "Fire Melee"), so (b) is a schema change, not polish |
| B5 | **L** | A bad fragment `icon` falls back to `BARRIER` | fall back to `AMETHYST_SHARD`, the fragment picker's own title icon, or keep |
| B6 | **L** | Icon materials | `PLAN-build-system.md` §3.3.1 item 4: *"Materials are PRESENTATION and can be overruled in one word each"* — Ben's, freely |
| B7 | **L** | "Choose a class and an element first." is `YELLOW`; other picker refusals `RED` | per X5 |

### 4.5 Equipment

| # | kind | item | options / note |
|---|---|---|---|
| E1 | **L** | "Inactive — requires class: Ranger" vs Build's "Inactive -- requires Recall equipped": two dashes, two shapes | one shape ___ (Ben). Pinned by GATE-accessories-b B16 |
| E2 | **L** | Five armour refusals are silent; binding and a full inventory speak | (a) leave; (b) a line per refusal ___ (Ben). The grindstone's silence ruling is about a *coloured button* and does not obviously transfer here |
| E3 | **L** | "Choose a class to unlock", "Accessories unavailable", "Unreadable item" are `BARRIER`s | per X3 |
| E4 | — | **Columns 5-7 are reserved for Slice C.** Polish must not paint them | ruled |

### 4.6 Vault

| # | kind | item | options / note |
|---|---|---|---|
| V1 | **L** | "Page N -- locked" / "Unlocks at level X" vs the hub's "Locked -- unlocks at level N": two shapes for one idea | one shape ___ (Ben) |
| V2 | **L** | The page number is not in the title | per X7 |

### 4.7 Crafting and the recipe browser

| # | kind | item | options / note |
|---|---|---|---|
| C1 | **M** | **The recipe browser drops the hub** (F1) | pass the hub `Supplier<Menu>` through `RecipeBrowserMenu` and back. **The one navigation defect this survey found** |
| C2 | **L** | **No Close on the browser** (49 is the page readout), and its Back is a `CRAFTING_TABLE`, not the ruled ARROW (F8) | (a) leave; (b) adopt `MenuIcons.back(ARROW, …)` — its javadoc names the browser *"the obvious second adopter"*; (c) move the readout off 49 — to ___ (Ben) — and put Close there. (b) and (c) move toward the two rulings, not away |
| C3 | **L** | "Back to crafting" (lower case) vs "Back to Build", "Back to Settings" | capitalise or keep. `MenuIconsTest` pins the lower-case form |
| C4 | **L** | Title "Recipes" vs button "Recipe Book" | match ___ (Ben) |
| C5 | **L** | *"You do not have the materials for that."* (browser) vs *"You no longer have…"* (crafting) | one, or keep: "no longer" is true of a suggestion that was craftable a moment ago. GATE-crafting Q26 pins the browser's |

### 4.8 Anvil

| # | kind | item | options / note |
|---|---|---|---|
| A1 | **M + L** | **"Moves the sacrifice's score onto the item above."** points at the sacrifice (F3) | that it is wrong is M; the words are Ben's. GATE-anvil R9-R15 pin the preview lore |
| A2 | **L** | "Arming... 3" has no unit (the grindstone's too) | add one, or keep. Pinned by `AnvilButtonTest`, `GrindstoneButtonTest`, GATE-anvil R21 |

### 4.9 Enchanting

| # | kind | item | options / note |
|---|---|---|---|
| N1 | **M + L** | **Four lines say "weapon" / "weapons or shields"** (F6) | that they are wrong is M; the noun is Ben's ___ |
| N2 | **L** | "…tool **above**", with the input below (F5) | wording ___ (Ben) |
| N3 | **L** | Title "Enchantments" vs "Enchanting" | match ___ (Ben). GATE-nexus Row 31 pins "Enchanting" |
| N4 | **L** | No status bar here; the other three stations have one | (a) leave; (b) add one, whose colours would need a meaning here ___ (Ben) |

### 4.10 Grindstone

| # | kind | item | options / note |
|---|---|---|---|
| G1 | **L** | Three phrasings of the 35% refund (§2.13) | one ___ (Ben) |
| G2 | **L** | Confirm name always `WHITE`; the anvil colours its name by state | match, or keep: the dye already carries the colour the grindstone's silence ruling relies on |
| G3 | **M** | A full-inventory close can print **up to 21** *"Your inventory was full -- dropped at your feet."* lines (`MenuSafety.give` speaks per call; GATE-nexus *ROW 45*: *"Known, and not this slice's to fix"*) (F17). Crafting (9 cells) and the anvil (2) share the path | one summary line per `returnEverything`. The sentence is pinned by GATE-nexus Row 61 and GATE-accessories-b B12 |

---

## 5. STACKING ON `feat/melee-m1` (PR #168) — WHAT WOULD BE AWKWARD

**Read from `git diff 2d60e9a3 origin/feat/melee-m1 --stat` (23 files, +1513 / -25, seven commits `86509005` …
`e305ab3a`) and the full diff of its `paper/` half.**

### 5.1 File overlap: NONE in the menu code (READ)

melee-m1 touches **no file under `P/menu/` or `P/nexus/`**. Its paper main code is `RpgPlugin.java` (one constructor
argument), `AdapterContext.java` (a new constructor parameter: the trace switch), `PaperCombatWorld.java`, and
`RpgCommand.java` (the `/rpg mobtrace` message). Its content: six new YAML files (three placeholder abilities,
`keen_arc.yml`, `restless_quake.yml`, `builds/melee_fire.yml`) and the edited `brawlers_gauntlet.yml` header.

**Polish would collide textually only if it:**

- **threads a new service through `AdapterContext`** (a menu-sound helper, say). melee-m1 changed that constructor and
  its one call in `RpgPlugin`. Small, but a rebase there is the kind that can land a hunk in the wrong place without
  a conflict. A static helper in `MenuIcons` avoids both files.
- **edits `RpgCommand.java`** — nothing in §4 does.
- **edits content display names** — nothing in §4 does (B4(b) would, and is out of scope).

### 5.2 The Build screen is melee-m1's test surface, and its rows are NOT READ (the real hazard)

`GATE-melee-cell.md` on the branch: *"R0 PASS; MC rows NOT READ (Ben ran no rows); NOT approved to merge"* and *"#168
stays OPEN. It needs a later boot that reads R0 + MC1, MC2 and MC3"*. Those rows read the Build screen and the head:

- **MC1:** *"The Build screen's CLASS options read **Mage, Melee, Ranger**, in that order … the cell's name reads
  **Fire Melee** in gold"* — B4 and B6 change what MC1 is read against.
- **MC2:** the default loadout as the Build screen shows it — B2 and B3 change those icons.
- **MC7:** the fragment and aspect pickers' contents — B1 and B5 change those icons.
- **MC8:** *"the stats sheet shows `+3 Melee Damage`"* — S1 changes the head's layout.

**The build plan is RULED: every slice stacks on melee-m1, and ONE boot of the stack top reads every gate, #168's
MC rows included.** So MC1, MC2, MC7 and MC8 will be read on a Build screen and a head that this thread's B* and S*
items may already have changed. That is not a reason to move the items out of the stack. It is a cost the gates must
pay, in one of two ways (MECHANISM, the seat's call):

- **(i) Polish preserves what the MC rows read.** B*/S* items leave the class-option names and order, the cell's
  name and colour, the loadout icons and the `+3 Melee Damage` line exactly as #168 ships them. The MC rows then read
  the same on the stack top as on #168 alone.
- **(ii) The MC rows' predictions are restated against the stack top**, in this thread's GATE file, naming each MC row
  whose expected text a B*/S* item changes. #168's own GATE file is not edited by this thread.

**The hazard in both cases: #168 merges first (bottom-up) on readings taken from a screen #168 alone does not
ship.** Under (i) that is harmless by construction. Under (ii) the MC readings prove the stack top, not #168, and the
seat decides whether that is enough to merge #168. Nothing in #168 reads the other screens, so their items carry
neither cost.

### 5.3 Content counts asserted downstream (READ)

- **melee R0c** pins the boot line: *"`Loaded 11 abilities, 27 visuals, 4 statuses, 7 elements, 10 enchants, 3 pools,
  6 fragments, 6 aspects, 13 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`"*, *"Against
  F20's reading on `002352cb`: abilities 8 → 11, pools 2 → 3, aspects 4 → 6"*. **A polish gate stacked on melee
  predicts melee's line, not master's.** No §4 item adds content. **If X1's sounds were authored as visuals under
  `content/`, "27 visuals" moves.**
- **Tests on the melee branch:** `AspectLoaderTest` asserts **6** shipped aspects; `PoolLoaderTest` asserts **3** cells
  (`exactlyTheThreeFireCellsShip`) and pins `"<gold>Fire Melee</gold>"`; `ScorchContentInvariantTest` has
  `KNOWN_FIRE_DAMAGE_SITES = 21`. Polish touches none of them unless it edits content.
- **`ContentValidatorTest`** (read on master): its counts are per fixture (`problems.size()`, the flint and lapis visual
  counts); none counts pools, aspects or anything a menu shows. Polish does not touch it.

### 5.4 Schema bumps

**No §4 item needs one.** `PlayerProfile.CURRENT_SCHEMA_VERSION` is **5**; `PlayerBuild`, `PlayerAccessories`,
`PlayerVault` are **1** (READ). melee-m1 touches no storage. **A per-player "menu sounds on/off" setting** (a natural
follow-on to X1) **would** add a profile field and bump to 6, with a migration: its own slice, flagged here so X1 is
not sold as one line if Ben wants a toggle.

### 5.5 Gate rows that already disagree with the code (from the doc sweep; not re-read row by row)

A polish gate should not carry these unexamined: `GATE-build-screen.md` **BB2** (*"offers exactly **Mage, Ranger**"*;
melee makes three), **BB4** (names Arc Surge, deleted, and Rekindle, renamed), **BB11** (*"Coming in a later update"*
barriers, deleted by slices 4-5); `GATE-nexus.md` **Row 12** (stale by its own account), **Rows 26/28/29** (stage
choosers on the Settings screen, which slice 10 moved into the picker), **Row 63 vs Row 82** (*"The Nexus sits here."*
vs `Current slot (Hotbar 9)`; today they are the lime cell's lore and name, so both may hold — not re-read), and the
sweep's note that *"No row reads the hub's TITLE"*.

### 5.6 The commands slice (§9) against melee-m1

- **Top-level commands** add nodes inside `RpgPlugin`'s COMMANDS handler. melee-m1's `RpgPlugin` hunk is in the
  `AdapterContext` construction, not in that handler, so the two should not conflict textually (READ from the diff;
  not rebased to prove it).
- **`/rpg <screen>`** edits `RpgCommand.java`, which melee-m1 also edits (the `mobtrace` message) — a second hunk in a
  2,384-line file, and `build`/`class` literals there turn `BuildScreenWiringSignatureTest` red by design.
- **`/menu <screen>`** edits only `MenuCommand.java`, which melee-m1 does not touch.
- **New permission nodes** edit `Permissions.java` and `paper-plugin.yml`; melee-m1 touches neither.

### 5.7 Files this polish would share with `PLAN-level-bonuses.md` (the head's screen, §1.2)

That plan is not in the repo, so this is a list of **likely** shared files from what a head click needs, for the seat
to sequence the two:

| file | why both would touch it |
|---|---|
| `P/menu/NexusMenu.java` | the head's click branch in `onClick` (today nothing handles slot 13); this polish touches `render` (names, lore, SO options) and the ten Back suppliers (X10) |
| `P/menu/NexusMenuLayout.java` | `STATS_SLOT` and the painted set; this polish may touch it only through X4 (materials beside slots) |
| `P/menu/NexusStatsLore.java` | the head's lore (a "Click to …" line, or lines moving to the new screen); this polish's S1/S2 |
| `P/menu/MenuIcons.java` | Back/Close/filler for the new screen; this polish's X3, X5, X8, X9, SO1-SO5 helpers |
| `P/menu/MenuRouting.java` | only if the level screen pages with clicks that need routing; this polish's X2 (which click types press a button) changes it for every screen |
| tests | `NexusMenuLayoutTest`, `NexusStatsLoreTest`, and the source scans that read `NexusMenu.java` (`BuildScreenWiringSignatureTest`, `ProgressionWiringSignatureTest`) |

**The collision that matters:** X2 (click types) and SO1-SO5 (button styling) are cross-screen, so whichever lands
second restyles or re-routes the other's screen. **Proposal (seat's call):** land the shared `MenuIcons` helpers
first, and build the level screen on them.

---

## 6. FINDINGS — recorded, not fixed

**Defects: the screen says or does something wrong. READ in code; none watched in game by this survey.**

- **F1 — the recipe browser drops the breadcrumb.** `CraftingMenu.onClick`'s `BROWSER_SLOT` branch builds
  `new RecipeBrowserMenu(viewer, adapters, catalogue)`, which takes no hub; `RecipeBrowserMenu.onClick`'s Back builds
  `new CraftingMenu(viewer, adapters, catalogue)`, the three-argument constructor, so `hub` is null, `origin()` is
  `FROM_BLOCK`, and 48 is painted as a status-bar cell. **INFERRED in game:** after Nexus → Crafting → Recipe Book →
  Back, the only ways to the Nexus are Close then the star, or `/menu`.
- **F2 — "Click to remove the fragment here." on the aspect picker.** `BuildPickerMenu.optionIcon`'s `EMPTY_THE_SLOT`
  branch serves `Kind.ASPECT` and `Kind.FRAGMENT` and hard-codes "fragment".
- **F3 — the anvil's confirm lore points at the sacrifice.** `AnvilMenu.confirmIcon`: *"Moves the sacrifice's score
  onto the item above."* `CONFIRM_SLOT` 31 − 9 = 22 = `DONOR_SLOT`. The target is 20, which the preview calls *"the
  item on the LEFT"*.
- **F5 — "above" on the enchant table.** `EnchantMenu.render`'s empty arm: *"Place a weapon, shield, armor piece or tool
  above"*; `INPUT_SLOT` 19 is below `INFO_SLOT` 4. It may have meant "above your inventory"; that is not recorded.
- **F6 — "weapon" where any gear is accepted.** `EnchantMenu.applyCandidateClick` (*"Put one of your weapons or shields
  in the slot above."*), the amount check in its input guard (*"One weapon at a time."*), its overflow check (*"This
  weapon carries more than this table can show…"*), `candidateIcon` (*"Active on this weapon."*). The same class's
  wrong-item refusal says *"weapons, shields, armor or tools"*.

**Inconsistencies (READ).**

- **F4 — Q and middle-click press buttons; shift-click does not** (`MenuRouting.route`, §2.0). INFERRED in game, and in
  no gate row (the sweep: GATE-nexus rows 83-85 cover duplication, and *"none of 83 to 85 covers Q/drop or
  double-click on a picker cell"* — the sweep's observation, not the file's).
- **F7 — two cooldown formats** (`AspectLore.seconds`, `BuildMenu.formatSeconds`).
- **F8 — the browser has no Close and a non-ruled Back** (`RecipeBrowserLayout`: `PAGE_SLOT = 49`, no close constant;
  `RecipeBrowserMenu.render` mints `CRAFTING_TABLE` "Back to crafting" inline).
- **F12** — the hub's Settings lore covers one of four settings. **F13** — "/menu opens this hub" on Settings. **F14** —
  *"…put it back in not known yet."* (INFERRED).
- **F17** — up to 21 chat lines on a full-inventory close (GATE-nexus *ROW 45*, known). **F18** — the hub paints once
  (GATE-nexus *ROW 18*, parked). **F19** — "Seven pages" is a literal.
- **F20** — `BARRIER` in ten roles (§3). **F21** — two label styles in the stats head (§2.2). **F22** — `—` vs `--`;
  "armour" vs "armor". **F23** — chat colours. **F24** — title vs button: Enchanting/Enchantments, Settings/Nexus
  Settings, Recipe Book/Recipes. **F25** — class shown as a capitalised id. **F26** — three Back mechanics.
  **F27** — `filler()` and `pane()` duplicate. **F28** — no sounds anywhere.

**Stale prose, found while reading (READ).**

- **F9 — `MenuIcons.placeholder` has zero callers.** Its javadoc (*UNUSED AGAIN, 2026-09-03 — AND KEPT*) keeps it
  because *"the anvil, class-select and stat screens are still ahead"*. The anvil and the class select (Build) have
  shipped without it. The delete trigger is *"a third graduation … with none of those screens built"*, so it has not
  fired, but two of its three premises have lapsed. Seat's call.
- **F10 — `MenuIcons.back`'s javadoc says *"One consumer today, the settings screen"*.** Ten classes call it:
  `AnvilMenu`, `BuildMenu`, `BuildPickerMenu`, `CraftingMenu`, `EnchantMenu`, `EquipmentMenu`, `GrindstoneMenu`,
  `NexusSlotPickerMenu`, `NexusVaultMenu`, `SettingsMenu`.
- **F11 — the `NexusMenu` constructor comment says "on all four screens"**; eight screens show "Back to the Nexus".
- **F15 — NEXT.md *TWO BARRIERS ON ONE SCREEN* says *"An empty browser shows the close button and the empty-state
  notice"*.** The shipped browser has no close button: `RecipeBrowserLayout` at its first squash (`2d2d8faf`, #52)
  already has `PAGE_SLOT = 49` and no close constant. Either the account describes a pre-squash state or it was never
  true of the merged code; not traced further.
- **F16 — `NexusStationGate.lockedLore` is "DRAFTED, NOT SETTLED -- Ben has the wording"**; the class javadoc adds
  *"proposals are in the PR conversation rather than here"*. Open since `PLAN-vault-screen.md` *DECISION 3*.

**Owed elsewhere, touching these screens (from the doc sweep; each belongs to its own file, not this plan).**

- NEXT.md *OWED: NOTHING IN THE REPO CAN DETECT THE NEXT ICON COLLISION* (→ X4).
- NEXT.md and GATE-crafting Q33: *"`MenuIcons.icon`/`close`/`filler` have NO unit test — they need a live server"*.
- GATE-nexus: slices 2, 3, 4b, 5, 6 and 10 are largely **NOT RUN** (Rows 9-13, 14-19, 25-30, 31-33, 35-47, 78-91);
  the vault block's *"OWED 103"* and *"PARTIAL 102"*.
- GATE-build-screen: *"EVERY BB ROW IS NOT READ"*; BB10 owed (carried forward as BA13).
- GATE-crafting: restaged Q18a, Q18b, Q22b *(not run)*; slice 5's row breakdown and Q2's counts owed.
- GATE-accessories-b: B6, B16(a), B18 *"NOT GIVEN"*; the one-tick unequip residual, accepted.
- `PLAN-build-system.md` §6 item 6 (`QuiverAmmo`'s *"THE ONLY GameMode READ"* is stale: Equipment reads it too), item 7
  (*"`ProfileService.setStarEnabled`'s javadoc sits above `setVaultMigrated`"*), and item 10's `lingering_sun` /
  `rooted_TEMP` coupling, which changes the Build screen's aspect lore when the TEMP fixture goes.
- `PLAN-build-system.md` §4: each Ultimate picker offers one placeholder; Ben owes the Ultimates.
- `PLAN-melee-class.md` §9.3 **F4**: a pool's `class:` is never validated, so a typo ships a class **into the Class
  picker**.
- NEXT.md *PARKED — DURABILITY REPAIR*: the anvil screen removed vanilla repair and rename, and `BrokenNotice` still
  says "repair it". One trigger is *"Anything proposes a second use for the anvil screen"*. Anvil polish is not that,
  but a reviewer should know the entry exists.
- **Checked and NOT found:** the sweep reported a temporary `Recipe catalogue built:` console line owed for removal
  from `RecipeCatalogue.build()` (GATE-crafting *The browser — LIVE from slice 6*). **`grep` finds no such string in
  `P/` at `2d60e9a3`** (the only near-hit is a comment in `RecipeBrowserMenu`), so it is gone or was never merged.
  Recorded so nobody re-derives it.

---

## 7. WHAT PINS THE TEXT AND THE SLOTS

### 7.1 Unit tests that assert player-facing strings (READ)

| test | pins |
|---|---|
| `MenuIconsTest.aBackButtonNAMESItsDESTINATION_neverABareBack` | "Back to the Nexus"; "Back to crafting" |
| `MenuIconsTest` (the `chromeOver` rows) | "Craft 4 more", "Uses items from your inventory" |
| `NexusStationGateTest` | levels 3/7/10/13; "Locked -- unlocks at level 13", "You are level 7.", the world routes; the names "Crafting", "Enchanting", "Grindstone", "Anvil"; the refusal sentences, the vault's included |
| `NexusStatsLoreTest` | "Level        13", "Lifetime XP  19,980", "To Next      2,770", "Level        99 (MAX)"; line counts |
| `StatsSheetTest` (`hud/`) | the stat-sheet labels |
| `AnvilButtonTest` | "Raise to 310 -- 910 XP", "This transfer costs 910 XP; you have 400." |
| `GrindstoneButtonTest` | "Add items to strip", "These have nothing to strip", "Strip 9 items -- +1463 XP", "Strip 1 item -- +123 XP" |
| `NexusSlotPickerLayoutTest` | "Hotbar 1", "Hotbar 9", "Row 1, slot 1" … |

### 7.2 Tests that pin slots, or read the menu SOURCE as text (READ)

- **Every layout test pins its slots as literals:** `NexusMenuLayoutTest` (with the bands and the painted/filler
  partition), `SettingsMenuLayoutTest`, `NexusSlotPickerLayoutTest`, `BuildMenuLayoutTest` (28 picker options),
  `EquipmentMenuLayoutTest` (with **the Slice C reservation**), `NexusVaultLayoutTest.backAndCloseMatchEveryOtherScreen`,
  `CraftingMenuLayoutTest`, `RecipeBrowserLayoutTest`, `AnvilMenuLayoutTest`, `EnchantMenuLayoutTest`,
  `GrindstoneMenuLayoutTest`. C2(c) moves `RecipeBrowserLayoutTest`.
- **"Wiring signature" tests scan the menu files as text:** `BuildScreenWiringSignatureTest` pins, in `NexusMenu.java`,
  `if (click.slot() == NexusMenuLayout.BUILD_SLOT) {`, `new BuildMenu(viewer, adapters, profiles,` and
  `getInventory().setItem(NexusMenuLayout.BUILD_SLOT,`, and **bans `getInventory().getItem(` and `.clone()` in
  `BuildMenu` and `BuildPickerMenu`**; `ProgressionWiringSignatureTest` pins `NexusStationGate.refusal(` in `NexusMenu`;
  `AnvilWiringSignatureTest` pins many `AnvilMenu` lines and the `new AnvilMenu(player` openers in `RpgListeners`;
  `VaultWiringSignatureTest` pins `NexusVaultMenu`'s `scheduleWrite` / `onDragPermitted` shape;
  `AspectWiringSignatureTest` also reads a menu file (not re-read). **X10, or a tidy of the ten repeated
  `new NexusMenu(...)` suppliers in `NexusMenu.onClick`, must keep those strings or update the scans in the same
  change.**

### 7.3 Gate rows that pin on-screen text (from the doc sweep; ids as reported)

- **GATE-nexus.md:** Rows 12b, 15, 17, 25, 26a, 27, 28, 31, 32, 33a/33c, 41, 46, 61, 63, 69, 74, 75, 81, 82, 86, 89,
  97, 98, 99/100.
- **GATE-anvil.md:** R1, R2, R3, R5, R8, R9-R15, R17, R20, R21, R27.
- **GATE-crafting.md:** Q1 (the slot map), Q18a/b, Q22/Q22b, Q26, Q33, Q35, S12/S12b.
- **GATE-accessories-b.md:** B2, B3, B5, B12, B13, B14b, B16, B20, B22.
- **GATE-build-screen.md:** BB1, BB2, BB3, BB5, BB11 (stale), BB12, BB13. **GATE-build-fragments.md** and
  **GATE-build-aspects.md:** "Empty this slot", *"Saved: Fragment N = (empty)"* / *"Aspect N = (empty)"*.
- **GATE-melee-cell.md (melee branch):** MC1, MC2, MC7, MC8 (§5.2).

**A polish gate should name which of these it restages.** Most are NOT RUN, so restaging is cheap; a row already read
(GATE-nexus Row 63, 2026-09-17) becomes history, not a failure.

---

## 8. SLICES — A PROPOSAL ONLY, FOR THE SEAT

Nothing here is decided. The order follows §5.2's hazard.

- **P1 — the mechanism items, no wording decided:** C1 (the breadcrumb), X2 (if ruled), X9, H4, T2, G3, and the stale
  javadoc (F9's disposition, F10, F11). **Touches no Build screen**, so it can stack on melee-m1 now. C1 has no headless
  witness (the constructors need a server); a gate row reads it, and a source scan in the
  `BuildScreenWiringSignatureTest` style could pin that the browser receives the hub.
- **P2 — Ben's wording, in one pass, after §10:** F2/B1, F3/A1, F5/N2, F6/N1, H1, H2, H3, T1, T3, C3, C4, C5, E1, G1,
  V1, X6. Each moves tests and rows in §7; the PR lists them.
- **P3 — chrome:** C2, X3/X4, X7, X8. Needs X3's answer first.
- **P4 — sounds (X1), only if Ben wants them.** One helper, one table of event → sound; volume ___ (Ben), pitch ___
  (Ben). A toggle is a schema bump (§5.4) and its own slice.
- **P5 — the Build screen and the head (B2-B7, S1-S2)**, in the stack like the rest. Its GATE file takes §5.2's (i)
  or (ii) for every MC row it touches, and it is sequenced with `PLAN-level-bonuses.md` (§5.7).
- **P6 — player commands (§9), once Ben has named them and ruled 9.3's rows.** The public opener in `P/menu/` first
  (with the gate), then the nodes. Independent of the Build screen, so it can stack on melee-m1; `/menu <screen>` or
  top-level nodes avoid `RpgCommand.java` (§5.6).
- **Weighting, per Ben's brief:** the button work (SO options, names, lore: P2 + P3) is what he named first; P1 is
  small and can ride with P2.

---

## 9. PLAYER SLASH COMMANDS — BEN'S "add slash commands for players to easily reach them"

**Names are Ben's. Candidates are listed; none is picked.** The mechanism questions (where they register, what they
check) are the seat's, and are marked **M**.

### 9.1 How commands work today (READ)

- **One registration point.** `RpgPlugin` registers **one** `LifecycleEvents.COMMANDS` handler and, inside it, **two**
  Brigadier nodes: `RpgCommand.build(...)` (`/rpg …`) and `MenuCommand.build(...)` (`/menu`). The comment there: *"ONE
  handler, two nodes. A second registerEventHandler would be the sprawl the banned-patterns table names; a second node
  inside this one is not."* So new top-level commands are **new nodes in that same handler**, which the project already
  calls legitimate.
- **`/menu` exists, top-level by Ben's ruling.** `RpgPlugin`: *"/menu, NOT /rpg menu -- Ben's ruling, for reach. It is
  the door to the hub and the ONLY route to it once a player turns the Nexus star off."* `MenuCommand.build`:
  requires `Permissions.MENU` (`rpg.command.menu`, `default: true` in `paper-plugin.yml`); refuses a non-player
  (*"Players only."*); refuses when `profiles.profile(...)` is empty, with `RpgCommand.profileUnavailable` (loading vs
  unreadable); then `new NexusMenu(...).open()` **directly, with no scheduler hop** (*"a command is not inside"* an
  inventory close). **It checks nothing else**: no game mode, no death, no open screen, no level.
- **Permission nodes.** `Permissions` holds six: `rpg.command.cast`, `rpg.command.stats`, `rpg.command.menu`
  (`default: true` — players get these), and `rpg.command.admin`, `rpg.command.give`, `rpg.command.dev`
  (`default: op`). **Players get a node by `default: true` in `paper-plugin.yml`, one node per command family.**
- **Player-facing `/rpg` subcommands today:** `/rpg cast` and `/rpg stats` (the chat stat sheet). The other 28
  top-level `/rpg` literals are op (`admin`, `give`, `dev`), **including `/rpg vault` and `/rpg enchant`**.
- **Deleted, and guarded as deleted:** `/rpg class`, `/rpg element` and the dev `/rpg build`, with the node
  `rpg.command.class`. `BuildScreenWiringSignatureTest` asserts `Commands.literal("class")`, `("element")` and
  `("build")` are **absent from `RpgCommand.java`**, and `"rpg.command.class"` absent from `Permissions` and
  `paper-plugin.yml` (§9.4).
- **Registration API (javap, pinned jar):** `Commands.register(LiteralCommandNode, String, Collection<String>)` takes
  **aliases**, and every `register` overload **returns a `Set<String>`** of labels; `CommandRegistrationFlag` has
  `FLATTEN_ALIASES` and `SERVER_ONLY`. What the returned set holds when a label collides with vanilla or another plugin
  is **UNVERIFIED** (the javadoc is not in the jar); a boot row should log it.
- **Vanilla collisions (javap, `run/versions/26.1.2/paper-26.1.2.jar`):** `EnchantCommand` registers **`enchant`** and
  `RecipeCommand` registers **`recipe`**. So a top-level **`/enchant` collides with vanilla's**, and `/recipes` sits one
  letter from `/recipe`.

### 9.2 One command per screen — candidates (L, Ben's), and what each must decide

| screen | candidate names (none picked) | the gate the hub applies today (READ) | notes |
|---|---|---|---|
| hub | **`/menu`** (exists); aliases ___ e.g. `/nexus`, `/n` | none | ruled top-level |
| Settings | `/settings`, `/nexus settings`, `/menu settings` | none | `/settings` is generic; another plugin may own it (UNVERIFIED) |
| slot picker | none proposed; reached from Settings | none | two targets (star, stone) |
| Build | `/build`, `/loadout`, `/class`, `/menu build` | none (ungated, PLAN-build-system ruling 16) | **`/rpg build` and `/rpg class` would trip `BuildScreenWiringSignatureTest`**; a top-level `/build` or `/class` in another file would not trip the text scan but **does re-open what was deleted on purpose** — Ben's call, flagged |
| Equipment | `/equipment`, `/gear`, `/eq`, `/accessories` | none (ungated) | |
| Vault | `/vault`, `/pv`, `/ec`, `/enderchest` | **level 20** (`VaultPageGate.HUB_SHORTCUT_LEVEL`); the ender-chest block is free | which gate a command uses is Ben's (9.3) |
| Crafting | `/craft`, `/workbench`, `/wb` | **level 3** | |
| recipe browser | `/recipes`, `/recipebook` | none of its own (via Crafting) | near vanilla `/recipe` |
| Anvil | `/anvil` | **level 7** | |
| Enchanting | `/enchanting`, `/et`, `/enchant` | **level 10** | **`/enchant` collides with vanilla** (9.1); from a command the table is unpowered, as from the hub |
| Grindstone | `/grindstone`, `/gs` | **level 13** | |
| stats head / level screen | `/level`, `/levels`, `/stats` | none | `/rpg stats` exists (chat sheet, `rpg.command.stats`); the level screen is `PLAN-level-bonuses.md`'s |

**A single-command alternative (M + L):** `/menu <screen>` — one top-level node with a literal per screen (`/menu
build`, `/menu vault` …), plus bare `/menu` as today. Reach is one word plus an argument, tab-complete lists the
screens, there is one permission node, and there is one collision surface (`menu`) instead of eleven.

### 9.3 What a command should do when the player cannot use the screen — each row is a choice

| condition | what exists today (READ) | options |
|---|---|---|
| **Station not unlocked** (`NexusStationGate`) | the hub refuses a click with `NexusStationGate.refusal` (*"Anvil unlocks at level 7. You are level 6. An anvil in the world still works."*); the world block ignores level | (a) the same refusal, same sentence (**M**: `NexusStationGate` is **package-private** in `P/menu/`, so a command in `P/command/` cannot call it without a public entry point in `menu`); (b) no gate, like the world block — this would make the level gates decorative for anyone who knows the command. **Ben's** |
| **Vault** | hub gate 20; ender chest free; pages gated separately inside the screen | (a) 20, like the hub; (b) free, like the ender chest (the pages still gate); **Ben's** |
| **No class chosen** | nothing refuses opening any screen without a class. Build then shows gray panes (*"Choose a class and an element first."*); Equipment shows the class slot as "Choose a class to unlock" | no check needed to open; listed because the brief asked |
| **In combat** | **no combat-tag concept exists in the code** (`grep` for `combatTag`, `inCombat`, `CombatTag`, "combat tag" over `P/` finds none) | (a) no check; (b) a new combat window — a new concept and a new listener (**M**), with a window of ___ (Ben) seconds and a refusal sentence ___ (Ben) |
| **Dead** | `MenuCommand` does not check. `Entity.isDead()` is on the jar | (a) refuse with a sentence ___ (Ben); (b) no check. What opening an inventory on a dead player does is **UNVERIFIED** |
| **The Nexus lock** (`NexusLock`) | `NexusLock` is a **click predicate** — *"Would this gesture move the Nexus star out of its locked slot?"* — so a command never passes through it. **The star being OFF** is the case that matters: `/menu` is *"the ONLY route"* then | none needed for the lock itself; every screen command must work with the star off, as `/menu` does |
| **A menu already open** | the star's gesture opens only from the **own-inventory screen** (*"Opening the hub from inside another menu is a nested transition"*). `/menu` does not check. `openInventory` closes the current screen, which runs our `onClose` → `returnEverything` (INFERRED from `Menu.handleClose` and the `RpgListeners` comment *"openInventory's implicit close is sufficient"*). Whether a player can type a command with a container open is client behaviour, **UNVERIFIED** (command blocks and clickable chat can) | (a) as `/menu`: open, let the implicit close return items; (b) close first, then open through `scheduler().onEntity`, the menu-to-menu pattern; (c) refuse while a screen is open. **M** |
| **Creative** | the star's gesture **excludes creative by construction** (`NexusOpenGesture`: *"Ben's question, not this slice's"*); **`/menu` does not** (no game-mode check, INFERRED from `MenuCommand.build`) | (a) commands work in creative, as `/menu` does; (b) mirror the gesture. **Ben's** — the question `NexusOpenGesture` already parked |
| **Profile loading / unreadable** | `/menu` refuses with `RpgCommand.profileUnavailable` | the same, for every screen (M) |

### 9.4 Bypass the star's gesture, or do the star's checks?

**`/menu` already bypasses all four of the gesture's conditions** (LEFT/RIGHT click, own slot, empty cursor, own-
inventory screen) — it has to, because the star can be switched off. **A screen command inherits that position unless
Ben says otherwise.** What it should NOT bypass is the **level gate** the hub applies to the five stations, unless Ben
wants the command to be as free as the world block (9.3, row 1).

**Back button (M + L):** a command-opened screen can be handed a hub `Supplier<Menu>` (Back reads "Back to the Nexus",
as from the hub) or `null` (no Back, as from a world block). `MenuCommand` already holds every service the hub needs,
so either is cheap. Which the player sees is Ben's.

### 9.5 `/rpg <screen>` or top-level — the seat's (M), with costs

| | top-level (`/build`, `/vault` …) | `/rpg <screen>` | `/menu <screen>` |
|---|---|---|---|
| reach | best; matches Ben's `/menu` ruling "for reach" | worst: `/rpg` is otherwise the operator's namespace (all but `cast` and `stats` are op) | one word + one argument |
| collisions | one per name; **`/enchant` collides with vanilla**; generic names (`/settings`, `/vault`) may collide with other plugins (UNVERIFIED) | **`/rpg vault` and `/rpg enchant` are already taken by op dev tooling** (`Permissions.DEV`: `VaultDevCommand`, the enchant `show`/`clear`/… tree), so two screens would need other literals | only `menu` (exists) |
| permission nodes | one per command, or all sharing `rpg.command.menu` | per subcommand `requires()`, as today | `rpg.command.menu`, already `default: true` |
| code | a node per command inside the one COMMANDS handler, or one `MenuCommand`-style builder returning several nodes | edits `RpgCommand.java` (2,384 lines by `wc -l`, **also touched by melee-m1**, §5.1); `build`/`class` literals **trip `BuildScreenWiringSignatureTest`** | edits `MenuCommand.java` only (not touched by melee-m1) |
| help / tab | each shows in `/help` | hidden behind `/rpg` | tab-complete lists the screens |

**Mechanism shared by all three:** the station and vault gates live in `P/menu/` as package-private classes
(`NexusStationGate`, `NexusMenuLayout`), so a command in `P/command/` needs **one public opener in `P/menu/`** (for
example a static `open(Player, Screen)` beside `NexusMenu`) that applies the same gate the hub applies. That keeps
*"one check in front of … branches"* (`NexusMenu.onClick`'s comment) true for the command route too, instead of a
second copy of the gate in `P/command/`.

**Tests the command slice would add or move (M):** a `Permissions` / `paper-plugin.yml` node per new permission (the
`BuildScreenWiringSignatureTest` shape reads both files as text); a source scan that each screen command reaches the
shared gate; a boot row per command reading the `register` return set for collisions.

---

## 10. QUESTIONS FOR BEN

**None answered yet.** Each is LOOK/FEEL/WORDING unless marked. The first three are the ones his brief named.

A. **"Make them stand out" (§4.0b):** which of SO1-SO10, and the colours they need (accent ___, per-kind ___, action
   line ___)?
B. **Slash commands (§9):** the name for each screen (9.2), or one `/menu <screen>`? Which of 9.3's rows apply — the
   level gate from a command (same as the hub, or free like the world block), the vault's gate (20 or free), creative,
   death, combat (a new concept, with a window of ___ seconds)? Should a command-opened screen show "Back to the
   Nexus"?
C. **Button names and lore:** the wording items gathered in 7, 8 and 11 below, and the locked-station lore (3).

1. **Sounds (X1):** (a) silent, (b) completed actions only, (c) plus navigation clicks, or (d) plus refusals? Does
   *"The Nexus is being kept quiet on purpose"* (`NexusCollisionNotice`) cover the menus? If sounds: which for which event, volume ___, pitch ___, and
   is a per-player off switch wanted (a schema bump, §5.4)?
2. **"Back to the Nexus" or "Back to the Nexus Menu"** (H1)? The code calls today's a guess.
3. **The locked-station wording** (H3): drafted, never settled.
4. **Barriers (X3):** keep all ten roles, keep them for Close and refusals only, or move Close?
5. **The recipe browser (C2-C4):** the ARROW Back and a Close at 49 (and then where does the page readout go)? "Back to
   crafting" or "Back to Crafting"? "Recipes" or "Recipe Book"?
6. **Title vs button (F24):** Enchanting or Enchantments; Settings or Nexus Settings?
7. **The wrong-text fixes (F2, F3, F5, F6):** words for the aspect's "Empty this slot" lore, the anvil's confirm lore,
   the enchant table's "above", and the noun that replaces "weapon".
8. **One style each for:** the dash (`--` or `—`); "armour" or "armor"; the refund sentence (G1); the locked-page vs
   locked-station sentence (V1); the cooldown format (B2); chat colours by kind (X5).
9. **The stats head (S1-S2):** one label style? blank lines between blocks? the Build on it? (What a click does is
   `PLAN-level-bonuses.md`'s question, not this plan's.)
10. **Equipment's silent armour refusals (E2):** keep silent, or say why?
11. **Settings wording (H2, T1):** the hub's Settings lore, and "/menu opens this hub either way" on Settings.
12. **Filler tooltip (X8); styled titles, or the page number in the Vault's title (X7):** wanted?
13. **For the seat (MECHANISM):** X2 (which clicks press a button), X10 (one Back mechanic), F9 (`placeholder`: keep or
    delete), H5 (repaint the hub), whether P5 waits for #168's MC rows (§5.2), top-level vs `/rpg` vs `/menu <screen>`
    (§9.5), the public opener in `P/menu/` (§9.5), the open-screen handling for commands (§9.3), and the order
    against `PLAN-level-bonuses.md` (§5.7).

