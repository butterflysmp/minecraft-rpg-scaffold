# GATE — NEXUS-BACK: "Back" from the five input-slot menus, and Crafting's Recipe Book, without the close-and-reopen

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/nexus-back`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read. **NO BOOT until the seat has diffed the code AND this gate, and Ben says ready.**

**Brief:** the review seat's, 2026-11-14 (NEXUS POLISH 2). **Ben's report, verbatim:** *"When clicking the back arrow
within certain submenus, the UI closes and then re opens the nexus menu which is clunky and gets annoying really fast
because the players cursor resets. Is it reasonably possible to fix this?"* and *"These are the menus this happens
with: Ender Chest/vault, Anvil, Crafting table, enchanting, and grindstone"*. **Branch** `feat/nexus-back` from master
`84462b2c` (`git ls-remote origin refs/heads/master` → `84462b2c78cbf2491af8d1797abaf690da659c17`, 2026-11-14).
**The rule every AUTO row answers to:** `.claude/rules/verification.md`, *AN AUTO READING IS A READING OF THE SERVER,
NEVER OF THE CLIENT*.

```
ROWS     17   R0a R0b R0c R0d
              ST9R2
              NB1 NB2 NB3 NB4 NB5 NB6 NB7
              H1 H2 H3 C1
              LAST
         ──
         17   = 4 R0 + 1 + 7 NB + 3 H + 1 C + 1     git grep -c -E '^### ' <ref> -- docs/gates/GATE-nexus-back.md
```

The seven `NB` rows are the scenarios in `paper/src/main/resources/selftest/GATE-nexus-back.yml`, added LAST in
`selftest/index.yml`; `SelfTestScenariosTest` pins that each scenario key is a `### <id> ` heading here.

## WHAT CHANGED (navigation only; nothing a menu shows, costs or allows)

**Before:** each of the five menus with input slots (Anvil, Enchanting, Grindstone, Crafting, the Vault), and
Crafting's Recipe Book button, called `viewer.closeInventory()` and then hopped a tick and opened the next menu.
**Now:** they call `Menu.navigateTo(scheduler, next)`, the one place this is written. It runs `returnEverything()` at
once, in the click (the player's items are back in the inventory in the same tick, as before); makes the departing
menu inert (every click and drag on it is dropped from then on); and hops a tick and opens the next menu, whose
`openInventory` closes the old one **implicitly** (F1), which reaches `onClose`. No `closeInventory()`.

**Unchanged, by the seat's ruling 4:** every Close button and Esc. **The twelve `closeInventory()` calls left in the
menu package are all Close buttons** (Anvil, Build, BuildPicker, Crafting, Enchant, Equipment, Grindstone, Level,
Nexus, NexusSlotPicker, NexusVault, Settings: one each, each under a `CLOSE_SLOT` branch and followed by `return;`),
pinned by `MenuNavigationSignatureTest.everyRemainingCloseInTheMenuPackageIsACloseButton`. **Outside the package,
unchanged:** `RpgPlugin`'s shutdown close (after `returnEverything` is called directly), `RpgListeners`' two closes
on death and quit, and the selftest's `CLOSE` verb and restore. **Not migrated:** the menus with NO input slots
(Level, Settings, Build, Equipment, NexusSlotPicker, BuildPicker, RecipeBrowser's Back, the hub's own buttons) already
hop and open without an explicit close, which is the new shape minus a cursor give-back and the freeze; Ben does not
report them.

## THE JAR FACTS (javap, Paper 26.1.2 build 74, read 2026-11-14)

Server jar `run/versions/26.1.2/paper-26.1.2.jar` (MANIFEST `Implementation-Version: 26.1.2-74-e4e17fc`,
`Build-Number: 74`), read with `C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe -c -p -cp <jar> <class>`; API
`paper-api-26.1.2.build.74-stable.jar` likewise.

| fact | class#member |
|---|---|
| **F1.** `openInventory(Inventory)` on a `Bukkit.createInventory` inventory goes `CraftContainer.getNotchInventoryType` → `openCustomInventory` → `CraftEventFactory.callInventoryOpenEventWithTitle(ServerPlayer, AbstractContainerMenu, false)`, whose FIRST act after `startOpen` is: if `containerMenu != inventoryMenu`, `connection.handleContainerClose(new ServerboundContainerClosePacket(containerMenu.containerId), Reason.OPEN_NEW)`. That calls `CraftEventFactory.handleInventoryCloseEvent(player, OPEN_NEW)`, which builds and **calls `InventoryCloseEvent` synchronously** with the OLD menu's view, then `doCloseContainer`. Only after it returns does the `InventoryOpenEvent` fire and `ClientboundOpenScreenPacket` get sent. **Reason `OPEN_NEW`.** Our `RpgListeners.onMenuClose` routes by `event.getView().getTopInventory().getHolder()`, which is the old menu, so `handleClose` → `onClose` runs. **The one exception:** `handleContainerClose` returns before the event if `player.isImmobile()` (`ServerPlayer#isImmobile`: dead or dying, sleeping, removed, invalid, or disconnected) | `CraftHumanEntity#openInventory(Inventory)` (bytecode 281-291), `#openCustomInventory`, `CraftEventFactory#callInventoryOpenEventWithTitle(…Z)` (4-36), `ServerGamePacketListenerImpl#handleContainerClose(…, Reason)` (12-35), `CraftEventFactory#handleInventoryCloseEvent`, `ServerPlayer#isImmobile`, `Player#isImmobile` |
| **The explicit path, for comparison:** `HumanEntity.closeInventory()` is a default method → `closeInventory(Reason.PLUGIN)` → `Player.closeContainer(Reason)`, overridden in `ServerPlayer`: `handleInventoryCloseEvent(this, reason)`, then **send `ClientboundContainerClosePacket`**, then `doCloseContainer`. No `isImmobile` check | `HumanEntity#closeInventory()` (paper-api), `CraftHumanEntity#closeInventory(Reason)`, `ServerPlayer#closeContainer(Reason)` (0-27) |
| **F2.** Both paths run `ServerPlayer.doCloseContainer` → `containerMenu.removed(player)` AFTER the close event. `CraftContainer` does not override `removed`; `AbstractContainerMenu.removed` takes the carried (cursor) stack, sets it EMPTY and calls `dropOrPlaceInInventory` (`placeItemBackInInventory` for a live connected player, else `drop`). **Identical on both paths.** And our `onClose` runs inside the event, BEFORE `removed`, and its `returnEverything` has already cleared the cursor with `setItemOnCursor(null)`, so `removed` finds it empty either way. **The new Back also clears the cursor a tick earlier, in the click** | `ServerPlayer#doCloseContainer`, `AbstractContainerMenu#removed`, `#dropOrPlaceInInventory` |
| **F3.** Explicit: a `ClientboundContainerClosePacket` and then, a tick later, the next menu's `ClientboundOpenScreenPacket`. Implicit: **the open packet alone** (`openCustomInventory` 118-139). **What the client does with its screen and its mouse pointer on an open with no close before it is CLIENT behaviour; the jar cannot answer it. That is H1, Ben's** | `ServerPlayer#closeContainer(Reason)` (5-23), `CraftHumanEntity#openCustomInventory` |
| **F4.** Nothing else in the old container's removal differs: both paths reach the same `doCloseContainer` → `removed` → `inventoryMenu.transferState` → `containerMenu = inventoryMenu`. A `Bukkit.createInventory` inventory is a `CraftContainer`, which overrides `removed` for nothing, so no slot of ours is touched by the server on either path; our `returnEverything` is the only thing that empties an input slot | `CraftContainer` (members listed: no `removed`), `ServerPlayer#doCloseContainer` |

**The STOP condition in the seat's ruling 1 is not met:** the implicit close reaches `onClose` (F1), and treats the
cursor exactly as the explicit one does (F2). **The `isImmobile` exception is recorded, not guarded:** death
(`ServerPlayer#die` calls `closeContainer(Reason.DEATH)`) and disconnect close the screen themselves before the hop,
and the hop's entity task is retired with a removed player; a sleeping player cannot be clicking a menu, and nothing in
this plugin puts one to sleep. In every case the give-back already ran in the click.

## GAME MODE

**SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL (GATE-selftest.md ST7), and the HAND rows are read
in SURVIVAL too: CREATIVE's own-inventory screen decomposes gestures (`verification.md`, *THE CREATIVE-DIVERGENCE
REGISTER*), and its cursor is not the survival cursor.

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root (`mkdir` without `-p`
   refuses an existing folder; NB5 writes Ben's vault page and puts it back):

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-nexus-back"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   Report the folder, both pairs of counts, and `diff -rq lines: 0` for each.
3. **Check, do not assume, the join toggle in Ben's local file:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml`
   must print `12:  dev-pack-on-join: true` (it did when this was written, 2026-11-14). If it does not, STOP and report;
   do not edit it without the seat.
4. **Boot:** `./scripts/dev-server.sh --refresh-content`.
5. **R0 binds to THIS boot** (PLAN-mining.md F-M44): read R0 only on a `latest.log` with a fresh mtime whose
   `[Rpg] Build:` line names this PR's head.

**Also carried, not actions:**
- **The world's saved difficulty wins over `server.properties`.** The run sheet READS it (`/difficulty`) and SETS it
  (`/difficulty hard`) before the run: this gate summons nothing, but `/rpg selftest all` runs five gates that do.
- **The pack offered at LOGIN** is predicted unchanged from the mining-auto boot (`sha1=49f2e465b2b6bbb24380425830c8572f14acd11c
  bytes=8227`): `git diff --stat 84462b2c HEAD -- paper/src/main/resources/resourcepack paper/src/main/java/io/github/butterflysmp/rpg/paper/mining/MiningPack.java`
  prints nothing. A different sha1 is recorded, not an R0 failure.
- **Ben's vault page 1 needs ONE free storage cell** for NB5 (the boltor goes into the first free one). If the page is
  full, NB5's `shift_click` moves nothing, and that is a reading, not a fault. NB5 takes the boltor back out before it
  ends; if the row is stopped between those two steps, the boltor stays in the vault (the selftest restores the
  inventory, not the vault file).
- **What the run gives and restores:** setup gives one boltor (`rpg give boltor`) and sets 20 levels; the run's
  restore puts back Ben's inventory, XP, held slot and game mode as they were at RUN START.

## R0 — THIS PR'S BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

`GATE-mining-auto.md`'s R0 rows are SUPERSEDED at this boot, never PASS or FAIL.

### R0a — the build line names this PR's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/nexus-back` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries this branch

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `selftest/GATE-nexus-back.yml`, paper `menu/Departure.class`; in `selftest/index.yml`, the line `  - nexus-back`. ABSENT: the control `selftest/NoSuchClassControl.class` | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'selftest/GATE-nexus-back.yml',
               'io/github/butterflysmp/rpg/paper/menu/Departure.class',
               'io/github/butterflysmp/rpg/paper/selftest/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$r = New-Object IO.StreamReader($zip.GetEntry('selftest/index.yml').Open()); $idx = $r.ReadToEnd(); $r.Close()
if ($idx -match '(?m)^  - nexus-back\s*$') { "PRESENT index nexus-back" } else { "ABSENT  index nexus-back" }
$zip.Dispose()
```

### R0c — the Loaded line, unchanged, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes, 1 ores, 4 vein sizes`**, exactly L43 of the master `84462b2c` boot (11:41:01, 2026-11-14), because this branch changes nothing the loaders read: `git diff 84462b2c HEAD -- paper/src/main/resources/content \| wc -l` prints 0. No `Refusing`, `Skipping`, `SEVERE` or `Exception` beyond `volley_stone`'s known pair | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 57 scenarios across 11 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back]`**, and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | |

**The 57 and the 11 are DERIVED.** From the repo root at this head:

```bash
cd paper/src/main/resources/selftest
grep -cE '^  - ' index.yml                                   # gates: 11
cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'   # keys: 68
cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^setup:'           # setups: 11
```

Printed when this was written: **11, 68, 11**, so 68 − 11 = **57**. **The control:** the master `84462b2c` boot's L49
reads `50 scenarios across 10 gates [… mining-auto]`, which is the same count without this gate's 7 rows and 1 setup.

---

## THE INSTRUMENT'S ROW

### ST9R2 — the selftest's settle, re-predicted from the jar (the owed diagnosis; SURVIVAL) — witness: the log

**THE DIAGNOSIS, read-only, 2026-11-14.** `GATE-mining-auto.md` ST9R read 11 kills that left a body: 5 settled at
`ticks=20`, 6 at `ticks=1` or `2`, and it was NOT DIAGNOSED. **From the pinned jar and that boot's own log
(`run/logs/2026-11-14-5.log.gz`, the `2ca368ad` boot):**

1. **`/kill` selects every matching entity and calls `Entity#kill(ServerLevel)` on it.** For a `LivingEntity` that is
   `hurtServer(level, damageSources().genericKill(), Float.MAX_VALUE)` (`LivingEntity#kill`, 0-15); the death that
   follows leaves a body, and `LivingEntity#tickDeath` removes it (`RemovalReason.KILLED`) on the tick `++deathTime`
   reaches 20 (bytecode 0-53). That is the 20.
2. **The row-start CLEAN does not remove the last row's mobs. It SCHEDULES their removal:**
   `SelfTestSnapshot.removeTagged` calls `scheduler.onEntity(e, e::remove)` and counts the entity at once, so the
   `CLEAN … removed=1` line is logged before anything is removed. `Scheduler.onEntity` → `EntityScheduler.run` runs
   **on the next tick** (measured in `Menu.open`'s javadoc against the pinned paper-api). `CraftEntity#remove` sets
   `pluginRemoved` and calls `Entity#discard` (bytecode 0-29), which removes the entity whatever its health.
3. **So when a row's FIRST kill hits a mob the PREVIOUS row left, that mob dies from the kill and is then DISCARDED by
   the CLEAN's scheduled `remove()` one tick later**, long before `tickDeath` would reach 20. `MOBREMOVE` (our
   `EntityRemoveFromWorldEvent` listener, `RpgListeners.onEntityRemove` → `MobNameplateManager.onMobRemove`) logs
   either removal. Whether the settle reads `1` or `2` depends on whether the mob's scheduled task or the player's
   settle poll runs first within that tick's entity-scheduler phase; **that order was not read from the jar.**

**Checked against all 11 bodies of the `2ca368ad` run** (each killed uuid's `MOBSEED` line against its row's `START`
line, `zcat run/logs/2026-11-14-5.log.gz`):

| uuid (short) | row | seeded inside the row? | CLEAN before the kill | SETTLED |
|---|---|---|---|---|
| 35cc8fad, e32fd10e, 77109d12 | level WD1 | yes (L272, L284, L297; START L259) | no | ticks=20 ×3 |
| bafc04bd, 6cd75e06 | wither-status WS2c (2nd, 3rd) | yes (L896, L917; START L884) | no | ticks=20 ×2 |
| ef5158c0 | level WD3 | no (L308; START L314) | `removed=1` L315 | ticks=1 |
| ce42379d | legacy-b LB5 | no (L699; START L709) | `removed=1` | ticks=1 |
| 1fecd947 (Knell) | wither-status WS1 | no (L805; START L824) | `removed=1` L825 | ticks=2 |
| da5cd80a | wither-status WS2b | no (L839; START L859) | `removed=1` | ticks=2 |
| 8c842160 | wither-status WS2c (1st) | no (L870; START L884) | `removed=1` | ticks=2 |
| 376b436d | wither-status WS3 | no (L932; START L942) | `removed=1` | ticks=1 |

**11 of 11 separate on that one fact, and "hit before the kill" only correlated with it**: the mobs a row inherits
are the ones the previous row hit. **Which entity types:** any (zombie, wither skeleton, the Knell); the type does not
enter. **This is a property of the instrument, not of the game.** The settle still waits for every body (each fast
one's `MOBREMOVE` precedes its SETTLED), so ST9's fix holds; the fast settles are bodies the instrument itself
removed. **No code change is proposed here** (the seat asked for a diagnosis).

| prediction (dated 2026-11-14, before this boot) | READING |
|---|---|
| Over the whole `/rpg selftest all` run: **32** `CMD /kill` lines (`git grep -h -E '^\s*- cmd: "kill ' HEAD -- 'paper/src/main/resources/selftest/*.yml' \| wc -l` printed 32; this gate adds none), each followed by exactly one `SETTLED`, every one `dying=0`. **The per-line rule:** a kill that finds nothing (`No entity was found`), or kills the player, reads `ticks=0 first=0`; **a kill whose body was seeded INSIDE its row reads `ticks=20 first=1`; a kill whose body was seeded BEFORE its row's START, in a row whose `CLEAN` line reads `removed=` ≥ 1, reads `ticks=1` or `ticks=2`, `first=1`.** No other `ticks=` value. **Expected counts**, the `2ca368ad` run's, because no other gate's scenario file changed (`git diff --stat 84462b2c HEAD -- paper/src/main/resources/selftest` lists only `index.yml` and `GATE-nexus-back.yml`): 21 at `ticks=0`, 5 at `20`, 6 at `1`/`2`. The per-line rule is the prediction; the counts depend on what each kill finds that day | |

---

## THE ROWS

**The log instrument for every NB row:** `Select-String run\logs\latest.log -Pattern 'SELFTEST nexus-back|SELFTEST RUN'`,
read after Ben's LOGIN, inside the row's own `SELFTEST nexus-back <row> START … DONE` bracket.

**What every NB row drives, read from the code:** `click` and `shift_click` are synthetic `InventoryClickEvent`s into
`RpgListeners.onMenuClick` → `Menu.handleClick`. **`shift_click` is this gate's one new verb** (`Scenario.SHIFT_CLICK`):
a `SHIFT_LEFT MOVE_TO_OTHER_INVENTORY` on the first slot of one half of the open view holding a weapon id. **Why a verb
was added:** no existing verb can put an item into a menu's input slot. `click` is a LEFT `PICKUP_ALL` on a menu slot
found by NAME, and a plain place is only PERMITTED by the router and applied by vanilla, which never sees a synthetic
event; `MenuRouting.shiftMove` PERFORMS a shift-move itself (clear the source, write the destination, `MenuClick
itemMoved=true`), so a synthetic shift-click moves the item for real on the shipped path. **It skips** the client's
packet, its decode and the client's own prediction of the move. **No verb stages an item on the CURSOR**, and none is
added: the only route would be `setItemOnCursor`, which is staging rather than a shipped path, and the cursor branch of
`returnEverything` is unchanged code that runs in the same click as before; H3 is Ben's.

**What the AUTO rows can and cannot tell apart.** The `INVENTORY` lines read the same under the old Back and the new
one (both returned the items in the click), so they are the "nothing lost, nothing doubled" half. **The same-tick
`SCREEN` line is the half that tells them apart:** the old Back closed the screen in the click, so a `dump: menu` in
that tick read `SCREEN none`; the new one leaves the departing menu up for the hop. **Whether the CLIENT shows a close
is H1's.**

**The baseline** for each row is its first `INVENTORY boltor slots=[…]` line, call it B. Setup gives one boltor; B is
one entry `<slot>x1` plus whatever boltors Ben's own inventory held at run start (the run sheet records them).
"Same" below means the same NUMBER of entries, each `x1`; the slot index may differ, because `MenuSafety.give` is
`addItem`, which fills the first free slot.

### NB1 — Anvil: a boltor in the target slot, Back, and it is back in the click's own tick (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `HELD boltor …` (before) and the closing `HELD boltor …` are **identical after the item id**: type, `x1`, name, lore. After `shift_click`: `DRIVE shift-click … bottom slot=<n> boltor x1`; `SCREEN AnvilMenu`, with a `SLOT 20` line for the boltor (`AnvilMenuLayout.TARGET_SLOT`, the lowest input slot; `MenuRouting.firstEmptyInput` walks a `TreeSet`); `INVENTORY` = B less one entry. After `click "Back to the Nexus"`, **in the same tick:** `INVENTORY` = B; **`SCREEN AnvilMenu`, with NO `SLOT 20` line** (the departing menu, still up, its input empty). After `wait 3`: `SCREEN NexusMenu`; `INVENTORY` = B | |

### NB2 — Enchanting (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| As NB1, with `SCREEN EnchantMenu` and the boltor at **`SLOT 19`** (`EnchantMenuLayout.INPUT_SLOT`, the only input). Same tick after Back: `INVENTORY` = B, `SCREEN EnchantMenu` with no `SLOT 19` line. After `wait 3`: `SCREEN NexusMenu`, `INVENTORY` = B; HELD lines identical | |

### NB3 — Grindstone (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| As NB1, with `SCREEN GrindstoneMenu` and the boltor in the LOWEST tray cell (`GrindstoneMenuLayout.INPUT_SLOTS`, first free in index order; the number is read, not predicted). Same tick after Back: `INVENTORY` = B, `SCREEN GrindstoneMenu` with that `SLOT` line gone. After `wait 3`: `SCREEN NexusMenu`, `INVENTORY` = B; HELD lines identical | |

### NB4 — Crafting, the boltor in the grid (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| As NB1, with `SCREEN CraftingMenu` and the boltor in the LOWEST grid slot (`CraftingMenuLayout.GRID_SLOTS`; a boltor stacks to 1, so `topUpTarget` finds no room and `firstEmptyInput` places it). Same tick after Back: `INVENTORY` = B, `SCREEN CraftingMenu` with that `SLOT` line gone. After `wait 3`: `SCREEN NexusMenu`, `INVENTORY` = B; HELD lines identical | |

### NB5 — the Vault: Back leaves the saved page exactly as a close would (SURVIVAL) — witness: the log

The vault is storage, not a workbench: while healthy `returnedSlots()` is EMPTY, so `navigateTo`'s give-back returns
the cursor alone and the page stays; the hub's implicit close then reaches `onClose`, which writes the page FIRST
(`writeCurrentPage`), then claims (nothing, while healthy), then `returnEverything` (the cursor, already empty), then
`closed = true`. The old explicit close ran the same `onClose`, one tick earlier. While DEGRADED, `navigateTo` hands
the unpersisted cells back in the click and `onClose` writes nothing; `VaultBackOrderTest` compares the two orders on
the healthy, refused-write, degraded and degraded-plus-disconnect paths. **This row reads the healthy path only**; a
degraded vault cannot be staged by the instrument.

| prediction | READING |
|---|---|
| After `shift_click`: `SCREEN NexusVaultMenu` with a `SLOT` line for the boltor in the first free storage cell; `INVENTORY` = B less one. After `click "Back to the Nexus"`, **same tick:** `INVENTORY` = B less one (**the boltor STAYS in the vault**), `SCREEN NexusVaultMenu` with that `SLOT` line still present. After `wait 3`: `SCREEN NexusMenu`. Reopened (`close`, `menu`, `click "Vault"`): **`SCREEN NexusVaultMenu` with the boltor's `SLOT` line at the same slot** (a new `NexusVaultMenu` paints its page from `VaultService.vault(uuid)`, the store `writePage` updates, not from the screen it replaced; so this is the saved page as the service holds it, not the bytes on disk), and `INVENTORY` = B less one. After `shift_click "top boltor"` and `wait 3`: `INVENTORY` = B. HELD lines identical | |

### NB6 — Crafting → Recipe Book with a loaded grid → Back to crafting (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| After `shift_click`: `SCREEN CraftingMenu` with the boltor in the lowest grid slot; `INVENTORY` = B less one. After `click "Recipe Book"`, **same tick:** `INVENTORY` = B, `SCREEN CraftingMenu` with that `SLOT` line gone. After `wait 3`: `SCREEN RecipeBrowserMenu`. After `click "Back to crafting"` and `wait 3`: `SCREEN CraftingMenu` with **no** grid `SLOT` line and a `Back to the Nexus` slot (D1's breadcrumb, `GATE-nexus-polish.md`); `INVENTORY` = B | |

### NB7 — a departing menu is inert: a shift-click in the hop's tick is dropped (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `click "Back to the Nexus"` in the Anvil, then `shift_click "bottom boltor"` **in the same tick**: the driver logs `DRIVE shift-click … bottom slot=<n> boltor x1` (it found the boltor and fired the event), and the same-tick `INVENTORY` = B (**the router never ran**: `Menu.handleClick` returns at `departure.underway()` before `MenuRouting.route`). After `wait 3`: `SCREEN NexusMenu`, `INVENTORY` = B. **The control is in the reading:** without the freeze, the router performs the move and the same-tick `INVENTORY` reads B less one (the hub's implicit close would then hand it back, so the `wait 3` reading alone could not tell) | |

### H1 — the screen does not visibly close, and the pointer stays (SURVIVAL) — witness: Ben (Q1-Q12)

Ben, by hand, at 20 levels (the run sheet sets it and puts it back). For each of the six departures, open the screen
from the Nexus (`/menu`, then the station), move the mouse pointer well away from the middle of the screen, rest it on
**Back to the Nexus** (or **Recipe Book** for the sixth), click once, and do not move the mouse.

| departure | Q: did the Nexus (or the Recipe Book) appear WITHOUT the screen visibly closing first? | Q: is the pointer still where it was when you clicked? | READING |
|---|---|---|---|
| Vault → Back | Q1 | Q2 | |
| Anvil → Back | Q3 | Q4 | |
| Crafting → Back | Q5 | Q6 | |
| Enchanting → Back | Q7 | Q8 | |
| Grindstone → Back | Q9 | Q10 | |
| Crafting → Recipe Book | Q11 | Q12 | |

**Predicted yes on all twelve, and this is the row Ben's report is about.** It rests on F3 (only the open packet is
sent) and on what the client does with an open screen packet while a screen is open, **which the jar cannot answer**.
A "no" is a finding about the client, recorded with which departure.

### H2 — a real item in a slot comes back (SURVIVAL) — witness: Ben (Q13)

| prediction | READING |
|---|---|
| Ben runs `/rpg give boltor`, opens the Anvil from the Nexus, puts the boltor in the left slot by hand, and clicks Back. **Q13: in your inventory now, is there exactly one boltor from this, and nothing on the ground at your feet?** Predicted yes | |

### H3 — an item held on the cursor comes back (SURVIVAL) — witness: Ben (Q14)

| prediction | READING |
|---|---|
| In the Anvil (from the Nexus), Ben picks the boltor up from his own inventory so it is on the pointer, and clicks Back while holding it. **Q14: when the Nexus is up, is the boltor in your inventory (not on the pointer, not on the ground)?** Predicted yes: `returnEverything`'s cursor branch hands it back in the click (`setItemOnCursor(null)`, then `MenuSafety.give`) | |

### C1 — CONTROL: the Close button and Esc still close the screen and return the items (SURVIVAL) — witness: Ben (Q15, Q16)

| prediction | READING |
|---|---|
| Unchanged code (`closeInventory()` under each `CLOSE_SLOT`; Esc's close event). Ben puts the boltor in the Anvil's left slot and clicks the **Close** button: **Q15: did the screen close, and is the boltor back in your inventory?** Then the same with **Esc**: **Q16: did the screen close, and is the boltor back?** Predicted yes and yes. **This row is the control on H1:** if H1's "without the screen visibly closing" were satisfied by every screen change, Close would fail to look like a close here | |

### LAST — the run ends and restores (SURVIVAL) — witness: the log + Ben (Q17)

| prediction | READING |
|---|---|
| `SELFTEST RUN START all rows=57`; eleven setups in index order, `nexus-back` last; **68** scenario STARTs, each with exactly one `DONE` or `SKIPPED` (every SKIPPED a reading, with its reason); `SELFTEST RUN FINISHED`, then `SELFTEST RUN RESTORED xp=<Ben's> … gamemode=SURVIVAL …`. **No `SELFTEST` line contains `PASS` or `FAIL` as a word.** **Q17: is the item that was in your hand before the run back in your hand?** Predicted yes | |

## HOW LONG IT RUNS (computed from the files, not measured)

```bash
cd paper/src/main/resources/selftest; for g in $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - //'); do f=GATE-$g.yml; echo "$g waits=$(grep -E '^\s*- wait:' $f | tr -d '\r' | awk '{s+=$3} END{print s+0}') scenarios=$(grep -cE '^[A-Za-z0-9-]+:' $f)"; done
```

- **This gate:** waits **113** + 2 × 8 scenarios = **129 ticks, about 6.5 s.**
- **The whole `/rpg selftest all`:** the `2ca368ad` run's DONE ticks summed to **7905** (GATE-mining-auto.md LAST);
  plus this gate's 129, **≈ 8034 ticks, about 6 min 42 s at 20 TPS.**

## THE RUN SHEET, IN OUTLINE (the booter posts it literally, filled from readings in hand; no placeholder)

1. Ben logs in.
2. **`/gamemode survival`**, alone, the first line after LOGIN.
3. **`/difficulty`** (recorded), then **`/difficulty hard`**.
4. Ben stands where the earlier gates need him (flat open ground, about 12 blocks clear to the east), with **four
   inventory slots free** and **one free cell on vault page 1**. The booter records any boltor already in his
   inventory (the NB baseline).
5. **`/rpg selftest all`**. Wait for `SELFTEST RUN RESTORED` (about 6 min 42 s). Ben does nothing while it runs.
6. **The booter reads the `RUN RESTORED` line's `xp=` value**, then posts the HAND block with the restore command
   written out from that number (at the last boot it read `xp=4`, which would make it `/rpg playerxp set @s 4 xp`).
7. **HAND block:** `/rpg playerxp set @s 20 levels`; `/rpg give boltor`; H1 (Q1-Q12), H2 (Q13), H3 (Q14), C1 (Q15,
   Q16); then the restore command from step 6. The boltor given here is Ben's to keep or throw away.
8. **Q17** (LAST).
9. **`/stop`**.

## UNIT ROWS AND MUTATIONS (run 2026-11-14 against this branch's working tree, before this file was committed)

**The unit rows.** `DepartureTest` (4): the give-back runs in the click, before the hop is requested, and the open
only inside the hop; underway from the click and for good; a second departure does nothing; a re-entrant one from
inside the give-back is refused. `MenuNavigationSignatureTest` (8): `navigateTo` hands `Departure` the real
`returnEverything`, then the scheduler hop, then the open, with no `closeInventory`; `Menu.java` has no close at all;
`handleClick` and `handleDrag` drop everything on a departing menu right after cancelling; each of the six departures
goes through `navigateTo` with no close and no hop of its own; the five input-slot menus call no `open()`; the census
of twelve Close buttons; and the scanner's two controls (comments skipped, code seen; a method body ends at its own
brace). `VaultBackOrderTest` (5): the vault's close modelled on the real `VaultReturnPolicy` and `VaultCloseDisposal`,
old order against new, on four paths, plus a control that the model can see a difference. `SelfTestShiftClickDriverTest`
(1): the verb builds the action the router's performed shift branch keys on. `SelfTestScenariosTest`: eleven gates,
`nexus-back` last, 57 rows, NB1-NB7 paired with this file's headings.

Each mutation: one literal edit by `mut.pl` (refuses unless the original occurs exactly once), marker grep, original
grep, line delta against a pristine copy taken before the edit, `-Dtest='DepartureTest,MenuNavigationSignatureTest,VaultBackOrderTest,VaultWiringSignatureTest'`
(36 tests), restored by `cp`, `cmp`-identical, markers left 0.

| mutation | edit | marker / original / lines | reddened |
|---|---|---|---|
| MUT-NB-NOGIVEBACK | `navigateTo` passes `() -> { }` instead of `this::returnEverything` | 1 / 0 / 1 | `MenuNavigationSignatureTest.navigateToGivesBackFirstThenHopsAndNeverClosesExplicitly` |
| MUT-NB-DEPNOGIVE | `Departure.begin` never runs `giveBack` | 1 / 0 / 1 | `DepartureTest`: 3 of 4 (`theGiveBackRunsInTheClick…`, `aSecondDeparture…`, `aReentrantDeparture…`) |
| MUT-NB-CLOSEBACK | the explicit close put back: `viewer.closeInventory()` before `next.get().open()` | 1 / 0 / 1 | `navigateToGivesBack…`, `menuJavaHasNoExplicitCloseAnywhere`, `everyRemainingCloseInTheMenuPackageIsACloseButton` |
| MUT-NB-BYPASS | the Anvil's Back reverted to the old close-then-hop | 1 / 0 / 1 | `eachOfTheSixDeparturesGoesThroughNavigateTo…`, `theFiveInputSlotMenusOpenNothingThemselves`, `everyRemainingClose…` |
| MUT-NB-NOFREEZE | `handleClick`'s `departure.underway()` return removed | 1 / 0 / 1 | `aDepartingMenuIsInertToClicksAndDrags` |
| MUT-NB-NOWRITE | the vault's `onClose` write replaced by `false` | 1 / 0 / 1 | `VaultWiringSignatureTest.theCloseFlushesBEFOREItHandsAnythingBack` (an existing row; `VaultBackOrderTest` is a model and cannot see this file) |

**What no unit row can see, and is this gate's to read:** that the implicit close reaches `onClose` on the live server
(NB5's reopened page; every NB row's hub), that the departing menu is still up in the click's tick (the same-tick
`SCREEN` lines), that the freeze drops a real event (NB7), and everything the client does (H1).
