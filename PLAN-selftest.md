# PLAN — SELFTEST: an op-only instrument that stages and drives the log-witnessed gate rows

**Status: PHASE 2 BUILT as #175, NOT RUN** (2026-10-05). Phase 1 was the survey below (sections 0-7), unchanged; section 8
records the six reads and section 9 the seat's rulings.

**Asked for:** the seat, 2026-10-05, after Ben's *"Yes, that would be nice"* (2026-10-05). The seat owns the
mechanism, because it is a seat-domain instrument; **Ben rules only on what he must still see.**

**Read against:** the stack top `feat/withered-shortbow` at **`f33ded99`** (#174, after ruling 1's rebase), the pinned
`paper-api-26.1.2.build.74-stable.jar` and the server jar `run/versions/26.1.2/paper-26.1.2.jar`. Every jar claim
below was read with `javap -c -p` (JDK at `~/.jdks/openjdk-26.0.1/bin`), and the bytecode offsets are quoted. Anything
not read says **NOT READ**. Repo claims name a class and method, never a line.

---

## 0. THE SHAPE, IN ONE PARAGRAPH

`/rpg selftest <gate> [row] | all | stop`, behind `Permissions.DEV`, runs only on a server marked as a dev server,
with exactly one player online. It runs one **scenario** per gate row. A scenario is a short list of **steps** from a
fixed vocabulary (`cmd`, `wait`, `summon`, `face`, `attack`, `use`, `swing`, `stone`, `click`, `dump-menu`, `probe`).
Each verb is implemented once in Java and drives a **shipped** entry point: a real command, a real Bukkit event into
our real listener, or a vanilla attack. The scenarios live in YAML beside the gates, keyed `<gate>#<row>`. The
instrument writes `SELFTEST` bracket lines around the real witness lines (PLAYERHIT, DOTTICK, MOBSEED…) that the
row causes. It **never prints PASS or FAIL**. Readings are still judged by a person, against the committed
predictions, from the real lines.

---

## 1. THE DRIVERS, READ FROM THE JAR

### 1.1 Melee: `Player#attack(Entity)` IS the click's path from `Player.attack` down; it skips the packet's gates

**The API.** `attack(Entity)` is declared on `org.bukkit.entity.LivingEntity`, not on `Player`. It is implemented in
`CraftLivingEntity.attack`: after a world-generation precondition, `40: instanceof net/minecraft/world/entity/player/Player`
→ `60: invokevirtual Player.attack(Entity)`. **That is the same NMS method the packet handler calls**
(`ServerGamePacketListenerImpl.handleAttack` `266: ServerPlayer.attack`; `ServerPlayer` does not override `attack`).

**What `Player.attack` does, in the order our listeners see it:**

| step | bytecode | what it means for us |
|---|---|---|
| `PrePlayerAttackEntityEvent` | `14: new`, `35: callEvent`, `38: ifeq 469` (a cancel aborts) | **our `RpgListeners.onPrePlayerAttack` runs**, reads `getAttackCooldown()` into `MeleeHits.record` |
| charge | `88: ldc 0.5f; 91: getAttackStrengthScale(F)F` | read from the real ticker |
| reset | `124: onAttack(Entity)` → `PlayerAttackEntityCooldownResetEvent`, then `33: resetOnlyAttackStrengthTicker` | **every call resets the charge**: the driver must WAIT real ticks between swings, as Ben does |
| crit | `canCriticalAttack`: `fallDistance > 0`, `!onGround`, … | **a standing call never vanilla-crits** (our crit is ours, rolled in `BukkitCombatant.snapshot`, and is unaffected) |
| sweep | `isSweepAttack`: full strength, no crit, no sprint knockback, `onGround`, slow, main hand `is(ItemTags.SWORDS)` | a standing full-charge swing with a SWORDS-tagged item **does** sweep (`399: doSweepAttack`) → our `onPlayerSweepAttack`. **Whether our minted weapons carry SWORDS: NOT READ** |
| damage | `349: hurtOrSimulate` → `hurtServer` → `CraftEventFactory.handleLivingEntityDamageEvent` → `callEntityDamageEvent` `24: new EntityDamageByEntityEvent`, cause `ENTITY_ATTACK` | **our `RpgListeners.onPlayerMeleeAttack` runs** → `WeaponFire.landVanillaMelee` → `CastExecutor.landBasicMelee` → **PLAYERHIT** |

**What it SKIPS** (all in `handleAttack`, before `266`): `16: isImmobile`, `24: hasClientLoaded`, `34: isSpectator`,
`98: WorldBorder.isWithinBounds`, **`142: isWithinAttackRange` (the reach check)**, **`153: PIERCING_WEAPON`**,
**`254-255: cannotAttackWithItem(item, 5)` (the `MINIMUM_ATTACK_CHARGE` gate)**, the invalid-target kicks. **And the
arm swing is a separate packet** (`handleAnimate`), which fires `PlayerArmSwingEvent` (`289: new`, `318: callEvent`)
and, on a miss, `PlayerInteractEvent LEFT_CLICK_AIR` (`137`). `attack()` fires neither; `swingMainHand()` only sends
an animation packet and fires **no event**.

**Consequence.** Every melee `sent=` row (WD1, MC6, MC8) is drivable on the real path. **No row that asks about reach,
minimum charge or the swing packet is.** Our `packet/WeaponSwingListener` (the weapon's left-click, decoded on the
Netty thread) is never reached by `attack()`; for a vanilla-driven basic melee that does not matter, because
`WeaponFire.attempt` returns empty for it (`BasicMelee.isVanillaDriven`) and the hit is delivered by the damage
event.

### 1.2 Weapon right-click and stone inputs: a synthetic Bukkit event into OUR listener

**There is no API that simulates a use-item.** `ServerboundUseItemPacket` is handled in `handleUseItem`, which fires
`PlayerInteractEvent` (`246: getstatic Action.RIGHT_CLICK_AIR`, `252: callPlayerInteractEvent`) and then
`512: ServerPlayerGameMode.useItem`. `LivingEntity#startUsingItem` skips both (it calls `LivingEntity.startUsingItem`
directly).

**But the events are constructible** (read from the API jar):
- `PlayerInteractEvent(Player, Action, ItemStack, Block, BlockFace, EquipmentSlot)`;
- `io.papermc.paper.event.player.PlayerArmSwingEvent(Player, EquipmentSlot)`, a `PlayerAnimationEvent`;
- `PlayerDropItemEvent(Player, Item)`;
- `InventoryClickEvent(InventoryView, SlotType, int, ClickType, InventoryAction)`.

**So the proposal is the synthetic event, not a direct call.** `Bukkit.getPluginManager().callEvent(new
PlayerInteractEvent(p, RIGHT_CLICK_AIR, held, null, null, HAND))` runs **every** registered listener, ours included:
`RpgListeners.onRightClick`, with its star, stone, hijacked-block and Plume checks, then `WeaponFire.attempt(player,
"right_click", …)` → `WeaponService.fire` (cooldown and mana, atomically) → the quiver → `CastExecutor`. **This is
the shipped path from the Bukkit event down.** The cheaper alternative, calling `WeaponFire.attempt` directly,
skips the listener's preamble and is **rejected**: it is the "parallel path" the seat forbids.

| input | synthetic event | our listener | first post-decode method |
|---|---|---|---|
| weapon right-click | `PlayerInteractEvent RIGHT_CLICK_AIR, HAND` | `RpgListeners.onRightClick` | `WeaponFire.attempt(p, "right_click", …)` |
| stone Right | the same | `onRightClick` → `stoneCaster.onInput(p, RIGHT, true, false)` | `StoneCaster.onInput` |
| stone Left | `PlayerArmSwingEvent(p, HAND)` | `RpgListeners.onStoneSwing` → `StoneCaster.onSwing` (1-tick defer, `SwingGuard`) | `StoneCaster.onInput(p, LEFT, …)` |
| stone Q | `PlayerDropItemEvent` — **needs an `Item` entity, and whether `HumanEntity#dropItem(EquipmentSlot, …)` fires the event itself: NOT READ** | `RpgListeners.onNexusDrop` | `StoneCaster.onInput(p, DROP, …)` |
| weapon left-click (non-melee) | **none**: decoded on the Netty thread in `packet/WeaponSwingListener.onPacketReceive`, then `bukkit(…)` → `onSwing` | — | **BEN** |

**What a synthetic event can NOT witness:** the packet decode (which hand, which action the client sent), the
client's held-right-click cadence (the 4-tick grid is a client property: `WeaponLoader`'s javadoc, *UNDER HELD
FIRE*), and anything the server does in `handleUseItem` before the event (rotation snap, `checkLimit`). **Those stay
BEN rows** (MC3's "the stone's inputs", SB4/WB7's held cadence).

**One hazard to design around:** a synthetic event reaches other plugins' listeners too. On the dev server there are
none of ours that would double-act, but the instrument must refuse to run if any plugin other than ours and
PacketEvents is enabled (§4).

### 1.3 Commands: `performCommand` is the same dispatcher, minus the preprocess event and the log line

`CraftPlayer.performCommand` → `21: CraftServer.dispatchCommand` → `67: Commands.getDispatcher()`, `77: parse`,
`146: Commands.performCommand`. The typed path (`performUnsignedChatCommand`) ends in `156: Commands.performCommand`
on the same `Commands`. **So `/rpg …`, `/level`, `/build` etc. run their real Brigadier handlers, including the
`.requires(…hasPermission…)` gates.** *That the `LifecycleEvents.COMMANDS` registrations land in that same tree is
inferred from both paths using the one dispatcher, NOT traced.*

**Skipped:** `PlayerCommandPreprocessEvent` and the **`{} issued server command: {}`** log line (both only in
`performUnsignedChatCommand`, offsets 16-70). Nothing of ours listens to the preprocess event: `git grep PlayerCommandPreprocessEvent f33ded99 -- *.java` finds 0
files (control: the same grep for `PlayerInteractEvent` finds 2). **So the instrument logs its own `SELFTEST … CMD /<command>` line**, or a reader will see no command in the
log at all.

### 1.4 Replies and screens: two new read instruments, and what each is a true witness of

**Screens.** All present in the API jar: `HumanEntity#getOpenInventory`, `InventoryView#title()` (Component),
`getTopInventory()`, `ItemStack#effectiveName()`, `ItemMeta#displayName()`, `ItemMeta#lore()`. Our menus are
`Menu implements InventoryHolder`, so **identity is `getTopInventory().getHolder() instanceof LevelMenu`**, never
the title. Proposed `dump-menu` step: one line per non-filler slot,
`SELFTEST <gate> <row> SLOT <n> <material> name=<plain> color=<#hex> lore=[<plain>|<#hex>; …]`, serialised with
`PlainTextComponentSerializer` plus each component's own `color()` and decorations.

- **A true witness of:** which screen opened, every name and lore line **as the server built it**, its colour as
  authored, and the slot.
- **NOT a witness of:** how the client renders it (font, a colour Ben would call green), the cursor not jumping on a
  page flip (LS3), or a tooltip's line order **as the client draws it** (the client adds lines of its own).
- **Clicks** (D1's Recipe Book → Back): a synthetic `InventoryClickEvent` into `RpgListeners.onMenuClick` →
  `Menu.handleClick`. Real routing; not the client's click packet.

**Chat replies** (CP2, CM2, the `/rpg apply` `Refused` line, `/rpg stats`). `performCommand` returns only a
boolean, and `Player#sendMessage` cannot be intercepted by Bukkit. **Two options, the seat's call:**
- **(A) A PacketEvents send-side reader** of system-chat packets to the test player, which only **copies** the
  component into a concurrent queue (CLAUDE.md's threading rule: read, compute, touch nothing else); the scenario
  drains the queue on the player's own scheduler and logs `SELFTEST … REPLY <plain> color=<#hex>`. **It reads what
  was actually sent to the client.** *Cost: a packet listener, and its `SYSTEM_CHAT` wrapper is NOT READ.*
- **(B) No reply capture.** The reply rows stay AUTO-PARTIAL, and Ben reads the chat.

Recommended: **(A)**, because without it CP2, BN1, CM2 and WS2c's reply half stay with Ben.

### 1.5 Staging, and the threads it runs on

| need | API (read) | thread |
|---|---|---|
| wait real ticks | `Scheduler.onEntityLater(player, r, n)` (`EntityScheduler.runDelayed`) | the player's |
| summon a plain zombie | `World#spawn(Location, Class, Consumer)`; NoAI by `setAI(false)` inside the consumer. **Or `/summon` via `performCommand`**, which is the gates' own recipe, so `MOBSEED … source=rolled` is the same line | the location's region |
| GS-20 / GS-200 | `performCommand("rpg spawn zombie 20")`, the gate's recipe | the command's |
| aim | `teleportAsync(loc with yaw/pitch)`; "facing entity eyes" computed from `getEyeLocation()` | the player's |
| level / XP | `performCommand("rpg playerxp set @s 50 levels")`, the gate's command | — |
| gear score | `performCommand("rpg gearscore set 100")` | — |
| `spawn_mobs` | `World#setGameRule(GameRules.SPAWN_MOBS, false)`. **Use `org.bukkit.GameRules`; `GameRule.DO_MOB_SPAWNING` is `Deprecated, forRemoval=true`** (javap -v) | global |
| time | `World#setTime(18000)` | global |
| mobtrace | `MobNameplateManager.toggleTrace()` returns the new state; there is **no setter**, so the instrument toggles until ON and records whether it toggled | — |

**Every wait is a real wait.** The reconcile is 5 ticks, a full emberblade charge ~12.5, a DOT period 40. The
scenario is a chain of `onEntityLater` continuations, never a sleep, so it runs on Folia unchanged. **`async` is
never used** (it may not touch Bukkit).

---

## 2. THE ROW INVENTORY, #168-#174

**AUTO** = staged and driven by the instrument on a shipped path, witnessed by a log line. **AUTO-PARTIAL** = one
half AUTO (named), the other BEN. **BEN** = needs the client or an eye. **AUTO\*** = AUTO only with reply capture
(§1.4 A); without it, AUTO-PARTIAL. **CORE** marks the seat's merge bars. "Driver" names the entry point from §1.

### #168 `GATE-melee-cell.md`

| row | class | driver / what stays with Ben |
|---|---|---|
| **MC1** CORE | AUTO-PARTIAL | `cmd /build` + `dump-menu`: the class options and their order, Fire only, "Fire Melee" and its colour as built. Picking Melee by a synthetic click. **Ben:** it reads gold on his screen |
| **MC2** CORE | AUTO-PARTIAL | `dump-menu` of the loadout slots after the pick. **Ben:** a glance |
| **MC3** CORE | AUTO-PARTIAL | **AUTO:** the three casts' PLAYERHIT lines via `PlayerArmSwingEvent` (Left), `PlayerInteractEvent` (Right), drop event (Q, *driver NOT READ*). **Ben:** his three real inputs cast them, and the stone never leaves its slot (the input decode) |
| MC4 | AUTO-PARTIAL | menu dumps of the two other cells |
| MC5 | BEN | the sword dropping is the client's Q |
| MC6 | AUTO | `attack()`, wait 40 t, `attack()` → `S`; `setHeldItemSlot` to the stone and back, `attack()` at once → below `S`. *Whether a server-side slot change resets the ticker the way a client swap does: NOT READ* |
| MC7 | AUTO-PARTIAL | picker dumps |
| MC8 | AUTO-PARTIAL | hits by `attack()`; the Gauntlet into the class slot by a synthetic click on the Equipment screen (*the slot's click path NOT READ*) |
| MC9 | AUTO | stone Right ×2, with and without the Gauntlet |
| MC10 | AUTO | teleport to d = 2.0, stone Right; the instrument logs `d` and pitch it measured |
| MC12 | AUTO | stone Left; `probe` logs `fireTicks` before and 20 t after |
| MC13 | AUTO | as MC10 at 0.6, **logs the measured d** and re-stages if ≥ 0.935 (the gate's VOID rule, applied by the instrument and logged) |
| MC14 | AUTO | as MC10 at (3.2, 3.2), facing feet |
| MC15 | AUTO-PARTIAL | creative hotbar Q via the drop event; the inventory-Q refusal is a client screen action: BEN |
| MC16 | AUTO | toggle, stone Right, toggle |
| MC17 | NOT READ | needs a second player, and the instrument refuses to run with one (§4) |

### #169 `GATE-level.md`

| row | class | driver / Ben |
|---|---|---|
| CP1 | AUTO\* | `cmd playerxp` + reply + `dump-menu` of the stats head |
| **CP2** CORE | AUTO\* | the same at 50 |
| CP3 | AUTO\* | the same at 60, then `/rpg stats` |
| **BN1** CORE (L1, L50) | AUTO\* | `cmd rpg stats` replies at L1 and L50 |
| BN2 | AUTO\* | the same, emberblade in hand and empty |
| **WD1** CORE | AUTO | `attack()` at L1 and L50, 40 t apart |
| WD2 | AUTO | `use` (synthetic right-click) at L1 and L50 |
| **WD3** CORE | AUTO | stone Left at L50 |
| **LS1** CORE | AUTO-PARTIAL | `dump-menu` of the star (the hint line), a synthetic click on the head, `dump-menu` of `LevelMenu`. **Ben:** a glance |
| LS2 | AUTO-PARTIAL | as LS1 at 50 |
| LS3 | AUTO-PARTIAL | paging and Back by clicks; **"the cursor does not jump" is BEN** |

### #170 `GATE-nexus-polish.md`

| row | class | driver / Ben |
|---|---|---|
| **R0d** CORE | AUTO already | a boot log line; the instrument has nothing to drive |
| **D1** CORE | AUTO-PARTIAL | three synthetic clicks and a dump after each. **Ben:** the button is where it says |
| D2-D5 | AUTO-PARTIAL | dumps; D3's two items by `give` |
| **G1** CORE | AUTO-PARTIAL | dumps carry each name's `#hex`. **Ben:** green reads green |
| G2 | AUTO-PARTIAL | the vault dump, a click on page 2, the reply\* |
| **G3** CORE | AUTO-PARTIAL | the last lore line of each button, with colour |
| **CM1** CORE | AUTO | `performCommand` per screen; holder class + title; Back by a click |
| **CM2** CORE | AUTO\* | six commands; **no screen opened** (the holder is unchanged) + the six replies with colour |
| CM3-CM5 | AUTO / AUTO\* | as CM1-CM2; CM5 sets creative and restores it |
| CM6 | AUTO-PARTIAL | the console half by `Bukkit.dispatchCommand(console, "build")` (the reply is in the console log). **The dead half is about whether the client can send a command: BEN** |

### #171 `GATE-legacy-a.md`

| row | class | driver / Ben |
|---|---|---|
| SB1 | AUTO-PARTIAL | `give` twice + an inventory probe (two slots); **the tooltip as drawn: BEN** (`GoldenLoreTest` already pins the text) |
| **SB2** CORE | AUTO | `use` at `~8`, PLAYERHIT `element=kinetic sent=16.000` |
| SB3 | AUTO-PARTIAL | a walking zombie; `probe` logs its velocity for 5 t after the hit (**a new instrument**; the push as seen stays BEN) |
| SB4 | AUTO-PARTIAL | the magazine and reload by `use` every 12 t with a quiver probe; **the held cadence is the client's: BEN** |

### #172 `GATE-legacy-b.md`

| row | class | driver / Ben |
|---|---|---|
| LB1 | AUTO-PARTIAL | the stack and "left-click does nothing" (mana, cooldown probe after a `PlayerArmSwingEvent`); the tooltip: BEN |
| **LB2** CORE | AUTO | `use` at `~8`: PLAYERHIT `sent=30.000`, and the three warnings absent |
| LB3 | BEN | the sprite |
| LB4 | AUTO-PARTIAL | a probe logging the body's y each tick is possible but new; **as seen: BEN** |
| LB4b | AUTO | fire at pitch −90; count `small_fireball` at 80 t and 200 t (the gate's own `execute if entity` by `performCommand`, reply\*) |
| **LB5** CORE | AUTO-PARTIAL | (a)(b): a block scan of the wall's face for `FIRE` and of the TNT block for `TNTPrimed` after each shot, logged. (c) the vanilla summon by `performCommand`. **Ben:** "vanishes at the wall" |
| LB6 | AUTO-PARTIAL | broken blocks and the zombie's position by probe; sound and particles: BEN |
| LB7, LB9, LB10 | BEN | visuals, sound, feel |
| LB8 | AUTO | health, `fireTicks` and PLAYERHIT absence, logged |
| LB11 | UNWITNESSED | as written |

### #173 `GATE-wither-status.md`

| row | class | driver / Ben |
|---|---|---|
| WS-C | AUTO-PARTIAL | spawn two knells, `mobdamage` on the first (aim by teleport), neighbour health logged; the blast particles: BEN |
| **WS1** CORE | AUTO | GS-20, `apply withering 200`, five DOTTICK 40 apart |
| **WS2b** CORE | AUTO | GS-100, the same |
| **WS2c** CORE | AUTO-PARTIAL\* | the two `Refused` replies\*, DOTTICK absence over 240 t, the control. **Ben:** no swirl |
| **WS3** CORE | AUTO-PARTIAL | the death, DOTTICK, the neighbour's health unchanged. **Ben:** no explosion seen |
| WS3-P | AUTO | the two applies **one tick apart**: the instrument can stage Q-W8's exact case every time, which a hand cannot |
| WS4 | — | moved to WB3 |
| WS5 | AUTO | re-apply every 10 t for 120 t |
| WS6 | AUTO-PARTIAL | the health after 12 s; the swirl: BEN |
| WS8, WS9 | UNWITNESSED | as written |

### #174 `GATE-withered-shortbow.md`

| row | class | driver / Ben |
|---|---|---|
| R0a-c | AUTO already | boot-log and jar readings |
| **WB1** CORE | BEN, with an AUTO half | the stack by `give` twice; **the tooltip as drawn: BEN** |
| **WB2** CORE | AUTO | `use` at `~8`: PLAYERHIT `element=wither sent=16.000`, `caster=` recorded |
| **WB3** CORE | AUTO-PARTIAL | five DOTTICK with `applier=` = WB2's `caster=`; `fireTicks` probe (no fire). **Ben:** the swirl |
| WB4b | AUTO | GS-200 |
| WB5 | AUTO-PARTIAL | the deaths and the neighbour's health; no explosion seen: BEN |
| WB6 | AUTO-PARTIAL | the velocity probe, as SB3 |
| WB7 | AUTO-PARTIAL | as SB4 |

---

## 3. THE OUTPUT

```
SELFTEST <gate> <row> START scenario=<file>#<row> selftest=<build sha> player=<uuid>
SELFTEST <gate> <row> CMD /rpg apply withering 200
SELFTEST <gate> <row> SLOT 13 EXPERIENCE_BOTTLE name=Level 1 color=#55FF55 lore=[…]
SELFTEST <gate> <row> PROBE fireTicks=-1 health=20.0 d=2.004 pitch=0.0
SELFTEST <gate> <row> REPLY Refused: wither_skeleton is immune … color=#AAAAAA
   … the real lines the row caused, unchanged: PLAYERHIT …, DOTTICK …, MOBSEED …
SELFTEST <gate> <row> DONE ticks=<elapsed>
SELFTEST <gate> <row> SKIPPED <why>
```

- **It never prints PASS, FAIL, OK, ✓ or an expected value.** It does not read the gate's predictions. A reader takes
  the lines between START and DONE and judges them against the committed row, as today.
- **The real witness lines are not re-emitted or reformatted**: PLAYERHIT, DOTTICK and MOBSEED come from the shipped
  `TracedHit.line`, `DotTick.line` and `MobNameplateManager`, through the shipped `mobTrace` gate.
- **A row's window is START to DONE.** A DOTTICK arriving after DONE belongs to no row; DONE waits each scenario's
  declared settle time (DOT rows: the window plus one period).
- **SKIPPED is a reading, not a silence.** A row that cannot stage (no space, a refused cast, an unexpected `max=`)
  says why. A scenario that throws logs `SELFTEST <row> ABORTED <exception>` and the run stops after restoring.

---

## 4. SAFETY

- **Op-only:** `.requires(hasPermission(Permissions.DEV))`, as every dev command.
- **A dev server only. There is NO dev-world notion in this repo today** (searched: no world-name check, no flag, no
  property; `config.yml` holds only `immobilize.anchor-drift-blocks`). **Proposed: a JVM property `-Drpg.dev=true`
  set by `scripts/dev-server.sh` and by nothing else.** Absent → `Refusing: not a dev server`. The seat's call: the
  property, or a `config.yml` key that a production config would never carry.
- **Exactly one player online**, and **no plugin enabled other than ours and PacketEvents** (synthetic events reach
  every listener, §1.2). Otherwise it refuses and names what it found.
- **Restores what it changed, from a snapshot taken before the first step:** lifetime XP
  (`ProfileService.setLifetimeXp`, the sanctioned lowering; **it writes to disk**), game mode, inventory contents
  and armour and the accessory slots, held slot, position and rotation, the Build cell and loadout, `spawn_mobs`, the
  time, the mobtrace state, mana. Every mob it spawned carries the tag `selftest` and is removed.
- **A crash mid-run cannot restore from memory.** So the snapshot is also written to `plugins/Rpg/selftest-restore.json`
  **before** the first change, and deleted after the restore. On the next join of that player, a leftover file is
  restored and reported. *Proposed; the Build-cell and accessory serialisation is the bulk of the work.*
- **Cancellable:** `/rpg selftest stop` cancels the pending continuation and runs the restore. Leaving, dying (other
  than CM6's staged death) or changing world cancels too.

---

## 5. SCALE AND HOME

**Scenarios are data, as content is** (CLAUDE.md invariant 2). One YAML file per gate,
`paper/src/main/resources/selftest/GATE-<name>.yml`, one entry per row, keyed by the gate's own row id:

```yaml
WB2:
  setup: [ level: 1, gearscore: 100, give: withered_shortbow, mobtrace: on ]
  steps:
    - summon: { recipe: gs100, offset: [8, 0] }
    - face: { entity: last, anchor: eyes }
    - use: right_click
    - wait: 40
```

- **Adding a row is a YAML entry; adding a verb is Java**, and verbs are few. The verbs are the only code that touches
  a driver, so each is read against the jar once.
- **A unit test pins the pairing:** every key in `selftest/GATE-x.yml` must be a `### <id>` heading in `GATE-x.md`,
  and a scenario naming a row that does not exist fails the build. This is the guard against a scenario silently
  driving a renamed row.
- **Home: a permanent, dev-gated feature merged to master** (agreeing with the seat's lean). It is only useful if every
  later gate can be written with scenarios beside it.
- **Where in the stack: a new slice on top of #174** (the seat's lean), **with one consequence the seat must rule on.**
  The ONE R0 is on `GATE-withered-shortbow.md` and names #174's head. A boot that uses the instrument boots **#175's**
  tip, so (i) the stack's one R0 moves to the selftest gate, and #174's R0 is SUPERSEDED like the rest, or (ii) #174
  is booted first with Ben's run sheet as it stands, and the instrument is proved on a later boot. **Not decided here.**
- **The instrument needs its own gate**, and its first rows are controls: one known-PLAYERHIT row run against a
  mutation that moves `sent=` must show the moved number, so a selftest reading is known to be able to differ.

---

## 6. HONESTY: HOW A READER TELLS AN AUTO READING FROM A BEN READING

**Proposed rule wording (for `verification.md`, the account; CLAUDE.md gets the pointer):**

> **AN AUTO READING IS A READING OF THE SERVER, NEVER OF THE CLIENT.** A row read by `/rpg selftest` is recorded
> **`AUTO`**, with the selftest build's sha and the START/DONE lines quoted. **It witnesses only what its driver
> reaches**: from the Bukkit event, the command dispatcher or `Player.attack` down. **It can never witness the input
> decode** (which click, which hand, the held cadence), **reach or minimum charge**, **what the client renders**
> (colour as seen, tooltip order, particles, sound, the swirl) **or feel.** A row whose prediction includes any of
> those is **AUTO-PARTIAL**, and its BEN half is read by Ben or stays NOT READ; **an AUTO half never makes a row
> PASS on its own.** A row's witness changes from Ben to AUTO only by an amendment committed **before** the boot.

- **In the gate:** a third column value: `**AUTO PASS** (selftest <sha>, run <n>): <the lines>` beside `**PASS**`
  (Ben's). A reader sees which instrument read it without opening the log.
- **The staging differs from Ben's**, and is stated: aim by teleport rather than by mouse; a synthetic event rather
  than a click. A prediction written for Ben's staging stays valid only where the gate's staging was itself a command
  (the four zombie commands, `tp … facing`), which is most of them.

---

## 7. THE ESTIMATED SPLIT

**The seat's CORE: 24 rows**, plus the 3 R0 rows already read by CC from the log and the jar:

- #168: MC1-3
- #169: CP2, BN1, WD1, WD3, LS1
- #170: R0d, D1, G1, G3, CM1, CM2
- #171: SB2
- #172: LB2, LB5
- #173: WS1, WS2b, WS2c, WS3
- #174: WB1-3

| class | with reply capture (A) | without (B) |
|---|---|---|
| **AUTO** | 12: CP2, BN1, WD1, WD3, R0d, CM1, CM2, SB2, LB2, WS1, WS2b, WB2 | 9 (CP2, BN1, CM2 drop to partial) |
| **AUTO-PARTIAL** | 11: MC1, MC2, MC3, LS1, D1, G1, G3, LB5, WS2c, WS3, WB3 | 14 |
| **BEN** | 1: WB1 (its stack half is AUTO) | 1 |

*These counts are this survey's classification, not a measurement.* The AUTO-PARTIAL rows assume the menu dump and the
synthetic click are accepted as witnesses of the server's half.

**What Ben's CORE sheet shrinks to, with (A).** No staging by hand and no PLAYERHIT or DOTTICK reading. **He presses
the three stone inputs once (MC3)**, then glances at:
- the Build cell in gold (MC1, MC2);
- the level screen (LS1);
- D1's Back button;
- the green names and gray "Also accessible via" lines (G1, G3);
- the wall with no fire (LB5);
- no swirl on the immune pair (WS2c);
- no explosion (WS3);
- the bow's tooltip (WB1);
- the swirl on the bow's target (WB3).

That is **about 12 looks against 24 staged rows** (an estimate). The OPTIONAL rows shrink the same way, and SB3, SB4,
WB6 and WB7 keep their feel halves.

---

## 8. PHASE 2: THE SIX READS, DONE 2026-10-05 (javap on the pinned jars; the repo read at `f33ded99`)

**Status: Phase 2 BUILT as #175 (`feat/selftest`), NOT RUN, no boot.** The seat's rulings a-d are in §9.

1. **The stone's Q has a real driver: `HumanEntity#dropItem(false)`.** `CraftHumanEntity.dropItem(Z)` calls
   `24: ServerPlayer.drop(Z)Z`. The Q packet calls the same method: `handlePlayerAction` → `478` and `517:
   ServerPlayer.drop(Z)Z`, after its drop-rate limit (`396-460`). `ServerPlayer.drop(Z)` → `51: drop(ItemStack,ZZ)` →
   `LivingEntity.drop(…)`, which builds and calls the event (`98: new PlayerDropItemEvent`, `123: callEvent`). On a
   cancel it puts the item back in the main hand (`138-176`). **Skipped:** only the packet's drop-rate limit. The
   `dropItem(EquipmentSlot, …)` overloads go through `internalDropItemFromInventory` and are not used.
2. **A server-side slot change resets the charge exactly as a client swap does.** The reset is in `Player.tick`, not
   in the packet handler. Each tick compares `lastItemInMainHand` with the main hand (`284-311: ItemStack.matches`,
   `isSameItem`) and calls `315: resetAttackStrengthTicker` on a change, whatever changed the slot.
   `CraftInventoryPlayer.setHeldItemSlot` only sets the slot and sends `ClientboundSetHeldSlotPacket`. **Skipped:**
   the client's `handleSetCarriedItem` fires `PlayerItemHeldEvent` (`58-92`), and `RpgListeners.onHeldItemChange`
   listens to it (the quiver sweep). **So the HOLD driver fires `PlayerItemHeldEvent` first and sets the slot only if
   it is not cancelled**, in the packet's own order.
3. **Our melee weapons sweep.** `emberblade.yml` authors no `material`, and `WeaponDefinition.DEFAULT_MATERIAL` is
   `iron_sword`. The jar's `data/minecraft/tags/item/swords.json` lists `minecraft:iron_sword`, so a standing,
   full-charge `attack()` meets `isSweepAttack`'s SWORDS clause. No CORE row places a second mob in reach.
4. **The reply reader is buildable copy-only.** PacketEvents 2.13.0's `WrapperPlayServerSystemChatMessage(PacketSendEvent)`
   decodes in `read()` (`2: readComponent`, `23: readBoolean` for the overlay). `getMessage()` returns
   `net.kyori.adventure.text.Component`, and **neither PacketEvents jar shades `net/kyori`** (`jar tf`: 0 entries),
   so it is Paper's own Adventure. `isOverlay()` separates the action bar. Precedent: `VanillaCritParticleListener`
   already reads a send-side wrapper. `EventManager.registerListener` returns a handle, and `unregisterListener`
   takes it.
5. **Lifecycle commands land in the dispatcher `performCommand` uses: traced.** `PaperCommands.setDispatcher` builds
   its dispatcher on a `PaperCommands$1 extends ApiMirrorRootNode`, whose `getDispatcher()` returns `4:
   net.minecraft.commands.Commands.getDispatcher()`. `CraftServer.dispatchCommand` parses against that dispatcher
   (`67`). **Also read:** `git grep PlayerCommandPreprocessEvent f33ded99 -- *.java` finds 0 files (control: the same
   grep for `PlayerInteractEvent` finds 2).
6. **The Equipment screen's class slot is a shift-click away, and a synthetic click carries real state.**
   `EquipmentMenu.shiftInElsewhere` → `accessoryShiftIn` handles a shift-click from the player's own inventory.
   `InventoryClickEvent.getCurrentItem()` reads `getView().getItem(rawSlot)`, and `getClickedInventory()` reads
   `getView().getInventory(rawSlot)`. So a synthetic event built from the live view hands `MenuRouting` the real slot
   and item. `Menu.handleClick` cancels first, and our menus do every move themselves, so **a synthetic click and a
   real one end in the same state.** Nothing vanilla runs after a cancelled click.

### 8.1 What the reads changed in §2's classification

| row | was | now | why |
|---|---|---|---|
| MC3 (CORE) | AUTO-PARTIAL, Q driver NOT READ | AUTO-PARTIAL, all three drivers read | read 1 |
| **MC5** | **BEN** | **AUTO-PARTIAL** | read 1: `dropItem(false)` is the Q packet's own call. The server half (the sword dropped, no PLAYERHIT) is AUTO; Ben's Q key is the decode. Not CORE, so no scenario |
| MC6 | AUTO, slot reset NOT READ | AUTO | read 2 |
| **MC8** | AUTO-PARTIAL | **AUTO** | read 6: the Gauntlet goes in and out by a shift-click on the real Equipment screen. Not CORE, so no scenario |
| WB1 (CORE) | "BEN, with an AUTO half" | AUTO-PARTIAL | the same thing, labelled consistently |

**The CORE split as built (24 scenarios + R0d):**
- **AUTO, 11:** CP2, BN1, WD1, WD3, CM1, CM2, SB2, LB2, WS1, WS2b, WB2.
- **AUTO-PARTIAL, 13:** MC1, MC2, MC3, LS1, D1, G1, G3, LB5, WS-C, WS2c, WS3, WB1, WB3.
- **R0d** is a boot line. §7 said 12 / 11 / 1. The difference is WS-C, now CORE (AUTO-PARTIAL), and WB1, relabelled.
  §7's AUTO count included R0d; the 11 here does not.

## 9. THE SEAT'S RULINGS, 2026-10-05 (verbatim in substance)

- **a.** Reply capture is **(A)**, the PacketEvents send-side reader. COPY-ONLY: it never cancels, modifies or delays
  a packet. It is registered only while a run is active, only for the test player, and unregistered on stop, finish
  or quit. A unit or signature test pins copy-only. *Built: `ReplyReaderCopyOnlyTest`, with a control.*
- **b.** The dev marker is `-Drpg.dev=true`, set only by `dev-server.sh`. The command refuses without it, in a non-dev
  world, or with a second player online. *Built. "Non-dev world" is read as "not the server's first world"; this is
  stated in `SelfTestGuard`.*
- **c.** The one R0 moves to #175: `GATE-selftest.md` carries the cumulative R0 for #168-#175. #174 gets the
  SUPERSEDED note. The boot is #175's tip.
- **d.** The honesty rule is accepted. Added: the instrument never prints PASS or FAIL. *Account in
  `verification.md`, pointer in `CLAUDE.md`, pinned by `SelfTestNeverJudgesTest`.*
- **Not ruled, NOT BUILT:** §4's crash-restore file, and §4's refusal over other enabled plugins (not built: Paper's
  bundled plugins would make it brittle).
