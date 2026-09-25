# GATE — accessories, Slice B: the Equipment submenu

**Status: NOT RUN.** Every prediction below was written **before** any boot. Readings go **beside** a
prediction, never over it, and a prediction is not edited once its row has been read. Readings are
verdicts, not figures, unless the row asks for a figure.

```
ROWS     24   R0a R0b B0b B1 B2 B3 B4 B5 B6 B7 B8 B9 B10 B11 B12 B13 B14 B15 B16 B17 B18 B19 B20 B21
         ──
         24   = git grep -c '^### R\|^### B' <ref> -- GATE-accessories-b.md
```

**Plan:** `PLAN-accessories.md` §4, with the seat's Slice B rulings (C1–C5 and the Phase 1 rulings of
2026-09-25). **NO BOOT until the seat has diffed the PR from origin.**

**Declared game mode: SURVIVAL, EVERY ROW, unless the row names another.** Creative changes the one
rule this screen reproduces that depends on mode (binding), and a creative click is a different click.

**Boot:** `./scripts/dev-server.sh`. This slice ships no content file, so `--refresh-content` is not
needed; it does no harm.

---

## *** R0 — THE FIRST ROWS OF THE FILE. If any R0 row fails, STOP. ***

### R0a — the build line names this branch's tip

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <tip>` naming the PR's tip SHA — not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | _(not run)_ |

### R0b — the deployed jar carries the screen, and NOT the deleted dev command

| prediction | instrument | READING |
|---|---|---|
| `EquipmentMenu.class` and `ArmorPlacement.class` PRESENT; **`AccessoryDevCommand.class` ABSENT** (the deletion); the control class ABSENT | the scan below | _(not run)_ |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/paper/menu/EquipmentMenu.class',
               'io/github/butterflysmp/rpg/core/equipment/ArmorPlacement.class',
               'io/github/butterflysmp/rpg/paper/accessory/AccessoryDevCommand.class',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$zip.Dispose()
```

> **Two ABSENTs are expected, and they mean different things.** The control is absent because it never
> existed; `AccessoryDevCommand` is absent because this slice deleted it. A scan printing PRESENT for the
> dev command is a stale jar or an undeleted class, and R0 stops there.

---

## THE ROWS

Set-up shared by most rows: an op player, survival, `/rpg class ranger` chosen, the Nexus open (`/menu`),
the **Equipment** button at slot 22 clicked. `/rpg stats` reads the stats sheet.

### B0b — no listener was added

| prediction | instrument | READING |
|---|---|---|
| **41** `@EventHandler` methods, all in `RpgListeners`, the same count as at `295d006`; the three PacketEvents `registerListener` calls name the same three classes (`ExampleTelegraphListener`, `WeaponSwingListener`, `VanillaCritParticleListener`) | `git grep -c '@EventHandler' <tip> -- paper/src/main` and `git grep -n 'registerListener(' <tip> -- paper/src/main` | _(not run)_ |

### B1 — with NO menu open, the vanilla inventory is untouched (extended per C5)

| prediction | READING |
|---|---|
| In the plain E-screen: armour slots take and give armour by click; shift-clicking a helmet from the inventory puts it on the head; with a helmet already on, a shift-click does vanilla's main↔hotbar hop (NOT our refusal — that divergence exists only inside our screen); right-clicking a chestplate in the air equips it; a Curse of Binding helmet cannot be taken off. | _(not run)_ |

### B2 — the Nexus button

| prediction | READING |
|---|---|
| The Nexus shows an **armour stand** named "Equipment" at slot 22 (row 3, under the stats head), at any level, and clicking it opens the Equipment screen. | _(not run)_ |

### B3 — the armour column IS the player's armour

| prediction | READING |
|---|---|
| The four armour slots show exactly what the player wears (empty ones as grey panes named Head / Chest / Legs / Feet). Taking the helmet out of the screen leaves the head **empty** (check with F5). | _(not run)_ |

### B4 — fit

| prediction | READING |
|---|---|
| A chestplate on the cursor, clicked onto the head slot: nothing happens, and the chestplate **stays on the cursor**. A block of dirt: the same. | _(not run)_ |

### B5 — binding (two modes)

| prediction | READING |
|---|---|
| **Survival:** a Curse of Binding helmet cannot be taken out (click, shift-click, or a swap), and the chat says `That is bound to you: Curse of Binding.` **Creative:** the same helmet CAN be taken out. | _(not run)_ |

### B6 — one equip sound, not two

| prediction | READING |
|---|---|
| Placing an iron chestplate plays the iron-armour equip sound **exactly once**. (F1: the write is non-silent; we play nothing ourselves.) | _(not run)_ |

### B7 — shift-in to armour

| prediction | READING |
|---|---|
| Shift-clicking a helmet in the player's half puts it on the head. With a helmet already on, **nothing moves** (the deliberate divergence, below). | _(not run)_ |

### B8 — one from a stack

| prediction | READING |
|---|---|
| A stack of 16 skeleton skulls on the cursor, clicked onto the empty head slot: one goes on, **15 stay on the cursor**. Shift-clicking a stack of 16 from the inventory: one goes on, 15 stay in the inventory. A stack of 2 on the cursor onto an OCCUPIED head slot: nothing happens (vanilla's no-swap rule). | _(not run)_ |

### B9 — the Nexus star

| prediction | READING |
|---|---|
| The star cannot be put into any armour or accessory slot, by click or by shift-click. | _(not run)_ |

### B10 — disconnect with an item on the cursor

| prediction | READING |
|---|---|
| Take the helmet onto the cursor, disconnect. Rejoin: the helmet is in the inventory — **one** copy, not on the head and in the inventory both. | _(not run)_ |

### B11 — death with the screen open

| prediction | READING |
|---|---|
| Take the helmet onto the cursor, `/kill`. After respawn: **one** helmet, in the inventory. | _(not run)_ |

### B12 — full inventory on close

| prediction | READING |
|---|---|
| Fill the inventory, take the helmet onto the cursor, close the screen: the helmet **drops at the feet** with `Your inventory was full -- dropped at your feet.` | _(not run)_ |

### B13 — accessory slot kinds

| prediction | READING |
|---|---|
| Slot 0 (the top accessory slot) refuses a Ward Charm (`This slot takes a class accessory.`); slots 1–3 refuse a Fletcher's Quiver. | _(not run)_ |

### B14 — refused gestures (C5)

| prediction | READING |
|---|---|
| Over each armour slot and each accessory slot: the number key, F, a drag, and a double-click all do **nothing** — no item moves, in either half. | _(not run)_ |

### B15 — Defense through the screen (C5)

| prediction | READING |
|---|---|
| Unarmoured: `/rpg stats` Defense **0**. Put a plain iron chestplate on through the screen: Defense **6** within a second. Take it out through the screen: back to **0**. | _(not run)_ |

### B16 — slot 0's states (rewritten per the seat)

| prediction | READING |
|---|---|
| (a) On a profile with **no class**, slot 0 shows a barrier named `Choose a class to unlock`. (b) As a ranger, equip the Fletcher's Quiver through the screen into slot 0, then `/rpg class mage`: reopen the screen — slot 0 shows the Quiver with the lore line `Inactive — requires class: Ranger`. (c) `/rpg class ranger`, reopen: that line is **gone**. | _(not run)_ |

> **(a) needs a class-less profile**, as `GATE-accessories-a.md` A4(d) did. If none is available,
> record (a) as NOT READ rather than improvising a profile edit.

### B17 — accessory moves through the screen (C5)

| prediction | READING |
|---|---|
| Put a Ward Charm into slot 1 from the cursor: it leaves the cursor, and `run/plugins/Rpg/accessories/<uuid>.json` gains an entry for slot 1. Click it out: it returns to the cursor, and the file's slot-1 entry is gone. **One copy throughout.** | _(not run)_ |

### B18 — the dev command is gone (rewritten per the seat)

| prediction | READING |
|---|---|
| `/rpg accessory` is an **unknown subcommand** at the tip. The `/rpg ` tab-complete list at the tip equals the list at `295d006` **minus `accessory`**. **Take both lists from running servers and paste them here**, in full. | _(not run)_ |

### B19 — a stale render cannot be acted on (new)

| prediction | READING |
|---|---|
| Open the screen wearing a helmet. From the console: `item replace entity <player> armor.head with air`. Click the head slot with an empty cursor: **nothing arrives on the cursor**, and the slot's display corrects to the empty pane. | _(not run)_ |

### B20 — the unreadable slot (new)

| prediction | READING |
|---|---|
| Stage `GATE-accessories-a.md` A10: with the server stopped, set slot 2's `item` text to `AAAA`. Boot, open the screen: slot 2 shows the barrier `Unreadable item`, and refuses a take (empty cursor) and a place (a charm on the cursor) with `That slot holds an item this server cannot read. It is kept, and cannot be moved.` Afterwards the file **still reads `AAAA`**. | _(not run)_ |

### B21 — shift-in routing for accessories (new)

| prediction | READING |
|---|---|
| With slots 1–3 empty, shift-click a Ward Charm from the inventory: it goes to **slot 1**. Another: **slot 2**. A Fletcher's Quiver: **slot 0**. With every target full, a shift-click moves **nothing**. | _(not run)_ |

---

## DELIBERATE DIVERGENCES FROM VANILLA, AND ONE RESIDUAL

Recorded here and in the code that makes them, so a reading that shows them is a PASS rather than a
surprise.

1. **Shift-in onto an OCCUPIED armour slot moves nothing.** Vanilla (`InventoryMenu.quickMoveStack`)
   falls through to its main↔hotbar hop. That hop belongs to vanilla's own screen; in ours a shift-click
   aims at the armour column. Ruled by the seat. Code: `ArmorPlacement`'s class note,
   `Outcome.REFUSE_OCCUPIED`. B1 confirms vanilla keeps its hop OUTSIDE our screen; B7 confirms ours.
2. **Binding is keyed on Curse of Binding by NAME.** Vanilla (`ArmorSlot.mayPickup`) keys it on the
   enchantment EFFECT `prevent_armor_change`. In 26.1.2's data only `binding_curse` carries that effect
   (1 of 43 enchantment files), so they agree today; a datapack adding the effect elsewhere would
   diverge. Code: `ArmorPlacement`, `EquipmentReads.bound`.
3. **Residual, not a divergence: an accessory unequip whose hand-back never runs.** An unequip writes
   first and hands the item over only when the write succeeds (§3.9). The hand-back hops to the
   player's thread, and the `Scheduler` drops a task for a player who is gone. So a player who
   disconnects in the tick between the write landing and the hop running is not handed the item. It is
   the vault's accepted "completion lost" residual family, named in `AccessoryService`'s javadoc. No
   row stages it (the window is one tick).
