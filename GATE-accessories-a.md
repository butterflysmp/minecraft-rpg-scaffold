# GATE — accessories, Slice A: the kind, the store and the stats (no menu)

**Status: READ on `421d85a`, 2026-09-24 -- all 18 rows reported good by Ben. Three sub-readings were NOT given: A4 (d), A9's stations, and A14's figures (Ben: "skip the numbers"). Each row says which.** Every prediction below was written **before** any boot. Readings go **beside** a
prediction, never over it, and a prediction is not edited once its row has been read. Readings are
verdicts, not figures, unless the row asks for a figure.

```
ROWS     18   R0a R0b R0c A1 A2 A3 A3b A4 A5 A6 A7 A8 A9 A10 A11 A12 A13 A14
         ──
         18   = git grep -c '^### R\|^### A' <ref> -- GATE-accessories-a.md
```

**Plan:** `PLAN-accessories.md` §3 and §5.2, with the RULINGS (Q1-Q7) applied, and the roster and
stats-sheet shape Ben ruled on 2026-09-24 while this slice was being built.

**Declared game mode: SURVIVAL, EVERY ROW.** Creative removes costs — consumption, durability, the
need to hold the item — and A9 is a row about consumption, so creative would pass it for free. Two
rows name an extra mode, and say why.

**Boot with `--refresh-content`.** `content/accessories/` is a NEW directory, so its six files would
copy on their own; the flag is still required because R0b reads the loader's count, and an old data
folder must not be the thing that answers it.

```
./scripts/dev-server.sh --refresh-content
```

---

## *** R0 — THE FIRST ROWS OF THE FILE. If any R0 row fails, STOP. ***

### R0a — the build line names this branch's tip

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <tip>` naming the PR's tip SHA — not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS** *(421d85a)* -- `[18:16:41] [Server thread/INFO]: [Rpg] Build: 421d85a`, the only build line in the file |

### R0b — the loader found all six, and skipped none

| prediction | instrument | READING |
|---|---|---|
| the Loaded line reads `..., 5 tools, 6 accessories, ...`, and **no** line reads `Skipping malformed accessory` | `Select-String -Path run\logs\latest.log -Pattern 'Loaded .* accessories\|malformed accessory\|No accessories loaded'` | **PASS** *(421d85a)* -- `[18:16:41] [Server thread/INFO]: [Rpg] Loaded 6 abilities, 28 visuals, 5 statuses, 7 elements, 10 enchants, 2 kits, 14 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes`; no `Skipping malformed accessory` line |

> **SIX IS RULED (Q2), SO A FIVE IS A SKIPPED FILE, NOT A SMALLER ROSTER.** The skip line names the
> file and the reason; read it before anything else.

### R0c — the deployed jar carries the kind and the content

| prediction | instrument | READING |
|---|---|---|
| `AccessoryDefinition.class` and `AccessoryService.class` PRESENT; **6** `content/accessories/*.yml` entries; the control class ABSENT | the scan below | **PASS** *(421d85a)* -- `PRESENT` AccessoryDefinition.class; `PRESENT` AccessoryService.class; `ABSENT` NoSuchClassControl.class (the control); `accessory content entries: 6` |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/weapon/AccessoryDefinition.class',
               'io/github/butterflysmp/rpg/paper/accessory/AccessoryService.class',
               'io/github/butterflysmp/rpg/paper/accessory/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
"accessory content entries: " + ($zip.Entries | Where-Object { $_.FullName -like 'content/accessories/*.yml' }).Count
$zip.Dispose()
```

> **The control is the third name.** A scan that printed PRESENT for all three would be answering
> something other than the question.

---

## THE ROWS

Set-up shared by most rows: an op player, survival, `/rpg class ranger` already chosen (A4 says what
a class-less profile sees). `/rpg accessory show` prints the four slots, and is the instrument for
"what is worn". The accessory commands are **dev instruments** (`Permissions.DEV`), and the gate is
the reason they exist.

### A1 — give mints ONE item, which does not stack, and its lore shows every modifier

**Risk:** a stack of two, where one write edits both (the standing decision); a drawback hidden.

| prediction | READING |
|---|---|
| `/rpg give fletchers_quiver` twice → **two separate items in two slots**, never one stack of 2. The name reads `Fletcher's Quiver` in the rare colour. The lore, top to bottom: `+5% Crit Chance` (green), `+3 Ranged Damage` (green), `-0.20/5s Health Regen` (**red**), `Ranger class accessory` (grey), a blank, the italic flavour line, a blank, `Rare Accessory`. **No enchant glint, and no Gear Score line.** | **PASS** *(421d85a)* |

### A2 — accessory Defense MERGES with armour Defense; neither wipes the other

**Risk:** the scanner-wipe — two reconciles on one stat, each clearing the other's sources.

| prediction | READING |
|---|---|
| Wear a **plain vanilla iron chestplate**: `/rpg stats` Defense **6**. `/rpg accessory equip 1` holding a Ward Charm → Defense **9** within a second. Take the chestplate **off** → Defense **3** (the charm's source survives). Put it back → **9**. `/rpg accessory unequip 1` → **6**. At no step does Defense read 3 with the chestplate on, or 6 with only the charm — each of those is one source wiping the other. | **PASS** *(421d85a)* |

### A3 — the class slot contributes only for the profile's class, and goes INERT on a change

**Risk:** the A1 gate absent, or a class change that removes/keeps the item wrongly.

| prediction | READING |
|---|---|
| As a **ranger**, `/rpg accessory equip 0` holding a Fletcher's Quiver → `/rpg stats`: Crit Chance **20%** (was 15%), Health Regen **0.80/5s** (was 1.00/5s). `/rpg class mage` → within a second Crit Chance **15%**, Health Regen **1.00/5s**, the quiver **still in slot 0** (`/rpg accessory show` marks it INACTIVE), and the stats sheet's block reads `Fletcher's Quiver: inactive (requires Ranger)`. `/rpg class ranger` → **20%** and **0.80/5s** again, with no other action. | **PASS** *(421d85a)* |

> The class change also mints the new kit's weapon — the **pre-existing** duplicate-kit-weapons
> defect (`PLAN-accessories.md` §8.1). It is not this slice's, and not this row's reading.

### A3b — class damage applies only with a weapon of the SAME class held (ruling Q3)

**Risk:** the grant merged AFTER `matching` (so it ignores the held weapon), or not merged at all.

| prediction | READING |
|---|---|
| Ranger, quiver in slot 0. Holding a **Boltor**: `/rpg stats` Damage is **exactly 3.00 higher** than the same Boltor with the quiver unequipped. Holding a **mage staff** (or nothing): Damage is **unchanged** by equipping the quiver. | **PASS** *(421d85a)* |

### A4 — the class slot refuses the wrong class, and is LOCKED with no class

**Risk:** the equip decision (`AccessorySlots.canEquip`) not wired to the command.

| prediction | READING |
|---|---|
| (a) As a ranger, `/rpg accessory equip 0` holding a **Sage's Scroll** → refused, `That accessory is for mage; your class is ranger.`, and the scroll **stays in the hand**. (b) Holding a **Brawler's Gauntlet** → refused the same way, naming `melee` — **no profile can wear it today (ruling Q1)**, and `/rpg class melee` is itself refused (`Unknown class: melee`). (c) Holding a Ward Charm, `equip 0` → refused, slot 0 takes a class accessory. (d) **On a profile with NO class** (a fresh account, or an alt that never ran `/rpg class`), `equip 0` with any accessory → `The class slot is locked: choose a class first`. | **(a) PASS, (b) PASS, (c) PASS** *(421d85a)*. **(d) NOT SPECIFIED** -- Ben reported "gates are good" and did not say whether (d) was read on a class-less account (PASS) or NOT READ |

> **(d) needs a class-less profile, and nothing can un-choose a class.** Read it on an account that
> has never run `/rpg class`. If none is available, record (d) as NOT READ rather than improvising a
> profile edit; `AccessorySlotsTest` covers the decision in the 2-second loop.

### A5 — a HELD accessory grants nothing

**Risk:** a stat key on the item, read by the all-slot scanners.

| prediction | READING |
|---|---|
| With nothing equipped, hold a Ward Charm in the **main hand**, then the **off hand** → `/rpg stats` Defense **unchanged** both times. Same with a Keen Charm → Crit Chance unchanged. | **PASS** *(421d85a)* |

### A6 — every station and command refuses an accessory EXPLICITLY, with the A4 words

**Risk:** a refusal that happens only by accident, in the wrong words (ruling A4).

| prediction | READING |
|---|---|
| Nexus enchant screen: placing an accessory → `Accessories cannot be enchanted.` and it stays on the cursor. Anvil → `Accessories cannot be worked at the anvil.` Grindstone → `Accessories have nothing to grind off.` `/rpg enchant show` holding one → `Accessories cannot be enchanted.` `/rpg gearscore set 300` → `Accessories carry no gear score.` `/rpg durability damage 1` → `Accessories do not wear out.` **None** reads "not one of your weapons…". | **PASS** *(421d85a)* |

> The enchant screen is level 10, the anvil 7, the grindstone 13 (`NexusStationGate`). Grant the
> level with `/rpg playerxp` first; a station refusal for LEVEL is not this row's reading.

### A7 — a freshly given accessory carries no enchant roll and no gear score

**Risk:** `/rpg give`'s acquisition path — `rollOnAcquire` would draw universal enchants (Unbreaking).

| prediction | READING |
|---|---|
| Give each of the six, hold each: `/rpg gearscore show` → `Held: no stamp (reads 100)` for all six; **no** tooltip carries an enchant line or a glint. | **PASS** *(421d85a)* |

### A8 — what is worn survives a restart

**Risk:** write-through not reaching disk; the load path.

| prediction | READING |
|---|---|
| Equip a quiver (slot 0) and a Ward Charm (slot 2). `run/plugins/Rpg/accessories/<uuid>.json` exists with **two** entries, slots **0** and **2**. Stop the server (normally), boot, rejoin: `/rpg accessory show` lists both in the same slots; `/rpg stats` shows their contributions without any action. | **PASS** *(421d85a)* |

### A9 — the four accessory materials are INERT in vanilla's hands

**Risk:** the half of §3.7 that was reasoned, not measured. **SURVIVAL is load-bearing here:** creative
does not consume, so every "not consumed" reading would pass for free.

| prediction | READING |
|---|---|
| For each of `echo_shard` (Ward Charm), `netherite_scrap` (Gauntlet), `shulker_shell` (Quiver), `prismarine_crystals` (Scroll): **brewing stand** — does not enter the ingredient slot, or sits there and brews nothing; **furnace** — not accepted as fuel; **smithing table** — produces no result in any slot; **crafting grid** (the 2x2 and a table) — no result that consumes it; **villager** — no trade whose price is the accessory; **right-click in air and on a block** — nothing is placed, eaten or used. **The accessory is still in the inventory afterwards, unchanged.** | **PASS** *(421d85a)* Nothing was consumed. Stations that accepted an accessory into a slot: **not reported** |

> A reading of "sits in the brewing ingredient slot" is **not** a failure by itself; a CONSUMED
> accessory is. Record which station accepted it, even if nothing was lost.

### A10 — a corrupt slot costs that slot, not the others

**Risk:** one bad entry taking the whole set down.

| prediction | READING |
|---|---|
| Wear a Ward Charm (slot 1) and a Keen Charm (slot 2). Stop the server. In `accessories/<uuid>.json`, replace slot 2's `item` text with `AAAA` (valid base64, not an item). Boot, rejoin: `/rpg accessory show` → slot 1 the Ward Charm, slot 2 `UNREADABLE -- kept verbatim`; `/rpg stats` Defense still carries the **+3**, Crit Chance **15%**; the sheet's block reads `Slot 2: unreadable (kept, contributes nothing)`. The file's slot-2 text is **still `AAAA`** after the session. | **PASS** *(421d85a)* |

> **And the structural half is a DIFFERENT outcome, by design:** a duplicate slot or bad JSON makes
> the whole store UNAVAILABLE for the session (SEVERE in the log, the sheet says `unavailable`),
> rather than presenting empty slots that the next equip would overwrite. Optional to read; if read,
> it goes beside this row, not in place of it.

### A11 — an ARROW-material accessory cannot be loaded

**Risk:** `QuiverAmmo` eating an accessory as ammunition.

| prediction | READING |
|---|---|
| Copy `ward_charm.yml` to `run/plugins/Rpg/content/accessories/arrow_probe.yml` with `material: arrow`. Boot **without** `--refresh-content`: the log reads `Skipping malformed accessory 'arrow_probe.yml': ... must be one of [echo_shard, netherite_scrap, prismarine_crystals, shulker_shell]`, and the Loaded line still reads **6 accessories**. Delete the probe afterwards. | **PASS** *(421d85a)* (probe deleted afterwards) |

### A12 — equip and unequip move the item exactly once

**Risk:** a duplicate or a loss at the write boundary.

| prediction | READING |
|---|---|
| Equip a charm into slot 3 → it **leaves the hand**, `Equipped Ward Charm in slot 3.` Equip another into slot 3 → refused `occupied`, and it **stays in the hand**. `unequip 3` → the charm **returns to the inventory**; `unequip 3` again → `Slot 3 is empty.` With a **full inventory**, `unequip` → the charm **drops at your feet** with `Your inventory was full -- dropped at your feet.` At no point do two copies exist. | **PASS** *(421d85a)* |

### A13 — the stats sheet carries the accessory block, on both surfaces (ruling Q4)

**Risk:** the two surfaces disagreeing; a drawback not visible where the total is.

| prediction | READING |
|---|---|
| Ranger, quiver in slot 0, Ward Charm in slot 1. `/rpg stats` ends with `Accessories`, then `  Fletcher's Quiver: +5% Crit Chance, +3 Ranged Damage, -0.20/5s Health Regen`, then `  Ward Charm: +3 Defense`. The **Nexus hub's stats head** tooltip ends with the **same three lines**. Unequip both → the block is **gone** from both, not a header over nothing. | **PASS** *(421d85a)* |

### A14 — class damage lands PER HIT on a real volley: the Dragon's Breath, 9 then 12

**Risk:** the class grant reaching the stats sheet (A3b) but not the hit — or reaching the press once
rather than every arrow. Added at the seat's review of `c381545`; prediction written before any boot.

**Staging — each step removes a different reason the popup would not read 9:**

- a **ranger** profile, holding a **Dragon's Breath**;
- `/rpg enchant clear` on it — a give-time roll can put **Power** on it, which multiplies the 9;
- `/rpg gearscore set 100` on it — the literal 9 is scaled by `score / 100`, and **100 is the identity**;
- the target is a Knell (`/rpg spawn knell`): 360 HP, so it survives several volleys, and no defense
  key. Fire at close range so several of the seven arrows land.

| prediction | READING |
|---|---|
| Quiver **unequipped**: one volley → every **non-crit** arrow's popup reads **9** (after the fire glyph). `/rpg accessory equip 0` holding the Fletcher's Quiver, then one more volley → every **non-crit** arrow's popup reads **12**. **Flat +3 PER HIT, before crit** — not +3 per press, and not +3 spread across seven arrows. **Read non-crit arrows only**: a crit doubles the whole hit, class bonus included (18 without, 24 with), and at 15-20% chance some of the seven will crit. **And read the IMPACT popups only**, not the Scorch burn ticks that follow, which are smaller and are not this row's quantity. | **PASS** *(421d85a)* -- **the per-arrow figures this row asks for were NOT recorded.** Ben, 2026-09-24: "skip the numbers. gates are good." The 9 / 12 prediction is therefore confirmed by Ben's verdict, not by a recorded figure |

---

## PREDICTIONS EDITED BEFORE ANY READING — seat review of `c381545`

**A1 and A13 were edited, not read.** Both printed the Quiver's drawback as `-0.04/s Health Regen`. The
seat ruled that the accessory lore and the sheet block use the stats sheet's own rate formatter, so the
same drawback now reads `-0.20/5s Health Regen`, in the unit the sheet's `Health Regen: 0.80/5s` total
uses. **No row in this file had been read when the edit was made**, which is the only condition under
which a prediction may be changed. A14 was ADDED at the same review, and is equally unread.

---

## CONJUNCTIONS WORTH WRITING DOWN BEFORE THE BOOT

- **A2 and A5 touch the Defense stat from opposite sides.** A2 proves the accessory reaches it through
  the STORE; A5 proves it does not reach it through the HAND. If A2 passes and A5 fails, the store is
  fine and the item carries a key it must not.
- **A3 and A3b are two gates on one item.** A3 is the profile class (who may wear it), A3b the held
  weapon (when its damage applies). A3 passing with A3b adding damage under a staff means the grant
  was merged after `matching`.
- **A3b and A14 read one number through two doors.** A3b reads the +3 off the stats sheet, and A14
  reads it off real hits. If A3b passes and A14 reads 9 with the quiver on, the stat converged but the
  cast path does not read it. If A14 reads 12 on only one arrow of the seven, it is being applied per
  press rather than per hit.
